"""
剣士の刀（64×64、6段階）の絵を作る。左下が柄、右上が切っ先。反りのある刃に刃文。
    python3 tools/gen_katana_art.py 出力フォルダ [--preview 出力.png]
"""
import math, os, sys
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
from gen_staff_art import Art, hx, NETH, MOON, GOLD, IRON, NIGHT, constellation

def katana(blade, hamon, tsuba, grip, wrap, deco=None, glow=None):
    a = Art()
    # 刃の背（峰）は少し反る：根元(20,44) から 切っ先(60,5)
    def spine(t):   # 0..1
        x = 20 + 40 * t
        y = 44 - 39 * t
        bend = 2.2 * math.sin(math.pi * t)       # 反り（刃側＝左上へふくらむ）
        return (x - bend * 0.7071, y - bend * 0.7071)
    N = (-0.7071, -0.7071)                       # 刃の幅の向き（左上 = 刃側）
    W = 3.6
    back = [spine(i / 30) for i in range(31)]
    edge = [(x + N[0] * W * (1 - 0.6 * (i / 30) ** 3), y + N[1] * W * (1 - 0.6 * (i / 30) ** 3)) for i, (x, y) in enumerate(back)]
    tip = (back[-1][0] + 1.5, back[-1][1] - 1.5)
    body = back + [tip] + edge[::-1]
    a.poly([(x + 0.8, y + 0.8) for x, y in body], blade[0])
    a.poly(body, blade[1])
    # 刃文（波）
    ham = []
    for i in range(31):
        t = i / 30
        x, y = back[i]
        w = W * (0.42 + 0.14 * math.sin(t * 36)) * (1 - 0.6 * t ** 3)
        ham.append((x + N[0] * w, y + N[1] * w))
    a.poly(edge + ham[::-1], hamon)
    a.d.line([a.P(*p) for p in back], fill=blade[2] + (255,), width=int(0.6 * 8))
    # 鍔（丸）
    a.ball(19, 45, 4.6, *tsuba)
    a.ring(19, 45, 3.6, 0.8, tsuba[2])
    # 柄と柄巻き（ひし形の模様）
    for i in range(5):
        x, y = 16 - i * 2.4, 48 + i * 2.4
        a.ball(x, y, 2.1, *grip)
        a.poly([(x, y - 1.6), (x + 1.4, y), (x, y + 1.6), (x - 1.4, y)], wrap)
    a.ball(4.5, 59.5, 2.0, *tsuba)   # 柄頭
    if deco: deco(a)
    return a.finish(glow=glow)

STONE = (hx("#4a4a4a"), hx("#8a8a8a"), hx("#b8b8b8"))
DIA = (hx("#1a6a6a"), hx("#4ad8d0"), hx("#c0fff8"))
BLACK = (hx("#1a1a22"), hx("#3a3a48"), hx("#5a5a6a"))
AZ = (hx("#1a3a7a"), hx("#3a7ae0"), hx("#a0d0ff"))

def star_deco(a):
    a.star(19, 45, 4.4, 1.8, 5, GOLD[0]); a.star(19, 45, 3.6, 1.4, 5, hx("#ffe040"))
    for t in (0.35, 0.55, 0.75):   # 刃に星のきらめき
        x, y = 20 + 40 * t, 44 - 39 * t
        a.star(x - 1.6, y - 1.6, 1.4, 0.5, 4, hx("#ffffff"), rot=0)
    for (x, y, r) in ((58, 18, 2.2), (30, 14, 1.6), (50, 32, 1.6), (8, 40, 1.4)): a.star(x, y, r, r * 0.35, 4, hx("#fff4c0"), rot=0)

def moon_deco(a):
    a.arc(19, 45, 6.5, 200, 340, 1.2, hx("#eef4ff"))
    a.star(56, 18, 2.0, 0.7, 4, hx("#eef4ff"), rot=0)

def neth_deco(a):
    a.ball(19, 45, 1.6, hx("#7a1010"), hx("#ff4020"), hx("#ffb080"))

KATANAS = {
    "stone_katana": lambda: katana(STONE, hx("#d0d0d0"), STONE, BLACK, hx("#8a8a8a")),
    "iron_katana": lambda: katana(IRON, hx("#ffffff"), BLACK, BLACK, hx("#c0c0c8")),
    "diamond_katana": lambda: katana(DIA, hx("#e8fffc"), GOLD, AZ, hx("#ffffff")),
    "netherite_katana": lambda: katana(NETH, hx("#c8b8c8"), GOLD, BLACK, hx("#e02a20"), deco=neth_deco),
    "moon_silver_katana": lambda: katana(MOON, hx("#ffffff"), MOON, AZ, hx("#eef4ff"), deco=moon_deco),
    "star_silver_katana": lambda: katana((hx("#7a7aa0"), hx("#e8ecff"), hx("#ffffff")), hx("#a8c8ff"), GOLD, NIGHT, GOLD[1], deco=star_deco, glow=(40, 24, 22, hx("#c0d8ff"))),
}

if __name__ == "__main__":
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    imgs = []
    for k, f in KATANAS.items():
        im = f(); im.save(os.path.join(out, k + ".png")); imgs.append(im)
    if "--preview" in sys.argv:
        prev = sys.argv[sys.argv.index("--preview") + 1]
        sheet = Image.new("RGBA", (len(imgs) * 200 + 10, 300), (40, 44, 52, 255))
        for i, im in enumerate(imgs):
            sheet.alpha_composite(im.resize((192, 192), Image.NEAREST), (i * 200 + 8, 8))
            sheet.alpha_composite(im.resize((48, 48), Image.NEAREST), (i * 200 + 80, 230))
        sheet.save(prev)
    print(list(KATANAS))
