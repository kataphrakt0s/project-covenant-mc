# Experience and Reward Systems

Covenant has progression XP systems that are separate from Minecraft's vanilla XP-orb/enchanting-level system. Vanilla XP is neither consumed nor converted by Covenant.

## Independent tracks

- **Character XP** advances the shared character level from 1 to a maximum of 60.
  It is primarily earned through combat and requires a selected combat class.
  Before class selection it is neither earned nor banked, and earlier combat
  receives no retroactive reward.
- **Profession XP** advances one independent profession at a time: Mining, Logging, Farming, or Crafting. Each profession starts at level 0 and defaults to a maximum of 20.
- Each profession uses the same XP curve, but has an independent XP balance and level. Character XP and profession XP never convert into one another.

See [[Character Progression]] and [[Profession Progression]].

## Character XP sources

Combat is the reliable, primary source of character XP. XP should reflect enemy/encounter significance and threat rather than simply rewarding the number of hits or the final blow. Significant accomplishments can supplement combat XP, including:

- Boss victories and major encounter completions.
- Dungeon completion.
- Meaningful discoveries or exploration milestones.
- Other explicitly authored world/encounter achievements.

These milestone awards should be occasional and meaningful, not repeatable substitutes for combat progression or easy farming exploits.

### First Wither victory exception (agreed, planned)

A player's first qualifying Wither kill grants one full character level even
above the normal dimension XP gate. Track the claim persistently per player.
Subsequent Wither kills use 2,000 base Covenant character XP before the ordinary
sharing and cap rules, forming the repeat-boss pacing benchmark. The global character
cap of 60 and class-selection requirement remain in force, and a granted level
awards its normal skill point.

Each eligible contributor who has not claimed this reward receives their own
full level, including support-only participants. Apply the normal recency,
online presence, and retained death/dimension-change participation rules. Do
not divide the milestone by group size or restrict it to the final attacker.
Track claims independently per player; an experienced contributor does not
consume or block another player's first-time reward.

Preserve percentage XP progress through the granted level: Level 18 at 40%
becomes Level 19 at 40%, rescaled to the new requirement with integer XP rounded
down as needed. At the global cap, retain no progress toward another level and
bank no excess.

This is a one-time level milestone, separate from the repeatable boss-XP pacing
benchmark. For a first-time recipient, it replaces their ordinary Wither XP
share for that kill. Repeat recipients receive their normal equal share under
the usual caps. Include all otherwise-eligible contributors when calculating
ordinary shares; do not redistribute a first-time recipient's forgone ordinary
share. A player does not receive both rewards for the same kill.

Mark the claim only upon successfully granting the level. Update level,
rescaled progress, skill point, and claim as one authoritative player-state
change. Classless players and players already at the global cap do not consume
the reward. There is no retroactive or queued award: another qualifying Wither
victory is required once the player can receive it. See [[Character Progression]].

Vanilla XP orbs do not award character XP. Non-combat everyday actions do not automatically award character XP; their progression belongs to their relevant profession track where applicable.

### Default eligible combat targets (agreed, planned)

Use hostile mobs as the default character-XP sources for the first slice.
Entity identifiers and tags allow other combat mobs to be explicitly included
through data. Neutral mobs require explicit inclusion rather than becoming
eligible simply because the player attacks them. Include vanilla Endermen
explicitly as XP sources for the endgame pacing target in [[Character Progression]]. Ordinary
livestock and player kills grant no character XP in the default slice.

Eligibility for an XP reward is separate from whether a skill may hit a target.
Do not use the broad skill-targeting policy as blanket XP eligibility. These
rules do not alter vanilla drops, vanilla XP orbs, or ordinary combat behavior.
Exact reward amounts and the authored target entries remain to be tuned.

### Spawners and future compatibility (planned)

Spawners must participate in Covenant's progression model, supporting the
eventual goal of full Apotheosis compatibility. Eligible combat mobs produced
by spawners are intended XP sources, subject to the normal class, contribution,
and dimension-gate rules. Do not categorically exclude them based on spawn
origin. In the first slice they award the same configured XP as equivalent
naturally spawned mobs, without a spawn-origin reduction. No spawn-provenance
tracking is required for XP reward selection. This deliberately permits spawner
farms as an efficient leveling route during initial testing.

Apotheosis-specific integration is future work; this slice remains standalone
and makes no claim of tested Apotheosis compatibility. See [[Mod Compatibility]].

## Multiplayer combat XP

XP from an enemy/encounter is split among contributing players rather than duplicated in full for everyone or given only to the final attacker. The implementation must avoid kill-stealing incentives and must not reward players solely for being nearby without meaningful participation.

### Equal shares (agreed, planned)

