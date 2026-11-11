---
project: Project Covenant
type: implementation-plan
status: gothic-rework-implemented-client-review-pending
updated: 2026-11-10
---

# Visual polish roadmap

## Bolt correction — particles as the body

The user rejected the floating-item appearance on 2026-11-10. Fire, Ice and
Lightning should read as flaming, icy or electrified bolts of pure energy.
Basic geometry may support the effect, but particles supply its main body.
This supersedes the generated-item core treatment recorded in Phase 3 below.

Runtime now uses distance-spaced crimson flame wisps, pale-blue frost energy
with sparse fractures, and electric-blue flickering lightning with drifting sparks.
Arcane also replaces its item core with violet energy for family consistency.
Fire/Ice/Arcane retain only narrow, travel-aligned geometric filaments and a tiny
end cap for Minimal visibility. These have no item texture or inventory silhouette.
Elemental collisions break into particles; accepted-damage marks remain separate.
Existing icons and item texture assets are retained independently of projectile rendering.

Emission is local and bounded: at most fifteen energy particles per projectile
tick plus two elemental accents every other tick, sixteen sparks per lightning
link, and eleven particles per elemental collision. Lifetimes are two to seven
ticks. Native particle settings, depth tests and server-owned hit semantics remain.
Manual review: first/third-person motion, steep/end-on aim, particle settings,
pale/dark backgrounds, overlapping casts, impacts and resource reload. No Minecraft
rendering or comparative performance acceptance is claimed.

## Current handoff — runtime JAR

On 2026-11-10 the user requested packaging all implemented polish, including
the latest catalogue corrections, before continuing the rest of this roadmap.
Phases 1–5, the gothic rework and the icon follow-ups already use runtime code
and assets; the local catalogue displays those same texture files. No design
decision blocks this build. The catalogue drafts are selected for inclusion,
with final appearance still subject to Minecraft review. See
[validation evidence](../docs/VALIDATION.md).

Phase 6's movement and combo batches now implement Disengage, Teleport,
Lunging Combo and Rip and Rush. The Reaver signature batch adds Battle Cry,
Rage Storm and Blood Fever, plus Discharge and Threefold. All eleven aura
activations now have release/receipt, final expiry and applicable consumption
cues. Phase 6 implementation is complete; Minecraft review remains pending.
Enduring Cry's
future badge and the golem's gameplay/form decisions belong to later work and
do not block packaging the currently implemented polish.

## Current priority — reference review and art reset

On 2026-11-10 the user rejected the overall desaturated, overly realistic art
direction and requested a more cohesive, bold dark fantasy flavour. The wolf and
traps remain accepted anchors; everything else is eligible for eventual redesign.
Technical implementation below is not visual acceptance. Before expanding the
existing style, use [[Art Reference Studies]] to collect and compare references
for each asset family, then establish a representative sample together.
FurfSky's strong silhouettes and darker black/purple/red/grey examples now supply
selected reference qualities; its broader flashy, neo-fantasy styling is unsuitable.
Exact palettes and family treatments remain open. The earlier phase sequence below
records the implementation baseline and does not override this new priority.

The first runtime rework is implemented: 129 textures, distinct relic component
silhouettes, gothic UI framing, and an eight-cuboid stone/iron ritual workbench.
The user's elemental correction is crimson fire, pale blue ice and electric blue
lightning. Wolf and trap device art is preserved; Flame Trap feedback follows the
crimson palette. See [[Art Direction]] and the local
[before/after review](../.local/gothic-rework/review.html). Further art acceptance
depends on review of this implementation in Minecraft; earlier delivery notes
below describe the superseded art where they conflict with this rework.

### Current next step: individual icon review

The user finds the first rework's silhouettes too unique. Work through icons
one at a time, matching each player's intended reading to a close vanilla
Minecraft item/icon silhouette before adding restrained gothic colour/details.
Evaluate meaning and native-size recognition before extending a treatment to
other icons. Workbench is acceptable for now. See [[Art Direction]] for the
superseding icon rule; distinctiveness alone is not an acceptance criterion.

- Cleave: finished 16px sword-and-sweep icon **approved**, including the arc
  widening toward the blade. [Approved comparison](../.local/icon-review/cleave.html).
- Rend: finished sword-and-classic-teardrop icon **approved**, including the wider
  stain at unchanged length. [Approved comparison](../.local/icon-review/rend.html).
- Lunging Combo: finished crimson character lunge icon **approved**, with the
  steel sword. [Approved comparison](../.local/icon-review/lunging-combo.html).
- Rip and Rush: simplified crimson character with one straight sword arm and
  circular sweep **approved**. [Approved comparison](../.local/icon-review/rip-and-rush.html).
- Battle Cry: prior face rejected; fresh study from the user's manga/warcry
  references liked; requested mouth/jaw close-up now cropped from the source
  and implemented at 16×16 with eight colours for
  [approved comparison](../.local/icon-review/battle-cry.html). **Finished icon
  approved**, including the corner sword. Clean base and source study preserved
  for Enduring Cry.
- Future Enduring Cry: same face layout with a heart or shield corner badge;
  badge choice and implementation remain deferred.
- Rage Storm: finished cyclone icon **approved**, with exact prior colours
  retained. [Approved comparison](../.local/icon-review/rage-storm.html).
- Blood Fever and the remaining existing catalogue: **60 revised textures**
  implemented for [batch review](../.local/catalogue-review/index.html), covering
  skills, menus/professions, status, affix/respec items and matching bolt cores.
  These are review drafts. Approved icons, accepted boot, wolf, traps and
  workbench are preserved.
- Icy Veins: user-selected pale-blue hourglass implemented.
- Movement speed: previous single-boot silhouette retained exactly for the
  component and reused by Swiftness Aura, per user correction.
