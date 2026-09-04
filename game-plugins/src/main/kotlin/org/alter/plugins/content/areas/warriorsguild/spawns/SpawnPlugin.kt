package org.alter.plugins.content.areas.warriorsguild.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The Warriors' Guild staff, on their own tiles.
 *
 * Every coordinate here is the `{{Map}}` template from that npc's wiki page - the same sourcing
 * the rest of this project's area spawns use. Two are worth calling out:
 *
 * - **Ghommal is outside**, at (2879, 3547), beside the front door at (2877, 3546) rather than
 *   behind it. He is the guard, so he has to be reachable by someone who cannot get in.
 * - **Kamfreena stands on plane 2 at (2844, 3540)**, just west of her own doors at (2847, 3540)
 *   and (2847, 3541) - outside the gated room, which is why
 *   [org.alter.plugins.content.areas.warriorsguild.WarriorsGuild.TOP_FLOOR_CYCLOPS] starts at
 *   x 2848 and not at her.
 *
 * The cyclopes and the animated armour are not here. Monsters live with the plugin that defines
 * their combat, exactly as the slayer and dungeon packages do - see `CyclopsPlugin` and
 * `activities/AnimationRoomPlugin`.
 *
 * **Laidee Gnonock is not spawned.** She is the alternative door guard, an either/or with Ghommal
 * rather than a second npc standing next to him, and her wiki page carries no map pin of her own.
 * Her dialogue is bound in `npcs/GuildNpcPlugin` so that placing her later is one line.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        // Outside the front door.
        spawnNpc(npc = "npc.ghommal_13613", x = 2879, z = 3547, height = 0, walkRadius = 1, direction = Direction.WEST)

        // Ground floor: the guildmaster, the two room hosts and the potion seller.
        spawnNpc(npc = "npc.harrallak_menarous_13615", x = 2866, z = 3547, height = 0, walkRadius = 3, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.shanomi", x = 2857, z = 3542, height = 0, walkRadius = 2, direction = Direction.NORTH)
        spawnNpc(npc = "npc.ajjat", x = 2851, z = 3553, height = 0, walkRadius = 2, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.lilly", x = 2846, z = 3552, height = 0, walkRadius = 1, direction = Direction.EAST)

        // First floor: the catapult engineer and the keg challenge.
        spawnNpc(npc = "npc.gamfred", x = 2841, z = 3541, height = 1, walkRadius = 2, direction = Direction.NORTH)
        spawnNpc(npc = "npc.jimmy", x = 2872, z = 3540, height = 1, walkRadius = 2, direction = Direction.WEST)

        // Top floor and basement: the two cyclops wardens.
        spawnNpc(npc = "npc.kamfreena", x = 2844, z = 3540, height = 2, walkRadius = 1, direction = Direction.EAST)
        spawnNpc(npc = "npc.lorelai", x = 2909, z = 9972, height = 0, walkRadius = 1, direction = Direction.EAST)
    }
}
