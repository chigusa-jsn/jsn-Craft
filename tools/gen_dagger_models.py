"""
短剣の逆手持ちのモデル（手に持ったときの向き）を計算して書き出す。
バニラの handheld の向きを、絵の面の中で180度回し（刃が腕にそって後ろを向く）、
柄が手の位置にくるように平行移動を足す。
    python3 tools/gen_dagger_models.py
"""
import json, math, os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/assets/jsn_craft/models/item")
IDS = ["stone_dagger", "iron_dagger", "diamond_dagger", "netherite_dagger", "moon_silver_dagger", "star_silver_dagger"]

H_VANILLA = (-6.0, -6.0, 0.0)   # バニラの剣の柄の位置（絵の中心から、1/16ブロック単位、上が+）
H_DAGGER = (-3.5, -3.5, 0.0)    # この短剣の柄の真ん中

RIGHT = {   # バニラ item/handheld の右手
    "thirdperson_righthand": ((0, -90, 55), (0, 4.0, 0.5), 0.85),
    "firstperson_righthand": ((0, -90, 25), (1.13, 3.2, 1.13), 0.68),
}

def rot_xyz(x, y, z):
    """JOML の rotationXYZ と同じ回転行列（Rx・Ry・Rz）"""
    x, y, z = map(math.radians, (x, y, z))
    cx, sx, cy, sy, cz, sz = math.cos(x), math.sin(x), math.cos(y), math.sin(y), math.cos(z), math.sin(z)
    Rx = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    Ry = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    Rz = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    def mul(A, B): return [[sum(A[i][k] * B[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
    return mul(mul(Rx, Ry), Rz)

def apply(M, v): return tuple(sum(M[i][k] * v[k] for k in range(3)) for i in range(3))

def reverse(rot, tr, s):
    R = rot_xyz(*rot)
    off = apply(R, tuple(s * (a + b) for a, b in zip(H_VANILLA, H_DAGGER)))
    t2 = tuple(round(t + o, 3) for t, o in zip(tr, off))
    r2 = (rot[0], rot[1], rot[2] + 180)
    return r2, t2

def display():
    d = {}
    for key, (rot, tr, s) in RIGHT.items():
        r2, t2 = reverse(rot, tr, s)
        d[key] = {"rotation": list(r2), "translation": list(t2), "scale": [s, s, s]}
        left = key.replace("righthand", "lefthand")
        # 左手：ゲームが Y・Z の回転と X の移動を反転して使う（バニラの handheld と同じ書き方）
        d[left] = {"rotation": [r2[0], -r2[1], -r2[2]], "translation": [t2[0], t2[1], t2[2]], "scale": [s, s, s]}
    return d

if __name__ == "__main__":
    disp = display()
    for i in IDS:
        with open(os.path.join(OUT, i + ".json"), "w") as f:
            json.dump({"parent": "minecraft:item/handheld", "textures": {"layer0": f"jsn_craft:item/weapon/{i}"}, "display": disp}, f, indent=2)
            f.write("\n")
    print(json.dumps(disp, indent=1))
