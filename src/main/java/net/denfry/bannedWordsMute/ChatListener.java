package net.denfry.bannedWordsMute;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ChatListener implements Listener {
    private final BannedWordsMute plugin;
    private List<String> bannedWords;
    private final boolean isFolia;

    public ChatListener(BannedWordsMute plugin) {
        this.plugin = plugin;
        this.isFolia = plugin.isFoliaEnabled();
        loadBannedWords();
    }

    public void loadBannedWords() {
        bannedWords = plugin.getBannedWords().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().toLowerCase();

        String[] words = message.split("\\s+|\\p{Punct}");

        for (String word : words) {
            if (bannedWords.contains(word)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "❌ Ваше сообщение содержит запрещенные слова!");

                UUID playerUUID = player.getUniqueId();

                if (plugin.shouldUseLiteBans()) {
                    plugin.mutePlayerLiteBans(playerUUID, player.getName());
                } else {
                    if (isFolia) {
                        Bukkit.getGlobalRegionScheduler().execute(plugin, () ->
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                        "mute " + player.getName() + " " + plugin.getConfig().getInt(
                                                "mute-duration", 10) +
                                                "m " + plugin.getRawMessage("mute-reason")));
                    } else {
                        Bukkit.getScheduler().runTask(plugin, () ->
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                        "mute " + player.getName() + " " + plugin.getConfig().getInt(
                                                "mute-duration", 10) +
                                                "m " + plugin.getRawMessage("mute-reason")));
                    }
                }
                return;
            }
        }
    }
}
