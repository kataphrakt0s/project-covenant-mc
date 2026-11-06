# Classes and Specializations

## Playable classes

The initial playable identities are **Stalker**, **Reaver**, and **Weaver**. Classes determine combat identity, class resource, and access to their class skill tree. They do not impose rigid equipment restrictions; vanilla and modded equipment should remain usable where mechanically appropriate.

- **Stalker**: bow-focused ranged combat, precision, mobility, and maintaining distance. Resource: Focus.
- **Reaver**: direct melee combat, aggression, and close-quarters pressure. Resource: Rage.
- **Weaver**: elemental spellcasting and magical control. Resource: Mana.

Stable IDs are `covenant:stalker`, `covenant:reaver`, and `covenant:weaver`. The existing player profile owns and persists the selected class. The current command surface includes `/covenant class select <id>` for one-time server-authoritative selection and `/covenant class` to list choices/current selection. See `docs/RPG_DATA.md` and [[Class Resources]].

## Class skill progression

Each class has its own skill tree. Character level gates the first point invested in each skill; the first point unlocks Rank 1. Further ranks use skill points without additional character-level gates. A character may have no more than eight distinct skills allocated at once, matching the eight combat keybinds. Individual skill rank caps are 8 or 12.

See [[Combat Skill Progression]] and [[Skill Trees Brainstorm]].

## Equipment and professions

Classes do not restrict profession development. Every class can independently develop Mining, Logging, Farming, and Crafting. Profession abilities and their modest combat crossovers are available regardless of class and do not consume combat keybinds.

## Specialization scope

The class skill trees, their prerequisites, and explicitly mutually exclusive options are the current within-class customization foundation. A separate class-specialization or talent-point economy is not established by the current progression design. Do not invent one as a prerequisite for the vertical slice.

Related: [[Abilities]], [[Class Resources]], [[Character Progression]], [[Profession Progression]].
