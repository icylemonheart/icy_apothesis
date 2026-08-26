package com.icyapotheosis;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.UUID;

public class BossModifierConfig {
    
    public static final int OPERATION_ADD = 0;
    public static final int OPERATION_MULTIPLY_BASE = 1;
    public static final int OPERATION_MULTIPLY_TOTAL = 2;
    
    private String attributeId;
    private int operationType;
    private float valueMin;
    private float valueMax;
    private String name;
    
    public BossModifierConfig(String attributeId, int operationType, float valueMin, float valueMax, String name) {
        this.attributeId = attributeId;
        this.operationType = operationType;
        this.valueMin = valueMin;
        this.valueMax = valueMax;
        this.name = name != null ? name : "icy_apotheosis_boss_modifier";
    }
    
    public Attribute getAttribute() {
        try {
            ResourceLocation rl = new ResourceLocation(attributeId);
            return ForgeRegistries.ATTRIBUTES.getValue(rl);
        } catch (Exception e) {
            return null;
        }
    }
    
    private AttributeModifier.Operation getOperation() {
        try {
            if (operationType == OPERATION_ADD) {
                return AttributeModifier.Operation.valueOf("ADD_VALUE");
            } else if (operationType == OPERATION_MULTIPLY_BASE) {
                return AttributeModifier.Operation.valueOf("MULTIPLY_BASE");
            } else if (operationType == OPERATION_MULTIPLY_TOTAL) {
                return AttributeModifier.Operation.valueOf("MULTIPLY_TOTAL");
            }
        } catch (Exception e) {
            try {
                if (operationType == OPERATION_ADD) {
                    return AttributeModifier.Operation.valueOf("ADD");
                }
            } catch (Exception e2) {
            }
        }
        return AttributeModifier.Operation.MULTIPLY_BASE;
    }
    
    public float getValue(float random) {
        if (valueMin >= valueMax) return valueMin;
        return valueMin + random * (valueMax - valueMin);
    }
    
    public void applyToEntity(LivingEntity entity, float random) {
        Attribute attribute = getAttribute();
        if (attribute == null) return;
        
        AttributeInstance attributeInstance = entity.getAttribute(attribute);
        if (attributeInstance == null) return;
        
        float value = getValue(random);
        UUID uuid = UUID.randomUUID();
        AttributeModifier modifier = new AttributeModifier(uuid, name, value, getOperation());
        attributeInstance.addPermanentModifier(modifier);
    }
}
