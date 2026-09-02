package org.alter.plugins.content.commands.commands.owner

import org.alter.api.Skills
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.info.PlayerInfo
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.appearance.Appearance
import org.alter.game.model.appearance.Colours
import org.alter.game.model.appearance.Gender
import org.alter.game.model.appearance.Looks
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.model.move.moveTo
import org.alter.game.model.priv.Privilege
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.queue.TaskPriority
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.mechanics.music.MusicUnlocks

/**
 * A single owner-only command that bundles the most common staff actions (item
 * spawning, xp granting, healing, teleports and appearance editing) behind one
 * chatbox menu instead of many separate commands.
 */
class CheatMenuPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {

    init {
        onCommand("cheatmenu", Privilege.OWNER_POWER, description = "Opens the owner-only cheat menu") {
            player.queue(TaskPriority.STRONG) {
                openMenu(player)
            }
        }
    }

    private suspend fun QueueTask.openMenu(player: Player) {
        val entries =
            listOf(
                "Give item",
                "Give XP",
                "Heal fully",
                "Refill prayer points",
                "Teleport to a city",
                "Edit appearance / gender",
                "Unlock all music",
            )
        when (pagedOptions(player, entries, title = "Cheat Menu")) {
            1 -> giveItem(player)
            2 -> giveXp(player)
            3 -> heal(player)
            4 -> refillPrayer(player)
            5 -> teleport(player)
            6 -> editAppearance(player)
            7 -> unlockAllMusic(player)
        }
    }

    private fun unlockAllMusic(player: Player) {
        val before = MusicUnlocks.unlockedCount(player)
        MusicUnlocks.unlockAll(player)
        val after = MusicUnlocks.unlockedCount(player)
        player.message("Music unlocked: $before -> $after tracks.")
    }

    /**
     * Same as [options], but paginates [items] a few at a time. The chatbox option
     * list only has room for about 5 lines before entries get cut off with no way to
     * scroll, so anything longer needs "Next page"/"Previous page" entries instead.
     *
     * @return the 1-based index into [items] that was chosen, or -1 if closed.
     */
    private suspend fun QueueTask.pagedOptions(
        player: Player,
        items: List<String>,
        title: String,
        pageSize: Int = 3,
    ): Int {
        if (items.size <= 5) {
            return options(player, *items.toTypedArray(), title = title)
        }
        val totalPages = (items.size + pageSize - 1) / pageSize
        var page = 0
        while (true) {
            val start = page * pageSize
            val end = minOf(start + pageSize, items.size)
            val pageItems = items.subList(start, end).toMutableList()
            val itemCount = pageItems.size
            if (page > 0) pageItems.add("<< Previous page")
            if (page < totalPages - 1) pageItems.add("Next page >>")
            val choice = options(player, *pageItems.toTypedArray(), title = "$title (${page + 1}/$totalPages)")
            if (choice == -1) return -1
            val idx = choice - 1
            when {
                idx < itemCount -> return start + idx + 1
                page > 0 && idx == itemCount -> page--
                else -> page++
            }
        }
    }

    private suspend fun QueueTask.giveItem(player: Player) {
        val item = searchItemInputT(player, "Select an item to give yourself:")
        if (item == -1) return
        val amount =
            when (options(player, "1", "5", "X", "Max", title = "How many would you like to give?")) {
                1 -> 1
                2 -> 5
                3 -> inputInt(player, "Enter amount")
                4 -> Int.MAX_VALUE
                else -> return
            }
        if (amount <= 0) return
        val added = player.inventory.add(item, amount, assureFullInsertion = false)
        if (added.completed > 0) {
            player.message("You have given yourself ${added.completed} x ${Item(item).getName()}.")
        } else {
            player.message("You don't have enough inventory space.")
        }
    }

    private suspend fun QueueTask.giveXp(player: Player) {
        val name = inputString(player, "Enter a skill name").trim().lowercase()
        val skill = Skills.getSkillForName(world, player.getSkills().maxSkills, resolveSkillAlias(name))
        if (skill == -1) {
            player.message("Could not find a skill named '$name'.")
            return
        }
        val amount = inputInt(player, "Enter amount of xp to give")
        if (amount <= 0) return
        player.addXp(skill, amount.toDouble())
        player.message("Gave yourself $amount xp in ${Skills.getSkillName(world, skill)}.")
    }

    private fun heal(player: Player) {
        val skills = player.getSkills()
        skills.setCurrentLevel(Skills.HITPOINTS, skills.getBaseLevel(Skills.HITPOINTS))
        player.message("You have been fully healed.")
    }

