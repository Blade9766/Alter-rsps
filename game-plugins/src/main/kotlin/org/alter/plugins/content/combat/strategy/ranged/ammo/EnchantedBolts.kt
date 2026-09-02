package org.alter.plugins.content.combat.strategy.ranged.ammo

import org.alter.api.EquipmentType
import org.alter.api.NpcSkills
import org.alter.api.NpcSpecies
import org.alter.api.Skills
import org.alter.api.WeaponType
import org.alter.api.cfg.Graphic
import org.alter.api.ext.freeze
import org.alter.api.ext.getEquipment
import org.alter.api.ext.hasWeaponType
import org.alter.api.ext.isSpecies
import org.alter.game.model.World
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.plugins.content.mechanics.poison.poison
import org.alter.rscm.RSCM.getRSCM

/**
 * The `(e)` item ids for a gem's bolts, in both their regular and dragon bolt forms.
 *
 * Deliberately excludes the un-enchanted `<gem>_bolts` / `<gem>_dragon_bolts` ids -
 * those are plain ammo with no effect. Matching them was why un-enchanted dragonstone
 * bolts used to add damage.
 *
 * Kept top-level rather than in [EnchantedBolt]'s companion because enum entry
 * constructor arguments are evaluated before the companion object is initialised.
 */
private fun enchantedBolts(gem: String): Array<Int> =
    arrayOf(
        getRSCM("item.${gem}_bolts_e"),
        getRSCM("item.${gem}_dragon_bolts_e"),
    )

/**
 * Enchanted crossbow bolt effects.
 *
 * Activation chances are the wiki's base PvM figures
 * (https://oldschool.runescape.wiki/w/Enchanted_bolts). The Kandarin Hard diary's
 * relative +10% to those rates is not implemented (there is no diary system yet), and
 * the PvP-specific rates - which differ for sapphire (25%), ruby (11%), diamond (5%),
 * emerald (54%) and onyx (10%) - are not split out either; every target uses the PvM
 * column.
 *
 * **Effects only fire on a landed attack.** In the real game some effects, ruby in
 * particular, roll independently of the accuracy check and can trigger through a miss.
 * Gating everything on a landed hit keeps the interaction with
 * [org.alter.plugins.content.combat.dealExactHit] simple, and errs towards the bolts
 * being slightly weaker rather than handing out free damage on a splash.
 */
