package org.alter.plugins.content.skills.slayer

import org.alter.api.Skills
import org.alter.api.cfg.Varbit
import org.alter.api.cfg.Varp
import org.alter.api.ext.message
import org.alter.api.ext.setVarbit
import org.alter.api.ext.setVarp
import org.alter.game.model.World
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.plugins.content.items.jewellery.JewelleryPerks

/**
 * The Slayer skill's state and rules: what a player is hunting, how many are left, their streak,
 * their reward points, and what they have blocked or unlocked.
 *
 * Everything a player carries between logins is a persisted [AttributeKey] rather than a varbit.
 * The client's own Slayer varps and varbits *are* written - [syncInterface] mirrors the task,
 * amount, master, points and block list into them so the assignment shows up in the player's
 * interface instead of only in chat - but they are a display of this state, never the source of it.
 * Reward points alone would not fit in one varbit past 131,071, and the block list is a set of task
 * names, not ids.
 *
 * ## Where the numbers come from
 *
 * Assignment weights, amounts and requirements are transcribed from each master's own wiki
 * assignment table into `data/cfg/slayer/masters.json`; task categories and the monsters that count
 * towards them into `data/cfg/slayer/tasks.json`. Points per task, the streak milestones and the
 * "no points for the first four tasks" rule come from the wiki's Slayer reward point article.
 * Slayer experience per kill is not configured here at all - it is whatever the monster's own
 * combat definition declares in its `slayerData { xp = ... }` block, which the monster files under
 * `content/npcs` already set and which, until now, nothing in the server ever read.
 *
 * ## What is deliberately not built
 *
 * - **Konar and Krystilia.** Konar's tasks are location-locked and Krystilia's are Wilderness-only;
 *   both need systems this server does not have. Their npcs and their client master ids exist, so
 *   they are additions to the JSON rather than to this file.
 * - **Superior slayer monsters, the slayer helmet's crafting recipe, and the Slayer ring.** These
 *   are reward *unlocks* that need items and monsters built first; the unlocks themselves are
 *   purchasable and recorded, so the content can be hung off [hasUnlocked] when it lands.
 * - **Quest requirements.** There is no quest framework here, so the quest column of the wiki's
 *   assignment tables is not enforced - the same call `content/areas/edgeville/npcs/stores`
 *   documents for Oziach. Combat level and Slayer level requirements *are* enforced, because those
 *   this server can actually check.
 */
object Slayer {
    /** The task category the player is currently assigned, by [SlayerTaskEntry.name]. */
    private val TASK = AttributeKey<String>(persistenceKey = "slayer_task")

    /** Kills remaining on the current task. Absent or <= 0 means no task. */
    private val TASK_AMOUNT = AttributeKey<Int>(persistenceKey = "slayer_task_amount")

    /** The master who handed out the current task, by [SlayerMasterEntry.name]. */
    private val TASK_MASTER = AttributeKey<String>(persistenceKey = "slayer_task_master")

    private val POINTS = AttributeKey<Int>(persistenceKey = "slayer_points")

    /** Consecutive completed tasks. Drives both the milestone bonus and the four-task grace period. */
    private val STREAK = AttributeKey<Int>(persistenceKey = "slayer_streak")

    /** Blocked task names, comma separated - the same compact form `MusicUnlocks` persists with. */
    private val BLOCKED = AttributeKey<String>(persistenceKey = "slayer_blocked")

    /** Purchased reward unlock keys, comma separated. */
    private val UNLOCKS = AttributeKey<String>(persistenceKey = "slayer_unlocks")

    /** Points to cancel the current assignment. Flat across every standard master. */
    const val CANCEL_COST = 30

    /** Points to block a task category permanently. Flat across every standard master. */
    const val BLOCK_COST = 100

    /**
     * How many categories a player may block at once.
     *
     * OSRS grants one slot per 50 quest points, up to six. There are no quests here, so every player
     * gets the full six rather than none - the alternative would make the block feature dead code.
     * Six is also exactly how many blocked-task varbits the cache defines.
     */
    const val BLOCK_SLOTS = 6

