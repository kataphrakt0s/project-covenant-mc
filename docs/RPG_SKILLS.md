# Skills, class resources, and skill trees

This foundation shares the existing class/profile and native stat/Affix Page systems.
It adds nine code-backed skills, one persistent skill-state attachment, and one
unsaved entity-local cast/combat attachment. The Skills window presents the current
class's brainstorm tree with free allocation and eight saved assignments. Planned
nodes have no gameplay effects yet. Points, ranks, an action-bar HUD, pack
dependencies, and an alternate character system remain outside this pass.
Bleed is a separate target-owned persistent attachment; it never becomes permanent
character progression or item data.

The roster, learning prerequisites, and agreed gameplay direction are in
[Skill Trees Brainstorm](<../Project-Covenant/Skill Trees Brainstorm.md>).

## Implementation order

Implement behavior in small dependency-ordered batches, with prototype tuning where
the brainstorm defers numbers. Allocation stays free; ranks and skill points wait.

1. **Basic attacks (current batch):** native three-target Piercing Shot, frontal
   Cleave, and Aimed Shot using the existing cast pipeline. Arcane Bolt remains
   the basic spell projectile.
2. **Simple targeted attacks and defense:** Execute, Lightning Bolt, Mage Armour,
   and Arcane Explosion; then movement skills such as Disengage and Teleport.
3. **Statuses and periodic damage:** root, slow, bleed stacks, poison, burning,
   and vulnerability before skills which apply or consume them.
4. **Buffs and auras:** establish recipient/alliance rules, timed buffs, and
   slot-driven passives before the individual auras and shouts.
5. **Persistent areas, traps, and multi-hit attacks:** reuse established targeting
   and status behavior; add per-enemy hit intervals and ownership/replacement rules.
6. **Channels, companions, and compound skills:** held-input lifecycles, pet
   persistence/AI, elemental reserves, and mechanics combining the earlier pieces.

This is a dependency guide rather than a promise to implement an entire stage in
one pass. Rend and the bleed foundation were prioritized next for Reaver's
dependent skills. Skills whose requirements span stages follow their latest dependency.

## Trap contract

The dependency-ordered handoff and per-stage acceptance criteria are in
[Traps implementation plan](TRAPS_IMPLEMENTATION_PLAN.md). The rules below are
implemented prototype behavior; presentation and live-client checks remain manual.

Bear Trap, Frost Trap, Flame Trap, and Blast Trap are playable. Their
agreed placement is a fixed short toss, approximately three blocks along the
player's horizontal facing direction, independent of vertical crosshair aim.
Each player may have one armed trap of each type; another throw of the same
type replaces that player's previous armed trap of that type. Different types
can coexist.

Each player may also have one active Frost patch and one active Flame patch.
A new throw can arm while its owner's existing patch continues. Triggering
the new trap replaces that owner's previous active patch of the same type;
throwing or arming alone does not replace it. Frost and Flame patches coexist,
and different players have independent limits.

Owner death, logout, dimension change, or class switch removes that owner's
deployed traps and active patches. Unlearning a trap skill removes only that
skill's deployed traps and patches belonging to that owner.

Traps and active patches disappear when their containing chunk unloads. They
never force chunk loading and do not return on chunk reload. Unloading can
remove an untriggered trap before its 30-second armed lifetime expires.

Focus spending and cooldown commitment occur on a successful throw, not on
triggering. Expiry, replacement, and cleanup do not refund either. Rejected
activations spend nothing and do not start a new cooldown, consistent with
the existing server activation boundary.

Trap activation requires learning and Focus, with no consumable item,
ammunition, or held-weapon requirement. A bow, another weapon, and an empty
hand are all valid; do not add a trap item or crafting supply system for this pass.

For this prototype, damaging traps (Bear, Flame, and Blast) each have their own
base damage values. They do not scale from native `minecraft:attack_damage` or
the held weapon, so switching weapons and weapon damage upgrades do not change
trap damage. This adds no new damage attribute or scaling system.

Use the existing `skill_duration` multiplier for Bear's root and the active
Frost and Flame patch durations. The 0.5-second arming delay and 30-second
untriggered lifetime remain fixed. At +20% duration, the root lasts 3.6 seconds,
Frost lasts 9.6 seconds, and Flame lasts 7.2 seconds. Resource cost and cooldown
continue to use the existing activation pipeline's native skill multipliers.

Snapshot the owner's effective duration at successful throw time. Store the
resulting root or patch duration with that deployment. Equipment or stat
changes after the throw do not alter it, and triggering does not recalculate it.

Shields cannot block trap damage or prevent trap effects. Apply this shared
rule to all four traps, including triggered areas and explosions.

Solid walls and floors block Frost, Flame, and Blast effects even when a target
is within the listed radius. Area checks must respect solid cover, including
floors between the trap and targets on another level.

Use temporary skill visuals and effects without changing blocks. Frost does
not freeze water or place snow/ice. Flame does not place fire, ignite vegetation,
or melt blocks. Blast destroys no blocks. The visible patches show their
affected areas without terrain modification.

Do not add blanket boss immunity to Bear Trap's root or Frost Trap's slow.
Eligible bosses use the same control rules and agreed defenses as other
creatures, with invulnerability and native knockback resistance respected.
Flying creatures still need hitbox contact with the shallow ground-level
trigger region to set a trap off.

