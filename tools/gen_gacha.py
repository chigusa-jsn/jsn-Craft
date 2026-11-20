"""ガチャガチャ：絵・モデル・レシピ・名前・中身（data/jsn_craft/gacha/standard.json）"""
import json, os
from PIL import Image, ImageDraw

R = os.path.join(os.path.dirname(__file__), "..", "src/main/resources")
A = f"{R}/assets/jsn_craft"
D = f"{R}/data/jsn_craft"

def hx(s): s = s.lstrip("#"); return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)
def save(img, path): os.makedirs(os.path.dirname(path), exist_ok=True); img.save(path)
def wj(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f: json.dump(obj, f, ensure_ascii=False, indent=2); f.write("\n")
def tex(name, draw, size=32):
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0)); draw(ImageDraw.Draw(im), im); save(im, f"{A}/textures/block/gacha/{name}.png")

PINK, PINK_D, PINK_L, WHITE = hx("#d04070"), hx("#8a2048"), hx("#f080a0"), hx("#fff0f4")
CAPS = ["#e05050", "#50a0e0", "#f0d040", "#60c060", "#e070c0", "#f09040", "#a070f0"]

def pedestal(d, im):
    d.rectangle([0, 0, 15, 15], fill=hx("#e8e4dc")); d.rectangle([0, 0, 15, 15], outline=hx("#c8c0b0"))
def pedestal_top(d, im):
    d.rectangle([0, 0, 15, 15], fill=hx("#f4f0e8")); d.rectangle([1, 1, 14, 14], outline=hx("#d8d0c0"))
    d.rectangle([2, 2, 13, 13], outline=hx("#e6b836"))
tex("pedestal", pedestal, 16); tex("pedestal_top", pedestal_top, 16)

def face(t, uv=(0, 0, 16, 16)): return {"texture": "#" + t, "uv": list(uv)}
def box(fr, to, n, s=None, e=None, w=None, u=None, dn=None):
    s = s or n; e = e or n; w = w or n; u = u or n; dn = dn or n
    return {"from": fr, "to": to, "faces": {"north": face(n), "south": face(s), "east": face(e), "west": face(w), "up": face(u), "down": face(dn)}}


