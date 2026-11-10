# Profession foundation

The first expanded progression chunk implements four independent, classless
tracks, paid allocations, a dedicated menu, respec, and Logging's Offensive
Mastery. Every tuning value below is provisional. Design intent remains in the
[vault](<../Project-Covenant/Profession Progression.md>).

## Current playable boundary

Hold G and choose radial slot 3, **Professions**. Mining, Logging, Farming and
Crafting each show their own level, XP, available points and authored nodes.
Left-click adds a pending rank; right-click undoes pending ranks. Each tab keeps
its draft. Apply submits only the displayed track, out of combat. Closing the
screen discards drafts. The world keeps running. Eight planned abilities stay
visible and unavailable; their descriptions describe future behavior.

Vein Mining, Tree Felling, Farming Scything and Farming Automatic Replanting
are purchasable alongside all four Crafting nodes
and `covenant:logging/offensive_mastery`. Its five provisional
ranks cost one point each at levels 5/8/11/14/17, granting a total outgoing
player-damage bonus of 2/4/6/8/10%. Native attacks, arrows and attributed skill
damage use the native incoming-damage event. Owned-creature damage is excluded;
future summons can opt their damage types into the native
`covenant:ignores_offensive_mastery` tag. No owner traversal credits pet damage.

Natural activity XP is implemented. The shared curve is provisionally
100 times the target level per transition, from 100 to 2,000 XP, totaling 21,000
for level 20. Enchanting rewards are 20/50/100 XP for the first/second/third
options, regardless of displayed level requirement. Affix-operation rewards
are implemented at 20/40/60/80/100 XP for
Common through Legendary affixes, shared by all four operations and all
numerical tiers. Salvage XP requires at least one Covenant affix at
consumption; unaffixed gear gives zero XP even if its rarity was initialized.
Implemented salvage rewards are 10/20/30/40/50 XP for affixed Common-through-Legendary
equipment. All XP amounts remain provisional. Additional profession-level
recipe milestones are deferred beyond the progression slice; Workbench and
respec recipes remain ungated.
Other gathering abilities remain deferred. The Workbench, rarity, loot, affix operations
and salvage are implemented; see [ITEMIZATION.md](ITEMIZATION.md) for the loop,
four Crafting nodes, data formats and validation boundaries.

## Vein Mining and Tree Felling

Both are class-independent contextual abilities: purchase ranks in Professions,
then break a qualifying block with a tool in the main hand. Sneaking preserves
ordinary single-block harvesting. Creative, spectators and FakePlayers cannot
activate batches. There is no cooldown, class-resource cost or flat activation
charge. Logging Automatic Replanting and the other planned gathering effects
remain unavailable.

| Ability | Rank level gates | Point cost per rank | Total blocks per activation |
|---|---|---|---|
| Vein Mining | 2 / 5 / 10 / 15 | 1 | 4 / 8 / 16 / 32 |
| Tree Felling | 1 / 5 / 10 / 15 | 1 | 4 / 8 / 16 / 32 |

Limits include the initially harvested block and snapshot at that harvest.
Vein Mining requires an item in `minecraft:pickaxes`; Tree Felling requires
`minecraft:axes`. Modded tools can join those native tags and must pass native
harvest checks. Veins use all 26 face/edge/corner neighbours and the same
configured activity ID as their family. Stone/deepslate variants and Nether
gold therefore join their configured families. The first matching ore mapping
wins; packs can map multiple entries to one activity ID to share a family.
Placed ore is harvestable but keeps its ordinary zero-Mining-XP exclusion.

Tree Felling requires the bounded recognition below and an initiating log at
the lowest remaining height in that recognized tree. It follows only certified
same-family logs upward or at the same height, in breadth-first order. Chopping
an upper log is ordinary single-block harvesting. A capped or interrupted tree
can continue in another batch from a lowest remaining certified log. Its evidence
is transient, invalidates edited logs and is discarded on chunk/catalogue reload
or restart. Floating leftovers may then require ordinary chopping. Tree-shaped
constructions attached to real trunks remain the documented heuristic limitation.
Leaves retain native decay; there is no extra leaf harvest or replant.

