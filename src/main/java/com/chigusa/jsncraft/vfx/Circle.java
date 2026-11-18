package com.chigusa.jsncraft.vfx;

import net.minecraft.resources.ResourceLocation;

/**
 * 魔法陣の柄（絵は textures/particle/circle/、particles/spell_circle.json と同じ順番）。
 * 絵は tools/gen_magic_circles.py で作る。
 */
public enum Circle {
    SWORD, FIRE, ICE, THUNDER, STAR, RUNE, ALCHEMY, HOLY, NATURE, GEM, WATER, EARTH, WIND, DARK;

    public static Circle byId(int i) {
        Circle[] v = values();
        return i >= 0 && i < v.length ? v[i] : HOLY;
    }

    /** スキルの魔法陣：名前の頭が属性（fire_ball など）ならその属性、なければ職業の柄 */
    public static Circle forSkill(ResourceLocation id) {
        if (id == null) return HOLY;
        String path = id.getPath();
        int slash = path.indexOf('/');
        String job = slash < 0 ? "" : path.substring(0, slash);
        String name = slash < 0 ? path : path.substring(slash + 1);
        String head = name.contains("_") ? name.substring(0, name.indexOf('_')) : name;
        Circle e = element(head);
        return e != null ? e : forJob(job);
    }

    public static Circle element(String s) {
        return switch (s) {
            case "fire", "flame", "inferno", "explosion" -> FIRE;
            case "ice", "frost", "blizzard", "absolute" -> ICE;
            case "thunder", "lightning", "bolt", "chain", "judgment" -> THUNDER;
            case "star", "stellar", "meteor", "gravity" -> STAR;
            case "holy", "light", "heal", "sanctuary" -> HOLY;
            case "water", "aqua", "tidal" -> WATER;
            case "earth", "stone", "ground" -> EARTH;
            case "wind", "aero", "gale", "tornado", "tempest" -> WIND;
            case "dark", "shadow", "curse", "drain" -> DARK;
            default -> null;
        };
    }

    public static Circle forJob(String job) {
        return switch (job) {
            case "sword", "sword_saint" -> SWORD;
            case "alchemist", "alchemy_sage" -> ALCHEMY;
            case "enchanter", "rune_master" -> RUNE;
            case "jeweler", "gem_master" -> GEM;
            case "farmer", "harvest_keeper", "lumberjack", "forest_master", "rancher", "ranch_lord" -> NATURE;
            case "chef", "grand_chef", "smith", "master_smith" -> FIRE;
            case "angler", "net_master" -> WATER;
            case "miner", "vein_king" -> EARTH;
            default -> HOLY;
        };
    }
}
