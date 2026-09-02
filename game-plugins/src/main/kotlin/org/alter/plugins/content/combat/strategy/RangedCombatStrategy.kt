package org.alter.plugins.content.combat.strategy

import org.alter.api.EquipmentType
import org.alter.api.ProjectileType
import org.alter.api.Skills
import org.alter.api.WeaponType
import org.alter.api.ext.*
import org.alter.game.model.Tile
import org.alter.game.model.combat.AttackStyle
import org.alter.game.model.combat.PawnHit
import org.alter.game.model.combat.XpMode
import org.alter.game.model.entity.*
import org.alter.rscm.RSCM.getRSCM
import org.alter.plugins.content.combat.Combat
import org.alter.plugins.content.combat.CombatConfigs
import org.alter.plugins.content.combat.createProjectile
import org.alter.plugins.content.combat.dealExactHit
import org.alter.plugins.content.combat.dealHit
import org.alter.plugins.content.combat.formula.RangedCombatFormula
import org.alter.plugins.content.combat.strategy.ranged.RangedAmmo
import org.alter.plugins.content.items.blowpipe.Blowpipe
import org.alter.plugins.content.combat.strategy.ranged.RangedAoe
import org.alter.plugins.content.combat.strategy.ranged.ammo.Darts
import org.alter.plugins.content.combat.strategy.ranged.ammo.EnchantedBolt
import org.alter.plugins.content.combat.strategy.ranged.ammo.Knives
import org.alter.plugins.content.combat.strategy.ranged.weapon.BowType
import org.alter.plugins.content.combat.strategy.ranged.weapon.Bows
import org.alter.plugins.content.combat.strategy.ranged.weapon.CrossbowType

/**
 * @author Tom <rspsmods@gmail.com>
 */
object RangedCombatStrategy : CombatStrategy {
    private const val DEFAULT_ATTACK_RANGE = 7

    private const val MAX_ATTACK_RANGE = 10

    /** A chinchompa detonates over the 3x3 centred on its target, hitting up to nine. */
    private const val BLAST_RADIUS = 1
    private const val MAX_BLAST_TARGETS = 9

    override fun getAttackRange(pawn: Pawn): Int {
        if (pawn is Player) {
            val weapon = pawn.getEquipment(EquipmentType.WEAPON)
            val attackStyle = CombatConfigs.getAttackStyle(pawn)

            var range =
                when (weapon?.id) {
                    getRSCM("item.armadyl_crossbow") -> 8
                    getRSCM("item.craws_bow"), getRSCM("item.craws_bow_u") -> 10
                    getRSCM("item.chinchompa_10033"), getRSCM("item.red_chinchompa_10034"), getRSCM("item.black_chinchompa") -> 9
                    in Bows.LONG_BOWS -> 9
                    in Knives.KNIVES -> 6
                    in Darts.DARTS -> 3
                    in Bows.CRYSTAL_BOWS -> 10
                    else -> DEFAULT_ATTACK_RANGE
                }

            if (attackStyle == AttackStyle.LONG_RANGE) {
                range += 2
            }

            return Math.min(MAX_ATTACK_RANGE, range)
        }
        return DEFAULT_ATTACK_RANGE
    }

    override fun canAttack(
        pawn: Pawn,
        target: Pawn,
    ): Boolean {
        if (pawn is Player) {
            /*
             * A blowpipe carries its own darts and scales, so the quiver checks below
             * do not apply to it - but it cannot fire without both.
             */
            Blowpipe.equipped(pawn)?.let { blowpipe ->
                if (Blowpipe.dartCount(blowpipe) <= 0) {
                    pawn.message("Your blowpipe has no darts left in it.")
                    return false
                }
                if (Blowpipe.scaleCount(blowpipe) <= 0) {
                    pawn.message("Your blowpipe has run out of scales.")
                    return false
                }
                return true
            }

            val weapon = pawn.getEquipment(EquipmentType.WEAPON)
            val ammo = pawn.getEquipment(EquipmentType.AMMO)

            val crossbow = CrossbowType.values.firstOrNull { it.item == weapon?.id }
            if (crossbow != null && ammo?.id !in crossbow.ammo) {
                val message = if (ammo != null) "You can't use that ammo with your crossbow." else "There is no ammo left in your quiver."
                pawn.message(message)
                return false
            }

            val bow = BowType.values.firstOrNull { it.item == weapon?.id }
            if (bow != null && bow.ammo.isNotEmpty() && ammo?.id !in bow.ammo) {
                val message = if (ammo != null) "You can't use that ammo with your bow." else "There is no ammo left in your quiver."
                pawn.message(message)
                return false
            }
        }
        return true
    }

