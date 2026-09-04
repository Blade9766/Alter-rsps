package org.alter.plugins.content.items.jewellery

import dev.openrune.cache.CacheManager.getItem
import org.alter.api.ext.message
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.fs.ObjectExamineHolder
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.Plugin
import org.alter.game.plugin.PluginRepository
import org.alter.rscm.RSCM.getRSCM

/**
 * The ring of wealth's currency-collection toggle.
 *
 * The two effects themselves are not bound to any option - the rare drop enhancement lives in the
 * shared drop tables and the collection in `MonsterLoot` - so all this plugin does is let the player
 * turn the collection off, which the wiki says is done "by right-clicking the ring".
 *
 * Two cache options do that, and both are bound because the ring carries different ones in different
 * places: "Features" is on every ring in the inventory, while "Coin Collection" is a *worn* option
 * that only the charged rings have. Real OSRS opens a small menu from "Features"; there is no
 * interface for that here, so it toggles directly - the one thing that menu is for.
 *
 * "Boss Log", the other worn option on a charged ring, is deliberately left unbound: it opens a kill
 * log this project has no interface for, and a placeholder would only pretend otherwise.
 */
class RingOfWealthPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        RingOfWealth.ringKeys.forEach { key ->
            val id = getRSCM(key)

            if (getItem(id).interfaceOptions.any { it?.equals(FEATURES, ignoreCase = true) == true }) {
                onItemOption(key, option = FEATURES) { toggle() }
            }

            // Only the charged rings carry this one; the uncharged ones show "Rub" in its place.
            val worn = ObjectExamineHolder.EQUIPMENT_MENU.get(id)?.equipmentMenu.orEmpty()
            if (worn.any { it?.equals(COIN_COLLECTION, ignoreCase = true) == true }) {
                onEquipmentOption(key, option = COIN_COLLECTION) { toggle() }
            }
        }
    }

    private fun Plugin.toggle() {
        val disabled = player.attr[RingOfWealth.COLLECTION_DISABLED_ATTR] != true
        player.attr[RingOfWealth.COLLECTION_DISABLED_ATTR] = disabled
        if (disabled) {
            player.message("Your ring of wealth will no longer collect coins for you.")
        } else {
            player.message("Your ring of wealth will now collect coins for you.")
        }
    }

    private companion object {
        private const val FEATURES = "Features"
        private const val COIN_COLLECTION = "Coin Collection"
    }
}