One synchronous server batch starts only after the original block is confirmed
removed and its native loot/tool handling completes. Extra blocks use native
player harvesting, including per-block durability, exhaustion, enchantments,
loot, vanilla XP, profession XP, Adventure restrictions and BreakBlockEvent.
The batch stops at the first protected, cancelled, changed, unsuitable or
otherwise failed target, or when the original held tool breaks/is replaced.
Completed harvests retain their drops and XP. Item drops from the complete
action gather at the first block's native drop
point, including loot added through the drop event. Cancelled/removed loot is
not recreated, and preexisting loose items are untouched. If the initiating
block has no surviving item drop, its center is the fallback. Vanilla XP keeps
its native per-block spawn path; leaves retain ordinary decay and drop positions.
Checks also enforce spawn protection and world borders for extra blocks.
Extra breaks never expand into recursive batches. Searches use loaded chunks only, with a hard cap of 64
members and at most 26 neighbour checks per member; tree recognition retains
its separate fixed bounds. No job, tick scan or chunk ticket is created.
These bounds are not a performance benchmark or a claim of protection-mod
compatibility testing.

## Scything and Farming Automatic Replanting

Right-click a supported mature crop with a main-hand item in `minecraft:hoes`.
Without Scything, harvest one crop. Purchased Scything ranks I/II/III cost one
point each at Farming levels 1/5/10 and harvest a 3×3/5×5/9×9 square centered on
the targeted crop, on that Y level only. Sneaking limits the action to one crop.
Mixed supported crops qualify; immature crops, other heights and unloaded chunks
are skipped. Ordinary tilling and left-click breaking retain their native paths;
left-click does not trigger these abilities. Creative, spectators and FakePlayers
cannot activate Covenant farming. The client uses owner-synced crop maturity
targets to consume the hoe interaction and prevent a second offhand use; it
predicts no terrain, durability or inventory changes.

The target is first, followed by the square in X/Z order. Each crop uses native
player removal, BreakBlockEvent, loot/enchantments, exhaustion and existing
Farming XP. Drops retain their native per-crop positions. Each confirmed harvest
makes exactly one native durability attempt, with Unbreaking; replanting costs no
extra durability. The first protected, cancelled, changed or failed harvest, or
broken/replaced hoe, stops the action. Completed work is retained. Right-click
cancellation/denial is checked after the final event subscriber. No chunk loading,
recurring scan or saved job is introduced; the maximum area is 81 positions.

Farming Automatic Replanting costs two points at level 5 and has no prerequisite.
After each hoe-use harvest, including a sneaking single crop or the last crop
before hoe breakage, it attempts native planting from the player's inventory
(including offhand). It consumes one matching planting item only on successful
placement. Harvest drops are never used as seed supply. Missing items, invalid
soil or cancelled placement leave the crop harvested without spending a seed.
Wheat, carrots, potatoes, beetroot and Nether wart ship with planting mappings.
Other crop shapes/behaviors and pack/protection-mod interoperability remain
outside this initial support.

## Natural activity XP

All four tracks can progress before class selection. Real, living Survival or
Adventure players qualify; Creative, spectators and NeoForge FakePlayers do
not. Awards use the existing server engine and source-dimension caps, grant
normal level/point entitlements, and bank no excess XP at a cap. Vanilla drops,
enchantments, tool costs and vanilla XP stay native.

- Mining pays once per original eligible ore block successfully removed through
  the native player harvest path, after harvest-tool validation. Coal, copper
  and quartz pay 10; iron 17; gold/lapis/redstone 25; diamond/emerald 50; ancient
  debris 200. Stone/deepslate variants share a family; Nether gold pays 25.
  Fortune and Silk Touch do not multiply XP. Incorrect tools and cancelled
  breaks pay nothing.
- Successful ore placements are recorded after the final NeoForge placement
  decision. The unsynced `covenant:placed_ores` chunk attachment saves absolute
  block positions. Mutable edits explicitly mark the chunk unsaved. Exclusions
  survive native chunk serialization and apply to every later miner. They
  follow native piston movement and survive its moving-block intermediate state.
  Removing ore or replacing it with a non-ore block clears its exclusion.
  Tracking covers the common
  `c:ores` tag and configured ore matches; adding previously unclassified modded
  ores cannot recover their earlier placement history. Commands/world editing
  that bypass placement hooks do not supply provenance.
- Logging pays 20 per successfully harvested log in a recognized tree, without
  needing Tree Felling or a particular tool. Wild and sapling-grown trees use
  the same current-state heuristic. Start at a supported log column on
  `covenant:harvest_tree_ground`; follow matching logs upward and sideways, and
  require matching non-persistent leaves beside a log at least two blocks above
  the base. Recognition requires at least three logs, with limits of 128 logs,
  32 blocks of height, a four-block horizontal radius and 4,096 loaded-block
  reads per request. Any exceeded bound, missing chunk, unsuitable ground or
  missing natural canopy rejects recognition. It never loads chunks.
  Validated remaining logs are remembered in the unsaved, unsynced chunk
  attachment `covenant:harvest_trees`, allowing sequential single-log chopping
  after the base is gone. Editing a remembered position revokes its evidence;
  chunk unload/reload or catalogue reload discards it. Partly chopped trees
  may therefore fail a new recognition after reload. No log-placement history
  is saved. Tree-shaped constructions and branches joined to real trees remain
  the accepted heuristic limitation; large, leafless or unusual trees may give
  ordinary drops without Logging XP. No tree-search performance benchmark is
  claimed by the fixed bounds.
