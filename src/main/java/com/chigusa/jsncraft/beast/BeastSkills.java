package com.chigusa.jsncraft.beast;

import com.chigusa.jsncraft.magic.MageSpells;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillEffects;
import com.chigusa.jsncraft.vfx.Circle;
import com.chigusa.jsncraft.vfx.Vfx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * 獣使い（群狼の獣使い）の技。鞭で使う。
 * 相棒の呼び出しはトグル（もう一度使うと戻す）。相棒の強さのボーナスは BeastEvents。
 */
public final class BeastSkills {

    static final DustParticleOptions LEAF = new DustParticleOptions(new Vector3f(0.45F, 0.85F, 0.3F), 1.4F),
            AMBER = new DustParticleOptions(new Vector3f(0.95F, 0.65F, 0.2F), 1.5F);
    public static final String CHARGE = "jsn_beast_charge", FRENZY_UNTIL = "jsn_frenzy_until", FRENZY_POW = "jsn_frenzy_pow";

    public static boolean cast(ServerPlayer p, SkillDefinition s) {
        return switch (s.effect()) {
            case "beast_call_wolf" -> summon(p, s, "wolf");
            case "beast_call_golem" -> summon(p, s, "golem");
            case "beast_call_hawk" -> summon(p, s, "hawk");
            case "beast_call_bees" -> summon(p, s, "bee");
            case "beast_lash" -> lash(p, s);
            case "beast_charge" -> charge(p, s);
            case "beast_rally" -> rally(p, s);
            case "beast_roar" -> roar(p, s);
            case "beast_frenzy" -> frenzy(p, s);
            case "beast_call_fox" -> summon(p, s, "fox");
            case "beast_call_cat" -> summon(p, s, "cat");
            case "beast_call_lion" -> summon(p, s, "lion");
            case "beast_call_dragon" -> summon(p, s, "dragon");
            case "beast_king_command" -> kingCommand(p, s);
            default -> false;
        };
    }

    // ================= 相棒を呼ぶ／戻す =================

    /** [体力, 攻撃力] のもと */
    private static double[] base(String kind) {
        return switch (kind) {
            case "golem" -> new double[]{50, 7};
            case "hawk" -> new double[]{14, 3.5};
            case "bee" -> new double[]{10, 2};
            case "fox" -> new double[]{40, 6};
            case "cat" -> new double[]{40, 7};
            case "lion" -> new double[]{70, 9};
            case "dragon" -> new double[]{90, 8};
            default -> new double[]{22, 4};
        };
    }

    private static Mob create(ServerLevel level, String kind) {
        return switch (kind) {
            case "golem" -> ModBeasts.GOLEM.get().create(level);
            case "hawk" -> ModBeasts.HAWK.get().create(level);
            case "bee" -> ModBeasts.BEE.get().create(level);
            case "fox" -> ModBeasts.FOX.get().create(level);
            case "cat" -> ModBeasts.CAT.get().create(level);
            case "lion" -> ModBeasts.LION.get().create(level);
            case "dragon" -> ModBeasts.DRAGON.get().create(level);
            default -> ModBeasts.WOLF.get().create(level);
        };
    }

