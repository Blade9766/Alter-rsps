package org.alter.plugins.content.npcs

import org.alter.game.model.Direction

/**
 * The four cardinal facings, dealt round a camp so a graveyard is not twenty-one monsters all
 * looking south.
 *
 * Which way a spawn faces is not published anywhere and barely matters - it is overwritten the
 * moment the monster walks or picks a target - but every camp in this tree is a cluster of pins a
 * few tiles apart, and a uniform facing reads as a parade rather than a population.
 *
 * Lifted out of `content/npcs/zombie/ZombieSpawnPlugin`, which had it privately, once eight more
 * packages wanted the same four values.
 */
internal object SpawnFacings {
    val CYCLE = listOf(Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST)

    /** The facing for the [index]th pin of a camp. */
    fun at(index: Int): Direction = CYCLE[index % CYCLE.size]
}
