package org.alter.plugins.content.objects.gates

import io.github.oshai.kotlinlogging.KotlinLogging
import org.alter.api.Skills
import org.alter.api.cfg.Sound
import org.alter.api.ext.closeDoor
import org.alter.api.ext.getInteractingGameObj
import org.alter.api.ext.openDoor
import org.alter.api.ext.message
import org.alter.api.ext.playSound
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.GameObject
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.objects.door.DoorStickState

class GatePlugin(
    r: PluginRepository, world: World,
    server: Server) : KotlinPlugin(r, world, server) {

    val STICK_STATE = AttributeKey<DoorStickState>()

    /**
     * The [GateSet] an opened gate came from, stamped on the two objects spawned by [openGate].
     *
     * Several closed gates share one opened pair - the wrought iron gate has four closed id sets in
     * the cache (1568/1569, 1727/1728, 23552/23554, ...) and exactly one openable/closeable pair,
     * 1571/1572 - so the opened id alone cannot say which gate to put back. The clicked object can.
     */
    private val GATE_SET = AttributeKey<GateSet>()

    init {
        loadService(GateService())

        onWorldInit {
            val service = world.getService(GateService::class.java) ?: return@onWorldInit

            /*
             * PluginRepository.bindObject *throws* on a duplicate binding, and a throw here takes
             * the whole plugin - every gate in the game - down with it. Gate ids are therefore
             * collapsed to one entry each before anything is bound: closed ids are unique per set
             * and a repeat is a config mistake worth shouting about, while opened ids are shared by
             * design and the map only picks the fallback set for one that was never opened by us.
             */
            val byClosed = LinkedHashMap<Int, GateSet>()
            val byOpened = LinkedHashMap<Int, GateSet>()
            service.gates.forEach { gate ->
                listOf(gate.closed.hinge, gate.closed.extension).forEach { id ->
                    byClosed.putIfAbsent(id, gate)?.let {
                        logger.warn { "Closed gate $id is claimed by more than one gate set; ignoring the later one." }
                    }
                }
                listOf(gate.opened.hinge, gate.opened.extension).forEach { id ->
                    byOpened.putIfAbsent(id, gate)
                }
            }

            byClosed.forEach { (id, gate) ->
                val requirement = gate.requirement?.let { req ->
                    // Skills are named by the cache's own enum, so a typo in the json is caught here
                    // rather than silently gating on skill 0.
                    val skill = Skills.getSkillForName(world, SKILL_COUNT, req.skill.lowercase())
                    if (skill == -1) {
                        logger.warn { "Gate $id requires unknown skill '${req.skill}'; the requirement is ignored." }
                        null
                    } else {
                        skill to req.level
                    }
                }
                bindIfPresent(id, "open") {
                    if (requirement == null || meetsRequirement(player, requirement.first, requirement.second)) {
                        openGate(player, player.getInteractingGameObj(), gate)
                    }
                }
            }

            byOpened.forEach { (id, fallback) ->
                bindIfPresent(id, "close") {
                    val obj = player.getInteractingGameObj()
                    closeGate(player, obj, obj.attr[GATE_SET] ?: fallback)
                }
            }
        }

    }

    /**
     * Binds [option] on [obj] only if the object actually carries it.
     *
     * `onObjOption` throws when the option is absent, and not every gate has both halves: the
     * Emir's Arena gates (44920/44921) open into 44922/44923, which have no actions at all and so
     * can never be closed. Binding "close" on those unconditionally would throw during world init
     * and take *every* gate in the game down with it, Al Kharid's included - so a missing option is
     * treated as "this gate does not do that" rather than as a mistake in the config.
     */
    private fun bindIfPresent(
        obj: Int,
        option: String,
        logic: (org.alter.game.plugin.Plugin).() -> Unit,
    ) {
        val present =
            runCatching {
                dev.openrune.cache.CacheManager.getObject(obj).actions.any { it?.lowercase() == option }
            }.getOrDefault(false)
        if (present) {
            onObjOption(obj = obj, option = option, lineOfSightDistance = 1, logic = logic)
        }
    }

    /**
     * The one gate in the game with a skill requirement is the Wilderness Agility Course's
     * (23552/23554), which the wiki gates at 52 Agility. Boosts do not open it in the real game,
     * hence the base level rather than the current one.
     */
    private fun meetsRequirement(
        p: Player,
        skill: Int,
        level: Int,
    ): Boolean {
        if (p.getSkills().getBaseLevel(skill) >= level) {
            return true
        }
        p.message("You need ${Skills.getSkillName(world, skill)} level $level to open this gate.")
        return false
    }

    fun copyStickVars(from: GameObject, to: GameObject) {
        if (from.attr.has(STICK_STATE)) {
            to.attr[STICK_STATE] = from.attr[STICK_STATE]!!
        }
    }

    /**
     * Swings both leaves open.
     *
     * **A gate is two leaves, not one panel.** Each leaf pivots on its own outer post and they
     * swing apart, which is why the two calls below differ only in [org.alter.api.ext.openDoor]'s
     * `invertRot`: both leaves step onto the tile directly outside their closed one, and the
     * opposite rotations put each one's hinge end back on the post it started against. This is the
     * same treatment
     * [org.alter.plugins.content.objects.door.DoorPlugin.handleDoubleDoors] gives double doors and
     * that `areas/lumbridge/objs/AlkharidGate` hand-rolls for the toll gates, which are this very
     * model (1509/1511).
     *
     * What this replaced swung the *whole* gate about one post: both leaves ended up stacked in a
     * line two tiles deep, sticking out perpendicular to the fence with nothing left against the
     * other post. It round-tripped correctly and blocked the right tiles, so nothing but looking at
     * it in game would have caught it.
     */
    fun openGate(p: Player, obj: GameObject, gates: GateSet) {
        val hinge = obj.id == gates.closed.hinge
        val otherGate = getNeighbourGate(world, obj, if (hinge) gates.closed.extension else gates.closed.hinge) ?: return

        val hingeObj = if (hinge) obj else otherGate
        val extensionObj = if (hinge) otherGate else obj

        val newHinge = world.openDoor(hingeObj, opened = gates.opened.hinge, invertRot = true)
        val newExtension = world.openDoor(extensionObj, opened = gates.opened.extension)

        copyStickVars(hingeObj, newHinge)
        copyStickVars(extensionObj, newExtension)

        newHinge.attr[GATE_SET] = gates
        newExtension.attr[GATE_SET] = gates

        p.playSound(Sound.OPEN_DOOR_SFX)
    }

    /**
     * The exact inverse of [openGate]. `closeDoor` reads `invertRot` the other way round, so the
     * hinge leaf passes `true` to both flags and the extension passes neither - the pairing
     * `DoorPlugin` uses for its left leaf.
     */
    fun closeGate(p: Player, obj: GameObject, gates: GateSet) {
        val hinge = obj.id == gates.opened.hinge
        val otherGate = getNeighbourGate(world, obj, if (hinge) gates.opened.extension else gates.opened.hinge) ?: return

        val hingeObj = if (hinge) obj else otherGate
        val extensionObj = if (hinge) otherGate else obj

        val newHinge =
            world.closeDoor(hingeObj, closed = gates.closed.hinge, invertRot = true, invertTransform = true)
        val newExtension = world.closeDoor(extensionObj, closed = gates.closed.extension)

        copyStickVars(hingeObj, newHinge)
        copyStickVars(extensionObj, newExtension)

        p.playSound(Sound.CLOSE_DOOR_SFX)
    }

    fun getNeighbourGate(world: World, obj: GameObject, otherGate: Int): GameObject? {
        val tile = obj.tile

        for (x in -1..1) {
            for (z in -1..1) {
                if (x == 0 && z == 0) {
                    continue
                }
                val transform = tile.transform(x, z)
                val tileObj = world.getObject(transform, type = obj.type)
                if (tileObj?.id == otherGate) {
                    return tileObj
                }
            }
        }
        return null
    }

    private companion object {
        /** Attack through Construction - what [Skills] itself enumerates. */
        const val SKILL_COUNT = Skills.CONSTRUCTION + 1

        private val logger = KotlinLogging.logger {}
    }
}
