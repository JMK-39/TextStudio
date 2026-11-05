package dev.xyat.textstudiovalidation;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.xyat.kineticcore.api.command.CommandText;
import dev.xyat.textstudio.font.api.AuthorAPI;
import dev.xyat.textstudio.font.api.IStyle;
import dev.xyat.textstudio.font.client.parser.CompactTagCodec;
import dev.xyat.textstudio.font.client.render.CompatibleFontRenderer;
import dev.xyat.textstudio.font.config.AuthorConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/** Runs only in a loaded client, against Minecraft's Mixin-backed Style and the renderer's actual glyph buffer. */
public final class FontParserRuntimeChecks {
    private FontParserRuntimeChecks() {}

    public record Result(int checks, int failures, String details) {}

    public static Result run() {
        Check[] checks = {
                new Check("effect survives native style runs", FontParserRuntimeChecks::effectAcrossRuns),
                new Check("reset clears effect across native style runs", FontParserRuntimeChecks::resetAcrossRuns),
                new Check("split marker is recognized across native style runs", FontParserRuntimeChecks::splitMarker),
                new Check("native color bold font click hover insertion are preserved", FontParserRuntimeChecks::nativeStyles),
                new Check("source styles and shared EMPTY remain unchanged", FontParserRuntimeChecks::sourceIsolation),
                new Check("reset-only marker clears existing native effect without fallback", FontParserRuntimeChecks::resetExistingNativeEffect),
                new Check("native restrictions remain active beneath a custom prefix", FontParserRuntimeChecks::nativeRestrictions)
        };
        StringBuilder details = new StringBuilder("TEXTSTUDIO_PARSER_RUNTIME\n");
        Parser parser;
        try {
            effectData(Style.EMPTY);
            parser = new Parser();
        } catch (Throwable error) {
            details.append("FAIL runtime parser setup\n").append(stackTrace(error));
            return new Result(checks.length, checks.length, details.toString());
        }
        int failures = 0;
        for (Check check : checks) {
            try {
                check.body.run(parser);
                details.append("PASS ").append(check.name).append('\n');
            } catch (Throwable error) {
                failures++;
                details.append("FAIL ").append(check.name).append('\n').append(stackTrace(error));
            }
        }
        details.append("PARSER_").append(failures == 0 ? "PASS" : "FAIL")
                .append(" checks=").append(checks.length).append(" failures=").append(failures).append('\n');
        return new Result(checks.length, failures, details.toString());
    }

    /** Optional integration boundary: no compile-time dependency on AdventureSystems or a copied marker protocol. */
    public static Result runOptionalTipLimits() {
        StringBuilder details = new StringBuilder("TEXTSTUDIO_TIP_LIMITS_RUNTIME\n");
        Method isValid;
        try {
            Class<?> limits = Class.forName("dev.xyat.adventuresystems.tips.config.TipTextLimits");
            isValid = limits.getMethod("isValid", String.class);
        } catch (ClassNotFoundException absent) {
            return new Result(0, 0, details.append("SKIP AdventureSystems TipTextLimits is absent\n").toString());
        } catch (Throwable error) {
            return new Result(5, 5, details.append("FAIL TipTextLimits setup\n").append(stackTrace(error)).toString());
        }
        String sixteen = "\u4E2D".repeat(16);
        TipCheck[] checks = {
                new TipCheck("valid compact preset with 16 Chinese code points", CompactTagCodec.encodePreset(1, sixteen), true),
                new TipCheck("compact preset with 17 Chinese code points", CompactTagCodec.encodePreset(1, sixteen + "\u4E2D"), false),
                new TipCheck("compact preset and legacy color with 16 Chinese code points", CompactTagCodec.encodePreset(1, "\u00A7b" + sixteen), true),
                new TipCheck("damaged compact preset is counted literally", damagedPrefix(CompactTagCodec.encodePresetPrefix(1)) + sixteen, false),
                new TipCheck("ordinary variation selector remains a counted code point", "\u2665\uFE0F" + "\u4E2D".repeat(15), false)
        };
        int failures = 0;
        for (TipCheck check : checks) {
            try {
                Object actual = isValid.invoke(null, check.input);
                require(Boolean.valueOf(check.expected).equals(actual), "TipTextLimits.isValid: expected=" + check.expected
                        + " actual=" + actual + " input=" + printable(check.input));
                details.append("PASS ").append(check.name).append('\n');
            } catch (Throwable error) {
                failures++;
                details.append("FAIL ").append(check.name).append('\n').append(stackTrace(error));
            }
        }
        details.append("TIP_LIMITS_").append(failures == 0 ? "PASS" : "FAIL")
                .append(" checks=").append(checks.length).append(" failures=").append(failures).append('\n');
        return new Result(checks.length, failures, details.toString());
    }

