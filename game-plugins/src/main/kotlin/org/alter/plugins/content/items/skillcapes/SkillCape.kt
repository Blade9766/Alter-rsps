package org.alter.plugins.content.items.skillcapes

import org.alter.api.Skills

/**
 * The twenty-three skillcapes, each with its trimmed variant and its hood.
 *
 * A cape is the mark of level 99 in its skill, and until now this server enforced nothing: the
 * cache carries no requirement params on any of the sixty-nine items - a probe of ids 9747-9812
 * plus the Hunter set found `reqs=[]` on every single one - so a level 3 account could wear a
 * Woodcutting cape. That is what [SkillCapePlugin] fixes, along with wiring the Boost option the
 * capes have always advertised and never had.
 *
 * ## Where the ids come from
 *
 * The rscm keys, not raw ids, and the naming is not regular - it follows the cache's own
 * abbreviations rather than the skill names. Ranged is `ranging_*`, Runecrafting is `runecraft_*`,
 * Construction is `construct_*`, and Woodcutting's *trimmed* cape alone is `woodcut_capet` while
 * its plain cape and hood spell the skill out. Each of those is a key that silently resolves to
 * nothing if guessed, so `SkillCapeVerify` checks every one against the cache by name.
 *
 * ## Boost
 *
 * Every cape but the Agility one carries a `Boost` equipment option in the cache, which raises its
 * skill by one for a while. The Agility cape genuinely has no worn option - its effect is the
 * passive run-energy restore - so [hasBoost] is false for it and nothing is bound.
 *
 * The capes that carry *further* options - the Strength cape's "Warriors' Guild", the Crafting and
 * Farming capes' "Teleport", the Magic cape's "Spellbook", the Defence cape's "Toggle Effect" - are
 * teleports and toggles into content that does not exist here yet. They are deliberately left
 * unbound rather than stubbed.
 */
enum class SkillCape(
    val skill: Int,
    val cape: String,
    val trimmed: String,
    val hood: String,
) {
    ATTACK(Skills.ATTACK, "item.attack_cape", "item.attack_capet", "item.attack_hood"),
    DEFENCE(Skills.DEFENCE, "item.defence_cape", "item.defence_capet", "item.defence_hood"),
    STRENGTH(Skills.STRENGTH, "item.strength_cape", "item.strength_capet", "item.strength_hood"),
    HITPOINTS(Skills.HITPOINTS, "item.hitpoints_cape", "item.hitpoints_capet", "item.hitpoints_hood"),
    RANGED(Skills.RANGED, "item.ranging_cape", "item.ranging_capet", "item.ranging_hood"),
    PRAYER(Skills.PRAYER, "item.prayer_cape", "item.prayer_capet", "item.prayer_hood"),
    MAGIC(Skills.MAGIC, "item.magic_cape", "item.magic_capet", "item.magic_hood"),
    COOKING(Skills.COOKING, "item.cooking_cape", "item.cooking_capet", "item.cooking_hood"),
    WOODCUTTING(Skills.WOODCUTTING, "item.woodcutting_cape", "item.woodcut_capet", "item.woodcutting_hood"),
    FLETCHING(Skills.FLETCHING, "item.fletching_cape", "item.fletching_capet", "item.fletching_hood"),
    FISHING(Skills.FISHING, "item.fishing_cape", "item.fishing_capet", "item.fishing_hood"),
    FIREMAKING(Skills.FIREMAKING, "item.firemaking_cape", "item.firemaking_capet", "item.firemaking_hood"),
    CRAFTING(Skills.CRAFTING, "item.crafting_cape", "item.crafting_capet", "item.crafting_hood"),
    SMITHING(Skills.SMITHING, "item.smithing_cape", "item.smithing_capet", "item.smithing_hood"),
    MINING(Skills.MINING, "item.mining_cape", "item.mining_capet", "item.mining_hood"),
    HERBLORE(Skills.HERBLORE, "item.herblore_cape", "item.herblore_capet", "item.herblore_hood"),

    /** The one cape with no worn option at all - see the class comment. */
    AGILITY(Skills.AGILITY, "item.agility_cape", "item.agility_capet", "item.agility_hood"),
    THIEVING(Skills.THIEVING, "item.thieving_cape", "item.thieving_capet", "item.thieving_hood"),
    SLAYER(Skills.SLAYER, "item.slayer_cape", "item.slayer_capet", "item.slayer_hood"),
    FARMING(Skills.FARMING, "item.farming_cape", "item.farming_capet", "item.farming_hood"),
    RUNECRAFTING(Skills.RUNECRAFTING, "item.runecraft_cape", "item.runecraft_capet", "item.runecraft_hood"),
    HUNTER(Skills.HUNTER, "item.hunter_cape", "item.hunter_capet", "item.hunter_hood"),
    CONSTRUCTION(Skills.CONSTRUCTION, "item.construct_cape", "item.construct_capet", "item.construct_hood"),
    ;

    /** The cape and its trimmed variant - the two items the level requirement applies to. */
    val capes: List<String> get() = listOf(cape, trimmed)

    /** Whether this cape carries the `Boost` equipment option. True for everything but Agility. */
    val hasBoost: Boolean get() = this != AGILITY

    companion object {
        val values = enumValues<SkillCape>()

        /** The level a skillcape demands in its own skill. */
        const val REQUIRED_LEVEL = 99

        /**
         * How much a Boost raises the skill by.
         *
         * One level, and one only - a skillcape boost exists to cross a single level-100 threshold,
         * not to act as a potion.
         */
        const val BOOST_LEVELS = 1
    }
}
