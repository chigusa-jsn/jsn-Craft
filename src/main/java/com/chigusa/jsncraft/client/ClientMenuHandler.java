package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.network.BoardSnapshotPayload;
import net.minecraft.client.Minecraft;

/** サーバーから職業メニューの情報が届いたときの処理（クライアント専用）。 */
public final class ClientMenuHandler {
    public static void handle(BoardSnapshotPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof JobScreen screen) {
            screen.update(payload);
        } else if (payload.open()) {
            mc.setScreen(new JobScreen(payload));
        }
    }

    private ClientMenuHandler() {}
}
