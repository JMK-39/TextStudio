package dev.xyat.textstudio.chat.client;

import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.client.widget.KineticControl;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets;
import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import dev.xyat.kineticcore.api.minecraft.MinecraftChat;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

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
        SUBSCRIPTIONS.add(KineticClientEvents.onScreenRenderAfter(ChatScreenControls::onScreenRender));
        SUBSCRIPTIONS.add(KineticClientEvents.onScreenMouseButtonPressedBefore(ChatScreenControls::onMousePressed));
        SUBSCRIPTIONS.add(KineticClientEvents.onScreenMouseButtonReleasedBefore(ChatScreenControls::onMouseReleased));
    }

    private static void onScreenInit(KineticClientEvents.ScreenInitContext context) {
        if (!(context.screen() instanceof ChatScreen screen)) return;
        EditBox input = context.findExistingListener(EditBox.class);
        if (input == null) return;

        KineticControl button = KineticWidgets.createCompactButton(
                5,
                screen.height - 30,
                55,
                Component.translatable("gui.textstudio.chat.open_canvas"),
                Component.translatable("gui.textstudio.chat.open_canvas.desc"),
                () -> KineticClientRuntime.openScreen(
                        new ChatCopyCanvasScreen(screen, MinecraftChat.activeTrimmedMessages())
                )
        );
        ENTRIES.put(screen, new Entry(button, input));
    }

    private static void onScreenRender(Screen screen, net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Entry entry = ENTRIES.get(screen);
        if (entry == null) return;
        if (entry.input().getValue().startsWith("/")) return;
        KineticWidgets.renderControl(entry.button(), graphics, mouseX, mouseY, partialTick);
    }

    private static void onMousePressed(KineticClientEvents.ScreenMouseButtonContext context) {
        Entry entry = ENTRIES.get(context.screen());
        if (entry == null || entry.input().getValue().startsWith("/")) return;
        if (entry.button().mouseClicked(context.mouseX(), context.mouseY(), context.button())) {
            context.cancel();
        }
    }

    private static void onMouseReleased(KineticClientEvents.ScreenMouseButtonContext context) {
        Entry entry = ENTRIES.get(context.screen());
        if (entry == null || entry.input().getValue().startsWith("/")) return;
        if (entry.button().mouseReleased(context.mouseX(), context.mouseY(), context.button())) {
            context.cancel();
        }
    }

    private record Entry(KineticControl button, EditBox input) {
    }
}
