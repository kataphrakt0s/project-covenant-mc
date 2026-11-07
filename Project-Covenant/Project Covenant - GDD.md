---
project: Project Covenant
type: design-document
version: 0.2
status: foundational
---

# Project Covenant — Foundational GDD

## Vision

A modular, Minecraft-native RPG experience built around the activities, systems, equipment, dimensions, and modding ecosystem that make Minecraft compelling. Covenant adds class-based combat skills, long-term character progression, profession mastery, item customization, and more structured world challenges without replacing the sandbox or withholding vanilla functionality.

> Minecraft mods should contribute to the RPG rather than be replaced by it.

Covenant should feel like Minecraft roleplay with deeper character development—not an ARPG dropped into a 3D world that ignores the game around it.

See [[Project Vision]] and [[Design Pillars]].

## Core gameplay loop

1. Explore the world, its dimensions, structures, resources, and activities.
2. Fight enemies, gather materials, craft, farm, log, mine, enchant, and interact with the world.
3. Earn Covenant character XP from combat and significant accomplishments, and independent profession XP from relevant activities.
4. Develop a combat build through class skills and develop practical mastery through professions.
5. Acquire, craft, salvage, and customize existing vanilla or modded equipment.
6. Explore more dangerous dimensions and encounters as progression gates allow.
7. Pursue deeper builds, profession capabilities, equipment customization, dungeons, bosses, and other long-term goals.

See [[Core Gameplay Loop]] and [[Progression Systems]].

## Vanilla-friendly design

- Vanilla mechanics remain functional and worthwhile. Covenant does not gate baseline Minecraft activities, tools, blocks, resources, ordinary recipes, or interactions behind RPG levels.
- Vanilla XP orbs and the vanilla XP/enchanting-level system remain independent of Covenant progression.
- Covenant progression rewards meaningful participation and mastery, primarily by unlocking new capabilities, interactions, and options.
- Numerical bonuses from Covenant systems should remain controlled. Professions can complement combat, but should not replace class skill progression or make vanilla equipment irrelevant.
- Modded content should be supported through data-driven definitions, tags, identifiers, and optional integrations rather than mandatory dependencies or hardcoded mod lists.

See [[Design Pillars]], [[Experience and Reward Systems]], [[Profession Progression]], and [[Mod Compatibility]].

## Character progression

- Maximum character level: **60**.
- A new character begins at level 1 with **1 skill point**.
- Characters earn **1 additional skill point for each level gained after level 1**, for **60 lifetime skill points at level 60**.
- Character XP comes primarily from combat. Significant discoveries, dungeon completions, bosses, and other major accomplishments can grant supplemental XP.
- Vanilla XP orbs do not grant Covenant character XP.
- Character progression does not grant traditional attribute points or a separate general-stat point budget.
- Character levels gate the first point invested in a skill. They do not gate later ranks of an already-unlocked skill.
- Character progression is persistent and server-authoritative.
- The initial character dimension route is Overworld 20, Nether 40, End 60.
  A one-time first-Wither milestone grants each eligible first-time contributor
  a full level past dimension gates, preserving percentage progress and
  respecting the global cap; see [[Character Progression]].

See [[Character Progression]], [[Combat Skill Progression]], and [[Experience and Reward Systems]].

## Combat classes and skills

The current playable class identities are **Stalker**, **Reaver**, and **Weaver**. Classes establish combat style and class resource; they do not impose rigid equipment restrictions.

Combat skills are learned and ranked through a class-specific skill tree. No more than **8 distinct skills** may have points allocated at one time, matching the maximum of eight combat keybinds. Individual skills have a maximum rank of **8 or 12**, selected according to the value and range of their rank upgrades. Rank 1 unlocks the skill; subsequent points improve its rank.

Prerequisites and genuinely mutually exclusive choices remain enforced. A crafted respec item refunds the allocated combat skill points. See [[Classes and Specializations]], [[Class Resources]], [[Abilities]], [[Skill Trees Brainstorm]], and [[Combat Skill Progression]].

In the progression slice, tree edits are pending until a bottom-right Apply
button commits them out of combat; closing discards unapplied edits. Current
playable combos cap at 12 ranks and other playable skills at 8. Rank 1 retains
prototype tuning. Aura passive strengths stay fixed, and current arrow shots
gain 2% of Rank 1 velocity per extra rank, reaching +14% at Rank 8 with native
speed-based damage retained. Further rank content remains separate design work.

## Profession progression

The initial profession roster is:

- **Mining**
- **Logging**
- **Farming**
- **Crafting** (with Enchanting as a subset, not a separate profession)

Every character can develop every profession, independently of class. Each profession begins at level 0, uses the shared profession XP curve, and has a default maximum level of **20**. Each level gained awards one Profession Point, for 20 points at level 20. Points purchase level-gated abilities and interactions, generally at variable costs of 1–3 points. Most profession abilities should eventually be obtainable; only genuine alternative specializations should lock one another out.

Profession levels automatically grant core recipe unlocks at selected milestones.
These recipe rewards are a future direction: additional profession-level
recipe milestones are deferred beyond the progression vertical slice. Its
initial milestones use purchased abilities and automatic Crafting affix-tier
access; Workbench and respec recipes remain ungated.
New abilities and interactions require deliberate Profession Point investment.
Profession abilities do not consume combat keybinds. For the prototype, one
Profession Respec resets **all four professions' allocations together**, refunds
each track's spent points, and preserves every track's XP and level. Its item
is more expensive than Combat Respec and uses the same two-second held-item
interaction out of combat. See [[Profession Progression]].

