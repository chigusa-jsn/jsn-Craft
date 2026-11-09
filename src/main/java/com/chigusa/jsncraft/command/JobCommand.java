package com.chigusa.jsncraft.command;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.job.JobDefinition;
import com.chigusa.jsncraft.job.JobManager;
import com.chigusa.jsncraft.job.JobService;
import com.chigusa.jsncraft.job.JobType;
import com.chigusa.jsncraft.job.SkillBoardService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Map;

/**
 * /job          自分の職業を見る
 * /job list     職業の一覧
 * /job board    職業メニュー（ステータス・スキルボード）を開く。Kキーでも開ける
 * /job resetboard <life|combat>   スキルボードをリセット（OP用）
 * /job change <職業>   転職（例: /job change lumberjack）
 * /job title <プレイヤー> <肩書> | clear   肩書を付ける（OP用）
 * /job addxp <life|combat> <量>   経験値を足す（テスト用・OP用）
 */
public final class JobCommand {

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_JOBS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(
                    JobManager.INSTANCE.all().keySet().stream()
                            .map(id -> id.getNamespace().equals(JsnCraft.MODID) ? id.getPath() : id.toString()),
                    builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("job")
                .executes(JobCommand::info)
                .then(Commands.literal("info").executes(JobCommand::info))
                .then(Commands.literal("list").executes(JobCommand::list))
                .then(Commands.literal("board").executes(ctx -> {
                    SkillBoardService.sendSnapshot(ctx.getSource().getPlayerOrException(), true);
                    return 1;
                }))
                .then(Commands.literal("resetboard")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("life").executes(ctx -> resetBoard(ctx, JobType.LIFE)))
                        .then(Commands.literal("combat").executes(ctx -> resetBoard(ctx, JobType.COMBAT))))
                .then(Commands.literal("change")
                        .then(Commands.argument("job", ResourceLocationArgument.id())
                                .suggests(SUGGEST_JOBS)
                                .executes(JobCommand::change)))
                .then(Commands.literal("title")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.literal("clear")
                                        .executes(ctx -> setTitle(ctx, "")))
                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                        .executes(ctx -> setTitle(ctx, StringArgumentType.getString(ctx, "text"))))))
                .then(Commands.literal("addxp")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("life")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> addXp(ctx, JobType.LIFE))))
                        .then(Commands.literal("combat")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> addXp(ctx, JobType.COMBAT))))));
    }

    private static int info(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ctx.getSource().sendSuccess(() -> JobService.describe(player), false);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        MutableComponent out = Component.literal("=== 職業一覧 ===").withStyle(ChatFormatting.AQUA);
        for (Map.Entry<ResourceLocation, JobDefinition> e : JobManager.INSTANCE.all().entrySet()) {
            JobDefinition def = e.getValue();
            String shortId = e.getKey().getNamespace().equals(JsnCraft.MODID) ? e.getKey().getPath() : e.getKey().toString();
            out.append(Component.literal("\n[" + def.type().displayName() + "/" + def.tier().displayName() + "] ").withStyle(ChatFormatting.GRAY))
                    .append(def.name().copy())
                    .append(Component.literal("  " + shortId).withStyle(ChatFormatting.DARK_GRAY));
        }
        ctx.getSource().sendSuccess(() -> out, false);
        return 1;
    }

    private static int change(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ResourceLocation id = resolveJobId(ResourceLocationArgument.getId(ctx, "job"));
        Component error = JobService.changeJob(player, id);
        if (error != null) {
            ctx.getSource().sendFailure(error);
            return 0;
        }
        JobDefinition def = JobManager.INSTANCE.get(id);
        ctx.getSource().sendSuccess(() -> Component.literal(def.type().displayName() + "ジョブを ").withStyle(ChatFormatting.GREEN)
                .append(def.name().copy())
                .append(Component.literal(" に転職しました！").withStyle(ChatFormatting.GREEN)), false);
        return 1;
    }

    /** 「lumberjack」だけ打たれたら「jsn_craft:lumberjack」として探す。 */
    private static ResourceLocation resolveJobId(ResourceLocation typed) {
        if (JobManager.INSTANCE.get(typed) == null && typed.getNamespace().equals("minecraft")) {
            return JsnCraft.id(typed.getPath());
        }
        return typed;
    }

    private static int setTitle(CommandContext<CommandSourceStack> ctx, String title) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer p : targets) {
            JobService.data(p).setTitle(title);
            JobService.sync(p);
        }
        String msg = title.isEmpty() ? "肩書を外しました" : "肩書を《" + title + "》にしました";
        ctx.getSource().sendSuccess(() -> Component.literal(msg + "（" + targets.size() + "人）"), true);
        return targets.size();
    }

    private static int addXp(CommandContext<CommandSourceStack> ctx, JobType type) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ResourceLocation job = JobService.data(player).getJob(type);
        if (job == null) {
            ctx.getSource().sendFailure(Component.literal(type.displayName() + "ジョブに就いていません"));
            return 0;
        }
        JobService.addXp(player, job, IntegerArgumentType.getInteger(ctx, "amount"));
        return 1;
    }

    private static int resetBoard(CommandContext<CommandSourceStack> ctx, JobType type) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        SkillBoardService.reset(player, type);
        ctx.getSource().sendSuccess(() -> Component.literal(type.displayName() + "ジョブのスキルボードをリセットしました"), false);
        return 1;
    }

    private JobCommand() {}
}
