# Player Profiles

Persistent per-player RPG data includes initialization state, character progression, selected class, independent class-resource identity/current/maximum values, and profession progression. It is server-authoritative and survives logout, death/respawn, and server restart. Handle player cloning/respawn correctly and synchronize the relevant client data.

## Character progression data

Persist at minimum:

- Character level and Covenant character XP.
- Selected class and class resource state.
- Unspent combat skill points.
- Allocated skills and their ranks.
- Combat skill loadout/keybindings.

Character level is capped at 60. A new character begins at level 1 with one skill point; each level gained after level 1 awards one additional point, reaching 60 total by level 60.

## Profession progression data

Persist each profession independently:

- Profession XP and level.
- Unspent Profession Points.
- Purchased profession abilities/ranks and chosen mutually exclusive branches.

Initial professions are Mining, Logging, Farming, and Crafting. Each starts at level 0, uses the shared profession XP curve, and defaults to a maximum of level 20. Profession respec resets only the selected profession's allocations, refunds its Profession Points, and preserves its level and XP.

## Authority and synchronization

Progression changes, XP awards, dimension-cap checks, unlock validation, point spending, and respec operations are server-authoritative. Synchronize only the relevant data required for client presentation. Never trust client-supplied progression values or activity outcomes.

Do not store equipped item modifiers as permanent profile data. Item-bound RPG data remains on the item stack through [[Affix Pages]] and [[Item Data and Persistence]].

Related: [[Character Progression]], [[Combat Skill Progression]], [[Profession Progression]], [[Classes and Specializations]], [[Class Resources]], [[Networking and Authority]].
