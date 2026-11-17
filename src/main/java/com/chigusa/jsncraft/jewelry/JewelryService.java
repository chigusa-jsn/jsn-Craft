package com.chigusa.jsncraft.jewelry;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.job.JobService;
import com.chigusa.jsncraft.job.JobType;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillEffects;
import com.chigusa.jsncraft.skill.SkillService;
import com.chigusa.jsncraft.stat.ModAttributes;
import com.chigusa.jsncraft.vfx.Vfx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** 細工：装飾品づくり・研磨・はめ込み・特別な装飾品。 */
public final class JewelryService {

    public static final ResourceLocation JEWELER = JsnCraft.id("jeweler");
    public static final ResourceLocation MASTER = JsnCraft.id("gem_master");

    public static final int OK = 0, NOT_JEWELER = 1, NO_SELECT = 2, NO_METAL = 3, BAD_METAL = 4, LEVEL_LOW = 5,
            TOO_MANY_GEMS = 6, BAD_GEM = 7, NO_RAW = 8, NO_JEWELRY = 9, NOT_MASTER = 10, NO_MATERIAL = 11,
            NEED_SKILL = 12, NOT_ENOUGH_METAL = 13, UNIQUE_ITEM = 14;

    // ================= 職業 =================

    /** 細工の腕のレベル（細工師ならそのレベル、宝飾師なら 50 + レベル、それ以外は -1） */
    public static int jewelLevel(ServerPlayer player) {
        var data = JobService.data(player);
        ResourceLocation life = data.getJob(JobType.LIFE);
        if (JEWELER.equals(life)) return data.level(life);
        if (MASTER.equals(life)) return 50 + data.level(life);
        return -1;
    }

    public static boolean isMaster(ServerPlayer player) {
        return MASTER.equals(JobService.data(player).getJob(JobType.LIFE));
    }