Split the enemy's configured XP reward equally among eligible contributors.
Count each player once, whether they contribute damage, support, or both. Damage
amount, damage rank among players, and the final blow do not increase a player's
share. The goal is cooperative participation without a DPS race; qualifying
support receives the same share as qualifying damage.

For example, a 90-XP enemy with two eligible attackers and one eligible
support-only player grants 30 XP to each. Split the enemy's base reward, rather
than awarding that full base amount independently to every contributor.

### Round shares up (agreed, planned)

For N eligible contributors, each ordinary share is the enemy's configured XP
reward divided by N, rounded up to the next whole XP unit. A 10-XP reward split
three ways grants 4 XP to each player. This deliberately permits a small total
bonus from rounding: 12 XP is paid in that example, although the base reward is
10. Do not distribute a remainder to selected players or maintain fractional
XP/carry balances for sharing.

Round before applying each player's dimension/global cap. Zero-reward enemies
still award zero, and no eligible contributors means no payout. Otherwise-
eligible capped players still count in N, and blocked portions remain discarded
without redistribution. First-time Wither recipients replace their ordinary
rounded share with the full-level milestone as already specified.

### Applying progression caps to shares (agreed, planned)

Calculate equal shares before applying each recipient's progression cap. A
contributor who is otherwise eligible still counts in the split when already at
or above the applicable dimension cap or global character cap. Apply the enemy's
death-dimension gate and the global cap to that player's share afterward.

Discard any portion the player cannot receive; do not bank it or redistribute
it to other contributors. This also applies when only part of a share is needed
to reach a cap. For example, a 90-XP enemy split among three contributors gives
each a 30-XP share. If one is already capped, that player's 30 XP is discarded
and the other two still receive 30 XP each. If one can accept only 10 XP before
reaching the cap, they receive 10 and their remaining 20 is discarded.

This keeps each share independent of other players' progression and avoids
increasing the remaining players' shares when a capped friend contributes.

### Participation window (agreed, planned)

Use a refreshing 30-second window per player per enemy, measured as 600 server
game ticks. Each qualifying damage or support event records that player's latest
contribution time for the affected enemy. A new contribution refreshes that
player's window for that enemy only.

When the enemy dies and its XP reward is resolved, compare each contributor's
timestamp with the current server game time. Contributions older than the window
do not qualify. The timestamp check determines recency; other payout eligibility
rules still apply. A long fight requires continued qualifying participation to
retain credit.

Implement this with enemy-owned contributor timestamps updated by combat events
and checked at payout. Do not add recurring expiry scans, per-contributor tick
timers, or a global entity scan. This is the planned implementation approach,
not a measured performance result. See [[Progression Balance Configuration]].

#### Temporary record lifetime (agreed, planned)

Contribution records are server-only runtime state on the enemy. Do not save
or synchronize the contributor list or timestamps. Enemy unload/removal and
server restart discard the list; reloading an enemy starts with no prior
participation credit. Players must contribute again after reload to qualify.

No contributor-state migration, restart restoration, or chunk forcing is needed.
This reset concerns pending participation credit only. XP already awarded,
character/profession levels, points, and allocations remain persistent player
progression under their normal save/copy rules. Contributor death/respawn alone
does not reset an enemy's still-loaded contribution list.

### Damage participation (agreed, planned)

One confirmed positive damaging hit on an eligible enemy is enough to qualify
the attributed player as a damage contributor. There is no minimum damage
amount, percentage-of-health threshold, or required number of hits. Each
further qualifying damage event refreshes that player's contribution timestamp
for that enemy; dealing more damage does not increase their XP share.

Misses, fully blocked hits, immune hits, and other zero-damage attempts do not
qualify through damage. A successful control/debuff may independently qualify
under the support rules even when its accompanying damage is zero.

### Support participation (agreed, planned)

Support-only participation qualifies for shared combat XP in the first
progression slice. A player does not have to deal damage personally to be a
contributor. The character-XP requirement to select a combat class still applies.

For the first slice, qualifying support is a source-owned effect tied to actual
combat with the enemy being rewarded. It includes:

- Supplying an applicable beneficial aura to an ally while that ally fights the
  enemy. Passive aura support qualifies; activating the aura is not required.
- A supplied Fortitude shield absorbing damage from that enemy.
- Successfully applying a slow, freeze, or debuff to that enemy during the fight.

Merely standing nearby or assigning an aura without supplying a qualifying
effect during combat does not grant participation credit. Support qualification
must use server-observed effect ownership and combat events.

These are the initial slice rules, subject to playtesting. Detailed attribution
across combat events and remaining payout implementation details
remain open; equal shares and the refreshing 30-second window are settled.

#### Overlapping passive aura sources (agreed, planned)

