# Project Covenant: Stalker Class Balance Proposal

**Status:** Numerical baseline accepted on 2026-11-06; Disengage, Arcane Arrow and Multishot implemented at Rank 1; broader implementation and playtesting pending  
**Scope:** Stalker class, Focus economy, skill values, rank progression, equipment interactions, and server-datapack balance configuration  
**Design target:** Vanilla Minecraft by default; configurable for modpacks without hardcoded assumptions about modded content

The proposed numerical values in this document are accepted unchanged as the
starting balance baseline. They remain provisional for playtesting. Acceptance
does not mean the values or the proposed configuration surface are implemented;
current shipped behavior is documented separately in `docs/RPG_SKILLS.md`.
Where this baseline differs from existing prototype tuning, it defines the
intended future values. In particular, Piercing Shot is intended to change from
30 Focus / 2 seconds to 20 Focus / 3 seconds.

The selected Stalker skill slice is **Disengage, Arcane Arrow, and
Multishot**, covering escape/repositioning, shared damage-window setup, and
spread-group damage. These three now have gameplay at the numerical baselines
below. This first implementation makes only Rank 1 purchasable for all three
skills, retaining Rank 8 as their planned maximum. The user scoped this batch
to adding the three new skills: server-datapack skill tuning and conversion
of existing Stalker skills remain separate work. Detailed implementation
boundaries, persistence and acceptance checks are in `docs/RPG_SKILLS.md`.
Rank themes in section 6.3 remain directions rather than authored per-rank tables;
Ranks 2–8 are deferred until the complete Rank 1 mechanics can be playtested.

---

## 1. Executive summary

The Stalker is a ranged combat class built around bows, precision, mobility, traps, and battlefield control. It should feel like an extension of Minecraft combat rather than a replacement for it.

The recommended approach is:

1. **Ship conservative, vanilla-oriented defaults.** The default values should work against ordinary vanilla enemies and familiar Minecraft armor, weapons, arrows, status effects, and damage rules.
2. **Make balance values data-driven.** Server owners should be able to retune skills, resource costs, cooldowns, durations, rank effects, enemy progression, and relevant interaction rules through server datapacks.
3. **Keep the combat model native.** Use vanilla projectile behavior, damage sources, armor/toughness calculations, shields, fire resistance, knockback resistance, and other applicable vanilla mechanics wherever they fit. Do not introduce a second global damage or armor formula.
4. **Allow high-power builds through combinations.** A fully developed Stalker should be able to create strong synergies between control, damage-over-time, vulnerability, summons, and precision attacks. Individual abilities should remain understandable and useful without requiring a combo.
5. **Do not hardcode assumptions about enemy health or modpack progression.** Vanilla defaults are the reference point, not a universal ceiling. Modpacks can change enemy health, armor, dimensions, equipment, and encounter difficulty independently of Covenant.
6. **Keep bosses mechanically relevant.** The default rules should not grant bosses blanket immunity or resistance to Stalker control effects. If a particular pack needs boss-specific rules, it can configure them explicitly.

The numerical values below are proposed starting defaults, not claims of playtested balance. They should be treated as one coherent first-pass tuning set and adjusted through in-game testing.

---

## 2. Design identity and constraints

### 2.1 Class identity

- **Class:** Stalker
- **Resource:** Focus
- **Primary combat style:** Bow-focused ranged combat, precision, mobility, traps, and maintaining distance.
- **Strengths:** Reliable ranged damage, target selection, area denial, enemy control, and strong ability combinations.
- **Weaknesses:** Less comfortable when surrounded, dependent on positioning, and less effective when forced to fight at close range or when enemies can bypass controlled areas.

Classes do not impose rigid equipment restrictions. Vanilla and modded equipment should remain usable where mechanically appropriate.

### 2.2 Existing progression rules to preserve

- Character level is capped at 60.
- Character level gates the **first point** invested in a skill. Once unlocked at Rank 1, additional ranks do not require further character-level gates.
- Each rank costs one skill point.
- Skills have individual maximum ranks of 8 or 12. The currently playable Stalker skills use Rank 8.
- A player may have no more than eight distinct skills with points allocated at once. Prerequisites count toward this limit.
- Prerequisite skills remain usable after learning their dependent skills.
- Auras provide their passive effect while assigned to a skill slot. Activating the aura key triggers a separate temporary effect.
- Combat skill changes are committed through the existing Apply flow while out of combat.
- Combat respec remains a separate progression item and process.
- Profession abilities do not consume combat skill slots.

### 2.3 Vanilla compatibility and modpack flexibility

The intended relationship is **vanilla defaults plus server-owned configuration**, not separate vanilla and modded combat systems.

Covenant should not attempt to infer the correct skill damage from an arbitrary modpack's enemy health values. There is no reliable universal way to normalize every modded entity, armor system, damage modifier, or encounter mechanic.

Instead:

- The built-in datapack defines a sensible vanilla-oriented baseline.
- Server datapacks can override numerical skill values and relevant rules.
- Modded entities participate through the same targeting and damage pathways when compatible.
- Modded damage systems can be accommodated through explicit damage-type choices or optional integrations where necessary.
- A modpack can tune Covenant's skills to its own equipment and enemy ecosystem without forking the mod.

A datapack should be able to change values without requiring a code change or a Covenant rebuild. Exact JSON paths and field names should be finalized alongside the implementation contract; this proposal defines the required configuration surface, not a premature schema.

---

## 3. Global combat and balance assumptions

### 3.1 Damage units

All damage values in this proposal use Minecraft damage points:

- 2 damage points = 1 heart.
- 20 health points = 10 hearts, the ordinary vanilla player health pool.
- Values described as **raw damage** are before the target's applicable defenses.
- Values described as an **arrow damage coefficient** multiply the arrow's native damage calculation. They do not represent a fixed number of health points.

### 3.2 Native damage and mitigation

Do not add a Covenant-wide armor formula or universal damage reduction layer.

Use the applicable native Minecraft damage pipeline:

- Arrow attacks should use native projectile behavior and native arrow damage calculation.
- Physical damage should interact with armor and toughness as native damage does.
- Explosion damage should use native explosion mitigation, including applicable Blast Protection.
- Fire damage should respect Fire Resistance and native fire immunity.
- Dodge and shield interactions should follow the skill-specific rules below.
- Knockback should respect native knockback resistance and terrain.
- Invulnerability frames and other native damage handling should remain intact unless a skill explicitly requires a different behavior.

