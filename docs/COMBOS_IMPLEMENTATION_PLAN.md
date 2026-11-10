# Combo skills: implementation plan

Status: both playable prototypes and shared foundation implemented. Build and
62 required dedicated GameTests passed on 2026-11-05. Client movement feel,
latency, and HUD layout remain for manual playtesting. Numerical values remain
prototype tuning.

This plan covers the shared staged-activation foundation and playable prototypes
for Lunging Combo and Rip and Rush. Their existing behavior is recorded in
[Skill Trees Brainstorm](<../Project-Covenant/Skill Trees Brainstorm.md>).
Rip and Rush retains Lunging Combo as its learning prerequisite. Preserve both
stable skill IDs and the existing free allocation and eight-slot loadout.

## Agreed activation contract

Each combo uses one assigned skill key. Each fresh press requests the next stage
of that skill's fixed three-stage sequence; players cannot select or skip stages.
Holding the key does not automatically advance the combo. The logical server
owns progression; the client requests activation without supplying a stage.

Retain the brainstorm's two-second first follow-up window and three-second
second follow-up window. Each performed stage spends its own Rage cost; unused
follow-ups cost nothing. Each skill has one cooldown, beginning when its final
stage completes or its follow-up window expires. Lifecycle interruptions below
also end affected combos and start their cooldowns.

Capture the combo's effective cooldown duration, including the existing cooldown
multiplier, when its first stage is accepted and starts. Retain that duration
through the sequence; completion, timeout, or cancellation starts the cooldown
using that captured value at the actual ending time. Later equipment or modifier
changes do not recalculate it. Rejected initial activations capture nothing and
start no cooldown.

Calculate each stage's Rage cost and damage from the player's current equipment
and stats when that stage is accepted and begins, using existing cost scaling
and damage rules. Do not snapshot these values for the whole combo at stage one.
Equipment changes and buffs between stages may change later costs and damage;
each stage must independently pass its current resource check. Retain existing
application snapshots for effects already committed by a performed stage, such
as bleed stacks. The whole-combo cooldown snapshot remains the separate rule above.

The next stage becomes available only when the current stage finishes. Start
the full two-second window at stage-one completion and the full three-second
window at stage-two completion. Instant attack stages complete immediately;
movement stages complete when their lunge or dash ends. Movement time does not
consume that skill's next follow-up window. Requests for the same combo's next
stage before the current stage finishes are rejected without spending or queueing.
Other combos' already-open windows continue on their own original deadlines.

Follow-up windows are fixed at 40 and 60 game ticks (two and three seconds at
normal game speed). Skill-duration bonuses do not scale these input windows.
Keep follow-up deadlines separate from durations of stage-applied statuses and
buffs so their own duration rules cannot lengthen or shorten the combo windows.

Ordinary attacks and other non-combo skills do not cancel combo progression.
Follow-up timers continue running during those actions; they are not paused or
refreshed. Players may use ordinary attacks to generate Rage between stages,
subject to the existing resource rules. This does not bypass normal activation
checks, such as rejecting skill use while another skill is casting.

Active lunge/dash movement does not itself block ordinary attacks or stationary
stages belonging to other skills. Subject to normal activation checks, Rip and
Rush's circular Rend can execute while Lunging Combo's lunge is still moving
the player. The moving combo's own next stage remains unavailable until its
movement finishes. Keep movement execution distinct from the global pending-cast
lock so interleaving does not accidentally become an all-skill lockout.

Multiple different combo skills may be in progress at once. Lunging Combo and
Rip and Rush each retain their own stage and follow-up deadline; advancing one
does not pause, refresh, reset, or advance the other. Completion or timeout starts
only that skill's cooldown. Both spend from the player's existing shared Rage
balance. Progress belongs to the skill, not its assigned slot: duplicate bindings
activate the same sequence and do not create extra independent copies.

