package io.github.wavjaby;

import io.github.wavjaby.commands.ReloadCommand;
import io.github.wavjaby.yaml.YamlFile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    public final static ConsoleCommandSender console = Bukkit.getConsoleSender();//console
    public final static String PLUGIN_NAME = "PluginDevTool";
    final static String pluginPrefix = "[" + ChatColor.LIGHT_PURPLE + PLUGIN_NAME + ChatColor.RESET + "]";
    final static String errorPluginPrefix = "[" + ChatColor.RED + PLUGIN_NAME + ChatColor.RESET + "]";
    public static Main instance;
    public static String[] mcVersion;

    public YamlFile message;


    @Override
    public void onEnable() {
        instance = this;
        mcVersion = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",");
        message = new YamlFile("message.yml", true);
        initCommand();
        console.sendMessage(pluginPrefix + ChatColor.GREEN + "啟動完畢");
    }

    @Override
    public void onDisable() {
        console.sendMessage(pluginPrefix + ChatColor.GREEN + "已停止");
    }

    public void reload() {
        message.reload();
        reloadCommand.reload();
    }

    ReloadCommand reloadCommand;
    private void initCommand() {
        reloadCommand = new ReloadCommand();
        getCommand("devTool").setExecutor(reloadCommand);
        getCommand("devTool").setTabCompleter(reloadCommand);
    }

    public String getMessage(String path, String ...args) {
        String out = message.config.getString(path);
        if(out == null) return ChatColor.RED + "missing variable: " + path;
        for (int i = 0; i < args.length; i++)
            out = out.replace("{" + i + "}", String.valueOf(args[i]));
        return ChatColor.translateAlternateColorCodes('&', out);
    }

    public String getMessage(String path) {
        String out = message.config.getString(path);
        if(out == null) return ChatColor.RED + "missing variable: " + path;
        return ChatColor.translateAlternateColorCodes('&', out);
    }

    public static void console(String message) {
        console.sendMessage(pluginPrefix + message);
    }

    public static void error(String message) {
        console.sendMessage(errorPluginPrefix + message);
    }
}
