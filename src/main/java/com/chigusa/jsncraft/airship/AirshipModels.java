package com.chigusa.jsncraft.airship;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** 飛空艇の形（tools/gen_airship.py が作る。手で直さない） */
public final class AirshipModels {
    /** 段階ごとの部品の名前（頭で色の付け方が決まる：hull_ 木の色 / balloon_ 染料の色 / fixed_ そのまま / prop_ 回る） */
    public static final String[][] PARTS = {
            {"hull_0", "fixed_0", "balloon_0", "fixed_1", "fixed_2", "prop_0"}, 
            {"hull_0", "fixed_0", "balloon_0", "fixed_1", "fixed_2", "prop_0", "hull_1", "fixed_3", "fixed_4", "prop_1", "fixed_5", "prop_2"}, 
            {"hull_0", "fixed_0", "balloon_0", "fixed_1", "fixed_2", "prop_0", "hull_1", "fixed_3", "fixed_4", "prop_1", "fixed_5", "prop_2", "hull_2", "balloon_1", "fixed_6"}};

    public static LayerDefinition tier1() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("hull_0", CubeListBuilder.create().texOffs(0, 0).addBox(-5F, -2F, -12F, 10F, 2F, 24F).texOffs(69, 0).addBox(-6F, -3F, -12F, 12F, 1F, 24F).texOffs(142, 0).addBox(5F, -7F, -12F, 1F, 5F, 24F).texOffs(193, 0).addBox(-6F, -7F, -12F, 1F, 5F, 24F).texOffs(244, 0).addBox(-5F, -7F, -13F, 10F, 5F, 1F).texOffs(267, 0).addBox(-5F, -7F, 12F, 10F, 6F, 2F).texOffs(292, 0).addBox(-4F, -6.5F, 14F, 8F, 5F, 2F).texOffs(313, 0).addBox(-2F, -6F, 16F, 4F, 4F, 2F).texOffs(326, 0).addBox(-6.5F, -7F, -12F, 13F, 1F, 24F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("fixed_0", CubeListBuilder.create().texOffs(401, 0).addBox(-6.6F, -4.5F, -12.6F, 13.2F, 1F, 25.2F).texOffs(479, 0).addBox(-0.5F, -10F, 16F, 1F, 3F, 2F).texOffs(486, 0).addBox(-0.5F, -11F, -16F, 1F, 9F, 3F).texOffs(495, 0).addBox(-1F, -10F, -16F, 2F, 2F, 3F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("balloon_0", CubeListBuilder.create().texOffs(0, 30).addBox(-7.5F, -30F, -15F, 15F, 12F, 30F).texOffs(91, 30).addBox(-9F, -27F, -13F, 18F, 6F, 26F).texOffs(180, 30).addBox(-4.5F, -31.5F, -13F, 9F, 1.5F, 26F).texOffs(251, 30).addBox(-4.5F, -18F, -13F, 9F, 1.5F, 26F).texOffs(322, 30).addBox(-5.5F, -29F, 15F, 11F, 10F, 3F).texOffs(351, 30).addBox(-5.5F, -29F, -18F, 11F, 10F, 3F).texOffs(380, 30).addBox(-3.5F, -27F, 18F, 7F, 6F, 2F).texOffs(399, 30).addBox(-3.5F, -27F, -20F, 7F, 6F, 2F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("fixed_1", CubeListBuilder.create().texOffs(418, 30).addBox(-7.9F, -30.4F, -11F, 15.8F, 12.8F, 2F).texOffs(455, 30).addBox(-7.9F, -30.4F, 9F, 15.8F, 12.8F, 2F).texOffs(0, 73).addBox(-7.8F, -25F, -15.3F, 15.6F, 2F, 30.6F).texOffs(94, 73).addBox(4F, -18F, -10F, 0.6F, 11F, 0.6F).texOffs(98, 73).addBox(-4.6F, -18F, -10F, 0.6F, 11F, 0.6F).texOffs(102, 73).addBox(4F, -18F, 9F, 0.6F, 11F, 0.6F).texOffs(106, 73).addBox(-4.6F, -18F, 9F, 0.6F, 11F, 0.6F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("fixed_2", CubeListBuilder.create().texOffs(110, 73).addBox(-0.5F, -35F, -24F, 1F, 7F, 6F).texOffs(125, 73).addBox(-4F, -24.5F, -23F, 8F, 1F, 5F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("prop_0", CubeListBuilder.create().texOffs(152, 73).addBox(-0.6F, 4F, 16.2F, 1.2F, 10F, 0.6F).texOffs(157, 73).addBox(-5F, 8.4F, 16.2F, 10F, 1.2F, 0.6F).texOffs(180, 73).addBox(-1F, 8F, 15.7F, 2F, 2F, 1.6F), PartPose.offset(0F, -9F, -16.5F));
        return LayerDefinition.create(mesh, 512, 512);
    }
    public static LayerDefinition tier2() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("hull_0", CubeListBuilder.create().texOffs(0, 0).addBox(-7F, -2F, -17F, 14F, 2F, 34F).texOffs(97, 0).addBox(-8F, -3F, -17F, 16F, 1F, 34F).texOffs(198, 0).addBox(7F, -10F, -17F, 1F, 8F, 34F).texOffs(269, 0).addBox(-8F, -10F, -17F, 1F, 8F, 34F).texOffs(340, 0).addBox(-7F, -10F, -18F, 14F, 8F, 1F).texOffs(371, 0).addBox(-7F, -10F, 17F, 14F, 9F, 2F).texOffs(404, 0).addBox(-6F, -9.5F, 19F, 12F, 8F, 2F).texOffs(433, 0).addBox(-4F, -9F, 21F, 8F, 7F, 2F).texOffs(0, 43).addBox(-8.5F, -10F, -17F, 17F, 1F, 34F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("fixed_0", CubeListBuilder.create().texOffs(103, 43).addBox(-8.6F, -7.5F, -17.6F, 17.2F, 1F, 35.2F).texOffs(209, 43).addBox(-0.5F, -13F, 21F, 1F, 3F, 2F).texOffs(216, 43).addBox(-0.5F, -15F, -21F, 1F, 13F, 3F).texOffs(225, 43).addBox(-1F, -13F, -21F, 2F, 2F, 3F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("balloon_0", CubeListBuilder.create().texOffs(236, 43).addBox(-11F, -43F, -22F, 22F, 17F, 44F).texOffs(369, 43).addBox(-12.5F, -40F, -20F, 25F, 11F, 40F).texOffs(0, 105).addBox(-8F, -44.5F, -20F, 16F, 1.5F, 40F).texOffs(113, 105).addBox(-8F, -26F, -20F, 16F, 1.5F, 40F).texOffs(226, 105).addBox(-9F, -42F, 22F, 18F, 15F, 3F).texOffs(269, 105).addBox(-9F, -42F, -25F, 18F, 15F, 3F).texOffs(312, 105).addBox(-7F, -40F, 25F, 14F, 11F, 2F).texOffs(345, 105).addBox(-7F, -40F, -27F, 14F, 11F, 2F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("fixed_1", CubeListBuilder.create().texOffs(378, 105).addBox(-11.4F, -43.4F, -18F, 22.8F, 17.8F, 2F).texOffs(429, 105).addBox(-11.4F, -43.4F, 16F, 22.8F, 17.8F, 2F).texOffs(0, 148).addBox(-11.3F, -35.5F, -22.3F, 22.6F, 2F, 44.6F).texOffs(136, 148).addBox(6F, -26F, -15F, 0.6F, 16F, 0.6F).texOffs(140, 148).addBox(-6.6F, -26F, -15F, 0.6F, 16F, 0.6F).texOffs(144, 148).addBox(6F, -26F, 14F, 0.6F, 16F, 0.6F).texOffs(148, 148).addBox(-6.6F, -26F, 14F, 0.6F, 16F, 0.6F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("fixed_2", CubeListBuilder.create().texOffs(152, 148).addBox(-0.5F, -51F, -31F, 1F, 10F, 6F).texOffs(167, 148).addBox(-6F, -35F, -30F, 12F, 1F, 5F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("prop_0", CubeListBuilder.create().texOffs(202, 148).addBox(-0.6F, 5F, 21.2F, 1.2F, 14F, 0.6F).texOffs(207, 148).addBox(-7F, 11.4F, 21.2F, 14F, 1.2F, 0.6F).texOffs(238, 148).addBox(-1F, 11F, 20.7F, 2F, 2F, 1.6F), PartPose.offset(0F, -12F, -21.5F));
        root.addOrReplaceChild("hull_1", CubeListBuilder.create().texOffs(247, 148).addBox(-5F, -16F, -14F, 10F, 7F, 10F).texOffs(288, 148).addBox(-5.5F, -17F, -14.5F, 11F, 1F, 11F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("fixed_3", CubeListBuilder.create().texOffs(333, 148).addBox(-5.1F, -13F, -13F, 10.2F, 2F, 8F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("fixed_4", CubeListBuilder.create().texOffs(371, 148).addBox(8F, -9F, -2F, 4F, 1F, 3F).texOffs(386, 148).addBox(10F, -10F, -3F, 2F, 3F, 4F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("prop_1", CubeListBuilder.create().texOffs(399, 148).addBox(-11.5F, 4.5F, -1.75F, 1F, 8F, 0.5F).texOffs(403, 148).addBox(-15F, 8F, -1.75F, 8F, 1F, 0.5F), PartPose.offset(11F, -8.5F, 1.5F));
        root.addOrReplaceChild("fixed_5", CubeListBuilder.create().texOffs(421, 148).addBox(-12F, -9F, -2F, 4F, 1F, 3F).texOffs(436, 148).addBox(-12F, -10F, -3F, 2F, 3F, 4F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("prop_2", CubeListBuilder.create().texOffs(449, 148).addBox(10.5F, 4.5F, -1.75F, 1F, 8F, 0.5F).texOffs(453, 148).addBox(7F, 8F, -1.75F, 8F, 1F, 0.5F), PartPose.offset(-11F, -8.5F, 1.5F));
        return LayerDefinition.create(mesh, 512, 512);
    }
    public static LayerDefinition tier3() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("hull_0", CubeListBuilder.create().texOffs(0, 0).addBox(-10F, -2F, -23F, 20F, 2F, 46F).texOffs(133, 0).addBox(-11F, -3F, -23F, 22F, 1F, 46F).texOffs(270, 0).addBox(10F, -12F, -23F, 1F, 10F, 46F).texOffs(365, 0).addBox(-11F, -12F, -23F, 1F, 10F, 46F).texOffs(460, 0).addBox(-10F, -12F, -24F, 20F, 10F, 1F).texOffs(0, 57).addBox(-10F, -12F, 23F, 20F, 11F, 2F).texOffs(45, 57).addBox(-9F, -11.5F, 25F, 18F, 10F, 2F).texOffs(86, 57).addBox(-7F, -11F, 27F, 14F, 9F, 2F).texOffs(119, 57).addBox(-11.5F, -12F, -23F, 23F, 1F, 46F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("fixed_0", CubeListBuilder.create().texOffs(258, 57).addBox(-11.6F, -9.5F, -23.6F, 23.2F, 1F, 47.2F).texOffs(400, 57).addBox(-0.5F, -15F, 27F, 1F, 3F, 2F).texOffs(407, 57).addBox(-0.5F, -19F, -27F, 1F, 17F, 3F).texOffs(416, 57).addBox(-1F, -15F, -27F, 2F, 2F, 3F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("balloon_0", CubeListBuilder.create().texOffs(0, 107).addBox(-14F, -57F, -28.5F, 28F, 23F, 57F).texOffs(171, 107).addBox(-15.5F, -54F, -26.5F, 31F, 17F, 53F).texOffs(340, 107).addBox(-11F, -58.5F, -26.5F, 22F, 1.5F, 53F).texOffs(0, 188).addBox(-11F, -34F, -26.5F, 22F, 1.5F, 53F).texOffs(151, 188).addBox(-12F, -56F, 28.5F, 24F, 21F, 3F).texOffs(206, 188).addBox(-12F, -56F, -31.5F, 24F, 21F, 3F).texOffs(261, 188).addBox(-10F, -54F, 31.5F, 20F, 17F, 2F).texOffs(306, 188).addBox(-10F, -54F, -33.5F, 20F, 17F, 2F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("fixed_1", CubeListBuilder.create().texOffs(351, 188).addBox(-14.4F, -57.4F, -24.5F, 28.8F, 23.8F, 2F).texOffs(414, 188).addBox(-14.4F, -57.4F, 22.5F, 28.8F, 23.8F, 2F).texOffs(0, 244).addBox(-14.3F, -46.5F, -28.8F, 28.6F, 2F, 57.6F).texOffs(174, 244).addBox(9F, -34F, -21F, 0.6F, 22F, 0.6F).texOffs(178, 244).addBox(-9.6F, -34F, -21F, 0.6F, 22F, 0.6F).texOffs(182, 244).addBox(9F, -34F, 20F, 0.6F, 22F, 0.6F).texOffs(186, 244).addBox(-9.6F, -34F, 20F, 0.6F, 22F, 0.6F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("fixed_2", CubeListBuilder.create().texOffs(190, 244).addBox(-0.5F, -68F, -37.5F, 1F, 13F, 6F).texOffs(205, 244).addBox(-7.5F, -46F, -36.5F, 15F, 1F, 5F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("prop_0", CubeListBuilder.create().texOffs(246, 244).addBox(-0.6F, 4F, 27.2F, 1.2F, 20F, 0.6F).texOffs(251, 244).addBox(-10F, 13.4F, 27.2F, 20F, 1.2F, 0.6F).texOffs(294, 244).addBox(-1F, 13F, 26.7F, 2F, 2F, 1.6F), PartPose.offset(0F, -14F, -27.5F));
        root.addOrReplaceChild("hull_1", CubeListBuilder.create().texOffs(303, 244).addBox(-6.5F, -21F, -20F, 13F, 10F, 13F).texOffs(356, 244).addBox(-7F, -22F, -20.5F, 14F, 1F, 14F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("fixed_3", CubeListBuilder.create().texOffs(413, 244).addBox(-6.6F, -15F, -19F, 13.2F, 2F, 11F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("fixed_4", CubeListBuilder.create().texOffs(463, 244).addBox(11F, -11F, -2F, 4F, 1F, 3F).texOffs(478, 244).addBox(13F, -12F, -3F, 2F, 3F, 4F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("prop_1", CubeListBuilder.create().texOffs(491, 244).addBox(-14.5F, 4.5F, -1.75F, 1F, 12F, 0.5F).texOffs(0, 305).addBox(-20F, 10F, -1.75F, 12F, 1F, 0.5F), PartPose.offset(14F, -10.5F, 1.5F));
        root.addOrReplaceChild("fixed_5", CubeListBuilder.create().texOffs(26, 305).addBox(-15F, -11F, -2F, 4F, 1F, 3F).texOffs(41, 305).addBox(-15F, -12F, -3F, 2F, 3F, 4F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("prop_2", CubeListBuilder.create().texOffs(54, 305).addBox(13.5F, 4.5F, -1.75F, 1F, 12F, 0.5F).texOffs(58, 305).addBox(8F, 10F, -1.75F, 12F, 1F, 0.5F), PartPose.offset(-14F, -10.5F, 1.5F));
        root.addOrReplaceChild("hull_2", CubeListBuilder.create().texOffs(84, 305).addBox(11F, -9F, -6F, 19F, 1F, 12F).texOffs(147, 305).addBox(-30F, -9F, -6F, 19F, 1F, 12F).texOffs(210, 305).addBox(28F, -9F, -7F, 3F, 2F, 14F).texOffs(245, 305).addBox(-31F, -9F, -7F, 3F, 2F, 14F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("balloon_1", CubeListBuilder.create().texOffs(280, 305).addBox(-0.2F, -69F, -26.5F, 0.4F, 5F, 8F), PartPose.offset(0F, 0F, 0F));
        root.addOrReplaceChild("fixed_6", CubeListBuilder.create().texOffs(298, 305).addBox(-0.5F, -70F, -27.5F, 1F, 13F, 1F).texOffs(303, 305).addBox(-1.5F, -16F, 29F, 3F, 3F, 2F).texOffs(314, 305).addBox(10.5F, -15F, 17F, 1F, 3F, 1F).texOffs(319, 305).addBox(-11.5F, -15F, 17F, 1F, 3F, 1F).texOffs(324, 305).addBox(10.5F, -15F, -21F, 1F, 3F, 1F).texOffs(329, 305).addBox(-11.5F, -15F, -21F, 1F, 3F, 1F), PartPose.offset(0F, 0F, 0F));
        return LayerDefinition.create(mesh, 512, 512);
    }

    private AirshipModels() {}
}
