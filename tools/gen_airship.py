"""
飛空艇（小型・中型・大型）の立体の形（Java の LayerDefinition）と絵（テクスチャ）、アイテムの絵を作る。
    python3 tools/gen_airship.py
座標は「1 = 1/16 ブロック」、y は上が＋、z は前（舳先）が＋。Java 側で y を反転する（バニラのモデルは y が下向き）。
部品の名前の頭で色の付け方が決まる：
  hull_    船体（木の種類の色を掛ける）
  balloon_ 気球（染料の色を掛ける）
  fixed_   色を変えない部品（金具・ロープ・ガラス）
  prop_    プロペラ（回る。色は変えない）
絵は 2 倍の細かさ（宣言は 512、実際の絵は 1024）。
"""
import math, os, random
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), "..")
TEX = os.path.join(ROOT, "src/main/resources/assets/jsn_craft/textures/entity/airship")
ITEM = os.path.join(ROOT, "src/main/resources/assets/jsn_craft/textures/item/airship")
JAVA = os.path.join(ROOT, "src/main/java/com/chigusa/jsncraft/airship/AirshipModels.java")
TW = 512          # 宣言する絵の大きさ
S = 2             # 実際の絵は何倍か


class Box:
    def __init__(self, x, y, z, w, h, d, mat):
        self.x, self.y, self.z, self.w, self.h, self.d, self.mat = x, y, z, w, h, d, mat
        self.u = self.v = 0


class Part:
    def __init__(self, name, pivot=(0, 0, 0)):
        self.name, self.pivot, self.boxes = name, pivot, []

    def box(self, x, y, z, w, h, d, mat):
        self.boxes.append(Box(x, y, z, w, h, d, mat))
        return self


def sym(part, x, y, z, w, h, d, mat):
    """左右対称に2つ置く（x は右側の箱の左端）"""
    part.box(x, y, z, w, h, d, mat)
    part.box(-x - w, y, z, w, h, d, mat)


