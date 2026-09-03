package org.alter.plugins.content.areas.duelarena

import org.alter.api.EquipmentType

/**
 * The duel rules, exactly as this cache's client builds them.
 *
 * Every field here is transcribed from `[clientscript,6169]`, which calls `~script6170(bit, title,
 * component, description)` once per row - so the bit numbers are not a convention we picked, they
 * are what `testbit(%var286, n)` in `[proc,6175]` will look at when it decides which radio button
 * to light. Bits 10 and 11 are unused by the client and are left out here rather than invented.
 *
 * Note this differs from the wiki's list in two ways: there is no "Obstacles" rule (it went when
 * the arena was rebuilt), and there are two rules the wiki page does not mention, [NO_WEAPON_SWITCH]
 * and [SHOW_INVENTORIES].
 */
enum class DuelRule(
    val bit: Int,
    val component: Int,
    val title: String,
    val description: String,
) {
    NO_FORFEIT(0, 35, "No Forfeit", "Neither player is allowed to forfeit the duel."),
    NO_MOVEMENT(
        1,
        39,
        "No Movement",
        "Players stand next to each other and aren't allowed to move or use hold spells.",
    ),
    NO_WEAPON_SWITCH(2, 40, "No Weapon Switch", "Neither player is allowed to swap their weapons during the duel."),
    SHOW_INVENTORIES(
        3,
        41,
        "Show Inventories",
        "Show your opponent your worn and back pack inventory, and view theirs.",
    ),
    NO_RANGED(4, 30, "No Ranged", "Neither player is allowed to use ranged attacks."),
    NO_MELEE(5, 31, "No Melee", "Neither player is allowed to use melee attacks."),
    NO_MAGIC(6, 32, "No Magic", "Neither player is allowed to use magic attacks."),
    NO_DRINKS(7, 37, "No Drinks", "Neither player is allowed to use drinks."),
    NO_FOOD(8, 38, "No Food", "Neither player is allowed to use food."),
    NO_PRAYER(9, 36, "No Prayer", "Neither player is allowed to use prayer."),
    FUN_WEAPONS(
        12,
        34,
        "Fun Weapons",
        "Both players must use a 'fun weapon', such as flowers or a rubber chicken.",
    ),
    NO_SPECIAL_ATTACKS(13, 33, "No Special Attacks", "Neither player is allowed to use special attacks."),
    ;

    companion object {
        val values = enumValues<DuelRule>()

        /**
         * The order `[clientscript,6169]` builds the rows in. Each row targets a fixed component so
         * the order does not change the layout, but keeping it makes the two easy to compare.
         */
        val BUILD_ORDER =
            listOf(
                NO_RANGED, NO_MELEE, NO_MAGIC, NO_SPECIAL_ATTACKS, FUN_WEAPONS, NO_FORFEIT,
                NO_PRAYER, NO_DRINKS, NO_FOOD, NO_MOVEMENT, NO_WEAPON_SWITCH, SHOW_INVENTORIES,
            )

        private val byComponent = values.associateBy { it.component }

        fun byComponent(component: Int): DuelRule? = byComponent[component]

        /**
         * The three rules that cannot both be set, because between them they would leave neither
         * player any way to land a hit.
         */
        val ATTACK_STYLE_RULES = listOf(NO_MELEE, NO_RANGED, NO_MAGIC)

        /**
         * "Whip" preset - `[clientscript,6169]`'s row 94 loads these. It does not enforce a whip,
         * only the rules a whip duel is normally fought under.
         */
        val WHIP_PRESET = listOf(NO_RANGED, NO_MAGIC, NO_FORFEIT, NO_DRINKS, NO_FOOD, NO_PRAYER, NO_SPECIAL_ATTACKS)

        /** "Boxing" preset - as above, plus an empty weapon hand. */
        val BOXING_PRESET = listOf(NO_RANGED, NO_MAGIC, NO_FORFEIT, NO_DRINKS, NO_FOOD, NO_PRAYER, NO_SPECIAL_ATTACKS)
    }
}

/**
 * The eleven worn slots a duel can lock, in the order `[clientscript,duel_initworn]` lays them out.
 *
 * The slot numbers are equipment slots, not row positions - the script maps its row index to
 * `inv_getobj(worn, n)` with the gaps this list keeps (there is no slot 6, 8 or 11 among them).
 * [tooltip] is the string enum 4388 holds for the same slot, kept here so the server's refusal
 * message and the client's tooltip say the same thing.
 */
enum class DuelSlot(
    val slot: Int,
    val label: String,
    val tooltip: String,
) {
    HEAD(EquipmentType.HEAD.id, "helm", "Neither player will be allowed to wear items on their head."),
    CAPE(EquipmentType.CAPE.id, "cape", "Neither player will be allowed to wear items on their back."),
    AMULET(EquipmentType.AMULET.id, "amulet", "Neither player will be allowed to wear items around their neck."),
    WEAPON(EquipmentType.WEAPON.id, "weapon", "Neither player will be allowed to hold items in their right hand, including 2-handed items."),
    BODY(EquipmentType.CHEST.id, "body", "Neither player will be allowed to wear items on their torso."),
    SHIELD(EquipmentType.SHIELD.id, "shield", "Neither player will be allowed to hold items in their left hand, nor use 2-handed items."),
    LEGS(EquipmentType.LEGS.id, "legs", "Neither player will be allowed to wear items on their legs."),
    HANDS(EquipmentType.GLOVES.id, "gloves", "Neither player will be allowed to wear items on their hands."),
    FEET(EquipmentType.BOOTS.id, "boots", "Neither player will be allowed to wear items on their feet."),
    RING(EquipmentType.RING.id, "ring", "Neither player will be allowed to wear items on their fingers."),
    AMMO(EquipmentType.AMMO.id, "ammo", "Neither player will be allowed to have items in their quiver."),
    ;

    /**
     * Where this slot's "changed" flash sits in the flag word the confirm screen reads.
     *
     * `[clientscript,6193]` tests `getbit_range($flags, 14, 27)` for "Some worn items will be taken
     * off", so the worn slots share the rules' word from bit 14 up. The offset is the *equipment
     * slot*, not this enum's position: enum 4210 maps 17 to the weapon icon and 27 to the ammo one,
     * which only lines up if the bit is `14 + slot`. Getting that backwards would flash the wrong
     * row and mis-describe the duel on the confirm screen.
     */
    val flagBit: Int get() = 14 + slot

    companion object {
        val values = enumValues<DuelSlot>()

        private val bySlot = values.associateBy { it.slot }

        fun bySlot(slot: Int): DuelSlot? = bySlot[slot]

        /**
         * The worn-slot icons in the order `[clientscript,duel_initworn]` creates them into
         * component 755:42, which is the order their sub-component ids come back to the server in.
         * Note ammo sits fourth, not last, the way the screen lays the quiver out under the amulet.
         */
        val ICON_ORDER = listOf(HEAD, CAPE, AMULET, AMMO, WEAPON, BODY, SHIELD, LEGS, HANDS, FEET, RING)

        /** The [DuelSlot] behind sub-component [index] of 755:42, or null if there isn't one. */
        fun byIconIndex(index: Int): DuelSlot? = ICON_ORDER.getOrNull(index)
    }
}
