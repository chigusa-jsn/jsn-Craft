"""カジノの絵（チップ・両替機・スロット台）とブロックのモデル・レシピなどを書き出す。"""
import json, os
from PIL import Image, ImageDraw

R = os.path.join(os.path.dirname(__file__), "..", "src/main/resources")
A = f"{R}/assets/jsn_craft"
D = f"{R}/data/jsn_craft"

def hx(s): s = s.lstrip("#"); return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)
def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True); img.save(path)
def wj(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f: json.dump(obj, f, ensure_ascii=False, indent=2); f.write("\n")

# ---------------- チップ（16×16） ----------------
CHIP = {1: ("#f2f2f2", "#3a6ad8"), 10: ("#d83030", "#ffffff"), 100: ("#2a4ad0", "#ffffff"), 1000: ("#1a1a1a", "#e6b836")}
for v, (base, stripe) in CHIP.items():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    d.ellipse([1, 2, 14, 14], fill=hx("#1a1018"))                     # ふち（影）
    d.ellipse([1, 1, 14, 13], fill=hx(base))
    # まわりの模様（6か所）
    for (x, y) in ((7, 1), (7, 12), (1, 6), (13, 6), (3, 2), (11, 11)):
        d.rectangle([x, y, x + 1, y], fill=hx(stripe))
    d.ellipse([4, 4, 11, 10], fill=hx(stripe))
    d.ellipse([5, 5, 10, 9], fill=hx(base))
    if v == 1000:
        d.point([(7, 6), (8, 7), (7, 8), (6, 7)], fill=hx("#e6b836"))
    else:
        d.point([(7, 7), (8, 7)], fill=hx(stripe))
    d.point([(4, 3), (3, 4)], fill=(255, 255, 255, 200))
    save(im, f"{A}/textures/item/casino_chip_{v}.png")
    wj(f"{A}/models/item/casino_chip_{v}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"jsn_craft:item/casino_chip_{v}"}})

# ---------------- ブロックの絵（32×32） ----------------
def tex(name, draw):
    im = Image.new("RGBA", (32, 32), (0, 0, 0, 0)); draw(ImageDraw.Draw(im), im); save(im, f"{A}/textures/block/casino/{name}.png")

RED, RED_D, GOLD, GOLD_D, CREAM = hx("#b01c1c"), hx("#6a0e0e"), hx("#f0c040"), hx("#9a7018"), hx("#f6eedc")

def side(d, im):
    d.rectangle([0, 0, 31, 31], fill=RED)
    for y in range(0, 32, 4): d.line([0, y, 31, y], fill=hx("#a01818"))
    d.rectangle([0, 0, 31, 31], outline=GOLD, width=2)
    d.rectangle([2, 2, 29, 29], outline=GOLD_D)
tex("side", side)

def top(d, im):
    d.rectangle([0, 0, 31, 31], fill=RED_D); d.rectangle([0, 0, 31, 31], outline=GOLD, width=2)
tex("top", top)

def slot_front(d, im):
    side(d, im)
    d.rectangle([3, 3, 28, 18], fill=hx("#2a1a0a"))              # リールの窓
    for i, c in enumerate(("#d82020", "#2ad0e0", "#40d040")):
        x = 5 + i * 8
        d.rectangle([x, 5, x + 5, 16], fill=CREAM)
        d.rectangle([x + 1, 9, x + 4, 12], fill=hx(c))
    d.line([3, 10, 28, 10], fill=hx("#ff3030"))
    d.rectangle([6, 21, 25, 23], fill=GOLD_D); d.rectangle([7, 21, 24, 22], fill=GOLD)   # ボタンの台
    for i, c in enumerate(("#ff4040", "#40ff40", "#4080ff")): d.rectangle([9 + i * 5, 21, 11 + i * 5, 22], fill=hx(c))
    d.rectangle([9, 26, 22, 29], fill=hx("#1a1018")); d.rectangle([10, 27, 21, 28], fill=hx("#3a3040"))   # 受け皿
tex("slot_front", slot_front)

def slot_sign(d, im):
    d.rectangle([0, 0, 31, 31], fill=hx("#1a1018"))
    d.rectangle([0, 0, 31, 31], outline=GOLD, width=2)
    for x in range(3, 30, 4): d.point([(x, 3), (x + 1, 28)], fill=hx("#fff4a0"))
    # 7 7 7
    for i in range(3):
        x = 5 + i * 8
        d.line([x, 10, x + 5, 10], fill=hx("#ff3030"), width=2); d.line([x + 5, 10, x + 2, 21], fill=hx("#ff3030"), width=2)
tex("slot_sign", slot_sign)

def lever(d, im):
    d.rectangle([0, 0, 31, 31], fill=hx("#c8ccd6")); d.line([0, 0, 0, 31], fill=hx("#ffffff"))
tex("lever", lever)
def knob(d, im):
    d.rectangle([0, 0, 31, 31], fill=hx("#e82020")); d.rectangle([4, 4, 12, 12], fill=hx("#ff8080"))
tex("knob", knob)

def ex_front(d, im):
    side(d, im)
    d.rectangle([5, 4, 26, 11], fill=hx("#0a2a14")); d.rectangle([5, 4, 26, 11], outline=GOLD_D)   # 表示
    for x0 in (7, 13, 19): d.rectangle([x0, 6, x0 + 4, 9], fill=hx("#60ff80"))   # 数字の光
    d.rectangle([14, 14, 17, 18], fill=hx("#1a1018"))       # コインの口
    d.rectangle([5, 21, 26, 27], fill=hx("#1a1018")); d.rectangle([6, 22, 25, 26], fill=hx("#2a2030"))   # チップの受け口
    for i, c in enumerate(("#f2f2f2", "#d83030", "#2a4ad0", "#1a1a1a")): d.ellipse([7 + i * 5, 23, 10 + i * 5, 25], fill=hx(c))
tex("exchanger_front", ex_front)

def ex_top(d, im):
    top(d, im); d.rectangle([8, 8, 23, 23], fill=GOLD_D); d.ellipse([10, 10, 21, 21], fill=GOLD)
tex("exchanger_top", ex_top)

# ---------------- モデル ----------------
def face(t, uv=(0, 0, 16, 16)): return {"texture": "#" + t, "uv": list(uv)}
def box(fr, to, n="side", s="side", e="side", w="side", u="top", dn="top", rot=None):
    el = {"from": fr, "to": to, "faces": {"north": face(n), "south": face(s), "east": face(e), "west": face(w), "up": face(u), "down": face(dn)}}
    if rot: el["rotation"] = rot
    return el

T = {"side": "jsn_craft:block/casino/side", "top": "jsn_craft:block/casino/top", "particle": "jsn_craft:block/casino/side"}
slot = {"parent": "minecraft:block/block", "textures": dict(T, front="jsn_craft:block/casino/slot_front", sign="jsn_craft:block/casino/slot_sign",
        lever="jsn_craft:block/casino/lever", knob="jsn_craft:block/casino/knob"),
        "elements": [box([1, 0, 2], [15, 13, 15], n="front"),
                     box([2, 13, 4], [14, 16, 13], n="sign", s="sign"),
                     box([15, 6, 8], [16, 13, 9], n="lever", s="lever", e="lever", w="lever", u="lever", dn="lever"),
                     box([14.5, 12.5, 7.5], [16.5, 14.5, 9.5], n="knob", s="knob", e="knob", w="knob", u="knob", dn="knob")]}
wj(f"{A}/models/block/slot_machine.json", slot)
ex = {"parent": "minecraft:block/block", "textures": dict(T, front="jsn_craft:block/casino/exchanger_front", top2="jsn_craft:block/casino/exchanger_top"),
      "elements": [box([1, 0, 3], [15, 16, 15], n="front", u="top2"),
                   box([1, 6, 1], [15, 7, 3])]}
wj(f"{A}/models/block/chip_exchanger.json", ex)
for b in ("slot_machine", "chip_exchanger"):
    wj(f"{A}/blockstates/{b}.json", {"variants": {f"facing={f}": ({"model": f"jsn_craft:block/{b}"} | ({"y": y} if y else {}))
                                                  for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    wj(f"{A}/models/item/{b}.json", {"parent": f"jsn_craft:block/{b}"})
    wj(f"{D}/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"jsn_craft:{b}"}],
                                                                                  "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

# ---------------- レシピ ----------------
wj(f"{D}/recipe/slot_machine.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["GGG", "RDR", "III"],
    "key": {"G": {"item": "minecraft:gold_ingot"}, "R": {"item": "minecraft:redstone"}, "D": {"item": "minecraft:diamond"}, "I": {"item": "minecraft:iron_ingot"}},
    "result": {"id": "jsn_craft:slot_machine", "count": 1}})
wj(f"{D}/recipe/chip_exchanger.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["III", "GEG", "III"],
    "key": {"G": {"item": "minecraft:gold_ingot"}, "E": {"item": "minecraft:emerald"}, "I": {"item": "minecraft:iron_ingot"}},
    "result": {"id": "jsn_craft:chip_exchanger", "count": 1}})

# ---------------- 名前 ----------------
NAMES = {"ja_jp": {"block.jsn_craft.slot_machine": "スロットマシン", "block.jsn_craft.chip_exchanger": "両替機",
                   "item.jsn_craft.casino_chip_1": "カジノチップ（1）", "item.jsn_craft.casino_chip_10": "カジノチップ（10）",
                   "item.jsn_craft.casino_chip_100": "カジノチップ（100）", "item.jsn_craft.casino_chip_1000": "カジノチップ（1000）"},
         "en_us": {"block.jsn_craft.slot_machine": "Slot Machine", "block.jsn_craft.chip_exchanger": "Chip Exchanger",
                   "item.jsn_craft.casino_chip_1": "Casino Chip (1)", "item.jsn_craft.casino_chip_10": "Casino Chip (10)",
                   "item.jsn_craft.casino_chip_100": "Casino Chip (100)", "item.jsn_craft.casino_chip_1000": "Casino Chip (1000)"}}
for lang, add in NAMES.items():
    p = f"{A}/lang/{lang}.json"; d = json.load(open(p, encoding="utf-8")); d.update(add); wj(p, d)

# つるはしで壊せる
p = f"{R}/data/minecraft/tags/block/mineable/pickaxe.json"; d = json.load(open(p, encoding="utf-8"))
for b in ("jsn_craft:slot_machine", "jsn_craft:chip_exchanger"):
    if b not in d["values"]: d["values"].append(b)
wj(p, d)
print("ok")

# ================= パチスロ（縦2マス） =================
def pachi(name, body, body_d, trim, accent, sign_draw):
    def side2(d, im):
        d.rectangle([0, 0, 31, 31], fill=hx(body))
        for y in range(0, 32, 4): d.line([0, y, 31, y], fill=hx(body_d))
        d.rectangle([0, 0, 31, 31], outline=hx(trim), width=2)
    tex(f"{name}_side", side2)
    def topt(d, im):
        d.rectangle([0, 0, 31, 31], fill=hx(body_d)); d.rectangle([0, 0, 31, 31], outline=hx(trim), width=2)
    tex(f"{name}_top", topt)
    def lower(d, im):
        side2(d, im)
        d.rectangle([3, 2, 28, 15], fill=hx("#101010"))                       # リールの窓
        syms = [("#d82020", "#2a6ae0", "#d82020"), ("#8a40c0", "#d82020", "#e0c040"), ("#2a6ae0", "#d82020", "#8a40c0")]
        for i in range(3):
            x = 5 + i * 8
            d.rectangle([x, 3, x + 5, 14], fill=CREAM)
            for j, c in enumerate(syms[i]): d.rectangle([x + 1, 4 + j * 4, x + 4, 5 + j * 4], fill=hx(c))
        d.line([3, 8, 28, 8], fill=hx("#ff3030"))
        d.rectangle([2, 17, 29, 21], fill=hx(body_d)); d.rectangle([2, 17, 29, 17], fill=hx(trim))   # ボタンの台
        for i in range(3): d.rectangle([9 + i * 6, 19, 12 + i * 6, 20], fill=hx("#40a0ff"))
        d.rectangle([3, 18, 6, 20], fill=hx("#c8c8c8"))                         # レバー
        d.rectangle([9, 25, 22, 29], fill=hx("#1a1018")); d.rectangle([10, 26, 21, 28], fill=hx("#4a4050"))   # 受け皿
        d.rectangle([24, 23, 28, 26], fill=hx(accent))                          # ランプ
    tex(f"{name}_lower", lower)
    def upper(d, im):
        side2(d, im)
        d.rectangle([3, 3, 28, 28], fill=hx("#101018")); d.rectangle([3, 3, 28, 28], outline=hx(trim))
        sign_draw(d)
    tex(f"{name}_upper", upper)
    T2 = {"side": f"jsn_craft:block/casino/{name}_side", "top": f"jsn_craft:block/casino/{name}_top", "particle": f"jsn_craft:block/casino/{name}_side"}
    lo = {"parent": "minecraft:block/block", "textures": dict(T2, front=f"jsn_craft:block/casino/{name}_lower"),
          "elements": [box([1, 0, 2], [15, 16, 15], n="front"), box([1, 5, 0.5], [15, 7, 2])]}
    up = {"parent": "minecraft:block/block", "textures": dict(T2, front=f"jsn_craft:block/casino/{name}_upper"),
          "elements": [box([1, 0, 3], [15, 13, 15], n="front"), box([3, 13, 5], [13, 16, 13], n="front", s="front")]}
    b = f"pachislot_{name}"
    wj(f"{A}/models/block/{b}_lower.json", lo)
    wj(f"{A}/models/block/{b}_upper.json", up)
    wj(f"{A}/blockstates/{b}.json", {"variants": {f"facing={f},half={h}": ({"model": f"jsn_craft:block/{b}_{h}"} | ({"y": y} if y else {}))
                                                  for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)) for h in ("lower", "upper")}})
    wj(f"{A}/models/item/{b}.json", {"parent": f"jsn_craft:block/{b}_lower"})
    wj(f"{D}/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"jsn_craft:{b}",
        "conditions": [{"condition": "minecraft:block_state_property", "block": f"jsn_craft:{b}", "properties": {"half": "lower"}}]}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    p = f"{R}/data/minecraft/tags/block/mineable/pickaxe.json"; dd = json.load(open(p, encoding="utf-8"))
    if f"jsn_craft:{b}" not in dd["values"]: dd["values"].append(f"jsn_craft:{b}")
    wj(p, dd)

def crown_sign(d):
    g = hx("#f0c040")
    d.polygon([(8, 20), (8, 10), (12, 15), (16, 7), (20, 15), (24, 10), (24, 20)], fill=g)
    d.rectangle([8, 20, 24, 23], fill=hx("#b08020"))
    for x in (11, 16, 21): d.point((x, 21), fill=hx("#ff3050"))
    for x, y in ((5, 5), (26, 6), (6, 25), (25, 25)): d.point((x, y), fill=hx("#ffffa0"))

def tropical_sign(d):
    d.ellipse([18, 5, 27, 14], fill=hx("#ffb020"))                         # 太陽
    d.line([10, 26, 12, 12], fill=hx("#8a5a2a"), width=2)                  # ヤシの木
    for dx, dy in ((-6, -2), (6, -2), (-4, 3), (5, 3)): d.line([12, 12, 12 + dx, 12 + dy], fill=hx("#30c050"), width=2)
    for i, c in enumerate(("#ff3050", "#ff9030", "#ffe040", "#40e070", "#40a0ff")): d.rectangle([5 + i * 5, 27, 8 + i * 5, 28], fill=hx(c))

pachi("crown", "#22348a", "#1a2a6a", "#f0c040", "#ffe040", crown_sign)
pachi("tropical", "#0e7a62", "#0a5a4a", "#ff80b0", "#ff3050", tropical_sign)

wj(f"{D}/recipe/pachislot_crown.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["GCG", "RDR", "III"],
    "key": {"G": {"item": "minecraft:gold_ingot"}, "C": {"item": "minecraft:gold_block"}, "R": {"item": "minecraft:redstone"},
            "D": {"item": "minecraft:diamond"}, "I": {"item": "minecraft:iron_ingot"}}, "result": {"id": "jsn_craft:pachislot_crown", "count": 1}})
wj(f"{D}/recipe/pachislot_tropical.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["GCG", "RDR", "III"],
    "key": {"G": {"item": "minecraft:gold_ingot"}, "C": {"item": "minecraft:jungle_sapling"}, "R": {"item": "minecraft:redstone"},
            "D": {"item": "minecraft:diamond"}, "I": {"item": "minecraft:iron_ingot"}}, "result": {"id": "jsn_craft:pachislot_tropical", "count": 1}})
