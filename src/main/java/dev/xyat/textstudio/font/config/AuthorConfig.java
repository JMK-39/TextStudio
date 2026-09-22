package dev.xyat.textstudio.font.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import dev.xyat.textstudio.font.FontModule;
import dev.xyat.textstudio.font.api.AuthorAPI;
import dev.xyat.textstudio.font.common.annotation.KTModule;
import dev.xyat.kineticcore.api.runtime.KineticPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@KTModule
public class AuthorConfig {
    private static final Path CONFIG_DIR = KineticPaths.configDirectory().resolve("kineticcore");
    private static final Path FILE_PATH = CONFIG_DIR.resolve("textstudio_effects.toml");
    private static CommentedFileConfig configData;
    private static int refreshIntervalMs = 33;
    private static int maxAnimatedGlyphs = 512;
    private static int extraPassBudget = 8;

    public static class EffectSettings {
        // 颜色与发光 (Color & Glow)
        public boolean useRainbow = false;      public double rainbowSpeed = 0.15;    public double rainbowSpread = 0.05;
        public boolean pulse = false;           public double pulseBase = 0.5;        public double pulseAmp = 1.0;         public double pulseSpeed = 1.0;
        public boolean fade = false;            public double fadeMin = 0.2;          public double fadeSpeed = 1.0;
        public boolean neonFlicker = false;     public double neonFlickerSpeed = 1.0;

        // 色差与霓虹边缘发光 (Chromatic Aberration)
        public boolean chromatic = false;       public double chromaticOffset = 1.5;  public double chromaticAlpha = 0.6;
        public boolean chromaticPulse = false;
        public boolean shimmer = false;         public double shimmerSpeed = 1.0;     public double shimmerWidth = 2.5;    public double shimmerStrength = 0.75;
        public boolean sparkle = false;         public double sparkleSpeed = 1.0;     public double sparkleChance = 0.08; public double sparkleStrength = 0.85;

        // 物理位移 (Physical Transform)
        public boolean wave = false;            public double waveAmp = 1.0;          public double waveSpeed = 1.0;
        public boolean bounce = false;          public double bounceAmp = 1.0;        public double bounceSpeed = 1.0;
        public boolean shake = false;           public double shakeAmp = 1.0;         public double shakeSpeed = 1.0;
        public boolean swing = false;           public double swingAmp = 1.0;         public double swingSpeed = 1.0;
        public boolean wiggle = false;          public double wiggleAmp = 1.0;        public double wiggleSpeed = 1.0;
        public boolean turb = false;            public double turbAmp = 1.0;          public double turbSpeed = 1.0;
        public boolean pend = false;            public double pendAmp = 1.0;          public double pendSpeed = 1.0;
        public boolean orbit = false;           public double orbitAmp = 1.0;         public double orbitSpeed = 1.0;
        public boolean ripple = false;          public double rippleAmp = 1.0;        public double rippleSpeed = 1.0;
        public boolean drift = false;           public double driftAmp = 1.0;         public double driftSpeed = 1.0;

        // 撕裂与特殊演出 (Glitch & Special)
        public boolean glitch = false;          public double glitchAmp = 1.0;        public double glitchSpeed = 1.0;      public double glitchFlicker = 0.02;
        public boolean spasm = false;           public double spasmAmp = 1.0;         public double spasmSpeed = 1.0;
        public boolean blinkWave = false;       public double blinkWaveSpeed = 1.0;   public double blinkWaveDepth = 0.25;

        // 打字机设定 (Typewriter)
        public boolean typewriter = false;      public double typewriterSpeed = 1.0;  public boolean typewriterBack = false;

        // 自定义256色调色板 (Palette)
        public boolean palette = false;         public String paletteColors = "";     public double paletteSpeed = 1.0;
        public double paletteSpread = 0.05;     public boolean paletteFlash = false;
        public boolean noteBounce = false;      public double noteBounceAmp = 0.5;     public double noteBounceSpeed = 1.0;
        public boolean sweep = false;           public double sweepSpeed = 1.0;           public double sweepWidth = 3.0;          public double sweepStrength = 0.85;
        public boolean outline = false;         public double outlineWidth = 0.8;         public double outlineAlpha = 0.8;
        public boolean glow = false;            public double glowRadius = 1.0;           public double glowAlpha = 0.28;
        public boolean trail = false;           public double trailStrength = 1.0;        public double trailAlpha = 0.28;
        public boolean extrude = false;         public double extrudeDepth = 1.5;         public double extrudeAlpha = 0.5;

        public boolean glyphScale = false;      public double glyphScaleAmp = 0.18;     public double glyphScaleSpeed = 1.0;
        public boolean ecg = false;             public double ecgAmp = 1.0;             public double ecgSpeed = 1.0;          public double ecgWidth = 2.0;
        public boolean heartbeat = false;       public double heartbeatAmp = 1.0;       public double heartbeatSpeed = 1.0;
        public boolean glitchSpike = false;     public double glitchSpikeAmp = 1.0;     public double glitchSpikeSpeed = 1.0;  public double glitchSpikeChance = 0.15;
        public boolean signalLoss = false;      public double signalLossChance = 0.12;  public double signalLossSpeed = 1.0;

        public EffectSettings() {}

