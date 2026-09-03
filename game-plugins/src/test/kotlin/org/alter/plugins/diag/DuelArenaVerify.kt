package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.api.EquipmentType
import org.alter.plugins.content.areas.duelarena.DuelArena
import org.alter.plugins.content.areas.duelarena.DuelRule
import org.alter.plugins.content.areas.duelarena.DuelSlot
import org.alter.plugins.content.areas.duelarena.DuelStage
import org.junit.BeforeClass
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Checks the Duel Arena's tables against the cache they were transcribed from.
 *
 * Almost everything in [DuelRule] and [DuelSlot] is a number copied out of a decompiled client
 * script - which rule owns bit 12, which component draws it, where a locked worn slot sits in the
 * confirm screen's flag word. Every one of those is silently wrong if mistyped: the screen still
 * opens, the toggles still click, and the duel is simply fought under different rules than the ones
 * the players ticked. Nothing at runtime would ever complain.
 *
 * So the same facts are re-derived here from the enums in the cache and compared. Enum 4209 is the
 * rule -> component map, 4285 the worn slot -> overlay map, 4388 the worn slot -> tooltip map, and
 * 4210 the confirm-screen flag -> icon map that pins down the `14 + slot` offset.
 */
class DuelArenaVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
        }

        private const val ENUM_RULE_COMPONENT = 4209
        private const val ENUM_FLAG_TO_ICON = 4210
        private const val ENUM_SLOT_COMPONENT = 4285
        private const val ENUM_SLOT_TOOLTIP = 4388

        private fun enumValues(id: Int): Map<Int, Any> =
            CacheManager.getEnum(id).values.entries.associate { (k, v) -> (k as Number).toInt() to v }
    }

    /**
     * Each rule's component is the one enum 4209 gives for its bit. This is the single mapping that
     * decides which row a click means, so a swapped pair here would silently trade two rules.
     */
    @Test
    fun `every rule maps to the component the cache gives for its bit`() {
        val expected = enumValues(ENUM_RULE_COMPONENT)
        assertEquals(DuelRule.values.size, expected.size, "rule count differs from enum $ENUM_RULE_COMPONENT")

        DuelRule.values.forEach { rule ->
            val hash = expected[rule.bit]
            assertTrue(hash != null, "enum $ENUM_RULE_COMPONENT has no entry for bit ${rule.bit} (${rule.title})")
            val component = (hash as Number).toInt() and 0xffff
            val interfaceId = (hash.toInt() shr 16)
            assertEquals(DuelArena.OPTIONS_INTERFACE, interfaceId, "${rule.title} is not on the duel options screen")
            assertEquals(component, rule.component, "${rule.title} draws on the wrong component")
        }
    }

    /** No two rules may share a bit, or one would toggle the other. */
    @Test
    fun `rule bits are unique`() {
        val bits = DuelRule.values.map { it.bit }
        assertEquals(bits.size, bits.toSet().size, "two rules share a bit: $bits")
    }

    /**
     * The eleven lockable slots are exactly the keys of enum 4285, and each carries the cache's own
     * wording for what locking it does.
     */
    @Test
    fun `lockable slots match the cache`() {
        val components = enumValues(ENUM_SLOT_COMPONENT)
        val tooltips = enumValues(ENUM_SLOT_TOOLTIP)

        assertEquals(components.keys.sorted(), DuelSlot.values.map { it.slot }.sorted(), "lockable slot set differs")
        assertEquals(DuelSlot.values.size, tooltips.size, "tooltip count differs from enum $ENUM_SLOT_TOOLTIP")

        DuelSlot.values.forEach { slot ->
            assertEquals(tooltips[slot.slot], slot.tooltip, "${slot.label} tooltip does not match the cache")
        }
    }

    /**
     * The confirm screen packs locked worn slots into bits 14..27 of the same word as the rules.
     * Enum 4210 maps each of those flags to the worn icon it flashes, and the only offset that
     * makes it line up is `14 + equipment slot` - not `14 + position in the list`, which is the
     * mistake this test exists to catch.
     */
    @Test
    fun `worn slot flag bits are the equipment slot plus fourteen`() {
        val flagToIcon = enumValues(ENUM_FLAG_TO_ICON)
        assertEquals(DuelSlot.values.size, flagToIcon.size, "flag count differs from enum $ENUM_FLAG_TO_ICON")

        DuelSlot.values.forEach { slot ->
            val icon = flagToIcon[slot.flagBit]
            assertTrue(icon != null, "${slot.label}: enum $ENUM_FLAG_TO_ICON has no flag ${slot.flagBit}")
            assertEquals(
                DuelSlot.ICON_ORDER.indexOf(slot),
                (icon as Number).toInt(),
                "${slot.label} flashes the wrong worn icon",
            )
        }

        // Every flag is inside the range [clientscript,6193] reads with getbit_range(flags, 14, 27).
        DuelSlot.values.forEach { slot ->
            assertTrue(slot.flagBit in 14..27, "${slot.label} flag ${slot.flagBit} is outside the confirm screen's range")
        }
    }

    /**
     * The icon order is what turns a click's sub-component id back into a slot, and it is the order
     * `[clientscript,duel_initworn]` creates them in - ammo fourth, not last.
     */
    @Test
    fun `icon order covers every slot exactly once`() {
        assertEquals(DuelSlot.values.toSet(), DuelSlot.ICON_ORDER.toSet(), "icon order is missing a slot")
        assertEquals(DuelSlot.values.size, DuelSlot.ICON_ORDER.size, "icon order repeats a slot")
        assertEquals(DuelSlot.AMMO, DuelSlot.ICON_ORDER[3], "ammo is not in the quiver position")
        assertEquals(DuelSlot.WEAPON, DuelSlot.ICON_ORDER[4], "weapon is not where duel_initworn puts it")
    }

    /** The slot numbers are equipment slots, so they have to agree with [EquipmentType]. */
    @Test
    fun `slot ids are real equipment slots`() {
        val real = EquipmentType.values.map { it.id }.toSet()
        DuelSlot.values.forEach { slot ->
            assertTrue(slot.slot in real, "${slot.label} uses slot ${slot.slot}, which is not an equipment slot")
        }
    }

    /**
     * A preset that turned off all three attack styles would produce a duel neither player could
     * win, which the session refuses interactively - the presets must not walk into it themselves.
     */
    @Test
    fun `presets leave at least one way to attack`() {
        listOf("whip" to DuelRule.WHIP_PRESET, "boxing" to DuelRule.BOXING_PRESET).forEach { (name, preset) ->
            assertTrue(
                DuelRule.ATTACK_STYLE_RULES.any { it !in preset },
                "the $name preset disables every attack style",
            )
        }
    }

    /**
     * The four arenas have to be real, separate and the right shape - a duel is teleported into
     * whichever one is free, so an arena whose interior overlapped another's would put two fights
     * in the same place.
     */
    @Test
    fun `the four arenas are distinct and hold their start tiles`() {
        assertEquals(4, DuelArena.ARENAS.size)

        DuelArena.ARENAS.forEach { plot ->
            listOf(true, false).forEach { adjacent ->
                val (a, b) = plot.startTiles(adjacent)
                assertTrue(plot.contains(a), "${plot.name}: start tile $a is outside its own arena")
                assertTrue(plot.contains(b), "${plot.name}: start tile $b is outside its own arena")
                assertTrue(a != b, "${plot.name}: both players would start on the same tile")
            }

            // Apart must genuinely be apart, and adjacent genuinely adjacent.
            val (apartA, apartB) = plot.startTiles(adjacent = false)
            assertTrue(apartA.getDistance(apartB) > 5, "${plot.name}: 'apart' start tiles are within melee range")
            val (nearA, nearB) = plot.startTiles(adjacent = true)
            assertEquals(1, nearA.getDistance(nearB), "${plot.name}: 'adjacent' start tiles are not neighbours")
        }

        DuelArena.ARENAS.forEachIndexed { i, first ->
            DuelArena.ARENAS.drop(i + 1).forEach { second ->
                val overlaps = first.maxX >= second.minX && second.maxX >= first.minX &&
                    first.maxZ >= second.minZ && second.maxZ >= first.minZ
                assertTrue(!overlaps, "${first.name} overlaps ${second.name}")
            }
        }
    }

    /** No arena may sit inside the lobby, or standing in one would still offer Challenge. */
    @Test
    fun `arenas are outside the challenge lobby`() {
        DuelArena.ARENAS.forEach { plot ->
            val (a, b) = plot.startTiles(adjacent = false)
            assertTrue(!DuelArena.inLobby(a), "${plot.name} start tile is inside the lobby")
            assertTrue(!DuelArena.inLobby(b), "${plot.name} start tile is inside the lobby")
        }
        assertTrue(DuelArena.inLobby(DuelArena.LOBBY_TILE), "players are returned to a tile outside the lobby")
    }

    /**
     * Moving between the three screens closes the one before it. If that counted as walking away,
     * agreeing the stake would cancel the duel it had just agreed - which is what this pins down.
     */
    @Test
    fun `advancing between screens is not treated as abandoning the duel`() {
        val order = listOf(DuelStage.STAKE, DuelStage.OPTIONS, DuelStage.CONFIRM)

        order.zipWithNext().forEach { (from, to) ->
            val closing = from.screen!!
            assertTrue(
                !to.isAbandonedBy(closing),
                "advancing $from -> $to closes $closing and would be read as a decline",
            )
        }

        // Closing the screen the duel is actually sitting on still is a decline.
        order.forEach { stage ->
            assertTrue(stage.isAbandonedBy(stage.screen!!), "closing $stage's own screen should end the duel")
        }
    }

    /** Once the fight has started there is no screen left to walk away from. */
    @Test
    fun `committed stages cannot be abandoned by closing a screen`() {
        listOf(DuelStage.COUNTDOWN, DuelStage.FIGHTING, DuelStage.ENDED).forEach { stage ->
            listOf(335, 755, 756).forEach { screen ->
                assertTrue(!stage.isAbandonedBy(screen), "$stage should not be cancelled by closing $screen")
            }
        }
    }

    /** Two stages sharing a screen would make the guard above ambiguous. */
    @Test
    fun `each negotiation stage has its own screen`() {
        val screens = DuelStage.values().mapNotNull { it.screen }
        assertEquals(screens.size, screens.toSet().size, "two stages share a screen: $screens")
        assertEquals(3, screens.size, "expected exactly three screens with a stage")
    }

    /**
     * Every client script the duel calls, checked against the number of arguments the script itself
     * declares.
     *
     * This is the bug that cost the most to find. A `runClientScript` with the wrong arity does not
     * error, log, or fail visibly - the client simply discards the call, and the screen comes up
     * empty. It bit twice: the stake screen passed `interface_inv_init_big` five option strings
     * when it wants nine (inherited from the trade screen, where it had silently been broken all
     * along), and the duel options screen looked identical when its own init script did nothing.
     *
     * The counts are decoded from each script's trailer, so this compares the real call sites
     * against the real cache rather than against a transcription.
     */
    @Test
    fun `client script calls match the arity the cache declares`() {
        data class Call(val id: Int, val what: String, val ints: Int, val strings: Int)

        val calls =
            listOf(
                // DuelSession.openOptionsScreen: (index, title, component, description)
                Call(6170, "duel row builder", ints = 2, strings = 2),
                // DuelSession.refreshOptions
                Call(6175, "duel options refresh", ints = 0, strings = 0),
                // DuelSession.announceChange
                Call(6176, "duel option changed", ints = 1, strings = 0),
                // DuelSession.openConfirmScreen: (flags, rules, slots, opponent)
                Call(6193, "duel confirm summary", ints = 3, strings = 1),
                // DuelSession.refreshOptions -> duel_initworn
                Call(205, "duel worn icons", ints = 0, strings = 0),
                // DuelSession.armAcceptButton: (component, clock)
                Call(1443, "duel accept button", ints = 2, strings = 0),
                // DuelSession.openStakeScreen: 6 ints then nine option strings
                Call(150, "inventory overlay init", ints = 6, strings = 9),
            )

        calls.forEach { call ->
            val (ints, strings) = scriptArity(call.id)
            assertEquals(
                call.ints to call.strings,
                ints to strings,
                "${call.what} (script ${call.id}) is called with ${call.ints} int(s) and " +
                    "${call.strings} string(s), but the cache declares $ints and $strings",
            )
        }
    }

    /**
     * The int- and string-argument counts a client script declares, read out of its trailer: the
     * last two bytes give the switch-block length, and the twelve bytes before that hold the opcode
     * count, the two local counts and then the two argument counts.
     */
    private fun scriptArity(id: Int): Pair<Int, Int> {
        val data = CacheManager.cache.data(12, id, 0, null)
        assertTrue(data != null, "client script $id is missing from the cache")
        fun u16(offset: Int) = ((data!![offset].toInt() and 0xff) shl 8) or (data[offset + 1].toInt() and 0xff)
        val end = data!!.size - 2
        val start = end - u16(end) - 12
        return u16(start + 8) to u16(start + 10)
    }

    /**
     * The stake must be taken from the player's real inventory, never restored from a snapshot.
     *
     * `DuelSession.begin` used to overwrite the real inventory with the stake screen's working copy.
     * That copy is taken when the screen opens, and the inventory tab is usable again from the
     * options screen onwards - so equipping an item in between left it both worn and rewritten back
     * into the inventory. A duplication bug, reported from live testing.
     *
     * The source is asserted directly: the fix is structural, and a future edit that reintroduces
     * `inventory.removeAll()` followed by a copy-back would be silent and very costly.
     */
    @Test
    fun `the duel never overwrites a real inventory with its working copy`() {
        val source =
            java.io.File("src/main/kotlin/org/alter/plugins/content/areas/duelarena/DuelSession.kt")
                .readText()

        assertTrue(
            !source.contains("side.inventory.forEachIndexed"),
            "begin() is writing the stake screen's snapshot back over the real inventory again",
        )
        assertTrue(
            source.contains("private fun canCollectStakes()") && source.contains("private fun collectStakes()"),
            "the stake must be checked and then taken from the real inventory",
        )
        // The check has to happen before an arena is claimed, or a refusal leaves one allocated.
        val checkAt = source.indexOf("canCollectStakes()")
        val claimAt = source.indexOf("DuelArenas.claim(this)")
        assertTrue(checkAt in 1 until claimAt, "stakes must be verified before an arena is claimed")
    }
}
