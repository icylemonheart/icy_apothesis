package com.icyapotheosis;

import java.util.ArrayList;
import java.util.List;

public class BossRarityConfig {
    
    private String rarity;
    private float healthMultiplier;
    private float speedMultiplier;
    private float damageMultiplier;
    private float armorBonus;
    private List<BossEffectConfig> effects;
    private List<BossModifierConfig> modifiers;
    
    public BossRarityConfig(String rarity, float healthMultiplier, float speedMultiplier, 
                           float damageMultiplier, float armorBonus,
                           List<BossEffectConfig> effects, List<BossModifierConfig> modifiers) {
        this.rarity = rarity;
        this.healthMultiplier = healthMultiplier;
        this.speedMultiplier = speedMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.armorBonus = armorBonus;
        this.effects = effects != null ? effects : new ArrayList<>();
        this.modifiers = modifiers != null ? modifiers : new ArrayList<>();
    }
    
    public String getRarity() {
        return rarity;
    }
    
    public float getHealthMultiplier() {
        return healthMultiplier;
    }
    
    public float getSpeedMultiplier() {
        return speedMultiplier;
    }
    
    public float getDamageMultiplier() {
        return damageMultiplier;
    }
    
    public float getArmorBonus() {
        return armorBonus;
    }
    
    public List<BossEffectConfig> getEffects() {
        return effects;
    }
    
    public List<BossModifierConfig> getModifiers() {
        return modifiers;
    }
}
