package org.alter.plugins.content.areas.duelarena

/**
 * Where a [DuelSession] has got to.
 *
 * The three screens are ordered the way the player meets them: stake first, then rules, then the
 * summary of both. Acceptance is reset on entering each one, so agreeing to a stake is never also
 * agreeing to the rules that follow it.
 */
enum class DuelStage(val screen: Int?) {
    /** Offering items and coins, on the trade interfaces. */
    STAKE(screen = 335),

    /** Choosing rules and locked equipment slots, on interface 755. */
    OPTIONS(screen = 755),

    /** Reading back both of the above, on interface 756. */
    CONFIRM(screen = 756),

    /** Teleported into an arena, locked, counting down. */
    COUNTDOWN(screen = null),

    /** Fighting. Rules are enforced only in this stage. */
    FIGHTING(screen = null),

    /** Over - won, forfeited or abandoned. A session in this stage is inert. */
    ENDED(screen = null),
    ;

    /**
     * Whether closing [interfaceId] means the player walked away from the duel.
     *
     * Moving between the three screens closes the previous one, so a handler that treated every
     * close as a decline would call the duel off the instant both players agreed the stake. The
     * stage is advanced before the new screen is opened, so during a transition the interface
     * going away is never the one the duel is now sitting on - which is exactly what separates
     * "moved on" from "gave up".
     */
    fun isAbandonedBy(interfaceId: Int): Boolean = screen != null && screen == interfaceId
}
