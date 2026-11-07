# Crafting Profession

## Identity

Crafting is Covenant's profession for equipment customization and material recovery. Its two dedicated progression branches are:

1. [[Affix System and Equipment Customization]] — creating and modifying Covenant affixes on equipment.
2. [[Salvaging]] — dismantling equipment for chance-based material and affix-component returns.

Enchanting is **not** a dedicated Covenant specialization branch for the current design. It remains a supported Crafting XP activity through vanilla gameplay and optional mod integrations. Covenant does not add new enchantments or replace existing enchanting systems.

## Affix Crafting progression

### Basic Affix Crafting

- **Unlock:** Crafting Level 1
- **Cost:** 1 Profession Point

Purchasing Basic Affix Crafting enables the Covenant Workbench's affix customization operations, subject to the player's Crafting level, item eligibility, materials, affix capacity, affix limits, and the item's remaining Crafting Potential.

For the first pass, adding Common affixes uses ordinary vanilla ingredients
directly, without a salvaged affix-family component. This keeps the Level 1
ability usable before Salvaging unlocks at Level 5. The material route is
provisional: **Common Add costs three lapis lazuli plus five Potential for
every affix family**. Family-specific ingredients are deferred. Only Common
uses this direct vanilla-ingredient route. Uncommon, Rare, Epic, and Legendary additions
require a corresponding salvaged affix-family component.
That component must match the target affix's family and rarity exactly. Recovery
retains the salvaged affix's own rarity, not the equipment's initialized rarity.

The first playable Workbench includes **Add, Upgrade, Reroll, and Remove** under
this purchase, with a small initial affix pool. All four operations belong to
the vertical slice; their prototype ingredient costs are specified below,
and stat ranges are provisional. These are planned operations, not implemented menus
or item mutations.

The selected initial pool has ten families. **Prefixes:** Health, Attack
Damage, Armor, Attack Speed, and Dodge. **Suffixes:** Movement Speed, Resource
Cost, Cooldown, Cast Time, and Skill Duration. Class-resource affixes are
deferred from this starting set.
All ten are allowed on any affix-eligible equipment by default, with datapacks
able to restrict individual families by equipment category, tags, or item IDs.
See [[Affix System and Equipment Customization]].

An item can carry at most one Covenant affix from each family, across all
rarities, numerical tiers, and prefix/suffix variants. Improving an existing
family uses Upgrade or Reroll, not duplicate additions. See
[[Affix System and Equipment Customization]].

For the first pass, **Add, Upgrade, Reroll, and Remove each cost five Crafting
Potential**, across tiers, shown before committing. This is a temporary policy,
expected to change and possibly soon; keep its calculation easy to replace.
See [[Affix System and Equipment Customization]].

Valid affix operations always complete successfully in the first pass, with no
separate failure roll. Reroll still produces a random value within the permitted
range and may worsen it. Invalid or unaffordable requests consume nothing.

For now, Add creates a **Tier 1** affix and each Upgrade raises it by **one
numerical tier**, respecting equipment limits and unlocked crafting tiers.
Each Upgrade rolls a fresh random value in the new tier's permitted range;
relative roll quality is not preserved. Reroll randomizes within the current
tier's range.
These are provisional rules until random bonus successes are designed;
rarity and numerical tier remain separate. See
[[Affix System and Equipment Customization]].

Initial bonus ranges scale linearly by numerical tier: Tier N uses N times
the Tier 1 bonus range. This is a provisional curve expected to need later
adjustment, with every tier's range editable in datapacks. Scale modifier
bonus magnitudes, not whole effective attribute values or default coefficients.

Each family/tier has one base range shared by affix rarities. Common through
Legendary draw **1/2/3/4/5 independent samples and keep the strongest bonus**.
After selecting the best result, Epic multiplies its bonus magnitude by
**1.05**, and Legendary by **1.15**; other rarities use 1.00. These boosts use
the affix's rarity and apply to bonus magnitudes, including reductions, rather
than entire effective stats. They are provisional, with later loot rarity
intended to reflect the added power; equal prototype loot weights stay in place.
Use this model for loot, Add, Upgrade, and Reroll. See
[[Affix System and Equipment Customization]] for the complete roll contract.

