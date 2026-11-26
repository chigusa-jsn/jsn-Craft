"""鍛冶の追加アイテム（砥石・修理キット・当て板）と分解機のテクスチャを作る。"""
from PIL import Image, ImageDraw
import os, random, math
ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'jsn_craft', 'textures')

def img():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0))

def rect(im, x0, y0, x1, y1, c):
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            im.putpixel((x, y), c)

def outline(im, c=(30, 26, 30, 255)):
    src = im.copy()
    for x in range(16):
        for y in range(16):
            if src.getpixel((x, y))[3]: continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and src.getpixel((nx, ny))[3] and src.getpixel((nx, ny)) != c:
                    im.putpixel((x, y), c); break

def whetstone(base, light, dark, speck):
    im = img(); r = random.Random(3)
    # 斜めに置いた細長い石
    for i in range(11):
        x, y = 2 + i, 11 - i // 2 - (i % 2 == 0 and 0 or 0)
        for t in range(4):
            yy = y - 1 + t
            if 0 <= yy < 16:
                im.putpixel((x, yy), light if t == 0 else dark if t == 3 else base)
    for _ in range(6):
        x = r.randint(3, 12); 
        for y in range(16):
            if im.getpixel((x, y)) == base: im.putpixel((x, y), speck); break
    outline(im)
    return im

def kit(body, lid, latch):
    im = img()
    rect(im, 2, 6, 13, 13, body)
    rect(im, 2, 6, 13, 7, lid)
    rect(im, 2, 13, 13, 13, tuple(max(0, v - 50) if i < 3 else 255 for i, v in enumerate(body)))
    rect(im, 6, 3, 9, 3, (90, 90, 96, 255)); rect(im, 5, 4, 5, 5, (90, 90, 96, 255)); rect(im, 10, 4, 10, 5, (90, 90, 96, 255))
    rect(im, 7, 8, 8, 9, latch)
    # 白い十字
    rect(im, 7, 10, 8, 12, (240, 240, 240, 255)); rect(im, 6, 11, 9, 11, (240, 240, 240, 255))
    outline(im)
    return im

def plate():
    im = img()
    for y in range(3, 14):
        w = 5 if y < 11 else 5 - (y - 10)
        rect(im, 8 - w, y, 7 + w, y, (196, 110, 72, 255))
    rect(im, 3, 3, 12, 3, (232, 150, 110, 255))
    for x, y in ((4, 5), (11, 5), (4, 9), (11, 9)):
        im.putpixel((x, y), (120, 70, 50, 255))
    rect(im, 7, 4, 8, 12, (214, 128, 90, 255))
    outline(im)
    return im

def block_side(top=False):
    im = Image.new('RGBA', (16, 16), (88, 92, 98, 255)); r = random.Random(7 if top else 5)
    for x in range(16):
        for y in range(16):
            v = r.randint(-8, 8); c = im.getpixel((x, y))
            im.putpixel((x, y), (c[0] + v, c[1] + v, c[2] + v, 255))
    rect(im, 0, 0, 15, 0, (60, 62, 66, 255)); rect(im, 0, 15, 15, 15, (50, 52, 56, 255))
    rect(im, 0, 0, 0, 15, (60, 62, 66, 255)); rect(im, 15, 0, 15, 15, (50, 52, 56, 255))
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        im.putpixel((x, y), (170, 170, 175, 255))
    if top:
        rect(im, 3, 3, 12, 12, (40, 40, 44, 255))
        for i in range(4):
            rect(im, 4, 4 + i * 2, 11, 4 + i * 2, (120, 120, 126, 255))
    else:
        rect(im, 1, 7, 14, 8, (196, 150, 40, 255))
        for x in range(1, 15, 3): rect(im, x, 7, x, 8, (40, 40, 40, 255))
    return im

def block_front(on):
    im = block_side()
    rect(im, 3, 2, 12, 9, (30, 30, 34, 255))
    for i in range(5):  # 歯車の歯
        x = 4 + i * 2
        rect(im, x, 3 if i % 2 else 4, x, 8 if i % 2 else 7, (150, 150, 156, 255) if not on else (230, 160, 60, 255))
    rect(im, 4, 11, 11, 13, (60, 62, 66, 255))
    im.putpixel((5, 12), (80, 220, 80, 255) if on else (90, 40, 40, 255))
    im.putpixel((10, 12), (230, 60, 60, 255) if not on else (60, 70, 60, 255))
    return im

