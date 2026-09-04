package org.alter.plugins.content.skills.slayer

import org.alter.api.EquipmentType
import org.alter.api.ext.hasEquipped
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player

/**
 * The black mask and slayer helmet damage bonus, and the one condition it has always been missing:
 * that the thing you are hitting is actually your assignment.
 *
 * Before this, all three combat formulas applied the black mask multiplier to every target, and all
 * three said so in a comment - "This should only apply if you have the target || his category as a
 * Slayer Task", "TODO: this should only apply when target is slayer task?". The check was impossible
 * to write because nothing tracked a task. It is possible now, so it lives here, once, instead of
 * being pasted into three formulas that had each drifted into a slightly different version of it.
 *
 * ## The bonuses
 *
 * - **Melee and ranged: 7/6 (16.67%)** to both accuracy and max hit, from a black mask or a slayer
 *   helmet, imbued or not.
 * - **Magic: 15%**, and *only* from the imbued versions. A plain black mask or slayer helmet gives
 *   no magic bonus at all. The magic formula previously granted the plain mask 7/6 on the accuracy
 *   roll, which was wrong twice over - wrong item, wrong figure.
 *
 * The bonus never applies against another player: it is a Slayer bonus and there is no such thing as
 * a player being your Slayer task.
 */
object SlayerHeadgear {
    private val BLACK_MASKS =
        arrayOf(
            "item.black_mask",
            "item.black_mask_1", "item.black_mask_2", "item.black_mask_3", "item.black_mask_4",
            "item.black_mask_5", "item.black_mask_6", "item.black_mask_7", "item.black_mask_8",
            "item.black_mask_9", "item.black_mask_10",
        )

    private val BLACK_MASKS_I =
        arrayOf(
            "item.black_mask_i",
            "item.black_mask_1_i", "item.black_mask_2_i", "item.black_mask_3_i", "item.black_mask_4_i",
            "item.black_mask_5_i", "item.black_mask_6_i", "item.black_mask_7_i", "item.black_mask_8_i",
            "item.black_mask_9_i", "item.black_mask_10_i",
        )

    private val SLAYER_HELMS =
        arrayOf(
            "item.slayer_helmet",
            "item.black_slayer_helmet",
            "item.green_slayer_helmet",
            "item.red_slayer_helmet",
            "item.purple_slayer_helmet",
            "item.turquoise_slayer_helmet",
            "item.hydra_slayer_helmet",
            "item.twisted_slayer_helmet",
            "item.tztok_slayer_helmet",
            "item.vampyric_slayer_helmet",
        )

    private val SLAYER_HELMS_I =
        arrayOf(
            "item.slayer_helmet_i",
            "item.black_slayer_helmet_i",
            "item.green_slayer_helmet_i",
            "item.red_slayer_helmet_i",
            "item.purple_slayer_helmet_i",
            "item.turquoise_slayer_helmet_i",
            "item.hydra_slayer_helmet_i",
            "item.twisted_slayer_helmet_i",
            "item.tztok_slayer_helmet_i",
            "item.vampyric_slayer_helmet_i",
        )

    /** 7/6 with any black mask or slayer helmet, while [target] is the player's assignment. */
    fun meleeRangedMultiplier(
        player: Player,
        target: Pawn?,
    ): Double =
        if (onTaskWearing(player, target, BLACK_MASKS, BLACK_MASKS_I, SLAYER_HELMS, SLAYER_HELMS_I)) {
            7.0 / 6.0
        } else {
            1.0
        }

    /** 1.15 with an *imbued* black mask or slayer helmet only, while [target] is the assignment. */
    fun magicMultiplier(
        player: Player,
        target: Pawn?,
    ): Double =
        if (onTaskWearing(player, target, BLACK_MASKS_I, SLAYER_HELMS_I)) {
            1.15
        } else {
            1.0
        }

    private fun onTaskWearing(
        player: Player,
        target: Pawn?,
        vararg sets: Array<String>,
    ): Boolean {
        if (target !is Npc || !Slayer.isOnTask(player, target)) {
            return false
        }
        return sets.any { player.hasEquipped(EquipmentType.HEAD, *it) }
    }
}
