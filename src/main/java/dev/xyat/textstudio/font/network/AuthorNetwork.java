package dev.xyat.textstudio.font.network;

import dev.xyat.kineticcore.api.KTNetworkProtocol;
import dev.xyat.textstudio.font.FontModule;
import dev.xyat.textstudio.font.common.annotation.KTNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.UUID;
import java.util.function.Supplier;

@KTNetwork
public class AuthorNetwork {
    private static final String PROTOCOL_VERSION = "2";
    private static int packetId = 0;
    private static int id() { return packetId++; }

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(FontModule.MODID, "author_identity"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION).clientAcceptedVersions(KTNetworkProtocol::acceptsAnyVersion).serverAcceptedVersions(KTNetworkProtocol::acceptsAnyVersion).simpleChannel();

    public static void register() {
        CHANNEL.messageBuilder(SyncName.class, id(), NetworkDirection.PLAY_TO_CLIENT).decoder(SyncName::new).encoder(SyncName::toBytes).consumerMainThread(SyncName::handle).add();
        CHANNEL.messageBuilder(OpenScreen.class, id(), NetworkDirection.PLAY_TO_CLIENT).decoder(OpenScreen::new).encoder(OpenScreen::toBytes).consumerMainThread(OpenScreen::handle).add();
    }

    public static class SyncName {
        public final UUID uuid; public final String name; public final int effect; public final int styleFlags;
        public SyncName(UUID uuid, String name, int effect, int styleFlags) { this.uuid = uuid; this.name = (name == null) ? "" : name; this.effect = effect; this.styleFlags = styleFlags; }
        public SyncName(FriendlyByteBuf buf) { this.uuid = buf.readUUID(); this.name = buf.readUtf(); this.effect = buf.readInt(); this.styleFlags = buf.readInt(); }
        public void toBytes(FriendlyByteBuf buf) { buf.writeUUID(this.uuid); buf.writeUtf(this.name); buf.writeInt(this.effect); buf.writeInt(this.styleFlags); }
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AuthorNetworkClient.handleSync(this)));
            ctx.get().setPacketHandled(true);
        }
    }


    public static class OpenScreen {
        public final int screen;

        public OpenScreen(int screen) {
            this.screen = screen;
        }

        public OpenScreen(FriendlyByteBuf buf) {
            this.screen = buf.readVarInt();
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeVarInt(screen);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () -> () -> AuthorNetworkClient.handleOpenScreen(this)
            ));
            ctx.get().setPacketHandled(true);
        }
    }

    public static void sendToPlayer(Object msg, ServerPlayer player) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg); }
    public static void sendToAll(Object msg) { CHANNEL.send(PacketDistributor.ALL.noArg(), msg); }
}
