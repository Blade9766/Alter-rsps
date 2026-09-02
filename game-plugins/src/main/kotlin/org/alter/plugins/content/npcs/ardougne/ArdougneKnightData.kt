package org.alter.plugins.content.npcs.ardougne

import org.alter.game.model.combat.CombatStyle

/**
 * The Knights of Ardougne and the Paladins - East Ardougne's two high-level humanoids, and
 * the reason anyone trains Thieving there.
 *
 * Both are modelled together because they are the same shape: one unversioned stat block
 * each, Slash melee, not aggressive, and both already present in this server's
 * `data/cfg/thieving/pickpockets.json` as level 55 and level 70 pickpocket targets. **That
 * config has existed all along with nothing to pickpocket** - no plugin ever spawned either
 * npc - so placing them here is what makes the existing Thieving content reachable.
 *
 * Animations are identical for both and come from this cache's observed sets, resolved
 * through [org.alter.api.cfg.Animation]'s named constants: attack 390
 * (`HUMAN_SLASH_SWORD_ATTACK`), block 388 (`HUMAN_SLASH_SWORD_DEFEND`), death 836
 * (`HUMAN_DEATH`) - consistent with both infoboxes' `attack style = Slash`.
 *
 * Ranged defence uses the wiki's `dstandard`, since `NpcCombatDef` carries a single
 * `defenceRanged`.
 */
internal object ArdougneKnightData {
    data class Group(
        val name: String,
        val npcKeys: List<String>,
        val combatLevel: Int,
        val hitpoints: Int,
        val attack: Int,
        val strength: Int,
        val defence: Int,
        val attackSpeed: Int,
        val respawnCycles: Int,
        val defenceStab: Int,
        val defenceSlash: Int,
        val defenceCrush: Int,
        val defenceMagic: Int,
        val defenceRanged: Int,
        val attackBonus: Int,
        val strengthBonus: Int,
    )

    data class Spawn(val npcKey: String, val x: Int, val z: Int, val height: Int)

    const val ATTACK_ANIMATION = 390 // Animation.HUMAN_SLASH_SWORD_ATTACK
    const val BLOCK_ANIMATION = 388 // Animation.HUMAN_SLASH_SWORD_DEFEND
    const val DEATH_ANIMATION = 836 // Animation.HUMAN_DEATH

    /** Slash for both, per each infobox's `attack style`. */
    val COMBAT_STYLE = CombatStyle.SLASH

    /**
     * `respawn = 25` on the wiki, in game ticks - used as published, the same reading the
     * other monster files here apply to that field. Knights respawn twice as fast as almost
     * everything else built so far, which is real: every other npc in this project reads
     * `respawn = 50`.
     */
    val KNIGHT =
        Group(
            name = "Knight of Ardougne",
            npcKeys = listOf("npc.knight_of_ardougne", "npc.knight_of_ardougne_11936"),
            combatLevel = 46,
            hitpoints = 52,
            attack = 38,
            strength = 40,
            defence = 31,
            attackSpeed = 5,
            respawnCycles = 25,
            defenceStab = 39,
            defenceSlash = 40,
            defenceCrush = 36,
            defenceMagic = -11,
            defenceRanged = 36,
            attackBonus = 8,
            strengthBonus = 10,
        )

    val PALADIN =
        Group(
            name = "Paladin",
            npcKeys =
                listOf(
                    "npc.paladin_3293", "npc.paladin_3294", "npc.paladin_11930",
                    "npc.paladin_11931", "npc.paladin_11932", "npc.paladin_11933",
                ),
            combatLevel = 62,
            hitpoints = 57,
            attack = 54,
            strength = 54,
            defence = 54,
            attackSpeed = 5,
            respawnCycles = 50,
            defenceStab = 87,
            defenceSlash = 84,
            defenceCrush = 76,
            defenceMagic = -10,
            defenceRanged = 79,
            attackBonus = 20,
            strengthBonus = 22,
        )

    val GROUPS = listOf(KNIGHT, PALADIN)

    fun groupOf(npcKey: String): Group = GROUPS.first { npcKey in it.npcKeys }

    /**
     * The five East Ardougne knight pins. Unlike the paladins below, these carry no `title:`
     * annotation, so the two ids are dealt round-robin over the tiles in the wiki's listing
     * order - the same stable-assignment call made for the city guards.
     *
     * The wiki also lists knights in West Ardougne and the Mourner Tunnels. Both are left
     * out: West Ardougne is quest-locked content this project has not built, and its knights
     * are different ids (8854/11902) anyway.
     */
    private val KNIGHT_TILES =
        listOf(
            2582 to 3297, 2652 to 3318, 2653 to 3300, 2669 to 3298, 2671 to 3313,
        )

    /**
     * The paladins, all 22 of them, and **every one is on its real published tile as its real
     * published id** - the wiki's paladin pins carry `title:` annotations the way the White
     * Knights' do, so nothing here is assigned.
     *
     * Two stand in the market; the rest hold East Ardougne Castle across its ground and first
     * floors. The Song of the Elves West Ardougne and Mourner Tunnel pins are left out, being
     * post-quest placements of different ids (8853/11901).
     */
    private val PALADIN_SPAWNS =
        listOf(
            // Market.
            Spawn("npc.paladin_3294", 2653, 3315, 0),
            Spawn("npc.paladin_11933", 2657, 3307, 0),
            // Castle, ground floor.
            Spawn("npc.paladin_3293", 2571, 3307, 0),
            Spawn("npc.paladin_11931", 2572, 3303, 0),
            Spawn("npc.paladin_11930", 2575, 3296, 0),
            Spawn("npc.paladin_11933", 2577, 3308, 0),
            Spawn("npc.paladin_11932", 2581, 3286, 0),
            Spawn("npc.paladin_3293", 2581, 3299, 0),
            Spawn("npc.paladin_3293", 2583, 3292, 0),
            // Castle, first floor.
            Spawn("npc.paladin_11932", 2572, 3292, 1),
            Spawn("npc.paladin_3293", 2576, 3293, 1),
            Spawn("npc.paladin_11931", 2578, 3285, 1),
            Spawn("npc.paladin_3293", 2581, 3286, 1),
            Spawn("npc.paladin_11930", 2581, 3307, 1),
            Spawn("npc.paladin_3293", 2582, 3306, 1),
            Spawn("npc.paladin_11932", 2584, 3288, 1),
            Spawn("npc.paladin_11933", 2584, 3304, 1),
            Spawn("npc.paladin_11933", 2585, 3290, 1),
            Spawn("npc.paladin_11930", 2586, 3289, 1),
            Spawn("npc.paladin_3293", 2586, 3303, 1),
            Spawn("npc.paladin_11931", 2587, 3291, 1),
            Spawn("npc.paladin_11932", 2588, 3301, 1),
        )

    val SPAWNS: List<Spawn> =
        KNIGHT_TILES.mapIndexed { index, (x, z) ->
            Spawn(KNIGHT.npcKeys[index % KNIGHT.npcKeys.size], x, z, 0)
        } + PALADIN_SPAWNS
}
