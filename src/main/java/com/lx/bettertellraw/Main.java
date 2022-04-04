package com.lx.bettertellraw;

import com.lx.bettertellraw.Config.config;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("btellraw");
    public static Version versions = null;

    @Override
    public void onInitialize() {
        versions = FabricLoader.getInstance().getModContainer("btellraw").get().getMetadata().getVersion();
        LOGGER.info("[BetterTellraw] Version " + versions);
        config.loadConfig();
    }
}
