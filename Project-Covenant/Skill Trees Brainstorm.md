![[Pasted image 20261104154316.png]]

The image is the proposed skill roster and level layout. The levels transcribed
below are the minimum character levels required to invest the first point in each
skill; they are real unlock gates, not informational labels. Once a skill is
unlocked at Rank 1, further ranks do not require additional character levels.
Blood Fever has no skill prerequisite; the original image's Rend connection is
superseded by the current tree and the entry below.
Piercing Shot, Aimed Shot, Cleave, Rend, Arcane Bolt, all four Stalker traps, all
three elemental bolts, all eleven auras, Lunging Combo, Rip and Rush,
Disengage, Arcane Arrow, and Multishot are
implemented; the remaining entries are planned skills. The breakdowns record
agreed gameplay direction and current prototype status.
Stalker's numerical baseline is now accepted in
[[Project_Covenant_Stalker_Balance_Proposal]]. Disengage, Arcane Arrow, and
Multishot are implemented with only Rank 1 purchasable and Rank 8 retained as
the planned maximum. Higher-rank upgrades remain deferred until Rank 1
playtesting. Existing Stalker tuning and server-datapack skill configuration
were not changed in this batch; current behavior is in `docs/RPG_SKILLS.md`.
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

## Skill progression rules

Character level is capped at 60. A new character starts at level 1 with one skill
point, then earns one additional skill point per character level, for 60 lifetime
points at level 60. The first point invested in a skill unlocks it at Rank 1. Each
additional point increases its rank; each skill will have an individual maximum
rank of either 8 or 12, depending on its meaningful upgrade potential. For the
initial playable roster, Lunging Combo and Rip and Rush cap at 12; all other
currently playable combat skills cap at 8. Unfinished skills' eventual caps and
individual per-rank effects remain to be defined. See [[Combat Skill Progression]].

For the planned progression slice, existing prototype behavior and values become
Rank 1. Further ranks provide minor, flavorful, skill-specific improvements;
avoid flat damage per rank or routine damage scaling. Review the total benefit
at maximum rank to preserve vanilla balance. See [[Combat Skill Progression]]
for the agreed rank-design rules and [[Progression Vertical Slice]] for the
existing-skills-only content boundary.

For the planned aura rank tables, passive stat/effect strengths stay at Rank 1
values across ranks. Put modest improvements into activation or coverage where
appropriate, preserving the existing overlap rules. See
[[Combat Skill Progression]] for the agreed aura rank boundary.

For prototype arrow-shot ranks, use arrow velocity as the default improvement
for most arrow shots, including currently playable Piercing Shot and Aimed Shot.
Each extra rank adds 2% of Rank 1 launch velocity, additively, for +14% at Rank 8.
Retain native speed-based arrow damage, including the resulting indirect damage
growth. More varied rank progression is deferred. Aimed Shot's wind-up movement
remains at its Rank 1 baseline; see [[Combat Skill Progression]].

No more than eight distinct skills may have points allocated at once, matching
the eight combat keybinds. A skill with any points invested counts toward this
limit, including prerequisites. Vertical connections are prerequisites; both the
prerequisite and dependent skill remain usable. Crossed horizontal connections
mark genuinely mutually exclusive choices. Prerequisites and exclusions continue
to be validated by the server.

The current prototype uses free allocation for testing. The intended progression
economy replaces free learning with pending skill-point investment: left-click
adds a pending rank, right-click removes a pending rank without refunding
committed ranks, and a simple Apply button at the bottom right commits the
validated draft on the server. Pending skills grant no effects or binding access
before Apply succeeds. See [[Combat Skill Progression]]. A crafted Combat
Respec item resets combat skill allocations and refunds invested points. Its
prototype recipe is four paper plus one lapis lazuli, shapeless at a vanilla
crafting table, producing one item; see [[Combat Skill Progression]].

See [[Character Progression]] and [[Combat Skill Progression]].

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

## Aura model

Auras provide an always-on passive effect while assigned to a skill slot. Learning
an aura alone does not activate it; removing it from the loadout ends its passive.
Pressing its assigned keybind activates a different, stronger temporary effect.
The individual breakdowns below define each aura's prototype tuning. This model applies
to all eleven implemented auras across the classes.
See [Auras implementation plan](../docs/AURAS_IMPLEMENTATION_PLAN.md) for the
ordered engineering checklist and acceptance criteria.

All distinct auras assigned to the current class's skill slots provide their
passives simultaneously. There is no separate aura limit beyond the eight-slot
loadout and existing learning exclusions. Assigning the same aura to multiple
slots provides no additional passive benefit.

For effects described as benefiting the player and nearby allies, eligible
recipients are the caster, players on the caster's Minecraft scoreboard team,
and pets owned by the caster or those teammates. An unteamed caster benefits
only themselves and their own pets. This does not expand explicitly self-only
aura effects to allies.

Future compatibility must also recognize FTB chunk teams for aura allies.
That integration is deferred; the initial implementation uses scoreboard teams
and pet ownership without an FTB dependency. Keep aura ally resolution shared
so future team support applies consistently across these effects.

Team changes do not revoke existing aura buffs. Recipients who cease to be
allies stop receiving passive refreshes and new allied activations; their existing
passive coverage and timed active buffs expire naturally on their own timers.
Previously agreed caster and recipient lifecycle cleanup rules still apply.

Range-based passive benefits and debuffs are refreshed with a fixed one-second
(20-game-tick) duration while eligible recipients are inside the aura's range.
Leaving range stops refreshes rather than immediately removing the effect; the
remaining duration provides a brief grace period, especially during fast movement.
Refresh coverage every 0.5 seconds (10 game ticks), providing approximately
0.5–1 second of remaining coverage after leaving range at normal game speed.
This fixed coverage duration and refresh frequency are separate from damage-pulse
intervals, defined per effect below. Explicit caster cleanup events below still
remove that source's effects immediately. Allied activated buffs select
eligible recipients in range when the activation takes effect. Those recipients
retain the buff for its remaining duration after leaving range. Entering range
afterward does not grant that activation's buff.

Overlapping copies of the same aura passive do not stack; only the strongest
applicable copy affects a recipient or enemy. Other eligible sources provide
coverage when the strongest source no longer applies. Different aura passives
can coexist. This extends Shock Aura's strongest-copy rule to all aura passives.

Repeated copies of the same activated aura buff do not stack. Each cast retains
its own expiry, and only the strongest unexpired applicable copy provides the
effect. A later cast can therefore provide coverage after an earlier cast expires,
without adding their strengths or extending the earlier cast's timer. Fortitude
shield capacities from different casts do not add together. Dodge's shared
guarantee consumption and Fortitude's replacement/refresh rule are explicit
exceptions defined in their individual breakdowns.

An assigned aura's passive remains in effect during its activated effect and
throughout its activation cooldown, subject to the normal recipient and range
rules. Activation adds its temporary effect without suspending the passive.

Removing an aura's last slot assignment immediately ends that caster's passive
and active aura effects, including active buffs supplied to allies, without
resetting its cooldown. Removing a duplicate assignment has no effect while
another current-class slot still holds that aura. This prevents retaining active
aura buffs while replacing their slot commitments with other skills.

Caster death, logout, dimension change, class change, or unlearning the aura
clears that caster's supplied passive and active aura effects, including effects
on allies, without resetting cooldowns. Passives resume when the caster becomes
eligible again with the aura assigned; cleared active effects require a new
activation. Effects from other eligible casters are unaffected.

A recipient's death, logout, or dimension change clears aura buffs and Fortitude
shield capacity on that recipient. These temporary effects do not survive
respawn, reconnect, or dimension travel. Passive coverage can be received again
normally when eligible and in range. This does not alter the separately saved
bleed contract or committed projectile bonuses described below.

Aura passives have no resource upkeep and continue working at zero class
resource. The occupied skill slot is their ongoing commitment. Activating an
aura spends a one-time cost in the caster's class resource (Focus, Rage, or Mana)
and uses that aura's own cooldown. Individual costs and cooldowns are defined
below; there is no recurring resource drain during the activated effect.

All aura activations are instant, with no wind-up. A successful activation
immediately spends its one-time resource cost and starts its cooldown. There is
no aura cast window to interrupt. Rejected activations spend nothing and do not
start a cooldown.

Aura activation is rejected while the caster is already casting another skill,
following the existing cast pipeline. It does not interrupt or bypass that cast.
Aura passives continue to operate during other casts; activation must wait until
the current cast finishes or is cancelled.