A skill's damage type is part of its definition and should be configurable where the implementation supports it. Do not silently turn all Stalker abilities into one custom damage type.

### 3.3 Bosses and crowd control

The default Covenant rules currently have no boss-wide immunities or resistances. Preserve that.

- Bosses receive the normal duration and strength of Bear Trap root and Frost Trap slow by default.
- Bosses are not automatically immune to poison, vulnerability, knockback, or other Stalker effects.
- A boss's own native attributes and behavior may still affect the result. For example, native knockback resistance can reduce Blast Trap displacement.
- Server datapacks may add explicit boss or entity-tag overrides when a particular modpack or encounter needs them.
- Any such override should be targeted and visible in configuration, not an implicit Covenant rule.

This keeps bosses susceptible to builds and coordination while allowing modpacks to tune exceptional encounters.

### 3.4 Enemy health and progression

Do not scale Stalker damage automatically according to dimension, enemy level, or maximum health.

Covenant's enemy progression design already intends difficulty to come from region, enemy role, and encounter context rather than universal player-level scaling. Stalker values should therefore be tested against reference enemy groups:

| Test group | Purpose |
|---|---|
| Ordinary vanilla hostile mobs | Confirm basic skills remain useful and do not trivialize normal encounters |
| Armored vanilla enemies | Confirm native armor and damage types behave as expected |
| Nether enemies | Check fire immunity/resistance and higher threat |
| End enemies | Check ranged pressure and late-game survivability |
| Elite/champion enemies | Check whether control and damage-over-time remain useful |
| Bosses | Check combo strength, duration, uptime, and player counterplay |
| Modded enemy samples | Verify compatibility and identify datapack tuning needs |

The reference test set is a balance tool, not a hardcoded list of eligible targets.

---

## 4. Focus resource

### 4.1 Default resource rules

| Property | Default |
|---|---:|
| Starting Focus | 100 |
| Maximum Focus | 100 |
| Base regeneration | 25 Focus/second |
| Regeneration while channeling Barrage | Paused |
| Capacity from equipment | Not supported |
| Regeneration bonus cap | +25% over configured base |

Focus starts full, has a hard maximum of 100, and recovers at 25 per second by default. Equipment cannot increase Focus capacity. Regeneration modifiers are capped at 25% above the configured base.

Barrage pauses Focus regeneration for the duration of its channel. Regeneration resumes when the channel ends.

### 4.2 Resource-cost modifiers

Use the existing Resource Cost coefficient and equipment modifier pipeline. Cost reductions should add through the existing modifier semantics rather than compound independently per item.

The current equipment benchmark permits up to 34.5% lower skill costs from six maximum Legendary Tier 5 Resource Cost affixes. At that benchmark, the cost coefficient is 0.655 before other modifiers.

The server should validate the final cost and available Focus before activation. A rejected activation spends no Focus and starts no cooldown.

### 4.3 Focus cost design target

- Low-cost utility and basic control: 15–25 Focus.
- Core offensive skills: 20–35 Focus.
- Strong control or specialized attacks: 30–40 Focus.
- Major area or burst skills: 40–50 Focus.
- Sustained channels: spend Focus per projectile or tick, not as one up-front cost.

With 25 Focus/second regeneration, a 20-Focus skill represents less than one second of base regeneration, while a 50-Focus skill represents two seconds. Cooldowns, positioning, and opportunity cost should therefore do much of the balancing work.

---

## 5. Skill roster and proposed Rank 1 values

The skill unlock levels and prerequisite relationships below preserve the current Stalker tree. Values marked **proposed** are new starting values for the balance pass. Existing prototype values are identified separately.

### 5.1 Core attacks and mobility

#### Piercing Shot

**Minimum character level:** 1  
**Maximum rank:** 8  
**Prerequisites:** None

**Role:** Basic ranged attack that rewards lining up enemies.

| Parameter | Rank 1 default |
|---|---:|
| Focus cost | 20 |
| Cooldown | 3 seconds (proposed) |
| Arrow count | 1 |
| Maximum targets pierced | 3 |
| Damage coefficient | 3 (existing prototype) |
| Cast time | Instant |

- Fires one bow arrow at full speed.
- Uses native arrow piercing and collision handling.
- Can hit up to three eligible targets in a line.
- Stops at blocks.
- The damage coefficient of 3 is 50% above the vanilla arrow coefficient of 2.
- The held bow and ammunition remain relevant to the native arrow calculation.

**Balance intent:** This is the Stalker's reliable baseline skill. Its main advantage is hitting multiple aligned enemies, not replacing every other single-target option.

#### Disengage

**Minimum character level:** 3  
**Maximum rank:** 8  
**Prerequisites:** None

**Role:** Create distance while continuing to face the threat.

| Parameter | Rank 1 default |
|---|---:|
| Focus cost | 20 (proposed) |
| Cooldown | 6 seconds (proposed) |
| Backward displacement target | 4 blocks (proposed) |
| Travel time | 0.3 seconds (6 ticks; accepted provisional tuning) |
| Upward arc height | Approximately 0.5 blocks (accepted provisional tuning) |
| Damage | None |
| Cast time | Instant |

- Leaps backward using the player's horizontal facing direction captured on
  activation, ignoring camera pitch, with a small upward hop. The accepted
  provisional travel profile is four horizontal blocks over six ticks, with
  an approximately half-block upward arc in unobstructed travel. Terrain and
  collision may shorten or flatten the leap.
- Can activate while airborne, including after jumping or enemy knockback;
  standing on the ground is not required.
- Breaks an existing fall: clear accumulated fall distance during the leap,
  including on completion or interruption, then resume normal falling. This
  follows the existing Reaver dash fall-break behavior; a subsequent fall can
  still deal normal fall damage.
- Does not require a target.
- Preserves the player's facing direction.
- Actual displacement is subject to terrain and collision.
- Bow attacks and skills that do not move the player remain usable during the
  leap. Disengage grants no invulnerability; incoming attacks can still hit
  normally.

**Balance intent:** A dependable escape and repositioning tool, not a damage skill or an unconditional teleport.

#### Bear Trap