os.makedirs(os.path.join(ROOT, 'item', 'smith'), exist_ok=True)
whetstone((150, 146, 140, 255), (200, 196, 190, 255), (100, 96, 92, 255), (120, 116, 110, 255)).save(os.path.join(ROOT, 'item', 'smith', 'whetstone.png'))
whetstone((150, 110, 200, 255), (210, 180, 240, 255), (96, 64, 140, 255), (230, 210, 255, 255)).save(os.path.join(ROOT, 'item', 'smith', 'whetstone_adv.png'))
kit((190, 50, 46, 255), (220, 80, 70, 255), (230, 200, 60, 255)).save(os.path.join(ROOT, 'item', 'smith', 'repair_kit.png'))
kit((50, 150, 190, 255), (90, 200, 230, 255), (230, 200, 60, 255)).save(os.path.join(ROOT, 'item', 'smith', 'repair_kit_adv.png'))
plate().save(os.path.join(ROOT, 'item', 'smith', 'armor_plate.png'))
os.makedirs(os.path.join(ROOT, 'block'), exist_ok=True)
block_side().save(os.path.join(ROOT, 'block', 'recycler_side.png'))
block_side(True).save(os.path.join(ROOT, 'block', 'recycler_top.png'))
block_front(False).save(os.path.join(ROOT, 'block', 'recycler_front.png'))
block_front(True).save(os.path.join(ROOT, 'block', 'recycler_front_on.png'))

# ---------------- 特別な矢・投げ物・三叉槍の穂先 ----------------
def hexc(h, a=255):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255, a)

def shade(c, d):
    return tuple(max(0, min(255, v + d)) if i < 3 else v for i, v in enumerate(c))

def arrow_sprite(head, fletch=(235, 235, 235, 255), thick=False, extra=None):
    im = img()
    wood = (137, 103, 64, 255); wood_d = (98, 72, 44, 255)
    for i in range(3, 12):   # 矢柄：左下 → 右上
        x, y = i, 15 - i
        im.putpixel((x, y), wood)
        if thick:
            im.putpixel((x, y - 1) if y - 1 >= 0 else (x, y), wood_d)
    # 矢じり
    hx, hy = 12, 3
    for (dx, dy) in ((0, 0), (1, -1), (2, -2), (-1, -1), (0, -1), (1, -2), (0, -2), (1, 0), (2, -1), (1, 1) if thick else (0, 0)):
        x, y = hx + dx, hy + dy
        if 0 <= x < 16 and 0 <= y < 16: im.putpixel((x, y), head)
    im.putpixel((13, 1), shade(head, 50)); im.putpixel((14, 1), shade(head, 70))
    # 羽根
    for (x, y) in ((1, 12), (2, 13), (3, 14), (1, 13), (2, 14), (2, 11), (3, 12), (4, 13)):
        im.putpixel((x, y), fletch)
    im.putpixel((1, 14), shade(fletch, -40))
    if extra: extra(im)
    outline(im)
    return im

def rope_extra(im):
    for (x, y) in ((5, 7), (6, 6), (7, 7), (6, 8), (5, 8), (4, 9)):
        im.putpixel((x, y), (220, 200, 150, 255))

def blast_extra(im):
    for (x, y) in ((10, 6), (11, 6), (10, 7), (11, 7)):
        im.putpixel((x, y), (200, 40, 30, 255))

def knife():
    im = img()
    for i in range(6):   # 刃
        im.putpixel((8 + i, 7 - i), (220, 224, 232, 255)); im.putpixel((8 + i, 8 - i), (170, 175, 185, 255))
    im.putpixel((14, 1), (240, 240, 250, 255))
    rect(im, 6, 8, 7, 9, (80, 80, 90, 255))   # つば
    for i in range(4):
        im.putpixel((5 - i, 10 + i), (70, 50, 40, 255)); im.putpixel((6 - i, 10 + i), (110, 80, 60, 255))
    outline(im)
    return im

