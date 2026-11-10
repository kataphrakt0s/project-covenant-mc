# Skills, class resources, and skill trees

This foundation shares the existing class/profile and native stat/Affix Page systems.
It adds 35 code-backed skills, persistent skill resources/cooldowns, and an
unsaved entity-local cast/combat/combo attachment. The Skills window presents
the current class's tree with staged point/rank purchases and eight saved
assignments. Unimplemented nodes remain visible but unavailable. Piercing Shot
and Aimed Shot have ranks 1–8; other playable skills allow Rank 1 until upgrades are authored.
See [PROGRESSION.md](PROGRESSION.md) for the implemented economy, XP, caps,
support credit, balance reloads and crafted respec. A full icon action bar,
pack dependencies and additional skill implementations remain separate work.
Bleed is a separate target-owned persistent attachment; it never becomes permanent
character progression or item data.

The roster, learning prerequisites, and agreed gameplay direction are in
[Skill Trees Brainstorm](<../Project-Covenant/Skill Trees Brainstorm.md>).

## Implementation order

The [Stalker balance proposal](<../Project-Covenant/Project_Covenant_Stalker_Balance_Proposal.md>)
was accepted unchanged as the numerical starting baseline on 2026-11-06.
Disengage, Arcane Arrow, and Multishot are implemented with Rank 1 purchasable,
retaining Rank 8 as the planned maximum. Higher-rank upgrades remain deferred.
That batch added only those three skills. Hunter's Pet now has separate
server-datapack tuning below; converting other Stalker skills to the new balance
baseline remains separate work.
The implemented definitions below continue to describe current code; notably,
Piercing Shot still costs 30 Focus with a two-second cooldown, while the accepted
future baseline is 20 Focus with a three-second cooldown. Proposed rank themes
and server-datapack configuration are not claims of implemented functionality.

Implement behavior in small dependency-ordered batches, with prototype tuning
where the brainstorm defers numbers. The combat progression foundation now
uses the existing playable roster; further skill content follows this order.

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

## Hunter's Pet

Rank 1 is purchasable at Stalker level 5, with no prerequisite; higher ranks remain
unavailable. The shipped baseline is an instant summon for 35 Focus, one companion
with 40 health and 3 native melee damage, and at least 30 ticks between attack
attempts. Press again to dismiss for free. Summoning starts no cooldown; defeat or
dismissal starts a 600-tick cooldown. Shared cost/cooldown multipliers apply. The
cooldown duration snapshots on summon, so a reload or later stat change cannot
change that companion's penalty. A new summon starts at full health.

Override `data/covenant/skills/hunters_pet.json` and `/reload` to tune `cost`,
`cooldown_ticks`, `health`, `damage`, and `attack_interval_ticks`. `HunterPetData`
documents units/defaults/bounds. Each summon snapshots health, damage, interval,
and scaled cooldown independently of the owner's equipment damage/health.
Existing pets retain those values and their current health through reloads;
invalid reloads preserve live tuning. Tooltip numbers describe shipped defaults.

`HunterPet` subclasses the native wolf, reusing its navigation, floating,
environmental panic, leaping, melee reach/cover checks, following and physiology.
Landed-damage events select enemies the owner damages (including arrows/spells),
or enemies damaging the owner/pet. Shared Covenant ally/PvP protections apply,
including teammate-owned pets; creepers are valid targets. There is no autonomous
hunting or pack recruitment. Targets clear if no longer eligible or the pet
strays over 32 blocks from its owner, allowing following to resume. Native hostile
retaliation can draw aggro; there is no forced target switch or taunt. Native
damage, armor, hurt immunity, knockback and retaliation remain in effect.
The pet's own source receives nearby aura bonuses; its owner receives Covenant
combat participation/XP, once per player, including a pet killing blow. Pet combat
refreshes the owner's combat timer without granting player basic-attack Rage.

Native wolf food heals the companion with no passive regeneration. Right-click
does not toggle sitting, breed it, or equip wolf armor; the pet cannot be leashed.
The skill must remain assigned to at least one Stalker slot. Removing the last
assignment, respeccing, changing class, or confirmed owner death dismisses it with
the normal cooldown. Duplicate assignments never grant extra pets. A change
retaining an assignment preserves it; cancelled deaths preserve the active pet.

`covenant:hunters_pet` is a new server-only owner attachment, with no death copy
or client-supplied state. Its codec snapshots the live wolf through native entity
serialization whenever the owner is saved, retaining health, effects, fire,
attributes, UUID and remaining attack delay. The runtime entity/reference is not
saved in chunks, so there is only one persistence authority. Logout stores and
removes the entity; login/server restart restores it near the owner without a
new charge or heal. Dimension changes and entity chunk unloading use the same
stored state, clearing the old combat target on restoration. Saved IDs are
additive; older players start without an active companion.

Placement/recall checks at most 75 candidates within two horizontal blocks and
one vertical block of the owner, using only loaded chunks, collision-free space,
solid floor, no liquid and the world border. Failed initial placement/entity add
spends nothing and starts no cooldown. An existing stored companion waits when
placement is unsafe, retrying at most once per second on the existing player
cadence. There is no global entity scan, offline AI, extra tick subscription or
chunk forcing. Active pets retain the normal cost of native wolf AI; aggregate
multiplayer performance is unmeasured.

Client-only `HunterPetRenderer` uses the shipped `original_wolf` GeckoLib model,
atlas, idle/walk/attack/hurt/death clips. Server attacks authorize damage; clips
are visual feedback. Native corpse removal allows the authored one-second death
pose without the default sideways flip. Idle/walk selection also uses native cached
horizontal displacement when GeckoLib's native limb-speed flag is absent; it
ignores vertical-only motion and recall-sized discontinuities, and hurt/death keep
priority. Four small shin insets keep body/hip surfaces above overlapping leg faces.
Rendering and multiplayer timing need manual client checks.

The dedicated `pet_*` fixtures cover summon authority/costs, failed native joins,
reentrancy, unsafe placement, assignment changes, native feeding, cancelled/confirmed
deaths, ranged targeting/allies, native attack cadence/retaliation/following,
health/status save/load, chunk unloading, actual Nether travel, datapack reloads,
and registered controller idle/walk selection when the native movement flag is absent.
`pet_restart` normally seeds a native player-file checkpoint in ignored
`runs/gametest/hunter-pet-restart*`. After a seeding run exits, verify that exact
file in a new JVM with:

```powershell
$env:COVENANT_PET_RESTART_CHECK = 'verify'
try { .\scripts\Dev.ps1 verify }
finally { Remove-Item Env:COVENANT_PET_RESTART_CHECK }
```

GameTest worlds are disposable, so this verifies an actual native player file
across processes, including the original wounded pet identity and single restore;
it does not establish a complete retained-world restart or real client reconnect.
For client review, learn/assign Hunter's Pet at Stalker level 5, summon and feed
it, shoot an enemy and watch retaliation, then test recall/portals, dismissal and
defeat. Inspect all five animation transitions, model facing/scale and late tracking
with a second client. No client automation or Prism deployment is part of this run.

## Weaver Discharge, Threefold System and Teleport

These three skills implement the accepted provisional Rank 1 defaults. Their
existing level gates are 5, 15 and 10 respectively, with no prerequisites.
Purchases use the existing one-point Apply flow and eight-distinct-skill limit;
higher ranks remain unavailable. Override `data/covenant/skills/weaver.json`
and `/reload` for server tuning. `WeaverSkillData` documents field units and
enforces finite values, whole tick/count fields and bounded areas/chain limits.
Invalid reloads retain live data. Commands report live costs/timers; tooltip
numbers explicitly describe shipped defaults.

| Skill | Shipped Rank 1 defaults |
|---|---|
| Discharge | 30 Mana, 240-tick cooldown, ten-tick cast; five-block spherical radius; one direct damage per stored stack of each element |
| Threefold System | Assigned self-only passive; instant activation for 10 Mana, 500-tick cooldown; up to +30% damage per element for 160 base ticks |
| Teleport | Instant eight-block aimed blink for 25 Mana, 200-tick cooldown; no damage |

Discharge stores one charge per successfully completed Fire/Ice/Lightning Bolt
cast, even when it misses, while Discharge is learned. Counters are independent
and capped at five each. Aura ticks, secondary hits, arcane spells and Discharge
itself grant none. Empty reserves reject activation without spending. All charges
are consumed at successful cast completion, before damage callbacks, including
paid empty-area casts. Interrupted casts spend nothing and start no cooldown.
Shared Icy Veins removes its wind-up. Costs/cooldowns snapshot at cast start;
elemental power, effects, radius and current charge counts snapshot at completion.

Each elemental component is a separate attributed magic hit, bypassing armor,
shields and Dodge while retaining general reductions, absorption, Fortitude,
actual invulnerability and elemental defenses. Components bypass native hurt
immunity and preserve the previous hurt-window state. Positive ice damage applies
a saved native 30% slow for 60 base ticks, or a transient 20-base-tick freeze at
five ice charges. Freeze uses existing input/cast interruption and non-stacking
control rules, including eligible bosses; it has no separate immunity period.
Discharge slow uses the shared strongest-slow rule and native cleansing/save/load;
configured strength rounds to percentage points. Positive fire damage ignites for
80 base ticks using native burning. Five fire charges add one centered, fire- and
explosion-tagged hit, up to four damage with linear falloff to zero at the radius.
This extra hit retains native armor/explosion defenses. No blocks, fire blocks,
native lightning entities or terrain destruction are created.

Five lightning charges also create one chain from the nearest initial area
target, hitting at most two additional enemies outside the main radius. Each
jump selects the nearest unclaimed enemy within four blocks of the preceding
target's eyes, with UUID tie-breaking and solid-cover checks. Each takes 50% of
the lightning contribution without further falloff. Main-area targets cannot be
hit again by arcs. Main damage uses the shared enemy/alliance/PvP rules and clear
eye-to-eye cover checks; a wall blocks all components and riders.

Threefold banks 25% of personally attributed elemental damage into **each other**
element's reserve, capped at six damage points per reserve. Only actual health,
ordinary absorption and Fortitude loss counts, once, with health overkill removed.
Allied/self damage and fully negated hits give nothing. Covenant fire/ice/lightning
bolts, their chains and Discharge components qualify, as do personally attributed
fire-tagged hits, including owned Immolation pulses. Arcane damage and unattributed
native burning do not. Reserve entries expire individually after 200 game ticks;
this fixed ten-second window does not scale with skill duration. Storage uses a
bounded 200-tick ring, with same-tick contributions merged and overflow discarded.

