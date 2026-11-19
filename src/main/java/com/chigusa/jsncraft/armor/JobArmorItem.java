package com.chigusa.jsncraft.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

/** 戦闘職の服（立体の防具）。見た目は GeckoLib、絵は段階ごと。 */
public class JobArmorItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public final ArmorStyle style;
    public final int tier;

    public JobArmorItem(Holder<ArmorMaterial> material, Type type, Properties props, ArmorStyle style, int tier) {
        super(material, type, props);
        this.style = style;
        this.tier = tier;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private HumanoidModel<?> renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(@Nullable T entity, ItemStack stack, @Nullable EquipmentSlot slot, @Nullable HumanoidModel<T> original) {
                if (renderer == null) renderer = com.chigusa.jsncraft.client.JobArmorClient.renderer();
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal(style.jobName + "の服（" + tier + "段目）").withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal(style.jobName + "で着ると：" + style.pieceBonus.get(getType()).text(tier)).withStyle(ChatFormatting.BLUE));
        StringBuilder sb = new StringBuilder("4つそろえると：");
        for (int i = 0; i < style.setBonus.size(); i++) sb.append(i > 0 ? "・" : "").append(style.setBonus.get(i).text(tier));
        lines.add(Component.literal(sb.toString()).withStyle(ChatFormatting.DARK_AQUA));
        lines.add(Component.literal("（セットの強さは、いちばん低い段で決まる）").withStyle(ChatFormatting.DARK_GRAY));
        if (tier < 6) lines.add(Component.literal("鍛冶台で鍛冶型を使うと次の段へ（魔紋と強化はそのまま）").withStyle(ChatFormatting.DARK_GRAY));
    }
}