    /** No points at all until the fifth completed task. */
    private const val POINTS_GRACE_TASKS = 4

    private val BLOCK_VARBITS =
        intArrayOf(
            Varbit.SLAYER_BLOCKED_TASK_1,
            Varbit.SLAYER_BLOCKED_TASK_2,
            Varbit.SLAYER_BLOCKED_TASK_3,
            Varbit.SLAYER_BLOCKED_TASK_4,
            Varbit.SLAYER_BLOCKED_TASK_5,
            Varbit.SLAYER_BLOCKED_TASK_6,
        )

    fun service(world: World): SlayerService? = world.getService(SlayerService::class.java)

    // ------------------------------------------------------------------ state

    fun taskName(player: Player): String? = player.attr[TASK]?.takeIf { amount(player) > 0 }

    fun task(player: Player): SlayerTaskEntry? {
        val name = taskName(player) ?: return null
        return service(player.world)?.task(name)
    }

    fun amount(player: Player): Int = player.attr[TASK_AMOUNT] ?: 0

    fun masterName(player: Player): String? = player.attr[TASK_MASTER]

    fun points(player: Player): Int = player.attr[POINTS] ?: 0

    fun streak(player: Player): Int = player.attr[STREAK] ?: 0

    fun blocked(player: Player): List<String> =
        (player.attr[BLOCKED] ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() }

    fun hasUnlocked(
        player: Player,
        unlock: String,
    ): Boolean = unlock in unlocks(player)

