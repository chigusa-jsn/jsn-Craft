package com.chigusa.jsncraft.combat;

import com.chigusa.jsncraft.magic.MageSpells;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillEffects;
import com.chigusa.jsncraft.skill.TimedFlags;
import com.chigusa.jsncraft.stat.ModAttributes;
import com.chigusa.jsncraft.vfx.Circle;
import com.chigusa.jsncraft.vfx.CircleOptions;
import com.chigusa.jsncraft.vfx.Vfx;
import com.chigusa.jsncraft.weapon.JobBowItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.function.Consumer;

/**
 * 射手（風読みの射手・天穿の弓聖）の技。技の矢は本物の矢（拾えない）。
 * 矢のダメージ ＝ 弓の強さ（木の長弓・バニラの弓で6）× power × (1 + スキルダメージ)。
 */
public final class BowSkills {

    /** 技の矢につける印（ArcherEvents が読む） */
    public static final String TAG = "jsn_bow";
    static final DustParticleOptions GOLD_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.9F, 0.5F), 1.3F),
            WIND_DUST = new DustParticleOptions(new Vector3f(0.65F, 1.0F, 0.8F), 1.0F);

    public static boolean cast(ServerPlayer p, SkillDefinition s) {
        return switch (s.effect()) {
            case "bow_double_shot" -> doubleShot(p, s);
            case "bow_power_shot" -> powerShot(p, s);
            case "bow_back_step" -> backStep(p, s);
            case "bow_arrow_rain" -> arrowRain(p, s);
            case "bow_blast_arrow" -> blastArrow(p, s);
            case "bow_snipe" -> snipe(p, s);
            case "bow_wind_read" -> windRead(p, s);
            case "bow_arrow_storm" -> arrowStorm(p, s);
            case "bow_meteor_arrow" -> meteorArrow(p, s);
            case "bow_sky_pierce" -> skyPierce(p, s);
            default -> false;
        };
    }

    // ================= 共通 =================

    /** 弓の強さ（スキルダメージを入れない） */
    static float bowBase(ServerPlayer p) {
        return p.getMainHandItem().getItem() instanceof JobBowItem bow ? (float) (6 * bow.spec().damage()) : 6.0F;
    }

    /** 矢1本のダメージ（スキルダメージ込み） */
    static float arrowDamage(ServerPlayer p, double power) {
        return bowBase(p) * (float) power * (float) (1 + p.getAttributeValue(ModAttributes.SKILL_DAMAGE));
    }

    /**
     * 技の矢を撃つ。dmg はだいたいの当たりダメージ（速さで割って矢に入れる）。
     * withWeapon なら弓の魔紋（追尾・分裂など）も効く。
     */
    static Arrow fire(ServerPlayer p, Vec3 from, Vec3 dir, float speed, float dmg, int pierce, boolean noGravity,
                      boolean withWeapon, Consumer<CompoundTag> tag) {
        ServerLevel level = p.serverLevel();
        Arrow a = new Arrow(level, p, new ItemStack(Items.ARROW), withWeapon ? p.getMainHandItem().copy() : null);
        a.setPos(from.x, from.y, from.z);
        a.shoot(dir.x, dir.y, dir.z, speed, 0.0F);
        a.setBaseDamage(dmg / speed);
        a.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        if (pierce > 0) setPierce(a, pierce);
        CompoundTag t = new CompoundTag();
        if (noGravity) {
            a.setNoGravity(true);
            t.putInt("nograv", 60);   // 3秒たったら落ちる
        }
        if (tag != null) tag.accept(t);
        a.getPersistentData().put(TAG, t);
        level.addFreshEntity(a);
        return a;
    }

    private static java.lang.reflect.Method pierceMethod;

    /** 矢の貫通の数を決める（バニラでは外から呼べないので、名前で呼ぶ） */
    public static void setPierce(AbstractArrow a, int level) {
        try {
            if (pierceMethod == null) {
                pierceMethod = net.neoforged.fml.util.ObfuscationReflectionHelper.findMethod(AbstractArrow.class, "setPierceLevel", byte.class);
            }
            pierceMethod.invoke(a, (byte) Math.max(0, Math.min(127, level)));
        } catch (Exception e) {
            com.chigusa.jsncraft.JsnCraft.LOGGER.warn("[jsn-Craft] 矢の貫通を設定できませんでした", e);
        }
    }

    static Vec3 eye(ServerPlayer p) {
        return p.getEyePosition().add(p.getLookAngle().scale(0.4)).add(0, -0.1, 0);
    }

    static void bowSound(ServerPlayer p, float pitch) {
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ARROW_SHOOT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, pitch);
    }

    /** 上から矢を降らせる（中心 c、半径 r に n 本） */
    static void rain(ServerPlayer p, Vec3 c, double r, int n, float dmg) {
        var rnd = p.getRandom();
        for (int i = 0; i < n; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, d = Math.sqrt(rnd.nextDouble()) * r;
            Vec3 from = c.add(Math.cos(a) * d + (rnd.nextDouble() - 0.5), 12 + rnd.nextDouble() * 3, Math.sin(a) * d + (rnd.nextDouble() - 0.5));
            fire(p, from, new Vec3((rnd.nextDouble() - 0.5) * 0.1, -1, (rnd.nextDouble() - 0.5) * 0.1), 2.2F, dmg, 0, false, false, null);
        }
    }

    // ================= 風読みの射手 =================

    private static boolean doubleShot(ServerPlayer p, SkillDefinition s) {
        float dmg = arrowDamage(p, s.power());
        int n = Math.max(2, s.amplifier());
        for (int i = 0; i < n; i++) {
            SkillEffects.later(p, i * 4, () -> {
                if (!p.isAlive()) return;
                fire(p, eye(p), p.getLookAngle(), 3.0F, dmg, 0, false, true, null);
                bowSound(p, 1.3F);
            });
        }
        return true;
    }

    private static boolean powerShot(ServerPlayer p, SkillDefinition s) {
        float dmg = arrowDamage(p, s.power());
        Arrow a = fire(p, eye(p), p.getLookAngle(), 3.6F, dmg, Math.max(1, s.amplifier()), false, true, t -> t.putFloat("knock", 1.2F));
        a.setCritArrow(true);
        ServerLevel level = p.serverLevel();
        Vec3 e = eye(p);
        level.sendParticles(ParticleTypes.SONIC_BOOM, e.x + p.getLookAngle().x * 1.5, e.y + p.getLookAngle().y * 1.5, e.z + p.getLookAngle().z * 1.5, 1, 0, 0, 0, 0);
        bowSound(p, 0.6F);
        level.playSound(null, p.blockPosition(), SoundEvents.CROSSBOW_SHOOT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.6F);
        return true;
    }

    private static boolean backStep(ServerPlayer p, SkillDefinition s) {
        Vec3 look = p.getLookAngle().multiply(1, 0, 1);
        look = look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize();
        p.setDeltaMovement(-look.x * s.radius(), 0.45, -look.z * s.radius());
        p.hurtMarked = true;
        p.fallDistance = 0;
        p.serverLevel().sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(), 10, 0.3, 0.1, 0.3, 0.03);
        float dmg = arrowDamage(p, s.power());
        SkillEffects.later(p, 3, () -> {
            if (!p.isAlive()) return;
            fire(p, eye(p), p.getLookAngle(), 3.0F, dmg, 0, false, true, null);
            bowSound(p, 1.2F);
        });
        return true;
    }

    private static boolean arrowRain(ServerPlayer p, SkillDefinition s) {
        Vec3 at = ground(p, 24);
        float dmg = arrowDamage(p, s.power());
        int volleys = Math.max(1, s.amplifier());
        Vfx.magicCircle(p.serverLevel(), at, (float) s.radius(), volleys * 10 + 10, Circle.WIND);
        p.serverLevel().sendParticles(ParticleTypes.CRIT, p.getX(), p.getY() + 2, p.getZ(), 15, 0.2, 0.5, 0.2, 0.5);
        bowSound(p, 0.8F);
        MageSpells.repeat(p, 10, volleys, i -> {
            rain(p, at, s.radius(), 6, dmg);
            p.serverLevel().playSound(null, at.x, at.y + 10, at.z, SoundEvents.ARROW_SHOOT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.9F);
        });
        return true;
    }

    private static boolean blastArrow(ServerPlayer p, SkillDefinition s) {
        float dmg = arrowDamage(p, s.power());
        float r = (float) s.radius();
        fire(p, eye(p), p.getLookAngle(), 3.0F, dmg, 0, false, true, t -> {
            t.putFloat("blast", r);
            t.putFloat("blast_dmg", dmg);
        });
        p.serverLevel().sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1.5, p.getZ(), 8, 0.2, 0.2, 0.2, 0.02);
        bowSound(p, 0.9F);
        return true;
    }

    private static boolean snipe(ServerPlayer p, SkillDefinition s) {
        float dmg = arrowDamage(p, s.power());
        Arrow a = fire(p, eye(p), p.getLookAngle(), 5.0F, dmg, 1, true, true, t -> t.putBoolean("trail", true));
        a.setCritArrow(true);
        bowSound(p, 0.5F);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.6F);
        return true;
    }

    private static boolean windRead(ServerPlayer p, SkillDefinition s) {
        TimedFlags.set(p, "archer_wind", s.power(), s.duration());
        ServerLevel level = p.serverLevel();
        MageSpells.repeat(p, 5, (int) Math.round(s.duration() * 4), i ->
                level.sendParticles(WIND_DUST, p.getX(), p.getY() + 1, p.getZ(), 3, 0.5, 0.6, 0.5, 0));
        level.sendParticles(ParticleTypes.GUST, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
        Vfx.ring(level, p.position(), 2.5F, 12, Vfx.GREEN);
        level.playSound(null, p.blockPosition(), SoundEvents.BREEZE_IDLE_AIR, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.2F);
        p.displayClientMessage(Component.literal("風読み：矢がまっすぐ速く飛び、敵を追う").withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    // ================= 天穿の弓聖 =================

    private static boolean arrowStorm(ServerPlayer p, SkillDefinition s) {
        Vec3 at = ground(p, 28);
        float dmg = arrowDamage(p, s.power());
        int pulses = (int) Math.round(s.duration() * 4);
        ServerLevel level = p.serverLevel();
        Vfx.magicCircle(level, at, (float) s.radius(), pulses * 5 + 10, Circle.WIND);
        Vfx.circle(level, at.add(0, 12, 0), Circle.WIND, (float) s.radius(), pulses * 5 + 10, CircleOptions.SKY);
        bowSound(p, 0.7F);
        MageSpells.repeat(p, 5, pulses, i -> {
            rain(p, at, s.radius(), 3, dmg);
            if (i % 4 == 0) level.playSound(null, at.x, at.y + 10, at.z, SoundEvents.ARROW_SHOOT, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.8F);
        });
        return true;
    }

    private static boolean meteorArrow(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 from = eye(p), look = p.getLookAngle();
        BlockHitResult bh = level.clip(new ClipContext(from, from.add(look.scale(s.radius())), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 to = bh.getLocation();
        float dmg = bowBase(p) * (float) s.power();   // hit() がスキルダメージをかける
        int n = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(1.2), e -> SkillEffects.isEnemy(p, e))) {
            if (e.getBoundingBox().inflate(0.9).clip(from, to).isEmpty()) continue;
            SkillEffects.hit(p, e, dmg);
            n++;
        }
        double len = to.distanceTo(from);
        for (double t = 0; t < len; t += 0.5) {
            Vec3 q = from.add(look.scale(t));
            level.sendParticles(ParticleTypes.END_ROD, q.x, q.y, q.z, 1, 0.03, 0.03, 0.03, 0.01);
            level.sendParticles(GOLD_DUST, q.x, q.y, q.z, 1, 0.1, 0.1, 0.1, 0);
            if ((int) (t * 2) % 6 == 0) level.sendParticles(ParticleTypes.FIREWORK, q.x, q.y, q.z, 1, 0.1, 0.1, 0.1, 0.02);
        }
        level.sendParticles(ParticleTypes.FLASH, to.x, to.y, to.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.SONIC_BOOM, from.x + look.x * 2, from.y + look.y * 2, from.z + look.z * 2, 1, 0, 0, 0, 0);
        level.playSound(null, p.blockPosition(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 0.7F);
        level.playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 2.0F);
        p.displayClientMessage(Component.literal("流星の矢：" + n + "体を貫いた").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    private static boolean skyPierce(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 at = ground(p, 32);
        double r = s.radius();
        float dmg = bowBase(p) * (float) s.power();
        // 空へ撃ち上げる
        for (int h = 0; h < 20; h++) level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 2 + h, p.getZ(), 1, 0.05, 0.1, 0.05, 0.02);
        bowSound(p, 0.5F);
        level.playSound(null, p.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 0.6F);
        Vfx.magicCircle(level, at, (float) r, 40, Circle.WIND);
        Vfx.circle(level, at.add(0, 18, 0), Circle.WIND, (float) r * 0.8F, 40, CircleOptions.SKY | CircleOptions.CHARGE);
        // 巨大な光の矢が落ちてくる
        int fall = 8;
        for (int i = 0; i < fall; i++) {
            int k = i;
            SkillEffects.later(p, 20 + i, () -> {
                double y = 24 - 24.0 * (k + 1) / fall;
                for (int j = 0; j < 6; j++) {
                    level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + y + j * 0.8, at.z, 3, 0.25, 0.2, 0.25, 0.01);
                    level.sendParticles(GOLD_DUST, at.x, at.y + y + j * 0.8, at.z, 4, 0.35, 0.2, 0.35, 0);
                }
            });
        }
        SkillEffects.later(p, 20 + fall, () -> {
            if (!p.isAlive()) return;
            int n = 0;
            for (LivingEntity e : AxeSkills.around(p, at, r)) {
                SkillEffects.hit(p, e, dmg);
                Vec3 push = e.position().subtract(at).normalize().scale(1.2);
                e.push(push.x, 0.7, push.z);
                e.hurtMarked = true;
                n++;
            }
            rain(p, at, r, 16, arrowDamage(p, 0.5));
            level.sendParticles(ParticleTypes.FLASH, at.x, at.y + 1, at.z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1, at.z, 150, r * 0.3, 0.5, r * 0.3, 0.5);
            level.sendParticles(GOLD_DUST, at.x, at.y + 1, at.z, 100, r * 0.5, 1.0, r * 0.5, 0);
            Vfx.ring(level, at, (float) r * 1.5F, 18, Vfx.GOLD);
            Vfx.ring(level, at, (float) r, 14, Vfx.WHITE);
            level.playSound(null, BlockPos.containing(at), SoundEvents.LIGHTNING_BOLT_IMPACT, net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 0.6F);
            level.playSound(null, BlockPos.containing(at), SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 0.8F);
            p.displayClientMessage(Component.literal("天穿：" + n + "体に命中").withStyle(ChatFormatting.GOLD), true);
        });
        return true;
    }

    /** 目線の先の地面 */
    static Vec3 ground(ServerPlayer p, double range) {
        ServerLevel level = p.serverLevel();
        Vec3 e = p.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(e, e.add(p.getLookAngle().scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 t = hit.getType() == HitResult.Type.MISS ? hit.getLocation() : hit.getLocation().subtract(p.getLookAngle().scale(0.1));
        for (int i = 0; i < 16; i++) {
            BlockPos below = BlockPos.containing(t.x, t.y - 0.5, t.z);
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) break;
            t = t.subtract(0, 1, 0);
        }
        return new Vec3(t.x, Math.floor(t.y + 0.01), t.z);
    }

    private BowSkills() {}
}
