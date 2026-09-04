package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import dev.openrune.cache.SOUNDEFFECTS
import org.alter.api.cfg.Animation
import org.alter.game.model.combat.CombatStyle
import org.alter.plugins.content.combat.WeaponSounds
import org.alter.plugins.content.npcs.faladorguard.FaladorGuardData
import org.alter.plugins.content.npcs.guard.CityGuards
import org.alter.plugins.content.npcs.whiteknight.WhiteKnightData
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Checks on [WeaponSounds], the one table both the player and the npc attack sounds are
 * drawn from. The ids can't be derived from the cache - no combat sequence at this revision
 * carries embedded sound data - so a typo'd id is invisible in game: the client is handed a
 * sound with no archive and silently drops it, which looks exactly like having no sound.
 */
class WeaponAttackSoundVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
        }
    }

    private val soundArchives: Set<Int> by lazy { CacheManager.cache.archives(SOUNDEFFECTS).toSet() }

    @Test
    fun `every clip in every weapon set has a cache archive`() {
        WeaponSounds.Weapon.values().forEach { weapon ->
            mapOf("stab" to weapon.stab, "slash" to weapon.slash, "crush" to weapon.crush).forEach { (type, sound) ->
                assertTrue(
                    sound in soundArchives,
                    "the $weapon set's $type clip $sound has no cache archive, so nothing would play",
                )
            }
        }
    }

    /** The npc path resolves through animations, so each mapped one has to land on a real clip. */
    @Test
    fun `every sounded attack animation resolves to a real clip`() {
        WeaponSounds.soundedAnimations.forEach { animation ->
            val sound = WeaponSounds.forAnimation(animation)
            assertNotNull(sound, "animation $animation is listed as sounded but resolves to nothing")
            assertTrue(sound in soundArchives, "animation $animation resolves to $sound, which has no cache archive")
        }
    }

    /**
     * The three clips in a set have to be distinct, or keying the sound on the attack type
     * is a no-op for that weapon - which is the bug this table fixes.
     */
    @Test
    fun `the sword sets give each attack type its own clip`() {
        listOf(WeaponSounds.Weapon.SWORD, WeaponSounds.Weapon.DAGGER, WeaponSounds.Weapon.TWO_HANDED).forEach { weapon ->
            val clips = listOf(weapon.stab, weapon.slash, weapon.crush)
            assertEquals(clips.size, clips.toSet().size, "the $weapon set reuses a clip across attack types: $clips")
        }
    }

    /**
     * A swing and a thrust of the same weapon must not sound the same - stated as the pairs
     * that actually differ in game, so the table can't quietly collapse back to one clip.
     */
    @Test
    fun `a sword slash and a sword lunge are different sounds`() {
        assertTrue(
            WeaponSounds.forAnimation(Animation.HUMAN_SLASH_SWORD_ATTACK) !=
                WeaponSounds.forAnimation(WeaponSounds.Weapon.SWORD, Animation.HUMAN_DAGGER_STAB),
            "a longsword's slash and its lunge resolve to the same clip",
        )
        assertTrue(
            WeaponSounds.forAnimation(Animation.HUMAN_BLUNT_SWING) != WeaponSounds.forAnimation(Animation.HUMAN_BLUNT_STAB),
            "a mace's pound and its spike resolve to the same clip",
        )
    }

    /**
     * Pins which way round the two blunt animations run. `CombatConfigs.getAttackAnimation`
     * hands a mace's Spike 400 and its Pound 401, and a pickaxe the reverse - the pickaxe
     * branch was a copy of the mace one and had it backwards until it was pinned here.
     */
    @Test
    fun `the blunt animations keep their attack types`() {
        assertEquals(
            WeaponSounds.Weapon.MACE.stab,
            WeaponSounds.forAnimation(Animation.HUMAN_BLUNT_STAB),
            "400 is the blunt thrust - a mace's Spike and a pickaxe's Spike/Impale/Block",
        )
        assertEquals(
            WeaponSounds.Weapon.MACE.crush,
            WeaponSounds.forAnimation(Animation.HUMAN_BLUNT_SWING),
            "401 is the blunt swing - a mace's Pound/Pummel and a pickaxe's Smash",
        )
    }

    /**
     * The npcs that state a weapon, pinned to the clip each should now make. Varrock's and
     * Edgeville's guards carry a longsword but play the dagger-stab animation, so they take
     * the sword set's *thrust* - the case the weapon override exists for.
     */
    @Test
    fun `the guards' weapons resolve to the expected clips`() {
        val expected =
            mapOf(
                "Varrock" to WeaponSounds.Weapon.SWORD.stab,
                "Edgeville" to WeaponSounds.Weapon.SWORD.stab,
                "Ardougne" to WeaponSounds.Weapon.MACE.crush,
            )
        CityGuards.ALL.forEach { city ->
            assertEquals(expected[city.city], city.attackSound, "${city.city}'s guards resolve to the wrong clip")
            assertTrue(city.attackSound in soundArchives, "${city.city}'s guard clip has no cache archive")
        }

        val faladorExpected =
            mapOf(
                "sword" to WeaponSounds.Weapon.SWORD.stab,
                "crossbow" to WeaponSounds.Weapon.CROSSBOW.crush,
                "battleaxe" to WeaponSounds.Weapon.AXE.slash,
                "unarmed" to WeaponSounds.Weapon.UNARMED.crush,
                "longbow" to WeaponSounds.Weapon.BOW.crush,
            )
        FaladorGuardData.GROUPS.forEach { group ->
            assertEquals(faladorExpected[group.name], group.attackSound, "Falador's ${group.name} guards resolve to the wrong clip")
            assertTrue(group.attackSound in soundArchives, "Falador's ${group.name} guard clip has no cache archive")
        }
    }

    /**
     * The armed npcs that state no sound of their own reach a clip through
     * `MonsterAnimationsPlugin`'s weapon fallback, purely from the animation they swing.
     * White Knights are the check here: two-handed sword, crush animation.
     */
    @Test
    fun `armed npcs with no stated sound still resolve through their animation`() {
        assertEquals(
            WeaponSounds.Weapon.TWO_HANDED.crush,
            WeaponSounds.forAnimation(WhiteKnightData.ATTACK_ANIMATION),
            "White Knights swing a two-handed sword and should sound like one",
        )
    }

    /** A creature's own bite or swipe isn't a weapon and must not be given one. */
    @Test
    fun `a monster's own attack animation gets no weapon clip`() {
        val cowAttack = 5849 // the COW entry in named-combat-media.json
        assertNull(WeaponSounds.forAnimation(cowAttack), "a non-weapon animation was given a weapon clip")
    }

    /** Nothing in the table may claim an attack type the resolver can't answer. */
    @Test
    fun `every weapon answers every attack type`() {
        WeaponSounds.Weapon.values().forEach { weapon ->
            CombatStyle.values().forEach { style ->
                assertTrue(weapon.forAttackType(style) > 0, "$weapon has no clip for $style")
            }
        }
    }
}
