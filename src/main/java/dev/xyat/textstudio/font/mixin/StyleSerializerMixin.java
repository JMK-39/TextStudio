package dev.xyat.textstudio.font.mixin;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import dev.xyat.textstudio.font.api.IStyle;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Type;

@Mixin(Style.Serializer.class)
public class StyleSerializerMixin {
    //? if >=1.21 {
    /*@org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final @org.spongepowered.asm.mixin.Mutable
    private static com.mojang.serialization.MapCodec<Style> MAP_CODEC;
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final @org.spongepowered.asm.mixin.Mutable
    private static com.mojang.serialization.Codec<Style> CODEC;
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final @org.spongepowered.asm.mixin.Mutable
    private static net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Style> TRUSTED_STREAM_CODEC;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void textstudio_font$extendCodec(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        MAP_CODEC = dev.xyat.textstudio.font.common.text.PackedStyleCodec.wrap(MAP_CODEC,
                style -> java.util.Optional.ofNullable(((IStyle) style).textstudio_font$getStyleData())
                        .map(IStyle.TextEffectStyleData::pack),
                (style, packed) -> {
                    // Style's vanilla codec shares EMPTY. Effects must belong to a distinct instance.
                    Style result = style == Style.EMPTY
                            ? StyleConstructionAccess.textstudio_font$newStyle(null, null, null, null, null, null, null, null, null, null)
                            : style;
                    ((IStyle) result).textstudio_font$setStyleData(IStyle.TextEffectStyleData.unpack(packed));
                    return result;
                });
        CODEC = MAP_CODEC.codec();
        TRUSTED_STREAM_CODEC = net.minecraft.network.codec.ByteBufCodecs.fromCodecWithRegistriesTrusted(CODEC);
    }
    *///?} else {
    @Inject(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/network/chat/Style;", at = @At("RETURN"))
    private void textstudio_font$deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx, CallbackInfoReturnable<Style> cir) {
        Style result = cir.getReturnValue();
        if (result != null && jsonElement.isJsonObject()) {
            JsonObject json = jsonElement.getAsJsonObject();
            if (json.has("kf")) {
                int packedData = json.get("kf").getAsInt();
                ((IStyle) result).textstudio_font$setStyleData(IStyle.TextEffectStyleData.unpack(packedData));
            }
        }
    }

    @Inject(method = "serialize(Lnet/minecraft/network/chat/Style;Ljava/lang/reflect/Type;Lcom/google/gson/JsonSerializationContext;)Lcom/google/gson/JsonElement;", at = @At("RETURN"))
    private void textstudio_font$serialize(Style style, Type type, JsonSerializationContext ctx, CallbackInfoReturnable<JsonElement> cir) {
        JsonElement result = cir.getReturnValue();
        if (result != null && result.isJsonObject()) {
            IStyle.TextEffectStyleData data = ((IStyle) style).textstudio_font$getStyleData();
            if (data != null) {
                result.getAsJsonObject().addProperty("kf", data.pack());
            }
        }
    }
    //?}
}
