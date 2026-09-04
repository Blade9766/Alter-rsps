package org.alter.plugins.content.npcs.battlemage

import org.alter.api.cfg.Animation
import org.alter.api.cfg.Graphic
import org.alter.api.cfg.Sound

/**
 * The three Mage Arena battle mages - one for each god - and the nine pins the OSRS Wiki puts them
 * on.
 *
 * See [BattleMageCombatStrategy] for the god spells and [BattleMagePlugin] for the wiring. Stats
 * come from `data/cfg/npcs/monsterStats.json`: 120 hitpoints, **magic level 50**, and 1s across
 * attack, strength and defence - these are pure casters and nothing else.
 *
 * ## The animations were wrong before this package, in two different ways
 *
 * The Saradomin and Zamorak mages had attack and block backwards - `Animation.GOD_SPELL` (811), the
 * cast, was being played as the block and `HUMAN_STAFF_DEFEND` (415) as the attack. The **Guthix**
 * mage is a different problem again: it is a *gnome*, on a rig of its own
 * (`GUTHIX_BATTLE_MAGE_ATTACK` 197, `GNOME_HIT` 193, `GNOME_DEATH` 196), and one cache name covering
 * two rigs is exactly the case `id-combat-media.json` exists for. Both are fixed; see
 * `npc-animations/README.md`.
 *
 * ## The cape exemption is real
 *
 * `aggressive = Yes, unless wearing the same cape as the battle mage`, and the page is precise about
 * it: "only one type of mage will be tolerant of you at a time, even if you're wearing an item
 * aligned with them" - so it is the cape specifically, not god equipment generally, and it exempts
 * only the matching mage.
 *
 * That needed a one-line change in `content/mechanics/aggro/NpcAggroPlugin`, which used to install
 * its default `aggroCheck` unconditionally from a *global* spawn hook - and global hooks run after
 * per-npc ones, so a per-monster exemption was silently discarded. It now installs the default only
 * when the npc has none, which is what lets [BattleMagePlugin] give these three their own.
 */
internal data class BattleMage(
    /** The wiki's own version label - the god this one serves. */
    val god: String,
    val npcKey: String,
    /** The cape that makes this mage tolerant, and only this mage. */
    val capeKey: String,
    /** The god spell it casts, as an impact graphic - god spells have no travelling projectile. */
    val impactGfx: Int,
    /** Its cast animation. The Guthix mage is a gnome and has its own. */
    val castAnimation: Int,
    /**
     * The god spell's own clip, from `Sound`'s named constants.
     *
     * These three needed stating rather than being left to
     * [org.alter.plugins.content.npcs.animations.MonsterAnimationsPlugin]: that plugin fills an
     * npc's `defaultAttackSound` from the *def*, and a battle mage attacks through
     * [BattleMageCombatStrategy] instead, which never reads it. Nothing else in the codebase would
     * have made a sound here.
     */
    val castSound: Int,
)

internal object BattleMages {
    /** Wiki `combat = 54` on all three. */
    const val COMBAT_LEVEL = 54

    /** Wiki `respawn = 150`, in game ticks - by far the longest in this bestiary pass. */
    const val RESPAWN_CYCLES = 150

    /**
     * Wiki `max hit = 20`, identical for all three god spells. The page's own strategy note is that
     * "the mages can hit up to 20, and have high accuracy".
     */
    const val MAX_HIT = 20

    /** Aggression radius in tiles, matching every other aggressive monster package in this tree. */
    const val AGGRO_RADIUS = 4

    /** Cycles between aggression sweeps, matching every other monster package. */
    const val AGGRO_SEARCH_DELAY = 4

    /**
     * How long a mage stays interested, in cycles - the engine's own `DEFAULT_AGGRO_TIMER`, stated
     * because a def built from `monsterStats.json` starts with a **0** timer, which
     * `NpcAggroPlugin` reads as "stop being aggressive".
     */
    const val AGGRO_TIMER = 1000

    /** They stand on marked glyphs around the arena centre and should stay near them. */
    const val WALK_RADIUS = 2

    /** The engine's own magic default. The page publishes no attack range. */
    const val SPELL_RANGE = 10

    val ALL: List<BattleMage> =
        listOf(
            BattleMage(
                god = "Saradomin",
                npcKey = "npc.battle_mage_1611",
                capeKey = "item.saradomin_cape",
                impactGfx = Graphic.SARADOMIN_STRIKE,
                castAnimation = Animation.GOD_SPELL,
                castSound = Sound.SARADOMIN_STRIKE,
            ),
            BattleMage(
                god = "Zamorak",
                npcKey = "npc.battle_mage",
                capeKey = "item.zamorak_cape",
                impactGfx = Graphic.FLAMES_OF_ZAMORAK,
                castAnimation = Animation.GOD_SPELL,
                castSound = Sound.FLAMES_OF_ZAMORAK,
            ),
            BattleMage(
                god = "Guthix",
                npcKey = "npc.battle_mage_1612",
                capeKey = "item.guthix_cape",
                impactGfx = Graphic.CLAWS_OF_GUTHIX,
                // Not GOD_SPELL: this one is a gnome, and 197 is named for it outright.
                castAnimation = Animation.GUTHIX_BATTLE_MAGE_ATTACK,
                castSound = Sound.CLAWS_OF_GUTHIX,
            ),
        )

    /**
     * The plane the Mage Arena is really on.
     *
     * Its `LocLine` says `plane = 0`, and that is wrong in this cache: `SpawnTileProbe` found every
     * tile for twenty-seven squares around the arena flagged BLOCK_WALK on plane 0 - it is the solid
     * Wilderness rock the arena is built above - while plane 1 carries 566 painted tiles with all
     * nine pins on real floor. The same class of correction the God Wars Dungeon needed in the first
     * bestiary pass.
     */
    const val PLANE = 1

    /**
     * The nine published pins, all in the Mage Arena.
     *
     * The first six ring the arena centre in pairs and the last three sit on the eastern walkway,
     * which is the layout the page's map shows; they are dealt one mage each in turn so all three
     * gods are represented at both.
     */
    val TILES: List<Pair<Int, Int>> =
        listOf(
            3098 to 3925, 3098 to 3942, 3100 to 3927, 3100 to 3940, 3102 to 3929,
            3102 to 3938, 3110 to 3934, 3113 to 3934, 3116 to 3934,
        )

    /** Wiki tertiary. The Mage Arena is deep Wilderness, so this needs no position test. */
    const val LOOTING_BAG_ONE_IN = 3

    /** Every battle mage key, for code that needs "is this one of ours". */
    val ALL_KEYS: List<String> by lazy { ALL.map { it.npcKey } }
}
