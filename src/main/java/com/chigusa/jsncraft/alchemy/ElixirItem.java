package com.chigusa.jsncraft.alchemy;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 調合した薬。飲む（ゆっくり）／投げる／粉塵（周りの仲間みんな）の3つの形がある。 */
public class ElixirItem extends Item {
    private final int type;

    public ElixirItem(int type) {
        super(new Item.Properties().stacksTo(16));
        this.type = type;
    }

    public Elixirs.Type type() {
        return Elixirs.TYPES.get(type);
    }

    @Override
    public Component getName(ItemStack stack) {
        Elixirs.Type t = type();
        int form = ElixirData.form(stack);
        String name = form == ElixirData.DUST ? t.dustName() : form == ElixirData.THROW ? t.name() + "（投）" : t.name();
        return Component.literal(name);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return ElixirData.form(stack) == ElixirData.DUST ? 20 : 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return ElixirData.form(stack) == ElixirData.DUST ? UseAnim.BOW : UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ElixirData.form(stack) == ElixirData.THROW) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.5F, 0.6F);
            if (!level.isClientSide) {
                ThrownElixir e = new ThrownElixir(level, player);
                e.setItem(stack.copyWithCount(1));
                e.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.6F, 1.0F);
                level.addFreshEntity(e);
            }
            player.getCooldowns().addCooldown(this, 10);
            stack.consume(1, player);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ElixirData d = ElixirData.of(stack);
        if (entity instanceof ServerPlayer sp) {
            if (d.form() == ElixirData.DUST) {
                var lv = sp.serverLevel();
                double r = ElixirEffects.dustRadius(sp);
                for (ServerPlayer p : ElixirEffects.around(lv, sp.position(), r)) ElixirEffects.apply(p, type(), d, 0.75);
                int c = type().color();
                lv.sendParticles(new net.minecraft.core.particles.DustParticleOptions(
                        new org.joml.Vector3f(((c >> 16) & 255) / 255F, ((c >> 8) & 255) / 255F, (c & 255) / 255F), 2.0F),
                        sp.getX(), sp.getY() + 1.2, sp.getZ(), 120, r * 0.4, 0.8, r * 0.4, 0);
                com.chigusa.jsncraft.vfx.Vfx.ring(lv, sp.position().add(0, 0.1, 0), (float) r, 16, com.chigusa.jsncraft.vfx.Vfx.GREEN);
                lv.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.4F);
                lv.playSound(null, sp.blockPosition(), SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 0.8F, 1.6F);
            } else {
                ElixirEffects.apply(sp, type(), d, 1.0);
            }
        }
        if (entity instanceof Player p && !p.getAbilities().instabuild) {
            stack.shrink(1);
            if (d.form() == ElixirData.DRINK) {
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                if (stack.isEmpty()) return bottle;
                if (!p.getInventory().add(bottle)) p.drop(bottle, false);
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        Elixirs.Type t = type();
        ElixirData d = ElixirData.of(stack);
        tip.add(Component.literal("★".repeat(d.quality()) + "☆".repeat(5 - d.quality())).withStyle(ChatFormatting.GOLD));
        double power = d.power() * d.qualityMult(), time = d.time() * d.qualityMult();
        if (d.form() == ElixirData.THROW) power *= 0.8;
        if (d.form() == ElixirData.DUST) power *= 0.75;
        for (String line : Elixirs.describe(t, power, time)) tip.add(Component.literal(line).withStyle(ChatFormatting.BLUE));
        for (String ex : d.extras()) {
            Elixirs.Additive a = Elixirs.additive(ex);
            if (a != null) tip.add(Component.literal("＋" + a.desc()).withStyle(ChatFormatting.DARK_AQUA));
        }
        if (d.form() == ElixirData.THROW) tip.add(Component.literal("右クリックで投げる（当たった周り3ブロックの仲間に効く）").withStyle(ChatFormatting.GRAY));
        if (d.form() == ElixirData.DUST) tip.add(Component.literal("使うと周りの仲間みんなに効く").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return ElixirData.of(stack).quality() >= 5;
    }
}
