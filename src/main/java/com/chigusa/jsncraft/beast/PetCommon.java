package com.chigusa.jsncraft.beast;

import com.chigusa.jsncraft.job.JobService;
import com.chigusa.jsncraft.job.JobType;
import com.chigusa.jsncraft.magic.HolySpells;
import com.chigusa.jsncraft.stat.ModAttributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * 獣使いの相棒（オオカミ・ミニゴーレム・鷹・ミツバチ）に共通のしくみ。
 * 主人は persistentData の jsn_pet_owner に入れておく。相棒は保存されない（ログアウトで消え、また呼べる）。
 */
public final class PetCommon {

    public static final String OWNER = "jsn_pet_owner", KIND = "jsn_pet_kind";
    public static final String FOCUS = "jsn_pet_focus", FOCUS_UNTIL = "jsn_pet_focus_until";

    public static boolean isPet(Entity e) {
        return e.getPersistentData().hasUUID(OWNER);
    }

    public static UUID ownerId(Entity e) {
        return isPet(e) ? e.getPersistentData().getUUID(OWNER) : null;
    }

    public static ServerPlayer owner(Entity e) {
        UUID id = ownerId(e);
        if (id == null || !(e.level() instanceof ServerLevel level)) return null;
        return level.getServer().getPlayerList().getPlayer(id) instanceof ServerPlayer sp && sp.level() == level ? sp : null;
    }

    public static void setOwner(Entity e, ServerPlayer p, String kind) {
        e.getPersistentData().putUUID(OWNER, p.getUUID());
        e.getPersistentData().putString(KIND, kind);
    }

    public static String kind(Entity e) {
        return e.getPersistentData().getString(KIND);
    }

    /** その人の相棒（kind が空ならぜんぶ） */
    public static List<Mob> petsOf(ServerPlayer p, String kind) {
        List<Mob> out = new ArrayList<>();
        for (Entity e : p.serverLevel().getAllEntities()) {
            if (e instanceof Mob m && m.isAlive() && p.getUUID().equals(ownerId(m)) && (kind.isEmpty() || kind.equals(kind(m)))) out.add(m);
        }
        return out;
    }

    /** 獣使いの腕（獣使いはそのレベル、百獣の王は 50＋レベル） */
    public static int beastLevel(ServerPlayer p) {
        var data = JobService.data(p);
        ResourceLocation c = data.getJob(JobType.COMBAT);
        if (c == null) return 0;
        if (c.getPath().equals("beast_tamer")) return data.level(c);
        if (c.getPath().equals("beast_king")) return 50 + data.level(c);
        return 0;
    }

    /** 呼び出したときの強さ：獣使いの腕・主人の体力・スキルのダメージで上がる */
    public static void applyStats(Mob m, ServerPlayer p, double baseHp, double baseDmg) {
        int lv = beastLevel(p);
        double hp = baseHp * (1 + 0.03 * lv) * Math.max(1.0, p.getMaxHealth() / 20.0);
        double dmg = baseDmg * (1 + 0.025 * lv) * (1 + p.getAttributeValue(ModAttributes.SKILL_DAMAGE));
        var h = m.getAttribute(Attributes.MAX_HEALTH);
        if (h != null) h.setBaseValue(hp);
        var d = m.getAttribute(Attributes.ATTACK_DAMAGE);
        if (d != null) d.setBaseValue(dmg);
        m.setHealth(m.getMaxHealth());
    }

    /** 主人の敵か（主人・仲間・ほかの相棒・パーティの仲間はだめ） */
    public static boolean hostileTo(ServerPlayer owner, LivingEntity e) {
        if (e == owner || !e.isAlive() || isPet(e)) return false;
        if (e instanceof net.minecraft.world.entity.player.Player) return false;
        return com.chigusa.jsncraft.skill.SkillEffects.isEnemy(owner, e);
    }

