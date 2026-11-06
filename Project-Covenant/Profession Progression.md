# Profession Progression

## Purpose

Professions reward mastery of Minecraft activities. They add new capabilities, interactions, techniques, and selected recipes to the sandbox rather than gating normal gameplay or replacing vanilla mechanics. Profession progression is a parallel layer to character combat progression.

## Initial professions

1. [[Mining]]
2. [[Logging]]
3. [[Farming]]
4. [[Crafting]] — see [[Crafting Profession]]. Enchanting is a subset of Crafting, not a separate profession.

All characters can develop all four professions simultaneously. Profession choice is not class-restricted, there are no profession slots, and investing in one profession does not consume points from another. The framework should support future professions without changing its core architecture.

## Level and point economy

- Professions start at **Level 0**.
- Default maximum profession level is **20**; the cap can be configured per profession if a future profession warrants a different progression length.
- All professions use one shared XP curve, but each has independent XP, level, and Profession Point balances.
- Each profession level gained awards one Profession Point. Reaching Level 20 earns 20 points.
- Unspent points remain available; they do not expire.
- Profession abilities generally cost 1–3 points based on their significance. Costs are individually authored rather than forced to a single value.
- Abilities are normally single-purchase unlocks. Multi-rank profession abilities are exceptions that require a clear design reason.
- There is no global limit on the number of profession abilities a player can learn.

## Core rewards versus chosen abilities

### Automatic core rewards

Selected profession-level milestones can automatically grant core recipes. Automatic progression rewards are primarily recipe unlocks and should build on existing Minecraft mechanics.

### Chosen profession abilities

Abilities and new interactions require deliberate Profession Point investment. Profession level determines when an ability becomes available for purchase; prerequisites and explicit mutual exclusions may apply. Most of a profession's offerings should eventually be obtainable by a sufficiently developed character.

Mutually exclusive choices should be reserved for genuine competing alternatives, such as two different flavors of prospecting. Choosing one should not lock out unrelated abilities or entire sections of the profession.

## Ability types and combat crossover

Profession rewards may include:

- Passive benefits.
- Conditional benefits tied to a relevant state or activity.
- Active abilities and new contextual interactions.
- Recipe unlocks and advanced production options.

Profession abilities do not consume any of the eight combat keybinds. They should use contextual interactions, tools, workstations, or passive/conditional effects as appropriate.

Profession benefits are class-agnostic. All three classes can learn the same profession benefits. Modest combat benefits are allowed where they fit a profession's identity, but they must complement rather than replace class skill trees and should not create uncontrolled stat inflation.

## Vanilla-friendly rules

- Profession level never gates baseline vanilla functionality. A player can use ordinary tools, mine vanilla resources, chop trees, farm crops, use vanilla stations, enchant items, and access normal vanilla recipes without profession levels.
- Profession rewards primarily add new ways to interact with existing mechanics. Efficiency/yield improvements are secondary and should be justified.
- Professions do not grant generic character levels or character skill points.
- Vanilla XP orbs do not award Profession XP.

## XP and automation

Each profession advances from meaningful contextual actions associated with its activity. Trivial repetitive actions should not be efficient progression sources. Direct player participation is the default. Automated production grants no XP unless explicitly configured with attribution and anti-exploit safeguards. Crafting XP excludes ordinary vanilla crafting; meaningful enchanting, designated advanced recipes, Covenant workstation use, affix crafting, and salvaging may be configured sources.

See [[Experience and Reward Systems]].

## Cross-profession relationships

Professions can support one another through optional synergies, material supply chains, trade, and specifically authored cross-profession recipes. Most abilities and normal profession progression must not require levels in another profession. Explicit recipe exceptions may require multiple profession levels or products. The discussed idea of requiring Level 10 in all four professions for the Profession Respec recipe is an example, not a finalized recipe.

## Profession respec

Profession respec is allowed and uses its own crafted item. It is more expensive than a combat respec. One use resets **one selected profession** only: its purchased abilities and specialization choices are reset, and all spent Profession Points for that profession are refunded. The profession's XP and level are preserved. Other professions are unaffected. Exact recipe, cost, station, and acquisition pace remain open.

## Vertical-slice scope

All four professions are included in the initial vertical slice. Each receives a small but playable selection of meaningful abilities, a few real specialization choices, and core recipe rewards. The slice implements a reusable progression framework; it does not require fully populated Level 0–20 trees or the entire future content catalog.

Related: [[Progression Systems]], [[Experience and Reward Systems]], [[Dimensional Progression Gates]], [[Progression Balance Configuration]], [[Mining]], [[Logging]], [[Farming]], [[Crafting Profession]], [[Progression Vertical Slice]].
