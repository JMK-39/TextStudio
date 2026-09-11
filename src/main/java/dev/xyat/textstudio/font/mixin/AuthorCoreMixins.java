package dev.xyat.textstudio.font.mixin;

import com.mojang.authlib.GameProfile;
import dev.xyat.textstudio.font.api.AuthorAPI;
import dev.xyat.textstudio.font.api.IAuthorName;
import dev.xyat.textstudio.font.network.AuthorNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class AuthorCoreMixins implements IAuthorName {
    @Shadow public abstract GameProfile getGameProfile();

    @Unique private String textstudio_font$diyname = null;
    @Unique private int textstudio_font$nameEffect = 0;
    @Unique private int textstudio_font$styleFlags = -1;

    @Override
    public String textstudio_font$getCustomdiyname() {
        return textstudio_font$diyname != null ? textstudio_font$diyname : AuthorAPI.getDefaultCustomName(textstudio_font$asPlayer().getUUID());
    }

    @Override
    public void textstudio_font$setCustomdiyname(String name) {
        this.textstudio_font$diyname = name;
        textstudio_font$syncToClients();
    }

    @Override
    public int textstudio_font$getNameEffect() {
        int effect = textstudio_font$nameEffect > 0 ? textstudio_font$nameEffect : AuthorAPI.getDefaultNameEffect(textstudio_font$asPlayer().getUUID());
        if (!AuthorAPI.isAuthor(textstudio_font$asPlayer()) && effect == AuthorAPI.SPECIAL_AUTHOR_EFFECT) return 1;
        return effect;
    }

    @Override
    public void textstudio_font$setNameEffect(int effectId) {
        this.textstudio_font$nameEffect = !AuthorAPI.isAuthor(textstudio_font$asPlayer()) && effectId == AuthorAPI.SPECIAL_AUTHOR_EFFECT ? 1 : effectId;
        textstudio_font$syncToClients();
    }

    @Override
    public int textstudio_font$getStyleFlags() {
        int flags = textstudio_font$styleFlags >= 0 ? textstudio_font$styleFlags : AuthorAPI.getDefaultStyleFlags(textstudio_font$asPlayer().getUUID());
        return AuthorAPI.sanitizeStyleFlags(textstudio_font$asPlayer().getUUID(), flags);
    }

    @Override
    public void textstudio_font$setStyleFlags(int flags) {
        this.textstudio_font$styleFlags = AuthorAPI.sanitizeStyleFlags(textstudio_font$asPlayer().getUUID(), flags);
        textstudio_font$syncToClients();
    }

    @Override
    public void textstudio_font$setState(String name, int effectId, int flags) {
        this.textstudio_font$diyname = name;
        this.textstudio_font$nameEffect = !AuthorAPI.isAuthor(textstudio_font$asPlayer()) && effectId == AuthorAPI.SPECIAL_AUTHOR_EFFECT ? 1 : effectId;
        this.textstudio_font$styleFlags = AuthorAPI.sanitizeStyleFlags(textstudio_font$asPlayer().getUUID(), flags);
        textstudio_font$syncToClients();
    }

    @Unique
    private void textstudio_font$syncToClients() {
        Player player = textstudio_font$asPlayer();
        if (!player.level().isClientSide()) {
            AuthorNetwork.sendToAll(new AuthorNetwork.SyncName(player.getUUID(), AuthorAPI.getCustomName(player), textstudio_font$getNameEffect(), textstudio_font$getStyleFlags()));
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void textstudio_font$saveNameData(CompoundTag tag, CallbackInfo ci) {
        CompoundTag ktTag = new CompoundTag();
        ktTag.putInt("e", textstudio_font$getNameEffect());
        ktTag.putInt("f", textstudio_font$getStyleFlags());
        if (textstudio_font$diyname != null) ktTag.putString("n", textstudio_font$diyname);
        ktTag.putUUID("o", textstudio_font$asPlayer().getUUID());
        tag.put("kf", ktTag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void textstudio_font$loadNameData(CompoundTag tag, CallbackInfo ci) {
        if (!tag.contains("kf", 10)) return;
        CompoundTag ktTag = tag.getCompound("kf");
        Player player = textstudio_font$asPlayer();
        if (ktTag.hasUUID("o") && !ktTag.getUUID("o").equals(player.getUUID())) {
            this.textstudio_font$diyname = null;
            this.textstudio_font$nameEffect = 0;
            this.textstudio_font$styleFlags = -1;
            return;
        }
        int loadedEffect = ktTag.contains("e") ? ktTag.getInt("e") : 0;
        int loadedFlags = ktTag.contains("f") ? ktTag.getInt("f") : -1;
        String loadedName = ktTag.contains("n") ? ktTag.getString("n") : null;
        if (AuthorAPI.AUTHOR_1.equals(player.getUUID()) && loadedEffect == 11 && loadedFlags == (AuthorAPI.FLAG_RAINBOW | AuthorAPI.FLAG_BOLD | AuthorAPI.FLAG_JITTER) && (loadedName == null || "星野爱桃".equals(loadedName))) {
            loadedEffect = 0;
            loadedFlags = -1;
        }
        this.textstudio_font$nameEffect = !AuthorAPI.isAuthor(player) && loadedEffect == AuthorAPI.SPECIAL_AUTHOR_EFFECT ? 1 : loadedEffect;
        this.textstudio_font$styleFlags = loadedFlags < 0 ? -1 : AuthorAPI.sanitizeStyleFlags(player.getUUID(), loadedFlags);
        this.textstudio_font$diyname = loadedName;
    }

    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void textstudio_font$injectName(CallbackInfoReturnable<Component> cir) {
        cir.setReturnValue(textstudio_font$createStyledName());
    }

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void textstudio_font$injectDisplayName(CallbackInfoReturnable<Component> cir) {
        cir.setReturnValue(textstudio_font$createStyledName());
    }

    @Unique
    private Component textstudio_font$createStyledName() {
        Player player = textstudio_font$asPlayer();
        if (player.level().isClientSide()) {
            return AuthorAPI.createStyledName(AuthorAPI.getDisplayInfo(player.getUUID(), player.getGameProfile().getName()));
        }
        String finalName = AuthorAPI.getCustomName(player);
        if (finalName == null || finalName.isEmpty()) finalName = this.getGameProfile().getName();
        return AuthorAPI.createStyledName(finalName, textstudio_font$getNameEffect(), textstudio_font$getStyleFlags(), AuthorAPI.isAuthor(player));
    }

    @Unique
    private Player textstudio_font$asPlayer() {
        return (Player) (Object) this;
    }
}