for lang, add in {"ja_jp": {"block.jsn_craft.pachislot_crown": "パチスロ「ピカッとクラウン」", "block.jsn_craft.pachislot_tropical": "パチスロ「トロピカル・ドキドキ」"},
                  "en_us": {"block.jsn_craft.pachislot_crown": "Pachislot: Pika Crown", "block.jsn_craft.pachislot_tropical": "Pachislot: Tropical Doki-Doki"}}.items():
    p = f"{A}/lang/{lang}.json"; dd = json.load(open(p, encoding="utf-8")); dd.update(add); wj(p, dd)
print("pachi ok")

# ================= テーブル（ルーレット・ブラックジャック） =================
def felt_base(d):
    d.rectangle([0, 0, 31, 31], fill=hx("#0e5a36"))
    for i in range(0, 32, 2):
        for j in range(0, 32, 2):
            if (i * 7 + j * 3) % 5 == 0: d.point((i, j), fill=hx("#0c5030"))
    d.rectangle([0, 0, 31, 31], outline=hx("#5a3a1a"), width=2)

def roulette_top(d, im):
    felt_base(d)
    d.ellipse([3, 3, 28, 28], fill=hx("#5a3a1a")); d.ellipse([5, 5, 26, 26], fill=hx("#2a1a0a"))
    import math as _m
    for i in range(18):
        a = i * 2 * _m.pi / 18; x, y = 15.5 + 9 * _m.cos(a), 15.5 + 9 * _m.sin(a)
        d.rectangle([x - 1, y - 1, x + 1, y + 1], fill=hx("#c01818" if i % 2 else "#181818"))
    d.ellipse([11, 11, 20, 20], fill=hx("#6a4a24")); d.ellipse([14, 14, 17, 17], fill=hx("#e6b836"))
    d.point((24, 9), fill=hx("#ffffff"))
