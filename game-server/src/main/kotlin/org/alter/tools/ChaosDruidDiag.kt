package org.alter.tools

import org.alter.game.Server
import org.alter.game.model.Tile
import org.alter.game.model.collision.canOccupy
import org.alter.game.model.timer.FROZEN_TIMER
import org.alter.game.model.combat.CombatClass
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.entity.Npc
import java.nio.file.Paths
import kotlin.system.exitProcess

/**
 * TEMPORARY diagnostic - boots a world and re-checks everything `content/npcs/chaosdruid` was
 * built on: that the plugin registered at all, that the wiki's stat block survived into the live
 * combat def, that the custom combat strategy is the one the engine picks, that the melee formula
 * reproduces the published max hit of 2, and that all 37 spawn tiles are standable.
 *
 * The spawn check is the reason this exists. Chaos druids stand in five separate places, three of
 * them added blind from wiki pins - a dungeon under the Wilderness, a locked tower room and a
 * church *roof* on plane 3 - and a pin that lands in a wall or on a floor that does not exist at
 * that height produces an npc nobody can reach rather than an error.
 *
 * Standability is [org.alter.game.model.collision.canOccupy], not `isClipped`: the latter is true
 * for *any* collision flag, including the `BLOCK_NPCS` an npc plants on its own tile and the wall
 * flags a tile you can stand on perfectly well carries on its edges. Checking with it reports seven
 * of these tiles blocked, four of them spawns that have been in the game and working for weeks.
 *
 * The strategy and formula checks go through reflection because :game-server cannot see
 * :game-plugins at compile time (it is a `runtimeOnly` dependency); the classes are on the
 * runtime classpath, which is all this needs.
 *
 * Usage: gradlew :game-server:chaosDruidDiag
 */
object ChaosDruidDiag {
    private const val CHAOS_DRUID = 520

    /** Swings taken when exercising the combat strategy. */
    private const val ATTACK_SAMPLES = 2000

    /** Every published pin, by area, with the plane it stands on. */
    private val SPAWNS =
        listOf(
            "Chaos Druid Tower" to listOf(Tile(2561, 3355), Tile(2561, 3357), Tile(2563, 3355), Tile(2563, 3358)),
            "Slepe church roof" to
                listOf(Tile(3736, 3318, 3), Tile(3738, 3314, 3), Tile(3739, 3313, 3), Tile(3739, 3321, 3), Tile(3742, 3317, 3)),
            "Edgeville Dungeon" to
                listOf(
                    Tile(3104, 9942), Tile(3105, 9936), Tile(3106, 9941), Tile(3107, 9943),
                    Tile(3109, 9931), Tile(3110, 9941), Tile(3111, 9936), Tile(3111, 9939),
                    Tile(3114, 9929), Tile(3115, 9925), Tile(3115, 9932),
                ),
            "Taverley Dungeon" to
                listOf(
                    Tile(2929, 9848), Tile(2931, 9846), Tile(2932, 9852),
                    Tile(2936, 9846), Tile(2936, 9852), Tile(2937, 9849),
                ),
            "Yanille Agility Dungeon" to
                listOf(
                    Tile(2611, 9483), Tile(2611, 9487), Tile(2612, 9488), Tile(2613, 9482),
                    Tile(2613, 9521), Tile(2614, 9483), Tile(2614, 9521), Tile(2614, 9525),
                    Tile(2615, 9487), Tile(2616, 9484), Tile(2616, 9522),
                ),
        )

    private var failures = 0

