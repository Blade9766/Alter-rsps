package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.api.BonusSlot
import org.alter.game.service.game.ItemMetadataService
import org.junit.BeforeClass
import java.io.File
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Checks on the equipment bonuses every item ends up with, and on the two files in
 * `data/cfg/items/itemOverrides/stats/` that correct the ones the cache gets wrong.
 *
 * These numbers fail quietly. A bow with no ranged strength still fires, a wand swinging at seven
 * ticks instead of four still casts, and nothing in the log says either is happening - the damage
 * is simply wrong for as long as nobody works the maths out by hand. So the values are pinned here
 * against the live game's, and the override files are checked against the cache by *name*, the way
 * [EquipmentRequirementVerify] does, so an id that moves between cache revisions is caught rather
 * than silently restating some unrelated item's bonuses.
 */
class EquipmentBonusVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
        }

        private val OVERRIDES = File("../data/cfg/items/itemOverrides/stats")

        /**
         * Positions in `ItemType.bonuses` past the ten [BonusSlot] covers. `PlayerExt` reads them
         * at these same indices.
         */
        private const val MELEE_STRENGTH = 10
        private const val RANGED_STRENGTH = 11
        private const val MAGIC_DAMAGE = 12

        /**
         * Run once: [ItemMetadataService.loadAll] rebuilds every definition in the shared cache,
         * so there is nothing to gain from repeating it per test.
         */
        private val loaded: Boolean by lazy {
            ItemMetadataService().loadAll()
            true
        }
    }

    private fun bonus(
        item: Int,
        slot: Int,
    ): Int {
        assertTrue(loaded)
        return CacheManager.getItem(item).bonuses[slot]
    }

    private fun bonus(
        item: Int,
        slot: BonusSlot,
    ): Int = bonus(item, slot.id)

    private fun speed(item: Int): Int {
        assertTrue(loaded)
        return CacheManager.getItem(item).attackSpeed
    }

    /**
     * Every `id`/`name` pair an override file declares, read straight out of the YAML rather than
     * through Jackson - the point is to check the file's own text, not a parse of it.
     */
    private fun declaredNames(file: File): List<Pair<Int, String>> {
        val out = mutableListOf<Pair<Int, String>>()
        var id: Int? = null
        file.readLines().forEach { line ->
            when {
                line.startsWith("id: ") -> id = line.removePrefix("id: ").trim().toInt()
                line.startsWith("name: ") -> {
                    val name = line.removePrefix("name: ").trim().removeSurrounding("\"")
                    out += assertNotNull(id, "a name with no id above it in ${file.name}") to name
                    id = null
                }
            }
        }
        return out
    }

    @Test
    fun `both override files exist and are populated`() {
        mapOf("combat_bonuses.yml" to 100, "attack_speeds.yml" to 150).forEach { (name, least) ->
            val file = File(OVERRIDES, name)
            assertTrue(file.exists(), "${file.path} is missing")
            val declared = declaredNames(file).size
            assertTrue(declared >= least, "${file.name} declares $declared items, expected at least $least")
        }
    }

    /**
     * The name is the anchor. An id that has drifted to a different item would hand some unrelated
     * piece of equipment the twisted bow's ranged strength with nothing to show for it.
     */
    @Test
    fun `every overridden id is the equippable item the file names`() {
        OVERRIDES.listFiles()?.forEach { file ->
            declaredNames(file).forEach { (id, name) ->
                val def = assertNotNull(CacheManager.getItem(id), "id $id ($name) is not in the cache")
                assertEquals(name, def.name, "id $id is '${def.name}' in the cache, not '$name' - the id has moved")
                assertTrue(
                    def.equipSlot >= 0,
                    "id $id ($name) is not equippable, so overriding its bonuses does nothing",
                )
            }
        }
    }

    /**
     * Cache param 189 carries ranged strength for the items param 12 does not, and reading only the
     * latter left every one of these at +0 - a twisted bow hitting like an unstrung one. None of
     * these are written by an override file; they come straight from the cache.
     */
    @Test
    fun `ranged strength is read from both cache params`() {
        mapOf(
            20997 to 20, // Twisted bow
            21000 to 10, // Twisted buckler
            19547 to 5, // Necklace of anguish
            27229 to 4, // Masori body
            27226 to 2, // Masori mask
            27232 to 2, // Masori chaps
            22109 to 2, // Ava's assembler
            28951 to 3, // Dizana's quiver
            19481 to 15, // Heavy ballista
            27610 to 25, // Venator bow
            29591 to 40, // Scorching bow
            11926 to 4, // Odium ward
            26235 to 2, // Zaryte vambraces
            28310 to 2, // Venator ring
            22002 to 8, // Dragonfire ward
            892 to 49, // Rune arrow - the param 12 side, which already worked
            11212 to 60, // Dragon arrow - likewise
        ).forEach { (item, expected) ->
            assertEquals(
                expected,
                bonus(item, RANGED_STRENGTH),
                "${CacheManager.getItem(item).name} should have +$expected ranged strength",
            )
        }
    }

    /**
     * Salamanders keep a hidden magic strength in param 65, and it stays out of the equipment
     * bonuses on purpose: the live game neither shows it nor applies it to anything but the Blaze
     * attack style, which `SalamanderCombatStrategy` computes from its own copy of the numbers.
     * Folding it in here would multiply every ordinary spell cast while holding one.
     */
    @Test
    fun `salamander magic strength stays out of the equipment bonuses`() {
        listOf(10146, 10147, 10148, 10149, 28834).forEach { item ->
            assertEquals(
                0,
                bonus(item, MAGIC_DAMAGE),
                "${CacheManager.getItem(item).name} should show no magic damage bonus",
            )
        }
    }

    /** Magic damage is stored as whole percent, however the cache spells it. */
    @Test
    fun `magic damage is in whole percent`() {
        assertEquals(15, bonus(21006, MAGIC_DAMAGE), "Kodai wand, from param 299 (150 tenths)")
        assertEquals(5, bonus(12002, MAGIC_DAMAGE), "Occult necklace, from param 299 (50 tenths)")
        assertEquals(10, bonus(6914, MAGIC_DAMAGE), "Master wand, from combat_bonuses.yml")
        assertEquals(10, bonus(27624, MAGIC_DAMAGE), "Ancient sceptre, raised from 5% to 10%")
    }

    /**
     * The bonus corrections that matter most: rebalances made after this cache was built, and
     * charged variants the cache hands their uncharged stats to.
     */
    @Test
    fun `the override file's bonus corrections are applied`() {
        assertEquals(100, bonus(22324, BonusSlot.ATTACK_STAB), "Ghrazi rapier stab")
        assertEquals(93, bonus(22324, MELEE_STRENGTH), "Ghrazi rapier strength")
        assertEquals(100, bonus(23995, BonusSlot.ATTACK_SLASH), "Blade of Saeldor slash")
        assertEquals(102, bonus(24417, BonusSlot.ATTACK_CRUSH), "Inquisitor's mace crush")
        assertEquals(96, bonus(24417, MELEE_STRENGTH), "Inquisitor's mace strength")
        assertEquals(125, bonus(28338, MELEE_STRENGTH), "Soulreaper axe strength")
        assertEquals(70, bonus(11283, BonusSlot.DEFENCE_STAB), "Charged dragonfire shield")
        assertEquals(72, bonus(21633, BonusSlot.DEFENCE_STAB), "Charged ancient wyvern shield")
        assertEquals(70, bonus(11749, RANGED_STRENGTH), "Crystal bow (i), which had no bonuses at all")
        assertEquals(12, bonus(21739, BonusSlot.DEFENCE_RANGED), "Granite ring")
    }

    /**
     * An override may only write the fields it declares. Correcting the charged dragonfire
     * shield's four defences must not wipe the magic attack and strength the cache gives it.
     */
    @Test
    fun `an override leaves the fields it does not declare alone`() {
        assertEquals(-10, bonus(11283, BonusSlot.ATTACK_MAGIC), "Dragonfire shield magic attack")
        assertEquals(7, bonus(11283, MELEE_STRENGTH), "Dragonfire shield strength")
        assertEquals(-5, bonus(11283, BonusSlot.ATTACK_RANGED), "Dragonfire shield ranged attack")
    }

    /**
     * Cache param 14 is absent on 432 of the 1,443 weapons here, and the loader's fallback of
     * seven ticks is slower than all but the heaviest two-handers.
     */
    @Test
    fun `weapons without a cache attack rate get their real speed`() {
        mapOf(
            21006 to 4, // Kodai wand
            6914 to 4, // Master wand
            11791 to 4, // Staff of the dead
            22296 to 4, // Staff of Light
            12904 to 4, // Toxic staff of the dead
            4599 to 4, // Oak blackjack
            23900 to 4, // Crystal staff (perfected)
            23897 to 4, // Crystal halberd (perfected)
            4890 to 7, // Dharok's greataxe, fully degraded
            4986 to 5, // Verac's flail, fully degraded
            4866 to 6, // Ahrim's staff, fully degraded
            11748 to 5, // Crystal bow (i)
        ).forEach { (item, expected) ->
            assertEquals(expected, speed(item), "${CacheManager.getItem(item).name} attack speed")
        }
    }

    /** Weapons the cache does give a rate keep it. */
    @Test
    fun `cache attack rates are not disturbed`() {
        assertEquals(4, speed(4151), "Abyssal whip")
        assertEquals(6, speed(20997), "Twisted bow")
        assertEquals(7, speed(4718), "Dharok's greataxe")
        assertEquals(9, speed(11235), "Dark bow")
    }
}
