package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.api.EquipmentType
import org.alter.api.Skills
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.service.game.ItemMetadataService
import org.alter.plugins.content.magic.ElementalStaves
import org.alter.plugins.content.magic.MagicSpells
import org.alter.rscm.RSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Checks on the elemental staff family: the rune table in [ElementalStaves], and the equip
 * requirements that gate it.
 *
 * Both halves fail silently in the game. A staff missing from the table simply consumes runes as
 * if it were an ordinary weapon, which looks like nothing at all until someone notices their air
 * runes going down with a staff of air equipped; and a requirement that fails to apply leaves a
 * mystic staff wieldable at level 1. Neither raises an error anywhere, so they are asserted here
 * against the cache instead.
 */
class ElementalStaffVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        private const val AIR = 556
        private const val WATER = 555
        private const val EARTH = 557
        private const val FIRE = 554

        /** Run once; the merge is additive and therefore idempotent, but not cheap. */
        private val loaded: Boolean by lazy {
            ItemMetadataService().loadAll()
            true
        }

        /**
         * Every staff the table is expected to cover, with the runes it supplies and the level it
         * should need in both Attack and Magic - or `null` for the basic staves, which need
         * nothing. Straight off the OSRS wiki's Elemental staff article.
         */
        private val EXPECTED =
            listOf(
                Staff(1381, "Staff of air", setOf(AIR), null),
                Staff(1383, "Staff of water", setOf(WATER), null),
                Staff(1385, "Staff of earth", setOf(EARTH), null),
                Staff(1387, "Staff of fire", setOf(FIRE), null),
                Staff(1397, "Air battlestaff", setOf(AIR), 30),
                Staff(1395, "Water battlestaff", setOf(WATER), 30),
                Staff(1399, "Earth battlestaff", setOf(EARTH), 30),
                Staff(1393, "Fire battlestaff", setOf(FIRE), 30),
                Staff(20730, "Mist battlestaff", setOf(AIR, WATER), 30),
                Staff(20736, "Dust battlestaff", setOf(AIR, EARTH), 30),
                Staff(11998, "Smoke battlestaff", setOf(AIR, FIRE), 30),
                Staff(6562, "Mud battlestaff", setOf(WATER, EARTH), 30),
                Staff(11787, "Steam battlestaff", setOf(WATER, FIRE), 30),
                Staff(12795, "Steam battlestaff", setOf(WATER, FIRE), 30),
                Staff(3053, "Lava battlestaff", setOf(EARTH, FIRE), 30),
                Staff(21198, "Lava battlestaff", setOf(EARTH, FIRE), 30),
                Staff(1405, "Mystic air staff", setOf(AIR), 40),
                Staff(1403, "Mystic water staff", setOf(WATER), 40),
                Staff(1407, "Mystic earth staff", setOf(EARTH), 40),
                Staff(1401, "Mystic fire staff", setOf(FIRE), 40),
                Staff(20733, "Mystic mist staff", setOf(AIR, WATER), 40),
                Staff(20739, "Mystic dust staff", setOf(AIR, EARTH), 40),
                Staff(12000, "Mystic smoke staff", setOf(AIR, FIRE), 40),
                Staff(6563, "Mystic mud staff", setOf(WATER, EARTH), 40),
                Staff(11789, "Mystic steam staff", setOf(WATER, FIRE), 40),
                Staff(12796, "Mystic steam staff", setOf(WATER, FIRE), 40),
                Staff(3054, "Mystic lava staff", setOf(EARTH, FIRE), 40),
                Staff(21200, "Mystic lava staff", setOf(EARTH, FIRE), 40),
            )

        private const val TWINFLAME = 30634

        /** The plain battlestaff supplies no runes, but takes the battlestaff requirements. */
        private const val PLAIN_BATTLESTAFF = 1391

        private const val BRYOPHYTA_UNCHARGED = 22368
        private const val BRYOPHYTA = 22370

        /** The client's two-autocast-button panel; see `AutocastInterface`. */
        private const val MAGIC_STAFF_WEAPON_TYPE = 18

        private const val WEAPON_SLOT = 3
    }

    private data class Staff(val id: Int, val name: String, val runes: Set<Int>, val tier: Int?)

    private fun reqsOf(item: Int): Map<Int, Int> {
        assertTrue(loaded)
        val reqs = CacheManager.getItem(item).skillReqs ?: return emptyMap()
        return reqs.entries.associate { it.key.toInt() to it.value.toInt() }
    }

    private fun named(): List<Pair<Int, String>> = EXPECTED.map { it.id to it.name } + (TWINFLAME to "Twinflame staff")

    /** A player holding nothing, wielding [weapon]. */
    private fun wielding(weapon: Int?): Player {
        val player = Player(World(EatingVerify.GAME_CONTEXT, EatingVerify.DEV_CONTEXT))
        if (weapon != null) {
            player.equipment[EquipmentType.WEAPON.id] = Item(weapon)
        }
        return player
    }

    /** The rune ids the constants above name, in case a cache bump moves them. */
    @Test
    fun `the basic runes are the ids the table is built on`() {
        mapOf(AIR to "Air rune", WATER to "Water rune", EARTH to "Earth rune", FIRE to "Fire rune")
            .forEach { (id, name) -> assertEquals(name, CacheManager.getItem(id).name, "rune id $id has moved") }
    }

    @Test
    fun `every staff supplies exactly the runes it should`() {
        val table = ElementalStaves.table()
        EXPECTED.forEach { staff ->
            val supplied = assertNotNull(table[staff.id], "${staff.name} (${staff.id}) is not in the rune table")
            assertEquals(staff.runes, supplied.toSet(), "${staff.name} (${staff.id}) supplies the wrong runes")
        }
        assertEquals(
            setOf(WATER, FIRE),
            table[TWINFLAME]?.toSet(),
            "the Twinflame staff should supply fire and water",
        )
    }

    /**
     * The table is keyed by RSCM name, so a moved id would quietly point an entry at some other
     * item. The name is the anchor, as in [EquipmentRequirementVerify].
     */
    @Test
    fun `every id in the table is a real equippable staff with the recorded name`() {
        named().forEach { (id, name) ->
            val def = CacheManager.getItem(id)
            assertNotNull(def, "id $id ($name) is not in the cache")
            assertEquals(name, def.name, "id $id is '${def.name}' in the cache, not '$name' - the id has moved")
            assertEquals(WEAPON_SLOT, def.equipSlot, "id $id ($name) is not a weapon, so it can never supply runes")
        }
    }

    @Test
    fun `the table holds nothing beyond the family`() {
        val known = EXPECTED.map { it.id }.toSet() + TWINFLAME
        val extra = ElementalStaves.table().keys - known
        assertTrue(
            extra.isEmpty(),
            "unexpected staves in the rune table: ${extra.map { it to CacheManager.getItem(it).name }}",
        )
    }

    /**
     * End to end through the real loader. The battlestaff numbers are the ones that were wrong:
     * the four craftable elemental battlestaves carried a *Crafting* requirement from the cache
     * (66 air, 62 fire, 58 earth, 54 water - the levels to make them) and no combat requirement
     * at all, so they were both unwieldable and ungated at once.
     */
    @Test
    fun `battlestaves and mystic staves are gated on Attack and Magic`() {
        EXPECTED.forEach { staff ->
            val reqs = reqsOf(staff.id)
            val tier = staff.tier
            if (tier == null) {
                assertTrue(reqs.isEmpty(), "${staff.name} (${staff.id}) should need nothing to wield, but needs $reqs")
            } else {
                assertEquals(tier, reqs[Skills.ATTACK], "${staff.name} (${staff.id}) should need $tier Attack")
                assertEquals(tier, reqs[Skills.MAGIC], "${staff.name} (${staff.id}) should need $tier Magic")
            }
        }
        assertEquals(
            mapOf(Skills.ATTACK to 30, Skills.MAGIC to 30),
            reqsOf(PLAIN_BATTLESTAFF),
            "Battlestaff",
        )
    }

    @Test
    fun `no staff is left gated on a skill it has nothing to do with`() {
        EXPECTED.forEach { staff ->
            val stray = reqsOf(staff.id).keys - setOf(Skills.ATTACK, Skills.MAGIC)
            assertTrue(stray.isEmpty(), "${staff.name} (${staff.id}) is gated on skill ids $stray")
        }
    }

    /**
     * The table read the way the spell code reads it: through the equipment slot of a real
     * [Player]. A staff has to answer for the runes it supplies and for nothing else - a lava
     * battlestaff that also claimed air runes would let it cast the whole Standard book free.
     */
    @Test
    fun `a wielded staff supplies its own runes and no others`() {
        val basic = setOf(AIR, WATER, EARTH, FIRE)
        (EXPECTED + Staff(TWINFLAME, "Twinflame staff", setOf(WATER, FIRE), 60)).forEach { staff ->
            val player = wielding(staff.id)
            staff.runes.forEach { rune ->
                assertTrue(
                    ElementalStaves.providesUnlimited(player, rune),
                    "${staff.name} (${staff.id}) should supply rune $rune",
                )
            }
            (basic - staff.runes).forEach { rune ->
                assertFalse(
                    ElementalStaves.providesUnlimited(player, rune),
                    "${staff.name} (${staff.id}) should not supply rune $rune",
                )
            }
        }
    }

    @Test
    fun `an empty weapon slot supplies nothing`() {
        val player = wielding(null)
        listOf(AIR, WATER, EARTH, FIRE).forEach {
            assertFalse(ElementalStaves.providesUnlimited(player, it), "bare hands supply rune $it")
        }
    }

    /**
     * The half of the contract that costs runes rather than granting them: `removeRunes` has to
     * leave the supplied runes in the inventory and take everything else. A staff that granted a
     * cast but still charged for it would be worse than no staff at all.
     */
    @Test
    fun `casting with a combination staff consumes only the runes it does not supply`() {
        val player = wielding(3053) // Lava battlestaff - earth and fire.
        player.inventory.add(EARTH, 10)
        player.inventory.add(FIRE, 10)
        player.inventory.add(AIR, 10)

        // Earth Wave: 5 air, 7 earth, 5 blood - reduced to the air and blood by the staff.
        MagicSpells.removeRunes(player, listOf(Item(EARTH, 7), Item(FIRE, 4), Item(AIR, 5)))

        assertEquals(10, player.inventory.getItemCount(EARTH), "earth runes should not be spent")
        assertEquals(10, player.inventory.getItemCount(FIRE), "fire runes should not be spent")
        assertEquals(5, player.inventory.getItemCount(AIR), "air runes should be spent")
    }

    @Test
    fun `the twinflame staff needs 60 Magic and no Attack`() {
        assertEquals(mapOf(Skills.MAGIC to 60), reqsOf(TWINFLAME), "Twinflame staff")
    }

    /**
     * Not an elemental staff, but its uncharged form carried the same Crafting-level-as-
     * requirement defect, and its charged form already declares Attack 30 / Magic 30 from the
     * cache - so charging the staff used to *lower* what it needed.
     */
    @Test
    fun `both states of Bryophytas staff need the same levels`() {
        val expected = mapOf(Skills.ATTACK to 30, Skills.MAGIC to 30)
        assertEquals(expected, reqsOf(BRYOPHYTA_UNCHARGED), "Bryophyta's staff (uncharged)")
        assertEquals(expected, reqsOf(BRYOPHYTA), "Bryophyta's staff")
    }

    /**
     * Unlimited runes are moot on a weapon the client gives no autocast buttons to, so every
     * staff in the table has to resolve to the magic staff panel.
     */
    @Test
    fun `every staff has the autocast weapon type`() {
        assertTrue(loaded)
        named().forEach { (id, name) ->
            assertEquals(
                MAGIC_STAFF_WEAPON_TYPE,
                CacheManager.getItem(id).weaponType,
                "$name ($id) has no autocast panel",
            )
        }
    }
}
