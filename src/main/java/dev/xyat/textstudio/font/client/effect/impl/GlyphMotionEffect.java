package dev.xyat.textstudio.font.client.effect.impl;

import dev.xyat.textstudio.font.client.effect.IEffect;
import dev.xyat.textstudio.font.client.effect.AdvancedGlyphMath;
import dev.xyat.textstudio.font.client.effect.RenderState;
import dev.xyat.textstudio.font.config.AuthorConfig;

public final class GlyphMotionEffect implements IEffect {
    @Override
    public boolean isActive(RenderState state) {
        AuthorConfig.EffectSettings config = state.config;
        return config != null && (config.glyphScale
                || config.ecg
                || config.heartbeat
                || config.glitchSpike
                || config.signalLoss);
    }

    @Override
    public void apply(RenderState state) {
        AuthorConfig.EffectSettings config = state.config;
        if (config.glyphScale) {
            state.scale *= AdvancedGlyphMath.scale(state.time, state.index, config.glyphScaleAmp, config.glyphScaleSpeed);
        }
        if (config.ecg) {
            state.y += AdvancedGlyphMath.ecgOffset(
                    state.time,
                    state.index,
                    state.effectLength,
                    config.ecgAmp,
                    config.ecgSpeed,
                    config.ecgWidth
            );
        }
        if (config.heartbeat) {
            state.y += AdvancedGlyphMath.heartbeatOffset(state.time, state.index, config.heartbeatAmp, config.heartbeatSpeed);
        }
        if (config.glitchSpike) {
            state.x += AdvancedGlyphMath.glitchSpikeX(
                    state.time,
                    state.index,
                    config.glitchSpikeAmp,
                    config.glitchSpikeSpeed,
                    config.glitchSpikeChance
            );
        }
        if (config.signalLoss && AdvancedGlyphMath.signalLoss(state.time, state.index, config.signalLossSpeed, config.signalLossChance)) {
            state.setAlpha(0.0f);
        }
    }
}
