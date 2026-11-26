package com.chigusa.jsncraft.client;

import net.minecraft.client.Minecraft;

/** 飛空艇の上昇（ジャンプ）・下降（ダッシュ）のキー */
public final class AirshipInput {
    public static boolean up() {
        return Minecraft.getInstance().options.keyJump.isDown();
    }

    public static boolean down() {
        return Minecraft.getInstance().options.keySprint.isDown();
    }

    private AirshipInput() {}
}
