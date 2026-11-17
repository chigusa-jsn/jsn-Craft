"""
細工師のアイコン（16×16）を作る：原石・宝石・月銀／星銀・装飾品（種類×台座、宝石は色をつける別の層）・特別な装飾品・耳飾りの枠。
    python3 tools/gen_jewel_art.py [--preview 出力.png]
"""
import os, sys, math, zlib
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "src/main/resources/assets/jsn_craft/textures")
OUT = os.path.join(TEX, "item/jewelry")

GEMS = [("garnet", 0x9A1A2A), ("peridot", 0x9AD040), ("aquamarine", 0x70D8E8), ("onyx", 0x2A2A30), ("ruby", 0xE02040),
        ("sapphire", 0x2050E0), ("topaz", 0xF0A020), ("crystal", 0xE8E0F8), ("moonstone", 0xC8D8F8), ("opal", 0xF0E0F0)]
TIERS = [("copper", 0xC8784A), ("iron", 0xD8D8D8), ("gold", 0xF0C840), ("diamond", 0x5AE0D8), ("netherite", 0x5A4A50),
         ("moon_silver", 0xB8C8F0), ("star_silver", 0xF8F0D0)]
KINDS = ["ring", "necklace", "bracelet", "earring", "charm"]

def rgb(c): return ((c >> 16) & 255, (c >> 8) & 255, c & 255)
def mix(a, b, t): return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))
def px(im, x, y, c, a=255):
    if 0 <= x < 16 and 0 <= y < 16: im.putpixel((x, y), tuple(c) + (a,))
WHITE = (255, 255, 255); BLACK = (0, 0, 0)

def shade(base):
    return {"o": mix(base, BLACK, 0.55), "d": mix(base, BLACK, 0.28), "m": base, "l": mix(base, WHITE, 0.45), "w": mix(base, WHITE, 0.8)}

def draw(rows, pal, im=None):
    im = im or Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal and pal[ch] is not None: px(im, x, y, pal[ch])
    return im

# ---- 宝石・原石 ----
CUT = [
    "................",
    "................",
    "................",
    ".....oooooo.....",
    "....olwwwlmo....",
    "...olwlllmmdo...",
    "..olllllmmmddo..",
    "..ommmmmmmmddo..",
    "...odmmmmmddo...",
    "....odmmmddo....",
    ".....odmddo.....",
    "......oddo......",
    ".......oo.......",
    "................",
    "................",
    "................",
]
def raw_gem(col, seed):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rock = (120, 112, 105)
    import random
    r = random.Random(seed)
    for y in range(4, 13):
        for x in range(3, 13):
            if (x - 7.5) ** 2 / 25 + (y - 8.5) ** 2 / 18 <= 1:
                px(im, x, y, mix(rock, BLACK, r.random() * 0.25))
    for _ in range(7):
        cx, cy = r.randint(5, 10), r.randint(6, 11)
        px(im, cx, cy, col); px(im, cx + 1, cy, mix(col, BLACK, 0.3)); px(im, cx, cy - 1, mix(col, WHITE, 0.4))
    return im

