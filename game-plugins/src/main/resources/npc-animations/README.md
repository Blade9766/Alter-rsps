# NPC animation observations

`openosrs-animations.json` is a snapshot of the OpenOSRS service-animations database. It maps NPC
IDs to animations observed in the OSRS client. The runtime resolver removes movement sequences and
classifies the remaining action sequences using metadata from this server's own cache revision.

Source: https://github.com/open-osrs/service-animations/blob/master/animations.json

License: BSD-2-Clause (see `OPENOSRS-LICENSE.txt`).

## `id-combat-media.json`

Hand-authored overrides keyed by NPC **ID**, each naming a key in `named-combat-media.json`. These
are consulted *before* the name lookup, and exist for the case that file cannot express: a single
cache name covering two different rigs, or two different weapon states.

Add an ID here only with cache evidence - a differing `standAnim`/model, or an observed set that
disagrees with the name entry - and record what the evidence was.

| IDs | Name | Points at | Evidence |
| --- | --- | --- | --- |
| 85, 93, 2527 | Ghost | `ALT_GHOST` | These three carry model 21154 and **`standAnim` 5538**; every other `Ghost` ID carries models 21143-21149 and `standAnim` 5530. The NPC's own idle sequence is the test this file trusts, and 5538 sits in the same block as 5540/5541/5542. Their observed sets agree. |
| 4247, 7916 | Thief | `ARMED_HUMAN` | Both wield a weapon and are observed playing 386/388/836; the other seven `Thief` IDs play the unarmed 425/422/836. |

`ALT_GHOST` had been in `named-combat-media.json` since it was written and **nothing could ever
select it** - every NPC carrying that model is also called "Ghost", so the name lookup always
returned `GHOST`. This file is what made it reachable.

## `named-combat-media.json`

Hand-authored overrides keyed by NPC *name*, taking priority over the observations above. Use one
when the observed data is missing, or is right for a model the cache no longer ships.

### Vet'ion and Calvar'ion

Both were remodelled with **skeletal** animations, and both needed pinning here:

- **Calvar'ion has no entry in the observations at all** (none of 11993/11994/11995), so it fell
  through to the human fallback animations entirely.
- **Vet'ion's observations are pre-rework.** They list 5499/5507/5508, which are frame-group 1440 -
  the *old* model's rig. The current model's own idle and walk sequences (9965 and 9967, straight
  off the NPC definition) are skeletal, from the 9965-9981 block.

Calvar'ion 11993 shares Vet'ion 6611's models and idle/walk sequences exactly, so the two take the
same set. Within that block the roles were read off sequence metadata, using the same signals
`MonsterAnimationResolver` itself uses:

| Sequence | Role | Why |
| --- | --- | --- |
| 9969 | attack | One of the three 5-sound actions; the resolver treats frame-sound count as the attack signal. Shares its first three sound beats with 9978. |
| 9973 | block | Shortest of the low-sound actions, which is the resolver's hit-reaction rule. |
| 9980 | death | Priority 10, the death convention throughout this cache. |
| 9978 | shield bash | Priority 9 variant of 9969 - used directly by `WildernessBossCombatPlugin`. |
| 9977 | enrage | The one unpaired sequence in a block that is otherwise all normal/enraged pairs. |

9971 and 9975 are the remaining two actions (the hound summon and one other) and are unused.

Note that a skeletal sequence always reports `cycleLength = 0`, because `SequenceDecoder` only
computes a length from the classic frame-list opcode. `NpcDeathAction` compensates so that skeletal
death animations still play out.

### The rest of the Wilderness bosses

The same audit was run across the others with `npcAnimDiag`, comparing each NPC's own idle/walk
sequences against whatever the observations supply. Five more were broken, for three distinct
reasons:

| NPC | Was | Now |
| --- | --- | --- |
| Callisto / Artio | No observations for Artio at all; Callisto's are pre-rework | 10012 / 10016 / 10017 |
| Venenatis / Spindel | Same | 9991 / 9987 / 9992 |
| Chaos Elemental | Only **two** observations, and `MonsterAnimationResolver` needs three, so it bailed and left the human fallback | 3149 / 3146 / 3147 |
| Skeleton Hellhound | Name prefix-matched `SKELETON`, giving a quadruped the humanoid skeleton's animations | 6579 / 6578 / 6558 |
| Greater Skeleton Hellhound | Attack and block resolved the wrong way round (both candidates identical on sound count and duration, so the tie-break decided it) | 6579 / 6578 / 6558 |