def shuriken():
    im = img(); m = (170, 175, 190, 255); l = (220, 225, 235, 255)
    rect(im, 7, 3, 8, 12, m); rect(im, 3, 7, 12, 8, m)
    for (x, y) in ((7, 3), (12, 7), (8, 12), (3, 8)): im.putpixel((x, y), l)
    for (x, y) in ((6, 5), (9, 5), (5, 9), (10, 10), (10, 6), (5, 6), (6, 10), (9, 10)): im.putpixel((x, y), m)
    rect(im, 7, 7, 8, 8, (40, 40, 48, 255))
    outline(im)
    return im

def tip(col):
    im = img(); c = hexc(col)
    for y in range(2, 13):   # 三つ又の穂先
        for x in (4, 8, 12) if y < 7 else ():
            im.putpixel((x if x != 8 else 8, y), c)
    for x in (4, 8, 12):
        im.putpixel((x, 1), shade(c, 60))
    rect(im, 4, 7, 12, 8, shade(c, -30))
    rect(im, 7, 9, 9, 14, (90, 140, 130, 255))
    outline(im)
    return im

KINDS = [('steel_arrow', 0xC8CCD4, None, False), ('pierce_arrow', 0xF0E8D0, None, False), ('blast_arrow', 0xFF7030, blast_extra, False),
         ('fire_arrow', 0xFF9020, None, False), ('ice_arrow', 0x90E0FF, None, False), ('thunder_arrow', 0xFFF060, None, False),
         ('poison_arrow', 0x60C040, None, False), ('weakness_arrow', 0x8060A0, None, False), ('rope_arrow', 0xC8A060, rope_extra, False),
         ('light_arrow', 0xFFFFC0, None, False), ('retrieve_arrow', 0x50D0B0, None, False), ('homing_arrow', 0xD070FF, None, False),
         ('heavy_bolt', 0x707880, None, True)]
os.makedirs(os.path.join(ROOT, 'item', 'arrow'), exist_ok=True)
for name, col, extra, thick in KINDS:
    fl = shade(hexc(col), 30) if name not in ('steel_arrow', 'heavy_bolt', 'pierce_arrow') else (235, 235, 235, 255)
    arrow_sprite(hexc(col), fl, thick, extra).save(os.path.join(ROOT, 'item', 'arrow', name + '.png'))
knife().save(os.path.join(ROOT, 'item', 'arrow', 'throwing_knife.png'))
shuriken().save(os.path.join(ROOT, 'item', 'arrow', 'shuriken.png'))
tip(0xFF5020).save(os.path.join(ROOT, 'item', 'arrow', 'trident_tip_fire.png'))
tip(0x80E0FF).save(os.path.join(ROOT, 'item', 'arrow', 'trident_tip_frost.png'))
tip(0xFFE040).save(os.path.join(ROOT, 'item', 'arrow', 'trident_tip_storm.png'))

# ---------------- 上位の品（鍛冶師 Lv35〜・名工） ----------------
S = os.path.join(ROOT, 'item', 'smith')
whetstone((120, 116, 110, 255), (150, 146, 140, 255), (80, 76, 72, 255), (60, 58, 56, 255)).save(os.path.join(S, 'whetstone_rough.png'))
whetstone((236, 236, 240, 255), (255, 255, 255, 255), (180, 184, 196, 255), (210, 230, 255, 255)).save(os.path.join(S, 'finishing_whetstone.png'))
whetstone((190, 90, 230, 255), (240, 190, 255, 255), (110, 40, 150, 255), (255, 230, 120, 255)).save(os.path.join(S, 'divine_whetstone.png'))
kit((150, 110, 70, 255), (180, 140, 90, 255), (200, 200, 200, 255)).save(os.path.join(S, 'repair_kit_simple.png'))
kit((60, 30, 90, 255), (120, 70, 170, 255), (255, 210, 80, 255)).save(os.path.join(S, 'repair_kit_divine.png'))
def iron_plate():
    im = plate()
    for x in range(16):
        for y in range(16):
            r, g, b, a = im.getpixel((x, y))
            if a and (r, g, b) != (30, 26, 30):
                v = (r + g + b) // 3
                im.putpixel((x, y), (min(255, v + 30), min(255, v + 34), min(255, v + 42), 255))
    return im
