package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 家畜の★（血統）の表示。
 * 照準を合わせると、照準の下に「★★★☆☆ 牛」。しゃがんでいると中身も出る。
 * ★4・★5 の家畜は、ときどきキラッと光る。
 */
public final class RanchHud {
    private static final Map<Integer, Integer> RANKS = new HashMap<>();
    private static final Map<Integer, com.chigusa.jsncraft.ranch.Mutant> MUTANTS = new HashMap<>();
    private static final int[] COLORS = {0xFFFFFFFF, 0xFFC8C8C8, 0xFF7CE07C, 0xFF60B8FF, 0xFFD080FF, 0xFFFFC830};

    public static void setRank(int entityId, int rank, int mutant) {
        RANKS.put(entityId, rank);
        if (mutant > 0) MUTANTS.put(entityId, com.chigusa.jsncraft.ranch.Mutant.byId(mutant));
        else MUTANTS.remove(entityId);
    }

    public static com.chigusa.jsncraft.ranch.Mutant mutant(Entity e) {
        return MUTANTS.getOrDefault(e.getId(), com.chigusa.jsncraft.ranch.Mutant.NONE);
    }

    public static int rank(Entity e) {
        return RANKS.getOrDefault(e.getId(), 0);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.screen != null) return;
        if (!(mc.crosshairPickEntity instanceof Animal a)) return;
        int rank = rank(a);
        if (rank <= 0) return;
        Font font = mc.font;
        int cx = g.guiWidth() / 2, y = g.guiHeight() / 2 + 10;
        String stars = "★".repeat(rank) + "☆".repeat(5 - rank);
        var mut = mutant(a);
        String name = (mut != com.chigusa.jsncraft.ranch.Mutant.NONE ? "✦" + mut.label : a.getName().getString()) + (a.isBaby() ? "（子ども）" : "");
        String line = stars + " " + name;
        int color = mut == com.chigusa.jsncraft.ranch.Mutant.NONE ? COLORS[rank]
                : java.awt.Color.HSBtoRGB((mc.level.getGameTime() % 60) / 60F, 0.5F, 1F) | 0xFF000000;   // 突然変異は虹色に光る文字
        g.drawString(font, line, cx - font.width(line) / 2, y, color, true);
        if (!mc.player.isShiftKeyDown()) return;
        List<String> info = new ArrayList<>(mut.perks);
        if (rank > 1) info.add("取れる量 +" + 25 * (rank - 1) + "%");
        if (rank >= 3 && (a instanceof Cow || a instanceof Pig || a instanceof Sheep || a instanceof Chicken))
            info.add("高級肉 " + 25 * (rank - 2) + "%");
        if (a instanceof Sheep && rank > 1) info.add("羊毛 +" + (rank - 1));
        if (a instanceof Chicken && rank > 1) info.add("卵が早い" + (rank >= 4 ? "・金の卵" : ""));
        if (a instanceof Cow && rank >= 4) info.add("搾りたてミルク ×2");
        if (a instanceof net.minecraft.world.entity.animal.goat.Goat && rank > 1) info.add("倒すと角 " + 20 * (rank - 1) + "%");
        if (a instanceof net.minecraft.world.entity.animal.Bee && rank > 1) info.add("ハチミツが早くたまる");
        if (a instanceof net.minecraft.world.entity.animal.sniffer.Sniffer && rank > 1) info.add("古代の種 +1（" + 25 * (rank - 1) + "%）");
        if (a instanceof AbstractHorse && rank > 1) info.add("足 +" + 5 * (rank - 1) + "%・体力 +" + 2 * (rank - 1));
        if (rank > 1) info.add("早く育つ +" + 5 * (rank - 1) + "%");
        int ly = y + 11;
        for (String s : info) {
            g.drawString(font, s, cx - font.width(s) / 2, ly, 0xFFB0C8B0, true);
            ly += 10;
        }
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Register {
        @SubscribeEvent
        public static void onRegisterLayers(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.CROSSHAIR, JsnCraft.id("animal_rank"), RanchHud::render);
        }

        /** 突然変異のスポーンエッグの色 */
        @SubscribeEvent
        public static void onColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
            for (var egg : com.chigusa.jsncraft.ModTabs.MUTANT_EGGS) {
                event.register((stack, tint) -> stack.getItem() instanceof com.chigusa.jsncraft.ranch.MutantEggItem m
                        ? 0xFF000000 | (tint == 0 ? m.base : m.spots) : -1, egg.get());
            }
        }
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, value = Dist.CLIENT)
    public static final class Events {
        /** ★4・★5 の家畜がキラッと光る */
        @SubscribeEvent
        public static void onTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null || mc.isPaused() || RANKS.isEmpty()) return;
            var r = mc.level.getRandom();
            if (mc.level.getGameTime() % 600 == 0) {   // いなくなった動物を忘れる
                RANKS.keySet().removeIf(id -> mc.level.getEntity(id) == null);
                MUTANTS.keySet().removeIf(id -> mc.level.getEntity(id) == null);
            }
            for (Animal a : mc.level.getEntitiesOfClass(Animal.class, mc.player.getBoundingBox().inflate(24))) {
                int rank = rank(a);
                var mut = mutant(a);
                if (mut != com.chigusa.jsncraft.ranch.Mutant.NONE && r.nextInt(8) == 0) {   // 突然変異はよく光る
                    mc.level.addParticle(mut == com.chigusa.jsncraft.ranch.Mutant.PEGASUS ? ParticleTypes.CLOUD : ParticleTypes.END_ROD,
                            a.getX() + (r.nextDouble() - 0.5) * a.getBbWidth(), a.getY() + r.nextDouble() * a.getBbHeight() + 0.2,
                            a.getZ() + (r.nextDouble() - 0.5) * a.getBbWidth(), 0, 0.01, 0);
                    continue;
                }
                if (rank < 4 || r.nextInt(rank == 5 ? 25 : 50) != 0) continue;
                double x = a.getX() + (r.nextDouble() - 0.5) * a.getBbWidth();
                double y = a.getY() + r.nextDouble() * a.getBbHeight() + 0.2;
                double z = a.getZ() + (r.nextDouble() - 0.5) * a.getBbWidth();
                mc.level.addParticle(rank == 5 ? ParticleTypes.WAX_ON : ParticleTypes.HAPPY_VILLAGER, x, y, z, 0, 0.02, 0);
            }
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            RANKS.clear();
            MUTANTS.clear();
        }

        @SubscribeEvent
        public static void onRespawn(ClientPlayerNetworkEvent.Clone event) {
            RANKS.clear();   // ディメンションを移ると番号が変わるので、届き直すのを待つ
            MUTANTS.clear();
        }
    }

    private RanchHud() {}
}
