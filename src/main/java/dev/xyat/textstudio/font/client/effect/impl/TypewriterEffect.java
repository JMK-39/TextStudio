package dev.xyat.textstudio.font.client.effect.impl;

import dev.xyat.textstudio.font.client.effect.IEffect;
import dev.xyat.textstudio.font.client.effect.RenderState;

public final class TypewriterEffect implements IEffect {
    @Override
    public boolean isActive(RenderState state) {
        return state.config != null && state.config.typewriter;
    }

    @Override
    public void apply(RenderState state) {
        double speed = Math.max(0.1, state.config.typewriterSpeed);
        double millisPerChar = 1000.0 / (4.0 * speed);
        int length = Math.max(1, state.effectLength);
        long typeDuration = Math.max(1L, (long) (length * millisPerChar));
        long holdDuration = 10000L;
        boolean backspace = state.config.typewriterBack;
        boolean hasUntype = backspace || state.config.glitch || state.glitch || state.config.shake || state.jitter;
        long untypeDuration = hasUntype ? typeDuration : 0L;
        long cycle = Math.max(1L, typeDuration + holdDuration + untypeDuration);
        long localTime = state.time % cycle;

        if (localTime < typeDuration) {
            int revealIndex = (int) (localTime / millisPerChar);
            if (state.index > revealIndex) {
                state.setAlpha(0.0f);
            } else if (state.index == revealIndex) {
                float boost = (0.5f + 0.5f * (float) Math.sin(state.time * 0.015)) * 0.5f;
                state.r = Math.min(1.0f, state.r + boost);
                state.g = Math.min(1.0f, state.g + boost);
                state.b = Math.min(1.0f, state.b + boost);
            }
            return;
        }

        if (localTime < typeDuration + holdDuration || !hasUntype) {
            return;
        }

        long untypeTime = localTime - typeDuration - holdDuration;
        if (backspace) {
            int hiddenCount = Math.min(length, (int) (untypeTime / millisPerChar) + 1);
            int firstHiddenIndex = length - hiddenCount;
            if (state.index >= firstHiddenIndex) {
                state.setAlpha(0.0f);
            }
            return;
        }

        float progress = Math.min(1.0f, (float) untypeTime / Math.max(1L, untypeDuration));
        long cycleIndex = state.time / cycle;
        long seed = mix(cycleIndex * 31L + state.index * 17L);
        if (progress > unit(seed)) {
            state.setAlpha(0.0f);
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
