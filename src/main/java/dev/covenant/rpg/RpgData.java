package dev.covenant.rpg;

import dev.covenant.Covenant;
import dev.covenant.rpg.equipment.AffixPage;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
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
