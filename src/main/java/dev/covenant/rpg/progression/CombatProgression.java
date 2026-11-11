package dev.covenant.rpg.progression;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.skills.AuraDamage;
import dev.covenant.rpg.skills.Auras;
import dev.covenant.rpg.skills.ClassResources;
import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Runtime-only contribution ledger. Expiry is checked on actions and payout, never by ticking mobs. */
public final class CombatProgression {
    public static final int CONTRIBUTION_TICKS = 600;
    public static final class State {
        final Map<UUID, Long> contributors = new HashMap<>();
        final Map<UUID, Contact> contacts = new HashMap<>();
        boolean paid;
    }
    private record Contact(WeakReference<LivingEntity> enemy, long until) {}
    private CombatProgression() {}
    public static void register() {
        NeoForge.EVENT_BUS.addListener(CombatProgression::damaged);
        NeoForge.EVENT_BUS.addListener(CombatProgression::left);
    }
    private static long now(LivingEntity entity) { return ((ServerLevel) entity.level()).getServer().overworld().getGameTime(); }
    private static boolean eligible(ServerPlayer player) {
        return !player.isSpectator() && PlayerProfiles.getSelectedClass(player).isPresent();
    }
    private static boolean enemy(LivingEntity target) {
        return target.level() instanceof ServerLevel level && !(target instanceof ServerPlayer)
                && ProgressionData.get(level.getServer()).reward(target) > 0;
    }
    /** A successful debuff/control is an action in its own right, even without damage. */
    public static void supportEnemy(ServerPlayer source, LivingEntity target) {
        if (source.level().isClientSide() || !eligible(source) || !(target.level() instanceof ServerLevel) || Auras.allied(source, target)) { return; }
        credit(source, target);
        contact(source, target);
    }
    private static void credit(ServerPlayer player, LivingEntity target) {
        if (!eligible(player)) { return; }
        ClassResources.markCombat(player);
        if (!enemy(target)) { return; }
        var state = target.getData(RpgData.COMBAT_PROGRESSION);
        if (state.paid) { return; }
        long time = now(target);
        state.contributors.values().removeIf(last -> time - last >= CONTRIBUTION_TICKS);
        state.contributors.put(player.getUUID(), time);
    }
    private static void contact(ServerPlayer player, LivingEntity target) {
        var contacts = player.getData(RpgData.COMBAT_PROGRESSION).contacts;
        long time = now(player);
        contacts.values().removeIf(c -> c.until <= time || c.enemy.get() == null || c.enemy.get().isRemoved());
        contacts.put(target.getUUID(), new Contact(new WeakReference<>(target), time + ClassResources.COMBAT_GRACE_TICKS));
    }
    /** Aura coverage can support an ally's recent fight. Support never creates another contact chain. */
    public static void supportAlly(ServerPlayer source, LivingEntity ally) {
        if (source == ally || !eligible(source) || !ally.hasData(RpgData.COMBAT_PROGRESSION)) { return; }
        long time = now(ally);
        for (var contact : ally.getData(RpgData.COMBAT_PROGRESSION).contacts.values()) {
            var target = contact.enemy.get();
            if (contact.until > time && target != null && target.isAlive() && !target.isRemoved()) { credit(source, target); }
        }
    }
    /** Supplied defenses qualify on the actual enemy attack, including a fully absorbed/dodged hit. */
    public static void defended(LivingEntity ally, DamageSource source, Collection<UUID> owners) {
        if (owners.isEmpty() || !(ally.level() instanceof ServerLevel)
                || !(source.getEntity() instanceof LivingEntity target) || Auras.allied(ally, target)) { return; }
        if (ally instanceof ServerPlayer player && eligible(player)) { contact(player, target); ClassResources.markCombat(player); }
        creditOwners(ally, target, owners);
    }
    private static void creditOwners(LivingEntity context, LivingEntity target, Collection<UUID> owners) {
        var server = ((ServerLevel) context.level()).getServer();
        for (var id : owners) {
            var owner = server.getPlayerList().getPlayer(id);
            if (owner != null) { credit(owner, target); }
        }
    }
    private static void hit(LivingEntity target, DamageSource source) {
        if (!(target.level() instanceof ServerLevel) || !(source.getEntity() instanceof LivingEntity attacker)
                || attacker == target || Auras.allied(attacker, target)) { return; }
        if (attacker instanceof ServerPlayer player && eligible(player)) { credit(player, target); contact(player, target); }
        if (attacker instanceof dev.covenant.rpg.skills.HunterPet pet && pet.getOwner() instanceof ServerPlayer owner
                && dev.covenant.rpg.skills.HunterPets.current(owner) == pet && !Auras.allied(owner, target)) {
            credit(owner, target); contact(owner, target);
        }
        creditOwners(attacker, target, Auras.supportOwners(attacker, false));
        if (attacker instanceof dev.covenant.rpg.skills.ElementalGolem golem && golem.getOwner() instanceof ServerPlayer owner
                && dev.covenant.rpg.skills.ElementalGolems.current(owner)==golem && !Auras.allied(owner,target)) {
            credit(owner,target); contact(owner,target);
        }
        creditOwners(target, target, Auras.supportOwners(target, true));
        var projectile = source.getDirectEntity();
        if (projectile != null && projectile.hasData(RpgData.PROJECTILE_COMBAT)) {
            creditOwners(attacker, target, projectile.getData(RpgData.PROJECTILE_COMBAT).supportOwners());
        }
        if (target instanceof ServerPlayer player && eligible(player)) { contact(player, attacker); }
        if (target instanceof dev.covenant.rpg.skills.HunterPet pet && pet.getOwner() instanceof ServerPlayer owner
                && dev.covenant.rpg.skills.HunterPets.current(owner) == pet) { ClassResources.markCombat(owner); contact(owner, attacker); }
        creditOwners(target, attacker, Auras.supportOwners(target, false));
        if (target instanceof dev.covenant.rpg.skills.ElementalGolem golem && golem.getOwner() instanceof ServerPlayer owner
                && dev.covenant.rpg.skills.ElementalGolems.current(owner)==golem) { ClassResources.markCombat(owner); contact(owner,attacker); }
    }
    private static void damaged(LivingDamageEvent.Post event) {
        try (var zone = Profiler.get().zone("covenant:combat_credit")) {
            if (AuraDamage.landedDamage(event) > 0) { hit(event.getEntity(), event.getSource()); }
        }
    }
    /** Called after the final cancelable death decision, before aura cleanup and native death side effects. */
    public static void died(LivingEntity target, DamageSource source) {
        try (var zone = Profiler.get().zone("covenant:combat_rewards")) {
            if (!enemy(target)) { return; }
            var state = target.getData(RpgData.COMBAT_PROGRESSION);
            if (state.paid) { return; }
            hit(target, source); // Native Post damage occurs after die; include the killing blow here.
            state.paid = true;
            var level = (ServerLevel) target.level();
            var recipients = new ArrayList<ServerPlayer>();
            long time = now(target);
            for (var entry : state.contributors.entrySet()) {
                var player = level.getServer().getPlayerList().getPlayer(entry.getKey());
                if (time - entry.getValue() < CONTRIBUTION_TICKS && player != null && eligible(player)) { recipients.add(player); }
            }
            if (recipients.isEmpty()) { return; }
            long total = ProgressionData.get(level.getServer()).reward(target);
            long share = total / recipients.size() + (total % recipients.size() == 0 ? 0 : 1);
            for (var player : recipients) {
                if (!(target instanceof WitherBoss && Progression.firstWither(player))) { Progression.awardCharacter(player, level, share); }
            }
            state.contributors.clear();
        }
    }
    private static void left(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) { return; }
        // The dragon finishes its native death animation by removal, bypassing LivingEntity.die.
        if (event.getEntity() instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon
                && dragon.getRemovalReason() == net.minecraft.world.entity.Entity.RemovalReason.KILLED && dragon.isDeadOrDying()) {
            died(dragon, dragon.damageSources().generic());
        }
        event.getEntity().removeData(RpgData.COMBAT_PROGRESSION);
    }
}
