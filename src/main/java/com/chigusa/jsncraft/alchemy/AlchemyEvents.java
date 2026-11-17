package com.chigusa.jsncraft.alchemy;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.farming.ModCrops;
import com.chigusa.jsncraft.job.JobService;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/** 薬草の収穫：調合師の経験値と「薬草の目利き」 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class AlchemyEvents {
    private static final String[] HERBS = {"herb", "vigor_herb", "moon_herb", "antidote_herb", "fire_herb", "mandrake"};

    @SubscribeEvent
    public static void onDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) return;
        BlockState st = event.getState();
        if (!(st.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(st)) return;
        String herb = null;
        for (String h : HERBS) if (st.is(ModCrops.CROP_BLOCKS.get(h).get())) herb = h;
        if (herb == null) return;
        JobService.onAction(player, rule -> rule.action().equals("brew"), herb.equals("mandrake") ? 4 : 1);
        SkillDefinition luck = SkillService.passive(player, "herb_luck");
        if (luck != null && player.getRandom().nextDouble() < luck.chance()) {
            BlockPos pos = event.getPos();
            event.getDrops().add(new ItemEntity(event.getLevel(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    new ItemStack(ModCrops.PRODUCE.get(herb).get(), 1 + player.getRandom().nextInt(2))));
        }
    }

    private AlchemyEvents() {}
}
