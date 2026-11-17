package com.chigusa.jsncraft.ranch;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.horse.Horse;

import java.util.List;

/** 突然変異の種類（番号は保存と通信に使うので変えない） */
public enum Mutant {
    NONE("", 0, List.of()),
    GOLDEN_CHICKEN("金のニワトリ", 0xFFFFC820, List.of("金の卵をよく産む", "倒すと金塊を落とす")),
    RAINBOW_SHEEP("虹色の羊", 0x00000000, List.of("毛の色がくるくる変わる", "刈るといろんな色の羊毛が出る")),
    MARBLED_COW("霜降り牛", 0xFFF0A0A0, List.of("倒すと必ず霜降り牛肉", "搾りたてミルクが3本・早く搾れる")),
    TRUFFLE_PIG("トリュフ豚", 0xFF6A4020, List.of("ときどき地面を掘って", "キノコやめずらしい物を見つける")),
    PEGASUS("天馬", 0xFFC8F0FF, List.of("とても速く、高く跳べる", "落下ダメージを受けない")),
    QUEEN_BEE("女王バチ", 0xFFB070FF, List.of("巣箱のハチミツがすぐたまる"));

    public final String label;
    /** 体にかける色（ARGB、0 なら色をかけない） */
    public final int tint;
    public final List<String> perks;

    Mutant(String label, int tint, List<String> perks) {
        this.label = label;
        this.tint = tint;
        this.perks = perks;
    }

    public static Mutant byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : NONE;
    }

    /** この動物がなれる突然変異（なければ NONE） */
    public static Mutant forEntity(Entity e) {
        if (e instanceof Chicken) return GOLDEN_CHICKEN;
        if (e instanceof Sheep) return RAINBOW_SHEEP;
        if (e instanceof Cow && !(e instanceof MushroomCow)) return MARBLED_COW;
        if (e instanceof Pig) return TRUFFLE_PIG;
        if (e instanceof Horse) return PEGASUS;
        if (e instanceof Bee) return QUEEN_BEE;
        return NONE;
    }
}
