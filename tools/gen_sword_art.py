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

SWORDS = {
    "stone_job_sword": lambda: sword(STONE, hx("#c8c8c8"), WOOD, WOOD, STONE, 0.9),
    "iron_job_sword": lambda: sword(IRON, hx("#ffffff"), IRON, LEATHER, IRON, 0.95),
    "diamond_job_sword": lambda: sword(DIA, hx("#e8fffc"), GOLD, AZURE, DIA, 1.0),
    "netherite_job_sword": lambda: sword(NETH, hx("#a898a8"), GOLD, NETH, NETH, 1.02, neth_deco, 1.1),
    "moon_silver_job_sword": lambda: sword(MOON, hx("#ffffff"), MOON, AZURE, MOON, 1.05, moon_deco),
    "star_silver_job_sword": lambda: sword((hx("#8a8a9a"), hx("#e0e4f0"), hx("#ffffff")), hx("#fffbe8"), GOLD, AZURE, GOLD, 1.08, star_deco, 1.1),
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
