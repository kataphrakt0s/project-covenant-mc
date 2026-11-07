# Progression Vertical Slice

## Goal

Deliver a complete, playable progression framework that demonstrates both Covenant combat progression and Minecraft activity mastery while remaining vanilla-friendly. The slice includes all four initial professions, but only a deliberately modest first selection of actual profession content.

## Current implementation boundary

The combat slice and first expanded profession foundation are implemented.
Professions now have their own radial entry, four-tab drafts/Apply, paid-cost
saved allocations, server-datapack catalogue, provisional shared XP curve,
operator testing commands and all-four held-use respec. Logging's Offensive
Mastery and all four Crafting nodes are functional. The other twelve gathering
nodes remain planned. Workbench itemization, loot and salvage now close the
find/salvage/customize loop; see [Itemization](../docs/ITEMIZATION.md). Natural
ore/log/crop harvest XP, credited breeding and enchanting-table XP are now
implemented, with chunk-saved placed-ore exclusion and bounded tree recognition.
See [Profession foundation](../docs/PROFESSIONS.md). This implementation
does not finalize tuning or resolve the remaining questions in [[Open Questions]].

## Included systems

### Implemented itemization slice

The itemization prototype delivers the complete find/salvage/customize
loop: initial affixed loot, component recovery, and Workbench Add, Upgrade,
Reroll, and Remove, including their Crafting progression integration. Retain the
settled system rules below rather than delivering only Common-affix crafting.
Use Minecraft's netherite block texture as the Covenant Workbench placeholder.
See [[Milestone 3 - Itemization and Crafting]]. The first generic refund table
covers only iron and diamond gear; other eligible equipment can yield affix
components without a generic material refund. Rendered presentation and final
balance remain for playtesting.

The mob bonus uses Minecraft's native player kill credit, without a class
requirement: classless players and spawner mobs qualify, and environmental
deaths qualify while native player credit remains. Roll once per qualifying
hostile death for shared dropped equipment, not once per XP contributor.

### Save compatibility scope

Existing prototype-save compatibility is not required for this progression
slice. Plan and validate against fresh saves using the new progression format.
Do not add legacy decoders, free-skill conversion, a one-time allocation reset,
or another migration system solely to carry prototype saves forward.

Progression created under the new format must still persist correctly across
save/load, restart, reconnect, death/respawn, and dimension changes. This scope
decision removes legacy migration work, not normal persistence requirements.

### Character progression

- Covenant character level 1–60.
- One starting skill point at level 1, then one additional skill point per character level (60 total).
- Character progression begins on first combat class selection at Level 1 with
  zero character XP and one skill point. Classless players cannot earn or bank
  character XP; earlier combat grants no retroactive XP. Profession progress is
  preserved when a class is selected.
- Combat XP earned primarily from hostile combat, supplemented by major milestones.
- The default character dimension route is Overworld 20, Nether 40, End 60.
  A player's first qualifying Wither kill grants one full character level past
  the dimension gate, once per player. Later Wither kills use normal XP and caps.
  Every eligible unclaimed contributor, including support-only players, gets
  their own full level; group size and the final blow do not divide or reserve
  this milestone. Class selection and the global level cap still apply. Persist
  each player's claim. Preserve percentage XP progress through the level, with
  integer XP rounded down as needed; reaching the global cap banks no excess.
  The first-time milestone replaces that player's ordinary Wither XP share;
  repeat recipients receive normal shares under the usual caps. Forgone ordinary
  shares are not redistributed. Mark the claim only when the level is granted,
  committing it together with level, preserved progress, and the skill point.
  Classless/global-capped players do not consume it, and no ungrantable reward
  is queued or paid retroactively.
- Initial opening tuning targets roughly five ordinary hostile kills while solo
  to reach character Level 2. The starting Level 59-to-60 benchmark is 3,000
  ordinary vanilla Enderman kills or 30 reference boss kills at full solo rewards
  where the dimension gate allows Level 60. Include vanilla Endermen explicitly
  as XP sources. Use the agreed accelerating curve of roughly 13% more required
  XP per level, with reference rewards of 10 XP per ordinary mob, 20 per Enderman,
  and 2,000 per reference boss. Requirements run from 50 XP for Level 1-to-2 to
  60,000 for Level 59-to-60; see [[Character Progression]] for the exact formula
  and table. Classic WoW is the pacing-feel reference, subject to playtesting.
  The reference boss is a repeat Wither kill at 2,000 base character XP before
  sharing/caps, with the first-time level milestone replacing that player's
  ordinary share. The 30-kill final-level benchmark requires those repeat kills
  in a dimension allowing Level 60 (the End by default). Other enemy/boss rewards
  remain tunable; profession pacing is separate.
- Hostile mobs are the default combat XP sources. Other combat mobs, including
  neutral mobs, require explicit inclusion through entity IDs or tags. Ordinary
  livestock and player kills grant no character XP in the default slice.
- Eligible spawner-produced combat mobs award the same configured XP as
  equivalent naturally spawned mobs, using normal contribution and cap rules.
  No spawn-origin penalty or provenance tracking is required. Full Apotheosis compatibility is an eventual
  project target; this slice remains standalone without an integration dependency
  or a claim of tested compatibility. See [[Mod Compatibility]].
- Separate from vanilla XP orbs.
- Persistent, server-authoritative profiles.

### Combat skill progression

- Existing Stalker, Reaver, and Weaver trees.
- Diagram level requirements gate initial skill investment only.
- First point unlocks Rank 1; subsequent points increase rank.
- Rank 1 preserves current prototype behavior and values. Further ranks provide
  minor, flavorful, skill-specific upgrades, avoiding flat damage per rank or
  routine damage scaling. Total growth through maximum rank must remain modest
  enough to preserve vanilla balance; see [[Combat Skill Progression]].
- Aura passive strengths remain at Rank 1 values across ranks. Their rank
  upgrades may modestly improve activations or coverage, with existing stacking
  rules retained; exact tables remain to be authored.
- Most arrow-shot rank upgrades use arrow velocity for the prototype, including
  current Piercing Shot and Aimed Shot. More varied rank progression is deferred.
  Each extra rank adds 2% of Rank 1 launch velocity, reaching +14% at Rank 8.
  Retain native speed-based arrow damage, accepting its indirect damage growth;
  do not add damage compensation. Aimed Shot retains its current wind-up
  movement behavior. Other skills' rank tables remain a later content dependency.
- Until their upgrades are authored, non-arrow playable skills allow only Rank 1.
  Keep their planned 8/12 maximums as metadata; never sell a rank with no benefit.
- Per-skill maximum rank 8 or 12.
- Initial playable caps: Lunging Combo and Rip and Rush each cap at 12; all
  other currently playable combat skills cap at 8. Define caps per skill;
  unfinished skills' future caps remain open.
- Maximum eight distinct skills with points allocated at once.
- Server-enforced prerequisites and mutual exclusions.
- Skills tree clicks stage upgrades: left-click adds one pending rank and
  right-click removes one pending rank, never below the committed rank. A simple
  Apply button at the bottom right submits the complete draft for server
  validation and commit. Pending upgrades grant no gameplay effects or binding
  access. Invalid/stale requests cannot partially spend points or allocate ranks.
  Closing without Apply discards unapplied edits with no confirmation dialog;
  reopening reads committed server state. Drafts are not saved or restored.
  Editing can happen in combat, but Apply requires being out of combat using
  the shared seven-second timer, checked on the server at commit. A blocked
  Apply spends nothing and retains the local draft while the screen is open.
