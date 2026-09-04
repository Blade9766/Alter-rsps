package org.alter.plugins.content.npcs.dragon

import org.alter.api.NpcSpecies
import org.alter.api.ext.inWilderness
import org.alter.api.ext.npc
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.CombatConfigs
import org.alter.plugins.content.items.jewellery.RingOfWealth
import org.alter.plugins.content.npcs.MonsterLoot
import org.alter.rscm.RSCM.getRSCM

/**
 * Wires the nine dragon species: aggression, respawn, Slayer experience, the
 * [NpcSpecies.BASIC_DRAGON] tag, dragonfire for the adults, and every drop.
 *
 * [DragonSpawnPlugin] puts them on the map; stats live in `data/cfg/npcs/monsterStats.json`, the
 * tables in [DragonDrops], ids and tiles in [Dragons], the breath in [DragonfireCombatStrategy].
 *
 * ## The species tag
 *
 * `monsterStats.json` tags every dragon here `DRACONIC` and every adult `FIERY`, but nothing in that
 * table carries [NpcSpecies.BASIC_DRAGON] - it is a combat-mechanic tag rather than a wiki
 * `attributes` value, and the generator that built the table only knows the wiki's. It is the tag
 * [org.alter.plugins.content.combat.formula.DragonfireFormula] reads to decide whether Protect from
 * Magic reduces the breath to 35%, so without it every adult dragon in the game would breathe
 * straight through the prayer. It is added at spawn, alongside whatever the table already gave the
 * npc, rather than replacing the set.
 *
 * Only the breathers get it: the formula's `basicDragon` branch is about dragonfire, and a baby
 * dragon has none.
 *
 * ## The strategy registration
 *
 * [CombatConfigs.setNpcCombatStrategy] is called from this constructor, which is what makes it
 * effective for every dragon spawned afterwards - `getCombatStrategy` consults that map on every
 * swing. It is registered per id rather than per variant so that a def-level `attackRange` on one
 * particular dragon would still be respected.
 *
 * ## Drops
 *
 * Rolled here rather than through the combat DSL's `drops { }` block, for the reason every monster
 * package in this tree repeats: that block builds a `LootTable` which `NpcDeathAction` never rolls,
 * so a green dragon killed before this file existed dropped nothing - not the dragon bones, not the
 * hide.
 */
class DragonPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        Dragons.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { npcKey ->
                if (variant.breathesFire) {
                    CombatConfigs.setNpcCombatStrategy(getRSCM(npcKey), DragonfireCombatStrategy)
                }

                onNpcSpawn(npcKey) {
                    val current = npc.combatDef
                    val aggressive = npcKey !in Dragons.PASSIVE_KEYS
                    npc.combatDef =
                        current.copy(
                            respawnDelay = variant.respawnCycles ?: current.respawnDelay,
                            aggressiveRadius = if (aggressive) Dragons.AGGRO_RADIUS else 0,
                            aggroTargetDelay = Dragons.AGGRO_SEARCH_DELAY,
                            aggressiveTimer = if (aggressive) Dragons.AGGRO_TIMER else current.aggressiveTimer,
                            slayerXp = variant.slayerXp,
                            species = if (variant.breathesFire) current.species + NpcSpecies.BASIC_DRAGON else current.species,
                        )
                }

