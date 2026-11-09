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
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 職業メニュー（ステータス＋スキルボード）に必要な情報をまとめて、サーバーから本人に送る。
 * open が true ならメニューを開く。false なら、開いているときだけ中身を新しくする。
 */
public record BoardSnapshotPayload(
        boolean open,
        List<StatLine> stats,
        Optional<BoardView> combat,
        Optional<BoardView> life
) implements CustomPacketPayload {

    public static final Type<BoardSnapshotPayload> TYPE = new Type<>(JsnCraft.id("board_snapshot"));

    /** ステータス画面の1行。 */
    public record StatLine(Component name, String value, String bonus) {
        public static final Codec<StatLine> CODEC = RecordCodecBuilder.create(i -> i.group(
                ComponentSerialization.CODEC.fieldOf("name").forGetter(StatLine::name),
                Codec.STRING.fieldOf("value").forGetter(StatLine::value),
                Codec.STRING.fieldOf("bonus").forGetter(StatLine::bonus)
        ).apply(i, StatLine::new));
    }

    /** スキルボードのマス1つ（表示用）。 */
    public record NodeView(String id, int x, int y, Component name, Optional<ResourceLocation> icon,
                           boolean start, int cost, List<String> links, List<Component> effects) {
        public static final Codec<NodeView> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(NodeView::id),
                Codec.INT.fieldOf("x").forGetter(NodeView::x),
                Codec.INT.fieldOf("y").forGetter(NodeView::y),
                ComponentSerialization.CODEC.fieldOf("name").forGetter(NodeView::name),
                ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(NodeView::icon),
                Codec.BOOL.fieldOf("start").forGetter(NodeView::start),
                Codec.INT.fieldOf("cost").forGetter(NodeView::cost),
                Codec.STRING.listOf().fieldOf("links").forGetter(NodeView::links),
                ComponentSerialization.CODEC.listOf().fieldOf("effects").forGetter(NodeView::effects)
        ).apply(i, NodeView::new));
    }

    /** 1つの職業のスキルボード（表示用）。 */
    public record BoardView(String type, Component jobName, int level, int points,
                            List<NodeView> nodes, List<String> unlocked) {
        public static final Codec<BoardView> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("type").forGetter(BoardView::type),
                ComponentSerialization.CODEC.fieldOf("job_name").forGetter(BoardView::jobName),
                Codec.INT.fieldOf("level").forGetter(BoardView::level),
                Codec.INT.fieldOf("points").forGetter(BoardView::points),
                NodeView.CODEC.listOf().fieldOf("nodes").forGetter(BoardView::nodes),
                Codec.STRING.listOf().fieldOf("unlocked").forGetter(BoardView::unlocked)
        ).apply(i, BoardView::new));

        public boolean isUnlocked(String id) {
            return unlocked.contains(id);
        }

        /** 今このマスを解放できるか（ポイントは別にチェック）。 */
        public boolean isReachable(NodeView node) {
            if (node.start()) return true;
            Set<String> done = new HashSet<>(unlocked);
            for (String link : node.links()) if (done.contains(link)) return true;
            for (NodeView other : nodes) {
                if (done.contains(other.id()) && other.links().contains(node.id())) return true;
            }
            return false;
        }
    }

    public static final Codec<BoardSnapshotPayload> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.fieldOf("open").forGetter(BoardSnapshotPayload::open),
            StatLine.CODEC.listOf().fieldOf("stats").forGetter(BoardSnapshotPayload::stats),
            BoardView.CODEC.optionalFieldOf("combat").forGetter(BoardSnapshotPayload::combat),
            BoardView.CODEC.optionalFieldOf("life").forGetter(BoardSnapshotPayload::life)
    ).apply(i, BoardSnapshotPayload::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BoardSnapshotPayload> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