- Latest corrections restore Ice Bolt/Arcane Bolt and their cores; Rooted uses
  an independent circular bear trap, cooldown/duration affix images are swapped, and Dodge,
  Attack Speed, Combat Respec, Cast Time and Flame Cloak have clearer subject
  silhouettes. These revisions remain pending visual approval.

## Goal and first delivery

Make Covenant feel like Minecraft has an ancient, forgotten supernatural history.
Follow [[Art Direction]]: familiar materials and proportions, purposeful pixel
clusters, crafted hunting devices containing occult power, and expressive magic
at meaningful moments. Read the local reference index and relevant source images
before each asset brief. The approved logo guides original pixel treatment;
vanilla continuity determines how those ideas fit the world.

**Start with Frost Trap as one complete playable example.** It exercises a small
original model, materials, flight/arming states, a magical release, persistent
AoE readability and cleanup inside an implemented skill. It can establish useful
conventions without waiting for a new gameplay system. Include one small skill
icon so the same identity can carry into menus and the HUD.

This roadmap sets the production order. Phases 1-5 are implemented with
client review pending; current delivery
and validation boundaries are tracked below.
Phase numbers give the recommended delivery order, not a requirement to finish
every earlier asset before starting an independent UI or item-art task.

## Source-backed starting point

Reviewed current source and local asset documentation on 2026-11-10. These are
implementation observations and proposed improvements, not findings from a new
rendered usability test.

| Surface | Current foundation | Main opportunity |
| --- | --- | --- |
| Traps and AoE | Four original trap devices; supported depth-tested contact/patch outlines; one owner-only aura footprint. | Review silhouette, terrain/overlap readability and renderer cost in Minecraft. |
| Combat effects | Original everyday spell cores/sprites, exact-endpoint jagged lightning, bow-family streaks, Cleave/Rend contacts and quiet bleed pulses; native poses/sounds. | Review normal combat readability; movement and major-skill spectacle remain Phase 6. |
| Skills and bindings | Whole-class tree with initials, rank/tooltips, pending Apply and eight slots. | Shared original icons and clearer allocation/readiness states. |
| Professions | Four tabs, named nodes, rank text and pending markers. | Consistent icons, rank/cost hierarchy and planned/locked distinction. |
| Stats and radial | Text-heavy stats with raw modifier IDs; three active radial destinations. | Clear summaries, class discovery and recognizable navigation. |
| HUD | Current-class resource bar, thin Covenant XP bar, top-left statuses with vanilla icons. | Recognizable status art and legible counts/timers with the existing footprint. |
| Workbench and inventory | Original stone ritual station, direct/paged native choices, structured server previews, respec art and component-driven family/rarity marks. | Review in-game cost/consequence clarity, item recognition and shared-station latency. |
| Summons | Original static wolf source; pet and four-form golem gameplay remains planned. | A later entity/animation pipeline coordinated with companion implementation. |

Evidence lives in `src/main/java/dev/covenant/client/`,
`src/main/java/dev/covenant/rpg/skills/`, `rpg/equipment/WorkbenchMenu.java`,
`rpg/progression/Progression.java`, and `src/main/resources/assets/covenant/`.
Detailed existing combat/geometry contracts remain in [[Art Direction]] and
[RPG_SKILLS.md](../docs/RPG_SKILLS.md).

## Delivery sequence

| Phase | Deliverable | Why this order | Dependency |
| --- | --- | --- | --- |
| 1 | Frost Trap complete example | Establish physical art, occult effect language and area readability together. | Existing trap gameplay. |
| 2 | Remaining traps and aura boundaries | Reuse the first successful materials and boundary treatment. | Phase 1 range/state decisions. |
| 3 | Everyday combat effects | Give all three classes recognizable effects in normal play. | Phase 1 consumer lessons; each class ships separately. |
| 4 | Combat HUD, menus and navigation | Make skills, build choices and controls easier to recognize. | Can begin after Phase 1's icon sample; does not wait for all Phase 3 effects. |
| 5 | Workbench, items and progression feedback | Polish the complete find, salvage and customize loop. | Reuse Phase 1 materials and Phase 4 UI conventions; object art can start sooner. |
| 6 | Advanced skill spectacle | Apply proven class identities to complex effects and movement. | Corresponding elemental/melee/boundary families. |
| 7 | Hunter's Pet | Prove custom entity scale, animation and lifecycle with the existing wolf. | Companion gameplay decisions and native entity integration. |
| 8 | Elemental Golem | Reuse the companion pipeline for a more ambitious four-form summon. | Phase 7 pipeline; golem gameplay dimensions and form rules. |
| 9 | Cohesion and crowded-scene review | Assess the combined result across combat, menus and progression. | Review each shipped slice continuously; final pass uses the combined set. |

### Phase 1: Frost Trap

Ship in small steps within this first delivery:

1. Write an asset brief and make a simple blockout: a low iron hunting device
   binding a pale fractured crystal. Compare its size against the actual contact
   footprint and vanilla-scale materials. Inspect the supplied Ancient City and
   crafted-material references for restrained supernatural accents and construction.
2. Establish only the necessary material ramps, pixel density, a small rune motif
   and one native-size icon. Preview the silhouette and mapped texture before
   multiplying details or making the other trap variants.
3. Export and integrate the model, replacing the current flattened blue-ice
   selection and revisiting its transforms. Show thrown, landed/arming and armed
   states using existing entity state.
4. Add a brief trigger burst, readable frost patch boundary/interior accents and
   expiry cue. Keep actual damage/control timing and spent-core disappearance.
5. Wire the icon into the existing skill presentation with a fallback for other
   nodes. Tune sound timing/volume alongside the visual transitions.

**Completion evidence:** inspect the packaged model/texture references; verify
state transitions and cleanup where code changes warrant it; review the in-game
silhouette, trigger square, patch boundary and lifetime on grass, snow/pale ground,
dark stone, steps and ledges. The two states must not imply the same affected area.

