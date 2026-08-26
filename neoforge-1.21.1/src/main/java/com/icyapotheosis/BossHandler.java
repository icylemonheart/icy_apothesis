package com.icyapotheosis;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = IcyApotheosis.MOD_ID)
public class BossHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntitySpawn(FinalizeSpawnEvent event) {
        ServerLevelAccessor level = event.getLevel();
        if (level.isClientSide()) return;
        if (event.isSpawnCancelled()) return;
        if (!(event.getEntity() instanceof Mob)) return;

        Mob mob = (Mob) event.getEntity();

        ResourceLocation entityKey = EntityType.getKey(mob.getType());
        if (!IcyApotheosisConfig.entityRarityMap.containsKey(entityKey)) return;

        RandomSource random = mob.getRandom();
        String rarityStr = IcyApotheosisConfig.getRarityForEntity(entityKey, random);

        if (rarityStr == null || rarityStr.equals("null")) return;

        transformToApotheosisBoss(mob, rarityStr);

        IcyApotheosis.LOGGER.info("Transformed {} to Boss with rarity: {}", entityKey, rarityStr);

        if (IcyApotheosisConfig.shouldAnnounceBoss()) {
            announceBoss(mob, rarityStr);
        }

        if (IcyApotheosisConfig.shouldBossAutoAggro()) {
            Player nearestPlayer = level.getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), -1, false);
            if (nearestPlayer != null && !nearestPlayer.isCreative() && !nearestPlayer.isSpectator()) {
                mob.setTarget(nearestPlayer);
            }
        }
    }

    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBossChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob)) return;
        Mob mob = (Mob) event.getEntity();
        if (!mob.getPersistentData().getBoolean("apoth.boss")) return;

        LivingEntity newTarget = event.getNewAboutToBeSetTarget();
        if (newTarget instanceof Player) {
            Player p = (Player) newTarget;
            if (p.isCreative() || p.isSpectator()) {
                event.setCanceled(true);
                event.setNewAboutToBeSetTarget(null);
                mob.setTarget(null);
                mob.setLastHurtByMob(null);
            }
        }
    }

    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof ServerLevel)) return;
        ServerLevel level = (ServerLevel) event.getLevel();

        for (ServerPlayer p : level.players()) {
            if (!p.isCreative() && !p.isSpectator()) continue;
            AABB aabb = new AABB(p.blockPosition()).inflate(256);
            for (Mob boss : level.getEntitiesOfClass(Mob.class, aabb,
                    m -> m.getPersistentData().getBoolean("apoth.boss"))) {
                if (boss.getTarget() == p) boss.setTarget(null);
                if (boss.getLastHurtByMob() == p) boss.setLastHurtByMob(null);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob)) return;

        Mob mob = (Mob) event.getEntity();

        if (mob.getPersistentData().getBoolean("apoth.boss")) return;

        ResourceLocation entityKey = EntityType.getKey(mob.getType());
        if (!IcyApotheosisConfig.entityRarityMap.containsKey(entityKey)) return;

        RandomSource random = mob.getRandom();
        String rarityStr = IcyApotheosisConfig.getRarityForEntity(entityKey, random);

        if (rarityStr != null && !rarityStr.equals("null")) {
            transformToApotheosisBoss(mob, rarityStr);
        }
    }

    private static void transformToApotheosisBoss(Mob mob, String rarityStr) {
        RandomSource random = mob.getRandom();

        
        mob.getPersistentData().putBoolean("apoth.boss", true);
        mob.getPersistentData().putString("icy_apotheosis.rarity", rarityStr);
        mob.getPersistentData().putString("apoth.rarity", "apotheosis:" + rarityStr);

        BossRarityConfig rarityConfig = BossConfigManager.getInstance().getRarityConfig(rarityStr);

        applyBossEffects(mob, rarityConfig, random);
        applyBossModifiers(mob, rarityConfig, random);

        int color = getRarityColor(rarityStr);
        String bossNameStr = getBossName(mob, rarityStr);

        
        BossGearHelper.registerDeferred(mob, rarityStr, bossNameStr, color);

        if ("icycinder_end".equals(rarityStr)) {
            mob.setCustomNameVisible(true);
            CinderEndNameManager.registerBoss(mob);
        } else {
            MutableComponent bossNameComponent = createBossName(mob, rarityStr, color);
            mob.setCustomName(bossNameComponent);
            mob.setCustomNameVisible(true);
        }

        mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 3600));

        mob.setHealth(mob.getMaxHealth());
    }

    private static MutableComponent createBossName(Mob mob, String rarityStr, int color) {
        Component entityDisplayName = mob.getType().getDescription();
        MutableComponent rarityPrefix = Component.translatable("boss.icy_apotheosis.prefix." + rarityStr)
            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
        MutableComponent raritySuffix = Component.translatable("boss.icy_apotheosis.suffix." + rarityStr)
            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));

        MutableComponent bossName = Component.translatable("boss.icy_apotheosis.format." + rarityStr,
            rarityPrefix, entityDisplayName, raritySuffix);
        return bossName.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
    }

    private static String getBossName(Mob mob, String rarityStr) {
        Component entityDisplayName = mob.getType().getDescription();
        String entityName = entityDisplayName.getString();
        if ("icycinder_end".equals(rarityStr)) {
            return "冰烬之终焉" + entityName + "超主";
        }
        String prefix = Component.translatable("boss.icy_apotheosis.prefix." + rarityStr).getString();
        String suffix = Component.translatable("boss.icy_apotheosis.suffix." + rarityStr).getString();
        return prefix + entityName + suffix;
    }

    private static void applyBossEffects(Mob mob, BossRarityConfig config, RandomSource random) {
        int duration = mob instanceof net.minecraft.world.entity.monster.Creeper ? 6000 : Integer.MAX_VALUE;

        for (BossEffectConfig effectConfig : config.getEffects()) {
            if (effectConfig.shouldApply(random.nextFloat())) {
                MobEffectInstance effectInstance = effectConfig.createEffectInstance(duration, random.nextFloat());
                if (effectInstance != null) {
                    mob.addEffect(effectInstance);
                }
            }
        }
    }

    
    public static void reapplyBossEffects(Mob mob, String rarityStr) {
        BossRarityConfig config = BossConfigManager.getInstance().getRarityConfig(rarityStr);
        if (config == null) return;
        RandomSource random = mob.getRandom();
        int duration = mob instanceof net.minecraft.world.entity.monster.Creeper ? 6000 : Integer.MAX_VALUE;

        for (BossEffectConfig effectConfig : config.getEffects()) {
            
            MobEffectInstance effectInstance = effectConfig.createEffectInstance(duration, random.nextFloat());
            if (effectInstance != null) {
                mob.addEffect(effectInstance);
            }
        }
    }

    private static void applyBossModifiers(Mob mob, BossRarityConfig config, RandomSource random) {
        for (BossModifierConfig modifierConfig : config.getModifiers()) {
            modifierConfig.applyToEntity(mob, random.nextFloat());
        }
    }

    private static int getRarityColor(String rarityStr) {
        switch (rarityStr.toLowerCase()) {
            case "null": return 0xFFFFFF;
            case "common": return 0x808080;
            case "uncommon": return 0x55FF55;
            case "rare": return 0x5555FF;
            case "epic": return 0xAA00AA;
            case "mythic": return 0xFFAA00;
            case "ancient": return 0xFF55FF;
            case "guixu": return 0xFF0000;
            case "icycinder_end": return 0x66CCFF;
            default: return 0xFFFFFF;
        }
    }

    private static void announceBoss(Mob boss, String rarityStr) {
        Component entityDisplayName = boss.getType().getDescription();
        MutableComponent rarityName = Component.translatable("rarity.icy_apotheosis." + rarityStr)
            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(getRarityColor(rarityStr))));

        MutableComponent message = Component.translatable(
            "info.icy_apotheosis.boss_spawn",
            entityDisplayName,
            rarityName
        );

        ServerLevel level = (ServerLevel) boss.level();
        level.players().forEach(p -> {
            double distSqr = p.distanceToSqr(boss.getX(), p.getY(), boss.getZ());
            if (distSqr <= 256 * 256) {
                ((ServerPlayer) p).connection.send(
                    new ClientboundSetActionBarTextPacket(message)
                );
            }
        });
    }
}
