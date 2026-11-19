"""戦闘職の服のレシピ・アイテムモデル・名前（ja/en）を書き出す。"""
import json, os
R = 'src/main/resources'
STYLES = {
    'mage': dict(ja='魔導', tiers=['見習い', '水晶', '宝玉', 'ネザライト', '月銀', '星銀'],
                 en_t=['Apprentice', 'Crystal', 'Jewel', 'Netherite', 'Moon Silver', 'Star Silver'],
                 pieces=['魔導帽', '魔導ローブ', '魔導のすそ', '魔導の靴'],
                 en_p=['Mage Hat', 'Mage Robe', 'Mage Skirt', 'Mage Shoes'],
                 craft='minecraft:white_wool', up=['jsn_craft:crystal', 'jsn_craft:sapphire']),
    'warrior': dict(ja='剣士', tiers=['見習い', '鉄', 'ダイヤ', 'ネザライト', '月銀', '星銀'],
                    en_t=['Apprentice', 'Iron', 'Diamond', 'Netherite', 'Moon Silver', 'Star Silver'],
                    pieces=['剣士のはちまき', '剣士の胴着', '剣士のはかま', '剣士の足袋'],
                    en_p=['Swordsman Headband', 'Swordsman Gi', 'Swordsman Hakama', 'Swordsman Tabi'],
                    craft='minecraft:leather', up=['minecraft:iron_ingot', 'minecraft:diamond']),
    'axeman': dict(ja='', tiers=['見習い', '鉄', 'ダイヤ', 'ネザライト', '月銀', '星銀'], en_t=['Apprentice', 'Iron', 'Diamond', 'Netherite', 'Moon Silver', 'Star Silver'], pieces=['斧使いのかぶと', '斧使いの胴よろい', '斧使いのすね当て', '斧使いの長靴'], en_p=['Axeman Helm', 'Axeman Cuirass', 'Axeman Greaves', 'Axeman Boots'], craft='minecraft:red_wool', up=['minecraft:iron_ingot', 'minecraft:diamond']),
    'archer': dict(ja='', tiers=['見習い', '骨', 'ブレイズ', 'ネザライト', '月銀', '星銀'], en_t=['Apprentice', 'Bone', 'Blaze', 'Netherite', 'Moon Silver', 'Star Silver'], pieces=['射手のフード', '射手の上着', '射手のズボン', '射手の長靴'], en_p=['Archer Hood', 'Archer Tunic', 'Archer Trousers', 'Archer Boots'], craft='minecraft:green_wool', up=['minecraft:bone_block', 'minecraft:blaze_rod']),
    'lancer': dict(ja='', tiers=['見習い', '鉄', '海晶', 'ネザライト', '月銀', '星銀'], en_t=['Apprentice', 'Iron', 'Prismarine', 'Netherite', 'Moon Silver', 'Star Silver'], pieces=['槍兵のかぶと', '槍兵のうろこよろい', '槍兵の腰当て', '槍兵のすね当て'], en_p=['Lancer Helm', 'Lancer Scale Mail', 'Lancer Tassets', 'Lancer Greaves'], craft='minecraft:cyan_wool', up=['minecraft:iron_ingot', 'minecraft:prismarine_crystals']),
    'shield_knight': dict(ja='', tiers=['見習い', '鉄', 'ダイヤ', 'ネザライト', '月銀', '星銀'], en_t=['Apprentice', 'Iron', 'Diamond', 'Netherite', 'Moon Silver', 'Star Silver'], pieces=['盾騎士のかぶと', '盾騎士の胸よろい', '盾騎士の脚よろい', '盾騎士の鉄靴'], en_p=['Knight Helm', 'Knight Breastplate', 'Knight Leg Plates', 'Knight Sabatons'], craft='minecraft:copper_ingot', up=['minecraft:iron_block', 'minecraft:diamond']),
    'beast': dict(ja='', tiers=['見習い', '鉄', '骨', 'ネザライト', '月銀', '星銀'], en_t=['Apprentice', 'Iron', 'Bone', 'Netherite', 'Moon Silver', 'Star Silver'], pieces=['獣使いのおおかみずきん', '獣使いの毛皮の上着', '獣使いのズボン', '獣使いの毛皮の靴'], en_p=['Tamer Wolf Hood', 'Tamer Fur Coat', 'Tamer Trousers', 'Tamer Fur Boots'], craft='minecraft:rabbit_hide', up=['minecraft:iron_ingot', 'minecraft:bone_block']),
    'brawler': dict(ja='', tiers=['見習い', '鉄', '黒曜石', 'ネザライト', '月銀', '星銀'], en_t=['Apprentice', 'Iron', 'Obsidian', 'Netherite', 'Moon Silver', 'Star Silver'], pieces=['拳闘士のはちまき', '拳闘士の道着', '拳闘士のズボン', '拳闘士の足布'], en_p=['Brawler Headband', 'Brawler Gi', 'Brawler Pants', 'Brawler Foot Wraps'], craft='minecraft:orange_wool', up=['minecraft:iron_ingot', 'minecraft:obsidian']),
    'elementalist': dict(ja='', tiers=['見習い', 'マンドラゴラ', '精霊石', 'ネザライト', '月銀', '星銀'], en_t=['Apprentice', 'Mandrake', 'Spirit Stone', 'Netherite', 'Moon Silver', 'Star Silver'], pieces=['術師のずきん', '術師のローブ', '術師のすそ', '術師の靴'], en_p=['Elementalist Hood', 'Elementalist Robe', 'Elementalist Skirt', 'Elementalist Shoes'], craft='minecraft:lime_wool', up=['jsn_craft:mandrake', 'jsn_craft:spirit_stone']),
    'healer': dict(ja='', tiers=['見習い', '銀', '聖晶', 'ネザライト', '月銀', '星銀'], en_t=['Apprentice', 'Silver', 'Holy Crystal', 'Netherite', 'Moon Silver', 'Star Silver'], pieces=['癒し手のずきん', '癒し手の衣', '癒し手のすそ', '癒し手の靴'], en_p=['Healer Hood', 'Healer Vestment', 'Healer Skirt', 'Healer Shoes'], craft='minecraft:yellow_wool', up=['jsn_craft:silver_ingot', 'jsn_craft:holy_crystal']),
    'assassin': dict(ja='', tiers=['見習い', '鉄', 'ダイヤ', 'ネザライト', '月銀', '星銀'], en_t=['Apprentice', 'Iron', 'Diamond', 'Netherite', 'Moon Silver', 'Star Silver'], pieces=['暗殺者のずきん', '暗殺者の上着', '暗殺者のズボン', '暗殺者の足布'], en_p=['Assassin Hood', 'Assassin Jacket', 'Assassin Pants', 'Assassin Foot Wraps'], craft='minecraft:black_wool', up=['minecraft:iron_ingot', 'minecraft:diamond']),
}
PIECES = ['head', 'chest', 'legs', 'feet']
SHAPES = [["XXX", "X X"], ["X X", "XXX", "XXX"], ["XXX", "X X", "X X"], ["X X", "X X"]]
TPL = [None, 'jsn_craft:job_template_2', 'jsn_craft:job_template_3', 'minecraft:netherite_upgrade_smithing_template',
       'jsn_craft:moon_silver_template', 'jsn_craft:star_silver_template']

