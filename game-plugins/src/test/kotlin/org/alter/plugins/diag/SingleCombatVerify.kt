package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.game.Server
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.attr.COMBAT_TARGET_FOCUS_ATTR
import org.alter.game.model.attr.SINGLE_COMBAT_ATTACKER_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.plugins.content.combat.SingleCombat
import org.alter.plugins.content.mechanics.aggro.NpcAggroPlugin
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.junit.BeforeClass
import java.lang.ref.WeakReference
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Single-way combat: outside a multi-combat area, one monster fights a player at a time.
 *
 * Nothing enforced this until the second bestiary pass. `setMultiCombatRegion` and `Tile.isMulti`
 * only ever drove the client's multi-combat icon - `content/combat/strategy/ranged/RangedAoe` and
 * `content/npcs/darkwizard` both say so - so every aggressive npc within its radius picked a target
 * independently and they all picked the same player. Walking into any camp meant being attacked by
 * the whole camp; it went unnoticed while the spawned monsters hit for 3, and became obvious the
 * moment a pack of green dragons could breathe 50 apiece.
 *
 * The rule lives in [SingleCombat], and it is worth a test of its own because the failure is
 * invisible in exactly the same way the bug was: nothing errors, nothing logs, the fight just has
 * more monsters in it than it should.
 *
 * ## It was written three times before it was written once
 *
 * `content/npcs/darkwizard` implemented it first, `content/npcs/slayer` copied that method verbatim,
 * and the aggression layer added a third - and the two copies each declared their **own private**
 * `ENGAGED_BY` attribute, so a dark wizard and an aberrant spectre could hold the same player at
 * once and neither could see the other's claim. `a claim taken through the attack loop blocks a
 * different monster's attack loop` is the case that used to fail.
 *
 * ## Why the aggression cases exercise `singleCombatAllows` and not `canAttack`
 *
 * `canAttack` opens with `target.isOnline`, which is `index > 0` - true only for a player the login
 * pipeline has assigned a slot. A test player has no slot, so `canAttack` would refuse every one of
 * these cases before reaching the rule and the test would pass for the wrong reason. The single-way
 * rule is a separate function precisely so it can be asked directly.
 *
 * Engagement is set by writing [COMBAT_TARGET_FOCUS_ATTR] rather than calling `Pawn.attack`, because
 * that is exactly what `attack` does with it - the rest of `attack` is facing, queue interruption
 * and route-finding that a headless test has no client for.
 */
class SingleCombatVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
            RSCM.init()
        }

        /** An ordinary tile with no multi-combat chunk registered against it. */
        private val SINGLE = Tile(3200, 3200)
    }

    private fun world(): World = World(EatingVerify.GAME_CONTEXT, EatingVerify.DEV_CONTEXT)

    private fun aggro(world: World): NpcAggroPlugin = NpcAggroPlugin(world.plugins, world, Server())

    /** A running index, so each test npc looks spawned. See [nextIndex]. */
    private var indexCounter = 0

    /**
     * A green dragon that counts as spawned.
     *
     * `SingleCombat` treats a claim as stale unless its holder `isSpawned()`, which is `index > 0` -
     * one of the three conditions both of the implementations it replaced already checked, and a
     * real guard against an npc removed from the world still holding a target reference. A `Npc`
     * built directly has index -1, so without this every claim in this file would read as stale and
     * the tests would pass for the wrong reason.
     */
    private fun dragon(world: World): Npc =
        Npc(getRSCM("npc.green_dragon"), SINGLE, world).also {
            world.setNpcDefaults(it)
            it.index = ++indexCounter
        }

    private fun player(world: World): Player = Player(world).also { it.tile = SINGLE }

    /** What `Pawn.attack` does to the two attributes the rule reads. */
    private fun engage(
        npc: Npc,
        target: Player,
    ) {
        npc.attr[COMBAT_TARGET_FOCUS_ATTR] = WeakReference(target)
        target.attr[SINGLE_COMBAT_ATTACKER_ATTR] = WeakReference(npc)
    }

    @Test
    fun `a second monster will not join a fight in single-way combat`() {
        val world = world()
        val plugin = aggro(world)
        val first = dragon(world)
        val second = dragon(world)
        val target = player(world)

        assertTrue(
            plugin.singleCombatAllows(first, target),
            "the first dragon should be free to engage an unengaged player",
        )

        engage(first, target)

        assertFalse(
            plugin.singleCombatAllows(second, target),
            "a second dragon joined a fight the first had already claimed - single-way combat is not enforced",
        )
        assertTrue(
            plugin.singleCombatAllows(first, target),
            "the dragon that made the claim must still be allowed to keep attacking",
        )
    }

    /**
     * The claim is validated on read rather than cleared on every route out of combat - death,
     * leashing, logout and simply losing interest all drop the npc's combat target, and that is what
     * makes a stale claim fall away with it.
     */
    @Test
    fun `a claim left by a monster that has lost its target does not block anyone`() {
        val world = world()
        val plugin = aggro(world)
        val first = dragon(world)
        val second = dragon(world)
        val target = player(world)

        engage(first, target)
        assertFalse(
            plugin.singleCombatAllows(second, target),
            "precondition: the claim should hold while it is live",
        )

        // Whatever ended the first dragon's fight, it no longer has this player as its target.
        first.attr.remove(COMBAT_TARGET_FOCUS_ATTR)

        assertTrue(
            plugin.singleCombatAllows(second, target),
            "a stale claim from a monster that is no longer fighting must not lock the player out of combat forever",
        )
    }

    /**
     * The case the two copied implementations could not handle: a dark wizard and an aberrant
     * spectre each had their **own private** `ENGAGED_BY` key, so both could hold the same player at
     * once and neither saw the other. One attribute means one claim.
     */
    @Test
    fun `a claim taken through the attack loop blocks a different monster's attack loop`() {
        val world = world()
        val first = dragon(world)
        val second = dragon(world)
        val target = player(world)

        first.attr[COMBAT_TARGET_FOCUS_ATTR] = WeakReference(target)
        assertTrue(SingleCombat.claim(first, target), "the first caller should take a free claim")

        second.attr[COMBAT_TARGET_FOCUS_ATTR] = WeakReference(target)
        assertFalse(SingleCombat.claim(second, target), "a second caller took a claim somebody else held")
        assertTrue(SingleCombat.holds(first, target), "the original holder should still hold it")
        assertTrue(SingleCombat.claim(first, target), "the holder must be able to keep swinging")
    }

    /** Releasing frees the player on the same cycle rather than waiting for the claim to go stale. */
    @Test
    fun `releasing a claim lets the next monster take it`() {
        val world = world()
        val first = dragon(world)
        val second = dragon(world)
        val target = player(world)

        first.attr[COMBAT_TARGET_FOCUS_ATTR] = WeakReference(target)
        SingleCombat.claim(first, target)
        assertFalse(SingleCombat.claim(second, target), "precondition: the claim is held")

        SingleCombat.release(first, target)

        assertFalse(SingleCombat.holds(first, target), "release should drop the holder's own claim")
        second.attr[COMBAT_TARGET_FOCUS_ATTR] = WeakReference(target)
        assertTrue(SingleCombat.claim(second, target), "a released claim must be takeable")
    }

    /**
     * `release` is somebody else's business to call. A monster that never held the claim must not be
     * able to drop the holder's.
     */
    @Test
    fun `a monster cannot release a claim it does not hold`() {
        val world = world()
        val holder = dragon(world)
        val other = dragon(world)
        val target = player(world)

        holder.attr[COMBAT_TARGET_FOCUS_ATTR] = WeakReference(target)
        SingleCombat.claim(holder, target)

        SingleCombat.release(other, target)

        assertTrue(SingleCombat.holds(holder, target), "an unrelated monster dropped somebody else's claim")
    }

    /** A player already swinging at one monster should not be joined by its neighbour either. */
    @Test
    fun `a monster will not engage a player who is already fighting something else`() {
        val world = world()
        val plugin = aggro(world)
        val engaged = dragon(world)
        val bystander = dragon(world)
        val target = player(world)

        // No claim written - the player started this fight themselves.
        target.attr[COMBAT_TARGET_FOCUS_ATTR] = WeakReference(engaged)

        assertFalse(
            plugin.singleCombatAllows(bystander, target),
            "a bystander joined a player who was already in a fight of their own",
        )
        assertTrue(
            plugin.singleCombatAllows(engaged, target),
            "the monster the player is actually fighting must still be allowed to fight back",
        )
    }
}
