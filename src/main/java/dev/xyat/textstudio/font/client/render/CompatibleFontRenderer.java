package dev.xyat.textstudio.font.client.render;

import javax.annotation.Nonnull;

import dev.xyat.textstudio.font.api.IStyle;
import dev.xyat.textstudio.font.client.effect.AnimationPerformance;
import dev.xyat.textstudio.font.client.effect.EffectManager;
import dev.xyat.textstudio.font.client.effect.RenderState;
import dev.xyat.textstudio.font.client.effect.VisualEffectMath;
import dev.xyat.textstudio.font.client.parser.TextProcessor;
import dev.xyat.textstudio.font.config.AuthorConfig;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.Arrays;

public final class CompatibleFontRenderer {
    private static final ThreadLocal<RenderContext> CONTEXT = ThreadLocal.withInitial(RenderContext::new);

    private CompatibleFontRenderer() {
    }

    public static Integer tryMeasureString(Font font, String text) {
        RenderContext context = CONTEXT.get();
        if (context.rendering || text == null || !TextProcessor.hasMarkers(text)) {
            return null;
        }

        context.buffer.clear();
        context.bufferSink.set(context.buffer);
        TextProcessor.iterateFormatted(text, 0, Style.EMPTY, Style.EMPTY, context.bufferSink);
        if (!context.buffer.hasTextEffect) {
            return null;
        }

        context.measure.set(context.buffer);
        context.rendering = true;
        try {
            return font.width(context.measure);
        } finally {
            context.rendering = false;
        }
    }

    public static Integer tryRenderString(
            Font font,
            String text,
            float x,
            float y,
            int color,
            boolean shadow,
            Matrix4fc matrix,
            MultiBufferSource buffers,
            Font.DisplayMode mode,
            int backgroundColor,
            int packedLight
    ) {
        RenderContext context = CONTEXT.get();
        if (context.rendering || text == null || !TextProcessor.hasMarkers(text)) {
            return null;
        }

        context.buffer.clear();
        context.bufferSink.set(context.buffer);
        TextProcessor.iterateFormatted(text, 0, Style.EMPTY, Style.EMPTY, context.bufferSink);
        if (!context.buffer.hasTextEffect) {
            return null;
        }
        return render(font, context, x, y, color, shadow, context.bufferOut.set(matrix, buffers, mode, backgroundColor, packedLight));
    }

    public static Integer tryRenderComponent(
            Font font,
            Component component,
            float x,
            float y,
            int color,
            boolean shadow,
            Matrix4fc matrix,
            MultiBufferSource buffers,
            Font.DisplayMode mode,
            int backgroundColor,
            int packedLight
    ) {
        RenderContext context = CONTEXT.get();
        if (context.rendering || component == null) {
            return null;
        }
        Integer result = tryRenderSequence(font, component.getVisualOrderText(), x, y, color, shadow, matrix, buffers, mode, backgroundColor, packedLight);
        if (result != null) {
            return result;
        }
        if (context.probe.markerSeen) {
            String flatText = component.getString();
            if (TextProcessor.hasMarkers(flatText)) {
                return tryRenderString(font, flatText, x, y, color, shadow, matrix, buffers, mode, backgroundColor, packedLight);
            }
        }
        return null;
    }

    public static Integer tryRenderSequence(
            Font font,
            FormattedCharSequence sequence,
            float x,
            float y,
            int color,
            boolean shadow,
            Matrix4fc matrix,
            MultiBufferSource buffers,
            Font.DisplayMode mode,
            int backgroundColor,
            int packedLight
    ) {
        RenderContext context = CONTEXT.get();
        if (context.rendering) {
            return null;
        }
        return tryRenderSequence(font, sequence, x, y, color, shadow, context.bufferOut.set(matrix, buffers, mode, backgroundColor, packedLight));
    }

