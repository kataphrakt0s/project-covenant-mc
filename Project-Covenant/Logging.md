# Logging

## Identity

Logging develops forestry, tree felling, sustainable wood gathering, and direct offensive mastery. It adds specialized ways to interact with trees while preserving ordinary Minecraft chopping and wood recipes at all profession levels.

## Ability progression

### Tree Felling

| Rank | Minimum Logging level | Point cost | Intended effect |
|---|---:|---:|---|
| I | 1 | 1 | Initial tree-felling capability |
| II | 5 | 1 | Increased maximum logs affected |
| III | 10 | 1 | Further increased maximum logs affected |
| IV | 15 | 1 | Highest defined tree-felling capability |

- Breaking the base log of a supported tree with a valid axe triggers felling.
- Sneaking preserves normal single-block chopping.
- Connected logs belonging to the tree are felled together, subject to the rank's maximum.
- Modded trees and axes should be supported through tags or data-driven tree definitions.
- Normal leaf-decay behavior remains in place; Tree Felling does not introduce a separate forestry simulation.

Exact log limits, tree identification rules, and detailed tool/drop interactions remain to be balanced and tested.

### Offensive Mastery

| Rank | Minimum Logging level | Point cost | Direct player damage bonus |
|---|---:|---:|---:|
| I | 5 | 1 | 2% |
| II | 8 | 1 | 4% total |
| III | 11 | 1 | 6% total |
| IV | 14 | 1 | 8% total |
| V | 17 | 1 | 10% total |

Offensive Mastery is universal across Stalker, Reaver, and Weaver. It increases damage directly attributed to the player, including ordinary attacks and player-used skills. Damage dealt by companions, pets, Ballistas, and other summoned entities is excluded.

The 2% per-rank value and 10% maximum are established design values; exact damage event attribution and stacking behavior should be validated during implementation.

### Automatic Replanting

- **Unlock:** Logging Level 5
- **Cost:** 2 Profession Points
- **Type:** Single-purchase ability

When Tree Felling successfully removes a supported tree, Automatic Replanting attempts to plant the matching sapling at the base location.

- The matching sapling must be available in the player's inventory and is consumed on successful planting.
- Normal planting conditions must be valid.
- No saplings are generated for free.
- Modded tree-to-sapling relationships should be configurable through data-driven mappings.

## Point budget

| Ability | Defined investment |
|---|---:|
| Tree Felling (4 ranks) | 4 |
| Offensive Mastery (5 ranks) | 5 |
| Automatic Replanting | 2 |
| **Total currently defined** | **11** |

Nine Profession Points remain unallocated at the Level 20 cap.

## XP and compatibility

Logging XP should reward meaningful player-performed forestry actions. Repetitive low-value block breaking and automated tree farms do not award XP by default. Modded trees and tools should be supported through tags, data-driven definitions, and optional integrations where needed.

Ordinary tree chopping, wood collection, and vanilla wood recipes remain available without Logging investment.

Related: [[Profession Progression]], [[Experience and Reward Systems]], [[Mining]], [[Farming]], [[Crafting Profession]], [[Mod Compatibility]].
