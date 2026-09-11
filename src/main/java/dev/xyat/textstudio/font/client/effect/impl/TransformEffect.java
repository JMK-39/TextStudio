package dev.xyat.textstudio.font.client.effect.impl;

import dev.xyat.textstudio.font.client.effect.IEffect;
import dev.xyat.textstudio.font.client.effect.RenderState;
import dev.xyat.textstudio.font.config.AuthorConfig;

public final class TransformEffect implements IEffect {
    @Override
    public boolean isActive(RenderState state) {
        if (state.config == null) {
            return state.jitter;
        }
        return state.config.bounce
                || state.config.swing
                || state.config.pend
                || state.config.shake
                || state.config.wiggle
                || state.config.turb
                || state.config.spasm
                || state.config.fade
                || state.config.orbit
                || state.config.ripple
                || state.config.drift
                || state.jitter;
    }

    @Override
    public void apply(RenderState state) {
        long time = state.time;
        AuthorConfig.EffectSettings config = state.config;
        float dx = 0.0f;
        float dy = 0.0f;

        if (config != null) {
            float organic = 0.65f + 0.35f * (float) Math.sin(time * 0.0015);
            if (config.bounce) {
                dy -= (float) (Math.abs(Math.sin(time * 0.005 * config.bounceSpeed - state.index * 0.2)) * 4.0 * config.bounceAmp * organic);
            }
            if (config.swing) {
                dx += (float) (Math.sin(time * 0.01 * config.swingSpeed + state.index * 0.5) * 2.0 * config.swingAmp * organic);
            }
            if (config.pend) {
                dx += (float) (Math.cos(time * 0.005 * config.pendSpeed + state.index * 0.5) * 2.0 * config.pendAmp * organic);
                dy += (float) (Math.sin(time * 0.005 * config.pendSpeed + state.index * 0.5) * 2.0 * config.pendAmp * organic);
            }
            if (config.wiggle) {
                float directionX = (float) Math.sin(state.index * 123.45);
                float directionY = (float) Math.cos(state.index * 123.45);
                float delta = (float) (Math.sin(time * 0.01 * config.wiggleSpeed + state.index * 0.5) * 1.5 * config.wiggleAmp * organic);
                dx += directionX * delta;
                dy += directionY * delta;
            }
            if (config.turb) {
                dx += (float) (Math.sin(time * 0.002 * config.turbSpeed * 1.7 + state.index * 0.3) * 1.5 * config.turbAmp * organic);
                dy += (float) (Math.sin(time * 0.002 * config.turbSpeed * 2.3 + state.index * 0.2) * 1.5 * config.turbAmp * organic);
            }
            if (config.spasm) {
                long duration = Math.max(1L, (long) (60.0 / Math.max(0.1, config.spasmSpeed)));
                long seed = mix((time / duration) * 31L + state.index * 17L);
                if (unit(seed) < 0.15f) {
                    dx += (unit(mix(seed ^ 0x3243F6A8885A308DL)) - 0.5f) * 12.0f * (float) config.spasmAmp;
                    dy += (unit(mix(seed ^ 0x1B56C4E9036499FBL)) - 0.5f) * 12.0f * (float) config.spasmAmp;
                }
            }

            if (config.orbit) {
                double phase = time * 0.004 * config.orbitSpeed + state.index * 0.72;
                dx += (float) (Math.cos(phase) * 1.8 * config.orbitAmp * organic);
                dy += (float) (Math.sin(phase) * 1.15 * config.orbitAmp * organic);
            }
            if (config.ripple) {
                double phase = time * 0.006 * config.rippleSpeed - state.index * 0.78;
                double pulse = Math.sin(phase);
                if (pulse > 0.0) {
                    dy -= (float) (pulse * pulse * 2.6 * config.rippleAmp * organic);
                }
            }
            if (config.drift) {
                double t = time * 0.0015 * config.driftSpeed;
                dx += (float) (Math.sin(t + state.index * 1.37) * 0.85 * config.driftAmp);
                dy += (float) (Math.cos(t * 0.83 + state.index * 0.91) * 1.25 * config.driftAmp);
            }

            if (config.fade) {
                float alpha = (float) (config.fadeMin
                        + (1.0 - config.fadeMin) * (0.5 + 0.5 * Math.sin(time * 0.005 * config.fadeSpeed - state.index * 0.1)));
                state.a *= alpha;
            }
        }

        if (state.jitter || config != null && config.shake) {
            float amplitude = config != null && config.shakeAmp > 0.0 ? (float) config.shakeAmp : 1.0f;
            double speed = config != null ? Math.max(0.1, config.shakeSpeed) : 1.0;
            long hold = Math.max(1L, (long) (30.0 / speed));
            long seed = mix((time / hold) * 31L + state.index * 17L);
            dx += (unit(seed) - 0.5f) * 3.0f * amplitude;
            dy += (unit(mix(seed ^ 0x3243F6A8885A308DL)) - 0.5f) * 3.0f * amplitude;
        }

        state.offset(dx, dy);
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
