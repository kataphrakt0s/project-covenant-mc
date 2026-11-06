# Affix System and Equipment Customization

## Purpose

Covenant's affix system adds RPG properties to existing vanilla and modded equipment without replacing the equipment's identity. Affixes are properties stored directly on an individual equipment item, not standalone inventory items.

The former **Affix Pages** concept is retired. There are no separate Affix Page items to collect, trade, store, apply, or salvage.

## Item ownership and tradeability

- Covenant affix data belongs to an individual equipment item instance.
- Two otherwise identical base items can carry different Covenant affixes and Crafting Potential.
- Affixes and the item's remaining Crafting Potential travel with the item through inventory, containers, drops, pickup, trading, player transfer, and save/reload.
- Affixed equipment remains tradeable. Covenant affixes do not soulbind equipment.
- Affixes are active according to the equipment/stat rules; unequipping an item does not erase its affixes.
- Item-stack copy, split, and identity semantics must be safe. Uniquely customized equipment will generally need stack size one unless safe splitting is explicitly supported.

## Affix structure

### Prefixes and suffixes

Covenant affixes are classified as either **Prefixes** or **Suffixes**. Equipment has separate capacity limits for each category.

- An item can have a different number of available Prefix and Suffix slots.
- Item type, rarity, and overall power determine the applicable capacities.
- Crafting cannot exceed either category's capacity.
- Exact capacity tables are deferred to itemization and balance design.

### Affix eligibility

Affix eligibility is data-driven. Definitions can use equipment categories, tags, affix-family tags, and explicit identifiers to determine which items can receive an affix.

This must support vanilla and modded equipment without requiring a hardcoded list for every mod. Server owners and optional integrations should be able to extend compatibility rules.

### Affix limits

Each affix has a maximum crafting tier or power determined by the overall rarity and power of the item carrying it. A common or low-power item cannot be upgraded indefinitely into endgame equipment solely through Crafting investment.

The exact relationship between item rarity/power and affix maximums remains to be tuned.

## Crafting Potential

Each item has an item-wide **Crafting Potential** budget.

- The item's rarity and overall power influence its initial Crafting Potential.
- Affix crafting operations consume Crafting Potential.
- When the item's potential is exhausted, ordinary Covenant affix modification is no longer possible.
- The item remains usable and tradeable after its potential is exhausted.
- More favorable crafting outcomes may be possible, but are not guaranteed. The exact outcome model and how favorable outcomes interact with potential consumption remain to be designed.

Crafting Potential is distinct from affix capacity and individual affix maximums. All three constraints apply independently.

## Supported operations

The Covenant Workbench is intended to support four core operations:

1. **Add:** Apply a new Covenant affix to eligible equipment.
2. **Upgrade:** Increase an existing affix's tier or value, within the item's limits.
3. **Reroll:** Change an existing affix's value within its permitted range.
4. **Remove:** Permanently remove an existing Covenant affix.

Players deliberately select the target affix and operation. The system should be relatively deterministic; more favorable efficiency or outcomes may be possible but are not guaranteed. Exact material costs, probabilities, and operation-specific Crafting Potential costs are deferred for later tuning.

## Affix availability and rare components

- Standard/common affixes become available through Crafting profession progression.
- Higher affix crafting tiers become accessible at Crafting Levels 5, 10, 15, and 20.
- Rare affixes require successful recovery of the corresponding affix-family component through salvaging equipment that carries that affix.
- Each rare affix on a salvaged item receives its own independent component-recovery roll.
- Recovered rare affix components are consumable. Applying the corresponding rare affix consumes the required component; recovery is not a permanent account unlock.
- Generic crafting materials are also used. Rare affixes require their specific component in addition to applicable generic materials.
- Crafting level may impose an additional requirement for applying particularly powerful affixes.

Exact affix pools, tiers, component identities, probabilities, and material costs remain open.

## Apotheosis and other mod compatibility

Covenant's Prefix/Suffix capacity, affix limits, and Crafting Potential apply only to Covenant affixes. Apotheosis affixes and other mod-provided item modifiers retain their own data, limits, and mechanics.

Both systems may coexist on the same item where technically compatible. Covenant must not overwrite, remove, or invalidate Apotheosis affixes, gems, sockets, or other item data. Apotheosis remains optional; Covenant's own affix and equipment customization systems must work without it.

See [[Crafting Profession]] and [[Salvaging]].
