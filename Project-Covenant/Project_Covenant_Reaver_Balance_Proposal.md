# Project Covenant: Reaver Class Balance Proposal

**Status:** Proposed numerical baseline for implementation and playtesting  
**Class:** Reaver  
**Resource:** Rage  
**Design philosophy:** Vanilla-first balance, with server datapack configurability for modpacks.

---

# 1. Executive Summary

The Reaver is Covenant's aggressive melee class. It rewards staying in combat, taking calculated risks, building pressure through repeated attacks, and converting that pressure into decisive finishers.

Its defining mechanics are:

- Rage generation through dealing and receiving damage.
- Bleed application and shared bleed stacks.
- Multi-stage melee combos.
- Offensive and defensive aura specialization.
- High-commitment burst abilities.
- Group utility through offensive buffs, defensive buffs, and healing.

The recommended balance direction:

1. Vanilla melee combat remains the foundation. Ordinary attacks retain their native Minecraft behavior.
2. Rage is an earned resource. The Reaver starts empty and builds Rage through combat.
3. Bleed is a shared setup mechanic with deterministic stacking, refresh, and attribution rules.
4. Direct-damage and bleed-specialist builds should both remain viable.
5. Ripping Aura and Fortitude Aura must be meaningful alternatives.
6. Bosses receive no blanket immunity or resistance by default.
7. All balance-critical values should be configurable through server datapacks.

The values in this proposal are first-pass recommendations, not playtested final values.

# 2. Class Identity

**Primary role:** Aggressive melee damage dealer and frontline combatant.

**Strengths:**
- Strong close-range damage.
- Excellent sustained pressure.
- Rage generation through active combat.
- Bleed synergies and finishers.
- Useful group buffs and defensive support.

**Weaknesses:**
- Must enter melee range.
- Vulnerable when committing to combos.
- Limited ranged flexibility.
- Resource management becomes important during prolonged encounters.

There should be no unnecessary weapon-category restrictions. Skills should work with vanilla and modded melee weapons where mechanically appropriate.

## Progression rules

Preserve the established Covenant progression system:

- Maximum character level: 60.
- First investment in a skill requires its unlock level.
- Subsequent ranks do not require additional character-level gates.
- Each rank costs one skill point.
- Normal maximum skill rank: 8.
- Lunging Combo and Rip and Rush maximum rank: 12.
- Maximum of eight distinct allocated skills.
- Prerequisites count toward this limit.
- Prerequisites remain usable after unlocking dependent skills.
- Ripping Aura and Fortitude Aura are mutually exclusive.
- Auras provide passive effects while assigned to a skill slot.
- Combat skill changes require the existing Apply flow while out of combat.

# 3. Global Combat Rules

## Damage terminology

All flat damage values use Minecraft damage points.

- 2 damage points = 1 heart.
- 20 health points = 10 hearts.
- Native Attack Damage means the player's effective Minecraft Attack Damage after equipment and attributes.
- Raw damage means damage before applicable defenses.

## Mitigation

Do not introduce a separate Covenant armor formula.

- Ordinary melee attacks use native Minecraft calculations.
- Armor and armor toughness remain native.
- Skill attacks use their explicitly configured damage sources and normal applicable mitigation.
- Armor shred modifies effective armor only, not toughness.
- Bleed bypasses armor, shield blocking, and Dodge.
- Bleed still respects general damage reduction, absorption, Fortitude shield, and actual invulnerability.
- Knockback respects native knockback resistance.

## Bosses

The default configuration introduces no blanket boss immunity.

Bosses can:
- Bleed.
- Be stunned.
- Be rooted or slowed.
- Receive armor shred.
- Be displaced where native knockback resistance permits.

Specific server datapacks may override these behaviors for particular encounters.

Do not silently reduce all crowd-control durations against bosses.

# 4. Rage Resource

## Recommended baseline

| Property | Value |
|---|---:|
| Starting Rage | 0 |
| Maximum Rage | 100 |
| Rage per damage point dealt | +1 |
| Rage per damage point received | +2 |
| Out-of-combat grace period | 7 seconds |
| Out-of-combat decay | 10 Rage/second |
| Passive in-combat generation | None |
| Rage from skill damage | None |

Rage generation should count confirmed damage after applicable defenses.

