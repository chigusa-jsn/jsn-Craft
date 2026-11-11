package com.chigusa.jsncraft.data;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.quest.PlayerQuestData;
import com.chigusa.jsncraft.stat.PlayerResources;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, JsnCraft.MODID);

    /** プレイヤーの職業データ。セーブされ、死んでも消えない。 */
    public static final Supplier<AttachmentType<PlayerJobData>> JOB_DATA = ATTACHMENT_TYPES.register(
            "job_data",
            () -> AttachmentType.builder(() -> new PlayerJobData())
                    .serialize(PlayerJobData.CODEC)
                    .copyOnDeath()
                    .build());

    /** 今のマナとスタミナ。セーブされる。死んだら満タンからやり直し。 */
    public static final Supplier<AttachmentType<PlayerResources>> RESOURCES = ATTACHMENT_TYPES.register(
            "resources",
            () -> AttachmentType.builder(() -> new PlayerResources())
                    .serialize(PlayerResources.CODEC)
                    .build());

    /** 依頼の受注・達成状況と、もらった肩書。死んでも消えない。 */
    public static final Supplier<AttachmentType<PlayerQuestData>> QUESTS = ATTACHMENT_TYPES.register(
            "quests",
            () -> AttachmentType.builder(() -> new PlayerQuestData())
                    .serialize(PlayerQuestData.CODEC)
                    .copyOnDeath()
                    .build());

    private ModAttachments() {}
}
