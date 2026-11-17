package com.chigusa.jsncraft.alchemy;

import com.chigusa.jsncraft.cooking.CookingService;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillEffects;
import com.chigusa.jsncraft.skill.SkillService;
import com.chigusa.jsncraft.stat.PlayerResources;
import com.chigusa.jsncraft.stat.ResourceService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;

/** 薬の効果をかける（飲む・投げる・粉塵で共通）。 */
public final class ElixirEffects {

    /**
     * target に薬の効果をかける。scale は形による倍率（投げる 0.8・粉塵 0.75）。
     * 自分で飲んだ調合師には「薬の心得」が乗る。
     */
    public static void apply(ServerPlayer target, Elixirs.Type t, ElixirData d, double scale) {
        double power = d.power() * d.qualityMult() * scale;
        double time = d.time() * d.qualityMult();
        SkillDefinition master = SkillService.passive(target, "potion_master");
        if (master != null) power *= 1 + master.power();
        ServerLevel level = target.serverLevel();
        PlayerResources res = ResourceService.get(target);
        switch (t.kind()) {
            case "heal" -> target.heal((float) (t.amount() * power));
            case "stamina" -> res.stamina = Math.min(ResourceService.maxStamina(target), res.stamina + (float) (t.amount() * power));
            case "mana" -> res.mana = Math.min(ResourceService.maxMana(target), res.mana + (float) (t.amount() * power));
            case "cure" -> {
                CookingService.special(target, "cleanse", 0, 0);
                target.heal((float) (t.amount() * power));
            }
            case "elixir" -> {
                target.heal((float) (t.amount() * power));
                target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, (int) Math.round(t.seconds() * time * 20), 1));
            }
            case "ancient" -> {
                SkillEffects.attributeBuff(target, Attributes.MAX_HEALTH, "elixir_ancient", t.amount() * power,
                        AttributeModifier.Operation.ADD_VALUE, t.seconds() * time);
                target.setHealth(target.getMaxHealth());
                res.stamina = ResourceService.maxStamina(target);
                res.mana = ResourceService.maxMana(target);
            }
            case "recall" -> recall(target);
            default -> {
                for (Elixirs.Buff b : t.buffs()) {
                    SkillEffects.attributeBuff(target, b.attr(), "elixir_" + t.id() + "_" + b.label().hashCode(), b.amount() * power, b.op(), t.seconds() * time);
                }
            }
        }
        for (String ex : new HashSet<>(d.extras())) extra(target, ex);
        if (t.kind().equals("ancient")) target.setHealth(target.getMaxHealth());

        // 見た目
        Vector3f col = new Vector3f(((t.color() >> 16) & 255) / 255F, ((t.color() >> 8) & 255) / 255F, (t.color() & 255) / 255F);
        level.sendParticles(new DustParticleOptions(col, 1.2F), target.getX(), target.getY() + 1, target.getZ(), 16, 0.35, 0.5, 0.35, 0);
        if (t.kind().equals("heal") || t.kind().equals("elixir") || t.kind().equals("ancient"))
            level.sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + 1.8, target.getZ(), 3, 0.3, 0.2, 0.3, 0);
        target.displayClientMessage(Component.literal(t.name() + "：").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal(String.join(" ", Elixirs.describe(t, power, time))).withStyle(ChatFormatting.WHITE)), true);
    }

    private static void extra(ServerPlayer p, String id) {
        PlayerResources res = ResourceService.get(p);
        switch (id) {
            case "heal" -> p.heal(4);
            case "stamina" -> res.stamina = Math.min(ResourceService.maxStamina(p), res.stamina + 20);
            case "mana" -> res.mana = Math.min(ResourceService.maxMana(p), res.mana + 15);
            case "cure" -> CookingService.special(p, "cleanse", 0, 0);
            case "might" -> SkillEffects.attributeBuff(p, Attributes.ATTACK_DAMAGE, "elixir_extra_might", 1, AttributeModifier.Operation.ADD_VALUE, 60);
            default -> {
                Object[] e = Elixirs.extraEffect(id);
                if (e != null) p.addEffect(new MobEffectInstance(Elixirs.effectOf(e), (int) e[1] * 20, (int) e[2]));
            }
        }
    }

    /** 帰還：ベッド（リスポーン地点）へ。なければ世界の初めの場所 */
    private static void recall(ServerPlayer p) {
        var server = p.getServer();
        if (server == null) return;
        ServerLevel from = p.serverLevel();
        from.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 60, 0.4, 0.8, 0.4, 0.4);
        BlockPos bed = p.getRespawnPosition();
        ServerLevel to = bed != null ? server.getLevel(p.getRespawnDimension()) : null;
        Vec3 dest;
        if (to != null) {
            dest = Vec3.atBottomCenterOf(bed).add(0, 0.6, 0);
        } else {
            to = server.overworld();
            BlockPos spawn = to.getSharedSpawnPos();
            BlockPos top = to.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn);
            dest = Vec3.atBottomCenterOf(top);
        }
        p.teleportTo(to, dest.x, dest.y, dest.z, p.getYRot(), p.getXRot());
        to.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        to.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 60, 0.4, 0.8, 0.4, 0.1);
    }

    /** 周りの仲間（プレイヤー）みんな */
    public static List<ServerPlayer> around(ServerLevel level, Vec3 at, double radius) {
        return level.getEntitiesOfClass(ServerPlayer.class, new net.minecraft.world.phys.AABB(at, at).inflate(radius),
                p -> p.isAlive() && !p.isSpectator() && p.position().distanceTo(at) <= radius);
    }

    /** 粉塵の広さ */
    public static double dustRadius(ServerPlayer user) {
        double r = 8;
        SkillDefinition s = SkillService.passive(user, "dust_range");
        if (s != null) r += s.power();
        return r;
    }

    private ElixirEffects() {}
}