    /**
     * Draws effect text glyph by glyph through { out}; returns null, drawing nothing, when the text has no
     * effects. 26.1 GUI text uses this with an output that emits one GUI text state per glyph pass.
     */
    public static Integer tryRenderSequence(
            Font font,
            FormattedCharSequence sequence,
            float x,
            float y,
            int color,
            boolean shadow,
            GlyphOut out
    ) {
        RenderContext context = CONTEXT.get();
        if (context.rendering || sequence == null) {
            return null;
        }

        context.probe.reset();
        sequence.accept(context.probe);

        if (context.probe.markerSeen && parseRawSequence(sequence, context)) {
            return render(font, context, x, y, color, shadow, out);
        }

        if (!context.probe.found) {
            return null;
        }

        context.buffer.clear();
        context.bufferSink.set(context.buffer);
        sequence.accept(context.bufferSink);
        return render(font, context, x, y, color, shadow, out);
    }

    public static Integer tryMeasureSequence(Font font, FormattedCharSequence sequence) {
        RenderContext context = CONTEXT.get();
        if (context.rendering || sequence == null) {
            return null;
        }

        context.probe.reset();
        sequence.accept(context.probe);

        boolean parsedMarkers = context.probe.markerSeen && parseRawSequence(sequence, context);
        if (!parsedMarkers) {
            if (!context.probe.found) {
                return null;
            }
            context.buffer.clear();
            context.bufferSink.set(context.buffer);
            sequence.accept(context.bufferSink);
        }

        context.measure.set(context.buffer);
        context.rendering = true;
        try {
            return font.width(context.measure);
        } finally {
            context.rendering = false;
        }
    }

    private static boolean parseRawSequence(FormattedCharSequence sequence, RenderContext context) {
        context.rawText.clear();
        sequence.accept(context.rawText);
        if (!TextProcessor.hasMarkers(context.rawText.text.toString())) {
            return false;
        }

        context.buffer.clear();
        context.bufferSink.set(context.buffer);
        for (int run = 0; run < context.rawText.runCount; run++) {
            int start = context.rawText.runStarts[run];
            int end = run + 1 < context.rawText.runCount ? context.rawText.runStarts[run + 1] : context.rawText.text.length();
            String text = context.rawText.text.substring(start, end);
            Style baseStyle = context.rawText.runStyles[run];
            TextProcessor.iterateFormatted(text, 0, baseStyle, baseStyle, context.bufferSink);
        }
        return context.buffer.hasTextEffect;
    }

    private static boolean effectsDiffer(IStyle.TextEffectStyleData first, IStyle.TextEffectStyleData second) {
        if (first == second) {
            return false;
        }
        if (first == null || second == null) {
            return true;
        }
        if (first.customConfig != null || second.customConfig != null) {
            return first.customConfig != second.customConfig;
        }
        return first.pack() != second.pack();
    }

    private static int countEffectRun(GlyphBuffer buffer, int start, IStyle.TextEffectStyleData data) {
        int end = start;
        while (end < buffer.size) {
            Style style = buffer.styles[end];
            if (!((Object) style instanceof IStyle effectStyle) || effectsDiffer(data, effectStyle.textstudio_font$getStyleData())) {
                break;
            }
            end++;
        }
        return Math.max(1, end - start);
    }

