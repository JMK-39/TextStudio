package dev.xyat.textstudio.font.mixin;

import dev.xyat.textstudio.font.api.IStyle;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Style.class)
public class StyleMixin implements IStyle {
    @Unique
    private TextEffectStyleData textstudio_font$styleData = null;

    @Override
    public TextEffectStyleData textstudio_font$getStyleData() {
        return textstudio_font$styleData;
    }

    @Override
    public void textstudio_font$setStyleData(TextEffectStyleData data) {
        this.textstudio_font$styleData = data;
    }

    @Inject(method = {
            "withColor(Lnet/minecraft/network/chat/TextColor;)Lnet/minecraft/network/chat/Style;",
            "withBold", "withItalic", "withUnderlined", "withStrikethrough", "withObfuscated",
            "withClickEvent", "withHoverEvent", "withInsertion", "withFont", "applyFormat",
            "applyLegacyFormat", "applyFormats"
    }, at = @At("RETURN"), cancellable = true)
    private void textstudio_font$onStyleTransform(CallbackInfoReturnable<Style> cir) {
        if (this.textstudio_font$styleData != null) {
            Style result = cir.getReturnValue();
            if (result != null && result != (Object)this) {
                if (result == Style.EMPTY) {
                    result = StyleConstructionAccess.textstudio_font$newStyle(null, null, null, null, null, null, null, null, null, null);
                    cir.setReturnValue(result);
                }
                ((IStyle) result).textstudio_font$setStyleData(this.textstudio_font$styleData);
            }
        }
    }

    @Inject(method = "applyTo", at = @At("RETURN"), cancellable = true)
    private void textstudio_font$onApplyTo(Style other, CallbackInfoReturnable<Style> cir) {
        Style result = cir.getReturnValue();
        if (result == null || result == (Object) this) {
            return;
        }
        TextEffectStyleData data = this.textstudio_font$styleData;
        if (data == null && other instanceof IStyle otherStyle) {
            data = otherStyle.textstudio_font$getStyleData();
        }
        if (data != null) {
            if (result == Style.EMPTY) {
                result = StyleConstructionAccess.textstudio_font$newStyle(null, null, null, null, null, null, null, null, null, null);
                cir.setReturnValue(result);
            }
            ((IStyle) result).textstudio_font$setStyleData(data);
        }
    }
}
