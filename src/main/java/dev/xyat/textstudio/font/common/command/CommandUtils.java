package dev.xyat.textstudio.font.common.command;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

public class CommandUtils {
    public static MutableComponent createHeader(String key) {
        return Component.translatable(key);
    }

    public static MutableComponent createSuggestCommand(String displayKey, String command, String descriptionKey) {
        return Component.translatable(displayKey).withStyle(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(descriptionKey).withStyle(ChatFormatting.GOLD))));
    }
}
