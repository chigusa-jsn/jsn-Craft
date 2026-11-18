package com.chigusa.jsncraft.alchemy;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.job.JobService;
import com.chigusa.jsncraft.job.JobType;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillService;
import com.chigusa.jsncraft.skill.TimedFlags;
import com.chigusa.jsncraft.stat.PlayerResources;
import com.chigusa.jsncraft.stat.ResourceService;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** 調合（薬づくり）と錬金術。 */
public final class AlchemyService {

    public static final ResourceLocation ALCHEMIST = JsnCraft.id("alchemist");
    public static final ResourceLocation SAGE = JsnCraft.id("alchemy_sage");

    // 状態
    public static final int OK = 0, NOT_ALCHEMIST = 1, NO_BOTTLE = 2, NO_MAIN = 3, UNKNOWN_MAIN = 4, LEVEL_LOW = 5,
            BAD_ADDITIVE = 6, SLOT_LOCKED = 7, FORM_CONFLICT = 8, NEED_DUST_ART = 9, CANT_FORM = 10,
            NOT_SAGE = 11, NO_SELECT = 12, NO_MATERIAL = 13, NO_XP = 14;

    // ================= 職業 =================

    /** 調合の腕のレベル（調合師ならそのレベル、錬金の賢者なら 50 + レベル、それ以外は -1） */
    public static int alchemyLevel(ServerPlayer player) {
        var data = JobService.data(player);
        ResourceLocation life = data.getJob(JobType.LIFE);
        if (ALCHEMIST.equals(life)) return data.level(life);
        if (SAGE.equals(life)) return 50 + data.level(life);
        return -1;
    }

    public static boolean isSage(ServerPlayer player) {
        return SAGE.equals(JobService.data(player).getJob(JobType.LIFE));
    }

    /** 追加素材の枠の数：2（賢者のスキルで3） */
    public static int additiveSlots(ServerPlayer player) {
        return SkillService.passive(player, "third_slot") != null ? 3 : 2;
    }

    public static boolean canDust(ServerPlayer player) {
        return SkillService.passive(player, "dust_art") != null;
    }

    // ================= 調合 =================

    /** 調べた結果：状態・薬の種類・できる薬の中身（品質は作るときに決まる）・まとめて作れる数 */
    public record Brew(int status, Elixirs.Type type, double power, double time, int form, List<String> extras, int max) {
        static Brew of(int status, Elixirs.Type type) {
            return new Brew(status, type, 1, 1, 0, List.of(), 0);
        }
    }

    public static Brew checkBrew(ServerPlayer player, AlchemyTableBlockEntity be) {
        int alv = alchemyLevel(player);
        if (alv < 0) return Brew.of(NOT_ALCHEMIST, null);
        ItemStack main = be.get(1);
        if (main.isEmpty()) return Brew.of(NO_MAIN, null);
        Elixirs.Type t = Elixirs.byMain(main.getItem());
        if (t == null) return Brew.of(UNKNOWN_MAIN, null);
        if (alv < t.unlock()) return Brew.of(LEVEL_LOW, t);
        if (!be.get(0).is(Items.GLASS_BOTTLE)) return Brew.of(NO_BOTTLE, t);
        int slots = additiveSlots(player);
        double power = 1, time = 1;
        int form = ElixirData.DRINK, strong = 0, longer = 0;
        List<String> extras = new ArrayList<>();
        int max = Math.min(be.get(0).getCount(), main.getCount());
        for (int i = 2; i <= 4; i++) {
            ItemStack s = be.get(i);
            if (s.isEmpty()) continue;
            if (i - 2 >= slots) return Brew.of(SLOT_LOCKED, t);
            Elixirs.Additive a = Elixirs.byAdditive(s.getItem());
            if (a == null) return Brew.of(BAD_ADDITIVE, t);
            max = Math.min(max, s.getCount());
            switch (a.kind()) {
                case "strong" -> {
                    if (strong++ < 2) power *= 1.5;
                }
                case "long" -> {
                    if (longer++ < 2) time *= 1.5;
                }
                case "throw", "dust" -> {
                    if (t.selfOnly()) return Brew.of(CANT_FORM, t);
                    if (form != ElixirData.DRINK) return Brew.of(FORM_CONFLICT, t);
                    if (a.kind().equals("dust") && !canDust(player)) return Brew.of(NEED_DUST_ART, t);
                    form = a.kind().equals("dust") ? ElixirData.DUST : ElixirData.THROW;
                }
                default -> {
                    if (!extras.contains(a.id())) extras.add(a.id());
                }
            }
        }
        return new Brew(OK, t, power, time, form, extras, Math.max(1, max));
    }

