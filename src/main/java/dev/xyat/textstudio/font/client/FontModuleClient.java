package dev.xyat.textstudio.font.client;

import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.config.client.KTConfigPage;
import dev.xyat.kineticcore.api.client.gui.KineticGui;
import dev.xyat.kineticcore.api.text.KineticI18n;

public class FontModuleClient {
    public static final String CONFIG_PAGE_ID = "textstudio:effects";

    public static void init() {
        FontModuleConfigPage.register();
        registerCoreConfigPage();
    }

    private static void registerCoreConfigPage() {
        KTConfigApi.register(KTConfigPage.builder(
                        CONFIG_PAGE_ID,
                        KineticI18n.translatable("mod.textstudio.name")
                )
                .applyTiming(KTConfigPage.ApplyTiming.MIXED)
                .action(
                        "open_visual_editor",
                        KineticI18n.translatable("gui.textstudio.font.editor.open"),
                        FontModuleClient::openEditor,
                        KineticI18n.translatable("gui.textstudio.font.editor.tip.open")
                )
                .build());
    }

    private static void openEditor() {
        KineticGui.openChild(new FontModuleConfigPage());
    }
}
