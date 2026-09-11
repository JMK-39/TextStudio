package dev.xyat.textstudio.font;

import dev.xyat.kineticcore.command.KTCommandApi;
import dev.xyat.kineticcore.command.KTCommandExtension;
import dev.xyat.textstudio.font.config.AuthorConfig;
import dev.xyat.textstudio.font.network.AuthorNetwork;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class FontModule {
    public static final String MODID = "textstudio";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FontModule() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        AuthorConfig.load();

        KTCommandApi.register(MODID, new KTCommandExtension() {
            @Override
            public void registerCommands(com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> root) {
                FontCommand.register(root);
            }

            @Override
            public void appendHelpItems(net.minecraft.commands.CommandSourceStack source, java.util.List<net.minecraft.network.chat.MutableComponent> items) {
                items.add(dev.xyat.kineticcore.command.CommandUtils.createSuggestCommand(
                        "/kt font",
                        "/kt font ",
                        "cmd.textstudio.font.desc"
                ));
            }
        });

        DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> dev.xyat.textstudio.font.client.FontModuleClient.init()
        );
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(AuthorNetwork::register);
    }
}
