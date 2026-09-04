package org.alter.plugins.content.interfaces.gameframe.tabs.settings

/**
 * The Settings catalogue this cache defines, as baked by `gradlew :game-server:settingsDump`.
 *
 * The "All Settings" panel is drawn by the client from cache data the server never sees, so this is
 * the server's copy of the same list. Every field here comes out of a struct param or one of the two
 * dispatch clientscripts - nothing is hand-written, which is what keeps it honest if the cache moves.
 *
 * See [org.alter.tools.SettingsDump] for how it is produced and [SettingsService] for the loader.
 */
data class SettingsCatalogue(
    val cacheRevision: Int,
    /**
     * The gate params that hide a row on this client, keyed by the value of varbit 1777 (membership).
     * Produced by running `[clientscript,3955]` in the dump tool rather than by transcribing it.
     */
    val hiddenGates: Map<Int, List<Int>>,
    val categories: List<SettingCategory>,
)

data class SettingCategory(
    val index: Int,
    val structId: Int,
    val name: String,
    val settings: List<SettingEntry>,
)

data class SettingEntry(
    /** Position in the category's enum. Also the order the client draws the rows in. */
    val row: Int,
    /** Struct param 1077. `-1` for a row that is pure decoration, such as a section header. */
    val settingId: Int,
    /** Struct param 1078. See [SettingType]. */
    val type: Int,
    val title: String,
    val description: String,
    val keywords: String,
    val requirementNote: String,
    /** `"varbit"`, `"varp"`, or null when `[clientscript,3960]` computes the value instead. */
    val varKind: String?,
    val varId: Int,
    /** Whether `[clientscript,3965]` has a case for this setting, i.e. the client will change it. */
    val writable: Boolean,
    val sliderSteps: Int,
    val sliderStepSize: Int,
    /** The params `[clientscript,3955]` tests to decide whether this row is drawn. */
    val gates: List<Int>,
) {
    val isHeader: Boolean get() = type == SettingType.HEADER

    /**
     * A row the server can both read and write. Everything else - headers, spacers, colour pickers,
     * and the handful of settings whose value is computed rather than stored - is left to the client.
     */
    val isLive: Boolean get() = writable && varKind != null && varId != -1 && settingId != -1

    val isToggle: Boolean get() = type in SettingType.TOGGLES
}

/**
 * Row types, read from struct param 1078. The numbers are the cases of the `switch` in
 * `[clientscript,3841]`, which is what decides how a row is drawn.
 */
object SettingType {
    const val TOGGLE = 0
    const val SLIDER = 1
    const val TOGGLE_DEPENDENT = 2
    const val KEYBIND = 3
    const val TOGGLE_WITH_INPUT = 4
    const val HEADER = 5
    const val BUTTON = 6
    const val SPACER = 7
    const val INFO_TEXT = 8
    const val TOGGLE_ALT = 9
    const val COLOUR = 10

    /** The row types that behave as an on/off switch, so a click is a flip of the setting's var. */
    val TOGGLES = setOf(TOGGLE, TOGGLE_DEPENDENT, TOGGLE_WITH_INPUT, TOGGLE_ALT)
}