    override fun attack(
        pawn: Pawn,
        target: Pawn,
    ) {
        val world = pawn.world

        val animation = CombatConfigs.getAttackAnimation(pawn)
        if (pawn is Npc) {
            val def = pawn.combatDef
            if (def.defaultAttackSound > 0) {
                if (def.defaultAttackSoundArea) {
                    world.spawn(
                        AreaSound(pawn.tile, def.defaultAttackSound, def.defaultAttackSoundRadius, def.defaultAttackSoundVolume),
                    )
                } else if (target is Player) {
                    target.playSound(def.defaultAttackSound, def.defaultAttackSoundVolume)
                }
            }
        } else if (pawn is Player) {
            world.spawn(AreaSound(pawn.tile, CombatConfigs.getWeaponAttackSound(pawn), 5, 1))
        }
        // @TODO later for player block sound.

        if (pawn is Player) {
            if (pawn.hasWeaponType(WeaponType.CHINCHOMPA)) {
                pawn.animate(animation)
                throwChinchompa(pawn, target)
                return
            }
            /*
             * Fire the equipped ammo first: the returned action drops the spent ammo at
             * the target's feet when the hit lands.
             */
            val ammoDropAction = RangedAmmo.fire(pawn, target)
            pawn.animate(animation)
            shoot(pawn, target, onHit = ammoDropAction)
            return
        }

        if (pawn is Npc) {
            fireNpcProjectile(pawn, target)
        }
        pawn.animate(animation)

        val maxHit = RangedCombatFormula.getMaxHit(pawn, target)
        val landHit = RangedCombatFormula.getAccuracy(pawn, target) >= world.randomDouble()
        val hitDelay = getHitDelay(pawn.getCentreTile(), target.tile.transform(target.getSize() / 2, target.getSize() / 2))
        pawn.dealHit(target = target, maxHit = maxHit, landHit = landHit, delay = hitDelay)
    }

