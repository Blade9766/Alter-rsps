package org.alter.plugins.content.combat

import org.alter.api.EquipmentType
import org.alter.api.WeaponType
import org.alter.api.ext.getAttackStyle
import org.alter.api.ext.getWeaponType
import org.alter.api.ext.getEquipment
import org.alter.api.ext.hasEquipped
import org.alter.api.ext.hasWeaponType
import org.alter.game.model.combat.*
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.plugins.content.combat.strategy.CombatStrategy
import org.alter.plugins.content.combat.strategy.MagicCombatStrategy
import org.alter.plugins.content.combat.strategy.MeleeCombatStrategy
import org.alter.plugins.content.combat.strategy.RangedCombatStrategy
import org.alter.plugins.content.combat.strategy.SalamanderCombatStrategy

/**
 * @author Tom <rspsmods@gmail.com>
 */
object CombatConfigs {
    private const val PLAYER_DEFAULT_ATTACK_SPEED = 4

    private const val MIN_ATTACK_SPEED = 1

    /*
     * Attack-style varp value for a salamander's Flare, the one option whose speed differs
     * from the weapon's cache attack rate. Its other two options, Scorch and Blaze, are
     * resolved through [WeaponStyles] like every other button.
     */
    private const val SALAMANDER_FLARE = 1

    private val DEFENDERS =
        arrayOf(
            "item.bronze_defender",
            "item.iron_defender",
            "item.steel_defender",
            "item.mithril_defender",
            "item.black_defender",
            "item.adamant_defender",
            "item.rune_defender",
            "item.dragon_defender",
            "item.dragon_defender_t",
            "item.avernic_defender",
        )

    private val BOOKS =
        arrayOf(
            "item.holy_book",
            "item.book_of_balance",
            "item.unholy_book",
            "item.book_of_law",
            "item.book_of_war",
            "item.book_of_darkness",
            "item.mages_book",
            "item.tome_of_fire",
            "item.tome_of_fire_empty",
        )

    private val BOXING_GLOVES =
        arrayOf(
            "item.boxing_gloves",
            "item.boxing_gloves_7673",
            "item.beach_boxing_gloves",
            "item.beach_boxing_gloves_11706",
        )

    private val GODSWORDS =
        arrayOf(
            "item.armadyl_godsword",
            "item.armadyl_godsword_or",
            "item.bandos_godsword",
            "item.bandos_godsword_or",
            "item.saradomin_godsword",
            "item.saradomin_godsword_or",
            "item.zamorak_godsword",
            "item.zamorak_godsword_or",
        )

    /**
     * Per-npc-id strategy overrides, registered by monster plugins through [setNpcCombatStrategy].
     *
     * This is the only place a monster can be given an attack the three ordinary strategies cannot
     * execute *without* losing the engine's combat loop. `onNpcCombat` looks like the hook for it
     * and is not: it replaces [org.alter.plugins.content.combat.CombatPlugin]'s loop wholesale, and
     * that loop is the only thing in the game that walks an npc towards its target -
     * [Combat.moveToAttackRange], despite the name, only *tests* range (its walk call is commented
     * out). That is invisible for the casters that use `onNpcCombat` today, which stand off at ten
     * tiles and never need to close, and fatal for anything that fights at melee range: it would
     * stand on its spawn tile swinging at nothing.
     *
     * A strategy registered here keeps all of it - routing, leashing, line of sight, retaliation,
     * attack speed - and replaces only the swing itself.
     */
    private val npcStrategies = HashMap<Int, CombatStrategy>()

    /**
     * Registers [strategy] as the attack for every npc with [npcId]. Called from a monster plugin's
     * constructor, so it is in place before any npc of that id can be spawned.
     */
    fun setNpcCombatStrategy(
        npcId: Int,
        strategy: CombatStrategy,
    ) {
        npcStrategies[npcId] = strategy
    }

    fun getCombatStrategy(pawn: Pawn): CombatStrategy =
        /*
         * A salamander's combat class is real and is reported honestly by
         * [getCombatClass] - prayers, defence rolls and experience all key off it - but
         * none of the three ordinary strategies can execute its attack. See
         * [SalamanderCombatStrategy].
         */
        if (SalamanderCombatStrategy.applies(pawn)) {
            SalamanderCombatStrategy
        } else if (pawn is Npc && npcStrategies.containsKey(pawn.id)) {
            npcStrategies.getValue(pawn.id)
        } else {
            when (getCombatClass(pawn)) {
                CombatClass.MELEE -> MeleeCombatStrategy
                CombatClass.MAGIC -> MagicCombatStrategy
                CombatClass.RANGED -> RangedCombatStrategy
                else -> throw IllegalStateException("Invalid combat class: ${getCombatClass(pawn)} for $pawn")
            }
        }