Chaos Fanatic and the Crazy archaeologist are also pinned, but only to correct the ordering - both
sets are the observed ones, and both are humanoids legitimately reusing the shared human sequences
(`836` is the human death the resolver already prefers). Scorpia, her guardian and the King Black
Dragon were checked and left alone: their observations are complete and in their own rigs.

Two notes:

- **Frame group is not a compatibility test.** Humanoids routinely mix their own rig with the
  shared human sequences, so a group mismatch alone proves nothing. What *is* decisive is the
  skeletal/frame-based split, and an NPC having no usable data at all.
- **Callisto's and Venenatis' live IDs are 6609 and 6610**, not 6503/6504. The OpenOSRS capture
  observed 6609 and 6610 in play and never the other two, and only 6609/6610 carry the reworked
  models. 6503/6504 are retained legacy NPCs; they share the same *name*, so they would pick up
  these entries too and should not be spawned.
- Callisto's block is entirely priority 5, so its three roles were assigned on sound count and
  duration alone - the least certain of the entries here.

### The bestiary pass (dwarves, wolves, unicorns, thieves, ghosts, bandits, hobgoblins, moss giants)

One entry was **wrong** and three were **missing**, all found while wiring the `content/npcs`
packages for those species. Every ID involved was re-checked with `npcAnimDiag`.

| Entry | Change | Why |
| --- | --- | --- |
| `WOLF` | 6581 / 6574 / 6576 -> **6559 / 6557 / 6558** | The old set is the *hellhound's*: NPC 3133 is the only NPC in the observations that has ever played any of the three. Every wolf ID in the cache - 106, 107, 108, 110, 2490, 2491, 3912 - is observed playing 6559/6557/6558, which is also what the existing `ICE_WOLF` entry holds. Both sets are frame group 1662, so the wrong one played rather than failing visibly. This also fixed `White wolf`, which suffix-matches `WOLF`. |
| `UNICORN` | new: 6376 / 6375 / 6377 | The resolver got attack and block backwards here. This rig carries no frame sounds, so it falls through to duration, and 6375 (three cycles) beat 6376 (two) despite 6375 being `forcedPriority` 5 - a block - against 6376's 6. Confirmed twice over: the hand-written `STARLIGHT` entry, a unicorn on the same rig, already read 6376/6375/6377; and `crystalline_unicorn`/`corrupted_unicorn` are observed playing only 6376 and 6377, which is what an NPC that just attacks and dies shows. `Unicorn Foal` picks this up by prefix. |
| `BANDIT` | new: 386 / 388 / 836 | Same failure mode on the armed-human set: no frame sounds, and the block (388, ten frames) is longer than the attack (386, six), so the resolver swapped them. Bandits would have parried when they meant to swing. Covers the desert and Pollnivneach bandits and `Bandit champion` by prefix. |
| `THIEF` | new: 425 / 422 / 836 | Not a correction - the same values the resolver reaches for the unarmed thieves - but stated because 5218, 5219 and 5220 have **no** observations at all and were falling through to the engine's bare `NpcCombatDef.DEFAULT`. The two armed thieves are handled by ID in `id-combat-media.json`. |

`DWARF`, `GHOST`, `HOBGOBLIN` and `MOSS_GIANT` were audited the same way and left alone: all four
match what their IDs are observed playing.

### The second bestiary pass (dragons, demons, hellhounds, ice, frogs, and eight more)

Audited with `game-plugins/src/test/.../diag/BestiaryAnimationAudit.kt`, which prints what
`MonsterAnimationsPlugin` would actually hand each NPC. Of the twenty-odd species wired in that
pass, **eight resolved wrongly and two resolved to nothing at all**. Every correction below is
against `Animation`'s own named constants, which is what makes them checkable rather than guesses.

