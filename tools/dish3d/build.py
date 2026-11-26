"""
立体料理のモデルを作る道具。
    python3 tools/dish3d/build.py [--preview 出力フォルダ]
  ・素材テクスチャ → assets/jsn_craft/textures/block/dish3d/*.png（32×32）
  ・料理のモデル   → assets/jsn_craft/models/block/dish3d/<料理>.json
  ・アイテムのモデル → assets/jsn_craft/models/item/dish/<料理>.json（立体のモデルを使う）
"""
import json, math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import materials
from render import Renderer

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
TEX = os.path.join(ROOT, "src/main/resources/assets/jsn_craft/textures/block/dish3d")
MODEL = os.path.join(ROOT, "src/main/resources/assets/jsn_craft/models/block/dish3d")
ITEM = os.path.join(ROOT, "src/main/resources/assets/jsn_craft/models/item/dish")

DIRS = ("up", "down", "north", "south", "east", "west")

class Dish:
    def __init__(self): self.els = []

    def box(self, f, t, mat=None, top=None, side=None, bottom=None, fit=(), faces=None):
        """f, t: [x,y,z]。top/side/bottom で面ごとの素材。fit に入れた面は素材をぴったり1枚貼る"""
        f = [round(v, 3) for v in f]; t = [round(v, 3) for v in t]
        if any(t[i] - f[i] <= 0 for i in range(3)): return
        fs = {}
        for d in DIRS:
            m = (faces or {}).get(d) or (top if d == "up" else bottom if d == "down" else side) or mat
            if m is None: continue
            face = {"texture": "#" + m}
            if d in fit: face["uv"] = [0, 0, 16, 16]
            fs[d] = face
        self.els.append({"from": f, "to": t, "faces": fs})

    def mask_boxes(self, cells, step, y1, y2, **kw):
        """マス目（集合）を長方形にまとめて箱にする"""
        cells = set(cells); done = set()
        for (i, j) in sorted(cells, key=lambda c: (c[1], c[0])):
            if (i, j) in done: continue
            w = 1
            while (i + w, j) in cells and (i + w, j) not in done: w += 1
            h = 1
            while all((i + k, j + h) in cells and (i + k, j + h) not in done for k in range(w)): h += 1
            for k in range(w):
                for l in range(h): done.add((i + k, j + l))
            self.box([i * step, y1, j * step], [(i + w) * step, y2, (j + h) * step], **kw)

    @staticmethod
    def ellipse_cells(cx, cz, rx, rz, step=0.5):
        n = int(16 / step)
        return {(i, j) for i in range(n) for j in range(n)
                if ((i + 0.5) * step - cx) ** 2 / rx ** 2 + ((j + 0.5) * step - cz) ** 2 / rz ** 2 <= 1}

    def disc(self, cx, cz, r, y1, y2, rz=None, step=0.5, **kw):
        self.mask_boxes(self.ellipse_cells(cx, cz, r, rz or r, step), step, y1, y2, **kw)

    def ring(self, cx, cz, r_out, r_in, y1, y2, outer, inner, top, step=0.5):
        """お椀の壁：外側の面は outer、内側の面は inner（横に並んだマスはまとめて箱を減らす）"""
        out = self.ellipse_cells(cx, cz, r_out, r_out, step); inn = self.ellipse_cells(cx, cz, r_in, r_in, step)
        cells = out - inn
        def kind(c):  # 0: 壁の続き（面なし） 1: 内側 2: 外側
            return 0 if c in cells else 1 if c in inn else 2
        mat = {1: inner, 2: outer}
        rows = {}
        for (i, j) in cells: rows.setdefault(j, []).append(i)
        for j, xs in rows.items():
            xs.sort(); k = 0
            while k < len(xs):
                i0 = xs[k]; key = (kind((i0, j - 1)), kind((i0, j + 1)))
                e = k
                while e + 1 < len(xs) and xs[e + 1] == xs[e] + 1 and (kind((xs[e + 1], j - 1)), kind((xs[e + 1], j + 1))) == key: e += 1
                i1 = xs[e]
                faces = {"up": top, "down": outer}
                if key[0]: faces["north"] = mat[key[0]]
                if key[1]: faces["south"] = mat[key[1]]
                w = kind((i0 - 1, j)); ea = kind((i1 + 1, j))
                if w: faces["west"] = mat[w]
                if ea: faces["east"] = mat[ea]
                self.box([i0 * step, y1, j * step], [(i1 + 1) * step, y2, (j + 1) * step], faces=faces)
                k = e + 1

    # ---- よく使う器 ----
    def plate(self, r=7.0, h=1.0, top="porcelain_top", side="porcelain_side", cx=8, cz=8, rz=None):
        self.disc(cx, cz, r, 0, h, rz=rz, top=top, side=side, bottom=side)

    def bowl(self, r=7.3, soup=None, soup_y=3.9, layers=((3.6, 0.0, 0.6), (5.0, 0.6, 1.5), (6.0, 1.5, 2.5), (6.8, 2.5, 3.5), (7.3, 3.5, 4.4)),
             outer="bowl_outer", inner="bowl_inner", rim="bowl_rim", cx=8, cz=8, wall=0.9):
        """どんぶり：下がすぼまって、上に向かって広がる形。layers は (半径, 下, 上)"""
        st = 1.0 if layers[-1][0] >= 5.5 else 0.5   # 大きい器は1マス単位（箱の数を減らす）
        if st == 1.0: wall = max(wall, 1.0)
        for i, (rr, y1, y2) in enumerate(layers):
            if i == 0:
                self.disc(cx, cz, rr, y1, y2, mat=outer, step=st)
                continue
            self.ring(cx, cz, rr, rr - wall, y1, y2, outer, inner, rim, step=st)
            if y1 < soup_y - 0.3:
                self.disc(cx, cz, rr - wall, y1, min(y2, soup_y - 0.3), mat=inner, step=st)
        if soup: self.disc(cx, cz, layers[-1][0] - wall, soup_y - 0.3, soup_y, top=soup, side=soup, bottom=soup, step=st)

    def small_bowl(self, cx, cz, r, h, soup, outer, inner=None, rim=None):
        """小さいお椀（r=半径、h=高さ）"""
        inner = inner or outer; rim = rim or outer
        L = ((r * 0.55, 0, h * 0.15), (r * 0.8, h * 0.15, h * 0.5), (r, h * 0.5, h))
        self.bowl(soup=soup, soup_y=h * 0.85, layers=L, outer=outer, inner=inner, rim=rim, cx=cx, cz=cz, wall=0.6)

    def pot(self, cx, cz, r, h, soup, outer, rim, soup_y=None, handles=True):
        """鍋（まっすぐの壁＋取っ手）"""
        st = 1.0 if r >= 5.5 else 0.5
        wall = 1.0 if st == 1.0 else 0.8
        self.disc(cx, cz, r, 0, 0.6, mat=outer, step=st)
        self.ring(cx, cz, r, r - wall, 0.6, h, outer, outer, rim, step=st)
        sy = soup_y or h - 0.6
        self.disc(cx, cz, r - wall, 0.6, sy, top=soup, side=outer, bottom=outer, step=st)
        if handles:
            self.box([cx - r - 1.2, h - 1.4, cz - 1], [cx - r + 0.2, h - 0.6, cz + 1], mat=rim)
            self.box([cx + r - 0.2, h - 1.4, cz - 1], [cx + r + 1.2, h - 0.6, cz + 1], mat=rim)

    def tray(self, x1, z1, x2, z2, h, mat, top=None):
        self.box([x1, 0, z1], [x2, h, z2], top=top or mat, side=mat, bottom=mat)

    def triangle(self, cx, cz, w, h, d, y0, mat, steps=5):
        """おにぎりの三角"""
        for k in range(steps):
            ww = w * (1 - k / steps * 0.85)
            self.box([cx - ww / 2, y0 + h * k / steps, cz - d / 2], [cx + ww / 2, y0 + h * (k + 1) / steps, cz + d / 2], mat=mat)

    def fish(self, x1, x2, cz, y0, h, w, body, tail=None, head=None):
        """焼き魚など丸ごとの魚（x 方向に長い）"""
        L = x2 - x1
        for k, (f, t) in enumerate(((0.0, 1.0), (0.1, 0.9), (0.2, 0.8))):
            ww = w * (1 - k * 0.25)
            self.box([x1 + L * f * 0.3, y0 + h * k / 3, cz - ww / 2], [x2 - L * (1 - t) * 0.3, y0 + h * (k + 1) / 3, cz + ww / 2], mat=body)
        self.box([x2, y0 + h * 0.1, cz - w * 0.55], [x2 + 1.6, y0 + h * 0.6, cz + w * 0.55], mat=tail or body)
        if head: self.box([x1 - 0.01, y0 + h * 0.4, cz - w * 0.3], [x1 + 0.4, y0 + h * 0.7, cz - w * 0.05], mat=head)

    def glass(self, cx, cz, r, h, liquid, liquid_y, mat="glass", y0=0.0):
        """グラス。側面は中の液体が透けて見える"""
        side = mat
        if mat == "glass" and liquid in LIQ:
            side = materials.tinted(liquid, LIQ[liquid], y0 + liquid_y)
        self.disc(cx, cz, r, y0, y0 + 0.5, mat=mat)
        self.ring(cx, cz, r, r - 0.5, y0 + 0.5, y0 + h, side, mat, "glass")
        self.disc(cx, cz, r - 0.5, y0 + 0.5, y0 + liquid_y, top=liquid, side=liquid, bottom=liquid)

    def bottle(self, cx, cz, r, h, mat, cap="cork", lbl="label"):
        """瓶：胴・肩・首・栓（ラベルつき）"""
        self.disc(cx, cz, r, 0, h * 0.6, mat=mat)
        if lbl: self.disc(cx, cz, r + 0.05, h * 0.18, h * 0.45, top=mat, side=lbl, bottom=mat)
        self.disc(cx, cz, r * 0.7, h * 0.6, h * 0.7, mat=mat)
        self.disc(cx, cz, r * 0.35 + 0.2, h * 0.7, h * 0.92, mat=mat)
        self.disc(cx, cz, r * 0.35 + 0.25, h * 0.92, h, mat=cap)

    def slices(self, pts, w, d, h, y, mat, side=None):
        for (x, z) in pts:
            self.box([x, y, z], [x + w, y + h, z + d], top=mat, side=side or mat, bottom=side or mat)

    def json(self, textures):
        used = sorted({f["texture"][1:] for e in self.els for f in e["faces"].values()})
        tex = {u: f"jsn_craft:block/dish3d/{u}" for u in used}
        tex["particle"] = tex[used[0]]
        return {"parent": "minecraft:block/block", "ambientocclusion": False, "textures": tex, "elements": self.els,
                "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, 2.5, 0], "scale": [0.8, 0.8, 0.8]},
                            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
                            "fixed": {"rotation": [-90, 0, 0], "translation": [0, 0, -6], "scale": [0.8, 0.8, 0.8]},
                            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.4, 0.4, 0.4]},
                            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 3, 0], "scale": [0.45, 0.45, 0.45]},
                            "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 3, 0], "scale": [0.45, 0.45, 0.45]}}}

