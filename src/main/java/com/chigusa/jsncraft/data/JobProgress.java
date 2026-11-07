package com.chigusa.jsncraft.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** ひとつの職業の育ち具合（レベルと、次のレベルまでに貯まった経験値）。 */
public class JobProgress {
    public static final Codec<JobProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("level").forGetter(p -> p.level),
            Codec.INT.fieldOf("xp").forGetter(p -> p.xp)
    ).apply(i, JobProgress::new));

    public int level;
    public int xp;

    public JobProgress(int level, int xp) {
        this.level = level;
        this.xp = xp;
    }
}
