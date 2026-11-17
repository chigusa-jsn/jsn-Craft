"""
立体料理の素材テクスチャ（32×32）。上の面は「ワールドの位置」で貼られるので、
お皿の円などは中心 (16,16)px＝ブロックの真ん中 として描く。
fit=True で使う素材（チャーシューの断面など）は、その面ぴったりに1枚貼られる。
"""
import numpy as np
import zlib
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

# ======================== 追加の素材（45品用） ========================

def simple(name, col, n=8, g=4, cell=4, pts=(), shine_col="#ffffff", strength=0.5, r=2.0, seed=None):
    """単色＋ノイズ＋ツヤの素材をまとめて登録する"""
    s = seed if seed is not None else zlib.crc32(name.encode()) % 10000
    def f():
        return img(shine(fill(col, s, n, g, cell), list(pts), shine_col, strength, r))
    f.__name__ = name
    M[name] = f

def plate_top_of(name, base, rim, edge=None):
    def f():
        a = fill(base, zlib.crc32(name.encode()) % 999, 4, 2)
        a = blend(a, (R > 12.0) & (R < 13.2), rim, 0.9)
        a = blend(a, R >= 13.2, edge or base, 0.8)
        return img(shine(a, [(9, 8)], strength=0.4))
    f.__name__ = name; M[name] = f

def side_of(name, base, band=None, dark_bottom="#00000000"):
    def f():
        a = fill(base, zlib.crc32(name.encode()) % 999, 4, 3)
        if band: a = blend(a, (yy >= 26) & (yy <= 27), band, 1.0)
        return img(blend(a, yy > 29, base, 0.0))
    f.__name__ = name; M[name] = f

# 器
plate_top_of("navy_plate_top", "#24345e", "#1a2648", "#3a4a7a")
plate_top_of("gold_plate_top", "#e8c040", "#b88a10", "#ffe080")
plate_top_of("ice_plate_top", "#c8ecfa", "#90c8e8", "#e8f8ff")
plate_top_of("black_plate_top", "#2a2a2e", "#18181c", "#3a3a40")
plate_top_of("clay_plate_top", "#c8784a", "#a85a30", "#d8906a")
side_of("navy_side", "#1e2c50"); side_of("gold_side", "#c89a20"); side_of("ice_side", "#a8d8f0")
side_of("black_side", "#222226"); side_of("clay_side", "#a85a30")
simple("lacquer_black", "#1c1414", 4, 2, pts=[(8, 6)], strength=0.25)
simple("lacquer_red", "#9a1a14", 6, 3, pts=[(8, 6)], strength=0.3)
simple("lacquer_gold", "#d8a830", 6, 3)
simple("iron", "#3a3a40", 8, 5)
simple("iron_handle", "#2a2a2e", 6, 4)
simple("glass", "#d8eef6", 4, 2, pts=[(6, 6), (7, 14)], strength=0.8, r=1.5)
simple("paper", "#f4efe0", 4, 3)
@mat
def newspaper():
    a = fill("#ece6d4", 60, 4, 3)
    return img(blend(a, (yy % 3 == 0) & (xx % 8 < 6), "#8a8478", 0.7))
simple("bamboo_leaf", "#3a8a3a", 10, 6)
simple("tray_wood", "#a8743a", 8, 4, cell=8)
simple("donabe", "#5a3a2a", 8, 4)
simple("donabe_glaze", "#7a5a3a", 8, 4, pts=[(8, 8)], strength=0.3)

# ごはん・パン・麺
@mat
def toast_top():
    a = fill("#e8c48a", 61, 6, 4)
    a = blend(a, (xx < 2) | (xx > 29) | (yy < 2) | (yy > 29), "#a8642a", 1.0)
    return img(a)
simple("toast_side", "#b8743a", 8, 4)
simple("bread_bun", "#d8904a", 8, 3, pts=[(10, 8), (20, 10)], shine_col="#f0c080", strength=0.5)
@mat
def sesame_bun():
    a = fill("#d8904a", 62, 8, 3)
    rng = np.random.default_rng(63)
    for _ in range(12):
        x, y = rng.integers(2, N - 2, 2); a[y, x] = rgb("#fff4d8")
    return img(shine(a, [(10, 8)], "#f0c080", 0.5, 2))
