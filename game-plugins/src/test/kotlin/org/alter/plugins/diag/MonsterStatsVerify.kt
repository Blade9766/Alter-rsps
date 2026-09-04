package org.alter.plugins.diag

import dev.openrune.cache.CacheManager
import org.alter.api.NpcSpecies
import org.alter.api.ext.NPC_MAGIC_DAMAGE_BONUS_INDEX
import org.alter.api.ext.NPC_RANGED_STRENGTH_BONUS_INDEX
import org.alter.api.ext.NPC_STRENGTH_BONUS_INDEX
import org.alter.api.ext.NPC_ATTACK_BONUS_INDEX
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.combat.CombatClass
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.combat.NpcCombatDef
import org.alter.game.model.entity.Npc
import org.alter.game.service.game.NpcMetadataService
import org.junit.BeforeClass
import java.io.File
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Checks `data/cfg/npcs/monsterStats.json`, the table that gives a monster its combat stats when
 * no plugin declares a `setCombatDef` for it.
 *
 * Before that table existed, an npc nothing had authored spawned as [NpcCombatDef.DEFAULT] - ten
 * hitpoints, zero attack, strength, defence, ranged and magic, and no bonuses at all. There are a
 * few dozen authored monsters and a few thousand npcs, so that was very nearly all of them, and
 * nothing anywhere said so: an abyssal demon with ten hitpoints still walks, attacks and dies, it
 * just dies to one hit.
 *
 * The table is checked against the cache by *name* the way [EquipmentBonusVerify] checks the item
 * overrides, and the tiers are checked end to end through the real [World.setNpcDefaults] rather
 * than by re-implementing the lookup here.
 */
