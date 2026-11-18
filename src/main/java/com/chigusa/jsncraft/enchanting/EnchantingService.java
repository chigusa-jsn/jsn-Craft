package com.chigusa.jsncraft.enchanting;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.job.JobService;
import com.chigusa.jsncraft.job.JobType;
import com.chigusa.jsncraft.party.PartyManager;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillEffects;
import com.chigusa.jsncraft.skill.SkillService;
import com.chigusa.jsncraft.skill.TimedFlags;
import com.chigusa.jsncraft.stat.ModAttributes;
import com.chigusa.jsncraft.vfx.Vfx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 付与術：魔紋を刻む・上限突破・付け替え（本に抜き出す／本から付ける）。 */
public final class EnchantingService {

    public static final ResourceLocation ENCHANTER = JsnCraft.id("enchanter");
    public static final ResourceLocation MASTER = JsnCraft.id("rune_master");

    // 状態（画面に出す理由）
    public static final int OK = 0, NO_ITEM = 1, NOT_ENCHANTER = 2, WRONG_ITEM = 3, LEVEL_LOW = 4, MAX = 5, SLOTS_FULL = 6,
            NO_LAPIS = 7, NO_CATALYST = 8, NO_XP = 9, NOT_AT_MAX = 10, OVER_LIMIT = 11, NO_ENCH = 12, CURSE = 13,
            INCOMPATIBLE = 14, NO_SELECT = 15, NO_BOOK = 16;

    /** 調べた結果：状態と、必要なラピス・経験値レベル・触媒の数、上がった後のレベル */
    public record Check(int status, int lapis, int xp, int catalyst, int target) {
        static Check of(int status) {
            return new Check(status, 0, 0, 0, 0);
        }
    }

    // ================= 職業 =================

    /** 付与の腕のレベル（付与術師ならそのレベル、刻印師なら 50 + レベル、それ以外は -1） */
    public static int enchantLevel(ServerPlayer player) {
        var data = JobService.data(player);
        ResourceLocation life = data.getJob(JobType.LIFE);
        if (ENCHANTER.equals(life)) return data.level(life);
        if (MASTER.equals(life)) return 50 + data.level(life);
        return -1;
    }

    public static boolean isMaster(ServerPlayer player) {
        return MASTER.equals(JobService.data(player).getJob(JobType.LIFE));
    }

    /** 1つの物に刻める魔紋の数：付与術師 2 / 刻印師 3（＋スキルで1） */
    public static int runeSlots(ServerPlayer player) {
        int lv = enchantLevel(player);
        if (lv < 0) return 0;
        int n = isMaster(player) ? 3 : 2;
        if (SkillService.passive(player, "rune_slot") != null) n++;
        return n;
    }

    /** 上限突破でいくつまで超えられるか：付与術師 +1（スキルで +2）/ 刻印師 +3（スキルで +5） */
    public static int limitBonus(ServerPlayer player) {
        int lv = enchantLevel(player);
        if (lv < 0) return 0;
        if (isMaster(player)) return SkillService.passive(player, "limit_master") != null ? 5 : 3;
        return SkillService.passive(player, "limit_up") != null ? 2 : 1;
    }

    private static boolean divine(ServerPlayer player) {
        return TimedFlags.get(player, "divine_inscription") > 0;
    }

    private static int lapisAfterSkills(ServerPlayer player, int base) {
        if (divine(player)) return 0;
        SkillDefinition s = SkillService.passive(player, "rune_lapis");
        return s == null ? base : Math.max(1, (int) Math.ceil(base * (1 - Math.min(0.8, s.power()))));
    }

    private static int xpAfterSkills(ServerPlayer player, int base) {
        if (divine(player) || player.isCreative()) return 0;
        SkillDefinition s = SkillService.passive(player, "rune_xp");
        return s == null ? base : Math.max(1, (int) Math.ceil(base * (1 - Math.min(0.8, s.power()))));
    }

