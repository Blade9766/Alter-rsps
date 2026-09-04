package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.plugins.content.combat.specialattack.SpecialAttackDefs
import org.alter.plugins.content.interfaces.attack.AttackTab
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Checks on the special attack system, which had gone wrong in two places at once and in both cases
 * silently.
 *
 * **The button.** The Combat Options tab's special attack bar is component **38**. It had been
 * bound at 36, which in this revision is the "Toggle set effect" button beside Auto retaliate - so
 * every click on the real bar fell through to the unhandled-button branch and no special could ever
 * be armed from the tab. Nothing failed; the bar simply did nothing. These first tests re-read both
 * components out of the cache so a future revision moving them says so out loud.
 *
 * **The bindings.** Specials are bound by the cache's own name for them (see [SpecialAttackDefs]),
 * which is what makes one registration cover a weapon's poisoned, ornamented, `(cr)`, `(bh)` and
 * deadman variants. [SpecialAttacks.registerByName] throws when a name is not in the cache, but a
 * plugin whose constructor throws registers nothing and says nothing, so [IMPLEMENTED] re-checks
 * every name this server implements without needing to boot a world.
 *
 * [IMPLEMENTED] is also the coverage report, and it now names every special attack in this cache -
 * so `every special attack in the cache is implemented` is the test that says a future cache has
 * added one nobody has built yet.
 */
class SpecialAttackVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
        }

        /** Cache index 3 is the interface index; archive = interface, file = component. */
        private const val INTERFACE_INDEX = 3

        /**
         * Every special attack this server implements, by the cache's name for it. One entry covers
         * every item id that carries that name.
         */
        private val IMPLEMENTED =
            setOf(
                // Melee
                "Puncture", "Abyssal Puncture", "Penance", "Cleave", "Shatter", "Sever", "Wild Stab",
                "Smash", "Pulverize", "Hammer Blow", "Quick Smash", "Slice and Dice", "Powerstab",
                "Sweep", "Shove", "Energy Drain", "Binding Tentacle", "Impale", "Sunder", "Weaken",
                "Rampage", "Sanctuary", "Backstab", "Bear Down", "Behead", "Blood Sacrifice",
                "Blood infusion", "Break Shackles", "Burning barrage", "Celebrate",
                "Crystalline Severance", "Disrupt", "Echo slash", "Eviscerate",
                "Favour of the War God", "Feint", "Lingering Lightning", "Liquify", "Retainer",
                "Saradomin's Lightning", "Shield Bash", "Sol Slam", "Spear Wall", "Tumeken's Light",
                "Unleash", "Virulence", "Wrath of Amascut",
                // Godswords
                "The Judgement", "Warstrike", "Healing Blade", "Ice Cleave",
                // Ranged
                "Snapshot", "Powershot", "Descent of Darkness", "Duality", "Momentum Throw",
                "Chainhit", "Toxic Siphon", "Annihilate", "Armadyl Eye", "Concentrated Shot",
                "Division", "Eclipse", "Evoke", "Hamstring", "Phantom Strike",
                "Scorching shackles", "Snipe", "Soulshot", "Swarm",
                // Magic
                "Condemn", "Immolate", "Invocate", "Power of Death", "Pulsate", "Scatter ashes",
                // Skilling tools
                "Lumber Up", "Rock Knocker", "Fishstabber",
            )
    }

    /** The printable strings inside a component's raw definition, longest first. */
    private fun textOf(
        interfaceId: Int,
        component: Int,
    ): List<String> {
        val data =
            runCatching { CacheManager.cache.data(INTERFACE_INDEX, interfaceId, component, null) }
                .getOrNull() ?: return emptyList()
        val found = mutableListOf<String>()
        val current = StringBuilder()
        data.forEach { byte ->
            val c = byte.toInt() and 0xff
            if (c in 0x20..0x7e) {
                current.append(c.toChar())
            } else {
                if (current.length > 3) found.add(current.toString())
                current.setLength(0)
            }
        }
        if (current.length > 3) found.add(current.toString())
        return found
    }

    @Test
    fun `the combat tab's special attack bar is component 38`() {
        val text = textOf(AttackTab.ATTACK_TAB_INTERFACE_ID, AttackTab.SPECIAL_ATTACK_BAR_COMPONENT)
        assertTrue(
            text.any { "Special Attack" in it },
            "593:${AttackTab.SPECIAL_ATTACK_BAR_COMPONENT} no longer carries the 'Use Special Attack' option, got $text",
        )
    }

    /**
     * The component the bar used to be bound to. Named here so that if a future cache ever does put
     * the bar at 36, this test fails and points at the reason the binding was moved.
     */
    @Test
    fun `component 36 is the set-effect button, not the special attack bar`() {
        val text = textOf(AttackTab.ATTACK_TAB_INTERFACE_ID, 36)
        assertTrue(
            text.any { "set effect" in it },
            "593:36 was the 'Toggle set effect' button, got $text",
        )
        assertTrue(text.none { "Special Attack" in it }, "593:36 now carries the special attack option too")
    }

    @Test
    fun `the minimap special attack orb is 160-35`() {
        val text = textOf(AttackTab.SPECIAL_ORB_INTERFACE_ID, AttackTab.SPECIAL_ORB_COMPONENT)
        assertTrue(
            text.any { "Special Attack" in it },
            "160:${AttackTab.SPECIAL_ORB_COMPONENT} is not the special attack orb, got $text",
        )
    }

    @Test
    fun `the cache still registers special attacks in enums 906 and 1739`() {
        assertTrue(
            SpecialAttackDefs.weapons.size > 200,
            "enum 906 gave only ${SpecialAttackDefs.weapons.size} special attack weapons - it has moved",
        )
        assertTrue(
            SpecialAttackDefs.names.size > 50,
            "enum 1739 gave only ${SpecialAttackDefs.names.size} special attack names - it has moved",
        )
    }

    @Test
    fun `every special attack this server implements is still in the cache`() {
        val missing = IMPLEMENTED.filter { SpecialAttackDefs.itemsWith(it).isEmpty() }
        assertEquals(emptyList(), missing, "no cache item carries these special attacks any more")
    }

    /**
     * The other direction: nothing in the cache is left unbound.
     *
     * A new cache revision adding a weapon - or renaming an existing special - lands here rather
     * than as a player finding a bar that does nothing.
     */
    @Test
    fun `every special attack in the cache is implemented`() {
        val unimplemented = SpecialAttackDefs.names.filter { it !in IMPLEMENTED }.sorted()
        assertEquals(emptyList(), unimplemented, "these special attacks have no implementation")
    }

    /**
     * The dragon dagger is the one that proves the point: nine item ids share one special, and the
     * poisoned grades are the ones players actually carry.
     */
    @Test
    fun `one name binds every variant of a weapon`() {
        val daggers = SpecialAttackDefs.itemsWith("Puncture")
        assertTrue(daggers.size >= 9, "expected every dragon dagger to share 'Puncture', got ${daggers.size}")
        daggers.forEach { id ->
            assertEquals(25, SpecialAttackDefs.cost(id), "dragon dagger $id should cost 25% of the bar")
        }
    }

    /** Costs come from the cache, and the cache does not price every variant the same. */
    @Test
    fun `variants of the same weapon can cost different amounts`() {
        val mauls = SpecialAttackDefs.itemsWith("Quick Smash").mapNotNull { SpecialAttackDefs.cost(it) }.toSet()
        assertTrue(
            mauls.size > 1,
            "the granite mauls were expected to disagree on price (60% and 50%), got $mauls",
        )
    }

    /**
     * Two different weapons are called "Smash" and do different things, which is why the dragon
     * warhammer's binding filters on the description.
     */
    @Test
    fun `Smash is shared by two unrelated weapons`() {
        val descriptions = SpecialAttackDefs.itemsWith("Smash").mapNotNull { SpecialAttackDefs.description(it) }.toSet()
        assertTrue(
            descriptions.any { "50% more damage" in it } && descriptions.any { "minimum of 25% extra damage" in it },
            "expected the dragon warhammer and Statius's warhammer to share the name 'Smash', got $descriptions",
        )
    }
}
