package dev.xyat.textstudio.chat.mixin.client;

import dev.xyat.textstudio.chat.client.ChatCopyCanvasScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatCopyMixins extends Screen {

    @Shadow protected EditBox input;

    @Unique
    private Button textstudio_chat$canvasButton;

    protected ChatCopyMixins(Component title) { super(title); }

    @Inject(method = "init", at = @At("RETURN"))
    private void textstudio_chat$addCanvasButton(CallbackInfo ci) {
        this.textstudio_chat$canvasButton = Button.builder(
                        Component.translatable("gui.textstudio.chat.open_canvas"),
                        b -> {
                            Minecraft mc = Minecraft.getInstance();
                            // 这里更新为独立的 Accessor
                            ChatComponentAccessor accessor = (ChatComponentAccessor) mc.gui.getChat();
                            mc.setScreen(new ChatCopyCanvasScreen((Screen) (Object) this, accessor.getTrimmedMessages()));
                        }
                )
                .bounds(5, this.height - 30, 55, 12)
                .tooltip(Tooltip.create(Component.translatable("gui.textstudio.chat.open_canvas.desc")))
                .build();

        if (this.input != null) {
            this.setInitialFocus(this.input);
        }
    }

    /**
     * 手动拦截鼠标点击：
     * 由于按钮不在 children 列表中，键盘碰不到它，但我们需要在这里手动让鼠标能点中它。
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void textstudio_chat$handleManualClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (this.textstudio_chat$canvasButton != null && this.textstudio_chat$canvasButton.visible) {
            // 手动调用按钮的点击检测
            if (this.textstudio_chat$canvasButton.mouseClicked(mouseX, mouseY, button)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void textstudio_chat$handleManualRelease(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (this.textstudio_chat$canvasButton != null && this.textstudio_chat$canvasButton.visible) {
            if (this.textstudio_chat$canvasButton.mouseReleased(mouseX, mouseY, button)) {
                cir.setReturnValue(true);
            }
        }
    }

    /**
     * 手动渲染按钮：
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void textstudio_chat$manualRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (this.textstudio_chat$canvasButton != null && this.input != null) {
            // 状态同步
            this.textstudio_chat$canvasButton.visible = !this.input.getValue().startsWith("/");

            if (this.textstudio_chat$canvasButton.visible) {
                // 手动渲染。因为不在 children 列表里，所以它永远不会被方向键选中高亮
                this.textstudio_chat$canvasButton.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
    }
}