@mat
def onigiri_rice():
    a = np.array(np.asarray(M["rice"]())[..., :3], dtype=float)
    return img(a)
simple("yaki_rice", "#c88a3a", 12, 6, pts=[(10, 10)], shine_col="#e8b060", strength=0.6)
simple("chawan_tea", "#b8b070", 8, 3, pts=[(12, 12)], strength=0.4)
simple("yellow_rice", "#f0c838", 10, 5)
simple("fries", "#f8d860", 8, 4, pts=[(8, 8)], shine_col="#fff4b0", strength=0.5)
simple("batter", "#f0c860", 14, 8, pts=[(9, 9), (20, 18)], shine_col="#fff0a0", strength=0.5)
simple("pizza_crust", "#d8964a", 8, 4)
@mat
def pizza_top():
    a = fill("#f4c84a", 64, 8, 4)
    a = blend(a, R > 12.5, "#c87a30", 1.0)                       # 耳
    a = blend(a, (R > 11.5) & (R <= 12.5), "#d84a2a", 0.6)
    rng = np.random.default_rng(65)
    for _ in range(8):                                            # サラミ
        x, y = rng.integers(8, 24, 2); a = blend(a, np.hypot(xx - x, yy - y) < 2.2, "#b8282a", 1.0)
    for _ in range(10):
        x, y = rng.integers(6, 26, 2); a[y, x] = rgb("#3a8a2a")
    for _ in range(14):
        x, y = rng.integers(6, 26, 2); a[y, x] = rgb("#fff4c0")
    return img(a)
@mat
def tart_top():
    a = fill("#f8e8b8", 66, 4, 3)
    a = blend(a, R > 11, "#c88a40", 1.0)
    return img(a)
simple("tart_side", "#c88a40", 8, 4)
simple("custard", "#f8e090", 6, 3)
simple("cheese_melt", "#f8d850", 8, 3, pts=[(8, 8), (20, 14)], shine_col="#fff8c0", strength=0.6)
@mat
def gratin_top():
    a = fill("#f0c850", 67, 10, 4, cell=6)
    rng = np.random.default_rng(68)
    for _ in range(12):
        x, y = rng.integers(2, N - 2, 2); a = blend(a, np.hypot(xx - x, yy - y) < 1.8, "#b8702a", 0.9)
    return img(a)
simple("bread_cube", "#e0a860", 8, 4)

# 汁・ソース
simple("miso_broth", "#b8803a", 10, 4, cell=8, pts=[(9, 9), (22, 18)], shine_col="#e8b878", strength=0.4)
simple("veg_soup", "#e0782a", 10, 4, cell=8, pts=[(10, 10)], shine_col="#f8b060", strength=0.5)
simple("stew", "#6a3214", 10, 5, cell=6, pts=[(10, 12), (22, 18)], shine_col="#a8643a", strength=0.6)
simple("tomato_sauce_m", "#c8301e", 10, 4, pts=[(10, 10), (20, 20)], shine_col="#ff7a60", strength=0.5)
simple("mapo", "#b8281a", 12, 5, pts=[(8, 8), (20, 22)], shine_col="#ff6a40", strength=0.6)
simple("crimson_sauce", "#d0180c", 12, 5, pts=[(10, 10)], shine_col="#ffb040", strength=0.7)
simple("teriyaki", "#7a3a14", 10, 4, pts=[(10, 8), (20, 14)], shine_col="#d8904a", strength=0.7)
simple("honey", "#f0a820", 6, 3, pts=[(9, 9), (20, 16)], shine_col="#fff0a0", strength=0.8)
simple("lemonade_liquid", "#fff080", 6, 3, pts=[(8, 8)], strength=0.5)
simple("grape_jelly_m", "#7a2a9a", 6, 3, pts=[(9, 8), (20, 14)], shine_col="#d8a0ff", strength=0.7)
simple("void_jelly_m", "#3a1a6a", 6, 3, pts=[(9, 8), (16, 20)], shine_col="#e0a0ff", strength=0.8)
simple("hotpot_broth", "#d8b878", 8, 3, cell=8, pts=[(10, 10)], strength=0.4)
simple("soul_broth", "#1a3a4a", 8, 3, cell=8, pts=[(10, 10), (20, 20)], shine_col="#60e0ff", strength=0.7)
simple("dipping_sauce", "#4a2a14", 6, 3, pts=[(10, 10)], strength=0.4)

