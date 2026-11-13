package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.network.BoardSnapshotPayload;
import com.chigusa.jsncraft.network.QuestEditorPayloads;
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

    /** 次に開くメニューのタブを決める（受付嬢に話しかけたら依頼タブ）。 */
    public static void startOnTab(int tab) {
        JobScreen.startOnTab(tab);
    }

    public static void openEditor(QuestEditorPayloads.Open payload) {
        Minecraft.getInstance().setScreen(new QuestEditorScreen(payload.data(), payload.jobs()));
    }

    public static void editorResult(QuestEditorPayloads.Result result) {
        if (Minecraft.getInstance().screen instanceof QuestEditorScreen screen) {
            screen.onResult(result.ok(), result.message());
        }
    }

    public static void openJobChange(com.chigusa.jsncraft.network.JobChangePayloads.Open payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof JobChangeScreen screen) screen.update(payload.jobs());
        else mc.setScreen(new JobChangeScreen(payload.jobs()));
    }

    private ClientMenuHandler() {}
}
