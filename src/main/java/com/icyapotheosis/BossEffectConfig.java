package com.icyapotheosis;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

public class BossEffectConfig {
    
    private float chance;
    private String effectId;
    private int amplifierMin;
    private int amplifierMax;
    private boolean ambient;
    private boolean visible;
    
    public BossEffectConfig(float chance, String effectId, int amplifierMin, int amplifierMax, boolean ambient, boolean visible) {
        this.chance = chance;
        this.effectId = effectId;
        this.amplifierMin = amplifierMin;
        this.amplifierMax = amplifierMax;
        this.ambient = ambient;
        this.visible = visible;
    }
    
    public float getChance() {
        return chance;
    }
    
    public MobEffect getEffect() {
        try {
            ResourceLocation rl = new ResourceLocation(effectId);
            return ForgeRegistries.MOB_EFFECTS.getValue(rl);
        } catch (Exception e) {
            return null;
        }
    }
    
    public int getAmplifier(int random) {
        if (amplifierMin >= amplifierMax) return amplifierMin;
        return amplifierMin + random % (amplifierMax - amplifierMin + 1);
    }
    
    public boolean isAmbient() {
        return ambient;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public MobEffectInstance createEffectInstance(int duration, float random) {
        MobEffect effect = getEffect();
        if (effect == null) return null;
        
        int amplifier = amplifierMin;
        if (amplifierMax > amplifierMin) {
            amplifier = amplifierMin + (int)(random * (amplifierMax - amplifierMin + 1));
        }
        
        return new MobEffectInstance(effect, duration, amplifier, ambient, visible);
    }
    
    public boolean shouldApply(float random) {
        return random <= chance;
    }
}