    private static int render(
            Font font,
            RenderContext context,
            float x,
            float y,
            int color,
            boolean shadow,
            GlyphOut out
    ) {
        GlyphBuffer buffer = context.buffer;
        int baseAlpha = color >>> 24;
        if (baseAlpha == 0) {
            baseAlpha = 255;
        }
        int baseRgb = color & 0xFFFFFF;
        long time = AnimationPerformance.quantizeTime(Util.getMillis(), AuthorConfig.getRefreshIntervalMs());
        float cursorX = x;

        context.rendering = true;
        try {
            int visualIndex = 0;
            int effectIndex = 0;
            int effectLength = 1;
            int animatedLength = 1;
            AuthorConfig.EffectSettings currentRunConfig = null;
            IStyle.TextEffectStyleData previousData = null;
            while (visualIndex < buffer.size) {
                Style style = buffer.styles[visualIndex];
                IStyle.TextEffectStyleData data = (Object) style instanceof IStyle effectStyle
                        ? effectStyle.textstudio_font$getStyleData()
                        : null;

                if (data == null) {
                    previousData = null;
                    currentRunConfig = null;
                    effectIndex = 0;
                    effectLength = 1;
                    int end = visualIndex + 1;
                    while (end < buffer.size) {
                        Style nextStyle = buffer.styles[end];
                        if ((Object) nextStyle instanceof IStyle nextEffectStyle && nextEffectStyle.textstudio_font$getStyleData() != null) {
                            break;
                        }
                        end++;
                    }
                    context.range.set(buffer, visualIndex, end);
                    out.draw(font, context.range, cursorX, y, color, shadow, true, 1.0f, 0.0f, 0.0f);
                    cursorX += font.width(context.range);
                    visualIndex = end;
                    continue;
                }

                boolean newEffectRun = effectsDiffer(previousData, data);
                if (newEffectRun) {
                    effectIndex = 0;
                    effectLength = countEffectRun(buffer, visualIndex, data);
                    animatedLength = AnimationPerformance.animatedGlyphLimit(effectLength, AuthorConfig.getMaxAnimatedGlyphs());
                    currentRunConfig = resolveSettings(data);
                }
                previousData = data;

                int codePoint = buffer.codePoints[visualIndex];
                int sourceRgb = baseRgb;
                TextColor styleColor = style.getColor();
                if (styleColor != null) {
                    sourceRgb = styleColor.getValue();
                }

                RenderState state = RenderState.get();
                state.init(
                        effectIndex,
                        effectLength,
                        time,
                        cursorX,
                        y,
                        ((sourceRgb >> 16) & 0xFF) / 255.0f,
                        ((sourceRgb >> 8) & 0xFF) / 255.0f,
                        (sourceRgb & 0xFF) / 255.0f,
                        baseAlpha / 255.0f,
                        data
                );

                Style measureStyle = style;
                if (state.bold && !measureStyle.isBold()) {
                    measureStyle = measureStyle.withBold(true);
                }
                int advance = context.advanceCache.width(font, measureStyle, codePoint, context.single);

                if (effectIndex < animatedLength) {
                    if (!context.effectCache.restore(state, data, sourceRgb, baseAlpha)) {
                        EffectManager.applyAll(state);
                        context.effectCache.store(state, data, sourceRgb, baseAlpha);
                    }
                    if (state.a > 0.001f) {
                        drawEffectGlyph(
                                font,
                                context,
                                state,
                                style,
                                codePoint,
                                advance,
                                shadow,
                                out
                        );
                    }
                } else {
                    context.single.set(measureStyle, codePoint);
                    int plainColor = baseAlpha << 24 | sourceRgb;
                    if (isEmissiveGlow(currentRunConfig)) {
                        drawEmissiveGlowGlyph(
                                font,
                                context,
                                context.single,
                                cursorX,
                                y,
                                advance,
                                plainColor,
                                sourceRgb,
                                baseAlpha / 255.0f,
                                currentRunConfig,
                                out,
                                1.0f,
                                0.0f,
                                0.0f
                        );
                    } else {
                        out.draw(font, context.single, cursorX, y, plainColor, shadow, true, 1.0f, 0.0f, 0.0f);
                    }
                }

                cursorX += advance;
                visualIndex++;
                effectIndex++;
            }
        } finally {
            context.rendering = false;
        }
        return (int) Math.ceil(cursorX);
    }

    private static AuthorConfig.EffectSettings resolveSettings(IStyle.TextEffectStyleData data) {
        if (data == null || !data.advancedAllowed) {
            return null;
        }
        if (data.customConfig != null) {
            return data.customConfig;
        }
        return AuthorConfig.getRenderSettings(data.effectId, true);
    }

    private static boolean isEmissiveGlow(AuthorConfig.EffectSettings config) {
        return config != null && config.glow && config.glowAlpha > 0.001;
    }

