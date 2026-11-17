"""
魚のアイコン（16×16のドット絵）を作る道具。
    python3 tools/gen_fish_art.py            … 全部作る（src/main/resources/assets/jsn_craft/textures/item/fish/）
    python3 tools/gen_fish_art.py --preview  … 確認用の一覧画像も作る
形（shape）と色を決めると、輪郭・影・目・模様を自動でつける。
"""
import math, os, random, sys
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/assets/jsn_craft/textures/item/fish")

def hx(c):
    c = c.lstrip('#'); return tuple(int(c[i:i+2], 16) for i in (0, 2, 4))
def mix(a, b, t): return tuple(int(round(a[i] * (1 - t) + b[i] * t)) for i in range(3))
def dark(c, t): return mix(c, (0, 0, 0), t)
def light(c, t): return mix(c, (255, 255, 255), t)

class Canvas:
    def __init__(self): self.px = {}
    def set(self, x, y, c):
        if 0 <= x < 16 and 0 <= y < 16: self.px[(x, y)] = c
    def get(self, x, y): return self.px.get((x, y))
    def image(self, outline):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        filled = set(self.px)
        for x in range(16):
            for y in range(16):
                if (x, y) in filled: continue
                if any((x + dx, y + dy) in filled for dx, dy in ((1,0),(-1,0),(0,1),(0,-1))):
                    if outline: img.putpixel((x, y), outline + (255,))
        for (x, y), c in self.px.items():
            img.putpixel((x, y), (c + (255,)) if len(c) == 3 else c)
        return img

def inside_ellipse(x, y, cx, cy, rx, ry):
    return ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2 <= 1.0

def shade(cv, pts, body, cy, ry):
    for (x, y) in pts:
        py = y + 0.5
        if py < cy - ry * 0.35: cv.set(x, y, dark(body, 0.22))
        elif py > cy + ry * 0.3: cv.set(x, y, light(body, 0.35))
        else: cv.set(x, y, body)

