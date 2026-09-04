package org.alter.plugins.content.npcs.imp

import org.alter.api.ElementalWeakness
import org.alter.api.Elements
import org.alter.api.NpcSpecies
import org.alter.game.model.combat.CombatStyle
import org.alter.plugins.content.npcs.WeightedDrop
import org.alter.rscm.RSCM.getRSCM

/**
 * Imps - the level 2 nuisance that wanders half of Gielinor, and the level 7 God Wars
 * Dungeon version that exists to be killed for Zamorak kill-count.
 *
 * Everything here is the OSRS Wiki's `Infobox Monster`, read as raw wikitext, with both
 * published versions defined rather than only the one that gets spawned - the same rule
 * `content/npcs/critters` and `content/npcs/goblin` follow, and for the same reason: an
 * undefined version silently inherits [org.alter.game.model.combat.NpcCombatDef.DEFAULT],
 * which is 10 hitpoints and zeroes across the board.
 *
 * Four ids in this cache carry the word "Imp". Only two of them are this monster:
 *
 * | id   | name         | level | options |
 * |------|--------------|-------|---------|
 * | 5007 | Imp          | 2     | Attack  |
 * | 3134 | Imp          | 7     | Attack  |
 * | 5728 | Imp          | 1     | *none*  |
 * | 3355 | Imp Champion | 14    | Attack  |
 *
 * **5728 is not a monster.** It is the form a player is transformed into, with no options
 * at all, and giving it a combat def would be meaningless. **3355 is the Imp Champion**, a
 * Champions' Challenge boss with its own page, its own stats and a minigame this server
 * does not have; it is a different monster that happens to share the word, and it is
 * deliberately not defined here.
 *
 * Worth stating outright, because each is easy to get wrong:
 *
 * 1. **Both versions are `aggressive = No`.** Even the God Wars one - it is the only thing
 *    in that dungeon that will not start a fight with you. So there is no `aggro { }` block.
 * 2. **Imps are demons** (`attributes = demon`), which is what makes Arclight, the
 *    demonbane swords and the scorching bow work against them. Declared through
 *    `species { +NpcSpecies.DEMON }`; nothing in the engine derives species from the cache.
 * 3. **The elemental weakness is Water at 10%**, added by the 25 June 2025 "Summer Sweep
 *    Up: Combat" update. It is stated once, unversioned, so it applies to both.
 * 4. **Max hits are not stored.** The wiki publishes 0 for the level 2 and 1 for the level
 *    7; this server derives both from strength and strength bonus through
 *    `MeleeCombatFormula`, exactly as it does for every other monster here.
 *
 * Animations are 169 attack / 170 block / 172 death, from this project's own
 * `npc-animations/named-combat-media.json` `IMP` entry, cross-checked against
 * `openosrs-animations.json`, which observes [170, 172, 4289, 4288, 169] for npc 5007 and
 * [170, 172, 169] for 3134. The two extra sequences on 5007 are the teleport pair - see
 * [ImpPlugin] for why they are not used. Restating the three here is not redundant:
 * setting a combat def at all takes an npc off `MonsterAnimationsPlugin`'s resolver path,
 * though that plugin still matches the name "Imp" to `IMP` and fills in sounds 534/536/535.
 */
internal data class ImpVariant(
    val name: String,
    val combatLevel: Int,
    val npcKey: String,
    val hitpoints: Int,
    val attack: Int,
    val strength: Int,
    val defence: Int,
    val respawnCycles: Int,
)

internal object Imps {
    /** The common level 2 imp. Wiki version 1, `id1 = 5007`. */
    const val NORMAL_ID = "npc.imp_5007"

    /** The level 7 God Wars Dungeon imp. Wiki version 2, `id2 = 3134`. */
    const val GWD_ID = "npc.imp_3134"

    /** `mage = 1` and `range = 1`, stated unversioned. */
    const val MAGIC_LEVEL = 1
    const val RANGED_LEVEL = 1

    /** `attack speed = 4`, `attack style = Stab`, both unversioned. */
    const val ATTACK_SPEED = 4
    val COMBAT_STYLE = CombatStyle.STAB

    /** `attbns` / `strbns`, unversioned - the level 7 is no better armed than the level 2. */
    const val ATTACK_BONUS = -42
    const val STRENGTH_BONUS = -37

    /** `dstab` through `dheavy` are all -42, so one number covers every slot. */
    const val DEFENCE_BONUS = -42

    const val ATTACK_ANIMATION = 169
    const val BLOCK_ANIMATION = 170
    const val DEATH_ANIMATION = 172

    /** `elementalweaknesstype = Water`, `elementalweaknesspercent = 10`, unversioned. */
    val ELEMENTAL_WEAKNESS = ElementalWeakness(Elements.WATER, 10)

    /** `attributes = demon`. */
    val SPECIES = listOf(NpcSpecies.DEMON)

    val VARIANTS: List<ImpVariant> =
        listOf(
            ImpVariant(
                name = "Imp",
                combatLevel = 2,
                npcKey = NORMAL_ID,
                hitpoints = 8,
                attack = 1,
                strength = 1,
                defence = 1,
                respawnCycles = 50,
            ),
            ImpVariant(
                name = "Imp (God Wars Dungeon)",
                combatLevel = 7,
                npcKey = GWD_ID,
                hitpoints = 10,
                attack = 5,
                strength = 5,
                defence = 6,
                respawnCycles = 60,
            ),
        )
}

