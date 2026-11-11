package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.JsnCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** サーバー → 本人：依頼タブに出す一覧（受注中＋受けられる依頼）。 */
public record QuestListPayload(List<QuestView> quests, int activeCount, int maxActive) implements CustomPacketPayload {

    public static final Type<QuestListPayload> TYPE = new Type<>(JsnCraft.id("quest_list"));

    public record ObjectiveView(Component label, int progress, int count) {
        public static final Codec<ObjectiveView> CODEC = RecordCodecBuilder.create(i -> i.group(
                ComponentSerialization.CODEC.fieldOf("label").forGetter(ObjectiveView::label),
                Codec.INT.fieldOf("progress").forGetter(ObjectiveView::progress),
                Codec.INT.fieldOf("count").forGetter(ObjectiveView::count)
        ).apply(i, ObjectiveView::new));

        public boolean done() {
            return progress >= count;
        }
    }

    public record QuestView(String id, Component title, Component description, String category, boolean active,
                            boolean canTurnIn, String period, List<ObjectiveView> objectives, List<Component> rewards) {
        public static final Codec<QuestView> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(QuestView::id),
                ComponentSerialization.CODEC.fieldOf("title").forGetter(QuestView::title),
                ComponentSerialization.CODEC.fieldOf("description").forGetter(QuestView::description),
                Codec.STRING.fieldOf("category").forGetter(QuestView::category),
                Codec.BOOL.fieldOf("active").forGetter(QuestView::active),
                Codec.BOOL.fieldOf("can_turn_in").forGetter(QuestView::canTurnIn),
                Codec.STRING.fieldOf("period").forGetter(QuestView::period),
                ObjectiveView.CODEC.listOf().fieldOf("objectives").forGetter(QuestView::objectives),
                ComponentSerialization.CODEC.listOf().fieldOf("rewards").forGetter(QuestView::rewards)
        ).apply(i, QuestView::new));
    }

    public static final Codec<QuestListPayload> CODEC = RecordCodecBuilder.create(i -> i.group(
            QuestView.CODEC.listOf().fieldOf("quests").forGetter(QuestListPayload::quests),
            Codec.INT.fieldOf("active_count").forGetter(QuestListPayload::activeCount),
            Codec.INT.fieldOf("max_active").forGetter(QuestListPayload::maxActive)
    ).apply(i, QuestListPayload::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestListPayload> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    /** クライアントが覚えている最新の一覧 */
    public static volatile QuestListPayload client = new QuestListPayload(List.of(), 0, 5);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