**Minimum character level:** 3  
**Maximum rank:** 8  
**Prerequisites:** None

**Role:** Single-target root and setup.

| Parameter | Rank 1 default |
|---|---:|
| Focus cost | 20 (existing prototype) |
| Cooldown | 8 seconds (existing prototype) |
| Damage | 4 |
| Root duration | 3 seconds |
| Arming delay | 0.5 seconds |
| Untriggered lifetime | 30 seconds |
| Throw distance | Approximately 3 blocks |

- Throws a single-use trap a short distance in front of the player.
- Roots the first eligible enemy to step on it.
- Root prevents self-directed walking and jumping, but does not prevent turning, attacking, or casting.
- Gravity and knockback still apply.
- Taking damage does not break the root.
- Reapplying the root refreshes it to the longer of the remaining duration or the new duration; durations do not add.
- Root applies only after the trap deals positive damage to health or absorption.
- Root duration is affected by the existing Skill Duration coefficient.
- Capture the owner's effective duration modifier when the trap is thrown.
- The trap's damage is independent of the held weapon's Attack Damage stat.

**Balance intent:** Strong single-target control without being a stun or removing the enemy's ability to retaliate.

#### Aimed Shot

**Minimum character level:** 5  
**Maximum rank:** 8  
**Prerequisites:** Piercing Shot

**Role:** Concentrated single-target arrow damage.

| Parameter | Rank 1 default |
|---|---:|
| Focus cost | 40 (existing prototype) |
| Cooldown | 4 seconds (existing prototype) |
| Wind-up | 1 second (existing prototype) |
| Damage coefficient | 4 (existing prototype) |
| Movement while winding up | 20% of normal movement speed |

- Fires one stronger arrow at a single target.
- Aim and movement remain available during the wind-up.
- Movement is slowed to 20% of normal speed; moving does not cancel the wind-up.
- Removing the bow or ammunition, unlearning the skill, or another defined lifecycle interruption cancels the cast without spending Focus.
- Cost and cooldown begin when the arrow fires.

**Balance intent:** A reliable burst option that trades time and mobility for concentrated damage. Its damage coefficient is twice vanilla's arrow coefficient.

#### Hunter's Pet

**Minimum character level:** 5  
**Maximum rank:** 8  
**Prerequisites:** None

**Role:** Persistent companion, distraction, and supplemental damage.

| Parameter | Rank 1 default |
|---|---:|
| Focus cost | 35 (proposed) |
| Defeat cooldown | 30 seconds (proposed) |
| Active companion limit | 1 |
| Pet health | 40 (proposed) |
| Pet attack damage | 3 (proposed) |
| Pet attack interval | 1.5 seconds (proposed) |

- Summons a fixed, skill-defined companion.
- One companion follows the player and assists in combat.
- It attacks enemies the player damages and retaliates against enemies that damage the player.
- The companion remains until defeated or dismissed.
- Defeat starts the summon cooldown; once it expires, the pet can be summoned again.
- Pet damage and health should be separately configurable from player damage.

**Balance intent:** The pet contributes useful pressure and distraction but does not become the player's primary damage source. Its survival is part of managing the skill.

#### Barrage

**Minimum character level:** 5  
**Maximum rank:** 8  
**Prerequisites:** None

**Role:** Sustained, manually aimed arrow pressure.

| Parameter | Rank 1 default |
|---|---:|
| Focus cost | 6 per arrow (proposed) |
| Firing rate | 3 arrows/second (proposed) |
| Damage coefficient | 2.0 per arrow (proposed) |
| Separate skill cooldown | None (agreed) |
| Fixed channel duration | None |
| Focus regeneration while channeling | Paused |

- Fires a rapid sequence of arrows while the skill key is held and sufficient Focus remains.
- Aim can be adjusted throughout the channel.
- Releasing the key or lacking sufficient Focus stops firing.
- Each arrow spends Focus individually when it fires.
- Movement remains possible at normal bow-draw speed.
- Focus regeneration pauses during the channel and resumes when it ends.
- Uses native arrow behavior and damage calculation.

Agreed cooldown behavior (implemented Rank 1): Barrage has no separate skill cooldown.
Focus availability and the preserved firing interval limit use. After releasing
or switching skills, a fresh press can resume firing once sufficient Focus is
available and the next arrow's firing interval has elapsed.

Agreed firing cadence (implemented Rank 1): stopping and restarting preserves the earliest
allowed time of the next arrow. Restarting cannot fire sooner than the existing
interval permits; repeated presses cannot exceed the configured firing cadence.
Resuming may wait for the remaining interval, which releasing the key does not
reset.

Agreed damage interruption (implemented Rank 1): ordinary damage and knockback do not end
Barrage; aiming remains under player control. Effects that prevent acting, such
as freezing, end the channel. Root alone does not stop firing under the existing
movement-versus-action control rules.

Agreed channel interruption (implemented Rank 1): successfully activating another combat
skill ends Barrage and activates that skill immediately. Rejected activations
leave the channel running. Resuming after interruption requires a fresh Barrage
key press; holding the original key does not restart firing.

Agreed ordinary bow interaction (implemented Rank 1): starting ordinary bow use ends
Barrage and begins the normal bow draw. Starting Barrage during an existing
ordinary draw cancels that draw without firing its arrow. Ordinary bow use and
Barrage cannot fire simultaneously; resuming Barrage requires a fresh skill-key
press.

Agreed lifecycle (implemented Rank 1): owner death, logout, dimension/class change or
unlearning ends the channel under existing skill cleanup rules. It does not
resume after loading or reconnecting, and spent Focus is not refunded.

At base values, Barrage consumes 18 Focus per second and fires at the vanilla arrow damage coefficient. A full 100 Focus pool supports approximately 5.5 seconds of continuous firing, excluding timing and rounding.

**Balance intent:** A sustained damage option that rewards aim and uptime. It should not be the best answer to every encounter. Its channel cost and regeneration pause create a real resource commitment.

#### Snipe

**Minimum character level:** 10  
**Maximum rank:** 8  
**Prerequisites:** None

**Role:** Long-range precision burst.

| Parameter | Rank 1 default |
|---|---:|
| Focus cost | 40 (proposed) |
| Cooldown | 8 seconds (proposed) |
| Stationary charge | 1.5 seconds (proposed) |
| Minimum damage coefficient | 3 (proposed) |
| Maximum damage coefficient | 5 (proposed) |
| Distance for maximum bonus | 30 blocks (proposed) |

