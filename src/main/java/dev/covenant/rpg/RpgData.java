package dev.covenant.rpg;

import dev.covenant.Covenant;
import dev.covenant.rpg.equipment.AffixPage;
import dev.covenant.rpg.skills.SkillState;
import dev.covenant.rpg.skills.SkillLoadout;
import dev.covenant.rpg.skills.SkillRuntime;
import dev.covenant.rpg.skills.BleedState;
import dev.covenant.rpg.skills.FrostSlows;
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