Settle two rendering choices through this example: how supported surfaces and
occlusion are represented, and whether essential boundaries remain visible on
Minimal particles. Recommendation: preserve essential range information while
reducing decoration, with owner aura visibility still separate. A horizontal cue
must not falsely promise coverage at other elevations. These decisions do not
change the existing three-dimensional range or cover tests.

### Phase 2: Trap family and area clarity

Deliver **Bear, Flame and Blast individually**, then the passive aura outline.
Bear gets unmistakable jaws/pressure plate and a snap; Flame gets a vented ember
device and an ignition-to-patch handoff; Blast gets a compact charge/primer and a
short pressure burst. Share materials and density, but distinguish silhouettes
before color. A primer/fuse must not imply timed detonation.

Apply the boundary lessons to one merged owner-only aura range, preserving
self-only exclusions and the difference between aura and trap cover behavior.
Keep activation/received-buff cues separate from passive refresh. Add modest
root/freeze/chill recognition where existing vanilla feedback is insufficient.

**Completion evidence:** correct square/circle/volume semantics, no doubled
outlines for shared aura ranges, no lingering effects after removal or lifecycle
cleanup, identifiable trap silhouettes, and visible targets inside active patches.
Measure the chosen renderer in a representative overlap scene before claiming
it is cheaper than the current particles.

### Phase 3: Everyday combat effects

Produce one reference skill per class, then extend it in small batches:

| Batch | Work | Key distinction |
| --- | --- | --- |
| Class examples | Arcane Bolt, Cleave, Piercing Shot, one at a time. | Ordered spell geometry; forceful frontal weapon crescent; precise arrow streak. |
| Elemental spells | Fire Bolt, Ice Bolt, Lightning Bolt and its Shock-enabled chains. | Ember motion, angular fractures and connecting forks; distinct without relying only on hue. |
| Bow variants | Aimed Shot, Arcane Arrow, Multishot. | Real wind-up, magical hit, controlled fan; brightness/sound budget across the whole volley. |
| Melee/status | Rend and accepted bleeding ticks. | Single-target contact and sparse status feedback, distinct from a broad sweep. |

Use original compact projectile cores and sprite sequences where they improve
readability. Preserve native bow/weapon poses and ordinary arrows. Clearly
distinguish collision from a confirmed damaging hit; cosmetic lightning branches
must not suggest extra targets. The user approved a pale core with restrained
lavender/violet accents on 2026-11-10; Arcane Bolt establishes the family, now
extended to Arcane Arrow.

**Completion evidence:** each skill reads in first and third person, tracks the
actual hit/trajectory, leaves the crosshair and nearby targets visible, and gives
no false cue for a rejected use. Basic contact/release effects remain reusable.

### Phase 4: HUD, menus and navigation

Begin with a small shared set of icons and states, then apply it to each surface.
Use native fonts and widgets, clean stone/iron framing, readable light text and
small engraved accents. Keep ornament outside primary information and click areas.
Build only the shared rendering helpers needed by actual screens.

| Delivery | Concrete changes | Preserve |
| --- | --- | --- |
| Icons and combat status | Original icons for playable skills, auras, control states, combos, Discharge/Threefold; reuse identity across tree, bindings and HUD. Clarify counts, duration and active/passive state. | Resource colors/position, top-left status region, F1 behavior and native potion information. |
| Skills and bindings | Replace initials incrementally; distinguish available, learned, locked, planned, pending and assigned states using shapes/badges as well as color. Make pending point totals and Apply result obvious. | Whole tree on one page, prerequisite/exclusion meaning, eight assignments, actual rebound keys, click-to-assign/right-click-to-clear. |
| Professions | Four recognizable emblems, node icons, current/next-rank hierarchy and explicit pending costs. | Independent tab drafts, per-tab Apply and planned nodes remaining unavailable. |
| Stats and class choice | Visible class descriptions/emblems, grouped stat summaries and meaningful source labels with precise breakdowns available. | Server-owned class selection and actual native stat calculations; unknown modifier IDs retain a fallback. |
| Radial and discovery | Emblems for the three active destinations, clear dormant wedges and a concise path from class selection to learning/binding a skill. | Slot order, hold/release or click selection, Escape, non-pausing behavior and vanilla Controls. |
| Action feedback | Audit casting, cancellation, cooldown/resource/equipment rejection and successful Apply cues. Make messages concise and avoid repetitive success chat. | Real server outcomes and deadlines; no speculative success state. |

A permanent extra skill bar is not required for this pass. Evaluate missing
readiness/cast information with the existing HUD first; any new indicator needs
an actual authoritative source and a clear purpose. Static art replacement does
not justify new networking. Where a UX feature needs new presentation data,
budget it explicitly rather than deriving state from message strings.

**Completion evidence:** readable at small windows and supported GUI scales;
mouse and keyboard focus/tooltips work; no clipping or reliance on color alone;
hunger, air, mount UI and crowded statuses remain usable. A new player can find
class selection, learn one skill and understand its binding without commands.

### Phase 5: Workbench, inventory and progression

Treat the station's world object and its transaction screen as one experience,
delivered through smaller changes:

1. Replace the netherite-block placeholder with a recognizable crafting surface:
   user-selected stone ritual station, iron bindings and restrained carved marks
   (supersedes the initial timber proposal).
   Keep the first model compatible with current one-block occupancy. New facing
   or collision behavior is explicit follow-up work, not an accidental art change.
2. Show selected operation, input, cost, Potential change, possible result and
   rejection reason clearly. Replace awkward cycling where a native choice list
   is clearer; preserve native inventory interaction and server transactions.
   Rich previews need structured server presentation data; never parse English
   preview text or recompute random results on the client.
3. Add original Combat Respec and Profession Respec icons, plus affix component
   silhouettes/family marks. Start with representative variants; support the
   existing component-driven identities and unknown datapack entries without
   inventing new item IDs for every combination.
