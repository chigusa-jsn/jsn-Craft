package com.chigusa.jsncraft.job;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * 職業の段階。基本職 → 上位職、そして2つを合わせた合体職。
 * xpMultiplier は必要経験値の倍率（上位職は2倍、合体職は3倍）。
 */
public enum JobTier implements StringRepresentable {
    BASIC("basic", "基本職", 1),
    ADVANCED("advanced", "上位職", 2),
    HYBRID("hybrid", "合体職", 3);

    public static final Codec<JobTier> CODEC = StringRepresentable.fromEnum(JobTier::values);

    private final String id;
    private final String displayName;
    private final int xpMultiplier;

    JobTier(String id, String displayName, int xpMultiplier) {
        this.id = id;
        this.displayName = displayName;
        this.xpMultiplier = xpMultiplier;
    }

    public String displayName() {
        return displayName;
    }

    public int xpMultiplier() {
        return xpMultiplier;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