iron_plate().save(os.path.join(S, 'iron_plate.png'))
for name, col, extra, thick in [('hardened_arrow', 0xE8ECF4, None, True), ('inferno_arrow', 0xFF4010, None, True), ('glacier_arrow', 0x60C8FF, None, True),
                                ('storm_arrow', 0xFFFFA0, None, True), ('star_arrow', 0xB0A0FF, None, True), ('dragon_arrow', 0x60E080, None, True)]:
    arrow_sprite(hexc(col), shade(hexc(col), 40), thick, extra).save(os.path.join(ROOT, 'item', 'arrow', name + '.png'))
tip(0x7030A0).save(os.path.join(ROOT, 'item', 'arrow', 'trident_tip_abyss.png'))

# ---------------- 大きい炉・コークス炉・コークス ----------------
def furnace_tex(base, mouth_on, brick=True, seed=3):
    r = random.Random(seed)
    def side():
        im = Image.new('RGBA', (16, 16), base + (255,))
        for y in range(16):
            for x in range(16):
                row = y // 4
                off = 0 if row % 2 == 0 else 4
                edge = (y % 4 == 0) or ((x + off) % 8 == 0)
                v = r.randint(-10, 10) - (35 if edge else 0)
                im.putpixel((x, y), tuple(max(0, min(255, c + v)) for c in base) + (255,))
        return im
    s = side()
    top = side()
    rect(top, 4, 4, 11, 11, (30, 24, 22, 255)); rect(top, 5, 5, 10, 10, (60, 40, 30, 255))
    def front(on):
        im = side()
        rect(im, 3, 7, 12, 14, (20, 16, 16, 255))
        if on:
            for x in range(4, 12):
                for y in range(9, 14):
                    t = (y - 9) / 4
                    im.putpixel((x, y), (255, int(200 - 120 * t + r.randint(-20, 20)), int(60 - 40 * t), 255))
        rect(im, 2, 6, 13, 6, tuple(max(0, c - 40) for c in base) + (255,))
        return im
    return s, top, front(False), front(True)

for name, base in (('big_furnace', (150, 70, 55)), ('coke_oven', (120, 120, 126))):
    s, t, f, fo = furnace_tex(base, True)
    s.save(os.path.join(ROOT, 'block', f'{name}_side.png')); t.save(os.path.join(ROOT, 'block', f'{name}_top.png'))
    f.save(os.path.join(ROOT, 'block', f'{name}_front.png')); fo.save(os.path.join(ROOT, 'block', f'{name}_front_on.png'))

def coke_item():
    im = img(); r = random.Random(9)
    pts = [(3, 8), (6, 4), (11, 4), (13, 8), (11, 12), (5, 12)]
    ImageDraw.Draw(im).polygon(pts, fill=(52, 52, 58, 255))
    for _ in range(14):
        x, y = r.randint(4, 11), r.randint(5, 11)
        if im.getpixel((x, y))[3]: im.putpixel((x, y), (90, 90, 100, 255) if r.random() < 0.6 else (30, 30, 34, 255))
    im.putpixel((7, 6), (150, 150, 165, 255)); im.putpixel((10, 9), (150, 150, 165, 255))
    outline(im)
    return im
coke_item().save(os.path.join(ROOT, 'item', 'coke.png'))
cb = Image.new('RGBA', (16, 16)); r9 = random.Random(4)
for y in range(16):
    for x in range(16):
        v = 46 + r9.randint(-12, 14)
        cb.putpixel((x, y), (v, v, v + 6, 255))
for _ in range(20):
    cb.putpixel((r9.randint(0, 15), r9.randint(0, 15)), (120, 120, 132, 255))
cb.save(os.path.join(ROOT, 'block', 'coke_block.png'))

# ---------------- 製材台・落ち葉・腐葉土・豊穣の土 ----------------
def wood_noise(base, seed):
    r = random.Random(seed); im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            v = r.randint(-12, 12) - (25 if y % 4 == 0 else 0)
            im.putpixel((x, y), tuple(max(0, min(255, c + v)) for c in base) + (255,))
    return im
