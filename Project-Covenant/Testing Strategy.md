# Testing Strategy

High-value coverage: profile serialization/persistence; XP and level bounds; resource bounds; Affix Page serialization; item transfer/save-reload; safe copy/split; category resolution; stat aggregation/recalculation; dedicated-server classloading and multiplayer.

For the planned progression slice, verify the integer XP curve's 50-XP opening
and 60,000-XP final-level requirements, level/point accounting, dimension clamps,
shared contributor eligibility and equal payouts, and the persistent first-Wither
level exception. Test the new save format on fresh saves; prototype-save
migration is not required.

Balance-change tests must preserve earned levels, points, allocations, and raw
XP for online and offline players when curves become harder or easier. Reload
alone must not advance/delevel a character; future qualifying awards use the
new rules. Verify that lowering caps preserves earned state while blocking
new gains, and distinguish this from the percentage-preserving Wither milestone.

Shared-XP arithmetic must round each ordinary share upward before applying caps:
10 XP split three ways pays 4 each, while a zero reward remains zero. Test the
intentional rounding bonus, no-contributor case, cap clamping without
redistribution, and first-Wither reward replacement. Use arithmetic that does
not overflow for validated large reward values; no fractional carry is required.

Arrow rank checks must verify unchanged Rank 1 launch behavior and additive
velocity multipliers from 1.00 at Rank 1 to 1.14 at Rank 8 for Piercing Shot and
Aimed Shot. Preserve Aimed Shot's wind-up slow and other baseline tuning. Native
impact damage remains speed-based, so verify the accepted indirect damage
growth rather than asserting unchanged damage. Inspect existing projectile
save/load behavior so fired motion is retained without later rank edits
retroactively changing it.

For planned skill investment, verify pending edits grant no effects or binding
access, right-click cannot remove committed ranks, and Apply validates/commits
the complete allocation without partial spending. Check insufficient points,
rank/level gates, prerequisites/exclusions, the eight-skill limit, unfinished
skills, stale state, and repeated requests. The rendered tree must retain its
fit-to-page layout with a simple Apply button at the bottom right.
Closing without Apply must discard the local draft without changing points,
ranks, assignments, or gameplay effects, and without a confirmation dialog.
Reopening must show committed state; closing after submitting Apply does not
undo the server's accepted investment.
Draft editing may occur during combat; Apply must validate out-of-combat state
at server commit time and reject combat-time requests without spending points
or partially allocating. Confirm that the draft remains editable while open.

The intended Classic WoW-like grind is a manual pacing target. Record actual
kills per minute and time per level for representative solo builds, farms,
bosses, and groups before claiming that target is met. Numerical threshold
tests do not establish in-game leveling duration. These are planned checks;
no progression implementation or pacing observation is established by this note.

Manual presentation checks must verify the thin unlabeled Covenant XP bar below
vanilla XP at supported GUI scales, without upward HUD growth or hotbar
collision. Check normal F1 visibility and native contextual bar variants, and
verify that the Stats page exposes level, current/required XP, percentage, and
unspent skill points from synchronized state. These checks remain pending until
the feature is implemented and the user playtests the client.

Related: [[Technical Architecture]], [[Player Profiles]], [[Affix Pages]], [[Stat Calculation Pipeline]].