                onNpcDeath(npcKey) { onDeath(npc, variant) }
            }
        }
    }

    /**
     * The version's 100% rows, then one roll on its table where it has one, then its tertiaries -
     * each independent of the table and of each other.
     */
    private fun onDeath(
        npc: Npc,
        variant: DragonVariant,
    ) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world
        val wealth = RingOfWealth.enhancesDropTables(killer)
        val imbued = RingOfWealth.isImbued(killer)

        val loot = mutableListOf<Pair<Int, Int>>()
        GUARANTEED.getValue(variant.dropTable).forEach { (item, amount) -> loot.add(getRSCM(item) to amount) }

        // Absent for every baby: their pages publish no main table at all. See DragonDrops.BY_LABEL.
        DragonDrops.BY_LABEL[variant.dropTable]?.roll(world, wealth)?.let { loot.add(it) }

        when (variant.dropTable) {
            "bronze" -> {
                if (world.chance(1, DragonDrops.HARD_CLUE_ONE_IN)) {
                    loot.add(getRSCM("item.clue_scroll_hard") to 1)
                }
            }
            "green", "green wilderness" -> {
                if (killer.inWilderness() && world.chance(1, DragonDrops.LOOTING_BAG_ONE_IN)) {
                    loot.add(getRSCM("item.looting_bag") to 1)
                }
                if (world.chance(1, DragonDrops.ENSOULED_HEAD_35)) {
                    loot.add(getRSCM("item.ensouled_dragon_head") to 1)
                }
                if (rollHardClue(world, imbued)) {
                    loot.add(getRSCM("item.clue_scroll_hard") to 1)
                }
            }
            "blue" -> {
                if (world.chance(1, DragonDrops.ENSOULED_HEAD_50)) {
                    loot.add(getRSCM("item.ensouled_dragon_head") to 1)
                }
                if (world.chance(1, DragonDrops.SCALY_BLUE_HIDE_ONE_IN)) {
                    loot.add(getRSCM("item.scaly_blue_dragonhide") to 1)
                }
                // The blue dragon's clue publishes no altrarity - it is a flat 1/128.
                if (world.chance(1, DragonDrops.HARD_CLUE_ONE_IN)) {
                    loot.add(getRSCM("item.clue_scroll_hard") to 1)
                }
            }
            "red" -> {
                if (world.chance(1, DragonDrops.ENSOULED_HEAD_40)) {
                    loot.add(getRSCM("item.ensouled_dragon_head") to 1)
                }
                if (world.chance(1, DragonDrops.HARD_CLUE_ONE_IN)) {
                    loot.add(getRSCM("item.clue_scroll_hard") to 1)
                }
            }
            "black", "black wilderness" -> {
                if (killer.inWilderness() && world.chance(1, DragonDrops.LOOTING_BAG_ONE_IN)) {
                    loot.add(getRSCM("item.looting_bag") to 1)
                }
                if (world.chance(1, DragonDrops.ENSOULED_HEAD_35)) {
                    loot.add(getRSCM("item.ensouled_dragon_head") to 1)
                }
                if (rollHardClue(world, imbued)) {
                    loot.add(getRSCM("item.clue_scroll_hard") to 1)
                }
                val eliteOneIn =
                    if (imbued) DragonDrops.ELITE_CLUE_WEALTH_ONE_IN else DragonDrops.ELITE_CLUE_ONE_IN
                if (world.chance(1, eliteOneIn)) {
                    loot.add(getRSCM("item.clue_scroll_elite") to 1)
                }
                val visageOneIn =
                    if (variant.dropTable == "black wilderness") {
                        DragonDrops.WILDERNESS_VISAGE_ONE_IN
                    } else {
                        DragonDrops.VISAGE_ONE_IN
                    }
                if (world.chance(1, visageOneIn)) {
                    loot.add(getRSCM("item.draconic_visage") to 1)
                }
            }
            "baby blue" -> {
                if (world.chance(1, DragonDrops.BABY_DRAGON_BONE_ONE_IN)) {
                    loot.add(getRSCM("item.baby_dragon_bone") to 1)
                }
                if (world.chance(1, DragonDrops.SCALY_BLUE_HIDE_BABY_ONE_IN)) {
                    loot.add(getRSCM("item.scaly_blue_dragonhide") to 1)
                }
            }
            // The baby red's only tertiaries are the Forthos Dungeon pages and grubby key, and the
            // baby green's and baby black's are the Konar brimstone key - all excluded for the
            // reasons DragonDrops sets out, which leaves both with the bones alone.
        }

        MonsterLoot.drop(world, killer, loot, npc.tile)
    }

    /**
     * "The hard clue scroll rarity changes to 1/64 if a ring of wealth (i) is worn" - the green and
     * black dragons' own footnote, which unlike the hellhound's names no place, so there is no
     * Wilderness half to this condition.
     */
    private fun rollHardClue(
        world: World,
        imbued: Boolean,
    ): Boolean =
        world.chance(1, if (imbued) DragonDrops.HARD_CLUE_WEALTH_ONE_IN else DragonDrops.HARD_CLUE_ONE_IN)

    private companion object {
        /**
         * The `100%` section of each drop version, by the same label [DragonVariant.dropTable] uses.
         *
         * The bronze dragon's five bronze bars are the only multi-quantity row here, and the babies'
         * single `Babydragon bones` the only one that is not a hide-and-bones pair.
         */
        val GUARANTEED: Map<String, List<Pair<String, Int>>> =
            mapOf(
                "bronze" to listOf("item.dragon_bones" to 1, "item.bronze_bar" to 5),
                "green" to listOf("item.dragon_bones" to 1, "item.green_dragonhide" to 1),
                "green wilderness" to listOf("item.dragon_bones" to 1, "item.green_dragonhide" to 1),
                "blue" to listOf("item.dragon_bones" to 1, "item.blue_dragonhide" to 1),
                "red" to listOf("item.dragon_bones" to 1, "item.red_dragonhide" to 1),
                "black" to listOf("item.dragon_bones" to 1, "item.black_dragonhide" to 1),
                "black wilderness" to listOf("item.dragon_bones" to 1, "item.black_dragonhide" to 1),
                "baby" to listOf("item.babydragon_bones" to 1),
                "baby blue" to listOf("item.babydragon_bones" to 1),
                "baby red" to listOf("item.babydragon_bones" to 1),
            )
    }
}