Thrown traps collide with terrain and settle onto supporting solid surfaces,
including slabs and stairs. Walls stop forward travel and let the trap drop
at their base; ledges let it fall to the surface below. Traps do not attach to
walls or float in midair, so obstacles can shorten a throw.

After landing, traps and active patches require dry, solid support at their
placement point for their remaining lifetime. Entering water or lava fizzles
a thrown trap. Flooding the placement point or removing its supporting block
removes the landed trap or patch, without refunding Focus or cooldown. A toss
over a ledge can still fall normally until its first landing.

After landing, each trap has a 0.5-second (10 game ticks) arming delay and a
visible ready cue. Traps cannot trigger in flight or during that delay. An
eligible enemy already standing on a trap triggers it when arming completes.

All players, including opponents, can see deployed traps. Give each type a
distinct appearance and a clear armed cue so placements and readiness are
recognizable. Concealment is outside this pass.

Deployed traps cannot be destroyed by attacks, disarmed, or picked up. Do not
add trap health, a disarming action, or a recoverable trap item for this
prototype. Avoidance, triggering, and expiry provide the player interactions.

An untriggered trap expires harmlessly 30 seconds (600 game ticks) after arming
unless triggered or replaced sooner. Triggered effects have separate durations.

Trigger eligibility reuses `Skills.canHit`: living, non-spectator targets other
than the owner, excluding allies and the owner's pets, with native PvP restrictions
respected. Passive animals and modded living creatures can trigger traps; there
is no hostile-mob-only filter.

All four traps trigger on hitbox contact with a shallow region above a
1.5-block-wide footprint, rather than a proximity radius. Creatures passing
clear overhead do not trigger them. The larger triggered effect areas of Frost,
Flame, and Blast are separate from this shared contact footprint.

`TrapEntity` owns flight, landed arming, armed, triggered, and removed phases.
Its native entity type is unsaved and cannot be summoned. `SkillRuntime` retains
only bounded per-owner deployment/patch references; `onRemoval` clears them and
all affected Frost memberships, including the chunk-unload removal path.
Successful entity addition precedes spending, protected against reentrant activation;
only accepted throws replace an old deployment. Triggering replaces an active patch.
Deallocating prerequisites cleans every actually removed trap skill, without clearing
cooldowns. Existing profile, resource, and loadout saved formats are unchanged.

Concrete tuning: 0.1-block forward / 0.65-block-high release; 0.3625 forward and
0.14 upward blocks/tick initially; 0.06 blocks/tick² gravity. Eight forward steps
cap clear-ground horizontal travel at three blocks; falls continue up to a 200-tick
flight safety limit. Collision size is 0.3 × 0.15 blocks. Trigger contact is a
separate 1.5-block square extending 0.25 blocks above the collision surface.
Areas measure a sphere from target feet to placement: 3.5 blocks for Frost/Blast,
three for Flame. A collider ray 0.1 blocks above both feet positions rejects cover.
Patch visuals clip against
the same terrain rays. Blast's outward/upward impulse is at most 0.65/0.22
blocks/tick before distance falloff and both native knockback resistances.

Native item models identify Bear (iron trapdoor), Frost (blue ice), Flame (magma),
and Blast (TNT). Tracked type/phase drives colored contact rings, bright armed
sparkles, and larger patch rings on each viewing client. These are provisional
visuals requiring user playtesting; no client launch was performed.

### Bear Trap

Bear Trap is single-use and damages and roots the first eligible creature to
contact it. Its root blocks self-directed walking and jumping, while allowing
turning, attacks, and casting. Gravity and knockback remain effective. A rooted
creature can therefore be pushed by Blast Trap, but cannot resume walking until
the root expires.

Prototype base values: 20 Focus, eight-second
(160-tick) cooldown, four damage points (two hearts before defenses), three-second
(60-tick) root, and instant casting (zero cast ticks). The shared 0.5-second
post-landing arming delay still applies. These values are initial playtest tuning.

Taking damage does not end the root early. This allows follow-up attacks such
as Aimed Shot while the target remains rooted and able to retaliate.

On a repeated successful Bear Trap root, keep the longer of the existing
remaining duration and the new root's duration. Do not add durations or shorten
an existing longer root. A normal three-second root hitting with one second
remaining resets it to three seconds.

Once applied, the root belongs to the target and finishes independently of
owner death, logout, dimension change, or unlearning. Persist its remaining time
with the target, pause it while the target is not ticking, and clear it on
target death, matching bleed's lifecycle. Owner cleanup still removes deployed
traps and patches; Frost's slow ends immediately when its patch disappears.

Apply the root only after the snap deals positive health or absorption damage,
matching Rend's damaging-hit status rule. A fully prevented hit still consumes
the single-use trap but does not root its target.

The snap respects armor, Dodge, general mitigation, and invulnerability, but
bypasses shield blocking under the shared trap rule. Any positive health or
absorption damage applies the full root. The Dodge hook explicitly recognizes
only Bear's deployed direct strike; Flame and Blast bypass Dodge.
`covenant:rooted` is a native saved/synced mob effect. Input propulsion and ground/
fluid jumping are blocked by narrow common Mixins, preserving existing velocity,
gravity, attacks, and casting. Native `forceAddEffect` supports eligible bosses
without bypassing effect applicability hooks. Refresh keeps the longer duration.

### Frost Trap

Frost Trap requires Bear Trap. Triggering creates a temporary patch that slows
eligible enemies only while inside it, with no lingering slow after leaving.
It deals no damage.

