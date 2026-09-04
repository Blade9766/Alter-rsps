package org.alter.plugins.content.areas.wilderness.bosses

import org.alter.api.ext.npc
import org.alter.game.Server
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.DropRoll
import org.alter.plugins.content.npcs.MonsterLoot
import org.alter.rscm.RSCM.getRSCM

/**
 * Spawns the Wilderness bosses, makes them aggressive, and rolls their loot.
 *
 * ## Why there is no `setCombatDef` here
 *
 * `data/cfg/npcs/monsterStats.json` already carries wiki-sourced combat stats for all ten of these
 * - Callisto's 1,000 hitpoints, Venenatis' 300 magic, Vet'ion's two forms as separate ids, and so
 * on - and `World.setNpcDefaults` only falls back to that table for npcs **no plugin declares a
 * combat def for**. Declaring one here to get an aggression radius would therefore have thrown all
 * of that away and required every stat to be re-typed by hand, where it could then drift from the
 * table the rest of the game uses.
 *
 * So the def is taken as given and copied with the three fields the table has no opinion about
 * ([Placement.respawnDelay], [Placement.aggroRadius] and a permanent aggression timer) layered on
 * at spawn. That works on respawn too: `NpcDeathAction` calls `setNpcDefaults` and then
 * `executeNpcSpawn` again, and per-npc spawn hooks run *before* the global ones, so
 * [org.alter.plugins.content.mechanics.aggro.NpcAggroPlugin] always reads the patched def rather
 * than the bare one.
 *
 * Attack, block and death animations are left to
 * [org.alter.plugins.content.npcs.animations.MonsterAnimationsPlugin], which resolves them from
 * the cache for exactly the monsters that carry no hand-written def - which is now these.
 */
class WildernessBossPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        WildernessBosses.PLACEMENTS.forEach { placement ->
            spawnNpc(placement.npc, placement.tile, walkRadius = placement.walkRadius)

            if (placement.multi) {
                multiChunksAround(placement.tile).forEach { setMultiCombatChunk(it) }
            }

            onNpcSpawn(placement.npc) {
                npc.combatDef =
                    npc.combatDef.copy(
                        respawnDelay = placement.respawnDelay,
                        aggressiveRadius = placement.aggroRadius,
                        aggroTargetDelay = AGGRO_SEARCH_DELAY,
                        // A boss never loses interest, and never checks the player's level first:
                        // MAX_VALUE is the short-circuit `defaultAggressiveness` reads as
                        // "always aggressive" before it gets to either test.
                        aggressiveTimer = Int.MAX_VALUE,
                    )
            }
        }

        WildernessBosses.DROPS.forEach { drops ->
            onNpcDeath(drops.npc) { rollLoot(npc, drops) }
        }
    }

    /**
     * The boss' own chunk and the eight around it - a 24x24 box, which comfortably covers the
     * ground any of these fights actually happens on without claiming half the Wilderness.
     */
    private fun multiChunksAround(tile: Tile): List<Int> {
        val chunks = ArrayList<Int>()
        for (dx in -1..1) {
            for (dz in -1..1) {
                chunks.add(Tile(tile.x + dx * CHUNK_SIZE, tile.z + dz * CHUNK_SIZE, tile.height).chunkCoords.hashCode())
            }
        }
        return chunks
    }

    /**
     * Guaranteed rows, then the independent unique rolls, then one row off the main table, then
     * the tertiaries.
     *
     * Uniques are rolled independently rather than as rows inside the main table because that is
     * how the wiki states them - a Callisto kill can produce a unique *and* a main-table row, and
     * modelling the unique as a competing row would have made those mutually exclusive.
     */
    private fun rollLoot(
        npc: Npc,
        drops: WildernessBosses.BossDrops,
    ) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val loot = ArrayList<Pair<Int, Int>>()

        drops.always.forEach { row ->
            row.item?.let { loot.add(it to DropRoll.amount(row, world)) }
        }

        drops.uniques.forEach { unique ->
            if (world.chance(1, unique.oneIn)) {
                loot.add(getRSCM(unique.item) to amountOf(unique))
            }
        }

        if (drops.main.isNotEmpty()) {
            DropRoll.pick(drops.main, world)?.let { picked ->
                picked.item?.let { loot.add(it to DropRoll.amount(picked, world)) }
            }
        }

        drops.tertiary.forEach { tertiary ->
            if (world.chance(1, tertiary.oneIn)) {
                loot.add(getRSCM(tertiary.item) to amountOf(tertiary))
            }
        }

        loot.forEach { (item, amount) ->
            MonsterLoot.drop(world, killer, item, amount, npc.tile)
        }
    }

    private fun amountOf(drop: WildernessBosses.RolledDrop): Int =
        if (drop.max <= drop.min) drop.min else drop.min + world.random(drop.max - drop.min)

    private companion object {
        const val CHUNK_SIZE = 8

        /** Cycles between aggression sweeps, matching the other monster packages. */
        const val AGGRO_SEARCH_DELAY = 4
    }
}
