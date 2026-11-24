package dev.xyat.textstudiovalidation;

import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.client.gui.KineticGui;
import dev.xyat.kineticcore.api.minecraft.MinecraftChat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Checks the real parser and editor entrances. Optional Delete checks restore the in-memory draft without saving. */
public final class GuiCaptureValidation {
    private static final Logger LOG=LoggerFactory.getLogger(GuiCaptureValidation.class);
    private static final String ROOT=System.getProperty("textstudio.guiValidation.output","D:/IDEAWork/TextStudio/.gradle/gui-capture/");
    private static final boolean FULL_HD_ONLY=Boolean.getBoolean("textstudio.guiValidation.fullHdOnly");
    private static final boolean PARSER_ONLY=Boolean.getBoolean("textstudio.guiValidation.parserOnly");
    private static final String[] NAMES={"font-editor-packet1","font-editor-legacy-packet0","chat-copy","font-editor-config"};
    private static final String[] PAGES={"FontModuleConfigPage","FontModuleConfigPage","ChatCopyCanvasPage","FontModuleConfigPage"};
    private static final StringBuilder DETAILS=new StringBuilder();
    private static final long OPEN_TIMEOUT=20000;
    private static boolean installed,started,screenshot,afterScreenshot,finished,originalFullscreen,settingsChanged;
    private static String originalLanguage;
    private static int originalScale,originalWidth,originalHeight,phase=-1,page=-1,captures,failures,parserChecks,parserFailures,tipChecks,tipFailures;
    private static long due,deadline;
    private static CompletableFuture<Void> reload;
    private static Language stressOriginal;
    private static Throwable openFailure;