Qualifying damage includes:
- Ordinary melee attacks.
- Arrow and trident damage.
- Damage received from hostile enemies.

Exclude:
- Self-inflicted damage.
- Allied damage.
- Environmental damage.
- Skill damage as an outgoing Rage source.
- Bleed ticks as an outgoing Rage source.

Absorbed damage counts once toward Rage generation. Do not count both shield absorption and health damage.

## Rationale

The Reaver should begin encounters without a full resource bar.

Ordinary attacks are the reliable way to build Rage. Taking damage accelerates access to abilities, creating a risk/reward relationship.

Skills should not generate their own outgoing Rage, preventing self-sustaining ability loops.

The existing six-piece maximum Legendary Tier 5 Resource Cost benchmark can reduce costs by approximately 34.5%. A 40-Rage activation therefore costs approximately 26.2 Rage before rounding.

# 5. Shared Bleed System

Bleed is the Reaver's central long-term mechanic.

## Baseline values

| Property | Value |
|---|---:|
| Maximum stacks per target | 3 |
| Base damage per stack | 10% captured Attack Damage |
| Pulse interval | 1 second |
| Duration | 5 seconds |
| First pulse | 1 second after application |
| Reapplication | Refresh all stack durations |
| At stack cap | Replace oldest stack |

Each stack stores:
- Applying attacker.
- Captured base damage.
- Remaining duration.

### Stacking rules

- All players share the same three-stack cap per target.
- Reapplication refreshes existing stack durations.
- A new stack at the cap replaces the oldest stack.
- A weaker stack may replace a stronger one.
- Each stack retains its original attacker attribution.
- Each target uses one shared one-second pulse schedule.
- Adding stacks does not reset that schedule.
- Bleed persists through entity unloading and save/load.
- Timers pause while an entity is not ticking.
- Death clears all bleed stacks.

### Bleed defenses

Bleed bypasses:
- Armor.
- Shield blocking.
- Dodge.

Bleed still respects:
- General damage reduction.
- Absorption.
- Fortitude shield.
- Actual invulnerability.

Bleed is eligible on all otherwise-valid living targets, including undead, golems, bosses, and modded creatures.

There should be no creature-type immunity by default.

# 6. Skill Roster and Numerical Values

The following values are proposed Rank 1 baselines.

Existing prototype values are identified where known. New values are proposals requiring playtesting.

## 6.1 Cleave

**Unlock:** Level 1  
**Maximum rank:** 8  
**Prerequisites:** None

| Parameter | Rank 1 |
|---|---:|
| Rage cost | 20 |
| Cooldown | 3 seconds |
| Range | 3 blocks |
| Horizontal arc | 120 degrees |
| Damage | 100% native Attack Damage |
| Cast time | Instant |

A frontal area attack hitting eligible enemies within three blocks and a 120-degree horizontal arc.

- Uses the held weapon's effective Attack Damage.
- Requires line of sight.
- Empty attacks are paid whiffs.
- Fully defended hits still spend Rage.

**Purpose:** Reliable multi-target melee damage.

## 6.2 Rend

**Unlock:** Level 3  
**Maximum rank:** 8  
**Prerequisites:** None

| Parameter | Rank 1 |
|---|---:|
| Rage cost | 20 |
| Cooldown | 2 seconds |
| Initial damage | 100% native Attack Damage |
| Bleed damage | 10% captured Attack Damage/sec |
| Bleed duration | 5 seconds |
| Maximum stacks | 3 |

A single-target melee attack that applies one bleed stack after positive damage.

- No weapon-category restriction.
- Uses normal melee defenses.
- Uses crosshair targeting and native weapon reach.
- Missing or fully defended attacks do not apply bleed.

**Purpose:** Frequent bleed setup with competitive direct damage.

## 6.3 Battle Cry

**Unlock:** Level 3  
**Maximum rank:** 8  
**Prerequisites:** None

| Parameter | Rank 1 |
|---|---:|
| Rage cost | 30 |
| Cooldown | 30 seconds |
| Damage bonus | +15% |
| Duration | 6 seconds |
| Radius | 6 blocks |

Grants the Reaver and eligible nearby allies +15% outgoing damage.

- Applies to attacks, skills, and damage-over-time effects.
- Repeated copies do not stack in strength.
- Uses shared allied-recipient rules.

**Purpose:** Group offensive support and coordinated burst windows.

