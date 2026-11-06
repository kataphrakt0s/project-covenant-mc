# Open Questions

This file contains decisions that remain genuinely unresolved. Established progression rules are recorded in [[Progression Systems]], [[Character Progression]], [[Combat Skill Progression]], and [[Profession Progression]].

## Character and combat progression

- Playtested character-level pacing. The accelerating
  prototype curve, reference XP scale, and opening/final-level anchors are
  agreed in [[Character Progression]], with Classic WoW as the pacing-feel target.
  Observe kills per minute and time per level before treating that feel as
  validated; values remain configurable. The initial repeat-boss reference is
  a Wither kill at 2,000 base character XP before sharing/caps.
- Exact XP rewards by enemy/encounter category and milestone.
- Detailed damage/support attribution to enemies/encounters. Eligible contributors receive
  equal shares, counting each player once, with a refreshing 30-second window
  per player per enemy checked at payout. Each ordinary share rounds up to
  whole XP before caps. Support-only participation, including
  passive aura support tied to actual combat, already qualifies in the first slice; see
  [[Experience and Reward Systems]].
- Individual rank upgrades for each skill, using current prototype values at
  Rank 1 and minor, flavorful improvements under [[Combat Skill Progression]].
  Aura passive strengths stay fixed across ranks; their activation/coverage
  upgrades remain to be authored.

## Profession progression

Profession abilities and their requirements are being developed separately by
the user. Keep the content questions below pending that input rather than
selecting abilities or resolving their requirements in this planning session.
The established shared-track foundation can still be planned independently.

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

- Profession dimension caps and future modpack route overrides. The initial
  character route is Overworld 20, Nether 40, End 60, with the first-Wither
  level-reward exception; see [[Dimensional Progression Gates]].
- Server datapack JSON resource paths/field schema, validation/error reporting,
  reload failure handling, and tag-priority syntax. Balance changes preserve
  earned levels, points, allocations, and raw XP; percentages can change.
  Datapacks
  are the agreed source for character XP requirements, enemy rewards, and
  dimension-cap rules; see [[Progression Balance Configuration]].
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
