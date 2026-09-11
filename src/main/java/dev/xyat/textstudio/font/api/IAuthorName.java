package dev.xyat.textstudio.font.api;

public interface IAuthorName {
    String textstudio_font$getCustomdiyname();
    void textstudio_font$setCustomdiyname(String name);

    int textstudio_font$getNameEffect();
    void textstudio_font$setNameEffect(int effectId);

    int textstudio_font$getStyleFlags();
    void textstudio_font$setStyleFlags(int flags);

    void textstudio_font$setState(String name, int effectId, int flags);

    // 辅助方法：切换样式标志位
    default void toggleStyleFlag(int flagBit, boolean enable) {
        int current = textstudio_font$getStyleFlags();
        if (enable) {
            textstudio_font$setStyleFlags(current | flagBit);
        } else {
            textstudio_font$setStyleFlags(current & ~flagBit);
        }
    }
}
