package dev.xyat.textstudio.chat.util;

import dev.xyat.kineticcore.api.client.text.KineticText;
import net.minecraft.network.chat.MutableComponent;

public final class ColorText {
    private ColorText() {
    }

    public static MutableComponent translatable(String key, Object... args) {
        return KineticText.translatable(key, args);
    }
}
