# Affixes and Rarity

## Covenant equipment rarity (implemented prototype)

Introduce a **Covenant-owned equipment rarity system**, authored through server
datapacks. Modpack developers and other mods can opt equipment into the system
through data-driven rules using item identifiers and tags, without requiring
Covenant to enumerate every modded material in code.

Rarity definitions and equipment participation rules are separate authoring
concerns. Use stable namespaced identifiers so packs can extend and override
the supplied content. This itemization is implemented; see
[Itemization](../docs/ITEMIZATION.md). The per-affix rarity ID remains a separate concept.

### Built-in rarity ladder

The built-in order is **Common -> Uncommon -> Rare -> Epic -> Legendary**.
These are the five starting rarity definitions, not a closed code enum;
datapacks can extend or replace the definitions for a modpack's needs.

An initialized item's rarity is a **ceiling on the rarity of subsequent
affixes**. Rare equipment can accept Common, Uncommon, or Rare affixes, but not
Epic or Legendary. Normal affix crafting must respect this ceiling while
preserving the item's initialized rarity.

Default prefix/suffix capacities are **1+1 for Common/Uncommon**, **2+2 for
Rare/Epic**, and **3+3 for Legendary**, configurable through rarity definitions.
See [[Affix System and Equipment Customization]] for the capacity table.

Numerical affix-tier ceilings are **1/2/3/4/5** for
**Common/Uncommon/Rare/Epic/Legendary**, respectively, configurable through the
rarity definitions. Crafting also requires the crafter to have unlocked the
target tier; the effective ceiling is the lower of those two limits.

### Affix roll quality

Affix rarity also controls the implemented roll-quality model: **1/2/3/4/5
independent samples, keeping the strongest**, for Common through Legendary,
from a shared base range for each family/numerical tier. Epic then gains
**5%** and Legendary **15%** additional bonus magnitude, using multipliers
1.05 and 1.15 respectively; these boosts do not compound with each other or
scale the whole effective stat. Use the individual affix's rarity, not the
equipment's rarity. These boosts are provisional; later loot rarity should
reflect their added power, while equal prototype loot weights remain unchanged.
See [[Affix System and Equipment Customization]] for the full prototype contract.

### Item ownership

Equipment rarity belongs to the **individual item instance** and is saved with
its stack. Two copies of the same base item, such as two iron swords, can have
different Covenant rarities. The assigned rarity travels with the equipment
through inventory/container movement, drops, trading, player transfer, and
save/reload; changing owner does not assign a fresh rarity. Datapack
participation rules do not make rarity a single shared property of an item type.

### Crafted equipment initialization

Ordinary crafted equipment starts with **uninitialized Covenant rarity**.
The first successful affix application gives the resulting affixed item its
Covenant rarity by **inheriting the starting affix's rarity**: a Common starting
affix produces a Common item, and a Legendary starting affix produces a
Legendary item. Uninitialized is distinct from assigning the lowest rarity at
ordinary crafting time.

Once initialized, the item **retains that rarity through subsequent Add,
Upgrade, Reroll, and Remove operations**, including removal of the starting
affix or every affix. Removing affixes does not return the item to the
uninitialized state. Choosing a Common starting affix therefore commits that
item to Common rarity under these crafting rules. These are implemented prototype rules.

### Initial Crafting Potential

The first successful **Workbench Add to uninitialized crafted gear** initializes
Crafting Potential once from the starting affix's rarity definition, then deducts
that Add operation's Potential cost from the new budget. Commit the assigned
rarity, applied affix, and remaining Potential together. An invalid or
unaffordable first application does not initialize or spend anything.

**Freshly generated affixed loot starts with its full rarity-based Potential
budget.** Its natural starting affix does not incur an Add charge or tier-upgrade
charges. This is a one-time loot initialization, not a refill when equipment
changes hands or appears in a later loot/drop event.

Removing affixes, including every affix, never refills or reinitializes
Crafting Potential.

Provisional, configurable starting budgets are:

| Equipment rarity | Starting Crafting Potential |
|---|---:|
| Common | 20 |
| Uncommon | 30 |
| Rare | 40 |
| Epic | 50 |
| Legendary | 60 |

These are first-pass tuning values to revisit alongside operation costs, not
final balance. Add, Upgrade, Reroll, and Remove each cost **five Potential**
for now, including the first Workbench Add deducted from a new crafted item's
budget. Naturally generated starting affixes use the full-budget loot rule above.

Equipment rarity supplies prefix/suffix capacity, numerical affix-tier limits,
and starting Crafting Potential under [[Affix System and Equipment Customization]].
The initial ten families' provisional stat ranges and operation ingredient
costs are specified there. Final balance, remaining component content, and
loot-source edge cases remain under design.

## Further itemization

Future capabilities include exceptional/unique properties and
conditional/build-defining modifiers.

Affixes should create meaningful choices, not only linear stat inflation. Define pools, weighting, tier scaling, rerolls, locking, extraction, and unique behavior.

Related: [[Affix Pages]], [[Crafting]], [[Loot and Rewards]], [[Statistics]].