Health's endgame tuning target is **roughly ten bonus hearts across the whole
loadout**, benchmarked with six equipped Legendary Tier 5 Health affixes and
including the 1.15 Legendary boost. This is not ten hearts per affix or a global
health cap. The **provisional Tier 1 Health range is +0.3 to +0.6 health points**,
as a flat additive max-health bonus. Legendary Tier 5 becomes +1.725 to +3.45
points per affix, giving 10.35 bonus hearts with six maximum rolls. Preserve
fractional values. This is starting balance for later tuning, not validated
gameplay balance.

The **provisional Tier 1 Attack Damage range is +0.2 to +0.4 damage points**,
as a flat additive native attack-damage bonus. Legendary Tier 5 becomes +1.15
to +2.30 points per affix; six maximum rolls add **+13.8 Attack Damage**.
Keep fractional values and native attack-damage semantics, principally melee.
This is starting balance for later tuning.

Armor's prototype target is **80% vanilla armor reduction against a 30-heart
hit**, using six maximum Legendary Tier 5 Armor affixes on ordinary Netherite
(20 armor, 12 toughness), before other defensive effects. This needs **32 total
armor**, approximately **+12 across the loadout**, including the Legendary
boost. The **provisional Tier 1 Armor range is +0.15 to +0.35 armor points**,
as a flat additive native bonus with fractional values preserved. Keep linear
tier scaling: Legendary Tier 5 is +0.8625 to +2.0125 per affix, bringing ordinary
Netherite to **32.075 armor** with six maximum rolls. This is a tuning benchmark,
not a new armor cap or flat reduction; revisit balance after implementation.

Attack Speed should offer a **substantial increase over vanilla attack
recharge**. Its **provisional Tier 1 range is +2% to +4%**, using native
`MULTIPLY_BASE` modifiers whose percentages add across equipment. Legendary
Tier 5 is +11.5% to +23% per affix; six maximum rolls give +138%, or 2.38 times
normal weapon recharge speed before other percentage modifiers. Native target
hit immunity still limits damage throughput; gameplay balance tuning is
deferred, with hit-immunity behavior unchanged by this decision.

The **provisional Tier 1 Dodge range is +1 to +2 percentage points**, as flat
additive chance. Six maximum Legendary Tier 5 rolls give 69%, reaching 79%
with ordinary Dodge Aura or 84% with Mining Defensive Mastery. **Keep the 1-2
point range for the prototype**, with aura bonuses and the existing 85% cap
unchanged. Cap coverage is not a reason to increase the chosen upper bound;
overall balance can be revisited later.

The **provisional Tier 1 Movement Speed range is +0.5% to +1%**, using native
`MULTIPLY_BASE` modifiers whose percentages add across gear. Legendary Tier 5
is +2.875% to +5.75% per affix; six maximum rolls give **+34.5% Movement
Speed** before other modifiers. Native sprinting, status, and aura behavior
still applies. Revisit these starting values during gameplay tuning.

The **provisional Tier 1 Resource Cost range is 0.5% to 1% reduction** in
skill resource costs, through negative flat `ADD` modifiers on the existing
cost coefficient. Gear reductions add together. Legendary Tier 5 is 2.875%
to 5.75% reduction per affix; six maximum rolls give **34.5% lower costs**,
or a 0.655 coefficient before other modifiers. Preserve existing skill-cost
semantics and revisit these values during gameplay tuning.

The **provisional Tier 1 Cooldown range is 0.5% to 1% reduction**, through
negative flat `ADD` modifiers on the existing skill cooldown coefficient.
Reductions add across gear. Legendary Tier 5 is 2.875% to 5.75% per affix;
six maximum rolls give **34.5% shorter cooldowns**, or a 0.655 coefficient
before other modifiers and existing upward tick rounding. Preserve existing
cooldown semantics and revisit these values during gameplay tuning.

The **provisional Tier 1 Cast Time range is 1% to 2% reduction**, through
negative flat `ADD` modifiers on the existing skill cast-time coefficient.
Reductions add across gear. Legendary Tier 5 is 5.75% to 11.5% per affix;
six maximum rolls give **69% shorter casts**, or a 0.31 coefficient before
other modifiers and existing upward tick rounding. Preserve existing instant
skills/effects and revisit these values during gameplay tuning.

The **provisional Tier 1 Skill Duration range is +0.5% to +1%**, through
positive flat `ADD` modifiers on the existing duration coefficient, adding
across gear. Legendary Tier 5 is +2.875% to +5.75% per affix; six maximum rolls
give **+34.5% duration**, or a 1.345 coefficient before other modifiers and
existing timer rounding. Preserve supported effects' duration snapshots and
lifecycle rules; revisit these values during gameplay tuning.