    /** 主人が鞭で叩いた（または突撃させた）敵を、相棒みんなで狙う */
    public static void focus(ServerPlayer owner, LivingEntity target, int ticks) {
        owner.getPersistentData().putUUID(FOCUS, target.getUUID());
        owner.getPersistentData().putLong(FOCUS_UNTIL, owner.level().getGameTime() + ticks);
        for (Mob m : petsOf(owner, "")) m.setTarget(target);
    }

    /** 相棒が狙うべき敵：主人の指示 → 主人が攻撃した敵 → 主人を攻撃した敵 → 近くの敵 */
    static LivingEntity pickTarget(Mob pet, ServerPlayer owner) {
        ServerLevel level = owner.serverLevel();
        if (owner.getPersistentData().hasUUID(FOCUS) && owner.getPersistentData().getLong(FOCUS_UNTIL) > level.getGameTime()) {
            if (level.getEntity(owner.getPersistentData().getUUID(FOCUS)) instanceof LivingEntity f && f.isAlive() && f.distanceTo(pet) < 24) return f;
        }
        LivingEntity hurt = owner.getLastHurtMob();
        if (hurt != null && hurt.isAlive() && hostileTo(owner, hurt) && hurt.distanceTo(owner) < 16
                && owner.tickCount - owner.getLastHurtMobTimestamp() < 200) return hurt;
        LivingEntity by = owner.getLastHurtByMob();
        if (by != null && by.isAlive() && hostileTo(owner, by) && by.distanceTo(owner) < 16) return by;
        LivingEntity best = null;
        double bd = 100;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, pet.getBoundingBox().inflate(10), e -> e instanceof Enemy && !(e instanceof net.minecraft.world.entity.monster.Creeper) && hostileTo(owner, e))) {
            double d = e.distanceToSqr(pet);
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        return best;
    }

    /** 毎秒：主人がいない（ログアウト・別の世界・獣使いをやめた）なら消える */
    public static void tick(Mob m) {
        if (m.level().isClientSide || m.tickCount % 20 != 0 || !isPet(m)) return;
        ServerPlayer o = owner(m);
        if (o == null || beastLevel(o) <= 0 || !o.isAlive()) {
            if (m.level() instanceof ServerLevel level)
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, m.getX(), m.getY() + 0.5, m.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
            m.discard();
        }
    }

    /** 主人のそばへついていく（遠すぎたら瞬間移動）。主人のいない相棒は消える */
    public static class FollowGoal extends Goal {
        private final PathfinderMob mob;
        private final double speed;

        public FollowGoal(PathfinderMob mob, double speed) {
            this.mob = mob;
            this.speed = speed;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            ServerPlayer o = owner(mob);
            return o != null && mob.getTarget() == null && mob.distanceToSqr(o) > 36;
        }

        @Override
        public boolean canContinueToUse() {
            ServerPlayer o = owner(mob);
            return o != null && mob.getTarget() == null && mob.distanceToSqr(o) > 6 && !mob.getNavigation().isDone();
        }

        @Override
        public void tick() {
            ServerPlayer o = owner(mob);
            if (o == null) return;
            mob.getLookControl().setLookAt(o, 10, mob.getMaxHeadXRot());
            if (mob.distanceToSqr(o) > 24 * 24) {
                Vec3 to = o.position().add(o.getRandom().nextDouble() * 2 - 1, 0.2, o.getRandom().nextDouble() * 2 - 1);
                mob.moveTo(to.x, to.y, to.z, mob.getYRot(), mob.getXRot());
                mob.getNavigation().stop();
                return;
            }
            if (mob.tickCount % 10 == 0) mob.getNavigation().moveTo(o, speed);
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
        }
    }

    /** 狙う敵を決める */
    public static class PickTargetGoal extends TargetGoal {
        private LivingEntity next;

        public PickTargetGoal(Mob mob) {
            super(mob, false);
        }

        @Override
        public boolean canUse() {
            if (mob.tickCount % 10 != 0) return false;
            ServerPlayer o = owner(mob);
            if (o == null) return false;
            next = pickTarget(mob, o);
            return next != null && next != mob.getTarget();
        }

        @Override
        public void start() {
            mob.setTarget(next);
            super.start();
        }
    }

    private PetCommon() {}
}
