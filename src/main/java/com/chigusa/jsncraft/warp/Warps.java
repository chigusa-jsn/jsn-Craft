package com.chigusa.jsncraft.warp;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.casino.Money;
import com.chigusa.jsncraft.cooking.ModCooking;
import com.chigusa.jsncraft.npc.ModEntities;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ワープ地点：OPが /warp add で登録して、みんなはワープの石碑から選んで飛ぶ（ディメンションをまたいでもOK）。
 * 石碑はクラフトできない（クリエイティブ・OPだけ）。
 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class Warps {
    public static final DeferredBlock<WarpStoneBlock> STONE = ModCooking.BLOCKS.register("warp_stone",
            () -> new WarpStoneBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(-1.0F, 3600000.0F)
                    .sound(SoundType.STONE).lightLevel(s -> 12).noLootTable()));
    public static final DeferredItem<BlockItem> STONE_ITEM = ModEntities.ITEMS.register("warp_stone",
            () -> new BlockItem(STONE.get(), new Item.Properties().rarity(Rarity.EPIC)));

    private static final Map<UUID, Long> COOLDOWN = new HashMap<>();
    private static final int USE_RANGE = 6;

    public static void init() {}

    public static String dimName(String dim) {
        return switch (dim) {
            case "minecraft:overworld" -> "地上";
            case "minecraft:the_nether" -> "ネザー";
            case "minecraft:the_end" -> "エンド";
            default -> dim.contains(":") ? dim.substring(dim.indexOf(':') + 1) : dim;
        };
    }

    /** 近くにワープの石碑があるか */
    private static boolean nearStone(ServerPlayer p) {
        BlockPos c = p.blockPosition();
        for (BlockPos b : BlockPos.betweenClosed(c.offset(-USE_RANGE, -3, -USE_RANGE), c.offset(USE_RANGE, 3, USE_RANGE)))
            if (p.level().getBlockState(b).is(STONE.get())) return true;
        return false;
    }

    public static void openList(ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, WarpListPayload.of(p));
    }

    public static void go(ServerPlayer p, int index) {
        WarpData d = WarpData.get(p.server);
        if (index < 0 || index >= d.points.size()) return;
        boolean op = p.hasPermissions(2);
        if (!op && !nearStone(p)) {
            p.displayClientMessage(Component.literal("ワープの石碑の近くで使ってね").withStyle(ChatFormatting.RED), true);
            return;
        }
        long now = p.level().getGameTime();
        Long last = COOLDOWN.get(p.getUUID());
        if (last != null && now - last < 60 && !op) return;
        WarpData.Point pt = d.points.get(index);
        ResourceLocation dimId = ResourceLocation.tryParse(pt.dim);
        ServerLevel target = dimId == null ? null : p.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimId));
        if (target == null) {
            p.displayClientMessage(Component.literal("「" + pt.name + "」には今は行けません（ディメンションが見つからない）").withStyle(ChatFormatting.RED), false);
            return;
        }
        if (pt.cost > 0 && !op && !p.getAbilities().instabuild) {
            if (!Money.pay(p, pt.cost)) {
                p.displayClientMessage(Component.literal("お金が足りない（" + pt.cost + " 必要）").withStyle(ChatFormatting.RED), true);
                return;
            }
        }
        COOLDOWN.put(p.getUUID(), now);
        ServerLevel from = p.serverLevel();
        from.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 0.8, 0.4, 0.5);
        from.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.0F);
        p.stopRiding();
        p.teleportTo(target, pt.x, pt.y, pt.z, pt.yaw, p.getXRot());
        p.fallDistance = 0;
        target.sendParticles(ParticleTypes.REVERSE_PORTAL, pt.x, pt.y + 1, pt.z, 40, 0.4, 0.8, 0.4, 0.2);
        target.playSound(null, BlockPos.containing(pt.x, pt.y, pt.z), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.2F);
        p.displayClientMessage(Component.literal("「" + pt.name + "」へワープした").withStyle(ChatFormatting.AQUA), true);
    }

    // ================= コマンド（OPだけ。/warp だけなら誰でも一覧…ではなく、OPはどこでも開ける） =================

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static int msg(CommandSourceStack s, String text) {
        s.sendSuccess(() -> Component.literal(text), true);
        return 1;
    }

    private static void register(CommandDispatcher<CommandSourceStack> d) {
        var name = Commands.argument("name", StringArgumentType.string())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(WarpData.get(ctx.getSource().getServer()).points.stream()
                        .map(pt -> StringArgumentType.escapeIfRequired(pt.name)), b));
        d.register(Commands.literal("warp").requires(s -> s.hasPermission(2))
                .executes(ctx -> {
                    openList(ctx.getSource().getPlayerOrException());
                    return 1;
                })
                .then(Commands.literal("add").then(Commands.argument("name", StringArgumentType.string()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    String n = StringArgumentType.getString(ctx, "name");
                    WarpData data = WarpData.get(p.server);
                    WarpData.Point pt = data.find(n);
                    boolean isNew = pt == null;
                    if (isNew) {
                        pt = new WarpData.Point();
                        pt.name = n;
                        data.points.add(pt);
                    }
                    pt.dim = p.level().dimension().location().toString();
                    pt.x = p.getX();
                    pt.y = p.getY();
                    pt.z = p.getZ();
                    pt.yaw = p.getYRot();
                    data.setDirty();
                    return msg(ctx.getSource(), (isNew ? "ワープ地点「" : "ワープ地点「") + n + "」を" + (isNew ? "登録した" : "今の場所に動かした") + "（" + dimName(pt.dim) + "）");
                })))
                .then(Commands.literal("remove").then(name.executes(ctx -> {
                    String n = StringArgumentType.getString(ctx, "name");
                    WarpData data = WarpData.get(ctx.getSource().getServer());
                    if (!data.points.removeIf(pt -> pt.name.equals(n))) return msg(ctx.getSource(), "「" + n + "」は見つからない");
                    data.setDirty();
                    return msg(ctx.getSource(), "ワープ地点「" + n + "」を消した");
                })))
                .then(Commands.literal("cost").then(Commands.argument("name", StringArgumentType.string())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(WarpData.get(ctx.getSource().getServer()).points.stream()
                                .map(pt -> StringArgumentType.escapeIfRequired(pt.name)), b))
                        .then(Commands.argument("money", IntegerArgumentType.integer(0, 100000)).executes(ctx -> {
                            String n = StringArgumentType.getString(ctx, "name");
                            WarpData data = WarpData.get(ctx.getSource().getServer());
                            WarpData.Point pt = data.find(n);
                            if (pt == null) return msg(ctx.getSource(), "「" + n + "」は見つからない");
                            pt.cost = IntegerArgumentType.getInteger(ctx, "money");
                            data.setDirty();
                            return msg(ctx.getSource(), "「" + n + "」の使用料を " + pt.cost + " にした" + (pt.cost == 0 ? "（無料）" : ""));
                        }))))
                .then(Commands.literal("up").then(name.executes(ctx -> move(ctx.getSource(), StringArgumentType.getString(ctx, "name"), -1))))
                .then(Commands.literal("down").then(Commands.argument("name", StringArgumentType.string())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(WarpData.get(ctx.getSource().getServer()).points.stream()
                                .map(pt -> StringArgumentType.escapeIfRequired(pt.name)), b))
                        .executes(ctx -> move(ctx.getSource(), StringArgumentType.getString(ctx, "name"), 1))))
                .then(Commands.literal("list").executes(ctx -> {
                    WarpData data = WarpData.get(ctx.getSource().getServer());
                    if (data.points.isEmpty()) return msg(ctx.getSource(), "ワープ地点はまだない（/warp add 名前）");
                    for (WarpData.Point pt : data.points)
                        msg(ctx.getSource(), "・" + pt.name + "  " + dimName(pt.dim) + " (" + (int) pt.x + ", " + (int) pt.y + ", " + (int) pt.z + ")" + (pt.cost > 0 ? "  使用料 " + pt.cost : ""));
                    return 1;
                })));
    }

    /** 一覧の並び順を動かす */
    private static int move(CommandSourceStack s, String n, int dir) {
        WarpData data = WarpData.get(s.getServer());
        WarpData.Point pt = data.find(n);
        if (pt == null) return msg(s, "「" + n + "」は見つからない");
        int i = data.points.indexOf(pt), j = i + dir;
        if (j < 0 || j >= data.points.size()) return msg(s, "これ以上動かせない");
        data.points.set(i, data.points.get(j));
        data.points.set(j, pt);
        data.setDirty();
        return msg(s, "「" + n + "」を" + (dir < 0 ? "上" : "下") + "へ動かした");
    }

    private Warps() {}
}
