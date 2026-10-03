//? if >=1.21 {
/*package dev.xyat.textstudiovalidation;

import dev.xyat.textstudio.font.api.IStyle;
import dev.xyat.textstudio.font.mixin.StyleConstructionAccess;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.core.RegistryAccess;
import com.mojang.serialization.JsonOps;

@net.minecraftforge.fml.common.Mod("textstudio_validation")
public final class RuntimeStyleValidation {
    public RuntimeStyleValidation() {
        if (((IStyle) Style.EMPTY).textstudio_font$getStyleData() != null) throw new AssertionError("Shared EMPTY was mutated");
        for (int packed : new int[] {0, 255, 256, 4096, 8192, 16383}) {
            Style style = StyleConstructionAccess.textstudio_font$newStyle(null, null, null, null, null, null, null, null, null, null);
            ((IStyle) style).textstudio_font$setStyleData(IStyle.TextEffectStyleData.unpack(packed));
            var json = Style.Serializer.CODEC.encodeStart(JsonOps.INSTANCE, style).getOrThrow();
            Style decoded = Style.Serializer.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
            check(decoded, packed);
            String component = Component.Serializer.toJson(Component.literal("test").withStyle(style), RegistryAccess.EMPTY);
            check(Component.Serializer.fromJson(component, RegistryAccess.EMPTY).getStyle(), packed);
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), RegistryAccess.EMPTY);
            try {
                Style.Serializer.TRUSTED_STREAM_CODEC.encode(buffer, style);
                check(Style.Serializer.TRUSTED_STREAM_CODEC.decode(buffer), packed);
            } finally { buffer.release(); }
            check(style.withColor(0x123456).withColor((net.minecraft.network.chat.TextColor) null), packed);
        }
        if (((IStyle) Style.EMPTY).textstudio_font$getStyleData() != null) throw new AssertionError("Effects leaked into EMPTY");
        System.out.println("TEXTSTUDIO_RUNTIME_VALIDATION_PASS: style/component/network codec round trips and EMPTY isolation");
    }

    private static void check(Style style, int packed) {
        var data = ((IStyle) style).textstudio_font$getStyleData();
        if (data == null || data.pack() != packed) throw new AssertionError("Style effect round trip failed: " + packed);
    }
}
*///?}
