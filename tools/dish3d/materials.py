"""
立体料理の素材テクスチャ（32×32）。上の面は「ワールドの位置」で貼られるので、
お皿の円などは中心 (16,16)px＝ブロックの真ん中 として描く。
fit=True で使う素材（チャーシューの断面など）は、その面ぴったりに1枚貼られる。
"""
import numpy as np
from PIL import Image

N = 32
yy, xx = np.mgrid[0:N, 0:N]
R = np.hypot(xx + 0.5 - 16, yy + 0.5 - 16)  # 中心からの距離（px）

def rgb(h):
    h = h.lstrip('#'); return np.array([int(h[i:i+2], 16) for i in (0, 2, 4)], dtype=float)

def noise(seed, cell=4, amp=1.0):
    """なめらかなノイズ（-1〜1）"""
    rng = np.random.default_rng(seed)
    g = rng.uniform(-1, 1, (N // cell + 2, N // cell + 2))
    fx = (xx / cell); fy = (yy / cell)
    x0 = fx.astype(int); y0 = fy.astype(int); tx = fx - x0; ty = fy - y0
    tx = tx * tx * (3 - 2 * tx); ty = ty * ty * (3 - 2 * ty)
    a = g[y0, x0] * (1 - tx) + g[y0, x0 + 1] * tx
    b = g[y0 + 1, x0] * (1 - tx) + g[y0 + 1, x0 + 1] * tx
    return (a * (1 - ty) + b * ty) * amp

def grain(seed, amp):
    return np.random.default_rng(seed).uniform(-amp, amp, (N, N))

def img(arr, alpha=None):
    a = np.clip(arr, 0, 255).astype(np.uint8)
    out = np.dstack([a, np.full((N, N), 255, np.uint8) if alpha is None else alpha.astype(np.uint8)])
    return Image.fromarray(out, "RGBA")

def fill(col, seed=0, n=12, g=6, cell=4):
    base = np.zeros((N, N, 3)) + rgb(col)
    v = noise(seed, cell, n) + grain(seed + 1, g)
    return base + v[..., None]

def shine(arr, pts, col="#ffffff", strength=0.7, r=1.3):
    """ツヤ（明るい点）"""
    for (x, y) in pts:
        d = np.hypot(xx - x, yy - y)
        k = np.clip(1 - d / r, 0, 1)[..., None] * strength
        arr = arr * (1 - k) + rgb(col) * k
    return arr

def blend(arr, mask, col, k=1.0):
    m = (mask.astype(float) * k)[..., None]
    return arr * (1 - m) + rgb(col) * m

M = {}
def mat(f): M[f.__name__] = f; return f

# ---------------- 器 ----------------
@mat
def porcelain_top():
    a = fill("#f6f5f0", 1, 4, 2)
    a = blend(a, (R > 12.0) & (R < 13.2), "#d8dde4", 0.9)     # 縁の段差の影
    a = blend(a, (R >= 13.2), "#fbfbf8", 0.8)                 # 縁
    a = blend(a, R < 9, "#eeede6", 0.4)                       # 真ん中の少しくぼんだ影
    return img(shine(a, [(9, 8), (10, 9)], strength=0.6))
@mat
def porcelain_side():
    a = fill("#f0f0ec", 2, 3, 2)
    a = blend(a, yy > 22, "#c8ccd4", 0.7)
    return img(a)
@mat
def board_top():
    a = fill("#d8ad72", 3, 10, 4, cell=8)
    stripes = (np.sin(yy * 0.9 + noise(4, 8, 3)) > 0.75)
    return img(blend(a, stripes, "#b88a50", 0.6))
@mat
def board_side():
    return img(blend(fill("#b8884e", 5, 8, 4), yy % 8 == 0, "#8a6234", 0.5))
@mat
def bowl_outer():
    a = fill("#f4f2ec", 6, 3, 2)
    band = (yy >= 24) & (yy <= 27)
    a = blend(a, band, "#c62a24", 1.0)
    meander = band & (((xx // 2) + (yy - 24)) % 4 == 0)         # 雷紋っぽい模様
    a = blend(a, meander, "#f4f2ec", 0.9)
    a = blend(a, yy > 29, "#d8d4cc", 0.6)
    return img(a)
@mat
def bowl_inner():
    a = fill("#f2f0ea", 7, 3, 2)
    return img(blend(a, (yy >= 23) & (yy <= 23), "#c62a24", 1.0))
@mat
def bowl_rim():
    return img(fill("#faf8f2", 8, 2, 1))

# ---------------- ごはん・麺・粉もの ----------------
@mat
def rice():
    a = fill("#f2efe6", 9, 6, 3)
    rng = np.random.default_rng(10)
    for _ in range(70):                       # 米粒：細長い明るい粒と、下の影
        x, y = rng.integers(0, N, 2)
        horiz = rng.random() < 0.5
        for k in range(3):
            px, py = ((x + k) % N, y) if horiz else (x, (y + k) % N)
            a[py, px] = rgb("#ffffff")
        sx, sy = ((x + 1) % N, (y + 1) % N)
        a[sy, sx] = rgb("#d6d0c0")
    return img(a)
@mat
def noodles():
    a = fill("#ecd28a", 11, 6, 3)
    wave = np.sin(xx * 0.7 + np.sin(yy * 0.5) * 2) > 0.55
    a = blend(a, wave, "#f8e6a8", 0.9)
    a = blend(a, np.sin(xx * 0.7 + np.sin(yy * 0.5) * 2) < -0.85, "#c8a860", 0.7)
    return img(a)
@mat
def pancake_top():
    a = fill("#c87a30", 12, 8, 4)
    a = blend(a, R > 9.5, "#e8b060", 0.7)
    return img(shine(a, [(11, 11), (19, 13)], "#f0c070", 0.5, 2))
@mat
def pancake_side():
    a = fill("#ecc078", 13, 6, 4)
    a = blend(a, (yy < 4) | (yy > 27), "#b86e28", 0.9)       # 上下の焼き色
    rng = np.random.default_rng(14)
    for _ in range(25):
        x, y = rng.integers(0, N), rng.integers(6, 26)
        a[y, x] = rgb("#d8a058")                                 # 気泡のあと
    return img(a)

# ---------------- 魚・肉・卵 ----------------
@mat
def salmon():
    a = fill("#ff8a52", 15, 8, 3)
    fat = np.sin((xx + yy) * 0.55) > 0.7
    a = blend(a, fat, "#ffe6d0", 0.9)
    return img(shine(a, [(8, 6), (20, 10)], strength=0.55, r=2))
@mat
def tuna():
    a = fill("#c8243a", 16, 10, 4)
    a = blend(a, np.sin((xx - yy) * 0.4) > 0.85, "#e85a6a", 0.6)
    return img(shine(a, [(10, 7), (22, 12)], strength=0.5, r=2))
@mat
def tamago():
    a = fill("#f8d448", 17, 6, 3)
    a = blend(a, yy % 6 == 0, "#e0a830", 0.7)
    return img(blend(a, yy < 2, "#c88a28", 0.8))
@mat
def nori():
    a = fill("#1e2e1e", 18, 8, 6)
    return img(blend(a, np.random.default_rng(19).random((N, N)) > 0.9, "#3a5a32", 0.8))
@mat
def chashu():
    a = fill("#eaa89a", 20, 8, 3)
    a = blend(a, R > 12, "#9a4a2a", 1.0)                     # 外側の焼き目
    a = blend(a, (R > 9) & (R < 12) & (np.sin(xx * yy) > 0.3), "#f8d8cc", 0.5)
    a = blend(a, R > 15, "#7a3a20", 1.0)
    return img(shine(a, [(11, 10)], strength=0.4, r=3))
@mat
def chashu_side():
    return img(blend(fill("#8a4426", 21, 8, 4), (yy > 10) & (yy < 22), "#e8a090", 0.7))
@mat
def ajitama():
    a = fill("#fbf6ea", 22, 3, 2)
    a = blend(a, R > 13.5, "#a8642a", 1.0)                   # 味がしみた外側
    a = blend(a, R < 8, "#ff9c1e", 1.0)                      # 黄身
    a = blend(a, R < 5, "#ffb84a", 0.8)
    return img(shine(a, [(13, 13)], strength=0.7, r=1.5))
@mat
def ajitama_side():
    return img(fill("#b8742e", 23, 8, 3))
@mat
def naruto():
    a = fill("#fbf8f4", 24, 2, 2)
    ang = np.arctan2(yy - 16, xx - 16)
    spiral = np.sin(R * 0.9 - ang * 1.0) > 0.5
    a = blend(a, spiral & (R < 12), "#f070a0", 1.0)
    return img(blend(a, R > 14.5, "#f4c8d8", 1.0))
@mat
def beef():
    a = fill("#7a3a1e", 25, 10, 5)
    return img(shine(a, [(10, 10)], "#b06a3a", 0.5, 3))
@mat
def omelette():
    a = fill("#ffd23c", 26, 8, 3, cell=8)
    a = blend(a, np.random.default_rng(27).random((N, N)) > 0.95, "#e8a828", 0.8)
    return img(shine(a, [(9, 8), (10, 8), (20, 6)], "#fff6c8", 0.75, 2.2))
@mat
def ketchup():
    a = fill("#d4201a", 28, 6, 3)
    return img(shine(a, [(8, 8), (22, 20)], "#ff8070", 0.6, 2))

# ---------------- ソース・スープ ----------------
@mat
def curry():
    a = fill("#8e4a18", 29, 12, 4, cell=6)
    rng = np.random.default_rng(30)
    for _ in range(14):
        x, y = rng.integers(0, N, 2); a[y, x] = rgb("#d88a2a")
    return img(shine(a, [(10, 12), (20, 18), (24, 9)], "#d8945a", 0.6, 2))
@mat
def broth():
    a = fill("#d29640", 31, 10, 3, cell=8)
    rng = np.random.default_rng(32)
    for _ in range(18):                                        # 油の玉
        x, y = rng.integers(2, N - 2, 2)
        a = shine(a, [(x, y)], "#f8d080", 0.8, 1.2)
    return img(a)
@mat
def syrup():
    a = fill("#c46c10", 33, 8, 3)
    return img(shine(a, [(8, 10), (14, 6), (22, 16)], "#ffc060", 0.8, 2))
@mat
def butter():
    a = fill("#fff0a0", 34, 4, 2)
    return img(shine(blend(a, yy > 26, "#e8d070", 0.6), [(10, 8)], strength=0.6, r=3))

# ---------------- 野菜・添え物 ----------------
@mat
def potato():
    return img(shine(fill("#f2d47a", 35, 8, 4), [(10, 10)], strength=0.4, r=3))
@mat
def carrot():
    return img(shine(fill("#f07a1e", 36, 8, 4), [(10, 10)], strength=0.4, r=3))
@mat
def pickles():
    return img(fill("#c8242a", 37, 10, 6))
@mat
def parsley():
    a = fill("#3a8a2a", 38, 14, 8)
    return img(blend(a, np.random.default_rng(39).random((N, N)) > 0.7, "#6ac04a", 0.8))
@mat
def lettuce():
    a = fill("#7ac84a", 40, 10, 5)
    veins = (np.abs(np.sin(xx * 0.5 + yy * 0.2)) < 0.12)
    return img(blend(a, veins, "#c8f0a0", 0.9))
@mat
def tomato():
    return img(shine(fill("#e0281e", 41, 8, 3), [(9, 8)], strength=0.7, r=2.5))
@mat
def negi():
    a = fill("#6ab83a", 42, 8, 4)
    return img(blend(a, R < 6, "#d8f0b0", 0.8))
@mat
def gari():
    return img(blend(fill("#f8c0c0", 43, 6, 3), np.sin(yy * 1.2) > 0.6, "#ffd8d8", 0.8))
@mat
def wasabi():
    return img(fill("#8ac040", 44, 10, 6))
@mat
def blueberry():
    return img(shine(fill("#3a3a9a", 45, 10, 4), [(12, 10)], "#8a8ae0", 0.7, 3))
@mat
def strawberry():
    a = fill("#e0203a", 46, 8, 3)
    rng = np.random.default_rng(47)
    for _ in range(14):
        x, y = rng.integers(0, N, 2); a[y, x] = rgb("#ffe080")
    return img(a)

def build_all(out_dir):
    import os
    os.makedirs(out_dir, exist_ok=True)
    tex = {}
    for name, f in M.items():
        im = f(); im.save(os.path.join(out_dir, name + ".png")); tex[name] = im
    return tex