- Crafted respec item that refunds combat skill allocations.
- Prototype combat-respec recipe: four paper plus one lapis lazuli, shapeless
  at a vanilla crafting table, producing one item. Hold right-click for two
  seconds (40 server ticks) to use it; releasing early cancels without spending
  it. Successful server-validated completion consumes one and refunds combat
  allocations. Respec requires being out of combat at start and completion;
  entering combat during the hold cancels without consuming the item. Use one
  seven-second (140-tick) combat timer for all classes and Rage grace, refreshed
  by confirmed combat damage dealt/taken and qualifying support. The timer and
  support alone do not generate Rage; see [[Combat]].

#### Agreed combat-content boundary (planned)

No new combat skills are required for this slice. Add progression and meaningful
rank upgrades to existing playable skills that are reachable through implemented
prerequisites. Unfinished nodes remain visible but cannot receive points. A
playable skill behind an unfinished prerequisite also remains unavailable for
investment until that prerequisite is implemented; do not bypass or rewrite the
intended prerequisite chain to make it available in this slice.

This limits initial build variety while allowing the progression loop to be
demonstrated across Stalker, Reaver, and Weaver. Completing the remaining combat
skill roster is separate work. These are planned allocation rules; the current
free-allocation prototype has not yet changed.

### Professions

#### Content dependency

Profession implementation design is now being worked through using
[[Profession Progression]], [[Mining]], [[Logging]], [[Farming]], [[Crafting Profession]],
[[Affix System and Equipment Customization]], and [[Salvaging]] as its starting
requirements. Preserve their established ability concepts, costs, and level
gates while resolving remaining behavior, balance, and the initial playable
selection. This does not make every documented future ability a slice requirement.

Radial slot **3** will open **Professions**, separate from combat **Skills**,
including access before class selection. Four tabs select Mining, Logging,
Farming, or Crafting. Show one complete tree at a time, fitting on one page
without panning or scrolling, with that profession's level, XP, and available
points above it. The saved-track foundation is implemented; profession UI and
playable content are not.

Profession purchases use local drafts: left-click adds a pending rank at its
defined point cost; right-click undoes pending purchases without reducing
committed ranks. Drafts survive profession tab switches. **Apply** commits only
the displayed profession's purchases through server validation, leaving other
drafts pending. Closing discards all unapplied edits; pending ranks grant no
benefits, and committed purchases require profession respec. Players can browse
and draft during combat, but Apply requires the existing shared seven-second
combat timer to have expired. The server rechecks combat state on submission;
a blocked request spends nothing, retains the local draft while open, and does
not queue a purchase. See [[Profession Progression]].

All four are included:

- Mining
- Logging
- Farming
- Crafting (Enchanting is a subset)

Profession progression starts on first login and does not require a combat class
or character combat level. Players can earn XP, spend Profession Points, and use
earned profession benefits before selecting a class. Later class selection
preserves all profession progress and allocations. See [[Profession Progression]].

Each starts at level 0, defaults to level 20, shares a profession XP curve, and
has independent XP/points. One Profession Point is earned per level. The slice
implements a modest playable selection of level-gated abilities and variable-
cost investments, with automatic Crafting affix-tier access at Levels
5/10/15/20 after purchasing Basic Affix Crafting. Additional profession-level
recipe milestones are deferred beyond this slice. Workbench and respec recipes
remain ungated; no additional recipe-unlock framework is required for the slice.

The slice does not require all 20 levels to be fully populated or every future ability to be designed. Most eventual profession options should be obtainable, so the initial selection should demonstrate broad mastery rather than artificially restrictive builds.

Planned Mining XP comes from successful eligible ore extraction, once per
block, including each eligible Vein Mining block. Player-placed ores grant no
XP; the exclusion survives save/load and restart and applies to every player.
Silk Touch still earns XP on initial extraction of ore that was not player-placed.
Fortune and drop processing do not multiply XP, and Prospecting grants none.
See [[Mining]] for the agreed rules; activity sources are not yet implemented.

Planned Vein Mining follows all 26 neighboring block positions, including
face, edge, and corner connections, up to the rank's block limit. Veins touching
only diagonally can be mined together. Membership uses configurable ore-family
tags: stone and deepslate variants of one ore join, while different ore families
remain separate. Modded variants can opt into those families. The configurable
prototype limits for ranks I-IV are **8/16/32/64 blocks**, including the initial
block. Larger deposits require another mining action. Each block uses normal
tool durability and native loot/enchantment behavior, including Unbreaking,
Fortune, and Silk Touch. Tool breakage stops the activation immediately,
leaving remaining ore intact. Each harvested block also applies normal mining
exhaustion. Vein Mining has no cooldown, class-resource cost, or additional flat
activation charge and remains usable before class selection. See [[Mining]].

Vein Mining requires a mining tool that passes native harvest eligibility for
each targeted block, including suitable modded tools. An unsuitable tool leaves
additional blocks untouched and preserves ordinary single-block behavior.

Prospecting's configurable prototype radii are **8/12/16/24/24 blocks** for ranks
I-V, measured in all directions from the clicked block. Acceptance is conditional
on an inexpensive search validated by profiling. Prospecting has a five-second
(100-tick) per-player survey cooldown shared across ranks. The planned approach uses loaded
chunks, native section-palette rejection, and search work spread across ticks
under a shared server budget. This approach is source-checked but not implemented
or benchmarked. See [[Mining]].

The **provisional lowest Mining reward band is 10 XP per eligible original
coal, copper, or Nether quartz ore block**. Relevant stone/deepslate variants
share the family's reward. This requires no purchased ability or Prospecting
rank; ten eligible blocks in this band reach Level 1 under the shared curve.
**Iron provisionally awards 17 XP per eligible original ore block**, for both
stone and deepslate variants, without a purchased ability or Prospecting gate.
**Gold, lapis, and redstone provisionally award 25 XP per eligible original
ore block**, including Nether gold and relevant stone/deepslate variants.
**Diamond and emerald provisionally award 50 XP per eligible original ore
block**, shared by stone and deepslate variants.
**Ancient debris provisionally awards 200 XP per eligible original block**,
independently of the Level 20 Prospecting detection unlock.
Per-block extraction/placement eligibility rules still apply, independently
of drop quantity, Fortune, and later processing. The initial ore-family rewards
are now specified; remaining modded mappings and actual leveling-time balance
still need validation and tuning. See [[Mining]].

Prospecting ore access is cumulative: coal/copper/Nether quartz at Mining
Level 1, iron at Level 5, gold (including Nether gold)/lapis/redstone at
Level 10, and diamond/emerald at Level 15, including stone/deepslate variants
where applicable. Rank V at Level 20 is the ancient debris detection capstone,
requiring Rank IV and one additional point, with the radius retained at 24
blocks. Prospecting's five ranks cost five points total; Mining's defined
investment is 16 of 20 points. See [[Mining]].

The Mining tab has a Prospecting target selector, defaulting to **Any Ore**,
with individual ore families available according to committed Prospecting rank.
A survey reports the nearest matching eligible deposit; **Any Ore** searches
all unlocked families. Selecting one family prevents nearer ores of other
families from masking it. For the prototype, report the ore type, compass
direction, and Y coordinate in private chat to the surveying player, allowing
the result to be reread while digging. See [[Mining]].

