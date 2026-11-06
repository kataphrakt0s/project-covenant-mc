# Progression Systems

This page is the index and source-of-truth map for Covenant's progression architecture. The current direction is deliberately Minecraft-native: progression deepens the activities, dimensions, equipment, and interactions of Minecraft rather than replacing them with a disconnected ARPG loop.

## Core rules at a glance

| Track | Start | Default cap | XP source | Reward currency |
|---|---:|---:|---|---|
| Character | Level 1 | 60 | Primarily combat; significant milestones supplement | 1 skill point at level 1 and 1 per level thereafter |
| Mining | Level 0 | 20 | Meaningful mining/prospecting/extraction activity | 1 Profession Point per level gained |
| Logging | Level 0 | 20 | Meaningful logging/forestry activity | 1 Profession Point per level gained |
| Farming | Level 0 | 20 | Meaningful farming/agricultural activity | 1 Profession Point per level gained |
| Crafting | Level 0 | 20 | Meaningful enchanting, designated recipes, Covenant crafting/salvaging | 1 Profession Point per level gained |

Profession levels use one shared XP curve, but each profession retains its own XP, level, and Profession Point balance. Vanilla XP orbs are independent of all Covenant progression.

## Character combat build

- Character level 1 starts with one skill point; each subsequent character level grants one more. A level-60 character has 60 lifetime points.
- The first point invested unlocks a skill at Rank 1. Further points increase rank without additional character-level gates.
- Each skill has an individual maximum rank of 8 or 12.
- No more than eight distinct skills may have points allocated at once, matching the eight combat keybinds.
- Prerequisites and deliberate mutually exclusive choices are enforced.
- A crafted combat respec item resets combat allocations and refunds invested points.

See [[Character Progression]], [[Combat Skill Progression]], and [[Skill Trees Brainstorm]].

## Professions

The four initial professions are Mining, Logging, Farming, and Crafting. Enchanting is a subset of Crafting, not a separate profession. Every class can develop every profession; there are no profession slots or exclusive profession choices.

Each profession starts at level 0 and defaults to a maximum of 20. Each level gained awards one Profession Point. Selected profession milestones automatically unlock core recipes. Abilities and new interactions require deliberate point investment, with variable costs (typically 1–3 points), level gates, prerequisites, and only occasional mutually exclusive alternatives. Most offerings should eventually be obtainable. Profession abilities do not consume combat keybinds.

Profession respec resets one profession's allocations, refunds its points, preserves level and XP, and costs a crafted item more expensive than combat respec. Cross-profession requirements are allowed for specific recipes, but are not general ability/progression dependencies.

See [[Profession Progression]], [[Mining]], [[Logging]], [[Farming]], and [[Crafting Profession]].

## XP and rewards

Character XP is primarily combat-earned, with occasional significant rewards for bosses, dungeon completion, discoveries, and other milestones. Multiplayer combat XP is split among contributors; final eligibility and share calculation remain open.

Profession XP is contextual and action-based. Repetitive trivial actions should not be an efficient source. Direct player participation is the default. Automation grants no XP unless specifically configured with player/machine attribution and anti-exploit safeguards. Ordinary vanilla crafting does not grant Crafting XP.

See [[Experience and Reward Systems]].

## Dimensional gates

Every progression track can have its own maximum level per dimension. Rules match exact dimension IDs or dimension tags; exact IDs override tag matches, and tag priority is configurable. Unmatched dimensions use a fallback of character level 60 and profession level 20. XP earned beyond the current dimension's gate is discarded, not banked. Existing levels are never reduced when entering a lower-cap dimension.

See [[Dimensional Progression Gates]] and [[Progression Balance Configuration]].

## Vanilla-friendly boundaries

- Vanilla XP orbs and vanilla enchanting levels do not advance Covenant progression.
- Profession levels never gate baseline vanilla tools, blocks, resources, recipes, stations, or ordinary activities.
- Profession abilities primarily add capabilities and interactions. Passive efficiency/yield improvements are secondary and should be justified.
- Profession-based combat benefits are class-agnostic and modest; the class skill tree remains the main combat specialization system.
- Players can trade profession goods. Cross-profession recipe requirements are explicit exceptions rather than mandatory profession dependencies.
- Existing vanilla/modded gear remains relevant; Affix Pages enhance it rather than replace its identity.

## Vertical slice

The vertical slice includes the complete framework and a modest playable content selection for all four professions. It does not require fully populated future profession trees or finalized XP values. See [[Progression Vertical Slice]].

## Related source documents

- [[Project Covenant - GDD]] — overall vision and project boundaries.
- [[Character Progression]] — character XP, levels, points, and cap.
- [[Combat Skill Progression]] — skill ranks, allocation, and respec.
- [[Experience and Reward Systems]] — XP sources, multiplayer, and anti-exploit policy.
- [[Profession Progression]] — shared profession rules and economy.
- [[Mining]], [[Logging]], [[Farming]], [[Crafting Profession]] — per-profession design.
- [[Dimensional Progression Gates]] — dimension matching and progression caps.
- [[Progression Balance Configuration]] — configurable values and authoring expectations.
- [[Progression Vertical Slice]] — initial implementation scope and acceptance.
