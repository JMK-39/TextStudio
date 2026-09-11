package dev.xyat.textstudio.font.client.effect;

import dev.xyat.textstudio.font.client.effect.impl.ColorPulseEffect;
import dev.xyat.textstudio.font.client.effect.impl.GlitchEffect;
import dev.xyat.textstudio.font.client.effect.impl.GlyphMotionEffect;
import dev.xyat.textstudio.font.client.effect.impl.NoteBounceEffect;
import dev.xyat.textstudio.font.client.effect.impl.PaletteEffect;
import dev.xyat.textstudio.font.client.effect.impl.RainbowEffect;
import dev.xyat.textstudio.font.client.effect.impl.SweepEffect;
import dev.xyat.textstudio.font.client.effect.impl.TransformEffect;
import dev.xyat.textstudio.font.client.effect.impl.TypewriterEffect;
import dev.xyat.textstudio.font.client.effect.impl.WaveEffect;

public final class EffectRegistry {
    private static final IEffect[] PIPELINE = {
            new TypewriterEffect(),
            new NoteBounceEffect(),
            new GlyphMotionEffect(),
            new TransformEffect(),
            new WaveEffect(),
            new GlitchEffect(),
            new RainbowEffect(),
            new PaletteEffect(),
            new SweepEffect(),
            new ColorPulseEffect()
    };

    private EffectRegistry() {
    }

    public static IEffect[] getPipeline() {
        return PIPELINE;
    }
}