4. Improve equipment tooltip hierarchy for rarity, Potential, affix tier and
   readable values. Keep the underlying vanilla/modded equipment recognizable.
5. Add short accepted-operation and earned-level/point cues, and concise guidance
   for class discovery and the existing Crafting route. Preserve the thin XP bar;
   do not turn every XP gain into a popup or imply new recipes/unlock rules.
6. Review native gathering/replanting cues before adding anything. If needed, use
   one aggregate completion or actionable failure cue for a batch, with sneaking,
   protection denial, missing supplies and tool break represented honestly.

**Completion evidence:** a player understands the cost and consequence before
committing; failed/stale actions produce no success cue; storage and native
removal remain correct; level-up effects do not replay on login/reload/dimension
sync. Items read at inventory size. Large harvesting batches avoid duplicated
per-block spectacle and hidden-resource previews.

### Phase 6: Advanced class effects

Deliver these as separate skill groups after their base visual families exist:

| Group | Planned polish | Important boundary |
| --- | --- | --- |
| Movement | Disengage and Teleport departure/travel/arrival cues. | Match actual travel/endpoints and cancellation; keep first-person aim clear. |
| Reaver combos | Lunging Combo and Rip and Rush stage/contact/finisher effects. | Frontal versus circular attacks remain distinct; trails stop with actual movement. |
| Reaver signature skills | Battle Cry pulse, hollow Rage Storm volume, restrained Blood Fever response. | Shout is a recipient snapshot; storm is a moving 3D volume; passive stacks remain quiet. |
| Weaver signature skills | Discharge elemental release and Threefold activation/consumption/expiry. | Show actual consumed elements/strength; distinguish main burst from chains and avoid repeated banking spectacle. |
| Aura activations | Complete the active/received/consumed cues across all implemented auras. | Passive refresh is quiet; overlapping sources and shields do not create false full-screen feedback. |

**Completion evidence:** a strong moment has a clear preparation/release/end
rhythm where the mechanics provide it; no invented cast delay; multiple classes
remain readable together. Pair original icons with the same visual identities.

### Phase 7: Hunter's Pet

Reuse the existing wolf rather than redesigning it. First settle in-game scale,
rig needs and the minimum companion gameplay/lifecycle contract. The current
source has no animation clips or native playback integration.

Deliver scale/model integration, then idle/head tracking/locomotion, then attack,
hurt/death and summon/dismiss presentation. A short summoning seal can carry the
occult identity while the living animal stays natural. Optional collar/token
detail should follow the successful base integration.

**Completion evidence:** feet and joints behave through intermediate poses,
attack presentation matches actual contact, and summon/death/dismiss/owner
lifecycle transitions leave no stale effects. Full playable delivery depends on
the separate companion implementation; model exports alone are not completion.

### Phase 8: Elemental Golem

Resolve gameplay size/reach, form-selection timing and nearby-benefit rules before
finalizing the asset. Proposed form: old stone guardian, iron bindings and a
recessed elemental core. Author one base body/rig, integrate one working form,
then extend to fire, ice, lightning and arcane with shared proportions.

Distinguish forms through small silhouette changes, core motion and effects;
preserve the golem's identity across transitions. Scale spectacle alongside the
player's effects so its form remains readable without hiding combat.

**Completion evidence:** all four forms are identifiable, transitions follow
authoritative state, benefit cues match their actual range, and the shared rig
works in movement/combat poses. This phase depends on companion gameplay as well
as the entity pipeline proven with the wolf.

### Phase 9: Cohesion and continuing review

Review the complete journey: discover a class, learn/bind a skill, fight, earn a
point, inspect gear, salvage/customize, and use a companion when available. Check
mixed-class combat and overlapping effects alongside ordinary Minecraft lighting
and textures. Adjust inconsistent brightness, timing, material density, icon
weight and sound volume; fix the highest-impact readability issue first.

Keep visual clarity/performance checks in every phase. The combined pass adds
representative busy scenes, reduced-particle settings, resource reloads, different
GUI scales, distant culling and multiplayer observer views. Essential information
should survive reduced decorative effects. Measure before claiming performance
improvements; existing profiler scopes support attribution.

Future ruins, shrines, dungeon props, bosses, Ballista and other unimplemented
skills receive an asset brief when their gameplay/content is scheduled. Their
visual direction follows this roadmap, but they are not missing assets required
to finish polishing the currently playable systems. Avoid a global vanilla
texture replacement, permanent biome VFX or a shader requirement as incidental
polish work.

## Production and validation contract

- Keep a brief, editable source and export mapping for each family. Consult the
  ignored reference collection, pixel guide and modeling workflow when present;
  keep final runtime resources in Covenant assets and sources/previews ignored.
- Compare blockouts and native-size icon samples before expanding a family.
  Select concrete material ramps through these examples. UI and world effects
  share identity, but do not need identical detail density or palettes.
- Use native client renderers/resources and existing server-owned event/state
  lifetimes. Preserve saved IDs, hit geometry, costs, ranks, timings and ownership.
  Extend presentation synchronization only when a concrete cue needs it.
- Each runtime delivery gets `scripts/Dev.ps1 build`, relevant resource/geometry
  checks, and focused gameplay/network tests where its changes warrant them.
  Use `scripts/Dev.ps1 verify` for gameplay-affecting handoffs. Do not add tests
  that merely duplicate cosmetic constants or repeat checks without a reason.
- Report automated evidence separately from visual review. Headless model renders
  verify authored appearance, not Minecraft rendering. The user handles manual
  client playtesting; agent client automation/launches require a request under
  the development workflow. Record pending observations and continue independent
  work; pending manual review does not block technical delivery.
- Review sound timing with each effect family; original audio production is an
  optional later task if native sounds no longer fit the art.