/**
 * What an imp leaves behind.
 *
 * The main table is the one genuinely tidy thing about this page: the five wiki sub-tables
 * (Beads, Equipment, Food, Tools, Other) publish numerators that sum to **exactly 128**,
 * with no "Nothing" row. So this is one honest 128-weight roll rather than the rescaled
 * approximation [org.alter.plugins.content.npcs.DropRoll] has to make elsewhere - every
 * rarity below is the wiki's real rate, not a proportion of a truncated table.
 *
 * The four beads are why anyone kills an imp: they are the Imp Catcher quest items. That
 * quest does not exist here yet, but beads are ordinary items and drop regardless, so
 * whoever writes it will find them already in the world.
 *
 * **Not modelled**, flagged rather than faked:
 * - **Ecumenical key.** Its own drops template says keys "are only dropped in the
 *   Wilderness God Wars Dungeon", and that dungeon's mapsquare (46_157) is not in this
 *   cache - see [ImpSpawns], which drops those three spawns for the same reason. With
 *   nowhere for the condition to be true, an unconditional 1/60 would just be wrong.
 * - **The Combat Achievement tiers** that improve the key rate to 1/55 through 1/40. No
 *   Combat Achievements system exists here, and the base rate is the one that applies
 *   until it does.
 */
internal object ImpDrops {
    /** `100%`: one Fiendish ashes, on every kill, since the 16 June 2021 update. */
    val ALWAYS: List<String> = listOf("item.fiendish_ashes")

    /**
     * The five sub-tables flattened into one roll. Weights are the wiki's `x/128`
     * numerators and they total 128 on the nose - see [ImpDrops] - so nothing here is
     * approximated.
     */
    val TABLE: List<WeightedDrop> =
        listOf(
            // Beads - 20/128 between them. The Imp Catcher drops.
            WeightedDrop(getRSCM("item.black_bead"), weight = 5),
            WeightedDrop(getRSCM("item.red_bead"), weight = 5),
            WeightedDrop(getRSCM("item.white_bead"), weight = 5),
            WeightedDrop(getRSCM("item.yellow_bead"), weight = 5),
            // Equipment - 16/128.
            WeightedDrop(getRSCM("item.bronze_bolts"), weight = 8),
            WeightedDrop(getRSCM("item.blue_wizard_hat"), weight = 8),
            // Food - 24/128.
            WeightedDrop(getRSCM("item.egg"), weight = 5),
            WeightedDrop(getRSCM("item.raw_chicken"), weight = 5),
            WeightedDrop(getRSCM("item.burnt_bread"), weight = 4),
            WeightedDrop(getRSCM("item.burnt_meat"), weight = 4),
            WeightedDrop(getRSCM("item.cabbage"), weight = 2),
            WeightedDrop(getRSCM("item.bread_dough"), weight = 2),
            WeightedDrop(getRSCM("item.bread"), weight = 1),
            WeightedDrop(getRSCM("item.cooked_meat"), weight = 1),
            // Tools - 31/128.
            WeightedDrop(getRSCM("item.hammer"), weight = 8),
            WeightedDrop(getRSCM("item.tinderbox"), weight = 5),
            WeightedDrop(getRSCM("item.shears"), weight = 4),
            WeightedDrop(getRSCM("item.bucket"), weight = 4),
            WeightedDrop(getRSCM("item.bucket_of_water"), weight = 2),
            WeightedDrop(getRSCM("item.jug"), weight = 2),
            WeightedDrop(getRSCM("item.jug_of_water"), weight = 2),
            WeightedDrop(getRSCM("item.pot"), weight = 2),
            WeightedDrop(getRSCM("item.pot_of_flour"), weight = 2),
            // Other - 37/128.
            WeightedDrop(getRSCM("item.ball_of_wool"), weight = 8),
            WeightedDrop(getRSCM("item.mind_talisman"), weight = 7),
            WeightedDrop(getRSCM("item.ashes"), weight = 6),
            WeightedDrop(getRSCM("item.clay"), weight = 4),
            WeightedDrop(getRSCM("item.cadava_berries"), weight = 4),
            WeightedDrop(getRSCM("item.grain"), weight = 3),
            WeightedDrop(getRSCM("item.chefs_hat"), weight = 2),
            WeightedDrop(getRSCM("item.flyer"), weight = 2),
            // The Apothecary's potion (item 195), not a Herblore one.
            WeightedDrop(getRSCM("item.potion"), weight = 1),
        )

    /** The wiki's total for [TABLE], and the reason it needs no rescaling. */
    const val TABLE_WEIGHT = 128

    /** Tertiary, rolled independently of [TABLE]. */
    const val ENSOULED_HEAD_CHANCE = 1.0 / 25.0

    /** Tertiary, and Wilderness kills only. */
    const val LOOTING_BAG_CHANCE = 1.0 / 15.0

    /** Tertiary. The reason anyone keeps killing imps past their first set of beads. */
    const val CHAMPION_SCROLL_CHANCE = 1.0 / 5000.0
}
