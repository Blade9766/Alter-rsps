package org.alter.plugins.content.skills.smithing

/**
 * Which half of the anvil product list an item belongs to.
 *
 * Purely a presentation split. OSRS shows every product for a bar in one interface-312
 * grid, but this codebase's cache library has no interface/component decoder (see
 * `plugins/filestore/.../definition/decoder` - there are decoders for objects, npcs,
 * items, structs and enums, but none for interfaces), so interface 312's real component
 * ids cannot be verified from this cache and are not guessed. The products are instead
 * shown through the real `produceItemBox` skill-multi chatbox, which holds at most 10
 * items - hence two categories, sized 10 and 8 so each fits in one box.
 */
enum class SmithCategory {
    WEAPON,
    ARMOUR,
    ;

    val displayName: String get() = if (this == WEAPON) "Weapons" else "Armour"
}

/** One ore requirement of a smelting recipe. */
data class BarIngredient(
    val item: String,
    val amount: Int,
) {
    @Transient
    var itemId: Int = -1
}

/**
 * A smeltable bar. Level, experience and the ore combination are all wiki-sourced.
 */
data class BarEntry(
    val name: String,
    val bar: String,
    val level: Int,
    val experience: Double,
    val ingredients: List<BarIngredient>,
    /**
     * Chance the smelt succeeds. Only iron differs from 1.0 - the wiki states smelting
     * iron ore "will fail 50% of the time" without a ring of forging, superheat, or the
     * Blast Furnace.
     */
    val successChance: Double = 1.0,
) {
    @Transient
    var barItemId: Int = -1
}

/** One item smithable from a bar at an anvil. */
data class SmithableEntry(
    val item: String,
    val level: Int,
    val bars: Int,
    val category: SmithCategory,
) {
    @Transient
    var itemId: Int = -1
}

/**
 * A metal's anvil product list.
 *
 * Only the six smithable metals appear here. Silver and gold bars have no standard anvil
 * products in OSRS - they feed Crafting instead - so they are smeltable but not smithable,
 * which is why `bars.json` has eight entries and `products.json` only six.
 */
data class MetalEntry(
    val name: String,
    val bar: String,
    /**
     * Smithing XP granted per bar consumed; an item's XP is this times its bar count.
     * Cross-checked two independent ways against the wiki: the Smithing article states a
     * bronze platebody is 62.5 xp from 5 bars (12.5/bar), and every metal's spear row
     * lists exactly double its per-bar rate for a single bar (bronze 25, iron 50, steel
     * 75, mithril 100, adamant 125, rune 150).
     */
    val experiencePerBar: Double,
    val products: List<SmithableEntry>,
) {
    @Transient
    var barItemId: Int = -1
}
