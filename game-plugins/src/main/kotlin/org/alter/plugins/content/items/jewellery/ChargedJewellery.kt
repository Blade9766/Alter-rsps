package org.alter.plugins.content.items.jewellery

import org.alter.api.EquipmentType
import org.alter.game.model.Tile

/**
 * One destination on a piece of charged jewellery.
 *
 * [option] is the exact string the cache puts in that item's worn-equipment menu, which is what
 * `onEquipmentOption` matches on and what the "rub" dialogue lists.
 *
 * A null [tile] means "the player's respawn point", resolved when the teleport happens rather than
 * baked in. The ring of returning is the only item that works that way, and it is also why its one
 * option is literally named "Rub" - the cache gives it no destination options at all.
 */
data class JewelleryDestination(val option: String, val tile: Tile?)

/**
 * Every piece of charged teleport jewellery, its charge ladder, and where each of its options goes.
 *
 * **Destination names are not invented here.** Each [JewelleryDestination.option] is read off the
 * item's own cache worn-menu (item params 451-458), so this is really a table of tiles keyed by
 * options the client already draws. `JewelleryVerify` asserts every one of them still exists on
 * every charge of every item, which is what catches a cache revision renaming or moving an option.
 * Note that the cache, not the wiki, is the authority on the *wording*: the skills necklace's fourth
 * option is "Cooking Guild" here because that is what the cache says, even though the wiki calls the
 * place the Cooks' Guild.
 *
 * **Tiles** come from the `{{TeleportLocationLine}}` coordinates in each item's wiki article and
 * were then checked against this project's own collision map (`JewelleryVerify`) before being
 * written down.
 *
 * **Unlock gates are deliberately absent.** Several destinations are quest-locked in OSRS
 * (Miscellania, Dondakan, Fossil Island, Lithkren, Fortis Colosseum, Tears of Guthix, Wintertodt).
 * None of those quests exist in this project, so gating them would only make the option permanently
 * unreachable - the same call [org.alter.plugins.content.items.skillcapes.DefenceCapePlugin] already
 * made for the Ardougne diary.
 *
 * @see <a href="https://oldschool.runescape.wiki/w/Jewellery">Jewellery - OSRS Wiki</a>
 */
