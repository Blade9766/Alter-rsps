package org.alter.plugins.content.combat.strategy.magic

import org.alter.api.Skills
import org.alter.api.Spellbook
import org.alter.api.ext.getInteractingNpc
import org.alter.api.ext.getInteractingPlayer
import org.alter.api.ext.message
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.Combat
import org.alter.plugins.content.magic.MagicSpells
import org.alter.plugins.content.magic.SpellMetadata

class CombatSpellsPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {

        if (!MagicSpells.isLoaded()) {
            MagicSpells.loadSpellRequirements(world)
        }

        MagicSpells.getCombatSpells().forEach { entry ->
            val requirement = entry.value
            val standard = requirement.spellbook == Spellbook.NORMAL.id
            val ancients = requirement.spellbook == Spellbook.ANCIENTS.id

            if (standard || ancients) {
                onSpellOnNpc(requirement.interfaceId, requirement.component) {
                    castCombatSpellOnPawn(player, player.getInteractingNpc(), requirement)
                }

                onSpellOnPlayer(requirement.interfaceId, requirement.component) {
                    castCombatSpellOnPawn(player, player.getInteractingPlayer(), requirement)
                }
            }
        }
    }

    fun castCombatSpellOnPawn(
        player: Player,
        pawn: Pawn,
        spellMetadata: SpellMetadata,
    ) {
        val combatSpell = CombatSpell.values.firstOrNull { spell -> spell.id == spellMetadata.paramItem }
        if (combatSpell != null) {
            val curseEffect = combatSpell.curseEffect
            if (curseEffect != null && hasActiveStatDrain(pawn)) {
                player.message("That creature's stats have already been lowered.")
                return
            }
            player.attr[Combat.CASTING_SPELL] = combatSpell
            player.attack(pawn)
        } else {
            /*
             * The spell is not defined in [CombatSpell].
             */
            if (world.devContext.debugMagicSpells) {
                player.message("Undefined combat spell: [spellId=${spellMetadata.paramItem}, name=${spellMetadata.name}]")
            }
        }
    }

    /**
     * Whether any of [target]'s five combat stats are currently below their unlowered
     * level - per the wiki, curse spells "can only be cast if [the] opponent's/target's
     * stats haven't already been lowered", checked broadly rather than just the one
     * stat the spell being cast would itself drain (matching the real curse-spell
     * family's shared restriction - the same rule
     * [org.alter.plugins.content.npcs.darkwizard.DarkWizardCombatPlugin] already
     * applies for Dark wizards casting Confuse/Weaken *at* players).
     */
    private fun hasActiveStatDrain(target: Pawn): Boolean =
        when (target) {
            is Player -> COMBAT_STATS.any { target.getSkills().getCurrentLevel(it) < target.getSkills().getBaseLevel(it) }
            is Npc -> COMBAT_STATS.any { target.stats.getCurrentLevel(it) < target.stats.getMaxLevel(it) }
            else -> false
        }

    private companion object {
        val COMBAT_STATS = listOf(Skills.ATTACK, Skills.STRENGTH, Skills.DEFENCE, Skills.MAGIC, Skills.RANGED)
    }
}