Auto-Smelt's planned default eligible drops are raw iron, raw copper, raw gold,
and ancient debris, processed through their current furnace recipes. Stone and
cobblestone drops remain unchanged by default. Eligibility remains configurable;
normal loot quantities are calculated first, and Silk Touch suppresses the
processing. The Mining tab has an Auto-Smelt on/off toggle, enabled by default
when purchased; disabling it retains raw materials without requiring respec.
Conversion consumes no fuel and has no additional activation cost; ordinary
mining durability and exhaustion still apply. See [[Mining]].

Transfiguration's planned configurable default conversions are **raw copper to
raw iron** and **raw iron to raw gold**, each with a **4%** prototype trigger
chance. Transfiguration is always active once purchased and has no player-facing
toggle. Roll once per mined block; a successful roll replaces that block's
entire eligible drop quantity at a configurable **1:1** default ratio. Fortune
affects drop quantity without increasing the trigger chance. Processing order
is **normal loot -> Transfiguration -> Auto-Smelt**. Transfiguration rolls only
once per block without chaining conversions; Auto-Smelt can process the
replacement resource when enabled and eligible, retaining its Silk Touch
suppression. See [[Mining]].

Mining's Defensive Mastery is **recipient-owned and class-independent**. The
purchaser receives stronger Fortitude/Dodge Aura effects on themselves,
including auras supplied by other players, and improved Mage Armour when using
that self-buff. A caster's mastery does not strengthen their aura for other
recipients. For example, a Reaver using Ripping Aura can still benefit from an
ally's Dodge Aura through their own mastery. Normal skill access, aura coverage,
and class-tree exclusions remain in force. Mastery does not grant the underlying
effects by itself. On the purchaser, Dodge Aura grants **+15 percentage points
of Dodge** instead of +10, Fortitude grants an **eight-point total shield**
instead of six, and Mage Armour gains **+2 Armor and +1 Armor Toughness** on top
of its own bonuses. Fortitude's passive armor/toughness, existing durations,
caps, Dodge guarantees, and aura overlap/replacement rules remain unchanged.
These are specific bonuses, not a common percentage multiplier. Mage Armour's
base skill remains planned separately; profession effects are not implemented
yet. See [[Mining]].

Tree Felling's planned configurable limits are **4/8/16/32 logs** for ranks
I-IV, including the initially mined log. Rank gates remain Logging Levels
1/5/10/15 at one point per rank. Trees exceeding the cap can remain partially
standing after an activation. Tree recognition avoids tracking and saving every
player-placed log. Instead, require both a bounded base/trunk/canopy shape check
and nearby naturally grown, non-persistent leaves when chopping. Leafless trees
may fail, and uncertain cases use ordinary chopping. Naturally generated and
sapling-grown trees can qualify; this remains a heuristic with tree-like builds
as an edge case. Manually harvesting qualifying sapling-grown trees has the same
Logging XP eligibility as equivalent wild trees. Player-tended renewable tree
plots are valid progression sources; automated harvesting grants no Logging XP
by default. Award configured XP once per eligible log successfully harvested,
including each log removed by player-triggered Tree Felling, with no ability
XP multiplier or drop-quantity multiplier. Ordinary manual harvesting can earn
XP before purchasing Tree Felling. The **provisional reward is 20 XP per
eligible log**, shared across supported species and qualifying wild/sapling-grown
trees. Five qualifying logs reach Level 1 under the shared curve. These are
starting values for later balance work. See [[Logging]] for planned behavior
and remaining details.

Tree Felling follows Vein Mining's harvesting-cost rules: normal durability,
exhaustion, native loot, and enchantment handling for each log, stopping when
the axe breaks and leaving remaining logs intact. It has no cooldown, extra
activation cost, or class-resource cost. Completed harvests keep their drops
and eligible Logging XP. See [[Logging]].

Logging's Automatic Replanting triggers only when the current Tree Felling
activation removes the whole recognized tree. If the log cap or axe breakage
leaves it incomplete, skip replanting without consuming a sapling. Do not track
unfinished trees for later replanting; manual replanting in larger tree farms
is an accepted trade-off. A successful replant consumes the matching sapling
from inventory and requires valid planting conditions. See [[Logging]].

Right-click a mature supported crop with a hoe to harvest. With Scything
purchased, process its 3x3/5x5/9x9 rank area; holding sneak limits the action to
the targeted crop. The square uses one horizontal X/Z layer at the target's
Y level; terraced fields require separate actions at each height. There is no
persistent Scything-mode toggle. If Automatic
Replanting is learned, the same right-click action attempts to replant each
harvested crop using inventory planting items, including while sneaking.
Harvested crops stay removed when replanting cannot succeed. This is a
right-click interaction, not left-click breaking; ordinary hoe interactions
remain in place when no mature supported crop is targeted. See [[Farming]].

Each successfully harvested crop uses one point of hoe durability, including
the sneaking single-crop action, with Unbreaking applying normally. Hoe breakage
stops harvesting and leaves remaining crops untouched. Already harvested crops
retain normal drops and can complete their learned replanting step; replanting
adds no separate durability charge. See [[Farming]].

The initial supported harvest-and-replant crops are **wheat, carrots, potatoes,
beetroot, and Nether wart**, using data-driven definitions for maturity and
planting items. Cocoa, berries, and multi-block crops require additional
handling and are deferred from this initial set. See [[Farming]].

For now, Farming XP comes **only from mature supported crop harvests and
successful player-attributed breeding**. Award crop XP once per mature crop
harvested, including ordinary harvesting and each Scything crop, without ability
or drop-quantity multipliers. The **provisional crop reward is 5 XP per mature
supported crop**, shared by wheat, carrot, potato, beetroot, and Nether wart.
Twenty mature harvests reach Level 1 under the shared curve. Ordinary harvesting
earns XP before purchasing Scything. Planting/replanting, Bone Meal use, feeding alone, milking, shearing,
passive collection, and automation grant no Farming XP. Feeding or love mode
alone is not successful breeding. The **provisional breeding reward is 100 XP
once per successful event**, paid to vanilla's credited breeder without
splitting or duplicating it between players who fed different
parents. See [[Farming]].

Free Meal's Cultivation and Husbandry branches each use configurable
**20% / 30% / 40%** preservation chances at ranks I/II/III. Chances replace the
previous rank's value rather than adding together. Existing Farming level gates
5/10/15 and one-point costs per rank remain in place. Both branches can be
purchased independently. See [[Farming]].

Free Meal's Husbandry branch can preserve food used in normal feeding,
including breeding preparation, baby growth, and animal healing. Roll only
when the interaction would normally consume food; preserving the item retains
the feeding effect. Invalid attempts get no roll, and feeding alone still
grants no Farming XP. See [[Farming]].

Vitality reduces saturation lost to normal exhaustion across activities,
including native regeneration, sprinting, and harvesting. Preserve native
exhaustion thresholds, healing amounts, and regeneration timing. Saturation
can still reach zero, after which normal hunger depletion resumes; there is
no permanent saturation floor or extra reserve resource. Configurable prototype
reductions are **10% / 20% / 30%** for ranks I/II/III, replacing the previous
rank's value. Farming level gates 10/15/20 and one-point rank costs remain in
place. At Rank III, a normal one-point drain becomes 0.7 points, clamped at zero
saturation. See [[Farming]].

