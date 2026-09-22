package dev.xyat.textstudio.chat.client;

import dev.xyat.textstudio.chat.config.ChatConfigGui;

public final class ChatClientIntegration {
    private static boolean installed;

    private ChatClientIntegration() {
    }

    public static synchronized void install() {
        if (installed) return;
        installed = true;
        ChatConfigGui.load();
        ChatScreenControls.install();
    }
}
