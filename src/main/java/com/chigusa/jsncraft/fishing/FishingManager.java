package com.chigusa.jsncraft.fishing;

import com.chigusa.jsncraft.JsnCraft;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 魚ごとの「ミニゲームの難しさ」と「動き方」。data/<名前空間>/jsn_fishing/*.json から読む。
 * 書いていない物は、種類（ゴミ・魚・お宝）とレア度から自動で決まる。
 *
 * { "entries": [ { "target": "minecraft:pufferfish", "difficulty": 60, "behavior": "dart" }, … ] }
 *   target   … アイテムID か "#タグ"
 *   difficulty … 0〜100（大きいほど暴れる）
 *   behavior … normal（ふつう） / smooth（なめらか） / dart（すばやい） / sinker（底にいがち） / floater（上にいがち）
 */
public class FishingManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    public static final FishingManager INSTANCE = new FishingManager();

    public static final List<String> BEHAVIORS = List.of("normal", "smooth", "dart", "sinker", "floater");

    public record Entry(String target, int difficulty, String behavior) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("target").forGetter(Entry::target),
                Codec.intRange(0, 100).optionalFieldOf("difficulty", 35).forGetter(Entry::difficulty),
                Codec.STRING.optionalFieldOf("behavior", "normal").forGetter(Entry::behavior)
        ).apply(i, Entry::new));

        boolean matches(ItemStack stack) {
            if (target.startsWith("#")) {
                ResourceLocation tag = ResourceLocation.tryParse(target.substring(1));
                return tag != null && stack.is(TagKey.create(Registries.ITEM, tag));
            }
            ResourceLocation id = ResourceLocation.tryParse(target);
            return id != null && id.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
    }

    private static final Codec<List<Entry>> FILE = Entry.CODEC.listOf().fieldOf("entries").codec();

    private List<Entry> entries = List.of();

    private FishingManager() {
        super(GSON, "jsn_fishing");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller profiler) {
        List<Entry> loaded = new ArrayList<>();
        files.forEach((id, json) -> FILE.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(err -> JsnCraft.LOGGER.error("釣りの設定 {} の読み込みに失敗しました: {}", id, err))
                .ifPresent(loaded::addAll));
        entries = List.copyOf(loaded);
    }

    /** 難しさ（0〜100）と動き方の番号 */
    public int[] settings(ItemStack stack, FishingService.Kind kind) {
        if (kind == FishingService.Kind.LORD) return new int[]{92, 0};
        for (Entry e : entries) {
            if (e.matches(stack)) return new int[]{e.difficulty(), Math.max(0, BEHAVIORS.indexOf(e.behavior()))};
        }
        return switch (kind) {
            case JUNK -> new int[]{10, 3};
            case TREASURE -> new int[]{70, 2};
            default -> new int[]{Math.min(90, 35 + stack.getRarity().ordinal() * 15), 0};
        };
    }
}
