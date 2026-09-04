package org.alter.plugins.content.interfaces.gameframe.tabs.settings

/**
 * The sub index on `134:19` of each drawn row of one category.
 *
 * ### Why the server has to work this out
 *
 * The rows of the "All Settings" panel are `cc_create`d by the client, so a click arrives as nothing
 * but a **sub index** on component `134:19`. No packet carries a setting id. Recovering the setting
 * therefore means counting rows exactly the way the client counted them - and a row the client
 * skipped still shifts every index after it, so the skip rules have to match exactly rather than
 * approximately.
 *
 * ### What the client is doing
 *
 * `[clientscript,3841]` renders one row per setting and returns early when `[clientscript,3955]`
 * says the row is hidden. Measuring the control-flow graph of every row-builder script
 * (`3846`, `3850`, `3856`, `3860`, `3864`, `3865`, `3842`, `3868`, `3869`, `4182`, `7092`) gives a
 * uniform result: **each drawn row takes exactly one sub on `134:19`, and a section header takes
 * none** - headers create only on the label layer `134:18`. That is what makes this tractable.
 *
 * ### Which rows are hidden
 *
 * `3955` tests a row's gate params against a set of client capabilities. Those rules are *not*
 * written out here: [org.alter.tools.SettingsDump] runs 3955 itself and bakes the resulting set of
 * hiding gate params into the catalogue, keyed by varbit 1777. Transcribing them by hand was tried
 * and five of the seventeen came out inverted, which is a bad failure to have - a wrongly hidden row
 * does not vanish, it shifts every sub after it so clicks land on the neighbouring setting.
 */
class SettingsLayout(
    val category: SettingCategory,
    /** The gate params that hide a row for this player, from [SettingsCatalogue.hiddenGates]. */
    private val hiddenGates: Set<Int>,
) {
    private val bySub: List<SettingEntry> =
        category.settings.filter { entry -> !entry.isHeader && entry.gates.none { it in hiddenGates } }

    val size: Int get() = bySub.size

    /** The setting the client drew at [sub], or null if the panel has no such row. */
    fun settingAt(sub: Int): SettingEntry? = bySub.getOrNull(sub)

    /** The sub the client drew [entry] at, or `-1` when it is hidden for this player. */
    fun subOf(entry: SettingEntry): Int = bySub.indexOfFirst { it.row == entry.row }

    fun rows(): List<SettingEntry> = bySub
}
