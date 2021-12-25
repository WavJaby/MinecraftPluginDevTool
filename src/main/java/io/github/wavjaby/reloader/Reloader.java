package io.github.wavjaby.reloader;

import io.github.wavjaby.Main;
import io.github.wavjaby.commands.ReloadCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static io.github.wavjaby.Main.*;

public class Reloader {
    private final File pluginFolder = new File("plugins");
    private final BukkitCommandWrap commandWrap;
    private final PluginLoader pluginLoader;
    private final PluginManager manager = Bukkit.getPluginManager();
    private final HashMap<String, String> filePluginMap = new HashMap<>();
    private final HashMap<String, Long> fileModify = new HashMap<>();
    private List<String> disableList, autoReloadList;

    private final ReloadCommand reload;

    public Reloader(ReloadCommand reload) {
        this.reload = reload;
        pluginLoader = instance.getPluginLoader();
        commandWrap = new BukkitCommandWrap(mcVersion);
        loadSetting();
    }

    public void loadSetting() {
        disableList = reload.setting.getStringList("disabled");
        autoReloadList = reload.setting.getStringList("autoReload");

        filePluginMap.clear();
        fileModify.clear();
        for (File file : new File("plugins").listFiles()) {
            if (!file.getName().endsWith(".jar")) continue;
            try {
                PluginDescriptionFile desc = pluginLoader.getPluginDescription(file);
                filePluginMap.put(desc.getName(), file.getName());
                if (autoReloadList.contains(desc.getName()))
                    fileModify.put(file.getName(), file.lastModified());
            } catch (InvalidDescriptionException e) {
                e.printStackTrace();
            }
        }
    }

