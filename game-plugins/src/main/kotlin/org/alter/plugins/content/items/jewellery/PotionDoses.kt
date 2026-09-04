package org.alter.plugins.content.items.jewellery

import org.alter.plugins.content.items.consumables.potions.Potion
import org.alter.rscm.RSCM.getRSCM

/**
 * Three-dose potion to four-dose potion, for the amulet of chemistry.
 *
 * Built from [Potion]'s own dose ladders rather than from a table written here or a name search
 * through the cache. Every drinkable potion in the project already declares its four doses in order,
 * so the third and fourth entries of each ladder are exactly the pair the amulet turns one into the
 * other - and a potion the project has not defined simply is not in the map, which makes the amulet
 * a no-op on it rather than producing an id nothing can drink.
 *
 * Ladders that are not four doses long - the barbarian mixes are two - are skipped for the same
 * reason.
 */
object PotionDoses {
    private val threeToFour: Map<Int, Int> by lazy {
        Potion.values
            .asSequence()
            .filter { it.doses.size == 4 }
            .mapNotNull { potion ->
                val three = runCatching { getRSCM(potion.doses[2]) }.getOrNull() ?: return@mapNotNull null
                val four = runCatching { getRSCM(potion.doses[3]) }.getOrNull() ?: return@mapNotNull null
                three to four
            }
            .toMap()
    }

    /** The four-dose version of [threeDoseId], or null when there is no such potion. */
    fun fourDoseOf(threeDoseId: Int): Int? = threeToFour[threeDoseId]

    /** How many pairings were resolved, for `JewelleryVerify` to assert the map is not empty. */
    fun size(): Int = threeToFour.size
}