A rejected follow-up request spends no Rage, performs no stage, and preserves
the combo's current progression and original deadline. The player may retry
before that deadline, including after recovering Rage. Rejection does not refresh or pause the window or independently start
a cooldown; ordinary window expiry still ends that combo and starts its cooldown.

Once a stage is accepted and performed, its Rage remains spent and progression
advances even if there are no targets, or blocking, Dodge, or invulnerability
prevents its attack damage. No skill rejects activation for lack of targets.
Empty attacks spend normally, complete the stage, and follow normal cooldown
rules; range, cover, and enemy eligibility determine hits rather than activation.
Reaching a later stage does not require damaging an enemy with an earlier stage.
This differs from a rejected activation, which performs no stage. Advancement
alone does not establish whether a stage's secondary status effects apply; those
effects retain their separately specified hit and defense requirements.

Freeze does not end open combos or immediately start their cooldowns. It prevents
stage activation under the existing frozen-player validation, while each original
follow-up deadline continues running without pause or refresh. If freeze ends
before a deadline, the player may continue that combo; if the deadline passes
during freeze, the combo expires and starts its cooldown normally. Preserve the
existing freeze interruption of pending Covenant casts separately from combo
follow-up windows.

Root rejects movement-stage activation: Lunging Combo's initial lunge and Rip and
Rush's second-stage dash cannot start while rooted. Stationary attack stages
remain usable, subject to normal activation checks. A rejected movement follow-up
spends nothing, preserves progression and its original deadline, and cannot be
skipped; the player may retry after root ends if time remains. Root does not
itself cancel open combos or pause their timers. Rejecting the initial lunge
opens no combo and starts no cooldown.

Only one skill-driven lunge or dash may execute on a player at a time. Starting
another movement stage while one is underway is rejected without spending Rage,
advancing progression, or replacing the current movement. Existing follow-up
deadlines keep running. Do not queue the rejected request; the player must press
again after movement finishes if time remains. A rejected initial movement stage
opens no combo and starts no cooldown. Independent combo progression does not
permit overlapping lunge/dash execution.

Normalize the launch direction once when the movement stage starts. Floor/ceiling
contact flattens pitch at the current speed while retaining the captured horizontal
heading. Wall contact removes blocked horizontal components without renormalizing
the remainder: blocked forward travel must never become a full-speed left/right
slide. Glancing walls retain only their original tangential speed. Apply vertical
flattening before wall clipping at simultaneous contacts. Spend only the unused
fraction of the current movement tick along the surviving direction. Contact alone
does not complete the stage. If no tangent remains (a direct impact/full blockage),
stop early and complete the stage with Rage spent and the full next-stage window
opening then, even with almost no travel. Never pass through solid terrain.

Lunges and dashes use the player's full three-dimensional look direction,
captured when the movement stage starts. Both yaw and pitch determine direction,
including upward and downward travel; do not flatten movement onto the horizontal
plane. Turning during movement does not steer or retarget that movement. Solid
terrain redirects motion along floors, walls, and ceilings; full blockage stops it.

Each movement stage supplies its own fixed travel distance and travel time.
Movement-speed and skill-duration bonuses do not change either value, so they
do not accelerate or extend the lunge/dash. Collision and the interruption rules
below can still shorten actual travel. Agreed per-skill values are recorded below.

Suspend gravity during the brief lunge/dash so its path follows the captured
3D direction in open space, with collision-based surface sliding. Restore normal gravity when movement
ends, including an early terrain stop. Gravity suppression belongs only to the
movement stage, not its subsequent follow-up window; cleanup must not leave it
active after the movement ends.

End the skill's movement momentum when the lunge/dash ends, including an early
terrain stop. The player does not coast beyond the intended travel distance on
residual dash propulsion. An upward dash stops propelling the player upward at
stage completion, and normal falling resumes.

Starting a lunge/dash replaces the player's existing velocity with the skill's
movement. Do not add prior sprinting, falling, or knockback momentum to its path,
and do not restore that old velocity afterward. This allows activation to cancel
momentum already present, while new incoming knockback during movement follows
the interruption rule below. Fall-distance handling is specified separately below.