    fun getCombatClass(pawn: Pawn): CombatClass {
        if (pawn is Npc) {
            return pawn.combatClass
        }

        if (pawn is Player) {
            if (pawn.attr.has(Combat.CASTING_SPELL)) {
                return CombatClass.MAGIC
            }
            /*
             * Follows the selected button rather than the weapon, which matters for the
             * weapons whose class is not fixed: a salamander's Scorch is melee, Flare is
             * ranged and Blaze is magic, and a powered staff attacks with magic from what is
             * otherwise a melee-looking staff.
             */
            return when (selectedStyle(pawn)?.combatStyle) {
                CombatStyle.RANGED -> CombatClass.RANGED
                CombatStyle.MAGIC -> CombatClass.MAGIC
                else -> CombatClass.MELEE
            }
        }

        throw IllegalArgumentException("Invalid pawn type.")
    }

    fun getAttackDelay(pawn: Pawn): Int {
        if (pawn is Npc) {
            return pawn.combatDef.attackSpeed
        }

        if (pawn is Player) {
            val default = PLAYER_DEFAULT_ATTACK_SPEED
            val weapon = pawn.getEquipment(EquipmentType.WEAPON) ?: return default
            var delay = weapon.getDef().attackSpeed
            /*
             * Rapid fires one tick faster than the weapon's base speed - the cache's
             * attackSpeed is the Accurate/Longrange figure. This was missing entirely,
             * so Rapid was purely cosmetic and every ranged style attacked at the same
             * rate. Rapid only exists on ranged weapons (see getAttackStyle), so this
             * cannot affect melee or magic.
             */
            if (getAttackStyle(pawn) == AttackStyle.RAPID) {
                delay -= 1
            }
            /*
             * Salamanders are speed 5 in the cache, which is Scorch's and Blaze's rate;
             * Flare is a tick faster at 4. Flare is not the Rapid style, so the branch
             * above does not cover it.
             */
            if (pawn.hasWeaponType(WeaponType.SALAMANDER) && pawn.getAttackStyle() == SALAMANDER_FLARE) {
                delay -= 1
            }
            return Math.max(MIN_ATTACK_SPEED, delay)
        }

        throw IllegalArgumentException("Invalid pawn type.")
    }

    fun getCombatDef(pawn: Pawn): NpcCombatDef? {
        if (pawn is Npc) {
            return pawn.combatDef
        }
        return null
    }

    fun getAttackAnimation(pawn: Pawn): Int {
        if (pawn is Npc) {
            return pawn.combatDef.attackAnimation
        }

        if (pawn is Player) {
            val style = pawn.getAttackStyle()

            return when {
                pawn.hasEquipped(EquipmentType.WEAPON, *GODSWORDS) -> 7045
                pawn.hasWeaponType(WeaponType.AXE) -> if (style == 1) 401 else 395
                pawn.hasWeaponType(WeaponType.HAMMER) -> 401
                pawn.hasWeaponType(WeaponType.BULWARK) -> 7511
                pawn.hasWeaponType(WeaponType.SCYTHE) -> 8056
                pawn.hasWeaponType(WeaponType.BOW) -> 426
                pawn.hasWeaponType(WeaponType.CROSSBOW) -> 4230
                pawn.hasWeaponType(WeaponType.LONG_SWORD) -> if (style == 2) 386 else 390
                pawn.hasWeaponType(WeaponType.TWO_HANDED) -> if (style == 2) 406 else 407
                /*
                 * 400 is Animation.HUMAN_BLUNT_STAB and 401 HUMAN_BLUNT_SWING, so a
                 * pickaxe's styles run the opposite way round to a mace's: style 2 is its
                 * Smash (crush) and every other style is a thrust - Spike, Impale, Block.
                 * The mace line below shares the expression because style 2 is the one
                 * *stab* option on a mace; copying it here had the pickaxe thrusting on
                 * Smash and swinging on Spike.
                 */
                pawn.hasWeaponType(WeaponType.PICKAXE) -> if (style == 2) 401 else 400
                pawn.hasWeaponType(WeaponType.DAGGER) -> if (style == 2) 390 else 386
                pawn.hasWeaponType(WeaponType.MAGIC_STAFF) || pawn.hasWeaponType(WeaponType.STAFF) -> 419
                pawn.hasWeaponType(WeaponType.MACE) -> if (style == 2) 400 else 401
                pawn.hasWeaponType(WeaponType.CHINCHOMPA) -> 7618
                pawn.hasWeaponType(WeaponType.THROWN) -> if (pawn.hasEquipped(EquipmentType.WEAPON, "item.toktzxilul")) 7558 else 929
                pawn.hasWeaponType(WeaponType.WHIP) -> 1658
                pawn.hasWeaponType(WeaponType.SPEAR) || pawn.hasWeaponType(WeaponType.HALBERD) ->
                    if (style == 1) {
                        440
                    } else if (style == 2) {
                        429
                    } else {
                        428
                    }
                pawn.hasWeaponType(WeaponType.CLAWS) -> 393
                else -> if (style == 1) 423 else 422
            }
        }

        throw IllegalArgumentException("Invalid pawn type.")
    }

