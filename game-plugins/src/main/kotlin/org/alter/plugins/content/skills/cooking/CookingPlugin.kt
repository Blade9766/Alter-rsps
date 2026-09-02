package org.alter.plugins.content.skills.cooking

import dev.openrune.cache.CacheManager
import org.alter.api.EquipmentType
import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.api.cfg.Sound
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.move.hasMoveDestination
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.queue.TaskPriority
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.rscm.RSCM.getRSCM

/**
 * Cooking: turning raw meat and fish into food (or charcoal) on a fire or a range.
 *
 * Recipes, level requirements, XP and stop-burning levels are the OSRS Wiki's, loaded
 * from `data/cfg/cooking/food.json` through [CookingService]. Both wiki tables were read
 * for this - the main Cooking article for level/XP, and `Cooking/Burn level` for the
 * per-source stop-burning levels, including the cooking-gauntlet column that only
 * lobster, swordfish, monkfish, shark and anglerfish appear in.
 *
 * **What is faithful:** which foods exist and what they turn into, their level and XP,
 * the exact level at which each stops burning on a fire, on a normal range, on the
 * Lumbridge Castle range and with gauntlets, the fact that sharks / sea turtles / dark
 * crabs / manta rays / anglerfish never stop burning on their own, that a Cooking cape
 * prevents every burn, and that burnt food is a distinct item per fish family.
 *
 * **What is approximated:** the burn *chance* below the stop-burning level. Jagex's real
 * curve isn't public, so [burnChance] interpolates from [BASE_BURN_CHANCE] at the food's
 * own level down to zero at its stop level, scaled by a per-heat-source multiplier - the
 * shape the wiki describes ("ranges burn food less often than fires at the same level",
 * the Castle range "slightly lower ... than both") rather than a reproduction of it. Same
 * treatment Woodcutting's chop chance and Fishing's catch chance already get here.
 *
 * **Heat sources** are discovered from the cache rather than listed per area: every
 * object carrying a real "Cook" action is wired, which covers ranges, ovens, stoves, clay
 * ovens and gnome cookers everywhere they appear in the map data. Fires are the six
 * objects a player can actually create with a tinderbox (see
 * [org.alter.plugins.content.skills.firemaking.FiremakingPlugin]); fires have no "Cook"
 * action in OSRS, so they are reached by using the raw food on them.
 *
 * **Bread, pies, stews and curries** are cooked here too, from the uncooked items that
 * [CookingRecipePlugin] assembles. Everything but stew and curry carries [FoodEntry
 * .rangeOnly], because in OSRS an open fire cannot bake them - the wiki leaves their Fire
 * column blank and their item pages list "Facilities: Cooking range".
 *
 * Pitta bread is a curiosity worth knowing about: it needs level 58 to cook but the burn
 * table stops it burning at 37, a level you can never be at while cooking it. Its entry
 * therefore stops burning at its own requirement, so in practice it never burns - which
 * is the real in-game behaviour, not a shortcut.
 *
 * **Deliberately not implemented:** pizzas, cakes, wine, kebabs and gnome cooking - the
 * remaining dough-and-bowl chains, which want their own pass. Also left out: raw giant
 * carp (the wiki's burn table gives its cooking XP as 0, which is almost certainly a
 * table artefact rather than a real figure, and its only source - Fishing Trawler - isn't
 * implemented), the Hosidius Kitchen's 5%/10% bonus, the Bake Pie spell, and raw
 * karambwan's "Cook it poorly" option that yields a poison karambwan. Karambwan cooks to
 * the real cooked item, which has been the game's default option since February 2024.
 *
 * One sourcing caveat worth recording: the wiki's `Cooking/Burn level` table resists
 * clean extraction, and the Castle-range figures for mud pie and curry could only be read
 * once rather than confirmed twice. Mud pie's Castle level is therefore set equal to its
 * range level (no bonus assumed) rather than guessed at.
 *
 * Deliberately does not call [org.alter.game.model.entity.Pawn.lock], for the reason
 * documented on `WoodcuttingPlugin`: a full lock stops `walkTo()` from processing a
 * movement click, stranding the player mid-skill.
 */
class CookingPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        loadService(CookingService())

        onWorldInit {
            val service = world.getService(CookingService::class.java) ?: return@onWorldInit

            CacheManager.getObjects().forEach { (id, def) ->
                val cook =
                    def.actions.filterNotNull().firstOrNull { it.equals("Cook", ignoreCase = true) } ?: return@forEach
                val source =
                    when {
                        id == LUMBRIDGE_CASTLE_RANGE -> HeatSource.CASTLE_RANGE
                        // One cache object named "Fire" carries a Cook action; it should
                        // burn food like the open fire it looks like, not like a range.
                        def.name.equals("Fire", ignoreCase = true) -> HeatSource.FIRE
                        else -> HeatSource.RANGE
                    }

                onObjOption(obj = id, option = cook) {
                    player.queue(TaskPriority.STANDARD) { cookMenu(this, player, service, source) }
                }
                bindUseOn(service, id, source)
            }

            FIRE_OBJECTS.forEach { bindUseOn(service, getRSCM(it), HeatSource.FIRE) }
        }
    }

    /**
     * Using a raw food on a heat source is how fires get cooked on at all, and on a range
     * it's the shortcut that skips past the "what would you like to cook?" list straight
     * to the chosen food - exactly as in OSRS.
     */
    private fun bindUseOn(
        service: CookingService,
        objId: Int,
        source: HeatSource,
    ) {
        service.entries.forEach { entry ->
            onItemOnObj(obj = objId, item = entry.rawItemId) {
                player.queue(TaskPriority.STANDARD) { cookAmountPrompt(this, player, entry, source) }
            }
        }
    }

    /** The range's "Cook" option: offer every raw food the player is carrying. */
    private suspend fun cookMenu(
        task: QueueTask,
        player: Player,
        service: CookingService,
        source: HeatSource,
    ) {
        // Lowest-level first, then capped: the skill-multi chatbox renders ten items and
        // no more, so a player carrying eleven kinds of raw food sees the ten they are
        // likeliest to be able to cook. Using the food directly on the range still
        // reaches anything the list left off.
        val carried =
            player.inventory
                .filterNotNull()
                .mapNotNull { service.lookup(it.id) }
                .filter { !it.rangeOnly || source != HeatSource.FIRE }
                .distinct()
                .sortedBy { it.level }
                .take(PRODUCE_BOX_CAPACITY)

        if (carried.isEmpty()) {
            player.message("You have nothing you can cook here.")
            return
        }
        if (carried.size == 1) {
            cookAmountPrompt(task, player, carried.single(), source)
            return
        }

        var chosen: FoodEntry? = null
        var requested = 0

        task.produceItemBox(
            player,
            *carried.map { it.cookedItemId }.toIntArray(),
            title = "What would you like to cook?",
            maxProducable = carried.maxOf { player.inventory.getItemCount(it.rawItemId) },
        ) { item, qty ->
            chosen = carried.firstOrNull { it.cookedItemId == item }
            requested = qty
        }

        val entry = chosen ?: return
        cook(task, player, entry, source, requested)
    }

    /** The single-food path: straight to "how many?" for the food that was clicked. */
    private suspend fun cookAmountPrompt(
        task: QueueTask,
        player: Player,
        entry: FoodEntry,
        source: HeatSource,
    ) {
        // Checked before the box opens rather than after: clicking a raw shark at level 1,
        // or a pie at a campfire, should say so straight away rather than after picking a
        // quantity.
        if (entry.rangeOnly && source == HeatSource.FIRE) {
            player.message("You need to cook this on a range.")
            return
        }
        if (player.getSkills().getCurrentLevel(Skills.COOKING) < entry.level) {
            player.message("You need a Cooking level of ${entry.level} to cook this.")
            return
        }

        var requested = 0
        task.produceItemBox(
            player,
            entry.cookedItemId,
            title = "What would you like to cook?",
            maxProducable = player.inventory.getItemCount(entry.rawItemId),
        ) { _, qty ->
            requested = qty
        }
        cook(task, player, entry, source, requested)
    }

    private suspend fun cook(
        task: QueueTask,
        player: Player,
        entry: FoodEntry,
        source: HeatSource,
        requested: Int,
    ) {
        if (requested <= 0) {
            return
        }

        if (entry.rangeOnly && source == HeatSource.FIRE) {
            player.message("You need to cook this on a range.")
            return
        }

        if (player.getSkills().getCurrentLevel(Skills.COOKING) < entry.level) {
            player.message("You need a Cooking level of ${entry.level} to cook this.")
            return
        }

        val animation = if (source == HeatSource.FIRE) Animation.COOKING_ON_FIRE else Animation.COOKING_ON_RANGE
        var made = 0

        while (made < requested && !player.hasMoveDestination()) {
            if (!player.inventory.contains(entry.rawItemId)) {
                break
            }

            player.animate(animation)
            player.playSound(Sound.COOKING)
            task.wait(COOK_CYCLE_TICKS)

            // Re-checked after the wait: the player may have dropped or banked the raw
            // food while the animation was playing.
            if (player.inventory.remove(item = entry.rawItemId, amount = 1).hasFailed()) {
                break
            }

            // Rolled against the level as it stands this cycle, so a level gained
            // mid-inventory immediately cuts the burn rate.
            val level = player.getSkills().getCurrentLevel(Skills.COOKING)
            if (player.world.randomDouble() <= burnChance(player, level, entry, source)) {
                player.inventory.add(item = entry.burntItemId, amount = 1)
                player.message("You accidentally burn the ${entry.name.lowercase()}.")
            } else {
                player.inventory.add(item = entry.cookedItemId, amount = 1)
                player.addXp(Skills.COOKING, entry.experience)
                // Bread and pies come out of an oven in OSRS rather than being "cooked".
                if (entry.rangeOnly) {
                    player.message("You remove the ${entry.name.lowercase()} from the oven.")
                } else {
                    player.message("You successfully cook the ${entry.name.lowercase()}.")
                }
            }

            made++
        }
    }

    /**
     * Chance of ruining this cook.
     *
     * Zero once the player is at or past the stop-burning level for the heat source they
     * are using, or wearing a Cooking cape, which the wiki states "guarantees food will
     * never burn while wearing it". Below that it interpolates linearly down from
     * [BASE_BURN_CHANCE] at the food's own level requirement, scaled by the source's
     * multiplier - so a range is kinder than a fire at every level, not only at the point
     * where burning stops. Foods with no stop level at all (`-1`: sharks and up) decay
     * towards [RESIDUAL_BURN_CHANCE] at 99 instead of reaching zero, which is why a level
     * 99 chef still ruins the occasional shark barehanded.
     */
    private fun burnChance(
        player: Player,
        level: Int,
        entry: FoodEntry,
        source: HeatSource,
    ): Double {
        if (player.hasEquipped(EquipmentType.CAPE, *COOKING_CAPES)) {
            return 0.0
        }

        val sourceLevel =
            when (source) {
                HeatSource.FIRE -> entry.fireLevel
                HeatSource.RANGE -> entry.rangeLevel
                HeatSource.CASTLE_RANGE -> entry.castleLevel
            }
        // -1 means "never stops burning", so it has to lose to any real level rather than
        // win a plain minOf().
        val stopLevel =
            if (!player.hasEquipped(EquipmentType.GLOVES, COOKING_GAUNTLETS)) {
                sourceLevel
            } else if (sourceLevel == -1 || entry.gauntletLevel == -1) {
                maxOf(sourceLevel, entry.gauntletLevel)
            } else {
                minOf(sourceLevel, entry.gauntletLevel)
            }

        val multiplier =
            when (source) {
                HeatSource.FIRE -> 1.0
                HeatSource.RANGE -> RANGE_BURN_MULTIPLIER
                HeatSource.CASTLE_RANGE -> CASTLE_RANGE_BURN_MULTIPLIER
            }

        if (stopLevel == -1) {
            val span = (MAX_LEVEL - entry.level).coerceAtLeast(1)
            val progress = ((level - entry.level).toDouble() / span).coerceIn(0.0, 1.0)
            return (BASE_BURN_CHANCE - (BASE_BURN_CHANCE - RESIDUAL_BURN_CHANCE) * progress) * multiplier
        }

        if (level >= stopLevel) {
            return 0.0
        }
        val span = (stopLevel - entry.level).coerceAtLeast(1)
        val progress = ((level - entry.level).toDouble() / span).coerceIn(0.0, 1.0)
        return BASE_BURN_CHANCE * (1.0 - progress) * multiplier
    }

    private companion object {
        const val MAX_LEVEL = 99

        /** `produceItemBox` renders at most ten items. */
        const val PRODUCE_BOX_CAPACITY = 10

        /**
         * Ticks per item. An inventory of 26 takes about 65 seconds in OSRS, which is
         * 2.4 seconds - four ticks - apiece.
         */
        const val COOK_CYCLE_TICKS = 4

        /** Burn chance at exactly a food's own level requirement, on an open fire. */
        const val BASE_BURN_CHANCE = 0.45

        /** Where foods that never stop burning end up at 99, rather than at zero. */
        const val RESIDUAL_BURN_CHANCE = 0.05

        const val RANGE_BURN_MULTIPLIER = 0.75
        const val CASTLE_RANGE_BURN_MULTIPLIER = 0.65

        /** The "Cook-o-matic 100" in Lumbridge Castle's kitchen. */
        const val LUMBRIDGE_CASTLE_RANGE = 114

        /**
         * The fires a player can make with a tinderbox - the regular one plus the five
         * coloured variants. Real fires carry no "Cook" action, so unlike ranges they
         * can't be found by scanning for one.
         */
        val FIRE_OBJECTS =
            listOf(
                "object.fire_26185",
                "object.fire_26186",
                "object.fire_26575",
                "object.fire_26576",
                "object.fire_20000",
                "object.fire_20001",
            )

        const val COOKING_GAUNTLETS = "item.cooking_gauntlets"

        /** Untrimmed and trimmed both count; the max cape doesn't grant the perk. */
        val COOKING_CAPES = arrayOf("item.cooking_cape", "item.cooking_capet")
    }
}