Lunges and dashes break the player's fall. Clear accumulated fall distance when
movement starts and keep it cleared during the movement stage, including a
movement-ending landing. Resume normal fall tracking when movement ends, whether
normally or early through collision, knockback, root, or freeze. Later falling
can deal ordinary fall damage; protection does not extend into follow-up windows.
This allows a well-timed movement stage to save the player from a dangerous fall.

Incoming knockback that takes effect ends an active lunge/dash early. Stop skill
propulsion, restore normal gravity, and let the knockback apply normally; movement
cleanup must not erase the incoming impulse. The performed stage keeps its Rage
cost and counts as completed, opening the full next-stage window at interruption.
The combo is preserved rather than cancelled. Cover native knockback and Covenant
Blast Trap's explicit push when implementing this behavior.

Gaining root or freeze during an active lunge/dash ends skill movement early.
Stop dash propulsion, restore normal gravity, retain the spent Rage, and count
the stage as completed, opening the full next-stage window at interruption.
Do not cancel combo progression. Freeze then rejects stage activation until
thawing; root permits stationary attack stages but rejects movement stages.
The newly opened window runs normally during either control effect, without
pause or refresh. Other combos' already-open deadlines remain unchanged.

## Agreed lifecycle contract

Death, logout, dimension change, or class change ends every unfinished combo
belonging to that player and immediately starts each affected skill's cooldown.
Keep all Rage already spent; unused stages cost nothing and there is no refund.
Apply this to both executing stages and open follow-up windows. Stop any active
skill movement and restore normal gravity without opening another follow-up.

Combo progression and movement state are transient: do not save them, copy them
to replacement players, or resume them after reconnecting or travelling. Store
committed cooldowns in the existing persistent skill-state attachment and retain
its existing clock and death-copy behavior. Cleanup must commit each affected
cooldown once, before discarding runtime state or copying saved state to a
replacement player; it must not restart cooldowns for combos already ended.

Unlearning an unfinished combo ends that combo and immediately starts its
cooldown, with no refund or charge for unused stages. Stop any movement owned
by that combo and restore normal gravity without opening another follow-up.
Apply this to every combo actually deallocated, including Rip and Rush when
Lunging Combo is removed as its prerequisite. Leave other still-learned combos
intact. Relearning does not erase or restart an existing cooldown. Unlearning
an idle combo does not create a new cooldown.

Changing or clearing a slot assigned to an unfinished combo cancels that combo,
even if another slot still holds the same skill. Immediately start its cooldown,
keep spent Rage, charge nothing for unused stages, and stop its active movement
with normal gravity restored and no further follow-up. Reassigning the skill
does not reset the cooldown. Other combos whose bindings were not changed remain
intact. A rejected edit or a request that leaves the assignment unchanged does
not cancel a combo; editing an idle combo's binding creates no new cooldown.

Keep binding-change cancellation within Covenant's own skill-slot GUI and
existing server assignment checks. Physical key remaps in Minecraft Controls
do not change combo progress. Do not add Controls monitoring or a remap
notification packet.

## Agreed prototype feedback

The 2026-11-09 polish follow-up replaces the temporary text lines with top-left
native skill icons, stage numbers and remaining follow-up time. Interleaved combos
have separate icons and countdowns. Movement stages show their current stage
without a follow-up countdown until the window opens. Hover tooltips name the
skill and next/current action. Remove ended icons on completion, expiry or
cancellation. Cooldown gameplay remains independent of this presentation.

Sync only the owning player's minimal display state on combo transitions and
let the client display remaining time from server deadlines. Do not send a
countdown update every tick or let the display advance authoritative stages.
Integrate with existing HUD spacing/F1 behavior; status icons occupy the top-left
and the class resource bar retains its original position. Rendered layout remains
subject to user playtesting.

## Implementation direction

### Movement synchronization

