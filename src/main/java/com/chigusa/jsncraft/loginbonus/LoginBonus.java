package com.chigusa.jsncraft.loginbonus;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.casino.Money;
import com.chigusa.jsncraft.data.ModAttachments;
import com.chigusa.jsncraft.gacha.TitleTicketItem;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * ログインボーナス：1日1回、カレンダーのはんこを1つ押してご褒美をもらう（日付は日本時間の朝5時で変わる）。
 * 休んでも進み具合は戻らない。全部押すと次の周へ。連続ログインのおまけもある。
 * 中身は data/jsn_craft/login_bonus.json。
 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class LoginBonus {
    public static final ZoneId ZONE = ZoneId.of("Asia/Tokyo");
    public static final int RESET_HOUR = 5;

    /** ご褒美1つ：kind は item / money / title / stack（ゲームの中で置いたアイテム） */
    public record Reward(String kind, String id, int count, ItemStack stack) {
        public Reward(String kind, String id, int count) {
            this(kind, id, count, ItemStack.EMPTY);
        }
    }

    public record Table(List<List<Reward>> days, int streakEvery, List<Reward> streakRewards) {}

    private static Table cache;
    private static MinecraftServer cachedFor;

    public static long today() {
        return LocalDateTime.now(ZONE).minusHours(RESET_HOUR).toLocalDate().toEpochDay();
    }

    public static LoginBonusData data(ServerPlayer p) {
        return p.getData(ModAttachments.LOGIN_BONUS);
    }

    public static boolean canClaim(ServerPlayer p) {
        return data(p).last < today();
    }

    // ================= 中身の読み込み =================

    /** 編集したら呼ぶ（次に使うとき読み直す） */
    public static void invalidate() {
        cache = null;
    }

    /** login_bonus.json の中身（編集のはじめの形にも使う） */
    public static Table fileTable(MinecraftServer server) {
        List<List<Reward>> days = new ArrayList<>();
        int every = 7;
        List<Reward> streak = new ArrayList<>();
        try {
            var res = server.getResourceManager().getResource(JsnCraft.id("login_bonus.json"));
            if (res.isPresent()) try (var in = new InputStreamReader(res.get().open(), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(in).getAsJsonObject();
                for (JsonElement de : o.getAsJsonArray("days")) days.add(rewards(de));
                if (o.has("streak_every")) every = o.get("streak_every").getAsInt();
                if (o.has("streak_bonus")) streak = rewards(o.get("streak_bonus"));
            }
        } catch (Exception e) {
            JsnCraft.LOGGER.warn("[jsn-Craft] login_bonus.json が読めません: {}", e.toString());
        }
        if (days.isEmpty()) days.add(List.of(new Reward("item", "minecraft:bread", 8)));
        return new Table(days, Math.max(1, every), streak);
    }

    public static Table table(MinecraftServer server) {
        if (cache != null && cachedFor == server) return cache;
        LoginBonusSaved saved = LoginBonusSaved.get(server);
        if (saved.edited && !saved.days.isEmpty()) {
            List<List<Reward>> days = new ArrayList<>();
            for (LoginBonusSaved.Day d : saved.days) {
                List<Reward> rs = new ArrayList<>();
                if (d.money > 0) rs.add(new Reward("money", "", d.money));
                for (ItemStack st : d.items) if (!st.isEmpty()) rs.add(new Reward("stack", "", st.getCount(), st.copy()));
                days.add(rs);
            }
            List<Reward> streak = saved.streakMoney > 0 ? List.of(new Reward("money", "", saved.streakMoney)) : List.of();
            cache = new Table(days, Math.max(1, saved.streakEvery), streak);
        } else {
            cache = fileTable(server);
        }
        cachedFor = server;
        return cache;
    }

    /** 編集を始めるとき：まだ編集していなければ、ファイルの中身を写す */
    public static LoginBonusSaved startEdit(MinecraftServer server) {
        LoginBonusSaved saved = LoginBonusSaved.get(server);
        if (saved.edited && !saved.days.isEmpty()) return saved;
        Table t = fileTable(server);
        saved.days.clear();
        for (List<Reward> rs : t.days()) {
            LoginBonusSaved.Day d = new LoginBonusSaved.Day();
            for (Reward r : rs) {
                if (r.kind().equals("money")) d.money += r.count();
                else if (d.items.size() < LoginBonusSaved.SLOTS) d.items.add(icon(r));
            }
            saved.days.add(d);
        }
        saved.streakEvery = t.streakEvery();
        saved.streakMoney = 0;
        for (Reward r : t.streakRewards()) if (r.kind().equals("money")) saved.streakMoney += r.count();
        saved.edited = true;
        saved.setDirty();
        return saved;
    }

    private static List<Reward> rewards(JsonElement el) {
        List<Reward> out = new ArrayList<>();
        for (JsonElement e : el.isJsonArray() ? el.getAsJsonArray() : List.of(el)) {
            JsonObject r = e.getAsJsonObject();
            int count = r.has("count") ? r.get("count").getAsInt() : 1;
            if (r.has("money")) out.add(new Reward("money", "", r.get("money").getAsInt()));
            else if (r.has("title")) out.add(new Reward("title", r.get("title").getAsString(), 1));
            else if (r.has("item")) {
                ResourceLocation id = ResourceLocation.tryParse(r.get("item").getAsString());
                if (id != null && BuiltInRegistries.ITEM.containsKey(id)) out.add(new Reward("item", id.toString(), count));
            }
        }
        return out;
    }

    /** ご褒美の見た目（画面に出すアイテム） */
    public static ItemStack icon(Reward r) {
        return switch (r.kind()) {
            case "money" -> {
                Item coin = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("lightmanscurrency", "coin_iron"));
                yield Money.hasLightman() ? new ItemStack(coin, Math.min(99, r.count())) : new ItemStack(Items.EMERALD, Math.max(1, Math.min(99, r.count() / Money.CHIPS_PER_EMERALD)));
            }
            case "title" -> TitleTicketItem.of(r.id());
            case "stack" -> r.stack().copy();
            default -> new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(r.id())), r.count());
        };
    }

    public static String label(Reward r) {
        return switch (r.kind()) {
            case "money" -> Money.hasLightman() ? "お金（鉄コイン " + r.count() + " 枚分）" : "エメラルド ×" + Math.max(1, r.count() / Money.CHIPS_PER_EMERALD);
            case "title" -> "肩書チケット《" + r.id() + "》";
            default -> {
                ItemStack st = icon(r);
                yield st.getHoverName().getString() + (st.getCount() > 1 ? " ×" + st.getCount() : "");
            }
        };
    }

    private static void give(ServerPlayer p, Reward r) {
        if (r.kind().equals("money")) {
            int c = r.count();
            if (!Money.hasLightman()) c = Math.max(Money.CHIPS_PER_EMERALD, c / Money.CHIPS_PER_EMERALD * Money.CHIPS_PER_EMERALD);
            Money.give(p, c);
            return;
        }
        ItemStack s = icon(r);
        if (!p.getInventory().add(s)) p.drop(s, false);
    }

    // ================= 受け取る =================

    public static void claim(ServerPlayer p) {
        if (!canClaim(p)) {
            send(p, false);
            return;
        }
        LoginBonusData d = data(p);
        Table t = table(p.server);
        long today = today();
        d.streak = d.last == today - 1 ? d.streak + 1 : 1;
        d.last = today;
        d.total++;
        if (d.stamps >= t.days().size()) d.stamps = 0;
        int day = d.stamps;
        d.stamps++;
        List<String> got = new ArrayList<>();
        for (Reward r : t.days().get(day)) {
            give(p, r);
            got.add(label(r));
        }
        boolean streakBonus = d.streak % t.streakEvery() == 0 && !t.streakRewards().isEmpty();
        if (streakBonus) for (Reward r : t.streakRewards()) {
            give(p, r);
            got.add(label(r) + "（連続" + d.streak + "日のおまけ）");
        }
        p.sendSystemMessage(Component.literal("【ログインボーナス " + (day + 1) + "日目】").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(String.join("、", got)).withStyle(ChatFormatting.WHITE)));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.2F);
        if (streakBonus) p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 0.8F, 1.0F);
        if (d.stamps >= t.days().size()) {
            p.sendSystemMessage(Component.literal("カレンダーを全部うめた！ 明日から次の周がはじまるよ").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        send(p, false);
    }

    public static void send(ServerPlayer p, boolean open) {
        PacketDistributor.sendToPlayer(p, LoginBonusPayload.of(p, open));
    }

    // ================= ログイン・コマンド =================

    private static final Set<UUID> PENDING = new HashSet<>();

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && canClaim(p)) PENDING.add(p.getUUID());
    }

    /** ログインしてから少し待って、カレンダーを開く（読み込み画面に消されないように） */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount < 60 || !PENDING.remove(p.getUUID())) return;
        if (!canClaim(p)) return;
        send(p, true);
        p.sendSystemMessage(Component.literal("ログインボーナスが受け取れるよ！ ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("[カレンダーを開く]").withStyle(s -> s.withColor(ChatFormatting.AQUA).withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bonus"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("/bonus"))))));
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("bonus").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    send(p, true);
                    return 1;
                })
                .then(Commands.literal("edit").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    startEdit(p.server);
                    p.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new LoginBonusEditMenu(id, inv, p.server),
                            Component.literal("ログインボーナスの編集")));
                    return 1;
                }))
                .then(Commands.literal("again").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    data(p).last = today() - 1;
                    p.sendSystemMessage(Component.literal("ためしに、今日のログインボーナスをもう一度受け取れるようにしたよ").withStyle(ChatFormatting.GRAY));
                    send(p, true);
                    return 1;
                }))
                .then(Commands.literal("reset_file").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    LoginBonusSaved saved = LoginBonusSaved.get(ctx.getSource().getServer());
                    saved.edited = false;
                    saved.days.clear();
                    saved.setDirty();
                    invalidate();
                    ctx.getSource().sendSuccess(() -> Component.literal("ログインボーナスを login_bonus.json の中身にもどしたよ"), true);
                    return 1;
                })));
    }

    private LoginBonus() {}
}
