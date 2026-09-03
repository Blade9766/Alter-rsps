package org.alter.plugins.content.areas.duelarena

import org.alter.api.Skills
import org.alter.api.InterfaceDestination
import org.alter.api.ext.InterfaceEvent
import org.alter.api.ext.*
import org.alter.game.model.attr.POISON_TICKS_LEFT_ATTR
import org.alter.game.model.container.ContainerStackType
import org.alter.game.model.container.ItemContainer
import org.alter.game.model.entity.GroundItem
import org.alter.game.model.entity.Player
import org.alter.game.model.move.moveTo
import org.alter.game.model.move.stopMovement
import org.alter.game.model.timer.FROZEN_TIMER
import org.alter.game.model.timer.POISON_TIMER
import org.alter.plugins.service.marketvalue.ItemMarketValueService

/**
 * One duel, shared by both players.
 *
 * Trading runs two mirrored session objects, one per player, and spends a fair amount of effort
 * keeping them agreeing with each other. A duel has more shared state than a trade does - one set
 * of rules, one arena, one winner - so there is a single session here and each player gets a
 * [DuelSide]. "The other side" is then a lookup rather than a second object to keep in step.
 */
class DuelSession(first: Player, second: Player) {
    val sides = listOf(DuelSide(first), DuelSide(second))

    var stage: DuelStage = DuelStage.STAKE
        private set

    /** The rule bitmask, mirrored into varp 286 on both clients. */
    var rules: Int = 0
        private set

    /** The locked-slot bitmask, mirrored into varbit 642 on both clients. */
    var lockedSlots: Int = 0
        private set

    /** The arena this duel is being fought in, once one has been claimed. */
    var plot: DuelPlot? = null
        private set

    /**
     * The world cycle before which Accept does nothing. Re-armed by [touch] every time either
     * player changes anything, which is the server's half of `[clientscript,duel_accept_button]`.
     */
    private var acceptableAt: Int = 0

    private val priceService = first.world.getService(ItemMarketValueService::class.java)

    fun sideOf(player: Player): DuelSide = sides.first { it.player == player }

    fun other(player: Player): DuelSide = sides.first { it.player != player }

    fun both(action: (DuelSide) -> Unit) = sides.forEach(action)

    fun hasRule(rule: DuelRule): Boolean = (rules and (1 shl rule.bit)) != 0

    fun isSlotLocked(slot: Int): Boolean = (lockedSlots and (1 shl slot)) != 0

    fun isFighting(): Boolean = stage == DuelStage.FIGHTING

    /**
     * True once the players are in the arena, counting down or fighting - the point past which the
     * stake has left their hands and the duel has to end in a result rather than a decline.
     */
    fun isCommitted(): Boolean = stage == DuelStage.COUNTDOWN || stage == DuelStage.FIGHTING

    // ------------------------------------------------------------------------------------------
    // Stake screen
    // ------------------------------------------------------------------------------------------

    fun openStakeScreen() {
        stage = DuelStage.STAKE
        both { side ->
            side.accepted = false
            val player = side.player

            /*
             * Open first, configure second. `interface_inv_init_big` sets up the inventory that
             * lives on 336, and running it before the client has been told to open 336 leaves it
             * with nothing to configure. The trade screen this was modelled on runs the script
             * first, which is the other half of why its offer options never appeared.
             */
            player.openInterface(STAKE_OVERLAY_INTERFACE, InterfaceDestination.TAB_AREA)
            player.openInterface(STAKE_INTERFACE, InterfaceDestination.MAIN_SCREEN)

            player.setComponentText(STAKE_INTERFACE, 31, "Challenging: ${other(player).player.username}")
            player.sendItemContainer(key = STAKE_INVENTORY_KEY, container = side.inventory)
            player.runClientScript(
                INTERFACE_INV_INIT_BIG,
                STAKE_OVERLAY_INTERFACE.getInterfaceHash(),
                STAKE_INVENTORY_KEY,
                4,
                7,
                0,
                -1,
                "Stake",
                "Stake-5",
                "Stake-10",
                "Stake-All",
                "Stake-X",
                "",
                "",
                "",
                "",
            )
            player.setInterfaceEvents(
                interfaceId = STAKE_OVERLAY_INTERFACE,
                component = 0,
                range = 0..side.stake.capacity,
                setting = 1086,
            )
            player.setInterfaceEvents(
                interfaceId = STAKE_INTERFACE,
                component = PLAYER_STAKE_CHILD,
                range = 0..side.stake.capacity,
                setting = 1086,
            )
            player.setInterfaceEvents(
                interfaceId = STAKE_INTERFACE,
                component = OTHER_STAKE_CHILD,
                range = 0..side.stake.capacity,
                setting = 1024,
            )
        }
        refreshStake()
    }

