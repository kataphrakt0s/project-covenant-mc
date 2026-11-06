# Mod Compatibility

Covenant is standalone; ATM11 is a target compatibility test environment, not a dependency.

Support vanilla and modded equipment, tools, blocks, ores, entities, activities, recipes, and dimensions through extensible data-driven definitions, tags, identifiers, and optional adapters. Avoid unnecessarily replacing vanilla damage, combat, crafting, or progression mechanics. Keep the core mod independent of integrations.

## Progression compatibility

- Dimension gates must not rely on a hardcoded list of known dimensions. Support exact dimension identifiers and dimension tags, configurable tag priority, and fallback behavior.
- Unmatched dimensions use the configurable fallback, defaulting to character level 60 and profession level 20.
- Mining tools and eligible ores should be discoverable through tags or equivalent data-driven classification so modded tools/ores can opt in.
- Profession XP sources, reward values, recipes, and eligible activity definitions should be configurable through identifiers/tags where practical.
- Automated modded machines award profession XP only when explicitly configured; integrations must attribute rewards and prevent output-collection exploits.
- Optional compatibility integrations should be used when tags/configuration alone are insufficient. Do not add mandatory dependencies merely to support one modpack.

Develop on a clean current-version NeoForge instance, then validate against ATM11 on Minecraft 26.1.2.

Related: [[Equipment Categories]], [[Combat]], [[Enemy Progression]], [[Data-Driven Content]], [[Dimensional Progression Gates]], [[Progression Balance Configuration]], [[Technical Architecture]].
