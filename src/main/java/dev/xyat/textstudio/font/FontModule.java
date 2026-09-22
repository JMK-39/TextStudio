package dev.xyat.textstudio.font;

import com.mojang.logging.LogUtils;
import dev.xyat.kineticcore.api.command.CommandExtension;
import dev.xyat.kineticcore.api.command.CommandText;
import dev.xyat.kineticcore.api.command.KineticCommands;
import dev.xyat.kineticcore.api.runtime.KineticModLifecycle;
import dev.xyat.kineticcore.api.runtime.KineticPlatform;
import dev.xyat.textstudio.font.client.FontModuleClient;
import dev.xyat.textstudio.font.config.AuthorConfig;
import dev.xyat.textstudio.font.event.AuthorSyncHandler;
import dev.xyat.textstudio.font.network.AuthorNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.MutableComponent;
import org.slf4j.Logger;

import java.util.List;

public class FontModule {
    public static final String MODID = "textstudio";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FontModule() {
        AuthorConfig.load();

        KineticCommands.registerExtension(MODID + ":font", new CommandExtension() {
            @Override
            public void registerCommands(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> root) {
                FontCommand.register(root);
            }

            @Override
            public void appendHelpItems(CommandSourceStack source, List<MutableComponent> items) {
                items.add(CommandText.suggest(
                        "/kt font",
                        "/kt font ",
                        "cmd.textstudio.font.desc"
                ));
            }
        });

        AuthorSyncHandler.install();
        KineticModLifecycle.onCommonSetup(AuthorNetwork::register);
        KineticPlatform.runOnClient(() -> FontModuleClient::init);
    }
}