    /**
     * Moves [amount] of the item in [slot] out of the working inventory and into the stake.
     */
    fun stake(
        player: Player,
        slot: Int,
        amount: Int,
    ) {
        if (stage != DuelStage.STAKE) return
        val side = sideOf(player)
        val item = side.inventory[slot] ?: return
        val count = minOf(amount, side.inventory.getItemCount(item.id))
        if (count <= 0) return

        if (side.inventory.remove(item.id, count, assureFullRemoval = true, beginSlot = slot).hasSucceeded()) {
            side.stake.add(item.id, count)
        }
        touch()
        refreshStake()
    }

    /**
     * Pulls [amount] of the item in stake [slot] back into the working inventory.
     */
    fun unstake(
        player: Player,
        slot: Int,
        amount: Int,
    ) {
        if (stage != DuelStage.STAKE) return
        val side = sideOf(player)
        val item = side.stake[slot] ?: return
        val count = minOf(amount, side.stake.getItemCount(item.id))
        if (count <= 0) return

        if (side.stake.remove(item.id, count, assureFullRemoval = true).hasSucceeded()) {
            side.inventory.add(item.id, count)
            side.stake.shift()
        }
        touch()
        refreshStake()
    }

    private fun refreshStake() {
        both { side ->
            val player = side.player
            val opponent = other(player)
            player.sendItemContainer(STAKE_INVENTORY_KEY, side.inventory)
            player.sendItemContainer(PLAYER_STAKE_KEY, side.stake)
            player.sendItemContainerOther(PLAYER_STAKE_KEY, opponent.stake)

            player.setComponentText(
                STAKE_INTERFACE,
                24,
                "Your stake:<br>(Value: <col=FFFFFF>${value(side.stake).decimalFormat()}</col> coins)",
            )
            player.setComponentText(
                STAKE_INTERFACE,
                27,
                "${opponent.player.username}'s stake:<br>(Value: <col=FFFFFF>${value(opponent.stake).decimalFormat()}</col> coins)",
            )
            player.setComponentText(
                STAKE_INTERFACE,
                9,
                "${opponent.player.username} has ${opponent.inventory.freeSlotCount} free inventory slots.",
            )
            player.setComponentText(STAKE_INTERFACE, 30, statusText(side))
        }
    }

    private fun value(container: ItemContainer): Int =
        container.rawItems.filterNotNull().sumOf { item ->
            (priceService?.get(item.id) ?: item.getDef().cost ?: 0) * item.amount
        }

    private fun statusText(side: DuelSide): String =
        when {
            side.accepted -> "Waiting for other player..."
            other(side.player).accepted -> "Other player has accepted."
            else -> ""
        }

    // ------------------------------------------------------------------------------------------
    // Options screen
    // ------------------------------------------------------------------------------------------

