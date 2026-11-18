"""
槍兵の槍（6段階）と海神の三叉槍（4段階）の絵（64×64）。左下が石突き、右上が穂先。
    python3 tools/gen_spear_art.py 出力フォルダ [--preview 出力.png]
"""
import math, os, sys
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
from gen_staff_art import Art, hx, NETH, MOON, GOLD, IRON, WOOD, NIGHT, constellation

U = (0.7071, -0.7071)    # 柄の向き（右上）
P = (0.7071, 0.7071)     # 柄に直角

def at(base, u, v=0.0):
    return (base[0] + U[0] * u + P[0] * v, base[1] + U[1] * u + P[1] * v)

def spear(shaft, head, edge, collar, tassel=None, deco=None, bands=None, glow=None):
    a = Art()
    s0, s1 = (6, 58), (46, 18)
    a.shaft(*s0, *s1, 3.2, *shaft)
    if bands:
        for t in bands:
            x, y = s0[0] + (s1[0] - s0[0]) * t, s0[1] + (s1[1] - s0[1]) * t
            a.shaft(x - 1.3, y + 1.3, x + 1.3, y - 1.3, 4.4, *GOLD)
    base = s1
    # 穂先（木の葉形）
    leaf = [at(base, 0, 2.2), at(base, 7, 4.2), at(base, 16, 0), at(base, 7, -4.2), at(base, 0, -2.2)]
    a.poly([(x + 0.8, y + 0.8) for x, y in leaf], head[0])
    a.poly(leaf, head[1])
    a.poly([at(base, 1, 0), at(base, 7, 4.2), at(base, 16, 0)], edge)
    a.d.line([a.P(*at(base, 1, 0)), a.P(*at(base, 13, 0))], fill=head[2] + (255,), width=int(0.8 * 8))
    # 口金
    a.shaft(*at(base, -2.5), *at(base, 1), 4.6, *collar)
    if tassel:   # 房飾り
        for k in (-1, 1):
            p0 = at(base, -3, 0)
            a.d.line([a.P(*p0), a.P(p0[0] - 2 + k * 2.5, p0[1] + 6)], fill=tassel + (255,), width=int(1.4 * 8))
    a.ball(*s0, 2.2, *collar)
    if deco: deco(a, base)
    return a.finish(glow=glow)

def trident(shaft, head, edge, collar, deco=None, bands=None, glow=None):
    a = Art()
    s0, s1 = (6, 58), (42, 22)
    a.shaft(*s0, *s1, 3.2, *shaft)
    if bands:
        for t in bands:
            x, y = s0[0] + (s1[0] - s0[0]) * t, s0[1] + (s1[1] - s0[1]) * t
            a.shaft(x - 1.3, y + 1.3, x + 1.3, y - 1.3, 4.4, *GOLD)
    base = s1
    # 横の棒（三叉の根元）
    a.poly([at(base, 1, -8), at(base, 3.5, -8), at(base, 3.5, 8), at(base, 1, 8)], head[1])
    a.poly([at(base, 1, -8), at(base, 1.8, -8), at(base, 1.8, 8), at(base, 1, 8)], head[2])
    # 3本の刃
    for v, L in ((-7, 13), (0, 18), (7, 13)):
        prong = [at(base, 3, v - 1.4), at(base, L, v - 1.2), at(base, L + 4, v), at(base, L, v + 1.2), at(base, 3, v + 1.4)]
        a.poly([(x + 0.7, y + 0.7) for x, y in prong], head[0])
        a.poly(prong, head[1])
        a.poly([at(base, L, v - 1.2), at(base, L + 4, v), at(base, L, v)], edge)
        a.poly([at(base, L - 1, v + 1.2), at(base, L + 1.5, v + 3.2), at(base, L + 1, v + 0.2)], head[1])   # かえし
    a.shaft(*at(base, -2.5), *at(base, 1.5), 4.6, *collar)
    a.ball(*s0, 2.2, *collar)
    if deco: deco(a, base)
    return a.finish(glow=glow)