    /**
     * Resolves one ranged shot from [player] - accuracy, damage, enchanted bolt effects
     * and Ranged/Hitpoints experience.
     *
     * Shared by the ordinary attack above and by every ranged special attack, so that
     * specials get bolt effects, prayer bonuses and void scaling without restating any
     * of it. Specials adjust the shot through the multiplier parameters rather than
     * rolling their own damage.
     *
     * Firing the ammo itself is [RangedAmmo.fire]'s job and is deliberately not done
     * here: specials that fire several projectiles consume ammo per projectile but
     * resolve their hits separately.
     *
     * @param accuracyMultiplier scales the attack roll. Note that anything other than
     *   `1.0` replaces the twisted bow / dragon hunter crossbow scaling rather than
     *   stacking with it - that is [RangedCombatFormula]'s existing special-attack
     *   convention.
     * @param damageMultiplier scales the max hit, with the same caveat.
     * @param forcedLandHit replaces the accuracy roll with a fixed outcome, leaving the
     *   damage roll alone. `true` for the magic longbow's Powershot; a chinchompa's
     *   splash targets pass the primary target's result, since one roll decides the
     *   whole explosion.
     * @param boltChanceMultiplier scales enchanted bolt activation chance (the Armadyl
     *   crossbow's special doubles it).
     * @param minimumDamage floor applied to a landed hit (the dark bow's special).
     * @param damageCap ceiling applied to a landed hit (the dark bow's special).
     * @param ignoreOffensivePrayers drops Sharp Eye/Hawk Eye/Eagle Eye/Rigour from the
     *   damage roll (the rune thrownaxe's Chainhit).
     */
    fun shoot(
        player: Player,
        target: Pawn,
        accuracyMultiplier: Double = 1.0,
        damageMultiplier: Double = 1.0,
        forcedLandHit: Boolean? = null,
        boltChanceMultiplier: Double = 1.0,
        minimumDamage: Int = 0,
        damageCap: Int = Int.MAX_VALUE,
        ignoreOffensivePrayers: Boolean = false,
        hitDelay: Int = -1,
        onHit: (PawnHit).() -> Unit = {},
    ): PawnHit {
        val world = player.world

        val bolt = EnchantedBolt.roll(player, target, world, boltChanceMultiplier)

        val accuracy =
            RangedCombatFormula.getAccuracy(
                pawn = player,
                target = target,
                specialAttackMultiplier = accuracyMultiplier,
                ignoreDefence = bolt?.ignoresDefence() == true,
            )
        val maxHit =
            RangedCombatFormula.getMaxHit(
                pawn = player,
                target = target,
                specialAttackMultiplier = damageMultiplier,
                specialPassiveMultiplier = bolt?.maxHitMultiplier() ?: 1.0,
                ignoreOffensivePrayers = ignoreOffensivePrayers,
            )

        val landHit = forcedLandHit ?: (accuracy >= world.randomDouble())
        val rolled = if (landHit) world.random(maxHit) else 0
        val wasMaxRoll = landHit && rolled == maxHit

        var damage = rolled
        if (landHit) {
            if (bolt != null) {
                damage = EnchantedBolt.applyDamage(bolt, player, target, damage)
            }
            damage = damage.coerceAtLeast(minimumDamage).coerceAtMost(damageCap)
        }
        val dealt = damage

        val delay =
            if (hitDelay != -1) {
                hitDelay
            } else {
                getHitDelay(player.getCentreTile(), target.tile.transform(target.getSize() / 2, target.getSize() / 2))
            }

        val pawnHit =
            player.dealExactHit(
                target = target,
                damage = dealt,
                landHit = landHit,
                delay = delay,
                maxHit = wasMaxRoll,
            ) { hit ->
                onHit(hit)
                if (bolt != null && hit.landed()) {
                    target.graphic(bolt.gfx, 0)
                    EnchantedBolt.applyOnHit(bolt, player, target, dealt)
                }
            }

        if (dealt > 0) {
            addCombatXp(player, target, dealt)
        }
        return pawnHit
    }

    /**
     * A chinchompa throw: one chinchompa flies to [target], detonates, and hits
     * everything in the 3x3 around it.
     *
     * Two mechanics from the wiki (Chinchompa (weapon)) shape this:
     *
     * 1. **One accuracy roll decides the whole explosion.** The primary target is rolled
     *    normally; if it lands, every target caught in the blast is hit, and if it
     *    misses they all miss. Damage is still rolled per target, so the splats differ.
     * 2. **The fuse styles trade accuracy for range.** Short/medium/long fuse - the
     *    Accurate/Rapid/Longrange styles - each have a distance band they are fully
     *    accurate at, falling off to 75% and then 50% outside it. See [fuseAccuracy].
     *
     * Only one chinchompa is consumed, and it always is: chinchompas cannot be
     * recovered even when they do no damage, which [RangedAmmo.fire] already handles
     * through [org.alter.plugins.content.combat.strategy.ranged.RangedProjectile.breakOnImpact].
     *
     * **Deliberate deviation:** in the real game the splash only applies in multi-combat
     * areas. This codebase has no engine-level multi-combat enforcement at all - the
     * multiway varbit only drives the client's icon - so gating on it would mean gating
     * on nothing. The blast therefore always spreads, and
     * [org.alter.plugins.content.combat.strategy.ranged.RangedAoe] keeps it to npcs so
     * this cannot splash across bystanding players.
     */
    private fun throwChinchompa(
        player: Player,
        target: Pawn,
    ) {
        val targetCentre = target.tile.transform(target.getSize() / 2, target.getSize() / 2)
        val hitDelay = getHitDelay(player.getCentreTile(), targetCentre)
        val accuracyMultiplier = fuseAccuracy(CombatConfigs.getAttackStyle(player), player.getCentreTile().getDistance(targetCentre))

        /*
         * Read the explosion graphic before firing: firing consumes the chinchompa, and
         * on the last one the weapon slot is empty afterwards.
         */
        val blastGraphic = RangedAmmo.impactGraphic(player)

        /*
         * The chinchompa itself - projectile, explosion graphic, and the one chinchompa
         * spent - is fired at the primary target only.
         */
        RangedAmmo.fire(player, target)

        val primaryHit =
            shoot(
                player = player,
                target = target,
                accuracyMultiplier = accuracyMultiplier,
                hitDelay = hitDelay,
            )

        val splashTargets = RangedAoe.targetsAround(player, target, radius = BLAST_RADIUS, max = MAX_BLAST_TARGETS).drop(1)
        for (victim in splashTargets) {
            blastGraphic?.let { victim.graphic(it.id, it.height, hitDelay) }
            shoot(
                player = player,
                target = victim,
                accuracyMultiplier = accuracyMultiplier,
                forcedLandHit = primaryHit.landed,
                hitDelay = hitDelay,
            )
        }
    }

