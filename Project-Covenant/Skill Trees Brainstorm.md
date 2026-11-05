![[Pasted image 20261104154316.png]]

The image is the proposed skill roster and level layout. Piercing Shot,
Aimed Shot, Cleave, Rend, Arcane Bolt, and all four Stalker traps are implemented; the remaining entries are
planned skills. The breakdowns record agreed gameplay direction. Levels below
are transcribed from the image and are informational rather than level gates.
The first tree pass now displays only the current class's roster, with free
allocation and the connection rules below enforced by the server. Planned nodes
are bindable placeholders without gameplay effects. The right-hand Keybindings
tab holds the learned-skill picker and existing eight slots.
The entire current-class tree fits on one page using compact square nodes with
hover tooltips and temporary initials. Empty lanes and spacing are compressed;
tier order and prerequisite/exclusion flow remain the same. Skill icons come later.

## Connection rules

- Vertical connections are learning prerequisites: the lower skill must be learned
  before the connected higher skill. Both skills remain usable; learning a dependent
  skill does not replace its prerequisite.
- A crossed horizontal connection denotes mutually exclusive skills. Learning one
  locks out learning the other. The pictured pair is Ripping Aura and Fortitude Aura.
- Deallocating a prerequisite also deallocates its learned dependents and clears
  every affected slot. Allocation and deallocation are free in this pass.

## Future skill progression

Skills will eventually have multiple ranks, purchased from an available skill-point
pool that gains one point per character level. XP progression, the point economy,
and rank scaling are deferred from this brainstorm pass. The existing prototype's
free learning remains unchanged while skill behavior is implemented separately.

## Trap rules

The agreed design below has an
[ordered implementation checklist](../docs/TRAPS_IMPLEMENTATION_PLAN.md).
All four trap skills are implemented; visuals, gamefeel, and live multiplayer checks remain for user playtesting.

Bear Trap, Frost Trap, Flame Trap, and Blast Trap share a fixed short forward
toss, approximately three blocks along the player's horizontal facing direction.
Looking up or down does not change the intended throw distance or direction.
This keeps placement predictable while retreating. These traps share the transient native deployment in `TrapEntity`.

Thrown traps collide with terrain and settle onto supporting ground. A wall
stops forward travel and the trap drops at its base; a ledge lets it fall to
the surface below. Solid supporting surfaces include slabs and stairs. Traps
cannot attach to walls or remain suspended in midair. Obstructed throws can
therefore land short of the intended distance.

Landed traps and active patches require dry, solid support at their placement
point throughout their lifetime. A thrown trap entering water or lava fizzles.
Flooding the placement point or removing its support ends the landed trap or
patch. A toss over a ledge may still fall normally before its first landing.
These removals do not refund the spent Focus or cooldown.

Traps arm 0.5 seconds after landing, with a visible cue when ready. They cannot
trigger during the toss or arming delay. An eligible enemy already standing on
the trap triggers it once it becomes armed.

Traps are visible to all players, including opponents. Each type has a
distinct appearance and a clear armed cue so players can identify and avoid
them and owners can track their placements. Traps are not concealed in this pass.

Deployed traps cannot be destroyed by attacks, disarmed, or picked up in this
prototype. Players can avoid them, trigger them, or wait for expiry. There is
no trap health, disarming action, or recoverable trap item in this pass.

An untriggered trap expires harmlessly 30 seconds after arming unless triggered
or replaced sooner. Triggered effects have their own separate durations.

Trigger eligibility follows the existing skill targeting rules: living,
non-spectator targets other than the owner, excluding the owner's pets and
allies, and respecting PvP restrictions. Passive animals and modded living
creatures can trigger traps; eligibility is not limited to hostile mobs.

Triggering requires the creature's hitbox to intersect a shallow contact region
above the trap's 1.5-block-wide footprint. Mere proximity does not trigger it,
and creatures passing clear overhead do not set it off. Frost, Flame, and Blast
can affect a larger area after triggering; their effect areas are separate from
this shared contact footprint.

Each player may have one armed trap of each type. Throwing another trap of the
same type replaces that player's previous armed trap of that type. Different
trap types can coexist.

Each player may also have one active Frost patch and one active Flame patch.
Throwing or arming the next trap does not remove an existing active patch.
When the next trap of that type triggers, its new patch replaces that player's
previous active patch of the same type. Frost and Flame patches can coexist;
these limits are per owner, not shared across players.

Deployed traps and active patches disappear when their owner dies, logs out,
changes dimension, or switches class. Unlearning a trap skill removes only
that skill's deployed traps and patches belonging to that player.

Traps and active patches also disappear when their containing chunk unloads.
They do not keep chunks loaded or return when the chunk loads again; unloading
can therefore remove an unused trap before its 30-second armed lifetime ends.

