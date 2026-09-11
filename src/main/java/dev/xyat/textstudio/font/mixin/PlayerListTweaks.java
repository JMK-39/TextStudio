package dev.xyat.textstudio.font.mixin;

import dev.xyat.textstudio.font.api.AuthorAPI;
import dev.xyat.textstudio.font.common.text.AuthorNamePolicy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(PlayerList.class)
public abstract class PlayerListTweaks {
    @Inject(method = "getPlayerByName", at = @At("HEAD"), cancellable = true)
    private void textstudio_font$getByCustomName(String inputName, CallbackInfoReturnable<ServerPlayer> cir) {
        if (inputName == null) return;
        String cleanInput = AuthorNamePolicy.canonicalVisibleName(inputName);
        PlayerList list = (PlayerList) (Object) this;

        for (ServerPlayer player : list.getPlayers()) {
            if (player.getGameProfile().getName().equalsIgnoreCase(cleanInput)) {
                cir.setReturnValue(player);
                return;
            }
            String customName = AuthorAPI.getCustomName(player);
            if (customName != null) {
                String cleanPlayerName = AuthorNamePolicy.canonicalVisibleName(customName);
                if (!cleanPlayerName.isEmpty() && cleanPlayerName.equals(cleanInput)) {
                    cir.setReturnValue(player);
                    return;
                }
            }
        }
    }
}