Successful vanilla enchanting-table use is Crafting's starting XP route from
Level 0 to Level 1, before purchasing Basic Affix Crafting. This preserves the
normal vanilla progression requirement to gain access to enchanting first.
Native enchanting remains available at Crafting Level 0 with its normal
requirements and costs; ordinary vanilla crafting still grants no Crafting XP.
Each successful operation awards Crafting XP once: provisionally **20 / 50 /
100 XP** for the first, second, and third options respectively, regardless of
displayed level requirement. Neither levels spent nor the number of
enchantments produced scales the reward. These values are implemented and
remain provisional for playtesting.
See [[Crafting Profession]].

After purchasing Basic Affix Crafting for one point at Crafting Level 1, higher
affix tiers unlock automatically at Crafting Levels **5, 10, 15, and 20** with
no further point costs. Equipment eligibility, affix capacity and item-specific
limits, materials, and remaining Crafting Potential still constrain operations.
See [[Crafting Profession]] and [[Affix System and Equipment Customization]].

The Covenant Workbench prototype uses an ordinary **shapeless crafting-table
recipe** producing one station from **one crafting table, four planks, two
iron ingots, and two lapis lazuli**. Accept any/mixed plank types via the
native planks ingredient tag. Crafting the station has no profession-level
or purchased-ability requirement and grants no profession XP; profession
unlocks gate the operations. This recipe and station are implemented.

The first playable Covenant Workbench includes **Add, Upgrade, Reroll, and
Remove** affix operations under Basic Affix Crafting, with a small initial
affix pool. All four are selected slice scope; their ingredient costs and
initial stat ranges are now specified as provisional prototype tuning.
See [[Affix System and Equipment Customization]].

The first pass uses **five Crafting Potential for each Add, Upgrade, Reroll,
and Remove**, across tiers, shown before committing. Fixed spending is explicitly
temporary, expected to change and possibly soon. Keep the cost calculation
separate from validation and item mutation so this policy is easy to replace;
the later spending model remains under design.

For the first pass, valid affix operations always complete successfully without
a separate failure roll. Reroll still randomizes the value within its permitted
range and can worsen it. Invalid or unaffordable requests consume no materials
or Crafting Potential. See [[Affix System and Equipment Customization]].

For now, Add creates a **Tier 1** affix, and each Upgrade raises it by **one
numerical tier**, respecting equipment limits and unlocked crafting tiers.
Upgrade rolls a fresh random value in the new tier's permitted range without
preserving the previous roll's relative quality. Reroll randomizes within the
current tier's range.
This is provisional until random bonus successes are designed. Affix rarity
remains separate from numerical tier: a Legendary starting affix can be Tier 1.
See [[Affix System and Equipment Customization]].

Introduce a **Covenant-owned equipment rarity system** with server datapack
rarity definitions and item/tag-based opt-in rules. Modpack developers and
other mods can author participation through data, without a hardcoded Covenant
profile for every modded material. Equipment rarity is distinct from individual
affix rarity. The built-in ladder is **Common -> Uncommon -> Rare -> Epic ->
Legendary**, with datapacks able to extend or replace the definitions. Exact
rarity parameters and loot assignment remain under design. See [[Affixes and Rarity]].

Covenant equipment rarity belongs to the individual item instance and is saved
with its stack. Two copies of the same base item can have different rarities.
Inventory/container movement, drops, trading, player transfers, and save/reload
preserve the assigned rarity. Ordinary crafted equipment begins with
**uninitialized Covenant rarity**; its first successful affix application gives
the resulting item **the starting affix's rarity**. A Common starting affix
produces Common equipment, while a Legendary starting affix produces Legendary
equipment. Subsequent Add, Upgrade, Reroll, and Remove operations preserve that
rarity, including removal of the starting affix or all affixes. Affix removal
does not restore the uninitialized state. For a first Workbench Add to crafted
gear, initialize Crafting Potential once
from the starting affix's rarity definition, then deduct the first Add cost;
rarity, affix application, and remaining Potential commit together. Invalid or
unaffordable first applications initialize nothing. Removing affixes never
refills or reinitializes Potential. Provisional starting budgets are
**20/30/40/50/60** for **Common/Uncommon/Rare/Epic/Legendary**, configurable and
to be revisited alongside the provisional five-Potential operation costs.
See [[Affixes and Rarity]].

Initialized equipment rarity caps the rarity of subsequent affixes. For
example, Rare equipment accepts Common/Uncommon/Rare affixes and rejects
Epic/Legendary, while retaining its original equipment rarity. This rarity
ceiling is distinct from numerical affix upgrade tiers. See
[[Affix System and Equipment Customization]].

Built-in rarity definitions provide configurable prefix/suffix capacities:
**1+1 for Common/Uncommon**, **2+2 for Rare/Epic**, and **3+3 for Legendary**,
with a six-affix starting ceiling on Legendary equipment. These are planned
crafting limits. See [[Affix System and Equipment Customization]].

Equipment rarity caps numerical affix tiers at **1/2/3/4/5** for
**Common/Uncommon/Rare/Epic/Legendary**. The crafter also needs the relevant
tier unlocked at Crafting Levels **1/5/10/15/20** after purchasing Basic Affix
Crafting. The effective ceiling is the lower of the item and profession limits.
Common equipment therefore permits Tier 1 value rerolls but no numerical tier
upgrades. See [[Crafting Profession]] and [[Affix System and Equipment Customization]].

**Add, Upgrade, Reroll, and Remove each award Crafting XP once per successful
operation** to the acting player, when the server completes the operation and
spends its required materials and Potential. A worse or unchanged Reroll still
earns XP. Previews, rejected requests, and collecting/moving the resulting
equipment do not award XP. Rewards scale with the **affected affix's rarity**,
with higher built-in rarities paying more: the added affix for Add, targeted
affix for Upgrade/Reroll, and the removed affix's rarity before Remove. A Common
affix on Legendary equipment still pays the Common affix-operation reward.
The prototype uses **one XP reward per affix rarity, shared across all four
operations and all numerical tiers**: provisionally **20 / 40 / 60 / 80 / 100
Crafting XP** for Common / Uncommon / Rare / Epic / Legendary respectively.
These amounts are implemented in server datapack definitions and remain
provisional for playtesting.

For the prototype, the Affix Crafting interface uses **one equipment slot**,
operation and affix selectors, a visible material/Potential cost preview, and
an explicit **Craft** button. Required ingredients come directly from the
**acting player's inventory**, with no dedicated ingredient-staging slots.
Selections and previews spend nothing. The server revalidates the item,
operation, access, and costs before committing. This ingredient-sourcing flow
is provisional. The equipment slot uses **shared, persistent Workbench
storage**, with operations modifying the item in place for further work or
collection. The item remains when the GUI closes, accessible to other players,
and persists across chunk unloads and world saves/restarts. Breaking the
Workbench drops it once with the other stored contents. Each operation still
uses the acting player's ingredients, access, and progression.
See [[Crafting Profession]].

Each item permits at most **one Covenant affix per family**, regardless of
rarity, numerical tier, or prefix/suffix category. For example, two Health
variants cannot occupy separate slots on one item. Duplicate additions are
rejected without spending costs; this restriction does not span multiple
equipped items. See [[Affix System and Equipment Customization]].

