package dev.xyat.textstudio.font.mixin.client;

import dev.xyat.textstudio.font.client.render.CompatibleFontRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GuiGraphics.class, priority = 100)
public abstract class GuiGraphicsMixin {
    @Inject(
            method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I",
            at = @At("HEAD"),
            cancellable = true
    )
    private void textstudio_font$drawString(
            Font font,
            String text,
            int x,
            int y,
            int color,
            boolean shadow,
            CallbackInfoReturnable<Integer> cir
    ) {
        GuiGraphics graphics = (GuiGraphics) (Object) this;
        Integer result = CompatibleFontRenderer.tryRenderString(
                font,
                text,
                x,
                y,
                color,
                shadow,
                graphics.pose().last().pose(),
                graphics.bufferSource(),
                Font.DisplayMode.NORMAL,
                0,
                15728880
        );
        if (result != null) {
            graphics.flush();
            cir.setReturnValue(result);
        }
    }

    @Inject(
            method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I",
            at = @At("HEAD"),
            cancellable = true
    )
    private void textstudio_font$drawComponent(
            Font font,
            Component component,
            int x,
            int y,
            int color,
            boolean shadow,
            CallbackInfoReturnable<Integer> cir
    ) {
        GuiGraphics graphics = (GuiGraphics) (Object) this;
        Integer result = CompatibleFontRenderer.tryRenderComponent(
                font,
                component,
                x,
                y,
                color,
                shadow,
                graphics.pose().last().pose(),
                graphics.bufferSource(),
                Font.DisplayMode.NORMAL,
                0,
                15728880
        );
        if (result != null) {
            graphics.flush();
            cir.setReturnValue(result);
        }
    }

    @Inject(
            method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I",
            at = @At("HEAD"),
            cancellable = true
    )
    private void textstudio_font$drawSequence(
            Font font,
            FormattedCharSequence sequence,
            int x,
            int y,
            int color,
            boolean shadow,
            CallbackInfoReturnable<Integer> cir
    ) {
        GuiGraphics graphics = (GuiGraphics) (Object) this;
        Integer result = CompatibleFontRenderer.tryRenderSequence(
                font,
                sequence,
                x,
                y,
                color,
                shadow,
                graphics.pose().last().pose(),
                graphics.bufferSource(),
                Font.DisplayMode.NORMAL,
                0,
                15728880
        );
        if (result != null) {
            graphics.flush();
            cir.setReturnValue(result);
        }
    }
}
