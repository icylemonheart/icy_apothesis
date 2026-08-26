package com.icyapotheosis;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(IcyApotheosis.MOD_ID)
public class IcyApotheosis {

    public static final String MOD_ID = "icy_apotheosis";
    public static final Logger LOGGER = LogManager.getLogger();

    public IcyApotheosis(IEventBus bus, ModContainer modContainer) {
        
        bus.register(this);
        
        modContainer.registerConfig(ModConfig.Type.COMMON, IcyApotheosisConfig.SPEC);
        BossConfigManager.getInstance();
    }

    @SubscribeEvent
    public void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Icy Apotheosis Boss initialized.");
        LOGGER.info("Boss rarity configs loaded: {}", BossConfigManager.getInstance().getRarityConfigs());
    }

    
    @SubscribeEvent
    public void onModConfig(final ModConfigEvent event) {
        IcyApotheosisConfig.handleModConfigEvent(event);
    }
}