    /**
     * The chinchompa fuse accuracy multiplier, from the wiki's accuracy-by-distance
     * table. Each style is fully accurate inside its own band and degrades outside it:
     *
     * ```
     * style                 0-3 tiles   4-6 tiles   7+ tiles
     * Short fuse (Accurate)      100%         75%        50%
     * Medium fuse (Rapid)         75%        100%        75%
     * Long fuse (Longrange)       50%         75%       100%
     * ```
     */
    private fun fuseAccuracy(
        style: AttackStyle,
        distance: Int,
    ): Double {
        val band =
            when {
                distance <= 3 -> 0
                distance <= 6 -> 1
                else -> 2
            }
        return when (style) {
            AttackStyle.ACCURATE -> doubleArrayOf(1.0, 0.75, 0.5)
            AttackStyle.LONG_RANGE -> doubleArrayOf(0.5, 0.75, 1.0)
            // Rapid is medium fuse; anything unexpected falls back to it.
            else -> doubleArrayOf(0.75, 1.0, 0.75)
        }[band]
    }

    /**
     * Spawns the projectile a ranged npc fires, as configured by its `ranged { }` combat
     * definition block.
     *
     * Npcs never got a projectile before: the whole projectile block lived inside a
     * `pawn is Player` branch, so a ranged monster's shots arrived invisibly.
     */
    private fun fireNpcProjectile(
        npc: Npc,
        target: Pawn,
    ) {
        val def = npc.combatDef
        if (def.rangedProjectileGfx == -1) {
            return
        }
        val type =
            ProjectileType.values().getOrNull(def.rangedProjectileType) ?: ProjectileType.ARROW
        val projectile = npc.createProjectile(target, def.rangedProjectileGfx, type)
        if (def.rangedDrawbackGfx != -1) {
            npc.graphic(def.rangedDrawbackGfx, def.rangedDrawbackHeight)
        }
        if (def.rangedImpactGfx != -1) {
            target.graphic(def.rangedImpactGfx, def.rangedImpactHeight, projectile.lifespan)
        }
        npc.world.spawn(projectile)
    }

    fun getHitDelay(
        start: Tile,
        target: Tile,
    ): Int {
        val distance = start.getDistance(target)
        return 2 + (Math.floor((3.0 + distance) / 6.0)).toInt()
    }

    private fun addCombatXp(
        player: Player,
        target: Pawn,
        damage: Int,
    ) {
        val modDamage = if (target.entityType.isNpc) Math.min(target.getCurrentHp(), damage) else damage
        val mode = CombatConfigs.getXpMode(player)
        val multiplier = if (target is Npc) Combat.getNpcXpMultiplier(target) else 1.0

        if (mode == XpMode.RANGED) {
            player.addXp(Skills.RANGED, modDamage * 4.0 * multiplier)
            player.addXp(Skills.HITPOINTS, modDamage * 1.33 * multiplier)
        } else if (mode == XpMode.SHARED) {
            player.addXp(Skills.RANGED, modDamage * 2.0 * multiplier)
            player.addXp(Skills.DEFENCE, modDamage * 2.0 * multiplier)
            player.addXp(Skills.HITPOINTS, modDamage * 1.33 * multiplier)
        }
    }
}
