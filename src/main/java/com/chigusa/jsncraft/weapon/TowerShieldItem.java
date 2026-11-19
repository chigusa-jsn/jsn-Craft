package com.chigusa.jsncraft.weapon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 盾騎士の大盾（6段階）。バニラの盾と同じように構えられ、持っているだけで守りが上がる。 */
public class TowerShieldItem extends ShieldItem {

    /** armor：防御力、kb：ノックバック耐性、dr：受けるダメージ -○% */
    public record Spec(int tier, String id, String name, int uses, double armor, double kb, double dr, Rarity rarity, boolean fireproof) {}

    public static final List<Spec> SPECS = List.of(
            new Spec(1, "wood_tower_shield", "木の大盾", 400, 1, 0.1, 0.02, Rarity.COMMON, false),
            new Spec(2, "iron_tower_shield", "鉄の大盾", 700, 2, 0.15, 0.03, Rarity.COMMON, false),
            new Spec(3, "diamond_tower_shield", "ダイヤの大盾", 1600, 3, 0.2, 0.05, Rarity.UNCOMMON, false),
            new Spec(4, "netherite_tower_shield", "ネザライトの大盾", 2200, 4, 0.3, 0.07, Rarity.RARE, true),
            new Spec(5, "moon_silver_tower_shield", "月銀の大盾", 2800, 5, 0.35, 0.09, Rarity.RARE, true),
            new Spec(6, "star_silver_tower_shield", "星銀の大盾", 3600, 6, 0.4, 0.12, Rarity.EPIC, true));

    private final Spec spec;

    public TowerShieldItem(Spec spec) {
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("盾騎士の大盾（" + spec.tier() + "段目）").withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal("持っているだけで 防御力 +" + (int) spec.armor() + "・ノックバック耐性 +" + Math.round(spec.kb() * 100)
                + "%・受けるダメージ -" + Math.round(spec.dr() * 100) + "%").withStyle(ChatFormatting.BLUE));
        lines.add(Component.literal("盾騎士のパッシブ「見通しの大盾」：自分の目からは盾が半透明に見える").withStyle(ChatFormatting.AQUA));
        if (spec.tier() < 6) lines.add(Component.literal("鍛冶台で鍛冶型を使うと次の段へ（魔紋と強化はそのまま）").withStyle(ChatFormatting.DARK_GRAY));
    }
}