    private static void drawEffectGlyph(
            Font font,
            RenderContext context,
            RenderState state,
            Style sourceStyle,
            int codePoint,
            int advance,
            boolean shadow,
            GlyphOut out
    ) {
        int rgb = toRgb(state.r, state.g, state.b);
        int alpha = Math.max(1, Math.min(255, Math.round(clamp01(state.a) * 255.0f)));
        int finalColor = alpha << 24 | rgb;

        Style renderedStyle = context.styleCache.color(sourceStyle, rgb);
        if (state.bold && !renderedStyle.isBold()) {
            renderedStyle = renderedStyle.withBold(true);
        }
        if (state.strike && !renderedStyle.isStrikethrough()) {
            renderedStyle = renderedStyle.withStrikethrough(true);
        }

        // Glyphs are scaled around their centre.
        float glyphScale = Math.max(0.10f, Math.min(4.0f, state.scale));
        float centerX = state.x + Math.max(1.0f, advance) * 0.5f;
        float centerY = state.y + font.lineHeight * 0.5f;

        context.single.set(renderedStyle, codePoint);
        AuthorConfig.EffectSettings config = state.config;
        int passBudget = AuthorConfig.getExtraPassBudget();

        if (config != null && config.extrude && passBudget > 0) {
            int passes = Math.min(passBudget, 3);
            float depth = (float) Math.max(0.25, Math.min(4.0, config.extrudeDepth));
            int darkRgb = scaleRgb(rgb, 0.18f);
            for (int i = passes; i >= 1; i--) {
                float step = depth * i / passes;
                float passAlpha = state.a * (float) Math.max(0.0, Math.min(1.0, config.extrudeAlpha)) * (0.45f + 0.55f * i / passes);
                drawOffsetPass(font, context, renderedStyle, codePoint, state.x + step, state.y + step, darkRgb, passAlpha, out, glyphScale, centerX, centerY);
            }
            passBudget -= passes;
        }

        if (config != null && config.trail && passBudget > 0) {
            int passes = Math.min(passBudget, 2);
            float strength = (float) Math.max(0.0, Math.min(3.0, config.trailStrength));
            float dx = VisualEffectMath.trailX(state.x, state.baseX, strength);
            float dy = VisualEffectMath.trailY(state.y, state.baseY, strength);
            if (Math.abs(dx) + Math.abs(dy) < 0.05f) {
                double phase = state.time * 0.004 + state.index * 0.67;
                dx = (float) (-Math.cos(phase) * 0.9 * strength);
                dy = (float) (-Math.sin(phase) * 0.45 * strength);
            }
            for (int i = passes; i >= 1; i--) {
                float ratio = i / (float) passes;
                float passAlpha = state.a * (float) Math.max(0.0, Math.min(1.0, config.trailAlpha)) * (0.45f + 0.35f * ratio);
                drawOffsetPass(font, context, renderedStyle, codePoint, state.x + dx * ratio, state.y + dy * ratio, rgb, passAlpha, out, glyphScale, centerX, centerY);
            }
            passBudget -= passes;
        }

        if (config != null && config.outline && passBudget > 0) {
            int passes = Math.min(passBudget, 4);
            float width = (float) Math.max(0.35, Math.min(2.5, config.outlineWidth));
            float outlineAlpha = state.a * (float) Math.max(0.0, Math.min(1.0, config.outlineAlpha));
            int outlineRgb = scaleRgb(rgb, 0.08f);
            drawCardinalPasses(font, context, renderedStyle, codePoint, state.x, state.y, width, outlineRgb, outlineAlpha, passes, out, glyphScale, centerX, centerY);
            passBudget -= passes;
        }

        if (config != null && config.chromatic && config.chromaticAlpha > 0.0 && passBudget > 0) {
            float offset = (float) config.chromaticOffset;
            if (config.chromaticPulse) {
                offset *= (float) (1.0 + 0.5 * Math.sin(state.time * 0.005));
            }
            float chromaticAlpha = clamp01((float) config.chromaticAlpha * state.a);
            if (passBudget > 0) {
                drawOffsetPass(font, context, renderedStyle, codePoint, state.x - offset, state.y, 0x00FFFF, chromaticAlpha, out, glyphScale, centerX, centerY);
                passBudget--;
            }
            if (passBudget > 0) {
                drawOffsetPass(font, context, renderedStyle, codePoint, state.x + offset, state.y, 0xFF0000, chromaticAlpha, out, glyphScale, centerX, centerY);
            }
        }

        context.single.set(renderedStyle, codePoint);
        if (isEmissiveGlow(config)) {
            drawEmissiveGlowGlyph(
                    font,
                    context,
                    context.single,
                    state.x,
                    state.y,
                    advance,
                    finalColor,
                    rgb,
                    state.a,
                    config,
                    out,
                    glyphScale,
                    centerX,
                    centerY
            );
        } else {
            out.draw(font, context.single, state.x, state.y, finalColor, shadow, true, glyphScale, centerX, centerY);
        }
    }


