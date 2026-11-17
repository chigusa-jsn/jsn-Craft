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

# ---- 追加の料理（40品）----
def donburi(c, top=None):
    b = hx("#2a3a6a")
    c.ellipse(8, 8.5, 6.5, 2.8, hx("#f8f8f4"))
    for x in range(16):
        for y in range(9, 15):
            if ((x + .5 - 8) / 6.5) ** 2 + ((y + .5 - 9) / 5.2) ** 2 <= 1: c.set(x, y, b if y < 13 else dark(b, 0.3))
    c.rect(2, 10, 13, 10, hx("#c03030"))
    if top: top(c)
def nabe(c, soup, items):
    p = hx("#3a3a40"); s = hx(soup)
    c.ellipse(8, 8.5, 6.8, 2.8, s)
    for x in range(1, 15):
        for y in range(10, 15): c.set(x, y, p if y < 13 else dark(p, 0.3))
    c.rect(0, 10, 0, 11, p); c.rect(15, 10, 15, 11, p)
    dots(c, items, "#ffffff") if False else None
    for (x, y, col) in items: c.set(x, y, hx(col))
def glass(c, liquid, extra=None):
    g = hx("#d8e8f0"); l = hx(liquid)
    c.rect(5, 3, 10, 14, g); c.rect(6, 5, 9, 13, l); c.rect(6, 5, 9, 5, light(l, 0.4)); c.rect(5, 14, 10, 14, dark(g, 0.2))
    if extra: extra(c)
def skewer(c, cols):
    for k in range(14): c.set(1 + k, 14 - k, hx("#a08050"))
    for i, col in enumerate(cols):
        x, y = 4 + i * 3, 11 - i * 3
        c.ellipse(x + .5, y + .5, 1.8, 1.8, hx(col))
def blob(c, x, y, rx, ry, col, stripes=None):
    c.ellipse(x, y, rx, ry, hx(col), lambda px, py: hx(stripes) if stripes and (px + py) % 3 == 0 else hx(col))

@dish
def onigiri(c):
    for y in range(3, 14):
        w = (y - 2) * 0.6
        for x in range(16):
            if abs(x + .5 - 8) <= w: c.set(x, y, hx("#f8f8f4") if y < 10 else hx("#ececE4".lower()))
    c.rect(4, 10, 11, 13, hx("#1a2a1a")); c.rect(5, 11, 6, 11, hx("#2a4a2a"))
@dish
def edamame(c):
    bowl(c, "#e8e0c8", "#b07a40")
    for x, y in ((4, 7), (7, 6), (10, 7), (6, 8), (9, 8), (12, 8)): c.ellipse(x + .5, y + .5, 1.4, 0.9, hx("#7ab040"))
@dish
def miso_soup(c):
    bowl(c, "#c89050", "#3a1a1a"); dots(c, [(5, 8), (9, 7), (11, 9)], "#f0f0e8"); dots(c, [(7, 8), (10, 8)], "#4a8a3a")
@dish
def salad(c):
    bowl(c, "#7ac850", "#e8e8ec"); dots(c, [(5, 7), (10, 8), (8, 7)], "#e03a2a"); dots(c, [(6, 9), (11, 7)], "#f0e8f0"); dots(c, [(9, 9)], "#c8e8a0")
@dish
def pancake(c):
    plate(c)
    for i in range(3): blob(c, 8, 10 - i * 2, 5.5, 1.6, "#d8a058")
    c.rect(6, 4, 9, 5, hx("#fff4b0")); dots(c, [(10, 6), (5, 7), (11, 8)], "#c07a20")
@dish
def lemonade(c):
    glass(c, "#fff070", lambda c: (c.ellipse(11, 4, 2.2, 2.2, hx("#ffe040")), c.set(11, 4, hx("#fff8c0")), c.rect(8, 1, 8, 6, hx("#e04040"))))
@dish
def mushroom_omelette(c):
    plate(c); blob(c, 8, 9, 5.5, 3, "#ffd860"); dots(c, [(6, 8), (9, 9), (11, 8)], "#8a5a3a")
@dish
def yaki_onigiri(c):
    for y in range(3, 14):
        w = (y - 2) * 0.6
        for x in range(16):
            if abs(x + .5 - 8) <= w: c.set(x, y, hx("#c88a40") if (x + y) % 4 else hx("#a06a28"))
@dish
def cabbage_roll(c):
    bowl(c, "#d84a30", "#e8e8ec")
    for x in (5, 10): blob(c, x, 7.5, 2.4, 1.6, "#a8d880")
@dish
def curry_rice(c):
    plate(c); blob(c, 5.5, 9, 3.5, 2.6, "#f8f8f4"); blob(c, 10.5, 9.5, 4, 2.6, "#a8681a"); dots(c, [(10, 9), (12, 10), (9, 10)], "#e07a30")
@dish
def sushi(c):
    c.rect(1, 10, 14, 13, hx("#a0703a")); c.rect(1, 13, 14, 13, hx("#7a5028"))
    for x, col in ((2, "#f08070"), (7, "#ff9a50"), (11, "#f8f0e8")):
        c.rect(x, 8, x + 3, 9, hx("#f8f8f4")); c.rect(x, 6, x + 3, 7, hx(col))
