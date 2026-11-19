package com.chigusa.jsncraft.armor;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.job.JobService;
import com.chigusa.jsncraft.job.JobType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * 戦闘職の服の能力：その職業で着ているときだけ、部位ごとの能力とセット効果をつける（1秒ごとに見直す）。
 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class ArmorEvents {

    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        var job = JobService.data(p).getJob(JobType.COMBAT);
        String jobPath = job == null ? "" : job.getPath();
        // つけたい能力（同じ能力は足し合わせる）
        Map<ResourceLocation, Double> want = new HashMap<>();
        Map<ResourceLocation, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>> attrs = new HashMap<>();
        ArmorStyle setStyle = null;
        int count = 0, minTier = 9;
        for (EquipmentSlot slot : SLOTS) {
            if (!(p.getItemBySlot(slot).getItem() instanceof JobArmorItem a) || !a.style.jobs.contains(jobPath)) continue;
            ArmorStyle.Bonus b = a.style.pieceBonus.get(a.getType());
            add(want, attrs, b, a.tier);
            if (setStyle == null || setStyle == a.style) {
                setStyle = a.style;
                count++;
                minTier = Math.min(minTier, a.tier);
            }
        }
        if (setStyle != null && count == 4) for (ArmorStyle.Bonus b : setStyle.setBonus) add(want, attrs, b, minTier);
        // いまついている服の能力と見比べて、つけ直す
        for (var h : ALL) {
            AttributeInstance inst = p.getAttribute(h);
            if (inst == null) continue;
            ResourceLocation id = JsnCraft.id("job_armor/" + h.unwrapKey().map(k -> k.location().getPath()).orElse("x"));
            double v = want.getOrDefault(id, 0.0);
            AttributeModifier cur = inst.getModifier(id);
            if (v == 0) {
                if (cur != null) inst.removeModifier(id);
            } else if (cur == null || cur.amount() != v) {
                inst.addOrUpdateTransientModifier(new AttributeModifier(id, v, AttributeModifier.Operation.ADD_VALUE));
            }
        }
    }

    private static final java.util.List<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>> ALL = new java.util.ArrayList<>();

    static {
        for (ArmorStyle s : ArmorStyle.values()) {
            for (ArmorStyle.Bonus b : s.pieceBonus.values()) if (!ALL.contains(b.attribute())) ALL.add(b.attribute());
            for (ArmorStyle.Bonus b : s.setBonus) if (!ALL.contains(b.attribute())) ALL.add(b.attribute());
        }
    }

    private static void add(Map<ResourceLocation, Double> want, Map<ResourceLocation, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>> attrs,
                            ArmorStyle.Bonus b, int tier) {
        ResourceLocation id = JsnCraft.id("job_armor/" + b.attribute().unwrapKey().map(k -> k.location().getPath()).orElse("x"));
        want.merge(id, b.perTier() * tier, Double::sum);
        attrs.put(id, b.attribute());
    }

    private ArmorEvents() {}
}
