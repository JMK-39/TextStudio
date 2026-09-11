package dev.xyat.textstudio.font.client;

import dev.xyat.kineticcore.config.client.KTConfigApi;
import dev.xyat.kineticcore.config.client.KTConfigPage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class FontModuleClient {
    public static final String CONFIG_PAGE_ID = "textstudio:effects";

    public static void init() {
        FontModuleConfigScreen.register();
        registerCoreConfigPage();
    }

    private static void registerCoreConfigPage() {
        KTConfigApi.register(KTConfigPage.builder(
                        CONFIG_PAGE_ID,
                        Component.translatable("mod.textstudio.name")
                )
                .applyTiming(KTConfigPage.ApplyTiming.MIXED)
                .action(
                        "open_guide",
                        Component.translatable("gui.textstudio.font.guide.open"),
                        FontModuleClient::openGuide,
                        Component.empty()
                )
                .action(
                        "open_visual_editor",
                        Component.translatable("gui.textstudio.font.editor.open"),
                        FontModuleClient::openEditor,
                        Component.empty()
                )
                .build());
    }

    private static void openGuide() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(FontModuleGuideScreen.create(minecraft.screen));
    }

    private static void openEditor() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(FontModuleConfigScreen.create(minecraft.screen));
    }
}
