package com.chigusa.jsncraft.event;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.command.JobCommand;
import com.chigusa.jsncraft.command.QuestCommand;
import com.chigusa.jsncraft.quest.CustomQuestStore;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import com.chigusa.jsncraft.job.JobManager;
import com.chigusa.jsncraft.job.JobService;
import com.chigusa.jsncraft.job.SkillBoardService;
import com.chigusa.jsncraft.quest.QuestManager;
import com.chigusa.jsncraft.quest.QuestService;
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

    /** ログインしたとき・/reload したときに、調理台のレシピ一覧を送る */
    @SubscribeEvent
    public static void onDatapackSync(net.neoforged.neoforge.event.OnDatapackSyncEvent event) {
        var payload = com.chigusa.jsncraft.network.CookingRecipesPayload.create(false);
        var brewing = com.chigusa.jsncraft.network.CookingRecipesPayload.create(true);
        var seasonings = com.chigusa.jsncraft.network.SeasoningsPayload.create();
        event.getRelevantPlayers().forEach(p -> {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, seasonings);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, payload);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, brewing);
        });
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(JobManager.INSTANCE);
        event.addListener(QuestManager.INSTANCE);
        event.addListener(com.chigusa.jsncraft.fishing.FishingManager.INSTANCE);
        event.addListener(com.chigusa.jsncraft.fishing.FishSpeciesManager.INSTANCE);
        event.addListener(com.chigusa.jsncraft.cooking.CookingManager.RECIPES);
        event.addListener(com.chigusa.jsncraft.cooking.CookingManager.SEASONINGS);
        event.addListener(com.chigusa.jsncraft.cooking.CookingManager.BREWING);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        JobCommand.register(event.getDispatcher());
        QuestCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        CustomQuestStore.onServerStarted(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        CustomQuestStore.onServerStopped();
    }

    // ---- 経験値 ----

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && !player.isCreative()) {
            awardBreak(player, event.getState(), hasSilkTouch(player));
        }
    }

    /**
     * ブロックを壊したときの経験値と依頼。まとめ掘りなどのスキルからも呼ぶ。
     * シルクタッチで鉱石を掘ったときは経験値なし（置いて掘ってを繰り返せないように）。
     */
    public static void awardBreak(ServerPlayer player, net.minecraft.world.level.block.state.BlockState state, boolean silk) {
        if (!(silk && state.is(net.neoforged.neoforge.common.Tags.Blocks.ORES))) {
            JobService.onAction(player, rule -> rule.action().equals("break_block") && rule.matchesBlock(state));
        }
        QuestService.onBreak(player, state);
    }

    public static boolean hasSilkTouch(ServerPlayer player) {
        var reg = player.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                reg.getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), player.getMainHandItem()) > 0;
    }

    /** ブロックを置いたとき（苗木を植えるなど） */
    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.isCreative()) {
            var state = event.getPlacedBlock();
            JobService.onAction(player, rule -> rule.action().equals("place_block") && rule.matchesBlock(state));
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        // 矢などで倒した場合も getEntity() は撃ったプレイヤーになる
        if (event.getSource().getEntity() instanceof ServerPlayer player && !(event.getEntity() instanceof Player)) {
            var victim = event.getEntity();
            JobService.onAction(player, rule -> rule.action().equals("kill_entity") && rule.matchesEntity(victim));
            QuestService.onKill(player, victim);
        }
    }

    // ---- 頭上表示の同期 ----

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SkillBoardService.applyBonuses(player);
            JobService.sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SkillBoardService.applyBonuses(player);
            player.setHealth(player.getMaxHealth());
            JobService.sync(player);
        }
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
