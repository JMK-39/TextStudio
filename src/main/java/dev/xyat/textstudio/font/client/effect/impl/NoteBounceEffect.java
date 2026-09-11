package dev.xyat.textstudio.font.client.effect.impl;

import dev.xyat.textstudio.font.client.effect.IEffect;
import dev.xyat.textstudio.font.client.effect.RenderState;

public final class NoteBounceEffect implements IEffect {
    @Override
    public boolean isActive(RenderState state) {
        return state.config != null && state.config.noteBounce;
    }

    @Override
    public void apply(RenderState state) {
        double phase = state.time * 0.006 * state.config.noteBounceSpeed - state.index * 1.05;
        double pulse = Math.sin(phase);
        if (pulse > 0.0) {
            state.y -= (float) (pulse * pulse * 2.0 * state.config.noteBounceAmp);
        }
    }
}
