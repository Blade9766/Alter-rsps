package org.alter.plugins.content.npcs.darkwarrior

import org.alter.game.model.combat.CombatStyle

/**
 * The Dark warriors, transcribed from the OSRS Wiki's `Dark warrior` page - one
 * `Infobox Monster` block with five versions, and one `Locations` table with five map lines.
 *
 * The five versions are three different monsters wearing one name:
 * - **Level 8** (`531`) - the free-to-play original, at the Dark Warriors' Fortress.
 * - **Levels 37 / 51 / 62** (`11111` / `11110` / `11109`) - added by *A Kingdom Divided* and
 *   scattered around Great Kourend. They share one drop table, which is why the wiki calls
 *   their `dropversion` "Great Kourend" rather than naming a level.
 * - **Level 145** (`6606`) - the *Rejuvenating the Wilderness* rewrite, which is what actually
 *   stands on the fortress floor in a members world.
 *
 * The id-to-level mapping is the infobox's own `id1..id5` fields, and it is not in level order
 * (`id2 = 11111` is the *level 37*). It cross-checks against this server's own generated
 * `data/cfg/npcs/monsterStats.json`, which carries the same hitpoints and bonuses for the same
 * four ids from an independent source (the DPS calculator's monster dump); `DarkWarriorVerify`
 * asserts the cache agrees on the combat levels too.
 *
 * ## Things worth knowing
 *
 * **Max hits are not stored.** The wiki publishes 2 / 4 / 5 / 6 / 18, but feeding these strength
 * levels and strength bonuses through this server's own melee formula already reproduces them -
 * the same reading the barbarian and White Knight packages take.
 *
 * **The level 145's `xpbonus = 10` is not modelled.** That is the Wilderness bonus-experience
 * multiplier from the 2014 rejuvenation update, and there is no bonus-xp mechanic in this engine
 * to hang it on. Its combat xp is the ordinary damage-based amount.
 *
 * **Ranged defence is not published for any version.** The infobox gives `dlight`, `dstandard`
 * and `dheavy` all as 0, so no `defenceRanged` is declared and the builder's own 0 stands.
 */
internal object DarkWarriors {
    /** Which of the three published drop tables a version rolls on. */
    enum class Table { LEVEL_8, KOUREND, LEVEL_145 }

    data class Variant(
        val npcKey: String,
        val combatLevel: Int,
        val hitpoints: Int,
        val attack: Int,
        val strength: Int,
        val defence: Int,
        val magic: Int,
        val attackBonus: Int,
        val strengthBonus: Int,
        val defenceStab: Int,
        val defenceSlash: Int,
        val defenceCrush: Int,
        val slayerXp: Double,
        val table: Table,
        /**
         * True for the two Wilderness versions. Wilderness monsters ignore the "stops bothering
         * you above twice its own combat level" rule, which is the whole character of walking
         * into the fortress - see [DarkWarriorPlugin] for how that is expressed.
         */
        val wilderness: Boolean,
    )

    /** One spawn pin: an npc key on a tile, on a plane. */
    data class Spawn(val npcKey: String, val x: Int, val z: Int, val height: Int)

    const val LEVEL_8_KEY = "npc.dark_warrior"
    const val LEVEL_37_KEY = "npc.dark_warrior_11111"
    const val LEVEL_51_KEY = "npc.dark_warrior_11110"
    const val LEVEL_62_KEY = "npc.dark_warrior_11109"
    const val LEVEL_145_KEY = "npc.dark_warrior_6606"

    /** `attack speed = 4` on all five versions. */
    const val ATTACK_SPEED = 4

    /** The wiki's `respawn = 50`, in game ticks and so this engine's cycles one for one. */
    const val RESPAWN_CYCLES = 50

    /** `attack style = [[Slash]]`, shared by all five versions. */
    val COMBAT_STYLE = CombatStyle.SLASH

