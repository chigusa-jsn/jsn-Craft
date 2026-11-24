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
