package com.chigusa.jsncraft.capture;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;
import java.util.Optional;

/** 動物の入った捕獲玉。地面に向けて使うと、その場に出てくる。 */
public class CapturedOrbItem extends Item {
    public CapturedOrbItem(Properties p) {
        super(p);
    }

    private static CompoundTag tag(ItemStack st) {
        return st.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound("jsn_capture");
    }

    private static Optional<EntityType<?>> type(ItemStack st) {
        return EntityType.byString(tag(st).getString("type"));
    }

    @Override
    public Component getName(ItemStack stack) {
        return type(stack).map(t -> (Component) Component.literal("捕獲玉：").append(t.getDescription())).orElse(super.getName(stack));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        ItemStack st = ctx.getItemInHand();
        CompoundTag t = tag(st);
        Optional<EntityType<?>> type = type(st);
        if (type.isEmpty()) return InteractionResult.FAIL;
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        Entity e = type.get().create(level);
        if (e == null) return InteractionResult.FAIL;
        e.load(t.getCompound("data"));
        e.setUUID(net.minecraft.util.Mth.createInsecureUUID(level.random));
        e.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, ctx.getPlayer() != null ? ctx.getPlayer().getYRot() + 180 : 0, 0);
        if (!level.noCollision(e)) {
            if (ctx.getPlayer() != null) ctx.getPlayer().displayClientMessage(Component.literal("ここは狭くて出せない").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        level.addFreshEntity(e);
        level.sendParticles(ParticleTypes.PORTAL, e.getX(), e.getY() + 0.5, e.getZ(), 30, 0.4, 0.5, 0.4, 0.3);
        level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.2F);
        if (ctx.getPlayer() == null || !ctx.getPlayer().isCreative()) st.shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        CompoundTag d = tag(stack).getCompound("data");
        if (d.getInt("Age") < 0) tip.add(Component.literal("子ども").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("地面に向けて使うと出てくる").withStyle(ChatFormatting.DARK_AQUA));
    }
}
