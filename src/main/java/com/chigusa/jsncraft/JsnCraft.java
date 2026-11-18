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
        com.chigusa.jsncraft.fishing.FishItems.init(); // 魚アイテム
        com.chigusa.jsncraft.smithing.ModSmithing.init(); // 鍛冶台と強化石（登録先は調理台と共通なので先に読み込む）
        com.chigusa.jsncraft.enchanting.ModEnchanting.init(); // 魔紋の祭壇と魔紋の書
        com.chigusa.jsncraft.alchemy.ModAlchemy.init(); // 調合台と薬
        com.chigusa.jsncraft.jewelry.ModJewelry.init(); // 細工台と宝石・装飾品
        com.chigusa.jsncraft.ranch.ModRanch.init(); // エサ箱と畜産物
        com.chigusa.jsncraft.weapon.ModWeapons.init(); // 職業武器と鍛冶型
        com.chigusa.jsncraft.cooking.ModCooking.register(modEventBus); // 調理台と料理
        com.chigusa.jsncraft.farming.ModCrops.register(modEventBus); // 新しい作物と食材
        ModTabs.init(); // クリエイティブのタブ（突然変異のスポーンエッグもここ）
        ModEntities.ITEMS.register(modEventBus);
        ModTabs.TABS.register(modEventBus);
        // スキル用の専用エフェクト
        com.chigusa.jsncraft.vfx.ModParticles.PARTICLE_TYPES.register(modEventBus);
        // サーバー→クライアントへ職業情報を送る通信を登録
        modEventBus.addListener(ModNetwork::register);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