class MonsterStatsVerify {
    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            CacheManager.init(Paths.get("../data", "cache"), 228)
        }

        private val CONFIG = File("../data/cfg/npcs/monsterStats.json")

        /**
         * A world with the table loaded and no plugins, so what comes out of `setNpcDefaults` is
         * the table's own work.
         */
        private val world: World by lazy {
            World(EatingVerify.GAME_CONTEXT, EatingVerify.DEV_CONTEXT).also {
                NpcMetadataService().loadMonsterStats(it, CONFIG)
            }
        }
    }

    private fun defOf(id: Int): NpcCombatDef =
        assertNotNull(world.npcStats[id], "npc $id (${CacheManager.getNpcs()[id]?.name}) has no stats")

    @Test
    fun `the config parses and covers most of the attackable npcs`() {
        assertTrue(CONFIG.exists(), "${CONFIG.path} is missing; every unauthored monster is a punching bag")
        assertTrue(world.npcStats.size > 3000, "only ${world.npcStats.size} monsters loaded")

        val attackable =
            CacheManager.getNpcs().values.count { npc ->
                npc.actions.any { it.equals("Attack", ignoreCase = true) }
            }
        val covered = CacheManager.getNpcs().values.count { npc ->
            npc.actions.any { it.equals("Attack", ignoreCase = true) } && world.npcStats.containsKey(npc.id)
        }
        assertTrue(attackable > 3000, "only $attackable attackable npcs found; the cache did not load")
        assertTrue(
            covered * 100 / attackable >= 85,
            "only $covered of $attackable attackable npcs have stats",
        )
    }

    /**
     * The name is the anchor, exactly as in [EquipmentRequirementVerify]: an id that has drifted
     * between cache revisions would hand some unrelated monster another one's hitpoints.
     */
    @Test
    fun `every id is an attackable npc with the recorded name`() {
        val mapper = com.fasterxml.jackson.databind.ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val config = mapper.readValue(CONFIG, org.alter.game.service.game.MonsterStatsConfig::class.java)
        assertTrue(config.monsters.isNotEmpty(), "the config declares no monsters")
        config.monsters.forEach { monster ->
            val def = assertNotNull(CacheManager.getNpcs()[monster.id], "npc ${monster.id} is not in the cache")
            assertEquals(
                monster.name,
                def.name,
                "npc ${monster.id} is '${def.name}' in the cache, not '${monster.name}' - the id has moved",
            )
            assertTrue(
                def.actions.any { it.equals("Attack", ignoreCase = true) },
                "npc ${monster.id} (${def.name}) cannot be attacked, so giving it combat stats does nothing",
            )
            assertTrue(monster.hitpoints > 0, "npc ${monster.id} (${def.name}) has no hitpoints")
        }
    }

    /** Spot checks against the wiki, one per shape of monster. */
    @Test
    fun `monsters carry the levels the wiki gives them`() {
        // hitpoints, attack, strength, defence, magic, ranged
        mapOf(
            415 to listOf(150, 97, 67, 135, 1, 1), // Abyssal demon
            2790 to listOf(8, 1, 1, 1, 1, 1), // Cow
            239 to listOf(240, 240, 240, 240, 240, 1), // King Black Dragon
            260 to listOf(75, 68, 68, 68, 68, 1), // Green dragon
            2892 to null, // Brian, a shopkeeper - not attackable, so absent
        ).forEach { (id, expected) ->
            if (expected == null) {
                assertTrue(id !in world.npcStats.keys, "npc $id should have no combat stats")
                return@forEach
            }
            val def = defOf(id)
            assertEquals(
                expected,
                listOf(def.hitpoints, def.attack, def.strength, def.defence, def.magic, def.ranged),
                "${CacheManager.getNpcs()[id]?.name} levels",
            )
        }
    }

    /**
     * A monster has one attack bonus rather than five, and it has to land in every slot that reads
     * one: `MeleeCombatFormula` picks the attacker's bonus by combat style, and
     * `Combat.getNpcXpMultiplier` reads slot 10.
     */
    @Test
    fun `bonuses land in the slots that read them`() {
        val cow = defOf(2790).bonuses
        assertEquals(-15, cow[0], "cow stab attack")
        assertEquals(-15, cow[1], "cow slash attack")
        assertEquals(-15, cow[2], "cow crush attack")
        assertEquals(-15, cow[NPC_ATTACK_BONUS_INDEX], "cow attack bonus, which the xp rate reads")
        assertEquals(-15, cow[NPC_STRENGTH_BONUS_INDEX], "cow strength bonus, which its max hit reads")
        assertEquals(-21, cow[5], "cow stab defence")
        assertEquals(-21, cow[9], "cow ranged defence")
        assertEquals(14, cow.size, "the bonus array has to be the full fourteen slots")

        val spectre = defOf(2).bonuses
        assertEquals(20, spectre[5], "aberrant spectre stab defence")
        assertEquals(-15, spectre[9], "aberrant spectre ranged defence")
    }

    /** Attack speed, which decides how often a monster actually hits. */
    @Test
    fun `attack speeds are the wiki's`() {
        mapOf(415 to 4, 2790 to 4, 239 to 4, 42 to 5).forEach { (id, speed) ->
            assertEquals(speed, defOf(id).attackSpeed, "${CacheManager.getNpcs()[id]?.name} attack speed")
        }
        assertTrue(
            world.npcStats.values.none { it.attackSpeed <= 0 },
            "a monster with a zero attack speed would attack every tick",
        )
    }

    /**
     * Species drive the salve amulet, the demonbane weapons, dragonfire and the enchanted bolts,
     * and the loader has to resolve them reflectively because the enum lives in a module this one
     * cannot import. If that reflection ever breaks it fails silently, so it is pinned here.
     */
    @Test
    fun `species resolve to real NpcSpecies values`() {
        assertTrue(NpcSpecies.DEMON in defOf(415).species, "an abyssal demon is a demon")
        assertTrue(NpcSpecies.UNDEAD in defOf(2).species, "an aberrant spectre is undead")
        assertTrue(NpcSpecies.DRACONIC in defOf(239).species, "the King Black Dragon is draconic")
        assertTrue(NpcSpecies.FIERY in defOf(239).species, "and fiery")
        val withSpecies = world.npcStats.values.count { it.species.isNotEmpty() }
        assertTrue(withSpecies > 500, "only $withSpecies monsters carry a species; the reflection has broken")
        world.npcStats.values.forEach { def ->
            def.species.forEach { assertTrue(it is NpcSpecies, "a species resolved to ${it.javaClass} instead of NpcSpecies") }
        }
    }

    /** Elemental weakness, which the magic formula multiplies a spell's damage by. */
    @Test
    fun `elemental weaknesses are loaded`() {
        val spectre = defOf(2)
        assertEquals(1, spectre.elementalWeaknessElement, "aberrant spectre is weak to air (Elements.AIR)")
        assertEquals(50, spectre.elementalWeaknessPercent, "aberrant spectre weakness severity")
        assertTrue(
            world.npcStats.values.count { it.elementalWeaknessElement >= 0 } > 500,
            "almost no monster has an elemental weakness",
        )
    }

    /**
     * The table must never declare a monster MAGIC or RANGED. `MagicCombatStrategy` reads a
     * casting spell an npc never has and would throw the moment one attacked, and a ranged npc
     * with no projectile shoots nothing visible. Both stay the business of an authored plugin.
     */
    @Test
    fun `no monster is given a combat class it cannot use`() {
        world.npcStats.forEach { (id, def) ->
            assertEquals(
                CombatClass.MELEE,
                def.combatClass,
                "npc $id (${CacheManager.getNpcs()[id]?.name}) was given ${def.combatClass}",
            )
        }
    }

    /** Likewise the style: the melee formula throws on anything but these three. */
    @Test
    fun `combat styles are melee styles only`() {
        val styles = world.npcStats.values.map { it.combatStyle }.toSet()
        assertTrue(
            styles.all { it == CombatStyle.STAB || it == CombatStyle.SLASH || it == CombatStyle.CRUSH },
            "the table declares $styles",
        )
        assertEquals(CombatStyle.CRUSH, defOf(2790).combatStyle, "a cow attacks with crush")
        assertEquals(CombatStyle.STAB, defOf(415).combatStyle, "an abyssal demon attacks with stab")
    }

    /**
     * End to end. [World.setNpcDefaults] is the only thing that puts any of this onto a live npc,
     * and it has three tiers - authored plugin, this table, then the bare default.
     */
    @Test
    fun `setNpcDefaults applies the table to a spawned npc`() {
        val npc = Npc(id = 415, tile = Tile(3222, 3222), world = world)
        world.setNpcDefaults(npc)

        assertEquals(150, npc.getMaxHp(), "an abyssal demon should have 150 hitpoints, not the default 10")
        assertEquals(150, npc.getCurrentHp(), "and spawn on full health")
        assertEquals(97, npc.stats.getMaxLevel(0), "attack level")
        assertEquals(67, npc.stats.getMaxLevel(1), "strength level")
        assertEquals(135, npc.stats.getMaxLevel(2), "defence level")
        assertEquals(20, npc.equipmentBonuses[5], "stab defence bonus")
        assertEquals(CombatStyle.STAB, npc.combatStyle, "combat style")
        assertTrue(NpcSpecies.DEMON in npc.species, "species reach the live npc")
    }

    /**
     * The tier order, which is the whole safety argument for this table: a monster somebody has
     * authored must come out of `setNpcDefaults` exactly as authored, table or no table.
     */
    @Test
    fun `an authored combat definition still wins over the table`() {
        val authored = NpcCombatDef.DEFAULT.copy(hitpoints = 1234, attackSpeed = 9, combatStyle = CombatStyle.SLASH)
        world.plugins.npcCombatDefs[415] = authored
        try {
            val npc = Npc(id = 415, tile = Tile(3222, 3222), world = world)
            world.setNpcDefaults(npc)
            assertEquals(1234, npc.getMaxHp(), "the authored hitpoints, not the table's 150")
            assertEquals(9, npc.combatDef.attackSpeed, "the authored attack speed")
            assertEquals(CombatStyle.SLASH, npc.combatStyle, "the authored combat style")
        } finally {
            world.plugins.npcCombatDefs.remove(415)
        }
    }

    /** An npc in neither the plugins nor the table still gets the old default, not a crash. */
    @Test
    fun `an unknown npc still falls back to the default definition`() {
        val id = CacheManager.getNpcs().values.first { it.id !in world.npcStats.keys }.id
        val npc = Npc(id = id, tile = Tile(3222, 3222), world = world)
        world.setNpcDefaults(npc)
        assertEquals(NpcCombatDef.DEFAULT.hitpoints, npc.getMaxHp(), "npc $id should keep the default hitpoints")
        assertEquals(NpcCombatDef.DEFAULT.attackSpeed, npc.combatDef.attackSpeed, "and the default attack speed")
    }

    /** Slot 12 and 13 exist for the monsters that shoot and cast, even while they swing as melee. */
    @Test
    fun `ranged strength and magic damage are carried where the wiki gives them`() {
        val ranged = world.npcStats.values.count { it.bonuses[NPC_RANGED_STRENGTH_BONUS_INDEX] != 0 }
        val magic = world.npcStats.values.count { it.bonuses[NPC_MAGIC_DAMAGE_BONUS_INDEX] != 0 }
        assertTrue(ranged > 20, "only $ranged monsters have a ranged strength bonus")
        assertTrue(magic > 20, "only $magic monsters have a magic damage bonus")
    }
}