Activation requires assignment, a nonempty reserve and no existing Threefold
buff, even with operator cooldown bypass. Each element gains
`max_bonus * reserve / capacity`, fixed for the entire duration. All reserves are
consumed; banking pauses while active and resumes after expiry. Bonuses add to
Battle Cry, Immolation and other outgoing bonuses. New projectiles capture their
own elemental bonus once at launch; removing the buff never rewrites airborne
shots, and later buffs do not boost them retroactively. Active duration uses the
existing duration multiplier. Existing active snapshots survive balance reloads;
new contributions/activations adopt live data and reads clamp to the live cap.

Teleport sweeps the full player collision box up to eight blocks along full 3D aim,
stopping before solid walls without wall sliding, stepping up or passing through
thin shapes. Floor/ceiling contact clips vertical movement and lets the remaining
horizontal aim component continue, without adding speed or crossing the surface.
Entities do not obstruct the blink. The destination needs body space but no floor;
unsupported upward/downward endpoints are allowed. World border and
build-height bounds constrain travel. A route whose collision-query bounds enter
an unloaded chunk rejects without forcing/loading it. Less than 0.1 blocks of
available movement also rejects without spending or cooldown. Root, freeze,
mounting, sleeping and executing skill movement prevent activation. Successful
travel uses native server teleport synchronization, preserves facing, replaces
velocity with zero and resets accumulated fall distance; subsequent gravity and
fall damage behave normally. Lava and other hazards are not filtered out.

Charges, reserves and active bonuses are unsaved server runtime, never death
copied. Death, logout and class/dimension changes clear all three. Unlearning
Discharge clears charges and its owned freezes; its ordinary slow/burning can
finish naturally. Removing Threefold's last assignment or unlearning it clears
its bank and buff immediately; moving between slots while still assigned retains
them. Saved resources, cooldowns and loadouts retain their existing formats.
The new unsaved owner-only `covenant:weaver_display` shows charges and reserve
fullness or active bonuses in fire/ice/lightning order. Cast/activation/loadout
transitions update it immediately; banking/expiry changes publish at most four
times per second, only when values change. Display data never authorizes gameplay.

Manual checks: purchase and bind the skills, rotate elemental bolts and observe
the charge/reserve HUD, activate Threefold then Discharge, compare partial/full
element riders, and blink toward walls/ceilings and while falling. Rendered layout,
combat feel, real-client latency/teleport acknowledgement and pack compatibility
remain manual playtesting; dedicated fixtures are not evidence for those checks.

## Reaver shout, storm and slotted passive

Battle Cry, Rage Storm and Blood Fever implement Rank 1 from the
[Reaver balance proposal](<../Project-Covenant/Project_Covenant_Reaver_Balance_Proposal.md>).
All retain the normal level gates, one-point purchase, eight-distinct-skill limit,
and existing Apply flow. Blood Fever requires Rend; Rage Storm requires Cleave.
Higher ranks remain unavailable pending concrete upgrades.

| Skill | Default behavior |
|---|---|
| Battle Cry | Instant, 30 Rage, 600-tick cooldown; +15% outgoing damage for 120 base ticks to self and allies within six blocks |
| Rage Storm | 35 Rage, 240-tick cooldown, sixteen-tick wind-up; eight-block straight travel at 0.35 blocks/tick, two-block spherical radius, three damage per pulse |
| Blood Fever | Free passive while slotted; every five ticks count up to five distinct bleeding enemies within four blocks; +2% base movement and +3% base attack speed per enemy |

Battle Cry uses the shared team/owned-pet policy and source-aware aura lifetime
system. It has no passive component and does not require assignment for command
activation. Recipients are captured at shout time, including through cover; walking
away retains the buff, while late entrants receive nothing. Overlapping cries use
the strongest unexpired strength, with independent caster lifetimes. Its bonus adds
to existing outgoing aura bonuses. Native attributed attacks, skills and new bleeds
benefit, including allied pets. Projectile power and bleed power capture it once
when created; existing shots/stacks are unchanged, and damage processing never
reapplies a captured bonus. Unattributed environmental damage has no attacker to buff.
Support attribution follows the existing aura system. Unbinding the shout leaves
an already cast buff active; unlearning, source/recipient departure, confirmed death,
class/dimension changes and expiry clean it through existing lifecycle boundaries.

Rage Storm spends and starts cooldown only after its cast completes and eligibility
is revalidated. Failed entity admission spends nothing. Costs, wind-up and cooldown
snapshot at cast start; area tuning, power and full 3D aim snapshot at release.
The caster can move independently. The center stops at solid collision geometry;
each pulse requires clear line of sight from the center to the target's bounding-box
center, with that center inside the sphere. Fluids do not stop it. Pulses occur on
release and every ten ticks afterward. The default 23-tick journey offers three
pulses; four is a per-enemy attempt ceiling, not a guaranteed four-hit sequence.
Defended attempts count toward that ceiling. Armor, toughness, shields, Resistance,
absorption, Fortitude and actual invulnerability retain native behavior. This area
is not a melee strike/projectile for Dodge or Ripping triggers. Pulses bypass native
hurt cooldown and restore its previous state, allowing scheduled full hits without
changing other attacks' immunity window. Storm damage grants no outgoing Rage.
The native entity owns local queries and bounded crimson/sweep particles; it never
saves or forces chunks. Unloaded coverage, expiry/range, caster departure,
confirmed death, class/dimension change or unlearning discard it without refunds.
Skill-duration bonuses do not change its distance, speed, radius or pulse schedule.

Blood Fever uses native transient `ADD_MULTIPLIED_BASE` movement/attack-speed
modifiers. Shared teammate bleeds count; extra stacks on one enemy do not. Allies,
owned allied pets, dead targets and spectators are excluded by shared targeting.
It detects through cover, within a sphere measured between feet. Only eligible
slotted Reavers perform its local query, once per configured interval; no global
entity scan runs. Changes take effect within five ticks by default. Unbinding or
unlearning removes bonuses immediately; all normal player lifecycle cleanup applies.
Modifiers and current count never save or copy. Pressing its key reports that it is
passive and commits no spending, cast, cooldown or activation lock.

### Server datapack tuning

Override `data/covenant/skills/reaver.json` and use `/reload`. The built-in file is
the complete example. Times use game ticks (20/second), distances/radii use blocks,
speed uses blocks/tick, damage uses damage points, bonuses use decimal fractions.
The three JSON objects contain:

| Object | Fields and accepted bounds |
|---|---|
| `battle_cry` | `cost` 0–10000; `cooldown_ticks` 0–72000; `duration_ticks` 1–72000; `radius` 0.1–32; `damage_bonus` 0–10 |
| `rage_storm` | `cost` 0–10000; `cooldown_ticks`/`cast_ticks` 0–72000; `speed` 0.01–4; `distance` 0.1–64; `radius` 0.1–8; `damage` 0–10000; `pulse_ticks` 1–200; `max_hits` 1–64 |
| `blood_fever` | `radius` 0.1–16; `update_ticks` 1–200; `movement_per_enemy`/`attack_per_enemy` 0–1; `max_enemies` 1–32 |

All fields are required finite JSON numbers; tick/count fields require integers.
Invalid data rejects the reload without replacing this listener's live snapshot.
Reloads preserve allocations, resources and committed cooldowns. Existing shouts
and storms retain their snapshots; Blood Fever picks up new tuning on its next
scheduled refresh. Existing equipment cost/cooldown/cast multipliers and shout
duration scaling apply. Tooltips describe built-in defaults. Skill-tree structure,
rank upgrades, shared effect policy and older skills are outside this small tuning
file; the proposal's wider data-driven conversion remains future work.

For manual checks, select Reaver and use operator `/covenant progression level 5`
if setup is needed, then purchase Cleave, Rend and the three new skills through
Apply. Bind Blood Fever to enable its passive. Use `/covenant resource infinite true`
for repeated casts without changing resource balance behavior, and
`/covenant skill ignorecooldowns true` when testing repetition. Check Storm's
wind-up/3D aim/walls, Blood Fever's speed changes as shared bleeds enter/leave range,
and Battle Cry's timed ally/pet benefit with a second client. Commands list live
server costs/timers; descriptive tooltips show built-in defaults.

## Stalker mobility and arrow skills

All three skills are instant, server-authoritative Rank 1 activations. Existing
Resource Cost and Cooldown multipliers apply. They use the existing eight-skill
allocation/binding flow and unlock levels: Disengage 3, Arcane Arrow 15,
Multishot 20, with no prerequisites. No new datapack loader is added.

- **Disengage:** 20 Focus, six-second cooldown; capture horizontal facing and
  retreat four blocks over six ticks with an approximately half-block arc.
  Pitch and later turning do not steer it. Airborne activation replaces momentum
  and clears accumulated fall distance during travel and on exit; normal falling
  resumes afterward. Terrain can shorten or flatten travel. Ceiling/floor contact
  clips the hop without increasing horizontal speed. Native collision and the
  existing owner prediction/endpoint acknowledgement handle movement. The shared
  motion payload now includes vertical acceleration; client and server builds must
  match. Root/freeze, effective knockback, lifecycle changes, unlearning, rebinding,
  mounting and sleeping stop travel without refunds. Root/freeze prevent activation;
  a mounted/sleeping player or a second simultaneous movement is rejected. Bow
  attacks and stationary skills stay usable; there is no invulnerability. Temporary
  gravity suppression is excluded from saves, and there is no combo HUD row.
- **Arcane Arrow:** 30 Focus, eight-second cooldown; one full-speed native bow
  arrow replaces ordinary impact damage with four raw magic damage. Native damage
  tags make it bypass armor and shields; it remains a dodgeable projectile, with
  normal general mitigation and immunity. Bow damage enchantments and unrelated
  melee Attack Damage do not increase its fixed base hit; applicable offensive
  bonuses still apply.
  Tipped/spectral ammunition keeps native secondary effects. Only positive direct
  damage to health or native absorption applies vulnerability, after resolving the
  initial damage. Fortitude-only absorption does not qualify. Vulnerability grants
  +15% incoming damage from all sources for four base seconds, using Skill Duration
  captured at launch. Reapplications share one strength and retain the longer
  remaining/new duration. Shock Aura's +10% adds to produce +25%. The native harmful
  effect saves/syncs, pauses while unloaded, accepts native cleansing/applicability
  hooks without a Covenant boss exclusion, outlives owner departure, and clears on
  target death. Successful applications record existing support credit.
