package org.alter.plugins.content.combat

import org.alter.api.*
import org.alter.api.ext.*
import org.alter.game.model.Tile
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.attr.COMBAT_TARGET_FOCUS_ATTR
import org.alter.game.model.attr.LAST_HIT_ATTR
import org.alter.game.model.attr.LAST_HIT_BY_ATTR
import org.alter.game.model.collision.rayCast
import org.alter.game.model.combat.CombatClass
import org.alter.game.model.entity.AreaSound
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.timer.ACTIVE_COMBAT_TIMER
import org.alter.game.model.timer.ATTACK_DELAY
import org.alter.game.model.timer.PROTECTION_PRAYER_BLOCK_TIMER
import org.alter.plugins.content.combat.strategy.CombatStrategy
import org.alter.plugins.content.combat.strategy.MagicCombatStrategy
import org.alter.plugins.content.combat.strategy.MeleeCombatStrategy
import org.alter.plugins.content.combat.strategy.RangedCombatStrategy
import org.alter.plugins.content.combat.strategy.magic.CombatSpell
import org.alter.plugins.content.interfaces.attack.AttackTab
import org.alter.plugins.content.npcs.slayer.SlayerImmunity
import java.lang.ref.WeakReference
import org.alter.plugins.content.areas.duelarena.DuelRules
import org.alter.plugins.content.areas.duelarena.DuelStyle
import org.alter.plugins.content.areas.duelarena.getActiveDuel
import org.alter.plugins.content.areas.wilderness.WildernessRisk

/**
 * @author Tom <rspsmods@gmail.com>
 */
object Combat {
    val CASTING_SPELL = AttributeKey<CombatSpell>()
    val DAMAGE_DEAL_MULTIPLIER = AttributeKey<Double>()
    val DAMAGE_TAKE_MULTIPLIER = AttributeKey<Double>()

    /**
     * A melee-only version of [DAMAGE_TAKE_MULTIPLIER], read by [MeleeCombatFormula] alone.
     *
     * Needed because two specials reduce *melee* damage specifically and nothing else: the staff of
     * the dead's Power of Death (half melee damage for a minute) and Vesta's spear's Spear Wall
     * (melee immunity for five seconds). Putting either on the shared key would have quietly halved
     * incoming ranged damage too.
     */
    val MELEE_DAMAGE_TAKE_MULTIPLIER = AttributeKey<Double>()

    /**
     * Set by a special attack that grants an immediate follow-up attack rather than
     * waiting out the weapon's speed - the dragon thrownaxe's Momentum Throw. Consumed
     * by [postAttack], which runs after the special resolves and would otherwise
     * overwrite any attack delay the special set for itself.
     */
    val INSTANT_NEXT_ATTACK = AttributeKey<Boolean>()

    /**
     * Extra ticks added to the delay after the current attack, then cleared.
     *
     * [INSTANT_NEXT_ATTACK]'s opposite, and the same shape: a special cannot set the attack delay
     * itself because [postAttack] overwrites it immediately afterwards, so the slow specials -
     * the ballistas' Concentrated Shot at +2.4 seconds, the keris partisan of corruption's halved
     * attack speed - leave the extra here for [postAttack] to add on.
     */
    val EXTRA_ATTACK_DELAY = AttributeKey<Int>()

    /**
     * Set (to the pending defensive flag) while the player has clicked
     * Autocast or Defensive Autocast and the client's native autocast spell grid is
     * showing over the Combat Options tab, waiting for them to pick a spell - see
     * [org.alter.plugins.content.interfaces.gameframe.tabs.combat_options.AttackTabPlugin],
     * which sets it when opening the grid and consumes it when the pick comes back, and
     * [org.alter.plugins.content.magic.AutocastInterface] for how that grid works.
     */
    val AWAITING_AUTOCAST_SELECTION = AttributeKey<Boolean>()

    const val PRIORITY_PID_VARP = 1075
    const val SELECTED_AUTOCAST_VARBIT = 276
    const val DEFENSIVE_MAGIC_CAST_VARBIT = 2668

    fun reset(pawn: Pawn) {
        pawn.attr.remove(COMBAT_TARGET_FOCUS_ATTR)
    }

    fun canAttack(
        pawn: Pawn,
        target: Pawn,
        combatClass: CombatClass,
    ): Boolean = canAttack(pawn, target, getStrategy(combatClass))

