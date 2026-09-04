package org.alter.plugins.content.combat.strategy.ranged

import org.alter.api.EquipmentType
import org.alter.api.WeaponType
import org.alter.api.ext.getEquipment
import org.alter.api.ext.hasEquipped
import org.alter.api.ext.hasWeaponType
import org.alter.game.model.Graphic
import org.alter.game.model.combat.PawnHit
import org.alter.game.model.entity.GroundItem
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.plugins.content.combat.createProjectile
import org.alter.plugins.content.items.blowpipe.Blowpipe

/**
 * Firing the equipped ammo: the projectile that flies, the drawback and impact
 * graphics, and whether the ammo survives the shot.
 *
 * Split out of [org.alter.plugins.content.combat.strategy.RangedCombatStrategy] so that
 * ranged special attacks can fire ammo the same way the normal attack does - several of
 * them (magic shortbow, dark bow, dragon knife) fire more than one projectile per
 * special and consume ammo per projectile.
 */
object RangedAmmo {
    /**
     * The equipment slot the ammo actually comes from. Thrown weapons and chinchompas
     * *are* the ammo, so they consume from the weapon slot; everything else draws from
     * the quiver.
     */
    fun ammoSlot(player: Player): EquipmentType =
        when {
            player.hasWeaponType(WeaponType.THROWN) || player.hasWeaponType(WeaponType.CHINCHOMPA) -> EquipmentType.WEAPON
            else -> EquipmentType.AMMO
        }

    /**
     * Whether there is still ammo to fire.
     *
     * Multi-shot specials check this between shots: firing two arrows off one arrow
     * would otherwise deal both hits, and for thrown weapons - where the weapon *is*
     * the ammo - throwing the last knife empties the weapon slot, which would change
     * what [ammoSlot] resolves to for the second throw.
     */
    fun hasAmmo(player: Player): Boolean {
        // A blowpipe holds its darts internally, so the quiver is irrelevant to it.
        Blowpipe.equipped(player)?.let { return Blowpipe.canFire(it) }
        return player.getEquipment(ammoSlot(player)) != null
    }

    /**
     * Draws the equipped ammo's projectile at [target] - the flying spotanim plus its
     * drawback and impact graphics - without consuming anything.
     *
     * Separate from [fire] for area specials, which spend one piece of ammo but draw a
     * projectile at every target they catch.
     */
    fun drawProjectile(
        player: Player,
        target: Pawn,
    ) {
        val ammo = player.getEquipment(ammoSlot(player)) ?: return
        val ammoProjectile = RangedProjectile.values.firstOrNull { ammo.id in it.items } ?: return

        val projectile = player.createProjectile(target, ammoProjectile.gfx, ammoProjectile.type)
        ammoProjectile.drawback?.let { drawback -> player.graphic(drawback) }
        ammoProjectile.impact?.let { impact -> target.graphic(impact.id, impact.height, projectile.impactDelay) }
        player.world.spawn(projectile)
    }

    /**
     * The equipped ammo's impact spotanim, or null if it has none.
     *
     * For a chinchompa's splash targets: one chinchompa flies to the primary target and
     * detonates, and everything caught in the blast sees the explosion rather than a
     * chinchompa of its own arriving. Returned rather than drawn so the caller can read
     * it *before* [fire] consumes the ammo - on the last chinchompa the weapon slot is
     * empty by the time the blast is drawn, and an empty weapon slot also changes what
     * [ammoSlot] resolves to.
     */
    fun impactGraphic(player: Player): Graphic? {
        val ammo = player.getEquipment(ammoSlot(player)) ?: return null
        return RangedProjectile.values.firstOrNull { ammo.id in it.items }?.impact
    }