        public EffectSettings copy() {
            EffectSettings c = new EffectSettings();
            c.useRainbow = useRainbow; c.rainbowSpeed = rainbowSpeed; c.rainbowSpread = rainbowSpread;
            c.pulse = pulse;           c.pulseBase = pulseBase;       c.pulseAmp = pulseAmp;           c.pulseSpeed = pulseSpeed;
            c.fade = fade;             c.fadeMin = fadeMin;           c.fadeSpeed = fadeSpeed;
            c.neonFlicker = neonFlicker; c.neonFlickerSpeed = neonFlickerSpeed;
            c.chromatic = chromatic;   c.chromaticOffset = chromaticOffset; c.chromaticAlpha = chromaticAlpha; c.chromaticPulse = chromaticPulse;
            c.shimmer = shimmer;         c.shimmerSpeed = shimmerSpeed;       c.shimmerWidth = shimmerWidth;       c.shimmerStrength = shimmerStrength;
            c.sparkle = sparkle;         c.sparkleSpeed = sparkleSpeed;       c.sparkleChance = sparkleChance;     c.sparkleStrength = sparkleStrength;
            c.wave = wave;             c.waveAmp = waveAmp;           c.waveSpeed = waveSpeed;
            c.bounce = bounce;         c.bounceAmp = bounceAmp;       c.bounceSpeed = bounceSpeed;
            c.shake = shake;           c.shakeAmp = shakeAmp;         c.shakeSpeed = shakeSpeed;
            c.swing = swing;           c.swingAmp = swingAmp;         c.swingSpeed = swingSpeed;
            c.wiggle = wiggle;         c.wiggleAmp = wiggleAmp;       c.wiggleSpeed = wiggleSpeed;
            c.turb = turb;             c.turbAmp = turbAmp;           c.turbSpeed = turbSpeed;
            c.pend = pend;             c.pendAmp = pendAmp;           c.pendSpeed = pendSpeed;
            c.orbit = orbit;           c.orbitAmp = orbitAmp;         c.orbitSpeed = orbitSpeed;
            c.ripple = ripple;         c.rippleAmp = rippleAmp;       c.rippleSpeed = rippleSpeed;
            c.drift = drift;           c.driftAmp = driftAmp;         c.driftSpeed = driftSpeed;
            c.glitch = glitch;         c.glitchAmp = glitchAmp;       c.glitchSpeed = glitchSpeed;     c.glitchFlicker = glitchFlicker;
            c.spasm = spasm;           c.spasmAmp = spasmAmp;         c.spasmSpeed = spasmSpeed;
            c.blinkWave = blinkWave;   c.blinkWaveSpeed = blinkWaveSpeed; c.blinkWaveDepth = blinkWaveDepth;
            c.typewriter = typewriter; c.typewriterSpeed = typewriterSpeed; c.typewriterBack = typewriterBack;

            c.palette = palette;       c.paletteColors = paletteColors; c.paletteSpeed = paletteSpeed;
            c.paletteSpread = paletteSpread; c.paletteFlash = paletteFlash;
            c.noteBounce = noteBounce; c.noteBounceAmp = noteBounceAmp; c.noteBounceSpeed = noteBounceSpeed;
            c.sweep = sweep; c.sweepSpeed = sweepSpeed; c.sweepWidth = sweepWidth; c.sweepStrength = sweepStrength;
            c.outline = outline; c.outlineWidth = outlineWidth; c.outlineAlpha = outlineAlpha;
            c.glow = glow; c.glowRadius = glowRadius; c.glowAlpha = glowAlpha;
            c.trail = trail; c.trailStrength = trailStrength; c.trailAlpha = trailAlpha;
            c.extrude = extrude; c.extrudeDepth = extrudeDepth; c.extrudeAlpha = extrudeAlpha;
            c.glyphScale = glyphScale; c.glyphScaleAmp = glyphScaleAmp; c.glyphScaleSpeed = glyphScaleSpeed;
            c.ecg = ecg; c.ecgAmp = ecgAmp; c.ecgSpeed = ecgSpeed; c.ecgWidth = ecgWidth;
            c.heartbeat = heartbeat; c.heartbeatAmp = heartbeatAmp; c.heartbeatSpeed = heartbeatSpeed;
            c.glitchSpike = glitchSpike; c.glitchSpikeAmp = glitchSpikeAmp; c.glitchSpikeSpeed = glitchSpikeSpeed; c.glitchSpikeChance = glitchSpikeChance;
            c.signalLoss = signalLoss; c.signalLossChance = signalLossChance; c.signalLossSpeed = signalLossSpeed;
            return c;
        }
    }

    public static final List<EffectSettings> EFFECTS = new ArrayList<>();
    private static final List<EffectSettings> PUBLIC_EFFECTS = new ArrayList<>();
    private static final EffectSettings AUTHOR_NOTE_EFFECT = createAuthorNoteEffect();
    private static final EffectSettings AUTHOR_NOTE_PUBLIC_EFFECT = toPublicSafe(AUTHOR_NOTE_EFFECT);

    public static void load() {
        try {
            if (!Files.exists(CONFIG_DIR)) Files.createDirectories(CONFIG_DIR);
            configData = CommentedFileConfig.builder(FILE_PATH).sync().preserveInsertionOrder().writingMode(WritingMode.REPLACE).build();
            configData.load();
            if (!configData.contains("e")) {
                setupDefaults();
                configData.save();
            }
            readPerformanceValues();
            readValues();
        } catch (Exception e) {
            FontModule.LOGGER.error("AuthorConfig Load Failed", e);
        }
    }

