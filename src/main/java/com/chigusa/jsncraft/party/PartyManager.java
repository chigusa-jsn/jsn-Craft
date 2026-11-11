package com.chigusa.jsncraft.party;

import com.chigusa.jsncraft.network.PartyStatusPayload;
import com.chigusa.jsncraft.network.PartyStatusPayload.Member;
import com.chigusa.jsncraft.stat.ModAttributes;
import com.chigusa.jsncraft.stat.ResourceService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * パーティー。サーバーが動いている間だけ覚えている（ログアウトすると抜ける）。
 * 先頭のメンバーがリーダー。リーダーが抜けたら次の人がリーダーになる。
 */
public final class PartyManager {

    /** 1パーティーの最大人数 */
    public static final int MAX_MEMBERS = 4;
    /** 招待の有効時間（tick）。60秒 */
    private static final int INVITE_TICKS = 20 * 60;

    public static final class Party {
        public final Set<UUID> members = new LinkedHashSet<>();

        public UUID leader() {
            return members.iterator().next();
        }
    }

    private record Invite(Party party, UUID from, long expires) {}

    private static final Map<UUID, Party> BY_MEMBER = new HashMap<>();
    private static final Map<UUID, Invite> INVITES = new HashMap<>();

    @Nullable
    public static Party partyOf(ServerPlayer player) {
        return BY_MEMBER.get(player.getUUID());
    }

    // ---------------- 招待・参加・脱退 ----------------

    @Nullable
    public static Component invite(ServerPlayer from, ServerPlayer to) {
        if (from == to) return Component.literal("自分は誘えません");
        Party party = BY_MEMBER.get(from.getUUID());
        if (party != null && !party.leader().equals(from.getUUID())) return Component.literal("誘えるのはリーダーだけです");
        if (party != null && party.members.size() >= MAX_MEMBERS) return Component.literal("パーティーは " + MAX_MEMBERS + " 人までです");
        if (BY_MEMBER.containsKey(to.getUUID())) return Component.literal(to.getName().getString() + " はもう別のパーティーにいます");

        if (party == null) {
            party = new Party();
            party.members.add(from.getUUID());
            BY_MEMBER.put(from.getUUID(), party);
        }
        INVITES.put(to.getUUID(), new Invite(party, from.getUUID(), from.server.getTickCount() + INVITE_TICKS));

        to.sendSystemMessage(Component.literal(from.getName().getString() + " からパーティーに誘われました ").withStyle(ChatFormatting.AQUA)
                .append(button("[参加する]", "/party accept", ChatFormatting.GREEN))
                .append(" ")
                .append(button("[断る]", "/party decline", ChatFormatting.RED)));
        from.sendSystemMessage(Component.literal(to.getName().getString() + " をパーティーに誘いました（60秒以内に返事がなければ取り消し）")
                .withStyle(ChatFormatting.GRAY));
        sync(party, from.server);
        return null;
    }

    @Nullable
    public static Component accept(ServerPlayer player) {
        Invite inv = INVITES.remove(player.getUUID());
        if (inv == null || inv.expires() < player.server.getTickCount()) return Component.literal("届いている招待はありません");
        if (BY_MEMBER.containsKey(player.getUUID())) return Component.literal("もうパーティーにいます（/party leave で抜けられます）");
        Party party = inv.party();
        if (party.members.isEmpty()) return Component.literal("そのパーティーはもう解散しています");
        if (party.members.size() >= MAX_MEMBERS) return Component.literal("パーティーがいっぱいです");

        party.members.add(player.getUUID());
        BY_MEMBER.put(player.getUUID(), party);
        tell(party, player.server, Component.literal(player.getName().getString() + " がパーティーに参加しました").withStyle(ChatFormatting.GREEN));
        sync(party, player.server);
        return null;
    }

    public static void decline(ServerPlayer player) {
        Invite inv = INVITES.remove(player.getUUID());
        if (inv == null) return;
        ServerPlayer from = player.server.getPlayerList().getPlayer(inv.from());
        if (from != null) from.sendSystemMessage(Component.literal(player.getName().getString() + " は誘いを断りました").withStyle(ChatFormatting.GRAY));
        cleanupIfAlone(inv.party(), player.server);
    }

