"""
精霊の術師の魔導書（64×64）、精霊石、精霊の岩壁ブロック（16×16）の絵を作る。
    python3 tools/gen_grimoire_art.py 出力フォルダ(item) 出力フォルダ(block) [--preview 出力.png]
"""
import math, os, sys, random
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
from gen_staff_art import Art, hx, N, NIGHT, GOLD, constellation

def book(cover, edge, trim, gem, gem2=None, deco=None, star=False):
    """少し斜めから見た、閉じた厚い本。表紙の真ん中に宝石、角に金具"""
    a = Art()
    # 本の形（左下→右上へ少し傾く平行四辺形）
    TL, TR, BR, BL = (14, 12), (50, 8), (52, 50), (16, 56)
    a.poly([(x + 3, y + 3) for x, y in (TL, TR, BR, BL)], cover[0])          # 影
    a.poly([(BL[0], BL[1]), (BR[0], BR[1]), (BR[0] + 3, BR[1] + 3), (BL[0] + 3, BL[1] + 4)], edge[1])  # ページの小口
    for k in range(3):
        y0 = 1 + k
        a.d.line([a.P(BL[0] + 1, BL[1] + y0), a.P(BR[0] + 1, BR[1] + y0)], fill=edge[0] + (255,), width=int(0.5 * 8))
    a.poly([TL, TR, BR, BL], cover[1])                                         # 表紙
    a.poly([TL, TR, (TR[0] - 1, TR[1] + 6), (TL[0] + 0.5, TL[1] + 6)], cover[2])  # 上の光
    a.poly([TL, (TL[0] + 5, TL[1] - 0.5), (BL[0] + 5, BL[1] - 0.5), BL], cover[0])  # 背表紙
    # 縁取り
    inset = [(19, 16), (47, 13), (48, 46), (21, 51)]
    for i in range(4):
        p1, p2 = inset[i], inset[(i + 1) % 4]
        a.d.line([a.P(*p1), a.P(*p2)], fill=trim[1] + (255,), width=int(1.4 * 8))
    for (x, y) in inset:   # 角の金具
        a.poly([(x - 3, y), (x, y - 3), (x + 3, y), (x, y + 3)], trim[1])
        a.poly([(x - 1.5, y - 0.5), (x, y - 2), (x + 1, y - 0.5)], trim[2])
    if deco: deco(a)
    cx, cy = 34, 32
    a.poly([(cx, cy - 10), (cx + 9, cy), (cx, cy + 10), (cx - 9, cy)], trim[0])   # 宝石の台
    if star:   # 星銀：大きな金の星
        a.star(cx, cy, 9.0, 3.6, 5, hx("#8a5a10")); a.star(cx, cy, 7.6, 3.0, 5, hx("#ffe040")); a.star(cx - 0.5, cy - 0.5, 3.0, 1.2, 5, hx("#fffbe0"))
        for (x, y, r) in ((60, 6, 2.2), (8, 10, 1.8), (60, 52, 1.6)): a.star(x, y, r, r * 0.35, 4, hx("#fff4c0"), rot=0)
    elif gem2:   # 4色の精霊石
        cols = gem2
        for i, ang in enumerate((-90, 0, 90, 180)):
            r = math.radians(ang)
            a.ball(cx + 4 * math.cos(r), cy + 4 * math.sin(r), 3.3, *cols[i])
    else:
        a.ball(cx, cy, 6.0, *gem)
    # 留め具（右側）
    a.poly([(50, 26), (56, 25), (56, 33), (50, 34)], trim[1]); a.poly([(54, 27), (56, 27), (56, 31), (54, 31)], trim[2])
    return a.finish(glow=(cx, cy, 14 if star else 13, hx("#fff0a0") if star else (gem[2] if gem else hx("#ffffff"))))

BROWN = (hx("#3a2214"), hx("#6a4022"), hx("#8a5a32"))
PAGE = (hx("#b8a888"), hx("#e8dcc0"))
IRONT = (hx("#4a4a54"), hx("#a0a0ac"), hx("#e8e8f0"))
GOLDT = (hx("#6a4a10"), hx("#d0a030"), hx("#fff0a0"))

def mandrake_deco(a):   # 根っこと葉
    for (x1, y1, x2, y2) in ((24, 46, 30, 40), (40, 45, 36, 40), (26, 20, 31, 25), (44, 18, 38, 24)):
        a.d.line([a.P(x1, y1), a.P(x2, y2)], fill=hx("#3a6a2a") + (255,), width=int(1.4 * 8))
    for (x, y) in ((24, 46), (40, 45), (26, 20), (44, 18)):
        a.ball(x, y, 1.8, hx("#2a5a1a"), hx("#5aa040"), hx("#a0e080"))

def neth_deco(a):
    for (x1, y1, x2, y2) in ((22, 22, 27, 27), (45, 20, 40, 25), (24, 46, 29, 41), (45, 43, 40, 38)):
        a.d.line([a.P(x1, y1), a.P(x2, y2)], fill=hx("#ff5030") + (255,), width=int(1.0 * 8))

def moon_deco(a):
    a.arc(34, 32, 13, 200, 340, 1.4, hx("#eef4ff"))
    for (x, y) in ((25, 21), (44, 44)): a.star(x, y, 2.0, 0.7, 4, hx("#eef4ff"), rot=0)

def star_deco(a):
    a.ring(34, 32, 13, 1.0, hx("#ffe080"))
    for (x, y) in ((24, 20), (45, 18), (25, 46), (46, 44)): a.star(x, y, 2.2, 0.8, 4, hx("#fff4c0"), rot=0)