All ten initial families now have provisional starting ranges. See the summary
table in [[Affix System and Equipment Customization]]; no rolled acquisition
or Workbench gameplay is implemented by these design decisions.

Higher affix crafting tiers become accessible at the following Crafting levels:

| Crafting level | Access |
|---:|---|
| 1 | Tier 1 through Basic Affix Crafting (purchased ability) |
| 5 | Tier 2 |
| 10 | Tier 3 |
| 15 | Tier 4 |
| 20 | Tier 5 |

After the one-point Basic Affix Crafting purchase, the higher tiers unlock
automatically at Crafting Levels **5, 10, 15, and 20**, with no additional
Profession Point costs. Item eligibility, affix capacity, item-specific affix
limits, materials, and Crafting Potential still constrain each operation.
Equipment rarity also caps numerical affix tiers at 1/2/3/4/5 for
Common/Uncommon/Rare/Epic/Legendary. Both the equipment ceiling and the crafter's
unlocked tier apply. The initial ten-family pool, provisional tier ranges,
and prototype ingredient costs are specified in this note and
[[Affix System and Equipment Customization]]; final balance remains open.

See [[Affix System and Equipment Customization]].

## Salvaging progression

### Salvaging

- **Unlock:** Crafting Level 5
- **Cost:** 2 Profession Points
- **Workstation:** Covenant Workbench

Salvaging consumes eligible equipment and makes chance-based rolls for returned materials. It supports vanilla equipment, modded equipment, and equipment carrying Covenant affixes. Eligibility and recovery rules should be configurable, with optional mod integrations where required.

The two specializations are independently upgradeable. Refinement is excluded
from the current prototype; generic refunds have no purchased quality or
quantity upgrades.

| Specialization | Rank I | Rank II | Rank III | Cost per rank |
|---|---:|---:|---:|---:|
| Recovery | Level 5 | Level 10 | Level 15 | 1 point |
| Focused Recovery | Level 5 | Level 10 | Level 15 | 1 point |

- **Recovery:** Sets total generic-material recovery chance to **70% / 85% /
  100%** at ranks I/II/III, from a 50% base. Rank III guarantees the defined
  partial refund; keep output quantities below crafting costs so generic
  recycling remains lossy.
- **Focused Recovery:** Passively improves component-recovery rates for all
  eligible non-Common families, including additional yield above the guaranteed
  count. There is no family selector or per-family specialization; this leaves
  the generic-material roll separate.

Focused Recovery adds **80 percentage points per rank** to the 120% base rate,
giving **200% / 280% / 360%** at ranks I/II/III. Rank III guarantees three
components with a 60% chance of a fourth per eligible affix, averaging 3.6.
The eventual high-end salvaging direction is around four or more components;
these are additive bonuses, not multiplicative increases.

The input item is always consumed. Generic-material recovery has a configurable
50% base chance per salvaged item, independent of component recovery. Each
eligible non-Common affix (Uncommon/Rare/Epic/Legendary) has a **120% base
component-recovery rate**: one guaranteed matching component plus a 20% chance
of a second, independently per affix. Thus generic-only salvage can return
nothing, while an eligible non-Common affix guarantees a component at this
default rate. Generic outputs use **datapack-defined partial refunds of base
crafting materials**. The **prototype default is one primary material per
successful generic roll**: one iron ingot from iron gear or one diamond from
diamond gear. Weapons, tools, and armor share this quantity regardless of
original crafting cost. Remaining material mappings still need authoring,
with output items/counts data-driven for modpack tuning; audit refunds to keep generic recycling
lossy. Durability does not modify salvage quantities, recovery chances, or
affix-component yields; damaged and pristine equipment use the same rules.
Enchantments are destroyed with salvaged gear, without returning enchanted
books or vanilla XP. Crafting profession XP is awarded separately, once per
affixed equipment item actually consumed, including an item whose recovery rolls
return nothing. The player pressing Salvage receives that XP; collecting
outputs does not award it again. Per-item XP scales with the equipment's
Covenant rarity, with higher built-in rarities paying more. Equipment with no
Covenant affixes at consumption grants **zero Crafting XP**, including
uninitialized gear and initialized gear whose affixes were all removed.
Vanilla enchantments alone do not qualify; normal eligible salvage material
returns remain available. For affixed gear, the provisional reward is
**10 / 20 / 30 / 40 / 50 Crafting XP** for Common / Uncommon / Rare / Epic /
Legendary equipment respectively. Keep the amounts data-driven. These are agreed
starting values for future implementation and playtesting, not implemented XP
acquisition. See [[Salvaging]].

