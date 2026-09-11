package dev.xyat.textstudio.font.common.text;

import java.util.Collection;
import java.util.Locale;

public final class AuthorNamePolicy {
    private static final String LEGACY_CODES = "0123456789abcdefklmnorABCDEFKLMNOR";
    private static final String HEX_DIGITS = "0123456789abcdefABCDEF";

    private AuthorNamePolicy() {
    }

    public static String normalizeRawName(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    public static String visibleText(String value) {
        String withoutControls = InputMetrics.stripControlCodes(value);
        if (withoutControls.isEmpty() || withoutControls.indexOf('§') < 0) {
            return withoutControls;
        }
        StringBuilder out = new StringBuilder(withoutControls.length());
        for (int i = 0; i < withoutControls.length();) {
            char c = withoutControls.charAt(i);
            if (c == '§' && i + 1 < withoutControls.length()) {
                char code = withoutControls.charAt(i + 1);
                if ((code == 'x' || code == 'X') && isHexSequence(withoutControls, i)) {
                    i += 14;
                    continue;
                }
                if (LEGACY_CODES.indexOf(code) >= 0) {
                    i += 2;
                    continue;
                }
            }
            int codePoint = withoutControls.codePointAt(i);
            out.appendCodePoint(codePoint);
            i += Character.charCount(codePoint);
        }
        return out.toString();
    }

    public static int visibleCodePointLength(String value) {
        String visible = visibleText(value);
        return visible.codePointCount(0, visible.length());
    }

    public static boolean isValidName(String rawName, int maxVisibleCodePoints) {
        String visible = visibleText(rawName);
        if (visible.isEmpty() || visible.codePointCount(0, visible.length()) > Math.max(0, maxVisibleCodePoints)) {
            return false;
        }
        for (int i = 0; i < visible.length();) {
            int codePoint = visible.codePointAt(i);
            i += Character.charCount(codePoint);
            if (codePoint == ' ' || codePoint == '_' || codePoint == '-' || Character.isDigit(codePoint)) {
                continue;
            }
            Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
            if (script == Character.UnicodeScript.HAN || script == Character.UnicodeScript.LATIN && Character.isLetter(codePoint)) {
                continue;
            }
            return false;
        }
        return true;
    }

    public static boolean containsReservedName(String rawName, Collection<String> reservedNames) {
        if (reservedNames == null || reservedNames.isEmpty()) {
            return false;
        }
        String visible = canonicalVisibleName(rawName);
        if (visible.isEmpty()) {
            return false;
        }
        for (String reserved : reservedNames) {
            String canonicalReserved = canonicalVisibleName(reserved);
            if (!canonicalReserved.isEmpty() && visible.contains(canonicalReserved)) {
                return true;
            }
        }
        return false;
    }

    public static String canonicalVisibleName(String rawName) {
        return visibleText(rawName).trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static boolean isHexSequence(String text, int sectionIndex) {
        int end = sectionIndex + 14;
        if (end > text.length()) {
            return false;
        }
        for (int i = sectionIndex + 2; i < end; i += 2) {
            if (text.charAt(i) != '§' || HEX_DIGITS.indexOf(text.charAt(i + 1)) < 0) {
                return false;
            }
        }
        return true;
    }
}
