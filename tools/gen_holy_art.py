"""
癒し手の聖杖（64×64、6段階）と、銀インゴット・聖水晶の絵を作る。
    python3 tools/gen_holy_art.py 出力フォルダ [--preview 出力.png]
"""
import math, os, sys
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
from gen_staff_art import Art, hx, NETH, MOON, STAR, WOOD, GOLD, night_shaft, constellation

SILVER = (hx("#6a6e7a"), hx("#c4c8d4"), hx("#f4f6ff"))

def wing(a, cx, cy, side, col, light):
    """杖の頭の両わきの小さな翼（side=1 右、-1 左）"""
    for k in range(3):
        r = 9 - k * 2.2
        ang0 = -60 + k * 18
        pts = [(cx, cy)]
        for t in range(7):
            ang = math.radians(ang0 + t * 15)
            pts.append((cx + side * r * math.cos(ang) * 1.2, cy - r * math.sin(ang) * 0.9 - 2 + k * 2))
        a.poly(pts, col if k % 2 == 0 else light)

def holy(shaft, head, gem, halo, wings=True, deco=None):
    a = Art()
    a.shaft(8, 57, 40, 25, 3.4, *shaft)
    cx, cy = 46, 18
    if wings:
        wing(a, cx - 2, cy + 3, -1, head[1], head[2])
        wing(a, cx + 2, cy - 1, 1, head[1], head[2])
    a.ring(cx, cy, 9.5, 1.6, halo)
    # 十字の飾り
    a.poly([(cx - 1.6, cy - 12), (cx + 1.6, cy - 12), (cx + 1.6, cy + 8), (cx - 1.6, cy + 8)], head[0])
    a.poly([(cx - 8, cy - 1.6), (cx + 8, cy - 1.6), (cx + 8, cy + 1.6), (cx - 8, cy + 1.6)], head[0])
    a.poly([(cx - 1.0, cy - 11), (cx + 0.6, cy - 11), (cx + 0.6, cy + 7), (cx - 1.0, cy + 7)], head[2])
    a.ball(cx, cy, 4.6, *gem)
    if deco: deco(a)
    a.poly([(37, 28), (42, 23), (44, 25), (39, 30)], head[0])
    a.ball(12, 53, 2.4, *head)
    return a.finish(glow=(cx, cy, 15, halo))

def star_deco(a):
    for (x, y, r) in ((60, 5, 2.2), (32, 9, 1.6), (61, 30, 1.6)): a.star(x, y, r, r * 0.35, 4, hx("#fff4c0"), rot=0)

def star_holy():
    """星銀の聖杖：夜空色の柄、金の大きな翼、白金の光輪と十字、真ん中に星、光輪に星座"""
    a = Art()
    night_shaft(a, 8, 57, 40, 25)
    cx, cy = 46, 18
    for sgn in (-1, 1):   # 金の大きな翼（3枚の羽が外へ広がる）
        for i, (tx, ty, w) in enumerate(((17, -9, 4.0), (17, -2, 3.6), (14.5, 5, 3.2))):
            base = (cx + sgn * 4, cy + 1)
            tip = (cx + sgn * tx, cy + ty)
            mid = ((base[0] + tip[0]) / 2, (base[1] + tip[1]) / 2)
            dx, dy = tip[0] - base[0], tip[1] - base[1]
            L = math.hypot(dx, dy); nx, ny = -dy / L, dx / L
            feather = [base, (mid[0] + nx * w, mid[1] + ny * w), tip, (mid[0] - nx * w * 0.5, mid[1] - ny * w * 0.5)]
            a.poly([(x + 0.8, y + 0.8) for x, y in feather], GOLD[0])
            a.poly(feather, GOLD[1] if i != 1 else hx("#f0c040"))
            a.d.line([a.P(*base), a.P(*tip)], fill=GOLD[2] + (255,), width=int(0.7 * 8))
    a.ring(cx, cy, 10.5, 2.0, hx("#fff8e0")); a.ring(cx, cy, 10.5, 0.8, GOLD[1])
    constellation(a, [(cx - 7, cy - 7), (cx - 2, cy - 10), (cx + 4, cy - 9), (cx + 8, cy - 5)])
    a.poly([(cx - 1.8, cy - 13), (cx + 1.8, cy - 13), (cx + 1.8, cy + 9), (cx - 1.8, cy + 9)], GOLD[0])
    a.poly([(cx - 9, cy - 1.8), (cx + 9, cy - 1.8), (cx + 9, cy + 1.8), (cx - 9, cy + 1.8)], GOLD[0])
    a.poly([(cx - 1.0, cy - 12), (cx + 0.6, cy - 12), (cx + 0.6, cy + 8), (cx - 1.0, cy + 8)], hx("#fff8e0"))
    a.star(cx, cy, 6.4, 2.6, 5, GOLD[0]); a.star(cx, cy, 5.2, 2.1, 5, hx("#ffe040")); a.star(cx - 0.4, cy - 0.4, 2.2, 0.9, 5, hx("#fffbe0"))
    a.poly([(37, 28), (42, 23), (44, 25), (39, 30)], GOLD[1])
    a.star(9.5, 55.5, 3.6, 1.5, 5, GOLD[0]); a.star(9.5, 55.5, 2.8, 1.1, 5, hx("#ffe040"))
    for (x, y, r) in ((61, 4, 2.4), (28, 6, 1.8), (62, 32, 1.8)): a.star(x, y, r, r * 0.35, 4, hx("#fff4c0"), rot=0)
    return a.finish(glow=(cx, cy, 19, hx("#fff0a0")))

