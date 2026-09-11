package dev.xyat.textstudio.font.client.effect;

public final class AnimationPerformance {
    public static final int DEFAULT_REFRESH_INTERVAL_MS = 33;
    public static final int DEFAULT_MAX_ANIMATED_GLYPHS = 512;
    public static final int DEFAULT_EXTRA_PASS_BUDGET = 8;

    private AnimationPerformance() {
    }

    public static long quantizeTime(long time, int intervalMs) {
        if (intervalMs <= 1) {
            return time;
        }
        return time - Math.floorMod(time, intervalMs);
    }

    public static int animatedGlyphLimit(int length, int maxAnimatedGlyphs) {
        if (length <= 0) {
            return 0;
        }
        if (maxAnimatedGlyphs <= 0) {
            return length;
        }
        return Math.min(length, maxAnimatedGlyphs);
    }
}
