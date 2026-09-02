package org.alter.plugins.content.npcs.animations

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import dev.openrune.cache.CacheManager
import dev.openrune.cache.filestore.definition.SoundData
import dev.openrune.cache.filestore.definition.data.SequenceType
import org.alter.api.ext.npc
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.combat.NpcCombatDef
import org.alter.game.model.entity.Npc
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * Supplies cache-compatible combat animations to attackable NPCs that would otherwise use the
 * human fallback animations. Explicit NPC combat definitions always take precedence.
 */
class MonsterAnimationsPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    private val observedAnimations: Map<Int, List<Int>> = loadObservedAnimations()
    private val namedCombatMedia: Map<String, NamedCombatMedia> = loadNamedCombatMedia()

    init {
        onGlobalNpcSpawn {
            val spawned = npc
            if (spawned.def.isAttackable()) {
                applyResolvedCombatMedia(spawned, replaceFallbackAnimations = !world.plugins.npcCombatDefs.containsKey(spawned.id))
            }
        }
    }

    private fun applyResolvedCombatMedia(
        npc: Npc,
        replaceFallbackAnimations: Boolean,
    ) {
        val named = findNamedCombatMedia(npc.def.name)
        val resolved =
            if (replaceFallbackAnimations) {
                if (named != null) {
                    MonsterAnimationResolver.CombatAnimations(
                        attack = named.attackAnimation,
                        block = named.blockAnimation,
                        death = named.deathAnimation,
                    )
                } else {
                    val observed = observedAnimations[npc.id] ?: return
                    MonsterAnimationResolver.resolve(npc.def, observed) { animationId ->
                        CacheManager.getAnims()[animationId]
                    } ?: return
                }
            } else {
                MonsterAnimationResolver.CombatAnimations(
                    attack = npc.combatDef.attackAnimation,
                    block = npc.combatDef.blockAnimation,
                    death = npc.combatDef.deathAnimation.firstOrNull() ?: return,
                )
            }
        val attackSound = named?.attackSound?.let(::localSound) ?: soundFor(resolved.attack)
        val blockSound = named?.blockSound?.let(::localSound) ?: soundFor(resolved.block)
        val deathSound = named?.deathSound?.let(::localSound) ?: soundFor(resolved.death)
        val current = npc.combatDef

        npc.combatDef =
            current.copy(
                attackAnimation = resolved.attack,
                blockAnimation = resolved.block,
                deathAnimation = listOf(resolved.death),
                defaultAttackSound = current.defaultAttackSound.takeIf { it > 0 } ?: attackSound?.id ?: -1,
                defaultAttackSoundArea = if (current.defaultAttackSound > 0) current.defaultAttackSoundArea else attackSound?.isArea == true,
                defaultAttackSoundRadius = current.defaultAttackSoundRadius.takeIf { it >= 0 } ?: attackSound?.radius ?: -1,
                defaultAttackSoundVolume = current.defaultAttackSoundVolume.takeIf { it > 0 } ?: attackSound?.loops ?: -1,
                defaultBlockSound = current.defaultBlockSound.takeIf { it > 0 } ?: blockSound?.id ?: -1,
                defaultBlockSoundArea = if (current.defaultBlockSound > 0) current.defaultBlockSoundArea else blockSound?.isArea == true,
                defaultBlockSoundRadius = current.defaultBlockSoundRadius.takeIf { it >= 0 } ?: blockSound?.radius ?: -1,
                defaultBlockSoundVolume = current.defaultBlockSoundVolume.takeIf { it > 0 } ?: blockSound?.loops ?: -1,
                defaultDeathSound = current.defaultDeathSound.takeIf { it > 0 } ?: deathSound?.id ?: -1,
                defaultDeathSoundArea = if (current.defaultDeathSound > 0) current.defaultDeathSoundArea else deathSound?.isArea == true,
                defaultDeathSoundRadius = current.defaultDeathSoundRadius.takeIf { it >= 0 } ?: deathSound?.radius ?: -1,
                defaultDeathSoundVolume = current.defaultDeathSoundVolume.takeIf { it > 0 } ?: deathSound?.loops ?: -1,
            )
    }

    private fun soundFor(animationId: Int): CombatSound? {
        val sequence = CacheManager.getAnims()[animationId] ?: return null
        val sound = sequence.firstFrameSound() ?: return null
        return CombatSound(
            id = sound.id,
            loops = sound.loops.coerceAtLeast(1),
            radius = sound.location.coerceAtLeast(0),
        )
    }

    private fun localSound(id: Int) = CombatSound(id = id, loops = 1, radius = 0)

    private fun findNamedCombatMedia(npcName: String): NamedCombatMedia? {
        val normalized = npcName.uppercase().replace(Regex("[^A-Z0-9]+"), "_").trim('_')
        namedCombatMedia[normalized]?.let { return it }

        val suffix = namedCombatMedia.keys.filter { normalized == it || normalized.endsWith("_$it") }.maxByOrNull { it.length }
        if (suffix != null) return namedCombatMedia[suffix]

        val prefix = namedCombatMedia.keys.filter { normalized.startsWith("${it}_") }.maxByOrNull { it.length }
        return prefix?.let(namedCombatMedia::get)
    }

    private fun SequenceType.firstFrameSound(): SoundData? =
        sounds.minByOrNull { it.key }?.value
            ?: soundEffects.firstOrNull { it != null }

    private data class CombatSound(
        val id: Int,
        val loops: Int,
        val radius: Int,
    ) {
        val isArea: Boolean get() = radius > 0
    }

    private fun loadObservedAnimations(): Map<Int, List<Int>> {
        val stream = javaClass.getResourceAsStream(RESOURCE) ?: return emptyMap()
        return stream.use {
            ObjectMapper().readValue(it, object : TypeReference<Map<Int, List<Int>>>() {})
        }
    }

    private fun loadNamedCombatMedia(): Map<String, NamedCombatMedia> {
        val stream = javaClass.getResourceAsStream(NAMED_MEDIA_RESOURCE) ?: return emptyMap()
        return stream.use {
            ObjectMapper().readValue(it, object : TypeReference<Map<String, NamedCombatMedia>>() {})
        }
    }

    private data class NamedCombatMedia(
        val attackAnimation: Int = -1,
        val blockAnimation: Int = -1,
        val deathAnimation: Int = -1,
        val attackSound: Int? = null,
        val blockSound: Int? = null,
        val deathSound: Int? = null,
    )

    private companion object {
        const val RESOURCE = "/npc-animations/openosrs-animations.json"
        const val NAMED_MEDIA_RESOURCE = "/npc-animations/named-combat-media.json"
    }
}
