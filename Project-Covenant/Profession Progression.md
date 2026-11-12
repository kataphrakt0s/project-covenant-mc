# Profession Progression

## Purpose

Professions reward mastery of Minecraft activities. They add capabilities, interactions, and selected recipes without gating baseline vanilla gameplay or recreating systems already handled well by other mods.

The seven professions are:

1. [[Mining]]
2. [[Logging]]
3. [[Farming]]
4. [[Crafting Profession]]
5. [[Building]]
6. [[Fishing]]
7. [[Exploration]]

All characters can develop all seven professions simultaneously. Professions are
not class-restricted and have no profession slots. Each has its own level and
Profession Points; Exploration earns those through milestones instead of XP.

[[Building]] is an implemented meta-profession that receives a flat **65% of raw XP**
actually credited to Mining, Logging, Farming, Crafting and Fishing, in addition to their own rewards.
Capped source professions and discarded overflow do not contribute. The original
40-combined-level pacing target is approximate and depends on how those levels
are distributed. Building will use the shared Level 0-20 curve and earn one
Building Profession Point per level, retaining unspent points for its future
skills. Building defaults to Level 20 in every dimension and supports the same
configurable dimension caps. Its profession foundation is implemented; its
three planned skill entries remain unavailable.

Building targets fresh saves; older-save compatibility, migration and
retroactive Building XP are out of scope.

## Seven-profession foundation

[[Fishing]] and [[Exploration]] join Mining, Logging, Farming, Crafting and
Building in the implemented **seven-profession foundation**. Their skills remain
deferred; rendered behavior still needs user playtesting.
Fishing is Building's fifth XP source at the existing **65% of actually
credited XP**, retaining fractional credit and all source/Building cap rules.
Exploration contributes no Building XP.

Fishing earns **50 profession XP per successful loot-bearing reel-in**, including
fish, junk and treasure, regardless of item count. Empty reels and hooking
entities grant none. It uses the shared Level 0-20 curve: **420 catches** reach
Level 20 when no XP is discarded by caps. Exploration is an explicit
exception to XP-based profession progression: it advances through achievements
and unlocks, rather than XP. It retains **Level 0-20**, with each qualifying
one-time milestone granting one level and one Exploration Profession Point
directly, up to the cap. Unspent points are retained for future skills. Its menu
shows completed and outstanding milestones instead of an XP bar. Milestones
focus on discovering **biomes, structures and dimensions**. Dimension discovery
pools supply **10 Overworld, 8 Nether and 4 End level rewards**: 22 available
rewards toward the Level 20 cap. Players can skip two rewards, but both Nether
and End discoveries are needed to reach maximum level. Progression follows the available pool rewards,
without separate mandatory-visit gates. The pools are **server-datapack configurable** so modpack authors can
define their own Exploration progression. Definitions support individual
discoveries and collection goals over distinct targets, with partial collection
progress visible in the Exploration tab. Discovery counts immediately on
server-confirmed entry into the relevant biome, dimension or structure bounds,
including arrival by flight or teleport. Each discovery advances every matching
milestone; simultaneous completions can grant multiple levels and points up to
the cap. The default milestones and foundation acceptance criteria are detailed
in [[Exploration]]. The implemented JSON schema is documented in [PROFESSIONS.md](../docs/PROFESSIONS.md).
Overworld structure rewards are uneven: Woodland Mansion and Ancient City
discoveries each award one full level, while village types contribute to a
shared collection goal. The final Overworld biome milestone requires every
eligible Overworld biome; its target set is datapack configurable.
Five Overworld levels come from biome completion milestones at **10%, 25%, 50%,
75% and 100%** of that set, with whole-biome requirements rounded upward. Five
Overworld levels come from discovering a Woodland Mansion, Ancient City and
Ocean Monument, all configured village variants, and six distinct structure
families from a configured common-landmark pool (one level per milestone).
That pool contains eleven families: mineshafts, shipwrecks, ocean ruins, ruined
portals, desert pyramids, jungle temples, swamp huts, pillager outposts, trail
ruins, trial chambers and strongholds. Variants count together within a family.
The provisional Nether pool awards one level for each of its five biomes,
one for a Nether Fortress, one for a Bastion Remnant, and one for completing
the configured Nether biome collection. The final biome can therefore award
two levels together, up to the cap.
The End awards one level each for entering the dimension, discovering an
outer-island biome, finding an End City, and completing the configured End
biome collection (including the central-island biome).
Exploration records player discoveries persistently. New or changed datapack
milestones recognize recorded discoveries on reload and can grant new rewards;
earned levels, points and completed milestone claims remain intact. A claimed
milestone ID never pays again merely because its definition changes.
Exploration has a global Level 20 default cap; dimension XP gates do
not apply. Modpacks control its route and pacing through the milestone pool.
Skills for both professions are deferred to a future update. This expansion
targets fresh saves; older-save compatibility and migration are out of scope.