The initial affix pool has **Health, Attack Damage, Armor, Attack Speed, and
Dodge as prefixes**, and **Movement Speed, Resource Cost, Cooldown, Cast Time,
and Skill Duration as suffixes**.
Use existing stat semantics; native Attack Damage is principally melee, and
Attack Speed affects attack recharge. Class-resource affixes such as Mana
Capacity are deferred from this initial set. All ten families are allowed on
any affix-eligible equipment by default; datapacks can restrict individual
families by equipment category, item tags, or item IDs. Offensive bonuses on
armor and defensive bonuses on weapons are valid defaults. Initial bonus
ranges scale linearly: **Tier N uses N times the Tier 1 bonus range**. This is
explicitly provisional and will need later adjustment; individual tier ranges
remain editable in datapacks. Scale bonus magnitudes, including reductions,
not whole effective stats or their default coefficients. All ten initial
families have provisional Tier 1 ranges; see the summary table in
[[Affix System and Equipment Customization]].

Use one base range per family/numerical tier across affix rarities. Common
through Legendary draw **1/2/3/4/5 independent samples and keep the strongest
bonus**, using the individual affix's rarity. Epic then multiplies the selected
bonus magnitude by **1.05**, and Legendary by **1.15**; other rarities use 1.00.
Apply this to loot, Add, Upgrade, and Reroll, including beneficial reduction
magnitudes with their sign preserved. Scale the bonus, not the whole effective
stat; the Epic/Legendary boosts are not cumulative. They are provisional, with
later loot rarity intended to match the added power. Equal prototype loot
weights remain unchanged. See [[Affix System and Equipment Customization]].

For Health, target **roughly ten bonus hearts (+20 health points) across a
whole endgame loadout**, using six equipped Legendary Tier 5 Health affixes
as the benchmark (four armor pieces plus main hand/offhand). Include the
Legendary 1.15 boost in that budget. This is a tuning target, not ten hearts
per affix or a global health cap. The **provisional Tier 1 Health range is
+0.3 to +0.6 health points**, as a flat additive max-health bonus with fractional
points preserved. Legendary Tier 5 is +1.725 to +3.45 points per affix; six
maximum rolls yield **10.35 bonus hearts**. Later balance work must revisit
these starting values.

The **provisional Tier 1 Attack Damage range is +0.2 to +0.4 damage points**,
as a flat additive native attack-damage bonus. Legendary Tier 5 is +1.15 to
+2.30 per affix, giving **+13.8 Attack Damage** across six maximum equipped
rolls. Preserve fractional values and native attack-damage semantics,
principally melee. These are starting values for later balance work.

Armor should reach **80% vanilla armor reduction against a 30-heart hit
(60 damage points)** with a full set of maximum bonuses: six equipped
Legendary Tier 5 Armor affixes on ordinary Netherite (20 armor, 12 toughness).
That requires **32 total armor**, approximately **+12 across the loadout**,
including the Legendary boost. The **provisional Tier 1 Armor range is +0.15
to +0.35 armor points**, as a flat additive native bonus with fractional values
preserved. Keep linear tier scaling; Legendary Tier 5 is +0.8625 to +2.0125
per affix, giving **32.075 total armor** on ordinary Netherite with six maximum
rolls. These are starting values for later balance work. This is a native-mitigation tuning benchmark,
not a new armor cap or flat damage-reduction mechanic, and excludes Protection,
other defensive buffs, and armor-piercing/bypass effects.

Attack Speed should provide a **substantial increase over vanilla attack
recharge**. Use the **provisional Tier 1 range of +2% to +4%**, through native
`MULTIPLY_BASE` modifiers whose percentages add across equipment. Legendary
Tier 5 is +11.5% to +23% per affix. Six maximum rolls yield +138%, or 2.38
times normal weapon recharge speed before other percentage modifiers. Native
target hit immunity remains a separate limit on damage throughput. Revisit
balance in later gameplay tests; this decision does not change hit immunity.

The **provisional Tier 1 Dodge range is +1 to +2 percentage points**, as flat
additive chance. Six maximum Legendary Tier 5 rolls give 69%; ordinary Dodge
Aura brings that to 79%, or 84% with Mining Defensive Mastery. **Keep the
chosen 1-2 point range for the prototype**, with the aura bonuses and ordinary
85% cap unchanged. Do not increase the range to force cap coverage; overall
balance can be revisited later.

The **provisional Tier 1 Movement Speed range is +0.5% to +1%**, using native
`MULTIPLY_BASE` modifiers whose percentages add across gear. Legendary Tier 5
is +2.875% to +5.75% per affix, giving **+34.5% Movement Speed** across six
maximum rolls before other modifiers. Preserve native sprinting, status,
aura, and other modifier behavior; revisit these values in gameplay tuning.

The **provisional Tier 1 Resource Cost range is 0.5% to 1% reduction** in
skill resource costs. Negative flat `ADD` modifiers on the cost coefficient
make reductions add across gear. Legendary Tier 5 is 2.875% to 5.75% reduction
per affix; six maximum rolls give **34.5% lower costs**, or a 0.655 coefficient
before other modifiers. Preserve existing Focus/Rage/Mana skill-cost behavior;
these are starting values for later balance tuning.

The **provisional Tier 1 Cooldown range is 0.5% to 1% reduction** in Covenant
skill cooldowns. Negative flat `ADD` modifiers on the existing coefficient
make reductions add across gear. Legendary Tier 5 is 2.875% to 5.75% per affix;
six maximum rolls give **34.5% shorter cooldowns**, or a 0.655 coefficient
before other modifiers and existing upward tick rounding. Preserve existing
cooldown snapshots/lifecycle rules; revisit these values in gameplay tuning.

The **provisional Tier 1 Cast Time range is 1% to 2% reduction**. Negative flat
`ADD` modifiers on the existing coefficient make reductions add across gear.
Legendary Tier 5 is 5.75% to 11.5% per affix; six maximum rolls give **69%
shorter casts**, or a 0.31 coefficient before other modifiers and existing
upward tick rounding. Gear alone does not make positive base casts instant;
preserve existing instant skills/effects. Revisit these values in gameplay tuning.

The **provisional Tier 1 Skill Duration range is +0.5% to +1%**. Positive flat
`ADD` modifiers on the existing duration coefficient add across gear. Legendary
Tier 5 is +2.875% to +5.75% per affix; six maximum rolls give **+34.5% duration**,
or a 1.345 coefficient before other modifiers and existing timer rounding.
Preserve supported effects' duration snapshots and lifecycle rules; revisit
these values in gameplay tuning.