- Farming pays five per mature wheat, carrot, potato, beetroot or Nether wart
  block successfully harvested by the player. Immature crops, planting, Bone
  Meal, passive collection and unattributed destruction pay nothing. Both native
  left-click breaking and successful hoe right-click harvests use this reward.
- Successful breeding pays 100 Farming XP once to vanilla's credited breeder,
  through the native successful-breeding criterion. It includes special native
  breeding paths that use that criterion. Feeding/love preparation alone and
  final cancelled breeding do not pay; two feeders do not receive two awards.
- Successful enchanting-table operations pay 20/50/100 Crafting XP by option,
  at native completion after lapis and vanilla levels are spent. Displayed
  requirement and enchantment count do not scale the reward. Invalid,
  unaffordable or empty-result operations pay nothing. Anvil, grindstone and
  ordinary crafting interactions do not award Crafting XP.

There is no tick scan, offline queue, recurring tree job or client award request.
The narrow hooks observe native confirmed harvest/placement and criterion
boundaries because earlier cancellable events or request return values do not
prove completion. Optional automation integrations remain outside this chunk.

## Authoritative purchases and persistence

The existing `covenant:profession_progression` attachment ID and four track IDs
are unchanged. Each track saves `level`, `experience`, and an optional
`allocations` map keyed by ability ID. Each allocation contains `rank` and
`paid_points`. Older foundation tracks decode with an empty map. Persistent
snapshots remain immutable, owner-synced and copied on death/End return.

Available points equal level minus the sum of saved paid points. Never recalculate
past spending from current definitions. Cost changes affect newly purchased ranks.
Unknown or removed abilities retain their reservation until respec. Saved ranks
above a shortened definition use the last remaining rank's effect; removing a
definition disables its effect without erasing the purchase.

Apply includes the catalogue revision, expected paid allocations and desired
ranks. The server validates the entire request before one attachment replacement:
combat/alive/spectator state, stale revisions or allocations, rank sequence,
profession, implementation availability, prerequisites, gates and affordability.
Committed ranks cannot be refunded through Apply. Malformed/oversized packets
are bounded before handling and share the skill system's ingress budget.
The separate `covenant:profession_view` owner attachment is transient presentation;
it contains the server catalogue, current XP/cap values and supported mature-crop
targets for client interaction prediction, never authoritative purchases. Successful resource reloads invalidate stale purchase previews; failed
reloads retain the previous resource snapshot and player state.

## Server datapack content

Override `data/covenant/progression/professions.json`, then `/reload`. This is one
replacement catalogue, not a merging registry. Ability IDs are durable saved keys.

```json
{
  "abilities": [{
    "id": "covenant:logging/offensive_mastery",
    "profession": "covenant:logging",
    "order": 1,
    "ranks": [{"level": 5, "cost": 1, "bonus": 0.02}]
  }],
  "activity_rewards": {"covenant:logging/log": 20}
}
```

`profession` must name one of the four built-ins. Ability paths begin with that
profession's path plus `/`. `order` controls presentation (0–127); optional
`prerequisite` names another node in the same profession. Cycles, missing parents
and duplicate IDs fail reload. Up to 32 nodes per track and 64 ranks per node are
supported. Rank `level` and `cost` are positive whole numbers up to 1,000; gates
are nondecreasing. Optional `bonus` defaults to zero and must be finite within
0–1,024; its meaning belongs to the implemented ability (Offensive Mastery uses
a fractional total damage bonus). Integer fields reject fractional JSON values.
Datapacks cannot enable an ability whose runtime behavior is unimplemented.

Vein Mining and Tree Felling ranks require a whole-number `blocks` value in
1-64, including the initiating block. Zero, omitted, fractional and oversized
gathering limits reject the complete reload. Other abilities default this field
to zero. Existing replacement profession catalogues must add `blocks` to these
two nodes when adopting this version. No saved attachment or ability ID changes;
owned ranks/paid costs remain intact, and future activations use the current
rank's tuning. Removing a node disables its effect while retaining its paid
reservation until respec. If ranks are shortened, the last remaining rank's
limit applies, as for Offensive Mastery. Limits appear in owner-synced tooltips.

