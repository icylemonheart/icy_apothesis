package com.icyapotheosis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@EventBusSubscriber(modid = IcyApotheosis.MOD_ID)
public class CinderEndNameManager {

    private static final int INITIAL_COLOR = 0x66CCFF;
    private static final int COLOR_CHANGE_DELAY = 66;
    private static final int UPDATE_INTERVAL = 1;

    private static final int[] CYCLE_COLORS = {
        0xFF0000, 0xFF4500, 0xFF8C00, 0xFFD700,
        0x9ACD32, 0x00FF00, 0x00FA9A, 0x00CED1,
        0x1E90FF, 0x4169E1, 0x8A2BE2, 0xFF1493,
        0xFF69B4, 0xDC143C, 0xFF6347, 0xFFA500,
        0xADFF2F, 0x7FFF00, 0x40E0D0, 0x6495ED,
        0xDDA0DD, 0xEE82EE, 0xFF00FF, 0xBA55D3
    };

    private static final String[] CONNECTORS = {
        "之", "的", "·", " ", "", "丨", "—", "、", "‖",
        "一", "兮", "攸", "厥", "伊", "维", "乃", "若", "似", "其", "拟", "封", "汐", "雨", "冰", "洁", "柠", "檬", "心",
        "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖", "拾", "佰", "阡", "万", "亿", "兆", "京", "垓", "秭", "穰", "沟", "涧", "正", "载", "极", "恒", "河", "沙", "阿", "僧", "祇", "那", "由",
        "他", "她", "它", "祂", "不", "可", "思", "易", "无", "量", "大", "数",
        "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
        "α", "β", "γ", "δ", "ε", "ζ", "η", "θ", "ι", "κ", "λ", "μ", "ν", "ξ", "ο", "π", "ρ", "σ", "τ", "υ", "φ", "χ", "ψ", "ω",
        "Α", "Β", "Γ", "Δ", "Ε", "Ζ", "Η", "Θ", "Ι", "Κ", "Λ", "Μ", "Ν", "Ξ", "Ο", "Π", "Ρ", "Σ", "Τ", "Υ", "Φ", "Χ", "Ψ", "Ω"
    };

    private static final List<WeightedPrefix> PREFIX_PARTS = new ArrayList<>();
    private static final List<WeightedText> SUFFIX_TEXTS = new ArrayList<>();

    static {
        PREFIX_PARTS.add(new WeightedPrefix("冰烬", "终焉", 1));
        PREFIX_PARTS.add(new WeightedPrefix("轮回", "终末", 1));
        PREFIX_PARTS.add(new WeightedPrefix("虚空", "低语", 2));
        PREFIX_PARTS.add(new WeightedPrefix("魂灵", "月窠", 2));
        PREFIX_PARTS.add(new WeightedPrefix("世界", "喰煞", 2));
        PREFIX_PARTS.add(new WeightedPrefix("渴求", "器具", 2));
        PREFIX_PARTS.add(new WeightedPrefix("生命", "织缕", 4));
        PREFIX_PARTS.add(new WeightedPrefix("幽影", "溯行", 8));
        PREFIX_PARTS.add(new WeightedPrefix("星骸", "残响", 8));
        PREFIX_PARTS.add(new WeightedPrefix("妄念", "蜃楼", 8));
        PREFIX_PARTS.add(new WeightedPrefix("冻土", "沉眠止", 8));
        PREFIX_PARTS.add(new WeightedPrefix("焚风", "狂啸", 8));
        PREFIX_PARTS.add(new WeightedPrefix("渊流", "渡引", 8));
        PREFIX_PARTS.add(new WeightedPrefix("碎光", "残翼", 8));
        PREFIX_PARTS.add(new WeightedPrefix("枯朽", "蔓藤", 8));
        PREFIX_PARTS.add(new WeightedPrefix("时砂", "漏刻", 8));
        PREFIX_PARTS.add(new WeightedPrefix("寂空", "孤帆", 8));
        PREFIX_PARTS.add(new WeightedPrefix("碎语", "碑铭", 8));
        PREFIX_PARTS.add(new WeightedPrefix("云涌", "惊霆", 8));
        PREFIX_PARTS.add(new WeightedPrefix("薄暮", "残烛（消逝）", 8));
        PREFIX_PARTS.add(new WeightedPrefix("幻潮", "浮沤（虚妄）", 8));
        PREFIX_PARTS.add(new WeightedPrefix("寒渊", "镜影", 8));
        PREFIX_PARTS.add(new WeightedPrefix("荒墟", "残笛", 8));
        PREFIX_PARTS.add(new WeightedPrefix("星浪", "航标", 8));
        PREFIX_PARTS.add(new WeightedPrefix("烬灰", "萌芽", 8));
        PREFIX_PARTS.add(new WeightedPrefix("迷雾", "歧途", 8));
        PREFIX_PARTS.add(new WeightedPrefix("古弦", "余响", 8));
        PREFIX_PARTS.add(new WeightedPrefix("晶狱", "囚灵", 8));
        PREFIX_PARTS.add(new WeightedPrefix("长风", "信使", 8));
        PREFIX_PARTS.add(new WeightedPrefix("沉渊", "玉蚌", 8));
        PREFIX_PARTS.add(new WeightedPrefix("赤焰", "熔心", 8));

        SUFFIX_TEXTS.add(new WeightedText("冰烬", 1));
        SUFFIX_TEXTS.add(new WeightedText("终结", 1));
        SUFFIX_TEXTS.add(new WeightedText("智慧", 2));
        SUFFIX_TEXTS.add(new WeightedText("和谐", 2));
        SUFFIX_TEXTS.add(new WeightedText("战争", 2));
        SUFFIX_TEXTS.add(new WeightedText("物质", 2));
        SUFFIX_TEXTS.add(new WeightedText("生命", 4));
        SUFFIX_TEXTS.add(new WeightedText("漂泊", 8));
        SUFFIX_TEXTS.add(new WeightedText("湮灭", 8));
        SUFFIX_TEXTS.add(new WeightedText("幻梦", 8));
        SUFFIX_TEXTS.add(new WeightedText("静止", 8));
        SUFFIX_TEXTS.add(new WeightedText("躁动", 8));
        SUFFIX_TEXTS.add(new WeightedText("彼岸", 8));
        SUFFIX_TEXTS.add(new WeightedText("希冀", 8));
        SUFFIX_TEXTS.add(new WeightedText("腐朽", 8));
        SUFFIX_TEXTS.add(new WeightedText("时序", 8));
        SUFFIX_TEXTS.add(new WeightedText("孤独", 8));
        SUFFIX_TEXTS.add(new WeightedText("记忆", 8));
        SUFFIX_TEXTS.add(new WeightedText("裁决", 8));
        SUFFIX_TEXTS.add(new WeightedText("消逝", 8));
        SUFFIX_TEXTS.add(new WeightedText("虚妄", 8));
        SUFFIX_TEXTS.add(new WeightedText("自我", 8));
        SUFFIX_TEXTS.add(new WeightedText("离殇", 8));
        SUFFIX_TEXTS.add(new WeightedText("求索", 8));
        SUFFIX_TEXTS.add(new WeightedText("复现", 8));
        SUFFIX_TEXTS.add(new WeightedText("抉择", 8));
        SUFFIX_TEXTS.add(new WeightedText("传承", 8));
        SUFFIX_TEXTS.add(new WeightedText("封印", 8));
        SUFFIX_TEXTS.add(new WeightedText("自由", 8));
        SUFFIX_TEXTS.add(new WeightedText("隐秘", 8));
        SUFFIX_TEXTS.add(new WeightedText("热忱", 8));
    }

