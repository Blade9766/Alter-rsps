package org.alter.plugins.content.mechanics.music

import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
/**
 * The real OSRS music tab (interface 239). Component 3 is the "JUKEBOX" track list,
 * populated from [MusicTracks].
 *
 * [MusicTracks] now reads the client's own track table out of the cache (DBTable 44)
 * and orders it the way the client does, so a clicked row resolves to the track
 * actually shown in it. That replaces the previous best-effort list, which was built
 * from an incomplete JSON file in an unverifiable order and was the reason clicks
 * used to land on the wrong track - see [MusicTracks]' doc for the full story and the
 * one caveat that remains (this table yields 811 tracks; the client had been seen
 * showing 795).
 *
 * Clicking a locked row just explains why instead of playing it, since there's no
 * confirmed way to grey out individual rows client-side without more
 * interface-scripting knowledge than is available here.
 *
 * STILL A DIAGNOSTIC BUILD: it was never pinned down which component actually
 * receives row clicks, so this binds click ops across several candidates
 * (3/JUKEBOX, 4/SCROLLABLE, 6/TRACK) and reports which one fires and with what slot.
 * Once in-game testing confirms one, delete the other two and the debug message.
 */
class MusicTabPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onLogin {
            CANDIDATE_COMPONENTS.forEach { component ->
                player.setInterfaceEvents(
                    interfaceId = MUSIC_INTERFACE_ID,
                    component = component,
                    range = 0 until MusicTracks.all.size,
                    setting =
                        arrayOf(
                            InterfaceEvent.ClickOp1,
                            InterfaceEvent.ClickOp2,
                            InterfaceEvent.ClickOp3,
                        ),
                )
            }
        }

        CANDIDATE_COMPONENTS.forEach { component ->
            onButton(interfaceId = MUSIC_INTERFACE_ID, component = component) {
                val slot = player.getInteractingSlot()
                player.message("[music debug] component $component fired, slot=$slot")
                handleTrackClick(player, slot)
            }
        }
    }

    private fun handleTrackClick(
        player: Player,
        slot: Int,
    ) {
        val track = MusicTracks.all.getOrNull(slot) ?: return

        if (!MusicUnlocks.isUnlocked(player, track.id)) {
            player.message("You haven't unlocked this track yet - visit its area in-game to unlock it.")
            return
        }

        player.playSong(track.id)
    }

    private companion object {
        const val MUSIC_INTERFACE_ID = 239
        val CANDIDATE_COMPONENTS = listOf(3, 4, 6)
    }
}
