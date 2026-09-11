package dev.xyat.textstudio.font.client;

import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.overlay.GuiOverlay;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets.Scroll;
import dev.xyat.kineticcore.api.client.selector.ColorPickerScreen;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets.NumericEditBox;
import dev.xyat.kineticcore.config.client.KTConfigApi;
import dev.xyat.textstudio.font.api.IStyle;
import dev.xyat.textstudio.font.config.AuthorConfig;
import dev.xyat.textstudio.font.client.parser.CompactTagCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public final class FontModuleConfigScreen extends KineticScreen {
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

    private record FieldLabel(Component component, int x, int y) {
    }

    private record HoverTip(int x, int y, int w, int h, Component text) {
    }

    private record ContextEntry(Component label, Runnable action, boolean active) {
    }

    private final Screen parent;
    private final List<AuthorConfig.EffectSettings> draftEffects = new ArrayList<>();
    private final List<FieldLabel> fieldLabels = new ArrayList<>();
    private final List<HoverTip> hoverTips = new ArrayList<>();

    private EditorTab tab = EditorTab.COLOR;
    private boolean categoryMenuOpen;
    private int selectedPreset;
    private double presetScroll;
    private final Scroll.State presetScrollSmoothing = new Scroll.State();
    private boolean presetScrollbarDragging;
    private double previewScroll;
    private final Scroll.State previewScrollSmoothing = new Scroll.State();
    private boolean previewScrollbarDragging;
    private String previewText;
    private String previewLinesText;
    private int previewLinesPreset = -1;
    private AuthorConfig.EffectSettings previewLinesEffect;
    private List<FormattedCharSequence> previewLines = List.of();

    private List<ContextEntry> contextEntries = List.of();
    private int contextX;
    private int contextY;

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
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> create(parent))
        );
    }

    public static Screen create(Screen parent) {
        return new FontModuleConfigScreen(parent);
    }

    private FontModuleConfigScreen(Screen parent) {
        super(Component.translatable("gui.textstudio.font.editor.title"));
        this.parent = parent;
        this.previewText = I18n.get("gui.textstudio.font.editor.preview.default");
        useCanvas(CANVAS_W, CANVAS_H, 6);
        maxScale = 1.0f;
        for (AuthorConfig.EffectSettings effect : AuthorConfig.EFFECTS) {
            draftEffects.add(effect.copy());
        }
        if (draftEffects.isEmpty()) {
            draftEffects.add(new AuthorConfig.EffectSettings());
        }
        configureStandaloneDraft(this::captureFontDraftSnapshot, this::restoreFontDraftSnapshot);
    }

    @Override
    protected void buildUi() {
        fieldLabels.clear();
        hoverTips.clear();
        selectedPreset = Mth.clamp(selectedPreset, 0, draftEffects.size() - 1);
        clampPresetScroll();

        addActionButton(
                Component.translatable("gui.textstudio.font.editor.cancel"),
                512,
                PREVIEW_Y + 4,
                56,
                18,
                button -> onClose(),
                Component.translatable("gui.textstudio.font.editor.tip.cancel")
        );
        addActionButton(
                Component.translatable("gui.textstudio.font.editor.save"),
                572,
                PREVIEW_Y + 4,
                56,
                18,
                button -> saveAndClose(),
                Component.translatable("gui.textstudio.font.editor.tip.save")
        );

        EditBox previewBox = getEditBox();
        addRenderableWidget(previewBox);
        registerTip(
                PREVIEW_X + 12,
                PREVIEW_Y + 23,
                302,
                18,
                Component.translatable("gui.textstudio.font.editor.tip.preview_input")
        );

        addActionButton(
                Component.translatable("gui.textstudio.font.editor.copy"),
                452,
                PREVIEW_Y + 26,
                56,
                18,
                button -> copyPreviewText(),
                Component.translatable("gui.textstudio.font.editor.tip.copy")
        );
        addActionButton(
                Component.translatable("gui.textstudio.font.editor.copy_prefix"),
                512,
                PREVIEW_Y + 26,
                56,
                18,
                button -> copyEffectPrefix(),
                Component.translatable("gui.textstudio.font.editor.tip.copy_prefix")
        );
        addActionButton(
                Component.translatable("gui.textstudio.font.editor.copy_stop"),
                572,
                PREVIEW_Y + 26,
                56,
                18,
                button -> copyEffectStop(),
                Component.translatable("gui.textstudio.font.editor.tip.copy_stop")
        );

        addActionButton(
                Component.translatable("gui.textstudio.font.editor.category.current", tabLabel(tab)),
                CATEGORY_X,
                CATEGORY_Y,
                CATEGORY_W,
                CATEGORY_H,
                button -> toggleCategoryMenu(),
                Component.translatable("gui.textstudio.font.editor.tip.category_selector")
        );

        if (categoryMenuOpen) {
            buildCategoryMenuButtons();
        } else {
            buildCurrentTab();
        }

        Button addPresetButton = addActionButton(
                Component.translatable("gui.textstudio.font.editor.preset.add"),
                LIST_X + 10,
                LIST_Y + LIST_H - 26,
                LIST_W - 20,
                18,
                button -> addPreset(),
                Component.translatable("gui.textstudio.font.editor.tip.preset_add")
        );
        addPresetButton.active = draftEffects.size() < MAX_PRESETS;
    }

    private @NotNull EditBox getEditBox() {
        EditBox previewBox = new EditBox(
                font,
                PREVIEW_X + 12,
                PREVIEW_Y + 23,
                302,
                18,
                Component.translatable("gui.textstudio.font.editor.preview.input")
        );
        previewBox.setMaxLength(4096);
        previewBox.setValue(previewText);
        previewBox.setHint(Component.translatable("gui.textstudio.font.editor.preview.hint"));
        previewBox.setResponder(value -> {
            previewText = value;
            previewScroll = 0;
            invalidatePreviewLines();
        });
        return previewBox;
    }

    private Component tabLabel(EditorTab value) {
        return switch (value) {
            case COLOR -> Component.translatable("gui.textstudio.font.editor.tab.color");
            case MOTION -> Component.translatable("gui.textstudio.font.editor.tab.motion");
            case SPECIAL -> Component.translatable("gui.textstudio.font.editor.tab.special");
            case GLYPH -> Component.translatable("gui.textstudio.font.editor.tab.glyph");
            case VISUAL -> Component.translatable("gui.textstudio.font.editor.tab.visual");
            case PALETTE -> Component.translatable("gui.textstudio.font.editor.tab.palette");
        };
    }

    private void toggleCategoryMenu() {
        categoryMenuOpen = !categoryMenuOpen;
        closeContextMenu();
        rebuildScreen();
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
                    button -> selectCategory(target),
                    Component.translatable("gui.textstudio.font.editor.tip.tab", tabLabel(target))
            );
        }
    }

    private void selectCategory(EditorTab next) {
        tab = next;
        categoryMenuOpen = false;
        closeContextMenu();
        rebuildScreen();
    }

    private Button addActionButton(Component text, int x, int y, int w, int h, Button.OnPress action, Component tip) {
        Button button = Button.builder(text, action).bounds(x, y, w, h).build();
        addRenderableWidget(button);
        registerTip(x, y, w, h, tip);
        return button;
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
                Component.translatable("gui.textstudio.font.editor.palette.open"),
                fieldX(1),
                fieldY(1),
                145,
                18,
                button -> openPaletteEditor(),
                Component.translatable("gui.textstudio.font.editor.tip.palette_open")
        );
        fieldLabels.add(new FieldLabel(Component.translatable("gui.textstudio.font.editor.palette.current"), PALETTE_SWATCH_X, PALETTE_SWATCH_Y - 17));
        registerTip(
                PALETTE_SWATCH_X,
                PALETTE_SWATCH_Y,
                225,
                40,
                Component.translatable("gui.textstudio.font.editor.tip.palette_swatches")
        );
    }

    private void openPaletteEditor() {
        AuthorConfig.EffectSettings s = current();
        ColorPickerScreen.openPalette(
                this,
                Component.translatable("gui.textstudio.font.editor.palette.open"),
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
        Component label = Component.translatable(key);
        fieldLabels.add(new FieldLabel(label, x, y + 3));
        boolean value = getter.getAsBoolean();
        addRenderableWidget(Button.builder(
                Component.translatable(value ? "gui.textstudio.font.editor.state.on" : "gui.textstudio.font.editor.state.off"),
                button -> {
                    setter.accept(!getter.getAsBoolean());
                    closeContextMenu();
                    rebuildScreen();
                }
        ).bounds(x + FIELD_CONTROL_OFFSET, y, FIELD_CONTROL_W, FIELD_H).build());
        registerTip(
                x,
                y - 1,
                FIELD_CONTROL_OFFSET + FIELD_CONTROL_W,
                FIELD_H + 2,
                optionToggleTip(key, label)
        );
    }

    private void addNumber(String key, int x, int y, DoubleSupplier getter, DoubleConsumer setter, double min, double max) {
        Component label = Component.translatable(key);
        fieldLabels.add(new FieldLabel(label, x, y + 3));
        NumericEditBox box = NumericEditBox.decimal(font, x + FIELD_CONTROL_OFFSET, y, FIELD_CONTROL_W, FIELD_H, label, false, min, max);
        box.setValue(NumericEditBox.format(getter.getAsDouble()));
        box.setResponder(value -> {
            Double parsed = box.getDoubleValue();
            if (parsed != null) {
                setter.accept(parsed);
            }
        });
        addRenderableWidget(box);
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
        if (I18n.exists(detailKey)) {
            return Component.translatable(detailKey);
        }
        return Component.translatable("gui.textstudio.font.editor.tip.toggle", label);
    }

    private Component optionNumberTip(String key, Component label, double min, double max) {
        String detailKey = key + ".tip";
        Component range = Component.translatable(
                "gui.textstudio.font.editor.tip.range",
                formatNumber(min),
                formatNumber(max)
        );
        if (I18n.exists(detailKey)) {
            return Component.translatable(detailKey).copy().append(" ").append(range);
        }
        return Component.translatable(
                "gui.textstudio.font.editor.tip.number",
                label,
                formatNumber(min),
                formatNumber(max)
        );
    }

    private void rebuildScreen() {
        clearWidgets();
        buildUi();
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
        presetScroll = maxPresetScroll();
        tab = EditorTab.COLOR;
        closeContextMenu();
        rebuildScreen();
    }

    private void duplicatePreset(int index) {
        if (draftEffects.size() >= MAX_PRESETS || index < 0 || index >= draftEffects.size()) {
            return;
        }
        draftEffects.add(index + 1, draftEffects.get(index).copy());
        selectedPreset = index + 1;
        clampPresetScrollToSelection();
        closeContextMenu();
        rebuildScreen();
    }

    private void resetPreset(int index) {
        if (index < 0 || index >= draftEffects.size()) {
            return;
        }
        draftEffects.set(index, new AuthorConfig.EffectSettings());
        selectedPreset = index;
        closeContextMenu();
        rebuildScreen();
    }

    private void deletePreset(int index) {
        if (draftEffects.size() <= 1 || index < 0 || index >= draftEffects.size()) {
            return;
        }
        draftEffects.remove(index);
        selectedPreset = Mth.clamp(index, 0, draftEffects.size() - 1);
        clampPresetScroll();
        closeContextMenu();
        rebuildScreen();
    }

    private void copyPreviewText() {
        Minecraft.getInstance().keyboardHandler.setClipboard(buildInlineText(current(), previewText));
        showCopyToast();
    }

    private void copyEffectPrefix() {
        Minecraft.getInstance().keyboardHandler.setClipboard(buildEffectPrefix(current()));
        showCopyToast();
    }

    private void copyEffectStop() {
        Minecraft.getInstance().keyboardHandler.setClipboard(CompactTagCodec.encodeReset());
        showCopyToast();
    }

    private void showCopyToast() {
        GuiOverlay.toast(
                "textstudio_copy_success",
                Component.translatable("msg.textstudio.font.copy.success")
        );
    }

    private void saveAndClose() {
        AuthorConfig.EFFECTS.clear();
        for (AuthorConfig.EffectSettings effect : draftEffects) {
            AuthorConfig.EFFECTS.add(effect.copy());
        }
        AuthorConfig.save();
        KTConfigApi.notifySaved(FontModuleClient.CONFIG_PAGE_ID);
        commitDraft();
        onClose();
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

    @Override
    protected void renderCanvasBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiTheme.shadow(graphics, canvasWidth, canvasHeight);
        GuiTheme.panel(graphics, LIST_X, LIST_Y, LIST_W, LIST_H);
        GuiTheme.panel(graphics, PREVIEW_X, PREVIEW_Y, PREVIEW_W, PREVIEW_H);
        GuiTheme.panel(graphics, EDITOR_X, EDITOR_Y, EDITOR_W, EDITOR_H);
        if (categoryMenuOpen) {
            GuiTheme.panel(graphics, CATEGORY_X, CATEGORY_MENU_Y, CATEGORY_W, CATEGORY_MENU_H);
        }
    }

    @Override
    protected void renderCanvasForeground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawString(font, Component.translatable("gui.textstudio.font.editor.presets"), LIST_X + 9, LIST_Y + 8, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("gui.textstudio.font.editor.preview.title"), PREVIEW_X + 12, PREVIEW_Y + 8, 0xFFFFFF, false);
        graphics.drawString(
                font,
                Component.translatable("gui.textstudio.font.editor.preset_selected", selectedPreset + 1),
                PREVIEW_X + 96,
                PREVIEW_Y + 8,
                0xFFFFFF
        );

        renderPresetList(graphics, mouseX, mouseY);
        for (FieldLabel label : fieldLabels) {
            graphics.drawString(font, label.component(), label.x(), label.y(), 0xFFFFFF, false);
        }

        if (!categoryMenuOpen && tab == EditorTab.PALETTE) {
            renderPalette(graphics);
        }

        renderPreview(graphics, mouseX, mouseY);
        renderContextMenu(graphics, mouseX, mouseY);
    }

    private void renderPresetList(GuiGraphics graphics, int mouseX, int mouseY) {
        int contentY = LIST_Y + 25;
        int addButtonY = LIST_Y + LIST_H - 26;
        int contentH = addButtonY - contentY - 5;
        int visibleRows = Math.max(1, contentH / LIST_ROW_H);
        int maxScroll = maxPresetScroll();

        double visualPresetScroll = presetScrollSmoothing.follow(presetScroll, maxScroll);
        int presetStart = Math.max(0, Math.min((int) Math.floor(visualPresetScroll), maxScroll));
        int presetShift = (int) Math.round((visualPresetScroll - presetStart) * LIST_ROW_H);
        enableCanvasScissor(graphics, LIST_X + 5, contentY, LIST_X + LIST_W - 11, contentY + contentH);
        for (int row = 0; row < visibleRows + 2; row++) {
            int index = presetStart + row;
            if (index >= draftEffects.size()) break;
            int y = contentY + row * LIST_ROW_H - presetShift;
            if (y + LIST_ROW_H <= contentY || y >= contentY + contentH) continue;
            if (index == selectedPreset) {
                graphics.fill(LIST_X + 7, y, LIST_X + LIST_W - 13, y + LIST_ROW_H - 2, 0x66555555);
                graphics.renderOutline(LIST_X + 7, y, LIST_W - 20, LIST_ROW_H - 2, PRESET_SELECTED_OUTLINE);
            } else if (GuiTheme.hovering(mouseX, mouseY, LIST_X + 7, y, LIST_W - 20, LIST_ROW_H - 2)) {
                graphics.fill(LIST_X + 7, y, LIST_X + LIST_W - 13, y + LIST_ROW_H - 2, 0x33444444);
                graphics.renderOutline(LIST_X + 7, y, LIST_W - 20, LIST_ROW_H - 2, PRESET_HOVER_OUTLINE);
            }
            graphics.drawString(font, Component.translatable("gui.textstudio.font.editor.preset", index + 1), LIST_X + 11, y + 5, 0xFFFFFF, false);
            renderPresetSwatches(graphics, draftEffects.get(index), y + 5);
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int thumbHeight = Scroll.calculateThumbHeight(contentH, visibleRows, draftEffects.size(), 18);
            GuiTheme.scrollbar(
                    graphics,
                    mouseX,
                    mouseY,
                    LIST_X + LIST_W - 8,
                    contentY,
                    4,
                    contentH,
                    thumbHeight,
                    maxScroll,
                    visualPresetScroll,
                    presetScrollbarDragging
            );
        }
    }

    private void renderPresetSwatches(GuiGraphics graphics, AuthorConfig.EffectSettings effect, int y) {
        List<Integer> colors = parsePalette(effect.paletteColors);
        if (colors.isEmpty()) {
            return;
        }
        int count = Math.min(3, colors.size());
        for (int i = 0; i < count; i++) {
            int color = 0xFF000000 | colors.get(i);
            graphics.fill(86 + i * 8, y, 86 + i * 8 + 6, y + 6, color);
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
        graphics.pose().translate(PREVIEW_CONTENT_X + 2, PREVIEW_CONTENT_Y + 3, 0.0f);
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
                    14
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
        if (previewStyle instanceof IStyle effectStyle) {
            effectStyle.textstudio_font$setStyleData(data);
        }

        MutableComponent preview = Component.literal(text).setStyle(previewStyle);
        int wrapWidth = Math.max(20, (int) ((PREVIEW_CONTENT_W - 6) / PREVIEW_SCALE));
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

    private void renderPalette(GuiGraphics graphics) {
        List<Integer> colors = parsePalette(current().paletteColors);
        if (colors.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.textstudio.font.editor.palette.empty_short"), PALETTE_SWATCH_X, PALETTE_SWATCH_Y + 3, 0xFFFFFF, false);
            return;
        }
        for (int i = 0; i < colors.size(); i++) {
            int col = i % PALETTE_SWATCH_COLS;
            int row = i / PALETTE_SWATCH_COLS;
            int x = PALETTE_SWATCH_X + col * (PALETTE_SWATCH_CELL + PALETTE_SWATCH_GAP);
            int y = PALETTE_SWATCH_Y + row * (PALETTE_SWATCH_CELL + PALETTE_SWATCH_GAP);
            graphics.fill(x, y, x + PALETTE_SWATCH_CELL, y + PALETTE_SWATCH_CELL, 0xFF000000 | colors.get(i));
            graphics.renderOutline(x, y, PALETTE_SWATCH_CELL, PALETTE_SWATCH_CELL, 0xFFFFFFFF);
        }
    }

    private void renderContextMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        if (contextEntries.isEmpty()) {
            return;
        }
        int height = contextEntries.size() * CONTEXT_ROW_H + 4;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, 0.0f, 420.0f);
        graphics.fill(contextX, contextY, contextX + CONTEXT_W, contextY + height, 0xF0181818);
        graphics.renderOutline(contextX, contextY, CONTEXT_W, height, 0xFFFFB300);
        for (int i = 0; i < contextEntries.size(); i++) {
            ContextEntry entry = contextEntries.get(i);
            int y = contextY + 2 + i * CONTEXT_ROW_H;
            boolean hovered = GuiTheme.hovering(mouseX, mouseY, contextX + 2, y, CONTEXT_W - 4, CONTEXT_ROW_H);
            if (hovered && entry.active()) {
                graphics.fill(contextX + 2, y, contextX + CONTEXT_W - 2, y + CONTEXT_ROW_H, 0x55555555);
            }
            graphics.drawString(font, entry.label(), contextX + 7, y + 5, entry.active() ? 0xFFFFFF : 0xFF999999, false);
        }
        graphics.pose().popPose();
    }

    @Override
    protected void renderTooltips(GuiGraphics graphics, int scaledMouseX, int scaledMouseY, int mouseX, int mouseY) {
        if (!contextEntries.isEmpty()) {
            return;
        }

        if (categoryMenuOpen) {
            Component categoryTip = findCategoryTooltip(scaledMouseX, scaledMouseY);
            if (categoryTip != null) {
                GuiOverlay.requestTooltip(categoryTip, mouseX, mouseY);
            }
            return;
        }

        Component dynamic = findDynamicTooltip(scaledMouseX, scaledMouseY);
        if (dynamic != null) {
            GuiOverlay.requestTooltip(dynamic, mouseX, mouseY);
            return;
        }

        for (int i = hoverTips.size() - 1; i >= 0; i--) {
            HoverTip tip = hoverTips.get(i);
            if (GuiTheme.hovering(scaledMouseX, scaledMouseY, tip.x(), tip.y(), tip.w(), tip.h())) {
                GuiOverlay.requestTooltip(tip.text(), mouseX, mouseY);
                return;
            }
        }
    }

    private Component findCategoryTooltip(int mouseX, int mouseY) {
        if (GuiTheme.hovering(mouseX, mouseY, CATEGORY_X, CATEGORY_Y, CATEGORY_W, CATEGORY_H)) {
            return Component.translatable("gui.textstudio.font.editor.tip.category_selector");
        }
        if (!GuiTheme.hovering(mouseX, mouseY, CATEGORY_X, CATEGORY_MENU_Y, CATEGORY_W, CATEGORY_MENU_H)) {
            return null;
        }
        int row = (mouseY - (CATEGORY_MENU_Y + 2)) / CATEGORY_MENU_ROW_H;
        EditorTab[] values = EditorTab.values();
        if (row < 0 || row >= values.length) {
            return null;
        }
        int buttonY = CATEGORY_MENU_Y + 2 + row * CATEGORY_MENU_ROW_H;
        if (!GuiTheme.hovering(mouseX, mouseY, CATEGORY_X + 3, buttonY, CATEGORY_W - 6, 18)) {
            return null;
        }
        return Component.translatable("gui.textstudio.font.editor.tip.tab", tabLabel(values[row]));
    }

    private Component findDynamicTooltip(int mouseX, int mouseY) {
        int presetIndex = presetIndexAt(mouseX, mouseY);
        if (presetIndex >= 0) {
            return Component.translatable("gui.textstudio.font.editor.tip.preset_row", presetIndex + 1);
        }
        if (tab == EditorTab.PALETTE) {
            int swatchIndex = paletteSwatchIndexAt(mouseX, mouseY);
            if (swatchIndex >= 0) {
                List<Integer> colors = parsePalette(current().paletteColors);
                if (swatchIndex < colors.size()) {
                    return Component.translatable(
                            "gui.textstudio.font.editor.tip.palette_color",
                            String.format(Locale.ROOT, "%06X", colors.get(swatchIndex) & 0xFFFFFF)
                    );
                }
            }
        }
        return null;
    }

    @Override
    protected boolean canvasMouseClicked(double mouseX, double mouseY, int button) {
        if (!contextEntries.isEmpty()) {
            if (handleContextMenuClick(mouseX, mouseY, button)) {
                return true;
            }
            closeContextMenu();
            if (button == 0) {
                return true;
            }
        }

        if (categoryMenuOpen) {
            boolean inSelector = GuiTheme.hovering(mouseX, mouseY, CATEGORY_X, CATEGORY_Y, CATEGORY_W, CATEGORY_H);
            boolean inMenu = GuiTheme.hovering(mouseX, mouseY, CATEGORY_X, CATEGORY_MENU_Y, CATEGORY_W, CATEGORY_MENU_H);
            if (!inSelector && !inMenu) {
                categoryMenuOpen = false;
                rebuildScreen();
                return true;
            }
        }

        if (button == 1) {
            int presetIndex = presetIndexAt(mouseX, mouseY);
            if (presetIndex >= 0) {
                if (presetIndex != selectedPreset) {
                    selectedPreset = presetIndex;
                    rebuildScreen();
                }
                openPresetContextMenu(presetIndex, mouseX, mouseY);
                return true;
            }
            if (tab == EditorTab.PALETTE && paletteSwatchIndexAt(mouseX, mouseY) >= 0) {
                openPaletteEditor();
                return true;
            }
            return super.canvasMouseClicked(mouseX, mouseY, button);
        }

        if (button == 0) {
            int previewMaxScroll = previewMaxScroll();
            if (previewMaxScroll > 0 && GuiTheme.hovering(
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
                        14
                );
                previewScroll = Scroll.calculateScrollOffset(
                        mouseY,
                        PREVIEW_CONTENT_Y,
                        PREVIEW_CONTENT_H,
                        thumbHeight,
                        previewMaxScroll
                );
                return true;
            }

            int contentY = LIST_Y + 25;
            int addButtonY = LIST_Y + LIST_H - 26;
            int contentH = addButtonY - contentY - 5;
            int visibleRows = Math.max(1, contentH / LIST_ROW_H);
            int maxScroll = maxPresetScroll();

            if (maxScroll > 0 && GuiTheme.hovering(mouseX, mouseY, LIST_X + LIST_W - 10, contentY, 9, contentH)) {
                presetScrollbarDragging = true;
                int thumbHeight = Scroll.calculateThumbHeight(contentH, visibleRows, draftEffects.size(), 18);
                presetScroll = Scroll.calculateScrollOffset(mouseY, contentY, contentH, thumbHeight, maxScroll);
            presetScrollSmoothing.snap(presetScroll, maxScroll);
                return true;
            }

            int presetIndex = presetIndexAt(mouseX, mouseY);
            if (presetIndex >= 0) {
                selectedPreset = presetIndex;
                rebuildScreen();
                return true;
            }

            if (tab == EditorTab.PALETTE && paletteSwatchIndexAt(mouseX, mouseY) >= 0) {
                openPaletteEditor();
                return true;
            }
        }
        return super.canvasMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean canvasMouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && (presetScrollbarDragging || previewScrollbarDragging)) {
            presetScrollbarDragging = false;
            previewScrollbarDragging = false;
            return true;
        }
        return super.canvasMouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected boolean canvasMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && previewScrollbarDragging) {
            int maxScroll = previewMaxScroll();
            if (maxScroll > 0) {
                int thumbHeight = Scroll.calculateThumbHeight(
                        PREVIEW_CONTENT_H,
                        previewVisibleLines(),
                        getPreviewLines().size(),
                        14
                );
                previewScroll = Scroll.calculateScrollOffset(
                        mouseY,
                        PREVIEW_CONTENT_Y,
                        PREVIEW_CONTENT_H,
                        thumbHeight,
                        maxScroll
                );
                previewScrollSmoothing.snap(previewScroll, maxScroll);
            }
            return true;
        }
        if (button == 0 && presetScrollbarDragging) {
            int contentY = LIST_Y + 25;
            int addButtonY = LIST_Y + LIST_H - 26;
            int contentH = addButtonY - contentY - 5;
            int visibleRows = Math.max(1, contentH / LIST_ROW_H);
            int maxScroll = maxPresetScroll();
            int thumbHeight = Scroll.calculateThumbHeight(contentH, visibleRows, draftEffects.size(), 18);
            presetScroll = Scroll.calculateScrollOffset(mouseY, contentY, contentH, thumbHeight, maxScroll);
            presetScrollSmoothing.snap(presetScroll, maxScroll);
            return true;
        }
        return super.canvasMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected boolean canvasMouseScrolled(double mouseX, double mouseY, double delta) {
        if (!contextEntries.isEmpty()) {
            return true;
        }
        if (GuiTheme.hovering(mouseX, mouseY, PREVIEW_X, PREVIEW_Y + 43, PREVIEW_W, PREVIEW_H - 43)) {
            previewScroll = previewScrollSmoothing.wheel(previewScroll, delta, 1.0D / 3.0D, previewMaxScroll());
            return true;
        }
        if (GuiTheme.hovering(mouseX, mouseY, LIST_X, LIST_Y, LIST_W, LIST_H)) {
            presetScroll = presetScrollSmoothing.wheel(presetScroll, delta, 1.0D / 3.0D, maxPresetScroll());
            return true;
        }
        return super.canvasMouseScrolled(mouseX, mouseY, delta);
    }

    private int presetIndexAt(double mouseX, double mouseY) {
        int contentY = LIST_Y + 25;
        int addButtonY = LIST_Y + LIST_H - 26;
        int contentH = addButtonY - contentY - 5;
        if (!GuiTheme.hovering(mouseX, mouseY, LIST_X + 5, contentY, LIST_W - 18, contentH)) {
            return -1;
        }
        double visualPresetScroll = presetScrollSmoothing.follow(presetScroll, maxPresetScroll());
        int index = (int) Math.floor((mouseY - contentY) / LIST_ROW_H + visualPresetScroll);
        return index >= 0 && index < draftEffects.size() ? index : -1;
    }

    private int paletteSwatchIndexAt(double mouseX, double mouseY) {
        List<Integer> colors = parsePalette(current().paletteColors);
        for (int i = 0; i < colors.size(); i++) {
            int col = i % PALETTE_SWATCH_COLS;
            int row = i / PALETTE_SWATCH_COLS;
            int x = PALETTE_SWATCH_X + col * (PALETTE_SWATCH_CELL + PALETTE_SWATCH_GAP);
            int y = PALETTE_SWATCH_Y + row * (PALETTE_SWATCH_CELL + PALETTE_SWATCH_GAP);
            if (GuiTheme.hovering(mouseX, mouseY, x, y, PALETTE_SWATCH_CELL, PALETTE_SWATCH_CELL)) {
                return i;
            }
        }
        return -1;
    }

    private void openPresetContextMenu(int index, double mouseX, double mouseY) {
        List<ContextEntry> entries = new ArrayList<>();
        entries.add(new ContextEntry(
                Component.translatable("gui.textstudio.font.editor.context.copy_prefix"),
                this::copyEffectPrefix,
                true
        ));
        entries.add(new ContextEntry(
                Component.translatable("gui.textstudio.font.editor.context.duplicate"),
                () -> duplicatePreset(index),
                draftEffects.size() < MAX_PRESETS
        ));
        entries.add(new ContextEntry(
                Component.translatable("gui.textstudio.font.editor.context.reset"),
                () -> resetPreset(index),
                true
        ));
        entries.add(new ContextEntry(
                Component.translatable("gui.textstudio.font.editor.context.delete"),
                () -> deletePreset(index),
                draftEffects.size() > 1
        ));
        openContextMenu(entries, mouseX, mouseY);
    }

    private void openContextMenu(List<ContextEntry> entries, double mouseX, double mouseY) {
        contextEntries = List.copyOf(entries);
        int height = contextEntries.size() * CONTEXT_ROW_H + 4;
        int x = (int) mouseX + 6;
        int y = (int) mouseY + 6;
        if (x + CONTEXT_W > CANVAS_W - 4) {
            x = (int) mouseX - CONTEXT_W - 6;
        }
        if (y + height > CANVAS_H - 4) {
            y = (int) mouseY - height - 6;
        }
        contextX = Mth.clamp(x, 4, CANVAS_W - CONTEXT_W - 4);
        contextY = Mth.clamp(y, 4, CANVAS_H - height - 4);
    }

    private boolean handleContextMenuClick(double mouseX, double mouseY, int button) {
        if (button != 0 || contextEntries.isEmpty()) {
            return false;
        }
        int height = contextEntries.size() * CONTEXT_ROW_H + 4;
        if (!GuiTheme.hovering(mouseX, mouseY, contextX, contextY, CONTEXT_W, height)) {
            return false;
        }
        int row = (int) ((mouseY - contextY - 2) / CONTEXT_ROW_H);
        if (row >= 0 && row < contextEntries.size()) {
            ContextEntry entry = contextEntries.get(row);
            if (entry.active()) {
                entry.action().run();
            }
        }
        return true;
    }

    private void closeContextMenu() {
        contextEntries = List.of();
    }

    private int maxPresetScroll() {
        int contentY = LIST_Y + 25;
        int addButtonY = LIST_Y + LIST_H - 26;
        int contentH = addButtonY - contentY - 5;
        int visibleRows = Math.max(1, contentH / LIST_ROW_H);
        return Math.max(0, draftEffects.size() - visibleRows);
    }

    private void clampPresetScroll() {
        presetScroll = Mth.clamp(presetScroll, 0, maxPresetScroll());
    }

    private void clampPresetScrollToSelection() {
        int contentY = LIST_Y + 25;
        int addButtonY = LIST_Y + LIST_H - 26;
        int contentH = addButtonY - contentY - 5;
        int visibleRows = Math.max(1, contentH / LIST_ROW_H);
        if (selectedPreset < presetScroll) {
            presetScroll = selectedPreset;
        } else if (selectedPreset >= presetScroll + visibleRows) {
            presetScroll = selectedPreset - visibleRows + 1;
        }
        clampPresetScroll();
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
}
