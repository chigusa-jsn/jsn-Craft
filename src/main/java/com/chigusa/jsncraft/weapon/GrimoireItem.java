package com.chigusa.jsncraft.weapon;

import net.minecraft.world.item.Rarity;

import java.util.List;

/** 精霊の術師の魔導書（6段階）。性能は杖と同じで、右クリックで精霊弾。 */
public class GrimoireItem extends StaffItem {

    public static final List<Tier> TIERS = List.of(
            new Tier(1, "old_grimoire", "古びた魔導書", 200, 2.0F, 0.05, 10, 4.0F, Rarity.COMMON, 0xC8B8A0, false),
            new Tier(2, "mandrake_grimoire", "マンドラゴラの魔導書", 400, 2.5F, 0.10, 20, 5.0F, Rarity.COMMON, 0x7AE060, false),
            new Tier(3, "spirit_grimoire", "精霊石の魔導書", 800, 3.0F, 0.16, 30, 6.5F, Rarity.UNCOMMON, 0x7AE0C0, false),
            new Tier(4, "netherite_grimoire", "ネザライトの魔導書", 1800, 3.5F, 0.22, 40, 8.0F, Rarity.RARE, 0xFF5040, true),
            new Tier(5, "moon_silver_grimoire", "月銀の魔導書", 2500, 4.0F, 0.30, 55, 10.0F, Rarity.RARE, 0xD8E4FF, true),
            new Tier(6, "star_silver_grimoire", "星銀の魔導書", 3200, 4.5F, 0.40, 70, 12.5F, Rarity.EPIC, 0xFFE070, true));

    public GrimoireItem(Tier tier) {
        super(tier);
    }

    @Override
    public boolean isGrimoire() {
        return true;
    }

    @Override
    protected String kindLabel() {
        return "精霊の術師の魔導書";
    }

    @Override
    protected String boltLabel() {
        return "精霊弾";
    }
}
