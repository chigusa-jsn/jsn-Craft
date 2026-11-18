"""
魔法陣の絵（地面に重ねて表示する用、透明な背景のPNG）を作る。
    python3 tools/gen_magic_circles.py 出力フォルダ [--preview 出力.png]
"""
import math, os, sys, random
from PIL import Image, ImageDraw, ImageFilter

S = 4          # なめらかにするため、大きく描いてから縮める
N = 256        # 仕上がりの大きさ

def hx(h): h = h.lstrip("#"); return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))

class Pen:
    def __init__(self, color):
        self.size = N * S
        self.c = self.size / 2
        self.img = Image.new("RGBA", (self.size, self.size), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.img)
        self.col = hx(color)

    def p(self, r, ang):
        return (self.c + r * self.size / 2 * math.cos(ang), self.c + r * self.size / 2 * math.sin(ang))

    def circle(self, r, w=0.006, a=255):
        R = r * self.size / 2
        self.d.ellipse([self.c - R, self.c - R, self.c + R, self.c + R], outline=self.col + (a,), width=max(1, int(w * self.size)))

    def line(self, pts, w=0.005, a=255):
        self.d.line(pts, fill=self.col + (a,), width=max(1, int(w * self.size)), joint="curve")

    def poly(self, n, r, rot=0.0, step=1, w=0.005, a=255):
        pts = [self.p(r, rot + 2 * math.pi * (i * step % n) / n) for i in range(n + 1)]
        self.line(pts, w, a)

    def dot(self, r, ang, size=0.012, a=255):
        x, y = self.p(r, ang); R = size * self.size / 2
        self.d.ellipse([x - R, y - R, x + R, y + R], fill=self.col + (a,))

    def ticks(self, r1, r2, n, w=0.004, a=255):
        for i in range(n):
            ang = 2 * math.pi * i / n
            self.line([self.p(r1, ang), self.p(r2, ang)], w, a)

    def glyph_ring(self, r, n, seed, h=0.035, w=0.004, a=255):
        """円にそって、魔法の文字（でたらめな記号）を並べる"""
        rng = random.Random(seed)
        for i in range(n):
            ang = 2 * math.pi * (i + 0.5) / n
            # 文字の中心と、円の向き
            cx, cy = self.p(r, ang)
            tx, ty = -math.sin(ang), math.cos(ang)      # 円にそった向き
            nx, ny = math.cos(ang), math.sin(ang)       # 外向き
            H = h * self.size / 2; W = H * 0.6
            def q(u, v): return (cx + tx * u * W + nx * v * H, cy + ty * u * W + ny * v * H)
            strokes = rng.randint(2, 4)
            for _ in range(strokes):
                kind = rng.random()
                if kind < 0.4:
                    u = rng.uniform(-1, 1); self.line([q(u, -1), q(u, 1)], w, a)
                elif kind < 0.7:
                    v = rng.uniform(-1, 1); self.line([q(-1, v), q(1, v)], w, a)
                else:
                    self.line([q(-1, rng.choice([-1, 1])), q(0, 0), q(1, rng.choice([-1, 1]))], w, a)

    def finish(self):
        img = self.img.resize((N, N), Image.LANCZOS)
        glow = img.filter(ImageFilter.GaussianBlur(3))
        out = Image.new("RGBA", (N, N), (0, 0, 0, 0))
        out.alpha_composite(glow); out.alpha_composite(glow); out.alpha_composite(img)
        return out

# ---------------- 職業・属性ごとの魔法陣 ----------------

def sword():       # 剣士：青い剣の紋章
    p = Pen("#6ab8ff")
    p.circle(0.96, 0.010); p.circle(0.90); p.ticks(0.90, 0.96, 48)
    p.circle(0.62)
    for k in range(4):                                     # 4本の剣
        a = math.pi / 4 + k * math.pi / 2
        tip, base = p.p(0.86, a), p.p(0.18, a)
        side = 0.035
        l, r = p.p(0.62, a + side), p.p(0.62, a - side)
        p.line([base, l, tip, r, base], 0.005)
        p.line([p.p(0.30, a + 0.20), p.p(0.30, a - 0.20)], 0.007)   # つば
    p.poly(4, 0.62, 0); p.circle(0.16, 0.008); p.dot(0, 0, 0.05)
    return p.finish()

def element(color, seed, points, motif=None):   # 魔導士：属性の色で、六芒星と文字の輪。真ん中は属性の模様
    p = Pen(color)
    p.circle(0.97, 0.010); p.circle(0.91); p.glyph_ring(0.94 - 0.005, 36, seed, h=0.022)
    p.circle(0.80); p.ticks(0.78, 0.80, 72, 0.003)
    p.poly(6, 0.78, -math.pi / 2, step=2); p.poly(6, 0.78, math.pi / 2, step=2)   # 六芒星
    p.circle(0.46); p.glyph_ring(0.40, 18, seed + 1, h=0.03)
    p.circle(0.34)
    if motif: motif(p)
    else:
        rng = random.Random(seed)                            # 星座（点と線）
        stars = [(rng.uniform(0.05, 0.30), rng.uniform(0, 2 * math.pi)) for _ in range(points)]
        for i in range(points - 1): p.line([p.p(*stars[i]), p.p(*stars[i + 1])], 0.003, 200)
        for st in stars: p.dot(*st, 0.016)
    for k in range(6): p.dot(0.78, -math.pi / 2 + k * math.pi / 3, 0.022)
    return p.finish()