saw_side = wood_noise((160, 120, 75), 11)
rect(saw_side, 0, 0, 15, 2, (110, 80, 50, 255))
for x in (1, 14): rect(saw_side, x, 3, x, 15, (95, 68, 42, 255))
saw_top = wood_noise((175, 135, 85), 12)
d_ = ImageDraw.Draw(saw_top)
d_.ellipse([3, 3, 12, 12], fill=(185, 190, 200, 255), outline=(110, 115, 125, 255))
for a in range(0, 360, 30):
    import math as _m
    x = 7.5 + _m.cos(_m.radians(a)) * 5.5; y = 7.5 + _m.sin(_m.radians(a)) * 5.5
    saw_top.putpixel((int(round(x)), int(round(y))), (230, 232, 240, 255))
rect(saw_top, 7, 7, 8, 8, (60, 60, 66, 255))
saw_front = saw_side.copy(); rect(saw_front, 4, 5, 11, 11, (70, 50, 32, 255)); rect(saw_front, 5, 6, 10, 10, (200, 205, 215, 255))
saw_front_on = saw_front.copy(); rect(saw_front_on, 6, 7, 9, 9, (255, 230, 160, 255))
for nm, im_ in (('sawmill_side', saw_side), ('sawmill_top', saw_top), ('sawmill_front', saw_front), ('sawmill_front_on', saw_front_on)):
    im_.save(os.path.join(ROOT, 'block', nm + '.png'))

def soil(base, specks, seed):
    r = random.Random(seed); im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            v = r.randint(-14, 14)
            im.putpixel((x, y), tuple(max(0, min(255, c + v)) for c in base) + (255,))
    for col, n in specks:
        for _ in range(n):
            im.putpixel((r.randint(0, 15), r.randint(0, 15)), col)
    return im
soil((78, 56, 38), [((130, 90, 40, 255), 14), ((95, 120, 50, 255), 10)], 21).save(os.path.join(ROOT, 'block', 'leaf_mold.png'))
soil((62, 42, 30), [((240, 200, 80, 255), 8), ((120, 170, 70, 255), 10), ((150, 100, 50, 255), 10)], 22).save(os.path.join(ROOT, 'block', 'rich_soil.png'))
def fallen():
    im = img(); r = random.Random(31)
    cols = [(200, 120, 40), (220, 170, 50), (150, 90, 40), (120, 150, 50)]
    for i in range(4):
        cx, cy = 4 + (i % 2) * 7, 4 + (i // 2) * 7
        c = cols[i]
        ImageDraw.Draw(im).ellipse([cx - 3, cy - 2, cx + 3, cy + 2], fill=c + (255,))
        ImageDraw.Draw(im).line([cx - 3, cy + 2, cx + 3, cy - 2], fill=tuple(int(v * 0.6) for v in c) + (255,))
    outline(im)
    return im
fallen().save(os.path.join(ROOT, 'item', 'fallen_leaves.png'))

# ---------------- 農家：巨大作物・交配台・豊穣の骨粉・黄金の食材 ----------------
GIANT = {'wheat': 0xDCBE5A, 'carrot': 0xF08228, 'potato': 0xC8A05A, 'beetroot': 0x961E32,
         'rice': 0xF0EBD2, 'tomato': 0xDC3228, 'cabbage': 0x82C85A, 'onion': 0xDCB478}
os.makedirs(os.path.join(ROOT, 'block', 'giant'), exist_ok=True)
for name, col in GIANT.items():
    r = random.Random(hash(name) & 0xFFFF)
    c = hexc(col)
    body = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            v = r.randint(-14, 14) + int(12 * math.cos((x - 8) / 5.0)) - int(y * 0.8)
            body.putpixel((x, y), tuple(max(0, min(255, ch + v)) for ch in c[:3]) + (255,))
    if name in ('wheat', 'rice'):
        for x in range(0, 16, 3):
            for y in range(16):
                if (y + x) % 4 == 0: body.putpixel((x, y), shade(c, -40))
    if name == 'cabbage':
        for i in range(0, 16, 4): ImageDraw.Draw(body).arc([i - 6, 2, i + 6, 14], 200, 340, fill=shade(c, -45))
    body.putpixel((4, 4), shade(c, 60)); body.putpixel((5, 4), shade(c, 50))
    body.save(os.path.join(ROOT, 'block', 'giant', name + '.png'))
    leaf = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); dl = ImageDraw.Draw(leaf)
    lc = (220, 200, 120, 255) if name in ('wheat', 'rice') else (70, 150, 50, 255)
    for i in range(5):
        x = 2 + i * 3
        dl.line([x, 15, x + r.randint(-2, 2), r.randint(1, 5)], fill=lc, width=2)
    leaf.save(os.path.join(ROOT, 'block', 'giant', name + '_top.png'))

