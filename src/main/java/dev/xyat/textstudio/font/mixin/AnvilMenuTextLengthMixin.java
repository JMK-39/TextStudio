package dev.xyat.textstudio.font.mixin;

import dev.xyat.textstudio.font.common.text.InputMetrics;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuTextLengthMixin {
    @Redirect(
            method = "validateName",
            at = @At(value = "INVOKE", target = "Ljava/lang/String;length()I")
    )
    private static int textstudio_font$visibleNameLength(String text) {
        return InputMetrics.visibleCodePointLength(text);
    }
}
