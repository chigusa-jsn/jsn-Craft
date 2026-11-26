package com.chigusa.jsncraft.farming;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.job.JobLevels;
import com.chigusa.jsncraft.forestry.SoilBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 作物のランク（★1〜★5、6 は「黄金」）。
 * ・ランクの付いた種（取れた物）を植えると、その作物はそのランクから育つ（代々受け継ぐ）
 * ・豊穣の骨粉（農家が使う）で、育っている作物のランクが1つ上がる
 * ・収穫するとき：農家の腕と土（腐葉土・豊穣の土）で、ときどき1つ上がる
 * ・交配台で、同じ種を2つ合わせてランクを上げる。★5 どうしで「黄金」に進化する（米・小麦・じゃがいも・トマト）
 * ・ランクが高い作物で料理すると、料理の★が上がりやすい
 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class CropRanks {
    public static final int GOLDEN = 6;

    // ================= アイテム =================

    public static int rank(ItemStack st) {
        return st.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("jsn_crop_rank");
    }

    public static void setRank(ItemStack st, int r) {
        if (r <= 1) {
            CustomData.update(DataComponents.CUSTOM_DATA, st, t -> t.remove("jsn_crop_rank"));
            if (st.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).isEmpty()) st.remove(DataComponents.CUSTOM_DATA);
        } else CustomData.update(DataComponents.CUSTOM_DATA, st, t -> t.putInt("jsn_crop_rank", Math.min(GOLDEN, r)));
    }

    /** 料理の★の上がりやすさ（ランク1つにつき +0.04、黄金の食材は別に +0.3） */
    public static double qualityBonus(ItemStack st) {
        int r = rank(st);
        double b = r > 1 ? 0.04 * (Math.min(5, r) - 1) : 0;
        if (ModFarming.isGolden(st)) b += 0.3;
        return b;
    }

    public static String label(int r) {
        if (r >= GOLDEN) return "黄金";
        return "★".repeat(Math.max(1, r)) + "☆".repeat(5 - Math.max(1, r));
    }

    // ================= 畑（場所ごとのランク） =================

    public static final class Data extends SavedData {
        final Map<Long, Integer> map = new HashMap<>();

        static Data get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Data::new, Data::load), "jsn_craft_crop_ranks");
        }

        static Data load(CompoundTag t, HolderLookup.Provider reg) {
            Data d = new Data();
            long[] k = t.getLongArray("k");
            int[] v = t.getIntArray("v");
            for (int i = 0; i < Math.min(k.length, v.length); i++) d.map.put(k[i], v[i]);
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag t, HolderLookup.Provider reg) {
            long[] k = new long[map.size()];
            int[] v = new int[map.size()];
            int i = 0;
            for (var e : map.entrySet()) {
                k[i] = e.getKey();
                v[i++] = e.getValue();
            }
            t.putLongArray("k", k);
            t.putIntArray("v", v);
            return t;
        }
    }

    public static int at(ServerLevel level, BlockPos pos) {
        return Data.get(level).map.getOrDefault(pos.asLong(), 1);
    }

    public static void set(ServerLevel level, BlockPos pos, int r) {
        Data d = Data.get(level);
        if (r <= 1) d.map.remove(pos.asLong());
        else d.map.put(pos.asLong(), Math.min(GOLDEN, r));
        d.setDirty();
    }

    // ================= 植える・収穫する =================

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Player p)) return;
        BlockState st = event.getPlacedBlock();
        if (!(st.getBlock() instanceof CropBlock || st.getBlock() instanceof JsnCropBlock)) return;
        int r = 1;
        for (ItemStack held : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()}) {
            if (held.getItem() instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() == st.getBlock()) { r = rank(held); break; }
        }
        set(level, event.getPos(), r);
    }

    /** 収穫したときの取れた物にランクを付ける（黄金なら黄金の食材に変える）。ランクを返す */
    public static int apply(ServerLevel level, BlockPos pos, BlockState st, List<ItemStack> drops, ServerPlayer harvester) {
        int r = at(level, pos);
        if (r < GOLDEN && harvester != null && isCropLike(st)) {
            int lv = JobLevels.farmer(harvester);
            double chance = lv >= 0 ? 0.05 + Math.min(lv, 100) * 0.004 : 0;
            if (level.getBlockState(pos.below()).getBlock() instanceof SoilBlock soil) chance += 0.1 * soil.grade;
            if (chance > 0 && level.random.nextDouble() < chance) r = Math.min(5, r + 1);
        }
        if (r <= 1) return r;
        Item golden = r >= GOLDEN ? ModFarming.goldenFor(st) : null;
        boolean keptSeed = false;
        List<ItemStack> extra = new java.util.ArrayList<>();
        int n = drops.size();
        for (int i = 0; i < n; i++) {
            ItemStack s = drops.get(i);
            if (s.isEmpty()) continue;
            if (golden != null && ModFarming.goldenBase(golden) == s.getItem()) {
                // 1つは黄金の種として残して、あとは黄金の食材に
                boolean isSeed = st.getBlock().getCloneItemStack(level, pos, st).is(s.getItem());
                if (isSeed && !keptSeed) {
                    keptSeed = true;
                    ItemStack seed = s.split(1);
                    setRank(seed, GOLDEN);
                    extra.add(seed);
                    if (s.isEmpty()) continue;
                }
                ItemStack g = new ItemStack(golden, s.getCount());
                drops.set(i, g);
                continue;
            }
            setRank(s, r);
        }
        drops.addAll(extra);
        return r;
    }

    static boolean isCropLike(BlockState st) {
        return st.getBlock() instanceof CropBlock || st.getBlock() instanceof GiantCropBlock || st.getBlock() instanceof JsnCropBlock;
    }

    /** ふつうに壊したとき（右クリック収穫は FarmingService から apply を呼ぶ） */
    @SubscribeEvent
    public static void onDrops(BlockDropsEvent event) {
        ServerLevel level = event.getLevel();
        BlockPos pos = event.getPos();
        Data d = Data.get(level);
        if (!d.map.containsKey(pos.asLong()) && !(event.getBreaker() instanceof ServerPlayer)) return;
        if (!isCropLike(event.getState())) return;
        List<ItemStack> stacks = new java.util.ArrayList<>();
        for (ItemEntity e : event.getDrops()) stacks.add(e.getItem());
        int before = stacks.size();
        apply(level, pos, event.getState(), stacks, event.getBreaker() instanceof ServerPlayer sp ? sp : null);
        for (int i = 0; i < event.getDrops().size() && i < stacks.size(); i++) event.getDrops().get(i).setItem(stacks.get(i));
        for (int i = before; i < stacks.size(); i++)
            event.getDrops().add(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stacks.get(i)));
        set(level, pos, 1);
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, value = Dist.CLIENT)
    public static final class Client {
        @SubscribeEvent
        public static void onTooltip(ItemTooltipEvent event) {
            int r = rank(event.getItemStack());
            if (r <= 1) return;
            ChatFormatting c = r >= GOLDEN ? ChatFormatting.GOLD : r >= 5 ? ChatFormatting.LIGHT_PURPLE : r >= 4 ? ChatFormatting.RED : r >= 3 ? ChatFormatting.AQUA : ChatFormatting.GREEN;
            event.getToolTip().add(Math.min(1, event.getToolTip().size()), Component.literal("作物ランク " + label(r)).withStyle(c));
        }
    }

    private CropRanks() {}
}
