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

def body_front(d, im):
    d.rectangle([0, 0, 31, 31], fill=PINK); d.rectangle([0, 0, 31, 1], fill=PINK_L)
    d.rectangle([5, 2, 26, 7], fill=WHITE); d.text((7, 1), "GACHA", fill=hx("#d02040"))
    d.ellipse([9, 9, 22, 22], fill=hx("#e0e0e8")); d.ellipse([11, 11, 20, 20], fill=hx("#b8b8c8")); d.rectangle([10, 15, 21, 16], fill=hx("#404050"))
    d.rectangle([10, 24, 21, 31], fill=hx("#301018")); d.rectangle([11, 25, 20, 30], fill=hx("#180810"))
    d.rectangle([24, 10, 28, 12], fill=hx("#f0d040"))    # コイン入れ
def body_side(d, im):
    d.rectangle([0, 0, 31, 31], fill=PINK); d.rectangle([0, 0, 31, 1], fill=PINK_L); d.rectangle([0, 30, 31, 31], fill=PINK_D)
    for y in (8, 16, 24): d.line([2, y, 29, y], fill=hx("#c03868"))
def glass(d, im):
    d.rectangle([0, 0, 31, 31], fill=(170, 210, 255, 90))
    import random
    rnd = random.Random(3)
    for i in range(14):
        x, y = rnd.randint(2, 26), rnd.randint(8, 27); c = hx(CAPS[i % len(CAPS)])
        d.ellipse([x, y, x + 5, y + 5], fill=hx("#f4f4f4")); d.chord([x, y, x + 5, y + 5], 180, 360, fill=c)
    d.line([4, 4, 9, 2], fill=(255, 255, 255, 200)); d.line([4, 6, 6, 5], fill=(255, 255, 255, 160))
def cap_top(d, im):
    d.rectangle([0, 0, 31, 31], fill=hx("#e04060")); d.ellipse([10, 10, 21, 21], fill=hx("#f080a0"))
tex("body_front", body_front); tex("body_side", body_side); tex("glass", glass); tex("cap_top", cap_top)

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

gm = {"parent": "minecraft:block/block", "render_type": "minecraft:translucent",
      "textures": {"front": "jsn_craft:block/gacha/body_front", "side": "jsn_craft:block/gacha/body_side", "glass": "jsn_craft:block/gacha/glass",
                   "cap": "jsn_craft:block/gacha/cap_top", "particle": "jsn_craft:block/gacha/body_side"},
      "elements": [None,
                   box([3, 8, 3], [13, 15, 13], "glass"),
                   box([4, 15, 4], [12, 16, 12], "cap")]}
