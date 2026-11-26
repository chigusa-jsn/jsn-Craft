package com.chigusa.jsncraft.loginbonus;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** ログインボーナスの記録：最後に受け取った日・今の周の押した数・連続日数・合計日数。 */
public class LoginBonusData {
    public static final Codec<LoginBonusData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.optionalFieldOf("last", -1L).forGetter(d -> d.last),
            Codec.INT.optionalFieldOf("stamps", 0).forGetter(d -> d.stamps),
            Codec.INT.optionalFieldOf("streak", 0).forGetter(d -> d.streak),
            Codec.INT.optionalFieldOf("total", 0).forGetter(d -> d.total)
    ).apply(i, LoginBonusData::new));

    /** 最後に受け取った日（1970年からの日数。朝5時で日付が変わる）。-1 ならまだ */
    public long last;
    /** 今の周で受け取った数（0〜日数） */
    public int stamps;
    public int streak;
    public int total;

    public LoginBonusData() {
        this(-1, 0, 0, 0);
    }

    private LoginBonusData(long last, int stamps, int streak, int total) {
        this.last = last;
        this.stamps = stamps;
        this.streak = streak;
        this.total = total;
    }
}
