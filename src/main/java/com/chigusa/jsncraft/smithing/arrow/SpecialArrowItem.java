package com.chigusa.jsncraft.smithing.arrow;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SpecialArrowItem extends ArrowItem {
    private final ArrowKind kind;

    public SpecialArrowItem(ArrowKind kind, Properties props) {
        super(props);
        this.kind = kind;
    }

    public ArrowKind kind() {
        return kind;
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, @Nullable ItemStack weapon) {
        return new SpecialArrow(level, shooter, ammo.copyWithCount(1), weapon, kind);
    }

    @Override
    public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        SpecialArrow a = new SpecialArrow(level, pos.x(), pos.y(), pos.z(), stack.copyWithCount(1), kind);
        a.pickup = AbstractArrow.Pickup.ALLOWED;
        return a;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal(kind.desc).withStyle(ChatFormatting.GRAY));
        tip.add(Quality.line(stack));
        tip.add(Component.literal("強化した弓で撃つと効果が強くなる").withStyle(ChatFormatting.DARK_GRAY));
    }
}