    private static void drawEmissiveGlowGlyph(
            Font font,
            RenderContext context,
            FormattedCharSequence sequence,
            float x,
            float y,
            int advance,
            int bodyColor,
            int rgb,
            float glyphAlpha,
            AuthorConfig.EffectSettings config,
            GlyphOut out,
            float scale,
            float centerX,
            float centerY
    ) {
        int outlineColor = ScreenSpaceGlowMath.outlineArgb(
                rgb,
                glyphAlpha,
                config.glowAlpha,
                config.glowRadius
        );
        out.drawOutlined(font, sequence, x, y, bodyColor, outlineColor, scale, centerX, centerY);
    }

    private static void drawCardinalPasses(
            Font font,
            RenderContext context,
            Style style,
            int codePoint,
            float x,
            float y,
            float distance,
            int rgb,
            float alpha,
            int passes,
            GlyphOut out,
            float scale,
            float centerX,
            float centerY
    ) {
        for (int i = 0; i < passes; i++) {
            float dx = 0.0f;
            float dy = 0.0f;
            switch (i) {
                case 0 -> dx = -distance;
                case 1 -> dx = distance;
                case 2 -> dy = -distance;
                default -> dy = distance;
            }
            drawOffsetPass(font, context, style, codePoint, x + dx, y + dy, rgb, alpha, out, scale, centerX, centerY);
        }
    }

    private static void drawOffsetPass(
            Font font,
            RenderContext context,
            Style baseStyle,
            int codePoint,
            float x,
            float y,
            int rgb,
            float alpha,
            GlyphOut out,
            float scale,
            float centerX,
            float centerY
    ) {
        if (alpha <= 0.001f) {
            return;
        }
        int a = Math.max(1, Math.min(255, Math.round(clamp01(alpha) * 255.0f)));
        Style style = context.styleCache.color(baseStyle, rgb);
        context.single.set(style, codePoint);
        out.draw(font, context.single, x, y, a << 24 | rgb, false, false, scale, centerX, centerY);
    }

    private static int scaleRgb(int rgb, float factor) {
        int r = Math.max(0, Math.min(255, Math.round(((rgb >> 16) & 0xFF) * factor)));
        int g = Math.max(0, Math.min(255, Math.round(((rgb >> 8) & 0xFF) * factor)));
        int b = Math.max(0, Math.min(255, Math.round((rgb & 0xFF) * factor)));
        return r << 16 | g << 8 | b;
    }

    private static int toRgb(float red, float green, float blue) {
        int r = Math.round(clamp01(red) * 255.0f);
        int g = Math.round(clamp01(green) * 255.0f);
        int b = Math.round(clamp01(blue) * 255.0f);
        return r << 16 | g << 8 | b;
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }


    /**
     * Where effect glyphs go. Each call draws text at x, y, scaled by {@code scale} around (centerX, centerY);
     * {@code background} is false for the extra passes, so a text background is drawn once.
     */
    public interface GlyphOut {
        void draw(Font font, FormattedCharSequence text, float x, float y, int color, boolean shadow, boolean background,
                  float scale, float centerX, float centerY);

        /** Draws text with an eight-direction outline, for the emissive glow. */
        void drawOutlined(Font font, FormattedCharSequence text, float x, float y, int bodyColor, int outlineColor,
                          float scale, float centerX, float centerY);
    }

    // Full block and sky light, as LightTexture.FULL_BRIGHT.
    private static final int FULL_BRIGHT = 0xF000F0;