The initial server-teleport stream caused choppy movement in the user's client
playtest. Normal travel now uses a single owner-only `DashMotion` start trajectory
and stop result, with native collision steps on the server and owner prediction
on the client. The client advances current position through native `move`, leaving
previous position intact for normal frame interpolation; there is no per-tick
teleport or owner trajectory update. Starting position error is distributed over
the brief predicted path. Small normal endpoint differences converge over two
client physics ticks. Knockback stop results release client control immediately.

The server still owns distance/time, terrain stops, hit sweeps, interruptions,
resources, cooldowns, and progression. Predicted position packets cannot move the
server during the stage or rewind its endpoint afterward. `DashFinished` only
acknowledges the current completed sequence and reports an endpoint for validation;
it never supplies an authoritative destination. Matching endpoints reset vanilla's
movement anchor and resume ordinary packets; stale/executing-stage acknowledgements
are ignored. A mismatch or missing acknowledgement after 40 ticks uses one native
correction preserving current velocity, then releases the packet hold. Entity
removal clears the hold, and client state is scoped to the player/dimension.
Existing slot intent and physical key behavior are unchanged. Rendered smoothness
and actual network latency still require user playtesting.

Every authoritative step calls `connection.resetPosition()` after native movement,
without sending a packet. In the pinned version, `connection.tickPlayer()` caches
`firstGood` coordinates, calls `ServerPlayer.doTick()`, then restores the player to
that cache. Updating only at the final acknowledgement allowed the connection
tick to undo every server step, while the client predicted the full dash. Keep
the per-step anchor update and test through the full `connection.tick()` wrapper;
calling `doTick()` or posting a player-tick event alone cannot catch that rewind.

### Activation and cleanup

Reuse the existing slot intent packet and shared command/slot activation boundary.
Slot assignment edits already reach the server through `SkillLoadouts.bind`;
enforce cancellation there after a validated actual assignment change. Keep
repeated cancellation harmless and preserve existing cooldowns rather than
restarting them. Use the existing assignment and activation packets without
adding a physical-key change protocol.
`Skills.execute` retains the ordinary immediate-cooldown path; `Combos` implements
the staged path with delayed cooldown commitment. `SkillRuntime` owns separate
per-skill progression and one shared movement, alongside existing cast state.
Progression and movement remain server-owned and transient there, separate from
saved resource/cooldown state and permanent profile or item data. Extend death,
logout, dimension, and class-change cleanup using the lifecycle contract above.
Represent progression separately per combo skill rather than using one active
combo field. Verify that interleaved advancement and independent timeout leave
the other skill's progress and cooldown unchanged.

## Ordered foundation checklist

Implement these pieces in dependency order. Use the existing named skills and
small stage definitions; do not build a general combo scripting system. Both
production nodes now use the concrete per-skill behavior and tuning below.
The foundation and playable skill integration are implemented together.

### 1. Server progression and activation

- [x] Add bounded per-skill transient progression to `SkillRuntime`: executing
  stage or waiting follow-up, next stage, deadline, and captured cooldown duration.
  Keep one separate player movement execution shared by active combos.
- [x] Route combo commands and slot requests through the existing authoritative
  activation checks. Validate current class, learning, availability, cooldown,
  resource, and stage-specific requirements before spending or advancing.
- [x] Commit each stage's current cost once before effects. Preserve rejected
  requests, defended-hit advancement, and the existing activation throttle.
  Do not let damage callbacks re-enter and spend or advance the same stage twice.
- [x] Open fixed 40/60-tick windows only at preceding-stage completion. Handle
  expiry before accepting another follow-up; never restart stage one from an
  expired sequence without applying its cooldown first.
- [x] Use one ending path for completion, timeout, and cancellation, committing
  the captured cooldown exactly once. Maintain deadlines on active player state;
  add no world-wide entity scan, chunk forcing, or offline progression storage.

Acceptance: interleave both skills, expire one while advancing the other, retry
rejections without extending windows, and exercise duplicate bindings and rapid
requests. Check exact deadline boundaries, per-stage resource changes, defended
attacks, equipment changes between stages, and the stage-one cooldown snapshot.

