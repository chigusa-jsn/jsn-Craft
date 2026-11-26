package com.chigusa.jsncraft.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** 星銀のクワ：3×3 をまとめて耕す（しゃがむと1マスだけ）。 */
public class StarHoeItem extends HoeItem {
    private static boolean busy;

    public StarHoeItem(Tier tier, Properties p) {
        super(tier, p);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        InteractionResult center = super.useOn(ctx);
        if (busy || ctx.getPlayer() == null || ctx.getPlayer().isShiftKeyDown() || !center.consumesAction() || ctx.getLevel().isClientSide) return center;
        busy = true;
        try {
            BlockPos c = ctx.getClickedPos();
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos p = c.offset(dx, 0, dz);
                super.useOn(new UseOnContext(ctx.getPlayer(), ctx.getHand(), new BlockHitResult(Vec3.atCenterOf(p).add(0, 0.5, 0), Direction.UP, p, false)));
            }
        } finally {
            busy = false;
        }
        return center;
    }
}
