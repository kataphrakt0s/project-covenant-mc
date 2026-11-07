# Salvaging

## Purpose

Salvaging is a Crafting profession specialization for dismantling equipment into a chance-based return of generic materials and affix-family components. It supports a player-driven equipment economy without guaranteeing recovery of invested value.

## Access and workstation

- **Profession:** [[Crafting Profession]]
- **Unlock:** Crafting Level 5
- **Cost:** 2 Profession Points
- **Workstation:** Physical Covenant Workbench

The initial Covenant Workbench contains separate Affix Crafting and Salvaging interfaces. Keep their underlying operations and validation separate so they can become individual workstations later if needed.

## Salvage interface

The Salvaging interface has a **To Be Salvaged** section holding up to **ten
equipment items**, one item per input slot. Players can queue and remove items
before committing. Placing equipment in the section does not consume it.

Show **possible outputs and their recovery chances**, using the queued items
and the player's purchased Recovery/Focused Recovery ranks. The preview shows
guaranteed quantities and any chance of additional components; it does not roll
or reveal actual random results in advance.

An explicit **Salvage** button processes the queued batch. Resolve generic
recovery independently for each item and component recovery independently for
each eligible affix, using the same rules as individual salvage. Batch size
does not change recovery rates. The server validates and owns consumption,
rolls, and rewards.

Actual rewards appear in a separate **Salvage Results** area of the GUI, where
players collect the resulting items. It has **twelve withdrawal-only output
slots**, separate from the possible-output preview and the To Be Salvaged
inputs. Salvaging does not require automatic delivery into the
player's inventory.

Queued inputs and unclaimed results **remain in the physical Workbench when
the GUI closes**. They are shared workstation contents, accessible to other
players using that Workbench. Persist both inventories across chunk unloads
and world saves/restarts through the workstation's native block-entity storage.
Breaking the Workbench drops its remaining contents once.

Closing the GUI does not perform salvage or continue processing. The player
who presses Salvage supplies the purchased ranks and must satisfy the access
requirements for that operation; shared storage does not share profession ranks.

Process queued items in input-slot order. Before rolling or consuming each
item, check that its **maximum possible rewards** at the acting player's ranks
fit in the remaining result capacity, including valid merges into existing
stacks. If they do not fit, stop the batch: keep the current and later inputs
queued, and retain all completed rewards. Do not roll the blocked item or spill
outputs. Players can collect results and press Salvage again to continue;
collecting results does not automatically resume processing.

This workstation storage and menu are implemented; see [Itemization](../docs/ITEMIZATION.md).

## Eligible equipment

Covenant Salvaging is intended to support:

- Vanilla equipment.
- Modded equipment.
- Equipment carrying Covenant affixes.

Eligibility should be configurable using item identifiers, tags, and optional mod integrations. Reasonable default equipment categories should be provided rather than treating every arbitrary item as salvageable.

Apotheosis and other mods retain their own salvaging systems. Covenant Salvaging is an additional system and must not override or interfere with those mechanics.

## Salvage outcome rules

The first implementation authors generic refunds **only for iron and diamond
equipment**, returning one iron ingot or one diamond on a successful generic
roll. Other affix-eligible equipment remains salvageable for eligible affix
components, with no generic refund until a datapack supplies a mapping. This is
an agreed initial content boundary, not a restriction on future material tables.

- The input equipment is always consumed when a salvage operation is performed.
- **Enchantments are destroyed with the equipment.** Salvaging returns no
  enchanted books and refunds no vanilla XP. Crafting profession XP follows
  the separate consumption-based reward rule below.
- **Durability does not affect salvage returns.** Damaged and pristine
  equipment use the same generic-material refund quantities, recovery chances,
  and affix-component yields. Repairing an item before salvaging gives no
  recovery bonus.
- Generic-material returns remain chance-based. A generic-only salvage can
  return nothing; component output follows the recovery-rate rule below.
- Generic crafting materials use **datapack-defined partial refunds**. The
  **prototype default is one primary crafting material per successfully
  recovered equipment item**: iron gear returns one iron ingot, diamond gear
  one diamond. An iron sword therefore returns one ingot on success. Weapons,
  tools, and armor use the same quantity, even when their original crafting
  costs differ. A failed generic roll returns no generic material.
