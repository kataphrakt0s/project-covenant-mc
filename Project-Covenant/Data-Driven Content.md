# Data-Driven Content

Use data-driven definitions where concrete content benefits from authoring/configuration outside code: classes, abilities, skill rank caps/effects, profession definitions and abilities, recipes, XP rewards, progression curves, dimensional gates, affixes/rarity, region progression, enemy archetypes/modifiers, and rewards.

Progression content should support stable identifiers and tags so vanilla and modded dimensions, tools, ores, blocks, recipes, and activities can participate without hardcoded compatibility lists. Dimension gates support exact identifiers, tags, explicit priority, and fallback values.

For the planned progression slice, author character XP requirements, enemy XP
rewards, and dimension-cap rules in server datapack JSON, with built-in defaults
and datapack overrides. Loading, reload, validation, and client presentation
remain server-owned. Balance changes preserve earned levels, points,
allocations, and raw XP; new requirements apply to future XP awards, so the
displayed percentage can change. Exact field/path schema and validation/failure
handling remain open in [[Progression Balance Configuration]].
Profession implementation design is in progress in the individual profession
notes and [[Progression Vertical Slice]].

The planned Covenant equipment rarity system uses server datapack definitions
and item/tag-based opt-in rules. Modpack developers and other mods must be able
to author participation and rarity content without a per-material compatibility
list in Covenant code. See [[Affixes and Rarity]].

Avoid generic frameworks without concrete requirements. Keep configuration validation clear and make invalid entries fail safely with actionable diagnostics.

Related: [[Technical Architecture]], [[Classes and Specializations]], [[Abilities]], [[Progression Systems]], [[Profession Progression]], [[Dimensional Progression Gates]], [[Progression Balance Configuration]], [[Affixes and Rarity]], [[World Progression]].