    @JvmStatic
    fun main(args: Array<String>) {
        val server = Server()
        server.startServer(apiProps = Paths.get("../data/api.yml"))
        val world =
            server.startGame(
                filestore = Paths.get("../data", "cache"),
                gameProps = Paths.get("../game.yml"),
                devProps = Paths.get("../dev-settings.yml"),
            )

        println()
        println("=== combat def ===")
        val def = world.plugins.npcCombatDefs[CHAOS_DRUID]
        check("plugin registered a combat def", def != null)
        if (def == null) {
            report()
            exitProcess(1)
        }
        check("hitpoints 20", def.hitpoints == 20)
        check("attack 8", def.attack == 8)
        check("strength 8", def.strength == 8)
        check("defence 12", def.defence == 12)
        check("magic 10", def.magic == 10)
        check("attack speed 4", def.attackSpeed == 4)
        check("respawn 25", def.respawnDelay == 25)
        check("slayer xp 20", def.slayerXp == 20.0)
        check("attack anim 422", def.attackAnimation == 422)
        check("block anim 425", def.blockAnimation == 425)
        check("death anim 836", def.deathAnimation == listOf(836))
        check("aggressive", def.aggressiveRadius > 0)

        println()
        println("=== attack ===")
        val druid = Npc(CHAOS_DRUID, Tile(3104, 9942), world)
        world.spawn(druid)
        val dummy = Npc(CHAOS_DRUID, Tile(3105, 9942), world)
        world.spawn(dummy)

        val strategy =
            Class.forName("org.alter.plugins.content.combat.CombatConfigs")
                .getField("INSTANCE").get(null)
                .let { configs ->
                    configs.javaClass.getMethod("getCombatStrategy", Class.forName("org.alter.game.model.entity.Pawn"))
                        .invoke(configs, druid)
                }
        check(
            "engine picks ChaosDruidCombatStrategy (got ${strategy.javaClass.simpleName})",
            strategy.javaClass.name == "org.alter.plugins.content.npcs.chaosdruid.ChaosDruidCombatStrategy",
        )

        val pawnClass = Class.forName("org.alter.game.model.entity.Pawn")
        val formula = Class.forName("org.alter.plugins.content.combat.formula.MeleeCombatFormula").getField("INSTANCE").get(null)
        val maxHit =
            formula.javaClass
                .getMethod("getMaxHit", pawnClass, pawnClass, java.lang.Double.TYPE, java.lang.Double.TYPE)
                .invoke(formula, druid, dummy, 1.0, 1.0) as Int
        check("melee max hit is the published 2 (got $maxHit)", maxHit == 2)

        /*
         * Swing the strategy for real, many times, against a live target. Compiling is not evidence
         * that a combat strategy works: this is the only check that actually runs both branches,
         * and the one that would catch the trap this monster is most exposed to - a magic attack
         * that leaves `combatStyle` on MAGIC makes the *next punch* throw out of
         * MeleeCombatFormula.getEquipmentAttackBonus, which accepts only stab/slash/crush.
         *
         * `previouslySetAnim` is reset each swing because an npc's animate() refuses a second
         * animation in the same tick; without the reset every swing after the first would record
         * nothing and the two branches could not be told apart.
         */
        val attackMethod =
            strategy.javaClass.getMethod("attack", pawnClass, pawnClass).also { it.isAccessible = true }
        var punches = 0
        var casts = 0
        var thrown = 0
        repeat(ATTACK_SAMPLES) {
            druid.previouslySetAnim = -1
            // Clear the bind so the next swing is eligible to cast again.
            dummy.timers.remove(FROZEN_TIMER)
            try {
                attackMethod.invoke(strategy, druid, dummy)
            } catch (e: Exception) {
                if (thrown++ == 0) println("  first exception: ${(e.cause ?: e)}")
            }
            when (druid.previouslySetAnim) {
                422 -> punches++
                710 -> casts++
            }
        }
        check("$ATTACK_SAMPLES swings, none threw (threw $thrown)", thrown == 0)
        check("both branches fire (punched $punches, cast $casts)", punches > 0 && casts > 0)
        /*
         * Not an exact equality: the server's game loop is running on its own thread while this
         * loop swings, and `SynchronizationTask` clears `previouslySetAnim` on every spawned pawn
         * once a cycle. A sample whose read falls in that window records neither animation. It is
         * a handful in a few thousand and it is this diagnostic racing the tick, not the strategy
         * failing to swing - the "none threw" check above is what proves every swing happened.
         */
        val classified = punches + casts
        check(
            "nearly every swing is classifiable ($classified of $ATTACK_SAMPLES; the rest lost to the tick)",
            classified >= ATTACK_SAMPLES - (ATTACK_SAMPLES / 100),
        )
        // 1 in 4, with room for the sampling noise at this count.
        val castShare = casts.toDouble() / ATTACK_SAMPLES
        check("cast rate is about 1 in 4 (measured ${"%.3f".format(castShare)})", castShare in 0.20..0.30)
        check("combat style is still CRUSH after casting (${druid.combatStyle})", druid.combatStyle == CombatStyle.CRUSH)
        check("combat class is still MELEE after casting (${druid.combatClass})", druid.combatClass == CombatClass.MELEE)

        // A target already bound is never re-bound: the swing must fall through to a punch.
        dummy.timers[FROZEN_TIMER] = 10
        var castsWhileFrozen = 0
        repeat(ATTACK_SAMPLES) {
            druid.previouslySetAnim = -1
            runCatching { attackMethod.invoke(strategy, druid, dummy) }
            if (druid.previouslySetAnim == 710) castsWhileFrozen++
        }
        check("a bound target is never re-bound (casts while frozen: $castsWhileFrozen)", castsWhileFrozen == 0)
        dummy.timers.remove(FROZEN_TIMER)

        println()
        println("=== spawns ===")
        var placed = 0
        SPAWNS.forEach { (area, tiles) ->
            tiles.forEach { tile ->
                world.definitions.createRegion(world, tile.regionId)
                check("$area $tile standable", world.collision.canOccupy(tile))
                placed++
            }
        }
        check("37 pins in total (counted $placed)", placed == 37)

        val spawned = world.npcs.count { it.id == CHAOS_DRUID }
        check("world holds $placed live chaos druids (counted $spawned, minus the 2 test npcs)", spawned - 2 == placed)

        report()
        // The server's own threads are non-daemon; without this the diagnostic never returns.
        exitProcess(if (failures == 0) 0 else 1)
    }

    private fun check(
        label: String,
        ok: Boolean,
    ) {
        println(if (ok) "  ok    $label" else "  FAIL  $label")
        if (!ok) failures++
    }

    private fun report() {
        println()
        println(if (failures == 0) "All chaos druid checks passed." else "$failures chaos druid check(s) FAILED.")
    }
}
