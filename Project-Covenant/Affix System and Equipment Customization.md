# Affix System and Equipment Customization

## Purpose

Covenant's affix system adds RPG properties to existing vanilla and modded equipment without replacing the equipment's identity. Affixes are properties stored directly on an individual equipment item, not standalone inventory items.

The former **Affix Pages** concept is retired. There are no separate Affix Page items to collect, trade, store, apply, or salvage.

## Item ownership and tradeability

- Covenant affix data belongs to an individual equipment item instance.
- Two otherwise identical base items can carry different Covenant equipment
  rarities, affixes, and Crafting Potential.
- Equipment rarity, affixes, and the item's remaining Crafting Potential travel
  with the item through inventory, containers, drops, pickup, trading, player
  transfer, and save/reload.
- Affixed equipment remains tradeable. Covenant affixes do not soulbind equipment.
- Affixes are active according to the equipment/stat rules; unequipping an item does not erase its affixes.
- Item-stack copy, split, and identity semantics must be safe. Uniquely customized equipment will generally need stack size one unless safe splitting is explicitly supported.

## Affix structure

Equipment uses the implemented **Covenant equipment rarity system**, with
datapack-defined rarities and item/tag-based opt-in rules for modpack developers.
This is separate from a rarity label on an individual affix. Detailed rarity
definitions and assignment behavior belong to [[Affixes and Rarity]].

Ordinary crafted gear has uninitialized Covenant rarity until its first
successful affix application gives the item the starting affix's rarity.
A Common starting affix produces Common equipment; a Legendary starting affix
produces Legendary equipment. Subsequent Add, Upgrade, Reroll, and Remove
operations preserve that rarity, even if the starting affix or all affixes are
removed. An initialized item does not become uninitialized again through affix
removal. The first successful Workbench Add initializes Potential once from the starting
rarity's definition, then deducts that Add's Potential cost. Provisional
starting budgets are 20/30/40/50/60 for
Common/Uncommon/Rare/Epic/Legendary. Add, Upgrade, Reroll, and Remove each cost
five Potential in the first pass.

Freshly generated affixed loot instead begins with its full rarity-based
Potential budget. Its natural affix incurs no Add or Upgrade charges; later
Workbench operations use normal costs. Existing initialized items retain their
remaining Potential through later drops and ownership changes.

### Prefixes and suffixes

Covenant affixes are classified as either **Prefixes** or **Suffixes**. Equipment has separate capacity limits for each category.

- An item can have a different number of available Prefix and Suffix slots.
- Rarity definitions supply the built-in default capacities.
- Crafting cannot exceed either category's capacity.

The implemented, configurable starting limits are:

| Equipment rarity | Prefix slots | Suffix slots |
|---|---:|---:|
| Common | 1 | 1 |
| Uncommon | 1 | 1 |
| Rare | 2 | 2 |
| Epic | 2 | 2 |
| Legendary | 3 | 3 |

The built-in starting ceiling is therefore six affixes on Legendary equipment.
These limits are validated by the server during crafting.

### Affix eligibility

Affix eligibility is data-driven. Definitions can use equipment categories, tags, affix-family tags, and explicit identifiers to determine which items can receive an affix.

This must support vanilla and modded equipment without requiring a hardcoded list for every mod. Server owners and optional integrations should be able to extend compatibility rules.

For the prototype, **all ten initial affix families are allowed on any
affix-eligible equipment by default**. Equipment must still qualify through
the compatibility rules. Offensive bonuses may appear on armor and defensive
bonuses on weapons. Datapacks can restrict individual families by equipment
category, item tags, or explicit item identifiers.

### Family uniqueness

Each equipment item permits **at most one Covenant affix from each family**,
regardless of that affix's rarity, numerical tier, or prefix/suffix category.
For example, different rarity or tier variants of a Health affix cannot occupy
multiple slots on the same item. Upgrade or Reroll the existing affix to improve
it; Add must reject another member of the same family without spending costs.
This is a server-enforced per-item restriction, not a limit across equipped items.

### Initial affix pool (implemented prototype)

The first pool contains **ten families**, using existing supported stats:

| Prefix families | Suffix families |
|---|---|
| Health | Movement Speed |
| Attack Damage | Resource Cost |
| Armor | Cooldown |
| Attack Speed | Cast Time |
| Dodge | Skill Duration |

