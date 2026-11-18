"""
斧使いの斧（64×64、6段階）の絵を作る。左下が持ち手、右上が刃。
    python3 tools/gen_axe_art.py 出力フォルダ [--preview 出力.png]
"""
import math, os, sys
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
from gen_staff_art import Art, hx, NETH, MOON, STAR, WOOD, GOLD, IRON

def blade(a, cx, cy, size, metal, edge, double=False, rot=-45):
    """柄の先についた三日月形の刃。cx,cy は柄に付く根元"""
    r = math.radians(rot)
    ux, uy = math.cos(r), math.sin(r)          # 柄の向き（右上）
    px, py = -uy, ux                           # 柄に直角（左上→右下）
    def P(u, v): return (cx + ux * u + px * v, cy + uy * u + py * v)
    sides = (1, -1) if double else (1,)
    for sgn in sides:
        def edge_curve(scale_v, scale_u):
            out = []
            for k in range(11):
                t = -1 + k * 0.2
                out.append(P(size * scale_u * t, sgn * (size * scale_v + size * 0.35 * (1 - t * t))))
            return out
        outer = edge_curve(0.75, 0.75)
        pts = [P(-size * 0.22, 0), P(-size * 0.25, sgn * size * 0.35)] + outer + [P(size * 0.25, sgn * size * 0.35), P(size * 0.22, 0)]
        a.poly([(x + 1.2, y + 1.2) for x, y in pts], metal[0])
        a.poly(pts, metal[1])
        inner = edge_curve(0.6, 0.68)
        a.poly(outer + inner[::-1], edge)
        a.poly([P(-size * 0.15, sgn * size * 0.3), P(size * 0.15, sgn * size * 0.35), P(0, sgn * size * 0.7)], metal[2])

def axe(shaft, metal, edge, double=False, size=14, deco=None, wrap=None):
    a = Art()
    a.shaft(9, 56, 50, 15, 3.6, *shaft)
    if wrap:
        for t in (0.12, 0.22):
            x, y = 9 + 41 * t, 56 - 41 * t
            a.shaft(x - 1.6, y + 1.6, x + 1.6, y - 1.6, 4.8, *wrap)
    blade(a, 42, 23, size, metal, edge, double)
    a.ball(42, 23, 3.0, *metal)   # 刃の付け根
    if deco: deco(a)
    a.ball(10, 55, 2.6, *metal)
    return a.finish()

STONE = (hx("#4a4a4a"), hx("#8a8a8a"), hx("#b8b8b8"))
DIA = (hx("#1a6a6a"), hx("#4ad8d0"), hx("#c0fff8"))
LEATHER = (hx("#3a2418"), hx("#6a4428"), hx("#8a6038"))

def neth_deco(a):
    for (x, y) in ((40, 25), (44, 21)): a.ball(x, y, 1.4, hx("#7a1010"), hx("#ff4020"), hx("#ffb080"))

def moon_deco(a):
    a.arc(42, 23, 6, 120, 330, 1.4, hx("#eef4ff"))
    a.star(56, 8, 2.0, 0.7, 4, hx("#eef4ff"), rot=0)

def star_deco(a):
    a.star(42, 23, 4.5, 1.8, 5, hx("#ffe040"))
    for (x, y, r) in ((58, 6, 2.2), (60, 30, 1.6), (28, 10, 1.6)): a.star(x, y, r, r * 0.35, 4, hx("#fff4c0"), rot=0)

AXES = {
    "stone_job_axe": lambda: axe(WOOD, STONE, hx("#d0d0d0"), size=12),
    "iron_job_axe": lambda: axe(WOOD, IRON, hx("#ffffff"), size=13, wrap=LEATHER),
    "diamond_job_axe": lambda: axe(WOOD, DIA, hx("#e8fffc"), size=14, wrap=LEATHER),
    "netherite_job_axe": lambda: axe(NETH, NETH, hx("#c8b8c0"), double=True, size=13, deco=neth_deco, wrap=(hx("#8a5a10"), hx("#e0a830"), hx("#fff0a0"))),
    "moon_silver_job_axe": lambda: axe(MOON, MOON, hx("#ffffff"), double=True, size=14, deco=moon_deco),
    "star_silver_job_axe": lambda: axe(STAR, GOLD, hx("#fffbe0"), double=True, size=15, deco=star_deco),
}

if __name__ == "__main__":
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    imgs = []
    for k, f in AXES.items():
        im = f(); im.save(os.path.join(out, k + ".png")); imgs.append(im)
    if "--preview" in sys.argv:
        prev = sys.argv[sys.argv.index("--preview") + 1]
        sheet = Image.new("RGBA", (len(imgs) * 200 + 10, 300), (40, 44, 52, 255))
        for i, im in enumerate(imgs):
            sheet.alpha_composite(im.resize((192, 192), Image.NEAREST), (i * 200 + 8, 8))
            sheet.alpha_composite(im.resize((48, 48), Image.NEAREST), (i * 200 + 80, 230))
        sheet.save(prev)
    print(list(AXES))
