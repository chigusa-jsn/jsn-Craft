"""
スキルのアイコン（32×32）を作る。
  - 技（アクティブ）は丸い枠、パッシブは四角い枠。枠の色は職業の色。奥義（★・奥義）は金の枠
  - ボードの能力マス用のアイコン（枠なし）：HPは赤いハート、マナは青いハート、スタミナは黄色いハート など
  書き出し先：assets/jsn_craft/textures/gui/skill_icon/<job>/<skill>.png ・ .../stat/<attribute>.png
  スキルの json の "icon" を "jsn_craft:skill_icon/<job>/<skill>" に書きかえる。
    python3 tools/gen_skill_icons.py [--sheet out.png]
絵は 4倍の大きさ（128）でなめらかに描いてから 32 に縮め、ふちをくっきりさせてドット絵にする。
"""
import glob, json, math, os, sys
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src/main/resources")
OUT = os.path.join(RES, "assets/jsn_craft/textures/gui/skill_icon")
K = 4          # 下書きの倍率
N = 32         # 仕上がりの大きさ

C = dict(
    steel="#c8ccd6", steel_d="#7a808c", iron="#a8acb4", gold="#f0c040", gold_d="#b08020", wood="#9a6a3a", wood_d="#6a4424",
    leather="#8a5a34", fire="#ff6a20", fire_y="#ffd040", lava="#ff4010", water="#3a8cff", water_l="#8ad0ff", ice="#a8ecff",
    wind="#b0ffe0", earth="#a07a4a", stone="#8a8a90", stone_d="#5a5a62", holy="#fff4a0", dark="#4a2a6a", shadow="#2a1a3a",
    purple="#b060ff", green="#4ac040", leaf="#58b030", leaf_d="#2e7a20", red="#e83030", blood="#b01818", pink="#ff80b0",
    white="#ffffff", bone="#ece4d0", black="#1a1a1a", blue="#3a6aff", cyan="#40e0e0", orange="#ff9a30", yellow="#ffe040",
    brown="#7a5030", mana="#4a8aff", mana_l="#9ac0ff", gem="#40f0d0", ruby="#ff3050", emerald="#30e070", amethyst="#c070ff",
    poison="#80e040", gray="#9a9a9a", lapis="#2a50c0", star="#fff07a", wheat="#e8c050", carrot="#ff8a20", wool="#f0f0f0",
    meat="#d05040", ghost="#a0f0ff", moon="#d8e8ff", skin="#f0c090",
)

def col(c):
    c = C.get(c, c)
    c = c.lstrip("#")
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4)) + (255,)

def shade(c, k):
    r, g, b, a = col(c) if isinstance(c, str) else c
    if k > 0: return (int(r + (255 - r) * k), int(g + (255 - g) * k), int(b + (255 - b) * k), a)
    return (int(r * (1 + k)), int(g * (1 + k)), int(b * (1 + k)), a)

# ---------------- 下書き用の筆（座標は 0〜32） ----------------
class Pen:
    def __init__(self):
        self.img = Image.new("RGBA", (N * K, N * K), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.img)
    def P(self, pts): return [(x * K, y * K) for x, y in pts]
    def poly(self, pts, c): self.d.polygon(self.P(pts), fill=col(c) if isinstance(c, str) else c)
    def line(self, pts, c, w=1.5): self.d.line(self.P(pts), fill=col(c) if isinstance(c, str) else c, width=max(1, int(w * K)), joint="curve")
    def ell(self, x0, y0, x1, y1, c): self.d.ellipse([x0 * K, y0 * K, x1 * K, y1 * K], fill=col(c) if isinstance(c, str) else c)
    def ring(self, x0, y0, x1, y1, c, w=1.5): self.d.ellipse([x0 * K, y0 * K, x1 * K, y1 * K], outline=col(c) if isinstance(c, str) else c, width=int(w * K))
    def arc(self, x0, y0, x1, y1, a0, a1, c, w=1.5): self.d.arc([x0 * K, y0 * K, x1 * K, y1 * K], a0, a1, fill=col(c) if isinstance(c, str) else c, width=int(w * K))
    def rect(self, x0, y0, x1, y1, c): self.d.rectangle([x0 * K, y0 * K, x1 * K - 1, y1 * K - 1], fill=col(c) if isinstance(c, str) else c)
    def circ(self, x, y, r, c): self.ell(x - r, y - r, x + r, y + r, c)

# ---------------- モチーフ（中心 16,16、だいたい 5〜27 におさめる） ----------------
M = {}
def motif(name):
    def deco(f): M[name] = f; return f
    return deco

@motif("sword")
def _(p, c=None):
    b = c or "steel"
    p.poly([(23, 5), (26, 6), (27, 9), (13, 22), (10, 19)], b)
    p.line([(25, 7), (12, 20)], shade(b, 0.5), 0.8)
    p.line([(8, 17), (15, 24)], "gold", 2.2)
    p.line([(11, 21), (6, 26)], "wood", 2.4)
    p.circ(5.5, 26.5, 1.6, "gold")

@motif("katana")
def _(p, c=None):
    b = c or "steel"
    p.line([(26, 5), (22, 11), (16, 16), (11, 20)], b, 2.6)
    p.line([(26, 5), (22, 10), (16, 15)], shade(b, 0.5), 0.8)
    p.line([(9, 19), (13, 23)], "gold", 2.2)
    p.line([(11, 21), (6, 26)], "black", 2.4)

@motif("dagger")
def _(p, c=None):
    b = c or "steel"
    p.poly([(22, 8), (24, 9), (15, 19), (13, 17)], b)
    p.line([(11, 16), (16, 21)], "gold", 1.8)
    p.line([(13, 19), (9, 23)], "leather", 2.2)

@motif("axe")
def _(p, c=None):
    b = c or "steel"
    p.line([(10, 26), (22, 7)], "wood", 2.4)
    p.poly([(17, 9), (25, 5), (28, 11), (27, 18), (20, 15)], b)
    p.line([(26, 6), (28, 12), (27, 17)], shade(b, 0.5), 0.8)

@motif("spear")
def _(p, c=None):
    p.line([(7, 25), (21, 11)], "wood", 2.0)
    p.poly([(20, 9), (27, 5), (23, 12)], c or "steel")
    p.line([(18, 12), (21, 15)], "gold", 1.4)