    private fun refillPrayer(player: Player) {
        val skills = player.getSkills()
        skills.setCurrentLevel(Skills.PRAYER, skills.getBaseLevel(Skills.PRAYER))
        player.message("Your prayer points have been refilled.")
    }

    private suspend fun QueueTask.teleport(player: Player) {
        val regionChoice = pagedOptions(player, TELEPORT_REGIONS.map { it.first }, title = "Teleport to a city")
        if (regionChoice == -1) return
        val (region, destinations) = TELEPORT_REGIONS[regionChoice - 1]
        val choice = pagedOptions(player, destinations.map { it.first }, title = region)
        if (choice == -1) return
        val (name, tile) = destinations[choice - 1]
        player.moveTo(tile)
        player.message("Teleported to $name.")
    }

    private suspend fun QueueTask.editAppearance(player: Player) {
        when (
            options(
                player,
                "Set default male appearance",
                "Set default female appearance",
                "Change a body look",
                "Change a colour",
                title = "Edit Appearance",
            )
        ) {
            1 -> applyDefaultAppearance(player, Gender.MALE)
            2 -> applyDefaultAppearance(player, Gender.FEMALE)
            3 -> changeBodyLook(player)
            4 -> changeColour(player)
        }
    }

    private fun applyDefaultAppearance(
        player: Player,
        gender: Gender,
    ) {
        player.appearance = if (gender == Gender.MALE) Appearance.DEFAULT_MALE else Appearance.DEFAULT_FEMALE
        PlayerInfo(player).syncAppearance()
        player.message("Your appearance has been reset.")
    }

    private suspend fun QueueTask.changeBodyLook(player: Player) {
        val slots = lookSlots(player.appearance)
        while (true) {
            val labels = slots.map { it.first } + "Done"
            val partChoice = pagedOptions(player, labels, title = "Which body part?")
            if (partChoice == -1 || partChoice == labels.size) return
            val (lookIndex, values) = slots[partChoice - 1].second
            cycleValue(
                player,
                label = slots[partChoice - 1].first,
                size = values.size,
                get = { player.appearance.looks[lookIndex] },
                set = { player.appearance.looks[lookIndex] = it },
            )
        }
    }

    private suspend fun QueueTask.changeColour(player: Player) {
        val colours = colourSlots()
        while (true) {
            val labels = colours.map { it.first } + "Done"
            val colourChoice = pagedOptions(player, labels, title = "Which colour?")
            if (colourChoice == -1 || colourChoice == labels.size) return
            val (colourIndex, values) = colours[colourChoice - 1].second
            cycleValue(
                player,
                label = colours[colourChoice - 1].first,
                size = values.size,
                get = { player.appearance.colors[colourIndex] },
                set = { player.appearance.colors[colourIndex] = it },
            )
        }
    }

    /**
     * Lets the player step [get]/[set] up or down by one, one press at a time,
     * without the dialog closing between presses - like clicking the arrows on the
     * real character design screen instead of picking a style from a flat list.
     */
    private suspend fun QueueTask.cycleValue(
        player: Player,
        label: String,
        size: Int,
        get: () -> Int,
        set: (Int) -> Unit,
    ) {
        while (true) {
            val current = get()
            when (options(player, "Next", "Previous", "Done", title = "$label (${current + 1}/$size)")) {
                1 -> {
                    set((current + 1) % size)
                    PlayerInfo(player).syncAppearance()
                }
                2 -> {
                    set((current - 1 + size) % size)
                    PlayerInfo(player).syncAppearance()
                }
                else -> return
            }
        }
    }

    private fun lookSlots(appearance: Appearance): List<Pair<String, Pair<Int, Array<Int>>>> {
        val gender = appearance.gender
        return if (gender == Gender.MALE) {
            listOf(
                "Head" to (0 to Looks.getHeads(gender)),
                "Jaw" to (1 to Looks.getJaws(gender)),
                "Torso" to (2 to Looks.getTorsos(gender)),
                "Arms" to (3 to Looks.getArms(gender)),
                "Hands" to (4 to Looks.getHands(gender)),
                "Legs" to (5 to Looks.getLegs(gender)),
                "Feet" to (6 to Looks.getFeets(gender)),
            )
        } else {
            listOfNotNull(
                "Head" to (0 to Looks.getHeads(gender)),
                // The female jaw lives at the end of the look array rather than at slot 1,
                // so it is only offered once the array is long enough to hold it.
                if (appearance.looks.size > Appearance.FEMALE_JAW_INDEX) {
                    "Jaw" to (Appearance.FEMALE_JAW_INDEX to Looks.getJaws(gender))
                } else {
                    null
                },
                "Torso" to (1 to Looks.getTorsos(gender)),
                "Arms" to (2 to Looks.getArms(gender)),
                "Hands" to (3 to Looks.getHands(gender)),
                "Legs" to (4 to Looks.getLegs(gender)),
                "Feet" to (5 to Looks.getFeets(gender)),
            )
        }
    }