def ship(tier):
    k = [1.0, 1.45, 1.9][tier - 1]
    P = []
    hull = Part("hull_0"); P.append(hull)
    W = round(12 * k) // 2 * 2; L = round(24 * k) // 2 * 2; Hh = round(6 * k)
    hw, hl = W // 2, L // 2
    hull.box(-hw + 1, 0, -hl, W - 2, 2, L, "wood_dark")              # 船底
    hull.box(-hw, 2, -hl, W, 1, L, "deck")                            # 甲板
    sym(hull, hw - 1, 2, -hl, 1, Hh - 1, L, "wood")                   # 左右の舷
    hull.box(-hw + 1, 2, -hl - 1, W - 2, Hh - 1, 1, "wood")           # 船尾
    for i, (ww, dz) in enumerate(((W - 2, 0), (W - 4, 2), (max(2, W - 8), 4))):   # 舳先（とがる）
        hull.box(-ww / 2, 1 + i * 0.5, hl + dz, ww, Hh - i, 2, "wood")
    hull.box(-hw - 0.5, Hh, -hl, W + 1, 1, L, "wood_dark")           # 舷の上のふち
    trim = Part("fixed_0"); P.append(trim)
    trim.box(-hw - 0.6, Hh - 2.5, -hl - 0.6, W + 1.2, 1, L + 1.2, "brass")   # 金の帯
    trim.box(-0.5, Hh + 1, hl + 4, 1, 3, 2, "brass")                  # 舳先の飾り
    trim.box(-0.5, 2, -hl - 4, 1, round(9 * k), 3, "wood_dark")       # 舵
    # 気球
    bal = Part("balloon_0"); P.append(bal)
    bw, bh, bl = round(15 * k), round(12 * k), round(30 * k)
    by = Hh + round(12 * k)
    bal.box(-bw / 2, by, -bl / 2, bw, bh, bl, "fabric")
    bal.box(-bw / 2 - 1.5, by + 3, -bl / 2 + 2, bw + 3, bh - 6, bl - 4, "fabric")     # 横にふくらみ
    bal.box(-bw / 2 + 3, by + bh, -bl / 2 + 2, bw - 6, 1.5, bl - 4, "fabric")          # 上にふくらみ
    bal.box(-bw / 2 + 3, by - 1.5, -bl / 2 + 2, bw - 6, 1.5, bl - 4, "fabric")         # 下にふくらみ
    bal.box(-bw / 2 + 2, by + 1, bl / 2, bw - 4, bh - 2, 3, "fabric")
    bal.box(-bw / 2 + 2, by + 1, -bl / 2 - 3, bw - 4, bh - 2, 3, "fabric")
    bal.box(-bw / 2 + 4, by + 3, bl / 2 + 3, bw - 8, bh - 6, 2, "fabric")
    bal.box(-bw / 2 + 4, by + 3, -bl / 2 - 5, bw - 8, bh - 6, 2, "fabric")
    rig = Part("fixed_1"); P.append(rig)
    for zz in (-bl / 2 + 4, bl / 2 - 6):
        rig.box(-bw / 2 - 0.4, by - 0.4, zz, bw + 0.8, bh + 0.8, 2, "rope_band")   # 気球をしばる帯
    rig.box(-bw / 2 - 0.3, by + bh / 2 - 1, -bl / 2 - 0.3, bw + 0.6, 2, bl + 0.6, "brass")
    for zz in (-hl + 2, hl - 3):                                        # 船と気球をつなぐ綱
        sym(rig, hw - 2, Hh + 1, zz, 0.6, by - Hh - 1, 0.6, "rope")
    # 尾翼
    fin = Part("fixed_2"); P.append(fin)
    fin.box(-0.5, by + bh - 2, -bl / 2 - 9, 1, round(7 * k), 6, "brass_plate")
    fin.box(-round(8 * k) / 2, by + bh / 2 - 0.5, -bl / 2 - 8, round(8 * k), 1, 5, "brass_plate")
    # プロペラ（船尾）：軸と羽根
    py = Hh + 3
    trim.box(-1, py - 1, -hl - 4, 2, 2, 3, "iron")
    prop = Part("prop_0", (0, py, -hl - 4.5)); P.append(prop)
    pl = round(5 * k)
    prop.box(-0.6, -pl, -0.3, 1.2, pl * 2, 0.6, "blade")
    prop.box(-pl, -0.6, -0.3, pl * 2, 1.2, 0.6, "blade")
    prop.box(-1, -1, -0.8, 2, 2, 1.6, "brass")
    if tier >= 2:
        # 操舵室（窓つき）
        cab = Part("hull_1"); P.append(cab)
        cw, cl = round(W * 0.6), round(L * 0.28)
        cab.box(-cw / 2, Hh, -hl + 3, cw, round(5 * k), cl, "wood")
        cab.box(-cw / 2 - 0.5, Hh + round(5 * k), -hl + 2.5, cw + 1, 1, cl + 1, "wood_dark")
        win = Part("fixed_3"); P.append(win)
        win.box(-cw / 2 - 0.1, Hh + 2, -hl + 4, cw + 0.2, 2, cl - 2, "glass")
        # 左右の小さなプロペラ
        for side, nm in ((1, "prop_1"), (-1, "prop_2")):
            x = side * (hw + 3)
            arm = Part(f"fixed_{4 if side > 0 else 5}"); P.append(arm)
            arm.box(min(x, side * hw) - (0 if side > 0 else 0), Hh - 1, -2, abs(x - side * hw) + 1, 1, 3, "iron") if side > 0 else arm.box(x - 1, Hh - 1, -2, abs(x - side * hw) + 1, 1, 3, "iron")
            arm.box(x - 1, Hh - 2, -3, 2, 3, 4, "brass")
            pr = Part(nm, (x, Hh - 0.5, 1.5)); P.append(pr)
            sl = round(3 * k)
            pr.box(-0.5, -sl, -0.25, 1, sl * 2, 0.5, "blade")
            pr.box(-sl, -0.5, -0.25, sl * 2, 1, 0.5, "blade")
    if tier >= 3:
        # 翼（左右）と旗、舳先の女神像のかわりの星
        wing = Part("hull_2"); P.append(wing)
        sym(wing, hw, Hh - 3, -6, round(10 * k), 1, 12, "wood")
        sym(wing, hw + round(10 * k) - 2, Hh - 4, -7, 3, 2, 14, "wood_dark")
        flag = Part("balloon_1"); P.append(flag)
        flag.box(-0.2, by + bh + 7, -bl / 2 + 2, 0.4, 5, 8, "fabric")
        pole = Part("fixed_6"); P.append(pole)
        pole.box(-0.5, by + bh, -bl / 2 + 1, 1, 13, 1, "brass")
        pole.box(-1.5, Hh + 2, hl + 6, 3, 3, 2, "star")
        sym(pole, hw - 0.5, Hh + 1, hl - 6, 1, 3, 1, "lamp")
        sym(pole, hw - 0.5, Hh + 1, -hl + 2, 1, 3, 1, "lamp")
    return P


