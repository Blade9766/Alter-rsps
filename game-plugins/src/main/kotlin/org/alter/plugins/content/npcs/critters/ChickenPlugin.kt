package org.alter.plugins.content.npcs.critters

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * Puts the chickens in the world. Tiles, ids and the reasoning behind both live in
 * [ChickenSpawns]; this file is the wiring, and it is deliberately nothing but wiring.
 *
 * Combat stats, animations, immunities and the drop table are **not** here - they are in
 * [Critters] and [CritterPlugin], which have carried a correct, wiki-matching chicken since
 * they were written. The only thing missing was a chicken to apply them to.
 *
 * Two things this does not do, flagged rather than faked:
 *
 * - **Chickens do not flee.** The wiki says they "will sometimes try to retreat when they
 *   have 1 hitpoint left"; this engine has no notion of an npc breaking off a fight, in
 *   [org.alter.game.model.entity.Npc] or anywhere in the combat plugins, so there is nothing
 *   to hang it on. Adding one would be a combat-engine change, not a chicken change.
 * - **No Key (medium) drop.** Same reason [Critters] gives: it is conditional on a medium
 *   clue step that does not exist here, so it would have to drop unconditionally or never.
 */
class ChickenPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        ChickenSpawns.FLOCKS.forEach { flock ->
            flock.tiles.forEachIndexed { index, (x, z) ->
                spawnNpc(
                    npc = flock.npcKeys[index % flock.npcKeys.size],
                    x = x,
                    z = z,
                    height = flock.height,
                    walkRadius = flock.walkRadius,
                    direction = FACINGS[index % FACINGS.size],
                )
            }
        }
    }

    private companion object {
        /**
         * Dealt round the flock so a coop is not thirty birds all staring the same way. Which
         * way any one chicken faces is not published and does not matter - it is overwritten
         * the moment the bird walks or is attacked.
         */
        val FACINGS = listOf(Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST)
    }
}
