package com.chigusa.jsncraft.client.casino;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;

/** カジノの画面の共通の飾り */
final class CasinoUi {
    static final int FELT = 0xFF0E4A2E, FELT_D = 0xFF0A3622, GOLD = 0xFFE6B836, GOLD_D = 0xFF8A6A1A, RED = 0xFF8A1A1A;

    static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 3, y - 3, x + w + 3, y + h + 3, 0xFF1A0A0A);
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, GOLD_D);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, GOLD);
        g.fill(x, y, x + w, y + h, RED);
        g.fill(x + 4, y + 4, x + w - 4, y + h - 4, FELT);
    }

    static void sound(SoundEvent s, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch));
    }

    static void sound(Holder<SoundEvent> s, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch));
    }

    static String fmt(long n) {
        return String.format("%,d", n);
    }

    private CasinoUi() {}
}
