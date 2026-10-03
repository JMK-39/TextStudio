package dev.xyat.textstudio.chat.client;

import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.client.gui.KineticGui;
import dev.xyat.kineticcore.api.client.gui.input.MouseDragInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.scroll.KineticScrollController;
import dev.xyat.kineticcore.api.client.gui.widget.KineticButton;
import dev.xyat.kineticcore.api.client.gui.widget.KineticCustomControl;
import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import dev.xyat.kineticcore.api.minecraft.MinecraftChat;
import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.textstudio.chat.config.ChatConfig;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** 聊天界面注入控件：打开复制画布按钮与可拖拽滚动条 / Chat-screen controls: copy-canvas button and draggable scrollbar. */
public final class ChatScreenControls {
    private static final List<KineticEventSubscription> SUBSCRIPTIONS = new ArrayList<>();
    private static final Map<Screen, Entry> ENTRIES = new WeakHashMap<>();
    private static boolean installed;

    private ChatScreenControls() {
    }

    public static synchronized void install() {
        if (installed) return;
        installed = true;
        SUBSCRIPTIONS.add(KineticClientEvents.onScreenInitAfter(ChatScreenControls::onScreenInit));
        SUBSCRIPTIONS.add(KineticClientEvents.onScreenRenderBefore(ChatScreenControls::onScreenRender));
    }

    private static void onScreenInit(KineticClientEvents.ScreenInitContext context) {
        if (!(context.screen() instanceof ChatScreen screen)) return;
        EditBox input = context.findExistingListener(EditBox.class);
        if (input == null) return;

        KineticButton button = context.addButton(
                5,
                screen.height - 30,
                55,
                KineticI18n.translatable("gui.textstudio.chat.open_canvas"),
                () -> KineticGui.openChild(new ChatCopyCanvasPage(MinecraftChat.activeTrimmedMessages()))
        );
        button.setTooltip(KineticI18n.translatable("gui.textstudio.chat.open_canvas.desc"));
        ChatScrollbar scrollbar = context.addControl(new ChatScrollbar(screen));
        ENTRIES.put(screen, new Entry(button, input, scrollbar));
    }

    /** 以 "/" 开头输入命令时隐藏按钮 / Hides the button while a command is being typed. */
    private static void onScreenRender(Screen screen, KineticGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Entry entry = ENTRIES.get(screen);
        if (entry == null) return;
        boolean visible = !entry.input().getValue().startsWith("/");
        entry.button().setControlVisible(visible);
        entry.button().setEnabled(visible);
        entry.scrollbar().sync();
    }

    private record Entry(KineticButton button, EditBox input, ChatScrollbar scrollbar) {
    }

    /** Only replace the vanilla thumb when this screen has a usable custom scrollbar. */
    public static boolean replacesVanillaScrollbar() {
        Entry entry = ENTRIES.get(dev.xyat.kineticcore.api.runtime.KineticClientRuntime.currentScreen());
        return entry != null && ChatConfig.enableDraggableScrollbar && entry.scrollbar().sync() > 0;
    }

    /**
     * 聊天滚动条：原版滚动位置 0 表示最底部，这里映射为 Kinetic 滚动偏移（0 = 顶部）。
     * Chat scrollbar. Vanilla chat position 0 is the bottom; it is mapped to a Kinetic offset (0 = top).
     */
    private static final class ChatScrollbar extends KineticCustomControl {
        private static final int WIDTH = 6;
        private static final int MIN_THUMB = 10;

        private final Screen screen;
        private final KineticScrollController scroll = new KineticScrollController();
        private boolean dragging;

        ChatScrollbar(Screen screen) {
            super(0, 0, WIDTH, 1);
            this.screen = screen;
        }

        /** 同步位置与滚动范围，返回最大滚动行数 / Syncs bounds and range; returns the max scroll in lines. */
        int sync() {
            double scale = MinecraftChat.scale();
            int visibleLines = MinecraftChat.linesPerPage();
            int totalLines = MinecraftChat.activeTrimmedMessageCount();
            int maxScroll = totalLines - visibleLines;
            int trackHeight = Math.max(1, (int) (visibleLines * 9 * scale));
            moveControlX((int) ((MinecraftChat.width() + 4) * scale) + 2);
            moveControlY(screen.height - 40 - trackHeight);
            resizeControlHeight(trackHeight);
            setControlVisible(ChatConfig.enableDraggableScrollbar && maxScroll > 0);
            if (maxScroll <= 0) return 0;
            scroll.updateRange(maxScroll, totalLines, visibleLines);
            if (!dragging) scroll.setOffset(maxScroll - MinecraftChat.activeScrollbarPosition());
            return maxScroll;
        }

        private void apply(int maxScroll) {
            int delta = (maxScroll - scroll.offset()) - MinecraftChat.activeScrollbarPosition();
            if (delta != 0) MinecraftChat.scroll(delta);
        }

        @Override
        protected void render(KineticGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (sync() <= 0) return;
            scroll.render(graphics, mouseX, mouseY, controlX(), controlY(), controlWidth(), controlHeight(), MIN_THUMB);
        }

        @Override
        protected boolean onMouseClick(MouseInput input) {
            int maxScroll = sync();
            if (maxScroll <= 0) return false;
            if (!scroll.beginDrag(input.x(), input.y(), input.button(), controlX(), controlY(), controlWidth(), controlHeight(), MIN_THUMB)) {
                return false;
            }
            dragging = true;
            apply(maxScroll);
            return true;
        }

        @Override
        protected boolean onMouseDrag(MouseDragInput input) {
            if (!dragging) return false;
            int maxScroll = sync();
            if (maxScroll <= 0) return false;
            scroll.drag(input.y(), controlY(), controlHeight(), MIN_THUMB);
            apply(maxScroll);
            return true;
        }

        @Override
        protected boolean onMouseRelease(MouseInput input) {
            if (!dragging) return false;
            dragging = false;
            scroll.release(input.button());
            return true;
        }
    }
}
