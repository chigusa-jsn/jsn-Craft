package com.chigusa.jsncraft.command;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.quest.CustomQuestStore;
import com.chigusa.jsncraft.quest.QuestDefinition;
import com.chigusa.jsncraft.quest.QuestEditorService;
import com.chigusa.jsncraft.quest.QuestService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * ゲーム内で作る依頼の管理（OP用）。
 * /quest new            依頼エディターを開く（新規）
 * /quest list           ゲーム内で作った依頼の一覧（[編集] [停止] [削除] をクリックできる）
 * /quest edit <ID>      編集
 * /quest toggle <ID>    一時停止 ⇔ 再開
 * /quest delete <ID>    削除
 */
public final class QuestCommand {

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_CUSTOM = (ctx, builder) -> {
        CustomQuestStore store = CustomQuestStore.get();
        return SharedSuggestionProvider.suggest(
                store == null ? java.util.stream.Stream.empty() : store.all().keySet().stream().map(ResourceLocation::getPath),
                builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("quest")
                .requires(s -> s.hasPermission(QuestEditorService.PERMISSION))
                .then(Commands.literal("new").executes(ctx -> {
                    QuestEditorService.open(ctx.getSource().getPlayerOrException(), null);
                    return 1;
                }))
                .then(Commands.literal("list").executes(QuestCommand::list))
                .then(Commands.literal("edit")
                        .then(Commands.argument("id", StringArgumentType.word()).suggests(SUGGEST_CUSTOM)
                                .executes(ctx -> {
                                    ResourceLocation id = idArg(ctx);
                                    if (!exists(ctx, id)) return 0;
                                    QuestEditorService.open(ctx.getSource().getPlayerOrException(), id);
                                    return 1;
                                })))
                .then(Commands.literal("toggle")
                        .then(Commands.argument("id", StringArgumentType.word()).suggests(SUGGEST_CUSTOM)
                                .executes(QuestCommand::toggle)))
                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.word()).suggests(SUGGEST_CUSTOM)
                                .executes(QuestCommand::delete))));
    }

    private static ResourceLocation idArg(CommandContext<CommandSourceStack> ctx) {
        return JsnCraft.id(StringArgumentType.getString(ctx, "id"));
    }

    private static boolean exists(CommandContext<CommandSourceStack> ctx, ResourceLocation id) {
        CustomQuestStore store = CustomQuestStore.get();
        if (store == null || store.find(id) == null) {
            ctx.getSource().sendFailure(Component.literal("その依頼はありません: " + id.getPath()));
            return false;
        }
        return true;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        CustomQuestStore store = CustomQuestStore.get();
        if (store == null || store.all().isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("ゲーム内で作った依頼はまだありません（/quest new で作れます）")
                    .withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        MutableComponent out = Component.literal("=== ゲーム内で作った依頼 ===").withStyle(ChatFormatting.AQUA);
        store.all().forEach((id, def) -> {
            boolean off = store.isDisabled(id);
            String path = id.getPath();
            out.append(Component.literal("\n" + path + " ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(def.title().copy().withStyle(off ? ChatFormatting.GRAY : ChatFormatting.WHITE))
                    .append(Component.literal(off ? " (停止中) " : " ").withStyle(ChatFormatting.GRAY))
                    .append(button("[編集]", "/quest edit " + path, ChatFormatting.GREEN))
                    .append(" ")
                    .append(button(off ? "[再開]" : "[停止]", "/quest toggle " + path, ChatFormatting.YELLOW))
                    .append(" ")
                    .append(suggestButton("[削除]", "/quest delete " + path));
        });
        ctx.getSource().sendSuccess(() -> out, false);
        return 1;
    }

    private static Component button(String label, String command, ChatFormatting color) {
        return Component.literal(label).withStyle(s -> s.withColor(color)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(command))));
    }

    /** 削除はうっかり防止のため、入力欄にコマンドを入れるだけ（Enterで実行） */
    private static Component suggestButton(String label, String command) {
        return Component.literal(label).withStyle(s -> s.withColor(ChatFormatting.RED)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("クリックして Enter で削除"))));
    }

    private static int toggle(CommandContext<CommandSourceStack> ctx) {
        ResourceLocation id = idArg(ctx);
        if (!exists(ctx, id)) return 0;
        boolean off = CustomQuestStore.get().toggle(id);
        ctx.getSource().sendSuccess(() -> Component.literal(id.getPath() + " を" + (off ? "一時停止しました" : "再開しました")), true);
        refreshAll(ctx);
        return 1;
    }

    private static int delete(CommandContext<CommandSourceStack> ctx) {
        ResourceLocation id = idArg(ctx);
        if (!exists(ctx, id)) return 0;
        QuestDefinition def = CustomQuestStore.get().find(id);
        CustomQuestStore.get().remove(id);
        ctx.getSource().sendSuccess(() -> Component.literal("依頼「").append(def.title().copy()).append("」を削除しました"), true);
        refreshAll(ctx);
        return 1;
    }

    private static void refreshAll(CommandContext<CommandSourceStack> ctx) {
        for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) QuestService.sendList(p);
    }

    private QuestCommand() {}
}
