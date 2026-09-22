package dev.xyat.textstudio.chat.client;

import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import java.util.ArrayList;
import java.util.List;

import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.StateButton;
import dev.xyat.kineticcore.api.minecraft.MinecraftChat;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.WeakHashMap;

public final class ChatScreenControls {
    // These subscriptions remain active for the lifetime of this module.
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

        StateButton button = KineticWidgets.createCompactButton(
                5,
                screen.height - 30,
                55,
                Component.translatable("gui.textstudio.chat.open_canvas"),
                Component.translatable("gui.textstudio.chat.open_canvas.desc"),
                () -> KineticClientRuntime.openScreen(
                        new ChatCopyCanvasScreen(screen, MinecraftChat.activeTrimmedMessages())
                )
        );
        context.addControl(button);
        ENTRIES.put(screen, new Entry(button, input));
    }

    private static void onScreenRender(Screen screen, net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Entry entry = ENTRIES.get(screen);
        if (entry == null) return;
        KineticWidgets.setExternalWidgetVisible(
                entry.button(),
                !entry.input().getValue().startsWith("/")
        );
    }

    private record Entry(StateButton button, EditBox input) {
    }
}
