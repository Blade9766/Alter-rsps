package org.alter.plugins.content.combat

import org.alter.api.WeaponType
import org.alter.game.model.combat.AttackStyle
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.combat.XpMode

/**
 * Every attack style button in the game, as one table.
 *
 * A style is fully described by three things, and the server needs all three on every
 * attack: which bonus the roll uses ([CombatStyle]), which invisible level boost applies
 * ([AttackStyle]), and which skill is paid ([XpMode]). [CombatConfigs] used to answer those
 * three questions from three separate `when` cascades over [WeaponType], and they disagreed
 * with each other - a dagger's third button was Aggressive for experience but Controlled for
 * the level boost, so it paid Strength while granting +1/+1/+1 instead of +3 Strength. Axes,
 * pickaxes and two-handed swords had the same split, and maces and quarterstaves appeared in
 * none of the cascades at all, so they silently got no style bonus whatsoever.
 *
 * ### Where the numbers come from
 *
 * The style *category* of every entry is read out of the cache, not guessed. Enum 3908 maps
 * a weapon type to a per-type enum, which maps the style index - the raw value of
 * `Varp.WEAPON_ATTACK_STYLE` - to one of a handful of structs: 3722 Accurate, 3723
 * Aggressive, 3724 Controlled, 3725 Defensive, 3726 Ranging, 3727 Longrange, 3728 Casting,
 * and 3721 "Other" for a button the panel does not have. Those structs also carry the
 * experience split as params 1401/1402/1403/1404 (attack/strength/defence/hitpoints), which
 * is where each [XpMode] comes from: 3722 is attack-only, 3723 strength-only, 3724 is 4/4/4
 * shared, 3725 defence-only.
 *
 * The attack *type* - stab, slash or crush - is not in the cache, and is taken from the OSRS
 * Wiki's combat styles table on each weapon page.
 *
 * Index into a panel with the raw style index. A `null` entry is a button the panel does not
 * have.
 */
object WeaponStyles {
    /**
     * One button on the Combat Options tab.
     *
     * @param label the button's in-game name, for reference only.
     */
    data class Style(
        val label: String,
        val combatStyle: CombatStyle,
        val attackStyle: AttackStyle,
        val xpMode: XpMode,
    )

    /**
     * Style indices 4 and 5 are the two autocast slots, present only on a staff panel.
     */
    const val AUTOCAST_STYLE_INDEX = 4

    private const val STYLE_SLOTS = 6

    private fun styles(vararg entries: Style?): Array<Style?> {
        val slots = arrayOfNulls<Style>(STYLE_SLOTS)
        entries.forEachIndexed { index, style -> slots[index] = style }
        return slots
    }

    private fun accurate(
        label: String,
        style: CombatStyle,
    ) = Style(label, style, AttackStyle.ACCURATE, XpMode.ATTACK)

    private fun aggressive(
        label: String,
        style: CombatStyle,
    ) = Style(label, style, AttackStyle.AGGRESSIVE, XpMode.STRENGTH)

    private fun controlled(
        label: String,
        style: CombatStyle,
    ) = Style(label, style, AttackStyle.CONTROLLED, XpMode.SHARED)

    private fun defensive(
        label: String,
        style: CombatStyle,
    ) = Style(label, style, AttackStyle.DEFENSIVE, XpMode.DEFENCE)

    private fun ranging(
        label: String,
        attackStyle: AttackStyle,
    ) = Style(label, CombatStyle.RANGED, attackStyle, XpMode.RANGED)

    private fun longrange(label: String) = Style(label, CombatStyle.RANGED, AttackStyle.LONG_RANGE, XpMode.SHARED)

    /**
     * The two autocast buttons a magic-capable staff carries at indices 4 and 5. Magic has
     * no invisible level boost, so the [AttackStyle] is [AttackStyle.NONE] rather than the
     * cache's Casting/Defensive labels, which describe the experience split only.
     */
    private val CAST = Style("Spell", CombatStyle.MAGIC, AttackStyle.NONE, XpMode.MAGIC)
    private val DEFENSIVE_CAST = Style("Spell (defensive)", CombatStyle.MAGIC, AttackStyle.NONE, XpMode.SHARED)

    /**
     * A powered staff's own attack - Accurate on both of the first two buttons, Longrange on
     * the third. It deals magic damage but it is not a spell, so it never reads the autocast
     * slots above.
     */
    private val POWERED_ACCURATE = Style("Accurate", CombatStyle.MAGIC, AttackStyle.ACCURATE, XpMode.MAGIC)
    private val POWERED_LONGRANGE = Style("Longrange", CombatStyle.MAGIC, AttackStyle.LONG_RANGE, XpMode.SHARED)

