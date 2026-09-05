package org.alter.game.model.interf

import org.alter.game.model.interf.listener.InterfaceListener
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [InterfaceSet.currentModal] has to name an interface that is actually visible.
 *
 * It is read as "does this player have a screen up" - by `OSRSPlugin`'s modal-close logic, which
 * runs on very nearly every click, and by `Combat` when a player is attacked with a screen open.
 * Nothing re-checks it against [InterfaceSet.visible], so once the two disagree the modal pointer
 * is simply believed.
 *
 * The disagreement is reachable through the ordinary trade flow: the accept screen is opened as a
 * modal, then closed by destination - `Player.closeInterface(InterfaceDestination.MAIN_SCREEN)`,
 * which routes to [InterfaceSet.close] with a parent *and* child and so never looks at the modal
 * pointer. That is what put "Interface 334 is not visible and cannot be closed." in the log.
 */
class InterfaceSetTests {
    private object SilentListener : InterfaceListener {
        override fun onInterfaceOpen(interfaceId: Int) = Unit

        override fun onInterfaceClose(interfaceId: Int) = Unit
    }

    private companion object {
        /** The pane the main screen is drawn on. Any pair works; these are only map keys. */
        const val PARENT = 161
        const val CHILD = 13

        /** The trade accept screen, the interface the live warning actually named. */
        const val MODAL = 334
    }

    /**
     * Closing a modal by its pane - the destination overload's path - has to clear the modal
     * pointer, not just the visible entry.
     */
    @Test
    fun `closing a modal by pane clears the modal pointer`() {
        val set = InterfaceSet(SilentListener)
        set.openModal(PARENT, CHILD, MODAL)
        assertEquals(MODAL, set.getModal(), "the modal did not register as open")

        set.close(PARENT, CHILD)

        assertEquals(
            -1,
            set.getModal(),
            "The modal is no longer visible but is still the current modal, so the next " +
                "closeInterfaceModal() tries to close an interface that is not there.",
        )
    }

    /**
     * Replacing a modal with a plain interface on the same pane has to clear it too. The
     * replacement happens inside [InterfaceSet.open], which closes whatever was on the pane first.
     */
    @Test
    fun `replacing a modal with a non-modal interface clears the modal pointer`() {
        val set = InterfaceSet(SilentListener)
        set.openModal(PARENT, CHILD, MODAL)

        set.open(PARENT, CHILD, 335)

        assertEquals(-1, set.getModal(), "the replaced modal is still the current modal")
    }

    /** Opening one modal over another leaves the new one current, not -1. */
    @Test
    fun `replacing a modal with another modal makes the new one current`() {
        val set = InterfaceSet(SilentListener)
        set.openModal(PARENT, CHILD, MODAL)

        set.openModal(PARENT, CHILD, 335)

        assertEquals(335, set.getModal(), "the new modal did not become current")
    }

    /** Closing something else must not disturb the modal. */
    @Test
    fun `closing an unrelated interface leaves the modal alone`() {
        val set = InterfaceSet(SilentListener)
        set.openModal(PARENT, CHILD, MODAL)
        set.open(PARENT, CHILD + 1, 336)

        set.close(PARENT, CHILD + 1)

        assertEquals(MODAL, set.getModal(), "an unrelated close cleared the modal")
    }
}