### 2. Shared 3D movement execution

- [x] Check the resolved 26.1.2 movement/collision APIs before choosing hooks.
  Reuse native collision and synchronization; keep position and completion under
  server control, including normal client movement-packet reconciliation.
- [x] Capture full look direction and replace prior velocity. Use stage-supplied
  fixed distance/travel ticks, suppress gravity during travel, and prevent steering
  from turning or ordinary movement input. Do not let speed/duration stats scale it.
- [x] End on travel completion or fully blocked collision, stop dash propulsion, restore
  gravity, clear movement ownership, and notify the owning combo once. Do not
  overwrite knockback that caused an early stop or replay pre-dash velocity.
- [x] Reject overlapping movement and the moving combo's premature follow-ups,
  while allowing ordinary attacks and other skills' stationary stages.
- [x] Integrate root, freeze, native knockback, and Blast Trap pushes with the
  agreed early-completion rules. Clear fall distance during movement, including
  the final landing, then resume normal fall tracking on every exit path.

Acceptance: horizontal/upward/downward travel, turning/input during movement,
fixed travel under speed/duration modifiers, wall/floor/ceiling sliding, and
almost-zero-distance completion. Check early stops from root/freeze/knockback,
preserved incoming impulse, safe dash landings, and subsequent ordinary falls.

### 3. Lifecycle and loadout cleanup

- [x] Extend death/logout/dimension/class cleanup to cancel every unfinished
  combo before runtime is discarded or persistent state is copied.
- [x] Extend `SkillRuntime.cancelSkill`/deallocation cleanup for each actually
  removed combo, including dependent Rip and Rush removal.
- [x] Cancel the affected unfinished combo on validated, actual slot edits in
  `SkillLoadouts.bind`, including edits to duplicate assignments. Retain cooldowns
  across relearning/reassignment. Add no physical-key monitoring or new intent packet.
- [x] Make cleanup safe to repeat. A lifecycle cancellation must stop movement
  without the ordinary movement-completion callback opening a fresh window.

Acceptance: death/respawn, logout/reconnect, dimension/class changes, unlearning
either skill or its prerequisite, GUI reassignment/clearing, duplicate assignments,
and rejected/unchanged edits. Verify unaffected combos stay intact, idle skills
gain no cooldown, saved cooldowns survive reload, and no movement/gravity state leaks.

### 4. Owner display and handoff

- [x] Add minimal unsaved owner-only combo display sync on transitions. Include
  enough server timing information to render each independent deadline correctly.
- [x] Add temporary text indicators using existing HUD conventions. Show movement
  versus waiting stages correctly, remove ended rows, respect F1, and preserve
  resource/Fortitude/air/mount spacing. Defer icons and circular cooldown displays.
- [x] Add focused dedicated GameTests for progression, movement, cleanup, and
  saved cooldown behavior. Keep fixtures out of the production JAR.
- [x] Run `scripts/Dev.ps1 build`, `scripts/Dev.ps1 test`, and `git diff --check`
  after implementation. Update implemented-scope and validation documentation
  with actual results, then hand off manual movement/HUD checks to the user.

Acceptance: owner-only state, independent interleaved countdowns, no countdown
packets every tick, and clean display removal on every end path. Automated checks
do not establish movement feel, latency quality, or rendered HUD layout; the user
checks those in the client. Do not launch/automate a client for routine validation.

## Agreed Lunging Combo prototype

Stage one is a movement-only lunge: four blocks over three game ticks (0.15 seconds
at normal game speed), with no attack damage. It follows the captured full 3D
look direction and all shared collision, interruption, gravity, momentum, and
fall-distance rules above. Movement-speed and skill-duration bonuses do not
change its distance or travel time. The first follow-up window opens when the
lunge actually ends, including an early stop.

