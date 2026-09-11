package dev.xyat.textstudio.font.api;

import dev.xyat.textstudio.font.config.AuthorConfig;

public interface IStyle {
    TextEffectStyleData textstudio_font$getStyleData();
    void textstudio_font$setStyleData(TextEffectStyleData data);

    class TextEffectStyleData {
        public int effectId;
        public boolean rainbow, bold, strike, jitter, glitch;
        public boolean advancedAllowed;
        public AuthorConfig.EffectSettings customConfig = null;

        public TextEffectStyleData(int effectId, boolean rainbow, boolean bold, boolean strike, boolean jitter, boolean glitch) {
            this(effectId, rainbow, bold, strike, jitter, glitch, true);
        }

        public TextEffectStyleData(int effectId, boolean rainbow, boolean bold, boolean strike, boolean jitter, boolean glitch, boolean advancedAllowed) {
            this.effectId = effectId;
            this.rainbow = rainbow;
            this.bold = bold;
            this.strike = strike;
            this.jitter = jitter;
            this.glitch = glitch;
            this.advancedAllowed = advancedAllowed;
        }

        public int pack() {
            int val = effectId & 0xFF;
            if (rainbow) val |= (1 << 8);
            if (bold) val |= (1 << 9);
            if (strike) val |= (1 << 10);
            if (jitter) val |= (1 << 11);
            if (glitch) val |= (1 << 12);
            if (advancedAllowed) val |= (1 << 13);
            return val;
        }

        public static TextEffectStyleData unpack(int val) {
            return new TextEffectStyleData(
                    val & 0xFF,
                    (val & (1 << 8)) != 0,
                    (val & (1 << 9)) != 0,
                    (val & (1 << 10)) != 0,
                    (val & (1 << 11)) != 0,
                    (val & (1 << 12)) != 0,
                    (val & (1 << 13)) != 0
            );
        }
    }
}
