package com.chigusa.jsncraft.magic;

import com.chigusa.jsncraft.magic.MageSpells.Elem;
import com.chigusa.jsncraft.party.PartyManager;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillEffects;
import com.chigusa.jsncraft.stat.ModAttributes;
import com.chigusa.jsncraft.vfx.Circle;
import com.chigusa.jsncraft.vfx.CircleOptions;
import com.chigusa.jsncraft.vfx.Vfx;
import com.chigusa.jsncraft.weapon.ModWeapons;
import com.chigusa.jsncraft.weapon.SpiritRockBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.chigusa.jsncraft.magic.MageSpells.*;

/**
 * 精霊の術師（精霊の術師・四大精霊の契約者）の魔法：風・土・水・闇。敵を動かし、場を操る。
 * 組み合わせ：濡れた敵に土＝泥沼、宙に浮いた敵に土＝叩きつけ、濡れた敵に雷＝感電、濡れた敵に氷＝瞬間凍結、
 * 呪いの敵には全部の魔法 +25%、精霊の岩壁を風で飛ばす＝岩の雨。
 */
public final class SpiritSpells {

    static final DustParticleOptions WIND_DUST = dust(0xA0FFD0, 1.2F), EARTH_DUST = dust(0xC09060, 1.6F),
            WATER_DUST = dust(0x4A90F0, 1.4F), SHADOW_DUST = dust(0x6A30C0, 1.6F), VOID_DUST = dust(0x1A0A30, 2.4F);

    private static DustParticleOptions dust(int c, float s) {
        return new DustParticleOptions(new Vector3f(((c >> 16) & 255) / 255F, ((c >> 8) & 255) / 255F, (c & 255) / 255F), s);
    }

    public static boolean cast(ServerPlayer p, SkillDefinition s) {
        return switch (s.effect()) {
            case "spirit_wind_cutter" -> windCutter(p, s);
            case "spirit_gale" -> gale(p, s);
            case "spirit_aero_step" -> aeroStep(p, s);
            case "spirit_tornado" -> tornado(p, s);
            case "spirit_tempest" -> tempest(p, s);
            case "spirit_stone_bullet" -> stoneBullet(p, s);
            case "spirit_earth_wall" -> earthWall(p, s);
            case "spirit_stone_skin" -> stoneSkin(p, s);
            case "spirit_ground_spike" -> groundSpike(p, s);
            case "spirit_earthquake" -> earthquake(p, s);
            case "spirit_aqua_shot" -> aquaShot(p, s);
            case "spirit_healing_rain" -> healingRain(p, s);
            case "spirit_aqua_veil" -> aquaVeil(p, s);
            case "spirit_maelstrom" -> maelstrom(p, s);
            case "spirit_tidal_wave" -> tidalWave(p, s);
            case "spirit_shadow_bolt" -> shadowBolt(p, s);
            case "spirit_curse" -> curseSpell(p, s);
            case "spirit_drain" -> drain(p, s);
            case "spirit_shadow_bind" -> shadowBind(p, s);
            case "spirit_black_hole" -> blackHole(p, s);
            default -> false;
        };
    }

    private static Vec3 flatLook(ServerPlayer p) {
        Vec3 f = p.getLookAngle().multiply(1, 0, 1);
        return f.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : f.normalize();
    }

