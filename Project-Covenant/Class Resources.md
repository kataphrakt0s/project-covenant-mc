# Class Resources

Implemented resource infrastructure shares bounded spending, persistence and owner synchronization,
with independently preserved balances for Stalker, Reaver and Weaver. Only the selected class
recovers or depletes. See [the implementation contract](../docs/RPG_SKILLS.md).

The resource HUD shows only the current class's resource: red Rage for
Reaver, blue Mana for Weaver, or yellow Focus for Stalker. Inactive class resources
are hidden. The bar sits directly above the vanilla hunger bar and displays
centered current/maximum values, such as `75 / 100`. Its color identifies the
resource. It stays visible while a known class is selected, including at full or
empty resource and outside combat, and follows the normal F1 HUD visibility.
Air bubbles and mount health stack above it. Rendered behavior awaits playtesting.

## Rage

Planned progression change: the existing seven-second grace becomes the shared
in-combat state for every class and is also used to block combat respec.
Qualifying combat support refreshes that shared state. This holds Rage decay
off; support alone and the timer itself do not generate Rage or change the
damage-source generation rules below. See [[Combat]]. The current refresh path
is still Reaver-only until this planned change is implemented.

Reaver: starts empty, base maximum 100. Normal melee, arrow, and trident attacks
generate 1 Rage per damage point dealt; combat damage taken generates 2 Rage per
damage point. Both use confirmed damage after defenses, including absorption, and
the existing `covenant:rage_generation` multiplier. Skills (including Cleave, Rend,
and bleed) grant no outgoing Rage. Self, allied, and environmental damage are excluded.
Combat grants no passive generation. Confirmed actor damage, including skill damage,
refreshes a seven-second grace timer that prevents decay; afterward Rage depletes
10/second. Capacity, both damage coefficients, and depletion are server-configurable.

Planned exception: assigning Raging Aura to a skill slot reduces out-of-combat
Rage decay after the existing grace period. It does not extend the grace period.
Its activated buff stops passive Rage decay while skills continue to spend Rage
normally. This is agreed design in [[Skill Trees Brainstorm]] and is not
implemented yet.

## Mana
Weaver: starts full, base maximum 100, recovers 5/second. Capacity and recovery scale
through progression/native attributes and equipment modifiers, without a fixed 100-unit cap.

Planned exception: Mana regeneration pauses while channeling Arcane Barrage or
Scorching Ray and resumes when the channel ends. This is agreed design in
[[Skill Trees Brainstorm]] and is not implemented yet.

## Focus
Stalker: starts full, hard maximum 100, recovers 25/second. Equipment cannot raise
capacity. Regeneration modifiers cap at 25% above the configured base; this foundation
adds no common regeneration-affix acquisition source.

Planned exception: Focus regeneration pauses while channeling Barrage and resumes
when the channel ends. This is agreed design in [[Skill Trees Brainstorm]] and is
not implemented yet.

Related: [[Classes and Specializations]], [[Abilities]].