| Entry | Was resolving to | Now | Why |
| --- | --- | --- | --- |
| `GREEN_DRAGON`, `BLUE_DRAGON`, `RED_DRAGON`, `BLACK_DRAGON` | 81 / **80** / 92 | **80** / **89** / 92 | The resolver picked the *dragonfire breath* (81) as the melee attack and the *melee claw* (80) as the block. `Animation` names all five members of this rig outright: `CHROMATIC_DRAGON_MELEE_CLAW = 80`, `CHROMATIC_DRAGON_HIT = 89`, `CHROMATIC_DRAGON_DEATH = 92`, `CHROMATIC_DRAGON_DRAGONFIRE_ATTACK = 81`, `CHROMATIC_DRAGON_MELEE_HEADBUTT = 91`. The breath is not a role this file has - `DragonfireCombatStrategy` plays 81 itself. |
| `BRONZE_DRAGON` | 81 / **91** / 92 | **91** / **89** / 92 | Same failure. Metal dragons have no claw (80) in their observed set at all, so their melee attack is the headbutt. |
| `BABY_GREEN_DRAGON`, `BABY_BLUE_DRAGON`, `BABY_RED_DRAGON`, `BABY_BLACK_DRAGON` | **26 / 25** / 28, or nothing | **25 / 26** / 28 | Attack and block backwards on every baby dragon that had observations, and 245, 5194, 5872 and 5873 have **none**, so those four fell through to the human 422/424/836. `BABYDRAGON` has held the right values since this file was written and, like `ALT_GHOST` before it, nothing could ever select it: no NPC in the cache is *named* "Babydragon". These four names are. |
| `ICE_WARRIOR` | **389 / 843 / 391** | **391 / 389 / 843** | All three roles scrambled - `ICE_WARRIOR_ATTACK = 391` was being played as the death, `ICE_WARRIOR_HIT = 389` as the attack, and 843 (`EARTH_WARRIOR_DEATH`, the shared rig's death) as the block. 2851 has only two observations so the resolver bailed on it entirely, and 13802 has none. |
| `GIANT_FROG` | **4652 / 4651 / 4653** | **1793 / 1794 / 1795** | The `SKELETON_HELLHOUND` failure again, exactly: "Giant frog" starts with `GIANT_`, so a frog was given the hill giant's rig. `FROG` is added alongside it so `Big frog` and `Frog` are pinned rather than left to luck. All three are the toad rig `TOAD_ATTACK`/`TOAD_HIT`/`TOAD_DEATH`. |
| `OUTLAW`, `BLACK_HEATHER` | **388 / 390 / 836** | **390 / 388 / 836** | The armed-human swap `BANDIT` records, on two more names: `HUMAN_SLASH_SWORD_ATTACK = 390` was being played as the block and `HUMAN_SLASH_SWORD_DEFEND = 388` as the attack. |
| `HERO` | **403 / 390 / 836** | **390 / 403 / 836** | Same swap, with `HUMAN_BLUNT_DEFEND2 = 403` as the wrong half. |
| `BATTLE_MAGE` | **415 / 811 / 836** | **811 / 415 / 836** | Same swap: `GOD_SPELL = 811` - which is exactly what a Mage Arena battle mage casts - was the block, and `HUMAN_STAFF_DEFEND = 415` was the attack. |
| `ELDER_CHAOS_DRUID` | **425 / 727 / 836** | **727 / 425 / 836** | Same swap: `MAGIC_WAVE_CAST = 727` was the block and `HUMAN_DEFEND_COWARDLY = 425` the attack. 425 being a block is the same fact `content/npcs/chaosdruid` records for NPC 520. |
| `HELLHOUND` | 6562 / 6566 / 6576 (by luck) | same, pinned | Not a correction. The observations happen to resolve correctly for 104, 105, 7256 and 7877, but only because 6562 (`FOX_ATTACK`) beats 6566 (`JACKAL_HIT`) on duration; the entry states it so it cannot drift. |
| `GNOME_BATTLE_MAGE`, `GOD_WARS_HELLHOUND` | - | new, reached by ID | See the ID table above. |

Two NPCs are deliberately left on the human fallback:

- **`Rocks` (101, 103)**, the dormant half of the Rock Crab. They have a single observation (1313)
  so the resolver bails, and it does not matter: a rock never fights. It transforms into a Rock
  Crab - which has a correct `ROCK_CRAB` entry - the moment a player walks past. See
  `content/npcs/rockcrab`.
- **`Deadly red spider` (3021)**, whose observations resolve to 5327/5328/5329 on their own. That is
  the `GIANT_SPIDER` entry's set exactly, so pinning it would add a line that changes nothing.

One knock-on worth stating: adding `BLACK_DRAGON` means **King Black Dragon** now suffix-matches it,
and so do the brutal dragons. The brutal dragons share the chromatic rig, so that is correct. The
KBD declares its own combat def in `content/npcs/kbd/KbdConfigsPlugin`, which takes `MonsterAnimationsPlugin`
off the animation path for it entirely; and because none of the new dragon entries carries a
`*Sound` field, the sound path is unchanged for it too.

## Combat sounds

`MonsterAnimationsPlugin` fills an NPC's `defaultAttackSound`/`blockSound`/`deathSound` from three
sources, in order: an explicit `attackSound` field in this file, then frame-sound data on the
resolved animation, then `WeaponSounds` for the shared human weapon animations. If all three miss,
`defaultAttackSound` stays -1 and `MeleeCombatStrategy`'s `if (def.defaultAttackSound > 0)` gate
means **nothing is attempted at all** - the monster is silent.

A probe of this cache (`diag/CombatSoundProbe.kt`, kept) shows how little the second source gives:

| | |
| --- | --- |
| sequences in the rev-228 cache | 12025 |
| with a non-empty `sounds` map | 1178 |
| with any non-null `soundEffects` | **0** |

and of the twenty combat animations this bestiary uses, **only two** carry any frame sound at all.
So in practice the explicit fields here are the *only* source for a monster that is not swinging a
human weapon, and an entry written without them is an entry that plays nothing.

**One of the two that does carry frame sound is a trap.** The chromatic dragon's melee claw (80) and
its dragonfire breath (81) both carry sound **3752**, which `Sound` names
`DRAGONSLAYER_DRAGONSTOMP3` - a Dragon Slayer II cutscene stomp. Left to the frame data, every green
dragon in the game would stomp when it clawed. The dragon entries name `Sound.DRAGON_ATTACK` (408),
`DRAGON_HIT` (410) and `DRAGON_DEATH` (409) explicitly, which take priority over it.

Sixteen entries gained sounds in the second bestiary pass, all from `Sound`'s named constants:

| Entry | attack / block / death | Constants |
| --- | --- | --- |
| the five adult dragons | 408 / 410 / 409 | `DRAGON_ATTACK`, `DRAGON_HIT`, `DRAGON_DEATH` |
| the four baby dragons | 405 / 407 / 406 | `BABYDRAGON_*` - the set the unreachable `BABYDRAGON` entry always had |
| `HELLHOUND`, `GOD_WARS_HELLHOUND` | 544 / 546 / 911 | `JACKAL_ATTACK`, `JACKAL_HIT`, `WOLF_DEATH` |
| `ICE_WARRIOR` | – / 530 / 529 | `ICE_WARRIOR_HIT`, `ICE_WARRIOR_DEATH` |
| `ICE_GIANT` | 448 / 451 / 450 | `GIANT_ATTACK`, `GIANT_HIT`, `GIANT_DEATH` |
| `FROG` | 842 / 844 / 843 | `TOAD_ATTACK`, `TOAD_HIT`, `TOAD_DEATH` |
| `GIANT_FROG` | 838 / 840 / 839 | `GIANT_TOAD_*` |
| `GIANT_SPIDER` | 3605 / 3607 / 3606 | `BIG_SPIDER_*` |
| `DEADLY_RED_SPIDER` | 3605 / 3607 / 3606 | `BIG_SPIDER_*`, on the same 5327/5328/5329 rig its observations already resolved to - a new entry, because "Deadly red spider" matches no existing key (the suffix rule would need `_GIANT_SPIDER`) and so had no way to reach a sound |

The hellhound's set is the one inference here and it is worth stating. There is no `HELLHOUND` sound
in the table - only `SKELETAL_HELLHOUND_*`, which is a different monster. Two of its three animation
constants name their own family outright (`JACKAL_HIT` 6566 and `WOLF_DEATH` 6576), so those two
roles take the sound of the same name; the attack, whose animation is `FOX_ATTACK` and for which no
fox sound exists, takes `JACKAL_ATTACK` from the family its own block belongs to.

`ICE_WARRIOR` has no attack sound because the table has none: 529 and 530 sit between `KEBBIT_HIT`
(528) and `ICEFIEND_ATTACK` (531) with no third member. Inventing a neighbour's id would be worse
than a silent swing.

**Still silent, and honestly so:** the ogre and the dagannoth. Neither has any entry in `Sound` -
`OGRE_SWIM`, `OGRE_BOW` and `OGRE_BELLOWS` are the only ogre clips and none is a combat sound, and
`DAGANNOTH` does not appear at all. They stay silent rather than borrowing another monster's voice.

Three more sound their own way and need nothing here: the outlaws, heroes and Black Heather swing
`HUMAN_SLASH_SWORD_ATTACK`, which `WeaponSounds` covers; the Elder Chaos druid's Wind Wave carries
`CombatSpell.WIND_WAVE`'s own cast and impact clips; and the battle mages play their god spell's
clip from `BattleMages.castSound`, because a monster attacking through its own `CombatStrategy`
never reads `defaultAttackSound` at all.

### The sweep across the rest of the bestiary

`diag/SpawnedMonsterSoundAudit.kt` (kept) answers the question that matters - not "how many entries
in this file lack a sound", most of which describe monsters nobody spawns, but **how many monsters
actually placed in the world are silent**. It constructs every plugin the way `PluginRepository`'s
own scan does, walks the real spawn list, and resolves each npc through the same sources
`MonsterAnimationsPlugin` uses.

At the start of the sweep: 520 attackable ids placed, **68 of them silent**. After it: **50**, and
what is left is left for a stated reason.

Eighteen more entries gained sounds, every one from a `Sound` constant naming the same creature.
Where the unqualified form also has an entry here, it already carried exactly those ids - so these
are the file's own convention applied to variants that had been missed, not new inventions:

| Entries | Took | Note |
| --- | --- | --- |
| `CHAOS_ELEMENTAL` | `CHAOS_ELEMENTAL_*` | its own dedicated set |
| `HARPIE_BUG_SWARM` | `HARPIEBUGSWARM_*` | its own set, in preference to the generic `SWARM_*` |
| `ABYSSAL_DEMON` | `ABYSSAL_*` (276/278/277) | the abyssal demon's own set, sitting between `SPECTRE_*` and `ANIMATED_*`; **not** the lesser demon's `DEMON_*` |
| `PYREFIEND` | `PYREFIEND_*` | new entry, sounds only in practice - it has a declared def |
| `CRAWLING_HAND` | `HAND_*` | the only "hand" monster in the game |
| `CAVE_SLIME`, `CAVE_LIZARD`, `NINJA_MONKEY` | `SLIME_*`, `LIZARD_*`, `MONKEY_*` | qualified forms of the stem creature |
| `ALT_GHOST`, `ARMED_ZOMBIE`, `NECROMANCER_ZOMBIE` | `GHOST_*`, `ZOMBIE_*` | same creature; the base entries already use these ids |
| `GIANT_CRYPT_RAT`, `BRINE_RAT`, `ICE_WOLF` | `RAT_*`, `WOLF_*` | likewise |
| `PIT_SCORPION`, `MARBLE_GARGOYLE`, `SCREAMING_BANSHEE`, `MOUNTED_TERRORBIRD` | `SCORPION_*`, `GARGOYLE_*`, `BANSHEE_*`, `TERRORBIRD_*` | likewise |
| `HOBGOBLIN` | `GOBLIN_*` | its animations **are** the goblin-family rig (164/165/167), which is what makes this the matching voice rather than a borrow |
| `SPIDER`, `ICE_SPIDER`, `POISON_SPIDER` | `SMALL_SPIDER_*`, `BIG_SPIDER_*` | new entries, on the rigs their declared defs already use |
| `SCORPIA` | `SCORPION_*` | new entry; her own attack animation (6254) is `SCORPION_ATTACK` |
| `LEECH` | `LEECH_HIT`, `LEECH_DEATH` | no attack member exists |

**Rejected, with the reason:**

- **`UNICORN` and `STARLIGHT`.** The table has `ANGER_UNICORN_*` and `GODWARS_UNICORN_*` and no base
  unicorn set. Choosing between two qualified variants would be a coin flip.
- **`CAVE_HORROR` from `JUNGLE_HORROR_*`** and **`FLESH_CRAWLER` from `CAVE_CRAWLER_*`** - different
  monsters, not qualified forms of each other.
- **`SHAEDED_BEAST` from `BEAST_*`** - too vague a stem to call a match.
- **The Barrows brothers.** `AHRIM_ATTACK`, `GUTHAN_ATTACK`, `TORAG_ATTACK` and `VERAC_ATTACK` exist,
  but each sits beside that brother's *armour* constants (`AHRIMS_AURA`, `GUTHAN_BREASTPLATE`,
  `VERAC_BRASSARD`), so they are set-effect procs rather than swings.
