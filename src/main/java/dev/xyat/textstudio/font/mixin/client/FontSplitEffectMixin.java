package dev.xyat.textstudio.font.mixin.client;

import dev.xyat.textstudio.font.client.parser.EffectSpans;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Text split into lines (signed books, signs, descriptions) keeps its effect on every line, also where a mod such as
 * ModernUI splits text its own way and never reaches the vanilla splitter ({@link StringSplitterEffectMixin}).
 */
@Mixin(Font.class)
public abstract class FontSplitEffectMixin {
    @ModifyVariable(method = "split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;", at = @At("HEAD"), argsOnly = true)
    private FormattedText textstudio_font$styledText(FormattedText text) {
        return EffectSpans.styled(text);
    }
}