    public static void install() {
        if(installed)return;installed=true;
        // Effect text on signs and in books runs on its own, in place of the editor captures.
        if(Boolean.getBoolean("textstudio.surfaceValidation")){SurfaceEffectValidation.install();return;}
        KineticClientEvents.onTick(KineticClientEvents.TickPhase.END,GuiCaptureValidation::tick);
    }
    private static void tick() {
        if(finished)return;
        try {
            var mc=Minecraft.getInstance();
            if(!started) {
                if(mc.player==null || mc.level==null || mc.getSingleplayerServer()==null)return;
                started=true;originalLanguage=mc.getLanguageManager().getSelected();originalScale=mc.options.guiScale().get();
                originalWidth=mc.getWindow().getWidth();originalHeight=mc.getWindow().getHeight();originalFullscreen=mc.getWindow().isFullscreen();
                Files.createDirectories(Path.of(ROOT));
                Files.deleteIfExists(Path.of(ROOT,"result.txt"));
                verifyFontCommands();
                writeProgress("Parser runtime checks starting");
                FontParserRuntimeChecks.Result parserResult=FontParserRuntimeChecks.run();
                parserChecks=parserResult.checks();parserFailures=parserResult.failures();failures+=parserFailures;
                DETAILS.append(parserResult.details());
                writeProgress("Parser checks="+parserChecks+" failures="+parserFailures);
                FontParserRuntimeChecks.Result tipsResult=FontParserRuntimeChecks.runOptionalTipLimits();
                tipChecks=tipsResult.checks();tipFailures=tipsResult.failures();failures+=tipFailures;
                DETAILS.append(tipsResult.details());
                if(PARSER_ONLY) {
                    finish();return;
                }
                settingsChanged=true;
                mc.options.guiScale().set(0);
                if(originalFullscreen)mc.getWindow().toggleFullScreen();
                // The chat copy page lists the chat history, so give it lines of different styles and lengths.
                MinecraftChat.addMessage(Component.literal("TextStudio capture: plain line"));
                MinecraftChat.addMessage(Component.literal("Colored ").withStyle(ChatFormatting.GOLD).append(Component.literal("and bold").withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA)));
                MinecraftChat.addMessage(Component.literal("A long chat line that wraps across the chat width to check that copied lines keep their place in the list and never run into the next row of text"));
                MinecraftChat.addMessage(Component.literal("中文聊天行，用于检查中文文本在复制界面中的显示"));
                nextPhase();
                return;
            }
            if(reload!=null) {
                if(!reload.isDone() || mc.getOverlay()!=null)return;
                reload.join();reload=null;
                if(phase==4) {
                    stressOriginal=Language.getInstance();
                    Language.inject(new StressLanguage(stressOriginal));
                }
                nextPage();return;
            }
            if(openFailure!=null) {
                Throwable error=openFailure;openFailure=null;
                throw new IllegalStateException("Editor open-screen packet failed",error);
            }
            long now=System.currentTimeMillis();
            if(!screenshot) {
                String current=currentPage();
                if(!PAGES[page].equals(current)) {
                    if(now>=deadline)throw new IllegalStateException("page "+PAGES[page]+" did not open; showing "+current);
                    due=now+1000;return;
                }
                if(now>=due) {
                    verifyCanvasAndDelete();
                    capture("start");screenshot=true;due=now+550;
                }
                return;
            }
            if(now>=due) {
                if(!afterScreenshot) {
                    capture("after");afterScreenshot=true;
                    if(phase==4){due=now+2850;return;}
                }
                if(phase==4)capture("scroll");
                nextPage();
            }
        } catch(Throwable error) {
            recordFailure("TEXTSTUDIO_GUI_FAIL phase="+phase+" page="+page,error);
            if(phase<0){finish();return;}
            try { nextPage(); } catch(Throwable next) { recordFailure("TEXTSTUDIO_GUI_NEXT_PAGE_FAIL",next);finish(); }
        }
    }
    private static String currentPage() {
        var current=KineticGui.currentPage();
        return current==null?String.valueOf(Minecraft.getInstance().screen):current.getClass().getSimpleName();
    }
    private static void verifyFontCommands() throws Exception {
        var dispatcher=new com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack>();
        var root=net.minecraft.commands.Commands.literal("kt");
        dev.xyat.textstudio.font.FontCommand.register(root);dispatcher.register(root);
        var source=Minecraft.getInstance().getSingleplayerServer().createCommandSourceStack().withSuppressedOutput();
        for(String command:List.of("kt font","kt font help","kt font name help")) {
            if(dispatcher.execute(command,source)!=1)throw new AssertionError("Font help must work without opening a player editor: "+command);
        }
        for(String command:List.of("kt font guide","kt font editor")) {
            var result=dispatcher.parse(command,source);
            if(!result.getReader().canRead())throw new AssertionError("Editor command entry remains registered: "+command);
        }
        for(String command:List.of("kt font rename Example","kt font name set Example","kt font name effect 1",
                "kt font name toggle rainbow true","kt font name toggle bold false","kt font name toggle strike false","kt font name clear")) {
            var result=dispatcher.parse(command,source);
            if(result.getReader().canRead() || !result.getExceptions().isEmpty() || result.getContext().getCommand()==null)
                throw new AssertionError("Name command no longer resolves: "+command);
        }
        DETAILS.append("FONT_COMMANDS_PASS help works without a player; editor aliases absent; rename/effect/toggles/clear resolve\n");
    }
    private static void nextPhase() {
        if(stressOriginal!=null){Language.inject(stressOriginal);stressOriginal=null;}
        phase++;page=-1;
        // One English 1080p round by default; list phases (0-4) in textstudio.guiValidation.phases to run more.
        String selectedPhases=System.getProperty("textstudio.guiValidation.phases","1");
        while(phase<5 && !Boolean.getBoolean("textstudio.guiValidation.fullHdOnly") && !List.of(selectedPhases.split(",")).contains(String.valueOf(phase)))phase++;
        // Opt-in 1080p pass: English and Chinese; keep the usual phases by default.
        if(phase>=(FULL_HD_ONLY?2:5)){finish();return;}
        var mc=Minecraft.getInstance();
        mc.setScreen(null);
        String lang=(FULL_HD_ONLY?phase==1:phase==2 || phase==3)?"zh_cn":"en_us";
        mc.getLanguageManager().setSelected(lang);
        mc.options.languageCode=lang;
        boolean fullHd=FULL_HD_ONLY || phase==1 || phase==3;
        int width=fullHd?1920:854,height=fullHd?1080:480;
        mc.getWindow().setWindowed(width,height);mc.resizeDisplay();
        reload=mc.reloadResourcePacks();
        writeProgress("Phase="+phase+" language="+lang+" requested="+width+"x"+height);
        LOG.info("TEXTSTUDIO_GUI_PHASE phase={} language={} requested={}x{} autoScale=true",phase,lang,width,height);
    }
    private static void nextPage() throws Exception {
        page++;
        String selectedPages=System.getProperty("textstudio.guiValidation.pages", "");
        while(page<NAMES.length && !selectedPages.isBlank() && !List.of(selectedPages.split(",")).contains(String.valueOf(page)))page++;
        if(page>=NAMES.length){nextPhase();return;}
        screenshot=false;afterScreenshot=false;
        long now=System.currentTimeMillis();due=now+1000;deadline=now+OPEN_TIMEOUT;
        Minecraft.getInstance().setScreen(null);
        switch(page) {
            case 0 -> openFontEditorPacket(1);
            case 1 -> openFontEditorPacket(0);
            case 2 -> KineticGui.openChild(new dev.xyat.textstudio.chat.client.ChatCopyCanvasPage(MinecraftChat.activeTrimmedMessages()));
            default -> openFontEditorConfig();
        }
        writeProgress("Open phase="+phase+" case="+NAMES[page]+" expected="+PAGES[page]);
        LOG.info("TEXTSTUDIO_GUI_OPEN phase={} case={}",phase,NAMES[page]);
    }
    private static void openFontEditorConfig() {
        // F6 opens the Core config index; TextStudio's registered action must work without a font command.
        dev.xyat.kineticcore.api.config.client.KTConfigApi.openIndex();
        String pageId=dev.xyat.textstudio.font.client.FontModuleClient.CONFIG_PAGE_ID;
        var config=dev.xyat.kineticcore.api.config.client.KTConfigApi.find(pageId).orElseThrow();
        dev.xyat.kineticcore.api.config.client.KTConfigApi.openPage(pageId);
        config.entries().stream().filter(entry->entry.id().equals("open_visual_editor")).findFirst().orElseThrow().runAction();
        DETAILS.append("CONFIG_ENTRY_OPEN registered TextStudio action opened from the F6 config index\n");
    }
    private static void openFontEditorPacket(int screen) {
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();
        if(server==null || mc.player==null)throw new IllegalStateException("Editor packet validation requires a loaded singleplayer world");
        var uuid=mc.player.getUUID();
        server.execute(() -> {
            try {
                var player=server.getPlayerList().getPlayer(uuid);
                if(player==null)throw new IllegalStateException("Server player was not found for editor packet validation: "+uuid);
                dev.xyat.textstudio.font.network.AuthorNetwork.sendToPlayer(new dev.xyat.textstudio.font.network.AuthorNetwork.OpenScreen(screen),player);
            } catch(Throwable error) {mc.execute(() -> openFailure=error);}
        });
    }
    private static void capture(String frame)throws Exception {
        var mc=Minecraft.getInstance();Path path=Path.of(ROOT,String.format("%d-%02d-%s-%s.png",phase,page,NAMES[page],frame));Files.createDirectories(path.getParent());
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(path);}
        captures++;
        DETAILS.append("CAPTURE ").append(path.toAbsolutePath()).append(" image=").append(mc.getWindow().getWidth()).append('x').append(mc.getWindow().getHeight()).append('\n');
        writeProgress("Captured phase="+phase+" case="+NAMES[page]+" frame="+frame+" count="+captures);
        LOG.info("TEXTSTUDIO_GUI_CAPTURE phase={} case={} image={}x{}",phase,NAMES[page],mc.getWindow().getWidth(),mc.getWindow().getHeight());
    }
    @SuppressWarnings("unchecked")
    private static void verifyCanvasAndDelete() throws Exception {
        var current=KineticGui.currentPage();
        if(current.width()!=640 || current.height()!=360)throw new IllegalStateException("Unexpected editor canvas: "+current.width()+"x"+current.height());
        if(current instanceof dev.xyat.textstudio.chat.client.ChatCopyCanvasPage) verifyChatClip(current);
        if(!Boolean.getBoolean("textstudio.guiValidation.deleteCheck")
                || !(current instanceof dev.xyat.textstudio.font.client.FontModuleConfigPage))return;
        var effectsField=current.getClass().getDeclaredField("draftEffects");effectsField.setAccessible(true);
        var selectedField=current.getClass().getDeclaredField("selectedPreset");selectedField.setAccessible(true);
        var effects=(List<dev.xyat.textstudio.font.config.AuthorConfig.EffectSettings>)effectsField.get(current);
        var snapshot=new ArrayList<>(effects);int selected=selectedField.getInt(current);
        var clearFocus=dev.xyat.kineticcore.api.client.gui.page.KineticPage.class.getDeclaredMethod("clearFocus");clearFocus.setAccessible(true);
        int persistedCount=dev.xyat.textstudio.font.config.AuthorConfig.EFFECTS.size();
        try {
            var first=snapshot.get(0).copy();var second=snapshot.get(0).copy();
            effects.clear();effects.add(first);effects.add(second);selectedField.setInt(current,0);current.rebuild();clearFocus.invoke(current);
            if(!dev.xyat.kineticcore.internal.client.gui.GuiInputCompat.keyPressed(Minecraft.getInstance().screen,261,0,0)
                    || effects.size()!=1 || effects.get(0)!=second)throw new IllegalStateException("Delete did not remove only the selected preset");
            clearFocus.invoke(current);
            dev.xyat.kineticcore.internal.client.gui.GuiInputCompat.keyPressed(Minecraft.getInstance().screen,261,0,0);
            if(effects.size()!=1 || effects.get(0)!=second)throw new IllegalStateException("Delete removed the last preset");
            if(dev.xyat.textstudio.font.config.AuthorConfig.EFFECTS.size()!=persistedCount)throw new IllegalStateException("Draft Delete changed saved presets");
            DETAILS.append("DELETE_PASS selected preset removed; final preset retained; saved presets unchanged\n");
        } finally {
            effects.clear();effects.addAll(snapshot);selectedField.setInt(current,selected);current.rebuild();
        }
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void verifyChatClip(dev.xyat.kineticcore.api.client.gui.page.KineticPage current) throws Exception {
        Class<?> type=current.getClass();
        var linesField=type.getDeclaredField("lines");linesField.setAccessible(true);
        var lines=(List)linesField.get(current);var original=new ArrayList(lines);
        if(lines.isEmpty())throw new AssertionError("Chat clip regression requires a history line");
        var scrollField=type.getDeclaredField("scroll");scrollField.setAccessible(true);
        var scroll=(dev.xyat.kineticcore.api.client.gui.scroll.KineticScrollController)scrollField.get(current);
        int originalOffset=scroll.offset();
        var update=type.getDeclaredMethod("updateScroll");update.setAccessible(true);
        var hit=type.getDeclaredMethod("getLineIndexAt",double.class,double.class);hit.setAccessible(true);
        var click=type.getDeclaredMethod("onMouseClick",dev.xyat.kineticcore.api.client.gui.input.MouseInput.class);click.setAccessible(true);
        try {
            while(lines.size()<80)lines.add(original.get(0));
            update.invoke(current);scroll.setOffset(0);
            for(double[] point:new double[][]{{5,30},{626,30},{10,26},{10,337}})
                if((int)hit.invoke(current,point[0],point[1])!=-1)throw new AssertionError("Clipped chat padding accepted a text click: "+Arrays.toString(point));
            if((int)hit.invoke(current,6D,27D)!=0 || (int)hit.invoke(current,625D,336D)!=30)
                throw new AssertionError("Visible chat boundary pixels missed their rows");
            for(var button:List.of(dev.xyat.kineticcore.api.client.gui.input.MouseButton.RIGHT,dev.xyat.kineticcore.api.client.gui.input.MouseButton.MIDDLE)) {
                if((boolean)click.invoke(current,new dev.xyat.kineticcore.api.client.gui.input.MouseInput(10,30,button,button==dev.xyat.kineticcore.api.client.gui.input.MouseButton.RIGHT?1:2,0)))
                    throw new AssertionError("Non-left click started chat text selection: "+button);
            }
            DETAILS.append("CHAT_CLIP_PASS padding rejected; first/last visible pixels selectable; non-left clicks do not select\n");
        } finally {
            lines.clear();lines.addAll(original);update.invoke(current);scroll.setOffset(originalOffset);
        }
    }
    private static void finish() {
        if(finished)return;
        finished=true;
        var mc=Minecraft.getInstance();
        try {
            if(stressOriginal!=null){Language.inject(stressOriginal);stressOriginal=null;}
            if(settingsChanged) {
                mc.options.guiScale().set(originalScale);
                mc.getLanguageManager().setSelected(originalLanguage);mc.options.languageCode=originalLanguage;
                mc.setScreen(null);
                mc.getWindow().setWindowed(originalWidth,originalHeight);
                if(originalFullscreen && !mc.getWindow().isFullscreen())mc.getWindow().toggleFullScreen();
            }
        } catch(Throwable error) {recordFailure("TEXTSTUDIO_GUI_RESTORE_FAIL",error);}
        String summary="TEXTSTUDIO_GUI_"+(failures==0?"PASS":"FAIL")+" parserOnly="+PARSER_ONLY+" parserChecks="+parserChecks+" parserFailures="+parserFailures+" tipChecks="+tipChecks+" tipFailures="+tipFailures+" captures="+captures+" failures="+failures;
        try {
            Files.createDirectories(Path.of(ROOT));
            Files.writeString(Path.of(ROOT,"result.txt"),summary+"\n"+DETAILS);
        } catch(Throwable error) {LOG.error("TEXTSTUDIO_GUI_RESULT_WRITE_FAIL",error);}
        writeProgress(summary);
        LOG.info(summary);
        dev.xyat.kineticcore.api.runtime.KineticClientRuntime.stopClient();
    }
    private static void writeProgress(String message) {
        try {Files.writeString(Path.of(ROOT,"progress.txt"),message+"\n");}
        catch(Throwable error) {LOG.error("TEXTSTUDIO_GUI_PROGRESS_WRITE_FAIL",error);}
    }
    private static void recordFailure(String context,Throwable error) {
        failures++;
        StringWriter trace=new StringWriter();error.printStackTrace(new PrintWriter(trace));
        DETAILS.append(context).append('\n').append(trace);
        writeProgress(context+" failures="+failures);
        LOG.error(context,error);
    }
    private static final class StressLanguage extends Language {
        private final Language delegate;
        StressLanguage(Language delegate){this.delegate=delegate;}
        @Override public String getOrDefault(String key,String fallback) {
            String text=delegate.getOrDefault(key,fallback);
            return key.startsWith("gui.textstudio.") || key.startsWith("cfg.textstudio.")?text+" - deliberately extended translation to verify text stays inside its own region":text;
        }
        @Override public boolean has(String key){return delegate.has(key);}
        @Override public boolean isDefaultRightToLeft(){return delegate.isDefaultRightToLeft();}
        @Override public net.minecraft.util.FormattedCharSequence getVisualOrder(net.minecraft.network.chat.FormattedText text){return delegate.getVisualOrder(text);}
    }
}
