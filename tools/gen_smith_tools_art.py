"""鍛冶の追加アイテム（砥石・修理キット・当て板）と分解機のテクスチャを作る。"""
from PIL import Image
import os, random
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
