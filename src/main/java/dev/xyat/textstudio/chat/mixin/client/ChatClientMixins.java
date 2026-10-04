package dev.xyat.textstudio.chat.mixin.client;

import dev.xyat.kineticcore.api.client.gui.KineticGui;
import dev.xyat.kineticcore.api.minecraft.MinecraftChat;
import dev.xyat.kineticcore.api.minecraft.MinecraftPlayers;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.textstudio.chat.client.IChatComponentSync;
import dev.xyat.textstudio.chat.config.ChatConfig;
import dev.xyat.textstudio.chat.network.ChatSyncCodec;
import dev.xyat.textstudio.chat.network.ChatSyncNetwork;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ChatClientMixins {

    @Mixin(ChatComponent.class)
    public static abstract class ChatComponentTweaks implements IChatComponentSync {
        @Shadow @Final private List<GuiMessage> allMessages;
        @Shadow public abstract void rescaleChat();

        @Unique private boolean textstudio_chat$isSyncing = false;
        @Unique private boolean textstudio_chat$isRefreshing = false;
        @Unique private String textstudio_chat$lastRawMessage = "";
        @Unique private int textstudio_chat$counter = 1;
        @Unique private boolean textstudio_chat$needsRefresh = false;
        @Unique private Component textstudio_chat$capturedOriginal;
        @Unique private long textstudio_chat$providedTimestamp = -1;
        @Unique private static final DateTimeFormatter textstudio_chat$TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        @Unique private UUID textstudio_chat$lastSenderUUID = null;

        //? if >=1.21 <26.1 {
        /*// In 1.21.1 these two depth-100 fills belong only to the vanilla scrollbar.
        @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIIII)V"), require = 2)
        private void textstudio_chat$renderVanillaScrollbar(GuiGraphics graphics, int x1, int y1, int x2, int y2, int depth, int color) {
            if (!dev.xyat.textstudio.chat.client.ChatScreenControls.replacesVanillaScrollbar()) {
                graphics.fill(x1, y1, x2, y2, depth, color);
            }
        }
        *///?}
        //? if >=26.1 {
        /*// 26.1 draws the vanilla scrollbar with the third and fourth fill of the private extractRenderState; the line
        // backgrounds are filled inside a lambda, so they are not counted here.
        @Redirect(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
                at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;fill(IIIII)V", ordinal = 2))
        private void textstudio_chat$renderVanillaScrollbarThumb(ChatComponent.ChatGraphicsAccess graphics, int x1, int y1, int x2, int y2, int color) {
            if (!dev.xyat.textstudio.chat.client.ChatScreenControls.replacesVanillaScrollbar()) graphics.fill(x1, y1, x2, y2, color);
        }

        @Redirect(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
                at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;fill(IIIII)V", ordinal = 3))
        private void textstudio_chat$renderVanillaScrollbarEdge(ChatComponent.ChatGraphicsAccess graphics, int x1, int y1, int x2, int y2, int color) {
            if (!dev.xyat.textstudio.chat.client.ChatScreenControls.replacesVanillaScrollbar()) graphics.fill(x1, y1, x2, y2, color);
        }
        *///?}

        @Unique private final Map<GuiMessage.Line, UUID> textstudio_chat$lineToUuidMap = new WeakHashMap<>();
        @Unique private final Map<Component, UUID> textstudio_chat$componentToUuidMap = new WeakHashMap<>();

        // 记录行和组件绑定的时间戳宽度，用于精确定位头像
        @Unique private final Map<GuiMessage.Line, Integer> textstudio_chat$lineToTsWidthMap = new WeakHashMap<>();
        @Unique private final Map<Component, Integer> textstudio_chat$componentToTsWidthMap = new WeakHashMap<>();
        @Unique private int textstudio_chat$lastTimestampWidth = 0;

        @Unique private GuiMessage.Line textstudio_chat$currentRenderingLine = null;
        // Line content to line, so 26.1 chat drawing, which only sees the content, can find the head.
        @Unique private final Map<net.minecraft.util.FormattedCharSequence, GuiMessage.Line> textstudio_chat$contentToLineMap = new WeakHashMap<>();

        @Override
        public dev.xyat.textstudio.chat.client.IChatComponentSync.ChatHead textstudio_chat$headFor(net.minecraft.util.FormattedCharSequence lineContent) {
            GuiMessage.Line line = textstudio_chat$contentToLineMap.get(lineContent);
            UUID uuid = line == null ? null : textstudio_chat$lineToUuidMap.get(line);
            return uuid == null ? null : new dev.xyat.textstudio.chat.client.IChatComponentSync.ChatHead(uuid, textstudio_chat$lineToTsWidthMap.getOrDefault(line, 0));
        }

        @Override public void textstudio_chat$setSyncing(boolean syncing) { this.textstudio_chat$isSyncing = syncing; }
        @Override public void textstudio_chat$setProvidedTimestamp(long timestamp) { this.textstudio_chat$providedTimestamp = timestamp; }
        @Override public void textstudio_chat$setCapturedSender(UUID uuid) { this.textstudio_chat$lastSenderUUID = uuid; }

        //? if >=1.21 {
        /*@ModifyConstant(method = {"addMessageToQueue", "addMessageToDisplayQueue"}, constant = @Constant(intValue = 100))
        *///?} else {
        @ModifyConstant(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V", constant = @Constant(intValue = 100))
        //?}
        private int textstudio_chat$expandChatHistory(int original) { return ChatConfig.maxChatHistoryLines; }

        @Inject(method = "addRecentChat", at = @At("HEAD"))
        private void textstudio_chat$syncInputHistory(String message, CallbackInfo ci) {
            if (!this.textstudio_chat$isSyncing && KineticClientRuntime.connected()) {
                try {
                    ChatSyncNetwork.sendToServer(
                            ChatSyncCodec.TYPE_INPUT_LINE,
                            ChatSyncCodec.encodeInputLine(message, ChatConfig.maxChatLength)
                    );
                } catch (Exception ignored) {}
            }
        }

        //? if >=26.1 {
        /*@Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V", at = @At("HEAD"))
        *///?} else {
        @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V", at = @At("HEAD"))
        //?}
        //? if >=26.1 {
        /*private void textstudio_chat$onAddMessageHead(Component component, MessageSignature signature, net.minecraft.client.multiplayer.chat.GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci) {
            boolean refresh = false;
        *///?} else if >=1.21 {
        /*private void textstudio_chat$onAddMessageHead(Component component, MessageSignature signature, GuiMessageTag tag, CallbackInfo ci) {
            boolean refresh = false;
        *///?} else {
        private void textstudio_chat$onAddMessageHead(Component component, MessageSignature signature, int tick, GuiMessageTag tag, boolean refresh, CallbackInfo ci) {
        //?}
            this.textstudio_chat$isRefreshing = refresh;
            if (refresh) return;
            this.textstudio_chat$capturedOriginal = component;
            String rawContent = component.getString();
            if (ChatConfig.enableCompactChat && rawContent.equals(textstudio_chat$lastRawMessage) && !allMessages.isEmpty()) {
                textstudio_chat$counter++;
                allMessages.remove(0);
                textstudio_chat$needsRefresh = true;
            } else {
                textstudio_chat$lastRawMessage = rawContent;
                textstudio_chat$counter = 1;
                textstudio_chat$needsRefresh = false;
            }
        }

        //? if >=26.1 {
        /*@ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
        *///?} else {
        @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
        //?}
        private Component textstudio_chat$applyVisualDecorations(Component component) {
            if (this.textstudio_chat$isRefreshing) return component;
            MutableComponent root = Component.empty();

            int tsWidth = 0;
            if (ChatConfig.enableTimestamp) {
                long ts = textstudio_chat$providedTimestamp != -1 ? textstudio_chat$providedTimestamp : System.currentTimeMillis();
                String timeNumbers = LocalTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault()).format(textstudio_chat$TIME_FORMAT);
                String tsString = "[" + timeNumbers + "] ";
                root.append(KineticI18n.translatable("chat.textstudio.timestamp", timeNumbers));

                // 计算出当前时间戳的宽度，用于稍后渲染头像的 X 轴起始定位
                tsWidth = KineticClientRuntime.font().width(tsString);
            }
            this.textstudio_chat$lastTimestampWidth = tsWidth;

            // 若开启了头像渲染，并且确定该消息来自玩家，则在此处植入 3 个空格（12像素）作为头像占位符
            if (ChatConfig.enableChatHeads && this.textstudio_chat$lastSenderUUID != null) {
                root.append(Component.literal("   "));
            }

            root.append(component);

            if (ChatConfig.enableCompactChat && textstudio_chat$counter > 1) {
                // 使用 I18N 替代硬编码
                root.append(KineticI18n.translatable("chat.textstudio.compact", textstudio_chat$counter));
            }
            return root;
        }

        //? if >=26.1 {
        /*@Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V", at = @At("RETURN"))
        *///?} else {
        @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V", at = @At("RETURN"))
        //?}
        //? if >=26.1 {
        /*private void textstudio_chat$onAddMessageReturn(Component component, MessageSignature signature, net.minecraft.client.multiplayer.chat.GuiMessageSource source, GuiMessageTag guiTag, CallbackInfo ci) {
            boolean refresh = false;
        *///?} else if >=1.21 {
        /*private void textstudio_chat$onAddMessageReturn(Component component, MessageSignature signature, GuiMessageTag guiTag, CallbackInfo ci) {
            boolean refresh = false;
        *///?} else {
        private void textstudio_chat$onAddMessageReturn(Component component, MessageSignature signature, int tick, GuiMessageTag guiTag, boolean refresh, CallbackInfo ci) {
        //?}
            // 1. 获取消息发送者的 UUID 及时间戳宽度
            UUID messageUuid = textstudio_chat$lastSenderUUID;
            int tsWidth = this.textstudio_chat$lastTimestampWidth;

            if (!refresh && messageUuid != null) {
                textstudio_chat$componentToUuidMap.put(component, messageUuid);
                textstudio_chat$componentToTsWidthMap.put(component, tsWidth);
            } else if (refresh) {
                messageUuid = textstudio_chat$componentToUuidMap.get(component);
                tsWidth = textstudio_chat$componentToTsWidthMap.getOrDefault(component, 0);
            }

            // 2. 追踪消息的【视觉首行】以渲染头像并记录偏移宽度
            if (messageUuid != null) {
                List<GuiMessage.Line> trimmed = MinecraftChat.trimmedMessages((ChatComponent) (Object) this);
                if (!trimmed.isEmpty()) {
                    // 【核心算法修复】：倒推寻找消息视觉第一行
                    // Minecraft 将刚换行的字句倒序推入列表，越往上的字行 Index 越大
                    // 只要下一条历史消息的标志位(endOfEntry==true)还没出现，就继续往上找！
                    int topIndex = 0;
                    for (int i = 1; i < trimmed.size(); i++) {
                        if (trimmed.get(i).endOfEntry()) {
                            break;
                        }
                        topIndex = i;
                    }

                    GuiMessage.Line firstLine = trimmed.get(topIndex);
                    textstudio_chat$lineToUuidMap.put(firstLine, messageUuid);
                    textstudio_chat$contentToLineMap.put(firstLine.content(), firstLine);
                    textstudio_chat$lineToTsWidthMap.put(firstLine, tsWidth);
                }
            }

            if (refresh) return;
            if (textstudio_chat$needsRefresh) { this.rescaleChat(); textstudio_chat$needsRefresh = false; }

            if (!this.textstudio_chat$isSyncing && KineticClientRuntime.connected()) {
                try {
                    ChatSyncNetwork.sendToServer(
                            ChatSyncCodec.TYPE_CHAT_LINE,
                            ChatSyncCodec.encodeChatLine(
                                    dev.xyat.textstudio.chat.client.ChatJson.toJson(this.textstudio_chat$capturedOriginal),
                                    System.currentTimeMillis(),
                                    textstudio_chat$lastSenderUUID,
                                    ChatConfig.maxChatLength
                            )
                    );
                } catch (Exception ignored) {}
            }
            textstudio_chat$lastSenderUUID = null;
        }

        //? if >=1.21 {
        /*@Inject(method = "addMessageToDisplayQueue", at = @At("RETURN"))
        private void textstudio_chat$restoreLineMetadata(GuiMessage message, CallbackInfo ci) {
            UUID uuid = textstudio_chat$componentToUuidMap.get(message.content());
            if (uuid == null) return;
            List<GuiMessage.Line> lines = MinecraftChat.trimmedMessages((ChatComponent) (Object) this);
            if (lines.isEmpty()) return;
            int first = 0;
            for (int i = 1; i < lines.size() && !lines.get(i).endOfEntry(); i++) first = i;
            textstudio_chat$lineToUuidMap.put(lines.get(first), uuid);
            textstudio_chat$contentToLineMap.put(lines.get(first).content(), lines.get(first));
            textstudio_chat$lineToTsWidthMap.put(lines.get(first), textstudio_chat$componentToTsWidthMap.getOrDefault(message.content(), 0));
        }
        *///?}

        // 26.1 draws chat lines through ChatGraphicsAccess, see ChatLineHeadTweaks.
        //? if <26.1 {
        @ModifyVariable(method = "render", at = @At(value = "STORE"), ordinal = 0)
        private GuiMessage.Line textstudio_chat$captureRenderingLine(GuiMessage.Line line) {
            this.textstudio_chat$currentRenderingLine = line;
            return line;
        }

        @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"))
        private int textstudio_chat$renderChatRow(GuiGraphics graphics, net.minecraft.client.gui.Font font, net.minecraft.util.FormattedCharSequence text, int x, int y, int color) {
            // 文字始终渲染在默认原点 x。首行的 Component 内已经包含了 3 个空格的占位。
            int result = graphics.drawString(font, text, x, y, color);

            if (ChatConfig.enableChatHeads && this.textstudio_chat$currentRenderingLine != null) {
                UUID uuid = textstudio_chat$lineToUuidMap.get(this.textstudio_chat$currentRenderingLine);
                if (uuid != null) {
                    int tsWidth = textstudio_chat$lineToTsWidthMap.getOrDefault(this.textstudio_chat$currentRenderingLine, 0);
                    ResourceLocation skin = textstudio_chat$getSkin(uuid);
                    float alpha = (float) ((color >> 24) & 0xFF) / 255.0F;
                    graphics.setColor(1.0F, 1.0F, 1.0F, alpha);

                    // 将头像精准地渲染在“首行”的时间戳宽度之后。如果没开时间戳，tsWidth为0，渲染在最前面。
                    int headX = x + tsWidth;
                    graphics.blit(skin, headX, y, 8, 8, 8, 8, 8, 8, 64, 64);
                    graphics.blit(skin, headX, y, 8, 8, 40, 8, 8, 8, 64, 64);
                    graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
            }
            return result;
        }
        //?}

        @Unique private ResourceLocation textstudio_chat$getSkin(UUID uuid) {
            return dev.xyat.textstudio.chat.client.ChatHeads.skin(uuid);
        }

        @Inject(method = "clearMessages", at = @At("HEAD"))
        private void textstudio_chat$onClear(boolean pResetScroll, CallbackInfo ci) {
            textstudio_chat$lineToUuidMap.clear();
            textstudio_chat$componentToUuidMap.clear();
            textstudio_chat$lineToTsWidthMap.clear();
            textstudio_chat$contentToLineMap.clear();
            textstudio_chat$componentToTsWidthMap.clear();
        }
    }

    //? if >=26.1 {
    /*// 26.1 draws chat lines through two ChatGraphicsAccess implementations; the head goes after the timestamp of a
    // message's first line, where the message keeps three spaces free for it.
    @Mixin(targets = {
            "net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess",
            "net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess"
    })
    public static abstract class ChatLineHeadTweaks {
        // Multi-target mixins may only use shadows that are not remapped.
        @Shadow(remap = false) @Final private GuiGraphics graphics;

        @Inject(method = "handleMessage", at = @At("RETURN"))
        private void textstudio_chat$drawHead(int textTop, float opacity, net.minecraft.util.FormattedCharSequence message, CallbackInfoReturnable<Boolean> cir) {
            if (!ChatConfig.enableChatHeads) return;
            IChatComponentSync sync = MinecraftChat.extension(IChatComponentSync.class);
            IChatComponentSync.ChatHead head = sync == null ? null : sync.textstudio_chat$headFor(message);
            if (head == null) return;
            ResourceLocation skin = dev.xyat.textstudio.chat.client.ChatHeads.skin(head.sender());
            int color = net.minecraft.util.ARGB.color(opacity, 0xFFFFFF);
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, skin, head.timestampWidth(), textTop, 8.0F, 8.0F, 8, 8, 64, 64, color);
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, skin, head.timestampWidth(), textTop, 40.0F, 8.0F, 8, 8, 64, 64, color);
        }
    }
    *///?}

    @Mixin(ClientPacketListener.class)
    public static class ClientPacketTweaks {
        @Inject(method = "handlePlayerChat", at = @At("HEAD"))
        private void textstudio_chat$captureSender(net.minecraft.network.protocol.game.ClientboundPlayerChatPacket packet, CallbackInfo ci) {
            IChatComponentSync sync = MinecraftChat.extension(IChatComponentSync.class);
            if (sync != null) sync.textstudio_chat$setCapturedSender(packet.sender());
        }

        @Inject(method = "sendChat", at = @At("HEAD"))
        private void textstudio_chat$captureSelf(String message, CallbackInfo ci) {
            net.minecraft.client.player.LocalPlayer player = KineticClientRuntime.localPlayer();
            IChatComponentSync sync = MinecraftChat.extension(IChatComponentSync.class);
            if (player != null && sync != null) sync.textstudio_chat$setCapturedSender(player.getUUID());
        }

        @ModifyConstant(method = {"sendChat", "sendCommand"}, constant = @Constant(intValue = 256))
        private int textstudio_chat$increaseTrimLimit(int original) { return ChatConfig.maxChatLength; }
    }

    @Mixin(EditBox.class)
    public static abstract class EditBoxTweaks {
        @ModifyVariable(method = "setMaxLength", at = @At("HEAD"), argsOnly = true)
        private int textstudio_chat$onSetMaxLength(int length) {
            if (length == 256) return ChatConfig.maxChatLength;
            return length;
        }
    }

    @Mixin(net.minecraft.client.gui.screens.social.PlayerEntry.class)
    public static abstract class SocialInteractionsTweaks {
        @Shadow @Nullable private Button reportButton;

        @Inject(method = "<init>", at = @At("RETURN"))
        private void textstudio_chat$hideReportButton(Minecraft p_240760_, net.minecraft.client.gui.screens.social.SocialInteractionsScreen p_240761_, UUID p_240762_, String p_240763_, java.util.function.Supplier<ResourceLocation> p_240764_, boolean p_240765_, CallbackInfo ci) {
            if (ChatConfig.stripChatSignatures && this.reportButton != null) {
                this.reportButton.visible = false;
                this.reportButton.active = false;
            }
        }
    }

    @Mixin(net.minecraft.client.multiplayer.chat.report.ReportingContext.class)
    public static abstract class ReportingContextTweaks {
        @Inject(method = "hasReporting", at = @At("HEAD"), cancellable = true)
        private void textstudio_chat$disableReportingContext(CallbackInfoReturnable<Boolean> cir) {
            if (ChatConfig.stripChatSignatures) {
                cir.setReturnValue(false);
            }
        }
    }

    //? if >=1.21 {
    /*@Mixin(net.minecraft.client.gui.screens.reporting.AbstractReportScreen.class)
    *///?} else {
    @Mixin(net.minecraft.client.gui.screens.reporting.ChatReportScreen.class)
    //?}
    public static abstract class ReportScreenTweaks {
        @Inject(method = "init", at = @At("HEAD"), cancellable = true)
        private void textstudio_chat$abortReportScreen(CallbackInfo ci) {
            //? if >=1.21 {
            /*if (!((Object) this instanceof net.minecraft.client.gui.screens.reporting.ChatReportScreen)) return;
            *///?}
            if (ChatConfig.stripChatSignatures) {
                KineticGui.closeScreen();
                ci.cancel();
            }
        }
    }
}
