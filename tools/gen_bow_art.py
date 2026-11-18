"""
射手の弓（64×64、6段階 × 引いていない/引き3段階）の絵を作る。
    python3 tools/gen_bow_art.py 出力フォルダ [--preview 出力.png]
出力：<id>.png、<id>_pulling_0〜2.png
"""
import math, os, sys
from PIL import Image, ImageOps
sys.path.insert(0, os.path.dirname(__file__))
from gen_staff_art import Art, hx, NETH, MOON, WOOD, GOLD, NIGHT, constellation

T1, T2 = (10, 6), (58, 54)            # 弓の両端（左上・右下）
M = ((T1[0] + T2[0]) / 2, (T1[1] + T2[1]) / 2)
N = (0.7071, -0.7071)                 # 弓のふくらむ向き（右上）

def bez(t, bulge):
    c = (M[0] + N[0] * bulge, M[1] + N[1] * bulge)
    x = (1 - t) ** 2 * T1[0] + 2 * (1 - t) * t * c[0] + t * t * T2[0]
    y = (1 - t) ** 2 * T1[1] + 2 * (1 - t) * t * c[1] + t * t * T2[1]
    return x, y

def bow(limb, grip, string, pull, tips=None, deco=None, arrow=None, bulge=19):
    a = Art()
    b = bulge - pull * 0.25   # 引くと少ししなる
    # 弦
    S = (M[0] - N[0] * (pull * 3.2), M[1] - N[1] * (pull * 3.2))
    for p in (T1, T2):
        a.d.line([a.P(*p), a.P(*S)], fill=string + (255,), width=int(0.9 * 8))
    # 矢
    if pull > 0:
        ac = arrow or ((hx("#5a3a1a"), hx("#9a6a3a")), hx("#c8c8d0"), hx("#ffffff"))
        tip = (S[0] + N[0] * 30, S[1] + N[1] * 30)
        a.d.line([a.P(*S), a.P(*tip)], fill=ac[0][0] + (255,), width=int(2.2 * 8))
        a.d.line([a.P(*S), a.P(*tip)], fill=ac[0][1] + (255,), width=int(1.3 * 8))
        hx_, hy_ = tip
        a.poly([(hx_ + N[0] * 5.5, hy_ + N[1] * 5.5), (hx_ - N[1] * 3.2, hy_ + N[0] * 3.2), (hx_ + N[1] * 3.2, hy_ - N[0] * 3.2)], ac[1])
        for k in (2.0, 4.5):   # 羽根
            q = (S[0] + N[0] * k, S[1] + N[1] * k)
            a.poly([q, (q[0] - N[0] * 3 - N[1] * 3.0, q[1] - N[1] * 3 + N[0] * 3.0), (q[0] - N[0] * 3 + N[1] * 3.0, q[1] - N[1] * 3 - N[0] * 3.0)], ac[2])
    # 弓の本体（太さを変えて3重に描いて立体に）
    pts = [bez(i / 40, b) for i in range(41)]
    # 両端はそり返る（リカーブ）
    def curl(end, toward):
        d = (end[0] - toward[0], end[1] - toward[1]); L = math.hypot(*d); d = (d[0] / L, d[1] / L)
        return [end, (end[0] + d[0] * 2.5 + N[0] * 1.5, end[1] + d[1] * 2.5 + N[1] * 1.5), (end[0] + d[0] * 3.5 + N[0] * 3.5, end[1] + d[1] * 3.5 + N[1] * 3.5)]
    full = curl(pts[0], pts[3])[::-1] + pts + curl(pts[-1], pts[-4])
    for w, col, off in ((5.6, limb[0], 0.7), (4.2, limb[1], 0), (1.4, limb[2], -1.0)):
        q = [(x + N[0] * off, y + N[1] * off) for x, y in full]
        a.d.line([a.P(*p) for p in q], fill=col + (255,), width=int(w * 8), joint="curve")
    # 持ち手
    g = bez(0.5, b)
    for t in (0.42, 0.46, 0.5, 0.54, 0.58):
        gx, gy = bez(t, b)
        a.ball(gx, gy, 2.8, *grip)
    if tips:
        for p in (full[0], full[-1]): a.ball(p[0], p[1], 2.4, *tips)
    if deco: deco(a, b)
    # バニラの弓と同じ向き（木が左上・弦が左下→右上・矢は左上向き）にする。
    # 逆だと手に持ったとき矢が自分のほうを向いてしまう
    return ImageOps.mirror(a.finish())