def xb_tex():
    side = wood_noise((140, 110, 70), 41); rect(side, 0, 0, 15, 2, (90, 140, 60, 255))
    top = wood_noise((150, 120, 75), 42); rect(top, 2, 2, 13, 13, (100, 75, 45, 255)); rect(top, 3, 3, 12, 12, (90, 150, 60, 255))
    for (x, y) in ((5, 6), (10, 6), (7, 10), (9, 9)): top.putpixel((x, y), (240, 220, 120, 255))
    side.save(os.path.join(ROOT, 'block', 'crossbreed_table_side.png')); top.save(os.path.join(ROOT, 'block', 'crossbreed_table_top.png'))
xb_tex()

def dust(col, sparkle_col):
    im = img(); r = random.Random(col)
    for _ in range(46):
        x, y = int(8 + r.gauss(0, 2.6)), int(9 + r.gauss(0, 2.2))
        if 1 <= x <= 14 and 2 <= y <= 14: im.putpixel((x, y), hexc(col) if r.random() < 0.7 else shade(hexc(col), -40))
    for _ in range(5): im.putpixel((r.randint(4, 12), r.randint(4, 12)), hexc(sparkle_col))
    outline(im, (60, 60, 60, 255))
    return im
dust(0xF0F0D8, 0x80FF80).save(os.path.join(ROOT, 'item', 'fertile_bone_meal.png'))
os.makedirs(os.path.join(ROOT, 'item', 'golden'), exist_ok=True)
def golden_icon(kind):
    im = img(); d = ImageDraw.Draw(im); g = (245, 200, 60, 255); gl = (255, 240, 160, 255); gd = (190, 140, 30, 255)
    if kind == 'rice':
        for (x, y) in ((5, 6), (8, 5), (11, 7), (6, 10), (9, 9), (12, 11), (7, 13), (10, 12)):
            d.ellipse([x - 1, y - 2, x + 1, y + 1], fill=g, outline=gd); im.putpixel((x, y - 1), gl)
    elif kind == 'wheat':
        d.line([4, 14, 11, 3], fill=gd, width=1)
        for i in range(5): d.ellipse([5 + i, 9 - i * 1.5 - 1, 8 + i, 9 - i * 1.5 + 1], fill=g)
    elif kind == 'potato':
        d.ellipse([3, 5, 13, 13], fill=g, outline=gd); im.putpixel((6, 8), gd); im.putpixel((10, 10), gd); im.putpixel((5, 7), gl)
    else:
        d.ellipse([3, 4, 13, 14], fill=g, outline=gd); d.line([6, 4, 10, 4], fill=(90, 160, 60, 255)); im.putpixel((8, 3), (90, 160, 60, 255)); im.putpixel((5, 7), gl)
    outline(im)
    return im
for k in ('rice', 'wheat', 'potato', 'tomato'):
    golden_icon(k).save(os.path.join(ROOT, 'item', 'golden', f'golden_{k}.png'))

# ---------------- 月銀・星銀の道具、段階のある釣り竿 ----------------
TOOL_PAL = {'moon_silver': ((215, 228, 252), (160, 182, 228), (92, 108, 160), None),
            'star_silver': ((86, 96, 170), (46, 54, 118), (24, 28, 70), (245, 205, 90))}
HANDLE = ((140, 100, 60), (95, 64, 36))