Nearby aura effects use a sphere centered on the caster, with distance measured
between entity positions. Range reaches equally horizontally and vertically.
All nearby aura effects start with a six-block radius as shared prototype tuning;
individual overrides can be agreed when defining each aura. Allied aura benefits ignore solid cover:
walls and floors do not block passive coverage or selection for activated buffs.
Range and ally eligibility still apply. This avoids line-of-sight checks for
allied coverage and keeps buffs reliable around buildings; no performance
benchmark is implied. Enemy-facing aura effects also ignore walls and floors,
including Immolation damage and Freezing/Shock effects. They still require range
and enemy eligibility. Auras can therefore affect enemies in adjacent rooms or
on other floors within their spherical range. Aura coverage and area activation
selection require no line-of-sight checks; this does not change cover rules for
separate attacks such as Lightning Bolt or its future chain arcs.

Offensive aura effects use normal skill enemy eligibility: living, non-spectator
targets other than the caster, excluding aura allies and their pets and respecting
PvP restrictions. Passive animals and modded living creatures can be affected;
targets are not restricted to hostile mobs. Shared aura ally resolution applies
to these exclusions, including future FTB chunk-team support. An assigned
Immolation Aura can therefore damage eligible livestock within range through
walls and floors.

Raging and Immolation damage bonuses use each effect's normal damage-calculation
point. Do not move existing calculations earlier or later solely for auras, or
introduce a universal impact-time recalculation rule. Preserve the explicit bleed
application snapshots and Enchanting projectile-at-firing snapshots below.

Damage absorbed by Fortitude counts as landed combat damage for Rage gains,
combat grace, and bleed application, like ordinary absorption. Preserve the
existing restrictions on Rage generation: skill damage grants no outgoing Rage,
and self/environmental damage does not become combat damage. Count damage
absorbed by Fortitude only once; do not also count it as health damage. This is a
planned extension to current health/absorption-based hit confirmation.

## Bleed stacking

Repeated bleed applications add separate stacks, up to 3 stacks per enemy. Each
stack counts as one bleed instance for Consume and Guillotine's flat consumption
damage. Blood Fever counts bleeding enemies, not the number of stacks on them.
Reapplying bleed refreshes the duration of all existing stacks, including when
already at the 3-stack cap. Rend, the shared bleed foundation, Rip and Rush and
Blood Fever are implemented; the other dependent bleed skills remain planned.

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

## Combo activation model

Lunging Combo and Rip and Rush are implemented three-stage skills. Each uses one
assigned skill key: each fresh press advances that skill's fixed sequence, with
no stage selection or skipping. Holding the key does not advance stages.
The logical server determines the current stage; the client only requests
activation. See the [combo implementation plan](../docs/COMBOS_IMPLEMENTATION_PLAN.md)
for the agreed shared foundation and ordered implementation checklist. Implemented
prototype effects and tuning are recorded in the skill details below.

Capture the effective cooldown duration when the first stage is accepted and
starts, including the existing cooldown multiplier. Completion, timeout, and
cancellation use that captured duration, counting from the combo's actual end.
Later equipment or modifier changes do not recalculate it. Rejected starts
capture nothing and start no cooldown.

Each stage uses current equipment and stats for its Rage cost and damage when
accepted and begun. Buffs or weapon changes between stages can affect later
costs and damage, and each stage checks its own resource requirement. Already
committed effects retain their existing snapshots, including bleed stacks.
The combo's captured cooldown duration does not change.

Ordinary attacks and other non-combo skills preserve an open combo, but its
follow-up timer continues running without pausing or refreshing. Normal skill
activation restrictions still apply. Ordinary attacks can generate Rage for
later stages under the existing resource rules.

Ordinary attacks and other skills' stationary stages remain usable during active
lunge/dash movement, subject to normal activation checks. For example, Rip and
Rush's circular Rend may execute during Lunging Combo's lunge. The moving combo's
own next stage must still wait until its movement finishes.

Different combos may interleave. Each skill keeps independent progression,
follow-up deadlines, and cooldowns while drawing from the same Rage balance.
Using one does not pause or refresh the other. Duplicate slot assignments of
the same combo share its progression rather than opening extra sequences.

Rejected follow-ups spend nothing and preserve progression until the original
deadline. Players may retry within that window; a rejection neither refreshes
the timer nor independently starts a cooldown. Expiry still starts the combo's
cooldown normally.

A performed stage spends its Rage and advances the sequence even if blocking,
Dodge, or invulnerability prevents damage. Later stages do not require earlier
stages to land damaging hits. Secondary effects follow their own hit/defense
requirements; advancement alone does not guarantee their application.

Freeze prevents stage activation but does not cancel open combos or pause or
refresh their follow-up windows. Players may continue after thawing if time
remains; windows that expire during freeze start their cooldowns normally.
Existing interruption of pending Covenant casts by freeze remains unchanged.

Root blocks starting Lunging Combo's lunge and Rip and Rush's dash, while
stationary attack stages remain usable under normal checks. Rejected movement
stages spend nothing and cannot be skipped. An existing follow-up window keeps
its original deadline, allowing a retry after root ends if time remains. Root
does not itself cancel combos or pause their timers; a rejected initial lunge
opens no combo and starts no cooldown.

Skill-driven lunges and dashes execute one at a time per player. A conflicting
movement-stage request spends nothing, does not advance or queue a stage, and
leaves the current movement and existing deadlines unchanged. Players must press
again after movement finishes if time remains. Rejecting an initial movement
stage opens no combo and starts no cooldown.

Each next stage becomes available when the preceding stage finishes. Stage-one
completion opens the full two-second follow-up window; stage-two completion
opens the full three-second window. Instant attacks complete immediately, while
a lunge or dash must finish before its next stage becomes available. Early
requests for that combo's next stage are rejected without spending or queueing.
Other combos' existing windows continue running independently.

The follow-up windows are fixed at 40 and 60 game ticks (two and three seconds
at normal game speed). Skill-duration bonuses do not scale them; stage-applied
status and buff durations follow their own separate rules.

Normalize the captured launch direction once. Floor/ceiling contact flattens pitch
at the current speed while preserving horizontal heading. Wall contact clips
blocked horizontal components without renormalizing the remainder: blocked forward
travel cannot become a full-speed left/right slide. Glancing wall slides retain
their original tangential speed. Flatten pitch before clipping walls at simultaneous
contacts. Use only the remaining fraction of the current movement tick to preserve
the travel budget. A direct impact with no surviving tangent stops
movement early and completes the stage, retaining Rage spent and opening the
full next-stage window. No minimum travel distance is required, and movement
cannot pass through solid terrain.

Lunges and dashes follow the player's full 3D look direction captured at the
start of the movement stage, including upward and downward aim. Turning during
movement does not steer or retarget it. Floors, walls, and ceilings redirect
travel along their surfaces; full blockage stops it. Each stage has fixed distance and travel time, unaffected by movement-speed
or skill-duration bonuses; collisions and interruptions can still shorten travel.
Per-skill base values are specified in the skill details below where agreed.
Suspend gravity during the brief movement
so open-space travel follows the captured direction, with frictionless surface sliding on contact. Restore normal gravity when movement ends,
including an early terrain stop; follow-up windows do not suspend gravity.
The skill's movement momentum ends with the lunge/dash, including an early terrain
stop. There is no continued coasting from dash propulsion; an upward dash stops
propelling the player upward and normal falling resumes.

Starting a lunge/dash replaces existing velocity rather than adding to it, and
does not restore the old velocity afterward. Activation can cancel existing
falling or knockback momentum; new incoming knockback during movement follows
the interruption rule below.

Lunges and dashes break falls: clear accumulated fall distance at movement start
and throughout the movement stage, including a movement-ending landing. Normal
fall tracking resumes when movement ends, including any early stop. Subsequent
falling can hurt normally; follow-up windows provide no fall protection.

Incoming knockback that takes effect ends active lunge/dash movement early and
applies normally. Keep Rage spent, restore normal gravity, and open the full
next-stage window at interruption. This preserves the combo; ending dash momentum
must not erase the incoming knockback.

Gaining root or freeze during a lunge/dash stops skill movement early, restores
normal gravity, and counts as stage completion with Rage spent. Open the full
next-stage window without cancelling the combo. Freeze blocks activation until
thawing; root still allows stationary attacks. Windows continue running during
these effects, and other combos' existing deadlines do not change.

