package dev.xyat.textstudio.font;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.xyat.textstudio.font.api.AuthorAPI;
import dev.xyat.textstudio.font.api.IAuthorName;
import dev.xyat.textstudio.font.config.AuthorConfig;
import dev.xyat.textstudio.font.common.command.CommandUtils;
import dev.xyat.textstudio.font.common.text.AuthorNamePolicy;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

public class AuthorCommand {
    private static final String LANG_PREFIX = "cmd.textstudio.font.";
    private static final int MAX_NAME_CODEPOINTS = 24;

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        var name = Commands.literal("name");
        name.then(Commands.literal("help").executes(ctx -> sendHelp(ctx.getSource())));
        name.then(Commands.literal("set").then(Commands.argument("name", StringArgumentType.greedyString()).executes(context -> setName(context, StringArgumentType.getString(context, "name")))));
        name.then(Commands.literal("effect").then(Commands.argument("id", IntegerArgumentType.integer(1, 255)).executes(context -> setEffect(context, IntegerArgumentType.getInteger(context, "id")))));

        var toggle = Commands.literal("toggle");
        toggle.then(Commands.literal("rainbow").then(Commands.argument("enable", BoolArgumentType.bool()).executes(ctx -> toggleStyle(ctx.getSource(), AuthorAPI.FLAG_RAINBOW, BoolArgumentType.getBool(ctx, "enable"), "rainbow", false))));
        toggle.then(Commands.literal("bold").then(Commands.argument("enable", BoolArgumentType.bool()).executes(ctx -> toggleStyle(ctx.getSource(), AuthorAPI.FLAG_BOLD, BoolArgumentType.getBool(ctx, "enable"), "bold", false))));
        toggle.then(Commands.literal("strike").then(Commands.argument("enable", BoolArgumentType.bool()).executes(ctx -> toggleStyle(ctx.getSource(), AuthorAPI.FLAG_STRIKETHROUGH, BoolArgumentType.getBool(ctx, "enable"), "strike", false))));
        toggle.then(Commands.literal("jitter").requires(source -> source.isPlayer() && AuthorAPI.isAuthor(source.getPlayer())).then(Commands.argument("enable", BoolArgumentType.bool()).executes(ctx -> toggleStyle(ctx.getSource(), AuthorAPI.FLAG_JITTER, BoolArgumentType.getBool(ctx, "enable"), "jitter", true))));
        toggle.then(Commands.literal("glitch").requires(source -> source.isPlayer() && AuthorAPI.isAuthor(source.getPlayer())).then(Commands.argument("enable", BoolArgumentType.bool()).executes(ctx -> toggleStyle(ctx.getSource(), AuthorAPI.FLAG_GLITCH, BoolArgumentType.getBool(ctx, "enable"), "glitch", true))));

