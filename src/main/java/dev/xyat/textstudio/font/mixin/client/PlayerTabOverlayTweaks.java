package dev.xyat.textstudio.font.mixin.client;

import dev.xyat.textstudio.font.api.AuthorAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayTweaks {
    @Inject(method = "getNameForDisplay", at = @At("HEAD"), cancellable = true)
    private void textstudio_font$injectTabAuthorName(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
        if (Minecraft.getInstance().level == null) return;
        AuthorAPI.DisplayInfo display = AuthorAPI.getDisplayInfo(info.getProfile().getId(), info.getProfile().getName());
        if (display == null || display.name == null || display.name.isEmpty()) return;
        boolean renamed = !display.name.equals(info.getProfile().getName());
        boolean styled = display.isRainbow || display.isBold || display.isStrikethrough
                || display.isJitter || display.isGlitch || display.effect != 1;
        if (renamed || styled) {
            cir.setReturnValue(AuthorAPI.createStyledName(display));
        }
    }
}
