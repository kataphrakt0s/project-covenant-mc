# Skills, class resources, and skill trees

This foundation shares the existing class/profile and native stat/Affix Page systems.
It adds three code-backed skills, one persistent skill-state attachment, and one
unsaved entity-local cast/combat attachment. The Skills window presents the current
class's brainstorm tree with free allocation and eight saved assignments. Planned
nodes have no gameplay effects yet. Points, ranks, an action-bar HUD, pack
dependencies, and an alternate character system remain outside this pass.

The roster, learning prerequisites, and agreed gameplay direction are in
[Skill Trees Brainstorm](<../Project-Covenant/Skill Trees Brainstorm.md>).

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
players after accepted cast starts/successful execution. No new draw time,
targeting, damage, or resource rules are introduced. Custom character animation
rigs are outside this scope.

Arcane Bolt uses blue-white magic effects: a brief charge during its existing
half-second base cast, a sparkling projectile trail, and a small burst on impact.
These are presentation changes; its existing cast timing and single-hit damage
behavior are preserved.

Cleave uses a vanilla-style weapon swing, a brief circular sweep, and red sparks
on enemies hit. The circular effect reflects its existing all-around targeting
within three blocks; the targeting and damage rules are unchanged.

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
original skills keep their effects; all other nodes are placeholders. Allocating or
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
| Reaver | Rage | 0 / 100 | Build 10 units/second in combat; otherwise lose 10 units/second. |
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
Generation depends on elapsed server ticks, never damage size, hit count, or target
count. After exactly seven seconds without qualifying damage, the next resource
pulse depletes Rage. Switching away stops Rage changes; combat grace itself expires
on the shared server clock. Rates and grace use game time, so server lag slows them.

Tune base values in the world's `serverconfig/covenant-server.toml`:
`focusRegeneration` (25), `rageCapacity` (100), `rageGeneration` (10),
`rageDepletion` (10), `manaCapacity` (100), `manaRegeneration` (5).
All accept finite values in [0, 1000000]; rates are units/second, capacities are units.
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
| `covenant:piercing_shot` | Stalker | 30 Focus | 2 s | Instant | Fire one bow arrow at full speed with base damage coefficient 3 instead of vanilla 2 (+50%). |
| `covenant:cleave` | Reaver | 20 Rage | 3 s | Instant | Deal effective native attack damage to nearby non-allied living targets within 3 blocks and line of sight. |
| `covenant:arcane_bolt` | Weaver | 20 Mana | 2 s | 0.5 s | Fire a straight amethyst-rendered projectile; one collision deals 6 attributed indirect-magic damage. |

Piercing Shot requires a main-hand `BowItem` and supported arrow ammunition, uses
native arrow collision/damage/enchantment handling, honors native ammunition-use
enchantments (including Infinity), creative/infinite arrows, and wears the bow. Its name refers to increased
damage; it does not grant Minecraft's multi-target piercing. Cleave uses native melee
damage mitigation without replacing normal attacks, applying a second attack-speed
formula, or triggering vanilla sweeping/critical chains. Unarmed melee is permitted.
It excludes allies, the caster's own tame pets, spectators, and disallowed PvP targets.
Arcane Bolt uses Minecraft's throwable-projectile collision and damage pipeline, has
no gravity, expires after five seconds, and creates no fire, terrain damage, or area
effect. Active bolts are deliberately not saved across chunk/world unload. It uses
magic damage, with no elemental resistance/status framework.

`SkillDefinition` owns stable ID, translated name/description, allowed class set,
base cost, cooldown/cast ticks, effect kind, power and range. Add a definition,
translations, and an explicit effect branch for future simple skills. There is no
datapack registry or generic effect graph. Existing `resource_cost`, `cooldown`, and
`cast_time` native multipliers scale their respective bases; durations are unused
because these effects are instantaneous. Fractional timers round up to whole ticks.

`Skills.activate(ServerPlayer, SkillDefinition)` is the common server entry point:

1. Require the server thread, living non-spectator player, permitted class, learned skill,
   ready cooldown, no pending cast/activation in this tick, and effect prerequisites.
2. Calculate effective cost/timers and check available resource. Snapshot those
   values for this activation; later equipment changes do not reprice the cast.
3. For a timed cast, wait on server player ticks. Reject concurrent activation.
   Class change, death, logout/replacement, or dimension transition cancels it.
4. At completion revalidate class, state, cooldown, effect prerequisites, and resource.
   Rejection spends nothing and starts no cooldown.
5. Consume the active balance and commit the cooldown before applying any effect.
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
Cooldown changes/spending synchronize immediately; native attribute packets and server
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
2. Reaver: hit or take a hit from a mob, inspect Rage as it builds, then stand near
   several targets with a sword and run `/covenant skill use covenant:cleave`.
   Rage can be seeded with `resource set 60`. Stop fighting for seven seconds and
   confirm depletion; another real hit refreshes grace without granting an instant
   hit-count-based bonus.
3. Weaver: aim and run `/covenant skill use covenant:arcane_bolt`. Confirm casting
   feedback, delayed spend/projectile, single-target damage, and cooldown. Reissue
   during casting, switch classes during a cast, and try the other classes' skills.
4. Inspect balances, switch away/back, die, travel dimensions, reconnect, and save/
   restart the world. Balances/cooldowns must persist without refill or duplication;
   pending casts must cancel. Use `stats set covenant:mana_capacity 2` for a 200-unit
   temporary maximum and `stats set covenant:focus_regeneration 1.25` for its limit.
   `stats reset` removes all temporary stat overrides. Equipment testing uses the
   existing `affix apply` command with the five resource attribute IDs.

Run `scripts/Dev.ps1 test` for dedicated-server fixtures `skill_lifecycle`,
`skill_recovery`, `skill_effects`, `skill_loadout`, and `skill_tree`. They exercise actual player-file save/load,
death/End copying, snapshot codecs, class changes, commands/permissions, native page
modifiers and bounds, timed recovery/combat refresh/depletion, real projectile/area
damage, cast overlap/cancellation, duplicate rejection and cost scaling.
The tree fixture additionally covers every brainstorm link, both exclusion directions,
free level-one learning, dependent/slot cleanup, inactive-class and spectator rejection,
placeholder no-op activation, full-tree player-file persistence, and death/End copying.
Live client reconnect, dimension travel, retained-world restart and rendered feedback/projectiles
remain manual checks; codec tests alone do not establish observed client delivery.
