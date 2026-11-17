package com.chigusa.jsncraft.jewelry;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import top.theillusivec4.curios.api.CuriosApi;

/** 宝石の原石が掘ると出る／不死鳥のお守り */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class JewelryEvents {

    /** 石を掘ると 1/400、鉱石を掘ると 3% で原石。深いところほどめずらしい宝石が出やすい */
    @SubscribeEvent
    public static void onDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player) || player.isCreative()) return;
        BlockState st = event.getState();
        boolean ore = st.is(Tags.Blocks.ORES);
        boolean stone = ore || st.is(Tags.Blocks.STONES) || st.is(Blocks.STONE) || st.is(Blocks.DEEPSLATE) || st.is(Blocks.TUFF);
        if (!stone) return;
        double chance = ore ? 0.03 : 0.0025;
        SkillDefinition eye = SkillService.passive(player, "gem_eye");
        if (eye != null) chance *= 1 + eye.power();
        RandomSource r = player.getRandom();
        if (r.nextDouble() >= chance) return;
        BlockPos pos = event.getPos();
        Jewels.Gem g = pick(r, pos.getY() < 0, SkillService.passive(player, "rare_find") != null);
        Item raw = ModJewelry.RAW_GEMS.get(g.index()).get();
        event.getDrops().add(new ItemEntity(event.getLevel(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(raw)));
        event.getLevel().sendParticles(ParticleTypes.WAX_OFF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.1);
        event.getLevel().playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 1.6F);
        com.chigusa.jsncraft.job.JobService.onAction(player, rule -> rule.action().equals("jewel"), 3 + 2 * g.rarity());
        if (g.rarity() == 2) player.displayClientMessage(Component.literal("✦ めずらしい原石を見つけた！").withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    private static Jewels.Gem pick(RandomSource r, boolean deep, boolean rareFind) {
        double[] w = new double[Jewels.GEMS.size()];
        double sum = 0;
        for (Jewels.Gem g : Jewels.GEMS) {
            double v = g.rarity() == 0 ? 5 : g.rarity() == 1 ? 3 : 1;
            if (deep && g.rarity() == 2) v *= 3;
            if (deep && g.rarity() == 1) v *= 1.5;
            if (rareFind && g.rarity() >= 1) v *= 2;
            w[g.index()] = v;
            sum += v;
        }
        double x = r.nextDouble() * sum;
        for (Jewels.Gem g : Jewels.GEMS) {
            x -= w[g.index()];
            if (x <= 0) return g;
        }
        return Jewels.GEMS.get(0);
    }

    /** 不死鳥のお守り：倒れそうになっても一度だけ助かる（10分に1回） */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.isCanceled()) return;
        Item charm = ModJewelry.UNIQUES.get(4).get();
        if (p.getCooldowns().isOnCooldown(charm)) return;
        boolean has = CuriosApi.getCuriosInventory(p).flatMap(h -> h.findFirstCurio(charm)).isPresent();
        if (!has) return;
        event.setCanceled(true);
        p.setHealth(Math.max(6, p.getMaxHealth() * 0.3F));
        p.removeAllEffects();
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0));
        p.getCooldowns().addCooldown(charm, 20 * 600);
        p.serverLevel().sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 60, 0.5, 0.8, 0.5, 0.15);
        p.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 40, 0.5, 0.8, 0.5, 0.3);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.2F);
        p.displayClientMessage(Component.literal("不死鳥のお守りが守ってくれた！（10分後にまた使える）").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
    }

    private JewelryEvents() {}
}
