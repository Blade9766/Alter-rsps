package org.alter.plugins.content.npcs.dragon

import org.alter.api.cfg.Animation
import org.alter.api.cfg.Sound
import org.alter.api.ext.createProjectile
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.plugins.content.combat.Combat
import org.alter.plugins.content.combat.dealHit
import org.alter.plugins.content.combat.formula.DragonfireFormula
import org.alter.plugins.content.combat.strategy.CombatStrategy
import org.alter.plugins.content.combat.strategy.MeleeCombatStrategy
import org.alter.plugins.content.combat.strategy.RangedCombatStrategy
import org.alter.plugins.content.combat.strategy.playSpellSound

/**
 * Dragonfire: the attack that makes an adult dragon a dragon.
 *
 * Registered against every [Dragons.BREATHING_KEYS] id in [DragonPlugin]. The babies do not get it,
 * because they do not have it - no baby dragon in OSRS breathes, which is the whole reason a level
 * 48 baby blue is a training monster and the level 111 adult standing next to it is not.
 *
 * ## Why this exists at all
 *
 * `content/npcs/dungeon` had to leave the blue and black dragons out of an otherwise complete
 * dungeon roster, and said why: *"Blue dragon (111) and black dragon (227) - dragonfire. Without it
 * they would be ordinary melee monsters carrying a boss's stats, and antifire gear would do
 * nothing."* Both halves of that matter. A dragon without its breath is a pushover that hits for 10
 * at melee range; and every piece of antifire equipment in the game - the anti-dragon shield, the
 * dragonfire shield and ward, antifire potions, the `DRAGONFIRE_IMMUNITY_ATTR` the super antifire
 * sets - is dead weight with nothing in the world that breathes except the King Black Dragon.
 *
 * ## Why a strategy and not an `onNpcCombat` loop
 *
 * The King Black Dragon breathes through `onNpcCombat`, which is the older hook and is not usable
 * here. `onNpcCombat` replaces [org.alter.plugins.content.combat.CombatPlugin]'s loop wholesale, and
 * that loop is the only code in the game that walks an npc towards a target. The KBD never notices,
 * because it lives in a small lair and its own loop calls `moveToAttackRange`; a green dragon in the
 * Wilderness would notice immediately. A [CombatStrategy] keeps the engine's routing, leashing, line
 * of sight, retaliation and attack speed and replaces only the swing - the same argument
 * `content/npcs/chaosdruid` sets out.
 *
 * ## What is judged rather than published
 *
 * Two things, and they are stated here rather than buried:
 *
 * - **[BREATH_ODDS_IN_MELEE]**, how often a dragon standing next to you breathes instead of clawing.
 *   The wiki publishes both max hits - `14 (Melee), 50 (Dragonfire)` - and so establishes that a
 *   dragon does both, but says nothing about the split. One swing in three is the breath.
 * - **[DRAGONFIRE_RANGE]**, taken from `content/npcs/kbd/KbdConfigsPlugin`, which configures the
 *   King Black Dragon's `attackRange` at 6. The chromatic dragons' infoboxes publish no attack range
 *   at all, so the in-repo precedent for a dragon is used rather than a number invented for these.
 *   A def that sets its own `attackRange` still wins, through [Combat.npcAttackRange].
 *
 * Everything else is sourced: the breath's max hit is the pages' own `50`, the animation is
 * [Animation.CHROMATIC_DRAGON_DRAGONFIRE_ATTACK], and the projectile is the one the King Black
 * Dragon's own fire attack already uses.
 *
 * ## One consequence of the range worth knowing about
 *
 * `CombatPlugin`'s loop uses the *same* number for "can attack from here" and "stop walking", so a
 * dragon given a six-tile breath range will stand and breathe rather than closing to melee. That is
 * the right half of the trade: it is what makes running from a green dragon dangerous, and it is what
 * players expect. The wrong half is that a dragon which has already lost its target's melee range
 * will not walk back in - it stays put and breathes. Giving the strategy melee range instead would
 * fix that and delete dragonfire entirely, which is not a trade worth making. A dragon still meets
 * anyone who walks up to it, and still claws them when they do.
 *
 * ## Why there is no `prepareAttack`
 *
 * For the reason `content/npcs/chaosdruid` documents: `prepareAttack` leaves the npc's
 * [org.alter.game.model.combat.CombatStyle] set to MAGIC, and [MeleeCombatStrategy] rolls straight
 * through `MeleeCombatFormula`, which accepts only stab, slash and crush. The very next claw would
 * throw out of the combat queue and the dragon would stop fighting. The breath rolls its accuracy
 * through [DragonfireFormula] directly instead, which is what the KBD does too - it just also calls
 * `prepareAttack` before *every* attack, melee included, so it never sees the problem.
 */
