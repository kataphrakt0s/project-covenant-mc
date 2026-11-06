# Dimensional Progression Gates

## Purpose

Dimension gates control how far each Covenant progression track can advance from XP earned in a given dimension. They provide configurable progression pacing that can fit vanilla dimensions and modded dimensions without a hardcoded compatibility list.

A gate limits XP progression only. It does not prevent a player from entering or exploring a dimension, and it never reduces a level already earned.

## Independent tracks

Each dimension may define a maximum level for each track independently:

- Character progression (character XP; primarily combat-earned).
- Mining.
- Logging.
- Farming.
- Crafting.

The same dimension can have different limits for different tracks. A future profession or progression track should be addable through configuration/data definitions without redesigning the gate model.

## Matching rules

A configuration entry may match:

1. **Exact dimension identifier**, such as `minecraft:overworld` or a mod's registered dimension ID.
2. **Dimension tag**, such as `#namespace:dimension_group`, to target a set of dimensions.

Resolution precedence:

1. An exact dimension identifier overrides matching tag rules.
2. If multiple configured tags match, use their explicitly configured priority.
3. If no exact ID or configured tag matches, use the fallback caps.

Tag priority must be deterministic and configurable. Do not rely on file order or implicit dictionary iteration as precedence.

## Fallback caps

The default fallback is:

- Character progression: **60**.
- Each profession: **20** (or the configured global maximum for that profession, if overridden).

Server owners can adjust fallback behavior. A newly installed modded dimension therefore works immediately without a dedicated Covenant integration or manual entry.

## XP behavior at a gate

- XP is checked against the applicable source dimension's cap before being
  applied to the relevant track. For combat XP, this is the dimension where the
  enemy dies, not the recipient's dimension at payout.
- When the player is already at or above that dimension's cap for the track,
  ordinary XP for that track is not awarded there. The one-time first Wither
  level reward below is an explicit exception for character progression.
- XP beyond the gate is **discarded, not banked** for later dimensions.
- Returning to a lower-cap dimension does not reduce the player's already-earned level. It only prevents further XP gain for tracks whose current level is at or above that dimension's cap.
- Each track is evaluated independently. A Mining cap does not block character XP or Farming XP unless those tracks have also reached their respective caps.

### Shared combat XP at caps (planned)

First divide the enemy's reward equally among eligible contributors, including
those already at a dimension or global character cap. Then apply the applicable
caps to each player's share. A capped player's blocked share is discarded;
other contributors do not receive it. If a share reaches a cap partway through
the award, accept only the XP needed to reach that cap and discard the rest.
No blocked XP is banked or redistributed. See [[Experience and Reward Systems]].

### Combat payouts after dimension changes (planned)

Recorded combat contributors can receive XP after changing dimensions or
respawning elsewhere, provided they are online and their qualifying contribution
is still within the 30-second window. There is no same-dimension or distance
requirement at payout. Apply the enemy's death-dimension character gate to each
recipient; moving to a dimension with a different cap cannot change that award's
gate. Dimension changes neither reset nor pause contribution expiry. See
[[Experience and Reward Systems]].

## Initial character route and Wither exception (planned)

The initial vanilla character route uses Overworld 20, Nether 40, and End 60
as the dimension XP caps. These are configurable entries, not dimension-entry
restrictions or hardcoded compatibility checks. Profession dimension caps
remain separate and open pending their requirements.

A player's first qualifying Wither kill grants one full character level even
when the normal dimension gate would prevent further character XP. Later Wither
kills obey normal rewards and caps. This one-time reward does not bypass the
global character cap or permanently raise a dimension cap. Multiplayer
contributors, including support-only players, each receive their own full level
if eligible and unclaimed; the milestone is not split. It replaces the
recipient's normal XP share, preserves percentage XP progress, and marks the
claim only when the level is granted. See
[[Character Progression]] and [[Experience and Reward Systems]].

## Other illustrative examples only

The following are examples discussed during design, not final balance values:

- Mining was discussed with an Overworld cap of 20, but this is not a finalized profession rule.
- A modpack could instead configure a character/combat route such as Overworld 15, Nether 25, a first modded dimension 30, End 40, a second modded dimension 50, and a third modded dimension 60.

These examples do not imply that every profession shares the same caps. Mining,
Logging, Farming, and Crafting each receive independently authored values for
each dimension. Profession values and modpack route overrides remain open.

## Compatibility and validation

- Rules use registered dimension IDs and dimension tags, not hardcoded known-mod dimension lists.
- Unknown dimensions use fallback behavior.
- Invalid track IDs, negative values, and caps beyond the applicable global cap should be validated with clear diagnostics and safe behavior.
- Exact ID overrides tag matches. Tag conflicts resolve by explicit priority.
- Dimension rules are server-side and authoritative; the client must not decide whether an XP award is valid.

Related: [[Progression Systems]], [[Character Progression]], [[Profession Progression]], [[Experience and Reward Systems]], [[Progression Balance Configuration]], [[Mod Compatibility]], [[Data-Driven Content]].