Current tracking: **Phases 1-6 implemented; manual Minecraft review pending**.
The original Frost Trap uses twelve cuboids, a 32x32 opaque atlas at vanilla
texel density, and one 16x16 skill-tree icon with initials retained for other
skills. Sources and the asset brief live in ignored `models/frost_trap/`; mapped
headless previews live in `.local/blockbench/frost-trap/`.
Flight tumbles, arming has a lower silhouette and incomplete corner marks,
and readiness raises the device and completes the trigger square. The spent
device disappears at trigger, leaving a 3.5-block circular patch footprint,
sparse frost accents and a natural-expiry chime/breakup cue.

The user approved essential depth-tested boundaries on Minimal particles.
Same-elevation collision-support and low cover-ray samples leave gaps at walls,
steps and missing support. Marks never project onto lower floors; this is a
horizontal footprint, not a terrain-conforming guarantee of the spherical
gameplay volume. Decorative particles retain native settings. Terrain samples
are bounded and cached per deployment for five
ticks, with immediate rebuild on phase change. No comparative performance
measurement or Minecraft rendered acceptance is claimed.

Remaining Phase 1 review: grass, snow/pale and dark stone; slabs, stairs and
ledges; All/Decreased/Minimal; nearby observers; resource reload; readiness,
trigger and expiry timing; and lifecycle removal.

Phase 2 extends the family individually through Bear (14 cuboids, open toothed
jaws/pressure plate and contact snap), Flame (11 cuboids, vented ember cage,
ignition-to-patch handoff and natural-expiry extinguish), and Blast (5 cuboids,
bound charge/solid unlit primer and one short pressure burst). Each has an original
opaque 32x32 atlas at the same texel density and a 16x16 skill-tree icon. Sources
and briefs are in ignored `models/bear_trap/`, `models/flame_trap/`, and
`models/blast_trap/`; mapped previews are in `.local/blockbench/phase2/`.
All four devices now use the same native item-model path and supported
1.5-block square, switching only Frost/Flame to their real circular patch radii.

One merged broken owner-only aura footprint replaces the old dust points,
remaining visible on Minimal and excluding self-only/non-field skills. It samples
support at the owner's elevation but deliberately skips trap cover rays: aura
gameplay still ignores cover. Moving owners refresh terrain samples each tick,
stationary owners every five ticks. Native level render-state snapshots separate
extraction from submission. Small root/freeze/chill target hints use existing
synced state; passive refresh remains quiet. Gameplay rules and saved IDs remain
unchanged. Headless geometry/UV/resource checks and builds establish technical
delivery; no overlap-scene performance improvement is claimed.

Phase 2 client review: silhouettes at throw/arming/ready scale; contact snap and
ignition/pressure/expiry timing; supported square versus circular patch footprints;
grass, snow/pale and dark stone, walls, stairs, slabs and ledges; Minimal;
overlapping traps/auras and visible targets; one owner-only aura field with self-only
exclusions; root/freeze/chill recognition; resource reload and lifecycle cleanup.

Final Phase 2 automated validation: `scripts/Dev.ps1 build` and
`scripts/Dev.ps1 verify` passed, including **All 173 required tests passed :)**
and **BUILD SUCCESSFUL**. The packaged source/export/UV/PNG audit also passed;
see `docs/VALIDATION.md` for evidence and manual boundaries.

Phase 3's first reference batch delivers an original 16x16 pale/violet Arcane
core through native generated-item rendering, compact ordered motes and a
three-frame broken collision seal. Cleave has a three-frame 32x32 steel/crimson
120-degree crescent at the actual release position/yaw/range. Piercing Shot keeps
its native arrow and cosmetic bow pose, adding a two-tick narrow ivory/gold
motion-aligned streak. All three receive original 16x16 tree icons. A separate
compact contact cross requires accepted damage, including absorption/shields;
mere collisions, whiffs, prevented hits and rejected uses never claim success.
The shared melee contact helper also improves current Rend/combo hit marks;
their distinct skill/stage effects remain later work.

Sources and brief: ignored `models/combat_examples/`; sprite previews and
validation: ignored `.local/phase3/`. Five native decorative particle types use
short local lifetimes and normal particle settings; no new custom network
payload, saved data, targeting or timing changes. Arcane's entity core survives
Minimal through native full-bright item rendering, without bloom/world light.
No client rendering or crowded-scene performance claim is made.

Phase 3 client review: first/third-person core, charge cancellation, crescent
orientation at cardinal/diagonal yaw, frontal versus circular cues, visible
targets/crosshair, arrow motion including steep/vertical shots, wall/immune
collision versus accepted damage, pale/dark terrain, overlap, particle settings,
resource reload and projectile removal.

Phase 3 reference-batch automated validation: initial `scripts/Dev.ps1 build`
and final `scripts/Dev.ps1 verify` passed, including **All 174 required tests
passed :)** and **BUILD SUCCESSFUL**. Packaged sprite/reference/alpha/crescent-cell
geometry audit passed for 26 resource files. See `docs/VALIDATION.md` for native
emission checks and manual boundaries; these results do not establish Minecraft
appearance.

Phase 3's remaining batches now deliver original 16x16 Fire/Ice cores and three
elemental icons, rising embers, angular shard trails and element-specific short
collision sequences. Lightning uses <=64 jagged segments per real server link,
exact endpoints, <.079-block interior deviation and a four-tick 3D strip. No
decorative offshoot targets another enemy. Actual main-ray collisions and real
Shock endpoints have a small collision glyph; accepted damage has a separate
contact mark, including absorption/shields. A main-ray range miss has no false
collision or damage cue. Existing native sounds and gameplay stay unchanged.