# ---- 装飾品（台座＝色、宝石＝別の層） ----
SHAPES = {
    "ring": ([
        "................",
        "................",
        "................",
        "................",
        "......dmmd......",
        ".....dm..md.....",
        "....om....mo....",
        "...om......mo...",
        "...om......mo...",
        "...od......do...",
        "....od....do....",
        ".....oddddo.....",
        "......oooo......",
        "................",
        "................",
        "................"], [(7, 1), (8, 1), (6, 2), (7, 2), (8, 2), (9, 2), (7, 3), (8, 3)], [(6, 1), (9, 1), (5, 2), (10, 2), (6, 3), (9, 3)]),
    "necklace": ([
        "..m..........m..",
        "..d..........d..",
        "...m........m...",
        "...d........d...",
        "....m......m....",
        ".....m....m.....",
        "......mddm......",
        ".......om.......",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................"], [(7, 9), (8, 9), (6, 10), (7, 10), (8, 10), (9, 10), (6, 11), (7, 11), (8, 11), (9, 11), (7, 12), (8, 12)],
        [(7, 8), (8, 8), (5, 10), (10, 10), (5, 11), (10, 11), (6, 12), (9, 12), (7, 13), (8, 13)]),
    "bracelet": ([
        "................",
        "................",
        "................",
        "................",
        ".....oooooo.....",
        "...oolllllloo...",
        "..olmmmmmmmmlo..",
        "..om........mo..",
        "..od........do..",
        "..odmmmmmmmmdo..",
        "...oodddddddo...",
        ".....oooooo.....",
        "................",
        "................",
        "................",
        "................"], [(4, 6), (5, 6), (7, 6), (8, 6), (10, 6), (11, 6)], []),
    "earring": ([
        "................",
        "......ooo.......",
        ".....o...m......",
        ".........m......",
        ".........m......",
        "........dm......",
        "........m.......",
        ".......dmd......",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................"], [(8, 9), (7, 10), (8, 10), (9, 10), (7, 11), (8, 11), (9, 11), (8, 12)],
        [(7, 8), (9, 8), (6, 9), (10, 9), (6, 10), (10, 10), (6, 11), (10, 11), (7, 12), (9, 12), (8, 13)]),
    "charm": ([
        "................",
        ".......ll.......",
        "......l..l......",
        "......l..l......",
        "....ommmmmmo....",
        "....omllllmo....",
        "....om....mo....",
        "....om....mo....",
        "....om....mo....",
        "....om....mo....",
        "....om....mo....",
        "....omddddmo....",
        "....omd..dmo....",
        "....omd..dmo....",
        "....oooooooo....",
        "................"], [(6, 7), (7, 7), (8, 7), (9, 7), (6, 8), (7, 8), (8, 8), (9, 8), (6, 9), (7, 9), (8, 9), (9, 9)], []),
}

def jewel(kind, tier_col):
    rows, gem_px, rim = SHAPES[kind]
    pal = shade(rgb(tier_col))
    base = draw(rows, {k: pal[k] for k in "odmlw"})
    for (x, y) in rim: px(base, x, y, pal["d"])
    if kind == "charm":   # 布地っぽく
        for y in range(6, 11):
            for x in range(6, 10): px(base, x, y, mix(pal["m"], WHITE, 0.15) if (x + y) % 2 else pal["m"])
    gem = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for i, (x, y) in enumerate(gem_px):
        v = 255 if (x + y) % 3 else 200
        if i == 0: v = 255
        px(gem, x, y, (v, v, v))
    if gem_px: px(gem, gem_px[0][0], gem_px[0][1], (255, 255, 255))
    return base, gem

# ---- 特別な装飾品（決まった色） ----
UNIQUES = [("wanderer_ring", "ring", 0x8A5AC8, 0x40E0D0), ("harvest_charm", "charm", 0xF0B030, 0x60C040),
           ("night_earring", "earring", 0x3A3A6A, 0xB0F0FF), ("sea_bracelet", "bracelet", 0x3A8AB0, 0x40F0E0),
           ("phoenix_charm", "charm", 0xE04020, 0xFFD040), ("echo_earring", "earring", 0x1A4A5A, 0x30E0E0),
           ("dragon_necklace", "necklace", 0x6A2A8A, 0xE070FF), ("sovereign_ring", "ring", 0xF0C840, 0xFF2040)]

def ingot(col):
    rows = [
        "................",
        "................",
        "................",
        "................",
        "................",
        "......oooooooo..",
        ".....owwllllmo..",
        "....owllllmmdo..",
        "...olllllmmddo..",
        "..ommmmmmmmddo..",
        "..odddddddddo...",
        "..ooooooooooo...",
        "................",
        "................",
        "................",
        "................"]
    return draw(rows, shade(rgb(col)))

def slot_icon():
    rows = [
        "................",
        "................",
        "......ooo.......",
        ".....o...o......",
        ".........o......",
        "........o.......",
        "........o.......",
        ".......ooo......",
        "......o...o.....",
        "......o...o.....",
        ".......ooo......",
        "................",
        "................",
        "................",
        "................",
        "................"]
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == "o": px(im, x, y, (55, 55, 55), 140)
    return im

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    os.makedirs(os.path.join(TEX, "slot"), exist_ok=True)
    preview = []
    for i, (g, c) in enumerate(GEMS):
        cut = draw(CUT, shade(rgb(c)))
        raw = raw_gem(rgb(c), zlib.crc32(g.encode()))
        cut.save(os.path.join(OUT, g + ".png")); raw.save(os.path.join(OUT, "raw_" + g + ".png"))
        preview += [raw, cut]
    for name, col in (("moon_silver_ingot", 0xB8C8F0), ("star_silver_ingot", 0xF8F0D0)):
        im = ingot(col); im.save(os.path.join(OUT, name + ".png")); preview.append(im)
    for k in KINDS:
        for t, col in TIERS:
            b, g = jewel(k, col)
            b.save(os.path.join(OUT, f"{t}_{k}.png"))
            if t == TIERS[0][0]: g.save(os.path.join(OUT, f"{k}_gem.png"))
            show = b.copy()
            ruby = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
            for y in range(16):
                for x in range(16):
                    a = g.getpixel((x, y))
                    if a[3]: ruby.putpixel((x, y), tuple(int(a[i] * rgb(0xE02040)[i] / 255) for i in range(3)) + (255,))
            show.alpha_composite(ruby); preview.append(show)
    for name, k, metal, gemc in UNIQUES:
        b, g = jewel(k, metal)
        for y in range(16):
            for x in range(16):
                a = g.getpixel((x, y))
                if a[3]: b.putpixel((x, y), tuple(int(a[i] * rgb(gemc)[i] / 255) for i in range(3)) + (255,))
        b.save(os.path.join(OUT, name + ".png")); preview.append(b)
    slot_icon().save(os.path.join(TEX, "slot", "earring.png"))
    if "--preview" in sys.argv:
        cols = 10; rows = (len(preview) + cols - 1) // cols
        sheet = Image.new("RGBA", (cols * 52, rows * 52), (50, 60, 70, 255))
        for i, im in enumerate(preview):
            big = im.resize((48, 48), Image.NEAREST); sheet.paste(big, ((i % cols) * 52 + 2, (i // cols) * 52 + 2), big)
        sheet.save(sys.argv[-1])
    print(len(preview))