    /**
     * The weapon "clang"/swing sound heard by anyone near a player's melee or ranged
     * attack, picked out of [WeaponSounds] by the weapon in hand and the attack type the
     * selected style deals - so the sound and the attack animation are driven off the same
     * style and a slash no longer plays a lunge.
     */
    fun getWeaponAttackSound(pawn: Player): Int = getWeaponSounds(pawn).forAttackType(getCombatStyle(pawn))

    /**
     * Which set of clips the equipped weapon draws from. Unarmed is the one case the
     * attack *type* can't separate - Punch and Kick are both crush - so it reads the raw
     * style index instead.
     */
    private fun getWeaponSounds(pawn: Player): WeaponSounds.Weapon =
        when {
            pawn.hasWeaponType(WeaponType.NONE) ->
                if (pawn.getAttackStyle() == 1) WeaponSounds.Weapon.KICK else WeaponSounds.Weapon.UNARMED
            pawn.hasEquipped(EquipmentType.WEAPON, *GODSWORDS) -> WeaponSounds.Weapon.GODSWORD
            pawn.hasWeaponType(WeaponType.LONG_SWORD) -> WeaponSounds.Weapon.SWORD
            pawn.hasWeaponType(WeaponType.DAGGER) -> WeaponSounds.Weapon.DAGGER
            pawn.hasWeaponType(WeaponType.CLAWS) -> WeaponSounds.Weapon.CLAWS
            pawn.hasWeaponType(WeaponType.TWO_HANDED) -> WeaponSounds.Weapon.TWO_HANDED
            pawn.hasWeaponType(WeaponType.AXE) -> WeaponSounds.Weapon.AXE
            pawn.hasWeaponType(WeaponType.PICKAXE) -> WeaponSounds.Weapon.PICKAXE
            pawn.hasWeaponType(WeaponType.HAMMER, WeaponType.BLUDGEON) -> WeaponSounds.Weapon.HAMMER
            pawn.hasWeaponType(WeaponType.HALBERD, WeaponType.SPEAR, WeaponType.STAFF_HALBERD) -> WeaponSounds.Weapon.POLEARM
            pawn.hasWeaponType(WeaponType.SCYTHE) -> WeaponSounds.Weapon.SCYTHE
            pawn.hasWeaponType(WeaponType.MACE) -> WeaponSounds.Weapon.MACE
            pawn.hasWeaponType(WeaponType.STAFF, WeaponType.MAGIC_STAFF, WeaponType.POWERED_STAFF) -> WeaponSounds.Weapon.STAFF
            pawn.hasWeaponType(WeaponType.WHIP) -> WeaponSounds.Weapon.WHIP
            pawn.hasWeaponType(WeaponType.BOW) -> WeaponSounds.Weapon.BOW
            pawn.hasWeaponType(WeaponType.CROSSBOW) -> WeaponSounds.Weapon.CROSSBOW
            pawn.hasWeaponType(WeaponType.THROWN, WeaponType.CHINCHOMPA) -> WeaponSounds.Weapon.THROWN
            else -> WeaponSounds.Weapon.GENERIC
        }

