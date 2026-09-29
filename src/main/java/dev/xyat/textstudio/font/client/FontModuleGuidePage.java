package dev.xyat.textstudio.font.client;

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
import dev.xyat.kineticcore.api.client.gui.widget.list.KineticRowList;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.textstudio.font.api.IStyle;
import dev.xyat.textstudio.font.client.parser.CompactTagCodec;
import dev.xyat.textstudio.font.config.AuthorConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/** 字体效果快速指南 / Quick guide for the font effect presets. */
public final class FontModuleGuidePage extends KineticPage {
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
    private static final float PREVIEW_SCALE = 1.65f;
    private static final int PREVIEW_CONTENT_X = INFO_X + 12;
    private static final int PREVIEW_CONTENT_Y = INFO_Y + 94;
    private static final int PREVIEW_CONTENT_W = INFO_W - 30;
    private static final int PREVIEW_CONTENT_H = 176;
    private static final int PREVIEW_SCROLLBAR_X = INFO_X + INFO_W - 8;
    private static final int PREVIEW_SCROLLBAR_W = 4;

    private int selectedPreset;
    private int listScrollOffset;
    private final KineticScrollController previewScroll = new KineticScrollController();
    private boolean previewScrollbarDragging;
    private String previewText;
    private String previewLinesText;
    private int previewLinesPreset = -1;
    private AuthorConfig.EffectSettings previewLinesEffect;
    private List<FormattedCharSequence> previewLines = List.of();
    private PresetList presetList;

    public FontModuleGuidePage() {
        super(KineticI18n.translatable("gui.textstudio.font.guide.title"));
        setPausesGame(false);
        this.previewText = KineticI18n.string("gui.textstudio.font.editor.preview.default");
    }

    @Override
    protected void build(KineticUi ui) {
        selectedPreset = Mth.clamp(selectedPreset, 0, Math.max(0, AuthorConfig.EFFECTS.size() - 1));
        if (presetList != null) listScrollOffset = presetList.scrollOffset();

        ui.button(INFO_X + INFO_W - 72, INFO_Y + 6, 64)
                .text(KineticI18n.translatable("gui.textstudio.font.guide.back"))
                .onClick(this::close)
                .build();

        ui.textField(INFO_X + 12, INFO_Y + 52, 210)
                .label(KineticI18n.translatable("gui.textstudio.font.editor.preview.input"))
                .placeholder(KineticI18n.translatable("gui.textstudio.font.editor.preview.hint"))
                .tooltip(KineticI18n.translatable("gui.textstudio.font.editor.tip.preview_input"))
                .maxLength(4096)
                .value(previewText)
                .onChange(value -> {
                    previewText = value;
                    previewScroll.scrollTo(0);
                    invalidatePreviewLines();
                })
                .firstShownTextAsDefault().build();
        ui.button(INFO_X + 226, INFO_Y + 52, 56)
                .text(KineticI18n.translatable("gui.textstudio.font.editor.copy"))
                .tooltip(KineticI18n.translatable("gui.textstudio.font.editor.tip.copy"))
                .onClick(this::copyCurrent)
                .build();
        ui.button(INFO_X + 286, INFO_Y + 52, 44)
                .text(KineticI18n.translatable("gui.textstudio.font.editor.copy_prefix"))
                .tooltip(KineticI18n.translatable("gui.textstudio.font.editor.tip.copy_prefix"))
                .onClick(this::copyPrefix)
                .build();
        ui.button(INFO_X + 334, INFO_Y + 52, 44)
                .text(KineticI18n.translatable("gui.textstudio.font.editor.copy_stop"))
                .tooltip(KineticI18n.translatable("gui.textstudio.font.editor.tip.copy_stop"))
                .onClick(this::copyStop)
                .build();

        presetList = ui.add(new PresetList());
        List<Integer> presets = new ArrayList<>();
        for (int i = 0; i < AuthorConfig.EFFECTS.size(); i++) presets.add(i);
        presetList.setItems(presets);
        presetList.setSelectedIndex(selectedPreset);
        presetList.setScrollOffset(listScrollOffset);
        presetList.setOnSelect(index -> {
            if (index < 0) return;
            selectedPreset = index;
            previewScroll.scrollTo(0);
            invalidatePreviewLines();
        });
    }

    @Override
    protected void renderBackground(KineticGraphics graphics, int mouseX, int mouseY, float partialTick) {
        KineticTheme.shadow(graphics, width(), height());
        KineticTheme.panel(graphics, LIST_X, LIST_Y, LIST_W, LIST_H);
        KineticTheme.panel(graphics, INFO_X, INFO_Y, INFO_W, INFO_H);
    }

