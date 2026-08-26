package com.icyapotheosis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BossConfigManager {
    
    public static final int OPERATION_ADD = BossModifierConfig.OPERATION_ADD;
    public static final int OPERATION_MULTIPLY_BASE = BossModifierConfig.OPERATION_MULTIPLY_BASE;
    public static final int OPERATION_MULTIPLY_TOTAL = BossModifierConfig.OPERATION_MULTIPLY_TOTAL;
    
    private static BossConfigManager INSTANCE;
    
    private Map<String, BossRarityConfig> rarityConfigs;
    
    private BossConfigManager() {
        this.rarityConfigs = new HashMap<>();
        registerDefaultConfigs();
    }
    
    public static synchronized BossConfigManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BossConfigManager();
        }
        return INSTANCE;
    }
    
    private void registerDefaultConfigs() {
        registerNullConfig();
        registerCommonConfig();
        registerUncommonConfig();
        registerRareConfig();
        registerEpicConfig();
        registerMythicConfig();
        registerAncientConfig();
        registerGuixuConfig();
        registerIcyCinderEndConfig();
    }
    
    private void registerNullConfig() {
        List<BossEffectConfig> effects = new ArrayList<>();
        List<BossModifierConfig> modifiers = new ArrayList<>();
        
        rarityConfigs.put("null", new BossRarityConfig(
            "null",
            1.0f, 1.0f, 1.0f, 0.0f,
            effects, modifiers
        ));
    }
    
    private void registerCommonConfig() {
        List<BossEffectConfig> effects = new ArrayList<>();
        List<BossModifierConfig> modifiers = new ArrayList<>();
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.max_health",
            OPERATION_MULTIPLY_BASE,
            0.2f, 0.2f,
            "icy_apotheosis.common_health"
        ));
        
        rarityConfigs.put("common", new BossRarityConfig(
            "common",
            1.2f, 1.0f, 1.0f, 0.0f,
            effects, modifiers
        ));
    }
    
    
    private void registerUncommonConfig() {
        List<BossEffectConfig> effects = new ArrayList<>();
        effects.add(new BossEffectConfig(1.0f, "minecraft:speed", 0, 0, true, false));
        effects.add(new BossEffectConfig(1.0f, "minecraft:fire_resistance", 0, 0, true, false));
        
        List<BossModifierConfig> modifiers = new ArrayList<>();
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.max_health",
            OPERATION_MULTIPLY_BASE,
            0.5f, 0.5f,
            "icy_apotheosis.uncommon_health"
        ));
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.attack_damage",
            OPERATION_MULTIPLY_BASE,
            0.1f, 0.1f,
            "icy_apotheosis.uncommon_damage"
        ));
        
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.armor",
            OPERATION_ADD,
            1.0f, 1.0f,
            "icy_apotheosis.uncommon_armor"
        ));
        
        rarityConfigs.put("uncommon", new BossRarityConfig(
            "uncommon",
            1.5f, 1.0f, 1.1f, 1.0f,
            effects, modifiers
        ));
    }
    
    
    private void registerRareConfig() {
        List<BossEffectConfig> effects = new ArrayList<>();
        effects.add(new BossEffectConfig(1.0f, "minecraft:speed", 0, 1, true, false));
        effects.add(new BossEffectConfig(1.0f, "minecraft:strength", 0, 0, true, false));
        effects.add(new BossEffectConfig(1.0f, "minecraft:fire_resistance", 0, 0, true, false));
        
        List<BossModifierConfig> modifiers = new ArrayList<>();
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.max_health",
            OPERATION_MULTIPLY_BASE,
            1.0f, 1.0f,
            "icy_apotheosis.rare_health"
        ));
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.attack_damage",
            OPERATION_MULTIPLY_BASE,
            0.25f, 0.25f,
            "icy_apotheosis.rare_damage"
        ));
        
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.armor",
            OPERATION_ADD,
            4.0f, 4.0f,
            "icy_apotheosis.rare_armor"
        ));
        
        rarityConfigs.put("rare", new BossRarityConfig(
            "rare",
            2.0f, 1.05f, 1.25f, 4.0f,
            effects, modifiers
        ));
    }
    
    
    private void registerEpicConfig() {
        List<BossEffectConfig> effects = new ArrayList<>();
        effects.add(new BossEffectConfig(1.0f, "minecraft:speed", 1, 2, true, false));
        effects.add(new BossEffectConfig(1.0f, "minecraft:strength", 0, 1, true, false));
        effects.add(new BossEffectConfig(1.0f, "minecraft:fire_resistance", 0, 0, true, false));
        
        List<BossModifierConfig> modifiers = new ArrayList<>();
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.max_health",
            OPERATION_MULTIPLY_BASE,
            2.0f, 2.0f,
            "icy_apotheosis.epic_health"
        ));
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.attack_damage",
            OPERATION_MULTIPLY_BASE,
            0.5f, 0.5f,
            "icy_apotheosis.epic_damage"
        ));
        
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.armor",
            OPERATION_ADD,
            9.0f, 9.0f,
            "icy_apotheosis.epic_armor"
        ));
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.movement_speed",
            OPERATION_MULTIPLY_BASE,
            0.1f, 0.1f,
            "icy_apotheosis.epic_speed"
        ));
        
        rarityConfigs.put("epic", new BossRarityConfig(
            "epic",
            3.0f, 1.1f, 1.5f, 9.0f,
            effects, modifiers
        ));
    }
    
    
    private void registerMythicConfig() {
        List<BossEffectConfig> effects = new ArrayList<>();
        effects.add(new BossEffectConfig(1.0f, "minecraft:speed", 2, 2, true, false));
        effects.add(new BossEffectConfig(1.0f, "minecraft:strength", 1, 2, true, false));
        effects.add(new BossEffectConfig(1.0f, "minecraft:resistance", 0, 1, true, false));
        effects.add(new BossEffectConfig(1.0f, "minecraft:fire_resistance", 0, 0, true, false));
        
        List<BossModifierConfig> modifiers = new ArrayList<>();
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.max_health",
            OPERATION_MULTIPLY_BASE,
            4.0f, 4.0f,
            "icy_apotheosis.mythic_health"
        ));
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.attack_damage",
            OPERATION_MULTIPLY_BASE,
            1.0f, 1.0f,
            "icy_apotheosis.mythic_damage"
        ));
        
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.armor",
            OPERATION_ADD,
            16.0f, 16.0f,
            "icy_apotheosis.mythic_armor"
        ));
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.movement_speed",
            OPERATION_MULTIPLY_BASE,
            0.2f, 0.2f,
            "icy_apotheosis.mythic_speed"
        ));
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.knockback_resistance",
            OPERATION_ADD,
            1.0f, 1.0f,
            "icy_apotheosis.mythic_kb_resist"
        ));
        
        rarityConfigs.put("mythic", new BossRarityConfig(
            "mythic",
            5.0f, 1.2f, 2.0f, 16.0f,
            effects, modifiers
        ));
    }
    
    
    private void registerAncientConfig() {
        List<BossEffectConfig> effects = new ArrayList<>();
        effects.add(new BossEffectConfig(1.0f, "minecraft:speed", 2, 3, true, false));
        effects.add(new BossEffectConfig(1.0f, "minecraft:strength", 2, 3, true, false));
        effects.add(new BossEffectConfig(1.0f, "minecraft:resistance", 1, 2, true, false));
        effects.add(new BossEffectConfig(1.0f, "minecraft:fire_resistance", 0, 0, true, false));
        
        List<BossModifierConfig> modifiers = new ArrayList<>();
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.max_health",
            OPERATION_MULTIPLY_BASE,
            6.0f, 6.0f,
            "icy_apotheosis.ancient_health"
        ));
        
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.attack_damage",
            OPERATION_MULTIPLY_BASE,
            2.0f, 2.0f,
            "icy_apotheosis.ancient_damage"
        ));
        
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.armor",
            OPERATION_ADD,
            25.0f, 25.0f,
            "icy_apotheosis.ancient_armor"
        ));
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.movement_speed",
            OPERATION_MULTIPLY_BASE,
            0.3f, 0.3f,
            "icy_apotheosis.ancient_speed"
        ));
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.knockback_resistance",
            OPERATION_ADD,
            1.0f, 1.0f,
            "icy_apotheosis.ancient_kb_resist"
        ));
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.flying_speed",
            OPERATION_MULTIPLY_BASE,
            0.1f, 0.1f,
            "icy_apotheosis.ancient_flying_speed"
        ));
        
        rarityConfigs.put("ancient", new BossRarityConfig(
            "ancient",
            7.0f, 1.3f, 3.0f, 25.0f,
            effects, modifiers
        ));
    }
    
    
    private void registerGuixuConfig() {
        List<BossEffectConfig> effects = new ArrayList<>();
        effects.add(new BossEffectConfig(1.0f, "minecraft:speed", 3, 4, true, false));
        effects.add(new BossEffectConfig(1.0f, "minecraft:strength", 3, 4, true, false));
        effects.add(new BossEffectConfig(1.0f, "minecraft:resistance", 2, 3, true, false));
        effects.add(new BossEffectConfig(1.0f, "minecraft:fire_resistance", 0, 0, true, false));

        List<BossModifierConfig> modifiers = new ArrayList<>();
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.max_health",
            OPERATION_MULTIPLY_BASE,
            8.0f, 8.0f,
            "icy_apotheosis.guixu_health"
        ));
        
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.attack_damage",
            OPERATION_MULTIPLY_BASE,
            3.0f, 3.0f,
            "icy_apotheosis.guixu_damage"
        ));
        
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.armor",
            OPERATION_ADD,
            36.0f, 36.0f,
            "icy_apotheosis.guixu_armor"
        ));
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.movement_speed",
            OPERATION_MULTIPLY_BASE,
            0.35f, 0.35f,
            "icy_apotheosis.guixu_speed"
        ));
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.knockback_resistance",
            OPERATION_ADD,
            1.0f, 1.0f,
            "icy_apotheosis.guixu_kb_resist"
        ));
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.flying_speed",
            OPERATION_MULTIPLY_BASE,
            0.15f, 0.15f,
            "icy_apotheosis.guixu_flying_speed"
        ));
        
        rarityConfigs.put("guixu", new BossRarityConfig(
            "guixu",
            9.0f, 1.4f, 4.0f, 36.0f,
            effects, modifiers
        ));
    }
    
    
    private void registerIcyCinderEndConfig() {
        List<BossEffectConfig> effects = new ArrayList<>();
        effects.add(new BossEffectConfig(1.0f, "minecraft:speed", 4, 5, true, false));
        effects.add(new BossEffectConfig(1.0f, "minecraft:strength", 4, 5, true, false));
        effects.add(new BossEffectConfig(1.0f, "minecraft:resistance", 2, 3, true, false));
        effects.add(new BossEffectConfig(1.0f, "minecraft:fire_resistance", 0, 0, true, false));
        effects.add(new BossEffectConfig(0.5f, "minecraft:regeneration", 0, 1, true, false));

        List<BossModifierConfig> modifiers = new ArrayList<>();
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.max_health",
            OPERATION_MULTIPLY_BASE,
            10.0f, 10.0f,
            "icy_apotheosis.icycinder_end_health"
        ));
        
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.attack_damage",
            OPERATION_MULTIPLY_BASE,
            4.0f, 4.0f,
            "icy_apotheosis.icycinder_end_damage"
        ));
        
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.armor",
            OPERATION_ADD,
            49.0f, 49.0f,
            "icy_apotheosis.icycinder_end_armor"
        ));
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.movement_speed",
            OPERATION_MULTIPLY_BASE,
            0.4f, 0.4f,
            "icy_apotheosis.icycinder_end_speed"
        ));
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.knockback_resistance",
            OPERATION_ADD,
            1.0f, 1.0f,
            "icy_apotheosis.icycinder_end_kb_resist"
        ));
        
        modifiers.add(new BossModifierConfig(
            "minecraft:generic.flying_speed",
            OPERATION_MULTIPLY_BASE,
            0.2f, 0.2f,
            "icy_apotheosis.icycinder_end_flying_speed"
        ));
        
        rarityConfigs.put("icycinder_end", new BossRarityConfig(
            "icycinder_end",
            12.0f, 1.5f, 5.0f, 49.0f,
            effects, modifiers
        ));
    }
    
    public BossRarityConfig getRarityConfig(String rarity) {
        BossRarityConfig config = rarityConfigs.get(rarity.toLowerCase());
        if (config == null) {
            return rarityConfigs.get("common");
        }
        return config;
    }
    
    public boolean hasRarityConfig(String rarity) {
        return rarityConfigs.containsKey(rarity.toLowerCase());
    }
    
    public void registerRarityConfig(String rarity, BossRarityConfig config) {
        rarityConfigs.put(rarity.toLowerCase(), config);
    }
    
    public java.util.Set<String> getRarityConfigs() {
        return rarityConfigs.keySet();
    }
}