## 6.4 Rage Storm

**Unlock:** Level 5  
**Maximum rank:** 8  
**Prerequisites:** Cleave

| Parameter | Rank 1 |
|---|---:|
| Rage cost | 35 |
| Cooldown | 12 seconds |
| Wind-up | 0.8 seconds |
| Travel speed | 0.35 blocks/tick |
| Travel distance | 8 blocks |
| Area radius | 2 blocks |
| Damage per pulse | 3 |
| Pulse interval | 10 ticks |
| Maximum hits per target | 4 |

Releases a slowly travelling damaging area in the direction the Reaver faces.

- The direction is captured when the cast completes.
- The Reaver can move independently after release.
- Targets inside the area take repeated damage.
- Each target can be hit at most four times.

**Purpose:** Area pressure against clustered enemies.

## 6.5 Execute

**Unlock:** Level 5  
**Maximum rank:** 8  
**Prerequisites:** None

| Parameter | Rank 1 |
|---|---:|
| Rage cost | 30 |
| Cooldown | 8 seconds |
| Health threshold | 30% maximum health |
| Damage | 200% native Attack Damage |

Can only be activated against targets at or below 30% health.

- Uses normal melee defenses.
- Invalid targets or targets above the threshold cause no resource expenditure.
- Damage is calculated using the Reaver's effective Attack Damage.

**Purpose:** Conditional single-target finisher.

## 6.6 Blood Fever

**Unlock:** Level 5  
**Maximum rank:** 8  
**Prerequisites:** Rend

| Parameter | Rank 1 |
|---|---:|
| Rage cost | Passive |
| Detection radius | 4 blocks |
| Update interval | 5 ticks |
| Movement speed per enemy | +2% |
| Attack speed per enemy | +3% |
| Maximum enemies counted | 5 |
| Maximum movement bonus | +10% |
| Maximum attack speed bonus | +15% |

Counts distinct bleeding enemies within four blocks.

- Stack count does not increase the bonus.
- Bleeds applied by teammates count.
- Updates as targets enter, leave, bleed, stop bleeding, or die.
- Does not count pets or allies.

**Purpose:** Reward fighting in the middle of an active engagement.

## 6.7 Leap

**Unlock:** Level 10  
**Maximum rank:** 8  
**Prerequisites:** Cleave

| Parameter | Rank 1 |
|---|---:|
| Rage cost | 25 |
| Cooldown | 10 seconds |
| Maximum distance | 8 blocks |
| Impact radius | 3 blocks |
| Landing damage | 125% native Attack Damage |
| Movement time | 5 ticks |

Leaps toward the ground point under the player's aim.

- Does not require an enemy target.
- Deals area damage upon landing.
- Terrain and collision may shorten the movement.
- Invalid movement starts do not spend Rage.

**Purpose:** Gap closer and engagement initiator.

# 7. Mutually Exclusive Auras

The two auras represent distinct Reaver specializations.

Their passive effects remain active while assigned to a skill slot. Activating the aura key triggers a separate temporary effect.

Both use the shared six-block aura range and allied-recipient rules.

## 7.1 Ripping Aura

**Unlock:** Level 10  
**Maximum rank:** 8  
**Mutually exclusive with:** Fortitude Aura

| Parameter | Rank 1 |
|---|---:|
| Passive bleed bonus | +20% |
| Activation cost | 40 Rage |
| Cooldown | 20 seconds |
| Active duration | 5 seconds |
| Radius | 6 blocks |

### Passive

Grants the Reaver and eligible nearby allies +20% bleed damage.

- Applies to newly applied stacks.
- Damage is captured when the stack is created.
- Existing stacks do not retroactively gain the bonus.
- Boosted stacks retain their damage after leaving aura coverage.

### Active

Eligible recipients' ordinary melee attacks and melee skills apply bleed after positive damage.

- One aura-triggered application per attacker/target pair per second.
- Projectiles do not trigger it.
- Rend does not generate a duplicate application.
- Uses the shared three-stack cap.

**Purpose:** Bleed specialization and group melee synergy.

## 7.2 Fortitude Aura

**Unlock:** Level 10  
**Maximum rank:** 8  
**Mutually exclusive with:** Ripping Aura

