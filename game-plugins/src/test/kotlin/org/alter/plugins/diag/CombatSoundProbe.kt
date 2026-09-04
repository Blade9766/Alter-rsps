package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.rscm.RSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test

/**
 * A probe: does this cache carry any frame-sound data on combat animations at all?
 *
 * `MonsterAnimationsPlugin` fills an npc's `defaultAttackSound` from
 * `SequenceType.sounds` / `soundEffects` on the resolved attack animation, falling back to a
 * weapon-sound lookup. If those collections are empty for every sequence in this revision, then
 * every monster whose `named-combat-media.json` entry carries no explicit `attackSound` is silent -
 * and that is most of them.
 */
class CombatSoundProbe {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }
    }

    @Test
    fun `report frame sound data on combat animations`() {
        val anims = CacheManager.getAnims()
        var withSounds = 0
        var withEffects = 0
        anims.forEach { (_, seq) ->
            if (seq.sounds.isNotEmpty()) withSounds++
            if (seq.soundEffects.any { it != null }) withEffects++
        }
        println("=".repeat(90))
        println("sequences in cache: ${anims.size}")
        println("  with `sounds` map non-empty : $withSounds")
        println("  with `soundEffects` non-null: $withEffects")
        println("=".repeat(90))

        // The specific animations this bestiary pass depends on.
        val named =
            mapOf(
                "CHROMATIC_DRAGON_MELEE_CLAW" to 80,
                "CHROMATIC_DRAGON_DRAGONFIRE" to 81,
                "CHROMATIC_DRAGON_HIT" to 89,
                "BABYDRAGON_ATTACK" to 25,
                "HELLHOUND_ATTACK (FOX_ATTACK)" to 6562,
                "ICE_WARRIOR_ATTACK" to 391,
                "TOAD_ATTACK" to 1793,
                "DEMON_ATTACK" to 64,
                "OGRE_ATTACK" to 359,
                "ICE_GIANT_ATTACK" to 4672,
                "DAGANNOTH_ATTACK" to 1341,
                "SCORPION_ATTACK" to 6254,
                "ROCK_CRAB_ATTACK" to 1312,
                "BEAR_ATTACK" to 4925,
                "GIANT_SPIDER_ATTACK" to 5327,
                "HUMAN_SLASH_SWORD_ATTACK" to 390,
                "GOD_SPELL" to 811,
                "MAGIC_WAVE_CAST" to 727,
                "HUMAN_PUNCH" to 422,
                "HUMAN_DEATH" to 836,
            )
        println("%-34s %-6s %-10s %s".format("animation", "id", "sounds", "soundEffects"))
        named.forEach { (label, id) ->
            val seq = anims[id]
            if (seq == null) {
                println("%-34s %-6d NOT IN CACHE".format(label, id))
            } else {
                val s = seq.sounds.entries.joinToString(",") { "${it.key}:${it.value.id}" }.ifEmpty { "-" }
                val e = seq.soundEffects.filterNotNull().joinToString(",").ifEmpty { "-" }
                println("%-34s %-6d %-10s %s".format(label, id, s, e))
            }
        }
        println("=".repeat(90))
    }
}
