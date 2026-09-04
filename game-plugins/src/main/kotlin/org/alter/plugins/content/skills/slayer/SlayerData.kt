package org.alter.plugins.content.skills.slayer

/**
 * One Slayer task category, loaded from `data/cfg/slayer/tasks.json`.
 *
 * A task is a *category*, not an npc - "Spiders" covers the plain spider, the giant spider and the
 * poison spider alike. [monsters] therefore holds cache npc **names**, not rscm keys, and
 * [SlayerService] resolves each name to every npc id in the cache carrying it. Names rather than
 * ids for three reasons:
 *
 * - it is how the wiki publishes the data ("Giant spider ... counts towards Spiders"), so the JSON
 *   can be checked against the source line by line;
 * - a single monster routinely has a dozen ids in the cache (twenty for ghosts, thirty for
 *   goblins), and copying those lists into this file would duplicate what
 *   `content/npcs/critters`, `.../dungeon` and `.../goblin` already declare;
 * - it means a task starts working the moment its monster is built, with no edit here.
 *
 * [assignmentId] is the id the *client* uses for this category - the key into cache enum 693,
 * whose values read "Goblins", "Rats", "Abyssal Demons" and so on. Writing it to
 * [org.alter.api.cfg.Varp.SLAYER_CURRENT_ASSIGNMENT_TYPE] is what makes the task show up in the
 * player's own interface rather than only in chat, so these ids are dumped from this cache rather
 * than guessed.
 *
 * **Most entries in `tasks.json` deliberately have an empty [monsters] list.** The full assignment
 * tables of all six masters are transcribed, which means the file names every category those
 * masters can roll - roughly ninety of them - while this server has actually built about twenty
 * monsters. A task with no resolvable npc is switched off at load ([available] stays false) and is
 * never assigned; the startup log names them. Filling in a monster list is the whole job of turning
 * one back on.
 */
data class SlayerTaskEntry(
    val name: String,
    val assignmentId: Int,
    private val slayerLevel: Int = 1,
    val monsters: List<String>,
) {
    /**
     * The Slayer level this task needs, and the reason [slayerLevel] is private.
     *
     * Gson does not call Kotlin constructors - it allocates the object directly and writes fields -
     * so a `= 1` default never runs and a task whose JSON omits `slayerLevel` arrives holding 0, not
     * 1. Every other data class in this codebase has the same hole; it only bites here because 0 is
     * an invalid level rather than a harmless one. Reading the level through this property is what
     * makes the default real.
     */
    val requiredLevel: Int get() = if (slayerLevel < 1) 1 else slayerLevel

    /**
     * Every cache npc id whose name matches one of [monsters], resolved once at world init.
     *
     * Kept as a sorted [IntArray] and searched with a binary search: this is read on every hit
     * landed by every player through the combat formulas' black mask check, so it is on a hot path.
     */
    var npcIds: IntArray = IntArray(0)

    /** Whether this task resolved to at least one npc that actually spawns in this world. */
    var available: Boolean = false

    fun matches(npcId: Int): Boolean = npcIds.isNotEmpty() && npcIds.binarySearch(npcId) >= 0
}

/**
 * One row of a Slayer master's assignment table - the weight it is rolled at, how many kills it
 * asks for, and the combat level and reward unlock it is gated behind.
 *
 * [weight] is the raw wiki weight, not a percentage: a master picks a task by rolling against the
 * summed weights of everything the player currently qualifies for, so removing an entry (a blocked
 * task, or one whose monster does not exist here) correctly redistributes its share over the rest.
 *
 * The Slayer *level* requirement is not here - it belongs to the monster, is identical across every
 * master, and lives on [SlayerTaskEntry.requiredLevel]. The combat level requirement genuinely does
 * vary by master, so it does live here.
 */
data class SlayerAssignmentEntry(
    val task: String,
    val weight: Int,
    val min: Int,
    val max: Int,
    val combatLevel: Int = 0,
    val unlock: String? = null,
) {
    /** Resolved from [task] at load; a row naming an unknown task is a hard load error. */
    @Transient
    lateinit var entry: SlayerTaskEntry
}

/**
 * One Slayer master, loaded from `data/cfg/slayer/masters.json`.
 *
 * [masterId] is the client's id for this master (cache enum 1702), stored in
 * [org.alter.api.cfg.Varbit.SLAYER_CURRENT_MASTER] so the interface names whoever assigned the
 * current task.
 *
 * [npcs] is a list rather than a single key because several masters have more than one npc id in
 * the cache and only one of them carries the real `Assignment`/`Rewards` options - the interactive
 * id is the one configured here, and the others are combat or cutscene variants.
 *
 * [x]/[z]/[height] is where this plugin spawns the master, taken from the master's own wiki page.
 * None of the six were spawned by any existing plugin.
 */
data class SlayerMasterEntry(
    val name: String,
    val npcs: List<String>,
    val masterId: Int,
    val combatRequirement: Int,
    val slayerRequirement: Int,
    val pointsPerTask: Int,
    val x: Int,
    val z: Int,
    val height: Int,
    val assignments: List<SlayerAssignmentEntry>,
) {
    /** Resolved npc ids for [npcs]. */
    @Transient
    var npcIds: IntArray = IntArray(0)

    /**
     * Turael is the only master who hands out tasks for free and the only one whose tasks reset a
     * streak, so rather than a flag in the JSON it is derived from the one thing that actually
     * makes him different: he pays nothing.
     */
    val resetsStreak: Boolean get() = pointsPerTask == 0
}
