package dev.xyat.textstudio.font.common.text;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Adds the existing optional packed effect field without changing vanilla style fields. */
public final class PackedStyleCodec {
    private PackedStyleCodec() {}

    public static <T> MapCodec<T> wrap(MapCodec<T> base, Function<T, Optional<Integer>> effects,
                                      BiFunction<T, Integer, T> attach) {
        // DFU versions differ in how optionalFieldOf handles malformed values. Keep it strict on both.
        MapCodec<Optional<Integer>> field = new MapCodec<>() {
            @Override
            public <A> com.mojang.serialization.DataResult<Optional<Integer>> decode(
                    com.mojang.serialization.DynamicOps<A> ops, com.mojang.serialization.MapLike<A> input) {
                A value = input.get("kf");
                return value == null ? com.mojang.serialization.DataResult.success(Optional.empty())
                        : Codec.INT.parse(ops, value).map(Optional::of);
            }

            @Override
            public <A> com.mojang.serialization.RecordBuilder<A> encode(Optional<Integer> input,
                    com.mojang.serialization.DynamicOps<A> ops, com.mojang.serialization.RecordBuilder<A> prefix) {
                return input.isPresent() ? prefix.add("kf", Codec.INT.encodeStart(ops, input.get())) : prefix;
            }

            @Override
            public <A> java.util.stream.Stream<A> keys(com.mojang.serialization.DynamicOps<A> ops) {
                return java.util.stream.Stream.of(ops.createString("kf"));
            }
        };
        return Codec.mapPair(base, field).xmap(
                pair -> pair.getSecond().map(packed -> attach.apply(pair.getFirst(), packed)).orElse(pair.getFirst()),
                value -> Pair.of(value, effects.apply(value)));
    }
}
