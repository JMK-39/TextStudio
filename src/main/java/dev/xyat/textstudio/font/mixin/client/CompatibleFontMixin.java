package dev.xyat.textstudio.font.mixin.client;

import dev.xyat.textstudio.font.client.render.CompatibleFontRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Font.class, priority = 500)
public abstract class CompatibleFontMixin {
    @Inject(
            method = "drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I",
            at = @At("HEAD"),
            cancellable = true
    )
    private void textstudio_font$string(
            String text,
            float x,
            float y,
            int color,
            boolean shadow,
            Matrix4f matrix,
            MultiBufferSource buffers,
            Font.DisplayMode mode,
            int backgroundColor,
            int packedLight,
            CallbackInfoReturnable<Integer> cir
    ) {
        Integer result = CompatibleFontRenderer.tryRenderString(
                (Font) (Object) this,
                text,
                x,
                y,
                color,
                shadow,
                matrix,
                buffers,
                mode,
                backgroundColor,
                packedLight
        );
        if (result != null) {
            cir.setReturnValue(result);
        }
    }

    @Inject(
            method = "drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;IIZ)I",
            at = @At("HEAD"),
            cancellable = true
    )
    private void textstudio_font$stringBidi(
            String text,
            float x,
            float y,
            int color,
            boolean shadow,
            Matrix4f matrix,
            MultiBufferSource buffers,
            Font.DisplayMode mode,
            int backgroundColor,
            int packedLight,
            boolean bidirectional,
            CallbackInfoReturnable<Integer> cir
    ) {
        Integer result = CompatibleFontRenderer.tryRenderString(
                (Font) (Object) this,
                text,
                x,
                y,
                color,
                shadow,
                matrix,
                buffers,
                mode,
                backgroundColor,
                packedLight
        );
        if (result != null) {
            cir.setReturnValue(result);
        }
    }

    @Inject(
            method = "drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I",
            at = @At("HEAD"),
            cancellable = true
    )
    private void textstudio_font$component(
            Component component,
            float x,
            float y,
            int color,
            boolean shadow,
            Matrix4f matrix,
            MultiBufferSource buffers,
            Font.DisplayMode mode,
            int backgroundColor,
            int packedLight,
            CallbackInfoReturnable<Integer> cir
    ) {
        Integer result = CompatibleFontRenderer.tryRenderComponent(
                (Font) (Object) this,
                component,
                x,
                y,
                color,
                shadow,
                matrix,
                buffers,
                mode,
                backgroundColor,
                packedLight
        );
        if (result != null) {
            cir.setReturnValue(result);
        }
    }

    @Inject(
            method = "drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I",
            at = @At("HEAD"),
            cancellable = true
    )
    private void textstudio_font$sequence(
            FormattedCharSequence sequence,
            float x,
            float y,
            int color,
            boolean shadow,
            Matrix4f matrix,
            MultiBufferSource buffers,
            Font.DisplayMode mode,
            int backgroundColor,
            int packedLight,
            CallbackInfoReturnable<Integer> cir
    ) {
        Integer result = CompatibleFontRenderer.tryRenderSequence(
                (Font) (Object) this,
                sequence,
                x,
                y,
                color,
                shadow,
                matrix,
                buffers,
                mode,
                backgroundColor,
                packedLight
        );
        if (result != null) {
            cir.setReturnValue(result);
        }
    }
    @Inject(
            method = "width(Ljava/lang/String;)I",
            at = @At("HEAD"),
            cancellable = true
    )
    private void textstudio_font$widthString(String text, CallbackInfoReturnable<Integer> cir) {
        Integer result = CompatibleFontRenderer.tryMeasureString((Font) (Object) this, text);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }

    @Inject(
            method = "width(Lnet/minecraft/util/FormattedCharSequence;)I",
            at = @At("HEAD"),
            cancellable = true
    )
    private void textstudio_font$widthSequence(FormattedCharSequence sequence, CallbackInfoReturnable<Integer> cir) {
        Integer result = CompatibleFontRenderer.tryMeasureSequence((Font) (Object) this, sequence);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }

}
