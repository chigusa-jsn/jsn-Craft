"""宝珠（orb_<gem>_<grade>）と宝珠ばさみのテクスチャを作る"""
import math, os
from PIL import Image
OUT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/jsn_craft/textures/item/jewelry")
GEMS = {"garnet": 0x9A1A2A, "peridot": 0x9AD040, "aquamarine": 0x70D8E8, "onyx": 0x2A2A30, "ruby": 0xE02040,
        "sapphire": 0x2050E0, "topaz": 0xF0A020, "crystal": 0xE8E0F8, "moonstone": 0xC8D8F8, "opal": 0xF0E0F0}
def rgb(h): return ((h >> 16) & 255, (h >> 8) & 255, h & 255)
def mix(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))
def orb(color, grade):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); px = im.load()
    r = [4.2, 5.4, 6.6][grade - 1]; cx, cy = 7.5, 8.0 if grade < 3 else 7.5
    base = rgb(color); dark = mix(base, (10, 5, 20), 0.6); light = mix(base, (255, 255, 255), 0.65)
    gold = (232, 190, 80); gold_d = (150, 105, 40)
    for y in range(16):
        for x in range(16):
            dx, dy = x + .5 - cx - .5, y + .5 - cy - .5; d = math.hypot(dx, dy)
            if d <= r:
                lx, ly = dx + r * .35, dy + r * .4
                t = min(1, math.hypot(lx, ly) / (r * 1.4))
                c = mix(light, dark, t)
                if d > r - 1.0: c = mix(c, dark, .5)
                px[x, y] = c + (255,)
            elif d <= r + 1.0 and grade >= 2:
                px[x, y] = (gold if dy < 0 else gold_d) + (255,)
    hx, hy = int(cx - r * .35), int(cy - r * .4)
    px[hx, hy] = (255, 255, 255, 255)
    if grade >= 2: px[hx + 1, hy] = (255, 255, 255, 200)
    if grade == 3:
        for (x, y) in ((1, 1), (14, 2), (13, 13), (2, 12)):
            px[x, y] = (255, 250, 200, 255)
        for (x, y) in ((1, 2), (14, 3)):
            px[x, y] = (255, 230, 150, 140)
    else:
        # 小さな台座
        for x in range(6, 10): px[x, 14] = gold_d + (255,)
        for x in range(5, 11): px[x, 15] = gold + (255,) if grade == 2 else (120, 120, 130, 255)
    return im
def tweezers():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); px = im.load()
    for i in range(11):
        px[3 + i, 12 - i] = (200, 205, 215, 255)
        if i < 9: px[5 + i, 13 - i] = (150, 155, 170, 255)
    for (x, y) in ((2, 13), (3, 14), (2, 14)): px[x, y] = (110, 80, 50, 255)
    for (x, y) in ((13, 2), (14, 1), (14, 2)): px[x, y] = (200, 100, 240, 255)
    return im
os.makedirs(OUT, exist_ok=True)
for g, c in GEMS.items():
    for i in (1, 2, 3): orb(c, i).save(os.path.join(OUT, f"orb_{g}_{i}.png"))
tweezers().save(os.path.join(OUT, "orb_tweezers.png"))
print("ok")
