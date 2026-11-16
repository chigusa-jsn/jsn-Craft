package com.chigusa.jsncraft.skill;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.npc.ReceptionistEntity;
import com.chigusa.jsncraft.party.PartyManager;
import com.chigusa.jsncraft.vfx.Vfx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * アクティブスキルの中身。effect の名前ごとに動きを書く。
 * 見た目は jsn-Craft 専用のエフェクト（Vfx）を中心に、バニラの粒と音を重ねている。
 */
public final class SkillEffects {

    /** スキルを使う。使えない場面なら false（スタミナや待ち時間は減らない）。 */
    public static boolean activate(ServerPlayer player, SkillDefinition s) {
        return switch (s.effect()) {
            case "sweep" -> sweep(player, s);
            case "dash" -> dash(player, s);
            case "slash_line" -> slashLine(player, s);
            case "whirlwind" -> whirlwind(player, s);
            case "parry" -> parry(player, s);
            case "blink_slash" -> blinkSlash(player, s);
            case "buff" -> buff(player, s);
            case "potion" -> potion(player, s);
            case "heal_area" -> healArea(player, s);
            case "leap_slam" -> leapSlam(player, s);
            case "flurry" -> flurry(player, s);
            case "moon_fall" -> moonFall(player, s);
            case "leaf_clear" -> leafClear(player, s);
            case "grow_area" -> growArea(player, s);
            case "reveal_logs" -> revealLogs(player, s);
            case "mass_fell" -> massFell(player, s);
            case "grove" -> grove(player, s);
            case "summon_guardian" -> summonGuardian(player, s);
            case "reveal_ores" -> com.chigusa.jsncraft.mining.MiningService.revealOres(player, s);
            case "power_dig" -> com.chigusa.jsncraft.mining.MiningService.powerDig(player, s);
            case "wide_dig" -> com.chigusa.jsncraft.mining.MiningService.wideDig(player, s);
            case "blast_mine" -> com.chigusa.jsncraft.mining.MiningService.blastMine(player, s);
            case "rain_blessing" -> com.chigusa.jsncraft.farming.FarmingService.rainBlessing(player, s);
            case "mass_sow" -> com.chigusa.jsncraft.farming.FarmingService.massSow(player, s);
            case "harvest_dance" -> com.chigusa.jsncraft.farming.FarmingService.harvestDance(player, s);
            case "scythe_sweep" -> com.chigusa.jsncraft.farming.FarmingService.scytheSweep(player, s);
            case "harvest_ward" -> com.chigusa.jsncraft.farming.FarmingService.harvestWard(player, s);
            case "fish_buff" -> com.chigusa.jsncraft.fishing.FishingService.fishBuff(player, s);
            case "water_blessing" -> com.chigusa.jsncraft.fishing.FishingService.waterBlessing(player, s);
            case "cast_net" -> com.chigusa.jsncraft.fishing.FishingService.castNet(player, s);
            default -> {
                JsnCraft.LOGGER.warn("[jsn-Craft] 知らないスキル効果です: {}", s.effect());
                yield false;
            }
        };
    }

    // ================= 共通の道具 =================