- Charges while the player remains stationary, then fires one single-target shot.
- Moving during the charge cancels it.
- Damage bonus increases with distance, up to the configured cap.
- Recommended default: begin increasing the bonus after 10 blocks and reach the maximum at 30 blocks.
- Use a linear distance curve between those thresholds.

**Balance intent:** Reward deliberate positioning and long sightlines. The stationary charge and range requirement distinguish Snipe from Aimed Shot.

### 5.2 Auras

All auras follow the shared Covenant aura model:

- Passive effect is active while the aura is assigned to a skill slot.
- Activating the assigned key provides a separate temporary effect.
- The passive remains active during the aura's cooldown and while its active effect is running.
- Passives have no resource upkeep.
- Activations are instant and spend a one-time Focus cost.
- Nearby effects use the shared six-block spherical radius and ally eligibility rules.
- Duplicate copies of the same aura do not stack.
- Auras do not require line of sight under the shared aura model.

#### Dodge Aura

**Minimum character level:** 10  
**Maximum rank:** 8

| Parameter | Rank 1 default |
|---|---:|
| Passive Dodge | +10 percentage points |
| Activation cost | 25 Focus |
| Cooldown | 20 seconds |
| Guaranteed-dodge window | 3 seconds |

- Passive grants +10 percentage points of Dodge to the player and eligible nearby allies.
- Ordinary Dodge chance retains the global 85% cap, including equipment and aura bonuses.
- Active grants each eligible recipient one guaranteed dodge against the next dodgeable hit during the three-second window.
- Undodgeable damage remains undodgeable.
- Overlapping guarantees protect against one hit, not a queue of hits.
- The guarantee bypasses the ordinary Dodge cap.

**Rank design:** Do not increase passive Dodge through ranks. Improve the active's duration or coverage modestly.

#### Swiftness Aura

**Minimum character level:** 25  
**Maximum rank:** 8

| Parameter | Rank 1 default |
|---|---:|
| Passive movement speed | +10% |
| Activation cost | 25 Focus |
| Cooldown | 20 seconds |
| Active additional movement speed | +20% |
| Active duration | 4 seconds |
| Additional step height | 0.5 blocks |

- Passive grants +10% movement speed to the player and eligible nearby allies.
- Active clears existing vanilla Slowness and Covenant movement slows, then grants +20% additional movement speed and +0.5 blocks of step height.
- Passive and active speed bonuses add to +30% while both apply.
- The cleanse happens once on activation; it does not grant slow immunity.
- New hits and ongoing fields may reapply slows.
- Roots and full freezes are not removed.
- Third-party movement modifiers are not removed unless explicitly integrated.

**Rank design:** Keep the passive +10% fixed. Improve active duration or coverage modestly.

### 5.3 Traps and control

#### Frost Trap

**Minimum character level:** 10  
**Maximum rank:** 8  
**Prerequisites:** Bear Trap

**Role:** Area denial and group control.

| Parameter | Rank 1 default |
|---|---:|
| Focus cost | 30 (existing prototype) |
| Cooldown | 12 seconds (existing prototype) |
| Patch radius | 3.5 blocks |
| Patch duration | 8 seconds |
| Movement slow | 50% |

- When triggered, creates a temporary frost patch.
- Slows eligible enemies only while they remain inside the patch.
- Slow does not linger after leaving all eligible patches.
- Deals no damage.
- Overlapping Frost patches do not stack their slow; only the strongest applicable Frost slow applies.
- Patch duration is affected by Skill Duration, captured when the trap is thrown.

**Balance intent:** Control groups and make it easier to land other abilities. Its value is positional, not direct damage.

#### Arcane Arrow

**Minimum character level:** 15  
**Maximum rank:** 8

**Role:** Damage amplification and coordinated burst setup.

| Parameter | Rank 1 default |
|---|---:|
| Focus cost | 30 (proposed) |
| Cooldown | 8 seconds (proposed) |
| Direct damage | 4 (proposed) |
| Damage-taken increase | +15% (proposed) |
| Vulnerability duration | 4 seconds (proposed) |

- Fires a single-target arrow that deals magic damage and applies a short vulnerability effect.
- Requires a held bow and uses one arrow under the existing bow-skill ammunition
  rules, including native ammunition-use exemptions such as Infinity. Its four
  raw magic damage replaces ordinary arrow impact damage rather than adding
  a second damage hit.
- Tipped and spectral arrows retain their normal secondary effects, allowing
  one shot to apply both vulnerability and the ammunition's native effect.
- Apply vulnerability only after the direct hit deals positive damage to health
  or absorption hearts. A fully prevented hit applies no vulnerability. Resolve
  the direct hit before applying or refreshing the debuff, so the hit does not
  benefit from the vulnerability it newly applies.
- Vulnerability increases damage taken from all eligible sources, including allies.
- Repeated applications, including from multiple Stalkers, refresh one shared vulnerability duration without stacking its strength.
- Vulnerability adds to Shock Aura's damage-taken increase: +15% from Arcane
  Arrow and +10% from Shock Aura produce +25% damage taken while both apply.
  These two bonuses do not multiply each other.
- Damage type should use the configured native damage source and its normal mitigation behavior.
  In the current implementation, native magic tags bypass armor and shields;
  the projectile remains dodgeable, and general mitigation/immunity still apply.

**Balance intent:** This is a team-enabling skill and one of the Stalker's key combo tools. It should increase the value of a coordinated damage window rather than becoming a high-damage attack by itself.

#### Poison Shot

**Minimum character level:** 15  
**Maximum rank:** 8

**Role:** Sustained single-target pressure.

| Parameter | Rank 1 default |
|---|---:|
| Focus cost | 25 (proposed) |
| Cooldown | 6 seconds (proposed) |
| Initial damage | 3 (proposed) |
| Poison damage | 1 per second (proposed) |
| Poison duration | 5 seconds (proposed) |

- Fires a single-target arrow.
- Applies initial hit damage and a poison damage-over-time effect.
- Reapplication refreshes duration rather than stacking independent poison damage instances.
- Poison tick interval, damage, and duration are independently configurable.
- Poison should use an explicit damage source so modpacks can decide how it interacts with armor, poison immunity, and other effects.

