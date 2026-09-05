package org.alter.plugins.diag

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.openrune.cache.CacheManager
import dev.openrune.cache.SOUNDEFFECTS
import org.junit.BeforeClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards `named-combat-media.json`, the table every npc's combat audio and animations come from.
 *
 * Everything in it is a bare number, and a wrong one is close to invisible: the fight still plays
 * out, the npc still swings and dies, it just sounds or looks like something else. Three specific
 * ways it went wrong, all found from in-game reports rather than from anything failing:
 *
 *  - **A sound id that is not a real archive plays nothing.** Silence is indistinguishable from an
 *    entry that has no sound at all, so a typo here is permanent and quiet.
 *  - **An attack animation that is really a *defend* animation.** `WOMAN` carried attack 425
 *    (`HUMAN_DEFEND_COWARDLY`) and block 422 (`HUMAN_PUNCH`) - exactly inverted against `MAN`, so
 *    women punched by flinching and flinched by punching.
 *  - **An entry with no sounds at all** is an npc that fights in silence. That is legitimate for
 *    some creatures, so this only pins the ones that have been reported and fixed, rather than
 *    demanding the whole table be filled.
 */
class CombatAudioVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(java.nio.file.Paths.get("../data", "cache"), 228)
        }

        private const val MEDIA = "src/main/resources/npc-animations/named-combat-media.json"

        /**
         * Animations this codebase's own cache-derived names identify as a *defend*, which can
         * therefore never be a correct `attackAnimation`. See `org.alter.api.cfg.Animation`.
         */
        private val DEFEND_ANIMATIONS = mapOf(425 to "HUMAN_DEFEND_COWARDLY")

        private val entries: Map<String, Map<String, Any>> by lazy {
            Gson().fromJson(
                java.io.File(MEDIA).readText(),
                object : TypeToken<Map<String, Map<String, Any>>>() {}.type,
            )
        }

        private fun intOf(entry: Map<String, Any>, key: String): Int? =
            (entry[key] as? Number)?.toInt()
    }

    /** A sound id that is not an archive in index 4 plays nothing, silently. */
    @Test
    fun `every combat sound id is a real sound archive`() {
        val archives = CacheManager.cache.archives(SOUNDEFFECTS).toHashSet()
        assertTrue(archives.isNotEmpty(), "no sound archives loaded; the cache is not readable here")

        val bad = mutableListOf<String>()
        entries.forEach { (name, entry) ->
            listOf("attackSound", "blockSound", "deathSound").forEach { key ->
                val id = intOf(entry, key) ?: return@forEach
                if (id !in archives) {
                    bad += "$name.$key=$id"
                }
            }
        }
        assertTrue(bad.isEmpty(), "These sound ids are not archives in the cache and will play nothing: $bad")
    }

    /** An attack animation that is really a defend makes the npc flinch instead of swinging. */
    @Test
    fun `no entry attacks with a defend animation`() {
        val bad =
            entries.mapNotNull { (name, entry) ->
                val attack = intOf(entry, "attackAnimation")
                DEFEND_ANIMATIONS[attack]?.let { "$name attacks with $attack ($it)" }
            }
        assertTrue(bad.isEmpty(), "These entries attack with a defend animation: $bad")
    }

    /**
     * `MAN` and `WOMAN` are the same human unarmed set and must stay identical. They were
     * inverted against each other, which is what made the inversion visible at all.
     */
    @Test
    fun `man and woman share the human unarmed set`() {
        val man = entries.getValue("MAN")
        val woman = entries.getValue("WOMAN")
        listOf("attackAnimation", "blockAnimation", "deathAnimation", "attackSound", "blockSound", "deathSound")
            .forEach { key ->
                assertEquals(intOf(man, key), intOf(woman, key), "MAN and WOMAN disagree on $key")
            }
        assertEquals(422, intOf(man, "attackAnimation"), "the human unarmed attack is 422, HUMAN_PUNCH")
    }

    /**
     * Reported silent in combat.
     *
     * `ANGER_UNICORN_*` is the *only* unicorn set in `Sound.kt` - there is no plain `UNICORN_*`
     * triple - which is what makes it the right one. Note the `ANGER_` prefix does **not** simply
     * decorate a base name: `ANGER_BEAR_ATTACK` (297) and `BEAR_ATTACK` (300) both exist, three
     * ids apart, so it marks a distinct variant. An earlier version of this comment argued from
     * the ids' alphabetical position between TUROTH and VAMPIRE; that reasoning was wrong even
     * though the choice was right.
     */
    @Test
    fun `the unicorn has combat sounds`() {
        val unicorn = entries.getValue("UNICORN")
        assertEquals(876, intOf(unicorn, "attackSound"), "unicorn attack sound")
        assertEquals(877, intOf(unicorn, "deathSound"), "unicorn death sound")
        assertEquals(878, intOf(unicorn, "blockSound"), "unicorn hit sound")
    }

    /**
     * The ten entries filled in on 2026-09-04, each borrowing the clip of the same creature or a
     * direct variant of it. Pinned so a future edit cannot quietly drop them back to silence.
     *
     * Everything still silent is silent on purpose: the remaining entries - the God Wars and
     * wilderness bosses, Dagannoth, Kraken, Nechryael and the rest - have **no** matching
     * constant in `Sound.kt` at all, so there is nothing to borrow and inventing one would be a
     * guess. See the class note on where these ids come from.
     */
    @Test
    fun `the borrowed combat sounds are still assigned`() {
        mapOf(
            "BARBARIAN_SPIRIT" to Triple(3381, 3382, 3383),
            "BEAR_CUB" to Triple(300, 301, 302),
            "SKELETON_HELLHOUND" to Triple(1187, 1188, 1185),
            "GREATER_SKELETON_HELLHOUND" to Triple(1187, 1188, 1185),
            "MONKEY_ARCHER" to Triple(630, 632, 634),
            "SMALL_CRYPT_SPIDER" to Triple(3604, 3608, 3609),
            "GNOME_BATTLE_MAGE" to Triple(453, 454, 455),
            "CAVE_HORROR" to Triple(496, 499, 500),
            "SMOKE_DEVIL" to Triple(414, 415, 416),
            "SPIDER_KOLODION" to Triple(3605, 3606, 3607),
        ).forEach { (name, expected) ->
            val entry = entries.getValue(name)
            assertEquals(expected.first, intOf(entry, "attackSound"), "$name attack sound")
            assertEquals(expected.second, intOf(entry, "deathSound"), "$name death sound")
            assertEquals(expected.third, intOf(entry, "blockSound"), "$name hit sound")
        }
    }

    /**
     * The dwarf's own ids were correct all along - the fault was that a player being hit made no
     * sound at all (see `Combat.postDamage`). Pinned so the report cannot be misread as a data
     * problem a second time.
     */
    @Test
    fun `the dwarf uses its own sound set`() {
        val dwarf = entries.getValue("DWARF")
        assertEquals(417, intOf(dwarf, "attackSound"), "DWARF_ATTACK")
        assertEquals(419, intOf(dwarf, "blockSound"), "DWARF_HIT")
        assertEquals(418, intOf(dwarf, "deathSound"), "DWARF_DEATH")
    }
}