    private static Check costs(ServerPlayer player, ItemStack lapisSlot, ItemStack catSlot, Item catalyst,
                               int lapis, int xp, int cat, int target) {
        lapis = lapisAfterSkills(player, lapis);
        xp = xpAfterSkills(player, xp);
        if (divine(player)) cat = 0;
        if (lapis > 0 && (!lapisSlot.is(Items.LAPIS_LAZULI) || lapisSlot.getCount() < lapis)) return new Check(NO_LAPIS, lapis, xp, cat, target);
        if (cat > 0 && (!catSlot.is(catalyst) || catSlot.getCount() < cat)) return new Check(NO_CATALYST, lapis, xp, cat, target);
        if (player.experienceLevel < xp) return new Check(NO_XP, lapis, xp, cat, target);
        return new Check(OK, lapis, xp, cat, target);
    }

    // ================= 魔紋を刻む =================

    public static Check checkRune(ServerPlayer player, RuneAltarBlockEntity be, Runes.Rune rune) {
        int elv = enchantLevel(player);
        if (elv < 0) return Check.of(NOT_ENCHANTER);
        ItemStack item = be.item();
        if (item.isEmpty()) return Check.of(NO_ITEM);
        if (rune == null) return Check.of(NO_SELECT);
        String kinds = RuneData.kinds(item);
        if (kinds.isEmpty() || !rune.fits(kinds)) return Check.of(WRONG_ITEM);
        Map<String, Integer> has = RuneData.runes(item);
        int cur = has.getOrDefault(rune.id(), 0);
        if (cur >= rune.maxLevel()) return new Check(MAX, 0, 0, 0, cur);
        int target = cur + 1;
        if (elv < rune.requiredLevel(target)) return new Check(LEVEL_LOW, 0, 0, 0, target);
        if (cur == 0 && has.size() >= runeSlots(player)) return Check.of(SLOTS_FULL);
        return costs(player, be.lapis(), be.catalyst(), rune.catalyst(),
                Runes.lapisCost(target), Runes.xpCost(target), Runes.catalystCost(target), target);
    }

    public static boolean inscribe(ServerPlayer player, RuneAltarBlockEntity be, Runes.Rune rune) {
        Check c = checkRune(player, be, rune);
        if (c.status() != OK) return false;
        var r = player.getRandom();
        ItemStack item = be.item();
        int target = c.target();
        // 重ね刻み：まれに一気に2つ上がる
        SkillDefinition dbl = SkillService.passive(player, "rune_double");
        boolean twice = dbl != null && target + 1 <= rune.maxLevel() && enchantLevel(player) >= rune.requiredLevel(target + 1)
                && r.nextDouble() < dbl.chance();
        if (twice) target++;
        be.lapis().shrink(c.lapis());
        SkillDefinition save = SkillService.passive(player, "rune_save");
        if (save == null || r.nextDouble() >= save.chance()) be.catalyst().shrink(c.catalyst());
        if (c.xp() > 0) player.giveExperienceLevels(-c.xp());
        RuneData.set(item, rune.id(), target);
        be.setChanged();

        ServerLevel level = player.serverLevel();
        BlockPos pos = be.getBlockPos();
        Vec3 top = Vec3.atBottomCenterOf(pos).add(0, 1.05, 0);
        level.sendParticles(ParticleTypes.ENCHANT, top.x, top.y + 0.5, top.z, 60, 0.5, 0.6, 0.5, 1.0);
        level.sendParticles(ParticleTypes.WITCH, top.x, top.y + 0.3, top.z, 14, 0.3, 0.3, 0.3, 0.05);
        Vfx.ring(level, top, twice ? 2.4F : 1.6F, 16, twice ? Vfx.GOLD : Vfx.WHITE);
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, twice ? 1.6F : 1.2F);
        player.displayClientMessage(Component.literal(twice ? "重ね刻み！ " : "魔紋を刻んだ： ").withStyle(twice ? ChatFormatting.GOLD : ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
                .append(Component.literal("✦" + rune.name() + " " + Runes.roman(target)).withStyle(ChatFormatting.LIGHT_PURPLE)), true);
        JobService.onAction(player, rule -> rule.action().equals("enchant"), 8 + 6 * target);
        return true;
    }

