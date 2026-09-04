package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import dev.openrune.cache.SOUNDEFFECTS
import org.alter.plugins.content.npcs.guard.CityGuard
import org.alter.plugins.content.npcs.guard.CityGuards
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
 * Verify-before-wire checks for the Varrock, Edgeville and Ardougne city guards, plus a
 * regression guard against the bug they replaced: `areas/varrock/spawns/SpawnPlugin` used
 * to spawn `guard_998/999/1000`, which are inert level-0 npcs with no options at all.
 *
 * Falador's guards are covered separately by `FaladorCombatVerify`, since they are the one
 * city whose versions split into several stat groups.
 */
class CityGuardVerify {
    companion object {
        /**
         * The human skeleton, stored in the first two bytes of every frame file. Necessary
         * but **not sufficient** - see the transform-group test below. Every human frame
         * group in this cache resolves to it: 207, 209, 219, 197, 233, 245, 1088 and 1056
         * alike, which is why 808 (group 207) and 401 (group 209) coexist on one player
         * without a seam, and why this check alone cleared 6488-6490 when it should not
         * have. Non-human rigs resolve elsewhere: 574 to 461, 785 to 673, 2488 to 1913.
         */
        const val HUMAN_FRAMEMAP = 0

        /**
         * Guard ids whose observed animations describe a model Jagex has since replaced, so
         * they are not evidence about any guard now spawned. 3010 and 3011 are the pre-2022
         * Varrock guards the wiki still lists as `hist` aliases of 11911 and 11917.
         */
        val REPLACED_GUARD_IDS = setOf(3010, 3011)

        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }
    }

    @Test
    fun `every city guard is attackable and matches its wiki combat level`() {
        CityGuards.ALL.forEach { city ->
            city.npcKeys.forEach { key ->
                val def = CacheManager.getNpcs()[getRSCM(key)]
                assertNotNull(def, "$key (${city.city}) has no cache npc")
                assertTrue(
                    def.actions.any { it?.lowercase() == "attack" },
                    "$key (${city.city}) is not attackable [actions=${def.actions.toList()}]",
                )
                assertEquals(
                    city.combatLevel,
                    def.combatLevel,
                    "$key cache level ${def.combatLevel} != wiki ${city.combatLevel} for ${city.city}",
                )
            }
        }
    }

    /**
     * The three ids the old Varrock spawn plugin used. They are real cache entries but carry
     * combat level 0 and `actions=[null,null,null,null,null]` - scenery, not guards.
     * Asserting it documents precisely why they were replaced, and would fail loudly if a
     * future cache ever made them real.
     */
    @Test
    fun `the old varrock guard ids really are inert scenery`() {
        listOf("npc.guard_998", "npc.guard_999", "npc.guard_1000").forEach { key ->
            val def = CacheManager.getNpcs()[getRSCM(key)]!!
            assertEquals(0, def.combatLevel, "$key is combat level ${def.combatLevel}, not 0")
            assertTrue(
                def.actions.all { it == null },
                "$key has real options ${def.actions.toList()} - it may be a usable npc after all",
            )
        }
    }

    /** Those inert ids must not come back into the area spawn plugin. */
    @Test
    fun `varrock spawn plugin no longer spawns the inert guards`() {
        val source =
            Files.readString(
                Paths.get(
                    "src", "main", "kotlin", "org", "alter", "plugins", "content",
                    "areas", "varrock", "spawns", "SpawnPlugin.kt",
                ),
            )

        listOf("npc.guard_998", "npc.guard_999", "npc.guard_1000").forEach { key ->
            assertTrue(
                !source.contains("spawnNpc(npc = \"$key\""),
                "SpawnPlugin still spawns the inert $key",
            )
        }
    }

    @Test
    fun `spawn counts match the wiki's pin counts`() {
        // Varrock: 13 city + 2 walls + 7 castle ground + 4 castle first + 11 castle second.
        assertEquals(37, CityGuards.VARROCK.spawns.size, "Varrock guard spawn count")
        assertEquals(20, CityGuards.VARROCK.spawns.count { it.height == 0 }, "Varrock plane 0")
        assertEquals(6, CityGuards.VARROCK.spawns.count { it.height == 1 }, "Varrock plane 1")
        assertEquals(11, CityGuards.VARROCK.spawns.count { it.height == 2 }, "Varrock plane 2")

        assertEquals(6, CityGuards.EDGEVILLE.spawns.size, "Edgeville guard spawn count")
        assertEquals(10, CityGuards.ARDOUGNE.spawns.size, "Ardougne guard spawn count")
    }

    @Test
    fun `no two city guard spawns share a tile`() {
        val tiles = CityGuards.ALL.flatMap { it.spawns }.map { Triple(it.x, it.z, it.height) }
        val duplicates = tiles.groupBy { it }.filterValues { it.size > 1 }.keys
        assertTrue(duplicates.isEmpty(), "duplicate guard spawn tiles: $duplicates")
    }

    /** Mirrors the coordinates the areas/varrock plugins spawn their townsfolk on. */
    @Test
    fun `no guard spawn lands on a varrock town npc`() {
        val townTiles =
            setOf(
                Triple(3253, 3402, 0), // Aubury
                Triple(3217, 3415, 0), // Shop keeper
                Triple(3218, 3415, 0), // Shop assistant
                Triple(3230, 3437, 0), // Horvik
                Triple(3235, 3424, 0), // Lowe
                Triple(3206, 3399, 0), // Swordshop keeper
                Triple(3206, 3416, 0), // Thessalia
                Triple(3203, 3434, 0), // Zaff
                Triple(3214, 3422, 0), // Man
                Triple(3220, 3412, 0), // Woman
            )

        val clashes =
            CityGuards.VARROCK.spawns.map { Triple(it.x, it.z, it.height) }.filter { it in townTiles }
        assertTrue(clashes.isEmpty(), "guards spawned on town npc tiles: $clashes")
    }

    /**
     * The three cities' guards are not interchangeable, despite sharing a name and a drop
     * table. Pinning the differences stops a future edit from quietly copy-pasting one
     * city's stat line over another's.
     */
    @Test
    fun `each city's guards keep their own distinct stat line`() {
        assertEquals(
            org.alter.game.model.combat.CombatStyle.CRUSH,
            CityGuards.VARROCK.combatStyle,
            "Varrock guards are Crush",
        )
        assertEquals(
            org.alter.game.model.combat.CombatStyle.STAB,
            CityGuards.EDGEVILLE.combatStyle,
            "Edgeville guards are Stab",
        )
        assertEquals(
            org.alter.game.model.combat.CombatStyle.CRUSH,
            CityGuards.ARDOUGNE.combatStyle,
            "Ardougne guards are Crush",
        )

        // Ardougne is the odd one out: a level lower, slower, and the only guards anywhere
        // with positive magic defence.
        assertEquals(20, CityGuards.ARDOUGNE.combatLevel)
        assertEquals(5, CityGuards.ARDOUGNE.attackSpeed)
        assertTrue(
            CityGuards.ARDOUGNE.defenceMagic > 0,
            "Ardougne guards should have positive magic defence, got ${CityGuards.ARDOUGNE.defenceMagic}",
        )
        listOf(CityGuards.VARROCK, CityGuards.EDGEVILLE).forEach {
            assertEquals(21, it.combatLevel, "${it.city} guards are level 21")
            assertEquals(4, it.attackSpeed, "${it.city} guards are attack speed 4")
            assertTrue(it.defenceMagic < 0, "${it.city} guards should have negative magic defence")
        }

        // All three are modelled on the standard human rig, so all three die the human way.
        CityGuards.ALL.forEach {
            assertEquals(836, it.deathAnimation, "${it.city} guards use HUMAN_DEATH")
        }
    }

    /**
     * **An npc may only be given animations built against the skeleton its own stand
     * animation is built against.** That is the real compatibility rule, and it is the
     * framemap, not the frame group: a sequence from a different *group* but the same
     * framemap drives the model perfectly (808 and 401 on any player), while a sequence
     * built against a different framemap deforms it.
     */
    @Test
    fun `city guard animations are built against the same skeleton their models are`() {
        CityGuards.ALL.forEach { city ->
            city.npcKeys.forEach { key ->
                val def = CacheManager.getNpcs()[getRSCM(key)]!!
                assertEquals(
                    HUMAN_FRAMEMAP,
                    framemapOf(frameGroupOf(def.standAnim)),
                    "$key (${city.city}) stands on a non-human skeleton - its combat animations " +
                        "need re-deriving, not this assertion relaxing",
                )
            }

            mapOf(
                "attack" to city.attackAnimation,
                "block" to city.blockAnimation,
                "death" to city.deathAnimation,
            ).forEach { (role, animation) ->
                assertEquals(
                    HUMAN_FRAMEMAP,
                    framemapOf(frameGroupOf(animation)),
                    "${city.city}'s $role animation $animation is built against a different " +
                        "skeleton from the guards it is played on",
                )
            }
        }
    }

    /**
     * **The rule that actually decides whether an animation can drive a model: it must move
     * the transform groups the model is already posed by.** Framemap 0 declares 245 of them
     * and different rigs use disjoint blocks, so two sequences can agree on frame group and
     * framemap and still be unrelated.
     *
     * This is the check that catches 6488/6489/6490, the `VARROCK_GUARD_*` constants these
     * guards were once wired with. Frame group says 1056; framemap says 0, the same human
     * skeleton the guards stand on - so neither of the looser tests rejects them. The frame
     * data does: they drive groups **134-215** and touch nothing in the **0-61** block that
     * the guards' own stand and walk animations, and every animation any of the four cities
     * plays, live in.
     */
    @Test
    fun `city guard animations move the transform groups their models are posed by`() {
        CityGuards.ALL.forEach { city ->
            city.npcKeys.forEach { key ->
                val def = CacheManager.getNpcs()[getRSCM(key)]!!
                val posed = animatedGroups(def.standAnim) + animatedGroups(def.walkAnim)
                assertTrue(posed.isNotEmpty(), "$key has no decodable stand/walk frames")

                mapOf(
                    "attack" to city.attackAnimation,
                    "block" to city.blockAnimation,
                    "death" to city.deathAnimation,
                ).forEach { (role, animation) ->
                    val groups = animatedGroups(animation)
                    val shared = groups.count { it in posed }
                    assertTrue(
                        shared * 2 >= groups.size,
                        "${city.city}'s $role animation $animation moves ${groups.size} transform " +
                            "groups but only $shared of them are ones $key is posed by - it " +
                            "belongs to another model's rig and will deform this one " +
                            "[animation=$groups posed=$posed]",
                    )
                }
            }
        }
    }

    /**
     * The specific sequences that caused it, kept separately so the mechanism survives even
     * if the city data is rewritten - and so the two checks that *pass* them are recorded as
     * insufficient rather than quietly retried by someone later.
     *
     * The separation is not marginal. Measured against the groups 808 and 819 pose: 6488
     * shares 0 of 15, 6489 0 of 18, 6490 1 of 20 (group 32 alone). Every animation the four
     * cities actually play shares nearly all of them - 386 all 21 of 21, 390 23 of 24, death
     * 836 29 of 31. A simple majority therefore splits them cleanly with room to spare, which
     * is what both tests use.
     */
    @Test
    fun `the old varrock guard animations move a disjoint set of transform groups`() {
        val human = animatedGroups(808) + animatedGroups(819)

        listOf(6488, 6489, 6490).forEach { animation ->
            assertEquals(1056, frameGroupOf(animation), "$animation should sit in frame group 1056")
            assertEquals(
                HUMAN_FRAMEMAP,
                framemapOf(frameGroupOf(animation)),
                "$animation shares the human framemap - which is exactly why the framemap " +
                    "check alone is not enough to reject it",
            )
            val groups = animatedGroups(animation)
            val shared = groups.count { it in human }
            assertTrue(
                shared * 2 < groups.size,
                "$animation shares $shared of its ${groups.size} transform groups with 808/819 - " +
                    "it was expected to be almost entirely disjoint from them",
            )
        }

        assertTrue(
            CityGuards.VARROCK.npcKeys.none { getRSCM(it) == 3010 },
            "the historical id 3010 must not be spawned as a live Varrock guard",
        )
    }

    /**
     * Varrock's guards have no observed animation set of their own, so they take the one
     * every other human guard in the game is observed to use rather than an inference from
     * the weapon model: `[388, 386, 836]` for Edgeville's 3254, `[388, 836, 386]` for
     * Falador's 3269.
     */
    @Test
    fun `varrock guards use the animation set observed on every other human guard`() {
        val observed =
            Files.readString(
                Paths.get("src", "main", "resources", "npc-animations", "openosrs-animations.json"),
            )

        listOf(11911, 11912, 11913, 11914, 11915, 11916, 11917).forEach { id ->
            assertTrue(
                !observed.contains("\"$id\""),
                "npc $id now has an observed animation set - use it instead of its siblings'",
            )
        }

        assertEquals(386, CityGuards.VARROCK.attackAnimation, "Varrock attack matches guard 3254/3269")
        assertEquals(386, CityGuards.EDGEVILLE.attackAnimation, "Edgeville attack matches its observed set")
        assertEquals(388, CityGuards.VARROCK.blockAnimation, "Varrock block matches guard 3254/3269")
    }

    @Test
    fun `every city guard is pickpocketable and wired into the thieving config`() {
        val config = Files.readString(Paths.get("../data", "cfg", "thieving", "pickpockets.json"))

        CityGuards.ALL.forEach { city ->
            city.npcKeys.forEach { key ->
                val def = CacheManager.getNpcs()[getRSCM(key)]!!
                assertTrue(
                    def.actions.any { it?.lowercase() == "pickpocket" },
                    "$key (${city.city}) has no Pickpocket option [actions=${def.actions.toList()}]",
                )
                assertTrue(
                    config.contains("\"$key\""),
                    "$key is pickpocketable in the cache but missing from pickpockets.json",
                )
            }
        }
    }

    /**
     * The framemap (skeleton) a frame group's frames are built against: the big-endian short
     * at the head of any frame file in cache index 0. Two sequences drive the same model if
     * and only if they resolve to the same framemap.
     */
    private fun framemapOf(frameGroup: Int): Int {
        val files = CacheManager.cache.files(0, frameGroup)
        assertTrue(files.isNotEmpty(), "frame group $frameGroup has no frames")
        val frame =
            assertNotNull(
                CacheManager.cache.data(0, frameGroup, files.first()),
                "frame group $frameGroup has no frame data",
            )
        assertTrue(frame.size >= 2, "frame group $frameGroup's first frame cannot hold a framemap id")
        return ((frame[0].toInt() and 0xff) shl 8) or (frame[1].toInt() and 0xff)
    }


    /**
     * Every combat sound id must exist as an archive in cache index 4. A sound id with no
     * archive behind it is written to the client and then dropped without a word, which is
     * indistinguishable in game from the npc having had no sound configured at all - so
     * this is the check that a "fixed" guard actually makes a noise.
     *
     * The ids also have to be stated in the first place. `MonsterAnimationsPlugin` resolves
     * combat audio by npc name (`named-combat-media.json`) and then by whatever sound is
     * baked into the resolved animation; guards are named the bare string `Guard`, which is
     * in no entry, and this cache carries no embedded sound data on any sequence at all -
     * the second assertion pins that down, since if a future cache ever did carry it the
     * explicit ids here would stop being the only thing keeping guards audible.
     */
    @Test
    fun `every guard combat sound resolves to a real cache archive`() {
        val archives = CacheManager.cache.archives(SOUNDEFFECTS).toSet()
        CityGuards.ALL.forEach { city ->
            listOf(
                "attack" to city.attackSound,
                "block" to CityGuard.HUMAN_BLOCK_SOUND,
                "death" to CityGuard.HUMAN_DEATH_SOUND,
            ).forEach { (kind, sound) ->
                assertTrue(sound > 0, "${city.city}'s $kind sound is unset, so nothing will play")
                assertTrue(sound in archives, "${city.city}'s $kind sound $sound has no cache archive")
            }

            listOf(city.attackAnimation, city.blockAnimation, city.deathAnimation).forEach { animation ->
                val sequence = CacheManager.getAnims()[animation]!!
                assertTrue(
                    sequence.sounds.isEmpty() && sequence.soundEffects.none { it != null },
                    "animation $animation now carries embedded sound - the explicit ids may be redundant",
                )
            }
        }
    }

    /**
     * The framemap transform groups a sequence actually animates, unioned over its frames.
     *
     * A frame is a big-endian framemap id, a byte giving how many transform groups follow,
     * then one mask byte per group; a non-zero mask means this frame moves that group.
     */
    private fun animatedGroups(animation: Int): Set<Int> {
        val sequence = CacheManager.getAnims()[animation] ?: return emptySet()
        val frames = sequence.frameIDs ?: return emptySet()
        return frames
            .flatMap { frameId ->
                val data = CacheManager.cache.data(0, frameId shr 16, frameId and 0xFFFF)
                if (data == null || data.size < 3) {
                    emptyList()
                } else {
                    val count = data[2].toInt() and 0xff
                    (0 until count).filter { 3 + it < data.size && (data[3 + it].toInt() and 0xff) > 0 }
                }
            }.toSet()
    }

    /** The high half of a frame id is the frame group the sequence's frames live in. */
    private fun frameGroupOf(animation: Int): Int {
        val sequence = assertNotNull(CacheManager.getAnims()[animation], "animation $animation has no cache sequence")
        val frames = assertNotNull(sequence.frameIDs, "animation $animation has no frame list")
        assertTrue(frames.isNotEmpty(), "animation $animation has no frames")
        return frames.first() shr 16
    }
}
