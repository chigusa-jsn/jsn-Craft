package com.chigusa.jsncraft.furnace;

import com.chigusa.jsncraft.job.JobService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.UUID;

/**
 * 職業の炉。枠 0〜2：入れる所 / 3〜5：出てくる所 / 6：燃料（大きい炉だけ）。
 * その職業の人がスイッチを入れると、1個ずつ焼く（入れる所が空になるまで）。
 * 大きい炉：かまどで焼ける物を早く焼く。鉱石・原石は腕に応じて多めに取れる。
 * コークス炉：石炭・木炭をコークスに（石炭ブロックはコークスブロックに）。燃料は要らない。腕に応じて多めに取れる。
 */
public class MachineBlockEntity extends BlockEntity {
    public static final int IN = 3, OUT = 3, FUEL = 6;
    private static final TagKey<Item> RAW = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "raw_materials"));
    private static final TagKey<Item> ORES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ores"));

    private final SimpleContainer items = new SimpleContainer(7) {
        @Override
        public void setChanged() {
            super.setChanged();
            MachineBlockEntity.this.setChanged();
        }
    };
    boolean running;
    int progress, burn, burnMax, jobLevel = -1;
    UUID owner;
    /** 止まった理由：0 なし / 1 終わった / 2 出口がいっぱい / 3 燃料切れ */
    int stopReason;

    public MachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModFurnaces.MACHINE_BE.get(), pos, state);
    }

    public MachineKind kind() {
        return getBlockState().getBlock() instanceof MachineBlock b ? b.kind : MachineKind.BIG_FURNACE;
    }

    public SimpleContainer items() { return items; }
    public boolean running() { return running; }
    public int progress() { return progress; }

    public boolean hasInput() {
        for (int i = 0; i < IN; i++) if (!items.getItem(i).isEmpty()) return true;
        return false;
    }

    public void start(ServerPlayer p) {
        running = true;
        progress = 0;
        stopReason = 0;
        jobLevel = kind().level(p);
        owner = p.getUUID();
        setLit(true);
        getLevel().playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.6F, 0.9F);
        setChanged();
    }

    public void stop(int reason) {
        running = false;
        progress = 0;
        stopReason = reason;
        setLit(false);
        setChanged();
    }

    private void setLit(boolean on) {
        Level lv = getLevel();
        if (lv == null) return;
        BlockState st = getBlockState();
        if (st.hasProperty(MachineBlock.LIT) && st.getValue(MachineBlock.LIT) != on) lv.setBlock(worldPosition, st.setValue(MachineBlock.LIT, on), 3);
    }

    /** 焼いた結果（1個ぶん、多めに取れる分は含まない）。焼けない物は空 */
    public static ItemStack result(ServerLevel level, MachineKind kind, ItemStack in) {
        if (in.isEmpty()) return ItemStack.EMPTY;
        if (kind == MachineKind.COKE_OVEN) {
            if (in.is(Items.COAL) || in.is(Items.CHARCOAL)) return new ItemStack(ModFurnaces.COKE.get());
            if (in.is(Items.COAL_BLOCK)) return new ItemStack(ModFurnaces.COKE_BLOCK_ITEM.get());
            return ItemStack.EMPTY;
        }
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(in.copyWithCount(1)), level)
                .map(r -> r.value().getResultItem(level.registryAccess()).copy()).orElse(ItemStack.EMPTY);
    }

    /** 多めに取れる物か（大きい炉は鉱石・原石だけ、コークス炉は全部） */
    private static boolean bonusApplies(MachineKind kind, ItemStack in) {
        return kind == MachineKind.COKE_OVEN || in.is(RAW) || in.is(ORES) || in.is(Items.ANCIENT_DEBRIS);
    }

    public static void serverTick(Level lv, BlockPos pos, BlockState state, MachineBlockEntity be) {
        if (!be.running || !(lv instanceof ServerLevel sl)) return;
        MachineKind kind = be.kind();
        int slot = -1;
        ItemStack out = ItemStack.EMPTY;
        for (int i = 0; i < IN; i++) {
            ItemStack st = be.items.getItem(i);
            if (st.isEmpty()) continue;
            out = result(sl, kind, st);
            if (!out.isEmpty()) { slot = i; break; }
        }
        if (slot < 0) { be.stop(1); return; }
        // 燃料
        if (kind.needsFuel && be.burn <= 0) {
            ItemStack f = be.items.getItem(FUEL);
            int t = f.isEmpty() ? 0 : f.getBurnTime(RecipeType.SMELTING);
            if (t <= 0) { be.stop(3); return; }
            ItemStack rem = f.getCraftingRemainingItem();
            f.shrink(1);
            if (f.isEmpty() && !rem.isEmpty()) be.items.setItem(FUEL, rem);
            be.burn = be.burnMax = t;
        }
        if (kind.needsFuel) be.burn--;
        be.progress++;
        if (be.progress % 20 == 0) lv.playSound(null, pos, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.6F, 1.0F);
        if (be.progress < kind.time) { be.setChanged(); return; }
        ItemStack in = be.items.getItem(slot);
        int n = out.getCount();
        if (bonusApplies(kind, in)) {
            double total = n * kind.bonus(be.jobLevel);
            n = (int) Math.floor(total);
            if (lv.random.nextDouble() < total - n) n++;
        }
        ItemStack made = out.copyWithCount(n);
        if (!be.insert(made)) { be.progress = kind.time; be.stop(2); return; }
        in.shrink(1);
        be.items.setItem(slot, in);
        be.progress = 0;
        lv.playSound(null, pos, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.5F, 1.2F);
        ServerPlayer p = be.owner == null ? null : sl.getServer().getPlayerList().getPlayer(be.owner);
        if (p != null) {
            String action = kind == MachineKind.BIG_FURNACE ? "smith" : "coke";
            JobService.onAction(p, rule -> rule.action().equals(action), bonusApplies(kind, out.isEmpty() ? in : in) ? 1.0 : 0.5);
        }
        be.setChanged();
    }

    /** 出口に全部入るなら入れて true（入らなければ何もしない） */
    private boolean insert(ItemStack st) {
        ItemStack[] sim = new ItemStack[OUT];
        for (int i = 0; i < OUT; i++) sim[i] = items.getItem(IN + i).copy();
        ItemStack s = st.copy();
        for (int i = 0; i < OUT && !s.isEmpty(); i++) {
            if (!sim[i].isEmpty() && ItemStack.isSameItemSameComponents(sim[i], s)) {
                int m = Math.min(s.getCount(), sim[i].getMaxStackSize() - sim[i].getCount());
                if (m > 0) { sim[i].grow(m); s.shrink(m); }
            }
        }
        for (int i = 0; i < OUT && !s.isEmpty(); i++) {
            if (sim[i].isEmpty()) { sim[i] = s.copy(); s.setCount(0); }
        }
        if (!s.isEmpty()) return false;
        for (int i = 0; i < OUT; i++) items.setItem(IN + i, sim[i]);
        return true;
    }

    public List<Integer> data() {
        return List.of(progress, running ? 1 : 0, burn, burnMax, stopReason);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items.getItems(), registries);
        tag.putBoolean("running", running);
        tag.putInt("progress", progress);
        tag.putInt("burn", burn);
        tag.putInt("burnMax", burnMax);
        tag.putInt("jobLevel", jobLevel);
        if (owner != null) tag.putUUID("owner", owner);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clearContent();
        ContainerHelper.loadAllItems(tag, items.getItems(), registries);
        running = tag.getBoolean("running");
        progress = tag.getInt("progress");
        burn = tag.getInt("burn");
        burnMax = tag.getInt("burnMax");
        jobLevel = tag.contains("jobLevel") ? tag.getInt("jobLevel") : -1;
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    }
}