def flame(p):      # 火：三つの炎
    for k in range(3):
        a = -math.pi / 2 + k * 2 * math.pi / 3
        pts = [p.p(0.04, a + 0.9), p.p(0.16, a + 0.35), p.p(0.30, a), p.p(0.18, a - 0.15), p.p(0.22, a - 0.45), p.p(0.04, a - 0.9)]
        p.line(pts, 0.005)
    p.dot(0, 0, 0.035)

def snow(p):       # 氷：雪の結晶
    for k in range(6):
        a = -math.pi / 2 + k * math.pi / 3
        p.line([p.p(0.02, a), p.p(0.30, a)], 0.005)
        for r in (0.14, 0.22):
            p.line([p.p(r, a), p.p(r + 0.07, a + 0.35)], 0.004); p.line([p.p(r, a), p.p(r + 0.07, a - 0.35)], 0.004)

def bolt(p):       # 雷：真ん中に大きな稲妻ひとつ
    c, R = p.c, p.size / 2
    def q(x, y): return (c + x * R, c + y * R)
    shape = [q(0.06, -0.30), q(-0.14, 0.03), q(-0.01, 0.03), q(-0.08, 0.30), q(0.14, -0.05), q(0.01, -0.05), q(0.06, -0.30)]
    p.line(shape, 0.007)
    for k in range(8):
        a = 2 * math.pi * k / 8
        p.dot(0.30, a, 0.010)

def star5(p):      # 星：五芒星と星座
    p.poly(5, 0.30, -math.pi / 2, step=2, w=0.005)
    rng = random.Random(14)
    stars = [(rng.uniform(0.08, 0.28), rng.uniform(0, 2 * math.pi)) for _ in range(5)]
    for st in stars: p.dot(*st, 0.014)
    p.dot(0, 0, 0.03)

def rune():        # 付与術師：紫の魔紋の文字
    p = Pen("#c890ff")
    p.circle(0.96, 0.012); p.glyph_ring(0.89, 28, 7, h=0.05, w=0.006); p.circle(0.82)
    p.poly(8, 0.82, 0, step=3); p.circle(0.55); p.glyph_ring(0.48, 16, 8, h=0.045, w=0.006); p.circle(0.40)
    p.poly(4, 0.40, math.pi / 4); p.poly(4, 0.40, 0); p.dot(0, 0, 0.06)
    return p.finish()

def alchemy():     # 調合師：緑の錬金術の円
    p = Pen("#7ce08a")
    p.circle(0.96, 0.010); p.circle(0.90); p.ticks(0.90, 0.96, 24, 0.006)
    p.poly(3, 0.90, -math.pi / 2); p.poly(3, 0.90, math.pi / 2)
    p.circle(0.45, 0.008); p.poly(4, 0.45, math.pi / 4); p.circle(0.22)
    for k in range(6):                                       # 六つの小さな円
        x, y = p.p(0.68, -math.pi / 2 + k * math.pi / 3); R = 0.09 * p.size / 2
        p.d.ellipse([x - R, y - R, x + R, y + R], outline=p.col + (255,), width=int(0.005 * p.size))
    p.dot(0, 0, 0.05)
    return p.finish()

def holy():        # 癒し手：金色の光の輪
    p = Pen("#ffd870")
    p.circle(0.96, 0.008); p.circle(0.92, 0.004)
    for k in range(32):                                     # 光の線
        a = 2 * math.pi * k / 32; L = 0.86 if k % 2 == 0 else 0.76
        p.line([p.p(0.50, a), p.p(L, a)], 0.004 if k % 2 else 0.007)
    p.circle(0.50, 0.010); p.circle(0.44)
    for k in range(8):
        a = 2 * math.pi * k / 8
        p.line([p.p(0.10, a), p.p(0.40, a + 0.12), p.p(0.44, a), p.p(0.40, a - 0.12), p.p(0.10, a)], 0.004)
    p.dot(0, 0, 0.08)
    return p.finish()

CIRCLES = {
    "sword": sword,
    "fire": lambda: element("#ff7040", 11, 6, flame),
    "ice": lambda: element("#90e0ff", 12, 7, snow),
    "thunder": lambda: element("#ffe050", 13, 5, bolt),
    "star": lambda: element("#c8a0ff", 14, 8, star5),
    "rune": rune,
    "alchemy": alchemy,
    "holy": holy,
}

def ground_mock(circle, path):
    """草の地面に置いたときのイメージ（ななめから見た図）"""
    W, H = 520, 300
    bg = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    rng = random.Random(3)
    for y in range(H):                     # 夜の草地
        for x in range(0, W, 8):
            g = 40 + int(30 * (y / H)) + rng.randint(-6, 6)
            for k in range(8): bg.putpixel((x + k, y), (int(g * 0.45), g, int(g * 0.35), 255))
    flat = circle.resize((440, 170), Image.LANCZOS)
    bg.alpha_composite(flat, (40, 110))
    bg.save(path)

if __name__ == "__main__":
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    imgs = {}
    for k, f in CIRCLES.items():
        imgs[k] = f(); imgs[k].save(os.path.join(out, k + ".png"))
    if "--preview" in sys.argv:
        prev = sys.argv[sys.argv.index("--preview") + 1]
        cols = 4; rows = (len(imgs) + cols - 1) // cols
        sheet = Image.new("RGBA", (cols * 270, rows * 270), (18, 18, 26, 255))
        for i, im in enumerate(imgs.values()):
            sheet.alpha_composite(im, ((i % cols) * 270 + 7, (i // cols) * 270 + 7))
        sheet.save(prev)
        ground_mock(imgs["fire"], prev.replace(".png", "_ground.png"))
    print(list(imgs))
