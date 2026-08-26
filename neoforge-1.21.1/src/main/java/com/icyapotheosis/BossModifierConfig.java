package com.icyapotheosis;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

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

    
    public Holder<Attribute> getAttribute() {
        try {
            ResourceLocation rl = ResourceLocation.parse(attributeId);
            return BuiltInRegistries.ATTRIBUTE.getHolder(rl).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private AttributeModifier.Operation getOperation() {
        
        if (operationType == OPERATION_ADD) {
            return AttributeModifier.Operation.ADD_VALUE;
        } else if (operationType == OPERATION_MULTIPLY_BASE) {
            return AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        } else if (operationType == OPERATION_MULTIPLY_TOTAL) {
            return AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        }
        return AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
    }

    public float getValue(float random) {
        if (valueMin >= valueMax) return valueMin;
        return valueMin + random * (valueMax - valueMin);
    }

    public void applyToEntity(LivingEntity entity, float random) {
        Holder<Attribute> attribute = getAttribute();
        if (attribute == null) return;

        AttributeInstance attributeInstance = entity.getAttribute(attribute);
        if (attributeInstance == null) return;

        float value = getValue(random);
        
        
        ResourceLocation modifierId = ResourceLocation.tryParse("icy_apotheosis:" + name.toLowerCase().replace(' ', '_'));
        if (modifierId == null) {
            modifierId = ResourceLocation.parse("icy_apotheosis:boss_modifier");
        }
        AttributeModifier modifier = new AttributeModifier(modifierId, value, getOperation());
        attributeInstance.addPermanentModifier(modifier);
    }
}
