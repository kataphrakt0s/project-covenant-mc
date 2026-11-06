package dev.covenant.rpg;

import dev.covenant.Covenant;
import dev.covenant.rpg.equipment.AffixPage;
import dev.covenant.rpg.skills.SkillState;
import dev.covenant.rpg.skills.SkillLoadout;
import dev.covenant.rpg.skills.SkillRuntime;
import dev.covenant.rpg.skills.BleedState;
import dev.covenant.rpg.skills.FrostSlows;
import dev.covenant.rpg.skills.ProjectileCombat;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Stable saved-data IDs. Changing these IDs or field meanings requires a migration. */
public final class RpgData {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Covenant.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Covenant.MOD_ID);

    public static final Supplier<AttachmentType<CharacterProfile>> PROFILE = ATTACHMENTS.register(
            "character_profile", () -> AttachmentType.builder(() -> CharacterProfile.UNINITIALIZED)
                    .serialize(CharacterProfile.MAP_CODEC)
                    .copyOnDeath()
                    .sync((holder, recipient) -> holder == recipient, CharacterProfile.STREAM_CODEC)
                    .build());
    public static final Supplier<DataComponentType<AffixPage>> AFFIX_PAGE = COMPONENTS.registerComponentType(
            "affix_page", builder -> builder.persistent(AffixPage.CODEC)
                    .networkSynchronized(AffixPage.STREAM_CODEC));
    public static final Supplier<AttachmentType<SkillState>> SKILLS = ATTACHMENTS.register(
            "skill_state", () -> AttachmentType.builder(() -> SkillState.EMPTY)
                    .serialize(SkillState.CODEC.fieldOf("skills")).copyOnDeath()
                    .sync((holder, recipient) -> holder == recipient, SkillState.STREAM_CODEC).build());
    public static final Supplier<AttachmentType<SkillRuntime>> SKILL_RUNTIME = ATTACHMENTS.register(
            "skill_runtime", () -> AttachmentType.builder(SkillRuntime::new).build());
    /** Two owner-only combo rows, updated only at transitions; no saved/copy state. */
    public static final Supplier<AttachmentType<dev.covenant.rpg.skills.ComboDisplay>> COMBO_DISPLAY = ATTACHMENTS.register(
            "combo_display", () -> AttachmentType.builder(() -> dev.covenant.rpg.skills.ComboDisplay.EMPTY)
                    .sync((holder, recipient) -> holder == recipient, dev.covenant.rpg.skills.ComboDisplay.STREAM_CODEC).build());
    /** Tracking-synced execution flag for input/control hooks; owner trajectory uses start/stop payloads. */
    public static final Supplier<AttachmentType<Boolean>> COMBO_MOVING = ATTACHMENTS.register(
            "combo_moving", () -> AttachmentType.builder(() -> false).sync(ByteBufCodecs.BOOL).build());
    /** Active Frost patch references; never saved, synced, or copied to a replacement entity. */
    public static final Supplier<AttachmentType<FrostSlows.Sources>> FROST_SOURCES = ATTACHMENTS.register(
            "frost_sources", () -> AttachmentType.builder(FrostSlows.Sources::new).build());
    /** Target-owned saved wounds; relative timers pause offline, and death never copies them. */
    public static final Supplier<AttachmentType<BleedState>> BLEED = ATTACHMENTS.register(
            "bleed", () -> AttachmentType.builder(BleedState::new)
                    .serialize(BleedState.CODEC.fieldOf("bleed")).build());
    /** Transient, tracking-synced arrow visual marker; never serialized or copied to player saves. */
    public static final Supplier<AttachmentType<Boolean>> SKILL_SHOT = ATTACHMENTS.register(
            "skill_shot", () -> AttachmentType.builder(() -> false).sync(ByteBufCodecs.BOOL).build());
    /** Cosmetic bow-draw deadline in level ticks, tracking-synced but never saved. */
    public static final Supplier<AttachmentType<Long>> BOW_DRAW_UNTIL = ATTACHMENTS.register(
            "bow_draw_until", () -> AttachmentType.builder(() -> 0L).sync(ByteBufCodecs.VAR_LONG).build());
    /** Cosmetic draw length in ticks; transient and tracking-synced, including scaled Aimed Shot casts. */
    public static final Supplier<AttachmentType<Long>> BOW_DRAW_DURATION = ATTACHMENTS.register(
            "bow_draw_duration", () -> AttachmentType.builder(() -> 6L).sync(ByteBufCodecs.VAR_LONG).build());
    public static final Supplier<AttachmentType<SkillLoadout>> SKILL_LOADOUT = ATTACHMENTS.register(
            "skill_loadout", () -> AttachmentType.builder(() -> SkillLoadout.EMPTY)
                    .serialize(SkillLoadout.CODEC.fieldOf("loadout")).copyOnDeath()
                    .sync((holder, recipient) -> holder == recipient, SkillLoadout.STREAM_CODEC).build());

    /** Temporary source/recipient ownership; no saved or copied derived buffs. */
    public static final Supplier<AttachmentType<dev.covenant.rpg.skills.Auras.State>> AURAS = ATTACHMENTS.register(
            "auras", () -> AttachmentType.builder(dev.covenant.rpg.skills.Auras.State::new).build());
    public static final Supplier<AttachmentType<dev.covenant.rpg.skills.AuraDamage.Combat>> AURA_COMBAT = ATTACHMENTS.register(
            "aura_combat", () -> AttachmentType.builder(dev.covenant.rpg.skills.AuraDamage.Combat::new).build());
    /** Only changes on freeze transitions, so client movement can use the authoritative state. */
    public static final Supplier<AttachmentType<Boolean>> AURA_FROZEN = ATTACHMENTS.register(
            "aura_frozen", () -> AttachmentType.builder(() -> false).sync(ByteBufCodecs.BOOL).build());
    /** Separate owner-only shield capacity; never masquerades as native absorption. */
    public static final Supplier<AttachmentType<Float>> FORTITUDE_CAPACITY = ATTACHMENTS.register(
            "fortitude_capacity", () -> AttachmentType.builder(() -> 0f)
                    .sync((holder, recipient) -> holder == recipient, ByteBufCodecs.FLOAT).build());
    /** Additive save support at the existing ID; loaded native arrow damage must not be multiplied again. */
    public static final Supplier<AttachmentType<Double>> AURA_PROJECTILE_POWER = ATTACHMENTS.register(
            "aura_projectile_power", () -> AttachmentType.builder(() -> 1d)
                    .serialize(ProjectileCombat.POWER_CODEC.fieldOf("power")).build());
    /** Captured Enchanting damage survives projectile unload/reload; never synced to clients. */
    public static final Supplier<AttachmentType<Float>> ENCHANTED_SHOT = ATTACHMENTS.register(
            "enchanted_shot", () -> AttachmentType.builder(() -> 0f)
                    .serialize(ProjectileCombat.ENCHANTMENT_CODEC.fieldOf("damage")).build());
    /** New optional saved projectile metadata; missing legacy data has ordinary-shot defaults. */
    public static final Supplier<AttachmentType<ProjectileCombat>> PROJECTILE_COMBAT = ATTACHMENTS.register(
            "projectile_combat", () -> AttachmentType.builder(ProjectileCombat::new)
                    .serialize(ProjectileCombat.CODEC.fieldOf("combat")).build());

    private RpgData() {}

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        COMPONENTS.register(modBus);
        NeoForge.EVENT_BUS.addListener(RpgData::onLogin);
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // Lazily creating the default is safe after loading existing saved attachments.
        // NeoForge also sends initial state on login, respawn, and dimension changes.
        event.getEntity().getData(PROFILE);
    }
}
