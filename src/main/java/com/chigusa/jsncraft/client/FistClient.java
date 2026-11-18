package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.weapon.GauntletItem;
import com.chigusa.jsncraft.weapon.ModWeapons;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * 拳闘士の籠手（クライアント）：
 * 素手で殴ると左右の拳が交互に出る、自分の視点で左の拳も見える、両腕に籠手が見える。
 */
public final class FistClient {

    private static ModelPart part;
    private static boolean leftNext = false;

    static ModelPart part() {
        if (part == null) {
            MeshDefinition md = new MeshDefinition();
            md.getRoot().addOrReplaceChild("g", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, 0.0F, -2.5F, 5, 5, 5), PartPose.ZERO);
            part = LayerDefinition.create(md, 32, 32).bakeRoot().getChild("g");
        }
        return part;
    }

    static ResourceLocation texture(ItemStack stack) {
        return JsnCraft.id("textures/entity/gauntlet/" + ((GauntletItem) stack.getItem()).spec().id() + ".png");
    }

    /** 腕の付け根にいる pose から、手の位置に籠手を描く（right：右腕か） */
    static void drawOnArm(PoseStack pose, MultiBufferSource buffers, int light, ItemStack stack, boolean right) {
        pose.pushPose();
        pose.translate((right ? -1.0F : 1.0F) / 16F, 5.5F / 16F, 0);
        part().render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture(stack))), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    static boolean barehandedWithGauntlets(AbstractClientPlayer p) {
        return p != null && p.getMainHandItem().isEmpty() && !GauntletItem.equipped(p).isEmpty();
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, value = Dist.CLIENT)
    public static final class Game {

        /** 素手で殴るとき、左右の拳を交互に出す */
        @SubscribeEvent
        public static void onAttackKey(InputEvent.InteractionKeyMappingTriggered event) {
            Minecraft mc = Minecraft.getInstance();
            if (!event.isAttack() || !barehandedWithGauntlets(mc.player)) return;
            event.setSwingHand(false);
            mc.player.swing(leftNext ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            leftNext = !leftNext;
        }

        /** 自分の視点：利き手の腕に籠手を描く */
        @SubscribeEvent
        public static void onArm(RenderArmEvent event) {
            AbstractClientPlayer p = event.getPlayer();
            ItemStack g = GauntletItem.equipped(p);
            if (g.isEmpty()) return;
            boolean right = event.getArm() == HumanoidArm.RIGHT;
            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            pose.translate((right ? -5.0F : 5.0F) / 16F, 2.0F / 16F, 0);
            drawOnArm(pose, event.getMultiBufferSource(), event.getPackedLight(), g, right);
            pose.popPose();
        }

        /** 自分の視点：反対の手も空いていれば、拳（と籠手）を見せる */
        @SubscribeEvent
        public static void onHand(RenderHandEvent event) {
            Minecraft mc = Minecraft.getInstance();
            AbstractClientPlayer p = mc.player;
            if (event.getHand() != InteractionHand.OFF_HAND || !event.getItemStack().isEmpty() || !barehandedWithGauntlets(p) || p.isInvisible()) return;
            ItemStack g = GauntletItem.equipped(p);
            HumanoidArm side = p.getMainArm().getOpposite();
            boolean right = side == HumanoidArm.RIGHT;
            float f = right ? 1.0F : -1.0F;
            float swing = event.getSwingProgress();
            float equip = event.getEquipProgress();
            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            // ItemInHandRenderer#renderPlayerArm と同じ動き
            float f1 = Mth.sqrt(swing);
            float f2 = -0.3F * Mth.sin(f1 * (float) Math.PI);
            float f3 = 0.4F * Mth.sin(f1 * (float) (Math.PI * 2));
            float f4 = -0.4F * Mth.sin(swing * (float) Math.PI);
            pose.translate(f * (f2 + 0.64000005F), f3 - 0.6F + equip * -0.6F, f4 - 0.71999997F);
            pose.mulPose(Axis.YP.rotationDegrees(f * 45.0F));
            float f5 = Mth.sin(swing * swing * (float) Math.PI);
            float f6 = Mth.sin(f1 * (float) Math.PI);
            pose.mulPose(Axis.YP.rotationDegrees(f * f6 * 70.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(f * f5 * -20.0F));
            pose.translate(f * -1.0F, 3.6F, 3.5F);
            pose.mulPose(Axis.ZP.rotationDegrees(f * 120.0F));
            pose.mulPose(Axis.XP.rotationDegrees(200.0F));
            pose.mulPose(Axis.YP.rotationDegrees(f * -135.0F));
            pose.translate(f * 5.6F, 0.0F, 0.0F);
            PlayerRenderer renderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(p);
            if (right) renderer.renderRightHand(pose, event.getMultiBufferSource(), event.getPackedLight(), p);
            else renderer.renderLeftHand(pose, event.getMultiBufferSource(), event.getPackedLight(), p);
            pose.pushPose();
            pose.translate((right ? -5.0F : 5.0F) / 16F, 2.0F / 16F, 0);
            drawOnArm(pose, event.getMultiBufferSource(), event.getPackedLight(), g, right);
            pose.popPose();
            pose.popPose();
        }
    }

    /** ほかの人から見た姿：両腕に籠手 */
    public static final class GauntletRenderer implements ICurioRenderer {
        @Override
        public <T extends LivingEntity, M extends net.minecraft.client.model.EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack pose,
                RenderLayerParent<T, M> parent, MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount, float partialTicks,
                float ageInTicks, float netHeadYaw, float headPitch) {
            if (!(parent.getModel() instanceof HumanoidModel<?> model)) return;
            pose.pushPose();
            model.rightArm.translateAndRotate(pose);
            drawOnArm(pose, buffers, light, stack, true);
            pose.popPose();
            pose.pushPose();
            model.leftArm.translateAndRotate(pose);
            drawOnArm(pose, buffers, light, stack, false);
            pose.popPose();
        }
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Setup {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                for (var g : ModWeapons.GAUNTLETS) CuriosRendererRegistry.register(g.get(), GauntletRenderer::new);
            });
        }
    }

    private FistClient() {}
}
