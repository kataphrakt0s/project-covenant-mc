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

- XP is checked against the current dimension's cap before being applied to the relevant track.
- When the player is already at or above that dimension's cap for the track, XP for that track is not awarded there.
- XP beyond the gate is **discarded, not banked** for later dimensions.
- Returning to a lower-cap dimension does not reduce the player's already-earned level. It only prevents further XP gain for tracks whose current level is at or above that dimension's cap.
- Each track is evaluated independently. A Mining cap does not block character XP or Farming XP unless those tracks have also reached their respective caps.

## Illustrative examples only

The following are examples discussed during design, not final balance values:

- A vanilla-style character/combat route could allow character progression to level 20 in the Overworld, 40 in the Nether, and 60 in the End. Mining was also discussed with an Overworld cap of 20.
- A modpack could instead configure a character/combat route such as Overworld 15, Nether 25, a first modded dimension 30, End 40, a second modded dimension 50, and a third modded dimension 60.

These examples do not imply that every profession shares the same caps. Mining, Logging, Farming, and Crafting each receive independently authored values for each dimension. Exact values and the intended route remain open.

## Compatibility and validation

- Rules use registered dimension IDs and dimension tags, not hardcoded known-mod dimension lists.
- Unknown dimensions use fallback behavior.
- Invalid track IDs, negative values, and caps beyond the applicable global cap should be validated with clear diagnostics and safe behavior.
- Exact ID overrides tag matches. Tag conflicts resolve by explicit priority.
- Dimension rules are server-side and authoritative; the client must not decide whether an XP award is valid.

Related: [[Progression Systems]], [[Character Progression]], [[Profession Progression]], [[Experience and Reward Systems]], [[Progression Balance Configuration]], [[Mod Compatibility]], [[Data-Driven Content]].
