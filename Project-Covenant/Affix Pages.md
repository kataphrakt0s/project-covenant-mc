# Affix Pages

## Definition
An Affix Page is RPG data attached to an individual Minecraft item stack, allowing vanilla or modded equipment to gain RPG properties without replacing its identity.

## Non-negotiable semantics
- Belongs to an individual item instance, not item type, player profile, slot, or owner.
- Two identical base items may have different pages.
- Stored with the item stack.
- Travels through inventory/container movement, drops, pickup, trading, player transfers, and save/reload.
- Unequipping never removes or modifies the page.
- Modifiers contribute only while equipped.
- Transfer moves the page unchanged; receiving player's stats change when equipped.

## Technical considerations
Use the target version's supported item-stack data/component mechanism. Define safe copy/split semantics; unique equipment should generally be stack size one unless safe splitting is explicitly supported.

## Future scope
Identity, category, rarity/tier, multiple affixes, stat modifiers, upgrade metadata. Generation, acquisition, crafting, extraction, and UI are later systems.

Related: [[Equipment]], [[Equipment Categories]], [[Item Data and Persistence]], [[Affixes and Rarity]], [[Stat Calculation Pipeline]].
