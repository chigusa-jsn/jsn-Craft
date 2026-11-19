"""
戦闘職の服（立体の防具、GeckoLib）を作る。
  - 形（geo）：職業ごとに1つ   → assets/jsn_craft/geo/armor/<style>.geo.json
  - 絵：職業 × 6段階            → assets/jsn_craft/textures/armor/<style>_<tier>.png
  - 持ち物の絵：4部位 × 6段階    → assets/jsn_craft/textures/item/armor/<style>_<tier>_<piece>.png
    python3 tools/gen_armor.py [style ...]
座標はプレイヤーの足元が y=0、頭のてっぺんが y=32（ブロックベンチの「防具」の形と同じ）。
面ごとに絵の場所を決める（per-face UV）ので、面ごとに好きな模様を描ける。
"""
import json, math, os, sys, zlib
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/jsn_craft")
TW = 128          # geo の uv の大きさ
PX = 4            # 絵の 1uv あたりのピクセル（512×512 の絵）
BONES = {
    "armorHead": [0, 24, 0], "armorBody": [0, 24, 0],
    "armorRightArm": [-5, 22, 0], "armorLeftArm": [5, 22, 0],
    "armorRightLeg": [-1.9, 12, 0], "armorLeftLeg": [1.9, 12, 0],
    "armorRightBoot": [-1.9, 12, 0], "armorLeftBoot": [1.9, 12, 0],
}
PIECES = {"head": ["armorHead"], "chest": ["armorBody", "armorRightArm", "armorLeftArm"],
          "legs": ["armorRightLeg", "armorLeftLeg"], "feet": ["armorRightBoot", "armorLeftBoot"]}

def hx(s): s = s.lstrip("#"); return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))
def mix(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))
def dark(c, t=0.35): return mix(c, (0, 0, 0), t)
def light(c, t=0.3): return mix(c, (255, 255, 255), t)

class Cube:
    def __init__(self, bone, origin, size, part, inflate=0.0, rot=None, pivot=None, mirror=False, tags=(), tier_from=1):
        self.tier_from = tier_from
        self.bone, self.origin, self.size, self.part = bone, origin, size, part
        self.inflate, self.rot, self.pivot, self.mirror, self.tags = inflate, rot, pivot, mirror, set(tags)
        self.uv = {}

def mirror_x(c, bone):
    """右のパーツから左を作る（x を反転）"""
    o = [-(c.origin[0] + c.size[0]), c.origin[1], c.origin[2]]
    piv = None if c.pivot is None else [-c.pivot[0], c.pivot[1], c.pivot[2]]
    rot = None if c.rot is None else [c.rot[0], -c.rot[1], -c.rot[2]]
    return Cube(bone, o, list(c.size), c.part, c.inflate, rot, piv, True, c.tags, c.tier_from)

# ---------------- 面の大きさと、絵の置き場所 ----------------
FACES = ("north", "south", "east", "west", "up", "down")
def face_size(c, f):
    w, h, d = c.size
    return {"north": (w, h), "south": (w, h), "east": (d, h), "west": (d, h), "up": (w, d), "down": (w, d)}[f]

def pack(cubes):
    x = y = row = 0.0
    for c in cubes:
        for f in FACES:
            fw, fh = face_size(c, f)
            fw2, fh2 = math.ceil(fw), math.ceil(fh)
            if x + fw2 > TW: x, y, row = 0, y + row, 0
            c.uv[f] = (x, y, fw, fh)
            x += fw2; row = max(row, fh2)
    assert y + row <= TW, "絵が入りきらない"

# ---------------- 塗り ----------------
def rng(seed):
    s = [seed & 0xffffffff]
    def r():
        s[0] = (s[0] * 1103515245 + 12345) & 0x7fffffff
        return s[0] / 0x7fffffff
    return r

