package org.alter.plugins.content.areas.taverleydungeon.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The monsters of Taverley Dungeon.
 *
 * All 9 published pins from the OSRS Wiki Giant bat page's "Taverley Dungeon" LocLine,
 * every one level 27 - the normal aggressive variant, id 2834, not the passive Arceuus
 * one. Mapsquare 45_153 is in this project's decrypted cache, so these tiles have real
 * collision data. Combat stats and drops live in
 * `org.alter.plugins.content.npcs.critters`, shared with the Goblin Cave bats.
 *
 * They sit in a tight band, x2904-2925 at z9826-9840, which is the whole bat room rather
 * than a spread across the dungeon.
 *
 * **The deep half is gated.** The black demons, hellhounds and poison spiders in
 * mapsquares 11416/11417 sit behind the dusty key door, in
 * `areas/taverleydungeon/objs/DustyKeyDoorPlugin`. Until that gate existed the only thing
 * between a new account and a level 172 black demon was a walk.
 *
 * Beyond the bats this now holds fifteen more monster types, every one at its own published
 * pins from its own wiki page - the dungeon's monster table names them but carries no
 * coordinates, so each came from the monster's side rather than the dungeon's.
 *
 * **Four of its residents have stats in `content/npcs/dungeon` but are still not placed,
 * because the wiki publishes no coordinates for them here.** The baby blue and baby black
 * dragon both have a Taverley LocLine with zero pins on it; the dwarf has no Taverley
 * LocLine at all; and the suit of armour's page carries no LocLines whatsoever. Inventing
 * tiles for them would be the one thing this file has avoided throughout.
 *
 * **The monks of Zamorak are absent for a different reason**: the wiki does publish six pins
 * for them here, but they are magic attackers and `content/npcs/dungeon` deliberately does
 * not define them, so spawning them would put six 10-hitpoint placeholders in the dungeon.
 * They go in the day an NPC magic path exists.
 */
class SpawnPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
    init {
        // The bat chamber (9).
        spawnNpc(npc = "npc.giant_bat", x = 2904, z = 9832, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 2908, z = 9830, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat", x = 2909, z = 9837, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_bat", x = 2914, z = 9828, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.giant_bat", x = 2914, z = 9840, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npc.giant_bat", x = 2919, z = 9831, walkRadius = 8, direction = Direction.EAST)
        spawnNpc(npc = "npc.giant_bat", x = 2921, z = 9826, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.giant_bat", x = 2922, z = 9834, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npc.giant_bat", x = 2925, z = 9830, walkRadius = 8, direction = Direction.NORTH)

        // Black Knights (43), level 33.
        spawnNpc(npc = "npc.black_knight_517", x = 2891, z = 9693, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.black_knight_4331", x = 2891, z = 9701, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.black_knight_11953", x = 2892, z = 9678, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.black_knight", x = 2892, z = 9682, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.black_knight_11952", x = 2892, z = 9696, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.black_knight_517", x = 2892, z = 9710, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.black_knight_4331", x = 2893, z = 9702, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.black_knight_11953", x = 2895, z = 9711, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.black_knight", x = 2897, z = 9675, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.black_knight_11952", x = 2898, z = 9688, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.black_knight_517", x = 2899, z = 9682, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.black_knight_4331", x = 2899, z = 9705, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.black_knight_11953", x = 2899, z = 9709, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.black_knight", x = 2900, z = 9678, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.black_knight_11952", x = 2900, z = 9701, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.black_knight_517", x = 2901, z = 9711, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.black_knight_4331", x = 2902, z = 9693, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.black_knight_11953", x = 2904, z = 9690, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.black_knight", x = 2905, z = 9692, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.black_knight_11952", x = 2906, z = 9673, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.black_knight_517", x = 2906, z = 9687, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.black_knight_4331", x = 2906, z = 9706, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.black_knight_11953", x = 2907, z = 9676, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.black_knight", x = 2907, z = 9711, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.black_knight_11952", x = 2908, z = 9694, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.black_knight_517", x = 2908, z = 9702, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.black_knight_4331", x = 2910, z = 9691, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.black_knight_11953", x = 2911, z = 9677, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.black_knight", x = 2912, z = 9682, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.black_knight_11952", x = 2912, z = 9688, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.black_knight_517", x = 2913, z = 9694, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.black_knight_4331", x = 2915, z = 9703, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.black_knight_11953", x = 2915, z = 9710, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.black_knight", x = 2916, z = 9677, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.black_knight_11952", x = 2916, z = 9692, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.black_knight_517", x = 2917, z = 9682, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.black_knight_4331", x = 2917, z = 9695, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.black_knight_11953", x = 2919, z = 9679, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.black_knight", x = 2920, z = 9703, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.black_knight_11952", x = 2921, z = 9708, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.black_knight_517", x = 2923, z = 9710, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.black_knight_4331", x = 2939, z = 9812, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.black_knight_11953", x = 2943, z = 9801, walkRadius = 6, direction = Direction.SOUTH)

        // Black demons (24), level 172.
        spawnNpc(npc = "npc.black_demon", x = 2826, z = 9791, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.black_demon_2048", x = 2831, z = 9790, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.black_demon_2049", x = 2833, z = 9795, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.black_demon_2050", x = 2837, z = 9787, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.black_demon_2051", x = 2837, z = 9797, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.black_demon_2052", x = 2840, z = 9792, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.black_demon_5874", x = 2842, z = 9786, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.black_demon_5875", x = 2842, z = 9801, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.black_demon_5876", x = 2845, z = 9795, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.black_demon_5877", x = 2847, z = 9784, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.black_demon", x = 2849, z = 9779, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.black_demon_2048", x = 2849, z = 9791, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.black_demon_2049", x = 2853, z = 9786, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.black_demon_2050", x = 2854, z = 9776, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.black_demon_2051", x = 2854, z = 9781, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.black_demon_2052", x = 2858, z = 9772, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.black_demon_5874", x = 2860, z = 9763, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.black_demon_5875", x = 2860, z = 9781, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.black_demon_5876", x = 2862, z = 9776, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.black_demon_5877", x = 2863, z = 9769, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.black_demon", x = 2866, z = 9783, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.black_demon_2048", x = 2869, z = 9776, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.black_demon_2049", x = 2871, z = 9783, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.black_demon_2050", x = 2875, z = 9776, walkRadius = 6, direction = Direction.WEST)

        // Ghosts (14), level 19.
        spawnNpc(npc = "npc.ghost", x = 2888, z = 9849, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.ghost_86", x = 2894, z = 9849, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.ghost_87", x = 2900, z = 9819, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.ghost_88", x = 2901, z = 9848, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.ghost_89", x = 2905, z = 9851, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.ghost_90", x = 2907, z = 9818, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.ghost_91", x = 2909, z = 9848, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.ghost_92", x = 2912, z = 9820, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.ghost_93", x = 2915, z = 9851, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.ghost_95", x = 2920, z = 9818, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.ghost_97", x = 2920, z = 9848, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.ghost_99", x = 2933, z = 9838, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.ghost_472", x = 2936, z = 9829, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.ghost_473", x = 2938, z = 9836, walkRadius = 6, direction = Direction.EAST)

        // Hellhounds are spawned by `content/npcs/hellhound`, at the wiki's own thirteen pins for
        // this dungeon rather than the hand-picked ones that used to be here. Respawning
        // them here would double them.

        // Poison spiders (8), level 64.
        spawnNpc(npc = "npc.poison_spider", x = 2850, z = 9808, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.poison_spider_11999", x = 2857, z = 9822, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.poison_spider", x = 2859, z = 9802, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.poison_spider_11999", x = 2859, z = 9814, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.poison_spider", x = 2864, z = 9819, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.poison_spider_11999", x = 2870, z = 9799, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.poison_spider", x = 2871, z = 9792, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.poison_spider_11999", x = 2876, z = 9806, walkRadius = 6, direction = Direction.WEST)

        // Magic axes (7), level 42.
        spawnNpc(npc = "npc.magic_axe", x = 2955, z = 9775, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.magic_axe_7269", x = 2955, z = 9795, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.magic_axe", x = 2956, z = 9790, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.magic_axe_7269", x = 2957, z = 9780, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.magic_axe", x = 2962, z = 9791, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.magic_axe_7269", x = 2966, z = 9775, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.magic_axe", x = 2966, z = 9787, walkRadius = 6, direction = Direction.SOUTH)

        // Skeletons (7), levels 22 and 25 cycled evenly - the wiki lists them together.
        spawnNpc(npc = "npc.skeleton", x = 2884, z = 9836, walkRadius = 6, direction = Direction.NORTH)  // level 22
        spawnNpc(npc = "npc.skeleton_77", x = 2885, z = 9819, walkRadius = 6, direction = Direction.EAST)  // level 25
        spawnNpc(npc = "npc.skeleton_71", x = 2885, z = 9823, walkRadius = 6, direction = Direction.SOUTH)  // level 22
        spawnNpc(npc = "npc.skeleton_78", x = 2886, z = 9812, walkRadius = 6, direction = Direction.WEST)  // level 25
        spawnNpc(npc = "npc.skeleton_72", x = 2886, z = 9816, walkRadius = 6, direction = Direction.NORTH)  // level 22
        spawnNpc(npc = "npc.skeleton_79", x = 2886, z = 9825, walkRadius = 6, direction = Direction.EAST)  // level 25
        spawnNpc(npc = "npc.skeleton_73", x = 2887, z = 9821, walkRadius = 6, direction = Direction.SOUTH)  // level 22

        // Chaos druids (6), level 13.
        spawnNpc(npc = "npc.chaos_druid", x = 2929, z = 9848, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.chaos_druid", x = 2931, z = 9846, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.chaos_druid", x = 2932, z = 9852, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.chaos_druid", x = 2936, z = 9846, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.chaos_druid", x = 2936, z = 9852, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.chaos_druid", x = 2937, z = 9849, walkRadius = 6, direction = Direction.EAST)

        // Chaos dwarves (6), level 48.
        spawnNpc(npc = "npc.chaos_dwarf", x = 2915, z = 9760, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.chaos_dwarf", x = 2922, z = 9757, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.chaos_dwarf", x = 2925, z = 9769, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.chaos_dwarf", x = 2928, z = 9761, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.chaos_dwarf", x = 2932, z = 9784, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.chaos_dwarf", x = 2938, z = 9788, walkRadius = 6, direction = Direction.EAST)

        // Hill giants are NOT spawned here any more. Taverley's five used to be the entire hill
        // giant population of the server; they now live with the other twelve published
        // locations in content/npcs/dungeon/HillGiantSpawns, on these same five tiles. Adding
        // them back here would double them.

        // Lesser demons are spawned by `content/npcs/demon`, at the wiki's own five pins for this
        // dungeon. Respawning them here would double them.

        // Poison scorpions (5), level 20.
        spawnNpc(npc = "npc.poison_scorpion", x = 2929, z = 9752, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.poison_scorpion", x = 2934, z = 9775, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.poison_scorpion", x = 2935, z = 9758, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.poison_scorpion", x = 2935, z = 9769, walkRadius = 6, direction = Direction.WEST)
        spawnNpc(npc = "npc.poison_scorpion", x = 2941, z = 9779, walkRadius = 6, direction = Direction.NORTH)

        // Rats (4), level 1.
        spawnNpc(npc = "npc.rat_2854", x = 2930, z = 9701, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.rat_2855", x = 2932, z = 9687, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.rat_2854", x = 2932, z = 9692, walkRadius = 6, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.rat_2855", x = 2932, z = 9697, walkRadius = 6, direction = Direction.WEST)

        // Spiders (3), level 1.
        spawnNpc(npc = "npc.spider_3019", x = 2891, z = 9834, walkRadius = 6, direction = Direction.NORTH)
        spawnNpc(npc = "npc.spider_3019", x = 2893, z = 9828, walkRadius = 6, direction = Direction.EAST)
        spawnNpc(npc = "npc.spider_3019", x = 2896, z = 9833, walkRadius = 6, direction = Direction.SOUTH)

        // The jailer (1), level 47.
        spawnNpc(npc = "npc.jailer", x = 2933, z = 9693, walkRadius = 6, direction = Direction.NORTH)
    }
}
