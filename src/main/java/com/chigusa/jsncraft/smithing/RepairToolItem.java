package com.chigusa.jsncraft.smithing;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 砥石（武器・道具）／修理キット（防具）／当て板（防具の補強）。
 * 持ち物の画面で、手に持って直したい物の上で右クリック。
 * 手に持って右クリック：砥石はもう片方の手の武器、修理キットは一番傷んだ防具、当て板は一番補強の少ない防具に使う。
 */
public class RepairToolItem extends Item {
    public enum Kind { WHETSTONE, KIT, PLATE }

    public static final int PLATE_HITS = 40;

    private final Kind kind;
    private final double amount;

    public RepairToolItem(Kind kind, double amount, Properties props) {
        super(props);
        this.kind = kind;
        this.amount = amount;
    }

    private boolean fits(ItemStack target) {
        if (!Durability.isGear(target)) return false;
        String k = SmithData.kinds(target);
        return switch (kind) {
            case WHETSTONE -> k.contains("W") || k.contains("T");
            case KIT -> !(k.contains("W") || k.contains("T"));
            case PLATE -> k.contains("A");
        };
    }

    /** 使ってみる。使えたら true（サーバーでだけ中身を変える） */
    private boolean apply(Player player, ItemStack tool, ItemStack target) {
        if (!fits(target)) return false;
        if (player.level().isClientSide) return true;
        if (kind == Kind.PLATE) {
            if (Durability.broken(target)) return say(player, "壊れた防具には付けられない", ChatFormatting.RED);
            if (Durability.plate(target) >= PLATE_HITS) return say(player, "もう当て板が付いている", ChatFormatting.GRAY);
            Durability.setPlate(target, PLATE_HITS);
            tool.shrink(1);
            player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 1.0F, 1.1F);
            return say(player, "当て板を付けた（防御 +1.5・あと " + PLATE_HITS + " 回受け止める）", ChatFormatting.GREEN);
        }
        if (Durability.broken(target)) return say(player, "壊れている…鍛冶師に鍛冶台で直してもらおう", ChatFormatting.RED);
        if (Durability.ratio(target) >= Durability.KIT_CAP)
            return say(player, "ここから先（" + Math.round(Durability.KIT_CAP * 100) + "% より上）は鍛冶師の鍛冶台で", ChatFormatting.GRAY);
        double a = amount;
        if (player instanceof ServerPlayer sp && SmithingService.smithLevel(sp) >= 0) a += 0.1;
        double got = Durability.repair(target, a, Durability.KIT_CAP);
        tool.shrink(1);
        if (kind == Kind.WHETSTONE) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 0.8F, 1.3F);
        } else {
            player.level().playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
        }
        return say(player, (kind == Kind.WHETSTONE ? "研いだ！ " : "修理した！ ") + "+" + Math.round(got * 100) + "%（今 "
                + Math.round(Durability.ratio(target) * 100) + "%）", ChatFormatting.GREEN);
    }

    private static boolean say(Player p, String msg, ChatFormatting c) {
        p.displayClientMessage(Component.literal(msg).withStyle(c), true);
        return true;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack tool, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        ItemStack target = slot.getItem();
        if (!fits(target)) return false;
        apply(player, tool, target);
        slot.setChanged();
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack tool = player.getItemInHand(hand);
        ItemStack target = ItemStack.EMPTY;
        if (kind == Kind.WHETSTONE) {
            target = player.getItemInHand(Durability.otherHand(hand));
        } else {
            double best = Double.MAX_VALUE;
            for (EquipmentSlot s : EquipmentSlot.values()) {
                if (s.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
                ItemStack st = player.getItemBySlot(s);
                if (!fits(st)) continue;
                double v;
                if (kind == Kind.PLATE) {
                    if (Durability.broken(st) || Durability.plate(st) >= PLATE_HITS) continue;
                    v = Durability.plate(st);
                } else {
                    boolean br = Durability.broken(st);
                    if (!br && Durability.ratio(st) >= Durability.KIT_CAP) continue;
                    v = br ? 2 : Durability.ratio(st);
                }
                if (v < best) { best = v; target = st; }
            }
        }
        if (target.isEmpty() || !fits(target)) {
            if (!level.isClientSide) say(player, kind == Kind.WHETSTONE ? "もう片方の手に武器か道具を持とう" : "直せる防具がない", ChatFormatting.GRAY);
            return InteractionResultHolder.fail(tool);
        }
        apply(player, tool, target);
        return InteractionResultHolder.sidedSuccess(tool, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        switch (kind) {
            case WHETSTONE -> tip.add(Component.literal("武器・道具の切れ味を " + Math.round(amount * 100) + "% 戻す").withStyle(ChatFormatting.GRAY));
            case KIT -> tip.add(Component.literal("防具・盾・エリトラ・釣り竿の耐久を " + Math.round(amount * 100) + "% 戻す").withStyle(ChatFormatting.GRAY));
            case PLATE -> tip.add(Component.literal("防具に付けると 防御 +1.5・タフネス +1（" + PLATE_HITS + " 回受けると外れる）").withStyle(ChatFormatting.GRAY));
        }
        if (kind != Kind.PLATE) {
            tip.add(Component.literal(Math.round(Durability.KIT_CAP * 100) + "% まで。壊れた物・それより上は鍛冶師の鍛冶台で").withStyle(ChatFormatting.DARK_GRAY));
            tip.add(Component.literal("鍛冶師が使うと +10%").withStyle(ChatFormatting.DARK_GRAY));
        }
        tip.add(Component.literal("持って、直したい物の上で右クリック").withStyle(ChatFormatting.DARK_AQUA));
        for (var c : SmithingService.CRAFTS) {
            if (c.output().get() == this) {
                tip.add(Component.literal("作り方：鍛冶台の素材の枠に ").withStyle(ChatFormatting.DARK_GRAY)
                        .append(Component.translatable(c.input().getDescriptionId())).append(" ×" + c.count()
                        + (c.minLevel() > 0 ? "（鍛冶師Lv" + c.minLevel() + "〜）" : "（鍛冶師）")).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