    // ================= 上限突破 =================

    /** 上限を n 超えるのに使う触媒 */
    public static Item limitCatalyst(int bonus) {
        return bonus <= 2 ? Items.DIAMOND : bonus <= 4 ? Items.ECHO_SHARD : Items.NETHER_STAR;
    }

    public static Check checkLimit(ServerPlayer player, RuneAltarBlockEntity be, int index) {
        if (enchantLevel(player) < 0) return Check.of(NOT_ENCHANTER);
        ItemStack item = be.item();
        if (item.isEmpty()) return Check.of(NO_ITEM);
        var list = RuneData.enchantments(item);
        if (list.isEmpty()) return Check.of(NO_ENCH);
        var e = list.get(Math.floorMod(index, list.size()));
        Holder<Enchantment> h = e.getKey();
        if (h.is(EnchantmentTags.CURSE)) return Check.of(CURSE);
        int max = h.value().getMaxLevel();
        int lv = e.getValue();
        if (lv < max) return new Check(NOT_AT_MAX, 0, 0, 0, lv);
        int bonus = lv + 1 - max;
        if (bonus > limitBonus(player)) return new Check(OVER_LIMIT, 0, 0, 0, lv);
        return costs(player, be.lapis(), be.catalyst(), limitCatalyst(bonus), 8 * bonus, 5 + 5 * bonus, 1, lv + 1);
    }

