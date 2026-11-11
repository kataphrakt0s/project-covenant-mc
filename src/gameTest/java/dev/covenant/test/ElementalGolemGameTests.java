package dev.covenant.test;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.stats.RpgStats;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.skills.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Real native navigation/melee and shared pet policies; fixtures alone keep their chunks ticking. */
final class ElementalGolemGameTests {
    static void skill(GameTestHelper helper) {
        var f=new Fixture(helper,140);
        helper.startSequence().thenWaitUntil(f::assertReady).thenExecute(() -> {
            var owner=f.player();
            ClassResources.set(owner,PlayerClass.WEAVER,39);
            check(key(Skills.activate(owner,SkillDefinition.ELEMENTAL_GOLEM),"resource"),"Summon requires Mana");
            ClassResources.set(owner,PlayerClass.WEAVER,100);
            owner.getAttribute(RpgStats.COOLDOWN).setBaseValue(.5);
            check(key(SkillLoadouts.activateSlot(owner,0),"used"),"Slotted skill summons");
            var golem=ElementalGolems.current(owner); golem.setNoAi(true); golem.setNoGravity(true);
            check(golem.form()==ElementalGolem.Form.ARCANE && golem.getHealth()==60,"Initial Arcane summon");
            near(ClassResources.current(owner,PlayerClass.WEAVER),60,"Full base summon cost before Arcane discount");
            near(Skills.cooldownRemaining(owner,SkillDefinition.ELEMENTAL_GOLEM),0,"Summon starts no cooldown");
            near(owner.getAttributeValue(RpgStats.MANA_REGENERATION),1.2,"Arcane regen");
            near(owner.getAttributeValue(RpgStats.RESOURCE_COST),.9,"Arcane cost reduction");
            ClassResources.set(owner,PlayerClass.WEAVER,0);
            resetThrottle(owner);
            check(key(SkillLoadouts.activateSlot(owner,0),"busy"),"Initial five-tick cycle delay");
            helper.onEachTick(f::tick);
            helper.runAfterDelay(5,() -> f.run(() -> {
                ClassResources.set(owner,PlayerClass.WEAVER,0);
                check(key(SkillLoadouts.activateSlot(owner,0),"form"),"Cycle works at zero Mana");
                check(ElementalGolems.current(owner)==golem && golem.form()==ElementalGolem.Form.FIRE,"Cycle keeps same companion");
                near(ClassResources.current(owner,PlayerClass.WEAVER),0,"Cycling is free");
                resetThrottle(owner);
                check(key(SkillLoadouts.activateSlot(owner,0),"busy"),"Cycle cannot bypass fixed delay");
                helper.runAfterDelay(4,() -> f.run(() -> {
                    check(key(SkillLoadouts.activateSlot(owner,0),"busy"),"Cooldown reduction does not shorten five ticks");
                    helper.runAfterDelay(1,() -> f.run(() -> {
                        check(key(SkillLoadouts.activateSlot(owner,0),"form"),"Second cycle at five ticks");
                        check(golem.form()==ElementalGolem.Form.ICE,"Cycle order reaches Ice");
                        SkillLoadouts.bind(owner,1,SkillDefinition.ELEMENTAL_GOLEM.id()); SkillLoadouts.bind(owner,0,null);
                        check(ElementalGolems.current(owner)==golem,"Duplicate slot removal keeps companion");
                        SkillLoadouts.bind(owner,1,null);
                        check(!ElementalGolems.active(owner) && golem.isRemoved(),"Final unslot dismisses");
                        near(Skills.cooldownRemaining(owner,SkillDefinition.ELEMENTAL_GOLEM),300,"Summon-time cooldown snapshot");
                        near(owner.getAttributeValue(RpgStats.CAST_TIME),1,"Unslot removes benefit immediately");
                        SkillLoadouts.bind(owner,0,SkillDefinition.ELEMENTAL_GOLEM.id()); resetThrottle(owner);
                        check(key(SkillLoadouts.activateSlot(owner,0),"cooldown"),"Rebinding cannot bypass dismissal cooldown");
                        f.close(); helper.succeed();
                    }));
                }));
            }));
        });
    }

