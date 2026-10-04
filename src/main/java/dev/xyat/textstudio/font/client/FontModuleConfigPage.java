package dev.xyat.textstudio.font.client;

import dev.xyat.kineticcore.api.client.gui.input.MouseDragInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.input.ScrollInput;
import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.scroll.KineticScrollController;
import dev.xyat.kineticcore.api.client.gui.selector.KineticSelectors;
import dev.xyat.kineticcore.api.client.gui.text.KineticText;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.ui.NumberType;
import dev.xyat.kineticcore.api.client.gui.widget.KineticButton;
import dev.xyat.kineticcore.api.client.gui.widget.KineticNumberField;
import dev.xyat.kineticcore.api.client.gui.widget.list.KineticRowList;
import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.textstudio.font.api.IStyle;
import dev.xyat.textstudio.font.config.AuthorConfig;
import dev.xyat.textstudio.font.client.parser.CompactTagCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public final class FontModuleConfigPage extends KineticPage {
    private static final int CANVAS_W = 640;
    private static final int CANVAS_H = 360;

    private static final int LIST_X = 8;
    private static final int LIST_Y = 6;
    private static final int LIST_W = 120;
    private static final int LIST_H = 346;
    private static final int LIST_ROW_H = 18;
    private static final int PRESET_SELECTED_OUTLINE = 0xFFFFE600;
    private static final int PRESET_HOVER_OUTLINE = 0xFF55AAFF;

    private static final int PREVIEW_X = 134;
    private static final int PREVIEW_Y = 6;
    private static final int PREVIEW_W = 498;
    private static final int PREVIEW_H = 104;
    private static final float PREVIEW_SCALE = 1.45f;
    private static final int PREVIEW_CONTENT_X = PREVIEW_X + 12;
    private static final int PREVIEW_CONTENT_Y = PREVIEW_Y + 48;
    private static final int PREVIEW_CONTENT_W = PREVIEW_W - 30;
    private static final int PREVIEW_CONTENT_H = PREVIEW_H - 55;
    private static final int PREVIEW_SCROLLBAR_X = PREVIEW_X + PREVIEW_W - 8;
    private static final int PREVIEW_SCROLLBAR_W = 4;

    private static final int EDITOR_X = 134;
    private static final int EDITOR_Y = 116;
    private static final int EDITOR_W = 498;
    private static final int EDITOR_H = 236;

    private static final int FIELD_COL_1_X = 146;
    private static final int FIELD_COL_2_X = 307;
    private static final int FIELD_COL_3_X = 468;
    private static final int FIELD_CONTROL_OFFSET = 105;
    // Labels end 4px before their control.
    private static final int FIELD_LABEL_W = FIELD_CONTROL_OFFSET - 4;
    private static final int FIELD_CONTROL_W = 40;
    private static final int FIELD_H = 14;
    private static final int FIELD_ROW = 18;
    private static final int FIELD_START_Y = 151;

    private static final int CATEGORY_X = EDITOR_X + 5;
    private static final int CATEGORY_Y = EDITOR_Y + 6;
    private static final int CATEGORY_W = 178;
    private static final int CATEGORY_H = 18;
    private static final int CATEGORY_MENU_Y = CATEGORY_Y + CATEGORY_H + 2;
    private static final int CATEGORY_MENU_ROW_H = 20;
    private static final int CATEGORY_MENU_VISIBLE_ROWS = EditorTab.values().length;
    private static final int CATEGORY_MENU_H = CATEGORY_MENU_ROW_H * CATEGORY_MENU_VISIBLE_ROWS + 4;

    private static final int PALETTE_SWATCH_X = 307;
    private static final int PALETTE_SWATCH_Y = 226;
    private static final int PALETTE_SWATCH_CELL = 14;
    private static final int PALETTE_SWATCH_GAP = 3;
    private static final int PALETTE_SWATCH_COLS = 12;

    private static final int MAX_PALETTE_COLORS = 24;
    private static final int MAX_PRESETS = 255;
    private static final int CONTEXT_W = 112;
    private static final int CONTEXT_ROW_H = 18;

    private enum EditorTab {
        COLOR,
        MOTION,
        SPECIAL,
        GLYPH,
        VISUAL,
        PALETTE
    }

    // maxWidth is the room before the next control; longer (usually English) labels scroll instead of overlapping it.
    private record FieldLabel(Component component, int x, int y, int maxWidth) {
    }

    private record HoverTip(int x, int y, int w, int h, Component text) {
    }

    private final List<AuthorConfig.EffectSettings> draftEffects = new ArrayList<>();
    private final List<FieldLabel> fieldLabels = new ArrayList<>();
    private final List<HoverTip> hoverTips = new ArrayList<>();

    private EditorTab tab = EditorTab.COLOR;
    private boolean categoryMenuOpen;
    private int selectedPreset;
    private int presetScrollOffset;
    private boolean revealSelectedPreset;
    private PresetList presetList;
    private final KineticScrollController previewScroll = new KineticScrollController();
    private boolean previewScrollbarDragging;
    private String previewText;
    private String previewLinesText;
    private int previewLinesPreset = -1;
    private AuthorConfig.EffectSettings previewLinesEffect;
    private List<FormattedCharSequence> previewLines = List.of();


    private record FontDraftSnapshot(List<AuthorConfig.EffectSettings> effects, int selectedPreset) {
    }

    private FontDraftSnapshot captureFontDraftSnapshot() {
        List<AuthorConfig.EffectSettings> copies = new ArrayList<>(draftEffects.size());
        for (AuthorConfig.EffectSettings effect : draftEffects) {
            copies.add(effect.copy());
        }
        return new FontDraftSnapshot(copies, selectedPreset);
    }

    private void restoreFontDraftSnapshot(FontDraftSnapshot snapshot) {
        if (snapshot == null) return;
        draftEffects.clear();
        for (AuthorConfig.EffectSettings effect : snapshot.effects()) {
            draftEffects.add(effect.copy());
        }
        if (draftEffects.isEmpty()) draftEffects.add(new AuthorConfig.EffectSettings());
        selectedPreset = Mth.clamp(snapshot.selectedPreset(), 0, draftEffects.size() - 1);
        invalidatePreviewLines();
    }

    public static void register() {
        KTConfigApi.installConfigPage("textstudio", FontModuleConfigPage::new);
    }

    public FontModuleConfigPage() {
        super(KineticI18n.translatable("gui.textstudio.font.editor.title"));
        setPausesGame(false);
        this.previewText = KineticI18n.string("gui.textstudio.font.editor.preview.default");
        for (AuthorConfig.EffectSettings effect : AuthorConfig.EFFECTS) {
            draftEffects.add(effect.copy());
        }
        if (draftEffects.isEmpty()) {
            draftEffects.add(new AuthorConfig.EffectSettings());
        }
        configureStandaloneDraft(this::captureFontDraftSnapshot, this::restoreFontDraftSnapshot);
    }

    @Override
    protected void build(KineticUi ui) {
        fieldLabels.clear();
        hoverTips.clear();
        selectedPreset = Mth.clamp(selectedPreset, 0, draftEffects.size() - 1);
        if (presetList != null && presetScrollOffset != Integer.MAX_VALUE) presetScrollOffset = presetList.scrollOffset();

        addActionButton(
                KineticI18n.translatable("gui.textstudio.font.editor.cancel"),
                512,
                PREVIEW_Y + 4,
                56,
                18,
                this::close,
                KineticI18n.translatable("gui.textstudio.font.editor.tip.cancel")
        );
        addActionButton(
                KineticI18n.translatable("gui.textstudio.font.editor.save"),
                572,
                PREVIEW_Y + 4,
                56,
                18,
                this::save,
                KineticI18n.translatable("gui.textstudio.font.editor.tip.save")
        );

        buildPreviewInput();
        registerTip(
                PREVIEW_X + 12,
                PREVIEW_Y + 23,
                302,
                18,
                KineticI18n.translatable("gui.textstudio.font.editor.tip.preview_input")
        );

        addActionButton(
                KineticI18n.translatable("gui.textstudio.font.editor.copy"),
                452,
                PREVIEW_Y + 26,
                56,
                18,
                this::copyPreviewText,
                KineticI18n.translatable("gui.textstudio.font.editor.tip.copy")
        );
        addActionButton(
                KineticI18n.translatable("gui.textstudio.font.editor.copy_prefix"),
                512,
                PREVIEW_Y + 26,
                56,
                18,
                this::copyEffectPrefix,
                KineticI18n.translatable("gui.textstudio.font.editor.tip.copy_prefix")
        );
        addActionButton(
                KineticI18n.translatable("gui.textstudio.font.editor.copy_stop"),
                572,
                PREVIEW_Y + 26,
                56,
                18,
                this::copyEffectStop,
                KineticI18n.translatable("gui.textstudio.font.editor.tip.copy_stop")
        );

        addActionButton(
                KineticI18n.translatable("gui.textstudio.font.editor.category.current", tabLabel(tab)),
                CATEGORY_X,
                CATEGORY_Y,
                CATEGORY_W,
                CATEGORY_H,
                this::toggleCategoryMenu,
                KineticI18n.translatable("gui.textstudio.font.editor.tip.category_selector")
        );

        if (categoryMenuOpen) {
            buildCategoryMenuButtons();
        } else {
            buildCurrentTab();
        }

        KineticButton addPresetButton = addActionButton(
                KineticI18n.translatable("gui.textstudio.font.editor.preset.add"),
                LIST_X + 10,
                LIST_Y + LIST_H - 26,
                LIST_W - 20,
                18,
                this::addPreset,
                KineticI18n.translatable("gui.textstudio.font.editor.tip.preset_add")
        );
        addPresetButton.setEnabled(draftEffects.size() < MAX_PRESETS);

        presetList = ui.add(new PresetList());
        presetList.setItems(draftEffects);
        presetList.setSelectedIndex(selectedPreset);
        presetList.setScrollOffset(presetScrollOffset);
        if (revealSelectedPreset) presetList.scrollTo(selectedPreset);
        revealSelectedPreset = false;
        presetScrollOffset = presetList.scrollOffset();
    }

    private void buildPreviewInput() {
        ui().textField(PREVIEW_X + 12, PREVIEW_Y + 23, 302)
                .label(KineticI18n.translatable("gui.textstudio.font.editor.preview.input"))
                .placeholder(KineticI18n.translatable("gui.textstudio.font.editor.preview.hint"))
                .maxLength(4096)
                .value(previewText)
                .onChange(value -> {
                    previewText = value;
                    previewScroll.scrollTo(0);
                    invalidatePreviewLines();
                })
                .firstShownTextAsDefault().build();
    }

    private Component tabLabel(EditorTab value) {
        return switch (value) {
            case COLOR -> KineticI18n.translatable("gui.textstudio.font.editor.tab.color");
            case MOTION -> KineticI18n.translatable("gui.textstudio.font.editor.tab.motion");
            case SPECIAL -> KineticI18n.translatable("gui.textstudio.font.editor.tab.special");
            case GLYPH -> KineticI18n.translatable("gui.textstudio.font.editor.tab.glyph");
            case VISUAL -> KineticI18n.translatable("gui.textstudio.font.editor.tab.visual");
            case PALETTE -> KineticI18n.translatable("gui.textstudio.font.editor.tab.palette");
        };
    }

    private void toggleCategoryMenu() {
        categoryMenuOpen = !categoryMenuOpen;
        closeContextMenu();
        rebuild();
    }

    private void buildCategoryMenuButtons() {
        EditorTab[] values = EditorTab.values();
        for (int i = 0; i < values.length; i++) {
            EditorTab target = values[i];
            addActionButton(
                    tabLabel(target),
                    CATEGORY_X + 3,
                    CATEGORY_MENU_Y + 2 + i * CATEGORY_MENU_ROW_H,
                    CATEGORY_W - 6,
                    18,
                    () -> selectCategory(target),
                    KineticI18n.translatable("gui.textstudio.font.editor.tip.tab", tabLabel(target))
            );
        }
    }

    private void selectCategory(EditorTab next) {
        tab = next;
        categoryMenuOpen = false;
        closeContextMenu();
        rebuild();
    }

    private KineticButton addActionButton(Component text, int x, int y, int w, int h, Runnable action, Component tip) {
        return ui().button(x, y, w).text(text).tooltip(tip).compact().onClick(action).build();
    }

    private void registerTip(int x, int y, int w, int h, Component tip) {
        hoverTips.add(new HoverTip(x, y, w, h, tip));
    }

    private void buildCurrentTab() {
        switch (tab) {
            case COLOR -> buildColorTab();
            case MOTION -> buildMotionTab();
            case SPECIAL -> buildSpecialTab();
            case GLYPH -> buildGlyphTab();
            case VISUAL -> buildVisualTab();
            case PALETTE -> buildPaletteTab();
        }
    }

    private int fieldX(int column) {
        return switch (column) {
            case 1 -> FIELD_COL_2_X;
            case 2 -> FIELD_COL_3_X;
            default -> FIELD_COL_1_X;
        };
    }

    private int fieldY(int row) {
        return FIELD_START_Y + FIELD_ROW * row;
    }

    private void buildColorTab() {
        AuthorConfig.EffectSettings s = current();

        addToggle("cfg.textstudio.font.author.use_rainbow", fieldX(0), fieldY(0), () -> s.useRainbow, v -> s.useRainbow = v);
        addNumber("cfg.textstudio.font.author.rainbow_speed", fieldX(1), fieldY(0), () -> s.rainbowSpeed, v -> s.rainbowSpeed = v, 0.0, 100.0);
        addNumber("cfg.textstudio.font.author.rainbow_spread", fieldX(2), fieldY(0), () -> s.rainbowSpread, v -> s.rainbowSpread = v, 0.0, 10.0);
        addToggle("cfg.textstudio.font.author.pulse", fieldX(0), fieldY(1), () -> s.pulse, v -> s.pulse = v);
        addNumber("cfg.textstudio.font.author.pulse_base", fieldX(1), fieldY(1), () -> s.pulseBase, v -> s.pulseBase = v, 0.0, 10.0);
        addNumber("cfg.textstudio.font.author.pulse_amp", fieldX(2), fieldY(1), () -> s.pulseAmp, v -> s.pulseAmp = v, 0.0, 20.0);
        addNumber("cfg.textstudio.font.author.pulse_speed", fieldX(0), fieldY(2), () -> s.pulseSpeed, v -> s.pulseSpeed = v, 0.0, 100.0);
        addToggle("cfg.textstudio.font.author.fade", fieldX(1), fieldY(2), () -> s.fade, v -> s.fade = v);
        addNumber("cfg.textstudio.font.author.fade_min", fieldX(2), fieldY(2), () -> s.fadeMin, v -> s.fadeMin = v, 0.0, 1.0);
        addNumber("cfg.textstudio.font.author.fade_speed", fieldX(0), fieldY(3), () -> s.fadeSpeed, v -> s.fadeSpeed = v, 0.0, 100.0);
        addToggle("cfg.textstudio.font.author.neon_flicker", fieldX(1), fieldY(3), () -> s.neonFlicker, v -> s.neonFlicker = v);
        addNumber("cfg.textstudio.font.author.neon_flicker_speed", fieldX(2), fieldY(3), () -> s.neonFlickerSpeed, v -> s.neonFlickerSpeed = v, 0.0, 100.0);
        addToggle("cfg.textstudio.font.author.chromatic", fieldX(0), fieldY(4), () -> s.chromatic, v -> s.chromatic = v);
        addNumber("cfg.textstudio.font.author.chromatic_offset", fieldX(1), fieldY(4), () -> s.chromaticOffset, v -> s.chromaticOffset = v, 0.0, 30.0);
        addNumber("cfg.textstudio.font.author.chromatic_alpha", fieldX(2), fieldY(4), () -> s.chromaticAlpha, v -> s.chromaticAlpha = v, 0.0, 1.0);
        addToggle("cfg.textstudio.font.author.chromatic_pulse", fieldX(0), fieldY(5), () -> s.chromaticPulse, v -> s.chromaticPulse = v);
        addToggle("cfg.textstudio.font.author.shimmer", fieldX(1), fieldY(5), () -> s.shimmer, v -> s.shimmer = v);
        addNumber("cfg.textstudio.font.author.shimmer_speed", fieldX(2), fieldY(5), () -> s.shimmerSpeed, v -> s.shimmerSpeed = v, 0.05, 20.0);
        addNumber("cfg.textstudio.font.author.shimmer_width", fieldX(0), fieldY(6), () -> s.shimmerWidth, v -> s.shimmerWidth = v, 0.25, 10.0);
        addNumber("cfg.textstudio.font.author.shimmer_strength", fieldX(1), fieldY(6), () -> s.shimmerStrength, v -> s.shimmerStrength = v, 0.0, 1.0);
        addToggle("cfg.textstudio.font.author.sparkle", fieldX(2), fieldY(6), () -> s.sparkle, v -> s.sparkle = v);
        addNumber("cfg.textstudio.font.author.sparkle_speed", fieldX(0), fieldY(7), () -> s.sparkleSpeed, v -> s.sparkleSpeed = v, 0.05, 20.0);
        addNumber("cfg.textstudio.font.author.sparkle_chance", fieldX(1), fieldY(7), () -> s.sparkleChance, v -> s.sparkleChance = v, 0.0, 1.0);
        addNumber("cfg.textstudio.font.author.sparkle_strength", fieldX(2), fieldY(7), () -> s.sparkleStrength, v -> s.sparkleStrength = v, 0.0, 1.0);
    }

    private void buildMotionTab() {
        AuthorConfig.EffectSettings s = current();

        addToggle("cfg.textstudio.font.author.wave", fieldX(0), fieldY(0), () -> s.wave, v -> s.wave = v);
        addNumber("cfg.textstudio.font.author.wave_amp", fieldX(1), fieldY(0), () -> s.waveAmp, v -> s.waveAmp = v, 0.0, 100.0);
        addNumber("cfg.textstudio.font.author.wave_speed", fieldX(2), fieldY(0), () -> s.waveSpeed, v -> s.waveSpeed = v, 0.0, 100.0);
        addToggle("cfg.textstudio.font.author.bounce", fieldX(0), fieldY(1), () -> s.bounce, v -> s.bounce = v);
        addNumber("cfg.textstudio.font.author.bounce_amp", fieldX(1), fieldY(1), () -> s.bounceAmp, v -> s.bounceAmp = v, 0.0, 100.0);
        addNumber("cfg.textstudio.font.author.bounce_speed", fieldX(2), fieldY(1), () -> s.bounceSpeed, v -> s.bounceSpeed = v, 0.0, 100.0);
        addToggle("cfg.textstudio.font.author.shake", fieldX(0), fieldY(2), () -> s.shake, v -> s.shake = v);
        addNumber("cfg.textstudio.font.author.shake_amp", fieldX(1), fieldY(2), () -> s.shakeAmp, v -> s.shakeAmp = v, 0.0, 100.0);
        addNumber("cfg.textstudio.font.author.shake_speed", fieldX(2), fieldY(2), () -> s.shakeSpeed, v -> s.shakeSpeed = v, 0.0, 100.0);
        addToggle("cfg.textstudio.font.author.swing", fieldX(0), fieldY(3), () -> s.swing, v -> s.swing = v);
        addNumber("cfg.textstudio.font.author.swing_amp", fieldX(1), fieldY(3), () -> s.swingAmp, v -> s.swingAmp = v, 0.0, 100.0);
        addNumber("cfg.textstudio.font.author.swing_speed", fieldX(2), fieldY(3), () -> s.swingSpeed, v -> s.swingSpeed = v, 0.0, 100.0);
        addToggle("cfg.textstudio.font.author.wiggle", fieldX(0), fieldY(4), () -> s.wiggle, v -> s.wiggle = v);
        addNumber("cfg.textstudio.font.author.wiggle_amp", fieldX(1), fieldY(4), () -> s.wiggleAmp, v -> s.wiggleAmp = v, 0.0, 100.0);
        addNumber("cfg.textstudio.font.author.wiggle_speed", fieldX(2), fieldY(4), () -> s.wiggleSpeed, v -> s.wiggleSpeed = v, 0.0, 100.0);
        addToggle("cfg.textstudio.font.author.turb", fieldX(0), fieldY(5), () -> s.turb, v -> s.turb = v);
        addNumber("cfg.textstudio.font.author.turb_amp", fieldX(1), fieldY(5), () -> s.turbAmp, v -> s.turbAmp = v, 0.0, 100.0);
        addNumber("cfg.textstudio.font.author.turb_speed", fieldX(2), fieldY(5), () -> s.turbSpeed, v -> s.turbSpeed = v, 0.0, 100.0);
        addToggle("cfg.textstudio.font.author.pend", fieldX(0), fieldY(6), () -> s.pend, v -> s.pend = v);
        addNumber("cfg.textstudio.font.author.pend_amp", fieldX(1), fieldY(6), () -> s.pendAmp, v -> s.pendAmp = v, 0.0, 100.0);
        addNumber("cfg.textstudio.font.author.pend_speed", fieldX(2), fieldY(6), () -> s.pendSpeed, v -> s.pendSpeed = v, 0.0, 100.0);
        addToggle("cfg.textstudio.font.author.orbit", fieldX(0), fieldY(7), () -> s.orbit, v -> s.orbit = v);
        addNumber("cfg.textstudio.font.author.orbit_amp", fieldX(1), fieldY(7), () -> s.orbitAmp, v -> s.orbitAmp = v, 0.0, 20.0);
        addNumber("cfg.textstudio.font.author.orbit_speed", fieldX(2), fieldY(7), () -> s.orbitSpeed, v -> s.orbitSpeed = v, 0.05, 20.0);
        addToggle("cfg.textstudio.font.author.ripple", fieldX(0), fieldY(8), () -> s.ripple, v -> s.ripple = v);
        addNumber("cfg.textstudio.font.author.ripple_amp", fieldX(1), fieldY(8), () -> s.rippleAmp, v -> s.rippleAmp = v, 0.0, 20.0);
        addNumber("cfg.textstudio.font.author.ripple_speed", fieldX(2), fieldY(8), () -> s.rippleSpeed, v -> s.rippleSpeed = v, 0.05, 20.0);
        addToggle("cfg.textstudio.font.author.drift", fieldX(0), fieldY(9), () -> s.drift, v -> s.drift = v);
        addNumber("cfg.textstudio.font.author.drift_amp", fieldX(1), fieldY(9), () -> s.driftAmp, v -> s.driftAmp = v, 0.0, 20.0);
        addNumber("cfg.textstudio.font.author.drift_speed", fieldX(2), fieldY(9), () -> s.driftSpeed, v -> s.driftSpeed = v, 0.05, 20.0);
    }

    private void buildSpecialTab() {
        AuthorConfig.EffectSettings s = current();

        addToggle("cfg.textstudio.font.author.glitch", fieldX(0), fieldY(0), () -> s.glitch, v -> s.glitch = v);
        addNumber("cfg.textstudio.font.author.glitch_amp", fieldX(1), fieldY(0), () -> s.glitchAmp, v -> s.glitchAmp = v, 0.0, 100.0);
        addNumber("cfg.textstudio.font.author.glitch_speed", fieldX(2), fieldY(0), () -> s.glitchSpeed, v -> s.glitchSpeed = v, 0.0, 100.0);
        addNumber("cfg.textstudio.font.author.glitch_flicker", fieldX(0), fieldY(1), () -> s.glitchFlicker, v -> s.glitchFlicker = v, 0.0, 1.0);
        addToggle("cfg.textstudio.font.author.spasm", fieldX(1), fieldY(1), () -> s.spasm, v -> s.spasm = v);
        addNumber("cfg.textstudio.font.author.spasm_amp", fieldX(2), fieldY(1), () -> s.spasmAmp, v -> s.spasmAmp = v, 0.0, 100.0);
        addNumber("cfg.textstudio.font.author.spasm_speed", fieldX(0), fieldY(2), () -> s.spasmSpeed, v -> s.spasmSpeed = v, 0.0, 100.0);
        addToggle("cfg.textstudio.font.author.typewriter", fieldX(1), fieldY(2), () -> s.typewriter, v -> s.typewriter = v);
        addNumber("cfg.textstudio.font.author.typewriter_speed", fieldX(2), fieldY(2), () -> s.typewriterSpeed, v -> s.typewriterSpeed = v, 0.1, 100.0);
        addToggle("cfg.textstudio.font.author.typewriter_back", fieldX(0), fieldY(3), () -> s.typewriterBack, v -> s.typewriterBack = v);
        addToggle("cfg.textstudio.font.author.blink_wave", fieldX(1), fieldY(3), () -> s.blinkWave, v -> s.blinkWave = v);
        addNumber("cfg.textstudio.font.author.blink_wave_speed", fieldX(2), fieldY(3), () -> s.blinkWaveSpeed, v -> s.blinkWaveSpeed = v, 0.05, 20.0);
        addNumber("cfg.textstudio.font.author.blink_wave_depth", fieldX(0), fieldY(4), () -> s.blinkWaveDepth, v -> s.blinkWaveDepth = v, 0.0, 1.0);
    }

    private void buildGlyphTab() {
        AuthorConfig.EffectSettings s = current();

        addToggle("cfg.textstudio.font.author.glyph_scale", fieldX(0), fieldY(0), () -> s.glyphScale, v -> s.glyphScale = v);
        addNumber("cfg.textstudio.font.author.glyph_scale_amp", fieldX(1), fieldY(0), () -> s.glyphScaleAmp, v -> s.glyphScaleAmp = v, 0.0, 1.5);
        addNumber("cfg.textstudio.font.author.glyph_scale_speed", fieldX(2), fieldY(0), () -> s.glyphScaleSpeed, v -> s.glyphScaleSpeed = v, 0.05, 20.0);

        addToggle("cfg.textstudio.font.author.ecg", fieldX(0), fieldY(1), () -> s.ecg, v -> s.ecg = v);
        addNumber("cfg.textstudio.font.author.ecg_amp", fieldX(1), fieldY(1), () -> s.ecgAmp, v -> s.ecgAmp = v, 0.0, 10.0);
        addNumber("cfg.textstudio.font.author.ecg_speed", fieldX(2), fieldY(1), () -> s.ecgSpeed, v -> s.ecgSpeed = v, 0.05, 20.0);
        addNumber("cfg.textstudio.font.author.ecg_width", fieldX(0), fieldY(2), () -> s.ecgWidth, v -> s.ecgWidth = v, 0.25, 12.0);

        addToggle("cfg.textstudio.font.author.heartbeat", fieldX(1), fieldY(2), () -> s.heartbeat, v -> s.heartbeat = v);
        addNumber("cfg.textstudio.font.author.heartbeat_amp", fieldX(2), fieldY(2), () -> s.heartbeatAmp, v -> s.heartbeatAmp = v, 0.0, 10.0);
        addNumber("cfg.textstudio.font.author.heartbeat_speed", fieldX(0), fieldY(3), () -> s.heartbeatSpeed, v -> s.heartbeatSpeed = v, 0.05, 20.0);

        addToggle("cfg.textstudio.font.author.glitch_spike", fieldX(1), fieldY(3), () -> s.glitchSpike, v -> s.glitchSpike = v);
        addNumber("cfg.textstudio.font.author.glitch_spike_amp", fieldX(2), fieldY(3), () -> s.glitchSpikeAmp, v -> s.glitchSpikeAmp = v, 0.0, 10.0);
        addNumber("cfg.textstudio.font.author.glitch_spike_speed", fieldX(0), fieldY(4), () -> s.glitchSpikeSpeed, v -> s.glitchSpikeSpeed = v, 0.05, 20.0);
        addNumber("cfg.textstudio.font.author.glitch_spike_chance", fieldX(1), fieldY(4), () -> s.glitchSpikeChance, v -> s.glitchSpikeChance = v, 0.0, 1.0);

        addToggle("cfg.textstudio.font.author.signal_loss", fieldX(2), fieldY(4), () -> s.signalLoss, v -> s.signalLoss = v);
        addNumber("cfg.textstudio.font.author.signal_loss_chance", fieldX(0), fieldY(5), () -> s.signalLossChance, v -> s.signalLossChance = v, 0.0, 1.0);
        addNumber("cfg.textstudio.font.author.signal_loss_speed", fieldX(1), fieldY(5), () -> s.signalLossSpeed, v -> s.signalLossSpeed = v, 0.05, 20.0);

        addToggle("cfg.textstudio.font.author.note_bounce", fieldX(2), fieldY(5), () -> s.noteBounce, v -> s.noteBounce = v);
        addNumber("cfg.textstudio.font.author.note_bounce_amp", fieldX(0), fieldY(6), () -> s.noteBounceAmp, v -> s.noteBounceAmp = v, 0.0, 10.0);
        addNumber("cfg.textstudio.font.author.note_bounce_speed", fieldX(1), fieldY(6), () -> s.noteBounceSpeed, v -> s.noteBounceSpeed = v, 0.05, 20.0);
    }

    private void buildVisualTab() {
        AuthorConfig.EffectSettings s = current();

        addToggle("cfg.textstudio.font.author.sweep", fieldX(0), fieldY(0), () -> s.sweep, v -> s.sweep = v);
        addNumber("cfg.textstudio.font.author.sweep_speed", fieldX(1), fieldY(0), () -> s.sweepSpeed, v -> s.sweepSpeed = v, 0.05, 20.0);
        addNumber("cfg.textstudio.font.author.sweep_width", fieldX(2), fieldY(0), () -> s.sweepWidth, v -> s.sweepWidth = v, 0.25, 12.0);
        addNumber("cfg.textstudio.font.author.sweep_strength", fieldX(0), fieldY(1), () -> s.sweepStrength, v -> s.sweepStrength = v, 0.0, 1.0);

        addToggle("cfg.textstudio.font.author.outline", fieldX(1), fieldY(1), () -> s.outline, v -> s.outline = v);
        addNumber("cfg.textstudio.font.author.outline_width", fieldX(2), fieldY(1), () -> s.outlineWidth, v -> s.outlineWidth = v, 0.35, 2.5);
        addNumber("cfg.textstudio.font.author.outline_alpha", fieldX(0), fieldY(2), () -> s.outlineAlpha, v -> s.outlineAlpha = v, 0.0, 1.0);

        addToggle("cfg.textstudio.font.author.glow", fieldX(1), fieldY(2), () -> s.glow, v -> s.glow = v);
        addNumber("cfg.textstudio.font.author.glow_radius", fieldX(2), fieldY(2), () -> s.glowRadius, v -> s.glowRadius = v, 0.35, 3.0);
        addNumber("cfg.textstudio.font.author.glow_alpha", fieldX(0), fieldY(3), () -> s.glowAlpha, v -> s.glowAlpha = v, 0.0, 1.0);

        addToggle("cfg.textstudio.font.author.trail", fieldX(1), fieldY(3), () -> s.trail, v -> s.trail = v);
        addNumber("cfg.textstudio.font.author.trail_strength", fieldX(2), fieldY(3), () -> s.trailStrength, v -> s.trailStrength = v, 0.0, 3.0);
        addNumber("cfg.textstudio.font.author.trail_alpha", fieldX(0), fieldY(4), () -> s.trailAlpha, v -> s.trailAlpha = v, 0.0, 1.0);

        addToggle("cfg.textstudio.font.author.extrude", fieldX(1), fieldY(4), () -> s.extrude, v -> s.extrude = v);
        addNumber("cfg.textstudio.font.author.extrude_depth", fieldX(2), fieldY(4), () -> s.extrudeDepth, v -> s.extrudeDepth = v, 0.25, 4.0);
        addNumber("cfg.textstudio.font.author.extrude_alpha", fieldX(0), fieldY(5), () -> s.extrudeAlpha, v -> s.extrudeAlpha = v, 0.0, 1.0);
    }

    private void buildPaletteTab() {
        AuthorConfig.EffectSettings s = current();
        addToggle("gui.textstudio.font.editor.palette.enabled", fieldX(0), fieldY(0), () -> s.palette, v -> s.palette = v);
        addNumber("gui.textstudio.font.editor.palette.speed", fieldX(1), fieldY(0), () -> s.paletteSpeed, v -> s.paletteSpeed = v, 0.0, 100.0);
        addNumber("gui.textstudio.font.editor.palette.spread", fieldX(2), fieldY(0), () -> s.paletteSpread, v -> s.paletteSpread = v, 0.0, 10.0);
        addToggle("gui.textstudio.font.editor.palette.flash", fieldX(0), fieldY(1), () -> s.paletteFlash, v -> s.paletteFlash = v);

        addActionButton(
                KineticI18n.translatable("gui.textstudio.font.editor.palette.open"),
                fieldX(1),
                fieldY(1),
                145,
                18,
                this::openPaletteEditor,
                KineticI18n.translatable("gui.textstudio.font.editor.tip.palette_open")
        );
        fieldLabels.add(new FieldLabel(KineticI18n.translatable("gui.textstudio.font.editor.palette.current"), PALETTE_SWATCH_X, PALETTE_SWATCH_Y - 17, 225));
        registerTip(
                PALETTE_SWATCH_X,
                PALETTE_SWATCH_Y,
                225,
                40,
                KineticI18n.translatable("gui.textstudio.font.editor.tip.palette_swatches")
        );
    }

    private void openPaletteEditor() {
        AuthorConfig.EffectSettings s = current();
        KineticSelectors.openPalette(
                KineticI18n.translatable("gui.textstudio.font.editor.palette.open"),
                parsePalette(s.paletteColors),
                MAX_PALETTE_COLORS,
                colors -> {
                    s.paletteColors = joinPalette(colors);
                    if (!colors.isEmpty()) {
                        s.palette = true;
                    }
                }
        );
    }

    private void addToggle(String key, int x, int y, BooleanSupplier getter, Consumer<Boolean> setter) {
        Component label = KineticI18n.translatable(key);
        fieldLabels.add(new FieldLabel(label, x, y + 3, FIELD_LABEL_W));
        boolean value = getter.getAsBoolean();
        ui().button(x + FIELD_CONTROL_OFFSET, y, FIELD_CONTROL_W)
                .text(KineticI18n.translatable(value ? "gui.textstudio.font.editor.state.on" : "gui.textstudio.font.editor.state.off"))
                .onClick(() -> {
                    setter.accept(!getter.getAsBoolean());
                    closeContextMenu();
                    rebuild();
                })
                .build();
        registerTip(
                x,
                y - 1,
                FIELD_CONTROL_OFFSET + FIELD_CONTROL_W,
                FIELD_H + 2,
                optionToggleTip(key, label)
        );
    }

    private void addNumber(String key, int x, int y, DoubleSupplier getter, DoubleConsumer setter, double min, double max) {
        Component label = KineticI18n.translatable(key);
        fieldLabels.add(new FieldLabel(label, x, y + 3, FIELD_LABEL_W));
        KineticNumberField box = ui().numberField(x + FIELD_CONTROL_OFFSET, y, FIELD_CONTROL_W, NumberType.DECIMAL)
                .label(label)
                .allowNegative(false)
                .range(min, max)
                .firstShownTextAsDefault().build();
        box.setDoubleValue(getter.getAsDouble());
        box.onTextChange(value -> {
            Double parsed = box.getDoubleValue();
            if (parsed != null) {
                setter.accept(parsed);
            }
        });
        registerTip(
                x,
                y - 1,
                FIELD_CONTROL_OFFSET + FIELD_CONTROL_W,
                FIELD_H + 2,
                optionNumberTip(key, label, min, max)
        );
    }

    private Component optionToggleTip(String key, Component label) {
        String detailKey = key + ".tip";
        if (KineticI18n.hasTranslation(detailKey)) {
            return KineticI18n.translatable(detailKey);
        }
        return KineticI18n.translatable("gui.textstudio.font.editor.tip.toggle", label);
    }

    private Component optionNumberTip(String key, Component label, double min, double max) {
        String detailKey = key + ".tip";
        Component range = KineticI18n.translatable(
                "gui.textstudio.font.editor.tip.range",
                formatNumber(min),
                formatNumber(max)
        );
        if (KineticI18n.hasTranslation(detailKey)) {
            return KineticI18n.translatable(detailKey).copy().append(" ").append(range);
        }
        return KineticI18n.translatable(
                "gui.textstudio.font.editor.tip.number",
                label,
                formatNumber(min),
                formatNumber(max)
        );
    }

    private AuthorConfig.EffectSettings current() {
        return draftEffects.get(selectedPreset);
    }

    private void addPreset() {
        if (draftEffects.size() >= MAX_PRESETS) {
            return;
        }
        draftEffects.add(new AuthorConfig.EffectSettings());
        selectedPreset = draftEffects.size() - 1;
        presetScrollOffset = Integer.MAX_VALUE;
        tab = EditorTab.COLOR;
        closeContextMenu();
        rebuild();
    }

    private void duplicatePreset(int index) {
        if (draftEffects.size() >= MAX_PRESETS || index < 0 || index >= draftEffects.size()) {
            return;
        }
        draftEffects.add(index + 1, draftEffects.get(index).copy());
        selectedPreset = index + 1;
        revealSelectedPreset = true;
        closeContextMenu();
        rebuild();
    }

    private void resetPreset(int index) {
        if (index < 0 || index >= draftEffects.size()) {
            return;
        }
        draftEffects.set(index, new AuthorConfig.EffectSettings());
        selectedPreset = index;
        closeContextMenu();
        rebuild();
    }

    private void deletePreset(int index) {
        if (draftEffects.size() <= 1 || index < 0 || index >= draftEffects.size()) {
            return;
        }
        draftEffects.remove(index);
        selectedPreset = Mth.clamp(index, 0, draftEffects.size() - 1);
        closeContextMenu();
        rebuild();
    }

    private void copyPreviewText() {
        KineticClientRuntime.setClipboard(buildInlineText(current(), previewText));
        showCopyToast();
    }

    private void copyEffectPrefix() {
        KineticClientRuntime.setClipboard(buildEffectPrefix(current()));
        showCopyToast();
    }

    private void copyEffectStop() {
        KineticClientRuntime.setClipboard(CompactTagCodec.encodeReset());
        showCopyToast();
    }

    private void showCopyToast() {
        KineticOverlays.toast(
                "textstudio_copy_success",
                KineticI18n.translatable("msg.textstudio.font.copy.success")
        );
    }

    private void save() {
        AuthorConfig.EFFECTS.clear();
        for (AuthorConfig.EffectSettings effect : draftEffects) {
            AuthorConfig.EFFECTS.add(effect.copy());
        }
        AuthorConfig.save();
        KTConfigApi.notifySaved(FontModuleClient.CONFIG_PAGE_ID);
        commitDraft();
    }

    @Override
    protected void renderBackground(KineticGraphics graphics, int mouseX, int mouseY, float partialTick) {
        KineticTheme.shadow(graphics, width(), height());
        KineticTheme.panel(graphics, LIST_X, LIST_Y, LIST_W, LIST_H);
        KineticTheme.panel(graphics, PREVIEW_X, PREVIEW_Y, PREVIEW_W, PREVIEW_H);
        KineticTheme.panel(graphics, EDITOR_X, EDITOR_Y, EDITOR_W, EDITOR_H);
        if (categoryMenuOpen) {
            KineticTheme.panel(graphics, CATEGORY_X, CATEGORY_MENU_Y, CATEGORY_W, CATEGORY_MENU_H);
        }
    }

    @Override
    protected void renderForeground(KineticGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.scrollingText(KineticI18n.translatable("gui.textstudio.font.editor.presets"), LIST_X + 9, LIST_Y + 8, LIST_W - 18, 0xFFFFFF, false);
        graphics.scrollingText(KineticI18n.translatable("gui.textstudio.font.editor.preview.title"), PREVIEW_X + 12, PREVIEW_Y + 8, 80, 0xFFFFFF, false);
        graphics.text(
                KineticI18n.translatable("gui.textstudio.font.editor.preset_selected", selectedPreset + 1),
                PREVIEW_X + 96,
                PREVIEW_Y + 8,
                0xFFFFFF,
                true
        );

        for (FieldLabel label : fieldLabels) {
            graphics.scrollingText(label.component(), label.x(), label.y(), label.maxWidth(), 0xFFFFFF, false);
        }

        if (!categoryMenuOpen && tab == EditorTab.PALETTE) {
            renderPalette(graphics);
        }

        renderPreview(graphics, mouseX, mouseY);
    }

    private static void renderPresetSwatches(KineticGraphics graphics, AuthorConfig.EffectSettings effect, int x, int y) {
        List<Integer> colors = parsePalette(effect.paletteColors);
        if (colors.isEmpty()) {
            return;
        }
        int count = Math.min(3, colors.size());
        for (int i = 0; i < count; i++) {
            int color = 0xFF000000 | colors.get(i);
            KineticTheme.colorSwatch(graphics, x + i * 8, y, 6, 6, color, false);
        }
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
                    graphics.translate(PREVIEW_CONTENT_X + 2, PREVIEW_CONTENT_Y + 3);
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
                    PREVIEW_SCROLLBAR_W, PREVIEW_CONTENT_H, 14);
        }
    }

    private List<FormattedCharSequence> getPreviewLines() {
        AuthorConfig.EffectSettings effect = current();
        String text = previewText.isEmpty() ? " " : previewText;
        if (previewLinesText != null
                && previewLinesText.equals(text)
                && previewLinesPreset == selectedPreset
                && previewLinesEffect == effect) {
            return previewLines;
        }

        Style previewStyle = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF));
        IStyle.TextEffectStyleData data = new IStyle.TextEffectStyleData(
                selectedPreset + 1,
                false,
                false,
                false,
                false,
                false
        );
        data.customConfig = effect;
        if ((Object) previewStyle instanceof IStyle effectStyle) {
            effectStyle.textstudio_font$setStyleData(data);
        }

        MutableComponent preview = Component.literal(text).setStyle(previewStyle);
        int wrapWidth = Math.max(20, (int) ((PREVIEW_CONTENT_W - 6) / PREVIEW_SCALE));
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

    private void renderPalette(KineticGraphics graphics) {
        List<Integer> colors = parsePalette(current().paletteColors);
        if (colors.isEmpty()) {
            graphics.scrollingText(KineticI18n.translatable("gui.textstudio.font.editor.palette.empty_short"), PALETTE_SWATCH_X, PALETTE_SWATCH_Y + 3, 225, 0xFFFFFF, false);
            return;
        }
        for (int i = 0; i < colors.size(); i++) {
            int col = i % PALETTE_SWATCH_COLS;
            int row = i / PALETTE_SWATCH_COLS;
            int x = PALETTE_SWATCH_X + col * (PALETTE_SWATCH_CELL + PALETTE_SWATCH_GAP);
            int y = PALETTE_SWATCH_Y + row * (PALETTE_SWATCH_CELL + PALETTE_SWATCH_GAP);
            KineticTheme.colorSwatch(graphics, x, y, PALETTE_SWATCH_CELL, PALETTE_SWATCH_CELL, colors.get(i), true);
        }
    }


    @Override
    protected void renderTooltips(int scaledMouseX, int scaledMouseY) {
        if (categoryMenuOpen) {
            Component categoryTip = findCategoryTooltip(scaledMouseX, scaledMouseY);
            if (categoryTip != null) {
                showTooltip(categoryTip);
            }
            return;
        }

        Component dynamic = findDynamicTooltip(scaledMouseX, scaledMouseY);
        if (dynamic != null) {
            showTooltip(dynamic);
            return;
        }

        for (int i = hoverTips.size() - 1; i >= 0; i--) {
            HoverTip tip = hoverTips.get(i);
            if (KineticTheme.hovering(scaledMouseX, scaledMouseY, tip.x(), tip.y(), tip.w(), tip.h())) {
                showTooltip(tip.text());
                return;
            }
        }
    }

    private Component findCategoryTooltip(int mouseX, int mouseY) {
        if (KineticTheme.hovering(mouseX, mouseY, CATEGORY_X, CATEGORY_Y, CATEGORY_W, CATEGORY_H)) {
            return KineticI18n.translatable("gui.textstudio.font.editor.tip.category_selector");
        }
        if (!KineticTheme.hovering(mouseX, mouseY, CATEGORY_X, CATEGORY_MENU_Y, CATEGORY_W, CATEGORY_MENU_H)) {
            return null;
        }
        int row = (mouseY - (CATEGORY_MENU_Y + 2)) / CATEGORY_MENU_ROW_H;
        EditorTab[] values = EditorTab.values();
        if (row < 0 || row >= values.length) {
            return null;
        }
        int buttonY = CATEGORY_MENU_Y + 2 + row * CATEGORY_MENU_ROW_H;
        if (!KineticTheme.hovering(mouseX, mouseY, CATEGORY_X + 3, buttonY, CATEGORY_W - 6, 18)) {
            return null;
        }
        return KineticI18n.translatable("gui.textstudio.font.editor.tip.tab", tabLabel(values[row]));
    }

    private Component findDynamicTooltip(int mouseX, int mouseY) {
        if (tab == EditorTab.PALETTE) {
            int swatchIndex = paletteSwatchIndexAt(mouseX, mouseY);
            if (swatchIndex >= 0) {
                List<Integer> colors = parsePalette(current().paletteColors);
                if (swatchIndex < colors.size()) {
                    return KineticI18n.translatable(
                            "gui.textstudio.font.editor.tip.palette_color",
                            String.format(Locale.ROOT, "%06X", colors.get(swatchIndex) & 0xFFFFFF)
                    );
                }
            }
        }
        return null;
    }

    @Override
    protected boolean onMouseClickCapture(MouseInput input) {
        if (categoryMenuOpen) {
            boolean inSelector = input.inside(CATEGORY_X, CATEGORY_Y, CATEGORY_W, CATEGORY_H);
            boolean inMenu = input.inside(CATEGORY_X, CATEGORY_MENU_Y, CATEGORY_W, CATEGORY_MENU_H);
            if (!inSelector && !inMenu) {
                categoryMenuOpen = false;
                rebuild();
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean onMouseClick(MouseInput input) {
        double mouseX = input.x();
        double mouseY = input.y();
        if (input.isRight()) {
            if (tab == EditorTab.PALETTE && paletteSwatchIndexAt(mouseX, mouseY) >= 0) {
                openPaletteEditor();
                return true;
            }
            return false;
        }
        if (input.isLeft()) {
            if (previewScroll.canScroll() && previewScroll.beginDrag(mouseX, mouseY, input.button(),
                    PREVIEW_SCROLLBAR_X - 2, PREVIEW_CONTENT_Y, PREVIEW_SCROLLBAR_W + 4, PREVIEW_CONTENT_H, 14)) {
                previewScrollbarDragging = true;
                return true;
            }
            if (tab == EditorTab.PALETTE && paletteSwatchIndexAt(mouseX, mouseY) >= 0) {
                openPaletteEditor();
                return true;
            }
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
            previewScroll.drag(input.y(), PREVIEW_CONTENT_Y, PREVIEW_CONTENT_H, 14);
            return true;
        }
        return false;
    }

    @Override
    protected boolean onMouseScroll(ScrollInput input) {
        if (input.inside(PREVIEW_X, PREVIEW_Y + 43, PREVIEW_W, PREVIEW_H - 43)) {
            previewScroll.scroll(input.deltaY());
            return true;
        }
        return false;
    }

    private int paletteSwatchIndexAt(double mouseX, double mouseY) {
        List<Integer> colors = parsePalette(current().paletteColors);
        for (int i = 0; i < colors.size(); i++) {
            int col = i % PALETTE_SWATCH_COLS;
            int row = i / PALETTE_SWATCH_COLS;
            int x = PALETTE_SWATCH_X + col * (PALETTE_SWATCH_CELL + PALETTE_SWATCH_GAP);
            int y = PALETTE_SWATCH_Y + row * (PALETTE_SWATCH_CELL + PALETTE_SWATCH_GAP);
            if (KineticTheme.hovering(mouseX, mouseY, x, y, PALETTE_SWATCH_CELL, PALETTE_SWATCH_CELL)) {
                return i;
            }
        }
        return -1;
    }

    private void openPresetContextMenu(int index, double mouseX, double mouseY) {
        List<KineticOverlays.MenuItem> items = new ArrayList<>();
        items.add(KineticOverlays.MenuItem.action(
                KineticI18n.translatable("gui.textstudio.font.editor.context.copy_prefix"),
                this::copyEffectPrefix
        ));
        items.add(draftEffects.size() < MAX_PRESETS
                ? KineticOverlays.MenuItem.action(
                        KineticI18n.translatable("gui.textstudio.font.editor.context.duplicate"),
                        () -> duplicatePreset(index)
                )
                : KineticOverlays.MenuItem.disabled(
                        KineticI18n.translatable("gui.textstudio.font.editor.context.duplicate")
                ));
        items.add(KineticOverlays.MenuItem.action(
                KineticI18n.translatable("gui.textstudio.font.editor.context.reset"),
                () -> resetPreset(index)
        ));
        items.add(draftEffects.size() > 1
                ? KineticOverlays.MenuItem.danger(
                        KineticI18n.translatable("gui.textstudio.font.editor.context.delete"),
                        () -> deletePreset(index)
                )
                : KineticOverlays.MenuItem.disabled(
                        KineticI18n.translatable("gui.textstudio.font.editor.context.delete")
                ));
        openContextMenu(mouseX, mouseY, items);
    }

    private String buildEffectPrefix(AuthorConfig.EffectSettings settings) {
        if (selectedPreset >= 0
                && selectedPreset < AuthorConfig.EFFECTS.size()
                && CompactTagCodec.equivalent(settings, AuthorConfig.EFFECTS.get(selectedPreset))) {
            return CompactTagCodec.encodePresetPrefix(selectedPreset + 1);
        }
        return CompactTagCodec.encodeCustomPrefix(settings);
    }

    private String buildInlineText(AuthorConfig.EffectSettings settings, String text) {
        return buildEffectPrefix(settings) + (text == null ? "" : text) + CompactTagCodec.encodeReset();
    }

    private static String formatNumber(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static List<Integer> parsePalette(String raw) {
        List<Integer> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return result;
        }
        for (String part : raw.split("\\|")) {
            String value = part.trim();
            if (value.startsWith("#")) {
                value = value.substring(1);
            }
            if (!value.matches("[0-9a-fA-F]{6}")) {
                continue;
            }
            try {
                result.add(Integer.parseInt(value, 16) & 0xFFFFFF);
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    private static String joinPalette(List<Integer> colors) {
        StringBuilder builder = new StringBuilder();
        for (int color : colors) {
            if (!builder.isEmpty()) {
                builder.append('|');
            }
            builder.append(String.format(Locale.ROOT, "%06X", color & 0xFFFFFF));
        }
        return builder.toString();
    }

    /** 预设列表：只绘制行，滚动、滚动条、中键跳转由核心处理 / Preset list; scrolling and scrollbar are core-owned. */
    private final class PresetList extends KineticRowList<AuthorConfig.EffectSettings> {
        private static final int CONTENT_Y = LIST_Y + 25;
        private static final int CONTENT_H = LIST_Y + LIST_H - 26 - CONTENT_Y - 5;

        PresetList() {
            super(LIST_X + 7, CONTENT_Y, LIST_W - 11, CONTENT_H, LIST_ROW_H);
        }

        @Override
        protected void renderRowBackground(KineticGraphics graphics, int index, int x, int y, int width, int height,
                                           boolean hovered, boolean selected) {
            KineticTheme.stateSurface(graphics, x, y + 1, width, height - 2, KineticTheme.Surface.PANEL_ALT,
                    selected, hovered, false);
        }

        @Override
        protected void renderRow(KineticGraphics graphics, AuthorConfig.EffectSettings effect, int index, int x, int y,
                                 int width, int height, boolean hovered, boolean selected) {
            graphics.scrollingText(KineticI18n.translatable("gui.textstudio.font.editor.preset", index + 1), x + 4, y + 5, 66, 0xFFFFFF, false);
            renderPresetSwatches(graphics, effect, x + 71, y + 5);
        }

        @Override
        protected boolean onRowClick(AuthorConfig.EffectSettings effect, int index, MouseInput input) {
            if (input.isLeft()) {
                selectedPreset = index;
                rebuild();
                return true;
            }
            if (input.isRight()) {
                if (index != selectedPreset) {
                    selectedPreset = index;
                    rebuild();
                }
                openPresetContextMenu(index, input.x(), input.y());
                return true;
            }
            return false;
        }

        @Override
        protected Component rowTooltip(AuthorConfig.EffectSettings effect, int index) {
            return KineticI18n.translatable("gui.textstudio.font.editor.tip.preset_row", index + 1);
        }
    }
}