    public static boolean limitBreak(ServerPlayer player, RuneAltarBlockEntity be, int index) {
        Check c = checkLimit(player, be, index);
        if (c.status() != OK) return false;
        ItemStack item = be.item();
        var list = RuneData.enchantments(item);
        Holder<Enchantment> h = list.get(Math.floorMod(index, list.size())).getKey();
        int target = c.target();
        be.lapis().shrink(c.lapis());
        be.catalyst().shrink(c.catalyst());
        if (c.xp() > 0) player.giveExperienceLevels(-c.xp());
        EnchantmentHelper.updateEnchantments(item, m -> m.set(h, target));
        be.setChanged();

        ServerLevel level = player.serverLevel();
        BlockPos pos = be.getBlockPos();
        Vec3 top = Vec3.atBottomCenterOf(pos).add(0, 1.05, 0);
        level.sendParticles(ParticleTypes.END_ROD, top.x, top.y + 0.4, top.z, 30, 0.3, 0.5, 0.3, 0.08);
        level.sendParticles(ParticleTypes.ENCHANT, top.x, top.y + 0.5, top.z, 80, 0.5, 0.6, 0.5, 1.2);
        Vfx.magicCircle(level, Vec3.atBottomCenterOf(pos), 2.0F, 30, com.chigusa.jsncraft.vfx.Circle.RUNE);
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.7F);
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.8F, 1.4F);
        int bonus = target - h.value().getMaxLevel();
        player.displayClientMessage(Component.literal("上限突破！ ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Enchantment.getFullname(h, target)), true);
        if (bonus >= 3) {
            level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("✦ " + player.getGameProfile().getName() + " が ")
                    .withStyle(ChatFormatting.LIGHT_PURPLE).append(Enchantment.getFullname(h, target))
                    .append(Component.literal(" に至った！").withStyle(ChatFormatting.LIGHT_PURPLE)), false);
        }
        JobService.onAction(player, rule -> rule.action().equals("enchant"), 15 * bonus);
        return true;
    }

    // ================= 付け替え（本に抜き出す／本から付ける） =================

    private static final int MOVE_LAPIS = 4, MOVE_XP = 3;

    public static Check checkTransfer(ServerPlayer player, RuneAltarBlockEntity be, int index) {
        if (enchantLevel(player) < 0) return Check.of(NOT_ENCHANTER);
        ItemStack item = be.item(), cat = be.catalyst();
        if (item.isEmpty()) return Check.of(NO_ITEM);
        if (cat.is(Items.BOOK)) {
            if (item.is(Items.ENCHANTED_BOOK)) return Check.of(WRONG_ITEM);
            var list = RuneData.enchantments(item);
            if (list.isEmpty()) return Check.of(NO_ENCH);
            var e = list.get(Math.floorMod(index, list.size()));
            if (e.getKey().is(EnchantmentTags.CURSE)) return Check.of(CURSE);
            return costs(player, be.lapis(), cat, Items.BOOK, MOVE_LAPIS, MOVE_XP, 1, e.getValue());
        }
        if (cat.is(Items.ENCHANTED_BOOK)) {
            if (item.is(Items.ENCHANTED_BOOK) || item.is(Items.BOOK)) return Check.of(WRONG_ITEM);
            if (applicable(item, cat).isEmpty()) return Check.of(INCOMPATIBLE);
            return costs(player, be.lapis(), cat, Items.ENCHANTED_BOOK, MOVE_LAPIS, MOVE_XP, 1, 0);
        }
        return Check.of(NO_BOOK);
    }

    /** 本のエンチャントのうち、その物に付けられるもの（レベルは上限突破のまま） */
    private static List<EnchantmentInstance> applicable(ItemStack item, ItemStack book) {
        List<EnchantmentInstance> out = new ArrayList<>();
        var have = EnchantmentHelper.getEnchantmentsForCrafting(item);
        for (var e : RuneData.enchantments(book)) {
            Holder<Enchantment> h = e.getKey();
            if (!h.value().isSupportedItem(item)) continue;
            if (have.getLevel(h) >= e.getValue()) continue;
            List<Holder<Enchantment>> others = new ArrayList<>();
            for (Holder<Enchantment> k : have.keySet()) if (!k.equals(h)) others.add(k);
            if (!EnchantmentHelper.isEnchantmentCompatible(others, h)) continue;
            out.add(new EnchantmentInstance(h, e.getValue()));
        }
        return out;
    }

    public static boolean transfer(ServerPlayer player, RuneAltarBlockEntity be, int index) {
        Check c = checkTransfer(player, be, index);
        if (c.status() != OK) return false;
        ItemStack item = be.item(), cat = be.catalyst();
        ServerLevel level = player.serverLevel();
        BlockPos pos = be.getBlockPos();
        if (cat.is(Items.BOOK)) {
            var list = RuneData.enchantments(item);
            var e = list.get(Math.floorMod(index, list.size()));
            Holder<Enchantment> h = e.getKey();
            ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(h, e.getValue()));
            EnchantmentHelper.updateEnchantments(item, m -> m.set(h, 0));
            cat.shrink(1);
            if (!player.getInventory().add(book)) player.drop(book, false);
            player.displayClientMessage(Component.literal("本に抜き出した： ").withStyle(ChatFormatting.AQUA)
                    .append(Enchantment.getFullname(h, e.getValue())), true);
        } else {
            List<EnchantmentInstance> add = applicable(item, cat);
            for (EnchantmentInstance in : add) EnchantmentHelper.updateEnchantments(item, m -> m.set(in.enchantment, in.level));
            cat.shrink(1);
            player.displayClientMessage(Component.literal("本のエンチャントを付けた（" + add.size() + "つ）").withStyle(ChatFormatting.AQUA), true);
        }
        be.lapis().shrink(c.lapis());
        if (c.xp() > 0) player.giveExperienceLevels(-c.xp());
        be.setChanged();
        Vec3 top = Vec3.atBottomCenterOf(pos).add(0, 1.05, 0);
        level.sendParticles(ParticleTypes.ENCHANT, top.x, top.y + 0.5, top.z, 40, 0.4, 0.5, 0.4, 0.8);
        level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.7F, 1.4F);
        JobService.onAction(player, rule -> rule.action().equals("enchant"), 6);
        return true;
    }

    // ================= スキル =================

    private static List<ServerPlayer> allies(ServerPlayer player, double radius) {
        PartyManager.Party party = PartyManager.partyOf(player);
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : player.serverLevel().players()) {
            boolean ally = p == player || (party != null && party.members.contains(p.getUUID()));
            if (ally && p.distanceTo(player) <= radius) out.add(p);
        }
        return out;
    }

    /** 魔紋の障壁：自分と周りの仲間に、ダメージを吸収する障壁 */
    public static boolean runeBarrier(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        for (ServerPlayer p : allies(player, s.radius())) {
            p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, (int) Math.round(s.duration() * 20), s.amplifier()));
            Vfx.ring(level, p.position().add(0, 0.05, 0), 1.4F, 14, Vfx.AZURE);
            level.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.2, p.getZ(), 25, 0.4, 0.6, 0.4, 0.6);
        }
        Vfx.magicCircle(level, player.position(), (float) Math.max(1.5, s.radius() * 0.4), 30, com.chigusa.jsncraft.vfx.Circle.RUNE);
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.2F);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    /** 魔紋の嵐：周りの敵に魔法のダメージ＋弱体化 */
    public static boolean runeStorm(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        int hits = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(s.radius()))) {
            if (!SkillEffects.isEnemy(player, e) || e.distanceTo(player) > s.radius()) continue;
            SkillEffects.hit(player, e, (float) s.power());
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            level.sendParticles(ParticleTypes.WITCH, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 10, 0.3, 0.4, 0.3, 0.05);
            hits++;
        }
        Vfx.magicCircle(level, player.position(), (float) s.radius(), 30, com.chigusa.jsncraft.vfx.Circle.RUNE);
        Vfx.ring(level, player.position().add(0, 0.1, 0), (float) s.radius(), 16, Vfx.WHITE);
        level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1, player.getZ(), 120, s.radius() * 0.4, 1.0, s.radius() * 0.4, 1.5);
        level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.2F);
        level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.8F, 1.0F);
        return true;
    }

    /** 魔紋の加護：周りの仲間の魔法の威力と回復の力を上げる */
    public static boolean runicAura(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        for (ServerPlayer p : allies(player, s.radius())) {
            SkillEffects.attributeBuff(p, ModAttributes.MAGIC_POWER, "runic_aura_magic", s.power(), AttributeModifier.Operation.ADD_VALUE, s.duration());
            SkillEffects.attributeBuff(p, ModAttributes.HEALING_POWER, "runic_aura_heal", s.power(), AttributeModifier.Operation.ADD_VALUE, s.duration());
            level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
        }
        Vfx.magicCircle(level, player.position(), 2.6F, 36, com.chigusa.jsncraft.vfx.Circle.RUNE);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.5F);
        player.displayClientMessage(Component.literal("魔紋の加護：魔法の威力・回復の力 +" + Math.round(s.power() * 100) + "%（" + Math.round(s.duration()) + "秒）")
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }

    /** 神紋の祝福：しばらくの間、刻む・上限突破・付け替えにラピスも触媒も経験値もいらない */
    public static boolean divineInscription(ServerPlayer player, SkillDefinition s) {
        TimedFlags.set(player, "divine_inscription", 1, s.duration());
        ServerLevel level = player.serverLevel();
        Vfx.magicCircle(level, player.position(), 3.2F, 40, com.chigusa.jsncraft.vfx.Circle.RUNE);
        Vfx.ring(level, player.position().add(0, 0.05, 0), 4.0F, 24, Vfx.WHITE);
        level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.5, player.getZ(), 150, 1.0, 1.2, 1.0, 1.5);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.8F);
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 0.6F);
        player.displayClientMessage(Component.literal("神紋の祝福：" + Math.round(s.duration()) + "秒間、素材も経験値もいらない！")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        return true;
    }

    private EnchantingService() {}
}
