package org.alter.game.service.game

import AnimationData
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.openrune.cache.CacheManager
import dev.openrune.cache.CacheManager.getItem
import dev.openrune.cache.filestore.definition.data.ItemType
import dev.openrune.cache.filestore.definition.data.ParamMapper
import gg.rsmod.util.ServerProperties
import gg.rsmod.util.Stopwatch
import io.github.oshai.kotlinlogging.KotlinLogging
import it.unimi.dsi.fastutil.bytes.Byte2ByteOpenHashMap
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.service.Service
import org.yaml.snakeyaml.LoaderOptions
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

/**
 * @author Tom <rspsmods@gmail.com>
 */
class ItemMetadataService : Service {
    override fun init(
        server: Server,
        world: World,
        serviceProperties: ServerProperties,
    ) {
        loadAll()
    }

    var ms: Long = 0
    fun loadAll() {
        val stopwatch = Stopwatch.createStarted().reset().start()
        val loaderOptions = LoaderOptions()
        loaderOptions.codePointLimit = 10 * 1024 * 1024 // 10 MB
        val yamlFactory =
            YAMLFactory.builder()
                .loaderOptions(loaderOptions)
                .build()
        val mapper = YAMLMapper(yamlFactory)

        val path = Paths.get("../data/cfg/items")

        try {
            /**
             * Loads item examine text from an external CSV file and assigns it to item definitions.
             *
             * The file is expected to be located at `../data/cfg/objs.csv` and should contain item IDs
             * paired with their respective examine text, separated by commas.
             *
             * - The first value in each line is treated as the item ID.
             * - The remaining text after the first comma is treated as the examine description.
             * - The examine text is assigned to the corresponding item definition if the ID is valid.
             *
             * This ensures that item examine information gets loaded from an external source at runtime.
             */
            Paths.get("../data/cfg/objs.csv").toFile().forEachLine { line ->
                val parts = line.split(",")
                if (parts.size >= 2) {
                    val id = parts[0].toIntOrNull()
                    val examine = line.substringAfter(',').trim().removeSurrounding("\"")
                    if (id != null) {
                        getItem(id).examine = examine
                    }
                }
            }

            /**
             * Initializes item definitions by loading cached item configurations and updating specific attributes.
             *
             * - Adjusts item weight by dividing the cached value by 1000.
             * - Sets the attack speed using a validated parameter (ID 14).
             * - Determines the weapon type for equippable items in the weapon slot (equipSlot 3) based on their category.
             * - Assigns the equip type from the item's appearance override.
             * - Populates item bonuses using a predefined set of validated parameters.
             *
             * This process ensures that item attributes are properly loaded and validated from cache for use in gameplay.
             */
            CacheManager.getItems().forEach { (_, item) ->
                val def = getItem(item.id)

                def.weight /= 1000
                def.equipType = def.appearanceOverride1

                def.attackSpeed = def.getValidatedParam(
                    ParamMapper.item.ATTACK_RATE,
                    7
                ) // Just in case the Attack Rate would be not configurated in cache.

                if (def.equipSlot == 3) {
                    def.weaponType = WeaponCategory.get(def, def.category)
                }


                def.bonuses =
                    intArrayOf(
                        def.getValidatedParam(ParamMapper.item.STAB_ATTACK_BONUS),
                        def.getValidatedParam(ParamMapper.item.SLASH_ATTACK_BONUS),
                        def.getValidatedParam(ParamMapper.item.CRUSH_ATTACK_BONUS),
                        def.getValidatedParam(ParamMapper.item.MAGIC_ATTACK_BONUS),
                        def.getValidatedParam(ParamMapper.item.RANGED_ATTACK_BONUS),
                        def.getValidatedParam(ParamMapper.item.STAB_DEFENCE_BONUS),
                        def.getValidatedParam(ParamMapper.item.SLASH_DEFENCE_BONUS),
                        def.getValidatedParam(ParamMapper.item.CRUSH_DEFENCE_BONUS),
                        def.getValidatedParam(ParamMapper.item.MAGIC_DEFENCE_BONUS),
                        def.getValidatedParam(ParamMapper.item.RANGED_DEFENCE_BONUS),
                        def.getValidatedParam(ParamMapper.item.MELEE_STRENGTH),
                        def.rangedStrength(),
                        def.getValidatedParam(ParamMapper.item.MAGIC_DAMAGE_STRENGTH) / 10,
                        def.getValidatedParam(ParamMapper.item.PRAYER_BONUS),
                    )

                /*
                 * Only the requirement tiers the item actually declares may be read. Items with a
                 * single requirement carry no secondary/tertiary/quaternary params at all, and
                 * reading them through the defaulting accessor yields skill 0 (Attack) at level 0 -
                 * which then overwrites a real Attack requirement written by the primary tier. That
                 * left every Attack-gated weapon in the game wearable at level 1.
                 */
                val reqs = Byte2ByteOpenHashMap()
                SKILL_REQ_PARAMS.forEach { (skillKey, levelKey) ->
                    val skill = def.params?.get(skillKey) as? Int ?: return@forEach
                    val level = def.params?.get(levelKey) as? Int ?: return@forEach
                    if (level > 0) {
                        reqs[skill.toByte()] = level.toByte()
                    }
                }
                if (!reqs.isEmpty()) {
                    def.skillReqs = reqs
                }
            }

            /**
             * Loads and assigns render animations to item definitions from external JSON files.
             *
             * - `bas_mappings.json` maps animation identifiers to their corresponding animation data (e.g., ready, walk, run animations).
             * - `item_bas.json` maps item IDs to the animation identifiers used in the mappings.
             *
             * The process:
             * - Each item ID from `item_bas.json` is matched to its animation data from `bas_mappings.json`.
             * - If a matching animation is found, it populates the item's render animations array with the relevant animation IDs.
             *
             * This ensures that items have appropriate movement and action animations during gameplay.
             */
            val animationMap: Map<String, AnimationData> =
                mapper.readValue(File("../data/cfg/items/renderAnimations/bas_mappings.json").readText())
            val valueMap: Map<Int, Int> = ObjectMapper().apply {
                findAndRegisterModules()
            }.readValue(File("../data/cfg/items/renderAnimations/item_bas.json").readText())
            valueMap.forEach { (item, animMap) ->
                val animation = animationMap[animMap.toString()] ?: return@forEach
                val def = getItem(item)
                def.renderAnimations = intArrayOf(
                    animation.readyAnim,
                    animation.turnAnim,
                    animation.walkAnim,
                    animation.walkAnimBack,
                    animation.walkAnimLeft,
                    animation.walkAnimRight,
                    animation.runAnim,
                )
            }

            /**
             * Loads item override metadata from all files within the "itemOverrides" directory.
             *
             * - The directory is resolved relative to the provided path.
             * - Files are processed in parallel for efficient loading.
             * - Each file is deserialized into a `Metadata` object and passed to the `load` function.
             *
             * This process ensures that custom item attributes or behaviors are loaded at runtime.
             *
             * @TODO Add better context as to why file could not be loaded.
             * @TODO Add support for remaining [`def`] properties override method.
             */
            Files.walk(path.resolve("itemOverrides")).parallel().filter { it.toFile().isFile }.forEach { file ->
                if (file.fileName.toString().contains("FileExample.yml")) return@forEach

                /*
                 * Per file, so one malformed document does not abandon every override that
                 * would have been read after it.
                 */
                try {
                    val content = file.toFile().readText()
                    content.split(Regex("(?m)^---\\s*$"))
                        .filter { it.hasMappings() }.forEach { document ->
                            val data = mapper.readValue(document, Metadata::class.java)
                            load(data)
                        }
                } catch (e: Exception) {
                    logger.error(e) { "Could not load item overrides from $file." }
                }
            }

            loadEquipmentRequirements(mapper, path.resolve("equipmentRequirements.yml").toFile())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        ms = stopwatch.elapsed(TimeUnit.MILLISECONDS)
    }

    /**
     * Fills in the equip requirements the cache leaves out.
     *
     * The 228 cache carries requirements as item params, and the block above reads them - but it
     * only has them from the rune tier upward, so every steel, black, white, mithril and adamant
     * weapon and armour piece was equippable at level 1, along with a scattering of higher-tier
     * items the cache simply forgot (the dragon halberd, the granite set). See the config's own
     * header for the survey those numbers come from.
     *
     * Writes are **additive per skill**: a requirement is only recorded for a skill the item does
     * not already declare. That is what lets this run last, after both the cache params and the
     * per-item `itemOverrides/` documents, without being able to weaken either of them - the
     * granite maul keeps the Attack 50 it already had, and only its variants gain one.
     *
     * A missing config is not an error; the file is optional and the server starts without it.
     */
    private fun loadEquipmentRequirements(
        mapper: ObjectMapper,
        file: File,
    ) {
        if (!file.exists()) {
            logger.info { "No equipment requirement config at ${file.path}; cache params only." }
            return
        }

        val config =
            try {
                mapper.readValue(file, EquipmentRequirements::class.java)
            } catch (e: Exception) {
                logger.error(e) { "Could not load equipment requirements from ${file.path}." }
                return
            }

        var applied = 0
        config.groups.forEach { group ->
            val reqs = group.skillReqs.mapNotNull { req ->
                val skill = req.skill ?: return@mapNotNull null
                val level = req.level ?: return@mapNotNull null
                getSkillId(skill) to level.toByte()
            }
            if (reqs.isEmpty()) {
                return@forEach
            }
            group.items.forEach { entry ->
                val def = getItem(entry.id)
                val existing = def.skillReqs ?: Byte2ByteOpenHashMap().also { def.skillReqs = it }
                reqs.forEach { (skill, level) ->
                    if (!existing.containsKey(skill)) {
                        existing[skill] = level
                        applied++
                    }
                }
            }
        }
        logger.info { "Applied $applied equipment requirements from ${file.name}." }
    }

    /**
     * Applies one override document on top of the definition already built from the cache.
     *
     * Every field is optional, and only what a document actually declares is written. A document
     * that names just a skill requirement has to leave the item's cache-derived bonuses, weight,
     * attack speed and render animations alone: writing all of them unconditionally is what left
     * all six Barrows sets with no equipment bonuses at all and an attack speed of one tick.
     */
    fun load(item: Metadata) {
        val def = getItem(item.id)

        item.name?.let { def.name = it }
        item.examine?.let { def.examine = it }
        item.tradeable?.let { def.isTradeable = it }
        item.weight?.let { def.weight = it }
        item.cost?.let { def.cost = it }

        val equipment = item.equipment ?: return
        val slots = equipment.equipSlot?.let { getEquipmentSlots(it, def.id) }

        equipment.attackSpeed?.let { def.attackSpeed = it }

        /*
         * TODO def.attackSounds = equipment.attackSounds
         *  - Create Array of AttackStyleID -> It's Sound
         *  accurateAnim : accurateSound
         *  aggressiveAnim : aggressiveSound
         *  controlledAnim : controlledSound
         *  defensiveAnim : defensiveSound
         *  <--- AttackStyle can be from 0-3. If no data on it, it will be -1.
         *  blockAnim = When target attacks the Pawn on next tick?
         *
         *  TODO def.equipSound = equipment.equipSound
         */
        when {
            equipment.weaponType != null -> def.weaponType = equipment.weaponType
            slots?.slot == WEAPON_SLOT && def.weaponType <= 0 -> def.weaponType = DEFAULT_WEAPON_TYPE
        }

        equipment.renderAnimations?.let { def.renderAnimations = it.getAsArray() }

        if (slots != null) {
            def.equipSlot = slots.slot
            def.equipType = slots.secondary
        }

        equipment.skillReqs?.let { declared ->
            val reqs = Byte2ByteOpenHashMap()
            declared.forEach { req ->
                val skill = req.skill ?: return@forEach
                val level = req.level ?: return@forEach
                reqs[getSkillId(skill)] = level.toByte()
            }
            def.skillReqs = reqs
        }

        val bonuses = def.bonusesOrZero()
        equipment.attackStab?.let { bonuses[BonusIndex.ATTACK_STAB] = it }
        equipment.attackSlash?.let { bonuses[BonusIndex.ATTACK_SLASH] = it }
        equipment.attackCrush?.let { bonuses[BonusIndex.ATTACK_CRUSH] = it }
        equipment.attackMagic?.let { bonuses[BonusIndex.ATTACK_MAGIC] = it }
        equipment.attackRanged?.let { bonuses[BonusIndex.ATTACK_RANGED] = it }
        equipment.defenceStab?.let { bonuses[BonusIndex.DEFENCE_STAB] = it }
        equipment.defenceSlash?.let { bonuses[BonusIndex.DEFENCE_SLASH] = it }
        equipment.defenceCrush?.let { bonuses[BonusIndex.DEFENCE_CRUSH] = it }
        equipment.defenceMagic?.let { bonuses[BonusIndex.DEFENCE_MAGIC] = it }
        equipment.defenceRanged?.let { bonuses[BonusIndex.DEFENCE_RANGED] = it }
        equipment.meleeStrength?.let { bonuses[BonusIndex.MELEE_STRENGTH] = it }
        equipment.rangedStrength?.let { bonuses[BonusIndex.RANGED_STRENGTH] = it }
        equipment.magicDamage?.let { bonuses[BonusIndex.MAGIC_DAMAGE] = it }
        equipment.prayer?.let { bonuses[BonusIndex.PRAYER] = it }
        def.bonuses = bonuses
    }

    /**
     * Whether a chunk between two `---` separators actually declares anything.
     *
     * A plain `isNotBlank()` is not enough. Splitting on `---` makes whatever precedes the
     * first separator its own document, so a file that opens with a comment header yields a
     * document of nothing but `#` lines - not blank, but with no mappings for Jackson to
     * bind, which raises "No content to map due to end-of-input". That exception is caught
     * per *file*, so one header comment silently discarded every override in the file
     * beneath it.
     */
    private fun String.hasMappings(): Boolean =
        lineSequence().any { line ->
            val trimmed = line.trim()
            trimmed.isNotEmpty() && !trimmed.startsWith("#")
        }

    /**
     * [ItemType.bonuses] is `lateinit` and only populated for ids the cache actually knows about, so
     * an override for a purely custom id has to start from an empty set rather than throw.
     */
    private fun ItemType.bonusesOrZero(): IntArray =
        runCatching { bonuses }.getOrNull()?.copyOf() ?: IntArray(BONUS_COUNT)

    private fun getEquipmentSlots(
        slot: String,
        id: Int? = null,
    ): EquipmentSlots {
        val equipSlot: Int
        var equipType = -1
        when (slot) {
            "hat" -> equipSlot = 0
            "cape" -> equipSlot = 1
            "neck" -> equipSlot = 2
            "weapon" -> equipSlot = 3
            "torso" -> equipSlot = 4
            "shield" -> equipSlot = 5
            "legs" -> equipSlot = 7
            "hands" -> equipSlot = 9
            "feet" -> equipSlot = 10
            "ring" -> equipSlot = 12
            "ammo" -> equipSlot = 13

            "head" -> {
                equipSlot = 0
                equipType = 8
            }
            // For hats that requires hair removal
            "nohair" -> {
                equipSlot = 0
                equipType = 11
            }

            "2h" -> {
                equipSlot = 3
                equipType = 5
            }

            "body" -> {
                equipSlot = 4
                equipType = 6
            }

            else -> throw IllegalArgumentException("Illegal equipment slot: $slot, $id")
        }
        return EquipmentSlots(equipSlot, equipType)
    }

    private data class EquipmentSlots(val slot: Int, val secondary: Int)


    /**
     * Ranged strength, from whichever of the two params the item happens to use.
     *
     * The cache splits this bonus across param 12 and param 189 with no overlap - 317 items
     * declare the first, 59 the second, none both. Reading only param 12 is what left the
     * twisted bow, the whole Masori set, Ava's assembler, Dizana's quiver, the necklace of
     * anguish, the ballistae, the venator bows, Pegasian boots, Zaryte vambraces, the Odium
     * ward and the twisted buckler all sitting at +0 ranged strength.
     */
    private fun ItemType.rangedStrength(): Int {
        val primary = params?.get(ParamMapper.item.RANGED_STRENGTH_BONUS) as? Int
        if (primary != null) {
            return primary
        }
        return params?.get(ParamMapper.item.RANGED_STRENGTH_BONUS_ALT) as? Int ?: 0
    }

    private fun ItemType.getValidatedParam(key: Int, defaultValue: Int = 0): Int {
        if (this.params?.get(key) != null) {
            try {
                return this.params?.get(key) as Int
            } catch (e: Exception) {
                println("${this.id} || ${this.params}")
                e.printStackTrace()
            }
        }

        /**
         * @TODO Rethink the logic, gets printed out even for items that are not wearable.
         * logger.warn {
         *   "Item with ID: ${this.id} is missing the key '$key' in its params. Full params list: ${this.params}. Default value was set: $defaultValue."
         * }
         */
        return defaultValue
    }

    private fun getSkillId(name: String): Byte =
        when (name) {
            // Need to get a better dump db. As we can see, this one has some
            // inconsistency for some reason.
            "attack" -> 0
            "defence" -> 1
            "strength" -> 2
            "hitpoints" -> 3
            "range", "ranged" -> 4
            "prayer" -> 5
            "magic" -> 6
            "cooking" -> 7
            "woodcutting" -> 8
            "fletching" -> 9
            "fishing" -> 10
            "firemaking" -> 11
            "crafting" -> 12
            "smithing" -> 13
            "mining" -> 14
            "herblore" -> 15
            "agility" -> 16
            "thieving", "theiving" -> 17
            "slayer" -> 18
            "farming" -> 19
            "runecrafting", "runecraft" -> 20
            "hunter" -> 21
            "construction", "contruction" -> 22
            "combat" -> 3
            else -> throw IllegalArgumentException("Illegal skill name: $name")
        }

    /**
     * One override document. Every field is optional: `null` means "the document did not mention
     * this", and the value already loaded from the cache is kept. Field names are camelCase, with
     * the snake_case spellings accepted as aliases.
     */
    data class Metadata(
        @field:JsonProperty("id") val id: Int = -1,
        @field:JsonProperty("name") val name: String? = null,
        @field:JsonProperty("examine") val examine: String? = null,
        @field:JsonProperty("tradeable") val tradeable: Boolean? = null,
        @field:JsonProperty("weight") val weight: Double? = null,
        @field:JsonProperty("cost") val cost: Int? = null,
        /*
         * Accepted so existing documents keep parsing, but [ItemType] has nowhere to put them yet.
         */
        @field:JsonProperty("tradeable_on_ge") val tradeableOnGe: Boolean? = null,
        @field:JsonProperty("lowalch") val lowalch: Int? = null,
        @field:JsonProperty("highalch") val highalch: Int? = null,
        @field:JsonProperty("buy_limit") val buyLimit: Int? = null,
        @field:JsonProperty("equipment") val equipment: Equipment? = null,
    )

    data class Equipment(
        @field:JsonProperty("equipSlot") @field:JsonAlias("equip_slot") val equipSlot: String? = null,
        @field:JsonProperty("equipSound") @field:JsonAlias("equip_sound") val equipSound: Int? = null,
        @field:JsonProperty("weaponType") @field:JsonAlias("weapon_type") val weaponType: Int? = null,
        @field:JsonProperty("attackSpeed") @field:JsonAlias("attack_speed") val attackSpeed: Int? = null,
        @field:JsonProperty("attackStab") @field:JsonAlias("attack_stab") val attackStab: Int? = null,
        @field:JsonProperty("attackSlash") @field:JsonAlias("attack_slash") val attackSlash: Int? = null,
        @field:JsonProperty("attackCrush") @field:JsonAlias("attack_crush") val attackCrush: Int? = null,
        @field:JsonProperty("attackMagic") @field:JsonAlias("attack_magic") val attackMagic: Int? = null,
        @field:JsonProperty("attackRanged") @field:JsonAlias("attack_ranged") val attackRanged: Int? = null,
        @field:JsonProperty("defenceStab") @field:JsonAlias("defence_stab") val defenceStab: Int? = null,
        @field:JsonProperty("defenceSlash") @field:JsonAlias("defence_slash") val defenceSlash: Int? = null,
        @field:JsonProperty("defenceCrush") @field:JsonAlias("defence_crush") val defenceCrush: Int? = null,
        @field:JsonProperty("defenceMagic") @field:JsonAlias("defence_magic") val defenceMagic: Int? = null,
        @field:JsonProperty("defenceRanged") @field:JsonAlias("defence_ranged") val defenceRanged: Int? = null,
        @field:JsonProperty("meleeStrength") @field:JsonAlias("melee_strength") val meleeStrength: Int? = null,
        @field:JsonProperty("rangedStrength") @field:JsonAlias("ranged_strength") val rangedStrength: Int? = null,
        @field:JsonProperty("magicDamage") @field:JsonAlias("magic_damage") val magicDamage: Int? = null,
        @field:JsonProperty("prayer") val prayer: Int? = null,
        @field:JsonProperty("renderAnimations") @field:JsonAlias("render_animations") val renderAnimations: RenderAnimations? = null,
        @field:JsonProperty("attackSounds") @field:JsonAlias("attack_sounds") val attackSounds: List<Int>? = null,
        @field:JsonProperty("skillReqs") @field:JsonAlias("skill_reqs") val skillReqs: List<SkillRequirement>? = null,
    )

    data class RenderAnimations(
        @field:JsonProperty("standAnimId") val standAnimId: Int = 0,
        @field:JsonProperty("turnOnSpotAnim") val turnOnSpotAnim: Int = 0,
        @field:JsonProperty("walkForwardAnimId") val walkForwardAnimId: Int = 0,
        @field:JsonProperty("walkBackwardsAnimId") val walkBackwardsAnimId: Int = 0,
        @field:JsonProperty("walkLeftAnimId") val walkLeftAnimId: Int = 0,
        @field:JsonProperty("walkRightAnimId") val walkRightAnimId: Int = 0,
        @field:JsonProperty("runAnimId") val runAnimId: Int = 0,
    ) {
        fun getAsArray(): IntArray =
            intArrayOf(
                standAnimId,
                turnOnSpotAnim,
                walkForwardAnimId,
                walkBackwardsAnimId,
                walkLeftAnimId,
                walkRightAnimId,
                runAnimId,
            )
    }

    data class SkillRequirement(
        @field:JsonProperty("skill") val skill: String? = null,
        @field:JsonProperty("level") val level: Int? = null,
    )

    /**
     * `equipmentRequirements.yml`: requirements grouped by the set of skills they gate on, so the
     * hundreds of items sharing one rung of the metal ladder are listed once rather than each
     * repeating its own requirement block.
     */
    data class EquipmentRequirements(
        @field:JsonProperty("groups") val groups: List<RequirementGroup> = emptyList(),
    )

    data class RequirementGroup(
        @field:JsonProperty("skillReqs") @field:JsonAlias("skill_reqs") val skillReqs: List<SkillRequirement> = emptyList(),
        @field:JsonProperty("items") val items: List<RequirementItem> = emptyList(),
    )

    /**
     * [name] is not read into anything - it is there so the file stays reviewable, and so the
     * verify test can assert the id still carries that name in the cache.
     */
    data class RequirementItem(
        @field:JsonProperty("id") val id: Int = -1,
        @field:JsonProperty("name") val name: String? = null,
    )

    companion object {
        val logger = KotlinLogging.logger {}

        /**
         * Number of entries in [ItemType.bonuses], indexed by [BonusIndex].
         */
        private const val BONUS_COUNT = 14

        private const val WEAPON_SLOT = 3

        /**
         * Positions within [ItemType.bonuses]. The first ten mirror `org.alter.api.BonusSlot`, which
         * lives in a module this one cannot see.
         */
        private object BonusIndex {
            const val ATTACK_STAB = 0
            const val ATTACK_SLASH = 1
            const val ATTACK_CRUSH = 2
            const val ATTACK_MAGIC = 3
            const val ATTACK_RANGED = 4
            const val DEFENCE_STAB = 5
            const val DEFENCE_SLASH = 6
            const val DEFENCE_CRUSH = 7
            const val DEFENCE_MAGIC = 8
            const val DEFENCE_RANGED = 9
            const val MELEE_STRENGTH = 10
            const val RANGED_STRENGTH = 11
            const val MAGIC_DAMAGE = 12
            const val PRAYER = 13
        }

        /**
         * Weapon type applied to an override that puts an item in the weapon slot without saying
         * which animation set it uses.
         */
        private const val DEFAULT_WEAPON_TYPE = 17

        /**
         * The four equipment requirement tiers, as (skill param, level param) pairs.
         */
        private val SKILL_REQ_PARAMS = listOf(
            ParamMapper.item.PRIMARY_SKILL to ParamMapper.item.PRIMARY_LEVEL,
            ParamMapper.item.SECONDARY_SKILL to ParamMapper.item.SECONDARY_LEVEL,
            ParamMapper.item.TERTIARY_SKILL to ParamMapper.item.TERTIARY_LEVEL,
            ParamMapper.item.QUATERNARY_SKILL to ParamMapper.item.QUATERNARY_LEVEL,
        )
    }
}
