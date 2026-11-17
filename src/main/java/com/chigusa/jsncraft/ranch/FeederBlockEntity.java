package com.chigusa.jsncraft.ranch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** エサ箱の中身（9枠）と、5秒ごとのエサやり。 */
public class FeederBlockEntity extends BlockEntity {
    private final SimpleContainer items = new SimpleContainer(9) {
        @Override
        public void setChanged() {
            super.setChanged();
            FeederBlockEntity.this.setChanged();
        }
    };

    public FeederBlockEntity(BlockPos pos, BlockState state) {
        super(ModRanch.FEEDER_BE.get(), pos, state);
    }

    public SimpleContainer items() {
        return items;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FeederBlockEntity be) {
        if (level.getGameTime() % 100 != (pos.asLong() & 63) % 100 || !(level instanceof ServerLevel sl)) return;
        // 近く（16ブロック）に獣飼いがいるときだけ繁殖させる
        ServerPlayer rancher = null;
        for (ServerPlayer p : sl.players()) {
            if (p.blockPosition().distSqr(pos) <= 256 && RanchService.isRancher(p)) {
                rancher = p;
                break;
            }
        }
        int fed = 0;
        for (Animal a : sl.getEntitiesOfClass(Animal.class, new AABB(pos).inflate(6))) {
            if (fed >= 4) break;
            int slot = foodSlot(be, a);
            if (slot < 0) continue;
            if (a.isBaby()) {
                a.ageUp(AgeableMobHelper.growSeconds(a), true);   // 子どもは早く育つ
            } else if (rancher != null && a.getAge() == 0 && a.canFallInLove() && !a.isInLove()) {
                a.setInLove(rancher);
            } else {
                continue;
            }
            be.items.getItem(slot).shrink(1);
            be.setChanged();
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, a.getX(), a.getY() + a.getBbHeight(), a.getZ(), 3, 0.3, 0.2, 0.3, 0);
            fed++;
        }
    }

    private static int foodSlot(FeederBlockEntity be, Animal a) {
        for (int i = 0; i < be.items.getContainerSize(); i++) {
            ItemStack s = be.items.getItem(i);
            if (!s.isEmpty() && a.isFood(s)) return i;
        }
        return -1;
    }

    /** 子どもにエサ：残りの1割だけ早く育てる */
    static final class AgeableMobHelper {
        static int growSeconds(Animal a) {
            return (int) (-a.getAge() / 20 * 0.1);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items.getItems(), registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clearContent();
        ContainerHelper.loadAllItems(tag, items.getItems(), registries);
    }
}
