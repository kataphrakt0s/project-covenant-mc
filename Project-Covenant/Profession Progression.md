# Profession Progression

## Purpose

Professions reward mastery of Minecraft activities. They add capabilities, interactions, and selected recipes without gating baseline vanilla gameplay or recreating systems already handled well by other mods.

The four initial professions are:

1. [[Mining]]
2. [[Logging]]
3. [[Farming]]
4. [[Crafting Profession]]

All characters can develop all four professions simultaneously. Professions are not class-restricted, there are no profession slots, and each profession has its own XP, level, and Profession Point balance.

## First implementation chunk

The shared foundation now includes the four-tab menu, saved paid-cost
allocations, atomic per-tab Apply, server-datapack catalogue and the held-use
all-profession respec. Logging's Offensive Mastery is the first usable ability;
the other sixteen authored nodes remain planned and unavailable. Automatic
activity XP is now implemented for ore/log/crop harvests, breeding and enchanting;
gathering abilities and Workbench/itemization gameplay remain deferred. Runtime format
and manual checks: [Profession foundation](../docs/PROFESSIONS.md).
All balance values remain provisional during implementation and playtesting.

## Profession menu

Radial slot **3** opens a dedicated **Professions** window, separate from
the combat **Skills** option in slot 1 and **Stats** in slot 2. The window has
four tabs: **Mining**, **Logging**, **Farming**, and **Crafting**. Show one
complete profession tree at a time, with its level, XP, and available Profession
Points above the tree. The complete selected tree fits on one page without
panning or scrolling.

This entry is available before combat class selection, matching profession
progression's class-independent access. Rendered UI and input feel still require
manual client playtesting.

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
- All professions use a shared XP curve, but their XP and points are tracked independently.
- The **provisional shared curve is 100 times the target level per transition**:
  100 XP from Level 0 to 1, 200 more from 1 to 2, through 2,000 from 19 to 20.
  All four tracks use this curve independently; reaching Level 20 takes
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
specialization ranks across all four professions together**, refunding each
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
the hold, and at completion, then resets all four professions atomically and
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
- [[Affix System and Equipment Customization]] — item-bound Covenant affixes and crafting rules.
- [[Salvaging]] — Covenant's chance-based equipment recovery system.

Related: [[Progression Systems]], [[Experience and Reward Systems]], [[Dimensional Progression Gates]], [[Progression Balance Configuration]], [[Progression Vertical Slice]].