    fun openOptionsScreen() {
        stage = DuelStage.OPTIONS
        both { side ->
            side.accepted = false
            val player = side.player
            player.closeInterface(STAKE_OVERLAY_INTERFACE)
            player.openInterface(DuelArena.OPTIONS_INTERFACE, InterfaceDestination.MAIN_SCREEN)
            // Build the twelve rule rows one at a time - see SCRIPT_BUILD_ROW for why the screen's
            // own init script is not used. 6169 also ends by installing a var-transmit listener so
            // the client redraws itself when varp 286 changes; we do not need it, because every
            // change goes through refreshOptions, which redraws explicitly.
            DuelRule.BUILD_ORDER.forEach { rule ->
                player.runClientScript(
                    DuelArena.SCRIPT_BUILD_ROW,
                    rule.bit,
                    rule.title,
                    DuelArena.OPTIONS_INTERFACE.getInterfaceHash(rule.component),
                    rule.description,
                )
            }
            player.setComponentText(
                DuelArena.OPTIONS_INTERFACE,
                1,
                "Duel Options - ${other(player).player.username}",
            )
            /*
             * The click has to be enabled twice over. `[clientscript,6172]` already handles the op
             * on the client - it flips its own copy of var 286 and redraws - but that is only a
             * local mirror, and nothing carries it back here. The op only *also* reaches the server
             * if the server enables it, which is what this does; the server then redoes the same
             * arithmetic and stays the authority on what the rules actually are.
             */
            DuelRule.values.forEach { rule ->
                player.setInterfaceEvents(
                    interfaceId = DuelArena.OPTIONS_INTERFACE,
                    component = rule.component,
                    range = 0..0,
                    setting = InterfaceEvent.ClickOp1,
                )
            }
            // The worn icons are sub-components duel_initworn built into 42; it gave them their
            // opbase text but left the ops themselves to us.
            player.setInterfaceEvents(
                interfaceId = DuelArena.OPTIONS_INTERFACE,
                component = WORN_ICON_COMPONENT,
                range = 0 until DuelSlot.values.size,
                setting = InterfaceEvent.ClickOp1,
            )
            listOf(
                OPTIONS_ACCEPT_TEXT, OPTIONS_BAR, OPTIONS_ACCEPT, OPTIONS_DECLINE,
                WHIP_PRESET, BOXING_PRESET,
            ).forEach { component ->
                player.setInterfaceEvents(
                    interfaceId = DuelArena.OPTIONS_INTERFACE,
                    component = component,
                    range = 0..0,
                    setting = InterfaceEvent.ClickOp1,
                )
            }
        }
        refreshOptions()
    }

    /**
     * Flips one rule. Toggling anything re-arms the accept delay and drops both acceptances, so a
     * rule can never be slipped in after agreement.
     */
    fun toggleRule(
        player: Player,
        rule: DuelRule,
    ) {
        if (stage != DuelStage.OPTIONS) return

        val bit = 1 shl rule.bit
        val enabling = (rules and bit) == 0

        // Refusing all three styles would leave a duel that cannot be won; the client does not
        // stop it, so the server does.
        if (enabling && rule in DuelRule.ATTACK_STYLE_RULES &&
            DuelRule.ATTACK_STYLE_RULES.all { it == rule || hasRule(it) }
        ) {
            player.message("You can't disable every method of attack.")
            // 6172 flipped the client's own copy of var 286 before we ever saw the click, so a
            // refusal has to push the real value back or their screen keeps the rule we rejected.
            player.setVarp(DuelArena.RULES_VARP, rules)
            return
        }

        rules = rules xor bit
        announceChange(rule.bit)
        refreshOptions()
    }

    /**
     * Locks or unlocks one worn slot.
     */
    fun toggleSlot(
        player: Player,
        duelSlot: DuelSlot,
    ) {
        if (stage != DuelStage.OPTIONS) return

        lockedSlots = lockedSlots xor (1 shl duelSlot.slot)
        announceChange(duelSlot.flagBit)
        refreshOptions()
    }

    fun applyPreset(preset: List<DuelRule>) {
        if (stage != DuelStage.OPTIONS) return
        rules = preset.fold(0) { acc, rule -> acc or (1 shl rule.bit) }
        lockedSlots = 0
        touch()
        refreshOptions()
    }

