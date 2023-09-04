package com.lx862.btellraw.config;

import com.google.gson.*;
import com.lx862.btellraw.BetterTellraw;
import com.lx862.btellraw.data.TellrawEntry;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FilenameUtils;
import java.util.*;

public class Config {
    private static final File TELLRAW_DIR = FabricLoader.getInstance().getConfigDir().resolve("btellraw").resolve("tellraws").toFile();
    public static final Map<String, TellrawEntry> tellrawList = new HashMap<>();

    public static int load() {
        if(!Files.exists(TELLRAW_DIR.toPath())) {
            BetterTellraw.LOGGER.info("[BetterTellraw] Tellraws folder not found, generating one.");
            generateConfig();
            /* Load the config again from the files we just generated. */
            load();
            return 0;
        }

        BetterTellraw.LOGGER.info("[BetterTellraw] Reading Config...");
        tellrawList.clear();

        int loadedTellraw = 0;

        for (File file : Objects.requireNonNull(TELLRAW_DIR.listFiles())) {
            loadedTellraw += readTellraws(file.toPath());
        }

        BetterTellraw.LOGGER.info("[BetterTellraw] " + loadedTellraw + " tellraws Loaded");
        return loadedTellraw;
    }

    public static void generateConfig() {
        TELLRAW_DIR.mkdirs();
        final JsonObject jsonConfig = new JsonObject();

        jsonConfig.addProperty("example1", "{\"text\": \"This is a default tellraw written in Minecraft's Raw JSON Text.\", \"color\": \"gold\"}");
        jsonConfig.addProperty("example2", "<yellow>This is a default tellraw written with <hover:show_text:Hover Text><underline><click:open_url:https://placeholders.pb4.eu/user/text-format/>PlaceHolder API's Simplified Text Format");
        jsonConfig.addProperty("placeholder", "<aqua>Try running the following command:\n/btellraw send entity @s example.placeholder \"3 Minutes,Maintenance\"\n\n<green>This server will restart in %s due to %s.");
        try {
            Files.write(TELLRAW_DIR.toPath().resolve("example.json"), Collections.singleton(new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(jsonConfig)));
        } catch (Exception e) {
            BetterTellraw.LOGGER.warn("[BetterTellraw] Unable to generate Config File!");
            e.printStackTrace();
        }
    }

    public static void saveConfig() {
        Map<String, JsonObject> configs = new HashMap<>();

        for (Map.Entry<String, TellrawEntry> tellraws : tellrawList.entrySet()) {
            TellrawEntry tellraw = tellraws.getValue();

            JsonObject jsonConfig = configs.getOrDefault(tellraw.fileName, new JsonObject());
            jsonConfig.addProperty(tellraw.ID, tellraw.content);
            configs.put(tellraw.fileName, jsonConfig);
        }

        for (Map.Entry<String, JsonObject> config : configs.entrySet()) {
            try {
                Files.write(TELLRAW_DIR.toPath().resolve(config.getKey() + ".json"), Collections.singleton(new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(config.getValue())));
            } catch (Exception e) {
                BetterTellraw.LOGGER.warn("[BetterTellraw] Unable to generate Config File!");
                e.printStackTrace();
            }
        }
    }

    public static int readTellraws(Path tellrawLocation) {
        int loadedTellraw = 0;
        try {
            final JsonObject jsonConfig = new JsonParser().parse(String.join("", Files.readAllLines(tellrawLocation))).getAsJsonObject();

            for(Map.Entry<String, JsonElement> e : jsonConfig.entrySet()) {
                String tellrawID = e.getKey();
                String jsontext = e.getValue().getAsString();
                String fullID = FilenameUtils.getBaseName(tellrawLocation.getFileName().toString()) + "." + tellrawID;
                TellrawEntry tellrawObj = new TellrawEntry(FilenameUtils.getBaseName(tellrawLocation.getFileName().toString()), jsontext, fullID, tellrawID);
                tellrawList.put(fullID, tellrawObj);
                loadedTellraw++;
            }
            return loadedTellraw;
        } catch (Exception e) {
            BetterTellraw.LOGGER.error("Failed to load tellraw file " + tellrawLocation.getFileName().toString());
            e.printStackTrace();
            return 0;
        }
    }
}
