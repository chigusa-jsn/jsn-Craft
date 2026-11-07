package com.chigusa.jsncraft.job;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** 職業の段階。基本職 → 上位職、そして2つを合わせた合体職。 */
public enum JobTier implements StringRepresentable {
    BASIC("basic", "基本職"),
    ADVANCED("advanced", "上位職"),
    HYBRID("hybrid", "合体職");

    public static final Codec<JobTier> CODEC = StringRepresentable.fromEnum(JobTier::values);

    private final String id;
    private final String displayName;

    JobTier(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
