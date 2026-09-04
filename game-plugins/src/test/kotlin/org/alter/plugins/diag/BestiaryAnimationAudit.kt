package org.alter.plugins.diag

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import dev.openrune.cache.CacheManager
import org.alter.plugins.content.npcs.animations.MonsterAnimationResolver
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.io.File
import java.nio.file.Paths
import kotlin.test.Test

/**
 * Prints, for a named list of npcs, exactly which combat animations
 * [org.alter.plugins.content.npcs.animations.MonsterAnimationsPlugin] would give them.
 *
 * This is the audit the `npc-animations/README.md` describes running with `npcAnimDiag`, kept as a
 * test so it can be re-run. It is *not* an assertion - it reports - because the thing it is for is
 * finding the silent case the README names: an npc whose name prefix- or suffix-matches somebody
 * else's entry and quietly takes their rig, or one with no usable observations at all that falls
 * back to the human 422/424/836 set.
 *
 * Run it with `gradlew :game-plugins:test --tests "*BestiaryAnimationAudit*" -i` and read the
 * standard output.
 */
class BestiaryAnimationAudit {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /** The human fallback set every un-resolved attackable npc ends up with. */
        private val HUMAN = Triple(422, 424, 836)
    }

    private val mapper = ObjectMapper()

    private val observed: Map<Int, List<Int>> by lazy {
        File("src/main/resources/npc-animations/openosrs-animations.json").inputStream().use {
            mapper.readValue(it, object : TypeReference<Map<String, List<Int>>>() {})
        }.mapKeys { (k, _) -> k.toInt() }
    }

    private val named: Map<String, Map<String, Int>> by lazy {
        File("src/main/resources/npc-animations/named-combat-media.json").inputStream().use {
            mapper.readValue(it, object : TypeReference<Map<String, Map<String, Int>>>() {})
        }
    }

    private val byId: Map<Int, String> by lazy {
        File("src/main/resources/npc-animations/id-combat-media.json").inputStream().use {
            mapper.readValue(it, object : TypeReference<Map<String, String>>() {})
        }.mapKeys { (k, _) -> k.toInt() }
    }

    /** The plugin's own name matching, replicated so the audit reports what it really does. */
    private fun namedFor(
        id: Int,
        name: String,
    ): Pair<String, Map<String, Int>>? {
        byId[id]?.let { key -> named[key]?.let { return key to it } }
        val normalized = name.uppercase().replace(Regex("[^A-Z0-9]+"), "_").trim('_')
        named[normalized]?.let { return normalized to it }
        named.keys.filter { normalized == it || normalized.endsWith("_$it") }.maxByOrNull { it.length }
            ?.let { return it to named.getValue(it) }
        named.keys.filter { normalized.startsWith("${it}_") }.maxByOrNull { it.length }
            ?.let { return it to named.getValue(it) }
        return null
    }

    @Test
    fun `report the combat animations every new bestiary npc would get`() {
        val keys =
            listOf(
                // hellhounds
                "npc.hellhound_104", "npc.hellhound_105", "npc.hellhound_7256", "npc.hellhound_3133", "npc.hellhound_7877",
                // dragons - adults
                "npc.green_dragon", "npc.green_dragon_261", "npc.blue_dragon", "npc.red_dragon", "npc.black_dragon",
                "npc.bronze_dragon", "npc.bronze_dragon_7253",
                // dragons - babies
                "npc.baby_blue_dragon", "npc.baby_red_dragon_244", "npc.baby_green_dragon", "npc.baby_black_dragon",
                // demons
                "npc.lesser_demon", "npc.lesser_demon_7247", "npc.greater_demon", "npc.greater_demon_7244",
                // ice
                "npc.ice_warrior", "npc.ice_warrior_2842", "npc.ice_giant", "npc.ice_giant_2088",
                // the rest
                "npc.ogre", "npc.ogre_1153", "npc.dagannoth_970", "npc.dagannoth_973",
                "npc.deadly_red_spider", "npc.rock_crab", "npc.rock_crab_102", "npc.rocks", "npc.rocks_103",
                "npc.frog_8702", "npc.big_frog", "npc.big_frog_8701", "npc.giant_frog", "npc.giant_frog_8700",
                "npc.black_bear", "npc.outlaw", "npc.black_heather", "npc.hero_3295",
                "npc.battle_mage", "npc.elder_chaos_druid",
                "npc.scorpion", "npc.scorpion_3024", "npc.scorpion_2480", "npc.scorpion_5242",
            )

        println("=".repeat(110))
        println("%-30s %-6s %-24s %-28s".format("key", "id", "cache name", "resolved attack/block/death"))
        println("=".repeat(110))
        keys.forEach { key ->
            val id = getRSCM(key)
            val def = CacheManager.getNpc(id)
            if (def == null) {
                println("%-30s %-6d NOT IN CACHE".format(key, id))
                return@forEach
            }
            val name = def.name
            val hit = namedFor(id, name)
            val line =
                if (hit != null) {
                    val (matchedKey, media) = hit
                    "named[$matchedKey] ${media["attackAnimation"]}/${media["blockAnimation"]}/${media["deathAnimation"]}"
                } else {
                    val obs = observed[id]
                    if (obs == null) {
                        "NO OBSERVATIONS -> human fallback ${HUMAN.first}/${HUMAN.second}/${HUMAN.third}"
                    } else {
                        val r = MonsterAnimationResolver.resolve(def, obs) { CacheManager.getAnims()[it] }
                        if (r == null) {
                            "RESOLVER BAILED (observed=$obs) -> human fallback ${HUMAN.first}/${HUMAN.second}/${HUMAN.third}"
                        } else {
                            "observed ${r.attack}/${r.block}/${r.death}   (raw=$obs)"
                        }
                    }
                }
            println("%-30s %-6d %-24s %s".format(key, id, name, line))
        }
        println("=".repeat(110))
    }
}
