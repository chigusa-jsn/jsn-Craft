package com.chigusa.jsncraft.client;

import net.minecraft.client.Minecraft;

/** アイテムから画面を開く（クライアントだけで呼ぶ） */
public final class ClientOpen {
    public static void runeBook() {
        Minecraft.getInstance().setScreen(new RuneBookScreen());
    }

    private ClientOpen() {}
}
