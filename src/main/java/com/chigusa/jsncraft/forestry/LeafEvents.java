package com.chigusa.jsncraft.forestry;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.job.JobLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 伐り手が斧で葉っぱを壊すと、つながった葉っぱがまとめて壊れる（原木の一刀伐採とは別。しゃがんでいるときは1つだけ）。
 * 伐り手が葉っぱを壊すと「落ち葉」が出る（腕が上がるほど出やすい）。
 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class LeafEvents {
    private static final int MAX = 160;
    private static boolean busy;

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (busy || !(event.getPlayer() instanceof ServerPlayer p) || p.isCreative()) return;
        if (!event.getState().is(BlockTags.LEAVES)) return;
        int lv = JobLevels.lumberjack(p);
        if (lv < 0) return;
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos origin = event.getPos();
        dropLeaves(level, origin, lv, p);
        ItemStack axe = p.getMainHandItem();
        if (!(axe.getItem() instanceof AxeItem) || p.isShiftKeyDown()) return;
        List<BlockPos> leaves = find(level, origin);
        if (leaves.isEmpty()) return;
        busy = true;
        try {
            for (BlockPos pos : leaves) {
                level.destroyBlock(pos, true, p);
                dropLeaves(level, pos, lv, p);
            }
        } finally {
            busy = false;
        }
        axe.hurtAndBreak(Math.max(1, leaves.size() / 16), p, EquipmentSlot.MAINHAND);
        level.playSound(null, origin, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    private static void dropLeaves(ServerLevel level, BlockPos pos, int lv, ServerPlayer p) {
        float chance = 0.25F + Math.min(lv, 80) * 0.006F;
        if (level.random.nextFloat() < chance) Block.popResource(level, pos, new ItemStack(ModForestry.FALLEN_LEAVES.get()));
    }

    /** つながった葉っぱ（種類は問わない、斜めも含む） */
    private static List<BlockPos> find(ServerLevel level, BlockPos origin) {
        List<BlockPos> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        q.add(origin);
        seen.add(origin);
        while (!q.isEmpty() && out.size() < MAX) {
            BlockPos c = q.poll();
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                BlockPos n = c.offset(dx, dy, dz);
                if (n.distSqr(origin) > 12 * 12 || !seen.add(n)) continue;
                if (level.getBlockState(n).is(BlockTags.LEAVES)) {
                    out.add(n);
                    q.add(n);
                }
            }
        }
        return out;
    }

    private LeafEvents() {}
}
