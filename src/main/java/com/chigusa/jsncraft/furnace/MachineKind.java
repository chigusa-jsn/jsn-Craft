package com.chigusa.jsncraft.furnace;

import com.chigusa.jsncraft.job.JobLevels;
import com.chigusa.jsncraft.smithing.SmithingService;
import net.minecraft.server.level.ServerPlayer;

/** 職業の炉の種類：大きい炉（鍛冶師）／コークス炉（掘り師） */
public enum MachineKind {
    BIG_FURNACE("big_furnace", "大きい炉", "鍛冶師", true, 60),
    COKE_OVEN("coke_oven", "コークス炉", "掘り師", false, 100);

    public final String id, name, job;
    /** 燃料が要るか */
    public final boolean needsFuel;
    /** 1個にかかる時間（tick） */
    public final int time;

    MachineKind(String id, String name, String job, boolean needsFuel, int time) {
        this.id = id;
        this.name = name;
        this.job = job;
        this.needsFuel = needsFuel;
        this.time = time;
    }

    /** 動かせる人の腕のレベル（-1 なら動かせない） */
    public int level(ServerPlayer p) {
        return this == BIG_FURNACE ? SmithingService.smithLevel(p) : JobLevels.miner(p);
    }

    /** 腕のレベルによる「多めに取れる」倍率（大きい炉：鉱石が 1.3〜2.0 倍／コークス炉：1.0〜1.8 倍） */
    public double bonus(int lv) {
        if (lv < 0) return 1;
        if (this == BIG_FURNACE) return lv <= 50 ? 1.3 + lv * 0.008 : Math.min(2.0, 1.7 + (lv - 50) * 0.006);
        return lv <= 50 ? 1.0 + lv * 0.01 : Math.min(1.8, 1.5 + (lv - 50) * 0.006);
    }

    public static MachineKind byId(int i) {
        return values()[Math.max(0, Math.min(values().length - 1, i))];
    }
}
