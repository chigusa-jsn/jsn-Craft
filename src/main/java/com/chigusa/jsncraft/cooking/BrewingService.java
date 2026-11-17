package com.chigusa.jsncraft.cooking;

import com.chigusa.jsncraft.data.ModAttachments;
import com.chigusa.jsncraft.fishing.FishDexData;
import com.chigusa.jsncraft.job.JobService;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillService;
import com.chigusa.jsncraft.skill.TimedFlags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/** 酒樽：仕込む・熟成・樽出し。 */
public final class BrewingService {

    public static int recipeIndex(BrewingBarrelBlockEntity be) {
        var list = CookingManager.BREWING.entries();
        if (be.isBrewing()) {
            for (int i = 0; i < list.size(); i++) if (list.get(i).getKey().equals(be.recipe())) return i;
            return -1;
        }
        return CookingManager.find(CookingManager.BREWING, be.ingredients());
    }

    public static int status(ServerPlayer player, BrewingBarrelBlockEntity be) {
        if (be.isBrewing()) {
            if (!be.ready()) return BrewingMenu.ST_BREWING;
            ItemStack out = be.items().getItem(BrewingBarrelBlockEntity.OUTPUT);
            return out.isEmpty() ? BrewingMenu.ST_DONE : BrewingMenu.ST_FULL;
        }
        int idx = CookingManager.find(CookingManager.BREWING, be.ingredients());
        if (idx < 0) return BrewingMenu.ST_NONE;
        int lvl = CookingService.cookLevel(player);
        if (lvl < 0) return BrewingMenu.ST_NOT_CHEF;
        CookingRecipe r = CookingManager.get(CookingManager.BREWING, idx);
        if (r == null || lvl < r.level()) return BrewingMenu.ST_LEVEL;
        return BrewingMenu.ST_READY;
    }

    /** 仕込む（調理師だけ） */
    public static boolean start(ServerPlayer player, BrewingBarrelBlockEntity be) {
        if (status(player, be) != BrewingMenu.ST_READY) return false;
        int idx = CookingManager.find(CookingManager.BREWING, be.ingredients());
        var entry = CookingManager.BREWING.entries().get(idx);
        CookingRecipe recipe = entry.getValue();
        int[] use = recipe.match(be.ingredients());
        if (use == null) return false;
        for (int slot : use) CookingService.consume(be.items(), slot, be.getLevel(), be.getBlockPos());
        boolean divine = TimedFlags.get(player, "divine_kitchen") > 0;
        int base = CookingService.rollQuality(player, null, -1, divine, player.getRandom());
        int max = SkillService.passive(player, "cook_perfect") != null ? 4 : 3;
        double time = recipe.time();
        SkillDefinition speed = SkillService.passive(player, "cook_speed");
        if (speed != null) time *= 1 - Math.min(0.6, speed.power() * 0.5); // 熟成は火加減の半分だけ早くなる
        if (divine) time = Math.min(time, 200);
        be.start(entry.getKey(), base, max, (int) Math.round(time));
        JobService.onAction(player, rule -> rule.action().equals("cook"), recipe.xp());
        var level = be.getLevel();
        level.playSound(null, be.getBlockPos(), SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 1.0F, 0.8F);
        level.playSound(null, be.getBlockPos(), SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.8F, 1.0F);
        if (level instanceof ServerLevel sl) {
            var c = be.getBlockPos().getCenter();
            sl.sendParticles(ParticleTypes.BUBBLE_POP, c.x, c.y + 0.6, c.z, 12, 0.3, 0.1, 0.3, 0.02);
        }
        int minutes = (int) Math.ceil(time / 1200.0);
        player.displayClientMessage(Component.literal("仕込み完了：").withStyle(ChatFormatting.GRAY)
                .append(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(recipe.result()).getDescription().copy().withStyle(ChatFormatting.WHITE))
                .append(Component.literal("（約" + minutes + "分で飲み頃。寝かせるほど★が上がる）").withStyle(ChatFormatting.GRAY)), true);
        return true;
    }

    /** 樽出し（誰でも） */
    public static boolean bottle(ServerPlayer player, BrewingBarrelBlockEntity be) {
        if (status(player, be) != BrewingMenu.ST_DONE) return false;
        int idx = recipeIndex(be);
        CookingRecipe recipe = CookingManager.get(CookingManager.BREWING, idx);
        if (recipe == null) {
            be.clear();
            return false;
        }
        int q = be.currentQuality();
        ItemStack result = CookingService.buildResult(recipe, recipe.count(), q, recipe.buffs());
        be.items().setItem(BrewingBarrelBlockEntity.OUTPUT, result);
        ResourceLocation id = be.recipe();
        be.clear();
        int dex = recipe.no() < 999 && id != null ? player.getData(ModAttachments.DISH_DEX).record(id.toString(), q) : -1;
        var level = be.getLevel();
        level.playSound(null, be.getBlockPos(), SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.playSound(null, be.getBlockPos(), SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        var msg = Component.literal("樽出し：").withStyle(ChatFormatting.GRAY).append(result.getHoverName().copy().withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" " + DishItem.stars(q) + " ×" + result.getCount()).withStyle(q >= 3 ? ChatFormatting.GOLD : ChatFormatting.YELLOW));
        if (dex == FishDexData.NEW) msg.append(Component.literal(" 図鑑に登録！").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        player.displayClientMessage(msg, true);
        return true;
    }

    private BrewingService() {}
}
