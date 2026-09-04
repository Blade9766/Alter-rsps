package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.api.EquipmentType
import org.alter.api.cfg.Varp
import org.alter.api.ext.setVarp
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.service.game.ItemMetadataService
import org.alter.plugins.content.combat.Combat
import org.alter.plugins.content.combat.strategy.magic.CombatSpell
import org.alter.plugins.content.mechanics.poison.CombatPoison
import org.alter.rscm.RSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Drives [CombatPoison.sourceFor] with a real player and real equipment.
 *
 * [PoisonVerify] proves the table is right; this proves it is *reached*. The resolution in between
 * is where the mistakes hide, because it depends on three things that are easy to get backwards:
 * the attacker's combat class comes from the selected attack style rather than the weapon, a
 * ranged attack draws its poison from whichever slot actually held the ammo - the quiver for a
 * crossbow, the weapon slot for darts - and only the four smoke spells poison of the sixty-odd
 * spells that could be cast.
 *
 * The style indices are the raw values of [Varp.WEAPON_ATTACK_STYLE], and this is the reason the
 * ranged weapons below set one: a dart panel's Accurate button is index 0 like a dagger's, but the
 * two resolve through different rows of [org.alter.plugins.content.combat.WeaponStyles], and it is
 * that lookup - not the item - that decides whether a shot poisons for 2 or for 4.
 */
class CombatPoisonSourceVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
            // Populates ItemType.weaponType, which is what the combat class is read through.
            ItemMetadataService().loadAll()
        }

        private const val DRAGON_DAGGER_P_PLUS_PLUS = 5698
        private const val RUNE_DAGGER = 1213
        private const val ABYSSAL_TENTACLE = 12006
        private const val RUNE_CROSSBOW = 9185
        private const val RUNITE_BOLTS_P_PLUS = 9298
        private const val RUNITE_BOLTS = 9144
        private const val RUNE_DART_P = 817
        private const val MAGIC_SHORTBOW = 861
        private const val RUNE_ARROW_P_PLUS_PLUS = 5627

        /** Style index 0 is the first button on every panel - Accurate for melee, Accurate for ranged. */
        private const val FIRST_STYLE = 0
    }

    private fun player(): Player {
        val world = World(EatingVerify.GAME_CONTEXT, EatingVerify.DEV_CONTEXT)
        val player = Player(world)
        player.setVarp(Varp.WEAPON_ATTACK_STYLE, FIRST_STYLE)
        return player
    }

    private fun armed(
        weapon: Int,
        ammo: Int? = null,
    ): Player {
        val player = player()
        player.equipment[EquipmentType.WEAPON.id] = Item(weapon)
        if (ammo != null) {
            player.equipment[EquipmentType.AMMO.id] = Item(ammo)
        }
        return player
    }

    @Test
    fun `a coated melee weapon poisons for its melee damage`() {
        val source = assertNotNull(CombatPoison.sourceFor(armed(DRAGON_DAGGER_P_PLUS_PLUS)), "dragon dagger(p++) poisons")
        assertEquals(CombatPoison.Strength.EXTRA.meleeDamage, source.damage, "dragon dagger(p++) initial damage")
        assertEquals(25.0, source.chance, "melee poison is a 1/4 chance")
        assertEquals(false, source.appliesOnMiss, "player poison needs the hit to land")
    }

    @Test
    fun `an uncoated weapon poisons nothing`() {
        assertNull(CombatPoison.sourceFor(armed(RUNE_DAGGER)), "a plain rune dagger does not poison")
        assertNull(CombatPoison.sourceFor(player()), "bare fists do not poison")
    }

    @Test
    fun `the abyssal tentacle poisons on an ordinary attack`() {
        val source = assertNotNull(CombatPoison.sourceFor(armed(ABYSSAL_TENTACLE)), "the tentacle poisons")
        assertEquals(4, source.damage, "the tentacle starts at 4")
        assertEquals(25.0, source.chance, "the tentacle is a 1/4 chance")
    }

    /**
     * A crossbow draws from the quiver, and the reduced ranged figure applies: `(p+)` bolts start
     * at 3, not the 5 the same coating inflicts in melee.
     */
    @Test
    fun `coated bolts poison for the reduced ranged damage`() {
        val source =
            assertNotNull(
                CombatPoison.sourceFor(armed(RUNE_CROSSBOW, RUNITE_BOLTS_P_PLUS)),
                "runite bolts (p+) poison",
            )
        assertEquals(CombatPoison.Strength.SUPER.rangedDamage, source.damage, "runite bolts (p+) initial damage")
        assertEquals(12.5, source.chance, "ranged poison is a 1/8 chance")
        assertNull(CombatPoison.sourceFor(armed(RUNE_CROSSBOW, RUNITE_BOLTS)), "uncoated bolts do not poison")
    }

    @Test
    fun `coated arrows poison`() {
        val source =
            assertNotNull(
                CombatPoison.sourceFor(armed(MAGIC_SHORTBOW, RUNE_ARROW_P_PLUS_PLUS)),
                "rune arrow(p++) poisons",
            )
        assertEquals(CombatPoison.Strength.EXTRA.rangedDamage, source.damage, "rune arrow(p++) initial damage")
    }

    /**
     * Darts *are* the ammo, so the poison has to be read out of the weapon slot. Reading the quiver
     * unconditionally would have made every thrown weapon in the game inert.
     */
    @Test
    fun `a coated thrown weapon poisons from the weapon slot`() {
        val source = assertNotNull(CombatPoison.sourceFor(armed(RUNE_DART_P)), "rune dart(p) poisons")
        assertEquals(CombatPoison.Strength.REGULAR.rangedDamage, source.damage, "rune dart(p) initial damage")
        assertEquals(12.5, source.chance, "a thrown weapon is a ranged 1/8 chance")
    }

    @Test
    fun `a smoke spell poisons and other spells do not`() {
        val caster = armed(RUNE_DAGGER)
        caster.attr[Combat.CASTING_SPELL] = CombatSpell.SMOKE_BARRAGE
        val source = assertNotNull(CombatPoison.sourceFor(caster), "smoke barrage poisons")
        assertEquals(2, source.damage, "smoke poison starts at 2")
        assertEquals(12.5, source.chance, "smoke poison is a 1/8 chance")

        caster.attr[Combat.CASTING_SPELL] = CombatSpell.SHADOW_BARRAGE
        assertNull(CombatPoison.sourceFor(caster), "shadow barrage does not poison")
    }

    /**
     * The coating on the weapon is irrelevant to a spell: a mage holding a poisoned dagger and
     * casting fire blast is casting, not stabbing.
     */
    @Test
    fun `casting with a coated weapon in hand does not poison`() {
        val caster = armed(DRAGON_DAGGER_P_PLUS_PLUS)
        caster.attr[Combat.CASTING_SPELL] = CombatSpell.FIRE_BLAST
        assertNull(CombatPoison.sourceFor(caster), "a spell cast holding a dragon dagger(p++) does not poison")
    }
}