    /**
     * Spawns the projectile for the equipped ammo and consumes it.
     *
     * Returns the action to run when the resulting hit lands - dropping the spent ammo
     * at the target's feet, where applicable. The caller passes it to
     * [org.alter.plugins.content.combat.dealHit]'s `onHit`.
     *
     * [spawnProjectile] can be turned off by specials that draw their own projectile
     * instead of the ammo's - the dark bow's special fires shadow arrows, the Armadyl
     * crossbow's fires a distinct bolt - while still consuming ammo normally.
     */
    fun fire(
        player: Player,
        target: Pawn,
        spawnProjectile: Boolean = true,
    ): (PawnHit).() -> Unit {
        val world = player.world
        // Blowpipes keep their darts and scales inside the weapon - see Blowpipe.
        Blowpipe.equipped(player)?.let { return fireBlowpipe(player, target, it, spawnProjectile) }

        val ammo = player.getEquipment(ammoSlot(player)) ?: return {}

        val ammoProjectile = RangedProjectile.values.firstOrNull { ammo.id in it.items }
        if (spawnProjectile) {
            drawProjectile(player, target)
        }

        /*
         * Chinchompas detonate on impact - there is nothing left to recover or drop.
         */
        if (ammoProjectile != null && ammoProjectile.breakOnImpact()) {
            player.equipment.remove(ammo.id, 1)
            return {}
        }

        /*
         * Ammo has three possible fates, and the wiki gives the split per Ava's device:
         *
         *   device        recovered   break on impact   drop onto floor
         *   none               0%           20%               80%
         *   attractor         60%           20%               20%
         *   accumulator       72%           20%                8%
         *   assembler         80%           20%                0%
         *
         * Two things worth not "correcting" again. Break-on-impact is a flat 20% in
         * every case *including with no device at all* - it is not an artefact of the
         * old code, which had these three rates right. And the assembler is an 80%
         * save, not a perfect one; only its drop share is zero, which is what makes it
         * strictly better than the accumulator rather than free ammo.
         *
         * What the old code genuinely got wrong was having no attractor branch, so an
         * attractor fell through to the no-device case and saved nothing.
         *
         * One roll decides between the three, so they stay mutually exclusive.
         */
        val rates = ammoRates(player)
        val roll = world.randomDouble()

        if (roll < rates.recovered) {
            // Never leaves the quiver.
            return {}
        }

        player.equipment.remove(ammo.id, 1)

        if (roll < rates.recovered + rates.broken) {
            // Shattered on impact - nothing to pick up.
            return {}
        }

        return { world.spawn(GroundItem(ammo.id, 1, target.tile, player)) }
    }

    /** Recovered/broken split for the equipped Ava's device; the remainder drops. */
    private data class AmmoRates(val recovered: Double, val broken: Double)

    private fun ammoRates(player: Player): AmmoRates =
        when {
            player.hasEquipped(EquipmentType.CAPE, "item.avas_assembler", "item.avas_assembler_l") ->
                AmmoRates(recovered = 0.80, broken = 0.20)
            player.hasEquipped(EquipmentType.CAPE, "item.avas_accumulator", "item.avas_accumulator_23609") ->
                AmmoRates(recovered = 0.72, broken = 0.20)
            player.hasEquipped(EquipmentType.CAPE, "item.avas_attractor", "item.avas_device") ->
                AmmoRates(recovered = 0.60, broken = 0.20)
            else ->
                AmmoRates(recovered = 0.0, broken = 0.20)
        }

    /**
     * Firing a toxic blowpipe.
     *
     * Differs from ordinary ammo in two ways. The dart comes out of the *weapon* rather
     * than the quiver, so "recovered" means the dart simply never leaves the blowpipe;
     * and every shot also rolls a Zulrah's scale, which is spent on roughly two shots in
     * three. Ava's devices apply to the dart exactly as they do to arrows.
     *
     * The caller is expected to have checked [hasAmmo] first - a blowpipe with no darts
     * or no scales cannot fire at all.
     */
    private fun fireBlowpipe(
        player: Player,
        target: Pawn,
        blowpipe: Item,
        spawnProjectile: Boolean,
    ): (PawnHit).() -> Unit {
        val world = player.world
        val dartId = Blowpipe.dartId(blowpipe)
        if (dartId == -1 || Blowpipe.dartCount(blowpipe) <= 0 || Blowpipe.scaleCount(blowpipe) <= 0) {
            return {}
        }

        if (spawnProjectile) {
            RangedProjectile.values.firstOrNull { dartId in it.items }?.let { dart ->
                val projectile = player.createProjectile(target, dart.gfx, dart.type)
                dart.drawback?.let { drawback -> player.graphic(drawback) }
                dart.impact?.let { impact -> target.graphic(impact.id, impact.height, projectile.impactDelay) }
                world.spawn(projectile)
            }
        }

        Blowpipe.consumeScale(blowpipe, world)

        val rates = ammoRates(player)
        val roll = world.randomDouble()

        if (roll < rates.recovered) {
            // Stays in the blowpipe.
            return {}
        }

        Blowpipe.consumeDart(blowpipe)

        if (roll < rates.recovered + rates.broken) {
            return {}
        }

        return { world.spawn(GroundItem(dartId, 1, target.tile, player)) }
    }
}
