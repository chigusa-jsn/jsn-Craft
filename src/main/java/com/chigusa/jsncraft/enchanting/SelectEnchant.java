package com.chigusa.jsncraft.enchanting;

import com.chigusa.jsncraft.cooking.ModCooking;
import com.chigusa.jsncraft.job.JobService;
import com.chigusa.jsncraft.npc.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * 選べる付与台：エンチャントテーブルに出るエンチャント（宝・呪いは除く）から、好きなものとレベルを選んで付ける。
 * 付けられるレベルの上限は付与の腕で決まる。費用は経験値レベルとラピスラズリ。
 */
public final class SelectEnchant {
    public static final DeferredBlock<SelectTableBlock> TABLE = ModCooking.BLOCKS.register("select_enchant_table",
            () -> new SelectTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(5.0F, 1200.0F)
                    .sound(SoundType.STONE).lightLevel(s -> 7).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> TABLE_ITEM = ModEntities.ITEMS.registerSimpleBlockItem(TABLE);
    public static final Supplier<BlockEntityType<SelectTableBlockEntity>> BE = ModCooking.BLOCK_ENTITIES.register("select_enchant_table",
            () -> BlockEntityType.Builder.of(SelectTableBlockEntity::new, TABLE.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<SelectTableMenu>> MENU = ModCooking.MENUS.register("select_enchant_table",
            () -> IMenuTypeExtension.create(SelectTableMenu::new));

    public static void init() {}

    /** 付けられるレベルの上限：付与術師 Lv0〜 で1、Lv15 で2、Lv30 で3、Lv45 で4、刻印師で5 */
    public static int maxLevel(int lv) {
        if (lv < 0) return 0;
        if (lv >= 50) return 5;
        return 1 + lv / 15;
    }

    /** 経験値レベルの費用 */
    public static int xpCost(int level, boolean master) {
        int c = 3 + level * 4;
        return master ? Math.max(1, c * 3 / 4) : c;
    }

    /** ラピスの費用 */
    public static int lapisCost(int level) {
        return level;
    }

    /** 選べるエンチャントの一覧（決まった順。サーバーと画面で同じになる） */
    public static List<Holder<Enchantment>> candidates(HolderLookup.Provider reg, ItemStack stack) {
        List<Holder<Enchantment>> out = new ArrayList<>();
        if (stack.isEmpty()) return out;
        boolean book = stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK);
        var lookup = reg.lookupOrThrow(Registries.ENCHANTMENT);
        lookup.get(EnchantmentTags.IN_ENCHANTING_TABLE).ifPresent(set -> {
            for (Holder<Enchantment> h : set) {
                if (h.is(EnchantmentTags.CURSE)) continue;
                if (book || h.value().isSupportedItem(stack)) out.add(h);
            }
        });
        out.sort(Comparator.comparing(h -> h.unwrapKey().map(k -> k.location().toString()).orElse("")));
        return out;
    }

    /** 付けられない理由（null なら付けられる） */
    public static String problem(ItemStack stack, Holder<Enchantment> ench, int level, int jobLv) {
        if (jobLv < 0) return "付けられるのは付与術師・刻印師だけ";
        if (level < 1 || level > ench.value().getMaxLevel()) return "そのレベルはない";
        if (level > maxLevel(jobLv)) return "付与の腕が足りない（今は " + maxLevel(jobLv) + " まで）";
        ItemEnchantments cur = stack.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (stack.is(Items.ENCHANTED_BOOK)) cur = stack.getOrDefault(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        int have = cur.getLevel(ench);
        if (have >= level) return "もう同じかそれ以上が付いている";
        for (Holder<Enchantment> h : cur.keySet()) {
            if (!h.equals(ench) && !Enchantment.areCompatible(h, ench)) return "「" + h.value().description().getString() + "」と一緒に付けられない";
        }
        return null;
    }

    public static boolean apply(ServerPlayer p, SelectTableBlockEntity be, int index, int level) {
        ItemStack stack = be.items().getItem(0);
        List<Holder<Enchantment>> list = candidates(p.registryAccess(), stack);
        if (index < 0 || index >= list.size()) return false;
        Holder<Enchantment> ench = list.get(index);
        int lv = EnchantingService.enchantLevel(p);
        String why = problem(stack, ench, level, lv);
        boolean master = EnchantingService.isMaster(p);
        int xp = xpCost(level, master), lapis = lapisCost(level);
        if (why == null && !p.getAbilities().instabuild) {
            if (p.experienceLevel < xp) why = "経験値レベルが足りない（" + xp + " 必要）";
            else if (!be.items().getItem(1).is(Items.LAPIS_LAZULI) || be.items().getItem(1).getCount() < lapis) why = "ラピスラズリが足りない（" + lapis + " 個必要）";
        }
        if (why != null) {
            p.displayClientMessage(Component.literal(why).withStyle(ChatFormatting.RED), true);
            return false;
        }
        var thrift = com.chigusa.jsncraft.skill.SkillService.passive(p, "upper_rune_thrift");
        boolean free = thrift != null && p.getRandom().nextDouble() < thrift.chance();
        if (!p.getAbilities().instabuild && !free) {
            p.giveExperienceLevels(-xp);
            be.items().getItem(1).shrink(lapis);
        }
        if (free) p.displayClientMessage(Component.literal("神紋の節約：経験値とラピスを使わなかった！").withStyle(ChatFormatting.AQUA), false);
        if (stack.is(Items.BOOK)) {
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            book.enchant(ench, level);
            be.items().setItem(0, book);
        } else if (stack.is(Items.ENCHANTED_BOOK)) {
            EnchantmentHelper.updateEnchantments(stack, m -> m.set(ench, level));
        } else {
            EnchantmentHelper.updateEnchantments(stack, m -> m.set(ench, level));
        }
        be.items().setChanged();
        p.serverLevel().playSound(null, be.getBlockPos(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        p.displayClientMessage(Component.literal("「").append(Enchantment.getFullname(ench, level)).append("」を付けた！").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        JobService.onAction(p, rule -> rule.action().equals("enchant"), 4 + 3 * level);
        return true;
    }

    private SelectEnchant() {}
}
