package ru.nern.rollplay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Stream;

public class ConfigurationManager
{
    private static final String CONFIG_VERSION = FabricLoader.getInstance().getModContainer("rollplay").get().getMetadata().getVersion().getFriendlyString();
    public static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final File file = new File(FabricLoader.getInstance().getConfigDir().toFile(), "rollplay_config.json");

    public static void loadConfig() {
        try {
            if (file.exists()) {
                StringBuilder contentBuilder = new StringBuilder();
                try (Stream<String> stream = Files.lines(file.toPath(), StandardCharsets.UTF_8)) {
                    stream.forEach(s -> contentBuilder.append(s).append("\n"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                RollPlayMod.config = gson.fromJson(contentBuilder.toString(), Config.class);
            } else {
                RollPlayMod.config = new Config();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        setConfig(RollPlayMod.config);
    }

    public static void saveConfig() {
        RollPlayMod.config.lastLoadedVersion = CONFIG_VERSION;
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(gson.toJson(getConfig()));
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void onInit()
    {
        if(!file.exists())
        {
            saveConfig();
        }else{
            loadConfig();
            if(!RollPlayMod.config.lastLoadedVersion.equals(CONFIG_VERSION)) saveConfig();
        }
    }

    public static void setConfig(Config config) {
        RollPlayMod.config = config;
    }

    public static Config getConfig() {
        return RollPlayMod.config;
    }

    public static class Config
    {
        private String lastLoadedVersion = "";
        public RollCommand roll;
        public CoinCommand coin;
        public TryCommand trycmd;
        public String reloadConfigMessage = "Rollplay Mod Config был успешно перезагружен";

        public static class RollCommand {
            public String selfMessage = "Вам выпало: [%s]";
            public String message = "%p выбил: [%r]";
            public int messageRange = 20;
            public boolean playSound = true;
        }


        public static class CoinCommand {
            public String selfMessageOrel = "Выпал %s!";
            public String selfMessageReshka = "Выпала %s!";
            public String message = "%p выбил: [%r]";
            public String orel = "орёл";
            public String reshka = "решка";
            public int messageRange = 20;
            public boolean playSound = true;
        }

        public static class TryCommand {
            public String message = "%player %action [%result]";
            public String success = "Удачно";
            public String fail = "Неудачно";
            public int messageRange = 20;
            public boolean playSound = true;
        }

        public Config()
        {
            roll = new RollCommand();
            coin = new CoinCommand();
            trycmd = new TryCommand();
        }
    }
}
