package com.chigusa.jsncraft.race;

/** 馬の名前と、枠の色（名前はレースの種から決まるので、画面でも同じ名前になる） */
public final class RaceNames {
    static final String[] NAMES = {
            "ダイヤモンドラン", "クリーパーボム", "エンダーパール", "ハッピーニンジン", "ネザーブレイズ", "ゴールデンアップル", "スライムジャンプ",
            "レッドストーン", "ムーンライト", "サクラフブキ", "ミナミノカゼ", "ホシノキセキ", "ハヤテマル", "ウミネコ", "トリプルセブン",
            "チップタワー", "ラッキーパンチ", "ブラックジャック", "テンレンカクテイ", "オオアタリ", "ツギコソカツ", "ゴハンオカワリ",
            "ヒカリノヤ", "ネムレナイヨル", "エメラルドドリーム", "スニッファー", "ウォーデンノシズケサ", "ヨアケノカネ", "アマグモ",
            "コハルビヨリ", "テッペキ", "マンゲツ", "シロガネ", "ハナビ", "カミナリ", "オモチ", "アカネゾラ", "ミッドナイト",
            "ネザライトハート", "ニジノハシ", "スターダスト", "カゼノコ", "ウマノスケ", "ハシレメロス", "ダッシュボード"};
    public static final int[] COLORS = {0xFFF0F0F0, 0xFF303030, 0xFFE03030, 0xFF3060E0, 0xFFF0D030, 0xFF30A050, 0xFFF08020, 0xFFF080B0};
    public static final String[] COLOR_NAMES = {"白", "黒", "赤", "青", "黄", "緑", "橙", "桃"};

    public static String[] pick(long seed) {
        RaceSim.Rng r = new RaceSim.Rng(seed * 13 + 3);
        String[] out = new String[RaceSim.HORSES];
        boolean[] used = new boolean[NAMES.length];
        for (int i = 0; i < out.length; i++) {
            int k;
            do k = (int) (r.next() * NAMES.length); while (used[k]);
            used[k] = true;
            out[i] = NAMES[k];
        }
        return out;
    }

    private RaceNames() {}
}
