package com.bradleyjh.blazefly;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class EventsListener implements Listener {
	private Main plugin;

	public EventsListener(Main plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onDamage(EntityDamageEvent event) {
		if ((event.getEntity() instanceof Player)) {
			Player player = (Player) event.getEntity();

			// Protect players falling due to running out of fuel, if enabled
			if ((event.getCause().equals(EntityDamageEvent.DamageCause.FALL)) && (plugin.core.isFalling(player))) {
				if (plugin.getConfig().getBoolean("fallProtection")) {
					event.setCancelled(true);
					return;
				}
			}

			// Ignore these types of damage
			if (event.getCause().equals(EntityDamageEvent.DamageCause.DROWNING)) return;
			if (event.getCause().equals(EntityDamageEvent.DamageCause.STARVATION)) return;
			if (event.getDamage() == 0) return;

			// PvP stuff, break wings (disallow flight) if player takes damage
			if ((plugin.getConfig().getBoolean("breakableWings")) && (!plugin.hasPermission(player, "superwings"))
					&& (plugin.core.isFlying(player))) {
				plugin.core.setFalling(player, true);
				plugin.core.setFlying(player, false);
				plugin.core.setBrokenCounter(player, plugin.getConfig().getDouble("healTime"));
				player.setAllowFlight(false);
				plugin.core.messagePlayer(player, "wBroke", null);
			}
		}
	}

	// Reset flyspeed & message admins for new versions
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		player.setFlySpeed(0.1F);

		plugin.core.retrievePlayer(player);

		if (plugin.hasPermission(player, "updates") && plugin.updateAvailable != null) {
			String header = plugin.getConfig().getString("header");
			String message = header + plugin.updateAvailable + " is available for download";
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
		}
	}

	// Clear broken wing counter if player dies
	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		if ((event.getEntity() instanceof Player)) {
			Player player = (Player) event.getEntity();

			if (plugin.core.isBroken(player)) {
				plugin.core.removeBroken(player);
				player.setAllowFlight(true);
				plugin.core.setFlying(player, true);
				plugin.core.messagePlayer(player, "wHealed", null);
			}
		}
	}

	// Prevent flight toggle for mode switch
	@EventHandler
	public void onGameMode(PlayerGameModeChangeEvent event) {
		if (event.getNewGameMode() == GameMode.SURVIVAL || event.getNewGameMode() == GameMode.ADVENTURE) {
			if (plugin.core.isFlying(event.getPlayer())) {
				final Player player = event.getPlayer();
				plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					public void run() {
						player.setAllowFlight(true);
						player.getPlayer().setFlying(true);
					}
				}, 2L);
			}
		}
	}
}