    private fun announceChange(flagBit: Int) {
        touch()
        both { it.player.runClientScript(DuelArena.SCRIPT_OPTION_CHANGED, flagBit) }
    }

    private fun refreshOptions() {
        both { side ->
            val player = side.player
            player.setVarp(DuelArena.RULES_VARP, rules)
            player.setVarbit(DuelArena.SLOTS_VARBIT, lockedSlots)
            // The worn icons are drawn from each player's own equipment, so this is per-side.
            player.runClientScript(DuelArena.SCRIPT_INIT_WORN)
            player.runClientScript(DuelArena.SCRIPT_OPTIONS_REFRESH)
            player.setComponentText(DuelArena.OPTIONS_INTERFACE, 83, statusText(side))
            armAcceptButton(player, DuelArena.OPTIONS_INTERFACE, OPTIONS_ACCEPT)
        }
    }

    // ------------------------------------------------------------------------------------------
    // Confirm screen
    // ------------------------------------------------------------------------------------------

    fun openConfirmScreen() {
        stage = DuelStage.CONFIRM
        both { side ->
            side.accepted = false
            val player = side.player
            val opponent = other(player)
            player.openInterface(DuelArena.CONFIRM_INTERFACE, InterfaceDestination.MAIN_SCREEN)
            // 6193 writes the whole summary from the flag word: rules in bits 0..13, locked worn
            // slots in 14..27, which is why the two are packed together rather than sent apart.
            player.runClientScript(
                DuelArena.SCRIPT_CONFIRM_TEXT,
                confirmFlags(),
                rules,
                lockedSlots,
                "${opponent.player.username} (level-${opponent.player.combatLevel})",
            )
            player.setComponentText(
                DuelArena.CONFIRM_INTERFACE,
                CONFIRM_SUMMARY_TEXT,
                stakeSummary(side, opponent),
            )
            listOf(CONFIRM_ACCEPT, CONFIRM_DECLINE).forEach { component ->
                player.setInterfaceEvents(
                    interfaceId = DuelArena.CONFIRM_INTERFACE,
                    component = component,
                    range = 0..0,
                    setting = InterfaceEvent.ClickOp1,
                )
            }
            armAcceptButton(player, DuelArena.CONFIRM_INTERFACE, CONFIRM_ACCEPT)
        }
    }

    /**
     * The flag word `[clientscript,6193]` reads: the rules where it expects them, and each locked
     * worn slot moved up into bits 14..27 so `getbit_range($flags, 14, 27)` sees them.
     */
    private fun confirmFlags(): Int {
        var flags = rules
        DuelSlot.values.forEach { duelSlot ->
            if (isSlotLocked(duelSlot.slot)) {
                flags = flags or (1 shl duelSlot.flagBit)
            }
        }
        return flags
    }

    private fun stakeSummary(
        side: DuelSide,
        opponent: DuelSide,
    ): String {
        val yours = side.stake.rawItems.filterNotNull()
        val theirs = opponent.stake.rawItems.filterNotNull()
        if (yours.isEmpty() && theirs.isEmpty()) {
            return "Nothing has been staked on this duel."
        }
        val youWin =
            if (theirs.isEmpty()) "nothing" else "${theirs.size} item(s) worth ${value(opponent.stake).decimalFormat()} coins"
        val youLose =
            if (yours.isEmpty()) "nothing" else "${yours.size} item(s) worth ${value(side.stake).decimalFormat()} coins"
        return "If you win you will receive $youWin.<br>If you lose you will forfeit $youLose."
    }

    // ------------------------------------------------------------------------------------------
    // Acceptance
    // ------------------------------------------------------------------------------------------

