package dev.xyat.textstudio.chat.mixin;

import dev.xyat.textstudio.chat.ChatModule;
import dev.xyat.textstudio.chat.config.ChatConfig;
import dev.xyat.textstudio.chat.data.ChatHistoryServerManager;
import dev.xyat.textstudio.chat.network.ChatSyncCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.StringUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ChatServerMixins {

    @Mixin(ServerGamePacketListenerImpl.class)
    public static abstract class ServerPacketTweaks {
        @Shadow public ServerPlayer player;
        @Shadow @Final private MinecraftServer server;

        @Unique private static final int textstudio_chat$MAX_SYNC_PACKETS_PER_SECOND = 64;
        @Unique private static final int textstudio_chat$MAX_SYNC_BYTES_PER_SECOND = 512 * 1024;
        @Unique private long textstudio_chat$syncWindowStart;
        @Unique private int textstudio_chat$syncPacketsInWindow;
        @Unique private int textstudio_chat$syncBytesInWindow;
        @Unique private long textstudio_chat$lastRejectWarning;

        @ModifyConstant(method = "handleChat", constant = @Constant(intValue = 256))
        private int textstudio_chat$modifyChatLimit(int original) {
            return ChatConfig.maxChatLength;
        }

        @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
        private void textstudio_chat$onServerCustomPayload(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
            ResourceLocation id = packet.getIdentifier();
            if (!id.getNamespace().equals("textstudio") || !id.getPath().equals("chat_sync")) {
                return;
            }

            ci.cancel();
            if (!ChatConfig.enableChatHistorySaving) return;
            FriendlyByteBuf buf = packet.getData();
            int packetBytes = buf.readableBytes();
            if (packetBytes < 1 || packetBytes > ChatSyncCodec.MAX_SERVERBOUND_PACKET_BYTES) {
                textstudio_chat$warnRejected("invalid payload length " + packetBytes);
                return;
            }

            int type = buf.readUnsignedByte();
            // The client only sends individual chat/input records. Type 10 is
            // server-to-client only and must never be decompressed here.
            if (!ChatSyncCodec.isServerboundType(type)) {
                textstudio_chat$warnRejected("invalid serverbound type " + type);
                return;
            }
            if (!textstudio_chat$acquireSyncBudget(packetBytes)) {
                textstudio_chat$warnRejected("rate limit exceeded");
                return;
            }

            // Copy only after both the type and hard packet bound are known.
            byte[] payloadData = new byte[buf.readableBytes()];
            buf.readBytes(payloadData);
            this.server.execute(() -> {
                try {
                    if (type == ChatSyncCodec.TYPE_CHAT_LINE) {
                        ChatSyncCodec.ChatLine line =
                                ChatSyncCodec.decodeChatLine(payloadData, ChatConfig.maxChatLength);
                        ChatHistoryServerManager.addChatLine(
                                server,
                                this.player.getUUID(),
                                line.json(),
                                line.timestamp(),
                                line.senderUuid()
                        );
                    } else {
                        String input = ChatSyncCodec.decodeInputLine(payloadData, ChatConfig.maxChatLength);
                        ChatHistoryServerManager.addInputLine(server, this.player.getUUID(), input);
                    }
                } catch (IOException | RuntimeException exception) {
                    textstudio_chat$warnRejected("malformed type " + type + " payload");
                }
            });
        }

        @Unique
        private boolean textstudio_chat$acquireSyncBudget(int packetBytes) {
            long now = System.nanoTime();
            if (textstudio_chat$syncWindowStart == 0L
                    || now - textstudio_chat$syncWindowStart >= TimeUnit.SECONDS.toNanos(1)) {
                textstudio_chat$syncWindowStart = now;
                textstudio_chat$syncPacketsInWindow = 0;
                textstudio_chat$syncBytesInWindow = 0;
            }
            if (textstudio_chat$syncPacketsInWindow >= textstudio_chat$MAX_SYNC_PACKETS_PER_SECOND
                    || textstudio_chat$syncBytesInWindow > textstudio_chat$MAX_SYNC_BYTES_PER_SECOND - packetBytes) {
                return false;
            }
            textstudio_chat$syncPacketsInWindow++;
            textstudio_chat$syncBytesInWindow += packetBytes;
            return true;
        }

        @Unique
        private void textstudio_chat$warnRejected(String reason) {
            long now = System.nanoTime();
            if (textstudio_chat$lastRejectWarning == 0L
                    || now - textstudio_chat$lastRejectWarning >= TimeUnit.SECONDS.toNanos(10)) {
                textstudio_chat$lastRejectWarning = now;
                ChatModule.LOGGER.warn("Rejected chat sync payload from {}: {}",
                        this.player.getGameProfile().getName(), reason);
            }
        }
    }

    @Mixin(PlayerList.class)
    public static class PlayerListTweaks {
        @Inject(method = "placeNewPlayer", at = @At("RETURN"))
        private void textstudio_chat$onPlayerJoin(net.minecraft.network.Connection connection, ServerPlayer player, CallbackInfo ci) {
            ChatHistoryServerManager.syncToPlayer(player);
        }
    }

    @Mixin({ServerboundChatPacket.class, ServerboundChatCommandPacket.class})
    public static class PacketLengthTweaks {
        @ModifyConstant(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", constant = @Constant(intValue = 256), remap = false)
        private static int textstudio_chat$increasePacketReadLimit(int original) {
            return ChatConfig.maxChatLength;
        }
    }

    @Mixin(StringUtil.class)
    public static class StringUtilTweaks {
        @ModifyConstant(method = "trimChatMessage", constant = @Constant(intValue = 256))
        private static int textstudio_chat$increaseTrimLimit(int original) {
            return ChatConfig.maxChatLength;
        }
    }
}
