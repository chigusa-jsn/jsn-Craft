package com.chigusa.jsncraft.job;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * 職業の段階。基本職 → 上位職、そして2つを合わせた合体職。
 * xpPerLevel は「1レベル上がるごとに、必要経験値がいくつ増えるか」（基本職20、上位職40、合体職60）。
 */
public enum JobTier implements StringRepresentable {
    BASIC("basic", "基本職", 20),
    ADVANCED("advanced", "上位職", 40),
    HYBRID("hybrid", "合体職", 60);

    public static final Codec<JobTier> CODEC = StringRepresentable.fromEnum(JobTier::values);

    private final String id;
    private final String displayName;
    private final int xpPerLevel;

    JobTier(String id, String displayName, int xpPerLevel) {
        this.id = id;
        this.displayName = displayName;
        this.xpPerLevel = xpPerLevel;
    }

    public String displayName() {
        return displayName;
    }

    public int xpPerLevel() {
        return xpPerLevel;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