def thick_line(d, pts, col, w):
    d.line(pts, fill=col + (255,), width=w)

def tool_icon(kind, mat):
    im = img(); d = ImageDraw.Draw(im)
    hi, mid, dk, gold = TOOL_PAL[mat]
    # 柄
    end = {'pickaxe': (9, 6), 'axe': (10, 5), 'shovel': (9, 6), 'hoe': (10, 5)}[kind]
    d.line([(2, 13), end], fill=HANDLE[0] + (255,), width=1)
    d.line([(3, 13), (end[0] + 1, end[1])], fill=HANDLE[1] + (255,), width=1)
    if kind == 'pickaxe':
        thick_line(d, [(3, 4), (6, 2), (10, 2), (13, 5), (13, 9), (12, 12)], mid, 2)
        d.line([(4, 3), (6, 2), (10, 2)], fill=hi + (255,)); d.line([(13, 6), (13, 9)], fill=dk + (255,))
    elif kind == 'axe':
        d.polygon([(8, 2), (12, 1), (14, 3), (14, 8), (11, 9), (9, 6)], fill=mid + (255,))
        d.line([(12, 1), (14, 3), (14, 8)], fill=hi + (255,)); d.line([(9, 6), (11, 9)], fill=dk + (255,))
    elif kind == 'shovel':
        d.polygon([(9, 5), (11, 2), (14, 3), (14, 6), (11, 8)], fill=mid + (255,))
        d.line([(11, 2), (14, 3)], fill=hi + (255,)); d.line([(11, 8), (14, 6)], fill=dk + (255,))
    else:
        thick_line(d, [(7, 3), (11, 3), (13, 5)], mid, 2)
        d.line([(7, 3), (11, 3)], fill=hi + (255,))
    if gold:
        for (x, y) in {'pickaxe': ((6, 2), (13, 6), (10, 2)), 'axe': ((12, 1), (14, 6)), 'shovel': ((11, 2), (14, 5)), 'hoe': ((11, 3),)}[kind]:
            im.putpixel((x, y), gold + (255,))
        im.putpixel((4, 11), gold + (255,))
    outline(im)
    return im

os.makedirs(os.path.join(ROOT, 'item', 'tool'), exist_ok=True)
for mat in TOOL_PAL:
    for kind in ('pickaxe', 'axe', 'shovel', 'hoe'):
        tool_icon(kind, mat).save(os.path.join(ROOT, 'item', 'tool', f'{mat}_{kind}.png'))

ROD_PAL = {'iron': ((200, 204, 212), (120, 124, 132)), 'diamond': ((120, 230, 225), (40, 150, 150)),
           'netherite': ((90, 80, 86), (50, 42, 46)), 'moon_silver': ((215, 228, 252), (120, 140, 200)),
           'star_silver': ((70, 80, 160), (245, 205, 90))}
def rod_icon(mat, cast):
    im = img(); d = ImageDraw.Draw(im)
    a, b = ROD_PAL[mat]
    d.line([(2, 14), (13, 3)], fill=a + (255,), width=1)
    d.line([(3, 14), (14, 3)], fill=b + (255,), width=1)
    d.line([(2, 14), (4, 12)], fill=(60, 40, 30, 255), width=2)   # 持ち手
    d.ellipse([4, 9, 7, 12], fill=b + (255,), outline=(40, 40, 46, 255))   # リール
    if not cast:
        d.line([(14, 3), (14, 12)], fill=(230, 230, 230, 255))
        im.putpixel((14, 13), (220, 60, 50, 255)); im.putpixel((14, 12), (240, 240, 240, 255))
    outline(im)
    return im
os.makedirs(os.path.join(ROOT, 'item', 'rod'), exist_ok=True)
for mat in ROD_PAL:
    rod_icon(mat, False).save(os.path.join(ROOT, 'item', 'rod', f'{mat}_fishing_rod.png'))
    rod_icon(mat, True).save(os.path.join(ROOT, 'item', 'rod', f'{mat}_fishing_rod_cast.png'))