**Balance intent:** Keep pressure on a target while the Stalker repositions or prepares another attack. It should be reliable sustained damage, not a burst multiplier.

#### Multishot

**Minimum character level:** 20  
**Maximum rank:** 8

**Role:** Spread damage across a group.

| Parameter | Rank 1 default |
|---|---:|
| Focus cost | 30 (proposed) |
| Cooldown | 8 seconds (proposed) |
| Arrow count | 5 (proposed) |
| Total horizontal spread | 30 degrees (proposed) |
| Damage coefficient | 1.5 per arrow (proposed) |

- Fires several arrows simultaneously in a horizontal fan.
- The full five-arrow fan uses one ammunition item under the existing bow-skill
  ammunition rules, including native ammunition-use exemptions. The four extra
  arrows are unrecoverable; at most the central arrow can be recovered, subject
  to the normal ammunition/pickup rules. One activation cannot duplicate ammo.
- Covers enemies spread across the player's view.
- Each enemy can take damage at most once per activation, even if multiple arrows connect.
- The first eligible arrow contact claims that target's single damage attempt
  for the entire fan, even if the attempt is blocked, dodged, or otherwise
  prevented. Other arrows from that activation cannot retry damage or apply
  on-hit ammunition effects to that target. This prevents overlapping arrows
  from repeatedly testing defenses.
- Uses native arrow damage and projectile interactions.

**Balance intent:** Stronger against spread-out groups than against one isolated target. The one-hit-per-target rule prevents point-blank shotgun behavior.

#### Flame Trap

**Minimum character level:** 20  
**Maximum rank:** 8

**Role:** Sustained area damage.

| Parameter | Rank 1 default |
|---|---:|
| Focus cost | 35 (existing prototype) |
| Cooldown | 12 seconds (existing prototype) |
| Patch radius | 3 blocks |
| Patch duration | 6 seconds |
| Damage | 3 per second |

- Creates a temporary burning area when triggered.
- Damages eligible enemies only while they remain inside the patch.
- Leaving the patch ends its damage; it does not apply a lingering burn.
- An eligible enemy takes its first hit immediately on entry, then at most one hit per second from that patch.
- Re-entry does not bypass the per-enemy interval.
- Overlapping patches from different owners each deal their own damage with independent intervals and owner credit.
- A player's own next Flame Trap trigger replaces their previous active Flame patch.
- Damage is fire damage and bypasses Dodge and shields, as specified in the current trap design.
- Fire Resistance and natural fire immunity prevent the damage.
- General mitigation and invulnerability still apply.
- Patch duration is affected by Skill Duration, captured when the trap is thrown.

**Balance intent:** Reward keeping enemies inside the area. Frost Trap and Bear Trap create opportunities for Flame Trap to deal more of its potential damage.

#### Snaring Shot

**Minimum character level:** 25  
**Maximum rank:** 8  
**Prerequisites:** Poison Shot

**Role:** Damage over time plus pursuit control.

| Parameter | Rank 1 default |
|---|---:|
| Focus cost | 35 (proposed) |
| Cooldown | 8 seconds (proposed) |
| Initial damage | 3 (proposed) |
| Poison damage | 1 per second (proposed) |
| Poison duration | 5 seconds (proposed) |
| Movement slow | 40% (proposed) |
| Slow duration | 3 seconds (proposed) |

- Fires a single-target arrow.
- Applies poison damage over time and a temporary movement slow.
- Poison reapplication refreshes duration rather than stacking damage.
- The slow is separate from Frost Trap's area slow and does not require the target to remain inside an area.
- Where multiple Covenant slows apply, use the existing strongest-slow resolution rather than multiplying slow percentages.

**Balance intent:** A more flexible control option than Poison Shot, but it should not replace Frost Trap's group-control role.

#### Blast Trap

**Minimum character level:** 25  
**Maximum rank:** 8  
**Prerequisites:** Flame Trap

**Role:** Area burst and emergency separation.

| Parameter | Rank 1 default |
|---|---:|
| Focus cost | 40 (existing prototype) |
| Cooldown | 15 seconds (existing prototype) |
| Explosion radius | 3.5 blocks |
| Maximum center damage | 10 |
| Center knockback target | Approximately 3 blocks |

- Creates one TNT-like explosion when triggered.
- Damage and knockback are strongest at the center and fall toward zero at the outer edge.
- Does not destroy blocks.
- Primarily pushes enemies outward with a small upward bump.
- Native knockback resistance and terrain affect actual displacement.
- Explosion damage bypasses Dodge and shields, but armor and Blast Protection reduce damage.
- General mitigation and invulnerability still apply.
- Knockback can push enemies out of Frost or Flame patches; this is an intentional tactical trade-off.

**Balance intent:** Immediate burst and space creation. It should not be the Stalker's best sustained damage option.

### 5.4 High-level abilities

#### Meteor Arrow

**Minimum character level:** 30  
**Maximum rank:** 8  
**Prerequisites:** Arcane Arrow

**Role:** Delayed, mobile-target area burst.

| Parameter | Rank 1 default |
|---|---:|
| Focus cost | 50 (proposed) |
| Cooldown | 15 seconds (proposed) |
| Cast time | 1 second (proposed) |
| Meteor delay after marking | 1.5 seconds (proposed) |
| Impact radius | 4 blocks (proposed) |
| Center damage | 14 (proposed) |
| Edge damage | 4 (proposed) |
| Burn duration | 3 seconds (proposed) |

- Casts an instant-shot arrow to designate a target.
- A meteor lands on the marked target shortly afterward, following it if it moves.
- Meteor deals area damage around the impact point.
- Affected enemies are ignited; the impact does not leave burning ground.
- Meteor Arrow does not apply Arcane Arrow's vulnerability.
- Use an explicit configured damage source and normal mitigation rules.

**Balance intent:** A high-impact area attack with a visible setup and delay. Tracking the marked target makes it useful against moving enemies, but the delay should remain visible and understandable.

#### Ballista

**Minimum character level:** 30  
**Maximum rank:** 8

**Role:** Temporary stationary ranged support.

