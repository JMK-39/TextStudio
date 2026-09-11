package dev.xyat.textstudio.font.mixin.client;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import dev.xyat.textstudio.font.api.AuthorAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuthorClientMixins {

    @Mixin(CommandSuggestions.class)
    public static class CommandSuggestionsTweaks {
        @Inject(method = "sortSuggestions", at = @At("HEAD"), cancellable = true)
        private void textstudio_font$modifyCommandSuggestions(Suggestions suggestions, CallbackInfoReturnable<List<Suggestion>> cir) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() == null) return;

            List<Suggestion> newList = new ArrayList<>();
            boolean modified = false;
            var onlinePlayers = mc.getConnection().getOnlinePlayers();

            for (Suggestion s : suggestions.getList()) {
                String replacementText = s.getText();
                PlayerInfo targetPlayer = null;
                for (PlayerInfo p : onlinePlayers) {
                    if (p.getProfile().getName().equals(s.getText())) {
                        targetPlayer = p; break;
                    }
                }
                if (targetPlayer != null) {
                    AuthorAPI.DisplayInfo di = AuthorAPI.getDisplayInfo(targetPlayer.getProfile().getId(), targetPlayer.getProfile().getName());
                    if (di != null && di.name != null && !di.name.isEmpty()) {
                        replacementText = di.name;
                        modified = true;
                    }
                }
                newList.add(new Suggestion(s.getRange(), replacementText, s.getTooltip()));
            }
            if (modified) cir.setReturnValue(newList);
        }
    }

    @Mixin(PlayerTabOverlay.class)
    public static class PlayerTabOverlayTweaks {
        @Inject(method = "getNameForDisplay", at = @At("HEAD"), cancellable = true)
        private void textstudio_font$injectTabAuthorName(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
            if (Minecraft.getInstance().level != null) {
                UUID uuid = info.getProfile().getId();
                String rawName = info.getProfile().getName();
                AuthorAPI.DisplayInfo di = AuthorAPI.getDisplayInfo(uuid, rawName);

                if (di.isRainbow || di.isBold || di.isStrikethrough) {
                    Style style = Style.EMPTY;
                    if (di.isBold) style = style.withBold(true);
                    if (di.isStrikethrough) style = style.withStrikethrough(true);
                    cir.setReturnValue(Component.literal(di.name).withStyle(style));
                }
            }
        }
    }
}
