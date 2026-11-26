package com.chigusa.jsncraft.job;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** 生産職の腕のレベル：基本職ならそのレベル、上位職なら 50 + レベル、どちらでもなければ -1 */
public final class JobLevels {
    public static int life(ServerPlayer player, String basic, String advanced) {
        var data = JobService.data(player);
        ResourceLocation life = data.getJob(JobType.LIFE);
        if (JsnCraft.id(basic).equals(life)) return data.level(life);
        if (JsnCraft.id(advanced).equals(life)) return 50 + data.level(life);
        return -1;
    }

    public static int miner(ServerPlayer p) { return life(p, "miner", "vein_king"); }
    public static int lumberjack(ServerPlayer p) { return life(p, "lumberjack", "forest_master"); }
    public static int farmer(ServerPlayer p) { return life(p, "farmer", "harvest_keeper"); }
    public static int angler(ServerPlayer p) { return life(p, "angler", "net_master"); }

    private JobLevels() {}
}
