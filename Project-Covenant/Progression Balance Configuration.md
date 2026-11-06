# Progression Balance Configuration

## Purpose

Progression balance should be adjustable by server owners and modpack authors without editing Covenant source code or requiring hardcoded compatibility integrations. This page describes the intended configuration surface, not a finalized file syntax or schema.

## Global progression settings

Configurable values should include:

- Character global level cap (default 60).
- Character XP curve and any level pacing parameters.
- Starting character level (1) and starting skill points (1).
- Skill points awarded per character level (1).
- Profession global/default level cap (20), with per-profession override support where needed.
- Shared profession XP curve and reward tuning.
- Fallback dimension caps (character 60; profession 20 by default).

## Character XP rewards

Support configurable XP rewards for enemy/entity identifiers, tags, encounter roles, bosses, dungeon completion, discovery/milestone events, and other explicitly authored achievements. Combat remains the primary source. Multiplayer contributor qualification and exact split formula require further design.

## Profession XP rewards

Support contextual XP definitions for activity events, identifiers, tags, recipes, blocks/items/tools, entities, workstation actions, and optional machine integrations. Reward values should distinguish meaningful activity from trivial repeat actions. Ordinary vanilla crafting is excluded from Crafting XP.

Automation XP is disabled by default. Any explicitly enabled automation source should declare attribution behavior and anti-exploit controls so repeated output collection does not create unlimited XP.

## Dimensional gates

Support independently configured maximum levels for the character track and each profession in a dimension. A rule can match:

- An exact dimension identifier.
- A dimension tag.

Exact identifier rules override tag matches. Tag rules have explicit, deterministic priority. Unmatched dimensions use the configured fallback caps. See [[Dimensional Progression Gates]].

## Skill and profession content definitions

Progression definitions should support:

- Character-level requirement for first unlock of a combat skill.
- Per-skill maximum rank (8 or 12) and per-rank effect parameters.
- Combat skill-point costs (one point per rank under the current design).
- Profession-level requirement for purchasing an ability.
- Profession Point cost (typically 1–3), prerequisites, rank exceptions, and explicit mutually exclusive alternatives.
- Automatic profession recipe unlock milestones.

## Compatibility and configuration principles

- Prefer identifiers and tags over mod-name conditionals or hardcoded block/entity/dimension lists.
- Allow owners to add, remove, and edit dimension gate entries.
- New modded dimensions should work with fallback values even if no custom rule exists.
- Validate entries and provide clear error messages; avoid silently accepting ambiguous priorities or invalid caps.
- Keep authoritative decisions on the server.
- Do not expose every implementation detail as configuration without a demonstrated balance need. Keep the configuration surface understandable.

## Not yet finalized

The exact configuration file format, key names, reload semantics, schema validation, default per-dimension values, numerical XP curves, reward amounts, individual ability values, and rank scaling remain open. See [[Open Questions]].

Related: [[Progression Systems]], [[Character Progression]], [[Experience and Reward Systems]], [[Profession Progression]], [[Dimensional Progression Gates]], [[Data-Driven Content]], [[Mod Compatibility]].