- **Ogre, ogre chieftain, dagannoth, nechryael, magic axe, infernal mage, kraken, suqah, jogre,
  aviansie, smoke devil, cockroaches, goat** - `Sound` has no combat set for any of them.

## The role audit, and the end of the attack/block swap

Three separate passes had found the same bug by hand - a *defend* sequence being played as the
attack - so the fourth time it was worth automating instead. `diag/AnimationRoleAudit.kt` (kept, and
the only one of the three audits that **asserts**) reads `Animation`'s own constant names as the
statement of what each sequence is for: an npc whose attack animation is named `..._DEFEND`, or
whose block is named `..._ATTACK`, is wrong on this project's own standard. No judgement, no
eyeballing.

Run across all **520 attackable ids placed in the world**, it found **15 rows covering 21 ids**, and
every one had an unambiguous fix sitting in its own observed set:

| Monster | Was | Now | Note |
| --- | --- | --- | --- |
| `THIEF` | **425 / 422** / 836 | **422 / 425** / 836 | 422 is `HUMAN_PUNCH` and 425 `HUMAN_DEFEND_COWARDLY`. The entry was added by the first bestiary pass with the two the wrong way round, and `content/npcs/chaosdruid` had already read the same rig correctly - "npc 520's observed set is [425, 710, 422, 836] - block, this, punch, death". Seven thief ids were parrying when they meant to punch. |
| `MAN` | **425 / 422** / 836 | **422 / 425** / 836 | The same entry contents, the same fix. |
| `AL_KHARID_WARRIOR` | resolved **388 / 390** | new: **390 / 388** / 836 | `HUMAN_SLASH_SWORD_ATTACK` / `_DEFEND`, both already in its observed set. |
| `BARBARIAN` | resolved **425 / 422** | new: **422 / 425** / 836 | Only npc 3262 - the other seventeen barbarians have a declared def and were already right. |
| `MENAPHITE_THUG` | resolved **398 / 838** | new: **395 / 398** / 836 | `HUMAN_AXE_SWING` / `HUMAN_BAXE_DEFEND`. 808 in its observed set is `HUMAN_STAND` (movement) and 838 is unnamed and unused. |
| `Dark wizard` (declared def, `content/npcs/darkwizard`) | attack **425**, block **717** | attack **711**, block **425** | Both wrong: it swung a hit reaction, and **answered being punched by casting Weaken** - 717 is `CAST_WEAKEN_WIZARD`. 711 is `UNARMED_MAGIC_SPELL_CAST`, in every dark wizard's observed set. Mostly cosmetic, since `DarkWizardCombatPlugin` plays each spell's own cast animation. |

