package com.chigusa.jsncraft.tools;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.npc.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/** 釣り竿の部品の登録と、竿に付いている部品（CUSTOM_DATA の jsn_rod）。 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class RodParts {
    public static final Map<RodPart, DeferredItem<RodPartItem>[]> ITEMS = new EnumMap<>(RodPart.class);

    static {
        for (RodPart p : RodPart.values()) {
            @SuppressWarnings("unchecked") DeferredItem<RodPartItem>[] arr = new DeferredItem[4];
            for (int g = 1; g <= 4; g++) {
                int grade = g;
                Rarity r = g >= 4 ? Rarity.EPIC : g == 3 ? Rarity.RARE : g == 2 ? Rarity.UNCOMMON : Rarity.COMMON;
                arr[g - 1] = ModEntities.ITEMS.register("rod_" + p.id + "_" + g, () -> new RodPartItem(p, grade, new Item.Properties().stacksTo(16).rarity(r)));
            }
            ITEMS.put(p, arr);
        }
    }

    public static String fmt(RodPart p, int g) {
        double v = p.amount(g);
        return p == RodPart.BLANK ? String.format(Locale.ROOT, "%.1f", v) : String.format(Locale.ROOT, "%.0f%%", v * 100);
    }

    /** 竿に付いている部品の段階（付いていなければ 0） */
    public static int grade(ItemStack rod, RodPart p) {
        return rod.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound("jsn_rod").getInt(p.id);
    }

    public static void set(ItemStack rod, RodPart p, int g) {
        CustomData.update(DataComponents.CUSTOM_DATA, rod, t -> {
            CompoundTag c = t.getCompound("jsn_rod");
            if (g <= 0) c.remove(p.id);
            else c.putInt(p.id, g);
            t.put("jsn_rod", c);
        });
    }

    public static ItemStack partItem(RodPart p, int g) {
        return g <= 0 ? ItemStack.EMPTY : new ItemStack(ITEMS.get(p)[Math.min(4, g) - 1].get());
    }

    @SubscribeEvent
    public static void onModifiers(ItemAttributeModifierEvent event) {
        ItemStack st = event.getItemStack();
        if (!(st.getItem() instanceof FishingRodItem)) return;
        for (RodPart p : RodPart.values()) {
            int g = grade(st, p);
            if (g <= 0) continue;
            event.addModifier(p.attribute(), new AttributeModifier(JsnCraft.id("rod_part/" + p.id), p.amount(g), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HAND);
            if (p == RodPart.LURE && g >= 3)
                event.addModifier(com.chigusa.jsncraft.stat.ModAttributes.FISHING_TREASURE, new AttributeModifier(JsnCraft.id("rod_part/lure_treasure"), 0.02 * (g - 2), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HAND);
        }
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, value = Dist.CLIENT)
    public static final class Client {
        @SubscribeEvent
        public static void onTooltip(ItemTooltipEvent event) {
            ItemStack st = event.getItemStack();
            if (!(st.getItem() instanceof FishingRodItem)) return;
            int at = Math.min(1, event.getToolTip().size());
            for (RodPart p : RodPart.values()) {
                int g = grade(st, p);
                if (g > 0) event.getToolTip().add(at++, Component.literal("  " + p.name + "：" + p.names[g - 1]).withStyle(ChatFormatting.AQUA));
            }
        }
    }

    public static void init() {}

    private RodParts() {}
}