    private static void setupDefaults() {
        List<com.electronwill.nightconfig.core.Config> defaults = new ArrayList<>();
        addCuratedPresets(defaults);
        configData.set("e", defaults);
        configData.set("performance_refresh_ms", 33);
        configData.set("performance_max_animated_glyphs", 512);
        configData.set("performance_extra_pass_budget", 8);
    }

    private static void addCuratedPresets(List<com.electronwill.nightconfig.core.Config> defaults) {
        addPreset(defaults, s -> { s.useRainbow = true; s.rainbowSpeed = 0.05; s.rainbowSpread = 0.05; });
        addPreset(defaults, s -> { s.pulse = true; s.pulseBase = 0.68; s.pulseAmp = 0.42; s.pulseSpeed = 0.8; });
        addPreset(defaults, s -> { s.wave = true; s.waveAmp = 0.8; s.waveSpeed = 0.5; });
        addPreset(defaults, s -> { s.bounce = true; s.bounceAmp = 1.2; s.bounceSpeed = 1.5; });
        addPreset(defaults, s -> { s.swing = true; s.swingAmp = 0.5; s.swingSpeed = 0.7; });
        addPreset(defaults, s -> { s.fade = true; s.fadeMin = 0.3; s.fadeSpeed = 0.6; });
        addPreset(defaults, s -> { s.pend = true; s.pendAmp = 1.0; s.pendSpeed = 0.7; });
        addPreset(defaults, s -> { s.palette = true; s.paletteColors = "FF4FD8|5CE1FF|8B68FF"; s.paletteSpeed = 0.45; });
        addPreset(defaults, s -> { s.palette = true; s.paletteColors = "6BEAFF|B789FF|FF9DE1"; s.paletteSpeed = 0.2; s.sparkle = true; s.sparkleSpeed = 1.4; s.sparkleChance = 0.11; s.sparkleStrength = 0.95; });
        addPreset(defaults, s -> { s.palette = true; s.paletteColors = "2EE8FF|7A5CFF|FF4FC8"; s.paletteSpeed = 0.35; s.shimmer = true; s.shimmerSpeed = 1.15; s.shimmerWidth = 3.2; s.shimmerStrength = 0.9; });

        addPreset(defaults, s -> { s.typewriter = true; s.typewriterSpeed = 1.2; });
        addPreset(defaults, s -> { s.typewriter = true; s.fade = true; s.typewriterSpeed = 0.8; s.fadeMin = 0.45; });
        addPreset(defaults, s -> { s.typewriter = true; s.neonFlicker = true; s.neonFlickerSpeed = 0.8; });
        addPreset(defaults, s -> { s.palette = true; s.paletteColors = "51F6C9|53A4FF|BA67FF"; s.orbit = true; s.orbitAmp = 0.65; s.orbitSpeed = 0.7; });
        addPreset(defaults, s -> { s.palette = true; s.paletteColors = "27D7FF|6EF5FF|8A7CFF"; s.ripple = true; s.rippleAmp = 0.8; s.rippleSpeed = 0.85; });
        addPreset(defaults, s -> { s.palette = true; s.paletteColors = "F2FAFF|9FE7FF|B8C5FF"; s.drift = true; s.driftAmp = 0.55; s.driftSpeed = 0.45; });
        addPreset(defaults, s -> { s.glyphScale = true; s.glyphScaleAmp = 0.14; s.glyphScaleSpeed = 1.0; s.heartbeat = true; s.heartbeatAmp = 0.55; s.heartbeatSpeed = 0.8; s.pulse = true; s.pulseBase = 0.78; s.pulseAmp = 0.32; });
        addPreset(defaults, s -> { s.ecg = true; s.ecgAmp = 1.0; s.ecgSpeed = 1.0; s.ecgWidth = 2.0; s.pulse = true; s.pulseBase = 0.72; s.pulseAmp = 0.38; });
        addPreset(defaults, s -> { s.palette = true; s.paletteColors = "48E7FF|A16CFF|FF68CB"; s.noteBounce = true; s.noteBounceAmp = 0.75; s.noteBounceSpeed = 1.1; });
        addPreset(defaults, s -> { s.useRainbow = true; s.rainbowSpeed = 0.2; s.rainbowSpread = 0.05; s.bounce = true; s.bounceAmp = 0.65; s.swing = true; s.swingAmp = 0.35; });

        addPreset(defaults, s -> { s.glitch = true; s.glitchFlicker = 0.08; s.glitchAmp = 2.0; });
        addPreset(defaults, s -> { s.signalLoss = true; s.signalLossChance = 0.12; s.signalLossSpeed = 1.4; s.chromatic = true; s.chromaticOffset = 1.2; s.chromaticAlpha = 0.45; });
        addPreset(defaults, s -> { s.glitchSpike = true; s.glitchSpikeAmp = 1.0; s.glitchSpikeSpeed = 1.3; s.glitchSpikeChance = 0.16; s.chromatic = true; s.chromaticOffset = 1.5; s.chromaticAlpha = 0.55; });
        addPreset(defaults, s -> { s.chromatic = true; s.glitch = true; s.chromaticPulse = true; s.glitchAmp = 2.2; s.chromaticOffset = 2.0; });
        addPreset(defaults, s -> { s.palette = true; s.paletteColors = "55E7FF|BD71FF|FF81D5|8CFFEF"; s.paletteSpeed = 0.28; s.chromatic = true; s.chromaticOffset = 1.0; s.chromaticAlpha = 0.3; s.turb = true; s.turbAmp = 0.35; s.neonFlicker = true; s.sweep = true; s.sweepSpeed = 0.9; s.sweepWidth = 2.8; });
        addPreset(defaults, s -> { s.palette = true; s.paletteColors = "FF2020|9B0000"; s.paletteFlash = true; s.shake = true; s.shakeAmp = 1.5; s.paletteSpeed = 4.0; });
        addPreset(defaults, s -> { s.palette = true; s.paletteColors = "FF0000|FFFF00|00FF00|00FFFF|0000FF|FF00FF"; s.typewriter = true; s.typewriterSpeed = 2.0; s.glitch = true; s.glitchAmp = 0.55; });
        addPreset(defaults, s -> { s.chromatic = true; s.glitch = true; s.spasm = true; s.chromaticOffset = 4.0; s.chromaticAlpha = 0.4; s.spasmAmp = 1.2; });
        addPreset(defaults, s -> { s.chromatic = true; s.fade = true; s.swing = true; s.chromaticOffset = 3.0; s.chromaticAlpha = 0.3; s.trail = true; s.trailStrength = 1.1; s.trailAlpha = 0.22; });
        addPreset(defaults, s -> { s.useRainbow = true; s.rainbowSpeed = 0.3; s.rainbowSpread = 0.05; s.chromatic = true; s.glitch = true; s.turb = true; s.signalLoss = true; s.signalLossChance = 0.05; s.chromaticOffset = 2.6; });

        addPreset(defaults, s -> { s.outline = true; s.outlineWidth = 0.75; s.outlineAlpha = 0.85; });
        addPreset(defaults, s -> { s.glow = true; s.glowRadius = 1.0; s.glowAlpha = 0.3; s.pulse = true; s.pulseBase = 0.82; s.pulseAmp = 0.25; s.pulseSpeed = 0.8; });
        addPreset(defaults, s -> { s.palette = true; s.paletteColors = "A855F7|4DEBFF|F06CFF"; s.paletteSpeed = 0.32; s.glow = true; s.glowRadius = 1.2; s.glowAlpha = 0.32; s.shimmer = true; s.shimmerSpeed = 0.8; s.shimmerStrength = 0.65; });
        addPreset(defaults, s -> { s.palette = true; s.paletteColors = "2FE7FF|4D8DFF|7D5BFF"; s.paletteSpeed = 0.15; s.sweep = true; s.sweepSpeed = 1.2; s.sweepWidth = 2.2; s.sweepStrength = 1.0; });
        addPreset(defaults, s -> { s.palette = true; s.paletteColors = "5DEBFF|E8FCFF|C979FF|73FFD8"; s.paletteSpeed = 0.24; s.sweep = true; s.sweepSpeed = 0.85; s.sweepWidth = 2.6; s.chromatic = true; s.chromaticOffset = 0.8; s.chromaticAlpha = 0.2; });
        addPreset(defaults, s -> { s.trail = true; s.trailStrength = 1.2; s.trailAlpha = 0.28; s.drift = true; s.driftAmp = 0.7; s.driftSpeed = 0.55; s.fade = true; s.fadeMin = 0.58; s.fadeSpeed = 0.45; });
        addPreset(defaults, s -> { s.useRainbow = true; s.rainbowSpeed = 0.08; s.rainbowSpread = 0.07; s.shimmer = true; s.shimmerSpeed = 1.8; s.shimmerStrength = 0.8; s.drift = true; s.driftAmp = 0.35; s.trail = true; s.trailStrength = 0.9; s.trailAlpha = 0.2; });
        addPreset(defaults, s -> { s.palette = true; s.paletteColors = "FFE37A|FF9F43|FF5E5E"; s.paletteSpeed = 0.16; s.extrude = true; s.extrudeDepth = 1.8; s.extrudeAlpha = 0.55; });
        addPreset(defaults, s -> { s.palette = true; s.paletteColors = "FF2A00|FF7800|FFD21A|FFF2A0"; s.paletteSpeed = 0.7; s.wave = true; s.waveAmp = 0.35; s.glow = true; s.glowRadius = 1.0; s.glowAlpha = 0.24; });
        addPreset(defaults, s -> { s.palette = true; s.paletteColors = "3CF7FF|5A68FF|FF4DB8"; s.paletteSpeed = 0.28; s.extrude = true; s.extrudeDepth = 1.4; s.extrudeAlpha = 0.4; s.chromatic = true; s.chromaticOffset = 1.0; s.chromaticAlpha = 0.25; s.glitch = true; s.glitchAmp = 0.45; });

        addPreset(defaults, s -> { s.palette = true; s.paletteColors = "064A8C|008EA8|38E8FF"; s.paletteSpeed = 0.3; s.pulse = true; s.pulseBase = 0.76; s.pulseAmp = 0.28; s.glow = true; s.glowRadius = 0.9; s.glowAlpha = 0.22; s.drift = true; s.driftAmp = 0.25; });
        addPreset(defaults, s -> { s.palette = true; s.paletteColors = "FFF7C2|FFD700|FF9F1C"; s.paletteSpeed = 0.22; s.pulse = true; s.pulseSpeed = 1.3; s.sweep = true; s.sweepSpeed = 0.7; s.sweepWidth = 2.5; });
        addPreset(defaults, s -> { s.palette = true; s.paletteColors = "101A10|49FF42|0F8E35"; s.paletteSpeed = 0.55; s.glitch = true; s.glitchAmp = 0.65; s.signalLoss = true; s.signalLossChance = 0.035; });
        addPreset(defaults, s -> { s.palette = true; s.paletteColors = "FF1C2C|236BFF"; s.paletteFlash = true; s.paletteSpread = 0.0; s.paletteSpeed = 3.0; s.glow = true; s.glowRadius = 0.8; s.glowAlpha = 0.18; });
        addPreset(defaults, s -> { s.palette = true; s.paletteColors = "2D4435|39FF73|2D4435"; s.paletteSpeed = 0.32; s.sweep = true; s.sweepSpeed = 1.0; s.sweepWidth = 1.6; s.sweepStrength = 0.95; });
        addPreset(defaults, s -> { s.palette = true; s.paletteColors = "00F5D4|00BBF9|9B5DE5|F15BB5"; s.paletteSpeed = 0.45; s.orbit = true; s.orbitAmp = 0.45; s.shimmer = true; s.shimmerSpeed = 0.8; s.pulse = true; s.pulseBase = 0.78; s.pulseAmp = 0.28; });
        addPreset(defaults, s -> { s.useRainbow = true; s.rainbowSpeed = 0.14; s.rainbowSpread = 0.06; s.glow = true; s.glowRadius = 1.1; s.glowAlpha = 0.26; s.chromatic = true; s.chromaticOffset = 0.9; s.chromaticAlpha = 0.2; });
        addPreset(defaults, s -> { s.glyphScale = true; s.glyphScaleAmp = 0.12; s.heartbeat = true; s.heartbeatAmp = 0.45; s.glitchSpike = true; s.glitchSpikeAmp = 0.55; s.glitchSpikeChance = 0.08; s.signalLoss = true; s.signalLossChance = 0.025; s.glow = true; s.glowRadius = 0.8; s.glowAlpha = 0.18; });
        addPreset(defaults, s -> { s.turb = true; s.spasm = true; s.fade = true; s.turbAmp = 2.0; s.spasmAmp = 1.2; s.fadeMin = 0.22; s.trail = true; s.trailStrength = 0.85; s.trailAlpha = 0.18; });
        addPreset(defaults, s -> { s.palette = true; s.paletteColors = "35F2FF|6A7CFF|B05CFF|FF5FC7|FFD35A"; s.paletteSpeed = 0.32; s.glyphScale = true; s.glyphScaleAmp = 0.1; s.heartbeat = true; s.heartbeatAmp = 0.32; s.glitchSpike = true; s.glitchSpikeAmp = 0.45; s.signalLoss = true; s.signalLossChance = 0.025; s.chromatic = true; s.chromaticOffset = 0.8; s.chromaticAlpha = 0.2; s.sweep = true; s.sweepSpeed = 0.8; s.sweepWidth = 2.8; });
    }

