package dev.xyat.textstudio.chat.client;

import java.util.UUID;

public interface IChatComponentSync {
    void textstudio_chat$setSyncing(boolean syncing);
    void textstudio_chat$setProvidedTimestamp(long timestamp);
    /** 捕获当前正在处理的消息发送者 */
    void textstudio_chat$setCapturedSender(UUID uuid);
}
