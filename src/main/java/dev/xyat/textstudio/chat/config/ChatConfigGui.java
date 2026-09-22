package dev.xyat.textstudio.chat.config;

import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.config.client.KTConfigPage;
import dev.xyat.kineticcore.api.config.client.KTConfigScope;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ChatConfigGui {
    public static final String SERVER_PAGE_ID = "textstudio:chat";
    public static final String CLIENT_PAGE_ID = "textstudio:client";
    public static final String PAGE_ID = SERVER_PAGE_ID;

    private ChatConfigGui() {
    }

    public static void load() {
        KTConfigApi.register(KTConfigPage.builder(
                        SERVER_PAGE_ID,
                        Component.translatable("cfg.textstudio.chat.server")
                )
                .scope(KTConfigScope.SERVER_AUTHORITATIVE)
                .serverManaged()
                .pageDescription(Component.translatable("cfg.textstudio.chat.chat.description"))
                .applyTiming(KTConfigPage.ApplyTiming.IMMEDIATE)
                .intValue(
                        "max_length",
                        Component.translatable("cfg.textstudio.chat.chat.max_length"),
                        () -> ChatConfig.maxChatLength,
                        value -> ChatConfig.maxChatLength = value,
                        16384,
                        256,
                        32767,
                        Component.translatable("cfg.textstudio.chat.chat.max_length.desc")
                )
                .intValue(
                        "history_lines",
                        Component.translatable("cfg.textstudio.chat.chat.history_lines"),
                        () -> ChatConfig.maxChatHistoryLines,
                        value -> ChatConfig.maxChatHistoryLines = value,
                        10000,
                        100,
                        100000,
                        Component.translatable("cfg.textstudio.chat.chat.history_lines.desc")
                )
                .booleanValue(
                        "history_saving",
                        Component.translatable("cfg.textstudio.chat.chat.history_saving"),
                        () -> ChatConfig.enableChatHistorySaving,
                        value -> ChatConfig.enableChatHistorySaving = value,
                        true,
                        Component.translatable("cfg.textstudio.chat.chat.history_saving.desc")
                )
                .booleanValue(
                        "strip_signatures",
                        Component.translatable("cfg.textstudio.chat.chat.strip_signatures"),
                        () -> ChatConfig.stripChatSignatures,
                        value -> ChatConfig.stripChatSignatures = value,
                        true,
                        Component.translatable("cfg.textstudio.chat.chat.strip_signatures.desc")
                )
                .build());

        KTConfigApi.register(KTConfigPage.builder(
                        CLIENT_PAGE_ID,
                        Component.translatable("cfg.textstudio.chat.client")
                )
                .scope(KTConfigScope.CLIENT_LOCAL)
                .pageDescription(Component.translatable("cfg.textstudio.chat.client.description"))
                .applyTiming(KTConfigPage.ApplyTiming.IMMEDIATE)
                .booleanValue(
                        "draggable_scrollbar",
                        Component.translatable("cfg.textstudio.chat.chat.draggable_scrollbar"),
                        () -> ChatConfig.enableDraggableScrollbar,
                        value -> ChatConfig.enableDraggableScrollbar = value,
                        true,
                        Component.translatable("cfg.textstudio.chat.chat.draggable_scrollbar.desc")
                )
                .booleanValue(
                        "timestamp",
                        Component.translatable("cfg.textstudio.chat.chat.timestamp"),
                        () -> ChatConfig.enableTimestamp,
                        value -> ChatConfig.enableTimestamp = value,
                        true,
                        Component.translatable("cfg.textstudio.chat.chat.timestamp.desc")
                )
                .booleanValue(
                        "compact_chat",
                        Component.translatable("cfg.textstudio.chat.chat.compact_chat"),
                        () -> ChatConfig.enableCompactChat,
                        value -> ChatConfig.enableCompactChat = value,
                        true,
                        Component.translatable("cfg.textstudio.chat.chat.compact_chat.desc")
                )
                .booleanValue(
                        "chat_heads",
                        Component.translatable("cfg.textstudio.chat.chat.heads"),
                        () -> ChatConfig.enableChatHeads,
                        value -> ChatConfig.enableChatHeads = value,
                        true,
                        Component.translatable("cfg.textstudio.chat.chat.heads.desc")
                )
                .onSave(ChatConfig::saveClientSettings)
                .build());
    }

    public static Screen create(Screen parent) {
        return KTConfigApi.createScreen(parent, SERVER_PAGE_ID);
    }
}
