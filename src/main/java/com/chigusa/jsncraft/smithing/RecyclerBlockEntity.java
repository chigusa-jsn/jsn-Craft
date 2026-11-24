package com.chigusa.jsncraft.smithing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 分解機：枠0〜5 が入れる所、6〜11 が出てくる所。鍛冶師がスイッチを入れると、1個ずつガチャガチャ分解する。 */
public class RecyclerBlockEntity extends BlockEntity {
    public static final int IN = 6, OUT = 6, TIME = 40;

    private final SimpleContainer items = new SimpleContainer(IN + OUT) {
        @Override
        public void setChanged() {
            super.setChanged();
            RecyclerBlockEntity.this.setChanged();
        }
    };
    boolean running;
    int progress;
    int smithLevel;
    UUID owner;
    /** 止まった理由：0 なし / 1 終わった / 2 出口がいっぱい */
    int stopReason;

    public RecyclerBlockEntity(BlockPos pos, BlockState state) {
        super(ModSmithing.RECYCLER_BE.get(), pos, state);
    }

    public SimpleContainer items() {
        return items;
    }

    public boolean running() {
        return running;
    }

    public int progress() {
        return progress;
    }

    public boolean hasInput() {
        for (int i = 0; i < IN; i++) if (!items.getItem(i).isEmpty()) return true;
        return false;
    }

    public void start(ServerPlayer player) {
        running = true;
        progress = 0;
        stopReason = 0;
        smithLevel = SmithingService.smithLevel(player);
        owner = player.getUUID();
        setLit(true);
        level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.6F, 0.7F);
        setChanged();
    }

    public void stop(int reason) {
        running = false;
        progress = 0;
        stopReason = reason;
        setLit(false);
        if (level != null) level.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.6F, 0.7F);
        setChanged();
    }

    private void setLit(boolean on) {
        if (level == null) return;
        BlockState st = getBlockState();
        if (st.hasProperty(RecyclerBlock.LIT) && st.getValue(RecyclerBlock.LIT) != on) level.setBlock(worldPosition, st.setValue(RecyclerBlock.LIT, on), 3);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RecyclerBlockEntity be) {
        if (!be.running || !(level instanceof ServerLevel sl)) return;
        int slot = -1;
        for (int i = 0; i < IN; i++) if (!be.items.getItem(i).isEmpty()) { slot = i; break; }
        if (slot < 0) { be.stop(1); return; }
        ItemStack in = be.items.getItem(slot);
        // 分解できない物は、そのまま出口へ
        if (RecyclerService.passThrough(sl, in)) {
            if (!be.insert(List.of(in.copy()))) { be.stop(2); return; }
            be.items.setItem(slot, ItemStack.EMPTY);
            be.progress = 0;
            return;
        }
        be.progress++;
        var r = level.random;
        if (be.progress % 6 == 0) {
            float pitch = 0.9F + r.nextFloat() * 0.8F;
            switch (r.nextInt(3)) {
                case 0 -> level.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.15F, pitch + 0.4F);
                case 1 -> level.playSound(null, pos, SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, 0.7F, pitch);
                default -> level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.35F, pitch);
            }
            sl.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 4, 0.2, 0.05, 0.2, 0.1);
        }
        if (be.progress < TIME) { be.setChanged(); return; }
        ItemStack one = in.copyWithCount(1);
        List<ItemStack> result = RecyclerService.salvage(sl, one, Math.max(0, be.smithLevel), r);
        List<ItemStack> copy = new ArrayList<>();
        for (ItemStack s : result) copy.add(s.copy());
        if (!be.insert(copy)) { be.progress = TIME; be.stop(2); return; }
        in.shrink(1);
        be.items.setItem(slot, in);
        be.progress = 0;
        level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.25F, 1.5F);
        level.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 0.5F, 0.8F);
        sl.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 0.2, 0.1, 0.2, 0.01);
        ServerPlayer p = be.owner == null ? null : sl.getServer().getPlayerList().getPlayer(be.owner);
        if (p != null) {
            int xp = 1 + (one.getMaxDamage() > 0 ? 2 : 0) + SmithData.plus(one);
            com.chigusa.jsncraft.job.JobService.onAction(p, rule -> rule.action().equals("smith"), xp);
        }
        be.setChanged();
    }

    /** 出口に全部入るなら入れて true */
    private boolean insert(List<ItemStack> stacks) {
        ItemStack[] sim = new ItemStack[OUT];
        for (int i = 0; i < OUT; i++) sim[i] = items.getItem(IN + i).copy();
        for (ItemStack st : stacks) {
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
        }
        for (int i = 0; i < OUT; i++) items.setItem(IN + i, sim[i]);
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items.getItems(), registries);
        tag.putBoolean("running", running);
        tag.putInt("progress", progress);
        tag.putInt("smithLevel", smithLevel);
        if (owner != null) tag.putUUID("owner", owner);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clearContent();
        ContainerHelper.loadAllItems(tag, items.getItems(), registries);
        running = tag.getBoolean("running");
        progress = tag.getInt("progress");
        smithLevel = tag.getInt("smithLevel");
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    }
}
