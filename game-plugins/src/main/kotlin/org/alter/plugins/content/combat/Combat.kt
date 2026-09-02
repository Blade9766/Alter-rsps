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
import org.alter.plugins.content.combat.strategy.CombatStrategy
import org.alter.plugins.content.combat.strategy.MagicCombatStrategy
import org.alter.plugins.content.combat.strategy.MeleeCombatStrategy
import org.alter.plugins.content.combat.strategy.RangedCombatStrategy
import org.alter.plugins.content.combat.strategy.magic.CombatSpell
import org.alter.plugins.content.interfaces.attack.AttackTab
import java.lang.ref.WeakReference

/**
 * @author Tom <rspsmods@gmail.com>
 */
object Combat {
    val CASTING_SPELL = AttributeKey<CombatSpell>()
    val DAMAGE_DEAL_MULTIPLIER = AttributeKey<Double>()
    val DAMAGE_TAKE_MULTIPLIER = AttributeKey<Double>()

    /**
     * Set by a special attack that grants an immediate follow-up attack rather than
     * waiting out the weapon's speed - the dragon thrownaxe's Momentum Throw. Consumed
     * by [postAttack], which runs after the special resolves and would otherwise
     * overwrite any attack delay the special set for itself.
     */
    val INSTANT_NEXT_ATTACK = AttributeKey<Boolean>()

    /**
     * Set (to the pending defensive flag) while the player has clicked
     * Autocast/Defensive Autocast and the spellbook is showing waiting for them to pick
     * a spell - see [org.alter.plugins.content.interfaces.gameframe.tabs.combat_options.AttackTabPlugin]
     * (sets it, opens the spellbook) and
     * [org.alter.plugins.content.combat.strategy.magic.CombatSpellsPlugin] (consumes it
     * on the next spell button click instead of casting that spell immediately).
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
    ): Boolean = canEngage(pawn, target) && getStrategy(combatClass).canAttack(pawn, target)

    fun canAttack(
        pawn: Pawn,
        target: Pawn,
        strategy: CombatStrategy,
    ): Boolean = canEngage(pawn, target) && strategy.canAttack(pawn, target)

    fun isAttackDelayReady(pawn: Pawn): Boolean = !pawn.timers.has(ATTACK_DELAY)

    fun postAttack(
        pawn: Pawn,
        target: Pawn,
    ) {
        pawn.timers[ATTACK_DELAY] =
            if (pawn.attr[INSTANT_NEXT_ATTACK] == true) {
                pawn.attr.remove(INSTANT_NEXT_ATTACK)
                1
            } else {
                CombatConfigs.getAttackDelay(pawn)
            }
        target.timers[ACTIVE_COMBAT_TIMER] = 17 // 10,2 seconds

        pawn.attr[LAST_HIT_ATTR] = WeakReference(target)
        target.attr[LAST_HIT_BY_ATTR] = WeakReference(pawn)

        if (pawn.attr.has(CASTING_SPELL) && pawn is Player && pawn.getVarbit(SELECTED_AUTOCAST_VARBIT) == 0) {
            reset(pawn)
            pawn.attr.remove(CASTING_SPELL)
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
    ): Boolean {
        val world = pawn.world
        val start = pawn.tile
        val end = target.tile

        return start.isWithinRadius(end, distance) && world.lineValidator.rayCast(start, end, projectile = projectile)
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
        val world = pawn.world
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
        val withinRange = touching && world.lineValidator.rayCast(start, end, projectile = projectile)
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
        } else if (target is Player) {
            if (!target.isOnline || target.invisible) {
                return false
            }

            if (!target.lock.canBeAttacked()) {
                return false
            }

            if (pvp) {
                pawn as Player

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