Prototype base values: 30 Focus, 12-second (240-tick)
cooldown, 3.5-block patch radius, eight-second (160-tick) patch duration,
50% movement slow, and instant casting (zero cast ticks). The shared post-landing
arming delay still applies.

Overlapping Frost patches, including those from different players, apply only
the strongest Frost slow. They do not stack: two normal patches still slow
by 50%. Leaving one patch preserves the slow while another eligible patch
still covers the target; leaving all eligible patches removes it immediately.

### Flame Trap

Triggering creates a temporary burning patch that repeatedly damages eligible
enemies only while inside it. It does not ignite targets or apply lingering burn
damage after they leave. This supports keeping enemies in the patch with Bear
and Frost traps.

Prototype base values: 35 Focus, 12-second (240-tick)
cooldown, three-block patch radius, six-second (120-tick) patch duration,
three damage points per enemy every second (20 game ticks), and instant
casting (zero cast ticks). The shared post-landing arming delay still applies.

Different players' overlapping Flame patches each deal damage with independent
per-enemy intervals and owner credit. Two normal patches deal six damage points
per second before defenses to a target inside both. The same-owner active-patch
replacement rule still applies. Damage scheduling must allow each owner's due
damage to land independently rather than suppressing it through another patch's hit.

Deal the first hit immediately when an eligible enemy enters, including when
the patch forms around it. Thereafter allow at most one hit per second per
enemy per patch. Retain that interval across exit and re-entry; crossing the
boundary must not create extra hits inside the same interval. No damage is
dealt while the target is outside the patch.

Flame damage is fire damage and bypasses Dodge and shield blocking. Fire
Resistance and natural fire immunity prevent it. General mitigation and
invulnerability still apply.

### Blast Trap

Blast Trap requires Flame Trap. Triggering produces one TNT-like explosion
that damages eligible enemies and knocks them outward without destroying
blocks. Knockback can push enemies out of Frost or Flame patches, including
while rooted by Bear Trap.

Damage and knockback are strongest at the center and fall toward zero at the
outer edge.

Prototype base values: 40 Focus, 15-second (300-tick)
cooldown, 3.5-block explosion radius, maximum center damage of ten points
(five hearts before defenses), and instant casting (zero cast ticks). Damage
falls off with distance as above; the shared post-landing arming delay still
applies.

Knockback primarily pushes outward with a small upward bump. The initial
playtest target is roughly three blocks of displacement near the center on
flat ground before knockback resistance. Native knockback resistance and
distance falloff reduce it. Terrain affects actual travel; three blocks is a
tuning target rather than a guaranteed distance or a measured result.

Blast damage is explosion damage and bypasses Dodge and shield blocking.
Armor and Blast Protection reduce it; general mitigation and invulnerability
still apply.

## Bleed foundation

Rend implements these rules. Dependent bleed skills remain placeholders. Bleed has a shared cap of
three stacks per enemy across all attackers. Reapplication refreshes all existing
stack durations, including at the cap. Each stack counts as one instance for
Consume and Guillotine; Blood Fever counts bleeding enemies rather than stacks.

At the cap, a new application replaces the oldest stack with its own damage and
attacker while refreshing all stack durations. This can replace a stronger stack
with a weaker one.

Each stack retains its applying attacker for damage attribution and future healing
from that stack's bleed damage. Consume and Guillotine remove every bleed stack
on affected enemies regardless of who applied it, allowing shared setup and
finishers. These agreements do not implement the dependent skills.

Rend snapshots effective native attack damage when each stack is created and scales
that stack's base bleed damage from it. Later weapon/equipment changes do not alter
existing stacks, and refreshing a retained stack changes its duration without
recalculating its base damage. A replacement stack captures the new application's
attack damage. Each stack deals 10% of that captured attack damage on each
one-second pulse, for a five-second base duration. Three equal-power stacks deal
30% per second before applicable defenses. Native `skill_duration` scales the
duration at application time; cost/cooldown multipliers retain their usual roles.

Rend's initial strike uses normal melee defenses. Bleed ticks bypass armor,
shield blocking, and Dodge, while general damage reduction (such as Resistance),
absorption, and invulnerability still apply.

Bleed ticks operate independently of normal hurt-immunity windows. Normal hits
cannot suppress scheduled bleed damage, and bleed ticks cannot suppress normal
hits or other stacks' ticks. Actual invulnerability, such as Creative mode, remains
respected.

Each bleeding enemy has one shared tick schedule at one-second intervals (20 game
ticks), with the first pulse one second after bleeding starts. Each pulse applies
every active stack's damage separately with its own attacker credit. Adding,
refreshing, or replacing stacks does not reset this schedule. A new stack joins
the next pulse even if it was applied less than a second earlier.

Bleeding persists across save/load, target disconnects, and entity unloading.
Persist stack ownership, captured base damage, remaining durations, and time until
the target's next pulse. Duration and pulse timers pause while the target is not
ticking and resume without offline damage or catch-up bursts. An attacker
disconnecting does not remove existing bleeds. Death clears bleeding; it does not
copy to the replacement player after death.

Rend applies or refreshes bleeding only after its initial strike deals positive
damage to health or absorption. Dodged, fully blocked, and immune hits do not add,
replace, or refresh stacks. Damage absorbed by absorption hearts qualifies.

All otherwise-valid living targets can bleed, including undead, golems, bosses,
and modded living creatures. No creature-type bleed immunities are added in this
implementation; ordinary targeting rules and actual invulnerability still apply.

