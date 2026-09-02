package org.alter.plugins.content.npcs.animations

import dev.openrune.cache.filestore.definition.data.NpcType
import dev.openrune.cache.filestore.definition.data.SequenceType

/** Resolves combat animations from animations observed for an NPC in the OSRS client. */
internal object MonsterAnimationResolver {
    data class CombatAnimations(
        val attack: Int,
        val block: Int,
        val death: Int,
    )

    fun resolve(
        npc: NpcType,
        observed: List<Int>,
        sequence: (Int) -> SequenceType?,
    ): CombatAnimations? {
        val movement =
            setOf(
                npc.standAnim,
                npc.walkAnim,
                npc.rotateLeftAnim,
                npc.rotateRightAnim,
                npc.rotateBackAnim,
                npc.walkLeftAnim,
                npc.walkRightAnim,
                npc.runSequence,
                npc.runBackSequence,
                npc.runLeftSequence,
                npc.runRightSequence,
                npc.crawlSequence,
                npc.crawlBackSequence,
                npc.crawlLeftSequence,
                npc.crawlRightSequence,
            )

        val candidates = observed.distinct().filter { it >= 0 && it !in movement }.mapNotNull { id -> sequence(id)?.let { id to it } }
        if (candidates.size < 3) return null

        resolveKnownGoblinFamily(candidates.map { it.first }.toSet())?.let { return it }

        // Death sequences are normally the longest non-looping action. Known human death is
        // preferred because many humanoids have emotes and specials in the observed set too.
        val death =
            candidates.firstOrNull { it.first == HUMAN_DEATH }
                ?: candidates.maxWithOrNull(compareBy<Pair<Int, SequenceType>> { duration(it.second) }.thenBy { it.second.forcedPriority })
                ?: return null

        val actions = candidates.filterNot { it.first == death.first }
        if (actions.size < 2) return null

        // Attacks are the action sequences most likely to carry frame sounds. On equal sound
        // evidence, prefer a longer action; the remaining shortest action is the hit reaction.
        val attack =
            actions.maxWithOrNull(
                compareBy<Pair<Int, SequenceType>> { soundCount(it.second) }
                    .thenBy { duration(it.second) }
                    .thenByDescending { it.first },
            ) ?: return null
        val block =
            actions.filterNot { it.first == attack.first }
                .minWithOrNull(compareBy<Pair<Int, SequenceType>> { duration(it.second) }.thenBy { it.first })
                ?: return null

        return CombatAnimations(attack = attack.first, block = block.first, death = death.first)
    }

    private fun resolveKnownGoblinFamily(candidates: Set<Int>): CombatAnimations? {
        fun resolve(
            death: Int,
            block: Int,
            attacks: List<Int>,
        ): CombatAnimations? {
            if (death !in candidates || block !in candidates) return null
            val attack = attacks.firstOrNull { it in candidates } ?: return null
            return CombatAnimations(attack = attack, block = block, death = death)
        }

        return resolve(death = 6182, block = 6183, attacks = listOf(6184, 6185, 6188, 6199))
            ?: resolve(death = 6190, block = 6189, attacks = listOf(6188, 6199, 6185, 6184))
            ?: resolve(death = 6003, block = 6002, attacks = listOf(6001))
            ?: resolve(death = 167, block = 165, attacks = listOf(164, 4784))
    }

    private fun duration(sequence: SequenceType): Int =
        sequence.cycleLength.takeIf { it > 0 }
            ?: sequence.frameDelays?.sum()?.takeIf { it > 0 }
            ?: (sequence.skeletalRangeEnd - sequence.skeletalRangeBegin).coerceAtLeast(1)

    private fun soundCount(sequence: SequenceType): Int = sequence.sounds.size + sequence.soundEffects.count { it != null }

    private const val HUMAN_DEATH = 836
}
