package dev.xyat.textstudio.font.client.parser;

import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringDecomposer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Keeps an effect on text that vanilla cuts into lines. An effect code sits once, in front of its text; a line cut
 * from the middle (a wrapped book page, the second line of a sign or a multi-line field) holds no code of its own and
 * would lose the effect. {@link #styled} moves the codes into the styles of the characters they cover, so every line
 * cut from the text keeps them; {@link #lineText} puts the code in force at a cut in front of the piece, and
 * {@link #lineStyle} gives a piece drawn in a style of its own that style with the effect.
 */
public final class EffectSpans {
    private EffectSpans() {
    }

    /** Whether the text holds an effect code, without building its string. */
    public static boolean hasMarkers(FormattedText text) {
        return text != null && text.visit(part -> part.indexOf('\u2063') >= 0 ? Optional.of(Boolean.TRUE) : Optional.empty()).isPresent();
    }

    /**
     * The text with its effect codes taken into the styles of the characters they cover and left out; formatting codes
     * are applied as vanilla does. Text without codes comes back as it is.
     */
    public static FormattedText styled(FormattedText text) {
        if (!hasMarkers(text)) return text;
        StringBuilder raw = new StringBuilder();
        List<Style> styles = new ArrayList<>();
        StringDecomposer.iterateFormatted(text, Style.EMPTY, (index, style, codePoint) -> {
            int start = raw.length();
            raw.appendCodePoint(codePoint);
            for (int i = start; i < raw.length(); i++) styles.add(style);
            return true;
        });
        List<FormattedText> parts = new ArrayList<>();
        StringBuilder run = new StringBuilder();
        Style[] current = {null};
        TextProcessor.iterateStyled(raw.toString(), styles::get, (index, style, codePoint) -> {
            if (current[0] != null && style != current[0]) {
                parts.add(FormattedText.of(run.toString(), current[0]));
                run.setLength(0);
            }
            current[0] = style;
            run.appendCodePoint(codePoint);
            return true;
        });
        if (current[0] != null) parts.add(FormattedText.of(run.toString(), current[0]));
        return parts.isEmpty() ? FormattedText.EMPTY : FormattedText.composite(parts);
    }

    /**
     * The style a line of {@code text} starting at {@code begin} is drawn in: {@code style}, with the effect in force
     * there, if any. Book editors draw each line as a piece of the page in the style the splitter hands it.
     */
    public static Style lineStyle(String text, int begin, Style style) {
        if (begin <= 0 || text.indexOf('\u2063') < 0) return style;
        String code = codeAt(text, begin);
        return code == null ? style : TextProcessor.withEffectCode(style, code);
    }

    /** {@code value.substring(begin, end)}, led by the effect code in force at {@code begin}, if any. */
    public static String lineText(String value, int begin, int end) {
        String piece = value.substring(begin, end);
        if (begin <= 0 || value.indexOf('\u2063') < 0) return piece;
        String code = codeAt(value, begin);
        return code == null ? piece : code + piece;
    }

    /** The effect code in force at {@code index}: the last one before it, unless a stop code came after. */
    static String codeAt(String value, int index) {
        String code = null;
        int i = value.indexOf('\u2063');
        while (i >= 0 && i < index) {
            if (CompactTagCodec.isResetAt(value, i)) {
                code = null;
                i += CompactTagCodec.resetLength();
            } else {
                CompactTagCodec.PresetToken preset = CompactTagCodec.readPreset(value, i);
                CompactTagCodec.CustomToken custom = preset == null ? CompactTagCodec.readCustom(value, i) : null;
                int end = preset != null ? preset.endIndex() : custom != null ? custom.endIndex() : i + 1;
                // A code the cut falls into is not in force yet.
                if ((preset != null || custom != null) && end <= index) code = value.substring(i, end);
                i = end;
            }
            i = value.indexOf('\u2063', i);
        }
        return code;
    }
}
