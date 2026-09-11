package dev.xyat.textstudio.font.common.text;

public final class InputMetrics {
    private static final String PRESET_START = "\u2063\u2061";
    private static final String CUSTOM_START = "\u2063\u2062";
    private static final String META_END = "\u2061\u2063";
    private static final String RESET = "\u2063\u2064\uFE0B\uFE05\u2064\u2063";
    private static final char NIBBLE_BASE = '\uFE00';

    private InputMetrics() {
    }

    public static boolean hasControlCodes(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\u2063' && controlEnd(text, i) > i) {
                return true;
            }
        }
        return false;
    }

    public static int visibleCodePointLength(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int visible = 0;
        for (int i = 0; i < text.length();) {
            int controlEnd = controlEnd(text, i);
            if (controlEnd > i) {
                i = controlEnd;
                continue;
            }
            int codePoint = text.codePointAt(i);
            visible++;
            i += Character.charCount(codePoint);
        }
        return visible;
    }

    public static String stripControlCodes(String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }
        StringBuilder out = null;
        int copyFrom = 0;
        for (int i = 0; i < text.length();) {
            int controlEnd = controlEnd(text, i);
            if (controlEnd > i) {
                if (out == null) {
                    out = new StringBuilder(text.length());
                }
                if (copyFrom < i) {
                    out.append(text, copyFrom, i);
                }
                i = controlEnd;
                copyFrom = i;
                continue;
            }
            int codePoint = text.codePointAt(i);
            i += Character.charCount(codePoint);
        }
        if (out == null) {
            return text;
        }
        if (copyFrom < text.length()) {
            out.append(text, copyFrom, text.length());
        }
        return out.toString();
    }

    public static boolean fitsVisibleLimit(String text, int maxVisibleCodePoints) {
        return visibleCodePointLength(text) <= Math.max(0, maxVisibleCodePoints);
    }

    public static String truncateToVisibleLength(String text, int maxVisibleCodePoints) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }
        int limit = Math.max(0, maxVisibleCodePoints);
        int visible = 0;
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length();) {
            int controlEnd = controlEnd(text, i);
            if (controlEnd > i) {
                out.append(text, i, controlEnd);
                i = controlEnd;
                continue;
            }
            int codePoint = text.codePointAt(i);
            if (visible >= limit) {
                i += Character.charCount(codePoint);
                continue;
            }
            out.appendCodePoint(codePoint);
            visible++;
            i += Character.charCount(codePoint);
        }
        return out.toString();
    }

    private static int controlEnd(String text, int index) {
        if (index < 0 || index >= text.length() || text.charAt(index) != '\u2063') {
            return -1;
        }
        if (text.startsWith(RESET, index)) {
            return index + RESET.length();
        }
        int presetEnd = presetEnd(text, index);
        if (presetEnd > index) {
            return presetEnd;
        }
        return customEnd(text, index);
    }

    private static int presetEnd(String text, int index) {
        if (!text.startsWith(PRESET_START, index)) {
            return -1;
        }
        int pos = index + PRESET_START.length();
        if (pos + 6 + META_END.length() > text.length()) {
            return -1;
        }
        int id = 0;
        for (int i = 0; i < 4; i++) {
            int nibble = decodeNibble(text.charAt(pos + i));
            if (nibble < 0) {
                return -1;
            }
            id = id << 4 | nibble;
        }
        int high = decodeNibble(text.charAt(pos + 4));
        int low = decodeNibble(text.charAt(pos + 5));
        if (high < 0 || low < 0 || ((high << 4) | low) != presetChecksum(id)) {
            return -1;
        }
        pos += 6;
        return text.startsWith(META_END, pos) ? pos + META_END.length() : -1;
    }

    private static int customEnd(String text, int index) {
        if (!text.startsWith(CUSTOM_START, index)) {
            return -1;
        }
        int pos = index + CUSTOM_START.length();
        int end = text.indexOf(META_END, pos);
        if (end < 0 || end - pos < 2 || ((end - pos) & 1) != 0) {
            return -1;
        }
        int encodedPayloadLength = end - pos - 2;
        if ((encodedPayloadLength & 1) != 0) {
            return -1;
        }
        int checksumHigh = decodeNibble(text.charAt(end - 2));
        int checksumLow = decodeNibble(text.charAt(end - 1));
        if (checksumHigh < 0 || checksumLow < 0) {
            return -1;
        }
        int checksum = 0x5D;
        for (int i = 0; i < encodedPayloadLength; i += 2) {
            int high = decodeNibble(text.charAt(pos + i));
            int low = decodeNibble(text.charAt(pos + i + 1));
            if (high < 0 || low < 0) {
                return -1;
            }
            checksum = (checksum * 33 + ((high << 4) | low)) & 0xFF;
        }
        int expected = (checksumHigh << 4) | checksumLow;
        return checksum == expected ? end + META_END.length() : -1;
    }

    private static int decodeNibble(char c) {
        int value = c - NIBBLE_BASE;
        return value >= 0 && value <= 15 ? value : -1;
    }

    private static int presetChecksum(int id) {
        int value = 0xA7;
        value = (value * 33 + (id & 0xFF)) & 0xFF;
        value = (value * 33 + ((id >>> 8) & 0xFF)) & 0xFF;
        return value;
    }
}
