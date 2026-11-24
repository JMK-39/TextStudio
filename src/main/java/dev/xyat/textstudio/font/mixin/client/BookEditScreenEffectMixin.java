package dev.xyat.textstudio.font.mixin.client;

import dev.xyat.textstudio.font.client.parser.EffectSpans;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The page being written (1.20.1 and 1.21.1): each line is drawn as a piece of the page in the style the splitter
 * hands it; with the effect in force where the line starts added to that style, every line keeps the effect, whichever
 * splitter (vanilla or ModernUI) cut the page. 26.1 writes books in a multi-line field, see
 * {@link MultiLineEditBoxEffectMixin}.
 */
@Mixin(BookEditScreen.class)
public abstract class BookEditScreenEffectMixin {
    @Redirect(
            method = "rebuildDisplayCache",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/StringSplitter;splitLines(Ljava/lang/String;ILnet/minecraft/network/chat/Style;ZLnet/minecraft/client/StringSplitter$LinePosConsumer;)V"
            )
    )
    private void textstudio_font$effectLines(StringSplitter splitter, String text, int width, Style style, boolean withNewLines,
                                             StringSplitter.LinePosConsumer lines) {
        splitter.splitLines(text, width, style, withNewLines,
                (lineStyle, begin, end) -> lines.accept(EffectSpans.lineStyle(text, begin, lineStyle), begin, end));
    }
}
