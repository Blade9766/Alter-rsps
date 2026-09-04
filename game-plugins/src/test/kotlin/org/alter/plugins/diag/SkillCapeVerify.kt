package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.api.EquipmentType
import org.alter.api.Skills
import org.alter.plugins.content.items.skillcapes.SkillCape
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Checks on [SkillCape], whose sixty-nine rscm keys are the whole risk in it.
 *
 * The cache's cape names are irregular - `ranging_*`, `runecraft_*`, `construct_*`, and
 * `woodcut_capet` for one item out of a set of three whose siblings spell Woodcutting out - so a
 * key here is easy to get wrong, and a wrong one fails quietly: it resolves to nothing, the bind
 * never happens, and the cape stays wearable at level 1 exactly as before.
 *
 * The `Boost` assertions are the other half. [SkillCapePlugin] binds that option by name through
 * `onEquipmentOption`, which throws at plugin construction if the cache has no such option - and a
 * plugin whose constructor throws registers *nothing*, taking the level requirements down with it.
 */
class SkillCapeVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        private const val BOOST = "Boost"
    }

    private fun idOf(key: String): Int? = runCatching { getRSCM(key) }.getOrNull()

    private fun wornOptions(id: Int): List<String> {
        val def = CacheManager.getItem(id)
        return (0..7).mapNotNull { def.params?.get(451 + it) as? String }
    }

    /** One cape per skill, and every skill covered - there are 23 skills and 23 skillcapes. */
    @Test
    fun `there is exactly one cape per skill`() {
        assertEquals(23, SkillCape.values.size, "a skillcape has gone missing from the table")
        val skills = SkillCape.values.map { it.skill }
        assertEquals(skills.size, skills.toSet().size, "two capes claim the same skill")
        assertEquals((0..22).toSet(), skills.toSet(), "the capes do not cover every skill id")
        assertTrue(Skills.ATTACK in skills, "there is no Attack cape")
    }

    /**
     * Names are not asserted against the skill name - the cache abbreviates several ("Construct.
     * cape", "Woodcut. cape(t)") - only that the item exists, sits in the right slot, and that the
     * cape and its trim are two different items.
     */
    @Test
    fun `every key resolves to a real item in the right slot`() {
        SkillCape.values.forEach { cape ->
            listOf(cape.cape to EquipmentType.CAPE, cape.trimmed to EquipmentType.CAPE, cape.hood to EquipmentType.HEAD)
                .forEach { (key, slot) ->
                    val id = idOf(key)
                    assertTrue(id != null, "$cape: '$key' does not resolve")
                    val def = CacheManager.getItem(id!!)
                    assertEquals(
                        slot.id,
                        def.equipSlot,
                        "$cape: '$key' is ${def.name} in slot ${def.equipSlot}, expected ${slot.name}",
                    )
                }
            assertTrue(
                idOf(cape.cape) != idOf(cape.trimmed),
                "$cape: the cape and its trimmed variant resolve to the same id",
            )
        }
    }

    @Test
    fun `no item is claimed by two capes`() {
        val seen = mutableMapOf<Int, String>()
        SkillCape.values.forEach { cape ->
            listOf(cape.cape, cape.trimmed, cape.hood).forEach { key ->
                val id = idOf(key) ?: return@forEach
                val previous = seen.put(id, "$cape/$key")
                assertTrue(previous == null, "id $id is claimed by both $previous and $cape/$key")
            }
        }
    }

    /**
     * [SkillCape.hasBoost] has to agree with the cache exactly. Claiming a Boost the cache does not
     * have throws at plugin construction; missing one the cache does have silently drops the effect.
     */
    @Test
    fun `hasBoost matches what the cache actually declares`() {
        SkillCape.values.forEach { cape ->
            cape.capes.forEach { key ->
                val id = idOf(key) ?: return@forEach
                val declared = BOOST in wornOptions(id)
                assertEquals(
                    cape.hasBoost,
                    declared,
                    "$cape ('$key'): hasBoost=${cape.hasBoost} but the cache says ${wornOptions(id)}",
                )
            }
        }
    }

    /** The one cape with no worn option at all, called out in [SkillCape]'s comment. */
    @Test
    fun `the agility cape is the only one without a boost`() {
        val without = SkillCape.values.filter { !it.hasBoost }
        assertEquals(listOf(SkillCape.AGILITY), without, "the set of boost-less capes has changed")
    }

    /** Boost is the first worn option on every cape that has one - the op numbering depends on it. */
    @Test
    fun `boost is the first worn option`() {
        SkillCape.values.filter { it.hasBoost }.forEach { cape ->
            cape.capes.forEach { key ->
                val id = idOf(key) ?: return@forEach
                assertEquals(
                    BOOST,
                    wornOptions(id).firstOrNull(),
                    "$cape ('$key'): Boost is not the first worn option",
                )
            }
        }
    }
}
