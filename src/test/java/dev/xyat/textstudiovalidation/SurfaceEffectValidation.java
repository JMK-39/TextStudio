package dev.xyat.textstudiovalidation;

import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.textstudio.font.client.parser.CompactTagCodec;
import dev.xyat.textstudio.font.client.render.CompatibleFontRenderer;
import dev.xyat.textstudio.font.common.text.InputMetrics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Effect text where players put it: on signs (written by the server, and pasted into the sign editor as players do,
 * plain and glowing) and in books (being written, and signed). A book page wraps into several lines and every line
 * must still carry the effect; text too long for a sign's line is pasted onto the lines below, with the effect, and
 * what does not fit the sign is dropped. Saves screenshots of the signs, the book being written and the signed book,
 * and of every section of the effect editor's settings list in Chinese and English, then closes the game. Runs instead of the GUI capture with
 * -Dtextstudio.surfaceValidation=true.
 */
public final class SurfaceEffectValidation {
    private static final Logger LOG = LoggerFactory.getLogger(SurfaceEffectValidation.class);
    private static final String ROOT = System.getProperty("textstudio.guiValidation.output", "D:/IDEAWork/TextStudio/.gradle/surface/");
    /** Preset 1 is the plain rainbow. */
    private static final String PREFIX = CompactTagCodec.encodePresetPrefix(1);
    private static final String PAGE = PREFIX + "文字特效动态预览 123 ABC 大漠孤烟直长河落日圆欲穷千里目更上一层楼 白日依山尽黄河入海流";
    private static final String PASTE = PREFIX + "PASTE 粘贴的长文字会自动换到下面的空行，四行都放不下的部分不再粘贴，告示牌上只留下放得下的文字。多余的文字到这里为止，这一句应该完全看不到才对。";
    private static final StringBuilder DETAILS = new StringBuilder();
    private static final String[] LANGUAGES = {"zh_cn", "en_us"};
    private static boolean installed, finished;
    private static int step, checks, failures, captures, language, tab;
    private static String originalLanguage;
    private static java.util.concurrent.CompletableFuture<Void> reload;
    private static long due;
    private static BlockPos plain, glowing, pasted;

    private SurfaceEffectValidation() {
    }

    public static void install() {
        if (installed) return;
        installed = true;
        KineticClientEvents.onTick(KineticClientEvents.TickPhase.END, SurfaceEffectValidation::tick);
    }

