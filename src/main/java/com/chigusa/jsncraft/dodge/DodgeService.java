package com.chigusa.jsncraft.dodge;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.anim.PlayerAnims;
import com.chigusa.jsncraft.network.AnimPayloads;
import com.chigusa.jsncraft.stat.ModAttributes;
import com.chigusa.jsncraft.stat.ResourceService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ドッジロール：スタミナを使って転がってよける。転がり始めてから1秒は攻撃を受けない（無敵）。
 * 動き（ぐっと進む）は本人のパソコンで先にやって、サーバーはスタミナ・無敵・まわりへの見せ方を受け持つ。
 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class DodgeService {
    public static final float COST = 10;
    /** 無敵の長さ（1秒） */
    public static final int IFRAMES = 20;
    /** 次に転がれるまで（無敵が切れてから少しだけ間をあける） */
    public static final int COOLDOWN = 26;

    private static final Map<UUID, Long> UNTIL = new HashMap<>();
    private static final Map<UUID, Long> LAST = new HashMap<>();

    public static float cost(ServerPlayer p) {
        return (float) (COST * (1 - p.getAttributeValue(ModAttributes.STAMINA_COST_REDUCTION)));
    }

    /** dir：0 前 / 1 後ろ / 2 左 / 3 右 */
    public static void dodge(ServerPlayer p, int dir) {
        if (p.isSpectator() || p.isPassenger() || p.isFallFlying() || p.getAbilities().flying || !p.isAlive()) return;
        long now = p.serverLevel().getGameTime();
        Long last = LAST.get(p.getUUID());
        if (last != null && now - last < COOLDOWN - 2) return;   // 少しだけ通信の遅れを許す
        if (!p.isCreative() && !ResourceService.tryUseStamina(p, cost(p))) {
            p.displayClientMessage(Component.literal("スタミナが足りない").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        LAST.put(p.getUUID(), now);
        UNTIL.put(p.getUUID(), now + IFRAMES);
        ServerLevel level = p.serverLevel();
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5F, 1.6F);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WOOL_STEP, SoundSource.PLAYERS, 0.8F, 0.8F);
        level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.1, p.getZ(), 6, 0.3, 0.05, 0.3, 0.02);
        String anim = switch (dir) {
            case 1 -> "dodge_back";
            case 2 -> "dodge_left";
            case 3 -> "dodge_right";
            default -> "dodge_forward";
        };
        // 本人はもう転がっているので、まわりの人にだけ見せる
        PacketDistributor.sendToPlayersTrackingEntity(p, new AnimPayloads.Play(p.getId(), anim));
    }

    public static boolean invulnerable(ServerPlayer p) {
        Long u = UNTIL.get(p.getUUID());
        return u != null && p.serverLevel().getGameTime() <= u;
    }

    /** 転がっている間（1秒）は攻撃を受けない。奈落や /kill など、よけられない物はそのまま */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !invulnerable(p)) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        event.setCanceled(true);
        p.serverLevel().sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
        p.displayClientMessage(Component.literal("回避！").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
    }

    private DodgeService() {}
}
