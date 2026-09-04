package org.alter.tools

import org.alter.game.Server
import org.alter.game.model.entity.Client
import org.alter.game.saving.impl.VarpSerialisation
import org.bson.Document
import org.bson.json.JsonWriterSettings
import java.nio.file.Paths

/**
 * TEMPORARY diagnostic - proves varps survive a save and a reload.
 *
 * The old layout wrote `state -> varp id`, so varps sharing a state overwrote each other and all
 * but one was silently dropped. The first check below is exactly that case; it fails against the
 * old layout and passes against the current one. The rest cover the JSON round trip the Json save
 * format actually performs, and reading a document written in the old layout.
 *
 * Usage: gradlew :game-server:varpSaveDiag
 */
object VarpSaveDiag {

    /**
     * Deliberately collision-heavy: three varps on state 1, two on 2. Also the real makeover vars -
     * varp 387 (pronouns, 0..2) and varp 1797 (body type in bit 2, so state 4).
     */
    private val SAMPLE =
        mapOf(
            173 to 1,
            180 to 1,
            387 to 1,
            904 to 2,
            1075 to 2,
            1797 to 4,
            3417 to 100663296,
        )

    /** A document in the pre-fix layout, taken from a real save. */
    private val LEGACY_DOCUMENT =
        Document(
            mapOf(
                "1" to "173",
                "2" to "180",
                "1000" to "300",
                "-1" to "664",
                "60817531" to "904",
                "430" to "1075",
                "-2147483648" to "1737",
            ),
        )

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

        var failures = 0
        fun check(
            label: String,
            ok: Boolean,
        ) {
            println((if (ok) "  ok   " else "  FAIL ") + label)
            if (!ok) failures++
        }

        val serialisation = VarpSerialisation()
        val jsonSettings = JsonWriterSettings.builder().indent(true).build()

        fun client(): Client =
            Client(world).apply {
                loginUsername = "VarpSaveDiag"
            }

        fun readBack(doc: Document): Map<Int, Int> {
            val loaded = client()
            serialisation.fromDocument(loaded, doc)
            return loaded.varps.getAll().filter { it.state != 0 }.associate { it.id to it.state }
        }

        val saved = client().apply { SAMPLE.forEach { (id, state) -> varps.setState(id, state) } }
        val document = serialisation.asDocument(saved)

        println()
        println("=== Round trip ===")
        // What the old layout did, so this file says out loud that the sample can tell them apart.
        val oldLayoutEntries = SAMPLE.entries.associate { it.value to it.key }.size
        check(
            "the old layout would have lost ${SAMPLE.size - oldLayoutEntries} of ${SAMPLE.size}",
            oldLayoutEntries < SAMPLE.size,
        )
        check("saved every varp, got ${document.size - 1} of ${SAMPLE.size}", document.size - 1 == SAMPLE.size)
        check("restores identically", readBack(document) == SAMPLE)

        println()
        println("=== Round trip through the JSON save format ===")
        val reparsed = Document.parse(document.toJson(jsonSettings))
        check("restores identically", readBack(reparsed) == SAMPLE)

        println()
        println("=== Documents written before the fix ===")
        // The old reader took the key as the state and the value as the varp id.
        val expectedLegacy =
            mapOf(173 to 1, 180 to 2, 300 to 1000, 664 to -1, 904 to 60817531, 1075 to 430, 1737 to Int.MIN_VALUE)
        check("read the old way round, got ${readBack(LEGACY_DOCUMENT)}", readBack(LEGACY_DOCUMENT) == expectedLegacy)

        println()
        println(if (failures == 0) "All checks passed." else "$failures check(s) failed.")
        Runtime.getRuntime().halt(if (failures == 0) 0 else 1)
    }
}
