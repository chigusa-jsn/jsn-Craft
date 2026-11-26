package com.chigusa.jsncraft.capture;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** 杖を持って右クリックしたら、動物のふつうの反応（乗る・座るなど）より先に捕まえる。 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class CaptureEvents {
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getItemStack().getItem() instanceof CaptureWandItem wand) || !(event.getTarget() instanceof LivingEntity le)) return;
        InteractionResult r = wand.interactLivingEntity(event.getItemStack(), event.getEntity(), le, event.getHand());
        event.setCanceled(true);
        event.setCancellationResult(r);
    }

    private CaptureEvents() {}
}
