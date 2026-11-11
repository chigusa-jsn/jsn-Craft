package com.chigusa.jsncraft.party;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.command.PartyCommand;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = JsnCraft.MODID)
public final class PartyEvents {

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        PartyCommand.register(event.getDispatcher());
    }

    /** 0.5秒ごとに、仲間のHP・MPを送る */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 10 == 0) PartyManager.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PartyManager.onLogout(player);
    }

    private PartyEvents() {}
}