@motif("trident")
def _(p, c=None):
    b = c or "cyan"
    p.line([(7, 26), (19, 14)], b, 2.0)
    p.line([(15, 14), (19, 18)], b, 1.6)
    p.line([(16, 15), (24, 7)], b, 1.4); p.line([(14, 13), (19, 6)], b, 1.4); p.line([(18, 17), (26, 12)], b, 1.4)
    p.poly([(23, 5), (26, 6), (25, 9)], b); p.poly([(18, 4), (21, 5), (19, 8)], b); p.poly([(26, 10), (28, 12), (25, 13)], b)

@motif("bow")
def _(p, c=None):
    p.arc(4, 4, 28, 28, 225, 405, c or "wood", 2.4)
    p.line([(7.5, 7.5), (24.5, 24.5)], "white", 0.7)

@motif("arrow")
def _(p, c=None):
    p.line([(7, 25), (24, 8)], "wood", 1.5)
    p.poly([(26, 6), (20, 8), (24, 12)], c or "steel")
    p.poly([(6, 22), (4, 27), (9, 26), (10, 23)], "white")

@motif("shield")
def _(p, c=None):
    b = c or "steel"
    p.poly([(7, 6), (25, 6), (25, 16), (16, 27), (7, 16)], b)
    p.poly([(9, 8), (23, 8), (23, 15.5), (16, 24.5), (9, 15.5)], shade(b, -0.3))
    p.line([(16, 9), (16, 23)], "gold", 1.6); p.line([(10, 13), (22, 13)], "gold", 1.6)

@motif("fist")
def _(p, c=None):
    b = c or "skin"
    p.rect(9, 10, 23, 22, b)
    for x in (9, 12.5, 16, 19.5): p.rect(x + 0.3, 8, x + 3.2, 13, shade(b, 0.15))
    p.rect(6, 14, 10, 20, shade(b, -0.1))
    p.rect(10, 22, 22, 26, c and shade(c, -0.3) or "red")

@motif("boot")
def _(p, c=None):
    b = c or "leather"
    p.poly([(10, 6), (17, 6), (17, 19), (25, 21), (25, 26), (10, 26)], b)
    p.rect(10, 6, 17, 9, shade(b, 0.3))

@motif("whip")
def _(p, c=None):
    p.line([(6, 26), (11, 21)], "leather", 2.6)
    p.line([(11, 21), (16, 13), (24, 9), (27, 14), (22, 19)], c or "brown", 1.4)

@motif("staff")
def _(p, c=None):
    p.line([(8, 26), (21, 11)], "wood", 2.2)
    p.circ(23, 9, 4, c or "amethyst")
    p.circ(22, 8, 1.4, "white")

@motif("book")
def _(p, c=None):
    b = c or "purple"
    p.poly([(7, 9), (16, 11), (16, 26), (7, 24)], b); p.poly([(16, 11), (25, 9), (25, 24), (16, 26)], shade(b, -0.2))
    p.line([(9, 13), (14, 14)], "white", 0.8); p.line([(18, 14), (23, 13)], "white", 0.8)
    p.line([(9, 16), (14, 17)], "white", 0.8); p.line([(18, 17), (23, 16)], "white", 0.8)

@motif("hammer")
def _(p, c=None):
    p.line([(8, 26), (19, 13)], "wood", 2.2)
    p.poly([(14, 8), (20, 4), (27, 12), (21, 16)], c or "iron")

@motif("pickaxe")
def _(p, c=None):
    p.line([(8, 26), (21, 11)], "wood", 2.2)
    p.arc(8, 4, 30, 22, 200, 340, c or "steel", 2.6)

@motif("hoe")
def _(p, c=None):
    p.line([(8, 26), (21, 9)], "wood", 2.2)
    p.poly([(18, 7), (27, 6), (27, 10), (21, 11)], c or "iron")

@motif("scythe")
def _(p, c=None):
    p.line([(9, 27), (19, 6)], "wood", 2.0)
    p.poly([(18, 6), (10, 5), (5, 9), (12, 7.5), (18, 9)], c or "gold")

@motif("rod")
def _(p, c=None):
    p.line([(6, 27), (23, 6)], c or "wood", 1.6)
    p.line([(23, 6), (25, 18)], "white", 0.6)
    p.circ(25, 20, 1.8, "red")

@motif("fish")
def _(p, c=None):
    b = c or "water_l"
    p.ell(7, 11, 23, 21, b)
    p.poly([(21, 16), (28, 10), (27, 22)], shade(b, -0.2))
    p.circ(11.5, 15, 1.2, "black")

@motif("net")
def _(p, c=None):
    b = c or "white"
    for i in range(5):
        p.line([(6 + i * 5, 6), (6 + i * 5, 26)], b, 0.7); p.line([(6, 6 + i * 5), (26, 6 + i * 5)], b, 0.7)

@motif("wheat")
def _(p, c=None):
    b = c or "wheat"
    p.line([(10, 27), (17, 12)], "leaf", 1.4)
    for i in range(5):
        y = 7 + i * 3.2; x = 17 - i * 1.2
        p.ell(x - 3.6, y - 1.6, x, y + 1.6, b); p.ell(x, y - 1.6, x + 3.6, y + 1.6, b)

@motif("seed")
def _(p, c=None):
    for x, y in ((11, 12), (19, 10), (15, 18), (22, 19), (9, 21)):
        p.ell(x - 2, y - 1.4, x + 2, y + 1.4, c or "wheat")

@motif("sprout")
def _(p, c=None):
    p.line([(16, 27), (16, 14)], "leaf_d", 1.6)
    p.poly([(16, 15), (8, 9), (7, 14), (12, 16)], c or "leaf"); p.poly([(16, 13), (24, 6), (26, 11), (20, 14)], c or "leaf")
    p.rect(9, 25, 23, 28, "brown")

@motif("tree")
def _(p, c=None):
    p.rect(14, 18, 18, 27, "wood")
    p.circ(16, 12, 8, c or "leaf"); p.circ(13, 10, 3, shade(c or "leaf", 0.25))

@motif("leaf")
def _(p, c=None):
    p.poly([(7, 25), (8, 13), (16, 6), (26, 6), (25, 15), (18, 23)], c or "leaf")
    p.line([(8, 24), (22, 10)], shade(c or "leaf", -0.35), 1.0)

@motif("log")
def _(p, c=None):
    p.rect(7, 10, 25, 22, c or "wood"); p.ell(21, 10, 29, 22, "wheat"); p.ring(23, 13, 27, 19, "wood", 0.8)

@motif("flower")
def _(p, c=None):
    b = c or "yellow"
    for a in range(0, 360, 45):
        x, y = 16 + 7 * math.cos(math.radians(a)), 16 + 7 * math.sin(math.radians(a))
        p.circ(x, y, 3.6, b)
    p.circ(16, 16, 4.4, "brown")