- **Multishot:** 30 Focus, eight-second cooldown; five full-speed native arrows
  at yaw offsets -15, -7.5, 0, +7.5, +15 degrees, each with native coefficient 1.5.
  One ammunition item and one bow durability use pay for the entire fan, following
  existing ammunition-use exemptions. Only the central arrow may be recovered;
  the four extras are creative-only pickups. Native ammunition effects remain.
  Each eligible target gets one attempt per fan, claimed before damage callbacks;
  blocked/dodged/immune attempts count. Sibling arrows cannot retry damage or
  ammunition effects on that target, independently of hurt cooldowns. Different
  fans can hit independently. New arrows exclude allies, pets and disallowed PvP
  targets under the existing targeting policy.

`covenant:stalker_shot` saves the Arcane Arrow duration or Multishot fan ID/deadline
on native arrows, separately from cosmetic trails and existing combat provenance.
`covenant:multishot_hits` saves fan attempts on targets so save/load cannot bypass
the limit. Fan arrows expire after 1,200 overworld ticks (60 seconds), including
time unloaded, bounding the shared-claim lifetime. Expired claims are pruned on
the next fan hit, and target death clears them; no entity scans or ticking claim
registry are added. Existing saved profiles, resources, cooldowns and loadouts
retain their IDs and formats; older native arrows without the new payload are
unchanged. Multiplayer rendering, movement feel and balance remain manual checks.

## Combo skills

Lunging Combo and Rip and Rush are implemented three-stage skills. Their shared
staged-activation contract and prototype acceptance criteria are recorded in the
[combo implementation plan](COMBOS_IMPLEMENTATION_PLAN.md).
Each uses one assigned key with a fresh press per stage and server-owned progression;
the client does not select stages. Their existing per-stage Rage costs, two-second
then three-second follow-up windows, and cooldown on completion or timeout remain
the implemented behavior. No skill rejects activation for lack of targets:
empty attacks spend normally, advance their stage, and follow normal cooldown
rules. Range, cover, and enemy eligibility determine hits rather than activation.

Lunging Combo's first-stage prototype is a movement-only lunge of four
blocks over three game ticks (0.15 seconds), dealing no attack damage. It uses the
captured full 3D look direction and shared movement rules, with early completion
on collision or interruption. These values remain prototype tuning.
Stage two uses Rend's block-clipped crosshair targeting and native held-item melee
reach, retaining enemy eligibility and line-of-sight checks. Without a valid
target, perform a paid whiff and advance normally to stage three. The strike deals 125% of current attack
damage at stage acceptance, using existing melee-skill damage rules and applicable
damage modifiers.
Both the stage-two strike and stage-three finisher are instant attacks
with no cast time or added wind-up, retaining normal activation checks/throttling.
Stage three's area matches Cleave: a 120-degree frontal horizontal arc
within three blocks, requiring line of sight and retaining existing enemy
eligibility checks and the yaw-based arc calculation. Its damage is 100%
of current attack damage per enemy at stage acceptance, using existing melee-skill
damage rules and applicable damage modifiers.
An empty finisher performs a paid whiff, completes the combo, and starts its
captured cooldown. Defended hits also complete normally; whiffs and hits dealing
no damage apply no shred. Cleave and Rend likewise spend and start cooldowns on
empty attacks.
The finisher's armor shred reduces armor by 20%, leaving armor toughness
unchanged. Percentage-based shred and its strength are provisional prototype
tuning to revisit when enemy armor adjustments are introduced. Its provisional
base duration is 100 game ticks (five seconds), scaled by the caster's
skill-duration multiplier captured when the finisher starts. Later modifier
changes do not alter an already-applied shred's duration. Apply shred only after
the finisher deals positive damage to health, ordinary absorption, or a Fortitude
shield. Blocked, dodged, or invulnerable hits apply none. Resolve damage against
existing armor before applying this hit's shred, so the new application benefits
subsequent hits. Each target has one shared armor-shred debuff for Lunging Combo
and Rip and Rush, even across players. Strength does not stack: repeated
applications remain at 20% and set
expiry to the later of the existing expiry and the new application's expiry,
never shortening a longer application.
Once applied, shred belongs to the target and lasts for its remaining duration
even if the caster dies, logs out, changes class, or unlearns the skill. Caster
cleanup does not remove an already-applied shred from surviving targets.
Save its remaining duration across target save/load and reconnects. Pause the
timer while the target is unloaded or offline and resume when it ticks again.
Target death clears shred, without copying it to a respawned replacement player.
This target-owned timer is separate from transient combos and saved cooldowns.
Shred is as a normal harmful status: milk and normal effect-clearing can
remove it, and native effect-immunity/applicability checks can reject it. Such
rejection does not undo finisher damage, Rage spending, or stage completion.

Planned base Rage costs are 20 for the lunge, 10 for the single-target strike,
and 20 for the finisher (50 total before modifiers). Each stage checks and spends
its own cost using the player's current resource-cost multiplier when accepted.
Lunging Combo's base cooldown is 240 game ticks (12 seconds), reflecting
its extended sequence and combined mobility, damage, and armor-shred power. Use
the shared stage-one cooldown snapshot and start it at the combo's actual end.

Rip and Rush's opening circular Rend covers all directions within a
three-block radius around the player, requiring line of sight and retaining
existing enemy eligibility and melee-area distance checks without the frontal
arc restriction. Apply Rend once per eligible enemy: 100% current attack damage
with existing melee-skill modifiers, plus one normal bleed stack after positive
health, absorption, or Fortitude damage. Retain Rend's defenses and its guard
against an additional Ripping Aura bleed on that hit. Base bleed is 10% of
application-time attack damage per second for 100 base ticks (five seconds),
with existing bleed-power bonuses and skill-duration scaling captured at stage
start. Preserve the target-owned shared three-stack cap, refresh/replacement,
attribution, and persistence rules. Both circular attacks execute as paid whiffs
when empty: the opening opens the dash window and the finisher completes the
combo. Non-bleeding enemies can be damaged but grant no speed reward. The dash
requires no target or bleed.
The opening circular Rend and final circular AoE are instant, with no
cast time or added wind-up. Retain normal activation checks and throttling;
the dash remains an executing movement stage.

Rip and Rush's second-stage dash travels six blocks over four game
ticks (0.2 seconds), using the captured full 3D look direction and shared movement
rules. Movement-speed and skill-duration bonuses do not change these values.
Full blockage and agreed interruptions can end it early; the full third-stage window
opens when movement actually ends.
Planned dash damage is 50% of current attack damage captured when the stage
begins, using existing melee-skill damage rules and applicable damage modifiers.
Allow at most one hit attempt per enemy per dash, including defended attempts;
remaining in contact does not retry or deal repeated hits.
Contact requires an eligible enemy's hitbox to intersect the player's body swept
along the actual full 3D travelled path, without an added hit radius. Clip checks
to terrain-limited travel, including the final partial segment; enemies beside
the swept body or beyond an obstacle that stopped movement are not contacted.
The dash applies the same provisional 20% armor shred for 100 base ticks (five
seconds), without changing toughness. Snapshot scaled duration at dash-stage
start using the caster's skill-duration multiplier. Apply only after positive
health, absorption, or Fortitude damage, resolving the hit against existing
armor first. Reuse all shared shred cleansing, native immunity, target
persistence/death, and caster-independent expiry rules. Both skills and all
players refresh one target-owned debuff without stacking strength or shortening
a longer remaining application.

Rip and Rush's finisher matches its opening area: all directions within
a three-block radius around the player's current position when activated,
requiring line of sight and retaining the same enemy eligibility and melee-area
distance checks. It follows the activation position even if the player has moved
since the dash ended.
Its damage is 100% of current attack damage per eligible enemy, captured
when the finisher stage begins, using existing melee-skill damage rules and
applicable damage modifiers.
For each enemy qualifying for consumption, remove all shared bleed stacks,
including other players' stacks, through the existing shared consumption boundary.
Count each enemy once toward the movement-speed reward regardless of stack count.
This can spend teammates' remaining bleed damage and reduce bleed-dependent
nearby-enemy counts. Consume only after positive finisher damage to health,
absorption, or Fortitude. Blocked, dodged, or invulnerable hits preserve bleeds
and grant no speed reward. Killing blows against bleeding targets qualify;
retain pre-hit bleed presence for reward eligibility even if death cleanup has
already removed the stacks. Each qualifying enemy still counts once.
The caster gains +10% base movement speed per qualifying enemy whose bleed is
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

Rip and Rush's base Rage costs are 30 for the opening circular Rend,
20 for the dash, and 20 for the finisher (70 total before modifiers). Each stage
checks and spends its own cost using the player's current resource-cost
multiplier when accepted, following the shared per-stage rules.
Its base cooldown is 400 game ticks (20 seconds), using the shared
stage-one effective cooldown snapshot and starting at the combo's actual end.

Capture effective cooldown duration at the first accepted stage, using the
existing cooldown multiplier. Completion, timeout, and cancellation start that
captured duration at the combo's actual end. Later equipment/modifier changes
do not recalculate it; rejected starts capture nothing and start no cooldown.

Each accepted stage calculates its Rage cost and damage from current equipment
and stats when it begins, using existing scaling and damage rules. Recheck
resources per stage; later buffs or weapon changes may affect later costs and
damage. Already committed effects retain their own snapshots, including bleed.
The whole-combo cooldown snapshot remains unchanged.

Ordinary attacks and other non-combo skills preserve combo progress while its
follow-up timer continues without pausing or refreshing. Existing activation
checks still apply; ordinary attacks retain their normal Rage generation.

Active lunge/dash movement permits ordinary attacks and other skills' stationary
stages under normal activation checks. Rip and Rush's circular Rend may execute
during Lunging Combo's lunge, while Lunging Combo's own next stage remains locked
until its movement finishes. Do not treat skill movement as a global cast lock.

Different combo skills may interleave, with independent stages, follow-up
deadlines, and cooldowns. Their timers keep running while the other is used,
and both spend from the same Rage balance. Duplicate bindings share the same
skill's progression; they do not create additional independent sequences.

Rejected follow-ups spend no Rage and leave progression and the original
deadline intact. Retrying is allowed before expiry; rejection does not pause or
refresh the window or independently start a cooldown. Normal expiry still does.

Accepted, performed stages spend Rage and advance even when blocking, Dodge, or
invulnerability prevents damage. This does not waive separately specified hit
or defense requirements for secondary effects such as bleed or armor reduction.

Freeze rejects stage activation without cancelling combo progression. Original
deadlines keep running; thawing before expiry permits continuation, while expiry
during freeze starts the normal cooldown. Freeze still interrupts pending casts;
an open combo follow-up window is separate from a pending cast.

