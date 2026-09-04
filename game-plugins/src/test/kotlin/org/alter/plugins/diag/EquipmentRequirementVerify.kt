package org.alter.plugins.diag

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import dev.openrune.cache.CacheManager
import org.alter.api.Skills
import org.alter.game.service.game.ItemMetadataService
import org.junit.BeforeClass
import java.io.File
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Checks on `data/cfg/items/equipmentRequirements.yml`, the table that gates the equipment the
 * cache forgot to gate.
 *
 * The failure this guards against is silent in both directions. An id that has drifted to a
 * different item quietly puts a level requirement on the wrong thing, and a requirement that
 * fails to apply leaves a weapon wieldable at level 1 - neither surfaces as an error anywhere,
 * they just make Attack and Defence mean nothing. So the config is checked against the cache by
 * *name* rather than by id alone, and then the real loader is run and its output inspected,
 * rather than re-implementing the merge here and testing the re-implementation.
 */
class EquipmentRequirementVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
        }

        private val CONFIG = File("../data/cfg/items/equipmentRequirements.yml")

        private val SKILL_IDS =
            mapOf(
                "attack" to Skills.ATTACK,
                "defence" to Skills.DEFENCE,
                "strength" to Skills.STRENGTH,
                "ranged" to Skills.RANGED,
                "magic" to Skills.MAGIC,
            )

        /** The requirement param pairs, mirroring `ItemMetadataService.SKILL_REQ_PARAMS`. */
        private val SKILL_REQ_PARAMS = listOf(434 to 436, 435 to 437, 191 to 613, 579 to 614)

        /**
         * Run once. [ItemMetadataService.loadAll] mutates the shared cache definitions, so doing
         * it per test would repeat the whole merge - harmless, since it is additive and therefore
         * idempotent, but slow.
         */
        private val loaded: Boolean by lazy {
            ItemMetadataService().loadAll()
            true
        }
    }

    private val config: ItemMetadataService.EquipmentRequirements by lazy {
        YAMLMapper().readValue(CONFIG, ItemMetadataService.EquipmentRequirements::class.java)
    }

    private fun reqsOf(item: Int): Map<Int, Int> {
        assertTrue(loaded)
        val reqs = CacheManager.getItem(item).skillReqs ?: return emptyMap()
        return reqs.entries.associate { it.key.toInt() to it.value.toInt() }
    }

    /**
     * What the *cache alone* declares for an item, read from its raw params.
     *
     * `loadAll` never touches `params`, only the `skillReqs` map it derives from them, so this
     * stays the pre-merge truth even after the loader has run - which is what makes comparing the
     * two possible at all.
     */
    private fun cacheReqsOf(item: Int): Map<Int, Int> {
        val def = CacheManager.getItem(item)
        return SKILL_REQ_PARAMS.mapNotNull { (skillKey, levelKey) ->
            val skill = def.params?.get(skillKey) as? Int ?: return@mapNotNull null
            val level = def.params?.get(levelKey) as? Int ?: return@mapNotNull null
            if (level > 0) skill to level else null
        }.toMap()
    }

    @Test
    fun `the config parses and is not empty`() {
        assertTrue(CONFIG.exists(), "${CONFIG.path} is missing; every sub-rune item would be ungated")
        assertTrue(config.groups.isNotEmpty(), "the config declares no groups")
        assertTrue(config.groups.sumOf { it.items.size } > 400, "far fewer items than the ladder covers")
    }

    @Test
    fun `every group declares a usable requirement`() {
        config.groups.forEach { group ->
            assertTrue(group.skillReqs.isNotEmpty(), "a group lists items but no requirement")
            assertTrue(group.items.isNotEmpty(), "a group declares a requirement but no items")
            group.skillReqs.forEach { req ->
                val skill = assertNotNull(req.skill, "a requirement has no skill")
                val level = assertNotNull(req.level, "a $skill requirement has no level")
                assertTrue(skill in SKILL_IDS, "unknown skill '$skill' - the loader would throw on it")
                assertTrue(level in 1..99, "$skill level $level is out of range")
            }
            val skills = group.skillReqs.mapNotNull { it.skill }
            assertEquals(skills.size, skills.toSet().size, "a group names the same skill twice: $skills")
        }
    }

    /**
     * The name is the anchor. Ids move between cache revisions, and a moved id would gate some
     * unrelated item at level 60 with nothing to show it had happened.
     */
    @Test
    fun `every id is a real equippable item with the recorded name`() {
        config.groups.forEach { group ->
            group.items.forEach { entry ->
                val def = CacheManager.getItem(entry.id)
                assertNotNull(def, "id ${entry.id} (${entry.name}) is not in the cache")
                assertEquals(
                    entry.name,
                    def.name,
                    "id ${entry.id} is '${def.name}' in the cache, not '${entry.name}' - the id has moved",
                )
                assertTrue(
                    def.equipSlot >= 0,
                    "id ${entry.id} (${entry.name}) is not equippable, so gating it does nothing",
                )
            }
        }
    }

    @Test
    fun `no item is listed twice`() {
        val seen = mutableMapOf<Int, String>()
        config.groups.forEach { group ->
            val label = group.skillReqs.joinToString { "${it.skill} ${it.level}" }
            group.items.forEach { entry ->
                val previous = seen.put(entry.id, label)
                assertTrue(
                    previous == null,
                    "id ${entry.id} (${entry.name}) appears twice: '$previous' and '$label'",
                )
            }
        }
    }

    /**
     * End to end through the real loader: the ladder every melee weapon hangs off. These are the
     * levels the OSRS wiki's Attack article gives for each metal tier.
     */
    @Test
    fun `the metal ladder gates weapons on Attack`() {
        mapOf(
            1325 to 5, // Steel scimitar
            1327 to 10, // Black scimitar
            1329 to 20, // Mithril scimitar
            1331 to 30, // Adamant scimitar
            1333 to 40, // Rune scimitar - carried by the cache, not this config
            4587 to 60, // Dragon scimitar - likewise
            1377 to 60, // Dragon battleaxe - the cache had nothing for it
            20559 to 60, // Dragon 2h sword - likewise
        ).forEach { (item, level) ->
            assertEquals(
                level,
                reqsOf(item)[Skills.ATTACK],
                "${CacheManager.getItem(item).name} should need $level Attack",
            )
        }
    }

    @Test
    fun `the metal ladder gates armour on Defence`() {
        mapOf(
            1119 to 5, // Steel platebody
            1125 to 10, // Black platebody
            1121 to 20, // Mithril platebody
            1123 to 30, // Adamant platebody
            1127 to 40, // Rune platebody - carried by the cache
            20428 to 60, // Dragon chainbody
        ).forEach { (item, level) ->
            assertEquals(
                level,
                reqsOf(item)[Skills.DEFENCE],
                "${CacheManager.getItem(item).name} should need $level Defence",
            )
        }
    }

    /** The handful of pieces gated on two skills at once. */
    @Test
    fun `dual requirements are both applied`() {
        assertEquals(mapOf(Skills.ATTACK to 60, Skills.STRENGTH to 30), reqsOf(3204), "Dragon halberd")
        assertEquals(mapOf(Skills.DEFENCE to 50, Skills.STRENGTH to 50), reqsOf(10564), "Granite body")
        assertEquals(mapOf(Skills.DEFENCE to 50, Skills.STRENGTH to 50), reqsOf(21643), "Granite boots")
        assertEquals(mapOf(Skills.ATTACK to 50, Skills.STRENGTH to 50), reqsOf(12848), "Granite maul variant")
    }

    /**
     * The merge is additive, and these are the cases that prove it. Id 4153 already carries
     * Attack 50 and Strength 50 from the cache params and is not in the config at all; the
     * Barrows staff gets its pair from a document in `itemOverrides/`, which the loader runs
     * after. A loader that replaced rather than merged would break the second of these.
     */
    @Test
    fun `existing requirements are never weakened`() {
        assertEquals(mapOf(Skills.ATTACK to 50, Skills.STRENGTH to 50), reqsOf(4153), "Granite maul")
        assertEquals(mapOf(Skills.MAGIC to 70, Skills.ATTACK to 70), reqsOf(4710), "Ahrim's staff")
        assertEquals(70, reqsOf(4151)[Skills.ATTACK], "Abyssal whip")
    }

    /**
     * The Ranged ladder, which is a different ladder - launchers carry the requirement and the
     * higher bolts take theirs from the crossbow that fires them, not from their metal.
     */
    @Test
    fun `the ranged ladder gates launchers and the higher bolts`() {
        mapOf(
            9176 to 16, // Blurite crossbow
            9177 to 26, // Iron crossbow
            9179 to 31, // Steel crossbow
            9181 to 36, // Mithril crossbow
            9183 to 46, // Adamant crossbow
            845 to 5, // Oak longbow
            847 to 20, // Willow longbow
            855 to 40, // Yew longbow
            859 to 50, // Magic longbow
            808 to 5, // Steel dart
            3093 to 10, // Black dart
            25849 to 50, // Amethyst dart
            21207 to 61, // Dragon thrownaxe - the variant; 20849 carries 61 from the cache
            9290 to 46, // Adamant bolts (p)
        ).forEach { (item, level) ->
            assertEquals(
                level,
                reqsOf(item)[Skills.RANGED],
                "${CacheManager.getItem(item).name} should need $level Ranged",
            )
        }
    }

    /**
     * Arrows and javelins carry **no** Ranged requirement in OSRS - they are gated by the bow or
     * ballista that fires them, and their wiki infoboxes have no requirement field at all. The
     * cache agrees, leaving every one of them ungated.
     *
     * Asserted rather than merely commented because "adamant arrows are ungated" looks exactly
     * like an omission, and the obvious fix - adding them on the metal ladder - would be wrong.
     */
    @Test
    fun `arrows and javelins are deliberately absent`() {
        val wrong =
            config.groups.flatMap { group ->
                group.items.filter { entry ->
                    val name = entry.name?.lowercase() ?: return@filter false
                    ("arrow" in name || "javelin" in name) && "brutal" !in name
                }.map { "${it.id} ${it.name}" }
            }
        assertTrue(
            wrong.isEmpty(),
            "arrows and javelins have no Ranged requirement in OSRS, but the config gates these: $wrong",
        )
    }

    /**
     * Where the config and the cache both speak for the same item and skill, they must agree.
     *
     * This is the one failure the additive merge would otherwise hide completely. The loader
     * skips a skill the cache already declares, so a config entry with the *wrong* level for such
     * an item has no effect at all - the requirement looks correct in game while the file quietly
     * says something false, and the next person to read it believes the file.
     *
     * Twelve items are in this position today (Granite hammer and longsword, Barrelchest anchor,
     * Tzhaar-ket-om, Dragon warhammer, Elder maul, Scythe of vitur), and all twelve agree - which
     * is a free check on the wiki figures the rest of the Strength section rests on.
     */
    @Test
    fun `the config never contradicts the cache`() {
        val conflicts = mutableListOf<String>()
        config.groups.forEach { group ->
            group.items.forEach { entry ->
                val fromCache = cacheReqsOf(entry.id)
                group.skillReqs.forEach { req ->
                    val skill = SKILL_IDS[req.skill] ?: return@forEach
                    val level = req.level ?: return@forEach
                    val cached = fromCache[skill] ?: return@forEach
                    if (cached != level) {
                        conflicts += "${entry.id} ${entry.name}: config says ${req.skill} $level, cache says $cached"
                    }
                }
            }
        }
        assertTrue(conflicts.isEmpty(), "the config disagrees with the cache: $conflicts")
    }

    /**
     * Nothing listed in the config should still be reachable below its tier. A level 1 account
     * wearing full adamant is the bug this table exists to fix, so it is asserted across every
     * entry rather than only through the spot checks above.
     */
    @Test
    fun `no configured item is left ungated`() {
        val ungated =
            config.groups.flatMap { group ->
                group.items.filter { entry ->
                    val applied = reqsOf(entry.id)
                    group.skillReqs.any { req ->
                        val skill = SKILL_IDS[req.skill] ?: return@any false
                        applied[skill] == null
                    }
                }.map { "${it.id} ${it.name}" }
            }
        assertTrue(ungated.isEmpty(), "these stayed ungated after loading: $ungated")
    }
}
