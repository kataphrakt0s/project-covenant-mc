# Progression Vertical Slice

## Goal

Deliver a complete, playable progression framework that demonstrates both Covenant combat progression and Minecraft activity mastery while remaining vanilla-friendly. The slice includes all four initial professions, but only a deliberately modest first selection of actual profession content.

## Included systems

### Character progression

- Covenant character level 1–60.
- One starting skill point at level 1, then one additional skill point per character level (60 total).
- Combat XP earned primarily from hostile combat, supplemented by major milestones.
- Separate from vanilla XP orbs.
- Persistent, server-authoritative profiles.

### Combat skill progression

- Existing Stalker, Reaver, and Weaver trees.
- Diagram level requirements gate initial skill investment only.
- First point unlocks Rank 1; subsequent points increase rank.
- Per-skill maximum rank 8 or 12.
- Maximum eight distinct skills with points allocated at once.
- Server-enforced prerequisites and mutual exclusions.
- Crafted respec item that refunds combat skill allocations.

### Professions

All four are included:

- Mining
- Logging
- Farming
- Crafting (Enchanting is a subset)

Each starts at level 0, defaults to level 20, shares a profession XP curve, and has independent XP/points. One Profession Point is earned per level. The slice implements a modest playable selection of level-gated abilities, variable-cost investments, a few genuine competing choices, and automatic recipe unlocks at selected profession milestones.

The slice does not require all 20 levels to be fully populated or every future ability to be designed. Most eventual profession options should be obtainable, so the initial selection should demonstrate broad mastery rather than artificially restrictive builds.

### Profession respec

A crafted item resets one selected profession's ability/specialization allocations, refunds that profession's points, and preserves its XP/level. Its cost is higher than combat respec. Exact recipe remains to be determined.

### XP and dimensions

- Separate Covenant character XP and profession XP tracks.
- Contextual action-based profession rewards; ordinary vanilla crafting excluded from Crafting XP.
- Direct player activity is default; automation disabled unless specifically configured with attribution/anti-exploit safeguards.
- Multiplayer combat XP split among contributors; final eligibility/math remains open.
- Dimension gates use exact IDs or tags, configurable priority, and fallback (character 60, profession 20).
- XP beyond a dimension cap is discarded, not banked; existing levels never decrease.

### Balance/configuration

- Configurable XP curves/rewards, track caps, and dimensional rules.
- Data-driven identifiers/tags for modded content and dimensions.
- All classes can access all professions.
- No primary attribute point system.
- Profession effects are class-agnostic and modest when they overlap with combat.
- Profession abilities do not consume combat keybinds.

## Explicit boundaries

- Do not replace or gate vanilla Minecraft XP, enchanting levels, baseline crafting, mining, logging, farming, tools, blocks, resources, or ordinary recipes.
- Do not create a mandatory profession choice or profession slot system.
- Do not introduce a second character attribute-point economy.
- Do not hardcode a dimension compatibility list.
- Do not require every future profession ability, recipe, or custom workstation to exist in this slice.
- Specialized Covenant workstations for advanced affix crafting and salvaging are planned; exact roster and delivery milestone remain open.

## Acceptance criteria

1. Character XP advances character level independently of vanilla XP orbs and awards the correct skill-point schedule.
2. Character level gates the first skill point only; further ranks are available without additional character-level gates.
3. The server prevents allocation in more than eight distinct skills and enforces rank caps, prerequisites, and exclusions.
4. Combat respec refunds invested points without reducing character level/XP.
5. Each of the four professions independently gains XP, levels, and Profession Points from configured meaningful activities.
6. Profession rewards distinguish automatic recipe unlocks from purchased abilities.
7. Each profession has enough playable initial content to demonstrate its interaction style and at least one meaningful investment decision.
8. Profession respec resets one profession only and retains its XP/level.
9. Dimension gates match exact identifiers and tags, resolve priorities deterministically, use fallback for unmatched dimensions, and never reduce existing levels.
10. XP past a dimension gate is not banked for later dimensions.
11. Modded tools/ores/dimensions and configured activity sources can participate without hardcoded mod support.
12. Vanilla XP and baseline vanilla gameplay remain unaffected.
13. Character and profession progression persist across logout/restart and remain server-authoritative in multiplayer.

## Deferred beyond the slice

Fully populated profession trees; final numerical XP/ability balance; all possible modded dimension/activity integrations; complete future workstation roster; every affix manipulation/salvage recipe; all future profession branches; and complete endgame content.

Related: [[Progression Systems]], [[Project Covenant - GDD]], [[Character Progression]], [[Combat Skill Progression]], [[Experience and Reward Systems]], [[Profession Progression]], [[Dimensional Progression Gates]], [[Progression Balance Configuration]].