    fun canAttack(
        pawn: Pawn,
        target: Pawn,
        strategy: CombatStrategy,
    ): Boolean =
        canEngage(pawn, target) &&
            duelAllowsStyle(pawn, strategy) &&
            strategy.canAttack(pawn, target)

    /**
     * A duel's "No Melee" / "No Ranged" / "No Magic" rules, asked at the only point the style being
     * used is actually known. [canEngage] settles whether the two may fight at all; this settles
     * whether they may do it this way.
     */
    private fun duelAllowsStyle(
        pawn: Pawn,
        strategy: CombatStrategy,
    ): Boolean {
        val player = pawn as? Player ?: return true
        if (player.getActiveDuel() == null) return true
        return DuelRules.canAttackWith(player, styleOf(strategy))
    }

    fun isAttackDelayReady(pawn: Pawn): Boolean = !pawn.timers.has(ATTACK_DELAY)

    fun postAttack(
        pawn: Pawn,
        target: Pawn,
    ) {
        /*
         * Skulling, before anything below touches LAST_HIT_BY_ATTR - the retaliation test reads
         * the attacker's own copy of it, which is only written when *they* are hit, but keeping
         * this first means the two can never be read out of order. A duel is its own PvP area with
         * its own consequences and never skulls anyone.
         */
        if (pawn is Player && target is Player && pawn.getActiveDuel() == null) {
            WildernessRisk.onPlayerAttackedPlayer(pawn, target)
        }

        val extraDelay = pawn.attr[EXTRA_ATTACK_DELAY] ?: 0
        pawn.attr.remove(EXTRA_ATTACK_DELAY)
        pawn.timers[ATTACK_DELAY] =
            if (pawn.attr[INSTANT_NEXT_ATTACK] == true) {
                pawn.attr.remove(INSTANT_NEXT_ATTACK)
                1
            } else {
                CombatConfigs.getAttackDelay(pawn) + extraDelay
            }
        target.timers[ACTIVE_COMBAT_TIMER] = 17 // 10,2 seconds

        pawn.attr[LAST_HIT_ATTR] = WeakReference(target)
        target.attr[LAST_HIT_BY_ATTR] = WeakReference(pawn)

        /*
         * A spell cast by hand out of the spellbook is a one-off. What follows it
         * depends on whether autocast is set.
         *
         * With no autocast, the cast ends the fight - [reset] drops the target focus so
         * the player stands still afterwards, the same as real OSRS.
         *
         * With autocast set, the fight carries on, but it has to carry on with the
         * *autocast* spell rather than the one just cast by hand. Clearing the attribute
         * is all that is needed: [org.alter.plugins.content.combat.CombatPlugin] puts the
         * autocast spell back at the top of the next cycle, before the strategy is
         * chosen, because it only skips that step while a spell is already set. Leaving
         * it set is what let a hand-cast spell quietly take over as the autocast spell
         * and keep repeating until another one was picked, while the Combat Options tab
         * still showed the real autocast spell.
         *
         * Clearing it here is safe.
         * [org.alter.plugins.content.combat.strategy.MagicCombatStrategy.attack] reads
         * the spell into a local and finishes with it before returning, and postAttack
         * runs after that - the delayed-hit callback closes over that local rather than
         * reading the attribute again.
         */
        if (pawn is Player && pawn.attr.has(CASTING_SPELL)) {
            val autocastId = pawn.getVarbit(SELECTED_AUTOCAST_VARBIT)
            if (autocastId == 0) {
                reset(pawn)
                pawn.attr.remove(CASTING_SPELL)
            } else if (pawn.attr[CASTING_SPELL]?.autoCastId != autocastId) {
                pawn.attr.remove(CASTING_SPELL)
            }
        }

        if (target is Player && target.interfaces.getModal() != -1) {
            target.closeInterface(target.interfaces.getModal())
            target.interfaces.setModal(-1)
        }
    }

