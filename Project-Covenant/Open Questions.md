# Open Questions

This file contains decisions that remain genuinely unresolved. Established progression rules are recorded in [[Progression Systems]], [[Character Progression]], [[Combat Skill Progression]], and [[Profession Progression]].

## Character and combat progression

- Exact character XP curve, required XP per level, and intended time-to-level pacing.
- Exact XP rewards by enemy/encounter category and milestone.
- Contributor eligibility for shared combat XP, support credit, participation window, and exact split formula.
- Which individual skills use an 8-rank cap versus a 12-rank cap.
- Per-rank effect scaling for each skill.
- Combat respec item recipe, crafting station, and acquisition pacing.

## Profession progression

- Exact shared profession XP curve and level pacing.
- Action-specific XP rewards, contextual significance rules, and anti-repeat/exploit safeguards for each profession.
- Vertical-slice recipe unlock milestones and initial ability selection for Mining, Logging, Farming, and Crafting.
- Profession ability costs, level gates, prerequisites, rank exceptions, and mutually exclusive branches.
- Exact Mining Prospecting output: nearby deposit awareness, directional/range information, ore detection, or another presentation. The interaction is set: right-click a suitable stone/deepslate block while holding any valid mining tool.
- Exact rank scaling and target limit for Mining's ore-only Vein Mining ability. Its interaction is set: active during ordinary ore mining; sneaking performs normal single-block mining.
- Concrete Logging and Farming ability content, and the final Crafting branches/content.
- Profession respec item recipe, station, relative cost, and whether any additional restrictions apply.
- Default and exceptional automation XP integration rules, attribution model, and anti-exploit details.

## Dimensional gates and configuration

- Actual dimension-by-track cap values for the intended progression path. Illustrative Overworld/Nether/End values are not final.
- Final balance-configuration format, reload behavior, validation/error reporting, and tag-priority syntax.
- Which dimensions/tags receive curated defaults in the initial configuration. Unmatched-dimension fallback is established: character 60, professions 20.

## Crafting and itemization

- Covenant workstation roster, recipes, materials, user interactions, and which stations belong in the vertical slice versus later releases.
- Affix acquisition, crafting, rerolling, locking, upgrading, extraction, salvage returns, and associated costs.
- Crafting profession's exact relationship to vanilla enchanting and future magical item customization.
- Exact cross-profession recipes and their requirements. The Profession Respec recipe requiring level 10 in all four professions is an example, not yet finalized.

## Statistics and combat balance

- Final launch stat IDs/units, modifier order and caps, and diminishing-return behavior where applicable. The traditional primary-attribute system and attribute points are out of scope.
- Final balance of profession-derived combat effects relative to class skills and equipment.
- Class specialization/talent structure beyond the current class-specific skill trees, if any; no separate talent-point economy is currently established.

## Itemization detail

- Rarity tiers, affix pools/tiers, Affix Page acquisition/generation, and unique/exceptional properties.
- Exact affix locking, extraction, rerolling, replacing, upgrading, and salvage/recovery rules.

## World and endgame

- Region progression map, danger signaling, encounter reward design, and how dimensions/regions map onto world content.
- Challenge-tier access, modifiers, reward scaling, and repeatable endgame loop within the level-60 character cap.
- Additional region/dungeon/boss scope and long-term rewards.

Related: [[Design Decisions]], [[Progression Systems]], [[Development Roadmap]].
