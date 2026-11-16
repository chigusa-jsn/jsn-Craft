package com.chigusa.jsncraft.fishing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

/**
 * 魚1種類（図鑑の1ページ）。data/<名前空間>/jsn_fish/*.json から読む。
 *
 * no        … 図鑑の番号（小さい順に並ぶ）
 * item      … 釣れるアイテム
 * rank      … common / uncommon / rare / epic / legend
 * weight    … 出やすさ（書かなければランクで決まる）
 * dimension … ディメンション（minecraft:overworld / the_nether / the_end）
 * fluid     … water（水） / lava（溶岩）
 * biomes    … バイオームID か "#タグ"。"freshwater" は海と浜辺以外。空ならどこでも
 * time      … any / day / night      weather … any / clear / rain / thunder
 * moon      … 月の満ち欠け（-1 なら関係なし、0 満月 〜 4 新月）
 * y         … 釣り針の高さの範囲 [下, 上]     size … 大きさの範囲 [最小cm, 最大cm]
 * after_dragon … エンダードラゴンを倒した後だけ
 * hint      … 図鑑に出るヒント     behavior / difficulty … ミニゲームの動き方と難しさ（難しさ -1 はランクで決まる）
 */
public record FishSpecies(int no, ResourceLocation item, String rank, double weight, ResourceLocation dimension, String fluid,
                          List<String> biomes, String time, String weather, int moon, List<Integer> y, List<Double> size,
                          boolean afterDragon, String hint, String behavior, int difficulty) {

    public static final Codec<FishSpecies> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("no", 999).forGetter(FishSpecies::no),
            ResourceLocation.CODEC.fieldOf("item").forGetter(FishSpecies::item),
            Codec.STRING.optionalFieldOf("rank", "common").forGetter(FishSpecies::rank),
            Codec.DOUBLE.optionalFieldOf("weight", -1.0).forGetter(FishSpecies::weight),
            ResourceLocation.CODEC.optionalFieldOf("dimension", ResourceLocation.withDefaultNamespace("overworld")).forGetter(FishSpecies::dimension),
            Codec.STRING.optionalFieldOf("fluid", "water").forGetter(FishSpecies::fluid),
            Codec.STRING.listOf().optionalFieldOf("biomes", List.of()).forGetter(FishSpecies::biomes),
            Codec.STRING.optionalFieldOf("time", "any").forGetter(FishSpecies::time),
            Codec.STRING.optionalFieldOf("weather", "any").forGetter(FishSpecies::weather),
            Codec.INT.optionalFieldOf("moon", -1).forGetter(FishSpecies::moon),
            Codec.INT.listOf().optionalFieldOf("y", List.of(-2048, 4096)).forGetter(FishSpecies::y),
            Codec.DOUBLE.listOf().optionalFieldOf("size", List.of(10.0, 30.0)).forGetter(FishSpecies::size),
            Codec.BOOL.optionalFieldOf("after_dragon", false).forGetter(FishSpecies::afterDragon),
            Codec.STRING.optionalFieldOf("hint", "").forGetter(FishSpecies::hint),
            Codec.STRING.optionalFieldOf("behavior", "normal").forGetter(FishSpecies::behavior),
            Codec.INT.optionalFieldOf("difficulty", -1).forGetter(FishSpecies::difficulty)
    ).apply(i, FishSpecies::new));

    public FishRank rankEnum() {
        return FishRank.byName(rank);
    }

    public double baseWeight() {
        return weight >= 0 ? weight : rankEnum().weight;
    }

    public int difficultyValue() {
        return difficulty >= 0 ? difficulty : rankEnum().difficulty;
    }

    public int behaviorIndex() {
        return Math.max(0, FishingManager.BEHAVIORS.indexOf(behavior));
    }

    /** この場所・時間・天気で釣れるか */
    public boolean matches(ServerLevel level, BlockPos pos, String fluidHere) {
        if (!fluid.equals(fluidHere)) return false;
        if (!level.dimension().location().equals(dimension)) return false;
        if (pos.getY() < y.get(0) || (y.size() > 1 && pos.getY() > y.get(1))) return false;
        long t = level.getDayTime() % 24000;
        boolean day = t < 12300 || t > 23850;
        if (time.equals("day") && !day) return false;
        if (time.equals("night") && day) return false;
        switch (weather) {
            case "clear" -> { if (level.isRaining()) return false; }
            case "rain" -> { if (!level.isRaining()) return false; }
            case "thunder" -> { if (!level.isThundering()) return false; }
            default -> { }
        }
        if (moon >= 0 && level.getMoonPhase() != moon) return false;
        if (afterDragon) {
            var end = level.getServer().getLevel(Level.END);
            if (end == null || end.getDragonFight() == null || !end.getDragonFight().hasPreviouslyKilledDragon()) return false;
        }
        if (biomes.isEmpty()) return true;
        Holder<Biome> biome = level.getBiome(pos);
        for (String b : biomes) {
            if (b.equals("freshwater")) {
                if (!biome.is(BiomeTags.IS_OCEAN) && !biome.is(BiomeTags.IS_BEACH)) return true;
            } else if (b.startsWith("#")) {
                ResourceLocation tag = ResourceLocation.tryParse(b.substring(1));
                if (tag != null && biome.is(TagKey.create(Registries.BIOME, tag))) return true;
            } else {
                ResourceLocation id = ResourceLocation.tryParse(b);
                if (id != null && biome.is(ResourceKey.create(Registries.BIOME, id))) return true;
            }
        }
        return false;
    }

    public float rollSize(net.minecraft.util.RandomSource r) {
        double min = size.get(0), max = size.size() > 1 ? size.get(1) : min;
        double v = min + (max - min) * Math.pow(r.nextDouble(), 1.8); // 大物ほど出にくい
        return (float) (Math.round(v * 10) / 10.0);
    }
}
