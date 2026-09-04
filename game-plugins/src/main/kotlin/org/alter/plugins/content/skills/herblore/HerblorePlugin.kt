package org.alter.plugins.content.skills.herblore

import dev.openrune.cache.CacheManager.getItem
import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.api.cfg.Sound
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.move.hasMoveDestination
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.queue.TaskPriority
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.items.jewellery.JewelleryPerks

/**
 * Herblore: cleaning herbs, dropping them into vials of water, and mixing the result into
 * something drinkable.
 *
 * Levels, experience and ingredients are the OSRS Wiki's, loaded from
 * `data/cfg/herblore/herbs.json` and `data/cfg/herblore/potions.json` through
 * [HerbloreService]. The main Herblore article's two tables were read for this - the herb
 * table for cleaning levels, cleaning experience and the level each unfinished potion needs,
 * and the potion table for every potion's level, experience, unfinished base and secondary.
 * Grinding and coconut milk live next door in [HerbloreGrindingPlugin], which is where the
 * dusts most of these recipes want come from.
 *
 * **The three steps.** Cleaning is one grimy herb per click, instantly, for a little
 * experience; there is no animation for it in OSRS and none is played here. Adding a clean
 * herb to a vial of water makes an unfinished potion in one tick and pays **nothing** -
 * every point of a potion's experience arrives at the last step. Adding the secondary to
 * the unfinished potion takes two ticks and pays the whole amount.
 *
 * **All of it is boostable.** The wiki's recipe blocks mark Herblore mixing
 * `skill1boostable = Yes` (prayer potion, saradomin brew, super combat, the divine potions
 * - all of them), so every check here is against the *current* level rather than the base
 * one. Attack potion's block is the lone page saying otherwise, which reads as a
 * transcription slip rather than a rule.
 *
 * **Per-dose potions work at any dose**, which is the one piece of real mechanism in this
 * plugin. An amylase crystal per dose turns super energy into stamina; a lava scale shard
 * per dose extends an antifire; five of Zulrah's scales per dose turn antidote++ into
 * anti-venom; twenty ancient essence per dose turn an ancient brew into a forgotten one;
 * one crystal dust per dose makes any of the divine potions. Both the ingredient cost and
 * the experience scale with the dose, so a 4-dose stamina potion costs four crystals and
 * pays 102 - the figure the wiki quotes as "4 × 25.5". See [PotionRecipe] for how the
 * config expresses that.
 *
 * **Recipes wanting more than two items** - a super combat potion's three supers plus a
 * torstol, a sanfew serum's three secondaries - are written out once per ingredient that
 * can start them, so clicking any of them onto the base works, as it does in OSRS.
 *
 * **Deliberately not implemented:** the amulet of chemistry and alchemist's amulet, which
 * give a chance of a 4-dose potion and would need charge tracking; Guthix rest tea, whose
 * base is a cup of hot water rather than a vial; Guthix balance, whose silver dust comes
 * from the Ectofuntus bone grinder rather than a pestle; and the Sailing-era coral potions,
 * the surge potion and the extended stamina potion, whose secondaries (yellow fin, crab
 * paste, demonic tallow, marlin scales) have no item in this cache. Potions are made at
 * three doses, as in OSRS - decanting is a Grand Exchange service, not a Herblore action,
 * so using one potion on another does nothing here yet.
 *
 * A sanfew serum takes all three of its secondaries in one step rather than paying partial
 * experience for each, which is the one simplification of substance: the cache has no
 * intermediate item to hold a half-built serum, so there is nothing for a partial step to
 * produce.
 *
 * Deliberately does not call [org.alter.game.model.entity.Pawn.lock], for the reason
 * documented on `WoodcuttingPlugin`: a full lock stops `walkTo()` from processing a
 * movement click, stranding the player mid-skill.
 */
class HerblorePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        loadService(HerbloreService())

        onWorldInit {
            val service = world.getService(HerbloreService::class.java) ?: return@onWorldInit

            service.herbs.forEach { herb ->
                onItemOption(item = herb.grimy, option = "Clean") { clean(player, herb) }
            }

            service.mixesByPair().forEach { (pair, group) ->
                val (first, second) = pair
                onItemOnItem(first, second) {
                    player.queue(TaskPriority.STANDARD) { make(this, player, group) }
                }
            }
        }
    }

    /**
     * Rubs the dirt off one herb. Instant and one at a time, as in OSRS - the herb sack and
     * the Master farmer's clean-all are separate features, not this.
     */
    private fun clean(
        player: Player,
        herb: HerbEntry,
    ) {
        if (player.getSkills().getCurrentLevel(Skills.HERBLORE) < herb.cleanLevel) {
            player.message("You need a Herblore level of ${herb.cleanLevel} to clean this herb.")
            return
        }

        val slot = player.getInteractingSlot()
        if (player.inventory.remove(item = herb.grimyItemId, amount = 1, beginSlot = slot).hasFailed()) {
            return
        }
        player.inventory.add(item = herb.cleanItemId, amount = 1, beginSlot = slot)
        player.playSound(Sound.HERBLORE_CLEAN_HERB_1)
        player.addXp(Skills.HERBLORE, herb.cleanExperience)
    }

    /**
     * Opens the "how many?" chatbox for whatever the clicked pair makes, then mixes that
     * many.
     *
     * [group] holds every mix behind this pair. It is normally a single entry, and the box
     * still opens for it - that is what turns an inventory of unfinished potions into one
     * action rather than fourteen clicks. Where a pair really does make more than one thing
     * the box becomes a choice, the same way flour on water offers four doughs in Cooking.
     */
    private suspend fun make(
        task: QueueTask,
        player: Player,
        group: List<PotionMix>,
    ) {
        // Anything the player can't hold the ingredients for is left out of the box
        // entirely. Clicking a torstol onto a super attack(4) with no super strength to go
        // with it lands here, so say what is missing rather than doing nothing at all.
        val available = group.filter { affordable(player, it) > 0 }
        if (available.isEmpty()) {
            val missing = shortfall(player, group.first())
            player.message("You need ${getItem(missing).name.lowercase()} to make that.")
            return
        }

        // Checked before the box opens rather than after: clicking red spiders' eggs onto a
        // snapdragon potion at level 1 should say so straight away rather than after picking
        // a quantity. Only when there is a single thing to make, since a real choice would
        // have a level apiece.
        if (available.size == 1 && player.getSkills().getCurrentLevel(Skills.HERBLORE) < available.single().level) {
            player.message("You need a Herblore level of ${available.single().level} to make this.")
            return
        }

        var chosen: PotionMix? = null
        var requested = 0

        task.produceItemBox(
            player,
            *available.map { it.productId }.toIntArray(),
            title = "What would you like to make?",
            maxProducable = available.maxOf { affordable(player, it) },
        ) { item, qty ->
            chosen = available.firstOrNull { it.productId == item }
            requested = qty
        }

        val mix = chosen ?: return
        if (requested <= 0) {
            return
        }
        if (player.getSkills().getCurrentLevel(Skills.HERBLORE) < mix.level) {
            player.message("You need a Herblore level of ${mix.level} to make this.")
            return
        }

        var made = 0
        while (made < requested && !player.hasMoveDestination()) {
            if (affordable(player, mix) <= 0) {
                break
            }

            player.animate(Animation.HERBLORE_POTION_MAKING)
            player.playSound(Sound.VIAL_MIX)
            task.wait(mix.ticks)

            // Re-checked after the wait: the player may have dropped or banked an
            // ingredient while the animation was playing.
            if (!combine(player, mix)) {
                break
            }
            made++
        }

        if (made > 0) {
            player.animate(-1)
        }
    }

    /**
     * How many of [mix] the player could make right now, from the ingredients they hold.
     */
    private fun affordable(
        player: Player,
        mix: PotionMix,
    ): Int {
        var possible = player.inventory.getItemCount(mix.baseId)
        possible = minOf(possible, player.inventory.getItemCount(mix.secondaryId) / mix.secondaryAmount)
        mix.extraIds.forEach { possible = minOf(possible, player.inventory.getItemCount(it)) }
        return possible
    }

    /**
     * The first ingredient of [mix] the player hasn't got enough of, for the message when
     * nothing can be made. The bound pair is skipped: the player just clicked those two
     * together, so what they are short of is a quantity or one of the [PotionMix.extraIds] -
     * the other two supers a super combat potion wants, or the fifth of Zulrah's scales.
     */
    private fun shortfall(
        player: Player,
        mix: PotionMix,
    ): Int =
        mix.extraIds.firstOrNull { !player.inventory.contains(it) }
            ?: mix.secondaryId.takeIf { player.inventory.getItemCount(it) < mix.secondaryAmount }
            ?: mix.baseId

    /** Returns false when the mix couldn't be made, so a repeated run stops early. */
    private fun combine(
        player: Player,
        mix: PotionMix,
    ): Boolean {
        // The removals aren't atomic, so anything already taken goes back if a later one
        // fails; a half-applied recipe would quietly destroy a ranarr weed's worth of work.
        val removed = mutableListOf<Pair<Int, Int>>()
        val needed =
            listOf(mix.baseId to 1, mix.secondaryId to mix.secondaryAmount) +
                mix.extraIds.map { it to 1 }

        needed.forEach { (id, amount) ->
            if (player.inventory.remove(item = id, amount = amount, assureFullRemoval = true).hasFailed()) {
                removed.forEach { (back, n) -> player.inventory.add(item = back, amount = n) }
                return false
            }
            removed += id to amount
        }

        // An amulet of chemistry occasionally makes the potion a four-dose one instead.
        player.inventory.add(item = JewelleryPerks.potionYield(player, mix.productId), amount = 1)

        // OSRS words the two steps differently - a herb goes *into* the vial, a secondary is
        // *mixed into* the potion - and both lines name the ingredient rather than the
        // product. An unfinished potion is the only mix that pays nothing, so its zero
        // experience is what tells the two apart. The base is named rather than assumed to
        // be a vial of water, since four of these are mixed into coconut milk and one into
        // a vial of blood.
        val ingredient = getItem(mix.secondaryId).name.lowercase()
        if (mix.experience > 0.0) {
            player.addXp(Skills.HERBLORE, mix.experience)
            player.message("You mix the $ingredient into your potion.")
        } else {
            player.message("You put the $ingredient into the ${getItem(mix.baseId).name.lowercase()}.")
        }
        return true
    }
}
