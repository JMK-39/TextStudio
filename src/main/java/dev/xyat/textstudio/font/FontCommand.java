package dev.xyat.textstudio.font;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.xyat.textstudio.font.network.AuthorNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class FontCommand {
    private static final int SCREEN_GUIDE = 0;
    private static final int SCREEN_EDITOR = 1;

    private FontCommand() {
    }

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        var font = Commands.literal("font")
                .executes(ctx -> open(ctx.getSource(), SCREEN_GUIDE));

        font.then(Commands.literal("guide").executes(ctx -> open(ctx.getSource(), SCREEN_GUIDE)));
        font.then(Commands.literal("help").executes(ctx -> open(ctx.getSource(), SCREEN_GUIDE)));
        font.then(Commands.literal("editor").executes(ctx -> open(ctx.getSource(), SCREEN_EDITOR)));

        AuthorCommand.register(font);
        root.then(font);
    }

    private static int open(CommandSourceStack source, int screen) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            AuthorNetwork.sendToPlayer(new AuthorNetwork.OpenScreen(screen), player);
            return 1;
        } catch (Exception ignored) {
            return 0;
        }
    }
}