internal object DragonfireCombatStrategy : CombatStrategy {
    /** The breath travels, so range is tested as line of sight rather than line of walk. */
    override val usesProjectile: Boolean = true

    override fun getAttackRange(pawn: Pawn): Int = Combat.npcAttackRange(pawn, DRAGONFIRE_RANGE)

    override fun canAttack(
        pawn: Pawn,
        target: Pawn,
    ): Boolean = true

    /**
     * Claw at melee range, breathe otherwise - and breathe some of the time at melee range too,
     * which is what makes standing next to a dragon dangerous rather than merely slow.
     */
    override fun attack(
        pawn: Pawn,
        target: Pawn,
    ) {
        if (pawn !is Npc) {
            MeleeCombatStrategy.attack(pawn, target)
            return
        }
        val adjacent = Combat.edgeDistance(pawn, target) <= MELEE_RANGE
        if (adjacent && !pawn.world.chance(1, BREATH_ODDS_IN_MELEE)) {
            MeleeCombatStrategy.attack(pawn, target)
        } else {
            breathe(pawn, target)
        }
    }

    private fun breathe(
        pawn: Npc,
        target: Pawn,
    ) {
        val world = pawn.world
        val projectile =
            pawn.createProjectile(
                target,
                gfx = DRAGONFIRE_PROJECTILE,
                startHeight = 43,
                endHeight = 31,
                delay = 51,
                angle = 15,
                steepness = 127,
            )
        pawn.animate(Animation.CHROMATIC_DRAGON_DRAGONFIRE_ATTACK)
        world.spawn(projectile)
        /*
         * The breath was silent until this line. A monster attacking through its own CombatStrategy
         * never touches `defaultAttackSound` - that field is only read by the three ordinary
         * strategies - so the sound `named-combat-media.json` gives a dragon covers its *claw* and
         * nothing else. The same gap the battle mages had.
         *
         * An npc has no client of its own, so the breath has to be heard through the player it is
         * aimed at; `playSpellSound` is the helper the casters in this tree already use for that.
         */
        playSpellSound(pawn, target, Sound.DRAGONBREATH)

        /*
         * The whole point of the formula: it is the only place in the codebase that reads the
         * anti-dragon shield, the dragonfire shield and ward, the antifire potion's charges and
         * DRAGONFIRE_IMMUNITY_ATTR, and it is what turns a 50 into a 0. It also halves-and-more
         * against Protect from Magic, but only for a BASIC_DRAGON - which is why [DragonPlugin]
         * adds that species tag at spawn.
         */
        pawn.dealHit(
            target = target,
            formula = DragonfireFormula(maxHit = MAX_BREATH_HIT),
            delay = RangedCombatStrategy.getHitDelay(pawn.getFrontFacingTile(target), target.getCentreTile()) - 1,
        )
    }

    /** Adjacent, in [Combat.edgeDistance] terms - which already accounts for the dragon's size. */
    private const val MELEE_RANGE = 1

    /**
     * How far a dragon can breathe, unless its own def says otherwise. Judged from the King Black
     * Dragon's configured range; see the class doc.
     */
    private const val DRAGONFIRE_RANGE = 6

    /** One swing in N at melee range is the breath rather than the claw. Judged; see the class doc. */
    private const val BREATH_ODDS_IN_MELEE = 3

    /**
     * `max hit = ... 50 ([[Dragonfire]])`, published identically on the bronze, green, blue, red and
     * black dragons. The level 247 black dragon's page writes `50+`, which is not a number; it takes
     * the same 50 as the rest.
     */
    private const val MAX_BREATH_HIT = 50

    /** The King Black Dragon's own fire-breath projectile, reused because it is the same attack. */
    private const val DRAGONFIRE_PROJECTILE = 393
}
