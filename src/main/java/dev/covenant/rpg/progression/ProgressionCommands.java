package dev.covenant.rpg.progression;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import dev.covenant.rpg.PlayerProfiles;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Bounded operator tools for testing this fresh-save prototype without changing production tuning. */
public final class ProgressionCommands {
    private ProgressionCommands() {}
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("covenant").then(Commands.literal("progression")
                .executes(context -> {
                    var p = PlayerProfiles.get(context.getSource().getPlayerOrException());
                    context.getSource().sendSuccess(() -> Component.literal("Level " + p.level() + ", " + p.experience() + " XP; first Wither claimed: " + p.witherClaimed()), false);
                    return 1;
                })
                .then(Commands.literal("xp").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("amount", LongArgumentType.longArg(1)).executes(context -> {
                            var player = context.getSource().getPlayerOrException();
                            return Progression.awardCharacter(player, player.level(), LongArgumentType.getLong(context, "amount")) ? 1 : 0;
                        })))
                .then(Commands.literal("level").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 1000)).executes(context -> {
                            var player = context.getSource().getPlayerOrException();
                            int level = IntegerArgumentType.getInteger(context, "level");
                            int cap = ProgressionData.get(player.level().getServer()).maximum(ProgressionData.CHARACTER);
                            if (!PlayerProfiles.get(player).created() || level > cap) { return 0; }
                            PlayerProfiles.update(player, p -> Progression.atLevel(p, level)); Progression.refreshView(player); return 1;
                        })))));
    }
}
