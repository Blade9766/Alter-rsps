package org.alter.plugins.content.items.jewellery

import org.alter.api.ext.getInteractingItemSlot
import org.alter.api.ext.message
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.fs.ObjectExamineHolder
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.Plugin
import org.alter.game.plugin.PluginRepository
import org.alter.rscm.RSCM.getRSCM
import dev.openrune.cache.CacheManager.getItem

/**
 * The "Check" and "Break" options every piece of [PerkJewellery] carries. The perks themselves are
 * applied by the skills, through [JewelleryPerks].
 *
 * Both options are the cache's own, and which of them a given piece has varies - the bracelet of
 * clay has "Check" only while worn and no "Break" at all, the dodgy necklace has "Check" in both
 * places, and the amulet of chemistry has neither (its worn option is "Options", a configuration
 * screen for whether to keep brewing once the amulet crumbles, which this project has no interface
 * for). So each binding is made only where the option exists, with `JewelleryVerify` asserting the
 * set rather than a constructor throw taking the whole plugin down.
 */
class PerkJewelleryPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        PerkJewellery.values.forEach { perk ->
            val id = getRSCM(perk.item)

            if (hasWornOption(id, CHECK)) {
                onEquipmentOption(perk.item, option = CHECK) { check(perk) }
            }
            if (hasInventoryOption(id, CHECK)) {
                onItemOption(perk.item, option = CHECK) { check(perk) }
            }

            /*
             * "Break" destroys the piece and refills the pool, which is the only way to guarantee
             * the next one starts at full - dropping or alching one does not, because the charges
             * were never on the item in the first place.
             */
            if (hasInventoryOption(id, BREAK)) {
                onItemOption(perk.item, option = BREAK) {
                    val slot = player.getInteractingItemSlot()
                    if (player.inventory[slot]?.id != id) {
                        return@onItemOption
                    }
                    player.inventory[slot] = null
                    JewelleryPerks.reset(player, perk)
                    player.message("You break the ${perk.displayName}. The next one you use will be as good as new.")
                }
            }
        }
    }

    private fun Plugin.check(perk: PerkJewellery) {
        val left = JewelleryPerks.remaining(player, perk)
        player.message("Your ${perk.displayName} has $left ${perk.chargeNoun} left.")
    }

    private fun hasWornOption(
        id: Int,
        option: String,
    ): Boolean =
        ObjectExamineHolder.EQUIPMENT_MENU.get(id)?.equipmentMenu
            ?.any { it?.equals(option, ignoreCase = true) == true } == true

    private fun hasInventoryOption(
        id: Int,
        option: String,
    ): Boolean = getItem(id).interfaceOptions.any { it?.equals(option, ignoreCase = true) == true }

    private companion object {
        private const val CHECK = "Check"
        private const val BREAK = "Break"
    }
}
