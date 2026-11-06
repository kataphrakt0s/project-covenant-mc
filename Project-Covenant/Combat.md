# Combat

Hybrid model: preserve recognizable Minecraft attacks, weapons, blocking, and equipment interactions while adding abilities, class resources, cooldowns, stats, status effects, and specialized mechanics.

Enemy roles: ordinary, elite, champion/rare, boss. Modded entities should participate without mandatory source-mod dependencies.

## Shared in-combat state (agreed, planned)

Use one logical-server seven-second combat timer (140 game ticks) for all
classes. Confirmed combat damage dealt/taken and qualifying combat support
refresh it. Follow the existing actor-damage exclusions for self, allied, and
environmental damage. Support qualification follows
[[Experience and Reward Systems]], while generic combat damage recognition is
not restricted to enemies that award character XP.

Rage's combat grace, skill-investment Apply, and out-of-combat respec checks read this same state, rather
than maintaining separate combat timers. Rage generation remains damage-driven
under its existing source rules: being in combat or supplying support alone
does not generate Rage. The shared timer prevents out-of-combat Rage decay
until it expires.

Combat respec requires being out of combat when use begins and completes.
Re-entering combat during the two-second hold cancels use without item
consumption or allocation changes.

Applying pending skill ranks also requires being out of combat, validated by
the server when committing. Players can edit their pending draft during combat;
a blocked Apply spends nothing and leaves the draft available while open.

This is planned progression work; the current implementation's grace refresh is still restricted to Reaver.
Broaden/reuse that server-owned timestamp without adding proximity scans.

Related: [[Abilities]], [[Class Resources]], [[Encounter Design]], [[Mod Compatibility]].
