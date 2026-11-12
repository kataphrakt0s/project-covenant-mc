package dev.covenant.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.skills.SkillLoadout;
import dev.covenant.rpg.skills.SkillNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.profiling.Profiler;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.lifecycle.ClientStartedEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Eight rebindable logical slots. Dispatch only initial presses, with modified mappings taking priority. */
final class SkillKeyMappings {
    private static final KeyMapping[] SLOTS = new KeyMapping[SkillLoadout.SLOT_COUNT];
    private static int channelSlot=-1, heartbeat;
    private static long channelSequence;
    private static InputConstants.Key channelKey;

    private SkillKeyMappings() {}

    static void register(RegisterKeyMappingsEvent event, KeyMapping.Category category) {
        int[] keys = {GLFW.GLFW_KEY_Q, GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_C};
        for (int slot = 0; slot < SLOTS.length; slot++) {
            SLOTS[slot] = new KeyMapping("key.covenant.skill_slot_" + (slot + 1), KeyConflictContext.IN_GAME,
                    slot < 4 ? KeyModifier.NONE : KeyModifier.SHIFT, InputConstants.Type.KEYSYM, keys[slot % 4], category);
            event.register(SLOTS[slot]);
        }
    }

    static Component label(int slot) { return SLOTS[slot].getTranslatedKeyMessage(); }

    static void started(ClientStartedEvent event) {
        var options = Minecraft.getInstance().options;
        boolean changed = false;
        if (SkillClientConfig.UNBIND_DROP_ITEM.get()
                && options.keyDrop.getKey().equals(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_Q))) {
            options.keyDrop.setKeyModifierAndCode(KeyModifier.NONE, InputConstants.UNKNOWN);
            changed = true;
        }
        if (SkillClientConfig.UNBIND_SWAP_HANDS.get()
                && options.keySwapOffhand.getKey().equals(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F))) {
            options.keySwapOffhand.setKeyModifierAndCode(KeyModifier.NONE, InputConstants.UNKNOWN);
            changed = true;
        }
        if (changed) {
            KeyMapping.resetMapping();
            options.save();
        }
    }

    static void press(InputConstants.Key key) {
        try (var zone = Profiler.get().zone("covenant:skill_key_input")) {
            var minecraft = Minecraft.getInstance();
            // A held modifier must never also fire the unmodified slot sharing the same key.
            for (int pass = 0; pass < 2; pass++) {
                for (int slot = 0; slot < SLOTS.length; slot++) {
                    var binding = SLOTS[slot];
                    if ((binding.getKeyModifier() != KeyModifier.NONE) != (pass == 0)) { continue; }
                    if (binding.isActiveAndMatches(key)) {
                        if (minecraft.player.getData(RpgData.SKILL_LOADOUT).bindings().containsKey(slot)) {
                            var id=minecraft.player.getData(RpgData.SKILL_LOADOUT).bindings().get(slot);
                            if (dev.covenant.rpg.skills.SkillDefinition.BARRAGE.id().equals(id)) {
                                stopChannel(); channelSlot=slot; channelKey=binding.getKey(); heartbeat=0;
                                ClientPacketDistributor.sendToServer(new SkillNetworking.ChannelInput(slot,++channelSequence,true,true));
                            } else { ClientPacketDistributor.sendToServer(new SkillNetworking.Use(slot)); }
                        }
                        return;
                    }
                }
            }
        }
    }

    static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        if (channelSlot<0) { return; }
        var mc=Minecraft.getInstance();
        if (mc.player==null || mc.level==null) { channelSlot=-1; return; }
        boolean down=channelKey.getType()==InputConstants.Type.MOUSE
                ? GLFW.glfwGetMouseButton(mc.getWindow().handle(),channelKey.getValue())==GLFW.GLFW_PRESS
                : channelKey.getType()==InputConstants.Type.KEYSYM
                ? GLFW.glfwGetKey(mc.getWindow().handle(),channelKey.getValue())==GLFW.GLFW_PRESS : SLOTS[channelSlot].isDown();
        if (!down || !mc.player.isAlive() || mc.screen!=null || mc.getOverlay()!=null || !mc.isWindowActive()
                || !SLOTS[channelSlot].getKey().equals(channelKey) || !SLOTS[channelSlot].isConflictContextAndModifierActive()
                || !dev.covenant.rpg.skills.SkillDefinition.BARRAGE.id().equals(mc.player.getData(RpgData.SKILL_LOADOUT).bindings().get(channelSlot))) {
            stopChannel();
        } else if (++heartbeat>=5) {
            heartbeat=0;
            ClientPacketDistributor.sendToServer(new SkillNetworking.ChannelInput(channelSlot,channelSequence,false,true));
        }
    }
    private static void stopChannel() {
        if (channelSlot>=0 && Minecraft.getInstance().getConnection()!=null) {
            ClientPacketDistributor.sendToServer(new SkillNetworking.ChannelInput(channelSlot,channelSequence,false,false));
        }
        channelSlot=-1;
    }
    /** A mouse-bound channel must not also trigger vanilla's attack/use action on that same button. */
    static void interaction(net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered event) {
        if (channelSlot<0) { return; }
        var mc=Minecraft.getInstance();
        boolean conflict=event.isUseItem() && mc.options.keyUse.getKey().equals(channelKey)
                || event.isAttack() && mc.options.keyAttack.getKey().equals(channelKey);
        if (conflict) { event.setCanceled(true); event.setSwingHand(false); }
    }
}
