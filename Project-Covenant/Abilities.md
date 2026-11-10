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
- A crafted combat respec item refunds invested skill points. The prototype
  recipe is four paper plus one lapis lazuli, shapeless at a vanilla crafting
  table, producing one item; see [[Combat Skill Progression]].

The current free-learning behavior is a prototype/testing state, not the intended progression economy. See [[Combat Skill Progression]] and [[Skill Trees Brainstorm]].

## Existing keybindings and auras

The current keybinding/loadout implementation has eight combat slots: Q, R, F, C, and their Shift variants. Auras use the shared model described in [[Skill Trees Brainstorm]]: their passive effect is active while assigned to a slot, and pressing the assigned key activates a distinct temporary effect. Profession abilities do not consume these combat slots; they use contextual actions, tools, workstations, or passive/conditional effects as appropriate.

## Current implementation notes

Learning/assignments currently persist with the player. Unlearning clears matching assignments and cancels a pending cast without resetting cooldowns in the prototype. The final respec behavior is defined by [[Combat Skill Progression]]. Visual and playtesting status for individual skills remains in [[Skill Trees Brainstorm]] and [the implementation contract](../docs/RPG_SKILLS.md).

## Presentation and feedback

The current polish priority is **player feel and effect clarity**. Effects should
communicate the feedback a player needs from using a skill without crowding the
visual space. Minecraft's native sounds, models, animations, and particles are
appropriate for this pass; custom assets are not a prerequisite.

Choose cues around meaningful skill events: cast start, release, impact, deployment
readiness, and relevant buff or control-state changes. Give each cue a clear purpose
and keep repeated or persistent effects restrained. Effects should communicate the
actual affected area and distinguish skill or elemental behavior where that helps
the player act. A stronger effect is useful when it communicates a more significant
outcome.

Implementation and exact asset choices remain iterative. In-game feedback should
guide timing, volume, particle density, first-person visibility, and readability
with several active skills or players. Automated checks establish behavior and
technical boundaries; the user evaluates how the result looks, sounds, and feels.

The first combat pass now provides native sound cues, elemental and arrow
distinction, trap transition feedback, and attack/deployment shapes matching the
existing gameplay. See [Combat feedback](../docs/RPG_SKILLS.md#combat-feedback-pass)
for its scope and manual checks.

Simple native Minecraft icons in the top-left now track open combos, Discharge
charges, Threefold reserves/bonuses and received buffs. Small counts and timers
provide actionable feedback; the old status text above hunger is removed.
The class resource bar keeps its existing position. Buff icons describe actual
server-owned effects, including effects received from allies before class choice.

While an area aura is slotted, a thin persistent particle outline shows its
six-block horizontal footprint near the owner's feet. Shared radii combine into
one local-owner ring to keep the scene readable. Self-only auras show no area;
particle settings still apply. This visual does not change the underlying
spherical range, solid-cover rules, or passive refresh/grace. Deployed traps use
grounded native block models, with a small tumble during flight and particles
left to show triggered patches. Progression/Workbench feedback remains a later pass.

Related: [[Classes and Specializations]], [[Class Resources]], [[Combat]], [[Profession Progression]].
