package org.alter.plugins.content.quests

import org.alter.api.cfg.Varp
import org.alter.api.ext.message
import org.alter.api.ext.setVarp
import org.alter.game.model.entity.Player

/**
 * Player-facing quest state.
 *
 * Everything a quest plugin needs to read or move a player's progress lives here, so no plugin ever
 * touches [Quest.attribute] directly.
 */

/** This player's stage in [quest]; `0` when they have never started it. */
fun Player.questStage(quest: Quest): Int = attr[quest.attribute] ?: 0

/** Whether this player has reached at least [stage] of [quest]. */
fun Player.questAtLeast(
    quest: Quest,
    stage: Int,
): Boolean = questStage(quest) >= stage

fun Player.questState(quest: Quest): QuestState =
    when {
        questStage(quest) <= 0 -> QuestState.NOT_STARTED
        questStage(quest) >= quest.completedStage -> QuestState.COMPLETED
        else -> QuestState.IN_PROGRESS
    }

fun Player.isQuestComplete(quest: Quest): Boolean = questState(quest) == QuestState.COMPLETED

fun Player.hasStartedQuest(quest: Quest): Boolean = questStage(quest) > 0

/**
 * Move this player to [stage] of [quest].
 *
 * **Only ever moves forward.** A stage lower than the one already reached is ignored, which makes
 * every caller safe to run twice - re-using an item, an interrupted dialogue restarted from the top,
 * a double click on the same npc option - without needing its own guard. Quest steps in this
 * codebase are written as "set the stage I represent", not "increment", precisely so this holds.
 */
fun Player.setQuestStage(
    quest: Quest,
    stage: Int,
) {
    if (stage <= questStage(quest)) {
        return
    }
    attr[quest.attribute] = stage
    syncVarps()
}

/**
 * This player's quest points, summed over the quests they have completed.
 *
 * Recomputed on demand rather than stored - see the class comment on [Quest].
 */
fun Player.questPoints(): Int = Quests.all().filter { isQuestComplete(it) }.sumOf { it.questPoints }

/**
 * Push quest state into the client's own player-variables.
 *
 * Called on login and on every stage change. Player-variables are not persisted by this server, so
 * without this a returning player's client would believe every quest was unstarted.
 *
 * ## What is verified here and what is not
 *
 * The **variable ids are real**: [Varp.QUEST_POINTS] is 101 and each quest's own id is the one the
 * live game uses, both cross-checked against RuneLite's generated `VarPlayerID` (`QP = 101`,
 * `FLUFFS = 180`).
 *
 * The **per-stage values are this server's own**. The real game's intermediate values for these
 * variables are not published anywhere, and - unlike the object, npc and item ids this project
 * normally resolves from the cache - this cache carries no quest struct to read them out of (its
 * only structs naming a quest are Achievement Diary tasks). So the stage counter is written
 * straight through. That is right at both ends, which is what actually matters: `0` really is
 * "not started" in the live game, and any value at or above the true completion value reads as
 * complete, so a finished quest never renders as unfinished.
 */
fun Player.syncVarps() {
    Quests.all().forEach { quest ->
        setVarp(quest.varp, questStage(quest))
    }
    setVarp(Varp.QUEST_POINTS, questPoints())
}

/**
 * Finish [quest] and hand out its points.
 *
 * The caller is responsible for the quest's own rewards (experience, items, the completion
 * dialogue): this only does the part that is identical for every quest, so that "what does
 * completing a quest do to the player's account" has one implementation.
 *
 * Returns `false` if the quest was already complete, letting a caller skip a reward it would
 * otherwise hand out twice.
 */
fun Player.completeQuest(quest: Quest): Boolean {
    if (isQuestComplete(quest)) {
        return false
    }
    setQuestStage(quest, quest.completedStage)
    message("<col=ff0000>Congratulations! Quest complete!</col>")
    message("You have completed ${quest.name}.")
    val points = questPoints()
    message("Your Quest point total is now $points.")
    return true
}
