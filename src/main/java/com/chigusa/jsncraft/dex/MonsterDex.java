package com.chigusa.jsncraft.dex;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.data.ModAttachments;
import com.chigusa.jsncraft.gacha.FigureItem;
import com.chigusa.jsncraft.gacha.GachaKind;
import com.chigusa.jsncraft.gacha.GachaPool;
import com.chigusa.jsncraft.gacha.TitleTicketItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * モンスター図鑑（倒すと登録、倒した数・強さ・弱点）とフィギュア図鑑（持つと登録）。
 * 登録数が区切りに届くと、自動でご褒美がもらえる。
 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class MonsterDex {
    private static final Set<String> SKIP = Set.of("minecraft:giant", "minecraft:illusioner");

    /** ご褒美：[登録数, 説明]（数が -1 なら全部） */
    public record Milestone(int count, String desc) {}

    public static final List<Milestone> MONSTER_REWARDS = List.of(
            new Milestone(10, "エメラルド ×8"), new Milestone(20, "エメラルド ×16"), new Milestone(30, "ダイヤモンド ×3"),
            new Milestone(40, "ダイヤモンド ×5"), new Milestone(-1, "肩書《魔物博士》＋ネザースター"));
    public static final List<Milestone> FIGURE_REWARDS = List.of(
            new Milestone(10, "エメラルド ×8"), new Milestone(25, "ダイヤモンド ×2"), new Milestone(50, "ダイヤモンド ×5"),
            new Milestone(70, "エメラルドブロック ×3"), new Milestone(-1, "肩書《フィギュア王》＋ネザースター"));

    public static MonsterDexData data(ServerPlayer p) {
        return p.getData(ModAttachments.MONSTER_DEX);
    }

    private static List<String> monsterCache;

    /** 図鑑に載るモンスター（敵の種類）の一覧 */
    public static List<String> monsters() {
        if (monsterCache == null) {
            List<String> out = new ArrayList<>();
            for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
                if (t.getCategory() != MobCategory.MONSTER) continue;
                String id = BuiltInRegistries.ENTITY_TYPE.getKey(t).toString();
                if (!SKIP.contains(id)) out.add(id);
            }
            monsterCache = out;
        }
        return monsterCache;
    }

    /** フィギュアの一覧（フィギュアガチャの中身）：id → レア度 */
    public static Map<String, Integer> figures(MinecraftServer server) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (var e : GachaPool.get(server, GachaKind.FIGURE).figures()) out.putIfAbsent(e.getKey(), e.getValue());
        for (GachaKind k : GachaKind.values()) {
            if (k == GachaKind.FIGURE) continue;
            for (var e : GachaPool.get(server, k).figures()) out.putIfAbsent(e.getKey(), e.getValue());
        }
        return out;
    }

    // ================= 強さと弱点 =================

    @SuppressWarnings("unchecked")
    public static float[] stats(EntityType<?> t) {
        try {
            EntityType<? extends LivingEntity> lt = (EntityType<? extends LivingEntity>) t;
            if (!DefaultAttributes.hasSupplier(lt)) return new float[]{0, 0};
            AttributeSupplier s = DefaultAttributes.getSupplier(lt);
            float hp = s.hasAttribute(Attributes.MAX_HEALTH) ? (float) s.getBaseValue(Attributes.MAX_HEALTH) : 0;
            float atk = s.hasAttribute(Attributes.ATTACK_DAMAGE) ? (float) s.getBaseValue(Attributes.ATTACK_DAMAGE) : 0;
            return new float[]{hp, atk};
        } catch (Exception e) {
            return new float[]{0, 0};
        }
    }

    public static String weakness(EntityType<?> t) {
        List<String> w = new ArrayList<>();
        if (t.is(EntityTypeTags.SENSITIVE_TO_SMITE) || t.is(EntityTypeTags.UNDEAD)) w.add("アンデッド特効・治癒のポーション");
        if (t.is(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) w.add("虫特効");
        if (t.is(EntityTypeTags.SENSITIVE_TO_IMPALING)) w.add("水棲特効");
        if (t == EntityType.ENDERMAN || t == EntityType.BLAZE || t == EntityType.SNOW_GOLEM) w.add("水");
        if (t == EntityType.CREEPER) w.add("爆発する前に倒す・ネコ");
        if (t == EntityType.PHANTOM) w.add("ネコ・昼の光");
        if (!t.fireImmune() && w.isEmpty()) w.add("火");
        if (t.fireImmune()) w.add("（火は効かない）");
        return String.join("、", w);
    }

    // ================= 記録 =================

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p)) return;
        EntityType<?> t = event.getEntity().getType();
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(t).toString();
        if (!monsters().contains(id)) return;
        MonsterDexData d = data(p);
        if (d.kill(id)) {
            p.displayClientMessage(Component.literal("モンスター図鑑に登録：").withStyle(ChatFormatting.RED)
                    .append(t.getDescription().copy().withStyle(ChatFormatting.WHITE)), true);
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
            checkRewards(p);
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 40 != 0) return;
        MonsterDexData d = data(p);
        boolean any = false;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!(s.getItem() instanceof FigureItem)) continue;
            String id = FigureItem.entityId(s);
            if (id.isEmpty() || !d.figure(id)) continue;
            EntityType<?> t = FigureItem.type(id);
            p.displayClientMessage(Component.literal("フィギュア図鑑に登録：").withStyle(ChatFormatting.LIGHT_PURPLE)
                    .append(t == null ? Component.literal(id) : t.getDescription().copy().withStyle(ChatFormatting.WHITE)), true);
            any = true;
        }
        if (any) {
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.3F);
            checkRewards(p);
        }
    }

    // ================= ご褒美 =================

    public static int monsterCount(ServerPlayer p) {
        MonsterDexData d = data(p);
        int n = 0;
        for (String id : monsters()) if (d.kills(id) > 0) n++;
        return n;
    }

    public static int figureCount(ServerPlayer p, Map<String, Integer> all) {
        MonsterDexData d = data(p);
        int n = 0;
        for (String id : all.keySet()) if (d.hasFigure(id)) n++;
        return n;
    }

    public static void checkRewards(ServerPlayer p) {
        MonsterDexData d = data(p);
        int mTotal = monsters().size(), mHave = monsterCount(p);
        for (Milestone m : MONSTER_REWARDS) {
            int need = m.count() < 0 ? mTotal : Math.min(m.count(), mTotal);
            if (mHave >= need && d.claim("monster_" + m.count())) give(p, "モンスター図鑑", m, true);
        }
        Map<String, Integer> figs = figures(p.server);
        int fTotal = figs.size(), fHave = figureCount(p, figs);
        if (fTotal > 0) for (Milestone m : FIGURE_REWARDS) {
            int need = m.count() < 0 ? fTotal : Math.min(m.count(), fTotal);
            if (fHave >= need && d.claim("figure_" + m.count())) give(p, "フィギュア図鑑", m, false);
        }
    }

    private static void give(ServerPlayer p, String book, Milestone m, boolean monster) {
        List<ItemStack> items = new ArrayList<>();
        switch (m.count()) {
            case 10 -> items.add(new ItemStack(Items.EMERALD, 8));
            case 20 -> items.add(new ItemStack(Items.EMERALD, 16));
            case 25 -> items.add(new ItemStack(Items.DIAMOND, 2));
            case 30 -> items.add(new ItemStack(Items.DIAMOND, 3));
            case 40, 50 -> items.add(new ItemStack(Items.DIAMOND, 5));
            case 70 -> items.add(new ItemStack(Items.EMERALD_BLOCK, 3));
            default -> {
                items.add(TitleTicketItem.of(monster ? "魔物博士" : "フィギュア王"));
                items.add(new ItemStack(Items.NETHER_STAR));
            }
        }
        for (ItemStack s : items) if (!p.getInventory().add(s)) p.drop(s, false);
        String when = m.count() < 0 ? "全部そろった" : m.count() + "種類 登録";
        p.sendSystemMessage(Component.literal("【" + book + "】" + when + "のご褒美：" + m.desc()).withStyle(ChatFormatting.GOLD));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.4F);
    }

    public static ResourceLocation key(String id) {
        return ResourceLocation.tryParse(id);
    }

    private MonsterDex() {}
}
