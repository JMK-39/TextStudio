package dev.xyat.textstudio.font.mixin.client;

import dev.xyat.textstudio.font.client.parser.EffectSpans;
import net.minecraft.client.gui.components.MultiLineEditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Multi-line fields (the 26.1 book editor among them) draw each line, and the parts before and after the cursor, as
 * pieces of their text: each piece is led by the effect code in force where it starts, so it keeps the effect.
 */
@Mixin(MultiLineEditBox.class)
public abstract class MultiLineEditBoxEffectMixin {
    @Redirect(
            //? if >=26.1 {
            /*method = "extractContents",
            *///?} else {
            method = "renderContents",
            //?}
            at = @At(value = "INVOKE", target = "Ljava/lang/String;substring(II)Ljava/lang/String;")
    )
    private String textstudio_font$effectLine(String value, int begin, int end) {
        return EffectSpans.lineText(value, begin, end);
    }
}
