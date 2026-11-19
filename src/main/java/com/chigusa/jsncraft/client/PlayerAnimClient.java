package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.network.AnimPayloads;
import com.chigusa.jsncraft.weapon.DaggerItem;
import com.chigusa.jsncraft.weapon.GauntletItem;
import com.chigusa.jsncraft.weapon.JobSpearItem;
import com.chigusa.jsncraft.weapon.StaffItem;
import com.chigusa.jsncraft.weapon.WhipItem;
import dev.kosmx.playerAnim.api.IPlayable;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 武器の振り・技のときの体の動きを見せる（Player Animator）。三人称・まわりの人・一人称（腕と武器）で見える。
 * 剣・斧・槍は振るたびに型が変わる（連撃）。短剣の二刀流・籠手は左右の手で動きが変わる。
 */
public final class PlayerAnimClient {

    static final ResourceLocation LAYER = JsnCraft.id("weapon");
    private static int combo;
    private static long lastSwing;

    /** この人に動きをさせる */
    public static void play(AbstractClientPlayer p, String name) {
        if (p == null || name == null) return;
        if (!(PlayerAnimationAccess.getPlayerAssociatedData(p).get(LAYER) instanceof ModifierLayer<?> raw)) {
            JsnCraft.LOGGER.warn("[jsn-craft] 体の動きの層がありません");
            return;
        }
        @SuppressWarnings("unchecked") ModifierLayer<IAnimation> layer = (ModifierLayer<IAnimation>) raw;
        IPlayable anim = PlayerAnimationRegistry.getAnimation(JsnCraft.id(name));
        if (anim == null) {
            JsnCraft.LOGGER.warn("[jsn-craft] 体の動き {} が見つかりません（player_animations/weapons.json）", name);
            return;
        }
        // 一人称でも、三人称と同じ腕と武器の動きを見せる（バニラの手の代わりに）
        var play = anim.playAnimation();
        play.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
        play.setFirstPersonConfiguration(new FirstPersonConfiguration(true, true, true, true));
        layer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(2, Ease.OUTQUAD), play, true);
    }

    /** サーバーから：entityId の人に動きをさせる */
    public static void playOn(int entityId, String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getEntity(entityId) instanceof AbstractClientPlayer p) play(p, name);
    }

    /** 振った武器と手から、動きを決める */
    static String attackAnim(LocalPlayer p) {
        ItemStack main = p.getMainHandItem();
        Item i = main.getItem();
        boolean dual = (i instanceof DaggerItem && p.getOffhandItem().getItem() instanceof DaggerItem)
                || (main.isEmpty() && !GauntletItem.equipped(p).isEmpty());
        boolean left = dual && p.swingingArm == InteractionHand.OFF_HAND;
        long now = p.level().getGameTime();
        if (now - lastSwing > 25) combo = 0;
        lastSwing = now;
        int c = combo++;
        if (i instanceof DaggerItem) return left ? "dagger_left" : "dagger_right";
        if (main.isEmpty()) return GauntletItem.equipped(p).isEmpty() ? null : left ? "fist_left" : "fist_right";
        if (i instanceof WhipItem) return "whip_crack";
        if (i instanceof StaffItem) return "staff_strike";
        if (i instanceof SwordItem) return new String[]{"sword_slash_1", "sword_slash_2", "sword_slash_3"}[c % 3];
        if (i instanceof AxeItem) return new String[]{"axe_chop_1", "axe_chop_2"}[c % 2];
        if (i instanceof JobSpearItem || i instanceof TridentItem) return new String[]{"spear_thrust", "spear_sweep", "spear_thrust"}[c % 3];
        return null;
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, value = Dist.CLIENT)
    public static final class Game {
        /** 攻撃ボタン（ブロックを掘るときは動かさない）。籠手・二刀流の左右は FistClient が先に決める */
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onAttackKey(InputEvent.InteractionKeyMappingTriggered event) {
            if (!event.isAttack() || event.isCanceled()) return;
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer p = mc.player;
            if (p == null || p.isSpectator() || (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK)) return;
            String anim = attackAnim(p);
            if (anim == null) return;
            play(p, anim);
            PacketDistributor.sendToServer(new AnimPayloads.Swing(anim));
        }
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 42, player -> new ModifierLayer<>());
        }
    }

    private PlayerAnimClient() {}
}
