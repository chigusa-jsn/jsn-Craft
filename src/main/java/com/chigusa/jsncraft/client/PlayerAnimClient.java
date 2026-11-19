package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.network.AnimPayloads;
import com.chigusa.jsncraft.weapon.DaggerItem;
import com.chigusa.jsncraft.weapon.GauntletItem;
import com.chigusa.jsncraft.weapon.JobSpearItem;
import com.chigusa.jsncraft.weapon.KatanaItem;
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
    /** 持っているあいだのかまえ（刀の両手持ちなど）。振りの動きより下の層 */
    static final ResourceLocation HOLD = JsnCraft.id("hold");
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
        if (anim == null && name.startsWith("katana_")) anim = PlayerAnimationRegistry.getAnimation(JsnCraft.id("sword_" + name.substring(7)));
        if (anim == null) {
            JsnCraft.LOGGER.warn("[jsn-craft] 体の動き {} が見つかりません（player_animations/weapons.json）", name);
            return;
        }
        // 一人称でも、三人称と同じ腕と武器の動きを見せる（バニラの手の代わりに）
        var play = anim.playAnimation();
        play.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
        play.setFirstPersonConfiguration(new FirstPersonConfiguration(true, true, true, true));
        layer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(2, Ease.OUTQUAD), play, true);
        scheduleSounds(p, name);
    }

    /** 振る音：動きの「振り抜く」瞬間に鳴らす（AnimSounds の表） */
    private record PendingSound(java.lang.ref.WeakReference<AbstractClientPlayer> player, long at, net.minecraft.sounds.SoundEvent sound, float pitch, float lash) {}
    private static final java.util.List<PendingSound> PENDING = new java.util.ArrayList<>();

    static void scheduleSounds(AbstractClientPlayer p, String name) {
        var e = com.chigusa.jsncraft.anim.AnimSounds.SOUNDS.get(name);
        if (e == null && name.startsWith("katana_")) e = com.chigusa.jsncraft.anim.AnimSounds.SOUNDS.get("sword_" + name.substring(7));
        if (e == null) return;
        var holder = com.chigusa.jsncraft.sound.ModSounds.SWING.get(e.kind());
        if (holder == null) return;
        long now = p.level().getGameTime();
        PENDING.removeIf(s -> s.player().get() == p); // 振り直したら前の音はやめる
        // 鞭は、音といっしょに本物の鞭を伸ばす（技の鞭打ちは8ブロック、ふつうは届く距離まで）
        float lash = e.kind().equals("whip") ? (name.equals("whip_lash") ? 8.0F : (float) p.entityInteractionRange()) : 0;
        for (int t : e.ticks()) PENDING.add(new PendingSound(new java.lang.ref.WeakReference<>(p), now + t, holder.get(), 0.92F + p.getRandom().nextFloat() * 0.16F, lash));
    }

    static void tickSounds() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            PENDING.clear();
            return;
        }
        long now = mc.level.getGameTime();
        PENDING.removeIf(s -> {
            AbstractClientPlayer p = s.player().get();
            if (p == null || p.isRemoved()) return true;
            if (s.at() > now) return false;
            mc.level.playLocalSound(p.getX(), p.getY() + 1.2, p.getZ(), s.sound(), net.minecraft.sounds.SoundSource.PLAYERS, 0.9F, s.pitch(), false);
            if (s.lash() > 0) WhipLash.start(p, s.lash());
            return true;
        });
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
        if (i instanceof WhipItem) return c % 2 == 0 ? "whip_crack" : "whip_side";
        if (i instanceof StaffItem) return "staff_strike";
        if (i instanceof KatanaItem) return new String[]{"katana_slash_1", "katana_slash_2", "katana_slash_3"}[c % 3];
        if (i instanceof SwordItem) return new String[]{"sword_slash_1", "sword_slash_2", "sword_slash_3"}[c % 3];
        if (i instanceof AxeItem) return new String[]{"axe_chop_1", "axe_chop_2", "axe_chop_3"}[c % 3];
        if (i instanceof JobSpearItem || i instanceof TridentItem) return new String[]{"spear_thrust", "spear_sweep", "spear_thrust_2"}[c % 3];
        return null;
    }

    private static final java.util.Map<java.util.UUID, String> HOLDING = new java.util.HashMap<>();

    /** 持っている武器のかまえ（刀は両手持ち）。刀を持ったら始めて、持ちかえたらやめる */
    static void tickHold(AbstractClientPlayer p) {
        if (!(PlayerAnimationAccess.getPlayerAssociatedData(p).get(HOLD) instanceof ModifierLayer<?> raw)) return;
        @SuppressWarnings("unchecked") ModifierLayer<IAnimation> layer = (ModifierLayer<IAnimation>) raw;
        Item held = p.getMainHandItem().getItem();
        boolean free = p.getOffhandItem().isEmpty() && !p.isSleeping() && !p.isPassenger() && !p.isSwimming() && !p.isFallFlying()
                && !p.isUsingItem();
        String want = !free ? null
                : held instanceof KatanaItem ? "katana_hold"
                : held instanceof com.chigusa.jsncraft.weapon.JobAxeItem ? "axe_hold"
                : held instanceof JobSpearItem || held instanceof com.chigusa.jsncraft.weapon.SeaTridentItem ? "spear_hold" : null;
        boolean on = layer.getAnimation() != null && layer.getAnimation().isActive();
        String now = HOLDING.get(p.getUUID());
        if (want != null && (!on || !want.equals(now))) {
            IPlayable anim = PlayerAnimationRegistry.getAnimation(JsnCraft.id(want));
            if (anim != null) layer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(4, Ease.INOUTSINE), anim.playAnimation(), true);
            HOLDING.put(p.getUUID(), want);
        } else if (want == null && on) {
            layer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(4, Ease.INOUTSINE), null);
            HOLDING.remove(p.getUUID());
        }
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, value = Dist.CLIENT)
    public static final class Game {
        @SubscribeEvent
        public static void onTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            for (AbstractClientPlayer p : mc.level.players()) tickHold(p);
            tickSounds();
        }

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
            PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(HOLD, 41, player -> new ModifierLayer<>());
        }
    }

    private PlayerAnimClient() {}
}
