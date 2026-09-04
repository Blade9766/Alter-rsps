package org.alter.plugins.content.npcs.ice

import org.alter.api.ext.inWilderness
import org.alter.api.ext.npc
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.items.jewellery.RingOfWealth
import org.alter.plugins.content.npcs.MonsterLoot
import org.alter.rscm.RSCM.getRSCM

/**
 * Makes ice warriors and ice giants aggressive, worth Slayer experience and worth killing.
 * [IceSpawnPlugin] puts them on the map; stats live in `data/cfg/npcs/monsterStats.json`, the tables
 * and rates in [IceCreatures].
 *
 * Both species had **empty `monsters` lists** in their `data/cfg/slayer/tasks.json` categories -
 * `Ice giants` and `Ice warriors` both existed as assignable tasks naming nothing, which
 * `SlayerService.markAvailable` reads as "not assignable". Both lists name their monster now, and
 * the experience is real, which is the same double fix `content/npcs/mossgiant` had to make.
 */
class IcePlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        IceCreatures.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { npcKey ->
                onNpcSpawn(npcKey) {
                    npc.combatDef =
                        npc.combatDef.copy(
                            respawnDelay = IceCreatures.RESPAWN_CYCLES,
                            aggressiveRadius = IceCreatures.AGGRO_RADIUS,
                            aggroTargetDelay = IceCreatures.AGGRO_SEARCH_DELAY,
                            aggressiveTimer = IceCreatures.AGGRO_TIMER,
                            slayerXp = variant.slayerXp,
                        )
                }

                onNpcDeath(npcKey) { onDeath(npc, variant) }
            }
        }
    }

    private fun onDeath(
        npc: Npc,
        variant: IceVariant,
    ) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world
        val wealth = RingOfWealth.enhancesDropTables(killer)

        val loot = mutableListOf<Pair<Int, Int>>()

        if (variant.giant) {
            loot.add(getRSCM("item.big_bones") to 1)
            IceCreatures.GIANT_TABLE.roll(world, wealth)?.let { loot.add(it) }
            giantTertiaries(killer, world, loot)
        } else {
            // No 100% section at all: an ice warrior is animated armour and leaves no bones.
            IceCreatures.WARRIOR_TABLE.roll(world, wealth)?.let { loot.add(it) }
            warriorTertiaries(killer, world, loot)
        }

        MonsterLoot.drop(world, killer, loot, npc.tile)
    }

    private fun warriorTertiaries(
        killer: Player,
        world: World,
        loot: MutableList<Pair<Int, Int>>,
    ) {
        if (killer.inWilderness() && world.chance(1, IceCreatures.LOOTING_BAG_ONE_IN)) {
            loot.add(getRSCM("item.looting_bag") to 1)
        }
        /*
         * "Increases to 1/64 if a ring of wealth (i) is worn and fought in the Wilderness" - both
         * halves required, which is why this checks the imbued ring specifically as well as the
         * position. Two of the five ice warrior camps are on the Frozen Waste Plateau, so the
         * condition really can be met.
         */
        val clueOneIn =
            if (RingOfWealth.isImbued(killer) && killer.inWilderness()) {
                IceCreatures.MEDIUM_CLUE_WEALTH_ONE_IN
            } else {
                IceCreatures.MEDIUM_CLUE_ONE_IN
            }
        if (world.chance(1, clueOneIn)) {
            loot.add(getRSCM("item.clue_scroll_medium") to 1)
        }
    }

    private fun giantTertiaries(
        killer: Player,
        world: World,
        loot: MutableList<Pair<Int, Int>>,
    ) {
        if (killer.inWilderness() && world.chance(1, IceCreatures.LOOTING_BAG_ONE_IN)) {
            loot.add(getRSCM("item.looting_bag") to 1)
        }
        if (world.chance(1, IceCreatures.GIANT_ENSOULED_HEAD_ONE_IN)) {
            loot.add(getRSCM("item.ensouled_giant_head") to 1)
        }
        if (world.chance(1, IceCreatures.GIANT_BEGINNER_CLUE_ONE_IN)) {
            loot.add(getRSCM("item.clue_scroll_beginner") to 1)
        }
        if (world.chance(1, IceCreatures.GIANT_LONG_BONE_ONE_IN)) {
            loot.add(getRSCM("item.long_bone") to 1)
        }
        if (world.chance(1, IceCreatures.GIANT_CHAMPION_SCROLL_ONE_IN)) {
            loot.add(getRSCM("item.giant_champion_scroll") to 1)
        }
        // 1/5012.5, a non-integer rate, so this cannot go through World.chance.
        if (world.randomDouble() < 1.0 / IceCreatures.GIANT_CURVED_BONE_ONE_IN) {
            loot.add(getRSCM("item.curved_bone") to 1)
        }
    }
}
