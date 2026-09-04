package org.alter.plugins.content.npcs.darkwizard

import org.alter.api.dsl.setCombatDef
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.npcs.MonsterLoot
import org.alter.plugins.content.npcs.darkwizard.DarkWizardData.DropTier
import org.alter.rscm.RSCM.getRSCM

/**
 * Dark wizards: spawns, combat stats, and rarity-weighted drops. Real combat attack
 * logic lives in [DarkWizardCombatPlugin].
 *
 * Every stat, level, location, and drop rarity below comes from the OSRS Wiki's Dark
 * wizard page. Level 7/11/20/22/23 are all real variants (confirmed against this
 * cache's own npc.rscm) placed at their real wiki-documented locations: level 6-7
 * Wilderness, behind Draynor Village bank, the Varrock stone circle, Dark Wizards'
 * Tower (all 3 floors), and Kourend Castle's cages. Where the wiki lists multiple valid
 * levels for the same set of tiles without distinguishing which exact tile is which
 * level (e.g. "levels 7, 20" for every Wilderness spot), the applicable ids are cycled
 * evenly across those tiles rather than guessing a false per-tile precision the wiki
 * itself doesn't provide.
 *
 * Attack/block/death animations (425/717/836) were derived by running this project's
 * own [org.alter.plugins.content.npcs.animations.MonsterAnimationResolver] - the same
 * resolver [org.alter.plugins.content.npcs.animations.MonsterAnimationsPlugin] uses
 * automatically for NPCs *without* an explicit combat def - against the real observed
 * animation-id sets already bundled for these npc ids. All variants reuse the same
 * result (from the base `npc.dark_wizard` id) since they're the same in-game character
 * model; combat sounds aren't set explicitly because that same generic plugin fills
 * them in automatically from the cache's own sequence data once the animations are known.
 * Bonuses are near-entirely 0 per the wiki's infobox, aside from a small magic defence
 * (wiki gives 3-5 depending on level; 4 is used uniformly here as an approximation).
 * Respawn is the wiki's `respawn = 50`, which is in game ticks and so is this engine's
 * cycles one-for-one - used as published.
 *
 * The `drops { }` block in [org.alter.api.dsl.NpcCombatDsl] is never actually consulted
 * by [org.alter.game.action.NpcDeathAction] - `roll()` is imported there but never
 * called, confirmed by tracing the death-handling code - so it would silently produce no
 * drops at all if used. Drops are instead rolled directly in [onDeath] using real
 * wiki-sourced rarity numerators as relative weights, the same manual-drop pattern
 * [org.alter.plugins.content.npcs.CowPlugin] already uses, but weighted instead of
 * always-drop. The wiki splits its drop table by variant *drop tier*, not by level
 * number: levels 7 and 11 share the "low" table, and levels 20, 22, and 23 share the
 * "high" table - reproduced faithfully as [LOW_MAIN_TABLE]/[HIGH_MAIN_TABLE] rather than
 * re-grouped by level. Energy potion rarities were the one gap in the wiki's published
 * numbers - given a low, unlabeled-in-source weight consistent with the rarest tier
 * nearby, same as this session's established approach for unpublished specifics
 * elsewhere (e.g. Woodcutting/Fishing's catch-chance approximations).
 */
class DarkWizardConfigsPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        spawnCycled(DarkWizardData.WILDERNESS_IDS, DarkWizardData.WILDERNESS_SPOTS, height = 0)
        spawnCycled(DarkWizardData.DRAYNOR_IDS, DarkWizardData.DRAYNOR_SPOTS, height = 0)
        spawnCycled(DarkWizardData.VARROCK_IDS, DarkWizardData.VARROCK_SPOTS, height = 0)
        spawnCycled(DarkWizardData.TOWER_IDS, DarkWizardData.TOWER_GROUND_SPOTS, height = 0)
        spawnCycled(DarkWizardData.TOWER_IDS, DarkWizardData.TOWER_FIRST_SPOTS, height = 1)
        spawnCycled(DarkWizardData.TOWER_IDS, DarkWizardData.TOWER_SECOND_SPOTS, height = 2)
        spawnCycled(DarkWizardData.KOUREND_CASTLE_IDS, DarkWizardData.KOUREND_CASTLE_SPOTS, height = 2)

        DarkWizardData.VARIANTS.forEach { variant ->
            setCombatDef(variant.npcKey) {
                configs {
                    attackSpeed = 4
                    attackRange = 8
                    respawnDelay = RESPAWN_CYCLES
                }
                aggro {
                    radius = 5
                    searchDelay = 1
                }
                stats {
                    hitpoints = variant.hitpoints
                    attack = variant.attack
                    strength = variant.strength
                    defence = variant.defence
                    magic = variant.magic
                }
                bonuses {
                    defenceMagic = variant.magicDefenceBonus
                }
                anims {
                    attack = ATTACK_ANIMATION
                    block = BLOCK_ANIMATION
                    death = DEATH_ANIMATION
                }
            }

            onNpcDeath(variant.npcKey) { onDeath(npc, variant.dropTier) }
        }
    }

    private fun spawnCycled(
        ids: List<String>,
        spots: List<Pair<Int, Int>>,
        height: Int,
    ) {
        spots.forEachIndexed { index, (x, z) ->
            spawnNpc(npc = ids[index % ids.size], x = x, z = z, height = height, walkRadius = 3, direction = Direction.SOUTH)
        }
    }

    private fun onDeath(
        npc: Npc,
        tier: DropTier,
    ) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world
        val table = if (tier == DropTier.LOW) LOW_MAIN_TABLE else HIGH_MAIN_TABLE

        val drops = mutableListOf(getRSCM("item.bones") to 1)
        weightedPick(table, world)?.let { drops.add(it.item to it.amount) }

        val lootingBagChance = if (tier == DropTier.LOW) 1.0 / 15.0 else 1.0 / 7.0
        if (killer.inWilderness() && world.randomDouble() <= lootingBagChance) {
            drops.add(getRSCM("item.looting_bag") to 1)
        }

        val clueChance = if (tier == DropTier.LOW) 1.0 / 50.0 else 1.0 / 35.0
        if (world.randomDouble() <= clueChance) {
            drops.add(getRSCM("item.clue_scroll_beginner") to 1)
        }

        drops.forEach { (item, amount) ->
            MonsterLoot.drop(world, killer, item, amount, npc.tile)
        }
    }

    private data class WeightedDrop(val item: Int, val amount: Int, val weight: Int)

    /** Picks one entry from [table] by relative weight, or null for a "Nothing" roll. */
    private fun weightedPick(
        table: List<WeightedDrop?>,
        world: World,
    ): WeightedDrop? {
        val total = table.sumOf { it?.weight ?: NOTHING_WEIGHT }
        var roll = world.randomDouble() * total
        for (drop in table) {
            val weight = drop?.weight ?: NOTHING_WEIGHT
            if (roll < weight) {
                return drop
            }
            roll -= weight
        }
        return null
    }

    private companion object {
        const val RESPAWN_CYCLES = 50 // wiki respawn = 50, in game ticks
        const val NOTHING_WEIGHT = 16 // "Nothing" roll weight, both tables

        /**
         * The three combat animations on the def, corrected after `AnimationRoleAudit` caught them
         * in the wrong roles.
         *
         * They used to read `attack = 425, block = 717`, and `Animation`'s own names say both were
         * wrong: **425 is `HUMAN_DEFEND_COWARDLY`** - a hit reaction, which the wizard was swinging -
         * and **717 is `CAST_WEAKEN_WIZARD`**, so a dark wizard that got punched answered by casting
         * Weaken at nothing. `content/npcs/chaosdruid` reads the same rig the same way: npc 520's
         * observed set is "[425, 710, 422, 836] - block, this, punch, death".
         *
         * The attack is now **711**, `UNARMED_MAGIC_SPELL_CAST`, which is the right idle-hands cast
         * for a wizard and is in every dark wizard's observed set. It is also mostly cosmetic:
         * [DarkWizardCombatPlugin] plays each spell's own `castAnimation` when it casts, so this is
         * what shows only on the rare ordinary swing.
         */
        const val ATTACK_ANIMATION = 711

        const val BLOCK_ANIMATION = 425

        const val DEATH_ANIMATION = 836

        val LOW_MAIN_TABLE: List<WeightedDrop?> =
            listOf(
                WeightedDrop(getRSCM("item.staff"), 1, 8),
                WeightedDrop(getRSCM("item.wizard_hat"), 1, 6),
                WeightedDrop(getRSCM("item.black_robe"), 1, 3),
                WeightedDrop(getRSCM("item.earth_rune"), 36, 4),
                WeightedDrop(getRSCM("item.air_rune"), 10, 3),
                WeightedDrop(getRSCM("item.water_rune"), 10, 3),
                WeightedDrop(getRSCM("item.earth_rune"), 10, 3),
                WeightedDrop(getRSCM("item.fire_rune"), 10, 3),
                WeightedDrop(getRSCM("item.air_rune"), 18, 2),
                WeightedDrop(getRSCM("item.water_rune"), 18, 2),
                WeightedDrop(getRSCM("item.earth_rune"), 18, 2),
                WeightedDrop(getRSCM("item.fire_rune"), 18, 2),
                WeightedDrop(getRSCM("item.nature_rune"), 4, 7),
                WeightedDrop(getRSCM("item.chaos_rune"), 5, 6),
                WeightedDrop(getRSCM("item.mind_rune"), 10, 3),
                WeightedDrop(getRSCM("item.body_rune"), 10, 3),
                WeightedDrop(getRSCM("item.mind_rune"), 18, 2),
                WeightedDrop(getRSCM("item.body_rune"), 18, 2),
                WeightedDrop(getRSCM("item.blood_rune"), 2, 2),
                WeightedDrop(getRSCM("item.cosmic_rune"), 2, 1),
                WeightedDrop(getRSCM("item.law_rune"), 3, 1),
                WeightedDrop(getRSCM("item.coins_995"), 1, 17),
                WeightedDrop(getRSCM("item.coins_995"), 2, 16),
                WeightedDrop(getRSCM("item.coins_995"), 4, 7),
                WeightedDrop(getRSCM("item.coins_995"), 29, 3),
                WeightedDrop(getRSCM("item.coins_995"), 30, 1),
                WeightedDrop(getRSCM("item.water_talisman"), 1, 1),
                WeightedDrop(getRSCM("item.fire_talisman"), 1, 1),
                WeightedDrop(getRSCM("item.energy_potion2"), 1, 1),
                WeightedDrop(getRSCM("item.energy_potion3"), 1, 1),
                WeightedDrop(getRSCM("item.energy_potion4"), 1, 1),
                null,
            )

        val HIGH_MAIN_TABLE: List<WeightedDrop?> =
            listOf(
                WeightedDrop(getRSCM("item.staff"), 1, 4),
                WeightedDrop(getRSCM("item.wizard_hat"), 1, 6),
                WeightedDrop(getRSCM("item.black_robe"), 1, 3),
                WeightedDrop(getRSCM("item.earth_rune"), 36, 4),
                WeightedDrop(getRSCM("item.air_rune"), 10, 3),
                WeightedDrop(getRSCM("item.water_rune"), 10, 3),
                WeightedDrop(getRSCM("item.earth_rune"), 10, 3),
                WeightedDrop(getRSCM("item.fire_rune"), 10, 3),
                WeightedDrop(getRSCM("item.air_rune"), 18, 2),
                WeightedDrop(getRSCM("item.water_rune"), 18, 2),
                WeightedDrop(getRSCM("item.earth_rune"), 18, 2),
                WeightedDrop(getRSCM("item.fire_rune"), 18, 2),
                WeightedDrop(getRSCM("item.nature_rune"), 4, 7),
                WeightedDrop(getRSCM("item.chaos_rune"), 4, 6),
                WeightedDrop(getRSCM("item.mind_rune"), 10, 3),
                WeightedDrop(getRSCM("item.body_rune"), 10, 3),
                WeightedDrop(getRSCM("item.mind_rune"), 18, 2),
                WeightedDrop(getRSCM("item.body_rune"), 18, 2),
                WeightedDrop(getRSCM("item.blood_rune"), 2, 2),
                WeightedDrop(getRSCM("item.cosmic_rune"), 2, 1),
                WeightedDrop(getRSCM("item.law_rune"), 3, 1),
                WeightedDrop(getRSCM("item.coins_995"), 1, 17),
                WeightedDrop(getRSCM("item.coins_995"), 2, 16),
                WeightedDrop(getRSCM("item.coins_995"), 4, 9),
                WeightedDrop(getRSCM("item.coins_995"), 29, 3),
                WeightedDrop(getRSCM("item.coins_995"), 30, 1),
                WeightedDrop(getRSCM("item.water_talisman"), 1, 2),
                WeightedDrop(getRSCM("item.fire_talisman"), 1, 2),
                WeightedDrop(getRSCM("item.energy_potion3"), 1, 1),
                WeightedDrop(getRSCM("item.energy_potion4"), 1, 1),
                null,
            )
    }
}
