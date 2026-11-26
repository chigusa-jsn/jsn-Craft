package com.chigusa.jsncraft.jewelry;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.job.JobLevels;
import com.chigusa.jsncraft.npc.ModEntities;
import com.chigusa.jsncraft.smithing.SmithData;
import com.chigusa.jsncraft.smithing.SmithingService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 宝珠：宝石から作る珠。防具に1つずつはめられる（はめる・外すのは細工師）。
 * 段階：小宝珠（×2）・宝珠（×4）・大宝珠（×7）。宝石の能力がその倍率で防具に付く。
 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class Orbs {
    public static final String[] GRADE_NAME = {"小宝珠", "宝珠", "大宝珠"};
    public static final double[] MULT = {2, 4, 7};
    /** [宝石の番号][段階-1] */
    public static final List<DeferredItem<OrbItem>[]> ITEMS = new ArrayList<>();
    public static final DeferredItem<OrbTweezersItem> TWEEZERS = ModEntities.ITEMS.register("orb_tweezers",
            () -> new OrbTweezersItem(new Item.Properties().durability(64)));

    static {
        for (Jewels.Gem g : Jewels.GEMS) {
            @SuppressWarnings("unchecked") DeferredItem<OrbItem>[] arr = new DeferredItem[3];
            for (int i = 1; i <= 3; i++) {
                int grade = i;
                Rarity r = i == 3 ? Rarity.EPIC : i == 2 ? Rarity.RARE : Rarity.UNCOMMON;
                arr[i - 1] = ModEntities.ITEMS.register("orb_" + g.id() + "_" + i, () -> new OrbItem(g, grade, new Item.Properties().stacksTo(16).rarity(r)));
            }
            ITEMS.add(arr);
        }
    }

    /** 細工の腕のレベル（細工師はそのレベル、宝飾師は 50 + レベル） */
    public static int jewelLevel(ServerPlayer p) {
        return JobLevels.life(p, "jeweler", "gem_master");
    }

    /** はめられる段階：細工師 Lv0 で小、Lv20 で中、Lv40（または宝飾師）で大 */
    public static int maxGrade(int lv) {
        if (lv < 0) return 0;
        return lv >= 40 ? 3 : lv >= 20 ? 2 : 1;
    }

    public static boolean socketable(ItemStack armor) {
        return SmithData.kinds(armor).contains("A");
    }

    /** 防具にはまっている宝珠（なければ null） */
    public static OrbItem orbOf(ItemStack armor) {
        CompoundTag t = armor.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound("jsn_orb");
        if (!t.contains("gem")) return null;
        Jewels.Gem g = Jewels.gem(t.getString("gem"));
        int grade = t.getInt("grade");
        if (g == null || grade < 1 || grade > 3) return null;
        return ITEMS.get(g.index())[grade - 1].get();
    }

    public static void setOrb(ItemStack armor, OrbItem orb) {
        CustomData.update(DataComponents.CUSTOM_DATA, armor, t -> {
            if (orb == null) t.remove("jsn_orb");
            else {
                CompoundTag c = new CompoundTag();
                c.putString("gem", orb.gem.id());
                c.putInt("grade", orb.grade);
                t.put("jsn_orb", c);
            }
        });
    }

    @SubscribeEvent
    public static void onModifiers(ItemAttributeModifierEvent event) {
        ItemStack st = event.getItemStack();
        OrbItem orb = orbOf(st);
        if (orb == null) return;
        EquipmentSlotGroup group = SmithingService.groupOf(st);
        int i = 0;
        for (Jewels.Stat s : orb.gem.stats()) {
            event.addModifier(s.attr(), new AttributeModifier(JsnCraft.id("orb/" + group.getSerializedName() + "_" + i++),
                    s.amount() * MULT[orb.grade - 1], s.op()), group);
        }
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, value = Dist.CLIENT)
    public static final class Client {
        @SubscribeEvent
        public static void onTooltip(ItemTooltipEvent event) {
            OrbItem orb = orbOf(event.getItemStack());
            if (orb == null) return;
            int at = Math.min(1, event.getToolTip().size());
            StringBuilder sb = new StringBuilder();
            for (Jewels.Stat s : orb.gem.stats()) sb.append(sb.length() > 0 ? "・" : "").append(Jewels.format(s, MULT[orb.grade - 1]));
            event.getToolTip().add(at, Component.literal("◈ " + orb.gem.name() + "の" + GRADE_NAME[orb.grade - 1] + "：" + sb).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    public static void init() {}

    private Orbs() {}
}
