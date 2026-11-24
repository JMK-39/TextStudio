package dev.xyat.textstudio.font.common.text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Pasting into a sign. Vanilla refuses a paste that does not fit the line whole, so long text could only be pasted after
 * cutting it down by hand. Here the line takes what fits; the rest wraps onto the empty lines below it (a line break in
 * the pasted text starts the next line) and what is still left over is dropped. Lines that already hold text are never
 * written over. Text after the cursor follows the pasted text, as vanilla puts it. Effect codes are never cut, and
 * every wrapped line starts with the effect in force where it was cut, so the effect carries on. Breaks fall between
 * words in Latin text and keep Chinese punctuation off the start of a line. Works without Minecraft, so it can be
 * checked headless.
 */
public final class SignPaste {
    /** How many characters a break may move back for a word or punctuation rule. */
    private static final int MAX_MOVE = 3;
    private static final String NO_START = "，。、；：？！）］｝〕〉》」』】〙〗〟｠”’»…‥·・—～％‰℃．｡､,.;:?!)]}%";
    private static final String NO_END = "（［｛〔〈《「『【〘〖〝｟“‘«([{";

    /** The sign's lines after the paste, and where the cursor ends: after the last pasted character. */
    public record Result(String[] lines, int line, int cursor) {
    }

    private SignPaste() {
    }

    /**
     * Pastes {@code clipboard} at the cursor of {@code lines[line]}, replacing the selection
     * ({@code selection} to {@code cursor}). {@code fits} tells whether a whole line fits the sign.
     */
    public static Result paste(String[] lines, int line, int cursor, int selection, String clipboard, Predicate<String> fits) {
        String current = lines[line];
        int start = clamp(Math.min(cursor, selection), current.length());
        int end = clamp(Math.max(cursor, selection), current.length());
        String after = current.substring(end);
        // The lines the paste may use: its own and the empty ones right below it.
        int lastLine = line;
        while (lastLine + 1 < lines.length && lines[lastLine + 1].isEmpty()) {
            lastLine++;
        }

        List<String> filled = new ArrayList<>();
        Row row = new Row(current.substring(0, start));
        String[] paragraphs = clipboard.replace("\r", "").split("\n");
        boolean full = false;
        for (int p = 0; p < paragraphs.length && !full; p++) {
            if (p > 0) {
                if (line + filled.size() >= lastLine) {
                    break;
                }
                filled.add(row.text());
                row = new Row(codeInForce(row.text()));
            }
            List<String> units = units(paragraphs[p]);
            int i = 0;
            while (i < units.size()) {
                int fit = fitCount(row, units, i, fits);
                if (i + fit == units.size()) {
                    row.add(units, i, units.size());
                    break;
                }
                if (line + filled.size() >= lastLine) {
                    row.add(units, i, i + fit);
                    full = true;
                    break;
                }
                if (fit == 0 && row.units.isEmpty() && row.head.equals(codeInForce(row.head))) {
                    // Too wide even for a line of its own.
                    i++;
                    continue;
                }
                int cut = betterBreak(units, i, i + fit);
                row.add(units, i, cut);
                row.trimSpaces();
                filled.add(row.text());
                row = new Row(codeInForce(row.text()));
                i = cut;
                while (i < units.size() && units.get(i).equals(" ")) {
                    i++;
                }
            }
        }

        // The text that was after the cursor goes after the pasted text: on its line if it fits, else on a line of its
        // own below, else pasted text makes room for it.
        int cursorLine = line + filled.size();
        String pasted = row.text();
        if (after.isEmpty() || fits.test(pasted + after)) {
            filled.add(pasted + after);
        } else if (cursorLine < lastLine) {
            filled.add(pasted);
            filled.add(codeInForce(pasted) + after);
        } else {
            while (!row.units.isEmpty() && !fits.test(row.text() + after)) {
                row.units.remove(row.units.size() - 1);
            }
            pasted = row.text();
            filled.add(pasted + after);
        }

        String[] result = lines.clone();
        for (int i = 0; i < filled.size(); i++) {
            result[line + i] = filled.get(i);
        }
        return new Result(result, cursorLine, pasted.length());
    }

