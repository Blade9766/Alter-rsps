package org.alter.plugins.content.mechanics.appearance

import org.alter.game.model.appearance.Appearance
import org.alter.game.model.appearance.Colours
import org.alter.game.model.appearance.Gender
import org.alter.game.model.appearance.Looks

/**
 * The makeover ("Character Creator") window, interface 679, and the arithmetic behind it.
 *
 * Everything below was read out of the rev-228 cache rather than guessed. The interface itself
 * holds almost no visible content: every label, arrow and dropdown row is drawn at runtime by the
 * onload scripts baked into the components, so the component numbers are the only contract the
 * server has with it.
 *
 *  - Each editable row is a *stepper*: a label plus two buttons that both run
 *    `[clientscript,script3746]`. The pair differs only in that script's last argument, which is
 *    fed to `cc_sethflip`; sprite 2519 is a right-pointing arrow, so the flipped button of each
 *    pair is the left arrow. That is what makes the lower component of each pair "previous" and
 *    the higher one "next" - see [Stepper].
 *  - The two body type buttons run `[clientscript,script3753]` with the literal labels "A" and
 *    "B", and its worker `[clientscript,script3755]` ticks the selected one by comparing
 *    varbit 14021 against the label. That varbit is the whole body type state as far as the
 *    client is concerned, so the server has to keep it in step with [Appearance.gender].
 *  - The pronoun row is the standard settings dropdown, handed struct 994 by
 *    `[clientscript,script6046]`. Its rows are dynamic children the client only builds when the
 *    list is opened, so unlike everything else here they need `IfSetEvents` before the client will
 *    tell the server which one was picked. See [PRONOUN_DROPDOWN_ROWS].
 *  - The model at [PREVIEW] is empty in the cache; the client only draws the player into it once
 *    the server sends `IfSetPlayerModelSelf` for that component.
 *
 * Every static button here already carries op 1 in its cache click mask, so none of them need
 * `IfSetEvents` to become clickable.
 */
object Makeover {

    /** The makeover window. Also the character creation screen, hence the client's own name for it. */
    const val INTERFACE_ID = 679

    /** Type 6 component the player's own model is drawn into. */
    const val PREVIEW = 73

    /** Closes the window. Drawn by the same script as the arrows, but with a text label. */
    const val CONFIRM = 74

    /** Body type A - [Gender.MALE]. */
    const val BODY_TYPE_A = 68

    /** Body type B - [Gender.FEMALE]. */
    const val BODY_TYPE_B = 69

    /**
     * The closed pronoun control. Clicking it only opens the list, which the client does by itself,
     * so nothing here is bound to it.
     */
    const val PRONOUN_DROPDOWN = 72

    /**
     * The open pronoun list. `[clientscript,script4568]` builds one text child per option onto this
     * component, in enum 5500's key order and numbered from 1 - `[clientscript,script4569]` takes
     * the sub back down to a row index by subtracting one, and so does [pronounForSub].
     *
     * These children are created by the client, which means they carry no click mask from the
     * cache: without [PRONOUN_ROW_SUBS] being opened up with `IfSetEvents` the client runs its own
     * handler - closing the list and relabelling it - and never sends the choice anywhere.
     */
    const val PRONOUN_DROPDOWN_ROWS = 78

    /** Comfortably wider than enum 5500; the rows sit at subs 1..3. */
    val PRONOUN_ROW_SUBS = 0..10

    /** Body type is one bit: 0 = A, 1 = B. */
    const val VARBIT_BODY_TYPE = 14021

    /**
     * Pronoun choice, as [Pronoun.ordinal] - two bits of varp 2855, which is exactly the varp the
     * dropdown asks to be redrawn on.
     *
     * Not varp 387. Struct 994's param 1077 holds 387, but that is the *setting* id rather than a
     * var: `[clientscript,script3962]` is the switch that maps it, and case 387 there returns this
     * varbit. Writing varp 387 changes an unrelated setting and the pronoun row never moves.
     */
    const val VARBIT_PRONOUN = 10988

    /** The pronoun a click on sub [sub] of [PRONOUN_DROPDOWN_ROWS] chose, if it was a row at all. */
    fun pronounForSub(sub: Int): Pronoun? = Pronoun.values.getOrNull(sub - 1)

    /**
     * One row of the window: the button that steps its value back, the button that steps it
     * forward, and how to read and write that value on an [Appearance].
     */
    data class Stepper(
        val previous: Int,
        val next: Int,
        val label: String,
        val get: (Appearance) -> Int,
        val set: (Appearance, Int) -> Unit,
        val size: (Appearance) -> Int,
    )

