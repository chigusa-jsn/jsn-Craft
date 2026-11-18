package com.chigusa.jsncraft.weapon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** 射手の弓（6段階）。本物の弓として引け、段が上がるほど矢が強く速くなる。持つとスキルのダメージも上がる。 */
public class JobBowItem extends BowItem {

    /** damage：矢のダメージの倍率、speed：矢の速さの倍率、skill：スキルダメージ +○% */
    public record Spec(int tier, String id, String name, int uses, double damage, float speed, double skill, Rarity rarity, boolean fireproof) {}

    public static final List<Spec> SPECS = List.of(
            new Spec(1, "wood_job_bow", "木の長弓", 400, 1.0, 1.0F, 0.03, Rarity.COMMON, false),
            new Spec(2, "bone_job_bow", "骨の長弓", 600, 1.1, 1.05F, 0.06, Rarity.COMMON, false),
            new Spec(3, "blaze_job_bow", "ブレイズの長弓", 1000, 1.25, 1.1F, 0.09, Rarity.UNCOMMON, false),
            new Spec(4, "netherite_job_bow", "ネザライトの長弓", 1800, 1.4, 1.15F, 0.12, Rarity.RARE, true),
            new Spec(5, "moon_silver_job_bow", "月銀の長弓", 2400, 1.6, 1.2F, 0.16, Rarity.RARE, true),
            new Spec(6, "star_silver_job_bow", "星銀の長弓", 3200, 1.85, 1.25F, 0.20, Rarity.EPIC, true));

    private final Spec spec;

    public JobBowItem(Spec spec) {
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
    protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float velocity, float inaccuracy, float angle,
                                   @Nullable LivingEntity target) {
        if (projectile instanceof AbstractArrow arrow) arrow.setBaseDamage(arrow.getBaseDamage() * spec.damage());
        super.shootProjectile(shooter, projectile, index, velocity * spec.speed(), inaccuracy, angle, target);
    }

    @Override
    public int getEnchantmentValue() {
        return 10 + spec.tier() * 2;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("射手の長弓（" + spec.tier() + "段目）").withStyle(ChatFormatting.GREEN));
        lines.add(Component.literal("矢のダメージ ×" + spec.damage() + "・矢の速さ ×" + spec.speed()).withStyle(ChatFormatting.BLUE));
        lines.add(Component.literal("持つとスキルのダメージ +" + Math.round(spec.skill() * 100) + "%").withStyle(ChatFormatting.BLUE));
        if (spec.tier() < 6) lines.add(Component.literal("鍛冶台で鍛冶型を使うと次の段へ（魔紋と強化はそのまま）").withStyle(ChatFormatting.DARK_GRAY));
    }
}
