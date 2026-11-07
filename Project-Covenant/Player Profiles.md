# Player Profiles

Persistent per-player RPG data includes initialization state, character progression, selected class, independent class-resource identity/current/maximum values, and profession progression. It is server-authoritative and survives logout, death/respawn, and server restart. Handle player cloning/respawn correctly and synchronize the relevant client data.

## Initial progression format compatibility

The planned progression slice does not require support for existing prototype
saves. Build and validate the new progression format using fresh saves, without
legacy profile/loadout migration or conversion of freely learned skills. The
new format must retain normal persistence and lifecycle guarantees once created.
See [[Progression Vertical Slice]].

## Character progression data

Persist at minimum:

- Character level and Covenant character XP.
- Selected class and class resource state.
- Unspent combat skill points.
- Allocated skills and their ranks.
- Combat skill loadout/keybindings.
- Persistent first-Wither level-reward claim state, so the one-time reward
  cannot repeat after reconnect, restart, or death/respawn. Mark it only when
  the level is granted, committing the level, rescaled XP progress, skill point,
  and claim together. An ungrantable reward is neither consumed nor queued;
  see [[Character Progression]].

Character level is capped at 60. A new character begins at level 1 with one skill point; each level gained after level 1 awards one additional point, reaching 60 total by level 60.

Character progression begins when the player first selects a combat class, at
Level 1 with zero character XP and one skill point. Players without a selected
class do not earn or bank character XP. Earlier combat is not rewarded
retroactively. Profession initialization and progress are independent.

## Profession progression data

Planned profession initialization occurs on first login, independently of combat
class selection. Players without a selected class can earn profession XP and
points, purchase abilities, and use earned profession benefits. Selecting a class
later must preserve all profession state. The current implementation initializes
the character profile during class selection; it has no profession tracks yet.
Character XP requires class selection; profession progression does not.

Persist each profession independently:

- Profession XP and level.
- Unspent Profession Points.
- Purchased profession abilities/ranks and chosen mutually exclusive branches.

Initial professions are Mining, Logging, Farming, and Crafting. Each starts at
level 0, uses the shared profession XP curve, and defaults to a maximum of
level 20. Planned prototype Profession Respec resets all four professions'
allocations together, refunds each track's spent Profession Points, and
preserves every track's level and XP. See [[Profession Progression]].

## Authority and synchronization

Balance reloads and restarts with edited progression definitions preserve
earned levels, points, allocations, and raw current-level XP for online and
offline characters. Do not reset progress or recalculate earned levels from the
new curve. Percentages can change because future requirements changed; see
[[Progression Balance Configuration]].

For planned combat XP sharing, contributor identity follows the player UUID
through death/respawn. Death preserves credit within the original contribution
window. Payouts while dead or across player replacement must reach the retained
progression state exactly once; a stale player entity must not lose or duplicate
the award. Combat XP payouts require the player to be online at enemy death;
there is no offline award queue or retroactive payout on reconnect. See
[[Experience and Reward Systems]].

Progression changes, XP awards, dimension-cap checks, unlock validation, point spending, and respec operations are server-authoritative. Synchronize only the relevant data required for client presentation. Never trust client-supplied progression values or activity outcomes.

Do not store equipped item modifiers as permanent profile data. Item-bound RPG data remains on the item stack through [[Affix Pages]] and [[Item Data and Persistence]].

Related: [[Character Progression]], [[Combat Skill Progression]], [[Profession Progression]], [[Classes and Specializations]], [[Class Resources]], [[Networking and Authority]].
