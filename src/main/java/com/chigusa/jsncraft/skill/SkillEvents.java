package com.chigusa.jsncraft.skill;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.job.JobService;
import com.chigusa.jsncraft.quest.QuestService;
import com.chigusa.jsncraft.stat.ResourceService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** パッシブスキルの効果と、スキルまわりの細かい処理。 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class SkillEvents {

    /** 一刀伐採で一度に倒せる原木の数 */
    private static final int MAX_LOGS = 96;

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(SkillManager.INSTANCE);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        SkillEffects.tickBuffs(event.getServer());
        SkillService.tickCasts(event.getServer());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            SkillService.autoFill(p);
            SkillService.sync(p);
        }
    }

    // ---------------- 戦闘のパッシブ ----------------

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDamage(LivingIncomingDamageEvent event) {
        // 受け流し：かまえている間の近接攻撃を無効にして反撃
        if (event.getEntity() instanceof ServerPlayer victim
                && event.getSource().getDirectEntity() instanceof LivingEntity attacker && attacker != victim) {
            PlayerSkillData pd = SkillService.data(victim);
            if (victim.level().getGameTime() <= pd.parryUntil) {
                event.setCanceled(true);
                SkillEffects.counter(victim, attacker);
                return;
            }
        }

        // 見切り：近接攻撃をたまにかわす
        if (event.getEntity() instanceof ServerPlayer victim
                && event.getSource().getDirectEntity() instanceof LivingEntity attacker && attacker != victim) {
            SkillDefinition dodge = SkillService.passive(victim, "dodge");
            if (dodge != null && victim.getRandom().nextDouble() < dodge.chance()) {
                event.setCanceled(true);
                victim.serverLevel().sendParticles(ParticleTypes.CLOUD, victim.getX(), victim.getY() + 1, victim.getZ(), 12, 0.4, 0.5, 0.4, 0.04);
                victim.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT, victim.getX(), victim.getY() + 1.2, victim.getZ(), 10, 0.4, 0.5, 0.4, 0.2);
                victim.level().playSound(null, victim.blockPosition(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 1.2F, 1.4F);
                victim.level().playSound(null, victim.blockPosition(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.6F, 1.8F);
                victim.displayClientMessage(net.minecraft.network.chat.Component.literal("✦ 見切り！ ✦")
                        .withStyle(net.minecraft.ChatFormatting.AQUA, net.minecraft.ChatFormatting.BOLD), true);
                return;
            }
        }

        // 自分の近接攻撃が強くなる系
        if (event.getSource().getEntity() instanceof ServerPlayer player && event.getSource().getDirectEntity() == player) {
            float amount = event.getAmount();
            // HP満タンで攻撃アップ
            SkillDefinition full = SkillService.passive(player, "full_hp_bonus");
            if (full != null && player.getHealth() >= player.getMaxHealth()) amount *= (float) (1 + full.power());
            // 踏み込み直後の一撃
            PlayerSkillData d = SkillService.data(player);
            if (d.empowered > 0 && player.level().getGameTime() <= d.empoweredUntil) {
                amount *= (float) (1 + d.empowered);
                player.crit(event.getEntity());
                d.empowered = 0;
                HitFlags.CRIT.set(true);
                com.chigusa.jsncraft.vfx.Vfx.sparks(player.serverLevel(), event.getEntity().getBoundingBox().getCenter(), 16);
                com.chigusa.jsncraft.vfx.Vfx.slash(player.serverLevel(), event.getEntity().getBoundingBox().getCenter(), 1.6F,
                        com.chigusa.jsncraft.vfx.Vfx.GOLD, 30);
            }
            event.setAmount(amount);
        }
    }

    // ---------------- 生活のパッシブ ----------------

    /** 一刀伐採：しゃがみながら原木を切ると、つながった原木を丸ごと倒す（1本ごとにスタミナを使う） */
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()) return;
        BlockState state = event.getState();
        if (!state.is(BlockTags.LOGS)) return;
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos origin = event.getPos();

        SkillDefinition feller = player.isShiftKeyDown() ? SkillService.passive(player, "tree_feller") : null;
        List<BlockPos> felled = new ArrayList<>();
        if (feller != null) {
            felled = findTree(level, origin, state.getBlock());
            int cut = 0;
            for (BlockPos pos : felled) {
                if (!ResourceService.tryUseStamina(player, feller.stamina())) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("スタミナ切れ！"), true);
                    break;
                }
                BlockState s = level.getBlockState(pos);
                level.destroyBlock(pos, true, player);
                // 経験値と依頼も、1本ずつ数える
                JobService.onAction(player, rule -> rule.action().equals("break_block") && rule.matchesBlock(s));
                QuestService.onBreak(player, s);
                cut++;
            }
            if (cut > 0) {
                player.getMainHandItem().hurtAndBreak(Math.max(1, cut / 4), player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                level.playSound(null, origin, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);
            }
        }

        // 植林の心得：根元を切ったら苗木を植え直す
        SkillDefinition replant = SkillService.passive(player, "replant");
        if (replant != null && level.getBlockState(origin.below()).is(BlockTags.DIRT)) {
            Block sapling = saplingFor(state.getBlock());
            if (sapling != null) {
                level.getServer().execute(() -> {
                    if (level.getBlockState(origin).isAir()) level.setBlockAndUpdate(origin, sapling.defaultBlockState());
                });
            }
        }
    }

    /** つながった同じ種類の原木を探す（斜めも含む、上と横方向へ） */
    private static List<BlockPos> findTree(ServerLevel level, BlockPos origin, Block log) {
        List<BlockPos> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        seen.add(origin);
        while (!queue.isEmpty() && out.size() < MAX_LOGS) {
            BlockPos p = queue.poll();
            for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                BlockPos n = p.offset(dx, dy, dz);
                if (seen.add(n) && level.getBlockState(n).is(log)) {
                    out.add(n);
                    queue.add(n);
                }
            }
        }
        return out;
    }

    /** オークの原木 → オークの苗木 のように名前から探す */
    private static Block saplingFor(Block log) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(log);
        String path = id.getPath().replace("stripped_", "").replace("_log", "_sapling").replace("_wood", "_sapling");
        ResourceLocation sid = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), path);
        return BuiltInRegistries.BLOCK.containsKey(sid) ? BuiltInRegistries.BLOCK.get(sid) : null;
    }

    /** 森の恵みなど：決まったブロックを壊したとき、たまにドロップが増える（target にタグ） */
    @SubscribeEvent
    public static void onDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) return;
        SkillDefinition extra = SkillService.passive(player, "extra_drop");
        if (extra == null || !matches(extra.target(), event.getState())) return;
        if (player.getRandom().nextDouble() >= extra.chance()) return;
        List<ItemEntity> add = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            ItemStack copy = drop.getItem().copy();
            add.add(new ItemEntity(event.getLevel(), drop.getX(), drop.getY(), drop.getZ(), copy));
        }
        event.getDrops().addAll(add);
        event.getLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, event.getPos().getX() + 0.5, event.getPos().getY() + 0.5,
                event.getPos().getZ() + 0.5, 5, 0.3, 0.3, 0.3, 0);
    }

    private static boolean matches(String target, BlockState state) {
        if (target.isEmpty() || target.equals("*")) return true;
        if (target.startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(target.substring(1));
            return tag != null && state.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, tag));
        }
        return target.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
    }

    private SkillEvents() {}
}