- The configurable prototype **base generic-material recovery chance is 50%
  per salvaged item**. This is the roll for whether generic materials are
  recovered. Remaining item/material mappings still need authoring; retain
  data-driven output items/counts for modpack tuning and modded equipment.
- Affix-family components may be returned from equipment carrying eligible
  non-Common Covenant affixes: Uncommon, Rare, Epic, or Legendary.
- Each eligible non-Common affix has a configurable prototype **120% base
  component-recovery rate**: one guaranteed matching component plus a 20%
  chance of a second. Resolve each affix independently, retaining its family
  and rarity. This is an expected yield of 1.2 components per eligible affix,
  not a probability clamped at 100%.
- Generic-material recovery and affix-component recovery are independent. A
  failed generic roll still permits all eligible component output. At the
  default 120% component rate, an item with an eligible non-Common affix yields
  at least one component even when its generic-material roll fails.
- Salvaging never returns intact Affix Pages. Affix Pages are not standalone items and the old terminology is retired.
- Salvaging does not guarantee recovery of all crafting inputs or their total
  invested value; component counts can exceed their component input at high rank.

Generic recovery starts at 50% and component recovery at 120% before
specialization bonuses. Generic outputs are partial material refunds; audit
their quantities against crafting costs to keep generic recycling lossy.
The remaining material-mapping coverage and final balance remain under design.
The default one-material quantity does not scale with equipment category,
rarity, or power; durability rules are also unchanged. These rules and rolls are implemented with provisional tuning.

## Specializations

Recovery and Focused Recovery each have three independently purchasable ranks.
Both branches can be mastered. **Refinement is excluded from the current
prototype**; generic refunds have no purchased quality or quantity upgrades.

| Specialization | Rank I | Rank II | Rank III | Cost per rank |
|---|---:|---:|---:|---:|
| Recovery | Level 5 | Level 10 | Level 15 | 1 Profession Point |
| Focused Recovery | Level 5 | Level 10 | Level 15 | 1 Profession Point |

### Recovery

Improves generic-material recovery chance, independently of component yield.
Ranks I/II/III set the total chance to **70% / 85% / 100%**, from the 50% base.
Rank III guarantees the defined partial material refund. Generic recycling
remains lossy through output quantities below crafting costs, rather than a
failure chance at maximum rank; audit the output tables and recipes accordingly.

### Focused Recovery

Passively improves the component-recovery rate for **all eligible non-Common
affix families**, including the chance of additional components above the
guaranteed count. There is no family selector or per-family specialization.
Use the salvaging player's purchased ranks; this bonus does not alter the
separate generic-material recovery roll.

Each rank adds **80 percentage points** to the 120% base recovery rate:

| Focused Recovery rank | Crafting level | Total recovery rate | Components per eligible affix |
|---|---:|---:|---|
| I | 5 | 200% | 2 guaranteed |
| II | 10 | 280% | 2 guaranteed, 80% chance of a third |
| III | 15 | 360% | 3 guaranteed, 60% chance of a fourth |

These bonuses are additive percentage points, not multiplicative 80% bonuses.
Resolve whole hundreds as guaranteed components and the remainder as one extra
component's chance. Rank III averages 3.6 components per eligible affix.
The eventual direction is high-end **salvaging** producing around four or more
components; further progression or tuning toward that target remains future work.

These specialization effects are implemented with provisional tuning.

## Component economy

Salvaging can return two broad types of materials:

1. **Generic crafting materials**, defined in datapack partial-refund tables
   for the equipment's base crafting materials and usable across general
   Covenant crafting recipes.
2. **Affix-family components**, retaining the eligible affix's family and rarity
   and used when adding matching non-Common affixes.

Non-Common affix components are consumable, not permanent unlocks. The
provisional component costs for affix crafting are:

| Affix rarity | Add / one-tier Upgrade | Reroll |
|---|---:|---:|
| Uncommon | 3 | 1 |
| Rare | 3 | 1 |
| Epic | 4 | 2 |
| Legendary | 5 | 3 |

These operations consume components matching the affix's **family and rarity**
exactly. Component quantities do not scale with numerical tier, and each
operation also costs the provisional five Potential. Reroll spends the full
cost even for a worse or unchanged result, without changing family, rarity,
or numerical tier. For the **prototype, Common Reroll costs one lapis lazuli
plus five Potential**, with no affix-family component. Its provisional recipe
also spends the full cost for worse or unchanged results.
Upgrade changes numerical tier, not
affix or equipment rarity. **Common equipment** has no numerical upgrade under
its Tier 1 cap. Common affixes on higher-rarity equipment can still be upgraded
within the item and crafter's tier limits. For the **prototype, Common Upgrade
costs three lapis lazuli plus five Potential**, without an affix-family
component, at every valid target tier; this provisional recipe matches Common Add.

