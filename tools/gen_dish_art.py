"""
料理と調理台の絵（16×16）を作る道具。
    python3 tools/gen_dish_art.py [--preview 出力.png]
"""
import os, sys, random
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ITEM = os.path.join(ROOT, "src/main/resources/assets/jsn_craft/textures/item/dish")
BLOCK = os.path.join(ROOT, "src/main/resources/assets/jsn_craft/textures/block")

def hx(c):
    c = c.lstrip('#'); return tuple(int(c[i:i+2], 16) for i in (0, 2, 4)) + (255,)
def mix(a, b, t): return tuple(int(round(a[i] * (1 - t) + b[i] * t)) for i in range(3)) + (255,)
def dark(c, t): return mix(c, (0, 0, 0), t)
def light(c, t): return mix(c, (255, 255, 255), t)

class C:
    def __init__(self): self.im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    def set(self, x, y, c):
        if 0 <= x < 16 and 0 <= y < 16: self.im.putpixel((int(x), int(y)), c)
    def ellipse(self, cx, cy, rx, ry, c, shade=None):
        for x in range(16):
            for y in range(16):
                if ((x + .5 - cx) / rx) ** 2 + ((y + .5 - cy) / ry) ** 2 <= 1:
                    self.set(x, y, shade(x, y) if shade else c)
    def rect(self, x0, y0, x1, y1, c):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1): self.set(x, y, c)
    def outline(self, c):
        px = self.im.load(); pts = []
        for x in range(16):
            for y in range(16):
                if px[x, y][3] == 0 and any(0 <= x+dx < 16 and 0 <= y+dy < 16 and px[x+dx, y+dy][3] > 0 and px[x+dx, y+dy] != c
                                             for dx, dy in ((1,0),(-1,0),(0,1),(0,-1))):
                    pts.append((x, y))
        for p in pts: px[p] = c

def plate(c, color="#e8e8ec"):
    p = hx(color)
    c.ellipse(8, 11.5, 7.2, 3.6, p, lambda x, y: dark(p, 0.15) if y > 12 else p)
def bowl(c, soup, color="#8a5a30"):
    b = hx(color); s = hx(soup)
    c.ellipse(8, 9, 6.8, 2.6, s, lambda x, y: light(s, 0.15) if y < 8 else s)
    for x in range(16):
        for y in range(10, 16):
            if ((x + .5 - 8) / 6.8) ** 2 + ((y + .5 - 9.5) / 5.0) ** 2 <= 1 and y >= 10: c.set(x, y, b if y < 13 else dark(b, 0.2))
    c.rect(5, 14, 10, 14, dark(b, 0.35))

def dots(c, pts, col):
    for p in pts: c.set(p[0], p[1], hx(col))

DISHES = {}
def dish(f): DISHES[f.__name__] = f; return f

@dish
def grilled_fish_set(c):
    c.rect(1, 9, 14, 14, hx("#a0703a")); c.rect(1, 14, 14, 14, hx("#7a5028"))
    c.ellipse(6, 10, 4.2, 1.8, hx("#c88a40"), lambda x, y: hx("#8a5a20") if (x + y) % 3 == 0 else hx("#c88a40"))
    dots(c, [(2, 10)], "#202020"); c.set(10, 9, hx("#8a5a20")); c.set(11, 10, hx("#8a5a20")); c.set(10, 11, hx("#8a5a20"))
    c.ellipse(12.5, 8.5, 2.2, 2.0, hx("#f4f4f0")); c.rect(11, 10, 14, 11, hx("#5a3a2a"))
    dots(c, [(3, 12), (4, 13), (5, 12)], "#3a7a3a")
@dish
def vegetable_soup(c):
    bowl(c, "#e07a30"); dots(c, [(5, 8), (9, 9), (11, 8), (7, 9)], "#4aa040"); dots(c, [(6, 9), (10, 8)], "#f0c040")
@dish
def meat_stew(c):
    bowl(c, "#7a4020"); dots(c, [(5, 8), (8, 8), (10, 9)], "#a05030"); dots(c, [(6, 9), (11, 8)], "#e09030"); dots(c, [(9, 7)], "#c0a080")
@dish
def honey_toast(c):
    plate(c)
    t = hx("#d8a058"); c.rect(4, 4, 11, 11, t); c.rect(4, 4, 11, 4, dark(t, 0.3)); c.rect(4, 4, 4, 11, dark(t, 0.3)); c.rect(11, 4, 11, 11, dark(t, 0.3)); c.rect(4, 11, 11, 11, dark(t, 0.3))
    c.rect(5, 5, 10, 10, hx("#f0d090"))
    for p in [(6, 6), (7, 7), (8, 6), (9, 8), (7, 9), (6, 8)]: c.set(*p, hx("#ffb020"))
@dish
def fish_and_chips(c):
    plate(c)
    c.ellipse(6, 9, 4.0, 2.2, hx("#e0a040"), lambda x, y: hx("#c07a20") if (x * 2 + y) % 4 == 0 else hx("#e0a040"))
    for x in (10, 12, 14):
        for y in range(5, 11): c.set(x - (y % 2), y, hx("#ffd860"))
