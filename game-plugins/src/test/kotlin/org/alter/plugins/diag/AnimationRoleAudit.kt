package org.alter.plugins.diag

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import dev.openrune.cache.CacheManager
import io.github.classgraph.ClassGraph
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.game.service.game.NpcMetadataService
import org.alter.plugins.content.npcs.animations.MonsterAnimationResolver
import org.alter.rscm.RSCM
import org.junit.BeforeClass
import java.io.File
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Finds monsters whose combat animations are in the **wrong roles** - a defend sequence played as
 * the attack, an attack played as the block - across every npc actually placed in the world.
 *
 * ## Why this can be detected rather than eyeballed
 *
 * `org.alter.api.cfg.Animation` names nearly every sequence by its role: `HUMAN_SLASH_SWORD_ATTACK`,
 * `HUMAN_SLASH_SWORD_DEFEND`, `HUMAN_DEATH`, `ICE_WARRIOR_HIT`. Those names are the arbiter this
 * codebase already trusts - `npc-animations/README.md` uses them to justify every correction it
 * records - so an npc whose *attack* animation is named `..._DEFEND` is, on the project's own
 * standard, wrong. That makes the check mechanical instead of a judgement call.
 *
 * The failure it catches has now been found three separate times by hand: the first bestiary pass
 * (`BANDIT`, `UNICORN`), the second (`OUTLAW`, `HERO`, `BATTLE_MAGE`, `ELDER_CHAOS_DRUID`, the
 * dragons, the ice warriors) and the sound sweep after it. `MonsterAnimationResolver` has no frame
 * sounds to go on in this cache, so it falls through to comparing durations, and a defend sequence
 * is routinely longer than the swing it answers.
 *
 * ## This asserts, unlike the other two audits in this package
 *
 * It found fifteen rows covering twenty-one ids when it was written, and every one of them was a
 * genuine mistake with an unambiguous fix from the observed set. With the count at zero it is worth
 * more as a guard than as a report: the next monster wired with its attack and block the wrong way
 * round fails the build instead of shipping.
 *
 * If a monster ever legitimately needs a role its name disagrees with, name the id and the reason
 * here rather than deleting the check - that is the whole value of it.
 */
class AnimationRoleAudit {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        private val STATS_CONFIG = File("../data/cfg/npcs/monsterStats.json")

        /** The roots that contain a `spawnNpc` call. See [SpawnedMonsterSoundAudit] for why scoped. */
        private val PACKAGES =
            arrayOf(
                "org.alter.plugins.content.npcs",
                "org.alter.plugins.content.areas",
                "org.alter.plugins.content.quests",
                "org.alter.plugins.content.skills",
            )

        /** Words in an `Animation` constant that mean "this sequence is a swing". */
        private val ATTACK_WORDS = listOf("ATTACK", "STAB", "SWING", "SLASH", "CRUSH", "SMASH", "PUNCH", "KICK", "CAST", "SPELL")

        /** Words that mean "this sequence is a hit reaction". */
        private val BLOCK_WORDS = listOf("DEFEND", "HIT", "BLOCK", "PARRY")

