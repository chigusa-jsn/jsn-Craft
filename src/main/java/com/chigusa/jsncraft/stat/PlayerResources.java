package com.chigusa.jsncraft.stat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** 今のマナとスタミナ。-1 は「まだ決まっていない（最大値で始める）」。 */
public class PlayerResources {
    public static final Codec<PlayerResources> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("mana").forGetter(r -> r.mana),
            Codec.FLOAT.fieldOf("stamina").forGetter(r -> r.stamina)
    ).apply(i, PlayerResources::new));

    public float mana = -1;
    public float stamina = -1;

    // 最後にクライアントへ送った値（保存はしない）
    transient int lastSentMana = Integer.MIN_VALUE;
    transient int lastSentStamina = Integer.MIN_VALUE;
    /** 最後にスタミナを使ったゲーム時間（この後しばらくは回復しない） */
    transient long lastStaminaUse = Long.MIN_VALUE / 2;

    public PlayerResources() {}

    private PlayerResources(float mana, float stamina) {
        this.mana = mana;
        this.stamina = stamina;
    }
}
