# Mod Compatibility

Covenant is standalone; ATM11 is a target compatibility test environment, not a dependency.

Support vanilla and modded equipment, tools, blocks, ores, entities, activities, recipes, and dimensions through extensible data-driven definitions, tags, identifiers, and optional adapters. Avoid unnecessarily replacing vanilla damage, combat, crafting, or progression mechanics. Keep the core mod independent of integrations.

## Apotheosis target (planned)

Full Apotheosis compatibility is an eventual project target. Spawners must have
a place in Covenant's progression model; do not design combat XP around a
permanent exclusion of spawner-produced mobs. Eligible combat mobs produced by
spawners are intended to participate, subject to Covenant's normal class,
contribution, and dimension-gate rules.

The first progression slice remains standalone. This direction does not add an
Apotheosis dependency, integration adapter, or claim of tested compatibility.
Verify the target version and relevant APIs when that integration is designed.
Exact feature interactions and compatibility acceptance criteria belong to a
later dedicated integration pass. In the first slice, eligible spawner mobs
award normal configured XP, using the same class, contribution, and dimension
rules as equivalent naturally spawned mobs. There is no spawn-origin XP penalty
or required spawn-provenance tracking. See [[Experience and Reward Systems]].

## Progression compatibility

- Dimension gates must not rely on a hardcoded list of known dimensions. Support exact dimension identifiers and dimension tags, configurable tag priority, and fallback behavior.
- Unmatched dimensions use the configurable fallback, defaulting to character level 60 and profession level 20.
- Mining tools and eligible ores should be discoverable through tags or equivalent data-driven classification so modded tools/ores can opt in.
- Profession XP sources, reward values, recipes, and eligible activity definitions should be configurable through identifiers/tags where practical.
- Automated modded machines award profession XP only when explicitly configured; integrations must attribute rewards and prevent output-collection exploits.
- Optional compatibility integrations should be used when tags/configuration alone are insufficient. Do not add mandatory dependencies merely to support one modpack.

Develop on a clean current-version NeoForge instance, then validate against ATM11 on Minecraft 26.1.2.

Related: [[Equipment Categories]], [[Combat]], [[Enemy Progression]], [[Data-Driven Content]], [[Dimensional Progression Gates]], [[Progression Balance Configuration]], [[Technical Architecture]].
