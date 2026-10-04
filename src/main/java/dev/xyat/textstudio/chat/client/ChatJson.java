package dev.xyat.textstudio.chat.client;

import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Chat lines travel and are stored as component JSON. 1.20.1 writes it without registries, 1.21.1 with them, and
 * 26.1 only offers the component codec; the JSON is the same on every version.
 */
public final class ChatJson {
    private ChatJson() {
    }

    public static String toJson(Component component) {
        //? if >=26.1 {
        /*return net.minecraft.network.chat.ComponentSerialization.CODEC
                .encodeStart(registries().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), component)
                .getOrThrow().toString();
        *///?} else if >=1.21 {
        /*return Component.Serializer.toJson(component, registries());
        *///?} else {
        return Component.Serializer.toJson(component);
        //?}
    }

    public static @Nullable Component fromJson(String json) {
        //? if >=26.1 {
        /*return net.minecraft.network.chat.ComponentSerialization.CODEC
                .parse(registries().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), com.google.gson.JsonParser.parseString(json))
                .result().orElse(null);
        *///?} else if >=1.21 {
        /*return Component.Serializer.fromJson(json, registries());
        *///?} else {
        return Component.Serializer.fromJson(json);
        //?}
    }

    //? if >=1.21 {
    /*// Components may refer to registry entries, so they are read with the world's registries when there is one.
    private static net.minecraft.core.HolderLookup.Provider registries() {
        LocalPlayer player = KineticClientRuntime.localPlayer();
        return player != null ? player.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
    }
    *///?}
}
