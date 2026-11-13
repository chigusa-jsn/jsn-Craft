package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.npc.JobMasterEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 転職の神官の見た目。プレイヤーと同じ体（太い腕）に、神官のスキンを貼る。 */
public class JobMasterRenderer extends HumanoidMobRenderer<JobMasterEntity, PlayerModel<JobMasterEntity>> {

    private static final ResourceLocation TEXTURE = JsnCraft.id("textures/entity/job_master.png");

    public JobMasterRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(JobMasterEntity entity) {
        return TEXTURE;
    }
}
