package dev.xyat.textstudio.font.client.effect.impl;

import dev.xyat.textstudio.font.client.effect.IEffect;
import dev.xyat.textstudio.font.client.effect.RenderState;

public final class WaveEffect implements IEffect {
    @Override
    public boolean isActive(RenderState state) {
        return state.config != null && state.config.wave;
    }

    @Override
    public void apply(RenderState state) {
        float amplitude = (float) state.config.waveAmp;
        float speed = (float) state.config.waveSpeed;
        state.y += (float) (Math.sin(state.time * 0.01 * speed + state.index * 0.6) * 2.0 * amplitude);
    }
}
