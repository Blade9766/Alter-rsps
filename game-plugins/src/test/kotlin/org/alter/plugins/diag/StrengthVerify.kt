package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import dev.openrune.cache.MAPS
import dev.openrune.cache.filestore.TileData
import dev.openrune.cache.filestore.loadLocations
import dev.openrune.cache.filestore.loadTerrain
import org.alter.api.Skills
import org.alter.plugins.content.items.consumables.Boost
import org.alter.plugins.content.items.consumables.Drain
import org.alter.plugins.content.items.consumables.Damage
import org.alter.plugins.content.items.consumables.DivineBoost
import org.alter.plugins.content.items.consumables.food.Food
import org.alter.plugins.content.items.consumables.potions.Divine
import org.alter.plugins.content.items.consumables.potions.Potion
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.weapons.dragonbattleaxe.DragonBattleaxePlugin
import org.alter.game.DevContext
import org.alter.game.GameContext
import org.alter.game.Server
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.plugin.PluginRepository
import org.alter.game.saving.formats.SaveFormatType
import org.alter.plugins.content.skills.strength.GrappleShortcuts
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Checks on the non-combat half of Strength: the drinks that boost it, the lesson that spends it
 * on barehanded fishing, and the weapons it gates.
 *
 * [EatingVerify] already proves every drink *binds*, and [EquipmentRequirementVerify] proves the
 * weapon table parses and applies. What is left, and what fails silently, is whether the numbers
 * are the ones the wiki gives - a boost typed as flat where it should be a percentage still binds
 * and still applies, it is just wrong forever. So the Strength-affecting drinks are asserted
 * against their published "N% + M" modifiers here, one place a careless edit would be caught.
 */
class StrengthVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /** Otto Godblessed's tile, from the wiki's map template: Otto's Grotto, plane 0. */
        const val OTTO_X = 2502
        const val OTTO_Z = 3489

        /** Equipment slot 13 - the ammunition slot the grapple has to sit in. */
        const val AMMO_SLOT = 13

        /** Nothing below reads either; both only exist so a [World] can be built. */
        val GAME_CONTEXT =
            GameContext(
                initialLaunch = false,
                name = "test",
                revision = 228,
                saveFormat = SaveFormatType.JSON,
                cycleTime = 600,
                playerLimit = 1,
                home = Tile(3222, 3222),
                skillCount = 23,
                npcStatCount = 6,
                runEnergy = true,
                gItemPublicDelay = 100,
                gItemDespawnDelay = 300,
                preloadMaps = false,
            )

        val DEV_CONTEXT =
            DevContext(
                debugExamines = false,
                debugObjects = false,
                debugButtons = false,
                debugMagicSpells = false,
                debugItemActions = false,
                debugPackets = false,
            )

        /**
         * Every drink that moves Strength, with the wiki's own modifiers as `flat to percent`.
         * A boost is `floor(BaseLevel * percent / 100) + flat`, a drain the same off the current
         * level, which is exactly what [Boost] and [Drain] compute.
         */
        val STRENGTH_DRINKS =
            mapOf(
                Food.BEER to Pair(Boost(Skills.STRENGTH, 1, 2), Drain(Skills.ATTACK, 1, 6)),
                Food.ASGARNIAN_ALE to Pair(Boost(Skills.STRENGTH, 2, 0), Drain(Skills.ATTACK, 2, 5)),
                Food.DRAGON_BITTER to Pair(Boost(Skills.STRENGTH, 2, 0), Drain(Skills.ATTACK, 2, 5)),
                Food.GROG to Pair(Boost(Skills.STRENGTH, 1, 4), Drain(Skills.ATTACK, 3, 5)),
                Food.BEER_TANKARD to Pair(Boost(Skills.STRENGTH, 2, 4), Drain(Skills.ATTACK, 2, 10)),
                Food.KEG_OF_BEER to Pair(Boost(Skills.STRENGTH, 2, 10), Drain(Skills.ATTACK, 5, 50)),
                Food.BRANDY to Pair(Boost(Skills.STRENGTH, 1, 5), Drain(Skills.ATTACK, 3, 2)),
                Food.GIN to Pair(Boost(Skills.STRENGTH, 1, 5), Drain(Skills.ATTACK, 3, 2)),
                Food.VODKA to Pair(Boost(Skills.STRENGTH, 1, 5), Drain(Skills.ATTACK, 3, 2)),
                Food.WHISKY to Pair(Boost(Skills.STRENGTH, 1, 5), Drain(Skills.ATTACK, 3, 2)),
            )

        /** The ales that cost Strength rather than paying it, and what they buy with it. */
        val STRENGTH_DRAINING_ALES =
            mapOf(
                Food.DWARVEN_STOUT to Drain(Skills.STRENGTH, 2, 4),
                Food.GREENMANS_ALE to Drain(Skills.STRENGTH, 3, 0),
                Food.WIZARDS_MIND_BOMB to Drain(Skills.STRENGTH, 1, 5),
                Food.CHEFS_DELIGHT to Drain(Skills.STRENGTH, 2, 5),
                Food.SLAYERS_RESPITE to Drain(Skills.STRENGTH, 2, 2),
                Food.AXEMANS_FOLLY to Drain(Skills.STRENGTH, 3, 0),
                Food.CIDER to Drain(Skills.STRENGTH, 2, 0),
                Food.BANDITS_BREW to Drain(Skills.STRENGTH, 3, 6),
            )
    }

    private fun terrainFor(
        rx: Int,
        rz: Int,
    ): Array<Array<Array<TileData>>>? = CacheManager.cache.data(MAPS, "m${rx}_$rz")?.let { loadTerrain(it) }

    @Test
    fun `every Strength-boosting drink carries the published modifiers`() {
        STRENGTH_DRINKS.forEach { (food, expected) ->
            val (boost, drain) = expected
            assertTrue(boost in food.effects, "${food.item} is missing $boost; it has ${food.effects}")
            assertTrue(drain in food.effects, "${food.item} is missing $drain; it has ${food.effects}")
        }
    }

    @Test
    fun `every Strength-draining ale carries the published drain`() {
        STRENGTH_DRAINING_ALES.forEach { (food, drain) ->
            assertTrue(drain in food.effects, "${food.item} is missing $drain; it has ${food.effects}")
        }
    }

    /**
     * Every drink is served in something, and getting the leftover wrong is the kind of mistake
     * that only shows up as an item that quietly appears from nowhere - or a beer glass that
     * never does, which is a money maker in the live game.
     */
    @Test
    fun `glass-served drinks hand back a beer glass and the rest hand back nothing`() {
        val glass = "item.beer_glass"
        val glassServed =
            listOf(
                Food.BEER, Food.ASGARNIAN_ALE, Food.DRAGON_BITTER, Food.GROG, Food.DWARVEN_STOUT,
                Food.GREENMANS_ALE, Food.WIZARDS_MIND_BOMB, Food.CHEFS_DELIGHT, Food.SLAYERS_RESPITE,
                Food.AXEMANS_FOLLY, Food.CIDER, Food.BANDITS_BREW, Food.MOONLIGHT_MEAD,
            )
        glassServed.forEach { assertEquals(glass, it.replacement, "${it.item} should leave a beer glass") }

        listOf(Food.BEER_TANKARD, Food.KEG_OF_BEER, Food.BRANDY, Food.GIN, Food.VODKA, Food.WHISKY)
            .forEach { assertEquals(null, it.replacement, "${it.item} should leave nothing behind") }
    }

    /**
     * The keg trap: two cache items are named "Keg of beer" and only 3801 can be drunk. Binding
     * the other one throws in `EatingPlugin`'s constructor, which takes eating away from every
     * food in the game rather than from the keg.
     */
    @Test
    fun `every drink actually carries a Drink option`() {
        Food.values.filter { it.option.equals("drink", ignoreCase = true) }.forEach { food ->
            val def = CacheManager.getItem(getRSCM(food.item))
            assertTrue(
                def.interfaceOptions.any { it?.equals("Drink", ignoreCase = true) == true },
                "${food.item} (${def.name}) has no Drink option; it has " +
                    "${def.interfaceOptions.filterNotNull().filter { o -> o.isNotBlank() }}",
            )
        }
    }

    /**
     * `onObjOption(obj, option)` throws when the named option is not on the object, and a plugin
     * whose constructor throws registers nothing - so one bad key here would take every grapple
     * shortcut away at once rather than the one that named it.
     */
    @Test
    fun `every grapple shortcut object carries a Grapple option`() {
        GrappleShortcuts.ALL.forEach { shortcut ->
            shortcut.objects.forEach { key ->
                val id = getRSCM(key)
                assertTrue(id > 0, "$key did not resolve to an object id.")
                val def = assertNotNull(CacheManager.getObject(id), "$key ($id) is not in this cache.")
                assertTrue(
                    def.actions.filterNotNull().any { it.equals("Grapple", ignoreCase = true) },
                    "${shortcut.name}: $key ($id) has no Grapple option; it has " +
                        "${def.actions.filterNotNull().filter { o -> o.isNotBlank() }}",
                )
            }
        }
    }

    /**
     * An object id that is never placed in any map region binds cleanly and then never fires, so
     * the shortcut simply is not in the world with nothing logged. Every id below was found by a
     * full-cache scan; this asserts the scan still holds.
     */
    @Test
    fun `every grapple shortcut object is placed somewhere in this cache`() {
        val wanted = GrappleShortcuts.ALL.flatMap { it.objects }.associateBy { getRSCM(it) }
        val found = HashSet<Int>()

        for (rx in 0 until 256) {
            for (rz in 0 until 256) {
                val land = runCatching { CacheManager.cache.data(MAPS, "l${rx}_$rz") }.getOrNull() ?: continue
                runCatching {
                    loadLocations(land) { loc ->
                        if (loc.id in wanted) {
                            found += loc.id
                        }
                    }
                }
            }
        }

        val missing = wanted.filterKeys { it !in found }.values
        assertTrue(missing.isEmpty(), "grapple objects that are not placed anywhere: $missing")
    }

    /**
     * The grapple trap, and the same shape as the keg above: two cache items are named "Mith
     * grapple" and only 9419 can be equipped at all, so checking for the other one would mean no
     * player could ever satisfy the requirement.
     */
    @Test
    fun `the mith grapple used by the shortcuts is the wieldable one`() {
        val id = getRSCM(GrappleShortcuts.MITH_GRAPPLE)
        val def = CacheManager.getItem(id)
        assertEquals("Mith grapple", def.name, "${GrappleShortcuts.MITH_GRAPPLE} ($id) is not a mith grapple.")
        assertEquals(AMMO_SLOT, def.equipSlot, "${GrappleShortcuts.MITH_GRAPPLE} ($id) does not go in the ammo slot.")
    }

    /**
     * `PotionPlugin` binds every dose of every potion the same way `EatingPlugin` binds food, so a
     * divine dose whose item has no Drink option would throw in its constructor and take *all*
     * potion drinking with it - the keg trap again, one package over.
     */
    @Test
    fun `every divine potion dose resolves and carries a Drink option`() {
        listOf(Potion.DIVINE_SUPER_STRENGTH, Potion.DIVINE_SUPER_COMBAT).forEach { potion ->
            assertEquals(4, potion.doses.size, "$potion should have four doses")
            potion.doses.forEach { key ->
                val id = getRSCM(key)
                assertTrue(id > 0, "$key did not resolve to an item id.")
                val def = CacheManager.getItem(id)
                assertTrue(
                    def.interfaceOptions.any { it?.equals("Drink", ignoreCase = true) == true },
                    "$key (${def.name}) has no Drink option; it has " +
                        "${def.interfaceOptions.filterNotNull().filter { o -> o.isNotBlank() }}",
                )
            }
        }
    }

    /**
     * The whole point of a divine potion is the floor, and a `DivineBoost` that silently degraded
     * to an ordinary `Boost` would look identical on drinking and only differ a minute later.
     */
    @Test
    fun `divine potions boost through DivineBoost and cost hitpoints`() {
        listOf(Potion.DIVINE_SUPER_STRENGTH, Potion.DIVINE_SUPER_COMBAT).forEach { potion ->
            assertTrue(
                DivineBoost(Skills.STRENGTH, 5, 15) in potion.effects,
                "$potion is missing the divine Strength boost; it has ${potion.effects}",
            )
            assertTrue(
                Damage(Divine.HITPOINT_COST) in potion.effects,
                "$potion should cost ${Divine.HITPOINT_COST} hitpoints; it has ${potion.effects}",
            )
            assertEquals(
                Divine.MIN_HITPOINTS,
                potion.minHitpoints,
                "$potion should refuse a dose below ${Divine.MIN_HITPOINTS} hitpoints",
            )
        }
    }

    /**
     * Rampage is registered `executeInstantly` because it hits nobody - the spec bar fires it with
     * a null target. Registered against a normal special instead, clicking the bar would only arm
     * it and the boost would never happen.
     */
    @Test
    fun `the dragon battleaxe special is instant and costs the whole bar`() {
        // The plugin's init only writes into SpecialAttacks, so a bare world and server are enough
        // and this stays off a full server boot. The registry is not cleared first: nothing else in
        // the suite registers a battleaxe, and clearing a global other tests may rely on is the
        // riskier of the two.
        val world = World(GAME_CONTEXT, DEV_CONTEXT)
        DragonBattleaxePlugin(PluginRepository(world), world, Server())

        listOf("item.dragon_battleaxe", "item.dragon_battleaxe_cr").forEach { key ->
            val id = getRSCM(key)
            val special = assertNotNull(SpecialAttacks.attacks[id], "$key ($id) registered no special attack")
            assertEquals(100, special.energyRequired, "$key should cost the full special bar")
            assertTrue(special.executeOnSpecBar, "$key must fire straight off the spec bar - it has no target")
        }
    }

    @Test
    fun `jangerberries are edible and boost Strength`() {
        val def = CacheManager.getItem(getRSCM(Food.JANGERBERRIES.item))
        assertTrue(
            def.interfaceOptions.any { it?.equals("Eat", ignoreCase = true) == true },
            "jangerberries (${def.name}) have no Eat option",
        )
        assertTrue(
            Boost(Skills.STRENGTH, 1, 0) in Food.JANGERBERRIES.effects,
            "jangerberries should boost Strength by 1; they have ${Food.JANGERBERRIES.effects}",
        )
    }

    @Test
    fun `Otto Godblessed resolves and can be talked to`() {
        val id = getRSCM("npc.otto_godblessed")
        assertTrue(id > 0, "npc.otto_godblessed did not resolve.")
        val def = assertNotNull(CacheManager.getNpc(id), "otto_godblessed ($id) is not in this cache.")
        assertEquals("Otto Godblessed", def.name, "npc $id is not Otto.")
        assertTrue(
            def.actions.filterNotNull().any { it.equals("Talk-to", ignoreCase = true) },
            "Otto ($id) has no Talk-to option, so the lesson could never be asked for.",
        )
    }

    /**
     * Otto's Grotto is a long way from anything else this server builds, so the mapsquare being
     * absent is a real possibility - and an npc spawned into a region that never loads simply is
     * not there, with nothing logged.
     */
    @Test
    fun `Otto's tile is in this cache and walkable`() {
        val rx = OTTO_X shr 6
        val rz = OTTO_Z shr 6
        val tiles = assertNotNull(terrainFor(rx, rz), "mapsquare ${rx}_$rz (Otto's Grotto) is not in the cache")

        val data = tiles[0][OTTO_X - (rx shl 6)][OTTO_Z - (rz shl 6)]
        assertTrue(
            data.overlayId.toInt() != 0 || data.underlayId.toInt() != 0,
            "($OTTO_X, $OTTO_Z, 0) has no floor",
        )
        assertTrue((data.settings.toInt() and 0x1) == 0, "($OTTO_X, $OTTO_Z, 0) is flagged BLOCK_WALK")
    }
}
