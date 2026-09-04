package org.alter.tools

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import dev.openrune.cache.CacheManager
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Paths
import java.time.Instant

/**
 * Bakes the Grand Exchange guide prices into `data/cfg/grandexchange/prices.json`.
 *
 * The OSRS wiki runs the community price API that the in-game GE guide price is modelled on
 * (https://prices.runescape.wiki/api/v1/osrs). We take the 24-hour volume weighted average where
 * one exists - that is the closest public analogue of Jagex's guide price - fall back to the
 * midpoint of the latest instant-buy/instant-sell pair, and finally to the item's own cache value.
 *
 * Only items the cache marks tradeable get an entry, so the file doubles as the "can this be
 * offered on the GE" set for [org.alter.plugins.content.mechanics.grandexchange.GrandExchangeMarket].
 *
 * Usage: gradlew :game-server:gePriceDump
 *
 * Re-run it whenever you want fresher prices; the file it writes is meant to be committed.
 */
object GrandExchangePriceDump {

    private const val API = "https://prices.runescape.wiki/api/v1/osrs"

    /**
     * The wiki asks every automated caller to identify itself and to say what the traffic is for;
     * an anonymous or browser-spoofing agent gets rate limited.
     */
    private const val USER_AGENT = "Alter-RSPS guide price bake - github.com/AlterRSPS/Alter"

    @JvmStatic
    fun main(args: Array<String>) {
        CacheManager.init(Paths.get("data/cache"), 228)

        val mapping = fetch("$API/mapping").asJsonArray
        val latest = fetch("$API/latest").asJsonObject.getAsJsonObject("data")
        val daily = fetch("$API/24h").asJsonObject.getAsJsonObject("data")
        println("Fetched ${mapping.size()} mapped items, ${latest.size()} latest, ${daily.size()} 24h averages.")

        // The wiki tracks the live game, which runs ahead of our cache revision; anything it lists
        // that this cache has never heard of is dropped rather than baked in as a dead entry.
        val prices = sortedMapOf<Int, Int>()
        var fromDaily = 0
        var fromLatest = 0
        var fromCache = 0
        var skipped = 0

        for (element in mapping) {
            val entry = element.asJsonObject
            val id = entry.get("id").asInt
            val def = itemOrNull(id)
            if (def == null || !isTradeable(id)) {
                skipped++
                continue
            }

            val key = id.toString()
            val average = daily.getAsJsonObject(key)?.let { average(it, "avgHighPrice", "avgLowPrice") }
            val instant = latest.getAsJsonObject(key)?.let { average(it, "high", "low") }
            val price = when {
                average != null && average > 0 -> { fromDaily++; average }
                instant != null && instant > 0 -> { fromLatest++; instant }
                else -> { fromCache++; def.cost.coerceAtLeast(1) }
            }
            prices[id] = price
        }

        // Tradeable items the wiki has no row for at all - untraded junk, quest-locked drops - still
        // need a price or they cannot be offered; the cache value is the only source left.
        for (id in 0 until CacheManager.itemSize()) {
            if (prices.containsKey(id) || !isTradeable(id)) {
                continue
            }
            val def = itemOrNull(id) ?: continue
            prices[id] = def.cost.coerceAtLeast(1)
            fromCache++
        }

        val output = File("data/cfg/grandexchange/prices.json")
        output.parentFile.mkdirs()
        val gson = GsonBuilder().setPrettyPrinting().create()
        val document =
            linkedMapOf<String, Any>(
                "source" to "prices.runescape.wiki 24h average, latest midpoint, then cache value",
                "generated" to Instant.now().toString(),
                "prices" to prices.mapKeys { it.key.toString() },
            )
        output.writeText(gson.toJson(document))

        println("Wrote ${prices.size} prices to ${output.path}")
        println("  24h average: $fromDaily, latest midpoint: $fromLatest, cache value: $fromCache, skipped: $skipped")
    }

    /**
     * A cache item is offerable when it is tradeable in its own right. Noted items carry the same
     * `isTradeable` flag as the item they stand for, so they are excluded by only accepting objects
     * that are their own unnoted form - the GE always deals in unnoted ids.
     */
    private fun isTradeable(id: Int): Boolean {
        val def = itemOrNull(id) ?: return false
        if (!def.isTradeable) return false
        if (def.noted || def.isPlaceholder) return false
        return def.name.isNotBlank() && !def.name.equals("null", ignoreCase = true)
    }

    /**
     * [CacheManager.getItem] throws for anything the cache does not hold, and the live wiki listing
     * runs ahead of this cache revision, so every lookup here goes through a null-returning wrapper.
     */
    private fun itemOrNull(id: Int): dev.openrune.cache.filestore.definition.data.ItemType? =
        runCatching { CacheManager.getItem(id) }.getOrNull()

    private fun average(
        json: com.google.gson.JsonObject,
        highKey: String,
        lowKey: String,
    ): Int? {
        val high = json.get(highKey)?.takeIf { !it.isJsonNull }?.asInt
        val low = json.get(lowKey)?.takeIf { !it.isJsonNull }?.asInt
        return when {
            high != null && low != null -> (high + low) / 2
            high != null -> high
            low != null -> low
            else -> null
        }
    }

    private fun fetch(url: String): com.google.gson.JsonElement {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.connectTimeout = 30_000
        connection.readTimeout = 60_000
        connection.inputStream.bufferedReader().use {
            return JsonParser.parseReader(it)
        }
    }
}