When multiple players supply the same qualifying passive aura to an ally
fighting an enemy, each aura owner receives contribution credit. The aura bonus
still applies only once under its normal stacking rules; XP attribution does
not select one owner as the winner or require extra benefit from each copy.

Evaluate valid source-owned coverage at qualifying combat events. Merely having
the aura assigned or standing nearby is still insufficient. Credit refreshes
each qualifying owner's timestamp for that enemy. Each player counts only once
in the eventual split, even if they supply several auras or also deal damage.

This deliberately allows redundant passive aura coverage to qualify and avoids
competition between support players over identical aura ownership. It does not
change effect strength, stacking, or the normal ownership of a replaced shield.

### Contributor death (agreed, planned)

Death does not erase a player's already-recorded contribution or disqualify them
solely because they are dead or have respawned when the enemy dies. If their last
qualifying contribution is still within the 30-second window, they retain
participation credit, subject to the other payout rules. Death neither resets
nor pauses that window.

Track the contributor by player UUID rather than a particular player entity so
native respawn replacement does not lose or duplicate credit. Awarded XP must
survive the normal progression copy/persistence path across respawn.

Minecraft owns Hardcore death and spectating behavior. Covenant does not add a
separate Hardcore mode, permadeath system, or additional Hardcore death penalty.
Other payout details follow the rules below.

### Online at payout (agreed, planned)

A contributor must be online when the enemy dies and its XP reward is resolved.
Offline contributors are ineligible for that payout and are excluded from the
recipient set before equal shares are calculated. There is no offline reward
queue, deferred player-file award, or retroactive payout on reconnect.

This checks online presence at payout, not whether the player survived the
fight. An online player who died or respawned still retains contribution credit
within the remaining window under the death rule above. Other eligibility
checks remain applicable.

### Distance at payout (agreed, planned)

Do not impose a distance/radius requirement on already-recorded contributors
when the enemy dies. Retreating, teleporting away, or respawning far from the
enemy does not by itself remove credit. The player must still be online and
have a qualifying contribution within the remaining 30-second window. Distance
does not refresh or extend that window.

This concerns payout eligibility only. Skills, auras, and support qualification
retain their normal range and targeting rules.

### Dimension at payout (agreed, planned)

Changing dimensions, including respawning in another dimension, does not erase
recorded contribution credit or impose a same-dimension payout requirement.
The contributor must still be online and within the remaining participation
window. Dimension changes do not pause or refresh that window.

For combat XP, resolve the dimension gate from the dimension where the enemy
dies. Apply that gate to each recipient, regardless of their location at payout.
Teleporting before payout therefore cannot substitute the destination's cap.

For example, a player who contributes in the Nether and respawns in the
Overworld can receive their share if the enemy dies in the Nether within the
remaining window. The Nether's character XP cap applies to that award. These
initial slice rules are subject to playtesting; see [[Dimensional Progression Gates]].

## Profession XP sources

The planned shared prototype profession curve costs **100 × the target level
per transition**, from 100 XP to reach Level 1 through 2,000 more to reach
Level 20. All four tracks retain independent XP; Level 0 to 20 totals
**21,000 XP per profession**. These units provide room for widely differing
activity rewards. They do not establish validated leveling time; action
rewards and pacing still require tuning. See [[Profession Progression]].

Profession XP is event-driven, contextual, and tied to the relevant Minecraft activity. Rewards should recognize meaningful participation and significance, not grant a fixed efficient reward for every repeated low-value action.

Examples of intended source categories:

- Mining: successful extraction of eligible ore blocks, once per block,
  including player-triggered Vein Mining. Player-placed ores grant no XP;
  Silk Touch still rewards initial extraction of ore that was not player-placed.
  Fortune and drop processing do not multiply XP. Prospecting grants no XP.
  The provisional coal/copper/Nether-quartz reward is **10 XP per eligible
  original ore block**, including the relevant stone/deepslate variants.
  **Iron awards 17 XP per eligible original block**, shared by stone and
  deepslate variants.
  **Gold, lapis, and redstone award 25 XP per eligible original block**,
  including Nether gold and relevant stone/deepslate variants.
  **Diamond and emerald award 50 XP per eligible original block**, shared by
  stone and deepslate variants.
  **Ancient debris awards 200 XP per eligible original block**, independently
  of the Level 20 Prospecting detection unlock. See [[Mining]] for the complete
  provisional initial reward table and planned eligibility rules.
- Logging: meaningful tree felling, forestry, and wood-processing interactions.
  Player-performed harvesting of qualifying sapling-grown trees has the same
  XP eligibility as equivalent wild trees; automated harvesting grants no XP
  by default. Harvest rewards are paid once per eligible log actually removed,
  including player-triggered Tree Felling, with no ability XP multiplier or
  drop-quantity multiplier. The **provisional reward is 20 XP per eligible log**,
  shared across supported tree species; five qualifying logs reach Level 1.
  See [[Logging]].
