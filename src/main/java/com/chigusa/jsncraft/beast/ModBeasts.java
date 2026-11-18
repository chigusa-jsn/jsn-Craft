package com.chigusa.jsncraft.beast;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.npc.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Wolf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/** 獣使いの相棒の種類。 */
@EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModBeasts {

    public static final DeferredHolder<EntityType<?>, EntityType<PetWolf>> WOLF = ModEntities.ENTITY_TYPES.register("pet_wolf",
            () -> EntityType.Builder.of(PetWolf::new, MobCategory.CREATURE).sized(0.6F, 0.85F).clientTrackingRange(10).build("pet_wolf"));
    public static final DeferredHolder<EntityType<?>, EntityType<PetGolem>> GOLEM = ModEntities.ENTITY_TYPES.register("pet_golem",
            () -> EntityType.Builder.of(PetGolem::new, MobCategory.MISC).sized(0.7F, 1.35F).clientTrackingRange(10).build("pet_golem"));
    public static final DeferredHolder<EntityType<?>, EntityType<PetHawk>> HAWK = ModEntities.ENTITY_TYPES.register("pet_hawk",
            () -> EntityType.Builder.of(PetHawk::new, MobCategory.CREATURE).sized(0.5F, 0.9F).clientTrackingRange(10).build("pet_hawk"));
    public static final DeferredHolder<EntityType<?>, EntityType<PetBee>> BEE = ModEntities.ENTITY_TYPES.register("pet_bee",
            () -> EntityType.Builder.of(PetBee::new, MobCategory.CREATURE).sized(0.7F, 0.6F).clientTrackingRange(8).build("pet_bee"));

    // 百獣の王の相棒（jsn-boss のモデルを小さくしたもの）
    public static final DeferredHolder<EntityType<?>, EntityType<PetFox>> FOX = ModEntities.ENTITY_TYPES.register("pet_white_fox",
            () -> EntityType.Builder.of(PetFox::new, MobCategory.CREATURE).sized(0.8F, 1.05F).clientTrackingRange(10).build("pet_white_fox"));
    public static final DeferredHolder<EntityType<?>, EntityType<PetCat>> CAT = ModEntities.ENTITY_TYPES.register("pet_wind_cat",
            () -> EntityType.Builder.of(PetCat::new, MobCategory.CREATURE).sized(0.8F, 0.9F).clientTrackingRange(10).build("pet_wind_cat"));
    public static final DeferredHolder<EntityType<?>, EntityType<PetLion>> LION = ModEntities.ENTITY_TYPES.register("pet_thunder_lion",
            () -> EntityType.Builder.of(PetLion::new, MobCategory.CREATURE).sized(1.15F, 1.05F).clientTrackingRange(10).build("pet_thunder_lion"));
    public static final DeferredHolder<EntityType<?>, EntityType<PetDragon>> DRAGON = ModEntities.ENTITY_TYPES.register("pet_blade_dragon",
            () -> EntityType.Builder.of(PetDragon::new, MobCategory.CREATURE).sized(1.6F, 1.3F).passengerAttachments(1.15F).clientTrackingRange(12).build("pet_blade_dragon"));

    private static net.minecraft.world.entity.ai.attributes.AttributeSupplier geo(double hp, double speed, double dmg) {
        return net.minecraft.world.entity.Mob.createMobAttributes().add(Attributes.MAX_HEALTH, hp).add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.ATTACK_DAMAGE, dmg).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.FLYING_SPEED, 0.7)
                .add(Attributes.STEP_HEIGHT, 1.0).build();
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(WOLF.get(), Wolf.createAttributes().build());
        event.put(GOLEM.get(), IronGolem.createAttributes().build());
        event.put(HAWK.get(), Parrot.createAttributes().add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.FOLLOW_RANGE, 32.0).build());
        event.put(BEE.get(), Bee.createAttributes().build());
        event.put(FOX.get(), geo(40, 0.32, 6));
        event.put(CAT.get(), geo(40, 0.38, 7));
        event.put(LION.get(), geo(70, 0.32, 9));
        event.put(DRAGON.get(), geo(90, 0.3, 8));
    }

    public static void init() {}

    private ModBeasts() {}
}
