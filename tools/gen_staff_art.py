"""
魔導士の杖のアイコン（64×64）を作る。左下が持ち手、右上が飾り（マイクラの道具と同じ向き）。
    python3 tools/gen_staff_art.py 出力フォルダ [--preview 出力.png]
"""
import math, os, sys
from PIL import Image, ImageDraw, ImageFilter

N = 64
S = 8   # 大きく描いてから縮める

def hx(h): h = h.lstrip("#"); return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))
def mix(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

class Art:
    def __init__(self):
        self.img = Image.new("RGBA", (N * S, N * S), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.img)

    def P(self, x, y): return (x * S, y * S)

    def shaft(self, x1, y1, x2, y2, w, dark, mid, light):
        """斜めの柄。影・本体・光の3本で立体に見せる"""
        dx, dy = x2 - x1, y2 - y1
        L = math.hypot(dx, dy); nx, ny = -dy / L, dx / L
        for off, col, ww in ((w * 0.30, dark, w), (0, mid, w * 0.75), (-w * 0.22, light, w * 0.28)):
            self.d.line([self.P(x1 + nx * off, y1 + ny * off), self.P(x2 + nx * off, y2 + ny * off)], fill=col + (255,), width=int(ww * S))

    def ball(self, cx, cy, r, dark, mid, light, white=(255, 255, 255)):
        self.d.ellipse([*self.P(cx - r, cy - r), *self.P(cx + r, cy + r)], fill=dark + (255,))
        r2 = r * 0.82
        self.d.ellipse([*self.P(cx - r2 - r * 0.08, cy - r2 - r * 0.08), *self.P(cx + r2 - r * 0.08, cy + r2 - r * 0.08)], fill=mid + (255,))
        r3 = r * 0.40
        self.d.ellipse([*self.P(cx - r3 - r * 0.32, cy - r3 - r * 0.32), *self.P(cx + r3 - r * 0.32, cy + r3 - r * 0.32)], fill=light + (255,))
        r4 = r * 0.14
        self.d.ellipse([*self.P(cx - r4 - r * 0.42, cy - r4 - r * 0.42), *self.P(cx + r4 - r * 0.42, cy + r4 - r * 0.42)], fill=white + (255,))

    def poly(self, pts, col):
        self.d.polygon([self.P(x, y) for x, y in pts], fill=col + (255,))

    def ring(self, cx, cy, r, w, col):
        self.d.ellipse([*self.P(cx - r, cy - r), *self.P(cx + r, cy + r)], outline=col + (255,), width=int(w * S))

    def arc(self, cx, cy, r, a1, a2, w, col):
        self.d.arc([*self.P(cx - r, cy - r), *self.P(cx + r, cy + r)], a1, a2, fill=col + (255,), width=int(w * S))

    def star(self, cx, cy, r1, r2, n, col, rot=-90):
        pts = []
        for i in range(n * 2):
            r = r1 if i % 2 == 0 else r2
            a = math.radians(rot + 180 * i / n)
            pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
        self.poly(pts, col)

    def finish(self, glow=None):
        img = self.img.resize((N, N), Image.LANCZOS)
        # ふちをくっきり（半透明を減らす）
        px = img.load()
        for y in range(N):
            for x in range(N):
                r, g, b, a = px[x, y]
                px[x, y] = (r, g, b, 255 if a >= 110 else 0)
        # 暗いふち取り
        out = Image.new("RGBA", (N, N), (0, 0, 0, 0))
        op = out.load()
        for y in range(N):
            for x in range(N):
                if px[x, y][3]: continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    xx, yy = x + dx, y + dy
                    if 0 <= xx < N and 0 <= yy < N and px[xx, yy][3]:
                        op[x, y] = (24, 18, 30, 255); break
        out.alpha_composite(img)
        if glow:   # 宝石のまわりの光（うすい点）
            gx, gy, gr, gc = glow
            gp = out.load()
            for i in range(10):   # まわりに小さなきらめき（十字）
                ang = math.radians(i * 36 + 18)
                rr = gr * (0.95 if i % 2 == 0 else 0.78)
                x, y = int(round(gx + rr * math.cos(ang))), int(round(gy + rr * math.sin(ang)))
                pts = [(x, y)] + ([(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)] if i % 2 == 0 else [])
                for (px2, py2) in pts:
                    if 0 <= px2 < N and 0 <= py2 < N and gp[px2, py2][3] == 0:
                        gp[px2, py2] = (gc if (px2, py2) != (x, y) else (255, 255, 255)) + (255,)
        # 本体から離れた点（きらめき・星座）は、手に持つと四角いブロックが浮いて見えるので消す
        from clean_floating import drop_floating
        return drop_floating(out)

WOOD = (hx("#4a2c16"), hx("#7a4a26"), hx("#a8723e"))
IRON = (hx("#5a5a68"), hx("#9a9aaa"), hx("#e0e0ea"))
GOLD = (hx("#8a5a10"), hx("#e0a830"), hx("#fff0a0"))
MOON = (hx("#5a6a90"), hx("#a8b8e0"), hx("#eef4ff"))
STAR = (hx("#8a7a50"), hx("#e8dcb0"), hx("#fffaf0"))

def wood_staff():      # 木の杖：枝分かれした先にクォーツ
    a = Art()
    a.shaft(8, 57, 44, 21, 4.2, *WOOD)
    a.shaft(44, 21, 50, 10, 3.0, *WOOD); a.shaft(44, 21, 55, 18, 2.6, *WOOD)   # 枝
    a.poly([(47, 14), (52, 9), (56, 13), (51, 18)], hx("#e8e0d8")); a.poly([(48, 14), (52, 10), (53, 12), (49, 15)], hx("#ffffff"))
    a.ball(14, 51, 2.6, *WOOD)   # 握りのこぶ
    return a.finish()

def crystal_staff():   # 水晶の杖：鉄の柄、ひし形の水晶を爪で留める
    a = Art()
    a.shaft(8, 57, 42, 23, 3.6, *IRON)
    for t in (0.2, 0.5):   # 革の巻き
        x, y = 8 + 34 * t, 57 - 34 * t
        a.shaft(x - 2, y + 2, x + 2, y - 2, 4.8, hx("#3a2418"), hx("#6a4428"), hx("#8a6038"))
    a.poly([(49, 6), (57, 16), (49, 27), (41, 16)], hx("#6aa8d8")); a.poly([(49, 6), (57, 16), (49, 17)], hx("#b8e8ff")); a.poly([(49, 8), (52, 13), (49, 15)], hx("#ffffff"))
    a.shaft(40, 25, 44, 21, 3, *IRON); a.shaft(38, 18, 42, 22, 1.6, *IRON); a.shaft(44, 26, 46, 24, 1.6, *IRON)
    return a.finish(glow=(49, 16, 14, hx("#b8e8ff")))

def orb_staff():       # 宝玉の杖：金の柄、輪の中にサファイアの玉
    a = Art()
    a.shaft(8, 57, 40, 25, 3.8, *GOLD)
    a.ring(47, 17, 10.5, 2.4, GOLD[1]); a.arc(47, 17, 10.5, 200, 290, 1.2, GOLD[2])
    a.ball(47, 17, 6.5, hx("#102a80"), hx("#2050e0"), hx("#80b0ff"))
    a.poly([(38, 27), (43, 22), (45, 24), (40, 29)], GOLD[0])
    a.ball(12, 53, 2.4, *GOLD)
    return a.finish(glow=(47, 17, 16, hx("#a0c8ff")))

NETH = (hx("#1e1a1e"), hx("#4a424a"), hx("#7a6e78"))

def netherite_staff(): # ネザライトの杖：黒い柄、とげの冠に赤く光る石
    a = Art()
    a.shaft(8, 57, 40, 25, 3.8, *NETH)
    for t in (0.25, 0.55):   # 金の帯
        x, y = 8 + 32 * t, 57 - 32 * t
        a.shaft(x - 1.5, y + 1.5, x + 1.5, y - 1.5, 5.0, hx("#8a5a10"), hx("#e0a830"), hx("#fff0a0"))
    for ang in (-150, -100, -45, 10, 60):   # とげの冠
        r = math.radians(ang)
        a.poly([(47 + 6 * math.cos(r - 0.35), 17 + 6 * math.sin(r - 0.35)), (47 + 13 * math.cos(r), 17 + 13 * math.sin(r)),
                (47 + 6 * math.cos(r + 0.35), 17 + 6 * math.sin(r + 0.35))], NETH[1])
    a.ball(47, 17, 7.2, *NETH)
    a.ball(47, 17, 5.2, hx("#7a1010"), hx("#e02a20"), hx("#ff9a60"), white=hx("#fff0c0"))
    a.ball(12, 53, 2.6, *NETH)
    return a.finish(glow=(47, 17, 15, hx("#ff6040")))

def moon_staff():      # 月銀の杖：月銀の柄、三日月にムーンストーン
    a = Art()
    a.shaft(8, 57, 40, 25, 3.4, *MOON)
    a.arc(46, 18, 12, 120, 380, 4.2, MOON[1]); a.arc(46, 18, 12, 150, 230, 1.6, MOON[2])   # 三日月
    a.ball(47, 17, 5.2, hx("#7a88b0"), hx("#c8d8f8"), hx("#f4f8ff"))
    for (x, y) in ((58, 6), (36, 8), (60, 28)): a.star(x, y, 2.2, 0.8, 4, hx("#eef4ff"), rot=0)
    a.ball(12, 53, 2.2, *MOON)
    return a.finish(glow=(47, 17, 15, hx("#e0e8ff")))

NIGHT = (hx("#141030"), hx("#2a2460"), hx("#4a44a0"))

def night_shaft(a, x1, y1, x2, y2, bands=(0.1, 0.22, 0.34)):
    """星銀シリーズの柄：夜空色に金の帯"""
    a.shaft(x1, y1, x2, y2, 3.8, *NIGHT)
    for t in bands:
        x, y = x1 + (x2 - x1) * t, y1 + (y2 - y1) * t
        a.shaft(x - 1.4, y + 1.4, x + 1.4, y - 1.4, 5.2, *GOLD)

def constellation(a, pts, col=None):
    col = col or hx("#8aa8ff")
    for i in range(len(pts) - 1):
        a.d.line([a.P(*pts[i]), a.P(*pts[i + 1])], fill=col + (255,), width=int(0.8 * 8))
    for q in pts: a.ball(q[0], q[1], 0.9, hx("#6a88ff"), hx("#c0d8ff"), hx("#ffffff"))

def star_staff():      # 星銀の杖：夜空色の柄、金の三日月と星の輪、真ん中に大きな星、輪に星座
    a = Art()
    night_shaft(a, 8, 57, 40, 25)
    cx, cy = 46, 18
    a.ring(cx, cy, 13, 2.2, GOLD[1]); a.ring(cx, cy, 13, 0.8, GOLD[2])
    a.ring(cx, cy, 10, 1.0, hx("#c0d8ff"))
    constellation(a, [(cx - 9, cy - 3), (cx - 5, cy - 9), (cx + 2, cy - 10), (cx + 8, cy - 6)])
    # 下の金の三日月（輪を支える）
    a.arc(cx, cy, 15, 100, 200, 3.0, GOLD[1]); a.arc(cx, cy, 15, 110, 190, 1.0, GOLD[2])
    a.star(cx, cy, 9.0, 3.6, 5, GOLD[0]); a.star(cx, cy, 7.6, 3.0, 5, hx("#ffe040")); a.star(cx - 0.5, cy - 0.5, 3.2, 1.3, 5, hx("#fffbe0"))
    a.poly([(37, 28), (42, 23), (44, 25), (39, 30)], GOLD[1])
    a.star(9.5, 55.5, 3.6, 1.5, 5, GOLD[0]); a.star(9.5, 55.5, 2.8, 1.1, 5, hx("#ffe040"))
    for (x, y, r) in ((61, 4, 2.4), (30, 8, 1.8), (62, 32, 1.8), (32, 36, 1.4)): a.star(x, y, r, r * 0.35, 4, hx("#fff4c0"), rot=0)
    return a.finish(glow=(cx, cy, 19, hx("#fff0a0")))

def template(plate, mid, light, gem):
    """鍛冶型：斜めの板に、武器の形が彫ってある"""
    a = Art()
    pts = [(14, 6), (52, 10), (50, 58), (12, 54)]
    a.poly([(x + 1.5, y + 1.5) for x, y in pts], plate[0])
    a.poly(pts, plate[1])
    a.poly([(16, 9), (49, 12), (48, 22), (15, 19)], plate[2])   # 上の光
    # 彫り（杖と剣が交差）
    a.d.line([a.P(19, 49), a.P(44, 17)], fill=plate[0] + (255,), width=3 * S)
    a.d.line([a.P(19, 17), a.P(44, 49)], fill=plate[0] + (255,), width=3 * S)
    a.d.line([a.P(20, 48), a.P(45, 16)], fill=mid + (255,), width=2 * S)
    a.d.line([a.P(20, 16), a.P(45, 48)], fill=mid + (255,), width=2 * S)
    a.ball(32, 32, 5.0, *gem)
    for x, y in ((17, 10), (47, 13), (45, 53), (16, 50)):
        a.ball(x, y, 1.6, plate[0], light, light)
    return a.finish()

TEMPLATES = {
    "job_template_2": lambda: template((hx("#4a4a54"), hx("#8a8a96"), hx("#b4b4c0")), hx("#d8d8e0"), hx("#ffffff"), (hx("#6a6a74"), hx("#c0c0cc"), hx("#ffffff"))),
    "job_template_3": lambda: template((hx("#6a4a10"), hx("#c0902a"), hx("#e8c050")), hx("#fff0a0"), hx("#fff8d0"), (hx("#106a6a"), hx("#30d0d0"), hx("#c0ffff"))),
    "moon_silver_template": lambda: template((hx("#3a4670"), hx("#7a8ab8"), hx("#a8b8e0")), hx("#eef4ff"), hx("#ffffff"), (hx("#7a88b0"), hx("#c8d8f8"), hx("#f4f8ff"))),
    "star_silver_template": lambda: template((hx("#3a2a50"), hx("#6a5090"), hx("#8a70b0")), hx("#ffe080"), hx("#fff4c0"), (hx("#c08010"), hx("#ffe040"), hx("#fffbe0"))),
}

STAFFS = {"wood_staff": wood_staff, "crystal_staff": crystal_staff, "orb_staff": orb_staff,
          "netherite_staff": netherite_staff, "moon_silver_staff": moon_staff, "star_silver_staff": star_staff}

if __name__ == "__main__":
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    imgs = []
    for k, f in {**STAFFS, **TEMPLATES}.items():
        im = f(); im.save(os.path.join(out, k + ".png")); imgs.append(im)
    if "--preview" in sys.argv:
        prev = sys.argv[sys.argv.index("--preview") + 1]
        sheet = Image.new("RGBA", (len(imgs) * 200 + 10, 420), (40, 44, 52, 255))
        for i, im in enumerate(imgs):
            big = im.resize((192, 192), Image.NEAREST); sheet.alpha_composite(big, (i * 200 + 8, 8))
            # ゲームの持ち物と同じくらいの大きさ（32px）でも
            small = im.resize((48, 48), Image.NEAREST); sheet.alpha_composite(small, (i * 200 + 80, 240))
            vanilla16 = im.resize((16, 16), Image.LANCZOS).resize((48, 48), Image.NEAREST); sheet.alpha_composite(vanilla16, (i * 200 + 80, 320))
        sheet.save(prev)
    print(list(STAFFS) + list(TEMPLATES))
