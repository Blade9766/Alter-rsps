package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.api.EquipmentType
import org.alter.api.cfg.Varp
import org.alter.api.ext.setVarp
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.service.game.ItemMetadataService
import org.alter.plugins.content.combat.strategy.ranged.ammo.EnchantedBolt
import org.alter.plugins.content.combat.strategy.ranged.weapon.CrossbowType
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Checks that enchanted bolt effects are actually reachable, and that the bolts they and the
 * poisoned bolts live on are ammo a crossbow will accept.
 *
 * Three separate gates stand between equipping a bolt and its effect firing, and every one of them
 * fails silently: [CrossbowType] decides whether the bolt can be fired at all, [EnchantedBolt.roll]
 * refuses anything that is not a crossbow, and the `(e)` item ids have to be the ones actually in
 * the cache. A bolt that clears none of them behaves exactly like a bolt that clears all of them
 * and keeps rolling badly.
 *
 * The activation roll itself is random, so it is driven here with a chance multiplier large enough
 * to make it certain. What is being asserted is that the *lookup* reaches the effect, not that a
 * 6% chance comes up.
 */
class EnchantedBoltVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
            ItemMetadataService().loadAll()
        }

        /** Enough to make any activation chance certain, so the roll stops being the variable. */
        private const val ALWAYS = 100.0

        private const val FIRST_STYLE = 0
    }

    private fun world() = World(EatingVerify.GAME_CONTEXT, EatingVerify.DEV_CONTEXT)

    private fun armed(
        world: World,
        weapon: String,
        ammo: String?,
    ): Player {
        val player = Player(world)
        player.setVarp(Varp.WEAPON_ATTACK_STYLE, FIRST_STYLE)
        player.equipment[EquipmentType.WEAPON.id] = Item(getRSCM(weapon))
        if (ammo != null) {
            player.equipment[EquipmentType.AMMO.id] = Item(getRSCM(ammo))
        }
        return player
    }

    @Test
    fun `every enchanted bolt is reachable from a crossbow`() {
        val world = world()
        val target = Player(world)

        EnchantedBolt.values.forEach { bolt ->
            bolt.items.forEach { item ->
                val name = CacheManager.getItem(item).name
                // A dragon crossbow fires every tier, so one weapon covers the whole table.
                val player = armed(world, "item.dragon_crossbow", null)
                player.equipment[EquipmentType.AMMO.id] = Item(item)

                val rolled = EnchantedBolt.roll(player, target, world, chanceMultiplier = ALWAYS)
                assertEquals(bolt, rolled, "\"$name\" (id $item) did not resolve to ${bolt.effect}")
            }
        }
    }

    @Test
    fun `unenchanted and non-crossbow cases resolve to nothing`() {
        val world = world()
        val target = Player(world)

        // The plain tipped bolt carries no effect - only the (e) form does.
        val plain = armed(world, "item.dragon_crossbow", "item.emerald_bolts")
        assertNull(EnchantedBolt.roll(plain, target, world, ALWAYS), "unenchanted emerald bolts have no effect")

        // A bow is not a crossbow, whatever is in the quiver.
        val bow = armed(world, "item.magic_shortbow", "item.emerald_bolts_e")
        assertNull(EnchantedBolt.roll(bow, target, world, ALWAYS), "a shortbow does not fire bolt effects")

        // An empty quiver has nothing to roll.
        val empty = armed(world, "item.dragon_crossbow", null)
        assertNull(EnchantedBolt.roll(empty, target, world, ALWAYS), "an empty quiver has no bolt effect")
    }

    /**
     * Emerald bolts are the only enchanted bolt whose effect is poison, and they reach it through
     * [EnchantedBolt.applyOnHit] rather than through
     * [org.alter.plugins.content.mechanics.poison.CombatPoison] - so the two paths are independent
     * and both have to work.
     */
    @Test
    fun `emerald bolts are the poisoning enchantment`() {
        val poisoning = EnchantedBolt.values.filter { it.effect == "Magical Poison" }
        assertEquals(listOf(EnchantedBolt.EMERALD), poisoning, "only emerald bolts poison")
    }

    /**
     * The ammo gate in [org.alter.plugins.content.combat.strategy.RangedCombatStrategy] refuses to
     * attack at all with a bolt the crossbow does not list, so a coated or enchanted bolt missing
     * from these arrays would not just lose its effect - it would be unusable.
     */
    @Test
    fun `a rune crossbow accepts coated and enchanted bolts`() {
        val runeCrossbow = assertNotNull(CrossbowType.values.firstOrNull { it.item == getRSCM("item.rune_crossbow") })

        listOf(
            "item.runite_bolts",
            "item.runite_bolts_p",
            "item.runite_bolts_p_9298",
            "item.runite_bolts_p_9305",
            "item.emerald_bolts_e",
            "item.diamond_bolts_e",
            "item.onyx_bolts_e",
        ).forEach { key ->
            val id = getRSCM(key)
            assertTrue(id in runeCrossbow.ammo, "${CacheManager.getItem(id).name} (id $id) cannot be fired from a rune crossbow")
        }

        // Dragon bolts are above its tier and stay refused.
        assertTrue(
            getRSCM("item.dragon_bolts") !in runeCrossbow.ammo,
            "a rune crossbow should not fire dragon bolts",
        )
    }
}
