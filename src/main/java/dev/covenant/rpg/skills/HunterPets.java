package dev.covenant.rpg.skills;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.stats.RpgStats;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** One owner-local companion. No entity index, offline tick, world-saved copy or chunk tickets. */
public final class HunterPets {
    /** The codec captures the live native wolf only when the owner is saved. The entity reference
     * and retry deadline are runtime-only; native health/status/fire/attack state is stored once. */
    public static final class State {
        public static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(
                CompoundTag.CODEC.optionalFieldOf("pet").forGetter(State::snapshot),
                ExtraCodecs.NON_NEGATIVE_LONG.optionalFieldOf("cooldown_ticks", 600L).forGetter(s -> s.cooldown)
        ).apply(i, State::new));
        HunterPet pet;
        CompoundTag stored;
        long cooldown = 600, retryAt;
        public State() {}
        private State(Optional<CompoundTag> saved, long cooldown) { stored = saved.map(CompoundTag::copy).orElse(null); this.cooldown = cooldown; }
        private Optional<CompoundTag> snapshot() {
            if (pet != null && pet.isAlive() && !pet.isRemoved()) { return Optional.of(capture(pet)); }
            return Optional.ofNullable(stored);
        }
        public boolean active() { return pet != null || stored != null; }
    }

    private HunterPets() {}
    public static void register(IEventBus bus) {
        bus.addListener((EntityAttributeCreationEvent event) -> event.put(Skills.HUNTER_PET.get(), HunterPet.createAttributes().build()));
        NeoForge.EVENT_BUS.addListener(HunterPetData::register);
        NeoForge.EVENT_BUS.addListener(HunterPets::damaged);
    }
    public static boolean assigned(ServerPlayer player) {
        var loadout = player.getData(RpgData.SKILL_LOADOUT);
        return player.isAlive() && !player.isSpectator() && !player.hasDisconnected()
                && PlayerProfiles.getSelectedClass(player).orElse(null) == PlayerClass.STALKER
                && loadout.learned().contains(SkillDefinition.HUNTERS_PET.id())
                && loadout.bindings().containsValue(SkillDefinition.HUNTERS_PET.id());
    }
    /** Returns the sole accepted, currently materialized companion. */
    public static HunterPet current(ServerPlayer player) {
        return player.hasData(RpgData.HUNTER_PET) ? player.getData(RpgData.HUNTER_PET).pet : null;
    }
    public static boolean active(ServerPlayer player) { return player.hasData(RpgData.HUNTER_PET) && player.getData(RpgData.HUNTER_PET).active(); }

    static Component activate(ServerPlayer player, SkillDefinition skill) {
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        runtime.executing = true;
        try (var zone = Profiler.get().zone("covenant:pet_activate")) {
            if (active(player)) {
                dismiss(player);
                runtime.nextActivation = ClassResources.now(player) + 1;
                return Skills.message("pet_dismissed");
            }
            double cost = RpgStats.scaleSkillValue(player, RpgStats.RESOURCE_COST, skill.cost());
            if (!ClassResources.canAfford(player, cost)) { return Skills.message("resource"); }
            var state = player.getData(RpgData.HUNTER_PET);
            var pet = new HunterPet(Skills.HUNTER_PET.get(), player.level());
            pet.initialize(player, HunterPetData.get(player.level().getServer()));
            if (!place(player, pet)) { return Skills.message("destination"); }
            if (!player.level().addFreshEntity(pet) || pet.isRemoved() || !pet.isAlive() || !assigned(player)
                    || pet.level() != player.level() || state.active()) {
                pet.discard(); return Skills.message("unavailable");
            }
            if (!ClassResources.consume(player, cost)) { pet.discard(); return Skills.message("resource"); }
            state.pet = pet;
            state.cooldown = Math.max(1, (long) Math.ceil(RpgStats.scaleSkillValue(player, RpgStats.COOLDOWN, skill.cooldownTicks())));
            runtime.nextActivation = ClassResources.now(player) + 1;
            return Skills.message("used", skill.displayName(), cost);
        } finally { runtime.executing = false; }
    }

    /** Dismissal and defeat share the summon-time scaled cooldown; replacing state happens before native hooks. */
    public static void dismiss(ServerPlayer player) {
        if (!active(player)) { return; }
        var state = player.getData(RpgData.HUNTER_PET);
        var pet = state.pet;
        state.pet = null; state.stored = null;
        long until = ClassResources.now(player) + state.cooldown;
        player.setData(RpgData.SKILLS, player.getData(RpgData.SKILLS).withCooldown(SkillDefinition.HUNTERS_PET.id(), until));
        if (pet != null && pet.isAlive()) { pet.discard(); }
    }
    static void defeated(HunterPet pet) {
        pet.triggerAnim("main", "death");
        var player = pet.serverOwner();
        if (player != null && current(player) == pet) { dismiss(player); }
    }
    /** Store before removal, so removal callbacks cannot restore or duplicate the old instance. */
    public static void store(ServerPlayer player) {
        var pet = current(player);
        if (pet == null) { return; }
        var state = player.getData(RpgData.HUNTER_PET);
        state.stored = pet.isAlive() ? capture(pet) : null;
        state.pet = null;
        pet.discard();
    }
    static void removed(HunterPet pet, Entity.RemovalReason reason) {
        var player = pet.serverOwner();
        if (player == null || current(player) != pet) { return; }
        if (reason == Entity.RemovalReason.UNLOADED_TO_CHUNK || reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
            var state = player.getData(RpgData.HUNTER_PET);
            state.stored = pet.isAlive() ? capture(pet) : null;
            state.pet = null;
        }
        else { dismiss(player); }
    }
    public static void lifecycle(ServerPlayer player, SkillLifecycle.Reason reason) {
        switch (reason) {
            case LOGOUT, DIMENSION_CHANGE -> store(player);
            case CLASS_CHANGE, CONFIRMED_DEATH -> dismiss(player);
        }
    }
    public static void reconcile(ServerPlayer player) {
        if (active(player) && !assigned(player)) { dismiss(player); }
    }
    /** Called by the existing player cadence. Only a stored active companion retries, at most once per second. */
    static void tick(ServerPlayer player) {
        if (!player.hasData(RpgData.HUNTER_PET)) { return; }
        reconcile(player);
        var state = player.getData(RpgData.HUNTER_PET);
        if (state.pet != null || state.stored == null || !assigned(player) || ClassResources.now(player) < state.retryAt) { return; }
        state.retryAt = ClassResources.now(player) + 20;
        try (var zone = Profiler.get().zone("covenant:pet_restore")) {
            var pet = new HunterPet(Skills.HUNTER_PET.get(), player.level());
            var saved = state.stored;
            pet.load(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), saved));
            pet.setOwner(player); pet.setTarget(null); pet.setLastHurtByMob(null);
            pet.setDeltaMovement(Vec3.ZERO); pet.fallDistance = 0;
            if (!pet.isAlive()) { dismiss(player); return; }
            if (!place(player, pet)) { return; }
            if (!player.level().addFreshEntity(pet) || pet.isRemoved() || !pet.isAlive() || !assigned(player)
                    || pet.level() != player.level() || state.pet != null || state.stored != saved) {
                pet.discard(); return;
            }
            state.pet = pet;
            state.stored = null;
        }
    }

    private static CompoundTag capture(HunterPet pet) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, pet.registryAccess());
        pet.saveWithoutId(output);
        return output.buildResult();
    }
    /** At most 75 already-loaded candidates within two blocks horizontally and one vertically. */
    static boolean place(ServerPlayer owner, HunterPet pet) {
        var center = owner.blockPosition();
        for (int dy : new int[] {0, 1, -1}) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx == 0 && dz == 0) { continue; }
                    var pos = center.offset(dx, dy, dz);
                    if (safe(owner, pet, pos)) {
                        pet.snapTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, owner.getYRot(), 0);
                        return true;
                    }
                }
            }
        }
        return false;
    }
    private static boolean safe(ServerPlayer owner, HunterPet pet, BlockPos pos) {
        var level = owner.level();
        var box = pet.getDimensions(pet.getPose()).makeBoundingBox(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        if (!level.hasChunksAt(BlockPos.containing(box.minX, box.minY - 1, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))
                || !level.getWorldBorder().isWithinBounds(box) || !level.isInWorldBounds(pos)) { return false; }
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                && !level.containsAnyLiquid(box) && level.noCollision(pet, box);
    }
    static void teleportToOwner(HunterPet pet) {
        if (pet.getOwner() instanceof ServerPlayer player && place(player, pet)) {
            pet.getNavigation().stop(); pet.setTarget(null); pet.setDeltaMovement(Vec3.ZERO); pet.fallDistance = 0;
        }
    }
    private static void damaged(LivingDamageEvent.Post event) {
        if (AuraDamage.landedDamage(event) <= 0) { return; }
        var source = event.getSource().getEntity();
        var target = event.getEntity();
        if (source instanceof ServerPlayer owner) {
            var pet = current(owner);
            if (pet != null && pet.canAttack(target)) { pet.setTarget(target); }
        }
        if (source instanceof net.minecraft.world.entity.LivingEntity attacker) {
            var pet = target instanceof ServerPlayer owner ? current(owner) : target instanceof HunterPet companion ? companion : null;
            if (pet != null && pet.canAttack(attacker)) { pet.setTarget(attacker); }
        }
    }
}