    private static void tick() {
        if (finished) return;
        Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        try {
            switch (step) {
                case 0 -> {
                    if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
                    Files.createDirectories(Path.of(ROOT));
                    onServer(SurfaceEffectValidation::placeSigns);
                    due = now + 3000L;
                    step = 1;
                }
                case 1 -> {
                    if (now < due) return;
                    // The way a player writes a sign: the server opens its editor, the text is pasted in, then it is closed.
                    onServer(() -> {
                        MinecraftServer server = mc.getSingleplayerServer();
                        SignBlockEntity sign = (SignBlockEntity) server.overworld().getBlockEntity(pasted);
                        // As a player's click on the sign does: it may edit this sign, then the editor opens.
                        ((net.minecraft.world.level.block.SignBlock) sign.getBlockState().getBlock())
                                .openTextEdit(server.getPlayerList().getPlayers().get(0), sign, true);
                        return null;
                    });
                    due = now + 1000L;
                    step = 2;
                }
                case 2 -> {
                    if (now < due) return;
                    // Too long for one line: the editor wraps it onto the lines below and drops what is left.
                    Method paste = Arrays.stream(net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen.class.getDeclaredMethods())
                            .filter(method -> method.getName().endsWith("textstudio_font$paste")).findFirst().orElseThrow();
                    paste.setAccessible(true);
                    paste.invoke(mc.screen, PASTE);
                    mc.screen.onClose();
                    due = now + 2500L;
                    step = 3;
                }
                case 3 -> {
                    if (now < due) return;
                    String[] storedLines = onServer(() -> {
                        SignText text = ((SignBlockEntity) mc.getSingleplayerServer().overworld().getBlockEntity(pasted)).getFrontText();
                        return new String[]{text.getMessage(0, false).getString(), text.getMessage(1, false).getString(),
                                text.getMessage(2, false).getString(), text.getMessage(3, false).getString()};
                    });
                    int maxWidth = ((SignBlockEntity) mc.level.getBlockEntity(pasted)).getMaxTextLineWidth();
                    for (int i = 0; i < storedLines.length; i++) {
                        String plainLine = InputMetrics.stripControlCodes(storedLines[i]);
                        check(storedLines[i].startsWith(PREFIX) && !plainLine.isEmpty() && mc.font.width(plainLine) <= maxWidth,
                                "pasted sign line " + (i + 1) + " is filled, fits and keeps the effect: " + visible(storedLines[i]));
                    }
                    check(String.join("", storedLines).replace(PREFIX, "").length() < PASTE.replace(PREFIX, "").length(),
                            "text the sign has no room for is dropped");
                    for (BlockPos pos : List.of(plain, glowing, pasted)) {
                        SignBlockEntity sign = (SignBlockEntity) mc.level.getBlockEntity(pos);
                        Font font = mc.font;
                        FormattedCharSequence[] lines = sign.getFrontText().getRenderMessages(false, component -> {
                            List<FormattedCharSequence> split = font.split(component, sign.getMaxTextLineWidth());
                            return split.isEmpty() ? FormattedCharSequence.EMPTY : split.get(0);
                        });
                        check(effect(font, lines[0]), "sign at " + pos.toShortString() + " line 1 carries the effect");
                        if (pos.equals(pasted)) check(effect(font, lines[3]), "pasted sign line 4 carries the effect");
                    }
                    capture("signs");
                    openBookEditor(mc);
                    due = now + 1500L;
                    step = 4;
                }
                case 4 -> {
                    if (now < due) return;
                    checkBookEditor(mc);
                    capture("book-edit");
                    openBookView(mc);
                    due = now + 1500L;
                    step = 5;
                }
                case 5 -> {
                    if (now < due) return;
                    List<FormattedCharSequence> lines = mc.font.split(Component.literal(PAGE), 114);
                    check(lines.size() >= 3, "the signed page wraps into " + lines.size() + " lines");
                    for (int i = 0; i < lines.size(); i++) check(effect(mc.font, lines.get(i)), "signed book line " + (i + 1) + " carries the effect");
                    capture("book-view");
                    mc.setScreen(null);
                    // The effect editor's tabs, in Chinese and then English.
                    originalLanguage = mc.getLanguageManager().getSelected();
                    language = 0;
                    reload = setLanguage(mc, LANGUAGES[0]);
                    step = 6;
                }
                case 6 -> {
                    if (!reload.isDone() || mc.getOverlay() != null) return;
                    openEditor();
                    tab = 0;
                    due = now + 2000L;
                    step = 7;
                }
                case 7 -> {
                    if (now < due) return;
                    var editor = dev.xyat.kineticcore.api.client.gui.KineticGui.currentPage();
                    Class<?> tabs = Class.forName(dev.xyat.textstudio.font.client.FontModuleConfigPage.class.getName() + "$Section");
                    Object[] values = tabs.getEnumConstants();
                    if (tab > 0) capture("editor-" + LANGUAGES[language] + "-" + values[tab - 1].toString().toLowerCase(java.util.Locale.ROOT));
                    if (tab < values.length) {
                        var select = editor.getClass().getDeclaredMethod("revealSection", tabs);
                        select.setAccessible(true);
                        select.invoke(editor, values[tab]);
                        tab++;
                        due = now + 800L;
                        return;
                    }
                    mc.setScreen(null);
                    if (++language < LANGUAGES.length) {
                        reload = setLanguage(mc, LANGUAGES[language]);
                        step = 6;
                        return;
                    }
                    reload = setLanguage(mc, originalLanguage);
                    step = 8;
                }
                case 8 -> {
                    if (!reload.isDone() || mc.getOverlay() != null) return;
                    finish();
                }
                default -> {
                }
            }
        } catch (Throwable error) {
            failures++;
            LOG.error("TEXTSTUDIO_SURFACE_ERROR step={}", step, error);
            DETAILS.append("ERROR step ").append(step).append(": ").append(error).append('\n');
            finish();
        }
    }

    private static java.util.concurrent.CompletableFuture<Void> setLanguage(Minecraft mc, String code) {
        mc.getLanguageManager().setSelected(code);
        mc.options.languageCode = code;
        return mc.reloadResourcePacks();
    }