    private static boolean summon(ServerPlayer p, SkillDefinition s, String kind) {
        ServerLevel level = p.serverLevel();
        int n = Math.max(1, s.amplifier());
        List<Mob> have = PetCommon.petsOf(p, kind);
        if (have.size() >= n) { // ぜんぶいる → 戻す
            for (Mob m : have) {
                level.sendParticles(LEAF, m.getX(), m.getY() + 0.6, m.getZ(), 16, 0.3, 0.4, 0.3, 0);
                level.sendParticles(ParticleTypes.POOF, m.getX(), m.getY() + 0.5, m.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
                m.discard();
            }
            level.playSound(null, p.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8F, 0.7F);
            return true;
        }
        double[] b = base(kind);
        double mult = s.power() > 0 ? s.power() : 1.0;
        boolean flying = kind.equals("hawk") || kind.equals("bee") || kind.equals("dragon");
        Vec3 feet = p.position();
        Vfx.magicCircle(level, feet.add(0, 0.05, 0), 2.2F, 30, Circle.NATURE);
        for (int i = have.size(); i < n; i++) {
            Mob m = create(level, kind);
            if (m == null) continue;
            double ang = Math.PI * 2 * i / n + p.getRandom().nextDouble() * 0.5;
            Vec3 at = feet.add(Math.cos(ang) * 1.6, flying ? 1.4 : 0.1, Math.sin(ang) * 1.6);
            if (!level.noCollision(m, m.getType().getDimensions().makeBoundingBox(at))) at = feet.add(0, flying ? 1.6 : 0.1, 0);
            m.moveTo(at.x, at.y, at.z, p.getYRot(), 0);
            PetCommon.setOwner(m, p, kind);
            if (m instanceof TamableAnimal t) {
                t.setTame(true, false);
                t.setOwnerUUID(p.getUUID());
            }
            PetCommon.applyStats(m, p, b[0], b[1] * mult);
            level.addFreshEntity(m);
            level.sendParticles(LEAF, at.x, at.y + 0.5, at.z, 24, 0.4, 0.5, 0.4, 0);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y + 0.6, at.z, 8, 0.4, 0.4, 0.4, 0);
        }
        SoundEvent snd = switch (kind) {
            case "golem" -> SoundEvents.IRON_GOLEM_REPAIR;
            case "hawk" -> SoundEvents.PHANTOM_FLAP;
            case "bee" -> SoundEvents.BEEHIVE_EXIT;
            case "fox" -> SoundEvents.FOX_AMBIENT;
            case "cat" -> SoundEvents.CAT_AMBIENT;
            case "lion" -> SoundEvents.LIGHTNING_BOLT_THUNDER;
            case "dragon" -> SoundEvents.ENDER_DRAGON_GROWL;
            default -> SoundEvents.WOLF_HOWL;
        };
        level.playSound(null, p.blockPosition(), snd, SoundSource.PLAYERS, kind.equals("lion") ? 0.4F : 1.0F,
                kind.equals("hawk") || kind.equals("dragon") ? 1.4F : 1.0F);
        if (kind.equals("dragon")) p.displayClientMessage(Component.literal("右クリックで背中に乗れる（スニークで降りる）").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return true;
    }

    // ================= 鞭打ち：遠くの敵をしなる鞭で叩いて、相棒に狙わせる =================

    private static boolean lash(ServerPlayer p, SkillDefinition s) {
        LivingEntity t = MageSpells.lookTarget(p, s.radius());
        if (t == null) return false;
        ServerLevel level = p.serverLevel();
        p.swing(InteractionHand.MAIN_HAND, true);
        Vec3 from = p.getEyePosition().add(p.getLookAngle().scale(0.6)).add(0, -0.3, 0);
        Vec3 to = t.getBoundingBox().getCenter();
        int steps = (int) Math.max(6, from.distanceTo(to) * 4);
        for (int i = 0; i <= steps; i++) {
            double k = i / (double) steps;
            Vec3 q = from.lerp(to, k).add(0, Math.sin(k * Math.PI) * 0.6, 0);
            level.sendParticles(AMBER, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
        level.sendParticles(ParticleTypes.CRIT, to.x, to.y, to.z, 14, 0.3, 0.3, 0.3, 0.3);
        level.playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.6F);
        level.playSound(null, t.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.6F, 1.8F);
        SkillEffects.hit(p, t, (float) (p.getAttributeValue(Attributes.ATTACK_DAMAGE) * s.power()));
        // 少し引き寄せる（大きな敵はそのまま）
        if (t.getBbWidth() < 2.0F && s.amplifier() > 0) {
            Vec3 pull = p.position().subtract(t.position()).normalize().scale(0.25 * s.amplifier());
            t.setDeltaMovement(t.getDeltaMovement().add(pull.x, 0.2, pull.z));
            t.hurtMarked = true;
        }
        PetCommon.focus(p, t, 200);
        return true;
    }

    // ================= 突撃：相棒みんなを、ねらった敵へ飛びかからせる =================

    private static boolean charge(ServerPlayer p, SkillDefinition s) {
        List<Mob> pets = PetCommon.petsOf(p, "");
        if (pets.isEmpty()) {
            p.displayClientMessage(Component.literal("相棒がいません").withStyle(ChatFormatting.RED), true);
            return false;
        }
        LivingEntity t = MageSpells.lookTarget(p, s.radius());
        if (t == null) return false;
        ServerLevel level = p.serverLevel();
        int ticks = (int) Math.round(s.duration() * 20);
        PetCommon.focus(p, t, ticks + 100);
        for (Mob m : pets) {
            m.setTarget(t);
            m.getPersistentData().putDouble(CHARGE, s.power());
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 1));
            if (s.amplifier() > 0) m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, s.amplifier() - 1));
            Vec3 d = t.position().subtract(m.position());
            if (d.lengthSqr() > 1) {
                Vec3 v = d.normalize().scale(Math.min(1.6, 0.5 + d.length() * 0.08));
                m.setDeltaMovement(v.x, m.isNoGravity() || m instanceof net.minecraft.world.entity.animal.FlyingAnimal ? v.y : 0.45, v.z);
                m.hurtMarked = true;
            }
            level.sendParticles(AMBER, m.getX(), m.getY() + 0.5, m.getZ(), 10, 0.3, 0.3, 0.3, 0);
        }
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, t.getX(), t.getY() + t.getBbHeight() + 0.4, t.getZ(), 3, 0.3, 0.1, 0.3, 0);
        Vfx.ring(level, t.position().add(0, 0.1, 0), 1.6F, 20, Vfx.GOLD);
        level.playSound(null, p.blockPosition(), SoundEvents.WOLF_GROWL, SoundSource.PLAYERS, 1.2F, 0.8F);
        p.displayClientMessage(Component.literal("行けっ！").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        return true;
    }

    // ================= 集まれ：相棒を呼び寄せて回復し、守りを固める =================

    private static boolean rally(ServerPlayer p, SkillDefinition s) {
        List<Mob> pets = PetCommon.petsOf(p, "");
        if (pets.isEmpty()) {
            p.displayClientMessage(Component.literal("相棒がいません").withStyle(ChatFormatting.RED), true);
            return false;
        }
        ServerLevel level = p.serverLevel();
        int ticks = (int) Math.round(s.duration() * 20);
        int i = 0;
        for (Mob m : pets) {
            double ang = Math.PI * 2 * i++ / pets.size();
            boolean fly = m instanceof net.minecraft.world.entity.animal.FlyingAnimal;
            Vec3 at = p.position().add(Math.cos(ang) * 1.8, fly ? 1.5 : 0.1, Math.sin(ang) * 1.8);
            if (!level.noCollision(m, m.getBoundingBox().move(at.subtract(m.position())))) at = p.position().add(0, fly ? 1.6 : 0.1, 0);
            level.sendParticles(LEAF, m.getX(), m.getY() + 0.5, m.getZ(), 10, 0.3, 0.3, 0.3, 0);
            m.teleportTo(at.x, at.y, at.z);
            m.getNavigation().stop();
            m.setTarget(null);
            m.heal((float) (m.getMaxHealth() * s.power()));
            m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, Math.max(0, s.amplifier())));
            m.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 0));
            level.sendParticles(ParticleTypes.HEART, at.x, at.y + 1.0, at.z, 3, 0.3, 0.2, 0.3, 0);
        }
        p.getPersistentData().remove(PetCommon.FOCUS);
        Vfx.magicCircle(level, p.position().add(0, 0.05, 0), 3.0F, 30, Circle.NATURE);
        level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 1.2F, 1.4F);
        p.displayClientMessage(Component.literal("集まれ！").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), true);
        return true;
    }

    // ================= 獣の咆哮：相棒を奮い立たせ、周りの敵をすくませる =================

    private static boolean roar(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        int ticks = (int) Math.round(s.duration() * 20);
        for (Mob m : PetCommon.petsOf(p, "")) {
            if (m.distanceTo(p) > 32) continue;
            m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, Math.max(0, s.amplifier())));
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 0));
            m.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, 1));
            level.sendParticles(AMBER, m.getX(), m.getY() + 0.8, m.getZ(), 12, 0.3, 0.4, 0.3, 0);
        }
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 0));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(s.radius()), e -> SkillEffects.isEnemy(p, e))) {
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0), p);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks / 2, 1), p);
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ(), 1, 0.2, 0.1, 0.2, 0);
        }
        Vec3 c = p.position().add(0, 0.1, 0);
        Vfx.ring(level, c, (float) s.radius(), 20, Vfx.GOLD);
        Vfx.ring(level, c, (float) s.radius() * 0.6F, 16, Vfx.RED);
        level.sendParticles(ParticleTypes.SONIC_BOOM, p.getX(), p.getY() + 1.2, p.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.2F, 1.1F);
        level.playSound(null, p.blockPosition(), SoundEvents.WOLF_HOWL, SoundSource.PLAYERS, 1.2F, 0.8F);
        return true;
    }

    // ================= ★野生解放：相棒たちが本気になる（攻撃が強くなり、周りにも当たる） =================

    private static boolean frenzy(ServerPlayer p, SkillDefinition s) {
        List<Mob> pets = PetCommon.petsOf(p, "");
        if (pets.isEmpty()) {
            p.displayClientMessage(Component.literal("相棒がいません").withStyle(ChatFormatting.RED), true);
            return false;
        }
        ServerLevel level = p.serverLevel();
        int ticks = (int) Math.round(s.duration() * 20);
        long until = level.getGameTime() + ticks;
        for (Mob m : pets) {
            m.getPersistentData().putLong(FRENZY_UNTIL, until);
            m.getPersistentData().putDouble(FRENZY_POW, s.power());
            m.setHealth(m.getMaxHealth());
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 1));
            m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 0));
            m.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0));
            Vfx.magicCircle(level, m.position().add(0, 0.05, 0), 1.4F, 30, Circle.NATURE);
            level.sendParticles(AMBER, m.getX(), m.getY() + 0.8, m.getZ(), 24, 0.4, 0.5, 0.4, 0);
        }
        Vec3 feet = p.position().add(0, 0.05, 0);
        Vfx.castCircles(level, feet, Circle.NATURE, 30, 3);
        Vfx.releaseCircles(level, feet, Circle.NATURE, 3);
        level.playSound(null, p.blockPosition(), SoundEvents.WOLF_HOWL, SoundSource.PLAYERS, 1.5F, 0.7F);
        level.playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0F, 0.8F);
        p.displayClientMessage(Component.literal("野生解放！").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        return true;
    }

    // ================= ★百獣の号令：すべての相棒が本気になり、いっせいに大技を放つ =================

    private static boolean kingCommand(ServerPlayer p, SkillDefinition s) {
        List<Mob> pets = PetCommon.petsOf(p, "");
        if (pets.isEmpty()) {
            p.displayClientMessage(Component.literal("相棒がいません").withStyle(ChatFormatting.RED), true);
            return false;
        }
        ServerLevel level = p.serverLevel();
        int ticks = (int) Math.round(s.duration() * 20);
        long until = level.getGameTime() + ticks;
        for (Mob m : pets) {
            m.getPersistentData().putLong(FRENZY_UNTIL, until);
            m.getPersistentData().putDouble(FRENZY_POW, s.power());
            m.setHealth(m.getMaxHealth());
            m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, Math.max(0, s.amplifier())));
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 1));
            m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 1));
            if (m instanceof GeoPet g) g.readySpecial();
            level.sendParticles(AMBER, m.getX(), m.getY() + 0.8, m.getZ(), 30, 0.5, 0.6, 0.5, 0);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, m.getX(), m.getY() + 0.8, m.getZ(), 20, 0.4, 0.5, 0.4, 0.3);
        }
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 0));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 0));
        Vec3 feet = p.position().add(0, 0.05, 0);
        Vfx.castCircles(level, feet, Circle.NATURE, 40, 3);
        Vfx.releaseCircles(level, feet, Circle.NATURE, 3);
        Vfx.magicCircle(level, feet.add(0, 6, 0), 5.0F, 40, Circle.STAR);
        level.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.0F, 1.2F);
        level.playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.2F, 0.8F);
        level.playSound(null, p.blockPosition(), SoundEvents.WOLF_HOWL, SoundSource.PLAYERS, 1.5F, 0.9F);
        p.displayClientMessage(Component.literal("百獣の号令！").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        return true;
    }

    private BeastSkills() {}
}
