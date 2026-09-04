package org.alter.plugins.content.npcs

/**
 * Deals a species' npc ids across its camps so that every id the wiki publishes actually appears
 * somewhere in the world.
 *
 * ## Why a shared cursor rather than one per camp
 *
 * The obvious way to place a camp is `npcKeys[tileIndex % npcKeys.size]`, which is what
 * `content/npcs/zombie/ZombieSpawnPlugin` does and what every camp here started out doing. It is
 * correct only when a camp has at least as many pins as it names ids - and for several species it
 * does not, by a wide margin. The wiki gives the level 19 ghost **twenty** ids and then publishes
 * locations with two, three and five pins. Restarting the count at zero in every camp means npc 85
 * stands in all twelve of them and five other ghosts stand nowhere at all.
 *
 * Keying the cursor on the **id pool** rather than on the camp fixes that: the twelve ghost camps
 * that share `LEVEL_19_IDS` share one running count, so the twenty ids are dealt across all
 * seventy-odd of their pins and every one gets used. Camps naming a different pool keep their own
 * count, so a location the wiki gives specific ids is unaffected.
 *
 * Equal lists share a cursor, which is the intent - `listOf(PLAIN, ARMED_42)` is the same pool
 * wherever it appears. A camp whose pool is a bespoke per-tile ordering (the Edgeville Dungeon
 * hobgoblins, whose pins carry their own published levels) is unique, starts at zero, and so is
 * dealt positionally exactly as written.
 *
 * `BestiaryVerify` walks the camps through one of these in the same order to assert the coverage
 * this exists for.
 */
internal class SpawnDealer {
    private val cursors = HashMap<List<String>, Int>()

    /** The next id from [pool], advancing that pool's own cursor. */
    fun next(pool: List<String>): String {
        val cursor = cursors.getOrDefault(pool, 0)
        cursors[pool] = cursor + 1
        return pool[cursor % pool.size]
    }
}