Aimed Shot adds small gathering brackets only during its actual cast and a
stronger narrow streak. Arcane Arrow shares pale/violet geometry and a distinct
damaging-hit cross. Multishot shows its actual five-arrow fan with one small
two-tick streak per arrow/tick and one release sound for the complete volley.
Cosmetic style survives pre-damage entity collision so native accepted hits can
select the correct contact art; block impact clears it immediately, while native
entity removal/the existing age guard ends remaining trails. Saved provenance/fan claims
remain separate. Rend uses compact single-target crimson/steel slashes; accepted
bleed stacks share one small mark per target per pulse, with no extra sound.
Four matching icons complete the ten Phase 3 skills.

Nineteen native decorative particle types and three full-bright generated-item
spell cores now serve Phase 3. Sources/briefs also live in ignored
`models/elemental_spells/` and `models/bow_melee_effects/`; previews/evidence remain
in `.local/phase3/`. All 14 current original skill icons include the four traps.
The complete packaged resource audit covers 99 files and the production lightning
geometry test covers exact endpoints, vertical/zero-length aim, finite budgets
and bounded ornament. Minecraft rendering and overlap performance are unverified.

Additional Phase 3 client review: all elemental silhouettes in grayscale, ember
rise versus ice fracture, near/steep/vertical Lightning and cover-clipped chains,
real Aimed wind-up/cancellation, Arcane collision versus damage, whole-volley
brightness/sound, immune/blocked arrow contact, and quiet Rend/bleed pulses.
Phase 6 movement delivery and Phase 5 delivery are recorded below;
Phase 6's final aura group is recorded below; Phases 7-9 remain pending in this polish sequence.

Final complete Phase 3 automated validation: `scripts/Dev.ps1 build` and
`scripts/Dev.ps1 verify` passed, including **All 178 required tests passed :)**
and **BUILD SUCCESSFUL**. The production lightning geometry check and complete
99-file packaged resource audit passed. Native packet/physical-collision tests
cover actual links and cover, accepted elemental/bow hits, Aimed cancellation
and aggregate/prevented bleed pulses. See `docs/VALIDATION.md` for evidence and
the pending Minecraft/client boundaries.

Phase 4 shares original icons for all 35 playable skills across the tree, bindings
and top-left status region, with stone/iron menu framing. The user rejected the
first abstract icon study. The redraw favors recognizable boots, shields, blades,
a horn, wolf head, books, tools and elemental silhouettes. Its native/enlarged
pale/dark preview is ignored `.local/phase4/icons-v2.png`. Ten Phase 3 tree icons
are redrawn; spell-core/particle art is separate, and the four trap miniatures
retain their original machinery. Revised artwork still awaits user acceptance.

Tree badges/shapes and focus tooltips distinguish available, learned, locked,
planned, pending and assigned states. Locked/planned nodes remain inspectable
without becoming purchasable. Eight binding cards show actual rebound keys above
skill identity. Four profession emblems and node icons support current/draft/next
rank costs and per-tab drafts. Stats has visible class previews before explicit
permanent choice, grouped summaries and an optional precise native breakdown.
Known equipment/aura sources receive readable labels; unfamiliar IDs stay visible.
Three radial destinations have emblems, dim dormant wedges and a discovery hint.

Apply confirms only matching authoritative rank changes; conflicting updates and
five-second timeouts do not show success. Profession submissions are independent
per tab and clear on catalogue reload. Skill key requests, binding/Apply responses
and cast completion/equipment cancellation use native server action-bar messages
instead of repeated chat. No message parsing, new custom payload, saved data or
extra skill bar. Root/chill tiles read hidden native effects and real remaining
durations; freeze uses synced state without an invented expiry. Visible native
potion tiles are not duplicated. Solid/hollow marks distinguish active/passive
and reserve state. Resource colors/position, thin XP bar and F1 remain.

Phase 4 client review: revised icon recognition; small windows/GUI scales;
mouse/keyboard focus and tooltips; rebound keys and right-click clearing;
planned/locked/pending/assigned distinction; per-tab Apply, rejections and combat
gating; class preview/commit and command-free learning; crowded statuses and
controls; hunger/air/mount stacking; F1 and resource reload. Sources/briefs are
ignored `models/ui_phase4/`; evidence is in `docs/VALIDATION.md`. No Minecraft
appearance, latency/multiplayer or comparative performance acceptance is claimed.

Final Phase 4 automated validation: `scripts/Dev.ps1 verify` passed, including
**All 178 required tests passed :)** and **BUILD SUCCESSFUL**. The final UI build,
authoritative Apply checks and packaged 60-icon/62-resource audit passed. The
compatible combat audit passed for 120 resources / 19 native particle families.
Cast completion and equipment-cancellation tests verify real native overlay
packets and unchanged spending/ammo boundaries. These are technical checks;
Minecraft appearance and revised art acceptance remain pending.

Phase 5 now supplies the user-selected stone ritual station with a worn working
slab, iron bands and carved niches. One native cube and a 32x32 atlas retain
one texel per model unit, full-block collision and the absence of facing.
Original 16x16 Combat Respec parchment and Profession Respec ledger replace
placeholders. Ten component family marks and five rarity pip bands read the
existing synced component; unknown entries retain a generic charm and their
named identity, with no invented item IDs or rarity mapping.

Workbench operation buttons and paged native choice lists replace cycling in
the screen. Protocol 4 supplies distinct cost/Potential/result/rejection fields
plus full paged details and bounded choices. Server tokens guard both direct
choices and commits; the client neither parses preview prose nor calculates
rolls. Results explicitly describe random ranges, consumed enchantments, no
removal refunds and partial salvage batches. Only accepted transactions receive
one owner sound and a menu receipt. Native storage, slots and removal persist.

Equipment tooltips separate rarity, remaining Potential, affix tier and readable
values. Respec instructions and Crafting guidance describe existing behavior;
class discovery continues through the Phase 4 radial/Stats/Skills path. Earned
character/profession levels and first-Wither level awards emit a short owner
overlay and sound at the actual award boundary. Multi-level awards aggregate;
ordinary XP, view sync, admin snapshots and capped awards remain quiet.

