package com.chigusa.jsncraft.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** ひとつの職業の育ち具合（レベル、次のレベルまでの経験値、解放したスキルボードのマス）。 */
public class JobProgress {
    public static final Codec<JobProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("level").forGetter(p -> p.level),
            Codec.INT.fieldOf("xp").forGetter(p -> p.xp),
            Codec.STRING.listOf().optionalFieldOf("unlocked", List.of()).forGetter(p -> new ArrayList<>(p.unlocked))
    ).apply(i, JobProgress::new));

    public int level;
    public int xp;
    public final Set<String> unlocked = new LinkedHashSet<>();

    public JobProgress(int level, int xp) {
        this.level = level;
        this.xp = xp;
    }

    private JobProgress(int level, int xp, List<String> unlocked) {
        this(level, xp);
        this.unlocked.addAll(unlocked);
    }
}
