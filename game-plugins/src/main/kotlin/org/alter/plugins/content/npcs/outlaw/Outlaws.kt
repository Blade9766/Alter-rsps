package org.alter.plugins.content.npcs.outlaw

import org.alter.plugins.content.npcs.MonsterDropTable
import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.rscm.RSCM.getRSCM

/**
 * The outlaws west of the Grand Exchange - ten ids, one location, ten pins.
 *
 * See [OutlawPlugin] for the wiring and everything else. Stats come from
 * `data/cfg/npcs/monsterStats.json`, including the **-21 strength bonus** that is the whole
 * character of the monster: an outlaw is level 32 and hits 3.
 *
 * ## They were parrying when they meant to swing
 *
 * The animation resolver had attack and block backwards: `HUMAN_SLASH_SWORD_ATTACK` (390) was being
 * played as the block and `HUMAN_SLASH_SWORD_DEFEND` (388) as the attack. That is the same
 * armed-human swap the `BANDIT` entry in `npc-animations/README.md` records, on a tenth name.
 *
 * ## The respawn really is two ticks
 *
 * `respawn = 2`, the shortest in this whole bestiary pass by a factor of ten, and it is not a typo
 * on the page - these are the Grand Exchange's permanent scenery and are meant to be back before you
 * have walked away. It is taken as published.
 */
internal object Outlaws {
    private fun drop(
        item: String,
        min: Int = 1,
        max: Int = min,
        weight: Int,
    ) = WeightedDrop(getRSCM(item), min, max, weight)

    private fun coins(
        min: Int,
        weight: Int,
    ) = WeightedDrop(getRSCM("item.coins_995"), min, min, weight)

    /** `id1`..`id10`, all one version at level 32 - they differ only in appearance. */
    val NPC_KEYS =
        listOf(
            "npc.outlaw", "npc.outlaw_4168", "npc.outlaw_4169", "npc.outlaw_4170", "npc.outlaw_4171",
            "npc.outlaw_4172", "npc.outlaw_4173", "npc.outlaw_4174", "npc.outlaw_4175", "npc.outlaw_4176",
        )

    const val COMBAT_LEVEL = 32

    /** Wiki `respawn = 2`. See the file doc. */
    const val RESPAWN_CYCLES = 2

    const val WALK_RADIUS = 4

    /**
     * The outlaw table.
     *
     * ## Two readings, and what each comes to
     *
     * The `Coins` section's largest row - 10 coins at 32/128 - is marked `{{(f)}}` with the footnote
     * "only dropped in free-to-play". Counting it the table comes to **159**; dropping it, to
     * **127**. That is the members reading [MonsterDropTable] documents, and it lands one slot short
     * rather than exactly - the only table in this tree that does.
     *
     * The one unaccounted slot is written as a `Nothing` row rather than being absorbed into the
     * rows, for the reason [Frogs][org.alter.plugins.content.npcs.frog.Frogs] gives about its own
     * gap: rescaling 127 rows to fill 128 slots would make every published rate very slightly more
     * common than the page says, and an explicit row keeps all of them exact while still letting
     * `BestiaryVerify` check the arithmetic.
     *
     * **`Rat's paper` is not modelled** even though it is published `Always`: its footnote makes it
     * a What Lies Below drop, and that quest does not exist here.
     */
    val TABLE =
        MonsterDropTable(
            denominator = 128,
            herbWeight = 32,
            rows =
                listOf(
                    // Armour - 2.
                    drop("item.bronze_med_helm", weight = 2),
                    // Runes - 6.
                    drop("item.mind_rune", 9, weight = 2),
                    drop("item.water_rune", 6, weight = 2),
                    drop("item.earth_rune", 5, weight = 2),
                    // Coins - 17. The free-to-play-only 10-coin row at 32/128 is absent.
                    coins(5, weight = 12),
                    coins(15, weight = 4),
                    coins(25, weight = 1),
                    // Other - 70.
                    drop("item.rope", weight = 46),
                    drop("item.fishing_bait", weight = 15),
                    drop("item.cabbage", weight = 6),
                    drop("item.copper_ore", weight = 2),
                    drop("item.knife", weight = 1),
                    // The one slot the members reading leaves over; see the doc above.
                    WeightedDrop(item = null, weight = 1),
                ),
        )

    /** The one published `LocLine`, plane 0 - ten outlaws in the alley west of the Grand Exchange. */
    val TILES: List<Pair<Int, Int>> =
        listOf(
            3116 to 3473, 3117 to 3477, 3118 to 3470, 3118 to 3474, 3119 to 3472,
            3119 to 3477, 3121 to 3471, 3123 to 3473, 3123 to 3477, 3124 to 3476,
        )
}
