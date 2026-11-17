"""
作物（成長段階4枚ずつ）と食材アイテムの絵（16×16）を作る道具。
    python3 tools/gen_crop_art.py [--preview 出力.png]
"""
import os, sys, random, math
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ITEM = os.path.join(ROOT, "src/main/resources/assets/jsn_craft/textures/item/ingredient")
BLOCK = os.path.join(ROOT, "src/main/resources/assets/jsn_craft/textures/block/crop")

def hx(c):
    c = c.lstrip('#'); return tuple(int(c[i:i+2], 16) for i in (0, 2, 4)) + (255,)
def mix(a, b, t): return tuple(int(round(a[i] * (1 - t) + b[i] * t)) for i in range(3)) + (255,)
def dark(c, t): return mix(c, (0, 0, 0), t)
def light(c, t): return mix(c, (255, 255, 255), t)

class C:
    def __init__(self): self.im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    def set(self, x, y, c):
        x, y = int(round(x)), int(round(y))
        if 0 <= x < 16 and 0 <= y < 16: self.im.putpixel((x, y), c)
    def ellipse(self, cx, cy, rx, ry, c, shade=True):
        for x in range(16):
            for y in range(16):
                if ((x + .5 - cx) / rx) ** 2 + ((y + .5 - cy) / ry) ** 2 <= 1:
                    col = c
                    if shade:
                        if x + .5 < cx - rx * 0.3 and y + .5 < cy - ry * 0.2: col = light(c, 0.3)
                        elif y + .5 > cy + ry * 0.4: col = dark(c, 0.2)
                    self.set(x, y, col)
    def rect(self, x0, y0, x1, y1, c):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1): self.set(x, y, c)
    def outline(self, c=(40, 30, 25, 255)):
        px = self.im.load(); pts = []
        for x in range(16):
            for y in range(16):
                if px[x, y][3] == 0 and any(0 <= x+dx < 16 and 0 <= y+dy < 16 and px[x+dx, y+dy][3] > 0 and px[x+dx, y+dy] != c
                                             for dx, dy in ((1,0),(-1,0),(0,1),(0,-1))):
                    pts.append((x, y))
        for p in pts: px[p] = c

G = hx("#4a9a30"); GD = hx("#2f6a20"); GL = hx("#7ac850"); BR = hx("#6a4a2a")

# ================= 作物の成長段階 =================

def stalks(c, h, col=G, xs=(2, 5, 9, 12), lean=0.0):
    for i, x in enumerate(xs):
        hh = h - (i % 2)
        for k in range(hh):
            c.set(x + lean * k * (1 if i % 2 else -1), 15 - k, col if k % 3 else dark(col, 0.15))

def leaves(c, h, xs=(3, 8, 12), col=G):
    for x in xs:
        for k in range(h):
            c.set(x, 15 - k, dark(col, 0.2))
            if k % 2 == 1:
                c.set(x - 1, 15 - k, col); c.set(x + 1, 14 - k, col)

