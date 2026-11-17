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

def rune_altar(d):
    # 土台
    d.box([1, 0, 1], [15, 3, 15], top="rune_stone", side="rune_stone", bottom="rune_stone")
    d.box([0.6, 1, 0.6], [15.4, 1.6, 15.4], mat="lapis_inlay")
    # 柱（魔紋が光る）
    d.box([4, 3, 4], [12, 10, 12], top="rune_stone", side="rune_glyph", bottom="rune_stone")
    # 天板
    d.box([2, 10, 2], [14, 12, 14], top="rune_top", side="rune_stone", bottom="rune_stone")
    # 開いた本
    d.box([4.4, 12, 5.0], [11.6, 12.4, 11.0], mat="book_cover")
    d.box([4.8, 12.4, 5.4], [7.9, 13.2, 10.6], top="book_page", side="book_page", fit=("up",))
    d.box([8.1, 12.4, 5.4], [11.2, 13.2, 10.6], top="book_page", side="book_page", fit=("up",))
    d.box([7.9, 12.4, 5.2], [8.1, 12.9, 10.8], mat="book_cover")
    # 四すみのアメジスト
    for x, z in ((1.6, 1.6), (12.6, 1.6), (1.6, 12.6), (12.6, 12.6)):
        d.box([x, 3, z], [x + 1.8, 6.5, z + 1.8], mat="amethyst")
        d.box([x + 0.4, 6.5, z + 0.4], [x + 1.4, 7.6, z + 1.4], mat="amethyst")

BLOCKS = {"forge": forge, "rune_altar": rune_altar}

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
