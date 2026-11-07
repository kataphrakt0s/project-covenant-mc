# Itemization and Covenant Workbench

The first complete find/salvage/customize prototype uses existing equipment,
ten affix families, five built-in rarities and the native attribute bridge.
All tuning is provisional. Gathering abilities and accessory-slot integrations
remain deferred. It adds no class requirement or equipment level restriction.

## Playing the loop

Craft one Workbench in an ordinary crafting table from one crafting table,
four planks (any/mixed types), two iron ingots and two lapis. This ungated recipe
grants no profession XP. Its model references Minecraft's netherite block as
placeholder art. Start Crafting XP through successful vanilla enchanting.

Purchase Basic Affix Crafting for one point at level 1. Numerical tiers 2/3/4/5
unlock automatically at levels 5/10/15/20. Rarity and numerical tier are distinct:
a traded Legendary component can fund a Tier 1 starting affix at low Crafting
level. Salvaging costs two points at level 5. Recovery and Focused Recovery each
have three one-point ranks at levels 5/10/15, requiring Salvaging. All four
Crafting nodes now accept real purchases; other unfinished nodes stay unavailable.

Right-click the station. Affix Crafting has one equipment slot and cycling
operation/family/rarity/target selectors. Add uses family and rarity; other
operations use an existing target. Hover Craft for full server-authored costs
and status. Only the explicit action spends ingredients from the acting
player's inventory, including offhand, and the item's Potential.

Salvaging has ten single-item inputs and twelve withdrawal-only result slots.
Hover Salvage for possible outputs, maximum quantities, guaranteed component
yield and extra-component chance. Process occupied inputs in slot order, stopping
before an item's maximum possible rewards cannot fit. Never roll a blocked
input or spill rewards; collecting results does not resume the batch. Consumed
equipment and enchantments are destroyed without books or vanilla XP refunds.
Durability does not affect returns.

All 23 slots are shared block-entity storage, retained on GUI close and native
save/load. Vanilla block-entity removal drops contents once. No station ticker
runs. Hoppers can move native contents but cannot execute operations or earn XP;
FakePlayers cannot execute Workbench operations.

## Ownership and operations

`covenant:equipment_quality` is an immutable persistent/synced item component
with equipment rarity ID and remaining Potential. Absence means uninitialized.
The existing `covenant:affix_page` keeps its UUID, category, base modifiers,
ordered rolled affixes and old progression metadata. No item bonuses are written
to a character profile. Native item/container transfer and serialization carry
both components; equipped pages feed the existing native attributes.

First Workbench Add initializes rarity/Potential from the starting affix, spends
that Add's cost and attaches a page together. Later operations preserve equipment
rarity and page identity, even with no affixes left. Removal never refills
Potential. Existing operator-created pages without quality remain usable debug
equipment but reject crafting until quality is initialized through debug setup.
Operator `clear` empties bonuses while preserving page identity and quality;
`reset` removes both components, retaining the existing max-stack override.

One affix per family is allowed per item. Prefix/suffix capacities are 1+1,
1+1, 2+2, 2+2, 3+3 and equipment numerical ceilings are 1/2/3/4/5. An affix's
rarity cannot exceed initialized equipment rarity. Add creates Tier 1; Upgrade
advances one numerical tier and rolls afresh. Reroll spends costs even for
unchanged/worse values. Upgrade/Reroll require the target numerical tier to be
unlocked; Remove can delete a found affix above the crafter's tier. Upgrade and
Reroll preserve affix rarity.

All operations cost five Potential. Common Add/Upgrade costs three lapis;
Common Reroll one lapis. Non-Common Add/Upgrade costs 3/3/4/5 matching components,
and Reroll 1/1/2/3, for Uncommon through Legendary. Remove has no ingredient
cost or refund. Success pays 20/40/60/80/100 Crafting XP by affected affix rarity,
independently of tier or equipment rarity. Invalid operations spend nothing.
Normal profession dimension/global XP caps apply.

