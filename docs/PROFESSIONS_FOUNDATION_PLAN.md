# Professions foundation, first implementation chunk

This chunk implements the shared profession boundary before gathering abilities,
workstation itemization, or endgame content. All balance values are provisional.

## Scope

- Keep `covenant:profession_progression` and its four independent, classless tracks. Add
  immutable committed allocations with the points actually paid. XP awards,
  reloads, death copying, and respec must preserve the correct independent state.
- Author a bounded server-datapack profession catalogue: stable ability IDs,
  ordered rank gates/costs, prerequisites, and provisional balance. Store the
  agreed activity rewards for future event integrations; do not invent Crafting
  XP payouts or additional recipe milestones.
- Commit one displayed profession's draft atomically, out of combat. Validate
  expected state, catalogue revision, rank sequence, prerequisites, level,
  availability, and points on the server. Retain paid costs when tuning changes.
- Add owner-synced catalogue/progression presentation and radial slot 3's
  four-tab Professions screen. Drafts survive tab changes, Apply targets the
  displayed profession, right-click undoes pending ranks, and closing discards
  drafts. Unimplemented nodes remain visible and unavailable.
- Make Logging's Offensive Mastery the first usable branch: apply its provisional
  bonus to player-attributed damage through the native incoming-damage event.
  Owned creature damage is not credited to its player owner.
- Implement the agreed Profession Respec item and shapeless recipe. Native
  two-second held use, shared out-of-combat gate, all-four atomic refund,
  preservation of XP/levels, and consumption only on success.
- Enable the agreed provisional shared XP curve and add operator-only self
  testing commands. Natural activity XP integrations follow in another chunk.

## Persistence and reload contract

Existing track IDs and attachment IDs stay unchanged. Allocations are an optional
addition to each track's codec; existing foundation tracks decode with none.
Store rank and paid points, not effective attributes or recalculated costs.
Unknown/removed allocations retain their paid-point reservation until respec.
Reloading costs affects future ranks, never retroactively spends/refunds points.

## Validation and handoff

Cover decoding/round trips, paid-cost preservation, classless awards, caps,
atomic/stale/forged requests, prerequisites and unavailable content, native
Offensive Mastery damage, all-four respec interruption/completion, reload
behavior, and owner/death-copy registration. Run `scripts/Dev.ps1 build`,
dedicated GameTests, `git diff --check`, and JAR packaging inspection. Hand off
manual radial/screen interaction checks; do not launch or deploy the client.

## Deferred

Natural activity XP, gathering activations and tracking/search jobs, remaining
profession effects, Workbench/rarity/affix gameplay, Crafting XP payouts, and
additional automatic recipe milestones. This chunk does not finalize tuning.

## Result

Implemented this scope. The production build and all three layout checks pass;
dedicated tests report 89/89 required passes, including seven profession tests.
Real resource reload checks verify retained historical spending, stale-preview
rejection, failed-reload preservation and removed-definition respec. Packaging
inspection excludes test/local artifacts. Runtime documentation and manual
client checks are in [PROFESSIONS.md](PROFESSIONS.md); evidence is in
[VALIDATION.md](VALIDATION.md). No client launch or deployment was performed.

## Follow-on: natural activity XP

The follow-on chunk now implements natural Mining/Logging/Farming XP and vanilla
enchanting-table Crafting XP through confirmed server outcomes. It reuses the
four saved tracks, award engine and catalogue, adds chunk-owned placed-ore
exclusions (including native piston movement), and recognizes trees with bounded
loaded-chunk searches and transient evidence for sequential chopping. No
gathering abilities or Workbench/itemization effects are enabled by this chunk.
Runtime boundaries and manual checks are in [PROFESSIONS.md](PROFESSIONS.md).
Crafting reward decisions are now recorded in the linked design notes; all
balance remains provisional. Additional recipe milestones are outside the slice.

## Follow-on: gathering batches

Vein Mining and Tree Felling now use the existing purchases, original-block
activity XP and loaded-chunk harvesting definitions. Native per-block costs and
loot are retained, sneaking bypasses expansion, and tool break or the first
blocked harvest stops the batch. Lowest remaining certified logs can continue
partial trees using disposable evidence. Other gathering abilities, including
Automatic Replanting, remain deferred. Current contracts, configurable limits
and validation evidence are in [PROFESSIONS.md](PROFESSIONS.md) and
[VALIDATION.md](VALIDATION.md).
