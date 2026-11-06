# Progression Balance Configuration

## Purpose

Progression balance is adjustable by server owners and modpack authors without editing Covenant source code or requiring compatibility integrations. The implemented combat/shared schema is documented in [PROGRESSION.md](../docs/PROGRESSION.md); profession content remains separate.

## Definition source

Character XP requirements, enemy XP rewards, and dimension-cap rules live in
server datapack JSON. Ship working defaults with Covenant, with server/datapack
overrides using stable identifiers and tags. The server loads and validates
these definitions and supplies clients only the presentation data they need.

Support loading the definitions with server resources and reloading through
the server's datapack reload workflow. Override
`data/covenant/progression/balance.json`; successful reloads publish one immutable
snapshot and failed reloads retain the previous one. This decision does not choose
profession abilities or their content schema, which await separate requirements.

### Preserving earned progress on balance changes

Preserve earned character levels, skill points, allocations, and the raw XP
already stored toward the next level when progression balance changes. Do not
reset progression, subtract earned XP, recalculate earned levels from a new
curve, or revoke points. This applies to both online players and saved offline
players when they reconnect, including changes applied through restart.

New requirements and rewards govern future XP awards. The displayed percentage
is the retained raw XP divided by the new requirement, so it can change. For
example, 300/1,000 XP becomes 300/2,000 XP if that level becomes harder: 300 XP
is retained and the bar changes from 30% to 15%. Making progression harder adds
future work; it does not delete previously earned levels, points, or XP.

Do not grant or remove levels merely because a definition reloads. If a cheaper
requirement makes stored XP sufficient for advancement, resolve it on the next
qualifying XP award under the new rules. Lowered caps restrict future gains
without reducing earned levels or wiping existing stored XP. This retains
existing progress only; newly blocked XP is still discarded under the cap rules.

The first-Wither milestone remains a distinct operation: it grants a level and
preserves percentage progress as specified in [[Character Progression]].

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

The initial opening target is roughly five ordinary hostile kills while solo
to advance from character Level 1 to Level 2. Tune the first level requirement
and reference enemy reward together.

The agreed starting Level 59-to-60 benchmark is 3,000 ordinary vanilla Enderman
kills or 30 reference boss kills at full solo rewards, in a dimension whose
gate allows Level 60. The reference boss reward is therefore equivalent to
100 ordinary Endermen. Include an explicit Enderman reward entry.
The agreed prototype uses approximately 13% more required XP per level. Initial
reference weights are 10 XP for an ordinary mob, 20 for an Enderman, and 2,000
for a reference boss. From current level L (1 through 59), the integer next-level
requirement is `round(50 * 1200^((L - 1) / 58))`, giving 50 XP for Level 1-to-2
and 60,000 for Level 59-to-60. Precompute requirements for award-time lookup.
Use `minecraft:wither` at 2,000 base Covenant character XP as the repeat-boss
benchmark, before sharing/caps. First-time recipients receive their full-level
milestone instead. At Level 59, ordinary repeat Wither rewards can advance the
character in the End under the default route; Overworld/Nether caps still apply
to kills there. Keep requirements and rewards adjustable; other individual
boss/enemy entries remain open. Classic WoW is the target pacing feel,
subject to Minecraft playtesting. Profession pacing is separate. See
[[Character Progression]].

Support configurable XP rewards for enemy/entity identifiers, tags, encounter roles, bosses, dungeon completion, discovery/milestone events, and other explicitly authored achievements. Combat remains the primary source.

Default combat reward eligibility covers hostile mobs. Other combat mobs,
including neutral mobs, can be explicitly included by entity IDs or tags.
Ordinary livestock and player kills grant no character XP in the default slice.
Reward definitions must distinguish XP eligibility from skill hit eligibility.

Eligible spawner mobs use the normal configured reward for an equivalent
naturally spawned mob in the first slice. Do not require spawn-origin metadata
or introduce a spawner XP reduction for this slice. Apotheosis-specific reward
interactions remain future integration work; see [[Mod Compatibility]].

Multiplayer rewards use equal shares among eligible damage and support
contributors, counting each player once. Round each ordinary share up to whole
XP before applying caps; the resulting small total bonus is intentional. Keep
integer XP rather than a fractional-sharing accumulator. The design avoids a
DPS race. Detailed source attribution remains to be implemented; see
[[Experience and Reward Systems]].

The participation window defaults to 30 seconds (600 server game ticks),
refreshing per player per enemy on qualifying damage or support. Expose the
duration as server-side balance tuning. Timestamp recency is checked at enemy
death/payout; changing this duration does not require recurring expiry scans.

## Profession XP rewards

Support contextual XP definitions for activity events, identifiers, tags, recipes, blocks/items/tools, entities, workstation actions, and optional machine integrations. Reward values should distinguish meaningful activity from trivial repeat actions. Ordinary vanilla crafting is excluded from Crafting XP.

Automation XP is disabled by default. Any explicitly enabled automation source should declare attribution behavior and anti-exploit controls so repeated output collection does not create unlimited XP.

## Dimensional gates

Initial character entries use caps of 20 for the Overworld, 40 for the Nether,
and 60 for the End. These remain configurable. A one-time per-player first-Wither
level milestone bypasses the dimension gate; later Wither XP does not. The
global character cap remains in force. Profession dimension entries remain
open pending their separate requirements.

Support independently configured maximum levels for the character track and each profession in a dimension. A rule can match:

- An exact dimension identifier.
- A native dimension-type tag (`tags/dimension_type`), grouping dimensions that use those types.

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

Server datapack JSON is settled for character XP requirements, enemy rewards,
and dimension gates. Resource paths/field names, validation/error handling,
tag-priority syntax, individual enemy
and boss reward entries, profession content, and combat rank tables remain open.
The initial character curve and vanilla dimension-cap defaults are agreed
prototype tuning, subject to playtesting. See [[Open Questions]].

Related: [[Progression Systems]], [[Character Progression]], [[Experience and Reward Systems]], [[Profession Progression]], [[Dimensional Progression Gates]], [[Data-Driven Content]], [[Mod Compatibility]].
