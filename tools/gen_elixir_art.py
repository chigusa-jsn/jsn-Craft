"""
薬のアイコン（16×16）を作る。種類ごとに 飲む・投げる・粉塵 の3枚。
    python3 tools/gen_elixir_art.py [--preview 出力.png]
"""
import os, sys
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/assets/jsn_craft/textures/item/elixir")

# Elixirs.java と同じ順番・色
TYPES = [("heal", 0xE03040), ("stamina", 0xF0B020), ("mana", 0x4080F0), ("cure", 0xA060E0), ("great_heal", 0xFF5070),
         ("elixir", 0xFF80C0), ("ancient", 0xFFD040), ("swift", 0x80E0F0), ("might", 0xD03010), ("armor", 0x8A8A9A),
         ("runner", 0xF0D040), ("crit", 0xE8E8F0), ("arcane", 0xB070F0), ("focus", 0x6A4020), ("miner", 0xC08040),
         ("angler", 0x30A080), ("training", 0x40D060), ("luck", 0x50E050), ("recall", 0x20A0A0)]

def rgb(c): return ((c >> 16) & 255, (c >> 8) & 255, c & 255, 255)
def mix(a, b, t): return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3)) + (255,)
WHITE = (255, 255, 255, 255); BLACK = (0, 0, 0, 255)
GLASS = (200, 225, 240, 255); GLASS_D = (120, 150, 175, 255); OUTL = (40, 40, 55, 255)
CORK = (150, 105, 60, 255); CORK_D = (110, 75, 40, 255)

def draw(mask, pal):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(mask):
        for x, ch in enumerate(row):
            if ch in pal and pal[ch]: im.putpixel((x, y), pal[ch])
    return im

DRINK = [
    "................",
    "......kkkk......",
    "......cCCc......",
    ".......gG.......",
    ".......gG.......",
    "......ogGo......",
    ".....oggggo.....",
    "....ogLLLLgo....",
    "...oLhLLLLLLo...",
    "...oLhLLLLLLo...",
    "...oLLLLLLLLo...",
    "...oLLLLLLLDo...",
    "....oLLLLLDo....",
    ".....oDDDDo.....",
    "......oooo......",
    "................",
]
THROW = [
    "........f.......",
    ".......f........",
    "......kkkk......",
    "......cCCc......",
    ".....ogGGgo.....",
    "....ogLLLLgo....",
    "...oLhLLLLLLo...",
    "..oLhLLLLLLLLo..",
    "..obbbbbbbbbbo..",
    "..oLLLLLLLLLLo..",
    "..oLLLLLLLLLDo..",
    "...oLLLLLLLDo...",
    "....oDDDDDDo....",
    ".....oooooo.....",
    "................",
    "................",
]
DUST = [
    "...s.......s....",
    ".....s..s.......",
    "..s....LL...s...",
    "......LhLL......",
    ".....LLLLLL.....",
    "......tttt......",
    ".....pPPPPp.....",
    "....pPPPPPPp....",
    "...pPPPPPPPPp...",
    "...pPPPPLPPPp...",
    "...pPPPLhLPPp...",
    "...pPPPPLPPPp...",
    "....pPPPPPPp....",
    ".....pppppp.....",
    "................",
    "................",
]

def make(color):
    L = rgb(color); h = mix(L, WHITE, 0.55); D = mix(L, BLACK, 0.35)
    base = {"o": OUTL, "g": GLASS, "G": GLASS_D, "k": CORK_D, "c": CORK, "C": CORK_D, "L": L, "h": h, "D": D}
    drink = draw(DRINK, base)
    throw = draw(THROW, dict(base, b=(70, 70, 80, 255), f=(90, 60, 30, 255)))
    dust = draw(DUST, {"L": L, "h": h, "s": mix(L, WHITE, 0.3), "t": (200, 170, 90, 255),
                       "p": (110, 80, 50, 255), "P": (185, 150, 105, 255)})
    return drink, throw, dust

def dust_base():
    m = [
        "................",
        "................",
        "................",
        "................",
        ".......ww.......",
        "......wWWw......",
        ".....wWWWWw.....",
        "....wWWsWWWw....",
        "...wWWWWWsWWw...",
        "..wWWsWWWWWWWw..",
        "..wWWWWWWsWWWw..",
        "...ddddddddddd..",
        "................",
        "................",
        "................",
        "................",
    ]
    return draw(m, {"w": (190, 185, 175, 255), "W": (235, 232, 222, 255), "s": (255, 240, 160, 255), "d": (150, 145, 135, 255)})

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    imgs = []
    for name, col in TYPES:
        d, t, s = make(col)
        d.save(os.path.join(OUT, name + ".png")); t.save(os.path.join(OUT, name + "_throw.png")); s.save(os.path.join(OUT, name + "_dust.png"))
        imgs += [d, t, s]
    db = dust_base(); db.save(os.path.join(OUT, "dust_base.png")); imgs.append(db)
    if "--preview" in sys.argv:
        cols = 9; rows = (len(imgs) + cols - 1) // cols
        sheet = Image.new("RGBA", (cols * 52, rows * 52), (60, 60, 70, 255))
        for i, im in enumerate(imgs):
            big = im.resize((48, 48), Image.NEAREST); sheet.paste(big, ((i % cols) * 52 + 2, (i // cols) * 52 + 2), big)
        sheet.save(sys.argv[-1])
    print(len(imgs))
