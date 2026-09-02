package org.alter.plugins.content.skills.woodcutting

import dev.openrune.cache.CacheManager.getObject
import org.alter.api.Skills
import org.alter.api.cfg.Sound
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.DynamicObject
import org.alter.game.model.entity.GameObject
import org.alter.game.model.entity.Player
import org.alter.game.model.move.hasMoveDestination
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.queue.TaskPriority
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.rscm.RSCM.getRSCM

/**
 * Woodcutting: chop trees registered in [WoodcuttingService] for logs and experience.
 *
 * The exact chance-per-swing formula Jagex uses isn't public, so [chopChance] is an
 * approximation (better level relative to the tree's requirement, and a better axe,
 * both help) rather than a faithful reproduction. Everything else - level
 * requirements, xp rates, log items, the 1/8 deplete chance on non-regular trees, and
 * respawn times - is sourced from the OSRS Wiki.
 *
 * Deliberately does not call [org.alter.game.model.entity.Pawn.lock] around the chop
 * loop - that's a hard lock that blocks `walkTo()` from even processing a movement
 * click, which would leave a player stuck chopping with no way to walk away. Matches
 * how combat handles it: no lock at all, relying on the engine cancelling this queued
 * task the moment the player issues a new action.
 */
class WoodcuttingPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    /**
     * Axe tiers ordered worst to best, each with the Woodcutting level required to use
     * it and the chop animation it plays.
     */
    private val axes =
        listOf(
            Axe(getRSCM("item.bronze_axe"), 1, 879),
            Axe(getRSCM("item.iron_axe"), 1, 877),
            Axe(getRSCM("item.steel_axe"), 6, 875),
            Axe(getRSCM("item.black_axe"), 11, 873),
            Axe(getRSCM("item.mithril_axe"), 21, 871),
            Axe(getRSCM("item.adamant_axe"), 31, 869),
            Axe(getRSCM("item.rune_axe"), 41, 867),
            Axe(getRSCM("item.dragon_axe"), 61, 2846),
        )

    init {
        loadService(WoodcuttingService())

        onWorldInit {
            val service = world.getService(WoodcuttingService::class.java) ?: return@onWorldInit
            service.entries.forEach { entry ->
                entry.objectIds.forEach { objId ->
                    val chopOptions =
                        getObject(objId).actions.filterNotNull().filter { it.contains("chop", ignoreCase = true) }
                    chopOptions.forEach { option ->
                        onObjOption(obj = objId, option = option) {
                            val obj = player.getInteractingGameObj()
                            player.queue(TaskPriority.STANDARD) { chopTree(this, player, obj, entry) }
                        }
                    }
                }
            }
        }
    }

    private data class Axe(val item: Int, val level: Int, val animation: Int)

    private suspend fun chopTree(
        task: QueueTask,
        player: Player,
        obj: GameObject,
        entry: TreeEntry,
    ) {
        val level = player.getSkills().getCurrentLevel(Skills.WOODCUTTING)
        val treeName = obj.getDef().name ?: "tree"

        if (level < entry.level) {
            player.message("You need a Woodcutting level of ${entry.level} to chop this $treeName.")
            return
        }

        val axe = bestUsableAxe(player, level)
        if (axe == null) {
            player.message("You need an axe to chop down this $treeName.")
            return
        }

        if (player.inventory.isFull) {
            player.message("Your inventory is too full to hold any more logs.")
            return
        }

        player.faceTile(obj.tile)
        while (obj.isSpawned(world) && !player.inventory.isFull && !player.hasMoveDestination()) {
            player.animate(axe.animation)
            player.playSound(Sound.WOODCHOP)
            task.wait(4)

            if (!obj.isSpawned(world)) {
                break
            }

            if (player.world.randomDouble() <= chopChance(level, entry, axe)) {
                player.addXp(Skills.WOODCUTTING, entry.experience)
                player.inventory.add(item = entry.logItemId, amount = 1)
                player.message("You get some ${treeName.lowercase()} logs.")

                if (player.world.randomDouble() <= entry.depleteChance) {
                    player.playSound(Sound.TREE_FALL)
                    depleteTree(player.world, obj, entry)
                    break
                }
            }
        }
    }

    /**
     * The best axe the player can actually swing. Ranked by position in [axes] (worst
     * to best) rather than by level requirement, because bronze and iron share a
     * requirement of 1 - ranking by level made a player carrying both get bronze, and
     * with it bronze's slower chop rate.
     */
    private fun bestUsableAxe(
        player: Player,
        level: Int,
    ): Axe? =
        axes.lastOrNull {
            it.level <= level && (player.equipment.contains(it.item) || player.inventory.contains(it.item))
        }

    /**
     * Chance of getting a log on a single swing.
     *
     * Interpolates from the tree's [TreeEntry.baseChance] at exactly its own level
     * requirement up to [TreeEntry.maxChance] at 99, then adds a flat bonus per axe
     * tier. As the class doc says, Jagex's real per-swing formula isn't public, so
     * this is a deliberately calibrated approximation rather than a reproduction - the
     * shape (better level and better axe both help, rarer trees are slower) is right,
     * the exact numbers are tuned for playable rates and live in `trees.json` so they
     * can be adjusted without touching this file.
     *
     * The previous version started every tree at a flat 5% and crept up by 2% a level,
     * which meant a level 1 player with a bronze axe needed ~20 swings - about 48
     * seconds - for a single normal log.
     */
    private fun chopChance(
        level: Int,
        entry: TreeEntry,
        axe: Axe,
    ): Double {
        val levelsToMax = (MAX_LEVEL - entry.level).coerceAtLeast(1)
        val progress = ((level - entry.level).coerceAtLeast(0).toDouble() / levelsToMax).coerceIn(0.0, 1.0)
        val levelled = entry.baseChance + (entry.maxChance - entry.baseChance) * progress
        val axeBonus = axes.indexOf(axe) * AXE_TIER_BONUS
        return (levelled + axeBonus).coerceIn(entry.baseChance, MAX_CHOP_CHANCE)
    }

    private fun depleteTree(
        world: World,
        obj: GameObject,
        entry: TreeEntry,
    ) {
        val tile = obj.tile
        val type = obj.type
        val rot = obj.rot
        val originalId = obj.id

        world.remove(obj)

        val stump =
            if (entry.stumpObjectId != -1) {
                DynamicObject(id = entry.stumpObjectId, type = type, rot = rot, tile = tile).also { world.spawn(it) }
            } else {
                null
            }

        world.queue {
            wait(world.random(entry.respawnTicksMin..entry.respawnTicksMax))
            if (stump != null && world.isSpawned(stump)) {
                world.remove(stump)
            }
            world.spawn(DynamicObject(id = originalId, type = type, rot = rot, tile = tile))
        }
    }

    private companion object {
        const val MAX_LEVEL = 99

        /** Added per step up the [axes] list, so dragon is +7 steps' worth over bronze. */
        const val AXE_TIER_BONUS = 0.02

        /** Never a guaranteed log, so there's always some swing-to-swing variance. */
        const val MAX_CHOP_CHANCE = 0.95
    }
}
