# Open Questions

This file contains decisions that remain genuinely unresolved. Established progression rules are recorded in [[Progression Systems]], [[Character Progression]], [[Combat Skill Progression]], and [[Profession Progression]].

## Character and combat progression

- Playtested character-level pacing. The accelerating
  prototype curve, reference XP scale, and opening/final-level anchors are
  agreed in [[Character Progression]], with Classic WoW as the pacing-feel target.
  Observe kills per minute and time per level before treating that feel as
  validated; values remain configurable. The initial repeat-boss reference is
  a Wither kill at 2,000 base character XP before sharing/caps.
- Exact XP rewards by enemy/encounter category and milestone.
- Detailed damage/support attribution to enemies/encounters. Eligible contributors receive
  equal shares, counting each player once, with a refreshing 30-second window
  per player per enemy checked at payout. Each ordinary share rounds up to
  whole XP before caps. Support-only participation, including
  passive aura support tied to actual combat, already qualifies in the first slice; see
  [[Experience and Reward Systems]].
- Individual rank upgrades for each skill, using current prototype values at
  Rank 1 and minor, flavorful improvements under [[Combat Skill Progression]].
  Aura passive strengths stay fixed across ranks; their activation/coverage
  upgrades remain to be authored.

## Profession progression

Profession implementation design is in progress using [[Profession Progression]],
[[Mining]], [[Logging]], [[Farming]], and [[Crafting Profession]]. Those notes
record settled rules; the remaining questions below do not reopen them.

The first implementation chunk now provides menu/drafts/Apply, paid-cost saved
allocations, catalogue/reload handling, provisional curve and all-four respec.
Logging's Offensive Mastery and all four Crafting nodes have purchasable runtime effects.
Natural activity XP is now implemented for ore/log/crop harvests, credited
breeding and enchanting-table use. The other twelve gathering nodes remain deferred; see
[Profession foundation](../docs/PROFESSIONS.md). No tuning became final through
implementation.

Crafting XP rewards are now settled provisionally: enchanting-table options
pay **20 / 50 / 100 XP**; Common-through-Legendary affix operations pay
**20 / 40 / 60 / 80 / 100 XP**; salvaging affixed Common-through-Legendary
equipment pays **10 / 20 / 30 / 40 / 50 XP**. Unaffixed gear grants zero salvage
XP, including initialized gear whose affixes were all removed. See
[[Crafting Profession]] and [[Salvaging]] for triggers and attribution. Activity
integrations for the natural activities are implemented; Workbench XP and
actual leveling-time validation remain deferred.

Additional profession-level recipe milestones are deferred beyond the slice.
Workbench and respec recipes remain ungated; purchased abilities and automatic
Crafting affix-tier access provide the initial progression milestones. The
listed profession scope decisions are resolved for the prototype; technical
authoring and validation below remain, and final balance is still provisional.

**Implementation and validation follow-ups:**

- Profile Prospecting's loaded-chunk/palette search and choose its bounded
  server work budget. The 8/12/16/24/24-block radii remain conditional on an
  inexpensive search; ore tiers, the five-second cooldown, and the private
  ore-type/direction/Y report are settled in [[Mining]].
