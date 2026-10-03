package dev.xyat.textstudio.font.client;

import dev.xyat.kineticcore.api.client.search.KineticSearch;
import java.util.LinkedHashMap;
import java.util.Map;

/** Loadable helper types used by the EntityArgument mixin. */
public final class NameSuggestionSupport {
    private NameSuggestionSupport() {}

    public record NameMatch(String name, int score) {}

    public static Map<String, KineticSearch.PinyinData> newCache() {
        return new LinkedHashMap<>(64, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, KineticSearch.PinyinData> eldest) {
                return size() > 256;
            }
        };
    }
}
