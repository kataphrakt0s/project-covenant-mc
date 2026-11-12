package dev.covenant.rpg.progression;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import dev.covenant.rpg.RpgData;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Self inspection and operator-only testing; does not fabricate allocations or activity rewards. */
public final class ProfessionCommands {
    private ProfessionCommands() {}
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("covenant").then(Commands.literal("profession")
                .executes(context -> {
                    var state = context.getSource().getPlayerOrException().getData(RpgData.PROFESSIONS);
                    for (var id : ProgressionData.PROFESSIONS) {
                        var track = state.tracks().get(id);
                        context.getSource().sendSuccess(() -> Component.literal(id + ": level " + track.level() + ", "
                                + track.experience() + (track.experienceFraction() == 0 ? "" : "." + (track.experienceFraction() < 10 ? "0" : "") + track.experienceFraction())
                                + (id.equals(ProgressionData.EXPLORATION) ? " (milestones; no XP); " : " XP; ")
                                + track.points() + " available / " + track.spentPoints() + " paid points"), false);
                    }
                    return 1;
                })
                .then(Commands.literal("xp").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("profession", IdentifierArgument.id())
                                .suggests((c, b) -> SharedSuggestionProvider.suggestResource(ProgressionData.XP_PROFESSIONS, b))
                                .then(Commands.argument("amount", LongArgumentType.longArg(1)).executes(context -> {
                                    var player = context.getSource().getPlayerOrException();
                                    var id = IdentifierArgument.getId(context, "profession");
                                    if (!ProgressionData.PROFESSIONS.contains(id)) { return 0; }
                                    return Progression.awardProfession(player, player.level(), id, LongArgumentType.getLong(context, "amount")) ? 1 : 0;
                                }))))
                .then(Commands.literal("level").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("profession", IdentifierArgument.id())
                                .suggests((c, b) -> SharedSuggestionProvider.suggestResource(ProgressionData.PROFESSIONS, b))
                                .then(Commands.argument("level", IntegerArgumentType.integer(0, 1000)).executes(context -> {
                                    var player = context.getSource().getPlayerOrException();
                                    var id = IdentifierArgument.getId(context, "profession");
                                    var state = player.getData(RpgData.PROFESSIONS); var track = state.tracks().get(id);
                                    int level = IntegerArgumentType.getInteger(context, "level");
                                    if (track == null || level < track.spentPoints()
                                            || level > ProgressionData.get(player.level().getServer()).maximum(id)) { return 0; }
                                    player.setData(RpgData.PROFESSIONS, state.with(id,
                                            new ProfessionState.Track(level, 0, track.allocations())));
                                    Progression.refreshView(player);
                                    return 1;
                                }))))));
    }
}
