package dev.covenant.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.covenant.Covenant;
import dev.covenant.rpg.skills.Skills;
import dev.covenant.rpg.skills.SkillVisuals;
import net.minecraft.util.profiling.Profiler;
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
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) ->
                event.register(dev.covenant.rpg.equipment.Workbench.MENU.get(), WorkbenchScreen::new));
        modBus.addListener(ResourceHud::register);
        modBus.addListener(StatusHud::register);
        modBus.addListener(ProgressionHud::register);
        modBus.addListener(ClientDash::register);
        modBus.addListener(ClientSkillVisuals::register);
        modBus.addListener(CombatParticle::register);
        modBus.addListener(EnergyParticle::register);
        modBus.addListener(MovementParticle::register);
        modBus.addListener(ComboParticle::register);
        modBus.addListener(ReaverParticle::register);
        modBus.addListener(WeaverParticle::register);
        modBus.addListener(AuraParticle::register);
        modBus.addListener(AffixMaterialProperty::register);
        modBus.addListener(this::registerKeys);
        NeoForge.EVENT_BUS.addListener(this::onKey);
        NeoForge.EVENT_BUS.addListener(this::onMouseButton);
        NeoForge.EVENT_BUS.addListener(SkillKeyMappings::started);
        NeoForge.EVENT_BUS.addListener(SkillKeyMappings::tick);
        NeoForge.EVENT_BUS.addListener(SkillKeyMappings::interaction);
        NeoForge.EVENT_BUS.addListener(SkillVisuals::arrowTick);
        NeoForge.EVENT_BUS.addListener(TrapVisuals::tick);
        NeoForge.EVENT_BUS.addListener(ControlVisuals::tick);
        NeoForge.EVENT_BUS.addListener(AuraRangeVisuals::tick);
        NeoForge.EVENT_BUS.addListener(AuraRangeVisuals::extract);
        NeoForge.EVENT_BUS.addListener(AuraRangeVisuals::submit);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, ClientFarming::use);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Skills.HUNTER_PET.get(), HunterPetRenderer::new);
        event.registerEntityRenderer(Skills.ELEMENTAL_GOLEM.get(), ElementalGolemRenderer::new);
        event.registerEntityRenderer(Skills.ARCANE_BOLT.get(), context -> new BoltRenderer<>(context, BoltRenderer.Kind.ARCANE));
        event.registerEntityRenderer(Skills.FIRE_BOLT.get(), context -> new BoltRenderer<>(context, BoltRenderer.Kind.FIRE));
        event.registerEntityRenderer(Skills.ICE_BOLT.get(), context -> new BoltRenderer<>(context, BoltRenderer.Kind.ICE));
        event.registerEntityRenderer(Skills.TRAP.get(), TrapRenderer::new);
        event.registerEntityRenderer(Skills.RAGE_STORM.get(), RageStormRenderer::new);
        event.registerEntityRenderer(Skills.GROUND_SKILL.get(), GroundSkillRenderer::new);
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
        try (var zone = Profiler.get().zone("covenant:keyboard_input")) {
            // Only the initial press opens the wheel. OS repeats must not reopen it after Escape.
            if (event.getAction() == GLFW.GLFW_PRESS && canOpen()
                    && radialMenu.isConflictContextAndModifierActive() && radialMenu.matches(event.getKeyEvent())) {
                Minecraft.getInstance().setScreen(new RadialMenuScreen(radialMenu, false, event.getKey()));
            } else if (event.getAction() == GLFW.GLFW_PRESS && canOpen()) {
                SkillKeyMappings.press(InputConstants.getKey(event.getKeyEvent()));
            }
        }
    }

    private void onMouseButton(InputEvent.MouseButton.Post event) {
        try (var zone = Profiler.get().zone("covenant:mouse_input")) {
            if (event.getAction() == GLFW.GLFW_PRESS && canOpen()
                    && radialMenu.isActiveAndMatches(InputConstants.Type.MOUSE.getOrCreate(event.getButton()))) {
                Minecraft.getInstance().setScreen(new RadialMenuScreen(radialMenu, true, event.getButton()));
            } else if (event.getAction() == GLFW.GLFW_PRESS && canOpen()) {
                SkillKeyMappings.press(InputConstants.Type.MOUSE.getOrCreate(event.getButton()));
            }
        }
    }
}
