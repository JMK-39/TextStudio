package dev.xyat.textstudio.chat.config;

import dev.xyat.kineticcore.api.text.KineticI18n;

import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.config.client.KTConfigPage;
import dev.xyat.kineticcore.api.config.client.KTConfigScope;

public final class ChatConfigGui {
    public static final String SERVER_PAGE_ID = "textstudio:chat";
    public static final String CLIENT_PAGE_ID = "textstudio:client";
    public static final String PAGE_ID = SERVER_PAGE_ID;

    private ChatConfigGui() {
    }

    public static void load() {
        KTConfigApi.register(KTConfigPage.builder(
                        SERVER_PAGE_ID,
                        KineticI18n.translatable("cfg.textstudio.chat.server")
                )
                .scope(KTConfigScope.SERVER_AUTHORITATIVE)
                .serverManaged()
                .pageDescription(KineticI18n.translatable("cfg.textstudio.chat.chat.description"))
                .applyTiming(KTConfigPage.ApplyTiming.IMMEDIATE)
                .intValue(
                        "max_length",
                        KineticI18n.translatable("cfg.textstudio.chat.chat.max_length"),
                        () -> ChatConfig.maxChatLength,
                        value -> ChatConfig.maxChatLength = value,
                        16384,
                        256,
                        32767,
                        KineticI18n.translatable("cfg.textstudio.chat.chat.max_length.desc")
                )
                .intValue(
                        "history_lines",
                        KineticI18n.translatable("cfg.textstudio.chat.chat.history_lines"),
                        () -> ChatConfig.maxChatHistoryLines,
                        value -> ChatConfig.maxChatHistoryLines = value,
                        10000,
                        100,
                        100000,
                        KineticI18n.translatable("cfg.textstudio.chat.chat.history_lines.desc")
                )
                .booleanValue(
                        "history_saving",
                        KineticI18n.translatable("cfg.textstudio.chat.chat.history_saving"),
                        () -> ChatConfig.enableChatHistorySaving,
                        value -> ChatConfig.enableChatHistorySaving = value,
                        true,
                        KineticI18n.translatable("cfg.textstudio.chat.chat.history_saving.desc")
                )
                .booleanValue(
                        "strip_signatures",
                        KineticI18n.translatable("cfg.textstudio.chat.chat.strip_signatures"),
                        () -> ChatConfig.stripChatSignatures,
                        value -> ChatConfig.stripChatSignatures = value,
                        true,
                        KineticI18n.translatable("cfg.textstudio.chat.chat.strip_signatures.desc")
                )
                .build());

        KTConfigApi.register(KTConfigPage.builder(
                        CLIENT_PAGE_ID,
                        KineticI18n.translatable("cfg.textstudio.chat.client")
                )
                .scope(KTConfigScope.CLIENT_LOCAL)
                .pageDescription(KineticI18n.translatable("cfg.textstudio.chat.client.description"))
                .applyTiming(KTConfigPage.ApplyTiming.IMMEDIATE)
                .booleanValue(
                        "draggable_scrollbar",
                        KineticI18n.translatable("cfg.textstudio.chat.chat.draggable_scrollbar"),
                        () -> ChatConfig.enableDraggableScrollbar,
                        value -> ChatConfig.enableDraggableScrollbar = value,
                        true,
                        KineticI18n.translatable("cfg.textstudio.chat.chat.draggable_scrollbar.desc")
                )
                .booleanValue(
                        "timestamp",
                        KineticI18n.translatable("cfg.textstudio.chat.chat.timestamp"),
                        () -> ChatConfig.enableTimestamp,
                        value -> ChatConfig.enableTimestamp = value,
                        true,
                        KineticI18n.translatable("cfg.textstudio.chat.chat.timestamp.desc")
                )
                .booleanValue(
                        "compact_chat",
                        KineticI18n.translatable("cfg.textstudio.chat.chat.compact_chat"),
                        () -> ChatConfig.enableCompactChat,
                        value -> ChatConfig.enableCompactChat = value,
                        true,
                        KineticI18n.translatable("cfg.textstudio.chat.chat.compact_chat.desc")
                )
                .booleanValue(
                        "chat_heads",
                        KineticI18n.translatable("cfg.textstudio.chat.chat.heads"),
                        () -> ChatConfig.enableChatHeads,
                        value -> ChatConfig.enableChatHeads = value,
                        true,
                        KineticI18n.translatable("cfg.textstudio.chat.chat.heads.desc")
                )
                .onSave(ChatConfig::saveClientSettings)
                .build());
    }
}
