# Mining

Natural ore-harvest XP and persistent chunk-owned placed-ore exclusions are
implemented, including native piston movement. Mining abilities below remain
planned. Runtime contracts: [Profession activities](../docs/PROFESSIONS.md#natural-activity-xp).

## Identity

Mining develops the player's mastery of prospecting, ore extraction, material processing, and resource transfiguration. It adds specialized options to the existing Minecraft mining loop; ordinary mining and access to vanilla resources remain available without Mining investment.

## Ability progression

### Prospecting

| Rank | Minimum Mining level | Point cost | Prototype detection radius | Newly detectable ores |
|---|---:|---:|---:|---|
| I | 1 | 1 | 8 blocks | Coal, copper, Nether quartz |
| II | 5 | 1 | 12 blocks | Iron |
| III | 10 | 1 | 16 blocks | Gold (including Nether gold), lapis, redstone |
| IV | 15 | 1 | 24 blocks | Diamond, emerald |
| V | 20 | 1 | 24 blocks | Ancient debris |

Radii are configurable and measured in all directions from the clicked block.
These defaults are accepted subject to finding and validating an inexpensive
search; reduce or revise them if profiling shows unacceptable cost.

Prospecting is a deliberate survey interaction:

- Right-click a suitable stone or deepslate block while holding a valid mining tool.
- The tool requirement is tag- or data-driven so modded mining tools can qualify.
- Prospecting has a **five-second (100-tick) per-player survey cooldown**, shared
  across its ranks.
- The Mining tab includes a Prospecting target selector, defaulting to **Any
  Ore**. Players can instead select one ore family unlocked by their committed
  Prospecting rank.
- Search for the nearest matching eligible ore deposit within the rank's
  detection radius. **Any Ore** considers all unlocked families; selecting
  Diamond, for example, ignores nearby coal and other non-matching ores.
- Report the ore type, one of eight compass directions, and the deposit's Y coordinate.
- For the prototype, send the result as a private chat message to the surveying
  player so the direction and Y coordinate can be reread while digging, for
  example: `Diamond detected northeast, Y -42.`
- Do not reveal exact block coordinates or highlight ore through walls.
- Ranks II-IV increase detection radius and unlock higher ore tiers. Rank V
  unlocks ancient debris while retaining the 24-block radius.
- Ore tiers are assigned through data-driven tags, allowing modded ores to participate without hardcoded lists.

Ore access is cumulative and includes stone/deepslate variants where applicable.
Rank V is the Level 20 capstone, costs one additional Profession Point, and
requires Rank IV under the shared sequential-rank purchase rule.

#### Search implementation approach to validate

Run only on a survey request, using already-loaded chunks without loading or
generating terrain. Check intersecting sections with native
`LevelChunkSection.maybeHas` to skip sections whose palettes rule out eligible
ore. This is a conservative filter, not an ore-location index; a positive result
still requires a block search, and the global palette cannot reject a section.

Spread remaining search work across ticks under a shared server work budget
for active surveys, bounding both per-request and aggregate work. Profile the
largest radius and concurrent requests before accepting performance. No search
is implemented or benchmarked yet. The checked 26.1.2.125 sources expose
`LevelChunkSection.maybeHas`, its palette implementation, and the loaded-chunk
lookup `ServerChunkCache.getChunkNow`.

### Vein Mining

| Rank | Minimum Mining level | Point cost | Maximum blocks per activation |
|---|---:|---:|---:|
| I | 2 | 1 | 8 |
| II | 5 | 1 | 16 |
| III | 10 | 1 | 32 |
| IV | 15 | 1 | 64 |

Block limits include the initially mined block. These are configurable prototype
defaults; a deposit larger than the limit requires another mining action.

- Activates when the player mines an eligible ore without sneaking.
- Sneaking performs normal single-block mining.
- The held tool must qualify as a mining tool and pass native harvest
  eligibility for each targeted block. Suitable modded mining tools qualify
  through the same checks. An unsuitable tool leaves additional ore untouched
  and preserves ordinary single-block behavior.
- Only connected blocks in the same configured ore family are included.
  Stone and deepslate variants of the same ore share a family: iron ore and
  deepslate iron ore can be mined together, while touching copper is excluded.
- Connectivity includes all 26 neighboring block positions: faces, edges, and
  corners. Follow connected ore up to the rank's block limit; veins touching
  only at an edge or corner can therefore be mined together.
- Each rank substantially increases the maximum number of connected ore blocks affected.
- Configurable ore-family tags let modded ore variants participate in the same
  grouping rules.
- Each block is harvested using normal tool durability and native loot and
  enchantment behavior, including Unbreaking, Fortune, and Silk Touch.
- Each harvested block applies normal mining exhaustion as well as normal
  durability use. Vein Mining has no cooldown, no class-resource cost, and no
  additional flat activation charge; it remains usable before class selection.
- Stop immediately when the tool breaks, leaving all remaining ore intact.
  Ore already harvested retains its normal drops and any eligible Mining XP.

Remaining edge cases are still being designed. The rules above are planned
behavior, not implemented gameplay.

### Auto-Smelt

- **Unlock:** Mining Level 5
- **Cost:** 2 Profession Points
- **Type:** Single-purchase specialization; further rank structure is not currently defined.

Auto-Smelt can process eligible drops from blocks mined with a valid pickaxe-classified tool. Eligibility is configurable by block/item identifiers and tags. Existing furnace cooking recipes determine the output, allowing modded recipes to participate.

- The Mining tab has an **Auto-Smelt on/off toggle**, enabled by default when
  the ability is purchased. Players can disable processing to retain raw
  materials without refunding the ability or using a respec.
- Once purchased and enabled, conversion consumes no fuel and has no additional
  activation cost. Ordinary mining durability and exhaustion still apply.
- Shovel-mined drops are not eligible.
- Normal loot calculation occurs before processing so Fortune-related quantities are respected.
- Silk Touch suppresses automatic smelting.
- Default eligible drops are **raw iron**, **raw copper**, **raw gold**, and
  **ancient debris**. Use their current furnace recipes to obtain the output
  (the corresponding ingots or netherite scrap).
- Stone and cobblestone drops remain unchanged by default.
- Server-side configuration determines which otherwise eligible pickaxe-mined drops are processed.

Treatment of unusual recipe types remains open. The list above is the agreed
planned default, not implemented drop processing.

### Transfiguration

- **Unlock:** Mining Level 10
- **Cost:** 2 Profession Points
- **Type:** Specialization; exact rank structure is deferred.

Transfiguration gives an eligible mined resource a chance to be replaced by a configured alternative resource.

- Once purchased, Transfiguration is always active for eligible mining drops.
  It has no player-facing toggle; its 4% chance is low enough to preserve
  ordinary copper and iron gathering without another setting.
- Roll once per mined block, not once per resulting item.
- A successful result replaces the original eligible resource rather than creating a bonus drop.
- Fortune affects the original drop quantity but does not increase the chance to trigger Transfiguration.
- Each transformation defines its source, replacement, chance, and configurable conversion ratio.
- Use identifiers and tags so modded resources can participate.

The planned default conversion table is:

| Original resource | Replacement resource | Chance per eligible block | Conversion ratio |
|---|---|---:|---:|
| Raw copper | Raw iron | 4% | 1:1 |
| Raw iron | Raw gold | 4% | 1:1 |

On a successful roll, replace the block's entire eligible drop quantity at the
configured ratio. The 1:1 defaults preserve Fortune's quantity benefit: six raw
copper becomes six raw iron, for example. Chances and ratios are configurable
prototype defaults. Rank scaling remains to be tuned.

#### Shared drop-processing order (planned)

Calculate normal loot first, including Fortune and Silk Touch. Then resolve
Transfiguration's single per-block roll against the original eligible resource.
Finally, apply Auto-Smelt to eligible resulting drops when enabled. Do not
chain another Transfiguration roll on a replacement resource.

For example, with both abilities enabled, a successful roll can convert six
raw copper to six raw iron, then Auto-Smelt processes those into six iron
ingots. Auto-Smelt's Silk Touch suppression remains in force.

### Defensive Mastery

- **Unlock:** Mining Level 10
- **Cost:** 3 Profession Points
- **Type:** One class-independent specialization that improves received
  defensive effects on its purchaser.

The bonus belongs to the **recipient**, not the caster. A player who purchases
Defensive Mastery gains more protection from eligible defensive effects applied
to themselves, whether supplied by their own skill or another player's aura.
The caster's Mining investment does not strengthen the effect for other
recipients; each player benefits according to their own purchased mastery.

| Defensive effect | Recipient-owned benefit |
|---|---|
| Fortitude Aura | Grants the purchaser an **eight-point shield** instead of six; passive armor and toughness retain their normal values |
| Dodge Aura | Grants the purchaser **+15 percentage points of Dodge** instead of +10 |
| Mage Armour | Adds **+2 Armor and +1 Armor Toughness** to the purchaser's own Mage Armour bonuses when that self-buff is available |

Normal skill requirements, aura coverage, and class-tree exclusions still apply
to supplying effects. A Reaver using Ripping Aura can benefit from an ally's
Dodge or Fortitude Aura through their own Defensive Mastery. Purchasing mastery
does not grant the underlying effects by itself.

These are specific profession bonuses, not a shared percentage multiplier.
Existing effect durations, Dodge caps, the active Dodge guarantee, and aura
overlap/replacement rules remain in force. Fortitude's eight points are its
total shield capacity on the purchaser, not eight additional points. Mage
Armour's underlying skill is still planned; its base values remain separate
design work. These profession effects are planned, not implemented gameplay.

## Point budget

| Ability | Defined investment |
|---|---:|
| Prospecting (5 ranks) | 5 |
| Vein Mining (4 ranks) | 4 |
| Auto-Smelt | 2 |
| Transfiguration | 2 |
| Defensive Mastery | 3 |
| **Total currently defined** | **16** |

Four Profession Points remain unallocated at the Level 20 cap.

## XP and compatibility

### Extraction rewards

- Successfully extracting an eligible ore block awards its configured Mining XP
  once. Each eligible block removed by player-triggered Vein Mining earns its
  own reward for that player.
- Player-placed ore blocks award no Mining XP, including ores obtained with
  Silk Touch and then placed again. The exclusion must survive save/load and
  server restarts and apply regardless of which player later mines the block.
- Silk Touch still earns Mining XP when initially extracting eligible ore that
  was not player-placed. Fortune changes drops without multiplying Mining XP.
- XP depends on the original ore block, not drop quantity or the output of
  Auto-Smelt or Transfiguration.
- Prospecting helps locate ore but grants no Mining XP itself.
- Ordinary low-value block breaking and automated mining award no Mining XP
  by default. Eligibility and reward values are configurable, with tags and
  identifiers allowing modded ores to participate.

The **provisional Mining rewards per eligible original ore block** are:

| Ore family | XP per eligible original block |
|---|---:|
| Coal | 10 |
| Copper | 10 |
| Nether quartz | 10 |
| Iron | 17 |
| Gold (including Nether gold) | 25 |
| Lapis | 25 |
| Redstone | 25 |
| Diamond | 50 |
| Emerald | 50 |
| Ancient debris | 200 |

Stone/deepslate variants share their family's reward where applicable; modded
variants can participate through the configured mappings. These rewards do not
require a purchased Mining ability or Prospecting rank. With the shared
100-XP requirement for Level 1, ten eligible blocks in the 10-XP band reach
Level 1. Iron uses 17 XP for both stone and deepslate variants. Gold's 25-XP
reward also applies to Nether gold; lapis and redstone use 25 XP for both
stone and deepslate variants. Diamond and emerald use 50 XP for both variants.

Ancient debris awards 200 XP per eligible original block. Its reward is not
gated by the Level 20 Prospecting detection capstone; normal tool/harvest and
placement eligibility still applies.

All initial ore families now have implemented ordinary-harvest sources with
provisional prototype rewards. Vein Mining remains planned. Remaining modded mapping
coverage and actual leveling-time balance still need validation and tuning.

Ordinary Minecraft mining, tool use, and resource access are never gated by Mining level.

Related: [[Profession Progression]], [[Experience and Reward Systems]], [[Dimensional Progression Gates]], [[Logging]], [[Farming]], [[Crafting Profession]], [[Mod Compatibility]].