| Parameter | Rank 1 |
|---|---:|
| Passive Armor | +4 |
| Passive Armor Toughness | +2 |
| Activation cost | 40 Rage |
| Cooldown | 20 seconds |
| Shield capacity | 6 damage points |
| Shield duration | 7 seconds |
| Radius | 6 blocks |

### Passive

Grants +4 Armor and +2 Armor Toughness to eligible nearby allies.

Uses native flat attribute modifiers.

### Active

Grants each eligible recipient a separate six-point Fortitude shield.

- Shield damage is resolved after armor and other applicable reductions.
- Fortitude is consumed before ordinary absorption and health.
- A new activation replaces the previous Fortitude shield.
- Capacity does not stack.
- Entering aura range after activation does not grant the already-cast shield.

**Purpose:** Defensive frontline specialization.

# 8. Advanced Skills

## 8.1 Lunging Combo

**Unlock:** Level 15  
**Maximum rank:** 12  
**Prerequisites:** None

| Parameter | Rank 1 |
|---|---:|
| Stage 1 Rage cost | 20 |
| Stage 2 Rage cost | 10 |
| Stage 3 Rage cost | 20 |
| Total cost | 50 |
| Lunge distance | 4 blocks |
| Lunge duration | 3 ticks |
| Stage 2 damage | 125% Attack Damage |
| Stage 2 follow-up window | 2 seconds |
| Stage 3 damage | 100% Attack Damage |
| Cleave area | 3 blocks, 120-degree arc |
| Armor shred | 20% |
| Shred duration | 5 seconds |
| Shared cooldown | 12 seconds |

Three-stage combo:

1. **Lunge:** Movement-only lunge four blocks over three ticks.
2. **Strike:** Single-target strike dealing 125% Attack Damage.
3. **Cleave:** Frontal cleave dealing 100% Attack Damage per target and applying 20% armor shred.

Armor shred:
- Leaves armor toughness unchanged.
- Applies after positive damage.
- Lasts five seconds at Rank 1.
- Uses one shared target-owned debuff.
- Reapplication does not increase strength.
- Expiry becomes the later of existing and new expiry.

Each stage spends its own Rage cost.

**Purpose:** A skill-expression combo with mobility, focused damage, and a short armor-shred window.

## 8.2 Bloody Cloak

**Unlock:** Level 15  
**Maximum rank:** 8  
**Prerequisites:** Rend

| Parameter | Rank 1 |
|---|---:|
| Rage cost | 30 |
| Cooldown | 30 seconds |
| Duration | 6 seconds |
| Healing conversion | 20% of personally attributed bleed damage |

Activating Bloody Cloak clears the Reaver's existing Wither, ignite/burning, and poison effects.

While active:
- Prevents those effects from being reapplied.
- Heals the Reaver for 20% of actual bleed damage personally dealt.
- Does not heal from teammate-attributed bleeds.
- Healing cannot exceed missing health.

**Purpose:** Sustain and status protection for bleed builds.

## 8.3 Overpower

**Unlock:** Level 20  
**Maximum rank:** 8  
**Prerequisites:** Cleave

| Parameter | Rank 1 |
|---|---:|
| Rage cost | 30 |
| Cooldown | 8 seconds |
| Damage against bleeding targets | 125% Attack Damage |
| Damage against non-bleeding targets | 200% Attack Damage |

A focused single-target melee attack.

- Bleeding status is checked when the attack is accepted.
- Does not consume bleed stacks.
- Uses native melee defenses.

**Purpose:** Direct-damage alternative to bleed-dependent skills.

## 8.4 Consume

**Unlock:** Level 20  
**Maximum rank:** 8  
**Prerequisites:** Rend

| Parameter | Rank 1 |
|---|---:|
| Rage cost | 35 |
| Cooldown | 12 seconds |
| Radius | 4 blocks |
| Damage per removed stack | 4 |

Removes all bleed stacks from eligible enemies in the area.

- Deals four damage points per removed stack.
- Can consume stacks applied by other players.
- Damage is attributed to the Reaver.
- Uses the configured damage source and normal applicable mitigation.

**Purpose:** Convert sustained bleed pressure into immediate area burst.

## 8.5 Enduring Cry

**Unlock:** Level 20  
**Maximum rank:** 8  
**Prerequisites:** Battle Cry

