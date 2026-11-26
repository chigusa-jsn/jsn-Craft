package com.chigusa.jsncraft.tools;

import com.chigusa.jsncraft.job.JobLevels;
import com.chigusa.jsncraft.job.JobService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 釣り竿カスタム台：枠0 に竿、枠1〜5 に部品（竿・リール・ルアー・ウキ・ライン）。釣り師が「取り付ける」。 */
public class RodTableBlockEntity extends BlockEntity {
    private final SimpleContainer items = new SimpleContainer(6) {
        @Override
        public void setChanged() {
            super.setChanged();
            RodTableBlockEntity.this.setChanged();
        }
    };

    public RodTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModTools.ROD_TABLE_BE.get(), pos, state);
    }

    public SimpleContainer items() { return items; }

    /** 部品の段階の上限：釣り師 Lv0 で2段目、Lv20 で3段目、Lv40（または網主）で4段目 */
    public static int maxGrade(int lv) {
        if (lv < 0) return 0;
        return lv >= 40 ? 4 : lv >= 20 ? 3 : 2;
    }

    public boolean attach(ServerPlayer p) {
        ItemStack rod = items.getItem(0);
        int lv = JobLevels.angler(p);
        if (lv < 0 || !(rod.getItem() instanceof FishingRodItem)) return false;
        int max = maxGrade(lv);
        int done = 0;
        for (RodPart part : RodPart.values()) {
            ItemStack st = items.getItem(1 + part.ordinal());
            if (!(st.getItem() instanceof RodPartItem pi) || pi.part != part) continue;
            if (pi.grade > max) {
                p.displayClientMessage(Component.literal(part.names[pi.grade - 1] + "は、釣り師のレベルが足りなくて付けられない").withStyle(ChatFormatting.RED), false);
                continue;
            }
            int old = RodParts.grade(rod, part);
            if (old > 0) {
                ItemStack back = RodParts.partItem(part, old);
                if (!p.getInventory().add(back)) p.drop(back, false);
            }
            RodParts.set(rod, part, pi.grade);
            st.shrink(1);
            done++;
        }
        if (done == 0) return false;
        setChanged();
        p.serverLevel().playSound(null, worldPosition, SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.BLOCKS, 0.8F, 1.3F);
        p.displayClientMessage(Component.literal("部品を " + done + " つ取り付けた！").withStyle(ChatFormatting.GREEN), true);
        JobService.onAction(p, rule -> rule.action().equals("rod_build"), done);
        return true;
    }

    public boolean detach(ServerPlayer p) {
        ItemStack rod = items.getItem(0);
        if (JobLevels.angler(p) < 0 || !(rod.getItem() instanceof FishingRodItem)) return false;
        int n = 0;
        for (RodPart part : RodPart.values()) {
            int g = RodParts.grade(rod, part);
            if (g <= 0) continue;
            ItemStack back = RodParts.partItem(part, g);
            if (!p.getInventory().add(back)) p.drop(back, false);
            RodParts.set(rod, part, 0);
            n++;
        }
        if (n > 0) {
            setChanged();
            p.serverLevel().playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        return n > 0;
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
