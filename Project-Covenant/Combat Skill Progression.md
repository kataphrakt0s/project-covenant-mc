# Combat Skill Progression

## Character point budget

- Character level starts at 1 and is capped at 60.
- Character creation grants one skill point at level 1.
- Each subsequent character level grants one additional point: 60 lifetime points by level 60.
- Unspent points remain available and never expire.
- Character levels do not grant attribute points or separate general-stat points.

See [[Character Progression]].

## Unlocking and ranking skills

- Every skill has a minimum character level, transcribed from the proposed class-tree diagram in [[Skill Trees Brainstorm]].
- The listed level is a requirement to invest the **first** point in that skill.
- The first point unlocks the skill at Rank 1.
- Additional points can be invested immediately, without further character-level gates, until the skill reaches its individual maximum rank.
- Each rank consumes one skill point.
- Each skill's maximum rank is individually defined as either **8 or 12**. The cap depends on how much meaningful improvement the skill can support; it is not a blanket rank for all skills.
- Every rank should provide a meaningful upgrade. A 12-rank skill should have enough meaningful tuning opportunities to justify the extra ranks rather than padding the cap with negligible bonuses.

The exact choice of 8 versus 12 and the effect of each rank remain per-skill balance work.

## Eight-skill allocation limit

A character may have no more than **eight distinct skills with points allocated** at one time. This corresponds to the eight available combat keybinds (Q, R, F, C, and their Shift variants). A skill counts toward the limit as soon as it has at least one point invested, including prerequisite skills. Rank investment in an already-allocated skill does not consume another allocation. This is an allocation ceiling, not a requirement to fill every keybind slot.

The eight slots are a hard combat-loadout limit. Profession abilities do not use or compete for these keybinds.

## Prerequisites and exclusions

- Vertical tree connections represent prerequisites. The prerequisite must be unlocked before the connected skill can receive its first point. Both skills remain usable after the dependent is unlocked.
- Crossed horizontal connections represent deliberate mutually exclusive choices. Only explicitly connected alternatives lock one another out.
- Prerequisites and exclusions are validated server-side.
- The existing dependency behavior—removing a prerequisite also removes dependent allocations and clears their loadout assignments—must remain coherent with the point refund/respec rules.

## Respec

Combat respec is allowed and requires a crafted respec item. Using it resets the character's combat skill allocations and refunds all invested skill points to the unspent pool. Character level and earned XP are retained. The respec item recipe, materials, crafting station, and acquisition pacing remain undecided.

The current tree prototype's free allocation/unlearning is a testing implementation and does not define the final progression economy.

## Skill design and balance

Skill rank progression should improve the skill's own identity and usefulness. Avoid making every rank a raw damage multiplier; upgrades can affect damage, area, duration, cooldown, resource cost, control, reliability, or other mechanics where appropriate. Preserve the vanilla-friendly stat philosophy in [[Statistics]] and [[Design Pillars]].

Related: [[Character Progression]], [[Skill Trees Brainstorm]], [[Abilities]], [[Classes and Specializations]], [[Player Profiles]], [[Progression Balance Configuration]].
