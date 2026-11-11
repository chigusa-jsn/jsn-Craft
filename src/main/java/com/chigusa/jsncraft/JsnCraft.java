package com.chigusa.jsncraft;

import com.chigusa.jsncraft.data.ModAttachments;
import com.chigusa.jsncraft.network.ModNetwork;
import com.chigusa.jsncraft.npc.ModEntities;
import com.chigusa.jsncraft.stat.ModAttributes;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(JsnCraft.MODID)
public class JsnCraft {
    public static final String MODID = "jsn_craft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public JsnCraft(IEventBus modEventBus, ModContainer modContainer) {
        // プレイヤーに職業データをくっつける仕組みを登録
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        // マナ・スタミナなど、jsn-Craft の能力値を登録
        ModAttributes.ATTRIBUTES.register(modEventBus);
        // 受付嬢NPCとスポーンエッグ
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModEntities.ITEMS.register(modEventBus);
        // サーバー→クライアントへ職業情報を送る通信を登録
        modEventBus.addListener(ModNetwork::register);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
