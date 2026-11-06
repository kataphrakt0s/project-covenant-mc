# Character Progression

## Rules

- Character level starts at **1** and has a global maximum of **60**.
- A new character starts with **1 skill point** at level 1.
- Each character level gained awards one additional skill point. A level-60 character has earned **60 skill points in total**.
- Unspent skill points remain available; points do not expire or have a lifetime timer.
- Character levels do not award attribute points or a separate general-stat budget.
- Character XP is a separate Covenant progression currency. Vanilla Minecraft XP orbs and vanilla enchanting levels do not contribute to it.
- Choosing a combat class is required to earn character XP. Before selection,
  combat earns no character XP and nothing is banked or awarded retroactively.
  First class selection starts character progression at Level 1, zero character
  XP, and one skill point; independently earned profession progress is preserved.
- Character XP comes primarily from combat, with supplemental rewards for significant milestones such as boss victories, dungeon completion, and meaningful discoveries.
- Multiplayer XP from an enemy is split equally among eligible contributing
  players, including support-only participants who deal no damage themselves.
  Each player counts once; dealing more damage or landing the final blow does
  not increase their share. Detailed qualification rules remain open; see
  [[Experience and Reward Systems]].
- Progression is persistent and server-authoritative.

## Combat skill relationship

Character level determines when a skill can receive its **first** point, using the minimum character levels shown in [[Skill Trees Brainstorm]]. The first point unlocks the skill at Rank 1. Further ranks do not require additional character levels; they require available skill points and remain subject to the skill's individual maximum rank.

No more than **8 distinct skills** may have points allocated at once. The maximum number of allocated skills matches the eight available combat keybind slots; this is an allocation ceiling, not a requirement to fill every slot. Skill maximum rank is defined per skill as either **8 or 12**. For the initial playable roster, Lunging Combo and Rip and Rush cap at 12; all other currently playable combat skills cap at 8. Individual rank effects remain balance work; see [[Combat Skill Progression]].

A crafted combat respec item resets the character's combat skill allocations
and refunds the invested skill points. Its prototype recipe is four paper plus
one lapis lazuli, shapeless at a vanilla crafting table, producing one item.
See [[Combat Skill Progression]] for its planned behavior.

See [[Combat Skill Progression]] and [[Skill Trees Brainstorm]].

## Progression presentation (agreed, planned)

Place a thin Covenant character-XP bar directly below the vanilla XP bar, within
the existing bottom HUD footprint. Do not grow the health/hunger/resource stack
upward or move the hotbar to make another labeled row. The Covenant bar has no
text, character-level number, percentage, icon, or label for now. Use a visually
distinct fill and normal HUD visibility, with character progression available
after class selection.

The existing Stats page shows the detailed progression: character level,
current-level XP versus the next-level requirement, percentage progress, and
unspent combat skill points. These values read owner-synced server state.
Classless players should see that class selection is required to earn character
XP. The HUD bar does not consume, convert, or replace vanilla XP.

Exact pixel thickness/alignment must fit the pinned client layout and GUI
scales without colliding with the hotbar, item selection, or native contextual
bar variants. This is planned presentation; rendered layout remains a manual
client check.

## XP pacing

### Opening target (agreed, planned)

At the initial default tuning, reaching Level 2 from Level 1 should take roughly
five ordinary hostile kills while solo. This is a prototype pacing target for
an ordinary reference enemy, not an identical reward requirement for every mob.
Shared XP, stronger enemy rewards, and milestone awards can change the number
of kills an individual player needs.

### Final level target (agreed, planned)

Advancing from Level 59 to Level 60 should require thousands of ordinary
vanilla Enderman kills, or dozens of boss kills, when the player receives the
full solo reward and the applicable dimension gate allows Level 60. This is
the requirement for that one level, not the total journey from Level 1.

The agreed starting benchmark is **3,000 ordinary vanilla Endermen or 30
reference boss kills** for that level. A reference boss therefore awards the
equivalent of 100 ordinary Endermen. Treat these as configurable prototype
balance anchors, not a claim of validated playtime.

Author an explicit vanilla Enderman XP entry as the ordinary endgame reference.
The agreed reference boss is a repeat Wither kill, awarding 2,000 base Covenant
character XP before sharing and caps. The first-time Wither milestone still
replaces the recipient's ordinary XP share. For Level 59-to-60, the 30-kill
benchmark therefore means solo repeat Wither kills in the End, whose cap allows
Level 60. Overworld and Nether repeat kills retain caps of 20 and 40.

