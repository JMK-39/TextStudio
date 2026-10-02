package dev.xyat.textstudio.font.client.render;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;

import java.util.function.Function;

/**
 * 文本样式前缀码用到的控制字符：{@code U+2061..U+2064}（起止与停止标记）和 {@code U+FE00..U+FE0F}（半字节载荷）。
 * 它们只是给文本样式解析用的内部标识，<b>在任何渲染路径里都不允许出现</b>：不画任何字形，也不占宽度。
 * <p>
 * 正常情况下前缀码在绘制前就被解析掉了；但物品提示的自动换行、Toast、第三方模组的自绘文字等路径会先按“字符”
 * 量宽 / 拆行，前缀码一旦被拆开或没被解析，原版字体会把它们画成一排带十六进制编号的方框并占去大量宽度。
 * 在字形层统一把这些字符变成零宽度的空字形，量宽、拆行、绘制三处都不再受它们影响。
 */
public final class InvisibleMarkers {
    /** 零宽度、无字形的字形信息（粗体与阴影也不加偏移）。 */
    public static final GlyphInfo INFO = new GlyphInfo() {
        @Override
        public float getAdvance() {
            return 0.0F;
        }

        @Override
        public float getAdvance(boolean bold) {
            return 0.0F;
        }

        @Override
        public float getBoldOffset() {
            return 0.0F;
        }

        @Override
        public float getShadowOffset() {
            return 0.0F;
        }

        @Override
        public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> baker) {
            return EmptyGlyph.INSTANCE;
        }
    };

    private InvisibleMarkers() {
    }

    public static boolean is(int codePoint) {
        return (codePoint >= 0x2061 && codePoint <= 0x2064) || (codePoint >= 0xFE00 && codePoint <= 0xFE0F);
    }
}
