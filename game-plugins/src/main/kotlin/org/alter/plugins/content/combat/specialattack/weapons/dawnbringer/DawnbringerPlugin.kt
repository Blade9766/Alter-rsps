package org.alter.plugins.content.combat.specialattack.weapons.dawnbringer

import org.alter.api.cfg.Animation
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.specialattack.dealMagicSpecialHit

/**
 * Dawnbringer - **Pulsate**: 75 to 150 damage, guaranteed.
 *
 * The single most damaging attack in the game, and deliberately unaffected by anything the player
 * is wearing or praying - magic damage bonuses, prayer and the Magic level itself all do nothing to
 * it, so the roll is flat.
 *
 * In the real game it only works against Verzik Vitur, and the wand is only obtainable inside the
 * Theatre of Blood. Neither exists in this codebase, so the target restriction is not enforced -
 * gating on a raid that is not here would mean gating on nothing.
 */
class DawnbringerPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        SpecialAttacks.registerByName("Pulsate") {
            player.animate(Animation.NIGHTMARE_STAFF_SPECIAL)
            player.dealMagicSpecialHit(
                target = target,
                damage = world.random(MIN_DAMAGE..MAX_DAMAGE),
                landHit = true,
            )
        }
    }

    private companion object {
        const val MIN_DAMAGE = 75
        const val MAX_DAMAGE = 150
    }
}
