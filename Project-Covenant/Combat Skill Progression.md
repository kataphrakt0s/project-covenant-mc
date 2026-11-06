# Combat Skill Progression

## Character point budget

- Character level starts at 1 and is capped at 60.
- Character creation grants one skill point at level 1.
- Each subsequent character level grants one additional point: 60 lifetime points by level 60.
- Unspent points remain available and never expire.
- Character levels do not grant attribute points or separate general-stat points.

See [[Character Progression]].

## Unlocking and ranking skills

- Every skill has a minimum character level, transcribed from the proposed class-tree diagram in [[Skill Trees Brainstorm]].
- The listed level is a requirement to invest the **first** point in that skill.
- The first point unlocks the skill at Rank 1.
- Additional points can be invested without further character-level gates,
  until the skill reaches its individual maximum rank. Investment is committed
  through Apply as described below.
- Each rank consumes one skill point.
- Each skill's maximum rank is individually defined as either **8 or 12**. The cap depends on how much meaningful improvement the skill can support; it is not a blanket rank for all skills.
- Every rank should provide a meaningful upgrade. A 12-rank skill should have enough meaningful tuning opportunities to justify the extra ranks rather than padding the cap with negligible bonuses.

### Initial rank-cap assignments (agreed, planned)

Lunging Combo (`covenant:lunging_combo`) and Rip and Rush
(`covenant:rip_and_rush`) each have a maximum rank of **12**. Every other
currently playable combat skill has a maximum rank of **8** for this slice.
The combos have three stages across which minor upgrades can be distributed;
fully ranking one therefore uses more of the 60-point budget.

Store the cap in each skill's definition rather than deriving it from a global
effect-category rule. These assignments cover the existing playable roster;
unfinished skills remain unavailable and their eventual caps are not selected
by this decision. Individual rank-effect tables remain to be authored.

### Initial vertical-slice availability

The first progression slice uses existing playable combat skills only; it does
not require implementing new combat skills. Unfinished nodes remain visible but
unavailable for point investment. Existing playable skills whose prerequisite
chains include unfinished skills are also unavailable for investment. Preserve
the intended tree connections rather than bypassing those prerequisites. Rank
upgrades above Rank 1 are available only for Piercing Shot and Aimed Shot for
now. All other playable skills allow Rank 1 until meaningful upgrades are
authored; their planned 8/12 maximums remain metadata. Never sell ranks that add
no benefit. Completing the remaining roster is separate work. See
[[Progression Vertical Slice]].

## Pending investment and Apply

Keep the interaction simple in the existing Skills tree:

- Left-click adds one pending rank, including a pending Rank 1 unlock.
- Right-click removes one pending rank, stopping at the already-committed rank.
  It cannot refund previously committed points; that requires the respec item.
- A plain **Apply** button at the bottom right commits the pending upgrades.

Display pending upgrades distinctly from committed ranks and account for their
point cost in the available draft budget. Pending ranks grant no gameplay
effects, learned-skill access, or bindable skills until the server accepts Apply.
The client cannot spend real points or change committed loadout state merely by
clicking nodes.

Players may edit a pending draft during combat, but Apply requires being out of
combat under the shared seven-second timer in [[Combat]]. Check this on the
logical server at commit time. If combat resumes before the request is handled,
reject it without changing ranks or spending points. Keep the local draft while
the screen remains open so the player can apply it after combat ends.

On Apply, send one bounded investment request to the logical server. Validate
the complete proposed allocation against current class/level, point budget,
rank caps, the eight-skill limit, prerequisites, exclusions, implemented-skill
availability, and the existing committed state. Commit the complete valid
investment together, or reject it without partial spending or allocation.
Do not allow a pending request or repeated Apply to purchase the same draft
twice. The UI reflects the server-confirmed result.

These are planned investment rules; the current free-allocation prototype has
not yet changed.

Closing the Skills screen without Apply discards unapplied edits immediately,
with no confirmation dialog. Reopening starts from server-committed ranks and
point balances; drafts are not saved or restored. Closing does not undo an
investment already submitted through Apply. Switching tabs within the same
open screen is not closing it.

## Eight-skill allocation limit

A character may have no more than **eight distinct skills with points allocated** at one time. This corresponds to the eight available combat keybinds (Q, R, F, C, and their Shift variants). A skill counts toward the limit as soon as it has at least one point invested, including prerequisite skills. Rank investment in an already-allocated skill does not consume another allocation. This is an allocation ceiling, not a requirement to fill every keybind slot.

The eight slots are a hard combat-loadout limit. Profession abilities do not use or compete for these keybinds.

## Prerequisites and exclusions

- Vertical tree connections represent prerequisites. The prerequisite must be unlocked before the connected skill can receive its first point. Both skills remain usable after the dependent is unlocked.
- Crossed horizontal connections represent deliberate mutually exclusive choices. Only explicitly connected alternatives lock one another out.
- Prerequisites and exclusions are validated server-side.
- The existing dependency behavior—removing a prerequisite also removes dependent allocations and clears their loadout assignments—must remain coherent with the point refund/respec rules.