Death, logout, dimension travel, or changing class ends every unfinished combo
and immediately starts its own cooldown, without refunding spent Rage or charging
unused stages. Stop active skill movement and restore normal gravity; do not open
another follow-up. Progress and movement are transient and do not survive these
transitions. Cooldowns retain the existing persistent skill-state behavior.

Unlearning an unfinished combo ends it and starts its cooldown with no refund
or charge for unused stages. Stop its active skill movement without opening
another follow-up. This also applies to Rip and Rush when removing its Lunging
Combo prerequisite; other still-learned combos remain intact. Relearning retains
the cooldown, and unlearning an idle combo creates no new cooldown.

Changing or clearing a slot assigned to an unfinished combo cancels it and starts
its cooldown, even if a duplicate assignment remains. Keep spent Rage, stop its
movement, restore normal gravity, and open no further follow-up. Reassigning does
not reset the cooldown; other combos with unchanged bindings remain intact.
Rejected or unchanged assignments do not cancel progress, and editing an idle
combo's binding creates no new cooldown.

Binding-change cancellation is limited to Covenant's own skill-slot GUI and
existing server assignment checks. Physical key remaps in Minecraft Controls
do not affect combo progress; no Controls monitoring or remap packet is needed.

Prototype feedback uses one compact HUD text line per open combo showing the
skill name, next stage, and remaining follow-up time. Interleaved combos have
separate lines; executing movement shows its current action until its follow-up
window opens. Ended combos disappear from this display. Icons with circular
cooldown countdowns are the intended later presentation, outside this foundation.

# Skill Details
## Stalker

#### Piercing Shot
Minimum character level: 1.

Planned behavior: fire one arrow that can hit up to three enemies in a line,
stopping at blocks. This gives the skill a role in damaging lined-up enemies.
Damage tuning is deferred.

Current implementation: fire one bow arrow at full speed through up to three
targets, stopping at blocks, with base damage coefficient 3 instead of vanilla 2
(+50%). This uses native arrow piercing and collision handling.

#### Disengage
Minimum character level: 3.

Current implementation: leap backward relative to the direction the player is facing.
No target is required. This lets the player keep aiming at an approaching enemy
while creating distance. Accepted Rank 1 baseline: 20 Focus, six-second cooldown,
four-block backward displacement target, instant activation, and no damage.
Capture horizontal facing on activation and leap backward with a small upward
hop, ignoring camera pitch. This is the accepted initial movement direction.
Activation is allowed while airborne, including after jumping or enemy knockback;
standing on the ground is not required.
The leap breaks an existing fall, clearing accumulated fall distance during
travel and on completion or interruption, as Reaver's dashes do. Normal falling
resumes afterward; a subsequent fall can still deal normal fall damage.
Accepted provisional travel profile: four horizontal blocks over 0.3 seconds
(six ticks), with an approximately half-block upward arc in unobstructed travel.
Terrain and collision may shorten or flatten the leap. Shared dash prediction,
control restrictions, interruptions and lifecycle cleanup apply; per-rank
upgrades remain deferred.
Bow attacks and skills that do not move the player remain usable during the
leap. Disengage grants no invulnerability; incoming attacks can still hit normally.

#### Bear Trap
Minimum character level: 3.

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
Minimum character level: 5.

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
Minimum character level: 5.

Implemented Rank 1: summon a fixed, skill-defined companion. One companion follows
the player and assists in combat, remaining until defeated or dismissed. Pet
survival is part of managing the skill.

Combat behavior: automatically attack enemies the player damages and retaliate
against enemies that damage the player.

Implementation: reuse native wolf movement and combat AI wherever
possible. The pet draws attacks through normal enemy retaliation; its attacks do
not force target changes or apply a taunt. Enemies may continue pursuing the owner.

Defeat rule: defeat starts a cooldown. Once that cooldown expires, the
player can summon the pet again. The agreed Rank 1 baseline is 30 seconds, subject
to the shared skill cooldown multiplier. Pressing the skill while the pet is alive
dismisses it without a Focus cost and starts the same cooldown. After the cooldown,
a new summon costs 35 Focus and creates a full-health replacement; dismissal
cannot provide an immediate heal through resummoning.

Persistence: retain the living companion's health and state across logout and
server restarts in a small owner-saved record. Remove its world entity while the
owner is offline and restore it beside the owner on login without healing or
duplicating it. Offline companions do not tick or force chunks; use lifecycle
events rather than global scans.

Dimension travel: transfer the companion beside its owner without a Focus cost or
cooldown, preserving health and clearing its combat target. If no safe placement
is available, keep the companion stored until it can appear safely. Do not force
chunks to find a placement.

Owner death: a confirmed owner death dismisses the companion and starts its normal
30-second resummon cooldown. A new summon after respawn costs 35 Focus and starts
at full health. Cancelled deaths do not dismiss the pet or start this cooldown.

Healing: use native wolf food and feeding behavior to restore health. The
companion has no automatic health regeneration.

Targeting: assist against any valid enemy the owner damages, including creepers,
and defend the owner or itself against attackers. Use Covenant's shared ally and
PvP protections. Do not independently hunt skeletons or wildlife. Retain native
wolf movement and melee AI; native species exclusions do not govern companion
target selection. Engaging a creeper may expose the pet to its explosion.

Loadout: the companion requires Hunter's Pet to remain assigned to a skill slot.
Removing its last assignment, unlearning it through respec, or switching away
from Stalker dismisses it and starts its normal cooldown. A change that retains
at least one assignment preserves the companion; duplicate assignments grant no
additional pets.

Interactions: the summoned companion always follows when not fighting. Native
wolf food heals it; there is no sit/stay toggle, breeding, or wolf armor. Use the
skill key to dismiss it. Rank 1 uses the accepted provisional balance of 40 health,
3 attack damage and a 30-tick attack interval; higher ranks remain deferred.

#### Barrage
Minimum character level: 5.

Planned behavior: fire a rapid sequence of arrows while the skill keybind is held
and sufficient Focus remains. Aim is adjustable throughout firing, allowing the
player to concentrate on one enemy or sweep across several. Releasing the key or
running out of sufficient Focus stops firing. There is no fixed burst duration.
Each arrow spends Focus individually when it fires. Focus regeneration pauses
while channeling Barrage and resumes when the channel ends. Movement remains
allowed at normal bow-draw speed during the channel. Firing rate, damage, and
per-arrow Focus cost are deferred.

#### Snipe
Minimum character level: 10.

Planned behavior: charge while standing still, then fire one single-target shot.
Its damage bonus increases with distance, up to a cap. Remaining stationary during
the charge is required; moving cancels the charge rather than preventing movement.
Exact charge duration, distance scaling, and cap are deferred.

#### Dodge Aura
Minimum character level: 10.

Planned recipients: the player and nearby allies, for both the always-on passive
and the temporary activated buff, using the shared aura ally rules and six-block
prototype radius.

Passive: add 10 percentage points of Dodge while the aura is assigned to a skill
slot. Ordinary Dodge chance has an agreed global cap of 85%, including equipment
and aura bonuses. This cap is implemented in the shared Dodge handler. The active's guaranteed
dodge bypasses this ordinary chance cap.

Active: grant each recipient a brief window in which one dodge is guaranteed.
The guarantee applies to one dodgeable incoming hit per recipient, ensuring that
hit is avoided regardless of ordinary Dodge chance and its 85% cap. Undodgeable
damage remains undodgeable. The guarantee lasts three seconds (60 game ticks)
at base, scaled by the caster's existing skill-duration multiplier, and expires
unused if no dodgeable hit arrives. Its base cooldown is 20 seconds (400 game
ticks), scaled by the existing cooldown multiplier. This exceeds Blast Trap's
current 15-second base cooldown, satisfying the agreed minimum. Activation costs
25 Focus at base, scaled by the existing resource-cost multiplier.

On the next dodgeable incoming hit, resolve the guarantee before ordinary Dodge
and consume all currently overlapping Dodge Aura guarantees on that recipient.
Overlapping activations therefore protect against one hit, not a queue of hits.
A new activation after consumption can supply another guarantee. Undodgeable
damage neither triggers nor consumes the guarantees.

#### Frost Trap
Minimum character level: 10.

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
Minimum character level: 15.