For the prototype, **Remove costs five Potential only**, with no ingredients
and no component refund. It permanently deletes the selected affix without
restoring Potential or changing equipment rarity; the normal operation XP
still applies. Component recovery remains an equipment-consuming salvage
operation.

Applying the affix consumes the required components. Only Common additions
use the direct vanilla-ingredient route: **three lapis lazuli plus five
Potential for every affix family** as the provisional prototype recipe.
Family-specific ingredients are deferred. For the prototype, **non-Common Add,
Upgrade, and Reroll require only their matching components and five Potential**,
with no additional ingredients. Generic salvage refunds mainly support ordinary
crafting for now.

With these provisional costs and Rank III Focused Recovery, an Uncommon or
Rare affix costing three components to add can return three or four components
when salvaged. The input equipment is consumed, so component recovery can
exceed the component input while losing the base item. Generic salvage refunds
can offset part of that item's crafting cost; there is no extra ingredient sink
for non-Common operations in this prototype.

This component-producing craft-and-salvage loop is an **accepted high-rank
profession benefit for the prototype**. Apply the same recovery-rate rules
to player-crafted affixes and loot-generated affixes. Do not cap crafted
component returns at the number previously spent; consuming the equipment
funds this loop under the prototype recipes.

Components must match **both family and rarity exactly**. For example, an
Uncommon Health component is used to add Uncommon Health. A Legendary item's
Uncommon Health affix still yields an Uncommon Health component on successful
recovery; the item's initialized rarity does not promote the component.
Different component rarities occupy separate inventory stacks.

This stockpile-and-spend economy takes inspiration from
[Apotheosis's data-driven salvage outputs](https://github.com/Shadows-of-Fire/Apotheosis/blob/26.1/src/main/java/dev/shadowsoffire/apotheosis/affix/salvaging/SalvagingRecipe.java).
The 3-5 costs, 120% recovery rate, and family/rarity matching are Covenant's
chosen rules, not claims about Apotheosis's exact numbers.

The equipment itself is destroyed; its Covenant affixes are not returned as intact items or pages.

## Crafting XP

Award Crafting profession XP **once per affixed equipment item actually consumed**
by a completed salvage operation, to the player who pressed Salvage. An eligible
item still earns its reward if all recovery rolls return nothing. A batch awards
the sum for the items consumed, with no batch multiplier or per-component bonus.

Placing/removing queued items, previewing outcomes, and collecting or moving
results award no XP. Items left queued because output capacity is insufficient,
and rejected/invalid operations, award no XP. Sharing the Workbench does not
share or duplicate this award.

**Equipment with no Covenant affixes at the moment of consumption grants zero
Crafting XP**, including uninitialized equipment and initialized equipment whose
affixes were all removed. Vanilla enchantments alone do not meet this condition.
Such equipment can still be salvaged for its normal eligible material returns.

For equipment with at least one Covenant affix, the per-item reward **scales with
the consumed equipment's Covenant rarity**, with higher built-in rarities
rewarding more XP. Use equipment rarity, independently of individual affix
rarities, affix count, and the number of recovered components.

| Consumed equipment rarity (at least one Covenant affix) | Crafting XP |
|---|---:|
| Common | 10 |
| Uncommon | 20 |
| Rare | 30 |
| Epic | 40 |
| Legendary | 50 |

These are implemented provisional, data-driven rewards for playtesting.

## Multiplayer and economy considerations

Salvaging should be a server-authoritative workstation operation. The server validates the input, eligibility, player access, and required state; consumes the item exactly once; performs recovery rolls; and grants outputs. Repeated or duplicated requests must not duplicate returns.

Completed salvaging destroys the equipment and its enchantments. Generic
refunds remain partial even when Recovery guarantees their roll; affix-component
production follows the accepted high-rank rules above. Repeated requests must
never duplicate the input or its rewards.

Related: [[Crafting Profession]], [[Affix System and Equipment Customization]], [[Profession Progression]], [[Mod Compatibility]].
