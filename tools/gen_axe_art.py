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

def star_silver_axe():
    """星銀の大斧：夜空色の柄に金の帯、白金の大きな両刃、真ん中に光る星、刃に星座の彫り"""
    a = Art()
    NIGHT = (hx("#141030"), hx("#2a2460"), hx("#4a44a0"))
    WHITE = (hx("#7a7aa0"), hx("#e8ecff"), hx("#ffffff"))
    a.shaft(8, 57, 52, 13, 3.8, *NIGHT)
    for t in (0.08, 0.2, 0.32):   # 金の帯
        x, y = 8 + 44 * t, 57 - 44 * t
        a.shaft(x - 1.4, y + 1.4, x + 1.4, y - 1.4, 5.2, *GOLD)
    # 金のふち（少し大きい刃を先に描く）
    blade(a, 41, 24, 19.5, GOLD, hx("#ffe880"), double=True)
    blade(a, 41, 24, 17.5, WHITE, hx("#d8f0ff"), double=True)
    # 刃の星座の彫り
    for pts in (((33, 13), (30, 17), (26, 16), (24, 20)), ((49, 35), (52, 31), (56, 32), (58, 28))):
        for i in range(len(pts) - 1):
            a.d.line([a.P(*pts[i]), a.P(*pts[i + 1])], fill=hx("#8aa8ff") + (255,), width=int(0.8 * 8))
        for q in pts: a.ball(q[0], q[1], 0.9, hx("#6a88ff"), hx("#c0d8ff"), hx("#ffffff"))
    # 上の槍先
    a.poly([(49, 16), (57, 4), (60, 7), (52, 18)], GOLD[1]); a.poly([(51, 15), (57, 6), (58, 7), (52, 16)], GOLD[2])
    # 真ん中の大きな星
    a.star(41, 24, 7.5, 3.0, 5, GOLD[0])
    a.star(41, 24, 6.2, 2.5, 5, hx("#ffe040"))
    a.star(40.5, 23.5, 2.8, 1.2, 5, hx("#fffbe0"))
    a.ball(10, 55, 3.0, *GOLD)
    for (x, y, r) in ((60, 20, 2.4), (24, 6, 2.0), (62, 44, 1.6), (18, 30, 1.4)): a.star(x, y, r, r * 0.35, 4, hx("#fff4c0"), rot=0)
    return a.finish(glow=(41, 24, 24, hx("#fff0a0")))

AXES = {
    "stone_job_axe": lambda: axe(WOOD, STONE, hx("#d0d0d0"), size=12),
    "iron_job_axe": lambda: axe(WOOD, IRON, hx("#ffffff"), size=13, wrap=LEATHER),
    "diamond_job_axe": lambda: axe(WOOD, DIA, hx("#e8fffc"), size=14, wrap=LEATHER),
    "netherite_job_axe": lambda: axe(NETH, NETH, hx("#c8b8c0"), double=True, size=13, deco=neth_deco, wrap=(hx("#8a5a10"), hx("#e0a830"), hx("#fff0a0"))),
    "moon_silver_job_axe": lambda: axe(MOON, MOON, hx("#ffffff"), double=True, size=14, deco=moon_deco),
    "star_silver_job_axe": star_silver_axe,
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