    /** A line being filled: the text it starts with (what was before the cursor, or the effect carried over) and the units added to it. */
    private static final class Row {
        private final String head;
        private final List<String> units = new ArrayList<>();

        private Row(String head) {
            this.head = head;
        }

        private void add(List<String> from, int start, int end) {
            units.addAll(from.subList(start, end));
        }

        private void trimSpaces() {
            while (!units.isEmpty() && units.get(units.size() - 1).equals(" ")) {
                units.remove(units.size() - 1);
            }
        }

        private String text() {
            return head + String.join("", units);
        }

        private String textWith(List<String> more, int start, int end) {
            return text() + String.join("", more.subList(start, end));
        }
    }

    /** How many units from {@code start} on fit after what the row holds. */
    private static int fitCount(Row row, List<String> units, int start, Predicate<String> fits) {
        int count = 0;
        while (start + count < units.size() && fits.test(row.textWith(units, start, start + count + 1))) {
            count++;
        }
        return count;
    }

    /**
     * Where to end a line that would break before unit {@code at}: between words rather than inside one, and not
     * with closing punctuation starting the next line or opening punctuation ending this one. Never empties the line.
     */
    private static int betterBreak(List<String> units, int start, int at) {
        if (at <= start + 1) {
            return at;
        }
        int next = visibleCodePoint(units, at, 1);
        int previous = visibleCodePoint(units, at - 1, -1);
        if (isWordChar(next) && isWordChar(previous)) {
            for (int i = at - 1; i > start; i--) {
                if (units.get(i).equals(" ")) {
                    return i + 1;
                }
            }
            return at;
        }
        for (int move = 0; move <= MAX_MOVE && at - move > start; move++) {
            int cut = at - move;
            int first = visibleCodePoint(units, cut, 1);
            int last = visibleCodePoint(units, cut - 1, -1);
            if (NO_START.indexOf(first) < 0 && NO_END.indexOf(last) < 0) {
                return cut;
            }
        }
        return at;
    }

    /** The first character from unit {@code index} on (or back, for a negative {@code step}) that is not an effect code; -1 if none. */
    private static int visibleCodePoint(List<String> units, int index, int step) {
        for (int i = index; i >= 0 && i < units.size(); i += step) {
            String unit = units.get(i);
            if (InputMetrics.controlEnd(unit, 0) < 0) {
                return unit.codePointAt(0);
            }
        }
        return -1;
    }

    private static boolean isWordChar(int codePoint) {
        return codePoint >= 0 && codePoint < 0x2E80 && Character.isLetterOrDigit(codePoint);
    }

    /** The text cut into what cannot be split: effect codes whole, everything else by character. Control characters are left out. */
    private static List<String> units(String text) {
        List<String> units = new ArrayList<>();
        for (int i = 0; i < text.length();) {
            int controlEnd = InputMetrics.controlEnd(text, i);
            if (controlEnd > i) {
                units.add(text.substring(i, controlEnd));
                i = controlEnd;
                continue;
            }
            int codePoint = text.codePointAt(i);
            i += Character.charCount(codePoint);
            if (codePoint == '\t') {
                units.add(" ");
            } else if (codePoint >= ' ' && codePoint != 127) {
                units.add(new String(Character.toChars(codePoint)));
            }
        }
        return units;
    }

    /** The effect code in force at the end of the text, so a line cut from it can carry on with it; "" if none. */
    static String codeInForce(String text) {
        String code = "";
        for (int i = text.indexOf('\u2063'); i >= 0; i = text.indexOf('\u2063', i)) {
            int controlEnd = InputMetrics.controlEnd(text, i);
            if (controlEnd < 0) {
                i++;
                continue;
            }
            code = InputMetrics.isReset(text, i) ? "" : text.substring(i, controlEnd);
            i = controlEnd;
        }
        return code;
    }

    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(value, max));
    }
}
