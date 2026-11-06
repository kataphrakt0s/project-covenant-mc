# Milestone 1 — RPG Foundation

## Goal

Persistent, server-authoritative character and profession progression foundations, class resources, item-bound Affix Pages, and reliable stat calculation.

## Scope

Player profiles; Covenant character XP and level 1–60 progression; initial skill-point ownership; independent profession XP/levels/points for Mining, Logging, Farming, and Crafting; configurable XP curves/rewards; dimensional gate resolver; class definitions/resources; Affix Pages on stacks; equipment category resolution; equipped-item discovery; stat calculation pipeline; debug commands and tests.

## Acceptance proof

A player creates a character, gains Covenant character XP independently of vanilla XP orbs, receives skill points according to the level schedule, and retains progression across logout/restart. Each profession advances independently through its own activity XP. Dimension gates enforce configured track caps without lowering existing levels. A player equips an existing Minecraft item with an Affix Page, receives its modifiers while equipped, unequips without losing the page, transfers it to another player, and retains it after save/reload.

## Out of scope

Full production UI, final XP tuning, all profession abilities/recipes, complete class skill trees, affix generation, full crafting interface/workstation content, world generation, full mob/boss roster, and endgame content.

Related: [[Player Profiles]], [[Progression Systems]], [[Affix Pages]], [[Stat Calculation Pipeline]], [[Testing Strategy]].
