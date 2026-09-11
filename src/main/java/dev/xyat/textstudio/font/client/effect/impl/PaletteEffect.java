package dev.xyat.textstudio.font.client.effect.impl;

import dev.xyat.textstudio.font.client.effect.IEffect;
import dev.xyat.textstudio.font.client.effect.RenderState;
import dev.xyat.textstudio.font.config.AuthorConfig;

public final class PaletteEffect implements IEffect {
    private static final int CACHE_SIZE = 128;
    private static final ThreadLocal<PaletteCache> CACHE = ThreadLocal.withInitial(PaletteCache::new);

    @Override
    public boolean isActive(RenderState state) {
        return state.config != null && state.config.palette && state.config.paletteColors != null && !state.config.paletteColors.isEmpty();
    }

    @Override
    public void apply(RenderState state) {
        if (state.a <= 0.0f) return;
        int color = sampleColor(state.config, state.time, state.index, 0.0f);
        if (color < 0) return;
        state.r = ((color >> 16) & 0xFF) / 255.0f;
        state.g = ((color >> 8) & 0xFF) / 255.0f;
        state.b = (color & 0xFF) / 255.0f;
    }

    public static int sampleColor(AuthorConfig.EffectSettings config, long time, int index, float phaseOffset) {
        if (config == null || config.paletteColors == null || config.paletteColors.isEmpty()) return -1;
        int[] colors = getColors(config.paletteColors);
        if (colors.length == 0) return -1;
        if (colors.length == 1) return colors[0];
        float speed = (float) config.paletteSpeed;
        float spread = (float) config.paletteSpread;
        float timeSpeed = spread == 0.0f ? speed : 20.0f * speed * spread;
        float progress = ((time * 0.001f * timeSpeed) - (index * spread) + phaseOffset) % 1.0f;
        if (progress < 0.0f) progress += 1.0f;
        float scaled = progress * colors.length;
        int firstIndex = Math.min(colors.length - 1, (int) scaled);
        if (config.paletteFlash) return colors[firstIndex];
        int secondIndex = (firstIndex + 1) % colors.length;
        float ratio = scaled - firstIndex;
        int first = colors[firstIndex];
        int second = colors[secondIndex];
        int red = (int) lerp((first >> 16) & 0xFF, (second >> 16) & 0xFF, ratio);
        int green = (int) lerp((first >> 8) & 0xFF, (second >> 8) & 0xFF, ratio);
        int blue = (int) lerp(first & 0xFF, second & 0xFF, ratio);
        return red << 16 | green << 8 | blue;
    }

    private static int[] getColors(String text) {
        PaletteCache cache = CACHE.get();
        int hash = text.hashCode();
        int slot = hash & (CACHE_SIZE - 1);
        String key = cache.keys[slot];
        if (key != null && cache.hashes[slot] == hash && key.equals(text)) return cache.values[slot];
        int[] value = parseColors(text);
        cache.keys[slot] = text;
        cache.hashes[slot] = hash;
        cache.values[slot] = value;
        return value;
    }

    private static float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    private static int[] parseColors(String text) {
        int estimated = 1;
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) == '|') estimated++;
        int[] colors = new int[estimated];
        int count = 0;
        int start = 0;
        int length = text.length();
        for (int i = 0; i <= length; i++) {
            if (i == length || text.charAt(i) == '|') {
                int begin = start;
                int end = i;
                while (begin < end && (text.charAt(begin) == ' ' || text.charAt(begin) == '#')) begin++;
                while (end > begin && text.charAt(end - 1) == ' ') end--;
                if (end > begin) {
                    try {
                        colors[count++] = Integer.parseInt(text.substring(begin, end), 16) & 0xFFFFFF;
                    } catch (NumberFormatException ignored) {
                    }
                }
                start = i + 1;
            }
        }
        if (count == colors.length) return colors;
        int[] result = new int[count];
        System.arraycopy(colors, 0, result, 0, count);
        return result;
    }

    private static final class PaletteCache {
        private final String[] keys = new String[CACHE_SIZE];
        private final int[] hashes = new int[CACHE_SIZE];
        private final int[][] values = new int[CACHE_SIZE][];
    }
}
