---
project: Project Covenant
type: design-document
status: direction-reset-reference-review-open
updated: 2026-11-10
---

# Art direction

## Approved promotional-art benchmark — 2026-11-10

The user approved the finished hybrid three-class banner as the benchmark for
promotional art. See [the canonical image and visual rules](../docs/promotional-art/README.md).
Match its Minecraft-adjacent voxel forms, restrained neutral lighting on characters,
soft floor light/contact shadows, quiet near-black background, silver/lavender gothic
border and serif lettering. No overhead/background halos. Limited imagegen refinement
is acceptable at this restrained level; earlier cinematic treatments are superseded.
This approval is specific to promotional art and does not approve runtime visuals.

## Current correction — 2026-11-10

The user finds the overall art too desaturated and realistic, with too little
bold dark fantasy flavour. **The wolf and traps are accepted anchors; all other
art is subject to eventual redesign.** Pursue stronger colour identity, purposeful
pixel clusters and expressive fantasy silhouettes while retaining Minecraft scale.
Earlier calls for subtle flavour, stone/lavender dominance and restrained accents
must not constrain this correction. Preserve gameplay readability and semantics.

[[Art Reference Studies]] tracks the new comparison sources and open decisions.
The first Dungeons/Faithless and Dokucraft comparison did not settle a direction;
the user requested broader exploration. Subsequent FurfSky feedback approves
its **strong silhouettes** and favours its darker **black, purple, red and grey**
examples. Much of its flashy, neo-fantasy styling is unsuitable for Covenant's
dark gothic identity; use those accepted qualities selectively. Exact swatches,
ornament and per-family treatments remain open. Maintain rich colour and readable
forms rather than returning to washed-out realism or indistinct near-black art.
The sections below retain earlier design history and technical contracts; their
aesthetic proposals are subordinate to this correction and reference feedback.

### Bolt correction: pure energy

On 2026-11-10 the user rejected the floating-item spell presentation. Elemental
bolts must be primarily particles: flaming, icy or electrified energy. Simple
geometric supports/particle attractors are acceptable; recognizable item cores
are not the intended world effect. Fire remains crimson, ice pale blue and
lightning electric blue. Runtime now uses flowing particle bodies and thin
geometric filaments for Fire/Ice/Arcane, plus flickering particle lightning.
This supersedes earlier item-core guidance, without changing the approved icon
direction. Final Minecraft appearance awaits user review.

### Icon correction: vanilla recognition first

The first gothic rework made icon silhouettes too distinctive. Review icons
**individually**, starting from what each must communicate to the player and the
closest vanilla Minecraft item or status symbol. Keep the vanilla object's
proportions, orientation and recognizable silhouette; use colour and small
interior details for Covenant's gothic identity. Do not invent an exotic weapon
or relic shape simply to make an icon unique.

For skills, distinguish the action with one readable secondary cue where needed
(for example, a sweep beside a familiar sword). Judge at native 16px first.
Shared meanings may reuse familiar shapes. Descriptive clarity takes priority
over silhouette variety. The crimson/pale-blue/electric-blue elemental palette
remains agreed. The workbench is acceptable for now and needs no further pass.

Individual review: **Cleave** uses the approved vanilla-shaped iron sword plus
one broad sweep that widens in an arc toward the blade, with a small crimson
grip accent. **The user approved the finished 16px icon**, including the widened
arc, on 2026-11-10. Use this as the reference for familiar weapon proportions;
other icons still need individual review. This is art approval, not a client test.
[Cleave comparison](../.local/icon-review/cleave.html).

**Rend**: sword-and-blood-drop direction approved. The user explicitly requests
a very clear, simple classic water-droplet shape. Implemented as a separate
crimson teardrop with a centered pointed top and rounded base, beside the same
familiar sword proportions. **Finished icon approved on 2026-11-10**, including
the stain widened across the lower blade face without increasing its length.
[Rend comparison](../.local/icon-review/rend.html). This is art approval;
in-game testing remains separate.

**Lunging Combo**: the sword-and-streaks implementation did not convey lunging
clearly enough. The user suggested a character silhouette instead. The new
study uses a square-headed blocky player leaning into a sword thrust, with a
bent leading knee and extended rear leg. **The user accepted this pose** and
requested red for the character as part of Reaver's identity. Reaver crimson
is now applied to the character; the sword remains steel-coloured. Pose conveys
forward movement; tooltip/stage labels describe the full combo. **The finished
crimson character icon is approved on 2026-11-10.**
[Lunging Combo comparison](../.local/icon-review/lunging-combo.html).

