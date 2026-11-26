package com.chigusa.jsncraft.warp;

import com.chigusa.jsncraft.cooking.ModCooking;
import com.chigusa.jsncraft.npc.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;
import java.util.function.Supplier;

/**
 * ワープのモニュメント：OPが置くと3×3（高さ6）の全体ができて、その場所がワープ地点になる。
 * どこかを壊すと全体が消えて、ワープ地点の登録も消える。
 */
public final class Monuments {
    public static final int HEIGHT = 5; // 台 + 柱3 + 柱の上

    public static final DeferredBlock<WarpMonumentBlock> BLOCK = ModCooking.BLOCKS.register("warp_monument",
            () -> new WarpMonumentBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(-1.0F, 3600000.0F)
                    .sound(SoundType.STONE).lightLevel(s -> s.getValue(WarpMonumentBlock.PART) == MonumentPart.TOP ? 15 : 6)
                    .noLootTable().noOcclusion().pushReaction(PushReaction.BLOCK)));
    public static final Supplier<BlockEntityType<WarpMonumentBlockEntity>> BE = ModCooking.BLOCK_ENTITIES.register("warp_monument",
            () -> BlockEntityType.Builder.of(WarpMonumentBlockEntity::new, BLOCK.get()).build(null));
    public static final DeferredItem<MonumentItem> ITEM = ModEntities.ITEMS.register("warp_monument",
            () -> new MonumentItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(16)));
    /** 上に浮かぶクリスタルの見た目（持ち物には出ない） */
    public static final DeferredItem<Item> CRYSTAL = ModEntities.ITEMS.registerSimpleItem("warp_crystal");

    public static void init() {}

    private static boolean removing;

    /** 真ん中から見た全部の場所と部分 */
    private static void forEach(BlockPos c, PartConsumer f) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = c.offset(dx, 0, dz);
                if (dx == 0 && dz == 0) f.accept(p, MonumentPart.CENTER, Direction.NORTH);
                else if (dx == 0 || dz == 0) f.accept(p, MonumentPart.EDGE, dx < 0 ? Direction.WEST : dx > 0 ? Direction.EAST : dz < 0 ? Direction.NORTH : Direction.SOUTH);
                else f.accept(p, MonumentPart.CORNER, dx < 0 ? (dz < 0 ? Direction.NORTH : Direction.WEST) : (dz < 0 ? Direction.EAST : Direction.SOUTH));
            }
        }
        for (int y = 1; y < HEIGHT - 1; y++) f.accept(c.above(y), MonumentPart.PILLAR, Direction.NORTH);
        f.accept(c.above(HEIGHT - 1), MonumentPart.TOP, Direction.NORTH);
    }

    private interface PartConsumer {
        void accept(BlockPos pos, MonumentPart part, Direction facing);
    }

    public static boolean place(ServerLevel level, BlockPos center) {
        boolean[] ok = {true};
        forEach(center, (p, part, f) -> {
            if (!level.getBlockState(p).canBeReplaced()) ok[0] = false;
        });
        if (!ok[0]) return false;
        removing = true;
        forEach(center, (p, part, f) -> level.setBlock(p, BLOCK.get().defaultBlockState()
                .setValue(WarpMonumentBlock.PART, part).setValue(WarpMonumentBlock.FACING, f), 3));
        removing = false;
        return true;
    }

    /** 1マスでも壊れたら、全体を消して登録も消す */
    public static void onPartRemoved(Level level, BlockPos pos, BlockState state) {
        if (removing || !(level instanceof ServerLevel sl)) return;
        BlockPos center = findCenter(sl, pos, state);
        if (center == null) return;
        removing = true;
        forEach(center, (p, part, f) -> {
            if (level.getBlockState(p).is(BLOCK.get())) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        });
        removing = false;
        WarpData d = WarpData.get(sl.getServer());
        String dim = sl.dimension().location().toString();
        if (d.points.removeIf(pt -> pt.monument && pt.dim.equals(dim) && pt.mx == center.getX() && pt.my == center.getY() && pt.mz == center.getZ()))
            d.setDirty();
    }

    private static BlockPos findCenter(ServerLevel level, BlockPos pos, BlockState state) {
        MonumentPart part = state.getValue(WarpMonumentBlock.PART);
        if (part == MonumentPart.CENTER) return pos;
        if (part == MonumentPart.PILLAR || part == MonumentPart.TOP) {
            for (int y = 1; y < HEIGHT; y++) {
                BlockPos b = pos.below(y);
                BlockState s = level.getBlockState(b);
                if (s.is(BLOCK.get()) && s.getValue(WarpMonumentBlock.PART) == MonumentPart.CENTER) return b;
            }
            return pos.below(part == MonumentPart.TOP ? HEIGHT - 1 : 1);
        }
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos b = pos.offset(dx, 0, dz);
                BlockState s = level.getBlockState(b);
                if (s.is(BLOCK.get()) && s.getValue(WarpMonumentBlock.PART) == MonumentPart.CENTER) return b;
            }
        // 真ん中がもうない（真ん中から壊れた）ときは、ふちの向きから計算
        Direction f = state.getValue(WarpMonumentBlock.FACING);
        if (part == MonumentPart.EDGE) return pos.relative(f.getOpposite());
        return null;
    }

    /** ワープのモニュメント（置く用のアイテム） */
    public static class MonumentItem extends Item {
        public MonumentItem(Properties p) {
            super(p);
        }

        @Override
        public InteractionResult useOn(UseOnContext ctx) {
            Level level = ctx.getLevel();
            if (!(ctx.getPlayer() instanceof ServerPlayer p) || !(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
            if (!p.hasPermissions(2)) {
                p.displayClientMessage(Component.literal("ワープのモニュメントを置けるのはOPだけ").withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
            BlockPos center = ctx.getClickedPos().relative(ctx.getClickedFace());
            if (ctx.getClickedFace() != Direction.UP) center = ctx.getClickedPos().above();
            if (!place(sl, center)) {
                p.displayClientMessage(Component.literal("3×3、高さ5のすき間が必要だよ").withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
            ItemStack st = ctx.getItemInHand();
            WarpData d = WarpData.get(sl.getServer());
            String name = st.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME) ? st.getHoverName().getString() : null;
            if (name == null || name.isBlank()) {
                int n = 1;
                while (d.find("ワープ広場" + n) != null) n++;
                name = "ワープ広場" + n;
            }
            while (d.find(name) != null) name = name + "＋";
            // 着く場所：南のふちの外側、モニュメントの方を向いて
            Direction front = p.getDirection().getOpposite();
            BlockPos arrive = center.relative(front, 2);
            WarpData.Point pt = new WarpData.Point();
            pt.name = name;
            pt.dim = sl.dimension().location().toString();
            pt.x = arrive.getX() + 0.5;
            pt.y = arrive.getY();
            pt.z = arrive.getZ() + 0.5;
            pt.yaw = front.getOpposite().toYRot();
            pt.monument = true;
            pt.mx = center.getX();
            pt.my = center.getY();
            pt.mz = center.getZ();
            d.points.add(pt);
            d.setDirty();
            sl.playSound(null, center, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.2F);
            p.sendSystemMessage(Component.literal("ワープ地点「" + name + "」を作った（名前は /warp rename で変えられるよ）").withStyle(ChatFormatting.AQUA));
            if (!p.getAbilities().instabuild) st.shrink(1);
            return InteractionResult.CONSUME;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
            tip.add(Component.literal("OPだけ置ける。3×3・高さ5のモニュメントができて、ワープ地点になる").withStyle(ChatFormatting.GRAY));
            tip.add(Component.literal("金床で名前を付けてから置くと、その名前の地点になる").withStyle(ChatFormatting.DARK_AQUA));
            tip.add(Component.literal("着く場所は、置いた人が立っている側").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private Monuments() {}
}
