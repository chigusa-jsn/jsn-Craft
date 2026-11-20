package com.chigusa.jsncraft.gacha;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * ガチャの中身：data/jsn_craft/gacha/standard.json（データパックで差しかえられる）。
 * {"rates":[60,28,9,2.5,0.5], "pity":100, "price":10, "tiers":[[{"item":"minecraft:bread","count":8},{"figure":"minecraft:pig"},{"title":"…"}], …]}
 */
public final class GachaPool {
    public static final String[] RARITY = {"ノーマル", "レア", "スーパーレア", "ウルトラレア", "レジェンド"};
    public static final String[] SHORT = {"N", "R", "SR", "UR", "LR"};
    public static final int[] COLOR = {0xFFB0B0B0, 0xFF50A0F0, 0xFFC060F0, 0xFFF0B030, 0xFFFF5070};

    record Entry(String kind, String id, int count, int weight) {}

    public double[] rates = {60, 28, 9, 2.5, 0.5};
    public int pity = 100, price = 10;
    final List<List<Entry>> tiers = new ArrayList<>();

    private static GachaPool cached;
    private static MinecraftServer cachedFor;

    public static GachaPool get(MinecraftServer server) {
        if (cached != null && cachedFor == server) return cached;
        GachaPool p = new GachaPool();
        try {
            var res = server.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("jsn_craft", "gacha/standard.json"));
            if (res.isPresent()) try (var in = new InputStreamReader(res.get().open(), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(in).getAsJsonObject();
                if (o.has("rates")) {
                    JsonArray r = o.getAsJsonArray("rates");
                    for (int i = 0; i < 5 && i < r.size(); i++) p.rates[i] = r.get(i).getAsDouble();
                }
                if (o.has("pity")) p.pity = o.get("pity").getAsInt();
                if (o.has("price")) p.price = o.get("price").getAsInt();
                for (JsonElement te : o.getAsJsonArray("tiers")) {
                    List<Entry> list = new ArrayList<>();
                    for (JsonElement ee : te.getAsJsonArray()) {
                        JsonObject e = ee.getAsJsonObject();
                        int w = e.has("weight") ? e.get("weight").getAsInt() : 1;
                        if (e.has("item")) {
                            String id = e.get("item").getAsString();
                            ResourceLocation rl = ResourceLocation.tryParse(id);
                            if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) continue;   // ないMODのアイテムはとばす
                            list.add(new Entry("item", id, e.has("count") ? e.get("count").getAsInt() : 1, w));
                        } else if (e.has("figure")) {
                            if (FigureItem.type(e.get("figure").getAsString()) == null) continue;
                            list.add(new Entry("figure", e.get("figure").getAsString(), 1, w));
                        } else if (e.has("title")) {
                            list.add(new Entry("title", e.get("title").getAsString(), 1, w));
                        }
                    }
                    p.tiers.add(list);
                }
            }
        } catch (Exception e) {
            com.chigusa.jsncraft.JsnCraft.LOGGER.warn("ガチャの中身が読めなかった", e);
        }
        while (p.tiers.size() < 5) p.tiers.add(new ArrayList<>());
        cached = p;
        cachedFor = server;
        return p;
    }

    /** レア度を決める。luck はレア以上の出やすさ（0〜0.15） */
    public int rollRarity(RandomSource r, double luck) {
        double[] w = rates.clone();
        for (int i = 2; i < 5; i++) w[i] *= 1 + luck * 2;
        double sum = 0;
        for (double v : w) sum += v;
        double x = r.nextDouble() * sum;
        for (int i = 0; i < 5; i++) {
            x -= w[i];
            if (x < 0) return i;
        }
        return 0;
    }

    public ItemStack roll(RandomSource r, int rarity) {
        // そのレア度が空なら、ひとつ下へ
        for (int t = rarity; t >= 0; t--) {
            List<Entry> list = tiers.get(t);
            if (list.isEmpty()) continue;
            int sum = 0;
            for (Entry e : list) sum += e.weight();
            int x = r.nextInt(Math.max(1, sum));
            for (Entry e : list) {
                x -= e.weight();
                if (x < 0) return make(e);
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack make(Entry e) {
        return switch (e.kind()) {
            case "figure" -> FigureItem.of(e.id());
            case "title" -> TitleTicketItem.of(e.id());
            default -> new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(e.id())), e.count());
        };
    }

    /** 中身の見本（画面の「なにが出る？」用）：レア度ごとに最大12こ */
    public List<List<ItemStack>> preview() {
        List<List<ItemStack>> out = new ArrayList<>();
        for (List<Entry> l : tiers) {
            List<ItemStack> a = new ArrayList<>();
            int n = Math.min(8, l.size());
            for (int i = 0; i < n; i++) a.add(make(l.get(i * l.size() / n)));
            out.add(a);
        }
        return out;
    }
}