For the first pass, adding a **Common affix** consumes ordinary vanilla
ingredients directly, without requiring a salvaged affix-family component.
This makes Basic Affix Crafting usable at Level 1 before Salvaging unlocks
at Level 5. The **provisional prototype recipe is three lapis lazuli plus five
Potential for every affix family**; family-specific ingredients are deferred.
**Only Common affixes use this direct route.** Uncommon, Rare, Epic,
and Legendary additions require corresponding family components recovered
through salvaging. Each eligible non-Common affix receives an independent
recovery yield, and adding an affix consumes **3 matching components for
Uncommon, 3 for Rare, 4 for Epic, and 5 for Legendary** as provisional costs.
**Upgrade consumes the same 3/3/4/5 components**, matching the targeted affix's
family and rarity exactly, at every numerical tier, plus its provisional
five-Potential cost. It raises numerical tier by one without changing affix
or equipment rarity. Common equipment cannot exceed Tier 1; a Common affix on
higher-rarity gear can still be upgraded within the equipment and crafter's
tier limits. For the **prototype, Common Upgrade costs three lapis lazuli
plus five Potential**, without an affix-family component, at every valid target
tier. This provisional recipe matches Common Add.
**Reroll consumes 1/1/2/3 matching components** for
Uncommon/Rare/Epic/Legendary, plus its provisional five-Potential cost. Match
the targeted affix's family and rarity exactly, independently of numerical
tier. A worse or unchanged result still spends the full cost. Reroll preserves
family, rarity, and numerical tier. For the **prototype, Common Reroll costs
one lapis lazuli plus five Potential**, with no affix-family component; this
recipe is provisional and also spends its full cost for worse or unchanged results.
For the **prototype, Remove costs five Potential only**, with no ingredients
and no component refund, at every affix rarity and numerical tier. Permanently
delete the selected affix, preserving equipment rarity and other affixes,
without restoring Potential. Normal successful-operation XP still applies,
using the removed affix's rarity.
For the prototype, **non-Common Add, Upgrade, and Reroll require only their
matching components and five Potential**, with no additional ingredients.
Generic salvage materials mainly support ordinary crafting for now. Each component retains
the salvaged affix's family and rarity and must match both exactly when adding
an affix. Use the individual affix's rarity, not the equipment's initialized
rarity; different component rarities occupy separate inventory stacks. See
[[Crafting Profession]] and [[Salvaging]].

For the prototype, **100% of eligible equipment generated in mob drops and
chest loot is affixed**, supplying the initial non-Common components through
salvaging. **Increase equipment quantity through additional equipment rolls**,
with bonus equipment using the same 100% affix rate. Keep the affix rate and
extra-equipment rules configurable, including disabling the bonus, so later
tuning can bring availability back toward vanilla balance. The provisional
mob bonus is a **25% chance for one additional affixed equipment piece per
player-killed hostile mob**, rolled once per qualifying death alongside
ordinary loot. Vanilla loot chests receive **one guaranteed additional affixed
equipment piece when their loot is first generated**, not on later reopenings.
Both bonuses remain configurable and disableable. New affixed loot uses
**20% each for Common, Uncommon, Rare, Epic, and Legendary**, with configurable
weights shared by ordinary and bonus equipment for prototype testing. Each
generated piece starts with **exactly one random affix matching its rolled
rarity**, leaving its remaining slots for crafting. Roll its numerical tier
uniformly from 1 through the equipment rarity's cap, inclusive, independently
of the recipient's Crafting level. Both the affix-count rule and tier
distribution are provisional. Freshly generated loot starts with **its full
rarity-based Potential budget**, without Add or Upgrade charges for its natural
affix. Later Workbench operations use normal costs; subsequent drops or owner
changes do not reset remaining Potential. Bonus rolls use a configurable pool
of iron sword/axe/pickaxe/shovel/hoe, the four iron armor pieces, bow, crossbow,
and shield. This pool governs extra gear; existing eligible loot retains its
base item. Remaining source conditions and detailed pool weighting remain
future content. This loot support is implemented with provisional tuning.
See [[Loot and Rewards]].

Salvaging's configurable prototype base generic-material recovery chance is
**50% per salvaged item**, independent of the eligible affixes' component rolls.
Each eligible non-Common affix has a configurable **120% base component-recovery
rate**: one guaranteed matching component plus a 20% chance of a second,
independently per affix. Generic failure does not cancel that component yield.
Generic-only salvage can return nothing; the input item is consumed in all
completed cases. Recovery ranks I/II/III set the total generic-material chance
to **70% / 85% / 100%**. Rank III guarantees the defined partial refund;
generic material recycling remains lossy through quantities below crafting
costs. Generic outputs use **datapack-defined
partial refunds of base crafting materials**. The **prototype default is one
primary material per successful generic roll**, such as one iron ingot from
iron gear or one diamond from diamond gear, using the same quantity for weapons,
tools, and armor. Remaining material mappings still need authoring; output
items/counts stay data-driven for modpack tuning. **Durability does not affect
salvage returns**: damaged and pristine equipment use the same generic refund
quantities, recovery chances, and affix-component yields. **Enchantments are
destroyed**, with no enchanted-book output or vanilla XP refund. Crafting
profession XP is a separate reward, paid once per affixed equipment item actually
consumed to the player pressing Salvage, even if its recovery rolls yield
nothing. Queuing items, previewing outcomes, collecting results, rejected
operations, and items left unprocessed award no XP. Batching adds no multiplier;
per-item XP scales with the equipment's Covenant rarity, with higher built-in
rarities paying more. Equipment with **no Covenant affixes at consumption grants
zero Crafting XP**, including uninitialized gear and initialized gear with all
affixes removed; vanilla enchantments alone do not qualify. Normal eligible
salvage material returns remain available. For affixed gear, provisional salvage
rewards are **10 / 20 / 30 / 40 / 50 Crafting XP** for Common / Uncommon / Rare /
Epic / Legendary equipment respectively. Keep these amounts data-driven; they
are implemented starting values, provisional for playtesting. Further item-specific output
tables remain under design; check those tables
against crafting costs for material duplication. The multi-component crafting cost
and yield direction takes inspiration from Apotheosis; these numbers and the
family/rarity matching are Covenant's rules.
See [[Salvaging]].

**Refinement is excluded from the current prototype**, with no purchased
generic-material quality or quantity upgrades. Crafting's defined point budget
is now **9 points**, leaving **11** at Level 20: Basic Affix Crafting (1),
Salvaging (2), and three ranks each of Recovery and Focused Recovery (6).

The Salvaging GUI supports **up to ten queued equipment items** in a
**To Be Salvaged** section, one per input slot. Show possible outputs,
guaranteed quantities, and recovery chances before an explicit **Salvage**
button processes the batch. Placing/removing inputs does not perform salvage
or roll rewards. Each item's generic roll and each eligible affix's component
roll remain independent, with no batch yield bonus. Actual rewards appear in
a separate **Salvage Results** area with **twelve withdrawal-only output slots** for
collection. Queued inputs and unclaimed results stay in the physical Workbench
when its GUI closes, shared with other players using it. Native block-entity
storage preserves both inventories across chunk unloads and world saves/restarts;
breaking the workstation drops its remaining contents once. Only an explicit
Salvage action processes inputs, using the acting player's ranks and access.
Process inputs in slot order. Before rolling or consuming each item, ensure
its maximum possible rewards fit in the remaining output capacity, accounting
for valid stack merges. Otherwise stop, preserving completed rewards and all
unprocessed inputs. Collecting outputs does not resume processing automatically;
press Salvage again to continue once space is available.
See [[Salvaging]].

