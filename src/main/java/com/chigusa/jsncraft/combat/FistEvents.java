package com.chigusa.jsncraft.combat;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.skill.HitFlags;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillService;
import com.chigusa.jsncraft.weapon.GauntletItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** 拳闘士：素手のパンチに籠手と素手の心得の力を足し、気をためる。籠手は殴るたびに少しすり減る。 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class FistEvents {

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || event.getSource().getDirectEntity() != p) return;
        if (HitFlags.SKILL.get() || !p.getMainHandItem().isEmpty()) return;
        float add = 0;
        ItemStack g = GauntletItem.equipped(p);
        if (g.getItem() instanceof GauntletItem gi) {
            add += gi.spec().punch();
            g.hurtAndBreak(1, p.serverLevel(), p, item -> {});
        }
        SkillDefinition bare = SkillService.passive(p, "fist_bare");
        if (bare != null) add += (float) bare.power();
        if (add > 0) event.setAmount(event.getAmount() + add);
        if (bare != null || !g.isEmpty()) {
            SkillDefinition ki = SkillService.passive(p, "fist_ki_master");
            FistSkills.addKi(p, ki != null ? 2 : 1);
        }
    }

    private FistEvents() {}
}
