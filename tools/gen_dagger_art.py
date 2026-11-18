"""
暗殺者の短剣（64×64、6段階）。柄の真ん中が (18,46)、切っ先が右上。
（逆手持ちの向きは tools/gen_dagger_models.py で計算する）
    python3 tools/gen_dagger_art.py 出力フォルダ [--preview 出力.png]
"""
import math, os, sys
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
from gen_staff_art import Art, hx, NETH, MOON, GOLD, IRON, NIGHT

U = (0.7071, -0.7071); P = (0.7071, 0.7071)
GRIP = (18, 46)

def at(u, v=0.0):
    return (GRIP[0] + U[0] * u + P[0] * v, GRIP[1] + U[1] * u + P[1] * v)

def dagger(blade, edge, guard, grip, pommel, curve=0.0, deco=None, glow=None):
    a = Art()
    # 刃（片刃・少し反る）
    L0, L1 = 6, 36
    back, front_ = [], []
    for i in range(21):
        t = i / 20
        u = L0 + (L1 - L0) * t
        bend = curve * math.sin(math.pi * t)
        w = 3.2 * (1 - t ** 2.2) + 0.4
        back.append(at(u, -1.2 + bend))
        front_.append(at(u, w - 1.2 + bend))
    body = back + front_[::-1]
    a.poly([(x + 0.8, y + 0.8) for x, y in body], blade[0])
    a.poly(body, blade[1])
    a.poly(front_[:-1] + [at(L1 - 1.5, 1.0 + curve * 0.2)] + [at(L0, 0.8)], edge)
    a.d.line([a.P(*p) for p in back], fill=blade[2] + (255,), width=int(0.7 * 8))
    # 鍔
    a.poly([at(L0 - 1.5, -5), at(L0, -5), at(L0, 5), at(L0 - 1.5, 5)], guard[1])
    a.ball(*at(L0 - 0.8, -5), 1.4, *guard); a.ball(*at(L0 - 0.8, 5), 1.4, *guard)
    # 柄（巻き）
    for k in range(4):
        a.ball(*at(L0 - 3 - k * 2.4), 2.0, *grip)
    a.ball(*at(L0 - 13.5), 2.3, *pommel)
    if deco: deco(a)
    return a.finish(glow=glow)

STONE = (hx("#4a4a4a"), hx("#8a8a8a"), hx("#c0c0c0"))
DIA = (hx("#1a6a6a"), hx("#4ad8d0"), hx("#c0fff8"))
BLACK = (hx("#141418"), hx("#2e2e38"), hx("#4a4a5a"))
PURPLE = (hx("#2a1240"), hx("#5a2a8a"), hx("#9a6ad0"))

def neth_deco(a):
    a.ball(*at(4.8, 0), 1.6, hx("#7a1010"), hx("#ff4020"), hx("#ffb080"))

def moon_deco(a):
    x, y = at(4.8, 0); a.arc(x, y, 5, 160, 320, 1.2, hx("#eef4ff"))

def star_deco(a):
    x, y = at(4.8, 0)
    a.star(x, y, 4.2, 1.7, 5, GOLD[0]); a.star(x, y, 3.4, 1.4, 5, hx("#ffe040"))
    for t in (14, 22, 30):
        px, py = at(t, 0.6); a.star(px, py, 1.3, 0.5, 4, hx("#c0d8ff"), rot=0)
    for (px, py, r) in ((56, 8, 2.2), (34, 6, 1.6), (58, 30, 1.6)): a.star(px, py, r, r * 0.35, 4, hx("#fff4c0"), rot=0)

DAGGERS = {
    "stone_dagger": lambda: dagger(STONE, hx("#d8d8d8"), STONE, (hx("#3a2418"), hx("#6a4428"), hx("#8a6038")), STONE),
    "iron_dagger": lambda: dagger(IRON, hx("#ffffff"), IRON, BLACK, IRON),
    "diamond_dagger": lambda: dagger(DIA, hx("#e8fffc"), BLACK, PURPLE, DIA, curve=0.8),
    "netherite_dagger": lambda: dagger(NETH, hx("#b8a8b8"), GOLD, BLACK, NETH, curve=1.0, deco=neth_deco),
    "moon_silver_dagger": lambda: dagger(MOON, hx("#ffffff"), MOON, PURPLE, MOON, curve=1.0, deco=moon_deco),
    "star_silver_dagger": lambda: dagger((hx("#7a7aa0"), hx("#e8ecff"), hx("#ffffff")), hx("#c8d8ff"), GOLD, NIGHT, GOLD, curve=1.2, deco=star_deco,
                                         glow=(32, 32, 20, hx("#c0d8ff"))),
}

if __name__ == "__main__":
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    imgs = []
    for k, f in DAGGERS.items():
        im = f(); im.save(os.path.join(out, k + ".png")); imgs.append(im)
    if "--preview" in sys.argv:
        prev = sys.argv[sys.argv.index("--preview") + 1]
        sheet = Image.new("RGBA", (len(imgs) * 200 + 10, 210), (40, 44, 52, 255))
        for i, im in enumerate(imgs):
            sheet.alpha_composite(im.resize((192, 192), Image.NEAREST), (i * 200 + 8, 8))
        sheet.save(prev)
    print(list(DAGGERS))
