package org.alter.plugins.content.items.jewellery

/**
 * The seven jewellery enchantment spells, and every unenchanted piece each one converts.
 *
 * Level requirement, rune cost and spellbook component are **not** listed here - they are read out
 * of each spell's own cache params at load time by
 * [org.alter.plugins.content.magic.MagicSpells.loadSpell], the same params that already drive every
 * combat spell in the project. Only the two things the cache does not hold are written down: the
 * Magic experience, and which item turns into which.
 *
 * [spellItem] is the cache item whose params carry the spell (there is no obtainable item behind
 * it; the spellbook is built out of these). The ids were read out of this project's own cache by
 * scanning every item def for a spell-name param containing "Enchant", and `JewelleryVerify` asserts
 * each one still carries the name recorded in [spellName] so a cache update cannot silently
 * repoint a spell.
 *
 * The conversions come from the wiki's enchantment tables, cross-checked against the rune costs and
 * level requirements the cache reports for each spell - those agree exactly, which is the strongest
 * available confirmation that the cache and the wiki are describing the same seven spells.
 *
 * @see <a href="https://oldschool.runescape.wiki/w/Enchant">Enchant - OSRS Wiki</a>
 */
enum class EnchantSpell(
    val spellItem: Int,
    val spellName: String,
    val gems: String,
    val graphic: Int,
    val xp: Double,
    val conversions: Map<String, String>,
) {
    LVL_1(
        spellItem = 3276,
        spellName = "Lvl-1 Enchant",
        gems = "sapphire or opal",
        graphic = 114,
        xp = 17.5,
        conversions =
            mapOf(
                "item.sapphire_ring" to "item.ring_of_recoil",
                "item.sapphire_necklace" to "item.games_necklace8",
                "item.sapphire_bracelet" to "item.bracelet_of_clay",
                "item.sapphire_amulet" to "item.amulet_of_magic",
                "item.opal_ring" to "item.ring_of_pursuit",
                "item.opal_necklace" to "item.dodgy_necklace",
                "item.opal_bracelet" to "item.expeditious_bracelet",
                "item.opal_amulet" to "item.amulet_of_bounty",
            ),
    ),

    LVL_2(
        spellItem = 3287,
        spellName = "Lvl-2 Enchant",
        gems = "emerald or jade",
        graphic = 115,
        xp = 37.0,
        conversions =
            mapOf(
                "item.emerald_ring" to "item.ring_of_dueling8",
                "item.emerald_necklace" to "item.binding_necklace",
                "item.emerald_bracelet" to "item.castle_wars_bracelet3",
                "item.emerald_amulet" to "item.amulet_of_defence",
                "item.jade_ring" to "item.ring_of_returning5",
                "item.jade_necklace" to "item.necklace_of_passage5",
                "item.jade_bracelet" to "item.flamtaer_bracelet",
                "item.jade_amulet" to "item.amulet_of_chemistry",
                // Not an emerald amulet: the amulet of nature is enchanted from a pre-nature
                // amulet, an emerald amulet (u) strung with magic string.
                "item.prenature_amulet" to "item.amulet_of_nature",
            ),
    ),

    LVL_3(
        spellItem = 3298,
        spellName = "Lvl-3 Enchant",
        gems = "ruby or topaz",
        graphic = 116,
        xp = 59.0,
        conversions =
            mapOf(
                "item.ruby_ring" to "item.ring_of_forging",
                "item.ruby_necklace" to "item.digsite_pendant_5",
                "item.ruby_bracelet" to "item.inoculation_bracelet",
                "item.ruby_amulet" to "item.amulet_of_strength",
                "item.topaz_ring" to "item.efaritays_aid",
                "item.topaz_necklace" to "item.necklace_of_faith",
                "item.topaz_bracelet" to "item.bracelet_of_slaughter",
                "item.topaz_amulet" to "item.burning_amulet5",
            ),
    ),

    LVL_4(
        spellItem = 3305,
        spellName = "Lvl-4 Enchant",
        gems = "diamond",
        graphic = 153,
        xp = 67.0,
        conversions =
            mapOf(
                "item.diamond_ring" to "item.ring_of_life",
                "item.diamond_necklace" to "item.phoenix_necklace",
                "item.diamond_bracelet" to "item.abyssal_bracelet5",
                "item.diamond_amulet" to "item.amulet_of_power",
            ),
    ),

    /**
     * The one enchant that does *not* produce a charged item: dragonstone jewellery enchants to the
     * uncharged glory / skills necklace / combat bracelet / ring of wealth, which then have to be
     * charged separately at a fountain. See [ChargedJewellery].
     */
    LVL_5(
        spellItem = 3318,
        spellName = "Lvl-5 Enchant",
        gems = "dragonstone",
        graphic = 154,
        xp = 78.0,
        conversions =
            mapOf(
                "item.dragonstone_ring" to "item.ring_of_wealth",
                // The dragonstone necklace is named "Dragon necklace" in game and in the cache.
                "item.dragon_necklace" to "item.skills_necklace",
                "item.dragonstone_bracelet" to "item.combat_bracelet",
                "item.dragonstone_amulet" to "item.amulet_of_glory",
            ),
    ),

    LVL_6(
        spellItem = 6567,
        spellName = "Lvl-6 Enchant",
        gems = "onyx",
        graphic = 452,
        xp = 97.0,
        conversions =
            mapOf(
                "item.onyx_ring" to "item.ring_of_stone",
                "item.onyx_necklace" to "item.berserker_necklace",
                "item.onyx_bracelet" to "item.regen_bracelet",
                "item.onyx_amulet" to "item.amulet_of_fury",
            ),
    ),

    LVL_7(
        spellItem = 19475,
        spellName = "Lvl-7 Enchant",
        gems = "zenyte",
        // UNVERIFIED. RuneLite names spot animations for Lvl-1 through Lvl-6 only
        // (ENCHANT_AMULET_LVL1..3 = 114/115/116, ENCHANT_AMULET2_LVL4..6 = 153/154/452); zenyte
        // enchanting arrived years later and no source publishes its id, so the Lvl-6 graphic is
        // reused. Purely cosmetic - nothing else about the spell depends on it.
        graphic = 452,
        xp = 110.0,
        conversions =
            mapOf(
                "item.zenyte_ring" to "item.ring_of_suffering",
                "item.zenyte_necklace" to "item.necklace_of_anguish",
                "item.zenyte_bracelet" to "item.tormented_bracelet",
                "item.zenyte_amulet" to "item.amulet_of_torture",
            ),
    ),
    ;

    companion object {
        /**
         * The player animation for casting any jewellery enchant - RuneLite's
         * `AnimationID.HUMAN_CAST_ENCHANTRING`, the only "cast enchant" player animation the client
         * defines. The older per-level `HUMAN_ENCHANTAMULETLVL1..3` (719-721) are unused by modern
         * OSRS, which plays this one for all seven spells.
         */
        const val CAST_ANIMATION = 931

        val values = enumValues<EnchantSpell>()
    }
}
