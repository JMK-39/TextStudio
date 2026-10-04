package dev.xyat.textstudio.font.network;

import dev.xyat.textstudio.font.api.AuthorAPI;
import dev.xyat.kineticcore.api.client.gui.KineticGui;
import dev.xyat.textstudio.font.client.FontModuleConfigPage;
import dev.xyat.textstudio.font.client.FontModuleGuidePage;
//? if <26.1 {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}

// 26.1 no longer strips @OnlyIn members; this class is only reached from client packet handling.
//? if <26.1
@OnlyIn(Dist.CLIENT)
public class AuthorNetworkClient {
    public static void handleSync(AuthorNetwork.SyncName packet) {
        AuthorAPI.DisplayInfo previous = AuthorAPI.CLIENT_CACHE.get(packet.uuid);
        AuthorAPI.DisplayInfo info = new AuthorAPI.DisplayInfo();
        info.name = packet.name != null && !packet.name.isEmpty()
                ? packet.name
                : previous != null && previous.name != null && !previous.name.isEmpty() ? previous.name : "Player";
        info.effect = packet.effect;
        info.isRainbow = (packet.styleFlags & AuthorAPI.FLAG_RAINBOW) != 0;
        info.isBold = (packet.styleFlags & AuthorAPI.FLAG_BOLD) != 0;
        info.isStrikethrough = (packet.styleFlags & AuthorAPI.FLAG_STRIKETHROUGH) != 0;
        info.isJitter = (packet.styleFlags & AuthorAPI.FLAG_JITTER) != 0;
        info.isGlitch = (packet.styleFlags & AuthorAPI.FLAG_GLITCH) != 0;
        info.isAuthor = AuthorAPI.isAuthor(packet.uuid);
        info.isDynamic = true;
        AuthorAPI.CLIENT_CACHE.put(packet.uuid, info);
    }

    public static void handleOpenScreen(AuthorNetwork.OpenScreen packet) {
        if (packet.screen == 1) {
            KineticGui.openChild(new FontModuleConfigPage());
        } else {
            KineticGui.openChild(new FontModuleGuidePage());
        }
    }
}
