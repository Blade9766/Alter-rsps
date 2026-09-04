package org.alter.plugins.content.npcs.blackheather

import org.alter.api.ext.npc
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.items.jewellery.RingOfWealth
import org.alter.plugins.content.npcs.MonsterDropTable
import org.alter.plugins.content.npcs.MonsterLoot
import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.rscm.RSCM.getRSCM

/**
 * Black Heather - one named bandit, one tile, in the Wilderness Bandit Camp.
 *
 * A package of her own rather than a row in `content/npcs/bandit`, which already owns that camp: she
 * is a **separate page** with her own level, her own stat block and a drop table that shares nothing
 * with the level 22 or level 130 bandits'. Adding her to that package would have meant a third
 * `BanditVariant` whose table is unrelated to the other two.
 *
 * Stats come from `data/cfg/npcs/monsterStats.json` (37 hitpoints, 32/26/27) and her animations are
 * now pinned - the resolver had `HUMAN_SLASH_SWORD_ATTACK` (390) as her block and
 * `HUMAN_SLASH_SWORD_DEFEND` (388) as her attack, the same armed-human swap the `BANDIT` entry in
 * `npc-animations/README.md` records.
 *
 * She is `aggressive = No`, so nothing here sets an aggression radius.
 */
class BlackHeatherPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onNpcSpawn(NPC_KEY) {
            npc.combatDef = npc.combatDef.copy(respawnDelay = RESPAWN_CYCLES, slayerXp = SLAYER_XP)
        }

        onNpcDeath(NPC_KEY) { onDeath(npc) }

        spawnNpc(npc = NPC_KEY, x = TILE.first, z = TILE.second, walkRadius = WALK_RADIUS, direction = Direction.SOUTH)
    }

    /** Guaranteed bones, one roll on the table, then her two tertiaries. */
    private fun onDeath(npc: Npc) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world

        val loot = mutableListOf(getRSCM("item.bones") to 1)
        TABLE.roll(world, RingOfWealth.enhancesDropTables(killer))?.let { loot.add(it) }

        /*
         * No `killer.inWilderness()` test, unlike most looting bags in this tree: she stands in the
         * Wilderness Bandit Camp and nowhere else, so the condition the other packages check would
         * be true on every kill.
         */
        if (world.chance(1, LOOTING_BAG_ONE_IN)) {
            loot.add(getRSCM("item.looting_bag") to 1)
        }
        val clueOneIn = if (RingOfWealth.isImbued(killer)) MEDIUM_CLUE_WEALTH_ONE_IN else MEDIUM_CLUE_ONE_IN
        if (world.chance(1, clueOneIn)) {
            loot.add(getRSCM("item.clue_scroll_medium") to 1)
        }

        MonsterLoot.drop(world, killer, loot, npc.tile)
    }

    private companion object {
        const val NPC_KEY = "npc.black_heather"

        const val COMBAT_LEVEL = 34

        /** Wiki `respawn = 50`, in game ticks. */
        const val RESPAWN_CYCLES = 50

        /** Wiki `slayxp = 37`, under `cat = Bandits` - a category not in `data/cfg/slayer/tasks.json`. */
        const val SLAYER_XP = 37.0

        /** She is one npc in a small camp; a wide radius would walk her out of it. */
        const val WALK_RADIUS = 3

        /** Her one published pin. */
        val TILE = 3039 to 3700

        const val LOOTING_BAG_ONE_IN = 4

        const val MEDIUM_CLUE_ONE_IN = 128

        /** The clue's `altrarity`, on a worn ring of wealth (i). */
        const val MEDIUM_CLUE_WEALTH_ONE_IN = 64

        private fun drop(
            item: String,
            min: Int = 1,
            max: Int = min,
            weight: Int,
        ) = WeightedDrop(getRSCM(item), min, max, weight)

        private fun coins(
            min: Int,
            weight: Int,
        ) = WeightedDrop(getRSCM("item.coins_995"), min, min, weight)

        /**
         * Her table - rows 111, herbs 15, gem 2, summing to the published 128 on the members
         * reading.
         *
         * The `Coins` section publishes seven rows, one of them - 10 coins at 15/128 - marked
         * `{{(f)}}`. Counting it the table comes to 143; dropping it, to 128 exactly. That is the
         * members reading [MonsterDropTable] documents, landing on the denominator the way it does
         * on almost every table in this tree.
         *
         * The `Swordfish` row is published `5 (noted)` and drops as five swordfish: there is no
         * note-on-drop mechanic here, the same call every other table in this tree makes.
         */
        val TABLE =
            MonsterDropTable(
                denominator = 128,
                herbWeight = 15,
                gemWeight = 2,
                rows =
                    listOf(
                        // Weapons and armour - 2.
                        drop("item.steel_longsword", weight = 1),
                        drop("item.steel_full_helm", weight = 1),
                        // Runes - 18.
                        drop("item.law_rune", 2, weight = 4),
                        drop("item.nature_rune", 4, weight = 4),
                        drop("item.body_rune", 12, weight = 3),
                        drop("item.chaos_rune", 3, weight = 3),
                        drop("item.water_rune", 30, weight = 3),
                        drop("item.mind_rune", 5, weight = 1),
                        // Coins - 76. The free-to-play-only 10-coin row at 15/128 is absent.
                        coins(48, weight = 30),
                        coins(15, weight = 18),
                        coins(8, weight = 11),
                        coins(70, weight = 10),
                        coins(5, weight = 5),
                        coins(150, weight = 2),
                        // Other - 15, including the page's own explicit Nothing row.
                        drop("item.silver_ore", weight = 11),
                        WeightedDrop(item = null, weight = 2),
                        drop("item.swordfish", 5, weight = 2),
                    ),
            )
    }
}
