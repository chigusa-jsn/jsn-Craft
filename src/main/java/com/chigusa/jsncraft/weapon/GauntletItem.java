package com.chigusa.jsncraft.weapon;

import com.chigusa.jsncraft.stat.ModAttributes;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * 拳闘士の籠手（両手ひと組、6段階）。キュリオスの「籠手」の枠につける。
 * 素手で殴ると punch だけダメージが増え、左右交互に拳が出る。
 */
public class GauntletItem extends Item implements ICurioItem {

    public record Spec(int tier, String id, String name, int uses, float punch, double skill, double armor, Rarity rarity, boolean fireproof) {}

    public static final List<Spec> SPECS = List.of(
            new Spec(1, "leather_gauntlets", "革の籠手", 300, 2.0F, 0.03, 0, Rarity.COMMON, false),
            new Spec(2, "iron_gauntlets", "鉄の籠手", 600, 3.0F, 0.06, 1, Rarity.COMMON, false),
            new Spec(3, "obsidian_gauntlets", "黒曜石の籠手", 1600, 4.5F, 0.09, 2, Rarity.UNCOMMON, false),
            new Spec(4, "netherite_gauntlets", "ネザライトの籠手", 2100, 6.0F, 0.12, 3, Rarity.RARE, true),
            new Spec(5, "moon_silver_gauntlets", "月銀の籠手", 2600, 8.0F, 0.16, 3, Rarity.RARE, true),
            new Spec(6, "star_silver_gauntlets", "星銀の籠手", 3200, 10.5F, 0.20, 4, Rarity.EPIC, true));

    private final Spec spec;

    public GauntletItem(Spec spec) {
        super(props(spec));
        this.spec = spec;
    }

    private static Properties props(Spec s) {
        Properties p = new Properties().durability(s.uses()).rarity(s.rarity());
        if (s.fireproof()) p = p.fireResistant();
        return p;
    }

    public Spec spec() {
        return spec;
    }

    /** はめている籠手（なければ空） */
    public static ItemStack equipped(LivingEntity e) {
        return CuriosApi.getCuriosInventory(e).flatMap(h -> h.findFirstCurio(st -> st.getItem() instanceof GauntletItem))
                .map(r -> r.stack()).orElse(ItemStack.EMPTY);
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> map = LinkedHashMultimap.create();
        map.put(ModAttributes.SKILL_DAMAGE, new AttributeModifier(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "/skill"), spec.skill(), AttributeModifier.Operation.ADD_VALUE));
        if (spec.armor() > 0) {
            map.put(Attributes.ARMOR, new AttributeModifier(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "/armor"), spec.armor(), AttributeModifier.Operation.ADD_VALUE));
        }
        return map;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("拳闘士の籠手（" + spec.tier() + "段目）・両手ひと組").withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal("キュリオスの「籠手」の枠につける").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("素手のパンチ +" + spec.punch() + "・スキルのダメージ +" + Math.round(spec.skill() * 100) + "%"
                + (spec.armor() > 0 ? "・防御力 +" + (int) spec.armor() : "")).withStyle(ChatFormatting.BLUE));
        lines.add(Component.literal("素手で殴ると、左右の拳が交互に出る").withStyle(ChatFormatting.BLUE));
        if (spec.tier() < 6) lines.add(Component.literal("鍛冶台で鍛冶型を使うと次の段へ").withStyle(ChatFormatting.DARK_GRAY));
    }
}
