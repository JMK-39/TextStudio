package dev.xyat.textstudio.font.client.render;

//? if >=26.1 {
/*import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;

import java.util.ArrayList;
import java.util.List;

/^*
 * 26.1 的界面文字先排进渲染状态、稍后才绘制，所以每个特效字形都作为一个独立的文字状态加入，位置与缩放写进它的
 * 变换矩阵 / 26.1 GUI text is queued as render state and drawn later, so every effect glyph pass is added as its own text
 * state, with its position and scale in the state's pose. The renderer's sequences are reused objects, so each pass
 * keeps a copy of its text.
 ^/
public final class GuiGlyphOut implements CompatibleFontRenderer.GlyphOut {
    private static final ThreadLocal<GuiGlyphOut> CURRENT = ThreadLocal.withInitial(GuiGlyphOut::new);
    // Offsets of the eight outline passes, as Font.drawInBatch8xOutline.
    private static final int[][] OUTLINE = {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};

    private GuiRenderState target;
    private GuiTextRenderState source;

    private GuiGlyphOut() {
    }

    /^* The output for one GUI text state, drawing into the render state that received it. ^/
    public static GuiGlyphOut of(GuiRenderState target, GuiTextRenderState source) {
        GuiGlyphOut out = CURRENT.get();
        out.target = target;
        out.source = source;
        return out;
    }

    @Override
    public void draw(Font font, FormattedCharSequence text, float x, float y, int color, boolean shadow, boolean background,
                     float scale, float centerX, float centerY) {
        GuiRenderState target = this.target;
        GuiTextRenderState source = this.source;
        Matrix3x2f pose = new Matrix3x2f(source.pose);
        if (Math.abs(scale - 1.0f) > 0.0001f) {
            pose.translate(centerX, centerY).scale(scale).translate(-centerX, -centerY);
        }
        pose.translate(x, y);
        target.addText(new GuiTextRenderState(font, copy(text), pose, 0, 0, color,
                background ? source.backgroundColor : 0, shadow, false, source.scissor));
        // addText passes the glyph state back through the GUI text hook, which rebinds this shared output to it;
        // without restoring, the next glyph would be placed relative to this one.
        this.target = target;
        this.source = source;
    }

    @Override
    public void drawOutlined(Font font, FormattedCharSequence text, float x, float y, int bodyColor, int outlineColor,
                             float scale, float centerX, float centerY) {
        for (int[] offset : OUTLINE) {
            draw(font, text, x + offset[0], y + offset[1], outlineColor, false, false, scale, centerX, centerY);
        }
        draw(font, text, x, y, bodyColor, false, true, scale, centerX, centerY);
    }

    private static FormattedCharSequence copy(FormattedCharSequence text) {
        List<FormattedCharSequence> glyphs = new ArrayList<>(4);
        text.accept((index, style, codePoint) -> {
            glyphs.add(FormattedCharSequence.codepoint(codePoint, style));
            return true;
        });
        return glyphs.size() == 1 ? glyphs.get(0) : FormattedCharSequence.composite(glyphs);
    }
}
*///?}
