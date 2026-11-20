package com.chigusa.jsncraft.card;

import com.chigusa.jsncraft.stat.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Map;

/** カードの能力：どの能力値が、レア度ごとにどれだけ上がるか */
public final class CardStats {
    public record Stat(String label, Holder<Attribute> attr, AttributeModifier.Operation op, double base, boolean percent) {}

    public static final double[] MULT = {1, 1.6, 2.4, 3.6, 5.5};
    public static final String[] RARITY = {"ノーマル", "レア", "スーパーレア", "ウルトラレア", "レジェンド"};
    public static final int[] COLOR = {0xFFB0B0B0, 0xFF50A0F0, 0xFFC060F0, 0xFFF0B030, 0xFFFF5070};
    public static final String[] FMT = {"§7", "§9", "§d", "§6", "§c"};

    private static Map<String, Stat> MAP;

    public static Map<String, Stat> map() {
        if (MAP == null) MAP = Map.ofEntries(
                Map.entry("attack", new Stat("攻撃力", Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADD_VALUE, 0.5, false)),
                Map.entry("health", new Stat("最大HP", Attributes.MAX_HEALTH, AttributeModifier.Operation.ADD_VALUE, 1, false)),
                Map.entry("armor", new Stat("防御力", Attributes.ARMOR, AttributeModifier.Operation.ADD_VALUE, 0.5, false)),
                Map.entry("speed", new Stat("移動速度", Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, 0.01, true)),
                Map.entry("luck", new Stat("運", Attributes.LUCK, AttributeModifier.Operation.ADD_VALUE, 0.25, false)),
                Map.entry("kbres", new Stat("ノックバック耐性", Attributes.KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_VALUE, 0.02, true)),
                Map.entry("mana", new Stat("最大マナ", ModAttributes.MAX_MANA, AttributeModifier.Operation.ADD_VALUE, 3, false)),
                Map.entry("stamina", new Stat("最大スタミナ", ModAttributes.MAX_STAMINA, AttributeModifier.Operation.ADD_VALUE, 5, false)),
                Map.entry("crit", new Stat("会心率", ModAttributes.CRIT_CHANCE, AttributeModifier.Operation.ADD_VALUE, 0.005, true)),
                Map.entry("critdmg", new Stat("会心ダメージ", ModAttributes.CRIT_DAMAGE, AttributeModifier.Operation.ADD_VALUE, 0.02, true)),
                Map.entry("magic", new Stat("魔力", ModAttributes.MAGIC_POWER, AttributeModifier.Operation.ADD_VALUE, 0.02, true)),
                Map.entry("heal", new Stat("回復力", ModAttributes.HEALING_POWER, AttributeModifier.Operation.ADD_VALUE, 0.02, true)),
                Map.entry("fortune", new Stat("採掘の幸運", ModAttributes.MINING_FORTUNE, AttributeModifier.Operation.ADD_VALUE, 0.05, false)),
                Map.entry("xp", new Stat("職業経験値", ModAttributes.JOB_XP_BONUS, AttributeModifier.Operation.ADD_VALUE, 0.02, true)),
                Map.entry("regen", new Stat("HP自然回復", ModAttributes.HP_REGEN, AttributeModifier.Operation.ADD_VALUE, 0.05, false)),
                Map.entry("guard", new Stat("被ダメージ軽減", ModAttributes.DAMAGE_REDUCTION, AttributeModifier.Operation.ADD_VALUE, 0.005, true)),
                Map.entry("steal", new Stat("吸血", ModAttributes.LIFESTEAL, AttributeModifier.Operation.ADD_VALUE, 0.003, true)),
                Map.entry("skill", new Stat("スキルダメージ", ModAttributes.SKILL_DAMAGE, AttributeModifier.Operation.ADD_VALUE, 0.015, true)),
                Map.entry("cdr", new Stat("クールタイム短縮", ModAttributes.COOLDOWN_REDUCTION, AttributeModifier.Operation.ADD_VALUE, 0.005, true)),
                Map.entry("boss", new Stat("ボスへのダメージ", ModAttributes.BOSS_DAMAGE, AttributeModifier.Operation.ADD_VALUE, 0.02, true)));
        return MAP;
    }

    public static double amount(CardList.Card c) {
        Stat s = map().get(c.stat());
        return s == null ? 0 : s.base() * MULT[c.rarity()];
    }

    public static String describe(String stat, double amount) {
        Stat s = map().get(stat);
        if (s == null) return "";
        return s.label() + " +" + (s.percent() ? trim(amount * 100) + "%" : trim(amount));
    }

    static String trim(double v) {
        double r = Math.round(v * 100) / 100.0;
        return r == (long) r ? String.valueOf((long) r) : String.valueOf(r);
    }

    private CardStats() {}
}
