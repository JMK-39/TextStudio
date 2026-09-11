package dev.xyat.textstudio.font.api;

import dev.xyat.textstudio.font.common.text.AuthorNamePolicy;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuthorAPI {
    public static final int FLAG_RAINBOW = 1;
    public static final int FLAG_BOLD = 2;
    public static final int FLAG_STRIKETHROUGH = 4;
    public static final int FLAG_JITTER = 8;
    public static final int FLAG_GLITCH = 16;
    public static final int PUBLIC_STYLE_FLAGS = FLAG_RAINBOW | FLAG_BOLD | FLAG_STRIKETHROUGH;
    public static final int SPECIAL_AUTHOR_EFFECT = 255;

    public static final UUID AUTHOR_1 = UUID.fromString("d5a0221c-1f7b-4290-82ad-c1264f4db8d8");
    public static final UUID AUTHOR_2 = UUID.fromString("fd3e50f0-73e1-475a-8181-d652be327727");
    public static final UUID AUTHOR_3 = UUID.fromString("0bb94502-d887-472e-ab32-d36b9b2facd6");
    public static final UUID AUTHOR_4 = UUID.fromString("bf3efe8a-7d1a-4a7b-ae65-a9e704487613");
    public static final UUID AUTHOR_5 = UUID.fromString("7c09008d-96d5-45fc-afa9-b765c4190313");

    private static final Set<String> RESERVED_NAMES = new HashSet<>(Arrays.asList(
            "星野爱桃", "白仄黎", "CT_ice_tofu", "水煮鱼配虾饺", "呆猫大总管"
    ));

    public static class DisplayInfo {
        public String name = "";
        public int effect = 1;
        public boolean isRainbow = false;
        public boolean isBold = false;
        public boolean isStrikethrough = false;
        public boolean isJitter = false;
        public boolean isGlitch = false;
        public boolean isAuthor = false;
        public boolean isDynamic = false;
    }

    public static final Map<UUID, DisplayInfo> CLIENT_CACHE = new ConcurrentHashMap<>();

    public static boolean isAuthor(UUID uuid) {
        return AUTHOR_1.equals(uuid) || AUTHOR_2.equals(uuid) || AUTHOR_3.equals(uuid)
                || AUTHOR_4.equals(uuid) || AUTHOR_5.equals(uuid);
    }

    public static boolean isAuthor(Player player) {
        return player != null && isAuthor(player.getUUID());
    }

    public static boolean isReservedName(String name) {
        return AuthorNamePolicy.containsReservedName(name, RESERVED_NAMES);
    }

    public static boolean isNameTaken(MinecraftServer server, String name, UUID requesterUUID) {
        if (server == null || name == null) return false;
        String cleanName = AuthorNamePolicy.canonicalVisibleName(name);
        return server.getPlayerList().getPlayers().stream().anyMatch(player -> {
            if (player.getUUID().equals(requesterUUID)) return false;
            String otherName = getCustomName(player);
            return otherName != null && cleanName.equals(AuthorNamePolicy.canonicalVisibleName(otherName));
        });
    }

    public static int sanitizeStyleFlags(UUID uuid, int flags) {
        return isAuthor(uuid) ? flags : flags & PUBLIC_STYLE_FLAGS;
    }

    public static int getDefaultStyleFlags(UUID uuid) {
        if (AUTHOR_1.equals(uuid)) return 0;
        if (AUTHOR_3.equals(uuid)) return FLAG_BOLD | FLAG_STRIKETHROUGH | FLAG_RAINBOW;
        if (isAuthor(uuid)) return FLAG_RAINBOW | FLAG_BOLD | FLAG_JITTER;
        return FLAG_RAINBOW;
    }

    public static int getDefaultNameEffect(UUID uuid) {
        if (AUTHOR_1.equals(uuid)) return SPECIAL_AUTHOR_EFFECT;
        if (AUTHOR_3.equals(uuid)) return 1;
        return isAuthor(uuid) ? 11 : 1;
    }

    public static String getDefaultCustomName(UUID uuid) {
        if (AUTHOR_1.equals(uuid)) return "星野爱桃";
        if (AUTHOR_2.equals(uuid)) return "白仄黎";
        if (AUTHOR_3.equals(uuid)) return "CT_ice_tofu";
        if (AUTHOR_4.equals(uuid)) return "水煮鱼配虾饺";
        if (AUTHOR_5.equals(uuid)) return "呆猫大总管";
        return null;
    }

    public static String getCustomName(Player player) {
        if (player == null) return null;
        if (player.level().isClientSide()) {
            return getDisplayInfo(player.getUUID(), player.getGameProfile().getName()).name;
        }
        String rawName = player.getGameProfile().getName();
        if (player instanceof IAuthorName auth) {
            String diy = auth.textstudio_font$getCustomdiyname();
            if (diy != null && !diy.isEmpty()) rawName = diy;
        }
        return rawName;
    }

    public static Component createStyledName(DisplayInfo info) {
        if (info == null) return Component.empty();
        int flags = 0;
        if (info.isRainbow) flags |= FLAG_RAINBOW;
        if (info.isBold) flags |= FLAG_BOLD;
        if (info.isStrikethrough) flags |= FLAG_STRIKETHROUGH;
        if (info.isJitter) flags |= FLAG_JITTER;
        if (info.isGlitch) flags |= FLAG_GLITCH;
        return createStyledName(info.name, info.effect, flags, info.isAuthor);
    }

    public static Component createStyledName(String name, int effectId, int flags) {
        return createStyledName(name, effectId, flags, true);
    }

    public static Component createStyledName(String name, int effectId, int flags, boolean advancedAllowed) {
        String displayName = name == null || name.isEmpty() ? "Player" : name;
        int safeFlags = advancedAllowed ? flags : flags & PUBLIC_STYLE_FLAGS;
        MutableComponent component = Component.literal(displayName);
        Style style = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF));
        ((IStyle) style).textstudio_font$setStyleData(new IStyle.TextEffectStyleData(
                effectId,
                (safeFlags & FLAG_RAINBOW) != 0,
                (safeFlags & FLAG_BOLD) != 0,
                (safeFlags & FLAG_STRIKETHROUGH) != 0,
                (safeFlags & FLAG_JITTER) != 0,
                (safeFlags & FLAG_GLITCH) != 0,
                advancedAllowed
        ));
        if ((safeFlags & FLAG_BOLD) != 0) style = style.withBold(true);
        if ((safeFlags & FLAG_STRIKETHROUGH) != 0) style = style.withStrikethrough(true);
        return component.withStyle(style);
    }

    public static DisplayInfo getDisplayInfo(UUID uuid, String originalName) {
        return CLIENT_CACHE.computeIfAbsent(uuid, key -> {
            DisplayInfo info = new DisplayInfo();
            info.name = Optional.ofNullable(getDefaultCustomName(uuid)).orElseGet(() ->
                    originalName == null || originalName.isEmpty() ? "Player" : originalName);
            info.effect = getDefaultNameEffect(uuid);
            int flags = sanitizeStyleFlags(uuid, getDefaultStyleFlags(uuid));
            info.isRainbow = (flags & FLAG_RAINBOW) != 0;
            info.isBold = (flags & FLAG_BOLD) != 0;
            info.isStrikethrough = (flags & FLAG_STRIKETHROUGH) != 0;
            info.isJitter = (flags & FLAG_JITTER) != 0;
            info.isGlitch = (flags & FLAG_GLITCH) != 0;
            info.isAuthor = isAuthor(uuid);
            return info;
        });
    }
}