    @Override
    protected void renderForeground(KineticGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.text(KineticI18n.translatable("gui.textstudio.font.guide.presets"), LIST_X + 9, LIST_Y + 8, 0xFFFFFF, false);

        graphics.text(KineticI18n.translatable("gui.textstudio.font.guide.quick_use"), INFO_X + 12, INFO_Y + 10, 0xFFFFFF, false);
        graphics.text(KineticI18n.translatable("gui.textstudio.font.guide.step1"), INFO_X + 12, INFO_Y + 25, 0xFFFFFF, false);
        graphics.text(KineticI18n.translatable("gui.textstudio.font.guide.step2"), INFO_X + 12, INFO_Y + 37, 0xFFFFFF, false);
        graphics.text(
                KineticI18n.translatable("gui.textstudio.font.guide.selected", selectedPreset + 1, presetName(selectedPreset)),
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

    private static Component presetName(int index) {
        String key = "gui.textstudio.font.preset." + (index + 1);
        if (KineticI18n.hasTranslation(key)) {
            return KineticI18n.translatable(key);
        }
        return KineticI18n.translatable("gui.textstudio.font.editor.preset", index + 1);
    }

    private void renderPreview(KineticGraphics graphics, int mouseX, int mouseY) {
        List<FormattedCharSequence> lines = getPreviewLines();
        int visibleLines = previewVisibleLines();
        previewScroll.update(lines.size(), visibleLines);
        int lineHeight = graphics.lineHeight();

        graphics.clipped(
                PREVIEW_CONTENT_X + 1,
                PREVIEW_CONTENT_Y + 1,
                PREVIEW_CONTENT_X + PREVIEW_CONTENT_W - 1,
                PREVIEW_CONTENT_Y + PREVIEW_CONTENT_H - 1,
                () -> graphics.isolated(() -> {
                    graphics.translate(PREVIEW_CONTENT_X + 4, PREVIEW_CONTENT_Y + 4);
                    graphics.scale(PREVIEW_SCALE, PREVIEW_SCALE);
                    int previewStart = previewScroll.smoothIndexOffset();
                    int previewShift = previewScroll.visualShift(lineHeight);
                    int end = Math.min(lines.size(), previewStart + visibleLines + 2);
                    for (int i = previewStart; i < end; i++) {
                        int lineY = (i - previewStart) * lineHeight - previewShift;
                        if (lineY + lineHeight <= 0 || lineY >= PREVIEW_CONTENT_H / PREVIEW_SCALE) continue;
                        graphics.text(lines.get(i), 0, lineY, 0xFFFFFFFF, true);
                    }
                })
        );

        if (previewScroll.canScroll()) {
            previewScroll.render(graphics, mouseX, mouseY, PREVIEW_SCROLLBAR_X, PREVIEW_CONTENT_Y,
                    PREVIEW_SCROLLBAR_W, PREVIEW_CONTENT_H, 18);
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
        previewLines = KineticText.wrap(preview, wrapWidth);
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

    private static int previewVisibleLines() {
        return Math.max(1, (int) (PREVIEW_CONTENT_H / (KineticText.lineHeight() * PREVIEW_SCALE)));
    }

    private void copyCurrent() {
        if (AuthorConfig.EFFECTS.isEmpty()) {
            return;
        }
        KineticClientRuntime.setClipboard(CompactTagCodec.encodePreset(selectedPreset + 1, previewText));
        showCopyToast();
    }

    private void copyPrefix() {
        if (AuthorConfig.EFFECTS.isEmpty()) {
            return;
        }
        KineticClientRuntime.setClipboard(CompactTagCodec.encodePresetPrefix(selectedPreset + 1));
        showCopyToast();
    }

    private void copyStop() {
        KineticClientRuntime.setClipboard(CompactTagCodec.encodeReset());
        showCopyToast();
    }

    private static void showCopyToast() {
        KineticOverlays.toast("textstudio_copy_success", KineticI18n.translatable("msg.textstudio.font.copy.success"));
    }

    private void drawRightAlignedNote(KineticGraphics graphics, String key, int y) {
        Component note = KineticI18n.translatable(key);
        int x = INFO_X + INFO_W - 12 - graphics.textWidth(note);
        graphics.text(note, Math.max(INFO_X + 12, x), y, 0xFFFFFF, false);
    }

    @Override
    protected boolean onMouseClick(MouseInput input) {
        if (previewScroll.canScroll() && previewScroll.beginDrag(input.x(), input.y(), input.button(),
                PREVIEW_SCROLLBAR_X - 2, PREVIEW_CONTENT_Y, PREVIEW_SCROLLBAR_W + 4, PREVIEW_CONTENT_H, 18)) {
            previewScrollbarDragging = true;
            return true;
        }
        return false;
    }

    @Override
    protected boolean onMouseRelease(MouseInput input) {
        if (previewScrollbarDragging) {
            previewScrollbarDragging = false;
            previewScroll.release(input.button());
            return true;
        }
        return false;
    }

    @Override
    protected boolean onMouseDrag(MouseDragInput input) {
        if (previewScrollbarDragging) {
            previewScroll.drag(input.y(), PREVIEW_CONTENT_Y, PREVIEW_CONTENT_H, 18);
            return true;
        }
        return false;
    }

    @Override
    protected boolean onMouseScroll(ScrollInput input) {
        if (input.inside(PREVIEW_CONTENT_X, PREVIEW_CONTENT_Y, PREVIEW_CONTENT_W, PREVIEW_CONTENT_H)) {
            previewScroll.scroll(input.deltaY());
            return true;
        }
        return false;
    }

    /** 预设列表：只绘制行内容，滚动与滚动条由核心处理 / Preset list; scrolling and the scrollbar are core-owned. */
    private final class PresetList extends KineticRowList<Integer> {
        PresetList() {
            super(LIST_X + 6, LIST_Y + 28, LIST_W - 10, VISIBLE_ROWS * ROW_H, ROW_H);
        }

        @Override
        protected void renderRowBackground(KineticGraphics graphics, int index, int x, int y, int width, int height,
                                           boolean hovered, boolean selected) {
            KineticTheme.stateSurface(graphics, x, y + 1, width, height - 2, KineticTheme.Surface.PANEL_ALT,
                    selected, hovered, false);
        }

        @Override
        protected void renderRow(KineticGraphics graphics, Integer index, int rowIndex, int x, int y, int width,
                                 int height, boolean hovered, boolean selected) {
            graphics.text(Component.literal((index + 1) + ". ").append(presetName(index)), x + 4, y + 4, 0xFFFFFF, false);
        }
    }
}