`covenant:affix_component` is a stackable item whose immutable
`covenant:affix_material` component holds family and rarity IDs. Both must match
to pay a cost; different identities do not merge. Common affixes yield no
components. Generic salvage starts at 50%, with Recovery totals 70/85/100%.
Only iron/diamond equipment have default generic mappings, returning one
ingot/diamond on success; other eligible gear can still yield components.
Each non-Common affix starts at expected yield 1.2: one guaranteed plus 20%
chance of another. Focused Recovery adds 0.8/1.6/2.4 for totals 2/2.8/3.6.
Salvage pays 10/20/30/40/50 XP by equipment rarity only if an affix remains at
consumption, including zero-output salvage. Empty-affix gear pays zero.

## Fresh loot

`covenant:affixed_equipment` is a native NeoForge global loot modifier. In
26.1.2 its JSON loads directly from `data/<namespace>/loot_modifiers/`; the older
global-modifier-list manifest is not used. Packs can override conditions/priority.

Eligible fresh equipment from entity death tables and vanilla
`minecraft:chests/` tables has a 100% default affix chance, one random eligible
family matching rolled rarity, equal rarity weights, and uniform numerical tier
from 1 through the rarity ceiling. Loot starts with full 20/30/40/50/60 Potential
without Add/Upgrade charges. Existing quality or a page prevents reinitialization.
Spawn-equipped and picked-up inventory is outside fresh table generation;
dropping previously owned equipment never initializes or refills it.

Player-credited hostile deaths roll once at 25% for one shared bonus item.
Native `LAST_DAMAGE_PLAYER` credit qualifies independently of class or Covenant
XP, including environmental deaths while credit persists and spawner mobs.
Vanilla chest loot generation adds one bonus once; native unpacking prevents
reopening rewards. The twelve bonus types have equal weights: iron sword/axe/
pickaxe/shovel/hoe, four iron armor pieces, bow, crossbow and shield. Existing
loot keeps its base item. All chances and bonuses are configurable/disableable.

## Datapack format and reload

Override `data/covenant/itemization/definitions.json` with a complete bounded
catalogue. Shipped JSON documents prototype values; units and fields are:

- `rarities`: stable `id`, unique ascending `order`, prefix/suffix capacities,
  numerical `tier` ceiling, initial `potential`, roll `samples`, bonus `magnitude`
  multiplier, component `add_cost`/`reroll_cost`, `operation_xp`/`salvage_xp` and
  nonnegative loot `weight`. `covenant:common` retains the lapis route.
- `families`: stable `id`, `prefix`, registered attribute `stat`, modifier
  `operation` (`add`, `multiply_base`, `multiply_total`), `reduction`, and one
  positive-magnitude `min`/`max` range per numerical tier. Best-of-1/2/3/4/5
  samples use the affix's rarity; Epic/Legendary multiply the bonus by 1.05/1.15.
  Reductions negate the bonus, never scale the whole effective stat.
- Family `items`, `tags`, and `categories` form an optional union allowlist;
  absent/empty lists allow all participating gear.
- Ordered `participation` maps item/tag selectors to equipment categories;
  first match wins. Defaults use existing `affix_compatible/*` tags. Explicit
  item rules can opt gear in without a category tag. Affixed stacks become
  singleton; custom categories still require equipment-slot integration.
- Ordered `refunds` uses item/tag selectors, output ID and count; first match
  wins. No match means no generic refund. Counts ignore durability/tier/rarity.
  The prototype authors only iron and diamond mappings.
- `bonus_pool` lists equally weighted item IDs. `settings` supplies Potential/
  lapis costs, probabilities in [0,1], expected component yields (not capped
  probabilities), Recovery totals, Focused Recovery additive yields, numerical
  tier level gates, mob bonus probability and chest bonus count (zero disables).

Validation rejects invalid bounds, identifiers, finite values, ranges, registry
references, costs, missing tier coverage and zero total loot weight. `/reload`
assigns a new catalogue revision. Existing rolls, rarity, remaining Potential,
XP and paid allocations remain snapshots. Future operations use new costs;
open menus refresh and reject old previews. Removed definitions remain saved,
while affected operations fail safely. Invalid itemization reloads retain the
previous live catalogue.

## Debug commands

The replacement for the old raw-stat Affix Page commands focuses on
preparing test equipment and supplying affix-family/rarity crafting components.
Item tooltips provide inspection; the replacement does not need an inspect
command. The old `apply` and `inspect` commands are retired.