    private static String damagedPrefix(String encoded) {
        char[] characters = encoded.toCharArray();
        // Flip a nibble in the real encoded token without duplicating token layout or checksum calculation.
        for (int i = characters.length - 1; i >= 0; i--) {
            if (characters[i] >= '\uFE00' && characters[i] <= '\uFE0F') {
                characters[i] = (char) (characters[i] ^ 1);
                return new String(characters);
            }
        }
        throw new AssertionError("The actual compact encoder did not emit a variation-selector nibble");
    }

    // Restarting the parser for each native run loses the effect on B and C.
    private static void effectAcrossRuns(Parser parser) throws Exception {
        Component input = Component.empty()
                .append(Component.literal(CompactTagCodec.encodePresetPrefix(1) + "A").withStyle(ChatFormatting.RED))
                .append(Component.literal("B").withStyle(ChatFormatting.BOLD, ChatFormatting.BLUE))
                .append(Component.literal("C").withStyle(ChatFormatting.ITALIC, ChatFormatting.GREEN));
        Glyphs glyphs = parser.parse(input);
        assertText(glyphs, "ABC");
        assertEffect(glyphs, 0, 1);
        assertEffect(glyphs, 1, 1);
        assertEffect(glyphs, 2, 1);
    }

    // The compact reset must clear the persistent effect, including following native runs.
    private static void resetAcrossRuns(Parser parser) throws Exception {
        Component input = Component.empty()
                .append(Component.literal(CompactTagCodec.encodePresetPrefix(1) + "A").withStyle(ChatFormatting.RED))
                .append(Component.literal("B" + CompactTagCodec.encodeReset() + "C").withStyle(ChatFormatting.BOLD))
                .append(Component.literal("D").withStyle(ChatFormatting.AQUA));
        Glyphs glyphs = parser.parse(input);
        assertText(glyphs, "ABCD");
        assertEffect(glyphs, 0, 1);
        assertEffect(glyphs, 1, 1);
        assertEffect(glyphs, 2, null);
        assertEffect(glyphs, 3, null);
    }

    // Parsing each run independently exposes pieces of a control token instead of recognizing it.
    private static void splitMarker(Parser parser) throws Exception {
        String prefix = CompactTagCodec.encodePresetPrefix(1);
        String reset = CompactTagCodec.encodeReset();
        Component input = Component.empty()
                .append(Component.literal(prefix.substring(0, 3)).withStyle(ChatFormatting.RED))
                .append(Component.literal(prefix.substring(3) + "A" + reset.substring(0, 2)).withStyle(ChatFormatting.BOLD))
                .append(Component.literal(reset.substring(2) + "B").withStyle(ChatFormatting.AQUA));
        Glyphs glyphs = parser.parse(input);
        assertText(glyphs, "AB");
        assertEffect(glyphs, 0, 1);
        assertEffect(glyphs, 1, null);
    }

    // Flattening the sequence or applying a white legacy format destroys native styling and event metadata.
    private static void nativeStyles(Parser parser) throws Exception {
        Style first = nativeStyle(0x336699, true, "first insertion");
        Style second = nativeStyle(0xCC8844, false, "second insertion");
        Glyphs glyphs = parser.parse(Component.empty()
                .append(Component.literal(CompactTagCodec.encodePresetPrefix(1) + "A").withStyle(first))
                .append(Component.literal("B" + CompactTagCodec.encodeReset() + "C").withStyle(second)));
        assertText(glyphs, "ABC");
        assertNativeStyle(first, glyphs.styles[0], "A");
        assertNativeStyle(second, glyphs.styles[1], "B");
        assertNativeStyle(second, glyphs.styles[2], "C after reset");
        assertEffect(glyphs, 0, 1);
        assertEffect(glyphs, 1, 1);
        assertEffect(glyphs, 2, null);
    }

