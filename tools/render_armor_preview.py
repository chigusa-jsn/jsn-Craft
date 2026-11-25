"""防具（GeckoLib の geo と絵）を、ななめ前・ななめ後ろから見た見本の絵にする（確認用）。
    python3 tools/render_armor_preview.py <style> <tier> out.png"""
import json, math, sys, os
import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/jsn_craft")
BODY = [  # 中の人（灰色の箱）：origin, size
    ([-4, 24, -4], [8, 8, 8], (196, 160, 130)), ([-4, 12, -2], [8, 12, 4], (90, 110, 150)),
    ([-8, 12, -2], [4, 12, 4], (196, 160, 130)), ([4, 12, -2], [4, 12, 4], (196, 160, 130)),
    ([-4, 0, -2], [4, 12, 4], (60, 60, 90)), ([0, 0, -2], [4, 12, 4], (60, 60, 90))]

def rotm(rx, ry, rz):
    rx, ry, rz = map(math.radians, (rx, ry, rz))
    X = np.array([[1, 0, 0], [0, math.cos(rx), -math.sin(rx)], [0, math.sin(rx), math.cos(rx)]])
    Y = np.array([[math.cos(ry), 0, math.sin(ry)], [0, 1, 0], [-math.sin(ry), 0, math.cos(ry)]])
    Z = np.array([[math.cos(rz), -math.sin(rz), 0], [math.sin(rz), math.cos(rz), 0], [0, 0, 1]])
    return Z @ Y @ X

# 面：名前 → (角の番号 3つ：左上, 右上, 左下) を箱の8つの角 [x0/x1][y0/y1][z0/z1] から
def corners(o, s, inf):
    x0, y0, z0 = o[0] - inf, o[1] - inf, o[2] - inf
    x1, y1, z1 = o[0] + s[0] + inf, o[1] + s[1] + inf, o[2] + s[2] + inf
    return {"north": [(x1, y1, z0), (x0, y1, z0), (x1, y0, z0)], "south": [(x0, y1, z1), (x1, y1, z1), (x0, y0, z1)],
            "east": [(x0, y1, z0), (x0, y1, z1), (x0, y0, z0)], "west": [(x1, y1, z1), (x1, y1, z0), (x1, y0, z1)],
            "up": [(x1, y1, z1), (x0, y1, z1), (x1, y1, z0)], "down": [(x1, y0, z0), (x0, y0, z0), (x1, y0, z1)]}

def render(style, tier, yaw, scale=12):
    geo = json.load(open(f"{ROOT}/geo/armor/{style}.geo.json"))["minecraft:geometry"][0]
    tex = Image.open(f"{ROOT}/textures/armor/{style}_{tier}.png").convert("RGBA")
    TWg = geo["description"]["texture_width"]; k = tex.width / TWg
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    pitch = math.radians(12)
    def view(p):
        x, y, z = p
        xr, zr = x * cy - z * sy, x * sy + z * cy
        yr = y * math.cos(pitch) - zr * math.sin(pitch); zr2 = y * math.sin(pitch) + zr * math.cos(pitch)
        return (-xr, -yr, zr2)
    W, H = 30 * scale, 50 * scale
    quads = []
    for o, s, col in BODY:
        cs = corners(o, s, 0)
        for f, (a, b, c_) in cs.items():
            im = Image.new("RGBA", (8, 8), col + (255,))
            quads.append((im, [view(a), view(b), view(c_)]))
    for bone in geo["bones"]:
        for cube in bone.get("cubes", []):
            o, s, inf = cube["origin"], cube["size"], cube.get("inflate", 0)
            R = rotm(*[-v if i < 2 else v for i, v in enumerate(cube["rotation"])]) if "rotation" in cube else None
            piv = np.array(cube.get("pivot", [0, 0, 0]), float)
            cs = corners(o, s, inf)
            for f, pts in cs.items():
                uv = cube["uv"][f]; u, v = uv["uv"]; uw, uh = uv["uv_size"]
                reg = tex.crop((int(u * k), int(v * k), int(u * k) + max(1, int(round(uw * k))), int(v * k) + max(1, int(round(uh * k)))))
                if reg.getbbox() is None: continue
                P = []
                for p in pts:
                    p = np.array(p, float)
                    if R is not None: p = R @ (p - piv) + piv
                    P.append(view(p))
                quads.append((reg, P))
    # 奥から手前へ
    def depth(q):
        (a, b, c_) = q[1]; return (a[2] + b[2] + c_[2]) / 3 + 0.0
    quads.sort(key=depth, reverse=True)
    out = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ox, oy = W / 2, H * 0.08
    for reg, (a, b, c_) in quads:
        A = np.array([a[0], a[1]]) * scale + [ox, oy + 40 * scale]
        B = np.array([b[0], b[1]]) * scale + [ox, oy + 40 * scale]
        C = np.array([c_[0], c_[1]]) * scale + [ox, oy + 40 * scale]
        e1, e2 = B - A, C - A
        if abs(e1[0] * e2[1] - e1[1] * e2[0]) < 1e-3: continue
        # 裏向きの面は描かない
        if (e1[0] * e2[1] - e1[1] * e2[0]) < 0: continue
        w, h = reg.size
        M = np.array([[e1[0] / w, e2[0] / h, A[0]], [e1[1] / w, e2[1] / h, A[1]], [0, 0, 1]])
        inv = np.linalg.inv(M)
        tr = reg.transform((W, H), Image.AFFINE, tuple(inv[:2].flatten()), Image.NEAREST)
        out.alpha_composite(tr)
    return out

if __name__ == "__main__":
    style, tier, path = sys.argv[1], int(sys.argv[2]), sys.argv[3]
    a, b = render(style, tier, 25), render(style, tier, 205)
    sheet = Image.new("RGBA", (a.width * 2, a.height), (36, 40, 56, 255))
    sheet.alpha_composite(a, (0, 0)); sheet.alpha_composite(b, (a.width, 0))
    sheet.save(path)