Root rejects Lunging Combo's initial lunge and Rip and Rush's dash without
spending, while stationary attack stages remain usable under normal checks.
It does not cancel progress, pause timers, or permit skipping stages. Rejected
follow-ups retain their original deadline; a rejected initial lunge opens no
combo and starts no cooldown.

Skill-driven lunges/dashes cannot overlap on one player. Reject conflicting
movement-stage requests without spending, advancing, queueing, or replacing the
current movement; existing deadlines continue. Retrying requires another press.
Rejecting an initial movement stage opens no combo and starts no cooldown.

Follow-ups become available at preceding-stage completion, with a full two-second
window after stage one and a full three-second window after stage two. Instant
attacks complete immediately; movement stages must finish first. Early requests
for that combo's next stage are rejected without spending or queueing. Other
combos' already-open windows continue running independently.

Follow-up windows remain fixed at 40 and 60 game ticks (two and three seconds
at normal game speed), unaffected by skill-duration bonuses. Keep these input
deadlines separate from the duration rules of stage-applied statuses and buffs.

Normalize the captured launch direction once. Floor/ceiling contact flattens pitch
at the current speed without changing horizontal heading. Walls clip blocked
horizontal components without renormalizing the remainder, so a blocked forward
dash cannot turn tiny sideways aim into a full-speed strafe. Glancing wall slides
retain only their original tangential speed. At simultaneous contacts, flatten
pitch before clipping wall components. Use only the remaining fraction of the
current tick along the surviving direction to preserve the travel budget.
A direct impact with no surviving tangent stops movement early and completes
the stage, keeping Rage spent and opening the full next-stage window. Never pass
through solid terrain or require a minimum distance to unlock a follow-up.

Lunges/dashes capture the player's full 3D look direction at movement start,
including pitch for upward/downward travel. Turning afterward does not steer
or retarget movement. Each movement stage has fixed distance and travel time,
unaffected by movement-speed or skill-duration bonuses. Both skills' agreed
movement values are recorded above. Collision/interruption
may shorten travel. Surface collision may redirect the captured path; turning
does not steer it. Suspend gravity during movement to preserve straight open-space
travel and frictionless surface sliding; restore normal
gravity at movement end, including early terrain collision. Follow-up windows
do not suspend gravity. End the skill's movement momentum when the stage ends,
including an early terrain stop, so residual dash propulsion does not extend its
range. An upward dash stops propelling upward and normal falling resumes.
Starting a lunge/dash replaces existing velocity and does not restore it later.
Prior sprinting, falling, or knockback momentum does not add to the skill's path.
New incoming knockback follows the interruption rule below.

Lunges/dashes break falls by clearing accumulated fall distance at movement start
and throughout movement, including a movement-ending landing. Resume normal fall
tracking at any movement end, including an early interruption. Later falling
can deal normal fall damage; follow-up windows provide no fall protection.

Incoming knockback that takes effect ends active lunge/dash movement early,
restores normal gravity, and applies normally. Keep Rage spent and open the full
next-stage window without cancelling the combo. Movement cleanup must preserve
the incoming impulse, including Blast Trap's explicit push.

Gaining root or freeze during a lunge/dash ends skill movement early and counts
as stage completion. Restore normal gravity, keep Rage spent, and open the full
next-stage window without cancelling progression. Freeze blocks activation until
thawing; root allows stationary attacks. All open windows keep running normally,
and other combos' existing deadlines remain unchanged.

Death, logout, dimension travel, or class change ends all unfinished combos and
immediately starts each affected skill's cooldown. Keep spent Rage with no refund
or charge for unused stages. Stop active skill movement and restore normal
gravity without opening another follow-up. Combo progression/movement are
transient; committed cooldowns use the existing persistent, death-copied state.

Unlearning an unfinished combo ends it, stops its movement, restores normal
gravity, and starts its cooldown without refunds or charges for unused stages.
Cover dependent deallocation: removing Lunging Combo also ends unfinished Rip
and Rush. Other still-learned combos remain intact. Relearning retains cooldowns;
unlearning an idle combo creates no new cooldown.

Changing or clearing an unfinished combo's slot assignment cancels it and starts
its cooldown, even if another slot still holds it. Keep spent Rage, charge nothing
for unused stages, stop its movement, and restore normal gravity without opening
another follow-up. Reassigning retains the cooldown; unrelated combos remain
intact. Rejected or unchanged assignments do not cancel progress, and editing an
idle combo's binding creates no new cooldown.

Keep binding-change cancellation within Covenant's skill-slot GUI and existing
server checks in `SkillLoadouts.bind`. Physical key remaps in Minecraft Controls
do not affect combo progress. Reuse the existing packets; add no Controls
monitoring or remap notification protocol.

The top-left status HUD shows one native icon per open combo, with its stage
number and remaining follow-up time. Executing movement shows its current stage
without a countdown until the window opens. Interleaved combos have independent
icons, removed when each combo ends. Hovering while a screen is open names the
skill and next/current action. Owner-only transition snapshots supply server
deadlines; no per-tick countdown packets or gameplay predictions are added.

## Combo movement synchronization

Combo movement uses owner-only start/stop trajectories with native collision
steps on both sides. The server owns travel/hits/stage completion; the client
predicts movement with normal frame interpolation. Normal travel sends no native
teleport stream. Predicted movement packets are held until the owner acknowledges
the server's endpoint. Each authoritative step also updates the native connection
position anchor: Minecraft's enclosing connection tick otherwise restores its
pre-step cached position after `ServerPlayer.doTick()` and undoes the movement.
After endpoint acknowledgement, vanilla's movement anchor is reset again. Only a mismatched
endpoint or a 40-tick acknowledgement timeout causes a native correction, retaining
current velocity. Stale/early acknowledgements cannot advance a stage or move the
server. Native knockback is applied immediately at interruption; player/dimension
cleanup discards client prediction. See `COMBOS_IMPLEMENTATION_PLAN.md` for details.
Waiting for a dash acknowledgement holds incoming position reports while native
server gravity, fluids, knockback and piston displacement continue. Root/freeze
use the same server-physics boundary for unmounted players. Divergent reports
receive bounded native corrections; outstanding teleport IDs retain vanilla
acknowledgement/retry handling. Acknowledging a control correction cannot rewind
external motion that occurred after it was sent. Unrelated native teleports retain
their normal destination semantics and supersede dash movement.
Freeze also gates native instant item use, entity/block interaction, melee and
spear stab attacks; root continues permitting actions. Mounted vehicle steering
is unchanged: controlling the rider does not apply a status to the mount.
Rendered movement and latency remain manual client checks.

## Aura implementation boundaries

`Auras` owns transient source/recipient grants and reverse cleanup membership.
Only assigned sources query their six-block surroundings, once per ten ticks,
sharing one query across range passives. Recipient expiries preserve independent
sources. No chunks are forced and no loaded-entity scan or recurring coverage
raycast runs. Self passives use assignment transitions instead of spatial queries.
`Auras.allied` centralizes scoreboard and native `OwnableEntity` membership.

`AuraDamage` accounts for Fortitude in native pre/post damage events, after
mitigation and before ordinary absorption. A scoped hit frame handles nested
hits without double counting. A landed-hit snapshot is shared by Rend, elemental
status impacts, Bear Trap, and bleed feedback. Only the separate remaining shield
capacity is owner-synced for the Fortitude status icon; ordinary absorption is untouched.
Freeze sync changes only on transitions; activation particles are bounded bursts.

`CovenantSlows` resolves one native movement modifier from the strongest live
Covenant slow. Chilled retains its native saved effect and relative timer; load,
refresh, expiration, and removal reconcile the derived modifier. Frost patch exit
retains its immediate removal. Pets without the player-only Dodge attribute use
their aura coverage for ordinary Dodge, with no attribute added to unrelated mobs.
Pets exposing that attribute use its native value. The guaranteed-hit path is
shared by players and owned pets.

Freeze suppresses native movement input, jumps, player melee, native mob melee,
item-use starts, bow/crossbow shooting, and native mob goal/brain/navigation work;
Covenant casts reject frozen users and pending casts cancel without spending.
Damage and external velocity remain. This covers native action paths; third-party
AI that performs attacks or movement outside those hooks may require a specific
integration. No universal modded boss/AI compatibility is claimed.

Auras, Fortitude, freeze state, and pair deadlines are transient and never copied
or saved. Reconnect restores assigned passives through normal server ticks;
active buffs and shields must be activated again. Existing Chilled saves retain
their effect ID and duration. Committed projectile power and Enchanting bonuses
survive source cleanup and native projectile save/load; see Persistence and APIs.
Profile, loadout, cooldown, and bleed registry IDs remain unchanged.

## Aura contract

All eleven auras are implemented. Tuning and the delivery checklist are in the
[aura implementation plan](AURAS_IMPLEMENTATION_PLAN.md).
An aura provides its passive while assigned to a
current-class skill slot; learning alone provides no passive. All distinct assigned
auras provide their passives simultaneously, with no separate aura limit beyond
the eight-slot loadout and existing learning exclusions. Duplicate assignments of
the same aura provide no additional passive benefit. Pressing its assigned key
activates its separately defined temporary effect. The individual sections below
and the brainstorm record the agreed rules and tuning.

For allied aura effects, recipients are the caster, players on the caster's
Minecraft scoreboard team, and pets owned by the caster or those teammates.
An unteamed caster benefits only themselves and their own pets. Explicitly
self-only effects remain self-only. Use shared aura ally resolution, with future
support for FTB chunk teams recorded as a compatibility requirement. The initial
implementation adds no FTB dependency or integration; that work remains deferred.

Team changes do not revoke existing buffs. Former allies stop receiving passive
refreshes and new allied activations, while existing passive coverage and timed
active buffs expire naturally. The explicit caster and recipient lifecycle
cleanup rules below remain in effect.

Range-based passive benefits and debuffs refresh a fixed 20-game-tick (one-second)
coverage duration while recipients are eligible and in range. Leaving range stops
refreshes; the remaining duration provides a brief grace period during movement.
Refresh coverage every 10 game ticks (0.5 seconds), giving approximately 0.5–1
second of grace after leaving range at normal game speed. Damage intervals are
separate and defined per effect below. Explicit caster cleanup events still remove
effects immediately.
Allied activated buffs select eligible recipients in range when activation takes effect; those
recipients retain the timed buff after leaving range. Later arrivals receive no
buff from that activation.

Overlapping copies of the same aura passive use only the strongest applicable
copy, for both allied benefits and effects on enemies. Other eligible sources
provide coverage when that source no longer applies. Different aura passives
can coexist; this generalizes the brainstorm's existing Shock Aura rule.

