# Exploration

## Agreed scope

Exploration and [[Fishing]] are implemented profession foundations, bringing
the roster to seven. Their skills remain deferred. Rendered checklist behavior
and real-client exploration still require manual playtesting.

Exploration is a special profession exception: progression is based on
achievements and unlocks, **not XP**. The shared profession XP curve does not
govern Exploration. Its milestone categories are **biome discoveries**,
**structure discoveries**, and **dimension discoveries**. The default milestone
pools are defined below. See [Profession foundation](../docs/PROFESSIONS.md) for
the implemented schema, observation policy, saved data and validation boundaries.

## Levels and Profession Points

- Starts at **Level 0**, with a default maximum of **Level 20**.
- Each qualifying one-time milestone grants **one Exploration level directly**
  and **one Exploration Profession Point**, up to the cap.
- Unspent points are retained for the profession's future skills, giving
  **20 lifetime points** at Level 20.
- Exploration has no XP balance or XP curve. Its menu presents completed and
  outstanding milestones in place of an XP bar.
- Exploration milestones contribute **no Building XP**; no XP equivalent is
  created for their direct level and point rewards.

## Dimension discovery pools

Exploration's global profession cap defaults to **Level 20**. Distribute its
available level rewards across dimension discovery pools:

| Discovery pool | Available level rewards |
|---|---:|
| Overworld | 10 |
| Nether | 8 |
| End | 4 |
| Total | 22 |

Players need **20 of the 22 available rewards** to reach Level 20. Both Nether
and End discoveries are necessary: Overworld plus Nether provides at most 18
levels, while Overworld plus End provides at most 14. Two rewards can be skipped.

The available rewards in each pool establish the progression route, without
separate mandatory-visit gates. Each qualifying milestone grants one level and
one point; define the pools to supply the intended number of level rewards.

These are discovery-pool rewards, independent of XP-based dimension caps and
the player's current total level. Modpacks can configure their own pools and
progression routes.

## Discovery milestones

The agreed scope focuses on discovering biomes, structures and dimensions.
Provide alternative discoveries within the dimension pools while distributing
their available level rewards as described above. No single elusive biome or
structure should be mandatory for completion. Balance the pools so common
discoveries do not make progression too quick.

The default catalogue contains 22 one-level milestones, detailed below.

### Overworld reward weighting

Overworld structure rewards are intentionally uneven. Discovering a **Woodland
Mansion** grants one full Exploration level, and discovering an **Ancient City**
grants another full level. Each is its own one-time milestone. Different village
types contribute to a shared collection goal for slower progression rather than
each type independently awarding a level.

The **final Overworld biome milestone requires discovering every eligible
Overworld biome**, not a fixed subset such as 30 biomes. Its target set must cover
all possible Overworld biomes for the configured progression. Keep that set
datapack configurable so modpacks can define the biomes relevant to their worlds.
The Overworld's ten level rewards are split into **five biome milestones** and
**five structure milestones**.

### Overworld biome milestones

Award one level at each of **10%, 25%, 50%, 75% and 100%** distinct-biome
completion of the configured eligible Overworld biome set. These five
milestones provide five of the Overworld's ten available level rewards. The
final milestone requires the entire set; it does not use a fixed biome count.

Use whole-biome requirements, rounding each percentage threshold upward. Each
milestone remains independently one-time; if a discovery reaches multiple
thresholds, apply the agreed simultaneous-completion rule. The Exploration tab
shows the discovered/required biome counts for each milestone.

Completing the final biome milestone earns its normal one-level reward. It does
not itself set the player's Exploration level to 20; the profession retains
22 available rewards toward its global Level 20 cap.

### Overworld structure milestones

Each of the following grants one level, providing five Overworld level rewards:

| Milestone | Requirement |
|---|---|
| Woodland Mansion | Discover one Woodland Mansion. |
| Ancient City | Discover one Ancient City. |
| Ocean Monument | Discover one Ocean Monument. |
| Villages | Discover every configured village variant. |
| Other landmarks | Discover six distinct structure families from a configured pool. |

Village variants collectively earn one level. Repeated visits to the same
variant do not advance its collection. The common-landmark collection likewise
counts distinct families, grouping variants according to its authored targets.

The default common-landmark pool contains these **eleven families**, with any
**six distinct families** completing its one-level milestone:

| Family | Default Minecraft structure IDs (namespace `minecraft`) |
|---|---|
| Mineshafts | `mineshaft`, `mineshaft_mesa` |
| Shipwrecks | `shipwreck`, `shipwreck_beached` |
| Ocean ruins | `ocean_ruin_cold`, `ocean_ruin_warm` |
| Ruined portals | `ruined_portal`, `ruined_portal_desert`, `ruined_portal_jungle`, `ruined_portal_mountain`, `ruined_portal_ocean`, `ruined_portal_swamp` |
| Desert pyramids | `desert_pyramid` |
| Jungle temples | `jungle_pyramid` |
| Swamp huts | `swamp_hut` |
| Pillager outposts | `pillager_outpost` |
| Trail ruins | `trail_ruins` |
| Trial chambers | `trial_chambers` |
| Strongholds | `stronghold` |

