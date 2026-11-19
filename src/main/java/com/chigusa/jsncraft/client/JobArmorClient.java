package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.armor.JobArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/** 戦闘職の服の描き方（GeckoLib）。形は職業ごと、絵は段階ごと。 */
public final class JobArmorClient {

    static final class Model extends GeoModel<JobArmorItem> {
        private static final ResourceLocation ANIM = JsnCraft.id("animations/armor/none.animation.json");

        @Override
        public ResourceLocation getModelResource(JobArmorItem item) {
            return JsnCraft.id("geo/armor/" + item.style.id + ".geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(JobArmorItem item) {
            return JsnCraft.id("textures/armor/" + item.style.id + "_" + item.tier + ".png");
        }

        @Override
        public ResourceLocation getAnimationResource(JobArmorItem item) {
            return ANIM;
        }
    }

    public static GeoArmorRenderer<JobArmorItem> renderer() {
        return new GeoArmorRenderer<>(new Model());
    }

    private JobArmorClient() {}
}
