# Item Data and Persistence

Item-instance RPG data must be stored with the associated stack using the target version's supported item data/component mechanism.

Must survive save/reload, inventory/container movement, drop/pickup, trade/player
transfer, and safe copy/split operations. Current owner and inventory slot are
never the source of truth for item-instance RPG data.

Planned Covenant equipment rarity is saved on each individual equipment stack,
alongside its item-owned customization state. Two copies of a base item may
have different rarities; transferring the stack preserves its assigned rarity.
Ordinary affix operations preserve the initialized rarity too, including
removal of the starting affix or all affixes. A zero-affix initialized item must
remain distinguishable from a never-initialized crafted item.

For uninitialized crafted gear, initialize Potential once alongside the first
Workbench affix and rarity, deducting the Add cost in the same commit. Freshly
generated affixed loot starts with its full rarity-based budget, without Add or
Upgrade charges for its natural starting affix. Keep remaining Potential on the
item even if all affixes are removed; removal, transfer, and later loot/drop
events do not reset the budget.
See [[Affixes and Rarity]] and [[Affix System and Equipment Customization]].

Related: [[Affix Pages]], [[Equipment]], [[Equipment Categories]].
