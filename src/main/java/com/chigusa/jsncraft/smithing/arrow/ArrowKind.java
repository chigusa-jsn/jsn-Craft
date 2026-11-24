package com.chigusa.jsncraft.smithing.arrow;

/** 鍛冶師が作る特別な矢の種類。 */
public enum ArrowKind {
    STEEL("steel_arrow", "鋼の矢", "ダメージ +50%", 1.5, 0xC8CCD4),
    PIERCE("pierce_arrow", "貫通矢", "敵を3体までつらぬく", 1.1, 0xE8E0C8),
    BLAST("blast_arrow", "炸裂矢", "当たった所で小さく爆発（ブロックは壊さない）", 0.8, 0xFF7030),
    FIRE("fire_arrow", "火矢", "燃やす", 1.0, 0xFF9020),
    ICE("ice_arrow", "氷矢", "凍らせて遅くする", 1.0, 0x90E0FF),
    THUNDER("thunder_arrow", "雷矢", "ときどき雷が落ちる", 1.0, 0xFFF060),
    POISON("poison_arrow", "毒矢", "毒にする", 1.0, 0x60C040),
    WEAKNESS("weakness_arrow", "弱体の矢", "弱らせて遅くする", 1.0, 0x8060A0),
    ROPE("rope_arrow", "ロープ矢", "壁に刺さると、下へはしごが伸びる", 0.5, 0xC8A060),
    LIGHT("light_arrow", "光の矢", "刺さった所がしばらく光る", 0.8, 0xFFFFC0),
    RETRIEVE("retrieve_arrow", "回収の矢", "当たった所の周りの落とし物を引き寄せる", 0.8, 0x50D0B0),
    HOMING("homing_arrow", "追尾の矢", "近くの敵へ少し曲がって飛ぶ", 1.0, 0xD070FF),
    BOLT("heavy_bolt", "太矢", "クロスボウ用。威力 ×2.2（弓だと弱い）", 2.2, 0x707880);

    public final String id, name, desc;
    public final double damage;
    public final int color;

    ArrowKind(String id, String name, String desc, double damage, int color) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.damage = damage;
        this.color = color;
    }

    public static ArrowKind byId(int i) {
        ArrowKind[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, i))];
    }
}
