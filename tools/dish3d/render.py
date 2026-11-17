"""
立体料理の確認用レンダラー（マイクラのブロックモデルJSONを斜め上から見た絵にする）。
ゲームそのものではなく近い見た目の再現図。面の明るさはマイクラと同じ（上1.0 / 南北0.8 / 東西0.6）。
"""
import math
from PIL import Image

SHADE = {"up": 1.0, "south": 0.8, "east": 0.6, "north": 0.8, "west": 0.6, "down": 0.5}

def auto_uv(face, f, t):
    x1, y1, z1 = f; x2, y2, z2 = t
    return {"up": (x1, z1, x2, z2), "down": (x1, 16 - z2, x2, 16 - z1),
            "south": (x1, 16 - y2, x2, 16 - y1), "north": (16 - x2, 16 - y2, 16 - x1, 16 - y1),
            "east": (16 - z2, 16 - y2, 16 - z1, 16 - y1), "west": (z1, 16 - y2, z2, 16 - y1)}[face]

def face_geom(face, f, t):
    """面の左上の角、u方向、v方向（3D）"""
    x1, y1, z1 = f; x2, y2, z2 = t
    if face == "up":    return (x1, y2, z1), (x2 - x1, 0, 0), (0, 0, z2 - z1)
    if face == "south": return (x1, y2, z2), (x2 - x1, 0, 0), (0, -(y2 - y1), 0)
    if face == "east":  return (x2, y2, z2), (0, 0, -(z2 - z1)), (0, -(y2 - y1), 0)
    raise ValueError(face)

class Renderer:
    def __init__(self, w, h, scale, ox, oy):
        self.img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.z = [[-1e9] * h for _ in range(w)]
        self.w, self.h, self.s, self.ox, self.oy = w, h, scale, ox, oy
        self.c30, self.s30 = math.cos(math.radians(30)), math.sin(math.radians(30))

    def proj(self, x, y, z):
        return (self.ox + (x - z) * self.c30 * self.s, self.oy + ((x + z) * self.s30 - y) * self.s)

    def model(self, model, textures, offset=(0, 0, 0)):
        """model: JSONの dict、textures: {"#名前": PIL画像(32×32)}"""
        ox, oy, oz = offset
        for el in model["elements"]:
            f = [el["from"][i] + offset[i] for i in range(3)]
            t = [el["to"][i] + offset[i] for i in range(3)]
            for face in ("up", "south", "east"):
                fd = el["faces"].get(face)
                if not fd: continue
                tex = textures[fd["texture"].lstrip("#")]
                uv = fd.get("uv") or auto_uv(face, el["from"], el["to"])
                self.quad(face, f, t, tex, uv, fd.get("tint", 1.0))

    def quad(self, face, f, t, tex, uv, tint):
        p0, eu, ev = face_geom(face, f, t)
        P0 = self.proj(*p0)
        Pu = self.proj(p0[0] + eu[0], p0[1] + eu[1], p0[2] + eu[2])
        Pv = self.proj(p0[0] + ev[0], p0[1] + ev[1], p0[2] + ev[2])
        a = (Pu[0] - P0[0], Pu[1] - P0[1]); b = (Pv[0] - P0[0], Pv[1] - P0[1])
        det = a[0] * b[1] - a[1] * b[0]
        if abs(det) < 1e-9: return
        xs = [P0[0], Pu[0], Pv[0], Pu[0] + b[0]]; ys = [P0[1], Pu[1], Pv[1], Pu[1] + b[1]]
        tw, th = tex.size; px = tex.load()
        sh = SHADE[face] * tint
        for X in range(max(0, int(min(xs))), min(self.w, int(max(xs)) + 2)):
            for Y in range(max(0, int(min(ys))), min(self.h, int(max(ys)) + 2)):
                dx, dy = X + 0.5 - P0[0], Y + 0.5 - P0[1]
                s = (dx * b[1] - dy * b[0]) / det; r = (a[0] * dy - a[1] * dx) / det
                if not (0 <= s <= 1 and 0 <= r <= 1): continue
                wx = p0[0] + eu[0] * s + ev[0] * r; wy = p0[1] + eu[1] * s + ev[1] * r; wz = p0[2] + eu[2] * s + ev[2] * r
                depth = wx + wy * 1.01 + wz
                if depth < self.z[X][Y]: continue
                u = uv[0] + (uv[2] - uv[0]) * s; v = uv[1] + (uv[3] - uv[1]) * r
                tx = min(tw - 1, max(0, int(u / 16 * tw))); ty = min(th - 1, max(0, int(v / 16 * th)))
                c = px[tx, ty]
                if c[3] < 128: continue
                self.z[X][Y] = depth
                self.img.putpixel((X, Y), (int(c[0] * sh), int(c[1] * sh), int(c[2] * sh), 255))