Focus is spent and the cooldown starts when a trap is successfully thrown,
regardless of whether it later triggers. Expiry, replacement, and cleanup give
no refund. Rejected activations, including insufficient Focus or an active
cooldown, spend nothing and do not start a new cooldown.

Traps are learned skills paid for with Focus. They require no consumable trap
item, ammunition, or particular held weapon, and can be thrown with a bow,
another weapon, or an empty hand.

For the first implementation, Bear Trap, Flame Trap, and Blast Trap each use
their own base damage values, independent of the held weapon's native melee
Damage stat. Switching between a bow and a sword does not change trap damage;
weapon damage upgrades do not scale it in this prototype.

The existing skill-duration multiplier scales Bear Trap's root duration and
the active Frost and Flame patch durations. It does not scale the fixed
0.5-second arming delay or the fixed 30-second untriggered lifetime. A 20%
duration bonus gives a 3.6-second Bear root, 9.6-second Frost patch, and
7.2-second Flame patch at the agreed base tuning.

Capture the owner's effective duration bonus when the trap is thrown. Later
equipment or stat changes do not alter that deployment's root or patch duration;
triggering does not recalculate the captured duration.

Shields cannot block trap damage or prevent trap effects. This shared rule
applies to all four traps, including triggered areas and explosions.

Solid walls and floors block Frost, Flame, and Blast effects. Radius alone
does not allow a trap effect to reach enemies through a solid barrier, including
an enemy on the floor above. Cover therefore protects against these area effects.

Patches are temporary skill effects and leave terrain unchanged. Frost does
not freeze water or place snow or ice; Flame does not place fire, ignite
vegetation, or melt blocks. Their visuals show the affected area without
changing blocks, consistent with Blast's no-block-destruction rule.

There is no blanket boss immunity to Bear Trap's root or Frost Trap's slow in
this prototype. Eligible bosses receive the same control effects as other
creatures, subject to the agreed defenses and invulnerability rules. Native
knockback resistance still applies to Blast. Flying creatures must still
contact the shallow ground-level trigger region to set a trap off.

## Planned aura model

Auras provide an always-on passive effect while assigned to a skill slot. Learning
an aura alone does not activate it; removing it from the loadout ends its passive.
Pressing its assigned keybind activates a different, stronger temporary buff. Each
aura's passive and active effects will be defined individually. This model applies
to auras across the classes and is not implemented yet.

## Bleed stacking

Repeated bleed applications add separate stacks, up to 3 stacks per enemy. Each
stack counts as one bleed instance for Consume and Guillotine's flat consumption
damage. Blood Fever counts bleeding enemies, not the number of stacks on them.
Reapplying bleed refreshes the duration of all existing stacks, including when
already at the 3-stack cap. Rend and the shared bleed foundation are implemented;
the dependent bleed skills below remain planned.

At the cap, a new application replaces the oldest stack and refreshes all stack
durations. The replacement carries the new application's damage and attacker;
a weaker application can therefore replace a stronger stack.

The three-stack limit is shared across attackers. Each stack retains its applying
attacker for damage attribution and future healing from that stack's bleed damage.
Consume and Guillotine remove all bleed stacks on affected enemies, including
stacks applied by other players. This allows shared setup and finishers; consuming
another player's stacks also removes that player's ongoing bleed damage.

Rend creates each stack with base bleed damage scaled from the attacker's effective
native attack damage at application time. Later weapon/equipment changes do not
recalculate existing stacks. Refreshing a retained stack's duration preserves its
base damage; a replacement stack captures the new application's attack damage.
Rend's prototype bleed damage is 10% of captured attack damage per stack per
one-second pulse, with a five-second base duration. Three equal-power stacks deal
30% of that attack damage per second before the remaining applicable defenses.

Rend's initial strike uses normal melee defenses. Bleed ticks bypass armor,
shield blocking, and Dodge, but respect general damage reduction (such as
Resistance), absorption, and invulnerability. Bleed is not damage that ignores
every defense.

Bleed ticks operate independently of the brief hurt-immunity window between normal
hits: normal hits cannot suppress scheduled bleed damage, and bleed ticks cannot
suppress normal hits or other stacks' ticks. Actual invulnerability, such as
Creative mode, remains respected.

Each bleeding enemy uses one shared tick schedule at one-second intervals (20
game ticks). The first tick occurs one second after bleeding starts. Each pulse
applies every active stack's damage separately with its own attacker attribution.
Adding, refreshing, or replacing stacks does not reset the schedule; a newly
applied stack participates in the next pulse even if it is less than a second old.

Bleeding persists across save/load, target disconnects, and entity unloading. Save
each stack's attacker ownership, captured base damage, and remaining duration,
plus the target's time until its next bleed pulse. Duration and pulse timers pause
while the target is not ticking and resume without offline damage or catch-up
bursts. An attacker disconnecting does not remove their existing bleeds. Death
clears bleeding; stacks do not carry into a replacement player after death.

