package org.alter.plugins.content.skills.mining

import dev.openrune.cache.CacheManager.getObject
import org.alter.api.Skills
import org.alter.api.cfg.Animation
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
 * Mining: mine the rocks registered in [MiningService] for ore and experience.
 *
 * Built to mirror `content/skills/woodcutting` deliberately - JSON-configured entries, a
 * [org.alter.game.service.Service] that indexes them by object id, and a plugin that
 * binds off whatever the real cache action string is. Rocks are part of the base OSRS map
 * data, so this works everywhere those rocks already exist with no per-area spawning,
 * Barbarian Village's mine included.
 *
 * **Rock object ids and their ore are cache-verified, not guessed.** Every entry in
 * `rocks.json` came from dumping every object in this cache carrying a real "Mine"
 * action and grouping by name ("Iron rocks", "Coal rocks", ...); the resulting id sets
 * were then cross-checked against each rock's wiki infobox `id1`/`id2`/`id3` and matched
 * exactly. Note the cache's rock actions are `[Mine, hidden]` - only "Mine" is bound.
 *
 * **The depleted rock is derived, not configured.** OSRS ore rocks are a handful of
 * shared models (1388/1390/1391) recoloured per ore, and the grey depleted counterparts
 * are objects 8830/8828/8829 using those same three models with no actions - confirmed by
 * dumping the cache rather than assuming a single global "empty rock" id. So the correct
 * depleted object depends on the rock's *model*, not its ore, which is why it's resolved
 * per-rock in [depletedRockFor] instead of being a field in `rocks.json`. The newer
 * Prifddinas rock model (37841) has no depleted counterpart in this cache, so those rocks
 * simply vanish until they respawn, the same fallback Woodcutting uses for trees with no
 * configured stump.
 *
 * Sourced from the OSRS Wiki: level requirements, XP per ore, and the per-ore respawn
 * time (from each rock page's `Mining info` template `time =` field, converted at 0.6s
 * per tick - e.g. clay 1.2s = 2 ticks, coal 30s = 50 ticks, runite 12 min = 1200 ticks).
 * Pickaxe level requirements are the wiki's, and the swing animations are this codebase's
 * own `Animation.MINING_*_PICKAXE` constants.
 *
 * **Approximation, labelled:** [mineChance] is not Jagex's formula - the real per-swing
 * mining roll isn't public, exactly as with Woodcutting's `chopChance`. The shape is
 * right (a better level relative to the rock, and a better pickaxe, both help; rarer ores
 * are slower) and the numbers live in `rocks.json` so they can be retuned without
 * touching this file.
 *
 * Deliberately does not call [org.alter.game.model.entity.Pawn.lock] around the mining
 * loop, for the reason documented on `WoodcuttingPlugin`: a full lock stops `walkTo()`
 * from even processing a movement click, which would strand the player mid-skill.
 */
class MiningPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    /**
     * Pickaxe tiers ordered worst to best, each with the Mining level required to use it
     * and the swing animation it plays. Bronze and iron share a requirement of 1, so - as
     * in Woodcutting - the "best" pickaxe is chosen by position in this list rather than
     * by level, otherwise a player holding both would swing the slower bronze.
     *
     * Stops at dragon. Infernal, 3rd age and crystal pickaxes are all level 61+ tiers
     * that carry extra mechanics (auto-smelting, charges) which aren't built here, so
     * they're deliberately left out rather than wired up as plain reskins.
     */
    private val pickaxes =
        listOf(
            Pickaxe(getRSCM("item.bronze_pickaxe"), 1, Animation.MINING_BRONZE_PICKAXE),
            Pickaxe(getRSCM("item.iron_pickaxe"), 1, Animation.MINING_IRON_PICKAXE),
            Pickaxe(getRSCM("item.steel_pickaxe"), 6, Animation.MINING_STEEL_PICKAXE),
            Pickaxe(getRSCM("item.black_pickaxe"), 11, Animation.MINING_BLACK_PICKAXE),
            Pickaxe(getRSCM("item.mithril_pickaxe"), 21, Animation.MINING_MITHRIL_PICKAXE),
            Pickaxe(getRSCM("item.adamant_pickaxe"), 31, Animation.MINING_ADAMANT_PICKAXE),
            Pickaxe(getRSCM("item.rune_pickaxe"), 41, Animation.MINING_RUNE_PICKAXE),
            Pickaxe(getRSCM("item.dragon_pickaxe"), 61, Animation.MINING_DRAGON_PICKAXE),
        )

    init {
        loadService(MiningService())

        onWorldInit {
            val service = world.getService(MiningService::class.java) ?: return@onWorldInit
            service.entries.forEach { entry ->
                entry.objectIds.forEach { objId ->
                    val mineOptions =
                        getObject(objId).actions.filterNotNull().filter { it.equals("Mine", ignoreCase = true) }
                    mineOptions.forEach { option ->
                        onObjOption(obj = objId, option = option) {
                            val obj = player.getInteractingGameObj()
                            player.queue(TaskPriority.STANDARD) { mineRock(this, player, obj, entry) }
                        }
                    }
                }
            }
        }
    }

    private data class Pickaxe(val item: Int, val level: Int, val animation: Int)

    private suspend fun mineRock(
        task: QueueTask,
        player: Player,
        obj: GameObject,
        entry: RockEntry,
    ) {
        val level = player.getSkills().getCurrentLevel(Skills.MINING)

        if (level < entry.level) {
            player.message("You need a Mining level of ${entry.level} to mine this rock.")
            return
        }

        val pickaxe = bestUsablePickaxe(player, level)
        if (pickaxe == null) {
            player.message("You need a pickaxe to mine this rock, and one you have the Mining level to use.")
            return
        }

        if (player.inventory.isFull) {
            player.message("Your inventory is too full to hold any more ore.")
            return
        }

        player.faceTile(obj.tile)
        player.message("You swing your pick at the rock.")

        while (obj.isSpawned(world) && !player.inventory.isFull && !player.hasMoveDestination()) {
            player.animate(pickaxe.animation)
            player.playSound(Sound.MINE_ORE)
            task.wait(4)

            if (!obj.isSpawned(world)) {
                break
            }

            if (player.world.randomDouble() <= mineChance(level, entry, pickaxe)) {
                player.addXp(Skills.MINING, entry.experience)
                player.inventory.add(item = entry.oreItemId, amount = 1)
                player.message("You manage to mine some ${entry.name}.")

                // Every one of these ore rocks yields a single ore and then depletes -
                // clay and copper included, they just come back within a few ticks.
                depleteRock(player.world, obj, entry)
                break
            }
        }
    }

    /** The best pickaxe the player can actually swing, held or wielded. */
    private fun bestUsablePickaxe(
        player: Player,
        level: Int,
    ): Pickaxe? =
        pickaxes.lastOrNull {
            it.level <= level && (player.equipment.contains(it.item) || player.inventory.contains(it.item))
        }

    /**
     * Chance of getting an ore on a single swing.
     *
     * Interpolates from the rock's [RockEntry.baseChance] at exactly its own level
     * requirement up to [RockEntry.maxChance] at 99, then adds a flat bonus per pickaxe
     * tier. As the class doc says, this is a calibrated approximation of a formula Jagex
     * has never published, not a reproduction of it.
     */
    private fun mineChance(
        level: Int,
        entry: RockEntry,
        pickaxe: Pickaxe,
    ): Double {
        val levelsToMax = (MAX_LEVEL - entry.level).coerceAtLeast(1)
        val progress = ((level - entry.level).coerceAtLeast(0).toDouble() / levelsToMax).coerceIn(0.0, 1.0)
        val levelled = entry.baseChance + (entry.maxChance - entry.baseChance) * progress
        val pickaxeBonus = pickaxes.indexOf(pickaxe) * PICKAXE_TIER_BONUS
        return (levelled + pickaxeBonus).coerceIn(entry.baseChance, MAX_MINE_CHANCE)
    }

    private fun depleteRock(
        world: World,
        obj: GameObject,
        entry: RockEntry,
    ) {
        val tile = obj.tile
        val type = obj.type
        val rot = obj.rot
        val originalId = obj.id

        world.remove(obj)

        val depletedId = depletedRockFor(originalId)
        val depleted =
            if (depletedId != -1) {
                DynamicObject(id = depletedId, type = type, rot = rot, tile = tile).also { world.spawn(it) }
            } else {
                null
            }

        world.queue {
            wait(entry.respawnTicks)
            if (depleted != null && world.isSpawned(depleted)) {
                world.remove(depleted)
            }
            world.spawn(DynamicObject(id = originalId, type = type, rot = rot, tile = tile))
        }
    }

    /**
     * The grey depleted-rock object matching [rockId]'s own model, or -1 if this cache has
     * none for that model.
     *
     * Ore rocks are the same few rock models recoloured per ore, and each of those models
     * has an actionless grey twin - so the depleted object is a property of the rock's
     * shape, not of the ore it holds. Looked up through the cache so a rock added to
     * `rocks.json` later gets the right depleted object with no extra configuration.
     */
    private fun depletedRockFor(rockId: Int): Int {
        val model = getObject(rockId).objectModels?.firstOrNull() ?: return -1
        return DEPLETED_ROCK_BY_MODEL[model] ?: -1
    }

    private companion object {
        const val MAX_LEVEL = 99

        /** Added per step up the [pickaxes] list, so dragon is +7 steps' worth over bronze. */
        const val PICKAXE_TIER_BONUS = 0.02

        /** Never a guaranteed ore, so there's always some swing-to-swing variance. */
        const val MAX_MINE_CHANCE = 0.95

        /**
         * Rock model -> its actionless grey "Rocks" counterpart, both taken from this
         * cache. 1388/1390/1391 are the three shared ore-rock models.
         */
        val DEPLETED_ROCK_BY_MODEL: Map<Int, Int> =
            mapOf(
                1388 to getRSCM("object.rocks_8830"),
                1390 to getRSCM("object.rocks_8828"),
                1391 to getRSCM("object.rocks_8829"),
            )
    }
}