# ================= ガチャガチャ本体（縦2マス・4種類） =================
import random
KINDS = {  # id: (名前, 英名, 本体の色, 濃い色, 明るい色, ラベル)
    "equip": ("装備ガチャ", "Gear Gacha", "#c03030", "#701818", "#f07070", "GEAR"),
    "material": ("素材ガチャ", "Material Gacha", "#3a9a4a", "#1a5a28", "#80d090", "ITEM"),
    "figure": ("フィギュアガチャ", "Figure Gacha", "#d04070", "#8a2048", "#f080a0", "FIG"),
    "title": ("肩書ガチャ", "Title Gacha", "#d0a020", "#806010", "#f0d070", "NAME"),
}
for kid, (jn, en, body, dark, light, label) in KINDS.items():
    B, BD, BL = hx(body), hx(dark), hx(light)
    def lower(d, im):
        d.rectangle([0, 0, 31, 31], fill=B); d.rectangle([0, 0, 31, 1], fill=BL); d.rectangle([0, 30, 31, 31], fill=BD)
        d.rectangle([4, 3, 27, 9], fill=WHITE); d.text((6, 1), label, fill=BD)
        d.ellipse([9, 11, 22, 24], fill=hx("#e0e0e8")); d.ellipse([11, 13, 20, 22], fill=hx("#b8b8c8")); d.rectangle([10, 17, 21, 18], fill=hx("#404050"))
        d.rectangle([24, 12, 28, 14], fill=hx("#f0d040"))
        d.rectangle([9, 25, 22, 30], fill=hx("#301018")); d.rectangle([10, 26, 21, 29], fill=hx("#180810"))
    def side(d, im):
        d.rectangle([0, 0, 31, 31], fill=B); d.rectangle([0, 0, 31, 1], fill=BL); d.rectangle([0, 30, 31, 31], fill=BD)
        for y in (8, 16, 24): d.line([2, y, 29, y], fill=BD)
    def glass(d, im):
        d.rectangle([0, 0, 31, 31], fill=(170, 210, 255, 80))
        rnd = random.Random(hash(kid) % 999)
        for i in range(16):
            x, y = rnd.randint(1, 26), rnd.randint(10, 27); c = hx(CAPS[rnd.randrange(len(CAPS))])
            d.ellipse([x, y, x + 5, y + 5], fill=hx("#f4f4f4")); d.chord([x, y, x + 5, y + 5], 180, 360, fill=c)
        d.line([4, 4, 9, 2], fill=(255, 255, 255, 200)); d.line([4, 7, 6, 6], fill=(255, 255, 255, 160))
    def cap(d, im):
        d.rectangle([0, 0, 31, 31], fill=B); d.ellipse([9, 9, 22, 22], fill=BL); d.rectangle([0, 0, 31, 31], outline=BD, width=2)
    tex(f"{kid}_lower", lower); tex(f"{kid}_side", side); tex(f"{kid}_glass", glass); tex(f"{kid}_cap", cap)
    t = {"front": f"jsn_craft:block/gacha/{kid}_lower", "side": f"jsn_craft:block/gacha/{kid}_side", "glass": f"jsn_craft:block/gacha/{kid}_glass",
         "cap": f"jsn_craft:block/gacha/{kid}_cap", "particle": f"jsn_craft:block/gacha/{kid}_side"}
    b = f"gacha_{kid}"
    wj(f"{A}/models/block/{b}_lower.json", {"parent": "minecraft:block/block", "textures": t,
        "elements": [box([2, 0, 2], [14, 16, 14], "front", s="side", e="side", w="side", u="cap", dn="side"),
                     box([1, 0, 1], [15, 2, 15], "side", u="cap")]})
    wj(f"{A}/models/block/{b}_upper.json", {"parent": "minecraft:block/block", "render_type": "minecraft:translucent", "textures": t,
        "elements": [box([2.5, 0, 2.5], [13.5, 12, 13.5], "glass"), box([3.5, 12, 3.5], [12.5, 14, 12.5], "cap"), box([6, 14, 6], [10, 15, 10], "cap")]})
    wj(f"{A}/blockstates/{b}.json", {"variants": {f"facing={f},half={h}": ({"model": f"jsn_craft:block/{b}_{h}"} | ({"y": y} if y else {}))
                                                   for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)) for h in ("lower", "upper")}})
    # アイテムの見た目：2マスを縮めて1つに
    wj(f"{A}/models/item/{b}.json", {"parent": "minecraft:block/block", "textures": t, "elements": [
        box([4, 0, 4], [12, 8, 12], "front", s="side", e="side", w="side", u="cap", dn="side"),
        box([4.5, 8, 4.5], [11.5, 14, 11.5], "glass"), box([5.5, 14, 5.5], [10.5, 15, 10.5], "cap")]})
    wj(f"{D}/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"jsn_craft:{b}",
        "conditions": [{"condition": "minecraft:block_state_property", "block": f"jsn_craft:{b}", "properties": {"half": "lower"}}]}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    dye = {"equip": "red_dye", "material": "green_dye", "figure": "pink_dye", "title": "yellow_dye"}[kid]
    wj(f"{D}/recipe/{b}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["GGG", "GPG", "ICI"],
        "key": {"G": {"item": "minecraft:glass"}, "P": {"item": f"minecraft:{dye}"}, "C": {"item": "minecraft:chest"}, "I": {"item": "minecraft:iron_ingot"}},
        "result": {"id": f"jsn_craft:{b}", "count": 1}})
# 前の1マスの台は消す
for old in (f"{A}/models/block/gacha_machine.json", f"{A}/blockstates/gacha_machine.json", f"{A}/models/item/gacha_machine.json",
            f"{D}/loot_table/blocks/gacha_machine.json", f"{D}/recipe/gacha_machine.json", f"{D}/gacha/standard.json"):
    if os.path.exists(old): os.remove(old)
for f in ("body_front", "body_side", "glass", "cap_top"):
    q = f"{A}/textures/block/gacha/{f}.png"
    if os.path.exists(q): os.remove(q)
