package org.alter.plugins.content.npcs.slayer

import org.alter.api.dsl.setCombatDef
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.items.jewellery.RingOfWealth
import org.alter.plugins.content.npcs.DropRoll
import org.alter.plugins.content.npcs.GemDropTable
import org.alter.plugins.content.npcs.HerbDropTable
import org.alter.plugins.content.npcs.MonsterLoot
import org.alter.plugins.content.npcs.RareDropTable
import org.alter.plugins.content.npcs.SeedDropTable
import org.alter.rscm.RSCM.getRSCM

/**
 * Combat definitions and drops for the Slayer Tower and Fremennik Slayer Dungeon monsters.
 *
 * Stats live in [SlayerMonsters], tables in [SlayerMonsterDrops]; this file is the wiring, and it
 * follows the same shape as `content/npcs/dungeon` and `content/npcs/critters`.
 *
 * Spawns are not here - the tower's are in `content/areas/slayertower/spawns` and the dungeon's in
 * `content/areas/fremennikslayerdungeon/spawns`, so each area owns its own population.
 *
 * Three things this file does that the older monster plugins do not:
 *
 * - **`slayerData { levelRequirement }` is set on every one of them**, which is what makes the
 *   Slayer level actually gate the fight: `Combat.canEngage` already refuses an attack when the
 *   player's Slayer level is below `combatDef.slayerReq`, and before this roster there was almost
 *   nothing in the world with a requirement worth enforcing.
 * - **Drops are rolled here rather than through the combat DSL's `drops { }` block**, for the reason
 *   `content/npcs/critters` documents: that block builds a loot table `NpcDeathAction` never rolls.
 * - **Pre-rolls are independent of the main table.** A kill rolls each entry in
 *   [SlayerMonster.preRolls] on its own, then the main table, then the gem, herb and clue rolls. So
 *   an abyssal demon can drop a whip *and* coins on the same kill, which is how the real table
 *   behaves - folding a 1/512 whip into a 128-row table would have made it a 1/512 chance of
 *   replacing a drop rather than an extra one.
 */
class SlayerMonsterPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        /*
         * Resolve the shared tables up front. They are `object` fields whose item keys would
         * otherwise first be looked up inside a death handler, turning a mistyped key into an
         * exception mid-kill rather than a plugin that visibly fails to load.
         */
        Server.logger.info {
            "Slayer monsters: ${SeedDropTable.warmUp()} seed rows, ${HerbDropTable.TABLE.size} herb rows and " +
                "${RareDropTable.warmUp() + GemDropTable.TABLE.size} rare/gem rows resolved."
        }

        SlayerMonsters.ALL.forEach { monster ->
            monster.npcKeys.forEach { npcKey ->
                setCombatDef(npcKey) {
                    /*
                     * Species is what gear conditions key off - the salve amulet's undead check
                     * being the one that matters here. Nothing set it before, so the amulet had
                     * nothing in the world to work against.
                     */
                    if (monster.species.isNotEmpty()) {
                        species {
                            monster.species.forEach { +it }
                        }
                    }

                    configs {
                        attackSpeed = monster.attackSpeed
                        respawnDelay = monster.respawnCycles
                        if (monster.poisonDamage > 0) {
                            poisonDamage = monster.poisonDamage
                        }
                    }
                    if (monster.aggroRadius > 0) {
                        aggro {
                            radius = monster.aggroRadius
                        }
                    }
                    stats {
                        hitpoints = monster.hitpoints
                        attack = monster.attack
                        strength = monster.strength
                        defence = monster.defence
                        magic = monster.magic
                        ranged = monster.ranged
                    }
                    bonuses {
                        attackBonus = monster.attackBonus
                        strengthBonus = monster.strengthBonus
                        defenceStab = monster.defenceStab
                        defenceSlash = monster.defenceSlash
                        defenceCrush = monster.defenceCrush
                        defenceMagic = monster.defenceMagic
                        defenceRanged = monster.defenceRanged
                    }
                    if (monster.poisonImmune || monster.venomImmune) {
                        immunities {
                            poison = monster.poisonImmune
                            venom = monster.venomImmune
                        }
                    }
                    anims {
                        attack = monster.attackAnimation
                        block = monster.blockAnimation
                        death = monster.deathAnimation
                    }
                    if (monster.attackSound >= 0) {
                        sound {
                            attackSound = monster.attackSound
                            blockSound = monster.blockSound
                            deathSound = monster.deathSound
                        }
                    }
                    slayerData {
                        levelRequirement = monster.slayerLevel
                        xp = monster.slayerXp
                    }
                }

                /*
                 * Combat style is set on spawn because the engine never copies one out of the
                 * combat def - the same reason `content/npcs/critters` does it.
                 *
                 * The two casters are skipped deliberately. `Npc.combatStyle` starts at STAB and
                 * `Npc.combatClass` at MELEE, and [SlayerCasterPlugin] sets both together through
                 * `prepareAttack` on every cast, exactly as the dark wizards do. Stamping MAGIC on
                 * the style here instead would leave them carrying a magic style with a melee class
                 * from spawn until their first attack, and `MeleeCombatFormula.getEquipmentAttackBonus`
                 * throws `IllegalStateException` on any style that is not stab, slash or crush.
                 */
                if (monster.magicMaxHit == null) {
                    onNpcSpawn(npc = npcKey) { npc.combatStyle = monster.combatStyle }
                }

                onNpcDeath(npcKey) { onDeath(npc, monster) }
            }
        }
    }

    private fun onDeath(
        npc: Npc,
        monster: SlayerMonster,
    ) {
        val killer = npc.attr[KILLER_ATTR]?.get() as? Player ?: return
        val world = npc.world

        val loot = monster.guaranteedDrops.map { getRSCM(it) to 1 }.toMutableList()

        monster.preRolls.forEach { (item, chance) ->
            if (world.randomDouble() <= chance) {
                loot.add(getRSCM(item) to 1)
            }
        }

        if (monster.table.isNotEmpty()) {
            DropRoll.pick(monster.table, world)?.let { picked ->
                picked.item?.let { loot.add(it to DropRoll.amount(picked, world)) }
            }
        }

        /*
         * The primary rare drop table and the gem table are separate rolls at separate rates, not
         * two names for one thing - a monster carrying `{{RareDropTable|2/128|5/128}}` reaches both
         * on the same kill. Only the abyssal demon and nechryael reach the primary table.
         */
        monster.rareTableChance?.let { chance ->
            if (world.randomDouble() <= chance) {
                RareDropTable.roll(world, RingOfWealth.enhancesDropTables(killer))?.let { picked ->
                    picked.item?.let { loot.add(it to DropRoll.amount(picked, world)) }
                }
            }
        }

        monster.gemTableChance?.let { chance ->
            if (world.randomDouble() <= chance) {
                GemDropTable.roll(world, RingOfWealth.enhancesDropTables(killer))?.let { picked ->
                    picked.item?.let { loot.add(it to DropRoll.amount(picked, world)) }
                }
            }
        }

        monster.herbTableChance?.let { chance ->
            if (world.randomDouble() <= chance) {
                DropRoll.pick(HerbDropTable.TABLE, world)?.let { picked ->
                    picked.item?.let { loot.add(it to DropRoll.amount(picked, world)) }
                }
            }
        }

        /*
         * The general seed table needs the *monster's* combat level - its six sub-tables are chosen
         * by a roll against it - which is why this passes the level through rather than treating
         * every seed table as a flat list.
         */
        monster.seedRoll?.let { roll ->
            if (world.randomDouble() <= roll.chance) {
                repeat(roll.rolls) {
                    val picked =
                        when (roll.table) {
                            SeedTable.ALLOTMENT -> DropRoll.pick(SeedDropTable.ALLOTMENT, world)
                            SeedTable.RARE -> DropRoll.pick(SeedDropTable.RARE, world)
                            SeedTable.GENERAL -> SeedDropTable.rollGeneral(monster.combatLevel, world)
                        }
                    picked?.item?.let { loot.add(it to DropRoll.amount(picked, world)) }
                }
            }
        }

        monster.clueScroll?.let { (item, chance) ->
            if (world.randomDouble() <= chance) {
                loot.add(getRSCM(item) to 1)
            }
        }

        /*
         * The one Wilderness tertiary kept from the published tables, matching what
         * `content/npcs/critters` and the dark wizards already do. None of these monsters spawn in
         * the Wilderness on this server yet, so it costs nothing and is correct the day they do.
         */
        if (killer.inWilderness() && world.randomDouble() <= 1.0 / 3.0) {
            loot.add(getRSCM("item.looting_bag") to 1)
        }

        loot.forEach { (item, amount) ->
            MonsterLoot.drop(world, killer, item, amount, npc.tile)
        }
    }
}
