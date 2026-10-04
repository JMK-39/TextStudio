package dev.xyat.textstudio.chat.client;

import dev.xyat.kineticcore.api.minecraft.MinecraftPlayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** Skin textures for chat heads: the player's own skin while they are listed, otherwise the default skin. */
public final class ChatHeads {
    private ChatHeads() {
    }

    public static ResourceLocation skin(UUID uuid) {
        PlayerInfo info = MinecraftPlayers.playerInfo(uuid);
        //? if >=26.1 {
        /*return (info != null ? info.getSkin() : DefaultPlayerSkin.get(uuid)).body().texturePath();
        *///?} else if >=1.21 {
        /*return info != null ? info.getSkin().texture() : DefaultPlayerSkin.get(uuid).texture();
        *///?} else {
        return info != null ? info.getSkinLocation() : DefaultPlayerSkin.getDefaultSkin(uuid);
        //?}
    }
}