    fun unlocks(player: Player): Set<String> =
        (player.attr[UNLOCKS] ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    fun unlock(
        player: Player,
        unlock: String,
    ) {
        player.attr[UNLOCKS] = (unlocks(player) + unlock).sorted().joinToString(",")
    }

    fun addPoints(
        player: Player,
        amount: Int,
    ) {
        player.attr[POINTS] = (points(player) + amount).coerceAtLeast(0)
        syncInterface(player)
    }

    fun spendPoints(
        player: Player,
        amount: Int,
    ): Boolean {
        if (points(player) < amount) {
            return false
        }
        addPoints(player, -amount)
        return true
    }

    // ------------------------------------------------------- on-task checking

    /**
     * Whether [npc] counts towards the player's current assignment.
     *
     * This is the check the black mask and slayer helmet damage bonuses hang off, so it runs on
     * every hit landed - hence the sorted-array binary search in [SlayerTaskEntry.matches] rather
     * than a set lookup through a boxed map.
     */
    fun isOnTask(
        player: Player,
        npc: Npc,
    ): Boolean {
        if (amount(player) <= 0) {
            return false
        }
        return task(player)?.matches(npc.id) == true
    }

    // ---------------------------------------------------------- assigning work

    /**
     * Why a master refused to hand out a task, so the caller can say the right line.
     */
    sealed interface AssignResult {
        data class Assigned(val task: SlayerTaskEntry, val amount: Int) : AssignResult

        data class CombatTooLow(val required: Int) : AssignResult

        data class SlayerTooLow(val required: Int) : AssignResult

        /** The master has nothing this player currently qualifies for. */
        data object NothingSuitable : AssignResult
    }

    /**
     * Whether asking [master] for work right now would be a Turael skip: the player is holding an
     * unfinished task from a *different* master, and this one gives tasks away for free.
     *
     * This is the one case where a master hands out a new assignment over the top of a live one.
     * Every other master tells the player to go and finish what they have.
     */
    fun isTuraelSkip(
        player: Player,
        master: SlayerMasterEntry,
    ): Boolean {
        if (!master.resetsStreak || amount(player) <= 0) {
            return false
        }
        val holder = masterName(player)
        return holder != null && holder != master.name
    }

    fun canAssign(
        player: Player,
        master: SlayerMasterEntry,
    ): AssignResult? =
        when {
            player.combatLevel < master.combatRequirement -> AssignResult.CombatTooLow(master.combatRequirement)
            player.getSkills().getBaseLevel(Skills.SLAYER) < master.slayerRequirement ->
                AssignResult.SlayerTooLow(master.slayerRequirement)
            else -> null
        }

    /**
     * Roll a new assignment from [master] and give it to the player.
     *
     * Selection is a plain weighted draw over every row the player qualifies for right now:
     * unblocked, monster actually spawned in this world, Slayer level high enough, combat level high
     * enough, and any required reward unlock purchased. Filtering *before* the roll rather than
     * re-rolling on a rejection is what makes the published weights mean what they say - a blocked
     * task's share is redistributed over the rest instead of vanishing.
     *
     * Replacing an unfinished task is allowed here and refused by the caller for every master but
     * Turael - see [isTuraelSkip]. When it does happen it costs the streak, which is the published
     * price of "Turael skipping": the streak resets to zero and four more tasks have to be completed
     * before points start being paid again.
     */
    fun assign(
        player: Player,
        master: SlayerMasterEntry,
    ): AssignResult {
        canAssign(player, master)?.let { return it }

        val slayerLevel = player.getSkills().getBaseLevel(Skills.SLAYER)
        val blocked = blocked(player).toSet()

        val candidates =
            master.assignments.filter { row ->
                row.entry.available &&
                    row.entry.name !in blocked &&
                    slayerLevel >= row.entry.requiredLevel &&
                    player.combatLevel >= row.combatLevel &&
                    (row.unlock == null || hasUnlocked(player, row.unlock))
            }

        if (candidates.isEmpty()) {
            return AssignResult.NothingSuitable
        }

        if (isTuraelSkip(player, master)) {
            player.attr[STREAK] = 0
        }

        val row = pickWeighted(candidates, player.world)
        val amount = player.world.random(row.min..row.max)

        player.attr[TASK] = row.entry.name
        player.attr[TASK_AMOUNT] = amount
        player.attr[TASK_MASTER] = master.name
        syncInterface(player)

        return AssignResult.Assigned(row.entry, amount)
    }

    private fun pickWeighted(
        candidates: List<SlayerAssignmentEntry>,
        world: World,
    ): SlayerAssignmentEntry {
        val total = candidates.sumOf { it.weight }
        var roll = world.random(total - 1)
        candidates.forEach { row ->
            roll -= row.weight
            if (roll < 0) {
                return row
            }
        }
        return candidates.last()
    }

    // -------------------------------------------------------------- kill hook

    /**
     * Credit a kill towards the player's task, if it counts.
     *
     * Slayer experience is awarded here and only here, from the dying npc's own
     * [org.alter.game.model.combat.NpcCombatDef.slayerXp]. That mirrors the real skill, where a
     * monster gives Slayer experience only while it is your assignment - and it is the reason the
     * `slayerData { xp = ... }` blocks scattered across `content/npcs` were, before this, values
     * nothing in the server ever read.
     */
    fun onKill(
        player: Player,
        npc: Npc,
    ) {
        if (!isOnTask(player, npc)) {
            return
        }
        val task = task(player) ?: return

        val xp = npc.combatDef.slayerXp
        if (xp > 0.0) {
            player.addXp(Skills.SLAYER, xp)
        }

        /*
         * The two Slayer bracelets, which is why the kill is worth a *count* rather than a fixed 1:
         * an expeditious bracelet makes it worth two, a bracelet of slaughter worth none. Asked
         * after the experience above, because neither of them changes what the kill pays - the
         * expeditious bracelet's extra kill grants no experience, and the slaughter bracelet's
         * skipped kill still does.
         */
        val counted = JewelleryPerks.slayerKillCount(player)
        if (counted == 0) {
            return
        }

        val left = (amount(player) - counted).coerceAtLeast(0)
        player.attr[TASK_AMOUNT] = left

        if (left > 0) {
            syncInterface(player)
            if (left == 1) {
                player.message("You're assigned to kill ${task.name.lowercase()}; only one more to go.")
            } else {
                player.message("You're assigned to kill ${task.name.lowercase()}; only $left more to go.")
            }
            return
        }

        complete(player, task)
    }

    private fun complete(
        player: Player,
        task: SlayerTaskEntry,
    ) {
        val master = masterName(player)?.let { service(player.world)?.master(it) }
        val streak = streak(player) + 1
        player.attr[STREAK] = streak
        player.attr[TASK_AMOUNT] = 0

        val award = pointsFor(master, streak)
        player.message("You have completed your task! Return to a Slayer master.")

        if (award > 0) {
            addPoints(player, award)
            player.message(
                "You've completed $streak tasks in a row and received $award points; " +
                    "you now have ${points(player)}.",
            )
        } else {
            syncInterface(player)
        }
    }

    /**
     * Points for completing the [streak]-th consecutive task under [master].
     *
     * Nothing for the first four tasks, then the master's base rate, multiplied on milestones -
     * every 10th task pays five times, every 50th fifteen, every 100th twenty-five, every 250th
     * thirty-five and every 1,000th fifty. The milestones nest (task 1,000 is also a multiple of
     * 250, 100, 50 and 10), so the largest matching multiplier is the one that applies.
     */
    fun pointsFor(
        master: SlayerMasterEntry?,
        streak: Int,
    ): Int {
        if (master == null || master.pointsPerTask <= 0 || streak <= POINTS_GRACE_TASKS) {
            return 0
        }
        val multiplier =
            when {
                streak % 1000 == 0 -> 50
                streak % 250 == 0 -> 35
                streak % 100 == 0 -> 25
                streak % 50 == 0 -> 15
                streak % 10 == 0 -> 5
                else -> 1
            }
        return master.pointsPerTask * multiplier
    }

    // ------------------------------------------------------- cancel and block

    fun cancel(player: Player) {
        player.attr[TASK_AMOUNT] = 0
        player.attr.remove(TASK)
        player.attr.remove(TASK_MASTER)
        syncInterface(player)
    }

    fun block(
        player: Player,
        taskName: String,
    ): Boolean {
        val current = blocked(player)
        if (taskName in current || current.size >= BLOCK_SLOTS) {
            return false
        }
        player.attr[BLOCKED] = (current + taskName).joinToString(",")
        cancel(player)
        return true
    }

    fun unblock(
        player: Player,
        taskName: String,
    ): Boolean {
        val current = blocked(player)
        if (taskName !in current) {
            return false
        }
        player.attr[BLOCKED] = current.filter { it != taskName }.joinToString(",")
        syncInterface(player)
        return true
    }

    // --------------------------------------------------------- client display

    /**
     * Mirror the current state into the client's own Slayer varps and varbits.
     *
     * [Varp.SLAYER_CURRENT_ASSIGNMENT_TYPE] takes the cache enum 693 id of the task category and
     * [Varp.SLAYER_CURRENT_ASSIGNMENT_AMOUNT] the kills left; between them the client renders the
     * assignment itself, so this is what makes a task visible outside of chat. The blocked-task
     * varbits hold enum ids too, one per slot, zeroed for empty slots.
     *
     * Reward points are clamped to the 17 bits varbit 4068 actually has. A player who somehow banks
     * more than 131,071 points keeps them - [points] is the real figure - and only the display
     * saturates.
     */
    fun syncInterface(player: Player) {
        val service = service(player.world)
        val task = taskName(player)?.let { service?.task(it) }
        val amount = amount(player)

        player.setVarp(Varp.SLAYER_CURRENT_ASSIGNMENT_TYPE, if (amount > 0 && task != null) task.assignmentId else 0)
        player.setVarp(Varp.SLAYER_CURRENT_ASSIGNMENT_AMOUNT, amount)

        masterName(player)?.let { name ->
            service?.master(name)?.let { player.setVarbit(Varbit.SLAYER_CURRENT_MASTER, it.masterId) }
        }

        player.setVarbit(Varbit.SLAYER_REWARD_POINTS, points(player).coerceIn(0, 131_071))

        val blockedIds = blocked(player).mapNotNull { service?.task(it)?.assignmentId }
        BLOCK_VARBITS.forEachIndexed { index, varbit ->
            player.setVarbit(varbit, blockedIds.getOrElse(index) { 0 })
        }
    }
}
