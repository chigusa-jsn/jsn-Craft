"""魔紋の書のアイコン（16×16）を作る。  python3 tools/gen_rune_art.py"""
import os
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/assets/jsn_craft/textures/item/enchanting")

def rune_book():
    P = {
        ".": None, "K": "#1a0e24", "C": "#4a1e6a", "c": "#6a2e94", "h": "#8a4ac0",
        "P": "#efe4c8", "p": "#c8b890", "G": "#e8b8ff", "g": "#b070f0", "Y": "#e8c040",
    }
    rows = [
        "................",
        "...KKKKKKKKKK...",
        "..KhhhhhhhhhcK..",
        "..KhccccccccCKP.",
        "..KhcYccccYcCKP.",
        "..Khcc.gGg.cCKP.",
        "..Khccg.G.gcCKP.",
        "..KhccgGGGgcCKP.",
        "..Khcc.gGg.cCKP.",
        "..KhccccGcccCKP.",
        "..KhcYccccYcCKP.",
        "..KhccccccccCKp.",
        "..KhCCCCCCCCCKp.",
        "..KKKKKKKKKKKKp.",
        "...pppppppppppp.",
        "................",
    ]
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            col = P.get(ch)
            if ch == "." and 5 <= y <= 9 and 6 <= x <= 10: col = P["c"]
            if col:
                h = col.lstrip("#")
                im.putpixel((x, y), tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,))
    im.save(os.path.join(OUT, "rune_book.png"))

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    rune_book()
    print("ok")
