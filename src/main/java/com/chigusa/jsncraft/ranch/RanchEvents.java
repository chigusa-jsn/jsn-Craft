package com.chigusa.jsncraft.ranch;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.job.JobService;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** 牧場のできごと：★の付与と同期・繁殖の制限・子の★・畜産物。 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class RanchEvents {
    private static final String MILK_KEY = "jsn_milk_time";

    private static void xp(ServerPlayer p, double mult) {
        JobService.onAction(p, rule -> rule.action().equals("ranch"), mult);
    }

    // ================= ★の付与と同期 =================

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Animal a)) return;
        if (RanchService.rank(a) == 0) RanchService.setRank(a, RanchService.wildRank(a.getRandom()));
    }

    @SubscribeEvent
    public static void onTrack(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer p && event.getTarget() instanceof Animal a) RanchService.sync(p, a);
    }

    // ================= 繁殖は獣飼いだけ／搾乳／毛刈り =================

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(event.getTarget() instanceof Animal a)) return;
        ItemStack stack = event.getItemStack();
        boolean rancher = RanchService.isRancher(p);

        // 繁殖（大人にエサ）
        if (a.isFood(stack) && !a.isBaby() && a.getAge() == 0 && a.canFallInLove() && !rancher) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            p.displayClientMessage(Component.literal("繁殖させられるのは獣飼いだけ").withStyle(ChatFormatting.RED), true);
            return;
        }

        // 搾りたてミルク（獣飼いがガラス瓶で）
        if (a instanceof Cow cow && stack.is(Items.GLASS_BOTTLE) && !cow.isBaby() && rancher) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            int rank = RanchService.rankOr1(cow);
            double wait = 6000 * (1 - 0.1 * (rank - 1));
            SkillDefinition mm = SkillService.passive(p, "milk_master");
            if (mm != null) wait *= 1 - Math.min(0.8, mm.power());
            long now = p.level().getGameTime();
            long last = cow.getPersistentData().getLong(MILK_KEY);
            if (last > 0 && now - last < wait) {
                p.displayClientMessage(Component.literal("まだ搾れない（あと" + (int) Math.ceil((wait - (now - last)) / 20.0) + "秒）").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            cow.getPersistentData().putLong(MILK_KEY, now);
            int count = 1 + (rank >= 4 ? 1 : 0);
            if (!p.getAbilities().instabuild) stack.shrink(1);
            ItemStack milk = new ItemStack(ModRanch.FRESH_MILK.get(), count);
            if (!p.getInventory().add(milk)) p.drop(milk, false);
            p.level().playSound(null, cow.blockPosition(), SoundEvents.COW_MILK, SoundSource.PLAYERS, 1.0F, 1.2F);
            xp(p, 2);
            return;
        }

        // 毛刈り：★が高いほど羊毛が多い
        if (a instanceof Sheep sheep && stack.is(Items.SHEARS) && sheep.readyForShearing()) {
            int extra = RanchService.rankOr1(sheep) - 1;
            SkillDefinition rich = SkillService.passive(p, "rich_yield");
            if (rich != null && p.getRandom().nextDouble() < rich.chance()) extra++;
            if (extra > 0) {
                Item wool = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(sheep.getColor().getName() + "_wool"));
                sheep.spawnAtLocation(new ItemStack(wool, extra), 1);
            }
            if (rancher) xp(p, 1);
        }
    }

    // ================= 子どもが生まれたとき =================

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBaby(BabyEntitySpawnEvent event) {
        if (event.isCanceled()) return;
        ServerPlayer breeder = event.getCausedByPlayer() instanceof ServerPlayer sp ? sp : null;
        if (breeder != null && !RanchService.isRancher(breeder)) {
            event.setCanceled(true);
            return;
        }
        if (!(event.getParentA() instanceof Animal a) || !(event.getParentB() instanceof Animal b)) return;
        AgeableMob child = event.getChild();
        if (!(child instanceof Animal baby) || !(a.level() instanceof ServerLevel level)) return;
        RandomSource r = level.getRandom();
        int rank = RanchService.childRank(breeder, a, b, r);
        RanchService.setRank(baby, rank);
        grow(breeder, baby, rank);

        int born = 1;
        if (breeder != null) {
            SkillDefinition twin = SkillService.passive(breeder, "twin_birth");
            SkillDefinition triple = SkillService.passive(breeder, "triplets");
            int extra = 0;
            if (triple != null && r.nextDouble() < triple.chance()) extra = 2;
            else if (twin != null && r.nextDouble() < twin.chance()) extra = 1;
            for (int i = 0; i < extra; i++) {
                AgeableMob more = a.getBreedOffspring(level, b);
                if (!(more instanceof Animal m)) continue;
                m.setBaby(true);
                m.moveTo(a.getX(), a.getY(), a.getZ(), 0.0F, 0.0F);
                int rk = RanchService.childRank(breeder, a, b, r);
                RanchService.setRank(m, rk);
                grow(breeder, m, rk);
                level.addFreshEntityWithPassengers(m);
                rank = Math.max(rank, rk);
                born++;
            }
            xp(breeder, born * (6 + rank * 4));
            String what = born == 3 ? "三つ子" : born == 2 ? "双子" : "子ども";
            breeder.displayClientMessage(Component.literal(what + "が生まれた！ ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(RanchService.stars(rank)).withStyle(rank >= 4 ? ChatFormatting.GOLD : ChatFormatting.YELLOW)), true);
        }
        if (rank >= 4) {
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, a.getX(), a.getY() + 1, a.getZ(), 20, 0.4, 0.4, 0.4, 0.2);
            level.playSound(null, a.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.6F, 1.6F);
        }
    }

    /** ★が高い子・スキル「早く育つ」で、大人になるまでが短くなる */
    private static void grow(ServerPlayer breeder, Animal baby, int rank) {
        double fast = 0.05 * (rank - 1);
        if (breeder != null) {
            SkillDefinition g = SkillService.passive(breeder, "fast_growth");
            if (g != null) fast += g.power();
        }
        baby.setAge((int) Math.round(AgeableMob.BABY_START_AGE * (1 - Math.min(0.9, fast))));
    }

    // ================= 倒したとき：★で取れる量が増える・高級肉 =================

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Animal a) || a.isBaby()) return;
        int rank = RanchService.rankOr1(a);
        RandomSource r = a.getRandom();
        ServerPlayer killer = event.getSource().getEntity() instanceof ServerPlayer sp ? sp : null;
        if (rank > 1) {
            double m = 1 + 0.25 * (rank - 1);
            for (ItemEntity drop : event.getDrops()) {
                ItemStack s = drop.getItem();
                if (s.isDamageableItem()) continue;
                double want = s.getCount() * m;
                int n = (int) want + (r.nextDouble() < want - (int) want ? 1 : 0);
                s.setCount(Math.min(s.getMaxStackSize(), n));
            }
        }
        Item premium = a instanceof Cow ? ModRanch.PREMIUM_BEEF.get() : a instanceof Pig ? ModRanch.PREMIUM_PORK.get()
                : a instanceof Sheep ? ModRanch.PREMIUM_MUTTON.get() : a instanceof Chicken ? ModRanch.PREMIUM_CHICKEN.get() : null;
        if (premium == null) return;
        double chance = 0.25 * (rank - 2);
        if (killer != null) {
            SkillDefinition gold = SkillService.passive(killer, "golden_touch");
            if (gold != null) chance *= 1 + gold.power();
        }
        if (chance > 0 && r.nextDouble() < chance) {
            event.getDrops().add(new ItemEntity(a.level(), a.getX(), a.getY() + 0.5, a.getZ(), new ItemStack(premium, rank >= 5 ? 2 : 1)));
        }
    }

    // ================= ニワトリ：★で卵が早い・金の卵 =================

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Chicken c) || c.level().isClientSide() || c.tickCount % 20 != 0 || c.isBaby()) return;
        int rank = RanchService.rankOr1(c);
        if (rank <= 1) return;
        c.eggTime = Math.max(1, c.eggTime - 5 * (rank - 1));
        if (rank >= 4 && c.getRandom().nextDouble() < 0.0015 * (rank - 3)) {
            c.spawnAtLocation(new ItemStack(ModRanch.GOLDEN_EGG.get()));
            c.playSound(SoundEvents.CHICKEN_EGG, 1.0F, 1.4F);
            if (c.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.WAX_ON, c.getX(), c.getY() + 0.3, c.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
        }
    }

    private RanchEvents() {}
}