    /** 岩の雨：近くの精霊の岩壁を風で砕き、岩のかけらを近くの敵へ飛ばす */
    private static void rockRain(ServerPlayer p, Vec3 center, double r, float dmg) {
        ServerLevel level = p.serverLevel();
        List<BlockPos> rocks = new ArrayList<>();
        BlockPos c = BlockPos.containing(center);
        int R = (int) Math.ceil(r);
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-R, -2, -R), c.offset(R, 3, R))) {
            if (level.getBlockState(q).is(ModWeapons.SPIRIT_ROCK.get())) rocks.add(q.immutable());
        }
        if (rocks.isEmpty()) return;
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (BlockPos q : rocks) {
            level.removeBlock(q, false);
            Vec3 from = Vec3.atCenterOf(q);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, stone), from.x, from.y, from.z, 12, 0.3, 0.3, 0.3, 0.1);
            LivingEntity t = level.getEntitiesOfClass(LivingEntity.class, new AABB(from, from).inflate(10), e -> SkillEffects.isEnemy(p, e))
                    .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(from))).orElse(null);
            if (t != null) {
                Vec3 v = t.getBoundingBox().getCenter().subtract(from).normalize().scale(1.4);
                shoot(p, from, v, 12, 1, 0.3, EARTH_DUST, e -> magicHit(p, e, dmg, Elem.EARTH), at -> {});
            }
        }
        sound(p, center, SoundEvents.STONE_BREAK, 1.5F, 0.7F);
        p.displayClientMessage(Component.literal("岩の雨！").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
    }

    // ================= 風 =================

    private static boolean windCutter(ServerPlayer p, SkillDefinition s) {
        float dmg = base(p) * (float) s.power();
        Vec3 look = p.getLookAngle();
        sound(p, p.position(), SoundEvents.BREEZE_SHOOT, 1.0F, 1.4F);
        shoot(p, hand(p), look.scale(2.0), 14, Math.max(1, s.amplifier()), 0, WIND_DUST, e -> {
            magicHit(p, e, dmg, Elem.WIND);
            e.push(look.x * 0.6, 0.15, look.z * 0.6);
            e.hurtMarked = true;
            p.serverLevel().sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + 1, e.getZ(), 1, 0, 0, 0, 0);
        }, at -> p.serverLevel().sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 6, 0.2, 0.2, 0.2, 0.05));
        return true;
    }

    private static boolean gale(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 look = flatLook(p);
        double r = s.radius();
        float dmg = base(p) * (float) s.power();
        for (LivingEntity e : enemies(p, p.position(), r)) {
            Vec3 d = e.position().subtract(p.position()).multiply(1, 0, 1);
            if (d.lengthSqr() > 0.01 && d.normalize().dot(look) < 0.3) continue;   // 前の扇の中だけ
            magicHit(p, e, dmg, Elem.WIND);
            e.setDeltaMovement(look.x * 1.6, 0.7, look.z * 1.6);
            e.hurtMarked = true;
        }
        for (int i = 1; i <= (int) r; i++) {
            Vec3 q = p.position().add(look.scale(i)).add(0, 1, 0);
            level.sendParticles(ParticleTypes.GUST, q.x, q.y, q.z, 1, 0.4, 0.3, 0.4, 0);
            level.sendParticles(WIND_DUST, q.x, q.y, q.z, 6, 0.6, 0.5, 0.6, 0);
        }
        sound(p, p.position(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.2F, 0.8F);
        rockRain(p, p.position().add(look.scale(r * 0.5)), r * 0.6, dmg);
        return true;
    }

    private static boolean aeroStep(ServerPlayer p, SkillDefinition s) {
        Vec3 look = p.getLookAngle();
        Vec3 v = new Vec3(look.x, Math.max(0.15, look.y * 0.6 + 0.25), look.z).normalize().scale(s.power());
        p.setDeltaMovement(v);
        p.hurtMarked = true;
        p.fallDistance = 0;
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, (int) Math.round(s.duration() * 20), 0, false, false, true));
        ServerLevel level = p.serverLevel();
        level.sendParticles(ParticleTypes.GUST, p.getX(), p.getY() + 0.5, p.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(WIND_DUST, p.getX(), p.getY() + 0.5, p.getZ(), 15, 0.4, 0.3, 0.4, 0);
        sound(p, p.position(), SoundEvents.BREEZE_JUMP, 1.0F, 1.2F);
        return true;
    }

    private static boolean tornado(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 dir = flatLook(p);
        Vec3 start = p.position().add(dir.scale(2));
        float dmg = base(p) * (float) s.power();
        double r = s.radius();
        int pulses = (int) Math.round(s.duration() * 4);
        sound(p, start, SoundEvents.BREEZE_IDLE_GROUND, 2.0F, 0.5F);
        MageSpells.repeat(p, 5, pulses, i -> {
            Vec3 c = start.add(dir.scale(i * 0.35));
            for (int h = 0; h < 10; h++) {   // うずまきの柱
                double a = i * 0.9 + h * 0.8, rr = 0.4 + h * 0.18;
                level.sendParticles(ParticleTypes.CLOUD, c.x + Math.cos(a) * rr, c.y + h * 0.5, c.z + Math.sin(a) * rr, 1, 0, 0, 0, 0.01);
                level.sendParticles(WIND_DUST, c.x - Math.cos(a) * rr, c.y + h * 0.5, c.z - Math.sin(a) * rr, 1, 0, 0, 0, 0);
            }
            for (LivingEntity e : enemies(p, c, r)) {
                Vec3 pull = c.subtract(e.position()).multiply(1, 0, 1);
                Vec3 v = pull.lengthSqr() > 0.2 ? pull.normalize().scale(0.3) : Vec3.ZERO;
                e.setDeltaMovement(v.x, 0.45, v.z);
                e.hurtMarked = true;
                if (i % 2 == 0) magicHit(p, e, dmg, Elem.WIND);
            }
            if (i % 4 == 0) rockRain(p, c, r, dmg * 1.5F);
            if (i % 6 == 0) sound(p, c, SoundEvents.BREEZE_WHIRL, 1.2F, 0.6F);
        });
        return true;
    }

    private static boolean tempest(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 at = p.position();
        double r = s.radius();
        float dmg = base(p) * (float) s.power();
        int pulses = (int) Math.round(s.duration() * 2);
        Vfx.magicCircle(level, at, (float) r, pulses * 10, Circle.WIND);
        Vfx.circle(level, at.add(0, 7, 0), Circle.WIND, (float) r, pulses * 10, CircleOptions.SKY);
        MageSpells.repeat(p, 10, pulses, i -> {
            for (int k = 0; k < 24; k++) {
                double a = i * 0.5 + k * Math.PI / 12, rr = r * (0.3 + (k % 3) * 0.3);
                level.sendParticles(ParticleTypes.CLOUD, at.x + Math.cos(a) * rr, at.y + 1 + (k % 5), at.z + Math.sin(a) * rr, 1, 0, 0, 0, 0.02);
            }
            level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, at.x, at.y + 1, at.z, 1, r * 0.3, 0.5, r * 0.3, 0);
            for (LivingEntity e : enemies(p, at, r)) {
                if (i % 2 == 0) e.setDeltaMovement(e.getDeltaMovement().x * 0.3, 0.9, e.getDeltaMovement().z * 0.3);   // 巻き上げる
                else e.setDeltaMovement(e.getDeltaMovement().x * 0.3, -1.4, e.getDeltaMovement().z * 0.3);   // 叩きつける
                e.hurtMarked = true;
                magicHit(p, e, dmg, Elem.WIND);
            }
            if (i % 2 == 0) sound(p, at, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.5F, 0.6F);
            rockRain(p, at, r, dmg * 1.5F);
        });
        return true;
    }

    // ================= 土 =================

    private static boolean stoneBullet(ServerPlayer p, SkillDefinition s) {
        float dmg = base(p) * (float) s.power();
        sound(p, p.position(), SoundEvents.STONE_PLACE, 1.2F, 0.6F);
        BlockParticleOption stone = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState());
        shoot(p, hand(p), p.getLookAngle().scale(1.5), 22, 1, 0, EARTH_DUST, e -> {
            magicHit(p, e, dmg, Elem.EARTH);
            Vec3 l = p.getLookAngle();
            e.push(l.x * 0.5, 0.1, l.z * 0.5);
            e.hurtMarked = true;
        }, at -> {
            p.serverLevel().sendParticles(stone, at.x, at.y, at.z, 15, 0.2, 0.2, 0.2, 0.1);
            sound(p, at, SoundEvents.STONE_BREAK, 1.0F, 0.8F);
        });
        return true;
    }

    private static boolean earthWall(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 look = flatLook(p);
        Vec3 side = new Vec3(-look.z, 0, look.x);
        Vec3 center = groundTarget(p, 3.5);
        int half = Math.max(1, (int) Math.round(s.radius()) / 2);
        int height = Math.max(1, s.amplifier());
        int ticks = (int) Math.round(s.duration() * 20);
        float dmg = base(p) * (float) s.power();
        int placed = 0;
        for (int w = -half; w <= half; w++) {
            Vec3 q = center.add(side.scale(w));
            for (int h = 0; h < height; h++) {
                BlockPos bp = BlockPos.containing(q.x, center.y + h, q.z);
                if (SpiritRockBlock.place(level, bp, ticks)) placed++;
            }
        }
        // 出てきた岩の上の敵は突き上げられる
        for (LivingEntity e : enemies(p, center, half + 1)) {
            Vec3 d = e.position().subtract(center);
            if (Math.abs(d.dot(look)) < 1.2) {
                magicHit(p, e, dmg, Elem.EARTH);
                e.setDeltaMovement(look.x * 0.8, 0.8, look.z * 0.8);
                e.hurtMarked = true;
            }
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), center.x, center.y + 1, center.z, 40, half, 0.8, 0.5, 0.1);
        sound(p, center, SoundEvents.STONE_PLACE, 1.5F, 0.5F);
        if (placed == 0) p.displayClientMessage(Component.literal("ここには岩壁を出せない").withStyle(ChatFormatting.GRAY), true);
        return true;
    }

    private static boolean stoneSkin(ServerPlayer p, SkillDefinition s) {
        SkillEffects.attributeBuff(p, ModAttributes.DAMAGE_REDUCTION, "spirit_stone_skin", s.power(), AttributeModifier.Operation.ADD_VALUE, s.duration());
        SkillEffects.attributeBuff(p, Attributes.KNOCKBACK_RESISTANCE, "spirit_stone_skin_kb", 0.6, AttributeModifier.Operation.ADD_VALUE, s.duration());
        ServerLevel level = p.serverLevel();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), p.getX(), p.getY() + 1, p.getZ(), 40, 0.5, 0.8, 0.5, 0.05);
        Vfx.ring(level, p.position(), 2.0F, 12, Vfx.GOLD);
        sound(p, p.position(), SoundEvents.ANVIL_PLACE, 0.6F, 0.6F);
        return true;
    }

    private static boolean groundSpike(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 dir = flatLook(p);
        Vec3 start = p.position();
        int len = (int) Math.round(s.radius());
        float dmg = base(p) * (float) s.power();
        Set<Integer> hit = new HashSet<>();
        BlockParticleOption stone = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DRIPSTONE_BLOCK.defaultBlockState());
        for (int i = 1; i <= len; i++) {
            int k = i;
            SkillEffects.later(p, i, () -> {
                Vec3 q = start.add(dir.scale(k));
                for (int h = 0; h < 6; h++) level.sendParticles(stone, q.x, q.y + h * 0.3, q.z, 3, 0.15, 0.1, 0.15, 0.05);
                level.sendParticles(ParticleTypes.DUST_PLUME, q.x, q.y + 0.2, q.z, 3, 0.3, 0.1, 0.3, 0.02);
                for (LivingEntity e : enemies(p, q, 1.6)) {
                    if (!hit.add(e.getId())) continue;
                    magicHit(p, e, dmg, Elem.EARTH);
                    e.setDeltaMovement(e.getDeltaMovement().x, 0.9, e.getDeltaMovement().z);
                    e.hurtMarked = true;
                }
                if (k % 3 == 0) sound(p, q, SoundEvents.POINTED_DRIPSTONE_LAND, 1.2F, 0.7F);
            });
        }
        return true;
    }

    private static boolean earthquake(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 at = p.position();
        double r = s.radius();
        float dmg = base(p) * (float) s.power();
        int pulses = Math.max(1, s.amplifier());
        Vfx.magicCircle(level, at, (float) r, pulses * 10 + 10, Circle.EARTH);
        MageSpells.repeat(p, 10, pulses, i -> {
            var rnd = level.random;
            for (int k = 0; k < 20; k++) {
                double a = rnd.nextDouble() * Math.PI * 2, d = Math.sqrt(rnd.nextDouble()) * r;
                BlockPos below = BlockPos.containing(at.x + Math.cos(a) * d, at.y - 0.5, at.z + Math.sin(a) * d);
                BlockState st = level.getBlockState(below);
                if (st.isAir()) st = Blocks.DIRT.defaultBlockState();
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), below.getX() + 0.5, below.getY() + 1.1, below.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0.15);
            }
            for (LivingEntity e : enemies(p, at, r)) {
                if (!e.onGround()) continue;   // 地面にいる敵だけ揺れる
                magicHit(p, e, dmg, Elem.EARTH);
                e.setDeltaMovement(e.getDeltaMovement().x * 0.2, 0.35, e.getDeltaMovement().z * 0.2);
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 3));
            }
            Vfx.ring(level, at, (float) r, 10, Vfx.GOLD);
            sound(p, at, SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 0.4F);
            sound(p, at, SoundEvents.ROOTED_DIRT_BREAK, 2.0F, 0.5F);
        });
        return true;
    }

    // ================= 水 =================

    private static boolean aquaShot(ServerPlayer p, SkillDefinition s) {
        float dmg = base(p) * (float) s.power();
        sound(p, p.position(), SoundEvents.BUCKET_EMPTY, 1.0F, 1.5F);
        shoot(p, hand(p), p.getLookAngle().scale(1.6), 20, 1, 0, ParticleTypes.SPLASH, e -> magicHit(p, e, dmg, Elem.WATER),
                at -> {
                    p.serverLevel().sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 30, 0.4, 0.3, 0.4, 0.2);
                    p.serverLevel().sendParticles(WATER_DUST, at.x, at.y, at.z, 10, 0.3, 0.3, 0.3, 0);
                    sound(p, at, SoundEvents.GENERIC_SPLASH, 0.6F, 1.4F);
                });
        return true;
    }

    private static boolean healingRain(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 at = p.position();
        double r = s.radius();
        int pulses = (int) Math.round(s.duration());
        float heal = (float) (s.power() * (1 + p.getAttributeValue(ModAttributes.HEALING_POWER)));
        PartyManager.Party party = PartyManager.partyOf(p);
        Vfx.magicCircle(level, at, (float) r, pulses * 20, Circle.WATER);
        MageSpells.repeat(p, 20, pulses, i -> {
            level.sendParticles(ParticleTypes.FALLING_WATER, at.x, at.y + 5, at.z, 80, r * 0.6, 0.3, r * 0.6, 0);
            level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 5.5, at.z, 12, r * 0.5, 0.2, r * 0.5, 0.01);
            for (ServerPlayer o : level.players()) {
                boolean ally = o == p || (party != null && party.members.contains(o.getUUID()));
                if (!ally || o.position().distanceTo(at) > r) continue;
                o.heal(heal);
                if (o.isOnFire()) o.clearFire();
                level.sendParticles(ParticleTypes.HEART, o.getX(), o.getY() + 2, o.getZ(), 1, 0.2, 0.1, 0.2, 0);
            }
            for (LivingEntity e : enemies(p, at, r)) e.getPersistentData().putLong("jsn_wet", e.level().getGameTime() + 160);
            if (i % 3 == 0) sound(p, at, SoundEvents.WEATHER_RAIN, 0.8F, 1.2F);
        });
        return true;
    }

    private static boolean aquaVeil(ServerPlayer p, SkillDefinition s) {
        int ticks = (int) Math.round(s.duration() * 20);
        SkillEffects.attributeBuff(p, ModAttributes.DAMAGE_REDUCTION, "spirit_aqua_veil", s.power(), AttributeModifier.Operation.ADD_VALUE, s.duration());
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ticks, 0, false, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, ticks, 0, false, false, true));
        p.clearFire();
        ServerLevel level = p.serverLevel();
        MageSpells.repeat(p, 10, ticks / 10, i ->
                level.sendParticles(ParticleTypes.BUBBLE_POP, p.getX(), p.getY() + 1, p.getZ(), 5, 0.5, 0.7, 0.5, 0.02));
        level.sendParticles(ParticleTypes.SPLASH, p.getX(), p.getY() + 1, p.getZ(), 40, 0.6, 0.8, 0.6, 0.1);
        Vfx.ring(level, p.position(), 2.0F, 12, Vfx.AZURE);
        sound(p, p.position(), SoundEvents.CONDUIT_ACTIVATE, 0.8F, 1.4F);
        return true;
    }

    private static boolean maelstrom(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 at = groundTarget(p, 20).add(0, 0.3, 0);
        double r = s.radius();
        float dmg = base(p) * (float) s.power();
        int pulses = (int) Math.round(s.duration() * 2);
        Vfx.magicCircle(level, at, (float) r, pulses * 10, Circle.WATER);
        sound(p, at, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 2.0F, 0.6F);
        MageSpells.repeat(p, 10, pulses, i -> {
            for (int k = 0; k < 24; k++) {
                double a = i * 0.7 + k * Math.PI / 12, rr = r * (1 - (k % 4) * 0.22);
                level.sendParticles(ParticleTypes.SPLASH, at.x + Math.cos(a) * rr, at.y + 0.2, at.z + Math.sin(a) * rr, 1, 0, 0, 0, 0);
                level.sendParticles(WATER_DUST, at.x + Math.cos(a + 0.3) * rr, at.y + 0.4, at.z + Math.sin(a + 0.3) * rr, 1, 0, 0, 0, 0);
            }
            for (LivingEntity e : enemies(p, at, r + 1)) {
                Vec3 d = at.subtract(e.position()).multiply(1, 0, 1);
                Vec3 tangent = new Vec3(-d.z, 0, d.x);
                Vec3 v = d.lengthSqr() > 0.25 ? d.normalize().scale(0.3).add(tangent.normalize().scale(0.25)) : Vec3.ZERO;
                e.setDeltaMovement(v.x, -0.05, v.z);
                e.hurtMarked = true;
                magicHit(p, e, dmg, Elem.WATER);
            }
        });
        return true;
    }

    private static boolean tidalWave(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 dir = flatLook(p);
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        Vec3 start = p.position();
        int len = (int) Math.round(s.radius());
        double half = 4.5;
        float dmg = base(p) * (float) s.power();
        Set<Integer> hit = new HashSet<>();
        sound(p, start, SoundEvents.ELDER_GUARDIAN_CURSE, 0.6F, 0.5F);
        for (int i = 1; i <= len; i++) {
            int k = i;
            SkillEffects.later(p, i, () -> {
                Vec3 c = start.add(dir.scale(k));
                double h = 3.5 * Math.sin(Math.PI * Math.min(1, k / (double) len * 1.2));
                for (double w = -half; w <= half; w += 0.7) {
                    Vec3 q = c.add(side.scale(w));
                    level.sendParticles(ParticleTypes.SPLASH, q.x, q.y + h, q.z, 4, 0.2, 0.3, 0.2, 0.2);
                    level.sendParticles(WATER_DUST, q.x, q.y + h * 0.5, q.z, 3, 0.2, h * 0.3, 0.2, 0);
                    level.sendParticles(ParticleTypes.BUBBLE_POP, q.x, q.y + 0.5, q.z, 1, 0.2, 0.2, 0.2, 0);
                }
                for (LivingEntity e : enemies(p, c, half + 1)) {
                    Vec3 d = e.position().subtract(c);
                    if (Math.abs(d.dot(dir)) > 1.5 || !hit.add(e.getId())) continue;
                    magicHit(p, e, dmg, Elem.WATER);
                    e.setDeltaMovement(dir.x * 2.0, 0.6, dir.z * 2.0);
                    e.hurtMarked = true;
                }
                if (k % 4 == 0) sound(p, c, SoundEvents.GENERIC_SPLASH, 2.0F, 0.5F);
            });
        }
        return true;
    }

    // ================= 闇 =================

    private static boolean shadowBolt(ServerPlayer p, SkillDefinition s) {
        float dmg = base(p) * (float) s.power();
        sound(p, p.position(), SoundEvents.WITHER_SHOOT, 0.5F, 1.6F);
        shoot(p, hand(p), p.getLookAngle().scale(1.3), 26, 1, 0.15, SHADOW_DUST, e -> magicHit(p, e, dmg, Elem.DARK),
                at -> p.serverLevel().sendParticles(ParticleTypes.SQUID_INK, at.x, at.y, at.z, 10, 0.2, 0.2, 0.2, 0.05));
        return true;
    }

    private static boolean curseSpell(ServerPlayer p, SkillDefinition s) {
        LivingEntity t = lookTarget(p, 24);
        if (t == null) {
            p.displayClientMessage(Component.literal("ねらう敵がいない").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ServerLevel level = p.serverLevel();
        int ticks = (int) Math.round(s.duration() * 20);
        int n = 0;
        for (LivingEntity e : enemies(p, t.position(), s.radius())) {
            MageSpells.curse(e, ticks);
            level.sendParticles(SHADOW_DUST, e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ(), 15, 0.3, 0.2, 0.3, 0);
            level.sendParticles(ParticleTypes.SOUL, e.getX(), e.getY() + 1, e.getZ(), 4, 0.3, 0.4, 0.3, 0.02);
            n++;
        }
        Vfx.magicCircle(level, t.position(), (float) s.radius(), 30, Circle.DARK);
        sound(p, t.position(), SoundEvents.EVOKER_CAST_SPELL, 1.0F, 0.7F);
        p.displayClientMessage(Component.literal("カース：" + n + "体を呪った").withStyle(ChatFormatting.DARK_PURPLE), true);
        return true;
    }

    private static boolean drain(ServerPlayer p, SkillDefinition s) {
        LivingEntity t = lookTarget(p, 16);
        if (t == null) {
            p.displayClientMessage(Component.literal("ねらう敵がいない").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ServerLevel level = p.serverLevel();
        float dmg = base(p) * (float) s.power();
        float before = t.getHealth();
        magicHit(p, t, dmg, Elem.DARK);
        float dealt = Math.max(0, before - Math.max(0, t.getHealth()));
        p.heal(dealt * (float) s.chance());
        Vec3 a = t.getBoundingBox().getCenter(), b = p.position().add(0, 1, 0);
        for (int i = 0; i <= 12; i++) {
            Vec3 q = a.add(b.subtract(a).scale(i / 12.0));
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, q.x, q.y, q.z, 1, 0.05, 0.05, 0.05, 0);
            level.sendParticles(SHADOW_DUST, q.x, q.y, q.z, 1, 0.05, 0.05, 0.05, 0);
        }
        sound(p, p.position(), SoundEvents.WARDEN_HEARTBEAT, 1.0F, 1.4F);
        return true;
    }

    private static boolean shadowBind(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 at = groundTarget(p, 20);
        double r = s.radius();
        float dmg = base(p) * (float) s.power();
        int ticks = (int) Math.round(s.duration() * 20);
        Vfx.magicCircle(level, at, (float) r, ticks, Circle.DARK);
        List<LivingEntity> bound = enemies(p, at, r);
        for (LivingEntity e : bound) {
            magicHit(p, e, dmg, Elem.DARK);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9, false, false));
            e.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 128, false, false));   // 跳べない
            e.setDeltaMovement(Vec3.ZERO);
            e.hurtMarked = true;
        }
        MageSpells.repeat(p, 5, ticks / 5, i -> {
            for (LivingEntity e : bound) {
                if (!e.isAlive()) continue;
                for (int k = 0; k < 4; k++) {   // 影の鎖
                    double a = i * 0.4 + k * Math.PI / 2;
                    level.sendParticles(SHADOW_DUST, e.getX() + Math.cos(a) * 0.6, e.getY() + 0.2 + (i % 4) * 0.4, e.getZ() + Math.sin(a) * 0.6, 1, 0, 0, 0, 0);
                }
            }
        });
        sound(p, at, SoundEvents.CHAIN_PLACE, 1.5F, 0.5F);
        sound(p, at, SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.4F, 1.6F);
        return true;
    }

    private static boolean blackHole(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 at = groundTarget(p, 24).add(0, 2.0, 0);
        double r = s.radius();
        float dmg = base(p) * (float) s.power();
        int pulses = (int) Math.round(s.duration() * 2);
        Vfx.magicCircle(level, at.subtract(0, 2.0, 0), (float) r, pulses * 10 + 10, Circle.DARK);
        sound(p, at, SoundEvents.WARDEN_SONIC_CHARGE, 1.5F, 0.5F);
        MageSpells.repeat(p, 10, pulses, i -> {
            level.sendParticles(VOID_DUST, at.x, at.y, at.z, 40, 0.5, 0.5, 0.5, 0);
            for (int k = 0; k < 30; k++) {   // 吸い込まれる光
                double a = level.random.nextDouble() * Math.PI * 2, b = level.random.nextDouble() * Math.PI - Math.PI / 2;
                double rr = r * (0.4 + level.random.nextDouble() * 0.6);
                Vec3 q = at.add(Math.cos(a) * Math.cos(b) * rr, Math.sin(b) * rr * 0.6, Math.sin(a) * Math.cos(b) * rr);
                Vec3 v = at.subtract(q).scale(0.12);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, q.x, q.y, q.z, 0, v.x, v.y, v.z, 1.0);
            }
            for (LivingEntity e : enemies(p, at, r + 2)) {
                Vec3 d = at.subtract(e.position().add(0, e.getBbHeight() / 2, 0));
                Vec3 v = d.lengthSqr() > 0.5 ? d.normalize().scale(0.5) : Vec3.ZERO;
                e.setDeltaMovement(v.x, v.y * 0.6, v.z);
                e.hurtMarked = true;
                magicHit(p, e, dmg, Elem.DARK);
            }
            if (i % 2 == 0) sound(p, at, SoundEvents.PORTAL_AMBIENT, 1.5F, 0.4F);
        });
        // 最後に崩れて大ダメージ
        SkillEffects.later(p, pulses * 10, () -> {
            if (!p.isAlive()) return;
            for (LivingEntity e : enemies(p, at, r)) {
                magicHit(p, e, dmg * 4, Elem.DARK);
                Vec3 push = e.position().subtract(at).normalize().scale(1.2);
                e.push(push.x, 0.6, push.z);
                e.hurtMarked = true;
            }
            level.sendParticles(ParticleTypes.SONIC_BOOM, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            level.sendParticles(SHADOW_DUST, at.x, at.y, at.z, 150, r * 0.4, r * 0.3, r * 0.4, 0);
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 60, 0.3, 0.3, 0.3, 0.6);
            Vfx.ring(level, at.subtract(0, 2, 0), (float) r * 1.5F, 18, Vfx.WHITE);
            sound(p, at, SoundEvents.WARDEN_SONIC_BOOM, 2.0F, 0.6F);
        });
        return true;
    }

    private SpiritSpells() {}
}
