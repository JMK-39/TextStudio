package dev.xyat.textstudio.chat;

import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import java.util.ArrayList;
import java.util.List;

import com.mojang.logging.LogUtils;
import dev.xyat.kineticcore.api.config.server.KTServerConfigApi;
import dev.xyat.kineticcore.api.config.server.KTServerConfigSpec;
import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.runtime.KineticModLifecycle;
import dev.xyat.kineticcore.api.runtime.KineticPlatform;
import dev.xyat.kineticcore.api.server.event.KineticServerEvents;
import dev.xyat.textstudio.chat.command.ChatCommandExtension;
import dev.xyat.textstudio.chat.config.ChatConfig;
import dev.xyat.textstudio.chat.client.ChatClientIntegration;
import dev.xyat.textstudio.chat.data.ChatHistoryServerManager;
import dev.xyat.textstudio.chat.network.ChatSyncCodec;
import dev.xyat.textstudio.chat.network.ChatSyncNetwork;
import org.slf4j.Logger;

public final class ChatModule {
    // These subscriptions remain active for the lifetime of this module.
    private static final List<KineticEventSubscription> SUBSCRIPTIONS = new ArrayList<>();
    public static final String MODID = "textstudio";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ChatModule() {
        ChatConfig.load();
        KTServerConfigApi.register(KTServerConfigSpec.builder("textstudio:chat")
                .intValue(
                        "max_length",
                        () -> ChatConfig.maxChatLength,
                        value -> ChatConfig.maxChatLength = value,
                        ChatSyncCodec.MIN_CHAT_LENGTH,
                        ChatSyncCodec.MAX_CHAT_LENGTH
                )
                .intValue(
                        "history_lines",
                        () -> ChatConfig.maxChatHistoryLines,
                        value -> ChatConfig.maxChatHistoryLines = value,
                        ChatSyncCodec.MIN_HISTORY_LINES,
                        ChatSyncCodec.MAX_HISTORY_LINES
                )
                .booleanValue(
                        "history_saving",
                        () -> ChatConfig.enableChatHistorySaving,
                        value -> ChatConfig.enableChatHistorySaving = value
                )
                .booleanValue(
                        "strip_signatures",
                        () -> ChatConfig.stripChatSignatures,
                        value -> ChatConfig.stripChatSignatures = value
                )
                .onSave(ChatConfig::saveServerSettings)
                .build());
        ChatCommandExtension.install();
        KineticModLifecycle.onCommonSetup(ChatSyncNetwork::register);
        SUBSCRIPTIONS.add(KineticServerEvents.onPlayerLogin(
                KineticEventPriority.NORMAL,
                ChatHistoryServerManager::syncToPlayer
        ));
        KineticPlatform.runOnClient(() -> ChatClientIntegration::install);
    }
}