Rend applies or refreshes bleeding only when its initial strike deals positive
damage to health or absorption. Dodged, fully blocked, and immune hits do not add
stacks, replace stacks, or refresh their durations. Damage absorbed by absorption
hearts still qualifies as a damaging hit.

All otherwise-valid living targets can bleed, including undead, golems, bosses,
and modded living creatures. This implementation adds no creature-type bleed
immunities; ordinary targeting rules and actual invulnerability still apply.

# Skill Details
## Stalker

#### Piercing Shot
Diagram level: 1.

Planned behavior: fire one arrow that can hit up to three enemies in a line,
stopping at blocks. This gives the skill a role in damaging lined-up enemies.
Damage tuning is deferred.

Current implementation: fire one bow arrow at full speed through up to three
targets, stopping at blocks, with base damage coefficient 3 instead of vanilla 2
(+50%). This uses native arrow piercing and collision handling.

#### Disengage
Diagram level: 3.

Planned behavior: leap backward relative to the direction the player is facing.
No target is required. This lets the player keep aiming at an approaching enemy
while creating distance. Leap distance and other numerical tuning are deferred.

#### Bear Trap
Diagram level: 3.

Current implementation: throw a single-use trap a short distance in front of the player.
It damages and briefly roots the first enemy to step on it. The root blocks
self-directed walking and jumping, while allowing turning, attacks, and casting.
Gravity and knockback still apply: Blast Trap can push a rooted enemy away,
but that enemy cannot resume walking until the root expires. This creates an
opportunity to gain distance and prepare a shot.

Implemented prototype tuning: 20 Focus, eight-second cooldown,
four damage points (two hearts before defenses), three-second root, and an
instant throw with no cast wind-up. The shared post-landing arming delay still
applies. These are base values for initial playtesting.

Taking damage does not break the root; it lasts its full duration. The target
can still retaliate with attacks or casting while rooted.

Repeated successful Bear Trap roots keep the longer of the existing remaining
duration and the new root's duration; durations are not added. A normal
three-second root hitting with one second remaining resets the duration to
three seconds, without shortening an existing longer root.

Once applied, a Bear root belongs to its target and finishes independently of
owner death, logout, dimension change, or unlearning. Save its remaining duration
with the target and pause it while the target is not ticking; target death clears
it, matching bleed's lifecycle. This does not keep any trap or patch deployed.

The snap applies its root only after dealing positive damage to health or
absorption hearts. A fully prevented hit consumes the single-use trap without
rooting the target, matching Rend's damaging-hit requirement for bleed.

The snap respects armor, Dodge, general mitigation, and invulnerability, but
bypasses shield blocking under the shared trap rule. Any positive health or
absorption damage that gets through qualifies for the full root.

#### Aimed Shot
Diagram level: 5.

Requires: Piercing Shot.

Planned behavior: a short wind-up followed by one stronger, single-target arrow.
The player can adjust their aim throughout the wind-up. This trades firing speed
for concentrated damage, distinct from Piercing Shot's line attack. Movement is
allowed at the normal bow-draw slowdown and does not cancel the wind-up. Exact
wind-up duration and damage tuning are deferred.

Current prototype: 40 Focus, 4-second cooldown, 1-second wind-up, base arrow damage
coefficient 4 (double vanilla). Aim and movement remain available during wind-up;
movement speed is 20%. Removing the bow/ammunition, unlearning, or lifecycle
interruption cancels without spending. Cost and cooldown begin when the arrow fires.

#### Hunter's Pet
Diagram level: 5.

Planned behavior: summon a fixed, skill-defined companion. One companion follows
the player and assists in combat, remaining until defeated or dismissed. Pet
survival is part of managing the skill.

Combat behavior: automatically attack enemies the player damages and retaliate
against enemies that damage the player.

Initial defeat rule: defeat starts a cooldown. Once that cooldown expires, the
player can summon the pet again. Cooldown duration is deferred.

#### Barrage
Diagram level: 5.

Planned behavior: fire a rapid sequence of arrows while the skill keybind is held
and sufficient Focus remains. Aim is adjustable throughout firing, allowing the
player to concentrate on one enemy or sweep across several. Releasing the key or
running out of sufficient Focus stops firing. There is no fixed burst duration.
Each arrow spends Focus individually when it fires. Focus regeneration pauses
while channeling Barrage and resumes when the channel ends. Movement remains
allowed at normal bow-draw speed during the channel. Firing rate, damage, and
per-arrow Focus cost are deferred.

#### Snipe
Diagram level: 10.

Planned behavior: charge while standing still, then fire one single-target shot.
Its damage bonus increases with distance, up to a cap. Remaining stationary during
the charge is required; moving cancels the charge rather than preventing movement.
Exact charge duration, distance scaling, and cap are deferred.