Only Overworld discoveries advance these Overworld goals. Variants in a family
count once collectively; the Nether ruined-portal variant is not an Overworld
target. The default village collection includes `village_plains`,
`village_desert`, `village_savanna`, `village_snowy` and `village_taiga`.

These structure IDs and their generation placements were verified against the
resolved Minecraft 26.1.2 / NeoForge 26.1.2.126 artifact. The families, members
and completion threshold remain datapack configurable.

### Nether milestones (provisional)

The Nether supplies eight one-time level rewards:

| Milestone | Requirement | Levels |
|---|---|---:|
| Nether Wastes | Discover this biome. | 1 |
| Soul Sand Valley | Discover this biome. | 1 |
| Crimson Forest | Discover this biome. | 1 |
| Warped Forest | Discover this biome. | 1 |
| Basalt Deltas | Discover this biome. | 1 |
| Nether Fortress | Discover one Nether Fortress. | 1 |
| Bastion Remnant | Discover one Bastion Remnant. | 1 |
| Nether biome collection | Discover every biome in the configured Nether set. | 1 |

The default collection contains the five biomes listed above. Its completion
reward is additional to their individual rewards, so the final newly discovered
biome completes two milestones and can grant two levels together, up to the cap.
This is provisional balance for the initial profession foundation; modpacks can
replace the definitions through the configured discovery pool.

### End milestones

The End supplies four one-time level rewards:

| Milestone | Requirement | Levels |
|---|---|---:|
| Enter the End | First server-confirmed entry into the End dimension. | 1 |
| Outer islands | Discover any biome in the configured outer-island biome set. | 1 |
| End City | Discover one End City. | 1 |
| End biome collection | Discover every biome in the configured End set. | 1 |

The default End biome collection contains The End, Small End Islands, End
Midlands, End Highlands and End Barrens. The outer-island set contains the four
biomes other than The End. These are biome-presence checks; the outer-islands
milestone does not add a travel-distance requirement. Entering the End and
reaching the outer islands can grant their rewards close together.

### Milestone types

Support both **individual discoveries** and **collection goals** in datapack
definitions. An individual discovery requires visiting one specified target.
A collection goal requires discovering a configured number of distinct targets
from a configured set of biomes or structure types. Repeated visits to the same
target do not add collection progress.

Examples of supported shapes include entering a particular dimension,
discovering five distinct biomes from a set, or visiting three distinct structure
types from a set. These illustrate the definition capabilities; the shipped
default milestones are specified in the tables above.

Track partial collection completion and display it clearly in the Exploration
tab, such as **3/5 biomes discovered**. Completing either milestone type grants
the same one-time level and point reward, up to the cap.

### Overlapping milestones

Each discovery advances **every matching milestone**. For example, a first
desert visit can complete an individual desert milestone and contribute to a
distinct-biome collection goal at the same time. Milestones do not consume or
reserve discoveries exclusively.

When one discovery completes several milestones, award all newly completed
milestones together, granting multiple levels and points up to the profession
cap. Each milestone remains a one-time reward. Occasional bursts of progression
are intentional and must be considered when balancing the default pool.

### What counts as discovery

Discovery triggers immediately when the player's **server-confirmed position**
enters the relevant biome, dimension or structure bounds. Arrival by flight or
teleport counts, including portals and travel to other players. Presence at the
destination is sufficient; the milestone does not require a waiting period or
an additional activity at the location.

This keeps exploration compatible with different travel methods and modded
transport. Portal networks and teleport access can make individual discoveries
easier; that is an accepted trade-off.

## Datapack configuration

The milestone pool is **server-datapack configurable** through
`data/covenant/progression/exploration.json` (one replacement catalogue). Modpack authors can
define their own Exploration progression by customizing the available discovery
milestones, including targets from their modded content. The shipped pool is a
default, not a mandatory progression route.

### Reloads and recorded discoveries

Keep a persistent per-player record of discoveries made while Exploration
tracking is active. New or changed datapack milestones recognize these recorded
discoveries; players do not have to revisit known locations. A successful
datapack reload may complete newly satisfied milestones and grant their levels
and points, up to the global cap.

Preserve earned levels, points and completed milestone claims when definitions
change. A previously claimed milestone does not pay again under the same ID,
even if its requirements change. This is ordinary progression within the new
format; older-save migration remains out of scope.

## Foundation acceptance criteria

- Add Fishing and Exploration to the class-independent profession roster and
  menu, bringing the total to seven. Retain points for future skills.
- Fishing pays its confirmed catch reward once and contributes accepted XP to
  Building under the existing conversion and cap rules.
- Exploration uses the 22 authored default milestones and no XP, with a Level
  20 default cap and no dimension XP gates or extra mandatory-visit gates.
- Server-confirmed discoveries update all matching milestones and their
  visible collection progress. Flight and teleport arrivals qualify.
- Save discoveries and claims through normal save/load, reconnect, death and
  dimension travel. Reloads recognize recorded discoveries without revoking
  earned levels or repeating claims.
- Ship server-datapack defaults that modpacks can replace, while keeping both
  professions' skills deferred to a future update.

Exploration skills are deferred to a future update. This expansion targets fresh
saves; older-save compatibility and migration are out of scope.

See [[Profession Progression]] for the shared profession foundation.
