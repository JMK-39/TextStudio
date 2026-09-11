package dev.xyat.textstudio.font.mixin.client;

import dev.xyat.textstudio.font.api.AuthorAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.LinkedHashSet;

@Mixin(ClientSuggestionProvider.class)
public class ClientSuggestionProviderTweaks {
    @Inject(method = "getOnlinePlayerNames", at = @At("RETURN"), cancellable = true)
    private void textstudio_font$appendDisplayNames(CallbackInfoReturnable<Collection<String>> cir) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) return;

        LinkedHashSet<String> names = new LinkedHashSet<>(cir.getReturnValue());
        for (PlayerInfo info : minecraft.getConnection().getOnlinePlayers()) {
            String rawName = info.getProfile().getName();
            AuthorAPI.DisplayInfo display = AuthorAPI.getDisplayInfo(info.getProfile().getId(), rawName);
            if (display == null || display.name == null) continue;
            String customName = display.name.trim();
            if (customName.isEmpty() || customName.equals(rawName) || !isCommandSafe(customName)) continue;
            names.add(customName);
        }
        cir.setReturnValue(names);
    }

    private static boolean isCommandSafe(String name) {
        for (int i = 0; i < name.length(); i++) {
            if (Character.isWhitespace(name.charAt(i))) return false;
        }
        return !name.startsWith("@");
    }
}
