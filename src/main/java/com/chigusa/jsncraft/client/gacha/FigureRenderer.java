package com.chigusa.jsncraft.client.gacha;

import com.chigusa.jsncraft.gacha.FigureBlock;
import com.chigusa.jsncraft.gacha.FigureBlockEntity;
import com.chigusa.jsncraft.gacha.FigureItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.properties.RotationSegment;

import java.util.HashMap;
import java.util.Map;

/** フィギュア：台の上に、そのモンスターを小さくして立たせる */
public class FigureRenderer implements BlockEntityRenderer<FigureBlockEntity> {
    private static final Map<EntityType<?>, Entity> CACHE = new HashMap<>();
    private static ClientLevel cacheLevel;

    public FigureRenderer(BlockEntityRendererProvider.Context ctx) {}

    static Entity entity(String id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        if (cacheLevel != mc.level) {
            CACHE.clear();
            cacheLevel = mc.level;
        }
        EntityType<?> type = FigureItem.type(id);
        if (type == null) return null;
        return CACHE.computeIfAbsent(type, t -> {
            try {
                Entity e = t.create(mc.level);
                if (e instanceof LivingEntity le) {
                    le.setYRot(0);
                    le.yBodyRot = 0;
                    le.yHeadRot = 0;
                    le.yBodyRotO = 0;
                    le.yHeadRotO = 0;
                }
                return e;
            } catch (Throwable ex) {
                return null;
            }
        });
    }

    /** 台の上（高さ2/16）に、高さ height ブロックまでに収めて描く */
    static void draw(String id, PoseStack ps, MultiBufferSource buf, int light, float yaw, float height, float partial) {
        Entity e = entity(id);
        if (e == null) return;
        float big = Math.max(e.getBbHeight(), e.getBbWidth() * 1.2F);
        float scale = Math.min(0.5F, height / Math.max(0.1F, big));
        ps.pushPose();
        ps.translate(0.5, 2 / 16.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
        ps.scale(scale, scale, scale);
        EntityRenderDispatcher d = Minecraft.getInstance().getEntityRenderDispatcher();
        d.setRenderShadow(false);
        try {
            d.render(e, 0, 0, 0, 0, partial, ps, buf, light);
        } catch (Throwable ignored) {
        }
        d.setRenderShadow(Minecraft.getInstance().options.entityShadows().get());
        ps.popPose();
    }

    @Override
    public void render(FigureBlockEntity be, float partial, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        float yaw = -RotationSegment.convertToDegrees(be.getBlockState().getValue(FigureBlock.ROTATION));
        draw(be.entityId, ps, buf, light, yaw + 180, 0.75F, partial);
    }
}