    private static void readPerformanceValues() {
        refreshIntervalMs = Math.max(0, Math.min(250, integer(configData, "performance_refresh_ms", 33)));
        maxAnimatedGlyphs = Math.max(32, Math.min(8192, integer(configData, "performance_max_animated_glyphs", 512)));
        extraPassBudget = Math.max(0, Math.min(16, integer(configData, "performance_extra_pass_budget", 8)));
    }

    public static int getRefreshIntervalMs() {
        return refreshIntervalMs;
    }

    public static int getMaxAnimatedGlyphs() {
        return maxAnimatedGlyphs;
    }

    public static int getExtraPassBudget() {
        return extraPassBudget;
    }

    private static void addPreset(List<com.electronwill.nightconfig.core.Config> defaults, Consumer<EffectSettings> configurator) {
        EffectSettings s = new EffectSettings();
        configurator.accept(s);
        defaults.add(toConfigObj(s));
    }

    private static com.electronwill.nightconfig.core.Config toConfigObj(EffectSettings s) {
        com.electronwill.nightconfig.core.Config c = com.electronwill.nightconfig.core.Config.inMemory();
        EffectSettings d = new EffectSettings();

        putBool(c, "r", s.useRainbow); putNum(c, "r0", s.rainbowSpeed, d.rainbowSpeed); putNum(c, "r1", s.rainbowSpread, d.rainbowSpread);
        putBool(c, "p", s.pulse); putNum(c, "p0", s.pulseBase, d.pulseBase); putNum(c, "p1", s.pulseAmp, d.pulseAmp); putNum(c, "p2", s.pulseSpeed, d.pulseSpeed);
        putBool(c, "f", s.fade); putNum(c, "f0", s.fadeMin, d.fadeMin); putNum(c, "f1", s.fadeSpeed, d.fadeSpeed);
        putBool(c, "n", s.neonFlicker); putNum(c, "n0", s.neonFlickerSpeed, d.neonFlickerSpeed);
        putBool(c, "c", s.chromatic); putNum(c, "c0", s.chromaticOffset, d.chromaticOffset); putNum(c, "c1", s.chromaticAlpha, d.chromaticAlpha); putBool(c, "c2", s.chromaticPulse);
        putBool(c, "h", s.shimmer); putNum(c, "h0", s.shimmerSpeed, d.shimmerSpeed); putNum(c, "h1", s.shimmerWidth, d.shimmerWidth); putNum(c, "h2", s.shimmerStrength, d.shimmerStrength);
        putBool(c, "l", s.sparkle); putNum(c, "l0", s.sparkleSpeed, d.sparkleSpeed); putNum(c, "l1", s.sparkleChance, d.sparkleChance); putNum(c, "l2", s.sparkleStrength, d.sparkleStrength);
        putBool(c, "w", s.wave); putNum(c, "w0", s.waveAmp, d.waveAmp); putNum(c, "w1", s.waveSpeed, d.waveSpeed);
        putBool(c, "b", s.bounce); putNum(c, "b0", s.bounceAmp, d.bounceAmp); putNum(c, "b1", s.bounceSpeed, d.bounceSpeed);
        putBool(c, "s", s.shake); putNum(c, "s0", s.shakeAmp, d.shakeAmp); putNum(c, "s1", s.shakeSpeed, d.shakeSpeed);
        putBool(c, "x", s.swing); putNum(c, "x0", s.swingAmp, d.swingAmp); putNum(c, "x1", s.swingSpeed, d.swingSpeed);
        putBool(c, "g", s.wiggle); putNum(c, "g0", s.wiggleAmp, d.wiggleAmp); putNum(c, "g1", s.wiggleSpeed, d.wiggleSpeed);
        putBool(c, "u", s.turb); putNum(c, "u0", s.turbAmp, d.turbAmp); putNum(c, "u1", s.turbSpeed, d.turbSpeed);
        putBool(c, "d", s.pend); putNum(c, "d0", s.pendAmp, d.pendAmp); putNum(c, "d1", s.pendSpeed, d.pendSpeed);
        putBool(c, "o", s.orbit); putNum(c, "o0", s.orbitAmp, d.orbitAmp); putNum(c, "o1", s.orbitSpeed, d.orbitSpeed);
        putBool(c, "i", s.ripple); putNum(c, "i0", s.rippleAmp, d.rippleAmp); putNum(c, "i1", s.rippleSpeed, d.rippleSpeed);
        putBool(c, "v", s.drift); putNum(c, "v0", s.driftAmp, d.driftAmp); putNum(c, "v1", s.driftSpeed, d.driftSpeed);
        putBool(c, "k", s.glitch); putNum(c, "k0", s.glitchAmp, d.glitchAmp); putNum(c, "k1", s.glitchSpeed, d.glitchSpeed); putNum(c, "k2", s.glitchFlicker, d.glitchFlicker);
        putBool(c, "z", s.spasm); putNum(c, "z0", s.spasmAmp, d.spasmAmp); putNum(c, "z1", s.spasmSpeed, d.spasmSpeed);
        putBool(c, "j", s.blinkWave); putNum(c, "j0", s.blinkWaveSpeed, d.blinkWaveSpeed); putNum(c, "j1", s.blinkWaveDepth, d.blinkWaveDepth);
        putBool(c, "t", s.typewriter); putNum(c, "t0", s.typewriterSpeed, d.typewriterSpeed); putBool(c, "t1", s.typewriterBack);
        putBool(c, "q", s.palette);
        if (s.paletteColors != null && !s.paletteColors.isBlank()) c.set("q0", s.paletteColors);
        putNum(c, "q1", s.paletteSpeed, d.paletteSpeed); putNum(c, "q2", s.paletteSpread, d.paletteSpread); putBool(c, "q3", s.paletteFlash);
        putBool(c, "nb", s.noteBounce); putNum(c, "nb0", s.noteBounceAmp, d.noteBounceAmp); putNum(c, "nb1", s.noteBounceSpeed, d.noteBounceSpeed);
        putBool(c, "sw", s.sweep); putNum(c, "sw0", s.sweepSpeed, d.sweepSpeed); putNum(c, "sw1", s.sweepWidth, d.sweepWidth); putNum(c, "sw2", s.sweepStrength, d.sweepStrength);
        putBool(c, "ou", s.outline); putNum(c, "ou0", s.outlineWidth, d.outlineWidth); putNum(c, "ou1", s.outlineAlpha, d.outlineAlpha);
        putBool(c, "go", s.glow); putNum(c, "go0", s.glowRadius, d.glowRadius); putNum(c, "go1", s.glowAlpha, d.glowAlpha);
        putBool(c, "tr", s.trail); putNum(c, "tr0", s.trailStrength, d.trailStrength); putNum(c, "tr1", s.trailAlpha, d.trailAlpha);
        putBool(c, "xd", s.extrude); putNum(c, "xd0", s.extrudeDepth, d.extrudeDepth); putNum(c, "xd1", s.extrudeAlpha, d.extrudeAlpha);
        putBool(c, "sc", s.glyphScale); putNum(c, "sc0", s.glyphScaleAmp, d.glyphScaleAmp); putNum(c, "sc1", s.glyphScaleSpeed, d.glyphScaleSpeed);
        putBool(c, "ec", s.ecg); putNum(c, "ec0", s.ecgAmp, d.ecgAmp); putNum(c, "ec1", s.ecgSpeed, d.ecgSpeed); putNum(c, "ec2", s.ecgWidth, d.ecgWidth);
        putBool(c, "hb", s.heartbeat); putNum(c, "hb0", s.heartbeatAmp, d.heartbeatAmp); putNum(c, "hb1", s.heartbeatSpeed, d.heartbeatSpeed);
        putBool(c, "gx", s.glitchSpike); putNum(c, "gx0", s.glitchSpikeAmp, d.glitchSpikeAmp); putNum(c, "gx1", s.glitchSpikeSpeed, d.glitchSpikeSpeed); putNum(c, "gx2", s.glitchSpikeChance, d.glitchSpikeChance);
        putBool(c, "sl", s.signalLoss); putNum(c, "sl0", s.signalLossChance, d.signalLossChance); putNum(c, "sl1", s.signalLossSpeed, d.signalLossSpeed);
        return c;
    }

