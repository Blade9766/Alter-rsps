package org.alter.plugins.content.quests

import org.alter.game.model.attr.AttributeKey

/**
 * The lifecycle of one quest, for one player.
 *
 * Derived from the player's stage counter rather than stored, so there is exactly one source of
 * truth ([Quest.attribute]) and no way for the two to disagree.
 */
enum class QuestState {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
}

/**
 * One quest's identity.
 *
 * ## How progress is modelled
 *
 * A quest is an `Int` stage counter, persisted per player under `quest_<id>`. `0` always means
 * "not started"; [completedStage] and anything above it means "finished"; everything between is
 * the quest's own business. Quests name their stages as constants (see
 * [org.alter.plugins.content.quests.gertrudescat.GertrudesCat]) rather than using bare numbers.
 *
 * A monotonic counter rather than a set of flags is deliberate: it is what the real game's quest
 * varps are, it makes "have I got at least this far" a comparison instead of a lookup, and it
 * survives being written out of order by a buggy branch (the worst case is a skipped step, not a
 * quest that can never be finished).
 *
 * ## Quest points are not stored
 *
 * There is no persisted quest-point total. [org.alter.plugins.content.quests.questPoints] sums
 * [questPoints] over the completed quests every time it is asked. That costs nothing at this scale
 * and makes the classic double-award bug - completing a quest twice, or an interrupted completion
 * awarding points but not setting the stage - impossible to write.
 *
 * @param id stable key used for persistence. Never rename one of these after players have saves.
 * @param name the quest's display name, as the real game spells it.
 * @param questPoints quest points awarded on completion.
 * @param varp the real OSRS player-variable this quest occupies. See [syncVarps] for what is and
 *   is not known about these.
 * @param completedStage the stage at which the quest counts as finished.
 */
data class Quest(
    val id: String,
    val name: String,
    val questPoints: Int,
    val varp: Int,
    val completedStage: Int,
) {
    /**
     * Where this quest's stage lives on the player.
     *
     * Built per instance, which is safe only because [AttributeKey] defines equality and hashing on
     * `persistenceKey` when it is non-null - two separately constructed keys with the same string
     * are the same key. The save layer relies on that too: it reconstructs
     * `AttributeKey<Any>(key)` from the document on load.
     */
    val attribute: AttributeKey<Int> = AttributeKey(persistenceKey = "quest_$id")
}

/**
 * Every quest the server knows about.
 *
 * Registration happens in each quest plugin's constructor, so the roster is assembled at plugin-load
 * time and is complete before any player can log in.
 */
object Quests {
    private val byId = LinkedHashMap<String, Quest>()

    /**
     * Add [quest] to the roster.
     *
     * Idempotent by id, because plugins are constructed once per repository load but the process may
     * load a repository more than once (the reload command does exactly that), and a duplicate entry
     * would double-count the quest's points.
     */
    fun register(quest: Quest) {
        byId[quest.id] = quest
    }

    fun get(id: String): Quest? = byId[id]

    /** Every registered quest, in registration order. */
    fun all(): Collection<Quest> = byId.values

    fun size(): Int = byId.size

    /** The maximum quest points obtainable from everything currently built. */
    fun totalQuestPoints(): Int = byId.values.sumOf { it.questPoints }
}
