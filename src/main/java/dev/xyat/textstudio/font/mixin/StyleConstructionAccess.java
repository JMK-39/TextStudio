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
    // 26.1 added a shadow color and describes the font with FontDescription.
    //? if >=26.1 {
    /*@Invoker("<init>")
    static Style textstudio_font$newStyle(TextColor color, Integer shadowColor, Boolean bold, Boolean italic,
                                          Boolean underlined, Boolean strike, Boolean obfuscated, ClickEvent click,
                                          HoverEvent hover, String insertion, net.minecraft.network.chat.FontDescription font) {
        throw new AssertionError("Mixin constructor invoker was not applied");
    }
    *///?} else {
    @Invoker("<init>")
    static Style textstudio_font$newStyle(TextColor color, Boolean bold, Boolean italic, Boolean underlined,
                                          Boolean strike, Boolean obfuscated, ClickEvent click, HoverEvent hover,
                                          String insertion, ResourceLocation font) {
        throw new AssertionError("Mixin constructor invoker was not applied");
    }
    //?}
}