    /**
     * The seven "Design" rows, in the order [Appearance.getLook] takes its parts: head, jaw,
     * torso, arms, hands, legs, feet.
     */
    val LOOKS: List<Stepper> =
        listOf(
            lookStepper(15, 16, part = 0, label = "Head"),
            lookStepper(19, 20, part = 1, label = "Jaw"),
            lookStepper(23, 24, part = 2, label = "Torso"),
            lookStepper(27, 28, part = 3, label = "Arms"),
            lookStepper(31, 32, part = 4, label = "Hands"),
            lookStepper(35, 36, part = 5, label = "Legs"),
            lookStepper(39, 40, part = 6, label = "Feet"),
        )

    /** The five "Colour" rows. These index straight into [Appearance.colors]. */
    val COLOURS: List<Stepper> =
        listOf(
            colourStepper(46, 47, slot = 0, label = "Hair"),
            colourStepper(50, 51, slot = 1, label = "Torso"),
            colourStepper(54, 55, slot = 2, label = "Legs"),
            colourStepper(58, 59, slot = 3, label = "Feet"),
            colourStepper(62, 63, slot = 4, label = "Skin"),
        )

    val ALL_STEPPERS: List<Stepper> = LOOKS + COLOURS

    /**
     * Where part [part] of a look - in [Appearance.getLook] order - actually sits in the
     * [Appearance.looks] array.
     *
     * [Gender.FEMALE] arrays keep the jaw last, at [Appearance.FEMALE_JAW_INDEX], because that
     * slot was added after appearances were already being saved without it; the other six keep
     * the order they have always had.
     */
    fun lookIndex(
        gender: Gender,
        part: Int,
    ): Int =
        when {
            gender == Gender.MALE -> part
            part == 0 -> 0
            part == 1 -> Appearance.FEMALE_JAW_INDEX
            else -> part - 1
        }

    /** The identikits selectable for [part] on [gender], in the order the arrows step through. */
    fun looksFor(
        gender: Gender,
        part: Int,
    ): Array<Int> =
        when (part) {
            0 -> Looks.getHeads(gender)
            1 -> Looks.getJaws(gender)
            2 -> Looks.getTorsos(gender)
            3 -> Looks.getArms(gender)
            4 -> Looks.getHands(gender)
            5 -> Looks.getLegs(gender)
            6 -> Looks.getFeets(gender)
            else -> emptyArray()
        }

    private fun coloursFor(slot: Int): Array<Int> =
        when (slot) {
            0 -> Colours.HAIR_COLOURS
            1 -> Colours.TORSO_COLOURS
            2 -> Colours.LEG_COLOURS
            3 -> Colours.FEET_COLOURS
            4 -> Colours.SKIN_COLOURS
            else -> emptyArray()
        }

    private fun lookStepper(
        previous: Int,
        next: Int,
        part: Int,
        label: String,
    ) = Stepper(
        previous = previous,
        next = next,
        label = label,
        get = { it.looks.getOrElse(lookIndex(it.gender, part)) { 0 } },
        set = { appearance, value -> appearance.looks[lookIndex(appearance.gender, part)] = value },
        size = { looksFor(it.gender, part).size },
    )

    private fun colourStepper(
        previous: Int,
        next: Int,
        slot: Int,
        label: String,
    ) = Stepper(
        previous = previous,
        next = next,
        label = label,
        get = { it.colors[slot] },
        set = { appearance, value -> appearance.colors[slot] = value },
        size = { coloursFor(slot).size },
    )

    /**
     * The same appearance rebuilt for [gender].
     *
     * The two body types index into different identikit tables, so a look cannot be carried over
     * by value; what is carried over is each row's *position*, clamped to the length of the table
     * it lands in. Colours are shared between the body types and come across untouched.
     */
    fun Appearance.asBodyType(gender: Gender): Appearance {
        if (gender == this.gender) return this
        val parts = IntArray(7) { part -> looks.getOrElse(lookIndex(this.gender, part)) { 0 } }
        val rebuilt = IntArray(7)
        for (part in 0..6) {
            val size = looksFor(gender, part).size
            rebuilt[lookIndex(gender, part)] = parts[part].coerceIn(0, (size - 1).coerceAtLeast(0))
        }
        return Appearance(rebuilt, colors.copyOf(), gender)
    }
}

/**
 * The pronouns the game uses for a player. Ordinals are the values varbit
 * [Makeover.VARBIT_PRONOUN] holds, and the keys of enum 5500, whose values are the labels the
 * dropdown draws - so they must not be reordered.
 */
enum class Pronoun(val subject: String, val objective: String, val possessive: String) {
    HE("he", "him", "his"),
    SHE("she", "her", "her"),
    THEY("they", "them", "their"),
    ;

    /** How the makeover window and the settings list label this choice; enum 5500, verbatim. */
    val label: String get() = "${subject.replaceFirstChar { it.uppercase() }}/$objective"

    companion object {
        val values = enumValues<Pronoun>()

        fun of(value: Int): Pronoun = values.getOrElse(value) { THEY }
    }
}
