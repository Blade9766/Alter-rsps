package org.alter.plugins.content.combat.specialattack.weapons.runethrownaxe

import org.alter.api.ProjectileType
import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.combat.PawnHit
import org.alter.game.model.entity.AreaSound
import org.alter.game.model.entity.Pawn
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.createProjectile
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.strategy.RangedCombatStrategy
import org.alter.plugins.content.combat.strategy.ranged.RangedAmmo
import org.alter.plugins.content.combat.strategy.ranged.RangedAoe
import org.alter.plugins.content.interfaces.attack.AttackTab

/**
 * Rune thrownaxe - **Chainhit** (10% per target, up to 50%).
 *
 * One axe is thrown and ricochets between up to five targets. Every mechanic here comes
 * from https://oldschool.runescape.wiki/w/Rune_thrownaxe, and several of them are
 * counter-intuitive:
 *
 * - **The bounce targets are picked once, up front**, from the 7x7 (3-tile radius)
 *   centred on the *original* target - the axe does not cascade outward from each
 *   successive victim - and they are taken in **random order**.
 * - **The axe never rebounds to the original target.**
 * - **An accuracy roll is made after each target and a failed roll ends the sequence.**
 *   So the chain is resolved sequentially, not fanned out at once, and a miss anywhere
 *   stops it dead.
 * - **Damage ignores offensive prayers** - the wiki has it using only the visible Ranged
 *   level and the thrownaxe's own ranged strength bonus, so Eagle Eye and Rigour do
 *   nothing for it. That is [RangedCombatStrategy.shoot]'s `ignoreOffensivePrayers`.
 *
 * **Energy is charged per target, which `SpecialAttacks` cannot express** - it deducts
 * one fixed cost up front. So this registers at the cost of a single hit and charges
 * each additional bounce itself, stopping when the player can no longer pay. That also
 * means a player with 10% energy gets exactly one hit, which is the correct behaviour.
 *
 * **Deliberate deviation:** the real chain only happens in a multi-combat area. There is
 * no engine-level multi-combat enforcement in this codebase, so it always chains;
 * [RangedAoe] keeps it to npcs so it cannot ricochet through bystanding players.
 */
class RuneThrownaxePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.register("item.rune_thrownaxe", ENERGY_PER_TARGET) {
            player.animate(Animation.RUNE_THROWNAXE_SPECIAL)
            player.graphic(id = Graphic.RUNE_THROWNAXE_SPECIAL_DRAWBACK, height = 96)
            world.spawn(AreaSound(tile = player.tile, id = THROWN_SOUND, radius = 10, volume = 1))

            /*
             * One axe leaves the player's hand for the whole special. Its own projectile
             * is suppressed so each leg of the ricochet can be drawn separately below.
             */
            val ammoDropAction = RangedAmmo.fire(player, target, spawnProjectile = false)

            /*
             * Candidates are gathered once, from around the original target, and
             * shuffled - the wiki specifies a random order excluding the original.
             */
            val bounceTargets =
                RangedAoe
                    .targetsAround(player, target, radius = CHAIN_RADIUS, max = CANDIDATE_LIMIT)
                    .drop(1)
                    .shuffled()
                    .take(MAX_TARGETS - 1)

            var origin: Pawn = player
            var hitDelay = 0

            for ((index, victim) in (listOf(target) + bounceTargets).withIndex()) {
                /*
                 * The registered cost already paid for the first target; every bounce
                 * after it is charged here, and the chain stops when it can't be paid.
                 */
                if (index > 0) {
                    val energy = AttackTab.getEnergy(player)
                    if (energy < ENERGY_PER_TARGET) {
                        break
                    }
                    AttackTab.setEnergy(player, energy - ENERGY_PER_TARGET)
                }

                val victimCentre = victim.tile.transform(victim.getSize() / 2, victim.getSize() / 2)
                hitDelay += RangedCombatStrategy.getHitDelay(origin.getCentreTile(), victimCentre)
                world.spawn(origin.createProjectile(victim, Graphic.RUNE_THROWNAXE_SPECIAL_PROJECTILE, ProjectileType.THROWN))

                val hit =
                    RangedCombatStrategy.shoot(
                        player = player,
                        target = victim,
                        hitDelay = hitDelay,
                        ignoreOffensivePrayers = true,
                        onHit = if (index == 0) ammoDropAction else NO_DROP,
                    )

                /*
                 * A missed roll ends the sequence - the axe stops bouncing.
                 */
                if (!hit.landed) {
                    break
                }
                origin = victim
            }
        }
    }

    private companion object {
        /** 10% per target hit, so five targets is the documented 50% maximum. */
        const val ENERGY_PER_TARGET = 10
        const val MAX_TARGETS = 5

        /** 3-tile radius, i.e. the 7x7 centred on the original target. */
        const val CHAIN_RADIUS = 3

        /**
         * Bound on how many nearby npcs are considered before shuffling. Only
         * [MAX_TARGETS] - 1 are ever used; this just keeps the shuffle cheap in a crowd.
         */
        const val CANDIDATE_LIMIT = 32

        /** Only the first target's hit drops the single spent axe. */
        val NO_DROP: (PawnHit).() -> Unit = {}

        /** Reuses the ordinary thrown-weapon effect - see CombatConfigs.getWeaponAttackSound. */
        const val THROWN_SOUND = 2696
    }
}
