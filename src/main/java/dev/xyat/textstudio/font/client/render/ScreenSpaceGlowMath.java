package dev.xyat.textstudio.font.client.render;

public final class ScreenSpaceGlowMath {
    private ScreenSpaceGlowMath() {
    }

    public static int outlineArgb(int rgb, float glyphAlpha, double configuredAlpha, double configuredRadius) {
        float alpha = outlineAlpha(glyphAlpha, configuredAlpha, configuredRadius);
        int a = Math.max(1, Math.min(255, Math.round(alpha * 255.0f)));
        return a << 24 | outlineRgb(rgb, configuredAlpha, configuredRadius);
    }

    public static float outlineAlpha(float glyphAlpha, double configuredAlpha, double configuredRadius) {
        float textAlpha = clamp01(glyphAlpha);
        float glow = clamp01((float) configuredAlpha);
        float radius = clamp((float) configuredRadius, 0.35f, 3.0f);
        float radius01 = (radius - 0.35f) / 2.65f;
        float strength = 0.82f + 0.12f * radius01;
        return clamp01(textAlpha * (float) Math.sqrt(glow) * strength);
    }

    public static int outlineRgb(int rgb, double configuredAlpha, double configuredRadius) {
        float glow = clamp01((float) configuredAlpha);
        float radius = clamp((float) configuredRadius, 0.35f, 3.0f);
        float radius01 = (radius - 0.35f) / 2.65f;
        float targetLuma = 198.0f + 30.0f * glow + 18.0f * radius01;
        return raiseLuminance(rgb, Math.min(236.0f, targetLuma));
    }

    public static int raiseLuminance(int rgb, float targetLuma) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        float luma = r * 0.2126f + g * 0.7152f + b * 0.0722f;
        float target = clamp(targetLuma, 0.0f, 255.0f);
        if (luma >= target || luma >= 254.999f) {
            return rgb & 0xFFFFFF;
        }
        float mix = (target - luma) / (255.0f - luma);
        return mixTowardWhite(rgb, mix);
    }

    public static int mixTowardWhite(int rgb, float amount) {
        float mix = clamp01(amount);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        r = Math.min(255, Math.round(r + (255 - r) * mix));
        g = Math.min(255, Math.round(g + (255 - g) * mix));
        b = Math.min(255, Math.round(b + (255 - b) * mix));
        return r << 16 | g << 8 | b;
    }

    private static float clamp01(float value) {
        return clamp(value, 0.0f, 1.0f);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