| Parameter | Rank 1 |
|---|---:|
| Rage cost | 30 |
| Cooldown | 30 seconds |
| Damage reduction | 20% |
| Duration | 5 seconds |
| Radius | 6 blocks |

Grants the Reaver and eligible nearby allies 20% damage reduction.

Repeated copies do not stack in strength.

**Purpose:** Timed group defensive utility.

## 8.6 Skull Bash

**Unlock:** Level 25  
**Maximum rank:** 8  
**Prerequisites:** Overpower

| Parameter | Rank 1 |
|---|---:|
| Rage cost | 25 |
| Cooldown | 12 seconds |
| Damage | 75% Attack Damage |
| Stun duration | 1 second |
| Range | Native melee reach |

A focused melee strike that stuns after positive damage.

- Stun prevents movement and attacks.
- Reapplication refreshes rather than adds duration.
- Native effect applicability and immunity checks remain in force.
- Damage still resolves even if the stun is rejected.

**Purpose:** Short interrupt and dangerous-enemy control.

## 8.7 Raging Aura

**Unlock:** Level 25  
**Maximum rank:** 8  
**Prerequisites:** Battle Cry

| Parameter | Rank 1 |
|---|---:|
| Passive Rage decay | 3/second |
| Activation cost | 40 Rage |
| Cooldown | 30 seconds |
| Damage bonus | +20% |
| Duration | 6 seconds |

### Passive

While assigned, reduces out-of-combat Rage decay from 10 to 3 per second.

### Active

Grants the Reaver +20% damage for six seconds.

- Applies to ordinary attacks, skills, and personal bleed damage.
- Does not affect pet damage.
- Stops passive Rage decay while active.
- Bleed stacks capture the bonus when applied.
- Existing bleed stacks do not gain the bonus retroactively.

Ripping Aura and Raging Aura combine additively for +40% bleed damage, not multiplicatively.

**Purpose:** Personal burst specialization and resource retention.

## 8.8 War Banner

**Unlock:** Level 25  
**Maximum rank:** 8  
**Prerequisites:** Enduring Cry

| Parameter | Rank 1 |
|---|---:|
| Rage cost | 35 |
| Cooldown | 35 seconds |
| Banner lifetime | 12 seconds |
| Radius | 6 blocks |
| Regeneration | 1 health point/second |

Plants a stationary banner that provides nearby allies:

- One health point of regeneration per second.
- Prevention of exhaustion-driven hunger loss while in range.

It does not refill hunger or saturation.

**Purpose:** Area sustain and group defensive utility.

## 8.9 Rip and Rush

**Unlock:** Level 30  
**Maximum rank:** 12  
**Prerequisites:** Lunging Combo

| Parameter | Rank 1 |
|---|---:|
| Stage 1 Rage cost | 30 |
| Stage 2 Rage cost | 20 |
| Stage 3 Rage cost | 20 |
| Total cost | 70 |
| Stage 1 radius | 3 blocks |
| Stage 1 damage | 100% Attack Damage |
| Stage 1 bleed | 10% Attack Damage/sec |
| Stage 2 dash | 6 blocks |
| Stage 2 duration | 4 ticks |
| Stage 2 damage | 50% Attack Damage |
| Stage 2 armor shred | 20% for 5 seconds |
| Stage 3 radius | 3 blocks |
| Stage 3 damage | 100% Attack Damage |
| Speed per qualifying enemy | +10% |
| Maximum speed bonus | +30% |
| Speed duration | 4 seconds |
| Shared cooldown | 20 seconds |

Three-stage combo:

1. **Opening Rend:** Applies Rend-style damage and bleed to enemies within three blocks.
2. **Dash:** Dashes six blocks, damaging contacted enemies and applying shared armor shred.
3. **Consume Finisher:** Damages enemies within three blocks and consumes their bleed stacks.

For every enemy whose bleed is successfully consumed, grant +10% movement speed, up to +30%.

- Counts each target once.
- Bleeds applied by other players qualify.
- Fully blocked or dodged hits preserve bleeds.
- Speed lasts four seconds.
- New qualifying finishers replace the existing speed buff rather than stacking.

**Purpose:** The Reaver's most demanding bleed-specialist combo. Powerful against groups, less efficient against isolated enemies.

## 8.10 Guillotine

**Unlock:** Level 30  
**Maximum rank:** 8  
**Prerequisites:** Consume

