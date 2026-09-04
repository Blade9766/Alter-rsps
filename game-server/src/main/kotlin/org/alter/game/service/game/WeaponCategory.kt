package org.alter.game.service.game

import dev.openrune.cache.filestore.definition.data.ItemType
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Maps an item's cache *category* to the *weapon type* written to varbit 357, which is what
 * decides the player's Combat Options tab and, through it, every attack style the server
 * resolves.
 *
 * The two numbering schemes are unrelated, and three of the mappings here used to point at
 * the wrong panel:
 *
 *  - Two-handed swords (61) claimed type 23 rather than 10. 23 is a different melee panel
 *    that nothing else uses, and the game-api enum had it labelled TRIDENT, so every
 *    two-handed sword in the game reported a combat style of MAGIC. `MeleeCombatFormula`
 *    only accepts STAB/SLASH/CRUSH and throws on anything else, so attacking with a rune 2h
 *    or a godsword raised `IllegalStateException` instead of hitting.
 *  - Blunt weapons (26, 55, 15) claimed 27 rather than 2. 27 is the abyssal bludgeon's
 *    all-Aggressive panel.
 *  - Keris partisans (1588) claimed 17 (stab sword) rather than 30, their own panel.
 *
 * Categories that hold more than one kind of weapon - notably 1, which is every staff from a
 * plain quarterstaff to Tumeken's shadow - get the most common type here and are corrected
 * per item by the YAML documents under `data/cfg/items/itemOverrides/weapons`, which
 * [ItemMetadataService] applies afterwards.
 */
enum class WeaponCategory(val id: List<Int>, val weaponType: Int) {
    BOW(listOf(64, 106), 3),
    SLASH_SWORD(listOf(21), 9),
    TWO_HANDED(listOf(61), 10),
    AXE(listOf(35), 1),
    BANNER(listOf(92, 42), 25),
    BLUNT(listOf(26, 55, 15), 2),
    BULWARK(listOf(1014), 28),
    CLAWS(listOf(65), 4),
    PICKAXE(listOf(67), 11),
    POLEARM(listOf(66, 273), 12),
    SCYTHE(listOf(1143, 1193, 14), 14),
    SPEAR(listOf(36), 15),
    SPIKED(listOf(39), 16),
    STAB_SWORD(listOf(25), 17),
    UNARMED(listOf(188, -1, 148, 95, 1194, 0, 2053), 0),
    WHIP(listOf(150), 20),
    CHINCHOMPA(listOf(572), 7),
    CROSSBOW(listOf(567, 37), 5),
    GUN(listOf(96), 8),
    THROWN(listOf(24), 19),
    STAFF(listOf(1), 18),
    SALAMANDER(listOf(586), 6),
    PARTISAN(listOf(1588), 30),
    MULTISTYLE(listOf(975), 31),

    // Not able to equip
    FOOD(listOf(86), 0),
    SPRAY(listOf(886), 0),
    EASTEREGG(listOf(319), 0),
    Potion(listOf(69), 0)
    ;

    companion object {
        private val logger = KotlinLogging.logger {}

        val values = values()

        /**
         * The panel used for a weapon-slot item whose category is not listed above. Matches
         * `ItemMetadataService.DEFAULT_WEAPON_TYPE`.
         *
         * Returning a default rather than throwing matters more than it looks: this is
         * called from the single pass over every item in the cache, and that pass sits
         * inside one `try` block. A throw here did not skip one item - it abandoned the
         * whole loop, leaving every item after it with no bonuses, no attack speed and no
         * requirements at all.
         */
        private const val FALLBACK_WEAPON_TYPE = 17

        fun get(def: ItemType, id: Int): Int {
            values.forEach {
                if (it.id.contains(id)) {
                    return it.weaponType
                }
            }
            logger.warn {
                "Item ${def.id} (${def.name}) has unmapped weapon category $id; " +
                    "defaulting to weapon type $FALLBACK_WEAPON_TYPE."
            }
            return FALLBACK_WEAPON_TYPE
        }
    }
}
