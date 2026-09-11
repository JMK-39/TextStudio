package dev.xyat.textstudio.font.client;

import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.overlay.GuiOverlay;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets.Scroll;
import dev.xyat.textstudio.font.api.IStyle;
import dev.xyat.textstudio.font.client.parser.CompactTagCodec;
import dev.xyat.textstudio.font.config.AuthorConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class FontModuleGuideScreen extends KineticScreen {
    private static final int CANVAS_W = 640;
    private static final int CANVAS_H = 360;
    private static final int LIST_X = 8;
    private static final int LIST_Y = 8;
    private static final int LIST_W = 226;
    private static final int LIST_H = 344;
    private static final int ROW_H = 17;
    private static final int INFO_X = 242;
    private static final int INFO_Y = 8;
    private static final int INFO_W = 390;
    private static final int INFO_H = 344;
    private static final int VISIBLE_ROWS = 18;
    private static final int LIST_SCROLLBAR_X = LIST_X + LIST_W - 8;
    private static final int LIST_SCROLLBAR_W = 4;
    private static final int PRESET_SELECTED_OUTLINE = 0xFFFFE600;
    private static final int PRESET_HOVER_OUTLINE = 0xFF55AAFF;
    private static final float PREVIEW_SCALE = 1.65f;
    private static final int PREVIEW_CONTENT_X = INFO_X + 12;
    private static final int PREVIEW_CONTENT_Y = INFO_Y + 94;
    private static final int PREVIEW_CONTENT_W = INFO_W - 30;
    private static final int PREVIEW_CONTENT_H = 176;
    private static final int PREVIEW_SCROLLBAR_X = INFO_X + INFO_W - 8;
    private static final int PREVIEW_SCROLLBAR_W = 4;

    private final Screen parent;
    private int selectedPreset;
    private double scroll;
    private final Scroll.State listScrollSmoothing = new Scroll.State();
    private boolean listScrollbarDragging;
    private double previewScroll;
    private final Scroll.State previewScrollSmoothing = new Scroll.State();
    private boolean previewScrollbarDragging;
    private String previewText;
    private EditBox previewBox;
    private String previewLinesText;
    private int previewLinesPreset = -1;
    private AuthorConfig.EffectSettings previewLinesEffect;
    private List<FormattedCharSequence> previewLines = List.of();

    public static Screen create(Screen parent) {
        return new FontModuleGuideScreen(parent);
    }

    private FontModuleGuideScreen(Screen parent) {
        super(Component.translatable("gui.textstudio.font.guide.title"));
        this.parent = parent;
        this.previewText = I18n.get("gui.textstudio.font.editor.preview.default");
        useCanvas(CANVAS_W, CANVAS_H, 6);
        maxScale = 1.0f;
    }

    @Override
    protected void buildUi() {
        selectedPreset = Mth.clamp(selectedPreset, 0, Math.max(0, AuthorConfig.EFFECTS.size() - 1));
        scroll = Mth.clamp(scroll, 0, maxScroll());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.textstudio.font.guide.back"),
                button -> onClose()
        ).bounds(INFO_X + INFO_W - 72, INFO_Y + 6, 64, 18).build());

        previewBox = new EditBox(
                font,
                INFO_X + 12,
                INFO_Y + 52,
                210,
                18,
                Component.translatable("gui.textstudio.font.editor.preview.input")
        );
        previewBox.setMaxLength(4096);
        previewBox.setValue(previewText);
        previewBox.setHint(Component.translatable("gui.textstudio.font.editor.preview.hint"));
        previewBox.setTooltip(Tooltip.create(Component.translatable("gui.textstudio.font.editor.tip.preview_input")));
        previewBox.setResponder(value -> {
            previewText = value;
            previewScroll = 0;
            invalidatePreviewLines();
        });
        addRenderableWidget(previewBox);

        addRenderableWidget(Button.builder(
                Component.translatable("gui.textstudio.font.editor.copy"),
                button -> copyCurrent()
        ).bounds(INFO_X + 226, INFO_Y + 52, 56, 18)
                .tooltip(Tooltip.create(Component.translatable("gui.textstudio.font.editor.tip.copy")))
                .build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.textstudio.font.editor.copy_prefix"),
                button -> copyPrefix()
        ).bounds(INFO_X + 286, INFO_Y + 52, 44, 18)
                .tooltip(Tooltip.create(Component.translatable("gui.textstudio.font.editor.tip.copy_prefix")))
                .build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.textstudio.font.editor.copy_stop"),
                button -> copyStop()
        ).bounds(INFO_X + 334, INFO_Y + 52, 44, 18)
                .tooltip(Tooltip.create(Component.translatable("gui.textstudio.font.editor.tip.copy_stop")))
                .build());
    }

    @Override
    protected void renderCanvasBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiTheme.shadow(graphics, canvasWidth, canvasHeight);
        GuiTheme.panel(graphics, LIST_X, LIST_Y, LIST_W, LIST_H);
        GuiTheme.panel(graphics, INFO_X, INFO_Y, INFO_W, INFO_H);
    }

    @Override
    protected void renderCanvasForeground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawString(font, Component.translatable("gui.textstudio.font.guide.presets"), LIST_X + 9, LIST_Y + 8, 0xFFFFFF, false);
        renderPresetList(graphics, mouseX, mouseY);

        graphics.drawString(font, Component.translatable("gui.textstudio.font.guide.quick_use"), INFO_X + 12, INFO_Y + 10, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("gui.textstudio.font.guide.step1"), INFO_X + 12, INFO_Y + 25, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("gui.textstudio.font.guide.step2"), INFO_X + 12, INFO_Y + 37, 0xFFFFFF, false);

        graphics.drawString(
                font,
                Component.translatable("gui.textstudio.font.guide.selected", selectedPreset + 1, presetName(selectedPreset)),
                INFO_X + 12,
                INFO_Y + 78,
                0xFFFFFF,
                false
        );

        renderPreview(graphics, mouseX, mouseY);

        drawRightAlignedNote(graphics, "gui.textstudio.font.guide.note1", INFO_Y + 286);
        drawRightAlignedNote(graphics, "gui.textstudio.font.guide.note2", INFO_Y + 300);
        drawRightAlignedNote(graphics, "gui.textstudio.font.guide.note3", INFO_Y + 314);
        drawRightAlignedNote(graphics, "gui.textstudio.font.guide.note4", INFO_Y + 328);
    }

    private Component presetName(int index) {
        String key = "gui.textstudio.font.preset." + (index + 1);
        if (I18n.exists(key)) {
            return Component.translatable(key);
        }
        return Component.translatable("gui.textstudio.font.editor.preset", index + 1);
    }

    private void renderPresetList(GuiGraphics graphics, int mouseX, int mouseY) {
        int y0 = LIST_Y + 28;
        int listHeight = VISIBLE_ROWS * ROW_H;
        int maxScroll = maxScroll();
        double visualScroll = listScrollSmoothing.follow(scroll, maxScroll);
        int start = Math.max(0, Math.min((int) Math.floor(visualScroll), maxScroll));
        int shift = (int) Math.round((visualScroll - start) * ROW_H);
        int count = Math.min(VISIBLE_ROWS + 2, Math.max(0, AuthorConfig.EFFECTS.size() - start));
        for (int row = 0; row < count; row++) {
            int index = start + row;
            int y = y0 + row * ROW_H - shift;
            if (y + ROW_H <= y0 || y >= y0 + listHeight) continue;
            int rowX = LIST_X + 6;
            int rowW = LIST_W - 18;
            int rowH = ROW_H - 1;
            boolean hovered = GuiTheme.hovering(mouseX, mouseY, rowX, y, rowW, rowH);
            if (index == selectedPreset) {
                graphics.fill(rowX, y, rowX + rowW, y + rowH, 0x66555555);
                graphics.renderOutline(rowX, y, rowW, rowH, PRESET_SELECTED_OUTLINE);
            } else if (hovered) {
                graphics.fill(rowX, y, rowX + rowW, y + rowH, 0x33444444);
                graphics.renderOutline(rowX, y, rowW, rowH, PRESET_HOVER_OUTLINE);
            }
            graphics.drawString(
                    font,
                    Component.literal((index + 1) + ". ").append(presetName(index)),
                    LIST_X + 10,
                    y + 4,
                    0xFFFFFF,
                    false
            );
        }

        if (maxScroll > 0) {
            int thumbHeight = Scroll.calculateThumbHeight(
                    listHeight,
                    VISIBLE_ROWS,
                    AuthorConfig.EFFECTS.size(),
                    18
            );
            GuiTheme.scrollbar(
                    graphics,
                    mouseX,
                    mouseY,
                    LIST_SCROLLBAR_X,
                    y0,
                    LIST_SCROLLBAR_W,
                    listHeight,
                    thumbHeight,
                    maxScroll,
                    visualScroll,
                    listScrollbarDragging
            );
        }
    }

    private void renderPreview(GuiGraphics graphics, int mouseX, int mouseY) {
        List<FormattedCharSequence> lines = getPreviewLines();
        int visibleLines = previewVisibleLines();
        int maxScroll = Math.max(0, lines.size() - visibleLines);
        previewScroll = Mth.clamp(previewScroll, 0, maxScroll);

        enableCanvasScissor(
                graphics,
                PREVIEW_CONTENT_X,
                PREVIEW_CONTENT_Y,
                PREVIEW_CONTENT_X + PREVIEW_CONTENT_W,
                PREVIEW_CONTENT_Y + PREVIEW_CONTENT_H
        );
        graphics.pose().pushPose();
        graphics.pose().translate(PREVIEW_CONTENT_X + 4, PREVIEW_CONTENT_Y + 4, 0.0f);
        graphics.pose().scale(PREVIEW_SCALE, PREVIEW_SCALE, 1.0f);
        double visualPreviewScroll = previewScrollSmoothing.follow(previewScroll, maxScroll);
        int previewStart = Math.max(0, Math.min((int) Math.floor(visualPreviewScroll), maxScroll));
        int previewShift = (int) Math.round((visualPreviewScroll - previewStart) * font.lineHeight);
        int end = Math.min(lines.size(), previewStart + visibleLines + 2);
        for (int i = previewStart; i < end; i++) {
            int lineY = (i - previewStart) * font.lineHeight - previewShift;
            if (lineY + font.lineHeight <= 0 || lineY >= PREVIEW_CONTENT_H / PREVIEW_SCALE) continue;
            graphics.drawString(font, lines.get(i), 0, lineY, 0xFFFFFFFF, true);
        }
        graphics.pose().popPose();
        graphics.disableScissor();

        if (maxScroll > 0) {
            int thumbHeight = Scroll.calculateThumbHeight(
                    PREVIEW_CONTENT_H,
                    visibleLines,
                    lines.size(),
                    18
            );
            GuiTheme.scrollbar(
                    graphics,
                    mouseX,
                    mouseY,
                    PREVIEW_SCROLLBAR_X,
                    PREVIEW_CONTENT_Y,
                    PREVIEW_SCROLLBAR_W,
                    PREVIEW_CONTENT_H,
                    thumbHeight,
                    maxScroll,
                    visualPreviewScroll,
                    previewScrollbarDragging
            );
        }
    }

    private List<FormattedCharSequence> getPreviewLines() {
        if (AuthorConfig.EFFECTS.isEmpty()) {
            return List.of(Component.literal(" ").getVisualOrderText());
        }

        AuthorConfig.EffectSettings effect = AuthorConfig.EFFECTS.get(selectedPreset);
        String text = previewText.isEmpty() ? " " : previewText;
        if (previewLinesText != null
                && previewLinesText.equals(text)
                && previewLinesPreset == selectedPreset
                && previewLinesEffect == effect) {
            return previewLines;
        }

        Style style = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF));
        IStyle.TextEffectStyleData data = new IStyle.TextEffectStyleData(
                selectedPreset + 1,
                false,
                false,
                false,
                false,
                false
        );
        data.customConfig = effect;
        if (style instanceof IStyle effectStyle) {
            effectStyle.textstudio_font$setStyleData(data);
        }

        MutableComponent preview = Component.literal(text).setStyle(style);
        int wrapWidth = Math.max(20, (int) ((PREVIEW_CONTENT_W - 8) / PREVIEW_SCALE));
        previewLines = font.split(preview, wrapWidth);
        if (previewLines.isEmpty()) {
            previewLines = List.of(Component.literal(" ").getVisualOrderText());
        }
        previewLinesText = text;
        previewLinesPreset = selectedPreset;
        previewLinesEffect = effect;
        return previewLines;
    }

    private void invalidatePreviewLines() {
        previewLinesText = null;
        previewLinesPreset = -1;
        previewLinesEffect = null;
        previewLines = List.of();
    }

    private int previewVisibleLines() {
        return Math.max(1, (int) (PREVIEW_CONTENT_H / (font.lineHeight * PREVIEW_SCALE)));
    }

    private int previewMaxScroll() {
        return Math.max(0, getPreviewLines().size() - previewVisibleLines());
    }

    private void copyCurrent() {
        if (AuthorConfig.EFFECTS.isEmpty()) {
            return;
        }
        Minecraft.getInstance().keyboardHandler.setClipboard(
                CompactTagCodec.encodePreset(selectedPreset + 1, previewText)
        );
        showCopyToast();
    }

    private void copyPrefix() {
        if (AuthorConfig.EFFECTS.isEmpty()) {
            return;
        }
        Minecraft.getInstance().keyboardHandler.setClipboard(
                CompactTagCodec.encodePresetPrefix(selectedPreset + 1)
        );
        showCopyToast();
    }

    private void copyStop() {
        Minecraft.getInstance().keyboardHandler.setClipboard(CompactTagCodec.encodeReset());
        showCopyToast();
    }

    private void showCopyToast() {
        GuiOverlay.toast(
                "textstudio_copy_success",
                Component.translatable("msg.textstudio.font.copy.success")
        );
    }

    private void drawRightAlignedNote(GuiGraphics graphics, String key, int y) {
        Component note = Component.translatable(key);
        int x = INFO_X + INFO_W - 12 - font.width(note);
        graphics.drawString(font, note, Math.max(INFO_X + 12, x), y, 0xFFFFFF, false);
    }

    @Override
    protected boolean canvasMouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int maxListScroll = maxScroll();
            int listY = LIST_Y + 28;
            int listHeight = VISIBLE_ROWS * ROW_H;
            if (maxListScroll > 0 && GuiTheme.hovering(
                    mouseX,
                    mouseY,
                    LIST_SCROLLBAR_X - 2,
                    listY,
                    LIST_SCROLLBAR_W + 4,
                    listHeight
            )) {
                listScrollbarDragging = true;
                int thumbHeight = Scroll.calculateThumbHeight(
                        listHeight,
                        VISIBLE_ROWS,
                        AuthorConfig.EFFECTS.size(),
                        18
                );
                scroll = Scroll.calculateScrollOffset(
                        mouseY,
                        listY,
                        listHeight,
                        thumbHeight,
                        maxListScroll
                );
                listScrollSmoothing.snap(scroll, maxListScroll);
                return true;
            }

            int maxPreviewScroll = previewMaxScroll();
            if (maxPreviewScroll > 0 && GuiTheme.hovering(
                    mouseX,
                    mouseY,
                    PREVIEW_SCROLLBAR_X - 2,
                    PREVIEW_CONTENT_Y,
                    PREVIEW_SCROLLBAR_W + 4,
                    PREVIEW_CONTENT_H
            )) {
                previewScrollbarDragging = true;
                int thumbHeight = Scroll.calculateThumbHeight(
                        PREVIEW_CONTENT_H,
                        previewVisibleLines(),
                        getPreviewLines().size(),
                        18
                );
                previewScroll = Scroll.calculateScrollOffset(
                        mouseY,
                        PREVIEW_CONTENT_Y,
                        PREVIEW_CONTENT_H,
                        thumbHeight,
                        maxPreviewScroll
                );
                previewScrollSmoothing.snap(previewScroll, maxPreviewScroll);
                return true;
            }

            int index = presetIndexAt(mouseX, mouseY);
            if (index >= 0) {
                selectedPreset = index;
                previewScroll = 0;
                invalidatePreviewLines();
                return true;
            }
        }
        return super.canvasMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean canvasMouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && (listScrollbarDragging || previewScrollbarDragging)) {
            listScrollbarDragging = false;
            previewScrollbarDragging = false;
            return true;
        }
        return super.canvasMouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected boolean canvasMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && listScrollbarDragging) {
            int maxListScroll = maxScroll();
            if (maxListScroll > 0) {
                int listY = LIST_Y + 28;
                int listHeight = VISIBLE_ROWS * ROW_H;
                int thumbHeight = Scroll.calculateThumbHeight(
                        listHeight,
                        VISIBLE_ROWS,
                        AuthorConfig.EFFECTS.size(),
                        18
                );
                scroll = Scroll.calculateScrollOffset(
                        mouseY,
                        listY,
                        listHeight,
                        thumbHeight,
                        maxListScroll
                );
                listScrollSmoothing.snap(scroll, maxListScroll);
            }
            return true;
        }
        if (button == 0 && previewScrollbarDragging) {
            int maxPreviewScroll = previewMaxScroll();
            if (maxPreviewScroll > 0) {
                int thumbHeight = Scroll.calculateThumbHeight(
                        PREVIEW_CONTENT_H,
                        previewVisibleLines(),
                        getPreviewLines().size(),
                        18
                );
                previewScroll = Scroll.calculateScrollOffset(
                        mouseY,
                        PREVIEW_CONTENT_Y,
                        PREVIEW_CONTENT_H,
                        thumbHeight,
                        maxPreviewScroll
                );
                previewScrollSmoothing.snap(previewScroll, maxPreviewScroll);
            }
            return true;
        }
        return super.canvasMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected boolean canvasMouseScrolled(double mouseX, double mouseY, double delta) {
        if (GuiTheme.hovering(mouseX, mouseY, PREVIEW_CONTENT_X, PREVIEW_CONTENT_Y, PREVIEW_CONTENT_W, PREVIEW_CONTENT_H)) {
            previewScroll = previewScrollSmoothing.wheel(previewScroll, delta, 1.0D / 3.0D, previewMaxScroll());
            return true;
        }
        if (GuiTheme.hovering(mouseX, mouseY, LIST_X, LIST_Y, LIST_W, LIST_H)) {
            scroll = listScrollSmoothing.wheel(scroll, delta, 1.0D / 3.0D, maxScroll());
            return true;
        }
        return super.canvasMouseScrolled(mouseX, mouseY, delta);
    }

    private int presetIndexAt(double mouseX, double mouseY) {
        int y0 = LIST_Y + 28;
        int h = VISIBLE_ROWS * ROW_H;
        if (!GuiTheme.hovering(mouseX, mouseY, LIST_X + 6, y0, LIST_W - 18, h)) {
            return -1;
        }
        double visualScroll = listScrollSmoothing.follow(scroll, maxScroll());
        int index = (int) Math.floor((mouseY - y0) / ROW_H + visualScroll);
        return index >= 0 && index < AuthorConfig.EFFECTS.size() ? index : -1;
    }

    private int maxScroll() {
        return Math.max(0, AuthorConfig.EFFECTS.size() - VISIBLE_ROWS);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
