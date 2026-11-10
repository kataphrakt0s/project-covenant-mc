---
project: Project Covenant
type: design-document
status: direction-agreed-asset-proposals-open
updated: 2026-11-10
---

# Art direction

## Agreed direction

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
them. This family direction is agreed. The specific models, materials, motifs,
effect treatments and production order below remain proposals for review, not
approved asset designs or implemented features.

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
| Traps | Iron trapdoor, blue ice, magma and TNT block models, scaled into shallow grounded objects with flight tumble. | Replace placeholders with four related, recognizable devices. |
| AoE and auras | Particle outlines communicate contact squares, patch circles and one owner-only aura range. | Establish more stable boundaries and deliberate magical markings. |
| Projectiles | Arcane/Fire/Ice bolts use thrown vanilla item models; lightning uses sampled electric sparks. | Original cores, angular lightning, authored trails and impact frames. |
| Original assets | The packaged logo and local original wolf demonstrate the pixel/model direction. No custom combat texture/particle family is packaged under Covenant assets yet. | Produce one complete playable example before expanding the family. |
| Summons | Hunter's Pet and Elemental Golem remain planned skills. The wolf has geometry, UVs and texture, but no animation clips or runtime integration. | Plan assets now; companion gameplay and entity integration are separate implementation work. |

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
ivory/gold lightning. Arcane currently uses blue-white effects; lavender/violet
seals around a pale core are a proposal to review, not an agreed recolor.
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

**Hunter's Pet:** reuse the existing natural gray/cream wolf with amber eyes.
Its model has 20 cuboids, eight bones and a 128x64 atlas at one texel per model
unit. At nominal scale it is about 1.375 blocks to the ears; choose final scale
in the actual consumer. Preserve the animal's readable silhouette. A small
collar or oath-token is optional, not a required redesign. Summoning can supply
the occult moment through a brief ground seal and gathering motes.

Plan idle/head tracking, locomotion, attack, hurt/death and summon/dismiss
presentation against the eventual gameplay states. There are currently no clips.
The existing leg bones support a simple rigid gait; an articulated bite needs a
separate jaw pivot. Poses require joint-clearance review before animation approval.
See the local `models/original_wolf/BRIEF.md` for source and preview details.

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

The existing design selects form from the player's last cast and gives each form
a nearby benefit. Exactly when form changes are evaluated remains gameplay work;
visual planning must not decide it implicitly. Both summons follow the lasting
companion rules in [[Skill Trees Brainstorm]]. Final size, combat reach, benefit
range and movement behavior must inform the asset brief before production.

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

Recommended production sequence, not yet commissioned:

1. **Frost Trap complete slice:** model, texture, arming cue, trigger burst,
   persistent patch boundary and expiry. This proves physical art and occult AoE
   language in one already playable skill.
2. Extend the successful boundary treatment to the other traps and auras while
   preserving each shape, lifetime and visibility rule.
3. Give Arcane Bolt, Cleave and Piercing Shot representative original effects;
   then apply their family treatments to broader elemental and advanced skills.
4. Integrate/animate the existing wolf alongside the separate companion gameplay
   slice. Use it to establish the first custom entity pipeline.
5. Block out one golem body before producing four form variants.

Review the first slice at normal gameplay scale on grass, pale terrain and dark
stone, in daylight and caves, on steps/ledges, with overlapping effects and reduced
particles. Check geometry and lifetime automatically; judge silhouette, spectacle,
occlusion and readability in the client. This planning pass changes no runtime
assets and establishes no new rendered or performance validation.

## Next decisions

Review the Frost Trap concept first: **low iron frame with a bound crystal** is
the recommended expression of the agreed crafted-device direction. Then settle
the AoE ornament density and particle-setting behavior through that example.
Arcane's possible violet shift and the golem's stone-guardian concept remain open.

Related: [[Project Covenant - GDD]], [[Abilities]], [[Classes and Specializations]],
[[Skill Trees Brainstorm]], [combat feedback contract](../docs/RPG_SKILLS.md#combat-feedback-pass).