The count is now **0 of 520**, which is why the audit asserts: the next monster wired with its roles
reversed fails the build rather than shipping. Three of these were also silent, because
`WeaponSounds` was being asked about a block animation - fixing the roles made them audible and took
the silent count from 50 to **47**.

**Not a swap, despite looking like one:** the Farmer's attack is **433**, which is `FARMER_ATTACK` -
correct. It is silent only because `Sound` has no farmer clip (`FARMERSFORK_STAB` is a weapon, not
the monster), and one Guard id (397) has no animation data in any source at all.

### The gap a `CombatStrategy` leaves, and where it has bitten

Everything above is about `defaultAttackSound` on the combat def. **A monster that attacks through
its own `CombatStrategy` or an `onNpcCombat` loop never reads that field** - only the three ordinary
strategies do - so giving such a monster an entry here sounds its *ordinary swing* and nothing else.
Its special attack stays silent unless its own code plays a clip.

That has now caught three things, each found only by asking the question directly:

| | Was | Now |
| --- | --- | --- |
| **Battle mages** | silent entirely - they only ever cast | `BattleMages.castSound`, the god spell's own clip, played from `BattleMageCombatStrategy` |
| **Dragonfire** | the claw was audible from `DRAGON_ATTACK`, the **breath was silent** | `Sound.DRAGONBREATH` (585), played from `DragonfireCombatStrategy` |
| **King Black Dragon** | all four breaths silent | its four breaths take the four constants named for them |

