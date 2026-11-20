package com.chigusa.jsncraft.gacha;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 置いたフィギュア：どのモンスターか */
public class FigureBlockEntity extends BlockEntity {
    public String entityId = "minecraft:creeper";

    public FigureBlockEntity(BlockPos pos, BlockState state) {
        super(ModGacha.FIGURE_BE.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        tag.putString("figure", entityId);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        if (tag.contains("figure")) entityId = tag.getString("figure");
    }

    // アイテム → 置いたとき
    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        CustomData d = input.get(DataComponents.CUSTOM_DATA);
        if (d != null && d.copyTag().contains("figure")) entityId = d.copyTag().getString("figure");
    }

    // 壊したとき → アイテムに
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder b) {
        CompoundTag t = new CompoundTag();
        t.putString("figure", entityId);
        b.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("figure");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider reg) {
        return saveWithoutMetadata(reg);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
