"""
突然変異の動物の専用テクスチャを作る。
マイクラの元の絵（配置と陰影）を下じきにして、色を塗りかえる。元の絵はこのリポジトリには入れない。
    python3 tools/gen_mutant_art.py 元の絵のフォルダ [--preview 出力.png]
元の絵のフォルダ：chicken.png cow.png pig.png horse_white.png bee.png bee_angry.png bee_nectar.png bee_angry_nectar.png
"""
import os, sys, colorsys
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/assets/jsn_craft/textures/entity/mutant")

def hx(h): h = h.lstrip("#"); return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))
def lerp(a, b, t): t = max(0.0, min(1.0, t)); return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))
def hsl(p):
    h, l, s = colorsys.rgb_to_hls(p[0] / 255, p[1] / 255, p[2] / 255)
    return h * 360, s, l

def recolor(src, rule):
    im = src.convert("RGBA"); out = im.copy()
    for y in range(im.height):
        for x in range(im.width):
            p = im.getpixel((x, y))
            if p[3] == 0: continue
            c = rule(x, y, p[:3])
            if c is not None: out.putpixel((x, y), tuple(c) + (p[3],))
    return out

def golden_chicken(src):
    lo, hi = hx("#9a6408"), hx("#fff0a0")
    def rule(x, y, p):
        h, s, l = hsl(p)
        if s < 0.2 and l > 0.45:                      # 白い羽根 → 金
            t = (l - 0.45) / 0.55
            c = lerp(lo, hi, t ** 0.8)
            if (x * 7 + y * 13) % 23 == 0: c = hx("#ffffff")   # キラッ
            return c
        if 0 <= h < 20 or h > 340:                     # とさか → 濃い赤
            return lerp(hx("#a00010"), hx("#ff3040"), l * 1.4)
        return None
    return recolor(src, rule)

def marbled_cow(src):
    def rule(x, y, p):
        h, s, l = hsl(p)
        if 22 <= x < 28 and y < 6: return None         # 角はそのまま
        if l < 0.12: return None                       # 目
        if s > 0.25 and (h < 30 or h > 320) and l > 0.4: return lerp(hx("#c85068"), hx("#ffb0c0"), l)   # 鼻・乳 → 濃いピンク
        if l < 0.35:                                   # こげ茶 → クリーム色
            return lerp(hx("#d8ccc0"), hx("#fbf6ee"), l / 0.35)
        return lerp(hx("#d06a80"), hx("#f6a8b8"), (l - 0.35) / 0.65)   # 白い模様 → ピンクの霜降り
    return recolor(src, rule)

def truffle_pig(src):
    def rule(x, y, p):
        h, s, l = hsl(p)
        if s < 0.15 and l > 0.85: return None          # 白目
        if l < 0.15: return None                       # 黒目
        c = lerp(hx("#2a180e"), hx("#7a5236"), (l - 0.35) / 0.55)
        n = (x * 374761393 + y * 668265263) & 0xFFFFFFFF
        n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
        if (n >> 24) < 26: c = lerp(c, hx("#1a0e08"), 0.45)          # 土っぽいまだら
        elif (n >> 24) > 236: c = lerp(c, hx("#a07a50"), 0.4)
        return c
    return recolor(src, rule)

def pegasus(src):
    def rule(x, y, p):
        h, s, l = hsl(p)
        if s > 0.22: return None                       # 鞍・手綱・チェストはそのまま
        if l < 0.15: return None                       # 目
        mane = (56 <= x < 64 and 36 <= y < 58) or (42 <= x < 56 and 36 <= y < 54)   # たてがみ・しっぽ
        if mane: return lerp(hx("#3a8ae0"), hx("#b8f0ff"), l)
        return lerp(hx("#b8c8e8"), hx("#ffffff"), (l - 0.3) / 0.6)
    return recolor(src, rule)

def queen_bee(src):
    def rule(x, y, p):
        h, s, l = hsl(p)
        face = 10 <= x < 17 and 10 <= y < 17           # 顔（目は黒いまま）
        if l < 0.2:
            if face: return None
            return lerp(hx("#a07010"), hx("#ffd040"), 0.5 + l)          # 黒いしま → 金
        if 35 <= h <= 65 and s > 0.4:                  # 黄色 → 紫
            return lerp(hx("#4a1a8a"), hx("#c890ff"), l)
        return None
    out = recolor(src, rule)
    # 小さな王冠（体の上の面、前のほう）
    crown = {(11, 9): "#ffd040", (13, 9): "#ffd040", (15, 9): "#ffd040",
             (11, 8): "#ffe880", (13, 8): "#ff3050", (15, 8): "#ffe880", (12, 9): "#e0a020", (14, 9): "#e0a020"}
    for (x, y), c in crown.items(): out.putpixel((x, y), hx(c) + (255,))
    return out

JOBS = [("chicken.png", "golden_chicken.png", golden_chicken), ("cow.png", "marbled_cow.png", marbled_cow),
        ("pig.png", "truffle_pig.png", truffle_pig), ("horse_white.png", "pegasus.png", pegasus),
        ("bee.png", "queen_bee.png", queen_bee), ("bee_angry.png", "queen_bee_angry.png", queen_bee),
        ("bee_nectar.png", "queen_bee_nectar.png", queen_bee), ("bee_angry_nectar.png", "queen_bee_angry_nectar.png", queen_bee)]

if __name__ == "__main__":
    src_dir = sys.argv[1]
    os.makedirs(OUT, exist_ok=True)
    made = []
    for s, d, f in JOBS:
        im = f(Image.open(os.path.join(src_dir, s)))
        im.save(os.path.join(OUT, d)); made.append((Image.open(os.path.join(src_dir, s)).convert("RGBA"), im))
    if "--preview" in sys.argv:
        sheet = Image.new("RGBA", (2 * 264, 5 * 264), (80, 80, 90, 255))
        for i, (a, b) in enumerate(made[:5]):
            for j, im in enumerate((a, b)):
                k = 256 // im.width; big = im.resize((im.width * k, im.height * k), Image.NEAREST)
                sheet.paste(big, (j * 264 + 4, i * 264 + 4), big)
        sheet.save(sys.argv[-1])
    print(len(made))