    /**
     * Attack, block and death animations.
     *
     * Not invented: `openosrs-animations.json` - the observed-animation bundle this project's own
     * [org.alter.plugins.content.npcs.animations.MonsterAnimationResolver] reads - lists exactly
     * `[390, 388, 836]` for both npc `531` and npc `6606`, and 836 is the resolver's own
     * `HUMAN_DEATH` constant. That leaves 390 as the sword attack and 388 as the block, which is
     * what the resolver would pick unaided if this package declared no combat def at all.
     *
     * The three Kourend ids have no observed set in that bundle, so they reuse the same trio; they
     * are the same human-with-a-sword rig, and without this they would fall back to the generic
     * human animations anyway.
     *
     * Combat *sounds* are deliberately not set here.
     * [org.alter.plugins.content.npcs.animations.MonsterAnimationsPlugin] fills them in from the
     * cache's own sequence data once the animations are known, including for npcs that do declare
     * a combat def.
     */
    const val ATTACK_ANIMATION = 390
    const val BLOCK_ANIMATION = 388
    const val DEATH_ANIMATION = 836

    /** Cycles between aggro sweeps, matching the other monster packages. */
    const val AGGRO_SEARCH_DELAY = 4

    /** Tiles a dark warrior will notice a player from. */
    const val AGGRO_RADIUS = 4

    /**
     * How long after entering an area a non-Wilderness dark warrior stays aggressive, in cycles -
     * ten minutes, the interval the real game uses before monsters lose interest in a player who
     * has stood in their region that long.
     *
     * This is [org.alter.api.NpcCombatBuilder.DEFAULT_AGGRO_TIMER], which an `aggro { }` block
     * that does not name a timer now takes on its own, so it is only restated here to keep the
     * Wilderness/non-Wilderness split in [DarkWarriorPlugin] readable as one either/or.
     */
    const val AGGRO_CYCLES = org.alter.api.NpcCombatBuilder.DEFAULT_AGGRO_TIMER

    val LEVEL_8 =
        Variant(
            npcKey = LEVEL_8_KEY,
            combatLevel = 8,
            hitpoints = 17,
            attack = 5,
            strength = 5,
            defence = 5,
            magic = 1,
            attackBonus = 20,
            strengthBonus = 16,
            defenceStab = 96,
            defenceSlash = 79,
            defenceCrush = 59,
            slayerXp = 17.0,
            table = Table.LEVEL_8,
            wilderness = true,
        )

    /**
     * The three Kourend versions differ only in levels and hitpoints; their bonuses are one shared
     * set (`0 / 0` attack and strength, `10 / 20 / 40` melee defence) published identically on all
     * three, which is why they are a `copy` chain rather than three transcriptions.
     */
    val LEVEL_37 =
        Variant(
            npcKey = LEVEL_37_KEY,
            combatLevel = 37,
            hitpoints = 50,
            attack = 30,
            strength = 30,
            defence = 20,
            magic = 10,
            attackBonus = 0,
            strengthBonus = 0,
            defenceStab = 10,
            defenceSlash = 20,
            defenceCrush = 40,
            slayerXp = 50.0,
            table = Table.KOUREND,
            wilderness = false,
        )

    val LEVEL_51 =
        LEVEL_37.copy(
            npcKey = LEVEL_51_KEY,
            combatLevel = 51,
            hitpoints = 70,
            attack = 40,
            strength = 40,
            defence = 30,
            magic = 15,
            slayerXp = 70.0,
        )

    val LEVEL_62 =
        LEVEL_37.copy(
            npcKey = LEVEL_62_KEY,
            combatLevel = 62,
            hitpoints = 80,
            attack = 50,
            strength = 50,
            defence = 40,
            magic = 20,
            slayerXp = 80.0,
        )

    val LEVEL_145 =
        Variant(
            npcKey = LEVEL_145_KEY,
            combatLevel = 145,
            hitpoints = 165,
            attack = 75,
            strength = 75,
            defence = 55,
            magic = 1,
            attackBonus = 80,
            strengthBonus = 76,
            defenceStab = 106,
            defenceSlash = 109,
            defenceCrush = 139,
            slayerXp = 181.5,
            table = Table.LEVEL_145,
            wilderness = true,
        )