| Parameter | Rank 1 |
|---|---:|
| Rage cost | 30 |
| Cooldown | 10 seconds |
| Damage per consumed stack | 6 |
| Range | Native melee reach |

A single-target melee attack that consumes all bleed stacks after positive damage.

Deals six damage points per removed stack.

- Can consume teammate-applied stacks.
- Fully blocked, dodged, or invulnerable attacks do not consume bleed.
- Consumption damage is attributed to Guillotine's caster.

**Purpose:** Focused single-target bleed conversion.

## 8.11 Demon Mode

**Unlock:** Level 30  
**Maximum rank:** 8  
**Prerequisites:** Raging Aura

| Parameter | Rank 1 |
|---|---:|
| Rage cost | 50 |
| Cooldown | 45 seconds |
| Duration | 6 seconds |
| Teleport strike range | 5 blocks |
| Healing per damaging melee strike | 2 health points |
| Healing per attributed bleed pulse | 1 health point |

Activates a high-commitment combat mode.

While active:
- Eligible attacks against targets within five blocks teleport the Reaver to a valid adjacent position.
- Damaging melee strikes heal two health points.
- Personally attributed bleed pulses heal one health point.
- Healing is based on actual damage dealt.
- Healing cannot exceed missing health.
- Teammate-attributed bleeds do not provide healing.

**Purpose:** High-risk pursuit mode with personal sustain.

# 9. Skill Rank Progression

Rank 1 should provide the complete functional version of each skill.

Ranks 2–8 should offer modest, skill-specific improvements rather than routine damage inflation.

Recommended upgrade directions:

| Skill | Rank progression |
|---|---|
| Cleave | Sweep handling and arc improvements |
| Rend | Bleed duration or application handling |
| Battle Cry | Duration or coverage |
| Rage Storm | Travel distance or area control |
| Execute | Threshold or cost efficiency |
| Blood Fever | Detection radius or responsiveness |
| Leap | Landing control or displacement |
| Ripping Aura | Activation duration or coverage |
| Fortitude Aura | Activation duration or coverage |
| Lunging Combo | Follow-up windows and handling |
| Bloody Cloak | Duration or healing conversion |
| Overpower | Cost efficiency |
| Consume | Radius or cost efficiency |
| Enduring Cry | Duration or coverage |
| Skull Bash | Cooldown or hit reliability |
| Raging Aura | Activation duration |
| War Banner | Lifetime or coverage |
| Rip and Rush | Follow-up windows and handling |
| Guillotine | Cost efficiency |
| Demon Mode | Duration or healing reliability |

## Aura rank boundaries

Keep passive aura strengths fixed across all ranks:

- Ripping Aura: +20% bleed damage.
- Fortitude Aura: +4 Armor and +2 Armor Toughness.
- Raging Aura: 3 Rage/second passive decay.

Ranking should primarily improve activation duration or coverage.

This prevents passive stats from scaling indefinitely alongside equipment.

# 10. Equipment Scaling

Use the existing equipment modifier system.

Relevant modifiers include:

- Resource Cost.
- Cooldown.
- Cast Time.
- Skill Duration.
- Movement Speed.
- Attack Speed.
- Attack Damage.
- Armor.
- Health.

Do not introduce a generic skill-damage affix solely for the Reaver.

## Native weapon scaling

Use native Attack Damage for:

- Cleave.
- Rend.
- Lunging Combo.
- Rip and Rush.

Bleed stacks capture the applying attacker's effective Attack Damage when applied.

Changing weapons or equipment does not retroactively change existing stacks.

Flat-damage skills retain their configured values unless explicitly designed otherwise.

## Equipment benchmark

The current maximum Legendary Tier 5 equipment benchmark:

| Modifier | Maximum combined bonus |
|---|---:|
| Resource Cost | -34.5% |
| Cooldown | -34.5% |
| Cast Time | -69% |
| Skill Duration | +34.5% |
| Movement Speed | +34.5% |

Test the Reaver against this ceiling.

Pay particular attention to:
- Rage cost reduction on Rip and Rush.
- Cooldown reduction on short-cooldown attacks.
- Duration scaling on bleed and armor shred.
- Attack Damage scaling on Rend.
- Aura uptime and group buffs.

Implement optional per-skill minimums for effective Rage cost, cooldown, and cast time where appropriate.

