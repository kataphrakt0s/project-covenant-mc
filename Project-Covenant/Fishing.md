# Fishing

## Agreed scope

Fishing and [[Exploration]] are implemented profession foundations, bringing
the roster to seven. Their skills remain deferred. Rendered UI and real-client
fishing still require manual playtesting; see [validation](../docs/VALIDATION.md).

Successful fishing awards Fishing profession XP. Fishing skills are deferred to
a future update. This expansion targets fresh saves; older-save compatibility
and migration are out of scope.

## Progression and catch rewards

Fishing uses the shared **Level 0-20** profession progression, earning one
Fishing Profession Point per level and retaining unspent points for future
skills. The provisional shared curve requires **21,000 total XP** from Level 0
to Level 20.

Award **50 Fishing XP once per successful loot-bearing reel-in**, including
fish, junk and treasure. The reward is per catch, regardless of item count;
rarer catches do not grant extra profession XP. Empty reels and hooking entities
grant no Fishing XP. On the shared curve this requires **420 successful catches**
to reach Level 20, assuming no XP is discarded by caps.

Use the existing server-authoritative profession eligibility and confirmed
outcome rules. Ordinary fishing loot, vanilla XP and rod behavior remain native.
The reward is provisional balance and belongs with the existing server-datapack
activity reward tuning.

## Building contribution

Fishing joins Mining,
Logging, Farming and Crafting as a source for [[Building]]. Building receives
**65% of XP actually credited to Fishing**, in addition to Fishing's own full
reward. Retain the existing fractional-credit and source/Building cap rules:
capped Fishing and discarded Fishing overflow contribute nothing. Exploration
milestones grant no Building XP.

See [[Profession Progression]] for the shared foundation.
