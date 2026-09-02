package org.alter.plugins.content.skills.smithing

import dev.openrune.cache.CacheManager
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
 * Smithing, half two: hammering bars into equipment at an anvil.
 *
 * Every product's level requirement, bar cost and XP is the OSRS Wiki's, loaded from
 * `data/cfg/smithing/products.json` through [SmithingService]. XP is the metal's per-bar
 * rate times the item's bar count - see [MetalEntry.experiencePerBar] for the two
 * independent cross-checks behind those rates. Note the wiki's own tables are not a clean
 * formula: bronze's levels run one below the other metals from the axe onward, and rune
 * compresses at the top (2h sword, platelegs, plateskirt and platebody are all level 99
 * rather than 99/101/101/103), so the levels are transcribed per metal rather than
 * derived from an offset.
 *
 * **Known deviation from OSRS, deliberate:** the real game shows one interface-312 grid
 * per bar. This codebase's cache library has no interface or component decoder, so 312's
 * component ids can't be verified from this cache, and guessing them would be exactly the
 * kind of unverified assumption the rest of this content avoids. Instead the flow is:
 * pick the metal (only asked when carrying more than one kind of bar), pick Weapons or
 * Armour, then the real `produceItemBox` skill-multi chatbox - which holds ten items, so
 * the 10-weapon and 8-armour halves each fit in one box. The item data underneath is
 * unaffected.
 *
 * Anvils are found by scanning the cache for objects with a real "Smith" action whose
 * name mentions an anvil, so every anvil in the map data works with no per-area
 * configuration, Barbarian Village's included.
 *
 * **Not implemented:** the ammo and utility products (dart tips, arrowtips, bolts, knives,
 * javelin heads, nails, wire, limbs, keel parts, studs, oil lantern frame, iron spit),
 * which produce stacks at per-item rates rather than single equipment pieces; cannonballs,
 * which need an ammo mould and Dwarf Cannon; spears and hastae, which are Barbarian
 * smithing on a specific anvil at double XP; and the whole Gold/Silver bar path, which in
 * OSRS feeds Crafting rather than an anvil.
 */
class AnvilPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onWorldInit {
            val service = world.getService(SmithingService::class.java) ?: return@onWorldInit

            CacheManager.getObjects().forEach { (id, def) ->
                val name = def.name ?: return@forEach
                if (!name.contains("anvil", ignoreCase = true)) return@forEach
                val smith = def.actions.filterNotNull().firstOrNull { it.equals("Smith", ignoreCase = true) } ?: return@forEach

                onObjOption(obj = id, option = smith) {
                    player.queue(TaskPriority.STANDARD) { anvilMenu(this, player, service) }
                }
            }
        }
    }

    private suspend fun anvilMenu(
        task: QueueTask,
        player: Player,
        service: SmithingService,
    ) {
        if (!player.inventory.contains(HAMMER) && !player.equipment.contains(HAMMER)) {
            player.message("You need a hammer to work the metal with.")
            return
        }

        val carried = service.metals.filter { player.inventory.contains(it.barItemId) }
        if (carried.isEmpty()) {
            player.message("You have no bars to work with.")
            return
        }

        val metal =
            if (carried.size == 1) {
                carried.single()
            } else {
                val choice = task.options(player, *carried.map { "${it.name} bar" }.toTypedArray(), title = "Which bar?")
                carried.getOrNull(choice - 1) ?: return
            }

        val category =
            when (task.options(player, SmithCategory.WEAPON.displayName, SmithCategory.ARMOUR.displayName)) {
                1 -> SmithCategory.WEAPON
                2 -> SmithCategory.ARMOUR
                else -> return
            }

        val products = metal.products.filter { it.category == category }
        if (products.isEmpty()) {
            return
        }

        val barsHeld = player.inventory.getItemCount(metal.barItemId)
        var chosen: SmithableEntry? = null
        var requested = 0

        task.produceItemBox(
            player,
            *products.map { it.itemId }.toIntArray(),
            title = "What would you like to make?",
            maxProducable = barsHeld.coerceAtLeast(1),
        ) { item, qty ->
            chosen = products.firstOrNull { it.itemId == item }
            requested = qty
        }

        val product = chosen ?: return
        if (requested <= 0) {
            return
        }
        smith(task, player, metal, product, requested)
    }

    private suspend fun smith(
        task: QueueTask,
        player: Player,
        metal: MetalEntry,
        product: SmithableEntry,
        requested: Int,
    ) {
        val productName = CacheManager.getItem(product.itemId)?.name ?: "item"

        if (player.getSkills().getCurrentLevel(Skills.SMITHING) < product.level) {
            player.message("You need a Smithing level of ${product.level} to make a ${productName.lowercase()}.")
            return
        }

        val experience = metal.experiencePerBar * product.bars
        var made = 0

        while (made < requested && !player.hasMoveDestination()) {
            if (player.inventory.getItemCount(metal.barItemId) < product.bars) {
                player.message("You need ${product.bars} ${metal.name.lowercase()} bars to make a ${productName.lowercase()}.")
                break
            }
            if (player.inventory.isFull && !player.inventory.contains(product.itemId)) {
                player.message("Your inventory is too full to hold any more.")
                break
            }

            player.animate(Animation.SMITHING_ANVIL)
            player.playSound(Sound.ANVIL)
            task.wait(SMITH_CYCLE_TICKS)

            // Re-checked after the wait, same as smelting: the bars may be gone by now.
            if (player.inventory.getItemCount(metal.barItemId) < product.bars) {
                break
            }

            player.inventory.remove(item = metal.barItemId, amount = product.bars)
            player.inventory.add(item = product.itemId, amount = 1)
            player.addXp(Skills.SMITHING, experience)
            player.message("You hammer the ${metal.name.lowercase()} and make a ${productName.lowercase()}.")

            made++
        }
    }

    private companion object {
        val HAMMER = getRSCM("item.hammer")

        /** Ticks per item, matching the anvil animation's length in OSRS. */
        const val SMITH_CYCLE_TICKS = 5
    }
}