    // Attaching effect data to an original Style (especially an already-white style) leaks into later text.
    private static void sourceIsolation(Parser parser) throws Exception {
        Style first = nativeStyle(0xFFFFFF, true, "untouched insertion");
        Style second = Style.EMPTY.withColor(ChatFormatting.WHITE);
        assertNoEffect(first, "original rich style before parsing");
        assertNoEffect(second, "original white style before parsing");
        assertNoEffect(Style.EMPTY, "shared EMPTY before parsing");
        Style firstSnapshot = nativeStyle(0xFFFFFF, true, "untouched insertion");
        Style secondSnapshot = Style.EMPTY.withColor(ChatFormatting.WHITE);
        parser.parse(Component.empty()
                .append(Component.literal(CompactTagCodec.encodePresetPrefix(1) + "A").withStyle(first))
                .append(Component.literal("B" + CompactTagCodec.encodeReset() + "C").withStyle(second))
                .append(Component.literal("D").withStyle(Style.EMPTY)));
        parser.parse(Component.literal(CompactTagCodec.encodePresetPrefix(1) + "E" + CompactTagCodec.encodeReset() + "F"));
        assertNativeStyle(firstSnapshot, first, "original rich source");
        assertNativeStyle(secondSnapshot, second, "original white source");
        assertNoEffect(first, "original rich source");
        assertNoEffect(second, "original white source");
        assertNoEffect(Style.EMPTY, "shared EMPTY after parsing");
        assertNativeStyle(Style.EMPTY, Component.literal("plain").getStyle(), "new plain component");
    }

    // Reporting false after reset-only parsing makes rendering fall back to the original effect-bearing sequence.
    private static void resetExistingNativeEffect(Parser parser) throws Exception {
        Style source = Style.EMPTY.withColor(0xFFFFFF);
        require(source != Style.EMPTY, "The existing-effect fixture must have its own source Style");
        IStyle.TextEffectStyleData originalData = new IStyle.TextEffectStyleData(1, false, false, false, false, false);
        ((IStyle) (Object) source).textstudio_font$setStyleData(originalData);
        Glyphs glyphs = parser.parse(Component.literal(CompactTagCodec.encodeReset() + "AB").setStyle(source));
        assertText(glyphs, "AB");
        assertEffect(glyphs, 0, null);
        assertEffect(glyphs, 1, null);
        require(effectData(source) == originalData, "Reset changed the original source effect-data reference");
        require(originalData.effectId == 1, "Reset changed the original source effect ID");
        assertNoEffect(Style.EMPTY, "shared EMPTY after resetting an existing native effect");
    }

    private static Style nativeStyle(int color, boolean bold, String insertion) {
        // Vanilla's codec supplies the correct font representation on every supported Minecraft version.
        //? if >=1.21 {
        /*
        Style font = Style.Serializer.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"font\":\"minecraft:uniform\"}"))
                .result().orElseThrow(() -> new AssertionError("Vanilla failed to decode the test font"));
        *///?} else {
        Style font = Style.EMPTY.withFont(new net.minecraft.resources.ResourceLocation("minecraft:uniform"));
        //?}
        return CommandText.clickToSuggest(Component.literal("events"), "/kt font", Component.literal("parser hover"))
                .getStyle().withColor(color).withBold(bold).withFont(font.getFont()).withInsertion(insertion);
    }

    private static void nativeRestrictions(Parser parser) throws Exception {
        AuthorConfig.EffectSettings settings = new AuthorConfig.EffectSettings();
        settings.useRainbow = true;
        settings.wave = true;
        Component restricted = AuthorAPI.createStyledName("B", 1, 0, false);
        IStyle.TextEffectStyleData originalData = effectData(restricted.getStyle());
        Glyphs glyphs = parser.parse(Component.literal(CompactTagCodec.encodeCustomPrefix(settings) + "A")
                .append(restricted).append(Component.literal("C")));
        assertText(glyphs, "ABC");
        IStyle.TextEffectStyleData first = effectData(glyphs.styles[0]);
        IStyle.TextEffectStyleData bounded = effectData(glyphs.styles[1]);
        IStyle.TextEffectStyleData last = effectData(glyphs.styles[2]);
        require(first != null && first.advancedAllowed && first.customConfig.wave,
                "The unrestricted prefix lost its wave effect");
        require(bounded != null && !bounded.advancedAllowed && bounded.customConfig != null
                        && !bounded.customConfig.wave && bounded.customConfig.useRainbow,
                "The custom prefix bypassed the native run's restricted effect policy");
        require(last != null && last.advancedAllowed && last.customConfig.wave,
                "The restricted run mutated the persistent prefix state");
        require(effectData(restricted.getStyle()) == originalData && !originalData.advancedAllowed,
                "The restricted native source effect data was changed");
    }