Stage two uses Rend's block-clipped crosshair targeting and native held-item melee
reach, with the existing enemy eligibility and line-of-sight checks. With no
valid target, perform a paid whiff and advance normally to stage three.
The strike deals 125% of the player's current attack damage at stage acceptance,
using the existing melee-skill damage rules and applicable damage modifiers.

Both the stage-two strike and stage-three finisher are instant, with no cast
time or added wind-up, matching Rend and Cleave. They retain shared activation
checks and throttling; instant execution does not bypass those rules.

Stage three uses Cleave's area: a 120-degree frontal horizontal arc within three
blocks, with line of sight required. Retain Cleave's existing enemy eligibility
checks and yaw-based arc calculation. Deal 100% of the player's current attack
damage per enemy at stage acceptance, using existing melee-skill damage rules
and applicable damage modifiers.

An empty finisher area performs a paid whiff and completes the combo, starting
its captured cooldown. Defended hits also count as performed stages, but neither
whiffs nor hits dealing no damage apply shred. Cleave and Rend follow the same
no-target-rejection policy: spend and start their cooldown even on a whiff.

The finisher's armor shred reduces armor by 20%, leaving armor toughness
unchanged. Percentage-based shred and its strength are provisional prototype
tuning, to be revisited when enemy armor adjustments are introduced.
Its provisional base duration is 100 game ticks (five seconds), scaled by the
caster's skill-duration multiplier captured when the finisher starts. Later
modifier changes do not alter an already-applied shred's duration.
Apply shred only after the finisher deals positive damage to the target's health,
ordinary absorption, or Fortitude shield. Blocked, dodged, or invulnerable hits
apply no shred. Resolve the finisher's damage against the target's existing armor
before applying this hit's shred; the new application benefits subsequent hits.
Each target has one shared armor-shred debuff for Lunging Combo and Rip and Rush,
including applications from different players. Its strength does not stack:
repeated applications remain at
20% and set expiry to the later of the current expiry and the new application's
expiry. A shorter application never shortens an existing longer one.
Once applied, shred belongs to the target and remains for its remaining duration
if the caster dies, logs out, changes class, or unlearns the skill. Caster cleanup
does not remove an already-applied shred from surviving targets.
Save the target's remaining shred duration across save/load and reconnects.
Its timer pauses while the target is unloaded or offline, then resumes when the
target ticks again. Target death clears shred; do not copy it to a respawned
replacement player. These target-owned timers remain separate from transient
combo progression and the existing persistent cooldown clock.
Treat shred as a normal harmful status: milk and normal effect-clearing can
remove it, and native effect-immunity/applicability checks can reject it. A
damaging finisher does not guarantee shred against a status-immune target;
rejected status application does not undo damage, spending, or stage completion.

Base Rage costs are 20 for the lunge, 10 for the single-target strike, and 20 for
the finisher (50 total if all three stages are performed, before cost modifiers).
Each accepted stage applies the player's current resource-cost multiplier and
spends only its own cost, following the shared per-stage resource rules above.

Base cooldown is 240 game ticks (12 seconds), reflecting the combo's extended
sequence and combined mobility, damage, and armor-shred power. Capture its
effective duration with the cooldown multiplier when stage one starts, then
start that captured cooldown at completion, timeout, or cancellation under the
shared rules above.

These prototype rules are implemented; automated and manual validation are
reported separately in `docs/VALIDATION.md`.

### Lunging Combo acceptance criteria

- [ ] Observe four-block/three-tick movement without damage in open space; verify
  early terrain/control/knockback completion and shared movement cleanup rules.
- [ ] Verify instant 125% single-target strike and instant 100% finisher per
  eligible enemy in the three-block/120-degree visible frontal area. Empty
  attacks spend and advance; empty finishers start the captured cooldown.
  Defended performed hits still advance. Check 20/10/20 base costs and current modifiers per stage.
- [ ] Verify shred applies after positive health/absorption/Fortitude damage,
  reduces armor by 20% without changing toughness, and lasts 100 base ticks with
  a finisher-time duration snapshot. Check shared non-stacking refresh, native
  immunity and cleansing, target persistence/death, and caster-independent expiry.