`covenant:bleed` is a saved, server-only attachment on each affected living target.
It stores at most three ordered entries (attacker UUID, captured damage per pulse,
remaining game ticks), plus the shared relative pulse countdown. Entity tick events
check for this attachment without creating state on unaffected targets. Active
timers mutate in place; only a pulse takes a bounded snapshot for reentrant damage
hooks. Expiry and death remove the attachment. Non-death entity replacement retains
it through NeoForge's native serialization copy; death replacement does not.
No existing attachment format or ID changes, and old saves start without bleeding.

The data-driven damage type `covenant:bleed` bypasses armor, shields, and native
hurt cooldown through damage-type tags, with no direct melee/projectile actor for
Dodge to evade. It retains a causing player when online and saved UUID ownership
when offline, without retaining entity references or loading chunks. A narrow
common Mixin preserves the prior native hurt timer and hit strength around bleed
damage, keeping melee and other stacks independent. Native damage events still
handle mitigation, absorption, attribution, and death. Positive attributed bleed
damage refreshes the existing Rage combat deadline but grants no outgoing Rage.
Small red particles mark accepted bleed damage. There is no separate bleed HUD.

`Bleeding.count(target)` reads stack count without creating state;
`Bleeding.consume(target)` removes all owners' stacks and returns the removed count
for future finishers. Those dependent skills and healing effects are not implemented.

## Resource HUD and skill visuals

Resource colors are **red for Rage**, **blue for Mana**, and **yellow for Focus**.
The HUD shows one bar
for the current class's resource, directly above the vanilla hunger bar; inactive
class resources are hidden. Centered current/maximum numbers (for example,
`75 / 100`) show the balance, with the bar color identifying the resource.
The bar stays visible whenever a known class is selected, including at full/empty
resource, outside combat, and in Creative. F1 hides it with the normal HUD. No class
means no resource bar. It reads the existing owner-synced snapshot and effective
capacity; it does not predict recovery or spending. Values display whole units
(rounded down), and large values scale to fit the hunger-width bar. The HUD reserves
a row so air bubbles and mount health stack above it rather than covering it.

Skill animations use vanilla-style hand/weapon movements with skill-specific
particles. Cleave and Arcane Bolt send native swings to the caster and tracking
players after accepted cast starts/successful execution. Custom character animation
rigs are outside this scope.

Arcane Bolt uses blue-white magic effects: a brief charge during its existing
half-second base cast, a sparkling projectile trail, and a small burst on impact.
These are presentation changes; its existing cast timing and single-hit damage
behavior are preserved.

Cleave uses a vanilla-style weapon swing, a brief forward arc, and red sparks
on enemies hit. The arc follows its 120-degree horizontal targeting within three blocks.

Piercing Shot uses the vanilla bow-charge animation for six game ticks (0.3 seconds
at 20 TPS), a thin yellow-white arrow trail, and a small impact spark.
The trail stays subtle to preserve aim
visibility. Activation remains instant, with no added draw time.
The short draw is cosmetic in first and third person: it does not start actual
item use, change movement, or call vanilla bow release. Real item use takes priority.

Native particles respect each client's particle settings and normal tracking/view
distance. Arcane Bolt keeps its existing amethyst-shard core with a blue-white
particle trail. Charge particles follow the pending server cast and stop when it
ends or is canceled. Impact bursts occur on projectile collisions; Cleave's hit
sparks require an accepted damage call. Trails are generated locally on clients
from tracked projectile movement, without per-tick trail packets or world scans.

`covenant:skill_shot` is an unsaved, tracking-synced boolean attachment marking
Piercing Shot arrows. Ordinary arrows are unaffected. Its trail stops on impact
or after five seconds; the native arrow itself retains its normal lifecycle.
Unloading/reloading an arrow does not retain this cosmetic marker. No existing
saved IDs or fields change, and no player progression migration is needed.

`covenant:bow_draw_until` is an unsaved, tracking-synced cosmetic player deadline
in level game ticks. Client-only Mixins scope the native bow-use pose/model reads
to first-person rendering and player render-state extraction, restoring the render
context even if rendering throws. Simulation and input continue to see real item-use
state. No player use flags or inventory values are changed to animate the bow.

## Skills window and bindings

Hold G, hover radial slot 1 (**Skills**), then release G or left-click. The window
continues after G is released and does not pause the game. Escape or Done closes it.
Only the current class's tree is visible. Choose a class on the Stats page (radial
slot 2) or through `/covenant class select <id>` first. The 62-node roster and layout
follow `Project-Covenant/Skill Trees Brainstorm.md`: 19 Stalker, 20 Reaver, 23 Weaver.
Left-click a node to allocate/select; right-click to deallocate. Learning/unlearning
is free and repeatable. Gold means learned; gray nodes are blocked. Hover for a
description, prerequisites, exclusions, and implementation status. All eight tiers
fit on the page at once, with higher tiers above lower tiers. Empty diagram lanes
are compressed while preserving branch order and all connections. Square nodes
resize to the available space and use temporary initials until skill icons are
added; full names remain in hover/focus tooltips. No tree scrolling or panning is
required. The separate Keybindings page retains its scrollable allocated-skill list.