    val VARIANTS = listOf(LEVEL_8, LEVEL_37, LEVEL_51, LEVEL_62, LEVEL_145)

    /**
     * Every dark warrior spawn, from the wiki's five `LocLine` map-pin lists.
     *
     * ## The fortress is read as a members world
     *
     * The wiki publishes three fortress lines: seven level-8 pins on the ground floor, eighteen
     * level-145 pins on the *same* ground floor, and eight level-8 pins upstairs. Those first two
     * are alternatives, not neighbours - the 2014 rejuvenation update replaced the ground floor's
     * level 8s with level 145s in members worlds, and the seven-pin line is what a free-to-play
     * world sees instead. Several of the pins are even the same tiles.
     *
     * This server already runs members content (Barrows, the King Black Dragon, Kourend), so it
     * takes the members reading, exactly as the White Knight package does for its members-only
     * drop rows: **the ground floor is the eighteen level 145s and the upper floor is the eight
     * level 8s.** Both versions are reachable, no tile is claimed twice, and nobody meets a level
     * 8 and a level 145 standing on the same square. The seven free-to-play ground-floor pins are
     * the only published spawns deliberately left unplaced.
     *
     * ## Where the level assignment is a guess and where it is not
     *
     * The fortress lines each name a single level, so every tile there carries its real version.
     * The two Kourend lines name several (`levels = 37, 62` and `levels = 37, 51, 62`) without
     * saying which pin is which, so - as the dark wizard package already does for the same
     * problem - the applicable ids are cycled evenly across those tiles rather than inventing a
     * per-tile precision the wiki does not publish.
     *
     * The Shaman Caves barrel encounter the page also mentions is not a spawn at all; it is a
     * random encounter from an activity that does not exist here.
     */
    val SPAWNS: List<Spawn> =
        fortressGroundFloor() + fortressUpperFloor() + shayzien() + lovakengjWoods()

    /** `Dark Warriors' Fortress`, level 145, plane 0 - eighteen pins. */
    private fun fortressGroundFloor(): List<Spawn> =
        listOf(
            3021 to 3638, 3022 to 3626, 3023 to 3629, 3023 to 3632, 3026 to 3630,
            3027 to 3634, 3028 to 3629, 3028 to 3632, 3029 to 3634, 3030 to 3630,
            3030 to 3638, 3031 to 3634, 3032 to 3626, 3032 to 3632, 3033 to 3630,
            3035 to 3625, 3035 to 3629, 3037 to 3637,
        ).map { (x, z) -> Spawn(LEVEL_145_KEY, x, z, height = 0) }

    /** `Dark Warriors' Fortress (1st floor)`, level 8, plane 1 - eight pins. */
    private fun fortressUpperFloor(): List<Spawn> =
        listOf(
            3022 to 3624, 3022 to 3639, 3023 to 3632, 3028 to 3626,
            3029 to 3637, 3035 to 3631, 3036 to 3624, 3036 to 3639,
        ).map { (x, z) -> Spawn(LEVEL_8_KEY, x, z, height = 1) }

    /** `South of Shayzien`, `levels = 37, 62` - two pins, cycled. */
    private fun shayzien(): List<Spawn> {
        val ids = listOf(LEVEL_37_KEY, LEVEL_62_KEY)
        return listOf(1478 to 3534, 1480 to 3530)
            .mapIndexed { index, (x, z) -> Spawn(ids[index % ids.size], x, z, height = 0) }
    }

    /** `Woods between Lovakengj and Arceuus`, `levels = 37, 51, 62` - eight pins, cycled. */
    private fun lovakengjWoods(): List<Spawn> {
        val ids = listOf(LEVEL_37_KEY, LEVEL_51_KEY, LEVEL_62_KEY)
        return listOf(
            1588 to 3766, 1590 to 3758, 1591 to 3761, 1593 to 3759,
            1594 to 3763, 1600 to 3752, 1602 to 3755, 1604 to 3751,
        ).mapIndexed { index, (x, z) -> Spawn(ids[index % ids.size], x, z, height = 0) }
    }
}
