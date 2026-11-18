package com.chigusa.jsncraft.weapon;

import com.chigusa.jsncraft.npc.ModEntities;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.List;

/** 職業武器（6段階）と、全職業共通の鍛冶型。 */
public final class ModWeapons {

    public static final List<DeferredItem<StaffItem>> STAFFS = new ArrayList<>();

    /** 2段目・3段目の鍛冶型（作業台で作れる） */
    public static final DeferredItem<TemplateItem> TEMPLATE_2 = ModEntities.ITEMS.register("job_template_2",
            () -> new TemplateItem(new Item.Properties(), "職業武器 1段目 → 2段目"));
    public static final DeferredItem<TemplateItem> TEMPLATE_3 = ModEntities.ITEMS.register("job_template_3",
            () -> new TemplateItem(new Item.Properties().rarity(Rarity.UNCOMMON), "職業武器 2段目 → 3段目"));
    /** 4段目はバニラの「ネザライト強化」の鍛冶型 */
    /** 5段目・6段目（強敵や宝箱から。素材で複製できる） */
    public static final DeferredItem<TemplateItem> TEMPLATE_MOON = ModEntities.ITEMS.register("moon_silver_template",
            () -> new TemplateItem(new Item.Properties().rarity(Rarity.RARE), "職業武器 ネザライト → 月銀"));
    public static final DeferredItem<TemplateItem> TEMPLATE_STAR = ModEntities.ITEMS.register("star_silver_template",
            () -> new TemplateItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant(), "職業武器 月銀 → 星銀"));

    public static final List<DeferredItem<TemplateItem>> TEMPLATES = List.of(TEMPLATE_2, TEMPLATE_3, TEMPLATE_MOON, TEMPLATE_STAR);

    static {
        for (StaffItem.Tier t : StaffItem.TIERS) STAFFS.add(ModEntities.ITEMS.register(t.id(), () -> new StaffItem(t)));
    }

    public static void init() {}

    private ModWeapons() {}
}