Adding a non-Common affix consumes matching components at provisional costs of
**3 for Uncommon, 3 for Rare, 4 for Epic, and 5 for Legendary**. For the
prototype, non-Common Add, Upgrade, and Reroll use **only their matching
components and five Potential**, with no additional ingredients. This economy takes inspiration from
Apotheosis while retaining Covenant's family-and-rarity rules.

**Upgrade uses the same component costs as Add**, matching the targeted
affix's family and rarity exactly: **3/3/4/5** for
Uncommon/Rare/Epic/Legendary. This cost is the same at every numerical tier,
in addition to the provisional five Potential per operation. A successful
Upgrade raises the numerical tier by one; neither affix nor equipment rarity
changes. **Common equipment** has no numerical upgrade under its Tier 1 cap;
a Common affix on higher-rarity equipment can be upgraded within the item and
crafter's tier limits. For the **prototype, Common Upgrade costs three lapis
lazuli plus five Potential**, with no affix-family component, at every valid
target tier. This provisional recipe matches Common Add's cost.

**Reroll consumes 1/1/2/3 matching components** for
Uncommon/Rare/Epic/Legendary, plus the provisional five Potential. Match the
targeted affix's family and rarity exactly. This component cost is independent
of numerical tier; Reroll randomizes within the current tier without changing
family or rarity, and spends the full cost even for a worse or unchanged value.
For the **prototype, Common Reroll costs one lapis lazuli plus five Potential**,
with no affix-family component. This recipe is provisional and spends its full
cost even for a worse or unchanged result.

For the **prototype, Remove costs only five Potential**, with **no ingredient
cost and no component refund**, at every affix rarity and numerical tier.
It permanently deletes the selected affix while preserving equipment rarity
and the other affixes. It still awards the normal successful-operation XP
based on the removed affix's rarity. Removing an affix never restores Potential.

At Rank III Focused Recovery, the three-component Uncommon/Rare Add costs can
be recovered as three or four components by salvaging the crafted item. This
is an accepted high-rank prototype production benefit: crafted affixes use the
same yield rules as found affixes, while base equipment is consumed. There is
no additional ingredient sink for non-Common operations in this prototype;
generic salvage refunds can offset part of the base equipment's crafting cost.

See [[Salvaging]].

## Enchanting and optional mod integrations

Enchanting activities can contribute Crafting XP when they meet configured criteria, but Covenant does not introduce new enchantments or a dedicated Enchanting skill tree. Vanilla and modded enchanting systems retain ownership of their mechanics.

Apotheosis is a first-class optional compatibility target, not a mandatory dependency. Covenant should coexist with Apotheosis's systems—including its enchanting, affixes, gems, sockets, reforging, salvaging, and spawner mechanics—without duplicating, overriding, or invalidating them. Apotheosis activities such as supported enchanting, reforging, or salvaging interactions may award Crafting XP through optional integration.

Covenant must remain fully functional without Apotheosis. Optional integrations should be modular and should not assume every Apotheosis module is installed. The same compatibility-first principle applies to other enchanting, equipment, and content mods.

## Covenant Workbench

The initial design uses one physical **Covenant Workbench** with separate interfaces for:

- Affix Crafting.
- Salvaging.

Craft **one Covenant Workbench** with an ordinary **shapeless crafting-table
recipe**: **one crafting table, four planks, two iron ingots, and two lapis
lazuli**. Allow any planks through the native planks ingredient tag, including
mixed plank types. Crafting the station has no profession-level or purchased
ability requirement; profession unlocks gate its operations. This ordinary
recipe grants no Crafting profession XP. The recipe is agreed prototype
content, not an implemented block/item/recipe.

For the **prototype**, Affix Crafting uses **one equipment slot**, operation
and affix selectors, a visible material/Potential cost preview, and an explicit
**Craft** button. Required ingredients are drawn directly from the **acting
player's inventory**, with no dedicated ingredient-staging slots. Only the
Craft action consumes ingredients and Potential; the server revalidates the
equipment, selected operation/affix, player access, and all costs before
committing the operation. This inventory-sourcing flow is provisional.

The equipment slot is **shared, persistent Workbench storage**. Each completed
operation modifies that same item in place, ready for another operation or
collection. Closing the GUI leaves it in the Workbench, accessible to other
players using it. Preserve the item across chunk unloads and world saves/restarts;
breaking the Workbench drops it once with the other stored contents. The acting
player still supplies ingredients, access, and progression for each operation.

