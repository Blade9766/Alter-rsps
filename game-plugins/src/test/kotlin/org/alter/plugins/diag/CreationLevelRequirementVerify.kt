package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.api.Skills
import org.alter.game.service.game.ItemMetadataService
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Checks on `itemOverrides/creation_level_requirements.yml`, the file that clears the equip
 * requirements the cache never meant as equip requirements.
 *
 * Cache param pairs 434/436 and friends hold a skill id and a level, and `ItemMetadataService`
 * reads all of them as requirements to wear the item. Most are. Some are the level needed to
 * *make* the item, or the entry level of the activity it drops from, and there is nothing in the
 * data separating the two - so a tiara needed 23 Crafting to put on.
 *
 * The failure is silent both ways, which is what earns a test. An override that stops applying
 * leaves an item ungettable-on for no visible reason; an override that clears too much silently
 * hands out gear that should be gated. So this asserts both halves: the listed items end up with
 * no requirement at all, **and** the genuine non-combat requirements elsewhere in the cache -
 * Agility on crystal gear, Hitpoints on the torture jewellery, Slayer on the leaf-bladed weapons -
 * still stand.
 */
class CreationLevelRequirementVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
        }

        /**
         * Run once. [ItemMetadataService.loadAll] mutates the shared cache definitions; doing it
         * per test would repeat the whole merge.
         */
        private val loaded: Boolean by lazy {
            ItemMetadataService().loadAll()
            true
        }

        /**
         * Every item the override file clears, with the level it used to be gated on and the
         * skill that level belongs to. Checked against the OSRS wiki: none of these has a wield
         * requirement, and each level below is the level to make the item - except the Bruma
         * torches, whose 50 Firemaking is the Wintertodt entry requirement.
         */
        private val CLEARED =
            listOf(
                Cleared(2961, "Silver sickle", Skills.CRAFTING, 18),
                Cleared(5525, "Tiara", Skills.CRAFTING, 23),
                Cleared(26788, "Gold tiara", Skills.CRAFTING, 42),
                Cleared(7637, "Silvthrill rod", Skills.CRAFTING, 25),
                Cleared(21081, "Opal ring", Skills.CRAFTING, 1),
                Cleared(21084, "Jade ring", Skills.CRAFTING, 13),
                Cleared(21087, "Topaz ring", Skills.CRAFTING, 16),
                Cleared(21090, "Opal necklace", Skills.CRAFTING, 16),
                Cleared(21093, "Jade necklace", Skills.CRAFTING, 25),
                Cleared(21096, "Topaz necklace", Skills.CRAFTING, 32),
                Cleared(21117, "Opal bracelet", Skills.CRAFTING, 22),
                Cleared(21120, "Jade bracelet", Skills.CRAFTING, 29),
                Cleared(21123, "Topaz bracelet", Skills.CRAFTING, 38),
                Cleared(20720, "Bruma torch", Skills.FIREMAKING, 50),
                Cleared(29777, "Bruma torch (off-hand)", Skills.FIREMAKING, 50),
            )

        /**
         * The other side of the line: non-combat requirements that are real, and that clearing
         * the ones above must not touch. These are what the loader actually produces today, not
         * what OSRS ideally asks for - the two differ where the cache is simply *missing* a
         * requirement, which is a different gap (the one `equipmentRequirements.yml` exists to
         * fill) and not this file's to close. The crystal helm is the example: the wiki wants
         * Ranged 70 alongside the Defence 70 and Agility 50 below, and the cache does not carry
         * it.
         */
        private val KEPT =
            listOf(
                Kept(23971, "Crystal helm", mapOf(Skills.DEFENCE to 70, Skills.AGILITY to 50)),
                Kept(24123, "Crystal bow", mapOf(Skills.RANGED to 70, Skills.AGILITY to 50)),
                Kept(25865, "Bow of faerdhinen", mapOf(Skills.RANGED to 80, Skills.AGILITY to 70)),
                Kept(19553, "Amulet of torture", mapOf(Skills.HITPOINTS to 75)),
                Kept(19547, "Necklace of anguish", mapOf(Skills.HITPOINTS to 75)),
                Kept(19550, "Ring of suffering", mapOf(Skills.HITPOINTS to 75)),
                Kept(19544, "Tormented bracelet", mapOf(Skills.HITPOINTS to 75)),
                Kept(11902, "Leaf-bladed sword", mapOf(Skills.ATTACK to 50, Skills.SLAYER to 55)),
                Kept(4170, "Slayer's staff", mapOf(Skills.MAGIC to 50, Skills.SLAYER to 55)),
                Kept(
                    22951,
                    "Boots of brimstone",
                    mapOf(Skills.MAGIC to 70, Skills.RANGED to 70, Skills.DEFENCE to 70, Skills.SLAYER to 44),
                ),
                /*
                 * The Nightmare staves were flagged as suspect on the grounds that the wiki wants
                 * 50 Attack. It does not - it wants Magic 72 and Hitpoints 50, which is exactly
                 * what the cache says. Pinned here so the next person to look does not re-open it.
                 */
                Kept(24422, "Nightmare staff", mapOf(Skills.MAGIC to 72, Skills.HITPOINTS to 50)),
            )

        /**
         * The skills a level in a cache requirement param never legitimately names. Agility,
         * Slayer and Hitpoints all do (see [KEPT]); Crafting and Firemaking never do - OSRS has no
         * equipment gated on either.
         */
        private val NEVER_GATES_EQUIPMENT = setOf(Skills.CRAFTING, Skills.FIREMAKING)
    }

    private data class Cleared(val id: Int, val name: String, val skill: Int, val level: Int)

    private data class Kept(val id: Int, val name: String, val reqs: Map<Int, Int>)

    private fun reqsOf(item: Int): Map<Int, Int> {
        assertTrue(loaded)
        val reqs = CacheManager.getItem(item).skillReqs ?: return emptyMap()
        return reqs.entries.associate { it.key.toInt() to it.value.toInt() }
    }

    /** What the *cache alone* says, read from raw params - `loadAll` never touches those. */
    private fun cacheReqsOf(item: Int): Map<Int, Int> {
        val def = CacheManager.getItem(item)
        return listOf(434 to 436, 435 to 437, 191 to 613, 579 to 614).mapNotNull { (skillKey, levelKey) ->
            val skill = def.params?.get(skillKey) as? Int ?: return@mapNotNull null
            val level = def.params?.get(levelKey) as? Int ?: return@mapNotNull null
            if (level > 0) skill to level else null
        }.toMap()
    }

    /**
     * The name is the anchor: an id that has moved between cache revisions would clear the
     * requirement on some unrelated item, and nothing would say so.
     */
    @Test
    fun `every id is a real equippable item with the recorded name`() {
        (CLEARED.map { it.id to it.name } + KEPT.map { it.id to it.name }).forEach { (id, name) ->
            val def = CacheManager.getItem(id)
            assertEquals(name, def.name, "id $id is '${def.name}' in the cache, not '$name' - the id has moved")
            assertTrue(def.equipSlot >= 0, "id $id ($name) is not equippable, so its requirement can never fire")
        }
    }

    /**
     * The defect is still in the cache. If a future revision stops shipping these levels as
     * requirements, the override file becomes dead weight and should go - this is what says so.
     */
    @Test
    fun `the cache still ships the creation level as a requirement`() {
        CLEARED.forEach { item ->
            assertEquals(
                item.level,
                cacheReqsOf(item.id)[item.skill],
                "${item.name} (${item.id}) no longer declares skill ${item.skill} in the cache - " +
                    "the override for it is obsolete",
            )
        }
    }

    /** End to end through the real loader: nothing gates these items any more. */
    @Test
    fun `the cleared items need nothing to equip`() {
        CLEARED.forEach { item ->
            assertEquals(
                emptyMap(),
                reqsOf(item.id),
                "${item.name} (${item.id}) is still gated",
            )
        }
    }

    /** The other half: clearing those must not have taken any genuine requirement with it. */
    @Test
    fun `genuine non-combat requirements still stand`() {
        KEPT.forEach { item ->
            assertEquals(item.reqs, reqsOf(item.id), "${item.name} (${item.id})")
        }
    }

    /**
     * The sweep that keeps this file honest without anyone maintaining a list. Crafting and
     * Firemaking gate no equipment in OSRS, so any equippable item left needing either is a new
     * instance of the same defect - from a cache bump, or from an item nobody probed for.
     */
    @Test
    fun `no equippable item is gated on Crafting or Firemaking`() {
        assertTrue(loaded)
        val offenders =
            CacheManager.getItems().values.filter { it.equipSlot >= 0 }.mapNotNull { def ->
                val reqs = CacheManager.getItem(def.id).skillReqs ?: return@mapNotNull null
                val bad = reqs.entries.filter { it.key.toInt() in NEVER_GATES_EQUIPMENT }
                if (bad.isEmpty()) null else "${def.id} ${def.name} ${bad.map { it.key to it.value }}"
            }
        assertTrue(
            offenders.isEmpty(),
            "equippable items gated on a skill that gates no equipment in OSRS: $offenders",
        )
    }
}
