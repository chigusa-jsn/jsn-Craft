package com.chigusa.jsncraft.armor;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.npc.ModEntities;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/** 戦闘職の服：重さ × 段階の素材と、職業 × 段階 × 4部位のアイテム。 */
public final class ModArmor {

    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, JsnCraft.MODID);
    public static final List<DeferredItem<JobArmorItem>> ITEMS = new ArrayList<>();

    /** 守り：［重さ］［段階-1］＝｛頭, 胴, 脚, 足｝ */
    private static final int[][][] DEF = {
            {{1, 3, 2, 1}, {1, 4, 3, 1}, {2, 5, 4, 2}, {3, 6, 5, 2}, {3, 7, 6, 3}, {4, 8, 6, 3}},
            {{2, 4, 3, 1}, {2, 5, 4, 2}, {3, 6, 5, 2}, {3, 7, 6, 3}, {4, 8, 6, 3}, {4, 9, 7, 4}},
            {{2, 5, 4, 2}, {3, 6, 5, 2}, {3, 8, 6, 3}, {4, 8, 7, 3}, {4, 9, 7, 4}, {5, 10, 8, 4}}};
    private static final float[][] TOUGH = {{0, 0, 1, 2, 2, 3}, {0, 1, 1.5F, 2.5F, 3, 3.5F}, {1, 1.5F, 2, 3, 3.5F, 4}};
    private static final int[] DURA = {10, 15, 25, 37, 40, 45};
    private static final Rarity[] RARITY = {Rarity.COMMON, Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE, Rarity.RARE, Rarity.EPIC};

    @SuppressWarnings("unchecked")
    private static final DeferredHolder<ArmorMaterial, ArmorMaterial>[][] MAT = new DeferredHolder[3][6];

    static {
        for (ArmorStyle.Weight w : ArmorStyle.Weight.values()) {
            for (int t = 1; t <= 6; t++) {
                final int wi = w.ordinal(), ti = t - 1, tt = t;
                String name = "job_" + w.name().toLowerCase() + "_" + t;
                MAT[wi][ti] = MATERIALS.register(name, () -> new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
                    m.put(ArmorItem.Type.HELMET, DEF[wi][ti][0]);
                    m.put(ArmorItem.Type.CHESTPLATE, DEF[wi][ti][1]);
                    m.put(ArmorItem.Type.LEGGINGS, DEF[wi][ti][2]);
                    m.put(ArmorItem.Type.BOOTS, DEF[wi][ti][3]);
                    m.put(ArmorItem.Type.BODY, DEF[wi][ti][1]);
                }), 10 + ti * 3, wi == 2 ? SoundEvents.ARMOR_EQUIP_IRON : wi == 1 ? SoundEvents.ARMOR_EQUIP_CHAIN : SoundEvents.ARMOR_EQUIP_LEATHER,
                        () -> Ingredient.of(ti >= 3 ? Items.NETHERITE_SCRAP : Items.LEATHER),
                        List.of(new ArmorMaterial.Layer(JsnCraft.id(name))), TOUGH[wi][ti], wi == 2 && tt >= 4 ? 0.05F * (tt - 3) : 0));
            }
        }
    }

    public static void init() {
        ArmorItem.Type[] types = {ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS};
        for (ArmorStyle s : ArmorStyle.values()) {
            for (int t = 1; t <= 6; t++) {
                for (ArmorItem.Type type : types) {
                    int tier = t;
                    Holder<ArmorMaterial> mat = MAT[s.weight.ordinal()][t - 1];
                    Item.Properties props = new Item.Properties().durability(type.getDurability(DURA[t - 1])).rarity(RARITY[t - 1]);
                    if (t >= 4) props = props.fireResistant();
                    Item.Properties fp = props;
                    ITEMS.add(ModEntities.ITEMS.register(id(s, t, type), () -> new JobArmorItem(mat, type, fp, s, tier)));
                }
            }
        }
    }

    public static String piece(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> "head";
            case CHESTPLATE -> "chest";
            case LEGGINGS -> "legs";
            default -> "feet";
        };
    }

    public static String id(ArmorStyle s, int tier, ArmorItem.Type type) {
        return s.id + "_" + tier + "_" + piece(type);
    }

    static ResourceLocation key(Item i) {
        return BuiltInRegistries.ITEM.getKey(i);
    }

    private ModArmor() {}
}