# 魚・肉・卵
@mat
def grilled_fish():
    a = fill("#c0a080", 69, 10, 6)
    a = blend(a, (xx % 6 == 0), "#5a3a20", 0.7)                     # 焼き目
    return img(blend(a, yy > 24, "#f0e8d8", 0.8))
simple("fish_skin_silver", "#a8b4c0", 8, 4, pts=[(10, 8)], strength=0.6)
@mat
def sashimi_red():
    return M["tuna"]()
@mat
def toro():
    a = fill("#f08a96", 70, 6, 3)
    a = blend(a, np.sin((xx + yy) * 0.8) > 0.6, "#ffe0e4", 0.9)
    return img(shine(a, [(10, 8)], strength=0.6, r=2))
@mat
def glacier_salmon_m():
    a = fill("#ffb0a0", 71, 6, 3)
    a = blend(a, np.sin((xx + yy) * 0.6) > 0.6, "#fff0ec", 0.9)
    return img(shine(a, [(10, 8)], strength=0.6, r=2))
simple("white_fish", "#f4ece4", 6, 3, pts=[(10, 10)], strength=0.5)
@mat
def steak_top():
    a = fill("#7a3418", 72, 8, 4)
    grill = (np.abs(((xx + yy) % 8) - 4) < 1)
    return img(shine(blend(a, grill, "#2a1008", 0.9), [(10, 10)], "#c07a4a", 0.5, 3))
simple("steak_side", "#5a2410", 8, 4)
simple("patty", "#5a2a14", 10, 6)
simple("chicken", "#d8a050", 10, 5, pts=[(10, 8)], shine_col="#f8d090", strength=0.6)
simple("shrimp", "#f07a4a", 8, 4, pts=[(10, 8)], strength=0.4)
simple("mussel", "#1a1a2a", 6, 4, pts=[(10, 8)], shine_col="#6a6a9a", strength=0.6)
simple("eel_glaze", "#6a3010", 8, 4, pts=[(8, 8), (22, 10)], shine_col="#e0904a", strength=0.8)
simple("golden_fish", "#e8a818", 10, 4, pts=[(10, 8)], shine_col="#fff0a0", strength=0.8)
simple("coela_fish", "#4a5a7a", 10, 5)
simple("soul_meat", "#3a2418", 10, 5)
simple("dragon_fish", "#c0200c", 10, 5, pts=[(10, 8)], shine_col="#ff9a40", strength=0.7)
simple("ender_fish", "#2a1438", 10, 5, pts=[(10, 8)], shine_col="#c060ff", strength=0.7)
simple("storm_steak", "#3a1a10", 8, 4, pts=[(10, 10)], shine_col="#ffe040", strength=0.4)
simple("egg_yolk", "#ffa820", 6, 3, pts=[(12, 12)], strength=0.6)
simple("tofu", "#faf6ec", 4, 2)
simple("bento_meat", "#8a4a24", 10, 5, pts=[(10, 8)], shine_col="#c08050", strength=0.5)
simple("cheese_m", "#f8d040", 6, 3)

# 野菜・果物・飾り
simple("edamame_pod", "#7ab040", 10, 5, pts=[(10, 8)], strength=0.4)
simple("mushroom", "#a8784a", 8, 4)
simple("mushroom_stem", "#f0e4cc", 4, 3)
simple("onion_ring", "#f4ecd8", 6, 3)
simple("wakame", "#2a5a2a", 8, 5)
simple("shiso", "#3a9a3a", 10, 6)
simple("daikon", "#f8f8f4", 4, 2)
simple("lemon_m", "#ffe23a", 6, 3, pts=[(10, 8)], strength=0.5)
@mat
def lemon_slice():
    a = fill("#fff6a0", 73, 4, 2)
    a = blend(a, R > 13, "#ffd820", 1.0)
    ang = np.arctan2(yy - 16, xx - 16)
    return img(blend(a, (np.abs(np.sin(ang * 4)) < 0.15) & (R < 13), "#fffce0", 1.0))
