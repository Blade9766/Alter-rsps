package org.alter.plugins.content.combat

import org.alter.api.cfg.Animation
import org.alter.api.cfg.Sound
import org.alter.game.model.combat.CombatStyle

/**
 * Which clip a weapon makes when it swings, by weapon class *and* by the attack type of
 * the swing - so a scimitar's Chop and its Lunge are different sounds, and neither is the
 * generic thrust that every sword used to play.
 *
 * None of these ids can be read out of the cache: no combat sequence at this revision
 * carries embedded sound data, and neither `NpcType` nor `ItemType` has an attack-sound
 * field. They come from [Sound]'s own tables, whose constant names encode the weapon class
 * and the attack type (`HACKSWORD_SLASH`, `_2H_CRUSH`, `MACE_STAB`, ...) and agree with the
 * per-weapon list in that file's header comment wherever the two overlap.
 *
 * Players and NPCs answer "which attack type is this swing?" from different places:
 *
 * - a player's attack animation and attack type both fall out of the attack style they
 *   selected, so [CombatConfigs.getWeaponAttackSound] resolves the style and passes the
 *   resulting [CombatStyle] to [Weapon.forAttackType];
 * - an NPC never selects a style. The [CombatStyle] on its combat def is a *stats* fact -
 *   it picks which defence bonus the roll goes against - and the wiki's answer can
 *   disagree with what the model is holding: Varrock's guards are published Crush and
 *   swing a longsword. What an NPC's attack should sound like follows the animation the
 *   player can see, so NPCs come in through [forAnimation].
 */
object WeaponSounds {
    /**
     * A weapon class and the three clips it owns. Classes with no clip of their own for an
     * attack type reuse their nearest one - the axe set has no thrust, the mace set no
     * slash - and single-clip weapons (whips, bows) hold the same id three times.
     */
    enum class Weapon(
        val stab: Int,
        val slash: Int,
        val crush: Int,
        /** Used when a swing's attack type is unknown - see [forAnimation]. */
        val defaultAttackType: CombatStyle,
    ) {
        /** Scimitars and longswords. */
        SWORD(Sound.HACKSWORD_STAB, Sound.HACKSWORD_SLASH, Sound.HACKSWORD_CRUSH, CombatStyle.SLASH),

        /** Daggers - the same set claws use, thrust-first. */
        DAGGER(Sound.STABSWORD_STAB, Sound.STABSWORD_SLASH, Sound.STABSWORD_CRUSH, CombatStyle.STAB),
        CLAWS(Sound.STABSWORD_STAB, Sound.STABSWORD_SLASH, Sound.STABSWORD_CRUSH, CombatStyle.SLASH),

        TWO_HANDED(Sound._2H_STAB, Sound._2H_SLASH, Sound._2H_CRUSH, CombatStyle.SLASH),

        /** Godswords have their own hit/smash pair and no stab style of their own. */
        GODSWORD(
            Sound.GODWARS_GODSWORD_SLASH,
            Sound.GODWARS_GODSWORD_SLASH,
            Sound.GODWARS_GODSWORD_CRUSH,
            CombatStyle.SLASH,
        ),

        /** Battleaxes. The axe set is a swing and a smash; it has no thrust. */
        AXE(Sound.BAXE_SLASH, Sound.BAXE_SLASH, Sound.BAXE_CRUSH, CombatStyle.SLASH),

        /**
         * Pickaxes. No pickaxe-specific combat clip exists in the sound tables, so the axe
         * pair stands in, with the heavy [Sound.BAXE_CRUSH] on the Spike/Impale thrusts
         * rather than the swing.
         */
        PICKAXE(Sound.BAXE_CRUSH, Sound.BAXE_SLASH, Sound.BAXE_CRUSH, CombatStyle.STAB),

        /** Warhammers and the bludgeon - crush whatever the style. */
        HAMMER(Sound.WARHAMMER_CRUSH, Sound.WARHAMMER_CRUSH, Sound.WARHAMMER_CRUSH, CombatStyle.CRUSH),

        /** Halberds and spears: a long-shaft jab, swipe and pound. */
        POLEARM(Sound.STAFF_STAB, Sound.SCYTHE_SLASH, Sound.STAFF_CRUSH, CombatStyle.STAB),

        SCYTHE(Sound.SCYTHE_STAB, Sound.SCYTHE_SLASH, Sound.SCYTHE_HIT, CombatStyle.SLASH),

        /** Maces and flails - the mace set has no slash clip. */
        MACE(Sound.MACE_STAB, Sound.MACE_CRUSH, Sound.MACE_CRUSH, CombatStyle.CRUSH),

        /** Staves, battlestaves and tridents swung as a melee weapon. */
        STAFF(Sound.STAFF_STAB, Sound.STAFF_HIT, Sound.STAFF_HIT, CombatStyle.CRUSH),

        WHIP(Sound.WHIP, Sound.WHIP, Sound.WHIP, CombatStyle.SLASH),

        UNARMED(Sound.UNARMED_PUNCH, Sound.UNARMED_PUNCH, Sound.UNARMED_PUNCH, CombatStyle.CRUSH),

        /** Unarmed's aggressive style, which is a kick rather than a punch. */
        KICK(Sound.UNARMED_KICK, Sound.UNARMED_KICK, Sound.UNARMED_KICK, CombatStyle.CRUSH),

        BOW(Sound.LONGBOW, Sound.LONGBOW, Sound.LONGBOW, CombatStyle.RANGED),
        CROSSBOW(Sound.CROSSBOW, Sound.CROSSBOW, Sound.CROSSBOW, CombatStyle.RANGED),
        THROWN(Sound.DART, Sound.DART, Sound.DART, CombatStyle.RANGED),

        /** Anything with no set of its own - bulwarks, novelty weapons, a salamander's scorch. */
        GENERIC(Sound.BAXE_CRUSH, Sound.BAXE_SLASH, Sound.BAXE_CRUSH, CombatStyle.CRUSH),
        ;

        /**
         * [CombatStyle.RANGED], [CombatStyle.MAGIC] and [CombatStyle.NONE] fall in with
         * crush: no melee class is ever asked for one, and the weapons that are - bows and
         * the like - hold one clip in all three slots anyway.
         */
        fun forAttackType(attackType: CombatStyle): Int =
            when (attackType) {
                CombatStyle.STAB -> stab
                CombatStyle.SLASH -> slash
                else -> crush
            }
    }