Gathering review retained native destruction, placement, loot and tool-break
cues. Replanting now reports one aggregate missing-inventory-supplies message
for the actual harvested crops; sneaking reports one, placement denial is not
mislabeled, and failed harvests cannot imply a completed replant. No extra
per-block spectacle, new recipes or progression rules were added.

Phase 5 client review: station appearance on pale/dark terrain; inventory-size
respec/family/rarity recognition; resource reload and unknown datapack art;
small windows/GUI scales, paged choices/details and keyboard focus; carried-item,
shift-click and shared-station use; stale/rejected actions and partial salvage;
quiet level-up/audio balance, reconnect/dimension/reload silence and replant
shortage messages. Headless previews are ignored `.local/phase5/` and
`.local/blockbench/phase5-workbench.png`; editable sources/brief are ignored
`models/ritual_workbench/`. See `docs/VALIDATION.md` for automated evidence.
Technical delivery does not establish Minecraft appearance or art acceptance.

Phase 5 automated validation: `scripts/Dev.ps1 verify` passed **All 181 required
tests passed :)** and **BUILD SUCCESSFUL**. The final choice-label/resource build
and 43-resource packaged asset audit also passed. Three new native server tests
cover Workbench choice/commit feedback, earned-level cues and aggregate replant
shortages; existing storage/removal, harvesting and progression coverage remains
passing. See `docs/VALIDATION.md` for precise evidence and manual limits.

Phase 6's movement reference batch now uses emerald open chevrons and low flecks
for Disengage, plus violet broken rectangular brackets for Teleport. Native
glitter sprites supply short breakup; current skill icons retain their separate
review status. Departures follow accepted uses. Disengage's trail follows each
actual collision-resolved segment, with one arrival at natural/terrain completion
and no arrival on interruption or lifecycle cleanup. Turning does not rotate the
captured retreat heading. Teleport shows confirmed departure/arrival only, with
no invented travel delay or straight line across its potentially clipped path.

Seven native particle definitions use existing Minecraft sprites; small motes
expire in four/six ticks, honor particle settings and collide during breakup.
Two Teleport packets create at most 24 motes. An unobstructed Disengage creates
28, bounded to 64 under the movement solver's segment cap. No new custom
payload, saved data, sound, cost, range, cooldown or movement rule is introduced.
Sources/brief are in ignored `models/movement_effects/`; evidence is in
`docs/VALIDATION.md` and ignored `.local/phase6/`.

Phase 6 movement client review: first/third-person aim clarity, turning during
Disengage, walls and low ceilings, airborne/vertical Teleport, root/knockback and
lifecycle interruption, pale/dark terrain, nearby observers, overlap, particle
settings and resource reload. Later signature and aura batches are recorded
below. No rendered or comparative performance acceptance
is claimed for this reference batch.

Phase 6 movement automated validation: `scripts/Dev.ps1 build` and
`scripts/Dev.ps1 verify` passed **All 183 required tests passed :)**.
The two new native packet tests cover actual movement, interruption and rejected
blink boundaries; the packaged audit passes 30 definitions / 146 sprite references.

Phase 6's Reaver combo batch adds steel Lunging Combo and crimson Rip and Rush
trails on actual collision-resolved movement segments. The lunge stays movement-only;
its instant follow-up has a compact hand-level jab and its finisher reuses the
original 120-degree Cleave crescent. Rip and Rush's opening uses a thin circular
cut; its finisher closes an open steel/crimson circle inward. Neither circle is
a filled disk or a terrain-conforming guarantee of the gameplay sphere.

Small pale chips confirm accepted strike/dash/finisher damage. Opening Rend
retains its existing accepted-hit marks. Inward crimson target flecks occur only
in the qualifying bleed-consumption branch, including lethal hits with pre-hit
bleed; dry and prevented hits cannot claim consumption. Chips do not claim that
native armor-shred application succeeded. The existing stage HUD, sounds,
windows, movement, targeting, costs, effects and cooldowns remain unchanged.

Nine native particle definitions reuse glitter frames, with four-tick lifetimes:
three flecks per real segment, 24 per circular release, five per jab, four per
contact and six per qualifying consumption. No new custom payload, persisted
state or recurring stage emitter. Current skill icons are unchanged. Brief and
editable-source mapping: ignored `models/combo_effects/BRIEF.md`; validation:
`docs/VALIDATION.md` and ignored `.local/phase6/combo-*`.

Phase 6 combo client review: first/third-person aim, vertical/wall-clipped/sliding
movement, root/knockback cancellation, frontal versus circular recognition,
whiffs and defended hits, lethal consumption versus ordinary contact, crowded
enemies, pale/dark terrain, nearby observers, particle settings and resource
reload. No Minecraft appearance or comparative performance acceptance is claimed.

Phase 6 combo automated validation: `scripts/Dev.ps1 build` passed and
`scripts/Dev.ps1 verify` reported **All 185 required tests passed :)**.
Two new native activation/particle tests cover stages, hit/whiff distinctions,
trail termination and qualifying bleed consumption. Packaged validation passes
39 particle definitions / 218 sprite references. See `docs/VALIDATION.md`.

Phase 6's Reaver signature batch gives Battle Cry one low shout pulse at the
configured radius plus brief receipt marks on the existing actual recipient
snapshot. It remains a shout, not a maintained field: late entrants, rejected
uses, display refresh and lifecycle cleanup never replay activation.

Rage Storm now has short opposing hand motes during its real cast and a hollow
rotating steel/crimson cyclone after paid release. Five broken latitude bands
stay within the actual spherical radius and leave targets visible. A single
native synced radius captures release tuning; tracked movement/removal owns the
effect. Both frustum and distance culling account for the visual volume. The
80-quad depth-tested shell survives Minimal, with six optional local motes every
other tick. Accepted damage gets a compact contact cue; natural/terrain stop
gets one breakup at the last valid center. Cancelled casts and lifecycle removal
do not claim natural completion. Existing pulse timing, cover and damage stay.