Scything ranks require an `area` field: an odd whole-number square width in
1-9, defaulting to 3/5/9 in the shipped ranks. Missing, zero, even, fractional or
oversized widths reject reload. Replacement catalogues must add these widths
when adopting this version. Owner-synced tooltips show each rank's actual area.
Required profession networking advances to `professions/4` for the area/crop
presentation contract. Connecting clients must use the same build.
Saved allocations and ability IDs are unchanged; current catalogue tuning and
node removal apply to future actions.

`activity_rewards` holds up to 4,096 nonnegative whole-long values, keyed by
namespaced activity ID. Native hooks consume the shipped Mining family IDs,
`covenant:logging/log`, `covenant:farming/mature_crop`,
`covenant:farming/breeding` and `covenant:crafting/enchant_option_1|2|3`.
Missing non-ore activity rewards disable that payout. Ore definitions must
reference an existing activity reward. Zero disables a reward without disabling
ore placement tracking. Extra activity IDs do not create new automatic hooks.

The optional `harvesting` object in the same catalogue defines:

```json
"harvesting": {
  "ores": [{"blocks": "#c:ores/iron", "activity": "covenant:mining/iron"}],
  "crops": [{"block": "minecraft:wheat", "age_property": "age", "mature_age": 7, "planting_item": "minecraft:wheat_seeds"}],
  "trees": [{"logs": "#minecraft:oak_logs", "leaves": "minecraft:oak_leaves"}],
  "tree_ground": "#covenant:harvest_tree_ground"
}
```

Ore/tree/ground matches accept a block ID or `#block-tag` ID. The first matching
ore/tree definition wins. Limits are 256 ore definitions, 256 unique crop
blocks and 128 tree definitions. Crops use an existing integer property and a
valid whole-number maturity value; unknown blocks, duplicate crops, invalid
properties/ages, oversized arrays and unknown ore reward references fail the
complete catalogue reload. Optional `planting_item` must identify a registered
BlockItem that places the configured crop. Invalid/mismatched items fail reload.
Definitions without it retain activity XP but do not join hoe-use harvesting.
Native placement supplies survival/placement checks for compatible modded crops;
no special handling for multi-block or nonstandard crops is implied.
Omitted `harvesting` means no block activity
matches; enchanting and breeding use their reward keys independently. Tag
matching uses current native tags. Reload replaces the catalogue atomically
and changes future awards without rewriting earned progression.
Shared requirements and dimension/global caps remain in
`data/covenant/progression/balance.json`; see [PROGRESSION.md](PROGRESSION.md).
Neither resource is a final balance declaration.

## Respec and testing commands

Craft `covenant:profession_respec` with four paper, four lapis lazuli and one
emerald, shapeless. No class or profession level is required. Hold right-click
for 40 server ticks out of combat. Early release or combat interrupts without
consumption. A successful finish consumes one item and refunds all four tracks
atomically, preserving their XP/levels and combat skill state. Empty allocations
cannot consume another item. The book icon is a prototype placeholder.

`/covenant profession` inspects your four tracks. Operators can use:

- `/covenant profession xp covenant:logging 1500` for ordinary capped XP awards.
- `/covenant profession level covenant:logging 17` to set a test level and clear
  that track's raw XP. It preserves allocations and refuses levels below paid
  spending or above the configured maximum.

## Manual client checks

Automated tests cover server state, native damage/use lifecycles, reloads,
persistence, malformed intent and pure layout geometry. Rendered UI and input
feel still need user playtesting:

1. Before selecting a class, open slot 3; inspect all four tabs and planned nodes.
2. Set Logging to 17, draft ranks, switch tabs, undo pending ranks, resize the
   window, then Apply. Verify points and pending state; close/reopen to discard
   an uncommitted draft.
3. Enter combat and try Apply/respec. After combat expires, hold the respec item
   for two seconds; check one-item consumption and preserved levels/XP.
4. Reconnect/respawn, inspect committed purchases, and compare damage before and
   after Offensive Mastery. Check long tooltips and controls at your GUI scale.
5. On a classless Survival character, check natural ore, a whole ordinary tree,
   mature versus immature crops, successful breeding and all three enchanting
   options against the rewards above. Observe leveling time; values are provisional.
6. Place Silk-Touched ore, save/reopen, and mine it as another player. Move placed
   ore with a piston before mining it. Both should yield normal drops and zero
   Mining XP. Check nonstandard/large trees for the documented conservative fallback.
7. Purchase Vein Mining/Tree Felling, compare normal and sneaking harvests, and
   check drops gathering at the first block, tool damage and partial-tree
   continuation at each rank.
   Break a nearly exhausted tool; verify remaining blocks stay intact. Reconnect
   or reload with a partial tree and check the conservative evidence fallback.
   Check rendered rank limits and tooltips. Protection/modpack interoperability,
   simultaneous real clients and unusual tree coverage remain manual checks.
