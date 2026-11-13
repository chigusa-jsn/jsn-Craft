package com.chigusa.jsncraft.skill;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.npc.ReceptionistEntity;
import com.chigusa.jsncraft.party.PartyManager;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** アクティブスキルの中身。effect の名前ごとに動きを書く。 */
public final class SkillEffects {

    /** 一時的な能力アップ：プレイヤー → (付けた能力値, 外す時間) */
    private record Buff(UUID player, Holder<Attribute> attribute, ResourceLocation modifier, long until) {}

    private static final Map<String, Buff> BUFFS = new ConcurrentHashMap<>();

    /** スキルを使う。使えない場面なら false（スタミナや待ち時間は減らない）。 */
    public static boolean activate(ServerPlayer player, SkillDefinition s) {
        return switch (s.effect()) {
            case "sweep" -> sweep(player, s);
            case "dash" -> dash(player, s);
            case "slash_line" -> slashLine(player, s);
            case "buff" -> buff(player, s);
            case "potion" -> potion(player, s);
            case "heal_area" -> healArea(player, s);
            default -> {
                JsnCraft.LOGGER.warn("[jsn-Craft] 知らないスキル効果です: {}", s.effect());
                yield false;
            }
        };
    }

    private static float attackDamage(ServerPlayer player) {
        return (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    /** 攻撃してよい相手か（自分・ほかのプレイヤー・受付嬢・味方のペットはだめ） */
    private static boolean isEnemy(ServerPlayer player, LivingEntity e) {
        if (e == player || e instanceof Player || e instanceof ReceptionistEntity || !e.isAlive()) return false;
        if (e instanceof net.minecraft.world.entity.TamableAnimal pet && pet.isOwnedBy(player)) return false;
        return e.isAttackable();
    }

    // ---- sweep：前方の敵をまとめてなぎ払う ----
    private static boolean sweep(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 center = player.position().add(look.scale(s.radius() * 0.5)).add(0, 1, 0);
        float damage = attackDamage(player) * (float) s.power();
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(s.radius(), 1.0, s.radius()), e -> isEnemy(player, e));
        for (LivingEntity e : targets) {
            Vec3 to = e.position().subtract(player.position()).multiply(1, 0, 1);
            if (to.lengthSqr() > 0.01 && to.normalize().dot(look) < 0.2) continue; // 前方だけ
            e.hurt(player.damageSources().playerAttack(player), damage);
            e.knockback(0.5, -look.x, -look.z);
        }
        for (int i = -2; i <= 2; i++) {
            Vec3 side = new Vec3(-look.z, 0, look.x).scale(i * 0.6);
            Vec3 p = center.add(side);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.9F);
        return true;
    }

    // ---- dash：前に踏み込む。次の一撃が強くなる ----
    private static boolean dash(ServerPlayer player, SkillDefinition s) {
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        player.setDeltaMovement(look.x * s.power(), 0.25, look.z * s.power());
        player.hurtMarked = true; // 動きをクライアントにすぐ伝える
        player.fallDistance = 0;
        if (s.bonus() > 0) {
            PlayerSkillData d = SkillService.data(player);
            d.empowered = s.bonus();
            d.empoweredUntil = player.level().getGameTime() + 60; // 3秒以内の一撃
        }
        player.serverLevel().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2, player.getZ(), 8, 0.3, 0.1, 0.3, 0.02);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 1.3F);
        return true;
    }

    // ---- slash_line：斬撃を飛ばして、一直線上の敵を斬る ----
    private static boolean slashLine(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        Vec3 start = player.getEyePosition();
        Vec3 dir = player.getLookAngle();
        Vec3 end = start.add(dir.scale(s.radius()));
        float damage = attackDamage(player) * (float) s.power();
        AABB box = new AABB(start, end).inflate(1.5);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> isEnemy(player, e))) {
            // 線からの距離が近いものだけ
            Vec3 c = e.getBoundingBox().getCenter();
            double t = Math.max(0, Math.min(s.radius(), c.subtract(start).dot(dir)));
            if (c.distanceTo(start.add(dir.scale(t))) > 1.5) continue;
            e.hurt(player.damageSources().playerAttack(player), damage);
        }
        for (double t = 1; t <= s.radius(); t += 0.7) {
            Vec3 p = start.add(dir.scale(t));
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.0);
            if (((int) (t * 10)) % 3 == 0) level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 1.0F, 1.4F);
        return true;
    }

    // ---- buff：しばらく能力アップ（target に能力値ID、power に割合） ----
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
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6F, 1.5F);
        player.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY() + 1, player.getZ(), 15, 0.4, 0.5, 0.4, 0.1);
        return true;
    }

    /** 時間切れの能力アップを外す（毎tick） */
    public static void tickBuffs(net.minecraft.server.MinecraftServer server) {
        long now = server.overworld().getGameTime();
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

    // ---- potion：ポーション効果（target にエフェクトID） ----
    private static boolean potion(ServerPlayer player, SkillDefinition s) {
        ResourceLocation id = ResourceLocation.tryParse(s.target());
        Optional<Holder.Reference<MobEffect>> effect = id == null ? Optional.empty() : BuiltInRegistries.MOB_EFFECT.getHolder(id);
        if (effect.isEmpty()) return false;
        player.addEffect(new MobEffectInstance(effect.get(), (int) Math.round(s.duration() * 20), s.amplifier()));
        player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1, player.getZ(), 15, 0.5, 0.6, 0.5, 0);
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    // ---- heal_area：自分と近くのパーティーの仲間を回復（power が回復量） ----
    private static boolean healArea(ServerPlayer player, SkillDefinition s) {
        PartyManager.Party party = PartyManager.partyOf(player);
        for (ServerPlayer p : player.serverLevel().players()) {
            boolean ally = p == player || (party != null && party.members.contains(p.getUUID()));
            if (!ally || p.distanceTo(player) > s.radius()) continue;
            p.heal((float) s.power());
            player.serverLevel().sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.8, p.getZ(), 4, 0.3, 0.2, 0.3, 0);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.6F);
        return true;
    }

    private SkillEffects() {}
}