Keep the existing [stat semantics](../docs/RPG_STATS.md): Attack Damage follows
native attack-damage consumers, principally melee, and Attack Speed affects
attack recharge. Resource Cost, Cooldown, and Cast Time bonuses reduce their
respective coefficients; Skill Duration increases its coefficient.

Class-resource affixes such as Mana Capacity are deferred from the initial
pool. The ten families use broad default equipment eligibility, with datapack
restrictions available. Their provisional starting ranges are specified below.
These are implemented provisional rolls; final balance remains for playtesting.

| Family | Tier 1 base range | Six maximum Legendary Tier 5 affixes |
|---|---|---|
| Health | +0.3 to +0.6 health points | +20.7 health points (10.35 hearts) |
| Attack Damage | +0.2 to +0.4 damage points | +13.8 Attack Damage |
| Armor | +0.15 to +0.35 armor points | +12.075 Armor |
| Attack Speed | +2% to +4% | +138% Attack Speed |
| Dodge | +1 to +2 percentage points | +69 percentage points |
| Movement Speed | +0.5% to +1% | +34.5% Movement Speed |
| Resource Cost | 0.5% to 1% reduction | 34.5% lower skill costs |
| Cooldown | 0.5% to 1% reduction | 34.5% shorter skill cooldowns |
| Cast Time | 1% to 2% reduction | 69% shorter casts |
| Skill Duration | +0.5% to +1% | +34.5% duration |

Each total is a separate six-piece benchmark for that family, including the
Legendary 1.15 boost, before other modifiers and applicable timer rounding.
Existing prefix/suffix slot limits and stat rules still apply. All values are
provisional and will need later balance work.

### Provisional numerical tier scaling

For the initial balance, scale each family's bonus range linearly with its
numerical tier: **Tier N uses N times the Tier 1 bonus range**, through Tier 5.
Scale the bonus magnitude, including the magnitude of cost/time reductions,
not the entire effective attribute value or its default coefficient.

Use **one base range per family and numerical tier**, shared across affix
rarities. Affix rarity improves roll quality by drawing independent samples
from that range and keeping the strongest bonus, then applying the following
magnitude multiplier:

| Affix rarity | Samples; keep the best | Bonus magnitude multiplier |
|---|---:|---:|
| Common | 1 | 1.00 |
| Uncommon | 2 | 1.00 |
| Rare | 3 | 1.00 |
| Epic | 4 | 1.05 |
| Legendary | 5 | 1.15 |

Use the **individual affix's rarity**, not equipment rarity. Apply this model
to loot-generated affixes and fresh rolls from Add, Upgrade, and Reroll.
Upgrade still rolls afresh within its new tier; Reroll can still worsen or
repeat its old value. For reduction bonuses, best means the greatest beneficial
reduction magnitude; preserve the modifier's sign when multiplying it.

The Epic and Legendary boosts are **5% and 15% of the rolled bonus**,
respectively, not percentage points or multipliers on the entire effective
stat. They are not cumulative: Legendary uses 1.15, not 1.05 times 1.15.
For example, a selected +2 bonus becomes +2.10 Epic or +2.30 Legendary.
Common/Uncommon/Rare share a ceiling; Epic/Legendary may exceed it by 5%/15%.
Keep these boosts provisional while later loot rarity is tuned to match their
added power. The prototype's equal loot-rarity weights remain unchanged.

This is explicitly a **temporary starting curve that will need adjustment**.
Keep each tier's range editable through datapacks so later tuning can depart
from the linear defaults. All ten initial families now have provisional
Tier 1 ranges; no numerical roll ranges are implemented yet.

For **Health**, target approximately **ten bonus hearts (+20 health points)
across a whole endgame loadout**, not per affix. Use six equipped Health affixes
(four armor pieces, main hand, and offhand), at Legendary numerical Tier 5,
as the tuning benchmark. Include the Legendary 1.15 bonus multiplier in that
budget. This is a tuning target, not a new global health cap.

The **provisional prototype Health range is +0.3 to +0.6 health points at
Tier 1**, applied as a flat additive max-health bonus. Preserve fractional
health points; display formatting must not round the stored modifier to whole
points. Before rarity boosts, Tier 5 is +1.5 to +3.0 points. Legendary Tier 5
is therefore +1.725 to +3.45 points per affix, and six maximum rolls add
**20.7 health points (10.35 hearts)**. These values are a starting point for
later balance work, not validated in-game tuning.