tex("roulette_top", roulette_top)

def bj_top(d, im):
    felt_base(d)
    d.arc([-6, -20, 37, 26], 20, 160, fill=hx("#e6b836"), width=1)
    for x in (6, 13, 20):
        d.rectangle([x, 18, x + 5, 25], fill=hx("#f8f4ea")); d.rectangle([x + 1, 19, x + 2, 20], fill=hx("#c01818"))
    d.ellipse([13, 4, 18, 9], fill=hx("#f0c040"))
tex("blackjack_top", bj_top)

def table_side(d, im):
    d.rectangle([0, 0, 31, 31], fill=hx("#6a4424"))
    for y in range(0, 32, 5): d.line([0, y, 31, y], fill=hx("#5a3a1a"))
    d.rectangle([0, 0, 31, 4], fill=hx("#3a2410"))
tex("table_side", table_side)

for b, top_tex in (("roulette_table", "roulette_top"), ("blackjack_table", "blackjack_top")):
    m = {"parent": "minecraft:block/block", "textures": {"felt": f"jsn_craft:block/casino/{top_tex}", "wood": "jsn_craft:block/casino/table_side",
                                                          "particle": "jsn_craft:block/casino/table_side"},
         "elements": [box([0, 11, 0], [16, 14, 16], n="wood", s="wood", e="wood", w="wood", u="felt", dn="wood"),
                      box([6, 0, 6], [10, 11, 10], n="wood", s="wood", e="wood", w="wood", u="wood", dn="wood"),
                      box([3, 0, 3], [13, 1, 13], n="wood", s="wood", e="wood", w="wood", u="wood", dn="wood")]}
    wj(f"{A}/models/block/{b}.json", m)
    wj(f"{A}/blockstates/{b}.json", {"variants": {f"facing={f}": ({"model": f"jsn_craft:block/{b}"} | ({"y": y} if y else {}))
                                                  for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    wj(f"{A}/models/item/{b}.json", {"parent": f"jsn_craft:block/{b}"})
    wj(f"{D}/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"jsn_craft:{b}"}],
                                                                                  "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    p = f"{R}/data/minecraft/tags/block/mineable/axe.json"
    dd = json.load(open(p, encoding="utf-8")) if os.path.exists(p) else {"replace": False, "values": []}
    if f"jsn_craft:{b}" not in dd["values"]: dd["values"].append(f"jsn_craft:{b}")
    wj(p, dd)
wj(f"{D}/recipe/roulette_table.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["GWG", "PCP", " P "],
    "key": {"G": {"item": "minecraft:gold_ingot"}, "W": {"item": "minecraft:green_carpet"}, "P": {"tag": "minecraft:planks"}, "C": {"item": "minecraft:compass"}},
    "result": {"id": "jsn_craft:roulette_table", "count": 1}})
wj(f"{D}/recipe/blackjack_table.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["GWG", "PBP", " P "],
    "key": {"G": {"item": "minecraft:gold_ingot"}, "W": {"item": "minecraft:green_carpet"}, "P": {"tag": "minecraft:planks"}, "B": {"item": "minecraft:book"}},
    "result": {"id": "jsn_craft:blackjack_table", "count": 1}})
for lang, add in {"ja_jp": {"block.jsn_craft.roulette_table": "ルーレット台", "block.jsn_craft.blackjack_table": "ブラックジャック台"},
                  "en_us": {"block.jsn_craft.roulette_table": "Roulette Table", "block.jsn_craft.blackjack_table": "Blackjack Table"}}.items():
    p = f"{A}/lang/{lang}.json"; dd = json.load(open(p, encoding="utf-8")); dd.update(add); wj(p, dd)
print("tables ok")
