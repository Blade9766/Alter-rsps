package org.alter.plugins.content.areas.warriorsguild.activities

import org.alter.api.Skills
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.areas.warriorsguild.WarriorsGuild

/**
 * The shot put room: throw a weight as far as you can for Strength experience and tokens.
 *
 * Behind the heavy doors, which [org.alter.plugins.content.areas.warriorsguild.WarriorsGuildPlugin]
 * gates at [WarriorsGuild.SHOT_PUT_STRENGTH] Strength. Two throwable shots sit in the room, one
 * light and one heavy, and each `Throw` picks a distance; the further it goes the more it pays.
 *
 * ## The model, and how much of it is sourced
 *
 * The wiki states the shape of the activity and its ceiling but not its table: "the farther a
 * successful shot put is thrown, the more Strength experience and warrior guild tokens are
 * earned", three throw styles, a 50 Strength requirement, and that the best rate is "the 22lb shot
 * with the 'Standing throw' style at 100% run energy". Everything numeric beyond the token ranges
 * -- 2-15 for the 18lb shot and 4-17 for the 22lb -- lives on a calculator page rather than in
 * prose.
 *
 * So the distance model here is **this project's**, built to land on the published endpoints:
 *
 * - Distance is a roll scaled by Strength level and by run energy, the two inputs the wiki names.
 * - Tokens are that distance mapped onto the shot's published range, so a bad throw with the 18lb
 *   pays 2 and a perfect throw with the 22lb pays 17.
 * - Strength experience is [XP_PER_TOKEN] times the tokens, which keeps the two rewards moving
 *   together the way the wiki describes without inventing a second table.
 *
 * Run energy is **spent** on the throw, which is what makes the wiki's advice ("recovering the
 * energy used in-between every throw") mean anything at all.
 */
class ShotPutPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        /*
         * Which pile is which is settled by the cache's own examine text, not by position:
         * `data/cfg/locs.csv` has 15664 as "It's a pile of 18lb shot." and 15665 as "It's a pile
         * of 22lb shot." Both objects are simply named "Shot", so the examine line is the only
         * thing that tells them apart - and getting it backwards would have paid the heavy shot's
         * rate for the light one.
         */
        SHOTS.forEach { (obj, shot) ->
            onObjOption(obj = obj, option = "throw", lineOfSightDistance = 1) {
                throwShot(player, shot)
            }
        }
    }

    private fun throwShot(
        player: Player,
        shot: Shot,
    ) {
        val strength = player.getSkills().getBaseLevel(Skills.STRENGTH)
        if (strength < WarriorsGuild.SHOT_PUT_STRENGTH) {
            player.message("You need ${WarriorsGuild.SHOT_PUT_STRENGTH} Strength to throw the shot.")
            return
        }
        if (player.runEnergy < ENERGY_COST) {
            player.message("You are too tired to throw the shot.")
            return
        }

        player.runEnergy -= ENERGY_COST

        /*
         * Three inputs, each in 0..1: how far past the entry requirement the player's Strength is,
         * how much energy they had, and the throw itself. Averaging them means neither a maxed
         * Strength level nor full energy alone guarantees a good throw, which is what keeps the
         * activity from being a single number.
         */
        val levelFactor = ((strength - WarriorsGuild.SHOT_PUT_STRENGTH).toDouble() / LEVEL_SPAN).coerceIn(0.0, 1.0)
        val energyFactor = (player.runEnergy / MAX_ENERGY).coerceIn(0.0, 1.0)
        val quality = (levelFactor + energyFactor + world.randomDouble()) / 3.0

        val tokens = shot.minTokens + Math.round(quality * (shot.maxTokens - shot.minTokens)).toInt()
        val xp = tokens * XP_PER_TOKEN

        player.addXp(Skills.STRENGTH, xp)
        player.inventory.add(WarriorsGuild.TOKEN, tokens)
        player.message("You hurl the ${shot.label} shot ${describe(quality)}.")
    }

    private fun describe(quality: Double): String =
        when {
            quality >= 0.8 -> "the length of the room"
            quality >= 0.5 -> "a good distance"
            quality >= 0.25 -> "a fair way"
            else -> "barely past your feet"
        }

    /** The two shots, with the token ranges the wiki publishes for each. */
    private enum class Shot(
        val label: String,
        val minTokens: Int,
        val maxTokens: Int,
    ) {
        LIGHT("18lb", 2, 15),
        HEAVY("22lb", 4, 17),
    }

    private companion object {
        /** Keyed off the examine text - see the binding above. */
        val SHOTS = listOf("object.shot_15664" to Shot.LIGHT, "object.shot_15665" to Shot.HEAVY)

        /** Strength above the entry requirement at which throws stop improving. */
        const val LEVEL_SPAN = 49.0

        const val MAX_ENERGY = 100.0

        /** Enough that throwing back to back drains a player, as the wiki's advice implies. */
        const val ENERGY_COST = 10

        /**
         * Strength experience per token.
         *
         * Chosen rather than sourced - see the class comment. It ties the two rewards together so
         * that the better throw pays better in both, which is all the wiki actually claims.
         */
        const val XP_PER_TOKEN = 5.0
    }
}