    /**
     * Marks [player] as having accepted the current screen, and moves the duel on once both have.
     */
    fun accept(player: Player) {
        val side = sideOf(player)
        if (side.accepted) return

        if (player.world.currentCycle < acceptableAt) {
            player.message("Wait a moment - the options have just changed.")
            return
        }

        if (stage == DuelStage.STAKE && !canCarryWinnings(player)) return

        side.accepted = true

        if (!sides.all { it.accepted }) {
            when (stage) {
                DuelStage.STAKE -> refreshStake()
                DuelStage.OPTIONS -> refreshOptions()
                DuelStage.CONFIRM ->
                    both { other ->
                        other.player.setComponentText(
                            DuelArena.CONFIRM_INTERFACE,
                            CONFIRM_SUMMARY_TEXT,
                            stakeSummary(other, other(other.player)) + "<br>" + statusText(other),
                        )
                    }
                else -> Unit
            }
            return
        }

        when (stage) {
            DuelStage.STAKE -> openOptionsScreen()
            DuelStage.OPTIONS -> openConfirmScreen()
            DuelStage.CONFIRM -> begin()
            else -> Unit
        }
    }

    /**
     * Whether [player] could actually hold everything they stand to win. Checked before the stake
     * is agreed rather than when it is paid out, so a duel never ends with prizes on the floor.
     */
    private fun canCarryWinnings(player: Player): Boolean {
        val side = sideOf(player)
        val prize = other(player).stake.rawItems.filterNotNull()
        // The player's own stake comes back to them too if they win, so it costs no extra room.
        val needed = prize.count { item -> !item.getDef().stackable || !side.inventory.contains(item.id) }
        if (side.inventory.freeSlotCount < needed) {
            player.message("You don't have enough inventory space to claim what you'd win.")
            return false
        }
        return true
    }

    /**
     * Re-arms the accept delay and drops both acceptances. Called for every change either player
     * makes to the stake or to the rules.
     */
    private fun touch() {
        acceptableAt = sides[0].player.world.currentCycle + DuelArena.ACCEPT_DELAY_TICKS
        both { it.accepted = false }
    }

    private fun armAcceptButton(
        player: Player,
        interfaceId: Int,
        component: Int,
    ) {
        // The client kills the button itself until clientclock reaches the value it is handed; the
        // server keeps the same deadline in [acceptableAt] rather than trusting that.
        player.runClientScript(
            DuelArena.SCRIPT_ACCEPT_BUTTON,
            interfaceId.getInterfaceHash(component),
            DuelArena.ACCEPT_DELAY_TICKS,
        )
    }

    // ------------------------------------------------------------------------------------------
    // The fight
    // ------------------------------------------------------------------------------------------

    /**
     * Commits the stake, claims an arena and puts both players in it.
     */
    private fun begin() {
        /*
         * Check both players can still pay before anything is claimed or taken.
         *
         * The stake screen works on a snapshot of the inventory, and the inventory tab is usable
         * again from the options screen onwards - so a staked item can be equipped, dropped or
         * otherwise moved before the duel starts. `begin` used to overwrite the real inventory with
         * that snapshot, which duplicated anything equipped in between: worn *and* restored. The
         * real inventory is the authority.
         */
        if (!canCollectStakes()) {
            both {
                it.accepted = false
                it.player.message("The duel was called off - a staked item is no longer in your inventory.")
            }
            return
        }

        val arena = DuelArenas.claim(this)
        if (arena == null) {
            // Both had accepted to get here; drop that so Accept works again once one frees up,
            // rather than leaving them staring at a confirm screen that does nothing.
            both {
                it.accepted = false
                it.player.message("All of the arenas are currently in use. Please wait a moment.")
            }
            return
        }
        plot = arena
        stage = DuelStage.COUNTDOWN

        val adjacent = hasRule(DuelRule.NO_MOVEMENT)
        val (firstTile, secondTile) = arena.startTiles(adjacent)
        val tiles = listOf(firstTile, secondTile)

        collectStakes()

        sides.forEachIndexed { index, side ->
            val player = side.player

            player.closeInterface(InterfaceDestination.MAIN_SCREEN)
            player.closeInterface(STAKE_OVERLAY_INTERFACE)

            stripLockedSlots(player)
            applyStartingConditions(player)

            player.stopMovement()
            player.moveTo(tiles[index])
            player.lock()
            player.message("You are about to duel ${other(player).player.username}!")

            player.attr[DuelArena.COUNTDOWN_LEFT] = DuelArena.COUNTDOWN_TICKS
            player.timers[DuelArena.COUNTDOWN_TIMER] = 1
        }

        sides.forEachIndexed { index, side -> side.player.faceTile(tiles[1 - index]) }
    }