The **provisional prototype Attack Damage range is +0.2 to +0.4 damage points
at Tier 1**, as a flat additive native attack-damage bonus. Before rarity
boosts, Tier 5 is +1.0 to +2.0 points; Legendary Tier 5 is +1.15 to +2.30 per
affix. Six maximum equipped rolls add **+13.8 Attack Damage**. Preserve
fractional values and existing native attack-damage semantics, principally
melee; this does not independently rescale projectiles or every skill's damage.
These are starting values for later balance work, not validated gameplay tuning.

For **Armor**, target a full set of bonuses reaching vanilla's **80% armor
reduction against a 30-heart hit (60 damage points)**. Benchmark six maximum
equipped Legendary Tier 5 Armor affixes on ordinary Netherite's **20 armor
and 12 toughness**, including the Legendary 1.15 boost. The vanilla formula
requires **32 total armor**, or approximately **+12 armor across the loadout
(+2 per maximum affix)**, for this hit. The **provisional Tier 1 Armor range
is +0.15 to +0.35 armor points**, as a flat additive native armor bonus with
fractional values preserved. Keep the existing linear numerical-tier scaling:
Tier 5 is +0.75 to +1.75 before rarity boosts, and Legendary Tier 5 is +0.8625
to +2.0125 per affix. Six maximum rolls give **+12.075 armor**, bringing
ordinary Netherite to **32.075 total armor** and meeting the 80% target.

Use native armor mitigation, without introducing a flat damage-reduction
affix or a new global armor cap. This benchmark excludes Protection, other
defensive buffs, and armor-piercing/bypass effects; those retain their normal
rules. The target is prototype tuning, not an in-game validated balance claim.

Attack Speed should provide a **substantial improvement over vanilla attack
recharge**. The **provisional Tier 1 Attack Speed range is +2% to +4%**, using
native `MULTIPLY_BASE` modifiers (0.02 to 0.04), so these percentages add
across equipment. Before rarity boosts, Tier 5 is +10% to +20%; Legendary
Tier 5 is +11.5% to +23% per affix. Six maximum rolls give **+138% Attack
Speed**, or **2.38 times the weapon's normal recharge speed** in the absence
of other percentage modifiers. Keep recharge speed distinct from actual damage
throughput: native target hit immunity is a separate constraint. These are
provisional starting values; tune them later through gameplay testing rather
than changing hit-immunity behavior as part of this decision.

The **provisional Tier 1 Dodge range is +1 to +2 percentage points**, as flat
additive chance (`ADD` amounts 0.01 to 0.02). Legendary Tier 5 is +5.75 to
+11.5 percentage points per affix, giving **69% from six maximum rolls**.
With ordinary Dodge Aura's +10 points this reaches **79%**, or **84%** with
Mining Defensive Mastery's +15-point recipient bonus. **Keep the chosen 1-2
percentage-point Tier 1 range for the prototype**, rather than increasing its
upper bound to force gear-plus-aura cap coverage. The ordinary **85% cap** and
aura bonuses retain their agreed values. Revisit overall balance later;
do not claim this gear-plus-aura combination already reaches 85%.

The **provisional Tier 1 Movement Speed range is +0.5% to +1%**, using native
`MULTIPLY_BASE` modifiers (0.005 to 0.01). Equipment percentages add together;
they do not compound per affix or add flat movement-speed units. Before rarity
boosts, Tier 5 is +2.5% to +5%; Legendary Tier 5 is +2.875% to +5.75% per
affix. Six maximum rolls give **+34.5% Movement Speed** before other modifiers.
Preserve native sprinting, status, aura, and other modifier behavior. These
are provisional starting values for later gameplay tuning.

The **provisional Tier 1 Resource Cost range is 0.5% to 1% reduction** in
skill resource costs. Use negative flat `ADD` modifiers on the existing
Resource Cost coefficient (amounts -0.01 to -0.005), so equipment reductions
add together rather than compound per affix. Before rarity boosts, Tier 5 is
2.5% to 5% reduction; Legendary Tier 5 is 2.875% to 5.75% per affix. Six
maximum rolls give **34.5% lower skill resource costs**, or a **0.655 cost
coefficient** before other modifiers. Preserve the existing Focus/Rage/Mana
skill-cost consumers and native attribute modifier behavior. These are
provisional starting values for later gameplay tuning.

