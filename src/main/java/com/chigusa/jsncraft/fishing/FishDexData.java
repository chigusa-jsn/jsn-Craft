package com.chigusa.jsncraft.fishing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;

/** プレイヤーの魚図鑑：魚ごとの釣った数と最大サイズ。 */
public class FishDexData {
    public record Rec(int count, float best) {
        public static final Codec<Rec> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("count").forGetter(Rec::count),
                Codec.FLOAT.fieldOf("best").forGetter(Rec::best)
        ).apply(i, Rec::new));
    }

    public static final Codec<FishDexData> CODEC = Codec.unboundedMap(Codec.STRING, Rec.CODEC)
            .xmap(FishDexData::new, d -> d.records);

    public static final int NONE = 0, BEST = 1, NEW = 2;

    private final Map<String, Rec> records;

    public FishDexData() {
        this(new HashMap<>());
    }

    private FishDexData(Map<String, Rec> records) {
        this.records = new HashMap<>(records);
    }

    /** 記録する。初めてなら NEW、自己ベストなら BEST */
    public int record(String id, float size) {
        Rec old = records.get(id);
        if (old == null) {
            records.put(id, new Rec(1, size));
            return NEW;
        }
        records.put(id, new Rec(old.count() + 1, Math.max(old.best(), size)));
        return size > old.best() ? BEST : NONE;
    }

    public Rec get(String id) {
        return records.get(id);
    }
}
