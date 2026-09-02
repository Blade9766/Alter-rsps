package org.alter.plugins.content.mechanics.levelup

import org.alter.api.cfg.Graphic
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.LEVEL_UP_INCREMENT
import org.alter.game.model.attr.LEVEL_UP_SKILL_ID
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.TaskPriority
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * Everything that happens when a skill level goes up: the skill's jingle, the
 * fireworks over the player's head, and the "Congratulations, you just advanced..."
 * chatbox box.
 *
 * The engine already did the hard part and nothing consumed it. `Player.addXp`
 * detects the level increase, stashes [LEVEL_UP_SKILL_ID]/[LEVEL_UP_INCREMENT] and
 * calls `executeSkillLevelUp`, and `QueueTask.levelUpMessageBox` already builds the
 * whole interface-233 chatbox with the right per-skill icon - but nothing had ever
 * called [org.alter.game.plugin.KotlinPlugin.setLevelUpLogic], so the hook fired into
 * an empty binding and levelling up was completely silent. This plugin is that
 * missing binding.
 *
 * The jingle and the graphic are fired immediately. The message box has to go through
 * a queued task: this codebase closes chat dialogues through the queue return-value
 * mechanism (the "Click here to continue" click resumes the suspended task, whose
 * `terminateAction` then closes the component), so a dialogue opened outside a
 * QueueTask would have nothing able to dismiss it.
 *
 * **Known trade-off.** `PawnQueueTaskSet.cycle` only ever runs the front task and
 * stops there, and `queue` adds new tasks to the front - so while the box is up, an
 * in-progress skilling task (woodcutting's chop loop, say) is frozen until the player
 * clicks continue or clicks away. Real OSRS keeps you chopping. Fixing that properly
 * means a non-blocking dialogue path, which this engine does not currently have.
 * [TaskPriority.WEAK] is used rather than STANDARD (which refuses to start while a
 * menu is open, and would stall the queue while showing nothing) or STRONG (which
 * calls `terminateTasks` and would cancel the skilling action outright).
 */
class LevelUpPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        setLevelUpLogic {
            val skill = player.attr[LEVEL_UP_SKILL_ID] ?: return@setLevelUpLogic
            val increment = player.attr[LEVEL_UP_INCREMENT] ?: 1
            onLevelUp(player, skill, increment)
        }
    }

    private fun onLevelUp(
        player: Player,
        skill: Int,
        increment: Int,
    ) {
        val newLevel = player.getSkills().getBaseLevel(skill)

        val jingle = LevelUpJingles.jingleFor(skill, newLevel)
        if (jingle != -1) {
            player.playJingle(jingle)
        }

        // Reaching 99 gets the bigger burst; every other level gets the standard one.
        player.graphic(id = if (newLevel >= MAX_SKILL_LEVEL) Graphic.FINAL_LEVEL_UP else Graphic.LEVEL_UP)

        player.queue(TaskPriority.WEAK) {
            levelUpMessageBox(player, skill, increment)
        }
    }

    private companion object {
        const val MAX_SKILL_LEVEL = 99
    }
}