CLOTH = ("robe", "hood", "cloth", "cape", "mantle", "hat", "hakama", "sash", "band")
def paint_face(img, c, f, pal, tier, style):
    u, v, fw, fh = c.uv[f]
    x0, y0, W, H = int(u * PX), int(v * PX), max(1, int(round(fw * PX))), max(1, int(round(fh * PX)))
    part = c.part
    base = pal[part]
    shade = {"up": 1.12, "down": 0.72, "north": 1.0, "south": 0.88, "east": 0.94, "west": 0.94}[f]
    col = tuple(min(255, int(ch * shade)) for ch in base)
    d = ImageDraw.Draw(img)
    r = rng(zlib.crc32(f"{style}{tier}{part}{f}{c.origin}".encode()))
    side = f in ("north", "south", "east", "west")
    metal = part in ("metal", "plate", "gem")
    # 地の色：上から下へ少し暗く（金属は強めのつや）
    for yy in range(H):
        t = yy / max(1, H - 1)
        k = (1.06 - 0.14 * t) if not metal else (1.18 - 0.36 * t)
        if not side: k = 1.0
        row = tuple(max(0, min(255, int(ch * k))) for ch in col)
        d.line([x0, y0 + yy, x0 + W - 1, y0 + yy], fill=row + (255,))
    # 布のひだ
    if part in CLOTH and side and W >= 8:
        step = PX * (2 if part != "hakama" else 1.5)
        xx = step
        while xx < W - 2:
            d.line([x0 + int(xx), y0 + 1, x0 + int(xx), y0 + H - 2], fill=dark(col, 0.12 if part != "hakama" else 0.22) + (255,))
            xx += step + int(r() * 2)
    # 金属：まんなかの稜線
    if part in ("metal", "plate") and side and "ridge" in c.tags:
        d.line([x0 + W // 2, y0 + 1, x0 + W // 2, y0 + H - 2], fill=light(col, 0.35) + (255,), width=max(1, PX // 2))
    trim = pal["trim"]
    tw = max(1, PX // 2) * (1 if tier < 4 else 2)
    if side:
        if "hem" in c.tags and tier >= 2:
            d.rectangle([x0, y0 + H - tw - PX // 2, x0 + W - 1, y0 + H - 1 - PX // 2], fill=trim + (255,))
            if tier >= 3:   # すその模様（ひし形）
                for xx in range(x0 + PX, x0 + W - PX, PX * 2):
                    cy = y0 + H - tw - PX // 2 - PX
                    d.polygon([(xx, cy), (xx + PX // 2, cy - PX // 2), (xx + PX, cy), (xx + PX // 2, cy + PX // 2)], fill=trim + (255,))
        if "top_trim" in c.tags and tier >= 2:
            d.rectangle([x0, y0, x0 + W - 1, y0 + tw - 1], fill=trim + (255,))
        if "front" in c.tags and f == "north" and tier >= 2:
            d.rectangle([x0 + W // 2 - tw // 2 - 1, y0, x0 + W // 2 + tw // 2, y0 + H - 1], fill=trim + (255,))
        if "lapel" in c.tags and f == "north":   # 着物のえり（V字に重なる）
            lc = pal.get("lapel", trim)
            for i in range(int(H * 0.55)):
                xa = x0 + i * W // (2 * int(H * 0.55)) ; xb = x0 + W - 1 - i * W // (2 * int(H * 0.55))
                d.line([xa, y0 + i, xa + PX, y0 + i], fill=lc + (255,))
                d.line([xb - PX, y0 + i, xb, y0 + i], fill=lc + (255,))
        if "border" in c.tags and tier >= 3:
            d.rectangle([x0, y0, x0 + W - 1, y0 + H - 1], outline=trim + (255,), width=max(1, PX // 2))
    # 縁の立体感（左上を明るく、右下を暗く）
    d.line([x0, y0, x0 + W - 1, y0], fill=light(col, 0.18) + (255,))
    d.line([x0, y0, x0, y0 + H - 1], fill=light(col, 0.1) + (255,))
    d.line([x0, y0 + H - 1, x0 + W - 1, y0 + H - 1], fill=dark(col, 0.35) + (255,))
    d.line([x0 + W - 1, y0, x0 + W - 1, y0 + H - 1], fill=dark(col, 0.25) + (255,))
    if part == "belt" and f == "north":
        gw = PX * 2
        d.rectangle([x0 + W // 2 - gw // 2, y0 + 1, x0 + W // 2 + gw // 2 - 1, y0 + H - 2], fill=pal["gem"] + (255,), outline=dark(pal["gem"], 0.4) + (255,))
    if part == "gem" and W > 3 and H > 3:
        d.rectangle([x0 + 1, y0 + 1, x0 + W - 2, y0 + H - 2], fill=light(col, 0.2) + (255,))
        d.point([x0 + 1, y0 + 1], fill=(255, 255, 255, 255))
    # びょう
    if part in ("metal", "plate") and tier >= 2 and side and W > 10 and H > 10 and "rivets" in c.tags:
        for (px, py) in ((x0 + 3, y0 + 3), (x0 + W - 4, y0 + 3), (x0 + 3, y0 + H - 4), (x0 + W - 4, y0 + H - 4)):
            d.rectangle([px - 1, py - 1, px, py], fill=trim + (255,))
    # 星銀：金の星と星座
    if tier == 6 and part in CLOTH and side and W >= 12 and H >= 12:
        g = pal["star"]
        pts = []
        for _ in range(int(W * H / 420) + 1):
            sx, sy = x0 + 3 + int(r() * (W - 6)), y0 + 3 + int(r() * (H - 6))
            pts.append((sx, sy))
            d.point([sx, sy], fill=(255, 250, 220, 255))
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)): d.point([sx + dx, sy + dy], fill=g + (255,))
        if len(pts) >= 3:
            d.line(pts[:3], fill=mix(g, col, 0.55) + (255,))
    if "face_hole" in c.tags and f == "north":
        hx0, hx1 = x0 + int(1.2 * PX), x0 + W - int(1.2 * PX)
        hy0, hy1 = y0 + int(2.2 * PX), y0 + H - int(0.6 * PX)
        d.rectangle([hx0 - tw, hy0 - tw, hx1 + tw - 1, hy1 - 1], fill=trim + (255,))
        d.rectangle([hx0, hy0, hx1 - 1, hy1 - 1], fill=(0, 0, 0, 0))
    if "open_bottom" in c.tags and f == "down":
        d.rectangle([x0, y0, x0 + W - 1, y0 + H - 1], fill=(0, 0, 0, 0))
    if "open_top" in c.tags and f == "up":
        d.rectangle([x0, y0, x0 + W - 1, y0 + H - 1], fill=(0, 0, 0, 0))

def build(style, cubes, palettes, emblem=None):
    pack(cubes)
    # geo
    bones = []
    for name, piv in BONES.items():
        cs = []
        for c in cubes:
            if c.bone != name: continue
            e = {"origin": c.origin, "size": c.size, "uv": {f: {"uv": [c.uv[f][0], c.uv[f][1]], "uv_size": [c.uv[f][2], c.uv[f][3]]} for f in FACES}}
            if c.inflate: e["inflate"] = c.inflate
            if c.rot: e["rotation"] = c.rot; e["pivot"] = c.pivot
            cs.append(e)
        bones.append({"name": name, "pivot": piv, "cubes": cs})
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{"description": {"identifier": f"geometry.{style}_armor", "texture_width": TW, "texture_height": TW,
            "visible_bounds_width": 4, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1.5, 0]}, "bones": bones}]}
    os.makedirs(f"{ROOT}/geo/armor", exist_ok=True)
    json.dump(geo, open(f"{ROOT}/geo/armor/{style}.geo.json", "w"), indent=1)
    os.makedirs(f"{ROOT}/textures/armor", exist_ok=True)
    os.makedirs(f"{ROOT}/textures/item/armor", exist_ok=True)
    sheets = []
    for tier in range(1, 7):
        pal = palettes[tier]
        img = Image.new("RGBA", (TW * PX, TW * PX), (0, 0, 0, 0))
        for c in cubes:
            if c.part == "gem" and tier < pal.get("gem_from", 1): continue
            if tier < c.tier_from: continue      # まだ出てこない飾り（絵を描かない＝見えない）
            for f in FACES: paint_face(img, c, f, pal, tier, style)
        if emblem: emblem(img, cubes, pal, tier)
        img.save(f"{ROOT}/textures/armor/{style}_{tier}.png")
        icons = []
        for piece, bnames in PIECES.items():
            ic = icon(img, [c for c in cubes if c.bone in bnames and tier >= c.tier_from and not (c.part == "gem" and tier < pal.get("gem_from", 1))], piece)
            ic.save(f"{ROOT}/textures/item/armor/{style}_{tier}_{piece}.png"); icons.append(ic)
        sheets.append((img, icons))
    return sheets

# ---------------- 持ち物の絵：前から見た形を、そのまま縮めて描く ----------------
def icon(tex, cubes, piece):
    S = 8   # 1uv = 8px で描いてから 32px に
    canvas = Image.new("RGBA", (64 * S, 64 * S), (0, 0, 0, 0))
    # 部位ごとに、腕・脚の間をつめて見やすく
    spread = {"armorRightArm": 1.5, "armorLeftArm": -1.5, "armorRightLeg": -0.6, "armorLeftLeg": 0.6, "armorRightBoot": -0.6, "armorLeftBoot": 0.6}
    items = []
    for c in cubes:
        if c.rot and abs(c.rot[0]) > 25: continue
        inf = c.inflate
        x = c.origin[0] - inf + spread.get(c.bone, 0)
        top = c.origin[1] + c.size[1] + inf
        z = c.origin[2] - inf
        items.append((z, x, top, c))
    items.sort(key=lambda t: -t[0])   # 奥から手前へ
    minx = min(t[1] for t in items); maxx = max(t[1] + t[3].size[0] + 2 * t[3].inflate for t in items)
    miny = min(t[3].origin[1] - t[3].inflate for t in items); maxy = max(t[2] for t in items)
    for z, x, top, c in items:
        u, v, fw, fh = c.uv["north"]
        reg = tex.crop((int(u * PX), int(v * PX), int(u * PX) + max(1, int(round(fw * PX))), int(v * PX) + max(1, int(round(fh * PX)))))
        w = (c.size[0] + 2 * c.inflate) * S; h = (c.size[1] + 2 * c.inflate) * S
        reg = reg.resize((max(1, int(w)), max(1, int(h))), Image.NEAREST)
        canvas.alpha_composite(reg, (int((x - minx) * S), int((maxy - top) * S)))
    bw, bh = int((maxx - minx) * S), int((maxy - miny) * S)
    canvas = canvas.crop((0, 0, max(1, bw), max(1, bh)))
    side = max(bw, bh)
    sq = Image.new("RGBA", (side, side), (0, 0, 0, 0)); sq.alpha_composite(canvas, ((side - bw) // 2, (side - bh) // 2))
    out = sq.resize((28, 28), Image.LANCZOS)
    px = out.load()
    for yy in range(28):
        for xx in range(28):
            r_, g_, b_, a_ = px[xx, yy]; px[xx, yy] = (r_, g_, b_, 255 if a_ > 100 else 0)
    ic = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # ふち取り
    sh = Image.new("RGBA", (32, 32), (0, 0, 0, 0)); sp = sh.load()
    for yy in range(28):
        for xx in range(28):
            if px[xx, yy][3]:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    sp[xx + 2 + dx, yy + 2 + dy] = (20, 16, 28, 255)
    ic.alpha_composite(sh); ic.alpha_composite(out, (2, 2))
    return ic

# ================= 職業ごとの形 =================
def pal(P):
    return {k: {kk: (hx(vv) if isinstance(vv, str) else vv) for kk, vv in v.items()} for k, v in P.items()}

def mage():
    """星詠みの魔導士：とんがり帽子、高いえりのケープ、すそが広がる長いローブ、背中のマント"""
    C = []
    C.append(Cube("armorHead", [-6.5, 30.3, -6.5], [13, 0.8, 13], "hat"))
    C.append(Cube("armorHead", [-4.4, 30.8, -4.4], [8.8, 1.8, 8.8], "band"))
    C.append(Cube("armorHead", [-4, 32.4, -4], [8, 3.6, 8], "hat"))
    C.append(Cube("armorHead", [-3, 35.6, -2.6], [6, 3.6, 6], "hat", rot=[-10, 0, 0], pivot=[0, 35.6, 0]))
    C.append(Cube("armorHead", [-2, 38.6, -0.8], [4, 3.2, 4], "hat", rot=[-22, 0, 0], pivot=[0, 38.6, 0]))
    C.append(Cube("armorHead", [-1, 41.2, 1.6], [2, 2.6, 2], "hat", rot=[-40, 0, 0], pivot=[0, 41, 1.5]))
    C.append(Cube("armorHead", [-1.2, 31.0, -4.9], [2.4, 2.4, 0.5], "gem"))
    C.append(Cube("armorBody", [-4, 12, -2], [8, 12, 4], "robe", 0.5, tags=("front",)))
    C.append(Cube("armorBody", [-4.6, 22.2, -2.9], [9.2, 3.2, 5.8], "mantle", 0.1, tags=("top_trim",)))
    C.append(Cube("armorBody", [-5.6, 19.4, -3.3], [11.2, 3.6, 6.6], "mantle", 0.1, tags=("hem",)))
    C.append(Cube("armorBody", [-4, 11.6, -2], [8, 1.8, 4], "belt", 0.8))
    C.append(Cube("armorBody", [-4.4, 1.0, 2.7], [8.8, 20, 0.6], "cape", 0, rot=[5, 0, 0], pivot=[0, 21, 2.7], tags=("hem",)))
    arm = [Cube("armorRightArm", [-8, 15, -2], [4, 9, 4], "robe", 0.45),
           Cube("armorRightArm", [-9, 10.8, -3], [6, 4.6, 6], "robe", 0.1, tags=("hem",))]
    C += arm + [mirror_x(c, "armorLeftArm") for c in arm]
    leg = [Cube("armorRightLeg", [-3.9, 3, -2], [4, 9, 4], "robe", 0.7),
           Cube("armorRightLeg", [-4.9, 0.3, -2.9], [5.8, 5.2, 5.8], "robe", 0.25, tags=("hem",))]
    C += leg + [mirror_x(c, "armorLeftLeg") for c in leg]
    boot = [Cube("armorRightBoot", [-3.9, 0, -2], [4, 2.4, 4], "boot", 0.45),
            Cube("armorRightBoot", [-3.9, 0, -3.6], [4, 1.1, 1.8], "boot", 0.35)]
    C += boot + [mirror_x(c, "armorLeftBoot") for c in boot]
    P = {
        1: dict(hat="#5e5040", band="#3a2c20", robe="#6c5c48", mantle="#5a4c3c", cape="#4a3e30", belt="#3a2c20", boot="#3a2c20", trim="#b09a78", gem="#c8b090", star="#ffe060", gem_from=9),
        2: dict(hat="#3e4ea0", band="#2a2a44", robe="#4858aa", mantle="#3a4894", cape="#2e3a7a", belt="#2a2a40", boot="#2a2a40", trim="#dfe6f6", gem="#b8ecff", star="#ffe060", gem_from=2),
        3: dict(hat="#22306e", band="#d8b040", robe="#283882", mantle="#1e2a68", cape="#1a2458", belt="#2a2030", boot="#2a2030", trim="#dcb444", gem="#5a9aff", star="#ffe060"),
        4: dict(hat="#262026", band="#b03020", robe="#2e262c", mantle="#3a2a30", cape="#5a1414", belt="#1a1418", boot="#1a1418", trim="#c03824", gem="#ff6a30", star="#ffe060"),
        5: dict(hat="#dfe4f4", band="#8aa0d8", robe="#e8ecf8", mantle="#cad4f0", cape="#8aa0d8", belt="#6a7ab0", boot="#6a7ab0", trim="#7d94d0", gem="#cfe0ff", star="#ffe060"),
        6: dict(hat="#1a1a46", band="#e0b030", robe="#1e1e4e", mantle="#26265e", cape="#141438", belt="#141430", boot="#141430", trim="#e6b836", gem="#ffe060", star="#f0c840"),
    }
    return C, pal(P)

def warrior():
    """蒼刃の剣士（刀）：はちまき、着物のえりの上着、帯、はかま。段が上がると、胴・袖・草ずり・くわがたが増える"""
    C = []
    C.append(Cube("armorHead", [-4, 29.2, -4], [8, 1.8, 8], "band", 0.55, tags=("open_top", "open_bottom")))
    C.append(Cube("armorHead", [-1.6, 26.6, 4.5], [1.4, 3.6, 0.4], "band", 0, rot=[20, 10, 0], pivot=[-1, 30, 4.6]))
    C.append(Cube("armorHead", [0.2, 26.4, 4.5], [1.4, 3.8, 0.4], "band", 0, rot=[16, -12, 0], pivot=[1, 30, 4.6]))
    C.append(Cube("armorHead", [-3.2, 29.2, -4.75], [6.4, 1.8, 0.5], "plate", tags=("ridge",), tier_from=2))
    C.append(Cube("armorHead", [-3.2, 30.6, -5.0], [2.4, 3.4, 0.3], "gem", rot=[0, 0, -28], pivot=[-1, 30.8, -5], tier_from=4))
    C.append(Cube("armorHead", [0.8, 30.6, -5.0], [2.4, 3.4, 0.3], "gem", rot=[0, 0, 28], pivot=[1, 30.8, -5], tier_from=4))
    C.append(Cube("armorBody", [-4, 12, -2], [8, 12, 4], "cloth", 0.45, tags=("lapel",)))
    C.append(Cube("armorBody", [-4, 14.6, -2.75], [8, 6.0, 1.0], "metal", 0.2, tags=("ridge", "rivets", "border"), tier_from=2))
    C.append(Cube("armorBody", [-4, 11.6, -2], [8, 2.0, 4], "sash", 0.8))
    C.append(Cube("armorBody", [-4.6, 3.0, 2.9], [9.2, 19.5, 0.5], "cape", 0, rot=[6, 0, 0], pivot=[0, 22.5, 2.9], tags=("hem",), tier_from=3))
    arm = [Cube("armorRightArm", [-8, 13, -2], [4, 11, 4], "cloth", 0.4),
           Cube("armorRightArm", [-9.6, 16.6, -2.8], [1.2, 6.4, 5.6], "plate", 0, rot=[0, 0, 12], pivot=[-8.4, 23, 0], tags=("rivets", "border"), tier_from=3),
           Cube("armorRightArm", [-8.4, 21.6, -2.8], [5.0, 1.4, 5.6], "plate", tier_from=3),
           Cube("armorRightArm", [-8, 12, -2], [4, 4.5, 4], "metal", 0.62, tags=("border",), tier_from=2)]
    C += arm + [mirror_x(c, "armorLeftArm") for c in arm]
    leg = [Cube("armorRightLeg", [-3.9, 1.4, -2], [4, 10.6, 4], "hakama", 0.62),
           Cube("armorRightLeg", [-4.9, 0.4, -2.9], [5.6, 6, 5.8], "hakama", 0.2, tags=("hem",)),
           Cube("armorRightLeg", [-4.1, 7.6, -3.2], [4.2, 4.2, 0.8], "plate", 0.1, tags=("border",), tier_from=4)]
    C += leg + [mirror_x(c, "armorLeftLeg") for c in leg]
    boot = [Cube("armorRightBoot", [-3.9, 0, -2], [4, 3, 4], "boot", 0.5),
            Cube("armorRightBoot", [-3.9, 0.4, -2.95], [4, 4.6, 1.0], "metal", 0.15, tags=("border",), tier_from=3)]
    C += boot + [mirror_x(c, "armorLeftBoot") for c in boot]
    P = {
        1: dict(band="#2a3a6a", plate="#7a7a80", metal="#8a8a90", cloth="#4c5a6c", cape="#3a5a8a", sash="#2a2a3a", hakama="#3a3e48", boot="#2a2420", lapel="#d8d8d0", trim="#b0b0b0", gem="#9a9aa0", star="#ffe060", gem_from=9),
        2: dict(band="#2a4aa0", plate="#b8bcc8", metal="#c8ccd6", cloth="#34446a", cape="#2a4aa0", sash="#1e2236", hakama="#2a2e3e", boot="#22201e", lapel="#e8ecf4", trim="#e0e4ee", gem="#c8ccd8", star="#ffe060", gem_from=9),
        3: dict(band="#1a5ab0", plate="#5ed6cf", metal="#c8d6de", cloth="#22385e", cape="#1a5ab0", sash="#e8f8f8", hakama="#1c2a44", boot="#1c1a22", lapel="#e8f8f8", trim="#e8f8f8", gem="#a0fff8", star="#ffe060", gem_from=3),
        4: dict(band="#8a1a1a", plate="#48404a", metal="#3a3238", cloth="#2a2228", cape="#7a1616", sash="#c0a040", hakama="#1e181c", boot="#141014", lapel="#c03024", trim="#c0a040", gem="#ff5a30", star="#ffe060"),
        5: dict(band="#3a4a9a", plate="#e6ecfa", metal="#c4d2f0", cloth="#e0e4f0", cape="#3a4a9a", sash="#5a6aa0", hakama="#b8c4e4", boot="#5a6aa0", lapel="#3a4a9a", trim="#8aa0d8", gem="#c8d8ff", star="#ffe060"),
        6: dict(band="#e0b030", plate="#e0b850", metal="#26265c", cloth="#1c1c48", cape="#1a1a44", sash="#e0b030", hakama="#16163a", boot="#101028", lapel="#e8c050", trim="#e8c050", gem="#ffe060", star="#f0c840"),
    }
    return C, pal(P)

STYLES = {"mage": mage, "warrior": warrior}

if __name__ == "__main__":
    names = sys.argv[1:] or list(STYLES)
    prev = []
    for s in names:
        cubes, palette = STYLES[s]()
        sheets = build(s, cubes, palette)
        prev.append((s, sheets))
    # 見本の一覧（持ち物の絵）
    out = os.environ.get("ARMOR_PREVIEW")
    if out:
        rows = len(prev) * 6
        sheet = Image.new("RGBA", (4 * 72 + 8, rows * 72 + 8), (40, 44, 60, 255))
        for i, (s, sh) in enumerate(prev):
            for t, (_, icons) in enumerate(sh):
                for j, ic in enumerate(icons):
                    sheet.alpha_composite(ic.resize((64, 64), Image.NEAREST), (8 + j * 72, 8 + (i * 6 + t) * 72))
        sheet.save(out)
    print("ok", names)
