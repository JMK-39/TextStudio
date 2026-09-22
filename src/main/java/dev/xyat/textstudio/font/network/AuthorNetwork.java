package dev.xyat.textstudio.font.network;

import dev.xyat.kineticcore.api.network.ClientboundSender;
import dev.xyat.kineticcore.api.network.KineticNetwork;
import dev.xyat.kineticcore.api.network.NetworkChannel;
import dev.xyat.kineticcore.api.network.NetworkCodec;
import dev.xyat.kineticcore.api.network.NetworkVersionPolicy;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.textstudio.font.FontModule;
import dev.xyat.textstudio.font.common.annotation.KTNetwork;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

@KTNetwork
public final class AuthorNetwork {
    private static final String PROTOCOL_VERSION = "2";
    private static final NetworkChannel CHANNEL = KineticNetwork.channel(
            KineticResourceIds.of(FontModule.MODID, "author_identity"),
            PROTOCOL_VERSION,
            NetworkVersionPolicy.ANY
    );
    private static final KineticRegistrationBatch REGISTRATIONS = new KineticRegistrationBatch();

    private static ClientboundSender<SyncName> syncNameSender;
    private static ClientboundSender<OpenScreen> openScreenSender;

    private AuthorNetwork() {
    }

    public static void register() {
        REGISTRATIONS.runSequential(
                () -> syncNameSender = CHANNEL.registerClientbound(
                        0,
                        SyncName.class,
                        NetworkCodec.of(
                                (buffer, message) -> {
                                    buffer.writeUuid(message.uuid);
                                    buffer.writeUtf(message.name);
                                    buffer.writeInt(message.effect);
                                    buffer.writeInt(message.styleFlags);
                                },
                                buffer -> new SyncName(
                                        buffer.readUuid(),
                                        buffer.readUtf(),
                                        buffer.readInt(),
                                        buffer.readInt()
                                )
                        ),
                        message -> AuthorNetworkClient.handleSync(message)
                ),
                () -> openScreenSender = CHANNEL.registerClientbound(
                        1,
                        OpenScreen.class,
                        NetworkCodec.of(
                                (buffer, message) -> buffer.writeVarInt(message.screen),
                                buffer -> new OpenScreen(buffer.readVarInt())
                        ),
                        message -> AuthorNetworkClient.handleOpenScreen(message)
                )
        );
    }

    public static final class SyncName {
        public final UUID uuid;
        public final String name;
        public final int effect;
        public final int styleFlags;

        public SyncName(UUID uuid, String name, int effect, int styleFlags) {
            this.uuid = uuid;
            this.name = name == null ? "" : name;
            this.effect = effect;
            this.styleFlags = styleFlags;
        }
    }

    public static final class OpenScreen {
        public final int screen;

        public OpenScreen(int screen) {
            this.screen = screen;
        }
    }

    public static void sendToPlayer(SyncName message, ServerPlayer player) {
        if (syncNameSender == null) throw new IllegalStateException("Author sync network is not registered");
        syncNameSender.send(player, message);
    }

    public static void sendToPlayer(OpenScreen message, ServerPlayer player) {
        if (openScreenSender == null) throw new IllegalStateException("Author screen network is not registered");
        openScreenSender.send(player, message);
    }

    public static void sendToAll(SyncName message) {
        if (syncNameSender == null) throw new IllegalStateException("Author sync network is not registered");
        syncNameSender.broadcast(message);
    }
}
