package dev.xyat.textstudio.font.client.parser;

import dev.xyat.textstudio.font.config.AuthorConfig;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class CompactTagCodec {
    private static final String PRESET_START = "\u2063\u2061";
    private static final String CUSTOM_START = "\u2063\u2062";
    private static final String META_END = "\u2061\u2063";
    private static final String RESET = "\u2063\u2064\uFE0B\uFE05\u2064\u2063";
    private static final char NIBBLE_BASE = '\uFE00';
    private static final int MARKER_CACHE_SIZE = 1024;
    private static final int CUSTOM_CACHE_SIZE = 256;

    private static final ThreadLocal<MarkerCache> MARKER_CACHE = ThreadLocal.withInitial(MarkerCache::new);
    private static final ThreadLocal<CustomCache> CUSTOM_CACHE = ThreadLocal.withInitial(CustomCache::new);

    private CompactTagCodec() {
    }

    public static boolean hasMarker(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        MarkerCache cache = MARKER_CACHE.get();
        int hash = text.hashCode();
        int slot = hash & (MARKER_CACHE_SIZE - 1);
        String cached = cache.keys[slot];
        if (cached != null && cache.hashes[slot] == hash && cached.equals(text)) {
            return cache.values[slot];
        }
        boolean value = text.indexOf('\u2063') >= 0;
        cache.keys[slot] = text;
        cache.hashes[slot] = hash;
        cache.values[slot] = value;
        return value;
    }

    public static boolean isResetAt(String text, int index) {
        return text != null && text.startsWith(RESET, index);
    }

    public static int resetLength() {
        return RESET.length();
    }

    public static String encodePresetPrefix(int presetId) {
        int id = Math.max(1, Math.min(0xFFFF, presetId));
        int checksum = presetChecksum(id);
        StringBuilder out = new StringBuilder(PRESET_START.length() + 8 + META_END.length());
        out.append(PRESET_START);
        appendNibble(out, id >>> 12);
        appendNibble(out, id >>> 8);
        appendNibble(out, id >>> 4);
        appendNibble(out, id);
        appendNibble(out, checksum >>> 4);
        appendNibble(out, checksum);
        out.append(META_END);
        return out.toString();
    }

    public static String encodeCustomPrefix(AuthorConfig.EffectSettings settings) {
        String payload = serializePayload(settings);
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        int checksum = payloadChecksum(bytes);
        StringBuilder out = new StringBuilder(CUSTOM_START.length() + bytes.length * 2 + 10);
        out.append(CUSTOM_START);
        for (byte value : bytes) {
            int b = value & 0xFF;
            appendNibble(out, b >>> 4);
            appendNibble(out, b);
        }
        appendNibble(out, checksum >>> 4);
        appendNibble(out, checksum);
        out.append(META_END);
        return out.toString();
    }

    public static String encodeReset() {
        return RESET;
    }

    public static String encodePreset(int presetId, String text) {
        String visibleText = text == null ? "" : text;
        return encodePresetPrefix(presetId) + visibleText + RESET;
    }

    public static String encodeCustom(AuthorConfig.EffectSettings settings, String text) {
        String visibleText = text == null ? "" : text;
        return encodeCustomPrefix(settings) + visibleText + RESET;
    }

    public static boolean equivalent(AuthorConfig.EffectSettings a, AuthorConfig.EffectSettings b) {
        if (a == null || b == null) {
            return a == b;
        }
        return serializePayload(a).equals(serializePayload(b));
    }

    public static PresetToken readPreset(String text, int index) {
        if (text == null || !text.startsWith(PRESET_START, index)) {
            return null;
        }
        int pos = index + PRESET_START.length();
        if (pos + 6 + META_END.length() > text.length()) {
            return null;
        }
        int id = 0;
        for (int i = 0; i < 4; i++) {
            int nibble = decodeNibble(text.charAt(pos + i));
            if (nibble < 0) {
                return null;
            }
            id = id << 4 | nibble;
        }
        int high = decodeNibble(text.charAt(pos + 4));
        int low = decodeNibble(text.charAt(pos + 5));
        if (high < 0 || low < 0 || ((high << 4) | low) != presetChecksum(id)) {
            return null;
        }
        pos += 6;
        if (!text.startsWith(META_END, pos)) {
            return null;
        }
        if (id < 1 || id > AuthorConfig.EFFECTS.size()) {
            return null;
        }
        return new PresetToken(id, pos + META_END.length());
    }

    public static CustomToken readCustom(String text, int index) {
        if (text == null || !text.startsWith(CUSTOM_START, index)) {
            return null;
        }
        int pos = index + CUSTOM_START.length();
        int end = text.indexOf(META_END, pos);
        if (end < 0 || end - pos < 2 || ((end - pos) & 1) != 0) {
            return null;
        }
        int encodedPayloadLength = end - pos - 2;
        if ((encodedPayloadLength & 1) != 0) {
            return null;
        }
        int checksumHigh = decodeNibble(text.charAt(end - 2));
        int checksumLow = decodeNibble(text.charAt(end - 1));
        if (checksumHigh < 0 || checksumLow < 0) {
            return null;
        }
        byte[] bytes = new byte[encodedPayloadLength / 2];
        for (int i = 0; i < bytes.length; i++) {
            int high = decodeNibble(text.charAt(pos + i * 2));
            int low = decodeNibble(text.charAt(pos + i * 2 + 1));
            if (high < 0 || low < 0) {
                return null;
            }
            bytes[i] = (byte) ((high << 4) | low);
        }
        int expected = (checksumHigh << 4) | checksumLow;
        if (payloadChecksum(bytes) != expected) {
            return null;
        }
        String payload = new String(bytes, StandardCharsets.UTF_8);
        AuthorConfig.EffectSettings settings = cachedPayload(payload);
        return new CustomToken(settings, end + META_END.length());
    }

    private static AuthorConfig.EffectSettings cachedPayload(String payload) {
        CustomCache cache = CUSTOM_CACHE.get();
        int hash = payload.hashCode();
        int slot = hash & (CUSTOM_CACHE_SIZE - 1);
        String key = cache.keys[slot];
        if (key != null && cache.hashes[slot] == hash && key.equals(payload)) {
            return cache.values[slot];
        }
        AuthorConfig.EffectSettings value = parsePayload(payload);
        cache.keys[slot] = payload;
        cache.hashes[slot] = hash;
        cache.values[slot] = value;
        return value;
    }

    private static int presetChecksum(int id) {
        int value = 0xA7;
        value = (value * 33 + (id & 0xFF)) & 0xFF;
        value = (value * 33 + ((id >>> 8) & 0xFF)) & 0xFF;
        return value;
    }

    private static int payloadChecksum(byte[] bytes) {
        int value = 0x5D;
        for (byte b : bytes) {
            value = (value * 33 + (b & 0xFF)) & 0xFF;
        }
        return value;
    }

    private static String serializePayload(AuthorConfig.EffectSettings s) {
        AuthorConfig.EffectSettings d = new AuthorConfig.EffectSettings();
        List<String> p = new ArrayList<>();

        flag(p, 'a', s.useRainbow); num(p, 'b', s.rainbowSpeed, d.rainbowSpeed); num(p, 'c', s.rainbowSpread, d.rainbowSpread);
        flag(p, 'd', s.pulse); num(p, 'e', s.pulseBase, d.pulseBase); num(p, 'f', s.pulseAmp, d.pulseAmp); num(p, 'g', s.pulseSpeed, d.pulseSpeed);
        flag(p, 'h', s.fade); num(p, 'i', s.fadeMin, d.fadeMin); num(p, 'j', s.fadeSpeed, d.fadeSpeed);
        flag(p, 'k', s.neonFlicker); num(p, 'l', s.neonFlickerSpeed, d.neonFlickerSpeed);
        flag(p, 'm', s.chromatic); num(p, 'n', s.chromaticOffset, d.chromaticOffset); num(p, 'o', s.chromaticAlpha, d.chromaticAlpha); flag(p, 'p', s.chromaticPulse);
        flag(p, 'q', s.wave); num(p, 'r', s.waveAmp, d.waveAmp); num(p, 's', s.waveSpeed, d.waveSpeed);
        flag(p, 't', s.bounce); num(p, 'u', s.bounceAmp, d.bounceAmp); num(p, 'v', s.bounceSpeed, d.bounceSpeed);
        flag(p, 'w', s.shake); num(p, 'x', s.shakeAmp, d.shakeAmp); num(p, 'y', s.shakeSpeed, d.shakeSpeed);
        flag(p, 'z', s.swing); num(p, 'A', s.swingAmp, d.swingAmp); num(p, 'B', s.swingSpeed, d.swingSpeed);
        flag(p, 'C', s.wiggle); num(p, 'D', s.wiggleAmp, d.wiggleAmp); num(p, 'E', s.wiggleSpeed, d.wiggleSpeed);
        flag(p, 'F', s.turb); num(p, 'G', s.turbAmp, d.turbAmp); num(p, 'H', s.turbSpeed, d.turbSpeed);
        flag(p, 'I', s.pend); num(p, 'J', s.pendAmp, d.pendAmp); num(p, 'K', s.pendSpeed, d.pendSpeed);
        flag(p, 'L', s.glitch); num(p, 'M', s.glitchAmp, d.glitchAmp); num(p, 'N', s.glitchSpeed, d.glitchSpeed); num(p, 'O', s.glitchFlicker, d.glitchFlicker);
        flag(p, 'P', s.spasm); num(p, 'Q', s.spasmAmp, d.spasmAmp); num(p, 'R', s.spasmSpeed, d.spasmSpeed);
        flag(p, 'S', s.typewriter); num(p, 'T', s.typewriterSpeed, d.typewriterSpeed); flag(p, 'U', s.typewriterBack);
        flag(p, 'V', s.palette);
        if (s.paletteColors != null && !s.paletteColors.isBlank()) p.add("W" + s.paletteColors);
        num(p, 'X', s.paletteSpeed, d.paletteSpeed); num(p, 'Y', s.paletteSpread, d.paletteSpread); flag(p, 'Z', s.paletteFlash);
        flag(p, '0', s.shimmer); num(p, '1', s.shimmerSpeed, d.shimmerSpeed); num(p, '2', s.shimmerWidth, d.shimmerWidth); num(p, '3', s.shimmerStrength, d.shimmerStrength);
        flag(p, '4', s.sparkle); num(p, '5', s.sparkleSpeed, d.sparkleSpeed); num(p, '6', s.sparkleChance, d.sparkleChance); num(p, '7', s.sparkleStrength, d.sparkleStrength);
        flag(p, '8', s.orbit); num(p, '9', s.orbitAmp, d.orbitAmp); num(p, '!', s.orbitSpeed, d.orbitSpeed);
        flag(p, '@', s.ripple); num(p, '#', s.rippleAmp, d.rippleAmp); num(p, '$', s.rippleSpeed, d.rippleSpeed);
        flag(p, '%', s.drift); num(p, '^', s.driftAmp, d.driftAmp); num(p, '&', s.driftSpeed, d.driftSpeed);
        flag(p, '*', s.blinkWave); num(p, '(', s.blinkWaveSpeed, d.blinkWaveSpeed); num(p, ')', s.blinkWaveDepth, d.blinkWaveDepth);
        flag(p, '_', s.noteBounce); num(p, '-', s.noteBounceAmp, d.noteBounceAmp); num(p, '+', s.noteBounceSpeed, d.noteBounceSpeed);
        flag(p, ']', s.glyphScale); num(p, '{', s.glyphScaleAmp, d.glyphScaleAmp); num(p, '}', s.glyphScaleSpeed, d.glyphScaleSpeed);
        flag(p, ';', s.ecg); num(p, ':', s.ecgAmp, d.ecgAmp); num(p, '\'', s.ecgSpeed, d.ecgSpeed); num(p, '"', s.ecgWidth, d.ecgWidth);
        flag(p, '<', s.heartbeat); num(p, '>', s.heartbeatAmp, d.heartbeatAmp); num(p, '?', s.heartbeatSpeed, d.heartbeatSpeed);
        flag(p, '/', s.glitchSpike); num(p, '\\', s.glitchSpikeAmp, d.glitchSpikeAmp); num(p, '|', s.glitchSpikeSpeed, d.glitchSpikeSpeed); num(p, '~', s.glitchSpikeChance, d.glitchSpikeChance);
        flag(p, '`', s.signalLoss); num(p, '¡', s.signalLossChance, d.signalLossChance); num(p, '¢', s.signalLossSpeed, d.signalLossSpeed);
        flag(p, '¤', s.sweep); num(p, '¥', s.sweepSpeed, d.sweepSpeed); num(p, '¦', s.sweepWidth, d.sweepWidth); num(p, '©', s.sweepStrength, d.sweepStrength);
        flag(p, '®', s.outline); num(p, '°', s.outlineWidth, d.outlineWidth); num(p, '±', s.outlineAlpha, d.outlineAlpha);
        flag(p, '²', s.glow); num(p, '³', s.glowRadius, d.glowRadius); num(p, '´', s.glowAlpha, d.glowAlpha);
        flag(p, 'µ', s.trail); num(p, '¶', s.trailStrength, d.trailStrength); num(p, '·', s.trailAlpha, d.trailAlpha);
        flag(p, '¸', s.extrude); num(p, '¹', s.extrudeDepth, d.extrudeDepth); num(p, 'º', s.extrudeAlpha, d.extrudeAlpha);

        return String.join(",", p);
    }

    private static AuthorConfig.EffectSettings parsePayload(String payload) {
        AuthorConfig.EffectSettings s = new AuthorConfig.EffectSettings();
        if (payload == null || payload.isBlank()) {
            return s;
        }
        String[] parts = payload.split(",");
        for (String part : parts) {
            if (part == null || part.isEmpty()) continue;
            char key = part.charAt(0);
            String value = part.length() > 1 ? part.substring(1) : "";
            try {
                switch (key) {
                    case 'a' -> s.useRainbow = true;
                    case 'b' -> { s.rainbowSpeed = Double.parseDouble(value); s.useRainbow = true; }
                    case 'c' -> { s.rainbowSpread = Double.parseDouble(value); s.useRainbow = true; }
                    case 'd' -> s.pulse = true;
                    case 'e' -> { s.pulseBase = Double.parseDouble(value); s.pulse = true; }
                    case 'f' -> { s.pulseAmp = Double.parseDouble(value); s.pulse = true; }
                    case 'g' -> { s.pulseSpeed = Double.parseDouble(value); s.pulse = true; }
                    case 'h' -> s.fade = true;
                    case 'i' -> { s.fadeMin = Double.parseDouble(value); s.fade = true; }
                    case 'j' -> { s.fadeSpeed = Double.parseDouble(value); s.fade = true; }
                    case 'k' -> s.neonFlicker = true;
                    case 'l' -> { s.neonFlickerSpeed = Double.parseDouble(value); s.neonFlicker = true; }
                    case 'm' -> s.chromatic = true;
                    case 'n' -> { s.chromaticOffset = Double.parseDouble(value); s.chromatic = true; }
                    case 'o' -> { s.chromaticAlpha = Double.parseDouble(value); s.chromatic = true; }
                    case 'p' -> { s.chromaticPulse = true; s.chromatic = true; }
                    case 'q' -> s.wave = true;
                    case 'r' -> { s.waveAmp = Double.parseDouble(value); s.wave = true; }
                    case 's' -> { s.waveSpeed = Double.parseDouble(value); s.wave = true; }
                    case 't' -> s.bounce = true;
                    case 'u' -> { s.bounceAmp = Double.parseDouble(value); s.bounce = true; }
                    case 'v' -> { s.bounceSpeed = Double.parseDouble(value); s.bounce = true; }
                    case 'w' -> s.shake = true;
                    case 'x' -> { s.shakeAmp = Double.parseDouble(value); s.shake = true; }
                    case 'y' -> { s.shakeSpeed = Double.parseDouble(value); s.shake = true; }
                    case 'z' -> s.swing = true;
                    case 'A' -> { s.swingAmp = Double.parseDouble(value); s.swing = true; }
                    case 'B' -> { s.swingSpeed = Double.parseDouble(value); s.swing = true; }
                    case 'C' -> s.wiggle = true;
                    case 'D' -> { s.wiggleAmp = Double.parseDouble(value); s.wiggle = true; }
                    case 'E' -> { s.wiggleSpeed = Double.parseDouble(value); s.wiggle = true; }
                    case 'F' -> s.turb = true;
                    case 'G' -> { s.turbAmp = Double.parseDouble(value); s.turb = true; }
                    case 'H' -> { s.turbSpeed = Double.parseDouble(value); s.turb = true; }
                    case 'I' -> s.pend = true;
                    case 'J' -> { s.pendAmp = Double.parseDouble(value); s.pend = true; }
                    case 'K' -> { s.pendSpeed = Double.parseDouble(value); s.pend = true; }
                    case 'L' -> s.glitch = true;
                    case 'M' -> { s.glitchAmp = Double.parseDouble(value); s.glitch = true; }
                    case 'N' -> { s.glitchSpeed = Double.parseDouble(value); s.glitch = true; }
                    case 'O' -> { s.glitchFlicker = Double.parseDouble(value); s.glitch = true; }
                    case 'P' -> s.spasm = true;
                    case 'Q' -> { s.spasmAmp = Double.parseDouble(value); s.spasm = true; }
                    case 'R' -> { s.spasmSpeed = Double.parseDouble(value); s.spasm = true; }
                    case 'S' -> s.typewriter = true;
                    case 'T' -> { s.typewriterSpeed = Double.parseDouble(value); s.typewriter = true; }
                    case 'U' -> { s.typewriterBack = true; s.typewriter = true; }
                    case 'V' -> s.palette = true;
                    case 'W' -> { s.paletteColors = value; s.palette = true; }
                    case 'X' -> { s.paletteSpeed = Double.parseDouble(value); s.palette = true; }
                    case 'Y' -> { s.paletteSpread = Double.parseDouble(value); s.palette = true; }
                    case 'Z' -> { s.paletteFlash = true; s.palette = true; }
                    case '0' -> s.shimmer = true;
                    case '1' -> { s.shimmerSpeed = Double.parseDouble(value); s.shimmer = true; }
                    case '2' -> { s.shimmerWidth = Double.parseDouble(value); s.shimmer = true; }
                    case '3' -> { s.shimmerStrength = Double.parseDouble(value); s.shimmer = true; }
                    case '4' -> s.sparkle = true;
                    case '5' -> { s.sparkleSpeed = Double.parseDouble(value); s.sparkle = true; }
                    case '6' -> { s.sparkleChance = Double.parseDouble(value); s.sparkle = true; }
                    case '7' -> { s.sparkleStrength = Double.parseDouble(value); s.sparkle = true; }
                    case '8' -> s.orbit = true;
                    case '9' -> { s.orbitAmp = Double.parseDouble(value); s.orbit = true; }
                    case '!' -> { s.orbitSpeed = Double.parseDouble(value); s.orbit = true; }
                    case '@' -> s.ripple = true;
                    case '#' -> { s.rippleAmp = Double.parseDouble(value); s.ripple = true; }
                    case '$' -> { s.rippleSpeed = Double.parseDouble(value); s.ripple = true; }
                    case '%' -> s.drift = true;
                    case '^' -> { s.driftAmp = Double.parseDouble(value); s.drift = true; }
                    case '&' -> { s.driftSpeed = Double.parseDouble(value); s.drift = true; }
                    case '*' -> s.blinkWave = true;
                    case '(' -> { s.blinkWaveSpeed = Double.parseDouble(value); s.blinkWave = true; }
                    case ')' -> { s.blinkWaveDepth = Double.parseDouble(value); s.blinkWave = true; }
                    case '_' -> s.noteBounce = true;
                    case '-' -> { s.noteBounceAmp = Double.parseDouble(value); s.noteBounce = true; }
                    case '+' -> { s.noteBounceSpeed = Double.parseDouble(value); s.noteBounce = true; }
                    case ']' -> s.glyphScale = true;
                    case '{' -> { s.glyphScaleAmp = Double.parseDouble(value); s.glyphScale = true; }
                    case '}' -> { s.glyphScaleSpeed = Double.parseDouble(value); s.glyphScale = true; }
                    case ';' -> s.ecg = true;
                    case ':' -> { s.ecgAmp = Double.parseDouble(value); s.ecg = true; }
                    case '\'' -> { s.ecgSpeed = Double.parseDouble(value); s.ecg = true; }
                    case '"' -> { s.ecgWidth = Double.parseDouble(value); s.ecg = true; }
                    case '<' -> s.heartbeat = true;
                    case '>' -> { s.heartbeatAmp = Double.parseDouble(value); s.heartbeat = true; }
                    case '?' -> { s.heartbeatSpeed = Double.parseDouble(value); s.heartbeat = true; }
                    case '/' -> s.glitchSpike = true;
                    case '\\' -> { s.glitchSpikeAmp = Double.parseDouble(value); s.glitchSpike = true; }
                    case '|' -> { s.glitchSpikeSpeed = Double.parseDouble(value); s.glitchSpike = true; }
                    case '~' -> { s.glitchSpikeChance = Double.parseDouble(value); s.glitchSpike = true; }
                    case '`' -> s.signalLoss = true;
                    case '¡' -> { s.signalLossChance = Double.parseDouble(value); s.signalLoss = true; }
                    case '¢' -> { s.signalLossSpeed = Double.parseDouble(value); s.signalLoss = true; }
                    case '¤' -> s.sweep = true;
                    case '¥' -> { s.sweepSpeed = Double.parseDouble(value); s.sweep = true; }
                    case '¦' -> { s.sweepWidth = Double.parseDouble(value); s.sweep = true; }
                    case '©' -> { s.sweepStrength = Double.parseDouble(value); s.sweep = true; }
                    case '®' -> s.outline = true;
                    case '°' -> { s.outlineWidth = Double.parseDouble(value); s.outline = true; }
                    case '±' -> { s.outlineAlpha = Double.parseDouble(value); s.outline = true; }
                    case '²' -> s.glow = true;
                    case '³' -> { s.glowRadius = Double.parseDouble(value); s.glow = true; }
                    case '´' -> { s.glowAlpha = Double.parseDouble(value); s.glow = true; }
                    case 'µ' -> s.trail = true;
                    case '¶' -> { s.trailStrength = Double.parseDouble(value); s.trail = true; }
                    case '·' -> { s.trailAlpha = Double.parseDouble(value); s.trail = true; }
                    case '¸' -> s.extrude = true;
                    case '¹' -> { s.extrudeDepth = Double.parseDouble(value); s.extrude = true; }
                    case 'º' -> { s.extrudeAlpha = Double.parseDouble(value); s.extrude = true; }
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return s;
    }

    private static void flag(List<String> out, char key, boolean enabled) {
        if (enabled) out.add(String.valueOf(key));
    }

    private static void num(List<String> out, char key, double value, double defaultValue) {
        if (Double.compare(value, defaultValue) != 0) {
            out.add(key + format(value));
        }
    }

    private static String format(double value) {
        if (value == (long) value) {
            return Long.toString((long) value);
        }
        String s = Double.toString(value);
        if (s.startsWith("0.")) return s.substring(1);
        if (s.startsWith("-0.")) return "-" + s.substring(2);
        return s;
    }

    private static void appendNibble(StringBuilder out, int value) {
        out.append((char) (NIBBLE_BASE + (value & 0x0F)));
    }

    private static int decodeNibble(char c) {
        int value = c - NIBBLE_BASE;
        return value >= 0 && value <= 15 ? value : -1;
    }

    public record PresetToken(int presetId, int endIndex) {
    }

    public record CustomToken(AuthorConfig.EffectSettings settings, int endIndex) {
    }

    private static final class MarkerCache {
        private final String[] keys = new String[MARKER_CACHE_SIZE];
        private final int[] hashes = new int[MARKER_CACHE_SIZE];
        private final boolean[] values = new boolean[MARKER_CACHE_SIZE];
    }

    private static final class CustomCache {
        private final String[] keys = new String[CUSTOM_CACHE_SIZE];
        private final int[] hashes = new int[CUSTOM_CACHE_SIZE];
        private final AuthorConfig.EffectSettings[] values = new AuthorConfig.EffectSettings[CUSTOM_CACHE_SIZE];
    }
}