All 11 vertical links require the lower skill first; both remain allocated and
usable independently. Ripping Aura and Fortitude Aura are mutually exclusive.
Removing a prerequisite also removes its learned dependents and all their bindings,
while preserving unrelated nodes and assignments. Diagram levels are informational:
no character-level gate, point budget, or rank scaling is implemented. The three
original skills, Aimed Shot, and Rend have gameplay; all other nodes are placeholders. Allocating or
binding placeholders provides no passive bonuses or gameplay effects. Pressing a
placeholder binding only reports that its gameplay is not implemented; it spends
no resource and starts no cooldown.

Use the right-hand **Tree** and **Keybindings** tabs to switch pages. In Keybindings,
select a current-class skill from the scrollable allocated-skill list, then left-click
a slot to assign; right-click a slot to clear. A learned node selected in Tree stays
selected when changing tabs. Other classes' trees and learned-skill lists are hidden.
Defaults for slots 1-8 are Q, R, F, C, Shift+Q, Shift+R, Shift+F, Shift+C. The same
skill may occupy several slots. Controls > Key Binds > Project Covenant can rebind
each slot independently; the window displays the current bindings. Press a binding
outside screens to activate once. Holding it does not repeatedly cast, and a modified
binding takes priority over an unmodified binding on the same key. Empty slots do
nothing. Costs, prerequisites, cooldowns and cast behavior use the existing server
activation pipeline. Both keybind and command activation now require learning.

`config/covenant-client.toml` has `unbindDropItem = true` by default. At startup it
clears vanilla Drop Item if that action uses Q (including Shift+Q), resets the input
lookup, and saves Controls. Drop bindings on other keys are preserved. Set this option
false to deliberately keep Q, then move skill bindings in Controls. Resetting vanilla
Controls can restore Q during that session; startup reapplies this policy. The
creative C hotbar mapping is retained. `unbindSwapHands = true` applies the same policy
to vanilla Swap Hands on F, freeing both F and Shift+F without changing other swap
bindings. Set false to retain F and rebind those skill slots.

`covenant:skill_loadout` is a separate immutable, persistent, death-copied, owner-synced
player attachment. `learned` is a set of skill IDs; `bindings` maps string-serialized
indices 0-7 to learned IDs. Missing attachment/fields default empty. Unknown saved
skill IDs survive serialization but cannot be learned, assigned, or activated through
requests. Keyboard choices remain local in vanilla options, while assignments follow
the player across machines. Multiple slots may reference one skill. Unlearning clears
all matching slots for that node and its learned dependents and cancels any affected
pending cast; it preserves resource balances and cooldowns. Testing class switches
retain inactive learned skills and assignments,
with class checks at assignment, activation, and cast completion.

Three intent-only serverbound payloads request learning/unlearning (ID + boolean),
assignment/clear (index + optional ID), or activation (index). NeoForge's main-thread
handlers resolve the sender and validate living/non-spectator status, slot bounds,
known tree ID, learning, class eligibility, prerequisites and mutual exclusions
before mutation/effects. No client supplies resource costs, timers, damage, or
targets. Loadout changes synchronize immediately;
client UI reads snapshots without optimistic progression changes.

This is an additive saved attachment. Existing worlds retain profiles, resources and
cooldowns but must learn their class skill once in the new window. Downgrading loses
loadout behavior and is not a supported progression migration. No existing saved
field or ID was renamed.

The tree pass reuses that exact attachment and payload format. Existing learning
and assignments remain valid because the original skills are prerequisite-free roots.
`SkillTree` owns node identity, class, diagram coordinates and allocation rules;
`SkillDefinition` remains the catalog of executable effects. Adding a placeholder
does not register a gameplay effect or expose it as an executable command.

## Resources

| Class | Resource | Initial / base maximum | Default behavior at 20 TPS |
| --- | --- | --- | --- |
| Stalker | Focus | 100 / 100 | Recover 25 units/second, including in combat. Capacity always 100. |
| Reaver | Rage | 0 / 100 | Gain 1 per normal-attack damage dealt and 2 per combat damage taken. No passive generation; lose 10/second after combat grace. |
| Weaver | Mana | 100 / 100 | Recover 5 units/second, including in combat. Capacity can exceed 100. |

Only the selected class recovers or depletes. Resource pulses occur every five server
ticks (four per second). Inactive balances stay unchanged except for clamping when
their effective capacity falls. Capacity increases do not refill initialized balances.
There is no offline recovery/depletion. Death retains balances, rather than refilling
them. Unknown saved class IDs have no active resource or skills.

Confirmed positive damage between distinct, non-allied living actors refreshes Rage's
140-tick combat deadline on the logical server. Projectile owners count as attackers.
Self damage, environmental damage without an actor, zero-damage hits, invulnerable
hits, blocked/dodged damage, and repeated artificial zero events do not refresh it.
Rage gains use confirmed post-defense damage, including absorption: normal melee,
arrow, and trident attacks grant 1 Rage per outgoing damage point; damage taken
from qualifying actors grants 2 per damage point. Skill damage (Cleave, Rend,
bleed, skill arrows, magic, traps) grants no outgoing Rage, but still refreshes
combat grace and grants incoming Rage to an eligible Reaver victim. Fractional
gains are retained and capacity clamps every gain. The existing
`covenant:rage_generation` attribute multiplies both coefficients. Direct melee
skills temporarily mark their exact native DamageSource (restored in `finally`);
skill arrows use their existing marker. No saved fields or IDs change.
Combat grace prevents decay and grants no passive generation.
After exactly seven seconds without qualifying damage, the next resource
pulse depletes Rage. Switching away stops Rage changes; combat grace itself expires
on the shared server clock. Rates and grace use game time, so server lag slows them.

