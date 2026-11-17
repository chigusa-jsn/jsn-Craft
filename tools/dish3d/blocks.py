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

def alchemy_table(d):
    # 脚と棚
    for x, z in ((1, 1), (13, 1), (1, 13), (13, 13)):
        d.box([x, 0, z], [x + 2, 9, z + 2], mat="alch_wood_dark")
    d.box([1.5, 3, 1.5], [14.5, 4, 14.5], mat="alch_wood")
    # 天板
    d.box([0, 9, 0], [16, 11, 16], top="alch_top", side="alch_wood", bottom="alch_wood")
    # 丸いフラスコ（赤い薬）
    d.disc(5, 5, 2.6, 11, 14.2, mat="flask_red")
    d.disc(5, 5, 0.8, 14.2, 16.2, mat="glass")
    d.disc(5, 5, 1.0, 16.2, 16.8, mat="cork")
    # 細い瓶（青い薬）
    d.disc(3.6, 12, 1.3, 11, 14.5, mat="flask_blue")
    d.disc(3.6, 12, 0.7, 14.5, 15.2, mat="cork")
    # すり鉢とすりこぎ
    d.disc(11, 10.5, 2.4, 11, 13, top="mortar_top", side="mortar_stone", bottom="mortar_stone", fit=("up",))
    d.box([11.6, 12.5, 10.0], [12.4, 15.5, 10.8], mat="alch_wood_dark")
    # 薬草の束
    d.box([9, 11, 2.5], [14, 12.2, 4.5], mat="herb_bundle")

def jeweler_table(d):
    # 脚と引き出し
    for x, z in ((1, 1), (13, 1), (1, 13), (13, 13)):
        d.box([x, 0, z], [x + 2, 9, z + 2], mat="jewel_wood")
    d.box([1.5, 6, 1.5], [14.5, 9, 14.5], mat="jewel_wood")
    d.box([5, 6.8, 0.9], [11, 8.2, 1.5], mat="brass")              # 引き出しの取っ手
    # 天板（緑の布張り）
    d.box([0, 9, 0], [16, 10.5, 16], top="jewel_top", side="jewel_wood", bottom="jewel_wood", fit=("up",))
    # 小さな金床
    d.box([2.5, 10.5, 9.5], [6.5, 11.5, 13.5], mat="anvil_dark")
    d.box([3.5, 11.5, 10.5], [5.5, 12.5, 12.5], mat="anvil_dark")
    d.box([2.0, 12.5, 10.0], [7.0, 13.5, 13.0], mat="anvil_dark")
    # 拡大鏡（スタンド）
    d.box([10.5, 10.5, 10.5], [12.5, 11, 12.5], mat="brass")
    d.box([11.2, 11, 11.2], [11.8, 14.5, 11.8], mat="brass")
    d.box([9.5, 14.0, 11.0], [13.5, 17.0, 12.0], mat="brass")
    d.box([10.0, 14.5, 10.9], [13.0, 16.5, 12.1], mat="lens")
    # 宝石
    d.box([3, 10.5, 3], [4.5, 11.6, 4.5], mat="gem_red")
    d.box([5.5, 10.5, 3.5], [6.8, 11.4, 4.8], mat="gem_blue")
    d.box([4.2, 10.5, 5.5], [5.4, 11.3, 6.7], mat="gem_green")
    # 指輪（小さな輪）
    d.ring(10.5, 4.5, 1.6, 1.0, 10.5, 11.2, "brass", "brass", "brass")

def feeder(d):
    # 脚
    for x, z in ((1, 2), (13, 2), (1, 12), (13, 12)):
        d.box([x, 0, z], [x + 2, 3, z + 2], mat="feeder_wood")
    # 箱（底と4つの壁）
    d.box([1, 3, 2], [15, 4, 14], mat="feeder_wood")
    d.box([1, 4, 2], [15, 8, 3], mat="feeder_wood")
    d.box([1, 4, 13], [15, 8, 14], mat="feeder_wood")
    d.box([1, 4, 3], [2, 8, 13], mat="feeder_wood")
    d.box([14, 4, 3], [15, 8, 13], mat="feeder_wood")
    # 干し草
    d.box([2, 4, 3], [14, 7, 13], top="hay_top", side="hay_top", bottom="hay_top", fit=("up",))
    d.box([4, 7, 5], [8, 7.6, 9], mat="hay_top"); d.box([9, 7, 7], [12, 7.4, 11], mat="hay_top")

BLOCKS = {"forge": forge, "rune_altar": rune_altar, "alchemy_table": alchemy_table, "jeweler_table": jeweler_table, "feeder": feeder}

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