Copies of the same activated aura buff also do not stack. Each cast keeps its own
expiry; only the strongest unexpired applicable copy provides the effect. A later
cast may outlast an earlier cast without adding strengths or extending the earlier
timer. Fortitude shield capacities do not add together. Dodge's shared guarantee
consumption and Fortitude's replacement/refresh behavior are explicit individual
exceptions defined below.

An assigned aura's passive continues during its activated effect and activation
cooldown, subject to normal eligibility and range rules. Activating an aura adds
its temporary effect without suspending its passive.

Removing an aura's last assignment immediately ends that caster's passive and
active effects, including buffs supplied to allies, while preserving cooldowns.
Removing a duplicate assignment does not cancel effects while another
current-class slot still holds that aura.

Caster death, logout, dimension change, class change, or unlearning clears that
caster's supplied aura effects on all recipients without resetting cooldowns.
Passives resume when the caster is eligible again with the aura assigned; active
effects require a new activation. Other casters' eligible effects are unaffected.

Recipient death, logout, or dimension change clears their aura buffs and
Fortitude capacity; temporary buffs do not survive respawn, reconnect, or
dimension travel. Eligible nearby recipients can receive passive coverage again
normally. Separately saved bleeds and committed projectile bonuses retain their
documented contracts.

Passives require no resource upkeep and remain effective at zero resource.
Activations spend a one-time class-resource cost (Focus, Rage, or Mana) and use
independent per-aura cooldowns; activated effects have no recurring drain.
Individual costs and cooldown values are defined below.

All aura activations are instant, without a wind-up. Success immediately spends
the one-time cost and starts the cooldown; rejection spends nothing and starts
no cooldown. There is no aura cast window to interrupt.

Aura activation follows the existing cast pipeline and is rejected while another
skill is being cast. It neither interrupts nor bypasses the current cast. Aura
passives continue during other casts.

Nearby aura effects use spherical range centered on the caster, measured between
entity positions, with equal horizontal and vertical reach. The shared prototype
radius is six blocks for nearby aura effects, unless an individual override is
agreed. Allied benefits ignore walls and floors for both passive
coverage and activated-buff recipient selection; range and ally eligibility still
apply. Enemy-facing aura effects also ignore cover, including Immolation damage
and Freezing/Shock effects, while retaining range and enemy eligibility checks.
Aura coverage and area activation selection require no line-of-sight checks.
This does not alter separate attack cover rules, including Lightning Bolt or its
chains. Performance has not been benchmarked.

Offensive auras use normal skill enemy eligibility: living non-spectator targets
other than the caster, excluding aura allies and their pets and respecting PvP
restrictions. Include passive animals and modded living creatures, rather than
only hostile mobs. Shared aura ally resolution governs friendly exclusions,
including future FTB chunk-team support. Immolation can automatically damage
eligible livestock within range, including through cover.

Raging and Immolation bonuses follow each effect's existing damage-calculation
timing. Apply the applicable bonus at that normal calculation point rather than
moving calculations or imposing universal impact-time evaluation. Explicit bleed
application snapshots and Enchanting projectile-at-firing snapshots remain.

Fortitude-absorbed damage counts as landed combat damage for Rage gains, combat
grace, and bleed application, like ordinary absorption. Preserve existing Rage
source rules, including no outgoing Rage from skill damage and no combat credit
for self/environmental damage. Account for shield absorption once without also
counting it as health damage. This extends current hit-confirmation behavior.

### Dodge Aura prototype direction

Passive: +10 percentage points of Dodge for the caster and nearby allies,
using shared coverage rules and the six-block radius. Ordinary Dodge chance has a global 85% cap across sources. The activated effect
guarantees avoidance of one dodgeable incoming hit per recipient, bypassing that
chance cap without making undodgeable damage dodgeable. The guarantee has a
three-second (60-game-tick) base window, scaled by the caster's existing
skill-duration multiplier, and expires unused if no dodgeable hit arrives. Its
base cooldown is 20 seconds (400 game ticks), scaled by the existing cooldown
multiplier. This exceeds Blast Trap's current 15-second base cooldown, satisfying
the agreed minimum. Activation costs 25 Focus at base, scaled by the existing
resource-cost multiplier. On a dodgeable incoming hit, resolve the guarantee
before ordinary Dodge and consume all currently overlapping Dodge Aura guarantees
on that recipient. Overlapping casts do not queue guaranteed dodges. A subsequent
activation can grant a fresh guarantee. Undodgeable damage does not consume it.

### Swiftness Aura prototype direction

Passive: +10% movement speed for the caster and nearby allies through a
native movement-speed modifier, using the shared six-block radius and passive
coverage refresh/grace rules. The active clears existing slows and adds
20 percentage points of movement speed plus 0.5 blocks of step height. Passive
and active bonuses add to +30% while both apply; after passive coverage expires,
the active's +20% remains for its remaining duration. The active has a four-second
(80-game-tick) base duration, scaled by the caster's existing skill-duration
multiplier. Cleanse slows once on activation, without granting slow immunity;
new hits or ongoing fields can reapply slows afterward. Clear vanilla Slowness
and Covenant movement slows (including Chilled and aura/trap slows), leaving
roots and full freezes intact. Do not strip arbitrary third-party movement
modifiers. Activation costs 25 Focus at base and has a 20-second (400-game-tick)
base cooldown, using the existing resource-cost and cooldown multipliers.

### Ripping Aura prototype direction

Passive: +20% bleed damage for the caster and nearby allies, using shared
six-block range and passive coverage rules. Direct-hit damage is unchanged.
Capture the bonus when applying a bleed stack. Entering coverage does not boost
existing stacks; leaving coverage does not reduce captured damage. Refreshing
retained stacks preserves damage; replacement stacks capture the current bonus.
Aura cleanup removes coverage and the active buff, without removing or changing
already applied bleeds. The active triggers from recipients' ordinary
melee attacks and melee skills (including Cleave and Rend), requiring positive
health, ordinary absorption, or Fortitude shield damage. Projectiles and damage-over-time pulses do not
trigger it. Rend uses its own bleed application, with one stack per target per
hit and no additional Ripping stack. Other eligible hits apply a bleed matching
Rend: 10% of captured native attack damage per one-second pulse for a five-second
base duration, using existing shared stacking, refresh/replacement, attribution,
and defense rules. The captured +20% passive makes that 12% when applicable.
The active buff lasts five seconds (100 game ticks) at base, scaled by the
caster's existing skill-duration multiplier. Applied bleeds retain their own
remaining durations after the buff ends. Activation costs 40 Rage at base and
has a 20-second (400-game-tick) base cooldown, using the existing resource-cost
and cooldown multipliers.
Ripping and Fortitude remain mutually exclusive.

Limit aura-triggered bleed applications to once per 20 game ticks per
attacker/target pair, shared across overlapping Ripping buffs. This gates stack
applications and refreshes, not bleed damage pulses. Different targets have
independent limits; Rend's own bleed application remains unchanged.

### Fortitude Aura prototype direction

Passive: +4 Armor and +2 Armor Toughness for the caster and nearby allies
through native flat attribute modifiers, using shared six-block range and passive
coverage rules. Ripping and Fortitude remain mutually exclusive. The
active grants a temporary damage-absorbing shield that absorbs damage after armor
and other applicable damage reduction, before health is lost. For example, a hit
reduced from 10 damage to 6 consumes 6 shield points. Each recipient receives a
flat six-point shield (three hearts), independent of maximum health. The shield
lasts seven seconds (140 game ticks) at base, scaled by the caster's existing
skill-duration multiplier, ending sooner if depleted. Unused capacity expires.
Track Fortitude as a separate capacity pool consumed before ordinary absorption
or health, after mitigation. Expiry and source cleanup remove only Fortitude's
remaining capacity, leaving unrelated absorption intact.

Fortitude overrides the shared independent-copy buff rule: activation selects
eligible entities in the six-block area at that moment and grants a fresh
six-point shield with its full duration, even if they already have Fortitude.
Replace the previous shield and timer; capacity resets to six points instead of
adding to remaining capacity. Keep no backup copy. The new cast owns the
replacement for source cleanup. Later arrivals receive nothing from that
activation. Activation costs 40 Rage at base and has a 20-second (400-game-tick)
base cooldown, using the existing resource-cost and cooldown multipliers.

### Raging Aura prototype direction

Self-only passive: set out-of-combat Rage loss to 3 Rage per second after
the existing seven-second grace period, without extending grace or generating
Rage. The self-only active grants +20% to all damage personally dealt by the
player, including ordinary melee attacks, skills, and bleeding, excluding pet
damage. It also stops passive Rage decay while skills still spend Rage normally.
Bleed stacks capture Raging's bonus on application: existing stacks are unchanged
when it activates, and boosted stacks retain damage after it ends. Refreshing
retained stacks preserves damage; replacements capture the current bonus.
Ripping and Raging combine additively for +40% bleed damage, captured once on
application: a base pulse of 10% captured attack damage becomes 14% with both.
Do not multiply the bonuses or apply Raging again on bleed pulses. The active
lasts six seconds (120 game ticks) at base, scaled by the caster's existing
skill-duration multiplier. Activation costs 40 Rage at base and has a 30-second
(600-game-tick) base cooldown, using the existing resource-cost and cooldown
multipliers. The longer cooldown is intended to make this an occasional burst
rather than an activation for every enemy encounter.

### Icy Veins prototype direction

Self-only passive: 10% reduced skill cooldowns while assigned, through
the existing native skill-cooldown multiplier. A base 20-second cooldown becomes
18 seconds before other modifiers. Apply the reduction when calculating new
cooldowns through the existing skill pipeline; assignment or removal does not
rewrite running timers. The active removes spell wind-ups temporarily,
without changing channel firing rates, spell resource costs, or cooldowns.
The active lasts four seconds (80 game ticks) at base, scaled by the caster's
existing skill-duration multiplier. There is no cast-count limit; players may
rotate all available spell cooldowns during the window if they have the Mana.
Activation costs 30 Mana at base and has a 30-second (600-game-tick) base cooldown,
using the existing resource-cost and cooldown multipliers. Its own passive makes
that 27 seconds before other modifiers.

### Flame Cloak prototype direction

