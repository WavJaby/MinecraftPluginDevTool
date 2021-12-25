package io.github.wavjaby.yaml;

import io.github.wavjaby.Main;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static io.github.wavjaby.Main.console;
import static io.github.wavjaby.Main.error;

public class YamlFile {
    public final static String PLUGIN_DIR = "plugins" + File.separator + Main.PLUGIN_NAME + File.separator;
    public FileConfiguration config;
    private File configFile;

    public YamlFile(String name, boolean copyDefault) {
        File configFolder = new File(PLUGIN_DIR);
        if (!configFolder.exists()) configFolder.mkdir();

        configFile = new File(PLUGIN_DIR + name);

        if (copyDefault) {
            if (!configFile.exists()) {
                console("Loading default " + name);
                configFile = exportResource(name);
            }
            if (configFile == null || !configFile.exists()) {
                error("failed to load " + name);
                return;
            }
        } else if (!configFile.exists()) {
            try {
                if (!configFile.createNewFile())
                    error("failed to create " + name);
            } catch (IOException e) {
                e.printStackTrace();
                error("failed to create " + name);
            }
        }
        console("Loading " + name);
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reload() {
        try {
            config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            error("failed to reload " + configFile.getName());
        }
    }

    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
            error("failed to save " + configFile.getName());
        }
    }

    private File exportResource(String resourceName) {
        InputStream fileInJar = getClass().getClassLoader().getResourceAsStream(resourceName);

        try {
            if (fileInJar == null) {
                error("cant get resource " + resourceName);
                return null;
            }
            Files.copy(fileInJar, Paths.get(PLUGIN_DIR + resourceName), StandardCopyOption.REPLACE_EXISTING);
            return new File(PLUGIN_DIR + resourceName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
