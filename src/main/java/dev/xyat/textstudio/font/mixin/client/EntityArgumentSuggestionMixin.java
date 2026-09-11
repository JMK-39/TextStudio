package dev.xyat.textstudio.font.mixin.client;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.xyat.kineticcore.api.client.search.KineticSearch;
import dev.xyat.textstudio.font.api.AuthorAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.commands.arguments.EntityArgument;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Mixin(EntityArgument.class)
public abstract class EntityArgumentSuggestionMixin {
    private static final int SEARCH_CACHE_LIMIT = 256;
    private static final Map<String, KineticSearch.PinyinData> SEARCH_CACHE = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, KineticSearch.PinyinData> eldest) {
            return size() > SEARCH_CACHE_LIMIT;
        }
    };

    @Inject(method = "listSuggestions", at = @At("RETURN"), cancellable = true)
    private <S> void textstudio_font$appendCustomPlayerNames(
            CommandContext<S> context,
            SuggestionsBuilder builder,
            CallbackInfoReturnable<CompletableFuture<Suggestions>> cir
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            return;
        }

        String remaining = builder.getRemaining();
        if (remaining.startsWith("@")) {
            return;
        }

        String query = remaining.trim().toLowerCase(Locale.ROOT);
        List<NameMatch> matches = new ArrayList<>();

        for (PlayerInfo info : minecraft.getConnection().getOnlinePlayers()) {
            String rawName = info.getProfile().getName();
            AuthorAPI.DisplayInfo display = AuthorAPI.getDisplayInfo(info.getProfile().getId(), rawName);
            if (display == null || display.name == null) {
                continue;
            }

            String customName = display.name.trim();
            if (customName.isEmpty() || customName.equals(rawName) || !isCommandSafe(customName)) {
                continue;
            }

            int score = matchScore(customName, query);
            if (score >= 0) {
                matches.add(new NameMatch(customName, score));
            }
        }

        if (matches.isEmpty()) {
            return;
        }

        matches.sort(Comparator.comparingInt(NameMatch::score).thenComparing(NameMatch::name, String.CASE_INSENSITIVE_ORDER));

        CompletableFuture<Suggestions> originalFuture = cir.getReturnValue();
        cir.setReturnValue(originalFuture.thenApply(original -> {
            SuggestionsBuilder extraBuilder = new SuggestionsBuilder(builder.getInput(), builder.getStart());
            String last = null;
            for (NameMatch match : matches) {
                if (!match.name().equals(last)) {
                    extraBuilder.suggest(match.name());
                    last = match.name();
                }
            }
            Suggestions extra = extraBuilder.build();
            if (extra.isEmpty()) {
                return original;
            }
            return Suggestions.merge(builder.getInput(), List.of(original, extra));
        }));
    }

    private static int matchScore(String customName, String query) {
        if (query.isEmpty()) {
            return 0;
        }

        String lowerName = customName.toLowerCase(Locale.ROOT);
        if (lowerName.equals(query)) {
            return 0;
        }
        if (lowerName.startsWith(query)) {
            return 1;
        }

        KineticSearch.PinyinData searchData = getSearchData(customName);
        if (searchData.searchData().isEmpty()) {
            return -1;
        }
        if (searchData.full().startsWith(query)) {
            return 2;
        }
        if (searchData.initials().startsWith(query)) {
            return 3;
        }
        if (searchData.matches(query)) {
            return 4;
        }
        return -1;
    }

    private static KineticSearch.PinyinData getSearchData(String customName) {
        synchronized (SEARCH_CACHE) {
            KineticSearch.PinyinData cached = SEARCH_CACHE.get(customName);
            if (cached != null) {
                return cached;
            }

            KineticSearch.PinyinData value = KineticSearch.preparePinyin(customName);
            SEARCH_CACHE.put(customName, value);
            return value;
        }
    }

    private static boolean isCommandSafe(String name) {
        for (int i = 0; i < name.length(); i++) {
            if (Character.isWhitespace(name.charAt(i))) {
                return false;
            }
        }
        return !name.startsWith("@");
    }

    private record NameMatch(String name, int score) {
    }
}
