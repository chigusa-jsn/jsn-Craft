"""ギャンブラーの勝負札（6段階）の絵・モデル・レシピ・名前を作る。"""
import json, os
from PIL import Image, ImageDraw

R = os.path.join(os.path.dirname(__file__), "..", "src/main/resources")
A, D = f"{R}/assets/jsn_craft", f"{R}/data/jsn_craft"
def hx(s): s = s.lstrip("#"); return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)
def wj(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f: json.dump(o, f, ensure_ascii=False, indent=2); f.write("\n")

TIERS = [  # id, 名前, 英語名, ふち, 裏の色, 模様
    ("paper_cards", "紙の勝負札", "Paper Cards", "#d8d0c0", "#b03030", "#f0e8d8"),
    ("iron_cards", "鉄の勝負札", "Iron Cards", "#c8ccd6", "#3a4a8a", "#e8ecf4"),
    ("gold_cards", "金の勝負札", "Gold Cards", "#f0c040", "#8a1a2a", "#ffe8a0"),
    ("netherite_cards", "ネザライトの勝負札", "Netherite Cards", "#4a4048", "#1a1418", "#e05a2a"),
    ("moon_silver_cards", "月銀の勝負札", "Moon Silver Cards", "#e2e8f8", "#5a6aa0", "#cfe0ff"),
    ("star_silver_cards", "星銀の勝負札", "Star Silver Cards", "#e6b836", "#16163a", "#ffe060"),
]
for t, (iid, name, en, edge, back, mark) in enumerate(TIERS, 1):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    # 3枚を扇に広げる（後ろ2枚は裏、いちばん前は表）
    def card(poly, face, front):
        d.polygon(poly, fill=hx("#1a1018"))
        inner = [(x + (1 if i in (0, 3) else -1) * 0, y) for i, (x, y) in enumerate(poly)]
        d.polygon(poly, outline=hx("#1a1018"), fill=hx(edge))
    def quad(cx, cy, ang):
        import math
        w, h = 2.6, 4.6
        c, s = math.cos(math.radians(ang)), math.sin(math.radians(ang))
        pts = [(-w, -h), (w, -h), (w, h), (-w, h)]
        return [(cx + x * c - y * s, cy + x * s + y * c) for x, y in pts]
    for ang, cx, cy, front in ((-38, 4.6, 9.6, False), (-6, 7.8, 7.4, False), (30, 11.2, 9.0, True)):
        q = quad(cx, cy, ang)
        d.polygon(q, fill=hx("#1a1018"))
        qi = quad(cx, cy, ang)
        import math
        shrink = [(cx + (x - cx) * 0.78, cy + (y - cy) * 0.82) for x, y in qi]
        d.polygon(q, fill=hx(edge))
        d.polygon(shrink, fill=hx("#f8f4ea") if front else hx(back))
        if front:
            x0, y0 = int(round(cx)) - 1, int(round(cy)) - 1
            d.point([(x0, y0), (x0 + 2, y0), (x0, y0 + 1), (x0 + 1, y0 + 1), (x0 + 2, y0 + 1), (x0 + 1, y0 + 2)], fill=hx("#d01818"))
        else:
            d.point([(int(cx), int(cy))], fill=hx(mark))
    if t >= 5:
        for x, y in ((2, 2), (13, 3)): d.point((x, y), fill=hx(mark))
    im.save(f"{A}/textures/item/{iid}.png")
    wj(f"{A}/models/item/{iid}.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": f"jsn_craft:item/{iid}"}})
    for lang, nm in (("ja_jp", name), ("en_us", en)):
        p = f"{A}/lang/{lang}.json"; dd = json.load(open(p, encoding="utf-8")); dd[f"item.jsn_craft.{iid}"] = nm; wj(p, dd)
# レシピ
wj(f"{D}/recipe/paper_cards.json", {"type": "minecraft:crafting_shaped", "category": "combat", "pattern": [" P ", "PNP", " P "],
    "key": {"P": {"item": "minecraft:paper"}, "N": {"item": "minecraft:iron_nugget"}}, "result": {"id": "jsn_craft:paper_cards", "count": 1}})
UP = [("jsn_craft:job_template_2", "minecraft:iron_ingot"), ("jsn_craft:job_template_3", "minecraft:gold_ingot"),
      ("minecraft:netherite_upgrade_smithing_template", "minecraft:netherite_ingot"), ("jsn_craft:moon_silver_template", "jsn_craft:moon_silver_ingot"),
      ("jsn_craft:star_silver_template", "jsn_craft:star_silver_ingot")]
for i, (tpl, add) in enumerate(UP):
    wj(f"{D}/recipe/{TIERS[i + 1][0]}_smithing.json", {"type": "minecraft:smithing_transform", "template": {"item": tpl},
        "base": {"item": f"jsn_craft:{TIERS[i][0]}"}, "addition": {"item": add}, "result": {"id": f"jsn_craft:{TIERS[i + 1][0]}", "count": 1}})
# 星銀はキラキラ対象
print("cards ok")
