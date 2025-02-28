package net.denfry.bannedWordsMute;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;

import java.util.UUID;

public class ChatListener implements Listener {
    private final BannedWordsMute plugin;

    public ChatListener(BannedWordsMute plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        String message = event.getMessage().toLowerCase();

        for (String word : plugin.getBannedWords()) {
            if (message.contains(word.toLowerCase())) {
                event.setCancelled(true);

                if (plugin.shouldUseLiteBans()) {
                    plugin.mutePlayerLiteBans(playerUUID, player.getName());
                } else {
                    player.sendMessage(ChatColor.RED + plugin.getRawMessage("muted-message"));
                }
                return;
            }
        }
    }

}
