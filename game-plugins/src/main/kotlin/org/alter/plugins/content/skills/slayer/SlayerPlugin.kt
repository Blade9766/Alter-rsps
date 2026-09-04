package org.alter.plugins.content.skills.slayer

import dev.openrune.cache.CacheManager
import gg.rsmod.util.ServerProperties
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.shop.PurchasePolicy
import org.alter.game.model.shop.ShopItem
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.mechanics.shops.CoinCurrency
import org.alter.rscm.RSCM.getRSCM

/**
 * Slayer: the six standard masters, their assignment tables, the kill counter, the reward points,
 * and the shop and rewards menu that hang off them.
 *
 * The skill splits the same way the other skills here do - [SlayerData] is the shape of the
 * configuration, [SlayerService] loads and resolves it, [Slayer] holds the state and the rules, and
 * this file is the wiring. [SlayerRewards] carries the points menu, which is long enough to deserve
 * its own file.
 *
 * ## What this fixes on the way past
 *
 * Two things in the existing combat code were already reaching for Slayer and finding nothing:
 *
 * - **`slayerData { xp = ... }` was write-only.** Monsters across `content/npcs` declare Slayer
 *   experience and `NpcCombatDef` carries it, but nothing in the engine ever read the field, so
 *   Slayer experience was unobtainable. [Slayer.onKill] reads it now.
 * - **The black mask and slayer helmet bonus applied to everything.** The melee formula's own
 *   comment said so - "This should only apply if you have the target || his category as a Slayer
 *   Task". It is gated on [Slayer.isOnTask] now, in all three combat formulas.
 *
 * The Slayer *level* requirement on a monster was already enforced, in `Combat.canEngage`.
 *
 * ## Masters
 *
 * None of the six were spawned anywhere before this, so this plugin spawns them, each at the
 * coordinates on its own wiki page. Five of the six stand in regions no area plugin has been built
 * for yet - Burthorpe, Canifis, Zanaris, the Tree Gnome Stronghold, Shilo Village - which affects
 * nothing here: the map data is in the cache regardless of whether a content plugin claims the
 * region, and a player who walks there finds the master waiting.
 *
 * The cache holds several npc ids per master and only one of them carries the real
 * `Assignment`/`Trade`/`Rewards` options; `masters.json` names that one. The others are combat or
 * cutscene variants - Turael 13433 and 13434, for instance, are the level 154 versions with no
 * options at all - and binding to those would have produced a master nobody could talk to.
 *
 * ## Deliberately not the real rewards interface
 *
 * OSRS renders the rewards shop on interface 426, which is driven almost entirely by clientscripts
 * this server does not run. Rather than open an interface that would come up empty, the rewards menu
 * is built from ordinary dialogue options, the same way every other menu in this codebase is. The
 * *state* behind it is real - unlocks, blocks and points are all persisted and all mirrored into the
 * client's own Slayer varbits - so an interface can be swapped in later without touching [Slayer].
 */
class SlayerPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    /**
     * Loaded here, during construction, rather than left to the repository.
     *
     * The masters have to be spawned while this plugin is being built - `spawnEntities` runs before
     * any service's `init` - so the configuration has to be readable now. [SlayerService.init] is
     * idempotent for exactly this reason: the repository calls it again a moment later and it does
     * nothing.
     */
    private val service = SlayerService().also { it.init(server, world, ServerProperties()) }

    init {
        loadService(service)

        onWorldInit { service.markAvailable(world) }

        createSlayerShop()
        bindMasters()

        /*
         * Credit every kill, not just kills on npcs this plugin knows about: whether the npc counts
         * is Slayer's own question and [Slayer.onKill] answers it. Bound on death rather than on
         * pre-death so the kill is only credited once the npc is actually gone.
         */
        onAnyNpcDeath {
            val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return@onAnyNpcDeath
            Slayer.onKill(killer, npc)
        }

        /*
         * The client's Slayer varps are not persisted, so a returning player's task has to be
         * pushed back into them or their interface shows an empty assignment they demonstrably have.
         */
        onLogin { Slayer.syncInterface(player) }

        bindEnchantedGem()
    }

    /**
     * Spawn each master and bind their four options.
     *
     * `walkRadius = 0`: every one of them stands still in the real game.
     */
    private fun bindMasters() {
        service.masters.forEach { master ->
            val primary = master.npcs.first()

            spawnNpc(
                npc = primary,
                x = master.x,
                z = master.z,
                height = master.height,
                walkRadius = 0,
                direction = Direction.SOUTH,
            )

            onNpcOption(primary, option = "talk-to") { player.queue { talkTo(this, player, master) } }
            onNpcOption(primary, option = "assignment") { player.queue { assignment(this, player, master) } }
            onNpcOption(primary, option = "trade") { player.openShop(SHOP_NAME) }
            onNpcOption(primary, option = "rewards") {
                player.queue { SlayerRewards.open(this, player, master) }
            }
        }
    }

    /**
     * The masters' shared Slayer Equipment shop.
     *
     * Stock and quantities are the wiki's shop table; prices are computed from this cache's own
     * `ItemType.cost` against that table's `sellmultiplier=1000|buymultiplier=600` - so the shop
     * sells at the item's base value and buys back at 60% of it, exactly as the other stores in this
     * codebase derive their prices.
     *
     * Items tied to content that does not exist here yet are still stocked. They are cheap, they are
     * the real stock list, and several of them (nose peg, earmuffs, facemask, spiny helmet) are
     * required equipment the moment their monsters are built - having them already purchasable is
     * the correct starting state, not a gap.
     */
    private fun createSlayerShop() {
        val stock =
            listOf(
                "item.enchanted_gem" to 50,
                "item.mirror_shield" to 100,
                "item.leafbladed_spear" to 50,
                "item.broad_arrows" to 50000,
                "item.bag_of_salt" to 5000,
                "item.rock_hammer" to 50,
                "item.facemask" to 50,
                "item.earmuffs" to 50,
                "item.nose_peg" to 50,
                "item.slayers_staff" to 50,
                "item.spiny_helmet" to 50,
                "item.fishing_explosive" to 5000,
                "item.ice_cooler" to 5000,
                "item.slayer_gloves" to 50,
                "item.unlit_bug_lantern" to 50,
                "item.insulated_boots" to 50,
                "item.fungicide_spray_10" to 50,
                "item.fungicide" to 5000,
                "item.witchwood_icon" to 50,
                "item.slayer_bell" to 50,
                "item.broad_arrowheads" to 3000,
                "item.unfinished_broad_bolts" to 5000,
                "item.rock_thrownhammer" to 5000,
                "item.boots_of_stone" to 50,
            )

        createShop(SHOP_NAME, CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_STOCK) {
            stock.forEachIndexed { index, (key, amount) ->
                val id = getRSCM(key)
                val cost = CacheManager.getItem(id).cost
                items[index] = ShopItem(id, amount, sellPrice = cost, buyPrice = cost * 6 / 10)
            }
        }
    }

    /**
     * The enchanted gem's inventory options.
     *
     * "Check" is the useful one and the reason the gem exists - it reads out the current assignment
     * without walking back to a master. "Activate" contacts a master in the real game, which needs a
     * two-way dialogue with an npc who is not there; it reports the assignment instead of pretending
     * to open a channel. "Partner" and "Log" belong to Slayer partners and the kill log, neither of
     * which is built, so they are left unbound rather than stubbed.
     */
    private fun bindEnchantedGem() {
        listOf("Check", "Activate").forEach { option ->
            onItemOption(item = "item.enchanted_gem", option = option) {
                player.queue { reportTask(this, player) }
            }
        }

        onItemOption(item = "item.slayer_helmet", option = "Check") {
            player.queue { reportTask(this, player) }
        }
    }

    private suspend fun reportTask(
        task: QueueTask,
        player: Player,
    ) {
        val assignment = Slayer.taskName(player)
        val left = Slayer.amount(player)

        if (assignment == null || left <= 0) {
            task.chatPlayer(player, "I don't have a Slayer assignment at the moment.")
            return
        }

        val master = Slayer.masterName(player) ?: "a Slayer master"
        task.chatPlayer(
            player,
            "You're assigned to kill $left ${assignment.lowercase()}.<br>" +
                "The task was given to you by $master.",
        )
    }

    // ------------------------------------------------------------- dialogues

    private suspend fun talkTo(
        task: QueueTask,
        player: Player,
        master: SlayerMasterEntry,
    ) {
        task.chatNpc(player, "'Ello, and what are you after then?")

        when (
            task.options(
                player,
                "I need another assignment.",
                "Do you have anything for trade?",
                "Have you any rewards for me?",
                "Er... nothing...",
            )
        ) {
            1 -> {
                task.chatPlayer(player, "I need another assignment.")
                assignment(task, player, master)
            }

            2 -> {
                task.chatPlayer(player, "Do you have anything for trade?")
                task.chatNpc(player, "Of course. I've a fine stock of Slayer equipment.")
                player.openShop(SHOP_NAME)
            }

            3 -> {
                task.chatPlayer(player, "Have you any rewards for me?")
                SlayerRewards.open(task, player, master)
            }

            4 -> task.chatPlayer(player, "Er... nothing...")
        }
    }

    /**
     * The "Assignment" option: report the current task, or roll a new one.
     *
     * Holding an unfinished task normally means the master sends the player away - except at Turael,
     * who will take it off their hands and replace it with something easy. That is the whole of
     * "Turael skipping", and the player is warned what it costs before it happens, because it is
     * paid for out of a streak they cannot get back.
     */
    private suspend fun assignment(
        task: QueueTask,
        player: Player,
        master: SlayerMasterEntry,
    ) {
        if (Slayer.amount(player) > 0) {
            val name = Slayer.taskName(player)!!

            if (!Slayer.isTuraelSkip(player, master)) {
                task.chatNpc(
                    player,
                    "You're still hunting ${name.lowercase()}; you have ${Slayer.amount(player)} to go.<br>" +
                        "Come back when you're done.",
                )
                return
            }

            task.chatNpc(
                player,
                "You're hunting ${name.lowercase()} for ${Slayer.masterName(player)}. I can<br>" +
                    "give you something easier, but you'll lose your<br>task streak of ${Slayer.streak(player)}.",
            )

            if (task.options(player, "Give me a new task.", "No, I'll finish this one.") != 1) {
                task.chatPlayer(player, "No, I'll finish this one.")
                return
            }
        }

        when (val result = Slayer.assign(player, master)) {
            is Slayer.AssignResult.CombatTooLow ->
                task.chatNpc(
                    player,
                    "You're not a high enough level to get an assignment<br>" +
                        "from me. Come back when you're combat level ${result.required}.",
                )

            is Slayer.AssignResult.SlayerTooLow ->
                task.chatNpc(
                    player,
                    "You'll need ${result.required} Slayer before I'll give you<br>an assignment.",
                )

            is Slayer.AssignResult.NothingSuitable ->
                task.chatNpc(
                    player,
                    "I've nothing suitable for you at the moment. Try<br>" +
                        "unblocking something, or come back when you're<br>stronger.",
                )

            is Slayer.AssignResult.Assigned -> {
                task.chatNpc(
                    player,
                    "Your new task is to kill ${result.amount} ${result.task.name.lowercase()}.",
                )
                player.message("You have been assigned ${result.amount} ${result.task.name.lowercase()}.")
            }
        }
    }

    private companion object {
        const val SHOP_NAME = "Slayer Equipment"
    }
}
