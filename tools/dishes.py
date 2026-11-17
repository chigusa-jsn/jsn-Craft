"""
料理50品のデータ（レシピJSONと言語ファイルをまとめて作る）。
    python3 tools/dishes.py
絵は tools/gen_dish_art.py（ここの "art" を使う）。
"""
import json, os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
R = os.path.join(ROOT, "src/main/resources/")
J = lambda n: "jsn_craft:" + n
F = lambda n: "jsn_craft:fish/" + n
FISH, MEAT, COOKED = "#c:foods/raw_fish", "#c:foods/raw_meat", "#c:foods/cooked_meat"
E = lambda e, s, a=0: {"effect": "minecraft:" + e, "amplifier": a, "seconds": s}
def A(a, v, s, op="add_value"):
    a = a if ":" in a else ("jsn_craft:" + a)
    return {"attribute": a, "amount": v, "operation": op, "seconds": s}
HP, ATK, ASPD = "minecraft:generic.max_health", "minecraft:generic.attack_damage", "minecraft:generic.attack_speed"
LUCK, MINE = "minecraft:generic.luck", "minecraft:player.block_break_speed"

# id, 日本語, 英語, ランク, Lv, 材料, 満腹度, 持ち, xp, バフ
DISHES = [
    # ---- ふつう 15 ----
    ("onigiri", "おにぎり", "Onigiri", "normal", 1, [J("cooked_rice"), J("salt"), "minecraft:dried_kelp"], 7, 0.7, 5, [A("stamina_regen", 0.4, 180)]),
    ("edamame", "枝豆", "Edamame", "normal", 1, [J("soybean"), J("salt")], 4, 0.5, 4, [A(LUCK, 0.5, 180)]),
    ("vegetable_soup", None, None, "normal", 1, None, 0, 0, 0, None),
    ("grilled_fish_set", None, None, "normal", 1, None, 0, 0, 0, None),
    ("miso_soup", "味噌汁", "Miso Soup", "normal", 3, [J("miso"), J("dashi"), J("onion")], 6, 0.8, 6, [A("hp_regen", 0.3, 240)]),
    ("salad", "サラダ", "Garden Salad", "normal", 3, [J("cabbage"), J("tomato"), J("onion")], 6, 0.6, 6, [E("speed", 120)]),
    ("honey_toast", None, None, "normal", 5, None, 0, 0, 0, None),
    ("meat_stew", None, None, "normal", 5, None, 0, 0, 0, None),
    ("pancake", "パンケーキ", "Pancakes", "normal", 5, [J("flour"), "minecraft:egg", "minecraft:milk_bucket", "minecraft:sugar"], 8, 0.8, 8, [E("haste", 180)]),
    ("lemonade", "レモネード", "Lemonade", "normal", 5, [J("lemon"), "minecraft:sugar", "minecraft:honey_bottle"], 4, 0.6, 6, [E("speed", 180), E("regeneration", 10)]),
    ("mushroom_omelette", "きのこオムレツ", "Mushroom Omelette", "normal", 8, ["minecraft:egg", "minecraft:brown_mushroom", J("butter")], 8, 0.8, 8, [A(MINE, 0.1, 240, "add_multiplied_base")]),
    ("yaki_onigiri", "焼きおにぎり", "Grilled Onigiri", "normal", 8, [J("cooked_rice"), J("soy_sauce")], 8, 0.9, 8, [E("fire_resistance", 90)]),
    ("fish_and_chips", None, None, "normal", 10, None, 0, 0, 0, None),
    ("berry_tart", None, None, "normal", 10, None, 0, 0, 0, None),
    ("cabbage_roll", "ロールキャベツ", "Cabbage Rolls", "normal", 10, [J("cabbage"), MEAT, J("tomato_sauce")], 10, 0.9, 10, [A(HP, 4, 240)]),
    # ---- ごちそう 18 ----
    ("sashimi_platter", None, None, "feast", 15, None, 0, 0, 0, None),
    ("curry_rice", "カレーライス", "Curry Rice", "feast", 15, [J("cooked_rice"), J("chili"), J("onion"), MEAT], 12, 1.0, 15, [A(ATK, 2, 300), E("fire_resistance", 120)]),
    ("sushi", "寿司", "Sushi", "feast", 15, [J("cooked_rice"), FISH, J("soy_sauce")], 10, 1.0, 15, [A("fishing_zone", 0.12, 300), A(LUCK, 1, 300)]),
    ("omurice", "オムライス", "Omurice", "feast", 18, [J("cooked_rice"), "minecraft:egg", J("tomato_sauce"), J("onion")], 12, 1.0, 16, [A(HP, 4, 300), E("absorption", 120)]),
    ("ramen", "ラーメン", "Ramen", "feast", 18, [J("dough"), J("dashi"), J("soy_sauce"), COOKED], 12, 1.1, 16, [A("max_stamina", 30, 300), A("stamina_regen", 0.5, 300)]),
    ("miner_lunch", None, None, "feast", 20, None, 0, 0, 0, None),
    ("warrior_steak", None, None, "feast", 20, None, 0, 0, 0, None),
    ("pizza", "ピザ", "Pizza", "feast", 20, [J("dough"), J("tomato_sauce"), J("cheese"), "minecraft:brown_mushroom"], 12, 1.0, 18, [A("skill_damage", 0.08, 300)]),
    ("tempura", "天ぷら", "Tempura", "feast", 20, [J("flour"), "minecraft:egg", FISH, J("salt")], 11, 1.0, 18, [A("crit_chance", 0.06, 300)]),
    ("grape_jelly", "ブドウのゼリー", "Grape Jelly", "feast", 20, [J("grape"), "minecraft:sugar", "minecraft:slime_ball"], 6, 0.8, 16, [A("cooldown_reduction", 0.06, 240)]),
    ("gyudon", "牛丼", "Gyudon", "feast", 22, [J("cooked_rice"), "minecraft:beef", J("onion"), J("soy_sauce")], 14, 1.1, 20, [A(ATK, 1.5, 300), A("damage_reduction", 0.05, 300)]),
    ("hamburger", "ハンバーガー", "Hamburger", "feast", 22, ["minecraft:bread", "minecraft:beef", J("cheese"), J("tomato")], 14, 1.1, 20, [A(HP, 6, 300)]),
    ("ume_ochazuke", "梅茶漬け", "Ume Ochazuke", "feast", 22, [J("cooked_rice"), J("ume"), J("dashi"), J("salt")], 10, 1.0, 18, [A("stamina_cost_reduction", 0.1, 300)]),
    ("gratin", "グラタン", "Gratin", "feast", 24, [J("cheese"), J("butter"), J("flour"), "minecraft:potato"], 13, 1.1, 22, [A("damage_reduction", 0.08, 300)]),
    ("mapo_tofu", "麻婆豆腐", "Mapo Tofu", "feast", 24, [J("soybean"), J("chili"), MEAT, J("miso")], 12, 1.0, 22, [E("strength", 120), E("fire_resistance", 180)]),
    ("lemon_chicken", "レモンチキン", "Lemon Chicken", "feast", 25, ["minecraft:chicken", J("lemon"), J("butter"), J("salt")], 13, 1.1, 22, [E("speed", 240), A("lifesteal", 0.03, 240)]),
    ("paella", "パエリア", "Paella", "feast", 26, [J("cooked_rice"), J("tomato"), FISH, J("lemon")], 13, 1.1, 24, [A("fishing_treasure", 0.04, 300), A("fishing_bite", 0.2, 300)]),
    ("cheese_fondue", "チーズフォンデュ", "Cheese Fondue", "feast", 26, [J("cheese"), J("cheese"), "minecraft:bread", "minecraft:baked_potato"], 13, 1.2, 24, [A("hp_regen", 0.6, 300)]),
    # ---- 逸品 11 ----
    ("golden_feast", None, None, "fine", 30, None, 0, 0, 0, None),
    ("toro_don", "マグロの大トロ丼", "Fatty Tuna Bowl", "fine", 30, [J("cooked_rice"), F("tuna"), J("soy_sauce")], 15, 1.3, 40, [A("skill_damage", 0.12, 360), A("crit_chance", 0.06, 360)]),
    ("swordfish_steak", "カジキのステーキ", "Swordfish Steak", "fine", 32, [F("swordfish"), J("butter"), J("lemon")], 15, 1.3, 40, [A("crit_damage", 0.25, 360)]),
    ("anglerfish_hotpot", "あんこう鍋", "Anglerfish Hotpot", "fine", 32, [F("anglerfish"), J("miso"), J("cabbage"), J("dashi")], 16, 1.3, 42, [A(HP, 8, 360), E("night_vision", 360)]),
    ("eel_kabayaki", "雷ウナギの蒲焼き", "Thunder Eel Kabayaki", "fine", 35, [F("thunder_eel"), J("cooked_rice"), J("soy_sauce"), "minecraft:sugar"], 16, 1.4, 50, [A(ASPD, 0.3, 360), E("speed", 360, 1)]),
    ("golden_carp_nitsuke", "黄金鯉の煮付け", "Simmered Golden Carp", "fine", 35, [F("golden_carp"), J("soy_sauce"), "minecraft:sugar", J("dashi")], 15, 1.3, 50, [A(LUCK, 2, 360), A("job_xp_bonus", 0.15, 360)]),
    ("glacier_sashimi", "氷河サーモンの刺身", "Glacier Salmon Sashimi", "fine", 36, [F("glacier_salmon"), J("soy_sauce"), J("lemon")], 14, 1.3, 50, [A("cooldown_reduction", 0.1, 360), E("speed", 360)]),
    ("coelacanth_stew", "シーラカンスの古代煮", "Ancient Coelacanth Stew", "fine", 38, [F("coelacanth"), J("miso"), J("onion"), J("dashi")], 16, 1.4, 55, [A(HP, 10, 360), A("damage_reduction", 0.08, 360)]),
    ("crimson_curry", "紅蓮カレー", "Crimson Curry", "fine", 38, [F("crimson_piranha"), J("chili"), J("cooked_rice"), J("onion")], 16, 1.4, 55, [E("fire_resistance", 600), A(ATK, 3, 360)]),
    ("soul_eater_skewer", "魂喰らいの串焼き", "Soul Eater Skewer", "fine", 40, [F("soul_eater"), J("salt"), "minecraft:stick", J("chili")], 15, 1.3, 60, [A("lifesteal", 0.06, 360), A("boss_damage", 0.1, 360)]),
    ("void_jelly_dessert", "虚空クラゲのデザート", "Void Jelly Dessert", "fine", 40, [F("void_jelly"), "minecraft:sugar", "minecraft:chorus_fruit"], 12, 1.2, 60, [A("skill_damage", 0.1, 360), A("cooldown_reduction", 0.08, 360), E("slow_falling", 120)]),
    # ---- 伝説 6 ----
    ("moon_lord_sashimi", "月の主の刺身", "Lord of the Moon Sashimi", "legend", 45, [F("moon_lord"), J("soy_sauce"), J("lemon"), "minecraft:glow_berries"], 20, 1.6, 150,
     [A(LUCK, 3, 600), E("night_vision", 900), A("job_xp_bonus", 0.25, 600), A("hp_regen", 1.0, 600)]),
    ("storm_king_steak", "嵐の王の雷鳴ステーキ", "Storm King Thunder Steak", "legend", 45, [F("storm_king"), J("butter"), J("chili"), J("salt")], 20, 1.6, 150,
     [A(ATK, 4, 600), A(ASPD, 0.4, 600), E("speed", 600, 1)]),
    ("ice_queen_carpaccio", "氷の女王のカルパッチョ", "Ice Queen Carpaccio", "legend", 45, [F("ice_queen"), J("lemon"), J("cheese"), J("salt")], 18, 1.6, 150,
     [A("damage_reduction", 0.12, 600), A("cooldown_reduction", 0.12, 600), E("resistance", 300)]),
    ("inferno_dragon_roast", "炎獄の竜魚の姿焼き", "Roasted Inferno Dragonfish", "legend", 48, [F("inferno_dragonfish"), J("chili"), J("salt"), "minecraft:blaze_powder"], 20, 1.6, 180,
     [E("fire_resistance", 1200), E("strength", 600, 1), A("skill_damage", 0.15, 600)]),
    ("soulflame_nabe", "魂炎の大鯰鍋", "Soulflame Catfish Hotpot", "legend", 48, [F("soulflame_catfish"), J("miso"), J("dashi"), J("cabbage")], 20, 1.7, 180,
     [A(HP, 16, 600), A("hp_regen", 1.0, 600), A("lifesteal", 0.06, 600)]),
    ("ender_dragon_feast", "終焉の龍魚の満漢全席", "Ender Dragonfish Grand Banquet", "legend", 50, [F("ender_dragonfish"), "minecraft:golden_apple", J("cooked_rice"), J("soy_sauce")], 20, 2.0, 250,
     [A("skill_damage", 0.2, 900), A("crit_chance", 0.1, 900), A(HP, 10, 900), E("slow_falling", 300), E("regeneration", 60, 1)]),
]