def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, indent=2); f.write('\n')

langs = {}
for lang in ('ja_jp', 'en_us'):
    p = f'{R}/assets/jsn_craft/lang/{lang}.json'
    langs[lang] = (p, json.load(open(p, encoding='utf-8')))

for sid, s in STYLES.items():
    for t in range(1, 7):
        for pi, piece in enumerate(PIECES):
            iid = f'{sid}_{t}_{piece}'
            w(f'{R}/assets/jsn_craft/models/item/{iid}.json',
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"jsn_craft:item/armor/{iid}"}})
            langs['ja_jp'][1][f'item.jsn_craft.{iid}'] = f"{s['tiers'][t-1]}の{s['pieces'][pi]}"
            langs['en_us'][1][f'item.jsn_craft.{iid}'] = f"{s['en_t'][t-1]} {s['en_p'][pi]}"
            if t == 1:
                w(f'{R}/data/jsn_craft/recipe/{iid}.json', {
                    "type": "minecraft:crafting_shaped", "category": "equipment",
                    "pattern": SHAPES[pi], "key": {"X": {"item": s['craft']}},
                    "result": {"id": f"jsn_craft:{iid}", "count": 1}})
            else:
                add = s['up'][t-2] if t <= 3 else ['minecraft:netherite_ingot', 'jsn_craft:moon_silver_ingot', 'jsn_craft:star_silver_ingot'][t-4]
                w(f'{R}/data/jsn_craft/recipe/{iid}_smithing.json', {
                    "type": "minecraft:smithing_transform",
                    "template": {"item": TPL[t-1]},
                    "base": {"item": f"jsn_craft:{sid}_{t-1}_{piece}"},
                    "addition": {"item": add},
                    "result": {"id": f"jsn_craft:{iid}", "count": 1}})
for p, d in langs.values():
    w(p, d)
print('ok')