Self-only passive: reduce incoming fire-tagged damage by 20% while
assigned, including burning, lava, and fire-tagged attacks. Use the native fire
damage-type tag; non-fire damage is unchanged and no fire immunity is granted.
The active ignites eligible melee attackers only when their hit damages
the player's health, ordinary absorption, or Fortitude shield. Dodged, fully
blocked, or otherwise fully negated hits do not trigger it. Retaliation ignites
for four seconds (80 game ticks) at base, scaled by the caster's existing
skill-duration multiplier. Reuse native burning, extinguishing, fire immunity,
and burning-time modifiers as with Fire Bolt. Repeated hits refresh burning
without independent burn stacks. Target 40% base uptime: base retaliation-window
duration is 40% of the base cooldown. Apply the existing cooldown and
skill-duration multipliers independently afterward; effective uptime may exceed
40%. Base cooldown is 20 seconds (400 game ticks), giving an eight-second
(160-game-tick) base retaliation window. Activation costs 30 Mana at base, scaled
by the existing resource-cost multiplier.

### Freezing Aura prototype direction

Passive: 10% movement slow on nearby eligible enemies, using the shared
six-block radius and one-second coverage refreshed every 0.5 seconds. The target
is the smallest noticeable slow; perceived strength requires in-game playtesting.
Covenant movement slows use only the strongest active slow rather than combining:
10% Freezing Aura yields to 30% Chilled or 50% Frost Trap. Weaker effects keep
their own timers and resume if still active when the stronger effect ends. Roots
and full freezes remain separate. This shared rule also applies to Chilled and Frost Trap. The active briefly freezes nearby enemies,
preventing voluntary movement, jumping, attacks, and skill casting. Frozen targets
still take damage and receive external knockback; freeze neither grants
invulnerability nor cancels external forces. Freeze lasts 1.5 seconds (30 game
ticks) at base, scaled by the caster's existing skill-duration multiplier.
Damage does not break freeze, including ongoing bleeding or burning. Applying
freeze cancels pending Covenant casts rather than pausing them, with no resource
spending or cooldown for the unfinished cast. Targets must cast again after
thawing. Activation costs 40 Mana at base and has a 25-second (500-game-tick)
base cooldown, using the existing resource-cost and cooldown multipliers.
Requires Icy Veins.

An otherwise valid Freezing activation spends Mana and starts cooldown even if
no eligible enemy is in range or receives freeze. It is a committed area action
and does not require a target to activate.

Eligible bosses receive the slow and freeze without blanket boss immunity, while
actual invulnerability remains respected, matching the prototype trap policy.
There is no post-freeze immunity period or diminishing-return system: targets
can be frozen again immediately after thawing, allowing coordinated staggered
freezes. Overlaps retain the shared non-stacking active-effect rules.

### Shock Aura prototype direction

Passive: nearby eligible enemies take 10% increased damage from all
sources, including allies' attacks and ongoing damage, using shared six-block
range and passive coverage/grace rules. Identical Shock passives use the
strongest copy; the bonus stacks with the planned Arcane Arrow vulnerability.
The active temporarily makes the caster's Lightning Bolt chain from its initial
target to up to two additional eligible enemies, with at most three total targets
and no repeat hits per cast. Each jump reaches up to four blocks from the previous
target; chain targets need not be inside the caster's six-block passive field.
Each jump selects the nearest unhit eligible enemy with a clear path from the
previous target. Solid cover blocks arcs; the chain ends if no candidate qualifies
within four blocks. Cover checks occur during the cast, not during recurring
passive coverage updates. Each chained hit deals 50% of Lightning Bolt's base
damage with no further falloff: currently 6, then 3 and 3, before bonuses and
defenses. Derive jump power from base damage, not damage received by the previous
target. The active lasts seven seconds (140 game ticks) at base, scaled by the
caster's existing skill-duration multiplier, with no cast-count limit. Target
roughly three or four chaining casts as cooldowns and Mana allow. Activation costs
30 Mana at base and has a 25-second (500-game-tick) base cooldown, using the
existing resource-cost and cooldown multipliers.

### Immolation Aura prototype direction

Passive: one point of fire damage per second (20-game-tick pulses) to
nearby eligible enemies, before defenses, using shared six-block range and
coverage rules. Overlapping copies use the strongest passive without adding
damage or independent pulse schedules. Pulses operate independently of normal
hurt immunity without suppressing other hits or being suppressed by them. Actual
invulnerability and fire immunity remain respected. Pulses bypass armor, shield
blocking, and Dodge, while respecting fire protections, general damage reduction,
Fortitude shields, and ordinary absorption. The self-only active grants +30% fire
damage personally dealt by the caster, including Immolation pulses and Fire
Bolt's direct damage. The fire archetype targets high damage throughput. Boost
only fire damage attributed to the player; native burning ticks remain unchanged,
including burns started by Fire Bolt or Flame Cloak. Native burning does not
reliably retain caster ownership. The active lasts six seconds (120 game ticks)
at base, scaled by the caster's existing skill-duration multiplier. Activation
costs 40 Mana at base and has a 30-second (600-game-tick) base cooldown, using
the existing resource-cost and cooldown multipliers. Requires Flame Cloak.

### Enchanting Aura prototype direction

Passive: +20% Mana regeneration for the caster and nearby allies through
the existing Mana-regeneration attribute, using shared six-block range and
passive coverage rules. Maximum Mana is unchanged; this does not enable inactive
class resource recovery or benefit classes without Mana. The active adds
two points of magic damage (one heart) per eligible ordinary melee or bow/crossbow
hit, independent of weapon damage and excluding skills. Require the original
hit's player melee bonus to scale by the pre-hit vanilla attack strength, as
enchantment bonus damage does: two points times attack strength in [0, 1]. Capture
this before the attack resets; fully charged swings receive two points and weak
swings receive less. Ordinary bow shots scale the two-point bonus by bow draw
strength; ordinary crossbow shots receive the full two points. Capture the scaled
projectile bonus at firing time. Require the original hit to damage health,
ordinary absorption, or Fortitude's shield. Each ordinary projectile may apply
its captured bonus once per successfully damaged enemy; vanilla piercing can
enhance multiple target hits and multishot projectiles each carry the bonus.
Skill-created projectiles remain excluded. Dodged, fully
blocked, or otherwise fully negated hits do not trigger bonus damage. Ordinary
bow/crossbow shots capture the bonus when fired, retain it after buff expiry,
and cannot gain it from activations after firing. Captured bonuses belong to
committed shots; source-buff cleanup does not retroactively erase them. Added
damage follows vanilla magic defenses: bypass armor, armor toughness, and shield
blocking, while respecting Protection enchantments, Resistance, ordinary
absorption, and actual invulnerability. Fortitude also absorbs it. Apply no
second Dodge or blocking check; the added portion is not suppressed by the
original hit's hurt immunity and does not extend that window. Both the original
successful hit and its added magic portion must apply; do not use a naive second
ordinary damage call that lets hurt immunity discard the magic portion. The
active buff lasts six seconds (120 game ticks) at base, scaled by the caster's
existing skill-duration multiplier. Activation costs 40 Mana at base and has a
25-second (500-game-tick) base cooldown, using the existing resource-cost and
cooldown multipliers.

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
Frost patch updates reuse membership sets for linear recipient reconciliation;
the derived slow changes only when its first source arrives or last source leaves.
Patch and recipient ticks still check coverage and cover, preserving immediate
exit/obstruction removal and reapplication after cleansing while inside a patch.
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

Rend and Rip and Rush's circular opening implement these rules. Other dependent
bleed skills remain placeholders. Bleed has a shared cap of
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
Lightning sends one dimension-scoped endpoint payload per beam segment to nearby
players; clients generate the bounded spark samples. The skills channel registers
this cosmetic payload alongside the existing skill/dash messages. Damage and target
selection remain server-owned; the visual message cannot activate a skill.

`covenant:skill_shot` is an unsaved, tracking-synced boolean attachment marking
skill arrows for visuals only. Ordinary arrows are unaffected. Its trail stops
on a terminal/block impact or after five seconds; the native arrow itself retains
its normal lifecycle. Unloading/reloading an arrow does not retain this cosmetic
marker. Removing it never changes skill damage classification, outgoing Rage
eligibility, or committed Enchanting behavior.

`covenant:bow_draw_until` is an unsaved, tracking-synced cosmetic player deadline
in level game ticks. Client-only Mixins scope the native bow-use pose/model reads
to first-person rendering and player render-state extraction, restoring the render
context even if rendering throws. Simulation and input continue to see real item-use
state. No player use flags or inventory values are changed to animate the bow.

### Combat feedback pass

Presentation prioritizes useful feedback and clear aim space. Native positional
sounds use the **Players** volume slider, include the caster, and follow ordinary
distance attenuation. Sound choices and volumes are provisional pending listening
in the client; no new audio assets or repeating sound loops are introduced.

- Accepted wind-ups play one quiet start cue. Successful execution plays one
  release cue; canceled casts and rejected requests cannot replay that release.
  Multishot plays one release for its whole five-arrow volley.
- Arcane, Fire and Ice projectiles play one collision cue when they terminate.
  Lightning plays an endpoint cue only on an entity or block collision. Collision
  cues do not imply damage; ordinary arrow collision audio remains native.
- Melee whiffs retain the release sweep. Positive health, absorption or Covenant
  shield damage adds one contact accent per attack, regardless of victim count.
  Combo dashes play that accent only for their first damaging contact. Bleed,
  aura refreshes and damage-area pulses do not add repeated sound cues.
- Traps have separate throw, landing, armed and trigger sounds. Their deployment
  outline matches the square contact region; active patches retain circular
  covered-area boundaries. Flight has no ground outline. Trigger particles now
  distinguish Bear, Frost, Flame and Blast.
- Teleport plays endpoint sounds, with a single source cue for travel under two
  blocks to avoid overlapping duplicates. Buff activations use brief recipient
  particles matching their element or class; passive refreshes remain quiet.

Fire, Ice, Lightning and Arcane use distinct cast particles. Fire/Ice trails and
impacts add small flame/snowflake accents. Arcane Arrow uses blue magic accents,
Aimed Shot adds a brief critical-impact accent, and Multishot uses fewer particles
per arrow. Rip and Rush's circular stages show a full circle; Cleave and Lunging
Combo's frontal finisher retain their forward arc.

`covenant:skill_shot_style` supplements the existing cosmetic arrow marker:
0 = Piercing/default, 1 = Aimed, 2 = Arcane, 3 = Multishot. It is tracking-synced,
never saved, and clears with the marker on terminal impacts. Saved projectile
provenance remains independent. Skills network revision **6** requires matching
clients for cosmetic arrow styles and the buff display below. Damage, costs,
cooldowns, cast timing and movement are unchanged by the presentation passes.

