# Mining

## Identity

Mining develops the player's mastery of prospecting, ore extraction, material processing, and resource transfiguration. It adds specialized options to the existing Minecraft mining loop; ordinary mining and access to vanilla resources remain available without Mining investment.

## Ability progression

### Prospecting

| Rank | Minimum Mining level | Point cost |
|---|---:|---:|
| I | 1 | 1 |
| II | 5 | 1 |
| III | 10 | 1 |
| IV | 15 | 1 |

Prospecting is a deliberate survey interaction:

- Right-click a suitable stone or deepslate block while holding a valid mining tool.
- The tool requirement is tag- or data-driven so modded mining tools can qualify.
- Search for the nearest eligible ore deposit within the rank's detection radius.
- Report the ore type, one of eight compass directions, and the deposit's Y coordinate.
- Do not reveal exact block coordinates or highlight ore through walls.
- Higher ranks increase detection radius and unlock higher ore tiers.
- Ore tiers are assigned through data-driven tags, allowing modded ores to participate without hardcoded lists.

Exact radii, tier definitions, and the rank at which each tier becomes detectable remain to be balanced.

### Vein Mining

| Rank | Minimum Mining level | Point cost |
|---|---:|---:|
| I | 2 | 1 |
| II | 5 | 1 |
| III | 10 | 1 |
| IV | 15 | 1 |

- Activates when the player mines an eligible ore without sneaking.
- Sneaking performs normal single-block mining.
- Only connected blocks of the same eligible ore are included.
- Each rank substantially increases the maximum number of connected ore blocks affected.
- Modded ores opt in through tags or equivalent data-driven classification.

Exact block limits, connectivity rules, tool wear, loot/enchantment interactions, and edge cases remain implementation and balance details.

### Auto-Smelt

- **Unlock:** Mining Level 5
- **Cost:** 2 Profession Points
- **Type:** Single-purchase specialization; further rank structure is not currently defined.

Auto-Smelt can process eligible drops from blocks mined with a valid pickaxe-classified tool. Eligibility is configurable by block/item identifiers and tags. Existing furnace cooking recipes determine the output, allowing modded recipes to participate.

- Shovel-mined drops are not eligible.
- Normal loot calculation occurs before processing so Fortune-related quantities are respected.
- Silk Touch suppresses automatic smelting.
- Server-side configuration determines which otherwise eligible pickaxe-mined drops are processed.

The exact default eligibility list and treatment of unusual recipe types remain open.

### Transfiguration

- **Unlock:** Mining Level 10
- **Cost:** 2 Profession Points
- **Type:** Specialization; exact rank structure is deferred.

Transfiguration gives an eligible mined resource a chance to be replaced by a configured alternative resource.

- Roll once per mined block, not once per resulting item.
- A successful result replaces the original eligible resource rather than creating a bonus drop.
- Fortune affects the original drop quantity but does not increase the chance to trigger Transfiguration.
- Each transformation defines its source, replacement, chance, and configurable conversion ratio.
- Use identifiers and tags so modded resources can participate.

Exact transformation tables, ratios, chances, and rank scaling remain to be tuned.

### Defensive Mastery

- **Unlock:** Mining Level 10
- **Cost:** 3 Profession Points
- **Type:** One specialization with class-dependent behavior.

The specialization enhances the appropriate existing defensive class skill; it does not create three separate profession nodes or a new keybind.

| Class | Enhanced skill |
|---|---|
| Reaver | Fortification Aura |
| Stalker | Dodge Aura |
| Weaver | Mage Armour |

Exact effect values and the interaction with skill ranks remain to be balanced.

## Point budget

| Ability | Defined investment |
|---|---:|
| Prospecting (4 ranks) | 4 |
| Vein Mining (4 ranks) | 4 |
| Auto-Smelt | 2 |
| Transfiguration | 2 |
| Defensive Mastery | 3 |
| **Total currently defined** | **15** |

Five Profession Points remain unallocated at the Level 20 cap.

## XP and compatibility

Mining XP should reward meaningful, player-performed mining and prospecting actions. Repetitive low-value block breaking and automated mining do not award XP by default. Modded tools and ores should participate through tags, configurable definitions, and optional integrations where necessary.

Ordinary Minecraft mining, tool use, and resource access are never gated by Mining level.

Related: [[Profession Progression]], [[Experience and Reward Systems]], [[Dimensional Progression Gates]], [[Logging]], [[Farming]], [[Crafting Profession]], [[Mod Compatibility]].