#### Dodge Aura
Diagram level: 10.

Planned recipients: the player and nearby allies, for both the always-on passive
and the temporary activated buff. How allies are identified is deferred from this
design pass.

Passive: increase dodge chance while the aura is assigned to a skill slot.

Active: grant each recipient a brief window in which one dodge is guaranteed.
The guarantee applies to one dodgeable incoming hit per recipient. Passive bonus,
aura range, active window duration, resource cost, and cooldown are deferred.

#### Frost Trap
Diagram level: 10.

Requires: Bear Trap.

Current implementation: when triggered, create a temporary frost patch that slows enemies
only while they are inside it. The slow does not linger after leaving the patch.
This controls groups, complementing Bear Trap's single-target root. It deals
no damage.

Implemented prototype base values: 30 Focus, 12-second cooldown,
3.5-block patch radius, eight-second patch duration, 50% movement slow, and
an instant throw. The shared post-landing arming delay still applies.

Overlapping Frost patches do not stack their slows; only the strongest Frost
slow applies. Two normal patches still slow by 50%. Leaving one patch does
not remove the slow if the target remains inside another eligible patch;
leaving all eligible patches ends it immediately.

#### Arcane Arrow
Diagram level: 15.

Planned behavior: fire a single-target arrow that deals magic damage and briefly
increases the target's damage taken from everyone, including teammates. Repeated
applications, including from multiple Stalkers, refresh one shared vulnerability
effect's duration without stacking its strength. This vulnerability stacks with
Shock Aura's damage-taken increase. Damage, vulnerability strength, and debuff
duration are deferred.

#### Poison Shot
Diagram level: 15.

Planned behavior: fire a single-target arrow that deals initial hit damage and
then applies poison damage over time. This maintains pressure while the player
repositions or prepares another attack. Initial damage, poison damage, tick
interval, and poison duration are deferred.

#### Multishot
Diagram level: 20.

Planned behavior: fire several arrows simultaneously in a horizontal fan, covering
enemies spread across the player's view. Each enemy can take damage at most once
per activation, even if multiple arrows connect (no shotgunning). Arrow count,
spread angle, and damage are deferred.

#### Flame Trap
Diagram level: 20.

Current implementation: when triggered, create a temporary burning area that repeatedly
damages enemies only while they are inside it. Leaving the patch ends its damage;
it does not ignite targets or apply a lingering burn. This rewards keeping enemies
inside with Bear and Frost traps.

Implemented prototype base values: 35 Focus, 12-second cooldown,
three-block patch radius, six-second patch duration, three damage points per
enemy every second, and an instant throw. The shared post-landing arming delay
still applies.

Overlapping Flame patches from different players each deal their own damage
with independent per-enemy intervals and owner credit. Two normal patches deal
six damage points per second before defenses to an enemy inside both. A player's
own active Flame patch is still replaced when that player's next Flame Trap triggers.

An eligible enemy takes its first Flame hit immediately on entry, including
when the patch forms around it, then at most one hit per second from that
patch. Each patch retains the per-enemy interval across leaving and re-entering;
crossing the boundary does not grant additional hits within that interval.

Flame patch damage is fire damage and bypasses Dodge and shields. Fire Resistance
and natural fire immunity prevent it. General mitigation and invulnerability
still apply.

#### Swiftness Aura
Diagram level: 25.

Planned recipients: the player and nearby allies.

Passive: increase movement speed while the aura is assigned to a skill slot.

Active: clear existing slows and grant a larger temporary movement-speed boost
plus 0.5 blocks of additional step height. Speed bonuses, aura range, active buff
duration, resource cost, and cooldown are deferred.

#### Snaring Shot
Diagram level: 25.

Requires: Poison Shot.

Planned behavior: fire a shot that applies poison damage over time plus a strong
temporary movement slow. This combines damage over time with ranged pursuit
control. Poison damage, poison duration, slow strength, and slow duration are
deferred.

#### Blast Trap
Diagram level: 25.

Requires: Flame Trap.

Current implementation: when triggered, produce one TNT-like explosion that damages
nearby enemies and knocks them away without destroying blocks. This provides
immediate area burst damage and creates space, though knockback can push enemies
out of Frost or Flame patches.

Damage and knockback are strongest at the explosion's center and fall toward
zero at its outer edge. Placement close to the enemy therefore improves both
burst damage and displacement.

Implemented prototype base values: 40 Focus, 15-second cooldown,
3.5-block explosion radius, maximum center damage of ten points (five hearts
before defenses), and an instant throw. Damage falls off with distance as above;
the shared post-landing arming delay still applies.

