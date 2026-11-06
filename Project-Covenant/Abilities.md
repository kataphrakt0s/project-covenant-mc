# Abilities

Abilities express class identity through damage, defense, mobility, control, utility, and resource interactions.

The basic class abilities include Piercing Shot (Stalker), Cleave (Reaver), and Arcane Bolt (Weaver), with additional implemented and planned skills recorded in [[Skill Trees Brainstorm]]. Code-backed definitions own class access, resource costs, cooldowns, cast times, and effect parameters. The server validates, casts, spends, and executes abilities. See [the implementation contract](../docs/RPG_SKILLS.md).

## Combat skill progression

The intended progression model replaces free prototype learning:

- Character level 1 grants the first skill point; each subsequent character level grants one more, for 60 total at level 60.
- A skill's listed minimum character level gates its initial point only. Spending that first point unlocks the skill at Rank 1.
- Further points increase the skill's rank without further character-level gates.
- Skill maximum rank is assigned individually as 8 or 12.
- No more than eight distinct skills may have points allocated at once, corresponding to the eight combat keybinds.
- Prerequisites and genuine mutually exclusive choices are enforced.
- A crafted combat respec item refunds invested skill points. Its recipe remains undecided.

The current free-learning behavior is a prototype/testing state, not the intended progression economy. See [[Combat Skill Progression]] and [[Skill Trees Brainstorm]].

## Existing keybindings and auras

The current keybinding/loadout implementation has eight combat slots: Q, R, F, C, and their Shift variants. Auras use the shared model described in [[Skill Trees Brainstorm]]: their passive effect is active while assigned to a slot, and pressing the assigned key activates a distinct temporary effect. Profession abilities do not consume these combat slots; they use contextual actions, tools, workstations, or passive/conditional effects as appropriate.

## Current implementation notes

Learning/assignments currently persist with the player. Unlearning clears matching assignments and cancels a pending cast without resetting cooldowns in the prototype. The final respec behavior is defined by [[Combat Skill Progression]]. Visual and playtesting status for individual skills remains in [[Skill Trees Brainstorm]] and [the implementation contract](../docs/RPG_SKILLS.md).

Related: [[Classes and Specializations]], [[Class Resources]], [[Combat]], [[Profession Progression]].
