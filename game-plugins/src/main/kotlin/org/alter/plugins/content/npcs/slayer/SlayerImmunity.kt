package org.alter.plugins.content.npcs.slayer

import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import org.alter.api.EquipmentType
import org.alter.api.ext.hasEquipped
import org.alter.game.model.combat.CombatClass
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.plugins.content.combat.CombatConfigs
import org.alter.rscm.RSCM.getRSCM

/**
 * The turoth's and kurask's weapon requirement: they can only be hurt by leaf-bladed weaponry, broad
 * ammunition, or the Magic Dart spell.
 *
 * This is the one protective/immunity rule in the Slayer Tower and Fremennik Slayer Dungeon roster
 * that decides whether the monster can be killed at all rather than how comfortable the kill is,
 * which is why it is built while the earmuffs, nose peg, mirror shield, bag of salt and rock hammer
 * mechanics are not - those change the punishment for turning up unequipped, and their monsters are
 * still perfectly killable without them. A turoth without a leaf-bladed weapon is not.
 *
 * ## Where it is checked, and what that costs
 *
 * `Combat.canEngage` - beside the Slayer level check it already makes - rather than per hit. That is
 * a slightly coarser reading than the real game's: OSRS lets you swing at a kurask with a scimitar
 * and simply never damages it, where this refuses the attack outright with a message. Refusing is
 * the better failure here, because "your hits all splash for no stated reason" is the kind of thing
 * a player reads as a bug.
 *
 * Checking at engage time also means checking against the *style the player is about to attack
 * with*, which is what makes the check precise: broad bolts in the ammo slot do not license a
 * melee swing, and a leaf-bladed sword on the back does not license a bowshot.
 *
 * **Magic Dart is not honoured, because it does not exist here.** `CombatSpell` carries no
 * `MAGIC_DART` entry, so there is no way to be casting it; magic is therefore refused outright
 * rather than silently allowed. The day the spell is added, it belongs in [MAGIC_ALLOWED].
 */
internal object SlayerImmunity {
    /** Melee weapons that can hurt a turoth or kurask. */
    private val LEAF_BLADED =
        arrayOf(
            "item.leafbladed_spear",
            "item.leafbladed_sword",
            "item.leafbladed_battleaxe",
        )

    /** Ammunition that can hurt a turoth or kurask. */
    private val BROAD_AMMO =
        arrayOf(
            "item.broad_arrows",
            "item.broad_bolts",
            "item.amethyst_broad_bolts",
        )

    /**
     * No spell here can hurt them yet - Magic Dart is the only one that may, and it is not built.
     * Left as an empty gate rather than a hard `false` so adding the spell is a one-line change.
     */
    private val MAGIC_ALLOWED = emptyList<String>()

    /**
     * The npc ids this rule applies to, resolved once from the roster rather than listed again -
     * a turoth level added to [SlayerMonsters] is covered without touching this file.
     */
    private val IMMUNE: IntOpenHashSet by lazy {
        val ids = IntOpenHashSet()
        SlayerMonsters.ALL
            .filter { it.name.startsWith("Turoth") || it.name == "Kurask" }
            .forEach { monster -> monster.npcKeys.forEach { ids.add(getRSCM(it)) } }
        ids
    }

    fun isProtected(npc: Npc): Boolean = IMMUNE.contains(npc.id)

    /**
     * Whether [player] is currently equipped to hurt [npc]. Only meaningful for an npc
     * [isProtected] returns true for; everything else is damageable by anything.
     */
    fun canDamage(
        player: Player,
        npc: Npc,
    ): Boolean {
        if (!isProtected(npc)) {
            return true
        }
        return when (CombatConfigs.getCombatClass(player)) {
            CombatClass.MELEE -> player.hasEquipped(EquipmentType.WEAPON, *LEAF_BLADED)
            CombatClass.RANGED -> player.hasEquipped(EquipmentType.AMMO, *BROAD_AMMO)
            CombatClass.MAGIC -> MAGIC_ALLOWED.isNotEmpty()
        }
    }

    /** The line the real game gives, near enough: it names the two routes that work here. */
    const val MESSAGE = "Your weapon seems to have no effect on this creature. Try leaf-bladed weaponry or broad ammunition."
}
