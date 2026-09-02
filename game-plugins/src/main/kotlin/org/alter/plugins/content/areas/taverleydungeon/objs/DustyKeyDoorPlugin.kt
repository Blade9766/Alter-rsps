package org.alter.plugins.content.areas.taverleydungeon.objs

import org.alter.api.cfg.Sound
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.rscm.RSCM.getRSCM

/**
 * The dusty key door in Taverley Dungeon - the metal door the wiki describes as barring
 * "access to the remaining half of the dungeon, which can only be opened with the dusty
 * key".
 *
 * This matters now in a way it did not before: `areas/taverleydungeon/spawns` puts black
 * demons (172), hellhounds (122), poison spiders and lesser demons in the deep half, all
 * aggressive and all with real stats from `content/npcs/dungeon`. Without a gate, the only
 * thing between a level 3 account and a black demon is a walk.
 *
 * **Identifying the door was the whole job here, and the cache settled it rather than the
 * wiki.** There is no wiki page for the object, but `data/cfg/locs.csv` has exactly one
 * entry in the entire cache examining as "This door requires a key." - id **1804** - which
 * is as unambiguous as identification gets.
 *
 * **Two traps in that neighbourhood, both avoided:**
 * 1. [org.alter.api.ext.openDoor] defaults its opened state to `obj.id + 1`. For this door
 *    that is **1805, which the cache examines as "The door to the Champions' Guild"** - a
 *    different door entirely, not this one's open state. The opened id is therefore passed
 *    explicitly. Taking the default would have swapped a Taverley door for a Varrock one.
 * 2. [org.alter.api.ext.closeDoor] mirrors that with `obj.id - 1`, so the close passes its
 *    id explicitly too.
 *
 * **The opened state is an inference, and the one thing here worth re-checking.** Object
 * 1803 is `null_1803`: unnamed, no examine, immediately below the door. That is the shape
 * an opened-door placeholder takes in this cache - `content/areas/lumbridge/objs/AlkharidGate`
 * opens its gates onto `null_1573`/`null_1574` the same way. If it turns out to be wrong the
 * fix is [OPENED_DOOR] alone; the gating logic does not depend on it.
 *
 * The key is **not consumed** - a dusty key is reusable in OSRS - and the door **closes
 * itself again** after [OPEN_CYCLES]. That second part is a deliberate departure from a real
 * door, which stays open until someone shuts it: objects here are world state, not
 * per-player, so a door left open would be open for everyone and the gate would stop
 * gating the moment one keyholder walked through.
 *
 * **Not implemented: the three Agility shortcuts that bypass or supplement this door** - the
 * 70 Agility obstacle pipe between the entrance and the blue dragons (which skips the door
 * entirely), the 63 Agility loose railing between the magic axes and lesser demons (which
 * still needs the key), and the 80 Agility strange floor to the poison spiders. Each is its
 * own object with its own skill check, and none exists here yet. Their absence makes the
 * deep half slightly harder to reach than in OSRS, not easier, which is the safe direction
 * to be wrong in.
 */
class DustyKeyDoorPlugin(
    r: PluginRepository,
    world: World,
    server: Server,
) : KotlinPlugin(r, world, server) {
    init {
        onObjOption(obj = CLOSED_DOOR, option = "open", lineOfSightDistance = 1) {
            val door = player.getInteractingGameObj()
            if (player.inventory.getItemCount(getRSCM(DUSTY_KEY)) < 1) {
                player.message("This door is locked.")
            } else {
                player.message("You unlock the door with your dusty key.")
                world.queue {
                    val opened = world.openDoor(door, OPENED_DOOR)
                    player.playSound(Sound.OPEN_DOOR_SFX)
                    wait(OPEN_CYCLES)
                    world.closeDoor(opened, CLOSED_DOOR)
                }
            }
        }
    }

    private companion object {
        /** The only object in this cache examining as "This door requires a key." */
        const val CLOSED_DOOR = "object.door_1804"

        /** Inferred - see this class's doc comment. */
        const val OPENED_DOOR = "object.null_1803"

        const val DUSTY_KEY = "item.dusty_key"

        /** Roughly 6 seconds - long enough to walk through, short enough to stay a gate. */
        const val OPEN_CYCLES = 10
    }
}
