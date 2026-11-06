# Technical Architecture

Build a maintainable standalone NeoForge mod. Verify exact Minecraft 26.1.2 NeoForge/Java requirements against the official MDK; do not reuse old-version API assumptions.

Boundaries:

- Character progression, class state, skill points/ranks, and independent profession tracks live in persistent player profiles.
- Item-instance RPG data lives on item stacks.
- Effective statistics are derived from base values and applicable modifiers.
- Gameplay-critical progression, XP awards, dimension-gate checks, unlocks, respecs, and combat outcomes are server-authoritative.
- Planned combat contribution timestamps are enemy-owned server-only runtime
  state. Qualifying events refresh them; death/payout checks their 30-second
  recency without recurring expiry scans. They are never saved or synced and
  reset on enemy unload/removal or server restart. Awarded player progression
  remains persistent; see [[Experience and Reward Systems]].
- Progression definitions, XP rewards/curves, profession abilities/recipes, and dimensional rules are data-driven/configurable where useful.
- Character XP requirements, enemy XP rewards, and dimension-cap rules in the
  planned slice are server datapack JSON with built-in defaults and overrides;
  schema and reload behavior follow [[Progression Balance Configuration]].
- Dimension compatibility uses identifiers and tags with configured priority/fallback, not a hardcoded mod-dimension list.
- Optional integrations remain separate from core.

Keep character XP, each profession XP track, vanilla XP, skill points, Profession Points, and item-owned Affix Pages as distinct concepts and data sources.

Related: [[Player Profiles]], [[Item Data and Persistence]], [[Stat Calculation Pipeline]], [[Progression Systems]], [[Data-Driven Content]], [[Networking and Authority]], [[Mod Compatibility]], [[Testing Strategy]].