Blast primarily shoves enemies outward with a small upward bump, rather than
launching them high. The initial playtest target is roughly three blocks of
displacement near the center on flat ground before knockback resistance.
Resistance reduces the knockback, and strength falls off with distance.
Terrain affects actual travel; this is a tuning target, not a guaranteed distance.

Blast deals explosion damage and bypasses Dodge and shields. Armor and Blast
Protection reduce its damage; general mitigation and invulnerability still apply.

#### Meteor Arrow
Diagram level: 30.

Requires: Arcane Arrow.

Planned behavior: a spell with a cast time that uses an instant-shot arrow to
designate whatever it connects with as the target of a meteor. The meteor lands
on that target shortly afterward, following it if the arrow marked a moving
enemy. The meteor deals area damage around its impact. Cast duration, meteor
delay, impact radius, and damage are deferred.

The impact ignites affected enemies only; it leaves no burning ground area. It
does not apply Arcane Arrow's vulnerability. Burn duration is deferred.

#### Ballista
Diagram level: 30.

Planned behavior: deploy a temporary stationary turret that automatically fires
heavy bolts at nearby enemies. This provides position-holding ranged support.
Enemies cannot select the Ballista as an attack target, but it can still take
incidental damage, such as from Wither or Ghast explosions and similar effects.
Each player may have two active Ballistas. Deploying a third removes the oldest,
keeping the two most recently deployed.
Health, duration, firing rate, targeting range, and bolt damage are deferred.

#### Volley
Diagram level: 30.

Planned behavior: rain arrows vertically across a broad spread over the chosen
ground area. The total skill area has a 6-block radius. Each arrow deals area
damage on impact within a 2-block diameter (1-block radius). Arrow landing
positions are random within the overall skill area. Enemies can take repeated
damage from overlapping impacts, subject to a short per-enemy interval between
damaging hits. Arrow count, rain duration, damage, and hit interval are deferred.

One activation starts the rain for a fixed duration; holding the key is not
required. The player can reposition and use other skills while the arrows fall.

## Reaver

#### Cleave
Diagram level: 1.

Planned behavior: a wide forward-facing melee sweep, rewarding facing and
positioning. Sweep angle and damage tuning are deferred.

Current prototype: deal effective native attack damage to non-allied living targets
within 3 blocks, line of sight, and a 120-degree horizontal forward arc. Vertical
aim does not rotate the arc. Cost remains 20 Rage and cooldown 3 seconds.

#### Rend
Diagram level: 3.

Planned behavior: a single-target melee strike that deals an initial hit and
applies bleeding damage over time using the shared bleed rules above.

Current prototype: instant attack against the enemy under the crosshair, using
the held item's native attack range and block-clipped targeting (normally 3 blocks
in Survival). The initial strike deals 100% of effective native attack damage.
A damaging hit applies one five-second stack at 10% of captured attack damage per
second. Cost: 20 Rage; cooldown: 2 seconds. Existing cost, cooldown, and duration
multipliers apply. No valid target spends nothing and starts no cooldown. Once a
strike is attempted, its cost and cooldown remain spent even if defenses stop the
damage and prevent bleed application. No weapon-category restriction is added.

#### Battle Cry
Diagram level: 3.

Planned behavior: an active shout that briefly increases all outgoing damage for
the player and nearby allies, including spells and damage over time. This creates
a shared offensive window. Buff strength, range, duration, resource cost, and
cooldown are deferred. Ally identification remains outside this design pass.

#### Rage Storm
Diagram level: 5.

Requires: Cleave.

Planned behavior: a spell-like cast with a wind-up, followed by an area of effect
released from the player that slowly moves forward. It travels in a fixed
direction set when the cast completes, allowing the player to reposition
independently after release. Enemies take repeated damage at short intervals
while they remain inside the moving area. Wind-up duration, movement speed, area
size, travel distance, damage, and hit interval are deferred.

#### Execute
Diagram level: 5.

Planned behavior: a heavy single-target melee attack usable only when the enemy
is below a health threshold. Health threshold, damage, resource cost, and cooldown
are deferred.

#### Blood Fever
Diagram level: 5.

Planned behavior: grant the player movement-speed and attack-speed bonuses based
on the number of nearby bleeding enemies. While the buff is active, its strength
updates as that count changes, including enemies entering or leaving range and
dying.

Detection uses a 4-block radius with periodic local checks. Tick cost must be
measured during implementation. Update interval and bonuses per enemy are
deferred.

#### Leap
Diagram level: 10.

Planned behavior: leap toward a ground point the player aims at, dealing area
damage on landing. No enemy target is required. Leap range, impact radius,
damage, resource cost, and cooldown are deferred.

#### Ripping Aura
Diagram level: 10.

Mutually exclusive with: Fortitude Aura.

Planned recipients: the player and nearby allies.

Passive: increase bleed damage while the aura is assigned to a skill slot.