Manually check repeated casts, mixed-element combat, five-arrow volleys, all trap
transitions, frontal versus circular attacks, and sound balance for nearby players.
Check first-person aim visibility and reduced particle settings as well as normal
third-person viewing. Dedicated GameTests verify emission and gameplay boundaries,
not audible quality or rendered readability.

### Status icons, aura outlines and trap models

The top-left status HUD uses native Minecraft item and effect icons. Open combos
show stage/timer indicators; Discharge shows fire/ice/lightning charge counts;
Threefold shows reserve percentages or active damage bonuses plus remaining time.
Received aura passives and activations, Battle Cry, Fortitude shield capacity,
Blood Fever stacks and Rip and Rush's speed bonus use small status icons. Cool
accents denote passives and gold denotes timed activations; hover tooltips while
a screen is open name the effects and values. F1, death and spectator mode hide
the HUD. Vanilla potion icons remain native. The resource bar above hunger stays,
while its old shield/combo/Weaver text rows are removed.

`covenant:buff_display` is an unsaved owner-only attachment containing a skill-clock
offset and at most 32 rows (skill/status ID, passive presence, active deadline,
stack count). Existing aura grant/refresh/expiry/consumption, shield changes,
Threefold transitions, Blood Fever count changes and combo speed transitions
update changed snapshots. Passive coverage refreshes do not repeatedly sync
unchanged presence. Recipients without a class can see allied buffs. Logout,
death, class/dimension changes and source cleanup retain their existing gameplay
boundaries and clear the applicable display. No display state authorizes gameplay.

Slotted area auras show one thin local-owner ground outline at their shared six-block
radius, near foot height. Dodge, Swiftness, Ripping, Fortitude, Freezing, Shock,
Immolation and Enchanting use this field. Raging, Icy Veins and Flame Cloak are
self-only; Battle Cry and Discharge are not persistent aura fields. Shock chains
are centered on struck targets, so they do not add a misleading caster ring.
The outline shows a horizontal footprint of the existing spherical range; terrain
can obscure it. It does not change ignored-cover or vertical-distance rules.

Duplicate slots/shared radii merge into one outline. Phase 2 replaces dust with
a quiet broken ring of depth-tested quads that survives Minimal particles.
At most 56 of 64 segments sample dry collision support at the owner's elevation;
there is no trap-style center-to-edge cover ray, because aura gameplay ignores
cover. Missing support leaves gaps; marks never project onto another floor or
promise coverage at another elevation. Terrain is sampled each moving owner tick,
or every five stationary ticks. Extraction snapshots geometry and interpolated
owner position into native level render state; submission reads that snapshot.
Unslotting, death, spectator mode and player/level replacement stop the outline.
There are no new server packets, entity scans or forced chunks.

All four traps now use original native item models with opaque 32x32 atlases
at 16 texels/block. Bear has open opposing toothed jaws and an inset pressure
plate (14 cuboids); Frost has its low iron frame and fractured crystal (12);
Flame has an open vented ember cage (11); Blast has an iron-bound wrapped charge
and a solid pressure primer (5). Blast has no lit fuse or countdown effect.
A lazily created client-owned visual stack selects `items/<skill_id>.json` ->
`models/item/<skill_id>.json` -> `textures/item/<skill_id>.png`; there is no new
registered or saved item. Native lighting and reload-aware model resolution remain
in use. Visual stacks are allocated only on actual in-world extraction;
26.1 startup renderer factories run before item components are bound. The model's
`particle` texture aliases `#0` so resource reload has a valid fallback sprite.
Smaller devices tumble about their center in flight; each has a lower arming silhouette and incomplete
corner marks, then full height and the complete 1.5-block square when armed.

Each spent device disappears on trigger. Every armed device has its essential
contact square; Frost/Flame switch to a 3.5/3-block circular patch footprint.
Bear and Blast have no lasting area. Thin native depth-tested quads use a dark
backing and material/element-colored inner edge,
including on Minimal particles. Up to 64 segments sample loaded collision support
at the trap's elevation and the existing low cover-ray geometry. Unsupported or
covered samples leave gaps; nothing projects onto lower floors, and the marking
does not guarantee coverage on another elevation. Checks cache for five ticks per
weakly held client entity and rebuild immediately on phase changes. State removal
stops geometry immediately; no global scan, retained world reference or chunk
forcing is introduced. These are sampled footprint marks, not terrain decals.

Sparse supported frost fractures/low flame embers and small ready particles use
native particle settings. Bear's actual contact snap has opposing inward crit
streaks and an iron close sound; Blast has one brief pressure/poof burst and the
existing explosion sound. Flame ignition hands off to its patch without leaving
the device. Natural Frost/Flame expiry emits a native entity event for a small
breakup/extinguish cue and quiet sound; cancellation/removal does not play false
natural-expiry cues. No extra gameplay timer is synced, and damage/control/
ownership/lifetimes are unchanged. Small iron/root, angular freeze and snowflake/
Chilled target cues use existing synced status state, with freeze taking visual
priority and no invisible-target reveal or extra networking. Passive aura refresh
adds no activation or received-buff burst.

Original `textures/gui/skills/<skill_id>.png` provides a native 16x16 tree icon for
all four traps; initials remain for unauthored skills and very small nodes.
Editable sources/briefs: ignored `models/<skill_id>/`; mapped headless previews:
`.local/blockbench/frost-trap/` and `.local/blockbench/phase2/`. No rendered
Minecraft acceptance or comparative performance claim follows from previews.

Manual checks: inspect top-left layout at normal/high GUI scale, F1 and open-screen
tooltips; follow combo windows and Threefold active expiry; consume a Dodge charge
and Fortitude shield; slot/un-slot overlapping and self-only auras while moving;
compare All/Decreased/Minimal particles and terrain slopes; inspect grounded traps
on full blocks and slabs, in darkness and after trigger. These rendered behaviors
remain user playtesting work.

## Skills window and bindings

Hold G, hover radial slot 1 (**Skills**), then release G or left-click. The window
continues after G is released and does not pause the game. Escape or Done closes it.
Only the current class's tree is visible. Choose a class on the Stats page (radial
slot 2) or through `/covenant class select <id>` first. The 62-node roster and layout
follow `Project-Covenant/Skill Trees Brainstorm.md`: 19 Stalker, 20 Reaver, 23 Weaver.
Left-click a node to draft a rank; right-click to remove pending ranks only.
The bottom-right Apply button commits the batch out of combat. Closing discards
unapplied changes. Gold means committed; cyan marks pending upgrades; gray nodes
are blocked. Tooltips show unlock levels, available/planned ranks, prerequisites,
exclusions and implementation status. All eight tiers fit on the page at once,
with higher tiers above lower tiers. Empty lanes are compressed without changing
branch order or connections; temporary initials remain until icons are added.
No tree scrolling or panning is required.

Unlock levels gate first investment only. All 11 links retain their prerequisites;
Ripping Aura and Fortitude Aura remain exclusive. At most eight distinct skills
can hold points. Unimplemented nodes and branches behind them cannot be bought.
Each earned character level supplies one point. Piercing/Aimed Shot allow eight
ranks, each additional rank adding 2% base launch velocity with native speed-based
damage retained. All other playable skills currently allow Rank 1, with planned
8/12 caps retained. Applied ranks require the crafted respec item to refund.

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
player attachment. Its `ranks` map stores committed skill ranks; `bindings` maps
string-serialized indices 0–7 to purchased IDs. Keyboard choices remain local
in vanilla options, while assignments follow the player across machines.
Multiple slots may reference one skill. Respec clears all ranks/bindings and
cancels owned activity while preserving resources and committed cooldowns.
Operator class switches retain inactive skills and assignments; assignment,
activation and cast completion still validate the selected class.

The bounded Apply payload sends expected and desired rank maps. The server
validates the full batch before a single immutable snapshot update, including
combat state, points, unlock levels, rank limits, class, eight-skill ceiling,
prerequisites, exclusions and executable availability. Stale/replayed requests
cannot buy twice. Bind/clear and activation remain intent-only packets with
the existing shared per-player ingress budget. The older Learn intent routes
through the same purchase validation; unlearning through it is rejected.
Neither path supplies costs, timers, damage or targets. Payload protocol is 3.

This progression format requires fresh saves. No free-skill conversion or legacy
profile/loadout decoder is supplied. New saves retain normal persistence, death,
End-return and owner-sync guarantees. `SkillTree` owns tree/rank availability;
`SkillDefinition` remains the executable effect catalog.

## Resources

All classes now share the existing seven-second/140-tick server combat deadline.
Confirmed combat damage and qualifying support refresh it; Rage grace, Apply and
out-of-combat respec read that same timestamp. Support grants no Rage. Existing
damage-based Rage coefficients remain. See [PROGRESSION.md](PROGRESSION.md).

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
skill arrows use saved `ProjectileCombat` provenance, independently of their visual trail.
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
| `covenant:disengage` | Stalker | 20 Focus | 6 s | Instant | Four-block backward hop over six ticks, with a half-block arc and fall break; works airborne. |
| `covenant:arcane_arrow` | Stalker | 30 Focus | 8 s | Instant | Native arrow with 4 magic damage; damaging hits apply +15% damage taken for four base seconds. |
| `covenant:multishot` | Stalker | 30 Focus | 8 s | Instant | Five-arrow/30-degree fan, coefficient 1.5 each, one ammo item and one attempt per target. |
| `covenant:cleave` | Reaver | 20 Rage | 3 s | Instant | Deal effective native attack damage to visible non-allied living targets within 3 blocks and a 120-degree horizontal forward arc. |
| `covenant:rend` | Reaver | 20 Rage | 2 s | Instant | Strike the crosshair target within native held-item melee reach for effective attack damage; damaging hits apply one 5-second bleed stack at 10% captured attack damage per second. |
| `covenant:lunging_combo` | Reaver | 20 / 10 / 20 Rage | 12 s from combo end | Lunge, then instant attacks | Four-block/three-tick lunge, 125% single-target strike, then three-block/120-degree 100% cleave and shared armor shred. Fixed 2 s / 3 s follow-up windows. |
| `covenant:rip_and_rush` | Reaver | 30 / 20 / 20 Rage | 20 s from combo end | Instant areas, with dash | Three-block circular Rend, six-block/four-tick 50% dash and shared shred, then circular 100% finisher consuming shared bleeds for +10% base speed per enemy (max +30%) for four base seconds. |
| `covenant:arcane_bolt` | Weaver | 20 Mana | 2 s | 0.5 s | Fire a straight amethyst-rendered projectile; one collision deals 6 attributed indirect-magic damage. |
| `covenant:fire_bolt` | Weaver | 20 Mana | 2 s | 0.5 s | Straight single-hit projectile; 6 fire damage and 4 seconds of native burning. |
| `covenant:ice_bolt` | Weaver | 20 Mana | 2 s | 0.5 s | Straight single-hit projectile; 6 ice damage and 30% Chilled movement slow for 3 seconds. |
| `covenant:lightning_bolt` | Weaver | 20 Mana | 2 s | 0.5 s | Instant aimed hit after the cast, up to 24 blocks, stopping at the first eligible enemy or solid block; 6 damage with no status. |

