package dev.xyat.textstudio.chat.command;

import dev.xyat.textstudio.chat.ChatModule;
import dev.xyat.textstudio.chat.config.ChatConfig;
import dev.xyat.kineticcore.command.KTCommandApi;
import dev.xyat.kineticcore.command.KTCommandExtension;
import net.minecraft.commands.CommandSourceStack;

public final class ChatCommandExtension implements KTCommandExtension {
    private ChatCommandExtension() {
    }

    public static void install() {
        KTCommandApi.register(ChatModule.MODID, new ChatCommandExtension());
    }

    @Override
    public void reload(CommandSourceStack source) {
        ChatConfig.load();
    }
}