Do not impose universal floors on instant skills or combo follow-up windows unless explicitly intended.

# 11. Intended Reaver Builds

The Reaver should support several viable specializations.

These are conceptual builds, not finalized eight-skill loadouts. Prerequisites count toward the skill limit.

## Bleed Engine

Core skills:
- Rend.
- Blood Fever.
- Ripping Aura.
- Consume.
- Rip and Rush.
- Bloody Cloak.

Establish bleeds, amplify them, convert them into burst, and heal through personally attributed bleed damage.

**Strength:** Excellent sustained pressure and group synergy.

**Weakness:** Requires setup and forces a decision between preserving bleeds and consuming them.

## Executioner

Core skills:
- Cleave.
- Overpower.
- Execute.
- Skull Bash.
- Battle Cry.
- Raging Aura.

Focuses on direct damage without requiring bleed setup.

**Strength:** Reliable direct damage and target priority.

**Weakness:** Less sustained value against durable enemies than a developed bleed build.

## Vanguard

Core skills:
- Cleave.
- Leap.
- Fortitude Aura.
- Enduring Cry.
- War Banner.
- Lunging Combo.

A defensive frontline build focused on positioning, armor, shields, and group utility.

**Strength:** Group durability and frontline support.

**Weakness:** Lower personal damage amplification.

## Blood Berserker

Core skills:
- Rend.
- Blood Fever.
- Raging Aura.
- Demon Mode.
- Rip and Rush.
- Guillotine.

Combines personal burst, bleed pressure, mobility, and healing.

**Strength:** High commitment and close-range momentum.

**Weakness:** High Rage demand and significant exposure during engagement.

## Group Breaker

Core skills:
- Battle Cry.
- Ripping Aura.
- Rage Storm.
- Consume.
- Enduring Cry.
- War Banner.

Focuses on team-wide offensive and defensive effects.

**Strength:** High group contribution.

**Weakness:** Less self-sufficient when separated from allies.

# 12. Stacking and Interaction Rules

| Effect | Default behavior |
|---|---|
| Bleed stacks | Shared three-stack cap |
| Bleed refresh | Refresh all stack durations |
| Bleed attribution | Per-stack attacker |
| Ripping Aura passive | Captured at application |
| Ripping Aura active | One application per attacker/target per second |
| Raging Aura | Captured on new bleed stacks |
| Ripping + Raging | Additive +40% |
| Consume | Removes all stacks |
| Guillotine | Removes all stacks |
| Blood Fever | Counts bleeding targets, not stacks |
| Armor shred | Shared 20% target-owned debuff |
| Armor shred refresh | Extends expiry without increasing strength |
| Battle Cry | Does not stack in strength |
| Enduring Cry | Does not stack in strength |
| Fortitude shield | Replaced by new activation |
| Ripping/Fortitude | Mutually exclusive |
| Raging Aura | Self-only |
| Rip and Rush speed | Replaced by new qualifying finisher |
| Demon Mode healing | Personally attributed damage only |

Every future effect should explicitly define whether it stacks by strength, duration, source, target, or not at all.

# 13. Server Datapack Configuration

The built-in Reaver should use vanilla-oriented defaults.

Server datapacks should be able to override balance-critical values without modifying Covenant source code.

## Configurable resource values

- Starting Rage.
- Maximum Rage.
- Damage-to-Rage coefficients.
- Combat grace period.
- Out-of-combat decay.
- Raging Aura passive decay.
- Exclusion rules for self, allied, environmental, and skill damage.

## Configurable skill values

- Unlock levels.
- Prerequisites.
- Maximum ranks.
- Rage costs.
- Cooldowns.
- Cast times.
- Wind-up and follow-up windows.
- Damage amounts and Attack Damage coefficients.
- Range, arc, and radius.
- Travel distance and speed.
- Pulse intervals.
- Bleed damage, duration, and stack limits.
- Armor shred strength and duration.
- Stun, root, and slow durations.
- Aura range and strength.
- Shield capacity.
- Healing conversion.
- Per-rank effects.

## Shared effect configuration

- Bleed stacking policy.
- Bleed refresh and replacement behavior.
- Bleed attribution.
- Armor-shred stacking and refresh.
- Aura recipient eligibility.
- Entity tags and optional overrides.
- Damage-source behavior.

## Recommended structure

