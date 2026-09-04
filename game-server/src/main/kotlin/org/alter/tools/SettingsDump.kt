package org.alter.tools

import com.google.gson.GsonBuilder
import dev.openrune.cache.CLIENTSCRIPT
import dev.openrune.cache.CacheManager
import java.io.File
import java.nio.file.Paths

/**
 * Bakes this cache's own Settings catalogue into `data/cfg/settings/settings.json`.
 *
 * The "All Settings" panel (interface 134) is built entirely by the client: its onload runs
 * `[clientscript,3826]` -> `3827` -> `3840`, which walks **enum 422** (the category list), reads
 * each category struct's param **745** (an enum of that category's setting structs) and renders one
 * row per setting. None of that list reaches the server, so the server has to read the same cache
 * data to know what the player is looking at.
 *
 * Two clientscripts hold the part that is not in the structs at all:
 *
 *  - **3960** is a single `switch` on the setting id whose every case is one `get_varbit` /
 *    `get_varp`. It is the authoritative *setting id -> var* map, and there is no other copy of it.
 *  - **3965** is the matching write dispatcher; a setting id with a case there is one the client
 *    will actually change, which is what separates a live toggle from a decorative row.
 *
 * Both are decoded here rather than transcribed, so this file can be regenerated if the cache
 * revision ever moves.
 *
 * Usage: gradlew :game-server:settingsDump
 */
object SettingsDump {

    /** Category struct params. */
    private const val PARAM_CATEGORY_NAME = 744
    private const val PARAM_CATEGORY_SETTINGS = 745

    /** Setting struct params. */
    private const val PARAM_SETTING_ID = 1077
    private const val PARAM_TYPE = 1078
    private const val PARAM_TITLE = 1086
    private const val PARAM_TITLE_ALT = 1087
    private const val PARAM_KEYWORDS = 1088
    private const val PARAM_DESCRIPTION = 1096
    private const val PARAM_REQUIREMENT_NOTE = 1116
    private const val PARAM_SLIDER_STEPS = 1101
    private const val PARAM_SLIDER_STEP_SIZE = 1102

    /**
     * The params `[clientscript,3955]` tests to decide whether a row is drawn at all.
     *
     * 3955 first collects eight client-capability answers (`1972`, `100`, `5339`, `3160`, `5849`,
     * `403`, `5548`, `1138`) and then hides the row when one of these params is set and the matching
     * capability does not hold - which is how one catalogue serves the mobile, Steam and web clients
     * from a single cache. Each row records the gates it carries, and [hiddenGates] works out which
     * of them hide a row here by running 3955 rather than by anyone reading its branches.
     */
    private val GATE_PARAMS =
        intArrayOf(619, 620, 739, 740, 741, 742, 1157, 1271, 1272, 1273, 1408, 1414, 1565, 1608, 1868, 1957, 1996)

    /** The category list, and the two dispatch scripts that hold the var mapping. */
    private const val ENUM_CATEGORIES = 422
    private const val SCRIPT_READ_SETTING = 3960
    private const val SCRIPT_WRITE_SETTING = 3965

    /** The predicate that decides whether a row is drawn. */
    private const val SCRIPT_ROW_VISIBLE = 3955

    /**
     * The client-capability answers `[clientscript,3955]` collects before testing the gate params,
     * in the order it caches them, and the value each has on the client this server is played on.
     *
     * They are all 0 for a plain desktop client: `1972` and `100` both bottom out in varbit 6352 and
     * the client-type opcodes (6518/6519), and 6352 is a var the server never sets. That single
     * assumption is what the whole hidden-row set follows from, so it is stated once, here.
     *
     * The result is checked against a category whose contents are known: with these values the
     * Controls panel shows shift-click-to-drop, middle-mouse camera and Esc-closes-interface, and
     * hides tap-to-drop, the vibration options and the mobile hotkey rows. Any other combination
     * gets that backwards.
     */
    private val CLIENT_CAPABILITIES =
        mapOf(1972 to 0, 100 to 0, 5339 to 0, 3160 to 0, 5849 to 0, 403 to 0, 5548 to 0, 1138 to 0)

    /** Varbit 1777, the only input to 3955 that varies per player rather than per client. */
    private val MEMBERSHIP_VALUES = listOf(0, 1, 2)

