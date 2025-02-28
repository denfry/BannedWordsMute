package net.denfry.bannedWordsMute;

import litebans.api.Database;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

public final class BannedWordsMute extends JavaPlugin implements TabCompleter, Listener {

    private boolean useLiteBans;
    private final Logger log = getLogger();

    @Override
    public @NotNull Path getDataPath() {
        return super.getDataPath();
    }

    @Override
    public void onEnable() {
        log.info("📢 Загрузка BannedWordsMute...");

        saveDefaultConfig();
        createFile("banned-words.yml");
        createFile("messages.yml");
        reloadConfigValues();

        Objects.requireNonNull(getCommand("chatmute")).setExecutor(this);
        Objects.requireNonNull(getCommand("chatmute")).setTabCompleter(this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(this, this);

        checkLiteBans();

        log.info("✅ Плагин BannedWordsMute загружен!");
    }

    private void checkLiteBans() {
        Plugin liteBans = Bukkit.getServer().getPluginManager().getPlugin("LiteBans");

        if (liteBans != null && liteBans.isEnabled()) {
            try {
                if (Database.get() != null) {
                    useLiteBans = true;
                    log.info("✅ LiteBans найден! Муты будут применяться через него.");
                } else {
                    useLiteBans = false;
                    log.warning("⚠ LiteBans загружен, но API недоступно! Будет использоваться стандартный мут.");
                }
            } catch (Exception e) {
                useLiteBans = false;
                log.severe("❌ Ошибка при проверке LiteBans API: " + e.getMessage());
            }
        }
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (event.getPlugin().getName().equalsIgnoreCase("LiteBans")) {
            log.info("🔄 Обнаружена загрузка LiteBans! Проверяем снова...");
            checkLiteBans();
        }
    }


    private void createFile(String fileName) {
        File file = new File(getDataFolder(), fileName);
        if (!file.exists()) {
            saveResource(fileName, false);
            log.info("📄 Создан файл: " + fileName);
        }
    }

    public void reloadConfigValues() {
        reloadConfig();
        log.info("🔄 Конфигурация перезагружена.");
    }


    public void mutePlayerLiteBans(UUID playerUUID, String playerName) {
        if (!useLiteBans) return;

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            if (Database.get().getMute(playerUUID, null, null) != null) {
                log.info("⚠ Игрок " + playerName + " уже замучен.");
                return;
            }

            Bukkit.getScheduler().runTask(this, () -> {
                String timeFormat = getConfig().getString("mute-time-format", "m");
                int muteTime = getConfig().getInt("mute-duration", 10);
                String muteReason = getRawMessage("mute-reason");

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "tempmute " + playerUUID + " " + muteTime + timeFormat + " " + muteReason);

                log.info("🔇 Игрок " + playerName + " был замучен на " + muteTime + timeFormat + " через LiteBans.");
            });
        });
    }

    public List<String> getBannedWords() {
        File file = new File(getDataFolder(), "banned-words.yml");
        if (!file.exists()) {
            log.warning("⚠ Файл banned-words.yml отсутствует! Используются стандартные значения.");
            return new ArrayList<>();
        }
        return YamlConfiguration.loadConfiguration(file).getStringList("banned-words");
    }

    public String getRawMessage(String key) {
        File file = new File(getDataFolder(), "messages.yml");
        if (!file.exists()) {
            log.warning("⚠ Файл messages.yml отсутствует! Используется стандартное сообщение.");
            return "Причина не найдена!";
        }
        return YamlConfiguration.loadConfiguration(file).getString(key, "Причина не найдена!");
    }

    public boolean shouldUseLiteBans() {
        return useLiteBans;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String @NotNull [] args) {
        if (command.getName().equalsIgnoreCase("chatmute")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("bannedwordsmute.reload")) {
                    reloadConfigValues();
                    sender.sendMessage(ChatColor.GREEN + "✅ Конфигурация успешно перезагружена!");
                    log.info("🔄 Конфигурация перезагружена через команду.");
                } else {
                    sender.sendMessage(ChatColor.RED + "❌ У вас нет прав для использования этой команды!");
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, Command command, @NotNull String alias, String @NotNull [] args) {
        List<String> completions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("chatmute")) {
            if (args.length == 1 && sender.hasPermission("bannedwordsmute.reload")) {
                completions.add("reload");
            }
        }
        return completions;
    }
}
