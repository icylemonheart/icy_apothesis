package com.icyapotheosis;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(IcyApotheosis.MOD_ID)
public class IcyApotheosis {

    public static final String MOD_ID = "icy_apotheosis";
    public static final Logger LOGGER = LogManager.getLogger();

    public IcyApotheosis() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        IcyApotheosisConfig.register();
        BossConfigManager.getInstance();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Icy Apotheosis Boss initialized.");
        LOGGER.info("Boss rarity configs loaded: {}", BossConfigManager.getInstance().getRarityConfigs());
    }
}
