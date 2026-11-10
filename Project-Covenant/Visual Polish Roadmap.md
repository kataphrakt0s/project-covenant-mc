---
project: Project Covenant
type: implementation-plan
status: phases-1-2-implemented-client-review-pending
updated: 2026-11-10
---

# Visual polish roadmap

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

This roadmap sets the production order. Phases 1 and 2 are implemented with client review
pending; current delivery and validation boundaries are tracked below.
Phase numbers give the recommended delivery order, not a requirement to finish
every earlier asset before starting an independent UI or item-art task.

## Source-backed starting point

Reviewed current source and local asset documentation on 2026-11-10. These are
implementation observations and proposed improvements, not findings from a new
rendered usability test.

| Surface | Current foundation | Main opportunity |
| --- | --- | --- |
| Traps and AoE | Four original trap devices; supported depth-tested contact/patch outlines; one owner-only aura footprint. | Review silhouette, terrain/overlap readability and renderer cost in Minecraft. |
| Combat effects | Native particles, poses and event sounds; thrown-item bolts; sampled lightning; particle-only Rage Storm. | Authored projectile, impact, movement and major-skill identities. |
| Skills and bindings | Whole-class tree with initials, rank/tooltips, pending Apply and eight slots. | Shared original icons and clearer allocation/readiness states. |
| Professions | Four tabs, named nodes, rank text and pending markers. | Consistent icons, rank/cost hierarchy and planned/locked distinction. |
| Stats and radial | Text-heavy stats with raw modifier IDs; three active radial destinations. | Clear summaries, class discovery and recognizable navigation. |
| HUD | Current-class resource bar, thin Covenant XP bar, top-left statuses with vanilla icons. | Recognizable status art and legible counts/timers with the existing footprint. |
| Workbench and inventory | Netherite-block station placeholder; cycle selectors and mostly textual previews; vanilla respec/component imagery. | Recognizable station, understandable transactions and distinct item identities. |
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
must not suggest extra targets. Choose whether Arcane gains violet accents during
its example; blue-white remains the current behavior until that choice is made.

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
   proposed worn timber/iron, stone work plate and restrained old binding marks.
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

Current tracking: **Phases 1 and 2 implemented; manual Minecraft review pending**.
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
Other phases remain pending.
Final Phase 2 automated validation: `scripts/Dev.ps1 build` and
`scripts/Dev.ps1 verify` passed, including **All 173 required tests passed :)**
and **BUILD SUCCESSFUL**. The packaged source/export/UV/PNG audit also passed;
see `docs/VALIDATION.md` for evidence and manual boundaries.
Keep this roadmap as the production-order source; [[Art Direction]] owns the
agreed style and open aesthetic decisions.

Related: [[Development Roadmap]], [[Abilities]], [[Progression Vertical Slice]],
[development workflow](../docs/DEVELOPMENT.md),
[itemization contract](../docs/ITEMIZATION.md),
[profession contract](../docs/PROFESSIONS.md).
