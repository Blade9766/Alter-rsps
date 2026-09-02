package org.alter.plugins.content.combat.specialattack.weapons.magicbow

import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.AreaSound
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.strategy.RangedCombatStrategy
import org.alter.plugins.content.combat.strategy.ranged.RangedAmmo

/**
 * Magic bow special attacks.
 *
 * - **Snapshot** (magic shortbow, 55%; imbued, 50%) fires two arrows in quick
 *   succession, each rolled independently at 10/7 (~1.43x) accuracy. Both arrows are
 *   consumed.
 * - **Powershot** (magic longbow, 35%) fires a single arrow that is guaranteed to hit.
 *
 * Both go through [RangedCombatStrategy.shoot], so prayer bonuses, void and enchanted
 * bolt effects all still apply exactly as they do on an ordinary shot.
 */
class MagicBowPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        for ((bow, energy) in
            listOf(
                "item.magic_shortbow" to 55,
                "item.magic_shortbow_i" to 50,
            )
        ) {
            SpecialAttacks.register(bow, energy) {
                player.animate(Animation.MAGIC_SHORTBOW_SPECIAL)
                player.graphic(id = Graphic.MAGIC_SHORTBOW_SPECIAL_DRAWBACK, height = 96)
                world.spawn(AreaSound(tile = player.tile, id = SHORTBOW_SPECIAL_SOUND, radius = 10, volume = 1))

                /*
                 * Two independent shots. The second arrow's hit is delayed by a tick so
                 * the splats land in succession rather than stacking on the same frame.
                 */
                repeat(2) { shot ->
                    if (!RangedAmmo.hasAmmo(player)) {
                        return@repeat
                    }
                    val ammoDropAction = RangedAmmo.fire(player, target)
                    RangedCombatStrategy.shoot(
                        player = player,
                        target = target,
                        accuracyMultiplier = SNAPSHOT_ACCURACY,
                        hitDelay =
                            RangedCombatStrategy.getHitDelay(
                                player.getCentreTile(),
                                target.tile.transform(target.getSize() / 2, target.getSize() / 2),
                            ) + shot,
                        onHit = ammoDropAction,
                    )
                }
            }
        }

        SpecialAttacks.register("item.magic_longbow", 35) {
            player.animate(Animation.HUMAN_BOW_ATTACK)
            world.spawn(AreaSound(tile = player.tile, id = LONGBOW_SPECIAL_SOUND, radius = 10, volume = 1))

            val ammoDropAction = RangedAmmo.fire(player, target)
            RangedCombatStrategy.shoot(
                player = player,
                target = target,
                forcedLandHit = true,
                onHit = ammoDropAction,
            )
        }
    }

    private companion object {
        /** 10/7 - the wiki's "+43% accuracy" for Snapshot. */
        const val SNAPSHOT_ACCURACY = 10.0 / 7.0

        /*
         * Bow release sounds. The cache carries no embedded sound data for combat
         * sequences (see CombatConfigs.getWeaponAttackSound), so these reuse the same
         * bow-fire effect the ordinary ranged attack already plays.
         */
        const val SHORTBOW_SPECIAL_SOUND = 2700
        const val LONGBOW_SPECIAL_SOUND = 2700
    }
}
