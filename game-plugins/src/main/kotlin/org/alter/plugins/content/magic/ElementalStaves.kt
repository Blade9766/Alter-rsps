package org.alter.plugins.content.magic

import org.alter.api.EquipmentType
import org.alter.game.model.entity.Player
import org.alter.rscm.RSCM.getRSCM

/**
 * The staves that stand in for runes: while one is equipped, the spells it covers can be cast
 * without holding or consuming the runes it supplies. Consulted by [MagicSpells.canCast] and
 * [MagicSpells.removeRunes], which is what makes it apply to *every* combat spell rather than a
 * hand-picked list.
 *
 * Three tiers, all functionally identical as rune sources and differing only in their equip
 * requirements and bonuses (which are equipment data, not behaviour, and live in
 * `data/cfg/items/equipmentRequirements.yml` and the cache):
 *
 *  - **Basic elemental staves** - no requirements, one basic rune each.
 *  - **Battlestaves** - 30 Attack and 30 Magic. Four elemental, plus the six combination
 *    staves, which are craftable only as their elemental forms and otherwise drop-only.
 *  - **Mystic staves** - 40 Attack and 40 Magic, the Thormac upgrade of a battlestaff.
 *
 * plus the [Twinflame staff][TwinflameStaff], which is its own thing: 60 Magic, fire *and* water,
 * and passives beyond the rune supply.
 *
 * ## Combination staves supply the two basic runes, not the combination rune
 *
 * A lava battlestaff is described as a source of "lava runes", but no spell in the game requires
 * a lava rune - lava runes are themselves a stand-in for an earth and a fire rune. So the useful
 * thing for a combination staff to declare, and what the wiki's own item pages say it does, is
 * the *pair* of basic runes: "provides unlimited amounts of earth and fire runes". Mapping one of
 * these to `item.lava_rune` would have matched nothing a spell ever asks for, and the staff would
 * have supplied no runes at all.
 *
 * ## Deliberately not covered
 *
 * The Staff of the dead family (staff of the dead, toxic staff of the dead, staff of light, staff
 * of balance) are also unlimited air rune sources, and Bryophyta's staff is an unlimited nature
 * rune source. They are not elemental staves and each carries its own separate mechanics, so they
 * are left for whoever builds those items rather than half-wired here. Adding one is one more
 * entry in [RUNES_BY_STAFF].
 *
 * Combination *runes* in the inventory - holding lava runes to pay for a fire spell - are a
 * different substitution and are not implemented anywhere yet; this covers staves only.
 */
object ElementalStaves {
    private const val AIR = "item.air_rune"
    private const val WATER = "item.water_rune"
    private const val EARTH = "item.earth_rune"
    private const val FIRE = "item.fire_rune"

    /**
     * Staff item -> the basic runes it supplies without limit.
     *
     * Names, not ids, so a cache bump that moves an id is a load-time failure with the item's
     * name in it rather than a staff that silently stops working. `ElementalStaffVerify` checks
     * every entry still resolves to an equippable weapon carrying the expected cache name.
     */
    private val RUNES_BY_STAFF: Map<String, Array<String>> =
        mapOf(
            // Basic elemental staves - no requirements.
            "item.staff_of_air" to arrayOf(AIR),
            "item.staff_of_water" to arrayOf(WATER),
            "item.staff_of_earth" to arrayOf(EARTH),
            "item.staff_of_fire" to arrayOf(FIRE),
            // Battlestaves - 30 Attack, 30 Magic.
            "item.air_battlestaff" to arrayOf(AIR),
            "item.water_battlestaff" to arrayOf(WATER),
            "item.earth_battlestaff" to arrayOf(EARTH),
            "item.fire_battlestaff" to arrayOf(FIRE),
            "item.mist_battlestaff" to arrayOf(AIR, WATER),
            "item.dust_battlestaff" to arrayOf(AIR, EARTH),
            "item.smoke_battlestaff" to arrayOf(AIR, FIRE),
            "item.mud_battlestaff" to arrayOf(WATER, EARTH),
            "item.steam_battlestaff" to arrayOf(WATER, FIRE),
            "item.lava_battlestaff" to arrayOf(EARTH, FIRE),
            /*
             * The upgrade-kit forms. Cosmetic only - identical stats, requirements and rune
             * supply - but they are separate item ids, so a staff decorated with a steam or lava
             * staff upgrade kit stops supplying runes entirely if they are left out.
             */
            "item.steam_battlestaff_12795" to arrayOf(WATER, FIRE),
            "item.lava_battlestaff_21198" to arrayOf(EARTH, FIRE),
            // Mystic staves - 40 Attack, 40 Magic.
            "item.mystic_air_staff" to arrayOf(AIR),
            "item.mystic_water_staff" to arrayOf(WATER),
            "item.mystic_earth_staff" to arrayOf(EARTH),
            "item.mystic_fire_staff" to arrayOf(FIRE),
            "item.mystic_mist_staff" to arrayOf(AIR, WATER),
            "item.mystic_dust_staff" to arrayOf(AIR, EARTH),
            "item.mystic_smoke_staff" to arrayOf(AIR, FIRE),
            "item.mystic_mud_staff" to arrayOf(WATER, EARTH),
            "item.mystic_steam_staff" to arrayOf(WATER, FIRE),
            "item.mystic_lava_staff" to arrayOf(EARTH, FIRE),
            "item.mystic_steam_staff_12796" to arrayOf(WATER, FIRE),
            "item.mystic_lava_staff_21200" to arrayOf(EARTH, FIRE),
            // 60 Magic. See [TwinflameStaff] for the rest of what it does.
            TwinflameStaff.ITEM to arrayOf(WATER, FIRE),
        )

    /**
     * The same table with both sides resolved, so a cast is one map lookup rather than an RSCM
     * string lookup per entry per required rune.
     *
     * Resolved lazily: this is a top-level `object`, and touching it during class initialisation
     * would run [getRSCM] before `RSCM.init()` has read `data/cfg/rscm/`.
     */
    private val runesByStaffId: Map<Int, IntArray> by lazy {
        RUNES_BY_STAFF.entries.associate { (staff, runes) -> getRSCM(staff) to runes.map { getRSCM(it) }.toIntArray() }
    }

    /** Whether [player]'s equipped weapon supplies [runeId] without limit. */
    fun providesUnlimited(
        player: Player,
        runeId: Int,
    ): Boolean {
        val weapon = player.equipment[EquipmentType.WEAPON.id] ?: return false
        val supplied = runesByStaffId[weapon.id] ?: return false
        return runeId in supplied
    }

    /** Staff item id -> the rune item ids it supplies. For the verify test. */
    fun table(): Map<Int, IntArray> = runesByStaffId
}
