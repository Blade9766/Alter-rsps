package org.alter.plugins.content.skills.fishing

import org.alter.api.Skills
import org.alter.api.cfg.Sound
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.model.move.hasMoveDestination
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.queue.TaskPriority
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.skills.strength.Strength
import org.alter.rscm.RSCM.getRSCM

/**
 * The core Fishing skill: small net, rod+bait, fly rod+feathers, harpoon, and lobster
 * pot fishing.
 *
 * Spot placements below come from the user's own in-game click coordinates (same as the
 * original Draynor spots), so unlike the wiki (which never publishes exact fishing-spot
 * tiles in article text, only on its interactive map) these didn't need to be guessed:
 * - Draynor Village: real Net+Bait combo (shrimp/anchovy net, sardine/herring bait).
 * - Lumbridge Swamp east + Catherby: real "Small Net"+Bait combo (a distinct cache NPC
 *   from Draynor's plain "Net" variant - same fish, different exact action wording).
 * - Barbarian Village + Lumbridge River: real "Rod Fishing spot" NPC, which exposes
 *   "Lure" (fly rod+feathers: trout/salmon/rainbow fish) and "Bait" (rod+bait:
 *   sardine/herring/pike) as two separate menu options - confirmed via cache diagnostic,
 *   which resolved an earlier assumption that bait/lure shared one ambiguous option.
 * - Catherby + Musa Point (Karamja): real Cage+Harpoon combo (lobster pot -> lobster;
 *   harpoon -> tuna/swordfish).
 *
 * Every other real "fishing_spot"/"rod_fishing_spot" NPC id/action combo in the cache
 * that isn't placed above is still wired generically off its exact action set (see
 * [BAIT_ONLY_SPOTS], [HARPOON_AND_CAGE_SPOTS], [HARPOON_ONLY_SPOTS], [CAGE_ONLY_SPOTS]),
 * so a future area needs zero new plugin code to add more of these same spot types -
 * same pattern as [org.alter.plugins.content.skills.woodcutting.WoodcuttingPlugin] wiring
 * off whatever "chop"-ish action a tree object actually has.
 *
 * As with Woodcutting's chop chance, the exact per-attempt catch-success formula Jagex
 * uses isn't public, so [catchChance] and [rollFish]'s tier-weighting are labeled
 * approximations; level requirements, XP, and bait/feather-per-catch consumption are
 * sourced from the wiki (feather consumption is inferred by analogy to bait's confirmed
 * "one per catch" behaviour, since feathers are described as fly fishing's bait but the
 * wiki doesn't spell out their consumption rate explicitly).
 *
 * Deliberately does not call [org.alter.game.model.entity.Pawn.lock] around any fishing
 * loop - see [org.alter.plugins.content.skills.woodcutting.WoodcuttingPlugin]'s doc for
 * why that traps the player.
 */
class FishingPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        DRAYNOR_SPOTS.forEach { (x, z) ->
            spawnNpc(npc = DRAYNOR_SPOT, x = x, z = z, height = 0, walkRadius = 0, direction = Direction.SOUTH)
        }
        onNpcOption(DRAYNOR_SPOT, option = "net") {
            val spot = player.getInteractingNpc()
            player.queue(TaskPriority.STANDARD) {
                fishWithTool(this, player, spot, getRSCM("item.small_fishing_net"), "small fishing net", SMALL_NET_ANIMATION, Sound.NET, NET_CATCHES)
            }
        }
        onNpcOption(DRAYNOR_SPOT, option = "bait") {
            val spot = player.getInteractingNpc()
            player.queue(TaskPriority.STANDARD) { fishWithBait(this, player, spot, SARDINE_HERRING_CATCHES) }
        }

        SMALL_NET_BAIT_SPOTS.forEach { (x, z) ->
            spawnNpc(npc = SMALL_NET_BAIT_SPOT, x = x, z = z, height = 0, walkRadius = 0, direction = Direction.SOUTH)
        }
        onNpcOption(SMALL_NET_BAIT_SPOT, option = "small net") {
            val spot = player.getInteractingNpc()
            player.queue(TaskPriority.STANDARD) {
                fishWithTool(this, player, spot, getRSCM("item.small_fishing_net"), "small fishing net", SMALL_NET_ANIMATION, Sound.NET, NET_CATCHES)
            }
        }
        onNpcOption(SMALL_NET_BAIT_SPOT, option = "bait") {
            val spot = player.getInteractingNpc()
            player.queue(TaskPriority.STANDARD) { fishWithBait(this, player, spot, SARDINE_HERRING_CATCHES) }
        }

        ROD_LURE_BAIT_SPOTS.forEach { (x, z) ->
            spawnNpc(npc = ROD_FISHING_SPOT, x = x, z = z, height = 0, walkRadius = 0, direction = Direction.SOUTH)
        }
        onNpcOption(ROD_FISHING_SPOT, option = "lure") {
            val spot = player.getInteractingNpc()
            player.queue(TaskPriority.STANDARD) { fishWithFly(this, player, spot, FLY_CATCHES) }
        }
        onNpcOption(ROD_FISHING_SPOT, option = "bait") {
            val spot = player.getInteractingNpc()
            player.queue(TaskPriority.STANDARD) { fishWithBait(this, player, spot, FULL_BAIT_CATCHES) }
        }

        CAGE_HARPOON_SPOTS.forEach { (x, z) ->
            spawnNpc(npc = CAGE_HARPOON_SPOT, x = x, z = z, height = 0, walkRadius = 0, direction = Direction.SOUTH)
        }
        onNpcOption(CAGE_HARPOON_SPOT, option = "cage") {
            val spot = player.getInteractingNpc()
            player.queue(TaskPriority.STANDARD) { fishCage(this, player, spot) }
        }
        onNpcOption(CAGE_HARPOON_SPOT, option = "harpoon") {
            val spot = player.getInteractingNpc()
            player.queue(TaskPriority.STANDARD) { fishHarpoon(this, player, spot) }
        }

        BAIT_ONLY_SPOTS.forEach { id ->
            onNpcOption("npc.fishing_spot_$id", option = "bait") {
                val spot = player.getInteractingNpc()
                player.queue(TaskPriority.STANDARD) { fishWithBait(this, player, spot, FULL_BAIT_CATCHES) }
            }
        }
        HARPOON_AND_CAGE_SPOTS.forEach { id ->
            onNpcOption("npc.fishing_spot_$id", option = "harpoon") {
                val spot = player.getInteractingNpc()
                player.queue(TaskPriority.STANDARD) { fishHarpoon(this, player, spot) }
            }
            onNpcOption("npc.fishing_spot_$id", option = "cage") {
                val spot = player.getInteractingNpc()
                player.queue(TaskPriority.STANDARD) { fishCage(this, player, spot) }
            }
        }
        HARPOON_ONLY_SPOTS.forEach { id ->
            onNpcOption("npc.fishing_spot_$id", option = "harpoon") {
                val spot = player.getInteractingNpc()
                player.queue(TaskPriority.STANDARD) { fishHarpoon(this, player, spot) }
            }
        }
        CAGE_ONLY_SPOTS.forEach { id ->
            onNpcOption("npc.fishing_spot_$id", option = "cage") {
                val spot = player.getInteractingNpc()
                player.queue(TaskPriority.STANDARD) { fishCage(this, player, spot) }
            }
        }
    }

    /**
     * One catchable fish.
     *
     * [strengthLevel] and [strengthExperience] are only ever non-default for the barehanded
     * catches: every other way of fishing asks nothing of Strength and pays nothing into it.
     */
    private data class Fish(
        val name: String,
        val itemId: Int,
        val level: Int,
        val experience: Double,
        val message: String,
        val strengthLevel: Int = 1,
        val strengthExperience: Double = 0.0,
    )

    /**
     * Net / harpoon / cage fishing: a held tool, no bait consumption.
     *
     * [tool] is null for barehanded fishing, the one method with no tool at all - the arm is the
     * bait. That method is also the only one whose catches carry a Strength requirement, so the
     * eligible set is filtered on both skills rather than on Fishing alone and the roll is handed
     * the already-filtered list; otherwise a player past the Fishing level but short of the
     * Strength one would land fish they cannot pull in.
     */
    private suspend fun fishWithTool(
        task: QueueTask,
        player: Player,
        spot: Npc,
        tool: Int?,
        toolName: String,
        animation: Int,
        sound: Int,
        catches: List<Fish>,
    ) {
        if (tool != null && !player.inventory.contains(tool) && !player.equipment.contains(tool)) {
            player.message("You need a $toolName to fish here.")
            return
        }
        val level = player.getSkills().getCurrentLevel(Skills.FISHING)
        val strength = player.getSkills().getCurrentLevel(Skills.STRENGTH)
        val eligible = catches.filter { level >= it.level && strength >= it.strengthLevel }
        if (eligible.isEmpty()) {
            val easiest = catches.minByOrNull { it.level }!!
            if (level < easiest.level) {
                player.message("You need a Fishing level of ${easiest.level} to fish here.")
            } else {
                player.message("You need a Strength level of ${easiest.strengthLevel} to fish here.")
            }
            return
        }
        if (player.inventory.isFull) {
            player.message("Your inventory is too full to hold any more fish.")
            return
        }

        player.faceTile(spot.tile)
        while (spot.isSpawned() && !player.inventory.isFull && !player.hasMoveDestination()) {
            player.animate(animation)
            player.playSound(sound)
            task.wait(4)

            if (player.world.randomDouble() <= catchChance(level)) {
                val caught = rollFish(level, eligible, player.world)
                player.addXp(Skills.FISHING, caught.experience)
                if (caught.strengthExperience > 0.0) {
                    player.addXp(Skills.STRENGTH, caught.strengthExperience)
                }
                player.inventory.add(item = caught.itemId, amount = 1)
                player.message(caught.message)
            }
        }
    }

    /**
     * Harpooning, or barehanded fishing when there is no harpoon to hand.
     *
     * The client sends the same "Harpoon" option either way - barehanded fishing adds no menu
     * entry of its own - so the choice is made here: a player carrying a harpoon uses it, and a
     * player with none who has been taught by Otto Godblessed fishes with their arm instead.
     * Without the lesson the harpoon branch runs and says what is missing, as it always did.
     */
    private suspend fun fishHarpoon(
        task: QueueTask,
        player: Player,
        spot: Npc,
    ) {
        val harpoon = getRSCM("item.harpoon")
        val carrying = player.inventory.contains(harpoon) || player.equipment.contains(harpoon)
        if (!carrying && Strength.hasBarehandFishing(player)) {
            fishWithTool(task, player, spot, null, "harpoon", BAREHAND_ANIMATION, Sound.FISH_SPLASH, BAREHAND_CATCHES)
            return
        }
        fishWithTool(task, player, spot, harpoon, "harpoon", HARPOON_ANIMATION, Sound.FISH_SPLASH, HARPOON_CATCHES)
    }

    private suspend fun fishCage(
        task: QueueTask,
        player: Player,
        spot: Npc,
    ) = fishWithTool(task, player, spot, getRSCM("item.lobster_pot"), "lobster pot", CAGE_ANIMATION, Sound.FISH_SPLASH, CAGE_CATCHES)

    /** Rod+bait fishing: consumes 1 fishing bait per successful catch. */
    private suspend fun fishWithBait(
        task: QueueTask,
        player: Player,
        spot: Npc,
        catches: List<Fish>,
    ) {
        val rod = getRSCM("item.fishing_rod")
        val bait = getRSCM("item.fishing_bait")

        if (!player.inventory.contains(rod) && !player.equipment.contains(rod)) {
            player.message("You need a fishing rod to fish here.")
            return
        }
        if (!player.inventory.contains(bait)) {
            player.message("You need some fishing bait to fish here.")
            return
        }
        val level = player.getSkills().getCurrentLevel(Skills.FISHING)
        val minLevel = catches.minOf { it.level }
        if (level < minLevel) {
            player.message("You need a Fishing level of $minLevel to fish here.")
            return
        }
        if (player.inventory.isFull) {
            player.message("Your inventory is too full to hold any more fish.")
            return
        }

        player.faceTile(spot.tile)
        while (spot.isSpawned() &&
            !player.inventory.isFull &&
            !player.hasMoveDestination() &&
            player.inventory.contains(bait)
        ) {
            player.animate(BAIT_OR_FLY_ANIMATION)
            player.playSound(Sound.FISHING_CAST)
            task.wait(4)

            if (player.world.randomDouble() <= catchChance(level)) {
                player.inventory.remove(item = bait, amount = 1)
                val caught = rollFish(level, catches, player.world)
                player.addXp(Skills.FISHING, caught.experience)
                player.inventory.add(item = caught.itemId, amount = 1)
                player.message(caught.message)
            }
        }
        if (!player.inventory.contains(bait)) {
            player.message("You've run out of fishing bait.")
        }
    }

    /** Fly rod+feathers fishing: consumes 1 feather per successful catch. */
    private suspend fun fishWithFly(
        task: QueueTask,
        player: Player,
        spot: Npc,
        catches: List<Fish>,
    ) {
        val rod = getRSCM("item.fly_fishing_rod")
        val feather = getRSCM("item.feather")

        if (!player.inventory.contains(rod) && !player.equipment.contains(rod)) {
            player.message("You need a fly fishing rod to fish here.")
            return
        }
        if (!player.inventory.contains(feather)) {
            player.message("You need some feathers to fish here.")
            return
        }
        val level = player.getSkills().getCurrentLevel(Skills.FISHING)
        val minLevel = catches.minOf { it.level }
        if (level < minLevel) {
            player.message("You need a Fishing level of $minLevel to fish here.")
            return
        }
        if (player.inventory.isFull) {
            player.message("Your inventory is too full to hold any more fish.")
            return
        }

        player.faceTile(spot.tile)
        while (spot.isSpawned() &&
            !player.inventory.isFull &&
            !player.hasMoveDestination() &&
            player.inventory.contains(feather)
        ) {
            player.animate(BAIT_OR_FLY_ANIMATION)
            player.playSound(Sound.FISHING_CAST)
            task.wait(4)

            if (player.world.randomDouble() <= catchChance(level)) {
                player.inventory.remove(item = feather, amount = 1)
                val caught = rollFish(level, catches, player.world)
                player.addXp(Skills.FISHING, caught.experience)
                player.inventory.add(item = caught.itemId, amount = 1)
                player.message(caught.message)
            }
        }
        if (!player.inventory.contains(feather)) {
            player.message("You've run out of feathers.")
        }
    }

    private fun catchChance(level: Int): Double = (0.1 + level * 0.015).coerceIn(0.1, 0.8)

    private fun rollFish(
        level: Int,
        catches: List<Fish>,
        world: World,
    ): Fish {
        val eligible = catches.filter { level >= it.level }.sortedByDescending { it.level }
        if (eligible.size <= 1) {
            return eligible.firstOrNull() ?: catches.minByOrNull { it.level }!!
        }
        for (fish in eligible.dropLast(1)) {
            val chance = (0.15 + (level - fish.level) * 0.01).coerceAtMost(0.5)
            if (world.randomDouble() <= chance) {
                return fish
            }
        }
        return eligible.last()
    }

    private companion object {
        const val SMALL_NET_ANIMATION = 621 // AnimationID.HUMAN_SMALLNET
        const val BAIT_OR_FLY_ANIMATION = 622 // AnimationID.HUMAN_FISHING_CASTING
        const val HARPOON_ANIMATION = 618 // AnimationID.HUMAN_HARPOON
        const val CAGE_ANIMATION = 619 // AnimationID.HUMAN_LOBSTER

        /**
         * RuneLite's AnimationID.FISHING_BAREHAND. The catch-specific follow-ups it names
         * alongside this one - 6705-6708, 6710 and 6711, a pair per fish - are not played: the
         * loop below has a single animation slot, and a reeling-in animation only reads right
         * when it follows a successful catch, which needs its own timing pass.
         */
        const val BAREHAND_ANIMATION = 6709

        const val DRAYNOR_SPOT = "npc.fishing_spot_1499" // real Net+Bait combo variant
        const val SMALL_NET_BAIT_SPOT = "npc.fishing_spot_1497" // real Small Net+Bait combo variant
        const val ROD_FISHING_SPOT = "npc.rod_fishing_spot" // real Lure+Bait variant
        const val CAGE_HARPOON_SPOT = "npc.fishing_spot_1510" // real Cage+Harpoon combo variant

        val SHRIMP = Fish("shrimps", getRSCM("item.raw_shrimps"), 1, 10.0, "You catch some shrimps.")
        val ANCHOVIES = Fish("anchovies", getRSCM("item.raw_anchovies"), 15, 40.0, "You catch some anchovies.")
        val SARDINE = Fish("sardine", getRSCM("item.raw_sardine"), 5, 20.0, "You catch a sardine.")
        val HERRING = Fish("herring", getRSCM("item.raw_herring"), 10, 30.0, "You catch a herring.")
        val PIKE = Fish("pike", getRSCM("item.raw_pike"), 25, 60.0, "You catch a pike.")
        val TROUT = Fish("trout", getRSCM("item.raw_trout"), 20, 50.0, "You catch a trout.")
        val SALMON = Fish("salmon", getRSCM("item.raw_salmon"), 30, 70.0, "You catch a salmon.")
        val RAINBOW_FISH = Fish("rainbow fish", getRSCM("item.raw_rainbow_fish"), 38, 80.0, "You catch a rainbow fish.")
        val TUNA = Fish("tuna", getRSCM("item.raw_tuna"), 35, 80.0, "You catch a tuna.")
        val LOBSTER = Fish("lobster", getRSCM("item.raw_lobster"), 40, 90.0, "You catch a lobster.")
        val SWORDFISH = Fish("swordfish", getRSCM("item.raw_swordfish"), 50, 100.0, "You catch a swordfish.")

        val NET_CATCHES = listOf(SHRIMP, ANCHOVIES)
        val SARDINE_HERRING_CATCHES = listOf(SARDINE, HERRING)
        val FULL_BAIT_CATCHES = listOf(SARDINE, HERRING, PIKE)
        val FLY_CATCHES = listOf(TROUT, SALMON, RAINBOW_FISH)
        /**
         * Barehanded tuna and swordfish: twenty Fishing levels above the harpoon versions, a
         * Strength requirement equal to the harpoon version's Fishing level, and a Strength
         * payout of a tenth of the Fishing experience. Shark is the third barehanded catch in
         * the live game (96 Fishing / 76 Strength) but is left out because harpooning cannot
         * catch one here either - it belongs with a pass that puts sharks at the spots that
         * actually have them.
         */
        val TUNA_BAREHAND =
            TUNA.copy(
                level = 55,
                strengthLevel = 35,
                strengthExperience = 8.0,
                message = "You catch a tuna with your bare hands.",
            )
        val SWORDFISH_BAREHAND =
            SWORDFISH.copy(
                level = 70,
                strengthLevel = 50,
                strengthExperience = 10.0,
                message = "You catch a swordfish with your bare hands.",
            )

        val HARPOON_CATCHES = listOf(TUNA, SWORDFISH)
        val BAREHAND_CATCHES = listOf(TUNA_BAREHAND, SWORDFISH_BAREHAND)
        val CAGE_CATCHES = listOf(LOBSTER)

        // Draynor Village riverbank (real Net+Bait: shrimp/anchovy, sardine/herring).
        val DRAYNOR_SPOTS =
            listOf(
                3086 to 3228,
                3085 to 3229,
                3085 to 3232,
                3083 to 3234,
                3086 to 3224,
            )

        // Lumbridge Swamp (east) + Catherby (real Small Net+Bait: shrimp/anchovy,
        // sardine/herring).
        val SMALL_NET_BAIT_SPOTS =
            listOf(
                3246 to 3155,
                3245 to 3152,
                3244 to 3150,
                2838 to 3431,
                2836 to 3431,
            )

        // Barbarian Village + Lumbridge River (real Lure+Bait: trout/salmon/rainbow
        // fish via fly rod, sardine/herring/pike via rod+bait).
        val ROD_LURE_BAIT_SPOTS =
            listOf(
                3110 to 3434,
                3104 to 3424,
                3239 to 3241,
                3238 to 3252,
            )

        // Catherby + Musa Point/Karamja (real Cage+Harpoon: lobster, tuna/swordfish).
        val CAGE_HARPOON_SPOTS =
            listOf(
                2853 to 3423,
                2844 to 3429,
                2926 to 3179,
                2923 to 3180,
                2925 to 3181,
            )

        // Remaining real "fishing_spot" NPC id/action combos in the cache, grouped by
        // their exact action set (verified via a throwaway cache diagnostic) and not
        // spawned anywhere yet - wired generically so a future area needs zero new
        // plugin code to add more of these same spot types.
        val BAIT_ONLY_SPOTS = listOf(2653, 2654, 2655, 4079, 4080, 4081, 4082, 4928, 6488, 6784)
        val HARPOON_AND_CAGE_SPOTS =
            listOf(1519, 1522, 1533, 2146, 3657, 3914, 5820, 7199, 7460, 7465, 7470, 7946, 9173, 9174, 10515, 10635, 12777, 14039)
        val HARPOON_ONLY_SPOTS = listOf(10565, 10568, 10569)
        val CAGE_ONLY_SPOTS = listOf(1535, 1536)
    }
}
