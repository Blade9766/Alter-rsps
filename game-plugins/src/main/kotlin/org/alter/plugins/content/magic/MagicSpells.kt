package org.alter.plugins.content.magic

import dev.openrune.cache.CacheManager.getEnum
import dev.openrune.cache.CacheManager.getItem
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.alter.api.Skills
import org.alter.api.ext.getSpellbook
import org.alter.api.ext.getVarbit
import org.alter.api.ext.message
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.rscm.RSCM.getRSCM

/**
 * @author Tom <rspsmods@gmail.com>
 */
object MagicSpells {
    const val INF_RUNES_VARBIT = 4145

    private const val SPELLBOOK_POINTER_ENUM = 1981

    private const val SPELL_SPELLBOOK_KEY = 336
    private const val SPELL_RUNE1_ID_KEY = 365
    private const val SPELL_RUNE1_AMT_KEY = 366
    private const val SPELL_RUNE2_ID_KEY = 367
    private const val SPELL_RUNE2_AMT_KEY = 368
    private const val SPELL_RUNE3_ID_KEY = 369
    private const val SPELL_RUNE3_AMT_KEY = 370
    private const val SPELL_COMPONENT_HASH_KEY = 596
    private const val SPELL_ID_KEY = 599
    private const val SPELL_NAME_KEY = 601
    private const val SPELL_DESC_KEY = 602
    private const val SPELL_LVL_REQ_KEY = 604
    private const val SPELL_TYPE_KEY = 605

    private const val COMBAT_SPELL_TYPE = 0
    private const val MISC_SPELL_TYPE = 1
    private const val TELEPORT_SPELL_TYPE = 2

    private val STAFF_ITEMS =
        arrayOf(
            "item.ibans_staff",
            "item.ibans_staff_u",
            "item.slayers_staff",
            "item.slayers_staff_e",
            "item.saradomin_staff",
            "item.guthix_staff",
            "item.zamorak_staff",
        )

    private val metadata = Int2ObjectOpenHashMap<SpellMetadata>()

    fun getMetadata(spellId: Int): SpellMetadata? = metadata[spellId]

    fun getCombatSpells(): Map<Int, SpellMetadata> = metadata.filter { it.value.spellType == COMBAT_SPELL_TYPE }

    fun canCast(
        p: Player,
        lvl: Int,
        items: List<Item>,
        requiredBook: Int,
    ): Boolean {
        if (requiredBook != -1 && p.getSpellbook().id != requiredBook) {
            p.message("You can't cast this spell.")
            return false
        }
        if (p.getSkills().getBaseLevel(Skills.MAGIC) < lvl) {
            p.message("Your Magic level is not high enough for this spell.")
            return false
        }
        if (p.getVarbit(INF_RUNES_VARBIT) == 0) {
            for (item in items) {
                if (ElementalStaves.providesUnlimited(p, item.id)) {
                    continue
                }
                if (p.inventory.getItemCount(item.id) < item.amount && p.equipment.getItemCount(item.id) < item.amount) {
                    p.message("You do not have enough ${item.getDef().name}s to cast this spell.")
                    return false
                }
            }
        }
        return true
    }

    fun removeRunes(
        p: Player,
        items: List<Item>,
    ) {
        if (p.getVarbit(INF_RUNES_VARBIT) == 0) {
            for (item in items) {
                /*
                 * Do not remove staff item requirements.
                 */
                if (item.id in getRSCM(STAFF_ITEMS)) {
                    continue
                }
                if (ElementalStaves.providesUnlimited(p, item.id)) {
                    continue
                }
                p.inventory.remove(item)
            }
        }
    }

    fun isLoaded(): Boolean = metadata.isNotEmpty()

    fun loadSpellRequirements(world: World) {
        val spellBookEnums = getEnum(SPELLBOOK_POINTER_ENUM)
        val spellBooks = spellBookEnums.values.values.map { it as Int }
        spellBooks.forEach { spellBook ->
            val spellBookEnum = getEnum(spellBook)
            val spellItems = spellBookEnum.values.values.map { it as Int }

            for (item in spellItems) {
                val spell = readSpell(item) ?: continue
                metadata[item] = spell
            }
        }
    }

    /**
     * The metadata for one spell, read straight off that spell's own item params and cached.
     *
     * [loadSpellRequirements] only walks the four top-level spellbook enums, so a spell that lives
     * behind one of the book's *sub-pages* is never indexed by it. The seven jewellery enchants are
     * exactly that case: the standard book's enum holds a single "Jewellery Enchantments" entry
     * (item 27089) and the client's own scripts open the page listing Lvl-1 through Lvl-7. Their
     * params are ordinary spell params all the same, which is what lets them be read on demand
     * here rather than having their level, runes and component hardcoded in a plugin.
     */
    fun loadSpell(spellItem: Int): SpellMetadata? = metadata[spellItem] ?: readSpell(spellItem)?.also { metadata[spellItem] = it }

    private fun readSpell(item: Int): SpellMetadata? {
        val itemDef = getItem(item)
        val params = itemDef.params ?: return null
        val name = params[SPELL_NAME_KEY] as? String ?: return null

        val spellbook = params[SPELL_SPELLBOOK_KEY] as Int
        val lvl = params[SPELL_LVL_REQ_KEY] as Int
        val componentHash = params[SPELL_COMPONENT_HASH_KEY] as Int
        val spellType = params[SPELL_TYPE_KEY] as Int

        val interfaceId = componentHash shr 16
        val component = componentHash and 0xFFFF
        val runes = mutableListOf<Item>()

        if (params.containsKey(SPELL_RUNE1_ID_KEY)) {
            runes.add(Item(params[SPELL_RUNE1_ID_KEY] as Int, params[SPELL_RUNE1_AMT_KEY] as Int))
        }
        if (params.containsKey(SPELL_RUNE2_ID_KEY)) {
            runes.add(Item(params[SPELL_RUNE2_ID_KEY] as Int, params[SPELL_RUNE2_AMT_KEY] as Int))
        }
        if (params.containsKey(SPELL_RUNE3_ID_KEY)) {
            runes.add(Item(params[SPELL_RUNE3_ID_KEY] as Int, params[SPELL_RUNE3_AMT_KEY] as Int))
        }

        return SpellMetadata(interfaceId, component, item, spellbook, spellType, name, lvl, runes)
    }

    // fun KotlinPlugin.on_magic_spell_button(name: String, plugin: Plugin.(SpellMetadata) -> Unit) {
    //    if (!MagicSpells.isLoaded()) {
    //        MagicSpells.loadSpellRequirements(world)
    //    }
    //    // If this line throws an error, it means the spell with said name
    //    // is not found in cache.
    //    val spell = metadata.values.first { it.name == name }
    //    on_button(spell.interfaceId, spell.component) {
    //        plugin(this, spell)
    //    }
    // }
}
