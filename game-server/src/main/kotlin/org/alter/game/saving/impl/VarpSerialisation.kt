package org.alter.game.saving.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import org.alter.game.model.entity.Client
import org.alter.game.saving.DocumentHandler
import org.bson.Document

/**
 * Saves and restores a player's varps.
 *
 * Documents are `varp id -> state`, and carry [FORMAT_KEY] to say so.
 *
 * They used to be written the other way round, `state -> varp id`, which silently lost data: a
 * document's keys are unique, so every varp sharing a state collapsed into a single entry and only
 * one of them survived the save. That is not a rare collision - most varps hold small numbers, so
 * states like 1 and 2 were routinely shared by several varps and all but one was dropped on every
 * save.
 *
 * A document without [FORMAT_KEY] was written before that fix and is read the old way round, so
 * existing saves still load - carrying whatever they had already lost. The first save after that
 * rewrites them in the current layout.
 */
class VarpSerialisation(override val name: String = "varps") : DocumentHandler {

    override fun fromDocument(
        client: Client,
        doc: Document,
    ) {
        val idKeyed = (doc[FORMAT_KEY].asInt() ?: 0) >= FORMAT_ID_KEYED
        doc.forEach { key, value ->
            if (key == FORMAT_KEY) {
                return@forEach
            }
            val left = key.asInt() ?: return@forEach
            val right = value.asInt() ?: return@forEach
            val id = if (idKeyed) left else right
            val state = if (idKeyed) right else left
            if (id !in 0 until client.varps.maxVarps) {
                // Reachable from a save written against a cache with more varps than this one, and
                // from an old-layout document whose state happened to be larger than any varp id.
                logger.warn { "Skipping out of range varp $id for ${client.loginUsername}." }
                return@forEach
            }
            client.varps.setState(id, state)
        }
    }

    override fun asDocument(client: Client): Document =
        Document().apply {
            append(FORMAT_KEY, FORMAT_ID_KEYED)
            client.varps
                .getAll()
                .filter { it.state != 0 }
                .forEach { append(it.id.toString(), it.state) }
        }

    private companion object {
        private val logger = KotlinLogging.logger {}

        /**
         * Not a number, so the pre-fix reader skips it rather than restoring it as a varp.
         */
        const val FORMAT_KEY = "format"

        /** Documents at this version or later are keyed by varp id. */
        const val FORMAT_ID_KEYED = 2

        /**
         * Values have been written as both strings and numbers over the life of this file, and a
         * document that has been through a JSON round trip can hand back a whole number as a
         * double.
         */
        fun Any?.asInt(): Int? =
            when (this) {
                null -> null
                is Number -> toInt()
                else -> toString().toIntOrNull() ?: toString().toDoubleOrNull()?.toInt()
            }
    }
}