Current implementation: fire a single-target arrow that deals magic damage and briefly
increases the target's damage taken from everyone, including teammates. Repeated
applications, including from multiple Stalkers, refresh one shared vulnerability
effect's duration without stacking its strength. This vulnerability stacks with
Shock Aura's damage-taken increase additively: Arcane Arrow's +15% and Shock
Aura's +10% produce +25% damage taken while both apply. Accepted Rank 1 baseline: 30 Focus,
eight-second cooldown, four direct magic damage, and +15% damage taken for four
seconds. Requires a held bow and uses one arrow under the existing bow-skill
ammunition rules, including native ammunition-use exemptions such as Infinity.
Tipped and spectral arrows retain their normal secondary effects, allowing
one shot to apply both vulnerability and the ammunition's native effect.
The four raw magic damage replaces ordinary arrow impact damage rather than
adding a second damage hit. Apply or refresh vulnerability only after positive
direct-hit damage to health or absorption hearts; a fully prevented hit applies
no debuff. Resolve direct damage first, so the hit does not benefit from the
vulnerability it newly applies. Native flight, armor/shield-bypassing magic projectile damage,
and a saved/synced target-owned debuff follow `docs/RPG_SKILLS.md`; per-rank
upgrades remain deferred.

#### Poison Shot
Minimum character level: 15.

Planned behavior: fire a single-target arrow that deals initial hit damage and
then applies poison damage over time. This maintains pressure while the player
repositions or prepares another attack. Initial damage, poison damage, tick
interval, and poison duration are deferred.

#### Multishot
Minimum character level: 20.

Current implementation: fire several arrows simultaneously in a horizontal fan, covering
enemies spread across the player's view. Each enemy can take damage at most once
per activation, even if multiple arrows connect (no shotgunning). Accepted Rank 1
baseline: 30 Focus, eight-second cooldown, five arrows over a total horizontal
spread of 30 degrees, and native arrow damage coefficient 1.5 per arrow.
Per-rank upgrades remain deferred. The full fan uses
one ammunition item under the existing bow-skill ammunition rules, including
native ammunition-use exemptions. The four extra arrows are unrecoverable;
at most the central arrow can be recovered under normal ammunition/pickup rules,
so one activation cannot duplicate ammo.
The first eligible arrow contact claims the target's single damage attempt
for the whole fan, including blocked, dodged, or otherwise prevented attempts.
Other arrows from that activation cannot retry damage or apply on-hit
ammunition effects to that target; overlapping arrows do not repeatedly test
defenses.
Saved arrow payloads and target-owned fan claims preserve the limit across
save/load. Fan arrows expire after sixty seconds of overworld game time,
including time unloaded; expired target claims are pruned on the next fan hit.

#### Flame Trap
Minimum character level: 20.

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
Minimum character level: 25.

Planned recipients: the player and nearby allies.

Passive: grant +10% movement speed to the player and nearby allies while the aura
is assigned to a skill slot, using a native movement-speed modifier and the
shared six-block range, coverage refresh, and grace rules.

Active: clear existing slows and grant an additional 20 percentage points of
movement speed plus 0.5 blocks of additional step height. The passive and active
speed bonuses add to +30% while both apply. After passive coverage expires on
leaving range, recipients retain the active's +20% for its remaining duration.
The active lasts four seconds (80 game ticks) at base, scaled by the caster's
existing skill-duration multiplier. Slow cleansing happens once on activation;
the buff grants no slow immunity. New hits and ongoing slowing fields can
reapply slows afterward. The cleanse removes vanilla Slowness and Covenant
movement slows, including Chilled and aura/trap slows, but leaves roots and full
freezes intact. It does not remove arbitrary third-party movement modifiers.
Activation costs 25 Focus at base and has a 20-second (400-game-tick) base
cooldown, using the existing resource-cost and cooldown multipliers.

#### Snaring Shot
Minimum character level: 25.

Requires: Poison Shot.

Planned behavior: fire a shot that applies poison damage over time plus a strong
temporary movement slow. This combines damage over time with ranged pursuit
control. Poison damage, poison duration, slow strength, and slow duration are
deferred.

#### Blast Trap
Minimum character level: 25.

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
Minimum character level: 30.

Requires: Arcane Arrow.

Planned behavior: a spell with a cast time that uses an instant-shot arrow to
designate whatever it connects with as the target of a meteor. The meteor lands
on that target shortly afterward, following it if the arrow marked a moving
enemy. The meteor deals area damage around its impact. Cast duration, meteor
delay, impact radius, and damage are deferred.

The impact ignites affected enemies only; it leaves no burning ground area. It
does not apply Arcane Arrow's vulnerability. Burn duration is deferred.

#### Ballista
Minimum character level: 30.

Planned behavior: deploy a temporary stationary turret that automatically fires
heavy bolts at nearby enemies. This provides position-holding ranged support.
Enemies cannot select the Ballista as an attack target, but it can still take
incidental damage, such as from Wither or Ghast explosions and similar effects.
Each player may have two active Ballistas. Deploying a third removes the oldest,
keeping the two most recently deployed.
Health, duration, firing rate, targeting range, and bolt damage are deferred.

#### Volley
Minimum character level: 30.

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
Minimum character level: 1.

Planned behavior: a wide forward-facing melee sweep, rewarding facing and
positioning. Sweep angle and damage tuning are deferred.

Current prototype: deal effective native attack damage to non-allied living targets
within 3 blocks, line of sight, and a 120-degree horizontal forward arc. Vertical
aim does not rotate the arc. Cost remains 20 Rage and cooldown 3 seconds.

#### Rend
Minimum character level: 3.

Planned behavior: a single-target melee strike that deals an initial hit and
applies bleeding damage over time using the shared bleed rules above.

Current prototype: instant attack against the enemy under the crosshair, using
the held item's native attack range and block-clipped targeting (normally 3 blocks
in Survival). The initial strike deals 100% of effective native attack damage.
A damaging hit applies one five-second stack at 10% of captured attack damage per
second. Cost: 20 Rage; cooldown: 2 seconds. Existing cost, cooldown, and duration
multipliers apply. Missing targets produce paid whiffs and normal cooldowns.
The cost and cooldown remain spent even if defenses stop the
damage and prevent bleed application. No weapon-category restriction is added.

#### Battle Cry
Minimum character level: 3.

Implemented Rank 1: instant shout granting self and eligible allies within six
blocks +15% outgoing damage for six base seconds. Costs 30 Rage; thirty-second
cooldown. Uses shared aura team/owned-pet recipients and source-aware cleanup.
Copies do not stack strength. Recipients retain the buff after moving away; late
entrants get nothing. New projectiles/bleeds capture it once; existing ones are
unchanged. Higher ranks remain deferred. See [[Project_Covenant_Reaver_Balance_Proposal]].

#### Rage Storm
Minimum character level: 5.

Requires: Cleave.

Implemented Rank 1: sixteen-tick wind-up, then a two-block-radius spherical area
travelling eight blocks at 0.35 blocks/tick along full 3D aim captured at release.
The player moves independently. Solid terrain stops its center and blocks damage
through cover. Pulses deal three armor-mitigated damage immediately on release,
then every ten ticks, at most four attempts per enemy. The default journey offers
three pulses. Costs 35 Rage at completion; twelve-second cooldown. It is a
temporary native entity with lifecycle cleanup and no saving or chunk forcing.
Higher ranks remain deferred. See [[Project_Covenant_Reaver_Balance_Proposal]].

#### Execute
Minimum character level: 5.

Planned behavior: a heavy single-target melee attack usable only when the enemy
is below a health threshold. Health threshold, damage, resource cost, and cooldown
are deferred.

#### Blood Fever
Minimum character level: 5.

No skill prerequisite; Rend is not required.

Implemented Rank 1: free self-only passive while assigned to a skill slot.
Every five ticks, count distinct bleeding enemies within four blocks (including
shared teammate bleeds) for +2% base movement and +3% base attack speed each,
capped at five enemies (+10%/+15%). Extra stacks, allies and allied pets do not
count. Native transient modifiers update as enemies move, stop bleeding or die;
unbinding removes them immediately. Higher ranks remain deferred. The local
query is measured in the dedicated fixture; see `docs/VALIDATION.md` for evidence
and [[Project_Covenant_Reaver_Balance_Proposal]] for the balance baseline.

#### Leap
Minimum character level: 10.

Planned behavior: leap toward a ground point the player aims at, dealing area
damage on landing. No enemy target is required. Leap range, impact radius,
damage, resource cost, and cooldown are deferred.