STONE = (hx("#4a4a4a"), hx("#8a8a8a"), hx("#c0c0c0"))
PRIS = (hx("#2a6a6a"), hx("#5ab8a8"), hx("#b0f0e0"))
SEA = (hx("#1a4a6a"), hx("#3a90b8"), hx("#a0e0ff"))
AQUA_SHAFT = (hx("#1a3a4a"), hx("#2a6a80"), hx("#4aa0b8"))

def neth_deco(a, base):
    x, y = at(base, -0.5); a.ball(x, y, 1.6, hx("#7a1010"), hx("#ff4020"), hx("#ffb080"))

def moon_deco(a, base):
    x, y = at(base, -0.5); a.arc(x, y, 5.5, 140, 300, 1.2, hx("#eef4ff"))
    a.star(58, 20, 2.0, 0.7, 4, hx("#eef4ff"), rot=0)

def star_deco(a, base):
    x, y = at(base, -0.5)
    a.star(x, y, 4.6, 1.9, 5, GOLD[0]); a.star(x, y, 3.8, 1.5, 5, hx("#ffe040"))
    for (px, py, r) in ((60, 4, 2.2), (34, 8, 1.6), (60, 30, 1.6), (20, 40, 1.4)): a.star(px, py, r, r * 0.35, 4, hx("#fff4c0"), rot=0)

def sea_deco(a, base):
    x, y = at(base, -0.5); a.ball(x, y, 2.0, hx("#1a3a7a"), hx("#3a9ae0"), hx("#c0f0ff"))
    for (px, py) in ((56, 30), (30, 10)): a.ball(px, py, 1.2, hx("#3a90b8"), hx("#a0e0ff"), hx("#ffffff"))

STAR_HEAD = (hx("#8a5a10"), hx("#e0a830"), hx("#fff0a0"))

SPEARS = {
    "stone_spear": lambda: spear(WOOD, STONE, hx("#d8d8d8"), STONE),
    "iron_spear": lambda: spear(WOOD, IRON, hx("#ffffff"), IRON, tassel=hx("#c02a2a")),
    "prismarine_spear": lambda: spear(AQUA_SHAFT, PRIS, hx("#e0fff8"), GOLD, tassel=hx("#2a8ab0")),
    "netherite_spear": lambda: spear(NETH, NETH, hx("#b8a8b8"), GOLD, tassel=hx("#e02a20"), deco=neth_deco, bands=(0.2,)),
    "moon_silver_spear": lambda: spear(MOON, MOON, hx("#ffffff"), MOON, tassel=hx("#a0d0ff"), deco=moon_deco),
    "star_silver_spear": lambda: spear(NIGHT, STAR_HEAD, hx("#fffbe0"), GOLD, tassel=hx("#ffe080"), deco=star_deco, bands=(0.12, 0.24, 0.36), glow=(50, 14, 14, hx("#fff0a0"))),
}
TRIDENTS = {
    "sea_trident": lambda: trident(AQUA_SHAFT, SEA, hx("#e0fbff"), GOLD, deco=sea_deco),
    "netherite_sea_trident": lambda: trident(NETH, NETH, hx("#b8a8b8"), GOLD, deco=neth_deco, bands=(0.2,)),
    "moon_silver_sea_trident": lambda: trident(MOON, MOON, hx("#ffffff"), MOON, deco=moon_deco),
    "star_silver_sea_trident": lambda: trident(NIGHT, STAR_HEAD, hx("#fffbe0"), GOLD, deco=star_deco, bands=(0.12, 0.24, 0.36), glow=(48, 16, 16, hx("#fff0a0"))),
}

if __name__ == "__main__":
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    imgs = []
    for k, f in {**SPEARS, **TRIDENTS}.items():
        im = f(); im.save(os.path.join(out, k + ".png")); imgs.append(im)
    if "--preview" in sys.argv:
        prev = sys.argv[sys.argv.index("--preview") + 1]
        cols = 5
        sheet = Image.new("RGBA", (cols * 200 + 10, 2 * 200 + 10), (40, 44, 52, 255))
        for i, im in enumerate(imgs):
            sheet.alpha_composite(im.resize((192, 192), Image.NEAREST), ((i % cols) * 200 + 8, (i // cols) * 200 + 8))
        sheet.save(prev)
    print(list(SPEARS) + list(TRIDENTS))