BONE = (hx("#9a9080"), hx("#e8e0c8"), hx("#ffffff"))
BLAZE = (hx("#8a4a00"), hx("#f0a020"), hx("#fff0a0"))
LEATHER = (hx("#3a2418"), hx("#6a4428"), hx("#8a6038"))

def blaze_deco(a, b):
    for t in (0.2, 0.8):
        x, y = bez(t, b); a.ball(x, y, 1.6, hx("#a02000"), hx("#ff6020"), hx("#ffd080"))

def neth_deco(a, b):
    x, y = bez(0.5, b); a.ball(x, y, 1.8, hx("#7a1010"), hx("#ff4020"), hx("#ffb080"))

def moon_deco(a, b):
    x, y = bez(0.5, b); a.arc(x, y, 5, 200, 340, 1.4, hx("#eef4ff"))
    a.star(60, 6, 2.0, 0.7, 4, hx("#eef4ff"), rot=0)

def star_deco(a, b):
    for t in (0.18, 0.82):   # 金の飾り羽
        x, y = bez(t, b)
        for k in (-1, 1):
            a.poly([(x, y), (x + N[0] * 8 + k * N[1] * 3, y + N[1] * 8 - k * N[0] * 3), (x + N[0] * 3, y + N[1] * 3)], GOLD[1])
    constellation(a, [bez(0.28, b), bez(0.36, b + 3), bez(0.44, b + 1)])
    x, y = bez(0.5, b)
    a.star(x, y, 7.0, 2.8, 5, GOLD[0]); a.star(x, y, 5.8, 2.3, 5, hx("#ffe040")); a.star(x - 0.4, y - 0.4, 2.4, 1.0, 5, hx("#fffbe0"))
    for (px, py, r) in ((60, 4, 2.2), (4, 30, 1.6), (34, 60, 1.6)): a.star(px, py, r, r * 0.35, 4, hx("#fff4c0"), rot=0)

STAR_ARROW = ((hx("#8a5a10"), hx("#e0a830")), hx("#fffbe0"), hx("#c0d8ff"))

BOWS = {
    "wood_job_bow": dict(limb=WOOD, grip=LEATHER, string=hx("#e8e8e8")),
    "bone_job_bow": dict(limb=BONE, grip=LEATHER, string=hx("#f0f0f0"), tips=BONE),
    "blaze_job_bow": dict(limb=BLAZE, grip=(hx("#4a2a10"), hx("#7a4a20"), hx("#aa7040")), string=hx("#ffe0a0"), deco=blaze_deco),
    "netherite_job_bow": dict(limb=NETH, grip=GOLD, string=hx("#d0c0d0"), tips=GOLD, deco=neth_deco),
    "moon_silver_job_bow": dict(limb=MOON, grip=(hx("#1a3a7a"), hx("#3a7ae0"), hx("#a0d0ff")), string=hx("#ffffff"), tips=MOON, deco=moon_deco),
    "star_silver_job_bow": dict(limb=NIGHT, grip=GOLD, string=hx("#fff0a0"), tips=GOLD, deco=star_deco, arrow=STAR_ARROW),
}

if __name__ == "__main__":
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    rows = []
    for k, kw in BOWS.items():
        row = []
        for i, pull in enumerate((0, 1, 2, 3)):
            im = bow(pull=pull, **kw)
            im.save(os.path.join(out, k + (".png" if pull == 0 else f"_pulling_{pull - 1}.png")))
            row.append(im)
        rows.append(row)
    if "--preview" in sys.argv:
        prev = sys.argv[sys.argv.index("--preview") + 1]
        sheet = Image.new("RGBA", (len(rows) * 170 + 10, 4 * 140 + 10), (40, 44, 52, 255))
        for i, row in enumerate(rows):
            for j, im in enumerate(row):
                sheet.alpha_composite(im.resize((128, 128), Image.NEAREST), (i * 170 + 8, j * 140 + 8))
        sheet.save(prev)
    print(list(BOWS))
