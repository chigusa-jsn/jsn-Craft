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

    public static final DeferredHolder<EntityType<?>, EntityType<JobMasterEntity>> JOB_MASTER = ENTITY_TYPES.register(
            "job_master",
            () -> EntityType.Builder.of(JobMasterEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build("job_master"));

    public static final DeferredItem<DeferredSpawnEggItem> JOB_MASTER_EGG = ITEMS.register(
            "job_master_spawn_egg",
            () -> new DeferredSpawnEggItem(JOB_MASTER, 0x2A2A2A, 0xD4AF37, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> RECEPTIONIST_EGG = ITEMS.register(
            "receptionist_spawn_egg",
            () -> new DeferredSpawnEggItem(RECEPTIONIST, 0xE8DDF5, 0x9A7CC8, new Item.Properties()));

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(RECEPTIONIST.get(), ReceptionistEntity.createAttributes().build());
        event.put(JOB_MASTER.get(), ReceptionistEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(RECEPTIONIST_EGG);
            event.accept(JOB_MASTER_EGG);
        }
    }

    private ModEntities() {}
}