Tune base values in the world's `serverconfig/covenant-server.toml`:
`focusRegeneration` (25), `rageCapacity` (100), `ragePerDamageDealt` (1), `ragePerDamageTaken` (2),
`rageDepletion` (10), `manaCapacity` (100), `manaRegeneration` (5).
All accept finite values in [0, 1000000]; recovery/decay rates are units/second,
damage coefficients are Rage/damage point, and capacities are units. The old
`rageGeneration` time-based config entry is retired; existing worlds receive the
new coefficients at their defaults. Saved balances and the existing generation
attribute ID retain their formats.
Restarting the server applies configuration changes predictably. The combat grace
is fixed at 140 ticks and Focus capacity at 100.

Five ordinary, player-only synced attributes multiply those configured bases:

| Attribute | Base | Bounds |
| --- | --- | --- |
| `covenant:focus_regeneration` | 1 | [0, 1.25] |
| `covenant:rage_capacity` | 1 | [0, 1024] |
| `covenant:rage_generation` | 1 | [0, 1024] |
| `covenant:mana_capacity` | 1 | [0, 1024] |
| `covenant:mana_regeneration` | 1 | [0, 1024] |

For example, Mana capacity `add: 0.5` gives 150 units with default configuration;
Mana regeneration `multiply_total: 0.2` gives 6 units/second. Existing stat commands
and Affix Pages accept these IDs. Focus recovery cannot exceed 125% of its configured
base through modifiers; no Focus capacity attribute exists. There is no affix rolling
or acquisition system, so this foundation adds no common regeneration affix source.
Trusted progression integrations can adjust native bases/modifiers using the existing
stat system. Effective capacities and recovery rates are never saved as character data.

## Definitions and activation

| Skill ID | Class | Base cost | Cooldown | Cast | Effect |
| --- | --- | --- | --- | --- | --- |
| `covenant:piercing_shot` | Stalker | 30 Focus | 2 s | Instant | Fire one bow arrow through up to three targets, stopping at blocks, with base damage coefficient 3 instead of vanilla 2 (+50%). |
| `covenant:aimed_shot` | Stalker | 40 Focus | 4 s | 1 s | Wind up, then fire one single-target arrow with base damage coefficient 4 (double vanilla). Aim remains adjustable; movement speed is 20% during wind-up. |
| `covenant:cleave` | Reaver | 20 Rage | 3 s | Instant | Deal effective native attack damage to visible non-allied living targets within 3 blocks and a 120-degree horizontal forward arc. |
| `covenant:rend` | Reaver | 20 Rage | 2 s | Instant | Strike the crosshair target within native held-item melee reach for effective attack damage; damaging hits apply one 5-second bleed stack at 10% captured attack damage per second. |
| `covenant:arcane_bolt` | Weaver | 20 Mana | 2 s | 0.5 s | Fire a straight amethyst-rendered projectile; one collision deals 6 attributed indirect-magic damage. |
| `covenant:bear_trap` | Stalker | 20 Focus | 8 s | Instant | Toss a single-use trap: 4 physical damage and a 3-second root on a damaging snap. |
| `covenant:frost_trap` | Stalker | 30 Focus | 12 s | Instant | On contact, form an 8-second, 3.5-block patch with a source-aware 50% movement slow. |
| `covenant:flame_trap` | Stalker | 35 Focus | 12 s | Instant | On contact, form a 6-second, 3-block patch: 3 fire damage on entry and at most once per second per target. |
| `covenant:blast_trap` | Stalker | 40 Focus | 15 s | Instant | On contact, explode once: up to 10 explosion damage and a low outward shove, with 3.5-block falloff and cover checks. |

Both bow skills require a main-hand `BowItem` and supported arrow ammunition, use
native arrow collision/damage/enchantment handling, honor native ammunition-use
enchantments (including Infinity), creative/infinite arrows, and wear the bow.
Piercing Shot uses native piercing level 2 (three total targets); a narrow Mixin
invoker accesses the pinned private setter. Native arrow collision and saved
piercing state remain authoritative. Aimed Shot forces piercing level 0, aims from
the player's current position/rotation at completion, and consumes ammunition and
durability only when it fires. Its cost/timers/damage and Cleave's 120-degree arc
are initial prototype tuning rather than final balance.
Cleave measures yaw independently of pitch and uses native melee
damage mitigation without replacing normal attacks, applying a second attack-speed
formula, or triggering vanilla sweeping/critical chains. Unarmed melee is permitted.
It excludes allies, the caster's own tame pets, spectators, and disallowed PvP targets.
Rend uses the same exclusions with native held-item attack-range targeting, including
native Creative/reach adjustments and block clipping. It spends nothing without a
valid target. Attempted strikes spend normally even if blocked, dodged, or immune;
only positive health or absorption damage applies or refreshes bleed. Its initial
hit follows the native melee damage pipeline without adding vanilla critical,
sweeping, attack-charge, or on-hit enchantment chains, matching Cleave's boundary.
Arcane Bolt uses Minecraft's throwable-projectile collision and damage pipeline, has
no gravity, expires after five seconds, and creates no fire, terrain damage, or area
effect. Active bolts are deliberately not saved across chunk/world unload. It uses
magic damage, with no elemental resistance/status framework.

