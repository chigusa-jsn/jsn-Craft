"""ビッグホイール：円盤・枠・針の絵、軸とかけ台のモデル、レシピ、名前"""
import json, math, os
from PIL import Image, ImageDraw, ImageFont

R = os.path.join(os.path.dirname(__file__), "..", "src/main/resources")
A = f"{R}/assets/jsn_craft"; D = f"{R}/data/jsn_craft"
def hx(s): s = s.lstrip("#"); return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)
def save(img, path): os.makedirs(os.path.dirname(path), exist_ok=True); img.save(path)
def wj(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f: json.dump(obj, f, ensure_ascii=False, indent=2); f.write("\n")

SEG = [20, 1, 3, 1, 5, 1, 3, 1, 10, 1, 3, 1, 5, 1, 3, 1, 5, 1, 10, 1, 3, 1, 5, 3, 1]
COL = {1: "#b0b0b0", 3: "#40c060", 5: "#4080f0", 10: "#b050e0", 20: "#f0c030"}
S = 512; C = S / 2; RAD = S / 2 - 6
STEP = 360 / len(SEG)
try:
    FONT = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 26)
except Exception:
    FONT = ImageFont.load_default()

# 円盤
im = Image.new("RGBA", (S, S), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
d.ellipse([C - RAD - 4, C - RAD - 4, C + RAD + 4, C + RAD + 4], fill=hx("#3a1a10"))
for i, v in enumerate(SEG):
    a0 = -90 + (i - 0.5) * STEP; a1 = a0 + STEP
    d.pieslice([C - RAD, C - RAD, C + RAD, C + RAD], a0, a1, fill=hx(COL[v]), outline=hx("#2a1a10"), width=3)
for i, v in enumerate(SEG):
    # 数字：外側に、外向きの文字
    ang = math.radians(-90 + i * STEP)
    r = RAD * 0.80
    x, y = C + math.cos(ang) * r, C + math.sin(ang) * r
    t = Image.new("RGBA", (60, 34), (0, 0, 0, 0)); td = ImageDraw.Draw(t)
    td.text((30, 17), str(v), font=FONT, fill=hx("#1a1010"), anchor="mm")
    t = t.rotate(-(i * STEP), resample=Image.BICUBIC, expand=True)
    im.alpha_composite(t, (int(x - t.width / 2), int(y - t.height / 2)))
d.ellipse([C - 46, C - 46, C + 46, C + 46], fill=hx("#5a2a18"), outline=hx("#f0c030"), width=6)
d.ellipse([C - 14, C - 14, C + 14, C + 14], fill=hx("#f0c030"))
save(im, f"{A}/textures/block/wheel/face.png")

# 枠：木の輪と電球
F = 512; FC = F / 2
im = Image.new("RGBA", (F, F), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
outer = F / 2 - 4; inner = outer * 2.5 / 2.9 - 2
d.ellipse([FC - outer, FC - outer, FC + outer, FC + outer], fill=hx("#6a3018"))
d.ellipse([FC - outer + 8, FC - outer + 8, FC + outer - 8, FC + outer - 8], fill=hx("#8a4020"))
d.ellipse([FC - inner, FC - inner, FC + inner, FC + inner], fill=hx("#2a1008"))
for k in range(28):
    a = math.radians(k * 360 / 28)
    rr = (outer + inner) / 2
    x, y = FC + math.cos(a) * rr, FC + math.sin(a) * rr
    c = hx("#fff0a0") if k % 2 == 0 else hx("#ff8060")
    d.ellipse([x - 9, y - 9, x + 9, y + 9], fill=hx("#3a1a08")); d.ellipse([x - 6, y - 6, x + 6, y + 6], fill=c)
save(im, f"{A}/textures/block/wheel/frame.png")

# 針（下向きの三角）
im = Image.new("RGBA", (64, 64), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
d.polygon([(8, 4), (56, 4), (32, 60)], fill=hx("#f0f0f0"), outline=hx("#202020"))
d.polygon([(16, 10), (48, 10), (32, 46)], fill=hx("#e03030"))
save(im, f"{A}/textures/block/wheel/pointer.png")

# 軸のブロック（小さな金具）
def tex16(name, fn):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); fn(ImageDraw.Draw(im)); save(im, f"{A}/textures/block/wheel/{name}.png")
tex16("axle", lambda d: (d.rectangle([0, 0, 15, 15], fill=hx("#8a6a30")), d.rectangle([4, 4, 11, 11], fill=hx("#f0c030"))))
def face(t, uv=(0, 0, 16, 16)): return {"texture": "#" + t, "uv": list(uv)}
def allf(t): return {k: face(t) for k in ("north", "south", "east", "west", "up", "down")}
wj(f"{A}/models/block/big_wheel.json", {"parent": "minecraft:block/block", "textures": {"a": "jsn_craft:block/wheel/axle", "particle": "jsn_craft:block/wheel/axle"},
    "elements": [{"from": [5, 5, 12], "to": [11, 11, 16], "faces": allf("a")}]})
wj(f"{A}/blockstates/big_wheel.json", {"variants": {f"facing={f}": ({"model": "jsn_craft:block/big_wheel"} | ({"y": y} if y else {}))
                                                    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
# アイテム：円盤の絵
small = Image.open(f"{A}/textures/block/wheel/face.png").resize((32, 32), Image.LANCZOS)
save(small, f"{A}/textures/item/big_wheel.png")
wj(f"{A}/models/item/big_wheel.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "jsn_craft:item/big_wheel"}})

# かけ台
def term_front(d):
    d.rectangle([0, 0, 15, 15], fill=hx("#3a2a20")); d.rectangle([2, 2, 13, 8], fill=hx("#101820"))
    for i, v in enumerate([1, 3, 5, 10, 20]):
        d.rectangle([2 + i * 2 + (i > 0) * 0, 10, 3 + i * 2, 12], fill=hx(COL[v]))
    d.point([(4, 4), (7, 5), (10, 4)], fill=hx("#f0c030"))
def term_side(d):
    d.rectangle([0, 0, 15, 15], fill=hx("#3a2a20")); d.rectangle([0, 0, 15, 1], fill=hx("#f0c030"))
def term_top(d):
    d.rectangle([0, 0, 15, 15], fill=hx("#5a3a28"))
    for i, v in enumerate([1, 3, 5, 10, 20]): d.rectangle([1 + i * 3, 5, 2 + i * 3, 10], fill=hx(COL[v]))
tex16("terminal_front", term_front); tex16("terminal_side", term_side); tex16("terminal_top", term_top)
wj(f"{A}/models/block/wheel_terminal.json", {"parent": "minecraft:block/block", "textures": {"f": "jsn_craft:block/wheel/terminal_front",
    "s": "jsn_craft:block/wheel/terminal_side", "t": "jsn_craft:block/wheel/terminal_top", "particle": "jsn_craft:block/wheel/terminal_side"},
    "elements": [{"from": [2, 0, 2], "to": [14, 14, 14], "faces": {"north": face("f"), "south": face("s"), "east": face("s"), "west": face("s"), "up": face("t"), "down": face("s")}}]})
wj(f"{A}/blockstates/wheel_terminal.json", {"variants": {f"facing={f}": ({"model": "jsn_craft:block/wheel_terminal"} | ({"y": y} if y else {}))
                                                         for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
wj(f"{A}/models/item/wheel_terminal.json", {"parent": "jsn_craft:block/wheel_terminal"})
for b in ("big_wheel", "wheel_terminal"):
    wj(f"{D}/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"jsn_craft:{b}"}],
                                                                              "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
wj(f"{D}/recipe/big_wheel.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["PGP", "GCG", "PGP"],
    "key": {"P": {"tag": "minecraft:planks"}, "G": {"item": "minecraft:gold_ingot"}, "C": {"item": "minecraft:clock"}}, "result": {"id": "jsn_craft:big_wheel", "count": 1}})
wj(f"{D}/recipe/wheel_terminal.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["GBG", "PRP", "PPP"],
    "key": {"P": {"tag": "minecraft:planks"}, "G": {"item": "minecraft:gold_nugget"}, "B": {"item": "minecraft:stone_button"}, "R": {"item": "minecraft:redstone"}},
    "result": {"id": "jsn_craft:wheel_terminal", "count": 1}})
pk = f"{R}/data/minecraft/tags/block/mineable/axe.json"
dd = json.load(open(pk, encoding="utf-8")) if os.path.exists(pk) else {"replace": False, "values": []}
for b in ("jsn_craft:big_wheel", "jsn_craft:wheel_terminal"):
    if b not in dd["values"]: dd["values"].append(b)
wj(pk, dd)
NAMES = {"ja_jp": {"block.jsn_craft.big_wheel": "ビッグホイール", "block.jsn_craft.wheel_terminal": "ホイールのかけ台"},
         "en_us": {"block.jsn_craft.big_wheel": "Big Wheel", "block.jsn_craft.wheel_terminal": "Wheel Betting Terminal"}}
for lang, add in NAMES.items():
    p = f"{A}/lang/{lang}.json"; dd = json.load(open(p, encoding="utf-8")); dd.update(add); wj(p, dd)
print("ok")

# ================= 競馬の台 =================
def race_front(d):
    d.rectangle([0, 0, 15, 15], fill=hx("#1a3a24")); d.rectangle([1, 1, 14, 9], fill=hx("#3a8a3a"))
    for y, c in ((3, "#e03030"), (5, "#3060e0"), (7, "#f0d030")): d.rectangle([3 + y % 3, y, 5 + y % 3, y + 1], fill=hx(c))
    d.line([12, 1, 12, 9], fill=hx("#ffffff")); d.rectangle([2, 11, 13, 13], fill=hx("#f0c030"))
def race_side(d):
    d.rectangle([0, 0, 15, 15], fill=hx("#1a3a24")); d.rectangle([0, 0, 15, 1], fill=hx("#60c060"))
def race_top(d):
    d.rectangle([0, 0, 15, 15], fill=hx("#2a5a34")); d.rectangle([2, 2, 13, 13], outline=hx("#60c060"))
tex16("race_front", race_front); tex16("race_side", race_side); tex16("race_top", race_top)
wj(f"{A}/models/block/race_terminal.json", {"parent": "minecraft:block/block", "textures": {"f": "jsn_craft:block/wheel/race_front",
    "s": "jsn_craft:block/wheel/race_side", "t": "jsn_craft:block/wheel/race_top", "particle": "jsn_craft:block/wheel/race_side"},
    "elements": [{"from": [2, 0, 2], "to": [14, 14, 14], "faces": {"north": face("f"), "south": face("s"), "east": face("s"), "west": face("s"), "up": face("t"), "down": face("s")}}]})
wj(f"{A}/blockstates/race_terminal.json", {"variants": {f"facing={f}": ({"model": "jsn_craft:block/race_terminal"} | ({"y": y} if y else {}))
                                                        for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
wj(f"{A}/models/item/race_terminal.json", {"parent": "jsn_craft:block/race_terminal"})
wj(f"{D}/loot_table/blocks/race_terminal.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "jsn_craft:race_terminal"}],
                                                                                     "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
wj(f"{D}/recipe/race_terminal.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["GHG", "PRP", "PPP"],
    "key": {"P": {"tag": "minecraft:planks"}, "G": {"item": "minecraft:gold_nugget"}, "H": {"item": "minecraft:hay_block"}, "R": {"item": "minecraft:redstone"}},
    "result": {"id": "jsn_craft:race_terminal", "count": 1}})
pk = f"{R}/data/minecraft/tags/block/mineable/axe.json"; dd = json.load(open(pk, encoding="utf-8"))
if "jsn_craft:race_terminal" not in dd["values"]: dd["values"].append("jsn_craft:race_terminal")
wj(pk, dd)
for lang, name in (("ja_jp", "競馬の台"), ("en_us", "Horse Race Terminal")):
    p = f"{A}/lang/{lang}.json"; dd = json.load(open(p, encoding="utf-8")); dd["block.jsn_craft.race_terminal"] = name; wj(p, dd)
print("race ok")

# ================= チンチロ対戦の卓 =================
def tbl_top(d):
    d.rectangle([0, 0, 15, 15], fill=hx("#2a6a3a")); d.rectangle([0, 0, 15, 15], outline=hx("#8a5a2a"))
def tbl_side(d):
    d.rectangle([0, 0, 15, 15], fill=hx("#8a5a2a")); d.line([0, 3, 15, 3], fill=hx("#6a4018")); d.line([0, 12, 15, 12], fill=hx("#6a4018"))
def bowl(d):
    d.rectangle([0, 0, 15, 15], fill=hx("#f0e8d8")); d.rectangle([0, 0, 15, 2], fill=hx("#3060a0"))
def bowl_in(d):
    d.rectangle([0, 0, 15, 15], fill=hx("#f4f0e6")); d.rectangle([3, 4, 5, 6], fill=hx("#ffffff")); d.point((4, 5), fill=hx("#d01818"))
    d.rectangle([9, 8, 11, 10], fill=hx("#ffffff")); d.point([(9, 8), (11, 10)], fill=hx("#1a1a1a"))
tex16("chin_top", tbl_top); tex16("chin_side", tbl_side); tex16("chin_bowl", bowl); tex16("chin_bowl_in", bowl_in)
def el(fr, to, faces): return {"from": fr, "to": to, "faces": faces}
def F6(n, u=None, dn=None): return {"north": face(n), "south": face(n), "east": face(n), "west": face(n), "up": face(u or n), "down": face(dn or n)}
wj(f"{A}/models/block/chinchiro_table.json", {"parent": "minecraft:block/block", "textures": {"t": "jsn_craft:block/wheel/chin_top", "s": "jsn_craft:block/wheel/chin_side",
    "b": "jsn_craft:block/wheel/chin_bowl", "i": "jsn_craft:block/wheel/chin_bowl_in", "particle": "jsn_craft:block/wheel/chin_side"},
    "elements": [el([0, 8, 0], [16, 10, 16], F6("s", u="t")), el([1, 0, 1], [3, 8, 3], F6("s")), el([13, 0, 1], [15, 8, 3], F6("s")),
                 el([1, 0, 13], [3, 8, 15], F6("s")), el([13, 0, 13], [15, 8, 15], F6("s")), el([5, 10, 5], [11, 13, 11], F6("b", u="i"))]})
wj(f"{A}/blockstates/chinchiro_table.json", {"variants": {"": {"model": "jsn_craft:block/chinchiro_table"}}})
wj(f"{A}/models/item/chinchiro_table.json", {"parent": "jsn_craft:block/chinchiro_table"})
wj(f"{D}/loot_table/blocks/chinchiro_table.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "jsn_craft:chinchiro_table"}],
                                                                                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
wj(f"{D}/recipe/chinchiro_table.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": [" B ", "GGG", "P P"],
    "key": {"P": {"tag": "minecraft:planks"}, "G": {"item": "minecraft:green_carpet"}, "B": {"item": "minecraft:bowl"}}, "result": {"id": "jsn_craft:chinchiro_table", "count": 1}})
pk = f"{R}/data/minecraft/tags/block/mineable/axe.json"; dd = json.load(open(pk, encoding="utf-8"))
if "jsn_craft:chinchiro_table" not in dd["values"]: dd["values"].append("jsn_craft:chinchiro_table")
wj(pk, dd)
for lang, name in (("ja_jp", "チンチロ対戦の卓"), ("en_us", "Chinchiro Table")):
    p = f"{A}/lang/{lang}.json"; dd = json.load(open(p, encoding="utf-8")); dd["block.jsn_craft.chinchiro_table"] = name; wj(p, dd)
print("chinchiro ok")
