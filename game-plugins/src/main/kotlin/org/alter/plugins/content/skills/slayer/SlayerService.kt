package org.alter.plugins.content.skills.slayer

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.openrune.cache.CacheManager
import gg.rsmod.util.ServerProperties
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.service.Service
import org.alter.rscm.RSCM.getRSCM
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Loads the Slayer task categories and the six masters' assignment tables, and resolves both
 * against this cache.
 *
 * Two-phase on purpose. [init] runs while plugins are still loading, so it can read the JSON and
 * turn monster *names* into cache npc ids - the cache is fully decoded by then. It cannot yet know
 * which of those monsters this world actually populates, because
 * [org.alter.game.plugin.PluginRepository.init] only spawns entities *after* every service has
 * finished starting. [markAvailable] closes that gap and is called from
 * [SlayerPlugin]'s `onWorldInit`, which does run after the spawns are in the world.
 *
 * That availability pass is the mechanism that keeps this honest: `tasks.json` transcribes the full
 * published assignment tables, this server has built a fraction of the monsters in them, and rather
 * than quietly handing out tasks nobody can finish, anything with no spawned monster is switched
 * off and named in the startup log.
 */
class SlayerService : Service {
    private val gson = Gson()

    val tasks: ObjectArrayList<SlayerTaskEntry> = ObjectArrayList()

    val masters: ObjectArrayList<SlayerMasterEntry> = ObjectArrayList()

    private val tasksByName: Object2ObjectOpenHashMap<String, SlayerTaskEntry> = Object2ObjectOpenHashMap()

    /** Every task an npc id counts towards. Sized for the whole monster roster, read on kill. */
    private val tasksByNpc: Int2ObjectOpenHashMap<SlayerTaskEntry> = Int2ObjectOpenHashMap()

    private val mastersByNpc: Int2ObjectOpenHashMap<SlayerMasterEntry> = Int2ObjectOpenHashMap()

    private var loaded = false

    /**
     * Idempotent, and it has to be.
     *
     * [SlayerPlugin] needs the master list while it is still being *constructed*, because a master
     * has to be spawned before [org.alter.game.plugin.PluginRepository.init] gets to `spawnEntities`
     * and that happens before any service's `init` is called. So the plugin builds this service and
     * loads it itself, then hands the same instance to `loadService`, and the repository's own call
     * a moment later lands here and does nothing.
     */
    override fun init(
        server: Server,
        world: World,
        serviceProperties: ServerProperties,
    ) {
        if (loaded) {
            return
        }
        loaded = true

        val taskFile = Paths.get(serviceProperties.get("tasks") ?: "../data/cfg/slayer/tasks.json")
        val masterFile = Paths.get(serviceProperties.get("masters") ?: "../data/cfg/slayer/masters.json")

        Files.newBufferedReader(taskFile).use { reader ->
            val listType = object : TypeToken<List<SlayerTaskEntry>>() {}.type
            tasks.addAll(gson.fromJson<List<SlayerTaskEntry>>(reader, listType))
        }

        Files.newBufferedReader(masterFile).use { reader ->
            val listType = object : TypeToken<List<SlayerMasterEntry>>() {}.type
            masters.addAll(gson.fromJson<List<SlayerMasterEntry>>(reader, listType))
        }

        resolveTaskNpcs()
        resolveMasters()

        Server.logger.info {
            "Loaded ${tasks.size} Slayer task categories and ${masters.size} Slayer masters."
        }
    }

    /**
     * Turn each task's monster names into cache npc ids.
     *
     * Matching is by name, case-insensitively, across every npc in the cache. That deliberately
     * sweeps up every id a monster has - the twenty ghosts, the thirty goblins - which is the point:
     * the alternative is copying id lists that `content/npcs` already owns and letting the two
     * drift.
     */
    private fun resolveTaskNpcs() {
        val idsByName = Object2ObjectOpenHashMap<String, IntOpenHashSet>()
        CacheManager.getNpcs().entries.forEach { (id, def) ->
            val name = def.name ?: return@forEach
            if (name.isBlank() || name == "null") {
                return@forEach
            }
            idsByName.getOrPut(name.lowercase()) { IntOpenHashSet() }.add(id)
        }

        tasks.forEach { task ->
            require(task.name.isNotBlank()) { "Slayer task must have a name." }
            require(task.assignmentId > 0) { "Slayer task '${task.name}' must have a positive assignment id." }
            require(task.requiredLevel in 1..99) { "Slayer task '${task.name}' level requirement must be 1..99." }
            require(tasksByName.put(task.name, task) == null) { "Duplicate Slayer task '${task.name}'." }

            val ids = IntOpenHashSet()
            task.monsters.forEach { monster ->
                idsByName[monster.lowercase()]?.let { ids.addAll(it) }
            }
            task.npcIds = ids.toIntArray().also { it.sort() }
            task.npcIds.forEach { npcId -> tasksByNpc.putIfAbsent(npcId, task) }
        }
    }

    private fun resolveMasters() {
        masters.forEach { master ->
            require(master.assignments.isNotEmpty()) { "Slayer master '${master.name}' has no assignments." }
            master.npcIds = master.npcs.map { getRSCM(it) }.toIntArray()
            master.npcIds.forEach { id -> mastersByNpc.put(id, master) }

            master.assignments.forEach { assignment ->
                val task =
                    tasksByName[assignment.task]
                        ?: error("Slayer master '${master.name}' assigns unknown task '${assignment.task}'.")
                require(assignment.weight > 0) { "'${master.name}' -> '${assignment.task}' must have a positive weight." }
                require(assignment.min in 1..assignment.max) {
                    "'${master.name}' -> '${assignment.task}' amount range must be 1..max."
                }
                assignment.entry = task
            }
        }
    }

    /**
     * Decide which tasks this world can actually hand out, by checking every task's npc ids against
     * the npcs that were spawned during plugin loading.
     *
     * A monster with a combat definition but no spawn - the baby dragons, which exist as
     * definitions only so far - does not count. Assigning a task with none of its monsters in the
     * world would be a task the player cannot finish and cannot get rid of without paying to
     * cancel it. "Birds" was the standing example until `content/npcs/critters/ChickenSpawns`
     * put chickens on the map; it is assignable now, and this check is what flips it.
     */
    fun markAvailable(world: World) {
        val spawned = IntOpenHashSet()
        world.npcs.forEach { npc -> spawned.add(npc.id) }

        tasks.forEach { task -> task.available = task.npcIds.any { spawned.contains(it) } }

        val enabled = tasks.filter { it.available }
        val perMaster =
            masters.joinToString(", ") { master ->
                "${master.name} ${master.assignments.count { it.entry.available }}/${master.assignments.size}"
            }

        Server.logger.info {
            "Slayer: ${enabled.size} of ${tasks.size} task categories are assignable " +
                "(${enabled.joinToString(", ") { it.name }})."
        }
        Server.logger.info { "Slayer: assignable tasks per master - $perMaster." }
    }

    fun task(name: String): SlayerTaskEntry? = tasksByName[name]

    fun taskFor(npcId: Int): SlayerTaskEntry? = tasksByNpc[npcId]

    fun master(npcId: Int): SlayerMasterEntry? = mastersByNpc[npcId]

    fun master(name: String): SlayerMasterEntry? = masters.firstOrNull { it.name == name }

    private inline fun <K, V> Object2ObjectOpenHashMap<K, V>.getOrPut(
        key: K,
        default: () -> V,
    ): V = this[key] ?: default().also { put(key, it) }
}
