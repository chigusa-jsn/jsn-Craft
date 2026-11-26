package com.chigusa.jsncraft.airship;

/** 上昇・下降のキー（本人のパソコンでだけ読む。サーバーでは呼ばれない） */
final class AirshipInputBridge {
    static boolean up() {
        return com.chigusa.jsncraft.client.AirshipInput.up();
    }

    static boolean down() {
        return com.chigusa.jsncraft.client.AirshipInput.down();
    }

    private AirshipInputBridge() {}
}