#### Ripping Aura
Minimum character level: 10.

Mutually exclusive with: Fortitude Aura.

Planned recipients: the player and nearby allies.

Passive: grant +20% bleed damage to the player and nearby allies while the aura
is assigned to a skill slot, using the shared six-block range and passive
coverage rules. Direct-hit damage is unchanged. Capture the bonus in each bleed
stack's damage when that stack is applied, matching the existing bleed snapshot
model. Existing stacks do not gain a bonus when their attacker enters coverage,
and boosted stacks retain their captured damage after coverage ends. Refreshing
a retained stack preserves its captured damage; replacements capture the current
bonus. Aura cleanup removes the source's coverage and active buff, not already
applied bleed stacks or their captured damage.

Active: temporarily make recipients' ordinary melee attacks and melee skills
(including Cleave and Rend) apply bleeding when they deal positive damage to
health, ordinary absorption, or Fortitude's shield. Projectiles and
damage-over-time pulses do not trigger it.
Rend retains its own bleed application and applies only one stack per target per
hit; Ripping does not add a second stack to Rend. Other eligible hits apply a
bleed matching Rend's prototype: 10% of the attacker's captured native attack
damage per one-second pulse, with a five-second base duration. Use the existing
shared three-stack cap, refresh/replacement rules, attribution, and defenses.
Ripping's captured passive bonus raises the pulse to 12% when applicable.
The active buff has a five-second (100-game-tick) base duration, scaled by the
caster's existing skill-duration multiplier. Applied bleeds continue for their
own remaining durations after the buff ends. Activation costs 40 Rage at base
and has a 20-second (400-game-tick) base cooldown, using the existing
resource-cost and cooldown multipliers.

Each recipient can apply at most one aura-triggered bleed per target per second
(20 game ticks). This limit is per attacker/target pair, shared across overlapping
Ripping buffs on that attacker, and limits applications and refreshes rather than
damage pulses. Different targets have independent limits. Rend's own bleed
application remains unchanged and does not trigger an extra Ripping application.

#### Fortitude Aura
Minimum character level: 10.

Mutually exclusive with: Ripping Aura.

Passive: grant the player and nearby allies +4 Armor and +2 Armor Toughness
while the aura is assigned to a skill slot. Use native flat attribute modifiers
and the shared six-block range and passive coverage rules.

Active: grant the player and nearby allies a temporary damage-absorbing shield.
The shield absorbs damage after armor and other applicable damage reduction,
before health is lost. A hit reduced from 10 damage to 6 consumes 6 shield points.
Each recipient receives a flat six-point shield (three hearts), independent of
maximum health. The shield lasts seven seconds (140 game ticks) at base, scaled
by the caster's existing skill-duration multiplier, or ends sooner when depleted.
Unused capacity expires with the shield. Track Fortitude capacity separately
from ordinary absorption hearts and consume Fortitude first, before ordinary
absorption or health. Expiry and source cleanup remove only Fortitude's own
remaining capacity, preserving unrelated absorption.

Fortitude is an exception to the shared independent-copy buff rule: select
eligible recipients inside the six-block area only when the active is pressed.
Each activation grants the normal six-point shield and restarts its full duration
on those recipients, including recipients already carrying Fortitude. Replace
the old Fortitude shield with the fresh cast's shield rather than adding capacity
or retaining a backup copy; the fresh cast owns the replacement for cleanup.
Capacity is restored to six points, never added on top of remaining points.
Entering the area afterward does not grant or refresh a shield. Activation costs
40 Rage at base and has a 20-second (400-game-tick) base cooldown, using the
existing resource-cost and cooldown multipliers.

#### Lunging Combo
Minimum character level: 15.

Planned behavior: a three-activation combo.

1. First activation: perform a movement-only lunge of four blocks over three game
   ticks (0.15 seconds), dealing no attack damage. Use the captured full 3D look
   direction and shared movement rules; collision or interruption may end it early.
2. Second activation, within 2 seconds after the lunge finishes: perform a
   single-target attack using Rend's block-clipped crosshair targeting and native
   held-item melee reach, retaining enemy eligibility and line-of-sight checks.
   Deal 125% of current attack damage at stage acceptance, using existing
   melee-skill damage rules and applicable damage modifiers.
   Without a valid target, perform a paid whiff and advance to stage three.
3. Third activation, within 3 seconds after the second stage finishes: perform an AoE
   cleave that applies 20% armor shred, leaving armor toughness unchanged.
   Percentage-based shred and its strength are provisional prototype tuning to
   revisit when enemy armor adjustments are introduced. Match Cleave's area: a 120-degree
   frontal horizontal arc within three blocks, requiring line of sight and
   retaining existing enemy eligibility checks and the yaw-based arc calculation.
   Deal 100% of current attack damage per enemy at stage acceptance, using existing
   melee-skill damage rules and applicable damage modifiers.
   An empty area performs a paid whiff and completes the combo, starting its
   captured cooldown. Defended hits also complete normally; neither whiffs nor
   hits dealing no damage apply shred. Cleave and Rend likewise spend and start
   cooldowns on empty attacks.
   Shred lasts a provisional 100 game ticks (five seconds) at base, scaled by the
   caster's skill-duration multiplier captured when the finisher starts; later
   modifier changes do not alter an already-applied shred's duration.
   Apply shred only after positive damage to health, ordinary absorption, or a
   Fortitude shield; blocked, dodged, or invulnerable hits apply none. Resolve
   damage against existing armor before applying this hit's shred, so the new
   application benefits subsequent hits.
   Each target has one shared armor-shred debuff for Lunging Combo and Rip and
   Rush, even across players. Repeated
   applications remain at 20% and set expiry to the later of the existing expiry
   and the new application's expiry, never shortening a longer application.
   Once applied, shred belongs to the target and remains for its remaining
   duration if the caster dies, logs out, changes class, or unlearns the skill;
   caster cleanup does not remove it from surviving targets.
   Save remaining shred duration across target save/load and reconnects; pause
   its timer while the target is unloaded or offline and resume when it ticks
   again. Target death clears shred; do not copy it to a respawned replacement
   player. This target-owned timer is separate from combo progress and cooldowns.
   Treat shred as a normal harmful status: milk and normal effect-clearing can
   remove it, and native effect-immunity/applicability checks can reject it.
   Rejected status application does not undo finisher damage, Rage spending,
   or stage completion.

Base Rage costs are 20 for the lunge, 10 for the single-target strike, and 20 for
the finisher (50 total before modifiers). Each stage checks and spends its own
cost using the player's current resource-cost multiplier when accepted.

Rage is spent separately for each stage performed. Unused follow-ups cost nothing;
the player must retain enough Rage to perform later stages.

One shared cooldown begins when the final cleave completes or a follow-up window
expires. Letting the combo lapse after the lunge or second attack still starts
that cooldown.

Base cooldown is 240 game ticks (12 seconds), reflecting the extended sequence
and combined mobility, damage, and armor-shred power. Capture the effective
duration with the cooldown multiplier when stage one starts, then start it at
completion, timeout, or cancellation under the shared rules.

The stage-two strike and stage-three finisher are instant, with no cast time or
added wind-up, retaining normal activation checks and throttling. This is agreed
implemented prototype behavior; numerical balance remains provisional.

#### Bloody Cloak
Minimum character level: 15.

Planned behavior: a temporary self-buff that heals the player for a portion of
the bleed damage they deal. Activation clears existing Wither, ignite/burning,
and poison. Immunity prevents those effects from being reapplied while the buff
is active. Healing fraction, buff duration, resource cost, and cooldown are
deferred.

#### Overpower
Minimum character level: 20.

Planned behavior: a single-target melee hit that deals normal damage to bleeding
enemies and gains a huge damage bonus against non-bleeding enemies. This supports
a non-bleed Reaver build alongside the class's bleed-focused options, while
remaining usable when a teammate applies bleed. Damage, resource cost, and
cooldown are deferred.

#### Consume
Minimum character level: 20.

Planned behavior: an area skill that removes bleeds from affected enemies. Each
affected enemy takes a flat damage amount per bleed instance removed from it.
Guillotine is the single-target counterpart. Area size, flat damage per instance,
resource cost, and cooldown are deferred.

#### Enduring Cry
Minimum character level: 20.

Requires: Battle Cry.

