package io.github.wavjaby.commands;

import io.github.wavjaby.reloader.Reloader;
import io.github.wavjaby.yaml.YamlFile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static io.github.wavjaby.Main.instance;

public class ReloadCommand implements CommandExecutor, TabCompleter {
    private final Reloader reloader;
    public final YamlFile settingFile;
    public FileConfiguration setting;

    public ReloadCommand() {
        settingFile = new YamlFile("reloader.yml", true);
        setting = settingFile.config;
        reloader = new Reloader(this);
        reloader.startAutoReload();
    }

    public void reload() {
        settingFile.reload();
        setting = settingFile.config;
        reloader.loadSetting();
    }

    @Override
    public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, String[] args) {
        if (args.length == 0)
            return null;

        List<String> list = new ArrayList<>();
        switch (args.length) {
            case 1:
                if ("reload".contains(args[0]))
                    list.add("reload");
                if ("unload".contains(args[0]))
                    list.add("unload");
                if ("load".contains(args[0]))
                    list.add("load");
                if ("plugins".contains(args[0]))
                    list.add("plugins");
                if ("enable".contains(args[0]))
                    list.add("enable");
                if ("disable".contains(args[0]))
                    list.add("disable");
                if ("addAutoReload".contains(args[0]))
                    list.add("addAutoReload");
                break;
            case 2:
                boolean enable = args[0].equals("enable");
                boolean disable = args[0].equals("disable");
                if (args[0].equals("reload") || args[0].equals("unload") ||
                        enable || disable ||
                        args[0].equals("addAutoReload")
                ) {
                    for (Plugin plugin : Bukkit.getPluginManager().getPlugins())
                        if (plugin.getName().contains(args[1]) &&
                                (!enable || !plugin.isEnabled()) &&
                                (!disable || plugin.isEnabled()))
                            list.add(plugin.getName());
                } else if (args[0].equals("load"))
                    for (File file : new File("plugins").listFiles()) {
                        if (!file.getName().endsWith(".jar")) continue;
                        String name = file.getName().substring(0, file.getName().length() - 4);
                        if (!reloader.loaded(file))
                            list.add(name);
                    }
                break;
        }
        return list;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String[] args) {
        switch (args.length) {
            case 1:
                if (args[0].equals("reload")) {
                    instance.reload();
                    sender.sendMessage(ChatColor.GREEN + "DevTool config reloaded");
                } else if (args[0].equals("plugins")) {
                    StringBuilder builder = new StringBuilder();
                    Plugin[] plugins = Bukkit.getPluginManager().getPlugins();
                    for (int i = 0; i < plugins.length; i++) {
                        if (i != 0) builder.append(ChatColor.RESET)
                                .append(", ");
                        builder.append(plugins[i].isEnabled() ? ChatColor.GREEN : ChatColor.BLUE)
                                .append(plugins[i].getName());
                    }
                    sender.sendMessage(instance.getMessage("list.list", String.valueOf(plugins.length), builder.toString()));
                }
                break;
            case 2:
                switch (args[0]) {
                    case "reload" -> sender.sendMessage(reloader.reload(args[1]));
                    case "load" -> sender.sendMessage(reloader.load(args[1]));
                    case "unload" -> sender.sendMessage(reloader.unload(args[1]));
                    case "enable" -> sender.sendMessage(reloader.enable(args[1]));
                    case "disable" -> sender.sendMessage(reloader.disable(args[1]));
                    case "addAutoReload" -> sender.sendMessage(reloader.addAutoReload(args[1]));
                }
                break;
        }
        return false;
    }
}