The **provisional Tier 1 Cooldown range is 0.5% to 1% reduction** in Covenant
skill cooldowns. Use negative flat `ADD` modifiers on the existing Cooldown
coefficient (amounts -0.01 to -0.005), adding across gear. Before rarity boosts,
Tier 5 is 2.5% to 5% reduction; Legendary Tier 5 is 2.875% to 5.75% per affix.
Six maximum rolls give **34.5% shorter cooldowns**, or a **0.655 coefficient**
before other modifiers and existing upward tick rounding. Preserve existing
cooldown calculation, snapshots, and skill-specific lifecycle rules. These
are provisional starting values for later gameplay tuning.

The **provisional Tier 1 Cast Time range is 1% to 2% reduction** in skill
cast time. Use negative flat `ADD` modifiers on the existing Cast Time
coefficient (amounts -0.02 to -0.01), adding across gear. Before rarity boosts,
Tier 5 is 5% to 10% reduction; Legendary Tier 5 is 5.75% to 11.5% per affix.
Six maximum rolls give **69% shorter casts**, or a **0.31 coefficient** before
other modifiers and existing upward tick rounding. Gear alone at these values
does not make a positive base cast instant. Preserve existing instant skills,
instant-cast effects, and cast-time consumers. These are provisional starting
values for later gameplay tuning.

The **provisional Tier 1 Skill Duration range is +0.5% to +1%**, using positive
flat `ADD` modifiers on the existing Skill Duration coefficient (0.005 to 0.01),
adding across gear. Before rarity boosts, Tier 5 is +2.5% to +5%; Legendary
Tier 5 is +2.875% to +5.75% per affix. Six maximum rolls give **+34.5% duration**,
or a **1.345 coefficient** before other modifiers and existing timer rounding.
Apply through existing supported skill-duration consumers; preserve their
effect-specific snapshot and lifecycle rules. These are provisional starting
values for later gameplay tuning.

### Affix limits

An initialized item's rarity caps the rarity of later affixes: Rare equipment
accepts Common, Uncommon, and Rare affixes, but rejects Epic and Legendary.
Normal crafting operations must respect this ceiling without reassigning the
item's rarity. Affix rarity and numerical upgrade tier remain distinct concepts.

Equipment rarity also supplies a configurable numerical affix-tier ceiling:

| Equipment rarity | Maximum numerical affix tier |
|---|---:|
| Common | 1 |
| Uncommon | 2 |
| Rare | 3 |
| Epic | 4 |
| Legendary | 5 |

Crafting must satisfy both this ceiling and the crafter's unlocked tier; use
the lower of the two. Common equipment permits value rerolls within Tier 1
but has no room for a numerical tier upgrade. These limits are validated by the server.

## Crafting Potential

Each item has an item-wide **Crafting Potential** budget.

Provisional, configurable starting budgets are **20/30/40/50/60** for
**Common/Uncommon/Rare/Epic/Legendary**, respectively. Tune these alongside the
operation costs; they are not final balance. See [[Affixes and Rarity]].

- The starting affix's rarity definition supplies the initial Crafting Potential
  budget on the first successful Workbench Add, with that operation's cost deducted
  immediately. Rarity, affix application, and remaining Potential commit together.
- Freshly generated affixed loot starts with the full rarity-based budget,
  without an Add or Upgrade charge for its natural affix. Later crafting uses
  normal costs; subsequent drop/loot events do not reset initialized items.
- Initialization happens once. Removing affixes, including all affixes, never
  refills or reinitializes the budget.
- Affix crafting operations consume Crafting Potential.
- When the item's potential is exhausted, ordinary Covenant affix modification is no longer possible.
- The item remains usable and tradeable after its potential is exhausted.
- For the **first pass**, Add, Upgrade, Reroll, and Remove each cost **five
  Crafting Potential**, shown before committing. Use the same provisional cost
  across tiers.
- Fixed spending is explicitly temporary and expected to change, potentially
  soon. Variable spending or favorable efficiency outcomes remain future
  design work. Keep the cost calculation separate from validation and item
  mutation so the initial policy is easy to replace without a new framework.

Crafting Potential is distinct from affix capacity and individual affix maximums. All three constraints apply independently.

| Operation | First-pass Potential cost |
|---|---:|
| Add | 5 |
| Upgrade | 5 |
| Reroll | 5 |
| Remove | 5 |

## Supported operations

