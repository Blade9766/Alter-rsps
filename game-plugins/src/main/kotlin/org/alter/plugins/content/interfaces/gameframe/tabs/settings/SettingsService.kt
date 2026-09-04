package org.alter.plugins.content.interfaces.gameframe.tabs.settings

import com.google.gson.Gson
import gg.rsmod.util.ServerProperties
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.service.Service
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Loads `data/cfg/settings/settings.json` - this cache's Settings catalogue, as baked by
 * `gradlew :game-server:settingsDump`.
 *
 * Nothing here is hand-maintained. The file is a transcription of enum 422, the category structs and
 * the two dispatch clientscripts, so the only thing this service adds is the lookups the plugin needs
 * and a startup report of how much of the catalogue this server can actually act on.
 */
class SettingsService : Service {
    private val gson = Gson()

    lateinit var catalogue: SettingsCatalogue
        private set

    /** Every live setting by its cache setting id (struct param 1077). */
    private val byId = mutableMapOf<Int, SettingEntry>()

    /** The category a setting belongs to, so a click can be resolved back to its list. */
    private val categoryOf = mutableMapOf<Int, SettingCategory>()

    private var loaded = false

    override fun init(
        server: Server,
        world: World,
        serviceProperties: ServerProperties,
    ) {
        if (loaded) {
            return
        }
        loaded = true

        val file = Paths.get(serviceProperties.get("catalogue") ?: "../data/cfg/settings/settings.json")
        Files.newBufferedReader(file).use { reader ->
            catalogue = gson.fromJson(reader, SettingsCatalogue::class.java)
        }

        for (category in catalogue.categories) {
            for (entry in category.settings) {
                if (entry.settingId == -1) {
                    continue
                }
                // A setting id can appear in more than one category - the client shows the same row
                // in both places. The first wins; they resolve to the same var either way.
                byId.putIfAbsent(entry.settingId, entry)
                categoryOf.putIfAbsent(entry.settingId, category)
            }
        }

        val rows = catalogue.categories.sumOf { it.settings.size }
        val live = catalogue.categories.sumOf { category -> category.settings.count { it.isLive } }
        Server.logger.info {
            "Loaded the Settings catalogue: ${catalogue.categories.size} categories, $rows rows, " +
                "$live the server can read and write (cache revision ${catalogue.cacheRevision})."
        }

        // [Setting] names a var per entry so content can read a setting without carrying var ids
        // around. Those numbers came from this same catalogue, and this is what keeps them that way:
        // a cache change that moves a setting's var shows up here rather than as content quietly
        // reading the wrong bit.
        for (setting in Setting.entries) {
            val entry = byId[setting.settingId]
            when {
                entry == null ->
                    Server.logger.warn {
                        "Setting ${setting.name} (id ${setting.settingId}) is not in the catalogue for this cache."
                    }
                entry.varKind != "varbit" || entry.varId != setting.varbit ->
                    Server.logger.warn {
                        "Setting ${setting.name} expects varbit ${setting.varbit} but the catalogue " +
                            "maps id ${setting.settingId} to ${entry.varKind} ${entry.varId}."
                    }
            }
        }

    }

    /**
     * The gate params that hide a row for this player. Membership is the only input to
     * `[clientscript,3955]` that varies per player; everything else is a property of the client and
     * is already folded into the baked sets.
     */
    fun hiddenGates(membership: Int): Set<Int> =
        (catalogue.hiddenGates[membership] ?: catalogue.hiddenGates[0].orEmpty()).toSet()

    /** The rows of [category] as the client draws them, in sub order. */
    fun layout(
        category: SettingCategory,
        membership: Int,
    ): SettingsLayout = SettingsLayout(category, hiddenGates(membership))

    fun category(index: Int): SettingCategory? = catalogue.categories.getOrNull(index)

    fun categories(): List<SettingCategory> = catalogue.categories

    fun setting(settingId: Int): SettingEntry? = byId[settingId]

    fun categoryOf(settingId: Int): SettingCategory? = categoryOf[settingId]

    /** Every live setting, in catalogue order. Used by the login sync. */
    fun liveSettings(): List<SettingEntry> = catalogue.categories.flatMap { category -> category.settings.filter { it.isLive } }
}
