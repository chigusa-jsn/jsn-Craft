package com.chigusa.jsncraft.job;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** 職業の枠。生活ジョブと戦闘ジョブを1つずつ持てる。 */
public enum JobType implements StringRepresentable {
    LIFE("life", "生活"),
    COMBAT("combat", "戦闘");

    public static final Codec<JobType> CODEC = StringRepresentable.fromEnum(JobType::values);

    private final String id;
    private final String displayName;

    JobType(String id, String displayName) {
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