Active: temporarily make recipients' melee hits apply bleeding. Passive bonus,
aura range, active duration, applied bleed damage and duration, resource cost, and
cooldown are deferred.

#### Fortitude Aura
Diagram level: 10.

Mutually exclusive with: Ripping Aura.

Passive: increase armor and armor toughness while the aura is assigned to a skill
slot.

Active: grant the player and nearby allies a temporary damage-absorbing shield.
Passive bonuses, aura range, shield capacity and duration, resource cost, and
cooldown are deferred.

#### Lunging Combo
Diagram level: 15.

Planned behavior: a three-activation combo.

1. First activation: perform a short lunge.
2. Second activation, within 2 seconds of the lunge: perform a single-target attack.
3. Third activation, within 3 seconds of the second activation: perform an AoE
   cleave that applies a small armor reduction.

Rage is spent separately for each stage performed. Unused follow-ups cost nothing;
the player must retain enough Rage to perform later stages.

One shared cooldown begins when the final cleave completes or a follow-up window
expires. Letting the combo lapse after the lunge or second attack still starts
that cooldown.

Lunge distance, damage, cleave area, armor-reduction strength and duration,
per-stage Rage costs, and cooldown duration are deferred.

#### Bloody Cloak
Diagram level: 15.

Planned behavior: a temporary self-buff that heals the player for a portion of
the bleed damage they deal. Activation clears existing Wither, ignite/burning,
and poison. Immunity prevents those effects from being reapplied while the buff
is active. Healing fraction, buff duration, resource cost, and cooldown are
deferred.

#### Overpower
Diagram level: 20.

Planned behavior: a single-target melee hit that deals normal damage to bleeding
enemies and gains a huge damage bonus against non-bleeding enemies. This supports
a non-bleed Reaver build alongside the class's bleed-focused options, while
remaining usable when a teammate applies bleed. Damage, resource cost, and
cooldown are deferred.

#### Consume
Diagram level: 20.

Planned behavior: an area skill that removes bleeds from affected enemies. Each
affected enemy takes a flat damage amount per bleed instance removed from it.
Guillotine is the single-target counterpart. Area size, flat damage per instance,
resource cost, and cooldown are deferred.

#### Enduring Cry
Diagram level: 20.

Requires: Battle Cry.

Planned behavior: an active shout that briefly reduces damage taken for the player
and nearby allies. This provides a defensive window to time before dangerous
attacks. Damage reduction, range, duration, resource cost, and cooldown are
deferred. Ally identification remains outside this design pass.

#### Skull Bash
Diagram level: 25.

Planned behavior: a single-target melee hit that briefly stuns the enemy,
preventing movement and attacks. Damage, stun duration, resource cost, and
cooldown are deferred.

#### Raging Aura
Diagram level: 25.

Passive: while assigned to a skill slot, reduce the player's out-of-combat Rage
decay rate after the existing grace period. The passive does not extend that
grace period.

Active: temporarily increase damage and stop passive Rage decay. Skills still
spend Rage normally during the buff. Passive sustain tuning, damage bonus, active
duration, resource cost, and cooldown are deferred.

#### War Banner
Diagram level: 25.

Planned behavior: plant a temporary stationary banner that grants health
regeneration and prevents hunger loss for the player and nearby allies within
its radius. Regeneration strength, range, duration, resource cost, and cooldown
are deferred. Ally identification remains outside this design pass.

#### Rip and Rush
Diagram level: 30.

Requires: Lunging Combo.

Planned behavior: a three-activation combo.

1. First activation: apply Rend in a circular area around the player.
2. Second activation, within 2 seconds of the first: dash forward through enemies,
   applying armor shred to those passed through.
3. Third activation, within 3 seconds of the second: perform another circular AoE
   around the player's current position, consuming bleed on affected enemies to
   grant a temporary movement-speed bonus.

Rage is spent separately for each stage performed; unused follow-ups cost nothing.
One cooldown for this combo begins when the final stage completes or a follow-up
window expires, matching Lunging Combo's timing and spending rules.

The finisher's movement-speed bonus scales with the number of bleeding enemies
whose bleed is consumed, up to a cap. Consuming bleeds can reduce Blood Fever's
count of nearby bleeding enemies.
Area sizes, dash distance, damage, armor-shred strength and duration, movement-speed
bonus per enemy, bonus cap and duration, per-stage Rage costs, and cooldown duration
are deferred.

#### Guillotine
Diagram level: 30.

Planned behavior: remove bleeds from one target, dealing a flat damage amount per
bleed instance removed, as the single-target counterpart to Consume. Flat damage
per instance, range, resource cost, and cooldown are deferred.

#### Demon Mode
Diagram level: 30.