# お酒の液体の色（グラスの側面に透けさせる）
LIQ = {"beer_liquid": "#e8a020", "mead_liquid": "#d88a18", "wine_liquid": "#7a0e24", "sake_liquid": "#f4f0dc",
       "shochu_liquid": "#eef4f4", "rum_liquid": "#8a4210", "umeshu_liquid": "#e0a830", "whisky_liquid": "#c06a14",
       "soda_liquid": "#cfeaff", "highball_liquid": "#f0c060", "sour_liquid": "#fff4b0", "sangria_liquid": "#a01830",
       "mojito_liquid": "#c8f0c0", "nether_liquid": "#e83a0c", "ender_liquid": "#6a1aa8", "lemonade_liquid": "#fff080",
       "grape_jelly_m": "#7a2a9a", "void_jelly_m": "#3a1a6a",
       "milk_liquid": "#f8f6f0", "shake_liquid": "#fff0c8", "cocoa_liquid": "#6a3a1e", "melon_juice": "#ff5a6a",
       "smoothie_liquid": "#c84a8a", "amazake_liquid": "#f4f0e4", "greentea_liquid": "#a8c060", "coffee_liquid": "#3a2010",
       "matcha_liquid": "#7ab040", "hotwine_liquid": "#8a1a24", "cider_liquid": "#f0d070", "honeylemon_liquid": "#f8d878",
       "berryorange_liquid": "#f07a3a", "blue_liquid": "#3aa8f0", "martini_liquid": "#e8f0e0"}

DISHES = {}
def dish(f): DISHES[f.__name__] = f; return f

DRINKS = {}
def drink(f): DRINKS[f.__name__] = f; return f

# ======================== 料理 ========================

@dish
def sushi(d):
    d.box([1, 1, 3.5], [15, 2.4, 12.5], top="board_top", side="board_side", bottom="board_side")
    d.box([2, 0, 4], [3.5, 1, 12], mat="board_side"); d.box([12.5, 0, 4], [14, 1, 12], mat="board_side")
    for i, (x, fish) in enumerate(((2.3, "tuna"), (6.5, "salmon"), (10.7, "tamago"))):
        d.box([x + 0.3, 2.4, 5.2], [x + 2.7, 4.2, 10.8], mat="rice")
        d.box([x + 0.5, 4.2, 5.4], [x + 2.5, 4.5, 10.6], mat="rice")
        d.box([x, 4.3, 4.6], [x + 3, 5.2, 11.4], top=fish, side=fish, bottom=fish)
        if fish == "tamago": d.box([x - 0.15, 2.3, 7.4], [x + 3.15, 5.35, 8.6], mat="nori")
    d.box([1.6, 2.4, 10.8], [3.4, 3.0, 12.2], mat="gari"); d.box([12.8, 2.4, 11.0], [13.8, 3.2, 12.0], mat="wasabi")

@dish
def curry_rice(d):
    d.plate()
    for (r, y1, y2) in ((3.6, 1, 2.4), (2.8, 2.4, 3.4), (1.8, 3.4, 4.2)):
        d.disc(5.2, 8, r, y1, y2, mat="rice")
    d.disc(10.2, 8.4, 4.4, 1, 1.7, rz=5.0, mat="curry")
    d.box([8.6, 1.6, 6.4], [9.9, 2.8, 7.7], mat="potato"); d.box([11.2, 1.6, 7.4], [12.4, 2.6, 8.6], mat="potato")
    d.box([10.4, 1.6, 9.8], [11.4, 2.5, 10.8], mat="carrot"); d.box([9.0, 1.6, 10.6], [9.9, 2.4, 11.6], mat="carrot")
    d.box([10.0, 1.6, 5.0], [11.4, 2.6, 6.2], mat="beef"); d.box([12.0, 1.6, 9.6], [13.2, 2.5, 10.8], mat="beef")
    d.box([3.2, 1, 11.2], [5.0, 1.6, 12.4], mat="pickles")
    d.box([4.9, 4.2, 7.7], [5.6, 4.6, 8.4], mat="parsley")

@dish
def ramen(d):
    y = 3.9  # スープの高さ
    d.bowl(soup="broth", soup_y=y)
    d.disc(8, 8.6, 3.8, y, y + 0.3, rz=3.2, mat="noodles")
    d.box([2.4, y, 5.0], [6.0, y + 0.7, 8.6], top="chashu", side="chashu_side", bottom="chashu_side", fit=("up",))
    d.box([4.2, y + 0.1, 8.0], [7.6, y + 0.9, 11.4], top="chashu", side="chashu_side", bottom="chashu_side", fit=("up",))
    d.box([9.4, y, 9.4], [11.8, y + 1.1, 11.8], top="ajitama", side="ajitama_side", bottom="ajitama_side", fit=("up",))
    d.box([9.6, y, 5.2], [11.8, y + 0.5, 7.4], top="naruto", side="bowl_rim", bottom="bowl_rim", fit=("up",))
    d.box([8.4, y - 0.4, 2.0], [12.4, y + 3.6, 2.5], mat="nori")
    for (x, z) in ((7.4, 11.6), (6.2, 12.4), (12.4, 8.4), (8.4, 6.4), (5.2, 9.6), (11.0, 12.2)):
        d.box([x, y + 0.25, z], [x + 0.8, y + 0.55, z + 0.8], top="negi", side="parsley", bottom="parsley", fit=("up",))

@dish
def omurice(d):
    d.plate()
    for (rx, rz, y1, y2) in ((5.6, 3.3, 1, 2.6), (5.0, 2.8, 2.6, 3.6), (4.0, 2.0, 3.6, 4.3)):
        d.disc(7.6, 8, rx, y1, y2, rz=rz, mat="omelette")
    for (x1, z1, x2, z2) in ((4.4, 7.4, 6.2, 8.2), (6.0, 7.8, 7.8, 8.6), (7.6, 7.4, 9.4, 8.2), (9.2, 7.8, 10.8, 8.6)):
        d.box([x1, 4.3, z1], [x2, 4.6, z2], mat="ketchup")
    d.box([1.8, 1, 10.0], [5.0, 1.4, 12.6], mat="lettuce")
    d.box([2.6, 1.4, 10.4], [4.2, 2.5, 11.8], mat="tomato")
    d.box([11.6, 1, 10.6], [12.6, 1.8, 11.6], mat="parsley")

@dish
def pancake(d):
    d.plate()
    for k, (y1, y2) in enumerate(((1, 2.4), (2.4, 3.8), (3.8, 5.2))):
        d.disc(8 + (0.2 if k == 1 else 0), 8 - (0.2 if k == 2 else 0), 5.0, y1, y2, top="pancake_top", side="pancake_side", bottom="pancake_side")
    d.disc(8, 8, 3.6, 5.2, 5.45, mat="syrup")
    d.box([12.7, 3.0, 7.4], [13.2, 5.3, 8.4], mat="syrup"); d.box([7.0, 2.2, 12.8], [8.0, 5.3, 13.25], mat="syrup")
    d.box([6.9, 5.4, 6.9], [9.3, 6.8, 9.3], mat="butter")
    d.box([3.4, 5.2, 4.6], [4.4, 6.0, 5.6], mat="blueberry"); d.box([10.6, 1, 11.4], [11.8, 2.2, 12.6], mat="strawberry")
    d.box([12.0, 1, 10.0], [12.9, 1.8, 10.9], mat="blueberry")


# ======================== ふつう ========================

@dish
def onigiri(d):
    d.plate(r=6.5, top="bamboo_leaf", side="bamboo_leaf", h=0.5)
    for cx in (5.4, 10.6):
        d.triangle(cx, 8, 4.6, 5.0, 3.0, 0.5, "rice")
        d.box([cx - 2.4, 0.4, 6.4], [cx + 2.4, 1.9, 9.6], mat="nori")
    d.box([7.0, 0.5, 11.4], [9.6, 1.1, 12.6], mat="takuan")

