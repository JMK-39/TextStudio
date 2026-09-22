package dev.xyat.textstudio.chat.mixin;

import dev.xyat.textstudio.chat.config.ChatConfig;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.StringUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

public class ChatServerMixins {

    @Mixin(ServerGamePacketListenerImpl.class)
    public static abstract class ServerPacketTweaks {
        @ModifyConstant(method = "handleChat", constant = @Constant(intValue = 256))
        private int textstudio_chat$modifyChatLimit(int original) {
            return ChatConfig.maxChatLength;
        }
    }

    @Mixin({ServerboundChatPacket.class, ServerboundChatCommandPacket.class})
    public static class PacketLengthTweaks {
        @ModifyConstant(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", constant = @Constant(intValue = 256), remap = false)
        private static int textstudio_chat$increasePacketReadLimit(int original) {
            return ChatConfig.maxChatLength;
        }
    }

    @Mixin(StringUtil.class)
    public static class StringUtilTweaks {
        @ModifyConstant(method = "trimChatMessage", constant = @Constant(intValue = 256))
        private static int textstudio_chat$increaseTrimLimit(int original) {
            return ChatConfig.maxChatLength;
        }
    }
}
