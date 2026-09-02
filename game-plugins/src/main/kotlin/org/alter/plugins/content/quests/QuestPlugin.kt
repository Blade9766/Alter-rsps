package org.alter.plugins.content.quests

import org.alter.api.ext.message
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * The quest framework's own wiring: the parts that belong to every quest rather than any one of
 * them.
 *
 * Individual quests live in their own package under `content/quests/` and register themselves; this
 * plugin only keeps the client in step and provides a way to read quest state back.
 *
 * ## Why there is no quest journal interface
 *
 * OSRS renders the quest list on interface 629, populated by clientscripts driven from cache data.
 * Two things stop that being wired up honestly here. `plugins/filestore` has decoders for objects,
 * npcs, items, structs and enums but **none for interfaces**, so 629's component ids cannot be
 * verified from this cache - the same wall the Smithing anvil UI hit, and the same answer: do not
 * guess component ids and open an interface that comes up empty or wrong. And this cache carries no
 * quest struct to read the list out of even if it could: a sweep of all 5,822 structs and 5,714
 * enums finds the name "Gertrude" only in Achievement Diary task entries.
 *
 * So the journal is [questsCommand] - a chat listing - and the real player-variables are kept
 * accurate underneath it ([syncVarps]). If a quest-list interface is built later, the state it needs
 * is already there and correct; nothing in `content/quests` has to change.
 */
class QuestPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        /*
         * Player-variables are not persisted, so quest state has to be pushed back into the client
         * on every login or a returning player's client believes nothing has been done.
         */
        onLogin { player.syncVarps() }

        questsCommand()
    }

    /**
     * `::quests` - the stand-in for the quest journal.
     *
     * Deliberately available to everyone rather than gated behind a power: it is the only way a
     * player on this server can see what they have done.
     */
    private fun questsCommand() {
        onCommand("quests", description = "List your quest progress.") {
            val quests = Quests.all()
            if (quests.isEmpty()) {
                player.message("No quests are available yet.")
                return@onCommand
            }

            player.message("<col=8b0000>Quest points: ${player.questPoints()} / ${Quests.totalQuestPoints()}</col>")
            quests.forEach { quest ->
                /*
                 * The same three colours the real quest list uses: red unstarted, yellow started,
                 * green complete.
                 */
                val (colour, label) =
                    when (player.questState(quest)) {
                        QuestState.NOT_STARTED -> "ff0000" to "Not started"
                        QuestState.IN_PROGRESS -> "ffff00" to "In progress"
                        QuestState.COMPLETED -> "008000" to "Completed"
                    }
                player.message("<col=$colour>${quest.name}</col> - $label")
            }
        }
    }
}
