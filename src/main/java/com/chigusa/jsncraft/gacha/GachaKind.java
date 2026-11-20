package com.chigusa.jsncraft.gacha;

import com.chigusa.jsncraft.casino.CasinoGame;

/** ガチャガチャの種類（中身は data/jsn_craft/gacha/<id>.json） */
public enum GachaKind {
    EQUIP("equip", "装備ガチャ", CasinoGame.GACHA, 0xFFC03030),
    MATERIAL("material", "素材ガチャ", CasinoGame.GACHA_MATERIAL, 0xFF3A9A4A),
    FIGURE("figure", "フィギュアガチャ", CasinoGame.GACHA_FIGURE, 0xFFD04070),
    TITLE("title", "肩書ガチャ", CasinoGame.GACHA_TITLE, 0xFFD0A020);

    public final String id, name;
    public final CasinoGame game;
    public final int color;

    GachaKind(String id, String name, CasinoGame game, int color) {
        this.id = id;
        this.name = name;
        this.game = game;
        this.color = color;
    }

    public static GachaKind of(CasinoGame g) {
        for (GachaKind k : values()) if (k.game == g) return k;
        return null;
    }
}
