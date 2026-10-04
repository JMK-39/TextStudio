package dev.xyat.textstudio.font.mixin.client;

import dev.xyat.textstudio.font.client.parser.InlineComponentParser;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = ChatComponent.class, priority = 100)
public abstract class ChatComponentMixin {
    @ModifyVariable(
            //? if >=26.1 {
            /*method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            *///?} else {
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V",
            //?}
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private Component textstudio_font$parseInlineChat(Component message) {
        return InlineComponentParser.parse(message);
    }
}
