package com.chigusa.jsncraft.gambler;

import com.chigusa.jsncraft.casino.VideoPoker;
import com.chigusa.jsncraft.magic.MageSpells;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillEffects;
import com.chigusa.jsncraft.vfx.Vfx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * ギャンブラー（賽振りの博徒・天運の大勝負師）の技。勝負札（カード）を持って使う。
 * どの技も「運」で強さがゆれる。結果は画面の上に出す。
 */
public final class GamblerSkills {

    static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.82F, 0.25F), 1.3F),
            RED = new DustParticleOptions(new Vector3f(0.95F, 0.15F, 0.2F), 1.2F),
            BLACK = new DustParticleOptions(new Vector3f(0.12F, 0.1F, 0.15F), 1.2F),
            WHITE = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 1.0F);
    /** 一時的な効果の名前（persistentData） */
    public static final String COIN_UNTIL = "jsn_gamble_coin", ALLIN_UNTIL = "jsn_gamble_allin", ALLIN_POW = "jsn_gamble_allin_pow",
            DOUBLE_UNTIL = "jsn_gamble_double", HOUSE_UNTIL = "jsn_gamble_house", HOUSE_POW = "jsn_gamble_house_pow";

    public static boolean cast(ServerPlayer p, SkillDefinition s) {
        return switch (s.effect()) {
            case "gamble_card_throw" -> cardThrow(p, s);
            case "gamble_coin_toss" -> coinToss(p, s);
            case "gamble_dice_shot" -> diceShot(p, s);
            case "gamble_chip_guard" -> chipGuard(p, s);
            case "gamble_roulette" -> roulette(p, s);
            case "gamble_double_up" -> doubleUp(p, s);
            case "gamble_poker_hand" -> pokerHand(p, s, false);
            case "gamble_all_in" -> allIn(p, s);
            case "gamble_slot_burst" -> slotBurst(p, s);
            case "gamble_jackpot_rain" -> jackpotRain(p, s);
            case "gamble_fate_dice" -> fateDice(p, s);
            case "gamble_house_rules" -> houseRules(p, s);
            case "gamble_royal_flush" -> pokerHand(p, s, true);
            case "gamble_seven_heaven" -> sevenHeaven(p, s);
            default -> false;
        };
    }

    static float atk(ServerPlayer p) {
        return (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    static void say(ServerPlayer p, String msg) {
        p.displayClientMessage(Component.literal(msg), true);
    }

    static List<LivingEntity> enemiesAround(ServerPlayer p, Vec3 c, double r) {
        return p.level().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r), e -> SkillEffects.isEnemy(p, e) && e.position().distanceTo(c) <= r);
    }

    static void burst(ServerLevel level, Vec3 c, DustParticleOptions dust, int n, double spread) {
        level.sendParticles(dust, c.x, c.y, c.z, n, spread, spread * 0.6, spread, 0.02);
    }

    // ================= カード投げ：扇形に3枚 =================
    private static boolean cardThrow(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getLookAngle();
        int n = 3 + Math.max(0, s.amplifier());
        float dmg = atk(p) * (float) s.power();
        for (int i = 0; i < n; i++) {
            double yaw = Math.toRadians((i - (n - 1) / 2.0) * 9);
            Vec3 dir = new Vec3(look.x * Math.cos(yaw) - look.z * Math.sin(yaw), look.y, look.x * Math.sin(yaw) + look.z * Math.cos(yaw)).normalize();
            DustParticleOptions col = i % 2 == 0 ? RED : BLACK;
            MageSpells.shoot(p, p.getEyePosition().add(dir.scale(0.6)).add(0, -0.15, 0), dir.scale(1.4), (int) (s.radius() / 1.4) + 1, 0, 0.05, col,
                    e -> {
                        SkillEffects.hit(p, e, dmg);
                        level.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY(0.6), e.getZ(), 6, 0.2, 0.2, 0.2, 0.2);
                    }, end -> {});
        }
        level.playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.8F);
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6F, 1.7F);
        return true;
    }

    // ================= コイントス：表なら攻撃アップ、裏なら回復 =================
    private static boolean coinToss(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        boolean heads = luckyRoll(p, 0.5);
        Vec3 c = p.position().add(0, 2.2, 0);
        burst(level, c, GOLD, 16, 0.2);
        level.playSound(null, p.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 2.0F);
        if (heads) {
            p.getPersistentData().putLong(COIN_UNTIL, level.getGameTime() + Math.round(s.duration() * 20));
            p.getPersistentData().putDouble(COIN_UNTIL + "_pow", s.power());
            say(p, "§6§l表！ §fしばらく攻撃 +" + Math.round(s.power() * 100) + "%");
            level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.6F);
        } else {
            p.heal((float) s.bonus());
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, (int) (s.duration() * 20), 1));
            say(p, "§b§l裏！ §f体力 +" + (int) s.bonus() + "・足が速くなる");
            level.sendParticles(ParticleTypes.HEART, p.getX(), p.getY(1.2), p.getZ(), 4, 0.4, 0.3, 0.4, 0);
        }
        return true;
    }

    /** 運のよさ（幸運）で少し当たりやすくなる */
    static boolean luckyRoll(ServerPlayer p, double base) {
        double luck = p.getAttributeValue(Attributes.LUCK);
        return p.getRandom().nextDouble() < Math.min(0.9, base + luck * 0.03);
    }

    static int die(ServerPlayer p) {
        int a = 1 + p.getRandom().nextInt(6);
        // 幸運が高いと、ふり直して良いほうをとることがある
        if (p.getRandom().nextDouble() < p.getAttributeValue(Attributes.LUCK) * 0.04) a = Math.max(a, 1 + p.getRandom().nextInt(6));
        return a;
    }

    // ================= ダイス撃ち：出た目でダメージ。6なら動けなくする =================
    private static boolean diceShot(ServerPlayer p, SkillDefinition s) {
        LivingEntity t = MageSpells.lookTarget(p, s.radius());
        if (t == null) return false;
        ServerLevel level = p.serverLevel();
        int d = die(p);
        Vec3 from = p.getEyePosition(), to = t.getBoundingBox().getCenter();
        for (int i = 0; i <= 10; i++) {
            Vec3 q = from.lerp(to, i / 10.0).add(0, Math.sin(i / 10.0 * Math.PI) * 0.5, 0);
            level.sendParticles(WHITE, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
        SkillEffects.hit(p, t, atk(p) * (float) s.power() * d);
        level.sendParticles(d == 6 ? GOLD : WHITE, to.x, to.y, to.z, 6 + d * 3, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, t.blockPosition(), SoundEvents.BAMBOO_WOOD_HIT, SoundSource.PLAYERS, 1.0F, 1.0F + d * 0.1F);
        if (d == 6) {
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 6));
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 1));
            level.playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
        say(p, (d == 6 ? "§6§l" : d == 1 ? "§7" : "§f") + "⚀ " + d + " の目！" + (d == 6 ? " 敵が動けない！" : ""));
        return true;
    }

    // ================= チップの盾：ふった数だけ守りのハート =================
    private static boolean chipGuard(ServerPlayer p, SkillDefinition s) {
        int a = die(p), b = die(p);
        int hearts = a + b;
        p.removeEffect(MobEffects.ABSORPTION);
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, (int) (s.duration() * 20), Math.max(0, hearts / 2 - 1 + s.amplifier())));
        ServerLevel level = p.serverLevel();
        for (int i = 0; i < 12; i++) {
            double ang = i * Math.PI / 6;
            level.sendParticles(i % 3 == 0 ? RED : i % 3 == 1 ? GOLD : WHITE, p.getX() + Math.cos(ang) * 0.9, p.getY(0.6), p.getZ() + Math.sin(ang) * 0.9, 2, 0.05, 0.3, 0.05, 0);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 1.4F);
        say(p, "§e⚂ " + a + "＋" + b + " ＝ " + hearts + "  §fチップの盾！");
        return true;
    }

    // ================= ルーレット：6つの結果からひとつ =================
    private static boolean roulette(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        RandomSource r = p.getRandom();
        int n = r.nextInt(37);
        if (n != 0 && luckyRoll(p, 0.0) && r.nextInt(8) == 0) n = 0;
        Vec3 c = p.position();
        double rad = s.radius();
        float dmg = atk(p) * (float) s.power();
        Vfx.ring(level, c, (float) rad, 14, n == 0 ? Vfx.GOLD : isRed(n) ? 0xFF3030 : 0x303030);
        level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 1.0F, 2.0F);
        if (n == 0) {
            say(p, "§a§l0 ！ 大当たり！ §fまわりに大ダメージ");
            for (LivingEntity e : enemiesAround(p, c, rad)) SkillEffects.hit(p, e, dmg * 3);
            burst(level, c.add(0, 1, 0), GOLD, 60, rad * 0.5);
            level.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.2F);
        } else if (isRed(n)) {
            say(p, "§c§l赤の " + n + "！ §fまわりを燃やす");
            for (LivingEntity e : enemiesAround(p, c, rad)) {
                SkillEffects.hit(p, e, dmg);
                e.igniteForSeconds(4);
            }
            burst(level, c.add(0, 1, 0), RED, 40, rad * 0.5);
            level.sendParticles(ParticleTypes.FLAME, c.x, c.y + 0.5, c.z, 30, rad * 0.4, 0.3, rad * 0.4, 0.02);
        } else {
            say(p, "§8§l黒の " + n + "！ §fまわりを弱らせる");
            for (LivingEntity e : enemiesAround(p, c, rad)) {
                SkillEffects.hit(p, e, dmg);
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            }
            burst(level, c.add(0, 1, 0), BLACK, 40, rad * 0.5);
        }
        if (n != 0 && n % 2 == 0) {
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0));
            p.displayClientMessage(Component.literal("§f偶数ボーナス：少し力が出る"), false);
        } else if (n % 2 == 1) {
            p.heal(3);
        }
        return true;
    }

    static boolean isRed(int n) {
        return com.chigusa.jsncraft.casino.Roulette.isRed(n);
    }

    // ================= ダブルアップ：次の一撃が 2倍か、半分か =================
    private static boolean doubleUp(ServerPlayer p, SkillDefinition s) {
        p.getPersistentData().putLong(DOUBLE_UNTIL, p.level().getGameTime() + Math.round(s.duration() * 20));
        p.getPersistentData().putDouble(DOUBLE_UNTIL + "_chance", Math.min(0.85, s.chance()));
        burst(p.serverLevel(), p.position().add(0, 1.2, 0), GOLD, 20, 0.4);
        p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.0F, 1.5F);
        say(p, "§e§lダブルアップ！ §f次の一撃は…？");
        return true;
    }

    // ================= ポーカーハンド：5枚引いて、役で強さが決まる =================
    private static final double[] HAND_POWER = {0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 5.0, 7.0, 10.0};

    private static boolean pokerHand(ServerPlayer p, SkillDefinition s, boolean royal) {
        LivingEntity t = MageSpells.lookTarget(p, s.radius() + 8);
        Vec3 c = t != null ? t.position() : p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(4));
        ServerLevel level = p.serverLevel();
        RandomSource r = p.getRandom();
        int[] hand = new int[5];
        int rank;
        if (royal) {
            int suit = r.nextInt(4);
            hand = new int[]{suit * 13, suit * 13 + 9, suit * 13 + 10, suit * 13 + 11, suit * 13 + 12};
            rank = 9;
        } else {
            java.util.List<Integer> deck = new java.util.ArrayList<>();
            for (int i = 0; i < 52; i++) deck.add(i);
            java.util.Collections.shuffle(deck, new java.util.Random(r.nextLong()));
            for (int i = 0; i < 5; i++) hand[i] = deck.get(i);
            rank = VideoPoker.rank(hand);
            // 幸運：1回だけ引き直しのチャンス
            if (rank < 2 && luckyRoll(p, 0.15)) {
                for (int i = 0; i < 5; i++) hand[i] = deck.get(5 + i);
                rank = Math.max(rank, VideoPoker.rank(hand));
            }
        }
        float dmg = atk(p) * (float) (s.power() * HAND_POWER[rank]);
        double rad = s.radius() * (rank >= 7 ? 1.5 : 1);
        for (LivingEntity e : enemiesAround(p, c, rad)) SkillEffects.hit(p, e, dmg);
        // 見た目：カードが5枚まわる
        for (int i = 0; i < 5; i++) {
            double a = i * Math.PI * 2 / 5;
            Vec3 q = c.add(Math.cos(a) * 1.5, 1.5, Math.sin(a) * 1.5);
            level.sendParticles(hand[i] / 13 == 1 || hand[i] / 13 == 2 ? RED : BLACK, q.x, q.y, q.z, 8, 0.1, 0.2, 0.05, 0);
            level.sendParticles(WHITE, q.x, q.y, q.z, 4, 0.12, 0.2, 0.05, 0);
        }
        if (rank >= 4) {
            Vfx.magicCircle(level, c, (float) rad, 20, Vfx.GOLD);
            level.sendParticles(ParticleTypes.FIREWORK, c.x, c.y + 1, c.z, 40 + rank * 10, rad * 0.4, 1, rad * 0.4, 0.1);
        }
        level.playSound(null, BlockPosOf(c), rank >= 7 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.2F);
        if (rank >= 2) level.playSound(null, BlockPosOf(c), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.4F + rank * 0.05F, 1.4F);
        say(p, handText(hand) + "  §6§l" + VideoPoker.NAMES[rank] + (rank == 0 ? "" : "！"));
        return true;
    }

    static net.minecraft.core.BlockPos BlockPosOf(Vec3 v) {
        return net.minecraft.core.BlockPos.containing(v);
    }

    static String handText(int[] hand) {
        String[] R = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"}, S = {"♠", "♥", "♦", "♣"};
        StringBuilder sb = new StringBuilder();
        for (int c : hand) sb.append(c / 13 == 1 || c / 13 == 2 ? "§c" : "§f").append(S[c / 13]).append(R[c % 13]).append(" ");
        return sb.toString().trim();
    }

    // ================= オールイン：体力を賭けて、しばらく大きく強くなる =================
    private static boolean allIn(ServerPlayer p, SkillDefinition s) {
        float cost = p.getHealth() * (float) s.chance();
        if (p.getHealth() - cost < 1) cost = Math.max(0, p.getHealth() - 1);
        p.setHealth(p.getHealth() - cost);
        p.getPersistentData().putLong(ALLIN_UNTIL, p.level().getGameTime() + Math.round(s.duration() * 20));
        p.getPersistentData().putDouble(ALLIN_POW, s.power());
        ServerLevel level = p.serverLevel();
        burst(level, p.position().add(0, 1, 0), RED, 40, 0.6);
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, p.getX(), p.getY(1.8), p.getZ(), 3, 0.3, 0.1, 0.3, 0);
        level.playSound(null, p.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.3F, 1.8F);
        say(p, "§c§lオールイン！ §f" + Math.round(s.duration()) + "秒、ダメージ +" + Math.round(s.power() * 100) + "%");
        return true;
    }

    // ================= スロットバースト：3つそろえば大爆発 =================
    private static final String[] SLOT = {"§c7", "§bダイヤ", "§aエメ", "§6金", "§dベリー"};

    private static boolean slotBurst(ServerPlayer p, SkillDefinition s) {
        RandomSource r = p.getRandom();
        int a = r.nextInt(5), b = r.nextInt(5), c = r.nextInt(5);
        if (a != b && luckyRoll(p, 0.1)) b = a;
        if (b != c && a == b && luckyRoll(p, 0.1)) c = a;
        double mult = a == b && b == c ? (a == 0 ? 7 : 4) : a == b || b == c || a == c ? 2 : 1;
        LivingEntity t = MageSpells.lookTarget(p, 16);
        Vec3 at = t != null ? t.position() : p.position();
        ServerLevel level = p.serverLevel();
        double rad = s.radius() * (mult >= 4 ? 1.5 : 1);
        for (LivingEntity e : enemiesAround(p, at, rad)) SkillEffects.hit(p, e, atk(p) * (float) (s.power() * mult));
        level.sendParticles(mult >= 4 ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, mult >= 4 ? 1 : 3, rad * 0.3, 0.2, rad * 0.3, 0);
        burst(level, at.add(0, 1, 0), GOLD, (int) (20 * mult), rad * 0.4);
        level.playSound(null, BlockPosOf(at), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7F, 1.2F);
        if (mult >= 4) level.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        say(p, "§f[ " + SLOT[a] + " §f| " + SLOT[b] + " §f| " + SLOT[c] + " §f]  " + (mult >= 7 ? "§c§l777！！" : mult >= 4 ? "§6§lそろった！" : mult >= 2 ? "§eおしい！" : "§7…"));
        return true;
    }

    // ================= ジャックポット：金貨の雨 =================
    private static boolean jackpotRain(ServerPlayer p, SkillDefinition s) {
        LivingEntity t = MageSpells.lookTarget(p, 20);
        Vec3 at = t != null ? t.position() : p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(5));
        ServerLevel level = p.serverLevel();
        float dmg = atk(p) * (float) s.power();
        int times = (int) Math.max(1, s.duration() * 4);
        say(p, "§6§l★ ジャックポット！ ★");
        level.playSound(null, BlockPosOf(at), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.4F);
        MageSpells.repeat(p, 5, times, k -> {
            level.sendParticles(GOLD, at.x, at.y + 5, at.z, 40, s.radius() * 0.5, 0.5, s.radius() * 0.5, 0);
            level.sendParticles(ParticleTypes.WAX_ON, at.x, at.y + 3, at.z, 20, s.radius() * 0.5, 1.5, s.radius() * 0.5, 0);
            for (LivingEntity e : enemiesAround(p, at, s.radius())) SkillEffects.hit(p, e, dmg);
            level.playSound(null, BlockPosOf(at), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.7F, 1.6F + (k % 3) * 0.1F);
        });
        return true;
    }

    // ================= 運命の賽：2つ振って合計。ゾロ目ならもう一回 =================
    private static boolean fateDice(ServerPlayer p, SkillDefinition s) {
        LivingEntity t = MageSpells.lookTarget(p, s.radius());
        if (t == null) return false;
        ServerLevel level = p.serverLevel();
        StringBuilder text = new StringBuilder("§d運命の賽：");
        int total = 0;
        for (int round = 0; round < 4; round++) {
            int a = die(p), b = die(p);
            total += a + b;
            text.append("§f[").append(a).append("・").append(b).append("] ");
            if (a != b) break;
            text.append("§6ゾロ目！ ");
        }
        int sum = total;
        MageSpells.repeat(p, 4, 3, k -> {
            if (!t.isAlive()) return;
            SkillEffects.hit(p, t, atk(p) * (float) s.power() * sum / 3F);
            Vec3 c = t.getBoundingBox().getCenter();
            level.sendParticles(WHITE, c.x, c.y, c.z, 10, 0.3, 0.3, 0.3, 0.05);
            level.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 10, 0.3, 0.3, 0.3, 0.3);
            level.playSound(null, t.blockPosition(), SoundEvents.BAMBOO_WOOD_HIT, SoundSource.PLAYERS, 1.0F, 0.8F + k * 0.2F);
        });
        say(p, text.append("§e合計 ").append(sum).toString());
        return true;
    }

    // ================= 胴元の掟：まわりの敵は弱く、もろくなる =================
    private static boolean houseRules(ServerPlayer p, SkillDefinition s) {
        p.getPersistentData().putLong(HOUSE_UNTIL, p.level().getGameTime() + Math.round(s.duration() * 20));
        p.getPersistentData().putDouble(HOUSE_POW, s.power());
        p.getPersistentData().putDouble(HOUSE_POW + "_r", s.radius());
        ServerLevel level = p.serverLevel();
        Vfx.magicCircle(level, p.position(), (float) s.radius(), (int) (s.duration() * 20), Vfx.GOLD);
        level.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.6F);
        say(p, "§6§l胴元の掟 §f… この場はわたしの賭場だ");
        return true;
    }

    // ================= 奥義 セブンス・ヘヴン：7本の光の柱 =================
    private static boolean sevenHeaven(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        float dmg = atk(p) * (float) s.power();
        say(p, "§6§l7 7 7  セブンス・ヘヴン！");
        level.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 0.8F);
        MageSpells.repeat(p, 4, 7, k -> {
            double a = k * Math.PI * 2 / 7;
            Vec3 q = c.add(Math.cos(a) * s.radius() * 0.6, 0, Math.sin(a) * s.radius() * 0.6);
            for (int y = 0; y < 10; y++) level.sendParticles(y % 2 == 0 ? GOLD : WHITE, q.x, q.y + y * 0.5, q.z, 4, 0.15, 0.2, 0.15, 0);
            level.sendParticles(ParticleTypes.END_ROD, q.x, q.y + 2, q.z, 12, 0.2, 2, 0.2, 0.02);
            for (LivingEntity e : enemiesAround(p, q, 3.0)) SkillEffects.hit(p, e, dmg);
            level.playSound(null, BlockPosOf(q), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.0F, 0.6F + k * 0.15F);
            if (k == 6) {
                for (LivingEntity e : enemiesAround(p, c, s.radius())) SkillEffects.hit(p, e, dmg * 2);
                level.sendParticles(ParticleTypes.FIREWORK, c.x, c.y + 2, c.z, 120, s.radius() * 0.4, 1.5, s.radius() * 0.4, 0.15);
            }
        });
        return true;
    }

    private GamblerSkills() {}
}
