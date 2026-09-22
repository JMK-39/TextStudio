package dev.xyat.textstudio.chat.network;

import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import dev.xyat.kineticcore.api.network.ClientboundSender;
import dev.xyat.kineticcore.api.network.KineticNetwork;
import dev.xyat.kineticcore.api.network.NetworkChannel;
import dev.xyat.kineticcore.api.network.NetworkCodec;
import dev.xyat.kineticcore.api.network.NetworkVersionPolicy;
import dev.xyat.kineticcore.api.network.ServerboundSender;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.server.event.KineticServerEvents;
import dev.xyat.textstudio.chat.ChatModule;
import dev.xyat.textstudio.chat.config.ChatConfig;
import dev.xyat.textstudio.chat.data.ChatHistoryServerManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class ChatSyncNetwork {
    private static final int MAX_SYNC_PACKETS_PER_SECOND = 64;
    private static final int MAX_SYNC_BYTES_PER_SECOND = 512 * 1024;
    private static final int MAX_SERVERBOUND_PAYLOAD_BYTES = ChatSyncCodec.MAX_SERVERBOUND_PACKET_BYTES - 1;
    private static final int MAX_CLIENTBOUND_PAYLOAD_BYTES = ChatSyncCodec.MAX_COMPRESSED_HISTORY_BYTES;

    private static final NetworkChannel CHANNEL = KineticNetwork.channel(
            KineticResourceIds.of(ChatModule.MODID, "chat_sync"),
            "1",
            NetworkVersionPolicy.ANY
    );
    private static final KineticRegistrationBatch REGISTRATIONS = new KineticRegistrationBatch();
    private static final Map<UUID, SyncBudget> SYNC_BUDGETS = new ConcurrentHashMap<>();

    private static ServerboundSender<ServerboundSync> serverboundSender;
    private static ClientboundSender<ClientboundSync> clientboundSender;
    // Retained for the lifetime of the network registrations.
    private static KineticEventSubscription logoutSubscription;

    private ChatSyncNetwork() {
    }

    public static void register() {
        REGISTRATIONS.runSequential(
                () -> serverboundSender = CHANNEL.registerServerbound(
                        0,
                        ServerboundSync.class,
                        NetworkCodec.of(
                                (buffer, message) -> {
                                    buffer.writeByte(message.type());
                                    buffer.writeByteArray(message.payload(), MAX_SERVERBOUND_PAYLOAD_BYTES);
                                },
                                buffer -> new ServerboundSync(
                                        Byte.toUnsignedInt(buffer.readByte()),
                                        buffer.readByteArray(MAX_SERVERBOUND_PAYLOAD_BYTES)
                                )
                        ),
                        ChatSyncNetwork::handleServerbound
                ),
                () -> clientboundSender = CHANNEL.registerClientbound(
                        1,
                        ClientboundSync.class,
                        NetworkCodec.of(
                                (buffer, message) -> {
                                    buffer.writeByte(message.type());
                                    buffer.writeByteArray(message.payload(), MAX_CLIENTBOUND_PAYLOAD_BYTES);
                                },
                                buffer -> new ClientboundSync(
                                        Byte.toUnsignedInt(buffer.readByte()),
                                        buffer.readByteArray(MAX_CLIENTBOUND_PAYLOAD_BYTES)
                                )
                        ),
                        message -> ChatSyncNetworkClient.handle(message)
                ),
                () -> logoutSubscription = KineticServerEvents.onPlayerLogout(
                        KineticEventPriority.NORMAL,
                        player -> SYNC_BUDGETS.remove(player.getUUID())
                )
        );
    }

    public static void sendToServer(int type, byte[] payload) {
        if (serverboundSender == null || payload == null) return;
        if (!ChatSyncCodec.isServerboundType(type)) return;
        if (payload.length + 1 > ChatSyncCodec.MAX_SERVERBOUND_PACKET_BYTES) return;
        serverboundSender.send(new ServerboundSync(type, payload));
    }

    public static void sendToPlayer(int type, byte[] payload, ServerPlayer player) {
        if (clientboundSender == null || player == null || payload == null) return;
        if (!ChatSyncCodec.isClientboundType(type)) return;
        if (type == ChatSyncCodec.TYPE_CHAT_HISTORY) {
            if (payload.length < 1 || payload.length > ChatSyncCodec.MAX_COMPRESSED_HISTORY_BYTES) return;
        } else if (payload.length + 1 > ChatSyncCodec.MAX_SERVERBOUND_PACKET_BYTES) {
            return;
        }
        clientboundSender.send(player, new ClientboundSync(type, payload));
    }

    private static void handleServerbound(ServerboundSync message, dev.xyat.kineticcore.api.network.ServerPacketContext context) {
        ServerPlayer player = context.sender();
        if (!ChatConfig.enableChatHistorySaving || player == null) return;
        int type = message.type();
        byte[] payload = message.payload();
        int packetBytes = payload == null ? 1 : payload.length + 1;

        if (!ChatSyncCodec.isServerboundType(type)) {
            warnRejected(player, "invalid serverbound type " + type);
            return;
        }
        if (payload == null || packetBytes < 1 || packetBytes > ChatSyncCodec.MAX_SERVERBOUND_PACKET_BYTES) {
            warnRejected(player, "invalid payload length " + packetBytes);
            return;
        }
        if (rejectSyncBudget(player, packetBytes)) {
            warnRejected(player, "rate limit exceeded");
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) return;
        try {
            if (type == ChatSyncCodec.TYPE_CHAT_LINE) {
                ChatSyncCodec.ChatLine line = ChatSyncCodec.decodeChatLine(payload, ChatConfig.maxChatLength);
                ChatHistoryServerManager.addChatLine(
                        server,
                        player.getUUID(),
                        line.json(),
                        line.timestamp(),
                        line.senderUuid()
                );
            } else {
                String input = ChatSyncCodec.decodeInputLine(payload, ChatConfig.maxChatLength);
                ChatHistoryServerManager.addInputLine(server, player.getUUID(), input);
            }
        } catch (IOException | RuntimeException exception) {
            warnRejected(player, "malformed type " + type + " payload");
        }
    }

    private static boolean rejectSyncBudget(ServerPlayer player, int packetBytes) {
        SyncBudget budget = SYNC_BUDGETS.computeIfAbsent(player.getUUID(), ignored -> new SyncBudget());
        return !budget.acquire(packetBytes);
    }

    private static void warnRejected(ServerPlayer player, String reason) {
        SyncBudget budget = SYNC_BUDGETS.computeIfAbsent(player.getUUID(), ignored -> new SyncBudget());
        if (budget.shouldWarn()) {
            ChatModule.LOGGER.warn(
                    "Rejected chat sync payload from {}: {}",
                    player.getGameProfile().getName(),
                    reason
            );
        }
    }

    public record ServerboundSync(int type, byte[] payload) {
    }

    public record ClientboundSync(int type, byte[] payload) {
    }

    private static final class SyncBudget {
        private long windowStart;
        private int packetsInWindow;
        private int bytesInWindow;
        private long lastRejectWarning;

        private synchronized boolean acquire(int packetBytes) {
            long now = System.nanoTime();
            if (windowStart == 0L || now - windowStart >= TimeUnit.SECONDS.toNanos(1)) {
                windowStart = now;
                packetsInWindow = 0;
                bytesInWindow = 0;
            }
            if (packetsInWindow >= MAX_SYNC_PACKETS_PER_SECOND
                    || bytesInWindow > MAX_SYNC_BYTES_PER_SECOND - packetBytes) {
                return false;
            }
            packetsInWindow++;
            bytesInWindow += packetBytes;
            return true;
        }

        private synchronized boolean shouldWarn() {
            long now = System.nanoTime();
            if (lastRejectWarning == 0L || now - lastRejectWarning >= TimeUnit.SECONDS.toNanos(10)) {
                lastRejectWarning = now;
                return true;
            }
            return false;
        }
    }
}
