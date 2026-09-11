package dev.xyat.textstudio.font.client.effect.impl;

import dev.xyat.textstudio.font.client.effect.IEffect;
import dev.xyat.textstudio.font.client.effect.RenderState;
import dev.xyat.textstudio.font.client.effect.VisualEffectMath;

public final class SweepEffect implements IEffect {
    @Override
    public boolean isActive(RenderState state) {
        return state.config != null && state.config.sweep;
    }

    @Override
    public void apply(RenderState state) {
        if (state.a <= 0.0f) {
            return;
        }
        float strength = VisualEffectMath.sweepStrength(
                state.time,
                state.index,
                state.effectLength,
                state.config.sweepSpeed,
                state.config.sweepWidth
        );
        strength *= (float) Math.max(0.0, Math.min(1.0, state.config.sweepStrength));
        state.r += (1.0f - state.r) * strength;
        state.g += (1.0f - state.g) * strength;
        state.b += (1.0f - state.b) * strength;
    }
}
