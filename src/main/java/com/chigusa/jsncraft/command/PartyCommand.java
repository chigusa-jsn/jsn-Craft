package com.chigusa.jsncraft.command;

import com.chigusa.jsncraft.party.PartyManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /party invite <プレイヤー>   誘う（パーティーがなければ自分がリーダーで作る）
 * /party accept / decline      誘いに答える（チャットのボタンでもOK）
 * /party leave                 抜ける
 * /party kick <プレイヤー>     外す（リーダーだけ）
 * /party                       メンバーを見る
 */
public final class PartyCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("party")
                .executes(PartyCommand::list)
                .then(Commands.literal("list").executes(PartyCommand::list))
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> result(ctx, PartyManager.invite(ctx.getSource().getPlayerOrException(),
                                        EntityArgument.getPlayer(ctx, "player"))))))
                .then(Commands.literal("accept")
                        .executes(ctx -> result(ctx, PartyManager.accept(ctx.getSource().getPlayerOrException()))))
                .then(Commands.literal("decline").executes(ctx -> {
                    PartyManager.decline(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("leave").executes(ctx -> {
                    PartyManager.leave(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("kick")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> result(ctx, PartyManager.kick(ctx.getSource().getPlayerOrException(),
                                        EntityArgument.getPlayer(ctx, "player")))))));
    }

    private static int result(CommandContext<CommandSourceStack> ctx, Component error) {
        if (error != null) {
            ctx.getSource().sendFailure(error);
            return 0;
        }
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PartyManager.Party party = PartyManager.partyOf(player);
        if (party == null) {
            ctx.getSource().sendSuccess(() -> Component.literal("パーティーに入っていません（/party invite 名前 で誘えます）")
                    .withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        String names = String.join("  ", PartyManager.memberNames(party, ctx.getSource().getServer()));
        ctx.getSource().sendSuccess(() -> Component.literal("パーティー（" + party.members.size() + "/" + PartyManager.MAX_MEMBERS + "）: ")
                .withStyle(ChatFormatting.AQUA).append(Component.literal(names).withStyle(ChatFormatting.WHITE)), false);
        return 1;
    }

    private PartyCommand() {}
}
