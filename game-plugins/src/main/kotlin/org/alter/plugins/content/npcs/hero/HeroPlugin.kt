package org.alter.plugins.content.npcs.hero

import org.alter.api.ext.npc
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.MonsterLoot
import org.alter.plugins.content.npcs.SpawnDealer
import org.alter.plugins.content.npcs.SpawnFacings
import org.alter.rscm.RSCM.getRSCM

/**
 * The heroes of Ardougne Market - three ids, three pins, and the best pickpocket in the game.
 *
 * One plugin rather than three files: one location, three pins, and a `Drops` section with exactly
 * one row in it. Stats come from `data/cfg/npcs/monsterStats.json` (82 hitpoints, 54/55/54, 87
 * stab defence, attack speed 5).
 *
 * ## Nearly all of a hero's value is in the Thieving table, and that already exists
 *
 * `data/cfg/thieving/pickpockets.json` carries the hero's pickpocket entry - level 80, 163.3
 * experience, the coin pouch and the death runes. This package deliberately does not touch it: what
 * was missing was the *monster*, which had no spawns, no respawn and no drop handler at all.
 *
 * Killing one drops bones and nothing else, which is the whole of the page's `Drops` section.
 *
 * ## They were parrying when they meant to swing
 *
 * The animation resolver had `HUMAN_BLUNT_DEFEND2` (403) as the attack and
 * `HUMAN_SLASH_SWORD_ATTACK` (390) as the block - the armed-human swap the `BANDIT` entry in
 * `npc-animations/README.md` records, on another name. Pinned as 390 / 403 / 836.
 *
 * ## Aggression
 *
 * `aggressive = No, unless caught stealing from the gem stall`. The exemption's *condition* is the
 * interesting half and it is not modelled: nothing in `content/skills/thieving/stall` tells a nearby
 * npc that a theft was seen. So a hero is wired plainly passive, which is what a player who is not
 * stealing experiences, rather than aggressive to everyone - the same call `content/npcs/dungeon`
 * makes about the suit of armour's conditional aggression.
 *
 * ## The Thieving test area
 *
 * `content/areas/thieving-test/spawns` places one `npc.hero_3295` of its own for stall testing. It
 * is left alone: it stands nowhere near Ardougne Market, so it clashes with none of these pins, and
 * it now gets this plugin's respawn and drops like any other hero.
 */
class HeroPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        NPC_KEYS.forEach { npcKey ->
            onNpcSpawn(npcKey) {
                npc.combatDef = npc.combatDef.copy(respawnDelay = RESPAWN_CYCLES)
            }

            onNpcDeath(npcKey) { onDeath(npc) }
        }

        val dealer = SpawnDealer()
        TILES.forEachIndexed { index, (x, z) ->
            spawnNpc(
                npc = dealer.next(NPC_KEYS),
                x = x,
                z = z,
                walkRadius = WALK_RADIUS,
                direction = SpawnFacings.at(index),
            )
        }
    }

    private fun onDeath(npc: Npc) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        MonsterLoot.drop(npc.world, killer, getRSCM("item.bones"), 1, npc.tile)
    }

    private companion object {
        /** `id1`..`id3`, all one version at level 69 - they differ only in appearance. */
        val NPC_KEYS = listOf("npc.hero_3295", "npc.hero_11934", "npc.hero_11935")

        const val COMBAT_LEVEL = 69

        /** Wiki `respawn = 50`, in game ticks. */
        const val RESPAWN_CYCLES = 50

        /** They wander a busy market; a small radius keeps them near their stalls. */
        const val WALK_RADIUS = 4

        /** The one published `LocLine`, plane 0. */
        val TILES = listOf(2630 to 3288, 2647 to 3306, 2667 to 3316)
    }
}