Planned behavior: while this temporary mode is active, attacking teleports the
player to the enemy under their crosshair, up to 5 blocks away. The player heals
each time they strike an enemy or one of their bleeds deals damage. Healing is a
fixed amount per trigger, with separate amounts for strikes and bleed ticks.
Healing amounts, mode duration, resource cost, and cooldown are deferred.

## Weaver

#### Arcane Bolt
Diagram level: 1.

Fire a straight amethyst-rendered projectile; one collision deals 6 attributed indirect-magic damage.

#### Lightning Bolt
Diagram level: 3.

Planned behavior: once cast, strike instantly along the player's aim, stopping at
the first enemy or solid block. Cast time, range, damage, Mana cost, and cooldown
are deferred.

#### Mage Armour
Diagram level: 3.

Planned behavior: a temporary self-buff that increases armor, armor toughness,
and knockback resistance, paid for with an upfront Mana cost. Stat bonuses,
duration, Mana cost, and cooldown are deferred.

#### Arcane Barrage
Diagram level: 5.

Requires: Arcane Bolt.

Planned behavior: hold the skill keybind to repeatedly fire arcane missiles while
sufficient Mana remains. Missiles corkscrew around the aimed direction, widening
as they travel, and each has a 0.5-block-radius AoE. Aim is adjustable throughout
the channel. Releasing the key or running out of sufficient Mana stops firing.
This uses the same input pattern as Stalker's Barrage. Each missile spends Mana
individually when it fires.
Mana regeneration pauses during the channel and resumes when it ends. Movement
is allowed at normal bow-draw speed while channeling, matching Stalker's Barrage.
Firing rate, missile damage, and per-missile Mana cost are deferred.

#### Ice Bolt
Diagram level: 5.

Planned behavior: fire a traveling single-target projectile that deals damage and
briefly slows the enemy's movement. Damage, slow strength and duration, cast time,
Mana cost, and cooldown are deferred.

#### Discharge
Diagram level: 5.

Planned behavior: store stacks from previously cast ice, fire, and lightning
spells, with a separate counter capped at 5 for each element. Activating Discharge
deals damage and applies effects in an AoE centered on the player, based on the
stored stack counts and element types. All stored stacks are consumed when the
cast completes. Damage values, area size, Mana cost, and cooldown are deferred.

Ice contribution: add a movement slow, upgraded to a brief freeze when all
5 ice stacks are consumed. Slow strength and duration and freeze duration are
deferred.

Fire contribution: ignite affected enemies. Consuming 5 fire stacks also adds one
explosion at Discharge's center. Burn duration and explosion damage are deferred.

Lightning contribution: add lightning damage. Consuming 5 lightning stacks also
sends chain-lightning arcs beyond the main AoE. Damage scaling, chain range, and
chain target limit are deferred.

#### Fire Bolt
Diagram level: 5.

Planned behavior: fire a traveling single-target projectile that deals an initial
hit and ignites the enemy. Initial damage, burn duration, cast time, Mana cost,
and cooldown are deferred.

#### Icy Veins
Diagram level: 10.

Uses the shared aura model despite its name.

Passive: reduce skill cooldowns while assigned to a skill slot.

Active: make all casts instant for a limited period by removing spell wind-ups.
Channel firing rates, including Arcane Barrage's missile rate, remain unchanged.
Passive cooldown reduction, active duration, Mana cost, and activation cooldown
are deferred.

#### Flame Cloak
Diagram level: 10.

Uses the shared aura model.

Passive: reduce incoming fire damage while assigned to a skill slot.

Active: temporarily ignite enemies that hit the player in melee. Passive damage
reduction, retaliation duration, ignite duration, Mana cost, and cooldown are
deferred.

#### Teleport
Diagram level: 10.

Planned behavior: blink along the player's aim, stopping at a clear position
before solid obstacles. No enemy target is required. Range, Mana cost, and
cooldown are deferred.

#### Freezing Aura
Diagram level: 15.

Requires: Icy Veins.

Passive: slow nearby enemies while assigned to a skill slot.

Active: briefly freeze nearby enemies, preventing movement and attacks. Aura
range, passive slow strength, freeze duration, Mana cost, and cooldown are
deferred.

#### Shock Aura
Diagram level: 15.

Passive: increase nearby enemies' damage taken while assigned to a skill slot.
This increase stacks with Arcane Arrow's vulnerability. Overlapping Shock Auras
do not stack with one another; only the strongest copy applies.

Active: temporarily make Lightning Bolt chain between nearby enemies.
Vulnerability strength, aura range, active duration, chain range and target limit,
Mana cost, and cooldown are deferred.

#### Immolation Aura
Diagram level: 15.

Requires: Flame Cloak.

Passive: damage nearby enemies with fire while assigned to a skill slot.

Active: temporarily boost all fire damage the player deals. Aura range, passive
damage and interval, active damage bonus and duration, Mana cost, and cooldown
are deferred.

