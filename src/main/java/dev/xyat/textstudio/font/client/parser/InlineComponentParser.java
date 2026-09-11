package dev.xyat.textstudio.font.client.parser;

import dev.xyat.textstudio.font.api.IStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class InlineComponentParser {
    private InlineComponentParser() {
    }

    public static Component parse(Component component) {
        if (component == null || !TextProcessor.hasMarkers(component.getString())) {
            return component;
        }

        MutableComponent output = Component.empty();
        boolean[] changed = {false};

        component.visit((style, contents) -> {
            if (contents == null || contents.isEmpty()) {
                return Optional.empty();
            }

            if (!TextProcessor.hasMarkers(contents)) {
                output.append(Component.literal(contents).setStyle(style));
                return Optional.empty();
            }

            SegmentCollector collector = new SegmentCollector();
            TextProcessor.iterateFormatted(contents, 0, style, style, collector);
            if (collector.hasTextEffect()) {
                collector.appendTo(output);
                changed[0] = true;
            } else {
                output.append(Component.literal(contents).setStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);

        return changed[0] ? output : component;
    }

    private static final class SegmentCollector implements FormattedCharSink {
        private final List<Entry> entries = new ArrayList<>();
        private boolean hasTextEffect;

        @Override
        public boolean accept(int index, Style style, int codePoint) {
            entries.add(new Entry(style, codePoint));
            if (style instanceof IStyle effectStyle && effectStyle.textstudio_font$getStyleData() != null) {
                hasTextEffect = true;
            }
            return true;
        }

        private boolean hasTextEffect() {
            return hasTextEffect;
        }

        private void appendTo(MutableComponent target) {
            for (Entry entry : entries) {
                target.append(Component.literal(new String(Character.toChars(entry.codePoint()))).setStyle(entry.style()));
            }
        }
    }

    private record Entry(Style style, int codePoint) {
    }
}