simple("grape_m", "#6a2a8a", 8, 4, pts=[(10, 10)], shine_col="#c890e8", strength=0.7)
simple("ume_m", "#d83a3a", 8, 4, pts=[(10, 10)], strength=0.6)
simple("takuan", "#f8d838", 6, 3)
simple("red_ginger", "#e83a4a", 8, 4)
simple("coal_bit", "#2a2a2a", 8, 6)
simple("golden_carrot_m", "#f8c020", 8, 4, pts=[(10, 8)], shine_col="#fff4a0", strength=0.8)
simple("melon_m", "#f0d040", 8, 4, pts=[(8, 8)], strength=0.7)
simple("golden_apple_m", "#ffd020", 6, 3, pts=[(10, 10)], strength=0.9)
simple("ice", "#d8f4ff", 4, 3, pts=[(8, 8), (20, 20)], strength=0.8)
simple("lightning", "#ffe838", 4, 2, pts=[(16, 16)], strength=0.6)
simple("flame", "#ff7a18", 10, 5, pts=[(16, 10)], shine_col="#ffe060", strength=0.8, r=4)
simple("soul_flame", "#30c8e8", 10, 5, pts=[(16, 10)], shine_col="#c0ffff", strength=0.8, r=4)
simple("glow_cyan", "#60f0ff", 4, 2)
simple("glow_purple", "#d070ff", 4, 2)
simple("moon_glow", "#fff4a0", 4, 2, pts=[(16, 16)], strength=0.5, r=6)
simple("crown_gold", "#ffd040", 6, 3, pts=[(10, 10)], strength=0.7)
simple("straw_red", "#e83a3a", 4, 2)
simple("chorus_m", "#9a6a9a", 8, 4)
simple("chili_m", "#d8201a", 8, 4, pts=[(10, 8)], strength=0.6)
simple("fork", "#c8ccd4", 4, 2)
simple("chopsticks", "#8a5a30", 4, 2)
simple("stick", "#a8844a", 4, 2)

# ======================== お酒 ========================

def bubbles(a, seed, col="#ffffff", n=10, k=0.6):
    rng = np.random.default_rng(seed)
    for _ in range(n):
        x, y = rng.integers(1, N - 1, 2)
        a = shine(a, [(x, y)], col, k, 1.0)
    return a

def liquid(name, col, bub=0, glow=None):
    def f():
        a = fill(col, zlib.crc32(name.encode()) % 999, 6, 3, cell=8)
        if bub: a = bubbles(a, 5, n=bub)
        if glow: a = shine(a, [(10, 10), (22, 18)], glow, 0.7, 3)
        return img(shine(a, [(9, 8)], strength=0.5, r=2))
    f.__name__ = name; M[name] = f

liquid("beer_liquid", "#e8a020", bub=12)
liquid("mead_liquid", "#d88a18", bub=3)
liquid("wine_liquid", "#7a0e24")
liquid("sake_liquid", "#f4f0dc")
liquid("shochu_liquid", "#eef4f4", bub=2)
liquid("rum_liquid", "#8a4210")
liquid("umeshu_liquid", "#e0a830")
liquid("whisky_liquid", "#c06a14")
liquid("soda_liquid", "#e0f4ff", bub=16)
liquid("highball_liquid", "#f0c060", bub=14)
liquid("sour_liquid", "#fff4b0", bub=10)
liquid("sangria_liquid", "#a01830")
liquid("mojito_liquid", "#d8f4d0", bub=8)
liquid("nether_liquid", "#e83a0c", glow="#ffd040")
liquid("ender_liquid", "#6a1aa8", glow="#e0a0ff")

@mat
def foam():
    a = fill("#fbf6ea", 80, 4, 3)
    return img(bubbles(a, 81, "#ffffff", 14, 0.5))

TINTS = {}
def tinted(liq_name, liq_hex, liquid_y):
    """液体が入ったグラスの側面：液面より下は液体の色が透けて見える"""
    name = f"glass_{liq_name}_{int(liquid_y * 10)}"
    if name in M: return name
    def f():
        a = fill("#e4f2f8", zlib.crc32(name.encode()) % 999, 3, 2)
        line = int((16 - liquid_y) * 2)
        below = yy >= line
        a = blend(a, below, liq_hex, 0.75)
        a = blend(a, yy == line, "#ffffff", 0.6)
        a = blend(a, (xx % 16 == 3), "#ffffff", 0.45)          # 光の筋
        return img(a)
    f.__name__ = name; M[name] = f
    return name