The prototype Affix Crafting interface uses **one equipment slot**, operation
and affix selectors, a visible material/Potential cost preview, and an explicit
**Craft** button. Draw required ingredients from the **acting player's
inventory**, without dedicated ingredient-staging slots. Previews and
selections spend nothing; the server revalidates the item, operation, access,
and costs before committing. This ingredient-sourcing flow is provisional.

The equipment slot is **shared, persistent Workbench storage**. Modify the
item in place after each completed operation, leaving it available for further
work or collection. It stays in the Workbench when the GUI closes, accessible
to other players, and persists across chunk unloads and world saves/restarts.
Breaking the Workbench drops it once with the remaining stored contents.
Ingredients and profession access still come from the acting player.

The first playable Covenant Workbench includes four core operations, enabled
by purchasing Basic Affix Crafting, with a small initial affix pool:

1. **Add:** Apply a new Covenant affix at **Tier 1** to eligible equipment.
2. **Upgrade:** Raise an existing affix by **one numerical tier**, within the
   item's limits and the crafter's unlocked tiers, and roll a **fresh random
   value in the new tier's permitted range**. Do not preserve relative roll quality.
3. **Reroll:** Randomize an existing affix's value within its current tier's
   permitted range.
4. **Remove:** Permanently remove an existing Covenant affix. For the prototype,
   spend **five Potential only**, with **no ingredients or component refund**,
   at every affix rarity and numerical tier. Preserve equipment rarity and all
   other affixes; do not restore Potential. The normal successful-operation XP
   reward still applies using the removed affix's rarity.

All four operations are implemented; see [Itemization](../docs/ITEMIZATION.md).

Tier 1 additions and one-tier upgrades are **provisional first-pass rules**
until random bonus successes are designed. Bonus-success chances and outcomes
are future work; do not introduce them in this first pass. Affix rarity remains
separate from numerical tier, so a Legendary starting affix can begin at Tier 1.

Players deliberately select the target affix and operation. The first pass
previews fixed Crafting Potential costs, with the spending policy explicitly
provisional. In this first pass, valid operations **always complete successfully**
without a separate failure roll. Reroll still randomizes the affix value within
its permitted range and can produce a worse result; guaranteed completion is
not guaranteed improvement.

Reject invalid or unaffordable operations without consuming materials or
Crafting Potential. Prototype ingredient costs are specified below and value-roll
ranges above; the ranges and five-Potential costs are provisional.

All four operations award **Crafting XP once per successful operation** to the
acting player, after the server completes the operation and spends its required
materials and Potential. A Reroll that produces a worse or unchanged value still
qualifies. Previews, rejected operations, and collecting/moving the resulting
item grant no XP. XP scales with the **affected affix's rarity**, with higher
built-in rarities paying more: the added affix for Add, the targeted affix for
Upgrade/Reroll, and the removed affix's rarity before Remove. Equipment rarity
does not determine this reward. The prototype uses **one XP reward per affix
rarity, shared across all four operations and all numerical tiers**:
provisionally **20 / 40 / 60 / 80 / 100 Crafting XP** for Common / Uncommon /
Rare / Epic / Legendary respectively. Keep these amounts data-driven. These
are agreed starting values for future implementation and playtesting, not
implemented XP acquisition; see [[Crafting Profession]] and
[[Experience and Reward Systems]].

## Affix availability and rare components

For the prototype, **100% of eligible equipment generated in mob drops and
chest loot receives Covenant affixes**, providing the first non-Common affixes
available for salvaging. Also increase equipment quantity through additional
loot rolls. Both the affix rate and extra-equipment rules are configurable for
later reductions toward vanilla balance; bonus equipment can be disabled.
The provisional mob bonus is a 25% chance for one additional affixed equipment
piece per player-killed hostile mob, rolled once per qualifying death. Vanilla
loot chests receive one guaranteed additional affixed equipment piece when
their loot is first generated, not on reopening. Both bonuses are configurable
and disableable. New affixed loot uses configurable equal rarity weights of
20% each for Common, Uncommon, Rare, Epic, and Legendary, for both ordinary and
bonus equipment. For now, each generated piece has exactly one random affix
matching the rolled rarity, leaving its remaining slots for crafting. Roll its
numerical tier uniformly from 1 through the equipment rarity's cap, inclusive,
independently of the recipient's Crafting level. This tier distribution is
provisional. Fresh loot starts with its full rarity-based Potential budget,
with no charge for its natural affix. Bonus equipment uses a configurable
starter pool of iron weapons/tools and the four iron armor pieces, plus bow,
crossbow, and shield; existing eligible loot retains its base item. See
[[Loot and Rewards]] for the twelve-item pool and remaining source details.

