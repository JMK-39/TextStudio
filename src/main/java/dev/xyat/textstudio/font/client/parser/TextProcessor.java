package dev.xyat.textstudio.font.client.parser;

import dev.xyat.textstudio.font.api.IStyle;
import dev.xyat.textstudio.font.config.AuthorConfig;
import dev.xyat.textstudio.font.mixin.client.StringDecomposerAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;

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

    private static Style clearTextEffectStyle(Style current) {
        Style style = current.applyFormat(ChatFormatting.WHITE);
        ((IStyle) style).textstudio_font$setStyleData(null);
        return style;
    }

    private static Style applyPresetStyle(Style current, int presetId, boolean advancedAllowed) {
        Style style = current.applyFormat(ChatFormatting.WHITE);
        IStyle.TextEffectStyleData data = new IStyle.TextEffectStyleData(
                presetId, false, false, false, false, false, advancedAllowed
        );
        ((IStyle) style).textstudio_font$setStyleData(data);
        return style;
    }

    private static Style applyCustomStyle(Style current, AuthorConfig.EffectSettings settings, boolean advancedAllowed) {
        Style style = current.applyFormat(ChatFormatting.WHITE);
        IStyle.TextEffectStyleData data = new IStyle.TextEffectStyleData(
                1, false, false, false, false, false, advancedAllowed
        );
        data.customConfig = advancedAllowed ? settings : AuthorConfig.toPublicSafeSettings(settings);
        ((IStyle) style).textstudio_font$setStyleData(data);
        return style;
    }

    private static boolean advancedAllowed(Style style) {
        if (style instanceof IStyle effectStyle) {
            IStyle.TextEffectStyleData data = effectStyle.textstudio_font$getStyleData();
            if (data != null) {
                return data.advancedAllowed;
            }
        }
        return true;
    }
}