        private val DEATH_WORDS = listOf("DEATH", "DIE")
    }

    /** Every `Animation` constant, by the id it names. One id can carry several names. */
    private val animationNames: Map<Int, List<String>> by lazy {
        val text = File("../game-api/src/main/kotlin/org/alter/api/cfg/Animation.kt").readText()
        val out = HashMap<Int, MutableList<String>>()
        Regex("const val ([A-Z0-9_]+) = (\\d+)").findAll(text).forEach {
            out.getOrPut(it.groupValues[2].toInt()) { mutableListOf() }.add(it.groupValues[1])
        }
        out
    }

    private fun role(id: Int?): String? {
        val names = id?.let { animationNames[it] } ?: return null
        // A death name wins over the others: HUMAN_DEATH is unambiguous.
        if (names.any { n -> DEATH_WORDS.any { n.endsWith("_$it") || n == it } }) return "death"
        if (names.any { n -> BLOCK_WORDS.any { n.contains("_$it") || n.endsWith(it) } }) return "block"
        if (names.any { n -> ATTACK_WORDS.any { n.contains("_$it") || n.endsWith(it) } }) return "attack"
        return null
    }

    private fun label(id: Int?): String =
        if (id == null) "none" else "$id" + (animationNames[id]?.firstOrNull()?.let { "($it)" } ?: "")

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

    private lateinit var repository: PluginRepository

    private fun everyPlacedNpcId(): Set<Int> {
        val world = World(EatingVerify.GAME_CONTEXT, EatingVerify.DEV_CONTEXT)
        NpcMetadataService().loadMonsterStats(world, STATS_CONFIG)
        repository = world.plugins
        val server = Server()
        ClassGraph().enableClassInfo().acceptPackages(*PACKAGES).scan().use { result ->
            result.getSubclasses(KotlinPlugin::class.java.name).forEach { info ->
                try {
                    Class.forName(info.name)
                        .getConstructor(PluginRepository::class.java, World::class.java, Server::class.java)
                        .newInstance(repository, world, server)
                } catch (e: Exception) {
                    // A plugin that will not build places nothing, exactly as the real scan behaves.
                }
            }
        }
        val field = PluginRepository::class.java.getDeclaredField("npcSpawns").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val spawns = field.get(repository) as List<org.alter.game.model.entity.Npc>
        return spawns.map { it.id }.toSet()
    }

    @Test
    fun `no spawned monster has its combat animations in the wrong roles`() {
        val ids = everyPlacedNpcId()
        val rows = sortedMapOf<String, MutableList<Int>>()
        var checked = 0
        var unnamed = 0

        ids.forEach { id ->
            val def = CacheManager.getNpc(id) ?: return@forEach
            if (def.actions.filterNotNull().none { it.equals("Attack", ignoreCase = true) }) return@forEach

            val declared = repository.npcCombatDefs[id]
            val hit = namedFor(id, def.name)
            val (atk, blk, dth) =
                if (declared != null) {
                    Triple(
                        declared.attackAnimation.takeIf { it > 0 },
                        declared.blockAnimation.takeIf { it > 0 },
                        declared.deathAnimation.firstOrNull()?.takeIf { it > 0 },
                    )
                } else if (hit != null) {
                    Triple(
                        hit.second["attackAnimation"], hit.second["blockAnimation"], hit.second["deathAnimation"],
                    )
                } else {
                    val r = observed[id]?.let { obs ->
                        MonsterAnimationResolver.resolve(def, obs) { CacheManager.getAnims()[it] }
                    }
                    Triple(r?.attack, r?.block, r?.death)
                }

            checked++
            val problems = mutableListOf<String>()
            role(atk)?.let { if (it != "attack") problems += "attack is a $it" }
            role(blk)?.let { if (it != "block") problems += "block is a $it" }
            role(dth)?.let { if (it != "death") problems += "death is a $it" }
            if (atk == null && blk == null && dth == null) {
                unnamed++
                return@forEach
            }
            if (problems.isEmpty()) return@forEach

            val source = if (declared != null) "declared def" else hit?.first ?: "observed"
            val obs = observed[id]?.joinToString(",") ?: "-"
            rows.getOrPut(
                "%-26s %-18s a=%-26s b=%-26s d=%-22s  [%s]  observed=[%s]".format(
                    def.name, source, label(atk), label(blk), label(dth), problems.joinToString("; "), obs,
                ),
            ) { mutableListOf() }.add(id)
        }

        println("=".repeat(150))
        println("attackable spawned ids checked: $checked   (no named animation at all: $unnamed)")
        println("rows with a role disagreement : ${rows.size}  covering ${rows.values.sumOf { it.size }} ids")
        println("=".repeat(150))
        rows.forEach { (row, list) -> println("$row  ids=${list.sorted()}") }
        println("=".repeat(150))

        assertTrue(
            rows.isEmpty(),
            "These monsters play a sequence in a role its own Animation constant contradicts:" +
                System.lineSeparator() +
                rows.entries.joinToString(System.lineSeparator()) { (row, list) -> "$row  ids=${list.sorted()}" },
        )
    }
}