The three elemental bolts use attributed `covenant:fire_bolt`, `ice_bolt`, and
`lightning_bolt` damage types. Like Arcane Bolt, damage bypasses armor and shields,
but retains Dodge, native hurt cooldowns, resistance, enchantments, absorption,
and invulnerability. Fire is tagged as fire damage, including native fire immunity
and Fire Resistance. Lightning uses no native lightning entity, so it does not
start fires or transform mobs. Fire/Ice travel at 1.5 blocks/tick without gravity,
stop on their first collision, and expire after 100 ticking ticks; projectiles are
transient and do not survive chunk unload/restart. Aim resolves at cast completion.

Fire/Ice apply their status only after positive health or absorption damage.
Duration snapshots the caster's skill-duration multiplier at firing. Native burning
uses vanilla burn damage, extinguishing, burning-time modifiers, persistence, and
combat credit; it is one shared fire timer, without per-caster stacks or custom DoT
ownership. Chilled is a native saved/synced harmful effect with a relative timer,
normal immunity/removal rules, and no offline catch-up. Reapplication retains the
longer duration without stacking. Chilled, Frost Trap, and Freezing Aura contribute
only their strongest Covenant slow; vanilla Slowness remains independent. Neither freezing nor
Shock is implemented by these bolts. Learning, class restrictions, casting
cancellation, spending, and cooldown persistence reuse the existing skill boundary.
| `covenant:bear_trap` | Stalker | 20 Focus | 8 s | Instant | Toss a single-use trap: 4 physical damage and a 3-second root on a damaging snap. |
| `covenant:frost_trap` | Stalker | 30 Focus | 12 s | Instant | On contact, form an 8-second, 3.5-block patch with a source-aware 50% movement slow. |
| `covenant:flame_trap` | Stalker | 35 Focus | 12 s | Instant | On contact, form a 6-second, 3-block patch: 3 fire damage on entry and at most once per second per target. |
| `covenant:blast_trap` | Stalker | 40 Focus | 15 s | Instant | On contact, explode once: up to 10 explosion damage and a low outward shove, with 3.5-block falloff and cover checks. |

All bow skills require a main-hand `BowItem` and supported arrow ammunition, use
native arrow collision/ammunition handling, honor native ammunition-use
enchantments (including Infinity), creative/infinite arrows, and wear the bow.
Arcane Arrow's fixed magic impact replaces native arrow damage as described above;
the other bow skills retain native damage and enchantment handling.
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
native Creative/reach adjustments and block clipping. An empty crosshair,
out-of-reach target, or intervening wall produces a paid whiff and normal cooldown.
Attempted strikes spend normally even if blocked, dodged, or immune;
only positive health, absorption, or Fortitude damage applies or refreshes bleed. Its initial
hit follows the native melee damage pipeline without adding vanilla critical,
sweeping, attack-charge, or on-hit enchantment chains, matching Cleave's boundary.
Arcane Bolt uses Minecraft's throwable-projectile collision and damage pipeline, has
no gravity, expires after five seconds, and creates no fire, terrain damage, or area
effect. Active bolts are deliberately not saved across chunk/world unload. It uses
magic damage, with no elemental resistance/status framework.

`SkillDefinition` owns stable ID, translated name/description, allowed class set,
base cost, cooldown/cast ticks, effect kind, and family-specific power/range.
Trap power/range fields are unused zeroes: `TrapKind` owns damage, effect radius,
and duration, while `Traps`/`TrapEntity` own deployment/contact behavior.
Add a definition, translations, and an explicit effect-family branch for future simple skills. There is no
datapack registry or generic effect graph. Existing `resource_cost`, `cooldown`, and
`cast_time` native multipliers scale their respective bases; `skill_duration` scales
Rend's bleed, Bear's root, and Frost/Flame patch duration. Fractional timers round up to whole ticks, with a minimum
one-tick bleed duration.

`Skills` owns activation validation and resource/cooldown commitment; `BowSkills`,
`MeleeSkills`, and `SpellSkills` implement their existing execution families.
`PendingCast` owns one unsaved start-time snapshot and idempotent cast presentation
cleanup, including Aimed Shot's movement modifier and bow pose. Family helpers do
not introduce a second activation or resource-spending path.

`SkillLifecycle` centralizes class/logout/dimension cancellation and validated
loadout edits. Confirmed-death cleanup runs only after the native death hook has
finished and accepted the death, so another mod's canceled death does not delete
active skills or target statuses. Persistent target wounds remain independent of
their caster's departure; saved resource/cooldown/loadout ownership is unchanged.

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

Learn/bind/use packets pass a separate unsaved per-player ingress budget before
validation or feedback: sixteen distinct requests may burst, one token recovers
per server game tick, and identical requests in the same tick are ignored.
Excess traffic is silently dropped. Invalid requests consume the budget too;
rejected requests cannot create an unbounded feedback/synchronization loop.
Dash acknowledgements remain outside this budget so a GUI/input burst cannot
starve movement cleanup. Direct server commands retain their usual validation.

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

Native projectiles save captured `covenant:aura_projectile_power` and
`covenant:enchanted_shot` values at their existing IDs. The optional server-only
`covenant:projectile_combat` attachment saves skill provenance and UUIDs of targets
that already received that projectile's Enchanting bonus. A reloaded native arrow
keeps its saved base damage without applying the captured multiplier twice;
skill arrows remain excluded from ordinary-shot bonuses and outgoing Rage even
after their cosmetic trail disappears. This does not make unsaved skill bolts
or traps persistent.

These projectile fields are additive. Existing player/item IDs and formats remain
unchanged. Old projectiles without metadata retain their native damage and the
ordinary-shot fallback; previously discarded skill identity, captured bonuses,
and target history cannot be reconstructed. No migration guesses that missing state.

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

For the new Stalker skills, purchase Rank 1 at their unlock levels and assign
them through the existing Keybindings tab. Manual client checks:

1. Disengage on the ground and in midair while looking up/down, then near walls
   and low ceilings. Confirm the short backward hop feels smooth, breaks the
   existing fall, permits shooting, and leaves normal falling afterward.
2. Arcane Arrow against an ordinary and an armored target, then a blocking
   player (native magic bypasses that shield). Check the vulnerability status,
   the following burst window, and tipped/spectral ammunition feedback. With a
   Weaver, confirm Shock combines
   with vulnerability without duplicate status strength.
3. Multishot against a spread-out group and a close single target. Confirm the
   five-arrow fan, one ammo spent, one recoverable center arrow, and no shotgun
   damage. Check movement/shooting visuals from another player's view.
4. Reconnect or change dimension during a leap, and reload a target with an
   active vulnerability. Confirm cleanup, saved cooldowns and remaining status
   duration. Automated native save/load checks do not replace a retained-world
   restart or real-client latency testing.

Use a disposable world in the isolated NeoForge instance. Select a class with the
existing `/covenant class select <id>`, or switch with the operator-only command above.
`/covenant resource set <amount>` edits the active resource (clamped) for tests.
`/covenant resource infinite` toggles an operator-only per-player debug bypass;
`/covenant resource infinite true|false` sets it explicitly. It bypasses resource
requirements and spending for the selected class, including cast completion and
combo stages. Stored balances, HUD values, recovery/decay, and other activation
rules remain ordinary; switching the mode does not fill or drain a balance.
Disabling during a cast restores its normal resource check at completion.
`/covenant resource` reports the mode. The unsaved runtime flag survives class
switches on the same player but resets on reconnect or replacement/respawn.
`/covenant skill ignorecooldowns` toggles a separate operator-only, self-targeted
cooldown-check bypass; `ignorecooldowns true|false` sets it explicitly. It covers
instant skills, cast start/completion, and combo starts. Existing cooldowns are
preserved, and successful activations still commit normal timers; disabling
immediately restores remaining cooldowns, including a pending cast's completion
check. Resources, equipment, learning, cast times, control restrictions, combo
windows, and the one-tick request throttle still apply. `/covenant skill` reports
the mode and actual cooldown times. Like infinite resource, this unsaved flag
survives class switching on the same player and resets on reconnect/respawn.
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
Use a fresh save. Select a class, draft upgrades, remove pending ranks and close
without Apply; reopening should show only committed ranks. Apply a valid draft
out of combat and confirm points, bindings and effects follow the server result.
Try level-locked, unimplemented, prerequisite and exclusive nodes. For rapid
testing an operator may use `/covenant progression level 60`.
Check the cyan pending state, all tiers at each GUI scale, and draft retention
between tabs. Only committed skills should appear in Keybindings.

Assign a purchased skill to all eight slots, then clear one slot. Test normal
and Shift bindings, cooldowns, Controls remaps, reconnect and dimension travel.
Craft a Combat Respec with four paper and one lapis, hold it for two seconds
out of combat, and confirm all ranks/bindings clear while cooldowns/resources
remain. Release early and enter combat during use: neither should consume it.
Inspect the unlabeled bar below vanilla XP and exact Stats-page progression.
Retained-world restart, rendered feedback and live multiplayer remain manual.

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
   authored families through `affix set`, including `covenant:resource_cost`;
   resource attributes without an authored affix family use `stats set`.
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
The tree/loadout fixtures cover the roster, class/level/point/rank limits,
prerequisites, exclusions, atomic batches, stale snapshots, spectator/combat
rejection, bounded Apply transport, respec cleanup, actual player-file ranks
and bindings, and death/End copying. Progression fixtures add native rewards,
support, Wither claims, caps, raw-XP balance preservation and real server reloads.
Bleed fixtures cover native attack-range components and clipping, defended strikes,
absorption-only application, native multipliers, shared replacement/refresh/credit,
independent hurt cooldown, mitigation, exact expiry, consumption, saved relative
timers and ownership, inactive-target pause, offline attackers, and non-death versus
death copying. The persistence fixture resumes a restored mob through real entity
ticks after controlled pulse checks. Retained-world process restart remains manual.
Live client reconnect, dimension travel, retained-world restart and rendered feedback/projectiles
remain manual checks; codec tests alone do not establish observed client delivery.
