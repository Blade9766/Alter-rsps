package org.alter.plugins.content.skills.slayer

import org.alter.api.cfg.Varbit
import org.alter.api.ext.chatNpc
import org.alter.api.ext.message
import org.alter.api.ext.messageBox
import org.alter.api.ext.options
import org.alter.api.ext.setVarbit
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask

/**
 * The Slayer masters' rewards menu: spend points to unlock task categories, to block a category, or
 * to cancel the current assignment.
 *
 * Built as dialogue rather than as OSRS's interface 426, for the reason [SlayerPlugin] documents -
 * that interface is clientscript-driven and would come up empty here. Every unlock a player buys is
 * persisted through [Slayer.unlock] *and* written to the same varbit the real client uses, so this
 * menu is a front end over correct state rather than a parallel system.
 *
 * ## Which unlocks are sold
 *
 * Only the ones that do something. The seven listed in [UNLOCKS] each open a category in one or more
 * masters' assignment tables, and buying one has an immediate, visible effect: that task starts
 * being rolled. The rest of the real rewards shop is deliberately absent, because on this server it
 * would be selling nothing:
 *
 * - **Malevolent Masquerade** (craft a slayer helmet) and **Ring Bling** (craft a slayer ring) need
 *   a Crafting recipe that does not exist here yet.
 * - **Bigger and Badder** needs superior slayer monsters, which are not built.
 * - **Gargoyle Smasher**, **Slug Salter**, **Reptile Freezer** and **'Shroom Sprayer** are
 *   finishing-blow automations for four monsters none of which exist here.
 * - **Broader Fletching** needs Fletching, which is not built.
 * - **Like a Boss**, **Duly Noted**, **Task Storage**, **Double Trouble** and **Stop the Wyvern**
 *   all hang off content that is not built either.
 *
 * They are listed here rather than silently dropped so that adding one back is a matter of adding a
 * line to [UNLOCKS] once its content lands, not rediscovering the list.
 *
 * Costs, and the flat 30-point cancel and 100-point block, are the wiki's.
 */
object SlayerRewards {
    /**
     * A purchasable unlock: the key persisted on the player, what the menu calls it, what it costs,
     * and the client varbit that mirrors it.
     *
     * [key] is what `masters.json` names in an assignment row's `unlock` field, which is how buying
     * one of these actually changes what a master will roll.
     *
     * "Warped Reality" has no named varbit constant in [Varbit], so its [varbit] is null; the unlock
     * still persists and still gates the task, it just is not mirrored to the client.
     */
    private data class Unlock(
        val key: String,
        val label: String,
        val cost: Int,
        val varbit: Int?,
    )

    private val UNLOCKS =
        listOf(
            Unlock("seeing_red", "Seeing red - red dragon tasks", 50, Varbit.SLAYER_SEEING_RED),
            Unlock("warped_reality", "Warped Reality - warped creature tasks", 60, null),
            Unlock("reptile_got_ripped", "Reptile got ripped - lizardman tasks", 75, Varbit.SLAYER_REPTILE_GOT_RIPPED),
            Unlock("watch_the_birdie", "Watch the birdie - aviansie tasks", 80, Varbit.SLAYER_WATCH_THE_BIRDIE),
            Unlock("basilocked", "Basilocked - basilisk tasks", 80, Varbit.SLAYER_BASILOCKED),
            Unlock(
                "actual_vampyre_slayer",
                "Actual Vampyre Slayer - vampyre tasks",
                80,
                Varbit.SLAYER_ACTUAL_VAMPYRE_SLAYER,
            ),
            Unlock("hot_stuff", "Hot stuff - TzHaar tasks", 100, Varbit.SLAYER_HOT_STUFF),
        )

    suspend fun open(
        task: QueueTask,
        player: Player,
        master: SlayerMasterEntry,
    ) {
        when (
            task.options(
                player,
                "Unlock a reward.",
                "Block or unblock a task.",
                "Cancel my current task. (${Slayer.CANCEL_COST} points)",
                "How many points do I have?",
                "Nothing, thanks.",
                title = "Slayer Rewards",
            )
        ) {
            1 -> unlockMenu(task, player, master)
            2 -> blockMenu(task, player, master)
            3 -> cancelTask(task, player, master)
            4 -> {
                task.chatNpc(
                    player,
                    "You have ${Slayer.points(player)} Slayer reward points, and<br>" +
                        "a task streak of ${Slayer.streak(player)}.",
                )
                open(task, player, master)
            }
        }
    }

