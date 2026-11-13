package com.chigusa.jsncraft.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * スキルバー（5枠）にセットしたスキルと、待ち時間。
 * slots は "jsn_craft:sword/sweep" のようなスキルID（空文字は空き枠）。
 * 待ち時間は保存しない（ログインし直すとリセット）。
 */
public class PlayerSkillData {
    public static final int SLOTS = 5;

    public static final Codec<PlayerSkillData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().optionalFieldOf("slots", List.of()).forGetter(d -> d.slots)
    ).apply(i, PlayerSkillData::new));

    public final List<String> slots = new ArrayList<>();
    /** スキルID → 使えるようになるゲーム時間（tick） */
    public final transient Map<String, Long> readyAt = new HashMap<>();
    /** 踏み込みなどで「次の一撃が強くなる」状態：倍率と期限 */
    public transient double empowered = 0;
    public transient long empoweredUntil = 0;
    /** ため中のスキル（なければ null）と、出る時間 */
    public transient String casting = null;
    public transient long castEnd = 0;
    /** 受け流しのかまえ：期限と反撃の強さ */
    public transient long parryUntil = 0;
    public transient double parryPower = 0;

    public PlayerSkillData() {
        while (slots.size() < SLOTS) slots.add("");
    }

    private PlayerSkillData(List<String> saved) {
        slots.addAll(saved);
        while (slots.size() < SLOTS) slots.add("");
        while (slots.size() > SLOTS) slots.remove(slots.size() - 1);
    }
}