def fish(cv, f):
    """ふつうの魚。h=体の高さ、tail=尾の大きさ"""
    h = f.get("h", 6); cy = 8.0; rx = f.get("rx", 5.6); cx = 1.2 + rx
    ry = h / 2
    body = hx(f["body"]); fin = hx(f.get("fin", "#" + "%02x%02x%02x" % dark(body, 0.25)))
    pts = [(x, y) for x in range(16) for y in range(16) if inside_ellipse(x, y, cx, cy, rx, ry)]
    shade(cv, pts, body, cy, ry)
    if f.get("belly"):
        for (x, y) in pts:
            if y + 0.5 > cy + ry * 0.3: cv.set(x, y, hx(f["belly"]))
    # 尾びれ
    tx = int(cx + rx) - 1; ts = f.get("tail", 1.0)
    for x in range(tx, min(16, tx + 4)):
        k = (x - tx + 1) * 0.85 * ts
        for y in range(16):
            d = abs(y + 0.5 - cy)
            if d <= k and (d >= k - 1.6 * ts or x <= tx + 1): cv.set(x, y, fin)
    # 背びれ・腹びれ
    top = int(cy - ry) - 1; mid = int(cx)
    for x in range(mid - 1, mid + 2 + f.get("dorsal", 0)): cv.set(x, top, fin)
    if f.get("dorsal", 0) >= 2:
        for x in range(mid, mid + 2): cv.set(x, top - 1, fin)
    cv.set(mid, int(cy + ry), fin)
    # 模様
    rnd = random.Random(f["id"])
    pat = f.get("pattern")
    acc = hx(f.get("accent", "#000000"))
    if pat == "spots":
        for _ in range(f.get("n", 5)):
            x, y = rnd.choice(pts)
            if x > 3: cv.set(x, y, acc)
    elif pat == "stripes":
        for x in range(4, int(cx + rx), 2):
            for (px, py) in pts:
                if px == x and py + 0.5 < cy + ry * 0.4: cv.set(px, py, acc)
    elif pat == "band":
        for (px, py) in pts:
            if abs(py + 0.5 - cy) < 0.8 and px > 2: cv.set(px, py, acc)
    elif pat == "line":
        for (px, py) in pts:
            if int(py) == int(cy) and px > 3: cv.set(px, py, acc)
    elif pat == "waves":
        for (px, py) in pts:
            if py + 0.5 < cy - 0.5 and (px + py) % 3 == 0: cv.set(px, py, acc)
    elif pat == "zigzag":
        for x in range(3, int(cx + rx)):
            y = int(cy) + (-1 if (x // 2) % 2 else 0)
            if (x, y) in pts: cv.set(x, y, acc)
    # 目
    ex = int(cx - rx) + 1 + f.get("eye_dx", 0); ey = int(cy - 1)
    cv.set(ex, ey, hx(f.get("eye", "#101010")))
    if f.get("eye_glow"): cv.set(ex, ey, hx(f["eye_glow"]))
    if f.get("whiskers"):
        w = hx(f.get("whisker_color", "#202020"))
        cv.set(int(cx - rx) - 1, int(cy) + 1, w); cv.set(int(cx - rx) - 2, int(cy) + 2, w)
    if f.get("teeth"):
        cv.set(int(cx - rx) + 0, int(cy) + 1, (240, 240, 240))
    if f.get("crown"):
        c = hx(f["crown"])
        for x in (ex, ex + 2, ex + 4): cv.set(x, top - 1, c)
        for x in range(ex, ex + 5): cv.set(x, top, c)
    if f.get("bill"):
        for x in range(0, int(cx - rx) + 1): cv.set(x, int(cy) - 0, hx(f.get("bill_color", "#303030")))

def eel(cv, f):
    body = hx(f["body"]); acc = hx(f.get("accent", "#000000"))
    for x in range(1, 15):
        yc = 8 + 2.2 * math.sin((x - 1) * 0.55)
        for y in range(16):
            if abs(y + 0.5 - yc) <= 1.1: cv.set(x, y, body if y + 0.5 >= yc else dark(body, 0.25))
            if f.get("pattern") == "zigzag" and abs(y + 0.5 - yc) <= 0.4 and x % 2 == 0: cv.set(x, y, acc)
    cv.set(1, int(8 + 0) - 1, hx("#101010"))
    cv.set(15, int(8 + 2.2 * math.sin(14 * 0.55)), dark(body, 0.25))

def flat(cv, f):
    body = hx(f["body"]); acc = hx(f.get("accent", "#000000"))
    pts = [(x, y) for x in range(16) for y in range(16) if inside_ellipse(x, y, 7.0, 8.5, 6.0, 4.6)]
    shade(cv, pts, body, 8.5, 4.6)
    rnd = random.Random(f["id"])
    for _ in range(6):
        x, y = rnd.choice(pts); cv.set(x, y, acc)
    for x in range(13, 16):
        for y in range(7, 11): cv.set(x, y, dark(body, 0.2))
    cv.set(3, 6, (16, 16, 16)); cv.set(4, 5, (16, 16, 16))

def squid(cv, f):
    body = hx(f["body"]); acc = hx(f.get("accent", "#000000"))
    pts = [(x, y) for x in range(16) for y in range(16) if inside_ellipse(x, y, 8.0, 5.5, 3.6, 5.2)]
    shade(cv, pts, body, 5.5, 5.2)
    for x in (8, 9): cv.set(x, 0, dark(body, 0.2))
    for i, x in enumerate((5, 7, 9, 11)):
        for y in range(10, 15 + (i % 2)): cv.set(x + (1 if y > 12 and i < 2 else 0), y, light(body, 0.1) if y % 2 else body)
    cv.set(6, 8, (16, 16, 16)); cv.set(10, 8, (16, 16, 16))
    rnd = random.Random(f["id"])
    for _ in range(4):
        x, y = rnd.choice(pts); cv.set(x, y, acc)

def crab(cv, f):
    body = hx(f["body"]); acc = hx(f.get("accent", "#ff60c0"))
    pts = [(x, y) for x in range(16) for y in range(16) if inside_ellipse(x, y, 8.0, 9.5, 5.0, 3.2)]
    shade(cv, pts, body, 9.5, 3.2)
    for (cx, cy) in ((2.5, 5.5), (13.5, 5.5)):
        for x in range(16):
            for y in range(16):
                if inside_ellipse(x, y, cx, cy, 2.0, 1.8): cv.set(x, y, light(body, 0.1))
    cv.set(2, 4, dark(body, 0.4)); cv.set(13, 4, dark(body, 0.4))
    for x in (3, 12): cv.set(x, 7, body); cv.set(x, 8, body)
    for x, y in ((3, 12), (4, 13), (12, 12), (11, 13), (2, 11), (13, 11)): cv.set(x, y, dark(body, 0.3))
    cv.set(6, 6, (16, 16, 16)); cv.set(9, 6, (16, 16, 16))
    for x, y in ((7, 9), (9, 10), (6, 10)): cv.set(x, y, acc)
    if f.get("accent2"): cv.set(8, 8, hx(f["accent2"])); cv.set(10, 9, hx(f["accent2"]))

def angler(cv, f):
    body = hx(f["body"])
    pts = [(x, y) for x in range(16) for y in range(16) if inside_ellipse(x, y, 8.0, 9.5, 5.6, 4.8)]
    shade(cv, pts, body, 9.5, 4.8)
    for x in range(13, 16):
        for y in range(8, 12): cv.set(x, y, dark(body, 0.2))
    for x in range(3, 7): cv.set(x, 11, (20, 10, 10))
    for x in (3, 5): cv.set(x, 10, (240, 240, 230))
    cv.set(4, 12, (240, 240, 230))
    cv.set(6, 7, (230, 230, 80))
    for (x, y) in ((6, 4), (5, 3), (4, 2), (3, 2)): cv.set(x, y, dark(body, 0.3))
    lure = hx(f.get("lure", "#fff070"))
    cv.set(2, 2, lure); cv.set(2, 3, light(lure, 0.5)); cv.set(1, 2, light(lure, 0.5))

def shrimp(cv, f):
    body = hx(f["body"]); acc = hx(f.get("accent", "#ffffff"))
    # 丸まった体（右上から左下へ弧）
    for i in range(9):
        a = math.pi * (0.15 + i * 0.09)
        cx = 8 + 5.0 * math.cos(a); cy = 6 + 5.0 * math.sin(a)
        r = 2.0 - i * 0.12
        for x in range(16):
            for y in range(16):
                if inside_ellipse(x, y, cx, cy, r, r): cv.set(x, y, body if i % 2 else light(body, 0.15))
    for x, y in ((13, 4), (14, 3), (12, 3)): cv.set(x, y, dark(body, 0.3))  # 尾
    cv.set(3, 7, (16, 16, 16))
    for x, y in ((2, 6), (1, 5), (0, 4), (2, 5), (1, 3)): cv.set(x, y, acc)  # ひげ
    for x, y in ((6, 12), (8, 13), (10, 12)): cv.set(x, y, dark(body, 0.25))  # 脚

def turtle(cv, f):
    body = hx(f["body"]); shell = hx(f.get("shell", "#6a4a3a")); acc = hx(f.get("accent", "#3a2a20"))
    for x in range(16):
        for y in range(16):
            if inside_ellipse(x, y, 8.5, 8.5, 5.2, 4.0):
                rim = not inside_ellipse(x, y, 8.5, 8.5, 4.2, 3.0)
                plate = (x % 3 == 1 and y % 3 == 1)
                cv.set(x, y, dark(shell, 0.3) if rim else light(shell, 0.25) if plate else shell)
    for x, y in ((2, 8), (3, 8), (2, 9), (3, 9), (1, 8)): cv.set(x, y, body)  # 頭
    cv.set(1, 8, (16, 16, 16))
    for x, y in ((5, 4), (4, 4), (12, 4), (13, 4), (5, 13), (4, 13), (12, 13), (13, 13), (14, 8)): cv.set(x, y, body)
    if f.get("glow"):
        g = hx(f["glow"])
        for x, y in ((7, 7), (10, 9), (8, 10)): cv.set(x, y, g)

def jelly(cv, f):
    body = hx(f["body"]); acc = hx(f.get("accent", "#ffffff"))
    for x in range(16):
        for y in range(16):
            if inside_ellipse(x, y, 8.0, 6.5, 5.5, 4.5) and y <= 7:
                cv.set(x, y, light(body, 0.3) if y < 4 else body)
    for x in range(3, 14): cv.set(x, 7, dark(body, 0.2))
    for i, x in enumerate((4, 6, 8, 10, 12)):
        for y in range(8, 14 + (i % 2)):
            cv.set(x + (1 if (y + i) % 4 == 0 else 0), y, acc if y % 3 == 0 else light(body, 0.1))
    for x, y in ((6, 4), (9, 3), (10, 5)): cv.set(x, y, acc)

def ray(cv, f):
    body = hx(f["body"]); acc = hx(f.get("accent", "#ffffff"))
    for x in range(16):
        for y in range(16):
            dx = abs(x + 0.5 - 8); dy = y + 0.5 - 7
            if abs(dy) <= 3.5 - dx * 0.45 and dx < 7.5:
                cv.set(x, y, dark(body, 0.2) if dy < -1 else body)
    for i, (x, y) in enumerate(((8, 10), (9, 11), (10, 12), (11, 13), (12, 14), (13, 15))): cv.set(x, y, dark(body, 0.3))
    cv.set(6, 5, (16, 16, 16)); cv.set(10, 5, (16, 16, 16))
    for x, y in ((3, 7), (13, 7), (8, 8), (5, 8), (11, 8)): cv.set(x, y, acc)

SHAPES = {"jelly": jelly, "ray": ray, "shrimp": shrimp, "turtle": turtle, "fish": fish, "eel": eel, "flat": flat, "squid": squid, "crab": crab, "angler": angler}

def make(f):
    cv = Canvas()
    SHAPES[f.get("shape", "fish")](cv, f)
    body = hx(f["body"])
    outline = hx(f["outline"]) if f.get("outline") else dark(body, 0.6)
    return cv.image(outline)

# ---- 魚の一覧（id は jsn_craft:fish/<id> のアイテム名になる） ----
FISH = [
    # コモン
    dict(id="crucian_carp", body="#8c8c5c", h=6),
    dict(id="carp", body="#b08a50", h=6.5, whiskers=True, pattern="spots", accent="#8a6a38", n=4),
    dict(id="sardine", body="#6f8fb4", belly="#e8eef4", h=4, pattern="spots", accent="#20304a", n=4),
    dict(id="goby", body="#a89070", h=4.5, pattern="spots", accent="#6a5440", n=6),
    dict(id="bluegill", body="#5e7f8f", belly="#e0904a", h=7, pattern="stripes", accent="#4a6676"),
    # アンコモン
    dict(id="yamame", body="#a8ae98", h=5, pattern="spots", accent="#6a7aa4", n=6),
    dict(id="iwana", body="#6a7058", h=5, pattern="spots", accent="#f0f0e0", n=7),
    dict(id="ayu", body="#b8b070", belly="#eeeac8", h=5, pattern="spots", accent="#f0d030", n=1),
    dict(id="horse_mackerel", body="#9fb0b8", belly="#eef2f4", h=5, pattern="line", accent="#4a5a60"),
    dict(id="mackerel", body="#4f8f8f", belly="#e6eef0", h=5, pattern="waves", accent="#1a2a2a"),
    dict(id="red_sea_bream", body="#e06a6a", h=7.5, dorsal=2, pattern="spots", accent="#6fc8ff", n=5),
    dict(id="flounder", shape="flat", body="#8a7558", accent="#5a4a34"),
    dict(id="catfish", body="#5b5a52", h=5, whiskers=True, whisker_color="#30302a"),
    dict(id="eel", shape="eel", body="#4a4f3a"),
    dict(id="piranha", body="#8a9098", belly="#d04a3a", h=7.5, teeth=True),
    dict(id="squid", shape="squid", body="#e8c8c0", accent="#b06a6a"),
    dict(id="goldfish", body="#ff8a2a", fin="#ffb070", h=6, tail=1.4, pattern="spots", accent="#fff0e0", n=3),
    # レア
    dict(id="moonlight_crucian", body="#b8c8e8", fin="#e8f0ff", h=6, pattern="spots", accent="#fff8b0", n=5, outline="#4a5a8a"),
    dict(id="rainbow_trout", body="#a8b0a4", h=5, pattern="band", accent="#e07a9a"),
    dict(id="anglerfish", shape="angler", body="#5a4040", lure="#fff070"),
    dict(id="tuna", body="#2f3f6a", belly="#c8d0dc", fin="#e8c040", h=6, tail=1.2),
    dict(id="swordfish", body="#3a5a8a", belly="#c8d0e0", h=4.5, rx=5.0, dorsal=2, bill=True, bill_color="#2a3a5a"),
    dict(id="arapaima", body="#5a6050", fin="#c04030", h=5, rx=6.0, pattern="spots", accent="#c04030", n=3),
    dict(id="jewel_crab", shape="crab", body="#3fb0a8", accent="#ff60c0", accent2="#80f0ff"),
    # エピック
    dict(id="thunder_eel", shape="eel", body="#2a2a4a", pattern="zigzag", accent="#ffe030"),
    dict(id="glacier_salmon", body="#c8e0f0", fin="#90c0e0", h=5.5, pattern="band", accent="#f0a0b0", outline="#3a5a7a"),
    dict(id="coelacanth", body="#3a4a6a", h=7, dorsal=2, pattern="spots", accent="#e8eef8", n=7),
    dict(id="golden_carp", body="#ffc830", fin="#fff090", h=6.5, whiskers=True, whisker_color="#a07010", pattern="spots", accent="#fff6c0", n=4, outline="#7a5000"),
    dict(id="arowana", body="#c8c4b0", fin="#e0a040", h=4.5, rx=6.0, whiskers=True, pattern="spots", accent="#e0b050", n=6),
    dict(id="ghost_fish", body="#d8e0e8", fin="#f0f6ff", h=6, eye_glow="#40ffff", outline="#8090a8"),
    # レジェンド
    dict(id="moon_lord", body="#2a3a6a", fin="#8ab0ff", h=8, dorsal=2, tail=1.2, pattern="spots", accent="#fff6a0", n=7, eye_glow="#fff6a0"),
    dict(id="storm_king", body="#303848", fin="#6080c0", h=7.5, dorsal=2, tail=1.2, pattern="zigzag", accent="#ffe040", eye_glow="#ffe040"),
    # ---- ネザー ----
    dict(id="lava_goby", body="#d0502a", h=4.5, pattern="spots", accent="#ffd040", n=5, outline="#4a1000"),
    dict(id="flame_shrimp", shape="shrimp", body="#ff6a20", accent="#ffe060"),
    dict(id="ash_eel", shape="eel", body="#5a5a62", pattern="zigzag", accent="#ff8030"),
    dict(id="warped_catfish", body="#2a8a80", h=5, whiskers=True, whisker_color="#5ef0d0", pattern="spots", accent="#5ef0d0", n=5),
    dict(id="crimson_piranha", body="#a0202a", belly="#ff6040", h=7.5, teeth=True, pattern="spots", accent="#ff9070", n=3),
    dict(id="basalt_catfish", body="#3a3a42", h=5.5, whiskers=True, whisker_color="#20202a", pattern="spots", accent="#707078", n=6),
    dict(id="ghast_fish", body="#f0f0f0", fin="#d8d8d8", h=7, eye="#404040", pattern="spots", accent="#a0e0ff", n=2, outline="#808080"),
    dict(id="inferno_shachihoko", body="#f0b020", fin="#ff6020", h=7, dorsal=2, tail=1.4, pattern="spots", accent="#fff0a0", n=4, outline="#6a3a00"),
    dict(id="soul_eater", body="#3a2a20", fin="#5a4030", h=7, teeth=True, eye_glow="#60e0ff", pattern="spots", accent="#60e0ff", n=3),
    dict(id="debris_turtle", shape="turtle", body="#7a5a48", shell="#5a3a2a", accent="#3a2018", glow="#c08060"),
    dict(id="inferno_dragonfish", body="#c02010", fin="#ffb020", h=6.5, rx=6.0, dorsal=2, tail=1.3, crown="#ffd040", pattern="zigzag", accent="#ffe060", eye_glow="#ffff80", outline="#400000"),
    dict(id="soulflame_catfish", body="#203040", fin="#3a6080", h=7.5, whiskers=True, whisker_color="#60e0ff", tail=1.2, pattern="spots", accent="#60e0ff", n=7, eye_glow="#60e0ff"),
    # ---- エンド ----
    dict(id="hollow_goby", body="#dcd9a0", h=4.5, pattern="spots", accent="#8a8660", n=5),
    dict(id="chorus_fish", body="#9a6a9a", fin="#c8a0c8", h=6, pattern="spots", accent="#e8c0f0", n=5),
    dict(id="purpur_pleco", body="#a77aa7", h=5, whiskers=True, whisker_color="#6a4a6a", pattern="stripes", accent="#c8a0c8"),
    dict(id="ender_medaka", body="#1a1a24", fin="#2a2a3a", h=4, rx=4.5, eye_glow="#e080ff", pattern="spots", accent="#7a40a0", n=2),
    dict(id="stardust_sardine", body="#20284a", belly="#3a4470", h=4, pattern="spots", accent="#ffffff", n=6),
    dict(id="crystal_shrimp", shape="shrimp", body="#f0c8f0", accent="#80ffff"),
    dict(id="shulker_fish", body="#8a5a8a", fin="#c8a0c8", h=7, dorsal=2, pattern="waves", accent="#e8c8e8", eye_glow="#ffff80"),
    dict(id="void_jelly", shape="jelly", body="#5a3a8a", accent="#e0a0ff"),
    dict(id="elytra_ray", shape="ray", body="#7a7aa8", accent="#c0c0e8"),
    dict(id="ender_dragonfish", body="#1a1020", fin="#a040e0", h=7, rx=6.0, dorsal=2, tail=1.3, crown="#c060ff", pattern="zigzag", accent="#c060ff", eye_glow="#ff80ff", outline="#000000"),
    dict(id="ice_queen", body="#e0f6ff", fin="#80d0ff", h=6.5, dorsal=0, crown="#ffd040", pattern="spots", accent="#a0e8ff", n=4, outline="#3a7aa0"),
]

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    imgs = []
    for f in FISH:
        img = make(f)
        img.save(os.path.join(OUT, f["id"] + ".png"))
        imgs.append(img)
    if "--preview" in sys.argv:
        cols = 8; rows = (len(imgs) + cols - 1) // cols
        sheet = Image.new("RGBA", (cols * 80, rows * 80), (60, 60, 70, 255))
        for i, im in enumerate(imgs):
            sheet.paste(im.resize((64, 64), Image.NEAREST), ((i % cols) * 80 + 8, (i // cols) * 80 + 8), im.resize((64, 64), Image.NEAREST))
        sheet.save(sys.argv[-1] if sys.argv[-1].endswith(".png") else "preview.png")
    print(len(FISH), "匹")
