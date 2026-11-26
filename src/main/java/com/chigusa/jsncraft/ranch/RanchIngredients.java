package com.chigusa.jsncraft.ranch;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/** 牧場の上質な材料と、料理での扱い（ふつうの材料の代わり・品質ボーナス）。 */
public final class RanchIngredients {
    private static final Map<ResourceLocation, ResourceLocation> SUBSTITUTES = Map.of(
            JsnCraft.id("premium_beef"), ResourceLocation.withDefaultNamespace("beef"),
            JsnCraft.id("premium_pork"), ResourceLocation.withDefaultNamespace("porkchop"),
            JsnCraft.id("premium_mutton"), ResourceLocation.withDefaultNamespace("mutton"),
            JsnCraft.id("premium_chicken"), ResourceLocation.withDefaultNamespace("chicken"),
            JsnCraft.id("fresh_milk"), ResourceLocation.withDefaultNamespace("milk_bucket"),
            JsnCraft.id("golden_egg"), ResourceLocation.withDefaultNamespace("egg"),
            JsnCraft.id("golden_rice"), JsnCraft.id("rice"),
            JsnCraft.id("golden_wheat"), ResourceLocation.withDefaultNamespace("wheat"),
            JsnCraft.id("golden_potato"), ResourceLocation.withDefaultNamespace("potato"),
            JsnCraft.id("golden_tomato"), JsnCraft.id("tomato"));

    /** この材料が代わりになる、ふつうの材料（なければ null） */
    public static ResourceLocation substituteFor(ResourceLocation id) {
        return SUBSTITUTES.get(id);
    }

    /** 品質の上がりやすさ（高級肉・搾りたてミルク +0.1、金の卵 +0.3） */
    public static double qualityBonus(ItemStack stack) {
        if (stack.is(ModRanch.GOLDEN_EGG.get())) return 0.3;
        if (stack.is(ModRanch.PREMIUM_BEEF.get()) || stack.is(ModRanch.PREMIUM_PORK.get()) || stack.is(ModRanch.PREMIUM_MUTTON.get())
                || stack.is(ModRanch.PREMIUM_CHICKEN.get()) || stack.is(ModRanch.FRESH_MILK.get())) return 0.1;
        return 0;
    }

    private RanchIngredients() {}
}
