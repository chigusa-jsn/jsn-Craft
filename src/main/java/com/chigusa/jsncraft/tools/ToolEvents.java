package com.chigusa.jsncraft.tools;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.stat.ModAttributes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** 星銀のツルハシ・スコップ・斧：しゃがんで掘ると 3×3 まとめて掘れる。釣り竿の段階の力。 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class ToolEvents {
    private static boolean busy;

    private static boolean area(ItemStack st) {
        return st.is(ModTools.STAR_PICKAXE.get()) || st.is(ModTools.STAR_SHOVEL.get()) || st.is(ModTools.STAR_AXE.get());
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (busy || !(event.getPlayer() instanceof ServerPlayer p) || !p.isShiftKeyDown()) return;
        ItemStack tool = p.getMainHandItem();
        if (!area(tool)) return;
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos c = event.getPos();
        BlockState cs = event.getState();
        if (!tool.isCorrectToolForDrops(cs)) return;
        float hard = cs.getDestroySpeed(level, c);
        // 見ている向きで、掘る面を決める（上下を見ていれば水平、そうでなければ縦の面）
        Direction face = Math.abs(p.getXRot()) > 50 ? Direction.UP : p.getDirection();
        busy = true;
        int broken = 0;
        try {
            for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) continue;
                BlockPos t = switch (face.getAxis()) {
                    case Y -> c.offset(a, 0, b);
                    case X -> c.offset(0, a, b);
                    default -> c.offset(a, b, 0);
                };
                BlockState s = level.getBlockState(t);
                float h = s.getDestroySpeed(level, t);
                if (s.isAir() || h < 0 || h > hard + 1.5F || !tool.isCorrectToolForDrops(s)) continue;
                if (!p.mayInteract(level, t)) continue;
                level.destroyBlock(t, true, p);
                broken++;
            }
        } finally {
            busy = false;
        }
        if (broken > 0) tool.hurtAndBreak(Math.max(1, broken / 3), p, EquipmentSlot.MAINHAND);
    }

    /** 釣り竿の段階：持っていると釣りの力が上がる */
    @SubscribeEvent
    public static void onModifiers(ItemAttributeModifierEvent event) {
        if (!(event.getItemStack().getItem() instanceof JsnRodItem rod)) return;
        int t = rod.tier;
        add(event, ModAttributes.FISHING_ZONE, "zone", 0.03 * t);
        add(event, ModAttributes.FISHING_REEL, "reel", 0.12 * t);
        add(event, ModAttributes.FISHING_BITE, "bite", 0.05 * t);
        add(event, ModAttributes.FISHING_TREASURE, "treasure", 0.015 * t);
        event.addModifier(Attributes.LUCK, new AttributeModifier(JsnCraft.id("rod_tier/luck"), 0.4 * t, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HAND);
    }

    private static void add(ItemAttributeModifierEvent e, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, String name, double v) {
        e.addModifier(attr, new AttributeModifier(JsnCraft.id("rod_tier/" + name), v, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HAND);
    }

    private ToolEvents() {}
}