    /** Draws into a MultiBufferSource: world text on every version, and GUI text before 26.1. */
    private static final class BufferGlyphOut implements GlyphOut {
        private final Matrix4f scaled = new Matrix4f();
        private final Matrix4f copy = new Matrix4f();
        private Matrix4fc matrix;
        private MultiBufferSource buffers;
        private Font.DisplayMode mode;
        private int backgroundColor;
        private int packedLight;

        private BufferGlyphOut set(Matrix4fc matrix, MultiBufferSource buffers, Font.DisplayMode mode, int backgroundColor, int packedLight) {
            this.matrix = matrix;
            this.buffers = buffers;
            this.mode = mode;
            this.backgroundColor = backgroundColor;
            this.packedLight = packedLight;
            return this;
        }

        private Matrix4fc matrix(float scale, float centerX, float centerY) {
            if (Math.abs(scale - 1.0f) <= 0.0001f) {
                return matrix;
            }
            return scaled.set(matrix)
                    .translate(centerX, centerY, 0.0f)
                    .scale(scale, scale, 1.0f)
                    .translate(-centerX, -centerY, 0.0f);
        }

        // Font takes a Matrix4fc on 26.1 and a Matrix4f before.
        //? if >=26.1 {
        /*private Matrix4fc drawMatrix(float scale, float centerX, float centerY) {
            return matrix(scale, centerX, centerY);
        }
        *///?} else {
        private Matrix4f drawMatrix(float scale, float centerX, float centerY) {
            Matrix4fc drawn = matrix(scale, centerX, centerY);
            return drawn instanceof Matrix4f plain ? plain : copy.set(drawn);
        }
        //?}

        @Override
        public void draw(Font font, FormattedCharSequence text, float x, float y, int color, boolean shadow, boolean background,
                         float scale, float centerX, float centerY) {
            font.drawInBatch(text, x, y, color, shadow, drawMatrix(scale, centerX, centerY), buffers, mode,
                    background ? backgroundColor : 0, packedLight);
        }

        @Override
        public void drawOutlined(Font font, FormattedCharSequence text, float x, float y, int bodyColor, int outlineColor,
                                 float scale, float centerX, float centerY) {
            font.drawInBatch8xOutline(text, x, y, bodyColor, outlineColor, drawMatrix(scale, centerX, centerY), buffers, FULL_BRIGHT);
        }
    }

    private static final class RenderContext {
        private final GlyphBuffer buffer = new GlyphBuffer();
        private final MutableSingleSequence single = new MutableSingleSequence();
        private final MutableRangeSequence range = new MutableRangeSequence();
        private final MutableMeasureSequence measure = new MutableMeasureSequence();
        private final BufferSink bufferSink = new BufferSink();
        private final EffectProbeSink probe = new EffectProbeSink();
        private final RawTextSink rawText = new RawTextSink();
        private final BufferGlyphOut bufferOut = new BufferGlyphOut();
        private final GlyphAdvanceCache advanceCache = new GlyphAdvanceCache();
        private final ColoredStyleCache styleCache = new ColoredStyleCache();
        private final EffectStateCache effectCache = new EffectStateCache();
        private boolean rendering;
    }

    private static final class GlyphAdvanceCache {
        private static final int SIZE = 1024;
        private final Font[] fonts = new Font[SIZE];
        private final Object[] fontIds = new Object[SIZE];
        private final int[] codePoints = new int[SIZE];
        private final boolean[] bold = new boolean[SIZE];
        private final int[] widths = new int[SIZE];

        private int width(Font font, Style style, int codePoint, MutableSingleSequence single) {
            if (style.isObfuscated()) {
                single.set(style, codePoint);
                return Math.max(0, font.width(single));
            }
            Object fontId = style.getFont();
            int hash = System.identityHashCode(font) * 31 + codePoint;
            hash = hash * 31 + (style.isBold() ? 1 : 0);
            hash = hash * 31 + (fontId == null ? 0 : fontId.hashCode());
            int slot = hash & (SIZE - 1);
            if (fonts[slot] == font
                    && codePoints[slot] == codePoint
                    && bold[slot] == style.isBold()
                    && (fontIds[slot] == fontId || fontIds[slot] != null && fontIds[slot].equals(fontId))) {
                return widths[slot];
            }
            single.set(style, codePoint);
            int width = Math.max(0, font.width(single));
            fonts[slot] = font;
            fontIds[slot] = fontId;
            codePoints[slot] = codePoint;
            bold[slot] = style.isBold();
            widths[slot] = width;
            return width;
        }
    }

