package org.alter.plugins.content.npcs.darkwizard

/**
 * Real Dark wizard combat variants and spawn locations, sourced from the OSRS Wiki's
 * Dark wizard page. See [DarkWizardConfigsPlugin] for how this is wired up.
 */
internal object DarkWizardData {
    enum class DropTier { LOW, HIGH }

    data class Variant(
        val npcKey: String,
        val combatLevel: Int,
        val hitpoints: Int,
        val attack: Int,
        val strength: Int,
        val defence: Int,
        val magic: Int,
        val dropTier: DropTier,
        /** Wiki dmagic: +3 on the level 7 and 20 variants, +5 on 11, 22 and 23. */
        val magicDefenceBonus: Int,
    )

    // Real npc ids per level variant, confirmed against this cache's own npc.rscm.
    val LEVEL_7 = listOf("npc.dark_wizard_512", "npc.dark_wizard_5086", "npc.dark_wizard_5087")
    val LEVEL_11 = listOf("npc.dark_wizard_2058", "npc.dark_wizard_2059")
    val LEVEL_20 = listOf("npc.dark_wizard", "npc.dark_wizard_5088", "npc.dark_wizard_5089")
    val LEVEL_22 = listOf("npc.dark_wizard_2057")
    val LEVEL_23 = listOf("npc.dark_wizard_2056")

    val VARIANTS: List<Variant> =
        LEVEL_7.map { Variant(it, 7, 12, 5, 2, 5, 6, DropTier.LOW, magicDefenceBonus = 3) } +
            LEVEL_11.map { Variant(it, 11, 15, 5, 5, 10, 10, DropTier.LOW, magicDefenceBonus = 5) } +
            LEVEL_20.map { Variant(it, 20, 24, 17, 17, 14, 22, DropTier.HIGH, magicDefenceBonus = 3) } +
            LEVEL_22.map { Variant(it, 22, 25, 20, 10, 15, 25, DropTier.HIGH, magicDefenceBonus = 5) } +
            LEVEL_23.map { Variant(it, 23, 25, 20, 10, 20, 25, DropTier.HIGH, magicDefenceBonus = 5) }

    // Level 6-7 Wilderness. Levels 7 and 20 both spawn here per the wiki, without
    // per-tile distinction, so the level-7 and level-20 ids are cycled evenly across
    // these tiles rather than guessing which exact tile is which level.
    val WILDERNESS_SPOTS =
        listOf(
            2976 to 3570, 2977 to 3565, 2980 to 3575, 2981 to 3562,
            2984 to 3568, 2985 to 3575, 2987 to 3569, 2988 to 3563,
        )
    val WILDERNESS_IDS = LEVEL_7 + LEVEL_20

    // Behind Draynor Village bank: level 7 only.
    val DRAYNOR_SPOTS = listOf(3084 to 3236, 3085 to 3238)
    val DRAYNOR_IDS = LEVEL_7

    // Varrock stone circle: levels 7 and 20, same caveat as Wilderness above.
    val VARROCK_SPOTS =
        listOf(
            3223 to 3367, 3223 to 3372, 3224 to 3370, 3225 to 3365, 3225 to 3374,
            3228 to 3373, 3230 to 3363, 3230 to 3365, 3230 to 3374, 3232 to 3367, 3232 to 3372,
        )
    val VARROCK_IDS = LEVEL_7 + LEVEL_20

    // Dark Wizards' Tower: levels 11, 22, 23 on all three floors, same caveat.
    val TOWER_GROUND_SPOTS = listOf(2905 to 3335, 2906 to 3334, 2907 to 3337, 2908 to 3333, 2909 to 3331, 2909 to 3334, 2910 to 3337)
    val TOWER_FIRST_SPOTS = listOf(2906 to 3333, 2906 to 3336, 2909 to 3333, 2909 to 3336)
    val TOWER_SECOND_SPOTS = listOf(2906 to 3336, 2908 to 3332, 2909 to 3334, 2910 to 3336)
    val TOWER_IDS = LEVEL_11 + LEVEL_22 + LEVEL_23

    // Kourend Castle, 2nd floor, in cages: levels 11 and 22.
    val KOUREND_CASTLE_SPOTS = listOf(1617 to 3669, 1618 to 3666)
    val KOUREND_CASTLE_IDS = LEVEL_11 + LEVEL_22
}
