package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.network.OreRevealPayload;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** 鉱石感知：見つけた鉱石の輪郭を、壁越しに色つきで光らせる（自分にだけ見える）。 */
@EventBusSubscriber(modid = JsnCraft.MODID, value = Dist.CLIENT)
public final class OreHighlight {
    private record Mark(BlockPos pos, int color) {}

    private static List<Mark> marks = List.of();
    private static long until;

    public static void show(OreRevealPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        List<Mark> list = new ArrayList<>();
        for (int i = 0; i < p.positions().size(); i++) {
            list.add(new Mark(BlockPos.of(p.positions().get(i)), i < p.colors().size() ? p.colors().get(i) : 0xFFFFFF));
        }
        marks = list;
        until = mc.level.getGameTime() + p.ticks();
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || marks.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        if (now > until) {
            marks = List.of();
            return;
        }
        // 消える前の2秒は点滅
        float alpha = until - now < 40 ? (now % 10 < 5 ? 0.35F : 0.9F) : 0.9F;

        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = ps.last().pose();

        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        for (Mark k : marks) {
            if (mc.level.getBlockState(k.pos()).isAir()) continue; // 掘ったら消える
            box(bb, m, k.pos(), k.color(), alpha);
        }
        MeshData mesh = bb.build();
        if (mesh != null) {
            RenderSystem.disableDepthTest(); // 壁越しに見えるように
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferUploader.drawWithShader(mesh);
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
        }
        ps.popPose();
    }

    private static void box(BufferBuilder bb, Matrix4f m, BlockPos p, int color, float alpha) {
        float r = ((color >> 16) & 0xFF) / 255F, g = ((color >> 8) & 0xFF) / 255F, b = (color & 0xFF) / 255F;
        float x0 = p.getX() + 0.02F, y0 = p.getY() + 0.02F, z0 = p.getZ() + 0.02F;
        float x1 = p.getX() + 0.98F, y1 = p.getY() + 0.98F, z1 = p.getZ() + 0.98F;
        float[][] c = {
                {x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1},
                {x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}};
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] e : edges) {
            bb.addVertex(m, c[e[0]][0], c[e[0]][1], c[e[0]][2]).setColor(r, g, b, alpha);
            bb.addVertex(m, c[e[1]][0], c[e[1]][1], c[e[1]][2]).setColor(r, g, b, alpha);
        }
    }

    private OreHighlight() {}
}
