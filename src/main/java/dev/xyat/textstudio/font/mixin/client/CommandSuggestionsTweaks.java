package dev.xyat.textstudio.font.mixin.client;

import dev.xyat.kineticcore.api.minecraft.MinecraftPlayers;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import dev.xyat.textstudio.font.api.AuthorAPI;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(CommandSuggestions.class)
public class CommandSuggestionsTweaks {
    @Inject(method = "sortSuggestions", at = @At("HEAD"), cancellable = true)
    private void textstudio_font$modifyCommandSuggestions(Suggestions suggestions, CallbackInfoReturnable<List<Suggestion>> cir) {
        var onlinePlayers = MinecraftPlayers.onlinePlayers();
        if (onlinePlayers.isEmpty()) return;

        List<Suggestion> newList = new ArrayList<>();
        boolean modified = false;

        for (Suggestion s : suggestions.getList()) {
            String replacementText = s.getText();
            PlayerInfo targetPlayer = null;
            for (PlayerInfo p : onlinePlayers) {
                if (p.getProfile().getName().equals(s.getText())) {
                    targetPlayer = p;
                    break;
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