enum class EnchantedBolt(
    /** The effect's in-game name, for reference. */
    val effect: String,
    /** Base PvM activation chance as a percentage. */
    val chance: Double,
    /** Spotanim played on the target when the effect fires. */
    val gfx: Int,
    val items: Array<Int>,
) {
    OPAL(effect = "Lucky Lightning", chance = 5.0, gfx = Graphic.LUCKY_LIGHTNING, items = enchantedBolts("opal")),
    JADE(effect = "Earth's Fury", chance = 6.0, gfx = Graphic.EARTHS_FURY, items = enchantedBolts("jade")),
    PEARL(effect = "Sea Curse", chance = 6.0, gfx = Graphic.SEA_CURSE, items = enchantedBolts("pearl")),
    TOPAZ(effect = "Down to Earth", chance = 4.0, gfx = Graphic.DOWN_TO_EARTH, items = enchantedBolts("topaz")),
    SAPPHIRE(effect = "Clear Mind", chance = 5.0, gfx = Graphic.CLEAR_MIND, items = enchantedBolts("sapphire")),
    EMERALD(effect = "Magical Poison", chance = 55.0, gfx = Graphic.MAGICAL_POISON, items = enchantedBolts("emerald")),
    RUBY(effect = "Blood Forfeit", chance = 6.0, gfx = Graphic.BLOOD_FORFEIT, items = enchantedBolts("ruby")),
    DIAMOND(effect = "Armour Piercing", chance = 10.0, gfx = Graphic.ARMOUR_PIERCING, items = enchantedBolts("diamond")),
    DRAGONSTONE(effect = "Dragon's Breath", chance = 6.0, gfx = Graphic.DRAGONS_BREATH, items = enchantedBolts("dragonstone")),
    ONYX(effect = "Life Leech", chance = 11.0, gfx = Graphic.LIFE_LEECH, items = enchantedBolts("onyx")),
    ;

    /**
     * Multiplier applied to the shooter's max hit when this effect fires. Diamond and
     * onyx are the only two that scale the roll rather than adding onto it.
     */
    fun maxHitMultiplier(): Double =
        when (this) {
            DIAMOND -> 1.15
            ONYX -> 1.20
            else -> 1.0
        }

    /** Armour Piercing rolls accuracy as though the target had no Ranged defence. */
    fun ignoresDefence(): Boolean = this == DIAMOND

    companion object {
        val values = enumValues<EnchantedBolt>()

        private fun forItem(item: Int): EnchantedBolt? = values.firstOrNull { item in it.items }

        /**
         * Rolls whether the equipped bolts' effect fires on this attack, returning the
         * effect that fired or `null`.
         *
         * [chanceMultiplier] scales the activation rate - the Armadyl crossbow's
         * special attack doubles it.
         */
        fun roll(
            player: Player,
            target: Pawn,
            world: World,
            chanceMultiplier: Double = 1.0,
        ): EnchantedBolt? {
            if (!player.hasWeaponType(WeaponType.CROSSBOW)) {
                return null
            }
            val ammo = player.getEquipment(EquipmentType.AMMO) ?: return null
            val bolt = forItem(ammo.id) ?: return null
            if (!canActivate(bolt, player, target)) {
                return null
            }
            return if ((bolt.chance * chanceMultiplier) / 100.0 > world.randomDouble()) bolt else null
        }

        /**
         * Whether [bolt]'s effect is allowed to fire against [target] at all.
         *
         * Ruby needs the shooter to be able to afford its self-damage, and onyx does
         * nothing to the undead.
         */
        fun canActivate(
            bolt: EnchantedBolt,
            player: Player,
            target: Pawn,
        ): Boolean =
            when (bolt) {
                RUBY -> player.getCurrentHp() > (player.getMaxHp() * 0.10)
                ONYX -> !isUndead(target)
                else -> true
            }

        /**
         * The damage the attack should deal once [bolt]'s effect is folded in.
         *
         * Called before the hit is queued so it can still read the target's pre-hit
         * hitpoints (ruby needs them) and charge the shooter (ruby's self-damage).
         */
        fun applyDamage(
            bolt: EnchantedBolt,
            player: Player,
            target: Pawn,
            damage: Int,
        ): Int {
            val ranged = player.getSkills().getCurrentLevel(Skills.RANGED)
            return when (bolt) {
                OPAL -> damage + (ranged / 10)
                PEARL -> damage + (ranged / if (isFiery(target)) 15 else 20)
                /*
                 * Dragon's Breath is fire damage, so anything that resists dragonfire
                 * shrugs it off. DRACONIC is the species this codebase already uses for
                 * "is a dragon" - see RangedCombatFormula's dragon hunter crossbow.
                 */
                DRAGONSTONE -> if (isDraconic(target)) damage else damage + (ranged / 5)
                /*
                 * Blood Forfeit: pay 10% of your own current hitpoints to deal 20% of
                 * the target's, capped at 100. The self-damage is applied here rather
                 * than through a Hit so it lands the moment the bolt is fired, matching
                 * the effect's "sacrifice" framing.
                 */
                RUBY -> {
                    val cost = (player.getCurrentHp() * 0.10).toInt()
                    player.setCurrentHp(player.getCurrentHp() - cost)
                    Math.min(RUBY_DAMAGE_CAP, (target.getCurrentHp() * 0.20).toInt())
                }
                else -> damage
            }
        }

        /**
         * Side effects that resolve when the hit lands - poison, drains, the freeze and
         * onyx's heal. [damage] is what the hit actually dealt.
         */
        fun applyOnHit(
            bolt: EnchantedBolt,
            player: Player,
            target: Pawn,
            damage: Int,
        ) {
            when (bolt) {
                EMERALD -> target.poison(EMERALD_POISON_DAMAGE) {}

                /*
                 * Clear Mind drains the target's Prayer by a twentieth of the shooter's
                 * Ranged level and gives the shooter back half of what was drained.
                 * Npcs have no Prayer stat, so against them only the shooter's half of
                 * the effect applies.
                 */
                SAPPHIRE -> {
                    val drain = player.getSkills().getCurrentLevel(Skills.RANGED) / 20
                    if (drain > 0) {
                        if (target is Player) {
                            target.getSkills().alterCurrentLevel(Skills.PRAYER, -drain)
                        }
                        if (drain / 2 > 0) {
                            player.getSkills().alterCurrentLevel(Skills.PRAYER, drain / 2)
                        }
                    }
                }

                TOPAZ ->
                    when (target) {
                        is Player -> target.getSkills().alterCurrentLevel(Skills.MAGIC, -1)
                        is Npc -> target.stats.alterCurrentLevel(NpcSkills.MAGIC, -1)
                        else -> {}
                    }

                /* Earth's Fury immobilises for 5 seconds - 8 game ticks at 0.6s each. */
                JADE -> target.freeze(JADE_FREEZE_CYCLES)

                ONYX -> {
                    val heal = (damage * 0.25).toInt()
                    if (heal > 0) {
                        player.setCurrentHp(Math.min(player.getMaxHp(), player.getCurrentHp() + heal))
                    }
                }

                else -> {}
            }
        }

        private fun isFiery(pawn: Pawn): Boolean = pawn is Npc && pawn.isSpecies(NpcSpecies.FIERY)

        private fun isDraconic(pawn: Pawn): Boolean = pawn is Npc && pawn.isSpecies(NpcSpecies.DRACONIC)

        private fun isUndead(pawn: Pawn): Boolean = pawn is Npc && pawn.isSpecies(NpcSpecies.UNDEAD)

        private const val RUBY_DAMAGE_CAP = 100
        private const val EMERALD_POISON_DAMAGE = 5
        private const val JADE_FREEZE_CYCLES = 8
    }
}
