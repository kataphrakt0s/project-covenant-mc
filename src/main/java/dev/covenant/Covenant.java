package dev.covenant;

import com.mojang.logging.LogUtils;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.ClassCommands;
import dev.covenant.rpg.equipment.AffixCommands;
import dev.covenant.rpg.stats.RpgStats;
import dev.covenant.rpg.stats.StatCommands;
import dev.covenant.rpg.skills.Skills;
import dev.covenant.rpg.skills.SkillConfig;
import dev.covenant.rpg.skills.SkillNetworking;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/** Mod entry point. Gameplay systems register with NeoForge as they are introduced. */
@Mod(Covenant.MOD_ID)
public final class Covenant {
    public static final String MOD_ID = "covenant";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Covenant(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, SkillConfig.SPEC);
        RpgData.register(modBus);
        dev.covenant.rpg.skills.CombatParticles.register(modBus);
        dev.covenant.rpg.equipment.Workbench.register(modBus);
        dev.covenant.rpg.progression.CombatRespecItem.register(modBus);
        dev.covenant.rpg.progression.ProfessionRespecItem.register(modBus);
        RpgStats.register(modBus);
        Skills.register(modBus);
        modBus.addListener(SkillNetworking::register);
        modBus.addListener(dev.covenant.rpg.progression.ProfessionNetworking::register);
        NeoForge.EVENT_BUS.addListener(ClassCommands::register);
        NeoForge.EVENT_BUS.addListener(StatCommands::register);
        NeoForge.EVENT_BUS.addListener(AffixCommands::register);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,
                dev.covenant.rpg.progression.ProfessionGathering::drops);
        NeoForge.EVENT_BUS.addListener(dev.covenant.rpg.progression.ProgressionData::register);
        NeoForge.EVENT_BUS.addListener(dev.covenant.rpg.progression.ProfessionData::register);
        NeoForge.EVENT_BUS.addListener(dev.covenant.rpg.progression.ProfessionEffects::incoming);
        NeoForge.EVENT_BUS.addListener(dev.covenant.rpg.progression.ProgressionCommands::register);
        NeoForge.EVENT_BUS.addListener(dev.covenant.rpg.progression.ProfessionCommands::register);
        LOGGER.info("Project Covenant loaded (mod id: {})", MOD_ID);
    }
}
