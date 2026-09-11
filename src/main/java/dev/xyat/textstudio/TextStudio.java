package dev.xyat.textstudio;

import dev.xyat.textstudio.chat.ChatModule;
import dev.xyat.textstudio.font.FontModule;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TextStudio.MODID)
public final class TextStudio {
    public static final String MODID = "textstudio";

    public TextStudio(FMLJavaModLoadingContext context) {
        new ChatModule(context);
        new FontModule();
    }
}