| Parameter | Rank 1 default |
|---|---:|
| Focus cost | 45 (proposed) |
| Cooldown | 20 seconds (proposed) |
| Maximum active Ballistas | 2 |
| Lifetime | 20 seconds (proposed) |
| Health | 40 (proposed) |
| Targeting range | 12 blocks (proposed) |
| Firing interval | 2 seconds (proposed) |
| Bolt damage | 6 (proposed) |

- Deploys a stationary turret that automatically fires heavy bolts at nearby eligible enemies.
- Each player may have two active Ballistas.
- Deploying a third removes the oldest.
- Enemies cannot select the Ballista as an attack target, but incidental damage can still affect it.
- Bolt damage and target selection should use server-authoritative rules.
- Ballista damage should be eligible for Arcane Arrow's vulnerability effect.
- Ballista should not gain unlimited uptime from cooldown or duration stacking; apply the existing effective cooldown and duration coefficients with configured minimums where necessary.

**Balance intent:** Reward holding a position and preparing an engagement. The turret is supplemental damage, not a substitute for active play.

#### Volley

**Minimum character level:** 30  
**Maximum rank:** 8

**Role:** Area saturation against groups.

| Parameter | Rank 1 default |
|---|---:|
| Focus cost | 50 (proposed) |
| Cooldown | 18 seconds (proposed) |
| Rain duration | 3 seconds (proposed) |
| Total area radius | 6 blocks (existing design) |
| Arrow count | 15 (proposed) |
| Impact radius | 1 block (existing design) |
| Damage per impact | 3 (proposed) |
| Per-target hit interval | 0.5 seconds (proposed) |

- Rains arrows vertically across a broad selected ground area.
- Arrow landing positions are randomized within the six-block overall radius.
- Each arrow deals area damage within a one-block impact radius.
- Enemies can take repeated damage from overlapping impacts, subject to a short per-enemy interval.
- One activation starts the rain; holding the key is not required.
- The player can reposition and use other skills while the arrows fall.
- Damage uses a configured arrow/impact damage source and the appropriate native mitigation.

**Balance intent:** High potential damage against clustered groups, with meaningful variance and placement dependence. It should be less reliable against a single moving target than precision attacks.

---

## 6. Rank progression

### 6.1 General rules

Rank 1 is the complete functional version of the skill. Ranks 2–8 should add modest, skill-specific improvements rather than routine flat damage increases.

Avoid:
- Flat damage added on every rank.
- Multiplicative damage increases repeated across multiple ranks.
- Rank upgrades that make prerequisites obsolete.
- Passive aura stat increases that compound with equipment and other auras.
- Rank upgrades that remove a skill's intended counterplay.

Use:
- Projectile velocity where already established.
- Slightly better handling, area coverage, duration, utility, or resource efficiency.
- Small improvements to activation windows or deployment behavior.
- Clear, inspectable per-rank values.

### 6.2 Established arrow rank progression

Piercing Shot and Aimed Shot use the existing agreed progression:

- Each additional rank adds 2% of Rank 1 launch velocity.
- Rank 8 reaches +14% launch velocity.
- The increase is additive relative to Rank 1, not compounded.
- Preserve native speed-based arrow damage, including the resulting indirect damage increase.
- Aimed Shot's 20% movement speed during wind-up remains at its Rank 1 baseline.

| Rank | Launch velocity |
|---:|---:|
| 1 | 100% |
| 2 | 102% |
| 3 | 104% |
| 4 | 106% |
| 5 | 108% |
| 6 | 110% |
| 7 | 112% |
| 8 | 114% |

### 6.3 Proposed rank themes

The following are rank-design directions, not a requirement to implement every listed upgrade immediately. Each skill's final per-rank table should be authored and validated separately.

| Skill | Rank progression direction |
|---|---|
| Piercing Shot | Established +2% Rank 1 velocity per rank |
| Aimed Shot | Established +2% Rank 1 velocity per rank |
| Disengage | Slightly improve displacement or recovery/handling; do not add damage |
| Bear Trap | Modest arming or deployment improvements; avoid large root-duration growth |
| Hunter's Pet | Improve survivability or responsiveness before damage |
| Barrage | Improve firing handling or reduce per-arrow cost modestly; avoid large damage scaling |
| Snipe | Improve charge handling or distance curve modestly; do not remove stationary requirement |
| Dodge Aura | Keep passive +10 points fixed; improve active window/coverage modestly |
| Frost Trap | Improve patch coverage or duration modestly; keep slow strength fixed |
| Arcane Arrow | Improve debuff duration or application handling; keep vulnerability strength fixed |
| Poison Shot | Improve poison duration or application handling; avoid stacking damage |
| Multishot | Improve spread control or arrow count cautiously; preserve one hit per target |
| Flame Trap | Improve patch duration or coverage modestly; keep damage rate fixed initially |
| Swiftness Aura | Keep passive +10% fixed; improve active duration/coverage modestly |
| Snaring Shot | Improve application reliability or duration modestly; do not compound slow strength |
| Blast Trap | Improve placement or knockback handling modestly; avoid large damage growth |
| Meteor Arrow | Improve mark/impact handling or area modestly; avoid large center-damage growth |
| Ballista | Improve lifetime or targeting utility modestly; avoid exponential autonomous DPS |
| Volley | Improve rain duration or arrow count cautiously; preserve hit interval and area identity |

### 6.4 Aura rank boundary

Aura passive strengths remain at Rank 1 values across ranks. Put modest improvements into activation or coverage where appropriate. Preserve shared overlap rules.

This avoids passive stats scaling without limit when combined with equipment and multiple aura users.

---

## 7. Equipment scaling and skill identity

The equipment system already supplies meaningful modifiers for:

- Resource Cost
- Cooldown
- Cast Time
- Skill Duration
- Movement Speed
- Attack Speed
- Dodge
- Health
- Attack Damage
- Armor

The Stalker should use these existing systems rather than adding a generic skill-damage affix solely to make endgame builds stronger.

### 7.1 Native weapon scaling

- Arrow skills use the native arrow damage calculation and relevant bow/ammunition behavior.
- Trap damage is independent of the held weapon's native Attack Damage stat, as specified in the existing trap design.
- Summon damage is defined by the summon skill and does not inherit arbitrary player melee Attack Damage unless explicitly configured.
- Ability-specific damage can be exposed as a datapack value without automatically scaling from every equipment stat.

This preserves the identity of each skill and prevents an unrelated melee weapon affix from unexpectedly scaling every Stalker ability.

