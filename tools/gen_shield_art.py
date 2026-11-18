"""
盾騎士の大盾（6段階）の絵。バニラの盾と同じ形（ShieldModel）に貼る 256×256 の絵（バニラの64×64の4倍）。
    python3 tools/gen_shield_art.py 出力フォルダ [--preview 出力.png]
配置（4倍）：表 (4,4)-(52,92)、裏 (56,4)-(104,92)、ふち (0,0)-(104,92) のまわり、持ち手 (104,0)-(168,48)
"""
import math, os, sys, random, zlib
from PIL import Image, ImageDraw
sys.path.insert(0, os.path.dirname(__file__))
from gen_staff_art import hx

S = 4
FW, FH = 48, 88          # 表の大きさ（px）

def front(base, rim, emblem=None, rivets=None, plank=False, seed=1):
    """盾の表（48×88）。8倍で描いて縮める"""
    K = 8
    img = Image.new("RGBA", (FW * K, FH * K), base[1] + (255,))
    d = ImageDraw.Draw(img)
    rng = random.Random(seed)
    if plank:   # 板目
        for i in range(4):
            x0 = i * FW * K // 4
            d.rectangle([x0, 0, x0 + FW * K // 4 - 1, FH * K], fill=(base[1] if i % 2 == 0 else base[2]) + (255,))
            d.line([x0, 0, x0, FH * K], fill=base[0] + (255,), width=K)
            for _ in range(6):
                y = rng.randint(0, FH * K)
                d.line([x0 + K * 2, y, x0 + FW * K // 4 - K * 2, y + rng.randint(-K, K)], fill=base[0] + (120,), width=K // 2)
    else:       # 金属の光
        for y in range(0, FH * K, K):
            t = y / (FH * K)
            c = tuple(int(base[1][i] + (base[2][i] - base[1][i]) * max(0, 0.5 - abs(t - 0.3)) * 1.6) for i in range(3))
            d.rectangle([0, y, FW * K, y + K], fill=c + (255,))
    # ふち
    w = 4 * K
    d.rectangle([0, 0, FW * K - 1, FH * K - 1], outline=rim[1] + (255,), width=w)
    d.rectangle([w, w, FW * K - 1 - w, FH * K - 1 - w], outline=rim[0] + (255,), width=K)
    d.line([K, K, FW * K - K, K], fill=rim[2] + (255,), width=K)
    if rivets:
        for (x, y) in ((6, 6), (FW - 7, 6), (6, FH - 7), (FW - 7, FH - 7), (6, FH // 2), (FW - 7, FH // 2)):
            d.ellipse([(x - 1.6) * K, (y - 1.6) * K, (x + 1.6) * K, (y + 1.6) * K], fill=rivets[1] + (255,))
            d.ellipse([(x - 1.0) * K, (y - 1.2) * K, (x + 0.2) * K, (y - 0.2) * K], fill=rivets[2] + (255,))
    if emblem: emblem(d, K)
    return img.resize((FW, FH), Image.LANCZOS)

def star_poly(cx, cy, r1, r2, n=5, rot=-90):
    pts = []
    for i in range(n * 2):
        r = r1 if i % 2 == 0 else r2
        a = math.radians(rot + 180 * i / n)
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts

def cross(col, col2):
    def e(d, K):
        cx = FW // 2
        d.rectangle([(cx - 3) * K, 12 * K, (cx + 3) * K, (FH - 12) * K], fill=col + (255,))
        d.rectangle([10 * K, 32 * K, (FW - 10) * K, 38 * K], fill=col + (255,))
        d.rectangle([(cx - 1) * K, 13 * K, cx * K, (FH - 13) * K], fill=col2 + (255,))
    return e

def gem(col, ring):
    def e(d, K):
        cx, cy = FW / 2, FH * 0.42
        d.polygon([((cx) * K, (cy - 12) * K), ((cx + 10) * K, cy * K), (cx * K, (cy + 12) * K), ((cx - 10) * K, cy * K)], fill=ring[1] + (255,))
        d.ellipse([(cx - 6) * K, (cy - 6) * K, (cx + 6) * K, (cy + 6) * K], fill=col[0] + (255,))
        d.ellipse([(cx - 5) * K, (cy - 5.5) * K, (cx + 4) * K, (cy + 4) * K], fill=col[1] + (255,))
        d.ellipse([(cx - 3.5) * K, (cy - 4) * K, (cx - 1) * K, (cy - 1.5) * K], fill=col[2] + (255,))
        d.polygon([(cx * K, (cy + 14) * K), ((cx + 3) * K, (cy + 30) * K), (cx * K, (cy + 40) * K), ((cx - 3) * K, (cy + 30) * K)], fill=ring[1] + (255,))
    return e

def moon_emblem(d, K):
    cx, cy = FW / 2, FH * 0.4
    d.ellipse([(cx - 13) * K, (cy - 13) * K, (cx + 13) * K, (cy + 13) * K], fill=hx("#eef4ff") + (255,))
    d.ellipse([(cx - 7) * K, (cy - 15) * K, (cx + 17) * K, (cy + 9) * K], fill=hx("#a4b2dc") + (255,))
    for (x, y) in ((cx + 9, cy + 16), (cx - 12, cy + 24), (cx + 4, cy + 32)):
        d.polygon(star_poly(x * K, y * K, 2.6 * K, 0.9 * K, 4, 0), fill=hx("#eef4ff") + (255,))

def star_emblem(d, K):
    cx, cy = FW / 2, FH * 0.42
    # 星座
    pts = [(10, 14), (16, 20), (13, 27), (20, 30)]
    pts2 = [(FW - 10, FH - 18), (FW - 16, FH - 24), (FW - 12, FH - 31), (FW - 19, FH - 34)]
    for ps in (pts, pts2):
        d.line([(x * K, y * K) for x, y in ps], fill=hx("#8aa8ff") + (255,), width=K)
        for x, y in ps: d.ellipse([(x - 1.2) * K, (y - 1.2) * K, (x + 1.2) * K, (y + 1.2) * K], fill=hx("#ffffff") + (255,))
    d.ellipse([(cx - 16) * K, (cy - 16) * K, (cx + 16) * K, (cy + 16) * K], outline=hx("#e0a830") + (255,), width=2 * K)
    d.polygon(star_poly(cx * K, cy * K, 14 * K, 5.6 * K), fill=hx("#8a5a10") + (255,))
    d.polygon(star_poly(cx * K, cy * K, 12 * K, 4.8 * K), fill=hx("#ffe040") + (255,))
    d.polygon(star_poly((cx - 0.8) * K, (cy - 0.8) * K, 5 * K, 2 * K), fill=hx("#fffbe0") + (255,))
    for (x, y, r) in ((8, FH - 10, 2.4), (FW - 9, 10, 2.4), (FW / 2, FH - 12, 2.0)):
        d.polygon(star_poly(x * K, y * K, r * K, r * 0.35 * K, 4, 0), fill=hx("#fff4c0") + (255,))

def neth_emblem(d, K):
    gem((hx("#7a1010"), hx("#ff4020"), hx("#ffb080")), (None, hx("#e0a830"), None))(d, K)

WOODB = (hx("#4a2c16"), hx("#7a4a26"), hx("#8a5a32"))
IRON = (hx("#5a5a68"), hx("#9a9aaa"), hx("#e0e0ea"))
GOLD = (hx("#8a5a10"), hx("#e0a830"), hx("#fff0a0"))
DIA = (hx("#1a6a6a"), hx("#3ac0b8"), hx("#c0fff8"))
NETH = (hx("#1e1a1e"), hx("#3a3238"), hx("#6a6068"))
MOON = (hx("#5a6a90"), hx("#9aaad8"), hx("#eef4ff"))
NIGHT = (hx("#141030"), hx("#2a2460"), hx("#4a44a0"))

SHIELDS = {
    "wood_tower_shield": dict(base=WOODB, rim=IRON, rivets=IRON, plank=True),
    "iron_tower_shield": dict(base=IRON, rim=IRON, rivets=GOLD, emblem=cross(hx("#3a7ae0"), hx("#a0d0ff"))),
    "diamond_tower_shield": dict(base=DIA, rim=GOLD, rivets=GOLD, emblem=gem((hx("#1a3a7a"), hx("#3a7ae0"), hx("#c0e0ff")), GOLD)),
    "netherite_tower_shield": dict(base=NETH, rim=GOLD, rivets=GOLD, emblem=neth_emblem),
    "moon_silver_tower_shield": dict(base=MOON, rim=(hx("#7a88b0"), hx("#e8ecff"), hx("#ffffff")), rivets=MOON, emblem=moon_emblem),
    "star_silver_tower_shield": dict(base=NIGHT, rim=GOLD, rivets=GOLD, emblem=star_emblem),
}

def texture(spec, key):
    tex = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    rim = spec["rim"]
    d = ImageDraw.Draw(tex)
    d.rectangle([0, 0, 103, 91], fill=rim[0] + (255,))           # ふち・側面
    d.rectangle([0, 0, 103, 3], fill=rim[1] + (255,))
    back = spec["base"][0]
    d.rectangle([56, 4, 103, 91], fill=back + (255,))              # 裏
    for y in range(8, 92, 12): d.line([56, y, 103, y], fill=tuple(max(0, c - 15) for c in back) + (255,), width=2)
    tex.paste(front(seed=zlib.crc32(key.encode()) % 1000, **spec), (4, 4))        # 表
    d.rectangle([104, 0, 167, 47], fill=hx("#3a2418") + (255,))    # 持ち手
    d.rectangle([104, 0, 167, 3], fill=hx("#6a4428") + (255,))
    return tex

if __name__ == "__main__":
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    fronts = []
    for k, spec in SHIELDS.items():
        tex = texture(spec, k)
        tex.save(os.path.join(out, k + ".png"))
        fronts.append(tex.crop((4, 4, 52, 92)))
    if "--preview" in sys.argv:
        prev = sys.argv[sys.argv.index("--preview") + 1]
        sheet = Image.new("RGBA", (len(fronts) * 160 + 10, 300), (40, 44, 52, 255))
        for i, f in enumerate(fronts):
            sheet.alpha_composite(f.resize((144, 264), Image.NEAREST), (i * 160 + 8, 18))
        sheet.save(prev)
    print(list(SHIELDS))
