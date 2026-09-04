package org.alter.plugins.content.combat.specialattack.weapons.dragonbattleaxe

import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.specialattack.SpecialAttacks

/**
 * Dragon battleaxe - **Rampage** (100%).
 *
 * The odd one out among the specials: it attacks nobody. It burns the whole bar to drain a tenth
 * off each of Attack, Defence, Ranged and Magic and pay a Strength boost back out of what it took,
 * which is why it is registered with `executeInstantly` - the spec bar fires it on the spot with a
 * null target rather than waiting for something to hit.
 *
 * That also makes it the only Strength boost in the game that is not drunk, and the reason players
 * carry one they never swing: hit Rampage, switch back to the real weapon, keep the levels.
 *
 * The boost is `10 + floor(levelsDrained / 4)` over the *base* Strength level, so it does not stack
 * on top of a super strength dose - the higher of the two wins, and the cap is the same
 * "base plus the boost" every other boost in the game uses.
 */
class DragonBattleaxePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Rampage", executeInstantly = true) {
            rampage(player)
        }
    }

    private fun rampage(player: Player) {
        player.animate(Animation.DRAGON_BATTLEAXE_SPECIAL)
        player.graphic(Graphic.DRAGON_BATTLEAXE_SPECIAL)
        player.forceChat(BATTLE_CRY)

        val skills = player.getSkills()

        /*
         * Every drain comes off the *current* level, so a player who has already potioned up loses
         * more and gets more back - which is the whole reason the 21-level maximum needs boosted
         * stats to reach. The drains are taken first and totalled, because the boost is paid out of
         * them.
         */
        var drained = 0
        DRAINED_SKILLS.forEach { skill ->
            val current = skills.getCurrentLevel(skill)
            val loss = current * DRAIN_PERCENT / 100
            if (loss > 0) {
                skills.setCurrentLevel(skill, current - loss)
                drained += loss
            }
        }

        val boost = BASE_BOOST + drained / DRAINED_PER_LEVEL
        val base = skills.getBaseLevel(Skills.STRENGTH)
        val cap = base + boost
        val current = skills.getCurrentLevel(Skills.STRENGTH)
        if (current < cap) {
            skills.setCurrentLevel(Skills.STRENGTH, cap)
        }

        player.message("You feel a surge of strength.")
    }

    private companion object {
        /** A tenth off each of the four, floored. */
        val DRAINED_SKILLS = listOf(Skills.ATTACK, Skills.DEFENCE, Skills.RANGED, Skills.MAGIC)
        const val DRAIN_PERCENT = 10

        /** `10 + floor(drained / 4)`, which tops out at 21 with the stats boosted first. */
        const val BASE_BOOST = 10
        const val DRAINED_PER_LEVEL = 4

        /** Forced chat, exactly as the wiki transcribes it. */
        const val BATTLE_CRY = "Raarrrrrgggggghhhhhhh!"
    }
}
