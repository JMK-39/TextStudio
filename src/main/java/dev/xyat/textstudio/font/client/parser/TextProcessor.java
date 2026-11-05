package dev.xyat.textstudio.font.client.parser;

import dev.xyat.textstudio.font.api.IStyle;
import dev.xyat.textstudio.font.config.AuthorConfig;
import dev.xyat.textstudio.font.mixin.StyleConstructionAccess;
import dev.xyat.textstudio.font.mixin.client.StringDecomposerAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;

import java.util.function.IntFunction;

public final class TextProcessor {
    private TextProcessor() {
    }

    public static boolean iterateFormatted(String string, int startIndex, Style style, Style plainStyle, FormattedCharSink sink) {
        int length = string.length();
        Style currentStyle = style;
        boolean advancedAllowed = advancedAllowed(style);

        for (int i = startIndex; i < length; ++i) {
            char c = string.charAt(i);
            if (c == '§') {
                if (i + 1 >= length) {
                    break;
                }
                ChatFormatting formatting = ChatFormatting.getByCode(string.charAt(i + 1));
                if (formatting != null) {
                    currentStyle = formatting == ChatFormatting.RESET ? plainStyle : currentStyle.applyLegacyFormat(formatting);
                }
                ++i;
                continue;
            }

            if (c == '\u2063') {
                if (CompactTagCodec.isResetAt(string, i)) {
                    currentStyle = clearTextEffectStyle(currentStyle);
                    i += CompactTagCodec.resetLength() - 1;
                    continue;
                }

                CompactTagCodec.PresetToken presetToken = CompactTagCodec.readPreset(string, i);
                if (presetToken != null) {
                    currentStyle = applyPresetStyle(currentStyle, presetToken.presetId(), advancedAllowed);
                    i = presetToken.endIndex() - 1;
                    continue;
                }

                CompactTagCodec.CustomToken customToken = CompactTagCodec.readCustom(string, i);
                if (customToken != null) {
                    currentStyle = applyCustomStyle(currentStyle, customToken.settings(), advancedAllowed);
                    i = customToken.endIndex() - 1;
                    continue;
                }
            }

            if (Character.isHighSurrogate(c)) {
                if (i + 1 >= length) {
                    return !sink.accept(i, currentStyle, 65533);
                }
                char d = string.charAt(i + 1);
                if (Character.isLowSurrogate(d)) {
                    if (!sink.accept(i, currentStyle, Character.toCodePoint(c, d))) {
                        return false;
                    }
                    ++i;
                    continue;
                }
                if (!sink.accept(i, currentStyle, 65533)) {
                    return false;
                }
                continue;
            }

            if (!StringDecomposerAccess.callFeedChar(currentStyle, sink, i, c)) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasMarkers(String text) {
        return CompactTagCodec.hasMarker(text);
    }

    /**
     * Parses a complete, already-decomposed sequence. Native styles come from their original UTF-16 offsets;
     * only the compact effect state persists between them, so tokens may cross native style boundaries.
     */
    public static boolean iterateStyled(String text, IntFunction<Style> nativeStyleAt, FormattedCharSink sink) {
        IStyle.TextEffectStyleData activeEffect = null;
        boolean effectOverridden = false;
        Style previousNative = null;
        IStyle.TextEffectStyleData previousEffect = null;
        Style outputStyle = null;
        for (int index = 0; index < text.length();) {
            Style nativeStyle = nativeStyleAt.apply(index);
            if (text.charAt(index) == '\u2063') {
                if (CompactTagCodec.isResetAt(text, index)) {
                    activeEffect = null;
                    effectOverridden = true;
                    index += CompactTagCodec.resetLength();
                    continue;
                }
                CompactTagCodec.PresetToken preset = CompactTagCodec.readPreset(text, index);
                if (preset != null) {
                    activeEffect = new IStyle.TextEffectStyleData(
                            preset.presetId(), false, false, false, false, false, advancedAllowed(nativeStyle));
                    effectOverridden = true;
                    index = preset.endIndex();
                    continue;
                }
                CompactTagCodec.CustomToken custom = CompactTagCodec.readCustom(text, index);
                if (custom != null) {
                    boolean advancedAllowed = advancedAllowed(nativeStyle);
                    activeEffect = new IStyle.TextEffectStyleData(
                            1, false, false, false, false, false, advancedAllowed);
                    activeEffect.customConfig = advancedAllowed ? custom.settings()
                            : AuthorConfig.toPublicSafeSettings(custom.settings());
                    effectOverridden = true;
                    index = custom.endIndex();
                    continue;
                }
            }

            Style style = nativeStyle;
            if (effectOverridden) {
                if (outputStyle == null || nativeStyle != previousNative || activeEffect != previousEffect) {
                    outputStyle = copyWithEffect(nativeStyle, activeEffect);
                    previousNative = nativeStyle;
                    previousEffect = activeEffect;
                }
                style = outputStyle;
            }
            int codePoint = text.codePointAt(index);
            int consumed = Character.charCount(codePoint);
            if (codePoint >= Character.MIN_SURROGATE && codePoint <= Character.MAX_SURROGATE) codePoint = 65533;
            if (!sink.accept(index, style, codePoint)) return false;
            index += consumed;
        }
        return true;
    }

    /** A fresh style prevents effect data from mutating the incoming native style or shared Style.EMPTY. */
    private static Style copyWithEffect(Style original, IStyle.TextEffectStyleData effect) {
        //? if >=26.1 {
        /*Style copy = StyleConstructionAccess.textstudio_font$newStyle(original.getColor(), original.getShadowColor(),
                original.isBold(), original.isItalic(), original.isUnderlined(), original.isStrikethrough(),
                original.isObfuscated(), original.getClickEvent(), original.getHoverEvent(), original.getInsertion(),
                original.getFont());
        *///?} else {
        Style copy = StyleConstructionAccess.textstudio_font$newStyle(original.getColor(), original.isBold(),
                original.isItalic(), original.isUnderlined(), original.isStrikethrough(), original.isObfuscated(),
                original.getClickEvent(), original.getHoverEvent(), original.getInsertion(), original.getFont());
        //?}
        IStyle.TextEffectStyleData outputEffect = effect;
        if (effect != null && effect.advancedAllowed && !advancedAllowed(original)) {
            outputEffect = new IStyle.TextEffectStyleData(effect.effectId, effect.rainbow, effect.bold,
                    effect.strike, effect.jitter, effect.glitch, false);
            outputEffect.customConfig = AuthorConfig.toPublicSafeSettings(effect.customConfig);
        }
        ((IStyle) (Object) copy).textstudio_font$setStyleData(outputEffect);
        return copy;
    }

    private static Style clearTextEffectStyle(Style current) {
        Style style = current.applyFormat(ChatFormatting.WHITE);
        ((IStyle) (Object) style).textstudio_font$setStyleData(null);
        return style;
    }

    private static Style applyPresetStyle(Style current, int presetId, boolean advancedAllowed) {
        Style style = current.applyFormat(ChatFormatting.WHITE);
        IStyle.TextEffectStyleData data = new IStyle.TextEffectStyleData(
                presetId, false, false, false, false, false, advancedAllowed
        );
        ((IStyle) (Object) style).textstudio_font$setStyleData(data);
        return style;
    }

    private static Style applyCustomStyle(Style current, AuthorConfig.EffectSettings settings, boolean advancedAllowed) {
        Style style = current.applyFormat(ChatFormatting.WHITE);
        IStyle.TextEffectStyleData data = new IStyle.TextEffectStyleData(
                1, false, false, false, false, false, advancedAllowed
        );
        data.customConfig = advancedAllowed ? settings : AuthorConfig.toPublicSafeSettings(settings);
        ((IStyle) (Object) style).textstudio_font$setStyleData(data);
        return style;
    }

    private static boolean advancedAllowed(Style style) {
        if ((Object) style instanceof IStyle effectStyle) {
            IStyle.TextEffectStyleData data = effectStyle.textstudio_font$getStyleData();
            if (data != null) {
                return data.advancedAllowed;
            }
        }
        return true;
    }
}
