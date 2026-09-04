package org.alter.plugins.content.areas.wilderness.bosses

import org.alter.api.EquipmentType
import org.alter.api.HitType
import org.alter.api.cfg.Graphic
import org.alter.api.Skills
import org.alter.api.ext.createProjectile
import org.alter.api.ext.getEquipment
import org.alter.api.ext.message
import org.alter.api.ext.hit
import org.alter.api.ext.npc
import org.alter.api.ext.prepareAttack
import org.alter.game.Server
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.combat.AttackStyle
import org.alter.game.model.combat.CombatClass
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.game.model.move.moveTo
import org.alter.game.model.queue.QueueTask
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.Combat
import org.alter.plugins.content.combat.canAttackMelee
import org.alter.plugins.content.combat.canEngageCombat
import org.alter.plugins.content.combat.dealHit
import org.alter.plugins.content.combat.playAttackSound
import org.alter.plugins.content.combat.formula.MagicCombatFormula
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.plugins.content.combat.getCombatTarget
import org.alter.plugins.content.combat.isAttackDelayReady
import org.alter.plugins.content.combat.moveToAttackRange
import org.alter.plugins.content.combat.postAttackLogic
import org.alter.plugins.content.combat.removeCombatTarget
import org.alter.plugins.content.combat.strategy.RangedCombatStrategy
import org.alter.rscm.RSCM.getRSCM

/**
 * The attacks that make each Wilderness boss its own fight.
 *
 * The engine's default combat loop only ever swings melee at whatever it is fighting - `Npc
 * .combatClass` defaults to MELEE and nothing but per-monster code changes it, the same limitation
 * `content/npcs/darkwizard` and `content/npcs/kbd` both document - so every one of these bosses
 * needs its own loop to do anything but punch. The shape is lifted from
 * [org.alter.plugins.content.npcs.kbd.KbdCombatPlugin]: engage, close to range, then pick an attack.
 *
 * Only the specials that *define* each fight are here. The ordinary swings use the stats
 * `data/cfg/npcs/monsterStats.json` already carries, so nothing in this file restates a stat.
 *
 * ## Where the animations come from
 *
 * Attack, block and death normally come from
 * [org.alter.plugins.content.npcs.animations.MonsterAnimationsPlugin]. That works for most of
 * these, but not for Vet'ion or Calvar'ion, so both are pinned by name in
 * `named-combat-media.json` instead - see that file's entries and the note below.
 *
 * ## What is not modelled, and why
 *
 * - **Vet'ion does not visually transform.** `Npc.id` is a `val` in this engine, so the enraged
 *   form's separate npc id (6612) cannot be swapped in, and despawning him to spawn it would throw
 *   away the damage map - and with it the killer, and therefore the loot. The phase itself *is*
 *   modelled: at half health he heals, speeds up, plays the enrage animation and summons two hounds.
 * - **The hounds do not grant damage immunity, and only the enraged phase summons them.** In the
 *   real game both phases summon a pair and the boss cannot be hurt while they live, which is what
 *   makes them worth killing. Immunity needs a damage hook that does not exist here yet.
 * - **The bosses' prayer-effectiveness reductions are not modelled.** Venenatis' spiderlings
 *   weakening protection prayers, and Callisto's melee being only halved rather than blocked,
 *   both need a per-attack prayer multiplier that the shared combat formulas have no hook for.
 * - **Callisto's bear traps and Venenatis' persistent web tiles are not modelled.** Both are
 *   ground effects that need a tile-effect system this codebase does not have; their damage is
 *   applied directly to the target instead.
 */
class WildernessBossCombatPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        bossCombat("npc.chaos_fanatic") { task, boss, target -> chaosFanatic(task, boss, target) }
        bossCombat("npc.crazy_archaeologist") { task, boss, target -> crazyArchaeologist(task, boss, target) }
        bossCombat("npc.chaos_elemental_2054") { task, boss, target -> chaosElemental(task, boss, target) }
        bossCombat("npc.scorpia") { task, boss, target -> scorpia(task, boss, target) }
        bossCombat("npc.callisto_6609") { task, boss, target -> callisto(task, boss, target) }
        bossCombat("npc.artio") { task, boss, target -> callisto(task, boss, target) }
        bossCombat("npc.venenatis_6610") { task, boss, target -> venenatis(task, boss, target) }
        bossCombat("npc.spindel") { task, boss, target -> venenatis(task, boss, target) }
        bossCombat("npc.vetion") { task, boss, target -> vetion(task, boss, target) }
        bossCombat("npc.calvarion") { task, boss, target -> vetion(task, boss, target) }
    }

    /**
     * Binds [attack] as the whole of [npc]'s combat: close to range, swing, repeat.
     *
     * [attack] is called once per attack, only when the boss is in range and off cooldown, and is
     * responsible for nothing but choosing and performing that one attack - the loop handles
     * facing, movement, the attack delay and disengaging.
     */
    private fun bossCombat(
        npc: String,
        attack: suspend (QueueTask, Npc, Pawn) -> Unit,
    ) {
        onNpcCombat(npc) {
            val boss = this.npc
            boss.queue {
                var target = boss.getCombatTarget() ?: return@queue

                while (boss.canEngageCombat(target)) {
                    boss.facePawn(target)
                    val range = Combat.npcAttackRange(boss, FALLBACK_RANGE)
                    if (boss.moveToAttackRange(this, target, distance = range, projectile = true) && boss.isAttackDelayReady()) {
                        attack(this, boss, target)
                        boss.postAttackLogic(target)
                    }
                    wait(1)
                    target = boss.getCombatTarget() ?: break
                }

                boss.resetFacePawn()
                boss.removeCombatTarget()
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // Chaos Fanatic - magic, with an explosion and a disarm.
    // -----------------------------------------------------------------------------------------

    private suspend fun chaosFanatic(
        task: QueueTask,
        boss: Npc,
        target: Pawn,
    ) {
        when {
            world.chance(1, EXPLOSION_RATE) -> {
                boss.forceChat("Burn!")
                boss.magicSplash(target, gfx = CHAOS_FANATIC_GFX, animation = CHAOS_FANATIC_ANIM, maxHit = 31)
                // "Three slow-moving projectiles which explode on impact", one of them always on
                // the tile the player is standing on - so the tile hit is guaranteed and the other
                // two are what moving avoids.
                repeat(2) {
                    if (world.chance(1, 2)) {
                        boss.dealHit(target = target, maxHit = 31, landHit = true, delay = 3)
                    }
                }
            }
            world.chance(1, DISARM_RATE) -> {
                boss.forceChat("Weakling!")
                boss.magicSplash(target, gfx = CHAOS_FANATIC_GFX, animation = CHAOS_FANATIC_ANIM, maxHit = 21)
                disarm(target)
            }
            else -> boss.magicSplash(target, gfx = CHAOS_FANATIC_GFX, animation = CHAOS_FANATIC_ANIM, maxHit = 21)
        }
    }

    /**
     * Moves the target's weapon into their inventory.
     *
     * The wiki notes this is "entirely negated by keeping inventory full", which falls out of the
     * implementation for free rather than needing a special case: with nowhere to put the weapon,
     * nothing is unequipped.
     */
    private fun disarm(target: Pawn) {
        val player = target as? Player ?: return
        val weapon = player.getEquipment(EquipmentType.WEAPON) ?: return
        if (!player.inventory.hasSpace) {
            return
        }
        player.equipment.remove(weapon.id, weapon.amount)
        player.inventory.add(weapon.id, weapon.amount, assureFullInsertion = false)
        player.message("Your weapon is knocked out of your hands!")
    }

    // -----------------------------------------------------------------------------------------
    // Crazy archaeologist - ranged, with the "Rain of Knowledge" barrage.
    // -----------------------------------------------------------------------------------------

    private suspend fun crazyArchaeologist(
        task: QueueTask,
        boss: Npc,
        target: Pawn,
    ) {
        if (world.chance(1, RAIN_OF_KNOWLEDGE_RATE)) {
            boss.forceChat("Rain of Knowledge!")
            boss.prepareAttack(CombatClass.RANGED, CombatStyle.RANGED, AttackStyle.ACCURATE)
            // He has exactly one action animation of his own, so the special reuses it and is
            // told apart by its projectile instead.
            boss.animate(ARCHAEOLOGIST_ATTACK_ANIM)
            world.spawn(
                boss.createProjectile(target, gfx = ARCHAEOLOGIST_SPECIAL_GFX, startHeight = 40, endHeight = 31, delay = 41, angle = 15),
            )
            // Three books in an arc, one of which splits in two - four landings in all, each its
            // own roll, which is what makes the special swingier than the basic attack rather than
            // simply harder-hitting.
            repeat(4) { index ->
                boss.dealHit(target = target, maxHit = 24, landHit = world.chance(1, 2), delay = 2 + index)
            }
            return
        }

        boss.prepareAttack(CombatClass.RANGED, CombatStyle.RANGED, AttackStyle.ACCURATE)
        boss.animate(ARCHAEOLOGIST_ATTACK_ANIM)
        world.spawn(boss.createProjectile(target, gfx = ARCHAEOLOGIST_GFX, startHeight = 40, endHeight = 31, delay = 41, angle = 15))
        boss.dealHit(
            target = target,
            maxHit = 14,
            landHit = MagicCombatFormula.getAccuracy(boss, target) >= world.randomDouble(),
            delay = RangedCombatStrategy.getHitDelay(boss.getFrontFacingTile(target), target.getCentreTile()),
        )
    }

    // -----------------------------------------------------------------------------------------
    // Chaos Elemental - Discord, Confusion and Madness.
    // -----------------------------------------------------------------------------------------

    private suspend fun chaosElemental(
        task: QueueTask,
        boss: Npc,
        target: Pawn,
    ) {
        when (world.random(5)) {
            0 -> {
                // Confusion: teleports the player somewhere nearby, away from it.
                boss.prepareAttack(CombatClass.MAGIC, CombatStyle.MAGIC, AttackStyle.ACCURATE)
                boss.animate(CHAOS_ELEMENTAL_SPECIAL_ANIM)
                displace(target)
            }
            1 -> {
                // Madness: unequips up to four items.
                boss.prepareAttack(CombatClass.MAGIC, CombatStyle.MAGIC, AttackStyle.ACCURATE)
                boss.animate(CHAOS_ELEMENTAL_SPECIAL_ANIM)
                unequipSome(target, count = 4)
            }
            // Discord: the same 28 whichever of the three styles it comes as, which is why the
            // style here only changes what the target's protection prayer has to be.
            2 -> boss.magicSplash(target, gfx = CHAOS_ELEMENTAL_GFX, animation = CHAOS_ELEMENTAL_ANIM, maxHit = 28)
            3 -> {
                boss.prepareAttack(CombatClass.RANGED, CombatStyle.RANGED, AttackStyle.ACCURATE)
                boss.animate(CHAOS_ELEMENTAL_ANIM)
                boss.dealHit(target = target, maxHit = 28, landHit = world.chance(2, 3), delay = 2)
            }
            else -> boss.meleeSwing(target, maxHit = 28)
        }
    }

    /** Walks the target a few tiles off, as Confusion does. */
    private fun displace(target: Pawn) {
        val player = target as? Player ?: return
        val dx = world.random(-CONFUSION_RANGE..CONFUSION_RANGE)
        val dz = world.random(-CONFUSION_RANGE..CONFUSION_RANGE)
        player.moveTo(Tile(player.tile.x + dx, player.tile.z + dz, player.tile.height))
        player.message("You feel disorientated.")
    }

    /** Takes up to [count] worn items off, Madness-style. Anything that will not fit stays on. */
    private fun unequipSome(
        target: Pawn,
        count: Int,
    ) {
        val player = target as? Player ?: return
        var removed = 0
        for (slot in 0 until player.equipment.capacity) {
            if (removed >= count) {
                break
            }
            val item = player.equipment[slot] ?: continue
            if (!player.inventory.hasSpace) {
                break
            }
            player.equipment.remove(item.id, item.amount)
            player.inventory.add(item.id, item.amount, assureFullInsertion = false)
            removed++
        }
        if (removed > 0) {
            player.message("The Chaos Elemental tears your equipment off!")
        }
    }

    // -----------------------------------------------------------------------------------------
    // Scorpia - melee that drains prayer, and guardians that heal her.
    // -----------------------------------------------------------------------------------------

    private suspend fun scorpia(
        task: QueueTask,
        boss: Npc,
        target: Pawn,
    ) {
        boss.meleeSwing(target, maxHit = 16)

        // "Whether successful or not", so the drain sits outside the hit rather than inside it.
        (target as? Player)?.let { player ->
            player.getSkills().alterCurrentLevel(Skills.PRAYER, -SCORPIA_PRAYER_DRAIN)
        }

        if (boss.getCurrentHp() * 2 <= boss.getMaxHp() && boss.attr[GUARDIANS_SUMMONED] != true) {
            boss.attr[GUARDIANS_SUMMONED] = true
            boss.forceChat("Guardians, to me!")
            repeat(2) { index ->
                val guardian = Npc(getRSCM("npc.scorpias_guardian"), boss.tile.transform(if (index == 0) -1 else 1, 1), world)
                guardian.respawns = false
                world.spawn(guardian)
            }
        }

        // The guardians heal her while they are near - the reason to kill them first.
        if (boss.attr[GUARDIANS_SUMMONED] == true) {
            val healers =
                world.npcs.count { other ->
                    other.id == getRSCM("npc.scorpias_guardian") &&
                        other.isActive() &&
                        other.tile.getDistance(boss.tile) <= GUARDIAN_HEAL_RANGE
                }
            if (healers > 0) {
                boss.setCurrentHp(minOf(boss.getMaxHp(), boss.getCurrentHp() + healers * GUARDIAN_HEAL))
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // Callisto / Artio - crush, with the roar that knocks you back.
    // -----------------------------------------------------------------------------------------

    private suspend fun callisto(
        task: QueueTask,
        boss: Npc,
        target: Pawn,
    ) {
        if (world.chance(1, ROAR_RATE)) {
            boss.forceChat("Grrrrrr!")
            boss.animate(boss.combatDef.attackAnimation)
            boss.dealHit(target = target, maxHit = 50, landHit = true, delay = 1)
            knockBack(boss, target)
            return
        }

        if (boss.canAttackMelee(task, target, moveIfNeeded = false)) {
            boss.meleeSwing(target, maxHit = 55, style = CombatStyle.CRUSH)
        } else {
            boss.magicSplash(target, gfx = null, animation = boss.combatDef.attackAnimation, maxHit = 31)
        }
    }

    /** Shoves the target directly away from the boss, as the roar does. */
    private fun knockBack(
        boss: Npc,
        target: Pawn,
    ) {
        val player = target as? Player ?: return
        val dx = (player.tile.x - boss.tile.x).coerceIn(-1, 1) * KNOCKBACK_TILES
        val dz = (player.tile.z - boss.tile.z).coerceIn(-1, 1) * KNOCKBACK_TILES
        player.moveTo(Tile(player.tile.x + dx, player.tile.z + dz, player.tile.height))
        player.message("Callisto's roar sends you flying!")
    }

    // -----------------------------------------------------------------------------------------
    // Venenatis / Spindel - three styles, and a web that drains prayer and run energy.
    // -----------------------------------------------------------------------------------------

    private suspend fun venenatis(
        task: QueueTask,
        boss: Npc,
        target: Pawn,
    ) {
        if (world.chance(1, WEB_RATE)) {
            boss.animate(boss.combatDef.attackAnimation)
            world.spawn(boss.createProjectile(target, gfx = VENENATIS_WEB_GFX, startHeight = 40, endHeight = 31, delay = 41, angle = 15))
            boss.dealHit(target = target, maxHit = 30, landHit = true, delay = 3)
            (target as? Player)?.let { player ->
                player.getSkills().alterCurrentLevel(Skills.PRAYER, -WEB_PRAYER_DRAIN)
                player.runEnergy = (player.runEnergy - WEB_RUN_DRAIN).coerceAtLeast(0.0)
                player.message("The sticky web saps your strength.")
            }
            return
        }

        if (boss.canAttackMelee(task, target, moveIfNeeded = false)) {
            boss.meleeSwing(target, maxHit = 21)
        } else if (world.chance(1, 2)) {
            boss.prepareAttack(CombatClass.RANGED, CombatStyle.RANGED, AttackStyle.ACCURATE)
            boss.animate(boss.combatDef.attackAnimation)
            boss.dealHit(target = target, maxHit = 35, landHit = world.chance(2, 3), delay = 3)
        } else {
            boss.magicSplash(target, gfx = VENENATIS_WEB_GFX, animation = boss.combatDef.attackAnimation, maxHit = 30)
        }
    }

    // -----------------------------------------------------------------------------------------
    // Vet'ion / Calvar'ion - lightning, and the enraged second half.
    // -----------------------------------------------------------------------------------------

    /**
     * Both forms of both bosses.
     *
     * **The ordinary attack is magic, not melee**, which is the whole shape of this fight: he
     * "launches lightning strikes that damage in a 3x3 radius", and although he swings his sword
     * to do it, that swing "does not deal any melee damage" and protection prayers do not stop the
     * lightning. So the sword animation plays on a magic hit that always lands, and the only real
     * melee here is the occasional shield bash he uses on someone standing next to him.
     *
     * Max hits differ between the two - 44 for Vet'ion, 26 for Calvar'ion - which is why [maxHit]
     * is read off the boss rather than fixed.
     */
    private suspend fun vetion(
        task: QueueTask,
        boss: Npc,
        target: Pawn,
    ) {
        if (boss.getCurrentHp() * 2 <= boss.getMaxHp() && boss.attr[ENRAGED] != true) {
            enrage(boss)
            return
        }

        val maxHit = if (boss.id == getRSCM("npc.vetion")) VETION_MAX_HIT else CALVARION_MAX_HIT

        // The shield bash is the one attack that is genuinely melee, and only within reach.
        if (boss.canAttackMelee(task, target, moveIfNeeded = false) && world.chance(1, SHIELD_BASH_RATE)) {
            boss.prepareAttack(CombatClass.MELEE, CombatStyle.CRUSH, AttackStyle.AGGRESSIVE)
            boss.animate(VETION_BASH_ANIM)
            if (MeleeCombatFormula.getAccuracy(boss, target) >= world.randomDouble()) {
                target.hit(world.random(maxHit), type = HitType.HIT, delay = 1)
            } else {
                target.hit(damage = 0, type = HitType.BLOCK, delay = 1)
            }
            return
        }

        boss.prepareAttack(CombatClass.MAGIC, CombatStyle.MAGIC, AttackStyle.ACCURATE)
        boss.animate(boss.combatDef.attackAnimation)
        boss.dealHit(target = target, maxHit = maxHit, landHit = true, delay = 2)
    }

    /**
     * The second half of the fight.
     *
     * The change of *model* is the one part not reproduced - see this class' doc comment - but the
     * enrage does play its own animation, and everything the phase actually does is here: full
     * health back, faster attacks, and two hounds.
     *
     * The enraged form keeps the normal form's animation ids rather than switching to the enraged
     * set (9966/9968/9970/...). Those are rigged for the enraged model, and since the npc id and
     * therefore the model cannot change here, playing them would put the wrong skeleton on the
     * model that is actually on screen.
     */
    private fun enrage(boss: Npc) {
        boss.attr[ENRAGED] = true
        boss.setCurrentHp(boss.getMaxHp())
        boss.combatDef = boss.combatDef.copy(attackSpeed = ENRAGED_ATTACK_SPEED)
        boss.animate(VETION_ENRAGE_ANIM)
        boss.forceChat("You'll pay for that!")

        repeat(2) { index ->
            val hound = Npc(getRSCM("npc.skeleton_hellhound_6613"), boss.tile.transform(if (index == 0) -2 else 2, 0), world)
            hound.respawns = false
            world.spawn(hound)
        }
    }

    // -----------------------------------------------------------------------------------------
    // Shared attack shapes.
    // -----------------------------------------------------------------------------------------

    /** A magic projectile whose damage is capped at [maxHit] and whose accuracy is the npc's. */
    /** [gfx] null fires no projectile, for the bosses whose own projectile is not sourced. */
    private fun Npc.magicSplash(
        target: Pawn,
        gfx: Int?,
        animation: Int,
        maxHit: Int,
    ) {
        prepareAttack(CombatClass.MAGIC, CombatStyle.MAGIC, AttackStyle.ACCURATE)
        animate(animation)
        /*
         * An `onNpcCombat` loop never touches `defaultAttackSound`, so every boss in this file was
         * silent regardless of what its media entry said. The Chaos Elemental is the one that had a
         * fully sourced set waiting - CHAOS_ELEMENTAL_ATTACK/HIT/DEATH - and could not reach it.
         * The bosses with no clip in `Sound` at all stay silent, and correctly so.
         */
        playAttackSound(target)
        gfx?.let { world.spawn(createProjectile(target, gfx = it, startHeight = 40, endHeight = 31, delay = 51, angle = 15)) }
        dealHit(
            target = target,
            maxHit = maxHit,
            landHit = MagicCombatFormula.getAccuracy(this, target) >= world.randomDouble(),
            delay = RangedCombatStrategy.getHitDelay(getFrontFacingTile(target), target.getCentreTile()),
        )
    }

    private fun Npc.meleeSwing(
        target: Pawn,
        maxHit: Int,
        style: CombatStyle = CombatStyle.CRUSH,
    ) {
        prepareAttack(CombatClass.MELEE, style, AttackStyle.AGGRESSIVE)
        animate(combatDef.attackAnimation)
        // Same gap as magicSplash: this animates directly instead of going through
        // MeleeCombatStrategy, which is the only other place that would have played the clip.
        playAttackSound(target)
        if (MeleeCombatFormula.getAccuracy(this, target) >= world.randomDouble()) {
            target.hit(world.random(maxHit), type = HitType.HIT, delay = 1)
        } else {
            target.hit(damage = 0, type = HitType.BLOCK, delay = 1)
        }
    }

    private companion object {
        /** Used only when a boss' combat def carries no attack range of its own. */
        const val FALLBACK_RANGE = 8

        const val EXPLOSION_RATE = 12
        const val DISARM_RATE = 15
        const val RAIN_OF_KNOWLEDGE_RATE = 10
        const val ROAR_RATE = 10
        const val WEB_RATE = 8
        /** How often a player standing in reach gets the shield bash instead of lightning. */
        const val SHIELD_BASH_RATE = 5

        const val CONFUSION_RANGE = 5
        const val KNOCKBACK_TILES = 3
        const val SCORPIA_PRAYER_DRAIN = 2
        const val WEB_PRAYER_DRAIN = 3
        const val WEB_RUN_DRAIN = 1000.0
        const val GUARDIAN_HEAL_RANGE = 3
        const val GUARDIAN_HEAL = 10
        const val ENRAGED_ATTACK_SPEED = 5

        /*
         * Every animation below was checked against the npc's own rig with
         * `gradlew :game-server:npcAnimDiag` - see `named-combat-media.json`'s notes. Two of these
         * had been written from memory and were wrong: the archaeologist's 7070/7071 turned out to
         * live in frame groups 1770 and 1575, which belong to entirely different creatures, and the
         * Chaos Elemental's 3146 is its block rather than its attack.
         */

        /** The shared caster animation, and one of the three the client was observed using. */
        const val CHAOS_FANATIC_ANIM = 811
        const val CHAOS_FANATIC_GFX = Graphic.CHAOS_FANATIC_HIT

        /** His one animation of his own (frame group 192, matching his idle); the rest are generic. */
        const val ARCHAEOLOGIST_ATTACK_ANIM = 3353
        const val ARCHAEOLOGIST_GFX = Graphic.CRAZY_ARCHAEOLOGIST_BOOK
        const val ARCHAEOLOGIST_SPECIAL_GFX = Graphic.RAIN_OF_KNOWLEDGE_BOOK

        /** 3149 is the action the client was observed using; 3148 is its other one. */
        const val CHAOS_ELEMENTAL_ANIM = 3149
        const val CHAOS_ELEMENTAL_SPECIAL_ANIM = 3148
        const val CHAOS_ELEMENTAL_GFX = Graphic.CHAOS_ELEMENTAL_CONFUSION_PROJECTILE

        /**
         * A stand-in. Neither `api/cfg/Graphic` nor the wiki names a projectile for Venenatis' web,
         * and Sarachnis - the game's other giant spider - has one that reads correctly. Callisto's
         * and Vet'ion's own projectiles are likewise unsourced, so neither fires one at all rather
         * than borrowing something that would plainly be the wrong effect.
         */
        const val VENENATIS_WEB_GFX = Graphic.SARACHNIS_WEB_PROJECTILE
        const val VETION_MAX_HIT = 44
        const val CALVARION_MAX_HIT = 26

        /**
         * The priority-9 variant of the sword swing (9969), sharing its first three sound
         * beats - the shield bash. See `named-combat-media.json` for how the rest of this
         * boss' skeletal animation block was identified.
         */
        const val VETION_BASH_ANIM = 9978

        /** The one unpaired animation in the block, which is what a one-off transition is. */
        const val VETION_ENRAGE_ANIM = 9977

        val GUARDIANS_SUMMONED = org.alter.game.model.attr.AttributeKey<Boolean>()
        val ENRAGED = org.alter.game.model.attr.AttributeKey<Boolean>()
    }
}
