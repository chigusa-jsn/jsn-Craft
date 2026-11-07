package com.chigusa.jsncraft.event;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.command.JobCommand;
import com.chigusa.jsncraft.job.JobManager;
import com.chigusa.jsncraft.job.JobService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = JsnCraft.MODID)
public final class GameEvents {

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(JobManager.INSTANCE);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        JobCommand.register(event.getDispatcher());
    }

    // ---- 経験値 ----

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && !player.isCreative()) {
            var state = event.getState();
            JobService.onAction(player, rule -> rule.action().equals("break_block") && rule.matchesBlock(state));
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        // 矢などで倒した場合も getEntity() は撃ったプレイヤーになる
        if (event.getSource().getEntity() instanceof ServerPlayer player && !(event.getEntity() instanceof Player)) {
            var victim = event.getEntity();
            JobService.onAction(player, rule -> rule.action().equals("kill_entity") && rule.matchesEntity(victim));
        }
    }

    // ---- 頭上表示の同期 ----

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) JobService.sync(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) JobService.sync(player);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) JobService.sync(player);
    }

    /** 他のプレイヤーが見える範囲に入ってきたら、その人の表示情報を送る。 */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer receiver) {
            JobService.syncTo(target, receiver);
        }
    }

    private GameEvents() {}
}
