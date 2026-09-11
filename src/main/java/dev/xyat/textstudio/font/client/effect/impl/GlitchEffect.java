package dev.xyat.textstudio.font.client.effect.impl;

import dev.xyat.textstudio.font.client.effect.IEffect;
import dev.xyat.textstudio.font.client.effect.RenderState;

public final class GlitchEffect implements IEffect {
    @Override
    public boolean isActive(RenderState state) {
        return state.glitch || state.config != null && state.config.glitch;
    }

    @Override
    public void apply(RenderState state) {
        float flicker = state.config != null && state.config.glitchFlicker > 0.0
                ? (float) state.config.glitchFlicker
                : 0.02f;
        float amplitude = state.config != null ? (float) state.config.glitchAmp : 1.0f;
        float speed = state.config != null ? (float) Math.max(0.1, state.config.glitchSpeed) : 1.0f;
        long hold = Math.max(30L, (long) (100.0f / speed));
        long seed = mix((state.time / hold) * 31L + state.index * 17L);
        float random = unit(seed);
        boolean surge = (state.time / 1000L) % 5L == 0L;
        float threshold = surge ? 0.40f : 0.05f;

        if (random >= threshold) {
            return;
        }

        float hashX = unit(mix(seed ^ 0x3243F6A8885A308DL));
        if (hashX < flicker * (surge ? 5.0f : 1.0f)) {
            state.setAlpha(0.0f);
            return;
        }

        float hashY = unit(mix(seed ^ 0x1B56C4E9036499FBL));
        state.offset((hashX - 0.5f) * 12.8f * amplitude, (hashY - 0.5f) * 4.8f * amplitude);
    }

    private static long mix(long value) {
        value ^= value >>> 12;
        value ^= value << 25;
        value ^= value >>> 27;
        return value * 0x2545F4914F6CDD1DL;
    }

    private static float unit(long value) {
        return (float) ((value >>> 11) * 0x1.0p-53);
    }
}
