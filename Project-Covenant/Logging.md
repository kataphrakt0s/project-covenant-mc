# Logging

Implementation status: Offensive Mastery is usable in the first profession
foundation. Natural log XP and bounded tree recognition are now implemented;
Tree Felling is implemented; Auto-Replant remains planned.
All numerical tuning remains provisional. See
[Profession foundation](../docs/PROFESSIONS.md) for runtime boundaries.

## Identity

Logging develops forestry, tree felling, sustainable wood gathering, and direct offensive mastery. It adds specialized ways to interact with trees while preserving ordinary Minecraft chopping and wood recipes at all profession levels.

## Ability progression

### Tree Felling

| Rank | Minimum Logging level | Point cost | Maximum logs per activation |
|---|---:|---:|---:|
| I | 1 | 1 | 4 |
| II | 5 | 1 | 8 |
| III | 10 | 1 | 16 |
| IV | 15 | 1 | 32 |

These are configurable prototype defaults and include the initially mined log.
A tree larger than the rank's limit can remain partially standing after an
activation. The profession catalogue's per-rank `blocks` field controls these
limits (1-64 inclusive); the shipped limits are implemented.

- Breaking the base log of a supported tree with a valid axe triggers felling.
- After a capped activation leaves part of the tree standing, breaking its
  lowest remaining recognized log can trigger another capped batch. Reuse the
  temporary tree evidence used by Logging XP; do not save unfinished trees.
  Chunk unload/reload, server restart or catalogue reload discards this evidence,
  so leftovers that cannot establish a fresh tree recognition need ordinary
  chopping. Tool classification uses `minecraft:axes`, with native per-log
  harvest validation. This continuation rule is implemented.
- Sneaking preserves normal single-block chopping.
- Connected logs belonging to the tree are felled together, subject to the rank's maximum.
- Modded trees and axes should be supported through tags or data-driven tree definitions.
- Each harvested log uses normal tool durability, exhaustion, native loot, and
  enchantment handling, following Vein Mining's per-block harvesting-cost rules.
  Stop when the axe breaks, leaving remaining logs intact; completed harvests
  keep their drops and eligible Logging XP.
- Stop the entire activation at the first protected, cancelled or otherwise
  failed additional harvest, preserving completed drops and XP, as for Vein Mining.
- Tree Felling has no cooldown, extra activation cost, or class-resource cost.
- Normal leaf-decay behavior remains in place; Tree Felling does not introduce a separate forestry simulation.
- Tree recognition must avoid tracking and saving every player-placed log.
  A guaranteed exclusion based on log-placement history is not part of the
  design.
- Recognize a tree at chopping time using **both** a bounded tree-shape check
  and nearby naturally grown, non-persistent leaves. Start from a base log on
  suitable growing ground, follow matching logs upward toward a canopy, and
  restrict sideways branching. Neither shape nor leaves alone is sufficient.
- Leafless trees may fail recognition and use ordinary chopping. Fall back to
  ordinary chopping whenever the bounded check cannot establish both conditions.
- This is a conservative current-block-state heuristic, not proof of a tree's
  origin. Naturally generated and sapling-grown trees can qualify; a constructed
  tree-like arrangement or a treehouse attached to a real trunk remains an edge
  case. Recognition runs only when triggered, with a fixed search budget.

The natural-XP implementation uses a 128-log, 32-block-height, four-block-radius
recognition limit and a 4,096 loaded-block-read budget per request. It remembers
recognized remaining logs transiently for sequential chopping, invalidates
edited positions and discards evidence on chunk/catalogue reload. See
[Profession activities](../docs/PROFESSIONS.md#natural-activity-xp) for fallback
limitations. Tree Felling uses the same evidence, retaining membership in the
recognized tree. Only a lowest remaining certified log can start another batch.
Harvest order is breadth-first across matching certified logs at the same
height or above; it never descends into lower branches or expands recursively.
No terrain is loaded and no unfinished harvesting job is saved.

### Offensive Mastery

| Rank | Minimum Logging level | Point cost | Direct player damage bonus |
|---|---:|---:|---:|
| I | 5 | 1 | 2% |
| II | 8 | 1 | 4% total |
| III | 11 | 1 | 6% total |
| IV | 14 | 1 | 8% total |
| V | 17 | 1 | 10% total |

Offensive Mastery is universal across Stalker, Reaver, and Weaver. It increases damage directly attributed to the player, including ordinary attacks and player-used skills. Damage dealt by companions, pets, Ballistas, and other summoned entities is excluded.

The 2% per-rank value and 10% maximum remain provisional tuning. The first
implementation uses the native incoming-damage event; automated checks cover
classless melee, attributed arrows/skill damage and owned-creature exclusions.
Client feel and balance remain for playtesting.

### Automatic Replanting

- **Unlock:** Logging Level 5
- **Cost:** 2 Profession Points
- **Type:** Single-purchase ability

When Tree Felling successfully removes a supported tree, Automatic Replanting attempts to plant the matching sapling at the base location.

- Replant only when the current Tree Felling activation removes the whole
  recognized tree. If the log cap or a broken axe leaves it incomplete, skip
  replanting and consume no sapling.
- Do not remember unfinished trees for a delayed replant. Larger tree farms
  may require manual replanting; this is an accepted trade-off of the felling
  limits.
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

Manually harvesting a qualifying sapling-grown tree has the same Logging XP
eligibility as harvesting an equivalent wild tree. Player-tended, renewable
tree plots are valid profession gameplay; exploring for new wild trees is not
required. Automated harvesting still grants no Logging XP by default. These
are implemented ordinary-harvest and Tree Felling eligibility rules.

Award configured Logging XP **once per eligible log successfully harvested**,
including each log removed by player-triggered Tree Felling. Tree Felling
accelerates harvesting without adding an XP multiplier; reward size follows
the eligible log count, not the number of drops. Ordinary manual harvesting
can earn XP without purchasing Tree Felling.

The **provisional reward is 20 XP per eligible log**, shared across all
supported tree species. Qualifying sapling-grown and wild trees use the same
value, and player-triggered Tree Felling awards it independently for each log
actually harvested. With the shared 100-XP requirement, five qualifying logs
earn Level 1. Keep reward mappings data-driven for modded trees and later
balance tuning; these values do not establish validated leveling time.

Ordinary tree chopping, wood collection, and vanilla wood recipes remain available without Logging investment.

Related: [[Profession Progression]], [[Experience and Reward Systems]], [[Mining]], [[Farming]], [[Crafting Profession]], [[Mod Compatibility]].
