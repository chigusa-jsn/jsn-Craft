"""
立体のブロックのモデル（鍛冶台など）を作る。
    python3 tools/dish3d/blocks.py
"""
import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import materials
from build import Dish, TEX, ROOT

BLOCK_MODEL = os.path.join(ROOT, "src/main/resources/assets/jsn_craft/models/block")

def forge(d):
    # 石の火床（手前に火の口）
    d.box([0, 0, 0], [16, 9, 16], top="forge_stone", side="forge_stone", bottom="forge_stone")
    d.box([4, 2, -0.01], [12, 7, 0.5], mat="forge_glow")
    d.box([2, 9, 9], [14, 10, 15], mat="forge_stone")
    d.box([3, 9.2, 10], [13, 10.2, 14], mat="coals")
    # 金床
    d.box([3, 9, 2], [9, 10, 7], mat="anvil_iron")
    d.box([4.5, 10, 3], [7.5, 12, 6], mat="anvil_iron")
    d.box([2, 12, 2], [12, 14, 7], mat="anvil_iron")
    d.box([12, 12.6, 3.2], [14.5, 13.6, 5.8], mat="anvil_iron")
    # 鎚
    d.box([9.5, 14, 3.4], [14, 14.6, 4.0], mat="tray_wood")
    d.box([8.4, 14, 2.8], [10, 15.6, 4.6], mat="iron")

BLOCKS = {"forge": forge}

if __name__ == "__main__":
    built = {}
    for name, f in BLOCKS.items():
        d = Dish(); f(d); built[name] = d
    tex = materials.build_all(TEX)
    for name, d in built.items():
        m = d.json(tex)
        m["ambientocclusion"] = True
        json.dump(m, open(os.path.join(BLOCK_MODEL, name + ".json"), "w"), indent=1)
        print(name, len(d.els))
