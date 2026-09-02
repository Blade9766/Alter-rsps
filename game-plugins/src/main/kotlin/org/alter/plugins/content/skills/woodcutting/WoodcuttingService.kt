package org.alter.plugins.content.skills.woodcutting

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
 * Loads the woodcutting configuration so that tree entries can be retrieved at runtime.
 */
class WoodcuttingService : Service {
    private val gson = Gson()

    val entries: ObjectArrayList<TreeEntry> = ObjectArrayList()

    private val entriesByObject: Int2ObjectOpenHashMap<TreeEntry> = Int2ObjectOpenHashMap()

    override fun init(server: Server, world: World, serviceProperties: ServerProperties) {
        val file = Paths.get(serviceProperties.get("trees") ?: "../data/cfg/woodcutting/trees.json")

        Files.newBufferedReader(file).use { reader ->
            val listType = object : TypeToken<List<TreeEntry>>() {}.type
            val loaded: List<TreeEntry> = gson.fromJson(reader, listType)
            entries.addAll(loaded)
        }

        entries.forEach { entry ->
            entry.objectIds = entry.objects.map { getRSCM(it) }.toIntArray()
            entry.stumpObjectId = entry.stumpObject?.let { getRSCM(it) } ?: -1
            entry.logItemId = getRSCM(entry.log)
            entry.objectIds.forEach { id ->
                entriesByObject.put(id, entry)
            }
        }

        Server.logger.info { "Loaded ${entries.size.appendToString("woodcutting tree definition")}." }
    }

    fun lookup(objectId: Int): TreeEntry? = entriesByObject[objectId]
}
