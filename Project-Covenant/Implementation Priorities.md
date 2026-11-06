# Implementation Priorities

Immediate priority: [[Milestone 1 - RPG Foundation]]. Establish persistent, server-authoritative character and profession progression, separate XP tracks, configurable XP definitions, and dimension-gate resolution while preserving vanilla XP.

Next: [[Milestone 2 - Character Gameplay]] for combat skill-point allocation, ranks, eight-skill limit, prerequisites, exclusions, and respec. Build a playable vertical slice of all four professions through [[Milestone 3 - Itemization and Crafting]] and [[Milestone 5 - Class Depth and Content]]; each profession needs a modest but meaningful set of recipes and chosen abilities rather than a complete future tree.

Architectural priorities: define [[Stat Calculation Pipeline]] early; keep profile and item data separate; keep vanilla XP separate from Covenant XP; use data-driven identifiers/tags for dimension, item, tool, ore, activity, and recipe compatibility; make core independent of integrations; develop against clean current NeoForge before ATM11.

Do not introduce attribute points, mandatory profession choices, baseline vanilla activity gates, or hardcoded mod-dimension lists. Defer final XP numbers and large profession content catalogs until playtesting provides evidence.
