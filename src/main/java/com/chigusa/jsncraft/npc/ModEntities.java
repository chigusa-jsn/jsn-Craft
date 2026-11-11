package com.chigusa.jsncraft.npc;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** NPCの登録。受付嬢と、そのスポーンエッグ。 */
@EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, JsnCraft.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(JsnCraft.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ReceptionistEntity>> RECEPTIONIST = ENTITY_TYPES.register(
            "receptionist",
            () -> EntityType.Builder.of(ReceptionistEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build("receptionist"));

    public static final DeferredItem<DeferredSpawnEggItem> RECEPTIONIST_EGG = ITEMS.register(
            "receptionist_spawn_egg",
            () -> new DeferredSpawnEggItem(RECEPTIONIST, 0xE8DDF5, 0x9A7CC8, new Item.Properties()));

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(RECEPTIONIST.get(), ReceptionistEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(RECEPTIONIST_EGG);
        }
    }

    private ModEntities() {}
}
