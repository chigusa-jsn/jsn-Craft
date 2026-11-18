package com.chigusa.jsncraft.weapon;

import com.chigusa.jsncraft.stat.ResourceService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;

/** 魔導士の杖（6段階）。右クリックでマナ弾を撃つ。魔法の威力と最大マナは WeaponEvents で付く。 */
public class StaffItem extends Item {

    /** 段階ごとの性能 */
    public record Tier(int tier, String id, String name, int durability, float melee, double magic, int mana,
                       float bolt, Rarity rarity, int color, boolean fireproof) {}

    public static final List<Tier> TIERS = List.of(
            new Tier(1, "wood_staff", "木の杖", 200, 2.0F, 0.05, 10, 4.0F, Rarity.COMMON, 0xF0E0B0, false),
            new Tier(2, "crystal_staff", "水晶の杖", 400, 2.5F, 0.10, 20, 5.0F, Rarity.COMMON, 0x9AD8FF, false),
            new Tier(3, "orb_staff", "宝玉の杖", 800, 3.0F, 0.16, 30, 6.5F, Rarity.UNCOMMON, 0x5080FF, false),
            new Tier(4, "netherite_staff", "ネザライトの杖", 1800, 3.5F, 0.22, 40, 8.0F, Rarity.RARE, 0xFF5040, true),
            new Tier(5, "moon_silver_staff", "月銀の杖", 2500, 4.0F, 0.30, 55, 10.0F, Rarity.RARE, 0xD8E4FF, true),
            new Tier(6, "star_silver_staff", "星銀の杖", 3200, 4.5F, 0.40, 70, 12.5F, Rarity.EPIC, 0xFFE070, true));

    /** マナ弾1発のマナ */
    public static final float BOLT_MANA = 3.0F;

    private final Tier tier;

    public StaffItem(Tier tier) {
        super(props(tier));
        this.tier = tier;
    }

    private static Properties props(Tier t) {
        Properties p = new Properties().durability(t.durability()).rarity(t.rarity())
                .attributes(ItemAttributeModifiers.builder()
                        .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, t.melee() - 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -3.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                        .build());
        if (t.fireproof()) p = p.fireResistant();
        return p;
    }

    public Tier tier() {
        return tier;
    }

    /** 精霊の術師の魔導書か（魔導士の杖なら false） */
    public boolean isGrimoire() {
        return false;
    }

    /** 癒し手の聖杖か */
    public boolean isHolyStaff() {
        return false;
    }

    /** 魔導士のふつうの杖か */
    public boolean isPlainStaff() {
        return !isGrimoire() && !isHolyStaff();
    }

    protected String kindLabel() {
        return "魔導士の杖";
    }

    protected String boltLabel() {
        return "マナ弾";
    }

    /** 撃つ間隔（tick） */
    public int cooldown() {
        return 14 - tier.tier();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
        if (player instanceof ServerPlayer sp) {
            if (!sp.isCreative() && !ResourceService.tryUseMana(sp, BOLT_MANA)) {
                sp.displayClientMessage(Component.literal("マナが足りない").withStyle(ChatFormatting.AQUA), true);
                return InteractionResultHolder.fail(stack);
            }
            ManaBolt.fire(sp, this);
            stack.hurtAndBreak(1, sp, LivingEntity.getSlotForHand(hand));
            sp.getCooldowns().addCooldown(this, cooldown());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 10 + tier.tier() * 2;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal(kindLabel() + "（" + tier.tier() + "段目）").withStyle(ChatFormatting.DARK_PURPLE));
        lines.add(Component.literal("右クリック：" + boltLabel() + "（ダメージ " + fmt(tier.bolt()) + "・マナ " + fmt(BOLT_MANA) + "）").withStyle(ChatFormatting.AQUA));
        lines.add(Component.literal("持つと 魔法の威力 +" + Math.round(tier.magic() * 100) + "%・最大マナ +" + tier.mana()).withStyle(ChatFormatting.BLUE));
        if (tier.tier() < 6) {
            lines.add(Component.literal("鍛冶台で鍛冶型を使うと次の段へ（魔紋と強化はそのまま）").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static String fmt(float v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.format(Locale.ROOT, "%.1f", v);
    }
}
