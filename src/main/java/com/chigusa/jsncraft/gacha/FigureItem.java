package com.chigusa.jsncraft.gacha;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** フィギュア：置くと台の上に小さなモンスターが立つ。どのモンスターかは custom_data の "figure" */
public class FigureItem extends BlockItem {
    public FigureItem(Block block, Properties props) {
        super(block, props);
    }

    public static ItemStack of(String entityId) {
        ItemStack s = new ItemStack(ModGacha.FIGURE_ITEM.get());
        CompoundTag t = new CompoundTag();
        t.putString("figure", entityId);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return s;
    }

    public static String entityId(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d == null ? "" : d.copyTag().getString("figure");
    }

    public static EntityType<?> type(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(rl) ? null : BuiltInRegistries.ENTITY_TYPE.get(rl);
    }

    @Override
    public Component getName(ItemStack stack) {
        EntityType<?> t = type(entityId(stack));
        return t == null ? super.getName(stack) : Component.literal("フィギュア：").append(t.getDescription());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("§7置いて飾れるよ（ガチャガチャの景品）"));
    }
}