The Salvaging interface accepts **up to ten equipment items** in a
**To Be Salvaged** section. It previews possible outputs and recovery chances
before an explicit **Salvage** button processes the queued batch. Each item
and eligible affix retains its independent recovery rolls; batching gives no
yield bonus. Actual rewards appear in a separate **Salvage Results** area
with **twelve withdrawal-only output slots** for collection. Process inputs in
slot order, stopping before an item's maximum possible rewards would exceed
remaining output capacity. Completed rewards remain available and unprocessed
equipment stays queued; collecting results and pressing Salvage again continues
the batch. Queued inputs and unclaimed
results stay in the physical Workbench when the GUI closes, persist across
world saves/restarts, and are accessible to other players using it. The player
pressing Salvage supplies the purchased ranks and must meet its access
requirements. See [[Salvaging]] for the interface and storage contract.

Keep the underlying operations, validation, inputs, and outputs separate so they can move into distinct physical workstations later without redesigning the systems. Enchanting improvements, if any are identified in the future, should integrate with existing enchanting stations rather than require a Covenant enchanting station.

## Point budget

| Ability | Defined investment |
|---|---:|
| Basic Affix Crafting | 1 |
| Salvaging unlock | 2 |
| Recovery and Focused Recovery (3 ranks each) | 6 |
| **Total currently defined** | **9** |

Eleven Profession Points remain unallocated at the Level 20 cap. The
Crafting-level affix access milestones unlock automatically after purchasing
Basic Affix Crafting and cost no additional points.

## XP and vanilla boundaries

**Successful vanilla enchanting-table use is the starting Crafting XP route**
from Level 0 to Level 1, before Basic Affix Crafting can be purchased. Players
therefore need access to enchanting as part of normal vanilla progression
before starting Covenant affix crafting. Ordinary enchanting remains available
at Crafting Level 0 with its native requirements and costs.

Award Crafting XP **once per successful enchanting-table operation**, using
fixed provisional rewards for the selected option:

| Enchanting-table option | Crafting XP |
|---|---:|
| First (top) | 20 |
| Second (middle) | 50 |
| Third (bottom) | 100 |

The option determines the reward regardless of its displayed level requirement.
Neither levels actually spent nor the number of enchantments produced scales
the payout. With the shared 100-XP requirement for Crafting Level 1, five first-
option, two second-option, or one third-option operations reach Level 1.
These are implemented prototype rewards, still provisional for playtesting.
Native successful enchanting completion pays once after its ordinary costs.
See [Profession activities](../docs/PROFESSIONS.md#natural-activity-xp).

Ordinary vanilla crafting does not award Crafting XP. Meaningful enchanting interactions, completed Covenant Workbench operations, Affix Crafting, Salvaging, and supported activities from optional integrations can be configured XP sources. Rewards should be contextual and should not create trivial repeatable crafting loops. Automated production does not award XP by default.

**Add, Upgrade, Reroll, and Remove each award Crafting XP once per successful
operation**, to the player performing it. Award only when the server completes
the operation and spends its required materials and Crafting Potential.
A completed Reroll earns XP even if its value is worse or unchanged. Previews,
rejected/invalid requests, and collecting or moving the resulting equipment
award no XP. Rewards **scale with the affected affix's rarity**, with higher
built-in rarities paying more. Add uses the added affix's rarity; Upgrade and
Reroll use the targeted affix's rarity; Remove uses its rarity before removal.
For example, modifying a Common affix on Legendary equipment earns the Common
affix-operation reward. For the prototype, **one XP reward per affix rarity is
shared by all four operations and all numerical tiers**. A high-tier Upgrade
and a low-tier Reroll of the same rarity therefore pay the same XP.

| Affected affix rarity | Crafting XP per successful operation |
|---|---:|
| Common | 20 |
| Uncommon | 40 |
| Rare | 60 |
| Epic | 80 |
| Legendary | 100 |

These are agreed provisional rewards for future implementation and playtesting,
not implemented XP acquisition. Keep the per-rarity amounts data-driven.

Vanilla crafting, enchanting, equipment use, and ordinary stations remain available at Crafting Level 0.

Related: [[Profession Progression]], [[Experience and Reward Systems]], [[Affix System and Equipment Customization]], [[Salvaging]], [[Crafting]], [[Mod Compatibility]], [[Mining]], [[Logging]], [[Farming]].
