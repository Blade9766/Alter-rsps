package org.alter.plugins.content.areas.wilderness.bosses

import org.alter.game.model.Tile
import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.rscm.RSCM.getRSCM

/**
 * Where the Wilderness bosses stand, and what they drop.
 *
 * ## The spawn tiles are real, and checked
 *
 * Every tile in [PLACEMENTS] was confirmed walkable against the cache's own collision data with
 * `gradlew :game-server:agilityMapDump` before it was written down, so no boss spawns inside a
 * rock or on the wrong side of a wall. Scorpia's cave was found the same way: object 26762
 * ("Cavern", `Enter`) at (3231, 3951) drops into region 12961, whose floor opens up from z 10332
 * northwards - see [org.alter.plugins.content.areas.wilderness.bosses.ScorpiaCavePlugin].
 *
 * ## These are the pre-rework locations
 *
 * The 2023 update moved Callisto, Venenatis and Vet'ion into instanced caves and split each into
 * a multi-combat and a "singles-plus" variant. The cache carries the three new entrances - the
 * `Check-Fee` objects at (3115, 3676), (3183, 3744) and (3221, 3787) - but the *interiors* are not
 * identifiable from map data alone (the boss rooms hold no distinguishing static objects), and
 * there is no instancing behind a per-player fee here to put them in anyway. So the three take
 * their long-standing open-Wilderness positions, and their singles variants stand a short way off
 * in single-combat ground rather than sharing a tile with them. That is the honest approximation
 * of the multi/singles split this engine can actually make; it is not the modern layout.
 *
 * ## Drop rates
 *
 * Rarities are the wiki's numerators used as relative weights within each table, which is the
 * approximation [org.alter.plugins.content.npcs.DropRoll] is built around and which the existing
 * monster tables in `content/npcs` already make - see [WeightedDrop]'s own comment for why that
 * rescales harmlessly. [uniques] and [tertiary] are independent probability rolls instead, because
 * that is how the wiki states them ("1/256", "1/3"), not as rows competing inside a table.
 */
internal object WildernessBosses {
    /**
     * One boss placement.
     *
     * [respawnDelay] and [aggroRadius] are layered onto the combat def at spawn time rather than
     * declared through `setCombatDef`, because declaring a def would discard the wiki-sourced
     * combat stats `data/cfg/npcs/monsterStats.json` already carries for all ten of these - see
     * [WildernessBossPlugin].
     */
    data class Placement(
        val npc: String,
        val tile: Tile,
        /** Ticks between death and respawn. */
        val respawnDelay: Int,
        val aggroRadius: Int,
        val walkRadius: Int = 5,
        /** Whether the ground the boss stands on should count as multi-combat. */
        val multi: Boolean = false,
    )

    /** An independent chance roll, as the wiki publishes uniques and tertiaries. */
    data class RolledDrop(
        val item: String,
        val min: Int = 1,
        val max: Int = min,
        /** Denominator: 256 means 1/256. */
        val oneIn: Int,
    )

    data class BossDrops(
        val npc: String,
        /** Rolled every kill. */
        val always: List<WeightedDrop> = emptyList(),
        /** Independent pre-rolls, each on its own chance. */
        val uniques: List<RolledDrop> = emptyList(),
        /** Exactly one row per kill, by relative weight. */
        val main: List<WeightedDrop> = emptyList(),
        /** Independent rolls alongside everything else. */
        val tertiary: List<RolledDrop> = emptyList(),
    )

    private fun drop(
        item: String,
        min: Int = 1,
        max: Int = min,
        weight: Int,
    ) = WeightedDrop(getRSCM(item), min, max, weight)

    /**
     * The three demi-bosses publish near-identical secondary tables (bulk runes, herbs, bones and
     * supplies at the same rates), so they share one here rather than repeating it three times.
     *
     * Declared above [DROPS] on purpose: an `object`'s properties initialise top to bottom, so a
     * table declared below the list that reads it would still be null when the list is built.
     */
    private val DEMI_BOSS_MAIN_TABLE: List<WeightedDrop> =
        listOf(
            drop("item.chaos_rune", min = 900, weight = 7),
            drop("item.death_rune", min = 700, weight = 7),
            drop("item.blood_rune", min = 500, weight = 7),
            drop("item.coins_995", min = 20000, max = 60000, weight = 6),
            drop("item.dragon_bones", min = 25, weight = 5),
            drop("item.super_restore4", min = 3, weight = 5),
            drop("item.dark_crab", min = 12, weight = 5),
            drop("item.grimy_torstol", min = 15, weight = 4),
            drop("item.grimy_snapdragon", min = 15, weight = 4),
            drop("item.magic_logs", min = 100, weight = 4),
            drop("item.runite_ore", min = 15, weight = 3),
            drop("item.rune_platebody", weight = 2),
        )

