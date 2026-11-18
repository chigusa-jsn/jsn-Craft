"""
獣使いの鞭（64×64、6段階）。柄が左下、鞭がくるりと輪を描いて右上へ。
    python3 tools/gen_whip_art.py 出力フォルダ [--preview 出力.png]
"""
import math, os, sys
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
from gen_staff_art import Art, hx, NETH, MOON, GOLD, IRON, NIGHT, WOOD, night_shaft, constellation

def bez(p0, p1, p2, p3, n):
    out = []
    for i in range(n):
        t = i / n
        mt = 1 - t
        out.append((mt ** 3 * p0[0] + 3 * mt * mt * t * p1[0] + 3 * mt * t * t * p2[0] + t ** 3 * p3[0],
                    mt ** 3 * p0[1] + 3 * mt * mt * t * p1[1] + 3 * mt * t * t * p2[1] + t ** 3 * p3[1]))
    return out

# 鞭の通り道：柄の先 → 右下へふくらんで輪 → 右上の先端へ
PATH = (bez((22, 42), (32, 30), (58, 30), (54, 46), 40)
        + bez((54, 46), (50, 58), (30, 54), (32, 38), 40)
        + bez((32, 38), (34, 24), (46, 14), (58, 5), 41))

def lash(a, cols, w0=3.4, w1=1.0, beads=None, bead_every=10, barbs=None):
    n = len(PATH)
    dark, mid, light = cols
    for layer, col, k, off in ((0, dark, 1.0, 0.35), (1, mid, 0.74, 0.0), (2, light, 0.28, -0.35)):
        for i, (x, y) in enumerate(PATH):
            w = w0 + (w1 - w0) * (i / (n - 1)) ** 0.8
            r = w / 2 * k
            o = off * w / 2
            a.d.ellipse([*a.P(x - r + o, y - r + o), *a.P(x + r + o, y + r + o)], fill=col + (255,))
    if barbs:
        for i in range(8, n - 6, 12):
            (x, y), (x2, y2) = PATH[i], PATH[i + 1]
            dx, dy = x2 - x, y2 - y; L = math.hypot(dx, dy) or 1
            nx, ny = -dy / L, dx / L
            a.poly([(x + nx * 1.0, y + ny * 1.0), (x + nx * 4.2 - dx / L * 2.2, y + ny * 4.2 - dy / L * 2.2), (x - dx / L * 0.8, y - dy / L * 0.8)], barbs)
    if beads:
        for i in range(bead_every // 2, n - 4, bead_every):
            x, y = PATH[i]
            w = w0 + (w1 - w0) * (i / (n - 1)) ** 0.8
            beads(a, x, y, max(1.0, w * 0.75))

def handle(a, grip, cap, pommel_r=2.6):
    a.shaft(6, 59, 21, 44, 4.4, *grip)
    for t in (0.25, 0.5, 0.75):
        x, y = 6 + 15 * t, 59 - 15 * t
        a.shaft(x - 1.3, y + 1.3, x + 1.3, y - 1.3, 4.8, grip[0], grip[0], grip[1])
    a.ball(21.5, 43.5, 2.4, *cap)
    a.ball(5.5, 59.5, pommel_r, *cap)

LEATHER = (hx("#3a2010"), hx("#7a4824"), hx("#b07440"))
DLEATHER = (hx("#1e1410"), hx("#4a3226"), hx("#7a5a46"))
BONE = (hx("#8a7a5a"), hx("#e4dcc4"), hx("#fffaf0"))
RED = (hx("#5a0808"), hx("#c02018"), hx("#ff8060"))

def leather_whip():
    a = Art()
    lash(a, LEATHER)
    handle(a, WOOD, LEATHER)
    return a.finish()

def iron_whip():
    a = Art()
    lash(a, DLEATHER, beads=lambda a, x, y, r: a.ball(x, y, r, *IRON), bead_every=8)
    handle(a, DLEATHER, IRON)
    return a.finish()

def bone_whip():
    a = Art()
    lash(a, (hx("#5a4a30"), hx("#a8946c"), hx("#d8c8a0")), w0=3.6,
         beads=lambda a, x, y, r: a.ball(x, y, r * 1.15, *BONE), bead_every=5)
    handle(a, BONE, BONE, 2.8)
    # 牙の鍔
    a.poly([(19, 42), (16, 35), (21, 41)], BONE[1]); a.poly([(23, 46), (30, 49), (22, 44)], BONE[1])
    return a.finish()

def netherite_whip():
    a = Art()
    lash(a, NETH, barbs=RED[1], beads=lambda a, x, y, r: a.ball(x, y, r * 0.8, *RED), bead_every=18)
    handle(a, NETH, GOLD)
    a.ball(5.5, 59.5, 1.4, *RED)
    return a.finish()

def moon_whip():
    a = Art()
    lash(a, MOON, w0=3.5, beads=lambda a, x, y, r: a.ball(x, y, r * 0.9, hx("#6a7ab0"), hx("#c8d8ff"), hx("#ffffff")), bead_every=12)
    handle(a, (hx("#2a2a50"), hx("#4a4a80"), hx("#7a7ab0")), MOON)
    a.arc(5.5, 59.5, 4.2, 150, 330, 1.3, hx("#eef4ff"))
    a.arc(56, 8, 4.0, 200, 20, 1.2, hx("#eef4ff"))
    return a.finish(glow=(56, 8, 7, hx("#c8d8ff")))

def star_whip():
    a = Art()
    # 夜空色の鞭に、金の星の鎖
    def starbead(a, x, y, r):
        a.star(x, y, r * 1.9, r * 0.8, 4, GOLD[0], rot=0); a.star(x, y, r * 1.5, r * 0.6, 4, hx("#ffe040"), rot=0)
    lash(a, NIGHT, w0=3.8, beads=starbead, bead_every=11)
    # 鞭のふちに金の細い線
    for i, (x, y) in enumerate(PATH[:-6]):
        if i % 2 == 0:
            a.d.point(a.P(x, y), fill=GOLD[2] + (255,))
    night_shaft(a, 6, 59, 21, 44, bands=(0.2, 0.45, 0.7))
    a.ball(21.5, 43.5, 2.6, *GOLD)
    # 柄頭に大きな金の星
    a.star(6.5, 58, 6.2, 2.5, 5, GOLD[0]); a.star(6.5, 58, 5.0, 2.0, 5, hx("#ffe040")); a.star(6.2, 57.6, 2.0, 0.8, 5, hx("#fffbe0"))
    # 先端の星
    a.star(58, 5, 4.6, 1.8, 5, GOLD[0]); a.star(58, 5, 3.6, 1.4, 5, hx("#ffe040"))
    constellation(a, [(8, 30), (13, 24), (20, 22), (24, 15)])
    for (x, y, r) in ((40, 30, 1.6), (62, 22, 1.8), (30, 6, 1.6)): a.star(x, y, r, r * 0.35, 4, hx("#fff4c0"), rot=0)
    return a.finish(glow=(58, 5, 8, hx("#fff0a0")))

WHIPS = {"leather_whip": leather_whip, "iron_whip": iron_whip, "bone_whip": bone_whip,
         "netherite_whip": netherite_whip, "moon_silver_whip": moon_whip, "star_silver_whip": star_whip}

if __name__ == "__main__":
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    imgs = []
    for k, f in WHIPS.items():
        im = f(); im.save(os.path.join(out, k + ".png")); imgs.append(im)
    if "--preview" in sys.argv:
        prev = sys.argv[sys.argv.index("--preview") + 1]
        sheet = Image.new("RGBA", (len(imgs) * 200 + 10, 210), (40, 44, 52, 255))
        for i, im in enumerate(imgs):
            sheet.alpha_composite(im.resize((192, 192), Image.NEAREST), (i * 200 + 8, 8))
        sheet.save(prev)
    print(list(WHIPS))