Planned behavior: an active shout that briefly reduces damage taken for the player
and nearby allies. This provides a defensive window to time before dangerous
attacks. Damage reduction, range, duration, resource cost, and cooldown are
deferred. Ally identification remains outside this design pass.

#### Skull Bash
Minimum character level: 25.

Planned behavior: a single-target melee hit that briefly stuns the enemy,
preventing movement and attacks. Damage, stun duration, resource cost, and
cooldown are deferred.

#### Raging Aura
Minimum character level: 25.

Passive: while assigned to a skill slot, set the player's out-of-combat Rage
decay rate to 3 Rage per second after the existing seven-second grace period.
This is a self-only passive and does not extend the grace period or generate Rage.

Active: temporarily grant +20% to all damage personally dealt by the player, including
ordinary melee attacks, skills, and bleeding, and stop passive Rage decay.
This is self-only and excludes pet damage. Skills still spend Rage normally
during the buff. Bleed stacks capture Raging's damage bonus when applied, matching
Ripping's snapshot rule. Existing stacks are unchanged when Raging activates;
boosted stacks retain their damage after it ends. Refreshing retained stacks
preserves captured damage, while replacement stacks capture the current bonus.
Ripping's +20% bleed bonus and Raging's +20% damage bonus combine additively for
+40% bleed damage, captured once per new stack. A base pulse of 10% captured
attack damage becomes 14% with both; do not multiply the bonuses or reapply
Raging's bonus on subsequent pulses. The active lasts six seconds (120 game
ticks) at base, scaled by the caster's existing skill-duration multiplier.
Activation costs 40 Rage at base and has a 30-second (600-game-tick) base
cooldown, using the existing resource-cost and cooldown multipliers. The longer
cooldown makes this an occasional burst rather than an activation for every
enemy encounter.

#### War Banner
Minimum character level: 25.

Planned behavior: plant a temporary stationary banner that grants health
regeneration and prevents hunger loss for the player and nearby allies within
its radius. Regeneration strength, range, duration, resource cost, and cooldown
are deferred. Ally identification remains outside this design pass.

#### Rip and Rush
Minimum character level: 30.

Requires: Lunging Combo.

Planned behavior: a three-activation combo.

1. First activation: apply Rend in a circular area covering all directions within
   a three-block radius around the player. Require line of sight and retain
   existing enemy eligibility and melee-area distance checks without the frontal
   arc restriction.
   Apply Rend once per eligible enemy: 100% current attack damage with existing
   melee-skill modifiers and one normal bleed stack after positive health,
   absorption, or Fortitude damage. Retain Rend's defenses and its guard against
   an additional Ripping Aura bleed on that hit. Base bleed is 10% of
   application-time attack damage per second for 100 base ticks (five seconds),
   with existing bleed-power bonuses and skill-duration scaling captured at stage
   start. Preserve the target-owned shared three-stack cap, duration refresh,
   oldest-stack replacement, attribution, and persistence rules.
2. Second activation, within 2 seconds after the first stage finishes: dash six
   blocks over four game ticks (0.2 seconds), applying armor shred to enemies
   passed through. Use the captured full 3D look direction and shared movement
   rules; movement-speed and skill-duration bonuses do not change distance or
   travel time. Full blockage and agreed interruptions can end movement early, opening
   the full third-stage window when the dash actually ends.
   Deal 50% of current attack damage captured when this stage begins, using
   existing melee-skill damage rules and applicable damage modifiers. Allow at
   most one hit attempt per enemy per dash, including defended attempts;
   remaining in contact does not retry or deal repeated hits.
   Contact requires an eligible enemy's hitbox to intersect the player's body
   swept along the actual full 3D travelled path, without an added hit radius.
   Clip checks to terrain-limited travel, including the final partial segment;
   enemies beside the swept body or beyond the stopping obstacle are not contacted.
   Apply Lunging Combo's provisional 20% shred for 100 base ticks (five seconds),
   leaving toughness unchanged. Snapshot scaled duration at dash-stage start with
   the caster's skill-duration multiplier. Require positive health, absorption,
   or Fortitude damage and resolve that hit against existing armor first. Reuse
   all shared shred cleansing, native immunity, target persistence/death, and
   caster-independent expiry rules. Both skills and all players refresh one
   target-owned debuff without stacking strength or shortening a longer application.
3. Third activation, within 3 seconds after the dash finishes: perform another circular AoE
   around the player's current position, consuming bleed on affected enemies to
   grant a temporary movement-speed bonus.
   Match the opening area: all directions within a three-block radius, requiring
   line of sight and retaining the same enemy eligibility and melee-area distance
   checks. Center it on the player's position when activated, even if the player
   has moved since the dash ended.
   Deal 100% of current attack damage per eligible enemy, captured when this
   stage begins, using existing melee-skill damage rules and applicable damage
   modifiers.
   For each enemy qualifying for consumption, remove all shared bleed stacks,
   including other players' stacks, through the existing shared consumption
   boundary. Count each enemy once toward the movement-speed reward regardless
   of stack count. This can spend teammates' remaining bleed damage.
   Consumption requires positive finisher damage to health, absorption, or
   Fortitude. Blocked, dodged, or invulnerable hits preserve bleeds and grant no
   speed reward. Killing blows against bleeding targets qualify; retain pre-hit
   bleed presence for reward eligibility even if death cleanup has removed the
   stacks. Each qualifying enemy still counts once.

Rage is spent separately for each stage performed; unused follow-ups cost nothing.
One cooldown for this combo begins when the final stage completes or a follow-up
window expires, matching Lunging Combo's timing and spending rules.

The finisher's movement-speed bonus scales with the number of bleeding enemies
whose bleed is consumed, up to a cap. Consuming bleeds can reduce Blood Fever's
count of nearby bleeding enemies.
Grant the caster +10% base movement speed per qualifying enemy whose bleed is
consumed, capped at +30% from Rip and Rush (three enemies). Extra stacks on one
enemy do not increase its contribution. Follow Swiftness Aura's native
base-relative modifier convention; this caps the skill's contribution rather
than total movement speed.
The speed buff lasts 80 base game ticks (four seconds), deliberately limiting
uptime, scaled by the caster's skill-duration multiplier captured when the
finisher starts. Later modifier changes do not alter its granted duration.
Keep one Rip and Rush speed buff: consuming bleed from at least one qualifying
enemy replaces it with the new finisher's strength and duration, even if weaker
or shorter. Do not stack strengths or add durations. Consuming no bleeds leaves
an existing buff unchanged without refreshing, extending, weakening, or removing it.
The speed buff is transient and unsaved. Clear it on death, logout,
dimension/class change, unlearning Rip and Rush (including dependent removal),
or a validated actual edit to its assigned slot, even if a duplicate binding
remains. Rejected/unchanged edits and physical key remaps leave it intact.
Cleanup applies after combo completion too, without restarting cooldowns or
creating a cooldown for an idle skill.
Base Rage costs are 30 for the opening circular Rend, 20 for the dash, and 20 for
the finisher (70 total before modifiers). Each stage checks and spends its own
cost using the player's current resource-cost multiplier when accepted,
following the shared per-stage resource rules.
Base cooldown is 400 game ticks (20 seconds). Capture its effective duration with
the cooldown multiplier when stage one starts, then start it at completion,
timeout, or cancellation under the shared rules.
The opening circular Rend and final circular AoE are instant attacks, with no
cast time or added wind-up. Retain normal activation checks and throttling;
the dash remains an executing movement stage.
Both circular attacks execute with no enemies present, spending their stage
cost and advancing normally. An empty opening opens the dash window; an empty
finisher completes the combo and starts its captured cooldown. Non-bleeding
enemies can be damaged but grant no speed reward. The dash requires no target
or bleed. No skill rejects activation for lack of targets.

#### Guillotine
Minimum character level: 30.

Planned behavior: remove bleeds from one target, dealing a flat damage amount per
bleed instance removed, as the single-target counterpart to Consume. Flat damage
per instance, range, resource cost, and cooldown are deferred.

#### Demon Mode
Minimum character level: 30.

Planned behavior: while this temporary mode is active, attacking teleports the
player to the enemy under their crosshair, up to 5 blocks away. The player heals
each time they strike an enemy or one of their bleeds deals damage. Healing is a
fixed amount per trigger, with separate amounts for strikes and bleed ticks.
Healing amounts, mode duration, resource cost, and cooldown are deferred.

## Weaver

#### Arcane Bolt
Minimum character level: 1.

