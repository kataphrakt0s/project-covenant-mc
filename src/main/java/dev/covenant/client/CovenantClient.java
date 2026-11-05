package dev.covenant.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.covenant.Covenant;
import dev.covenant.rpg.skills.Skills;
import dev.covenant.rpg.skills.SkillVisuals;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/** Physical-client entry point; common code must not reference this class. */
@Mod(value = Covenant.MOD_ID, dist = Dist.CLIENT)
public final class CovenantClient {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath(Covenant.MOD_ID, "controls"));
    private final KeyMapping radialMenu = new KeyMapping("key.covenant.radial_menu",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);

    public CovenantClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SkillClientConfig.SPEC);
        modBus.addListener(CovenantClient::registerRenderers);
        modBus.addListener(ResourceHud::register);
        modBus.addListener(this::registerKeys);
        NeoForge.EVENT_BUS.addListener(this::onKey);
        NeoForge.EVENT_BUS.addListener(this::onMouseButton);
        NeoForge.EVENT_BUS.addListener(SkillKeyMappings::started);
        NeoForge.EVENT_BUS.addListener(SkillVisuals::arrowTick);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Skills.ARCANE_BOLT.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(Skills.TRAP.get(), context -> new ThrownItemRenderer<>(context, 0.8f, true));
    }

    private void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(radialMenu);
        SkillKeyMappings.register(event, CATEGORY);
    }

    private boolean canOpen() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.screen == null && minecraft.getOverlay() == null
                && minecraft.player != null && minecraft.player.isAlive()
                && minecraft.level != null && minecraft.isWindowActive();
    }

    private void onKey(InputEvent.Key event) {
        // Only the initial press opens the wheel. OS repeats must not reopen it after Escape.
        if (event.getAction() == GLFW.GLFW_PRESS && canOpen()
                && radialMenu.isConflictContextAndModifierActive() && radialMenu.matches(event.getKeyEvent())) {
            Minecraft.getInstance().setScreen(new RadialMenuScreen(radialMenu, false, event.getKey()));
        } else if (event.getAction() == GLFW.GLFW_PRESS && canOpen()) {
            SkillKeyMappings.press(InputConstants.getKey(event.getKeyEvent()));
        }
    }

    private void onMouseButton(InputEvent.MouseButton.Post event) {
        if (event.getAction() == GLFW.GLFW_PRESS && canOpen()
                && radialMenu.isActiveAndMatches(InputConstants.Type.MOUSE.getOrCreate(event.getButton()))) {
            Minecraft.getInstance().setScreen(new RadialMenuScreen(radialMenu, true, event.getButton()));
        } else if (event.getAction() == GLFW.GLFW_PRESS && canOpen()) {
            SkillKeyMappings.press(InputConstants.Type.MOUSE.getOrCreate(event.getButton()));
        }
    }
}
