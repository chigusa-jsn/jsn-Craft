"""
剣士の剣（64×64、6段階）の絵を作る。左下が柄、右上が切っ先。
    python3 tools/gen_sword_art.py 出力フォルダ [--preview 出力.png]
"""
import math, os, sys
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
from gen_staff_art import Art, hx, NETH, MOON, STAR, WOOD, GOLD, IRON

def sword(blade, edge, guard, grip, pommel, length=1.0, deco=None, wide=1.0):
    a = Art()
    # 刃（左下→右上）。根元(22,42) から 切っ先へ
    bx, by = 22, 42
    tx, ty = 22 + 34 * length, 42 - 34 * length
    ux, uy = (tx - bx), (ty - by)
    L = math.hypot(ux, uy); ux, uy = ux / L, uy / L
    px, py = -uy, ux
    w = 3.6 * wide
    body = [(bx + px * w, by + py * w), (tx - ux * 6 + px * w * 0.8, ty - uy * 6 + py * w * 0.8), (tx, ty),
            (tx - ux * 6 - px * w * 0.8, ty - uy * 6 - py * w * 0.8), (bx - px * w, by - py * w)]
    a.poly([(x + 1, y + 1) for x, y in body], blade[0])
    a.poly(body, blade[1])
    # 片側を明るく（刃の光）、真ん中に樋（みぞ）
    a.poly([(bx + px * w, by + py * w), (tx - ux * 6 + px * w * 0.8, ty - uy * 6 + py * w * 0.8), (tx, ty), (bx, by)], edge)
    a.d.line([a.P(bx + ux * 2, by + uy * 2), a.P(tx - ux * 9, ty - uy * 9)], fill=blade[0] + (255,), width=int(1.0 * 8))
    # 鍔
    gw = 9
    a.poly([(bx + px * gw + ux * 1.5, by + py * gw + uy * 1.5), (bx + px * gw - ux * 1.5, by + py * gw - uy * 1.5),
            (bx - px * gw - ux * 1.5, by - py * gw - uy * 1.5), (bx - px * gw + ux * 1.5, by - py * gw + uy * 1.5)], guard[1])
    a.ball(bx + px * gw, by + py * gw, 1.8, *guard)
    a.ball(bx - px * gw, by - py * gw, 1.8, *guard)
    # 柄と柄頭
    a.shaft(bx - ux * 1.5, by - uy * 1.5, bx - ux * 13, by - uy * 13, 3.4, *grip)
    a.ball(bx - ux * 14.5, by - uy * 14.5, 2.6, *pommel)
    a.ball(bx, by, 2.2, *guard)
    if deco: deco(a, bx, by, tx, ty)
    return a.finish()

STONE = (hx("#4a4a4a"), hx("#8a8a8a"), hx("#b8b8b8"))
DIA = (hx("#1a6a6a"), hx("#4ad8d0"), hx("#c0fff8"))
LEATHER = (hx("#3a2418"), hx("#6a4428"), hx("#8a6038"))
AZURE = (hx("#1a3a7a"), hx("#3a7ae0"), hx("#a0d0ff"))

def neth_deco(a, bx, by, tx, ty):
    a.ball(bx, by, 2.4, hx("#7a1010"), hx("#ff4020"), hx("#ffb080"))

def moon_deco(a, bx, by, tx, ty):
    a.arc(bx, by, 6, 200, 330, 1.4, hx("#eef4ff"))
    a.star(tx - 2, ty + 10, 2.0, 0.7, 4, hx("#eef4ff"), rot=0)

def star_deco(a, bx, by, tx, ty):
    a.star(bx, by, 4.0, 1.6, 5, hx("#ffe040"))
    for (x, y, r) in ((60, 6, 2.2), (44, 6, 1.5), (60, 22, 1.5)): a.star(x, y, r, r * 0.35, 4, hx("#fff4c0"), rot=0)

