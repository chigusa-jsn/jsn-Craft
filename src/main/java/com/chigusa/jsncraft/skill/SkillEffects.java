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

    private static void later(ServerPlayer player, int ticks, Runnable run) {
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