    @JvmStatic
    fun main(args: Array<String>) {
        CacheManager.init(Paths.get("data/cache"), 228)

        val readVars = ClientScript.load(SCRIPT_READ_SETTING).firstSwitchTargets()
        val writable = ClientScript.load(SCRIPT_WRITE_SETTING).firstSwitchTargets().keys

        val categories = mutableListOf<DumpedCategory>()
        val categoryEnum = CacheManager.getEnum(ENUM_CATEGORIES)

        for (index in 0 until categoryEnum.getSize()) {
            val categoryStruct = CacheManager.getStruct(categoryEnum.getInt(index))
            val settingsEnumId = categoryStruct.getInt(PARAM_CATEGORY_SETTINGS)
            if (settingsEnumId == -1) {
                continue
            }
            val settingsEnum = CacheManager.getEnum(settingsEnumId)
            val settings = mutableListOf<DumpedSetting>()

            for (row in 0 until settingsEnum.getSize()) {
                val struct = CacheManager.getStruct(settingsEnum.getInt(row))
                val settingId = struct.getInt(PARAM_SETTING_ID)
                val target = readVars[settingId]
                settings +=
                    DumpedSetting(
                        row = row,
                        settingId = settingId,
                        type = struct.getInt(PARAM_TYPE),
                        title = struct.getString(PARAM_TITLE).ifEmpty { struct.getString(PARAM_TITLE_ALT) },
                        description = struct.getString(PARAM_DESCRIPTION),
                        keywords = struct.getString(PARAM_KEYWORDS),
                        requirementNote = struct.getString(PARAM_REQUIREMENT_NOTE),
                        varKind = target?.kind?.takeIf { it != "script" },
                        varId = target?.id ?: -1,
                        // A setting the client will not write is decorative no matter what its row
                        // looks like; acting on one would move a var the client never shows back.
                        writable = settingId != -1 && settingId in writable,
                        sliderSteps = struct.getInt(PARAM_SLIDER_STEPS),
                        sliderStepSize = struct.getInt(PARAM_SLIDER_STEP_SIZE),
                        gates = GATE_PARAMS.filter { struct.getInt(it) == 1 },
                    )
            }

            categories +=
                DumpedCategory(
                    index = index,
                    structId = categoryEnum.getInt(index),
                    name = categoryStruct.getString(PARAM_CATEGORY_NAME),
                    settings = settings,
                )
        }

        val visibility = ClientScript.load(SCRIPT_ROW_VISIBLE)
        val hidden = MEMBERSHIP_VALUES.associateWith { hiddenGates(visibility, it) }

        val out = File("data/cfg/settings/settings.json")
        out.parentFile.mkdirs()
        val gson = GsonBuilder().setPrettyPrinting().create()
        out.writeText(
            gson.toJson(
                DumpedCatalogue(
                    cacheRevision = CacheManager.cacheRevision,
                    hiddenGates = hidden,
                    categories = categories,
                ),
            ),
        )

        for ((membership, gates) in hidden) {
            println("  gates hiding a row at varbit 1777 = $membership: $gates")
        }

        val rows = categories.sumOf { it.settings.size }
        val mapped = categories.sumOf { category -> category.settings.count { it.varKind != null } }
        val live = categories.sumOf { category -> category.settings.count { it.writable && it.varKind != null } }
        println("Wrote " + out.path)
        println("  " + categories.size + " categories, " + rows + " rows, " + mapped + " with a var, " + live + " writable with a var.")
        for (category in categories) {
            println("  %-12s %3d rows".format(category.name, category.settings.size))
        }
    }

    /**
     * Evaluates `[clientscript,3955]` for a setting carrying exactly one gate param, so that each
     * param's effect can be read off in isolation.
     *
     * This is interpreted rather than transcribed on purpose. Read by hand, five of the seventeen
     * gates came out inverted - the branch polarity is easy to get backwards and the failure is
     * silent, because a wrongly hidden row does not disappear, it just shifts every sub index after
     * it and makes clicks land on the neighbouring setting.
     */
    private fun hiddenGates(
        script: ClientScript,
        membership: Int,
    ): List<Int> = GATE_PARAMS.filter { gate -> script.rowIsHidden(gate, membership) }

    data class DumpedCatalogue(
        val cacheRevision: Int,
        /**
         * The gate params that hide a row on this client, keyed by the value of varbit 1777.
         * Derived by running 3955; the server only has to ask whether a row carries one of these.
         */
        val hiddenGates: Map<Int, List<Int>>,
        val categories: List<DumpedCategory>,
    )

