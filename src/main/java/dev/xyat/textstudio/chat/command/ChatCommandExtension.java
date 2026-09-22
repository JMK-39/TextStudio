package dev.xyat.textstudio.chat.command;

import dev.xyat.kineticcore.api.command.CommandExtension;
import dev.xyat.kineticcore.api.command.KineticCommands;
import dev.xyat.textstudio.chat.ChatModule;
import dev.xyat.textstudio.chat.config.ChatConfig;
import net.minecraft.commands.CommandSourceStack;

public final class ChatCommandExtension implements CommandExtension {
    private ChatCommandExtension() {
    }

    public static void install() {
        KineticCommands.registerExtension(ChatModule.MODID + ":chat", new ChatCommandExtension());
    }

    @Override
    public void reload(CommandSourceStack source) {
        ChatConfig.load();
    }
}
