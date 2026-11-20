package com.chigusa.jsncraft.card;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 装備用バインダー：カードを6枚まで入れて、Curios の「カードバインダー」の枠につけると能力が上がる（同じカードは1枚まで） */
public class CardBinderItem extends Item implements ICurioItem {
    public static final int SLOTS = 6;

    public CardBinderItem(Properties p) {
        super(p);
    }

    public static List<String> cards(ItemStack s) {
        List<String> out = new ArrayList<>();
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        if (d == null) return out;
        ListTag l = d.copyTag().getList("cards", Tag.TAG_STRING);
        for (int i = 0; i < l.size(); i++) if (Cards.get(l.getString(i)) != null) out.add(l.getString(i));
        return out;
    }

    public static void setCards(ItemStack s, List<String> cards) {
        CompoundTag t = s.has(DataComponents.CUSTOM_DATA) ? s.get(DataComponents.CUSTOM_DATA).copyTag() : new CompoundTag();
        ListTag l = new ListTag();
        for (String c : cards) l.add(StringTag.valueOf(c));
        t.put("cards", l);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    /** 能力ごとの合計 */
    public static Map<String, Double> totals(ItemStack s) {
        Map<String, Double> m = new LinkedHashMap<>();
        for (String id : cards(s)) {
            CardList.Card c = Cards.get(id);
            m.merge(c.stat(), CardStats.amount(c), Double::sum);
        }
        return m;
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext ctx, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> map = LinkedHashMultimap.create();
        int n = 0;
        for (var e : totals(stack).entrySet()) {
            CardStats.Stat st = CardStats.map().get(e.getKey());
            if (st == null) continue;
            map.put(st.attr(), new AttributeModifier(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "/card_" + (n++)), e.getValue(), st.op()));
        }
        return map;
    }

    @Override
    public boolean canEquipFromUse(SlotContext ctx, ItemStack stack) {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide && ModCards.openBinder != null) ModCards.openBinder.accept(hand);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        List<String> c = cards(stack);
        tip.add(Component.literal("§7カード " + c.size() + " / " + SLOTS + "枚（右クリックで出し入れ）"));
        for (var e : totals(stack).entrySet()) tip.add(Component.literal("§a" + CardStats.describe(e.getKey(), e.getValue())));
        tip.add(Component.literal("§8Curiosの「カードバインダー」の枠につけると効果が出るよ"));
    }
}