- Vein Mining and Tree Felling are implemented; supported-tree planting
  mappings and Automatic Replanting remain deferred. Ordinary-harvest XP, persistent placed-ore exclusion with native
  piston movement, and bounded tree-shape recognition are implemented; see
  [Profession activities](../docs/PROFESSIONS.md#natural-activity-xp). The
  tree-shape-plus-natural-leaves policy retains its documented conservative
  fallback. Broader tree coverage and actual leveling time need playtesting.
- Author remaining item/material/tag mappings, component representation,
  loot-source handling, menu layout/assets, and new profession/itemization
  datapack formats. These are implementation/content-authoring tasks within
  the chosen rules rather than requests to redesign the features.
- Validate server authority, cancellation, persistence/reload behavior,
  one-time XP payouts, and actual leveling time. Final balance remains
  provisional; automation XP is disabled by default, with optional integrations
  outside the current baseline.

The shared prototype curve is **100 × the target level per transition**,
totaling **21,000 XP per track**. Mining's initial reward table is complete in
[[Mining]]; Logging is **20 XP per eligible log**; Farming is **5 XP per mature
crop** and **100 XP per successful credited breeding**. See
[[Profession Progression]], [[Logging]], and [[Farming]].

## Dimensional gates and configuration

- Future modpack profession dimension-cap overrides. Current prototype
  defaults allow profession Level 20 in every dimension. The initial character
  route is Overworld 20, Nether 40, End 60, with the first-Wither level-reward
  exception; see [[Dimensional Progression Gates]].
- Server datapack JSON resource paths/field schema, validation/error reporting,
  reload failure handling, and tag-priority syntax. Balance changes preserve
  earned levels, points, allocations, and raw XP; percentages can change.
  Datapacks
  are the agreed source for character XP requirements, enemy rewards, and
  dimension-cap rules; see [[Progression Balance Configuration]].
- Which dimensions/tags receive curated defaults in the initial configuration. Unmatched-dimension fallback is established: character 60, professions 20.

## Crafting and itemization

- Future workstation additions beyond the settled prototype Covenant Workbench.
  The prototype's one Covenant Workbench has a settled shapeless recipe:
  one crafting table, four any/mixed planks, two iron ingots, and two lapis
  lazuli for one station. Crafting it has no profession requirement and gives
  no profession XP; purchased unlocks gate the operations. See [[Crafting Profession]].
- Remaining Workbench layout details. Salvaging's ten input slots, twelve
  result slots, output preview, explicit Salvage button, persistent shared
  storage, and stop-before-overflow behavior are settled in [[Salvaging]].
  Prototype Affix Crafting uses one equipment slot, operation/affix selectors,
  a cost preview, and a Craft button, sourcing ingredients directly from the
  acting player's inventory. Its equipment slot is persistent shared Workbench
  storage; operations modify the item in place, and closing the GUI leaves it
  available to other players. See [[Crafting Profession]].
- Future affix features beyond the prototype's selected Add/Upgrade/Reroll/Remove
  operations and salvage rules. Their acquisition, costs, and initial stat ranges
  are settled in [[Affix System and Equipment Customization]].
- Future optional enchanting integrations and magical customization. The
  prototype preserves native enchanting and uses it as the initial Crafting
  XP route, without a dedicated Covenant Enchanting branch.
- Future cross-profession recipes. Prototype Profession Respec is settled:
  four paper, four lapis lazuli, and one emerald, shapeless in an ordinary
  crafting table, producing one item without profession requirements or XP.
  Hold right-click for two seconds out of combat, like Combat Respec, to refund
  all four professions' allocations together while preserving their XP/levels.
  Success consumes one item; interrupted/invalid uses consume nothing. No
  profession selector or GUI respec button is needed. See [[Profession Progression]].

## Statistics and combat balance

- Final launch stat IDs/units, modifier order and caps, and diminishing-return behavior where applicable. The traditional primary-attribute system and attribute points are out of scope.
- Final balance of profession-derived combat effects relative to class skills and equipment.
- Class specialization/talent structure beyond the current class-specific skill trees, if any; no separate talent-point economy is currently established.

## Itemization detail

- Remaining loot source conditions and detailed bonus-pool weighting. The
  starter bonus pool is iron weapons/tools and armor, bow/crossbow, and shield;
  see [[Loot and Rewards]] for the twelve-item list. Fresh affixed loot
  starts with its full rarity-based Potential budget without a charge for its
  natural affix; later drops/transfers do not refill it. For now, it has one random affix matching
  its rolled rarity, with a uniform numerical tier from 1 through the rarity
  cap, independent of the recipient's Crafting level. Prototype rarity weights
  are 20% each across the five rarities.
  Mob drops
  and chest loot use a configurable **100% affix rate** for eligible equipment
  in the prototype, plus configurable bonus equipment; see [[Loot and Rewards]].
  The mob bonus starts at a 25% chance for one piece per player-killed hostile mob.
  Native Minecraft player kill credit is settled: classless players and spawner
  mobs qualify, environmental deaths qualify while native player credit remains,
  and each qualifying death rolls once for a shared drop. See [[Loot and Rewards]].
  Vanilla loot chests get one guaranteed bonus affixed piece on first loot
  generation, not on reopening; both bonuses are configurable and disableable.
  Initial Potential budgets are provisionally 20/30/40/50/60, with each
  operation provisionally costing five. See
  [[Affixes and Rarity]] and [[Affix System and Equipment Customization]] for
  the settled ladder, initialization, rarity ceilings, and slot capacities.
- Further affix content, final numerical balance, random bonus-success crafting mechanics,
  and unique/exceptional properties.
  Roll quality uses one base range per family/tier and best-of-1/2/3/4/5
  samples for Common through Legendary. Epic and Legendary then multiply the
  selected bonus magnitude by 1.05 and 1.15 respectively, provisionally.
  Later loot rarity should reflect their added power; equal prototype weights
  remain unchanged. See [[Affix System and Equipment Customization]].
  Health targets roughly ten bonus hearts across the whole endgame loadout,
  benchmarked with six Legendary Tier 5 Health affixes including their 1.15
  boost; this is not a per-affix target or global health cap. Its provisional
  Tier 1 range is +0.3 to +0.6 health points as a flat additive bonus, giving
  10.35 bonus hearts from six maximum Legendary Tier 5 rolls. These are
  starting values for later balance work; all ten initial families now have
  provisional ranges in [[Affix System and Equipment Customization]].
  Attack Damage provisionally uses +0.2 to +0.4 damage points at Tier 1 as a
  flat additive native bonus, for +13.8 Attack Damage across six maximum
  Legendary Tier 5 rolls. Preserve native, principally melee semantics and
  revisit these values in later balance work.
  Armor targets 80% vanilla mitigation against a 30-heart hit with six maximum
  Legendary Tier 5 Armor affixes on ordinary Netherite (20 armor, 12 toughness),
  requiring 32 total armor or roughly +12 from affixes including the Legendary
  boost. Armor provisionally uses +0.15 to +0.35 points at Tier 1 as a flat
  additive native bonus, retaining linear tier scaling. Six maximum Legendary
  Tier 5 rolls bring ordinary Netherite to 32.075 armor. Revisit these values
  after implementation; this is a tuning benchmark rather than a new armor
  cap or flat damage-reduction rule.
  Attack Speed should offer a substantial increase over vanilla recharge;
  its provisional Tier 1 range is +2% to +4%, using native `MULTIPLY_BASE`
  modifiers with percentages adding across gear. Six maximum Legendary Tier 5
  rolls give +138% (2.38 times normal weapon recharge speed before other
  percentage modifiers). Defer balance tuning to gameplay testing; native
  target hit immunity still limits repeated-hit damage throughput.
  Dodge provisionally uses +1 to +2 percentage points at Tier 1, giving 69%
  from six maximum Legendary Tier 5 affixes, 79% with ordinary Dodge Aura, or
  84% with Mining Defensive Mastery. Keep that range without increasing its
  upper bound for cap coverage; the aura bonuses and 85% cap are unchanged.
  Revisit overall balance later. See [[Affix System and Equipment Customization]].
  Movement Speed provisionally uses +0.5% to +1% at Tier 1 with native
  `MULTIPLY_BASE` percentages adding across gear. Six maximum Legendary
  Tier 5 rolls give +34.5% before other modifiers; native sprinting, status,
  and aura behavior still applies. Revisit these values in gameplay tuning.
  Resource Cost provisionally uses 0.5% to 1% skill-cost reduction at Tier 1,
  adding across gear through negative flat `ADD` modifiers on its coefficient.
  Six maximum Legendary Tier 5 rolls give 34.5% lower costs (0.655 coefficient)
  before other modifiers. Preserve existing skill-cost semantics; revisit
  these values in gameplay tuning.
  Cooldown provisionally uses 0.5% to 1% skill-cooldown reduction at Tier 1,
  adding across gear through negative flat `ADD` modifiers on its coefficient.
  Six maximum Legendary Tier 5 rolls give 34.5% shorter cooldowns (0.655
  coefficient), before other modifiers and existing upward tick rounding.
  Preserve existing cooldown semantics; revisit these values in gameplay tuning.
  Cast Time provisionally uses 1% to 2% reduction at Tier 1, adding across gear
  through negative flat `ADD` modifiers on its coefficient. Six maximum
  Legendary Tier 5 rolls give 69% shorter casts (0.31 coefficient), before
  other modifiers and existing upward tick rounding. Preserve existing
  instant skills/effects; revisit these values in gameplay tuning.
  Skill Duration provisionally uses +0.5% to +1% at Tier 1, adding across gear
  through positive flat `ADD` modifiers on its coefficient. Six maximum
  Legendary Tier 5 rolls give +34.5% duration (1.345 coefficient), before
  other modifiers and timer rounding. Preserve supported effects' snapshot
  and lifecycle rules; revisit these values in gameplay tuning.
- Future affix locking, extraction, and replacement features beyond the
  prototype's four selected operations.
- Further generic salvage material mappings beyond the implemented iron/diamond
  table, final output balance,
  and high-end salvage tuning toward four-plus components. Refinement
  is excluded from the current prototype. Focused Recovery
  adds 80 percentage points per rank for 200/280/360% totals. Non-Common Add
  and each one-tier Upgrade cost 3/3/4/5 components matching the affix's family
  and rarity for Uncommon/Rare/Epic/Legendary, plus five Potential; these costs
  are provisional and do not scale with numerical tier. Reroll costs 1/1/2/3
  matching components for the same rarity order, plus five Potential, even
  for worse or unchanged results. Common Add provisionally costs three lapis
  lazuli plus five Potential for every affix family, with family-specific
  ingredients deferred. Common Upgrade uses the same three lapis plus five
  Potential at every valid target tier. Common Reroll provisionally costs one lapis
  lazuli plus five Potential, without an affix-family component.
  Remove costs only five Potential for the prototype, with no ingredients or
  component refund, while still awarding normal successful-operation XP.
  Non-Common Add, Upgrade, and Reroll use only matching components and Potential
  in the prototype, with no extra ingredients. Tier caps belong to equipment
  rarity, so a Common affix on higher-rarity gear can still be upgraded within
  the item and crafter's tier limits. The accepted component-production loop
  consumes base equipment, with generic refunds offsetting part of its cost.
  The base component-recovery rate is 120% per eligible non-Common
  affix (one guaranteed component plus a 20% chance of a second); generic
  material recovery remains a separate 50% base roll, raised to 70/85/100% by
  Recovery ranks I/II/III, with Rank III guaranteeing the defined partial
  refund. Generic recycling should remain lossy; check output
  quantities and recipes for material duplication. Generic outputs use
  datapack-defined partial refunds with a prototype default of **one primary
  material per successful generic roll**: one iron ingot from iron gear or one
  diamond from diamond gear, equally for weapons, tools, and armor. Remaining
  item/material mappings need authoring; output items/counts stay data-driven
  for modpack tuning.
  Durability does not alter generic refunds, recovery chances, or component
  yields; damaged and pristine equipment use the same rules.
  Salvaging destroys enchantments without returning enchanted books or
  vanilla XP. Crafting profession XP pays once per affixed item actually consumed,
  including zero-output salvage, to the player pressing Salvage; collecting
  outputs does not award XP again. Rewards scale with equipment rarity, with
  higher built-in rarities paying more. Items with no Covenant affixes at
  consumption give zero XP, including uninitialized gear and initialized gear
  whose affixes were all removed. Vanilla enchantments alone do not qualify;
  normal eligible material returns remain available. The agreed provisional
  salvage rewards are 10/20/30/40/50 XP for affixed Common-through-Legendary
  equipment; actual pacing remains to be playtested.
  See [[Salvaging]].

## World and endgame

- Region progression map, danger signaling, encounter reward design, and how dimensions/regions map onto world content.
- Challenge-tier access, modifiers, reward scaling, and repeatable endgame loop within the level-60 character cap.
- Additional region/dungeon/boss scope and long-term rewards.

Related: [[Design Decisions]], [[Progression Systems]], [[Development Roadmap]].