    private static void assertText(Glyphs glyphs, String expected) {
        String actual = new String(glyphs.codePoints, 0, glyphs.codePoints.length);
        require(expected.equals(actual), "visible glyphs: expected=" + expected + " actual=" + printable(actual));
        require(glyphs.parsed, "parser did not report the recognized control marker");
    }

    private static void assertEffect(Glyphs glyphs, int index, Integer expectedId) {
        IStyle.TextEffectStyleData data = effectData(glyphs.styles[index]);
        Integer actualId = data == null ? null : data.effectId;
        require(Objects.equals(expectedId, actualId), "glyph " + index + " effect: expected=" + expectedId + " actual=" + actualId);
    }

    private static void assertNoEffect(Style style, String context) {
        require(effectData(style) == null, context + " was polluted with effect data");
    }

    private static IStyle.TextEffectStyleData effectData(Style style) {
        require((Object) style instanceof IStyle, "Style Mixin is not installed: " + style.getClass().getName());
        return ((IStyle) (Object) style).textstudio_font$getStyleData();
    }

    private static void assertNativeStyle(Style expected, Style actual, String context) {
        require(Objects.equals(expected.getColor(), actual.getColor()), context + ": native color changed");
        require(expected.isBold() == actual.isBold(), context + ": native bold changed");
        require(expected.isItalic() == actual.isItalic(), context + ": native italic changed");
        require(expected.isUnderlined() == actual.isUnderlined(), context + ": native underline changed");
        require(expected.isStrikethrough() == actual.isStrikethrough(), context + ": native strike changed");
        require(expected.isObfuscated() == actual.isObfuscated(), context + ": native obfuscation changed");
        require(Objects.equals(expected.getFont(), actual.getFont()), context + ": native font changed");
        require(Objects.equals(expected.getClickEvent(), actual.getClickEvent()), context + ": native click event changed");
        require(Objects.equals(expected.getHoverEvent(), actual.getHoverEvent()), context + ": native hover event changed");
        require(Objects.equals(expected.getInsertion(), actual.getInsertion()), context + ": native insertion changed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static String printable(String text) {
        StringBuilder output = new StringBuilder();
        text.codePoints().forEach(cp -> {
            if (cp >= 0x2061 && cp <= 0x2064 || cp >= 0xFE00 && cp <= 0xFE0F) output.append(String.format("\\u%04X", cp));
            else output.appendCodePoint(cp);
        });
        return output.toString();
    }

    private static String stackTrace(Throwable error) {
        while (error instanceof InvocationTargetException invocation && invocation.getCause() != null) error = invocation.getCause();
        StringWriter output = new StringWriter();
        error.printStackTrace(new PrintWriter(output));
        return output.toString();
    }

    private record Check(String name, CheckedBody body) {}
    private record TipCheck(String name, String input, boolean expected) {}
    @FunctionalInterface private interface CheckedBody { void run(Parser parser) throws Exception; }
    private record Glyphs(boolean parsed, Style[] styles, int[] codePoints) {}

    private static final class Parser {
        private final Constructor<?> contextConstructor;
        private final Method parse;
        private final Field contextBuffer, bufferStyles, bufferCodePoints, bufferSize;

        private Parser() throws ReflectiveOperationException {
            Class<?> contextType = Class.forName(CompatibleFontRenderer.class.getName() + "$RenderContext");
            contextConstructor = contextType.getDeclaredConstructor();
            contextConstructor.setAccessible(true);
            parse = CompatibleFontRenderer.class.getDeclaredMethod("parseRawSequence", FormattedCharSequence.class, contextType);
            parse.setAccessible(true);
            contextBuffer = field(contextType, "buffer");
            Class<?> bufferType = contextBuffer.getType();
            bufferStyles = field(bufferType, "styles");
            bufferCodePoints = field(bufferType, "codePoints");
            bufferSize = field(bufferType, "size");
        }

        private Glyphs parse(Component component) throws ReflectiveOperationException {
            Object context = contextConstructor.newInstance();
            boolean recognized = (boolean) parse.invoke(null, component.getVisualOrderText(), context);
            Object buffer = contextBuffer.get(context);
            int size = bufferSize.getInt(buffer);
            return new Glyphs(recognized, Arrays.copyOf((Style[]) bufferStyles.get(buffer), size),
                    Arrays.copyOf((int[]) bufferCodePoints.get(buffer), size));
        }

        private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        }
    }
}
