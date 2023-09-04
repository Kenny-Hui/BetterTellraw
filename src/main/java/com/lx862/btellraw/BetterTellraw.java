package com.lx862.btellraw;

import com.lx862.btellraw.commands.btellraw;
import com.lx862.btellraw.config.Config;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BetterTellraw implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("btellraw");

    @Override
    public void onInitialize() {
        LOGGER.info("[BetterTellraw] BetterTellraw Loaded!");
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            btellraw.register(dispatcher);
        });
        Config.load();
    }
}
