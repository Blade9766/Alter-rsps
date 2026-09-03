package org.alter.plugins.content.areas.duelarena

/**
 * Hands out the four arenas.
 *
 * The Duel Arena was never instanced - the arenas are real places on the map and two duels cannot
 * share one - so this is a plain four-slot allocator rather than anything to do with instancing.
 * A fifth pair simply waits.
 */
object DuelArenas {
    private val occupants = HashMap<DuelPlot, DuelSession>()

    /**
     * Claims a free arena for [session], or null if all four are busy.
     */
    @Synchronized
    fun claim(session: DuelSession): DuelPlot? {
        val free = DuelArena.ARENAS.firstOrNull { it !in occupants } ?: return null
        occupants[free] = session
        return free
    }

    @Synchronized
    fun release(session: DuelSession) {
        occupants.entries.removeIf { it.value == session }
    }

    /**
     * The duel being fought in the arena containing this tile, if any. Used to keep bystanders out
     * of an arena that is in use.
     */
    @Synchronized
    fun sessionAt(plot: DuelPlot): DuelSession? = occupants[plot]

    @Synchronized
    fun clear() = occupants.clear()
}