    private static void effects(ServerPlayer player, BlockPos pos, boolean great) {
        ServerLevel level = player.serverLevel();
        Vec3 top = Vec3.atBottomCenterOf(pos).add(0, 1.0, 0);
        level.sendParticles(ParticleTypes.WAX_OFF, top.x, top.y + 0.2, top.z, 16, 0.25, 0.2, 0.25, 0.1);
        level.sendParticles(ParticleTypes.END_ROD, top.x, top.y + 0.3, top.z, great ? 20 : 6, 0.2, 0.3, 0.2, 0.03);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.3F);
        level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.4F, 1.6F);
        if (great) level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6F, 1.6F);
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    // ================= 装飾品づくり =================

    /** 宝石の枠（1〜3）に入っている宝石の種類（空きは飛ばす。宝石でない物があれば null） */
    private static List<String> gemsIn(JewelTableBlockEntity be) {
        List<String> out = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            ItemStack s = be.get(i);
            if (s.isEmpty()) continue;
            Jewels.Gem g = ModJewelry.gemOf(s.getItem(), false);
            if (g == null) return null;
            out.add(g.id());
        }
        return out;
    }

    public static int checkMake(ServerPlayer player, JewelTableBlockEntity be, Jewels.Kind kind) {
        int lv = jewelLevel(player);
        if (lv < 0) return NOT_JEWELER;
        if (kind == null) return NO_SELECT;
        ItemStack metal = be.get(0);
        if (metal.isEmpty()) return NO_METAL;
        Jewels.Tier t = Jewels.tierByMetal(metal.getItem());
        if (t == null) return BAD_METAL;
        if (lv < t.unlock()) return LEVEL_LOW;
        if (metal.getCount() < t.count()) return NOT_ENOUGH_METAL;
        List<String> gems = gemsIn(be);
        if (gems == null) return BAD_GEM;
        if (gems.size() > t.sockets()) return TOO_MANY_GEMS;
        return OK;
    }

    public static int rollQuality(ServerPlayer player, RandomSource r) {
        int lv = Math.max(0, jewelLevel(player));
        double bonus = 0;
        SkillDefinition q1 = SkillService.passive(player, "jewel_quality");
        SkillDefinition q2 = SkillService.passive(player, "jewel_quality2");
        if (q1 != null) bonus += q1.power();
        if (q2 != null) bonus += q2.power();
        int q = Math.max(1, Math.min(5, 1 + (int) (lv / 150.0 * 3 + bonus * 3 + r.nextDouble() * 1.5)));
        if (SkillService.passive(player, "fine_setting") != null) q = Math.max(3, q);
        return q;
    }

    public static boolean make(ServerPlayer player, JewelTableBlockEntity be, Jewels.Kind kind) {
        if (checkMake(player, be, kind) != OK) return false;
        var r = player.getRandom();
        Jewels.Tier t = Jewels.tierByMetal(be.get(0).getItem());
        List<String> gems = gemsIn(be);
        int sockets = t.sockets();
        SkillDefinition extra = SkillService.passive(player, "socket_master");
        boolean bonusSocket = extra != null && r.nextDouble() < extra.chance();
        if (bonusSocket) sockets++;
        int quality = rollQuality(player, r);
        SkillDefinition save = SkillService.passive(player, "metal_save");
        if (save == null || r.nextDouble() >= save.chance()) be.get(0).shrink(t.count());
        for (int i = 1; i <= 3; i++) if (!be.get(i).isEmpty()) be.get(i).shrink(1);
        be.setChanged();
        ItemStack out = new ItemStack(ModJewelry.jewelry(t, kind));
        new JewelData(quality, sockets, gems).write(out);
        give(player, out);
        effects(player, be.getBlockPos(), quality >= 5 || bonusSocket);
        player.displayClientMessage(Component.literal("作った：").withStyle(ChatFormatting.AQUA)
                .append(out.getHoverName().copy().withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" " + "★".repeat(quality)).withStyle(ChatFormatting.GOLD))
                .append(bonusSocket ? Component.literal(" 穴がひとつ多い！").withStyle(ChatFormatting.LIGHT_PURPLE) : Component.empty()), true);
        JobService.onAction(player, rule -> rule.action().equals("jewel"), 6 + t.index() * 4 + gems.size() * 2);
        return true;
    }

    // ================= 研磨 =================

    public static int checkPolish(ServerPlayer player, JewelTableBlockEntity be) {
        if (jewelLevel(player) < 0) return NOT_JEWELER;
        ItemStack raw = be.get(0);
        if (raw.isEmpty() || ModJewelry.gemOf(raw.getItem(), true) == null) return NO_RAW;
        return OK;
    }

    public static boolean polish(ServerPlayer player, JewelTableBlockEntity be) {
        if (checkPolish(player, be) != OK) return false;
        ItemStack raw = be.get(0);
        Jewels.Gem g = ModJewelry.gemOf(raw.getItem(), true);
        int n = raw.getCount();
        var r = player.getRandom();
        SkillDefinition bonus = SkillService.passive(player, "polish_bonus");
        SkillDefinition triple = SkillService.passive(player, "triple_polish");
        int made = 0;
        for (int i = 0; i < n; i++) {
            if (triple != null && r.nextDouble() < triple.chance()) made += 3;
            else if (bonus != null && r.nextDouble() < bonus.chance()) made += 2;
            else made++;
        }
        raw.setCount(0);
        be.setChanged();
        while (made > 0) {
            int c = Math.min(64, made);
            give(player, new ItemStack(ModJewelry.GEMS.get(g.index()).get(), c));
            made -= c;
        }
        effects(player, be.getBlockPos(), false);
        player.displayClientMessage(Component.literal("磨いた：" + g.name()).withStyle(ChatFormatting.AQUA), true);
        JobService.onAction(player, rule -> rule.action().equals("jewel"), n * (2 + g.rarity()));
        return true;
    }

    // ================= はめ込み（付け替え・取り外し） =================

    public static int checkSocket(ServerPlayer player, JewelTableBlockEntity be) {
        if (jewelLevel(player) < 0) return NOT_JEWELER;
        ItemStack j = be.get(0);
        if (!(j.getItem() instanceof JewelryItem item)) return NO_JEWELRY;
        if (item.unique() != null) return UNIQUE_ITEM;
        List<String> gems = gemsIn(be);
        if (gems == null) return BAD_GEM;
        if (gems.size() > item.data(j).sockets()) return TOO_MANY_GEMS;
        return OK;
    }

    public static boolean socket(ServerPlayer player, JewelTableBlockEntity be) {
        if (checkSocket(player, be) != OK) return false;
        ItemStack j = be.get(0);
        JewelryItem item = (JewelryItem) j.getItem();
        JewelData d = item.data(j);
        List<String> gems = gemsIn(be);
        for (String old : d.gems()) give(player, new ItemStack(ModJewelry.GEMS.get(Jewels.gem(old).index()).get()));
        for (int i = 1; i <= 3; i++) if (!be.get(i).isEmpty()) be.get(i).shrink(1);
        d.withGems(gems).write(j);
        be.setChanged();
        effects(player, be.getBlockPos(), false);
        player.displayClientMessage(Component.literal(gems.isEmpty() ? "宝石を外した" : "宝石をはめ込んだ").withStyle(ChatFormatting.AQUA), true);
        JobService.onAction(player, rule -> rule.action().equals("jewel"), 2 + gems.size());
        return true;
    }

    // ================= 特別な装飾品 =================

    public static int checkUnique(ServerPlayer player, JewelTableBlockEntity be, Jewels.Unique u) {
        int lv = jewelLevel(player);
        if (lv < 0) return NOT_JEWELER;
        if (!isMaster(player)) return NOT_MASTER;
        if (SkillService.passive(player, "unique_craft") == null) return NEED_SKILL;
        if (u == null) return NO_SELECT;
        if (lv - 50 < u.unlock()) return LEVEL_LOW;
        ItemStack a = be.get(0), b = be.get(1);
        if (!a.is(u.a().get()) || a.getCount() < u.aCount()) return NO_MATERIAL;
        if (!b.is(u.b().get()) || b.getCount() < u.bCount()) return NO_MATERIAL;
        return OK;
    }

    public static boolean makeUnique(ServerPlayer player, JewelTableBlockEntity be, Jewels.Unique u) {
        if (checkUnique(player, be, u) != OK) return false;
        be.get(0).shrink(u.aCount());
        be.get(1).shrink(u.bCount());
        be.setChanged();
        ItemStack out = new ItemStack(ModJewelry.UNIQUES.get(u.index()).get());
        give(player, out);
        effects(player, be.getBlockPos(), true);
        Vfx.ring(player.serverLevel(), Vec3.atBottomCenterOf(be.getBlockPos()).add(0, 1.05, 0), 2.0F, 20, Vfx.GOLD);
        player.getServer().getPlayerList().broadcastSystemMessage(Component.literal("✦ " + player.getGameProfile().getName() + " が「" + u.name() + "」を作り上げた！")
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        JobService.onAction(player, rule -> rule.action().equals("jewel"), 40 + u.unlock());
        return true;
    }

    // ================= スキル =================

    /** 閃光の宝石：周りの敵の目をくらませて、鈍くする */
    public static boolean dazzle(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(s.radius()))) {
            if (!SkillEffects.isEnemy(player, e) || e.distanceTo(player) > s.radius()) continue;
            int ticks = (int) Math.round(s.duration() * 20);
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0), player);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1), player);
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0), player);
            if (e instanceof net.minecraft.world.entity.Mob mob) mob.setTarget(null);
        }
        level.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.2, player.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 60, s.radius() * 0.4, 0.8, s.radius() * 0.4, 0.05);
        Vfx.ring(level, player.position().add(0, 0.1, 0), (float) s.radius(), 16, Vfx.WHITE);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0F, 1.5F);
        return true;
    }

    /** 宝石の守り：自分と周りの仲間に耐性 */
    public static boolean gemGuard(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(s.radius()))) {
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, (int) Math.round(s.duration() * 20), s.amplifier()));
            Vfx.ring(level, p.position().add(0, 0.05, 0), 1.3F, 12, Vfx.AZURE);
            level.sendParticles(ParticleTypes.WAX_OFF, p.getX(), p.getY() + 1, p.getZ(), 14, 0.4, 0.6, 0.4, 0.1);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    /** 輝石の閃光：周りの敵に魔法のダメージ＋目くらまし */
    public static boolean prismBurst(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(s.radius()))) {
            if (!SkillEffects.isEnemy(player, e) || e.distanceTo(player) > s.radius()) continue;
            SkillEffects.hit(player, e, (float) s.power());
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), player);
            Vec3 c = e.getBoundingBox().getCenter();
            level.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 8, 0.3, 0.3, 0.3, 0.08);
        }
        level.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.2, player.getZ(), 1, 0, 0, 0, 0);
        Vfx.magicCircle(level, player.position(), (float) s.radius(), 30, Vfx.WHITE);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
        level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    /** 王の宝冠：周りの仲間の攻撃力・防御力・魔法の威力をしばらく上げる */
    public static boolean royalJewel(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(s.radius()))) {
            SkillEffects.attributeBuff(p, Attributes.ATTACK_DAMAGE, "royal_jewel_atk", s.power() * 20, AttributeModifier.Operation.ADD_VALUE, s.duration());
            SkillEffects.attributeBuff(p, Attributes.ARMOR, "royal_jewel_def", s.power() * 30, AttributeModifier.Operation.ADD_VALUE, s.duration());
            SkillEffects.attributeBuff(p, ModAttributes.MAGIC_POWER, "royal_jewel_magic", s.power(), AttributeModifier.Operation.ADD_VALUE, s.duration());
            level.sendParticles(ParticleTypes.WAX_ON, p.getX(), p.getY() + 2.1, p.getZ(), 16, 0.3, 0.1, 0.3, 0.05);
        }
        Vfx.magicCircle(level, player.position(), 3.0F, 36, Vfx.GOLD);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.4F);
        player.displayClientMessage(Component.literal("王の宝冠：攻撃力 +" + Jewels.num(s.power() * 20) + "・防御力 +" + Jewels.num(s.power() * 30)
                + "・魔法の威力 +" + Math.round(s.power() * 100) + "%（" + Math.round(s.duration()) + "秒）").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    private JewelryService() {}
}