    private static void readValues() {
        EFFECTS.clear();
        List<com.electronwill.nightconfig.core.Config> effectConfigs = configData.get("e");
        if (effectConfigs == null) return;

        for (com.electronwill.nightconfig.core.Config c : effectConfigs) {
            EffectSettings s = new EffectSettings();
            s.useRainbow = bool(c, "r", false); s.rainbowSpeed = num(c, "r0", 0.15); s.rainbowSpread = num(c, "r1", 0.05);
            s.pulse = bool(c, "p", false); s.pulseBase = num(c, "p0", 0.5); s.pulseAmp = num(c, "p1", 1.0); s.pulseSpeed = num(c, "p2", 1.0);
            s.fade = bool(c, "f", false); s.fadeMin = num(c, "f0", 0.2); s.fadeSpeed = num(c, "f1", 1.0);
            s.neonFlicker = bool(c, "n", false); s.neonFlickerSpeed = num(c, "n0", 1.0);
            s.chromatic = bool(c, "c", false); s.chromaticOffset = num(c, "c0", 1.5); s.chromaticAlpha = num(c, "c1", 0.6); s.chromaticPulse = bool(c, "c2", false);
            s.shimmer = bool(c, "h", false); s.shimmerSpeed = num(c, "h0", 1.0); s.shimmerWidth = num(c, "h1", 2.5); s.shimmerStrength = num(c, "h2", 0.75);
            s.sparkle = bool(c, "l", false); s.sparkleSpeed = num(c, "l0", 1.0); s.sparkleChance = num(c, "l1", 0.08); s.sparkleStrength = num(c, "l2", 0.85);
            s.wave = bool(c, "w", false); s.waveAmp = num(c, "w0", 1.0); s.waveSpeed = num(c, "w1", 1.0);
            s.bounce = bool(c, "b", false); s.bounceAmp = num(c, "b0", 1.0); s.bounceSpeed = num(c, "b1", 1.0);
            s.shake = bool(c, "s", false); s.shakeAmp = num(c, "s0", 1.0); s.shakeSpeed = num(c, "s1", 1.0);
            s.swing = bool(c, "x", false); s.swingAmp = num(c, "x0", 1.0); s.swingSpeed = num(c, "x1", 1.0);
            s.wiggle = bool(c, "g", false); s.wiggleAmp = num(c, "g0", 1.0); s.wiggleSpeed = num(c, "g1", 1.0);
            s.turb = bool(c, "u", false); s.turbAmp = num(c, "u0", 1.0); s.turbSpeed = num(c, "u1", 1.0);
            s.pend = bool(c, "d", false); s.pendAmp = num(c, "d0", 1.0); s.pendSpeed = num(c, "d1", 1.0);
            s.orbit = bool(c, "o", false); s.orbitAmp = num(c, "o0", 1.0); s.orbitSpeed = num(c, "o1", 1.0);
            s.ripple = bool(c, "i", false); s.rippleAmp = num(c, "i0", 1.0); s.rippleSpeed = num(c, "i1", 1.0);
            s.drift = bool(c, "v", false); s.driftAmp = num(c, "v0", 1.0); s.driftSpeed = num(c, "v1", 1.0);
            s.glitch = bool(c, "k", false); s.glitchAmp = num(c, "k0", 1.0); s.glitchSpeed = num(c, "k1", 1.0); s.glitchFlicker = num(c, "k2", 0.02);
            s.spasm = bool(c, "z", false); s.spasmAmp = num(c, "z0", 1.0); s.spasmSpeed = num(c, "z1", 1.0);
            s.blinkWave = bool(c, "j", false); s.blinkWaveSpeed = num(c, "j0", 1.0); s.blinkWaveDepth = num(c, "j1", 0.25);
            s.typewriter = bool(c, "t", false); s.typewriterSpeed = num(c, "t0", 1.0); s.typewriterBack = bool(c, "t1", false);
            s.palette = bool(c, "q", false); s.paletteColors = str(c, "q0", ""); s.paletteSpeed = num(c, "q1", 1.0); s.paletteSpread = num(c, "q2", 0.05); s.paletteFlash = bool(c, "q3", false);
            s.noteBounce = bool(c, "nb", false); s.noteBounceAmp = num(c, "nb0", 0.5); s.noteBounceSpeed = num(c, "nb1", 1.0);
            s.sweep = bool(c, "sw", false); s.sweepSpeed = num(c, "sw0", 1.0); s.sweepWidth = num(c, "sw1", 3.0); s.sweepStrength = num(c, "sw2", 0.85);
            s.outline = bool(c, "ou", false); s.outlineWidth = num(c, "ou0", 0.8); s.outlineAlpha = num(c, "ou1", 0.8);
            s.glow = bool(c, "go", false); s.glowRadius = num(c, "go0", 1.0); s.glowAlpha = num(c, "go1", 0.28);
            s.trail = bool(c, "tr", false); s.trailStrength = num(c, "tr0", 1.0); s.trailAlpha = num(c, "tr1", 0.28);
            s.extrude = bool(c, "xd", false); s.extrudeDepth = num(c, "xd0", 1.5); s.extrudeAlpha = num(c, "xd1", 0.5);
            s.glyphScale = bool(c, "sc", false); s.glyphScaleAmp = num(c, "sc0", 0.18); s.glyphScaleSpeed = num(c, "sc1", 1.0);
            s.ecg = bool(c, "ec", false); s.ecgAmp = num(c, "ec0", 1.0); s.ecgSpeed = num(c, "ec1", 1.0); s.ecgWidth = num(c, "ec2", 2.0);
            s.heartbeat = bool(c, "hb", false); s.heartbeatAmp = num(c, "hb0", 1.0); s.heartbeatSpeed = num(c, "hb1", 1.0);
            s.glitchSpike = bool(c, "gx", false); s.glitchSpikeAmp = num(c, "gx0", 1.0); s.glitchSpikeSpeed = num(c, "gx1", 1.0); s.glitchSpikeChance = num(c, "gx2", 0.15);
            s.signalLoss = bool(c, "sl", false); s.signalLossChance = num(c, "sl0", 0.12); s.signalLossSpeed = num(c, "sl1", 1.0);
            EFFECTS.add(s);
        }
        rebuildPublicEffects();
    }

