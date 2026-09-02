package org.alter.plugins.content.skills.agility

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
 * Loads the agility course configuration so that obstacles can be retrieved at runtime.
 */
class AgilityService : Service {
    private val gson = Gson()

    val courses: ObjectArrayList<CourseEntry> = ObjectArrayList()

    /**
     * Every obstacle keyed by the object ids that trigger it, alongside the course it belongs to and
     * its position in that course's lap order.
     */
    private val obstaclesByObject: Int2ObjectOpenHashMap<ObstacleLocation> = Int2ObjectOpenHashMap()

    override fun init(server: Server, world: World, serviceProperties: ServerProperties) {
        val file = Paths.get(serviceProperties.get("courses") ?: "../data/cfg/agility/courses.json")

        Files.newBufferedReader(file).use { reader ->
            val listType = object : TypeToken<List<CourseEntry>>() {}.type
            val loaded: List<CourseEntry> = gson.fromJson(reader, listType)
            courses.addAll(loaded)
        }

        var obstacleCount = 0
        courses.forEach { course ->
            course.obstacles.forEachIndexed { index, obstacle ->
                obstacle.objectIds = obstacle.objects.map { getRSCM(it) }.toIntArray()
                obstacle.objectIds.forEach { id ->
                    obstaclesByObject.put(id, ObstacleLocation(course, obstacle, index))
                }
                obstacleCount++
            }
        }

        Server.logger.info {
            "Loaded ${courses.size.appendToString("agility course")} " +
                "(${obstacleCount.appendToString("obstacle")})."
        }
    }

    fun lookup(objectId: Int): ObstacleLocation? = obstaclesByObject[objectId]

    /**
     * An obstacle together with the course it sits in and its index in that course's lap order.
     */
    data class ObstacleLocation(
        val course: CourseEntry,
        val obstacle: ObstacleEntry,
        val index: Int,
    ) {
        val isFirst: Boolean get() = index == 0
        val isLast: Boolean get() = index == course.obstacles.size - 1
    }
}
