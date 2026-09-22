package dev.xyat.textstudio.font.event;

import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import java.util.ArrayList;
import java.util.List;

import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.server.event.KineticServerEvents;
import dev.xyat.textstudio.font.api.AuthorAPI;
import dev.xyat.textstudio.font.api.IAuthorName;
import dev.xyat.textstudio.font.network.AuthorNetwork;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class AuthorSyncHandler {
    // These subscriptions remain active for the lifetime of this module.
    private static final List<KineticEventSubscription> SUBSCRIPTIONS = new ArrayList<>();
    private static final ConcurrentLinkedQueue<UUID> PENDING_SYNC = new ConcurrentLinkedQueue<>();
    private static boolean installed;

    private AuthorSyncHandler() {
    }

    public static synchronized void install() {
        if (installed) return;
        installed = true;

        SUBSCRIPTIONS.add(KineticServerEvents.onPlayerLogin(KineticEventPriority.NORMAL, player -> PENDING_SYNC.add(player.getUUID())));
        SUBSCRIPTIONS.add(KineticServerEvents.onPlayerClone(KineticEventPriority.NORMAL, (original, current, wasDeath) -> {
            if (original instanceof IAuthorName oldAuth && current instanceof IAuthorName newAuth) {
                newAuth.textstudio_font$setState(
                        oldAuth.textstudio_font$getCustomdiyname(),
                        oldAuth.textstudio_font$getNameEffect(),
                        oldAuth.textstudio_font$getStyleFlags()
                );
            }
        }));
        SUBSCRIPTIONS.add(KineticServerEvents.onPlayerRespawn(
                KineticEventPriority.NORMAL,
                (player, endConquered) -> PENDING_SYNC.add(player.getUUID())
        ));
        SUBSCRIPTIONS.add(KineticServerEvents.onPlayerChangedDimension(
                KineticEventPriority.NORMAL,
                (player, from, to) -> PENDING_SYNC.add(player.getUUID())
        ));
        SUBSCRIPTIONS.add(KineticServerEvents.onTick(
                KineticEventPriority.NORMAL,
                KineticServerEvents.TickPhase.END,
                server -> {
                    UUID uuid;
                    while ((uuid = PENDING_SYNC.poll()) != null) {
                        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                        if (player != null) performSync(player);
                    }
                }
        ));
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