    static float attackDamage(ServerPlayer player) {
        return (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    /** 攻撃してよい相手か（自分・ほかのプレイヤー・NPC・味方のペットはだめ） */
    static boolean isEnemy(ServerPlayer player, LivingEntity e) {
        if (e == player || e instanceof Player || e instanceof ReceptionistEntity || !e.isAlive()) return false;
        if (e instanceof net.minecraft.world.entity.TamableAnimal pet && pet.isOwnedBy(player)) return false;
        return e.isAttackable();
    }

    /** スキルでダメージを与える（数字が蒼く出る）。当たったら火花 */
    static void hit(ServerPlayer player, LivingEntity e, float damage) {
        HitFlags.SKILL.set(true);
        try {
            e.invulnerableTime = 0; // 連続攻撃でも毎回当たるように
            damage *= (float) (1 + player.getAttributeValue(com.chigusa.jsncraft.stat.ModAttributes.SKILL_DAMAGE));
            e.hurt(player.damageSources().playerAttack(player), damage);
        } finally {
            HitFlags.SKILL.set(false);
        }
        Vfx.sparks(player.serverLevel(), e.getBoundingBox().getCenter(), 8);
    }

    private static Vec3 flatLook(ServerPlayer player) {
        Vec3 look = player.getLookAngle().multiply(1, 0, 1);
        return look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize();
    }

    /** あとで実行する予定（連続攻撃など） */
    private record Task(long at, Runnable run) {}

    private static final List<Task> TASKS = new ArrayList<>();

    public static void later(ServerPlayer player, int ticks, Runnable run) {
        TASKS.add(new Task(player.level().getGameTime() + ticks, run));
    }

    // ================= 斬り払い：前方の敵をまとめてなぎ払う =================

    private static boolean sweep(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        Vec3 look = flatLook(player);
        Vec3 right = new Vec3(-look.z, 0, look.x);
        float damage = attackDamage(player) * (float) s.power();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(s.radius(), 1.0, s.radius()), e -> isEnemy(player, e))) {
            Vec3 to = e.position().subtract(player.position()).multiply(1, 0, 1);
            if (to.lengthSqr() > 0.01 && to.normalize().dot(look) < 0.2) continue; // 前方だけ
            hit(player, e, damage);
            e.knockback(0.6, -look.x, -look.z);
        }
        // 横に流れる三日月を3枚
        Vec3 base = player.position().add(0, 1.1, 0);
        for (int i = -1; i <= 1; i++) {
            Vec3 p = base.add(look.scale(s.radius() * 0.55)).add(right.scale(i * s.radius() * 0.35));
            Vfx.slash(level, p, 1.5F, i == 0 ? Vfx.WHITE : Vfx.AZURE, 90 + i * 25);
        }
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, base.x + look.x * 1.5, base.y, base.z + look.z * 1.5, 1, 0, 0, 0, 0);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.9F);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.6F, 1.6F);
        return true;
    }

    // ================= 踏み込み：前に飛び込む。次の一撃が強くなる =================

    private static boolean dash(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        Vec3 look = flatLook(player);
        player.setDeltaMovement(look.x * s.power(), 0.25, look.z * s.power());
        player.hurtMarked = true; // 動きをクライアントにすぐ伝える
        player.fallDistance = 0;
        if (s.bonus() > 0) {
            PlayerSkillData d = SkillService.data(player);
            d.empowered = s.bonus();
            d.empoweredUntil = level.getGameTime() + 60; // 3秒以内の一撃
        }
        Vfx.ring(level, player.position(), 2.0F, 8, Vfx.WHITE);
        // 走り抜けた跡に白い筋を残す
        for (int t = 1; t <= 4; t++) {
            int delay = t;
            later(player, delay, () -> {
                if (!player.isAlive()) return;
                level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.3, player.getZ(), 3, 0.2, 0.1, 0.2, 0.01);
                Vfx.slash(level, player.position().add(0, 1.0, 0).subtract(look.scale(0.8)), 0.9F, Vfx.WHITE, 90);
            });
        }
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 1.3F);
        level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 0.7F, 1.2F);
        return true;
    }

    // ================= 旋風斬：その場で回転し、周りの敵を何回も斬る =================

    private static boolean whirlwind(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        int hits = Math.max(1, s.amplifier() > 0 ? s.amplifier() : 3);
        float damage = attackDamage(player) * (float) s.power();
        for (int n = 0; n < hits; n++) {
            int pulse = n;
            later(player, n * 6, () -> {
                if (!player.isAlive()) return;
                Vec3 c = player.position();
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(s.radius(), 1.0, s.radius()), e -> isEnemy(player, e))) {
                    if (e.distanceToSqr(player) > s.radius() * s.radius()) continue;
                    hit(player, e, damage);
                    Vec3 away = e.position().subtract(c).multiply(1, 0, 1);
                    if (away.lengthSqr() > 0.01) e.knockback(0.3, -away.normalize().x, -away.normalize().z);
                }
                // まわりを回る三日月（回すたびに角度をずらす）
                for (int k = 0; k < 6; k++) {
                    double a = Math.toRadians(k * 60 + pulse * 30);
                    Vec3 p = c.add(Math.cos(a) * s.radius() * 0.7, 1.0, Math.sin(a) * s.radius() * 0.7);
                    Vfx.slash(level, p, 1.4F, k % 2 == 0 ? Vfx.AZURE : Vfx.WHITE, (float) (k * 60 + pulse * 30));
                }
                Vfx.ring(level, c, (float) s.radius() * 1.1F, 10, Vfx.AZURE);
                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.8F + pulse * 0.15F);
            });
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.6F, 1.6F);
        return true;
    }

    // ================= 受け流し：かまえている間に殴られたら、無効にして反撃 =================

    private static boolean parry(ServerPlayer player, SkillDefinition s) {
        PlayerSkillData d = SkillService.data(player);
        d.parryUntil = player.level().getGameTime() + Math.round(Math.max(0.3, s.duration()) * 20);
        d.parryPower = s.power();
        Vfx.ring(player.serverLevel(), player.position(), 1.6F, 12, Vfx.WHITE);
        Vfx.magicCircle(player.serverLevel(), player.position(), 1.4F, (int) Math.round(Math.max(0.3, s.duration()) * 20), Vfx.WHITE);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 1.0F, 1.3F);
        return true;
    }

    /** 受け流しが決まったとき（SkillEvents から呼ぶ） */
    static void counter(ServerPlayer player, LivingEntity attacker) {
        PlayerSkillData d = SkillService.data(player);
        d.parryUntil = 0;
        ServerLevel level = player.serverLevel();
        hit(player, attacker, attackDamage(player) * (float) d.parryPower);
        Vec3 away = attacker.position().subtract(player.position()).multiply(1, 0, 1);
        if (away.lengthSqr() > 0.01) attacker.knockback(1.0, -away.normalize().x, -away.normalize().z);
        Vec3 mid = player.position().add(attacker.position()).scale(0.5).add(0, 1.2, 0);
        Vfx.sparks(level, mid, 20);
        Vfx.slash(level, mid, 1.6F, Vfx.GOLD, 45);
        Vfx.ring(level, player.position(), 2.5F, 10, Vfx.GOLD);
        level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.2F);
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.4F, 2.0F);
        player.displayClientMessage(Component.literal("✦ 受け流し！ ✦")
                .withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD), true);
    }

    // ================= 居合・一閃：前へ駆け抜け、通り道の敵を斬る =================

    private static boolean blinkSlash(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        Vec3 look = flatLook(player);
        Vec3 start = player.position();
        // 壁にぶつからないところまで進む
        Vec3 end = start;
        for (double t = 0.5; t <= s.radius(); t += 0.5) {
            Vec3 next = start.add(look.scale(t));
            AABB box = player.getBoundingBox().move(next.subtract(start));
            if (!level.noCollision(player, box)) break;
            end = next;
        }
        float damage = attackDamage(player) * (float) s.power();
        AABB path = new AABB(start, end).inflate(1.2, 1.0, 1.2).expandTowards(0, 1.8, 0);
        int count = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, path, e -> isEnemy(player, e))) {
            hit(player, e, damage);
            count++;
        }
        player.teleportTo(end.x, end.y, end.z);
        player.fallDistance = 0;
        // 通り道に一直線の斬撃
        double len = end.distanceTo(start);
        for (double t = 0.5; t <= len; t += 1.0) {
            Vec3 p = start.add(look.scale(t)).add(0, 1.0, 0);
            Vfx.slash(level, p, 1.3F, Vfx.AZURE, 0);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 2, 0.1, 0.2, 0.1, 0.01);
        }
        Vfx.slash(level, end.add(0, 1.1, 0).add(look.scale(0.8)), 2.2F, Vfx.WHITE, 0);
        Vfx.ring(level, end, 2.2F, 10, Vfx.AZURE);
        level.playSound(null, BlockPos.containing(end), SoundEvents.TRIDENT_RIPTIDE_3.value(), SoundSource.PLAYERS, 0.9F, 1.5F);
        level.playSound(null, BlockPos.containing(end), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.8F);
        if (count > 0) {
            player.displayClientMessage(Component.literal("一閃：" + count + "体を斬った").withStyle(net.minecraft.ChatFormatting.AQUA), true);
        }
        return true;
    }

    // ================= 天翔蒼刃：高く跳び、着地の衝撃で周りを打ち上げる =================

    private static boolean leapSlam(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        Vec3 look = flatLook(player);
        double up = s.bonus() > 0 ? s.bonus() : 1.15;
        player.setDeltaMovement(look.x * 0.5, up, look.z * 0.5);
        player.hurtMarked = true;
        Vfx.ring(level, player.position(), 2.5F, 10, Vfx.WHITE);
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2, player.getZ(), 12, 0.4, 0.1, 0.4, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 1.0F, 0.8F);
        waitForLanding(player, s, 0);
        return true;
    }

    /** 地面に着くまで毎tick見て、着いたら叩きつける（最大3秒） */
    private static void waitForLanding(ServerPlayer player, SkillDefinition s, int elapsed) {
        later(player, 1, () -> {
            if (!player.isAlive()) return;
            player.fallDistance = 0; // 落下ダメージなし
            if (elapsed > 5 && (player.onGround() || elapsed > 60)) {
                slam(player, s);
            } else {
                if (elapsed > 8 && player.getDeltaMovement().y < 0.2) {
                    player.setDeltaMovement(player.getDeltaMovement().add(0, -0.25, 0)); // 頂点を過ぎたら一気に落ちる
                    player.hurtMarked = true;
                }
                player.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.5, player.getZ(), 2, 0.2, 0.2, 0.2, 0);
                waitForLanding(player, s, elapsed + 1);
            }
        });
    }

    private static void slam(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        Vec3 c = player.position();
        float damage = attackDamage(player) * (float) s.power();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(s.radius(), 2.0, s.radius()), e -> isEnemy(player, e))) {
            if (e.distanceToSqr(player) > s.radius() * s.radius()) continue;
            hit(player, e, damage);
            e.setDeltaMovement(e.getDeltaMovement().add(0, 0.7, 0)); // 打ち上げる
            e.hurtMarked = true;
        }
        Vfx.ring(level, c, (float) s.radius() * 1.2F, 12, Vfx.AZURE);
        Vfx.ring(level, c, (float) s.radius() * 0.7F, 8, Vfx.WHITE);
        Vfx.magicCircle(level, c, (float) s.radius() * 0.8F, 16, Vfx.AZURE);
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(k * 45);
            Vec3 p = c.add(Math.cos(a) * s.radius() * 0.6, 0.6, Math.sin(a) * s.radius() * 0.6);
            Vfx.slash(level, p, 1.8F, k % 2 == 0 ? Vfx.AZURE : Vfx.WHITE, 90);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.3, c.z, 2, 0.5, 0.1, 0.5, 0);
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8F, 0.6F);
        level.playSound(null, player.blockPosition(), SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 0.7F, 1.2F);
    }

    // ================= 千刃乱舞：目の前の敵に連撃。最後の一撃は大きい =================

    private static boolean flurry(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        int hits = Math.max(2, s.amplifier() > 0 ? s.amplifier() : 8);
        float damage = attackDamage(player) * (float) s.power();
        for (int n = 0; n < hits; n++) {
            int i = n;
            boolean last = n == hits - 1;
            later(player, n * 3, () -> {
                if (!player.isAlive()) return;
                Vec3 look = flatLook(player);
                Vec3 front = player.position().add(look.scale(s.radius() * 0.5)).add(0, 1.1, 0);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(s.radius(), 1.0, s.radius()), e -> isEnemy(player, e))) {
                    Vec3 to = e.position().subtract(player.position()).multiply(1, 0, 1);
                    if (to.lengthSqr() > 0.01 && to.normalize().dot(look) < 0.3) continue;
                    hit(player, e, last ? damage * 3 : damage);
                    if (last) e.knockback(1.2, -look.x, -look.z);
                }
                float roll = (i * 67) % 180;
                if (last) {
                    Vfx.slash(level, front, 3.2F, Vfx.GOLD, 0);
                    Vfx.ring(level, player.position(), 3.0F, 10, Vfx.GOLD);
                    Vfx.sparks(level, front, 25);
                    level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.6F);
                    level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.3F, 1.8F);
                } else {
                    Vec3 jitter = new Vec3(player.getRandom().nextGaussian() * 0.4, player.getRandom().nextGaussian() * 0.3, player.getRandom().nextGaussian() * 0.4);
                    Vfx.slash(level, front.add(jitter), 1.6F, i % 2 == 0 ? Vfx.AZURE : Vfx.WHITE, roll);
                    level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6F, 1.2F + i * 0.08F);
                }
            });
        }
        return true;
    }

    // ================= 奥義・天穹蒼月：狙った場所に、空から蒼い月が落ちる =================

    private static boolean moonFall(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        // 目線の先（最大20ブロック）の地面をねらう
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(20));
        var clip = level.clip(new net.minecraft.world.level.ClipContext(eye, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        Vec3 target = clip.getLocation();
        for (int i = 0; i < 8; i++) { // 空中を狙ったら、下の地面まで下ろす
            BlockPos below = BlockPos.containing(target.x, target.y - 0.5, target.z);
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) break;
            target = target.subtract(0, 1, 0);
        }
        Vec3 ground = target;
        float radius = (float) s.radius();
        int fall = 14;
        Vfx.magicCircle(level, ground, radius, fall + 10, Vfx.AZURE);
        Vfx.magicCircle(level, ground, radius * 0.55F, fall + 10, Vfx.WHITE);
        level.playSound(null, BlockPos.containing(ground), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.6F);
        // 空から三日月が降りてくる
        for (int t = 0; t < fall; t++) {
            int tick = t;
            later(player, t, () -> {
                double h = (fall - tick) * 1.1;
                Vfx.slash(level, ground.add(0, h + 1, 0), 4.5F, Vfx.AZURE, 0);
                Vfx.slash(level, ground.add(0, h + 1.3, 0), 3.2F, Vfx.WHITE, 0);
                level.sendParticles(ParticleTypes.END_ROD, ground.x, ground.y + h + 1, ground.z, 4, 0.8, 0.3, 0.8, 0.01);
            });
        }
        float damage = attackDamage(player) * (float) s.power();
        later(player, fall, () -> {
            if (!player.isAlive()) return;
            int count = 0;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(ground, ground).inflate(radius, 3, radius),
                    e -> isEnemy(player, e))) {
                if (e.position().distanceToSqr(ground) > radius * radius) continue;
                hit(player, e, damage);
                e.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                count++;
            }
            Vfx.ring(level, ground, radius * 1.4F, 16, Vfx.AZURE);
            Vfx.ring(level, ground, radius * 0.9F, 12, Vfx.WHITE);
            for (int k = 0; k < 12; k++) {
                double a = Math.toRadians(k * 30);
                Vec3 p = ground.add(Math.cos(a) * radius * 0.7, 0.8, Math.sin(a) * radius * 0.7);
                Vfx.slash(level, p, 2.2F, k % 2 == 0 ? Vfx.AZURE : Vfx.WHITE, k * 30);
            }
            Vfx.sparks(level, ground.add(0, 1, 0), 40);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, ground.x, ground.y + 0.5, ground.z, 1, 0, 0, 0, 0);
            level.playSound(null, BlockPos.containing(ground), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0F, 0.8F);
            level.playSound(null, BlockPos.containing(ground), SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 1.0F, 0.7F);
            player.displayClientMessage(Component.literal("天穹蒼月：" + count + "体に命中").withStyle(net.minecraft.ChatFormatting.AQUA), true);
        });
        return true;
    }

    // ================= 伐り手：落葉払い（周りの葉を一気に払う） =================

    private static boolean leafClear(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        int r = (int) Math.round(s.radius());
        BlockPos c = player.blockPosition();
        int count = 0;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -2, -r), c.offset(r, r + 6, r))) {
            if (count >= 400) break;
            var state = level.getBlockState(p);
            if (!state.is(net.minecraft.tags.BlockTags.LEAVES)) continue;
            if (p.distSqr(c) > (r + 3) * (r + 3)) continue;
            level.destroyBlock(p.immutable(), true, player);
            count++;
        }
        if (count == 0) {
            player.displayClientMessage(Component.literal("近くに葉っぱがありません"), true);
            return false;
        }
        Vfx.ring(level, player.position(), r * 1.2F, 12, Vfx.GREEN);
        level.sendParticles(ParticleTypes.CHERRY_LEAVES, player.getX(), player.getY() + 3, player.getZ(), 40, r * 0.5, 2, r * 0.5, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.4F, 1.6F);
        player.displayClientMessage(Component.literal("落葉払い：" + count + "枚の葉を払った").withStyle(net.minecraft.ChatFormatting.GREEN), true);
        return true;
    }

    // ================= 伐り手：苗木の祝福（周りの苗木や作物を一気に育てる） =================

    private static boolean growArea(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        int r = (int) Math.round(s.radius());
        int times = Math.max(1, s.amplifier() > 0 ? s.amplifier() : 2);
        BlockPos c = player.blockPosition();
        int grown = 0;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -2, -r), c.offset(r, 3, r))) {
            BlockPos pos = p.immutable();
            boolean any = false;
            for (int t = 0; t < times; t++) {
                if (net.minecraft.world.item.BoneMealItem.growCrop(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BONE_MEAL), level, pos)) any = true;
            }
            if (any) {
                level.levelEvent(1505, pos, 0); // 骨粉のキラキラ
                grown++;
            }
        }
        Vfx.magicCircle(level, player.position(), r, 24, Vfx.GREEN);
        Vfx.ring(level, player.position(), r * 1.2F, 14, Vfx.GREEN);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.4F);
        player.displayClientMessage(Component.literal("苗木の祝福：" + grown + "か所が育った").withStyle(net.minecraft.ChatFormatting.GREEN), true);
        return true;
    }

    // ================= 伐り手：木霊の導き（周りの木の場所を光らせる。自分にだけ見える） =================

    private static boolean revealLogs(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        int r = (int) Math.round(s.radius());
        BlockPos c = player.blockPosition();
        List<BlockPos> roots = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -8, -r), c.offset(r, 8, r))) {
            var state = level.getBlockState(p);
            if (state.is(net.minecraft.tags.BlockTags.LOGS) && !level.getBlockState(p.below()).is(net.minecraft.tags.BlockTags.LOGS)) {
                roots.add(p.immutable());
                if (roots.size() >= 60) break;
            }
        }
        int ticks = (int) Math.round(Math.max(2, s.duration()) * 20);
        for (int t = 0; t < ticks; t += 10) {
            later(player, t, () -> {
                for (BlockPos p : roots) {
                    if (!level.getBlockState(p).is(net.minecraft.tags.BlockTags.LOGS)) continue;
                    // 自分にだけ、光の柱を見せる
                    level.sendParticles(player, ParticleTypes.END_ROD, true, p.getX() + 0.5, p.getY() + 1.5, p.getZ() + 0.5, 3, 0.1, 1.2, 0.1, 0.0);
                }
            });
        }
        Vfx.ring(level, player.position(), r * 0.5F, 16, Vfx.GREEN);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_STEP, SoundSource.PLAYERS, 1.0F, 1.2F);
        player.displayClientMessage(Component.literal("木霊の導き：" + roots.size() + "本の木を見つけた").withStyle(net.minecraft.ChatFormatting.GREEN), true);
        return true;
    }

    // ================= 古樹の守り人：千年樹伐り（周りの木をまとめて倒す） =================

    private static boolean massFell(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        int r = (int) Math.round(s.radius());
        BlockPos c = player.blockPosition();
        // 周りの木の根元を探す
        List<BlockPos> roots = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -3, -r), c.offset(r, 3, r))) {
            if (level.getBlockState(p).is(net.minecraft.tags.BlockTags.LOGS)
                    && !level.getBlockState(p.below()).is(net.minecraft.tags.BlockTags.LOGS)) roots.add(p.immutable());
        }
        if (roots.isEmpty()) {
            player.displayClientMessage(Component.literal("近くに木がありません"), true);
            return false;
        }
        float perLog = (float) Math.max(0, s.bonus());
        int cut = 0, trees = 0;
        int limit = s.amplifier() > 0 ? s.amplifier() : 300;
        outer:
        for (BlockPos root : roots) {
            var rootState = level.getBlockState(root);
            if (!rootState.is(net.minecraft.tags.BlockTags.LOGS)) continue;
            List<BlockPos> tree = new ArrayList<>();
            tree.add(root);
            tree.addAll(SkillEvents.findTree(level, root, rootState.getBlock(), 128));
            trees++;
            for (BlockPos p : tree) {
                if (cut >= limit) break outer;
                if (perLog > 0 && !com.chigusa.jsncraft.stat.ResourceService.tryUseStamina(player, perLog)) {
                    player.displayClientMessage(Component.literal("スタミナ切れ！"), true);
                    break outer;
                }
                var st = level.getBlockState(p);
                if (!st.is(net.minecraft.tags.BlockTags.LOGS)) continue;
                level.destroyBlock(p, true, player);
                com.chigusa.jsncraft.job.JobService.onAction(player, rule -> rule.action().equals("break_block") && rule.matchesBlock(st));
                com.chigusa.jsncraft.quest.QuestService.onBreak(player, st);
                cut++;
            }
        }
        Vfx.ring(level, player.position(), r * 1.3F, 14, Vfx.GREEN);
        Vfx.magicCircle(level, player.position(), r * 0.8F, 16, Vfx.GOLD);
        for (int k = 0; k < 6; k++) {
            double a = Math.toRadians(k * 60);
            Vfx.slash(level, player.position().add(Math.cos(a) * r * 0.6, 1.0, Math.sin(a) * r * 0.6), 1.8F, Vfx.GREEN, k * 60);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 0.5F);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.6F);
        player.displayClientMessage(Component.literal("千年樹伐り：" + trees + "本の木、" + cut + "個の原木").withStyle(net.minecraft.ChatFormatting.GREEN), true);
        return true;
    }

    // ================= 古樹の守り人：守りの森（結界：仲間は回復、敵は遅くなる） =================

    private static boolean grove(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position();
        float radius = (float) s.radius();
        int ticks = (int) Math.round(Math.max(2, s.duration()) * 20);
        Vfx.magicCircle(level, center, radius, ticks, Vfx.GREEN);
        PartyManager.Party party = PartyManager.partyOf(player);
        for (int t = 0; t < ticks; t += 20) {
            later(player, t, () -> {
                for (ServerPlayer p : level.players()) {
                    boolean ally = p == player || (party != null && party.members.contains(p.getUUID()));
                    if (ally && p.position().distanceTo(center) <= radius) {
                        p.heal((float) s.power());
                        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(), 4, 0.3, 0.5, 0.3, 0);
                    }
                }
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius, 3, radius),
                        e -> isEnemy(player, e))) {
                    if (e.position().distanceTo(center) > radius) continue;
                    e.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                }
                Vfx.ring(level, center, radius, 14, Vfx.GREEN);
                level.sendParticles(ParticleTypes.CHERRY_LEAVES, center.x, center.y + 3, center.z, 15, radius * 0.5, 1, radius * 0.5, 0.02);
            });
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.3F);
        return true;
    }

    // ================= 古樹の守り人：奥義・樹霊招来（樹の精霊が一緒に戦う） =================

    private static boolean summonGuardian(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        Vec3 look = flatLook(player);
        Vec3 pos = player.position().add(look.scale(2));
        var golem = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        if (golem == null) return false;
        golem.moveTo(pos.x, pos.y, pos.z, player.getYRot(), 0);
        golem.setPlayerCreated(true);
        golem.setCustomName(Component.literal("樹霊").withStyle(net.minecraft.ChatFormatting.GREEN));
        golem.setCustomNameVisible(true);
        var hp = golem.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(100 * Math.max(0.5, s.power()));
        var atk = golem.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) atk.setBaseValue(15 * Math.max(0.5, s.power()));
        golem.setHealth(golem.getMaxHealth());
        golem.addTag("jsn_craft_guardian");
        level.addFreshEntity(golem);
        Vfx.magicCircle(level, pos, 2.5F, 30, Vfx.GREEN);
        Vfx.ring(level, pos, 4.0F, 16, Vfx.GREEN);
        level.sendParticles(ParticleTypes.CHERRY_LEAVES, pos.x, pos.y + 2, pos.z, 40, 1, 1.5, 1, 0.05);
        level.playSound(null, BlockPos.containing(pos), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 1.0F, 0.7F);
        level.playSound(null, BlockPos.containing(pos), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 0.8F);
        // 時間がたったら森へ帰る
        int ticks = (int) Math.round(Math.max(5, s.duration()) * 20);
        later(player, ticks, () -> {
            if (!golem.isAlive()) return;
            level.sendParticles(ParticleTypes.CHERRY_LEAVES, golem.getX(), golem.getY() + 1.5, golem.getZ(), 40, 0.8, 1.2, 0.8, 0.05);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, golem.getX(), golem.getY() + 1.5, golem.getZ(), 20, 0.8, 1.2, 0.8, 0);
            golem.discard();
        });
        return true;
    }

    // ================= 蒼閃：青い三日月の斬撃が前へ飛び、通り道の敵を斬る =================

    private static final class Slash {
        final ServerLevel level;
        final UUID owner;
        final Vec3 dir;
        final double length;
        final float damage;
        final Set<Integer> hit = new HashSet<>();
        Vec3 pos;
        double traveled;

        Slash(ServerLevel level, UUID owner, Vec3 pos, Vec3 dir, double length, float damage) {
            this.level = level;
            this.owner = owner;
            this.pos = pos;
            this.dir = dir;
            this.length = length;
            this.damage = damage;
        }
    }

    private static final List<Slash> SLASHES = new ArrayList<>();
    private static final DustParticleOptions AZURE_DUST = new DustParticleOptions(new Vector3f(0.35F, 0.7F, 1.0F), 1.4F);
    private static final double SLASH_SPEED = 1.5;

    private static boolean slashLine(ServerPlayer player, SkillDefinition s) {
        Vec3 dir = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(0, -0.3, 0).add(dir.scale(0.8));
        SLASHES.add(new Slash(player.serverLevel(), player.getUUID(), start, dir, s.radius(), attackDamage(player) * (float) s.power()));
        Vfx.ring(player.serverLevel(), player.position(), 2.4F, 10, Vfx.AZURE);
        player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 1.0F, 1.4F);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.7F);
        return true;
    }

    private static void tickSlashes(MinecraftServer server) {
        Iterator<Slash> it = SLASHES.iterator();
        while (it.hasNext()) {
            Slash sl = it.next();
            ServerPlayer owner = server.getPlayerList().getPlayer(sl.owner);
            if (owner == null || owner.serverLevel() != sl.level) {
                it.remove();
                continue;
            }
            boolean done = false;
            for (int step = 0; step < 3 && !done; step++) { // 速くても敵をすり抜けないよう、3回に分けて進む
                sl.pos = sl.pos.add(sl.dir.scale(SLASH_SPEED / 3));
                sl.traveled += SLASH_SPEED / 3;
                BlockPos bp = BlockPos.containing(sl.pos);
                if (!sl.level.getBlockState(bp).getCollisionShape(sl.level, bp).isEmpty()) done = true;
                for (LivingEntity e : sl.level.getEntitiesOfClass(LivingEntity.class, new AABB(sl.pos, sl.pos).inflate(1.4),
                        e -> isEnemy(owner, e) && !sl.hit.contains(e.getId()))) {
                    hit(owner, e, sl.damage);
                    sl.hit.add(e.getId());
                    sl.level.playSound(null, e.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.2F);
                }
                if (sl.traveled >= sl.length) done = true;
            }
            // 大きな三日月が飛んでいく。後ろに青い粒の尾
            Vfx.slash(sl.level, sl.pos, 2.4F, Vfx.AZURE, 0);
            Vfx.slash(sl.level, sl.pos.subtract(sl.dir.scale(0.4)), 1.6F, Vfx.WHITE, 0);
            sl.level.sendParticles(AZURE_DUST, sl.pos.x, sl.pos.y, sl.pos.z, 6, 0.6, 0.2, 0.6, 0);
            if (done) {
                Vfx.ring(sl.level, sl.pos.subtract(0, 1, 0), 2.0F, 8, Vfx.AZURE);
                owner.displayClientMessage(Component.literal("蒼閃：" + sl.hit.size() + "体に命中").withStyle(net.minecraft.ChatFormatting.AQUA), true);
                it.remove();
            }
        }
    }

    // ================= 能力アップ・ポーション・回復 =================

    private record Buff(UUID player, Holder<Attribute> attribute, ResourceLocation modifier, long until) {}

    private static final Map<String, Buff> BUFFS = new ConcurrentHashMap<>();

    private static boolean buff(ServerPlayer player, SkillDefinition s) {
        ResourceLocation attrId = ResourceLocation.tryParse(s.target());
        Optional<Holder.Reference<Attribute>> attr = attrId == null ? Optional.empty() : BuiltInRegistries.ATTRIBUTE.getHolder(attrId);
        if (attr.isEmpty()) return false;
        AttributeInstance inst = player.getAttribute(attr.get());
        if (inst == null) return false;
        ResourceLocation mod = JsnCraft.id("skill_buff/" + attrId.getNamespace() + "/" + attrId.getPath());
        inst.removeModifier(mod);
        inst.addTransientModifier(new AttributeModifier(mod, s.power(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        long until = player.level().getGameTime() + Math.round(s.duration() * 20);
        BUFFS.put(player.getUUID() + "|" + mod, new Buff(player.getUUID(), attr.get(), mod, until));
        Vfx.ring(player.serverLevel(), player.position(), 2.2F, 12, Vfx.GOLD);
        Vfx.magicCircle(player.serverLevel(), player.position(), 1.6F, 20, Vfx.GOLD);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6F, 1.5F);
        return true;
    }

    /** 能力値を一時的に上げる（ほかの職業のスキルから使う道具）。key は同じ強化を上書きするための名前 */
    public static void attributeBuff(ServerPlayer player, Holder<Attribute> attr, String key, double amount,
                                     AttributeModifier.Operation op, double seconds) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) return;
        ResourceLocation mod = JsnCraft.id("skill_buff/" + key);
        inst.removeModifier(mod);
        inst.addTransientModifier(new AttributeModifier(mod, amount, op));
        long until = player.level().getGameTime() + Math.round(seconds * 20);
        BUFFS.put(player.getUUID() + "|" + mod, new Buff(player.getUUID(), attr, mod, until));
    }

    private static boolean potion(ServerPlayer player, SkillDefinition s) {
        ResourceLocation id = ResourceLocation.tryParse(s.target());
        Optional<Holder.Reference<MobEffect>> effect = id == null ? Optional.empty() : BuiltInRegistries.MOB_EFFECT.getHolder(id);
        if (effect.isEmpty()) return false;
        player.addEffect(new MobEffectInstance(effect.get(), (int) Math.round(s.duration() * 20), s.amplifier()));
        Vfx.magicCircle(player.serverLevel(), player.position(), 2.0F, 30, Vfx.GREEN);
        Vfx.ring(player.serverLevel(), player.position(), 2.5F, 14, Vfx.GREEN);
        player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1, player.getZ(), 15, 0.5, 0.6, 0.5, 0);
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private static boolean healArea(ServerPlayer player, SkillDefinition s) {
        PartyManager.Party party = PartyManager.partyOf(player);
        for (ServerPlayer p : player.serverLevel().players()) {
            boolean ally = p == player || (party != null && party.members.contains(p.getUUID()));
            if (!ally || p.distanceTo(player) > s.radius()) continue;
            p.heal((float) s.power());
            Vfx.ring(player.serverLevel(), p.position(), 1.6F, 12, Vfx.GREEN);
            player.serverLevel().sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.8, p.getZ(), 4, 0.3, 0.2, 0.3, 0);
        }
        Vfx.magicCircle(player.serverLevel(), player.position(), (float) s.radius(), 24, Vfx.GREEN);
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.6F);
        return true;
    }

    // ================= 毎tickの処理 =================

    /** 予定の実行、飛んでいる斬撃、時間切れの能力アップ（SkillEvents から毎tick呼ぶ） */
    public static void tickBuffs(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        List<Task> due = new ArrayList<>();
        TASKS.removeIf(t -> {
            if (t.at() > now) return false;
            due.add(t);
            return true;
        });
        for (Task t : due) t.run().run();

        tickSlashes(server);

        Iterator<Buff> it = BUFFS.values().iterator();
        while (it.hasNext()) {
            Buff b = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(b.player());
            if (p == null) {
                it.remove();
                continue;
            }
            if (now < b.until()) continue;
            AttributeInstance inst = p.getAttribute(b.attribute());
            if (inst != null) inst.removeModifier(b.modifier());
            it.remove();
        }
    }

    private SkillEffects() {}
}