    /** As F6 does: the config index, TextStudio's page, then its action that opens the effect editor. */
    private static void openEditor() {
        dev.xyat.kineticcore.api.config.client.KTConfigApi.openIndex();
        String pageId = dev.xyat.textstudio.font.client.FontModuleClient.CONFIG_PAGE_ID;
        var config = dev.xyat.kineticcore.api.config.client.KTConfigApi.find(pageId).orElseThrow();
        dev.xyat.kineticcore.api.config.client.KTConfigApi.openPage(pageId);
        config.entries().stream().filter(entry -> entry.id().equals("open_visual_editor")).findFirst().orElseThrow().runAction();
    }

    /** A plain and a glowing sign written by the server, and an empty one for the editor, in a row in front of the player. */
    private static Void placeSigns() {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        ServerLevel level = server.overworld();
        var player = server.getPlayerList().getPlayers().get(0);
        Direction facing = player.getDirection();
        BlockPos base = player.blockPosition().relative(facing, 3);
        plain = base.relative(facing.getCounterClockWise());
        glowing = base;
        pasted = base.relative(facing.getClockWise());
        int rotation = net.minecraft.world.level.block.state.properties.RotationSegment.convertToSegment(facing.getOpposite());
        for (BlockPos pos : List.of(plain, glowing, pasted)) {
            level.setBlock(pos, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rotation), 3);
        }
        setSign(level, plain, false);
        setSign(level, glowing, true);
        float yaw = facing.toYRot();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
                "tp " + player.getScoreboardName() + " " + player.getX() + " " + player.getY() + " " + player.getZ() + " " + yaw + " 20");
        return null;
    }

    private static void setSign(ServerLevel level, BlockPos pos, boolean glow) {
        SignBlockEntity sign = (SignBlockEntity) level.getBlockEntity(pos);
        SignText text = new SignText().setMessage(0, Component.literal(PREFIX + (glow ? "GLOW 发光" : "SIGN 告示牌")))
                .setMessage(1, Component.literal(PREFIX + "彩色文字")).setHasGlowingText(glow);
        sign.setText(text, true);
        sign.setChanged();
        level.sendBlockUpdated(pos, sign.getBlockState(), sign.getBlockState(), 3);
    }

    private static void openBookEditor(Minecraft mc) {
        var stack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);
        //? if >=1.21 {
        /*var content = new net.minecraft.world.item.component.WritableBookContent(List.of(net.minecraft.server.network.Filterable.passThrough(PAGE)));
        stack.set(net.minecraft.core.component.DataComponents.WRITABLE_BOOK_CONTENT, content);
        *///?} else {
        var pages = new net.minecraft.nbt.ListTag();
        pages.add(net.minecraft.nbt.StringTag.valueOf(PAGE));
        stack.getOrCreateTag().put("pages", pages);
        //?}
        //? if >=26.1 {
        /*mc.setScreen(new net.minecraft.client.gui.screens.inventory.BookEditScreen(mc.player, stack, net.minecraft.world.InteractionHand.MAIN_HAND, content));
        *///?} else {
        mc.setScreen(new net.minecraft.client.gui.screens.inventory.BookEditScreen(mc.player, stack, net.minecraft.world.InteractionHand.MAIN_HAND));
        //?}
    }

    /** Every line the book editor draws carries the effect, not only the first, which holds the effect code. */
    private static void checkBookEditor(Minecraft mc) throws Exception {
        List<FormattedCharSequence> lines = new ArrayList<>();
        //? if >=26.1 {
        /*var box = fieldOfType(mc.screen, net.minecraft.client.gui.components.MultiLineEditBox.class);
        var text = fieldOfType(box, net.minecraft.client.gui.components.MultilineTextField.class);
        String value = text.value();
        // The line views are a protected record: read them reflectively.
        var iterate = text.getClass().getMethod("iterateLines");
        for (Object line : (Iterable<?>) iterate.invoke(text)) {
            var begin = line.getClass().getDeclaredMethod("beginIndex");
            var end = line.getClass().getDeclaredMethod("endIndex");
            begin.setAccessible(true);
            end.setAccessible(true);
            lines.add(Component.literal(dev.xyat.textstudio.font.client.parser.EffectSpans.lineText(value, (int) begin.invoke(line), (int) end.invoke(line))).getVisualOrderText());
        }
        *///?} else {
        Object cache = null;
        for (Field field : mc.screen.getClass().getDeclaredFields()) {
            if (field.getType().getSimpleName().endsWith("DisplayCache")) {
                field.setAccessible(true);
                cache = field.get(mc.screen);
            }
        }
        for (Field field : cache.getClass().getDeclaredFields()) {
            if (field.getType().isArray() && field.getType().getComponentType().getSimpleName().endsWith("LineInfo")) {
                field.setAccessible(true);
                for (Object line : (Object[]) field.get(cache)) {
                    lines.add(fieldOfType(line, Component.class).getVisualOrderText());
                }
            }
        }
        //?}
        check(lines.size() >= 3, "the page being written wraps into " + lines.size() + " lines");
        for (int i = 0; i < lines.size(); i++) check(effect(mc.font, lines.get(i)), "book editor line " + (i + 1) + " carries the effect");
    }

    private static void openBookView(Minecraft mc) {
        //? if >=1.21 {
        /*mc.setScreen(new net.minecraft.client.gui.screens.inventory.BookViewScreen(
                new net.minecraft.client.gui.screens.inventory.BookViewScreen.BookAccess(List.of(Component.literal(PAGE)))));
        *///?} else {
        var stack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITTEN_BOOK);
        var pages = new net.minecraft.nbt.ListTag();
        pages.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal(PAGE))));
        stack.getOrCreateTag().put("pages", pages);
        stack.getOrCreateTag().putString("title", "TextStudio");
        stack.getOrCreateTag().putString("author", "TextStudio");
        mc.setScreen(new net.minecraft.client.gui.screens.inventory.BookViewScreen(
                new net.minecraft.client.gui.screens.inventory.BookViewScreen.WrittenBookAccess(stack)));
        //?}
    }

    /** Whether TextStudio draws this line with an effect: it keeps effect codes, or styles that carry one. */
    private static boolean effect(Font font, FormattedCharSequence line) {
        return CompatibleFontRenderer.tryMeasureSequence(font, line) != null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T fieldOfType(Object owner, Class<T> type) throws Exception {
        for (Class<?> c = owner.getClass(); c != null; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (type.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object value = field.get(owner);
                    if (value != null) return (T) value;
                }
            }
        }
        throw new IllegalStateException("no " + type.getSimpleName() + " in " + owner.getClass().getName());
    }

    private static String visible(String text) {
        StringBuilder out = new StringBuilder();
        text.codePoints().forEach(c -> out.append(c >= 0x2061 && c <= 0x2064 || c >= 0xFE00 && c <= 0xFE0F ? "·" : new String(Character.toChars(c))));
        return out.toString();
    }

    private static void check(boolean condition, String what) {
        checks++;
        if (!condition) failures++;
        DETAILS.append(condition ? "PASS " : "FAIL ").append(what).append('\n');
        LOG.info("TEXTSTUDIO_SURFACE_{} {}", condition ? "OK" : "MISS", what);
    }

    private static void capture(String name) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        Path path = Path.of(ROOT, String.format("surface-%02d-%s.png", captures++, name));
        // Written in the form stonecutter.gradle rewrites for 26.1, which hands the image to a callback.
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(path);}
    }

    private static <T> T onServer(Callable<T> task) throws Exception {
        return Minecraft.getInstance().getSingleplayerServer().submit(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).get();
    }

    private static void finish() {
        if (finished) return;
        finished = true;
        Minecraft mc = Minecraft.getInstance();
        if (originalLanguage != null && !originalLanguage.equals(mc.getLanguageManager().getSelected())) {
            mc.getLanguageManager().setSelected(originalLanguage);
            mc.options.languageCode = originalLanguage;
        }
        try {
            // The signs go again; the world keeps nothing of the run.
            onServer(() -> {
                ServerLevel level = Minecraft.getInstance().getSingleplayerServer().overworld();
                for (BlockPos pos : new BlockPos[]{plain, glowing, pasted}) if (pos != null) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                return null;
            });
        } catch (Exception e) {
            LOG.error("TEXTSTUDIO_SURFACE_CLEANUP_FAIL", e);
        }
        String summary = "TEXTSTUDIO_SURFACE_" + (failures == 0 ? "PASS" : "FAIL") + " checks=" + checks + " failures=" + failures + " captures=" + captures;
        try {
            Files.writeString(Path.of(ROOT, "result.txt"), summary + "\n" + DETAILS);
        } catch (Exception e) {
            LOG.error("TEXTSTUDIO_SURFACE_RESULT_FAIL", e);
        }
        LOG.info(summary);
        dev.xyat.kineticcore.api.runtime.KineticClientRuntime.stopClient();
    }
}