- Standard/common affixes become available through Crafting profession progression.
- For the **first pass**, adding a Common affix consumes ordinary vanilla
  ingredients directly and does not require a salvaged affix-family component.
  This makes Basic Affix Crafting usable at Level 1 before Salvaging unlocks
  at Level 5. **Only Common affixes use this direct vanilla-ingredient route.**
  The **provisional prototype recipe is three lapis lazuli plus five
  Potential for every affix family**; family-specific ingredients are deferred.
- After purchasing Basic Affix Crafting for one Profession Point, higher affix
  crafting tiers unlock automatically at Crafting Levels 5, 10, 15, and 20,
  without additional point costs. Item-specific affix limits still apply.
- **Uncommon, Rare, Epic, and Legendary affixes** require a corresponding
  affix-family component recovered through salvaging equipment carrying an
  eligible affix. Uncommon does not have a direct vanilla-only recipe route.
- Each eligible non-Common affix on a salvaged item receives its own independent
  component yield at a configurable **120% base recovery rate**: one guaranteed
  matching component plus a 20% chance of a second.
- Recovered affix-family components are consumable. Applying the corresponding
  non-Common affix consumes **3 for Uncommon, 3 for Rare, 4 for Epic, and 5 for
  Legendary** in the first pass; recovery is not a permanent
  account unlock.
- **Upgrade consumes the same 3/3/4/5 matching components as Add**, according
  to the targeted affix's rarity, at every numerical tier. Match both family
  and rarity exactly and also spend the provisional five Potential. A valid
  Upgrade raises numerical tier by one without changing affix or equipment
  rarity. Common equipment cannot exceed Tier 1; a Common affix on higher-rarity
  gear can be upgraded within the equipment and crafter's tier limits.
  For the **prototype, Common Upgrade costs three lapis lazuli plus five
  Potential**, with no affix-family component, at every valid target tier.
  This provisional recipe matches Common Add.
- **Reroll consumes 1/1/2/3 matching components** for
  Uncommon/Rare/Epic/Legendary, plus the provisional five Potential. Match the
  targeted affix's family and rarity exactly, independently of numerical tier.
  Spend the full cost even for a worse or unchanged result. Reroll preserves
  family, rarity, and numerical tier. For the **prototype, Common Reroll costs
  one lapis lazuli plus five Potential**, with no affix-family component;
  this recipe is provisional and also spends its full cost for worse or
  unchanged results.
- A recovered component retains **both the salvaged affix's family and its
  rarity** and must match both exactly when adding an affix. An Uncommon Health
  component adds Uncommon Health. Use the individual affix's rarity, not the
  equipment's initialized rarity; different component rarities occupy separate
  inventory stacks.
- For the prototype, **non-Common Add, Upgrade, and Reroll use only their
  matching components and five Potential**, with no additional ingredients.
  Generic salvage materials mainly support ordinary crafting for now.
- Crafting level may impose an additional requirement for applying particularly powerful affixes.

Initial components are implemented; further content and final affix balance remain open. See [[Salvaging]] for the
Apotheosis-inspired stockpile-and-spend direction and chosen Covenant defaults.

## Debug equipment setup

Operator-only `/covenant affix` commands now prepare valid test equipment and
supply non-Common family/rarity components. Setup bypasses profession access and
crafting costs, grants no XP, and preserves normal equipment limits. Set a family,
rarity, numerical tier and random/min/max roll; separately edit equipment rarity
or remaining Potential. Remove/clear preserve rarity and Potential; reset returns
the equipment to uninitialized Covenant state while preserving other item data.
Tooltips provide inspection. See [Itemization](../docs/ITEMIZATION.md#debug-commands)
for syntax, initialization and invalid-downgrade rules.

## Apotheosis and other mod compatibility

Covenant's Prefix/Suffix capacity, affix limits, and Crafting Potential apply only to Covenant affixes. Apotheosis affixes and other mod-provided item modifiers retain their own data, limits, and mechanics.

Both systems may coexist on the same item where technically compatible. Covenant must not overwrite, remove, or invalidate Apotheosis affixes, gems, sockets, or other item data. Apotheosis remains optional; Covenant's own affix and equipment customization systems must work without it.

See [[Crafting Profession]] and [[Salvaging]].
