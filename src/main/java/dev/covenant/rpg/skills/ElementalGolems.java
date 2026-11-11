package dev.covenant.rpg.skills;

import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.stats.RpgStats;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

/** One server-owned companion; native snapshots follow the same lifecycle as Hunter's Pet. */
public final class ElementalGolems {
    public static final class State {
        public static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(
                CompoundTag.CODEC.optionalFieldOf("golem").forGetter(State::snapshot),
                ExtraCodecs.NON_NEGATIVE_LONG.optionalFieldOf("cooldown_ticks",600L).forGetter(s -> s.cooldown)
        ).apply(i,State::new));
        ElementalGolem golem;
        CompoundTag stored;
        long cooldown = 600, retryAt, nextCycle;
        boolean spawning;
        ElementalGolem.Form applied;
        ElementalGolemData.Balance appliedBalance;
        public State() {}
        private State(Optional<CompoundTag> saved,long cooldown) { stored=saved.map(CompoundTag::copy).orElse(null); this.cooldown=cooldown; }
        private Optional<CompoundTag> snapshot() {
            return golem != null && golem.isAlive() && !golem.isRemoved() ? Optional.of(Companions.capture(golem)) : Optional.ofNullable(stored);
        }
        public boolean active() { return golem != null || stored != null; }
    }
    private static final Identifier MODIFIER = Identifier.fromNamespaceAndPath("covenant","elemental_golem");
    private ElementalGolems() {}

    public static void register(IEventBus bus) {
        bus.addListener((EntityAttributeCreationEvent event) -> event.put(Skills.ELEMENTAL_GOLEM.get(), ElementalGolem.createAttributes().build()));
        NeoForge.EVENT_BUS.addListener(ElementalGolems::commands);
        NeoForge.EVENT_BUS.addListener(ElementalGolemData::register);
    }
    public static ElementalGolem current(ServerPlayer owner) {
        return owner.hasData(RpgData.ELEMENTAL_GOLEM) ? owner.getData(RpgData.ELEMENTAL_GOLEM).golem : null;
    }

    public static boolean assigned(ServerPlayer owner) {
        var loadout = owner.getData(RpgData.SKILL_LOADOUT);
        return owner.isAlive() && !owner.isSpectator() && !owner.hasDisconnected()
                && PlayerProfiles.getSelectedClass(owner).orElse(null) == PlayerClass.WEAVER
                && loadout.learned().contains(SkillDefinition.ELEMENTAL_GOLEM.id())
                && loadout.bindings().containsValue(SkillDefinition.ELEMENTAL_GOLEM.id());
    }
    public static boolean active(ServerPlayer owner) { return owner.hasData(RpgData.ELEMENTAL_GOLEM) && owner.getData(RpgData.ELEMENTAL_GOLEM).active(); }

    /** Compatibility entry point for operator previews; uses the ordinary paid skill boundary. */
    public static Component summon(ServerPlayer owner) {
        return summon(owner, ElementalGolem.Form.ARCANE);
    }
    public static Component summon(ServerPlayer owner, ElementalGolem.Form form) {
        ClassResources.requireServer(owner);
        if (active(owner) || owner.getData(RpgData.ELEMENTAL_GOLEM).spawning) { return message("unavailable"); }
        var result = Skills.activate(owner, SkillDefinition.ELEMENTAL_GOLEM);
        var golem = current(owner);
        if (golem != null) { golem.setForm(form); return message("summoned"); }
        return result;
    }
    static Component activate(ServerPlayer owner, SkillDefinition skill) {
        var state = owner.getData(RpgData.ELEMENTAL_GOLEM);
        var runtime = owner.getData(RpgData.SKILL_RUNTIME);
        if (state.active()) {
            if (state.golem == null) { return Skills.message("unavailable"); }
            if (ClassResources.now(owner) < state.nextCycle) { return Skills.message("busy"); }
            state.golem.setForm(state.golem.form().next());
            state.nextCycle = ClassResources.now(owner) + 5;
            runtime.nextActivation = ClassResources.now(owner) + 1;
            SkillSounds.release(owner,skill);
            return Component.translatable("commands.covenant.golem.form", Component.translatable("entity.covenant.elemental_golem.form."+state.golem.form().id()));
        }
        double cost = RpgStats.scaleSkillValue(owner,RpgStats.RESOURCE_COST,skill.cost());
        if (!ClassResources.canAfford(owner,cost)) { return Skills.message("resource"); }
        // Snapshot before applying the new Arcane benefit, as with Hunter's Pet.
        long cooldown = Math.max(1,(long)Math.ceil(RpgStats.scaleSkillValue(owner,RpgStats.COOLDOWN,skill.cooldownTicks())));
        state.spawning = true;
        runtime.executing = true;
        try {
            var golem = new ElementalGolem(Skills.ELEMENTAL_GOLEM.get(), owner.level());
            golem.initialize(owner,ElementalGolemData.get(owner.level().getServer()));
            if (!Companions.place(owner, golem)) { return Skills.message("destination"); }
            if (!owner.level().addFreshEntity(golem) || golem.isRemoved() || !golem.isAlive() || !assigned(owner)
                    || golem.level() != owner.level() || state.active()) {
                golem.discard(); return Skills.message("unavailable");
            }
            if (!ClassResources.consume(owner,cost)) { golem.discard(); return Skills.message("resource"); }
            state.golem = golem;
            state.cooldown = cooldown;
            state.nextCycle = ClassResources.now(owner) + 5;
            runtime.nextActivation = ClassResources.now(owner) + 1;
            refresh(owner);
            SkillSounds.release(owner,skill);
            return Skills.message("used",skill.displayName(),cost);
        } finally { state.spawning = false; runtime.executing = false; }
    }
    public static boolean dismiss(ServerPlayer owner) {
        ClassResources.requireServer(owner);
        if (!active(owner)) { return false; }
        var state = owner.getData(RpgData.ELEMENTAL_GOLEM);
        var golem = state.golem;
        state.golem = null; state.stored = null;
        owner.setData(RpgData.SKILLS,owner.getData(RpgData.SKILLS).withCooldown(SkillDefinition.ELEMENTAL_GOLEM.id(),ClassResources.now(owner)+state.cooldown));
        refresh(owner);
        if (golem != null && golem.isAlive()) { golem.discard(); }
        return true;
    }
    static void defeated(ElementalGolem golem) {
        var owner = golem.serverOwner();
        if (owner != null && current(owner) == golem) { dismiss(owner); }
    }
    public static void store(ServerPlayer owner) {
        ClassResources.requireServer(owner);
        var golem = current(owner);
        if (golem == null) { return; }
        var state = owner.getData(RpgData.ELEMENTAL_GOLEM);
        state.stored = golem.isAlive() ? Companions.capture(golem) : null;
        state.golem = null;
        refresh(owner);
        golem.discard();
    }
    static void removed(ElementalGolem golem, Entity.RemovalReason reason) {
        var owner = golem.serverOwner();
        if (owner == null || current(owner) != golem) { return; }
        if (reason == Entity.RemovalReason.UNLOADED_TO_CHUNK || reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
            var state=owner.getData(RpgData.ELEMENTAL_GOLEM);
            state.stored=golem.isAlive() ? Companions.capture(golem) : null;
            state.golem=null;
            refresh(owner);
        }
        else { dismiss(owner); }
    }
    public static void lifecycle(ServerPlayer owner, SkillLifecycle.Reason reason) {
        switch (reason) {
            case LOGOUT, DIMENSION_CHANGE -> store(owner);
            case CLASS_CHANGE, CONFIRMED_DEATH -> dismiss(owner);
        }
    }
    public static void reconcile(ServerPlayer owner) {
        if (active(owner) && !assigned(owner)) { dismiss(owner); }
    }
    /** Existing player cadence only retries stored companions; production never loads chunks. */
    static void tick(ServerPlayer owner) {
        if (!owner.hasData(RpgData.ELEMENTAL_GOLEM)) { return; }
        reconcile(owner);
        var state = owner.getData(RpgData.ELEMENTAL_GOLEM);
        if (state.golem != null || state.stored == null || !assigned(owner) || ClassResources.now(owner) < state.retryAt) { return; }
        state.retryAt = ClassResources.now(owner)+20;
        var golem = new ElementalGolem(Skills.ELEMENTAL_GOLEM.get(),owner.level());
        var saved = state.stored;
        golem.load(TagValueInput.create(ProblemReporter.DISCARDING,owner.registryAccess(),saved));
        golem.setOwner(owner); golem.setTarget(null); golem.setLastHurtByMob(null);
        golem.setDeltaMovement(Vec3.ZERO); golem.fallDistance=0;
        if (!golem.isAlive()) { dismiss(owner); return; }
        if (!Companions.place(owner,golem)) { return; }
        if (!owner.level().addFreshEntity(golem) || golem.isRemoved() || !golem.isAlive() || !assigned(owner)
                || golem.level()!=owner.level() || state.golem!=null || state.stored!=saved) { golem.discard(); return; }
        state.golem=golem; state.stored=null;
        state.nextCycle=ClassResources.now(owner)+5;
        refresh(owner);
    }
    /** Owner-only, spherical feet distance; no entity search or line-of-sight requirement. */
    public static ElementalGolem.Form benefit(ServerPlayer owner) {
        var golem=current(owner);
        if (golem==null || !golem.isAlive() || golem.isRemoved() || !assigned(owner) || golem.level()!=owner.level()) { return null; }
        double range=ElementalGolemData.get(owner.level().getServer()).range();
        return golem.distanceToSqr(owner)<=range*range ? golem.form() : null;
    }
    public static double spellBonus(net.minecraft.world.entity.LivingEntity owner) {
        return owner instanceof ServerPlayer player && benefit(player)==ElementalGolem.Form.FIRE
                ? ElementalGolemData.get(player.level().getServer()).fireDamage() : 0;
    }
    /** Cheap owner-local range check each player tick; attributes/HUD mutate only on transitions/reload. */
    public static void refresh(ServerPlayer owner) {
        ClassResources.requireServer(owner);
        if (!owner.hasData(RpgData.ELEMENTAL_GOLEM)) { return; }
        var state=owner.getData(RpgData.ELEMENTAL_GOLEM);
        var form=benefit(owner);
        var b=ElementalGolemData.get(owner.level().getServer());
        if (state.applied==form && state.appliedBalance==b) { return; }
        // Read before changing capacity, including first-use resource initialization.
        double mana=ClassResources.current(owner,PlayerClass.WEAVER);
        state.applied=form; state.appliedBalance=b;
        modifier(owner,RpgStats.CAST_TIME,form==ElementalGolem.Form.ICE ? -b.iceCast() : 0);
        modifier(owner,RpgStats.COOLDOWN,form==ElementalGolem.Form.ICE ? -b.iceCooldown() : 0);
        modifier(owner,RpgStats.SKILL_DURATION,form==ElementalGolem.Form.LIGHTNING ? b.lightningDuration() : 0);
        modifier(owner,RpgStats.MANA_CAPACITY,form==ElementalGolem.Form.LIGHTNING ? b.lightningMana() : 0);
        modifier(owner,RpgStats.MANA_REGENERATION,form==ElementalGolem.Form.ARCANE ? b.arcaneRegen() : 0);
        modifier(owner,RpgStats.RESOURCE_COST,form==ElementalGolem.Form.ARCANE ? -b.arcaneCost() : 0);
        ClassResources.set(owner,PlayerClass.WEAVER,mana);
        BuffDisplay.sync(owner);
    }
    private static void modifier(ServerPlayer owner, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,double amount) {
        var instance=owner.getAttribute(attribute);
        if (amount==0) { instance.removeModifier(MODIFIER); }
        else { instance.addOrUpdateTransientModifier(new AttributeModifier(MODIFIER,amount,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)); }
    }

    private static Component message(String key) { return Component.translatable("commands.covenant.golem." + key); }
    private static void commands(RegisterCommandsEvent event) {
        var summon = Commands.literal("summon").executes(c -> {
            var result = summon(c.getSource().getPlayerOrException());
            c.getSource().sendSuccess(() -> result, false); return 1;
        });
        var appearance = Commands.literal("form");
        for (var form : ElementalGolem.Form.values()) {
            summon.then(Commands.literal(form.id()).executes(c -> {
                var result = summon(c.getSource().getPlayerOrException(), form);
                c.getSource().sendSuccess(() -> result, false); return 1;
            }));
            appearance.then(Commands.literal(form.id()).executes(c -> {
                var golem = current(c.getSource().getPlayerOrException());
                if (golem == null || !golem.isAlive()) { c.getSource().sendFailure(message("absent")); return 0; }
                golem.setForm(form);
                c.getSource().sendSuccess(() -> Component.translatable("commands.covenant.golem.form",
                        Component.translatable("entity.covenant.elemental_golem.form." + form.id())), false);
                return 1;
            }));
        }
        event.getDispatcher().register(Commands.literal("covenant")
                .then(Commands.literal("golem").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(summon).then(appearance)
                        .then(Commands.literal("dismiss").executes(c -> {
                            boolean dismissed = dismiss(c.getSource().getPlayerOrException());
                            c.getSource().sendSuccess(() -> message(dismissed ? "dismissed" : "absent"), false); return dismissed ? 1 : 0;
                        }))));
    }
}