SPIRIT4 = [(hx("#2a7a4a"), hx("#7ae0a0"), hx("#d0ffe0")),   # 風
           (hx("#7a4a1a"), hx("#d09050"), hx("#ffe0b0")),   # 土
           (hx("#1a4a9a"), hx("#4a90f0"), hx("#c0e0ff")),   # 水
           (hx("#3a1a6a"), hx("#8a50e0"), hx("#e0c0ff"))]   # 闇

def star_grimoire():
    """星銀の魔導書：夜空色の表紙、金の縁と角、青い星座の彫り、真ん中に大きな星、星の留め具"""
    def deco(a):
        constellation(a, [(23, 22), (27, 19), (31, 22), (29, 26)])
        constellation(a, [(38, 44), (42, 41), (45, 44), (43, 47)])
        a.ring(34, 32, 11, 1.0, hx("#c0d8ff"))
        for (x, y) in ((24, 46), (45, 20)): a.star(x, y, 1.6, 0.6, 4, hx("#ffffff"), rot=0)
    img = book(NIGHT, (hx("#c8b070"), hx("#fff0c0")), (hx("#8a5a10"), hx("#e0a830"), hx("#fff0a0")), None, deco=deco, star=True)
    return img

GRIMOIRES = {
    "old_grimoire": lambda: book(BROWN, PAGE, IRONT, (hx("#5a4a3a"), hx("#a09080"), hx("#e0d8c8"))),
    "mandrake_grimoire": lambda: book((hx("#1e3a1a"), hx("#3a6a2e"), hx("#5a8a40")), PAGE, IRONT, (hx("#2a5a1a"), hx("#6ac050"), hx("#d0ffb0")), deco=mandrake_deco),
    "spirit_grimoire": lambda: book((hx("#1a2a3a"), hx("#2e4a5e"), hx("#4a6a80")), PAGE, GOLDT, (hx("#2a7a4a"), hx("#7ae0a0"), hx("#ffffff")), gem2=SPIRIT4),
    "netherite_grimoire": lambda: book((hx("#18141a"), hx("#3a3238"), hx("#5a5058")), PAGE, GOLDT, (hx("#7a1010"), hx("#e02a20"), hx("#ff9a60")), deco=neth_deco),
    "moon_silver_grimoire": lambda: book((hx("#2a3458"), hx("#4a5a8a"), hx("#7a8ab8")), PAGE, (hx("#7a88b0"), hx("#c8d8f8"), hx("#ffffff")), (hx("#7a88b0"), hx("#c8d8f8"), hx("#f4f8ff")), deco=moon_deco),
    "star_silver_grimoire": star_grimoire,
}

def spirit_stone():
    a = Art()
    for i, (dx, dy) in enumerate(((0, -11), (11, 0), (0, 11), (-11, 0))):
        x, y = 32 + dx, 32 + dy
        c = SPIRIT4[i]
        a.poly([(x, y - 9), (x + 7, y), (x, y + 9), (x - 7, y)], c[0])
        a.poly([(x, y - 8), (x + 5.5, y), (x, y + 1), (x - 5.5, y)], c[1])
        a.poly([(x - 1, y - 6), (x + 2, y - 2), (x - 1, y - 1)], c[2])
    a.ball(32, 32, 4.5, hx("#a0a0b0"), hx("#ffffff"), hx("#ffffff"))
    return a.finish(glow=(32, 32, 22, hx("#ffffff")))

def spirit_rock():
    """精霊の岩壁（16×16、緑に光る筋の入った岩）"""
    rng = random.Random(77)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            v = 92 + rng.randint(-14, 14) + (8 if (x // 4 + y // 4) % 2 == 0 else 0)
            px[x, y] = (v, int(v * 0.95), int(v * 0.85), 255)
    for (x, y) in [(2, 3), (3, 4), (4, 4), (5, 5), (10, 2), (11, 3), (11, 4), (12, 5), (6, 10), (7, 11), (8, 11), (9, 12), (13, 10), (13, 11), (14, 12), (1, 13), (2, 13)]:
        px[x, y] = (130, 240, 170, 255)
    for i in range(16):
        px[i, 0] = tuple(max(0, c - 30) for c in px[i, 0][:3]) + (255,)
        px[0, i] = tuple(max(0, c - 30) for c in px[0, i][:3]) + (255,)
    return img

if __name__ == "__main__":
    out_item, out_block = sys.argv[1], sys.argv[2]
    os.makedirs(out_item, exist_ok=True); os.makedirs(out_block, exist_ok=True)
    imgs = []
    for k, f in GRIMOIRES.items():
        im = f(); im.save(os.path.join(out_item, k + ".png")); imgs.append(im)
    st = spirit_stone(); st.save(os.path.join(out_item, "spirit_stone.png")); imgs.append(st)
    spirit_rock().save(os.path.join(out_block, "spirit_rock.png"))
    if "--preview" in sys.argv:
        prev = sys.argv[sys.argv.index("--preview") + 1]
        sheet = Image.new("RGBA", (len(imgs) * 200 + 10, 300), (40, 44, 52, 255))
        for i, im in enumerate(imgs):
            sheet.alpha_composite(im.resize((192, 192), Image.NEAREST), (i * 200 + 8, 8))
            sheet.alpha_composite(im.resize((48, 48), Image.NEAREST), (i * 200 + 80, 230))
        sheet.alpha_composite(spirit_rock().resize((96, 96), Image.NEAREST), (len(imgs) * 200 - 100, 200))
        sheet.save(prev)
    print(list(GRIMOIRES) + ["spirit_stone", "spirit_rock"])