**Rip and Rush**: the user approved a crimson blocky character turning through
a broad circular sword sweep. The 16px redraw uses a planted stance and an arc
passing behind the body and across the foreground, emphasizing the circular
attack. The user found the arms overcomplicated for 16px: simplify to one short,
straight visible sword arm and omit the rear arm. Let the sweep convey the
movement. **The finished simplified icon is approved on 2026-11-10.**
[Rip and Rush comparison](../.local/icon-review/rip-and-rush.html).

**Battle Cry — reference reset**: the user rejected the previous face and
requested a fresh start using their supplied shouting manga and red warcry
images. The new study uses a tilted, close-cropped shouting face, dark hair,
shadowed brow/jaw, pale facial planes and crimson background. The generated
source was reduced to a native 16×16 eight-colour base grid, with mouth/teeth clusters
clarified in the editable source. The previous headband/flat-face design
is discarded. The user likes this reference-led direction and requests a zoom
so the mouth and jaw fill the frame. The current icon crops directly from the
larger study before 16px reduction, preserving the mouth/teeth detail; eyes and
hair are outside the crop. The user accepted this crop and requested the small
corner sword restored; Battle Cry now includes it. The clean crop is preserved
pixel-identically as `models/gothic_rework/cry_face_base.png`, with its editable
`cry_face_icon()` source and the larger study/crop beside it. **The finished
Battle Cry icon, including the restored sword, is approved on 2026-11-10.**
[Battle Cry comparison](../.local/icon-review/battle-cry.html).

**Future Enduring Cry** should share the shouting-face layout with a heart or
shield corner badge when implemented, using the preserved clean base above.
The choice between those two badges
remains open. This is a visual direction only, not a gameplay or asset delivery.

**Rage Storm**: the user requested a cyclone image while keeping the existing
colours. The redraw has a broad rotating crown and tapering funnel, using the
exact prior steel-grey, crimson, red highlight and dark outline palette.
**The finished cyclone icon is approved on 2026-11-10.**
[Rage Storm comparison](../.local/icon-review/rage-storm.html).

### Remaining catalogue pass — 2026-11-10

The user authorized continuing the remaining catalogue in small batches using
the approved examples, asking for direction where a result is unclear. This
supersedes the requirement to obtain a separate concept decision before every
ordinary icon; finished artwork still requires visual review.

After review corrections, the pass revises **60 existing textures**: 22 skill icons,
22 menu/profession emblems, three status icons and 13 item textures. The previous
Ice Bolt and Arcane Bolt, including their matching item cores, are restored exactly.
The six approved skill icons, accepted Movement Speed component, wolf,
traps, workbench, rarity pips and all other resources remain byte-identical.
[Complete native-size comparison](../.local/catalogue-review/index.html).

Key descriptive treatments:

- Blood Fever: classic blood drop with speed streaks. Ripping Aura shares the
  drop with the existing aura marker.
- Disengage: a green blocky character and a leftward retreat arrow. Bows/arrows,
  feathers, axes, picks, hoes, books and ingots use familiar vanilla proportions.
- **Icy Veins:** the user selected a **pale-blue hourglass**, communicating
  cooldown/cast-time benefits. This is direction approval, not final-icon approval.
- **Movement speed:** the user prefers the previous single-boot silhouette.
  Preserve the Movement Speed component exactly and reuse that boot in Swiftness
  Aura. Canonical source: `models/gothic_rework/movement_boot.png`.
- Fire remains crimson, ice pale blue and lightning electric blue. Threefold
  contains visibly different fire/ice/lightning shapes; Discharge releases them
  outward. Flame Cloak uses a broad cloth cloak with visible folds and crimson
  flames along its hem; avoid a central droplet-shaped flame.
- Teleport: a plain rectangular obsidian portal. Unknown content uses a simple
  question mark. Statistics uses bars; professions use recognizable tools.
