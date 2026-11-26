package com.chigusa.jsncraft.warp;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * プレイヤー一覧（Tabキー）の名前の横に、今いる場所を出す。
 * 近く（96ブロック以内）にワープ地点があればその名前、なければディメンションの名前。
 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class TabLocation {
    private static final double NEAR = 96;
    private static final Map<UUID, String> LAST = new HashMap<>();

    public static String where(ServerPlayer p) {
        String dim = p.level().dimension().location().toString();
        if (dim.startsWith("partydungeon:")) return "ダンジョン";
        String best = null;
        double bestD = NEAR * NEAR;
        for (WarpData.Point pt : WarpData.get(p.server).points) {
            if (!pt.dim.equals(dim)) continue;
            double d = p.distanceToSqr(pt.x, pt.y, pt.z);
            if (d < bestD) {
                bestD = d;
                best = pt.name;
            }
        }
        String dimName = Warps.dimLabel(p.server, dim);
        return best != null ? (dim.equals("minecraft:overworld") ? best : dimName + "・" + best) : dimName;
    }

    @SubscribeEvent
    public static void onTabName(PlayerEvent.TabListNameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        String w = LAST.getOrDefault(p.getUUID(), where(p));
        MutableComponent name = event.getDisplayName() != null ? event.getDisplayName().copy() : p.getName().copy();
        event.setDisplayName(name.append(Component.literal("  " + w).withStyle(ChatFormatting.GRAY)));
    }

    /** 5秒ごとに場所を見て、変わっていたら一覧を書きかえる */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 100 != 0) return;
        String w = where(p);
        if (w.equals(LAST.get(p.getUUID()))) return;
        LAST.put(p.getUUID(), w);
        p.refreshTabListName();
    }

    /** 表示名を変えたとき、みんなの一覧をすぐ書きかえる */
    public static void refreshAll(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            LAST.put(p.getUUID(), where(p));
            p.refreshTabListName();
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST.remove(event.getEntity().getUUID());
    }

    private TabLocation() {}
}