    public static void save() {
        if (configData == null) return;
        List<com.electronwill.nightconfig.core.Config> configsToSave = new ArrayList<>();
        for (EffectSettings effect : EFFECTS) configsToSave.add(toConfigObj(effect));
        configData.set("e", configsToSave);
        configData.save();
        rebuildPublicEffects();
    }

    public static EffectSettings getRenderSettings(int effectId, boolean advancedAllowed) {
        if (effectId == AuthorAPI.SPECIAL_AUTHOR_EFFECT) {
            return advancedAllowed ? AUTHOR_NOTE_EFFECT : AUTHOR_NOTE_PUBLIC_EFFECT;
        }
        if (EFFECTS.isEmpty()) return null;
        int index = Math.max(0, Math.min(effectId - 1, EFFECTS.size() - 1));
        if (advancedAllowed || PUBLIC_EFFECTS.size() != EFFECTS.size()) return EFFECTS.get(index);
        return PUBLIC_EFFECTS.get(index);
    }

    public static boolean hasAdvancedFeatures(int effectId) {
        if (effectId == AuthorAPI.SPECIAL_AUTHOR_EFFECT) return true;
        if (effectId < 1 || effectId > EFFECTS.size()) return false;
        return hasAdvancedFeatures(EFFECTS.get(effectId - 1));
    }

