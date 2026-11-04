# Item Data and Persistence

Item-instance RPG data must be stored with the associated stack using the target version's supported item data/component mechanism.

Must survive save/reload, inventory/container movement, drop/pickup, trade/player transfer, and safe copy/split operations. Current owner, inventory slot, and item type are never the source of truth for an Affix Page.

Related: [[Affix Pages]], [[Equipment]], [[Equipment Categories]].
