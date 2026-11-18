package com.chigusa.jsncraft.magic;

import com.chigusa.jsncraft.magic.MageSpells.Elem;
import com.chigusa.jsncraft.party.PartyManager;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillEffects;
import com.chigusa.jsncraft.skill.SkillService;
import com.chigusa.jsncraft.skill.TimedFlags;
import com.chigusa.jsncraft.stat.ModAttributes;
import com.chigusa.jsncraft.stat.PlayerResources;
import com.chigusa.jsncraft.stat.ResourceService;
import com.chigusa.jsncraft.vfx.Circle;
import com.chigusa.jsncraft.vfx.CircleOptions;
import com.chigusa.jsncraft.vfx.Vfx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static com.chigusa.jsncraft.magic.MageSpells.*;

/**
 * 癒し手（慈光の癒し手・聖光の祈り手）の魔法：回復・光・守護・浄化。
 * 回復量とダメージは「聖杖の聖光弾の威力 × power」。回復は回復力（healing_power）でも増える。
 * 光はアンデッドに2倍、呪われた敵に光を当てると「浄化」（1.8倍）。
 */
public final class HolySpells {

    static final DustParticleOptions GOLD_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.9F, 0.5F), 1.4F),
            WHITE_DUST = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 0.95F), 1.2F);

    public static boolean cast(ServerPlayer p, SkillDefinition s) {
        return switch (s.effect()) {
            case "holy_heal" -> heal(p, s);
            case "holy_heal_circle" -> healCircle(p, s);
            case "holy_regen" -> regen(p, s);
            case "holy_high_heal" -> highHeal(p, s);
            case "holy_sanctuary" -> sanctuary(p, s);
            case "holy_arrow" -> holyArrow(p, s);
            case "holy_light_pillar" -> lightPillar(p, s);
            case "holy_flash" -> flash(p, s);
            case "holy_ray" -> holyRay(p, s);
            case "holy_sacred_nova" -> sacredNova(p, s);
            case "holy_protect" -> protect(p, s);
            case "holy_barrier" -> barrier(p, s);
            case "holy_blessing" -> blessing(p, s);
            case "holy_guardian" -> guardian(p, s);
            case "holy_aegis" -> aegis(p, s);
            case "holy_purify" -> purify(p, s);
            case "holy_turn_undead" -> turnUndead(p, s);
            case "holy_mana_gift" -> manaGift(p, s);
            case "holy_exorcism" -> exorcism(p, s);
            case "holy_revive" -> revive(p, s);
            default -> false;
        };
    }

    // ================= 共通 =================

    static boolean ally(ServerPlayer p, ServerPlayer o) {
        if (o == p) return true;
        PartyManager.Party party = PartyManager.partyOf(p);
        return party != null && party.members.contains(o.getUUID());
    }

    /** 自分と、近くのパーティーの仲間 */
    static List<ServerPlayer> allies(ServerPlayer p, Vec3 c, double r) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer o : p.serverLevel().players()) {
            if (ally(p, o) && o.isAlive() && o.position().distanceTo(c) <= r) out.add(o);
        }
        return out;
    }

    /** 目線の先の仲間（いなければ自分） */
    static ServerPlayer lookAlly(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        ServerPlayer best = p;
        double bestDot = 0.92;
        for (ServerPlayer o : allies(p, p.position(), range)) {
            if (o == p) continue;
            Vec3 d = o.getBoundingBox().getCenter().subtract(eye).normalize();
            double dot = d.dot(look);
            if (dot > bestDot && p.hasLineOfSight(o)) {
                bestDot = dot;
                best = o;
            }
        }
        return best;
    }

    /** 回復量：聖杖 × power × (1 + 回復力)。恩寵なら10%で2倍 */
    static float healAmount(ServerPlayer p, double power) {
        float h = base(p) * (float) power * (float) (1 + p.getAttributeValue(ModAttributes.HEALING_POWER));
        SkillDefinition grace = SkillService.passive(p, "holy_grace");
        if (grace != null && p.getRandom().nextDouble() < grace.chance()) {
            h *= 2;
            p.displayClientMessage(Component.literal("恩寵！").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        }
        return h;
    }

    static void healTo(ServerPlayer p, ServerPlayer target, float amount) {
        target.heal(amount);
        ServerLevel level = p.serverLevel();
        level.sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + 2.0, target.getZ(), 3, 0.3, 0.2, 0.3, 0);
        level.sendParticles(GOLD_DUST, target.getX(), target.getY() + 1, target.getZ(), 10, 0.4, 0.6, 0.4, 0);
        SkillDefinition over = SkillService.passive(p, "holy_overflow_light");
        if (over != null) target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, (int) Math.round(over.duration() * 20), 0));
        if (target != p) {
            SkillDefinition ret = SkillService.passive(p, "holy_prayer_return");
            if (ret != null) {
                PlayerResources r = ResourceService.get(p);
                r.mana = Math.min(ResourceService.maxMana(p), r.mana + (float) ret.power());
            }
        }
    }

    /** 悪い効果と呪い・濡れなどを消す */
    static void cleanse(LivingEntity e) {
        List<MobEffectInstance> bad = new ArrayList<>();
        for (MobEffectInstance m : e.getActiveEffects()) if (m.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) bad.add(m);
        for (MobEffectInstance m : bad) e.removeEffect(m.getEffect());
        e.clearFire();
        e.setTicksFrozen(0);
    }

    static boolean undead(LivingEntity e) {
        return e.getType().is(EntityTypeTags.UNDEAD);
    }

    // ================= 回復 =================

    private static boolean heal(ServerPlayer p, SkillDefinition s) {
        ServerPlayer t = lookAlly(p, s.radius());
        healTo(p, t, healAmount(p, s.power()));
        Vfx.ring(p.serverLevel(), t.position(), 1.4F, 10, Vfx.GOLD);
        sound(p, t.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.6F);
        return true;
    }

    private static boolean healCircle(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 at = groundTarget(p, 12);
        double r = s.radius();
        int pulses = (int) Math.round(s.duration());
        float h = healAmount(p, s.power());
        Vfx.magicCircle(level, at, (float) r, pulses * 20, Circle.HOLY);
        sound(p, at, SoundEvents.BEACON_ACTIVATE, 0.8F, 1.6F);
        MageSpells.repeat(p, 20, pulses, i -> {
            level.sendParticles(GOLD_DUST, at.x, at.y + 0.3, at.z, 30, r * 0.5, 0.2, r * 0.5, 0);
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 0.5, at.z, 6, r * 0.4, 0.3, r * 0.4, 0.02);
            for (ServerPlayer o : allies(p, at, r)) {
                healTo(p, o, h);
                // 円の中で戦う仲間は魔法の威力が少し上がる
                SkillEffects.attributeBuff(o, ModAttributes.MAGIC_POWER, "holy_circle", 0.1, AttributeModifier.Operation.ADD_VALUE, 1.5);
            }
        });
        return true;
    }

    private static boolean regen(ServerPlayer p, SkillDefinition s) {
        int ticks = (int) Math.round(s.duration() * 20);
        for (ServerPlayer o : allies(p, p.position(), s.radius())) {
            o.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, Math.max(0, s.amplifier())));
            p.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, o.getX(), o.getY() + 1, o.getZ(), 10, 0.4, 0.6, 0.4, 0);
        }
        Vfx.magicCircle(p.serverLevel(), p.position(), (float) s.radius() * 0.6F, 24, Circle.HOLY);
        sound(p, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.3F);
        return true;
    }

    private static boolean highHeal(ServerPlayer p, SkillDefinition s) {
        ServerPlayer t = lookAlly(p, s.radius());
        healTo(p, t, healAmount(p, s.power()));
        cleanse(t);
        ServerLevel level = p.serverLevel();
        level.sendParticles(ParticleTypes.END_ROD, t.getX(), t.getY() + 4, t.getZ(), 40, 0.3, 1.5, 0.3, 0.02);
        Vfx.magicCircle(level, t.position(), 1.6F, 20, Circle.HOLY);
        sound(p, t.position(), SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.4F);
        return true;
    }

    private static boolean sanctuary(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 at = p.position();
        double r = s.radius();
        float big = healAmount(p, s.power());
        for (ServerPlayer o : allies(p, at, r)) {
            healTo(p, o, big);
            cleanse(o);
        }
        int pulses = (int) Math.round(s.duration());
        float small = big * 0.15F;
        Vfx.magicCircle(level, at, (float) r, pulses * 20, Circle.HOLY);
        Vfx.circle(level, at.add(0, 7, 0), Circle.HOLY, (float) r, pulses * 20, CircleOptions.SKY);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 6, at.z, 120, r * 0.5, 2, r * 0.5, 0.02);
        sound(p, at, SoundEvents.BEACON_ACTIVATE, 1.5F, 1.2F);
        sound(p, at, SoundEvents.PLAYER_LEVELUP, 1.0F, 0.6F);
        MageSpells.repeat(p, 20, pulses, i -> {
            level.sendParticles(GOLD_DUST, at.x, at.y + 0.5, at.z, 40, r * 0.5, 0.4, r * 0.5, 0);
            for (ServerPlayer o : allies(p, at, r)) healTo(p, o, small);
            for (LivingEntity e : enemies(p, at, r)) if (undead(e)) magicHit(p, e, base(p) * 0.5F, Elem.LIGHT);
        });
        return true;
    }

    // ================= 光 =================

    private static boolean holyArrow(ServerPlayer p, SkillDefinition s) {
        float dmg = base(p) * (float) s.power();
        sound(p, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 2.0F);
        shoot(p, hand(p), p.getLookAngle().scale(2.0), 18, 1, 0.1, ParticleTypes.END_ROD, e -> magicHit(p, e, dmg, Elem.LIGHT),
                at -> p.serverLevel().sendParticles(GOLD_DUST, at.x, at.y, at.z, 12, 0.2, 0.2, 0.2, 0));
        return true;
    }

    private static boolean lightPillar(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 at = groundTarget(p, 20);
        double r = s.radius();
        float dmg = base(p) * (float) s.power();
        int pulses = (int) Math.round(s.duration() * 2);
        Vfx.magicCircle(level, at, (float) r, pulses * 10, Circle.HOLY);
        sound(p, at, SoundEvents.BEACON_ACTIVATE, 1.2F, 1.8F);
        MageSpells.repeat(p, 10, pulses, i -> {
            for (int h = 0; h < 16; h++) {
                level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + h * 0.5, at.z, 2, r * 0.3, 0.1, r * 0.3, 0.01);
                level.sendParticles(WHITE_DUST, at.x, at.y + h * 0.5, at.z, 2, r * 0.25, 0.1, r * 0.25, 0);
            }
            for (LivingEntity e : enemies(p, at, r)) magicHit(p, e, dmg, Elem.LIGHT);
        });
        return true;
    }

    private static boolean flash(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        int ticks = (int) Math.round(s.duration() * 20);
        int n = 0;
        for (LivingEntity e : enemies(p, p.position(), s.radius())) {
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks + 20, 0));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9, false, false));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1, false, false));
            if (e instanceof net.minecraft.world.entity.Mob mob) mob.setTarget(null);
            n++;
        }
        level.sendParticles(ParticleTypes.FLASH, p.getX(), p.getY() + 1.5, p.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 80, s.radius() * 0.4, 1, s.radius() * 0.4, 0.3);
        Vfx.ring(level, p.position(), (float) s.radius(), 12, Vfx.WHITE);
        sound(p, p.position(), SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.5F, 1.4F);
        p.displayClientMessage(Component.literal("フラッシュ：" + n + "体の動きを止めた").withStyle(ChatFormatting.YELLOW), true);
        return true;
    }

    private static boolean holyRay(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 from = hand(p), look = p.getLookAngle();
        BlockHitResult bh = level.clip(new ClipContext(from, from.add(look.scale(s.radius())), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 to = bh.getLocation();
        float dmg = base(p) * (float) s.power();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(1.0), e -> SkillEffects.isEnemy(p, e))) {
            if (e.getBoundingBox().inflate(0.8).clip(from, to).isPresent()) magicHit(p, e, dmg, Elem.LIGHT);
        }
        double len = to.distanceTo(from);
        for (double t = 0; t < len; t += 0.4) {
            Vec3 q = from.add(look.scale(t));
            level.sendParticles(ParticleTypes.END_ROD, q.x, q.y, q.z, 1, 0.05, 0.05, 0.05, 0.01);
            level.sendParticles(GOLD_DUST, q.x, q.y, q.z, 2, 0.12, 0.12, 0.12, 0);
        }
        sound(p, from, SoundEvents.BEACON_DEACTIVATE, 1.2F, 2.0F);
        return true;
    }

    private static boolean sacredNova(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 at = p.position();
        double r = s.radius();
        float dmg = base(p) * (float) s.power();
        int n = 0;
        for (LivingEntity e : enemies(p, at, r)) {
            magicHit(p, e, dmg, Elem.LIGHT);
            Vec3 push = e.position().subtract(at).normalize().scale(1.0);
            e.push(push.x, 0.4, push.z);
            e.hurtMarked = true;
            n++;
        }
        float h = healAmount(p, s.chance());
        for (ServerPlayer o : allies(p, at, r)) healTo(p, o, h);
        level.sendParticles(ParticleTypes.FLASH, at.x, at.y + 1, at.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1, at.z, 250, r * 0.3, 1.0, r * 0.3, 0.6);
        level.sendParticles(GOLD_DUST, at.x, at.y + 1, at.z, 150, r * 0.5, 1.5, r * 0.5, 0);
        Vfx.ring(level, at, (float) r * 1.5F, 18, Vfx.GOLD);
        Vfx.ring(level, at, (float) r, 14, Vfx.WHITE);
        sound(p, at, SoundEvents.BEACON_ACTIVATE, 2.0F, 2.0F);
        sound(p, at, SoundEvents.TOTEM_USE, 0.8F, 1.4F);
        p.displayClientMessage(Component.literal("セイクリッドノヴァ：" + n + "体に命中").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    // ================= 守護 =================

    private static boolean protect(ServerPlayer p, SkillDefinition s) {
        ServerPlayer t = lookAlly(p, s.radius());
        SkillEffects.attributeBuff(t, ModAttributes.DAMAGE_REDUCTION, "holy_protect", s.power(), AttributeModifier.Operation.ADD_VALUE, s.duration());
        ServerLevel level = p.serverLevel();
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, t.getX(), t.getY() + 1, t.getZ(), 20, 0.4, 0.7, 0.4, 0.1);
        Vfx.ring(level, t.position(), 1.6F, 12, Vfx.AZURE);
        sound(p, t.position(), SoundEvents.ARMOR_EQUIP_DIAMOND.value(), 1.0F, 1.2F);
        return true;
    }

    private static boolean barrier(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 at = p.position();
        double r = s.radius();
        int pulses = (int) Math.round(s.duration() * 4);
        Vfx.magicCircle(level, at, (float) r, pulses * 5, Circle.HOLY);
        sound(p, at, SoundEvents.BEACON_POWER_SELECT, 1.2F, 1.0F);
        MageSpells.repeat(p, 5, pulses, i -> {
            // 半球の光の膜
            for (int k = 0; k < 20; k++) {
                double a = (i * 0.3 + k * Math.PI / 10), b = (k % 5) * Math.PI / 10;
                level.sendParticles(WHITE_DUST, at.x + Math.cos(a) * Math.cos(b) * r, at.y + Math.sin(b) * r, at.z + Math.sin(a) * Math.cos(b) * r, 1, 0, 0, 0, 0);
            }
            // 敵の矢や火の玉を消す
            for (Projectile pr : level.getEntitiesOfClass(Projectile.class, new AABB(at, at).inflate(r))) {
                Entity owner = pr.getOwner();
                if (owner instanceof ServerPlayer op && ally(p, op)) continue;
                if (pr.position().distanceTo(at) > r) continue;
                level.sendParticles(ParticleTypes.END_ROD, pr.getX(), pr.getY(), pr.getZ(), 5, 0.1, 0.1, 0.1, 0.05);
                pr.discard();
            }
            for (ServerPlayer o : allies(p, at, r)) {
                SkillEffects.attributeBuff(o, ModAttributes.DAMAGE_REDUCTION, "holy_barrier", s.power(), AttributeModifier.Operation.ADD_VALUE, 0.5);
            }
        });
        return true;
    }

    private static boolean blessing(ServerPlayer p, SkillDefinition s) {
        for (ServerPlayer o : allies(p, p.position(), s.radius())) {
            SkillEffects.attributeBuff(o, Attributes.ATTACK_DAMAGE, "holy_blessing_atk", s.power(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, s.duration());
            SkillEffects.attributeBuff(o, Attributes.MOVEMENT_SPEED, "holy_blessing_spd", 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, s.duration());
            SkillEffects.attributeBuff(o, ModAttributes.MAGIC_POWER, "holy_blessing_mag", s.power(), AttributeModifier.Operation.ADD_VALUE, s.duration());
            p.serverLevel().sendParticles(GOLD_DUST, o.getX(), o.getY() + 1, o.getZ(), 20, 0.4, 0.7, 0.4, 0);
            Vfx.ring(p.serverLevel(), o.position(), 1.4F, 10, Vfx.GOLD);
        }
        sound(p, p.position(), SoundEvents.BELL_BLOCK, 1.0F, 1.6F);
        return true;
    }

    private static boolean guardian(ServerPlayer p, SkillDefinition s) {
        ServerPlayer t = lookAlly(p, s.radius());
        TimedFlags.set(t, "holy_guardian", s.power(), s.duration());
        ServerLevel level = p.serverLevel();
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, t.getX(), t.getY() + 1, t.getZ(), 30, 0.4, 0.8, 0.4, 0.2);
        Vfx.magicCircle(level, t.position(), 1.4F, 30, Circle.HOLY);
        sound(p, t.position(), SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.8F);
        t.displayClientMessage(Component.literal("ガーディアン：" + Math.round(s.duration()) + "秒のあいだ、一度だけ倒れない").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    private static boolean aegis(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        for (ServerPlayer o : allies(p, p.position(), s.radius())) {
            TimedFlags.set(o, "holy_aegis", 1, s.duration());
            MageSpells.repeat(p, 4, (int) Math.round(s.duration() * 5), i -> {
                for (int k = 0; k < 8; k++) {
                    double a = i * 0.5 + k * Math.PI / 4;
                    level.sendParticles(GOLD_DUST, o.getX() + Math.cos(a) * 0.9, o.getY() + 0.2 + (k % 4) * 0.5, o.getZ() + Math.sin(a) * 0.9, 1, 0, 0, 0, 0);
                }
            });
            o.displayClientMessage(Component.literal("ホーリーアイギス：無敵！").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        }
        Vfx.ring(level, p.position(), (float) s.radius(), 16, Vfx.GOLD);
        sound(p, p.position(), SoundEvents.TOTEM_USE, 1.0F, 1.2F);
        return true;
    }

    // ================= 浄化 =================

    private static boolean purify(ServerPlayer p, SkillDefinition s) {
        ServerPlayer t = lookAlly(p, 16);
        ServerLevel level = p.serverLevel();
        float h = healAmount(p, s.power());
        for (ServerPlayer o : allies(p, t.position(), s.radius())) {
            cleanse(o);
            healTo(p, o, h);
            level.sendParticles(ParticleTypes.END_ROD, o.getX(), o.getY() + 1, o.getZ(), 15, 0.3, 0.6, 0.3, 0.05);
        }
        sound(p, t.position(), SoundEvents.BREWING_STAND_BREW, 1.0F, 1.6F);
        return true;
    }

    private static boolean turnUndead(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        float dmg = base(p) * (float) s.power();
        int n = 0;
        for (LivingEntity e : enemies(p, p.position(), s.radius())) {
            if (!undead(e) && !MageSpells.cursed(e)) continue;
            magicHit(p, e, dmg, Elem.LIGHT);
            e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), 60));
            level.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + 1, e.getZ(), 12, 0.3, 0.6, 0.3, 0.1);
            n++;
        }
        Vfx.magicCircle(level, p.position(), (float) s.radius(), 20, Circle.HOLY);
        Vfx.ring(level, p.position(), (float) s.radius(), 14, Vfx.WHITE);
        sound(p, p.position(), SoundEvents.BELL_RESONATE, 1.2F, 1.4F);
        p.displayClientMessage(Component.literal("ターンアンデッド：" + n + "体を祓った").withStyle(ChatFormatting.YELLOW), true);
        return true;
    }

    private static boolean manaGift(ServerPlayer p, SkillDefinition s) {
        int n = 0;
        for (ServerPlayer o : allies(p, p.position(), s.radius())) {
            if (o == p) continue;
            PlayerResources r = ResourceService.get(o);
            r.mana = Math.min(ResourceService.maxMana(o), r.mana + (float) s.amplifier());
            p.serverLevel().sendParticles(ParticleTypes.ENCHANT, o.getX(), o.getY() + 2, o.getZ(), 30, 0.4, 0.5, 0.4, 0.5);
            n++;
        }
        if (n == 0) {
            p.displayClientMessage(Component.literal("マナを分ける仲間が近くにいない").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        sound(p, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.8F);
        return true;
    }

    private static boolean exorcism(ServerPlayer p, SkillDefinition s) {
        LivingEntity t = lookTarget(p, 24);
        if (t == null) {
            p.displayClientMessage(Component.literal("ねらう敵がいない").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ServerLevel level = p.serverLevel();
        float dmg = base(p) * (float) s.power();
        if (undead(t) || MageSpells.cursed(t)) dmg *= 1.5F;
        Vec3 at = t.position();
        Vfx.magicCircle(level, at, 2.0F, 20, Circle.HOLY);
        Vfx.circle(level, at.add(0, 4, 0), Circle.HOLY, 1.6F, 20, CircleOptions.SKY);
        magicHit(p, t, dmg, Elem.LIGHT);
        // ついている良い効果もはがす
        List<MobEffectInstance> good = new ArrayList<>();
        for (MobEffectInstance m : t.getActiveEffects()) if (m.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) good.add(m);
        for (MobEffectInstance m : good) t.removeEffect(m.getEffect());
        for (int h = 0; h < 12; h++) level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + h * 0.4, at.z, 3, 0.2, 0.1, 0.2, 0.02);
        level.sendParticles(ParticleTypes.FLASH, at.x, at.y + 1, at.z, 1, 0, 0, 0, 0);
        sound(p, at, SoundEvents.BELL_BLOCK, 1.5F, 0.8F);
        return true;
    }

    private static boolean revive(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        for (ServerPlayer o : allies(p, p.position(), s.radius())) {
            TimedFlags.set(o, "holy_revive", s.power(), s.duration());
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, o.getX(), o.getY() + 1, o.getZ(), 40, 0.4, 0.8, 0.4, 0.3);
            o.displayClientMessage(Component.literal("リバイブ：" + Math.round(s.duration()) + "秒のあいだ、倒れても一度だけその場で復活").withStyle(ChatFormatting.GOLD), true);
        }
        Vfx.ring(level, p.position(), (float) s.radius(), 16, Vfx.GOLD);
        sound(p, p.position(), SoundEvents.BEACON_ACTIVATE, 1.5F, 1.5F);
        return true;
    }

    private HolySpells() {}
}