    public void startAutoReload() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(instance, () -> {
            for (Map.Entry<String, Long> data : fileModify.entrySet()) {
                File file = new File(pluginFolder, data.getKey());
                if (!file.exists() || !file.isFile()) continue;
                if (data.getValue() != file.lastModified()) {
                    try {
                        PluginDescriptionFile desc = pluginLoader.getPluginDescription(file);
                        String name = desc.getName();
                        unload(getPlugin(name), name);
                        load(file, name);
                    } catch (InvalidDescriptionException e) {
                        e.printStackTrace();
                        error(Main.instance.getMessage("load.des_error", file.getName()));
                    }
                }
                data.setValue(file.lastModified());
            }
        }, 1, 5);
    }

    public Set<String> getPluginsName() {
        return filePluginMap.keySet();
    }

    public String addAutoReload(String name) {
        Plugin plugin = getPlugin(name);
        if (plugin == null)
            return instance.getMessage("autoReload.not_found", name);
        if (autoReloadList.contains(plugin.getName()))
            return instance.getMessage("autoReload.already_added", plugin.getName());
        autoReloadList.add(plugin.getName());
        File file = new File(pluginFolder, filePluginMap.get(plugin.getName()));
        fileModify.put(file.getName(), file.lastModified());
        reload.setting.set("autoReload", autoReloadList);
        reload.settingFile.save();
        return instance.getMessage("autoReload.added", plugin.getName());
    }

    public String disable(String name) {
        Plugin plugin = getPlugin(name);
        if (plugin == null || !plugin.isEnabled())
            return instance.getMessage("disable.already_disabled", plugin.getName());
        manager.disablePlugin(plugin);
        disableList.add(plugin.getName());
        reload.setting.set("disabled", disableList);
        reload.settingFile.save();
        return instance.getMessage("disable.disabled", plugin.getName());
    }

    public String enable(String name) {
        Plugin plugin = getPlugin(name);
        if (plugin == null || plugin.isEnabled())
            return instance.getMessage("enable.already_enabled", plugin.getName());
        manager.enablePlugin(plugin);
        disableList.remove(plugin.getName());
        reload.setting.set("disabled", disableList);
        reload.settingFile.save();
        return instance.getMessage("enable.enabled", plugin.getName());
    }

    private boolean failed;

    public String reload(String name) {
        Plugin plugin = getPlugin(name);
        String message = null;
        failed = true;
        if (plugin != null)
            message = unload(plugin, name);
        if (failed) return message;
        failed = true;
        message = load(name);
        if (failed) return message;
        return Main.instance.getMessage("reload.reloaded", name);
    }

    public String unload(String name) {
        return unload(getPlugin(name), name);
    }

    private Plugin getPlugin(String name) {
        for (Plugin plugin : manager.getPlugins()) {
            if (plugin.getName().equalsIgnoreCase(name)) return plugin;
        }
        return null;
    }

    public boolean loaded(File file) {
        try {
            PluginDescriptionFile desc = pluginLoader.getPluginDescription(file);
            if (filePluginMap.containsKey(desc.getName())) {
                return true;
            }
        } catch (InvalidDescriptionException e) {
            return false;
        }
        return false;
    }

    /**
     * from https://github.com/TheBlackEntity/PlugMan/blob/master/src/main/java/com/rylinaux/plugman/util/PluginUtil.java
     * PlugMan PluginUtil.java load() and unload()
     */
    public String load(String name) {
        File pluginDir = new File("plugins");

        if (!pluginDir.isDirectory())
            return Main.instance.getMessage("load.plugin_directory");

        File pluginFile = new File(pluginDir, name + ".jar");

        if (!pluginFile.isFile()) for (File f : pluginDir.listFiles()) {
            if (!f.getName().endsWith(".jar")) continue;
            try {
                PluginDescriptionFile desc = pluginLoader.getPluginDescription(f);
                if (desc.getName().equalsIgnoreCase(name)) {
                    name = desc.getName();
                    pluginFile = f;
                    break;
                }
            } catch (InvalidDescriptionException e) {
                return Main.instance.getMessage("load.des_error", pluginFile.getName());
            }
        }
        else
            try {
                PluginDescriptionFile desc = pluginLoader.getPluginDescription(pluginFile);
                name = desc.getName();
            } catch (InvalidDescriptionException e) {
                return Main.instance.getMessage("load.des_error", pluginFile.getName());
            }
        if (!pluginFile.isFile())
            return Main.instance.getMessage("load.not_found", pluginFile.getName());

        return load(pluginFile, name);
    }

    public String load(File pluginFile, String name) {
        Plugin target;
        if (filePluginMap.containsKey(name))
            return Main.instance.getMessage("load.already_loaded", name);
        try {
            target = manager.loadPlugin(pluginFile);
        } catch (InvalidDescriptionException e) {
            e.printStackTrace();
            return Main.instance.getMessage("load.invalid_description");
        } catch (InvalidPluginException e) {
            e.printStackTrace();
            return Main.instance.getMessage("load.invalid_plugin");
        }

        target.onLoad();
        manager.enablePlugin(target);

        Plugin finalTarget = target;
        Bukkit.getScheduler().runTaskLater(Main.instance, () -> {
            Map<String, Command> knownCommands = getKnownCommands();

            for (Map.Entry<String, Command> entry : knownCommands.entrySet().stream().filter(stringCommandEntry -> stringCommandEntry.getValue() instanceof PluginIdentifiableCommand).filter(stringCommandEntry -> {
                PluginIdentifiableCommand command = (PluginIdentifiableCommand) stringCommandEntry.getValue();
                return command.getPlugin().getName().equalsIgnoreCase(finalTarget.getName());
            }).collect(Collectors.toList())) {
                String alias = entry.getKey();
                Command command = entry.getValue();
                commandWrap.wrap(command, alias);
            }

            if (Bukkit.getOnlinePlayers().size() >= 1)
                for (Player player : Bukkit.getOnlinePlayers()) player.updateCommands();
        }, 10L);

        filePluginMap.put(target.getName(), pluginFile.getName());
        failed = false;
        return Main.instance.getMessage("load.loaded", target.getName());
    }

    public String unload(Plugin plugin, String name) {
        failed = false;
        if (plugin == null) return Main.instance.getMessage("unload.already_unloaded", name);
        failed = true;
        name = plugin.getName();

        Map<String, Command> knownCommands = getKnownCommands();

        for (Map.Entry<String, Command> entry : knownCommands.entrySet().stream().filter(stringCommandEntry -> stringCommandEntry.getValue() instanceof PluginIdentifiableCommand).filter(stringCommandEntry -> {
            PluginIdentifiableCommand command = (PluginIdentifiableCommand) stringCommandEntry.getValue();
            return command.getPlugin().getName().equalsIgnoreCase(plugin.getName());
        }).collect(Collectors.toList())) {
            String alias = entry.getKey();
            commandWrap.unwrap(alias);
        }

        if (Bukkit.getOnlinePlayers().size() >= 1)
            for (Player player : Bukkit.getOnlinePlayers()) player.updateCommands();

        SimpleCommandMap commandMap = null;
        List<Plugin> plugins = null;
        Map<String, Plugin> names = null;
        Map<String, Command> commands = null;
        Map<Event, SortedSet<RegisteredListener>> listeners = null;

        manager.disablePlugin(plugin);
        boolean reloadListeners = true;
        try {
            Field pluginsField = manager.getClass().getDeclaredField("plugins");
            pluginsField.setAccessible(true);
            plugins = (List<Plugin>) pluginsField.get(manager);

            Field lookupNamesField = manager.getClass().getDeclaredField("lookupNames");
            lookupNamesField.setAccessible(true);
            names = (Map<String, Plugin>) lookupNamesField.get(manager);

            try {
                Field listenersField = manager.getClass().getDeclaredField("listeners");
                listenersField.setAccessible(true);
                listeners = (Map<Event, SortedSet<RegisteredListener>>) listenersField.get(manager);
            } catch (Exception e) {
                reloadListeners = false;
            }

            Field commandMapField = manager.getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            commandMap = (SimpleCommandMap) commandMapField.get(manager);

            Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            commands = (Map<String, Command>) knownCommandsField.get(commandMap);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return Main.instance.getMessage("unload.failed", name);
        }

        manager.disablePlugin(plugin);

        if (plugins != null)
            plugins.remove(plugin);

        if (names != null)
            names.remove(name);

        if (reloadListeners && listeners != null)
            for (SortedSet<RegisteredListener> set : listeners.values())
                set.removeIf(value -> value.getPlugin() == plugin);

        if (commandMap != null)
            for (Iterator<Map.Entry<String, Command>> it = commands.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, Command> entry = it.next();
                if (entry.getValue() instanceof PluginCommand c) {
                    if (c.getPlugin() == plugin) {
                        c.unregister(commandMap);
                        it.remove();
                    }
                }
            }

        // Attempt to close the classloader to unlock any handles on the plugin's jar file.
        ClassLoader cl = plugin.getClass().getClassLoader();
        if (cl instanceof URLClassLoader) {
            try {
                Field pluginField = cl.getClass().getDeclaredField("plugin");
                pluginField.setAccessible(true);
                pluginField.set(cl, null);

                Field pluginInitField = cl.getClass().getDeclaredField("pluginInit");
                pluginInitField.setAccessible(true);
                pluginInitField.set(cl, null);

            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(Reloader.class.getName()).log(Level.SEVERE, null, ex);
            }

            try {
                ((URLClassLoader) cl).close();
            } catch (IOException ex) {
                Logger.getLogger(Reloader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // Will not work on processes started with the -XX:+DisableExplicitGC flag, but lets try it anyway.
        // This tries to get around the issue where Windows refuses to unlock jar files that were previously loaded into the JVM.
        System.gc();

        filePluginMap.remove(name);
        failed = false;
        return Main.instance.getMessage("unload.unloaded", name);
    }

    private static Field commandMapField;

    private static Field knownCommandsField;

    public Map<String, Command> getKnownCommands() {
        if (commandMapField == null) try {
            commandMapField = Class.forName("org.bukkit.craftbukkit." + mcVersion[3] + ".CraftServer").getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
        } catch (NoSuchFieldException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        SimpleCommandMap commandMap;
        try {
            commandMap = (SimpleCommandMap) commandMapField.get(Bukkit.getServer());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        if (knownCommandsField == null) try {
            knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return null;
        }

        Map<String, Command> knownCommands;

        try {
            knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }

        return knownCommands;
    }
}