    static void benefits(GameTestHelper helper) {
        var f=new Fixture(helper,156);
        helper.startSequence().thenWaitUntil(f::assertReady).thenExecute(() -> {
            try (f) {
                var owner=f.player(); var ally=f.player(); var golem=f.summon(owner);
                golem.setPos(f.origin); golem.setHealth(17);
                golem.setForm(ElementalGolem.Form.FIRE);
                near(AuraDamage.elementalMultiplier(owner,WeaverPower.Element.FIRE),1.1,"Fire spell bonus");
                near(AuraDamage.elementalMultiplier(owner,WeaverPower.Element.ICE),1.1,"Ice spell bonus from Fire form");
                near(AuraDamage.elementalMultiplier(owner,WeaverPower.Element.LIGHTNING),1.1,"Lightning spell bonus from Fire form");
                near(AuraDamage.multiplier(owner,false),1,"Fire form does not increase physical attacks");
                near(AuraDamage.elementalMultiplier(ally,WeaverPower.Element.FIRE),1,"Benefit is owner-only");
                // Fire power must be captured by the actual spell release, once, even if form changes before impact.
                FixtureSkills.learn(owner,SkillDefinition.ARCANE_BOLT.id(),true);
                owner.getAttribute(RpgStats.CAST_TIME).setBaseValue(0); resetThrottle(owner);
                check(key(Skills.activate(owner,SkillDefinition.ARCANE_BOLT),"used"),"Native Arcane spell release");
                var bolt=helper.getLevel().getEntitiesOfClass(ArcaneBolt.class,owner.getBoundingBox().inflate(4),b -> b.getOwner()==owner).getFirst();
                f.entities.add(bolt);
                golem.setForm(ElementalGolem.Form.ARCANE);
                var enemy=f.mob(EntityType.HUSK,4,2);
                float before=enemy.getHealth();
                try {
                    var impact=ArcaneBolt.class.getDeclaredMethod("onHitEntity",net.minecraft.world.phys.EntityHitResult.class); impact.setAccessible(true);
                    impact.invoke(bolt,new net.minecraft.world.phys.EntityHitResult(enemy));
                } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
                near(before-enemy.getHealth(),SkillDefinition.ARCANE_BOLT.power()*1.1,"Arcane impact retains exactly one Fire bonus after switching");
                golem.setForm(ElementalGolem.Form.ICE);
                owner.getAttribute(RpgStats.CAST_TIME).setBaseValue(1);
                near(owner.getAttributeValue(RpgStats.CAST_TIME),.8,"Ice cast time");
                near(owner.getAttributeValue(RpgStats.COOLDOWN),.9,"Ice cooldown");
                near(ElementalGolems.spellBonus(owner),0,"Old Fire bonus removed");
                ClassResources.set(owner,PlayerClass.WEAVER,70);
                golem.setForm(ElementalGolem.Form.LIGHTNING);
                near(ClassResources.maximum(owner,PlayerClass.WEAVER),120,"Lightning maximum Mana");
                near(ClassResources.current(owner,PlayerClass.WEAVER),70,"Capacity grants no Mana");
                near(owner.getAttributeValue(RpgStats.SKILL_DURATION),1.2,"Lightning duration");
                ClassResources.set(owner,PlayerClass.WEAVER,120);
                owner.setPos(f.origin.add(0,12.01,0)); ElementalGolems.refresh(owner);
                near(ClassResources.maximum(owner,PlayerClass.WEAVER),100,"3D range removes capacity");
                owner.setPos(f.origin.add(0,12,0)); ElementalGolems.refresh(owner);
                near(ClassResources.maximum(owner,PlayerClass.WEAVER),120,"Range boundary includes exactly twelve blocks");
                near(ClassResources.current(owner,PlayerClass.WEAVER),100,"Returning cannot recover discarded Mana");
                owner.setPos(f.origin); golem.setForm(ElementalGolem.Form.ARCANE);
                near(owner.getAttributeValue(RpgStats.SKILL_DURATION),1,"Lightning duration removed");
                check(golem.getHealth()==17,"Cycling never heals");
                check(owner.getData(RpgData.BUFF_DISPLAY).rows().stream().anyMatch(r -> r.skill().equals(ElementalGolem.Form.ARCANE.benefitId())),"Owner status names current benefit");
                helper.succeed();
            }
        });
    }