    fun getBlockAnimation(pawn: Pawn): Int {
        if (pawn is Npc) {
            return pawn.combatDef.blockAnimation
        }

        if (pawn is Player) {
            return when {
                pawn.hasEquipped(EquipmentType.SHIELD, *BOOKS) -> 420
                pawn.hasEquipped(EquipmentType.WEAPON, "item.sled_4084") -> 1466
                pawn.hasEquipped(EquipmentType.WEAPON, "item.easter_basket") -> 1834
                pawn.hasEquipped(EquipmentType.SHIELD, *DEFENDERS) -> 4177
                pawn.getEquipment(EquipmentType.SHIELD) != null -> 1156 // If wearing any shield, this animation is used

                pawn.hasEquipped(EquipmentType.WEAPON, *BOXING_GLOVES) -> 3679
                pawn.hasEquipped(EquipmentType.WEAPON, *GODSWORDS) -> 7056
                pawn.hasEquipped(EquipmentType.WEAPON, "item.light_ballista", "item.heavy_ballista") -> 7219
                pawn.hasEquipped(EquipmentType.WEAPON, "item.zamorakian_spear") -> 1709

                pawn.hasWeaponType(WeaponType.DAGGER) -> 378
                pawn.hasWeaponType(WeaponType.LONG_SWORD) -> 388
                pawn.hasWeaponType(WeaponType.PICKAXE, WeaponType.CLAWS) -> 397
                pawn.hasWeaponType(WeaponType.MACE) -> 403
                pawn.hasWeaponType(WeaponType.TWO_HANDED) -> 410
                pawn.hasWeaponType(WeaponType.MAGIC_STAFF) -> 420
                pawn.hasWeaponType(WeaponType.BOW) -> 424
                pawn.hasWeaponType(WeaponType.SPEAR, WeaponType.HALBERD) -> 430
                pawn.hasWeaponType(WeaponType.WHIP) -> 1659
                pawn.hasWeaponType(WeaponType.BULWARK) -> 7512
                else -> 424
            }
        }

        throw IllegalArgumentException("Invalid pawn type.")
    }

    fun getAttackStyle(pawn: Pawn): AttackStyle {
        if (pawn.entityType.isNpc) {
            return (pawn as Npc).attackStyle
        }

        if (pawn is Player) {
            return selectedStyle(pawn)?.attackStyle ?: AttackStyle.NONE
        }

        throw IllegalArgumentException("Invalid pawn type.")
    }

    fun getCombatStyle(pawn: Pawn): CombatStyle {
        if (pawn.entityType.isNpc) {
            return (pawn as Npc).combatStyle
        }

        if (pawn is Player) {
            if (pawn.attr.has(Combat.CASTING_SPELL)) {
                return CombatStyle.MAGIC
            }
            return selectedStyle(pawn)?.combatStyle ?: CombatStyle.NONE
        }

        throw IllegalArgumentException("Invalid pawn type.")
    }

    fun getXpMode(player: Player): XpMode = selectedStyle(player)?.xpMode ?: XpMode.ATTACK

    /**
     * The button the player currently has selected on the Combat Options tab, resolved
     * through [WeaponStyles] from the equipped weapon's type and the raw style index.
     *
     * Falls back to the panel's first button when the selected index is not one this weapon
     * has, which happens for real when a player switches from a four-button weapon to a
     * three-button one without touching the tab - the style index is not reset by the switch
     * itself. `null` only for a weapon type with no panel at all.
     */
    private fun selectedStyle(player: Player): WeaponStyles.Style? {
        val style = WeaponStyles.getOrFirst(player.getWeaponType(), player.getAttackStyle()) ?: return null
        if (style.combatStyle != CombatStyle.MAGIC || player.attr.has(Combat.CASTING_SPELL)) {
            return style
        }
        /*
         * A magic style with no spell behind it. Powered staves and Nature's reprisal attack
         * with a spell that is built into the weapon rather than selected, and this server
         * has no such spell: nothing ever writes [Combat.CASTING_SPELL] for them, and
         * [MagicCombatStrategy] dereferences that attribute unconditionally, so reporting
         * MAGIC here would hand it a null spell and raise a NullPointerException on the
         * first attack.
         *
         * Until the built-in spells exist, these weapons keep the melee behaviour they have
         * always had rather than crashing. The weapon type itself stays correct, so the
         * Combat Options tab, [WeaponSounds] and MagicCombatFormula's powered-staff branch
         * all still see a powered staff.
         */
        return SPELL_LESS_MAGIC_FALLBACK
    }

    /**
     * See [selectedStyle]. Crush with no style bonus, which is what a powered staff resolved
     * to before it had a weapon type of its own.
     */
    private val SPELL_LESS_MAGIC_FALLBACK =
        WeaponStyles.Style("Bash", CombatStyle.CRUSH, AttackStyle.NONE, XpMode.ATTACK)
}