@dish
def edamame(d):
    d.small_bowl(8, 8, 6.0, 3.6, None, "clay_side", "clay_side", "clay_side")
    for i, (x, z, rot) in enumerate(((4.2, 6.0, 0), (7.0, 5.2, 1), (9.6, 6.4, 0), (5.4, 8.6, 1), (8.4, 8.8, 0), (10.6, 9.6, 1), (6.6, 10.6, 0))):
        if rot: d.box([x, 2.6 + (i % 2) * 0.4, z], [x + 1.2, 3.6 + (i % 2) * 0.4, z + 3.0], mat="edamame_pod")
        else:   d.box([x, 2.6 + (i % 2) * 0.4, z], [x + 3.0, 3.6 + (i % 2) * 0.4, z + 1.2], mat="edamame_pod")

@dish
def vegetable_soup(d):
    d.small_bowl(8, 8, 6.8, 3.6, "veg_soup", "porcelain_side", "porcelain_side", "porcelain_side")
    for (x, z, m) in ((5.0, 6.2, "carrot"), (9.2, 5.6, "potato"), (10.6, 9.0, "carrot"), (6.4, 9.6, "potato"), (8.0, 7.6, "parsley")):
        d.box([x, 2.9, z], [x + 1.2, 3.5, z + 1.2], mat=m)

@dish
def grilled_fish_set(d):
    d.tray(1, 1.5, 15, 14.5, 0.8, "tray_wood")
    d.box([1.8, 0.8, 2.2], [11.2, 1.4, 7.4], top="porcelain_side", side="porcelain_side")
    d.fish(2.6, 9.0, 4.8, 1.4, 1.6, 2.8, "grilled_fish", tail="steak_side", head="coal_bit")
    d.box([8.6, 1.4, 6.0], [10.4, 1.8, 7.0], mat="daikon")
    d.small_bowl(5.0, 10.8, 2.9, 3.0, "rice", "lacquer_black", "lacquer_black", "lacquer_red")
    d.small_bowl(11.6, 10.8, 2.6, 2.8, "miso_broth", "lacquer_red", "lacquer_black", "lacquer_black")
    d.box([12.0, 0.8, 2.6], [14.4, 1.6, 4.4], mat="takuan")

@dish
def miso_soup(d):
    d.small_bowl(8, 8, 6.4, 4.4, "miso_broth", "lacquer_red", "lacquer_black", "lacquer_black")
    for (x, z) in ((5.4, 6.2), (8.8, 8.8), (6.4, 9.6)):
        d.box([x, 3.4, z], [x + 1.2, 3.9, z + 1.2], mat="tofu")
    d.box([9.0, 3.6, 5.0], [11.0, 3.8, 6.8], mat="wakame"); d.box([4.4, 3.6, 8.0], [5.6, 3.8, 9.0], mat="negi")

@dish
def salad(d):
    d.small_bowl(8, 8, 6.8, 3.4, "lettuce", "glass", "glass", "glass")
    for (x, z, h) in ((4.0, 5.0, 1.4), (7.4, 4.2, 1.6), (10.0, 6.0, 1.2), (5.0, 9.0, 1.6), (9.6, 9.6, 1.4)):
        d.box([x, 2.7, z], [x + 2.6, 2.7 + h, z + 2.2], mat="lettuce")
    for (x, z) in ((6.6, 7.0), (10.6, 8.2), (4.4, 7.4)):
        d.box([x, 3.6, z], [x + 1.4, 4.8, z + 1.4], mat="tomato")
    d.box([7.2, 4.0, 9.2], [9.4, 4.4, 11.0], mat="onion_ring")

@dish
def honey_toast(d):
    d.plate()
    d.box([3.6, 1, 3.6], [12.4, 5.6, 12.4], top="toast_top", side="toast_side", bottom="toast_side", fit=("up",))
    d.box([6.6, 5.6, 6.6], [9.4, 6.6, 9.4], mat="butter")
    d.box([4.4, 5.6, 4.6], [11.4, 5.75, 11.6], mat="honey")
    d.box([12.4, 2.6, 7.0], [12.7, 5.7, 8.4], mat="honey"); d.box([5.4, 3.2, 12.4], [6.6, 5.7, 12.7], mat="honey")

@dish
def meat_stew(d):
    d.small_bowl(8, 8, 6.8, 3.8, "stew", "clay_side", "clay_side", "clay_side")
    for (x, z, m) in ((5.0, 6.0, "beef"), (9.0, 5.4, "beef"), (10.4, 9.0, "carrot"), (6.2, 9.4, "potato"), (8.2, 7.6, "beef"), (7.6, 10.6, "mushroom")):
        d.box([x, 3.1, z], [x + 1.6, 4.0, z + 1.6], mat=m)
    d.box([9.8, 3.9, 7.2], [10.4, 4.1, 7.8], mat="parsley")

@dish
def lemonade(d):
    d.glass(8, 8, 3.4, 9.0, "lemonade_liquid", 8.2)
    d.box([9.8, 6.5, 4.4], [11.2, 10.0, 7.2], top="lemon_slice", side="lemon_m", fit=("south", "north"))
    d.box([6.6, 8.0, 7.4], [7.4, 13.0, 8.2], mat="straw_red")
    d.box([7.0, 8.2, 9.0], [8.4, 9.6, 10.4], mat="ice")

@dish
def mushroom_omelette(d):
    d.plate()
    for (rx, rz, y1, y2) in ((5.2, 3.2, 1, 2.4), (4.4, 2.6, 2.4, 3.2)):
        d.disc(8, 8, rx, y1, y2, rz=rz, mat="omelette")
    for (x, z) in ((5.4, 7.0), (8.0, 7.6), (10.2, 6.8)):
        d.box([x + 0.3, 3.2, z + 0.3], [x + 0.9, 3.8, z + 0.9], mat="mushroom_stem")
        d.box([x, 3.8, z], [x + 1.2, 4.3, z + 1.2], mat="mushroom")
    d.box([11.0, 1, 10.6], [12.0, 1.6, 11.6], mat="parsley")

@dish
def yaki_onigiri(d):
    d.plate(r=6.5)
    for cx in (5.4, 10.6):
        d.triangle(cx, 8, 4.6, 5.0, 3.0, 1.0, "yaki_rice")
    d.box([7.0, 1.0, 11.4], [9.6, 1.6, 12.6], mat="takuan")

