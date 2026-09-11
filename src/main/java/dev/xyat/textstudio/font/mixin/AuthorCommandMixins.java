package dev.xyat.textstudio.font.mixin;

import com.mojang.brigadier.StringReader;
import dev.xyat.textstudio.font.api.AuthorAPI;
import dev.xyat.textstudio.font.common.text.AuthorNamePolicy;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.regex.Pattern;

public class AuthorCommandMixins {

    @Mixin(EntitySelectorParser.class)
    public static abstract class EntitySelectorTweaks {
        @Shadow @Final private StringReader reader;
        @Shadow private String playerName;
        @Shadow public abstract EntitySelector getSelector();
        @Unique private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

        @Inject(method = "parse", at = @At("HEAD"), cancellable = true)
        private void textstudio_font$onParse(CallbackInfoReturnable<EntitySelector> cir) {
            int cursor = reader.getCursor();
            while (reader.canRead() && Character.isWhitespace(reader.peek())) reader.skip();

            if (reader.canRead()) {
                if (reader.peek() == '@') { reader.setCursor(cursor); return; }
                String remaining = reader.getRemaining();
                if (remaining.length() >= 36) {
                    if (UUID_PATTERN.matcher(remaining.substring(0, 36)).matches()) {
                        reader.setCursor(cursor); return;
                    }
                }
            }

            int start = reader.getCursor();
            while (reader.canRead() && !Character.isWhitespace(reader.peek())) reader.skip();
            String potentialName = reader.getString().substring(start, reader.getCursor());

            if (!potentialName.isEmpty()) {
                this.playerName = potentialName;
                cir.setReturnValue(this.getSelector());
            } else {
                reader.setCursor(cursor);
            }
        }
    }

    @Mixin(PlayerList.class)
    public static abstract class PlayerListTweaks {
        @Inject(method = "getPlayerByName", at = @At("HEAD"), cancellable = true)
        private void textstudio_font$getByCustomName(String inputName, CallbackInfoReturnable<ServerPlayer> cir) {
            if (inputName == null) return;
            // 不再需要移除 NAME_MARKER
            String cleanInput = AuthorNamePolicy.canonicalVisibleName(inputName);
            PlayerList list = (PlayerList) (Object) this;

            for (ServerPlayer player : list.getPlayers()) {
                if (player.getGameProfile().getName().equalsIgnoreCase(cleanInput)) {
                    cir.setReturnValue(player); return;
                }
                String customName = AuthorAPI.getCustomName(player);
                if (customName != null) {
                    String cleanPlayerName = AuthorNamePolicy.canonicalVisibleName(customName);
                    if (!cleanPlayerName.isEmpty() && cleanPlayerName.equals(cleanInput)) {
                        cir.setReturnValue(player); return;
                    }
                }
            }
        }
    }
}