def crop_stage(name, s):
    c = C(); h = [3, 6, 10, 14][s]
    if name in ("rice", "barley"):
        stalks(c, h, G if s < 3 else hx("#9ab040"), lean=0.1)
        if s == 3:
            head = hx("#e0c060") if name == "rice" else hx("#d0a040")
            for x in (2, 5, 9, 12):
                for k in range(4): c.set(x + (1 if name == "rice" and k > 1 else 0), 2 + k, head)
                if name == "barley": c.set(x, 0, light(head, 0.3)); c.set(x + 1, 1, light(head, 0.3))
    elif name == "onion":
        stalks(c, h, GL, xs=(3, 6, 9, 12))
        if s >= 2:
            for x in (3, 9): c.ellipse(x + 1, 14, 2.2 if s == 3 else 1.5, 1.8 if s == 3 else 1.2, hx("#e8d0b0"))
    elif name == "cabbage":
        r = [1.5, 3, 4.5, 6][s]
        c.ellipse(8, 15 - r * 0.6, r, r * 0.8, GL if s == 3 else G)
        if s >= 2: c.ellipse(8, 15 - r * 0.6, r * 0.55, r * 0.45, light(GL, 0.3))
    elif name in ("soybean", "chili", "tomato", "hops"):
        leaves(c, h)
        if s == 3:
            fruit = {"soybean": hx("#9ac060"), "chili": hx("#d82a1a"), "tomato": hx("#e03a2a"), "hops": hx("#c8e888")}[name]
            spots = [(2, 6), (9, 4), (13, 8), (4, 10), (10, 11), (7, 8)]
            for x, y in spots:
                if name == "tomato": c.ellipse(x + .5, y + .5, 1.4, 1.4, fruit)
                elif name == "chili": c.set(x, y, fruit); c.set(x, y + 1, fruit); c.set(x + 1, y + 2, dark(fruit, 0.2))
                else: c.set(x, y, fruit); c.set(x + 1, y, fruit); c.set(x, y + 1, dark(fruit, 0.15)); c.set(x + 1, y + 1, fruit)
    elif name in ("tea", "coffee"):  # 低い木：茶の葉／赤いコーヒーの実
        trunk_h = [1, 2, 3, 3][s]
        for k in range(trunk_h): c.set(7, 15 - k, BR); c.set(8, 15 - k, dark(BR, 0.2))
        r = [2, 4, 5.5, 6.5][s]
        c.ellipse(7.5, 15 - trunk_h - r * 0.55, r, r * 0.7, hx("#2f7a2a") if name == "tea" else G)
        if s >= 2 and name == "tea":
            for x, y in ((5, 8), (9, 6), (11, 9), (6, 11)): c.set(x, y, hx("#8ad870"))
        if s == 3 and name == "coffee":
            for x, y in ((4, 9), (6, 6), (9, 8), (11, 6), (12, 10), (7, 11)):
                c.set(x, y, hx("#c8201a")); c.set(x + 1, y, hx("#a01010"))
    else:  # 果樹：grape lemon ume
        trunk_h = [2, 4, 6, 7][s]
        for k in range(trunk_h): c.set(7, 15 - k, BR); c.set(8, 15 - k, dark(BR, 0.2))
        if s >= 1:
            r = [0, 3, 5, 6.5][s]
            c.ellipse(7.5, 15 - trunk_h - r * 0.5, r, r * 0.75, G)
        if name == "ume" and s == 2:
            for x, y in ((5, 6), (9, 5), (7, 8), (10, 8)): c.set(x, y, hx("#ffb0c8"))
        if s == 3:
            fruit = {"grape": hx("#6a3a8a"), "lemon": hx("#ffe040"), "ume": hx("#c8d850")}[name]
            for x, y in ((3, 6), (11, 5), (6, 3), (9, 8), (4, 9), (12, 9)):
                if name == "grape":
                    for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1), (0, 2)): c.set(x + dx, y + dy, fruit if (dx + dy) % 2 == 0 else light(fruit, 0.2))
                else:
                    c.set(x, y, fruit); c.set(x + 1, y, fruit); c.set(x, y + 1, dark(fruit, 0.15)); c.set(x + 1, y + 1, fruit)
    return c.im

# ================= アイテム =================

def it(f):
    c = C(); f(c); c.outline(); return c.im

def grains(c, col, n=14, seed=1, cx=8, cy=10, r=5):
    rnd = random.Random(seed)
    for _ in range(n):
        a = rnd.random() * 6.28; d = rnd.random() * r
        x, y = cx + math.cos(a) * d, cy + math.sin(a) * d * 0.6
        c.set(x, y, col); c.set(x + 1, y, light(col, 0.25))

def seeds_of(col):
    return lambda c: grains(c, hx(col), n=7, seed=3, r=4)

def sapling_of(col):
    def f(c):
        for k in range(6): c.set(8, 15 - k, BR)
        c.ellipse(6, 8, 2.5, 1.6, G); c.ellipse(10.5, 7, 2.5, 1.6, GL); c.ellipse(8, 5, 2, 1.8, G)
        c.set(8, 4, hx(col))
    return f

