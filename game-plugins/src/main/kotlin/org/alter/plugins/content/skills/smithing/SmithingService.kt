package org.alter.plugins.content.skills.smithing

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import gg.rsmod.util.ServerProperties
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import org.alter.api.ext.appendToString
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.service.Service
import org.alter.rscm.RSCM.getRSCM
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Loads the smelting recipes and anvil product tables so they can be retrieved at runtime.
 */
class SmithingService : Service {
    private val gson = Gson()

    val bars: ObjectArrayList<BarEntry> = ObjectArrayList()

    val metals: ObjectArrayList<MetalEntry> = ObjectArrayList()

    private val metalsByBar: Int2ObjectOpenHashMap<MetalEntry> = Int2ObjectOpenHashMap()

    override fun init(server: Server, world: World, serviceProperties: ServerProperties) {
        val barsFile = Paths.get(serviceProperties.get("smithing_bars") ?: "../data/cfg/smithing/bars.json")
        val productsFile = Paths.get(serviceProperties.get("smithing_products") ?: "../data/cfg/smithing/products.json")

        Files.newBufferedReader(barsFile).use { reader ->
            val listType = object : TypeToken<List<BarEntry>>() {}.type
            bars.addAll(gson.fromJson<List<BarEntry>>(reader, listType))
        }

        Files.newBufferedReader(productsFile).use { reader ->
            val listType = object : TypeToken<List<MetalEntry>>() {}.type
            metals.addAll(gson.fromJson<List<MetalEntry>>(reader, listType))
        }

        bars.forEach { entry ->
            // Gson allocates without running the constructor, so neither a data class'
            // `init` block nor its Kotlin default values ever apply to these entries - a
            // field left out of the JSON silently arrives as 0/0.0. That already bit this
            // config once: every bar but iron had no `successChance` and so defaulted to
            // 0.0 rather than 1.0, which would have made them impossible to smelt. Hence
            // validating here, in code that actually runs, rather than trusting defaults.
            check(entry.level in 1..99) { "Bar '${entry.name}' has an invalid level: ${entry.level}." }
            check(entry.experience > 0.0) { "Bar '${entry.name}' has no experience set." }
            check(entry.successChance > 0.0 && entry.successChance <= 1.0) {
                "Bar '${entry.name}' has successChance ${entry.successChance}; it must be set explicitly in bars.json."
            }
            check(entry.ingredients.isNotEmpty()) { "Bar '${entry.name}' has no ingredients." }

            entry.barItemId = getRSCM(entry.bar)
            entry.ingredients.forEach {
                check(it.amount >= 1) { "Bar '${entry.name}' ingredient '${it.item}' has amount ${it.amount}." }
                it.itemId = getRSCM(it.item)
            }
        }

        metals.forEach { metal ->
            check(metal.experiencePerBar > 0.0) { "Metal '${metal.name}' has no experiencePerBar set." }
            check(metal.products.isNotEmpty()) { "Metal '${metal.name}' has no products." }

            metal.barItemId = getRSCM(metal.bar)
            metal.products.forEach {
                check(it.level in 1..99) { "Product '${it.item}' has an invalid level: ${it.level}." }
                check(it.bars >= 1) { "Product '${it.item}' needs at least one bar." }
                it.itemId = getRSCM(it.item)
            }
            metalsByBar.put(metal.barItemId, metal)
        }

        Server.logger.info {
            "Loaded ${bars.size.appendToString("smelting recipe")} and " +
                "${metals.sumOf { it.products.size }.appendToString("smithable item")}."
        }
    }

    fun metalForBar(barItemId: Int): MetalEntry? = metalsByBar[barItemId]
}