Blood Fever retains its quiet owner-only count, now explicitly labeled as
bleeding enemies with a crimson accent. Refreshes, extra stacks and count
changes create no world particles or sound. Existing icons, resources, cooldowns,
native sounds and saved IDs remain. Sources/brief: ignored
`models/reaver_signatures/BRIEF.md`; evidence: `docs/VALIDATION.md` and ignored
`.local/phase6/reaver-*`.

Phase 6 Reaver client review: shout recipients and observers, cast cancellation,
near/vertical/steep storm motion, hollow volume and targets, walls, endpoint and
lifecycle removal, Minimal, distant/frustum culling, pale/dark terrain, multiple
storms, Blood Fever readability, F1 and resource reload. Minecraft appearance,
latency/multiplayer and comparative performance remain unverified.

Phase 6 Reaver automated validation: `scripts/Dev.ps1 build` passed; final
`scripts/Dev.ps1 verify` reported **All 188 required tests passed :)**.
Three new native feedback tests and the production spherical-geometry check
passed. Packaged validation covers 42 particle definitions / 242 sprite references
and the Blood Fever label. See `docs/VALIDATION.md` for evidence and manual limits.

Phase 6's Weaver signature batch replaces Discharge's generic enchant cloud
with short elemental spokes from the actual consumed charges. Partial banks
have lighter bursts; full banks add density, with ice splinters. Wind-up motes
show held elements only. Main-target contacts aggregate accepted elemental/rider
hits into one mark, while lightning chains keep their actual external endpoints
and accepted-hit marks. No absent element, cancelled/unpaid release or protected
damage is invented. Existing target selection, damage, riders and costs stay.

Threefold uses actual consumed reserve fractions for inward colored sectors.
Banking/refresh stays quiet; the existing HUD retains bonus/duration information.
One small natural-expiry breakup remembers the consumed elements using an
unsaved receipt on the existing owner refresh. Unassignment and lifecycle
cancellation discard it silently. No new timer, world scan or custom payload.
Current icons and native short-lived energy/shards are reused.

Budgets: three wind-up motes every four owner ticks; at most 91 Discharge
children across all-full banks, 24 Threefold activation and nine expiry motes.
Native particle limiting applies; Minimal may omit decoration. These are code
limits, not measured performance gains. Source/brief:
ignored `models/weaver_signatures/BRIEF.md`; evidence: `docs/VALIDATION.md` and
ignored `.local/phase6/weaver-*`.

Phase 6 Weaver client review: first/third-person aim, partial/mixed/full banks,
actual chains, cover/protection, Threefold consumption/expiry versus cancellation,
Minimal, pale/dark terrain, overlapping casts, observers and resource reload.
Minecraft appearance, latency/multiplayer and performance remain unverified.
Aura activation polish is recorded below.

Phase 6 Weaver automated validation: `scripts/Dev.ps1 build` passed; final
`scripts/Dev.ps1 verify` reported **All 190 required tests passed :)**. Two new
native feedback tests passed; existing aura ownership and lifecycle-route
fixtures now wait for entity-ticking chunks. Packaged audit passed 47 particle
definitions / 282 sprite references. See `docs/VALIDATION.md` for run evidence
and manual limits.

Phase 6's final aura batch replaces generic receipt clouds across all eleven
auras with compact, family-colored shapes. Paid casts get a small local stamp;
actual recipients get green strokes, crimson hooks/wisps, steel brackets,
pale-blue collars/energy, electric-blue sparks or violet marks. Self-active
Shock/Immolation do not imply activation on their passive enemies. Freeze
immunity and actual teammate/pet snapshots remain authoritative.

Only the final overlapping active grant emits natural expiry. Passive refresh,
passive expiry, unassignment and lifecycle cleanup stay quiet. Guaranteed Dodge
consumption emits once after all guarantees are removed. Fortitude distinguishes
positive absorption from depletion; recasts replace capacity without a break,
and depleted shields do not later replay expiry. Icons, sounds, owner-only aura
footprint, native gameplay, durations and costs remain unchanged.

Budgets per event: 12 release, eight receipt/consumption, four partial-shield or
expiry children, using existing two-to-seven-tick providers and native limiting.
Shapes follow real entity size within fixed bounds. No new persistent visual
state, world scan, tick handler or custom payload. Source/brief: ignored
`models/aura_activations/BRIEF.md`; evidence: `docs/VALIDATION.md` and ignored
`.local/phase6/aura-*`. These are code limits, not measured performance gains.

Phase 6 aura client review: first/third-person aim, all eleven identities, pet
scale, allies/observers, overlapping sources, shield absorption/depletion versus
replacement, natural expiry versus cleanup, Minimal, terrain contrast and
resource reload. Minecraft rendering, real multiplayer/latency and performance
acceptance remain pending. Phase 7 Hunter's Pet is next in this sequence; audit
its existing gameplay/model/animations before treating older planning text as
unimplemented work.

Phase 6 aura automated validation: `scripts/Dev.ps1 build` passed; final
`scripts/Dev.ps1 verify` reported **All 193 required tests passed :)**. Three new
native feedback tests cover all eleven activations, overlap and defense receipts.
Packaged audit passed 52 particle definitions / 322 sprite references. See
`docs/VALIDATION.md` for evidence and remaining manual checks.

Keep this roadmap as the production-order source; [[Art Direction]] owns the
agreed style and open aesthetic decisions.

Related: [[Development Roadmap]], [[Abilities]], [[Progression Vertical Slice]],
[development workflow](../docs/DEVELOPMENT.md),
[itemization contract](../docs/ITEMIZATION.md),
[profession contract](../docs/PROFESSIONS.md).