ITEMS = {
    "rice": lambda c: (c.ellipse(8, 10.5, 6, 3.5, hx("#f0ece0")), c.ellipse(8, 8.5, 3.5, 2.5, hx("#faf8f0")), grains(c, hx("#d8d0b8"), n=6, r=4.5)),
    "onion": lambda c: (c.ellipse(8, 10, 5, 4.5, hx("#d8b080")), c.rect(7, 3, 8, 5, GL), c.set(6, 10, hx("#b08050"))),
    "soybean": lambda c: (c.ellipse(8, 9, 6, 2.6, hx("#8ab050")), [c.ellipse(x, 9, 1.3, 1.3, hx("#c0d880")) for x in (5, 8, 11)]),
    "barley": lambda c: ([c.set(4 + k * 0.6, 14 - k, hx("#9a8040")) for k in range(10)],
                         [c.ellipse(9 + k * 0.3, 8 - k * 1.2, 1.6, 1.1, hx("#d8b050")) for k in range(5)],
                         [c.set(12, 1 + k, hx("#f0d890")) for k in range(3)]),
    "tomato": lambda c: (c.ellipse(8, 9.5, 5.5, 5, hx("#e03a2a")), c.rect(6, 4, 10, 4, G), c.rect(8, 3, 8, 5, GD)),
    "cabbage": lambda c: (c.ellipse(8, 9, 6, 5.5, hx("#8ac860")), c.ellipse(8, 9, 3.5, 3.2, hx("#c8e8a0")), c.set(8, 9, hx("#e8f8c8"))),
    "chili": lambda c: ([c.ellipse(5 + k, 4 + k * 1.1, 1.8, 1.6, hx("#d82a1a")) for k in range(8)], c.rect(4, 2, 5, 3, G)),
    "hops": lambda c: (c.ellipse(8, 9, 3.8, 5.5, hx("#b8e080")), [c.set(x, y, hx("#7aa840")) for x, y in ((7, 6), (9, 8), (7, 10), (9, 12))], c.rect(8, 2, 8, 3, GD)),
    "grape": lambda c: ([c.ellipse(x, y, 1.6, 1.6, hx("#6a3a8a")) for x, y in ((6, 6), (9, 6), (12, 6), (7.5, 9), (10.5, 9), (9, 12))], c.rect(9, 2, 9, 4, BR), c.ellipse(11, 3, 2, 1.2, G)),
    "lemon": lambda c: (c.ellipse(8, 9, 6, 4.5, hx("#ffe040")), c.set(2, 9, hx("#e0c020")), c.set(14, 9, hx("#e0c020")), c.set(10, 4, G)),
    "ume": lambda c: (c.ellipse(8, 9, 5, 5, hx("#c8d850")), c.ellipse(10, 11, 2, 2, hx("#e88070")), c.rect(8, 3, 8, 4, BR), c.set(9, 3, G)),
    "tea": lambda c: ([c.ellipse(x, y, 2.6, 1.6, hx("#3a8a2a")) for x, y in ((6, 6), (10, 9), (6, 11))], [c.set(x, y, hx("#8ad870")) for x, y in ((5, 5), (9, 8), (5, 10))]),
    "coffee": lambda c: ([c.ellipse(x, y, 2.0, 1.6, hx("#5a3018")) for x, y in ((5, 7), (10, 6), (7, 11), (11, 11))], [c.set(x, y, hx("#2a1408")) for x, y in ((5, 7), (10, 6), (7, 11), (11, 11))]),
    "tea_sapling": sapling_of("#8ad870"), "coffee_sapling": sapling_of("#c8201a"),
    "matcha_powder": lambda c: (c.rect(3, 9, 12, 13, hx("#c8a060")), c.ellipse(7.5, 9, 4.5, 2, hx("#6ab038")), c.ellipse(7.5, 8.4, 2.5, 1.2, hx("#8ad050"))),
    "tomato_seeds": seeds_of("#f0d8a0"), "cabbage_seeds": seeds_of("#3a2a20"), "chili_seeds": seeds_of("#f0e0a0"), "hops_seeds": seeds_of("#7a9a40"),
    "grape_sapling": sapling_of("#6a3a8a"), "lemon_sapling": sapling_of("#ffe040"), "ume_sapling": sapling_of("#ffb0c8"),
    "cooked_rice": lambda c: (c.ellipse(8, 8, 6, 3, hx("#f8f8f4")), [c.set(x, y, hx("#e8e4d8")) for x, y in ((5, 7), (9, 6), (11, 8))],
                              c.rect(3, 9, 12, 12, hx("#3a5a8a")), c.rect(4, 13, 11, 13, hx("#2a4070")), c.rect(5, 10, 10, 10, hx("#e0e8f0"))),
    "flour": lambda c: (c.ellipse(8, 10, 5.5, 5, hx("#e8e0d0")), c.rect(6, 3, 10, 5, hx("#d8ccb0")), c.rect(7, 5, 9, 5, hx("#a08060")), c.rect(6, 9, 10, 11, hx("#c8a060"))),
    "dough": lambda c: (c.ellipse(8, 10, 6, 4, hx("#f0d8a8")), c.set(6, 8, hx("#fff0d0")), c.set(10, 9, hx("#d8b880"))),
    "salt": lambda c: (c.ellipse(8, 11, 6, 3.5, hx("#f8f8f8")), c.ellipse(8, 8, 3.5, 2.5, hx("#ffffff")), [c.set(x, y, hx("#d8e0e8")) for x, y in ((5, 11), (10, 12), (8, 9))]),
    "koji": lambda c: (c.rect(2, 9, 13, 13, hx("#a07a48")), c.ellipse(8, 9, 5.5, 2.5, hx("#f4f0d8")), [c.set(x, y, hx("#e8e0a0")) for x, y in ((5, 8), (9, 9), (11, 8))]),
    "miso": lambda c: (c.rect(2, 7, 13, 13, hx("#a07a48")), c.rect(3, 7, 12, 9, hx("#8a5a30")), c.ellipse(8, 7.5, 4, 1.5, hx("#a86a38"))),
    "soy_sauce": lambda c: (c.rect(5, 6, 10, 14, hx("#2a1810")), c.rect(6, 3, 9, 5, hx("#c03030")), c.rect(6, 9, 9, 11, hx("#f0e8d0")), c.rect(7, 2, 8, 2, hx("#c03030"))),
    "butter": lambda c: (c.rect(3, 8, 12, 12, hx("#f8e070")), c.rect(3, 8, 12, 8, hx("#fff4b0")), c.rect(13, 9, 13, 12, hx("#d8b840"))),
    "cheese": lambda c: ([c.rect(3 + k, 12 - k, 13, 12, hx("#f8d040")) for k in range(6)], c.rect(3, 13, 13, 13, hx("#d0a020")),
                         [c.set(x, y, hx("#d0a020")) for x, y in ((9, 10), (11, 8), (6, 11))]),
    "dashi": lambda c: (c.ellipse(8, 8, 6.5, 2.5, hx("#e8c070")), c.rect(2, 9, 13, 12, hx("#8a5a30")), c.rect(4, 13, 11, 13, hx("#6a4020")), c.set(6, 7, hx("#3a6a3a"))),
    "malt": lambda c: (c.ellipse(8, 10.5, 6, 3.5, hx("#c89050")), c.ellipse(8, 8.5, 3.5, 2.5, hx("#d8a868")), grains(c, hx("#8a6030"), n=7, r=4.5)),
    "tomato_sauce": lambda c: (c.rect(4, 5, 11, 14, hx("#c82a1a")), c.rect(5, 3, 10, 4, hx("#e8e8e8")), c.rect(5, 8, 10, 10, hx("#f0e8d0")), c.set(7, 9, hx("#e03a2a"))),
}

CROPS = ["rice", "onion", "soybean", "barley", "tomato", "cabbage", "chili", "hops", "grape", "lemon", "ume", "tea", "coffee"]

if __name__ == "__main__":
    os.makedirs(ITEM, exist_ok=True); os.makedirs(BLOCK, exist_ok=True)
    imgs = []
    for name in CROPS:
        for s in range(4):
            im = crop_stage(name, s); im.save(os.path.join(BLOCK, f"{name}_stage{s}.png")); imgs.append(im)
    for name, f in ITEMS.items():
        im = it(f); im.save(os.path.join(ITEM, name + ".png")); imgs.append(im)
    if "--preview" in sys.argv:
        cols = 12; rows = (len(imgs) + cols - 1) // cols
        sheet = Image.new("RGBA", (cols * 56, rows * 56), (90, 110, 80, 255))
        for i, im in enumerate(imgs):
            big = im.resize((48, 48), Image.NEAREST); sheet.paste(big, ((i % cols) * 56 + 4, (i // cols) * 56 + 4), big)
        sheet.save(sys.argv[-1])
    print(len(imgs))
