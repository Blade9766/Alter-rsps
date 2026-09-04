package org.alter.plugins.content.npcs.mossgiant

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
import org.alter.plugins.content.skills.slayer.Slayer
import org.alter.rscm.RSCM.getRSCM

/**
 * Makes moss giants aggressive, worth Slayer experience and worth killing. [MossGiantSpawnPlugin]
 * puts them on the map; stats live in `data/cfg/npcs/monsterStats.json`, the table in
 * [MossGiantDrops], ids and tiles in [MossGiants].
 *
 * ## What this changes, per giant
 *
 * Layered onto the def the engine already built, the pattern
 * `areas/wilderness/bosses/WildernessBossPlugin` documents:
 *
 * - **`respawnDelay`** 25 -> [MossGiants.RESPAWN_CYCLES]. Read by `NpcDeathAction` at death, so
 *   patching it at spawn is enough.
 * - **`aggressiveRadius`**, **`aggroTargetDelay`** and **`aggressiveTimer`**. Every version is
 *   `aggressive = Yes`, and all three fields are needed: without a radius `NpcAggroPlugin` never
 *   arms its timer, and without a non-zero timer it arms one that refuses every target.
 * - **`slayerXp`**, which `Slayer.onKill` reads off the dying npc and is the only place Slayer
 *   experience comes from. Zeroed before this, so `data/cfg/slayer/tasks.json`'s `Moss giants`
 *   category - which shipped with an **empty** monster list, and which
 *   `SlayerService.markAvailable` therefore read as "not assignable" - was doubly dead. Both halves
 *   are fixed: the list now names the monster and the experience is real.
 *
 * The patch is applied in an `onNpcSpawn` hook rather than once at load because a giant that dies is
 * re-defaulted: `NpcDeathAction` calls `World.setNpcDefaults` and then `executeNpcSpawn` again,
 * which would otherwise hand the respawned giant back its unpatched def. Per-npc spawn hooks run
 * before global ones, so both `NpcAggroPlugin` and
 * [org.alter.plugins.content.npcs.animations.MonsterAnimationsPlugin] see the patched version.
 *
 * ## Drops
 *
 * Rolled here rather than through the combat DSL's `drops { }` block, for the reason every monster
 * package in this tree repeats: that block builds a `LootTable` which `NpcDeathAction` never rolls,
 * so a moss giant killed before this file existed dropped nothing - not even the guaranteed big
 * bones.
 *
 * The **mossy key** is the one drop here with real conditional logic, and it is worth having right:
 * it is the only way into Bryophyta's lair, and the wiki quotes Mod Ash on all three of its rates.
 * See [rollMossyKey].
 */
class MossGiantPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        MossGiants.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { npcKey ->
                onNpcSpawn(npcKey) {
                    npc.combatDef =
                        npc.combatDef.copy(
                            respawnDelay = MossGiants.RESPAWN_CYCLES,
                            aggressiveRadius = MossGiants.AGGRO_RADIUS,
                            aggroTargetDelay = MossGiants.AGGRO_SEARCH_DELAY,
                            aggressiveTimer = MossGiants.AGGRO_TIMER,
                            slayerXp = variant.slayerXp,
                        )
                }

                onNpcDeath(npcKey) { onDeath(npc) }
            }
        }
    }

    /**
     * Guaranteed big bones, then one roll on the table, then the tertiaries - each independent of
     * the table and of each other.
     */
    private fun onDeath(npc: Npc) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world

        val loot = mutableListOf(getRSCM("item.big_bones") to 1)

        MossGiantDrops.TABLE.roll(world, RingOfWealth.enhancesDropTables(killer))?.let { loot.add(it) }

        // "Only dropped by those found in the Wilderness", 1/3. Gated on where the *killer* is
        // standing rather than on which variant died, matching `content/npcs/zombie` and
        // `content/npcs/slayer`: that is the wiki's own wording, and the Wilderness Pond giants
        // share their ids with ten camps outside it.
        if (killer.inWilderness() && world.chance(1, MossGiantDrops.LOOTING_BAG_ONE_IN)) {
            loot.add(getRSCM("item.looting_bag") to 1)
        }

        if (world.chance(1, MossGiantDrops.ENSOULED_HEAD_ONE_IN)) {
            loot.add(getRSCM("item.ensouled_giant_head") to 1)
        }
        if (world.chance(1, MossGiantDrops.BEGINNER_CLUE_ONE_IN)) {
            loot.add(getRSCM("item.clue_scroll_beginner") to 1)
        }
        if (rollMossyKey(killer, npc)) {
            loot.add(getRSCM("item.mossy_key") to 1)
        }
        if (world.chance(1, MossGiantDrops.LONG_BONE_ONE_IN)) {
            loot.add(getRSCM("item.long_bone") to 1)
        }
        if (world.chance(1, MossGiantDrops.GIANT_CHAMPION_SCROLL_ONE_IN)) {
            loot.add(getRSCM("item.giant_champion_scroll") to 1)
        }
        // 1/5012.5, the one non-integer rate here, so this cannot go through World.chance.
        if (world.randomDouble() < 1.0 / MossGiantDrops.CURVED_BONE_ONE_IN) {
            loot.add(getRSCM("item.curved_bone") to 1)
        }

        loot.forEach { (item, amount) ->
            MonsterLoot.drop(world, killer, item, amount, npc.tile)
        }
    }

    /**
     * The mossy key's three published rates, in the order the wiki quotes Mod Ash giving them: on a
     * moss giant Slayer task 1/75, otherwise in the Wilderness 1/100, otherwise 1/150.
     *
     * "On task" is [Slayer.isOnTask], which checks the player actually has moss giants assigned and
     * that this npc counts towards it - so it is the real condition rather than a proxy for it, and
     * it correctly includes a Wilderness task without stacking the two improvements. The Wilderness
     * test is where the **killer** is standing, matching every other position-gated drop in this
     * tree.
     */
    private fun rollMossyKey(
        killer: Player,
        npc: Npc,
    ): Boolean {
        val oneIn =
            when {
                Slayer.isOnTask(killer, npc) -> MossGiantDrops.MOSSY_KEY_ON_TASK_ONE_IN
                killer.inWilderness() -> MossGiantDrops.MOSSY_KEY_WILDERNESS_ONE_IN
                else -> MossGiantDrops.MOSSY_KEY_ONE_IN
            }
        return npc.world.chance(1, oneIn)
    }
}
