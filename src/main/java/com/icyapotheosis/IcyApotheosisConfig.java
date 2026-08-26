package com.icyapotheosis;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = IcyApotheosis.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class IcyApotheosisConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> NULL_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> COMMON_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> UNCOMMON_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RARE_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EPIC_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MYTHIC_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ANCIENT_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> GUIXU_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ICYCINDER_END_ENTITIES;

    public static final ForgeConfigSpec.ConfigValue<Boolean> ANNOUNCE_BOSS;
    public static final ForgeConfigSpec.ConfigValue<Boolean> BOSS_AUTO_AGGRO;

    private static final String[] DEFAULT_NULL = {
    };

    private static final String[] DEFAULT_COMMON = {
    };

    private static final String[] DEFAULT_UNCOMMON = {
    };

    private static final String[] DEFAULT_RARE = {
    };

    private static final String[] DEFAULT_EPIC = {
        "minecraft:warden",
        "minecraft:wither"
    };

    private static final String[] DEFAULT_MYTHIC = {
        "minecraft:ender_dragon"
    };

    private static final String[] DEFAULT_ANCIENT = {
    };

    private static final String[] DEFAULT_GUIXU = {
    };

    private static final String[] DEFAULT_ICYCINDER_END = {
    };

    static {
        
        
        
        
        
        
        
        
        BUILDER.comment("============================================================")
               .comment("实体稀有度配置 / Entity Rarity Configuration")
               .comment("============================================================")
               .comment("Format: \"entity_id\" (weight=1.0) or \"entity_id|weight\"")
               .comment("When an entity appears in multiple lists, tier is chosen by weighted random.");

        NULL_ENTITIES = BUILDER
            .comment("Null等级（原版实体，不做任何修改unchanged）")
            .comment("格式: '实体ID' 或 '实体ID|权重'")
            .defineList("nullEntities", List.of(DEFAULT_NULL), o -> o instanceof String);

        COMMON_ENTITIES = BUILDER
            .comment("")
            .comment("[普通 等级 / Common Tier] (ordinal 0)")
            .comment("固定成为【普通】等级神化Boss的实体列表。")
            .comment("Entities forced to be COMMON-tier Apotheosis Bosses.")
            .comment("Format: 'entityId' or 'entityId|weight'")
            .comment(" 如果同一实体出现在多个稀有度列表中，将根据权重随机选择等级。")
            .comment("If the same entity appears in multiple rarity lists, its tier is chosen randomly by weight.")
            .defineList("commonEntities", List.of(DEFAULT_COMMON), o -> o instanceof String);

        UNCOMMON_ENTITIES = BUILDER
            .comment("")
            .comment("[非凡 等级 / Uncommon Tier] (ordinal 1)")
            .comment("固定成为【非凡】等级神化Boss的实体列表。默认给予：速度 + 防火。")
            .comment("Entities forced to be UNCOMMON-tier Apotheosis Bosses. Auto-applies Speed + Fire Resistance.")
            .comment("Format: 'entityId' or 'entityId|weight'")
            .defineList("uncommonEntities", List.of(DEFAULT_UNCOMMON), o -> o instanceof String);

        RARE_ENTITIES = BUILDER
            .comment("")
            .comment("[珍稀 等级 / Rare Tier] (ordinal 2)")
            .comment("固定成为【珍稀】等级神化Boss的实体列表。")
            .comment("Entities forced to be RARE-tier Apotheosis Bosses.")
            .comment("Format: 'entityId' or 'entityId|weight'")
            .defineList("rareEntities", List.of(DEFAULT_RARE), o -> o instanceof String);

        EPIC_ENTITIES = BUILDER
            .comment("")
            .comment("[史诗 等级 / Epic Tier] (ordinal 3) — 默认：监守者 / 凋灵")
            .comment("固定成为【史诗】等级神化Boss的实体列表。（默认：监守者 / 凋灵）")
            .comment("Entities forced to be EPIC-tier Apotheosis Bosses. (default: Warden / Wither)")
            .comment("Format: 'entityId' or 'entityId|weight'")
            .defineList("epicEntities", List.of(DEFAULT_EPIC), o -> o instanceof String);

        MYTHIC_ENTITIES = BUILDER
            .comment("")
            .comment("[神话 等级 / Mythic Tier] (ordinal 4) — 默认：末影龙")
            .comment("固定成为【神话】等级神化Boss的实体列表。（默认：末影龙）")
            .comment("Entities forced to be MYTHIC-tier Apotheosis Bosses. (default: Ender Dragon)")
            .comment("Format: 'entityId' or 'entityId|weight'")
            .defineList("mythicEntities", List.of(DEFAULT_MYTHIC), o -> o instanceof String);

        ANCIENT_ENTITIES = BUILDER
            .comment("")
            .comment("[源起 等级 / Ancient (Yuanqi) Tier] (ordinal 5 — vanilla Apoth max)")
            .comment("固定成为【源起】等级神化Boss的实体列表。源起 = 神化原生最高等级远古。")
            .comment("Entities forced to be ANCIENT (Yuanqi / 源起) tier — vanilla Apotheosis' maximum rarity.")
            .comment("能稳定生成多词缀。")
            .comment("Reliably produces multiple STAT/ABILITY affixes.")
            .comment("Format: 'entityId' or 'entityId|weight'")
            .defineList("ancientEntities", List.of(DEFAULT_ANCIENT), o -> o instanceof String);

        GUIXU_ENTITIES = BUILDER
            .comment("")
            .comment("[归墟 等级 / Guixu (沫汐) Tier] (ordinal 6 — 自定义 / Custom)")
            .comment("固定成为【归墟·沫汐】等级神化Boss的实体列表。自定义等级，大红渲染。")
            .comment("Entities forced to GUIXU (归墟 · Moxi) tier — CUSTOM ordinal 6, crimson red.")
            .comment("属性：血量×9，攻击×4，护甲+36，速度IV~V，力量IV~V，抗性III~IV。保底 6 STAT / 4 ABILITY / 5 宝石槽。")
            .comment("Stats: HP×9, ATK×4, Armor+36, Speed IV-V, Strength IV-V, Resistance III-IV. Guarantees ≥6 STAT, ≥4 ABILITY, 5 Sockets.")
            .comment("Format: 'entityId' or 'entityId|weight'")
            .defineList("guixuEntities", List.of(DEFAULT_GUIXU), o -> o instanceof String);

        ICYCINDER_END_ENTITIES = BUILDER
            .comment("")
            .comment("[冰烬之终焉 等级 / Icycinder End (超主) Tier] (ordinal 7 — 最高自定义最高 / Highest custom)")
            .comment("固定成为【冰烬之终焉·超主】等级神化Boss的实体列表。最高等级，冰蓝颜色转换渲染，名字带超高速乱码闪烁特效。")
            .comment("Entities forced to ICYCINDER_END (冰烬之终焉 · Chaozhu) tier — HIGHEST custom ordinal 7, icy blue,  high-speed glitch-name shader.")
            .comment("属性：血量×12，攻击×5，护甲+49，速度V~VI，力量V~VI，抗性III~IV。保底 7 STAT / 4 ABILITY / 5 宝石槽。50%概率获得额外生命恢复。")
            .comment("Stats: HP×12, ATK×5, Armor+49, Speed V-VI, Strength V-VI, Resistance III-IV. Guarantees ≥7 STAT, ≥4 ABILITY, 5 Sockets. 50% chance for bonus Regeneration.")
            .comment("Format: 'entityId' or 'entityId|weight'")
            .defineList("icycinderEndEntities", List.of(DEFAULT_ICYCINDER_END), o -> o instanceof String);

        
        
        
        BUILDER.comment("")
               .comment("============================================================")
               .comment("行为开关 / Behaviour Toggles")
               .comment("============================================================");

        ANNOUNCE_BOSS = BUILDER
            .comment("")
            .comment("中文: Boss生成时是否向256格内玩家发送ActionBar公告。")
            .comment("English: Whether to broadcast an ActionBar announcement to players within 256 blocks when a custom Apoth Boss spawns.")
            .define("announceBoss", true);

        BOSS_AUTO_AGGRO = BUILDER
            .comment("")
            .comment("中文: Boss生成后是否自动锁定最近的生存/冒险模式玩家。三重保险确保创造/旁观玩家永不被锁定。")
            .comment("English: Whether the Boss automatically targets the nearest Survival/Adventure player on spawn. Triple-layer guard guarantees Creative/Spectator players are NEVER targeted.")
            .define("bossAutoAggro", true);

        SPEC = BUILDER.build();
    }

    public static Map<ResourceLocation, Map<String, Float>> entityRarityMap = new HashMap<>();

    
    @javax.annotation.Nullable
    private static net.minecraftforge.fml.config.ModConfig cachedModConfig;

    public static void register() {
        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, SPEC);
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        
        
        if (event.getConfig().getModId().equals(IcyApotheosis.MOD_ID)) {
            cachedModConfig = event.getConfig();
        }
        loadConfig();
    }

    
    public static void forceReloadFromDisk() {
        int before = entityRarityMap.size();
        try {
            net.minecraftforge.fml.config.ConfigTracker.INSTANCE.loadConfigs(
                net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get()
            );
            IcyApotheosis.LOGGER.info("[icy_apotheosis] ConfigTracker.INSTANCE.loadConfigs(COMMON) executed — SPEC refreshed from disk.");
        } catch (Throwable t) {
            IcyApotheosis.LOGGER.warn("[icy_apotheosis] ConfigTracker.loadConfigs failed (API mismatch?), proceeding with current SPEC cache: {}",
                    t.toString());
        }
        
        
        
        loadConfig();
        int after = entityRarityMap.size();
        IcyApotheosis.LOGGER.info("[icy_apotheosis] forceReloadFromDisk: before={} entities -> after={} entities.", before, after);
    }

    public static void loadConfig() {
        entityRarityMap.clear();

        loadRarityList(NULL_ENTITIES, "null");
        loadRarityList(COMMON_ENTITIES, "common");
        loadRarityList(UNCOMMON_ENTITIES, "uncommon");
        loadRarityList(RARE_ENTITIES, "rare");
        loadRarityList(EPIC_ENTITIES, "epic");
        loadRarityList(MYTHIC_ENTITIES, "mythic");
        loadRarityList(ANCIENT_ENTITIES, "ancient");
        loadRarityList(GUIXU_ENTITIES, "guixu");
        loadRarityList(ICYCINDER_END_ENTITIES, "icycinder_end");

        int totalEntities = entityRarityMap.size();
        IcyApotheosis.LOGGER.info("Loaded {} boss entities with rarity mappings", totalEntities);
    }

    private static void loadRarityList(ForgeConfigSpec.ConfigValue<List<? extends String>> config, String rarity) {
        List<? extends String> list = config.get();
        for (String entry : list) {
            parseEntry(entry, rarity);
        }
    }

    private static void parseEntry(String entry, String rarity) {
        if (entry == null || entry.trim().isEmpty()) return;

        String[] parts = entry.split("\\|");
        if (parts.length == 0) return;

        String entityId = parts[0].trim();
        float weight = 1.0f;

        if (parts.length > 1) {
            try {
                weight = Float.parseFloat(parts[1].trim());
                if (weight <= 0) weight = 1.0f;
            } catch (NumberFormatException e) {
                IcyApotheosis.LOGGER.warn("Invalid weight in entry: '{}'. Using default weight 1.0", entry);
                weight = 1.0f;
            }
        }

        try {
            ResourceLocation entityRL = new ResourceLocation(entityId);
            entityRarityMap.computeIfAbsent(entityRL, k -> new HashMap<>())
                .put(rarity, weight);
        } catch (Exception e) {
            IcyApotheosis.LOGGER.error("Failed to parse entity ID: '{}'", entityId, e);
        }
    }

    public static String getRarityForEntity(ResourceLocation entityKey, net.minecraft.util.RandomSource random) {
        Map<String, Float> rarityWeights = entityRarityMap.get(entityKey);
        if (rarityWeights == null || rarityWeights.isEmpty()) return null;

        if (rarityWeights.size() == 1) {
            return rarityWeights.keySet().iterator().next();
        }

        float totalWeight = 0.0f;
        for (Float w : rarityWeights.values()) {
            totalWeight += w;
        }

        float roll = random.nextFloat() * totalWeight;
        float currentWeight = 0.0f;

        for (Map.Entry<String, Float> entry : rarityWeights.entrySet()) {
            currentWeight += entry.getValue();
            if (roll < currentWeight) {
                return entry.getKey();
            }
        }

        return rarityWeights.keySet().iterator().next();
    }

    public static Map<ResourceLocation, Map<String, Float>> getEntityRarityMap() {
        return entityRarityMap;
    }

    public static boolean shouldAnnounceBoss() {
        return ANNOUNCE_BOSS.get();
    }

    public static boolean shouldBossAutoAggro() {
        return BOSS_AUTO_AGGRO.get();
    }
}