## Respec

Combat respec is allowed and requires a crafted respec item. Using it resets the character's combat skill allocations and refunds all invested skill points to the unspent pool. Character level and earned XP are retained.

The agreed prototype recipe is shapeless: **four paper plus one lapis lazuli**
produce **one combat-respec item** at a vanilla crafting table. This is an
intentionally inexpensive starting cost for build experimentation, subject to
later tuning. It adds no profession-level requirement or custom workstation.
Use the normal held-item interaction: hold right-click for two seconds
(40 server game ticks). Releasing early cancels without consuming the item or
changing allocations. On successful completion, the server consumes exactly
one item and refunds the combat allocations. Consume only on a successful
reset, with the held item and player state revalidated at completion. The
interaction does not require a separate confirmation screen.

Respec requires being out of combat. Use the shared seven-second (140-game-tick)
combat state from [[Combat]], also used by Rage's combat grace. Confirmed damage
dealt/taken or qualifying support refreshes it for any class. Validate that it
has expired both when item use begins and when it completes; re-entering combat
during the hold cancels without spending the item or changing allocations.

The current tree prototype's free allocation/unlearning is a testing implementation and does not define the final progression economy.

## Prototype-save compatibility scope

Existing prototype saves do not need support in the first progression slice.
Use fresh progression saves rather than converting free learned-skill sets or
implementing a one-time allocation/binding reset. Normal save/load, owner sync,
and lifecycle persistence remain required for the new point/rank format. See
[[Progression Vertical Slice]].

## Skill design and balance

### Agreed rank-design rules (planned)

- Existing playable skills retain their current prototype behavior and numerical
  values at Rank 1. These are the initial baseline, not a claim of final balance.
- Higher-rank improvements must be minor, flavorful, and specific to the skill's
  identity. Do not use flat damage per rank or a routine damage-scaling formula
  as the progression model.
- Favor modest improvements to handling, utility, or characteristic interactions.
  Numerical changes still need a skill-specific justification; changing duration,
  area, cost, or cooldown is not automatically harmless to balance.
- Assess the cumulative improvement from Rank 1 to maximum rank, including
  interactions with equipment and existing skill modifiers. Small individual
  upgrades must not accumulate into a large power increase that disrupts vanilla
  balance.
- Every purchased rank must have a useful, understandable benefit within that
  modest total improvement. Individual rank-effect tables remain to be designed
  under the initial cap assignments above.

Preserve the vanilla-friendly stat philosophy in [[Statistics]] and [[Design Pillars]].

### Aura rank boundary (agreed, planned)

Keep each aura's passive stat/effect strengths at its Rank 1 values across all
ranks. Ranking does not increase always-on damage bonuses, defenses, slows, or
resource/stat modifiers supplied by the passive. Existing overlap/stacking
rules remain in force.

Authored rank upgrades can modestly improve the aura's activation or coverage
where appropriate, within the shared minor-upgrade budget. This deliberately
gives passive-focused builds less reason to maximize an aura while limiting
always-on stat inflation. Individual active/coverage values remain to be
specified; do not apply a generic passive-power multiplier per rank.

### Prototype arrow-shot upgrades (agreed, planned)

Use increased arrow velocity as the default prototype rank improvement for
most arrow-shot skills. For the currently playable roster, this direction
applies to Piercing Shot and Aimed Shot. More varied, bespoke rank progression
can be designed later; do not extend this direction to traps, spell projectiles,
or every unfinished ranged skill without its own requirements.

Each additional rank adds **2% of Rank 1 launch velocity**, additively:
`velocityMultiplier(rank) = 1 + 0.02 * (rank - 1)`. Ranks 1 through 8 therefore
use multipliers 1.00, 1.02, 1.04, 1.06, 1.08, 1.10, 1.12, and 1.14. This is
+14% at Rank 8, not a compounding multiplier. The existing launch-speed parameter
is 3, giving 3.42 at Rank 8 before native inherited motion or other interactions.
Retain Aimed Shot's Rank 1 wind-up movement behavior and the current skill
damage coefficient, resource cost, cooldown, and cast time.

Native arrow impact damage multiplies impact speed by the arrow's damage
coefficient. Raising velocity can therefore increase damage even without a
separate rank damage modifier. **Retain that native speed-based damage behavior
for the prototype**, without a compensating damage adjustment. The resulting
indirect damage increase is accepted, subject to playtesting; do not claim
damage is unchanged. This modifies the skill-fired arrow at creation, not
ordinary vanilla bow shots, and later rank changes do not retune arrows already
in flight.

More varied arrow progression and other skills' detailed rank tables remain
later content/balance work. The velocity progression above is specified for
the two current arrow shots; it is not a complete rank table for the full
playable roster. The shared progression foundation can proceed independently.

Related: [[Character Progression]], [[Skill Trees Brainstorm]], [[Abilities]], [[Classes and Specializations]], [[Player Profiles]], [[Progression Balance Configuration]].