Other bosses' individual rewards remain open; do not assume all bosses have
identical rewards. Multiplayer sharing changes
the number of kills needed per player under the agreed equal-share model.

### Accelerating prototype curve (agreed, planned)

Use a smooth accelerating next-level requirement, growing by approximately
13% per successive level. Classic WoW is the intended reference for the feel
of sustained leveling and grind. This is a pacing direction, not a claim that
Minecraft playtime or encounter difficulty already matches that reference.

Use the following scale for the initial prototype: an ordinary reference mob
awards 10 character XP, an ordinary vanilla Enderman awards 20, and a reference
boss awards 2,000. These are reference weights, not a blanket reward for every
mob or boss. The resulting Level 1-to-2 requirement is 50 XP, and Level 59-to-60
is 60,000 XP, matching the agreed five-kill opening and 3,000-Enderman/30-boss
final-level anchors at full solo rewards.

For current level L from 1 through 59, the initial default curve is
represented as `round(50 * 1200^((L - 1) / 58))` XP to the next level. This is
about 13.0028% growth per step. Precompute the integer requirements for lookup
rather than evaluating the exponent on every XP award.

| Advance | Required XP | Approximate full-solo kill equivalents |
|---|---:|---|
| 1 to 2 | 50 | 5 ordinary reference mobs |
| 10 to 11 | 150 | 15 ordinary reference mobs |
| 30 to 31 | 1,732 | 87 ordinary Endermen |
| 50 to 51 | 19,969 | 1,000 ordinary Endermen |
| 59 to 60 | 60,000 | 3,000 ordinary Endermen or 30 reference bosses |

Requirements apply only where the dimension gate permits advancement. Most
of the grind is deliberately concentrated in later levels. Exact per-entity
rewards and playtested pacing remain tunable; the initial reference is the
2,000-XP repeat Wither kill.
Profession pacing remains separate and open.

Validate the intended feel through observed kills per minute and time per level,
including skill/build differences, farming, and the shared-XP group rules. These
prototype counts alone do not establish a verified leveling duration. See
[[Experience and Reward Systems]], [[Dimensional Progression Gates]],
[[Progression Balance Configuration]], and [[Open Questions]].

## First Wither victory (agreed, planned)

A player's first qualifying Wither kill grants one full character level,
bypassing the normal dimension XP cap. This is a one-time per-player reward,
not a bonus on every Wither or a permanent increase to the dimension cap.
Subsequent Wither kills use normal combat XP rewards and caps.

Every eligible contributor who has not claimed the milestone receives their
own full level, including support-only participants. Use the normal contribution
window, online-at-payout, and retained death/dimension-change credit rules.
The level reward is not divided by group size and does not require the final
blow. Each player's claim state is independent, so previous recipients do not
prevent first-time contributors from receiving the milestone.

The character/class-selection requirement, global character cap of 60, and
one-skill-point-per-level schedule still apply. Persist whether the player has
claimed this reward across death/respawn, reconnect, and restart. Ordinary XP
discarded at a dimension gate is not banked by this exception.

Preserve the player's percentage progress toward the next level through the
milestone: Level 18 at 40% becomes Level 19 at 40%. Rescale to the new level's XP
requirement using valid integer XP, rounding down where necessary. Grant one
skill point for the level gained. Reaching the global cap stores no progress
toward a nonexistent next level and banks no excess for later.

For a first-time recipient, the full-level milestone replaces that player's
ordinary Wither XP share for this kill; do not award both. Repeat recipients
receive their ordinary equal XP share under normal caps. Determine that share
using all otherwise-eligible contributors as usual. A first-time recipient's
forgone ordinary share is not redistributed to repeat recipients.

Mark the per-player claim only when the full level is successfully granted.
Commit the level, preserved progress, skill point, and claim together so the
milestone cannot be lost or repeated across save/respawn boundaries. A classless
kill does not consume it; the player needs another qualifying victory after
class selection, with no retroactive reward. A player at the global cap also
does not consume the claim. Do not queue an ungrantable reward for later.

See
[[Experience and Reward Systems]] and [[Dimensional Progression Gates]].

## No post-cap levels

Level 60 is the character-level cap. The current design has no level-100 progression and no post-level-60 character XP track. Long-term advancement after reaching the cap comes from skills, equipment, professions, dimensions, encounters, and endgame activities—not additional character levels.

Related: [[Player Profiles]], [[Classes and Specializations]], [[Class Resources]], [[Statistics]], [[Equipment]], [[Progression Systems]].
