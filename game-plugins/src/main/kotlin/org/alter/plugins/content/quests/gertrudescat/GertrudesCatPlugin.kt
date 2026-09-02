package org.alter.plugins.content.quests.gertrudescat

import org.alter.api.Skills
import org.alter.api.ext.chatNpc
import org.alter.api.ext.chatPlayer
import org.alter.api.ext.message
import org.alter.api.ext.messageBox
import org.alter.api.ext.options
import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.model.attr.INTERACTING_NPC_ATTR
import org.alter.game.model.entity.GroundItem
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.model.move.moveTo
import org.alter.game.model.queue.QueueTask
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.quests.Quests
import org.alter.plugins.content.quests.completeQuest
import org.alter.plugins.content.quests.hasStartedQuest
import org.alter.plugins.content.quests.isQuestComplete
import org.alter.plugins.content.quests.questStage
import org.alter.plugins.content.quests.setQuestStage

/**
 * Gertrude's Cat: the spawns, the dialogue and the world interactions.
 *
 * [GertrudesCat] holds the data and the sourcing notes; this file is the wiring. The quest is
 * registered here, in the constructor, so the roster is complete before anyone can log in.
 *
 * ## The two bits of the world this had to switch on
 *
 * The Lumber Yard was scenery. Dumping region 13110's locations turned up the two objects the quest
 * walks through, neither of which was bound to anything:
 *
 * - the **broken fence** (2618, at 3308,3492) - the only way into the yard, `Climb-over`;
 * - the yard's **ladder** (11794 up at 3310,3509 / 11795 down at 3310,3509 on the floor above).
 *
 * The ladder is handled by `content/objects/ladder/LadderPlugin`, which works off a hardcoded id
 * list that these two were missing from; they have been added there rather than duplicated here,
 * because they are ordinary vertical ladders with nothing quest-specific about them. The fence is
 * bound below, since it exists for this quest.
 */
class GertrudesCatPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    private val quest = GertrudesCat.QUEST

    init {
        Quests.register(quest)

        spawns()
        bindGertrude()
        bindSons()
        bindFluffs()
        bindCrates()
        bindBrokenFence()
        bindSeasoning()
    }

    /**
     * Everyone and everything the quest needs standing where the wiki puts them.
     *
     * Gertrude is spawned as npc 7284, her before-quest id, and stays that id forever. The real game
     * swaps her for 7723 - same name and appearance, but with a `Kitten` option for buying more
     * kittens afterwards - and that swap is per player, which this engine can only do through a
     * transform driven by the npc definition's own varp/varbit. 7284 declares neither, so there is
     * nothing to drive. Buying kittens is therefore a branch of her ordinary dialogue instead; see
     * [gertrudeAfterQuest].
     */
    private fun spawns() {
        /*
         * (3151,3410), not the wiki's (3151,3409). Her page marks the house with
         * `mtype=square|r=3` - an area, not a tile - and its centre lands squarely inside the
         * kitchen Table (object 2998 at 3150,3409, size 2x1, impenetrable), which covers both
         * (3150,3409) and (3151,3409). She stood in the furniture.
         *
         * (3151,3410) is the tile directly north of that table: floored, free of solid scenery (the
         * only loc on it is 11664, a type-22 floor decoration), and on the straight line in from her
         * north door at (3151,3412), so she faces anyone who walks in.
         */
        spawnNpc(npc = "npc.gertrude", x = 3151, z = 3410, walkRadius = 0, direction = Direction.SOUTH)

        /*
         * Wilough and Shilop are pinned on the wiki with a *square* marker - centre (3220,3435),
         * radius 4 - which is an area rather than a tile, so these are two tiles inside that square
         * by the Varrock Square newsstand, with a walk radius to match the marker. They wander in
         * the real game too.
         */
        spawnNpc(npc = "npc.wilough", x = 3220, z = 3435, walkRadius = 4, direction = Direction.SOUTH)
        spawnNpc(npc = "npc.shilop", x = 3218, z = 3433, walkRadius = 4, direction = Direction.SOUTH)

        spawnNpc(
            npc = "npc.gertrudes_cat_3497",
            tile = GertrudesCat.FLUFFS_TILE,
            walkRadius = 0,
            direction = Direction.SOUTH,
        )

        GertrudesCat.CRATE_TILES.forEach { tile ->
            spawnNpc(npc = "npc.crate_3499", tile = tile, walkRadius = 0, direction = Direction.SOUTH)
        }

        /*
         * The doogle leaves behind Gertrude's house, from the Doogle leaves page's own
         * ItemSpawnLine. Left on the default respawn, as every other ground spawn in this codebase
         * is.
         */
        spawnItem(item = "item.doogle_leaves", amount = 1, x = 3151, z = 3399)
        spawnItem(item = "item.doogle_leaves", amount = 1, x = 3152, z = 3399)
        spawnItem(item = "item.doogle_leaves", amount = 1, x = 3152, z = 3401)
        spawnItem(item = "item.doogle_leaves", amount = 1, x = 3156, z = 3401)
        spawnItem(item = "item.doogle_leaves", amount = 1, x = 3157, z = 3400)
    }

    // ------------------------------------------------------------------ Gertrude

    private fun bindGertrude() {
        onNpcOption("npc.gertrude", option = "talk-to") {
            player.queue {
                when {
                    player.isQuestComplete(quest) -> gertrudeAfterQuest(player)
                    player.questStage(quest) >= GertrudesCat.REUNITED -> gertrudeFinish(player)
                    player.questStage(quest) >= GertrudesCat.CAT_HAD_SARDINE -> gertrudeAfterSardine(player)
                    player.questStage(quest) >= GertrudesCat.CAT_HAD_MILK -> gertrudeSardineHint(player)
                    player.questStage(quest) >= GertrudesCat.KNOWS_LOCATION -> gertrudeAfterSons(player)
                    player.hasStartedQuest(quest) -> gertrudeStillLooking(player)
                    else -> gertrudeStart(player)
                }
            }
        }
    }

    private suspend fun QueueTask.gertrudeStart(player: Player) {
        chatPlayer(player, "Hello, are you okay?")
        chatNpc(player, "Do I look okay? Those kids drive me crazy.")
        chatNpc(player, "I'm sorry. It's just that I've lost her.")
        chatPlayer(player, "Lost who?")
        chatNpc(player, "Fluffs, poor Fluffs. She never hurt anyone.")
        chatPlayer(player, "Who's Fluffs?")
        chatNpc(
            player,
            "My beloved feline friend Fluffs. She's been purring by my side<br>" +
                "for almost a decade. Please, could you go search for her<br>" +
                "while I look over the kids?",
        )

        when (options(player, "Yes.", "No.")) {
            1 -> {
                chatPlayer(player, "Well, I suppose I could.")
                chatNpc(player, "Really? Thank you so much! I really have no idea where<br>she could be!")
                chatNpc(
                    player,
                    "I think my sons, Shilop and Wilough, saw the cat last.<br>They'll be out in the market place.",
                )
                chatPlayer(player, "Alright then, I'll see what I can do.")
                player.setQuestStage(quest, GertrudesCat.STARTED)
                player.message("<col=8b0000>You have started the ${quest.name} quest.</col>")
            }

            2 -> {
                chatPlayer(player, "Sorry, I'm too busy to play pet rescue.")
                chatNpc(player, "Well, okay then. I'll have to find someone else.")
            }
        }
    }

    private suspend fun QueueTask.gertrudeStillLooking(player: Player) {
        chatPlayer(player, "Hello Gertrude.")
        chatNpc(player, "Have you seen my poor Fluffs?")
        chatPlayer(player, "I'm afraid not.")
        chatNpc(player, "What about Shilop?")
        chatPlayer(player, "No sign of him either.")
        chatNpc(player, "Hmmm...strange, he should be at the market.")
    }

    private suspend fun QueueTask.gertrudeAfterSons(player: Player) {
        chatPlayer(player, "Hello Gertrude.")
        chatNpc(player, "Hello again, did you manage to find Shilop? I can't keep<br>an eye on him for the life of me.")
        chatPlayer(player, "He does seem quite a handful.")
        chatNpc(player, "You have no idea! Did he help at all?")
        chatPlayer(player, "I think so, I'm just going to look now.")
        chatNpc(player, "Thanks again, adventurer.")
    }

    private suspend fun QueueTask.gertrudeSardineHint(player: Player) {
        chatPlayer(player, "Hello again.")
        chatNpc(player, "Hello. How's it going? Any luck?")
        chatPlayer(player, "Yes, I've found Fluffs!")
        chatNpc(player, "Well well, you are clever! Did you bring her back?")
        chatPlayer(player, "Well, that's the thing, she refuses to leave.")
        chatNpc(player, "Oh dear, oh dear! Maybe she's just hungry. She loves<br>doogle sardines but I'm all out.")
        chatPlayer(player, "Doogle sardines?")
        chatNpc(
            player,
            "Yes, raw sardines seasoned with doogle leaves.<br>" +
                "Unfortunately I've used all my doogle leaves, but you may<br>" +
                "find some in the woods out back.",
        )
    }

    private suspend fun QueueTask.gertrudeAfterSardine(player: Player) {
        chatPlayer(player, "Hi!")
        chatNpc(player, "Hey traveller, did Fluffs eat the sardines?")
        chatPlayer(player, "Yeah, she loved them, but she still won't leave.")
        chatNpc(player, "Well that is strange, there must be a reason.")
    }

    /**
     * The completion conversation, and the only place the quest is finished.
     *
     * The rewards are handed out *before* [completeQuest] flips the stage, and the whole thing is
     * guarded by [completeQuest] returning false on a quest that is already done, so a player who
     * manages to reach this twice cannot collect twice.
     */
    private suspend fun QueueTask.gertrudeFinish(player: Player) {
        chatPlayer(player, "Hello Gertrude. Fluffs ran off with her kitten.")
        chatNpc(
            player,
            "You're back! Thank you! Thank you! Fluffs just came back!<br>" +
                "I think she was just upset as she couldn't find her kitten.",
        )
        messageBox(player, "Gertrude gives you a hug.")
        chatNpc(player, "If you hadn't found her kitten it would have died out<br>there!")
        chatPlayer(player, "That's okay, I like to do my bit.")
        chatNpc(
            player,
            "I don't know how to thank you. I have no real material<br>" +
                "possessions. I do have kittens! I can only really look after<br>one.",
        )
        chatPlayer(player, "Well, if it needs a home.")
        chatNpc(
            player,
            "I would sell it to my cousin in West Ardougne. I hear<br>" +
                "there's a rat epidemic there. But it's too far.",
        )
        chatNpc(player, "Here you go, look after her and thank you again!")
        chatNpc(
            player,
            "Oh by the way, the kitten can live in your backpack, but to<br>" +
                "make it grow you must take it out and feed and stroke it<br>often.",
        )

        if (!player.isQuestComplete(quest)) {
            giveRewards(player)
            player.completeQuest(quest)
        }
    }

    /**
     * Hand over the quest rewards.
     *
     * Anything that will not fit goes to the floor rather than being silently dropped from
     * existence - three items plus a kitten is easy to be short of space for at the end of a quest
     * that also wants a bucket and a sardine.
     */
    private fun giveRewards(player: Player) {
        player.addXp(Skills.COOKING, GertrudesCat.COOKING_XP)

        /* Colour is random in the real game unless you charm Gertrude with a Ring of Charos(a). */
        val kitten = GertrudesCat.KITTEN_COLOURS[world.random(GertrudesCat.KITTEN_COLOURS.size - 1)]
        listOf(kitten, "item.chocolate_cake", "item.stew").forEach { item ->
            if (player.inventory.add(item).completed == 0) {
                world.spawn(GroundItem(item = getRSCMItem(item), amount = 1, tile = player.tile, owner = player))
            }
        }
    }

    private suspend fun QueueTask.gertrudeAfterQuest(player: Player) {
        chatPlayer(player, "Hello Gertrude.")
        chatNpc(player, "Hello again! How is the kitten doing?")

        when (options(player, "Could I have another kitten?", "Just passing through.")) {
            1 -> buyKitten(player)
            2 -> chatPlayer(player, "Just passing through.")
        }
    }

    /**
     * Gertrude's post-quest kitten sales, at the real 100 coins.
     *
     * The live game hangs this off a `Kitten` right-click option on her after-quest id (7723) and
     * refuses when you already own a cat. There is no pet system here to own a cat *in*, so the
     * only check is that you can pay and carry it.
     */
    private suspend fun QueueTask.buyKitten(player: Player) {
        chatNpc(player, "Of course, they're 100 coins each.")

        if (player.inventory.getItemCount(getRSCMItem("item.coins_995")) < GertrudesCat.BRIBE) {
            chatPlayer(player, "I don't have enough coins.")
            return
        }
        if (!player.inventory.hasSpace) {
            chatPlayer(player, "I don't have room for a kitten.")
            return
        }

        player.inventory.remove("item.coins_995", GertrudesCat.BRIBE)
        val kitten = GertrudesCat.KITTEN_COLOURS[world.random(GertrudesCat.KITTEN_COLOURS.size - 1)]
        player.inventory.add(kitten)
        chatNpc(player, "Here you go, look after her!")
    }

    // ------------------------------------------------------------------ Wilough and Shilop

    /**
     * The two boys share one conversation - the transcript gives them alternating lines in a single
     * exchange rather than a script each, and either of them can be the one you talk to.
     */
    private fun bindSons() {
        listOf("npc.wilough", "npc.shilop").forEach { son ->
            onNpcOption(son, option = "talk-to") {
                player.queue {
                    when {
                        player.questStage(quest) >= GertrudesCat.KNOWS_LOCATION -> sonsReminder(player)
                        player.hasStartedQuest(quest) -> sonsNegotiation(player)
                        else -> sonsBeforeQuest(player)
                    }
                }
            }
        }
    }

    /**
     * Before the quest there is nothing to ask them about, so this stops at the boy's guilty
     * opening line - the one real line of his that does not assume the player is already looking
     * for the cat. No new dialogue is invented for a branch the wiki does not document.
     */
    private suspend fun QueueTask.sonsBeforeQuest(player: Player) {
        chatPlayer(player, "Hello there.")
        chatNpc(player, "I didn't mean to take it! I just forgot to pay.")
    }

    private suspend fun QueueTask.sonsNegotiation(player: Player) {
        chatPlayer(player, "Hello there, I've been looking for you.")
        chatNpc(player, "I didn't mean to take it! I just forgot to pay.")
        chatPlayer(player, "What? I'm trying to help your mum find Fluffs.")
        chatNpc(
            player,
            "Ohh...well, in that case I might be able to help. Fluffs<br>" +
                "followed me to my secret play area, I haven't seen her<br>since.",
        )
        chatPlayer(player, "Where is this play area?")
        chatNpc(player, "If I told you that, it wouldn't be a secret.")

        when (
            options(
                player,
                "Tell me sonny, or I will hurt you.",
                "What will make you tell me?",
                "Well never mind, it's Fluffs' loss.",
            )
        ) {
            1 -> {
                chatPlayer(player, "Tell me sonny, or I will hurt you.")
                chatNpc(
                    player,
                    "W..wh..what?! Y..you wouldn't! A young lad like me!<br>" +
                        "I'd have you behind bars before nightfall!",
                )
                messageBox(player, "You decided it's best not to hurt the boy.")
            }

            2 -> haggle(player)

            3 -> {
                chatPlayer(player, "Well, never mind, it's Fluffs' loss.")
                chatNpc(player, "I'm sure my mum will get over it.")
            }
        }
    }

    private suspend fun QueueTask.haggle(player: Player) {
        chatPlayer(player, "What will make you tell me?")
        chatNpc(player, "Well...now you ask, I am a bit short on cash.")
        chatPlayer(player, "How much?")
        chatNpc(player, "10 coins.")
        chatNpc(player, "10 coins?!")
        chatNpc(player, "I'll handle this.")
        chatNpc(player, "100 coins should cover it.")
        chatPlayer(player, "100 coins! Why should I pay you?")
        chatNpc(player, "You shouldn't, but we won't help otherwise. We never<br>liked that cat anyway, so what do you say?")

        when (options(player, "Okay then, I'll pay.", "I'm not paying you a penny.")) {
            1 -> {
                chatPlayer(player, "Okay then, I'll pay.")
                if (player.inventory.getItemCount(getRSCMItem("item.coins_995")) < GertrudesCat.BRIBE) {
                    chatNpc(player, "You haven't got 100 coins!")
                    return
                }

                player.inventory.remove("item.coins_995", GertrudesCat.BRIBE)
                messageBox(player, "You give the lad 100 coins.")
                chatPlayer(player, "There you go, now where did you see Fluffs?")
                chatNpc(
                    player,
                    "I play at an abandoned lumber mill to the north east.<br>" +
                        "Just beyond the Jolly Boar Inn. I saw Fluffs running<br>around in there.",
                )
                chatPlayer(player, "Anything else?")
                chatNpc(player, "Well, you'll have to find the broken fence to get in.<br>I'm sure you can manage that.")
                player.setQuestStage(quest, GertrudesCat.KNOWS_LOCATION)
            }

            2 -> {
                chatPlayer(player, "I'm not paying you a penny.")
                chatNpc(player, "Okay then, I'll find another way to make money.")
            }
        }
    }

    private suspend fun QueueTask.sonsReminder(player: Player) {
        chatPlayer(player, "Where did you say you saw Fluffs?")
        chatNpc(
            player,
            "Weren't you listening? I saw the flea bag in the old lumber<br>" +
                "mill just north east of here. Just walk past the Jolly Boar<br>Inn and you should find it.",
        )
    }

    // ------------------------------------------------------------------ Fluffs

    /**
     * Fluffs' three options and the two items she wants, in order.
     *
     * The order is the quest: she refuses to be picked up until she has had milk, then refuses again
     * until she has had the seasoned sardine, then refuses a third time because of the kitten. Each
     * refusal is what tells the player what to try next, so the hint has to match the stage exactly.
     */
    private fun bindFluffs() {
        val fluffs = "npc.gertrudes_cat_3497"

        onNpcOption(fluffs, option = "talk-to") {
            player.queue { fluffsSays(player, "Miaoww") }
        }

        onNpcOption(fluffs, option = "stroke") {
            player.queue {
                if (player.questStage(quest) >= GertrudesCat.REUNITED) {
                    fluffsSays(player, "Purr...")
                    return@queue
                }
                hiss(player)
                messageBox(player, "Perhaps the cat wants something?")
            }
        }

        onNpcOption(fluffs, option = "pick-up") {
            player.queue {
                when {
                    player.questStage(quest) >= GertrudesCat.REUNITED -> fluffsSays(player, "Purr...")

                    player.questStage(quest) >= GertrudesCat.CAT_HAD_SARDINE -> {
                        hiss(player)
                        messageBox(
                            player,
                            "The cat seems afraid to leave. In the distance you can hear<br>kittens mewing...",
                        )
                    }

                    player.questStage(quest) >= GertrudesCat.CAT_HAD_MILK -> {
                        hiss(player)
                        messageBox(player, "Maybe the cat is hungry?")
                    }

                    else -> {
                        hiss(player)
                        messageBox(player, "Maybe the cat is thirsty?")
                    }
                }
            }
        }

        onItemOnNpc(item = "item.bucket_of_milk", npc = fluffs) {
            player.queue {
                if (!player.hasStartedQuest(quest)) {
                    messageBox(player, "Nothing interesting happens.")
                    return@queue
                }
                if (player.questStage(quest) >= GertrudesCat.CAT_HAD_MILK) {
                    messageBox(player, "The cat has already had a drink.")
                    return@queue
                }
                if (player.inventory.remove("item.bucket_of_milk", assureFullRemoval = true).completed == 0) {
                    return@queue
                }

                /* You keep the bucket, the way every other use of milk in the game leaves one. */
                player.inventory.add("item.bucket")
                fluffsSays(player, "Mew!")
                player.setQuestStage(quest, GertrudesCat.CAT_HAD_MILK)
            }
        }

        onItemOnNpc(item = "item.seasoned_sardine", npc = fluffs) {
            player.queue {
                if (player.questStage(quest) < GertrudesCat.CAT_HAD_MILK) {
                    /*
                     * Refusing rather than skipping the milk: the quest's own hints walk the player
                     * through thirst before hunger, and letting the sardine jump the queue would
                     * strand the "Maybe the cat is thirsty?" branch permanently.
                     */
                    messageBox(player, "The cat is too thirsty to be interested in food.")
                    return@queue
                }
                if (player.questStage(quest) >= GertrudesCat.CAT_HAD_SARDINE) {
                    messageBox(player, "Nothing interesting happens.")
                    return@queue
                }
                if (player.inventory.remove("item.seasoned_sardine", assureFullRemoval = true).completed == 0) {
                    return@queue
                }

                fluffsSays(player, "Mew!")
                player.setQuestStage(quest, GertrudesCat.CAT_HAD_SARDINE)
            }
        }

        onItemOnNpc(item = "item.fluffs_kitten", npc = fluffs) {
            player.queue {
                if (player.questStage(quest) >= GertrudesCat.REUNITED) {
                    messageBox(player, "Nothing interesting happens.")
                    return@queue
                }
                if (player.inventory.remove("item.fluffs_kitten", assureFullRemoval = true).completed == 0) {
                    return@queue
                }

                fluffsSays(player, "Purr...")
                /*
                 * Fluffs stays standing here afterwards - she is one world npc shared by everyone,
                 * so despawning her would remove her from every other player's quest. See the
                 * deviations listed on [GertrudesCat].
                 */
                messageBox(player, "Fluffs has run off home with her offspring.")
                player.setQuestStage(quest, GertrudesCat.REUNITED)
            }
        }
    }

    /** Fluffs' overhead line, spoken by the cat the player is actually interacting with. */
    private fun fluffsSays(
        player: Player,
        line: String,
    ) {
        interactingNpc(player)?.forceChat(line)
    }

    private suspend fun QueueTask.hiss(player: Player) {
        fluffsSays(player, "Hisss!")
        player.forceChat("Ouch!")
        wait(2)
    }

    // ------------------------------------------------------------------ The crates

    /**
     * The six mewing crates.
     *
     * Which one holds the kitten is decided per player on their first search and then never changes
     * - the wiki is explicit that the mewing wanders but the kitten does not. The roll is stored
     * persistently, so logging out mid-hunt does not reshuffle it, and a player who drops the kitten
     * can go straight back to the same crate.
     */
    private fun bindCrates() {
        onNpcOption("npc.crate_3499", option = "search") {
            val crate = interactingNpc(player)
            player.queue {
                if (player.questStage(quest) < GertrudesCat.CAT_HAD_SARDINE) {
                    /*
                     * Before Fluffs has eaten, the player has no reason to be here and the real
                     * game gives them nothing.
                     */
                    messageBox(player, "You search the crate but find nothing.")
                    return@queue
                }
                if (player.questStage(quest) >= GertrudesCat.FOUND_KITTEN &&
                    player.inventory.contains("item.fluffs_kitten")
                ) {
                    messageBox(player, "You already have the kitten.")
                    return@queue
                }

                val crateTile = crate?.tile
                val index =
                    if (crateTile == null) -1 else GertrudesCat.CRATE_TILES.indexOfFirst { it.sameAs(crateTile) }
                if (index == -1) {
                    messageBox(player, "You search the crate but find nothing.")
                    return@queue
                }

                val chosen =
                    player.attr[GertrudesCat.CHOSEN_CRATE_ATTR]
                        ?: world.random(GertrudesCat.CRATE_TILES.size - 1).also {
                            player.attr[GertrudesCat.CHOSEN_CRATE_ATTR] = it
                        }

                if (index != chosen) {
                    messageBox(player, "You search the crate but find nothing.")
                    return@queue
                }
                if (!player.inventory.hasSpace) {
                    messageBox(player, "You hear a kitten, but you have no room to carry it.")
                    return@queue
                }

                player.inventory.add("item.fluffs_kitten")
                messageBox(player, "You find a kitten! You carefully place it in your backpack.")
                player.setQuestStage(quest, GertrudesCat.FOUND_KITTEN)
            }
        }
    }

    // ------------------------------------------------------------------ Getting in, and seasoning

    /**
     * The broken fence on the yard's southern side, the only way in.
     *
     * Which side the player ends up on is decided from which side they started, so the same object
     * works in both directions the way it does in game.
     */
    private fun bindBrokenFence() {
        onObjOption(obj = "object.broken_fence_2618", option = "climb-over") {
            val player = player
            player.queue {
                player.lock()
                player.animate(839)
                wait(2)
                val north = player.tile.z <= FENCE_Z
                player.moveTo(FENCE_X, if (north) FENCE_Z + 1 else FENCE_Z - 1, 0)
                player.unlock()
            }
        }
    }

    /**
     * Doogle leaves on a raw sardine.
     *
     * Stays available after the quest, as it is in the real game - the seasoned sardine simply
     * stops being useful for anything.
     */
    private fun bindSeasoning() {
        onItemOnItem(item1 = "item.doogle_leaves", item2 = "item.raw_sardine") {
            val player = player
            if (player.inventory.remove("item.doogle_leaves", assureFullRemoval = true).completed == 0) {
                return@onItemOnItem
            }
            if (player.inventory.remove("item.raw_sardine", assureFullRemoval = true).completed == 0) {
                /* Put the leaves back rather than eating them for nothing. */
                player.inventory.add("item.doogle_leaves")
                return@onItemOnItem
            }
            player.inventory.add("item.seasoned_sardine")
            player.message("You rub the doogle leaves all over the sardines.")
        }
    }

    // ------------------------------------------------------------------ helpers

    private fun interactingNpc(player: Player): Npc? = player.attr[INTERACTING_NPC_ATTR]?.get()

    private fun getRSCMItem(key: String): Int = org.alter.rscm.RSCM.getRSCM(key)

    private companion object {
        /** The broken fence's tile, from the region 13110 location dump. */
        const val FENCE_X = 3308
        const val FENCE_Z = 3492
    }
}
