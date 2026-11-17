package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.enchanting.Runes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** 魔紋の説明文（魔紋の書・祭壇の一覧・ツールチップで共通） */
public final class RuneText {

    /** 付与の腕のレベルを「Lv20」「刻印師 Lv15」の形に */
    public static String level(int req) {
        return req > 50 ? "刻印師 Lv" + (req - 50) : "Lv" + req;
    }

    /** dark = 暗い背景（ツールチップ）用の色、false なら明るい背景（本）用 */
    public static List<Component> details(Runes.Rune r, boolean dark) {
        ChatFormatting info = dark ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY;
        ChatFormatting cost = dark ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY;
        List<Component> out = new ArrayList<>();
        out.add(Component.literal("✦" + r.name()).withStyle(dark ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        out.add(Component.literal("刻める物：" + r.category()).withStyle(info));
        out.add(Component.literal("触媒：").withStyle(info).append(r.catalyst().getDescription().copy().withStyle(dark ? ChatFormatting.AQUA : ChatFormatting.DARK_AQUA)));
        for (int l = 1; l <= r.maxLevel(); l++) {
            out.add(Component.literal(Runes.roman(l) + "  ").withStyle(dark ? ChatFormatting.GOLD : ChatFormatting.DARK_PURPLE)
                    .append(Component.literal(r.effect().apply(l)).withStyle(dark ? ChatFormatting.WHITE : ChatFormatting.BLACK)));
            out.add(Component.literal("   " + level(r.requiredLevel(l)) + "／ラピス" + Runes.lapisCost(l) + "・触媒" + Runes.catalystCost(l)
                    + "・経験値Lv" + Runes.xpCost(l)).withStyle(cost));
        }
        return out;
    }

    private RuneText() {}
}
