package com.chigusa.jsncraft.capture;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/**
 * 捕獲の杖：動物を右クリックすると「捕獲玉」に入れて持ち歩ける（ワープや引っ越しで、リードがほどけない）。
 * 捕獲玉を地面に向けて使うと、そこに出てくる（名前・子ども・毛の色・ランク・突然変異など、そのまま）。
 * ほかの人のペットは捕まえられない。
 */
public class CaptureWandItem extends Item {
    public CaptureWandItem(Properties p) {
        super(p);
    }

    public static boolean capturable(Player p, LivingEntity e) {
        if (!(e instanceof Animal) || !e.isAlive()) return false;
        if (e.isVehicle()) return false;
        if (e instanceof OwnableEntity own && own.getOwnerUUID() != null && !own.getOwnerUUID().equals(p.getUUID())) return false;
        return true;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack wand, Player player, LivingEntity target, InteractionHand hand) {
        if (!capturable(player, target)) {
            if (!player.level().isClientSide) player.displayClientMessage(Component.literal("この生き物は捕まえられない").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.FAIL;
        }
        if (!(player.level() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        Animal a = (Animal) target;
        if (a.isLeashed()) a.dropLeash(true, true);
        CompoundTag data = new CompoundTag();
        a.saveWithoutId(data);
        data.remove("UUID");
        data.remove("Pos");
        data.remove("Motion");
        String type = EntityType.getKey(a.getType()).toString();
        ItemStack orb = new ItemStack(CaptureItems.ORB.get());
        CompoundTag t = new CompoundTag();
        t.putString("type", type);
        t.put("data", data);
        CustomData.update(DataComponents.CUSTOM_DATA, orb, root -> root.put("jsn_capture", t));
        if (a.hasCustomName()) orb.set(DataComponents.CUSTOM_NAME, Component.literal(a.getCustomName().getString()).withStyle(ChatFormatting.AQUA));
        level.sendParticles(ParticleTypes.PORTAL, a.getX(), a.getY() + 0.5, a.getZ(), 30, 0.4, 0.5, 0.4, 0.3);
        level.playSound(null, a.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.6F);
        a.discard();
        if (!player.getInventory().add(orb)) player.drop(orb, false);
        wand.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
        player.displayClientMessage(Component.literal("捕まえた！ ").withStyle(ChatFormatting.GREEN).append(a.getType().getDescription()), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("動物を右クリックで捕獲玉に入れる").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("捕獲玉を地面に向けて使うと出てくる").withStyle(ChatFormatting.DARK_GRAY));
    }
}