- Affix families: heart, sword, shield, feather, accepted boot, resource vial,
  hourglass for cooldown, clock for duration, a gathering arc/spell spark for
  cast wind-up, and crystal; their existing separate rarity pips retain reserved
  rows 13–15. Dodge uses a curved feather with an exposed quill; Attack Speed
  uses a clear upright sword with motion streaks. Combat Respec uses a dark ink
  sword glyph on parchment. Rooted uses an independent circular bear trap with
  inward teeth and a central pressure plate; it need not match the model or skill icon.

Authoritative exports: `models/gothic_rework/catalogue.py`, integrated into
`icons.py` and `materials.py`. The source rebuild and package audits pass;
review sheets are not Minecraft rendering. The 60 revised textures remain
**review drafts**, not automatically approved.

### Elemental palette and working-tree rework

The user selected **occult crimson fire**, **pale blue ice**, and **electric blue
lightning that pops as elemental bolts**. These supersede the earlier orange-fire
and ivory/gold-lightning proposals. Ice uses fractured shards; lightning uses
sharp forks and a bright white-blue core with a saturated blue edge. Arcane keeps
its separate violet identity. Keep native burning and ordinary vanilla assets
intact; Covenant's own fire cues use crimson.

The working-tree rework now covers 129 runtime textures: 71 skill/menu/status/item
textures, all 57 existing combat frames, and the workbench atlas. The workbench
is an eight-cuboid altar with recessed pointed niches, iron buttresses and a
crimson/violet seal, within the existing one-block footprint. UI panels share
dark iron, crimson inlay and violet selection accents. Affix families are distinct
relic objects, retaining separate rarity pips and existing component selectors.
Wolf and trap device assets remain unchanged; Flame Trap's elemental feedback
uses the new crimson family. No gameplay dimensions, timings or saved IDs change.

Editable native pixel sources live in ignored `models/gothic_rework/`, with the
existing effect sources in `models/{combat_examples,elemental_spells,bow_melee_effects}/`.
The altar source remains `models/ritual_workbench/workbench.bbmodel`.
[Before/after review](../.local/gothic-rework/review.html) includes native-size
light/dark icon samples, effect frames and headless model views. This is an authored
implementation for client review, not final visual acceptance. Branding and
separate promotional scenes are outside this runtime-art pass.

## Agreed combination — construction and occult meaning

Use **forged weapons, armour and cathedral ironwork** alongside **cursed relics,
ritual objects and occult symbols**, with the balance determined by context.
The user approved this combination on 2026-11-10. Gothic construction supplies
physical form; occult details can express an object's purpose, history or power.
Neither vocabulary needs to appear equally in every asset.

Working applications for future asset briefs:

- Physical equipment and martial icons: emphasize forged silhouettes, material
  separation and purposeful construction; add occult character where relevant.
- Relics and magical icons: emphasize distinctive ritual objects and meaningful
  symbols, using the shared pixel treatment and dark colour families.
- Ritual stations and magical constructs: combine architectural or forged
  structure with an identifiable occult focal feature.
- Ordinary profession tools and natural creatures: retain recognizable function
  and anatomy; the shared art treatment provides cohesion without obligatory runes.

These applications guide reference selection, not final asset designs. Preserve
the accepted wolf and traps. Exact motifs, palettes and family samples remain
subject to review.

## Earlier agreed foundation

Covenant should primarily **accent vanilla Minecraft**, while including occult
imagery and magical spectacle in a **dark fantasy, gothic-lite** style. The
world's familiar proportions, materials and pixel language provide the foundation.
The result should feel like **Minecraft has developed an ancient, forgotten
supernatural history**, with Covenant revealing traces of that history through
objects, creatures and magic. Preserve the recognizable world and add our own
subtle flavor. Striking skill and summon effects still belong in this direction,
with spectacle grounded in Minecraft's materials, shapes and visual language.
Begin with a recognizable Minecraft object or material, then suggest an older
supernatural purpose through its construction, wear, markings or behavior.

Use the existing original wolf as the visual foundation for Hunter's Pet.
Stalker traps are **crafted hunting devices containing occult power**: practical
mechanisms and recognizable construction with supernatural elements bound into
them. This family direction is agreed. The Frost and Phase 2 trap examples are
now implemented prototypes for client review; the remaining asset-specific
materials, motifs and effects below remain proposals unless explicitly recorded
as delivered. Headless validation alone does not establish visual acceptance.

