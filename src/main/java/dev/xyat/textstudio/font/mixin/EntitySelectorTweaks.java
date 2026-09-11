package dev.xyat.textstudio.font.mixin;

import com.mojang.brigadier.StringReader;
import dev.xyat.textstudio.font.api.AuthorAPI;
import dev.xyat.textstudio.font.common.text.AuthorNamePolicy;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Mixin(EntitySelectorParser.class)
public abstract class EntitySelectorTweaks {
    @Shadow @Final private StringReader reader;
    @Shadow private String playerName;
    @Shadow public abstract EntitySelector getSelector();
    @Unique private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    @Inject(method = "parse", at = @At("HEAD"), cancellable = true)
    private void textstudio_font$onParse(CallbackInfoReturnable<EntitySelector> cir) {
        int cursor = reader.getCursor();
        while (reader.canRead() && Character.isWhitespace(reader.peek())) {
            reader.skip();
        }

        if (!reader.canRead()) {
            reader.setCursor(cursor);
            return;
        }

        if (reader.peek() == '@') {
            reader.setCursor(cursor);
            return;
        }

        String remaining = reader.getRemaining();
        if (remaining.length() >= 36 && UUID_PATTERN.matcher(remaining.substring(0, 36)).matches()) {
            reader.setCursor(cursor);
            return;
        }

        int start = reader.getCursor();
        while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
            reader.skip();
        }
        String potentialName = reader.getString().substring(start, reader.getCursor());

        if (!potentialName.isEmpty() && textstudio_font$isExactCustomPlayerName(potentialName)) {
            this.playerName = potentialName;
            cir.setReturnValue(this.getSelector());
            return;
        }

        reader.setCursor(cursor);
    }

    @Unique
    private static boolean textstudio_font$isExactCustomPlayerName(String input) {
        String cleanInput = AuthorNamePolicy.canonicalVisibleName(input);
        for (Map.Entry<UUID, AuthorAPI.DisplayInfo> entry : AuthorAPI.CLIENT_CACHE.entrySet()) {
            AuthorAPI.DisplayInfo info = entry.getValue();
            if (info != null && info.name != null && AuthorNamePolicy.canonicalVisibleName(info.name).equals(cleanInput)) {
                return true;
            }
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return false;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String customName = AuthorAPI.getCustomName(player);
            if (customName != null && AuthorNamePolicy.canonicalVisibleName(customName).equals(cleanInput)) {
                return true;
            }
        }
        return false;
    }
}