# フィギュアの台
fm = {"parent": "minecraft:block/block", "textures": {"side": "jsn_craft:block/gacha/pedestal", "top": "jsn_craft:block/gacha/pedestal_top", "particle": "jsn_craft:block/gacha/pedestal"},
      "elements": [box([3, 0, 3], [13, 2, 13], "side", u="top")]}
wj(f"{A}/models/block/figure.json", fm)
wj(f"{A}/blockstates/figure.json", {"variants": {f"rotation={i}": {"model": "jsn_craft:block/figure"} for i in range(16)}})
wj(f"{A}/models/item/figure.json", {"parent": "minecraft:builtin/entity", "gui_light": "front", "display": {
    "gui": {"rotation": [20, 210, 0], "translation": [0, 1.5, 0], "scale": [0.9, 0.9, 0.9]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.4, 0.4, 0.4]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]},
    "thirdperson_righthand": {"rotation": [75, 315, 0], "translation": [0, 2.5, 0], "scale": [0.4, 0.4, 0.4]},
    "firstperson_righthand": {"rotation": [0, 315, 0], "translation": [0, 2, 0], "scale": [0.45, 0.45, 0.45]},
    "firstperson_lefthand": {"rotation": [0, 315, 0], "translation": [0, 2, 0], "scale": [0.45, 0.45, 0.45]}}})
wj(f"{D}/loot_table/blocks/figure.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "jsn_craft:figure",
    "functions": [{"function": "minecraft:copy_components", "source": "block_entity", "include": ["minecraft:custom_data"]}]}]}]})