@dish
def fish_and_chips(d):
    d.box([2.0, 0, 3.0], [14.0, 3.0, 13.0], top="newspaper", side="newspaper", bottom="newspaper")
    for (x, z) in ((2.6, 4.0), (6.0, 4.4)):
        d.box([x, 3.0, z], [x + 3.0, 4.6, z + 6.0], mat="batter")
    for i in range(7):
        x = 9.4 + (i % 3) * 1.4; z = 4.0 + (i // 3) * 2.6
        d.box([x, 3.0 + (i % 2) * 0.6, z], [x + 0.9, 3.9 + (i % 2) * 0.6, z + 2.4], mat="fries")
    d.box([3.6, 3.0, 10.6], [5.6, 4.6, 12.2], top="lemon_slice", side="lemon_m", fit=("up",))

@dish
def berry_tart(d):
    d.plate()
    d.disc(8, 8, 5.4, 1, 2.8, top="tart_top", side="tart_side", bottom="tart_side")
    for (x, z, m) in ((5.2, 6.0, "strawberry"), (8.0, 5.0, "blueberry"), (10.4, 6.6, "strawberry"), (6.4, 9.4, "blueberry"),
                      (9.4, 9.6, "strawberry"), (7.8, 7.6, "strawberry"), (4.6, 8.0, "blueberry"), (11.0, 8.8, "blueberry")):
        d.box([x, 2.8, z], [x + 1.2, 3.8, z + 1.2], mat=m)
    d.box([7.6, 3.8, 6.6], [8.6, 4.2, 7.4], mat="parsley")

@dish
def cabbage_roll(d):
    d.small_bowl(8, 8, 6.8, 3.2, "tomato_sauce_m", "porcelain_side")
    for (x, z) in ((3.8, 5.6), (8.4, 7.0)):
        d.box([x, 2.4, z], [x + 3.8, 4.8, z + 3.4], mat="lettuce")
        d.box([x + 0.2, 4.8, z + 0.2], [x + 3.6, 5.0, z + 3.2], mat="tomato_sauce_m")
    d.box([7.6, 3.0, 11.0], [8.4, 3.4, 11.8], mat="parsley")

# ======================== ごちそう ========================

@dish
def sashimi_platter(d):
    d.plate(r=7.2, top="black_plate_top", side="black_side")
    d.box([2.6, 1, 9.6], [6.6, 1.8, 13.0], mat="daikon")
    d.box([2.8, 1.8, 4.0], [6.0, 2.0, 8.6], mat="shiso")
    for i, (x, m) in enumerate(((3.0, "tuna"), (4.4, "tuna"), (5.8, "tuna"), (7.6, "salmon"), (9.0, "salmon"), (10.4, "salmon"), (11.8, "white_fish"))):
        d.box([x, 1.8, 4.2 + (i % 2) * 0.4], [x + 1.6, 2.9, 10.2 + (i % 2) * 0.4], mat=m)
    d.box([12.0, 1, 11.4], [13.2, 2.0, 12.6], mat="wasabi")

@dish
def miner_lunch(d):
    d.box([1.5, 0, 2.5], [14.5, 3.6, 13.5], top="lacquer_red", side="lacquer_black", bottom="lacquer_black")
    d.box([2.2, 3.0, 3.2], [7.6, 3.7, 12.8], mat="rice")
    d.box([4.4, 3.7, 7.4], [5.4, 4.2, 8.4], mat="ume_m")
    d.box([8.2, 3.0, 3.2], [13.8, 3.6, 7.6], mat="bento_meat")
    for (x, z) in ((8.6, 3.6), (10.6, 4.4), (12.2, 3.8)):
        d.box([x, 3.6, z], [x + 1.6, 4.4, z + 1.6], mat="bento_meat")
    d.box([8.2, 3.0, 8.2], [13.8, 3.6, 12.8], mat="lettuce")
    d.box([8.8, 3.6, 8.8], [10.8, 4.6, 10.6], mat="carrot"); d.box([11.2, 3.6, 10.6], [13.0, 4.6, 12.2], mat="tamago")
    d.box([7.6, 3.0, 2.5], [8.2, 3.8, 13.5], mat="lacquer_black")

@dish
def warrior_steak(d):
    d.box([1.5, 0, 3.0], [14.5, 1.2, 13.0], top="tray_wood", side="board_side", bottom="board_side")
    d.box([2.4, 1.2, 4.0], [10.4, 3.4, 11.4], top="steak_top", side="steak_side", bottom="steak_side", fit=("up",))
    d.box([11.0, 1.2, 4.4], [13.6, 2.8, 7.0], mat="potato"); d.box([11.4, 2.8, 4.8], [13.2, 3.1, 6.6], mat="butter")
    d.box([11.0, 1.2, 8.0], [12.4, 2.4, 9.4], mat="mushroom"); d.box([12.4, 1.2, 9.8], [13.6, 2.0, 11.0], mat="carrot")
    d.box([5.6, 3.4, 6.8], [7.0, 3.7, 8.2], mat="parsley")

@dish
def pizza(d):
    d.box([0.8, 0, 0.8], [15.2, 0.8, 15.2], top="tray_wood", side="board_side", bottom="board_side")
    d.disc(8, 8, 7.0, 0.8, 1.8, top="pizza_top", side="pizza_crust", bottom="pizza_crust")

@dish
def tempura(d):
    d.plate(r=7.0, top="black_plate_top", side="black_side")
    d.box([2.4, 1, 3.0], [11.0, 1.1, 12.6], mat="paper")
    for (z, rot) in ((4.0, 0), (6.8, 0), (9.6, 0)):
        d.box([3.0, 1.1, z], [9.4, 2.9, z + 2.0], mat="batter")
        d.box([9.4, 1.6, z + 0.3], [10.6, 2.4, z + 1.7], mat="shrimp")
    d.small_bowl(12.4, 11.2, 2.2, 2.2, "dipping_sauce", "porcelain_side")
    d.box([11.4, 1, 3.6], [13.6, 1.8, 5.8], mat="daikon")

@dish
def grape_jelly(d):
    d.glass(8, 8, 4.4, 6.0, "grape_jelly_m", 5.6)
    for (x, z) in ((6.4, 6.6), (8.4, 7.4), (7.2, 9.0), (9.6, 9.2)):
        d.box([x, 5.6, z], [x + 1.4, 7.0, z + 1.4], mat="grape_m")
    d.box([8.2, 7.0, 8.2], [8.6, 7.8, 8.6], mat="stick")

@dish
def gyudon(d):
    d.bowl(soup="rice", soup_y=3.9, outer="lacquer_black", inner="lacquer_red", rim="lacquer_red")
    d.disc(8, 8, 5.0, 3.9, 4.6, rz=4.6, mat="teriyaki")
    for (x, z) in ((5.4, 6.4), (9.2, 9.6), (7.6, 5.4)):
        d.box([x, 4.6, z], [x + 1.8, 4.9, z + 0.8], mat="onion_ring")
    d.box([10.2, 4.6, 5.6], [11.6, 5.2, 7.0], mat="red_ginger")

@dish
def hamburger(d):
    d.plate()
    layers = (("bread_bun", 1.0, 2.2, 3.6), ("lettuce", 2.2, 2.6, 4.2), ("patty", 2.6, 3.8, 3.8), ("cheese_m", 3.8, 4.1, 4.0),
              ("tomato", 4.1, 4.6, 3.5), ("lettuce", 4.6, 4.9, 3.9), ("bread_bun", 4.9, 6.2, 3.6), ("sesame_bun", 6.2, 7.0, 2.8))
    for (m, y1, y2, r) in layers:
        d.disc(6.6, 7.4, r, y1, y2, mat=m)
    for i in range(5):
        x = 10.2 + (i % 2) * 1.2; z = 8.6 + (i // 2) * 1.4
        d.box([x, 1, z], [x + 0.8, 1.8, z + 2.6], mat="fries")

@dish
def ume_ochazuke(d):
    d.small_bowl(8, 8, 6.4, 4.4, "chawan_tea", "lacquer_black", "porcelain_side", "porcelain_side")
    d.disc(8, 8, 3.4, 3.4, 3.9, mat="rice")
    d.box([7.2, 3.9, 7.2], [8.8, 5.0, 8.8], mat="ume_m")
    for (x, z) in ((5.0, 9.6), (9.8, 5.6), (10.4, 9.2)):
        d.box([x, 3.8, z], [x + 1.4, 4.0, z + 0.5], mat="nori")

@dish
def gratin(d):
    d.disc(8, 8, 6.6, 0, 3.6, rz=5.0, top="clay_side", side="clay_side", bottom="clay_side")
    d.disc(8, 8, 5.8, 3.0, 3.8, rz=4.2, mat="gratin_top")
    d.box([7.4, 3.8, 7.4], [8.2, 4.1, 8.2], mat="parsley")

@dish
def mapo_tofu(d):
    d.small_bowl(8, 8, 6.8, 3.0, "mapo", "black_side")
    for (x, z) in ((4.6, 5.4), (7.4, 4.8), (10.0, 6.2), (5.6, 8.6), (8.6, 8.4), (10.6, 9.6), (7.2, 11.0)):
        d.box([x, 2.4, z], [x + 1.6, 3.4, z + 1.6], mat="tofu")
    for (x, z) in ((6.6, 7.0), (9.4, 10.6), (4.6, 10.2)):
        d.box([x, 2.6, z], [x + 0.8, 2.9, z + 0.8], mat="negi")

@dish
def lemon_chicken(d):
    d.plate()
    d.box([2.0, 1, 7.6], [6.0, 1.6, 12.4], mat="lettuce")
    for (x, z) in ((4.0, 3.6), (7.4, 4.4), (6.2, 7.6), (9.6, 7.0)):
        d.box([x, 1, z], [x + 2.8, 2.8, z + 2.6], mat="chicken")
    for (x, z) in ((10.2, 10.0), (7.6, 10.8)):
        d.box([x, 1.0, z], [x + 2.2, 1.6, z + 2.2], top="lemon_slice", side="lemon_m", fit=("up",))

@dish
def paella(d):
    d.pot(8, 8, 6.6, 1.8, "yellow_rice", "iron", "iron_handle", soup_y=1.4)
    for (x, z, m, h) in ((4.0, 5.0, "shrimp", 0.9), (9.0, 4.2, "shrimp", 0.9), (10.4, 9.4, "shrimp", 0.9), (5.4, 9.6, "mussel", 0.8),
                         (7.6, 7.0, "mussel", 0.8), (11.0, 6.6, "mussel", 0.8), (6.8, 11.2, "parsley", 0.3)):
        d.box([x, 1.4, z], [x + 1.8, 1.4 + h, z + 1.2], mat=m)
    d.box([3.4, 1.4, 6.6], [5.0, 2.6, 8.4], top="lemon_slice", side="lemon_m", fit=("up",))

@dish
def cheese_fondue(d):
    d.box([0.8, 0, 0.8], [15.2, 0.8, 15.2], top="tray_wood", side="board_side", bottom="board_side")
    pot = Dish(); pot.pot(7.0, 7.0, 4.4, 5.0, "cheese_melt", "iron", "iron_handle", soup_y=4.4)
    for e in pot.els:
        e["from"][1] = round(e["from"][1] + 0.8, 3); e["to"][1] = round(e["to"][1] + 0.8, 3); d.els.append(e)
    for (x, z) in ((12.2, 2.0), (12.6, 4.6), (2.0, 12.4), (4.6, 12.8), (12.4, 12.4), (10.2, 13.0)):
        d.box([x, 0.8, z], [x + 1.8, 2.6, z + 1.8], mat="bread_cube")
    d.box([8.0, 5.2, 7.0], [8.4, 10.4, 7.4], mat="fork"); d.box([7.4, 4.8, 6.4], [9.0, 6.0, 8.0], mat="bread_cube")

# ======================== 逸品 ========================

@dish
def golden_feast(d):
    d.plate(top="gold_plate_top", side="gold_side")
    d.box([3.0, 1, 4.0], [9.0, 4.2, 9.6], mat="steak_side")
    d.box([3.2, 4.2, 4.2], [8.8, 4.4, 9.4], mat="honey")
    for (x, z) in ((10.0, 3.8), (11.4, 5.2)):
        d.box([x, 1, z], [x + 1.2, 2.2, z + 3.6], mat="golden_carrot_m")
    d.box([9.8, 1, 9.6], [12.2, 2.0, 12.0], mat="melon_m")
    d.box([4.6, 1, 10.2], [7.2, 3.6, 12.8], mat="golden_apple_m"); d.box([5.8, 3.6, 11.4], [6.0, 4.4, 11.6], mat="stick")

@dish
def toro_don(d):
    d.bowl(soup="rice", soup_y=3.9, outer="lacquer_black", inner="lacquer_red", rim="lacquer_red")
    for i, (x, z) in enumerate(((3.6, 5.0), (5.4, 4.4), (7.2, 4.2), (9.0, 4.4), (10.8, 5.0))):
        d.box([x, 3.9, z], [x + 1.8, 4.7, z + 6.0], mat="toro")
    d.box([7.2, 3.9, 11.2], [8.6, 4.6, 12.4], mat="wasabi"); d.box([4.0, 3.9, 11.0], [6.0, 4.3, 12.6], mat="shiso")

@dish
def swordfish_steak(d):
    d.plate()
    d.box([2.6, 1, 4.0], [10.6, 3.0, 11.0], top="white_fish", side="fish_skin_silver", bottom="fish_skin_silver")
    d.box([2.6, 3.0, 4.0], [10.6, 3.1, 11.0], mat="steak_top")
    d.box([11.0, 1, 5.0], [13.2, 2.8, 7.2], top="lemon_slice", side="lemon_m", fit=("up",))
    d.box([5.2, 3.1, 6.6], [6.4, 3.4, 7.8], mat="parsley"); d.box([11.2, 1, 9.0], [13.0, 1.6, 11.0], mat="lettuce")

@dish
def anglerfish_hotpot(d):
    d.pot(8, 8, 6.4, 4.4, "hotpot_broth", "donabe", "donabe_glaze", soup_y=3.8)
    for (x, z, m) in ((4.0, 5.0, "white_fish"), (8.6, 4.4, "white_fish"), (9.8, 9.0, "lettuce"), (4.8, 9.4, "lettuce"),
                      (7.2, 7.4, "mushroom"), (10.6, 6.0, "tofu"), (6.6, 10.6, "carrot")):
        d.box([x, 3.8, z], [x + 2.0, 4.8, z + 1.8], mat=m)

@dish
def eel_kabayaki(d):
    d.box([1.5, 0, 3.0], [14.5, 4.0, 13.0], top="lacquer_red", side="lacquer_black", bottom="lacquer_black")
    d.box([2.2, 3.2, 3.7], [13.8, 3.9, 12.3], mat="rice")
    for z in (4.2, 8.2):
        d.box([2.6, 3.9, z], [13.4, 4.9, z + 3.6], mat="eel_glaze")
    for (x, z) in ((4.0, 7.8), (9.0, 7.8)):
        d.box([x, 4.9, z], [x + 1.2, 5.1, z + 0.4], mat="lightning")
    d.box([11.4, 4.9, 4.6], [12.6, 5.3, 5.8], mat="parsley")

@dish
def golden_carp_nitsuke(d):
    d.plate(r=7.2, top="navy_plate_top", side="navy_side", rz=5.6)
    d.disc(8, 8, 6.0, 1, 1.2, rz=4.4, mat="teriyaki")
    d.fish(3.0, 11.0, 8, 1.2, 2.4, 3.6, "golden_fish", tail="golden_fish", head="coal_bit")
    d.box([5.4, 3.4, 7.2], [6.6, 3.7, 8.4], mat="negi")

@dish
def glacier_sashimi(d):
    d.plate(top="ice_plate_top", side="ice_side")
    for (x, z) in ((2.4, 3.0), (11.6, 10.4), (3.0, 11.0)):
        d.box([x, 1, z], [x + 2.0, 3.0, z + 2.0], mat="ice")
    for i, x in enumerate((4.6, 6.2, 7.8, 9.4, 11.0)):
        d.box([x, 1, 4.4 + (i % 2) * 0.4], [x + 1.7, 2.3, 10.0 + (i % 2) * 0.4], mat="glacier_salmon_m")
    d.box([11.8, 1, 4.6], [13.4, 2.4, 6.2], top="lemon_slice", side="lemon_m", fit=("up",))

@dish
def coelacanth_stew(d):
    d.pot(8, 8, 6.6, 4.8, "stew", "iron", "iron_handle", soup_y=4.2)
    for (x, z, m) in ((4.0, 5.0, "coela_fish"), (8.4, 4.4, "coela_fish"), (6.2, 8.4, "coela_fish"), (10.0, 9.0, "potato"), (4.6, 10.2, "carrot")):
        d.box([x, 4.2, z], [x + 2.4, 5.2, z + 2.0], mat=m)
    d.box([10.2, 4.2, 5.6], [11.2, 4.6, 6.6], mat="parsley")

@dish
def crimson_curry(d):
    d.plate(top="black_plate_top", side="black_side")
    for (r, y1, y2) in ((3.6, 1, 2.4), (2.8, 2.4, 3.4), (1.8, 3.4, 4.2)):
        d.disc(5.2, 8, r, y1, y2, mat="rice")
    d.disc(10.2, 8.4, 4.4, 1, 1.7, rz=5.0, mat="crimson_sauce")
    for (x, z) in ((9.0, 6.4), (11.2, 9.4), (10.0, 11.0)):
        d.box([x, 1.7, z], [x + 1.4, 2.8, z + 1.2], mat="dragon_fish")
    for (x, z) in ((10.6, 6.8), (12.0, 8.0)):
        d.box([x, 1.7, z], [x + 0.6, 3.2, z + 0.6], mat="flame")
    d.box([4.8, 4.2, 7.6], [5.6, 4.6, 8.4], mat="chili_m")

@dish
def soul_eater_skewer(d):
    d.plate(top="black_plate_top", side="black_side")
    for z in (5.0, 9.4):
        d.box([1.6, 1.6, z + 0.6], [14.4, 2.0, z + 1.0], mat="stick")
        for x in (3.6, 6.6, 9.6):
            d.box([x, 1.0, z], [x + 2.4, 2.8, z + 1.6], mat="soul_meat")
            d.box([x + 0.8, 2.8, z + 0.4], [x + 1.4, 3.2, z + 1.0], mat="glow_cyan")
    d.box([12.0, 1, 11.6], [13.2, 1.6, 12.8], mat="chili_m")

@dish
def void_jelly_dessert(d):
    d.glass(8, 8, 4.4, 7.0, "void_jelly_m", 6.4)
    for (x, z) in ((6.6, 6.6), (9.0, 8.6)):
        d.box([x, 6.4, z], [x + 1.2, 7.4, z + 1.2], mat="chorus_m")
    for (x, z, y) in ((5.0, 9.6, 7.6), (10.4, 5.4, 8.0), (8.0, 7.6, 8.4)):
        d.box([x, y, z], [x + 0.5, y + 0.5, z + 0.5], mat="glow_purple")

# ======================== 伝説 ========================

@dish
def moon_lord_sashimi(d):
    d.plate(r=7.6, top="navy_plate_top", side="navy_side")
    d.disc(5.6, 6.0, 2.4, 1, 1.4, mat="moon_glow"); d.disc(6.4, 5.2, 2.0, 1.4, 1.5, mat="navy_side")
    for i, (x, z) in enumerate(((7.6, 4.0), (9.4, 5.0), (10.8, 6.6), (11.4, 8.6), (10.8, 10.6), (9.2, 12.0))):
        d.box([x, 1, z], [x + 1.8, 2.3, z + 1.8], mat="white_fish" if i % 2 else "glacier_salmon_m")
    d.box([3.0, 1, 9.0], [6.6, 1.8, 12.4], mat="daikon"); d.box([4.0, 1.8, 10.0], [5.6, 2.4, 11.6], mat="shiso")
    for (x, z, y) in ((3.0, 3.6, 2.4), (13.0, 3.4, 2.8), (2.4, 7.6, 2.0)):
        d.box([x, y, z], [x + 0.5, y + 0.5, z + 0.5], mat="moon_glow")

@dish
def storm_king_steak(d):
    d.box([1.0, 0, 2.0], [15.0, 1.2, 14.0], top="iron", side="iron_handle", bottom="iron_handle")
    d.box([2.0, 1.2, 3.0], [12.0, 4.4, 12.0], top="steak_top", side="storm_steak", bottom="storm_steak", fit=("up",))
    for (x, y, z) in ((12.6, 4.0, 4.0), (13.2, 5.0, 4.6), (12.6, 6.0, 5.2), (13.2, 7.0, 5.8)):
        d.box([x, y, z], [x + 0.8, y + 1.0, z + 0.8], mat="lightning")
    for (x, z) in ((4.0, 5.0), (8.6, 8.6)):
        d.box([x, 4.4, z], [x + 1.6, 4.6, z + 0.4], mat="lightning")
    d.box([12.6, 1.2, 9.6], [14.4, 2.4, 11.4], mat="butter")

@dish
def ice_queen_carpaccio(d):
    d.plate(r=7.6, top="ice_plate_top", side="ice_side")
    for (x, z) in ((3.4, 5.0), (6.0, 3.6), (9.0, 4.0), (11.0, 6.2), (11.4, 9.2), (9.2, 11.4), (6.0, 11.6), (3.6, 9.4)):
        d.box([x, 1, z], [x + 2.4, 1.4, z + 2.2], mat="glacier_salmon_m")
    d.box([6.0, 1, 6.4], [10.0, 3.0, 9.6], mat="ice")
    for x in (6.2, 7.6, 9.0):
        d.box([x, 3.0, 7.6], [x + 0.8, 4.6, 8.4], mat="crown_gold")
    d.box([6.2, 3.0, 7.4], [9.8, 3.6, 8.6], mat="crown_gold")
    for (x, z) in ((4.6, 7.4), (11.8, 8.0), (7.6, 12.6)):
        d.box([x, 1.4, z], [x + 0.6, 2.4, z + 0.6], mat="ice")

@dish
def inferno_dragon_roast(d):
    d.plate(r=7.6, top="black_plate_top", side="black_side", rz=6.0)
    d.fish(2.6, 11.6, 8, 1, 3.0, 4.0, "dragon_fish", tail="flame", head="coal_bit")
    for (x, z) in ((4.0, 7.6), (6.0, 7.6), (8.0, 7.6), (10.0, 7.6)):
        d.box([x, 4.0, z], [x + 0.8, 5.0, z + 0.8], mat="flame")
    for (x, z, h) in ((2.2, 3.6, 2.0), (12.6, 3.8, 2.6), (3.0, 12.0, 1.8), (12.0, 12.4, 2.2)):
        d.box([x, 1, z], [x + 1.0, 1 + h, z + 1.0], mat="flame")
    d.box([5.2, 4.0, 6.2], [5.8, 4.6, 6.8], mat="chili_m")

@dish
def soulflame_nabe(d):
    d.box([2.6, 0, 2.6], [13.4, 1.0, 13.4], mat="iron_handle")
    for (x, z) in ((4.0, 4.0), (11.0, 4.0), (4.0, 11.0), (11.0, 11.0)):
        d.box([x, 1.0, z], [x + 1.0, 2.2, z + 1.0], mat="soul_flame")
    pot = Dish(); pot.pot(8, 8, 6.4, 4.4, "soul_broth", "black_side", "iron_handle", soup_y=3.8)
    for e in pot.els:
        e["from"][1] = round(e["from"][1] + 1.6, 3); e["to"][1] = round(e["to"][1] + 1.6, 3); d.els.append(e)
    for (x, z, m) in ((4.4, 5.0, "soul_meat"), (8.6, 4.6, "soul_meat"), (9.6, 9.0, "lettuce"), (5.0, 9.4, "tofu"), (7.4, 7.2, "glow_cyan")):
        d.box([x, 5.4, z], [x + 2.0, 6.2, z + 1.8], mat=m)

@dish
def ender_dragon_feast(d):
    d.box([0.4, 0, 0.4], [15.6, 1.0, 15.6], top="lacquer_black", side="lacquer_gold", bottom="lacquer_black")
    d.fish(2.4, 9.6, 5.0, 1.0, 2.6, 3.4, "ender_fish", tail="glow_purple", head="glow_purple")
    d.box([3.0, 3.6, 4.4], [9.0, 3.9, 5.6], mat="crown_gold")
    d.small_bowl(12.4, 4.8, 2.6, 3.0, "rice", "lacquer_gold", "lacquer_black", "lacquer_black")
    d.box([2.4, 1.0, 9.6], [5.4, 3.8, 12.6], mat="golden_apple_m"); d.box([3.8, 3.8, 11.0], [4.0, 4.6, 11.2], mat="stick")
    d.small_bowl(9.0, 11.4, 2.6, 2.6, "soul_broth", "lacquer_red", "lacquer_black", "lacquer_gold")
    d.box([12.4, 1.0, 9.4], [14.6, 1.6, 13.6], mat="toro")
    for (x, z, y) in ((7.0, 13.6, 2.0), (14.0, 8.0, 2.4), (1.6, 7.8, 2.6), (11.0, 2.0, 3.0)):
        d.box([x, y, z], [x + 0.5, y + 0.5, z + 0.5], mat="glow_purple")


# ======================== お酒・飲み物 ========================

@drink
def soda_water(d):
    d.bottle(8, 8, 2.6, 11, "bottle_clear", cap="cap_red", lbl=None)
    for (x, y, z) in ((7.4, 3.0, 10.5), (8.6, 5.0, 10.5), (7.8, 7.2, 10.5)):
        d.box([x, y, z], [x + 0.5, y + 0.5, z + 0.2], mat="foam")

@drink
def beer(d):
    d.glass(7, 8, 3.6, 8.0, "beer_liquid", 7.0)
    d.disc(7, 8, 3.7, 7.0, 8.6, mat="foam")
    d.box([10.4, 6.6, 7.8], [11.4, 8.0, 8.4], mat="foam")
    for (y1, y2) in ((2.0, 2.8), (5.6, 6.4)):
        d.box([10.5, y1, 7.4], [12.6, y2, 8.6], mat="glass")
    d.box([11.8, 2.8, 7.4], [12.6, 5.6, 8.6], mat="glass")

@drink
def mead(d):
    d.disc(7, 8, 3.6, 0, 7.0, top="mead_liquid", side="tankard_band", bottom="tankard_wood")
    d.ring(7, 8, 3.6, 2.9, 6.6, 7.4, "tankard_wood", "tankard_wood", "tankard_wood")
    for (y1, y2) in ((1.4, 2.2), (5.0, 5.8)):
        d.box([10.5, y1, 7.4], [12.6, y2, 8.6], mat="iron")
    d.box([11.8, 2.2, 7.4], [12.6, 5.0, 8.6], mat="iron")

@drink
def wine(d):
    d.disc(6, 9, 2.4, 0, 0.4, mat="glass"); d.box([5.6, 0.4, 8.6], [6.4, 4.0, 9.4], mat="glass")
    d.glass(6, 9, 2.8, 4.6, "wine_liquid", 2.6, y0=4.0)
    d.bottle(11.4, 5.4, 2.0, 12, "bottle_green")

@drink
def sake(d):
    d.box([1.5, 0, 4.0], [14.5, 0.6, 12.0], mat="lacquer_black")
    for (r, y1, y2) in ((2.6, 0.6, 4.6), (2.0, 4.6, 6.0), (1.2, 6.0, 8.2), (1.6, 8.2, 8.8)):
        d.disc(6, 8, r, y1, y2, top="sake_liquid" if y2 == 8.8 else "tokkuri", side="tokkuri", bottom="tokkuri")
    for x in (10.6, 13.0):
        d.glass(x, 8, 1.4, 1.6, "sake_liquid", 1.3, mat="porcelain_side", y0=0.6)

@drink
def shochu(d):
    d.glass(8, 8, 3.4, 5.6, "shochu_liquid", 4.6)
    for (x, z, y) in ((6.6, 6.6, 3.8), (8.6, 7.4, 4.2), (7.2, 8.8, 4.0)):
        d.box([x, y, z], [x + 1.6, y + 1.4, z + 1.6], mat="ice")

@drink
def rum(d):
    d.bottle(6.4, 7, 2.4, 12, "bottle_brown")
    d.glass(11.6, 10, 2.0, 3.6, "rum_liquid", 2.4)

@drink
def umeshu(d):
    d.glass(8, 8, 3.4, 5.6, "umeshu_liquid", 4.6)
    d.box([6.6, 3.6, 6.6], [8.2, 5.0, 8.2], mat="ume_m")
    d.box([8.4, 4.0, 7.8], [10.0, 5.4, 9.4], mat="ice")

@drink
def whisky(d):
    d.box([9.6, 0, 3.0], [13.6, 9.0, 7.0], top="bottle_brown", side="bottle_brown", bottom="bottle_brown")
    d.box([9.5, 2.4, 2.9], [13.7, 6.0, 7.1], top="bottle_brown", side="label", bottom="bottle_brown")
    d.box([10.8, 9.0, 4.2], [12.4, 11.0, 5.8], mat="bottle_brown"); d.box([10.6, 11.0, 4.0], [12.6, 12.0, 6.0], mat="cork")
    d.glass(6, 10, 3.2, 4.4, "whisky_liquid", 2.8)
    d.box([5.0, 2.0, 9.0], [7.4, 4.6, 11.4], mat="ice")

@drink
def highball(d):
    d.glass(8, 8, 2.8, 10.0, "highball_liquid", 9.0)
    for (x, z, y) in ((6.8, 6.8, 7.6), (8.2, 7.6, 8.2)):
        d.box([x, y, z], [x + 1.4, y + 1.2, z + 1.4], mat="ice")
    d.box([9.6, 8.2, 5.2], [11.2, 10.6, 5.6], top="lemon_slice", side="lemon_m", fit=("north", "south"))

@drink
def lemon_sour(d):
    d.glass(7, 8, 3.6, 8.0, "sour_liquid", 7.2)
    for (y1, y2) in ((2.0, 2.8), (5.6, 6.4)):
        d.box([10.5, y1, 7.4], [12.6, y2, 8.6], mat="glass")
    d.box([11.8, 2.8, 7.4], [12.6, 5.6, 8.6], mat="glass")
    d.box([5.4, 6.2, 6.6], [7.6, 7.4, 6.9], top="lemon_slice", side="lemon_slice", fit=("north", "south"))
    d.box([6.8, 6.6, 8.6], [8.4, 7.8, 10.2], mat="ice")

@drink
def sangria(d):
    d.glass(6.6, 7.4, 3.8, 8.6, "sangria_liquid", 7.4)
    d.box([9.8, 7.4, 6.6], [11.4, 8.6, 8.2], mat="glass")
    for (x, z, m) in ((5.0, 6.0, "orange_slice"), (7.4, 8.4, "lemon_m"), (6.0, 8.6, "grape_m")):
        d.box([x, 6.8, z], [x + 1.6, 7.6, z + 1.4], mat=m)
    d.glass(12.4, 11.6, 1.8, 3.6, "sangria_liquid", 2.8)

@drink
def mojito(d):
    d.glass(8, 8, 3.0, 9.0, "mojito_liquid", 8.0)
    for (x, z, y) in ((6.6, 6.8, 7.0), (8.4, 8.6, 7.4), (7.2, 9.0, 6.4)):
        d.box([x, y, z], [x + 1.4, y + 0.4, z + 1.0], mat="mint")
    d.box([7.0, 7.2, 7.2], [8.6, 8.4, 8.8], mat="ice")
    d.box([9.0, 7.6, 7.4], [9.6, 12.0, 8.0], mat="straw_red")

@drink
def nether_fire(d):
    d.glass(8, 8, 3.4, 5.2, "nether_liquid", 4.4)
    for (x, z, h) in ((7.0, 7.0, 2.0), (8.6, 8.2, 2.8), (7.6, 9.0, 1.6)):
        d.box([x, 4.4, z], [x + 0.8, 4.4 + h, z + 0.8], mat="flame")

@drink
def ender_elixir(d):
    d.disc(8, 8, 2.4, 0, 0.4, mat="glass"); d.box([7.6, 0.4, 7.6], [8.4, 4.0, 8.4], mat="glass")
    d.glass(8, 8, 3.0, 5.0, "ender_liquid", 3.4, y0=4.0)
    for (x, z, y) in ((6.0, 7.0, 9.6), (9.6, 6.4, 10.4), (8.4, 10.0, 9.0), (5.4, 10.0, 8.6)):
        d.box([x, y, z], [x + 0.5, y + 0.5, z + 0.5], mat="glow_purple")


@drink
def milk(d):
    d.glass(8, 8, 2.6, 9.0, "milk_liquid", 8.0)
    d.disc(8, 8, 2.2, 9.0, 10.0, mat="glass"); d.disc(8, 8, 2.3, 10.0, 10.6, mat="cap_red")

@drink
def milkshake(d):
    d.glass(8, 8, 2.8, 9.0, "shake_liquid", 8.4)
    d.disc(8, 8, 2.4, 8.4, 9.8, mat="cream"); d.disc(8, 8, 1.4, 9.8, 10.6, mat="cream")
    d.box([7.5, 10.6, 7.5], [8.5, 11.4, 8.5], mat="strawberry")
    d.box([9.2, 8.6, 7.6], [9.8, 13.0, 8.2], mat="straw_red")

@drink
def hot_cocoa(d):
    d.glass(7, 8, 3.4, 6.4, "cocoa_liquid", 5.6, mat="porcelain_side")
    for (x, z) in ((6.0, 7.0), (7.6, 8.4)):
        d.box([x, 5.6, z], [x + 1.2, 6.6, z + 1.2], mat="marshmallow")
    for (y1, y2) in ((1.4, 2.0), (4.4, 5.0)):
        d.box([10.3, y1, 7.5], [12.0, y2, 8.5], mat="porcelain_side")
    d.box([11.4, 2.0, 7.5], [12.0, 4.4, 8.5], mat="porcelain_side")

@drink
def watermelon_juice(d):
    d.glass(8, 8, 3.0, 8.0, "melon_juice", 7.2)
    d.box([9.6, 6.6, 6.6], [12.4, 8.4, 7.4], mat="melon_rind")
    d.box([9.8, 7.0, 6.4], [12.2, 8.6, 6.8], mat="melon_juice")
    d.box([6.6, 7.2, 8.6], [7.2, 11.6, 9.2], mat="straw_red")

@drink
def berry_smoothie(d):
    d.glass(8, 8, 3.0, 8.4, "smoothie_liquid", 7.8)
    for (x, z, m) in ((6.6, 6.8, "blueberry"), (8.0, 7.6, "strawberry"), (7.0, 8.8, "blueberry")):
        d.box([x, 7.8, z], [x + 1.0, 8.8, z + 1.0], mat=m)
    d.box([9.0, 7.8, 8.4], [9.6, 12.4, 9.0], mat="straw_red")

@drink
def amazake(d):
    d.small_bowl(8, 8, 3.6, 4.0, "amazake_liquid", "tray_wood", "tray_wood", "tray_wood")
    d.box([8.6, 3.4, 7.0], [9.4, 3.7, 7.8], mat="potato")

@drink
def green_tea(d):
    d.glass(6.4, 8.4, 2.6, 5.2, "greentea_liquid", 4.4, mat="yunomi")
    for (r, y1, y2) in ((2.8, 0, 3.0), (2.0, 3.0, 4.0), (1.4, 4.0, 4.6)):
        d.disc(11.4, 6.0, r, y1, y2, mat="chawan")
    d.box([13.6, 1.6, 5.6], [15.4, 2.2, 6.4], mat="chawan"); d.box([10.6, 4.6, 5.4], [12.2, 5.2, 6.6], mat="yunomi")

@drink
def coffee(d):
    d.disc(8, 8, 4.6, 0, 0.6, top="porcelain_top", side="porcelain_side")
    d.glass(7.6, 8, 3.0, 4.4, "coffee_liquid", 3.8, mat="porcelain_side", y0=0.6)
    d.box([10.4, 1.6, 7.5], [11.8, 2.2, 8.5], mat="porcelain_side"); d.box([11.2, 2.2, 7.5], [11.8, 3.8, 8.5], mat="porcelain_side")
    d.box([10.4, 3.8, 7.5], [11.8, 4.4, 8.5], mat="porcelain_side")
    d.box([6.8, 4.4, 7.2], [8.2, 4.5, 8.8], mat="cream")

@drink
def matcha(d):
    d.small_bowl(7, 8, 4.4, 4.0, "matcha_liquid", "chawan", "chawan", "chawan")
    d.box([11.4, 0, 9.4], [13.2, 2.4, 11.2], mat="chasen"); d.box([11.8, 2.4, 9.8], [12.8, 3.8, 10.8], mat="bamboo_leaf")

@drink
def hot_wine(d):
    d.glass(7, 8, 3.2, 6.6, "hotwine_liquid", 5.8)
    for (y1, y2) in ((1.4, 2.0), (4.6, 5.2)):
        d.box([10.1, y1, 7.5], [11.8, y2, 8.5], mat="glass")
    d.box([11.2, 2.0, 7.5], [11.8, 4.6, 8.5], mat="glass")
    d.box([5.4, 5.8, 6.6], [7.6, 6.2, 8.6], top="lemon_slice", side="orange_slice", fit=("up",))
    d.box([7.8, 5.4, 8.6], [8.4, 9.0, 9.2], mat="cinnamon")

@drink
def cider(d):
    d.bottle(6.0, 6.4, 2.2, 11, "bottle_green")
    d.glass(11.2, 9.6, 2.2, 5.0, "cider_liquid", 4.2)
    d.box([3.4, 0, 10.6], [6.0, 2.6, 13.2], mat="apple_m"); d.box([4.6, 2.6, 11.8], [4.8, 3.4, 12.0], mat="stick")

@drink
def honey_lemon_sour(d):
    d.glass(7, 8, 3.6, 8.0, "honeylemon_liquid", 7.2)
    for (y1, y2) in ((2.0, 2.8), (5.6, 6.4)):
        d.box([10.5, y1, 7.4], [12.6, y2, 8.6], mat="glass")
    d.box([11.8, 2.8, 7.4], [12.6, 5.6, 8.6], mat="glass")
    d.box([5.2, 6.4, 6.4], [7.4, 7.6, 6.7], top="lemon_slice", side="lemon_slice", fit=("north", "south"))
    d.box([7.0, 6.0, 8.4], [8.2, 7.4, 9.6], mat="honey")

@drink
def berry_orange(d):
    d.glass(8, 8, 3.0, 8.4, "berryorange_liquid", 7.6)
    d.box([9.6, 7.0, 5.6], [11.4, 9.0, 5.9], top="lemon_slice", side="lemon_slice", fit=("north", "south"))
    d.box([7.0, 7.6, 7.4], [8.0, 8.6, 8.4], mat="strawberry")

@drink
def tequila(d):
    d.bottle(5.6, 6.2, 2.2, 11, "bottle_clear")
    for x in (10.0, 12.6):
        d.glass(x, 10.4, 1.2, 2.4, "martini_liquid", 1.8)
        d.ring(x, 10.4, 1.25, 0.8, 2.3, 2.6, "salt_rim", "salt_rim", "salt_rim")
    d.box([10.4, 0, 6.4], [12.4, 1.2, 8.4], top="lemon_slice", side="lemon_m", fit=("up",))

@drink
def blue_lagoon(d):
    d.glass(8, 8, 2.8, 9.4, "blue_liquid", 8.6)
    d.box([9.4, 8.4, 5.6], [11.2, 10.2, 5.9], mat="pineapple")
    d.box([6.4, 9.4, 7.6], [6.8, 12.4, 8.0], mat="stick")
    d.box([5.0, 12.4, 6.4], [8.2, 12.8, 9.2], mat="umbrella"); d.box([5.8, 12.8, 7.2], [7.4, 13.2, 8.4], mat="umbrella")

@drink
def martini(d):
    d.disc(8, 8, 2.2, 0, 0.4, mat="glass"); d.box([7.6, 0.4, 7.6], [8.4, 5.0, 8.4], mat="glass")
    for (r, y1, y2) in ((1.2, 5.0, 5.8), (2.2, 5.8, 6.8), (3.2, 6.8, 7.8), (4.0, 7.8, 8.6)):
        d.ring(8, 8, r, max(0.1, r - 0.5), y1, y2, materials.tinted("martini_liquid", "#e8f0e0", 8.2), "glass", "glass")
        d.disc(8, 8, max(0.1, r - 0.5), y1, min(y2, 8.2), top="martini_liquid", side="martini_liquid", bottom="martini_liquid")
    d.box([7.4, 7.6, 7.4], [8.6, 8.8, 8.6], mat="ume_m"); d.box([7.9, 8.8, 7.9], [8.1, 11.0, 8.1], mat="stick")

# ======================== 牧場の料理 ========================

@dish
def marbled_steak(d):
    d.box([1.5, 0, 3.0], [14.5, 1.2, 13.0], top="tray_wood", side="board_side", bottom="board_side")
    d.box([2.4, 1.2, 4.0], [10.6, 3.6, 11.6], top="marbled_top", side="steak_side", bottom="steak_side", fit=("up",))
    d.box([5.2, 3.6, 6.6], [7.6, 4.2, 9.0], mat="butter")
    d.box([11.0, 1.2, 4.4], [13.6, 2.8, 7.0], mat="potato"); d.box([11.4, 2.8, 4.8], [13.2, 3.1, 6.6], mat="butter")
    d.box([11.2, 1.2, 8.4], [13.4, 2.2, 10.6], mat="carrot"); d.box([8.6, 3.6, 4.6], [9.6, 3.9, 5.6], mat="parsley")

@dish
def herb_roast_lamb(d):
    d.plate()
    d.disc(7.4, 8, 4.6, 1, 4.2, rz=3.4, top="lamb_top", side="lamb_side", bottom="lamb_side", fit=("up",))
    for (x, z) in ((11.6, 5.0), (12.4, 7.4), (11.8, 10.2)):
        d.box([x, 1, z], [x + 1.6, 2.2, z + 1.6], mat="potato")
    d.box([5.0, 4.2, 7.6], [9.6, 4.6, 8.4], mat="parsley")
    d.box([7.0, 4.6, 7.8], [7.4, 6.8, 8.2], mat="stick")

@dish
def jidori_karaage(d):
    d.plate()
    d.box([2.2, 1, 9.6], [6.6, 1.4, 13.4], mat="lettuce")
    for (x, z, h) in ((4.0, 4.0, 2.4), (6.6, 3.4, 2.2), (8.8, 4.8, 2.6), (5.2, 6.6, 2.4), (7.8, 7.2, 2.8), (10.2, 7.4, 2.2), (6.4, 9.6, 2.2), (9.0, 9.8, 2.4)):
        d.box([x, 1, z], [x + 2.4, 1 + h, z + 2.2], mat="karaage")
    d.box([11.4, 1, 10.6], [13.2, 2.2, 12.4], mat="lemon_slice")

@dish
def golden_omelette(d):
    d.plate()
    for (rx, rz, y1, y2) in ((5.6, 3.3, 1, 2.6), (5.0, 2.8, 2.6, 3.6), (4.0, 2.0, 3.6, 4.3)):
        d.disc(7.6, 8, rx, y1, y2, rz=rz, mat="gold_omelette")
    d.box([6.4, 4.3, 7.4], [8.8, 4.6, 8.6], mat="butter")
    d.box([1.8, 1, 10.0], [5.0, 1.4, 12.6], mat="lettuce")
    d.box([11.6, 1, 10.6], [12.6, 1.8, 11.6], mat="parsley")

@dish
def milk_pudding(d):
    d.plate(r=5.6)
    for (r, y1, y2) in ((3.6, 1, 2.4), (3.2, 2.4, 3.8), (2.8, 3.8, 5.0)):
        d.disc(8, 8, r, y1, y2, mat="pudding_side")
    d.disc(8, 8, 2.8, 5.0, 5.6, top="pudding_top", side="pudding_top", bottom="pudding_top")
    d.box([7.4, 1, 4.0], [8.8, 3.0, 4.6], mat="pudding_top")
    d.disc(8, 8, 1.0, 5.6, 6.6, mat="cream"); d.box([7.6, 6.6, 7.6], [8.4, 7.4, 8.4], mat="strawberry")

@dish
def cheesecake(d):
    d.plate()
    d.box([3.0, 1, 4.0], [11.0, 1.8, 12.0], mat="cookie_crust")
    d.box([3.0, 1.8, 4.0], [11.0, 5.4, 12.0], top="cheesecake_top", side="cheesecake_side", bottom="cheesecake_side", fit=("up",))
    d.box([4.4, 5.4, 5.4], [6.4, 5.8, 7.4], mat="berry_sauce"); d.box([8.0, 5.4, 8.6], [9.6, 5.8, 10.2], mat="berry_sauce")
    d.box([11.6, 1, 6.0], [13.0, 2.2, 7.4], mat="strawberry"); d.box([11.8, 1, 9.0], [12.8, 1.8, 10.0], mat="blueberry")

# ---- 農家の黄金の食材・巨大作物の料理 ----
@dish
def golden_onigiri(d):
    d.plate(r=6.5, top="gold_plate_top", side="gold_side", h=0.5)
    for cx in (5.4, 10.6):
        d.triangle(cx, 8, 4.6, 5.0, 3.0, 0.5, "golden_rice_m")
        d.box([cx - 2.4, 0.4, 6.4], [cx + 2.4, 1.9, 9.6], mat="nori")
    d.box([7.0, 0.5, 11.4], [9.6, 1.1, 12.6], mat="takuan")

@dish
def golden_fried_rice(d):
    d.plate()
    for (r, y1, y2) in ((4.8, 1, 2.4), (4.0, 2.4, 3.6), (2.8, 3.6, 4.6), (1.6, 4.6, 5.2)):
        d.disc(8, 8, r, y1, y2, mat="golden_rice_m")
    for (x, z) in ((5.4, 6.4), (9.6, 5.8), (10.4, 9.4), (6.4, 10.0), (8.0, 4.8)):
        d.box([x, 2.6, z], [x + 1.0, 3.2, z + 1.0], mat="tamago")
    for (x, z) in ((7.0, 7.0), (9.0, 8.6), (6.0, 8.4)):
        d.box([x, 4.4, z], [x + 0.6, 4.8, z + 0.6], mat="negi")

@dish
def golden_bread(d):
    d.board() if hasattr(d, "board") else d.tray(2.0, 3.0, 14.0, 13.0, 1.0, "tray_wood")
    d.box([3.4, 1, 5.0], [12.6, 4.4, 11.0], mat="golden_bread")
    d.box([4.0, 4.4, 5.6], [12.0, 5.4, 10.4], mat="golden_bread")
    for x in (5.2, 7.6, 10.0):
        d.box([x, 5.4, 6.4], [x + 1.0, 5.6, 9.6], mat="butter")

@dish
def golden_potato_gratin(d):
    d.disc(8, 8, 6.6, 0, 3.6, rz=5.0, top="clay_side", side="clay_side", bottom="clay_side")
    d.disc(8, 8, 5.8, 3.0, 3.8, rz=4.2, mat="gratin_top")
    for (x, z) in ((5.0, 6.4), (9.4, 6.0), (7.2, 9.0), (10.2, 9.2)):
        d.box([x, 3.6, z], [x + 1.4, 4.4, z + 1.4], mat="golden_potato_m")
    d.box([7.4, 3.8, 7.4], [8.2, 4.1, 8.2], mat="parsley")

@dish
def golden_tomato_soup(d):
    d.small_bowl(8, 8, 6.8, 3.6, "tomato_soup", "porcelain_side", "porcelain_side", "porcelain_side")
    for (x, z) in ((5.4, 6.4), (9.4, 6.8), (7.4, 9.6)):
        d.box([x, 2.9, z], [x + 1.4, 3.7, z + 1.4], mat="golden_tomato_m")
    d.box([8.0, 3.5, 5.6], [8.6, 3.7, 6.2], mat="parsley")

@dish
def harvest_stew(d):
    d.small_bowl(8, 8, 7.2, 4.0, "harvest_stew", "clay_side", "clay_side", "clay_side")
    for (x, z, m) in ((4.6, 6.0, "carrot"), (9.0, 5.0, "potato"), (10.6, 9.0, "carrot"), (6.0, 9.6, "potato"), (8.2, 7.6, "beef"), (7.6, 11.0, "lettuce"), (11.0, 6.6, "golden_potato_m")):
        d.box([x, 3.3, z], [x + 1.8, 4.3, z + 1.8], mat=m)

@dish
def golden_gozen(d):
    d.tray(1.0, 1.0, 15.0, 15.0, 0.8, "lacquer_red", top="lacquer_black")
    d.small_bowl(5.0, 5.4, 3.0, 2.6, "golden_rice_m", "lacquer_gold", "lacquer_gold", "lacquer_gold")
    d.small_bowl(11.0, 5.4, 3.0, 2.4, "tomato_soup", "lacquer_black", "lacquer_black", "lacquer_gold")
    d.box([3.0, 0.8, 9.4], [7.4, 2.2, 13.0], mat="golden_bread")
    d.box([9.0, 0.8, 9.6], [13.0, 1.4, 13.0], mat="porcelain_top")
    for (x, z) in ((9.6, 10.2), (11.4, 11.4)):
        d.box([x, 1.4, z], [x + 1.4, 2.6, z + 1.4], mat="golden_potato_m")
    d.box([11.0, 1.4, 9.8], [12.4, 2.6, 11.2], mat="golden_tomato_m")

# ======================== 出力 ========================

def build(preview_dir=None):
    os.makedirs(MODEL, exist_ok=True); os.makedirs(ITEM, exist_ok=True)
    DRINK_ITEM = os.path.join(os.path.dirname(ITEM), "drink"); os.makedirs(DRINK_ITEM, exist_ok=True)
    built = {}
    for name, f in list(DISHES.items()) + list(DRINKS.items()):
        d = Dish(); f(d); built[name] = d
    tex = materials.build_all(TEX)  # グラスの色つき素材も、モデルを組んだあとでまとめて作る
    out = {}
    for name, d in built.items():
        m = d.json(tex)
        json.dump(m, open(os.path.join(MODEL, name + ".json"), "w"), indent=1)
        folder = DRINK_ITEM if name in DRINKS else ITEM
        json.dump({"parent": f"jsn_craft:block/dish3d/{name}"}, open(os.path.join(folder, name + ".json"), "w"), indent=2)
        out[name] = (m, d.els)
        print(name, len(d.els), "個の箱")
    if preview_dir:
        os.makedirs(preview_dir, exist_ok=True)
        for name, (m, _) in out.items():
            r = Renderer(260, 260, 9, 130, 90)
            r.model(m, tex)
            r.img.save(os.path.join(preview_dir, name + ".png"))
    return out, tex

if __name__ == "__main__":
    build(sys.argv[sys.argv.index("--preview") + 1] if "--preview" in sys.argv else None)
