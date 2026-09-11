package dev.xyat.textstudio.font.client.effect.impl;

import dev.xyat.textstudio.font.client.effect.IEffect;
import dev.xyat.textstudio.font.client.effect.RenderState;
import net.minecraft.util.Mth;

public final class RainbowEffect implements IEffect {
    @Override
    public boolean isActive(RenderState state) {
        return state.rainbow || state.config != null && state.config.useRainbow;
    }

    @Override
    public void apply(RenderState state) {
        if (state.a <= 0.0f) {
            return;
        }

        float speed = state.config != null ? (float) state.config.rainbowSpeed : 1.0f;
        float spread = state.config != null && state.config.rainbowSpread > 0.0
                ? (float) state.config.rainbowSpread
                : 0.05f;
        float hueSpeed = 20.0f * speed * spread;
        float hue = ((state.time * 0.001f * hueSpeed) - (state.index * spread)) % 1.0f;
        if (hue < 0.0f) {
            hue += 1.0f;
        }

        int color = Mth.hsvToRgb(hue, 1.0f, 1.0f);
        state.r = ((color >> 16) & 0xFF) / 255.0f;
        state.g = ((color >> 8) & 0xFF) / 255.0f;
        state.b = (color & 0xFF) / 255.0f;
    }
}