@dish
def omurice(c):
    plate(c); blob(c, 8, 8.5, 6, 3.2, "#ffd040"); c.rect(5, 7, 11, 7, hx("#d03a2a")); c.rect(6, 8, 7, 8, hx("#d03a2a"))
@dish
def ramen(c):
    bowl(c, "#d8a048", "#c03030"); dots(c, [(5, 8), (6, 7), (7, 8), (8, 7)], "#f0e080"); blob(c, 11, 7.5, 1.6, 1.2, "#f8f8f0"); c.set(11, 7, hx("#ffb020")); dots(c, [(9, 9)], "#3a7a3a")
@dish
def pizza(c):
    blob(c, 8, 9, 7, 4.5, "#d89048"); blob(c, 8, 9, 5.8, 3.6, "#f0c040")
    dots(c, [(5, 8), (9, 7), (11, 10), (7, 10)], "#c02a1a"); dots(c, [(8, 9), (12, 8)], "#8a6a4a")
@dish
def tempura(c):
    plate(c)
    for i, x in enumerate((4, 8, 12)): blob(c, x, 8.5 - (i % 2), 1.8, 3.6, "#f0c860", "#d8a040")
@dish
def grape_jelly(c):
    c.rect(4, 5, 11, 13, hx("#8a4aa8")); c.rect(5, 5, 10, 6, hx("#b07ad0")); c.rect(3, 13, 12, 14, hx("#e8e8ec")); dots(c, [(6, 8), (9, 10)], "#c8a0e0")
@dish
def gyudon(c):
    donburi(c, lambda c: (blob(c, 8, 7.8, 5, 2, "#8a4a28", "#a8683a"), dots(c, [(6, 7), (10, 8)], "#f0e0c0")))
@dish
def hamburger(c):
    blob(c, 8, 5, 6, 2.5, "#d89048"); c.rect(2, 7, 13, 7, hx("#4a9a30")); c.rect(2, 8, 13, 8, hx("#f8d040"))
    c.rect(2, 9, 13, 10, hx("#6a3a20")); c.rect(3, 11, 12, 11, hx("#e03a2a")); blob(c, 8, 12.5, 6, 1.5, "#d89048"); dots(c, [(5, 4), (9, 3), (11, 5)], "#fff4d0")
@dish
def ume_ochazuke(c):
    bowl(c, "#d8d8a8", "#5a7a4a"); blob(c, 8, 7.5, 1.5, 1.2, "#d83a3a"); dots(c, [(5, 8), (11, 8)], "#f8f8f0"); dots(c, [(10, 7)], "#3a6a3a")
@dish
def gratin(c):
    c.rect(1, 8, 14, 13, hx("#f0f0f0")); c.rect(2, 7, 13, 11, hx("#f8d870")); dots(c, [(4, 8), (8, 9), (11, 8), (6, 10)], "#c88a30"); c.rect(1, 13, 14, 13, hx("#c8c8c8"))
@dish
def mapo_tofu(c):
    bowl(c, "#c83a1a", "#f0f0f0")
    for x, y in ((5, 7), (8, 8), (11, 7)): c.rect(x, y, x + 1, y + 1, hx("#f8f4e8"))
    dots(c, [(7, 7), (10, 9)], "#4a8a3a")
@dish
def lemon_chicken(c):
    plate(c); blob(c, 7, 9, 4.5, 2.8, "#d89048", "#b06a28"); blob(c, 12, 8, 1.8, 1.8, "#ffe040"); c.set(12, 8, hx("#fff8c0"))
@dish
def paella(c):
    nabe(c, "#f0c040", [(4, 8, "#e03a2a"), (7, 7, "#f08070"), (10, 8, "#ffe040"), (12, 8, "#3a6a3a"), (6, 9, "#ff9a50")])
@dish
def cheese_fondue(c):
    nabe(c, "#f8e070", [(6, 8, "#fff4b0"), (9, 7, "#fff4b0")])
    for k in range(5): c.set(10 + k * 0.6, 7 - k, hx("#a0a0a8"))
    blob(c, 13.5, 2.5, 1.5, 1.5, "#d89048")
@dish
def toro_don(c):
    donburi(c, lambda c: [c.rect(x, 7, x + 2, 8, hx("#f8a0a0" if i % 2 else "#e86a7a")) for i, x in enumerate((2, 5, 8, 11))])
@dish
def swordfish_steak(c):
    plate(c); blob(c, 7, 9, 5, 2.6, "#e8d8c0", "#c8a880"); blob(c, 12.5, 8, 1.6, 1.6, "#ffe040"); dots(c, [(4, 8)], "#f8e070")
@dish
def anglerfish_hotpot(c):
    nabe(c, "#c89050", [(4, 8, "#f8f0e0"), (7, 7, "#a8d880"), (10, 8, "#f8f0e0"), (12, 7, "#a8d880"), (8, 9, "#e07a30")])
