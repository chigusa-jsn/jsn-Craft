package com.chigusa.jsncraft.weapon;

import net.minecraft.world.item.Rarity;

import java.util.List;

/** 癒し手の聖杖（6段階）。性能は杖と同じで、右クリックで聖光弾。癒しの魔法の回復量もこの強さで決まる。 */
public class HolyStaffItem extends StaffItem {

    public static final List<Tier> TIERS = List.of(
            new Tier(1, "wood_holy_staff", "木の聖杖", 200, 2.0F, 0.05, 10, 4.0F, Rarity.COMMON, 0xFFF0B0, false),
            new Tier(2, "silver_holy_staff", "銀の聖杖", 400, 2.5F, 0.10, 20, 5.0F, Rarity.COMMON, 0xE0F0FF, false),
            new Tier(3, "crystal_holy_staff", "聖水晶の聖杖", 800, 3.0F, 0.16, 30, 6.5F, Rarity.UNCOMMON, 0xFFE880, false),
            new Tier(4, "netherite_holy_staff", "ネザライトの聖杖", 1800, 3.5F, 0.22, 40, 8.0F, Rarity.RARE, 0xFFB060, true),
            new Tier(5, "moon_silver_holy_staff", "月銀の聖杖", 2500, 4.0F, 0.30, 55, 10.0F, Rarity.RARE, 0xEEF4FF, true),
            new Tier(6, "star_silver_holy_staff", "星銀の聖杖", 3200, 4.5F, 0.40, 70, 12.5F, Rarity.EPIC, 0xFFE080, true));

    public HolyStaffItem(Tier tier) {
        super(tier);
    }

    @Override
    public boolean isHolyStaff() {
        return true;
    }

    @Override
    protected String kindLabel() {
        return "癒し手の聖杖";
    }

    @Override
    protected String boltLabel() {
        return "聖光弾";
    }
}