The approved packaged logo establishes clean silhouettes, deliberate pixel
clusters, charcoal forms, stone/lavender tones and selective violet accents.
Apply that treatment to each asset family; the logo's exact palette, sword/C
motif and enlarged pixel grid are not universal requirements. The local,
Git-ignored `docs/art/PIXEL_ART_GUIDELINES.md` remains the detailed production
guide when available. Preserve square native pixels, coherent material ramps,
consistent texel density and crisp authored texture edges.

## User-supplied reference collection

Before making art-direction decisions or writing an asset brief, consult the
relevant entries in the user's reference collection alongside the approved logo
and pixel-art guidance. Record which references inform the asset and which
specific qualities apply. Favor vanilla continuity when a more elaborate
reference would pull the asset away from the agreed direction.

Imported on 2026-11-10 from `covenant_gothic_lite_references.zip`. The collection
lives at **`docs/art/references/covenant_gothic_lite_references/`**, covered by the
existing `/docs/art/` Git ignore rule:

- [Reference index](../docs/art/references/covenant_gothic_lite_references/REFERENCE_INDEX.md):
  twelve source entries with links and notes.
- [Visual board](../docs/art/references/covenant_gothic_lite_references/VISUAL_BOARD.html):
  HTML board with remotely hosted previews; an internet connection is required.
- [Import notes](../docs/art/references/covenant_gothic_lite_references/IMPORT_NOTES.md):
  provenance and local availability details.

The supplied archive contains the index, board and README, with no embedded image
files, textures, models or resource packs. Source documents are preserved unchanged.
This collection is local-only and absent from a fresh checkout; the tracked
direction here remains available. If the collection is missing, state that and
use the agreed direction and packaged logo without claiming to have reviewed it.

Use the supplied index to choose relevant reference studies:

| Reference group | Intended use in Covenant |
| --- | --- |
| Ancient City and Deep Dark atmosphere | Familiar Minecraft ruins, layered dark stone, hints of forgotten activity, and small warm lights against cold supernatural accents. |
| Stay True, Jicklus and Excalibur | Continuity with vanilla proportions and pixel scale; restrained material variation, practical equipment and lived-in surroundings. |
| Gothic Chapel, Mystic Shrine, Conquest and John Smith Legacy | Selective construction details: pointed silhouettes, stone supports, ironwork, timber and wear. Adapt motifs to Covenant's density rather than adopting a whole replacement-world style. |
| Ritual Altar, Minecraft Dungeons and Dokucraft | Focused occult motifs, magical focal points and special-event spectacle. Use these more selectively for casts, summons and ancient objects. |

These roles are an interpretation of the supplied reference notes. Inspect the
actual linked image/source before using it as visual evidence for a design;
remote preview availability and attribution were not verified during import.
The board's Dokucraft card reuses the Mystic Shrine thumbnail; consult the linked
Dokucraft gallery before attributing that image's details to the pack.
Some showcases use shaders or different texture densities. Study their useful
qualities while retaining Covenant's native-rendering and pixel-density baseline.
Preserve source attribution and create original production assets.

## Current position

Source and existing asset previews inspected on 2026-11-10; this is not a new
Minecraft client playtest.

| Area | Present implementation | Opportunity |
| --- | --- | --- |
| Combat feedback | Cast/release/impact sounds, native poses, bounded particles, elemental cues and status icons already have lifecycle hooks. | Give these cues original shapes, timing and materials. |
| Traps | Original iron/crystal Frost, toothed Bear jaws/plate, vented Flame cage and bound Blast charge/solid primer. | Review the family together at gameplay scale. |
| AoE and auras | Supported depth-tested trap squares/patch circles and one broken owner-only aura footprint, including on Minimal. | Review terrain gaps, overlap readability and renderer cost in Minecraft. |
| Projectiles | Original pale/violet Arcane, ember Fire and fractured Ice cores; bounded jagged Lightning links; precise/magical/fan arrow streaks. | Review first/third-person aim visibility, real links and collision versus damage feedback. |
| Original assets | Packaged logo, original wolf and the four trap models/atlases/icons demonstrate the pixel/model direction. | Review Minecraft appearance before expanding the combat family. |
| Summons | Hunter's Pet is implemented, animated and user-accepted; Phase 7 is complete. Elemental Golem has approved model/textures, basic companion behavior, five body clips, a slow core tumble and minor particles across four forms, plus the level-25 full skill with cycling, nearby owner benefits and persistence. | Golem animations and full-skill gameplay await client review. |