simple("tankard_wood", "#8a5a2c", 8, 5, cell=8)
@mat
def tankard_band():
    a = fill("#8a5a2c", 82, 8, 5)
    return img(blend(a, ((yy >= 26) & (yy <= 27)) | ((yy >= 8) & (yy <= 9)), "#4a4a50", 1.0))
@mat
def tokkuri():
    a = fill("#f2f0ea", 83, 3, 2)
    return img(blend(a, (np.sin(xx * 0.8) > 0.5) & (yy > 14) & (yy < 22), "#2a4a9a", 0.9))
simple("bottle_green", "#1e4a2a", 6, 3, pts=[(6, 10)], strength=0.6, r=1.5)
simple("bottle_brown", "#5a2a0e", 6, 3, pts=[(6, 10)], strength=0.6, r=1.5)
simple("bottle_clear", "#cce8f4", 4, 2, pts=[(6, 10)], strength=0.7, r=1.5)
simple("cork", "#b8884a", 8, 5)
simple("cap_red", "#d02a2a", 4, 2)
@mat
def label():
    a = fill("#f4ecd4", 84, 3, 2)
    a = blend(a, (yy >= 12) & (yy <= 14), "#a02020", 1.0)
    return img(blend(a, (yy >= 18) & (yy <= 19) & (xx > 6) & (xx < 26), "#3a2a1a", 0.8))
simple("mint", "#3ab048", 10, 6)
simple("orange_slice", "#ff9a2a", 6, 3)

# ---- 飲み物（追加16種）----
liquid("milk_liquid", "#f8f6f0")
liquid("shake_liquid", "#fff0c8")
liquid("cocoa_liquid", "#6a3a1e")
liquid("melon_juice", "#ff5a6a", bub=4)
liquid("smoothie_liquid", "#c84a8a")
liquid("amazake_liquid", "#f4f0e4")
liquid("greentea_liquid", "#a8c060")
liquid("coffee_liquid", "#3a2010")
liquid("matcha_liquid", "#7ab040", bub=8)
liquid("hotwine_liquid", "#8a1a24")
liquid("cider_liquid", "#f0d070", bub=10)
liquid("honeylemon_liquid", "#f8d878", bub=10)
liquid("berryorange_liquid", "#f07a3a", bub=6)
liquid("blue_liquid", "#3aa8f0", bub=6)
liquid("martini_liquid", "#f0f6e8")
simple("cream", "#fffaf2", 4, 2, pts=[(10, 8)], strength=0.4)
simple("marshmallow", "#fff4f8", 4, 2)
simple("melon_rind", "#2a8a3a", 8, 4)
simple("yunomi", "#8a9a7a", 6, 3)
simple("chawan", "#4a3020", 8, 4, pts=[(10, 8)], strength=0.3)
simple("chasen", "#e0c890", 6, 3)
simple("umbrella", "#ff6aa8", 4, 2)
simple("pineapple", "#ffd040", 8, 4)
simple("apple_m", "#d8202a", 6, 3, pts=[(10, 8)], strength=0.6)
simple("cinnamon", "#8a4a20", 6, 3)
simple("salt_rim", "#f8f8f8", 6, 4)

# ---- 鍛冶台 ----
@mat
def forge_stone():
    a = fill("#6a6a70", 90, 8, 6)
    return img(blend(a, (yy % 8 == 7) | (((xx + (yy // 8) * 8) % 16) == 15), "#3a3a40", 0.9))
simple("anvil_iron", "#3a3a42", 6, 4, pts=[(8, 6), (20, 6)], shine_col="#8a8a96", strength=0.5)
@mat
def coals():
    a = fill("#2a1a14", 91, 8, 6)
    rng = np.random.default_rng(92)
    for _ in range(40):
        x, y = rng.integers(0, N, 2); a = shine(a, [(x, y)], "#ff8a20", 0.9, 1.4)
    return img(a)
@mat
def forge_glow():
    a = fill("#ff6a10", 93, 10, 5)
    return img(shine(a, [(16, 16)], "#ffe060", 0.9, 10))