    fun postDamage(
        pawn: Pawn,
        target: Pawn,
    ) {
        if (target.isDead()) {
            return
        }

        /*
         * Don't override the animation if one is already set. @Z-Kris
         *
         * This was dead for npcs until Pawn.animate started recording their claim too:
         * previouslySetAnim was only ever assigned for players, so every npc read -1 here and
         * played its block regardless. An npc that swung and was hit on the same tick had its
         * attack animation overwritten before the tick's extended info was ever encoded, so
         * the client only saw the flinch. It still gates the block *sound* as well as the
         * animation, which is why the check stays here rather than being left to animate().
         */
        val hasBlock = target.previouslySetAnim != -1

        if (!hasBlock) {
            target.animate(CombatConfigs.getBlockAnimation(target))
            if (target is Npc) {
                val npcDefs = target.combatDef
                if (npcDefs.defaultBlockSound > 0) {
                    if (npcDefs.defaultBlockSoundArea) {
                        target.world.spawn(
                            AreaSound(target.tile, npcDefs.defaultBlockSound, npcDefs.defaultBlockSoundRadius, npcDefs.defaultBlockSoundVolume),
                        )
                    } else {
                        (pawn as? Player)?.playSound(npcDefs.defaultBlockSound, npcDefs.defaultBlockSoundVolume)
                    }
                }
            }
        }

        if (target.lock.canAttack()) {
            if (target.entityType.isNpc) {
                /*
                 * An NPC that is already busy with a living target does not drop it
                 * just because someone else landed a hit - that is how OSRS behaves,
                 * and it also matters mechanically here: `attack()` begins with
                 * `interruptQueues()`, so re-targeting on every incoming hit tore down
                 * the NPC's running combat loop and restarted it, which read in-game
                 * as the NPC stuttering between engaging and not engaging.
                 */
                val engaged = target.getCombatTarget()
                val engagedGone = engaged == null || engaged.isDead() || (engaged is Player && !engaged.isOnline)
                if (engagedGone) {
                    target.attack(pawn)
                }
            } else if (target is Player) {
                val strategy = CombatConfigs.getCombatStrategy(target)
                val attackRange = strategy.getAttackRange(target)
                /*
                 * The auto-retaliate setting was being ignored entirely (the varp check
                 * was commented out), and a player already mid-fight was re-targeted
                 * onto whoever hit them last - again via `attack()`, so their own
                 * attack loop was interrupted and restarted mid-swing every time a
                 * second attacker connected.
                 */
                val autoRetaliateEnabled = target.getVarp(AttackTab.DISABLE_AUTO_RETALIATE_VARP) == 0
                val alreadyFighting = target.getCombatTarget()?.isDead() == false
                if (autoRetaliateEnabled && !alreadyFighting && target.tile.isWithinRadius(pawn.tile, attackRange)) {
                    target.attack(pawn)
                }
                /*
                 * @TODO Out-of-range auto-retaliate. A melee player attacked from
                 * range still won't retaliate at all, because of the attackRange
                 * guard above - in OSRS they'd walk to the attacker. Needs the chase
                 * behaviour that `moveToAttackRange` never got.
                 */
            }
        }
    }

    /**
     * The [NpcSkills] index matching player skill [skill], or `null` when npcs have no
     * such stat.
     *
     * Npcs store their combat levels in a five-slot array with its own ordering, so a
     * player [Skills] constant used directly against `Npc.stats` silently reads the
     * wrong stat (Strength and Defence are swapped) or overflows the array outright -
     * `Skills.MAGIC` is 6, which threw ArrayIndexOutOfBoundsException.
     */
    fun toNpcSkill(skill: Int): Int? =
        when (skill) {
            Skills.ATTACK -> NpcSkills.ATTACK
            Skills.STRENGTH -> NpcSkills.STRENGTH
            Skills.DEFENCE -> NpcSkills.DEFENCE
            Skills.MAGIC -> NpcSkills.MAGIC
            Skills.RANGED -> NpcSkills.RANGED
            else -> null
        }

    fun getNpcXpMultiplier(npc: Npc): Double {
        val attackLvl = npc.stats.getMaxLevel(NpcSkills.ATTACK)
        val strengthLvl = npc.stats.getMaxLevel(NpcSkills.STRENGTH)
        val defenceLvl = npc.stats.getMaxLevel(NpcSkills.DEFENCE)
        val hitpoints = npc.getMaxHp()

        val averageLvl = Math.floor((attackLvl + strengthLvl + defenceLvl + hitpoints) / 4.0)
        val averageDefBonus =
            Math.floor(
                (
                    npc.getBonus(
                        BonusSlot.DEFENCE_STAB,
                    ) + npc.getBonus(BonusSlot.DEFENCE_SLASH) + npc.getBonus(BonusSlot.DEFENCE_CRUSH)
                ) / 3.0,
            )
        return 1.0 + Math.floor(averageLvl * (averageDefBonus + npc.getStrengthBonus() + npc.getAttackBonus()) / 5120.0) / 40.0
    }

