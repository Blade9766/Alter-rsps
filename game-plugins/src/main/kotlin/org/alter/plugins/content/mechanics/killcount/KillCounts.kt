package org.alter.plugins.content.mechanics.killcount

import dev.openrune.cache.CacheManager
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player

/**
 * A per-monster kill count for every player, kept across sessions.
 *
 * ## Where the count lives
 *
 * One persistent string attribute holds the whole table, as `name:count` pairs joined by commas -
 * the same shape [org.alter.plugins.content.skills.slayer.Slayer] already uses for its blocked and
 * unlocked task lists. It is not the tidiest encoding, but the save layer
 * ([org.alter.game.saving.impl.AttributeSerialisation]) flattens an attribute map into a single BSON
 * document of scalars, so a nested map is not an option and one attribute per monster would leave
 * hundreds of keys in every save file.
 *
 * The whole table is decoded and re-encoded on each kill. With [MAX_TRACKED] capping the table that
 * is a few hundred short strings, which is nothing next to the drop roll and the loot spawn that
 * happen on the same tick.
 *
 * Cache display names are the keys, preserving their original casing for display; every lookup is
 * case-insensitive. A name carrying a `,` or a `:` would break the encoding, so [nameOf] refuses it
 * - no npc in the cache currently has one, and silently not counting a monster is better than
 * corrupting the rest of the table.
 */
object KillCounts {
    /** `name:count,name:count` - see the class comment for why it is one string. */
    private val COUNTS = AttributeKey<String>(persistenceKey = "kill_counts")

    /**
     * How many distinct monsters a single player's table may hold.
     *
     * Kills of monsters beyond the cap are still announced and still count towards the session, they
     * just stop being written down. The cap only exists so that a save file cannot grow without
     * bound; the whole npc cache is well under it.
     */
    private const val MAX_TRACKED = 512

    /**
     * Monsters whose kill count is announced in the chatbox as it happens.
     *
     * Everything else is counted silently and read back with `::kc`. Announcing every kill would
     * make training unreadable, and this is the same split the real game uses: bosses tell you where
     * you are, ordinary monsters do not.
     *
     * Names are matched case-insensitively against the cache display name, so entries for bosses
     * this server has not spawned yet simply never fire. The list covers what is here now (the
     * barrows brothers, the King Black Dragon) plus the rest of the standard roster, so a boss added
     * later starts announcing without anyone having to remember this file.
     */
    val ANNOUNCED: Set<String> =
        setOf(
            // Barrows.
            "ahrim the blighted", "dharok the wretched", "guthan the infested",
            "karil the tainted", "torag the corrupted", "verac the defiled",
            // Low-level and quest bosses.
            "obor", "bryophyta", "the mimic", "giant mole", "deranged archaeologist",
            "sarachnis", "scurrius", "skotizo", "hespori", "zalcano", "tempoross",
            // Slayer bosses.
            "cerberus", "abyssal sire", "kraken", "thermonuclear smoke devil",
            "alchemical hydra", "grotesque guardians", "dusk", "dawn",
            // Dagannoth kings and the kalphite queen.
            "dagannoth rex", "dagannoth prime", "dagannoth supreme", "kalphite queen",
            // God wars.
            "general graardor", "k'ril tsutsaroth", "commander zilyana", "kree'arra", "nex",
            // Wilderness.
            "callisto", "artio", "venenatis", "spindel", "vet'ion", "calvar'ion",
            "scorpia", "chaos elemental", "chaos fanatic", "crazy archaeologist",
            "king black dragon", "corporeal beast",
            // Dragons and the desert.
            "zulrah", "vorkath", "the nightmare", "phosani's nightmare", "phantom muspah",
            // Desert Treasure II.
            "duke sucellus", "the leviathan", "the whisperer", "vardorvis",
        )

    /**
     * The cache display name of [npc], or null if it is not something worth counting.
     *
     * Rejects the blank and literal `"null"` names the cache uses for placeholder npcs, and any name
     * carrying one of the encoding's separators.
     */
    fun nameOf(npc: Npc): String? {
        val name = CacheManager.getNpcs()[npc.id]?.name ?: return null
        if (name.isBlank() || name == "null") {
            return null
        }
        if (name.contains(',') || name.contains(':')) {
            return null
        }
        return name
    }

    /** Whether a kill on [name] should be announced as it happens. */
    fun isAnnounced(name: String): Boolean = name.lowercase() in ANNOUNCED

    /** Every monster this player has killed, by cache display name, in no particular order. */
    fun all(player: Player): Map<String, Int> = decode(player.attr[COUNTS])

    /** How many [name] this player has killed. Case-insensitive; 0 if they never have. */
    fun count(
        player: Player,
        name: String,
    ): Int = all(player).entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value ?: 0

    /** Every tracked monster whose name contains [query], most-killed first. */
    fun search(
        player: Player,
        query: String,
    ): List<Pair<String, Int>> =
        all(player)
            .filterKeys { it.contains(query, ignoreCase = true) }
            .toList()
            .sortedByDescending { it.second }

    /** The total number of monsters killed, across every tracked name. */
    fun total(player: Player): Int = all(player).values.sum()

    /**
     * Credit one kill of [name] and return the player's new count for it.
     *
     * The returned count is correct even when the table is full and the kill was not written down,
     * so the announcement never shows a number that goes backwards mid-fight.
     */
    fun record(
        player: Player,
        name: String,
    ): Int {
        val counts = all(player).toMutableMap()
        val key = counts.keys.firstOrNull { it.equals(name, ignoreCase = true) } ?: name
        val next = (counts[key] ?: 0) + 1

        if (key !in counts && counts.size >= MAX_TRACKED) {
            return next
        }

        counts[key] = next
        player.attr[COUNTS] = encode(counts)
        return next
    }

    /** Forget every kill this player has recorded. */
    fun clear(player: Player) {
        player.attr[COUNTS] = ""
    }

    private fun decode(raw: String?): Map<String, Int> {
        if (raw.isNullOrEmpty()) {
            return emptyMap()
        }
        val counts = LinkedHashMap<String, Int>()
        raw.split(",").forEach { entry ->
            val split = entry.lastIndexOf(':')
            if (split <= 0) {
                return@forEach
            }
            val name = entry.substring(0, split).trim()
            val count = entry.substring(split + 1).trim().toIntOrNull() ?: return@forEach
            if (name.isNotEmpty() && count > 0) {
                counts[name] = count
            }
        }
        return counts
    }

    private fun encode(counts: Map<String, Int>): String =
        counts.entries.joinToString(",") { "${it.key}:${it.value}" }
}