    private record WeightedPrefix(String part1, String part2, int weight) {}
    private record WeightedText(String text, int weight) {}

    private static class BossState {
        final UUID uuid;
        final Component entityName;
        final long spawnTick;
        int colorIndex;

        BossState(UUID uuid, Component entityName, long spawnTick) {
            this.uuid = uuid;
            this.entityName = entityName;
            this.spawnTick = spawnTick;
            this.colorIndex = 0;
        }
    }

    private static final Map<UUID, BossState> trackedBosses = new ConcurrentHashMap<>();
    private static long currentTick = 0;

    public static void registerBoss(Mob mob) {
        BossState state = new BossState(
            mob.getUUID(),
            mob.getType().getDescription(),
            currentTick
        );
        trackedBosses.put(mob.getUUID(), state);

        updateBossName(mob, state, mob.getRandom());
    }

    
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        currentTick++;
        if (currentTick % UPDATE_INTERVAL != 0) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        Iterator<Map.Entry<UUID, BossState>> it = trackedBosses.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, BossState> entry = it.next();
            BossState state = entry.getValue();

            Mob mob = findMob(server, state.uuid);
            if (mob == null || !mob.isAlive()) {
                it.remove();
                continue;
            }

            updateBossName(mob, state, mob.getRandom());
        }
    }

    private static void updateBossName(Mob mob, BossState state, RandomSource random) {
        WeightedPrefix wp = selectWeightedPrefix(random);
        String connector = CONNECTORS[random.nextInt(CONNECTORS.length)];
        String prefix = wp.part1 + connector + wp.part2;
        String suffix = selectWeightedText(SUFFIX_TEXTS, random);

        int color;
        if (currentTick - state.spawnTick < COLOR_CHANGE_DELAY) {
            color = INITIAL_COLOR;
        } else {
            color = CYCLE_COLORS[state.colorIndex % CYCLE_COLORS.length];
            state.colorIndex++;
        }

        Style style = Style.EMPTY.withColor(TextColor.fromRgb(color));

        MutableComponent name = Component.literal(prefix + " ")
            .append(state.entityName.copy())
            .append(Component.literal(" " + suffix))
            .withStyle(style);

        mob.setCustomName(name);
    }

    private static Mob findMob(MinecraftServer server, UUID uuid) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof Mob mob) return mob;
        }
        return null;
    }

    private static WeightedPrefix selectWeightedPrefix(RandomSource random) {
        int totalWeight = 0;
        for (WeightedPrefix t : PREFIX_PARTS) totalWeight += t.weight;
        int roll = random.nextInt(totalWeight);
        int current = 0;
        for (WeightedPrefix t : PREFIX_PARTS) {
            current += t.weight;
            if (roll < current) return t;
        }
        return PREFIX_PARTS.get(PREFIX_PARTS.size() - 1);
    }

    private static String selectWeightedText(List<WeightedText> texts, RandomSource random) {
        int totalWeight = 0;
        for (WeightedText t : texts) totalWeight += t.weight;
        int roll = random.nextInt(totalWeight);
        int current = 0;
        for (WeightedText t : texts) {
            current += t.weight;
            if (roll < current) return t.text;
        }
        return texts.get(texts.size() - 1).text;
    }
}