Equipment setup commands bypass ingredient and Potential costs and profession
requirements, including profession numerical-tier unlocks. They still enforce
valid equipment limits: participation and family compatibility, one affix per
family, prefix/suffix capacity, and equipment rarity and numerical-tier ceilings.

Affix value selection supports `random`, `min`, and `max` within the selected
family, affix rarity, and numerical tier. Random uses the normal rarity sampling
and magnitude rules; min/max select the weakest/strongest permitted bonus,
including the rarity magnitude multiplier. For reduction families, max means
the strongest reduction rather than the greatest signed modifier amount.

Changing initialized equipment rarity preserves existing affixes, their rolled
values, and remaining Potential. Reject the change without mutation if any affix
rarity, numerical tier, prefix/suffix occupancy, or remaining Potential exceeds
the requested rarity's limits. Rarity changes never silently remove or weaken
affixes or refill Potential; separate edits can prepare an item for a downgrade.

`/covenant affix set <family> <affix_rarity> <tier> <random|min|max>` adds the
family if absent, or replaces that family's existing affix rarity, tier, and
rolled value if present. Preserve other affixes and remaining Potential; reject
invalid results without mutation. Separate debug Add/Upgrade/Reroll commands are
unnecessary for this workflow.

On previously uninitialized equipment, a successful `set` initializes equipment
rarity to the requested affix rarity and grants that rarity's full starting
Potential, with no crafting cost. Initialization and the affix commit together
only after validation succeeds. Already-initialized equipment retains its rarity
and remaining Potential; an affix above its equipment limits requires an explicit
equipment rarity edit first.

`remove <family>` deletes one affix; `clear` deletes all affixes. Both preserve
equipment rarity and remaining Potential, without costs or refunds. `reset`
removes all Covenant affix and equipment-quality data so the item becomes
uninitialized again. Preserve enchantments, durability, and other item data.
The existing singleton max-stack override remains: no prior override was saved,
so reset cannot safely reconstruct it.

`/covenant affix component <family> <rarity> [count]` supplies crafting
components, defaulting to one. Accept only defined non-Common rarities
(Uncommon through Legendary in the shipped catalogue); Common uses lapis and
has no useful crafting component. Provide family and rarity tab completion.

### Equipment rarity and Potential

Additional commands under `/covenant affix` are:

| Command under `/covenant affix` | Behavior |
|---|---|
| `rarity <rarity>` | Initialize empty equipment at full Potential, or change initialized rarity under the preservation/rejection rules above. |
| `potential <amount>` | Set remaining Potential from zero through the current equipment rarity's starting budget. Requires initialized quality. |
| `potential full` | Restore the current equipment rarity's starting Potential budget. Requires initialized quality. |

Commands are operator-only and equipment edits target the executor's single
main-hand item. Setup commands grant no profession XP. Identifiers, limits,
completion, and roll bounds come from the current server itemization catalogue.
Edits preserve page identity; rejected operations leave all components unchanged.
Remove completion lists held families, including definitions removed by reload.
Clear also removes raw base modifiers left by legacy debug pages. Cleanup remains
available for items with missing definitions; set/rarity require the resulting
affix structure to satisfy current definitions, preserving untouched saved rolls.

Component counts are 1–64, default one. Native inventory insertion merges matching
family/rarity identities and drops overflow beside the player, including with a
full Creative inventory. It needs no held equipment. Use ordinary `/give` for
Common lapis and the Workbench. No saved component format or registry ID changes.

## Authority and validation

`WorkbenchOperations` owns server previews and transactions; `WorkbenchMenu`
owns native slot/button interactions. It validates the current menu, living
actor, distance, station lifetime, catalogue revision, shared storage revision,
inventory and profession state. Each action carries a preview token; changes
and accepted actions invalidate it. Stale/repeated clicks cannot spend against
different state. Hidden slots and result deposits reject native slot actions.
Clients never supply trusted costs, ranks, rolls or rewards.

GameTests cover operation costs/limits/XP, native equipped bonuses, material
identity, maximum-output batching, stale shared menus/native packet dispatch,
item/block-entity save/load/removal, registered loot/chest unpacking and live
valid/invalid reloads. Rendered UI, two real clients, retained-world restart and
pack compatibility remain manual checks; see [VALIDATION.md](VALIDATION.md).
