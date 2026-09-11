package dev.xyat.textstudio.font.client.effect;

public interface IEffect {
    boolean isActive(RenderState state);
    void apply(RenderState state);
}
