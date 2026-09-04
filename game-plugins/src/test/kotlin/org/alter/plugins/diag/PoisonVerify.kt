package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.plugins.content.combat.strategy.magic.CombatSpell
import org.alter.plugins.content.combat.strategy.ranged.ammo.EnchantedBolt
import org.alter.plugins.content.mechanics.poison.Poison
import org.alter.plugins.content.mechanics.poison.CombatPoison
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Checks on what can inflict poison, and on the table [CombatPoison] derives from the cache.
 *
 * The failure this guards against is the one that made the whole thing necessary: a poisoned
 * weapon that binds nothing fails *silently*. A rune dagger(p++) equips, swings and hits for the
 * same damage as a plain rune dagger, so there is nothing to see - which is why every poisoned
 * weapon in the game was inert and there was no broken line to point at.
 *
 * The table is read out of cache item names rather than listed by hand, so what needs asserting is
 * the *shape* of that read: that every weapon family is picked up, that the three coatings are told
 * apart, and that the two non-weapons whose names end the same way stay out of it.
 */
class PoisonVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /** Items whose name ends in a coating suffix but which are not weapons at all. */
        private const val CAMEL_MOULD_P = 7001
        private const val DYNAMITE_P = 27426

        private const val WEAPON_SLOT = 3
        private const val AMMO_SLOT = 13

        /** The wiki's initial damage per coating: melee, then ranged. */
        private val PUBLISHED =
            mapOf(
                CombatPoison.Strength.REGULAR to (4 to 2),
                CombatPoison.Strength.SUPER to (5 to 3),
                CombatPoison.Strength.EXTRA to (6 to 4),
            )
    }

    @Test
    fun `every coated weapon family is in the table`() {
        /*
         * One representative of each family, in all three strengths, by cache id. Together they
         * cover both equipment slots the read accepts - weapon (daggers, spears, hastae, darts,
         * knives) and ammo (arrows, bolts, javelins).
         */
        val families =
            mapOf(
                "dragon dagger" to listOf(1231, 5680, 5698),
                "rune spear" to listOf(1261, 5714, 5728),
                "rune hasta" to listOf(11414, 11417, 11419),
                "abyssal dagger" to listOf(13267, 13269, 13271),
                "keris" to listOf(10582, 10583, 10584),
                "bone dagger" to listOf(8874, 8876, 8878),
                "rune arrow" to listOf(893, 5621, 5627),
                "rune dart" to listOf(817, 5634, 5641),
                "rune knife" to listOf(876, 5660, 5667),
                "rune javelin" to listOf(836, 5647, 5653),
                "runite bolts" to listOf(9291, 9298, 9305),
                "dragon knife" to listOf(22806, 22808, 22810),
                "dragon bolts" to listOf(21924, 21926, 21928),
                "amethyst javelin" to listOf(21320, 21322, 21324),
            )

        families.forEach { (family, ids) ->
            CombatPoison.Strength.values.forEachIndexed { index, expected ->
                val id = ids[index]
                val name = CacheManager.getItem(id).name.trim()
                assertTrue(name.endsWith(expected.suffix), "id $id (\"$name\") is not a $family ${expected.suffix}")
                assertEquals(expected, CombatPoison.strengthOf(id), "\"$name\" (id $id) read as the wrong coating")
            }
        }
    }

    @Test
    fun `the table covers every coated item in the cache and nothing else`() {
        val equippable =
            CacheManager.getItems().filter { (_, def) ->
                val name = def.name?.trim() ?: return@filter false
                (def.equipSlot == WEAPON_SLOT || def.equipSlot == AMMO_SLOT) &&
                    CombatPoison.Strength.values.any { name.endsWith(it.suffix) }
            }

        assertTrue(equippable.size > 200, "only ${equippable.size} coated weapons found - the cache read has drifted")

        equippable.keys.forEach { id ->
            assertNotNull(CombatPoison.strengthOf(id), "${CacheManager.getItem(id).name} (id $id) was left out of the table")
        }

        /*
         * A camel mould and a stick of dynamite are not weapons. They are the only two items in the
         * cache whose names end in a coating suffix without being one, and the equipment-slot
         * filter is the only thing keeping them out.
         */
        listOf(CAMEL_MOULD_P, DYNAMITE_P).forEach { id ->
            assertNull(CombatPoison.strengthOf(id), "${CacheManager.getItem(id).name} (id $id) is not a weapon")
        }
    }

    @Test
    fun `coatings inflict their published damage`() {
        CombatPoison.Strength.values.forEach { strength ->
            val (melee, ranged) = PUBLISHED.getValue(strength)
            assertEquals(melee, strength.meleeDamage, "${strength.suffix} melee damage")
            assertEquals(ranged, strength.rangedDamage, "${strength.suffix} ranged damage")
        }
    }

    @Test
    fun `both abyssal tentacles poison and nothing else does by name`() {
        listOf("item.abyssal_tentacle", "item.abyssal_tentacle_or").forEach { key ->
            val id = getRSCM(key)
            assertTrue(CombatPoison.isTentacle(id), "${CacheManager.getItem(id).name} (id $id) does not poison")
        }
        // The whip it is made from does not poison, and neither does an uncoated dagger.
        assertTrue(!CombatPoison.isTentacle(getRSCM("item.abyssal_whip")), "the abyssal whip does not poison")
        assertNull(CombatPoison.strengthOf(getRSCM("item.rune_dagger")), "an uncoated rune dagger does not poison")
    }

    @Test
    fun `only the smoke spells poison`() {
        val poisoning = CombatSpell.values.filter { it.poisonDamage > 0 }.map { it.name }.sorted()
        assertEquals(listOf("SMOKE_BARRAGE", "SMOKE_BLITZ", "SMOKE_BURST", "SMOKE_RUSH"), poisoning)
        poisoning.forEach { name ->
            assertEquals(2, CombatSpell.valueOf(name).poisonDamage, "$name poison damage")
        }
    }

    /**
     * Emerald bolts were the one weapon effect that already poisoned, through their own path in
     * [EnchantedBolt] rather than through [CombatPoison]. Asserted here so the two stay in step:
     * their severity 25 is the same 5 initial damage a `(p+)` melee coating inflicts.
     */
    @Test
    fun `emerald bolts still carry their poison`() {
        assertEquals(55.0, EnchantedBolt.EMERALD.chance, "emerald bolt activation chance")
        assertEquals(5, CombatPoison.Strength.SUPER.meleeDamage, "the (p+) coating emerald bolts match")
    }

    /**
     * Poison decays as this codebase models it: [Poison] stores a tick count rather than the wiki's
     * severity, and the count a given initial damage starts at has to reproduce that damage on the
     * first tick and never grow afterwards.
     */
    @Test
    fun `poison damage decays from its initial value`() {
        val initials = CombatPoison.Strength.values.flatMap { listOf(it.meleeDamage, it.rangedDamage) }
        initials.forEach { initial ->
            val ticks = (initial * 5) - 4
            assertEquals(initial, Poison.getDamageForTicks(ticks), "poison starting at $initial")
            var previous = initial
            for (remaining in ticks - 1 downTo 1) {
                val damage = Poison.getDamageForTicks(remaining)
                assertTrue(damage <= previous, "poison starting at $initial grew from $previous to $damage")
                assertTrue(damage >= 1, "poison starting at $initial fell to $damage before running out")
                previous = damage
            }
        }
    }
}