### 7.2 Existing affix benchmarks

The current six-piece maximum Legendary Tier 5 equipment benchmark is intentionally powerful. It is a theoretical ceiling for the affix system, not an expectation for ordinary gear.

| Modifier | Six maximum Legendary Tier 5 affixes |
|---|---:|
| Resource Cost | -34.5% |
| Cooldown | -34.5% |
| Cast Time | -69% |
| Skill Duration | +34.5% |
| Movement Speed | +34.5% |

These values should be included in endgame Stalker testing. For example, a 20-second cooldown with 34.5% reduction becomes approximately 13.1 seconds before existing timer rounding and other modifiers.

### 7.3 Minimum effective values

To avoid degenerate values in extreme modpacks, skill definitions should support optional minimums for:
- Final resource cost.
- Final cooldown.
- Final cast time.
- Final duration where a minimum is meaningful.

These minimums should be configurable per skill or through a shared default policy. Do not assume every skill needs the same floor. Instant skills should remain instant; a minimum cast time should not be imposed on them.

---

## 8. Synergy goals and intended builds

The Stalker should reward deliberate combinations. A build's strength should come from how skills support each other, not just from selecting the largest individual damage numbers.

### 8.1 Battlefield Controller

**Core skills:** Bear Trap, Frost Trap, Flame Trap, Blast Trap, Piercing Shot.

- Bear Trap holds one dangerous target in place.
- Frost Trap slows a group inside an area.
- Flame Trap punishes enemies that remain in the patch.
- Piercing Shot attacks enemies aligned by the engagement.
- Blast Trap provides emergency burst and displacement.

**Strength:** Excellent control over enemy positioning and group pressure.  
**Weakness:** Less effective against enemies that resist or avoid control, or that attack from outside trap areas.

### 8.2 Precision Hunter

**Core skills:** Aimed Shot, Snipe, Arcane Arrow, Disengage, Dodge Aura.

- Arcane Arrow creates a vulnerability window.
- Aimed Shot provides reliable concentrated damage.
- Snipe rewards distance and stationary positioning.
- Disengage helps preserve range.
- Dodge Aura adds defensive reliability.

**Strength:** High single-target pressure and deliberate burst windows.  
**Weakness:** Wind-ups and positioning make the player vulnerable when pressured.

### 8.3 Sustained Hunter

**Core skills:** Barrage, Poison Shot, Hunter's Pet, Ballista, Swiftness Aura.

- Barrage provides sustained direct pressure.
- Poison Shot maintains damage while repositioning.
- Hunter's Pet contributes distraction and supplemental damage.
- Ballista adds temporary stationary damage.
- Swiftness Aura supports repositioning.

**Strength:** Multiple concurrent sources of pressure and flexible movement.  
**Weakness:** Lower immediate burst and dependence on resource management, pet survival, and deployment uptime.

### 8.4 Area Annihilator

**Core skills:** Frost Trap, Flame Trap, Multishot, Volley, Meteor Arrow.

- Frost Trap helps keep groups in a useful area.
- Flame Trap punishes enemies that remain in the patch.
- Multishot handles enemies spread across the player's view.
- Volley saturates a selected area.
- Meteor Arrow provides delayed area burst.

**Strength:** Strong against dense groups and predictable encounters.  
**Weakness:** Expensive, cooldown-dependent, and less reliable against scattered or highly mobile enemies.

These are conceptual builds, not finalized eight-skill loadouts. Prerequisites count toward the eight allocated-skill limit and must be included when validating actual builds.

---

## 9. Stacking, refresh, and interaction rules

A coherent stacking policy is more important than maximizing every individual number.

| Effect | Default interaction |
|---|---|
| Bear Trap root | Reapplication refreshes to the longer of remaining/new duration; does not add durations |
| Frost Trap slow | Strongest applicable Frost slow wins; copies do not add |
| Flame Trap damage | Different owners' patches each deal their own damage; same owner's next triggered patch replaces the previous one |
| Arcane Arrow vulnerability | One shared strength; repeated applications refresh duration |
| Poison Shot | Reapplication refreshes duration; poison damage does not stack |
| Snaring Shot poison | Uses the same non-stacking poison policy |
| Movement slows | Use strongest-slow resolution; do not multiply percentages |
| Dodge Aura passive | Same aura copies do not stack |
| Dodge Aura guarantee | Overlapping guarantees protect against one dodgeable hit, not a queue |
| Swiftness Aura active | Same activated aura buffs do not stack in strength |
| Ballista | Maximum two per player; third removes oldest |
| Volley | Repeated hits allowed subject to configured per-target interval |
| Vulnerability and Shock Aura | May stack as explicitly established in the current design |

For any new effect, its definition should state whether it stacks by strength, duration, source, target, or not at all. Avoid leaving this to incidental implementation behavior.

---

## 10. Server datapack configuration requirements

The built-in Stalker definitions should be data-driven and overridable by server datapacks. The purpose is to let a vanilla server use the default tuning while allowing a modpack author to rebalance the same class without code changes.

### 10.1 Values that should be configurable

At minimum, support configuration for:

**Class resource**
- Starting and maximum Focus.
- Base regeneration.
- Regeneration modifier cap.
- Channeling regeneration behavior.

**Each skill**
- Unlock level.
- Prerequisites and exclusions.
- Maximum rank.
- Focus cost.
- Cooldown.
- Cast time or charge time.
- Damage amount or arrow damage coefficient.
- Damage type/source where supported.
- Projectile count, velocity, spread, and targeting limits.
- Area radius and duration.
- Root, slow, poison, vulnerability, and other effect strengths/durations.
- Summon health, damage, attack interval, lifetime, targeting range, and active limit.
- Per-rank effect values.
- Minimum effective cost/cooldown/cast-time/duration safeguards where applicable.

**Shared combat rules**
- Target eligibility and exclusions.
- Effect stacking and refresh policy.
- Boss/entity-tag overrides for selected effects.
- Damage-source behavior and defense interactions where supported.
- Default area and aura radius.
- Cooldown/resource-cost/duration coefficient application.

### 10.2 Recommended datapack organization

The exact resource paths and schema are implementation decisions, but the conceptual organization should separate:

