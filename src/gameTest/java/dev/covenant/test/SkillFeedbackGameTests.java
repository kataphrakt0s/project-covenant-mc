package dev.covenant.test;

import com.mojang.authlib.GameProfile;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.skills.ClassResources;
import dev.covenant.rpg.skills.ArcaneBolt;
import dev.covenant.rpg.skills.CombatParticles;
import dev.covenant.rpg.skills.ElementalBolt;
import dev.covenant.rpg.skills.ElementalBolts;
import dev.covenant.rpg.skills.LightningVisual;
import dev.covenant.rpg.skills.Auras;
import dev.covenant.rpg.skills.Bleeding;
import dev.covenant.rpg.skills.ProjectileCombat;
import dev.covenant.rpg.skills.SkillDefinition;
import dev.covenant.rpg.skills.SkillLifecycle;
import dev.covenant.rpg.skills.Skills;
import dev.covenant.rpg.stats.RpgStats;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

/** Native sound/particle emission boundaries; these do not establish audible or rendered client quality. */
final class SkillFeedbackGameTests {
    private SkillFeedbackGameTests() {}

    static void volley(GameTestHelper helper) {
        try (var f = new Fixture(helper, 180)) {
            var player = f.player(PlayerClass.STALKER, SkillDefinition.MULTISHOT);
            check(key(Skills.activate(player, SkillDefinition.MULTISHOT), "bow"), "Missing bow rejects activation");
            player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            check(key(Skills.activate(player, SkillDefinition.MULTISHOT), "arrow"), "Missing ammunition rejects activation");
            player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.ARROW, 20));
            ClassResources.set(player, PlayerClass.STALKER, 0);
            check(key(Skills.activate(player, SkillDefinition.MULTISHOT), "resource"), "Insufficient Focus rejects activation");
            check(f.soundsAt(player) == 0, "Rejected requests emit no skill sounds");
            ClassResources.set(player, PlayerClass.STALKER, 100);
            check(key(Skills.activate(player, SkillDefinition.MULTISHOT), "used"), "Accepted volley");
            var arrows = helper.getLevel().getEntitiesOfClass(AbstractArrow.class, player.getBoundingBox().inflate(4),
                    arrow -> arrow.getOwner() == player);
            f.entities.addAll(arrows);
            check(arrows.size() == 5, "Presentation preserves the five native arrows");
            check(arrows.stream().allMatch(arrow -> arrow.getData(RpgData.SKILL_SHOT_STYLE) == 3),
                    "All volley arrows carry the lower-density cosmetic style");
            var reporter = new ProblemReporter.Collector();
            var output = TagValueOutput.createWithContext(reporter, player.registryAccess());
            var original = arrows.getFirst(); original.saveWithoutId(output);
            var saved = output.buildResult(); saved.remove("UUID");
            var loaded = (AbstractArrow) original.getType().create(helper.getLevel(), EntitySpawnReason.COMMAND);
            f.entities.add(loaded);
            loaded.load(TagValueInput.create(reporter, player.registryAccess(), saved));
            check(reporter.isEmpty() && !loaded.hasData(RpgData.SKILL_SHOT) && !loaded.hasData(RpgData.SKILL_SHOT_STYLE),
                    "Native serialization omits both cosmetic arrow attachments");
            check(ProjectileCombat.isSkill(loaded), "Cosmetic style does not replace saved skill provenance");
            check(f.soundsAt(player) == 1, "One release cue per volley, not one per arrow");
            check(player.getOffhandItem().getCount() == 19, "Volley still consumes one arrow");
            check(player.getMainHandItem().getDamageValue() == 1, "Volley still costs one bow durability");
            check(key(Skills.activate(player, SkillDefinition.MULTISHOT), "cooldown"), "Immediate repeat remains rejected");
            check(f.soundsAt(player) == 1, "Rejected repeat cannot replay release audio");
            helper.succeed();
        }
    }

    static void melee(GameTestHelper helper) {
        try (var f = new Fixture(helper, 225)) {
            var whiff = f.player(PlayerClass.REAVER, SkillDefinition.CLEAVE);
            whiff.setYRot(90);
            check(key(Skills.activate(whiff, SkillDefinition.CLEAVE), "used"), "Whiff is still an accepted strike");
            check(f.soundsAt(whiff) == 1, "Whiff has a swing cue but no damage confirmation");
            var crescent = f.particles(whiff, CombatParticles.CLEAVE.get());
            check(crescent.size() == 1 && crescent.getFirst().getCount() == 0, "Whiff emits one directional native particle");
            near(crescent.getFirst().getXDist(), Math.PI / 2, "Crescent carries accepted yaw in radians");
            near(crescent.getFirst().getYDist(), SkillDefinition.CLEAVE.range(), "Crescent carries actual attack range");
            check(!crescent.getFirst().isOverrideLimiter() && !crescent.getFirst().alwaysShow(), "Decoration respects native particle settings");
            check(f.particles(whiff, CombatParticles.CONTACT.get()).isEmpty(), "Whiff has no accepted-hit mark");

            var striker = f.player(PlayerClass.REAVER, SkillDefinition.CLEAVE);
            var first = f.target(striker.position().add(-.5, 0, 2));
            var second = f.target(striker.position().add(.5, 0, 2));
            float firstHealth = first.getHealth(), secondHealth = second.getHealth();
            check(key(Skills.activate(striker, SkillDefinition.CLEAVE), "used"), "Area strike accepted");
            check(first.getHealth() < firstHealth && second.getHealth() < secondHealth, "Both in-arc targets receive damage");
            check(f.soundsAt(striker) == 2, "Multi-target strike adds one aggregate damage confirmation");
            check(f.particles(striker, CombatParticles.CONTACT.get()).size() == 2, "Each damaged target gets one small contact mark");

            var blocked = f.player(PlayerClass.REAVER, SkillDefinition.CLEAVE);
            var immune = f.target(blocked.position().add(0, 0, 2)); immune.setInvulnerable(true);
            float immuneHealth = immune.getHealth();
            check(key(Skills.activate(blocked, SkillDefinition.CLEAVE), "used"), "Protected target does not reject the swing");
            near(immune.getHealth(), immuneHealth, "Protected target receives no damage");
            check(f.soundsAt(blocked) == 1, "Fully prevented damage produces no success cue");
            check(f.particles(blocked, CombatParticles.CONTACT.get()).isEmpty(), "Prevented damage has no accepted-hit mark");
            helper.succeed();
        }
    }

    static void casting(GameTestHelper helper) {
        var f = new Fixture(helper, 195);
        try {
            var completed = f.player(PlayerClass.WEAVER, SkillDefinition.ARCANE_BOLT);
            var cancelled = f.player(PlayerClass.WEAVER, SkillDefinition.ARCANE_BOLT);
            check(key(Skills.activate(completed, SkillDefinition.ARCANE_BOLT), "started"), "First cast accepted");
            check(key(Skills.activate(cancelled, SkillDefinition.ARCANE_BOLT), "started"), "Second cast accepted");
            check(f.soundsAt(completed) == 1 && f.soundsAt(cancelled) == 1, "Each accepted cast emits one start cue");
            check(key(Skills.activate(completed, SkillDefinition.ARCANE_BOLT), "casting"), "Overlapping cast rejected");
            check(f.soundsAt(completed) == 1, "Overlapping request does not replay start cue");
            near(ClassResources.current(completed, PlayerClass.WEAVER), 100, "Start cue does not spend Mana early");
            SkillLifecycle.cancelPlayer(cancelled, SkillLifecycle.Reason.DIMENSION_CHANGE);
            check(!Skills.isCasting(cancelled), "Lifecycle cancellation clears pending cast");
            var aimed = f.player(PlayerClass.STALKER, SkillDefinition.AIMED_SHOT);
            var aimedCancelled = f.player(PlayerClass.STALKER, SkillDefinition.AIMED_SHOT);
            var interrupted = f.player(PlayerClass.STALKER, SkillDefinition.AIMED_SHOT);
            for (var archer : List.of(aimed, aimedCancelled, interrupted)) {
                archer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                archer.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.ARROW, 2));
                check(key(Skills.activate(archer, SkillDefinition.AIMED_SHOT), "started"), "Aimed Shot begins its real wind-up");
            }
            SkillLifecycle.cancelPlayer(aimedCancelled, SkillLifecycle.Reason.DIMENSION_CHANGE);
            interrupted.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            helper.onEachTick(f::tickPlayers);
            helper.runAtTickTime(25, () -> {
                try (f) {
                    check(!Skills.isCasting(completed), "Normal cast completes");
                    check(f.soundsAt(completed) == 2, "Completion emits one release after its single start cue");
                    check(f.soundsAt(cancelled) == 1, "Cancelled cast never emits its release cue");
                    near(ClassResources.current(completed, PlayerClass.WEAVER), 80, "Completed cast spends normal Mana");
                    near(ClassResources.current(cancelled, PlayerClass.WEAVER), 100, "Cancelled cast preserves Mana");
                    check(Skills.cooldownRemaining(completed, SkillDefinition.ARCANE_BOLT) > 0, "Completion retains cooldown");
                    check(Skills.cooldownRemaining(cancelled, SkillDefinition.ARCANE_BOLT) == 0, "Cancellation commits no cooldown");
                    check(f.particles(cancelled, CombatParticles.ARCANE_MOTE.get()).isEmpty(), "Cancelled cast never emits later charge motes");
                    check(!f.particles(aimed, CombatParticles.AIMED_GATHER.get()).isEmpty(), "Aimed Shot has gathering cues during its actual wind-up");
                    check(f.particles(aimedCancelled, CombatParticles.AIMED_GATHER.get()).isEmpty(), "Cancelled Aimed Shot has no later gathering cues");
                    check(f.soundsAt(aimed) == 2 && f.soundsAt(aimedCancelled) == 1, "Only completed Aimed Shot emits release audio");
                    check(aimed.getOffhandItem().getCount() == 1 && aimedCancelled.getOffhandItem().getCount() == 2,
                            "Presentation preserves completed/cancelled ammunition spending");
                    check(f.messages.get(completed.getUUID()).stream().anyMatch(packet -> packet.overlay() && key(packet.content(), "used")),
                            "Real cast completion uses native action-bar feedback");
                    var notices = f.messages.get(interrupted.getUUID());
                    check(notices.stream().anyMatch(packet -> packet.overlay() && key(packet.content(), "cancelled")),
                            "Equipment cancellation has authoritative action-bar feedback");
                    check(notices.stream().noneMatch(packet -> key(packet.content(), "used")), "Cancelled cast has no success message");
                    near(ClassResources.current(interrupted, PlayerClass.STALKER), 100, "Equipment interruption spends no Focus");
                    check(interrupted.getOffhandItem().getCount() == 2, "Equipment interruption spends no ammunition");
                    helper.succeed();
                }
            });
        } catch (RuntimeException error) { f.close(); throw error; }
    }

    static void impacts(GameTestHelper helper) {
        var f = new Fixture(helper, 210);
        try {
            for (var skill : List.of(SkillDefinition.ARCANE_BOLT, SkillDefinition.FIRE_BOLT, SkillDefinition.ICE_BOLT)) {
                var player = f.player(PlayerClass.WEAVER, skill);
                player.getAttribute(RpgStats.CAST_TIME).setBaseValue(0);
                var wall = BlockPos.containing(player.getEyePosition().add(0, 0, 2));
                f.blocks.add(wall);
                helper.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
                check(key(Skills.activate(player, skill), "used"), "Instant bolt accepted: " + skill.id());
                check(f.soundsAt(player) == 1, "Instant bolt has one release without a nonexistent wind-up");
                var bolts = helper.getLevel().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(3),
                        bolt -> bolt.getOwner() == player);
                check(bolts.size() == 1, "Accepted bolt spawns one projectile");
                var bolt = bolts.getFirst(); f.entities.add(bolt);
                for (int i = 0; i < 6 && !bolt.isRemoved(); i++) { bolt.tick(); }
                check(bolt.isRemoved(), "Native block collision removes bolt: " + skill.id());
                check(f.soundsAt(player) == 2, "Terminal collision emits exactly one impact cue: " + skill.id());
                var collision = skill == SkillDefinition.ARCANE_BOLT ? CombatParticles.ARCANE_COLLISION.get()
                        : skill == SkillDefinition.FIRE_BOLT ? CombatParticles.FIRE_COLLISION.get() : CombatParticles.ICE_COLLISION.get();
                check(f.particles(player, collision).size() == 1, "Wall terminates the bolt with one element-specific collision sequence");
                check(f.particles(player, CombatParticles.CONTACT.get()).isEmpty(), "Wall collision never claims accepted damage");
                near(ClassResources.current(player, PlayerClass.WEAVER), 80, "Feedback retains normal spell cost");
            }
            helper.runAtTickTime(5, () -> {
                try (f) {
                    for (var player : f.players) {
                        check(f.soundsAt(player) == 2, "Removed projectile cannot repeat its impact on later server ticks");
                    }
                    helper.succeed();
                }
            });
        } catch (RuntimeException error) { f.close(); throw error; }
    }

    static void projectileContacts(GameTestHelper helper) {
        try (var f = new Fixture(helper, 240)) {
            var player = f.player(PlayerClass.WEAVER, SkillDefinition.ARCANE_BOLT);
            var bolt = new ArcaneBolt(Skills.ARCANE_BOLT.get(), helper.getLevel());
            bolt.setOwner(player);
            var target = f.target(player.position().add(0, 0, 2));
            var source = player.damageSources().indirectMagic(bolt, player);
            target.setInvulnerable(true);
            target.hurtServer(helper.getLevel(), source, 2);
            check(f.particles(player, CombatParticles.CONTACT.get()).isEmpty(), "Rejected projectile damage emits no contact mark");
            target.setInvulnerable(false);
            target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION).setBaseValue(5);
            target.setAbsorptionAmount(5);
            float health = target.getHealth();
            target.hurtServer(helper.getLevel(), source, 2);
            near(target.getHealth(), health, "Absorption-only hit preserves health");
            check(f.particles(player, CombatParticles.CONTACT.get()).size() == 1, "Absorption-only accepted damage emits one mark");
            check(f.particles(player, CombatParticles.ARCANE_COLLISION.get()).isEmpty(), "Damage event alone never invents a collision");

            var archer = f.player(PlayerClass.STALKER, SkillDefinition.PIERCING_SHOT);
            archer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            archer.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.ARROW, 2));
            check(key(Skills.activate(archer, SkillDefinition.PIERCING_SHOT), "used"), "Piercing arrow accepted");
            var arrow = helper.getLevel().getEntitiesOfClass(AbstractArrow.class, archer.getBoundingBox().inflate(4),
                    entity -> entity.getOwner() == archer).getFirst();
            var victim = f.target(archer.position().add(0, 0, 2));
            victim.hurtServer(helper.getLevel(), archer.damageSources().arrow(arrow, archer), 2);
            check(f.particles(archer, CombatParticles.CONTACT.get()).size() == 1, "Marked piercing arrow confirms accepted damage");
            arrow.removeData(RpgData.SKILL_SHOT);
            var ordinaryVictim = f.target(archer.position().add(1, 0, 2));
            ordinaryVictim.hurtServer(helper.getLevel(), archer.damageSources().arrow(arrow, archer), 2);
            check(f.particles(archer, CombatParticles.CONTACT.get()).size() == 1, "Unmarked ordinary arrow gets no Covenant contact mark");
            helper.succeed();
        }
    }

    static void elementalContacts(GameTestHelper helper) {
        try (var f = new Fixture(helper, 245)) {
            for (var skill : List.of(SkillDefinition.FIRE_BOLT, SkillDefinition.ICE_BOLT, SkillDefinition.LIGHTNING_BOLT)) {
                var player = f.player(PlayerClass.WEAVER, skill);
                var target = f.target(player.position().add(0, 0, 2));
                var bolt = new ElementalBolt(skill == SkillDefinition.FIRE_BOLT ? Skills.FIRE_BOLT.get() : Skills.ICE_BOLT.get(), helper.getLevel());
                bolt.setOwner(player);
                Entity direct = skill == SkillDefinition.LIGHTNING_BOLT ? player : bolt;
                target.setInvulnerable(true);
                ElementalBolts.hit(helper.getLevel(), player, direct, target, skill, 60);
                check(f.particles(player, CombatParticles.CONTACT.get()).isEmpty(), "Immune elemental hit has no damage mark: " + skill.id());
                target.setInvulnerable(false);
                target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION).setBaseValue(20);
                target.setAbsorptionAmount(20);
                float health = target.getHealth();
                ElementalBolts.hit(helper.getLevel(), player, direct, target, skill, 60);
                near(target.getHealth(), health, "Elemental absorption-only hit preserves health");
                check(f.particles(player, CombatParticles.CONTACT.get()).size() == 1, "Each accepted elemental hit has one damage mark");
            }
            helper.succeed();
        }
    }

    static void lightningLinks(GameTestHelper helper) {
        try (var f = new Fixture(helper, 250)) {
            var player = f.player(PlayerClass.WEAVER, SkillDefinition.LIGHTNING_BOLT);
            FixtureSkills.learn(player, SkillDefinition.SHOCK_AURA.id(), true);
            player.setData(RpgData.SKILL_LOADOUT, player.getData(RpgData.SKILL_LOADOUT).withBinding(0, SkillDefinition.SHOCK_AURA.id()));
            check(key(Skills.activate(player, SkillDefinition.SHOCK_AURA), "used"), "Existing Shock activation accepted");
            player.getAttribute(RpgStats.CAST_TIME).setBaseValue(0);
            var one = f.target(player.position().add(0,0,4), EntityType.HUSK);
            var two = f.target(player.position().add(0,0,7), EntityType.HUSK);
            var three = f.target(player.position().add(0,0,10), EntityType.HUSK);
            var four = f.target(player.position().add(0,0,13), EntityType.HUSK);
            for (var target : List.of(one,two,three,four)) {
                target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);
                target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);
                target.setHealth(200);
            }
            reset(player, SkillDefinition.LIGHTNING_BOLT);
            check(key(Skills.activate(player, SkillDefinition.LIGHTNING_BOLT), "used"), "Lightning release accepted");
            var beams = f.beams.get(player.getUUID());
            check(beams.size() == 3, "Exactly one main ray and two real chain links");
            check(beams.getFirst().start().equals(player.getEyePosition()) && one.getBoundingBox().inflate(.01).contains(beams.getFirst().end()),
                    "Main visual ends at the actual first entity collision");
            check(beams.get(1).start().equals(one.getEyePosition()) && beams.get(1).end().equals(two.getEyePosition()), "First link joins real targets");
            check(beams.get(2).start().equals(two.getEyePosition()) && beams.get(2).end().equals(three.getEyePosition()), "Second link joins real targets");
            near(four.getHealth(),200,"No cosmetic branch creates another damaged target");
            check(f.particlePackets.get(player.getUUID()).stream().filter(packet -> packet.getParticle().getType() == CombatParticles.CONTACT.get()).count() == 3,
                    "Main ray and accepted chain hits each get one contact mark");
            check(beams.stream().allMatch(beam -> beam.dimension().equals(helper.getLevel().dimension().identifier())), "Links retain their authoritative dimension");

            var wall=BlockPos.containing(player.getX(),player.getEyeY(),player.getZ()+5);
            f.blocks.add(wall); helper.getLevel().setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());
            one.invulnerableTime=0; beams.clear(); reset(player,SkillDefinition.LIGHTNING_BOLT);
            Skills.activate(player,SkillDefinition.LIGHTNING_BOLT);
            check(beams.size()==1,"Cover prevents both damage and invented chain links");
            for (var target : List.of(one,two,three,four)) { target.discard(); }
            beams.clear(); f.particlePackets.get(player.getUUID()).clear(); player.setYRot(180);
            reset(player,SkillDefinition.LIGHTNING_BOLT); Skills.activate(player,SkillDefinition.LIGHTNING_BOLT);
            check(beams.size()==1,"Empty aim still has exactly one main visual");
            near(beams.getFirst().start().distanceTo(beams.getFirst().end()),SkillDefinition.LIGHTNING_BOLT.range(),"Range miss stops at actual range");
            check(f.particles(player,CombatParticles.LIGHTNING_COLLISION.get()).isEmpty() && f.particles(player,CombatParticles.CONTACT.get()).isEmpty(),
                    "Range miss invents neither collision nor damage");
            helper.succeed();
        }
    }

    static void bowContacts(GameTestHelper helper) {
        try (var f = new Fixture(helper,255)) {
            for (var skill : List.of(SkillDefinition.AIMED_SHOT,SkillDefinition.ARCANE_ARROW,SkillDefinition.MULTISHOT)) {
                var player=f.player(PlayerClass.STALKER,skill);
                player.getAttribute(RpgStats.CAST_TIME).setBaseValue(0);
                player.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));
                player.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.ARROW,2));
                var target=f.target(player.position().add(0,0,2),EntityType.HUSK);
                target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200); target.setHealth(200);
                check(key(Skills.activate(player,skill),"used"),"Bow release accepted");
                var arrows=helper.getLevel().getEntitiesOfClass(AbstractArrow.class,player.getBoundingBox().inflate(3),arrow -> arrow.getOwner()==player);
                for (var arrow:arrows) { for (int i=0;i<4&&!arrow.isRemoved();i++) { arrow.tick(); } }
                var mark=skill==SkillDefinition.ARCANE_ARROW?CombatParticles.ARCANE_CONTACT.get():CombatParticles.CONTACT.get();
                check(f.particles(player,mark).size()==1,"Native collision retains style through one accepted hit: "+skill.id());
                check(target.getHealth()<200,"Contact mark corresponds to actual native damage");
                if (skill==SkillDefinition.MULTISHOT) { check(arrows.size()==5,"Five-arrow fan is retained"); }
            }
            helper.succeed();
        }
    }

    static void bleedFeedback(GameTestHelper helper) {
        try (var f=new Fixture(helper,260)) {
            var player=f.player(PlayerClass.REAVER,SkillDefinition.REND);
            var target=f.target(player.position().add(0,0,2),EntityType.HUSK);
            target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200); target.setHealth(200);
            target.setInvulnerable(true); Skills.activate(player,SkillDefinition.REND);
            check(f.particles(player,CombatParticles.REND_CONTACT.get()).isEmpty(),"Prevented Rend never marks success");
            target.setInvulnerable(false); reset(player,SkillDefinition.REND); Skills.activate(player,SkillDefinition.REND);
            check(f.particles(player,CombatParticles.REND_CONTACT.get()).size()==1,"Accepted Rend gets one single-target slash");
            check(f.particles(player,CombatParticles.CLEAVE.get()).isEmpty(),"Rend never implies a broad sweep");
            Bleeding.apply(target,player,1,100); Bleeding.apply(target,player,1,100);
            check(Bleeding.count(target)==3,"Shared bleed cap unchanged");
            for(int i=0;i<20;i++){NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(target));}
            check(f.particles(player,CombatParticles.BLEED_TICK.get()).size()==1,"Three accepted stacks share one sparse pulse cue");
            target.setInvulnerable(true);
            for(int i=0;i<20;i++){NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(target));}
            check(f.particles(player,CombatParticles.BLEED_TICK.get()).size()==1,"Prevented pulse emits no success mark");
            helper.succeed();
        }
    }

    private static void reset(ServerPlayer player,SkillDefinition skill) {
        player.removeData(RpgData.SKILL_RUNTIME);
        player.setData(RpgData.SKILLS,player.getData(RpgData.SKILLS).withCooldown(skill.id(),0));
        ClassResources.set(player,PlayerProfiles.getSelectedClass(player).orElseThrow(),100);
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;
        final Vec3 origin;
        final List<ServerPlayer> players = new ArrayList<>();
        final List<Entity> entities = new ArrayList<>();
        final List<BlockPos> blocks = new ArrayList<>();
        final List<Vec3> sounds = new ArrayList<>();
        final java.util.Map<UUID, List<net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket>> particlePackets = new java.util.HashMap<>();
        final java.util.Map<UUID, List<LightningVisual>> beams = new java.util.HashMap<>();
        final java.util.Map<UUID, List<net.minecraft.network.protocol.game.ClientboundSystemChatPacket>> messages = new java.util.HashMap<>();
        final Consumer<PlayLevelSoundEvent.AtPosition> listener = this::sound;
        boolean closed;

        Fixture(GameTestHelper helper, int height) {
            this.helper = helper;
            var pos = helper.absolutePos(new BlockPos(2, height, 2));
            origin = new Vec3(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
            NeoForge.EVENT_BUS.addListener(listener);
        }

        ServerPlayer player(PlayerClass playerClass, SkillDefinition skill) {
            var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "feedback-test"), false);
            var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
            var packets = new ArrayList<net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket>();
            particlePackets.put(player.getUUID(), packets);
            var links=new ArrayList<LightningVisual>(); beams.put(player.getUUID(),links);
            var notices = new ArrayList<net.minecraft.network.protocol.game.ClientboundSystemChatPacket>(); messages.put(player.getUUID(), notices);
            var connection = new Connection(PacketFlow.SERVERBOUND) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet, io.netty.channel.ChannelFutureListener listener, boolean flush) {
                    if (packet instanceof net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket particles) { packets.add(particles); }
                    if (packet instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket payload && payload.payload() instanceof LightningVisual beam) { links.add(beam); }
                    if (packet instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket notice) { notices.add(notice); }
                    super.send(packet, listener, flush);
                }
                @Override public boolean isMemoryConnection() { return true; }
            };
            new io.netty.channel.embedded.EmbeddedChannel(connection);
            net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
            helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
            players.add(player);
            try {
                var pending = player.connection.getClass().getDeclaredField("awaitingTeleport");
                pending.setAccessible(true);
                player.connection.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(pending.getInt(player.connection)));
            } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
            player.setPos(origin.add((players.size() - 1) * 12, 0, 0));
            player.connection.resetPosition();
            player.setGameMode(GameType.SURVIVAL);
            player.connection.markClientLoaded();
            player.setNoGravity(true); player.setYRot(0); player.setXRot(0);
            PlayerProfiles.selectClass(player, playerClass);
            FixtureSkills.learn(player, skill.id(), true);
            player.getAttribute(RpgStats.MANA_REGENERATION).setBaseValue(0);
            ClassResources.set(player, playerClass, 100);
            return player;
        }

        LivingEntity target(Vec3 position) {
            return target(position,EntityType.COW);
        }

        LivingEntity target(Vec3 position,EntityType<? extends net.minecraft.world.entity.Mob> type) {
            var target = type.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            target.setPos(position); target.setNoAi(true); target.setNoGravity(true);
            helper.getLevel().addFreshEntity(target); entities.add(target);
            return target;
        }

        private void sound(PlayLevelSoundEvent.AtPosition event) {
            if (event.getLevel() != helper.getLevel() || event.getSource() != SoundSource.PLAYERS || event.getSound() == null) { return; }
            if (players.stream().anyMatch(player -> event.getPosition().distanceToSqr(player.position()) < 25)) {
                check(event.getOriginalVolume() > 0 && event.getOriginalVolume() <= 1, "Skill sound uses restrained native volume");
                sounds.add(event.getPosition());
            }
        }

        long soundsAt(ServerPlayer player) { return sounds.stream().filter(pos -> pos.distanceToSqr(player.position()) < 25).count(); }

        List<net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket> particles(ServerPlayer player, net.minecraft.core.particles.ParticleType<?> type) {
            return particlePackets.get(player.getUUID()).stream().filter(packet -> packet.getParticle().getType() == type
                    && player.position().distanceToSqr(new Vec3(packet.getX(), packet.getY(), packet.getZ())) < 25).toList();
        }

        void tickPlayers() {
            if (closed) { return; }
            try { players.forEach(ServerPlayer::doTick); }
            catch (RuntimeException error) { close(); throw error; }
        }

        @Override public void close() {
            if (closed) { return; }
            closed = true;
            NeoForge.EVENT_BUS.unregister(listener);
            entities.forEach(Entity::discard);
            for (var player : players) {
                helper.getLevel().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(64),
                        projectile -> projectile.getOwner() == player).forEach(Entity::discard);
                SkillLifecycle.cancelPlayer(player, SkillLifecycle.Reason.LOGOUT);
                helper.getLevel().getServer().getPlayerList().remove(player);
            }
            blocks.forEach(pos -> helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()));
        }
    }

    private static boolean key(Component value, String suffix) {
        return value.getContents() instanceof TranslatableContents text && text.getKey().equals("commands.covenant.skill." + suffix);
    }
    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < .001, message + ": expected " + expected + ", got " + actual);
    }
    private static void check(boolean condition, String message) { if (!condition) { throw new IllegalStateException(message); } }
}
