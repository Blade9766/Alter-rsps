package org.alter.plugins.content.areas.warriorsguild.activities

import org.alter.api.Skills
import org.alter.api.cfg.Animation
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.combat.AttackStyle
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.areas.warriorsguild.WarriorsGuild
import org.alter.plugins.content.combat.CombatConfigs

/**
 * Ajjat's dummy room: seven dummies, each of which will only take a hit from one attack style.
 *
 * Hitting a dummy with the style it wants pays [TOKENS] warrior guild tokens and [ATTACK_XP]
 * Attack experience. Hitting it with anything else stuns the player for [STUN_TICKS] ticks - the
 * activity's whole cost, and the reason it is not simply free experience.
 *
 * ## Where the seven come from
 *
 * The wiki's dummy page carries a style tab per dummy - Accurate, Slash, Aggressive, Controlled,
 * Crush, Stab, Defensive - and that is the set modelled here. Note that they are two different
 * kinds of thing: Accurate, Aggressive, Controlled and Defensive are the *button* on the attack
 * tab, while Stab, Slash and Crush are the *attack type* the weapon rolls with. A player switching
 * between them may need to change weapon, not just style, which is exactly what the room is for.
 *
 * **Which dummy wants which style is this project's assignment.** The wiki lists the seven
 * requirements but does not tie them to individual dummies, and the cache cannot settle it either:
 * see [WarriorsGuild] for why the dummies are spawned onto hole tiles rather than read from the
 * map. The seven requirements are all present exactly once, which is the part that matters.
 */
class DummyRoomPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        DUMMIES.forEachIndexed { index, requirement ->
            val obj = "object.dummy_${FIRST_DUMMY_ID + index}"
            val tile = WarriorsGuild.DUMMY_TILES[index]

            spawnObj(obj = obj, tile = tile)

            onObjOption(obj = obj, option = "hit", lineOfSightDistance = 1) {
                hit(player, requirement)
            }
        }
    }

    private fun hit(
        player: Player,
        requirement: DummyStyle,
    ) {
        if (!requirement.matches(player)) {
            player.message("You hit the dummy with the wrong style, and it swings back at you.")
            player.animate(Animation.HUMAN_PUNCH)
            player.stun(STUN_TICKS)
            return
        }

        player.animate(CombatConfigs.getAttackAnimation(player))
        player.addXp(Skills.ATTACK, ATTACK_XP)
        player.inventory.add(WarriorsGuild.TOKEN, TOKENS)
        player.message("You strike the dummy cleanly. It wants a ${requirement.label} hit.")
    }

    /**
     * One dummy's requirement.
     *
     * Modelled as a sealed pair rather than one enum because the two halves are genuinely
     * different values in this engine - [AttackStyle] is the attack-tab button and [CombatStyle]
     * is the attack type - and flattening them into one list of strings would mean re-deriving
     * which kind each is at every check.
     */
    private sealed interface DummyStyle {
        val label: String

        fun matches(player: Player): Boolean

        data class Button(val style: AttackStyle, override val label: String) : DummyStyle {
            override fun matches(player: Player): Boolean = CombatConfigs.getAttackStyle(player) == style
        }

        data class Type(val style: CombatStyle, override val label: String) : DummyStyle {
            override fun matches(player: Player): Boolean = CombatConfigs.getCombatStyle(player) == style
        }
    }

    private companion object {
        /** Object ids 23958 through 23964, all named "Dummy" with a single `Hit` action. */
        const val FIRST_DUMMY_ID = 23958

        const val TOKENS = 2
        const val ATTACK_XP = 15.0

        /** The wiki: the wrong style "causes the player to be unable to move for 3 ticks". */
        const val STUN_TICKS = 3

        val DUMMIES =
            listOf(
                DummyStyle.Button(AttackStyle.ACCURATE, "accurate"),
                DummyStyle.Button(AttackStyle.AGGRESSIVE, "aggressive"),
                DummyStyle.Button(AttackStyle.CONTROLLED, "controlled"),
                DummyStyle.Button(AttackStyle.DEFENSIVE, "defensive"),
                DummyStyle.Type(CombatStyle.STAB, "stabbing"),
                DummyStyle.Type(CombatStyle.SLASH, "slashing"),
                DummyStyle.Type(CombatStyle.CRUSH, "crushing"),
            )
    }
}