    static void persistence(GameTestHelper helper) {
        var f=new Fixture(helper,172);
        helper.startSequence().thenWaitUntil(f::assertReady).thenExecute(() -> {
            var owner=f.player(); var original=f.summon(owner);
            original.setNoAi(true); original.setNoGravity(true); original.setHealth(13); original.setForm(ElementalGolem.Form.ICE);
            original.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS,140));
            var reporter=new net.minecraft.util.ProblemReporter.Collector();
            var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(reporter,owner.registryAccess());
            owner.saveWithoutId(output); var saved=output.buildResult();
            check(saved.toString().contains("covenant:elemental_golem"),"Native owner save captures golem");
            SkillLifecycle.cancelPlayer(owner,SkillLifecycle.Reason.LOGOUT);
            check(original.isRemoved() && ElementalGolems.current(owner)==null && ElementalGolems.active(owner),"Logout stores native snapshot");
            owner.load(net.minecraft.world.level.storage.TagValueInput.create(reporter,owner.registryAccess(),saved));
            check(reporter.isEmpty(),"Native player serialization reports no problems: "+reporter.getReport());
            check(ElementalGolems.current(owner)==null,"Loading does not pre-spawn");
            helper.onEachTick(f::tick);
            helper.runAfterDelay(8,() -> f.run(() -> {
                var restored=ElementalGolems.current(owner);
                check(restored!=null && restored.getUUID().equals(original.getUUID()),"Native restore retains identity");
                near(restored.getHealth(),13,"Restore retains injuries");
                check(restored.form()==ElementalGolem.Form.ICE && restored.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS),"Restore retains form and status");
                near(Skills.cooldownRemaining(owner,SkillDefinition.ELEMENTAL_GOLEM),0,"Restore starts no cooldown");
                check(ClassResources.current(owner,PlayerClass.WEAVER)<70,"Restore has no summon payment or resource refill");
                ElementalGolems.store(owner);
                var state=ElementalGolems.State.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,
                        ElementalGolems.State.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,owner.getData(RpgData.ELEMENTAL_GOLEM)).getOrThrow()).getOrThrow();
                owner.setData(RpgData.ELEMENTAL_GOLEM,state);
                helper.runAfterDelay(8,() -> f.run(() -> {
                    check(ElementalGolems.current(owner)!=null && ElementalGolems.current(owner).form()==ElementalGolem.Form.ICE,"Persisted owner codec restores selected form");
                    f.close(); helper.succeed();
                }));
            }));
        });
    }

    static void repairAndDefeat(GameTestHelper helper) {
        var f=new Fixture(helper,188);
        helper.startSequence().thenWaitUntil(f::assertReady).thenExecute(() -> {
            try (f) {
                var owner=f.player(); var golem=f.summon(owner); golem.setHealth(10);
                var hand=net.minecraft.world.InteractionHand.MAIN_HAND;
                owner.setItemInHand(hand,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT,3));
                golem.mobInteract(owner,hand); near(golem.getHealth(),35,"Iron repairs 25 health");
                golem.mobInteract(owner,hand); near(golem.getHealth(),60,"Repair caps at maximum");
                golem.mobInteract(owner,hand); check(owner.getMainHandItem().getCount()==1,"Full-health repair consumes nothing");
                Consumer<net.neoforged.neoforge.event.entity.living.LivingHealEvent> prevent=event -> { if (event.getEntity()==golem) { event.setCanceled(true); } };
                golem.setHealth(10); NeoForge.EVENT_BUS.addListener(prevent);
                try { golem.mobInteract(owner,hand); } finally { NeoForge.EVENT_BUS.unregister(prevent); }
                check(owner.getMainHandItem().getCount()==1 && golem.getHealth()==10,"Cancelled healing consumes nothing");
                Consumer<net.neoforged.neoforge.event.entity.living.LivingDeathEvent> save=event -> {
                    if (event.getEntity()==golem) { golem.setHealth(1); event.setCanceled(true); }
                };
                NeoForge.EVENT_BUS.addListener(save);
                try { golem.hurtServer(helper.getLevel(),golem.damageSources().genericKill(),1000); }
                finally { NeoForge.EVENT_BUS.unregister(save); }
                check(ElementalGolems.current(owner)==golem && golem.isAlive(),"Cancelled native death retains golem");
                near(Skills.cooldownRemaining(owner,SkillDefinition.ELEMENTAL_GOLEM),0,"Cancelled death starts no cooldown");
                golem.invulnerableTime=0;
                golem.hurtServer(helper.getLevel(),golem.damageSources().genericKill(),1000);
                check(!ElementalGolems.active(owner),"Confirmed defeat clears owner snapshot");
                near(Skills.cooldownRemaining(owner,SkillDefinition.ELEMENTAL_GOLEM),600,"Defeat starts cooldown");
                near(owner.getAttributeValue(RpgStats.RESOURCE_COST),1,"Defeat removes benefit");
                var replacement=f.summon(owner); var enemy=f.mob(EntityType.HUSK,3,2); enemy.setHealth(2);
                long xp=PlayerProfiles.get(owner).experience();
                replacement.doHurtTarget(helper.getLevel(),enemy);
                check(PlayerProfiles.get(owner).experience()>xp,"Golem kill credits owner Covenant XP");
                check(ClassResources.inCombat(owner),"Golem combat marks owner in combat");
                helper.succeed();
            }
        });
    }

    private static void near(double actual,double expected,String message) { check(Math.abs(actual-expected)<.001,message+": "+actual+" expected "+expected); }
    static void spawning(GameTestHelper helper) {
        var f = new Fixture(helper, 60);
        helper.startSequence().thenWaitUntil(f::assertReady).thenExecute(() -> {
            try (f) {
                var owner = f.player();
                Consumer<EntityJoinLevelEvent> reject = event -> {
                    if (event.getEntity() instanceof ElementalGolem golem && golem.getOwner() == owner) {
                        check(key(ElementalGolems.summon(owner), "unavailable"), "Reentrant summon rejected");
                        event.setCanceled(true);
                    }
                };
                NeoForge.EVENT_BUS.addListener(reject);
                try { check(key(ElementalGolems.summon(owner), "unavailable"), "Native join cancellation rejects spawn"); }
                finally { NeoForge.EVENT_BUS.unregister(reject); }
                check(ElementalGolems.current(owner) == null, "Cancelled spawn retains no companion");
                var golem = f.summon(owner);
                check(golem.isTame() && golem.getOwner() == owner, "Native tame ownership");
                check(key(ElementalGolems.summon(owner), "unavailable"), "Only one test golem per owner");
                check(Math.abs(golem.getBbWidth()-1.4) < .001 && Math.abs(golem.getBbHeight()-2.7) < .001, "Native golem-sized collision");
                check(helper.getLevel().noCollision(golem), "Spawn is clear for the whole body");
                check(!golem.isOrderedToSit() && !golem.canBeLeashed() && !golem.canFallInLove(), "No pet interactions beyond this test scope");
                check(golem.getAnimatableInstanceCache().getManagerForId(golem.getId()).getAnimationControllers().size()==2, "Body and core controllers registered");
                check(ElementalGolems.dismiss(owner) && golem.isRemoved(), "Explicit dismissal removes entity");
                // Two-block clearance fits the wolf, but must reject the taller golem.
                for (int x=-3;x<=3;x++) { for (int z=-3;z<=3;z++) { f.block(x,2,z,Blocks.STONE); f.block(x,3,z,Blocks.STONE); } }
                resetThrottle(owner);
                check(key(ElementalGolems.summon(owner), "destination"), "Low ceiling rejects the full golem body");
                check(ElementalGolems.current(owner) == null, "Unsafe placement leaves no state");
                helper.succeed();
            }
        });
    }

    static void targeting(GameTestHelper helper) {
        var f = new Fixture(helper, 76);
        helper.startSequence().thenWaitUntil(f::assertReady).thenExecute(() -> {
            try (f) {
                var owner=f.player(); var golem=f.summon(owner); var enemy=f.mob(EntityType.HUSK,4,0);
                var creeper=f.mob(EntityType.CREEPER,5,2);
                check(golem.getTarget()==null, "No independent hostile hunting");
                enemy.hurtServer(helper.getLevel(),owner.damageSources().playerAttack(owner),1);
                check(golem.getTarget()==enemy, "Landed owner damage selects target");
                creeper.hurtServer(helper.getLevel(),owner.damageSources().playerAttack(owner),1);
                check(golem.getTarget()==creeper, "Same engaged-creeper policy as Hunter's Pet");
                owner.hurtServer(helper.getLevel(),enemy.damageSources().mobAttack(enemy),1);
                check(golem.getTarget()==enemy, "Owner defense targets attacker");
                golem.hurtServer(helper.getLevel(),creeper.damageSources().mobAttack(creeper),1);
                check(golem.getTarget()==creeper, "Self defense targets attacker");
                var ally=f.player();
                var team=helper.getLevel().getScoreboard().addPlayerTeam("g"+UUID.randomUUID().toString().substring(0,8)); f.teams.add(team);
                for (var p:List.of(owner,ally)) { helper.getLevel().getScoreboard().addPlayerToTeam(p.getScoreboardName(),team); }
                var wolf=f.mob(EntityType.WOLF,3,2); wolf.setTame(true,false); wolf.setOwner(ally);
                check(!golem.canAttack(owner) && !golem.canAttack(ally) && !golem.canAttack(wolf), "Owner, team and allied pets protected");
                float health=ally.getHealth(); check(!golem.doHurtTarget(helper.getLevel(),ally) && ally.getHealth()==health, "Native attack boundary preserves allies");
                golem.setTarget(null); enemy.setInvulnerable(true); enemy.invulnerableTime=0;
                enemy.hurtServer(helper.getLevel(),owner.damageSources().playerAttack(owner),1);
                check(golem.getTarget()==null, "Prevented damage does not direct companion");
                helper.succeed();
            }
        });
    }

    static void nativeAi(GameTestHelper helper) {
        var f=new Fixture(helper,92);
        helper.startSequence().thenWaitUntil(f::assertReady).thenExecute(() -> {
            var owner=f.player(); var golem=f.summon(owner); golem.setPos(f.origin); owner.setPos(f.origin.add(10.5,0,0));
            helper.onEachTick(f::tick);
            var attacks=new ArrayList<Long>();
            Consumer<LivingDamageEvent.Post> observer=event -> {
                if (event.getSource().getEntity()==golem && AuraDamage.landedDamage(event)>0) {
                    attacks.add(helper.getLevel().getGameTime());
                    check(Math.abs(AuraDamage.landedDamage(event)-3)<.001,"Provisional native melee damage");
                }
            };
            NeoForge.EVENT_BUS.addListener(observer); f.cleanup.add(() -> NeoForge.EVENT_BUS.unregister(observer));
            helper.runAfterDelay(24,() -> f.run(() -> {
                check(golem.position().distanceToSqr(f.origin)>1,"Native FollowOwnerGoal walks toward nearby owner");
                check(golem.position().distanceToSqr(f.origin)<64,"Short follow walks instead of recalling");
                var enemy=f.mob(EntityType.HUSK,9,0); enemy.setNoAi(false); enemy.setTarget(owner);
                enemy.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0);
                enemy.hurtServer(helper.getLevel(),owner.damageSources().playerAttack(owner),1); enemy.invulnerableTime=0;
                helper.runAfterDelay(125,() -> f.run(() -> {
                    check(attacks.size()>=2,"Native melee pursues and repeatedly attacks: "+attacks);
                    for (int i=1;i<attacks.size();i++) { check(attacks.get(i)-attacks.get(i-1)>=40,"Two-second attack interval retained"); }
                    check(enemy.getTarget()==golem,"Native hostile retaliation targets golem");
                    enemy.setNoAi(true); enemy.setTarget(null); golem.setTarget(null);
                    owner.setPos(f.origin.add(15,0,0)); golem.setPos(f.origin);
                    helper.runAfterDelay(24,() -> f.run(() -> {
                        check(golem.distanceToSqr(owner)<100,"Native far-owner recall catches up on safe loaded terrain");
                        f.close(); helper.succeed();
                    }));
                }));
            }));
        });
    }

    static void cleanup(GameTestHelper helper) {
        var f=new Fixture(helper,108);
        helper.startSequence().thenWaitUntil(f::assertReady).thenExecute(() -> {
            try (f) {
                var owner=f.player();
                for (var reason:SkillLifecycle.Reason.values()) {
                    var golem=f.summon(owner); SkillLifecycle.cancelPlayer(owner,reason);
                    check(golem.isRemoved() && ElementalGolems.current(owner)==null,"Test golem clears at "+reason);
                    boolean stored=reason==SkillLifecycle.Reason.LOGOUT || reason==SkillLifecycle.Reason.DIMENSION_CHANGE;
                    check(ElementalGolems.active(owner)==stored,"Shared companion lifecycle at "+reason);
                    ElementalGolems.dismiss(owner);
                }
                var golem=f.summon(owner); golem.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                check(ElementalGolems.current(owner)==null && ElementalGolems.active(owner),"Unload retains owner snapshot for safe restoration");
                helper.succeed();
            }
        });
    }

    @SuppressWarnings("unchecked")
    static void animations(GameTestHelper helper) {
        var f=new Fixture(helper,124);
        helper.startSequence().thenWaitUntil(f::assertReady).thenExecute(() -> {
            try (f) {
                var owner=f.player(); var golem=f.summon(owner); golem.setNoAi(true); golem.setNoGravity(true); golem.tick();
                for (var form:ElementalGolem.Form.values()) {
                    golem.setForm(form);
                    check(golem.form()==form && golem.getHealth()==60 && golem.getAttributeValue(Attributes.ATTACK_DAMAGE)==3,
                            "Appearance selection does not change combat attributes");
                }
                var manager=new com.geckolib.animatable.manager.AnimatableManager<ElementalGolem>(golem);
                var field=com.geckolib.animation.AnimationController.class.getDeclaredField("stateHandler"); field.setAccessible(true);
                var controller=manager.getAnimationControllers().get("main");
                var handler=(com.geckolib.animation.AnimationController.AnimationStateHandler<ElementalGolem>)field.get(controller);
                var state=new com.geckolib.renderer.base.GeoRenderState.Impl();
                state.addGeckolibData(com.geckolib.constant.DataTickets.IS_MOVING,false);
                var test=new com.geckolib.animation.state.AnimationTest<>(golem,state,manager,controller);
                handler.handle(test);
                check(controller.getCurrentRawAnimation().equals(com.geckolib.animation.RawAnimation.begin().thenLoop("animation.elemental_golem.idle")),"Stationary golem idles");
                golem.setPos(golem.position().add(.12,0,0)); golem.tick(); handler.handle(test);
                check(controller.getCurrentRawAnimation().equals(com.geckolib.animation.RawAnimation.begin().thenLoop("animation.elemental_golem.walk")),"Tracked horizontal travel walks without native limb flag");
                golem.tick(); handler.handle(test);
                check(controller.getCurrentRawAnimation().equals(com.geckolib.animation.RawAnimation.begin().thenLoop("animation.elemental_golem.idle")),"Stopped golem returns to idle");
                var core=manager.getAnimationControllers().get("core");
                var coreHandler=(com.geckolib.animation.AnimationController.AnimationStateHandler<ElementalGolem>)field.get(core);
                var coreTest=new com.geckolib.animation.state.AnimationTest<>(golem,state,manager,core);
                coreHandler.handle(coreTest); double speed=core.getAnimationSpeed();
                check(speed>=.85 && speed<=1.15,"Slow per-entity core rate is bounded");
                CompanionAnimationFixture.checkActionRecovery(golem,manager,"elemental_golem");
                coreHandler.handle(coreTest);
                check(core.getAnimationSpeed()==0,"Core stops with death rather than surviving the companion");
                helper.succeed();
            } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
        });
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper; final Vec3 origin;
        final List<ServerPlayer> players=new ArrayList<>(); final List<Entity> entities=new ArrayList<>();
        final List<net.minecraft.world.scores.PlayerTeam> teams=new ArrayList<>(); final List<Runnable> cleanup=new ArrayList<>();
        final Map<BlockPos,net.minecraft.world.level.block.state.BlockState> blocks=new HashMap<>();
        final Set<ChunkPos> chunks=new HashSet<>(); boolean closed;
        final TicketType ticket=new TicketType(TicketType.NO_TIMEOUT,TicketType.FLAG_LOADING|TicketType.FLAG_SIMULATION|TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
        Fixture(GameTestHelper helper,int height) {
            this.helper=helper; origin=Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1,height,1)));
            helper.runBeforeTestEnd(() -> {
                close();
                helper.fail("Golem fixture did not finish before timeout");
            });
            for (int x=-4;x<=18;x++) { for (int z=-4;z<=4;z++) { block(x,-1,z,Blocks.STONE); } }
            for (int x=net.minecraft.util.Mth.floor(origin.x-4)>>4;x<=net.minecraft.util.Mth.floor(origin.x+19)>>4;x++) {
                for (int z=net.minecraft.util.Mth.floor(origin.z-4)>>4;z<=net.minecraft.util.Mth.floor(origin.z+5)>>4;z++) {
                    var chunk=new ChunkPos(x,z); chunks.add(chunk); helper.getLevel().getChunkSource().addTicketWithRadius(ticket,chunk,2);
                }
            }
        }
        void assertReady() { for (var chunk:chunks) {
            helper.assertTrue(helper.getLevel().areEntitiesActuallyLoadedAndTicking(chunk),"Golem fixture chunk "+chunk+" must tick entities");
        } }
        ServerPlayer player() {
            var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(),"golem"+UUID.randomUUID().toString().substring(0,7)),false);
            var p=new ServerPlayer(helper.getLevel().getServer(),helper.getLevel(),cookie.gameProfile(),cookie.clientInformation());
            var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND) { @Override public boolean isMemoryConnection() { return true; } };
            new io.netty.channel.embedded.EmbeddedChannel(connection); helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);
            net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
            p.setGameMode(GameType.SURVIVAL); p.connection.markClientLoaded(); p.setNoGravity(true); p.setPos(origin);
            p.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1); PlayerProfiles.selectClass(p,PlayerClass.WEAVER);
            FixtureSkills.learn(p,SkillDefinition.ELEMENTAL_GOLEM.id(),true);
            SkillLoadouts.bind(p,0,SkillDefinition.ELEMENTAL_GOLEM.id());
            players.add(p); return p;
        }
        ElementalGolem summon(ServerPlayer owner) {
            resetThrottle(owner);
            Skills.setIgnoreCooldowns(owner,true);
            ClassResources.set(owner,PlayerClass.WEAVER,100);
            check(key(ElementalGolems.summon(owner),"summoned"),"Test summon accepted");
            return ElementalGolems.current(owner);
        }
        <T extends Mob> T mob(EntityType<T> type,double x,double z) {
            var mob=type.create(helper.getLevel(),EntitySpawnReason.COMMAND); mob.setPos(origin.add(x,0,z)); mob.setNoAi(true);
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); mob.setHealth(200);
            if (mob.getAttribute(Attributes.ARMOR)!=null) { mob.getAttribute(Attributes.ARMOR).setBaseValue(0); }
            mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1); helper.getLevel().addFreshEntity(mob); entities.add(mob); return mob;
        }
        void block(int x,int y,int z,net.minecraft.world.level.block.Block block) {
            var pos=BlockPos.containing(origin).offset(x,y,z); blocks.putIfAbsent(pos,helper.getLevel().getBlockState(pos)); helper.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());
        }
        void tick() { if (!closed) { players.forEach(ServerPlayer::doTick); } }
        void run(Runnable action) { try { action.run(); } catch (RuntimeException|Error error) { close(); throw error; } }
        @Override public void close() {
            if (closed) { return; } closed=true;
            cleanup.forEach(Runnable::run);
            players.forEach(p -> { ElementalGolems.dismiss(p); helper.getLevel().getServer().getPlayerList().remove(p); });
            entities.forEach(Entity::discard); blocks.forEach((pos,state) -> helper.getLevel().setBlockAndUpdate(pos,state));
            teams.forEach(helper.getLevel().getScoreboard()::removePlayerTeam);
            chunks.forEach(chunk -> helper.getLevel().getChunkSource().removeTicketWithRadius(ticket,chunk,2));
        }
    }
    private static boolean key(Component value,String key) { return value.getContents() instanceof TranslatableContents t
            && (t.getKey().equals("commands.covenant.golem."+key) || t.getKey().equals("commands.covenant.skill."+key)); }
    private static void resetThrottle(ServerPlayer p) {
        try { var field=SkillRuntime.class.getDeclaredField("nextActivation"); field.setAccessible(true); field.setLong(p.getData(RpgData.SKILL_RUNTIME),0); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private static void check(boolean condition,String message) { if (!condition) { throw new IllegalStateException(message); } }
}