`SkillDefinition` owns stable ID, translated name/description, allowed class set,
base cost, cooldown/cast ticks, effect kind, power and range. Add a definition,
translations, and an explicit effect branch for future simple skills. There is no
datapack registry or generic effect graph. Existing `resource_cost`, `cooldown`, and
`cast_time` native multipliers scale their respective bases; `skill_duration` scales
Rend's bleed, Bear's root, and Frost/Flame patch duration. Fractional timers round up to whole ticks, with a minimum
one-tick bleed duration.

Aimed Shot's wind-up applies a native transient movement-speed multiplier of 0.2
and a tracking-synced cosmetic bow pose. It does not start actual bow use, so cast
cleanup cannot release an extra vanilla arrow. Movement never cancels the cast.
Losing the bow/ammunition cancels on the next player tick; unlearning (including
removing Piercing Shot), class/dimension changes, death, logout, and completion
clear the modifier and pose. The modifier, cast, and pose are not saved. Cost and
timers snapshot at start; resource availability is revalidated at completion.

`Skills.activate(ServerPlayer, SkillDefinition)` is the common server entry point:

1. Require the server thread, living non-spectator player, permitted class, learned skill,
   ready cooldown, no pending cast/activation in this tick, and effect prerequisites.
2. Calculate effective cost/timers and check available resource. Snapshot those
   values for this activation; later equipment changes do not reprice the cast.
3. For a timed cast, wait on server player ticks. Reject concurrent activation.
   Class change, death, logout/replacement, or dimension transition cancels it.
4. At completion revalidate class, state, cooldown, effect prerequisites, and resource.
   Rejection spends nothing and starts no cooldown.
5. Consume the active balance and commit the cooldown before damaging effects
   or replacing an existing deployment.
   A minimum one-tick deadline and activation guard prevent same-tick duplicate or
   reentrant execution even with zero cost/cooldown multipliers.

Commands and skill-slot requests share activation. There are no client-provided
costs, timings, target IDs, or damage values. Successful
use, pending casts, restrictions, missing equipment/targets, resources, and cooldowns
produce chat feedback. `/covenant skill` shows effective costs/times and remaining
cooldowns; `/covenant resource` shows every balance, maximum, recovery and active class.

## Persistence and APIs

`covenant:skill_state` stores immutable maps `balances` (class ID -> units) and
`cooldowns` (skill ID -> absolute overworld game tick). Values are finite/nonnegative;
native player files persist them, NeoForge copies them on death/End return, and only
the owning player receives attachment synchronization. Only changed balances sync.
Damage-driven Rage gains and cooldown changes/spending synchronize immediately; native attribute packets and server
configuration synchronization provide the capacity/rate inputs. Client code may read
the attachment and `ClassResources` helpers for display, but cannot mutate authority.

The overworld game clock is shared across dimensions and saved with the world.
Cooldowns continue as the world runs while the player is offline; they pause when
the server stops. Death, reconnect, dimension/class changes retain deadlines.
Combat/cast state is transient and does not resume after loading or player replacement.
`ClassResources.consume/set` require the logical server thread, validate amounts,
and clamp to the effective maximum. A capacity loss clamps all saved class balances
after the native equipment update; this avoids early Mana truncation on load.

This is an additive saved-format change. Existing profile/page IDs and fields remain
unchanged. The older profile `resource_type/current_resource/maximum_resource` fields
remain available to low-level integrations but are not the active built-in resource
store. If a known class lacks its new balance and the legacy resource ID matches
`covenant:focus`, `covenant:rage`, or `covenant:mana`, its current value imports once.
Otherwise Focus/Mana initialize full and Rage empty. Thereafter the new balance wins.
Unknown saved IDs are retained without fallback. Downgrading drops this attachment's
behavior and is not a supported resource/cooldown migration.

`PlayerProfiles.switchClass` changes only existing class identity, preserving level,
XP, specialization, legacy fields, and the independent balances/cooldowns. Changes
through the existing trusted `update` API also initialize missing prior balances and
cancel pending casts on class changes. Ordinary players still choose once; operators
can use the added `/covenant class switch <id>` testing branch.

## In-game testing

Use a disposable world in the isolated NeoForge instance. Select a class with the
existing `/covenant class select <id>`, or switch with the operator-only command above.
`/covenant resource set <amount>` edits the active resource (clamped) for tests.
Learn the current class's skill in the Skills window before command/key activation.

For visual polish, check the resource bar sits directly above hunger, uses the
current class color, and shows centered current/maximum values at empty, partial,
and full balances. Check class switches, no class, Creative, GUI scales, F1,
underwater bubbles, and mounted health. Test a maximum above 100 through the
existing Mana/Rage attributes. Check spending/recovery against `/covenant resource`.
Use each skill to inspect the native hand swing, agreed trail/sweep, and impact
particles; ordinary arrows should keep their existing appearance. Interrupt an
Arcane Bolt cast and confirm its charge stops without a projectile or impact burst.
Check nearby players can see the effects and lower particle settings reduce them.
These rendered/client checks require manual playtesting.
For Piercing Shot, inspect the quick bow draw in first and third person (including
left-handed settings and nearby viewers), confirm firing is immediate, and try
walking, switching items, and normal right-click bow use during the cosmetic pose.
There should be no forced melee swing, extra shot, or stuck drawing pose.