- [ ] Verify the 240-base-tick cooldown is captured at stage one and committed
  once at completion, timeout, or cancellation. Exercise interleaving and all
  shared lifecycle/loadout cleanup without cooldown restarts or movement leaks.

## Agreed Rip and Rush prototype

Stage one's circular Rend covers all directions within a three-block radius
around the player, with line of sight required. Retain existing enemy eligibility
checks and the existing melee-area distance check, without Cleave's frontal arc
restriction.

The opening circular Rend and final circular AoE are instant attacks, with no
cast time or added wind-up, matching Lunging Combo's attacks. Retain normal
activation checks and throttling; the dash remains an executing movement stage.

Apply the existing Rend damage/bleed behavior once per eligible enemy: deal 100%
of current attack damage with existing melee-skill modifiers, and apply one
normal bleed stack only after positive health, absorption, or Fortitude damage.
Retain Rend's defense handling and its guard against an additional Ripping Aura
bleed on the same Rend hit. Base bleed power is 10% of application-time attack
damage per second for 100 base ticks (five seconds), using existing bleed-power
bonuses and skill-duration scaling captured when the stage begins. Use the
target-owned shared three-stack cap, duration refresh, oldest-stack replacement,
attribution, and persistence rules unchanged.

Stage two dashes six blocks over four game ticks (0.2 seconds at normal game
speed), following the captured full 3D look direction and shared movement rules.
Movement-speed and skill-duration bonuses do not change its distance or travel
time. Full terrain blockage and agreed interruptions can end the dash early; its
full third-stage window opens when movement actually ends.
Dash damage is 50% of current attack damage captured when the stage begins,
using existing melee-skill damage rules and applicable damage modifiers. Allow
at most one hit attempt per enemy per dash, including attempts prevented by
defenses; remaining in contact does not retry or deal repeated hits.
Contact requires an eligible enemy's hitbox to intersect the player's body swept
along the actual travelled path. Use the full 3D path, without an added hit radius;
nearby enemies outside that swept body are not contacted. Clip contact checks to
each actual terrain-limited segment of the sliding path, including partial segments, so the dash
cannot hit enemies beyond an obstacle that stopped movement.

The dash applies the same provisional 20% armor shred as Lunging Combo, leaving
armor toughness unchanged, for 100 base ticks (five seconds). Capture its scaled
duration using the caster's skill-duration multiplier when the dash stage begins.
Apply only after positive health, absorption, or Fortitude damage, resolving the
dash hit against existing armor before adding shred. Retain all shared shred
cleansing, native immunity, target persistence/death, and caster-independent
expiry rules above. Both skills and all players refresh one target-owned debuff;
strength never stacks, and expiry is the later of the existing and new expiry.

Stage three's circular finisher matches the opening area: all directions within
a three-block radius around the player's current position when activated, with
line of sight required. Retain the same enemy eligibility and melee-area distance
checks; do not anchor the area to the dash's starting or ending position if the
player has moved before activating the finisher.
The finisher deals 100% of current attack damage per eligible enemy, captured
when that stage begins, using existing melee-skill damage rules and applicable
damage modifiers.
On each enemy qualifying for finisher consumption, remove all shared bleed
stacks, including those applied by other players. Count each enemy once toward
the movement-speed reward regardless of how many stacks it carried. Reuse the
existing shared bleed-consumption boundary; this can spend teammates' remaining
bleed damage and reduce bleed-dependent nearby-enemy counts.
Consumption requires the finisher to deal positive health, absorption, or
Fortitude damage to that enemy. Blocked, dodged, or invulnerable hits preserve
its bleeds and grant no speed reward. Killing blows against bleeding targets
qualify: retain pre-hit bleed presence for reward eligibility even if target
death cleanup has already removed the stacks. Each qualifying enemy still counts
only once.
The finisher grants the caster +10% base movement speed per qualifying enemy
whose bleed is consumed, capped at +30% from Rip and Rush. Three enemies reach
the cap; additional stacks on one enemy do not increase its contribution. Use
the same native base-relative movement-speed modifier convention as Swiftness
Aura. This is a cap on this skill's contribution, not on total movement speed.
The speed buff's base duration is 80 game ticks (four seconds), scaled by the
caster's skill-duration multiplier captured when the finisher starts. Later
modifier changes do not alter an already-granted buff's duration. The short
base duration deliberately limits uptime.
Keep one Rip and Rush speed buff on the caster. A finisher consuming bleed from
at least one qualifying enemy replaces the existing buff with that finisher's
new strength and duration, even if weaker or shorter. Do not stack strengths or
add durations. A finisher consuming no bleeds leaves an existing buff unchanged,
without refreshing, extending, weakening, or removing it.
The speed buff is transient and unsaved. Clear it on death, logout, dimension
or class change, unlearning Rip and Rush (including dependent deallocation), or
a validated actual edit to a slot assigned to Rip and Rush, even if another
duplicate assignment remains. Rejected/unchanged edits and physical key remaps
do not clear it. Cleanup also applies after the combo has completed, without
restarting its cooldown or creating a cooldown for an idle skill.

