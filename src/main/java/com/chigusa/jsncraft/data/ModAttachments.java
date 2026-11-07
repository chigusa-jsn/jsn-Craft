package com.chigusa.jsncraft.data;

import com.chigusa.jsncraft.JsnCraft;
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

    private ModAttachments() {}
}
