package dev.xyat.textstudio.chat.network;

import dev.xyat.kineticcore.api.minecraft.MinecraftChat;
import dev.xyat.textstudio.chat.ChatModule;
import dev.xyat.textstudio.chat.client.IChatComponentSync;
import dev.xyat.textstudio.chat.config.ChatConfig;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.List;

public final class ChatSyncNetworkClient {
    private ChatSyncNetworkClient() {
    }

    public static void handle(ChatSyncNetwork.ClientboundSync message) {
        if (message == null || !ChatSyncCodec.isClientboundType(message.type())) return;
        byte[] payload = message.payload();
        if (payload == null) return;

        if (message.type() == ChatSyncCodec.TYPE_CHAT_HISTORY) {
            if (payload.length < 1 || payload.length > ChatSyncCodec.MAX_COMPRESSED_HISTORY_BYTES) return;
        } else if (payload.length + 1 > ChatSyncCodec.MAX_SERVERBOUND_PACKET_BYTES) {
            return;
        }

        IChatComponentSync syncable = MinecraftChat.extension(IChatComponentSync.class);
        if (syncable == null) return;
        syncable.textstudio_chat$setSyncing(true);
        try {
            if (message.type() == ChatSyncCodec.TYPE_CHAT_HISTORY) {
                List<ChatSyncCodec.ChatLine> history = ChatSyncCodec.decodeCompressedHistory(
                        payload,
                        ChatSyncCodec.MAX_HISTORY_LINES,
                        ChatSyncCodec.MAX_CHAT_LENGTH,
                        ChatSyncCodec.clampHistoryLines(ChatConfig.maxChatHistoryLines)
                );
                for (ChatSyncCodec.ChatLine line : history) {
                    Component component = Component.Serializer.fromJson(line.json());
                    if (component != null) {
                        syncable.textstudio_chat$setProvidedTimestamp(line.timestamp());
                        syncable.textstudio_chat$setCapturedSender(line.senderUuid());
                        MinecraftChat.addMessage(component);
                    }
                }
            } else {
                String input = ChatSyncCodec.decodeInputLine(payload, ChatSyncCodec.MAX_CHAT_LENGTH);
                if (input.length() <= ChatSyncCodec.wireStringLimit(ChatConfig.maxChatLength)) {
                    MinecraftChat.addRecentChat(input);
                }
            }
        } catch (IOException | RuntimeException exception) {
            ChatModule.LOGGER.warn("Rejected malformed server chat sync payload: {}", exception.getMessage());
        } finally {
            syncable.textstudio_chat$setSyncing(false);
            syncable.textstudio_chat$setProvidedTimestamp(-1);
            syncable.textstudio_chat$setCapturedSender(null);
        }
    }
}