Conceptually separate:

- Reaver class and resource defaults.
- Individual skill definitions.
- Per-rank values.
- Shared bleed definitions.
- Shared armor-shred definitions.
- Shared aura behavior.
- Optional entity-tag overrides.

The exact resource paths and JSON schema should follow the implementation contract.

## Validation requirements

- Server datapacks are authoritative.
- Built-in defaults apply when no override exists.
- Invalid entries fail safely.
- Numeric values are validated.
- Negative costs and invalid durations are rejected.
- Reloading definitions does not erase player progression or allocations.
- Existing effects follow documented snapshot and lifecycle rules.
- New activations use newly loaded values.

Do not expose every internal implementation detail as a configurable field without a demonstrated need.

# 14. Playtesting Requirements

Do not declare the Reaver balanced from spreadsheet estimates alone.

## Vanilla baseline tests

1. Opening encounter: Does Rage build at a satisfying rate?
2. Resource cycle: Can the Reaver use several skills without chaining everything indefinitely?
3. Bleed single target: Does Rend produce meaningful sustained damage?
4. Bleed group: Do Ripping Aura and Blood Fever create worthwhile synergies?
5. Direct damage: Is Overpower viable without bleed setup?
6. Cleave: Is the arc and range appropriate?
7. Combo execution: Are follow-up windows forgiving but meaningful?
8. Armor shred: Does it make a noticeable difference against armored targets?
9. Aura choice: Are Ripping and Fortitude both useful?
10. Group support: Are Battle Cry and Enduring Cry worthwhile?
11. Boss testing: Can bosses bleed and receive control effects by default?
12. Endgame equipment: Does maximum modifier investment create excessive uptime?
13. Multiplayer: Are bleed caps, attribution, and cross-player consumption consistent?

## Modpack compatibility tests

Test against:

- Very high Attack Damage weapons.
- Fast and slow weapons.
- High-health enemies.
- High-armor enemies.
- Bosses with native knockback resistance.
- Unusual entity hitboxes.
- Additional damage types.
- Additional dimensions.
- Modded immunity systems.

The goal is not to make one default configuration perfectly balanced for every modpack.

The goal is predictable mechanics and enough datapack control to retune the class.

# 15. Values Requiring Playtesting

The following proposed values require particular attention:

1. Battle Cry's +15% damage and six-second duration.
2. Rage Storm's wind-up, travel speed, and pulse damage.
3. Execute's 30% health threshold.
4. Blood Fever's per-enemy bonuses.
5. Leap's range and landing damage.
6. Bloody Cloak's healing conversion.
7. Overpower's damage split.
8. Consume's per-stack damage.
9. Enduring Cry's 20% damage reduction.
10. Skull Bash's one-second stun.
11. War Banner's regeneration.
12. Guillotine's six damage per consumed stack.
13. Demon Mode's teleport and healing behavior.
14. Exact rank progression.
15. Whether particular modpacks require explicit boss overrides.

# 16. Final Design Position

The Reaver should be a vanilla-compatible melee class with a configurable ceiling.

In vanilla-oriented worlds:
- Weapons retain native damage.
- Armor and toughness remain meaningful.
- Rage comes from actual combat.
- Skills create openings rather than replacing ordinary attacks.
- Bleed creates sustained pressure.
- Combos reward timing and positioning.

In heavily modded worlds:
- Rage generation can be retuned.
- Skill values can be changed.
- Bleed mechanics can be adjusted.
- Armor shred can be adapted.
- Boss interactions can be overridden.
- Equipment scaling can be accommodated.

A fully invested Reaver should feel powerful through specialization and combinations.

The class should support:
- Bleed setup followed by burst conversion.
- Aggressive Rage generation and spending.
- Coordinated melee damage through Ripping Aura.
- Defensive frontline play through Fortitude Aura.
- Armor shred and team follow-up.
- High-commitment burst through Raging Aura and Demon Mode.
- A direct-damage route that remains effective without bleed setup.

The Reaver should not require custom armor math, automatic enemy-health scaling, or blanket boss exceptions to achieve that power.

**Final recommendation:** Adopt this as the Reaver's first complete numerical baseline. Preserve the existing Rage and bleed architecture, retain vanilla mitigation, expose balance-critical values through server datapacks, and tune the proposed numbers through actual combat testing before treating them as final.