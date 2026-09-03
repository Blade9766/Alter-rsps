package org.alter.game.model

import org.alter.game.model.timer.TimerKey
import org.alter.game.model.timer.TimerMap
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @author Tom <rspsmods@gmail.com>
 */
class TimerTests {
    @Test
    fun persistenceTests() {
        val timers = TimerMap()

        val key1 = TimerKey(persistenceKey = "persistent", tickOffline = true)
        val key2 = TimerKey(persistenceKey = "persistent", tickOffline = true)
        val key3 = TimerKey(persistenceKey = "persistent", tickOffline = false)
        val key4 = TimerKey()

        timers[key1] = 4

        assertTrue(timers.exists(key1))
        assertTrue(timers.exists(key2))
        assertFalse(timers.exists(key3))
        assertFalse(timers.exists(key4))
    }

    @Test
    fun uniqueTests() {
        val timers = TimerMap()

        val key1 = TimerKey()
        val key2 = TimerKey()

        timers[key1] = 4

        assertTrue(timers.exists(key1))
        assertFalse(timers.exists(key2))

        timers[key2] = 6

        assertTrue(timers.exists(key1))
        assertTrue(timers.exists(key2))
    }

    /**
     * A timer callback must be free to start or cancel timers on the same pawn.
     *
     * `Pawn.timerCycle` used to walk the map with a live iterator while invoking each callback from
     * inside the loop, so a callback that armed a new timer - a countdown arming the thing it was
     * counting down to - raised a ConcurrentModificationException. It escaped the try around the
     * callback and took down the entire PlayerCycleTask for that cycle. This reproduces the shape of
     * that loop over TimerMap directly.
     */
    @Test
    fun timerCallbackMayAddAndRemoveTimers() {
        val timers = TimerMap()
        val expired = TimerKey()
        val other = TimerKey()
        val added = TimerKey()

        timers[expired] = 0
        timers[other] = 5

        // The same snapshot-then-walk that Pawn.timerCycle does.
        for (key in timers.getTimers().keys.toList()) {
            val time = timers.getTimers()[key] ?: continue
            if (time > 0) continue
            if (key == expired) {
                timers[added] = 3
                timers.remove(other)
            }
        }

        assertTrue(timers.exists(added), "a callback must be able to arm a new timer")
        assertFalse(timers.exists(other), "a callback must be able to cancel another timer")
    }
}