    fun raycast(
        pawn: Pawn,
        target: Pawn,
        distance: Int,
        projectile: Boolean,
    ): Boolean = pawn.tile.isWithinRadius(target.tile, distance) && hasAttackLineOfSight(pawn, target, projectile)

    /**
     * [default], unless [pawn] is an npc whose combat def overrides its attack range.
     *
     * Every [CombatStrategy] hardcoded its range for npcs - 1 melee, 7 ranged, 10 magic
     * - because [org.alter.game.model.combat.NpcCombatDef] had nowhere to put one.
     * (`attackRanged` in the npc DSL is easy to mistake for this; it is the ranged
     * *attack bonus*, a stat, not a distance.) Npcs that leave it unset keep the
     * default, so this changes nothing on its own.
     */
    fun npcAttackRange(
        pawn: Pawn,
        default: Int,
    ): Int = (pawn as? Npc)?.combatDef?.attackRange?.takeIf { it != -1 } ?: default

    /**
     * The number of tiles between the closest edges of [pawn]'s and [target]'s
     * footprints - `0` when the two boxes touch or overlap, `1` when they are
     * adjacent (diagonals included), and so on.
     *
     * This is the metric attack ranges are expressed in, and the combat loop was not
     * using it. It compared `Tile.getDistance` - a *Euclidean* distance, rounded up -
     * against `attackRange + target.getSize()`, which is wrong twice over:
     *
     * - Euclidean distance overstates diagonals, so a 7-tile bow reached 7 tiles due
     *   north but only 5 to the north-east.
     * - Adding the target's size on top compensated for that by inflating every range,
     *   which is where "melee connecting from two tiles away" came from: a 1-tile
     *   weapon was really being allowed `ceil(sqrt(2)) = 2`.
     *
     * Measuring between box edges instead handles large npcs correctly on its own - a
     * 3x3 dragon is in melee range when you are next to any of its nine tiles - so no
     * size fudge is needed.
     */
    fun edgeDistance(
        pawn: Pawn,
        target: Pawn,
    ): Int {
        val a = Box(pawn.tile.x, pawn.tile.z, pawn.getSize() - 1, pawn.getSize() - 1)
        val b = Box(target.tile.x, target.tile.z, target.getSize() - 1, target.getSize() - 1)
        val dx = maxOf(0, maxOf(a.x1 - b.x2, b.x1 - a.x2))
        val dz = maxOf(0, maxOf(a.y1 - b.y2, b.y1 - a.y2))
        return maxOf(dx, dz)
    }

    /**
     * Whether [pawn] can see [target] well enough to attack it - i.e. nothing solid
     * stands between them.
     *
     * [projectile] picks which of the two line tests is used: `true` casts a line of
     * *sight*, which is what arrows, bolts and spells travel along and which some
     * objects (low fences, tables) deliberately let through; `false` casts a line of
     * *walk*, the stricter test used for melee, where anything you cannot step over
     * also stops you swinging.
     *
     * Both entities' sizes are passed through, so a large npc is attackable from any
     * tile that can see any part of it rather than only from tiles that can see its
     * south-west corner.
     *
     * A raycast across height levels is meaningless - and [rayCast] throws on one - so
     * differing heights are rejected up front. Standing on the target's own tile always
     * counts as visible, since there is no line to cast.
     */
    fun hasAttackLineOfSight(
        pawn: Pawn,
        target: Pawn,
        projectile: Boolean,
    ): Boolean {
        if (pawn.tile.height != target.tile.height) {
            return false
        }
        if (pawn.tile.sameAs(target.tile)) {
            return true
        }
        return pawn.world.lineValidator.rayCast(
            start = pawn.tile,
            target = target.tile,
            projectile = projectile,
            srcSize = pawn.getSize(),
            destWidth = target.getSize(),
            destLength = target.getSize(),
        )
    }