# ---------------- 釣り竿の部品・カスタム台 ----------------
GRADE_COL = [(150, 110, 60), (170, 175, 185), (240, 200, 70), (120, 230, 240)]
def part_icon(kind, g):
    im = img(); d = ImageDraw.Draw(im); c = GRADE_COL[g - 1]; dk = tuple(int(v * 0.6) for v in c)
    if kind == 'blank':
        d.line([(3, 13), (13, 3)], fill=c + (255,), width=2); d.line([(3, 13), (5, 11)], fill=(60, 40, 30, 255), width=3)
    elif kind == 'reel':
        d.ellipse([3, 3, 12, 12], fill=c + (255,), outline=dk + (255,)); d.ellipse([6, 6, 9, 9], fill=dk + (255,)); d.line([(9, 7), (14, 4)], fill=dk + (255,), width=2)
    elif kind == 'lure':
        body = [(200, 200, 200), (90, 160, 60), (240, 200, 70), (120, 230, 240)][g - 1]
        d.ellipse([4, 5, 12, 10], fill=body + (255,), outline=tuple(int(v * 0.6) for v in body) + (255,))
        d.polygon([(12, 7), (15, 4), (15, 10)], fill=body + (255,)); im.putpixel((6, 7), (20, 20, 20, 255))
        d.line([(6, 10), (6, 13), (8, 13)], fill=(200, 200, 210, 255))
    elif kind == 'float':
        top = [(220, 60, 50), (230, 70, 60), (200, 230, 240), (140, 255, 180)][g - 1]
        d.ellipse([5, 3, 10, 8], fill=top + (255,)); d.ellipse([5, 7, 10, 12], fill=(240, 240, 240, 255)); d.line([(7, 12), (7, 15)], fill=c + (255,))
    else:
        for i in range(3): d.arc([2 + i, 2 + i, 13 - i, 13 - i], 0, 300, fill=c + (255,))
    outline(im)
    return im
os.makedirs(os.path.join(ROOT, 'item', 'rod_part'), exist_ok=True)
for kind in ('blank', 'reel', 'lure', 'float', 'line'):
    for g in range(1, 5):
        part_icon(kind, g).save(os.path.join(ROOT, 'item', 'rod_part', f'rod_{kind}_{g}.png'))
rt_side = wood_noise((110, 140, 170), 51); rect(rt_side, 0, 0, 15, 2, (70, 90, 120, 255))
rt_top = wood_noise((140, 110, 75), 52); d_ = ImageDraw.Draw(rt_top); d_.line([(2, 13), (13, 2)], fill=(90, 60, 30, 255), width=2); d_.ellipse([4, 8, 7, 11], fill=(180, 185, 195, 255))
rt_side.save(os.path.join(ROOT, 'block', 'rod_table_side.png')); rt_top.save(os.path.join(ROOT, 'block', 'rod_table_top.png'))

# ---------------- 捕獲の杖・捕獲玉 ----------------
def wand_icon():
    im = img(); d = ImageDraw.Draw(im)
    d.line([(2, 14), (10, 6)], fill=(120, 85, 50, 255), width=1); d.line([(3, 14), (11, 6)], fill=(90, 60, 35, 255), width=1)
    d.ellipse([9, 1, 15, 7], fill=(120, 220, 160, 255), outline=(40, 120, 80, 255)); im.putpixel((11, 3), (240, 255, 240, 255))
    d.arc([7, 0, 16, 9], 200, 340, fill=(240, 200, 80, 255))
    outline(im)
    return im
def orb_icon():
    im = img(); d = ImageDraw.Draw(im)
    d.ellipse([2, 2, 13, 13], fill=(150, 230, 190, 255), outline=(40, 120, 80, 255))
    d.chord([2, 2, 13, 13], 0, 180, fill=(240, 240, 240, 255), outline=(40, 120, 80, 255))
    d.line([(2, 8), (13, 8)], fill=(40, 120, 80, 255)); d.ellipse([6, 6, 9, 9], fill=(255, 255, 255, 255), outline=(40, 120, 80, 255))
    im.putpixel((5, 4), (255, 255, 255, 255))
    outline(im)
    return im
wand_icon().save(os.path.join(ROOT, 'item', 'capture_wand.png')); orb_icon().save(os.path.join(ROOT, 'item', 'captured_animal.png'))
