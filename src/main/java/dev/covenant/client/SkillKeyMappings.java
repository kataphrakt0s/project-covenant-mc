package dev.covenant.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.skills.SkillLoadout;
import dev.covenant.rpg.skills.SkillNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.lifecycle.ClientStartedEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Eight rebindable logical slots. Dispatch only initial presses, with modified mappings taking priority. */
final class SkillKeyMappings {
    private static final KeyMapping[] SLOTS = new KeyMapping[SkillLoadout.SLOT_COUNT];

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
        var minecraft = Minecraft.getInstance();
        // A held modifier must never also fire the unmodified slot sharing the same key.
        for (int pass = 0; pass < 2; pass++) {
            for (int slot = 0; slot < SLOTS.length; slot++) {
                var binding = SLOTS[slot];
                if ((binding.getKeyModifier() != KeyModifier.NONE) != (pass == 0)) { continue; }
                if (binding.isActiveAndMatches(key)) {
                    if (minecraft.player.getData(RpgData.SKILL_LOADOUT).bindings().containsKey(slot)) {
                        ClientPacketDistributor.sendToServer(new SkillNetworking.Use(slot));
                    }
                    return;
                }
            }
        }
    }
}