    /**
     * Removes both players' staked items from their real inventories, all or nothing.
     *
     * Checked in full before anything is taken, so a player who has moved a staked item since
     * offering it cancels the duel rather than leaving the other one short.
     */
    /** What each player still owes the pot, totalled per item id. */
    private fun owedStakes(): Map<DuelSide, Map<Int, Int>> =
        sides.associateWith { side ->
            val totals = HashMap<Int, Int>()
            side.stake.rawItems.filterNotNull().forEach { item ->
                totals[item.id] = (totals[item.id] ?: 0) + item.amount
            }
            totals
        }

    /** Whether both players still hold everything they offered. */
    private fun canCollectStakes(): Boolean =
        owedStakes().all { (side, totals) ->
            totals.all { (id, amount) -> side.player.inventory.getItemCount(id) >= amount }
        }

    /** Takes both stakes out of the players' real inventories. Guarded by [canCollectStakes]. */
    private fun collectStakes() {
        owedStakes().forEach { (side, totals) ->
            totals.forEach { (id, amount) ->
                side.player.inventory.remove(id, amount, assureFullRemoval = true)
            }
        }
    }

    /**
     * Takes off anything worn in a slot this duel has locked.
     */
    private fun stripLockedSlots(player: Player) {
        val stripped = mutableListOf<String>()
        DuelSlot.values.forEach { duelSlot ->
            if (!isSlotLocked(duelSlot.slot)) return@forEach
            val worn = player.equipment[duelSlot.slot] ?: return@forEach
            stripped += duelSlot.label
            if (player.inventory.add(worn).hasSucceeded()) {
                player.equipment[duelSlot.slot] = null
            } else {
                // Nowhere to put it: drop it at their feet rather than silently keeping it on and
                // letting them fight in equipment the rules forbid.
                player.world.spawn(GroundItem(worn, player.tile, player))
                player.equipment[duelSlot.slot] = null
                player.message("Your ${duelSlot.label} was dropped - you had no room for it.")
            }
        }
        /*
         * Say so. The locked slots are shown on the options screen as a red overlay and the confirm
         * screen says "Some worn items will be taken off", but both are easy to miss - and a stray
         * click on the worn-icon panel locks a slot silently. Gear vanishing out of your equipment
         * with no explanation reads as the server losing it.
         */
        if (stripped.isNotEmpty()) {
            player.message("This duel locks your ${stripped.joinToString(", ")} - taken off and put in your inventory.")
        }
    }

    /**
     * The "Before the duel starts" half of the confirm screen, applied for real.
     */
    private fun applyStartingConditions(player: Player) {
        if (hasRule(DuelRule.NO_DRINKS) || hasRule(DuelRule.NO_SPECIAL_ATTACKS)) {
            player.getSkills().restoreAll()
        }
        if (hasRule(DuelRule.NO_PRAYER)) {
            DuelRules.stopAllPrayers(player)
        }
        // Both players start on full health and free of poison, the way the arena always healed
        // people on the way in.
        heal(player)
    }

    /**
     * Called by the plugin's countdown task once the arena has stopped counting.
     */
    fun startFighting() {
        if (stage != DuelStage.COUNTDOWN) return
        stage = DuelStage.FIGHTING
        both { side ->
            val player = side.player
            player.unlock()
            player.sendOption("Attack", DuelArena.ATTACK_OPTION_SLOT, leftClick = true)
            player.message("FIGHT!")
            if (hasRule(DuelRule.NO_MOVEMENT)) {
                // Held with the freeze timer rather than a lock, because a locked player cannot
                // attack either and a No Movement duel is still very much a fight.
                player.timers[FROZEN_TIMER] = NO_MOVEMENT_TICKS
            }
            if (hasRule(DuelRule.SHOW_INVENTORIES)) {
                player.sendItemContainerOther(STAKE_INVENTORY_KEY, other(player).player.inventory)
            }
            player.timers[DuelArena.WATCH_TIMER] = 1
        }
    }