- Class and resource defaults.
- Individual skill definitions.
- Per-rank effects.
- Shared effect/stacking definitions where genuinely reused.
- Optional entity-tag or boss-specific overrides.
- Built-in defaults from server overrides.

Prefer stable identifiers and tags over mod-name checks or hardcoded mod lists. For example, a server should be able to apply a boss-control rule to an entity tag without Covenant knowing which mod supplied those entities.

### 10.3 Override and validation behavior

- Server datapacks are authoritative.
- Built-in values provide defaults when no override exists.
- A higher-priority server datapack can replace selected fields without copying every unrelated skill definition, if the chosen loader/schema supports field-level composition safely.
- Invalid entries should fail safely and emit actionable diagnostics.
- Unknown optional fields should not silently change behavior.
- Numeric values should be range-validated to prevent negative cooldowns, impossible resource costs, invalid radii, or runaway tick rates.
- Reloading balance definitions must not erase player skill allocations, ranks, class selection, Focus state, or persistent progression.
- A changed skill definition should affect future activations. Already-deployed traps, patches, summons, and projectiles should follow their documented snapshot/lifecycle rules.
- Do not expose every implementation detail as a configurable field without a demonstrated need. Keep the configuration surface understandable.

### 10.4 Modpack authoring examples

A modpack might:
- Increase Arcane Arrow's vulnerability duration to support longer boss phases.
- Reduce Ballista damage because the pack has high-health enemies and strong summon equipment.
- Increase Snipe's maximum distance for a large-dimension exploration pack.
- Reduce trap slow against a specific boss tag while leaving ordinary enemies unaffected.
- Change Focus regeneration and skill costs to match a slower or faster combat environment.
- Retune the entire Stalker skill tree without modifying Covenant code.

These are examples of intended flexibility, not built-in balance assumptions.

---

## 11. Playtesting and acceptance criteria

Do not declare the Stalker balanced from spreadsheet calculations alone. Test the actual combat loop with vanilla equipment and then with representative modded gear and enemies.

### 11.1 Vanilla baseline tests

1. **Ordinary encounter:** A Stalker using basic attacks should remain viable without spending Focus on every enemy.
2. **Small group:** Piercing Shot and Multishot should reward enemy alignment or spread without becoming interchangeable.
3. **Armored target:** Arrow and trap damage should respect their intended native mitigation.
4. **Control combo:** Bear/Frost/Flame should create a strong setup without making enemies permanently helpless.
5. **Boss:** Bosses should be affected by default control rules. Confirm that the player still needs to manage positioning, cooldowns, and incoming attacks.
6. **Sustained channel:** Barrage should have meaningful damage uptime but visibly consume Focus and pause regeneration.
7. **Endgame equipment:** Test maximum or near-maximum resource-cost, cooldown, duration, and cast-time affixes together.
8. **Multiplayer:** Verify owner credit, allied targeting exclusions, shared vulnerability, overlapping traps, and summon behavior.

### 11.2 Modpack compatibility tests

Use a representative set of modded content with:
- High-health enemies.
- High-armor enemies.
- Bosses with native knockback resistance.
- Entities with unusual hitboxes or movement.
- Additional damage types or immunity systems.
- Modded bows, arrows, and ranged equipment.
- Extra dimensions and dimension-specific progression.

The goal is not to make one default numerical configuration perfectly balanced for every modpack. The goal is to confirm that the skills function predictably and can be retuned through datapacks.

### 11.3 Acceptance criteria

The first balance pass is ready for broader testing when:

- All Rank 1 skills have explicit, documented numerical defaults.
- Skill costs, cooldowns, cast times, durations, and effects are loaded from server-owned definitions.
- Vanilla arrows and native damage calculations remain intact where intended.
- Bosses have no implicit blanket control immunity or resistance.
- Effect stacking and refresh behavior is deterministic.
- Equipment modifiers affect the intended skill parameters without unexpectedly scaling unrelated damage.
- Server datapack overrides can change skill tuning without code changes.
- Invalid datapack entries fail safely with useful diagnostics.
- Multiplayer behavior is server-authoritative and consistent.
- No skill makes its prerequisite or an entire alternative build obsolete.

---

## 12. Decisions and items still requiring playtesting

The following should remain provisional until measured in-game:

1. Piercing Shot's proposed Focus cost and cooldown.
2. Disengage displacement and cooldown.
3. Barrage's per-arrow cost, firing rate, and arrow coefficient.
4. Snipe's distance curve and maximum coefficient.
5. Hunter's Pet health, damage, and defeat cooldown.
6. Arcane Arrow's vulnerability strength and duration.
7. Poison and Snaring Shot tick behavior and damage.
8. Meteor Arrow's tracking, delay, and damage falloff.
9. Ballista uptime and autonomous damage.
10. Volley arrow count, duration, and per-target hit interval.
11. The exact per-rank effects for skills other than Piercing Shot and Aimed Shot.
12. Whether any particular modpack needs explicit boss-tag overrides.

The current prototype values for Piercing Shot, Aimed Shot, Bear Trap, Frost Trap, Flame Trap, Blast Trap, Dodge Aura, and Swiftness Aura should remain identifiable as established starting points. This proposal does not imply that all of their values are final.

---

## 13. Final design position

The Stalker should be balanced as a vanilla-compatible class with a configurable ceiling, not as a class whose default design assumes a heavily modded environment.

In a vanilla-oriented world, the class should feel recognizable: bows remain bows, arrows behave like arrows, armor remains meaningful, and abilities create tactical opportunities rather than replacing ordinary combat.

In a heavily modded world, the same class should remain usable because its numbers, effects, progression, and selected interaction rules can be retuned by the server. Covenant should provide the mechanics and sensible defaults; the modpack should own its balance decisions.

A fully invested Stalker should feel powerful through specialization and combinations:

- Control enemies, then punish their positioning.
- Apply vulnerability, then coordinate burst damage.
- Maintain poison pressure while repositioning.
- Deploy a Ballista and defend its firing lane.
- Use equipment to improve ability uptime and resource efficiency.

The class should not need universal damage inflation, custom armor math, or automatic enemy-health scaling to achieve that power.

**Final recommendation:** Adopt this proposal as the Stalker's first complete numerical baseline. Keep vanilla behavior as the default, expose the balance-critical values through server datapacks, and use playtesting to adjust the defaults rather than adding a separate hardcoded modpack combat model.
