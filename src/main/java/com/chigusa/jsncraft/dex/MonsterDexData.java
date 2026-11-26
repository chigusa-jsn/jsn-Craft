package com.chigusa.jsncraft.dex;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** モンスター図鑑とフィギュア図鑑の記録：倒した数、集めたフィギュア、もらったご褒美。 */
public class MonsterDexData {
    public static final Codec<MonsterDexData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("kills", Map.of()).forGetter(d -> d.kills),
            Codec.STRING.listOf().optionalFieldOf("figures", List.of()).forGetter(d -> new ArrayList<>(d.figures)),
            Codec.STRING.listOf().optionalFieldOf("claimed", List.of()).forGetter(d -> new ArrayList<>(d.claimed))
    ).apply(i, MonsterDexData::new));

    final Map<String, Integer> kills;
    final Set<String> figures;
    final Set<String> claimed;

    public MonsterDexData() {
        this(Map.of(), List.of(), List.of());
    }

    private MonsterDexData(Map<String, Integer> kills, List<String> figures, List<String> claimed) {
        this.kills = new HashMap<>(kills);
        this.figures = new HashSet<>(figures);
        this.claimed = new HashSet<>(claimed);
    }

    /** 倒した数を1つ増やす。初めてなら true */
    public boolean kill(String id) {
        return kills.merge(id, 1, Integer::sum) == 1;
    }

    public int kills(String id) {
        return kills.getOrDefault(id, 0);
    }

    /** フィギュアを記録。初めてなら true */
    public boolean figure(String id) {
        return figures.add(id);
    }

    public boolean hasFigure(String id) {
        return figures.contains(id);
    }

    /** ご褒美をもらった印。初めてなら true */
    public boolean claim(String key) {
        return claimed.add(key);
    }
}