    /** 品質（★1〜5）：調合の腕が上がるほど、スキルで上がりやすくなる */
    public static int rollQuality(ServerPlayer player, RandomSource r) {
        int alv = Math.max(0, alchemyLevel(player));
        double bonus = 0;
        SkillDefinition q1 = SkillService.passive(player, "brew_quality");
        SkillDefinition q2 = SkillService.passive(player, "brew_quality2");
        if (q1 != null) bonus += q1.power();
        if (q2 != null) bonus += q2.power();
        double score = alv / 150.0 * 3 + bonus * 3 + r.nextDouble() * 1.5;
        return Math.max(1, Math.min(5, 1 + (int) score));
    }

    public static boolean brew(ServerPlayer player, AlchemyTableBlockEntity be, boolean all) {
        Brew b = checkBrew(player, be);
        if (b.status() != OK) return false;
        var r = player.getRandom();
        int times = all ? Math.min(16, b.max()) : 1;
        int quality = rollQuality(player, r);
        ElixirData d = new ElixirData(quality, b.power(), b.time(), b.form(), b.extras());
        SkillDefinition dbl = SkillService.passive(player, "brew_double");
        SkillDefinition save = SkillService.passive(player, "brew_save");
        int made = 0;
        for (int n = 0; n < times; n++) {
            be.get(0).shrink(1);
            be.get(1).shrink(1);
            boolean keep = save != null && r.nextDouble() < save.chance();
            if (!keep) for (int i = 2; i <= 4; i++) if (!be.get(i).isEmpty()) be.get(i).shrink(1);
            made += 1 + (dbl != null && r.nextDouble() < dbl.chance() ? 1 : 0);
        }
        be.setChanged();
        ItemStack out = ModAlchemy.make(b.type(), d, made);
        if (!player.getInventory().add(out)) player.drop(out, false);

        ServerLevel level = player.serverLevel();
        BlockPos pos = be.getBlockPos();
        Vec3 top = Vec3.atBottomCenterOf(pos).add(0, 1.0, 0);
        level.sendParticles(ParticleTypes.EFFECT, top.x, top.y, top.z, 20, 0.25, 0.2, 0.25, 0.05);
        level.sendParticles(ParticleTypes.BUBBLE_POP, top.x, top.y, top.z, 12, 0.2, 0.1, 0.2, 0.02);
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (quality >= 5) level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6F, 1.6F);
        player.displayClientMessage(Component.literal("調合した：").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(out.getHoverName().copy().withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" " + "★".repeat(quality) + " ×" + made).withStyle(ChatFormatting.GOLD)), true);
        JobService.onAction(player, rule -> rule.action().equals("brew"), times * (4 + b.type().unlock() / 5.0 + b.extras().size()));
        return true;
    }

    // ================= 錬金 =================

    public record Trans(int status, int xp) {}

    public static Trans checkTransmute(ServerPlayer player, AlchemyTableBlockEntity be, Transmutes.Recipe rec) {
        if (alchemyLevel(player) < 0) return new Trans(NOT_ALCHEMIST, 0);
        if (!isSage(player)) return new Trans(NOT_SAGE, 0);
        if (rec == null) return new Trans(NO_SELECT, 0);
        int lv = alchemyLevel(player) - 50;
        if (lv < rec.unlock()) return new Trans(LEVEL_LOW, 0);
        int xp = TimedFlags.get(player, "philosopher_stone") > 0 || player.isCreative() ? 0 : rec.xp();
        ItemStack a = be.get(1), bs = be.get(2);
        if (!a.is(rec.a().get()) || a.getCount() < rec.aCount()) return new Trans(NO_MATERIAL, xp);
        if (rec.hasB() && (!bs.is(rec.b().get()) || bs.getCount() < rec.bCount())) return new Trans(NO_MATERIAL, xp);
        if (player.experienceLevel < xp) return new Trans(NO_XP, xp);
        return new Trans(OK, xp);
    }

    public static boolean transmute(ServerPlayer player, AlchemyTableBlockEntity be, Transmutes.Recipe rec) {
        Trans c = checkTransmute(player, be, rec);
        if (c.status() != OK) return false;
        var r = player.getRandom();
        SkillDefinition save = SkillService.passive(player, "transmute_save");
        boolean stone = TimedFlags.get(player, "philosopher_stone") > 0;
        be.get(1).shrink(rec.aCount());
        if (rec.hasB()) be.get(2).shrink(rec.bCount());
        if (c.xp() > 0) player.giveExperienceLevels(-c.xp());
        be.setChanged();
        int count = rec.outCount();
        boolean twice = stone || (save != null && r.nextDouble() < save.chance());
        if (twice) count *= 2;
        ItemStack out = new ItemStack(rec.out().get(), count);
        if (!player.getInventory().add(out)) player.drop(out, false);

        ServerLevel level = player.serverLevel();
        BlockPos pos = be.getBlockPos();
        Vec3 top = Vec3.atBottomCenterOf(pos).add(0, 1.0, 0);
        level.sendParticles(ParticleTypes.END_ROD, top.x, top.y + 0.3, top.z, 20, 0.2, 0.3, 0.2, 0.05);
        level.sendParticles(ParticleTypes.WAX_ON, top.x, top.y + 0.3, top.z, 14, 0.3, 0.3, 0.3, 0.1);
        Vfx.ring(level, top, twice ? 1.8F : 1.2F, 14, Vfx.GOLD);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.2F);
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.5F, 1.8F);
        player.displayClientMessage(Component.literal(twice ? "錬金・倍成功！ " : "錬金した：").withStyle(twice ? ChatFormatting.GOLD : ChatFormatting.YELLOW)
                .append(out.getHoverName().copy().withStyle(ChatFormatting.WHITE)).append(" ×" + count), true);
        JobService.onAction(player, rule -> rule.action().equals("brew"), 6 + rec.unlock() / 5.0);
        return true;
    }

    // ================= スキル =================

    /** 癒しの霧：周りの仲間のHPを回復し、悪い効果を消す */
    public static boolean healingMist(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        for (ServerPlayer p : ElixirEffects.around(level, player.position(), s.radius())) {
            p.heal((float) s.power());
            com.chigusa.jsncraft.cooking.CookingService.special(p, "cleanse", 0, 0);
            level.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.8, p.getZ(), 3, 0.3, 0.2, 0.3, 0);
        }
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.6, player.getZ(), 60, s.radius() * 0.4, 0.4, s.radius() * 0.4, 0.01);
        level.sendParticles(ParticleTypes.EFFECT, player.getX(), player.getY() + 1, player.getZ(), 60, s.radius() * 0.4, 0.6, s.radius() * 0.4, 0.05);
        Vfx.ring(level, player.position().add(0, 0.1, 0), (float) s.radius(), 16, Vfx.GREEN);
        level.playSound(null, player.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0F, 1.4F);
        return true;
    }

    /** 霊薬の雨：周りの仲間のHP・スタミナ・マナを回復し、少しの間再生 */
    public static boolean elixirRain(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        for (ServerPlayer p : ElixirEffects.around(level, player.position(), s.radius())) {
            p.heal((float) s.power());
            PlayerResources res = ResourceService.get(p);
            res.stamina = Math.min(ResourceService.maxStamina(p), res.stamina + (float) (s.power() * 3));
            res.mana = Math.min(ResourceService.maxMana(p), res.mana + (float) (s.power() * 3));
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
            level.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.8, p.getZ(), 4, 0.3, 0.2, 0.3, 0);
        }
        level.sendParticles(ParticleTypes.FALLING_WATER, player.getX(), player.getY() + 4, player.getZ(), 200, s.radius() * 0.5, 0.5, s.radius() * 0.5, 0);
        level.sendParticles(ParticleTypes.EFFECT, player.getX(), player.getY() + 3, player.getZ(), 100, s.radius() * 0.5, 0.5, s.radius() * 0.5, 0.05);
        Vfx.magicCircle(level, player.position(), (float) s.radius() * 0.6F, 30, com.chigusa.jsncraft.vfx.Circle.ALCHEMY);
        level.playSound(null, player.blockPosition(), SoundEvents.WEATHER_RAIN, SoundSource.PLAYERS, 1.0F, 1.2F);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    /** 賢者の石：しばらくの間、錬金で経験値を使わず、必ず2倍できる */
    public static boolean philosopherStone(ServerPlayer player, SkillDefinition s) {
        TimedFlags.set(player, "philosopher_stone", 1, s.duration());
        ServerLevel level = player.serverLevel();
        Vfx.magicCircle(level, player.position(), 3.0F, 40, com.chigusa.jsncraft.vfx.Circle.ALCHEMY);
        Vfx.ring(level, player.position().add(0, 0.05, 0), 3.6F, 24, Vfx.GOLD);
        level.sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getY() + 1.2, player.getZ(), 60, 0.8, 1.0, 0.8, 0.2);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.2F);
        player.displayClientMessage(Component.literal("賢者の石：" + Math.round(s.duration()) + "秒間、錬金がすべて倍成功！")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        return true;
    }

    private AlchemyService() {}
}