def star_silver_sword():
    """星銀の剣：長い白金の刃に金のふち、青く光る樋に星座、金の翼の鍔に大きな星、夜空色の柄"""
    a = Art()
    NIGHT = (hx("#141030"), hx("#2a2460"), hx("#4a44a0"))
    bx, by = 19, 45
    tx, ty = 60, 4
    ux, uy = tx - bx, ty - by
    L = math.hypot(ux, uy); ux, uy = ux / L, uy / L
    px, py = -uy, ux
    def Q(u, v): return (bx + ux * u + px * v, by + uy * u + py * v)
    # 金のふち → 白金の刃
    for w, col in ((4.0, GOLD[1]), (3.1, hx("#e8ecff"))):
        body = [Q(0, w), Q(L - 8, w * 0.85), Q(L, 0), Q(L - 8, -w * 0.85), Q(0, -w)]
        if col == GOLD[1]: a.poly([(x + 1, y + 1) for x, y in body], GOLD[0])
        a.poly(body, col)
    a.poly([Q(0, 3.1), Q(L - 8, 2.6), Q(L, 0), Q(0, 0)], hx("#ffffff"))
    # 青く光る樋と星座
    a.d.line([a.P(*Q(5, 0)), a.P(*Q(L - 12, 0))], fill=hx("#4a6ae0") + (255,), width=int(1.4 * 8))
    a.d.line([a.P(*Q(5, 0)), a.P(*Q(L - 12, 0))], fill=hx("#a0c0ff") + (255,), width=int(0.6 * 8))
    for u in (12, 21, 30, 39):
        cx, cy = Q(u, 0)
        a.star(cx, cy, 1.8, 0.7, 4, hx("#ffffff"), rot=0)
    # 金の翼の鍔（刃の方へ反り上がる3枚の羽）
    for sgn in (1, -1):
        for i, (tu, tv, bu) in enumerate(((9, 12.5, 1.0), (4.5, 11.5, -0.5), (0, 9.5, -2.0))):
            feather = [Q(bu, sgn * 2.5), Q(bu + 2.5, sgn * (tv * 0.55)), Q(tu, sgn * tv), Q(bu - 1.0, sgn * (tv * 0.75)), Q(bu - 2.0, sgn * 3.0)]
            a.poly([(x + 0.8, y + 0.8) for x, y in feather], GOLD[0])
            a.poly(feather, GOLD[1] if i != 1 else hx("#f0c040"))
            x1, y1 = Q(bu + 0.5, sgn * 3.2); x2, y2 = Q(tu - 1.0, sgn * (tv - 1.5))
            a.d.line([a.P(x1, y1), a.P(x2, y2)], fill=GOLD[2] + (255,), width=int(0.7 * 8))
    # 柄（夜空色に金の帯）と星の柄頭
    a.shaft(*Q(-2, 0), *Q(-14, 0), 3.6, *NIGHT)
    for u in (-5, -9, -13):
        x1, y1 = Q(u + 0.8, 0); x2, y2 = Q(u - 0.8, 0)
        a.shaft(x1, y1, x2, y2, 4.6, *GOLD)
    cx, cy = Q(-16.5, 0)
    a.star(cx, cy, 4.0, 1.7, 5, GOLD[0]); a.star(cx, cy, 3.2, 1.3, 5, hx("#ffe040"))
    # 鍔の真ん中の大きな星
    a.star(bx, by, 5.4, 2.2, 5, GOLD[0])
    a.star(bx, by, 4.5, 1.8, 5, hx("#ffe040"))
    a.star(bx - 0.4, by - 0.4, 2.2, 0.9, 5, hx("#fffbe0"))
    for (x, y, r) in ((50, 4, 2.4), (62, 18, 2.0), (36, 12, 1.6), (58, 34, 1.4)): a.star(x, y, r, r * 0.35, 4, hx("#fff4c0"), rot=0)
    return a.finish(glow=(bx, by, 16, hx("#fff0a0")))

SWORDS = {
    "stone_job_sword": lambda: sword(STONE, hx("#c8c8c8"), WOOD, WOOD, STONE, 0.9),
    "iron_job_sword": lambda: sword(IRON, hx("#ffffff"), IRON, LEATHER, IRON, 0.95),
    "diamond_job_sword": lambda: sword(DIA, hx("#e8fffc"), GOLD, AZURE, DIA, 1.0),
    "netherite_job_sword": lambda: sword(NETH, hx("#a898a8"), GOLD, NETH, NETH, 1.02, neth_deco, 1.1),
    "moon_silver_job_sword": lambda: sword(MOON, hx("#ffffff"), MOON, AZURE, MOON, 1.05, moon_deco),
    "star_silver_job_sword": star_silver_sword,
}

if __name__ == "__main__":
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    imgs = []
    for k, f in SWORDS.items():
        im = f(); im.save(os.path.join(out, k + ".png")); imgs.append(im)
    if "--preview" in sys.argv:
        prev = sys.argv[sys.argv.index("--preview") + 1]
        sheet = Image.new("RGBA", (len(imgs) * 200 + 10, 300), (40, 44, 52, 255))
        for i, im in enumerate(imgs):
            sheet.alpha_composite(im.resize((192, 192), Image.NEAREST), (i * 200 + 8, 8))
            sheet.alpha_composite(im.resize((48, 48), Image.NEAREST), (i * 200 + 80, 230))
        sheet.save(prev)
    print(list(SWORDS))
