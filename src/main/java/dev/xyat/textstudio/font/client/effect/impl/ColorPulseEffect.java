package dev.xyat.textstudio.font.client.effect.impl;

import dev.xyat.textstudio.font.client.effect.IEffect;
import dev.xyat.textstudio.font.client.effect.RenderState;
import dev.xyat.textstudio.font.config.AuthorConfig;

public final class ColorPulseEffect implements IEffect {
    @Override
    public boolean isActive(RenderState state) {
        return state.config != null && (state.config.pulse || state.config.neonFlicker || state.config.shimmer
                || state.config.sparkle || state.config.blinkWave);
    }

    @Override
    public void apply(RenderState state) {
        if (state.a <= 0.0f) {
            return;
        }

        AuthorConfig.EffectSettings config = state.config;
        if (config.pulse) {
            float multiplier = (float) (config.pulseBase
                    + config.pulseAmp * (0.5 + 0.5 * Math.sin(state.time * 0.005 * config.pulseSpeed)));
            state.r = Math.min(1.0f, state.r * multiplier);
            state.g = Math.min(1.0f, state.g * multiplier);
            state.b = Math.min(1.0f, state.b * multiplier);
        }


        if (config.shimmer) {
            double phase = state.time * 0.006 * Math.max(0.05, config.shimmerSpeed) - state.index * 0.9;
            double wave = 0.5 + 0.5 * Math.sin(phase);
            double shaped = Math.pow(wave, Math.max(0.25, config.shimmerWidth));
            float strength = (float) Math.max(0.0, Math.min(1.0, config.shimmerStrength * shaped));
            state.r += (1.0f - state.r) * strength;
            state.g += (1.0f - state.g) * strength;
            state.b += (1.0f - state.b) * strength;
        }

        if (config.sparkle) {
            double speed = Math.max(0.05, config.sparkleSpeed);
            long duration = Math.max(18L, (long) (140.0 / speed));
            long seed = mix((state.time / duration) * 131L + state.index * 977L);
            if (unit(seed) < Math.max(0.0, Math.min(1.0, config.sparkleChance))) {
                float strength = (float) Math.max(0.0, Math.min(1.0, config.sparkleStrength));
                state.r += (1.0f - state.r) * strength;
                state.g += (1.0f - state.g) * strength;
                state.b += (1.0f - state.b) * strength;
            }
        }

        if (config.blinkWave) {
            double phase = state.time * 0.007 * Math.max(0.05, config.blinkWaveSpeed) - state.index * 0.85;
            float minAlpha = (float) Math.max(0.0, Math.min(1.0, config.blinkWaveDepth));
            float alpha = minAlpha + (1.0f - minAlpha) * (0.5f + 0.5f * (float) Math.sin(phase));
            state.a *= alpha;
        }

        if (config.neonFlicker) {
            float speed = (float) Math.max(0.1, config.neonFlickerSpeed);
            long duration = Math.max(1L, (long) (80.0f / speed));
            long seed = mix((state.time / duration) * 31L + state.index * 13L);
            float random = unit(seed);
            if (random < 0.20f) {
                float intensity = unit(mix(seed ^ 0x3243F6A8885A308DL));
                float multiplier = intensity < 0.4f ? 0.08f : 0.35f;
                state.r *= multiplier;
                state.g *= multiplier;
                state.b *= multiplier;
            }
        }
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
