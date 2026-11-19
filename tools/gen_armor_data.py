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
