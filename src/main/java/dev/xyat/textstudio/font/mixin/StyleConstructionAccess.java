package dev.xyat.textstudio.font.mixin;

import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Style.class)
public interface StyleConstructionAccess {
    @Invoker("<init>")
    static Style textstudio_font$newStyle(TextColor color, Boolean bold, Boolean italic, Boolean underlined,
                                          Boolean strike, Boolean obfuscated, ClickEvent click, HoverEvent hover,
                                          String insertion, ResourceLocation font) {
        throw new AssertionError("Mixin constructor invoker was not applied");
    }
}
