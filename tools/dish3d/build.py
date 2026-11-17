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
        """お椀の壁：外側の面は outer、内側の面は inner"""
        out = self.ellipse_cells(cx, cz, r_out, r_out, step); inn = self.ellipse_cells(cx, cz, r_in, r_in, step)
        cells = out - inn
        for (i, j) in cells:  # 1マスずつ（内側か外側かを面ごとに決めるため）
            faces = {"up": top, "down": outer}
            for d, (di, dj) in {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}.items():
                nb = (i + di, j + dj)
                if nb in cells: continue
                faces[d] = inner if nb in inn else outer
            self.box([i * step, y1, j * step], [(i + 1) * step, y2, (j + 1) * step], faces=faces)

    # ---- よく使う器 ----
    def plate(self, r=7.0, h=1.0):
        self.disc(8, 8, r, 0, h, top="porcelain_top", side="porcelain_side")

    def bowl(self, r=7.3, soup=None, soup_y=3.9, layers=((3.6, 0.0, 0.6), (5.0, 0.6, 1.5), (6.0, 1.5, 2.5), (6.8, 2.5, 3.5), (7.3, 3.5, 4.4))):
        """どんぶり：下がすぼまって、上に向かって広がる形。layers は (半径, 下, 上)"""
        for i, (rr, y1, y2) in enumerate(layers):
            if i == 0:
                self.disc(8, 8, rr, y1, y2, mat="bowl_outer")
                continue
            self.ring(8, 8, rr, rr - 0.9, y1, y2, "bowl_outer", "bowl_inner", "bowl_rim")
            if y1 < soup_y - 0.3:
                self.disc(8, 8, rr - 0.9, y1, min(y2, soup_y - 0.3), mat="bowl_inner")
        if soup: self.disc(8, 8, layers[-1][0] - 0.9, soup_y - 0.3, soup_y, top=soup, side=soup, bottom=soup)

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

DISHES = {}
def dish(f): DISHES[f.__name__] = f; return f

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

# ======================== 出力 ========================

def build(preview_dir=None):
    tex = materials.build_all(TEX)
    os.makedirs(MODEL, exist_ok=True); os.makedirs(ITEM, exist_ok=True)
    out = {}
    for name, f in DISHES.items():
        d = Dish(); f(d)
        m = d.json(tex)
        json.dump(m, open(os.path.join(MODEL, name + ".json"), "w"), indent=1)
        json.dump({"parent": f"jsn_craft:block/dish3d/{name}"}, open(os.path.join(ITEM, name + ".json"), "w"), indent=2)
        out[name] = (m, d.els)
        print(name, len(d.els), "個の箱")
    if preview_dir:
        os.makedirs(preview_dir, exist_ok=True)
        for name, (m, _) in out.items():
            r = Renderer(260, 200, 9, 130, 40)
            r.model(m, tex)
            r.img.save(os.path.join(preview_dir, name + ".png"))
    return out, tex

if __name__ == "__main__":
    build(sys.argv[sys.argv.index("--preview") + 1] if "--preview" in sys.argv else None)
