"""
持ち物の絵から「浮いている点」を消す。
マイクラは手に持った絵を1ピクセルずつ厚みのあるブロックにするので、本体から離れた星やきらめきが
小さな四角いブロックになって浮いてしまう。いちばん大きいかたまり（本体）とつながっていない点を消す。
    python3 tools/clean_floating.py 絵.png ...      （上書き）
"""
import sys
from PIL import Image

def drop_floating(img, ratio=0.2):
    img = img.convert("RGBA")
    w, h = img.size
    px = img.load()
    seen = [[False] * w for _ in range(h)]
    comps = []
    for y in range(h):
        for x in range(w):
            if seen[y][x] or px[x, y][3] == 0: continue
            stack, comp = [(x, y)], []
            seen[y][x] = True
            while stack:
                cx, cy = stack.pop(); comp.append((cx, cy))
                for dx in (-1, 0, 1):
                    for dy in (-1, 0, 1):
                        nx, ny = cx + dx, cy + dy
                        if 0 <= nx < w and 0 <= ny < h and not seen[ny][nx] and px[nx, ny][3] > 0:
                            seen[ny][nx] = True; stack.append((nx, ny))
            comps.append(comp)
    if not comps: return img
    biggest = max(len(c) for c in comps)
    for c in comps:
        # 本体と、本体と同じくらい大きいかたまり（籠手の2つめなど）は残す
        if len(c) < biggest * ratio:
            for (x, y) in c: px[x, y] = (0, 0, 0, 0)
    return img

if __name__ == "__main__":
    for f in sys.argv[1:]:
        drop_floating(Image.open(f)).save(f)
