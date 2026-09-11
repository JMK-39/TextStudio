package dev.xyat.textstudio.font.mixin.client;

import dev.xyat.textstudio.font.common.text.InputMetrics;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;

@Mixin(AbstractSignEditScreen.class)
public abstract class SignEditTextInputMixin {
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
}
