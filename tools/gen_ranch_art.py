"""牧場の畜産物のアイコン（16×16）を作る。  python3 tools/gen_ranch_art.py [--preview 出力.png]"""
import os, sys
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/assets/jsn_craft/textures/item/ranch")

def hx(h): h = h.lstrip("#"); return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)

def draw(rows, pal):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal: im.putpixel((x, y), hx(pal[ch]))
    return im

MEAT = [
    "................",
    "................",
    "................",
    ".....oooo.......",
    "...oommmmoo.....",
    "..ommmwmmmmo....",
    "..omwmmmmwmmoo..",
    ".ommmmwmmmmmmo..",
    ".ommwmmmmwmmmmo.",
    ".ommmmmmmmmwmmo.",
    "..ommwmmmmmmmo..",
    "..oommmmmwmmoo..",
    "....oommmmoo.bb.",
    "......oooo..bww.",
    "............bb..",
    "................",
]
DRUM = [
    "................",
    "................",
    "....oooo........",
    "...ommmmo.......",
    "..ommwmmmo......",
    "..ommmmmmmo.....",
    "..ommmmwmmo.....",
    "...ommmmmmo.....",
    "....ommmmmmo....",
    ".....ooommmo....",
    "........ommbo...",
    ".........obwwo..",
    "..........owwo..",
    "...........oo...",
    "................",
    "................",
]
EGG = [
    "................",
    "................",
    "................",
    "......oooo......",
    ".....ollllo.....",
    "....olwlmmmo....",
    "....olwmmmmo....",
    "...olwmmmmmmo...",
    "...olmmmmmmdo...",
    "...ommmmmmmdo...",
    "...ommmmmmddo...",
    "....ommmmddo....",
    ".....odddd o....",
    "......oooo......",
    "................",
    "................",
]
MILK = [
    "................",
    "......kkkk......",
    "......cCCc......",
    ".......gG.......",
    ".......gG.......",
    "......ogGo......",
    ".....oggggo.....",
    "....owwwwwwo....",
    "...owhwwwwwwo...",
    "...owhwwwwwwo...",
    "...owwwwbbbwo...",
    "...owwwbwwbwo...",
    "....owwwbbwo....",
    ".....oddddo.....",
    "......oooo......",
    "................",
]

def meat(base, fat):
    return draw(MEAT, {"o": "#3a1a10", "m": base, "w": fat, "b": "#e8e0d0"})

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    out = {
        "premium_beef": meat("#b02a2a", "#f0c0b0"),
        "premium_pork": meat("#e08080", "#fff0e8"),
        "premium_mutton": meat("#a83838", "#f8e0d0"),
        "premium_chicken": draw(DRUM, {"o": "#5a3a1a", "m": "#f0c890", "w": "#fff8e8", "b": "#e8e0d0"}),
        "golden_egg": draw(EGG, {"o": "#7a5a10", "l": "#fff8c0", "w": "#ffffff", "m": "#f0c020", "d": "#c08a10"}),
        "fresh_milk": draw(MILK, {"o": "#282837", "k": "#6e4b28", "c": "#966939", "C": "#6e4b28", "g": "#c8e1f0", "G": "#7896af",
                                  "w": "#fafaf5", "h": "#ffffff", "b": "#3a3a3a", "d": "#c8c8c0"}),
    }
    for k, im in out.items(): im.save(os.path.join(OUT, k + ".png"))
    if "--preview" in sys.argv:
        sheet = Image.new("RGBA", (len(out) * 52, 52), (60, 70, 60, 255))
        for i, im in enumerate(out.values()):
            big = im.resize((48, 48), Image.NEAREST); sheet.paste(big, (i * 52 + 2, 2), big)
        sheet.save(sys.argv[-1])
    print(len(out))