    private val RANGED_STYLES =
        styles(
            ranging("Accurate", AttackStyle.ACCURATE),
            ranging("Rapid", AttackStyle.RAPID),
            null,
            longrange("Longrange"),
        )

    private val TABLE: Map<Int, Array<Style?>> =
        mapOf(
            WeaponType.NONE.id to
                styles(
                    accurate("Punch", CombatStyle.CRUSH),
                    aggressive("Kick", CombatStyle.CRUSH),
                    null,
                    defensive("Block", CombatStyle.CRUSH),
                ),
            WeaponType.AXE.id to
                styles(
                    accurate("Chop", CombatStyle.SLASH),
                    aggressive("Hack", CombatStyle.SLASH),
                    aggressive("Smash", CombatStyle.CRUSH),
                    defensive("Block", CombatStyle.SLASH),
                ),
            WeaponType.HAMMER.id to
                styles(
                    accurate("Pound", CombatStyle.CRUSH),
                    aggressive("Pummel", CombatStyle.CRUSH),
                    null,
                    defensive("Block", CombatStyle.CRUSH),
                ),
            WeaponType.BOW.id to RANGED_STYLES,
            WeaponType.CLAWS.id to
                styles(
                    accurate("Chop", CombatStyle.SLASH),
                    aggressive("Slash", CombatStyle.SLASH),
                    controlled("Lunge", CombatStyle.STAB),
                    defensive("Block", CombatStyle.SLASH),
                ),
            WeaponType.CROSSBOW.id to RANGED_STYLES,
            /*
             * Scorch, Flare and Blaze. The wiki records that Flare, though "labelled as
             * 'Accurate'", grants no invisible ranged boost; the other two have no
             * documented boost either way, so all three stay at [AttackStyle.NONE]. Only the
             * combat class and the experience split - which the cache does state - are
             * modelled here.
             */
            WeaponType.SALAMANDER.id to
                styles(
                    Style("Scorch", CombatStyle.SLASH, AttackStyle.NONE, XpMode.STRENGTH),
                    Style("Flare", CombatStyle.RANGED, AttackStyle.NONE, XpMode.RANGED),
                    Style("Blaze", CombatStyle.MAGIC, AttackStyle.NONE, XpMode.MAGIC),
                ),
            WeaponType.CHINCHOMPA.id to
                styles(
                    ranging("Short fuse", AttackStyle.ACCURATE),
                    ranging("Medium fuse", AttackStyle.RAPID),
                    null,
                    longrange("Long fuse"),
                ),
            /*
             * A one-button Aggressive panel on the second slot, used only by the Fixed
             * device. Listed so that no weapon-slot item in the cache resolves to an empty
             * panel.
             */
            WeaponType.GUN.id to styles(null, ranging("Fire", AttackStyle.NONE)),
            WeaponType.LONG_SWORD.id to
                styles(
                    accurate("Chop", CombatStyle.SLASH),
                    aggressive("Slash", CombatStyle.SLASH),
                    controlled("Lunge", CombatStyle.STAB),
                    defensive("Block", CombatStyle.SLASH),
                ),
            WeaponType.TWO_HANDED.id to
                styles(
                    accurate("Chop", CombatStyle.SLASH),
                    aggressive("Slash", CombatStyle.SLASH),
                    aggressive("Smash", CombatStyle.CRUSH),
                    defensive("Block", CombatStyle.SLASH),
                ),
            WeaponType.PICKAXE.id to
                styles(
                    accurate("Spike", CombatStyle.STAB),
                    aggressive("Impale", CombatStyle.STAB),
                    aggressive("Smash", CombatStyle.CRUSH),
                    defensive("Block", CombatStyle.STAB),
                ),
            WeaponType.HALBERD.id to
                styles(
                    controlled("Jab", CombatStyle.STAB),
                    aggressive("Swipe", CombatStyle.SLASH),
                    null,
                    defensive("Fend", CombatStyle.STAB),
                ),
            WeaponType.STAFF.id to
                styles(
                    accurate("Bash", CombatStyle.CRUSH),
                    aggressive("Pound", CombatStyle.CRUSH),
                    null,
                    defensive("Focus", CombatStyle.CRUSH),
                ),
            WeaponType.SCYTHE.id to
                styles(
                    accurate("Reap", CombatStyle.SLASH),
                    aggressive("Chop", CombatStyle.SLASH),
                    aggressive("Jab", CombatStyle.CRUSH),
                    defensive("Block", CombatStyle.SLASH),
                ),
            WeaponType.SPEAR.id to
                styles(
                    controlled("Lunge", CombatStyle.STAB),
                    controlled("Swipe", CombatStyle.SLASH),
                    controlled("Pound", CombatStyle.CRUSH),
                    defensive("Block", CombatStyle.STAB),
                ),
            WeaponType.MACE.id to
                styles(
                    accurate("Pound", CombatStyle.CRUSH),
                    aggressive("Pummel", CombatStyle.CRUSH),
                    controlled("Spike", CombatStyle.STAB),
                    defensive("Block", CombatStyle.CRUSH),
                ),
            WeaponType.DAGGER.id to
                styles(
                    accurate("Stab", CombatStyle.STAB),
                    aggressive("Lunge", CombatStyle.STAB),
                    aggressive("Slash", CombatStyle.SLASH),
                    defensive("Block", CombatStyle.STAB),
                ),
            WeaponType.MAGIC_STAFF.id to
                styles(
                    accurate("Bash", CombatStyle.CRUSH),
                    aggressive("Pound", CombatStyle.CRUSH),
                    null,
                    defensive("Focus", CombatStyle.CRUSH),
                    CAST,
                    DEFENSIVE_CAST,
                ),
            WeaponType.THROWN.id to RANGED_STYLES,
            WeaponType.WHIP.id to
                styles(
                    accurate("Flick", CombatStyle.SLASH),
                    controlled("Lash", CombatStyle.SLASH),
                    null,
                    defensive("Deflect", CombatStyle.SLASH),
                ),
            WeaponType.STAFF_HALBERD.id to
                styles(
                    accurate("Jab", CombatStyle.STAB),
                    aggressive("Swipe", CombatStyle.SLASH),
                    null,
                    defensive("Fend", CombatStyle.CRUSH),
                    CAST,
                    DEFENSIVE_CAST,
                ),
            WeaponType.POWERED_STAFF.id to
                styles(
                    POWERED_ACCURATE,
                    POWERED_ACCURATE,
                    null,
                    POWERED_LONGRANGE,
                ),
            /*
             * Banners carry the same four-button panel as a slash sword. They have no
             * offensive bonuses at all, so the attack types below are the panel's shape
             * rather than something the wiki states outright.
             */
            WeaponType.BANNER.id to
                styles(
                    accurate("Chop", CombatStyle.SLASH),
                    aggressive("Slash", CombatStyle.SLASH),
                    controlled("Lunge", CombatStyle.STAB),
                    defensive("Block", CombatStyle.SLASH),
                ),
            WeaponType.BLUDGEON.id to
                styles(
                    aggressive("Pound", CombatStyle.CRUSH),
                    aggressive("Pummel", CombatStyle.CRUSH),
                    null,
                    aggressive("Smash", CombatStyle.CRUSH),
                ),
            /*
             * One button. Its Block option does not attack at all, which [Combat] handles
             * separately by refusing to start combat on style 3.
             */
            WeaponType.BULWARK.id to styles(accurate("Pummel", CombatStyle.CRUSH)),
            WeaponType.POWERED_WAND.id to
                styles(
                    POWERED_ACCURATE,
                    POWERED_ACCURATE,
                    null,
                    POWERED_LONGRANGE,
                ),
            WeaponType.PARTISAN.id to
                styles(
                    accurate("Stab", CombatStyle.STAB),
                    aggressive("Lunge", CombatStyle.STAB),
                    aggressive("Pound", CombatStyle.CRUSH),
                    defensive("Block", CombatStyle.STAB),
                ),
            WeaponType.MULTI_STYLE.id to
                styles(
                    Style("Melee", CombatStyle.SLASH, AttackStyle.NONE, XpMode.STRENGTH),
                    Style("Ranged", CombatStyle.RANGED, AttackStyle.NONE, XpMode.RANGED),
                    Style("Magic", CombatStyle.MAGIC, AttackStyle.NONE, XpMode.MAGIC),
                ),
        )

    /**
     * The style a weapon of [weaponType] deals on style index [styleIndex], or `null` where
     * the panel has no such button - which includes any weapon type nothing in the game maps
     * to.
     */
    fun get(
        weaponType: Int,
        styleIndex: Int,
    ): Style? = TABLE[weaponType]?.getOrNull(styleIndex)

    /**
     * As [get], but falls back to the panel's first button when the selected index is not one
     * this panel has.
     *
     * Combat must never be left without a style: [CombatConfigs] feeds the result to the
     * melee formula, which rejects anything that is not stab, slash or crush outright. A
     * player can genuinely end up on an index their weapon does not offer - the style varp
     * survives a weapon switch, and not every panel has four buttons - and answering "no
     * style" there would turn an ordinary weapon swap into a thrown exception mid-fight.
     */
    fun getOrFirst(
        weaponType: Int,
        styleIndex: Int,
    ): Style? {
        val panel = TABLE[weaponType] ?: return null
        return panel.getOrNull(styleIndex) ?: panel.firstOrNull { it != null }
    }
}