Focused Recovery passively boosts component yield for **all eligible
non-Common affix families**, using the salvaging player's purchased ranks.
There is no family selector or per-family specialization. Recovery improves
the separate generic-material chance; Focused Recovery affects components.
Each rank adds **80 percentage points** to the 120% base rate, for
**200% / 280% / 360%** total at ranks I/II/III. These yield two guaranteed;
two guaranteed plus an 80% chance of a third; and three guaranteed plus a 60%
chance of a fourth, respectively. Rank III averages 3.6 components per eligible
affix. The eventual high-end **salvaging** target is around four or more
components; this is not a crafting output bonus. With current three-component
Uncommon/Rare Add costs, salvaging those crafted affixes can return more
components than were consumed, while still destroying the equipment. Generic
refunds can offset part of the equipment's crafting cost; non-Common operations
have no extra ingredient sink in this prototype. This craft-and-salvage component-production loop is
an **accepted high-rank prototype profession benefit**. Crafted affixes use
the same yield rules as loot-generated affixes, with no cap based on prior
component expenditure. See [[Salvaging]].

### Profession respec

For the prototype, **one Profession Respec resets all four professions'
ability/specialization allocations together**, refunding each profession's
spent points while preserving every profession's XP/level. Combat progression
and allocations are unaffected. Craft **one Profession Respec** using an
ordinary **shapeless crafting-table recipe of four paper, four lapis lazuli,
and one emerald**, exceeding Combat Respec's cost.

For the prototype, crafting it has **no profession-level or purchased-ability
requirement** and grants no profession XP. Its material cost allows early
allocations to be corrected without leveling other professions.

Use the **same native held-item flow as Combat Respec**: hold right-click for
**two seconds (40 ticks)** out of combat, using the shared **seven-second
combat timer**. Require at least one profession with committed allocations.
Validate on the server at start, during use, and at completion; reset all four
professions atomically and consume **one item only on success**. Interrupted
or invalid uses consume nothing. No profession selector or Professions GUI
respec button is needed. This item/recipe remains planned, not implemented.

### XP and dimensions

- Separate Covenant character XP and profession XP tracks.
- The provisional shared profession curve costs **100 × the target level per
  transition**: 100 XP to reach Level 1, then 200 more to reach Level 2, through
  2,000 from Level 19 to 20. Each track independently needs **21,000 total XP**
  from Level 0 to 20. Keep these larger units for widely differing activity
  rewards; activity values and final pacing remain to be tuned.
- Contextual action-based profession rewards; ordinary vanilla crafting excluded from Crafting XP.
- Direct player activity is default; automation disabled unless specifically configured with attribution/anti-exploit safeguards.
- Multiplayer combat XP is split equally among eligible contributors, including support-only
  participants. One confirmed positive damaging hit qualifies without an amount
  or percentage threshold; misses and zero-damage attempts do not. Personal
  damage is not required for support qualification. Overlapping copies of the
  same qualifying passive aura credit all supplying players, while its bonus
  still applies once and each player counts once. Passive aura benefits supplied
  to fighting allies qualify, as do supplied shields absorbing enemy damage and
  successful enemy control/debuffs during combat. Presence alone is insufficient.
  Each player counts once, with no larger share for more damage or the final
  blow. A 30-second (600-server-tick) window per player per enemy refreshes on
  qualifying damage or support. Store contribution timestamps and check expiry
  at death/payout, without recurring expiry scans. Contribution lists are
  server-only runtime state, reset on enemy unload or server restart; awarded
  progression remains saved. Contributor death/respawn
  preserves recorded credit within the remaining window; Minecraft handles
  Hardcore without a separate Covenant system. Contributors must be online at
  payout; offline players are excluded before splitting, with no deferred award
  or payout on reconnect. No distance/radius check applies to recorded
  contributors at payout; action and effect ranges still apply normally.
  Dimension changes also preserve credit within the remaining window; apply
  the character XP cap of the dimension where the enemy dies, regardless of
  the recipient's location. Split before applying individual caps; capped
  contributors still count, and blocked portions are discarded without banking
  or redistribution. Detailed attribution,
  Round each ordinary share up to whole XP before applying caps: 10 XP split
  three ways grants 4 XP each. The small total rounding bonus is intentional;
  keep no fractional sharing balance. Remaining source-attribution details
  remain open; see
  [[Experience and Reward Systems]].
- Dimension gates use exact IDs or tags, configurable priority, and fallback (character 60, profession 20).
- XP beyond a dimension cap is discarded, not banked; existing levels never decrease.

### Balance/configuration

- Configurable XP curves/rewards, track caps, and dimensional rules.
- Character XP requirements, enemy rewards, and dimension-cap rules use server
  datapack JSON with built-in defaults and datapack overrides. Validate on load
  and support the server datapack reload workflow. Balance changes preserve
  earned levels, points, allocations, and raw XP; future awards use the new
  requirements, so percentages may change. A harder curve never subtracts
  earned progression. Exact schema and validation/failure handling remain open.
- Data-driven identifiers/tags for modded content and dimensions.
- All classes can access all professions.
- No primary attribute point system.
- Profession effects are class-agnostic and modest when they overlap with combat.
- Profession abilities do not consume combat keybinds.

### Character XP presentation (agreed, planned)

Squeeze a thin Covenant XP bar directly below vanilla XP in the existing bottom
HUD footprint. It has no text or labels, including no HUD character-level
number. Do not add a row above the existing HUD stack or move the hotbar.
Detailed level, current/required XP, percentage, and unspent combat points belong
on the existing Stats page, reading owner-synced authoritative progression.
See [[Character Progression]]. Pixel fit and native bar variants remain manual
client validation work, not verified rendering.

## Explicit boundaries

- Do not replace or gate vanilla Minecraft XP, enchanting levels, baseline crafting, mining, logging, farming, tools, blocks, resources, or ordinary recipes.
- Do not create a mandatory profession choice or profession slot system.
- Do not introduce a second character attribute-point economy.
- Do not hardcode a dimension compatibility list.
- Do not require every future profession ability, recipe, or custom workstation to exist in this slice.
- One shared Covenant Workbench implements affix crafting and salvaging. A future expanded workstation roster remains outside this slice.

## Acceptance criteria

1. Character XP advances character level independently of vanilla XP orbs and awards the correct skill-point schedule.
2. Character level gates the first skill point only; further ranks are available without additional character-level gates.
3. The server prevents allocation in more than eight distinct skills and enforces rank caps, prerequisites, and exclusions.
4. Combat respec refunds invested points without reducing character level/XP.
5. Each of the four professions independently gains XP, levels, and Profession Points from configured meaningful activities.
6. Profession rewards distinguish purchased abilities from automatic Crafting
   affix-tier access at Levels 5/10/15/20 after Basic Affix Crafting is purchased.
   Workbench and respec recipes remain ungated. Additional profession-level
   recipe milestones are deferred and are not slice acceptance requirements.
7. Each profession has enough playable initial content to demonstrate its interaction style and at least one meaningful investment decision.
8. One Profession Respec resets all four professions' allocations together,
   refunding each track's spent points while retaining its XP/level. It uses
   Combat Respec's two-second held-item flow and shared out-of-combat timer;
   interrupted/invalid uses consume nothing, and success consumes one item.