- Farming: **only mature supported crop harvests and successful player-attributed
  breeding** for the initial slice. Award crop XP once per mature crop harvested,
  including ordinary harvesting and Scything, without ability/drop-quantity
  multipliers. The **provisional crop reward is 5 XP per mature supported
  crop**, shared across the initial crop types; twenty mature harvests reach
  Level 1. Planting/replanting, Bone Meal use, feeding alone, milking,
  shearing, passive collection, and automation grant no Farming XP. Successful
  breeding provisionally pays **100 XP once per event** to vanilla's credited
  breeder, without splitting
  or duplicating it between players who fed different parents. See [[Farming]].
- Crafting: meaningful enchanting, designated advanced recipes, Covenant workstation interactions, affix crafting/modification, and salvaging.

Ordinary vanilla crafting does **not** award Crafting XP. Successful vanilla
enchanting-table operations are the initial Crafting XP route from Level 0 to
Level 1, before Basic Affix Crafting can be purchased. Each successful operation
awards XP once: provisionally **20 / 50 / 100 Crafting XP** for the first,
second, and third options respectively, regardless of displayed level
requirement. Levels actually spent and the number of enchantments produced
do not scale the reward. Native enchanting requirements and costs remain in
place. These enchanting rewards are implemented and remain provisional. See
[[Crafting Profession]].

Each successful **Add, Upgrade, Reroll, or Remove** operation awards Crafting
XP once to the acting player when the server completes it and spends the
required materials and Crafting Potential. Rerolls qualify even if the resulting
value is worse or unchanged. Previews, rejected requests, and collecting/moving
the equipment grant no XP. This reward scales with the **affected affix's
rarity**, with higher built-in rarities paying more. Add uses the added affix;
Upgrade/Reroll use the targeted affix; Remove uses its rarity before removal.
For example, working on a Common affix on Legendary equipment pays the Common
affix-operation reward. The prototype uses **one XP reward per affix rarity,
shared across all four operations and all numerical tiers**: provisionally
**20 / 40 / 60 / 80 / 100 Crafting XP** for Common / Uncommon / Rare / Epic /
Legendary respectively. These are agreed, data-driven starting rewards for
future implementation and playtesting, not implemented XP acquisition.

Salvaging awards Crafting XP **once per affixed equipment item actually consumed**,
to the player pressing Salvage, including when the item's recovery rolls
produce no outputs. Batch rewards are the sum for consumed items, without
batch or component-count multipliers. Queuing/removing inputs, previews,
collecting results, rejected operations, and items left queued award no XP.
Shared workstation access does not duplicate the award. Per-item salvage XP
scales with the consumed equipment's Covenant rarity, with higher built-in
rarities paying more. Equipment with **no Covenant affixes at consumption grants
zero Crafting XP**, including uninitialized gear and initialized gear whose
affixes were all removed. Vanilla enchantments alone do not qualify; normal
eligible salvage material returns remain available. For affixed gear, the
provisional reward is **10 / 20 / 30 / 40 / 50 Crafting XP** for Common /
Uncommon / Rare / Epic / Legendary equipment respectively. Keep these amounts
data-driven. These are agreed starting values for future implementation and
playtesting, not implemented XP acquisition.
See [[Salvaging]].

## Automation and modded content

Direct player participation is the default requirement for profession XP. Automated production grants no XP by default. A server owner may explicitly configure exceptions for particular machines, recipes, or activity sources. Such exceptions need reliable attribution to a player or machine owner and safeguards so repeatedly collecting the same output cannot generate unlimited XP.

Modded blocks, items, tools, entities, recipes, activities, and machines should participate through tags, identifiers, data definitions, or optional integrations rather than requiring a hardcoded dependency list.

## Anti-exploit and reward principles

- Reward meaningful activity, not repetitive trivial actions.
- Prevent duplicate event processing and repeated output collection from generating duplicate XP.
- Verify the action and eligible source on the server.
- Respect dimensional progression gates before awarding XP.
- Do not bank XP earned beyond a dimension's gate.
- Keep reward values configurable and validate malformed/unknown entries safely.

Action reward values, additional authored milestone rewards, profession
repeat/exploit rules, and detailed source attribution remain open. See
[[Open Questions]] and [[Progression Balance Configuration]].

Related: [[Character Progression]], [[Profession Progression]], [[Dimensional Progression Gates]], [[Mining]], [[Logging]], [[Farming]], [[Crafting Profession]], [[Mod Compatibility]].
