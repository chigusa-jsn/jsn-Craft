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
    def __init__(self, bone, origin, size, part, inflate=0.0, rot=None, pivot=None, mirror=False, tags=(), tier_from=1, tier_to=9):
        self.tier_from, self.tier_to = tier_from, tier_to
        self.bone, self.origin, self.size, self.part = bone, origin, size, part
        self.inflate, self.rot, self.pivot, self.mirror, self.tags = inflate, rot, pivot, mirror, set(tags)
        self.uv = {}

def mirror_x(c, bone):
    """右のパーツから左を作る（x を反転）"""
    o = [-(c.origin[0] + c.size[0]), c.origin[1], c.origin[2]]
    piv = None if c.pivot is None else [-c.pivot[0], c.pivot[1], c.pivot[2]]
    rot = None if c.rot is None else [c.rot[0], -c.rot[1], -c.rot[2]]
    return Cube(bone, o, list(c.size), c.part, c.inflate, rot, piv, True, c.tags, c.tier_from, c.tier_to)

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

CLOTH = ("robe", "hood", "cloth", "cape", "mantle", "hat", "hakama", "sash", "band", "tabard", "scarf", "pants", "coat", "vest")
def paint_face(img, c, f, pal, tier, style):
    u, v, fw, fh = c.uv[f]
    x0, y0, W, H = int(u * PX), int(v * PX), max(1, int(round(fw * PX))), max(1, int(round(fh * PX)))
    part = c.part
    base = pal.get(part, pal["trim"])   # 色が決まっていない部品は飾りの色
    shade = {"up": 1.12, "down": 0.72, "north": 1.0, "south": 0.88, "east": 0.94, "west": 0.94}[f]
    col = tuple(min(255, int(ch * shade)) for ch in base)
    d = ImageDraw.Draw(img)
    r = rng(zlib.crc32(f"{style}{tier}{part}{f}{c.origin}".encode()))
    side = f in ("north", "south", "east", "west")
    metal = part in ("metal", "plate", "gem", "blade", "shell", "trim2", "crown", "circlet", "halo")
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
    # 毛皮：ふさふさの毛
    if part in ("fur", "fur2", "mane") and side:
        for _ in range(int(W * H / 10)):
            fx, fy = x0 + int(r() * W), y0 + int(r() * H)
            d.line([fx, fy, fx, min(y0 + H - 1, fy + 2)], fill=(light(col, 0.18) if r() < 0.5 else dark(col, 0.2)) + (255,))
    # うろこ
    if part == "scale" and side:
        for yy in range(0, H, PX):
            off = (yy // PX) % 2 * (PX // 2)
            for xx in range(-PX, W, PX):
                ax = x0 + xx + off
                d.arc([ax, y0 + yy - PX // 2, ax + PX, y0 + yy + PX // 2], 0, 180, fill=dark(col, 0.35) + (255,))
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
    if "torn" in c.tags and side:    # ぼろぼろのすそ
        for xx in range(0, W, PX):
            k = 1 + int(r() * 3)
            d.polygon([(x0 + xx, y0 + H), (x0 + xx + PX, y0 + H), (x0 + xx + PX // 2, y0 + H - k * PX)], fill=(0, 0, 0, 0))
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
            if tier < c.tier_from or tier > c.tier_to: continue      # まだ出てこない飾り（絵を描かない＝見えない）
            for f in FACES: paint_face(img, c, f, pal, tier, style)
        if emblem: emblem(img, cubes, pal, tier)
        img.save(f"{ROOT}/textures/armor/{style}_{tier}.png")
        icons = []
        for piece, bnames in PIECES.items():
            ic = icon(img, [c for c in cubes if c.bone in bnames and c.tier_from <= tier <= c.tier_to and not (c.part == "gem" and tier < pal.get("gem_from", 1))], piece)
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
    # ---- 4段目：立ちえり・肩の飾り板・ブーツの金具
    C.append(Cube("armorBody", [-4.8, 22.6, 1.6], [9.6, 5.4, 0.6], "mantle", 0.05, rot=[-18, 0, 0], pivot=[0, 23, 2], tags=("top_trim", "border"), tier_from=4))
    sc = [Cube("armorBody", [-5.4, 22.6, -2.6], [0.6, 4.2, 4.4], "mantle", 0.05, rot=[0, 0, 10], pivot=[-5, 23, 0], tags=("top_trim",), tier_from=4)]
    C += sc + [mirror_x(c, "armorBody") for c in sc]
    sh = [Cube("armorRightArm", [-9.4, 21.6, -3.2], [5.6, 1.0, 6.4], "plate", 0.1, rot=[0, 0, 14], pivot=[-6, 22, 0], tags=("border",), tier_from=4),
          Cube("armorRightBoot", [-4.0, 1.6, -3.8], [4.2, 0.8, 4.8], "plate", 0.4, tier_from=4)]
    C += sh + [mirror_x(c, "armorLeftArm" if c.bone == "armorRightArm" else "armorLeftBoot") for c in sh]
    # ---- 5段目：帽子の三日月・前垂れ・袖の輪・肩の宝玉
    C.append(Cube("armorHead", [-2.6, 33.4, -4.6], [1.0, 3.2, 0.5], "plate", rot=[0, 0, 25], pivot=[-2, 35, -4.4], tier_from=5))
    C.append(Cube("armorHead", [-1.8, 32.6, -4.6], [3.0, 0.9, 0.5], "plate", tier_from=5))
    C.append(Cube("armorHead", [0.9, 33.4, -4.6], [1.0, 3.2, 0.5], "plate", rot=[0, 0, -25], pivot=[1.4, 35, -4.4], tier_from=5))
    C.append(Cube("armorBody", [-1.9, 2.5, -3.0], [3.8, 9.5, 0.4], "mantle", 0, rot=[-4, 0, 0], pivot=[0, 12, -2.9], tags=("hem", "border"), tier_from=5))
    s5 = [Cube("armorRightArm", [-8.7, 13.8, -2.7], [5.4, 1.0, 5.4], "plate", 0.15, tier_from=5),
          Cube("armorRightArm", [-8.2, 22.8, -1.0], [2.0, 2.0, 2.0], "gem", 0, rot=[45, 0, 45], pivot=[-7.2, 23.8, 0], tier_from=5)]
    C += s5 + [mirror_x(c, "armorLeftArm") for c in s5]
    # ---- 6段目：頭のうしろの星の輪・つばさのようなマント・肩の星
    for i in range(12):
        a = math.radians(i * 30)
        x, y = 6.6 * math.cos(a), 28.5 + 6.6 * math.sin(a)
        C.append(Cube("armorHead", [x - 0.75, y - 0.75, 4.9], [1.5, 1.5, 0.4], "gem", rot=[0, 0, 45], pivot=[x, y, 5.1], tier_from=6))
    C.append(Cube("armorHead", [-0.9, 40.0, 3.0], [1.8, 1.8, 0.4], "gem", rot=[-40, 0, 45], pivot=[0, 41, 3], tier_from=6))
    wg = [Cube("armorBody", [-6.6, 0.0, 2.8], [3.0, 21.0, 0.4], "cape", 0, rot=[8, 18, 10], pivot=[-4.2, 21, 2.8], tags=("hem",), tier_from=6)]
    C += wg + [mirror_x(c, "armorBody") for c in wg]
    st = [Cube("armorRightArm", [-9.2, 23.4, -0.8], [1.6, 1.6, 1.6], "gem", 0, rot=[0, 45, 45], pivot=[-8.4, 24.2, 0], tier_from=6),
          Cube("armorRightLeg", [-4.4, 8.6, -3.4], [1.6, 1.6, 0.4], "gem", 0, rot=[0, 0, 45], pivot=[-3.6, 9.4, -3.2], tier_from=6)]
    C += st + [mirror_x(c, "armorLeftArm" if c.bone == "armorRightArm" else "armorLeftLeg") for c in st]
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
    # ---- 4段目：はかまの横の草ずり
    kz = [Cube("armorRightLeg", [-4.7, 6.4, -2.8], [0.8, 5.4, 5.6], "plate", 0.1, rot=[0, 0, -6], pivot=[-4.3, 12, 0], tags=("border",), tier_from=4)]
    C += kz + [mirror_x(c, "armorLeftLeg") for c in kz]
    # ---- 5段目：かぶとのしころ（首まわり）・前立ての玉・二重の大袖・肩当て
    C.append(Cube("armorHead", [-4.6, 25.6, 3.6], [9.2, 3.8, 0.6], "plate", 0.1, rot=[22, 0, 0], pivot=[0, 29.4, 3.9], tags=("border",), tier_from=5))
    sk = [Cube("armorHead", [-5.1, 25.8, -2.6], [0.6, 3.6, 6.6], "plate", 0.1, rot=[0, 0, -16], pivot=[-4.8, 29.4, 0], tags=("border",), tier_from=5)]
    C += sk + [mirror_x(c, "armorHead") for c in sk]
    C.append(Cube("armorHead", [-0.9, 30.4, -5.3], [1.8, 1.8, 0.4], "gem", rot=[0, 0, 45], pivot=[0, 31.3, -5.1], tier_from=5))
    s5 = [Cube("armorRightArm", [-10.8, 14.4, -3.1], [1.2, 5.6, 6.2], "plate", 0, rot=[0, 0, 20], pivot=[-9.6, 20, 0], tags=("rivets", "border"), tier_from=5),
          Cube("armorRightArm", [-8.6, 22.8, -2.6], [5.2, 1.0, 5.2], "metal", 0.1, tags=("border",), tier_from=5)]
    C += s5 + [mirror_x(c, "armorLeftArm") for c in s5]
    C.append(Cube("armorBody", [-4.4, 20.4, -3.1], [8.8, 2.0, 0.6], "plate", 0.1, tags=("border",), tier_from=5))
    # ---- 6段目：大きな三日月の前立て・なびくえりまき・二枚目のマント・すねの金の飾り
    hn = [Cube("armorHead", [-4.0, 31.0, -5.25], [1.2, 5.5, 0.3], "gem", rot=[0, 0, -30], pivot=[-3.4, 31, -5.1], tier_from=6),
          Cube("armorHead", [-6.7, 35.4, -5.25], [1.1, 2.6, 0.3], "gem", rot=[0, 0, -6], pivot=[-6.1, 35.6, -5.1], tier_from=6)]
    C += hn + [mirror_x(c, "armorHead") for c in hn]
    C.append(Cube("armorBody", [-4.6, 22.6, -2.8], [9.2, 2.2, 5.6], "band", 0.35, tier_from=6))
    sf = [Cube("armorBody", [-3.4, 9.0, 3.0], [1.6, 14.0, 0.4], "band", 0, rot=[18, 12, 0], pivot=[-2.6, 23, 3], tags=("hem",), tier_from=6)]
    C += sf + [mirror_x(c, "armorBody") for c in sf]
    wg = [Cube("armorBody", [-7.0, 1.0, 3.0], [3.4, 20.5, 0.4], "cape", 0, rot=[8, 20, 12], pivot=[-4.4, 22, 3], tags=("hem",), tier_from=6)]
    C += wg + [mirror_x(c, "armorBody") for c in wg]
    sn = [Cube("armorRightBoot", [-3.0, 2.0, -3.6], [2.2, 2.2, 0.4], "gem", 0, rot=[0, 0, 45], pivot=[-1.9, 3.1, -3.4], tier_from=6)]
    C += sn + [mirror_x(c, "armorLeftBoot") for c in sn]
    P = {
        1: dict(band="#2a3a6a", plate="#7a7a80", metal="#8a8a90", cloth="#4c5a6c", cape="#3a5a8a", sash="#2a2a3a", hakama="#3a3e48", boot="#2a2420", lapel="#d8d8d0", trim="#b0b0b0", gem="#9a9aa0", star="#ffe060", gem_from=9),
        2: dict(band="#2a4aa0", plate="#b8bcc8", metal="#c8ccd6", cloth="#34446a", cape="#2a4aa0", sash="#1e2236", hakama="#2a2e3e", boot="#22201e", lapel="#e8ecf4", trim="#e0e4ee", gem="#c8ccd8", star="#ffe060", gem_from=9),
        3: dict(band="#1a5ab0", plate="#5ed6cf", metal="#c8d6de", cloth="#22385e", cape="#1a5ab0", sash="#e8f8f8", hakama="#1c2a44", boot="#1c1a22", lapel="#e8f8f8", trim="#e8f8f8", gem="#a0fff8", star="#ffe060", gem_from=3),
        4: dict(band="#8a1a1a", plate="#48404a", metal="#3a3238", cloth="#2a2228", cape="#7a1616", sash="#c0a040", hakama="#1e181c", boot="#141014", lapel="#c03024", trim="#c0a040", gem="#ff5a30", star="#ffe060"),
        5: dict(band="#3a4a9a", plate="#e6ecfa", metal="#c4d2f0", cloth="#e0e4f0", cape="#3a4a9a", sash="#5a6aa0", hakama="#b8c4e4", boot="#5a6aa0", lapel="#3a4a9a", trim="#8aa0d8", gem="#c8d8ff", star="#ffe060"),
        6: dict(band="#e0b030", plate="#e0b850", metal="#26265c", cloth="#1c1c48", cape="#1a1a44", sash="#e0b030", hakama="#16163a", boot="#101028", lapel="#e8c050", trim="#e8c050", gem="#ffe060", star="#f0c840"),
    }
    return C, pal(P)


# ================= 残りの職業（共通の道具） =================
def K(bone, o, s, part, inf=0.0, rot=None, piv=None, tags=(), t=1, to=9):
    if rot is not None and not any(rot): rot = None
    if rot is not None and piv is None: piv = [o[0] + s[0] / 2, o[1] + s[1] / 2, o[2] + s[2] / 2]
    return Cube(bone, list(o), list(s), part, inf, rot, piv, False, tags, t, to)

LEFT = {"armorRightArm": "armorLeftArm", "armorRightLeg": "armorLeftLeg", "armorRightBoot": "armorLeftBoot",
        "armorHead": "armorHead", "armorBody": "armorBody"}

def both(*cs):
    """右のパーツと、左右反転した左のパーツ"""
    return list(cs) + [mirror_x(c, LEFT[c.bone]) for c in cs]

def tiers(*per, **common):
    out = {}
    for i, d in enumerate(per):
        out[i + 1] = {"star": "#ffe060", **common, **d}
    return pal(out)

def axeman():
    """断鋼の斧使い（重い）：毛皮と鉄の戦士。角のかぶと → 大きな角・肩のとげ → 毛皮のマントと金の冠"""
    C = []
    C += [K("armorHead", [-4, 28.4, -4], [8, 3.6, 8], "fur", 0.6, tags=("open_bottom",), to=1),
          K("armorHead", [-4, 25, -4], [8, 7, 8], "metal", 0.75, tags=("face_hole", "open_bottom", "border"), t=2),
          K("armorHead", [-0.6, 32.4, -4.2], [1.2, 1.4, 8.4], "metal", 0.1, t=5),
          K("armorHead", [-0.6, 25.6, -5.0], [1.2, 4.6, 0.5], "metal", t=5)]
    C += both(K("armorHead", [-6.8, 29.2, -0.8], [2.6, 1.6, 1.6], "horn", t=3),
              K("armorHead", [-7.2, 30.4, -0.7], [1.4, 3.4, 1.4], "horn", rot=[0, 0, -12], piv=[-6.5, 30.4, 0], t=3),
              K("armorHead", [-7.5, 33.4, -0.6], [1.1, 2.6, 1.1], "horn", rot=[0, 0, 18], piv=[-7.0, 33.4, 0], t=4),
              K("armorHead", [-3.6, 32.4, -4.9], [1.2, 2.0, 0.4], "crown", rot=[0, 0, 10], piv=[-3, 32.4, -4.7], t=6))
    C.append(K("armorHead", [-0.7, 32.6, -5.0], [1.4, 2.6, 0.4], "crown", t=6))
    C += [K("armorBody", [-4, 12, -2], [8, 12, 4], "leather", 0.45),
          K("armorBody", [-4.8, 21.4, -2.8], [9.6, 2.8, 5.6], "fur", 0.15),
          K("armorBody", [-3.6, 14.8, -2.8], [7.2, 6.4, 0.9], "metal", 0.15, tags=("ridge", "rivets", "border"), t=2),
          K("armorBody", [-4, 11.6, -2], [8, 2, 4], "belt", 0.8),
          K("armorBody", [-3.4, 14.0, 2.4], [6.8, 7.0, 0.6], "metal", 0.1, tags=("ridge", "border"), t=5),
          K("armorBody", [-5.0, 1.5, 2.9], [10, 21, 0.7], "fur", 0, rot=[6, 0, 0], piv=[0, 22.5, 2.9], t=6),
          K("armorBody", [-4.2, 18.6, -3.1], [8.4, 0.8, 0.4], "crown", 0, rot=[0, 0, 28], piv=[0, 19, -3], t=6)]
    C += both(K("armorRightArm", [-8, 13, -2], [4, 11, 4], "leather", 0.4),
              K("armorRightArm", [-8, 12, -2], [4, 4.5, 4], "metal", 0.62, tags=("border",), t=2),
              K("armorRightArm", [-9.2, 19.6, -3.2], [5.6, 4.6, 6.4], "metal", 0.15, rot=[0, 0, 10], piv=[-6, 22, 0], tags=("rivets", "border"), t=3),
              K("armorRightArm", [-9.9, 17.6, -3.4], [1.2, 4.2, 6.8], "metal", 0, rot=[0, 0, 14], piv=[-8.6, 22, 0], tags=("border",), t=5),
              K("armorRightArm", [-8.4, 24.4, -1.4], [1.2, 2.6, 1.2], "horn", rot=[0, 0, -20], piv=[-7.8, 24.4, -0.8], t=4),
              K("armorRightArm", [-8.4, 24.0, 1.0], [1.0, 2.0, 1.0], "horn", rot=[0, 0, -26], piv=[-7.9, 24.0, 1.5], t=4))
    C += both(K("armorRightLeg", [-3.9, 1, -2], [4, 11, 4], "cloth", 0.55),
              K("armorRightLeg", [-4.0, 4.6, -2.9], [4.2, 3.2, 0.8], "metal", 0.1, tags=("border",), t=3),
              K("armorRightLeg", [-4.4, 7.4, -2.9], [4.6, 4.4, 0.6], "metal", 0.1, rot=[-8, 0, 0], piv=[-2, 11.8, -2.6], tags=("border",), t=5))
    C += both(K("armorRightBoot", [-3.9, 0, -2], [4, 4, 4], "leather", 0.5),
              K("armorRightBoot", [-4.3, 3.4, -2.4], [4.8, 1.6, 4.8], "fur", 0.2),
              K("armorRightBoot", [-3.9, 0, -2.9], [4, 1.8, 1], "metal", 0.3, t=2),
              K("armorRightBoot", [-3.9, 1.8, -2.95], [4, 3.4, 1], "metal", 0.15, tags=("border",), t=4))
    P = tiers(dict(fur="#6a5440", leather="#5a4030", cloth="#4a4038", metal="#7a7470", belt="#3a2a20", horn="#d8ccb0", trim="#a89070", gem="#9a8a70", gem_from=9),
              dict(fur="#6e5a48", leather="#4a3628", cloth="#3c3c44", metal="#b8bcc6", belt="#2e2420", horn="#e0d6c0", trim="#d8dce4", gem="#c8ccd8", gem_from=9),
              dict(fur="#5a4a3e", leather="#2e3a4a", cloth="#283044", metal="#62d8d0", belt="#1e2430", horn="#eae4d4", trim="#e0fafa", gem="#a0fff8"),
              dict(fur="#2a1e1c", leather="#2a2024", cloth="#221a1e", metal="#3e363c", belt="#1a1214", horn="#d8c8b0", trim="#e0582a", gem="#ff6a30"),
              dict(fur="#e8ecf4", leather="#9aa8cc", cloth="#5a6aa0", metal="#e2e8f8", belt="#4a5a90", horn="#f4f6ff", trim="#7d94d0", gem="#cfe0ff"),
              dict(fur="#26265a", leather="#1c1c48", cloth="#16163a", metal="#2c2c66", belt="#101028", horn="#f0e6c8", trim="#e6b836", gem="#ffe060", crown="#e8c040"),
              crown="#d8b040")
    return C, P

def archer():
    """風読みの射手（ふつう）：とんがりフード、短いマント、矢筒。葉の肩当て → 長い葉のマント → 羽根飾りと風のリボン"""
    C = []
    C += [K("armorHead", [-4, 25, -4], [8, 7, 8], "hood", 0.7, tags=("face_hole", "open_bottom")),
          K("armorHead", [-3, 31.8, -1.5], [6, 2.0, 5.5], "hood", 0.3, rot=[22, 0, 0], piv=[0, 31.8, 0]),
          K("armorHead", [-2, 32.2, 2.6], [4, 1.6, 3.4], "hood", 0, rot=[38, 0, 0], piv=[0, 32.4, 2.6])]
    C += both(K("armorHead", [-5.4, 29.2, -1.0], [0.5, 3.4, 2.4], "leaf", rot=[0, 0, 18], piv=[-5, 29, 0], t=5),
              K("armorHead", [-5.6, 31.4, 0.2], [0.4, 3.6, 1.6], "feather", rot=[-20, 0, 26], piv=[-5.2, 31.4, 1], t=5),
              K("armorHead", [-5.8, 33.0, 1.2], [0.4, 3.2, 1.4], "feather", rot=[-35, 0, 34], piv=[-5.4, 33, 2], t=6))
    C.append(K("armorHead", [-1.0, 30.4, -5.0], [2.0, 2.0, 0.4], "gem", rot=[0, 0, 45], piv=[0, 31.4, -4.8], t=4))
    C += [K("armorBody", [-4, 12, -2], [8, 12, 4], "cloth", 0.45, tags=("front",)),
          K("armorBody", [-3.8, 13.0, -2.7], [7.6, 9.0, 0.6], "leather", 0.1, tags=("border",), t=2),
          K("armorBody", [-4.8, 19.0, -2.9], [9.6, 4.8, 5.8], "cape", 0.1, tags=("hem",)),
          K("armorBody", [-4, 11.6, -2], [8, 1.8, 4], "belt", 0.8),
          K("armorBody", [-3.0, 10.0, 2.6], [3.0, 13.0, 3.0], "quiver", 0, rot=[0, 0, -24], piv=[-1.5, 16, 4], tags=("open_top",)),
          K("armorBody", [-2.6, 22.6, 2.9], [2.2, 2.4, 2.2], "feather", 0, rot=[0, 0, -24], piv=[-1.5, 16, 4]),
          K("armorBody", [-4.6, 0.5, 2.9], [9.2, 18.5, 0.5], "cape", 0, rot=[7, 0, 0], piv=[0, 19, 2.9], tags=("hem",), t=4),
          K("armorBody", [-4.0, 18.6, -3.1], [8.0, 0.7, 0.4], "leather", 0, rot=[0, 0, -32], piv=[0, 18.9, -3], t=1)]
    C += both(K("armorBody", [-7.4, 0.5, 2.8], [2.6, 19, 0.4], "ribbon", 0, rot=[10, 22, 14], piv=[-4.6, 20, 2.8], tags=("hem",), t=6))
    C += both(K("armorRightArm", [-8, 13, -2], [4, 11, 4], "cloth", 0.4),
              K("armorRightArm", [-8, 12, -2], [4, 5, 4], "leather", 0.62, tags=("border",), t=2),
              K("armorRightArm", [-9.0, 19.8, -3.0], [5.0, 1.2, 6.0], "leaf", 0.1, rot=[0, 0, 16], piv=[-6, 22, 0], t=3),
              K("armorRightArm", [-9.6, 17.6, -2.6], [1.0, 4.4, 5.2], "leaf", 0, rot=[0, 0, 18], piv=[-8.6, 22, 0], t=5))
    C += both(K("armorRightLeg", [-3.9, 1, -2], [4, 11, 4], "pants", 0.5),
              K("armorRightLeg", [-4.0, 4.4, -2.8], [4.2, 3.0, 0.6], "leather", 0.1, tags=("border",), t=3))
    C += both(K("armorRightBoot", [-3.9, 0, -2], [4, 6, 4], "leather", 0.5, tags=("border",)),
              K("armorRightBoot", [-4.2, 5.4, -2.3], [4.6, 1.2, 4.6], "cloth", 0.3, t=2),
              K("armorRightBoot", [-4.5, 3.0, -0.6], [0.4, 3.2, 1.6], "feather", rot=[0, 0, 18], piv=[-4.3, 3, 0], t=6))
    P = tiers(dict(hood="#5a6a40", cloth="#6a6048", cape="#4e5a38", leather="#6a4a30", belt="#4a3420", quiver="#7a5434", pants="#4a4434", leaf="#6a7a40", feather="#d8d0c0", ribbon="#8aa060", trim="#b0a070", gem="#a0b070", gem_from=9),
              dict(hood="#3e6a34", cloth="#5a5440", cape="#2e5a2c", leather="#7a5434", belt="#4a3420", quiver="#7a5434", pants="#3a3a30", leaf="#5a9a40", feather="#ece6da", ribbon="#7ac060", trim="#d8d0a0", gem="#a0e070", gem_from=9),
              dict(hood="#2a6a44", cloth="#384a40", cape="#1e5a3a", leather="#5a3a28", belt="#2a2018", quiver="#5a3a28", pants="#2a3430", leaf="#5ad8a0", feather="#f4f4f0", ribbon="#60e0b0", trim="#c8f8e8", gem="#60f0c0"),
              dict(hood="#2e2a26", cloth="#3a2420", cape="#5a1a12", leather="#2e2222", belt="#1a1214", quiver="#2a1e1e", pants="#221c1c", leaf="#b04020", feather="#f08040", ribbon="#e05020", trim="#f07030", gem="#ff8040"),
              dict(hood="#e8eef8", cloth="#c8d4ee", cape="#9ab0dc", leather="#6a7ab0", belt="#4a5a90", quiver="#6a7ab0", pants="#8a9ac8", leaf="#d8e4fc", feather="#ffffff", ribbon="#b8ccf4", trim="#7d94d0", gem="#cfe0ff"),
              dict(hood="#1e3a3a", cloth="#1c2c48", cape="#16304a", leather="#2a2a5a", belt="#101028", quiver="#1c1c48", pants="#16163a", leaf="#e6c040", feather="#fff4c8", ribbon="#e6b836", trim="#e6b836", gem="#ffe060"))
    return C, P

def lancer():
    """潮騒の槍兵（ふつう）：うろこのよろいと、ひれのかぶと。貝の肩当て → 背中のひれ → 珊瑚と金の冠"""
    C = []
    C += [K("armorHead", [-4, 25.5, -4], [8, 6.5, 8], "metal", 0.7, tags=("face_hole", "open_bottom", "border")),
          K("armorHead", [-0.5, 31.4, -3.6], [1.0, 3.0, 7.6], "fin", 0, rot=[-8, 0, 0], piv=[0, 32, 0], t=2),
          K("armorHead", [-0.4, 33.4, -1.0], [0.8, 2.6, 5.4], "fin", 0, rot=[-20, 0, 0], piv=[0, 33.4, 1], t=4)]
    C += both(K("armorHead", [-5.6, 27.6, -1.6], [0.6, 3.6, 4.6], "fin", rot=[0, -18, -10], piv=[-5, 28, 0], t=3),
              K("armorHead", [-3.6, 32.0, -4.6], [1.0, 3.4, 0.4], "coral", rot=[0, 0, 18], piv=[-3, 32, -4.5], t=6),
              K("armorHead", [-5.0, 34.0, -4.6], [0.9, 2.0, 0.4], "coral", rot=[0, 0, 50], piv=[-3.6, 34.6, -4.5], t=6))
    C.append(K("armorHead", [-0.8, 30.6, -5.1], [1.6, 1.6, 0.4], "gem", rot=[0, 0, 45], piv=[0, 31.4, -4.9], t=3))
    C += [K("armorBody", [-4, 12, -2], [8, 12, 4], "scale", 0.45, tags=("border",)),
          K("armorBody", [-4, 11.6, -2], [8, 2.0, 4], "sash", 0.8),
          K("armorBody", [-1.8, 4.0, -3.0], [3.6, 8.0, 0.4], "sash", 0, rot=[-4, 0, 0], piv=[0, 12, -2.9], tags=("hem",), t=2),
          K("armorBody", [-3.6, 15.0, -2.8], [7.2, 6.4, 0.8], "metal", 0.1, tags=("ridge", "border"), t=3),
          K("armorBody", [-4.4, 2.0, 2.8], [8.8, 20, 0.5], "cape", 0, rot=[6, 0, 0], piv=[0, 22, 2.8], tags=("hem",), t=4)]
    C += both(K("armorBody", [-3.0, 8.0, 3.1], [1.0, 14.0, 3.4], "fin", 0, rot=[10, -14, 0], piv=[-2.5, 22, 3.1], t=5),
              K("armorBody", [-7.4, 1.0, 2.8], [2.8, 19.5, 0.4], "cape", 0, rot=[8, 20, 12], piv=[-4.4, 21, 2.8], tags=("hem",), t=6))
    C += both(K("armorRightArm", [-8, 13, -2], [4, 11, 4], "scale", 0.4),
              K("armorRightArm", [-8, 12, -2], [4, 4.5, 4], "metal", 0.62, tags=("border",), t=2),
              K("armorRightArm", [-9.4, 19.4, -3.2], [5.8, 4.6, 6.4], "shell", 0.1, rot=[0, 0, 12], piv=[-6, 22, 0], tags=("border", "ridge"), t=4),
              K("armorRightArm", [-9.8, 18.0, -1.2], [1.0, 3.4, 2.4], "fin", 0, rot=[0, 0, 28], piv=[-9, 21, 0], t=5))
    C += both(K("armorRightLeg", [-3.9, 1, -2], [4, 11, 4], "cloth", 0.55),
              K("armorRightLeg", [-4.4, 6.6, -2.8], [4.6, 5.2, 5.6], "scale", 0.15, tags=("border",), t=3))
    C += both(K("armorRightBoot", [-3.9, 0, -2], [4, 4.4, 4], "metal", 0.5, tags=("border",)),
              K("armorRightBoot", [-4.5, 2.4, -1.0], [0.5, 2.6, 2.4], "fin", rot=[0, 0, 20], piv=[-4.2, 2.4, 0], t=5))
    P = tiers(dict(metal="#7a8088", scale="#4a6a78", sash="#2a4050", cloth="#3a4a54", cape="#2a4a5a", fin="#8aa0a8", shell="#c8b8a8", coral="#e07060", trim="#a0b0b8", gem="#a0c0c8", gem_from=9),
              dict(metal="#b8c0cc", scale="#3e7a90", sash="#1e3a5a", cloth="#2c3c4c", cape="#1e4a6a", fin="#9ad0e0", shell="#e8d8c8", coral="#f08070", trim="#dce8f0", gem="#b8f0ff", gem_from=9),
              dict(metal="#8adcd0", scale="#2a8a8a", sash="#1a3a54", cloth="#1c3040", cape="#1a5a6a", fin="#7af0e0", shell="#f0e8e0", coral="#ff8070", trim="#e0fff8", gem="#80fff0"),
              dict(metal="#3e363c", scale="#2a2e3a", sash="#5a1a1a", cloth="#1e1a20", cape="#5a1414", fin="#c04030", shell="#4a3a3a", coral="#e05030", trim="#e06030", gem="#ff6a30"),
              dict(metal="#e2e8f8", scale="#a8bce4", sash="#4a5a90", cloth="#8a9ac8", cape="#8aa0d8", fin="#d0e0ff", shell="#f4f6ff", coral="#b0c4f0", trim="#7d94d0", gem="#cfe0ff"),
              dict(metal="#2c2c66", scale="#1e2a5a", sash="#e0b030", cloth="#16163a", cape="#141438", fin="#40c8d8", shell="#e8d8a0", coral="#f0c040", trim="#e6b836", gem="#ffe060"))
    return C, P

def shield_knight():
    """不落の盾騎士（重い）：全身よろい。大きな肩当て → 陣羽織と羽根飾り → 金のつばさのかぶととマント"""
    C = []
    C += [K("armorHead", [-4, 28.6, -4], [8, 3.4, 8], "metal", 0.6, tags=("open_bottom", "border"), to=1),
          K("armorHead", [-5.6, 28.4, -5.6], [11.2, 0.6, 11.2], "metal", 0.0, to=1),
          K("armorHead", [-4, 24.6, -4], [8, 7.4, 8], "metal", 0.75, tags=("face_hole", "open_bottom", "ridge", "border"), t=2),
          K("armorHead", [-3.4, 27.2, -5.0], [6.8, 0.8, 0.5], "trim2", t=2),
          K("armorHead", [-0.7, 32.4, -2.4], [1.4, 3.4, 5.6], "plume", 0, rot=[-15, 0, 0], piv=[0, 32.4, 0], t=4),
          K("armorHead", [-0.6, 33.6, 2.0], [1.2, 3.0, 3.4], "plume", 0, rot=[-50, 0, 0], piv=[0, 33.6, 2.6], t=4)]
    C += both(K("armorHead", [-6.8, 28.0, -0.4], [2.2, 5.2, 0.4], "wing", rot=[0, -20, 18], piv=[-4.8, 28.4, 0], t=6),
              K("armorHead", [-8.4, 30.6, -0.2], [2.0, 4.0, 0.4], "wing", rot=[0, -20, 34], piv=[-6.6, 31, 0], t=6))
    C += [K("armorBody", [-4, 12, -2], [8, 12, 4], "metal", 0.5, tags=("ridge", "rivets", "border")),
          K("armorBody", [-4.4, 22.0, -2.6], [8.8, 2.6, 5.2], "metal", 0.2, tags=("border",), t=5),
          K("armorBody", [-4, 11.6, -2], [8, 2, 4], "belt", 0.8),
          K("armorBody", [-3.0, 3.0, -2.95], [6.0, 16.0, 0.4], "tabard", 0.35, tags=("hem", "front"), t=4),
          K("armorBody", [-3.0, 3.0, 2.55], [6.0, 16.0, 0.4], "tabard", 0.35, tags=("hem",), t=4),
          K("armorBody", [-1.5, 15.4, -3.5], [3.0, 3.0, 0.3], "trim2", rot=[0, 0, 45], piv=[0, 16.9, -3.4], t=4),
          K("armorBody", [-5.0, 1.0, 3.2], [10, 21.5, 0.6], "cape", 0, rot=[5, 0, 0], piv=[0, 22.5, 3.2], tags=("hem",), t=6)]
    C += both(K("armorRightArm", [-8, 12, -2], [4, 12, 4], "metal", 0.45, tags=("border",)),
              K("armorRightArm", [-9.0, 19.8, -3.0], [5.4, 4.2, 6.0], "metal", 0.15, rot=[0, 0, 8], piv=[-6, 22, 0], tags=("rivets", "border"), to=2),
              K("armorRightArm", [-9.8, 19.0, -3.6], [6.4, 5.2, 7.2], "metal", 0.15, rot=[0, 0, 10], piv=[-6, 22, 0], tags=("rivets", "border", "ridge"), t=3),
              K("armorRightArm", [-10.6, 16.8, -3.8], [1.4, 4.6, 7.6], "metal", 0, rot=[0, 0, 14], piv=[-9, 22, 0], tags=("border",), t=5),
              K("armorRightArm", [-9.4, 24.0, -2.6], [4.6, 0.8, 5.2], "trim2", rot=[0, 0, 10], piv=[-6, 22, 0], t=5))
    C += both(K("armorRightLeg", [-3.9, 0.5, -2], [4, 11.5, 4], "metal", 0.55, tags=("border",)),
              K("armorRightLeg", [-4.1, 4.4, -2.9], [4.2, 3.2, 0.8], "metal", 0.15, tags=("ridge", "border"), t=2),
              K("armorRightLeg", [-4.6, 7.0, -2.9], [4.8, 4.8, 5.8], "metal", 0.15, tags=("border",), t=3))
    C += both(K("armorRightBoot", [-3.9, 0, -2], [4, 4.6, 4], "metal", 0.55, tags=("border",)),
              K("armorRightBoot", [-3.9, 0, -3.6], [4, 1.6, 1.6], "metal", 0.4, t=2))
    P = tiers(dict(metal="#8a847c", belt="#4a3828", tabard="#5a4a3a", cape="#5a4a3a", plume="#b0a090", wing="#d0c8b8", trim="#b0a888", trim2="#c0b090", gem="#a09880", gem_from=9),
              dict(metal="#c4c8d2", belt="#3a2c22", tabard="#2a4a9a", cape="#2a4a9a", plume="#d03030", wing="#ffffff", trim="#e8ecf2", trim2="#e0c060", gem="#d0d4e0", gem_from=9),
              dict(metal="#9ae8e0", belt="#2a2a3a", tabard="#1e3a7a", cape="#1e3a7a", plume="#ffffff", wing="#ffffff", trim="#f0fffc", trim2="#f0d070", gem="#a0fff8"),
              dict(metal="#46404a", belt="#1a1418", tabard="#7a1616", cape="#6a1212", plume="#d04020", wing="#e05030", trim="#d0a040", trim2="#e0b040", gem="#ff6a30"),
              dict(metal="#e8eefc", belt="#4a5a90", tabard="#3a4a9a", cape="#3a4a9a", plume="#c8d8ff", wing="#ffffff", trim="#8aa0d8", trim2="#9ab0e8", gem="#cfe0ff"),
              dict(metal="#30306a", belt="#101028", tabard="#16163a", cape="#141438", plume="#f0c840", wing="#f0c840", trim="#e6b836", trim2="#f0c840", gem="#ffe060"))
    return C, P

def beast():
    """群狼の獣使い（ふつう）：おおかみの頭のフードと毛皮。耳 → 牙の首飾りと尻尾 → 金のたてがみ"""
    C = []
    C += [K("armorHead", [-4, 25, -4], [8, 7, 8], "hood", 0.7, tags=("face_hole", "open_bottom")),
          K("armorHead", [-2.6, 31.2, -6.0], [5.2, 2.0, 3.0], "fur", 0.1, t=2),
          K("armorHead", [-1.6, 30.6, -7.2], [3.2, 1.6, 1.6], "fur", 0, t=2),
          K("armorHead", [-0.5, 31.9, -7.4], [1.0, 0.6, 0.6], "nose", t=2),
          K("armorHead", [-4.6, 24.0, 3.0], [9.2, 5.0, 2.0], "mane", 0.2, rot=[18, 0, 0], piv=[0, 29, 3.5], t=6)]
    C += both(K("armorHead", [-4.0, 32.2, -1.0], [2.0, 2.6, 1.0], "fur", rot=[0, 0, 12], piv=[-3, 32.2, -0.5], t=1),
              K("armorHead", [-6.0, 25.0, -2.4], [1.6, 7.0, 6.4], "mane", 0.2, rot=[0, 0, -10], piv=[-4.5, 31, 0], t=6),
              K("armorHead", [-4.8, 30.6, -6.2], [0.6, 1.2, 0.6], "fang", t=4))
    C += [K("armorBody", [-4, 12, -2], [8, 12, 4], "leather", 0.45, tags=("lapel",)),
          K("armorBody", [-5.0, 20.6, -3.0], [10, 3.6, 6.0], "fur", 0.2, tags=("hem",)),
          K("armorBody", [-4, 11.6, -2], [8, 2, 4], "belt", 0.8),
          K("armorBody", [-3.0, 18.8, -3.0], [6.0, 0.6, 0.4], "cord", 0, rot=[0, 0, 0], t=3),
          K("armorBody", [-1.6, 4.0, 2.4], [3.2, 9.0, 3.2], "fur", 0.2, rot=[28, 0, 0], piv=[0, 12, 2.4], t=4),
          K("armorBody", [-1.0, -0.4, 6.4], [2.0, 4.6, 2.0], "fur2", 0.2, rot=[48, 0, 0], piv=[0, 4.2, 6.4], t=5),
          K("armorBody", [-5.4, 2.0, 3.0], [10.8, 21, 0.7], "fur", 0, rot=[6, 0, 0], piv=[0, 23, 3], tags=("hem",), t=5)]
    for i, x in enumerate((-2.4, -0.6, 1.2)):
        C.append(K("armorBody", [x, 17.4, -3.1], [0.8, 1.6, 0.4], "fang", t=3))
    C += both(K("armorRightArm", [-8, 13, -2], [4, 11, 4], "leather", 0.4),
              K("armorRightArm", [-8, 12, -2], [4, 4.5, 4], "fur", 0.62, t=2),
              K("armorRightArm", [-9.2, 19.6, -3.0], [5.4, 4.4, 6.0], "fur", 0.2, rot=[0, 0, 10], piv=[-6, 22, 0], t=3),
              K("armorRightArm", [-8.8, 12.6, -2.8], [0.8, 3.0, 0.8], "fang", rot=[0, 0, -20], piv=[-8.4, 15, -2.4], t=5),
              K("armorRightArm", [-8.8, 12.6, -0.4], [0.8, 3.0, 0.8], "fang", rot=[0, 0, -20], piv=[-8.4, 15, 0], t=5))
    C += both(K("armorRightLeg", [-3.9, 1, -2], [4, 11, 4], "cloth", 0.5),
              K("armorRightLeg", [-4.4, 1.0, -2.6], [4.8, 5.0, 5.2], "fur", 0.1, t=3))
    C += both(K("armorRightBoot", [-3.9, 0, -2], [4, 4, 4], "leather", 0.5),
              K("armorRightBoot", [-4.3, 3.2, -2.4], [4.8, 1.6, 4.8], "fur", 0.2, t=2))
    P = tiers(dict(hood="#7a6a58", fur="#8a7a66", fur2="#d8d0c0", leather="#5a4030", cloth="#4a4036", belt="#3a2a20", cord="#5a4030", fang="#ece4d0", nose="#2a2020", mane="#c89040", trim="#b09a78", gem="#a09078", gem_from=9),
              dict(hood="#6a6a72", fur="#8a8a94", fur2="#e8e8ee", leather="#4a3a2c", cloth="#3a3a40", belt="#2a2420", cord="#3a2c22", fang="#f4efe0", nose="#1a1a1a", mane="#c89040", trim="#dcdce4", gem="#c8ccd8", gem_from=9),
              dict(hood="#4a5a6a", fur="#6a7a8a", fur2="#f0f4f8", leather="#2e3a48", cloth="#22303c", belt="#1a2028", cord="#2a3a4a", fang="#ffffff", nose="#1a1a1a", mane="#c89040", trim="#a0fff0", gem="#80fff0"),
              dict(hood="#2a2224", fur="#3a2e30", fur2="#e05030", leather="#2a2022", cloth="#1e1a1c", belt="#1a1214", cord="#7a1a1a", fang="#f0e0d0", nose="#c03020", mane="#c04020", trim="#e06030", gem="#ff6a30"),
              dict(hood="#e8ecf6", fur="#f0f2fa", fur2="#a8bce4", leather="#8a9ac8", cloth="#6a7ab0", belt="#4a5a90", cord="#7d94d0", fang="#ffffff", nose="#4a5a90", mane="#cad8f8", trim="#7d94d0", gem="#cfe0ff"),
              dict(hood="#2a2a5a", fur="#34346e", fur2="#fff0c0", leather="#1c1c48", cloth="#16163a", belt="#101028", cord="#e6b836", fang="#fff8e0", nose="#101028", mane="#f0b830", trim="#e6b836", gem="#ffe060"))
    return C, P

def brawler():
    """鉄拳の拳闘士（軽い）：はちまき、そでなしの道着、長い帯、手首の布。肩当て → とげの腕輪 → 炎のなびく帯と金の龍"""
    C = []
    C += [K("armorHead", [-4, 28.6, -4], [8, 1.8, 8], "band", 0.55, tags=("open_top", "open_bottom")),
          K("armorHead", [-1.8, 28.8, -4.75], [3.6, 1.4, 0.4], "metal", t=2),
          K("armorHead", [-0.9, 30.4, -4.9], [1.8, 1.8, 0.4], "gem", rot=[0, 0, 45], piv=[0, 31.3, -4.7], t=4)]
    C += both(K("armorHead", [-2.0, 24.0, 4.5], [1.4, 5.0, 0.4], "band", 0, rot=[22, 12, 0], piv=[-1.4, 29.4, 4.6]),
              K("armorHead", [-6.4, 30.0, -4.6], [1.0, 3.4, 0.4], "crown", rot=[0, 0, 30], piv=[-3.6, 30, -4.5], t=6))
    C += [K("armorBody", [-4, 12, -2], [8, 12, 4], "cloth", 0.45, tags=("lapel",)),
          K("armorBody", [-4, 11.4, -2], [8, 2.4, 4], "sash", 0.85),
          K("armorBody", [-3.4, 3.0, 2.8], [1.6, 9.4, 0.4], "sash", 0, rot=[10, 0, 8], piv=[-2.6, 12.4, 2.9]),
          K("armorBody", [-1.0, 4.0, 2.8], [1.6, 8.4, 0.4], "sash", 0, rot=[14, 0, -6], piv=[-0.2, 12.4, 2.9]),
          K("armorBody", [-3.8, 14.0, -2.8], [7.6, 5.0, 0.6], "metal", 0.1, tags=("border",), t=3)]
    C += both(K("armorBody", [-6.4, 0.0, 2.9], [2.4, 19.0, 0.4], "flame", 0, rot=[12, 18, 14], piv=[-4.2, 19, 2.9], tags=("hem",), t=5),
              K("armorBody", [-3.6, 19.6, -3.2], [1.2, 1.2, 0.4], "crown", rot=[0, 0, 45], piv=[-3, 20.2, -3.1], t=6))
    C += [K("armorRightArm", [-9.4, 19.4, -3.0], [5.6, 4.6, 6.0], "metal", 0.1, rot=[0, 0, 12], piv=[-6, 22, 0], tags=("rivets", "border"), t=3),
          K("armorRightArm", [-10.0, 17.0, -2.8], [1.2, 4.2, 5.6], "metal", 0, rot=[0, 0, 16], piv=[-9, 22, 0], tags=("border",), t=5)]
    C += both(K("armorRightArm", [-8, 18.6, -2], [4, 5.4, 4], "cloth", 0.4, tags=("hem",)),
              K("armorRightArm", [-8, 12, -2], [4, 5, 4], "wrap", 0.55),
              K("armorRightArm", [-8.4, 13.8, -2.4], [4.8, 1.6, 4.8], "metal", 0.2, tags=("border",), t=2),
              K("armorRightArm", [-9.3, 14.0, -0.5], [1.0, 1.2, 1.0], "spike", rot=[0, 0, 0], t=4),
              K("armorRightArm", [-6.5, 14.0, -3.3], [1.0, 1.2, 1.0], "spike", t=4))
    C += both(K("armorRightLeg", [-3.9, 3, -2], [4, 9, 4], "pants", 0.65),
              K("armorRightLeg", [-4.6, 1.2, -2.7], [5.4, 4.4, 5.4], "pants", 0.2))
    C += both(K("armorRightBoot", [-3.9, 0, -2], [4, 3.6, 4], "wrap", 0.5),
              K("armorRightBoot", [-3.9, 1.4, -2.95], [4, 3.2, 1.0], "metal", 0.15, tags=("border",), t=3))
    P = tiers(dict(band="#8a3a2a", cloth="#d8d0c0", sash="#4a3a30", wrap="#e8e0d0", pants="#5a5048", metal="#8a8480", flame="#e07030", spike="#a0a0a0", crown="#e0b040", trim="#b0a080", lapel="#5a4a3a", gem="#c0a070", gem_from=9),
              dict(band="#c03020", cloth="#ece8e0", sash="#2a2a3a", wrap="#f4f0e8", pants="#3a3a48", metal="#c0c4ce", flame="#f08030", spike="#e0e4ec", crown="#e0b040", trim="#e0e4ee", lapel="#2a2a3a", gem="#d0d4e0", gem_from=9),
              dict(band="#e0a020", cloth="#f4f4f0", sash="#1a1a1a", wrap="#ffffff", pants="#2a2a30", metal="#2a2630", flame="#ff9030", spike="#d0d0e0", crown="#e0b040", trim="#e0a020", lapel="#e0a020", gem="#ffd060"),
              dict(band="#c02818", cloth="#2a2224", sash="#c02818", wrap="#3a2e2e", pants="#1e1a1c", metal="#46404a", flame="#ff5020", spike="#e06030", crown="#e0b040", trim="#e05024", lapel="#c02818", gem="#ff6a30"),
              dict(band="#5a6aa0", cloth="#eef2fa", sash="#4a5a90", wrap="#dce4f8", pants="#9aaad4", metal="#e2e8f8", flame="#a8c8ff", spike="#ffffff", crown="#cfe0ff", trim="#7d94d0", lapel="#4a5a90", gem="#cfe0ff"),
              dict(band="#e0b030", cloth="#1e1e4e", sash="#e0b030", wrap="#26265e", pants="#16163a", metal="#2c2c66", flame="#ffc040", spike="#f0d060", crown="#f0c840", trim="#e6b836", lapel="#e6b836", gem="#ffe060"))
    return C, P

def elementalist():
    """精霊の術師（軽い）：葉の飾りのフードと重ねたローブ。肩の精霊石 → 背中の浮かぶ水晶 → 四つの精霊の玉"""
    C = []
    C += [K("armorHead", [-4, 25, -4], [8, 7, 8], "hood", 0.7, tags=("face_hole", "open_bottom")),
          K("armorHead", [-4.6, 27.0, 3.2], [9.2, 5.6, 1.4], "hood", 0.2, rot=[14, 0, 0], piv=[0, 32, 3.6]),
          K("armorHead", [-3.6, 29.8, -4.9], [7.2, 0.8, 0.4], "circlet", t=2),
          K("armorHead", [-0.9, 30.2, -5.1], [1.8, 1.8, 0.4], "gem", rot=[0, 0, 45], piv=[0, 31.1, -4.9], t=2)]
    C += both(K("armorHead", [-5.4, 31.0, -2.0], [0.6, 3.2, 2.4], "leaf", rot=[0, 0, 24], piv=[-5, 31, -1], t=3),
              K("armorHead", [-5.6, 32.6, 0.6], [0.6, 3.6, 2.0], "leaf", rot=[-18, 0, 30], piv=[-5.2, 32.6, 1.4], t=5))
    C += [K("armorBody", [-4, 12, -2], [8, 12, 4], "robe", 0.5, tags=("front",)),
          K("armorBody", [-4.8, 19.4, -2.9], [9.6, 4.4, 5.8], "mantle", 0.1, tags=("hem",)),
          K("armorBody", [-4, 11.6, -2], [8, 1.8, 4], "belt", 0.8),
          K("armorBody", [-2.0, 2.4, -3.0], [4.0, 9.6, 0.4], "mantle", 0, rot=[-4, 0, 0], piv=[0, 12, -2.9], tags=("hem",), t=2),
          K("armorBody", [-4.4, 1.0, 2.8], [8.8, 20, 0.5], "cape", 0, rot=[5, 0, 0], piv=[0, 21, 2.8], tags=("hem",), t=3),
          K("armorBody", [-1.2, 20.0, 6.0], [2.4, 4.4, 2.4], "gem", 0, rot=[0, 45, 0], piv=[0, 22, 7.2], t=5),
          K("armorBody", [-0.6, 17.6, 6.6], [1.2, 2.0, 1.2], "gem", 0, rot=[0, 45, 0], piv=[0, 18.6, 7.2], t=5)]
    orbs = [("fire", -7.0, 28.0), ("water", 7.0, 28.0), ("wind", -7.6, 18.0), ("earth", 7.6, 18.0)]
    for part, x, y in orbs:
        C.append(K("armorBody", [x - 1.0, y - 1.0, 2.4], [2.0, 2.0, 2.0], part, 0, rot=[45, 45, 0], piv=[x, y, 3.4], t=6))
    C += both(K("armorRightArm", [-8, 15, -2], [4, 9, 4], "robe", 0.45),
              K("armorRightArm", [-9, 10.8, -3], [6, 4.6, 6], "robe", 0.1, tags=("hem",)),
              K("armorRightArm", [-9.2, 21.4, -3.0], [5.4, 1.0, 6.0], "leaf", 0.1, rot=[0, 0, 14], piv=[-6, 22, 0], t=3),
              K("armorRightArm", [-8.4, 22.6, -0.9], [1.8, 1.8, 1.8], "gem", 0, rot=[45, 0, 45], piv=[-7.5, 23.5, 0], t=4),
              K("armorRightArm", [-9.4, 18.6, -2.6], [1.0, 3.8, 5.2], "leaf", 0, rot=[0, 0, 18], piv=[-8.4, 22, 0], t=5))
    C += both(K("armorRightLeg", [-3.9, 3, -2], [4, 9, 4], "robe", 0.7),
              K("armorRightLeg", [-4.9, 0.3, -2.9], [5.8, 5.2, 5.8], "robe", 0.25, tags=("hem",)))
    C += both(K("armorRightBoot", [-3.9, 0, -2], [4, 2.4, 4], "boot", 0.45),
              K("armorRightBoot", [-4.3, 1.6, -2.4], [4.8, 1.0, 4.8], "leaf", 0.2, t=4))
    common = dict(fire="#ff6030", water="#40a0ff", wind="#a0ffd0", earth="#c09040")
    P = tiers(dict(hood="#5a6a48", robe="#6a6a50", mantle="#4e5a40", cape="#44503a", belt="#3a3020", boot="#3a3020", leaf="#7a9050", circlet="#a09070", trim="#a8a078", gem="#a0c080", gem_from=9, **common),
              dict(hood="#3e7a4a", robe="#4a6a50", mantle="#2e5a3a", cape="#2a4a34", belt="#2a2a20", boot="#2a2a20", leaf="#60b050", circlet="#d0d8c0", trim="#d8e8c8", gem="#a0ff80", **common),
              dict(hood="#2a6a6a", robe="#2a4a58", mantle="#1e5a5a", cape="#1a4a50", belt="#1a2428", boot="#1a2428", leaf="#50e0b0", circlet="#e0fff8", trim="#c8fff0", gem="#60ffe0", **common),
              dict(hood="#2a2226", robe="#2e2428", mantle="#4a1e1a", cape="#5a1810", belt="#1a1214", boot="#1a1214", leaf="#d05020", circlet="#e0a040", trim="#e06030", gem="#ff7030", **common),
              dict(hood="#e8f0f4", robe="#d0e0ec", mantle="#a8c8dc", cape="#88b0d0", belt="#4a6a90", boot="#4a6a90", leaf="#c8f0e8", circlet="#a8c0f0", trim="#7da0c8", gem="#d0f8ff", **common),
              dict(hood="#1a2a46", robe="#1e2a4e", mantle="#1a3a3a", cape="#141438", belt="#101028", boot="#101028", leaf="#e6c040", circlet="#f0c840", trim="#e6b836", gem="#ffe060", **common))
    return C, P

def healer():
    """慈光の癒し手（軽い）：白いずきんと長い衣、前かけの十字。肩の小さな羽 → 光の輪 → 大きな白いつばさ"""
    C = []
    C += [K("armorHead", [-4, 25, -4], [8, 7, 8], "hood", 0.7, tags=("face_hole", "open_bottom", "top_trim")),
          K("armorHead", [-4.4, 22.0, 2.4], [8.8, 8.0, 1.6], "hood", 0.3, rot=[8, 0, 0], piv=[0, 30, 3]),
          K("armorHead", [-3.4, 30.4, -4.9], [6.8, 0.8, 0.4], "trim2", t=2),
          K("armorHead", [-0.6, 30.0, -5.1], [1.2, 2.6, 0.4], "gem", t=3),
          K("armorHead", [-1.3, 30.7, -5.1], [2.6, 1.2, 0.4], "gem", t=3)]
    for i in range(14):
        a = math.radians(i * 360 / 14)
        x, y = 4.2 * math.cos(a), 0.8 + 4.2 * math.sin(a)
        C.append(K("armorHead", [x - 0.6, 34.6, y - 0.6], [1.2, 0.5, 1.2], "halo", t=5))
    C += [K("armorBody", [-4, 12, -2], [8, 12, 4], "robe", 0.5),
          K("armorBody", [-4.6, 20.4, -2.9], [9.2, 3.6, 5.8], "mantle", 0.1, tags=("hem",)),
          K("armorBody", [-4, 11.6, -2], [8, 1.8, 4], "belt", 0.8),
          K("armorBody", [-2.4, 2.0, -3.0], [4.8, 18.0, 0.4], "tabard", 0.2, tags=("hem", "border")),
          K("armorBody", [-0.6, 13.0, -3.4], [1.2, 5.6, 0.3], "trim2", t=2),
          K("armorBody", [-1.9, 16.0, -3.4], [3.8, 1.2, 0.3], "trim2", t=2),
          K("armorBody", [-4.4, 1.0, 2.8], [8.8, 20, 0.5], "cape", 0, rot=[5, 0, 0], piv=[0, 21, 2.8], tags=("hem",), t=4)]
    C += both(K("armorBody", [-4.6, 15.0, 2.8], [1.0, 7.0, 4.0], "wing", 0, rot=[0, 30, -14], piv=[-3, 21, 3], t=3, to=5),
              K("armorBody", [-9.6, 12.0, 3.6], [8.0, 10.0, 0.6], "wing", 0, rot=[0, -28, -12], piv=[-1.6, 21, 3.6], t=6),
              K("armorBody", [-12.6, 16.0, 4.2], [4.0, 9.0, 0.5], "wing", 0, rot=[0, -28, -24], piv=[-8, 23, 4.2], t=6),
              K("armorBody", [-8.4, 7.8, 3.4], [5.0, 4.4, 0.5], "wing", 0, rot=[0, -28, 8], piv=[-1.6, 12, 3.4], t=6))
    C += both(K("armorRightArm", [-8, 15, -2], [4, 9, 4], "robe", 0.45),
              K("armorRightArm", [-9, 10.8, -3], [6, 4.6, 6], "robe", 0.1, tags=("hem",)),
              K("armorRightArm", [-9.0, 21.6, -3.0], [5.2, 1.0, 6.0], "trim2", 0.1, rot=[0, 0, 12], piv=[-6, 22, 0], t=4),
              K("armorRightArm", [-8.4, 14.0, -2.6], [4.8, 0.9, 5.2], "trim2", 0.15, t=5))
    C += both(K("armorRightLeg", [-3.9, 3, -2], [4, 9, 4], "robe", 0.7),
              K("armorRightLeg", [-4.9, 0.3, -2.9], [5.8, 5.2, 5.8], "robe", 0.25, tags=("hem",)))
    C += both(K("armorRightBoot", [-3.9, 0, -2], [4, 2.4, 4], "boot", 0.45),
              K("armorRightBoot", [-4.4, 1.6, -0.6], [0.4, 2.4, 1.8], "wing", rot=[0, 0, 20], piv=[-4.2, 1.6, 0], t=5))
    P = tiers(dict(hood="#d8d0c0", robe="#e0d8c8", mantle="#c8bca8", cape="#b8ac98", tabard="#a89880", belt="#7a6a50", boot="#7a6a50", wing="#f0ece4", halo="#f0e0a0", trim="#b8a880", trim2="#c0a870", gem="#d8c890", gem_from=9),
              dict(hood="#f4f4f6", robe="#f0f0f4", mantle="#d8e0f0", cape="#c8d4ec", tabard="#4a6ac0", belt="#5a5a70", boot="#5a5a70", wing="#ffffff", halo="#fff0a0", trim="#c8d0e8", trim2="#e8d070", gem="#fff0a0", gem_from=9),
              dict(hood="#ffffff", robe="#f4fafa", mantle="#d0f0ec", cape="#b0e8e0", tabard="#2a8a8a", belt="#3a5a5a", boot="#3a5a5a", wing="#ffffff", halo="#fff0a0", trim="#e0fff8", trim2="#f0d870", gem="#a0fff0"),
              dict(hood="#2e2628", robe="#3a3034", mantle="#5a1e1a", cape="#6a1a14", tabard="#7a1616", belt="#1a1214", boot="#1a1214", wing="#e8d8c8", halo="#ffb040", trim="#d0a040", trim2="#e0b040", gem="#ff8040"),
              dict(hood="#ffffff", robe="#f4f6fe", mantle="#dce6fc", cape="#b8ccf4", tabard="#7d94d0", belt="#4a5a90", boot="#4a5a90", wing="#ffffff", halo="#e8f0ff", trim="#9ab0e8", trim2="#a8c0f0", gem="#cfe0ff"),
              dict(hood="#fffcf0", robe="#fffaf0", mantle="#f4e8c8", cape="#f0e0b0", tabard="#1e1e4e", belt="#e6b836", boot="#e6b836", wing="#fffef8", halo="#ffe060", trim="#e6b836", trim2="#f0c840", gem="#ffe060"))
    return C, P

def assassin():
    """影走りの暗殺者（軽い）：フードと口元の覆面、長いえりまき、体にそった服と短剣の帯。肩の板 → ぼろぼろのマント → 赤い影のつばさ"""
    C = []
    C += [K("armorHead", [-4, 25, -4], [8, 7, 8], "hood", 0.7, tags=("face_hole", "open_bottom")),
          K("armorHead", [-4, 24.2, -4], [8, 3.4, 8], "mask", 0.45, tags=("open_top", "open_bottom")),
          K("armorHead", [-3, 31.6, -1.5], [6, 1.6, 6], "hood", 0.2, rot=[18, 0, 0], piv=[0, 31.6, 0]),
          K("armorHead", [-0.5, 25.4, -4.85], [1.0, 1.0, 0.3], "gem", t=3)]
    C += both(K("armorHead", [-4.2, 31.6, -2.4], [1.2, 3.2, 1.2], "horn", rot=[-10, 0, 22], piv=[-3.6, 31.6, -1.8], t=6))
    C += [K("armorBody", [-4, 12, -2], [8, 12, 4], "cloth", 0.45),
          K("armorBody", [-4.6, 21.6, -2.8], [9.2, 2.8, 5.6], "scarf", 0.2),
          K("armorBody", [-3.4, 8.0, 2.9], [2.4, 14.0, 0.4], "scarf", 0, rot=[16, 0, 6], piv=[-2.2, 22, 2.9], tags=("hem",)),
          K("armorBody", [0.6, 5.0, 2.9], [2.4, 17.0, 0.4], "scarf", 0, rot=[20, 0, -8], piv=[1.8, 22, 2.9], tags=("hem",), t=4),
          K("armorBody", [-4, 11.6, -2], [8, 1.6, 4], "belt", 0.8),
          K("armorBody", [-4.4, 17.0, -3.0], [8.8, 1.0, 0.4], "belt", 0, rot=[0, 0, 32], piv=[0, 17.5, -2.9], t=2),
          K("armorBody", [-4.4, 17.0, -3.0], [8.8, 1.0, 0.4], "belt", 0, rot=[0, 0, -32], piv=[0, 17.5, -2.9], t=3),
          K("armorBody", [1.0, 16.4, -3.6], [0.8, 3.6, 0.6], "blade", 0, rot=[0, 0, -32], piv=[1.4, 18, -3.3], t=2),
          K("armorBody", [-4.8, 2.0, 2.6], [9.6, 20, 0.5], "cape", 0, rot=[6, 0, 0], piv=[0, 22, 2.6], tags=("hem", "torn"), t=5)]
    C += both(K("armorBody", [-10.0, 10.0, 3.6], [7.0, 11.0, 0.5], "shadow", 0, rot=[0, -30, -16], piv=[-2, 21, 3.6], tags=("torn",), t=6),
              K("armorBody", [-12.0, 15.0, 4.2], [3.6, 9.0, 0.5], "shadow", 0, rot=[0, -30, -30], piv=[-8, 22, 4.2], tags=("torn",), t=6))
    C += [K("armorRightArm", [-9.0, 19.8, -3.0], [5.2, 4.2, 6.0], "metal", 0.1, rot=[0, 0, 10], piv=[-6, 22, 0], tags=("border",), t=3),
          K("armorRightArm", [-9.8, 17.6, -2.8], [1.2, 4.0, 5.6], "metal", 0, rot=[0, 0, 14], piv=[-9, 22, 0], tags=("border",), t=5)]
    C += both(K("armorRightArm", [-8, 13, -2], [4, 11, 4], "cloth", 0.4),
              K("armorRightArm", [-8, 12, -2], [4, 5.4, 4], "wrap", 0.6),
              K("armorRightArm", [-8.7, 13.0, -0.8], [0.6, 4.2, 1.6], "blade", 0, rot=[0, 0, -10], piv=[-8.4, 17, 0], t=4))
    C += both(K("armorRightLeg", [-3.9, 1, -2], [4, 11, 4], "cloth", 0.5),
              K("armorRightLeg", [-4.2, 7.6, -2.4], [4.4, 1.0, 4.8], "belt", 0.3, t=2),
              K("armorRightLeg", [-4.5, 4.4, -0.8], [0.6, 3.6, 1.6], "blade", rot=[0, 0, 6], piv=[-4.2, 8, 0], t=3))
    C += both(K("armorRightBoot", [-3.9, 0, -2], [4, 5, 4], "wrap", 0.5),
              K("armorRightBoot", [-3.9, 0, -2.95], [4, 1.6, 1.0], "metal", 0.3, t=2))
    P = tiers(dict(hood="#4a4448", mask="#3a3438", cloth="#3e3a3e", scarf="#6a3a34", belt="#2a2220", wrap="#5a5450", blade="#9a9aa0", metal="#6a6670", cape="#3a3438", shadow="#2a2028", horn="#4a4048", trim="#8a8080", gem="#a08080", gem_from=9),
              dict(hood="#2e2e3a", mask="#22222c", cloth="#2a2a36", scarf="#7a2020", belt="#1e1a1a", wrap="#3a3a46", blade="#d8dce6", metal="#a8acb8", cape="#22222c", shadow="#1a1a24", horn="#3a3a4a", trim="#c0c4d0", gem="#e04040", gem_from=9),
              dict(hood="#1e2a36", mask="#16202a", cloth="#1a2430", scarf="#a01a30", belt="#121820", wrap="#2a3644", blade="#a0fff8", metal="#4ad0c8", cape="#16202a", shadow="#101820", horn="#2a3644", trim="#a0f0ec", gem="#ff3050"),
              dict(hood="#1e1a1c", mask="#141012", cloth="#1a1618", scarf="#c01818", belt="#0e0a0c", wrap="#2a2224", blade="#ff5030", metal="#3a3236", cape="#1a1214", shadow="#3a0a0a", horn="#5a1a1a", trim="#d03020", gem="#ff3020"),
              dict(hood="#c8d0e4", mask="#9aa8cc", cloth="#b4c0de", scarf="#4a5a90", belt="#3a4a80", wrap="#dce4f8", blade="#ffffff", metal="#e2e8f8", cape="#8a9ac8", shadow="#5a6aa0", horn="#e8eeff", trim="#7d94d0", gem="#cfe0ff"),
              dict(hood="#12122e", mask="#0c0c22", cloth="#141432", scarf="#c01830", belt="#0a0a1e", wrap="#1c1c40", blade="#f0c840", metal="#26265a", cape="#0e0e26", shadow="#4a0a20", horn="#e6b836", trim="#e6b836", gem="#ff3050"))
    return C, P

def gambler():
    """賽振りの博徒（軽い）：シルクハットと燕尾服。帽子のカード → 肩章・金の鎖 → 肩に浮かぶさいころ → カードの輪とマント"""
    C = []
    C += [K("armorHead", [-3.5, 31.8, -3.5], [7, 7.5, 7], "hat", 0.3, tags=("open_bottom",)),
          K("armorHead", [-5.6, 31.5, -5.6], [11.2, 0.8, 11.2], "hat", 0),
          K("armorHead", [-3.6, 32.3, -3.6], [7.2, 1.3, 7.2], "band", 0.32, tags=("open_top", "open_bottom")),
          K("armorHead", [1.8, 32.8, -4.25], [1.8, 2.8, 0.3], "card", rot=[0, 0, 18], piv=[2.7, 32.8, -4.1], t=2),
          K("armorHead", [-3.4, 33.0, -4.25], [1.6, 2.4, 0.3], "card", rot=[0, 0, -20], piv=[-2.6, 33.0, -4.1], t=4)]
    for i in range(8):
        a = math.radians(-90 + (i - 3.5) * 24)
        x, y = 6.8 * math.cos(a), 31 + 6.8 * math.sin(a) + 3
        C.append(K("armorHead", [x - 0.9, y - 1.3, 4.6], [1.8, 2.6, 0.3], "card", rot=[0, 0, (i - 3.5) * -24], piv=[x, y, 4.75], t=6))
    C += [K("armorBody", [-4, 12, -2], [8, 12, 4], "vest", 0.4, tags=("front",)),
          K("armorBody", [-4, 11.5, -2], [8, 12.5, 4], "coat", 0.55, tags=("lapel",)),
          K("armorBody", [-1.6, 22.0, -2.75], [3.2, 1.4, 0.5], "tie", 0),
          K("armorBody", [-4.0, 4.0, 2.2], [8, 8.5, 0.5], "coat", 0, rot=[8, 0, 0], piv=[0, 12.5, 2.2], tags=("hem",)),
          K("armorBody", [-3.4, 16.2, -2.85], [4.4, 0.45, 0.3], "metal", 0, rot=[0, 0, -14], piv=[-1.2, 16.4, -2.8], t=4),
          K("armorBody", [0.6, 14.6, -2.95], [1.4, 1.4, 0.3], "metal", 0, t=4),
          K("armorBody", [-4.8, 0.5, 2.9], [9.6, 21.5, 0.5], "cape", 0, rot=[6, 0, 0], piv=[0, 22, 2.9], tags=("hem",), t=6)]
    C += both(K("armorBody", [-4.2, 0.0, 2.6], [3.6, 9.0, 0.4], "coat", 0, rot=[12, 0, 6], piv=[-2.4, 9, 2.6], tags=("hem",), t=4))
    C += both(K("armorRightArm", [-8, 12, -2], [4, 12, 4], "coat", 0.4),
              K("armorRightArm", [-8, 12, -2], [4, 1.6, 4], "card", 0.55),
              K("armorRightArm", [-8.7, 23.0, -2.7], [5.4, 1.0, 5.4], "metal", 0.1, tags=("border",), t=3),
              K("armorRightArm", [-8.9, 21.0, -2.7], [0.4, 2.0, 5.4], "metal", 0, t=3),
              K("armorRightArm", [-9.8, 25.4, -1.0], [2.0, 2.0, 2.0], "dice", 0, rot=[25, 30, 0], piv=[-8.8, 26.4, 0], t=5))
    C += both(K("armorRightLeg", [-3.9, 1, -2], [4, 11, 4], "pants", 0.5),
              K("armorRightLeg", [-4.45, 1, -0.4], [0.3, 11, 0.8], "metal", 0, t=5))
    C += both(K("armorRightBoot", [-3.9, 0, -2.4], [4, 2.5, 4.4], "shoe", 0.4),
              K("armorRightBoot", [-4.1, 1.8, -2.2], [4.2, 1.6, 4.4], "card", 0.3, t=3))
    P = tiers(dict(hat="#5a4a3a", band="#8a3a2a", coat="#6a5440", vest="#c8b898", tie="#8a3a2a", pants="#4a3e32", shoe="#3a2a20", card="#ece4d0", metal="#a89070", cape="#5a4030", dice="#f0f0f0", trim="#a89070", gem="#c0a070", gem_from=9),
              dict(hat="#1a1a1e", band="#c02030", coat="#1e1e24", vest="#e8e8ec", tie="#c02030", pants="#22222a", shoe="#101014", card="#f8f8f8", metal="#c8ccd6", cape="#1e1e24", dice="#f8f8f8", trim="#c8ccd6", gem="#e04040", gem_from=9),
              dict(hat="#3a0e16", band="#e6b836", coat="#5a1420", vest="#f0e0c0", tie="#e6b836", pants="#2a0a10", shoe="#1a0808", card="#fff8e8", metal="#f0c040", cape="#5a1420", dice="#fff8e8", trim="#f0c040", gem="#ff3050"),
              dict(hat="#221c20", band="#e05a2a", coat="#2e2428", vest="#4a3a3e", tie="#e05a2a", pants="#1a1418", shoe="#120e10", card="#f0e0d0", metal="#e07030", cape="#3a1414", dice="#f0e0d0", trim="#e06030", gem="#ff6a30"),
              dict(hat="#e8eefc", band="#5a6aa0", coat="#dce4f8", vest="#ffffff", tie="#5a6aa0", pants="#a8b8e0", shoe="#5a6aa0", card="#ffffff", metal="#a8c0f0", cape="#8aa0d8", dice="#ffffff", trim="#7d94d0", gem="#cfe0ff"),
              dict(hat="#14143a", band="#e6b836", coat="#1a1a4a", vest="#fff4d0", tie="#e6b836", pants="#101030", shoe="#0a0a20", card="#fff8e0", metal="#f0c840", cape="#14143a", dice="#fff8e0", trim="#e6b836", gem="#ffe060"))
    return C, P

STYLES = {"mage": mage, "warrior": warrior, "axeman": axeman, "archer": archer, "lancer": lancer, "shield_knight": shield_knight,
          "beast": beast, "brawler": brawler, "elementalist": elementalist, "healer": healer, "assassin": assassin, "gambler": gambler}

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
