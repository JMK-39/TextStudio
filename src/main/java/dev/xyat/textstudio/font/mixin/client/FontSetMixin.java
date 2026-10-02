package dev.xyat.textstudio.font.mixin.client;

import com.mojang.blaze3d.font.GlyphInfo;
import dev.xyat.textstudio.font.client.render.InvisibleMarkers;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 前缀码的控制字符在字形层彻底隐形：零宽度、不画任何东西（见 {@link InvisibleMarkers}）。
 * 量宽、拆行、绘制都经过这里，所以无论前缀码有没有被解析、有没有被拆开，都不会显示出来或占位。
 */
@Mixin(value = FontSet.class, priority = 500)
public abstract class FontSetMixin {
    @Inject(
            method = "getGlyphInfo(IZ)Lcom/mojang/blaze3d/font/GlyphInfo;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void textstudio_font$markerInfo(int codePoint, boolean filterFishyGlyphs, CallbackInfoReturnable<GlyphInfo> cir) {
        if (InvisibleMarkers.is(codePoint)) {
            cir.setReturnValue(InvisibleMarkers.INFO);
        }
    }

    @Inject(
            method = "getGlyph(I)Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void textstudio_font$markerGlyph(int codePoint, CallbackInfoReturnable<BakedGlyph> cir) {
        if (InvisibleMarkers.is(codePoint)) {
            cir.setReturnValue(EmptyGlyph.INSTANCE);
        }
    }

    /** 乱码样式（§k）会按宽度随机取字形；零宽度的标记保持为空字形。 */
    @Inject(
            method = "getRandomGlyph(Lcom/mojang/blaze3d/font/GlyphInfo;)Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void textstudio_font$markerRandom(GlyphInfo info, CallbackInfoReturnable<BakedGlyph> cir) {
        if (info == InvisibleMarkers.INFO) {
            cir.setReturnValue(EmptyGlyph.INSTANCE);
        }
    }
}
