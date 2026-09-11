package dev.xyat.textstudio.font.client.effect;

public final class EffectManager {
    private EffectManager() {
    }

    public static void applyAll(RenderState state) {
        for (IEffect effect : EffectRegistry.getPipeline()) {
            if (effect.isActive(state)) {
                effect.apply(state);
                if (state.a <= 0.0f) {
                    return;
                }
            }
        }
    }
}
