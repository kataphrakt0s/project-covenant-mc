# Stalker traps: implementation plan

Status: all four traps implemented (2026-11-05); client presentation/gamefeel
and live multiplayer lifecycle checks remain for user playtesting. The ordered
acceptance guide below is retained; test evidence belongs in `docs/VALIDATION.md`.
The full behavior contract is in [RPG_SKILLS.md](RPG_SKILLS.md#trap-contract)
and [Skill Trees Brainstorm](<../Project-Covenant/Skill Trees Brainstorm.md#Trap rules>).
Those documents own the agreed rules; this checklist organizes their implementation.

Implemented tuning: release 0.1 blocks forward / 0.65 blocks above feet, initial
velocity 0.3625 blocks/tick forward and 0.14 upward, gravity 0.06 blocks/tick².
Forward movement stops after eight ticks (three blocks including release), while
falling continues up to a 200-tick flight safety limit. Native collision can shorten
the toss. The entity is 0.3 × 0.15 blocks; contact is a separate 1.5 × 0.25 × 1.5 region.
Frost/Blast use a 3.5-block feet-position radius; Flame uses three blocks.
Collision rays run 0.1 blocks above the source/target feet. Blast's custom impulse
is at most 0.65 outward / 0.22
upward blocks/tick, scaled by distance and both native knockback resistances.
Prototype visuals use iron trapdoor / blue ice / magma / TNT item models, colored
contact rings, a white armed sparkle, and cover-clipped patch rings.

## Base tuning

| Trap | Focus | Cooldown | Damage before defenses | Control / area | Effect duration |
|---|---:|---:|---|---|---:|
| Bear | 20 | 8 s | 4 on the triggering target | Root; walking/jumping blocked, attacks/casting allowed | 3 s |
| Frost | 30 | 12 s | None | 3.5-block radius; 50% movement slow inside | 8 s |
| Flame | 35 | 12 s | 3 per enemy immediately on entry, then at most once per second | 3-block radius; no lingering burn | 6 s |
| Blast | 40 | 15 s | Up to 10 at center, falling toward zero at edge | 3.5-block radius; low outward knockback with distance falloff | One explosion |

All throws are instant skills with an approximately three-block forward toss
using horizontal facing, terrain collision, and a fixed 0.5-second arming delay
after landing. Untriggered traps expire 30 seconds after arming. Cost/cooldown
use existing multipliers; root and patch durations snapshot `skill_duration`
on throw. Damage uses each trap's base value, independent of held equipment.

## 1. Shared deployment and lifecycle

Start with the smallest native entity-based deployment, shared by these four
named skills. Reuse the existing command/keybinding activation boundary rather
than adding a new input packet or trap inventory. Check pinned 26.1.2 sources
before selecting collision, entity removal, rendering, or effect APIs.

Relevant extension points are `SkillDefinition`, `Skills`, `SkillRuntime`,
`SkillLoadouts`, `PlayerProfiles`, `SkillVisuals`, and client-only renderer
registration in `CovenantClient`. The four stable `SkillTree` IDs already exist;
preserve them and their prerequisites. Keep incomplete skills as placeholders
until their respective behavior is ready.

- Represent flight, landed arming, armed, triggered, and removed states explicitly.
  Never trigger or damage a creature during flight or the arming delay.
- Use local entity/collision queries for the 1.5-block-wide shallow trigger
  footprint. Reuse `Skills.canHit` for eligibility; use the same exclusions for
  affected targets. Do not scan all players, entities, or chunks globally.
- Replace the owner's previous untriggered trap of the same type only on an
  accepted new throw. Include flight/arming in bookkeeping so obsolete pending
  deployments cannot later become extra armed traps. Different types coexist.
- Keep active Frost/Flame patch ownership separate from the next untriggered
  trap. A replacement throw does not end a patch; a replacement trigger does.
- Require dry, solid support after landing, including collision surfaces on
  slabs/stairs. Walls stop forward travel; ledges allow falling before landing.
  Fluids and lost support remove deployments as specified in the contract.
- Keep deployment state transient: no chunk forcing or restoration after chunk
  unload. Remove it on owner death, logout, dimension/class change, or unlearning.
  Cover removal paths and stale ownership entries without loading chunks.
- Apply unlearning cleanup to all actually removed skills, including Frost when
  Bear is deallocated and Blast when Flame is deallocated. Unbinding alone is
  not unlearning. Do not erase existing cooldowns during cleanup.
- Spend Focus and start the cooldown once per successful throw, with rejected
  activations leaving state unchanged. Later expiry/fizzle/replacement has no refund.
- Add visible type/readiness cues for all tracking players, with client rendering
  isolated from dedicated-server code. Traps have no health, disarming, pickup,
  ammunition, or held-item requirement.

Acceptance: ground, wall, ledge, slab/stair, fluid, and lost-support cases;
no early triggers; enemy already present at arming triggers; 10-tick arming and
600-tick armed expiry; targeting/PvP/pet exclusions; accepted versus rejected
spending/replacement; owner/dependent-skill cleanup; no restored traps after
chunk unload or restart. Keep player profiles, Affix Pages, and existing saved
resource/loadout formats unchanged.

## 2. Bear Trap and the root it needs

Add Bear's definition and single-target snap. Register a shield-bypassing
physical damage source that retains owner credit, respects armor/mitigation,
and participates in Dodge. The current Dodge hook only recognizes direct living
attackers/projectiles, so a deployed trap needs explicit, narrow support.

Use a target-owned root with native effect/persistence facilities where suitable.
Block self-directed walking and jumping while preserving turning, attacks,
casting, gravity, and external knockback. Do not implement root by freezing all
velocity. There is no blanket boss immunity or damage-break rule.

Apply root only after positive health or absorption damage. Repeat roots keep
the longer of the remaining duration and the new duration. The applied root
finishes independently of owner cleanup, saves with its target, pauses when
the target is not ticking, and clears on target death.

Acceptance: exactly one snap/target; normal and absorption hits root; Dodge and
invulnerability prevent damage/root; shields do not; armor mitigates; movement
and jump blocked while attacking/casting and external movement remain possible;
damage does not break root; refresh never adds time or shortens it; duration
snapshot and target save/load/death behavior. Include an eligible boss fixture
without introducing creature-type exemptions.

## 3. Frost Trap and source-aware area slowing

Add Frost's definition and ground patch using shared deployment. Respect solid
walls/floors. Track membership locally and apply only the strongest overlapping
Frost slow through native transient attribute state. Keep source bookkeeping
bounded to active patches/affected targets, without a general aura framework.

Leaving the last eligible patch, patch expiry/replacement, or patch cleanup
removes its slow immediately. Leaving one of several overlapping patches must
not remove another's slow. Handle target removal and dimension changes without
leaving stale modifiers. No damage, block changes, or lingering slow.

Acceptance: 50% slow only inside eligible uncovered area; multiple owners still
produce 50%; removing one source preserves another; final exit/removal restores
normal movement; separate armed/active limits; duration snapshot, owner cleanup,
walls/floors, and save/reload leave no stale slow. Recheck Bear root interaction.

## 4. Flame Trap and periodic area damage

Reuse the patch lifecycle and cover checks. Keep per-target next-hit deadlines
for each patch, including across exit/re-entry. An eligible target gets an
immediate first hit, then no more than one hit per 20 ticks from that patch.
There is no damage outside the area, lingering ignition, or terrain modification.

Use attributed fire damage that bypasses shields and Dodge, respects fire
immunity/Fire Resistance and general mitigation/invulnerability, and lets
different owners' patches deliver their due damage independently. Check native
hurt-immunity handling against the existing narrow bleed implementation; preserve
true invulnerability and avoid unrelated damage suppressing scheduled patch hits.

Acceptance: first-entry hit and one-second interval; re-entry cannot accelerate
damage; continuously present targets receive six hits (18 base damage) across
the base six-second lifetime, with no expiry-time extra hit; two owners each
deliver their own hits/credit; fire protections work; shields/Dodge do not;
cover, membership, replacement, and all cleanup paths remain correct.

## 5. Blast Trap

Add Blast's definition and one attributed explosion on trigger. Limit damage
and displacement to eligible living targets. Use native explosion damage and
protection handling where possible, without inheriting block destruction or
shield blocking. Respect solid cover, armor, Blast Protection, general
mitigation/invulnerability, and native knockback resistance; bypass Dodge.

Use center-to-edge falloff for damage and knockback. Tune a low outward shove
with a small lift; roughly three blocks of near-center travel on flat ground is
the manual playtest target, not a guaranteed physics distance. Blast can displace
a Bear-rooted target without ending its remaining root.

Acceptance: exactly one explosion; center/edge/outside and solid-cover cases;
eligible targets and owner credit; defenses and resistance; no block destruction;
no shield/Dodge prevention; rooted targets can be pushed but still cannot walk;
Frost/Flame coexistence and displacement out of patches. All four traps must work
through commands and saved skill assignments.

## Validation and handoff

Add focused dedicated GameTests alongside the existing skill/status fixtures for
each stage's observable behavior. After each playable trap, run
`scripts/Dev.ps1 build` and `scripts/Dev.ps1 test`, inspect failures, and run
`git diff --check`. Exercise dedicated-server loading, real native entity/player
serialization for saved roots, and multiplayer ownership using suitable fixtures.
Do not describe a same-process serialization check as a full process restart.

Leave exact flight arc, shallow-volume height, visual presentation, and knockback
impulse as bounded implementation/playtest tuning within the agreed behavior.
They must not turn ground-contact traps into proximity mines, let effects cross
solid cover, or freeze external movement. Record concrete values when implemented.

Hand the user a short client checklist covering throw distance/obstacles, distinct
visible traps and ready cues, real movement/aiming while rooted, patch boundaries,
and Blast's shove. Rendered behavior, gamefeel, and real-client multiplayer need
direct playtesting. Do not launch or automate the client without a request.
Update `CONTEXT.md`, the implementation contract, brainstorm status, and
`docs/VALIDATION.md` as behavior actually becomes implemented and verified.