if __name__ == "__main__":
    assert len(DISHES) == 50, len(DISHES)
    ja = json.load(open(R + "assets/jsn_craft/lang/ja_jp.json")); en = json.load(open(R + "assets/jsn_craft/lang/en_us.json"))
    counts = {}
    for no, (id_, jn, enn, rank, lv, ing, n, s, xp, buffs) in enumerate(DISHES, 1):
        counts[rank] = counts.get(rank, 0) + 1
        path = R + f"data/jsn_craft/jsn_cooking/{id_}.json"
        if ing is None:  # 今ある料理：ランクと番号だけ足す
            d = json.load(open(path)); d["rank"] = rank; d["no"] = no
        else:
            time = {"normal": 100, "feast": 160, "fine": 220, "legend": 300}[rank]
            d = {"result": f"jsn_craft:dish/{id_}", "ingredients": ing, "level": lv, "time": time, "xp": xp,
                 "nutrition": n, "saturation": s, "buffs": buffs, "rank": rank, "no": no}
            ja[f"item.jsn_craft.dish.{id_}"] = jn; en[f"item.jsn_craft.dish.{id_}"] = enn
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": f"jsn_craft:item/dish/{id_}"}},
                      open(R + f"assets/jsn_craft/models/item/dish/{id_}.json", "w"), indent=2)
        json.dump(d, open(path, "w"), ensure_ascii=False, indent=2)
    json.dump(ja, open(R + "assets/jsn_craft/lang/ja_jp.json", "w"), ensure_ascii=False, indent=2)
    json.dump(en, open(R + "assets/jsn_craft/lang/en_us.json", "w"), ensure_ascii=False, indent=2)
    # Java の登録リスト
    p = os.path.join(ROOT, "src/main/java/com/chigusa/jsncraft/cooking/ModCooking.java"); src = open(p).read()
    start = src.index("public static final String[] DISHES = {"); end = src.index("};", start) + 2
    names = [d[0] for d in DISHES]
    lines = ",\n            ".join(", ".join(f'"{x}"' for x in names[i:i + 5]) for i in range(0, len(names), 5))
    src = src[:start] + "public static final String[] DISHES = {\n            " + lines + "};" + src[end:]
    open(p, "w").write(src)
    print(counts)