    private static final class ColoredStyleCache {
        private static final int SIZE = 256;
        private final Style[] bases = new Style[SIZE];
        private final int[] colors = new int[SIZE];
        private final Style[] values = new Style[SIZE];

        private Style color(Style base, int rgb) {
            TextColor current = base.getColor();
            if (current != null && current.getValue() == rgb) {
                return base;
            }
            int hash = System.identityHashCode(base) * 31 + rgb;
            int slot = hash & (SIZE - 1);
            if (bases[slot] == base && colors[slot] == rgb && values[slot] != null) {
                return values[slot];
            }
            Style value = base.withColor(TextColor.fromRgb(rgb));
            bases[slot] = base;
            colors[slot] = rgb;
            values[slot] = value;
            return value;
        }
    }

    private static final class EffectStateCache {
        private static final int SIZE = 2048;
        private final long[] keys = new long[SIZE];
        private final long[] times = new long[SIZE];
        private final float[] dx = new float[SIZE];
        private final float[] dy = new float[SIZE];
        private final float[] r = new float[SIZE];
        private final float[] g = new float[SIZE];
        private final float[] b = new float[SIZE];
        private final float[] a = new float[SIZE];
        private final float[] scale = new float[SIZE];
        private final boolean[] used = new boolean[SIZE];

        private boolean restore(RenderState state, IStyle.TextEffectStyleData data, int sourceRgb, int baseAlpha) {
            long key = key(state, data, sourceRgb, baseAlpha);
            int slot = slot(key, state.time);
            if (!used[slot] || keys[slot] != key || times[slot] != state.time) {
                return false;
            }
            state.x = state.baseX + dx[slot];
            state.y = state.baseY + dy[slot];
            state.r = r[slot];
            state.g = g[slot];
            state.b = b[slot];
            state.a = a[slot];
            state.scale = scale[slot];
            return true;
        }

        private void store(RenderState state, IStyle.TextEffectStyleData data, int sourceRgb, int baseAlpha) {
            long key = key(state, data, sourceRgb, baseAlpha);
            int slot = slot(key, state.time);
            used[slot] = true;
            keys[slot] = key;
            times[slot] = state.time;
            dx[slot] = state.x - state.baseX;
            dy[slot] = state.y - state.baseY;
            r[slot] = state.r;
            g[slot] = state.g;
            b[slot] = state.b;
            a[slot] = state.a;
            scale[slot] = state.scale;
        }

        private static long key(RenderState state, IStyle.TextEffectStyleData data, int sourceRgb, int baseAlpha) {
            long value = data.pack() & 0xFFFFFFFFL;
            value = mix(value ^ ((long) System.identityHashCode(state.config) << 32));
            value = mix(value ^ ((long) state.index << 40) ^ ((long) state.effectLength << 20));
            value = mix(value ^ ((long) sourceRgb << 8) ^ baseAlpha);
            return value;
        }

        private static int slot(long key, long time) {
            return (int) mix(key ^ time) & (SIZE - 1);
        }

        private static long mix(long value) {
            value ^= value >>> 33;
            value *= 0xff51afd7ed558ccdl;
            value ^= value >>> 33;
            value *= 0xc4ceb9fe1a85ec53l;
            value ^= value >>> 33;
            return value;
        }
    }

    private static final class GlyphBuffer {
        private Style[] styles = new Style[64];
        private int[] codePoints = new int[64];
        private int size;
        private boolean hasTextEffect;

        private void clear() {
            Arrays.fill(styles, 0, size, null);
            size = 0;
            hasTextEffect = false;
        }