    /**
     * Called every tick for each fighting player. Handles the one thing that cannot be handled at
     * the point it happens: walking out of the arena.
     */
    fun watch(player: Player) {
        if (!isFighting()) return
        val arena = plot ?: return
        if (arena.contains(player.tile)) return

        if (hasRule(DuelRule.NO_FORFEIT)) {
            // Nothing to enforce it at the doorway, so they are simply put back.
            player.message("You can't leave this duel.")
            val (first, second) = arena.startTiles(hasRule(DuelRule.NO_MOVEMENT))
            player.moveTo(if (sides.first().player == player) first else second)
            return
        }
        finish(other(player).player, "You forfeited the duel.")
    }

    // ------------------------------------------------------------------------------------------
    // Endings
    // ------------------------------------------------------------------------------------------

    /**
     * Ends the duel with a winner, handing them both stakes.
     */
    fun finish(
        winner: Player,
        loserMessage: String,
        teleportLoser: Boolean = true,
    ) {
        if (stage == DuelStage.ENDED) return
        stage = DuelStage.ENDED

        val loser = other(winner).player
        val prize = sides.flatMap { it.stake.rawItems.filterNotNull() }

        both { side ->
            val player = side.player
            val dying = !teleportLoser && player == loser
            release(player, unlock = !dying)
            if (!dying) {
                player.moveTo(DuelArena.LOBBY_TILE)
                player.getSkills().restoreAll()
                heal(player)
                player.refreshChallengeOption()
            } else {
                // Killed rather than beaten: they are part way through dying, so they are left
                // where they fell and collected once the animation has run.
                player.attr[DUEL_AWAITING_RETURN_ATTR] = true
            }
            side.stake.removeAll()
            side.inventory.removeAll()
        }

        prize.forEach { item -> give(winner, item) }

        winner.message("You have won the duel!")
        if (prize.isNotEmpty()) {
            winner.message("You claim ${prize.size} staked item(s).")
        }
        loser.message(loserMessage)

        DuelArenas.release(this)
    }

    /**
     * Ends the duel with no winner, returning each player's own stake. Used when someone declines,
     * closes a screen, or leaves before the fight has started.
     */
    fun abort(
        reason: String = "The duel has been called off.",
        notify: Boolean = true,
    ) {
        if (stage == DuelStage.ENDED) return
        val wasCommitted = isCommitted()
        stage = DuelStage.ENDED

        both { side ->
            val player = side.player
            release(player)

            if (wasCommitted) {
                // Their working inventory is already theirs; only the stake is outstanding.
                player.moveTo(DuelArena.LOBBY_TILE)
                heal(player)
                player.refreshChallengeOption()
                side.stake.rawItems.filterNotNull().forEach { item -> give(player, item) }
            } else {
                // Still negotiating: the real inventory was never touched, so the working copies
                // are simply discarded rather than given back.
                player.closeInterface(STAKE_OVERLAY_INTERFACE)
            }

            side.stake.removeAll()
            side.inventory.removeAll()
            if (notify) player.message(reason)
        }

        DuelArenas.release(this)
    }

    private fun give(
        player: Player,
        item: org.alter.game.model.item.Item,
    ) {
        if (!player.inventory.add(item).hasSucceeded()) {
            player.world.spawn(GroundItem(item, player.tile, player))
        }
    }

    private fun heal(player: Player) {
        player.getSkills().setCurrentLevel(Skills.HITPOINTS, player.getSkills().getBaseLevel(Skills.HITPOINTS))
        player.timers.remove(POISON_TIMER)
        player.attr.remove(POISON_TICKS_LEFT_ATTR)
    }