    data class DumpedCategory(
        val index: Int,
        val structId: Int,
        val name: String,
        val settings: List<DumpedSetting>,
    )

    data class DumpedSetting(
        val row: Int,
        val settingId: Int,
        val type: Int,
        val title: String,
        val description: String,
        val keywords: String,
        val requirementNote: String,
        val varKind: String?,
        val varId: Int,
        val writable: Boolean,
        val sliderSteps: Int,
        val sliderStepSize: Int,
        val gates: List<Int>,
    )

    data class VarTarget(val kind: String, val id: Int)

    /**
     * The slice of the CS2 container format needed to read a dispatch table out of a script.
     *
     * A compiled script is `[ops][12 byte trailer][switch tables][2 byte switch block length]`, so
     * the op stream can only be found by measuring back from the end. Operands are sized by opcode:
     * opcode 3 carries a string, opcodes below 100 (bar 21/38/39, which are `return`/`pop_int`/
     * `pop_str`) carry an int, and everything else carries a single byte.
     */
    private class ClientScript(
        val ops: List<Pair<Int, Int>>,
        val switches: List<Pair<Int, Map<Int, Int>>>,
    ) {
        /**
         * Reads the first `switch` in the script as `case value -> the op it jumps to`, resolving
         * each case that lands directly on a var read. Both 3960 and 3965 are a single switch over
         * the setting id, so the first one is the one wanted.
         */
        fun firstSwitchTargets(): Map<Int, VarTarget> {
            val (opIndex, table) = switches.firstOrNull() ?: return emptyMap()
            val targets = mutableMapOf<Int, VarTarget>()
            for ((case, offset) in table) {
                // A case offset is relative to the op after the switch itself.
                val target = opIndex + offset + 1
                if (target !in ops.indices) {
                    continue
                }
                val (op, value) = ops[target]
                val kind =
                    when (op) {
                        OP_GET_VARBIT, OP_SET_VARBIT -> "varbit"
                        OP_GET_VARP, OP_SET_VARP -> "varp"
                        else -> null
                    }
                // Cases that jump to a helper call or a constant are settings whose value is
                // computed rather than stored; they are kept so the caller still sees the setting
                // as one the client handles, but with no var to mirror.
                targets[case] = if (kind != null) VarTarget(kind, value) else VarTarget("script", -1)
            }
            return targets
        }

        /**
         * Runs this script as `[clientscript,3955]` - "is this row hidden?" - for a setting whose
         * only gate param is [gate], with varbit 1777 set to [membership].
         *
         * A deliberately small machine: 3955 pushes constants, reads its cached capability answers
         * and varbit 1777, looks up struct params, compares and branches. Calls are answered from
         * [CLIENT_CAPABILITIES] rather than followed, which is exactly the substitution the script
         * itself makes when it caches them into locals up front. The answer is local 1, which the
         * script sets to 1 the moment any gate rejects the row.
         */
        fun rowIsHidden(
            gate: Int,
            membership: Int,
        ): Boolean {
            val locals = mutableMapOf(0 to 0, 1 to 0)
            val stack = ArrayDeque<Int>()
            var p = 0
            var steps = 0
            while (p in ops.indices && steps++ < STEP_LIMIT) {
                val (op, value) = ops[p]
                when (op) {
                    OP_PUSH_INT -> { stack.addLast(value); p++ }
                    OP_GET_LOCAL -> { stack.addLast(locals[value] ?: 0); p++ }
                    OP_SET_LOCAL -> { locals[value] = stack.removeLast(); p++ }
                    OP_GET_VARBIT -> { stack.addLast(if (value == VARBIT_MEMBERS) membership else 0); p++ }
                    OP_CALL -> { stack.addLast(CLIENT_CAPABILITIES[value] ?: 0); p++ }
                    OP_STRUCT_PARAM -> {
                        // (struct, param id) -> value. Only the gate under test is present.
                        val param = stack.removeLast()
                        stack.removeLast()
                        stack.addLast(if (param == gate) 1 else 0)
                        p++
                    }
                    OP_JUMP -> p += 1 + value
                    in BRANCHES -> {
                        val b = stack.removeLast()
                        val a = stack.removeLast()
                        val taken =
                            when (op) {
                                OP_IF_NE -> a != b
                                OP_IF_EQ -> a == b
                                OP_IF_LT -> a < b
                                OP_IF_GT -> a > b
                                OP_IF_LE -> a <= b
                                else -> a >= b
                            }
                        p += if (taken) 1 + value else 1
                    }
                    OP_RETURN -> return locals[1] == 1
                    // 3955 uses nothing else; anything unexpected is skipped rather than guessed at.
                    else -> p++
                }
            }
            return locals[1] == 1
        }

        companion object {
            private const val OP_GET_VARP = 1
            private const val OP_SET_VARP = 2
            private const val OP_PUSH_STRING = 3
            private const val OP_RETURN = 21
            private const val OP_GET_VARBIT = 25
            private const val OP_SET_VARBIT = 27
            private const val OP_POP_INT = 38
            private const val OP_POP_STRING = 39
            private const val OP_SWITCH = 60

            /** Opcodes the 3955 interpreter above understands. */
            private const val OP_PUSH_INT = 0
            private const val OP_JUMP = 6
            private const val OP_IF_NE = 7
            private const val OP_IF_EQ = 8
            private const val OP_IF_LT = 9
            private const val OP_IF_GT = 10
            private const val OP_IF_LE = 31
            private const val OP_IF_GE = 32
            private const val OP_GET_LOCAL = 33
            private const val OP_SET_LOCAL = 34
            private const val OP_CALL = 40
            private const val OP_STRUCT_PARAM = 6516

            private val BRANCHES = setOf(OP_IF_NE, OP_IF_EQ, OP_IF_LT, OP_IF_GT, OP_IF_LE, OP_IF_GE)

            private const val VARBIT_MEMBERS = 1777

            /** 3955 has no loops; the cap only stops a malformed script spinning. */
            private const val STEP_LIMIT = 50_000

            /** numOps(4) + localInt(2) + localStr(2) + intArg(2) + strArg(2). */
            private const val TRAILER_BYTES = 12

            fun load(scriptId: Int): ClientScript {
                val data =
                    CacheManager.cache.data(CLIENTSCRIPT, scriptId, 0)
                        ?: error("Clientscript $scriptId is missing from the cache.")

                val switchBlockLength = readUnsignedShort(data, data.size - 2)
                val trailer = data.size - 2 - switchBlockLength - TRAILER_BYTES

                var p = trailer + TRAILER_BYTES
                val switchCount = data[p++].toInt() and 0xFF
                val switches = mutableListOf<Map<Int, Int>>()
                repeat(switchCount) {
                    val cases = readUnsignedShort(data, p)
                    p += 2
                    val table = mutableMapOf<Int, Int>()
                    repeat(cases) {
                        table[readInt(data, p)] = readInt(data, p + 4)
                        p += 8
                    }
                    switches += table
                }

                // The script's own name is an optional leading string; a single 0 byte means unnamed.
                var op = if (data[0].toInt() == 0) 1 else skipString(data, 0)
                val ops = mutableListOf<Pair<Int, Int>>()
                while (op < trailer) {
                    val opcode = readUnsignedShort(data, op)
                    op += 2
                    val operand: Int
                    when {
                        opcode == OP_PUSH_STRING -> {
                            operand = 0
                            op = skipString(data, op)
                        }
                        opcode < 100 && opcode != OP_RETURN && opcode != OP_POP_INT && opcode != OP_POP_STRING -> {
                            operand = readInt(data, op)
                            op += 4
                        }
                        else -> {
                            operand = data[op].toInt() and 0xFF
                            op += 1
                        }
                    }
                    ops += opcode to operand
                }

                // Switch tables appear in the same order as the switch ops that use them.
                val switchOps = ops.indices.filter { ops[it].first == OP_SWITCH }
                return ClientScript(ops, switchOps.zip(switches))
            }

            private fun readUnsignedShort(
                data: ByteArray,
                at: Int,
            ): Int = ((data[at].toInt() and 0xFF) shl 8) or (data[at + 1].toInt() and 0xFF)

            private fun readInt(
                data: ByteArray,
                at: Int,
            ): Int =
                ((data[at].toInt() and 0xFF) shl 24) or
                    ((data[at + 1].toInt() and 0xFF) shl 16) or
                    ((data[at + 2].toInt() and 0xFF) shl 8) or
                    (data[at + 3].toInt() and 0xFF)

            private fun skipString(
                data: ByteArray,
                from: Int,
            ): Int {
                var at = from
                while (data[at].toInt() != 0) {
                    at++
                }
                return at + 1
            }
        }
    }
}
