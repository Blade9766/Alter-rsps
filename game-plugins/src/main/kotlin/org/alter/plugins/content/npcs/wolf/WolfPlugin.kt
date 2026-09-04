package org.alter.plugins.content.npcs.wolf

import org.alter.api.ext.inWilderness
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
 * Makes wolves aggressive, worth Slayer experience and worth killing. [WolfSpawnPlugin] puts them
 * on the map; stats live in `data/cfg/npcs/monsterStats.json` and ids and tiles in [Wolves].
 *
 * ## What this changes, per wolf
 *
 * Layered onto the def the engine already built, the pattern
 * `areas/wilderness/bosses/WildernessBossPlugin` documents:
 *
 * - **`respawnDelay`** 25 -> [Wolves.RESPAWN_CYCLES]. Read by `NpcDeathAction` at death, so patching
 *   it at spawn is enough.
 * - **`aggressiveRadius`**, **`aggroTargetDelay`** and **`aggressiveTimer`**, on the five versions
 *   the wiki marks `aggressive = Yes`. All three are needed: without a radius `NpcAggroPlugin`
 *   never arms its timer, and without a non-zero timer it arms one that refuses every target.
 * - **`slayerXp`**, which `Slayer.onKill` reads off the dying npc and is the only place Slayer
 *   experience comes from. Zeroed before this, so the `Wolves` task could be completed for nothing.
 *
 * The two Stronghold of Security wolves are `aggressive = No` and get the respawn and Slayer
 * experience only, which is what makes them the safe ones to train on at level 11 and 14.
 *
 * The patch is applied in an `onNpcSpawn` hook rather than once at load because a wolf that dies is
 * re-defaulted: `NpcDeathAction` calls `World.setNpcDefaults` and then `executeNpcSpawn` again,
 * which would otherwise hand the respawned wolf back its unpatched def. Per-npc spawn hooks run
 * before global ones, so both `NpcAggroPlugin` and
 * [org.alter.plugins.content.npcs.animations.MonsterAnimationsPlugin] see the patched version.
 *
 * ## Drops
 *
 * A wolf's whole published loot is **wolf bones**, plus two tertiaries. There is no drop table at
 * all on either page, which is why this package has no `*Drops.kt`.
 *
 * Rolled here rather than through the combat DSL's `drops { }` block, for the reason every monster
 * package in this tree repeats: that block builds a `LootTable` which `NpcDeathAction` never rolls.
 *
 * ### What is not modelled
 *
 * - **Wolf bone, 1/4.** "Only dropped during Rag and Bone Man II" - a quest this server does not
 *   have. A 1/4 drop is far too common to hand out unconditionally just because its condition is
 *   unbuilt, so it drops never rather than always. The zombie package makes the same call about the
 *   zombie bone.
 * - **The beginner clue scroll.** Published as `rarity=Rare` with no numerator, only from the
 *   Stronghold of Security wolves. There is no rate to implement and inventing one would be worse
 *   than leaving it out.
 * - **`Wolf bones` on free-to-play worlds**, where the page says wolves drop no bones at all. This
 *   server has no free-to-play/members split to key that off.
 */
class WolfPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        Wolves.VARIANTS.forEach { variant ->
            variant.npcKeys.forEach { npcKey ->
                onNpcSpawn(npcKey) {
                    val current = npc.combatDef
                    npc.combatDef =
                        if (variant.aggressive) {
                            current.copy(
                                respawnDelay = Wolves.RESPAWN_CYCLES,
                                aggressiveRadius = Wolves.AGGRO_RADIUS,
                                aggroTargetDelay = Wolves.AGGRO_SEARCH_DELAY,
                                aggressiveTimer = Wolves.AGGRO_TIMER,
                                slayerXp = variant.slayerXp,
                            )
                        } else {
                            current.copy(
                                respawnDelay = Wolves.RESPAWN_CYCLES,
                                slayerXp = variant.slayerXp,
                            )
                        }
                }

                onNpcDeath(npcKey) { onDeath(npc, variant) }
            }
        }
    }

    private fun onDeath(
        npc: Npc,
        variant: WolfVariant,
    ) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world

        val loot = mutableListOf(getRSCM("item.wolf_bones") to 1)

        // "Only dropped by those found in the Wilderness", at 1/6 for a wolf and 1/3 for a white
        // one. Gated on where the *killer* is standing rather than on which variant died, matching
        // `content/npcs/zombie` and `content/npcs/slayer`: that is the wiki's own wording, and a
        // hand-spawned Wilderness wolf should not print looting bags in Lumbridge.
        val lootingBagOneIn = if (variant.whiteWolf) WHITE_LOOTING_BAG_ONE_IN else LOOTING_BAG_ONE_IN
        if (killer.inWilderness() && world.chance(1, lootingBagOneIn)) {
            loot.add(getRSCM("item.looting_bag") to 1)
        }

        loot.forEach { (item, amount) ->
            MonsterLoot.drop(world, killer, item, amount, npc.tile)
        }
    }

    private companion object {
        /** `Wolf` tertiary, 1/6, Wilderness only. */
        const val LOOTING_BAG_ONE_IN = 6

        /** `White wolf` tertiary, 1/3, Wilderness only - the same rate a moss giant pays. */
        const val WHITE_LOOTING_BAG_ONE_IN = 3
    }
}