Base Rage costs are 30 for the opening circular Rend, 20 for the dash, and 20 for
the finisher (70 total if all stages are performed, before cost modifiers).
Each accepted stage checks and spends its own cost using the player's current
resource-cost multiplier, following the shared per-stage resource rules.

Base cooldown is 400 game ticks (20 seconds). Capture the effective cooldown
duration using the cooldown multiplier when stage one starts, then start that
captured cooldown at completion, timeout, or cancellation under the shared rules.

Both circular attacks may execute with no enemies present. They spend their
stage cost and advance normally; an empty opening opens the dash window, and an
empty finisher completes the combo and starts its cooldown. Non-bleeding enemies
still take normal finisher damage but contribute no speed reward. No targets or
bleeds are required to start the dash. Skill input never rejects for lack of
targets; existing class, learning, resource, cooldown, cast, control, and movement
ownership checks remain authoritative.

### Rip and Rush acceptance criteria

- [ ] Verify instant circular Rend in all directions within three blocks, with
  cover/allies excluded from hits, normal shared bleed ownership/cap/refresh,
  and exactly one Rend bleed despite Ripping Aura.
- [ ] Verify six-block/four-tick 3D dash and one 50% damage attempt per enemy
  intersecting the swept body; check diagonal travel, early terrain stops, and
  no hits beside the path or beyond stopping cover. Damaging contacts apply the
  same shared shred as Lunging Combo without stacking it.
- [ ] Verify the current-position three-block finisher deals 100% damage, removes
  every owner's bleeds only on damaging hits, counts each enemy once, includes
  lethal hits, and preserves bleeds on defended hits. Non-bleeding enemies still
  take damage without granting speed.
- [ ] Verify +10% base speed per qualifying enemy, capped at +30%, for 80 base
  ticks with a finisher-time duration snapshot; replacement never adds duration,
  zero consumption leaves an existing buff intact, and lifecycle/slot cleanup
  removes the unsaved modifier without restarting a completed combo's cooldown.
- [ ] Verify 30/20/20 Rage, the stage-one snapshot of a 400-base-tick cooldown,
  independent interleaving, and paid whiffs at every attack stage. Run the shared
  deadline, control, lifecycle, persistence, owner-sync, and HUD acceptance checks.

## Handoff

Build, both geometry checks, all 62 dedicated GameTests, and `git diff --check`
passed. The production JAR contains combo classes/mixins and excludes test code
and fixtures. See `docs/VALIDATION.md` for automated coverage and manual limits.
Implementation checklist ticks above record completed work; the per-skill
acceptance lists also include manual observations and remain unchecked as a
client-playtest handoff.

Production nodes now execute both named prototypes. The shared and per-skill
checklists above define implementation acceptance; numerical balance, rendered
HUD layout, movement feel, and latency remain subject to user playtesting.
