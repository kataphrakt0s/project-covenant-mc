# Building

## Implementation status

Building's meta-profession foundation is implemented alongside Mining,
Logging, Farming and Crafting. Progression, fractional credit, persistence,
dimension caps and the fifth menu tab are available; its skills remain
unimplemented. See [Profession foundation](../docs/PROFESSIONS.md) for the
runtime contract. Rendered client behavior still needs manual playtesting.

This work targets **fresh saves**. Compatibility with older profession save
formats, migration and retroactive Building catch-up are out of scope. New
players start Building at Level 0 with zero XP and points. Normal persistence
of Building progress within the new format remains required.

## Purpose and progression target

### Fishing source

The seven-profession foundation includes [[Fishing]] as Building's fifth
source profession. It contributes the same **65% of actually credited XP**,
using the existing source-cap, fractional-credit and Building-cap rules.
[[Exploration]] contributes nothing because its milestones grant levels and
points directly without XP. This adds another route to Building progression;
the conversion rate and Level 20 default cap stay the same. Fishing is implemented alongside the original four activity sources.

Building rewards progress in Mining, Logging, Farming, Crafting and Fishing. Its agreed
conversion rate is a **flat 65% of raw profession XP**, rather than a conversion
based on levels or percentage progress through a level. A qualifying 100-XP
profession award contributes 65 Building XP in addition to the source award;
it does not reduce the source profession's reward. Building does not feed itself.
Fractional conversion credit must be retained so that splitting the same total
XP into smaller awards does not change the effective 65% rate.

Only XP actually credited to the source profession contributes. Once a source
profession reaches its applicable cap, its activities stop granting Building
XP. XP discarded by a source cap, including overflow from the award that
reaches the cap, contributes nothing. For example, if only 10 XP of a 100-XP
award fits before the source cap, Building receives 6.5 XP of credit.

The original target of Building Level 20 around 40 combined profession levels
is approximate, not a guaranteed milestone for every distribution of levels.
On the current shared curve, Level 10 in all four professions represents
22,000 raw XP and contributes 14,300 Building XP (Level 16 with 700 XP toward
Level 17). Level 20 in two professions represents 42,000 raw XP and contributes
27,300 Building XP before Building's Level 20 cap discards excess. These
examples assume eligible source awards from a fresh start.

## Levels and Profession Points

Building follows the standard profession progression rules:

- Starts at **Level 0**, with a default maximum of **Level 20**.
- Uses the shared profession XP curve: **100 times the target level** per
  transition, totaling **21,000 Building XP** to reach Level 20.
- Awards **one Building Profession Point per level**, for **20 points** at
  Level 20. These points belong to Building, separate from the source
  professions' points.
- Unspent points are retained, including while Building's skills remain
  unimplemented. Leveling grants no planned skill automatically.

At the agreed 65% conversion, reaching Level 20 requires **32,308 combined
credited source XP**, retaining fractional conversion credit. Building is
capped rather than providing an unlimited leveling or point source.

## Dimensions

Building can reach **Level 20 in every dimension by default**, including
unmatched modded dimensions. It uses the existing server-configurable
profession dimension-cap system; no dimension travel is required by default.

Evaluate the source profession's cap first. Convert only its accepted XP, then
apply Building's own cap for the same source dimension. A Building cap never
reduces the source profession's award. Blocked Building XP is discarded rather
than banked, and entering a lower-cap dimension never removes earned levels.

## Professions menu

Building appears immediately as the **fifth tab** in the existing
Professions window, including before combat class selection. No profession
level or activity is required to reveal the tab.

The tab shows Building's level, XP progress and unspent Building Profession
Points, with an explanation that it receives 65% of XP actually credited to
Mining, Logging, Farming and Crafting. Planned skills remain visibly
unavailable; players can accumulate points but cannot spend them on
unimplemented skills. The three planned entries do not invent rank gates or
costs; the server rejects purchases for all three.

## Shared foundation integration

Reuse the existing class-independent, server-authoritative profession
foundation. Persist Building's level, XP, fractional conversion credit and
point entitlement through save/load, reconnect, death and dimension changes.
This is normal persistence for fresh-format saves, not old-save compatibility.

Award Building credit at the confirmed source-profession award boundary,
without periodic activity scans or recomputing it from accumulated levels.
Only newly credited source XP contributes; loading, synchronization, balance
reloads and direct operator level edits must not replay awards. Existing
source-activity eligibility and anti-farming rules continue to apply.

Use the existing profession allocation and respec behavior when Building
skills become playable: separate Building points, out-of-combat Apply and
the shared all-profession respec. Respec preserves levels, XP and fractional
credit; it never grants Building XP. No skill effects, material-swapping
system or new recipe milestones are part of this foundation.

## Foundation acceptance criteria

- A fresh player has Building Level 0, zero XP and zero points, with the
  fifth tab visible before class selection.
- 100 accepted source XP grants 65 Building XP without reducing source XP.
  Twenty separate one-XP source awards grant exactly 13 Building XP; fractional
  credit survives saving and reconnecting.
- Source cap overflow contributes nothing. A capped source grants no Building
  XP, and Building does not feed itself.
- Building caps independently in the source dimension. It defaults to Level
  20 everywhere, awards at most 20 points under the default cap, and discards
  blocked XP without affecting the source award.
- Normal persistence retains progress; reload, respec and synchronization do
  not create XP or duplicate points.
- The tab explains progression and keeps planned skills unavailable. No
  increased reach or material swapping is enabled by this foundation.

## Planned skill directions

- Several ranks of increased block reach.
- Visual-only, interchangeable wood- and stone-type swapping, with several
  material tiers.
- One further skill, not yet chosen.

These are directions for later skill design, not implemented abilities or
settled costs, level gates, material lists, or interaction rules.

Related: [[Profession Progression]], [[Mining]], [[Logging]], [[Farming]],
[[Crafting Profession]].