Current consumers: [TrapRenderer](../src/main/java/dev/covenant/client/TrapRenderer.java),
[SkillVisuals](../src/main/java/dev/covenant/rpg/skills/SkillVisuals.java),
[AuraRangeVisuals](../src/main/java/dev/covenant/client/AuraRangeVisuals.java),
[CovenantClient](../src/main/java/dev/covenant/client/CovenantClient.java).

## Proposed visual language

Use familiar iron, stone, leather, fur, ash and crystal. Weathering should follow
construction and use: chipped stone corners, worn metal edges, broad fur clusters
and readable seams. Small details should survive actual gameplay viewing distance.
Keep physical silhouettes simple enough to belong beside vanilla objects.

Place occult character in binding bands, engraved seals, pointed recesses,
fragmented ritual circles and suspended magical cores. Let larger casts and
summons briefly assemble these motifs in motion. Ordinary mechanical attacks
can remain physical; each class should have its own expression of the direction.

| Family | Materials and shapes | Proposed magic and motion |
| --- | --- | --- |
| Stalker | Dark iron, worn leather, practical hunting mechanisms, natural animals. | Precise streaks and sharp impacts; elemental traps reveal bound magic when triggered. |
| Reaver | Worn steel, charcoal, deep crimson and pale contact highlights. | Broad weapon crescents, forceful pulses, brief bloodlike marks and a hollow storm silhouette. |
| Weaver | Old stone, iron bindings, fractured crystal, pointed recesses and geometric seals. | Ritual arcs, angular elemental forms, luminous cores and more elaborate major releases. |

Retain clear elemental meanings: orange/ember fire, pale blue/white ice,
ivory/gold lightning. The user approved a pale Arcane core with restrained
lavender/violet geometric accents during Phase 3. Arcane Bolt establishes this
family, now shared by Arcane Arrow's trail/collision/accepted-hit art. Major Arcane
effects outside Phase 3 retain their existing presentation until scheduled.
The current red Rage, blue Mana and yellow Focus HUD colors remain established
functional cues. Material palettes can vary without recoloring that interface.

Use brightness, silhouette and motion together. Frost can fracture, flame can
curl upward in stepped tongues, lightning can fork, and arcane can assemble
ordered geometric fragments. Element identity should survive a grayscale view.

## Skill effects and AoE signifiers

Proposed effect rhythm:

1. **Preparation:** a compact gathering cue only where the skill actually has a
   cast or arming period. Do not add a visual wind-up that implies a new delay.
2. **Release:** the strongest short silhouette, flash or expanding motif.
3. **Sustain:** a readable edge with sparse interior motion, leaving targets and
   terrain visible during repeated or persistent effects.
4. **End:** a brief breakup, collapse or extinguishing cue tied to the real expiry,
   consumption or cancellation event.

For AoEs, pair a thin, stable boundary with sparse ritual markings or material
fragments. Reserve the more elaborate circle assembly for activation. Frost
patches could grow a few angular crystal marks; flame patches could expose
ember fissures; magical auras could use separated seal fragments around the rim.
The boundary describes range; ornament describes identity and state.

Keep the following existing geometry contracts:

- Armed traps trigger on a **1.5 x 1.5 block square**, **0.25 blocks high**,
  after the existing arming delay. A circular decoration must not imply circular
  proximity detection. Bear remains single-target.
- Frost and Blast effect radii are **3.5 blocks**; Flame is **3 blocks**.
  These use three-dimensional target-feet distance and cover checks.
- Area auras use a **six-block spherical range**, currently shown as a horizontal
  ring for the local owner. Their range queries do not use the traps' cover test.
  Self-only auras have no range outline; shared passive ranges merge visually.
- Cleave is a **120-degree frontal arc**. Circular attacks need circular cues.
  Rage Storm is a moving three-dimensional volume.

A ground marking is a footprint cue, not a guarantee that every target above it
qualifies. The current rings are not terrain-conforming decals. A new surface
renderer needs explicit treatment of steps, slopes, walls, ledges and missing
support; it must not project through floors or imply that an effect reaches
another elevation merely because its horizontal outline does.

