package dev.xyat.textstudio.chat.client;

import dev.xyat.kineticcore.api.client.gui.input.KeyInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseDragInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.input.ScrollInput;
import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.scroll.KineticScrollController;
import dev.xyat.kineticcore.api.client.gui.text.KineticText;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.widget.KineticTextField;
import dev.xyat.kineticcore.api.client.input.KineticKeyBindings;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.text.KineticI18n;
import net.minecraft.client.GuiMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ChatCopyCanvasPage extends KineticPage {
    private final List<CanvasLine> lines = new ArrayList<>();

    private final KineticScrollController scroll = new KineticScrollController();
    private int startLine = -1, startCol = -1;
    private int endLine = -1, endCol = -1;
    private boolean isDraggingText = false;
    private boolean isDraggingScrollbar = false;

    private long lastClickTime = 0;
    private int lastClickLine = -1;
    private int lastClickCol = -1;

    private boolean firstInit = true;

    private KineticTextField searchBox;
    private final List<SearchMatch> matches = new ArrayList<>();
    private int currentMatchIdx = -1;
    private String lastSearchQuery = "";
    private String lastSearchQueryRaw = "";
    private long flashStartTime = 0;

    private String toastText = "";
    private long toastEndTime = 0;

    private static final int FRAME_W = 360;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SCROLLBAR_MARGIN = 2;
    private static final int LINE_H = 10;
    private static final int MARGIN = 2;
    private static final int PADDING = 2;

    private static final int INNER_PADDING = 2;

    private static final int BOTTOM_EXPAND = 30;
    private static final int GOLDEN_COLOR = 0xFFFFD700;

    public ChatCopyCanvasPage(List<GuiMessage.Line> chatHistory) {
        super(KineticI18n.translatable("gui.textstudio.chat.canvas_title"));
        List<GuiMessage.Line> reversed = new ArrayList<>(chatHistory);
        Collections.reverse(reversed);
        for (GuiMessage.Line line : reversed) {
            this.lines.add(new CanvasLine(line));
        }
    }

    /** 可完整显示的行数 / Number of fully visible lines. */
    private int visibleLines() {
        int cH = height() - 75 + BOTTOM_EXPAND;
        return Math.max(0, cH - INNER_PADDING * 2) / LINE_H;
    }

    private void updateScroll() {
        scroll.update(lines.size(), visibleLines());
    }

    @Override
    protected void build(KineticUi ui) {
        updateScroll();
        if (this.firstInit) {
            scroll.setOffset(scroll.maxOffset());
            this.firstInit = false;
        }

        int cX = (width() - FRAME_W) / 2;
        int cY = 25;

        this.searchBox = ui.textField(cX + 2, cY - 20, 120)
                .label(KineticI18n.translatable("gui.textstudio.chat.search"))
                .placeholder(KineticI18n.translatable("gui.textstudio.chat.search_hint"))
                .value(lastSearchQueryRaw)
                .firstShownTextAsDefault().build();
        this.searchBox.onTextChange(this::onSearchChanged);

        ui.button(cX + 125, cY - 20, 30).text(KineticI18n.translatable("gui.textstudio.chat.previous")).onClick(() -> navigateMatch(-1)).build();
        ui.button(cX + 160, cY - 20, 30).text(KineticI18n.translatable("gui.textstudio.chat.next")).onClick(() -> navigateMatch(1)).build();
        ui.button(cX + FRAME_W - 70, cY - 20, 70)
                .text(KineticI18n.translatable("gui.textstudio.chat.chat.back"))
                .tooltip(KineticI18n.translatable("gui.textstudio.chat.chat.back.desc"))
                .onClick(this::close)
                .build();
    }

    private void onSearchChanged(String query) {
        this.lastSearchQueryRaw = query;
        this.matches.clear();
        this.currentMatchIdx = -1;
        this.lastSearchQuery = query.toLowerCase(Locale.ROOT);

        if (!lastSearchQuery.isEmpty()) {
            for (int i = 0; i < lines.size(); i++) {
                String text = lines.get(i).rawText.toLowerCase(Locale.ROOT);
                int index = text.indexOf(lastSearchQuery);
                while (index >= 0) {
                    matches.add(new SearchMatch(i, index, index + lastSearchQuery.length()));
                    index = text.indexOf(lastSearchQuery, index + 1);
                }
            }
            if (!matches.isEmpty()) {
                currentMatchIdx = matches.size() - 1;
                flashStartTime = System.currentTimeMillis();
                scrollToMatch(matches.get(currentMatchIdx));
            }
        }
    }

    private void navigateMatch(int direction) {
        if (matches.isEmpty()) return;
        currentMatchIdx = (currentMatchIdx + direction + matches.size()) % matches.size();
        flashStartTime = System.currentTimeMillis();
        scrollToMatch(matches.get(currentMatchIdx));
    }

    private void scrollToMatch(SearchMatch match) {
        updateScroll();
        int visible = visibleLines();
        double first = scroll.smoothOffset();
        if (match.lineIdx < first || match.lineIdx > first + visible - 1) {
            scroll.scrollTo((int) Math.round(match.lineIdx - visible / 2.0D));
        }
    }

    @Override
    protected void renderBackground(KineticGraphics g, int mx, int my, float pt) {
        updateScroll();
        int cX = (width() - FRAME_W) / 2;
        int cY = 25;
        int cH = height() - 75 + BOTTOM_EXPAND;
        int gutter = SCROLLBAR_WIDTH + SCROLLBAR_MARGIN + 2;
        int innerY = cY + INNER_PADDING;
        int innerH = Math.max(0, cH - INNER_PADDING * 2);
        int visibleLines = innerH / LINE_H;
        int visiblePixels = visibleLines * LINE_H;
        long now = System.currentTimeMillis();
        double visualScroll = visualScroll();

        if (!lastSearchQuery.isEmpty()) {
            String countText = (matches.isEmpty() ? 0 : currentMatchIdx + 1) + "/" + matches.size();
            g.text(countText, cX + 195, cY - 18, 0xFFAAAAAA, false);
        }

        KineticTheme.surface(g, cX, cY, FRAME_W, cH, KineticTheme.Surface.PANEL_ALT);
        drawOutwardBorder(g, cX, cY, cH);

        g.scissor(cX + INNER_PADDING, innerY, cX + FRAME_W - gutter - INNER_PADDING, innerY + visiblePixels);
        for (int i = 0; i < lines.size(); i++) {
            int lineY = innerY + (i * LINE_H) - (int) Math.round(visualScroll);
            if (lineY + LINE_H <= innerY || lineY >= innerY + visiblePixels) continue;

            CanvasLine line = lines.get(i);
            for (SearchMatch m : matches) {
                if (m.lineIdx == i) {
                    int xStart = cX + PADDING + line.getOffset(m.startCol);
                    int xEnd = cX + PADDING + line.getOffset(m.endCol);
                    boolean isCurrent = (matches.indexOf(m) == currentMatchIdx);
                    float highlightAlpha = isCurrent ? 0.67F : 0.53F;
                    KineticTheme.Indicator highlight = isCurrent ? KineticTheme.Indicator.WARNING : KineticTheme.Indicator.INFO;
                    if (isCurrent) {
                        long elapsed = now - flashStartTime;
                        if (elapsed < 400 && (elapsed / 100) % 2 == 0) {
                            highlight = KineticTheme.Indicator.MUTED;
                            highlightAlpha = 1.0F;
                        }
                    }
                    KineticTheme.indicatorFill(g, xStart, lineY - 1, Math.max(1, xEnd - xStart), 10, highlight, highlightAlpha);
                }
            }

            renderLineSelection(g, i, cX + PADDING, lineY, line);
            g.text(line.visual, cX + PADDING, lineY, 0xFFFFFFFF, true);

            for (SearchMatch m : matches) {
                if (m.lineIdx == i) {
                    boolean isCurrent = (matches.indexOf(m) == currentMatchIdx);
                    long elapsed = now - flashStartTime;
                    if (isCurrent && elapsed < 400 && (elapsed / 100) % 2 == 0) {
                        int xStart = cX + PADDING + line.getOffset(m.startCol);
                        String snippet = line.rawText.substring(m.startCol, m.endCol);
                        g.text(snippet, xStart, lineY, 0xFF000000, false);
                    }
                }
            }
        }
        g.endScissor();
        if (scroll.canScroll()) {
            scroll.render(g, mx, my, cX + FRAME_W - (SCROLLBAR_WIDTH + SCROLLBAR_MARGIN), innerY, SCROLLBAR_WIDTH, visiblePixels, 15);
        }
    }

    @Override
    protected void renderForeground(KineticGraphics g, int mx, int my, float pt) {
        int cY = 25;
        int cH = height() - 75 + BOTTOM_EXPAND;
        if (System.currentTimeMillis() < toastEndTime) {
            g.centeredText(toastText, width() / 2, cY + cH + 10, GOLDEN_COLOR, true);
        }
    }



    @Override
    protected boolean onMouseClickCapture(MouseInput input) {
        if (this.searchBox != null && !this.searchBox.contains(input.x(), input.y())) {
            blur(this.searchBox);
        }
        return false;
    }

    @Override
    protected boolean onMouseClick(MouseInput input) {
        double vMx = input.x(), vMy = input.y();
        int cX = (width() - FRAME_W) / 2;
        int cY = 25, cH = height() - 75 + BOTTOM_EXPAND;
        int gutter = SCROLLBAR_WIDTH + SCROLLBAR_MARGIN + 2;
        int innerY = cY + INNER_PADDING;
        int innerH = Math.max(0, cH - INNER_PADDING * 2);
        int visiblePixels = (innerH / LINE_H) * LINE_H;

        if (input.isRight() && hasSelection()) {
            openContextMenu(
                    vMx,
                    vMy,
                    List.of(KineticOverlays.MenuItem.action(
                            KineticI18n.translatable("gui.textstudio.chat.copy"),
                            this::doCopy
                    ))
            );
            return true;
        }

        updateScroll();
        if (scroll.canScroll() && scroll.beginDrag(vMx, vMy, input.button(), cX + FRAME_W - gutter, innerY, gutter, visiblePixels, 15)) {
            isDraggingScrollbar = true;
            return true;
        }

        int idx = getLineIndexAt(vMx, vMy);
        if (idx != -1) {
            int col = getColAt(idx, vMx);
            long currentTime = System.currentTimeMillis();

            if (input.isLeft() && (currentTime - lastClickTime < 300) && lastClickLine == idx && Math.abs(lastClickCol - col) <= 3) {
                int[] bounds = getWordBoundaries(lines.get(idx).rawText, col);
                startLine = endLine = idx;
                startCol = bounds[0];
                endCol = bounds[1];
                isDraggingText = false;
            } else {
                startLine = endLine = idx;
                startCol = endCol = col;
                isDraggingText = true;
            }

            lastClickTime = currentTime;
            lastClickLine = idx;
            lastClickCol = col;
            return true;
        }

        startLine = -1;
        return false;
    }

    @Override
    protected boolean onKeyPress(KeyInput input) {
        if (isFocused(this.searchBox)) {
            if (input.isEnter()) {
                navigateMatch(1);
                return true;
            }
            if (input.isEscape()) {
                blur(this.searchBox);
                return true;
            }
        }

        if (KineticClientRuntime.isCopyShortcut(input.keyCode()) && hasSelection()) {
            doCopy();
            return true;
        }
        if (input.hasControl() && input.is(KineticKeyBindings.Key.F)) {
            focus(this.searchBox);
            return true;
        }
        return false;
    }

    private void renderLineSelection(KineticGraphics g, int idx, int x, int y, CanvasLine line) {
        if (startLine == -1 || endLine == -1) return;
        int l1 = startLine, c1 = startCol, l2 = endLine, c2 = endCol;
        if (l1 > l2 || (l1 == l2 && c1 > c2)) { int t=l1; l1=l2; l2=t; t=c1; c1=c2; c2=t; }
        if (idx < l1 || idx > l2) return;

        int s = (idx == l1) ? c1 : 0;
        int e = (idx == l2) ? c2 : line.rawText.length();

        int xStart = x + line.getOffset(s);
        int xEnd = x + line.getOffset(e);

        KineticTheme.indicatorFill(g, xStart, y - 1, Math.max(1, xEnd - xStart), 10, KineticTheme.Indicator.INFO, 0.40F);
    }

    private void drawOutwardBorder(KineticGraphics g, int x, int y, int h) {
        KineticTheme.indicatorOutline(
                g,
                x - MARGIN,
                y - MARGIN,
                FRAME_W + MARGIN * 2,
                h + MARGIN * 2,
                KineticTheme.Indicator.WARNING
        );
    }

    private boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == ':' || c == '/' || c == '-' || c == '?' || c == '=' || c == '&' || c == '%';
    }

    private int[] getWordBoundaries(String text, int col) {
        if (text == null || text.isEmpty()) return new int[]{0, 0};
        if (col >= text.length()) col = text.length() - 1;
        if (col < 0) col = 0;

        char c = text.charAt(col);
        boolean isAlphanumeric = isWordChar(c);
        boolean isWhitespace = Character.isWhitespace(c);

        int start = col;
        while (start > 0) {
            char prev = text.charAt(start - 1);
            if (isWhitespace && Character.isWhitespace(prev)) start--;
            else if (isAlphanumeric && isWordChar(prev)) start--;
            else if (!isWhitespace && !isAlphanumeric && !Character.isWhitespace(prev) && !isWordChar(prev)) start--;
            else break;
        }

        int end = col;
        while (end < text.length() - 1) {
            char next = text.charAt(end + 1);
            if (isWhitespace && Character.isWhitespace(next)) end++;
            else if (isAlphanumeric && isWordChar(next)) end++;
            else if (!isWhitespace && !isAlphanumeric && !Character.isWhitespace(next) && !isWordChar(next)) end++;
            else break;
        }
        return new int[]{start, end + 1};
    }

    @Override
    protected boolean onMouseDrag(MouseDragInput input) {
        double vMx = input.x(), vMy = input.y();
        if (isDraggingScrollbar) {
            updateScroll();
            scroll.drag(vMy, 25 + INNER_PADDING, visibleLines() * LINE_H, 15);
            return true;
        }
        if (isDraggingText) {
            int idx = getLineIndexAt(vMx, vMy);
            if (idx != -1) {
                endLine = idx;
                endCol = getColAt(idx, vMx);
            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean onMouseRelease(MouseInput input) {
        isDraggingText = isDraggingScrollbar = false;
        scroll.release(input.button());
        return false;
    }

    @Override
    protected boolean onMouseScroll(ScrollInput input) {
        updateScroll();
        scroll.scroll(input.deltaY());
        return true;
    }

    private int getLineIndexAt(double vMx, double vMy) {
        int cX = (width() - FRAME_W) / 2;
        int cY = 25, cH = height() - 75 + BOTTOM_EXPAND;
        int gutter = SCROLLBAR_WIDTH + SCROLLBAR_MARGIN + 2;
        if (vMx < cX || vMx > cX + FRAME_W - gutter || vMy < cY || vMy > cY + cH) return -1;
        double relativeY = vMy - (cY + 2) + visualScroll();
        int idx = (int) Math.floor(relativeY / LINE_H);
        return (idx >= 0 && idx < lines.size()) ? idx : -1;
    }

    private int getColAt(int idx, double vMx) {
        CanvasLine line = lines.get(idx);
        double lx = vMx - ((width() - FRAME_W) / 2.0 + PADDING);
        if (lx <= 0) return 0;
        int bestCol = 0;
        double minDiff = Double.MAX_VALUE;
        for (int i = 0; i < line.splitOffsets.length; i++) {
            double diff = Math.abs(line.splitOffsets[i] - lx);
            if (diff < minDiff) { minDiff = diff; bestCol = i; }
        }
        return bestCol;
    }

    private double visualScroll() {
        return scroll.smoothOffset() * LINE_H;
    }

    private void doCopy() {
        StringBuilder sb = new StringBuilder();
        int l1 = startLine, c1 = startCol, l2 = endLine, c2 = endCol;
        if (l1 > l2 || (l1 == l2 && c1 > c2)) { int t=l1; l1=l2; l2=t; t=c1; c1=c2; c2=t; }
        for (int i = l1; i <= l2; i++) {
            CanvasLine line = lines.get(i);
            int s = (i == l1) ? c1 : 0;
            int e = (i == l2) ? c2 : line.rawText.length();
            if (s < e) sb.append(line.rawText, s, e);
            if (i < l2) sb.append("\n");
        }
        KineticClientRuntime.setClipboard(sb.toString());
        this.toastText = KineticI18n.string("msg.textstudio.chat.copy_success");
        this.toastEndTime = System.currentTimeMillis() + 2500;
    }

    private boolean hasSelection() { return startLine != -1 && (startLine != endLine || startCol != endCol); }

    private static class CanvasLine {
        final net.minecraft.util.FormattedCharSequence visual;
        final String rawText;
        final int[] splitOffsets;

        CanvasLine(GuiMessage.Line line) {
            this.visual = line.content();
            StringBuilder sb = new StringBuilder();
            List<Integer> boundaries = new ArrayList<>();
            int[] currentX = {0};
            boundaries.add(0);
            this.visual.accept((index, style, cp) -> {
                String s = new String(Character.toChars(cp));
                int w = KineticText.width(net.minecraft.util.FormattedCharSequence.forward(s, style));
                int startLen = sb.length();
                sb.append(s);
                int endLen = sb.length();
                for (int k = startLen; k < endLen; k++) {
                    currentX[0] += (k == startLen ? w : 0);
                    boundaries.add(currentX[0]);
                }
                return true;
            });
            this.rawText = sb.toString();
            this.splitOffsets = boundaries.stream().mapToInt(i -> i).toArray();
        }

        int getOffset(int col) {
            if (col <= 0 || splitOffsets.length == 0) return 0;
            if (col >= splitOffsets.length) return splitOffsets[splitOffsets.length - 1];
            return splitOffsets[col];
        }
    }

    private record SearchMatch(int lineIdx, int startCol, int endCol) {}
}
