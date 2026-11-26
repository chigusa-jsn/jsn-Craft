package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.airship.Airship;
import com.chigusa.jsncraft.airship.AirshipModels;
import com.chigusa.jsncraft.airship.ModAirships;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** 飛空艇の見た目：船体は木の色、気球は染料の色を掛ける。プロペラが回る。 */
public class AirshipRenderer extends EntityRenderer<Airship> {
    public static final ModelLayerLocation[] LAYERS = {
            new ModelLayerLocation(JsnCraft.id("airship"), "tier1"),
            new ModelLayerLocation(JsnCraft.id("airship"), "tier2"),
            new ModelLayerLocation(JsnCraft.id("airship"), "tier3")};
    private final ModelPart[] roots = new ModelPart[3];

    public AirshipRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        for (int i = 0; i < 3; i++) roots[i] = ctx.bakeLayer(LAYERS[i]);
        shadowRadius = 1.2F;
    }

    @Override
    public ResourceLocation getTextureLocation(Airship ship) {
        return JsnCraft.id("textures/entity/airship/airship_" + ship.tier() + ".png");
    }

    @Override
    public void render(Airship ship, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        float bob = Mth.sin((ship.tickCount + pt) * 0.08F) * 0.04F;
        pose.translate(0, bob, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(pt, ship.yRotO, ship.getYRot())));
        pose.scale(-1, -1, 1);
        int t = ship.tier();
        ModelPart root = roots[t - 1];
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(ship)));
        float prop = Mth.lerp(pt, ship.propAnglePrev, ship.propAngle) * Mth.DEG_TO_RAD;
        int hull = 0xFF000000 | ship.hullColor();
        int balloon = 0xFF000000 | ship.balloonColor();
        for (String name : AirshipModels.PARTS[t - 1]) {
            ModelPart part = root.getChild(name);
            int color = -1;
            if (name.startsWith("hull_")) color = hull;
            else if (name.startsWith("balloon_")) color = balloon;
            else if (name.startsWith("prop_")) part.zRot = name.equals("prop_0") ? prop : -prop * 1.3F;
            part.render(pose, vc, light, OverlayTexture.NO_OVERLAY, color);
        }
        pose.popPose();
        super.render(ship, yaw, pt, pose, buffers, light);
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Setup {
        @SubscribeEvent
        public static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(LAYERS[0], AirshipModels::tier1);
            event.registerLayerDefinition(LAYERS[1], AirshipModels::tier2);
            event.registerLayerDefinition(LAYERS[2], AirshipModels::tier3);
        }

        @SubscribeEvent
        public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModAirships.AIRSHIP.get(), AirshipRenderer::new);
        }

        @SubscribeEvent
        public static void onScreens(RegisterMenuScreensEvent event) {
            event.register(ModAirships.MENU.get(), AirshipScreen::new);
        }
    }
}
