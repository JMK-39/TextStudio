package dev.xyat.textstudio.font.mixin.client;

import dev.xyat.textstudio.font.common.text.InputMetrics;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(EditBox.class)
public abstract class EditBoxTextLengthMixin {
    @Shadow private String value;
    @Shadow private int maxLength;
    @Shadow private int cursorPos;
    @Shadow private int highlightPos;
    @Shadow private Predicate<String> filter;

    @Shadow public abstract void setCursorPosition(int position);
    @Shadow public abstract void setHighlightPos(int position);
    @Shadow public abstract void moveCursorToEnd();
    @Invoker("onValueChange")
    protected abstract void textstudio_font$invokeOnValueChange(String newText);

    @Inject(method = "setValue", at = @At("HEAD"), cancellable = true)
    private void textstudio_font$setValue(String text, CallbackInfo ci) {
        if (!InputMetrics.hasControlCodes(text)) {
            return;
        }
        String limited = InputMetrics.truncateToVisibleLength(text, maxLength);
        if (filter.test(limited)) {
            value = limited;
            moveCursorToEnd();
            setHighlightPos(cursorPos);
            textstudio_font$invokeOnValueChange(value);
        }
        ci.cancel();
    }

    @Inject(method = "insertText", at = @At("HEAD"), cancellable = true)
    private void textstudio_font$insertText(String textToWrite, CallbackInfo ci) {
        if (!InputMetrics.hasControlCodes(value) && !InputMetrics.hasControlCodes(textToWrite)) {
            return;
        }

        int start = Math.min(cursorPos, highlightPos);
        int end = Math.max(cursorPos, highlightPos);
        String filtered = SharedConstants.filterText(textToWrite);
        int selectedVisible = InputMetrics.visibleCodePointLength(value.substring(start, end));
        int currentVisible = InputMetrics.visibleCodePointLength(value);
        int remainingVisible = Math.max(0, maxLength - (currentVisible - selectedVisible));
        String limited = InputMetrics.truncateToVisibleLength(filtered, remainingVisible);
        String next = new StringBuilder(value).replace(start, end, limited).toString();

        if (filter.test(next)) {
            value = next;
            setCursorPosition(start + limited.length());
            setHighlightPos(cursorPos);
            textstudio_font$invokeOnValueChange(value);
        }
        ci.cancel();
    }

    @Inject(method = "setMaxLength", at = @At("HEAD"), cancellable = true)
    private void textstudio_font$setMaxLength(int length, CallbackInfo ci) {
        if (!InputMetrics.hasControlCodes(value)) {
            return;
        }
        maxLength = Math.max(0, length);
        String limited = InputMetrics.truncateToVisibleLength(value, maxLength);
        if (!limited.equals(value) && filter.test(limited)) {
            value = limited;
            moveCursorToEnd();
            setHighlightPos(cursorPos);
            textstudio_font$invokeOnValueChange(value);
        }
        ci.cancel();
    }
}