Fire a straight amethyst-rendered projectile; one collision deals 6 attributed indirect-magic damage.

#### Lightning Bolt
Minimum character level: 3.

Implemented prototype: after a 0.5-second cast, strike instantly along the player's
aim, stopping at the first eligible enemy or solid block within 24 blocks. Deals
6 lightning damage, costs 20 Mana, and has a two-second cooldown. Direct damage
only; no status effect or native lightning entity. See `docs/RPG_SKILLS.md`.

#### Mage Armour
Minimum character level: 3.

Planned behavior: a temporary self-buff that increases armor, armor toughness,
and knockback resistance, paid for with an upfront Mana cost. Stat bonuses,
duration, Mana cost, and cooldown are deferred.

#### Arcane Barrage
Minimum character level: 5.

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
Minimum character level: 5.

Implemented prototype: traveling single-target projectile dealing 6 ice damage.
Positive health or absorption damage applies a 30% Chilled movement slow for
3 seconds, scaled by skill duration. Reapplication refreshes without stacking.
Costs 20 Mana, casts in 0.5 seconds, and has a two-second cooldown.
See `docs/RPG_SKILLS.md` for defenses, persistence, and overlap rules.

#### Discharge
Minimum character level: 5.

Implemented Rank 1: completed Fire/Ice/Lightning Bolt casts store one charge of
that element while Discharge is learned, capped at five each, including misses.
Activate for 30 Mana, a 0.5-second cast and twelve-second cooldown to consume all
charges in a three-block player-centered area. Each charge adds one damage of
its element before defenses. An empty bank rejects; an empty area is a paid cast.

Positive ice damage applies a 30% slow for three seconds, replaced by a one-second
freeze at five ice charges. Positive fire damage ignites for four seconds; five
fire charges add one centered explosion dealing up to four extra damage with
linear distance falloff and no terrain destruction. Five lightning charges add
one chain to up to two external enemies, four-block jumps, dealing 50% of the
lightning contribution each. Cover blocks the area and arcs; all components
resolve independently through native defenses without hurt-window suppression.

Charges are transient and clear on death, logout, class/dimension change or
unlearning. Duration scaling and shared freeze/slow rules apply. Server datapacks
tune Rank 1 through `data/covenant/skills/weaver.json`; higher ranks are deferred.
See `docs/RPG_SKILLS.md` for attribution, snapshots, riders and cleanup.

#### Fire Bolt
Minimum character level: 5.

Implemented prototype: traveling single-target projectile dealing 6 fire damage.
Positive health or absorption damage ignites for 4 seconds, scaled by skill
duration and native burning-time modifiers. Uses native burning and extinguishing,
without custom burn stacks. Costs 20 Mana, casts in 0.5 seconds, and has a
two-second cooldown. See `docs/RPG_SKILLS.md` for defenses and persistence.

#### Icy Veins
Minimum character level: 10.

Uses the shared aura model despite its name.

Passive: self-only 10% reduced skill cooldowns while assigned to a skill slot,
using the existing native skill-cooldown multiplier. A base 20-second cooldown
becomes 18 seconds before other modifiers. Apply the reduction only when a new
cooldown is calculated through the existing skill pipeline. Assigning or removing
Icy Veins does not rewrite already-running cooldowns.

Active: make all casts instant for a limited period by removing spell wind-ups.
Channel firing rates, including Arcane Barrage's missile rate, remain unchanged.
The active lasts four seconds (80 game ticks) at base, scaled by the caster's
existing skill-duration multiplier. There is no cast-count limit: players may
rotate every available spell cooldown during the window if they have the Mana.
Spell resource costs and cooldowns remain unchanged by the active. Activation
costs 30 Mana at base and has a 30-second (600-game-tick) base cooldown, using the
existing resource-cost and cooldown multipliers. Its own passive reduces that
cooldown to 27 seconds before other modifiers.

#### Flame Cloak
Minimum character level: 10.

Uses the shared aura model.

Passive: self-only 20% reduced incoming fire damage while assigned to a skill
slot. Identify fire damage through the native fire damage-type tag, including
burning, lava, and fire-tagged attacks. This grants reduction, not fire immunity;
non-fire damage is unchanged.

Active: temporarily ignite eligible enemies whose melee hits deal positive
damage to the player's health, ordinary absorption, or Fortitude shield.
Dodged, fully blocked, or otherwise fully negated hits do not trigger retaliation.
Retaliation ignites for four seconds (80 game ticks) at base, scaled by the
caster's existing skill-duration multiplier. Use native burning with normal
extinguishing, fire immunity, and burning-time modifiers, as with Fire Bolt.
Repeated hits refresh native burning rather than adding independent burn stacks.
Target 40% base uptime for the retaliation window: its base duration is 40% of
the chosen base activation cooldown. Apply the existing cooldown and
skill-duration multipliers independently afterward, so effective uptime can
exceed 40%. The base cooldown is 20 seconds (400 game ticks), giving an
eight-second (160-game-tick) base retaliation window. Activation costs 30 Mana at
base, scaled by the existing resource-cost multiplier.

#### Teleport
Minimum character level: 10.

Implemented Rank 1: instantly blink up to eight blocks along full 3D aim for
25 Mana, with a ten-second cooldown. No target or destination floor is required.
Sweep the complete player collision box and stop before obstacles without sliding
or passing through walls. Respect border/build bounds and loaded chunks. Blocked
or negligible movement rejects without spending or cooldown. Root, freeze,
mounting, sleeping and existing skill movement prevent activation.

Success resets velocity and accumulated fall distance, using native server
teleport synchronization. Hazards and subsequent falling remain normal. Server
datapacks tune Rank 1 through `data/covenant/skills/weaver.json`; higher ranks are
deferred. See `docs/RPG_SKILLS.md` for collision and lifecycle boundaries.

#### Freezing Aura
Minimum character level: 15.

Requires: Icy Veins.

Passive: slow nearby enemies by 10% while assigned to a skill slot, using the
shared six-block radius and one-second coverage refreshed every 0.5 seconds.
The prototype target is the smallest noticeable slow; perceived impact requires
in-game playtesting. Covenant movement slows do not combine: apply only the
strongest active slow, so this 10% yields to Chilled's 30% or Frost Trap's 50%.
Weaker effects retain their own timers and resume if still active when stronger
coverage ends. Roots and full freezes are separate control effects. This shared
slow rule is planned and requires updating existing slow implementations.

Active: briefly freeze nearby enemies, preventing voluntary movement, jumping,
attacks, and skill casting. Frozen targets can still take damage and receive
external knockback; freeze grants no invulnerability and does not cancel external
forces. Freeze lasts 1.5 seconds (30 game ticks) at base, scaled by the caster's
existing skill-duration multiplier. Damage does not break freeze, including
ongoing bleeding or burning. Applying freeze cancels pending Covenant casts
rather than pausing them. Unfinished casts spend no resource and start no
cooldown, following existing cancellation behavior; targets must cast again
after thawing. Activation costs 40 Mana at base and has a 25-second
(500-game-tick) base cooldown, using the existing resource-cost and cooldown
multipliers.
An otherwise valid activation still spends its Mana and starts cooldown when
there are no eligible enemies in range, or no eligible enemy receives freeze.
This is a committed area action, not a target-required cast.
There is no blanket boss immunity to Freezing Aura's slow or freeze in this
prototype. Eligible bosses follow the same control rules, while actual
invulnerability remains respected, consistent with the trap control policy.
There is no post-freeze immunity period or diminishing-return system in this
prototype. Targets are eligible for another freeze immediately after thawing,
so coordinated casters can stagger freezes, including against eligible bosses.
Overlapping freezes still follow the shared non-stacking active-effect rules.

#### Shock Aura
Minimum character level: 15.

Passive: nearby eligible enemies take 10% increased damage from all sources while
the aura is assigned to a skill slot, using shared six-block range and passive
coverage/grace rules. This includes allies' attacks and ongoing damage.
This increase stacks with Arcane Arrow's vulnerability. Overlapping Shock Auras
do not stack with one another; only the strongest copy applies.

