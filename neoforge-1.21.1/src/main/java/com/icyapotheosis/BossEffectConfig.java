package com.icyapotheosis;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

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

    
    public Holder<MobEffect> getEffect() {
        try {
            ResourceLocation rl = ResourceLocation.parse(effectId);
            return BuiltInRegistries.MOB_EFFECT.getHolder(rl).orElse(null);
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
        Holder<MobEffect> effectHolder = getEffect();
        if (effectHolder == null) return null;

        int amplifier = amplifierMin;
        if (amplifierMax > amplifierMin) {
            amplifier = amplifierMin + (int) (random * (amplifierMax - amplifierMin + 1));
        }

        return new MobEffectInstance(effectHolder, duration, amplifier, ambient, visible);
    }

    public boolean shouldApply(float random) {
        return random <= chance;
    }
}
