package dev.xyat.textstudio.font;

import dev.xyat.textstudio.font.common.text.InputMetrics;
import dev.xyat.textstudio.font.common.text.SignPaste;

import java.util.Arrays;
import java.util.function.Predicate;

public final class SignPasteRegression {
    private static final String RESET = "\u2063\u2064\uFE0B\uFE05\u2064\u2063";
    // Ten visible characters a line, effect codes not counted, as the sign's width check does.
    private static final Predicate<String> FITS = text -> text.length() <= 384 && InputMetrics.visibleCodePointLength(text) <= 10;

    public static void main(String[] args) {
        String code = preset(1);
        expect("long text fills the lines and the rest is dropped",
                SignPaste.paste(empty(), 0, 0, 0, "一二三四五六七八九十甲乙丙丁戊己庚辛壬癸子丑寅卯辰巳午未申酉戌亥天地玄黄宇宙洪荒日月盈", FITS),
                new String[]{"一二三四五六七八九十", "甲乙丙丁戊己庚辛壬癸", "子丑寅卯辰巳午未申酉", "戌亥天地玄黄宇宙洪荒"}, 3, 10);
        expect("a wrapped line keeps the effect",
                SignPaste.paste(empty(), 0, 0, 0, code + "一二三四五六七八九十甲乙", FITS),
                new String[]{code + "一二三四五六七八九十", code + "甲乙", "", ""}, 1, code.length() + 2);
        expect("a stop code ends the carried effect",
                SignPaste.paste(empty(), 0, 0, 0, code + "一二三" + RESET + "四五六七八九十甲乙", FITS),
                new String[]{code + "一二三" + RESET + "四五六七八九十", "甲乙", "", ""}, 1, 2);
        expect("lines holding text are not written over",
                SignPaste.paste(new String[]{"", "", "keep", ""}, 0, 0, 0, "一二三四五六七八九十甲乙丙丁戊己庚辛壬癸子丑", FITS),
                new String[]{"一二三四五六七八九十", "甲乙丙丁戊己庚辛壬癸", "keep", ""}, 1, 10);
        expect("Latin text breaks between words",
                SignPaste.paste(empty(), 0, 0, 0, "hello world again", FITS),
                new String[]{"hello", "world", "again", ""}, 2, 5);
        expect("closing punctuation does not start a line",
                SignPaste.paste(empty(), 0, 0, 0, "一二三四五六七八九十，好", FITS),
                new String[]{"一二三四五六七八九", "十，好", "", ""}, 1, 3);
        expect("a line break in the text starts the next line",
                SignPaste.paste(empty(), 0, 0, 0, "ab\r\n\ncd\n", FITS),
                new String[]{"ab", "", "cd", ""}, 2, 2);
        expect("text after the cursor follows the paste",
                SignPaste.paste(new String[]{"XY", "", "", ""}, 0, 1, 1, "abc", FITS),
                new String[]{"XabcY", "", "", ""}, 0, 4);
        expect("text after the cursor wraps with the paste",
                SignPaste.paste(new String[]{"12345678Z", "", "", ""}, 0, 8, 8, "abcdef", FITS),
                new String[]{"12345678ab", "cdefZ", "", ""}, 1, 4);
        expect("on the last line the paste makes room for the text after it",
                SignPaste.paste(new String[]{"", "", "", "12345678Z"}, 3, 8, 8, "abcdef", FITS),
                new String[]{"", "", "", "12345678aZ"}, 3, 9);
        expect("a selection is replaced",
                SignPaste.paste(new String[]{"abcdef", "", "", ""}, 0, 1, 5, "XY", FITS),
                new String[]{"aXYf", "", "", ""}, 0, 3);
        System.out.println("SignPasteRegression: fill, wrap, effects, words, punctuation, breaks and cursor text passed");
    }

    private static String[] empty() {
        return new String[]{"", "", "", ""};
    }

    private static void expect(String name, SignPaste.Result result, String[] lines, int line, int cursor) {
        if (!Arrays.equals(result.lines(), lines) || result.line() != line || result.cursor() != cursor) {
            throw new AssertionError(name + ": got " + Arrays.toString(result.lines()) + " line " + result.line() + " cursor " + result.cursor());
        }
    }

    /** A preset effect code as the editor writes it. */
    private static String preset(int id) {
        int checksum = 0xA7;
        checksum = (checksum * 33 + (id & 0xFF)) & 0xFF;
        checksum = (checksum * 33 + ((id >>> 8) & 0xFF)) & 0xFF;
        StringBuilder out = new StringBuilder("\u2063\u2061");
        for (int shift = 12; shift >= 0; shift -= 4) {
            out.append((char) ('\uFE00' + ((id >>> shift) & 15)));
        }
        out.append((char) ('\uFE00' + (checksum >>> 4))).append((char) ('\uFE00' + (checksum & 15)));
        return out.append("\u2061\u2063").toString();
    }
}