# ---------------- 絵の場所を決める（バニラの箱の展開図：横 2d+2w、縦 d+h） ----------------
def pack(parts):
    x = y = row = 0
    for p in parts:
        for b in p.boxes:
            bw = math.ceil(2 * b.d + 2 * b.w) + 1
            bh = math.ceil(b.d + b.h) + 1
            if x + bw > TW:
                x, y, row = 0, y + row, 0
            b.u, b.v = x, y
            x += bw
            row = max(row, bh)
    assert y + row <= TW, "絵の場所が足りない"


# ---------------- 塗る ----------------
def rnd(seed):
    return random.Random(seed)


def fill_face(img, x0, y0, w, h, mat, r, vertical):
    """1つの面を塗る（x0,y0,w,h は実際の絵の画素）"""
    if w <= 0 or h <= 0:
        return
    d = ImageDraw.Draw(img)
    g = lambda v: (v, v, v, 255)
    if mat in ("wood", "deck", "wood_dark"):   # 木：灰色（あとで木の色を掛ける）
        base = 225 if mat != "wood_dark" else 160
        plank = 4 * S
        for yy in range(h):
            row = (yy // plank) if mat != "deck" else 0
            for xx in range(w):
                if mat == "deck":
                    row = xx // plank
                v = base - (row % 3) * 10 + int(r.random() * 10)
                if (mat == "deck" and xx % plank == 0) or (mat != "deck" and yy % plank == 0):
                    v -= 45
                img.putpixel((x0 + xx, y0 + yy), g(max(40, min(255, v))))
        # 木目
        for _ in range(max(1, w * h // 160)):
            sx, sy = x0 + int(r.random() * w), y0 + int(r.random() * h)
            d.line([sx, sy, min(x0 + w - 1, sx + 3 * S), sy], fill=g(base - 30))
    elif mat == "fabric":                       # 布：白っぽい灰色（あとで染料の色を掛ける）
        for yy in range(h):
            for xx in range(w):
                t = yy / max(1, h - 1)
                v = int(250 - 25 * t + (r.random() - 0.5) * 6)
                img.putpixel((x0 + xx, y0 + yy), g(v))
        step = 6 * S
        for xx in range(0, w, step):            # 縫い目
            d.line([x0 + xx, y0, x0 + xx, y0 + h - 1], fill=g(190))
        d.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], outline=g(200))
    else:
        cols = {"brass": (214, 170, 70), "brass_plate": (190, 150, 60), "iron": (150, 154, 162), "rope": (170, 140, 95),
                "rope_band": (150, 115, 70), "glass": (150, 210, 240), "blade": (120, 90, 60), "star": (255, 225, 110),
                "lamp": (255, 200, 90)}
        c = cols.get(mat, (200, 200, 200))
        for yy in range(h):
            for xx in range(w):
                t = yy / max(1, h - 1)
                k = 1.15 - 0.3 * t if mat in ("brass", "brass_plate", "iron", "star") else 1.0
                v = (r.random() - 0.5) * 10
                img.putpixel((x0 + xx, y0 + yy), (int(min(255, c[0] * k + v)), int(min(255, c[1] * k + v)), int(min(255, c[2] * k + v)), 255 if mat != "glass" else 200))
        if mat in ("brass", "brass_plate") and w > 2 * S and h > 2 * S:
            d.line([x0, y0, x0 + w - 1, y0], fill=(255, 240, 180, 255))
        if mat == "rope" or mat == "rope_band":
            for yy in range(0, h, 2 * S):
                d.line([x0, y0 + yy, x0 + w - 1, y0 + yy + S], fill=(120, 95, 60, 255))
        if mat == "glass":
            d.line([x0 + 1, y0 + h - 2, x0 + w // 3, y0 + 1], fill=(255, 255, 255, 230))


def paint(parts, tier):
    img = Image.new("RGBA", (TW * S, TW * S), (0, 0, 0, 0))
    for p in parts:
        for i, b in enumerate(p.boxes):
            r = rnd(hash((tier, p.name, i)) & 0xFFFFFF)
            u, v, w, h, d_ = b.u, b.v, b.w, b.h, b.d
            def px(a): return int(round(a * S))
            regs = [(u + d_, v, w, d_), (u + d_ + w, v, w, d_),            # 上・下
                    (u, v + d_, d_, h), (u + d_, v + d_, w, h), (u + d_ + w, v + d_, d_, h), (u + 2 * d_ + w, v + d_, w, h)]
            for (rx, ry, rw, rh) in regs:
                fill_face(img, px(rx), px(ry), max(1, px(rx + rw) - px(rx)), max(1, px(ry + rh) - px(ry)), b.mat, r, False)
    return img


# ---------------- Java ----------------
def java_num(v):
    s = f"{v:.3f}".rstrip("0").rstrip(".")
    return (s if s not in ("-0", "") else "0") + "F"


def java(all_parts):
    out = []
    names = []
    for tier, parts in enumerate(all_parts, 1):
        lines = [f"    public static LayerDefinition tier{tier}() {{",
                 "        MeshDefinition mesh = new MeshDefinition();",
                 "        PartDefinition root = mesh.getRoot();"]
        for p in parts:
            px_, py_, pz = p.pivot
            cb = "CubeListBuilder.create()"
            for b in p.boxes:
                # y を下向きに：箱の上端が -(y+h)。部品の中心（pivot）からの位置にする
                bx, by_, bz = b.x - px_, -(b.y + b.h) + py_, b.z - pz
                cb += f".texOffs({int(b.u)}, {int(b.v)}).addBox({java_num(bx)}, {java_num(by_)}, {java_num(bz)}, {java_num(b.w)}, {java_num(b.h)}, {java_num(b.d)})"
            lines.append(f'        root.addOrReplaceChild("{p.name}", {cb}, PartPose.offset({java_num(px_)}, {java_num(-py_)}, {java_num(pz)}));')
        lines.append(f"        return LayerDefinition.create(mesh, {TW}, {TW});")
        lines.append("    }")
        out.append("\n".join(lines))
        names.append("{" + ", ".join(f'"{p.name}"' for p in parts) + "}")
    src = f"""package com.chigusa.jsncraft.airship;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** 飛空艇の形（tools/gen_airship.py が作る。手で直さない） */
public final class AirshipModels {{
    /** 段階ごとの部品の名前（頭で色の付け方が決まる：hull_ 木の色 / balloon_ 染料の色 / fixed_ そのまま / prop_ 回る） */
    public static final String[][] PARTS = {{
            {(", " + chr(10) + "            ").join(names)}}};

{chr(10).join(out)}

    private AirshipModels() {{}}
}}
"""
    os.makedirs(os.path.dirname(JAVA), exist_ok=True)
    open(JAVA, "w").write(src)


# ---------------- アイテムの絵 ----------------
def item_icon(tier):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    bal = [(236, 236, 240), (150, 200, 240), (240, 210, 120)][tier - 1]
    dk = tuple(int(c * 0.7) for c in bal)
    d.ellipse([1, 1, 14, 8], fill=bal + (255,), outline=dk + (255,))
    d.line([3, 4, 12, 4], fill=dk + (255,))
    for x in (4, 11):
        d.line([x, 8, x, 10], fill=(140, 110, 70, 255))
    d.polygon([(2, 10), (14, 10), (12, 14), (4, 14)], fill=(150, 105, 60, 255), outline=(90, 60, 30, 255))
    d.line([3, 11, 13, 11], fill=(214, 170, 70, 255))
    if tier >= 2:
        d.rectangle([6, 9, 9, 10], fill=(110, 75, 40, 255))
    if tier >= 3:
        d.line([0, 12, 2, 11], fill=(150, 105, 60, 255)); d.line([14, 11, 15, 12], fill=(150, 105, 60, 255))
        d.point([8, 0], fill=(255, 230, 120, 255))
    return im


if __name__ == "__main__":
    os.makedirs(TEX, exist_ok=True); os.makedirs(ITEM, exist_ok=True)
    allp = []
    for t in (1, 2, 3):
        parts = ship(t)
        pack(parts)
        paint(parts, t).save(os.path.join(TEX, f"airship_{t}.png"))
        item_icon(t).save(os.path.join(ITEM, f"airship_{t}.png"))
        allp.append(parts)
    java(allp)
    print("ok")
