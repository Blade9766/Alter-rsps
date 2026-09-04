package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.game.model.Tile
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.alter.tools.CacheCollision
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verify-before-wire checks for Varrock's four pubs, Aris and the tramp.
 *
 * The three things that silently break this kind of content, in the order they bite:
 *
 * 1. an RSCM key that does not resolve, which throws on plugin construction and takes the whole
 *    plugin's registrations down with it;
 * 2. an npc that does not actually carry the option the plugin binds with `onNpcOption`, which
 *    registers nothing and leaves a mute NPC standing there;
 * 3. a spawn tile that is blocked, which strands the NPC somewhere a player cannot reach.
 *
 * Ground-item spawns are deliberately *not* checked against collision - several of Varrock's sit on
 * tables or behind the west bank basement wall, and that is the published behaviour.
 */
class VarrockPubVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }
    }

    /** Every NPC these plugins spawn, with the tile it is spawned on. */
    private val spawns = mapOf(
        // Blue Moon Inn
        "npc.bartender_1312" to Tile(3226, 3398),
        "npc.dr_harlow" to Tile(3222, 3397),
        "npc.jonny_the_beard" to Tile(3223, 3395),
        "npc.cook_2895" to Tile(3230, 3400),
        "npc.barbarian_3262" to Tile(3225, 3402),
        "npc.woman_3015" to Tile(3218, 3395),
        "npc.man_3014" to Tile(3231, 3395, 1),
        // Dancing Donkey Inn
        "npc.bartender_1311" to Tile(3268, 3391),
        "npc.hops_1108" to Tile(3268, 3389),
        "npc.da_vinci_1104" to Tile(3273, 3389),
        "npc.chancy_1106" to Tile(3270, 3388),
        // Jolly Boar Inn
        "npc.bartender_1310" to Tile(3277, 3490),
        "npc.cook_2896" to Tile(3285, 3489),
        "npc.johnathon" to Tile(3278, 3505, 1),
        "npc.black_knight" to Tile(3277, 3505),
        "npc.man_3106" to Tile(3277, 3495, 1),
        "npc.woman_3111" to Tile(3278, 3502),
        "npc.woman_3112" to Tile(3279, 3496),
        // Ratpit Bar, Aris, the tramp
        "npc.barman" to Tile(2909, 5078),
        "npc.aris" to Tile(3203, 3424),
        "npc.tramp_3255" to Tile(3240, 3398),
    )

    /** Everyone the plugins bind a `talk-to` handler to. */
    private val talkTo = listOf(
        "npc.bartender_1312", "npc.dr_harlow", "npc.jonny_the_beard", "npc.cook_2895",
        "npc.barbarian_3262",
        "npc.bartender_1311", "npc.hops_1108", "npc.da_vinci_1104", "npc.chancy_1106",
        "npc.bartender_1310", "npc.cook_2896", "npc.johnathon",
        "npc.barman", "npc.aris", "npc.tramp_3255",
    )

    private val itemKeys = listOf(
        "item.beer", "item.stew", "item.cabbage", "item.coins_995",
        "item.gold_bar", "item.gold_ore", "item.ruby_ring", "item.brass_necklace",
        "item.bucket", "item.pie_dish", "item.leather_body", "item.jug",
        "item.pot", "item.thread", "item.body_rune", "item.logs",
    )

    @Test
    fun `every item key resolves`() {
        itemKeys.forEach { key ->
            assertNotNull(CacheManager.getItem(getRSCM(key)), "$key has no cache item")
        }
    }

    @Test
    fun `every talked-to npc really has a talk-to option`() {
        talkTo.forEach { key ->
            val def = CacheManager.getNpcs()[getRSCM(key)]
            assertNotNull(def, "$key has no cache npc")
            assertTrue(
                def.actions.any { it?.lowercase() == "talk-to" },
                "$key has no Talk-to option [actions=${def.actions.toList()}]",
            )
        }
    }

    /**
     * The Blue Moon Inn's Jonny must be the *non*-attackable id. He only becomes attackable partway
     * through Shield of Arrav; spawning 14139 or 5213 instead would put a killable quest NPC in a
     * pub with no quest behind it.
     */
    @Test
    fun `jonny the beard is spawned as the non-attackable version`() {
        val def = CacheManager.getNpcs()[getRSCM("npc.jonny_the_beard")]!!
        assertEquals(0, def.combatLevel, "actions=${def.actions.toList()}")
        assertFalse(def.actions.any { it?.lowercase() == "attack" })

        // ...and the attackable ones really do exist, so the choice above is a choice.
        assertTrue(CacheManager.getNpcs()[getRSCM("npc.jonny_the_beard_14139")]!!.combatLevel == 2)
    }

    /** The three pub residents a player can fight, at the levels their wiki pages publish. */
    @Test
    fun `the attackable pub residents are attackable at the right level`() {
        mapOf(
            "npc.barbarian_3262" to 8,
            "npc.black_knight" to 33,
            "npc.man_3106" to 2,
            "npc.woman_3111" to 2,
            "npc.woman_3112" to 2,
            "npc.man_3014" to 2,
            "npc.woman_3015" to 2,
        ).forEach { (key, level) ->
            val def = CacheManager.getNpcs()[getRSCM(key)]!!
            assertTrue(
                def.actions.any { it?.lowercase() == "attack" },
                "$key is not attackable [actions=${def.actions.toList()}]",
            )
            assertEquals(level, def.combatLevel, "$key cache level != wiki level")
        }
    }

    @Test
    fun `every spawn tile can be stood on`() {
        val regions = spawns.values.map { CacheCollision.regionOf(it.x, it.z) }.toSet()
        val scene = CacheCollision.load(regions)

        spawns.forEach { (key, tile) ->
            assertTrue(
                scene.canStandOn(tile),
                "$key spawns on a blocked tile [${tile.x}, ${tile.z}, ${tile.height}]",
            )
        }
    }

    /**
     * Regression guard for the four tiles that were moved off their published pin because the pin
     * landed on furniture. If a future cache makes them walkable the move can be undone - but the
     * move must never be undone silently, so this asserts the reason still holds.
     */
    @Test
    fun `the four relocated spawns are relocated for a reason`() {
        val published = listOf(
            Tile(3272, 3389), // Da Vinci - Dancing Donkey bar counter
            Tile(3271, 3388), // Chancy - the same counter
            Tile(3277, 3489), // Jolly Boar bartender - his own bar counter
            Tile(3204, 3425), // Aris - her crystal ball
        )
        val scene = CacheCollision.load(published.map { CacheCollision.regionOf(it.x, it.z) }.toSet())

        published.forEach { tile ->
            assertFalse(
                scene.canStandOn(tile),
                "[${tile.x}, ${tile.z}] is walkable now - the spawn can go back on its wiki tile",
            )
        }
    }
}