HOLY = {
    "wood_holy_staff": lambda: holy(WOOD, (hx("#8a7a5a"), hx("#e8e0c8"), hx("#ffffff")), (hx("#a08850"), hx("#f0e0a0"), hx("#fffbe8")), hx("#fff0b0"), wings=False),
    "silver_holy_staff": lambda: holy(SILVER, SILVER, (hx("#7090b0"), hx("#c0e0ff"), hx("#ffffff")), hx("#e0f0ff")),
    "crystal_holy_staff": lambda: holy(GOLD, (hx("#c0b080"), hx("#fff8e0"), hx("#ffffff")), (hx("#c0a040"), hx("#fff8c0"), hx("#ffffff")), hx("#ffe880")),
    "netherite_holy_staff": lambda: holy(NETH, (hx("#8a5a10"), hx("#e0a830"), hx("#fff0a0")), (hx("#7a1010"), hx("#ff6040"), hx("#ffd0a0")), hx("#ffb060")),
    "moon_silver_holy_staff": lambda: holy(MOON, MOON, (hx("#7a88b0"), hx("#e0e8ff"), hx("#ffffff")), hx("#eef4ff")),
    "star_silver_holy_staff": star_holy,
}

def ingot():
    a = Art()
    top = [(14, 30), (38, 18), (52, 26), (28, 38)]
    a.poly([(14, 30), (28, 38), (28, 48), (14, 40)], SILVER[0])
    a.poly([(28, 38), (52, 26), (52, 36), (28, 48)], hx("#8a8e9a"))
    a.poly(top, SILVER[1])
    a.poly([(18, 30), (38, 20), (42, 22), (22, 32)], SILVER[2])
    return a.finish()

def holy_crystal():
    a = Art()
    pts = [(32, 6), (46, 22), (40, 54), (24, 54), (18, 22)]
    a.poly([(x + 2, y + 2) for x, y in pts], hx("#a08840"))
    a.poly(pts, hx("#fff4c0"))
    a.poly([(32, 6), (32, 54), (24, 54), (18, 22)], hx("#f0d880"))
    a.poly([(32, 8), (40, 22), (32, 30)], hx("#ffffff"))
    a.ring(32, 32, 26, 1.2, hx("#ffe880"))
    return a.finish(glow=(32, 30, 26, hx("#fff4c0")))

if __name__ == "__main__":
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    imgs = []
    for k, f in HOLY.items():
        im = f(); im.save(os.path.join(out, k + ".png")); imgs.append(im)
    for k, f in (("silver_ingot", ingot), ("holy_crystal", holy_crystal)):
        im = f(); im.save(os.path.join(out, k + ".png")); imgs.append(im)
    if "--preview" in sys.argv:
        prev = sys.argv[sys.argv.index("--preview") + 1]
        sheet = Image.new("RGBA", (len(imgs) * 200 + 10, 300), (40, 44, 52, 255))
        for i, im in enumerate(imgs):
            sheet.alpha_composite(im.resize((192, 192), Image.NEAREST), (i * 200 + 8, 8))
            sheet.alpha_composite(im.resize((48, 48), Image.NEAREST), (i * 200 + 80, 230))
        sheet.save(prev)
    print(list(HOLY))
