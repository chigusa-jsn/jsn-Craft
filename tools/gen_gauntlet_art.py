"""
拳闘士の籠手（6段階）の絵。
  - アイテムの絵（64×64、左右ひと組）  → textures/item/weapon/<id>.png
  - 腕にはめた見た目の絵（128×128、5×5×5の箱を4倍） → textures/entity/gauntlet/<id>.png
  - キュリオスの枠のアイコン（16×16） → textures/slot/gauntlet.png
    python3 tools/gen_gauntlet_art.py アセットのフォルダ [--preview 出力.png]
"""
import math, os, sys
from PIL import Image, ImageDraw
sys.path.insert(0, os.path.dirname(__file__))
from gen_staff_art import Art, hx, NETH, MOON, GOLD, IRON, NIGHT

LEATHER = (hx("#3a2418"), hx("#7a4a28"), hx("#a8723e"))
OBS = (hx("#140c20"), hx("#2e1e48"), hx("#5a3e88"))

def fist(a, cx, cy, metal, knuckle, cuff, flip=False, gem=None, star=False):
    """ひとつの籠手（拳を上に向けた形）"""
    s = -1 if flip else 1
    # 袖口（手首）
    a.poly([(cx - 7, cy + 8), (cx + 7, cy + 8), (cx + 8, cy + 20), (cx - 8, cy + 20)], cuff[0])
    a.poly([(cx - 6.5, cy + 8.5), (cx + 6.5, cy + 8.5), (cx + 7, cy + 19), (cx - 7, cy + 19)], cuff[1])
    a.poly([(cx - 7, cy + 17), (cx + 8, cy + 17), (cx + 8, cy + 20), (cx - 8, cy + 20)], cuff[2])
    # 手の甲
    a.poly([(cx - 9, cy - 6), (cx + 9, cy - 6), (cx + 8, cy + 9), (cx - 8, cy + 9)], metal[0])
    a.poly([(cx - 8, cy - 5.5), (cx + 8, cy - 5.5), (cx + 7, cy + 8), (cx - 7, cy + 8)], metal[1])
    a.poly([(cx - 8, cy - 5.5), (cx + 8, cy - 5.5), (cx + 7.5, cy - 2), (cx - 7.5, cy - 2)], metal[2])
    # 指の関節（ナックル）
    for i in range(4):
        x = cx - 6 + i * 4
        a.ball(x, cy - 7, 2.4, *knuckle)
    # 親指
    a.ball(cx + s * 9, cy + 2, 2.6, *metal)
    if gem: a.ball(cx, cy + 2, 2.6, *gem)
    if star:
        a.star(cx, cy + 2, 4.6, 1.9, 5, GOLD[0]); a.star(cx, cy + 2, 3.8, 1.5, 5, hx("#ffe040"))

def pair(metal, knuckle, cuff, gem=None, star=False, glow=None):
    a = Art()
    fist(a, 20, 26, metal, knuckle, cuff, flip=True, gem=gem, star=star)
    fist(a, 44, 30, metal, knuckle, cuff, flip=False, gem=gem, star=star)
    if star:
        for (x, y, r) in ((6, 8, 2.2), (58, 6, 2.0), (32, 58, 1.6), (60, 52, 1.4)): a.star(x, y, r, r * 0.35, 4, hx("#fff4c0"), rot=0)
    return a.finish(glow=glow)

STAR_METAL = (hx("#141030"), hx("#2a2460"), hx("#4a44a0"))
GAUNTLETS = {
    "leather_gauntlets": dict(metal=LEATHER, knuckle=LEATHER, cuff=(hx("#2a1a10"), hx("#5a3a20"), hx("#8a6038"))),
    "iron_gauntlets": dict(metal=IRON, knuckle=IRON, cuff=LEATHER),
    "obsidian_gauntlets": dict(metal=OBS, knuckle=(hx("#3a2a58"), hx("#7a5ab0"), hx("#c0a0ff")), cuff=IRON, gem=(hx("#3a1a6a"), hx("#9a5ae0"), hx("#e0c0ff"))),
    "netherite_gauntlets": dict(metal=NETH, knuckle=GOLD, cuff=NETH, gem=(hx("#7a1010"), hx("#ff4020"), hx("#ffb080"))),
    "moon_silver_gauntlets": dict(metal=MOON, knuckle=(hx("#7a88b0"), hx("#e8ecff"), hx("#ffffff")), cuff=(hx("#1a3a7a"), hx("#3a7ae0"), hx("#a0d0ff")), gem=(hx("#7a88b0"), hx("#c8d8f8"), hx("#ffffff"))),
    "star_silver_gauntlets": dict(metal=STAR_METAL, knuckle=GOLD, cuff=GOLD, star=True, glow=(32, 30, 26, hx("#fff0a0"))),
}

