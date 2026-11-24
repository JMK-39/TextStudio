package dev.xyat.textstudio.font.mixin.client;

import dev.xyat.textstudio.font.client.parser.EffectSpans;
import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Wrapped text keeps its effect on every line. An effect code sits once, in front of its text, so a line cut after it
 * had none: a signed book showed the effect on its first line only. {@link FontSplitEffectMixin} does the same where a
 * mod (ModernUI) replaces the splitter.
 */
@Mixin(StringSplitter.class)
public abstract class StringSplitterEffectMixin {
    /** Component text (signed books, signs, descriptions): the effect is carried by every character it covers. */
    @ModifyVariable(
            method = "splitLines(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/network/chat/Style;Ljava/util/function/BiConsumer;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private FormattedText textstudio_font$styledText(FormattedText text) {
        return EffectSpans.styled(text);
    }
}
