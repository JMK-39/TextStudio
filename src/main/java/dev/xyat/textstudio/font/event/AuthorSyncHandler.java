package dev.xyat.textstudio.font.event;

import dev.xyat.textstudio.font.FontModule;
import dev.xyat.textstudio.font.api.AuthorAPI;
import dev.xyat.textstudio.font.api.IAuthorName;
import dev.xyat.textstudio.font.network.AuthorNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mod.EventBusSubscriber(modid = FontModule.MODID)
public class AuthorSyncHandler {
    private static final ConcurrentLinkedQueue<UUID> PENDING_SYNC = new ConcurrentLinkedQueue<>();

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PENDING_SYNC.add(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof IAuthorName oldAuth && event.getEntity() instanceof IAuthorName newAuth) {
            newAuth.textstudio_font$setState(
                    oldAuth.textstudio_font$getCustomdiyname(),
                    oldAuth.textstudio_font$getNameEffect(),
                    oldAuth.textstudio_font$getStyleFlags()
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PENDING_SYNC.add(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PENDING_SYNC.add(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !PENDING_SYNC.isEmpty()) {
            UUID uuid;
            while ((uuid = PENDING_SYNC.poll()) != null) {
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);
                if (player != null) performSync(player);
            }
        }
    }

    private static void performSync(ServerPlayer player) {
        if (!(player instanceof IAuthorName auth)) {
            return;
        }

        player.refreshDisplayName();

        AuthorNetwork.sendToAll(new AuthorNetwork.SyncName(
                player.getUUID(),
                AuthorAPI.getCustomName(player),
                auth.textstudio_font$getNameEffect(),
                auth.textstudio_font$getStyleFlags()
        ));

        Objects.requireNonNull(player.getServer()).getPlayerList().getPlayers().forEach(other -> {
            if (other != player && other instanceof IAuthorName otherAuth) {
                AuthorNetwork.sendToPlayer(new AuthorNetwork.SyncName(
                        other.getUUID(),
                        AuthorAPI.getCustomName(other),
                        otherAuth.textstudio_font$getNameEffect(),
                        otherAuth.textstudio_font$getStyleFlags()
                ), player);
            }
        });
    }
}