    private suspend fun unlockMenu(
        task: QueueTask,
        player: Player,
        master: SlayerMasterEntry,
    ) {
        val available = UNLOCKS.filterNot { Slayer.hasUnlocked(player, it.key) }

        if (available.isEmpty()) {
            task.chatNpc(player, "You've already unlocked everything I can offer.")
            return
        }

        val labels = available.map { "${it.label} (${it.cost})" } + "Back."
        val choice = task.options(player, *labels.toTypedArray(), title = "Unlock (${Slayer.points(player)} points)")

        if (choice !in 1..available.size) {
            open(task, player, master)
            return
        }

        val unlock = available[choice - 1]

        if (!Slayer.spendPoints(player, unlock.cost)) {
            task.chatNpc(
                player,
                "That costs ${unlock.cost} points and you only have<br>${Slayer.points(player)}.",
            )
            return
        }

        Slayer.unlock(player, unlock.key)
        unlock.varbit?.let { player.setVarbit(it, 1) }
        player.message("You unlock '${unlock.label.substringBefore(" - ")}'.")
        task.chatNpc(player, "Done. You now have ${Slayer.points(player)} points left.")
    }

    private suspend fun blockMenu(
        task: QueueTask,
        player: Player,
        master: SlayerMasterEntry,
    ) {
        when (
            task.options(
                player,
                "Block my current task. (${Slayer.BLOCK_COST} points)",
                "Unblock a task.",
                "Back.",
                title = "Blocked tasks (${Slayer.blocked(player).size}/${Slayer.BLOCK_SLOTS})",
            )
        ) {
            1 -> blockCurrent(task, player)
            2 -> unblockMenu(task, player, master)
            else -> open(task, player, master)
        }
    }

    private suspend fun blockCurrent(
        task: QueueTask,
        player: Player,
    ) {
        val current = Slayer.taskName(player)

        if (current == null) {
            task.chatNpc(player, "You don't have a task to block.")
            return
        }

        if (Slayer.blocked(player).size >= Slayer.BLOCK_SLOTS) {
            task.chatNpc(
                player,
                "You've already blocked ${Slayer.BLOCK_SLOTS} tasks. Unblock<br>one before blocking another.",
            )
            return
        }

        if (!Slayer.spendPoints(player, Slayer.BLOCK_COST)) {
            task.chatNpc(
                player,
                "Blocking a task costs ${Slayer.BLOCK_COST} points and you<br>only have ${Slayer.points(player)}.",
            )
            return
        }

        Slayer.block(player, current)
        task.chatNpc(player, "Consider ${current.lowercase()} blocked. You won't be sent<br>after them again.")
    }

    private suspend fun unblockMenu(
        task: QueueTask,
        player: Player,
        master: SlayerMasterEntry,
    ) {
        val blocked = Slayer.blocked(player)

        if (blocked.isEmpty()) {
            task.chatNpc(player, "You haven't blocked anything.")
            return
        }

        val labels = blocked + "Back."
        val choice = task.options(player, *labels.toTypedArray(), title = "Unblock a task")

        if (choice !in 1..blocked.size) {
            open(task, player, master)
            return
        }

        val name = blocked[choice - 1]
        Slayer.unblock(player, name)
        task.chatNpc(player, "${name.replaceFirstChar { it.uppercase() }} are back on the list.")
    }

    /**
     * Cancelling clears the assignment but leaves the streak alone - it is a paid skip, not a
     * failure, and the streak only ever resets through Turael.
     */
    private suspend fun cancelTask(
        task: QueueTask,
        player: Player,
        master: SlayerMasterEntry,
    ) {
        if (Slayer.taskName(player) == null) {
            task.chatNpc(player, "You don't have a task to cancel.")
            return
        }

        if (!Slayer.spendPoints(player, Slayer.CANCEL_COST)) {
            task.chatNpc(
                player,
                "Cancelling costs ${Slayer.CANCEL_COST} points and you only<br>have ${Slayer.points(player)}.",
            )
            return
        }

        Slayer.cancel(player)
        task.messageBox(player, "Your task has been cancelled. Ask ${master.name} for a new one.")
    }
}
