package dev.xyat.textstudio.chat;

import com.mojang.logging.LogUtils;
import dev.xyat.textstudio.chat.command.ChatCommandExtension;
import dev.xyat.textstudio.chat.config.ChatConfig;
import dev.xyat.textstudio.chat.config.ChatConfigGui;
import dev.xyat.textstudio.chat.network.ChatSyncCodec;
import dev.xyat.kineticcore.config.server.KTServerConfigApi;
import dev.xyat.kineticcore.config.server.KTServerConfigSpec;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

public final class ChatModule {
    public static final String MODID = "textstudio";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ChatModule(FMLJavaModLoadingContext context) {
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
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ChatConfigGui::load);
    }
}
