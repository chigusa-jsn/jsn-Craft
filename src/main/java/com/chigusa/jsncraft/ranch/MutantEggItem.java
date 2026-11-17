package com.chigusa.jsncraft.ranch;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;
import java.util.function.Supplier;

/** 突然変異の動物のスポーンエッグ（クリエイティブ用）。★3 の突然変異の動物が出る。 */
public class MutantEggItem extends Item {
    private final Mutant mutant;
    private final Supplier<? extends EntityType<? extends Animal>> type;
    public final int base, spots;

    public MutantEggItem(Mutant mutant, Supplier<? extends EntityType<? extends Animal>> type, int base, int spots) {
        super(new Item.Properties());
        this.mutant = mutant;
        this.type = type;
        this.base = base;
        this.spots = spots;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        Animal a = type.get().spawn(level, ctx.getItemInHand(), ctx.getPlayer(), pos, MobSpawnType.SPAWN_EGG, true, false);
        if (a != null) {
            RanchService.setRank(a, 3);
            RanchService.setMutant(a, mutant);
            if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("✦" + mutant.label + "（★3）が出てくる").withStyle(ChatFormatting.GOLD));
        for (String p : mutant.perks) tip.add(Component.literal(p).withStyle(ChatFormatting.GRAY));
    }
}
