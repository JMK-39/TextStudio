package dev.xyat.textstudio.chat.client;

import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface IChatComponentSync {
    void textstudio_chat$setSyncing(boolean syncing);
    void textstudio_chat$setProvidedTimestamp(long timestamp);
    /** 捕获当前正在处理的消息发送者 */
    void textstudio_chat$setCapturedSender(UUID uuid);

    /** 某条聊天行（按显示内容查找）应绘制的头像；不是消息首行或没有发送者时为 null / The head drawn on a chat line, found by its displayed content; null unless the line starts a player's message. */
    @Nullable ChatHead textstudio_chat$headFor(FormattedCharSequence lineContent);

    /** 头像的发送者，以及头像前时间戳的宽度 / The sender of a head and the width of the timestamp before it. */
    record ChatHead(UUID sender, int timestampWidth) {
    }
}