The KBD's set is worth recording because the mapping is exact rather than chosen. `Sound` carries
`LIGHTNINGBREATH` (584), `DRAGONBREATH` (585), `KINGICEBREATH` (586) and `TOXICBREATH` (587) in one
consecutive block - one constant named for the King Black Dragon itself - and the KBD has exactly
four breaths, whose projectiles are 393, 394, 395 and 396:

| Attack | Projectile | Sound |
| --- | --- | --- |
| fire | 393 | `DRAGONBREATH` 585 |
| poison | 394 | `TOXICBREATH` 587 |
| freeze | 395 | `KINGICEBREATH` 586 |
| shock | 396 | `LIGHTNINGBREATH` 584 |

All of these go through `playSpellSound`, which is the helper the casters in this tree already use:
an npc has no client of its own, so a sound it makes has to be played to the player it is aimed at.

### The full sweep of bespoke attacks

There turned out to be exactly **nine** sites in the codebase where a monster attacks with its own
code - four `CombatStrategy` registrations and five `onNpcCombat` loops - and they are enumerable,
so this did not have to stay a by-hand check after all:

| Site | Was | Now |
| --- | --- | --- |
| `ChaosDruidCombatStrategy`, `ElderChaosDruidCombatStrategy`, `DarkWizardCombatPlugin` | already sounded | unchanged |
| `BattleMageCombatStrategy` | silent | the god spell's own clip |
| `DragonfireCombatStrategy` | silent | `Sound.DRAGONBREATH` |
| `KbdCombatPlugin` | all four breaths silent | the four constants named for them |
| `SlayerCasterPlugin` | silent | `playAttackSound` |
| `WildernessBossCombatPlugin` | silent | `playAttackSound`, in both shared paths |
| `AhrimCombatPlugin` | silent | `Sound.AHRIM_ATTACK`, plus `AHRIMS_AURA` on the proc |

