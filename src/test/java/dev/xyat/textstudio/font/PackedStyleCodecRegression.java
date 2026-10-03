package dev.xyat.textstudio.font;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import dev.xyat.textstudio.font.common.text.PackedStyleCodec;
import java.util.Optional;

public final class PackedStyleCodecRegression {
    private record Value(int color, Integer effects) {}

    public static void main(String[] args) {
        MapCodec<Value> base = Codec.INT.fieldOf("color").xmap(n -> new Value(n, null), Value::color);
        var codec = PackedStyleCodec.wrap(base, v -> Optional.ofNullable(v.effects()),
                (v, packed) -> new Value(v.color(), packed)).codec();
        for (int packed : new int[] {0, 255, 256, 4096, 8192, 16383}) {
            var input = new Value(0x55AAFF, packed);
            var json = codec.encodeStart(JsonOps.INSTANCE, input).result().orElseThrow();
            if (json.getAsJsonObject().get("kf").getAsInt() != packed) throw new AssertionError("Missing packed effect field");
            if (!input.equals(codec.parse(JsonOps.INSTANCE, json).result().orElseThrow())) throw new AssertionError("Effects lost in round trip");
        }
        var plain = codec.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"color\":7,\"extra\":true}")).result().orElseThrow();
        if (plain.effects() != null || plain.color() != 7) throw new AssertionError("Legacy styles changed");
        var json = codec.encodeStart(JsonOps.INSTANCE, plain).result().orElseThrow().getAsJsonObject();
        if (json.has("kf")) throw new AssertionError("Plain style gained effects");
        if (codec.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"color\":7,\"kf\":\"invalid\"}")).error().isEmpty())
            throw new AssertionError("Malformed packed effects accepted");
        System.out.println("PackedStyleCodecRegression: round trips and legacy/malformed styles passed");
    }
}