        name.then(toggle);
        name.then(Commands.literal("clear").executes(AuthorCommand::clearName));
        name.executes(ctx -> sendHelp(ctx.getSource()));
        root.then(name);
        root.then(Commands.literal("rename").then(Commands.argument("name", StringArgumentType.greedyString()).executes(context -> setName(context, StringArgumentType.getString(context, "name")))));
    }

    private static int sendHelp(CommandSourceStack source) {
        source.sendSuccess(() -> CommandUtils.createHeader("cmd.textstudio.font.desc"), false);
        source.sendSuccess(() -> CommandUtils.createSuggestCommand(LANG_PREFIX + "help.syntax.rename", "/kt font rename ", LANG_PREFIX + "help.set.desc"), false);
        source.sendSuccess(() -> CommandUtils.createSuggestCommand(LANG_PREFIX + "help.syntax.effect", "/kt font name effect ", LANG_PREFIX + "help.effect.desc"), false);
        source.sendSuccess(() -> CommandUtils.createSuggestCommand(LANG_PREFIX + "help.syntax.toggle", "/kt font name toggle ", "cmd.textstudio.font.help.switch_hint"), false);
        source.sendSuccess(() -> CommandUtils.createSuggestCommand(LANG_PREFIX + "help.syntax.clear", "/kt font name clear", LANG_PREFIX + "help.clear.desc"), false);
        source.sendSuccess(() -> Component.translatable("cmd.textstudio.font.help.header"), false);
        source.sendSuccess(() -> CommandUtils.createSuggestCommand(LANG_PREFIX + "help.syntax.guide", "/kt font guide", "cmd.textstudio.font.help.dynamic.desc"), false);
        return 1;
    }

    private static int setName(CommandContext<CommandSourceStack> context, String rawName) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String finalName = normalizeName(rawName);
            if (!isValidName(finalName)) {
                context.getSource().sendFailure(Component.translatable(LANG_PREFIX + "set.fail.invalid_name"));
                return 0;
            }
            if (AuthorAPI.isReservedName(finalName) && !AuthorAPI.isAuthor(player)) {
                context.getSource().sendFailure(Component.translatable(LANG_PREFIX + "set.fail.reserved"));
                return 0;
            }
            if (AuthorAPI.isNameTaken(Objects.requireNonNull(player.getServer()), finalName, player.getUUID())) {
                context.getSource().sendFailure(Component.translatable(LANG_PREFIX + "set.fail.taken"));
                return 0;
            }
            if (player instanceof IAuthorName auth) {
                auth.textstudio_font$setCustomdiyname(finalName);
                context.getSource().sendSuccess(() -> Component.translatable(LANG_PREFIX + "set.success", Component.literal(finalName).withStyle(ChatFormatting.GOLD)), false);
                player.refreshDisplayName();
            }
        } catch (Exception ignored) {
            return 0;
        }
        return 1;
    }

    private static String normalizeName(String value) {
        return AuthorNamePolicy.normalizeRawName(value);
    }

    private static boolean isValidName(String value) {
        return AuthorNamePolicy.isValidName(value, MAX_NAME_CODEPOINTS);
    }

    private static int setEffect(CommandContext<CommandSourceStack> context, int id) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            boolean author = AuthorAPI.isAuthor(player);
            int max = AuthorConfig.EFFECTS.size();
            boolean special = id == AuthorAPI.SPECIAL_AUTHOR_EFFECT;
            if (special && !author) {
                context.getSource().sendFailure(Component.translatable(LANG_PREFIX + "effect.fail.author_only"));
                return 0;
            }
            if (!special && (id < 1 || id > max)) {
                context.getSource().sendFailure(Component.translatable("cmd.textstudio.font.effect.fail.invalid_id", Component.literal(String.valueOf(max)).withStyle(ChatFormatting.GOLD)));
                return 0;
            }
            if (player instanceof IAuthorName auth) {
                auth.textstudio_font$setNameEffect(id);
                if (!author && AuthorConfig.hasAdvancedFeatures(id)) {
                    context.getSource().sendSuccess(() -> Component.translatable(LANG_PREFIX + "effect.filtered", Component.literal(String.valueOf(id)).withStyle(ChatFormatting.AQUA)), false);
                } else {
                    context.getSource().sendSuccess(() -> Component.translatable(LANG_PREFIX + "effect.success", Component.literal(String.valueOf(id)).withStyle(ChatFormatting.AQUA)), false);
                }
                player.refreshDisplayName();
            }
        } catch (Exception ignored) {
            return 0;
        }
        return 1;
    }

    private static int toggleStyle(CommandSourceStack source, int flagBit, boolean enable, String nameKey, boolean authorOnly) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            if (authorOnly && !AuthorAPI.isAuthor(player)) {
                source.sendFailure(Component.translatable(LANG_PREFIX + "style.fail.author_only"));
                return 0;
            }
            if (player instanceof IAuthorName auth) {
                auth.toggleStyleFlag(flagBit, enable);
                Component styleName = Component.translatable(LANG_PREFIX + "style." + nameKey).withStyle(ChatFormatting.GOLD);
                Component state = Component.translatable(LANG_PREFIX + (enable ? "state.on" : "state.off"))
                        .withStyle(enable ? ChatFormatting.GREEN : ChatFormatting.RED);
                source.sendSuccess(() -> Component.translatable(LANG_PREFIX + "toggle.success", styleName, state), false);
                player.refreshDisplayName();
            }
        } catch (Exception ignored) {
            return 0;
        }
        return 1;
    }

    private static int clearName(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            if (player instanceof IAuthorName auth) {
                auth.textstudio_font$setCustomdiyname(null);
                context.getSource().sendSuccess(() -> Component.translatable(LANG_PREFIX + "clear.success"), false);
                player.refreshDisplayName();
            }
        } catch (Exception ignored) {
            return 0;
        }
        return 1;
    }
}
