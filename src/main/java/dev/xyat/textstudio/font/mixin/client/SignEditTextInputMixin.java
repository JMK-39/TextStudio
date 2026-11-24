package dev.xyat.textstudio.font.mixin.client;

import dev.xyat.textstudio.font.common.text.InputMetrics;
import dev.xyat.textstudio.font.common.text.SignPaste;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
//? if >=26.1 {
/*import net.minecraft.client.input.KeyEvent;
*///?} else {
import net.minecraft.client.gui.screens.Screen;
//?}
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;
import java.util.function.Predicate;

@Mixin(AbstractSignEditScreen.class)
public abstract class SignEditTextInputMixin {
    /** The longest line the server takes from a sign (ServerboundSignUpdatePacket), effect codes included. */
    @Unique
    private static final int TEXTSTUDIO_FONT$MAX_LINE_CHARS = 384;

    @Shadow @Final private String[] messages;
    @Shadow private int line;
    @Shadow private TextFieldHelper signField;

    @ModifyArg(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/font/TextFieldHelper;<init>(Ljava/util/function/Supplier;Ljava/util/function/Consumer;Ljava/util/function/Supplier;Ljava/util/function/Consumer;Ljava/util/function/Predicate;)V"
            ),
            index = 4
    )
    private Predicate<String> textstudio_font$visibleSignValidation(Predicate<String> original) {
        return text -> original.test(InputMetrics.stripControlCodes(text));
    }

    // Vanilla drops a paste that does not fit the line whole; this pastes what fits and wraps the rest, see SignPaste.
    //? if >=26.1 {
    /*@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void textstudio_font$wrappingPaste(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (event.isPaste() && textstudio_font$paste(TextFieldHelper.getClipboardContents(Minecraft.getInstance()))) {
            cir.setReturnValue(true);
        }
    }
    *///?} else {
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void textstudio_font$wrappingPaste(int key, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (Screen.isPaste(key) && textstudio_font$paste(TextFieldHelper.getClipboardContents(Minecraft.getInstance()))) {
            cir.setReturnValue(true);
        }
    }
    //?}

    /** Pastes {@code clipboard} at the cursor; false before the editor is set up. */
    @Unique
    private boolean textstudio_font$paste(String clipboard) {
        if (signField == null) {
            return false;
        }
        TextFieldHelperAccess field = (TextFieldHelperAccess) signField;
        Predicate<String> valid = field.textstudio_font$validator();
        SignPaste.Result result = SignPaste.paste(messages, line, signField.getCursorPos(), signField.getSelectionPos(),
                clipboard,
                text -> text.length() <= TEXTSTUDIO_FONT$MAX_LINE_CHARS && valid.test(text));
        Consumer<String> setter = field.textstudio_font$messageSetter();
        for (int i = 0; i < messages.length; i++) {
            if (!result.lines()[i].equals(messages[i])) {
                line = i;
                setter.accept(result.lines()[i]);
            }
        }
        line = result.line();
        signField.setSelectionRange(result.cursor(), result.cursor());
        return true;
    }
}