@dish
def eel_kabayaki(c):
    donburi(c, lambda c: (blob(c, 8, 7.6, 5.5, 1.8, "#8a4a1a", "#5a2a0a"), dots(c, [(5, 7), (11, 7)], "#ffe030")))
@dish
def golden_carp_nitsuke(c):
    plate(c, "#2a3a5a"); blob(c, 7, 9, 5, 2.4, "#e0a020", "#b07010"); c.set(3, 8, hx("#202020")); c.rect(12, 8, 13, 10, hx("#e0a020"))
@dish
def glacier_sashimi(c):
    plate(c, "#c8e8f8")
    for i, x in enumerate((3, 6, 9, 12)): c.rect(x, 8, x + 1, 11, hx("#f8b0b8" if i % 2 else "#f8c8c8")); c.set(x, 8, hx("#ffffff"))
    blob(c, 13, 6, 1.4, 1.4, "#ffe040")
@dish
def coelacanth_stew(c):
    nabe(c, "#8a6a40", [(4, 8, "#3a4a6a"), (6, 8, "#3a4a6a"), (8, 7, "#e8eef8"), (11, 8, "#f8f0e0"), (12, 9, "#4a8a3a")])
@dish
def crimson_curry(c):
    plate(c, "#3a2020"); blob(c, 5.5, 9, 3.5, 2.6, "#f8f8f4"); blob(c, 10.5, 9.5, 4, 2.6, "#c82010"); dots(c, [(10, 8), (12, 10)], "#ffb040"); dots(c, [(9, 10)], "#ff6040")
@dish
def soul_eater_skewer(c):
    skewer(c, ["#3a2a20", "#60e0ff", "#3a2a20"]); dots(c, [(5, 10), (11, 4)], "#60e0ff")
@dish
def void_jelly_dessert(c):
    c.rect(4, 5, 11, 13, hx("#5a3a8a")); c.rect(5, 5, 10, 6, hx("#a070d0")); c.rect(3, 13, 12, 14, hx("#2a1a3a")); dots(c, [(6, 8), (9, 10), (8, 7)], "#e0a0ff")
@dish
def moon_lord_sashimi(c):
    plate(c, "#1a2a5a")
    for i, x in enumerate((3, 6, 9, 12)): c.rect(x, 8, x + 1, 11, hx("#e8f0ff" if i % 2 else "#c8d8ff"))
    blob(c, 8, 4, 2.4, 2.4, "#fff6a0"); c.set(9, 3, hx("#1a2a5a")); dots(c, [(2, 3), (13, 2), (14, 5)], "#fff6a0")
@dish
def storm_king_steak(c):
    plate(c, "#303848"); blob(c, 8, 9, 5.5, 2.8, "#5a2a18", "#2a1008")
    for x, y in ((5, 5), (6, 6), (5, 7), (6, 8)): c.set(x, y, hx("#ffe040"))
    for x, y in ((11, 4), (12, 5), (11, 6)): c.set(x, y, hx("#ffe040"))
@dish
def ice_queen_carpaccio(c):
    plate(c, "#a0e0ff")
    for x, y in ((4, 9), (7, 8), (10, 9), (6, 11), (9, 11)): blob(c, x + .5, y + .5, 1.8, 1.2, "#f8d0d8")
    for x in (6, 8, 10): c.set(x, 4, hx("#ffd040"))
    c.rect(6, 5, 10, 5, hx("#ffd040"))
@dish
def inferno_dragon_roast(c):
    plate(c, "#3a1010"); blob(c, 8, 8.5, 6.5, 2.6, "#c02010", "#801000"); c.set(2, 8, hx("#ffff80")); c.rect(13, 6, 14, 10, hx("#ffb020"))
    dots(c, [(5, 4), (9, 3), (12, 4)], "#ff8020"); dots(c, [(7, 5)], "#ffe060")
@dish
def soulflame_nabe(c):
    nabe(c, "#203040", [(4, 8, "#60e0ff"), (7, 7, "#a8d880"), (10, 8, "#60e0ff"), (12, 8, "#f8f0e0")])
    dots(c, [(5, 4), (8, 3), (11, 5)], "#60e0ff")
@dish
def ender_dragon_feast(c):
    c.rect(0, 9, 15, 13, hx("#1a1020")); c.rect(0, 13, 15, 13, hx("#000000"))
    blob(c, 4, 8.5, 3.2, 2, "#a040e0", "#5a2080"); blob(c, 11, 8, 3.5, 2.2, "#f8f8f4"); c.set(11, 7, hx("#d83a3a"))
    dots(c, [(2, 4), (7, 3), (13, 4), (9, 5)], "#ff80ff"); c.ellipse(7.5, 10.5, 1.5, 1, hx("#ffd040"))

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
        cols = 10; rows = (len(imgs) + cols - 1) // cols
        sheet = Image.new("RGBA", (cols * 72, rows * 72), (60, 60, 70, 255))
        for i, im in enumerate(imgs):
            big = im.resize((64, 64), Image.NEAREST); sheet.paste(big, ((i % cols) * 72 + 4, (i // cols) * 72 + 4), big)
        sheet.save(sys.argv[-1])
    print(len(DISHES), "品")
