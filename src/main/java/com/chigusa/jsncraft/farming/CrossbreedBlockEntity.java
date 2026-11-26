package com.chigusa.jsncraft.farming;

import com.chigusa.jsncraft.job.JobLevels;
import com.chigusa.jsncraft.job.JobService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 交配台：同じ種を2つ入れて、農家が「交配する」を押すと、ランクの上がった種が2つできる。 */
public class CrossbreedBlockEntity extends BlockEntity {
    private final SimpleContainer items = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            CrossbreedBlockEntity.this.setChanged();
        }
    };

    public CrossbreedBlockEntity(BlockPos pos, BlockState state) {
        super(ModFarming.CROSSBREED_BE.get(), pos, state);
    }

    public SimpleContainer items() { return items; }

    /** 交配できるか：0 できる / 1 種が足りない / 2 違う種 / 3 出口が空いていない */
    public int check() {
        ItemStack a = items.getItem(0), b = items.getItem(1), out = items.getItem(2);
        if (a.isEmpty() || b.isEmpty() || !ModFarming.isSeed(a) || !ModFarming.isSeed(b)) return 1;
        if (!a.is(b.getItem())) return 2;
        if (!out.isEmpty() && (!out.is(a.getItem()) || out.getCount() + 2 > out.getMaxStackSize())) return 3;
        return 0;
    }

    public boolean breed(ServerPlayer p) {
        int lv = JobLevels.farmer(p);
        if (lv < 0 || check() != 0) return false;
        ItemStack a = items.getItem(0), b = items.getItem(1);
        int ra = Math.max(1, CropRanks.rank(a)), rb = Math.max(1, CropRanks.rank(b));
        var r = p.getRandom();
        int result = Math.max(ra, rb);
        String msg;
        if (ra >= 5 && rb >= 5 && result < CropRanks.GOLDEN) {
            if (ModFarming.evolvable(a) && r.nextDouble() < 0.25 + lv * 0.005) {
                result = CropRanks.GOLDEN;
                msg = "進化した！ 黄金の種になった！";
            } else msg = ModFarming.evolvable(a) ? "進化しなかった…（もう一度）" : "これ以上はランクが上がらない";
        } else if (ra == rb && result < 5 && r.nextDouble() < 0.45 + lv * 0.008) {
            result++;
            msg = "ランクが上がった！ " + CropRanks.label(result);
        } else {
            msg = ra == rb ? "うまく混ざらなかった…" : "高いほうのランクにそろった " + CropRanks.label(result);
        }
        ItemStack made = a.copyWithCount(2);
        CropRanks.setRank(made, result);
        a.shrink(1);
        b.shrink(1);
        ItemStack out = items.getItem(2);
        if (out.isEmpty() || !ItemStack.isSameItemSameComponents(out, made)) {
            if (!out.isEmpty()) {
                if (!p.getInventory().add(out.copy())) p.drop(out.copy(), false);
            }
            items.setItem(2, made);
        } else out.grow(2);
        setChanged();
        ServerLevel level = p.serverLevel();
        boolean up = result > Math.max(ra, rb) || result != ra;
        level.playSound(null, worldPosition, up ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 1.0F, result >= CropRanks.GOLDEN ? 0.8F : 1.4F);
        level.sendParticles(result >= CropRanks.GOLDEN ? ParticleTypes.END_ROD : ParticleTypes.HAPPY_VILLAGER,
                worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, up ? 20 : 6, 0.3, 0.2, 0.3, 0.02);
        p.displayClientMessage(Component.literal(msg).withStyle(result >= CropRanks.GOLDEN ? ChatFormatting.GOLD : up ? ChatFormatting.GREEN : ChatFormatting.GRAY), true);
        JobService.onAction(p, rule -> rule.action().equals("breed"), result >= 5 ? 2.0 : 1.0);
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        ContainerHelper.saveAllItems(tag, items.getItems(), reg);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        items.clearContent();
        ContainerHelper.loadAllItems(tag, items.getItems(), reg);
    }
}