    val PLACEMENTS: List<Placement> =
        listOf(
            // Forgotten Cemetery, level 24. Verified walkable across the whole 2974-2986 band.
            Placement("npc.crazy_archaeologist", Tile(2980, 3711), respawnDelay = 50, aggroRadius = 10, multi = true),
            // West of the Lava Maze, level 42.
            Placement("npc.chaos_fanatic", Tile(2985, 3851), respawnDelay = 50, aggroRadius = 10, multi = true),
            // Inside the Scorpion Pit's cavern - see ScorpiaCavePlugin for the entrance.
            Placement("npc.scorpia", Tile(3238, 10340), respawnDelay = 50, aggroRadius = 10, multi = true),
            // South-west of Rogues' Castle, level 50.
            Placement("npc.chaos_elemental_2054", Tile(3280, 3916), respawnDelay = 50, aggroRadius = 12, multi = true),
            // Callisto's den, level 41.
            Placement("npc.callisto_6609", Tile(3294, 3839), respawnDelay = 75, aggroRadius = 12, multi = true),
            Placement("npc.artio", Tile(3300, 3833), respawnDelay = 75, aggroRadius = 12),
            // The web east of the Bandit Camp, level 29.
            Placement("npc.venenatis_6610", Tile(3316, 3742), respawnDelay = 75, aggroRadius = 12, multi = true),
            Placement("npc.spindel", Tile(3320, 3736), respawnDelay = 75, aggroRadius = 12),
            // North of the Graveyard of Shadows, level 34.
            Placement("npc.vetion", Tile(3193, 3785), respawnDelay = 75, aggroRadius = 12, multi = true),
            Placement("npc.calvarion", Tile(3199, 3779), respawnDelay = 75, aggroRadius = 12),
        )

