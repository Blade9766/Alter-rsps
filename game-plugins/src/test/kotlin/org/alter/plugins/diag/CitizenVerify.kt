package org.alter.plugins.diag

import com.fasterxml.jackson.databind.ObjectMapper
import dev.openrune.cache.CacheManager
import org.alter.api.BonusSlot
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.HerbDropTable
import org.alter.plugins.content.npcs.citizen.CitizenDrops
import org.alter.plugins.content.npcs.citizen.CitizenPlugin
import org.alter.plugins.content.npcs.citizen.Citizens
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verify-before-wire checks for `content/npcs/citizen` - every Man and Woman in the game.
 *
 * The interesting property here is **completeness**, not correctness of any one row. These
 * ids come from six wiki pages with overlapping id lists, several of them versioned, and the
 * failure mode that matters is an id nobody transcribed: it stays a 10-hitpoint punching bag
 * that drops nothing, and nothing at runtime says so. So the central test walks the *cache*
 * for attackable npcs named Man or Woman and demands the plugin covers each one, rather than
 * only checking the ids the plugin already lists.
 */
class CitizenVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /**
         * The unused `Man (level 4)` version 2. Combat level 0 and an empty option list in
         * this cache, so it is not a monster; see [Citizens]' West Ardougne comment.
         */
        const val UNUSED_MAN_ID = 1138
    }

    private val citizenDefs get() = CacheManager.getNpcs().filter { it.value.name == "Man" || it.value.name == "Woman" }

    private fun isAttackable(id: Int) =
        CacheManager.getNpcs()[id]?.actions?.any { it?.equals("Attack", ignoreCase = true) == true } == true

    @Test
    fun `every declared key resolves to an attackable Man or Woman at its wiki combat level`() {
        Citizens.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { key ->
                val id = getRSCM(key)
                assertTrue(id > 0, "$key (${variant.name}) did not resolve to an npc id.")
                val def = assertNotNull(CacheManager.getNpcs()[id], "$key ($id) is not in this cache.")
                assertTrue(
                    def.name == "Man" || def.name == "Woman",
                    "$key ($id) is named '${def.name}', not Man or Woman.",
                )
                assertTrue(
                    isAttackable(id),
                    "$key ($id, ${variant.name}) has no Attack option, so it is scenery rather than a " +
                        "monster [actions=${def.actions.toList()}].",
                )
                assertEquals(
                    variant.combatLevel,
                    def.combatLevel,
                    "$key ($id) is combat level ${def.combatLevel} in the cache but ${variant.combatLevel} " +
                        "on the wiki page this row was transcribed from (${variant.name}).",
                )
            }
        }
    }

    /**
     * The completeness check. Anything the cache calls an attackable Man or Woman must be
     * described, or it silently falls back to `NpcCombatDef.DEFAULT` and drops nothing.
     */
    @Test
    fun `every attackable Man and Woman in the cache is covered`() {
        val covered = Citizens.VARIANTS.flatMap { it.npcKeys }.map(::getRSCM).toSet()
        val attackable = citizenDefs.keys.filter { isAttackable(it) }.toSet()

        val missing = (attackable - covered).sorted()
        assertTrue(
            missing.isEmpty(),
            "Attackable Man/Woman ids with no combat def: " +
                missing.joinToString { "$it (${CacheManager.getNpcs()[it]?.name} lvl ${CacheManager.getNpcs()[it]?.combatLevel})" },
        )

        // And nothing is described that the cache would never let a player hit.
        assertTrue(
            UNUSED_MAN_ID !in covered,
            "npc $UNUSED_MAN_ID is the wiki's 'Unused' Man; it has no options in this cache and should stay undefined.",
        )
    }

    @Test
    fun `no npc id is claimed by two variants`() {
        val seen = mutableMapOf<Int, String>()
        Citizens.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { key ->
                val id = getRSCM(key)
                val previous = seen.put(id, variant.name)
                assertTrue(
                    previous == null,
                    "$key ($id) is declared by both '$previous' and '${variant.name}'; the later " +
                        "setCombatDef would silently win.",
                )
            }
        }
    }

    /**
     * The wiki's numerators are used as relative weights, which is only exact if they really do
     * add up. They do: 128 on the nose once the free-to-play 10-coin row is dropped and the
     * members `Nothing` row is the 8/128 one. Energy potion(3) is the single unpublished rate
     * and is excluded here for the same reason it is only approximated in the table.
     */
    @Test
    fun `the citizen drop table sums to the wiki's 128 slots`() {
        val energyPotion = getRSCM("item.energy_potion3")
        val published = CitizenDrops.TABLE.filter { it.item != energyPotion }.sumOf { it.weight }
        assertEquals(128, published, "Citizen drop weights no longer sum to the wiki's 128 slots.")
    }

    @Test
    fun `every drop table row resolves to a real item`() {
        val rows = CitizenDrops.TABLE + HerbDropTable.TABLE
        rows.forEach { drop ->
            val item = drop.item ?: return@forEach
            assertNotNull(CacheManager.getItems()[item], "Drop table row $item is not an item in this cache.")
            assertTrue(drop.min in 1..drop.max, "Drop table row $item has an impossible quantity ${drop.min}..${drop.max}.")
        }
        listOf("item.bones", "item.clue_scroll_beginner", "item.clue_scroll_easy", "item.looting_bag").forEach { key ->
            val id = getRSCM(key)
            assertNotNull(CacheManager.getItems()[id], "$key ($id) is not an item in this cache.")
        }
    }

    /**
     * The gap this package closed: `pickpockets.json` listed eight of the thirty citizens that
     * carry a `Pickpocket` option, so the rest showed the menu entry and did nothing.
     */
    @Test
    fun `every pickpocketable citizen is wired into the thieving config`() {
        val entry = manWomanPickpocketEntry()
        val configured = (entry["npcs"] as List<*>).map { it as String }.toSet()

        Citizens.PICKPOCKET_IDS.forEach { key ->
            val def = assertNotNull(CacheManager.getNpcs()[getRSCM(key)], "$key is not in this cache.")
            assertTrue(
                def.actions.any { it?.equals("Pickpocket", ignoreCase = true) == true },
                "$key is listed as pickpocketable but has no Pickpocket option [actions=${def.actions.toList()}].",
            )
            assertTrue(key in configured, "$key is pickpocketable in the cache but missing from pickpockets.json.")
        }

        // The reverse direction: every citizen in the cache with the option is on the list.
        val pickpocketable =
            citizenDefs
                .filter { (_, def) -> def.actions.any { it?.equals("Pickpocket", ignoreCase = true) == true } }
                .keys
                .sorted()
        val declared = Citizens.PICKPOCKET_IDS.map(::getRSCM).toSet()
        val missing = pickpocketable.filter { it !in declared }
        assertTrue(missing.isEmpty(), "Man/Woman ids with a Pickpocket option and no thieving entry: $missing")
    }

    /** The wiki publishes a flat 3 coins and a 4.8 second stun; both are easy to drift. */
    @Test
    fun `citizen pickpocketing pays the wiki's guaranteed three coins`() {
        val entry = manWomanPickpocketEntry()
        assertEquals(1, (entry["level"] as Number).toInt(), "Citizens are a level 1 Thieving target.")
        assertEquals(8.0, (entry["experience"] as Number).toDouble(), "Citizens give 8 Thieving xp.")

        val stun = entry["stun"] as Map<*, *>
        assertEquals(8, (stun["ticks"] as Number).toInt(), "The wiki's 4.8 second stun is 8 cycles.")

        val loot = entry["loot"] as List<*>
        assertEquals(1, loot.size, "The citizen pickpocket table is a single guaranteed row.")
        val row = loot.single() as Map<*, *>
        assertEquals("item.coins_995", row["item"])
        assertEquals(3, (row["min"] as Number).toInt())
        assertEquals(3, (row["max"] as Number).toInt())
    }

    /**
     * The plugin's `init` really runs here, which is the only way to catch the two failures a
     * data-only test cannot: `NpcCombatBuilder.setBonus`'s "Bonus already set" check throwing -
     * a plugin whose constructor throws registers *nothing*, silently - and a variant whose
     * numbers do not survive the round trip through the DSL.
     */
    @Test
    fun `the plugin builds and every citizen carries its own combat def`() {
        val world = World(EatingVerify.GAME_CONTEXT, EatingVerify.DEV_CONTEXT)
        val repo = PluginRepository(world)
        CitizenPlugin(repo, world, Server())
        val defs = repo.npcCombatDefs

        Citizens.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { key ->
                val def = assertNotNull(defs[getRSCM(key)], "$key (${variant.name}) registered no combat def.")
                assertEquals(variant.hitpoints, def.hitpoints, "$key hitpoints")
                assertEquals(variant.attack, def.attack, "$key attack")
                assertEquals(variant.strength, def.strength, "$key strength")
                assertEquals(variant.defence, def.defence, "$key defence")
                assertEquals(Citizens.ATTACK_SPEED, def.attackSpeed, "$key attack speed")
                assertEquals(Citizens.COMBAT_STYLE, def.combatStyle, "$key combat style")
                assertEquals(variant.respawnCycles, def.respawnDelay, "$key respawn delay")
                assertEquals(Citizens.ATTACK_ANIMATION, def.attackAnimation, "$key attack animation")
                assertEquals(Citizens.BLOCK_ANIMATION, def.blockAnimation, "$key block animation")
                assertEquals(listOf(Citizens.DEATH_ANIMATION), def.deathAnimation, "$key death animation")
                // NpcAggroPlugin only sweeps when the radius is > 0; the builder leaves it -1.
                assertTrue(def.aggressiveRadius <= 0, "$key is passive on every wiki page")
                assertEquals(
                    variant.defenceStab,
                    def.bonuses[BonusSlot.DEFENCE_STAB.id],
                    "$key stab defence bonus",
                )
                assertEquals(
                    variant.defenceRanged,
                    def.bonuses[BonusSlot.DEFENCE_RANGED.id],
                    "$key ranged defence bonus",
                )
            }
        }
    }

    private fun manWomanPickpocketEntry(): Map<*, *> {
        val json = Files.readString(Paths.get("../data", "cfg", "thieving", "pickpockets.json"))
        val entries = ObjectMapper().readValue(json, List::class.java)
        return assertNotNull(
            entries.filterIsInstance<Map<*, *>>().firstOrNull { entry ->
                (entry["npcs"] as? List<*>)?.contains("npc.man_3106") == true
            },
            "pickpockets.json no longer has an entry covering npc.man_3106.",
        )
    }
}
