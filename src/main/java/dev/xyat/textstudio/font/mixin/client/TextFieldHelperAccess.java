package dev.xyat.textstudio.font.mixin.client;

import net.minecraft.client.gui.font.TextFieldHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Consumer;
import java.util.function.Predicate;

@Mixin(TextFieldHelper.class)
public interface TextFieldHelperAccess {
    /** What a line must pass to be taken, as the screen set it up. */
    @Accessor("stringValidator")
    Predicate<String> textstudio_font$validator();

    /** Writes the screen's current line. */
    @Accessor("setMessageFn")
    Consumer<String> textstudio_font$messageSetter();
}
