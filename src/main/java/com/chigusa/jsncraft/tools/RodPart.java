package com.chigusa.jsncraft.tools;

import com.chigusa.jsncraft.stat.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** 釣り竿の部品の種類。段階（1〜4）ごとに効き目が上がる。 */
public enum RodPart {
    BLANK("blank", "竿", new String[]{"竹の竿", "樫の竿", "鋼の竿", "星の竿"}, "幸運（レアな魚が出やすい）"),
    REEL("reel", "リール", new String[]{"木のリール", "鉄のリール", "金のリール", "ダイヤのリール"}, "巻く速さ"),
    LURE("lure", "ルアー", new String[]{"羽根のルアー", "虫のルアー", "きらきらルアー", "光るルアー"}, "食いつきの速さ（3段目からお宝も）"),
    FLOAT("float", "ウキ", new String[]{"コルクのウキ", "木のウキ", "ガラスのウキ", "夜光のウキ"}, "魚がおとなしくなる"),
    LINE("line", "ライン", new String[]{"糸のライン", "麻のライン", "鋼線のライン", "魔法の糸"}, "当たりの幅が広がる");

    public final String id, name, effect;
    public final String[] names;

    RodPart(String id, String name, String[] names, String effect) {
        this.id = id;
        this.name = name;
        this.names = names;
        this.effect = effect;
    }

    public Holder<Attribute> attribute() {
        return switch (this) {
            case BLANK -> Attributes.LUCK;
            case REEL -> ModAttributes.FISHING_REEL;
            case LURE -> ModAttributes.FISHING_BITE;
            case FLOAT -> ModAttributes.FISHING_CALM;
            case LINE -> ModAttributes.FISHING_ZONE;
        };
    }

    /** 段階ごとの強さ */
    public double amount(int grade) {
        return switch (this) {
            case BLANK -> 0.6 * grade;
            case REEL -> 0.12 * grade;
            case LURE -> 0.05 * grade;
            case FLOAT -> 0.04 * grade;
            case LINE -> 0.035 * grade;
        };
    }

    public static RodPart byId(String id) {
        for (RodPart p : values()) if (p.id.equals(id)) return p;
        return null;
    }
}