        private void add(Style style, int codePoint) {
            ensureCapacity(size + 1);
            styles[size] = style;
            codePoints[size] = codePoint;
            if ((Object) style instanceof IStyle effectStyle && effectStyle.textstudio_font$getStyleData() != null) {
                hasTextEffect = true;
            }
            size++;
        }

        private void ensureCapacity(int required) {
            if (required <= styles.length) {
                return;
            }
            int next = Math.max(required, styles.length << 1);
            styles = Arrays.copyOf(styles, next);
            codePoints = Arrays.copyOf(codePoints, next);
        }
    }


    private static final class BufferSink implements FormattedCharSink {
        private GlyphBuffer buffer;

        private void set(GlyphBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public boolean accept(int index, @Nonnull Style style, int codePoint) {
            buffer.add(style, codePoint);
            return true;
        }
    }


    private static final class RawTextSink implements FormattedCharSink {
        private final StringBuilder text = new StringBuilder(128);
        private Style[] runStyles = new Style[8];
        private int[] runStarts = new int[8];
        private int runCount;

        private void clear() {
            text.setLength(0);
            runCount = 0;
        }

        @Override
        public boolean accept(int index, @Nonnull Style style, int codePoint) {
            if (runCount == 0 || stylesDiffer(runStyles[runCount - 1], style)) {
                ensureCapacity(runCount + 1);
                runStyles[runCount] = style;
                runStarts[runCount] = text.length();
                runCount++;
            }
            text.appendCodePoint(codePoint);
            return true;
        }

        private void ensureCapacity(int required) {
            if (required <= runStyles.length) {
                return;
            }
            int next = runStyles.length << 1;
            while (next < required) {
                next <<= 1;
            }
            runStyles = Arrays.copyOf(runStyles, next);
            runStarts = Arrays.copyOf(runStarts, next);
        }

        private static boolean stylesDiffer(Style first, Style second) {
            return first != second && (first == null || !first.equals(second));
        }
    }

    private static final class EffectProbeSink implements FormattedCharSink {
        private boolean found;
        private boolean markerSeen;

        private void reset() {
            found = false;
            markerSeen = false;
        }

        @Override
        public boolean accept(int index, @Nonnull Style style, int codePoint) {
            if (codePoint == '$' || codePoint == 0x2063) {
                markerSeen = true;
            }
            if ((Object) style instanceof IStyle effectStyle && effectStyle.textstudio_font$getStyleData() != null) {
                found = true;
            }
            return true;
        }
    }

    private static final class MutableMeasureSequence implements FormattedCharSequence {
        private GlyphBuffer buffer;

        private void set(GlyphBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public boolean accept(@Nonnull FormattedCharSink sink) {
            for (int i = 0; i < buffer.size; i++) {
                Style style = buffer.styles[i];
                if ((Object) style instanceof IStyle effectStyle) {
                    IStyle.TextEffectStyleData data = effectStyle.textstudio_font$getStyleData();
                    if (data != null) {
                        boolean bold = data.bold;
                        if (bold && !style.isBold()) {
                            style = style.withBold(true);
                        }
                    }
                }
                if (!sink.accept(i, style, buffer.codePoints[i])) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final class MutableSingleSequence implements FormattedCharSequence {
        private Style style = Style.EMPTY;
        private int codePoint;

        private void set(Style style, int codePoint) {
            this.style = style;
            this.codePoint = codePoint;
        }

        @Override
        public boolean accept(@Nonnull FormattedCharSink sink) {
            return sink.accept(0, style, codePoint);
        }
    }

    private static final class MutableRangeSequence implements FormattedCharSequence {
        private GlyphBuffer buffer;
        private int start;
        private int end;

        private void set(GlyphBuffer buffer, int start, int end) {
            this.buffer = buffer;
            this.start = start;
            this.end = end;
        }

        @Override
        public boolean accept(@Nonnull FormattedCharSink sink) {
            for (int i = start; i < end; i++) {
                if (!sink.accept(i - start, buffer.styles[i], buffer.codePoints[i])) {
                    return false;
                }
            }
            return true;
        }
    }
}