Active: temporarily make the caster's Lightning Bolt chain from its initial
target to up to two additional eligible enemies (three targets total). A target
cannot be hit twice by the same cast. Each jump reaches up to four blocks from
the previous target, independently of the caster's six-block passive field;
later targets can be outside that field. Each jump selects the nearest unhit
eligible enemy with a clear path from the previous target; solid cover blocks
arcs. If no candidate qualifies within four blocks, the chain ends. These cover
checks occur during the cast, not as recurring passive coverage checks. The
active lasts seven seconds (140 game ticks) at base, scaled by the caster's
existing skill-duration multiplier, with no cast-count limit. The tuning target
is roughly three or four chaining casts as spell cooldowns and Mana allow.
Activation costs 30 Mana at base and has a 25-second (500-game-tick) base cooldown,
using the existing resource-cost and cooldown multipliers. Each chained hit deals 50% of
Lightning Bolt's base damage without further falloff: at current tuning, 6 damage
on the initial target, then 3 and 3, before bonuses and defenses. Derive jump
damage from base power, not damage actually received by the previous target.

#### Immolation Aura
Minimum character level: 15.

Requires: Flame Cloak.

Passive: deal one point of fire damage per second (one pulse per 20 game ticks)
to nearby eligible enemies while assigned, using the shared six-block range and
coverage rules. Overlapping copies use only the strongest Immolation passive;
they do not add damage or independent damage schedules. Damage is before defenses.
Pulses operate independently of normal hurt-immunity windows: other attacks do
not suppress aura pulses, and aura pulses do not suppress other attacks. Actual
invulnerability and fire immunity remain respected.
Immolation pulses bypass armor, shield blocking, and Dodge, while respecting
fire protections, general damage reduction, Fortitude shields, and ordinary
absorption.

Active: self-only +30% fire damage personally dealt by the player, including
Immolation pulses and Fire Bolt's direct damage. The fire archetype targets
high damage throughput. The prototype boosts only fire damage attributed to
the player; ordinary native burning ticks remain unchanged because they do not
reliably retain caster ownership. Fire Bolt's direct hit and owned Immolation
pulses benefit, but burns caused by Fire Bolt or Flame Cloak do not gain boosted
tick damage. The active lasts six seconds (120 game ticks) at base, scaled by
the caster's existing skill-duration multiplier. Activation costs 40 Mana at
base and has a 30-second (600-game-tick) base cooldown, using the existing
resource-cost and cooldown multipliers.

#### Threefold System
Minimum character level: 15.

Design inspiration: combine the ideas behind Path of Exile's Elemental Overload
and Elemental Equilibrium; see [Grinding Gear Games' explanation](https://www.pathofexile.com/forum/view-thread/3185299).

Implemented Rank 1 follows the agreed elemental-reserve design direction:
fire builds ice/lightning, ice builds fire/lightning, and lightning builds
fire/ice. While assigned, bank 25% of personally attributed actual elemental
damage into each other reserve, capped at six damage points each. Individual
contributions expire after ten seconds. Health, absorption and Fortitude count
once; misses, overkill, allies and unattributed burning do not generate power.

Activate instantly for 10 Mana and a 25-second cooldown. Consume all reserves
for an eight-second self buff, granting each element up to +30% damage according
to its reserve fullness. Bonuses snapshot at activation and combine additively
with existing outgoing bonuses. Banking pauses while active and resumes after
expiry. New elemental projectiles capture their bonus when fired.

An empty bank or existing buff rejects activation. Banking requires assignment;
removing its last slot clears both reserves and buff. All runtime clears on death,
logout and class/dimension change. Server datapacks tune Rank 1 through
`data/covenant/skills/weaver.json`; higher ranks are deferred. See
`docs/RPG_SKILLS.md` for rolling expiry, attribution and damage snapshots.

#### Ice Tomb
Minimum character level: 20.

Planned behavior: encase the player in ice, granting temporary invulnerability
while preventing movement, attacks, and casting. While active, the tomb slows
nearby enemies and damages them over time. Pressing the skill keybind again
releases the tomb early; the original cooldown still applies. Tomb duration,
area radius, slow strength, damage and tick interval, Mana cost, and cooldown
are deferred.

#### Firestorm
Minimum character level: 20.

Planned behavior: one cast starts a fixed-duration rain of fiery impacts over an
aimed ground area, igniting enemies hit. The player can move and cast other spells
while the storm runs. Area size, impact damage and frequency, burn duration,
storm duration, Mana cost, and cooldown are deferred.

#### Weaver's Web
Minimum character level: 20.

Planned behavior: place a temporary web field at an aimed location. Enemies are
briefly rooted when they enter, preventing movement while still allowing attacks.
Placement range, field size and duration, root duration, Mana cost, and cooldown
are deferred.

#### Enchanting Aura
Minimum character level: 25.

Planned recipients: the player and nearby allies.

Passive: grant +20% Mana regeneration to the caster and nearby allies while
assigned, using the existing Mana-regeneration attribute and shared six-block
range and coverage rules. Maximum Mana is unchanged. Recipients whose class
does not use Mana receive no resource benefit; this does not enable inactive
class resource recovery.

Active: temporarily add two points of magic damage (one heart) per eligible
regular melee hit or ordinary bow/crossbow hit, independent of weapon damage.
For player melee attacks, scale the added magic portion by vanilla's pre-hit
attack strength, as vanilla scales enchantment bonus damage: two points times
attack strength in [0, 1]. Fully charged attacks receive the full two points;
rapid weak swings receive less. Capture attack strength before its reset.
Ordinary bow shots scale their bonus by bow draw strength, reaching two magic
damage at full draw. Ordinary crossbow shots receive the full two-point bonus.
Capture the scaled projectile bonus when fired.
Each ordinary projectile applies its captured bonus once per enemy it
successfully damages. Vanilla piercing can carry the bonus to multiple enemies;
each multishot projectile carries its own captured bonus. Skills, including
skill-created arrows, remain excluded.
Skills do not receive this added damage. The original hit must deal positive
damage to health, ordinary absorption, or Fortitude's shield to trigger the
bonus. Dodged, fully blocked, or otherwise fully negated hits grant no bonus
damage. Ordinary bow/crossbow projectiles capture Enchanting's bonus when fired:
they retain it if they hit after the buff expires, and already-airborne shots do
not gain it from a later activation. Captured projectile bonuses are committed
shot effects, not lingering recipient buffs; ending aura coverage or cleaning up
the source buff does not retroactively remove them. The added damage follows
vanilla magic defenses: bypass armor, armor toughness, and shield blocking;
respect Protection enchantments, Resistance, ordinary absorption, and actual
invulnerability. Fortitude's separate shield also absorbs it. Do not roll a
second Dodge or blocking check, and apply the added portion without suppression
by the original hit's hurt-immunity window or extending that window. Both the
successful original hit and its added magic portion must apply; a naive second
ordinary damage call must not let hurt immunity discard the magic portion.
The active buff lasts six seconds (120 game ticks) at base, scaled by the
caster's existing skill-duration multiplier. Activation costs 40 Mana at base
and has a 25-second (500-game-tick) base cooldown, using the existing
resource-cost and cooldown multipliers.

#### Beacon
Minimum character level: 25.

Planned behavior: mark the player's current position with a temporary beacon.
A second activation teleports the player back to it, with one return allowed per
beacon. Staying close to the beacon reduces Mana costs. Returning leaves that
cost-reduction area active until the beacon's duration expires. Beacon lifetime,
cost-reduction radius and strength, return range, Mana cost, and cooldown are
deferred.

#### Elemental Golem
Minimum character level: 25.

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
Minimum character level: 30.

Planned behavior: after a wind-up, release one large magic-damage burst centered
on the player. No elemental charges are required. Wind-up duration, area radius,
damage, Mana cost, and cooldown are deferred.

#### Blizzard
Minimum character level: 30.

Planned behavior: one cast starts a fixed-duration storm over an aimed area,
dealing cold damage over time and slowing enemies. Enemies remaining inside
long enough are briefly frozen. The storm continues after casting. Area size,
damage and tick interval, slow strength, exposure needed to freeze, freeze
duration, storm duration, Mana cost, and cooldown are deferred.

#### Scorching Ray
Minimum character level: 30.

Planned behavior: hold the skill keybind to channel an aimable beam that pierces
enemies but stops at solid blocks, hitting aligned enemies within its range. It
deals fire damage over time, ramping up while kept on a target. The player must
stand still while channeling; moving cancels the beam, while adjusting aim is
allowed. Mana is spent per channel tick, and Mana
regeneration pauses until the channel ends. Releasing the key or having
insufficient Mana stops the beam. Beam range and width, damage, ramp rate and
cap, Mana cost per tick, and cooldown are deferred.