    /**
     * Whether [pawn] is close enough to [target] to attack it from [distance] tiles,
     * with line of sight.
     *
     * **This does not move anything**, despite the name - the walking half
     * (`|| pawn.walkToInteract(...)`) has been commented out for as long as this file
     * has existed, so it is purely a range test. The engine's own combat loop
     * ([org.alter.plugins.content.combat.CombatPlugin.cycle]) does the chasing for
     * normal NPCs; custom per-NPC combat loops that call this (Dark Wizards, KBD) get
     * no chase behaviour at all and will simply wait for the target to come to them.
     *
     * The [distance] > 1 branch used to build the target's range box as
     * `Box(end.x, end.z, distance - 1, distance - 1)`, which is anchored at the
     * target's own tile and grows **north-east only** - so a ranged/magic NPC would
     * only ever find a target in range if that target stood north or east of it, and
     * was blind to anyone to the south or west no matter how close. That is why Dark
     * Wizards appeared to cast "only when you're up close" and inconsistently: being
     * adjacent on the north-east side worked, everything else failed the check and the
     * wizard silently did nothing. The box is now expanded by [distance] on all four
     * sides of the target instead, which is symmetric.
     *
     * The melee branch (`distance <= 1`) is unchanged and still uses [areBordering],
     * which correctly excludes pure diagonals.
     */
    suspend fun moveToAttackRange(
        it: QueueTask,
        pawn: Pawn,
        target: Pawn,
        distance: Int,
        projectile: Boolean,
    ): Boolean {
        val start = pawn.tile
        val end = target.tile

        val srcSize = pawn.getSize()
        val dstSize = target.getSize()

        val touching =
            if (distance > 1) {
                areOverlapping(
                    start.x,
                    start.z,
                    srcSize,
                    srcSize,
                    end.x - distance,
                    end.z - distance,
                    dstSize + distance * 2,
                    dstSize + distance * 2,
                )
            } else {
                areBordering(start.x, start.z, srcSize, srcSize, end.x, end.z, dstSize, dstSize)
            }
        val withinRange = touching && hasAttackLineOfSight(pawn, target, projectile)
        return withinRange //|| pawn.walkToInteract(it, target, lineOfSightRange = distance)
    }
    fun getProjectileLifespan(
        source: Pawn,
        target: Tile,
        type: ProjectileType,
    ): Int =
        when (type) {
            ProjectileType.MAGIC -> {
                val fastRoute = source.tile.getChebyshevDistance(target)
                5 + (fastRoute * 10)
            }
            else -> {
                val distance = source.tile.getDistance(target)
                type.calculateLife(distance)
            }
        }

    fun canEngage(
        pawn: Pawn,
        target: Pawn,
    ): Boolean {
        if (pawn.isDead() || target.isDead()) {
            return false
        }

        val maxDistance =
            when {
                pawn is Player && pawn.hasLargeViewport() -> Player.LARGE_VIEW_DISTANCE
                else -> Player.NORMAL_VIEW_DISTANCE
            }
        if (!pawn.tile.isWithinRadius(target.tile, maxDistance)) {
            return false
        }

        val pvp = pawn.entityType.isPlayer && target.entityType.isPlayer

        if (pawn is Player) {
            if (!pawn.isOnline) {
                return false
            }

            if (pawn.hasWeaponType(WeaponType.BULWARK) && pawn.getAttackStyle() == 3) {
                pawn.message("Your bulwark is in its defensive state and can't be used to attack.")
                return false
            }

            if (pawn.invisible && pvp) {
                pawn.message("You can't attack while invisible.")
                return false
            }
        } else if (pawn is Npc) {
            if (!pawn.isSpawned()) {
                return false
            }
        }

        if (target is Npc) {
            if (!target.isSpawned()) {
                return false
            }
            if (!target.def.isAttackable() || target.combatDef.hitpoints == -1) {
                (pawn as? Player)?.message("You can't attack this npc.")
                return false
            }
            if (pawn is Player && target.combatDef.slayerReq > pawn.getSkills().getBaseLevel(Skills.SLAYER)) {
                pawn.message("You need a higher Slayer level to know how to wound this monster.")
                return false
            }
            // Turoths and kurasks take damage only from leaf-bladed weaponry or broad ammunition.
            if (pawn is Player && !SlayerImmunity.canDamage(pawn, target)) {
                pawn.message(SlayerImmunity.MESSAGE)
                return false
            }
        } else if (target is Player) {
            if (!target.isOnline || target.invisible) {
                return false
            }

            if (!target.lock.canBeAttacked()) {
                return false
            }

            if (pvp) {
                pawn as Player

                /*
                 * A duel is its own PvP area with its own rules: the two players may fight each
                 * other whatever their levels, and nobody else may join in. Checked before the
                 * wilderness rules because none of those apply inside an arena.
                 */
                val duel = pawn.getActiveDuel()
                if (duel != null || target.getActiveDuel() != null) {
                    if (duel == null || duel.other(pawn).player != target) {
                        pawn.message("You can't interfere with a duel.")
                        return false
                    }
                    // The two duellists may fight regardless of level or of where they stand; which
                    // styles they may use is settled by duelAllowsStyle, where the strategy is known.
                    return true
                }

                if (!inPvpArea(pawn)) {
                    pawn.message("You can't attack players here.")
                    return false
                }

                if (!inPvpArea(target)) {
                    pawn.message("You can't attack ${target.username} there.")
                    return false
                }

                val combatLvlRange = getValidCombatLvlRange(pawn)
                if (target.combatLevel !in combatLvlRange) {
                    pawn.message("You can't attack ${target.username} - your level different is too great.")
                    return false
                }
            }
        }
        return true
    }