#### Threefold System
Diagram level: 15.

Planned design direction: combine the ideas behind Path of Exile's Elemental
Overload and Elemental Equilibrium. See
[Grinding Gear Games' explanation of both keystones](https://www.pathofexile.com/forum/view-thread/3185299).

Passive: store elemental power based on other elemental types the player has
used recently. Fire builds power for ice and lightning; ice builds power for fire
and lightning; lightning builds power for fire and ice.

Stored power is a fraction of elemental damage actually dealt, with capped
storage. Only damage dealt within the last 10 seconds contributes; older
contributions expire individually as they leave that rolling window.

Active: consume all stored power at once to grant a fixed-duration damage buff.
Each element receives a percentage damage bonus based on how full its reserve
is, up to the maximum bonus at full storage. Bonuses are set when activated and
remain fixed for that buff's duration. Banking pauses while the buff is active
and resumes after it ends.

Stored damage fraction, storage cap, boost strength and duration, Mana cost, and
cooldown are deferred.

#### Ice Tomb
Diagram level: 20.

Planned behavior: encase the player in ice, granting temporary invulnerability
while preventing movement, attacks, and casting. While active, the tomb slows
nearby enemies and damages them over time. Pressing the skill keybind again
releases the tomb early; the original cooldown still applies. Tomb duration,
area radius, slow strength, damage and tick interval, Mana cost, and cooldown
are deferred.

#### Firestorm
Diagram level: 20.

Planned behavior: one cast starts a fixed-duration rain of fiery impacts over an
aimed ground area, igniting enemies hit. The player can move and cast other spells
while the storm runs. Area size, impact damage and frequency, burn duration,
storm duration, Mana cost, and cooldown are deferred.

#### Weaver's Web
Diagram level: 20.

Planned behavior: place a temporary web field at an aimed location. Enemies are
briefly rooted when they enter, preventing movement while still allowing attacks.
Placement range, field size and duration, root duration, Mana cost, and cooldown
are deferred.

#### Enchanting Aura
Diagram level: 25.

Planned recipients: the player and nearby allies.

Passive: increase Mana regeneration while assigned to a skill slot.

Active: temporarily add magic damage to regular melee attacks and ordinary bow
and crossbow shots. Skills do not receive this added damage. Regeneration bonus,
aura range, added magic damage and buff duration, Mana cost, and cooldown are
deferred.

#### Beacon
Diagram level: 25.

Planned behavior: mark the player's current position with a temporary beacon.
A second activation teleports the player back to it, with one return allowed per
beacon. Staying close to the beacon reduces Mana costs. Returning leaves that
cost-reduction area active until the beacon's duration expires. Beacon lifetime,
cost-reduction radius and strength, return range, Mana cost, and cooldown are
deferred.

#### Elemental Golem
Diagram level: 25.

Planned behavior: the golem has 4 forms: fire, ice, lightning, and arcane. Its form
is selected according to the player's last cast. Each form grants a different
benefit while nearby. Proximity range, benefit strengths, Mana cost, and cooldown
are deferred.

Companion rules match Hunter's Pet: one lasting golem follows and assists the
player, remaining until defeated or dismissed. It attacks enemies the player
damages and retaliates against enemies that damage the player. Defeat starts
its resummon cooldown; it can be summoned again after that cooldown expires.

| Form | Benefit to the player while nearby |
| --- | --- |
| Fire | Increase all spell damage. |
| Ice | Increase armor and armor toughness. |
| Lightning | Reduce spell cast times and extend the duration of the player's spell effects. |
| Arcane | Increase Mana regeneration and reduce Mana costs. |

#### Arcane Explosion
Diagram level: 30.

Planned behavior: after a wind-up, release one large magic-damage burst centered
on the player. No elemental charges are required. Wind-up duration, area radius,
damage, Mana cost, and cooldown are deferred.

#### Blizzard
Diagram level: 30.

Planned behavior: one cast starts a fixed-duration storm over an aimed area,
dealing cold damage over time and slowing enemies. Enemies remaining inside
long enough are briefly frozen. The storm continues after casting. Area size,
damage and tick interval, slow strength, exposure needed to freeze, freeze
duration, storm duration, Mana cost, and cooldown are deferred.

#### Scorching Ray
Diagram level: 30.

Planned behavior: hold the skill keybind to channel an aimable beam that pierces
enemies but stops at solid blocks, hitting aligned enemies within its range. It
deals fire damage over time, ramping up while kept on a target. The player must
stand still while channeling; moving cancels the beam, while adjusting aim is
allowed. Mana is spent per channel tick, and Mana
regeneration pauses until the channel ends. Releasing the key or having
insufficient Mana stops the beam. Beam range and width, damage, ramp rate and
cap, Mana cost per tick, and cooldown are deferred.
