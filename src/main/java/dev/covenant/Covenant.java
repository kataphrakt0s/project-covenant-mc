package dev.covenant;

import com.mojang.logging.LogUtils;
import dev.covenant.rpg.RpgData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/** Mod entry point. Gameplay systems register with NeoForge as they are introduced. */
@Mod(Covenant.MOD_ID)
public final class Covenant {
    public static final String MOD_ID = "covenant";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Covenant(IEventBus modBus) {
        RpgData.register(modBus);
        LOGGER.info("Project Covenant loaded (mod id: {})", MOD_ID);
    }
}