    public static void leave(ServerPlayer player) {
        leave(player.getUUID(), player.getName().getString(), player.server);
        PacketDistributor.sendToPlayer(player, new PartyStatusPayload(List.of()));
    }

    private static void leave(UUID id, String name, MinecraftServer server) {
        Party party = BY_MEMBER.remove(id);
        if (party == null) return;
        party.members.remove(id);
        tell(party, server, Component.literal(name + " がパーティーを抜けました").withStyle(ChatFormatting.YELLOW));
        cleanupIfAlone(party, server);
        sync(party, server);
    }

    @Nullable
    public static Component kick(ServerPlayer leader, ServerPlayer target) {
        Party party = BY_MEMBER.get(leader.getUUID());
        if (party == null || !party.leader().equals(leader.getUUID())) return Component.literal("外せるのはリーダーだけです");
        if (BY_MEMBER.get(target.getUUID()) != party || target == leader) return Component.literal("その人はパーティーにいません");
        target.sendSystemMessage(Component.literal("パーティーから外されました").withStyle(ChatFormatting.YELLOW));
        leave(target);
        return null;
    }

    /** 1人だけになったパーティーは解散する。 */
    private static void cleanupIfAlone(Party party, MinecraftServer server) {
        boolean pendingInvite = INVITES.values().stream().anyMatch(i -> i.party() == party);
        if (party.members.size() <= 1 && !pendingInvite) {
            for (UUID id : new ArrayList<>(party.members)) {
                BY_MEMBER.remove(id);
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) {
                    p.sendSystemMessage(Component.literal("パーティーは解散しました").withStyle(ChatFormatting.GRAY));
                    PacketDistributor.sendToPlayer(p, new PartyStatusPayload(List.of()));
                }
            }
            party.members.clear();
        }
    }

    public static void onLogout(ServerPlayer player) {
        INVITES.remove(player.getUUID());
        leave(player.getUUID(), player.getName().getString(), player.server);
    }

    // ---------------- 毎秒の処理 ----------------

    /** 期限切れの招待を片付けて、全パーティーの様子をメンバーに送る。 */
    public static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        List<Party> expired = new ArrayList<>();
        INVITES.entrySet().removeIf(e -> {
            if (e.getValue().expires() >= now) return false;
            expired.add(e.getValue().party());
            return true;
        });
        for (Party p : expired) cleanupIfAlone(p, server);

        for (Party party : new LinkedHashSet<>(BY_MEMBER.values())) sync(party, server);
    }

    private static void sync(Party party, MinecraftServer server) {
        if (party.members.isEmpty()) return;
        List<Member> members = new ArrayList<>();
        UUID leader = party.leader();
        for (UUID id : party.members) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) continue;
            members.add(new Member(id.toString(), p.getName().getString(),
                    p.getHealth() + p.getAbsorptionAmount(), p.getMaxHealth(),
                    ResourceService.get(p).mana, (float) p.getAttributeValue(ModAttributes.MAX_MANA),
                    id.equals(leader), !p.isAlive()));
        }
        PartyStatusPayload payload = new PartyStatusPayload(members);
        for (UUID id : party.members) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) PacketDistributor.sendToPlayer(p, payload);
        }
    }

    // ---------------- 小道具 ----------------

    private static void tell(Party party, MinecraftServer server, Component msg) {
        for (UUID id : party.members) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) p.sendSystemMessage(msg);
        }
    }

    private static Component button(String label, String command, ChatFormatting color) {
        return Component.literal(label).withStyle(s -> s.withColor(color)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(command))));
    }

    public static List<String> memberNames(Party party, MinecraftServer server) {
        List<String> out = new ArrayList<>();
        for (UUID id : party.members) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            out.add((id.equals(party.leader()) ? "★" : "・") + (p != null ? p.getName().getString() : "?"));
        }
        return out;
    }

    private PartyManager() {}
}
