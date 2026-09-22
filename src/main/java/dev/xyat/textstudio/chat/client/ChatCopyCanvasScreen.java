package dev.xyat.textstudio.chat.client;

import dev.xyat.kineticcore.api.client.input.KineticKeyBindings;
import dev.xyat.kineticcore.api.client.input.KineticMouseButtons;
import dev.xyat.kineticcore.api.client.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.widget.scroll.KineticScroll;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.GuiGraphics;
import dev.xyat.kineticcore.api.client.widget.input.KineticTextFields.KineticEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ChatCopyCanvasScreen extends KineticScreen {
    private final Screen parent;
    private final List<CanvasLine> lines = new ArrayList<>();

    private double scrollTarget = 0D;
    private final KineticScroll.State scrollState = new KineticScroll.State();
    private int startLine = -1, startCol = -1;
    private int endLine = -1, endCol = -1;
    private boolean isDraggingText = false;
    private boolean isDraggingScrollbar = false;

    private long lastClickTime = 0;
    private int lastClickLine = -1;
    private int lastClickCol = -1;

    private boolean firstInit = true;

    private KineticEditBox searchBox;
    private final List<SearchMatch> matches = new ArrayList<>();
    private int currentMatchIdx = -1;
    private String lastSearchQuery = "";
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

    public ChatCopyCanvasScreen(Screen parent, List<GuiMessage.Line> chatHistory) {
        super(Component.translatable("gui.textstudio.chat.canvas_title"));
        setParentScreen(parent);
        this.parent = parent;
        List<GuiMessage.Line> reversed = new ArrayList<>(chatHistory);
        Collections.reverse(reversed);
        for (GuiMessage.Line line : reversed) {
            this.lines.add(new CanvasLine(line));
        }
    }

    private int getMaxScroll() {
        int cH = canvasHeight() - 75 + BOTTOM_EXPAND;
        int totalH = lines.size() * LINE_H;
        int visibleInnerH = Math.max(0, cH - INNER_PADDING * 2);
        int visibleLines = visibleInnerH / LINE_H;
        int visiblePixels = visibleLines * LINE_H;
        return Math.max(0, totalH - visiblePixels);
    }

    @Override
    protected void buildUi() {
        if (this.firstInit) {
            this.scrollTarget = this.getMaxScroll();
            this.scrollState.snap(this.scrollTarget, this.getMaxScroll());
            this.firstInit = false;
        }

        int cX = (canvasWidth() - FRAME_W) / 2;
        int cY = 25;

        this.searchBox = addTextField(
                cX + 2, cY - 18, 120,
                Component.translatable("gui.textstudio.chat.search")
        );
        this.searchBox.setPlaceholder(Component.translatable("gui.textstudio.chat.search_hint"));
        this.searchBox.setResponder(this::onSearchChanged);

        addButton(cX + 125, cY - 18, 30, Component.translatable("gui.textstudio.chat.previous"), null, () -> navigateMatch(-1));
        addButton(cX + 160, cY - 18, 30, Component.translatable("gui.textstudio.chat.next"), null, () -> navigateMatch(1));
        addButton(
                cX + FRAME_W - 70, cY - 18, 70,
                Component.translatable("gui.textstudio.chat.chat.back"),
                Component.translatable("gui.textstudio.chat.chat.back.desc"),
                this::onClose
        );
    }

    private void onSearchChanged(String query) {
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
        int cH = canvasHeight() - 75 + BOTTOM_EXPAND;
        int visibleInnerH = Math.max(0, cH - INNER_PADDING * 2);
        int visiblePixels = (visibleInnerH / LINE_H) * LINE_H;
        int targetY = (match.lineIdx * LINE_H);

        double currentScroll = visualScroll();
        if (targetY < currentScroll || targetY > currentScroll + visiblePixels - LINE_H) {
            scrollTarget = Mth.clamp(targetY - visiblePixels / 2.0D, 0D, getMaxScroll());
        }
    }

    @Override
    protected void renderCanvasBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        int cX = (canvasWidth() - FRAME_W) / 2;
        int cY = 25;
        int cH = canvasHeight() - 75 + BOTTOM_EXPAND;
        int gutter = SCROLLBAR_WIDTH + SCROLLBAR_MARGIN + 2;
        int innerY = cY + INNER_PADDING;
        int innerH = Math.max(0, cH - INNER_PADDING * 2);
        int visibleLines = innerH / LINE_H;
        int visiblePixels = visibleLines * LINE_H;
        long now = System.currentTimeMillis();
        double visualScroll = visualScroll();

        if (!lastSearchQuery.isEmpty()) {
            String countText = (matches.isEmpty() ? 0 : currentMatchIdx + 1) + "/" + matches.size();
            g.drawString(this.font, countText, cX + 195, cY - 16, 0xFFAAAAAA, false);
        }

        GuiTheme.surface(g, cX, cY, FRAME_W, cH, GuiTheme.Surface.PANEL_ALT);
        drawOutwardBorder(g, cX, cY, cH);

        enableUiScissor(g, cX + INNER_PADDING, innerY, cX + FRAME_W - gutter - INNER_PADDING, innerY + visiblePixels);
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
                    GuiTheme.Indicator highlight = isCurrent ? GuiTheme.Indicator.WARNING : GuiTheme.Indicator.INFO;
                    if (isCurrent) {
                        long elapsed = now - flashStartTime;
                        if (elapsed < 400 && (elapsed / 100) % 2 == 0) {
                            highlight = GuiTheme.Indicator.MUTED;
                            highlightAlpha = 1.0F;
                        }
                    }
                    GuiTheme.indicatorFill(g, xStart, lineY - 1, Math.max(1, xEnd - xStart), 10, highlight, highlightAlpha);
                }
            }

            renderLineSelection(g, i, cX + PADDING, lineY, line);
            g.drawString(this.font, line.visual, cX + PADDING, lineY, 0xFFFFFFFF, true);

            for (SearchMatch m : matches) {
                if (m.lineIdx == i) {
                    boolean isCurrent = (matches.indexOf(m) == currentMatchIdx);
                    long elapsed = now - flashStartTime;
                    if (isCurrent && elapsed < 400 && (elapsed / 100) % 2 == 0) {
                        int xStart = cX + PADDING + line.getOffset(m.startCol);
                        String snippet = line.rawText.substring(m.startCol, m.endCol);
                        g.drawString(this.font, snippet, xStart, lineY, 0xFF000000, false);
                    }
                }
            }
        }
        disableUiScissor(g);
        renderThickScrollbar(g, cX + FRAME_W - (SCROLLBAR_WIDTH + SCROLLBAR_MARGIN), innerY, visiblePixels, mx, my);
    }

    @Override
    protected void renderCanvasForeground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        int cX = (canvasWidth() - FRAME_W) / 2;
        int cY = 25;
        int cH = canvasHeight() - 75 + BOTTOM_EXPAND;
        int innerY = cY + INNER_PADDING;
        int innerH = Math.max(0, cH - INNER_PADDING * 2);
        int visiblePixels = (innerH / LINE_H) * LINE_H;

        if (System.currentTimeMillis() < toastEndTime) {
            g.drawCenteredString(this.font, toastText, canvasWidth() / 2, cY + cH + 10, GOLDEN_COLOR);
        }
    }



    @Override
    protected boolean canvasMouseClicked(double mx, double my, int btn) {

        if (this.searchBox != null && !this.searchBox.isMouseOver(mx, my)) {
            blurControl(this.searchBox);
        }

        if (super.canvasMouseClicked(mx, my, btn)) return true;

        double vMx = mx, vMy = my;
        int cX = (canvasWidth() - FRAME_W) / 2;
        int cY = 25, cH = canvasHeight() - 75 + BOTTOM_EXPAND;
        int gutter = SCROLLBAR_WIDTH + SCROLLBAR_MARGIN + 2;
        int innerY = cY + INNER_PADDING;
        int innerH = Math.max(0, cH - INNER_PADDING * 2);
        int visiblePixels = (innerH / LINE_H) * LINE_H;

        if (KineticMouseButtons.isSecondary(btn) && hasSelection()) {
            openContextMenu(
                    vMx,
                    vMy,
                    List.of(KineticOverlays.MenuItem.action(
                            Component.translatable("gui.textstudio.chat.copy"),
                            this::doCopy
                    ))
            );
            return true;
        }

        if (KineticMouseButtons.isPrimary(btn) && vMx >= cX + FRAME_W - gutter && vMx <= cX + FRAME_W && vMy >= innerY && vMy <= innerY + visiblePixels) {
            isDraggingScrollbar = true;
            updateScrollFromMouse(vMy);
            return true;
        }

        int idx = getLineIndexAt(vMx, vMy);
        if (idx != -1) {
            int col = getColAt(idx, vMx);
            long currentTime = System.currentTimeMillis();

            if (KineticMouseButtons.isPrimary(btn) && (currentTime - lastClickTime < 300) && lastClickLine == idx && Math.abs(lastClickCol - col) <= 3) {
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
    protected boolean canvasKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (isControlFocused(this.searchBox)) {
            if (KineticKeyBindings.matchesKeyCode(KineticKeyBindings.Key.ENTER, keyCode)
                    || KineticKeyBindings.matchesKeyCode(KineticKeyBindings.Key.KP_ENTER, keyCode)) {
                navigateMatch(1);
                return true;
            }
            if (KineticKeyBindings.matchesKeyCode(KineticKeyBindings.Key.ESCAPE, keyCode)) {
                blurControl(this.searchBox);
                return true;
            }
        }

        if (KineticClientRuntime.isCopyShortcut(keyCode) && hasSelection()) {
            doCopy();
            return true;
        }
        if (KineticClientRuntime.controlModifierDown()
                && KineticKeyBindings.matchesKeyCode(KineticKeyBindings.Key.F, keyCode)) {
            focusControl(this.searchBox);
            return true;
        }
        return super.canvasKeyPressed(keyCode, scanCode, modifiers);
    }

    private void renderThickScrollbar(GuiGraphics g, int x, int y, int h, double mouseX, double mouseY) {
        int totalH = lines.size() * LINE_H + 4;
        if (totalH <= h) return;
        float ratio = (float) h / totalH;
        int handleH = Math.max(15, (int) (h * ratio));
        int maxScroll = getMaxScroll();
        int handleY = y + (int) Math.round((h - handleH) * (visualScroll() / Math.max(1D, maxScroll)));
        boolean hovered = mouseX >= x && mouseX < x + SCROLLBAR_WIDTH
                && mouseY >= handleY && mouseY < handleY + handleH;

        GuiTheme.scrollbar(
                g,
                mouseX,
                mouseY,
                x,
                y,
                SCROLLBAR_WIDTH,
                h,
                handleH,
                maxScroll,
                visualScroll(),
                isDraggingScrollbar
        );
    }

    private void renderLineSelection(GuiGraphics g, int idx, int x, int y, CanvasLine line) {
        if (startLine == -1 || endLine == -1) return;
        int l1 = startLine, c1 = startCol, l2 = endLine, c2 = endCol;
        if (l1 > l2 || (l1 == l2 && c1 > c2)) { int t=l1; l1=l2; l2=t; t=c1; c1=c2; c2=t; }
        if (idx < l1 || idx > l2) return;

        int s = (idx == l1) ? c1 : 0;
        int e = (idx == l2) ? c2 : line.rawText.length();

        int xStart = x + line.getOffset(s);
        int xEnd = x + line.getOffset(e);

        GuiTheme.indicatorFill(g, xStart, y - 1, Math.max(1, xEnd - xStart), 10, GuiTheme.Indicator.INFO, 0.40F);
    }

    private void drawOutwardBorder(GuiGraphics g, int x, int y, int h) {
        GuiTheme.indicatorOutline(
                g,
                x - MARGIN,
                y - MARGIN,
                FRAME_W + MARGIN * 2,
                h + MARGIN * 2,
                GuiTheme.Indicator.WARNING
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
    protected boolean canvasMouseDragged(double mx, double my, int btn, double dx, double dy) {
        double vMx = mx, vMy = my;
        if (isDraggingScrollbar) {
            updateScrollFromMouse(vMy);
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
        return super.canvasMouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    protected boolean canvasMouseReleased(double mx, double my, int btn) {
        isDraggingText = isDraggingScrollbar = false;
        return super.canvasMouseReleased(mx, my, btn);
    }

    @Override
    protected boolean canvasMouseScrolled(double mx, double my, double delta) {
        scrollTarget = scrollState.wheel(scrollTarget, delta, LINE_H, getMaxScroll());
        return true;
    }

    private void updateScrollFromMouse(double vMy) {
        int cY = 25, cH = canvasHeight() - 75 + BOTTOM_EXPAND;
        int innerY = cY + INNER_PADDING;
        int innerH = Math.max(0, cH - INNER_PADDING * 2);
        int visiblePixels = (innerH / LINE_H) * LINE_H;
        float progress = (float)(vMy - innerY) / (float) visiblePixels;
        progress = Mth.clamp(progress, 0.0f, 1.0f);
        scrollTarget = progress * getMaxScroll();
        scrollState.snap(scrollTarget, getMaxScroll());
    }

    private int getLineIndexAt(double vMx, double vMy) {
        int cX = (canvasWidth() - FRAME_W) / 2;
        int cY = 25, cH = canvasHeight() - 75 + BOTTOM_EXPAND;
        int gutter = SCROLLBAR_WIDTH + SCROLLBAR_MARGIN + 2;
        if (vMx < cX || vMx > cX + FRAME_W - gutter || vMy < cY || vMy > cY + cH) return -1;
        double relativeY = vMy - (cY + 2) + visualScroll();
        int idx = (int) Math.floor(relativeY / LINE_H);
        return (idx >= 0 && idx < lines.size()) ? idx : -1;
    }

    private int getColAt(int idx, double vMx) {
        CanvasLine line = lines.get(idx);
        double lx = vMx - ((canvasWidth() - FRAME_W) / 2.0 + PADDING);
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
        return scrollState.follow(scrollTarget, getMaxScroll(), false);
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
        this.toastText = Component.translatable("msg.textstudio.chat.copy_success").getString();
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
            var font = KineticClientRuntime.font();
            boundaries.add(0);
            this.visual.accept((index, style, cp) -> {
                String s = new String(Character.toChars(cp));
                int w = font.width(net.minecraft.util.FormattedCharSequence.forward(s, style));
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