    /**
     * Undoes everything [begin] and [startFighting] did to a player's own state, and detaches the
     * session so nothing else can act on it.
     */
    private fun release(
        player: Player,
        unlock: Boolean = true,
    ) {
        player.removeOption(DuelArena.ATTACK_OPTION_SLOT)
        player.timers.remove(FROZEN_TIMER)
        player.timers.remove(DuelArena.COUNTDOWN_TIMER)
        player.timers.remove(DuelArena.WATCH_TIMER)
        player.attr.remove(DuelArena.COUNTDOWN_LEFT)
        /*
         * A player who is dying is part way through the death sequence, which locked them and will
         * unlock them itself. Unlocking here would hand them those few ticks back and let them act
         * after the killing blow.
         */
        if (unlock) player.unlock()
        player.setVarp(DuelArena.RULES_VARP, 0)
        player.setVarbit(DuelArena.SLOTS_VARBIT, 0)
        player.closeInterface(InterfaceDestination.MAIN_SCREEN)
        player.attr.remove(DUEL_SESSION_ATTR)
    }

    companion object {
        /**
         * The stake runs on the trade interfaces. Nothing in this cache offers a two-sided item
         * wager any more, and 335/336 already do exactly that job - two offers, both values, an
         * accept on each side - so they are relabelled rather than reinvented.
         */
        const val STAKE_INTERFACE = 335
        const val STAKE_OVERLAY_INTERFACE = 336

        const val PLAYER_STAKE_CHILD = 25
        const val OTHER_STAKE_CHILD = 28

        const val STAKE_INVENTORY_KEY = 93
        const val PLAYER_STAKE_KEY = 90

        /** Options screen: the worn-icon host, the presets and Accept. */
        const val WORN_ICON_COMPONENT = 42

        /**
         * The bottom bar's two button layers, 85 being the bar they sit in and 84 the text drawn
         * over it. Nothing in the cache records which half is which - as with the Grand Exchange's
         * panels the roles are the server's to assign - so this was settled by binding the whole bar
         * and seeing which component a click on Accept actually came from: 86, the left one.
         */
        const val OPTIONS_ACCEPT = 86
        const val OPTIONS_DECLINE = 87
        const val OPTIONS_BAR = 85
        const val OPTIONS_ACCEPT_TEXT = 84
        const val WHIP_PRESET = 94
        const val BOXING_PRESET = 96

        /**
         * Confirm screen. Same shape as the options screen's bar - two ~76px layers pinned to the
         * bottom of panel 8 - but the halves are the other way round: here Accept is the *right*
         * one (51) where on the options screen it is the left one (86). Both were settled by
         * watching which component a click on Accept actually reported, twice each; there is
         * nothing in the cache that says which is which. (31 and 33 look plausible in a component
         * listing but are a text field and a graphic, so binding those did nothing at all.)
         */
        const val CONFIRM_ACCEPT = 51
        const val CONFIRM_DECLINE = 50

        /** The confirm screen's free text area, under the summary 6193 builds into 756:53. */
        const val CONFIRM_SUMMARY_TEXT = 49

        /**
         * Long enough that no duel outlives it. The timer is cleared when the duel ends, so the
         * only thing this number decides is what happens if that somehow does not run.
         */
        private const val NO_MOVEMENT_TICKS = 10_000
    }
}

/**
 * One player's half of a duel.
 */
class DuelSide(val player: Player) {
    /**
     * A working copy of the inventory, so that nothing the player owns actually moves until the
     * duel begins. Same trick the trade screen uses, and for the same reason: a duel that falls
     * apart half way through should leave no trace.
     */
    val inventory = ItemContainer(player.inventory)

    /** What this player has put up. */
    val stake = ItemContainer(player.inventory.capacity, ContainerStackType.NORMAL)

    var accepted: Boolean = false
}
