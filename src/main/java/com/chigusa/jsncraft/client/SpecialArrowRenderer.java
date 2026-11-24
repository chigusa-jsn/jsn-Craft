package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.smithing.arrow.ArrowKind;
import com.chigusa.jsncraft.smithing.arrow.ModArrows;
import com.chigusa.jsncraft.smithing.arrow.SpecialArrow;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** 特別な矢の見た目（ふつうの矢・光る矢の絵＋飛んでいる間の色つきの軌跡）。 */
public class SpecialArrowRenderer extends ArrowRenderer<SpecialArrow> {
    private static final ResourceLocation ARROW = ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");
    private static final ResourceLocation SPECTRAL = ResourceLocation.withDefaultNamespace("textures/entity/projectiles/spectral_arrow.png");

    public SpecialArrowRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(SpecialArrow arrow) {
        ArrowKind k = arrow.kind();
        return k == ArrowKind.LIGHT || k == ArrowKind.HOMING || k == ArrowKind.THUNDER || k == ArrowKind.STORM || k == ArrowKind.STAR ? SPECTRAL : ARROW;
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Setup {
        @SubscribeEvent
        public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModArrows.SPECIAL_ARROW.get(), SpecialArrowRenderer::new);
            event.registerEntityRenderer(ModArrows.THROWN_BLADE.get(), ThrownItemRenderer::new);
        }
    }
}