gm["elements"][0] = box([2, 0, 2], [14, 8, 14], "front", s="side", e="side", w="side", u="side", dn="side")
wj(f"{A}/models/block/gacha_machine.json", gm)
wj(f"{A}/blockstates/gacha_machine.json", {"variants": {f"facing={f}": ({"model": "jsn_craft:block/gacha_machine"} | ({"y": y} if y else {}))
                                                        for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
wj(f"{A}/models/item/gacha_machine.json", {"parent": "jsn_craft:block/gacha_machine"})
wj(f"{D}/loot_table/blocks/gacha_machine.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "jsn_craft:gacha_machine"}],
                                                                                     "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
wj(f"{D}/recipe/gacha_machine.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["GGG", "PCP", "III"],
    "key": {"G": {"item": "minecraft:glass"}, "P": {"item": "minecraft:pink_dye"}, "C": {"item": "minecraft:chest"}, "I": {"item": "minecraft:iron_ingot"}},
    "result": {"id": "jsn_craft:gacha_machine", "count": 1}})

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

NAMES = {"ja_jp": {"block.jsn_craft.gacha_machine": "ガチャガチャ", "block.jsn_craft.figure": "フィギュア", "item.jsn_craft.figure": "フィギュア",
                   "item.jsn_craft.title_ticket": "肩書チケット"},
         "en_us": {"block.jsn_craft.gacha_machine": "Capsule Toy Machine", "block.jsn_craft.figure": "Figure", "item.jsn_craft.figure": "Figure",
                   "item.jsn_craft.title_ticket": "Title Ticket"}}
for lang, add in NAMES.items():
    p = f"{A}/lang/{lang}.json"; dd = json.load(open(p, encoding="utf-8")); dd.update(add); wj(p, dd)
p = f"{R}/data/minecraft/tags/block/mineable/pickaxe.json"; dd = json.load(open(p, encoding="utf-8"))
for b in ("jsn_craft:gacha_machine", "jsn_craft:figure"):
    if b not in dd["values"]: dd["values"].append(b)
wj(p, dd)

# ================= 中身 =================
def I(id, n=1, w=1): return {"item": id if ":" in id else "jsn_craft:" + id, "count": n, "weight": w}
def F(id, w=1): return {"figure": id if ":" in id else "minecraft:" + id, "weight": w}
def T(t, w=1): return {"title": t, "weight": w}
WEAP = ["job_axe", "job_sword", "katana", "tower_shield", "spear", "dagger", "whip", "gauntlets", "job_bow", "staff", "holy_staff", "grimoire"]
def weapon(tier):
    # 職業武器：段階ごとの名前（ない組み合わせは自動でとばされる）
    mats = {1: ["stone", "wood", "leather", "old"], 2: ["iron", "bone", "silver", "crystal", "mandrake"], 3: ["diamond", "blaze", "prismarine", "obsidian", "orb", "spirit"],
            4: ["netherite"], 5: ["moon_silver"], 6: ["star_silver"]}[tier]
    out = []
    for w in WEAP:
        for m in mats: out.append(I(f"{m}_{w}"))
    return out
STYLES = ["mage", "warrior", "axeman", "archer", "lancer", "shield_knight", "beast", "brawler", "ninja", "cleric", "assassin", "gambler"]
def armor(tier):
    return [I(f"{s}_{tier}_{p}") for s in STYLES for p in ("head", "chest", "legs", "feet")]
GEMS = ["garnet", "peridot", "aquamarine", "onyx", "ruby", "sapphire", "topaz", "crystal", "moonstone", "opal"]
JEW = ["ring", "necklace", "bracelet", "earring", "charm"]

N = [I("minecraft:bread", 8, 4), I("minecraft:cooked_beef", 6, 4), I("minecraft:coal", 12, 4), I("minecraft:iron_ingot", 4, 4), I("minecraft:arrow", 16, 3),
     I("minecraft:experience_bottle", 4, 3), I("minecraft:redstone", 12, 3), I("minecraft:lapis_lazuli", 8, 3), I("minecraft:torch", 16, 3), I("minecraft:cake", 1, 2),
     I("herb", 4, 3), I("vigor_herb", 3, 2), I("moon_herb", 3, 2), I("elixir_heal", 2, 3), I("elixir_stamina", 2, 2), I("elixir_mana", 2, 2),
     I("enhance_stone_iron", 2, 3), I("cooked_rice", 4, 2), I("salt", 4, 1), I("tea", 3, 1)] + \
    [I("raw_" + g, 2) for g in GEMS] + [dict(x, weight=1) for x in weapon(1)] + armor(1) + \
    [F(m, 2) for m in ("chicken", "pig", "cow", "sheep", "rabbit", "cod", "bat", "squid")]
R_ = [I("minecraft:gold_ingot", 6, 3), I("minecraft:emerald", 3, 3), I("minecraft:diamond", 1, 3), I("minecraft:ender_pearl", 4, 2), I("minecraft:golden_apple", 2, 3),
      I("minecraft:name_tag", 1, 2), I("minecraft:saddle", 1, 1), I("minecraft:glow_berries", 12, 1),
      I("enhance_stone_gold", 2, 3), I("elixir_swift", 1, 2), I("elixir_might", 1, 2), I("elixir_armor", 1, 2), I("elixir_luck", 1, 2), I("elixir_great_heal", 2, 2)] + \
     [I(g, 1) for g in GEMS] + [I(f"{m}_{j}") for m in ("copper", "iron") for j in JEW] + weapon(2) + armor(2) + \
     [F(m, 2) for m in ("zombie", "skeleton", "creeper", "spider", "slime", "drowned", "husk", "fox", "wolf", "cat", "parrot", "turtle")] + \
     [T(t) for t in ("見習い冒険者", "ガチャ好き", "カプセル収集家", "のんびり屋")]
SR = [I("minecraft:diamond", 3, 3), I("minecraft:emerald_block", 1, 1), I("minecraft:ender_eye", 2, 2), I("minecraft:experience_bottle", 16, 2),
      I("enhance_stone_diamond", 2, 3), I("rune_book", 1, 2), I("elixir_crit", 1, 2), I("elixir_arcane", 1, 2), I("elixir_training", 1, 2),
      I("moon_silver_ingot", 2, 2)] + [I(f"{m}_{j}") for m in ("gold", "diamond") for j in JEW] + weapon(3) + armor(3) + armor(4) + \
     [F(m, 2) for m in ("enderman", "blaze", "witch", "villager", "iron_golem", "axolotl", "panda", "piglin", "ghast", "guardian", "bee", "frog", "allay", "sniffer")] + \
     [T(t) for t in ("幸運の持ち主", "ガチャの申し子", "フィギュア職人", "夜ふかし勇者", "カジノの常連")]
UR = [I("minecraft:netherite_ingot", 1, 3), I("minecraft:totem_of_undying", 1, 2), I("minecraft:enchanted_golden_apple", 1, 2), I("minecraft:diamond_block", 1, 2),
      I("enhance_stone_netherite", 2, 3), I("elixir_elixir", 1, 2), I("star_silver_ingot", 1, 2)] + \
     [I(f"{m}_{j}") for m in ("netherite", "moon_silver") for j in JEW] + \
     [I(x) for x in ("wanderer_ring", "harvest_charm", "night_earring", "sea_bracelet", "echo_earring")] + weapon(4) + weapon(5) + armor(5) + \
     [F(m, 2) for m in ("wither_skeleton", "elder_guardian", "evoker", "ravager", "shulker", "breeze", "mooshroom", "strider")] + \
     [T(t, 2) for t in ("ウルトラ級の運", "伝説のコレクター", "大当たり職人", "ガチャ王")]
LR = [I("minecraft:nether_star", 1, 2), I("minecraft:elytra", 1, 2), I("minecraft:netherite_block", 1, 1), I("enhance_stone_star", 3, 3), I("elixir_ancient", 1, 2),
      I("phoenix_charm", 1, 2), I("dragon_necklace", 1, 2), I("sovereign_ring", 1, 2)] + \
     [I(f"star_silver_{j}") for j in JEW] + weapon(6) + armor(6) + \
     [F(m, 3) for m in ("warden", "ender_dragon", "wither")] + [T(t, 3) for t in ("ガチャの神", "天に選ばれし者", "レジェンド")]
wj(f"{D}/gacha/standard.json", {"_comment": "ガチャガチャの中身。rates はノーマル〜レジェンドの確率(%)、pity は天井、price は1回の値段(チップ枚数＝鉄コイン)",
                                "rates": [60, 28, 9, 2.5, 0.5], "pity": 100, "price": 10, "tiers": [N, R_, SR, UR, LR]})
print("ok", [len(t) for t in (N, R_, SR, UR, LR)])
