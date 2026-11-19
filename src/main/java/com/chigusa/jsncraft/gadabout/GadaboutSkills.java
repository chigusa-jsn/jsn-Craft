package com.chigusa.jsncraft.gadabout;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.casino.Chips;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillEffects;
import com.chigusa.jsncraft.skill.SkillService;
import com.chigusa.jsncraft.vfx.Vfx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 遊び人（気ままな遊び人 → 天下の大遊び人）。生活ジョブ。
 * 運（幸運）を上げるのがいちばんの仕事。カジノの勝負勘は casino/CasinoLuck。
 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class GadaboutSkills {

    private static final ResourceLocation LUCK_ID = JsnCraft.id("gadabout_luck");
    private static final String[] LUCK_PASSIVES = {"gad_luck_1", "gad_luck_2", "gad_luck_3", "gad_luck_4"};

    public static boolean cast(ServerPlayer p, SkillDefinition s) {
        return switch (s.effect()) {
            case "gad_whistle" -> whistle(p, s);
            case "gad_lottery" -> lottery(p, s, new int[]{1000, 100, 10, 3}, new long[]{3000, 300, 30, 10});
            case "gad_big_lottery" -> lottery(p, s, new int[]{1000, 50, 5, 2}, new long[]{100_000, 2000, 200, 60});
            case "gad_heaven" -> heaven(p, s);
            default -> false;
        };
    }

    // ================= いつも：運（幸運）を上げる =================
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 7) return;
        double luck = 0;
        for (String k : LUCK_PASSIVES) {
            SkillDefinition s = SkillService.passive(p, k);
            if (s != null) luck += s.power();
        }
        AttributeInstance inst = p.getAttribute(Attributes.LUCK);
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(LUCK_ID);
        if (luck <= 0) {
            if (cur != null) inst.removeModifier(LUCK_ID);
        } else if (cur == null || cur.amount() != luck) {
            inst.addOrUpdateTransientModifier(new AttributeModifier(LUCK_ID, luck, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    // ================= 拾い物上手：ブロックを壊すと、たまにお宝 =================
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer p) || p.isCreative()) return;
        SkillDefinition s = SkillService.passive(p, "gad_finder");
        if (s == null) return;
        double chance = s.chance() * (1 + p.getAttributeValue(Attributes.LUCK) * 0.05);
        if (p.getRandom().nextDouble() >= chance) return;
        double r = p.getRandom().nextDouble();
        ItemStack loot = r < 0.03 ? new ItemStack(Items.DIAMOND) : r < 0.25 ? new ItemStack(Items.EMERALD) : new ItemStack(Items.GOLD_NUGGET, 1 + p.getRandom().nextInt(3));
        var pos = event.getPos();
        ServerLevel level = p.serverLevel();
        level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, loot));
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.6F, 1.8F);
        p.displayClientMessage(Component.literal("§eラッキー！ ").append(loot.getHoverName()).append(" を拾った"), true);
    }

    // ================= 口笛：まわりの仲間の運を上げる =================
    private static boolean whistle(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        int n = 0;
        for (ServerPlayer o : level.getEntitiesOfClass(ServerPlayer.class, new AABB(p.blockPosition()).inflate(s.radius()))) {
            SkillEffects.attributeBuff(o, Attributes.LUCK, "gad_whistle", s.power(), AttributeModifier.Operation.ADD_VALUE, s.duration());
            level.sendParticles(ParticleTypes.NOTE, o.getX(), o.getY(2.2), o.getZ(), 4, 0.4, 0.2, 0.4, 1);
            if (o != p) o.displayClientMessage(Component.literal("§d" + p.getName().getString() + " の口笛で、運が +" + (int) s.power() + "！"), true);
            n++;
        }
        level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 1.0F, 1.6F);
        SkillEffects.later(p, 4, () -> level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 1.0F, 2.0F));
        p.displayClientMessage(Component.literal("§d♪ 口笛！ §f" + n + "人の運が +" + (int) s.power() + "（" + (int) s.duration() + "秒）"), true);
        return true;
    }

    // ================= 宝くじ：チップで1枚買う =================
    private static boolean lottery(ServerPlayer p, SkillDefinition s, int[] odds, long[] prize) {
        long cost = Math.round(s.bonus());
        if (!Chips.take(p, cost)) {
            p.displayClientMessage(Component.literal("§cカジノチップが " + cost + " 枚いるよ"), true);
            return false;
        }
        ServerLevel level = p.serverLevel();
        // 運が高いと、2回引いて良いほう（運1につき 2%、最大 40%）
        int tries = p.getRandom().nextDouble() < Math.min(0.4, p.getAttributeValue(Attributes.LUCK) * 0.02) ? 2 : 1;
        int best = odds.length;
        for (int t = 0; t < tries; t++) {
            for (int i = 0; i < odds.length; i++) {
                if (p.getRandom().nextInt(odds[i]) == 0) {
                    best = Math.min(best, i);
                    break;
                }
            }
        }
        com.chigusa.jsncraft.casino.CasinoLuck.played(p, cost, best < odds.length ? prize[best] : 0);
        if (best >= odds.length) {
            p.displayClientMessage(Component.literal("§7宝くじ… はずれ"), true);
            level.playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.8F);
            return true;
        }
        long win = prize[best];
        Chips.give(p, win);
        String[] rank = {"§6§l1等", "§e§l2等", "§a3等", "§f4等"};
        p.displayClientMessage(Component.literal(rank[best] + "！ §fチップ " + win + " 枚！"), true);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY(1.2), p.getZ(), 10 + (4 - best) * 10, 0.6, 0.6, 0.6, 0);
        level.playSound(null, p.blockPosition(), best <= 1 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
        if (best == 0) p.server.getPlayerList().broadcastSystemMessage(
                Component.literal("§6§l★ " + p.getName().getString() + " が宝くじの1等！ チップ " + win + " 枚！"), false);
        return true;
    }

    // ================= 天運：しばらく運が大きく上がる =================
    private static boolean heaven(ServerPlayer p, SkillDefinition s) {
        SkillEffects.attributeBuff(p, Attributes.LUCK, "gad_heaven", s.power(), AttributeModifier.Operation.ADD_VALUE, s.duration());
        ServerLevel level = p.serverLevel();
        Vfx.magicCircle(level, p.position(), 2.5F, 40, Vfx.GOLD);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY(1), p.getZ(), 60, 0.6, 0.8, 0.6, 0.3);
        level.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.4F);
        p.displayClientMessage(Component.literal("§6§l天運！ §f" + Math.round(s.duration() / 60) + "分間、運 +" + (int) s.power()), true);
        return true;
    }

    private GadaboutSkills() {}
}
