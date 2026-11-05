![[Pasted image 20261104154316.png]]

The image is the proposed skill roster and level layout. Only Piercing Shot,
Cleave, and Arcane Bolt are currently implemented; the remaining entries are
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

## Planned trap rules

Bear Trap, Frost Trap, Flame Trap, and Blast Trap share a short forward throw,
landing slightly in front of the player. Exact throw distance is deferred.

Each player may have one armed trap of each type. Throwing another trap of the
same type replaces that player's previous armed trap of that type. Different
trap types can coexist.

## Planned aura model

Auras provide an always-on passive effect while assigned to a skill slot. Learning
an aura alone does not activate it; removing it from the loadout ends its passive.
Pressing its assigned keybind activates a different, stronger temporary buff. Each
aura's passive and active effects will be defined individually. This model applies
to auras across the classes and is not implemented yet.

## Planned bleed stacking

Repeated bleed applications add separate stacks, up to 3 stacks per enemy. Each
stack counts as one bleed instance for Consume and Guillotine's flat consumption
damage. Blood Fever counts bleeding enemies, not the number of stacks on them.
Reapplying bleed refreshes the duration of all existing stacks, including when
already at the 3-stack cap. Bleed stacking is not implemented yet.

# Skill Details
## Stalker

#### Piercing Shot
Diagram level: 1.

Planned behavior: fire one arrow that can hit up to three enemies in a line,
stopping at blocks. This gives the skill a role in damaging lined-up enemies.
Damage tuning is deferred.

Current implementation: fire one bow arrow at full speed with base damage
coefficient 3 instead of vanilla 2 (+50%). It only hits one target; the planned
multi-target piercing is not implemented yet.

#### Disengage
Diagram level: 3.

Planned behavior: leap backward relative to the direction the player is facing.
No target is required. This lets the player keep aiming at an approaching enemy
while creating distance. Leap distance and other numerical tuning are deferred.

#### Bear Trap
Diagram level: 3.

Planned behavior: throw a single-use trap a short distance in front of the player.
It damages and briefly roots the first enemy to step on it. The root prevents
movement but still allows attacks. This creates an opportunity to gain distance
and prepare a shot. Damage and root duration are deferred.

#### Aimed Shot
Diagram level: 5.

Requires: Piercing Shot.

Planned behavior: a short wind-up followed by one stronger, single-target arrow.
The player can adjust their aim throughout the wind-up. This trades firing speed
for concentrated damage, distinct from Piercing Shot's line attack. Movement is
allowed at the normal bow-draw slowdown and does not cancel the wind-up. Exact
wind-up duration and damage tuning are deferred.

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

Planned behavior: when triggered, create a temporary frost patch that slows enemies
only while they are inside it. The slow does not linger after leaving the patch.
This controls groups, complementing Bear Trap's single-target root. Area size,
slow strength, and patch duration are deferred.

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

Planned behavior: when triggered, create a temporary burning area that repeatedly
damages enemies inside it. This provides sustained area damage. Area size, damage,
damage interval, and patch duration are deferred.

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

Planned behavior: when triggered, produce one TNT-like explosion that damages
nearby enemies and knocks them away without destroying blocks. This provides
immediate area burst damage and creates space, though knockback can push enemies
out of Frost or Flame patches. Explosion damage, radius, and knockback strength
are deferred.

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

Current implementation: deal effective native attack damage to nearby non-allied
living targets in every direction within 3 blocks and line of sight. The planned
frontal restriction is not implemented yet.

#### Rend
Diagram level: 3.

Planned behavior: a single-target melee strike that deals an initial hit and
applies bleeding damage over time. Initial damage, bleed damage, tick interval,
and bleed duration are deferred.

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