Recommended visibility policy for review: keep essential boundaries readable
when decorative particles are reduced, while retaining quiet owner-only passive
aura feedback. Current aura particles disappear on Minimal; preserving a boundary
there would be a deliberate setting/behavior change to decide during the prototype.
Keep trap visibility to other players; observer/ally aura visibility remains a
separate choice. Do not silently expand it through an art replacement.

## Proposed trap family

The agreed family is crafted hunting devices containing occult power. The
following individual silhouettes and effects are proposed expressions of it.

| Trap | Model concept | State feedback |
| --- | --- | --- |
| Bear | Low forged-iron jaws and a worn pressure plate; teeth provide the unmistakable silhouette. | Mechanical arming cue and a sharp snap at the real trigger. |
| Frost | Low iron frame holding a fractured cold crystal, with etched binding channels. | Small ready-state core; crystal breakup and an angular frost seal when triggered. |
| Flame | Shallow caged brazier or ember vessel with clear vents. | Contained embers while ready; a brief flare opens the burning patch. |
| Blast | Compact iron-bound powder charge with a recognizable primer/fuse shape. | Readiness cue tied to arming, then a short pressure flash and debris. A fuse must not suggest a timed detonation. |

Share metal ramps, construction details, footprint discipline and texture density;
distinguish the devices by silhouette before color. Frost/Flame cores currently
disappear when their patches begin. Preserve that handoff, with remaining visual
activity owned by the patch. Brief jaw or trigger animations need explicit render
support and must not delay damage or extend gameplay lifetime.

## Summon asset planning

**Hunter's Pet:** the existing natural gray/cream wolf with amber eyes is
implemented and animated. The user accepted it on 2026-11-10 and marked Phase 7
complete. Retain its current model, texture, animation and companion behavior.
The current animated source is local `models/original_wolf_animated/`;
`docs/GECKOLIB.md` and `docs/RPG_SKILLS.md` describe the integrated consumer.

**Elemental Golem:** proposed weathered stone guardian, with broad shoulders,
simple heavy limbs, iron bindings and a chest recess containing an elemental
core. A pointed face/recess can suggest old gothic masonry at Minecraft scale.
Use one recognizable body and rig across four forms, with small geometric
accents, distinct motion and core effects:

| Form | Proposed identifying treatment |
| --- | --- |
| Fire | Ember seams, vented core, short upward flame tongues. |
| Ice | Angular crystal plates and a cold fractured core. |
| Lightning | Forked conductors and brief connecting arcs. |
| Arcane | Ordered seal fragments around a suspended pale core; violet accent under review. |

The agreed skill design cycles the active golem's form when its skill is
activated, with a different nearby benefit per form. Other spell casts do not
select its form; removing its last slot assignment dismisses it. This implemented
control rule supersedes last-cast selection. Both summons follow the lasting
companion rules in [[Skill Trees Brainstorm]]. The user now requests model/texture
approval first, implementation second and animation third. The static golem
study uses provisional visual scale; runtime size is 1.4×2.7 blocks, attacks use native melee reach,
owner benefits reach 12 blocks and movement uses shared native companion rules. Review the first
[four-form asset study](../.local/blockbench/elemental_golem/review.html);
its materials and silhouettes were approved by the user on 2026-11-10. This is
asset approval; actual Minecraft appearance and the movement/melee test still
need client review. The full skill now supports four elemental appearances,
sharing the first body/core animation set and restrained family particles. See
the [motion review](../.local/blockbench/golem-animations/review.html). This is
animation delivery for review; user approval of the static assets does not
automatically approve playback or particle appearance.

## Feasible production path