    private fun colourSlots(): List<Pair<String, Pair<Int, Array<Int>>>> =
        listOf(
            "Hair" to (0 to Colours.HAIR_COLOURS),
            "Torso" to (1 to Colours.TORSO_COLOURS),
            "Legs" to (2 to Colours.LEG_COLOURS),
            "Feet" to (3 to Colours.FEET_COLOURS),
            "Skin" to (4 to Colours.SKIN_COLOURS),
        )

    private fun resolveSkillAlias(name: String): String =
        when (name) {
            "con" -> "construction"
            "hp" -> "hitpoints"
            "craft" -> "crafting"
            "hunt" -> "hunter"
            "slay" -> "slayer"
            "pray" -> "prayer"
            "mage" -> "magic"
            "fish" -> "fishing"
            "herb" -> "herblore"
            "rc" -> "runecrafting"
            "fm" -> "firemaking"
            else -> name
        }

    private companion object {
        val TELEPORT_REGIONS =
            listOf(
                "Misthalin" to
                    listOf(
                        "Lumbridge" to Tile(x = 3222, z = 3217, height = 0),
                        "Varrock" to Tile(x = 3211, z = 3424, height = 0),
                        "Grand Exchange" to Tile(x = 3164, z = 3487, height = 0),
                        "Draynor Village" to Tile(x = 3105, z = 3251, height = 0),
                        "Barbarian Village" to Tile(x = 3082, z = 3420, height = 0),
                        "Edgeville" to Tile(x = 3087, z = 3499, height = 0),
                        "Al Kharid" to Tile(x = 3293, z = 3193, height = 0),
                    ),
                "Asgarnia" to
                    listOf(
                        "Falador" to Tile(x = 2966, z = 3379, height = 0),
                        "Port Sarim" to Tile(x = 3042, z = 3235, height = 0),
                        "Rimmington" to Tile(x = 2957, z = 3215, height = 0),
                        "Taverley" to Tile(x = 2894, z = 3428, height = 0),
                        "Burthorpe" to Tile(x = 2900, z = 3538, height = 0),
                    ),
                "Kandarin" to
                    listOf(
                        "Ardougne" to Tile(x = 2662, z = 3305, height = 0),
                        "Catherby" to Tile(x = 2808, z = 3438, height = 0),
                        "Camelot" to Tile(x = 2757, z = 3477, height = 0),
                        "Seers' Village" to Tile(x = 2725, z = 3487, height = 0),
                        "Yanille" to Tile(x = 2606, z = 3093, height = 0),
                        "Fishing Guild" to Tile(x = 2612, z = 3392, height = 0),
                        "Tree Gnome Stronghold" to Tile(x = 2461, z = 3444, height = 0),
                        "Castle Wars" to Tile(x = 2440, z = 3090, height = 0),
                    ),
                "Kharidian Desert" to
                    listOf(
                        "Emir's Arena" to Tile(x = 3315, z = 3235, height = 0),
                        "Pollnivneach" to Tile(x = 3359, z = 2966, height = 0),
                        "Nardah" to Tile(x = 3427, z = 2914, height = 0),
                        "Sophanem" to Tile(x = 3282, z = 2765, height = 0),
                    ),
                "Karamja" to
                    listOf(
                        "Musa Point" to Tile(x = 2925, z = 3176, height = 0),
                        "Brimhaven" to Tile(x = 2762, z = 3232, height = 0),
                        "Shilo Village" to Tile(x = 2852, z = 2955, height = 0),
                    ),
                "Morytania" to
                    listOf(
                        "Canifis" to Tile(x = 3492, z = 3488, height = 0),
                        "Port Phasmatys" to Tile(x = 3685, z = 3465, height = 0),
                    ),
                "Great Kourend" to
                    listOf(
                        "Kourend Castle" to Tile(x = 1640, z = 3673, height = 0),
                        "Hosidius" to Tile(x = 1743, z = 3517, height = 0),
                    ),
                "Wilderness" to
                    listOf(
                        "Ferox Enclave" to Tile(x = 3129, z = 3630, height = 0),
                        "Edgeville ditch" to Tile(x = 3087, z = 3520, height = 0),
                    ),
            )
    }
}