Profession XP comes from meaningful activity-specific actions, not vanilla XP orbs. Ordinary vanilla crafting does not award Crafting XP. Automation grants no profession XP by default; explicitly configured automation exceptions must have attribution and anti-exploit safeguards.

See [[Profession Progression]], [[Mining]], [[Logging]], [[Farming]], and [[Crafting Profession]].

## Experience and dimensional progression

Character XP and each profession's XP are separate progression tracks. Character
XP requires a selected combat class; professions can progress before class
selection. Activity and combat XP rewards are contextual and configurable.
Multiplayer combat XP is split equally among eligible damage and support
contributors, counting each player once, without a larger share for higher DPS
or the final blow. Passive aura support tied to actual combat qualifies. A
30-second participation window per player per enemy refreshes on qualifying
damage or support and is checked at death/payout using timestamps. Detailed
eligibility and attribution rules remain to be specified.

Dimensional progression gates configure the maximum level that each track can reach from XP earned in a given dimension. Rules can match exact dimension identifiers or dimension tags, with explicit priority and a configurable fallback. Unmatched dimensions default to the global cap: character level 60 and profession level 20. XP earned beyond the current dimension's gate is discarded rather than banked. Existing levels are never reduced when entering a lower-cap dimension.

See [[Experience and Reward Systems]], [[Dimensional Progression Gates]], and [[Progression Balance Configuration]].

## Statistics and equipment

The current direct-stat vocabulary is Health, Damage, Armor, Movement Speed, Attack Speed, and Dodge. Skill-related modifiers include Resource Cost, Cooldown, Cast Time, and Skill Duration. There is no traditional Strength/Dexterity/Constitution/Intelligence/Willpower/Ego attribute system and no attribute-point allocation.

Equipment is based on existing Minecraft and modded items. RPG modifiers are attached to individual item stacks through [[Affix Pages]] and apply only while equipped. Equipment, Affix Pages, and selected skill/profession effects provide character customization without requiring a replacement gear ecosystem.

See [[Statistics]], [[Attributes]], [[Stat Calculation Pipeline]], [[Equipment]], [[Affix Pages]], and [[Affixes and Rarity]].

## Crafting and equipment customization

Ordinary vanilla crafting remains unchanged. The Crafting profession covers meaningful specialized crafting activities, including Enchanting, affix manipulation, equipment crafting, and salvaging. The first Covenant Workbench now implements Add, Upgrade, Reroll, Remove and shared-storage salvaging, with affixed loot and profession integration; see [Itemization](../docs/ITEMIZATION.md). Further stations and content remain future work, and the prototype tuning is provisional.

See [[Crafting]], [[Crafting Profession]], [[Affix Pages]], and [[Affixes and Rarity]].

## World and combat progression

The world remains open to exploration. Regions, dimensions, encounter roles, and content establish threat; avoid indiscriminate player-level scaling of every vanilla or modded entity. Dimensional XP gates provide configurable progression pacing but do not prevent players from physically entering or exploring a dimension.

See [[World Progression]], [[Regions and Biomes]], [[Enemy Progression]], [[Encounter Design]], and [[Dimensional Progression Gates]].

## Endgame

The character level cap is 60; there is no level-100 or post-level-100 progression track in the current design. Long-term play comes from higher-risk dimensions and encounters, equipment and Affix Page tuning, deeper class builds, profession mastery, dungeons, bosses, challenge tiers, and world events.

See [[Endgame]], [[Challenge Tiers]], [[Dungeons]], [[Bosses]], and [[World Events]].

## Mod compatibility

Covenant is standalone. ATM11 is a compatibility test target, not a mandatory dependency. Vanilla and modded tools, blocks, ores, activities, entities, equipment, recipes, and dimensions should participate where configured. Prefer tags, identifiers, data-driven definitions, and optional integrations over hardcoded compatibility lists.

See [[Mod Compatibility]] and [[Data-Driven Content]].

## Technical principles

Server-authoritative gameplay; persistent per-player progression profiles; item-instance RPG data stored with stacks; effective statistics derived from sources; data-driven progression and reward definitions where useful; dimension rules resolved from identifiers/tags; dedicated-server safety; optional integrations; automated tests; and clear separation of character progression, profession progression, item data, and derived statistics.

See [[Technical Architecture]], [[Player Profiles]], [[Item Data and Persistence]], [[Networking and Authority]], and [[Testing Strategy]].

## Development sequence

The vertical slice includes the complete progression framework and a modest, playable first content selection for all four professions. It does not require fully populated profession trees or final XP tuning. See [[Progression Vertical Slice]], [[Development Roadmap]], and [[Implementation Priorities]].

The existing milestone sequence remains: [[Milestone 1 - RPG Foundation]] → [[Milestone 2 - Character Gameplay]] → [[Milestone 3 - Itemization and Crafting]] → [[Milestone 4 - World Progression]] → [[Milestone 5 - Class Depth and Content]] → [[Milestone 6 - Endgame]].
