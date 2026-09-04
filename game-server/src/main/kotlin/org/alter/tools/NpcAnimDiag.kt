package org.alter.tools

import dev.openrune.cache.CacheManager
import java.nio.file.Paths

/**
 * TEMPORARY diagnostic - prints an npc's idle/movement animations and size, and the length and
 * frame group of any animation ids given, so a monster's combat animations can be matched against
 * the rig its model actually uses.
 *
 * Usage: gradlew :game-server:npcAnimDiag --args="6611,6612,11993 5495,5499,5503,5507,5508"
 */
object NpcAnimDiag {
    @JvmStatic
    fun main(args: Array<String>) {
        CacheManager.init(Paths.get("data/cache"), 228)

        args.getOrNull(0)?.split(',')?.mapNotNull { it.trim().toIntOrNull() }?.forEach { id ->
            val def = runCatching { CacheManager.getNpc(id) }.getOrNull() ?: return@forEach
            println(
                "npc=$id name='${def.name}' lvl=${def.combatLevel} size=${def.size} " +
                    "stand=${def.standAnim} walk=${def.walkAnim} attackable=${def.isAttackable()} " +
                    "actions=${def.actions.filterNotNull()} models=${def.models}",
            )
        }

        println()
        args.getOrNull(1)?.split(',')?.mapNotNull { it.trim().toIntOrNull() }?.forEach { id ->
            val seq = CacheManager.getAnims()[id]
            if (seq == null) {
                println("anim=$id MISSING")
                return@forEach
            }
            // A frame id packs the frame group in its high 16 bits; every frame of one animation
            // belongs to the same group, and that group is what ties it to a model's rig.
            val groups = seq.frameIDs?.map { it ushr 16 }?.distinct() ?: emptyList()
            println(
                "anim=$id cycles=${seq.cycleLength} frames=${seq.frameIDs?.size ?: 0} " +
                    "frameGroups=$groups skeletalId=${seq.skeletalId} priority=${seq.forcedPriority} " +
                    "loops=${seq.maxLoops} " +
                    "sounds=${seq.sounds.entries.joinToString { "f${it.key}=snd${it.value.id}" }} " +
                    "skelRange=${seq.skeletalRangeBegin}..${seq.skeletalRangeEnd}",
            )
        }
    }
}
