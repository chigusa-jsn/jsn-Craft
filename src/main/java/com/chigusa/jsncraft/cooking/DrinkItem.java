package com.chigusa.jsncraft.cooking;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** お酒・飲み物。飲むと空き瓶が戻る。料理と同じく★とバフがあり、置くこともできる。 */
public class DrinkItem extends DishItem {

    public DrinkItem() {
        super(new Item.Properties().stacksTo(16)
                .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2F).alwaysEdible().fast().build()));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public SoundEvent getEatingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack rest = super.finishUsingItem(stack, level, entity);
        if (entity instanceof Player p && !p.getAbilities().instabuild) {
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (rest.isEmpty()) return bottle;
            if (!p.getInventory().add(bottle)) p.drop(bottle, false);
        }
        return rest;
    }
}
