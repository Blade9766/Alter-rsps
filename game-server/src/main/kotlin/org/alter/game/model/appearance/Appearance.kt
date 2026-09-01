package org.alter.game.model.appearance

import org.alter.game.model.appearance.Looks.getArms
import org.alter.game.model.appearance.Looks.getFeets
import org.alter.game.model.appearance.Looks.getHands
import org.alter.game.model.appearance.Looks.getHeads
import org.alter.game.model.appearance.Looks.getJaws
import org.alter.game.model.appearance.Looks.getLegs
import org.alter.game.model.appearance.Looks.getTorsos
import org.alter.game.model.item.Item
import org.bson.Document


/**
 * @author Tom <rspsmods@gmail.com>
 */
data class Appearance(val looks: IntArray, val colors: IntArray, var gender: Gender) {

    /**
     * @param option - the specified look to select from the [Appearance]'s [looks]
     *      with valid options explicitly as follows:
     *      0 -> HEAD
     *      1 -> JAW
     *      2 -> TORSO
     *      3 -> ARMS
     *      4 -> HANDS
     *      5 -> LEGS
     *      6 -> FEET
     *
     * [Gender.FEMALE] look arrays store the jaw last, at [FEMALE_JAW_INDEX], because the
     * slot was added after appearances were already being saved without it; arrays that
     * predate it fall back to jaw index 0 (the clean-shaven kit).
     *
     * @returns the appropriate look model value for current appearance
     *      based on the supplies option
     */
    fun getLook(option: Int): Int {
        return when (gender) {
            Gender.MALE -> {
                when (option) {
                    0 -> getHeads(gender)[looks[0]]
                    1 -> getJaws(gender)[looks[1]]
                    2 -> getTorsos(gender)[looks[2]]
                    3 -> getArms(gender)[looks[3]]
                    4 -> getHands(gender)[looks[4]]
                    5 -> getLegs(gender)[looks[5]]
                    6 -> getFeets(gender)[looks[6]]
                    else -> -1
                }
            }
            Gender.FEMALE -> {
                when (option) {
                    0 -> getHeads(gender)[looks[0]]
                    1 -> getJaws(gender)[looks.getOrElse(FEMALE_JAW_INDEX) { 0 }]
                    2 -> getTorsos(gender)[looks[1]]
                    3 -> getArms(gender)[looks[2]]
                    4 -> getHands(gender)[looks[3]]
                    5 -> getLegs(gender)[looks[4]]
                    6 -> getFeets(gender)[looks[5]]
                    else -> -1
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Appearance

        if (!looks.contentEquals(other.looks)) return false
        if (!colors.contentEquals(other.colors)) return false
        if (gender != other.gender) return false

        return true
    }

    override fun hashCode(): Int {
        var result = looks.contentHashCode()
        result = 31 * result + colors.contentHashCode()
        result = 31 * result + gender.hashCode()
        return result
    }


    //TODO MAP Appearance of HEAD:VALUE
    fun asDocument(): Document {
        return Document()
            .append("looks", looks.toList())
            .append("colors", colors.toList())
            .append("gender", gender.name)
    }

    companion object {

        fun fromDocument(doc: Document): Appearance {
            val gender = Gender.valueOf(doc.getString("gender") ?: "MALE")
            return Appearance(
                withJawSlot(doc.getList("looks", Integer::class.java).map { it.toInt() }.toIntArray(), gender),
                doc.getList("colors", Integer::class.java).map { it.toInt() }.toIntArray(),
                gender
            )
        }

        /**
         * Grows a [Gender.FEMALE] look array saved before the jaw slot existed so it has
         * room for one, defaulting to the clean-shaven kit.
         */
        private fun withJawSlot(looks: IntArray, gender: Gender): IntArray =
            if (gender == Gender.FEMALE && looks.size == FEMALE_JAW_INDEX) looks + 0 else looks

        /**
         * Position of the jaw look in a [Gender.FEMALE] look array; the other six entries
         * keep the head/torso/arms/hands/legs/feet order they have always had.
         */
        const val FEMALE_JAW_INDEX = 6

        private val DEFAULT_COLORS = intArrayOf(0, 27, 9, 0, 0)

        private val DEFAULT_MALE_LOOKS = intArrayOf(15, 9, 3, 8, 0, 3, 1) // 133, 113, 21, 86, 33, 39, 43

        private val DEFAULT_FEMALE_LOOKS = intArrayOf(0, 0, 0, 0, 0, 0, 0) // 45, 56, 61, 67, 70, 79, 296

        // Fresh instances: the look and colour arrays are mutated in place by appearance
        // editors, so handing out a shared one would edit every default-appearance player.
        val DEFAULT_MALE: Appearance
            get() = Appearance(DEFAULT_MALE_LOOKS.copyOf(), DEFAULT_COLORS.copyOf(), Gender.MALE)

        val DEFAULT_FEMALE: Appearance
            get() = Appearance(DEFAULT_FEMALE_LOOKS.copyOf(), DEFAULT_COLORS.copyOf(), Gender.FEMALE)
    }
}