    private fun inPvpArea(player: Player): Boolean = player.inWilderness()

    /**
     * Which of the duel's three attack rules a strategy falls under.
     */
    private fun styleOf(strategy: CombatStrategy): DuelStyle =
        when (strategy) {
            RangedCombatStrategy -> DuelStyle.RANGED
            MagicCombatStrategy -> DuelStyle.MAGIC
            else -> DuelStyle.MELEE
        }

    private fun getValidCombatLvlRange(player: Player): IntRange {
        val wildLvl = player.tile.getWildernessLevel()
        val minLvl = Math.max(Skills.MIN_COMBAT_LVL, player.combatLevel - wildLvl)
        val maxLvl = Math.min(Skills.MAX_COMBAT_LVL, player.combatLevel + wildLvl)
        return minLvl..maxLvl
    }

    private fun getStrategy(combatClass: CombatClass): CombatStrategy =
        when (combatClass) {
            CombatClass.MELEE -> MeleeCombatStrategy
            CombatClass.RANGED -> RangedCombatStrategy
            CombatClass.MAGIC -> MagicCombatStrategy
        }

    private fun areOverlapping(
        x1: Int,
        z1: Int,
        width1: Int,
        length1: Int,
        x2: Int,
        z2: Int,
        width2: Int,
        length2: Int,
    ): Boolean {
        val a = Box(x1, z1, width1 - 1, length1 - 1)
        val b = Box(x2, z2, width2 - 1, length2 - 1)

        if (a.x1 > b.x2 || b.x1 > a.x2) {
            return false
        }

        if (a.y1 > b.y2 || b.y1 > a.y2) {
            return false
        }

        return true
    }

    /**
     * Checks to see if two AABB are bordering, but not overlapping.
     */
    fun areBordering(
        x1: Int,
        z1: Int,
        width1: Int,
        length1: Int,
        x2: Int,
        z2: Int,
        width2: Int,
        length2: Int,
    ): Boolean {
        val a = Box(x1, z1, width1 - 1, length1 - 1)
        val b = Box(x2, z2, width2 - 1, length2 - 1)

        if (b.x1 in a.x1..a.x2 && b.y1 in a.y1..a.y2 || b.x2 in a.x1..a.x2 && b.y2 in a.y1..a.y2) {
            return false
        }

        if (b.x1 > a.x2 + 1) {
            return false
        }

        if (b.x2 < a.x1 - 1) {
            return false
        }

        if (b.y1 > a.y2 + 1) {
            return false
        }

        if (b.y2 < a.y1 - 1) {
            return false
        }
        return true
    }

    data class Box(val x: Int, val y: Int, val width: Int, val length: Int) {
        val x1: Int get() = x

        val x2: Int get() = x + width

        val y1: Int get() = y

        val y2: Int get() = y + length
    }
}

/**
 * Whether [icon]'s protection prayer is actually stopping damage right now.
 *
 * The same question as [Pawn.hasPrayerIcon], plus the one thing that can suspend the answer without
 * suspending the prayer: the dragon scimitar's Sever leaves the prayer on and its icon up while
 * attacks go straight through it. Every damage formula asks this rather than the raw icon, so a
 * severed target is unprotected against melee, missiles and dragonfire alike for the five seconds.
 */
fun Pawn.protectionPrayersActive(icon: PrayerIcon): Boolean =
    hasPrayerIcon(icon) && !timers.has(PROTECTION_PRAYER_BLOCK_TIMER)
