package dev.xyat.textstudio.chat.mixin.client;

import dev.xyat.textstudio.chat.util.ColorText;
import dev.xyat.textstudio.chat.ChatModule;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets.Scroll;
import dev.xyat.textstudio.chat.client.IChatComponentSync;
import dev.xyat.textstudio.chat.config.ChatConfig;
import dev.xyat.textstudio.chat.network.ChatSyncCodec;
import io.netty.buffer.Unpooled;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ChatClientMixins {

    @Mixin(ChatComponent.class)
    public static abstract class ChatComponentTweaks implements IChatComponentSync {
        @Shadow @Final private List<GuiMessage> allMessages;
        @Shadow @Final private Minecraft minecraft;
        @Shadow public abstract void rescaleChat();
        @Shadow public abstract int getLinesPerPage();
        @Shadow public abstract double getScale();

        @Unique private boolean textstudio_chat$isSyncing = false;
        @Unique private boolean textstudio_chat$isRefreshing = false;
        @Unique private String textstudio_chat$lastRawMessage = "";
        @Unique private int textstudio_chat$counter = 1;
        @Unique private boolean textstudio_chat$needsRefresh = false;
        @Unique private Component textstudio_chat$capturedOriginal;
        @Unique private long textstudio_chat$providedTimestamp = -1;
        @Unique private static final DateTimeFormatter textstudio_chat$TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        @Unique private UUID textstudio_chat$lastSenderUUID = null;

        @Unique private final Map<GuiMessage.Line, UUID> textstudio_chat$lineToUuidMap = new WeakHashMap<>();
        @Unique private final Map<Component, UUID> textstudio_chat$componentToUuidMap = new WeakHashMap<>();

        // 记录行和组件绑定的时间戳宽度，用于精确定位头像
        @Unique private final Map<GuiMessage.Line, Integer> textstudio_chat$lineToTsWidthMap = new WeakHashMap<>();
        @Unique private final Map<Component, Integer> textstudio_chat$componentToTsWidthMap = new WeakHashMap<>();
        @Unique private int textstudio_chat$lastTimestampWidth = 0;

        @Unique private GuiMessage.Line textstudio_chat$currentRenderingLine = null;

        @Override public void textstudio_chat$setSyncing(boolean syncing) { this.textstudio_chat$isSyncing = syncing; }
        @Override public void textstudio_chat$setProvidedTimestamp(long timestamp) { this.textstudio_chat$providedTimestamp = timestamp; }
        @Override public void textstudio_chat$setCapturedSender(UUID uuid) { this.textstudio_chat$lastSenderUUID = uuid; }

        @Unique
        private void textstudio_chat$sendSyncPayload(int type, byte[] payloadData) {
            ClientPacketListener connection = Minecraft.getInstance().getConnection();
            if (connection == null
                    || payloadData == null
                    || payloadData.length + 1 > ChatSyncCodec.MAX_SERVERBOUND_PACKET_BYTES) {
                return;
            }

            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(payloadData.length + 1));
            boolean queued = false;
            try {
                buf.writeByte(type);
                buf.writeBytes(payloadData);
                connection.send(new ServerboundCustomPayloadPacket(
                        new ResourceLocation("textstudio", "chat_sync"),
                        buf
                ));
                queued = true;
            } finally {
                if (!queued) buf.release();
            }
        }

        @ModifyConstant(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V", constant = @Constant(intValue = 100))
        private int textstudio_chat$expandChatHistory(int original) { return ChatConfig.maxChatHistoryLines; }

        @Inject(method = "addRecentChat", at = @At("HEAD"))
        private void textstudio_chat$syncInputHistory(String message, CallbackInfo ci) {
            if (!this.textstudio_chat$isSyncing && Minecraft.getInstance().getConnection() != null) {
                try {
                    textstudio_chat$sendSyncPayload(
                            ChatSyncCodec.TYPE_INPUT_LINE,
                            ChatSyncCodec.encodeInputLine(message, ChatConfig.maxChatLength)
                    );
                } catch (Exception ignored) {}
            }
        }

        @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V", at = @At("HEAD"))
        private void textstudio_chat$onAddMessageHead(Component component, MessageSignature signature, int tick, GuiMessageTag tag, boolean refresh, CallbackInfo ci) {
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

        @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
        private Component textstudio_chat$applyVisualDecorations(Component component) {
            if (this.textstudio_chat$isRefreshing) return component;
            MutableComponent root = Component.empty();

            int tsWidth = 0;
            if (ChatConfig.enableTimestamp) {
                long ts = textstudio_chat$providedTimestamp != -1 ? textstudio_chat$providedTimestamp : System.currentTimeMillis();
                String timeNumbers = LocalTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault()).format(textstudio_chat$TIME_FORMAT);
                String tsString = "[" + timeNumbers + "] ";
                root.append(ColorText.translatable("chat.textstudio.timestamp", timeNumbers));

                // 计算出当前时间戳的宽度，用于稍后渲染头像的 X 轴起始定位
                tsWidth = this.minecraft.font.width(tsString);
            }
            this.textstudio_chat$lastTimestampWidth = tsWidth;

            // 若开启了头像渲染，并且确定该消息来自玩家，则在此处植入 3 个空格（12像素）作为头像占位符
            if (ChatConfig.enableChatHeads && this.textstudio_chat$lastSenderUUID != null) {
                root.append(Component.literal("   "));
            }

            root.append(component);

            if (ChatConfig.enableCompactChat && textstudio_chat$counter > 1) {
                // 使用 I18N 替代硬编码
                root.append(ColorText.translatable("chat.textstudio.compact", textstudio_chat$counter));
            }
            return root;
        }

        @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V", at = @At("RETURN"))
        private void textstudio_chat$onAddMessageReturn(Component component, MessageSignature signature, int tick, GuiMessageTag guiTag, boolean refresh, CallbackInfo ci) {
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
                ChatComponentAccessor accessor = (ChatComponentAccessor) this;
                List<GuiMessage.Line> trimmed = accessor.getTrimmedMessages();
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
                    textstudio_chat$lineToTsWidthMap.put(firstLine, tsWidth);
                }
            }

            if (refresh) return;
            if (textstudio_chat$needsRefresh) { this.rescaleChat(); textstudio_chat$needsRefresh = false; }

            if (!this.textstudio_chat$isSyncing && Minecraft.getInstance().getConnection() != null) {
                try {
                    textstudio_chat$sendSyncPayload(
                            ChatSyncCodec.TYPE_CHAT_LINE,
                            ChatSyncCodec.encodeChatLine(
                                    Component.Serializer.toJson(this.textstudio_chat$capturedOriginal),
                                    System.currentTimeMillis(),
                                    textstudio_chat$lastSenderUUID,
                                    ChatConfig.maxChatLength
                            )
                    );
                } catch (Exception ignored) {}
            }
            textstudio_chat$lastSenderUUID = null;
        }

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

        @Unique private ResourceLocation textstudio_chat$getSkin(UUID uuid) {
            if (minecraft.getConnection() != null) {
                PlayerInfo info = minecraft.getConnection().getPlayerInfo(uuid);
                if (info != null) return info.getSkinLocation();
            }
            return DefaultPlayerSkin.getDefaultSkin(uuid);
        }

        @Inject(method = "clearMessages", at = @At("HEAD"))
        private void textstudio_chat$onClear(boolean pResetScroll, CallbackInfo ci) {
            textstudio_chat$lineToUuidMap.clear();
            textstudio_chat$componentToUuidMap.clear();
            textstudio_chat$lineToTsWidthMap.clear();
            textstudio_chat$componentToTsWidthMap.clear();
        }
    }

    @Mixin(ChatScreen.class)
    public static abstract class ChatScreenTweaks extends net.minecraft.client.gui.screens.Screen {
        protected ChatScreenTweaks(Component title) { super(title); }
        @Unique private boolean textstudio_chat$isDraggingScrollbar = false;

        @Unique private int[] textstudio_chat$getScrollbarBounds(ChatComponent chat) {
            double scale = this.minecraft != null ? this.minecraft.options.chatScale().get() : 1.0;
            int chatRight = (int) ((chat.getWidth() + 4) * scale);
            int visibleLines = chat.getLinesPerPage();
            int sbH = (int) (visibleLines * 9 * scale);
            int chatBottom = this.height - 40;
            return new int[] { chatRight + 2, chatBottom - sbH, 6, sbH };
        }

        @Inject(method = "render", at = @At("RETURN"))
        private void textstudio_chat$renderScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
            if (!ChatConfig.enableDraggableScrollbar || this.minecraft == null) return;
            ChatComponent chat = this.minecraft.gui.getChat();
            ChatComponentAccessor accessor = (ChatComponentAccessor) chat;
            int totalLines = accessor.getTrimmedMessages().size();
            int visibleLines = chat.getLinesPerPage();
            int maxScroll = totalLines - visibleLines;
            if (maxScroll <= 0) return;

            int[] bounds = textstudio_chat$getScrollbarBounds(chat);
            int sbX = bounds[0], sbY = bounds[1], sbW = bounds[2], sbH = bounds[3];

            int thumbHeight = Scroll.calculateThumbHeight(sbH, visibleLines, totalLines, 10);

            boolean isHovered = mouseX >= sbX && mouseX <= sbX + sbW && mouseY >= sbY && mouseY <= sbY + sbH;
            boolean isDragging = this.textstudio_chat$isDraggingScrollbar || isHovered;

            int renderScroll = maxScroll - accessor.getChatScrollbarPos();
            Scroll.renderScrollbar(guiGraphics, mouseX, mouseY, sbX, sbY, sbW, sbH, thumbHeight, maxScroll, renderScroll, isDragging);

            if (this.textstudio_chat$isDraggingScrollbar) {
                if (GLFW.glfwGetMouseButton(this.minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
                    textstudio_chat$dragScrollbar(chat, accessor, mouseY, sbY, sbH, thumbHeight, maxScroll);
                } else { this.textstudio_chat$isDraggingScrollbar = false; }
            }
        }

        @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
        private void textstudio_chat$onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
            if (!ChatConfig.enableDraggableScrollbar || button != 0 || this.minecraft == null) return;
            ChatComponent chat = this.minecraft.gui.getChat();
            ChatComponentAccessor accessor = (ChatComponentAccessor) chat;
            int totalLines = accessor.getTrimmedMessages().size();
            int visibleLines = chat.getLinesPerPage();
            int maxScroll = totalLines - visibleLines;
            if (maxScroll <= 0) return;

            int[] bounds = textstudio_chat$getScrollbarBounds(chat);
            if (mouseX >= bounds[0] && mouseX <= bounds[0] + bounds[2] && mouseY >= bounds[1] && mouseY <= bounds[1] + bounds[3]) {
                this.textstudio_chat$isDraggingScrollbar = true;
                int thumbHeight = Scroll.calculateThumbHeight(bounds[3], visibleLines, totalLines, 10);
                textstudio_chat$dragScrollbar(chat, accessor, mouseY, bounds[1], bounds[3], thumbHeight, maxScroll);
                cir.setReturnValue(true);
            }
        }

        @Unique private void textstudio_chat$dragScrollbar(ChatComponent chat, ChatComponentAccessor accessor, double mouseY, int trackY, int trackHeight, int thumbHeight, int maxScroll) {
            int calculatedOffset = Scroll.calculateScrollOffset(mouseY, trackY, trackHeight, thumbHeight, maxScroll);
            int targetScroll = maxScroll - calculatedOffset;
            int delta = targetScroll - accessor.getChatScrollbarPos();
            if (delta != 0) chat.scrollChat(delta);
        }
    }

    @Mixin(ClientPacketListener.class)
    public static class ClientPacketTweaks {
        @Inject(method = "handlePlayerChat", at = @At("HEAD"))
        private void textstudio_chat$captureSender(net.minecraft.network.protocol.game.ClientboundPlayerChatPacket packet, CallbackInfo ci) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gui.getChat() instanceof IChatComponentSync sync) { sync.textstudio_chat$setCapturedSender(packet.sender()); }
        }

        @Inject(method = "sendChat", at = @At("HEAD"))
        private void textstudio_chat$captureSelf(String message, CallbackInfo ci) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.gui.getChat() instanceof IChatComponentSync sync) { sync.textstudio_chat$setCapturedSender(mc.player.getUUID()); }
        }

        @ModifyConstant(method = {"sendChat", "sendCommand"}, constant = @Constant(intValue = 256))
        private int textstudio_chat$increaseTrimLimit(int original) { return ChatConfig.maxChatLength; }

        @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
        private void textstudio_chat$receiveSyncHistory(ClientboundCustomPayloadPacket packet, CallbackInfo ci) {
            ResourceLocation id = packet.getIdentifier();
            if (id.getNamespace().equals("textstudio") && id.getPath().equals("chat_sync")) {
                ci.cancel();
                int type;
                byte[] payloadData;
                FriendlyByteBuf buf = packet.getData();
                try {
                    int packetBytes = buf.readableBytes();
                    if (packetBytes < 1) return;

                    type = buf.readUnsignedByte();
                    if (!ChatSyncCodec.isClientboundType(type)) return;

                    int payloadBytes = buf.readableBytes();
                    if (type == ChatSyncCodec.TYPE_CHAT_HISTORY) {
                        if (payloadBytes < 1 || payloadBytes > ChatSyncCodec.MAX_COMPRESSED_HISTORY_BYTES) return;
                    } else if (packetBytes > ChatSyncCodec.MAX_SERVERBOUND_PACKET_BYTES) {
                        return;
                    }

                    payloadData = new byte[payloadBytes];
                    buf.readBytes(payloadData);
                } finally {
                    buf.release();
                }

                Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> {
                    if (mc.gui.getChat() instanceof IChatComponentSync syncable) {
                        syncable.textstudio_chat$setSyncing(true);
                        try {
                            if (type == ChatSyncCodec.TYPE_CHAT_HISTORY) {
                                List<ChatSyncCodec.ChatLine> history =
                                        ChatSyncCodec.decodeCompressedHistory(
                                                payloadData,
                                                ChatSyncCodec.MAX_HISTORY_LINES,
                                                ChatSyncCodec.MAX_CHAT_LENGTH,
                                                ChatSyncCodec.clampHistoryLines(ChatConfig.maxChatHistoryLines)
                                        );
                                for (ChatSyncCodec.ChatLine line : history) {
                                    Component component = Component.Serializer.fromJson(line.json());
                                    if (component != null) {
                                        syncable.textstudio_chat$setProvidedTimestamp(line.timestamp());
                                        syncable.textstudio_chat$setCapturedSender(line.senderUuid());
                                        mc.gui.getChat().addMessage(component);
                                    }
                                }
                            } else {
                                String input = ChatSyncCodec.decodeInputLine(
                                        payloadData,
                                        ChatSyncCodec.MAX_CHAT_LENGTH
                                );
                                if (input.length() <= ChatSyncCodec.wireStringLimit(ChatConfig.maxChatLength)) {
                                    mc.gui.getChat().addRecentChat(input);
                                }
                            }
                        } catch (IOException | RuntimeException exception) {
                            ChatModule.LOGGER.warn("Rejected malformed server chat sync payload: {}",
                                    exception.getMessage());
                        } finally {
                            syncable.textstudio_chat$setSyncing(false);
                            syncable.textstudio_chat$setProvidedTimestamp(-1);
                            syncable.textstudio_chat$setCapturedSender(null);
                        }
                    }
                });
            }
        }
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

    @Mixin(net.minecraft.client.gui.screens.reporting.ChatReportScreen.class)
    public static abstract class ReportScreenTweaks {
        @Inject(method = "init", at = @At("HEAD"), cancellable = true)
        private void textstudio_chat$abortReportScreen(CallbackInfo ci) {
            if (ChatConfig.stripChatSignatures) {
                Minecraft.getInstance().setScreen(null);
                ci.cancel();
            }
        }
    }
}