    /**
     * What each generic human combat animation depicts. Both halves are read straight off
     * the animation's own cache-derived name in [Animation] - `HUMAN_BLUNT_STAB` is a mace
     * thrust, `HUMAN_AXE_SWING` an axe slash - which is why this half doesn't need the
     * second source the sound ids do.
     *
     * The weapon here is the one the animation is *named* for. An NPC whose model holds
     * something else - Varrock's longsword guards play the dagger-stab animation - passes
     * its own [Weapon] to [forAnimation] to override that half.
     */
    private val ANIMATIONS: Map<Int, Pair<Weapon, CombatStyle>> =
        mapOf(
            Animation.HUMAN_DAGGER_STAB to (Weapon.DAGGER to CombatStyle.STAB),
            Animation.HUMAN_SLASH_SWORD_ATTACK to (Weapon.SWORD to CombatStyle.SLASH),
            Animation.HUMAN_CLAWS_ATTACK to (Weapon.CLAWS to CombatStyle.SLASH),
            Animation.HUMAN_AXE_SWING to (Weapon.AXE to CombatStyle.SLASH),
            Animation.HUMAN_DHAROKS_GREATAXE_SWING to (Weapon.AXE to CombatStyle.SLASH),
            Animation.HUMAN_BLUNT_STAB to (Weapon.MACE to CombatStyle.STAB),
            Animation.HUMAN_BLUNT_SWING to (Weapon.MACE to CombatStyle.CRUSH),
            Animation.HUMAN_2H_SWORD_ATTACK to (Weapon.TWO_HANDED to CombatStyle.SLASH),
            Animation.HUMAN_2H_SWORD_CRUSH to (Weapon.TWO_HANDED to CombatStyle.CRUSH),
            Animation.HUMAN_2H_STRAIGHT_SWORD_SLASH to (Weapon.GODSWORD to CombatStyle.SLASH),
            Animation.HUMAN_2H_STRAIGHT_SWORD_SMASH to (Weapon.GODSWORD to CombatStyle.CRUSH),
            Animation.HUMAN_SPEAR_STAB to (Weapon.POLEARM to CombatStyle.STAB),
            Animation.HUMAN_SPEAR_SLICE to (Weapon.POLEARM to CombatStyle.SLASH),
            Animation.HUMAN_SPEAR_SMACK to (Weapon.POLEARM to CombatStyle.CRUSH),
            Animation.HUMAN_SCYTHE_OF_VITUR_ATTACK to (Weapon.SCYTHE to CombatStyle.SLASH),
            Animation.HUMAN_WHIP_SWING to (Weapon.WHIP to CombatStyle.SLASH),
            Animation.HUMAN_PUNCH to (Weapon.UNARMED to CombatStyle.CRUSH),
            Animation.HUMAN_KICK to (Weapon.KICK to CombatStyle.CRUSH),
            Animation.HUMAN_BOW_ATTACK to (Weapon.BOW to CombatStyle.RANGED),
            Animation.HUMAN_KARILS_CROSSBOW_ATTACK to (Weapon.CROSSBOW to CombatStyle.RANGED),
        )

    /** The animations this table can sound, for callers that want to check first. */
    val soundedAnimations: Set<Int> get() = ANIMATIONS.keys

    /**
     * The clip for an attack animation, or `null` if it isn't one of the generic human
     * weapon animations - a monster's own bite or claw swipe has no weapon to sound.
     */
    fun forAnimation(attackAnimation: Int): Int? {
        val (weapon, attackType) = ANIMATIONS[attackAnimation] ?: return null
        return weapon.forAttackType(attackType)
    }

    /**
     * The clip for an attack animation performed with a known weapon, for an NPC whose
     * model carries something the animation isn't named for. The animation still decides
     * the attack type; the weapon decides which set it is drawn from. An animation this
     * table doesn't know falls back to the weapon's [Weapon.defaultAttackType] rather than
     * going silent - audio never fails an attack.
     */
    fun forAnimation(
        weapon: Weapon,
        attackAnimation: Int,
    ): Int = weapon.forAttackType(ANIMATIONS[attackAnimation]?.second ?: weapon.defaultAttackType)
}