@motif("carrot")
def _(p, c=None):
    p.poly([(9, 24), (19, 10), (24, 15)], c or "carrot")
    p.poly([(19, 10), (22, 4), (23, 9)], "leaf"); p.poly([(21, 12), (28, 9), (24, 14)], "leaf")

@motif("apple")
def _(p, c=None):
    p.circ(16, 18, 8, c or "red"); p.line([(16, 10), (17, 6)], "wood", 1.2); p.poly([(17, 8), (23, 5), (21, 9)], "leaf")
    p.circ(13, 15, 2, "white")

@motif("potion")
def _(p, c=None):
    p.rect(13, 5, 19, 11, "ice"); p.rect(12, 4, 20, 6, "wood")
    p.circ(16, 19, 8, "ice"); p.ell(9, 17, 23, 27, c or "pink"); p.circ(13, 17, 1.6, "white")

@motif("cauldron")
def _(p, c=None):
    p.ell(6, 9, 26, 27, "stone_d"); p.ell(7, 8, 25, 13, c or "green")
    p.rect(8, 25, 11, 28, "stone_d"); p.rect(21, 25, 24, 28, "stone_d")
    p.circ(13, 8, 1.6, shade(c or "green", 0.4)); p.circ(19, 6, 1.2, shade(c or "green", 0.4))

@motif("gem")
def _(p, c=None):
    b = c or "gem"
    p.poly([(9, 11), (13, 6), (19, 6), (23, 11), (16, 26)], b)
    p.poly([(9, 11), (23, 11), (16, 26)], shade(b, -0.25))
    p.poly([(13, 6), (16, 11), (19, 6)], shade(b, 0.45))

@motif("ring")
def _(p, c=None):
    p.ring(8, 12, 24, 27, c or "gold", 2.4)
    p.poly([(12, 11), (16, 5), (20, 11), (16, 14)], "ruby")

@motif("crown")
def _(p, c=None):
    b = c or "gold"
    p.poly([(6, 22), (6, 10), (11, 16), (16, 7), (21, 16), (26, 10), (26, 22)], b)
    p.rect(6, 22, 26, 26, shade(b, -0.25)); p.circ(16, 19, 1.8, "ruby"); p.circ(10, 19, 1.3, "gem"); p.circ(22, 19, 1.3, "gem")

@motif("anvil")
def _(p, c=None):
    b = c or "stone_d"
    p.poly([(5, 9), (25, 9), (27, 13), (20, 14), (20, 19), (24, 24), (8, 24), (12, 19), (12, 14), (5, 12)], b)
    p.line([(6, 10), (25, 10)], shade(b, 0.4), 0.8)

@motif("ingot")
def _(p, c=None):
    b = c or "iron"
    p.poly([(6, 22), (10, 13), (26, 13), (26, 17), (22, 22)], b); p.poly([(10, 13), (14, 9), (28, 9), (26, 13)], shade(b, 0.35))
    p.poly([(26, 13), (28, 9), (28, 13), (26, 17)], shade(b, -0.2))

@motif("flame")
def _(p, c=None):
    b = c or "fire"
    p.poly([(16, 4), (24, 14), (25, 21), (21, 27), (11, 27), (7, 21), (9, 14), (12, 17), (13, 10)], b)
    p.poly([(16, 12), (21, 19), (19, 26), (13, 26), (11, 21)], "fire_y")

@motif("fireball")
def _(p, c=None):
    b = c or "fire"
    p.poly([(5, 27), (12, 14), (14, 18), (18, 10), (16, 22)], shade(b, -0.1))
    p.circ(19, 13, 7, b); p.circ(20, 12, 4, "fire_y")

@motif("drop")
def _(p, c=None):
    b = c or "water"
    p.poly([(16, 4), (23, 15), (9, 15)], b); p.circ(16, 18, 7.6, b); p.circ(13, 17, 2, shade(b, 0.5))

@motif("wave")
def _(p, c=None):
    b = c or "water"
    p.poly([(4, 26), (4, 18), (9, 12), (16, 9), (23, 10), (27, 15), (22, 14), (18, 16), (20, 21), (28, 22), (28, 26)], b)
    p.line([(9, 15), (16, 11), (23, 12)], "white", 1.2)

@motif("spiral")
def _(p, c=None):
    pts = [(16 + (1 + t * 0.55) * math.cos(t), 16 + (1 + t * 0.55) * math.sin(t)) for t in [i * 0.2 for i in range(0, 92)]]
    p.line(pts, c or "water", 1.8)

@motif("wind")
def _(p, c=None):
    b = c or "wind"
    p.line([(5, 11), (19, 11)], b, 1.6); p.arc(15, 5, 25, 13, 270, 450, b, 1.6)
    p.line([(4, 17), (24, 17)], b, 1.6); p.arc(20, 13, 28, 21, 270, 450, b, 1.6)
    p.line([(7, 23), (17, 23)], b, 1.6); p.arc(13, 19, 21, 27, 270, 420, b, 1.6)

@motif("tornado")
def _(p, c=None):
    b = c or "wind"
    for i, (w, y) in enumerate(((11, 6), (9, 10.5), (7, 15), (5, 19.5), (3.5, 24))):
        x = 16 + (i % 2) * 1.5 - 0.75
        p.line([(x - w, y), (x + w, y)], b, 2.4)

@motif("rock")
def _(p, c=None):
    b = c or "stone"
    p.poly([(6, 24), (8, 13), (14, 7), (22, 9), (27, 16), (25, 25)], b)
    p.poly([(14, 7), (22, 9), (19, 14), (11, 14)], shade(b, 0.3)); p.line([(14, 16), (17, 21), (22, 19)], shade(b, -0.4), 1)