9. Dimension gates match exact identifiers and tags, resolve priorities deterministically, use fallback for unmatched dimensions, and never reduce existing levels.
10. XP past a dimension gate is not banked for later dimensions.
11. Modded tools/ores/dimensions and configured activity sources can participate without hardcoded mod support.
12. Vanilla XP and baseline vanilla gameplay remain unaffected.
13. Character and profession progression persist across logout/restart and remain server-authoritative in multiplayer.
14. Unfinished combat skills and skills blocked by unfinished prerequisites remain visible but reject point investment on the server; the slice does not require new combat skill implementations.
15. Rank 1 matches current prototype behavior and values; authored rank upgrades follow the modest, flavorful progression rules in [[Combat Skill Progression]], including review of cumulative power at maximum rank.
16. A player can progress all four professions and use earned profession benefits before choosing a combat class; later class selection preserves that profession state.
17. Before first class selection, combat grants no character XP and banks nothing for later. Selecting a class starts character progression at Level 1, zero character XP, and one skill point without changing profession progress.
18. A class-selected player can qualify for shared combat XP through support alone, including passive aura support tied to actual combat, without personally dealing damage or activating the aura. Mere presence grants no credit. Qualification follows [[Experience and Reward Systems]]; detailed attribution rules remain to be specified before implementation.
19. Divide the enemy's base XP reward equally among eligible contributors, counting each player once across damage and support, and round each share up before applying caps. A 10-XP reward split three ways grants 4 XP each; the total rounding bonus is intentional. Higher damage and the final blow grant no larger share.
20. Qualifying damage and support refresh only that player's 30-second (600-server-tick) contribution window for that enemy. At death/payout, contributions older than the window are excluded using timestamps, without recurring expiry scans or per-contributor tick timers.
21. Contributor death/respawn does not erase or duplicate recorded participation credit or reset/pause its window. XP awarded before or across respawn persists correctly. Hardcore behavior remains Minecraft's responsibility.
22. Contributors must be online at enemy death/payout. Offline contributors are excluded before equal shares are calculated, and reconnecting does not grant a missed payout. No offline award queue or deferred player-file writes are required.
23. Recorded contributors have no payout distance/radius restriction. Retreat, teleportation, and respawning far away preserve credit within the original window; skill and support-effect range rules are unchanged.
24. Recorded credit survives dimension changes and cross-dimension respawn within the original window. Combat awards use the character XP cap of the enemy's death dimension, not the recipient's destination dimension.
25. Otherwise-eligible contributors at dimension/global character caps still count in the equal split. Apply each player's cap afterward, accepting only what fits and discarding the rest without banking or redistribution.
26. One confirmed positive damaging hit qualifies the attributed player without a minimum amount, percentage, or hit count. Misses and zero-damage attempts do not grant damage credit; successful support can still qualify independently.
27. All owners supplying the same qualifying passive aura during combat can receive credit, even though the bonus applies once. Multiple auras and damage/support participation never give a player multiple shares.
28. Enemy contributor records are never saved or synced and reset on enemy unload/removal or server restart. A reloaded enemy requires fresh participation; already-awarded player progression remains persistent.
29. Default combat XP eligibility covers hostile mobs, with explicit entity-ID/tag inclusion for other combat mobs. Ordinary livestock and player kills give no character XP; ordinary hit eligibility and vanilla rewards remain unchanged.
30. Eligible spawner-produced mobs award normal configured XP under the same contribution and cap rules as equivalent naturally spawned mobs. Spawn origin alone neither excludes nor reduces rewards. Apotheosis-specific integration remains future work.
31. Each eligible unclaimed Wither contributor, including support-only participants, receives a one-time full level past dimension gates, respecting the global cap and class requirement. Preserve percentage XP progress across that level, rounding integer XP down as needed and retaining no post-cap excess. The milestone replaces that player's ordinary Wither XP share without redistributing it; repeat recipients receive ordinary shares under normal caps. Track each claim persistently and independently; the milestone is neither split nor reserved for the final blow.

## Dependency-ordered implementation plan

Stages 1–5 are implemented in the combat/shared foundation. See
[PROGRESSION.md](../docs/PROGRESSION.md) for the shipped scope and data format,
and [VALIDATION.md](../docs/VALIDATION.md) for evidence and manual boundaries.
The profession menu/allocation foundation, natural activity XP and
Workbench/itemization are also implemented. Remaining gathering abilities follow
the settled linked profession notes;
non-arrow ranks above 1 remain unavailable until their upgrades are designed.

1. **Definitions and saved foundation.** Load server datapack XP requirements,
   enemy rewards, and dimension rules; validate and prepare lookup data. Extend
   fresh-save player progression with points/ranks and the Wither claim.
   Initialize the four profession tracks independently of combat class selection.
2. **XP and level engine.** Centralize server awards, multi-level advancement,
   point grants, dimension/global caps, and no-banking behavior. Preserve earned
   state across balance changes. Keep vanilla XP independent.
3. **Combat acquisition.** Add eligible enemy rewards, event-driven damage and
   source-owned support credit, runtime contributor timestamps, equal-share
   payouts, online checks, and death/respawn/dimension handling. Add the atomic
   per-player first-Wither milestone using the same eligibility boundary.
   Broaden the existing seven-second combat timer to all classes and qualifying
   support here, before Apply and respec depend on it. Keep it distinct from
   the 30-second per-enemy XP contribution window.
4. **Combat skill investment.** Replace free allocation with point/rank spending,
   initial level gates, the eight-skill ceiling, prerequisites, and exclusions.
   Stage left/right-click rank edits in the UI and commit them through the
   bottom-right Apply button using one validated server request, requiring the
   shared seven-second combat state to have expired at commit time.
   Keep unfinished nodes unavailable. Resolve rank parameters through existing
   skill execution boundaries, with current values at Rank 1 and small authored
   upgrades. Retain planned cap 12 for Lunging Combo and Rip and Rush and cap 8 for other
   currently playable skills. Apply the specified additive +2%-of-base velocity
   per extra rank to Piercing Shot and Aimed Shot, retaining native damage
   behavior. Other playable skills allow only Rank 1 for now. Their individual rank-effect tables remain separate design work;
   do not describe their ranked gameplay as complete before those tables exist.
5. **Playable presentation and combat respec.** Extend owner-synced screens/HUD
   to show progression and point/rank availability while preserving the compact
   current-class tree. Add the crafted combat respec and its server-side refund,
   assignment, and active-skill cleanup. Use the agreed four-paper/one-lapis
   shapeless crafting-table recipe and two-second native held-item use, with
   no consumption on early release. Enforce out-of-combat use through the shared
   seven-second timer used by Rage grace. Fit the thin unlabeled Covenant XP
   bar below vanilla XP without growing the HUD upward; show detailed progress
   on the existing Stats page.
6. **Profession content integration.** Integrate the supplied profession ability,
   reward, recipe, and respec requirements when ready. Shared foundation work
   alone does not satisfy the full slice's playable profession-content criteria.

Validate each stage with focused checks, then run the required project build and
dedicated-server checks against the final implementation. Verify new-format
persistence, network validation, payout attribution, caps, Wither claims, and
allocation/refund accounting. Hand off client presentation and representative
leveling-time checks for manual playtesting under the development workflow.

## Deferred beyond the slice

Fully populated profession trees; additional profession-level recipe milestones;
final numerical XP/ability balance; all possible modded dimension/activity
integrations; complete future workstation roster; every future affix
manipulation/salvage recipe beyond the selected prototype operations; all future
profession branches; and complete endgame content.

Related: [[Progression Systems]], [[Project Covenant - GDD]], [[Character Progression]], [[Combat Skill Progression]], [[Experience and Reward Systems]], [[Profession Progression]], [[Dimensional Progression Gates]], [[Progression Balance Configuration]].
