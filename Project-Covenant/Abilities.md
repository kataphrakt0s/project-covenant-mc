# Abilities

Abilities express class identity through damage, defense, mobility, control, utility, and resource interactions.

The basic implementation provides Piercing Shot (Stalker bow attack), Cleave (Reaver
nearby melee attack), and Arcane Bolt (Weaver single-target magic projectile).
Code-backed definitions own class access, resource costs, cooldowns, cast times,
and effect parameters. Existing native skill multipliers apply. The server validates,
casts, spends and executes. Radial slot 1 opens a small Skills window with one node
per class: left-click learns/selects for free, right-click unlearns, and eight slots
assign learned skills to Q/R/F/C and their Shift variants. Commands and keybindings
share the learned-skill requirement. Learning/assignments persist with the player;
unlearning clears matching assignments and cancels a pending cast without resetting
cooldowns. See [the implementation contract](../docs/RPG_SKILLS.md). Broad trees,
points/unlock progression, specializations, larger collections and action-bar HUD
remain deferred.

Planned skill-tree design is recorded in [[Skill Trees Brainstorm]]. Vertical
connections require learning the lower skill before the higher skill, with both
remaining usable. Crossed horizontal connections mark mutually exclusive learning
choices: learning Ripping Aura locks out Fortitude Aura, and vice versa. These
rules are agreed design and are not implemented in the current Skills window.

Planned aura model: an aura supplies an always-on passive effect while assigned to
a skill slot, and pressing its assigned keybind activates a different, stronger
temporary buff. Learning alone does not activate the passive; removing the aura
from the loadout ends it. Specific effects are being designed in
[[Skill Trees Brainstorm]]; auras are not implemented.

Related: [[Classes and Specializations]], [[Class Resources]], [[Combat]].

Rend and its shared bleed foundation are now implemented alongside Aimed Shot and
the original three skills. Rend is an instant native-reach melee strike costing
20 Rage with a two-second cooldown. A damaging hit adds one five-second stack,
dealing 10% of captured attack damage per second, up to three stacks shared across
attackers. Reapplication refreshes all durations and replaces the oldest stack at
the cap. Bleeding persists with paused timers while targets are inactive and clears
on death. See [[Skill Trees Brainstorm]] and the implementation contract for the
full targeting, defense, ownership, and persistence rules. Other bleed-dependent
skills remain planned.

Skill visuals use vanilla-style hand/weapon movements and skill-specific particles
for the existing three skills. Custom character animation rigs are outside this
pass. Rendered behavior awaits playtesting; see
[the implementation contract](../docs/RPG_SKILLS.md).

Arcane Bolt's effects are a blue-white charge during its existing cast,
a sparkling projectile trail, and a small impact burst.

Cleave's effects are a vanilla-style weapon swing, a brief circular sweep
matching its existing all-around reach, and red sparks on enemies hit.

Piercing Shot's effects are a brief vanilla bow-charge animation,
a subtle yellow-white arrow trail, and a small impact spark. It remains instant,
with no added firing delay. The cosmetic draw lasts six ticks (0.3 seconds at
20 TPS) and does not start actual item use or interfere with normal bow use.