## First implementation chunk

The shared foundation now includes the seven-tab menu, saved paid-cost
allocations, atomic per-tab Apply, server-datapack catalogue and the held-use
all-profession respec. Logging's Offensive Mastery, Vein Mining, Tree Felling,
Farming Scything/Automatic Replanting and all four Crafting nodes are usable;
eleven authored nodes (including Building's three entries) remain planned and unavailable. Automatic
activity XP is now implemented for ore/log/crop harvests, breeding and enchanting;
the Workbench/itemization loop and these four gathering abilities are implemented.
Other gathering abilities remain deferred. Runtime format
and manual checks: [Profession foundation](../docs/PROFESSIONS.md).
All balance values remain provisional during implementation and playtesting.

## Profession menu

Radial slot **3** opens a dedicated **Professions** window, separate from
the combat **Skills** option in slot 1 and **Stats** in slot 2. The window has
seven tabs: **Mining**, **Logging**, **Farming**, **Crafting**, **Building**,
**Fishing**, and **Exploration**. Narrow windows use two tab rows. Exploration
shows a paged discovery checklist instead of an XP bar/tree. Show one
complete profession tree at a time, with its level, XP, and available Profession
Points above the tree. The complete selected tree fits on one page without
panning or scrolling.

This entry is available before combat class selection, matching profession
progression's class-independent access. Rendered UI and input feel still require
manual client playtesting.

[[Building]] is the fifth tab, visible immediately and before class selection.
It shows level, XP including fractional credit, saved Building Profession
Points and the 65% source-XP explanation. Planned skills are visibly unavailable
for purchases. Players retain points for future skills.

### Point allocation

- Left-click adds one pending rank at its defined Profession Point cost.
- Right-click undoes pending purchases, never reducing a committed rank.
- Each profession retains its own local draft while switching profession tabs.
- **Apply** submits only the displayed profession's draft for server validation
  and commits its purchases. Other professions' drafts remain pending.
- Pending purchases grant no gameplay benefits. Closing the window discards all
  unapplied edits; reopening reads committed server state.
- Committed purchases require a profession respec to refund.

Players can browse trees and edit drafts during combat. **Apply** requires the
existing shared seven-second combat timer to have expired, using the same rule
as combat skill purchases. The server rechecks this when handling the request.
A combat-blocked Apply spends nothing and preserves the local draft while the
window remains open; it does not queue an automatic purchase for later.

## Shared progression rules

- Each profession starts at **Level 0** and has a default maximum of **Level 20**.
- Each level gained awards **1 Profession Point**. Reaching Level 20 therefore provides 20 points for that profession.
- All XP-based professions use a shared curve, with independent XP and points. Exploration instead earns levels directly from milestone claims.
- The **provisional shared curve is 100 times the target level per transition**:
  100 XP from Level 0 to 1, 200 more from 1 to 2, through 2,000 from 19 to 20.
  The six XP-based tracks use this curve; reaching Level 20 takes
  **21,000 total profession XP**. The larger units leave room for widely
  differing activity rewards. Activity values and final pacing remain to be tuned.
- Unspent Profession Points are retained.
- Ability costs generally range from 1–3 points according to their significance.
- A level requirement makes an ability or rank available for purchase; it does not grant the purchase automatically.
- Ranks must be purchased in sequence.
- Specializations require their parent ability where one exists.
- Independent branches do not lock one another out. Avoid unnecessary cross-profession prerequisites.
- There is no global limit on the number of profession abilities a player can learn.

## Broad access, selective mastery

The intended 20-point budget should allow broad access to a profession's foundational mechanics. Meaningful choices should primarily concern how deeply a player invests in optional upgrades, rather than locking players out of entire branches.

The currently defined vertical-slice options in each profession fit within 20 points. Future additions may create modest over-budget choices, but content should not be padded merely to force specialization.

## Automatic rewards and purchased abilities

Profession level milestones may automatically unlock recipes or access to additional content. Abilities and new interactions require deliberate Profession Point investment. Keep these two reward types distinct in both design and implementation.

**Additional profession-level recipe milestones are deferred beyond the
progression vertical slice.** Its purchased abilities and automatic Crafting
affix-tier access at Levels 5/10/15/20 provide the initial milestones. The
Covenant Workbench, Combat Respec, and Profession Respec recipes remain ungated.
Automatic recipe rewards remain a future design direction, not required
prototype content or an additional recipe-unlock framework for this slice.

Profession rewards can include passive or conditional effects, contextual interactions, active abilities that do not consume combat keybinds, recipes, and workstation operations.

## Vanilla and mod compatibility principles

- Profession levels never gate baseline vanilla tools, blocks, resources, recipes, stations, or ordinary activities.
- Professions should enhance existing Minecraft and modded gameplay rather than recreate or replace systems that other mods already handle well.
- Support modded content through tags, identifiers, data-driven definitions, and optional integrations where practical.
- Optional integrations must not become mandatory dependencies.
- Direct player participation is the default for Profession XP. Automated production does not award XP unless explicitly configured with appropriate attribution and anti-exploit safeguards.
- Vanilla XP orbs do not award Profession XP.

## Profession XP principles

Profession XP is contextual and tied to meaningful actions associated with the profession. Repetitive trivial actions should not be efficient progression sources. Ordinary vanilla crafting does not award Crafting XP; selected enchanting activities, specialized Covenant workstation operations, and optional mod integrations may do so.

The provisional shipped curve requires **100 × L XP to reach Level L from
Level L-1**. These are per-level costs, not cumulative thresholds; total XP
from Level 0 to L is **50 × L × (L+1)**.

| Target level | XP for this transition | Total XP from Level 0 |
|---|---:|---:|
| 1 | 100 | 100 |
| 5 | 500 | 1,500 |
| 10 | 1,000 | 5,500 |
| 15 | 1,500 | 12,000 |
| 20 | 2,000 | 21,000 |

For the existing `profession_requirements` format indexed by current level,
the provisional shipped Level 0-20 default is:

```json
[100, 200, 300, 400, 500, 600, 700, 800, 900, 1000,
 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800, 1900, 2000]
```

This is provisional prototype balance. Natural activity hooks, trusted server
awards and operator testing commands use this curve. Action values,
cooldowns, anti-farming thresholds, and final level pacing remain open for
tuning. See [[Experience and Reward Systems]].

## Respec

For the **prototype, one Profession Respec resets purchased abilities and
specialization ranks across all seven professions together**, refunding each
profession's spent points to that profession. Preserve every profession's XP
and level. Combat allocations and progression are unaffected.

Craft **one Profession Respec** using an ordinary **shapeless crafting-table
recipe** of **four paper, four lapis lazuli, and one emerald**. This exceeds
Combat Respec's four-paper/one-lapis cost. For the prototype, crafting it has
**no profession-level or purchased-ability requirement** and grants no Crafting
profession XP. Materials provide the restriction, allowing early allocations
to be corrected without leveling other professions first.

Use it **the same way as Combat Respec**: hold right-click with the item for
**two seconds (40 ticks)** while out of combat. Use the existing shared
**seven-second combat timer**. There is no profession selector or respec button
in the Professions GUI. At least one profession must have committed allocations
to refund. The logical server validates eligibility when use starts, during
the hold, and at completion, then resets all seven professions atomically and
consumes **one item only on success**. Early release, combat interruption,
or a rejected/invalid completion spends nothing.

The Profession Respec item and recipe are implemented in the first foundation
chunk; all tuning and recipe balance remain provisional.

## Current investment summary

These totals cover only the abilities and ranks currently defined. They are not intended to imply that all future profession content has been designed.

| Profession | Currently defined point investment | Points remaining at Level 20 |
|---|---:|---:|
| [[Mining]] | 16 | 4 |
| [[Logging]] | 11 | 9 |
| [[Farming]] | 14 | 6 |
| [[Crafting Profession]] | 9 | 11 |

## Profession documents

- [[Mining]] — prospecting, ore extraction, processing, transfiguration, and defensive mastery.
- [[Logging]] — tree felling, offensive mastery, and automatic replanting.
- [[Farming]] — cultivation, crop harvesting, animal husbandry, and vitality.
- [[Crafting Profession]] — equipment customization and salvaging.
- [[Building]] — implemented meta-profession foundation; reach and cosmetic material skills remain planned.
- [[Fishing]] — implemented profession foundation; successful fishing awards XP; skills deferred.
- [[Exploration]] — implemented achievement/unlock-based foundation; no XP; skills deferred.
- [[Affix System and Equipment Customization]] — item-bound Covenant affixes and crafting rules.
- [[Salvaging]] — Covenant's chance-based equipment recovery system.

Related: [[Progression Systems]], [[Experience and Reward Systems]], [[Dimensional Progression Gates]], [[Progression Balance Configuration]], [[Progression Vertical Slice]].