    /**
     * The two Wilderness "shard" bosses share a shape: bones, one main-table row, and an
     * independent 1/256 on each of their two shards.
     */
    val DROPS: List<BossDrops> =
        listOf(
            BossDrops(
                npc = "npc.chaos_fanatic",
                always = listOf(drop("item.bones", weight = 1)),
                uniques =
                    listOf(
                        RolledDrop("item.odium_shard_1", oneIn = 256),
                        RolledDrop("item.malediction_shard_1", oneIn = 256),
                    ),
                main =
                    listOf(
                        drop("item.coins_995", min = 499, max = 3998, weight = 18),
                        drop("item.monkfish", min = 3, weight = 8),
                        drop("item.shark", weight = 8),
                        drop("item.prayer_potion4", weight = 8),
                        drop("item.grimy_lantadyme", min = 4, weight = 8),
                        drop("item.ring_of_life", weight = 7),
                        drop("item.chaos_talisman", weight = 6),
                        drop("item.wine_of_zamorak", min = 10, weight = 6),
                        drop("item.battlestaff", min = 5, weight = 5),
                        drop("item.splitbark_body", weight = 5),
                        drop("item.splitbark_legs", weight = 5),
                        drop("item.uncut_emerald", min = 6, weight = 5),
                        drop("item.uncut_sapphire", min = 4, weight = 5),
                        drop("item.fire_rune", min = 250, weight = 4),
                        drop("item.smoke_rune", min = 30, weight = 4),
                        drop("item.chaos_rune", min = 175, weight = 4),
                        drop("item.blood_rune", min = 50, weight = 4),
                        drop("item.anchovy_pizza", min = 8, weight = 4),
                        drop("item.sinister_key", weight = 4),
                        drop("item.pure_essence", min = 250, weight = 2),
                        drop("item.ancient_staff", weight = 1),
                    ),
                tertiary = listOf(RolledDrop("item.looting_bag", oneIn = 3)),
            ),
            BossDrops(
                npc = "npc.crazy_archaeologist",
                always = listOf(drop("item.bones", weight = 1)),
                uniques =
                    listOf(
                        RolledDrop("item.odium_shard_2", oneIn = 256),
                        RolledDrop("item.malediction_shard_2", oneIn = 256),
                    ),
                main =
                    listOf(
                        drop("item.coins_995", min = 499, max = 3998, weight = 18),
                        drop("item.shark", weight = 8),
                        drop("item.prayer_potion4", weight = 8),
                        drop("item.grimy_dwarf_weed", min = 4, weight = 8),
                        drop("item.amulet_of_power", weight = 7),
                        drop("item.white_berries", min = 10, weight = 6),
                        drop("item.silver_ore", min = 40, weight = 6),
                        drop("item.rune_crossbow", weight = 5),
                        drop("item.uncut_emerald", min = 6, weight = 5),
                        drop("item.uncut_sapphire", min = 4, weight = 5),
                        drop("item.red_dragonhide", min = 10, weight = 5),
                        drop("item.red_dhide_body", weight = 4),
                        drop("item.rune_knife", min = 10, weight = 4),
                        drop("item.mud_rune", min = 30, weight = 4),
                        drop("item.cannonball", min = 150, weight = 4),
                        drop("item.rusty_sword", weight = 4),
                        drop("item.muddy_key", weight = 4),
                        drop("item.onyx_bolt_tips", min = 12, weight = 4),
                        drop("item.long_bone", weight = 2),
                        drop("item.dragon_arrow", min = 75, weight = 1),
                    ),
                tertiary =
                    listOf(
                        RolledDrop("item.looting_bag", oneIn = 3),
                        RolledDrop("item.fedora", oneIn = 128),
                    ),
            ),
            BossDrops(
                npc = "npc.scorpia",
                always = listOf(drop("item.coins_995", min = 25002, max = 34962, weight = 1)),
                uniques =
                    listOf(
                        RolledDrop("item.odium_shard_3", oneIn = 256),
                        RolledDrop("item.malediction_shard_3", oneIn = 256),
                    ),
                main =
                    listOf(
                        drop("item.death_rune", min = 100, max = 150, weight = 8),
                        drop("item.blood_rune", min = 100, max = 150, weight = 8),
                        drop("item.chaos_rune", min = 150, max = 200, weight = 8),
                        drop("item.battlestaff", min = 5, max = 8, weight = 6),
                        drop("item.uncut_diamond", min = 4, weight = 6),
                        drop("item.grimy_kwuarm", min = 4, weight = 5),
                        drop("item.grimy_torstol", min = 4, weight = 5),
                        drop("item.grimy_snapdragon", min = 4, weight = 5),
                        drop("item.runite_ore", min = 2, weight = 4),
                        drop("item.dragon_javelin_heads", min = 20, weight = 3),
                        drop("item.rune_2h_sword", weight = 2),
                        drop("item.dragon_scimitar", weight = 1),
                    ),
                tertiary =
                    listOf(
                        RolledDrop("item.looting_bag", oneIn = 3),
                        RolledDrop("item.ensouled_scorpion_head", oneIn = 18),
                    ),
            ),
            BossDrops(
                npc = "npc.chaos_elemental_2054",
                uniques = listOf(RolledDrop("item.dragon_pickaxe", oneIn = 256)),
                main =
                    listOf(
                        drop("item.coins_995", min = 2000, max = 6000, weight = 7),
                        drop("item.death_rune", min = 50, weight = 8),
                        drop("item.blood_rune", min = 50, weight = 8),
                        drop("item.dragon_bones", min = 2, weight = 5),
                        drop("item.super_restore4", weight = 5),
                        drop("item.grimy_ranarr_weed", min = 3, weight = 4),
                        drop("item.grimy_snapdragon", min = 3, weight = 4),
                        drop("item.rune_platebody", weight = 3),
                        drop("item.rune_kiteshield", weight = 3),
                        drop("item.rune_arrow", min = 150, weight = 3),
                        drop("item.rune_2h_sword", weight = 2),
                        drop("item.dragon_dagger", weight = 2),
                    ),
                tertiary =
                    listOf(
                        RolledDrop("item.looting_bag", oneIn = 3),
                        RolledDrop("item.weapon_poison", oneIn = 13),
                    ),
            ),
            // The three demi-bosses share one unique table shape: a signature item, the two dragon
            // items, a Voidwaker piece and a ring, each on its own published chance.
            BossDrops(
                npc = "npc.callisto_6609",
                always = listOf(drop("item.big_bones", weight = 1)),
                uniques =
                    listOf(
                        RolledDrop("item.claws_of_callisto", oneIn = 196),
                        RolledDrop("item.dragon_2h_sword", oneIn = 256),
                        RolledDrop("item.dragon_pickaxe", oneIn = 256),
                        RolledDrop("item.voidwaker_hilt", oneIn = 360),
                        RolledDrop("item.tyrannical_ring", oneIn = 512),
                    ),
                main = DEMI_BOSS_MAIN_TABLE,
                tertiary = listOf(RolledDrop("item.looting_bag", oneIn = 3)),
            ),
            BossDrops(
                npc = "npc.venenatis_6610",
                always = listOf(drop("item.big_bones", weight = 1)),
                uniques =
                    listOf(
                        RolledDrop("item.fangs_of_venenatis", oneIn = 196),
                        RolledDrop("item.dragon_2h_sword", oneIn = 256),
                        RolledDrop("item.dragon_pickaxe", oneIn = 256),
                        RolledDrop("item.voidwaker_gem", oneIn = 360),
                        RolledDrop("item.treasonous_ring", oneIn = 512),
                    ),
                main = DEMI_BOSS_MAIN_TABLE,
                tertiary = listOf(RolledDrop("item.looting_bag", oneIn = 3)),
            ),
            BossDrops(
                npc = "npc.vetion",
                always = listOf(drop("item.big_bones", weight = 1)),
                uniques =
                    listOf(
                        RolledDrop("item.skull_of_vetion", oneIn = 196),
                        RolledDrop("item.dragon_2h_sword", oneIn = 256),
                        RolledDrop("item.dragon_pickaxe", oneIn = 256),
                        RolledDrop("item.voidwaker_blade", oneIn = 360),
                        RolledDrop("item.ring_of_the_gods", oneIn = 512),
                    ),
                main = DEMI_BOSS_MAIN_TABLE,
                tertiary = listOf(RolledDrop("item.looting_bag", oneIn = 3)),
            ),
        )

}
