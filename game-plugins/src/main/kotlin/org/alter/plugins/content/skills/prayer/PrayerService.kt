package org.alter.plugins.content.skills.prayer

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.openrune.cache.CacheManager.getItem
import gg.rsmod.util.ServerProperties
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.service.Service
import org.alter.rscm.RSCM.getRSCM
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Loads `data/cfg/prayer/offerings.json` - every bone and ash that pays Prayer experience -
 * and pairs each one with the cache option the client actually shows on it.
 *
 * Validation is deliberately loud, for the reason documented on
 * [org.alter.plugins.content.skills.herblore.HerbloreService]: a plugin whose constructor
 * throws registers *nothing at all*, so a row naming an item that has neither "Bury" nor
 * "Scatter" would otherwise take the whole skill offline without saying why. Here it fails
 * with the offending row named.
 */
class PrayerService : Service {
    private val gson = Gson()

    val offerings: ObjectArrayList<OfferingEntry> = ObjectArrayList()

    private val byItem: Int2ObjectOpenHashMap<OfferingEntry> = Int2ObjectOpenHashMap()

    /** Every entry that is a bone, i.e. the ones an altar will take. Ashes are not offerable. */
    val bones: List<OfferingEntry>
        get() = offerings.filter { it.action == OfferingAction.BURY }

    fun forItem(item: Int): OfferingEntry? = byItem[item]

    override fun init(
        server: Server,
        world: World,
        serviceProperties: ServerProperties,
    ) {
        val file = Paths.get(serviceProperties.get("prayer_offerings") ?: "../data/cfg/prayer/offerings.json")

        Files.newBufferedReader(file).use { reader ->
            val listType = object : TypeToken<List<OfferingEntry>>() {}.type
            offerings.addAll(gson.fromJson<List<OfferingEntry>>(reader, listType))
        }

        offerings.forEach { entry ->
            check(entry.experience > 0.0) { "Offering '${entry.name}' pays no experience." }
            check(entry.levelRequired in 1..99) { "Offering '${entry.name}' has an invalid level: ${entry.levelRequired}." }

            entry.itemId = getRSCM(entry.item)

            // The cache decides which of the two actions this is. An item carrying both -
            // or neither - is a config mistake worth stopping for, since binding the wrong
            // option index would leave the item's real option silently dead.
            val options = getItem(entry.itemId).interfaceOptions.filterNotNull().filter { it.isNotBlank() }
            val actions = OfferingAction.values.filter { action -> options.any { it.equals(action.option, ignoreCase = true) } }
            check(actions.size == 1) {
                "Offering '${entry.name}' should carry exactly one of ${OfferingAction.values.map { it.option }} " +
                    "but the cache gives it $options."
            }
            entry.action = actions.single()

            check(byItem.put(entry.itemId, entry) == null) {
                "Two offering entries share the item '${entry.item}'."
            }
        }
    }
}
