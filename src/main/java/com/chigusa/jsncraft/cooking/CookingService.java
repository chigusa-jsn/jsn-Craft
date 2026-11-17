package com.chigusa.jsncraft.cooking;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.fishing.FishItems;
import com.chigusa.jsncraft.fishing.FishRank;
import com.chigusa.jsncraft.job.JobService;
import com.chigusa.jsncraft.job.JobType;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillService;
import com.chigusa.jsncraft.skill.TimedFlags;
import com.chigusa.jsncraft.stat.ModAttributes;
import com.chigusa.jsncraft.vfx.Vfx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/** 調理（品質・バフの決め方）、料理を食べたとき、炎の調理師・宴の料理長のスキル。 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class CookingService {

    public static final ResourceLocation CHEF = JsnCraft.id("chef");
    public static final ResourceLocation GRAND_CHEF = JsnCraft.id("grand_chef");

    /** 品質ごとの倍率（★1〜★4）：満腹度・効果時間・能力値の量 */
    private static final double[] FOOD_MULT = {1.0, 1.25, 1.5, 2.0};
    private static final double[] TIME_MULT = {1.0, 1.5, 2.0, 3.0};
    private static final double[] AMOUNT_MULT = {1.0, 1.25, 1.5, 2.0};

    /** 料理の腕のレベル（炎の調理師ならそのレベル、宴の料理長なら 50 + レベル、それ以外は -1） */
    public static int cookLevel(ServerPlayer player) {
        var data = JobService.data(player);
        ResourceLocation life = data.getJob(JobType.LIFE);
        if (CHEF.equals(life)) return data.level(life);
        if (GRAND_CHEF.equals(life)) return 50 + data.level(life);
        return -1;
    }

    public static int status(ServerPlayer player, CookingStationBlockEntity be) {
        if (be.isCooking()) return CookingMenu.ST_COOKING;
        int idx = CookingManager.findRecipe(be.ingredients());
        if (idx < 0) return CookingMenu.ST_NONE;
        int lvl = cookLevel(player);
        if (lvl < 0) return CookingMenu.ST_NOT_CHEF;
        CookingRecipe r = CookingManager.recipe(idx);
        if (r == null || lvl < r.level()) return CookingMenu.ST_LEVEL;
        ItemStack season = be.items().getItem(CookingStationBlockEntity.SEASONING);
        if (!season.isEmpty() && CookingManager.seasoning(season) == null) return CookingMenu.ST_BAD_SEASONING;
        if (!be.outputFree()) return CookingMenu.ST_FULL;
        return CookingMenu.ST_READY;
    }

    /** 「調理する」ボタン */
    public static boolean startCooking(ServerPlayer player, CookingStationBlockEntity be) {
        if (status(player, be) != CookingMenu.ST_READY) return false;
        int idx = CookingManager.findRecipe(be.ingredients());
        CookingRecipe recipe = CookingManager.recipe(idx);
        if (recipe == null) return false;
        int[] use = recipe.match(be.ingredients());
        if (use == null) return false;
        Level level = be.getLevel();
        BlockPos pos = be.getBlockPos();
        RandomSource r = player.getRandom();

        // 材料の中でいちばんランクの高い魚
        int fishRank = -1;
        for (int slot : use) {
            if (be.items().getItem(slot).getItem() instanceof FishItems.FishItem f) fishRank = Math.max(fishRank, f.rank().ordinal());
        }
        // 牧場の上質な材料（高級肉・搾りたてミルク・金の卵）は品質が上がりやすい
        double premium = 0;
        for (int slot : use) premium += com.chigusa.jsncraft.ranch.RanchIngredients.qualityBonus(be.items().getItem(slot));
        // 材料と隠し味を使う（はちみつの瓶などの入れ物は返す）
        for (int slot : use) consume(be, slot, level, pos);
        ItemStack seasonStack = be.items().getItem(CookingStationBlockEntity.SEASONING);
        Seasoning season = CookingManager.seasoning(seasonStack);
        if (season != null) consume(be, CookingStationBlockEntity.SEASONING, level, pos);

        boolean divine = TimedFlags.get(player, "divine_kitchen") > 0;
        int quality = rollQuality(player, season, fishRank, divine, r, premium);

        // バフ：料理のもの＋隠し味（同じものは強いほう）
        Map<String, FoodBuff> merged = new LinkedHashMap<>();
        for (FoodBuff b : recipe.buffs()) mergeBuff(merged, b);
        if (season != null) {
            SkillDefinition master = SkillService.passive(player, "seasoning_master");
            double sm = 1 + (master != null ? master.power() : 0);
            for (FoodBuff b : season.buffs()) mergeBuff(merged, b.scaled(sm, sm));
        }
        List<FoodBuff> buffs = new ArrayList<>(merged.values());

        // 出来上がり
        int count = recipe.count();
        SkillDefinition dbl = SkillService.passive(player, "cook_double");
        boolean doubled = dbl != null && r.nextDouble() < dbl.chance();
        if (doubled) count *= 2;
        ItemStack result = buildResult(recipe, count, quality, buffs);

        // 調理時間
        double time = recipe.time();
        SkillDefinition speed = SkillService.passive(player, "cook_speed");
        if (speed != null) time *= 1 - Math.min(0.8, speed.power());
        if (divine) time = 10;
        be.start(result, (int) Math.round(time));

        // 料理図鑑に記録
        int dexResult = -1;
        if (result.getItem() instanceof DishItem && recipe.no() < 999) {
            var id = CookingManager.RECIPES.entries().get(idx).getKey();
            dexResult = player.getData(com.chigusa.jsncraft.data.ModAttachments.DISH_DEX).record(id.toString(), quality);
        }

        // 職業経験値（品質が高いほど多い）
        JobService.onAction(player, rule -> rule.action().equals("cook"), recipe.xp() * FOOD_MULT[quality - 1]);

        // 見た目と音
        if (level instanceof ServerLevel sl) {
            Vec3 c = Vec3.atCenterOf(pos).add(0, 0.6, 0);
            sl.sendParticles(ParticleTypes.FLAME, c.x, c.y, c.z, 15, 0.3, 0.1, 0.3, 0.02);
            sl.sendParticles(ParticleTypes.CLOUD, c.x, c.y + 0.3, c.z, 8, 0.2, 0.1, 0.2, 0.02);
            if (quality >= 3) Vfx.ring(sl, Vec3.atBottomCenterOf(pos).add(0, 1.02, 0), 1.2F, 14, Vfx.GOLD);
        }
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.6F, 1.4F);
        level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 1.0F, 1.0F);
        ChatFormatting color = quality >= 4 ? ChatFormatting.LIGHT_PURPLE : quality == 3 ? ChatFormatting.GOLD
                : quality == 2 ? ChatFormatting.YELLOW : ChatFormatting.WHITE;
        var msg = Component.literal("調理開始：").withStyle(ChatFormatting.GRAY).append(result.getHoverName().copy().withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" " + DishItem.stars(quality)).withStyle(color));
        if (doubled) msg.append(Component.literal(" ×2 大量調理！").withStyle(ChatFormatting.AQUA));
        if (dexResult == com.chigusa.jsncraft.fishing.FishDexData.NEW) msg.append(Component.literal(" 図鑑に登録！").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        if (recipe.rankEnum() == DishRank.LEGEND && level instanceof ServerLevel sl2) {
            sl2.getServer().getPlayerList().broadcastSystemMessage(Component.literal("✦ " + player.getGameProfile().getName()
                    + " が伝説の料理「").withStyle(ChatFormatting.GOLD).append(result.getHoverName().copy())
                    .append(Component.literal("」" + DishItem.stars(quality) + " を作った！").withStyle(ChatFormatting.GOLD)), false);
        }
        player.displayClientMessage(msg, true);
        return true;
    }

    /** 料理・お酒のアイテムを作る（品質に合わせて満腹度とバフを強くし、★とバフを書き込む） */
    public static ItemStack buildResult(CookingRecipe recipe, int count, int quality, List<FoodBuff> baseBuffs) {
        ItemStack result = new ItemStack(BuiltInRegistries.ITEM.get(recipe.result()), count);
        if (result.getItem() instanceof DishItem) {
            int q = Math.max(1, Math.min(4, quality));
            double fm = FOOD_MULT[q - 1];
            FoodProperties.Builder fb = new FoodProperties.Builder()
                    .nutrition((int) Math.round(recipe.nutrition() * fm))
                    .saturationModifier((float) (recipe.saturation() * fm));
            if (result.getItem() instanceof DrinkItem) fb.alwaysEdible().fast();
            result.set(DataComponents.FOOD, fb.build());
            List<FoodBuff> buffs = new ArrayList<>();
            for (FoodBuff b : baseBuffs) buffs.add(b.scaled(TIME_MULT[q - 1], AMOUNT_MULT[q - 1]));
            DishItem.write(result, q, buffs, recipe.alcohol());
        }
        return result;
    }

    static void consume(CookingStationBlockEntity be, int slot, Level level, BlockPos pos) {
        consume(be.items(), slot, level, pos);
    }

    static void consume(net.minecraft.world.Container items, int slot, Level level, BlockPos pos) {
        ItemStack stack = items.getItem(slot);
        ItemStack remainder = stack.getCraftingRemainingItem();
        stack.shrink(1);
        items.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
        if (!remainder.isEmpty()) Block.popResource(level, pos.above(), remainder.copy());
    }

    private static void mergeBuff(Map<String, FoodBuff> map, FoodBuff b) {
        FoodBuff old = map.get(b.key());
        if (old == null) {
            map.put(b.key(), b);
            return;
        }
        map.put(b.key(), new FoodBuff(b.effect(), b.attribute(), Math.max(old.amplifier(), b.amplifier()),
                Math.max(old.amount(), b.amount()), b.operation(), Math.max(old.seconds(), b.seconds()), b.special()));
    }

    /** 品質（★1〜★4）を決める */
    static int rollQuality(ServerPlayer player, Seasoning season, int fishRank, boolean divine, RandomSource r) {
        return rollQuality(player, season, fishRank, divine, r, 0);
    }

    static int rollQuality(ServerPlayer player, Seasoning season, int fishRank, boolean divine, RandomSource r, double bonus) {
        double score = 0.15 + bonus + Math.max(0, cookLevel(player)) * 0.006
                + player.getAttributeValue(ModAttributes.COOKING_MASTERY);
        SkillDefinition taste = SkillService.passive(player, "cook_quality");
        if (taste != null) score += taste.power();
        if (season != null) score += season.quality();
        if (fishRank > 0) score += fishRank * 0.08;
        int q = 1;
        if (r.nextDouble() < score) q = 2;
        SkillDefinition star = SkillService.passive(player, "cook_star");
        if (q == 2 && r.nextDouble() < score * 0.5 + (star != null ? star.power() : 0)) q = 3;
        if (fishRank == FishRank.LEGEND.ordinal() || divine) q = Math.max(q, 3);
        SkillDefinition perfect = SkillService.passive(player, "cook_perfect");
        if (q == 3 && perfect != null && r.nextDouble() < perfect.chance()) q = 4;
        return q;
    }

    // ================= 食べたとき =================

    public static void onEat(ServerPlayer player, ItemStack stack) {
        List<FoodBuff> buffs = DishItem.buffs(stack);
        SkillDefinition glutton = SkillService.passive(player, "glutton");
        double timeMult = 1 + (glutton != null ? glutton.power() : 0);
        for (FoodBuff b : buffs) b.apply(player, timeMult);
        if (SkillService.passive(player, "iron_stomach") != null) {
            player.removeEffect(MobEffects.HUNGER);
            player.removeEffect(MobEffects.POISON);
            player.removeEffect(MobEffects.CONFUSION);
        }
        ServerLevel level = player.serverLevel();
        // 宴：周りの仲間にも同じバフ
        double share = TimedFlags.get(player, "feast_share");
        if (share > 0 && !buffs.isEmpty()) {
            for (ServerPlayer other : level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(share))) {
                if (other == player) continue;
                for (FoodBuff b : buffs) b.apply(other, 1.0);
                level.sendParticles(ParticleTypes.HEART, other.getX(), other.getY() + 2, other.getZ(), 3, 0.3, 0.2, 0.3, 0);
                other.displayClientMessage(Component.literal(player.getGameProfile().getName() + " の宴の料理の効果をもらった！")
                        .withStyle(ChatFormatting.GOLD), true);
            }
            Vfx.ring(level, player.position().add(0, 0.05, 0), (float) share, 16, Vfx.GOLD);
        }
        int alc = DishItem.alcohol(stack);
        if (alc > 0) drink(player, alc);
        int q = DishItem.quality(stack);
        if (q >= 3) level.sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getY() + 1.2, player.getZ(), 10, 0.4, 0.4, 0.4, 0.1);
    }

    // ================= 特別な効果 =================

    public static void special(ServerPlayer player, String kind, double amount, double seconds) {
        switch (kind) {
            case "cleanse" -> {
                var bad = new ArrayList<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>>();
                for (var e : player.getActiveEffects()) if (!e.getEffect().value().isBeneficial()) bad.add(e.getEffect());
                bad.forEach(player::removeEffect);
                DRUNK.remove(player.getUUID());
                player.displayClientMessage(Component.literal("すっきりした！").withStyle(ChatFormatting.AQUA), true);
            }
            case "sober" -> {
                DRUNK.computeIfPresent(player.getUUID(), (k, v) -> v - Math.max(1, amount) <= 0 ? null : v - Math.max(1, amount));
                player.removeEffect(MobEffects.CONFUSION);
            }
            case "warm" -> {
                TimedFlags.set(player, "warm", 1, seconds);
                player.setTicksFrozen(0);
            }
            case "awake" -> {
                player.getStats().setValue(player, net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.TIME_SINCE_REST), 0);
                player.displayClientMessage(Component.literal("目がさえた！").withStyle(ChatFormatting.AQUA), true);
            }
            default -> { }
        }
    }

    /** ぽかぽか：凍えない */
    @SubscribeEvent
    public static void onWarmTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 10 != 0) return;
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
            if (p.getTicksFrozen() > 0 && TimedFlags.get(p, "warm") > 0) p.setTicksFrozen(0);
        }
    }

    // ================= 酔い =================

    /** プレイヤーの酔いの量（20秒で1ずつ抜ける） */
    private static final Map<UUID, Double> DRUNK = new java.util.concurrent.ConcurrentHashMap<>();

    private static void drink(ServerPlayer player, int alcohol) {
        if (SkillService.passive(player, "iron_stomach") != null) return; // 鉄の胃袋は酔わない
        double v = DRUNK.merge(player.getUUID(), (double) alcohol, Double::sum);
        if (v >= 12) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 600, 0));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 1));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 0));
            player.displayClientMessage(Component.literal("飲みすぎた…世界が回る").withStyle(ChatFormatting.DARK_PURPLE), true);
        } else if (v >= 8) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 400, 0));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 0));
            player.displayClientMessage(Component.literal("酔っぱらってきた…").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        } else if (v >= 5) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
            player.displayClientMessage(Component.literal("ほろ酔い").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    @SubscribeEvent
    public static void onDrunkTick(ServerTickEvent.Post event) {
        if (DRUNK.isEmpty() || event.getServer().getTickCount() % 400 != 0) return;
        DRUNK.replaceAll((k, v) -> v - 1);
        DRUNK.values().removeIf(v -> v <= 0);
    }

    // ================= スキル =================

    private static List<ServerPlayer> around(ServerPlayer player, double r) {
        return player.serverLevel().getEntitiesOfClass(ServerPlayer.class, new AABB(player.blockPosition()).inflate(r));
    }

    /** まかない：自分と周りの仲間のおなかを満たす */
    public static boolean feedArea(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        int food = Math.max(1, (int) Math.round(s.power()));
        for (ServerPlayer p : around(player, s.radius())) {
            p.getFoodData().eat(food, 0.6F);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1.5, p.getZ(), 6, 0.3, 0.3, 0.3, 0);
        }
        Vfx.ring(level, player.position().add(0, 0.05, 0), (float) s.radius(), 14, Vfx.GOLD);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY() + 1, player.getZ(), 6, 0.4, 0.2, 0.4, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    /** 香りの誘い：周りの仲間に再生と移動速度 */
    public static boolean aroma(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        int ticks = (int) Math.round(s.duration() * 20);
        for (ServerPlayer p : around(player, s.radius())) {
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, Math.max(0, s.amplifier())));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 0));
        }
        Vec3 o = player.position();
        for (int k = 0; k < 12; k++) {
            double a = k * Math.PI / 6;
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, o.x + Math.cos(a) * 1.5, o.y + 0.5, o.z + Math.sin(a) * 1.5, 1, 0, 0.05, 0, 0.02);
        }
        Vfx.magicCircle(level, o, (float) s.radius() * 0.5F, 24, Vfx.GOLD);
        level.playSound(null, player.blockPosition(), SoundEvents.CAMPFIRE_CRACKLE, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
        return true;
    }

    /** 宴：しばらくの間、自分が料理を食べると周りの仲間にも同じバフ */
    public static boolean feast(ServerPlayer player, SkillDefinition s) {
        TimedFlags.set(player, "feast_share", s.radius(), s.duration());
        ServerLevel level = player.serverLevel();
        Vfx.magicCircle(level, player.position(), (float) s.radius() * 0.5F, 30, Vfx.GOLD);
        Vfx.ring(level, player.position().add(0, 0.05, 0), (float) s.radius(), 20, Vfx.RED);
        level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1, player.getZ(), 30, 0.6, 0.6, 0.6, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.5F);
        player.displayClientMessage(Component.literal("宴 " + Math.round(s.duration()) + "秒：料理を食べると仲間にも効果が広がる").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    /** 神の厨房：しばらくの間、一瞬で調理でき、品質が必ず★3以上 */
    public static boolean divineKitchen(ServerPlayer player, SkillDefinition s) {
        TimedFlags.set(player, "divine_kitchen", 1, s.duration());
        ServerLevel level = player.serverLevel();
        Vfx.magicCircle(level, player.position(), 3.0F, 40, Vfx.GOLD);
        Vfx.ring(level, player.position().add(0, 0.05, 0), 4.0F, 24, Vfx.WHITE);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 40, 0.6, 1.0, 0.6, 0.08);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.6F);
        player.displayClientMessage(Component.literal("神の厨房 " + Math.round(s.duration()) + "秒").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        return true;
    }

    // ---- 宴会の卓：しばらくその場に留まる、仲間を回復する場 ----

    private record Banquet(UUID owner, ResourceKey<Level> dim, Vec3 center, double radius, int amplifier, long until) {}

    private static final List<Banquet> BANQUETS = new CopyOnWriteArrayList<>();

    public static boolean banquet(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        BANQUETS.removeIf(b -> b.owner().equals(player.getUUID()));
        BANQUETS.add(new Banquet(player.getUUID(), level.dimension(), player.position(), s.radius(), Math.max(0, s.amplifier()),
                level.getGameTime() + Math.round(s.duration() * 20)));
        Vfx.magicCircle(level, player.position(), (float) s.radius(), 40, Vfx.GOLD);
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.2F);
        player.displayClientMessage(s.name().copy().append(" " + Math.round(s.duration()) + "秒").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (BANQUETS.isEmpty() || server.getTickCount() % 40 != 0) return;
        long now = server.overworld().getGameTime();
        for (Banquet b : BANQUETS) {
            if (now > b.until()) {
                BANQUETS.remove(b);
                continue;
            }
            ServerLevel level = server.getLevel(b.dim());
            if (level == null) continue;
            for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(BlockPos.containing(b.center())).inflate(b.radius()))) {
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, b.amplifier()));
                p.addEffect(new MobEffectInstance(MobEffects.SATURATION, 1, 0));
            }
            Vfx.magicCircle(level, b.center().add(0, 0.05, 0), (float) b.radius(), 40, Vfx.GOLD);
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, b.center().x, b.center().y + 0.5, b.center().z, 4, b.radius() * 0.4, 0.1, b.radius() * 0.4, 0.01);
            level.sendParticles(ParticleTypes.NOTE, b.center().x, b.center().y + 1.5, b.center().z, 3, b.radius() * 0.4, 0.3, b.radius() * 0.4, 0.5);
        }
    }

    private CookingService() {}
}