Original cuboid traps, pixel textures, custom particle sprites, animated impact
sequences and client-rendered area markings are feasible directions for the
native renderer. NeoForge documents [custom particle sprite sets](https://docs.neoforged.net/docs/resources/client/particles/)
and [client particle rendering](https://docs.neoforged.net/docs/rendering/particles/).
Exact implementation must follow the resolved 26.1.2 sources.

The headless tools author and preview models, UVs and animation; entity exports
still need adaptation to Covenant's client render-state and animation code.
Replacing a model file alone does not change the current trap/bolt consumers.
Revisit their transforms when replacing flattened vanilla blocks. Ordinary block
JSON cannot play a skeletal timeline. Particle flipbooks need their actual
particle-provider timing; block texture animation metadata is a separate consumer.
Bright painted pixels do not themselves implement emission, bloom or world light.
The baseline should look complete with ordinary Minecraft rendering.

Keep server-owned outcomes, dimensions and lifetimes authoritative; drive cosmetic
motion locally from existing entity state or bounded events. Cap recurring effects,
reuse geometry, cull distant visuals and measure busy scenes before making
performance claims. New VFX should not require global scans or forced chunks.

The proposed production order, independently deliverable slices and completion
checks live in [[Visual Polish Roadmap]]. Start with a complete Frost Trap
example, then expand the trap/AoE and core combat families alongside GUI/UX,
Workbench/items and progression feedback. Advanced effects and companion assets
follow their actual dependencies; the wolf establishes the entity pipeline before
four golem forms. This document owns the art direction; the roadmap owns sequence.

Review the first slice at normal gameplay scale on grass, pale terrain and dark
stone, in daylight and caves, on steps/ledges, with overlapping effects and reduced
particles. Check geometry and lifetime automatically; judge silhouette, spectacle,
occlusion and readability in the client. Phase 1 runtime delivery establishes no
new Minecraft rendered or comparative performance validation.

## Next decisions

Phase 1 implements the **low iron frame with a bound fractured crystal** example.
The user approved preserving essential depth-tested boundaries on Minimal,
with gaps at unsupported/covered samples at the trap's elevation. Decoration
follows native particle settings; passive aura visibility remains owner-only.
The prototype uses sparse interior fractures, ordinary lit original iron/crystal
textures, and a matching native-size icon. Client review will settle visual
acceptance and ornament density. Phase 2 implements the Bear jaws/pressure plate,
Flame ember cage and Blast charge/solid unlit primer, retaining Frost's iron ramp
and texel density. One merged broken aura outline uses the same supported
same-elevation treatment while deliberately omitting trap cover rays. Small
status hints and actual-trigger/expiry cues stay separate from passive refresh.
Headless mapped
previews and technical validation do not establish Minecraft appearance.
Phase 3's first batch establishes the approved pale/violet Arcane Bolt, a
steel/crimson frontal Cleave crescent and precise ivory/gold Piercing Shot
streaks, with matching small icons. Distinct collision and accepted-damage marks
reuse real server outcomes. The remaining Phase 3 batches now add Fire's stepped
embers, Ice's angular core/fractures, exact-endpoint jagged Lightning and real
Shock links, Aimed gathering during the actual cast, violet Arcane Arrow, a quiet
five-arrow Multishot fan and single-target Rend/one cue per accepted bleed pulse.
Native weapon poses and sounds remain. The golem's stone-guardian concept remains
open. Minecraft visual review is pending across these families.

The user rejected Phase 4's first abstract UI icon study in favor of recognizable
forms. The implemented redraw uses fuller silhouettes of familiar equipment,
tools, a wolf head, horn, cloak, book and distinct elements, with substantial
pixel clusters and restrained material ramps. Resolved vanilla inventory art was
studied for weight and negative space; no vanilla or texture-pack pixels are
exported. All playable skills share identities across tree, bindings and status.
Native/enlarged pale/dark samples are ignored `.local/phase4/icons-v2.png`, with
sources/reference notes in `models/ui_phase4/`. The revised art and Minecraft
rendering still await user acceptance.

For Phase 5 the user chose a **stone ritual station** over the initial timber
cabinet proposal. The implemented Workbench uses a worn stone work slab, heavy
iron binding strips and restrained carved niches/marks, preserving the existing
solid one-block footprint. The original 32x32 atlas packs native-density faces;
no world light or emission is added. New 16x16 respec art uses a sword-marked
parchment and a tool ledger. Component charms use recognizable family marks and
1–5 rarity pips; unknown datapack entries retain neutral art and named tooltips.
The packaged logo and official Ancient City corridor informed broad stone
clusters/bands. The linked Mystic Shrine imagery was host-blocked; no inspection
of that image or reuse of third-party pixels is claimed. Source/reference notes
are in ignored `models/ritual_workbench/BRIEF.md`. Headless previews were inspected;
Minecraft appearance and user acceptance of these assets remain pending.

Related: [[Project Covenant - GDD]], [[Abilities]], [[Classes and Specializations]],
[[Skill Trees Brainstorm]], [combat feedback contract](../docs/RPG_SKILLS.md#combat-feedback-pass).