    private static void rebuildPublicEffects() {
        PUBLIC_EFFECTS.clear();
        for (EffectSettings effect : EFFECTS) PUBLIC_EFFECTS.add(toPublicSafe(effect));
    }

    public static EffectSettings toPublicSafeSettings(EffectSettings source) {
        return source == null ? null : toPublicSafe(source);
    }

    private static EffectSettings toPublicSafe(EffectSettings source) {
        EffectSettings safe = new EffectSettings();
        safe.useRainbow = source.useRainbow;
        safe.rainbowSpeed = source.rainbowSpeed;
        safe.rainbowSpread = source.rainbowSpread;
        safe.palette = source.palette;
        safe.paletteColors = source.paletteColors;
        safe.paletteSpeed = source.paletteSpeed;
        safe.paletteSpread = source.paletteSpread;
        safe.paletteFlash = source.paletteFlash;
        return safe;
    }

    private static boolean hasAdvancedFeatures(EffectSettings effect) {
        return effect.pulse || effect.fade || effect.neonFlicker || effect.chromatic || effect.shimmer || effect.sparkle
                || effect.wave || effect.bounce || effect.shake || effect.swing || effect.wiggle || effect.turb
                || effect.pend || effect.orbit || effect.ripple || effect.drift || effect.glitch || effect.spasm
                || effect.blinkWave || effect.typewriter || effect.noteBounce || effect.glyphScale || effect.sweep
                || effect.outline || effect.glow || effect.trail || effect.extrude || effect.ecg || effect.heartbeat
                || effect.glitchSpike || effect.signalLoss;
    }

