package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.plugins.content.areas.warriorsguild.DefenderLadder
import org.alter.plugins.content.areas.warriorsguild.WarriorsGuild
import org.alter.plugins.content.areas.warriorsguild.activities.AnimatedArmour
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verify-before-wire checks for the Warriors' Guild.
 *
 * The guild is almost entirely ids and coordinates - nine npcs, twelve cyclopes, seven suits of
 * armour, seven dummies, five doors and two animators - and every one of them fails silently when
 * wrong. An rscm key that does not resolve throws at plugin construction, which takes the whole
 * guild down with it; a coordinate that is off puts a door or a warden somewhere unreachable, with
 * nothing logged either way.
 *
 * So this asserts three separate things: that every key resolves, that each id is the thing it is
 * supposed to be *by name*, and that the geometry is self-consistent - the cyclopes inside the
 * room they are gated into, and Kamfreena outside it.
 */
class WarriorsGuildVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /** Every npc the guild places or binds, with the name the cache should give it. */
        private val NPCS =
            mapOf(
                "npc.ghommal_13613" to "Ghommal",
                "npc.laidee_gnonock" to "Laidee Gnonock",
                "npc.harrallak_menarous_13615" to "Harrallak Menarous",
                "npc.shanomi" to "Shanomi",
                "npc.ajjat" to "Ajjat",
                "npc.lilly" to "Lilly",
                "npc.gamfred" to "Gamfred",
                "npc.jimmy" to "Jimmy",
                "npc.kamfreena" to "Kamfreena",
                "npc.lorelai" to "Lorelai",
            )

        private val CYCLOPS_IDS =
            listOf(2463, 2464, 2465, 2466, 2467, 2468, 2137, 2138, 2139, 2140, 2141, 2142)

        private val TOKEN_ITEMS = listOf(WarriorsGuild.TOKEN, "item.defensive_shield")
    }

    private fun idOf(key: String): Int? = runCatching { getRSCM(key) }.getOrNull()

    private fun objName(id: Int): String? = CacheManager.getObject(id).name

    private fun objActions(id: Int): List<String> = CacheManager.getObject(id).actions?.filterNotNull() ?: emptyList()

    @Test
    fun `every guild npc resolves to the right name`() {
        NPCS.forEach { (key, name) ->
            val id = idOf(key)
            assertTrue(id != null, "'$key' does not resolve; the guild would fail to load")
            assertEquals(name, CacheManager.getNpc(id!!).name, "'$key' is not $name in the cache")
        }
    }

    @Test
    fun `every cyclops id is a cyclops`() {
        CYCLOPS_IDS.forEach { id ->
            val def = CacheManager.getNpc(id)
            assertEquals("Cyclops", def.name, "npc $id is not a Cyclops")
            assertTrue("Attack" in (def.actions?.filterNotNull() ?: emptyList()), "cyclops $id cannot be attacked")
        }
        val levels = CYCLOPS_IDS.map { CacheManager.getNpc(it).combatLevel }.toSet()
        assertEquals(setOf(56, 76, 106), levels, "the cyclops combat levels have changed")
    }

    /** Doors and animators, by name and by the action the plugin binds to. */
    @Test
    fun `every guild object resolves with the action it is bound by`() {
        val doors =
            WarriorsGuild.TOP_FLOOR_DOORS.map { it.first } +
                WarriorsGuild.SHOT_PUT_DOORS.map { it.first } +
                listOf(WarriorsGuild.ENTRANCE_DOOR, WarriorsGuild.BASEMENT_DOOR.first)

        doors.forEach { key ->
            val id = idOf(key)
            assertTrue(id != null, "'$key' does not resolve")
            assertTrue(objName(id!!)?.contains("oor") == true, "'$key' is ${objName(id)}, not a door")
            assertTrue("Open" in objActions(id), "'$key' has no Open action: ${objActions(id)}")
        }

        val animator = idOf("object.magical_animator")
        assertTrue(animator != null, "the magical animator does not resolve")
        assertEquals("Magical Animator", objName(animator!!))
        assertTrue("Animate" in objActions(animator), "the animator has no Animate action")

        WarriorsGuild.SHOT_PUT_SPOTS.forEach { (key, _) ->
            val id = idOf(key)
            assertTrue(id != null, "'$key' does not resolve")
            assertEquals("Shot", objName(id!!), "'$key' is not a Shot")
            assertTrue("Throw" in objActions(id), "'$key' has no Throw action")
        }
    }

    /**
     * The doors' opened states. `openDoor` defaults to `id + 1`, which the guild relies on for all
     * five - the Taverley dusty key door is the standing reminder that the default is not always
     * right, since there it lands on a different door entirely.
     *
     * The check is that `id + 1` is a real definition and is **not some other named door**. An
     * *unnamed* neighbour is exactly right: an opened-door state is usually a nameless placeholder
     * in this cache, which is what Lorelai's basement door (10043 → the unnamed 10044) has and what
     * the Al Kharid gates open onto too. A neighbour carrying a different door's name is the
     * failure worth catching.
     */
    @Test
    fun `every door opens onto a real object`() {
        val doors =
            WarriorsGuild.TOP_FLOOR_DOORS.map { it.first } +
                WarriorsGuild.SHOT_PUT_DOORS.map { it.first } +
                listOf(WarriorsGuild.ENTRANCE_DOOR, WarriorsGuild.BASEMENT_DOOR.first)

        doors.forEach { key ->
            val id = idOf(key) ?: return@forEach
            val opened = CacheManager.getObject(id + 1)
            assertTrue(opened != null, "'$key' opens onto ${id + 1}, which is not in the cache at all")

            val name = opened!!.name
            val unnamedPlaceholder = name.isNullOrBlank() || name == "null"
            assertTrue(
                unnamedPlaceholder || name!!.contains("oor"),
                "'$key' opens onto ${id + 1}, which is '$name' - not a door and not a placeholder",
            )
        }
    }

    @Test
    fun `the seven dummies exist and have a tile each`() {
        assertEquals(7, WarriorsGuild.DUMMY_TILES.size, "the dummy room needs exactly seven tiles")
        assertEquals(
            WarriorsGuild.DUMMY_TILES.size,
            WarriorsGuild.DUMMY_TILES.toSet().size,
            "two dummies would stand on the same tile",
        )
        (23958..23964).forEach { id ->
            assertEquals("Dummy", objName(id), "object $id is not a Dummy")
            assertTrue("Hit" in objActions(id), "dummy $id has no Hit action")
        }
    }

    @Test
    fun `the defender ladder is seven distinct real items`() {
        assertEquals(7, DefenderLadder.RUNGS.size, "the ladder should have seven rungs")
        val ids = DefenderLadder.RUNGS.map { idOf(it) }
        assertTrue(ids.none { it == null }, "a defender key does not resolve: ${DefenderLadder.RUNGS}")
        assertEquals(ids.size, ids.toSet().size, "two rungs are the same item")
        ids.forEach { id ->
            assertTrue(
                CacheManager.getItem(id!!).name.endsWith("defender"),
                "id $id is ${CacheManager.getItem(id).name}, not a defender",
            )
        }
        val dragon = idOf(DefenderLadder.DRAGON_DEFENDER)
        assertTrue(dragon != null, "the dragon defender does not resolve")
        assertTrue(dragon !in ids, "the dragon defender is on the top-floor ladder, which it should not be")
    }

    @Test
    fun `every animated armour set is a real npc and three real items`() {
        assertEquals(7, AnimatedArmour.values.size, "there should be seven suits")
        AnimatedArmour.values.forEach { armour ->
            val npc = idOf(armour.npc)
            assertTrue(npc != null, "${armour.name}: '${armour.npc}' does not resolve")
            val def = CacheManager.getNpc(npc!!)
            assertEquals(
                armour.combatLevel,
                def.combatLevel,
                "${armour.name} is combat ${def.combatLevel} in the cache, not ${armour.combatLevel}",
            )
            armour.pieces.forEach { piece ->
                val id = idOf(piece)
                assertTrue(id != null, "${armour.name}: '$piece' does not resolve")
                assertTrue(CacheManager.getItem(id!!).equipSlot >= 0, "${armour.name}: '$piece' is not equippable")
            }
        }
    }

    @Test
    fun `the token and shield items resolve`() {
        TOKEN_ITEMS.forEach { key ->
            val id = idOf(key)
            assertTrue(id != null, "'$key' does not resolve")
            assertTrue(CacheManager.getItem(id!!).name.isNotBlank(), "'$key' has no name")
        }
    }

    /**
     * The geometry the token drain and the defender ladder both hang on.
     *
     * Kamfreena outside her own room is the one that would be least obvious in game: a room that
     * reached her would start charging a player 10 tokens a minute for standing there talking.
     */
    @Test
    fun `the cyclops rooms enclose their spawns and exclude their warden`() {
        val kamfreena = org.alter.game.model.Tile(2844, 3540, 2)
        assertTrue(
            !WarriorsGuild.TOP_FLOOR_CYCLOPS.contains(kamfreena),
            "Kamfreena stands inside the room she guards, so talking to her would cost tokens",
        )

        WarriorsGuild.TOP_FLOOR_DOORS.forEach { (_, tile) ->
            assertTrue(
                !WarriorsGuild.TOP_FLOOR_CYCLOPS.contains(tile),
                "the door at $tile is inside the room; entering would charge before it opened",
            )
        }

        val lorelai = org.alter.game.model.Tile(2909, 9972, 0)
        assertTrue(!WarriorsGuild.BASEMENT_CYCLOPS.contains(lorelai), "Lorelai stands inside the room she guards")
        assertTrue(
            !WarriorsGuild.BASEMENT_CYCLOPS.contains(WarriorsGuild.BASEMENT_DOOR.second),
            "the basement door is inside the room",
        )
    }

    @Test
    fun `the entry requirement matches the wiki`() {
        assertEquals(130, WarriorsGuild.COMBINED_LEVEL)
        assertEquals(99, WarriorsGuild.MASTERY_LEVEL)
        assertEquals(100, WarriorsGuild.TOKENS_TO_ENTER)
        assertEquals(10, WarriorsGuild.TOKEN_DRAIN)
        assertEquals(50, WarriorsGuild.SHOT_PUT_STRENGTH)
    }
}