For the tree GUI, check slot 1 opens on release/click, Escape cancels the
wheel, and releasing G leaves Skills open. Only the current class's nodes should
appear, including after an operator class switch while the window is open. With no
class, only the choose-class hint appears. At different GUI scales, confirm every
node and tier is visible at once and each compact node has its own hover/click target.
Try a dependent before its prerequisite; then allocate both and remove the prerequisite.
Both nodes and all their assignments must clear, leaving unrelated choices intact.
For Reaver, learn each aura in turn and confirm it blocks the other until removed.
Open the right-hand Keybindings tab, select a learned skill, and assign it to all
eight slots; close and press each binding separately, waiting for cooldown/casts.
Shift variants must activate only once and never also activate the corresponding base
slot. Right-click one assignment, then unlearn the node: all its remaining assignments
must clear and direct command use must fail. Relearn during cooldown to check it was
not reset. Unlearn Arcane Bolt during its cast: no projectile/spend/cooldown should
follow. Inspect after death, reconnect, dimension travel and save/restart. Check class
switches retain inactive assignments without allowing their use. Check Controls,
startup drop-Q clearing, other drop keys, GUI scale and keyboard/mouse rebindings.
Allocate a planned node at level one, bind it, and press its key outside the window:
only placeholder feedback should appear, without resource spending or gameplay.
Confirm the full learned list scrolls to the final node and retains selection between
tabs. Check allocation and binding persistence after reconnect/save/restart.

1. Stalker: hold a bow, carry arrows, aim at one mob, and run
   `/covenant skill use covenant:piercing_shot`. Inspect Focus before/after; default
   cost is 30 and it recovers quickly. Repeating immediately reports cooldown.
2. Stalker Aimed Shot: learn Piercing Shot, then Aimed Shot and bind it. Hold a bow
   with arrows and activate. Adjust aim and walk during the one-second wind-up:
   movement should slow, then return immediately when one stronger arrow fires.
   Default cost is 40 Focus, cooldown 4 seconds. Switch away from the bow, unlearn
   Piercing Shot, or change class during another wind-up: no shot, cost, cooldown,
   lingering slow, or stuck pose should follow. Check normal right-click bow use
   alongside the skill, zero/scaled cast times, first/third person and nearby viewers.
   For Piercing Shot, line up four targets: only the first three should take damage;
   place a wall in the line to verify it stops the arrow.
3. Reaver: hit or take a hit from a mob, inspect Rage as it builds, then stand near
   several targets with a sword and run `/covenant skill use covenant:cleave`.
   Rage can be seeded with `resource set 60`. Stop fighting for seven seconds and
   confirm depletion; another real hit grants damage-based Rage and refreshes grace.
   During grace, standing idle must not generate Rage. Cleave/Rend/bleed must not
   refund outgoing Rage. Put targets in front, at the sides, and behind: only those
   in the visible 120-degree forward arc within 3 blocks should be hit.
4. Weaver: aim and run `/covenant skill use covenant:arcane_bolt`. Confirm casting
   feedback, delayed spend/projectile, single-target damage, and cooldown. Reissue
   during casting, switch classes during a cast, and try the other classes' skills.
5. Inspect balances, switch away/back, die, travel dimensions, reconnect, and save/
   restart the world. Balances/cooldowns must persist without refill or duplication;
   pending casts must cancel. Use `stats set covenant:mana_capacity 2` for a 200-unit
   temporary maximum and `stats set covenant:focus_regeneration 1.25` for its limit.
   `stats reset` removes all temporary stat overrides. Equipment testing uses the
   existing `affix apply` command with the five resource attribute IDs.
6. Reaver Rend: learn and bind Rend, seed Rage if needed, and strike the enemy
   under the crosshair at ordinary melee reach. Confirm the instant initial hit,
   20 Rage cost, two-second cooldown, and small red bleed particles once per second.
   Reapply to reach three stacks, then stop: stacks should expire five seconds
   after the last application. Misses, walls, and out-of-reach targets spend nothing;
   blocked/dodged strikes spend but cannot apply or refresh bleeding. Try a weapon
   swap after application, armored/undead enemies, continued melee during bleed,
   another Reaver applying the fourth stack, and save/reconnect/chunk unloading.

Run `scripts/Dev.ps1 test` for dedicated-server fixtures `skill_lifecycle`,
`skill_recovery`, `skill_effects`, `skill_foundations`, `skill_loadout`, `skill_tree`,
`rend`, `bleed_pulses`, and `bleed_persistence`. They exercise actual player-file save/load,
death/End copying, snapshot codecs, class changes, commands/permissions, native page
modifiers and bounds, timed recovery/combat refresh/depletion, real projectile/area
damage, cast overlap/cancellation, duplicate rejection and cost scaling.
The tree fixture additionally covers every brainstorm link, both exclusion directions,
free level-one learning, dependent/slot cleanup, inactive-class and spectator rejection,
placeholder no-op activation, full-tree player-file persistence, and death/End copying.
Bleed fixtures cover native attack-range components and clipping, defended strikes,
absorption-only application, native multipliers, shared replacement/refresh/credit,
independent hurt cooldown, mitigation, exact expiry, consumption, saved relative
timers and ownership, inactive-target pause, offline attackers, and non-death versus
death copying. The persistence fixture resumes a restored mob through real entity
ticks after controlled pulse checks. Retained-world process restart remains manual.
Live client reconnect, dimension travel, retained-world restart and rendered feedback/projectiles
remain manual checks; codec tests alone do not establish observed client delivery.