def arm_texture(spec):
    """5×5×5 の箱の展開図（32×32 の4倍 = 128×128）"""
    K = 4
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    m = spec["metal"]; kn = spec["knuckle"]; cuff = spec["cuff"]
    faces = {"top": (5, 0), "bottom": (10, 0), "left": (0, 5), "front": (5, 5), "right": (10, 5), "back": (15, 5)}
    for name, (u, v) in faces.items():
        x0, y0 = u * K, v * K
        d.rectangle([x0, y0, x0 + 5 * K - 1, y0 + 5 * K - 1], fill=m[1] + (255,))
        d.rectangle([x0, y0, x0 + 5 * K - 1, y0 + 5 * K - 1], outline=m[0] + (255,), width=1)
        if name in ("left", "front", "right", "back"):
            d.rectangle([x0, y0, x0 + 5 * K - 1, y0 + K], fill=cuff[1] + (255,))           # 手首の帯（上）
            d.line([x0, y0 + K + 1, x0 + 5 * K - 1, y0 + K + 1], fill=m[2] + (255,), width=1)
        if name in ("front", "bottom"):
            for i in range(4):   # 関節のびょう
                cx = x0 + 2 + i * 5
                cy = y0 + 5 * K - 4 if name == "front" else y0 + 10
                d.rectangle([cx, cy - 1, cx + 2, cy + 1], fill=kn[1] + (255,))
                d.point((cx, cy - 1), fill=kn[2] + (255,))
        if name == "back" and spec.get("star"):
            d.polygon([(x0 + 10, y0 + 6), (x0 + 12, y0 + 10), (x0 + 16, y0 + 11), (x0 + 12, y0 + 13), (x0 + 10, y0 + 17), (x0 + 8, y0 + 13), (x0 + 4, y0 + 11), (x0 + 8, y0 + 10)],
                      fill=hx("#ffe040") + (255,))
        if name == "back" and spec.get("gem"):
            g = spec["gem"]
            d.ellipse([x0 + 7, y0 + 8, x0 + 13, y0 + 14], fill=g[1] + (255,))
            d.point((x0 + 9, y0 + 10), fill=g[2] + (255,))
    return img

def slot_icon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = (140, 140, 150, 140)
    d.rectangle([4, 3, 11, 9], fill=c)
    for i in range(4): d.rectangle([4 + i * 2, 2, 4 + i * 2, 2], fill=c)
    d.rectangle([5, 10, 10, 13], fill=c)
    d.rectangle([12, 5, 13, 7], fill=c)
    return img

if __name__ == "__main__":
    root = sys.argv[1]
    item_dir = os.path.join(root, "textures/item/weapon"); arm_dir = os.path.join(root, "textures/entity/gauntlet")
    os.makedirs(item_dir, exist_ok=True); os.makedirs(arm_dir, exist_ok=True)
    imgs = []
    for k, spec in GAUNTLETS.items():
        im = pair(**spec); im.save(os.path.join(item_dir, k + ".png")); imgs.append(im)
        arm_texture(spec).save(os.path.join(arm_dir, k + ".png"))
    slot_icon().save(os.path.join(root, "textures/slot/gauntlet.png"))
    if "--preview" in sys.argv:
        prev = sys.argv[sys.argv.index("--preview") + 1]
        sheet = Image.new("RGBA", (len(imgs) * 200 + 10, 210), (40, 44, 52, 255))
        for i, im in enumerate(imgs):
            sheet.alpha_composite(im.resize((192, 192), Image.NEAREST), (i * 200 + 8, 8))
        sheet.save(prev)
    print(list(GAUNTLETS))