enum class ChargedJewellery(
    val displayName: String,
    val noun: String,
    val slot: EquipmentType,
    /** Charge ladder, lowest first: index `n` is the item with `n + 1` charges left. */
    val chargeItems: List<String>,
    /** The item left behind at zero charges, or null if the piece is destroyed instead. */
    val uncharged: String?,
    val depletedMessage: String,
    /** "charges" for the rechargeable dragonstone four, "uses" for the pieces that crumble. */
    val chargeNoun: String,
    val destinations: List<JewelleryDestination>,
) {
    AMULET_OF_GLORY(
        displayName = "amulet of glory",
        noun = "amulet",
        slot = EquipmentType.AMULET,
        chargeItems =
            listOf(
                "item.amulet_of_glory1",
                "item.amulet_of_glory2",
                "item.amulet_of_glory3",
                "item.amulet_of_glory4",
                "item.amulet_of_glory5",
                "item.amulet_of_glory6",
            ),
        uncharged = "item.amulet_of_glory",
        depletedMessage = "Your amulet has run out of charges.",
        chargeNoun = "charges",
        destinations =
            listOf(
                JewelleryDestination("Edgeville", Tile(3087, 3496)),
                JewelleryDestination("Karamja", Tile(2918, 3176)),
                JewelleryDestination("Draynor Village", Tile(3105, 3251)),
                JewelleryDestination("Al Kharid", Tile(3293, 3163)),
            ),
    ),

    /**
     * The trimmed glory is a full parallel ladder in the cache - same slot, same four destinations,
     * same "Rub" on the uncharged one - and is included because it is the one cosmetic variant
     * players actually carry. The imbued ring of wealth below is here for the same reason. The
     * remaining variants are deliberately absent: the ornamented fury, torture and anguish and their
     * like carry no charge ladder of their own.
     */
    AMULET_OF_GLORY_TRIMMED(
        displayName = "amulet of glory",
        noun = "amulet",
        slot = EquipmentType.AMULET,
        chargeItems =
            listOf(
                "item.amulet_of_glory_t1",
                "item.amulet_of_glory_t2",
                "item.amulet_of_glory_t3",
                "item.amulet_of_glory_t4",
                "item.amulet_of_glory_t5",
                "item.amulet_of_glory_t6",
            ),
        uncharged = "item.amulet_of_glory_t",
        depletedMessage = "Your amulet has run out of charges.",
        chargeNoun = "charges",
        destinations =
            listOf(
                JewelleryDestination("Edgeville", Tile(3087, 3496)),
                JewelleryDestination("Karamja", Tile(2918, 3176)),
                JewelleryDestination("Draynor Village", Tile(3105, 3251)),
                JewelleryDestination("Al Kharid", Tile(3293, 3163)),
            ),
    ),

    SKILLS_NECKLACE(
        displayName = "skills necklace",
        noun = "necklace",
        slot = EquipmentType.AMULET,
        chargeItems =
            listOf(
                "item.skills_necklace1",
                "item.skills_necklace2",
                "item.skills_necklace3",
                "item.skills_necklace4",
                "item.skills_necklace5",
                "item.skills_necklace6",
            ),
        uncharged = "item.skills_necklace",
        depletedMessage = "Your necklace has run out of charges.",
        chargeNoun = "charges",
        destinations =
            listOf(
                JewelleryDestination("Fishing Guild", Tile(2611, 3390)),
                JewelleryDestination("Mining Guild", Tile(3049, 9763)),
                JewelleryDestination("Crafting Guild", Tile(2933, 3295)),
                JewelleryDestination("Cooking Guild", Tile(3144, 3438)),
                JewelleryDestination("Woodcutting Guild", Tile(1662, 3505)),
                // OSRS lands outside the guild below 45 Farming and inside above it. There is no
                // Farming skill here to branch on, so the wiki's single published pin is used.
                JewelleryDestination("Farming Guild", Tile(1248, 3719)),
            ),
    ),

    COMBAT_BRACELET(
        displayName = "combat bracelet",
        noun = "bracelet",
        slot = EquipmentType.GLOVES,
        chargeItems =
            listOf(
                "item.combat_bracelet1",
                "item.combat_bracelet2",
                "item.combat_bracelet3",
                "item.combat_bracelet4",
                "item.combat_bracelet5",
                "item.combat_bracelet6",
            ),
        uncharged = "item.combat_bracelet",
        depletedMessage = "Your bracelet has run out of charges.",
        chargeNoun = "charges",
        destinations =
            listOf(
                JewelleryDestination("Warriors' Guild", Tile(2882, 3547)),
                JewelleryDestination("Champions' Guild", Tile(3192, 3368)),
                JewelleryDestination("Monastery", Tile(3052, 3490)),
                JewelleryDestination("Ranging Guild", Tile(2653, 3439)),
            ),
    ),

    /**
     * The ring's worn menu also carries "Boss Log" and "Coin Collection". "Coin Collection" is a real
     * toggle and [RingOfWealthPlugin] binds it; "Boss Log" opens a kill log this project has no
     * interface for and is left unbound rather than wired to a placeholder that pretends otherwise.
     */
    RING_OF_WEALTH(
        displayName = "ring of wealth",
        noun = "ring",
        slot = EquipmentType.RING,
        chargeItems =
            listOf(
                "item.ring_of_wealth_1",
                "item.ring_of_wealth_2",
                "item.ring_of_wealth_3",
                "item.ring_of_wealth_4",
                "item.ring_of_wealth_5",
            ),
        uncharged = "item.ring_of_wealth",
        depletedMessage = "Your ring has run out of charges.",
        chargeNoun = "charges",
        destinations =
            listOf(
                JewelleryDestination("Miscellania", Tile(2534, 3862)),
                JewelleryDestination("Grand Exchange", Tile(3163, 3478)),
                // The cache option is "Falador"; the wiki pin is Falador Park, inside the city.
                JewelleryDestination("Falador", Tile(2995, 3375)),
                JewelleryDestination("Dondakan", Tile(2824, 10168)),
            ),
    ),

    /**
     * The imbued ring, from a ring of wealth scroll. A full parallel ladder in the cache, the same
     * way the trimmed glory is - same slot, same four destinations, same "Rub" on the uncharged one -
     * so its teleports work like the plain ring's. Its *own* perk, double clue scrolls in the
     * Wilderness, is not modelled: there are no clue scrolls here.
     */
    RING_OF_WEALTH_IMBUED(
        displayName = "ring of wealth",
        noun = "ring",
        slot = EquipmentType.RING,
        chargeItems =
            listOf(
                "item.ring_of_wealth_i1",
                "item.ring_of_wealth_i2",
                "item.ring_of_wealth_i3",
                "item.ring_of_wealth_i4",
                "item.ring_of_wealth_i5",
            ),
        uncharged = "item.ring_of_wealth_i",
        depletedMessage = "Your ring has run out of charges.",
        chargeNoun = "charges",
        destinations =
            listOf(
                JewelleryDestination("Miscellania", Tile(2534, 3862)),
                JewelleryDestination("Grand Exchange", Tile(3163, 3478)),
                JewelleryDestination("Falador", Tile(2995, 3375)),
                JewelleryDestination("Dondakan", Tile(2824, 10168)),
            ),
    ),

    RING_OF_DUELING(
        displayName = "ring of dueling",
        noun = "ring",
        slot = EquipmentType.RING,
        chargeItems =
            listOf(
                "item.ring_of_dueling1",
                "item.ring_of_dueling2",
                "item.ring_of_dueling3",
                "item.ring_of_dueling4",
                "item.ring_of_dueling5",
                "item.ring_of_dueling6",
                "item.ring_of_dueling7",
                "item.ring_of_dueling8",
            ),
        uncharged = null,
        depletedMessage = "Your ring of dueling crumbles to dust.",
        chargeNoun = "uses",
        destinations =
            listOf(
                JewelleryDestination("Emir's Arena", Tile(3315, 3235)),
                JewelleryDestination("Castle Wars", Tile(2440, 3090)),
                JewelleryDestination("Ferox Enclave", Tile(3151, 3635)),
                JewelleryDestination("Fortis Colosseum", Tile(1793, 3107)),
            ),
    ),

    GAMES_NECKLACE(
        displayName = "games necklace",
        noun = "necklace",
        slot = EquipmentType.AMULET,
        chargeItems =
            listOf(
                "item.games_necklace1",
                "item.games_necklace2",
                "item.games_necklace3",
                "item.games_necklace4",
                "item.games_necklace5",
                "item.games_necklace6",
                "item.games_necklace7",
                "item.games_necklace8",
            ),
        uncharged = null,
        depletedMessage = "Your games necklace crumbles to dust.",
        chargeNoun = "uses",
        destinations =
            listOf(
                JewelleryDestination("Burthorpe", Tile(2899, 3553)),
                JewelleryDestination("Barbarian Outpost", Tile(2520, 3571)),
                // The only destination whose wiki pin is unusable as published: it is given as
                // (2967, 4254) on map 10031, a rendered underground surface whose z axis is offset
                // 128 (two regions) from the real world. Corrected to the static-world tile, which
                // lands in the cave corridor between the "Cave exit" loc at (2963, 4382, 2) and the
                // "Passage" into the lair at (2971, 4382, 2) - both read straight out of this cache.
                JewelleryDestination("Corporeal Beast", Tile(2967, 4382, 2)),
                JewelleryDestination("Tears of Guthix", Tile(3245, 9500)),
                JewelleryDestination("Wintertodt Camp", Tile(1631, 3940)),
            ),
    ),

    DIGSITE_PENDANT(
        // Capitalised because that is how the wiki quotes the game's own charge message.
        displayName = "Digsite Pendant",
        noun = "necklace",
        slot = EquipmentType.AMULET,
        chargeItems =
            listOf(
                "item.digsite_pendant_1",
                "item.digsite_pendant_2",
                "item.digsite_pendant_3",
                "item.digsite_pendant_4",
                "item.digsite_pendant_5",
            ),
        uncharged = null,
        depletedMessage = "Your Digsite Pendant crumbles to dust.",
        chargeNoun = "uses",
        destinations =
            listOf(
                JewelleryDestination("Digsite", Tile(3341, 3445)),
                JewelleryDestination("Fossil Island", Tile(3763, 3869, 1)),
                JewelleryDestination("Lithkren Dungeon", Tile(3549, 10456)),
            ),
    ),

    NECKLACE_OF_PASSAGE(
        displayName = "necklace of passage",
        noun = "necklace",
        slot = EquipmentType.AMULET,
        chargeItems =
            listOf(
                "item.necklace_of_passage1",
                "item.necklace_of_passage2",
                "item.necklace_of_passage3",
                "item.necklace_of_passage4",
                "item.necklace_of_passage5",
            ),
        uncharged = null,
        depletedMessage = "Your necklace of passage crumbles away.",
        chargeNoun = "uses",
        destinations =
            listOf(
                JewelleryDestination("Wizards' Tower", Tile(3114, 3179)),
                JewelleryDestination("The Outpost", Tile(2430, 3349)),
                JewelleryDestination("Eagles' Eyrie", Tile(3405, 3158)),
            ),
    ),

    BURNING_AMULET(
        displayName = "burning amulet",
        noun = "amulet",
        slot = EquipmentType.AMULET,
        chargeItems =
            listOf(
                "item.burning_amulet1",
                "item.burning_amulet2",
                "item.burning_amulet3",
                "item.burning_amulet4",
                "item.burning_amulet5",
            ),
        uncharged = null,
        // Wording follows the ring of dueling and games necklace, the two crumbling pieces whose
        // messages the wiki does quote; the burning amulet's own is not published anywhere.
        depletedMessage = "Your burning amulet crumbles to dust.",
        chargeNoun = "uses",
        destinations =
            listOf(
                JewelleryDestination("Chaos Temple", Tile(3234, 3634)),
                JewelleryDestination("Bandit Camp", Tile(3038, 3651)),
                JewelleryDestination("Lava Maze", Tile(3028, 3842)),
            ),
    ),

    /**
     * The one piece with no named destinations: the cache gives it a single worn option, "Rub", and
     * it sends the wearer to their respawn point - hence the null tile.
     */
    RING_OF_RETURNING(
        displayName = "ring of returning",
        noun = "ring",
        slot = EquipmentType.RING,
        chargeItems =
            listOf(
                "item.ring_of_returning1",
                "item.ring_of_returning2",
                "item.ring_of_returning3",
                "item.ring_of_returning4",
                "item.ring_of_returning5",
            ),
        uncharged = null,
        depletedMessage = "Your ring of returning crumbles away.",
        chargeNoun = "uses",
        destinations = listOf(JewelleryDestination("Rub", null)),
    ),
    ;

    val maxCharges: Int get() = chargeItems.size

    /** True when a used-up piece leaves an item behind that a fountain can recharge. */
    val rechargeable: Boolean get() = uncharged != null

    companion object {
        val values = enumValues<ChargedJewellery>()
    }
}
