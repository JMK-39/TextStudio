package dev.xyat.textstudio.font.mixin.client;

import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(StringDecomposer.class)
public interface StringDecomposerAccess {
    @Invoker("feedChar")
    static boolean callFeedChar(Style style, FormattedCharSink sink, int index, char c) {
        throw new IllegalAccessError("Mixin failed to inject");
    }
}