@dish
def berry_tart(c):
    c.ellipse(8, 10, 7, 4.2, hx("#c08040"), lambda x, y: hx("#a06030") if y > 11 else hx("#c08040"))
    c.ellipse(8, 9.5, 5.5, 3.0, hx("#f0e0b0"))
    for p in [(5, 9), (7, 8), (9, 9), (11, 9), (8, 10), (6, 10), (10, 10), (8, 8)]: c.set(*p, hx("#c02030"))
    dots(c, [(7, 7), (9, 7)], "#40a040")
@dish
def sashimi_platter(c):
    plate(c, "#d0e0f0")
    for i, col in enumerate(["#f08070", "#ff9a50", "#f8f0e8", "#f08070"]):
        x = 3 + i * 3
        c.rect(x, 8, x + 2, 11, hx(col)); c.rect(x, 8, x + 2, 8, light(hx(col), 0.3))
    dots(c, [(13, 7), (14, 8), (13, 8), (2, 7), (2, 8)], "#40a040")
@dish
def miner_lunch(c):
    b = hx("#3a3a40"); c.rect(1, 4, 14, 14, b); c.rect(2, 5, 13, 13, hx("#5a5a62"))
    c.rect(2, 5, 7, 13, hx("#f4f4f0")); dots(c, [(4, 8), (5, 10)], "#202020")
    c.rect(8, 5, 13, 8, hx("#a05030")); c.rect(8, 10, 13, 13, hx("#e07a30")); c.rect(8, 9, 13, 9, b)
    c.rect(1, 4, 14, 4, dark(b, 0.3))
@dish
def warrior_steak(c):
    plate(c)
    c.ellipse(7, 9, 5, 3.0, hx("#7a3a20"), lambda x, y: hx("#3a1a10") if (x - y) % 3 == 0 else hx("#8a4a28"))
    c.ellipse(12.5, 9, 2.0, 1.8, hx("#e0c070")); c.set(12, 8, hx("#fff0c0"))
    dots(c, [(3, 11), (4, 12)], "#40a040")
@dish
def golden_feast(c):
    plate(c, "#f0c840")
    c.ellipse(6, 9, 3.5, 2.5, hx("#ffd040"), lambda x, y: light(hx("#ffd040"), 0.4) if (x + y) % 4 == 0 else hx("#e8b020"))
    c.ellipse(11.5, 8.5, 2.5, 2.5, hx("#ffd040")); c.set(11, 6, hx("#6a4a20")); c.set(12, 5, hx("#40a040"))
    dots(c, [(2, 4), (14, 3), (8, 4)], "#fffbe0"); dots(c, [(3, 3), (13, 4)], "#fff070")

def station():
    os.makedirs(BLOCK, exist_ok=True)
    rnd = random.Random(1)
    brick, mortar = hx("#8a4a32"), hx("#5a4a42")
    def bricks(im):
        for y in range(16):
            for x in range(16):
                row = y // 4; off = 4 if row % 2 else 0
                if y % 4 == 3 or (x + off) % 8 == 7: im.putpixel((x, y), mortar)
                else: im.putpixel((x, y), dark(brick, rnd.random() * 0.15))
    wood = hx("#a07a48")
    side = Image.new("RGBA", (16, 16)); bricks(side)
    for x in range(16):
        for y in range(0, 3): side.putpixel((x, y), dark(wood, 0.1 + (0.15 if y == 2 else 0)))
    side.save(os.path.join(BLOCK, "cooking_station_side.png"))
    for on in (False, True):
        f = side.copy()
        for x in range(4, 12):
            for y in range(8, 15): f.putpixel((x, y), hx("#1a1412"))
        for x in range(3, 13): f.putpixel((x, 7), hx("#3a3a3a"))
        if on:
            for x in range(5, 11):
                for y in range(10, 15):
                    f.putpixel((x, y), hx("#ffd040") if y > 12 else hx("#ff7a20") if (x + y) % 2 else hx("#ffb030"))
        f.save(os.path.join(BLOCK, "cooking_station_front" + ("_on" if on else "") + ".png"))
    top = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            top.putpixel((x, y), dark(wood, 0.25) if y % 4 == 3 else dark(wood, rnd.random() * 0.1))
    tc = C(); tc.im = top
    tc.ellipse(10, 7, 4.5, 4.5, hx("#2a2a2e"), lambda x, y: hx("#4a4a50") if (x + y) < 13 else hx("#2a2a2e"))
    tc.ellipse(10, 7, 3, 3, hx("#c07a30"))
    tc.rect(1, 10, 6, 14, hx("#e0c090")); tc.rect(2, 11, 3, 11, hx("#d04040")); tc.rect(4, 12, 5, 12, hx("#40a040"))
    top.save(os.path.join(BLOCK, "cooking_station_top.png"))

if __name__ == "__main__":
    os.makedirs(ITEM, exist_ok=True)
    imgs = []
    for name, f in DISHES.items():
        c = C(); f(c); c.outline((40, 30, 25, 255))
        c.im.save(os.path.join(ITEM, name + ".png")); imgs.append(c.im)
    station()
    for n in ("cooking_station_top", "cooking_station_side", "cooking_station_front", "cooking_station_front_on"):
        imgs.append(Image.open(os.path.join(BLOCK, n + ".png")))
    if "--preview" in sys.argv:
        sheet = Image.new("RGBA", (len(imgs) * 72, 72), (60, 60, 70, 255))
        for i, im in enumerate(imgs):
            big = im.resize((64, 64), Image.NEAREST); sheet.paste(big, (i * 72 + 4, 4), big)
        sheet.save(sys.argv[-1])
    print(len(DISHES), "品")
