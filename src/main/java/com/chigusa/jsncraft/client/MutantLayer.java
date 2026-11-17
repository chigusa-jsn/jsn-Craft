package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.ranch.Mutant;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 突然変異の動物を、専用の絵で描きなおす（金のニワトリ・霜降り牛・トリュフ豚・天馬・女王バチ）。
 * ふつうの絵の上に、同じ形で専用の絵をぴったり重ねて描く。
 */
public class MutantLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    public MutantLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    private static ResourceLocation tex(String name) {
        return JsnCraft.id("textures/entity/mutant/" + name + ".png");
    }

    private static ResourceLocation textureFor(Mutant m, LivingEntity e) {
        return switch (m) {
            case GOLDEN_CHICKEN -> tex("golden_chicken");
            case MARBLED_COW -> tex("marbled_cow");
            case TRUFFLE_PIG -> tex("truffle_pig");
            case PEGASUS -> tex("pegasus");
            case QUEEN_BEE -> {
                Bee bee = (Bee) e;
                yield tex("queen_bee" + (bee.isAngry() ? "_angry" : "") + (bee.hasNectar() ? "_nectar" : ""));
            }
            default -> null;
        };
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        Mutant m = RanchHud.mutant(entity);
        if (m == Mutant.NONE || entity.isInvisible()) return;
        ResourceLocation tex = textureFor(m, entity);
        if (tex == null) return;
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);
        getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(tex)), light, overlay, 0xFFFFFFFF);
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Setup {
        @SubscribeEvent
        @SuppressWarnings({"unchecked", "rawtypes"})
        public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
            for (EntityType<?> type : new EntityType<?>[]{EntityType.CHICKEN, EntityType.COW, EntityType.PIG, EntityType.HORSE, EntityType.BEE}) {
                if (event.getRenderer(type) instanceof LivingEntityRenderer r) r.addLayer(new MutantLayer(r));
            }
        }
    }
}
