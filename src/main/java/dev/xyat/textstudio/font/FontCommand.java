package dev.xyat.textstudio.font;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class FontCommand {
    private FontCommand() {
    }

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        var font = Commands.literal("font")
                .executes(ctx -> AuthorCommand.sendHelp(ctx.getSource()));

        font.then(Commands.literal("help").executes(ctx -> AuthorCommand.sendHelp(ctx.getSource())));

        AuthorCommand.register(font);
        root.then(font);
    }
}
