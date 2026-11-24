package com.chigusa.jsncraft.weapon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 鞭の当たり判定：伸びた鞭の線の上にいる敵、ぜんぶに当たる（狙った1体だけでなく、手前から先っぽまで）。
 * 照準の先の1体はいつもの攻撃で当たるので、それ以外を鞭の線で叩く。空振りでも線の上の敵には当たる。
 */
public final class WhipService {

    /** 線の太さ（敵の当たり箱をこれだけ広げて線と重なるか見る） */
    private static final double WIDTH = 0.6;
    private static final int MAX_TARGETS = 6;
    private static final Map<UUID, Long> LAST = new HashMap<>();

    private static boolean enemyOf(ServerPlayer p, Entity e) {
        if (!(e instanceof LivingEntity le) || e == p || !le.isAlive() || e instanceof ArmorStand || e instanceof Player) return false;
        if (e.isAlliedTo(p)) return false;
        if (e instanceof OwnableEntity own && own.getOwnerUUID() != null) return false;   // 相棒・ペットには当てない
        return true;
    }

    /** 振った（攻撃ボタン）ときに呼ぶ */
    public static void lash(ServerPlayer p) {
        if (!(p.getMainHandItem().getItem() instanceof WhipItem)) return;
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        double speed = Math.max(0.5, p.getAttributeValue(Attributes.ATTACK_SPEED));
        double full = 20.0 / speed;
        long last = LAST.getOrDefault(p.getUUID(), 0L);
        double charge = Math.min(1.0, (now - last) / full);
        if (charge < 0.25) return;   // 連打しすぎ
        LAST.put(p.getUUID(), now);

        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        double reach = p.entityInteractionRange();
        Vec3 end = eye.add(look.scale(reach));
        // 壁の向こうには届かない
        var block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        Vec3 stop = end;

        AABB box = new AABB(eye, stop).inflate(WIDTH + 1);
        List<LivingEntity> hits = new ArrayList<>();
        for (Entity e : level.getEntities(p, box, e -> enemyOf(p, e))) {
            if (e.getBoundingBox().inflate(WIDTH).clip(eye, stop).isPresent() || e.getBoundingBox().inflate(WIDTH).contains(eye)) hits.add((LivingEntity) e);
        }
        if (hits.isEmpty()) return;
        hits.sort(Comparator.comparingDouble(e -> e.distanceToSqr(eye)));
        // いちばん手前の1体は照準で狙っている（いつもの攻撃で当たる）ので外す
        Entity aimed = aimed(p, eye, stop);
        if (aimed != null) hits.remove(aimed);
        if (hits.isEmpty()) return;

        double dmg = p.getAttributeValue(Attributes.ATTACK_DAMAGE) * (0.2 + 0.8 * charge * charge);
        int n = 0;
        for (LivingEntity e : hits) {
            if (n++ >= MAX_TARGETS) break;
            if (e.hurt(p.damageSources().playerAttack(p), (float) dmg)) {
                e.knockback(0.25, -look.x, -look.z);
                level.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 6, 0.2, 0.2, 0.2, 0.1);
            }
        }
        p.getMainHandItem().hurtAndBreak(1, p, EquipmentSlot.MAINHAND);
    }

    /** 照準の先の敵（いつもの攻撃が当たる相手） */
    private static Entity aimed(ServerPlayer p, Vec3 eye, Vec3 end) {
        Entity best = null;
        double bestD = Double.MAX_VALUE;
        for (Entity e : p.level().getEntities(p, new AABB(eye, end).inflate(1), e -> e.isPickable() && !e.isSpectator())) {
            var hit = e.getBoundingBox().inflate(e.getPickRadius()).clip(eye, end);
            if (hit.isEmpty()) continue;
            double d = eye.distanceToSqr(hit.get());
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    private WhipService() {}
}
