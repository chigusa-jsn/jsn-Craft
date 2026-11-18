package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.network.SkillPayloads;
import com.chigusa.jsncraft.network.SkillPayloads.SkillView;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * クライアント側のスキル：サーバーから届いた一覧と待ち時間、スキルモードの切り替え。
 * スキルモード中は、数字キー1〜5がホットバーの持ち替えではなくスキルになる。
 */
public final class ClientSkills {

    /** 1ページの枠の数 */
    public static final int SLOTS = 5;
    private static final int MAX = 15;

    public static final KeyMapping TOGGLE = new KeyMapping("key.jsn_craft.skill_mode", GLFW.GLFW_KEY_R, "key.categories.jsn_craft");
    public static final KeyMapping PAGE_KEY = new KeyMapping("key.jsn_craft.skill_page", GLFW.GLFW_KEY_G, "key.categories.jsn_craft");

    /** 今のページ（0から） */
    public static int page = 0;

    private static SkillPayloads.State state = new SkillPayloads.State(List.of(), List.of("", "", "", "", ""), List.of(0F, 0F, 0F, 0F, 0F));
    private static final long[] readyAt = new long[MAX];
    public static boolean skillMode = false;

    public static SkillPayloads.State state() {
        return state;
    }

    public static void update(SkillPayloads.State s) {
        state = s;
        long now = gameTime();
        if (page >= pages()) page = 0;
        for (int i = 0; i < MAX && i < s.cooldownLeft().size(); i++) {
            readyAt[i] = now + Math.round(s.cooldownLeft().get(i) * 20);
        }
    }

    private static long gameTime() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0 : mc.level.getGameTime();
    }

    @Nullable
    public static SkillView find(String id) {
        if (id == null || id.isEmpty()) return null;
        for (SkillView v : state.skills()) if (v.id().equals(id)) return v;
        return null;
    }

    /** ページ数（サーバーが送ってきた枠の数で決まる。魔導士は3） */
    public static int pages() {
        return Math.max(1, Math.min(3, state.slots().size() / SLOTS));
    }

    /** 今のページの i 番目の枠の通し番号 */
    public static int onPage(int i) {
        return page * SLOTS + i;
    }

    public static void nextPage() {
        page = (page + 1) % pages();
    }

    /** 通し番号 i の枠のスキル */
    public static String slot(int i) {
        return i < state.slots().size() ? state.slots().get(i) : "";
    }

    /** 残り待ち時間（秒） */
    public static float cooldownLeft(int i) {
        return i < 0 || i >= MAX ? 0 : Math.max(0, (readyAt[i] - gameTime()) / 20F);
    }

    // ---- キー ----

    @EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Register {
        @SubscribeEvent
        public static void onKeys(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE);
            event.register(PAGE_KEY);
        }
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, value = Dist.CLIENT)
    public static final class Input {
        /** バニラがホットバーのキーを読む前に、スキルモードなら横取りする */
        @SubscribeEvent
        public static void onTick(ClientTickEvent.Pre event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                skillMode = false;
                return;
            }
            while (TOGGLE.consumeClick()) {
                if (mc.screen != null) continue;
                skillMode = !skillMode;
                mc.player.displayClientMessage(skillMode
                        ? Component.literal("スキルモード：1〜5でスキル（Rで戻る）").withStyle(ChatFormatting.AQUA)
                        : Component.literal("通常モード").withStyle(ChatFormatting.GRAY), true);
            }
            while (PAGE_KEY.consumeClick()) {
                if (mc.screen != null) continue;
                if (pages() <= 1) {
                    mc.player.displayClientMessage(Component.literal("スキルのページは1つだけです（魔導士は3ページ）").withStyle(ChatFormatting.GRAY), true);
                    continue;
                }
                nextPage();
                mc.player.displayClientMessage(Component.literal("スキルページ " + (page + 1) + " / " + pages()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            if (!skillMode || mc.player.isSpectator()) return;
            for (int i = 0; i < SLOTS; i++) {
                KeyMapping key = mc.options.keyHotbarSlots[i];
                while (key.consumeClick()) {
                    if (mc.screen == null) PacketDistributor.sendToServer(new SkillPayloads.Cast(onPage(i)));
                }
            }
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            skillMode = false;
        }
    }

    private ClientSkills() {}
}
