package org.alter.api

/**
 * The weapon types the client knows about, as written to `Varbit.WEAPON_TYPE_VARBIT` (357).
 *
 * These ids are not arbitrary: the client picks the Combat Options tab layout - which
 * buttons exist and what they are called - straight off that varbit, and the cache's own
 * `WEAPON_STYLES` enum (3908) is keyed by the same number. Every id below was read out of
 * that enum on the rev-228 cache: 3908 maps a weapon type to a second enum which maps the
 * style index (`Varp.WEAPON_ATTACK_STYLE`) to an attack-style struct - 3722 Accurate,
 * 3723 Aggressive, 3724 Controlled, 3725 Defensive, 3726 Ranging, 3727 Longrange,
 * 3728 Casting, 3721 "Other" (an unused button). See [org.alter.api.WeaponCategory] for the
 * item category that selects one of these, and `WeaponStyles` in game-plugins for the
 * per-style behaviour the table above yields.
 *
 * Several ids here were previously wrong, which silently disabled every behaviour keyed off
 * them - see the notes on the individual constants.
 *
 * @author Tom <rspsmods@gmail.com>
 */
enum class WeaponType(val id: Int) {
    NONE(id = 0),
    AXE(id = 1),

    /**
     * Warhammers, mauls and other blunt two-handers: Pound, Pummel, Block.
     */
    HAMMER(id = 2),
    BOW(id = 3),
    CLAWS(id = 4),
    CROSSBOW(id = 5),
    SALAMANDER(id = 6),
    CHINCHOMPA(id = 7),
    GUN(id = 8), // Aim / Fire options => used for Fixed Device

    /**
     * The cache's "slash sword" panel - longswords, scimitars, sickles, machetes.
     */
    LONG_SWORD(id = 9),

    /**
     * Was 23, which is a different panel entirely. Nothing mapped to 10, so no two-handed
     * sword in the game was ever a [TWO_HANDED]: they took the [POWERED_STAFF] branches
     * instead and reported a combat style of MAGIC, which the melee formula rejects
     * outright.
     */
    TWO_HANDED(id = 10),
    PICKAXE(id = 11),
    HALBERD(id = 12),

    /**
     * Quarterstaves and other melee-only staves - no autocast buttons.
     */
    STAFF(id = 13),
    SCYTHE(id = 14),
    SPEAR(id = 15),

    /**
     * The cache's "spiked" panel: maces and Verac's flail.
     */
    MACE(id = 16),

    /**
     * The cache's "stab sword" panel - daggers *and* swords, which share a category.
     */
    DAGGER(id = 17),

    /**
     * A staff with the two autocast buttons (style indices 4 and 5).
     */
    MAGIC_STAFF(id = 18),
    THROWN(id = 19),
    WHIP(id = 20),
    STAFF_HALBERD(id = 21),

    /**
     * Absent from enum 3908; the Blue moon spear is the only weapon that uses it.
     */
    BLUE_MOON_SPEAR(id = 22),

    /**
     * Powered staves - trident, sanguinesti staff, Tumeken's shadow. Was 23, which is a
     * separate melee panel, so [TWO_HANDED] collided with it and no actual powered staff
     * ever reached these branches.
     */
    POWERED_STAFF(id = 24),
    BANNER(id = 25),

    /**
     * Was 26, which is a different panel. The abyssal bludgeon's three buttons are all
     * Aggressive, which is what 27 declares.
     */
    BLUDGEON(id = 27),

    /**
     * Was 27, colliding with [BLUDGEON]. 28 is the one-button Accurate panel Dinh's
     * bulwark uses.
     */
    BULWARK(id = 28),

    /**
     * A second Casting/Casting/Defensive panel alongside [POWERED_STAFF].
     */
    POWERED_WAND(id = 29),

    /**
     * Keris partisans. Was mapped onto [DAGGER] (17).
     */
    PARTISAN(id = 30),

    /**
     * Melee / ranged / magic from one weapon, like a salamander.
     */
    MULTI_STYLE(id = 31),
    ;

    companion object {
        val values = enumValues<WeaponType>()

        fun forId(id: Int): WeaponType? = values.firstOrNull { it.id == id }
    }
}