    private static EffectSettings createAuthorNoteEffect() {
        EffectSettings effect = new EffectSettings();
        effect.palette = true;
        effect.paletteColors = "35F2FF|6A7CFF|B05CFF|FF5FC7|FFD35A";
        effect.paletteSpeed = 0.22;
        effect.paletteSpread = 0.11;
        effect.noteBounce = true;
        effect.noteBounceAmp = 0.58;
        effect.noteBounceSpeed = 0.95;
        effect.sweep = true;
        effect.sweepSpeed = 0.75;
        effect.sweepWidth = 2.6;
        effect.sweepStrength = 0.9;
        return effect;
    }

    private static void putBool(com.electronwill.nightconfig.core.Config c, String key, boolean value) {
        if (value) c.set(key, true);
    }

    private static void putNum(com.electronwill.nightconfig.core.Config c, String key, double value, double defaultValue) {
        if (Double.compare(value, defaultValue) != 0) c.set(key, value);
    }

    private static boolean bool(com.electronwill.nightconfig.core.Config c, String key, boolean fallback) {
        Object v = c.get(key);
        return v instanceof Boolean b ? b : fallback;
    }

    private static double num(com.electronwill.nightconfig.core.Config c, String key, double fallback) {
        Object v = c.get(key);
        return v instanceof Number n ? n.doubleValue() : fallback;
    }

    private static int integer(com.electronwill.nightconfig.core.Config c, String key, int fallback) {
        Object v = c.get(key);
        return v instanceof Number n ? n.intValue() : fallback;
    }

    private static String str(com.electronwill.nightconfig.core.Config c, String key, String fallback) {
        Object v = c.get(key);
        return v instanceof String text ? text : fallback;
    }

}
