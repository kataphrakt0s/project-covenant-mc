package dev.covenant.rpg.skills;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Only eligible slotted Reavers query nearby entities, at most once per configured interval. */
public final class BloodFever {
    private static final Identifier MOVEMENT = Identifier.fromNamespaceAndPath("covenant", "blood_fever_movement");
    private static final Identifier ATTACK = Identifier.fromNamespaceAndPath("covenant", "blood_fever_attack");
    private BloodFever() {}
    public static void register() { NeoForge.EVENT_BUS.addListener(BloodFever::tick); }

    public static boolean assigned(ServerPlayer player) {
        var loadout = player.getData(RpgData.SKILL_LOADOUT);
        return player.isAlive() && !player.isSpectator() && PlayerProfiles.getSelectedClass(player).orElse(null) == PlayerClass.REAVER
                && loadout.learned().contains(SkillDefinition.BLOOD_FEVER.id()) && loadout.bindings().containsValue(SkillDefinition.BLOOD_FEVER.id());
    }
    public static void reconcile(ServerPlayer player) {
        if (!assigned(player)) { clear(player); }
    }
    public static void clear(ServerPlayer player) {
        player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(MOVEMENT);
        player.getAttribute(Attributes.ATTACK_SPEED).removeModifier(ATTACK);
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        runtime.bloodFeverCount = 0; runtime.nextBloodFever = 0;
    }
    public static int count(ServerPlayer player) { return player.getData(RpgData.SKILL_RUNTIME).bloodFeverCount; }

    /** One bounded spatial query; counts shared bleed presence, never individual stacks. */
    public static void refresh(ServerPlayer player) {
        ClassResources.requireServer(player);
        if (!assigned(player)) { clear(player); return; }
        var config = ReaverSkillData.get(player.level().getServer()).fever();
        int count = 0;
        for (var target : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(config.radius()),
                target -> Skills.canHit(player, target) && Bleeding.count(target) > 0
                        && player.distanceToSqr(target) <= config.radius() * config.radius())) {
            if (++count >= config.enemies()) { break; }
        }
        player.getData(RpgData.SKILL_RUNTIME).bloodFeverCount = count;
        modifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT, count * config.movement());
        modifier(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK, count * config.attack());
    }
    private static void modifier(net.minecraft.world.entity.ai.attributes.AttributeInstance attribute, Identifier id, double amount) {
        var old = attribute.getModifier(id);
        if (amount == 0) { if (old != null) { attribute.removeModifier(id); } }
        else if (old == null || old.amount() != amount) {
            attribute.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }
    private static void tick(PlayerTickEvent.Post event) {
        try (var zone = Profiler.get().zone("covenant:blood_fever")) {
            if (!(event.getEntity() instanceof ServerPlayer player)) { return; }
            if (!assigned(player)) {
                if (player.getData(RpgData.SKILL_RUNTIME).bloodFeverCount > 0) { clear(player); }
                return;
            }
            var runtime = player.getData(RpgData.SKILL_RUNTIME);
            long now = ClassResources.now(player);
            if (now < runtime.nextBloodFever) { return; }
            runtime.nextBloodFever = now + ReaverSkillData.get(player.level().getServer()).fever().interval();
            refresh(player);
        }
    }
}
