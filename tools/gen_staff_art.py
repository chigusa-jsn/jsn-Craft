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
        return out

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

def moon_staff():      # 月銀の杖：月銀の柄、三日月にムーンストーン
    a = Art()
    a.shaft(8, 57, 40, 25, 3.4, *MOON)
    a.arc(46, 18, 12, 120, 380, 4.2, MOON[1]); a.arc(46, 18, 12, 150, 230, 1.6, MOON[2])   # 三日月
    a.ball(47, 17, 5.2, hx("#7a88b0"), hx("#c8d8f8"), hx("#f4f8ff"))
    for (x, y) in ((58, 6), (36, 8), (60, 28)): a.star(x, y, 2.2, 0.8, 4, hx("#eef4ff"), rot=0)
    a.ball(12, 53, 2.2, *MOON)
    return a.finish(glow=(47, 17, 15, hx("#e0e8ff")))

def star_staff():      # 星の杖：星銀の柄、光る星の宝石と輪
    a = Art()
    a.shaft(8, 57, 39, 26, 3.6, *STAR)
    a.ring(47, 17, 12, 1.6, hx("#ffe080")); a.ring(47, 17, 9, 1.0, hx("#fff4c0"))
    a.star(47, 17, 9.5, 4.0, 5, hx("#d0a020")); a.star(46.5, 16.5, 7.5, 3.2, 5, hx("#ffe040")); a.star(46, 16, 3.5, 1.5, 5, hx("#fffbe0"))
    for (x, y, r) in ((60, 5, 2.4), (33, 10, 1.8), (61, 30, 1.8), (36, 31, 1.4)): a.star(x, y, r, r * 0.35, 4, hx("#fff4c0"), rot=0)
    a.poly([(36, 30), (41, 25), (43, 27), (38, 32)], STAR[0])
    a.ball(12, 53, 2.6, hx("#8a5a10"), hx("#e0a830"), hx("#fff0a0"))
    return a.finish(glow=(47, 17, 17, hx("#fff0a0")))

STAFFS = {"wood_staff": wood_staff, "crystal_staff": crystal_staff, "orb_staff": orb_staff,
          "moon_staff": moon_staff, "star_staff": star_staff}

if __name__ == "__main__":
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    imgs = []
    for k, f in STAFFS.items():
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
    print(list(STAFFS))