@motif("wall")
def _(p, c=None):
    b = c or "stone"
    p.rect(5, 8, 27, 26, b)
    for y in (8, 14, 20):
        p.line([(5, y), (27, y)], shade(b, -0.4), 0.8)
        off = 0 if (y // 6) % 2 else 3.5
        for x in range(5, 27, 7): p.line([(x + off, y), (x + off, y + 6)], shade(b, -0.4), 0.8)

@motif("bolt")
def _(p, c=None):
    p.poly([(19, 3), (8, 18), (15, 18), (11, 29), (25, 12), (17, 12), (22, 3)], c or "yellow")

@motif("cloud")
def _(p, c=None):
    b = c or "gray"
    p.circ(11, 15, 5, b); p.circ(18, 12, 6.5, b); p.circ(23, 16, 4.5, b); p.rect(8, 15, 25, 20.5, b)

@motif("snow")
def _(p, c=None):
    b = c or "ice"
    for a in (0, 60, 120):
        r = math.radians(a)
        p.line([(16 - 11 * math.cos(r), 16 - 11 * math.sin(r)), (16 + 11 * math.cos(r), 16 + 11 * math.sin(r))], b, 1.6)
    p.circ(16, 16, 2.4, "white")

@motif("ice")
def _(p, c=None):
    b = c or "ice"
    p.poly([(16, 3), (21, 12), (19, 28), (13, 28), (11, 12)], b); p.poly([(16, 3), (16, 28), (13, 28), (11, 12)], shade(b, 0.4))
    p.poly([(7, 15), (10, 19), (9, 27), (6, 27)], shade(b, -0.1)); p.poly([(25, 14), (26, 27), (23, 27), (22, 19)], shade(b, -0.1))

@motif("star")
def _(p, c=None):
    b = c or "star"
    pts = []
    for i in range(10):
        r = 11 if i % 2 == 0 else 4.6
        a = math.radians(-90 + i * 36)
        pts.append((16 + r * math.cos(a), 16.5 + r * math.sin(a)))
    p.poly(pts, b)

@motif("sparkle")
def _(p, c=None):
    b = c or "star"
    p.poly([(16, 3), (18.5, 13.5), (29, 16), (18.5, 18.5), (16, 29), (13.5, 18.5), (3, 16), (13.5, 13.5)], b)

@motif("moon")
def _(p, c=None):
    p.circ(16, 16, 11, c or "moon"); p.circ(21, 12, 9.5, (0, 0, 0, 0))

@motif("sun")
def _(p, c=None):
    b = c or "fire_y"
    for a in range(0, 360, 30):
        r = math.radians(a)
        p.line([(16 + 8 * math.cos(r), 16 + 8 * math.sin(r)), (16 + 13 * math.cos(r), 16 + 13 * math.sin(r))], b, 1.6)
    p.circ(16, 16, 7, b); p.circ(14, 14, 2.4, "white")

@motif("meteor")
def _(p, c=None):
    p.poly([(4, 4), (14, 12), (18, 9), (12, 18), (9, 14)], "fire_y")
    p.circ(20, 20, 7.5, c or "lava"); p.circ(18, 18, 3, "fire_y")

@motif("beam")
def _(p, c=None):
    b = c or "holy"
    p.poly([(4, 21), (24, 6), (28, 10), (11, 28)], shade(b, -0.1)); p.line([(7, 23), (25, 8)], "white", 2.0)
    p.circ(6, 25, 4, b)

@motif("pillar")
def _(p, c=None):
    b = c or "holy"
    p.rect(11, 2, 21, 26, shade(b, -0.1)); p.rect(14, 2, 18, 26, "white"); p.ell(6, 23, 26, 29, b)

@motif("heart")
def _(p, c=None):
    b = c or "red"
    p.circ(11, 12, 6, b); p.circ(21, 12, 6, b); p.poly([(5.2, 13.5), (26.8, 13.5), (16, 27)], b)
    p.circ(10, 10.5, 2, shade(b, 0.55))

@motif("cross")
def _(p, c=None):
    b = c or "green"
    p.rect(12, 5, 20, 27, b); p.rect(5, 12, 27, 20, b)
    p.rect(13.5, 6.5, 15.5, 13, shade(b, 0.5))

@motif("holy_cross")
def _(p, c=None):
    b = c or "gold"
    p.rect(14, 4, 18, 28, b); p.rect(8, 10, 24, 14, b)

@motif("halo")
def _(p, c=None):
    p.ring(5, 9, 27, 19, c or "holy", 2.4)

@motif("wing")
def _(p, c=None):
    b = c or "white"
    p.poly([(6, 24), (7, 12), (14, 6), (27, 5), (24, 10), (27, 11), (23, 15), (25, 17), (19, 20), (15, 24)], b)
    p.line([(10, 18), (22, 9)], shade(b, -0.25), 0.8); p.line([(12, 21), (21, 15)], shade(b, -0.25), 0.8)

@motif("eye")
def _(p, c=None):
    b = c or "white"
    p.poly([(3, 16), (9, 9), (16, 7), (23, 9), (29, 16), (23, 23), (16, 25), (9, 23)], b)
    p.circ(16, 16, 6, c and shade(c, -0.4) or "green"); p.circ(16, 16, 3, "black"); p.circ(14, 14, 1.3, "white")

@motif("skull")
def _(p, c=None):
    b = c or "bone"
    p.circ(16, 13, 9, b); p.rect(11, 19, 21, 26, b)
    p.circ(12.5, 13.5, 2.6, "black"); p.circ(19.5, 13.5, 2.6, "black"); p.poly([(16, 17), (14.5, 20), (17.5, 20)], "black")
    for x in (13, 16, 19): p.line([(x, 23), (x, 26)], "black", 0.8)

@motif("ghost")
def _(p, c=None):
    b = c or "shadow"
    p.circ(16, 13, 8.5, b); p.poly([(7.5, 13), (24.5, 13), (25, 27), (21, 24), (18, 28), (14, 24), (10, 28), (7, 25)], b)
    p.circ(13, 13, 1.8, "ruby"); p.circ(19, 13, 1.8, "ruby")

@motif("chain")
def _(p, c=None):
    b = c or "iron"
    for i in range(4):
        x, y = 6 + i * 5.5, 23 - i * 5.5
        p.ring(x - 3.4, y - 3.4, x + 3.4, y + 3.4, b, 1.6)

@motif("rune")
def _(p, c=None):
    b = c or "amethyst"
    p.ring(4, 4, 28, 28, b, 1.4); p.ring(8, 8, 24, 24, b, 1.0)
    pts = [(16 + 8 * math.cos(math.radians(-90 + i * 144)), 16 + 8 * math.sin(math.radians(-90 + i * 144))) for i in range(6)]
    p.line(pts, b, 1.0)

@motif("hourglass")
def _(p, c=None):
    p.rect(8, 4, 24, 7, "wood"); p.rect(8, 25, 24, 28, "wood")
    p.poly([(10, 7), (22, 7), (17, 16), (22, 25), (10, 25), (15, 16)], "ice")
    p.poly([(12, 25), (20, 25), (16, 20)], c or "wheat"); p.poly([(12, 9), (20, 9), (16, 14)], c or "wheat")

@motif("bell")
def _(p, c=None):
    b = c or "gold"
    p.poly([(9, 22), (11, 11), (16, 6), (21, 11), (23, 22)], b); p.rect(7, 22, 25, 25, shade(b, -0.2)); p.circ(16, 26.5, 2, shade(b, -0.3))
    p.line([(13, 11), (12, 20)], shade(b, 0.5), 1)

@motif("horn")
def _(p, c=None):
    b = c or "bone"
    p.poly([(5, 10), (9, 6), (20, 14), (26, 20), (27, 26), (21, 25), (15, 19)], b)
    p.line([(10, 9), (7, 12)], shade(b, -0.3), 1); p.line([(15, 13), (12, 16)], shade(b, -0.3), 1)

@motif("paw")
def _(p, c=None):
    b = c or "brown"
    p.ell(9, 14, 23, 26, b)
    for x, y in ((8, 11), (13, 7), (19, 7), (24, 11)): p.circ(x, y, 2.8, b)

@motif("wolf")
def _(p, c=None):
    b = c or "gray"
    p.poly([(6, 4), (12, 10), (20, 10), (26, 4), (26, 16), (21, 23), (16, 28), (11, 23), (6, 16)], b)
    p.poly([(12, 20), (20, 20), (16, 28)], shade(b, 0.45)); p.circ(16, 26, 1.6, "black")
    p.poly([(10, 14), (14, 15), (12, 16)], "fire_y"); p.poly([(22, 14), (18, 15), (20, 16)], "fire_y")

@motif("cat")
def _(p, c=None):
    b = c or "wind"
    p.poly([(6, 6), (12, 11), (20, 11), (26, 6), (26, 20), (16, 26), (6, 20)], b)
    p.circ(12, 16, 1.6, "black"); p.circ(20, 16, 1.6, "black"); p.poly([(15, 19), (17, 19), (16, 20.5)], "pink")

@motif("fox")
def _(p, c=None):
    b = c or "white"
    p.poly([(5, 4), (11, 11), (21, 11), (27, 4), (26, 15), (16, 27), (6, 15)], b)
    p.circ(11.5, 15, 1.6, "black"); p.circ(20.5, 15, 1.6, "black"); p.circ(16, 25, 1.4, "black")
    p.poly([(7, 6), (10, 10), (8, 11)], "pink"); p.poly([(25, 6), (22, 10), (24, 11)], "pink")

@motif("lion")
def _(p, c=None):
    b = c or "gold"
    for a in range(0, 360, 30):
        r = math.radians(a); p.circ(16 + 9 * math.cos(r), 16 + 9 * math.sin(r), 4, shade(b, -0.25))
    p.circ(16, 16, 8, b); p.circ(13, 14, 1.5, "black"); p.circ(19, 14, 1.5, "black"); p.poly([(14.5, 18), (17.5, 18), (16, 20)], "brown")

@motif("dragon")
def _(p, c=None):
    b = c or "dark"
    p.poly([(4, 18), (10, 9), (18, 7), (26, 11), (28, 17), (20, 17), (22, 22), (14, 21), (9, 25)], b)
    p.poly([(14, 8), (12, 2), (18, 7)], shade(b, 0.3)); p.poly([(20, 8), (21, 2), (24, 9)], shade(b, 0.3))
    p.circ(21, 12, 1.6, "ruby"); p.line([(25, 16), (28, 16)], "white", 0.8)

@motif("bird")
def _(p, c=None):
    b = c or "brown"
    p.poly([(3, 12), (12, 14), (16, 18), (20, 14), (29, 12), (22, 19), (18, 21), (16, 27), (14, 21), (10, 19)], b)
    p.poly([(15, 14), (17, 14), (16, 11)], "fire_y")

@motif("bee")
def _(p, c=None):
    p.ell(7, 11, 25, 23, "yellow")
    for x in (12, 17): p.rect(x, 11, x + 2.5, 23, "black")
    p.ell(11, 4, 18, 12, "ice"); p.ell(16, 4, 23, 12, "ice"); p.poly([(24, 16), (28, 17), (24, 18)], "black")

@motif("golem")
def _(p, c=None):
    b = c or "iron"
    p.rect(8, 5, 24, 27, b); p.rect(10, 11, 14, 14, "fire"); p.rect(18, 11, 22, 14, "fire"); p.rect(14, 15, 18, 21, shade(b, -0.25))
    p.line([(9, 24), (15, 22)], "leaf", 1.2)

@motif("lantern")
def _(p, c=None):
    p.rect(14, 3, 18, 6, "stone_d"); p.rect(9, 6, 23, 9, "stone_d"); p.rect(10, 9, 22, 24, c or "fire_y"); p.rect(9, 24, 23, 27, "stone_d")
    p.line([(13, 9), (13, 24)], "stone_d", 0.8); p.line([(19, 9), (19, 24)], "stone_d", 0.8); p.circ(16, 16, 2.6, "white")

@motif("cake")
def _(p, c=None):
    p.rect(6, 15, 26, 26, "wheat"); p.rect(6, 12, 26, 16, c or "white")
    for x in (9, 16, 23): p.circ(x, 11, 1.8, "red")
    p.line([(16, 4), (16, 9)], "white", 1.2); p.circ(16, 3.5, 1.2, "fire")

@motif("pot")
def _(p, c=None):
    p.ell(5, 9, 27, 14, "stone_d"); p.rect(6, 11, 26, 24, c or "iron"); p.ell(6, 21, 26, 27, c or "iron")
    p.rect(2, 12, 6, 14, "stone_d"); p.rect(26, 12, 30, 14, "stone_d")
    for x in (11, 16, 21): p.arc(x - 2, 1, x + 2, 9, 120, 300, "white", 0.9)

@motif("bread")
def _(p, c=None):
    p.ell(4, 10, 28, 24, c or "wheat"); p.ell(5, 9, 27, 18, shade(c or "wheat", 0.2))
    for x in (11, 16, 21): p.line([(x - 1.5, 11), (x + 1.5, 15)], "brown", 1)

@motif("egg")
def _(p, c=None):
    p.ell(9, 6, 23, 27, c or "bone"); p.circ(13, 12, 1.8, "white")

@motif("bucket")
def _(p, c=None):
    p.poly([(7, 9), (25, 9), (22, 26), (10, 26)], "iron"); p.ell(7, 6, 25, 12, c or "white"); p.arc(6, 2, 26, 16, 180, 360, "stone_d", 1)

@motif("wool")
def _(p, c=None):
    b = c or "wool"
    for x, y in ((10, 12), (16, 9), (22, 12), (8, 18), (14, 16), (20, 17), (24, 20), (12, 22), (18, 23)): p.circ(x, y, 5, b)

@motif("cow")
def _(p, c=None):
    p.rect(7, 7, 25, 25, "brown"); p.rect(9, 18, 23, 25, "pink"); p.circ(12, 21.5, 1.3, "black"); p.circ(20, 21.5, 1.3, "black")
    p.rect(10, 11, 13, 14, "black"); p.rect(19, 11, 22, 14, "black"); p.rect(3, 7, 7, 10, "bone"); p.rect(25, 7, 29, 10, "bone")
    p.rect(14, 7, 21, 11, "white")

@motif("bomb")
def _(p, c=None):
    p.circ(15, 18, 9, c or "black"); p.circ(12, 15, 2.4, "gray"); p.line([(20, 10), (24, 6)], "wood", 1.4)
    p.poly([(25, 2), (27, 5), (30, 5), (27, 7), (28, 10), (25, 8), (22, 9), (24, 6)], "fire_y")

@motif("smoke")
def _(p, c=None):
    b = c or "gray"
    p.circ(11, 19, 6, b); p.circ(19, 17, 7, shade(b, 0.2)); p.circ(15, 11, 5.5, shade(b, 0.35)); p.circ(23, 23, 4.5, shade(b, -0.1))

@motif("blood")
def _(p, c=None):
    for x, y, s in ((12, 10, 1.0), (21, 16, 0.75), (13, 21, 0.6)):
        p.poly([(x, y - 6 * s), (x + 4 * s, y + 1 * s), (x - 4 * s, y + 1 * s)], c or "blood"); p.circ(x, y + 2 * s, 4.4 * s, c or "blood")

@motif("target")
def _(p, c=None):
    b = c or "red"
    p.ring(4, 4, 28, 28, b, 1.8); p.ring(10, 10, 22, 22, b, 1.8); p.circ(16, 16, 2.6, b)
    p.line([(16, 1), (16, 8)], b, 1.2); p.line([(16, 24), (16, 31)], b, 1.2); p.line([(1, 16), (8, 16)], b, 1.2); p.line([(24, 16), (31, 16)], b, 1.2)

@motif("ore")
def _(p, c=None):
    p.rect(5, 5, 27, 27, "stone")
    for x, y in ((9, 9), (19, 8), (14, 15), (22, 18), (9, 21)): p.rect(x, y, x + 4, y + 4, c or "gem")

@motif("spyglass")
def _(p, c=None):
    p.poly([(5, 24), (17, 12), (21, 16), (9, 28)], c or "gold"); p.poly([(17, 12), (22, 7), (26, 11), (21, 16)], "brown")
    p.circ(24, 9, 2.6, "ice")

@motif("swirl_up")
def _(p, c=None):
    b = c or "white"
    p.poly([(16, 4), (24, 13), (19, 13), (19, 26), (13, 26), (13, 13), (8, 13)], b)

@motif("burst")
def _(p, c=None):
    b = c or "fire_y"
    pts = []
    for i in range(16):
        r = 13 if i % 2 == 0 else 6
        a = math.radians(i * 22.5)
        pts.append((16 + r * math.cos(a), 16 + r * math.sin(a)))
    p.poly(pts, b); p.circ(16, 16, 4, "white")

@motif("explosion")
def _(p, c=None):
    M["burst"](p, c or "fire"); p.circ(16, 16, 6, "fire_y"); p.circ(16, 16, 3, "white")

@motif("blackhole")
def _(p, c=None):
    for i, r in enumerate((13, 10, 7)):
        p.ring(16 - r, 16 - r * 0.6, 16 + r, 16 + r * 0.6, [c or "purple", "amethyst", "pink"][i], 1.2)
    p.circ(16, 16, 5, "black")

@motif("barrier")
def _(p, c=None):
    b = c or "cyan"
    p.ring(3, 3, 29, 29, b, 2.0)
    p.arc(7, 7, 25, 25, 200, 250, "white", 1.4)

@motif("slash")
def _(p, c=None):
    p.arc(-6, 2, 30, 38, 270, 340, c or "white", 2.6)
    p.arc(-8, 4, 28, 40, 275, 330, shade(c or "white", -0.2), 1.2)

@motif("spin")
def _(p, c=None):
    b = c or "white"
    p.arc(3, 3, 29, 29, 20, 160, b, 2.0); p.arc(3, 3, 29, 29, 200, 340, b, 2.0)
    p.poly([(26, 21), (30, 16), (23, 15)], b); p.poly([(6, 11), (2, 16), (9, 17)], b)

@motif("rain")
def _(p, c=None):
    for x, y in ((7, 4), (14, 9), (21, 3), (26, 12), (9, 17), (18, 19), (24, 24), (12, 25)):
        p.line([(x, y), (x - 2, y + 5)], c or "water_l", 1.2)

@motif("lines")
def _(p, c=None):
    for y, x0 in ((10, 4), (16, 2), (22, 6)): p.line([(x0, y), (x0 + 9, y)], c or "white", 1.2)

@motif("up")
def _(p, c=None):
    p.poly([(16, 4), (26, 15), (20, 15), (20, 27), (12, 27), (12, 15), (6, 15)], c or "green")

@motif("plus")
def _(p, c=None):
    p.rect(13, 5, 19, 27, c or "green"); p.rect(5, 13, 27, 19, c or "green")

@motif("x2")
def _(p, c=None):
    b = c or "white"
    p.line([(6, 10), (14, 22)], b, 2.2); p.line([(14, 10), (6, 22)], b, 2.2)
    p.line([(17, 12), (19, 10), (24, 10), (26, 12), (26, 14), (17, 22), (27, 22)], b, 2.2)

@motif("clover")
def _(p, c=None):
    b = c or "green"
    for x, y in ((11, 11), (21, 11), (11, 20), (21, 20)): p.circ(x, y, 5.6, b)
    p.line([(16, 16), (22, 28)], "leaf_d", 1.6)

@motif("speed")
def _(p, c=None):
    M["boot"](p, c); M["lines"](p, "white")

@motif("footprint")
def _(p, c=None):
    for x, y in ((10, 20), (21, 11)):
        p.ell(x - 3.4, y - 5, x + 3.4, y + 4, c or "white")
        for dx in (-2.5, 0, 2.5): p.circ(x + dx, y - 7, 1.2, c or "white")

@motif("moonblade")
def _(p, c=None):
    p.circ(16, 16, 12, c or "cyan"); p.circ(21, 13, 11, (0, 0, 0, 0))

@motif("grimoire_orb")
def _(p, c=None):
    p.circ(16, 16, 9, c or "purple"); p.circ(13, 13, 3, "white")

@motif("shovel_fork")
def _(p, c=None):
    p.line([(8, 26), (20, 12)], "wood", 2.0)
    for dx in (-3, 0, 3): p.line([(20 + dx * 0.7, 12 - dx * 0.7), (25 + dx * 0.7, 6 - dx * 0.7)], c or "steel", 1.2)

@motif("lava")
def _(p, c=None):
    p.rect(4, 14, 28, 28, "lava"); p.poly([(4, 14), (9, 11), (15, 15), (21, 10), (28, 14)], "fire_y")

@motif("totem")
def _(p, c=None):
    b = c or "gold"
    p.rect(11, 5, 21, 16, b); p.rect(9, 16, 23, 26, shade(b, -0.15)); p.rect(5, 16, 9, 21, "emerald"); p.rect(23, 16, 27, 21, "emerald")
    p.rect(13, 9, 15, 11, "emerald"); p.rect(17, 9, 19, 11, "emerald")

@motif("mask")
def _(p, c=None):
    M["ghost"](p, c)

@motif("kunai_fan")
def _(p, c=None):
    for a in (-50, -25, 0, 25, 50):
        r = math.radians(a - 90)
        x1, y1 = 16 + 13 * math.cos(r), 26 + 13 * math.sin(r)
        p.line([(16, 26), (x1, y1)], c or "steel", 1.6)
        p.circ(x1, y1, 1.3, c or "steel")

@motif("mace")
def _(p, c=None):
    p.line([(7, 26), (17, 15)], "wood", 2.2); p.circ(20, 12, 6, c or "iron")
    for a in range(0, 360, 60):
        r = math.radians(a); p.poly([(20 + 5 * math.cos(r - 0.35), 12 + 5 * math.sin(r - 0.35)), (20 + 9 * math.cos(r), 12 + 9 * math.sin(r)), (20 + 5 * math.cos(r + 0.35), 12 + 5 * math.sin(r + 0.35))], c or "iron")

@motif("crack")
def _(p, c=None):
    p.rect(3, 18, 29, 29, "earth")
    p.line([(16, 18), (14, 22), (17, 25), (15, 29)], "black", 1.4); p.line([(14, 22), (9, 24)], "black", 1); p.line([(17, 25), (22, 26)], "black", 1)
    for x, y in ((10, 12), (19, 9), (24, 14)): p.rect(x, y, x + 3, y + 3, c or "stone")

@motif("spike")
def _(p, c=None):
    b = c or "stone"
    p.poly([(4, 28), (9, 10), (13, 28)], b); p.poly([(11, 28), (17, 4), (22, 28)], shade(b, 0.2)); p.poly([(20, 28), (25, 13), (29, 28)], b)

@motif("hand")
def _(p, c=None):
    b = c or "skin"
    p.rect(9, 14, 23, 27, b)
    for i, x in enumerate((9, 12.6, 16.2, 19.8)): p.rect(x, 5 + abs(1.5 - i) * 1.5, x + 3.2, 15, b)
    p.poly([(9, 18), (4, 13), (6, 11), (11, 15)], b)

@motif("note")
def _(p, c=None):
    p.circ(11, 23, 4, c or "yellow"); p.line([(14.5, 23), (14.5, 7), (24, 5), (24, 18)], c or "yellow", 1.8); p.circ(21, 19, 3.6, c or "yellow")

@motif("banner")
def _(p, c=None):
    p.line([(8, 3), (8, 29)], "wood", 1.6); p.poly([(9, 4), (26, 4), (26, 19), (17.5, 15), (9, 19)], c or "blue")
    M_fish_small(p)

def M_fish_small(p):
    p.ell(12, 8, 20, 12, "white"); p.poly([(19, 10), (23, 7.5), (23, 12.5)], "white")

@motif("sapling")
def _(p, c=None):
    M["sprout"](p, c)

@motif("tablet")
def _(p, c=None):
    p.rect(6, 12, 26, 26, c or "purple"); p.rect(4, 9, 28, 13, "ruby"); p.circ(16, 19, 3, "amethyst")

@motif("sheaf_arrows")
def _(p, c=None):
    for dx in (-5, 0, 5):
        p.line([(10 + dx, 27), (20 + dx, 6)], "wood", 1.1); p.poly([(20 + dx, 4), (22.5 + dx, 9), (17.5 + dx, 8)], c or "steel")

# ---------------- 組み立て ----------------
JOB_COL = {"green": "#3fae3a", "dark_green": "#2a7a3a", "aqua": "#2ab8c8", "dark_aqua": "#1a7a8a", "red": "#d03a30",
           "dark_red": "#8a1a1a", "gold": "#e0a020", "yellow": "#d8c030", "light_purple": "#c060e0", "dark_purple": "#7a30b0",
           "gray": "#8a8a96", "dark_gray": "#4a4a56", "blue": "#3a5ad8", "white": "#d8d8d8"}

def render_layers(layers):
    """[(motif, opts)] → 32×32 の絵（ふちどり付き）"""
    final = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    for name, o in layers:
        p = Pen()
        M[name](p, o.get("c"))
        img = p.img
        s, r, dx, dy = o.get("s", 1.0), o.get("r", 0), o.get("x", 0), o.get("y", 0)
        if r: img = img.rotate(r, resample=Image.BICUBIC, center=(N * K / 2, N * K / 2))
        if o.get("flip"): img = img.transpose(Image.FLIP_LEFT_RIGHT)
        if s != 1.0:
            w = int(N * K * s)
            sm = img.resize((w, w), Image.LANCZOS)
            img = Image.new("RGBA", (N * K, N * K), (0, 0, 0, 0))
            img.alpha_composite(sm, (int((N * K - w) / 2), int((N * K - w) / 2)))
        small = img.resize((N, N), Image.LANCZOS)
        px = small.load()
        for yy in range(N):
            for xx in range(N):
                rr, gg, bb, aa = px[xx, yy]
                if aa >= 110: px[xx, yy] = (min(255, rr * 255 // aa), min(255, gg * 255 // aa), min(255, bb * 255 // aa), 255)
                else: px[xx, yy] = (0, 0, 0, 0)
        # ふちどり（黒っぽい）
        out = Image.new("RGBA", (N, N), (0, 0, 0, 0))
        mask = small.split()[3].filter(ImageFilter.MaxFilter(3))
        edge = Image.new("RGBA", (N, N), (22, 16, 30, 255))
        out.paste(edge, (0, 0), mask)
        out.alpha_composite(small)
        tmp = Image.new("RGBA", (N, N), (0, 0, 0, 0))
        tmp.alpha_composite(out, (int(dx), int(dy)) if (dx or dy) else (0, 0))
        final.alpha_composite(tmp)
    return final

def frame(kind, jc, ult):
    """kind: active=丸 / passive=四角"""
    img = Image.new("RGBA", (N * K, N * K), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    ring = col("gold") if ult else col(jc)
    bg_out, bg_in = shade(jc, -0.62), shade(jc, -0.4)
    if kind == "active":
        d.ellipse([0, 0, N * K - 1, N * K - 1], fill=(18, 14, 24, 255))
        d.ellipse([K, K, N * K - K - 1, N * K - K - 1], fill=ring)
        d.ellipse([3 * K, 3 * K, N * K - 3 * K - 1, N * K - 3 * K - 1], fill=bg_out)
        d.ellipse([7 * K, 5 * K, N * K - 7 * K - 1, N * K - 9 * K - 1], fill=bg_in)
    else:
        d.rounded_rectangle([0, 0, N * K - 1, N * K - 1], radius=5 * K, fill=(18, 14, 24, 255))
        d.rounded_rectangle([K, K, N * K - K - 1, N * K - K - 1], radius=4 * K, fill=ring)
        d.rounded_rectangle([3 * K, 3 * K, N * K - 3 * K - 1, N * K - 3 * K - 1], radius=2 * K, fill=bg_out)
        d.rounded_rectangle([6 * K, 5 * K, N * K - 6 * K - 1, N * K - 10 * K - 1], radius=2 * K, fill=bg_in)
    sm = img.resize((N, N), Image.LANCZOS)
    px = sm.load()
    for yy in range(N):
        for xx in range(N):
            r_, g_, b_, a_ = px[xx, yy]
            px[xx, yy] = (r_, g_, b_, 255 if a_ >= 120 else 0)
    if ult:   # 奥義：金のきらめき
        for (x, y) in ((4, 4), (27, 27)):
            for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)): px[x + dx, y + dy] = (255, 250, 210, 255)
    return sm

def parse(spec):
    """'sword r=-10 s=.8 + slash c=cyan' → [(name, opts)]"""
    layers = []
    for part in spec.split("+"):
        toks = part.split()
        name, o = toks[0], {}
        for t in toks[1:]:
            k, v = t.split("=")
            o[k] = v if k == "c" else (v == "1" if k == "flip" else float(v))
        layers.append((name, o))
    return layers

def make_icon(spec, kind, jc, ult):
    base = frame(kind, jc, ult)
    art = render_layers(parse(spec))
    # 枠の内側（2〜29）に入るよう少し縮める
    art = art.resize((26, 26), Image.NEAREST) if False else art
    inner = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    a2 = art.resize((24, 24), Image.LANCZOS)
    px = a2.load()
    for yy in range(24):
        for xx in range(24):
            r_, g_, b_, a_ = px[xx, yy]
            px[xx, yy] = (r_, g_, b_, 255 if a_ >= 120 else 0)
    inner.alpha_composite(a2, (4, 4))
    base.alpha_composite(inner)
    return base

def make_stat(spec):
    art = render_layers(parse(spec))
    return art

from skill_icon_specs import SPECS, STATS   # noqa: E402

def main():
    sheet_out = sys.argv[sys.argv.index("--sheet") + 1] if "--sheet" in sys.argv else None
    jobs = {}
    for f in glob.glob(os.path.join(RES, "data/jsn_craft/jsn_jobs/*.json")):
        d = json.load(open(f, encoding="utf-8"))
        jobs[os.path.basename(f)[:-5]] = JOB_COL.get(d["name"].get("color", "white"), "#888888")
    # スキルのフォルダ → 職業（剣士のスキルは sword/ にあるので、職業の json から調べる）
    owner = {}
    for f in glob.glob(os.path.join(RES, "data/jsn_craft/jsn_jobs/*.json")):
        d = json.load(open(f, encoding="utf-8"))
        for u in d.get("skills", []):
            owner.setdefault(u["skill"].split(":")[-1].split("/")[0], os.path.basename(f)[:-5])
    made, missing = [], []
    for f in sorted(glob.glob(os.path.join(RES, "data/jsn_craft/jsn_skills/*/*.json"))):
        job, sid = f.split(os.sep)[-2], os.path.basename(f)[:-5]
        key = f"{job}/{sid}"
        d = json.load(open(f, encoding="utf-8"))
        if key not in SPECS:
            missing.append(key); continue
        name = d["name"]["text"]
        ult = name.startswith("★") or name.startswith("奥義")
        ic = make_icon(SPECS[key], "active" if d["type"] == "active" else "passive", jobs.get(owner.get(job, job), "#888888"), ult)
        os.makedirs(os.path.join(OUT, job), exist_ok=True)
        ic.save(os.path.join(OUT, job, sid + ".png"))
        new_icon = f"jsn_craft:skill_icon/{job}/{sid}"
        if d.get("icon") != new_icon:
            if "item_icon" not in d: d["item_icon"] = d.get("icon")
            d["icon"] = new_icon
            with open(f, "w", encoding="utf-8") as fp:
                json.dump(d, fp, ensure_ascii=False, indent=2)
        made.append((key, name, ic))
    os.makedirs(os.path.join(OUT, "stat"), exist_ok=True)
    stats = []
    for attr, spec in STATS.items():
        ic = make_stat(spec)
        ic.save(os.path.join(OUT, "stat", attr.split(":")[-1].replace(".", "_") + ".png"))
        stats.append((attr, ic))
    if missing: print("絵の決まっていないスキル:", missing)
    print(len(made), "skills,", len(stats), "stats")
    if sheet_out:
        cols = 16
        allic = [ic for _, _, ic in made] + [ic for _, ic in stats]
        rows = (len(allic) + cols - 1) // cols
        S = 64
        sh = Image.new("RGBA", (cols * (S + 6) + 6, rows * (S + 6) + 6), (48, 44, 58, 255))
        for i, ic in enumerate(allic):
            sh.alpha_composite(ic.resize((S, S), Image.NEAREST), (6 + (i % cols) * (S + 6), 6 + (i // cols) * (S + 6)))
        sh.save(sheet_out)

if __name__ == "__main__":
    sys.path.insert(0, HERE)
    main()
