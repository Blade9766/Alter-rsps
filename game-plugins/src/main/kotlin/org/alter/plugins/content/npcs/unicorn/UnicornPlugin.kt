package org.alter.plugins.content.npcs.unicorn

import org.alter.api.ext.npc
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.MonsterLoot
import org.alter.rscm.RSCM.getRSCM

/**
 * Makes unicorns worth killing. [UnicornSpawnPlugin] puts them on the map; stats live in
 * `data/cfg/npcs/monsterStats.json` and ids and tiles in [Unicorns].
 *
 * ## What this changes, per unicorn
 *
 * One field, layered onto the def the engine already built: **`respawnDelay`** 25 ->
 * [Unicorns.RESPAWN_CYCLES]. Read by `NpcDeathAction` at death, so patching it at spawn is enough.
 *
 * There is deliberately **no aggression patch** - both versions are `aggressive = No` - and **no
 * `slayerXp`**, because there is no unicorn Slayer category: `data/cfg/slayer/tasks.json` has none
 * and no master's assignment table names one, which matches the wiki, where neither page carries a
 * `cat`.
 *
 * The patch is applied in an `onNpcSpawn` hook rather than once at load because a unicorn that dies
 * is re-defaulted: `NpcDeathAction` calls `World.setNpcDefaults` and then `executeNpcSpawn` again,
 * which would otherwise hand the respawned unicorn back its unpatched def.
 *
 * ## Drops
 *
 * The adult drops **bones and a unicorn horn, both 100%**, which is the whole reason anyone kills
 * one: the horn is what `content/skills/herblore` grinds into unicorn horn dust for antipoisons.
 * The foal drops bones only, which its page states outright. Neither has a weighted table, which is
 * why this package has no `*Drops.kt`.
 *
 * Rolled here rather than through the combat DSL's `drops { }` block, for the reason every monster
 * package in this tree repeats: that block builds a `LootTable` which `NpcDeathAction` never rolls,
 * so a unicorn killed before this file existed dropped no horn.
 *
 * ### What is not modelled
 *
 * **Unicorn bone**, published as `Always` but "dropped only during Rag and Bone Man I" - a quest
 * this server does not have. Unlike a 1/4 tertiary this one is guaranteed, so handing it out
 * unconditionally would put a quest item in every player's inventory; it drops never rather than
 * always, the same call `content/npcs/zombie` makes about the zombie bone.
 */
class UnicornPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        Unicorns.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { npcKey ->
                onNpcSpawn(npcKey) {
                    npc.combatDef = npc.combatDef.copy(respawnDelay = Unicorns.RESPAWN_CYCLES)
                }

                onNpcDeath(npcKey) { onDeath(npc, variant) }
            }
        }
    }

    private fun onDeath(
        npc: Npc,
        variant: UnicornVariant,
    ) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world

        val loot = mutableListOf(getRSCM("item.bones") to 1)

        if (variant.adult) {
            loot.add(getRSCM("item.unicorn_horn") to 1)

            if (world.chance(1, ENSOULED_HEAD_ONE_IN)) {
                loot.add(getRSCM("item.ensouled_unicorn_head") to 1)
            }
        }

        loot.forEach { (item, amount) ->
            MonsterLoot.drop(world, killer, item, amount, npc.tile)
        }
    }

    private companion object {
        /**
         * Wiki tertiary, 1/35, on the adult only.
         *
         * Kept at its real rate even though there is no Arceuus reanimation spell to use the head
         * on yet - the same call `content/npcs/goblin` makes about the goblin champion scroll, and
         * for the same reason: faking a rarity to hide a missing system is worse than dropping an
         * item that currently only sits in a bank.
         */
        const val ENSOULED_HEAD_ONE_IN = 35
    }
}
