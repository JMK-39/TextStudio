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

    private static final int LIST_X = 0;
    private static final int LIST_Y = 0;
    private static final int LIST_W = 128;
    private static final int LIST_H = 360;
    private static final int LIST_ROW_H = 16;
    private static final int LIST_ROW_GAP = 2;
    private static final int PRESET_SELECTED_OUTLINE = 0xFFFFE600;
    private static final int PRESET_HOVER_OUTLINE = 0xFF55AAFF;

    private static final int PREVIEW_X = 132;
    private static final int PREVIEW_Y = 0;
    private static final int PREVIEW_W = 508;
    // The preview area: one row with the text field and every action (Copy, Prefix, Stop, Save at the top right),
    // then the live preview right under it; the effect editor follows.
    private static final int PREVIEW_H = 86;
    private static final float PREVIEW_SCALE = 1.45f;
    private static final int PREVIEW_CONTENT_X = PREVIEW_X + 4;
    private static final int PREVIEW_CONTENT_Y = PREVIEW_Y + 26;
    private static final int PREVIEW_CONTENT_W = PREVIEW_W - 16;
    private static final int PREVIEW_CONTENT_H = PREVIEW_H - 31;
    private static final int PREVIEW_SCROLLBAR_X = PREVIEW_X + PREVIEW_W - 8;
    private static final int PREVIEW_SCROLLBAR_W = 4;
    private static final int COPY_X = 596 - 4 - 40 - 4 - 40 - 4 - 56, INPUT_W = COPY_X - 4 - (PREVIEW_X + 4);

    private static final int EDITOR_X = 132;
    private static final int EDITOR_Y = 90;
    private static final int EDITOR_W = 508;
    private static final int EDITOR_H = 270;

    private static final int FIELD_COL_1_X = 146;
    private static final int FIELD_COL_2_X = 307;
    private static final int FIELD_COL_3_X = 468;
    private static final int FIELD_CONTROL_OFFSET = 105;
    // Labels end 4px before their control.
    private static final int FIELD_LABEL_W = FIELD_CONTROL_OFFSET - 4;
    private static final int FIELD_CONTROL_W = 40;
    private static final int FIELD_H = 14;
    private static final int FIELD_ROW = 18;
    // An effect's own settings sit this far right of its switch; effects in a column are this much further apart.
    private static final int GROUP_INDENT = 10;
    private static final int GROUP_GAP = 4;

    // Every effect setting in one list that scrolls (wheel or the scrollbar at its right edge, lined up with the
    // preview's), category after category; rows slide in and out cut at the list's edges.
    private static final int SETTINGS_TOP = EDITOR_Y + 4;
    private static final int SETTINGS_BOTTOM = EDITOR_Y + EDITOR_H - 4;
    private static final int SETTINGS_SCROLLBAR_X = EDITOR_X + EDITOR_W - 8;
    private static final int SETTINGS_SCROLLBAR_W = 4;
    private static final int SETTINGS_WHEEL_STEP = 36;
    private static final int SECTION_HEADING_H = 14;
    private static final int SECTION_GAP = 10;
    private static final int SECTION_HEADING_COLOR = 0xFFFFAA00;

    private static final int PALETTE_SWATCH_X = 307;
    private static final int PALETTE_SWATCH_CELL = 14;
    private static final int PALETTE_SWATCH_GAP = 3;
    private static final int PALETTE_SWATCH_COLS = 12;

    private static final int MAX_PALETTE_COLORS = 24;
    private static final int MAX_PRESETS = 255;
    private static final int CONTEXT_W = 112;
    private static final int CONTEXT_ROW_H = 18;

    private enum Section {
        COLOR,
        MOTION,
        SPECIAL,
        GLYPH,
        VISUAL,
        PALETTE
    }

    // A setting's label on its row (y is the row's place in the settings list). maxWidth is the room before the
    // control; longer (usually English) labels scroll instead of overlapping it.
    private record FieldLabel(Component component, int x, int y, int maxWidth) {
    }

    // inList: y is a place in the settings list, which scrolls; otherwise a place on the page.
    private record HoverTip(int x, int y, int w, int h, Component text, boolean inList) {
    }

    private record SectionHeading(Component title, int y) {
    }

    private final List<AuthorConfig.EffectSettings> draftEffects = new ArrayList<>();
    private final List<FieldLabel> fieldLabels = new ArrayList<>();
    private final List<HoverTip> hoverTips = new ArrayList<>();
    // Builds the list's controls: the core cuts them at the list's edges and moves them with the scroll.
    private KineticUi listUi;
    private final List<SectionHeading> sectionHeadings = new ArrayList<>();
    private final java.util.EnumMap<Section, Integer> sectionTops = new java.util.EnumMap<>(Section.class);
    private final KineticScrollController settingsScroll = new KineticScrollController();
    private boolean settingsScrollbarDragging;
    private int sectionTop;
    private int sectionBottom;
    private int paletteSwatchY;
    private int layoutColumn;
    private int layoutY;
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
        bindDeleteAction(() -> draftEffects.size() > 1 && selectedPreset >= 0 && selectedPreset < draftEffects.size(),
                () -> deletePreset(selectedPreset));
    }

    @Override
    protected void build(KineticUi ui) {
        fieldLabels.clear();
        hoverTips.clear();
        sectionHeadings.clear();
        selectedPreset = Mth.clamp(selectedPreset, 0, draftEffects.size() - 1);
        if (presetList != null && presetScrollOffset != Integer.MAX_VALUE) presetScrollOffset = presetList.scrollOffset();

        addActionButton(
                KineticI18n.translatable("gui.textstudio.font.editor.cancel"),
                LIST_X + 4,
                LIST_Y + 4,
                40,
                18,
                this::close,
                KineticI18n.translatable("gui.textstudio.font.editor.tip.cancel")
        );
        addActionButton(
                KineticI18n.translatable("gui.textstudio.font.editor.save"),
                596,
                PREVIEW_Y + 4,
                40,
                18,
                this::save,
                KineticI18n.translatable("gui.textstudio.font.editor.tip.save")
        );

        buildPreviewInput();
        registerTip(
                PREVIEW_X + 4,
                PREVIEW_Y + 4,
                INPUT_W,
                18,
                KineticI18n.translatable("gui.textstudio.font.editor.tip.preview_input")
        );

        addActionButton(
                KineticI18n.translatable("gui.textstudio.font.editor.copy"),
                COPY_X,
                PREVIEW_Y + 4,
                56,
                18,
                this::copyPreviewText,
                KineticI18n.translatable("gui.textstudio.font.editor.tip.copy")
        );
        addActionButton(
                KineticI18n.translatable("gui.textstudio.font.editor.copy_prefix"),
                COPY_X + 60,
                PREVIEW_Y + 4,
                40,
                18,
                this::copyEffectPrefix,
                KineticI18n.translatable("gui.textstudio.font.editor.tip.copy_prefix")
        );
        addActionButton(
                KineticI18n.translatable("gui.textstudio.font.editor.copy_stop"),
                COPY_X + 104,
                PREVIEW_Y + 4,
                40,
                18,
                this::copyEffectStop,
                KineticI18n.translatable("gui.textstudio.font.editor.tip.copy_stop")
        );

        buildSettings();

        KineticButton addPresetButton = addActionButton(
                KineticI18n.translatable("gui.textstudio.font.editor.preset.add"),
                LIST_X + 48,
                LIST_Y + 4,
                LIST_W - 52,
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
        ui().textField(PREVIEW_X + 4, PREVIEW_Y + 4, INPUT_W)
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

    private Component sectionLabel(Section value) {
        return switch (value) {
            case COLOR -> KineticI18n.translatable("gui.textstudio.font.editor.tab.color");
            case MOTION -> KineticI18n.translatable("gui.textstudio.font.editor.tab.motion");
            case SPECIAL -> KineticI18n.translatable("gui.textstudio.font.editor.tab.special");
            case GLYPH -> KineticI18n.translatable("gui.textstudio.font.editor.tab.glyph");
            case VISUAL -> KineticI18n.translatable("gui.textstudio.font.editor.tab.visual");
            case PALETTE -> KineticI18n.translatable("gui.textstudio.font.editor.tab.palette");
        };
    }

    private KineticButton addActionButton(Component text, int x, int y, int w, int h, Runnable action, Component tip) {
        return addActionButton(ui(), text, x, y, w, action, tip);
    }

    private static KineticButton addActionButton(KineticUi ui, Component text, int x, int y, int w, Runnable action, Component tip) {
        return ui.button(x, y, w).text(text).tooltip(tip).compact().onClick(action).build();
    }

    private void registerTip(int x, int y, int w, int h, Component tip) {
        hoverTips.add(new HoverTip(x, y, w, h, tip, false));
    }

    /** A tip over a place in the settings list. */
    private void registerListTip(int x, int y, int w, int h, Component tip) {
        hoverTips.add(new HoverTip(x, y, w, h, tip, true));
    }

    /** Every category's settings, one under the other, each under its title. */
    private void buildSettings() {
        listUi = ui().scrollViewport(EDITOR_X, SETTINGS_TOP, SETTINGS_SCROLLBAR_X - 2, SETTINGS_BOTTOM, settingsScroll::smoothOffset);
        int top = 0;
        for (Section section : Section.values()) {
            sectionTops.put(section, top);
            sectionHeadings.add(new SectionHeading(sectionLabel(section), top));
            sectionTop = top + SECTION_HEADING_H;
            sectionBottom = sectionTop;
            switch (section) {
                case COLOR -> buildColorSection();
                case MOTION -> buildMotionSection();
                case SPECIAL -> buildSpecialSection();
                case GLYPH -> buildGlyphSection();
                case VISUAL -> buildVisualSection();
                case PALETTE -> buildPaletteSection();
            }
            top = sectionBottom + SECTION_GAP;
        }
        settingsScroll.updateContent(top - SECTION_GAP, SETTINGS_BOTTOM - SETTINGS_TOP, 1);
    }

    /** Scrolls the list so a category's title is at its top. */
    private void revealSection(Section section) {
        settingsScroll.setOffset(sectionTops.getOrDefault(section, 0));
    }

    private int listScroll() {
        return (int) Math.round(settingsScroll.smoothOffset());
    }

    /** Where a place in the settings list is on the page. */
    private int listY(int y) {
        return SETTINGS_TOP + y - listScroll();
    }

    /** Whether the point is over the visible part of the settings list. */
    private static boolean overList(double mouseX, double mouseY) {
        return mouseX >= EDITOR_X && mouseX < SETTINGS_SCROLLBAR_X - 2 && mouseY >= SETTINGS_TOP && mouseY < SETTINGS_BOTTOM;
    }

    private int fieldX(int column) {
        return switch (column) {
            case 1 -> FIELD_COL_2_X;
            case 2 -> FIELD_COL_3_X;
            default -> FIELD_COL_1_X;
        };
    }

    private void buildColorSection() {
        AuthorConfig.EffectSettings s = current();
        beginColumns();
        effect("cfg.textstudio.font.author.use_rainbow", () -> s.useRainbow, v -> s.useRainbow = v);
        option("cfg.textstudio.font.author.rainbow_speed", () -> s.rainbowSpeed, v -> s.rainbowSpeed = v, 0.0, 100.0);
        option("cfg.textstudio.font.author.rainbow_spread", () -> s.rainbowSpread, v -> s.rainbowSpread = v, 0.0, 10.0);
        effect("cfg.textstudio.font.author.pulse", () -> s.pulse, v -> s.pulse = v);
        option("cfg.textstudio.font.author.pulse_base", () -> s.pulseBase, v -> s.pulseBase = v, 0.0, 10.0);
        option("cfg.textstudio.font.author.pulse_amp", () -> s.pulseAmp, v -> s.pulseAmp = v, 0.0, 20.0);
        option("cfg.textstudio.font.author.pulse_speed", () -> s.pulseSpeed, v -> s.pulseSpeed = v, 0.0, 100.0);
        nextColumn();
        effect("cfg.textstudio.font.author.fade", () -> s.fade, v -> s.fade = v);
        option("cfg.textstudio.font.author.fade_min", () -> s.fadeMin, v -> s.fadeMin = v, 0.0, 1.0);
        option("cfg.textstudio.font.author.fade_speed", () -> s.fadeSpeed, v -> s.fadeSpeed = v, 0.0, 100.0);
        effect("cfg.textstudio.font.author.neon_flicker", () -> s.neonFlicker, v -> s.neonFlicker = v);
        option("cfg.textstudio.font.author.neon_flicker_speed", () -> s.neonFlickerSpeed, v -> s.neonFlickerSpeed = v, 0.0, 100.0);
        effect("cfg.textstudio.font.author.chromatic", () -> s.chromatic, v -> s.chromatic = v);
        optionToggle("cfg.textstudio.font.author.chromatic_pulse", () -> s.chromaticPulse, v -> s.chromaticPulse = v);
        option("cfg.textstudio.font.author.chromatic_offset", () -> s.chromaticOffset, v -> s.chromaticOffset = v, 0.0, 30.0);
        option("cfg.textstudio.font.author.chromatic_alpha", () -> s.chromaticAlpha, v -> s.chromaticAlpha = v, 0.0, 1.0);
        nextColumn();
        effect("cfg.textstudio.font.author.shimmer", () -> s.shimmer, v -> s.shimmer = v);
        option("cfg.textstudio.font.author.shimmer_width", () -> s.shimmerWidth, v -> s.shimmerWidth = v, 0.25, 10.0);
        option("cfg.textstudio.font.author.shimmer_strength", () -> s.shimmerStrength, v -> s.shimmerStrength = v, 0.0, 1.0);
        option("cfg.textstudio.font.author.shimmer_speed", () -> s.shimmerSpeed, v -> s.shimmerSpeed = v, 0.05, 20.0);
        effect("cfg.textstudio.font.author.sparkle", () -> s.sparkle, v -> s.sparkle = v);
        option("cfg.textstudio.font.author.sparkle_strength", () -> s.sparkleStrength, v -> s.sparkleStrength = v, 0.0, 1.0);
        option("cfg.textstudio.font.author.sparkle_chance", () -> s.sparkleChance, v -> s.sparkleChance = v, 0.0, 1.0);
        option("cfg.textstudio.font.author.sparkle_speed", () -> s.sparkleSpeed, v -> s.sparkleSpeed = v, 0.05, 20.0);
    }

    private void buildMotionSection() {
        AuthorConfig.EffectSettings s = current();
        beginColumns();
        effect("cfg.textstudio.font.author.wave", () -> s.wave, v -> s.wave = v);
        option("cfg.textstudio.font.author.wave_amp", () -> s.waveAmp, v -> s.waveAmp = v, 0.0, 100.0);
        option("cfg.textstudio.font.author.wave_speed", () -> s.waveSpeed, v -> s.waveSpeed = v, 0.0, 100.0);
        effect("cfg.textstudio.font.author.bounce", () -> s.bounce, v -> s.bounce = v);
        option("cfg.textstudio.font.author.bounce_amp", () -> s.bounceAmp, v -> s.bounceAmp = v, 0.0, 100.0);
        option("cfg.textstudio.font.author.bounce_speed", () -> s.bounceSpeed, v -> s.bounceSpeed = v, 0.0, 100.0);
        effect("cfg.textstudio.font.author.shake", () -> s.shake, v -> s.shake = v);
        option("cfg.textstudio.font.author.shake_amp", () -> s.shakeAmp, v -> s.shakeAmp = v, 0.0, 100.0);
        option("cfg.textstudio.font.author.shake_speed", () -> s.shakeSpeed, v -> s.shakeSpeed = v, 0.0, 100.0);
        effect("cfg.textstudio.font.author.swing", () -> s.swing, v -> s.swing = v);
        option("cfg.textstudio.font.author.swing_amp", () -> s.swingAmp, v -> s.swingAmp = v, 0.0, 100.0);
        option("cfg.textstudio.font.author.swing_speed", () -> s.swingSpeed, v -> s.swingSpeed = v, 0.0, 100.0);
        nextColumn();
        effect("cfg.textstudio.font.author.wiggle", () -> s.wiggle, v -> s.wiggle = v);
        option("cfg.textstudio.font.author.wiggle_amp", () -> s.wiggleAmp, v -> s.wiggleAmp = v, 0.0, 100.0);
        option("cfg.textstudio.font.author.wiggle_speed", () -> s.wiggleSpeed, v -> s.wiggleSpeed = v, 0.0, 100.0);
        effect("cfg.textstudio.font.author.turb", () -> s.turb, v -> s.turb = v);
        option("cfg.textstudio.font.author.turb_amp", () -> s.turbAmp, v -> s.turbAmp = v, 0.0, 100.0);
        option("cfg.textstudio.font.author.turb_speed", () -> s.turbSpeed, v -> s.turbSpeed = v, 0.0, 100.0);
        effect("cfg.textstudio.font.author.pend", () -> s.pend, v -> s.pend = v);
        option("cfg.textstudio.font.author.pend_amp", () -> s.pendAmp, v -> s.pendAmp = v, 0.0, 100.0);
        option("cfg.textstudio.font.author.pend_speed", () -> s.pendSpeed, v -> s.pendSpeed = v, 0.0, 100.0);
        nextColumn();
        effect("cfg.textstudio.font.author.orbit", () -> s.orbit, v -> s.orbit = v);
        option("cfg.textstudio.font.author.orbit_amp", () -> s.orbitAmp, v -> s.orbitAmp = v, 0.0, 20.0);
        option("cfg.textstudio.font.author.orbit_speed", () -> s.orbitSpeed, v -> s.orbitSpeed = v, 0.05, 20.0);
        effect("cfg.textstudio.font.author.ripple", () -> s.ripple, v -> s.ripple = v);
        option("cfg.textstudio.font.author.ripple_amp", () -> s.rippleAmp, v -> s.rippleAmp = v, 0.0, 20.0);
        option("cfg.textstudio.font.author.ripple_speed", () -> s.rippleSpeed, v -> s.rippleSpeed = v, 0.05, 20.0);
        effect("cfg.textstudio.font.author.drift", () -> s.drift, v -> s.drift = v);
        option("cfg.textstudio.font.author.drift_amp", () -> s.driftAmp, v -> s.driftAmp = v, 0.0, 20.0);
        option("cfg.textstudio.font.author.drift_speed", () -> s.driftSpeed, v -> s.driftSpeed = v, 0.05, 20.0);
    }

    private void buildSpecialSection() {
        AuthorConfig.EffectSettings s = current();
        beginColumns();
        effect("cfg.textstudio.font.author.glitch", () -> s.glitch, v -> s.glitch = v);
        option("cfg.textstudio.font.author.glitch_amp", () -> s.glitchAmp, v -> s.glitchAmp = v, 0.0, 100.0);
        option("cfg.textstudio.font.author.glitch_speed", () -> s.glitchSpeed, v -> s.glitchSpeed = v, 0.0, 100.0);
        option("cfg.textstudio.font.author.glitch_flicker", () -> s.glitchFlicker, v -> s.glitchFlicker = v, 0.0, 1.0);
        nextColumn();
        effect("cfg.textstudio.font.author.spasm", () -> s.spasm, v -> s.spasm = v);
        option("cfg.textstudio.font.author.spasm_amp", () -> s.spasmAmp, v -> s.spasmAmp = v, 0.0, 100.0);
        option("cfg.textstudio.font.author.spasm_speed", () -> s.spasmSpeed, v -> s.spasmSpeed = v, 0.0, 100.0);
        effect("cfg.textstudio.font.author.typewriter", () -> s.typewriter, v -> s.typewriter = v);
        option("cfg.textstudio.font.author.typewriter_speed", () -> s.typewriterSpeed, v -> s.typewriterSpeed = v, 0.1, 100.0);
        optionToggle("cfg.textstudio.font.author.typewriter_back", () -> s.typewriterBack, v -> s.typewriterBack = v);
        nextColumn();
        effect("cfg.textstudio.font.author.blink_wave", () -> s.blinkWave, v -> s.blinkWave = v);
        option("cfg.textstudio.font.author.blink_wave_speed", () -> s.blinkWaveSpeed, v -> s.blinkWaveSpeed = v, 0.05, 20.0);
        option("cfg.textstudio.font.author.blink_wave_depth", () -> s.blinkWaveDepth, v -> s.blinkWaveDepth = v, 0.0, 1.0);
    }

    private void buildGlyphSection() {
        AuthorConfig.EffectSettings s = current();
        beginColumns();
        effect("cfg.textstudio.font.author.glyph_scale", () -> s.glyphScale, v -> s.glyphScale = v);
        option("cfg.textstudio.font.author.glyph_scale_amp", () -> s.glyphScaleAmp, v -> s.glyphScaleAmp = v, 0.0, 1.5);
        option("cfg.textstudio.font.author.glyph_scale_speed", () -> s.glyphScaleSpeed, v -> s.glyphScaleSpeed = v, 0.05, 20.0);
        effect("cfg.textstudio.font.author.ecg", () -> s.ecg, v -> s.ecg = v);
        option("cfg.textstudio.font.author.ecg_amp", () -> s.ecgAmp, v -> s.ecgAmp = v, 0.0, 10.0);
        option("cfg.textstudio.font.author.ecg_speed", () -> s.ecgSpeed, v -> s.ecgSpeed = v, 0.05, 20.0);
        option("cfg.textstudio.font.author.ecg_width", () -> s.ecgWidth, v -> s.ecgWidth = v, 0.25, 12.0);
        nextColumn();
        effect("cfg.textstudio.font.author.heartbeat", () -> s.heartbeat, v -> s.heartbeat = v);
        option("cfg.textstudio.font.author.heartbeat_amp", () -> s.heartbeatAmp, v -> s.heartbeatAmp = v, 0.0, 10.0);
        option("cfg.textstudio.font.author.heartbeat_speed", () -> s.heartbeatSpeed, v -> s.heartbeatSpeed = v, 0.05, 20.0);
        effect("cfg.textstudio.font.author.glitch_spike", () -> s.glitchSpike, v -> s.glitchSpike = v);
        option("cfg.textstudio.font.author.glitch_spike_amp", () -> s.glitchSpikeAmp, v -> s.glitchSpikeAmp = v, 0.0, 10.0);
        option("cfg.textstudio.font.author.glitch_spike_speed", () -> s.glitchSpikeSpeed, v -> s.glitchSpikeSpeed = v, 0.05, 20.0);
        option("cfg.textstudio.font.author.glitch_spike_chance", () -> s.glitchSpikeChance, v -> s.glitchSpikeChance = v, 0.0, 1.0);
        nextColumn();
        effect("cfg.textstudio.font.author.signal_loss", () -> s.signalLoss, v -> s.signalLoss = v);
        option("cfg.textstudio.font.author.signal_loss_chance", () -> s.signalLossChance, v -> s.signalLossChance = v, 0.0, 1.0);
        option("cfg.textstudio.font.author.signal_loss_speed", () -> s.signalLossSpeed, v -> s.signalLossSpeed = v, 0.05, 20.0);
        effect("cfg.textstudio.font.author.note_bounce", () -> s.noteBounce, v -> s.noteBounce = v);
        option("cfg.textstudio.font.author.note_bounce_amp", () -> s.noteBounceAmp, v -> s.noteBounceAmp = v, 0.0, 10.0);
        option("cfg.textstudio.font.author.note_bounce_speed", () -> s.noteBounceSpeed, v -> s.noteBounceSpeed = v, 0.05, 20.0);
    }

    private void buildVisualSection() {
        AuthorConfig.EffectSettings s = current();
        beginColumns();
        effect("cfg.textstudio.font.author.sweep", () -> s.sweep, v -> s.sweep = v);
        option("cfg.textstudio.font.author.sweep_speed", () -> s.sweepSpeed, v -> s.sweepSpeed = v, 0.05, 20.0);
        option("cfg.textstudio.font.author.sweep_width", () -> s.sweepWidth, v -> s.sweepWidth = v, 0.25, 12.0);
        option("cfg.textstudio.font.author.sweep_strength", () -> s.sweepStrength, v -> s.sweepStrength = v, 0.0, 1.0);
        effect("cfg.textstudio.font.author.outline", () -> s.outline, v -> s.outline = v);
        option("cfg.textstudio.font.author.outline_width", () -> s.outlineWidth, v -> s.outlineWidth = v, 0.35, 2.5);
        option("cfg.textstudio.font.author.outline_alpha", () -> s.outlineAlpha, v -> s.outlineAlpha = v, 0.0, 1.0);
        nextColumn();
        effect("cfg.textstudio.font.author.glow", () -> s.glow, v -> s.glow = v);
        option("cfg.textstudio.font.author.glow_radius", () -> s.glowRadius, v -> s.glowRadius = v, 0.35, 3.0);
        option("cfg.textstudio.font.author.glow_alpha", () -> s.glowAlpha, v -> s.glowAlpha = v, 0.0, 1.0);
        effect("cfg.textstudio.font.author.trail", () -> s.trail, v -> s.trail = v);
        option("cfg.textstudio.font.author.trail_strength", () -> s.trailStrength, v -> s.trailStrength = v, 0.0, 3.0);
        option("cfg.textstudio.font.author.trail_alpha", () -> s.trailAlpha, v -> s.trailAlpha = v, 0.0, 1.0);
        nextColumn();
        effect("cfg.textstudio.font.author.extrude", () -> s.extrude, v -> s.extrude = v);
        option("cfg.textstudio.font.author.extrude_depth", () -> s.extrudeDepth, v -> s.extrudeDepth = v, 0.25, 4.0);
        option("cfg.textstudio.font.author.extrude_alpha", () -> s.extrudeAlpha, v -> s.extrudeAlpha = v, 0.0, 1.0);
    }

    private void buildPaletteSection() {
        AuthorConfig.EffectSettings s = current();
        beginColumns();
        effect("gui.textstudio.font.editor.palette.enabled", () -> s.palette, v -> s.palette = v);
        option("gui.textstudio.font.editor.palette.speed", () -> s.paletteSpeed, v -> s.paletteSpeed = v, 0.0, 100.0);
        option("gui.textstudio.font.editor.palette.spread", () -> s.paletteSpread, v -> s.paletteSpread = v, 0.0, 10.0);
        optionToggle("gui.textstudio.font.editor.palette.flash", () -> s.paletteFlash, v -> s.paletteFlash = v);

        addActionButton(
                listUi,
                KineticI18n.translatable("gui.textstudio.font.editor.palette.open"),
                fieldX(1),
                SETTINGS_TOP + sectionTop,
                145,
                this::openPaletteEditor,
                KineticI18n.translatable("gui.textstudio.font.editor.tip.palette_open")
        );
        // Below the palette button: the label, then the colors, in the second column.
        fieldLabels.add(new FieldLabel(KineticI18n.translatable("gui.textstudio.font.editor.palette.current"),
                PALETTE_SWATCH_X, sectionTop + FIELD_ROW, 225));
        paletteSwatchY = sectionTop + 2 * FIELD_ROW;
        sectionBottom = Math.max(sectionBottom, paletteSwatchY + paletteSwatchesHeight());
        registerListTip(
                PALETTE_SWATCH_X,
                paletteSwatchY,
                225,
                paletteSwatchesHeight(),
                KineticI18n.translatable("gui.textstudio.font.editor.tip.palette_swatches")
        );
    }

    /** Lays a category's effects out from the top of its first column. */
    private void beginColumns() {
        layoutColumn = 0;
        layoutY = sectionTop;
    }

    /** The effects after this go into the next column, from the top of the category. */
    private void nextColumn() {
        layoutColumn++;
        layoutY = sectionTop;
    }

    /** An effect's switch; the settings added after it belong to it and sit indented beneath it. */
    private void effect(String key, BooleanSupplier getter, Consumer<Boolean> setter) {
        if (layoutY > sectionTop) {
            layoutY += GROUP_GAP;
        }
        addToggle(key, fieldX(layoutColumn), layoutY, 0, getter, setter);
        nextRow();
    }

    /** A number setting of the effect above it. */
    private void option(String key, DoubleSupplier getter, DoubleConsumer setter, double min, double max) {
        addNumber(key, fieldX(layoutColumn), layoutY, GROUP_INDENT, getter, setter, min, max);
        nextRow();
    }

    /** An on/off setting of the effect above it. */
    private void optionToggle(String key, BooleanSupplier getter, Consumer<Boolean> setter) {
        addToggle(key, fieldX(layoutColumn), layoutY, GROUP_INDENT, getter, setter);
        nextRow();
    }

    private void nextRow() {
        sectionBottom = Math.max(sectionBottom, layoutY + FIELD_ROW - 2);
        layoutY += FIELD_ROW;
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

    private void addToggle(String key, int x, int y, int indent, BooleanSupplier getter, Consumer<Boolean> setter) {
        Component label = KineticI18n.translatable(key);
        fieldLabels.add(new FieldLabel(label, x + indent, y, FIELD_LABEL_W - indent));
        boolean value = getter.getAsBoolean();
        listUi.button(x + FIELD_CONTROL_OFFSET, SETTINGS_TOP + y, FIELD_CONTROL_W)
                .text(KineticI18n.translatable(value ? "gui.textstudio.font.editor.state.on" : "gui.textstudio.font.editor.state.off"))
                .onClick(() -> {
                    setter.accept(!getter.getAsBoolean());
                    closeContextMenu();
                    rebuild();
                })
                .build();
        registerListTip(
                x + indent,
                y - 1,
                FIELD_CONTROL_OFFSET + FIELD_CONTROL_W - indent,
                FIELD_H + 2,
                optionToggleTip(key, label)
        );
    }

    private void addNumber(String key, int x, int y, int indent, DoubleSupplier getter, DoubleConsumer setter, double min, double max) {
        Component label = KineticI18n.translatable(key);
        fieldLabels.add(new FieldLabel(label, x + indent, y, FIELD_LABEL_W - indent));
        KineticNumberField box = listUi.numberField(x + FIELD_CONTROL_OFFSET, SETTINGS_TOP + y, FIELD_CONTROL_W, NumberType.DECIMAL)
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
        registerListTip(
                x + indent,
                y - 1,
                FIELD_CONTROL_OFFSET + FIELD_CONTROL_W - indent,
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
        KineticTheme.panel(graphics, 0, 0, width(), height());
        KineticTheme.surface(graphics, LIST_X + 1, LIST_Y + 1, LIST_W - 1, LIST_H - 2, KineticTheme.Surface.PANEL_ALT);
        KineticTheme.surface(graphics, PREVIEW_X, PREVIEW_Y + 1, PREVIEW_W - 1, PREVIEW_H - 1, KineticTheme.Surface.PANEL);
        KineticTheme.surface(graphics, EDITOR_X, EDITOR_Y, EDITOR_W - 1, EDITOR_H - 1, KineticTheme.Surface.PANEL);
        KineticTheme.verticalSeparator(graphics, PREVIEW_X - 2, 1, height() - 2);
        KineticTheme.separator(graphics, PREVIEW_X, EDITOR_Y - 2, PREVIEW_W - 1);
    }

    @Override
    protected void renderForeground(KineticGraphics graphics, int mouseX, int mouseY, float partialTick) {

        // The list's text is cut at its edges like its controls, so rows slide in and out whole.
        graphics.clipped(EDITOR_X, SETTINGS_TOP, SETTINGS_SCROLLBAR_X - 2, SETTINGS_BOTTOM, () -> {
            for (SectionHeading heading : sectionHeadings) {
                int y = listY(heading.y());
                if (y < SETTINGS_BOTTOM && y + SECTION_HEADING_H > SETTINGS_TOP) {
                    graphics.scrollingText(heading.title(), FIELD_COL_1_X, y + 2, SETTINGS_SCROLLBAR_X - 4 - FIELD_COL_1_X,
                            SECTION_HEADING_COLOR, false);
                }
            }
            for (FieldLabel label : fieldLabels) {
                int y = listY(label.y());
                if (y < SETTINGS_BOTTOM && y + FIELD_H > SETTINGS_TOP) {
                    graphics.scrollingText(label.component(), label.x(), y + 3, label.maxWidth(), 0xFFFFFF, false);
                }
            }
            renderPalette(graphics);
        });
        if (settingsScroll.canScroll()) {
            settingsScroll.render(graphics, mouseX, mouseY, SETTINGS_SCROLLBAR_X, SETTINGS_TOP, SETTINGS_SCROLLBAR_W,
                    SETTINGS_BOTTOM - SETTINGS_TOP, 14);
        }

        renderPreview(graphics, mouseX, mouseY);
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

        MutableComponent preview = effectPreviewComponent(effect, selectedPreset + 1, text);
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

    /** Use the draft itself for both list samples and the larger preview, including unsaved parameter changes. */
    private static MutableComponent effectPreviewComponent(AuthorConfig.EffectSettings effect, int presetId, String text) {
        Style previewStyle = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF));
        IStyle.TextEffectStyleData data = new IStyle.TextEffectStyleData(
                presetId,
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

        return Component.literal(text).setStyle(previewStyle);
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
        int top = listY(paletteSwatchY);
        if (colors.isEmpty()) {
            graphics.scrollingText(KineticI18n.translatable("gui.textstudio.font.editor.palette.empty_short"), PALETTE_SWATCH_X, top + 3, 225, 0xFFFFFF, false);
            return;
        }
        for (int i = 0; i < colors.size(); i++) {
            int col = i % PALETTE_SWATCH_COLS;
            int row = i / PALETTE_SWATCH_COLS;
            int x = PALETTE_SWATCH_X + col * (PALETTE_SWATCH_CELL + PALETTE_SWATCH_GAP);
            int y = top + row * (PALETTE_SWATCH_CELL + PALETTE_SWATCH_GAP);
            KineticTheme.colorSwatch(graphics, x, y, PALETTE_SWATCH_CELL, PALETTE_SWATCH_CELL, colors.get(i), true);
        }
    }

    /** Room the palette colors take: two rows of swatches, enough for the most a palette holds. */
    private static int paletteSwatchesHeight() {
        int rows = (MAX_PALETTE_COLORS + PALETTE_SWATCH_COLS - 1) / PALETTE_SWATCH_COLS;
        return rows * PALETTE_SWATCH_CELL + (rows - 1) * PALETTE_SWATCH_GAP;
    }


    @Override
    protected void renderTooltips(int scaledMouseX, int scaledMouseY) {
        Component dynamic = findDynamicTooltip(scaledMouseX, scaledMouseY);
        if (dynamic != null) {
            showTooltip(dynamic);
            return;
        }

        for (int i = hoverTips.size() - 1; i >= 0; i--) {
            HoverTip tip = hoverTips.get(i);
            if (tip.inList() && !overList(scaledMouseX, scaledMouseY)) {
                continue;
            }
            int tipY = tip.inList() ? listY(tip.y()) : tip.y();
            if (KineticTheme.hovering(scaledMouseX, scaledMouseY, tip.x(), tipY, tip.w(), tip.h())) {
                showTooltip(tip.text());
                return;
            }
        }
    }

    private Component findDynamicTooltip(int mouseX, int mouseY) {
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
        return null;
    }

    @Override
    protected boolean onMouseClick(MouseInput input) {
        double mouseX = input.x();
        double mouseY = input.y();
        if (input.isRight()) {
            if (paletteSwatchIndexAt(mouseX, mouseY) >= 0) {
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
            if (settingsScroll.canScroll() && settingsScroll.beginDrag(mouseX, mouseY, input.button(),
                    SETTINGS_SCROLLBAR_X - 2, SETTINGS_TOP, SETTINGS_SCROLLBAR_W + 4, SETTINGS_BOTTOM - SETTINGS_TOP, 14)) {
                settingsScrollbarDragging = true;
                return true;
            }
            if (paletteSwatchIndexAt(mouseX, mouseY) >= 0) {
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
        if (settingsScrollbarDragging) {
            settingsScrollbarDragging = false;
            settingsScroll.release(input.button());
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
        if (settingsScrollbarDragging) {
            settingsScroll.drag(input.y(), SETTINGS_TOP, SETTINGS_BOTTOM - SETTINGS_TOP, 14);
            return true;
        }
        return false;
    }

    @Override
    protected boolean onMouseScroll(ScrollInput input) {
        if (input.inside(PREVIEW_X, PREVIEW_CONTENT_Y - 4, PREVIEW_W, PREVIEW_H - (PREVIEW_CONTENT_Y - 4 - PREVIEW_Y))) {
            previewScroll.scroll(input.deltaY());
            return true;
        }
        if (input.inside(EDITOR_X, SETTINGS_TOP, EDITOR_W, SETTINGS_BOTTOM - SETTINGS_TOP) && settingsScroll.canScroll()) {
            settingsScroll.scroll(input.deltaY(), SETTINGS_WHEEL_STEP);
            return true;
        }
        return false;
    }

    private int paletteSwatchIndexAt(double mouseX, double mouseY) {
        if (!overList(mouseX, mouseY)) {
            return -1;
        }
        List<Integer> colors = parsePalette(current().paletteColors);
        for (int i = 0; i < colors.size(); i++) {
            int col = i % PALETTE_SWATCH_COLS;
            int row = i / PALETTE_SWATCH_COLS;
            int x = PALETTE_SWATCH_X + col * (PALETTE_SWATCH_CELL + PALETTE_SWATCH_GAP);
            int y = listY(paletteSwatchY) + row * (PALETTE_SWATCH_CELL + PALETTE_SWATCH_GAP);
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
        private static final int CONTENT_H = LIST_Y + LIST_H - CONTENT_Y - 5;

        PresetList() {
            super(LIST_X + 4, CONTENT_Y, LIST_W - 8, CONTENT_H, LIST_ROW_H);
        }

        @Override
        protected void renderRowBackground(KineticGraphics graphics, int index, int x, int y, int width, int height,
                                           boolean hovered, boolean selected) {
            KineticTheme.stateSurface(graphics, x, y + LIST_ROW_GAP / 2, width, height - LIST_ROW_GAP, KineticTheme.Surface.PANEL_ALT,
                    selected, hovered, false);
        }

        @Override
        protected void renderRow(KineticGraphics graphics, AuthorConfig.EffectSettings effect, int index, int x, int y,
                                 int width, int height, boolean hovered, boolean selected) {
            Component number = Component.literal((index + 1) + ". ");
            int numberWidth = KineticText.width(number);
            // Vertically centred in the 14 px row surface: 2 px above and below the text.
            graphics.scrollingText(number, x + 4, y + 4, numberWidth, 0xFFFFFF, false);
            String name = FontModuleGuidePage.presetName(index).getString();
            graphics.scrollingText(effectPreviewComponent(effect, index + 1, name),
                    x + 4 + numberWidth, y + 4, Math.max(0, width - 8 - numberWidth), 0xFFFFFF, false);
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
