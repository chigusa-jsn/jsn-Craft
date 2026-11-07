package com.chigusa.jsncraft.data;

import com.chigusa.jsncraft.job.JobType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * プレイヤー1人分の職業データ。
 * 転職しても前の職業のレベルは progress に残るので、戻ってきたら続きから育てられる。
 */
public class PlayerJobData {
    public static final Codec<PlayerJobData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("life_job").forGetter(d -> Optional.ofNullable(d.lifeJob)),
            ResourceLocation.CODEC.optionalFieldOf("combat_job").forGetter(d -> Optional.ofNullable(d.combatJob)),
            Codec.unboundedMap(ResourceLocation.CODEC, JobProgress.CODEC)
                    .optionalFieldOf("progress", Map.of()).forGetter(d -> d.progress),
            Codec.STRING.optionalFieldOf("title", "").forGetter(d -> d.title)
    ).apply(i, PlayerJobData::fromSaved));

    @Nullable private ResourceLocation lifeJob;
    @Nullable private ResourceLocation combatJob;
    private final Map<ResourceLocation, JobProgress> progress = new HashMap<>();
    private String title = "";

    public PlayerJobData() {}

    private static PlayerJobData fromSaved(Optional<ResourceLocation> life, Optional<ResourceLocation> combat,
                                           Map<ResourceLocation, JobProgress> progress, String title) {
        PlayerJobData d = new PlayerJobData();
        d.lifeJob = life.orElse(null);
        d.combatJob = combat.orElse(null);
        d.progress.putAll(progress);
        d.title = title;
        return d;
    }

    @Nullable
    public ResourceLocation getJob(JobType type) {
        return type == JobType.LIFE ? lifeJob : combatJob;
    }

    public void setJob(JobType type, @Nullable ResourceLocation job) {
        if (type == JobType.LIFE) lifeJob = job;
        else combatJob = job;
    }

    /** その職業の育ち具合。初めての職業なら Lv.1 で作る。 */
    public JobProgress progress(ResourceLocation job) {
        return progress.computeIfAbsent(job, k -> new JobProgress(1, 0));
    }

    /** その職業のレベル。一度も就いたことがなければ 0。 */
    public int level(ResourceLocation job) {
        JobProgress p = progress.get(job);
        return p == null ? 0 : p.level;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
    }
}