The last three go through a new `Npc.playAttackSound(target)` in `content/combat/PawnExt.kt`, which
plays whatever `defaultAttackSound` the def already carries. That is a straight extraction of a block
`MeleeCombatStrategy` and `RangedCombatStrategy` had a copy of each - they now call it too - and it
turns "sound this monster" into one line for any future bespoke attack.

Two of those were sounds this codebase had **already sourced and never played**: the aberrant spectre
carries `attackSound = 272` in its own combat def and had not made a noise since it was written, and
the Chaos Elemental had a full `CHAOS_ELEMENTAL_ATTACK/HIT/DEATH` set it could not reach. The rest of
the Wilderness bosses stay silent because `Sound` has no clip for them - but the plumbing is in, so an
entry added later works with no further code.

Ahrim is sounded in his plugin rather than through an entry in this file, and that is not
inconsistency: `AHRIM_ATTACK` has no `_HIT` or `_DEATH` sibling, so it cannot supply the three-role
triple an entry here needs, while it pairs exactly with his one attack - 2078 is
`Animation.HUMAN_AHRIMS_STAFF_ATTACK` and 1317 is that staff's clip.

**The check to run when wiring any monster with a bespoke attack:** does it call `playAttackSound` or
play a clip of its own? `SpawnedMonsterSoundAudit` cannot answer this - it reads the def, and these
monsters do not - but `grep -rn "setNpcCombatStrategy(\|onNpcCombat(" content` enumerates every one
of them in a second.
