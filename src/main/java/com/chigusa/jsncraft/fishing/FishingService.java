package com.chigusa.jsncraft.fishing;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.job.JobService;
import com.chigusa.jsncraft.network.FishingPayloads;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillEffects;
import com.chigusa.jsncraft.skill.SkillService;
import com.chigusa.jsncraft.stat.ModAttributes;
import com.chigusa.jsncraft.vfx.Vfx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 釣りミニゲームと釣り系スキル。
 *
 * 流れ：浮きが沈んでいるときに竿を引く → バニラが釣果を決める（他MODの魚もここで混ざる）
 * → その釣果をいったん預かって、ミニゲームを始める → 勝ったら釣果と経験値を渡す。
 * ミニゲームなしでは何も釣れないので、放置釣り場では職業経験値がたまらない。
 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class FishingService {

    public enum Kind { JUNK, FISH, TREASURE, LORD }

    private record Session(List<ItemStack> drops, Kind kind, long start, Vec3 pos) {}

    private record Buff(double power, long until) {}

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Buff>> BUFFS = new ConcurrentHashMap<>();

    public static final ResourceKey<LootTable> LORD_TABLE =
            ResourceKey.create(Registries.LOOT_TABLE, JsnCraft.id("gameplay/fishing_lord"));
    private static final TagKey<Item> RAW_FISH = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "foods/raw_fish"));
    private static final Set<Item> TREASURES = Set.of(Items.NAME_TAG, Items.SADDLE, Items.NAUTILUS_SHELL,
            Items.HEART_OF_THE_SEA, Items.TRIDENT, Items.ENCHANTED_GOLDEN_APPLE);

    /** 早すぎる「釣れた」はおかしいので無効（tick） */
    private static final int MIN_TICKS = 15;
    /** ミニゲームの制限時間（tick）。これを過ぎたら逃げられたことにする */
    private static final int TIMEOUT = 20 * 90;

    // ================= 釣果を預かってミニゲーム開始 =================

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onFished(ItemFishedEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) return;
        List<ItemStack> drops = new ArrayList<>();
        for (ItemStack s : event.getDrops()) if (!s.isEmpty()) drops.add(s.copy());
        if (drops.isEmpty()) return;
        event.setCanceled(true); // いったんバニラの釣果は渡さない（ミニゲームに勝ったら渡す）

        FishingHook hook = event.getHookEntity();
        ServerLevel level = player.serverLevel();
        RandomSource r = player.getRandom();
        Kind kind = null;

        // 海の主
        SkillDefinition lord = SkillService.passive(player, "sea_lord");
        if (lord != null && r.nextDouble() < lord.chance()) {
            List<ItemStack> loot = roll(level, LORD_TABLE, hook, player, hook.position());
            if (!loot.isEmpty()) {
                drops = loot;
                kind = Kind.LORD;
                level.sendParticles(ParticleTypes.SPLASH, hook.getX(), hook.getY() + 0.3, hook.getZ(), 120, 1.2, 0.2, 1.2, 0.3);
                level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, hook.getX(), hook.getY(), hook.getZ(), 60, 1.0, 0.3, 1.0, 0.2);
                Vfx.ring(level, hook.position().add(0, 0.1, 0), 4.0F, 20, Vfx.RED);
                level.playSound(null, hook.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.6F, 0.7F);
            }
        }
        if (kind == null) {
            kind = classify(drops.get(0));
            // お宝に化ける
            if (kind != Kind.TREASURE && r.nextDouble() < treasureChance(player)) {
                List<ItemStack> loot = roll(level, BuiltInLootTables.FISHING_TREASURE, hook, player, hook.position());
                if (!loot.isEmpty()) {
                    drops = loot;
                    kind = Kind.TREASURE;
                }
            }
        }

        Session session = new Session(drops, kind, level.getGameTime(), hook.position());

        // 一本釣り：ミニゲームなしで釣れる（主は除く）
        SkillDefinition instant = SkillService.passive(player, "fish_instant");
        if (kind != Kind.LORD && instant != null && r.nextDouble() < instant.chance()) {
            player.displayClientMessage(Component.literal("一本釣り！").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
            complete(player, session, false);
            return;
        }

        SESSIONS.put(player.getUUID(), session);
        int[] st = FishingManager.INSTANCE.settings(drops.get(0), kind);
        ItemStack icon = (kind == Kind.FISH || kind == Kind.JUNK) ? drops.get(0).copyWithCount(1) : ItemStack.EMPTY;
        PacketDistributor.sendToPlayer(player, new FishingPayloads.Start(st[0], kind.ordinal() * 8 + st[1],
                (float) zoneBonus(player), (float) calm(player), (float) reel(player), icon));
    }

    /** ミニゲームの結果を受け取る */
    public static void onResult(ServerPlayer player, boolean success, boolean perfect) {
        Session s = SESSIONS.remove(player.getUUID());
        if (s == null) return;
        long elapsed = player.level().getGameTime() - s.start();
        if (success && elapsed >= MIN_TICKS && elapsed <= TIMEOUT + 40) {
            complete(player, s, perfect);
        } else {
            player.displayClientMessage(Component.literal(s.kind() == Kind.LORD ? "主に逃げられた…" : "逃げられた…")
                    .withStyle(ChatFormatting.GRAY), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.PLAYERS, 0.6F, 0.6F);
        }
    }

    /** 釣れた！ 釣果・経験値・おまけを渡す */
    private static void complete(ServerPlayer player, Session s, boolean perfect) {
        ServerLevel level = player.serverLevel();
        RandomSource r = player.getRandom();
        int extra = 0;
        if (perfect) {
            SkillDefinition dbl = SkillService.passive(player, "fish_double");
            if (dbl != null && r.nextDouble() < dbl.chance()) extra++;
        }
        extra += (int) Math.round(buffPower(player, "bounty"));
        extra += (int) Math.round(buffPower(player, "tide"));
        boolean dup = s.kind() == Kind.FISH || s.kind() == Kind.JUNK; // お宝と主は増えない
        int times = dup ? 1 + extra : 1;

        ItemStack main = s.drops().get(0);
        for (int t = 0; t < times; t++) {
            for (ItemStack stack : s.drops()) give(player, stack.copy());
        }
        int orbs = s.kind() == Kind.LORD ? 40 : s.kind() == Kind.TREASURE ? 8 : 1 + r.nextInt(6);
        level.addFreshEntity(new ExperienceOrb(level, player.getX(), player.getY() + 0.5, player.getZ(), orbs));
        if (main.is(ItemTags.FISHES) || main.is(RAW_FISH)) player.awardStat(Stats.FISH_CAUGHT, times);

        // 職業経験値（パーフェクトは1.5倍、増えた分も入る）
        if (!player.isCreative()) {
            String cat = s.kind().name().toLowerCase(java.util.Locale.ROOT);
            double mult = (perfect ? 1.5 : 1.0) * times;
            JobService.onAction(player, rule -> rule.action().equals("fish") && rule.matchesItem(main, cat), mult);
        }

        // 漁師の宴：魚を釣ると少しおなかが満たされる
        SkillDefinition feast = SkillService.passive(player, "fisher_feast");
        if (feast != null && s.kind() != Kind.JUNK) player.getFoodData().eat((int) Math.round(feast.power()), 0.3F);

        // 見た目と音
        MutableComponent msg = Component.literal(s.kind() == Kind.LORD ? "海の主を釣り上げた！ " : "釣れた！ ")
                .withStyle(s.kind() == Kind.LORD ? ChatFormatting.GOLD : ChatFormatting.AQUA)
                .append(main.getHoverName().copy().withStyle(ChatFormatting.WHITE));
        if (times > 1) msg.append(Component.literal(" ×" + times).withStyle(ChatFormatting.YELLOW));
        if (perfect) msg.append(Component.literal("  PERFECT!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.displayClientMessage(msg, true);
        Vec3 p = player.position();
        level.sendParticles(ParticleTypes.SPLASH, s.pos().x, s.pos().y + 0.2, s.pos().z, 30, 0.4, 0.1, 0.4, 0.2);
        if (perfect || s.kind() == Kind.TREASURE || s.kind() == Kind.LORD) {
            Vfx.ring(level, p.add(0, 0.05, 0), 1.8F, 14, Vfx.GOLD);
            level.sendParticles(ParticleTypes.WAX_ON, p.x, p.y + 1.2, p.z, 15, 0.5, 0.5, 0.5, 0.2);
        }
        if (s.kind() == Kind.LORD) {
            Vfx.magicCircle(level, p, 3.0F, 30, Vfx.GOLD);
            level.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
            level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("🐋 " + player.getGameProfile().getName()
                    + " が海の主を釣り上げた！").withStyle(ChatFormatting.GOLD), false);
        } else {
            level.playSound(null, player.blockPosition(), perfect ? SoundEvents.PLAYER_LEVELUP : SoundEvents.ITEM_PICKUP,
                    SoundSource.PLAYERS, perfect ? 0.5F : 0.8F, perfect ? 1.8F : 1.0F);
        }
    }

    // ================= 能力の計算 =================

    private static double zoneBonus(ServerPlayer player) {
        double z = player.getAttributeValue(ModAttributes.FISHING_ZONE);
        SkillDefinition sense = SkillService.passive(player, "fish_zone");
        if (sense != null) z += sense.power();
        SkillDefinition wave = SkillService.passive(player, "wave_rider");
        if (wave != null && (player.isInWater() || player.getVehicle() instanceof Boat)) z += wave.power();
        return z;
    }

    private static double calm(ServerPlayer player) {
        double c = player.getAttributeValue(ModAttributes.FISHING_CALM);
        SkillDefinition calm = SkillService.passive(player, "fish_calm");
        if (calm != null) c += calm.power();
        return Math.min(0.6, c);
    }

    private static double reel(ServerPlayer player) {
        double v = player.getAttributeValue(ModAttributes.FISHING_REEL);
        if (buffPower(player, "bounty") > 0) v += 0.5; // 大漁旗
        return v;
    }

    private static double treasureChance(ServerPlayer player) {
        double c = player.getAttributeValue(ModAttributes.FISHING_TREASURE);
        SkillDefinition scent = SkillService.passive(player, "fish_treasure");
        if (scent != null) c += scent.chance();
        SkillDefinition rain = SkillService.passive(player, "rain_luck");
        if (rain != null && player.level().isRaining()) c += rain.power();
        return c;
    }

    private static double biteSpeed(ServerPlayer player) {
        double b = player.getAttributeValue(ModAttributes.FISHING_BITE) + buffPower(player, "bait");
        SkillDefinition rain = SkillService.passive(player, "rain_luck");
        if (rain != null && player.level().isRaining()) b += rain.bonus();
        return Math.min(0.9, b);
    }

    public static Kind classify(ItemStack stack) {
        if (stack.is(ItemTags.FISHES) || stack.is(RAW_FISH)) return Kind.FISH;
        if (stack.isEnchanted() || stack.has(DataComponents.STORED_ENCHANTMENTS) || TREASURES.contains(stack.getItem()))
            return Kind.TREASURE;
        return Kind.JUNK;
    }

    // ================= 魚がかかるのを早くする =================

    @Nullable
    private static final Field LURED = findField("timeUntilLured");

    @Nullable
    private static Field findField(String name) {
        try {
            Field f = FishingHook.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (Exception e) {
            JsnCraft.LOGGER.warn("[jsn-Craft] 釣りの待ち時間を短くできません（{}）", e.toString());
            return null;
        }
    }

    @SubscribeEvent
    public static void onHookTick(EntityTickEvent.Pre event) {
        if (LURED == null || !(event.getEntity() instanceof FishingHook hook) || hook.level().isClientSide) return;
        if (!(hook.getPlayerOwner() instanceof ServerPlayer player)) return;
        double bite = biteSpeed(player);
        if (bite <= 0 || player.getRandom().nextDouble() >= bite) return;
        try {
            int t = LURED.getInt(hook);
            if (t > 1) LURED.setInt(hook, t - 1); // 1tickで2回分進む
        } catch (Exception ignored) {
        }
    }

    // ================= スキル =================

    /** 撒き餌・大漁旗・潮の恵み：しばらく続く釣りの強化。target が種類（bait / bounty / tide） */
    public static boolean fishBuff(ServerPlayer player, SkillDefinition s) {
        long until = player.level().getGameTime() + Math.round(s.duration() * 20);
        BUFFS.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(s.target(), new Buff(s.power(), until));
        ServerLevel level = player.serverLevel();
        Vec3 p = player.position();
        Vfx.magicCircle(level, p, 2.0F, 24, Vfx.AZURE);
        Vfx.ring(level, p.add(0, 0.05, 0), 2.8F, 16, Vfx.AZURE);
        level.sendParticles(ParticleTypes.SPLASH, p.x, p.y + 1, p.z, 40, 0.8, 0.6, 0.8, 0.2);
        level.sendParticles(ParticleTypes.BUBBLE_POP, p.x, p.y + 1, p.z, 20, 0.6, 0.6, 0.6, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 0.7F, 1.4F);
        player.displayClientMessage(s.name().copy().append(" " + Math.round(s.duration()) + "秒").withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    /** 水の加護：水中呼吸＋イルカの好意 */
    public static boolean waterBlessing(ServerPlayer player, SkillDefinition s) {
        int ticks = (int) Math.round(s.duration() * 20);
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, ticks, 0));
        player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, ticks, Math.max(0, s.amplifier())));
        if (s.amplifier() >= 1) player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, ticks, 0));
        ServerLevel level = player.serverLevel();
        Vec3 p = player.position();
        Vfx.magicCircle(level, p, 2.2F, 30, Vfx.AZURE);
        level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, p.x, p.y + 0.5, p.z, 60, 0.6, 0.8, 0.6, 0.3);
        level.sendParticles(ParticleTypes.NAUTILUS, p.x, p.y + 1.5, p.z, 30, 0.6, 0.6, 0.6, 0.5);
        level.playSound(null, player.blockPosition(), SoundEvents.CONDUIT_AMBIENT_SHORT, SoundSource.PLAYERS, 1.0F, 1.2F);
        return true;
    }

    /** 投網：目の前の水に網を投げて、まとめて引き上げる */
    public static boolean castNet(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        HitResult hit = player.pick(s.radius(), 0, true);
        if (!(hit instanceof BlockHitResult bhr) || hit.getType() != HitResult.Type.BLOCK
                || !level.getFluidState(bhr.getBlockPos()).is(FluidTags.WATER)) {
            player.displayClientMessage(Component.literal("水面に向けて使おう").withStyle(ChatFormatting.RED), true);
            return false;
        }
        BlockPos waterPos = bhr.getBlockPos();
        Vec3 c = Vec3.atCenterOf(waterPos).add(0, 0.45, 0);
        level.playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 1.0F, 0.5F);
        // 網が飛んでいく軌跡
        Vec3 from = player.getEyePosition();
        for (int i = 1; i <= 8; i++) {
            Vec3 at = from.lerp(c, i / 8.0).add(0, Math.sin(Math.PI * i / 8.0) * 1.5, 0);
            int d = i;
            SkillEffects.later(player, d, () -> level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 3, 0.15, 0.15, 0.15, 0));
        }
        // 着水：波紋が広がる
        SkillEffects.later(player, 9, () -> {
            level.playSound(null, waterPos, SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.2F, 0.8F);
            level.sendParticles(ParticleTypes.SPLASH, c.x, c.y, c.z, 120, 1.6, 0.1, 1.6, 0.3);
            Vfx.ring(level, c, 2.0F, 14, Vfx.WHITE);
        });
        SkillEffects.later(player, 13, () -> Vfx.ring(level, c, 3.5F, 16, Vfx.AZURE));
        SkillEffects.later(player, 17, () -> Vfx.ring(level, c, 5.0F, 18, Vfx.AZURE));
        // 引き上げ
        SkillEffects.later(player, 28, () -> {
            if (!player.isAlive()) return;
            level.playSound(null, waterPos, SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 1.0F, 0.6F);
            level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, c.x, c.y, c.z, 80, 1.2, 0.3, 1.2, 0.4);
            level.sendParticles(ParticleTypes.SPLASH, c.x, c.y + 0.3, c.z, 80, 1.2, 0.3, 1.2, 0.4);
            int count = Math.max(1, s.amplifier());
            int caught = 0;
            for (int i = 0; i < count; i++) {
                for (ItemStack stack : roll(level, BuiltInLootTables.FISHING, null, player, c)) {
                    ItemEntity item = new ItemEntity(level, c.x, c.y + 0.3, c.z, stack.copy());
                    double dx = player.getX() - c.x, dy = player.getY() - c.y, dz = player.getZ() - c.z;
                    item.setDeltaMovement(dx * 0.1, dy * 0.1 + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08, dz * 0.1);
                    level.addFreshEntity(item);
                    caught++;
                    if (!player.isCreative()) {
                        String cat = classify(stack).name().toLowerCase(java.util.Locale.ROOT);
                        JobService.onAction(player, rule -> rule.action().equals("fish") && rule.matchesItem(stack, cat), 0.5);
                    }
                }
            }
            level.addFreshEntity(new ExperienceOrb(level, player.getX(), player.getY() + 0.5, player.getZ(), 2 * caught));
            player.awardStat(Stats.FISH_CAUGHT, caught);
            player.displayClientMessage(Component.literal("投網：" + caught + "個 引き上げた！").withStyle(ChatFormatting.AQUA), true);
        });
        return true;
    }

    // ================= 道具 =================

    private static List<ItemStack> roll(ServerLevel level, ResourceKey<LootTable> table, @Nullable Entity hook,
                                        ServerPlayer player, Vec3 origin) {
        LootParams.Builder b = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, origin)
                .withParameter(LootContextParams.TOOL, rod(player))
                .withLuck(player.getLuck());
        if (hook != null) b.withParameter(LootContextParams.THIS_ENTITY, hook);
        try {
            return new ArrayList<>(level.getServer().reloadableRegistries().getLootTable(table)
                    .getRandomItems(b.create(LootContextParamSets.FISHING)));
        } catch (Exception e) {
            JsnCraft.LOGGER.warn("[jsn-Craft] 釣りのドロップ表 {} が使えません: {}", table.location(), e.toString());
            return List.of();
        }
    }

    private static ItemStack rod(Player player) {
        if (player.getMainHandItem().getItem() instanceof FishingRodItem) return player.getMainHandItem();
        if (player.getOffhandItem().getItem() instanceof FishingRodItem) return player.getOffhandItem();
        return new ItemStack(Items.FISHING_ROD);
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private static double buffPower(ServerPlayer player, String key) {
        Map<String, Buff> m = BUFFS.get(player.getUUID());
        if (m == null) return 0;
        Buff b = m.get(key);
        return b != null && b.until() > player.level().getGameTime() ? b.power() : 0;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 0) return;
        long now = event.getServer().overworld().getGameTime();
        SESSIONS.entrySet().removeIf(e -> now - e.getValue().start() > TIMEOUT + 200);
        BUFFS.values().forEach(m -> m.values().removeIf(b -> b.until() <= now));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
        BUFFS.remove(event.getEntity().getUUID());
    }

    private FishingService() {}
}