# 肩書チケット（16×16）
im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
d.rectangle([1, 4, 14, 12], fill=hx("#f6e8b0")); d.rectangle([1, 4, 14, 12], outline=hx("#a07820"))
d.rectangle([3, 6, 12, 10], outline=hx("#e6b836")); d.line([5, 8, 10, 8], fill=hx("#c03040"))
for y in (5, 7, 9, 11): d.point((1, y), fill=(0, 0, 0, 0)); d.point((14, y), fill=(0, 0, 0, 0))
d.point((12, 5), fill=hx("#ffffff"))
save(im, f"{A}/textures/item/title_ticket.png")
wj(f"{A}/models/item/title_ticket.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "jsn_craft:item/title_ticket"}})

NAMES = {"ja_jp": {"block.jsn_craft.figure": "フィギュア", "item.jsn_craft.figure": "フィギュア",
                   "item.jsn_craft.title_ticket": "肩書チケット"},
         "en_us": {"block.jsn_craft.figure": "Figure", "item.jsn_craft.figure": "Figure",
                   "item.jsn_craft.title_ticket": "Title Ticket"}}
for kid, v in KINDS.items():
    NAMES["ja_jp"][f"block.jsn_craft.gacha_{kid}"] = v[0]; NAMES["en_us"][f"block.jsn_craft.gacha_{kid}"] = v[1]
for lang, add in NAMES.items():
    p = f"{A}/lang/{lang}.json"; dd = json.load(open(p, encoding="utf-8")); dd.pop("block.jsn_craft.gacha_machine", None); wj(p, dd)
for lang, add in NAMES.items():
    p = f"{A}/lang/{lang}.json"; dd = json.load(open(p, encoding="utf-8")); dd.update(add); wj(p, dd)
p = f"{R}/data/minecraft/tags/block/mineable/pickaxe.json"; dd = json.load(open(p, encoding="utf-8"))
if "jsn_craft:gacha_machine" in dd["values"]: dd["values"].remove("jsn_craft:gacha_machine")
for b in ["jsn_craft:figure"] + [f"jsn_craft:gacha_{k}" for k in KINDS]:
    if b not in dd["values"]: dd["values"].append(b)
wj(p, dd)


# ================= 中身（4台ぶん） =================
def I(id, n=1, w=1): return {"item": id if ":" in id else "jsn_craft:" + id, "count": n, "weight": w}
def F(id, w=1): return {"figure": id if ":" in id else "minecraft:" + id, "weight": w}
def T(t, w=1): return {"title": t, "weight": w}
WEAP = ["job_axe", "job_sword", "katana", "tower_shield", "spear", "sea_trident", "dagger", "whip", "gauntlets", "job_bow", "staff", "holy_staff", "grimoire"]
MATS = {1: ["stone", "wood", "leather", "old"], 2: ["iron", "bone", "silver", "mandrake", "sea"], 3: ["diamond", "blaze", "prismarine", "obsidian", "crystal", "orb", "spirit"],
        4: ["netherite"], 5: ["moon_silver"], 6: ["star_silver"]}
def weapon(tier): return [I(f"{m}_{w}") for w in WEAP for m in MATS[tier]]
STYLES = ["mage", "warrior", "axeman", "archer", "lancer", "shield_knight", "beast", "brawler", "ninja", "cleric", "assassin", "gambler", "rogue", "samurai", "monk", "bard"]
def armor(tier): return [I(f"{s}_{tier}_{p}") for s in STYLES for p in ("head", "chest", "legs", "feet")]
GEMS = ["garnet", "peridot", "aquamarine", "onyx", "ruby", "sapphire", "topaz", "crystal", "moonstone", "opal"]
JEW = ["ring", "necklace", "bracelet", "earring", "charm"]
def jew(*mats): return [I(f"{m}_{j}", 1, 2) for m in mats for j in JEW]
# 装備：武器・服・装飾品
equip = [weapon(1) + armor(1),
         weapon(2) + armor(2) + jew("copper"),
         weapon(3) + armor(3) + jew("iron", "gold"),
         weapon(4) + armor(4) + jew("diamond") + [I(x, 1, 3) for x in ("wanderer_ring", "harvest_charm", "night_earring", "sea_bracelet", "echo_earring")],
         weapon(5) + weapon(6) + armor(5) + armor(6) + jew("netherite", "moon_silver", "star_silver") + [I(x, 1, 4) for x in ("phoenix_charm", "dragon_necklace", "sovereign_ring")]]
# 素材：強化石・薬・宝石・食べもの・バニラ
material = [
    [I("minecraft:bread", 8), I("minecraft:cooked_beef", 6), I("minecraft:coal", 12), I("minecraft:iron_ingot", 4), I("minecraft:arrow", 16), I("minecraft:redstone", 12),
     I("minecraft:lapis_lazuli", 8), I("minecraft:torch", 16), I("minecraft:copper_ingot", 8), I("minecraft:string", 8), I("minecraft:leather", 6), I("minecraft:paper", 12),
     I("herb", 4), I("vigor_herb", 3), I("moon_herb", 3), I("antidote_herb", 3), I("fire_herb", 3), I("elixir_heal", 2), I("elixir_stamina", 2), I("elixir_mana", 2),
     I("enhance_stone_iron", 2), I("cooked_rice", 4), I("salt", 4), I("tea", 3), I("flour", 4), I("butter", 2), I("cheese", 2), I("miso", 2)] + [I("raw_" + g, 2) for g in GEMS],
    [I("minecraft:gold_ingot", 6), I("minecraft:emerald", 3), I("minecraft:diamond", 1), I("minecraft:ender_pearl", 4), I("minecraft:golden_apple", 2),
     I("minecraft:name_tag", 1), I("minecraft:experience_bottle", 8), I("minecraft:glow_berries", 12), I("minecraft:honey_bottle", 4), I("minecraft:amethyst_shard", 8),
     I("enhance_stone_gold", 2), I("elixir_swift"), I("elixir_might"), I("elixir_armor"), I("elixir_luck"), I("elixir_great_heal", 2), I("elixir_cure", 2),
     I("elixir_miner"), I("elixir_angler"), I("silver_ingot", 3), I("truffle", 2), I("golden_egg", 1), I("premium_beef", 3)] + [I(g, 1) for g in GEMS],
    [I("minecraft:diamond", 3), I("minecraft:emerald_block", 1), I("minecraft:ender_eye", 2), I("minecraft:experience_bottle", 24), I("minecraft:blaze_rod", 6),
     I("enhance_stone_diamond", 2), I("rune_book", 1), I("elixir_crit"), I("elixir_arcane"), I("elixir_training"), I("elixir_focus"), I("elixir_runner"),
     I("moon_silver_ingot", 2), I("holy_crystal", 2), I("spirit_stone", 2), I("mandrake", 2), I("job_template_2", 1)],
    [I("minecraft:netherite_ingot", 1), I("minecraft:totem_of_undying", 1), I("minecraft:enchanted_golden_apple", 1), I("minecraft:diamond_block", 1),
     I("minecraft:heart_of_the_sea", 1), I("enhance_stone_netherite", 2), I("elixir_elixir"), I("star_silver_ingot", 1), I("job_template_3", 1), I("moon_silver_template", 1),
     I("oblivion_orb", 1)],
    [I("minecraft:nether_star", 1), I("minecraft:elytra", 1), I("minecraft:netherite_block", 1), I("minecraft:dragon_egg", 1, 1), I("enhance_stone_star", 3),
     I("elixir_ancient", 1), I("star_silver_template", 1), I("elixir_recall", 3)]]
# フィギュア：マイクラのモンスター全部と、jsn-Craft の相棒
fig = [
    [F(m) for m in ("chicken", "pig", "cow", "sheep", "rabbit", "cod", "salmon", "tropical_fish", "pufferfish", "bat", "squid", "tadpole", "silverfish",
                    "endermite", "zombie", "skeleton", "spider", "cave_spider", "slime", "creeper")],
    [F(m) for m in ("husk", "stray", "drowned", "bogged", "zombie_villager", "frog", "bee", "fox", "wolf", "cat", "ocelot", "parrot", "turtle", "donkey",
                    "mule", "horse", "llama", "trader_llama", "goat", "dolphin", "magma_cube", "phantom", "witch", "pillager")],
    [F(m) for m in ("axolotl", "glow_squid", "panda", "polar_bear", "camel", "armadillo", "sniffer", "allay", "strider", "enderman", "blaze", "ghast",
                    "guardian", "villager", "wandering_trader", "iron_golem", "snow_golem", "vindicator", "piglin", "zombified_piglin")] +
    [F("jsn_craft:" + m) for m in ("pet_wolf", "pet_bee", "pet_hawk")],
    [F(m) for m in ("mooshroom", "skeleton_horse", "zombie_horse", "hoglin", "zoglin", "piglin_brute", "evoker", "vex", "ravager", "shulker", "breeze",
                    "wither_skeleton", "elder_guardian")] + [F("jsn_craft:" + m) for m in ("pet_golem", "pet_white_fox", "pet_wind_cat")],
    [F(m, 2) for m in ("warden", "ender_dragon", "wither", "illusioner")] + [F("jsn_craft:" + m, 2) for m in ("pet_thunder_lion", "pet_blade_dragon")]]
# 肩書
titles = [
    [T(t) for t in ("見習い冒険者", "のんびり屋", "ガチャ好き", "おにぎり好き", "朝寝坊", "迷子の達人", "草むしり名人", "日曜大工", "ひなたぼっこ", "雑草魂", "通りすがり", "はらぺこ")],
    [T(t) for t in ("カプセル収集家", "夜ふかし勇者", "カジノの常連", "鉱山のぬし", "釣りバカ", "料理研究家", "動物なかよし", "旅する商人", "宝石ハンター", "村の人気者")],
    [T(t) for t in ("幸運の持ち主", "ガチャの申し子", "フィギュア職人", "百戦錬磨", "ドラゴン見習い", "星読み", "伝説の料理人", "ダンジョン帰り")],
    [T(t) for t in ("ウルトラ級の運", "伝説のコレクター", "大当たり職人", "ガチャ王", "黄金の手", "世界を旅した者")],
    [T(t) for t in ("ガチャの神", "天に選ばれし者", "レジェンド", "千年に一人")]]
POOLS = {"equip": (equip, 1000), "material": (material, 10), "figure": (fig, 100), "title": (titles, 20)}
for kid, (tiers, price) in POOLS.items():
    wj(f"{D}/gacha/{kid}.json", {"_comment": f"{KINDS[kid][0]}の中身。rates はノーマル〜レジェンドの確率(%)、pity は天井、price は1回の値段(チップ枚数＝鉄コイン。10=金コイン1枚、100=エメラルドコイン1枚、1000=ダイヤコイン1枚)",
                                 "rates": [60, 28, 9, 2.5, 0.5], "pity": 100, "price": price, "tiers": tiers})
    print(kid, [len(t) for t in tiers])
