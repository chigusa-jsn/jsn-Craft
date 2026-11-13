package com.chigusa.jsncraft.stat;

import com.chigusa.jsncraft.data.ModAttachments;
import com.chigusa.jsncraft.network.ResourceSyncPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * マナとスタミナの管理。毎tick少しずつ回復して、変わったら本人のクライアントに送る。
 * スキルや魔法を作るときは tryUseMana / tryUseStamina を呼べばいい。
 */
public final class ResourceService {

    /** スタミナを使ってから回復が始まるまでの時間（tick）。40 = 2秒 */
    public static final int STAMINA_REGEN_DELAY = 40;

    public static PlayerResources get(ServerPlayer player) {
        return player.getData(ModAttachments.RESOURCES);
    }

    public static float maxMana(ServerPlayer p) {
        return (float) p.getAttributeValue(ModAttributes.MAX_MANA);
    }

    public static float maxStamina(ServerPlayer p) {
        return (float) p.getAttributeValue(ModAttributes.MAX_STAMINA);
    }

    public static void tick(ServerPlayer player) {
        PlayerResources r = get(player);
        float maxMana = maxMana(player);
        float maxStamina = maxStamina(player);
        if (r.mana < 0) r.mana = maxMana;
        if (r.stamina < 0) r.stamina = maxStamina;

        // 1秒(20tick)で回復量の分だけ増える
        r.mana = Math.min(maxMana, r.mana + (float) player.getAttributeValue(ModAttributes.MANA_REGEN) / 20F);
        // スタミナは、使ってから少しのあいだ回復しない
        if (player.level().getGameTime() - r.lastStaminaUse >= STAMINA_REGEN_DELAY) {
            r.stamina = Math.min(maxStamina, r.stamina + (float) player.getAttributeValue(ModAttributes.STAMINA_REGEN) / 20F);
        }

        int m = (int) r.mana, s = (int) r.stamina;
        if (m != r.lastSentMana || s != r.lastSentStamina) {
            r.lastSentMana = m;
            r.lastSentStamina = s;
            PacketDistributor.sendToPlayer(player, new ResourceSyncPayload(r.mana, r.stamina));
        }
    }

    /** 満タンにする（リスポーン時など）。 */
    public static void refill(ServerPlayer player) {
        PlayerResources r = get(player);
        r.mana = maxMana(player);
        r.stamina = maxStamina(player);
        r.lastSentMana = Integer.MIN_VALUE;
    }

    public static boolean tryUseMana(ServerPlayer player, float amount) {
        PlayerResources r = get(player);
        if (r.mana < amount) return false;
        r.mana -= amount;
        return true;
    }

    public static boolean tryUseStamina(ServerPlayer player, float amount) {
        PlayerResources r = get(player);
        if (r.stamina < amount) return false;
        r.stamina -= amount;
        if (amount > 0) r.lastStaminaUse = player.level().getGameTime();
        return true;
    }

    public static void set(ServerPlayer player, float mana, float stamina) {
        PlayerResources r = get(player);
        if (mana >= 0) r.mana = Math.min(mana, maxMana(player));
        if (stamina >= 0) r.stamina = Math.min(stamina, maxStamina(player));
    }

    private ResourceService() {}
}
