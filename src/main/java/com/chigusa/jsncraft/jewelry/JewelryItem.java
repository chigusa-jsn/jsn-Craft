package com.chigusa.jsncraft.jewelry;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 装飾品（Curios の枠につける）。
 * ふつうの装飾品：種類（枠）×台座。宝石と品質はアイテムのデータに入る。
 * 特別な装飾品：決まった能力と、特別な効果を持つ。
 */
public class JewelryItem extends Item implements ICurioItem {
    private final Jewels.Kind kind;
    private final Jewels.Tier tier;      // 特別な装飾品は null
    private final Jewels.Unique unique;  // ふつうの装飾品は null

    public JewelryItem(Jewels.Kind kind, Jewels.Tier tier) {
        super(new Item.Properties().stacksTo(1).rarity(tier.index() >= 5 ? Rarity.EPIC : tier.index() >= 3 ? Rarity.RARE : Rarity.COMMON));
        this.kind = kind;
        this.tier = tier;
        this.unique = null;
    }

    public JewelryItem(Jewels.Unique unique) {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
        this.kind = Jewels.KINDS.get(unique.kind());
        this.tier = null;
        this.unique = unique;
    }

    public Jewels.Kind kind() { return kind; }
    public Jewels.Tier tier() { return tier; }
    public Jewels.Unique unique() { return unique; }

    public JewelData data(ItemStack stack) {
        return JewelData.of(stack, tier == null ? 0 : tier.sockets());
    }

    /** つけたときの能力（能力値と倍率） */
    public List<Map.Entry<Jewels.Stat, Double>> stats(ItemStack stack) {
        List<Map.Entry<Jewels.Stat, Double>> out = new ArrayList<>();
        if (unique != null) {
            for (Jewels.Stat s : unique.stats()) out.add(Map.entry(s, 1.0));
            return out;
        }
        JewelData d = data(stack);
        double m = tier.power() * d.qualityMult();
        out.add(Map.entry(kind.base(), m));
        for (String id : d.gems()) {
            Jewels.Gem g = Jewels.gem(id);
            if (g == null) continue;
            double gm = m * (g.affinity().equals(kind.id()) ? 1.25 : 1.0);
            for (Jewels.Stat s : g.stats()) out.add(Map.entry(s, gm));
        }
        return out;
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        // 同じ能力値・同じ足し方はまとめる
        Map<String, Double> sums = new LinkedHashMap<>();
        Map<String, Jewels.Stat> keys = new LinkedHashMap<>();
        for (var e : stats(stack)) {
            Jewels.Stat s = e.getKey();
            String key = s.attr().unwrapKey().map(k -> k.location().toString()).orElse("?") + "|" + s.op().name();
            sums.merge(key, s.amount() * e.getValue(), Double::sum);
            keys.putIfAbsent(key, s);
        }
        Multimap<Holder<Attribute>, AttributeModifier> map = LinkedHashMultimap.create();
        int n = 0;
        for (var e : sums.entrySet()) {
            Jewels.Stat s = keys.get(e.getKey());
            ResourceLocation mid = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "/jsn_" + (n++));
            map.put(s.attr(), new AttributeModifier(mid, e.getValue(), s.op()));
        }
        return map;
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (unique == null) return;
        LivingEntity e = slotContext.entity();
        if (e.level().isClientSide) return;
        switch (unique.special()) {
            case "fall" -> e.fallDistance = 0;
            case "night" -> {
                if (e.tickCount % 20 == 0) {
                    MobEffectInstance cur = e.getEffect(MobEffects.NIGHT_VISION);
                    if (cur == null || (cur.getDuration() < 240 && cur.getDuration() >= 0))
                        e.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, true));
                }
            }
            case "water" -> {
                if (e.tickCount % 20 == 0 && e.isInWater())
                    e.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 100, 0, true, false, true));
            }
            default -> {
            }
        }
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        if (unique != null) {
            tip.add(Component.literal("特別な" + kind.name()).withStyle(ChatFormatting.LIGHT_PURPLE));
            if (!unique.specialText().isEmpty()) tip.add(Component.literal("✦" + unique.specialText()).withStyle(ChatFormatting.GOLD));
            return;
        }
        JewelData d = data(stack);
        tip.add(Component.literal("★".repeat(d.quality()) + "☆".repeat(5 - d.quality())).withStyle(ChatFormatting.GOLD));
        StringBuilder sb = new StringBuilder("宝石：");
        for (int i = 0; i < d.sockets(); i++) {
            if (i > 0) sb.append("・");
            Jewels.Gem g = i < d.gems().size() ? Jewels.gem(d.gems().get(i)) : null;
            sb.append(g == null ? "◇空き" : "◆" + g.name());
        }
        tip.add(Component.literal(sb.toString()).withStyle(ChatFormatting.AQUA));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return unique != null;
    }
}
