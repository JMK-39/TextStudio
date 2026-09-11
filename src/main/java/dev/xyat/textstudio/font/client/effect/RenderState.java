package dev.xyat.textstudio.font.client.effect;

import dev.xyat.textstudio.font.api.IStyle;
import dev.xyat.textstudio.font.config.AuthorConfig;

public final class RenderState {
    private static final ThreadLocal<RenderState> INSTANCE = ThreadLocal.withInitial(RenderState::new);

    public int index;
    public int effectLength;
    public long time;
    public float baseX;
    public float baseY;
    public float x;
    public float y;
    public float r;
    public float g;
    public float b;
    public float a;
    public float scale;
    public boolean rainbow;
    public boolean bold;
    public boolean strike;
    public boolean jitter;
    public boolean glitch;
    public boolean advancedAllowed;
    public AuthorConfig.EffectSettings config;

    private RenderState() {
    }

    public static RenderState get() {
        return INSTANCE.get();
    }

    public void init(int index, int effectLength, long time, float x, float y, float r, float g, float b, float a, IStyle.TextEffectStyleData styleData) {
        this.index = index;
        this.effectLength = Math.max(1, effectLength);
        this.time = time;
        this.baseX = x;
        this.baseY = y;
        this.x = x;
        this.y = y;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.scale = 1.0f;
        this.advancedAllowed = styleData.advancedAllowed;
        this.rainbow = styleData.rainbow;
        this.bold = styleData.bold;
        this.strike = styleData.strike;
        this.jitter = styleData.advancedAllowed && styleData.jitter;
        this.glitch = styleData.advancedAllowed && styleData.glitch;
        if (styleData.customConfig != null) {
            this.config = styleData.customConfig;
        } else {
            this.config = AuthorConfig.getRenderSettings(styleData.effectId, styleData.advancedAllowed);
        }
    }

    public void setAlpha(float value) {
        this.a = value;
    }

    public void offset(float dx, float dy) {
        this.x += dx;
        this.y += dy;
    }
}
