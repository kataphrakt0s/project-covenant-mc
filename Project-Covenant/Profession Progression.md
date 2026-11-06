# Profession Progression

## Purpose

Professions reward mastery of Minecraft activities. They add capabilities, interactions, and selected recipes without gating baseline vanilla gameplay or recreating systems already handled well by other mods.

The four initial professions are:

1. [[Mining]]
2. [[Logging]]
3. [[Farming]]
4. [[Crafting Profession]]

All characters can develop all four professions simultaneously. Professions are not class-restricted, there are no profession slots, and each profession has its own XP, level, and Profession Point balance.

## Shared progression rules

- Each profession starts at **Level 0** and has a default maximum of **Level 20**.
- Each level gained awards **1 Profession Point**. Reaching Level 20 therefore provides 20 points for that profession.
- All professions use a shared XP curve, but their XP and points are tracked independently.
- Exact XP curves, action rewards, and numerical balance are intentionally deferred.
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

Exact XP curves, action values, cooldowns, and anti-farming thresholds remain open for later tuning. See [[Experience and Reward Systems]].

## Respec

Profession respec uses a crafted item and is more expensive than combat respec. One use resets **one selected profession's** purchased abilities and specialization ranks, refunding that profession's spent points. The profession's XP and level are preserved; other professions are unaffected. Exact recipe, cost, station requirements, and acquisition pace remain undecided.

## Current investment summary

These totals cover only the abilities and ranks currently defined. They are not intended to imply that all future profession content has been designed.

| Profession | Currently defined point investment | Points remaining at Level 20 |
|---|---:|---:|
| [[Mining]] | 15 | 5 |
| [[Logging]] | 11 | 9 |
| [[Farming]] | 14 | 6 |
| [[Crafting Profession]] | 12 | 8 |

## Profession documents

- [[Mining]] — prospecting, ore extraction, processing, transfiguration, and defensive mastery.
- [[Logging]] — tree felling, offensive mastery, and automatic replanting.
- [[Farming]] — cultivation, crop harvesting, animal husbandry, and vitality.
- [[Crafting Profession]] — equipment customization and salvaging.
- [[Affix System and Equipment Customization]] — item-bound Covenant affixes and crafting rules.
- [[Salvaging]] — Covenant's chance-based equipment recovery system.

Related: [[Progression Systems]], [[Experience and Reward Systems]], [[Dimensional Progression Gates]], [[Progression Balance Configuration]], [[Progression Vertical Slice]].
